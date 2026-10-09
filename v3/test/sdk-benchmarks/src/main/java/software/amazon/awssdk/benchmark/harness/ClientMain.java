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
import java.lang.management.CompilationMXBean;
import java.lang.management.ManagementFactory;
import java.net.Socket;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.LockSupport;
import java.util.function.Predicate;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.node.ObjectNode;

/**
 * The forked client process of one cell repetition. It builds the cell's client and explicit transport, verifies one
 * untimed operation against the fixture, warms up, measures, and writes one result record (JSON) to
 * {@code --result-file}. Nothing is printed between the start of warmup and the end of measurement, so progress
 * output is never inside a timed region.
 *
 * <p>Warmup is either the JIT steady-state gate of {@link SteadyStatePolicy} ({@code --warmup-policy steady-state}:
 * drive until a window of {@code --steady-window} sampled intervals is stable, at most
 * {@code --warmup-timeout-seconds}) or, for exploratory runs only, a fixed {@code --warmup-seconds}. Both warmup and
 * measurement are sampled every {@code --sample-interval-seconds} by one sampler thread that reads cumulative
 * counters only (no output, no per-operation work), so the validity rules can re-check the gate's closing window and
 * every rolling window of the measured series from the record. The stability thresholds themselves are constants.
 *
 * <p>Usage: {@code ClientMain --cell ID --endpoint URL --result-file PATH [--warmup-policy fixed|steady-state]
 * [--warmup-seconds S] [--sample-interval-seconds S] [--steady-window N] [--warmup-timeout-seconds S]
 * [--measure-seconds S] [--model service=PATH]... [--repetition N] [--label L]}. Durations accept fractions so the
 * smoke tests run in seconds; the release minimums are enforced by the orchestrator's preflight and the validity
 * rules, not here.
 *
 * <p>Exit status: 0 when a record was written (whatever the operation outcomes; validity is judged from the record),
 * 2 for bad arguments, 3 when the cell could not be built or verified (the record then carries {@code error}).
 */
public final class ClientMain {

    /** The record schema identifier; bump on incompatible changes. */
    public static final String SCHEMA = "v3-benchmark-client-result/2";

    private static final CompilationMXBean COMPILATION = ManagementFactory.getCompilationMXBean();

    private ClientMain() {
    }

    /** Parsed client arguments. */
    record Args(BenchmarkCell cell, URI endpoint, WarmupPolicy warmup, Duration measure, Path resultFile,
                Map<String, Path> models, int repetition, String label) {

        static Args parse(String[] argv) {
            BenchmarkCell cell = null;
            URI endpoint = null;
            WarmupPolicy.Kind kind = WarmupPolicy.Kind.FIXED;
            Duration fixedWarmup = Duration.ofSeconds(10);
            SteadyStatePolicy defaults = SteadyStatePolicy.RELEASE;
            Duration interval = defaults.interval();
            int window = defaults.window();
            Duration timeout = defaults.warmupTimeout();
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
                    case "--warmup-policy" -> kind = WarmupPolicy.Kind.fromId(v);
                    case "--warmup-seconds" -> fixedWarmup = seconds(v);
                    case "--sample-interval-seconds" -> interval = seconds(v);
                    case "--steady-window" -> window = Integer.parseInt(v);
                    case "--warmup-timeout-seconds" -> timeout = seconds(v);
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
            if (measure.isZero() || measure.isNegative() || fixedWarmup.isNegative()) {
                throw new IllegalArgumentException("measure must be positive and warmup non-negative");
            }
            SteadyStatePolicy sampling = new SteadyStatePolicy(interval, window, timeout);
            WarmupPolicy warmup = kind == WarmupPolicy.Kind.FIXED ? WarmupPolicy.fixed(fixedWarmup, sampling)
                                                                  : WarmupPolicy.steadyState(sampling);
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

            record.withMember("expected_requests_per_op", provider.fixture(cell).requestsPerOperation());

            Control control = () -> serverStats(args.endpoint());
            Workers pool = new Workers(workerPool("bench-caller-", cell.concurrency()), workerPool("bench-sampler-", 1));
            try {
                warmupAndMeasure(args, cell, workload, control, pool, record);
            } finally {
                pool.callers().shutdownNow();
                pool.sampler().shutdownNow();
            }
        }
    }

    /**
     * The threads both drives run on: {@code concurrency} sync callers, and one sampler. Warmup and measurement reuse
     * the very same threads, which matters for the measured window: a fresh thread starts with empty thread-local
     * caches (NIO temporary buffers, charset coders), whose first use takes branches the warmed-up compiled code never
     * saw and deoptimizes it. Async paths submit from the main thread, the same in both drives.
     */
    record Workers(ExecutorService callers, ExecutorService sampler) {
    }

