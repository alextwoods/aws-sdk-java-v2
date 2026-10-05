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

package software.amazon.awssdk.benchmark.dynamodb;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Fixed-iteration V3 DynamoDB benchmark runner, ported from racecar's {@code BenchmarkRunner}: one arm per JVM,
 * talking plain HTTP over loopback to an out-of-process {@link MockDdbServer}.
 *
 * <p>Every arm is verified ({@link TransportCheck}) before anything is timed; {@code --no-verify} exists only for
 * endpoints that are not the V3 mock server. Output is racecar's {@code RESULT} line and CSV column layout, followed
 * by V3 columns (scope, transport identity, allocation, server bytes, verification, repetition bookkeeping).
 */
public final class BenchmarkRunner {

    private static final String USAGE = """
        Usage: BenchmarkRunner --arm <arm> [options]

          --arm X               v3-sync-apache5 | v3-sync-apache4 | v3-sync-urlconnection | v3-sync-crt |
                                v3-async-netty | v3-async-crt | v3-async-apache5   (required; --client is an alias)
                                Scope of every arm: serde+transport (no client engine, signer, endpoints,
                                retries or interceptors yet).
          --scenario X[,Y...]   small-get, small-put, batch-get, batch-put, or all (default: all)
          --iterations N        measured operations per scenario (default: 10000)
          --warmup N            warmup operations per scenario (default: min(2000, iterations))
          --warmup-mode X       quiesce | fixed (default: quiesce)
          --warmup-max-seconds N  ceiling on quiesce warmup (default: 60)
          --concurrency N       sync: N caller threads; async: N in flight from one thread (default: 1)
          --async-mode X        inflight | join (default: inflight), async arms only
          --endpoint URL        server endpoint (default: http://127.0.0.1:19080)
          --progress-seconds N  progress interval, 0 disables (default: 10, minimum 10)
          --cpu-source X        auto | procfs | mxbean (default: auto)
          --append-to-results-file PATH   append one CSV row per RESULT line
          --repetition N / --position N   recorded in the results row (set by paired.sh)
          --label NAME          value of the client column (default: the arm's CLI name); paired.sh's null experiment
                                runs one arm twice as ARM and ARM#null. The arm column always holds the real arm.
          --verify-only         run the pre-timing verification, print DIGEST lines, exit
          --no-verify           skip verification (only for endpoints that are not the V3 mock server)
        """;

    private static final int MIN_PROGRESS_SECONDS = 10;

    private BenchmarkRunner() {
    }

