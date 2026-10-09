/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License").
 * You may not use this file except in compliance with the License.
 * A copy of the License is located at
 *
 *  http://aws.amazon.com/apache2.0
 *
 * or in the "license" file accompanying this file. This file is distributed
 * on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied. See the License for the specific language governing
 * permissions and limitations under the License.
 */

package software.amazon.awssdk.benchmark.harness;

import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.node.ObjectNode;

/**
 * The forked client process of one cell repetition. It builds the cell's client and explicit transport, verifies one
 * untimed operation against the fixture, warms up, measures for a fixed duration, and writes one result record (JSON)
 * to {@code --result-file}. Nothing is printed between the start of warmup and the end of measurement, so progress
 * output is never inside a timed region.
 *
 * <p>Usage: {@code ClientMain --cell ID --endpoint URL --warmup-seconds S --measure-seconds S --result-file PATH
 * [--model service=PATH]... [--repetition N] [--label L]}. Durations accept fractions so the smoke test can measure for
 * a fraction of a second; 19.2 replaces the fixed warmup with the JIT steady-state gate and enforces the 30-second
 * minimum sample.
 *
 * <p>Exit status: 0 when a record was written (whatever the operation outcomes; validity is judged from the record),
 * 2 for bad arguments, 3 when the cell could not be built or verified (the record then carries {@code error}).
 */
public final class ClientMain {

    /** The record schema identifier; bump on incompatible changes. */
    public static final String SCHEMA = "v3-benchmark-client-result/1";

    private ClientMain() {
    }

    /** Parsed client arguments. */
    record Args(BenchmarkCell cell, URI endpoint, Duration warmup, Duration measure, Path resultFile,
                Map<String, Path> models, int repetition, String label) {

        static Args parse(String[] argv) {
            BenchmarkCell cell = null;
            URI endpoint = null;
            Duration warmup = Duration.ofSeconds(10);
            Duration measure = Duration.ofSeconds(30);
            Path result = null;
            Map<String, Path> models = new LinkedHashMap<>();
            int repetition = 0;
            String label = null;
            for (int i = 0; i < argv.length; i++) {
                String a = argv[i];
                if (i + 1 >= argv.length) {
                    throw new IllegalArgumentException("missing value for " + a);
                }
                String v = argv[++i];
                switch (a) {
                    case "--cell" -> cell = BenchmarkCell.parse(v);
                    case "--endpoint" -> endpoint = URI.create(v);
                    case "--warmup-seconds" -> warmup = seconds(v);
                    case "--measure-seconds" -> measure = seconds(v);
                    case "--result-file" -> result = Path.of(v);
                    case "--model" -> {
                        int eq = v.indexOf('=');
                        if (eq <= 0) {
                            throw new IllegalArgumentException("--model takes service=PATH: " + v);
                        }
                        models.put(v.substring(0, eq), Path.of(v.substring(eq + 1)));
                    }
                    case "--repetition" -> repetition = Integer.parseInt(v);
                    case "--label" -> label = v;
                    default -> throw new IllegalArgumentException("unknown argument: " + a);
                }
            }
            if (cell == null || endpoint == null || result == null) {
                throw new IllegalArgumentException("--cell, --endpoint and --result-file are required");
            }
            if (measure.isZero() || measure.isNegative() || warmup.isNegative()) {
                throw new IllegalArgumentException("measure must be positive and warmup non-negative");
            }
            return new Args(cell, endpoint, warmup, measure, result, models, repetition,
                            label == null ? cell.id() : label);
        }

        static Duration seconds(String v) {
            return Duration.ofNanos((long) (Double.parseDouble(v) * 1e9));
        }
    }