    static ExecutorService workerPool(String prefix, int threads) {
        AtomicLong n = new AtomicLong();
        return Executors.newFixedThreadPool(threads, r -> {
            Thread t = new Thread(r, prefix + n.incrementAndGet());
            t.setDaemon(true);
            return t;
        });
    }

    private static void warmupAndMeasure(Args args, BenchmarkCell cell, CellWorkload workload, Control control,
                                         Workers pool, ObjectNode.Builder record) throws Exception {
        WarmupPolicy policy = args.warmup();
        SteadyStatePolicy sampling = policy.sampling();
        record.withMember("sampling", sampling.toNode());
        long warmupStart = System.nanoTime();
        Measurement warm;
        boolean passed;
        if (policy.kind() == WarmupPolicy.Kind.STEADY_STATE) {
            warm = drive(workload, cell.concurrency(), pool, sampling.warmupTimeout(), sampling.interval(), control,
                         samples -> SteadyStateDetector.closingWindow(SteadyStateDetector.intervals(samples),
                                                                      sampling).stable());
            passed = warm.stoppedByGate;
        } else {
            warm = drive(workload, cell.concurrency(), pool, policy.fixedDuration(), sampling.interval(), control,
                         null);
            passed = false;
        }
        long warmupWall = System.nanoTime() - warmupStart;
        // Nothing runs between warmup and measurement: the measured drive brackets its own counters, and the
        // warmup record (Smithy nodes, formatting) is built afterwards, so neither can trigger compilation or
        // deoptimization inside the measured window.
        Measurement m = drive(workload, cell.concurrency(), pool, args.measure(), sampling.interval(), control, null);
        Map<String, Long> serverBefore = m.serverAtStart;
        Map<String, Long> serverAfter = m.serverAtEnd;
        SteadyStateDetector.Sample first = m.samples.get(0);
        long cpuBefore = first.cpuNanos();
        long cpuAfter = m.end.cpuNanos();
        long allocBefore = first.allocatedBytes();
        long allocAfter = m.end.allocatedBytes();

        String reason = policy.kind() == WarmupPolicy.Kind.FIXED
                        ? "fixed-duration warmup: no steady-state gate (exploratory only, not evidence)"
                        : passed ? "stable window closed after " + (warm.samples.size() - 1) + " intervals"
                                 : "no stable window within " + sampling.warmupTimeout().toNanos() / 1e9 + " s";
        SteadyStateDetector.WindowCheck closing =
            SteadyStateDetector.closingWindow(SteadyStateDetector.intervals(warm.samples), sampling);
        record.withMember("warmup", ObjectNode.builder()
                                              .withMember("policy", policy.kind().id())
                                              .withMember("fixed_s", policy.fixedDuration().toNanos() / 1e9)
                                              .withMember("passed", passed)
                                              .withMember("reason", reason)
                                              .withMember("wall_ns", warmupWall)
                                              .withMember("operations", warm.operations)
                                              .withMember("failed_operations", warm.failures)
                                              .withMember("control_failures", warm.controlFailures)
                                              .withMember("closing_window", closing.toNode())
                                              .withMember("samples", SteadyStateDetector.toNode(warm.samples))
                                              .build());

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
            .withMember("gc_ms", m.gcAtEnd - m.gcAtStart)
            .withMember("control_failures", m.controlFailures)
            .withMember("p50_ns", m.latency.quantile(0.50))
            .withMember("p99_ns", m.latency.quantile(0.99))
            .withMember("loaded_classes", ManagementFactory.getClassLoadingMXBean().getLoadedClassCount())
            .withMember("configured_s", args.measure().toNanos() / 1e9)
            .withMember("stability", SteadyStateDetector.everyWindow(SteadyStateDetector.intervals(m.samples),
                                                                     sampling).toNode())
            .withMember("samples", SteadyStateDetector.toNode(m.samples));
        if (warm.firstFailure.get() != null) {
            record.withMember("first_warmup_failure", warm.firstFailure.get());
        }
        if (m.firstFailure.get() != null) {
            measurement.withMember("first_failure", m.firstFailure.get());
        }
        if (m.firstControlFailure != null) {
            measurement.withMember("first_control_failure", m.firstControlFailure);
        }
        record.withMember("measurement", measurement.build());
        record.withMember("service", server.build());
    }