    public static void main(String[] args) throws Exception {
        Arm arm = null;
        List<Scenario> scenarios = List.of(Scenario.values());
        int iterations = 10_000;
        int warmup = -1;
        URI endpoint = URI.create("http://127.0.0.1:" + MockDdbServer.DEFAULT_PORT);
        int progressSeconds = MIN_PROGRESS_SECONDS;
        String cpuSourceName = "auto";
        Path resultsFile = null;
        int concurrency = 1;
        String asyncModeName = "inflight";
        String warmupModeName = "quiesce";
        int warmupMaxSeconds = 60;
        boolean verify = true;
        boolean verifyOnly = false;
        int repetition = 0;
        int position = 0;
        String label = null;
        for (int i = 0; i < args.length; i++) {
            switch (args[i]) {
                case "--arm", "--client" -> arm = Arm.fromCli(args[++i]);
                case "--scenario" -> {
                    String v = args[++i];
                    if (!"all".equals(v)) {
                        List<Scenario> selected = new ArrayList<>();
                        for (String name : v.split(",")) {
                            selected.add(Scenario.fromCli(name.trim()));
                        }
                        scenarios = selected;
                    }
                }
                case "--iterations" -> iterations = Integer.parseInt(args[++i]);
                case "--warmup" -> warmup = Integer.parseInt(args[++i]);
                case "--warmup-mode" -> warmupModeName = args[++i];
                case "--warmup-max-seconds" -> warmupMaxSeconds = Integer.parseInt(args[++i]);
                case "--concurrency" -> concurrency = Integer.parseInt(args[++i]);
                case "--async-mode" -> asyncModeName = args[++i];
                case "--endpoint" -> endpoint = URI.create(args[++i]);
                case "--progress-seconds" -> progressSeconds = Integer.parseInt(args[++i]);
                case "--cpu-source" -> cpuSourceName = args[++i];
                case "--append-to-results-file" -> resultsFile = Path.of(args[++i]);
                case "--repetition" -> repetition = Integer.parseInt(args[++i]);
                case "--position" -> position = Integer.parseInt(args[++i]);
                case "--label" -> label = args[++i];
                case "--verify-only" -> verifyOnly = true;
                case "--no-verify" -> verify = false;
                case "--help" -> {
                    System.out.print(USAGE);
                    return;
                }
                default -> usage("Unknown argument: " + args[i]);
            }
        }
        if (arm == null) {
            usage("--arm is required");
        }
        if (label == null || label.isBlank()) {
            label = arm.cliName;
        } else if (!label.matches("[A-Za-z0-9._#+-]+")) {
            usage("--label must match [A-Za-z0-9._#+-]+ (it is written unquoted into the CSV)");
        }
        if (warmup < 0) {
            warmup = Math.min(2000, iterations);
        }
        if (concurrency < 1) {
            usage("--concurrency must be >= 1");
        }
        if (!"inflight".equals(asyncModeName) && !"join".equals(asyncModeName)) {
            usage("--async-mode must be inflight or join");
        }
        if (!"quiesce".equals(warmupModeName) && !"fixed".equals(warmupModeName)) {
            usage("--warmup-mode must be quiesce or fixed");
        }
        if (progressSeconds > 0 && progressSeconds < MIN_PROGRESS_SECONDS) {
            progressSeconds = MIN_PROGRESS_SECONDS;
        }

        CpuTimeSource cpuSource = CpuTimeSources.bind(cpuSourceName);
        waitForServer(endpoint);
        ServerStats.prewarm(endpoint);

        long suiteStart = System.nanoTime();
        try (V3Workload workload = new V3Workload(arm, endpoint, concurrency)) {
            Driver.Mode mode = workload.supportsAsync() && "inflight".equals(asyncModeName)
                               ? Driver.Mode.INFLIGHT : Driver.Mode.BLOCKING;
            String asyncMode = workload.supportsAsync() ? (mode == Driver.Mode.INFLIGHT ? "inflight" : "join") : "n/a";
            System.out.printf("=== v3-benchmark arm=%s label=%s scope=%s transport=%s class=%s clientName=%s mode=%s"
                              + " scenarios=%s iterations=%,d warmup=%,d/%s concurrency=%d driver=%s async-mode=%s"
                              + " endpoint=%s pid=%d%n",
                              arm.cliName, arm.honestLabel(), Arm.SCOPE, arm.transportLabel(),
                              workload.transport.getClass().getName(), workload.clientName(), arm.mode,
                              scenarios.stream().map(s -> s.cliName).collect(Collectors.joining(",")), iterations,
                              warmup, warmupModeName, concurrency,
                              mode == Driver.Mode.INFLIGHT ? "in-flight" : "threads", asyncMode, endpoint,
                              ProcessHandle.current().pid());
            System.out.printf("=== scope: %s%n", Arm.SCOPE_DETAIL);
            System.out.printf("=== cpu-time source: %s user/system split: %s; application cpu (per-thread): %s;"
                              + " allocation (per-thread): %s%n",
                              cpuSource.name(), cpuSource.hasUserSystemSplit() ? "yes" : "no",
                              AppCpuMeter.supported() ? "yes" : "UNAVAILABLE",
                              AppCpuMeter.allocationSupported() ? "yes" : "UNAVAILABLE");
            System.out.printf("=== build: %s%n", BuildProvenance.summary());
            System.out.printf("=== host: %s%n", BuildProvenance.host());

            Map<Scenario, String> digests = Map.of();
            if (verify) {
                digests = TransportCheck.verify(workload, scenarios);
                for (Map.Entry<Scenario, String> e : digests.entrySet()) {
                    System.out.printf("DIGEST arm=%s scenario=%s sha256=%s%n", arm.cliName, e.getKey().cliName,
                                      e.getValue());
                }
            } else {
                System.out.println("=== WARNING verification skipped (--no-verify): results are not trusted");
            }
            if (verifyOnly) {
                return;
            }
            RunContext ctx = new RunContext(workload, arm, iterations, warmup,
                                            "quiesce".equals(warmupModeName) ? Warmup.Mode.QUIESCE : Warmup.Mode.FIXED,
                                            warmupMaxSeconds, concurrency, mode, asyncMode, progressSeconds, cpuSource,
                                            endpoint, resultsFile, repetition, position, label);
            for (Scenario scenario : scenarios) {
                runScenario(ctx, scenario, digests.getOrDefault(scenario, ""));
            }
        }
        System.out.printf("DONE total_wall_s=%.1f%n", (System.nanoTime() - suiteStart) / 1e9);
    }