    public static void main(String[] argv) throws Exception {
        Args args;
        try {
            args = Args.parse(argv);
        } catch (IllegalArgumentException e) {
            System.err.println(e.getMessage());
            System.exit(2);
            return;
        }
        ObjectNode.Builder record = ObjectNode.builder()
                                              .withMember("schema", SCHEMA)
                                              .withMember("cell", args.cell().id())
                                              .withMember("label", args.label())
                                              .withMember("repetition", args.repetition())
                                              .withMember("process", processMetadata());
        int status = 0;
        try {
            run(args, record);
        } catch (Exception | LinkageError e) {
            record.withMember("error", e.toString());
            status = 3;
        }
        Path parent = args.resultFile().toAbsolutePath().getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }
        Files.writeString(args.resultFile(), Node.prettyPrintJson(record.build()) + "\n", StandardCharsets.UTF_8);
        System.out.println("RESULT_FILE " + args.resultFile());
        System.exit(status);
    }

    private static void run(Args args, ObjectNode.Builder record) throws Exception {
        BenchmarkCell cell = args.cell();
        WorkloadProvider provider = WorkloadProvider.forCell(cell).orElseThrow(
            () -> new IllegalStateException("no workload fixture for cell " + cell));
        record.withMember("fixture", provider.name());
        record.withMember("expected_encoded_bytes_per_op", provider.fixture(cell).encodedBytes());
        long t0 = System.nanoTime();
        try (CellWorkload workload = provider.create(cell, args.endpoint(), new WorkloadProvider.Options(args.models()))) {
            long constructed = System.nanoTime();
            workload.verify();
            long firstCall = System.nanoTime();
            record.withMember("transport_class", workload.transportClass());
            record.withMember("construction_ns", constructed - t0);
            record.withMember("first_call_ns", firstCall - constructed);

            long warmupStart = System.nanoTime();
            Measurement warm = drive(workload, cell.concurrency(), args.warmup(), false);
            record.withMember("warmup", ObjectNode.builder()
                                                  .withMember("policy", "fixed-duration")
                                                  .withMember("wall_ns", System.nanoTime() - warmupStart)
                                                  .withMember("operations", warm.operations)
                                                  .withMember("failed_operations", warm.failures)
                                                  .build());

            Map<String, Long> serverBefore = serverStats(args.endpoint());
            long cpuBefore = processCpuNanos();
            long allocBefore = allocatedBytes();
            long gcBefore = gcMillis();
            Measurement m = drive(workload, cell.concurrency(), args.measure(), true);
            long gcAfter = gcMillis();
            long allocAfter = allocatedBytes();
            long cpuAfter = processCpuNanos();
            Map<String, Long> serverAfter = serverStats(args.endpoint());

            ObjectNode.Builder server = ObjectNode.builder();
            for (Map.Entry<String, Long> e : serverAfter.entrySet()) {
                server.withMember(e.getKey(), e.getValue() - serverBefore.getOrDefault(e.getKey(), 0L));
            }
            ObjectNode.Builder measurement = ObjectNode.builder()
                .withMember("wall_ns", m.wallNanos)
                .withMember("operations", m.operations)
                .withMember("failed_operations", m.failures)
                .withMember("process_cpu_ns", cpuAfter - cpuBefore)
                .withMember("allocated_bytes", allocBefore < 0 || allocAfter < 0 ? -1 : allocAfter - allocBefore)
                .withMember("gc_ms", gcAfter - gcBefore)
                .withMember("p50_ns", m.latency.quantile(0.50))
                .withMember("p99_ns", m.latency.quantile(0.99))
                .withMember("loaded_classes", ManagementFactory.getClassLoadingMXBean().getLoadedClassCount());
            if (m.firstFailure.get() != null) {
                measurement.withMember("first_failure", m.firstFailure.get());
            }
            record.withMember("measurement", measurement.build());
            record.withMember("service", server.build());
        }
    }

    /** The outcome of one driven window. */
    static final class Measurement {
        final LatencyHistogram latency = new LatencyHistogram();
        final AtomicLong opCount = new AtomicLong();
        final AtomicLong failCount = new AtomicLong();
        final AtomicReference<String> firstFailure = new AtomicReference<>();
        long operations;
        long failures;
        long wallNanos;

        void fail(Throwable t) {
            failCount.incrementAndGet();
            firstFailure.compareAndSet(null, String.valueOf(t));
        }
    }

    /**
     * Closed-loop drive for {@code duration}: sync paths run {@code concurrency} caller threads; async paths keep
     * {@code concurrency} operations in flight from one submitting thread. Waits for every started operation.
     */
    static Measurement drive(CellWorkload workload, int concurrency, Duration duration, boolean timed)
            throws InterruptedException {
        Measurement m = new Measurement();
        if (duration.isZero()) {
            return m;
        }
        long start = System.nanoTime();
        long deadline = start + duration.toNanos();
        if (workload.isAsync()) {
            Semaphore inflight = new Semaphore(concurrency);
            while (System.nanoTime() < deadline) {
                inflight.acquire();
                long s = System.nanoTime();
                try {
                    workload.callAsync().whenComplete((v, t) -> {
                        if (t != null) {
                            m.fail(t);
                        } else {
                            m.opCount.incrementAndGet();
                            if (timed) {
                                m.latency.record(System.nanoTime() - s);
                            }
                        }
                        inflight.release();
                    });
                } catch (RuntimeException e) {
                    m.fail(e);
                    inflight.release();
                }
            }
            inflight.acquire(concurrency);
        } else {
            List<Thread> threads = new ArrayList<>(concurrency);
            for (int i = 0; i < concurrency; i++) {
                Thread t = new Thread(() -> {
                    while (System.nanoTime() < deadline) {
                        long s = System.nanoTime();
                        try {
                            workload.call();
                            m.opCount.incrementAndGet();
                            if (timed) {
                                m.latency.record(System.nanoTime() - s);
                            }
                        } catch (RuntimeException | Error e) {
                            m.fail(e);
                        }
                    }
                }, "bench-caller-" + i);
                threads.add(t);
            }
            for (Thread t : threads) {
                t.start();
            }
            for (Thread t : threads) {
                t.join();
            }
        }
        m.wallNanos = System.nanoTime() - start;
        m.operations = m.opCount.get();
        m.failures = m.failCount.get();
        return m;
    }

    static ObjectNode processMetadata() {
        return ObjectNode.builder()
                         .withMember("pid", ProcessHandle.current().pid())
                         .withMember("jdk_vendor", System.getProperty("java.vendor"))
                         .withMember("jdk_version", System.getProperty("java.runtime.version"))
                         .withMember("jdk_vm", System.getProperty("java.vm.name") + " "
                                               + System.getProperty("java.vm.version"))
                         .withMember("jvm_input_arguments", String.join(" ", ManagementFactory.getRuntimeMXBean()
                                                                                         .getInputArguments()))
                         .withMember("available_processors", Runtime.getRuntime().availableProcessors())
                         .withMember("max_heap_bytes", Runtime.getRuntime().maxMemory())
                         .build();
    }

    private static long processCpuNanos() {
        return ((com.sun.management.OperatingSystemMXBean) ManagementFactory.getOperatingSystemMXBean())
            .getProcessCpuTime();
    }

    private static long allocatedBytes() {
        if (ManagementFactory.getThreadMXBean() instanceof com.sun.management.ThreadMXBean t
            && t.isThreadAllocatedMemorySupported() && t.isThreadAllocatedMemoryEnabled()) {
            return t.getTotalThreadAllocatedBytes();
        }
        return -1;
    }

    private static long gcMillis() {
        long total = 0;
        for (var gc : ManagementFactory.getGarbageCollectorMXBeans()) {
            total += Math.max(0, gc.getCollectionTime());
        }
        return total;
    }

    /** The mock service's {@code /stats} counters. */
    static Map<String, Long> serverStats(URI endpoint) throws IOException, InterruptedException {
        HttpClient http = HttpClient.newBuilder().version(HttpClient.Version.HTTP_1_1)
                                    .connectTimeout(Duration.ofSeconds(5)).build();
        HttpResponse<String> response = http.send(HttpRequest.newBuilder(endpoint.resolve("/stats")).GET().build(),
                                                  HttpResponse.BodyHandlers.ofString());
        Map<String, Long> stats = new LinkedHashMap<>();
        for (String line : response.body().split("\n")) {
            int eq = line.indexOf('=');
            if (eq > 0) {
                stats.put(line.substring(0, eq), Long.parseLong(line.substring(eq + 1).trim()));
            }
        }
        return stats;
    }
}