    /** The outcome of one driven window. */
    static final class Measurement {
        final LatencyHistogram latency = new LatencyHistogram();
        final AtomicLong opCount = new AtomicLong();
        final AtomicLong failCount = new AtomicLong();
        final AtomicReference<String> firstFailure = new AtomicReference<>();
        /** Written by the sampler thread only; read after it is joined. */
        final List<SteadyStateDetector.Sample> samples = new ArrayList<>();
        volatile boolean stop;
        boolean stoppedByGate;
        /** Counters bracketing the drive: mock {@code /stats}, GC time, and the closing cumulative sample. */
        Map<String, Long> serverAtStart = Map.of();
        Map<String, Long> serverAtEnd = Map.of();
        long gcAtStart;
        long gcAtEnd;
        SteadyStateDetector.Sample end;
        /** Control-channel reads that failed (sampler thread, then main thread after the join). */
        int controlFailures;
        String firstControlFailure;
        long operations;
        long failures;
        long wallNanos;

        void fail(Throwable t) {
            failCount.incrementAndGet();
            firstFailure.compareAndSet(null, String.valueOf(t));
        }

        Map<String, Long> poll(Control control) {
            try {
                return control.stats();
            } catch (IOException | RuntimeException e) {
                if (controlFailures++ == 0) {
                    firstControlFailure = String.valueOf(e);
                }
                return Map.of();
            }
        }

        SteadyStateDetector.Sample sample(long start, boolean origin) {
            long t = System.nanoTime() - start;
            double load = systemCpuLoad();
            return new SteadyStateDetector.Sample(origin ? 0 : t, opCount.get(), failCount.get(), processCpuNanos(),
                                                  allocatedBytes(), compilationMillis(), origin ? -1 : load);
        }
    }

    /**
     * Closed-loop drive for at most {@code duration}: sync paths run {@code concurrency} caller threads; async paths
     * keep {@code concurrency} operations in flight from one submitting thread. Waits for every started operation.
     *
     * <p>Warmup and measurement run this identical code, latency recording and control-channel reads included, so
     * the code (and the JIT profile) the gate saw stabilize is the code that is measured; nothing else runs between
     * the two drives. The drive brackets itself: mock {@code /stats}, GC time and an origin sample before the first
     * operation, then a sampler thread takes one cumulative sample and one {@code /stats} read at every
     * {@code interval} boundary up to the deadline, then, after every operation completed, the closing sample and
     * {@code /stats}. When {@code stopWhen} is given and accepts the samples so far, the drive stops early
     * ({@link Measurement#stoppedByGate}).
     */
    static Measurement drive(CellWorkload workload, int concurrency, Workers pool, Duration duration,
                             Duration interval, Control control,
                             Predicate<List<SteadyStateDetector.Sample>> stopWhen)
            throws InterruptedException {
        Measurement m = new Measurement();
        m.serverAtStart = m.poll(control);
        m.gcAtStart = gcMillis();
        long start = System.nanoTime();
        long deadline = start + duration.toNanos();
        m.samples.add(m.sample(start, true));
        if (duration.isZero()) {
            m.end = m.samples.get(0);
            m.serverAtEnd = m.serverAtStart;
            m.gcAtEnd = m.gcAtStart;
            return m;
        }
        Future<?> sampler = pool.sampler().submit(() -> {
            for (long k = 1; ; k++) {
                long target = start + k * interval.toNanos();
                if (target > deadline) {
                    return;
                }
                long now;
                while ((now = System.nanoTime()) < target) {
                    LockSupport.parkNanos(target - now);
                }
                m.samples.add(m.sample(start, false));
                m.poll(control);
                if (stopWhen != null && stopWhen.test(m.samples)) {
                    m.stoppedByGate = true;
                    m.stop = true;
                    return;
                }
            }
        });
        if (workload.isAsync()) {
            Semaphore inflight = new Semaphore(concurrency);
            for (long now = System.nanoTime(); now < deadline && !m.stop; now = System.nanoTime()) {
                asyncSegment(workload, m, inflight, Math.min(deadline, now + SEGMENT_NANOS));
            }
            inflight.acquire(concurrency);
        } else {
            List<Future<?>> callers = new ArrayList<>(concurrency);
            for (int i = 0; i < concurrency; i++) {
                callers.add(pool.callers().submit(() -> {
                    for (long now = System.nanoTime(); now < deadline && !m.stop; now = System.nanoTime()) {
                        syncSegment(workload, m, Math.min(deadline, now + SEGMENT_NANOS));
                    }
                }));
            }
            for (Future<?> f : callers) {
                await(f);
            }
        }
        m.wallNanos = System.nanoTime() - start;
        await(sampler);
        m.end = m.sample(start, false);
        m.serverAtEnd = m.poll(control);
        m.gcAtEnd = gcMillis();
        m.operations = m.opCount.get();
        m.failures = m.failCount.get();
        return m;
    }