    private record RunContext(V3Workload workload, Arm arm, int iterations, int warmup, Warmup.Mode warmupMode,
                              int warmupMaxSeconds, int concurrency, Driver.Mode mode, String asyncMode,
                              int progressSeconds, CpuTimeSource cpuSource, URI endpoint, Path resultsFile,
                              int repetition, int position, String label) {
    }

    private static void runScenario(RunContext c, Scenario scenario, String digest) throws Exception {
        Warmup.Result warmup = Warmup.run(c.workload, scenario, c.mode, c.warmupMode, c.warmup, c.concurrency,
                                          c.warmupMaxSeconds);
        System.out.printf("=== warmup %s: %s%n", scenario.cliName, warmup.summary());
        if (!warmup.quiesced() && c.warmupMode == Warmup.Mode.QUIESCE) {
            System.out.printf("=== WARNING %s: JVM had not settled; per-operation CPU from this run is not"
                              + " steady-state%n", scenario.cliName);
        }

        Driver.Progress progress = new Driver.Progress(c.mode == Driver.Mode.INFLIGHT ? 1 : c.concurrency);
        ServerStats serverBefore = ServerStats.fetch(c.endpoint);
        long compileBefore = JvmActivity.compilationMillis();
        JvmActivity.Gc gcBefore = JvmActivity.gc();
        long allocBefore = AppCpuMeter.snapshotAllocatedBytes();
        long appCpuBefore = AppCpuMeter.snapshotNanos();
        CpuTimeSource.Snapshot cpuBefore = c.cpuSource.snapshot();
        long start = System.nanoTime();
        Thread reporter = startProgressReporter(scenario, c.iterations, c.progressSeconds, progress, start);

        Driver.Samples samples = Driver.run(c.workload, scenario, c.mode, c.iterations, c.concurrency, progress);

        long wallNanos = System.nanoTime() - start;
        CpuTimeSource.Snapshot cpu = c.cpuSource.snapshot().minus(cpuBefore);
        long appCpuNanos = appCpuBefore < 0 ? -1 : AppCpuMeter.snapshotNanos() - appCpuBefore;
        long allocBytes = allocBefore < 0 ? -1 : AppCpuMeter.snapshotAllocatedBytes() - allocBefore;
        JvmActivity.Gc gc = JvmActivity.gc().minus(gcBefore);
        long compileMillis = compileBefore < 0 ? -1 : JvmActivity.compilationMillis() - compileBefore;
        ServerStats server = ServerStats.fetch(c.endpoint).minus(serverBefore);
        if (reporter != null) {
            reporter.interrupt();
            reporter.join();
        }
        Driver.LatencyStats latency = samples.stats();

        boolean steadyState = compileMillis >= 0 && compileMillis <= Math.max(50L, (long) (wallNanos / 1e6 * 0.02));
        if (compileMillis > 0 && !steadyState) {
            System.out.printf(Locale.US, "=== WARNING %s: %d ms of JIT compilation inside the measured window"
                                         + " (%.1f%% of wall); not steady-state%s%n",
                              scenario.cliName, compileMillis, compileMillis / (wallNanos / 1e6) * 100,
                              wallNanos < 5_000_000_000L ? "; the window is under 5s, raise --iterations" : "");
        }
        boolean complete = samples.count() == c.iterations;
        boolean serverAgrees = !server.unavailable && server.requests == c.iterations && server.errors == 0;
        boolean verified = !digest.isEmpty();
        // Req 20.27: a failed operation, incomplete sample or unverified arm invalidates the repetition. A JIT-unsettled
        // window is reported separately (steady_state) as in racecar.
        boolean valid = complete && serverAgrees && verified;

        Row r = new Row(c, scenario, wallNanos, cpu, appCpuNanos, allocBytes, gc, compileMillis, server, latency,
                        steadyState, warmup, verified, digest, valid);
        System.out.println(r.resultLine());
        if (c.resultsFile != null) {
            appendCsv(c.resultsFile, r.csvRow());
        }
    }

    static final String CSV_HEADER =
        // racecar's columns, in racecar's order ...
        "client,scenario,iterations,wall_ms,ops_per_wall_sec,cpu_ms,cpu_user_ms,cpu_sys_ms,"
        + "ops_per_cpu_sec,ops_per_user_cpu_sec,avg_us_per_op,phase,commit,sdk_commit,"
        + "concurrency,async_mode,transport,cpu_us_per_op,mean_lat_us,p50_us,p90_us,p99_us,p999_us,"
        + "max_us,server_cpu_ms,server_requests,server_saturated,"
        + "app_cpu_ms,app_cpu_us_per_op,unattributed_cpu_ms,jit_ms,gc_ms,gc_count,steady_state,"
        + "warmup_ops,warmup_settled,"
        // ... then V3 columns.
        + "arm_label,arm_scope,transport_class,transport_client_name,transport_mode,alloc_bytes,alloc_bytes_per_op,"
        + "server_bytes_in,server_bytes_out,verified,response_sha256,valid,dirty_files,harness,repetition,position,arm";

    private record Row(RunContext c, Scenario scenario, long wallNanos, CpuTimeSource.Snapshot cpu, long appCpuNanos,
                       long allocBytes, JvmActivity.Gc gc, long compileMillis, ServerStats server,
                       Driver.LatencyStats latency, boolean steadyState, Warmup.Result warmup, boolean verified,
                       String digest, boolean valid) {

        double wallSec() {
            return wallNanos / 1e9;
        }

        int n() {
            return c.iterations;
        }

        String resultLine() {
            double cpuSec = cpu.totalNanos() / 1e9;
            String split = cpu.hasSplit()
                           ? String.format(Locale.US, " cpu_user_ms=%.0f cpu_sys_ms=%.0f", cpu.userNanos() / 1e6,
                                           cpu.systemNanos() / 1e6)
                           : "";
            String app = appCpuNanos < 0 ? " app_cpu_ms=unavailable"
                         : String.format(Locale.US, " app_cpu_ms=%.0f app_cpu_us_per_op=%.1f unattributed_ms=%.0f",
                                         appCpuNanos / 1e6, appCpuNanos / 1e3 / n(),
                                         (cpu.totalNanos() - appCpuNanos) / 1e6);
            String alloc = allocBytes < 0 ? " alloc_bytes_per_op=unavailable"
                           : String.format(Locale.US, " alloc_bytes_per_op=%.0f", (double) allocBytes / n());
            return String.format(Locale.US,
                                 "RESULT client=%s scope=%s transport=%s scenario=%s iterations=%d concurrency=%d"
                                 + " async_mode=%s wall_ms=%.0f ops_per_wall_sec=%.1f cpu_ms=%.0f%s"
                                 + " ops_per_cpu_sec=%.1f cpu_us_per_op=%.1f%s%s avg_us_per_op=%.1f"
                                 + " mean_lat_us=%.1f p50_us=%.1f p90_us=%.1f p99_us=%.1f p999_us=%.1f max_us=%.1f"
                                 + " jit_ms=%d gc_ms=%d gc_count=%d steady_state=%s warmup_ops=%d"
                                 + " warmup_settled=%s%s verified=%s valid=%s",
                                 c.label, Arm.SCOPE, c.arm.transportLabel(), scenario.cliName, n(),
                                 c.concurrency, c.asyncMode, wallSec() * 1e3, n() / wallSec(), cpuSec * 1e3, split,
                                 cpuSec > 0 ? n() / cpuSec : 0.0, cpu.totalNanos() / 1e3 / n(), app, alloc,
                                 wallNanos / 1e3 / n(), latency.meanUs(), latency.p50Us(), latency.p90Us(),
                                 latency.p99Us(), latency.p999Us(), latency.maxUs(), compileMillis, gc.millis(),
                                 gc.count(), steadyState, warmup.ops(), warmup.quiesced(), server.summary(n()),
                                 verified, valid);
        }

        String csvRow() {
            double cpuSec = cpu.totalNanos() / 1e9;
            boolean split = cpu.hasSplit();
            double userSec = split ? cpu.userNanos() / 1e9 : 0;
            List<Object> cols = new ArrayList<>();
            cols.add(c.label);
            cols.add(scenario.cliName);
            cols.add(n());
            cols.add(f0(wallSec() * 1e3));
            cols.add(f1(n() / wallSec()));
            cols.add(f0(cpuSec * 1e3));
            cols.add(split ? f0(cpu.userNanos() / 1e6) : "");
            cols.add(split ? f0(cpu.systemNanos() / 1e6) : "");
            cols.add(f1(cpuSec > 0 ? n() / cpuSec : 0));
            cols.add(split && userSec > 0 ? f1(n() / userSec) : "");
            cols.add(f1(wallNanos / 1e3 / n()));
            cols.add(BuildProvenance.phase());
            cols.add(BuildProvenance.commit());
            cols.add(BuildProvenance.sdk());
            cols.add(c.concurrency);
            cols.add(c.asyncMode);
            cols.add(c.arm.transportLabel());
            cols.add(f1(cpu.totalNanos() / 1e3 / n()));
            cols.add(f1(latency.meanUs()));
            cols.add(f1(latency.p50Us()));
            cols.add(f1(latency.p90Us()));
            cols.add(f1(latency.p99Us()));
            cols.add(f1(latency.p999Us()));
            cols.add(f1(latency.maxUs()));
            cols.add(server.unavailable ? "" : server.cpuNanos / 1_000_000L);
            cols.add(server.unavailable ? "" : server.requests);
            cols.add(server.unavailable ? "" : server.saturated());
            cols.add(appCpuNanos < 0 ? "" : f0(appCpuNanos / 1e6));
            cols.add(appCpuNanos < 0 ? "" : f1(appCpuNanos / 1e3 / n()));
            cols.add(appCpuNanos < 0 ? "" : f0((cpu.totalNanos() - appCpuNanos) / 1e6));
            cols.add(compileMillis < 0 ? "" : compileMillis);
            cols.add(gc.millis());
            cols.add(gc.count());
            cols.add(steadyState);
            cols.add(warmup.ops());
            cols.add(warmup.quiesced());
            cols.add(c.arm.honestLabel());
            cols.add(Arm.SCOPE);
            cols.add(c.workload.transport.getClass().getName());
            cols.add(c.workload.clientName());
            cols.add(c.arm.mode);
            cols.add(allocBytes < 0 ? "" : allocBytes);
            cols.add(allocBytes < 0 ? "" : f0((double) allocBytes / n()));
            cols.add(server.unavailable ? "" : server.bytesIn);
            cols.add(server.unavailable ? "" : server.bytesOut);
            cols.add(verified);
            cols.add(digest);
            cols.add(valid);
            cols.add(BuildProvenance.dirty());
            cols.add(BuildProvenance.harness());
            cols.add(c.repetition);
            cols.add(c.position);
            cols.add(c.arm.cliName);
            return cols.stream().map(String::valueOf).collect(Collectors.joining(","));
        }
    }