    private static void await(Future<?> f) throws InterruptedException {
        try {
            f.get();
        } catch (ExecutionException e) {
            throw new IllegalStateException("benchmark worker failed", e.getCause());
        }
    }

    /** The mock service's control channel. */
    @FunctionalInterface
    interface Control {
        Map<String, Long> stats() throws IOException;
    }

    /**
     * Length of one drive segment. The operation loop lives in {@link #syncSegment}/{@link #asyncSegment}, invoked
     * once per segment, so during warmup those methods are invoked thousands of times and get a standard JIT
     * compilation that the measured drive reuses. A loop entered only once per drive would instead run as an OSR
     * compilation tied to that one invocation, and the measured drive would recompile it at its start, inside the
     * measured window. For the same reason a segment ends only at its deadline: the gate's stop is checked between
     * segments, so ending warmup takes no branch the compiled segment has never taken (which would deoptimize it and
     * recompile it at the start of measurement).
     */
    static final long SEGMENT_NANOS = 10_000_000L;

    /** Blocking operations until {@code segmentEnd}. */
    static void syncSegment(CellWorkload workload, Measurement m, long segmentEnd) {
        while (System.nanoTime() < segmentEnd) {
            long s = System.nanoTime();
            try {
                workload.call();
                m.opCount.incrementAndGet();
                m.latency.record(System.nanoTime() - s);
            } catch (RuntimeException | Error e) {
                m.fail(e);
            }
        }
    }

    /** Asynchronous operations, at most {@code inflight} permits outstanding, until {@code segmentEnd}. */
    static void asyncSegment(CellWorkload workload, Measurement m, Semaphore inflight, long segmentEnd)
            throws InterruptedException {
        while (System.nanoTime() < segmentEnd) {
            inflight.acquire();
            long s = System.nanoTime();
            try {
                workload.callAsync().whenComplete((v, t) -> {
                    if (t != null) {
                        m.fail(t);
                    } else {
                        m.opCount.incrementAndGet();
                        m.latency.record(System.nanoTime() - s);
                    }
                    inflight.release();
                });
            } catch (RuntimeException e) {
                m.fail(e);
                inflight.release();
            }
        }
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

    /** Total JIT compilation time since JVM start (ms), or -1 when the JVM does not measure it. */
    static long compilationMillis() {
        return COMPILATION != null && COMPILATION.isCompilationTimeMonitoringSupported()
               ? COMPILATION.getTotalCompilationTime() : -1;
    }

    /** Whole-host CPU load since the previous call (0..1), or -1 when unavailable. */
    private static double systemCpuLoad() {
        double load = ((com.sun.management.OperatingSystemMXBean) ManagementFactory.getOperatingSystemMXBean())
            .getCpuLoad();
        return Double.isFinite(load) && load >= 0 ? load : -1;
    }

    private static long gcMillis() {
        long total = 0;
        for (var gc : ManagementFactory.getGarbageCollectorMXBeans()) {
            total += Math.max(0, gc.getCollectionTime());
        }
        return total;
    }

    /**
     * The mock service's {@code /stats} counters, over one plain-socket HTTP/1.1 request. Deliberately not the JDK
     * {@code HttpClient}: this runs right before and after the measured window, and a full HTTP client there would
     * leave selector threads alive and queue JIT compilations into the window.
     */
    static Map<String, Long> serverStats(URI endpoint) throws IOException {
        String response;
        try (Socket socket = new Socket(endpoint.getHost(), endpoint.getPort())) {
            socket.setSoTimeout(5_000);
            socket.getOutputStream().write(("GET /stats HTTP/1.1\r\nHost: " + endpoint.getHost() + ":"
                                            + endpoint.getPort() + "\r\nConnection: close\r\n\r\n")
                                               .getBytes(StandardCharsets.US_ASCII));
            socket.getOutputStream().flush();
            response = new String(socket.getInputStream().readAllBytes(), StandardCharsets.US_ASCII);
        }
        int headerEnd = response.indexOf("\r\n\r\n");
        if (!response.startsWith("HTTP/1.1 200") || headerEnd < 0) {
            throw new IOException("bad /stats response: " + response.lines().findFirst().orElse(""));
        }
        Map<String, Long> stats = new LinkedHashMap<>();
        for (String line : response.substring(headerEnd + 4).split("\n")) {
            int eq = line.indexOf('=');
            if (eq > 0) {
                stats.put(line.substring(0, eq), Long.parseLong(line.substring(eq + 1).trim()));
            }
        }
        return stats;
    }
}