    private static String f0(double v) {
        return String.format(Locale.US, "%.0f", v);
    }

    private static String f1(double v) {
        return String.format(Locale.US, "%.1f", v);
    }

    private static void appendCsv(Path file, String row) throws IOException {
        boolean needsHeader = !Files.exists(file) || Files.size(file) == 0;
        Files.writeString(file, needsHeader ? CSV_HEADER + "\n" + row + "\n" : row + "\n",
                          StandardOpenOption.CREATE, StandardOpenOption.APPEND);
    }

    private static Thread startProgressReporter(Scenario scenario, int iterations, int progressSeconds,
                                                Driver.Progress progress, long start) {
        if (progressSeconds <= 0) {
            return null;
        }
        Thread t = new Thread(() -> {
            try {
                while (!Thread.currentThread().isInterrupted()) {
                    Thread.sleep(progressSeconds * 1000L);
                    long done = progress.total();
                    double elapsed = (System.nanoTime() - start) / 1e9;
                    double rate = elapsed > 0 ? done / elapsed : 0;
                    System.out.printf(Locale.US, "progress %s %,d/%,d (%.1f%%) %.0f ops/s eta %.0fs%n",
                                      scenario.cliName, done, iterations, 100.0 * done / iterations, rate,
                                      rate > 0 ? (iterations - done) / rate : 0);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } finally {
                AppCpuMeter.retireCurrentThread();
            }
        }, "bench-progress");
        t.setDaemon(true);
        t.start();
        return t;
    }

    private static void waitForServer(URI endpoint) throws InterruptedException {
        HttpClient http = HttpClient.newBuilder().version(HttpClient.Version.HTTP_1_1)
                                    .connectTimeout(Duration.ofSeconds(1)).build();
        long deadline = System.nanoTime() + 30_000_000_000L;
        while (true) {
            try {
                HttpResponse<Void> resp = http.send(HttpRequest.newBuilder(endpoint.resolve("/ping")).GET().build(),
                                                    HttpResponse.BodyHandlers.discarding());
                if (resp.statusCode() == 200) {
                    return;
                }
            } catch (IOException e) {
                // not up yet
            }
            if (System.nanoTime() > deadline) {
                throw new IllegalStateException("server at " + endpoint + " did not become ready within 30s");
            }
            Thread.sleep(100);
        }
    }

    private static void usage(String message) {
        System.err.println(message);
        System.err.print(USAGE);
        System.exit(2);
    }
}
