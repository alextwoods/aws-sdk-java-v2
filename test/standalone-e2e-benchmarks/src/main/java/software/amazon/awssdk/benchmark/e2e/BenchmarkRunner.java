package software.amazon.awssdk.benchmark.e2e;

import java.io.IOException;
import java.io.PrintStream;
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
import java.util.concurrent.CompletableFuture;

/**
 * Fixed-iteration DynamoDB benchmark runner. Talks to an out-of-process {@link MockDdbServer}
 * (or DynamoDB Local) over plain HTTP and reports wall-clock and process-CPU throughput.
 *
 * <p>See README.md for scenarios, CLI reference and caveats. Run with {@code --help} for usage.
 */
public final class BenchmarkRunner {

    private static final String USAGE = """
        Usage: BenchmarkRunner --client <v1|v2-sync|v2-async|smithy> [options]

        Options:
          --client X            SDK under test (required):
                                  v1        V1, Apache 4.x
                                  v2-sync   V2 sync, Apache5
                                  v2-async  V2 async, CRT
                                  smithy    smithy-java, HTTP/1.1
          --scenario X[,Y...]   small-get, small-put, batch-get, batch-put, or all (default: all)
          --iterations N        measured operations per scenario (default: 10000)
          --warmup N            warmup operations per scenario, unmeasured (default: min(2000, iterations))
          --warmup-mode X       quiesce | fixed (default: quiesce). `quiesce` runs --warmup and then
                                keeps going until JIT compilation stops for 2s, so the measured
                                window is steady-state rather than a guess. `fixed` stops at --warmup
                                exactly, reproducing older collections.
          --warmup-max-seconds N
                                ceiling on warmup wall time in quiesce mode (default: 60)
          --concurrency N       operations kept in flight (default: 2). Sync clients use N threads;
                                async clients keep N outstanding from one submitting thread. Every
                                client's connection pool is sized to exactly N.
          --async-mode X        inflight | join (default: inflight). Applies to async clients only.
                                `inflight` keeps N operations outstanding from one submitting thread,
                                which is the shape async exists for. `join` drives the async client
                                the way a blocking one is driven — N threads, each blocking on its own
                                future — which is what earlier collections measured. Comparing the two
                                separates the programming model from the transport.
          --endpoint URL        server endpoint (default: http://127.0.0.1:19080)
          --metrics             collect SDK-internal metrics and print a per-scenario summary
          --metrics-file PATH   write metric summaries to PATH instead of stdout (implies --metrics)
          --progress-seconds N  progress/ETA print interval, 0 disables (default: 10). Values
                                between 1 and 9 are raised to 10: progress output is for watching a
                                long run, and a collection's logs are worth keeping small.
          --cpu-source X        auto, oshi, procfs, mxbean (default: auto; probes in that order)
          --append-to-results-file PATH
                                append one CSV row per RESULT line to PATH; the file (and a
                                header row) is created if it does not exist
          --help                print this message
        """;

    enum Scenario {
        SMALL_GET("small-get") {
            void run(Workloads.Workload w) throws Exception {
                w.smallGet();
            }

            CompletableFuture<?> runAsync(Workloads.Workload w) {
                return w.smallGetAsync();
            }
        },
        SMALL_PUT("small-put") {
            void run(Workloads.Workload w) throws Exception {
                w.smallPut();
            }

            CompletableFuture<?> runAsync(Workloads.Workload w) {
                return w.smallPutAsync();
            }
        },
        BATCH_GET("batch-get") {
            void run(Workloads.Workload w) throws Exception {
                w.batchGet();
            }

            CompletableFuture<?> runAsync(Workloads.Workload w) {
                return w.batchGetAsync();
            }
        },
        BATCH_PUT("batch-put") {
            void run(Workloads.Workload w) throws Exception {
                w.batchPut();
            }

            CompletableFuture<?> runAsync(Workloads.Workload w) {
                return w.batchPutAsync();
            }
        };

        final String cliName;

        Scenario(String cliName) {
            this.cliName = cliName;
        }

        abstract void run(Workloads.Workload w) throws Exception;

        abstract CompletableFuture<?> runAsync(Workloads.Workload w);

        static Scenario fromCli(String name) {
            for (Scenario s : values()) {
                if (s.cliName.equals(name)) {
                    return s;
                }
            }
            throw new IllegalArgumentException("unknown scenario: " + name);
        }
    }

    /**
     * Floor for the progress interval, and its default. Progress output exists to watch a long run,
     * not to log the run: a full collection is hundreds of JVM invocations, and anything printed per
     * few seconds across all of them adds up to log files nobody reads. {@code --progress-seconds 0}
     * turns it off entirely, which is what {@code collect.sh} does.
     */
    private static final int MIN_PROGRESS_SECONDS = 10;

    /**
     * Operations kept in flight by default. Two, chosen from a sweep rather than picked: it roughly
     * doubles samples per second of wall clock, every client stays steady-state, and total CPU demand
     * stays near two cores so neither the host nor the mock server is anywhere near its limit. Four
     * and above is where the async client stops settling and where throughput starts running into the
     * loopback ceiling (~48k ops/s here), at which point the run is measuring the server.
     */
    private static final int DEFAULT_CONCURRENCY = 2;

    private BenchmarkRunner() {
    }

    public static void main(String[] args) throws Exception {
        String client = null;
        List<Scenario> scenarios = List.of(Scenario.values());
        int iterations = 10_000;
        int warmup = -1;
        URI endpoint = URI.create("http://127.0.0.1:" + MockDdbServer.DEFAULT_PORT);
        boolean metrics = false;
        Path metricsFile = null;
        int progressSeconds = MIN_PROGRESS_SECONDS;
        String cpuSourceName = "auto";
        Path resultsFile = null;
        int concurrency = DEFAULT_CONCURRENCY;
        String asyncModeName = "inflight";
        String warmupModeName = "quiesce";
        int warmupMaxSeconds = 60;

        for (int i = 0; i < args.length; i++) {
            switch (args[i]) {
                case "--client":
                    client = args[++i];
                    break;
                case "--scenario":
                    String v = args[++i];
                    if (!"all".equals(v)) {
                        List<Scenario> selected = new ArrayList<>();
                        for (String name : v.split(",")) {
                            selected.add(Scenario.fromCli(name.trim()));
                        }
                        scenarios = selected;
                    }
                    break;
                case "--iterations":
                    iterations = Integer.parseInt(args[++i]);
                    break;
                case "--warmup":
                    warmup = Integer.parseInt(args[++i]);
                    break;
                case "--warmup-mode":
                    warmupModeName = args[++i];
                    break;
                case "--warmup-max-seconds":
                    warmupMaxSeconds = Integer.parseInt(args[++i]);
                    break;
                case "--concurrency":
                    concurrency = Integer.parseInt(args[++i]);
                    break;
                case "--async-mode":
                    asyncModeName = args[++i];
                    break;
                case "--endpoint":
                    endpoint = URI.create(args[++i]);
                    break;
                case "--metrics":
                    metrics = true;
                    break;
                case "--metrics-file":
                    metricsFile = Path.of(args[++i]);
                    metrics = true;
                    break;
                case "--progress-seconds":
                    progressSeconds = Integer.parseInt(args[++i]);
                    break;
                case "--cpu-source":
                    cpuSourceName = args[++i];
                    break;
                case "--append-to-results-file":
                    resultsFile = Path.of(args[++i]);
                    break;
                case "--help":
                    System.out.print(USAGE);
                    return;
                default:
                    System.err.println("Unknown argument: " + args[i]);
                    System.err.print(USAGE);
                    System.exit(2);
            }
        }
        if (client == null) {
            System.err.println("--client is required");
            System.err.print(USAGE);
            System.exit(2);
        }
        if (warmup < 0) {
            warmup = Math.min(2000, iterations);
        }
        if (concurrency < 1) {
            System.err.println("--concurrency must be >= 1");
            System.exit(2);
        }
        if (!"inflight".equals(asyncModeName) && !"join".equals(asyncModeName)) {
            System.err.println("--async-mode must be inflight or join");
            System.exit(2);
        }
        if (!"quiesce".equals(warmupModeName) && !"fixed".equals(warmupModeName)) {
            System.err.println("--warmup-mode must be quiesce or fixed");
            System.exit(2);
        }
        if (progressSeconds > 0 && progressSeconds < MIN_PROGRESS_SECONDS) {
            System.err.printf("note: raising --progress-seconds %d to %d; use 0 to disable%n",
                              progressSeconds, MIN_PROGRESS_SECONDS);
            progressSeconds = MIN_PROGRESS_SECONDS;
        }

        CpuTimeSource cpuSource = CpuTimeSources.bind(cpuSourceName);
        waitForServer(endpoint);
        // Get the stats path's class loading and compilation out of the way here, not on the boundary
        // of a measured window.
        ServerStats.prewarm(endpoint);

        PrintStream metricsOut = metricsFile == null
                                 ? System.out
                                 : new PrintStream(Files.newOutputStream(metricsFile), true);

        long suiteStart = System.nanoTime();
        try (Workloads.Workload workload = Workloads.create(client, endpoint, metrics, concurrency)) {
            // A blocking client can only hold N operations in flight with N threads; an async client
            // in in-flight mode does it from one. `join` puts the async client on the blocking driver
            // instead — N threads each blocking on its own future — which is what earlier collections
            // measured, and which is a property of how the client is *used*, not of the transport.
            Driver.Mode mode = workload.supportsAsync() && "inflight".equals(asyncModeName)
                               ? Driver.Mode.INFLIGHT
                               : Driver.Mode.BLOCKING;
            String effectiveAsyncMode = workload.supportsAsync()
                                        ? (mode == Driver.Mode.INFLIGHT ? "inflight" : "join")
                                        : "n/a";

            Warmup.Mode warmupMode = "quiesce".equals(warmupModeName)
                                     ? Warmup.Mode.QUIESCE
                                     : Warmup.Mode.FIXED;

            System.out.printf("=== standalone-e2e-benchmark client=%s transport=%s scenarios=%s"
                              + " iterations=%,d warmup=%,d/%s concurrency=%d driver=%s async-mode=%s"
                              + " endpoint=%s metrics=%s pid=%d%n",
                              client, workload.transport(), names(scenarios), iterations, warmup,
                              warmupModeName, concurrency,
                              mode == Driver.Mode.INFLIGHT ? "in-flight" : "threads",
                              effectiveAsyncMode, endpoint,
                              metrics ? (metricsFile == null ? "on" : "on -> " + metricsFile) : "off",
                              ProcessHandle.current().pid());
            System.out.printf("=== cpu-time source: %s user/system split: %s;"
                              + " application cpu (per-thread): %s%n",
                              cpuSource.name(), cpuSource.hasUserSystemSplit() ? "yes" : "no",
                              AppCpuMeter.supported() ? "yes" : "UNAVAILABLE");
            System.out.printf("=== build: %s%n", BuildProvenance.get().summary());

            for (Scenario scenario : scenarios) {
                runScenario(workload, client, scenario, iterations, warmup, warmupMode,
                            warmupMaxSeconds, concurrency, mode, effectiveAsyncMode, progressSeconds,
                            metrics, metricsOut, cpuSource, endpoint, resultsFile);
            }
        }
        System.out.printf("DONE total_wall_s=%.1f%n", (System.nanoTime() - suiteStart) / 1e9);
        if (metricsFile != null) {
            metricsOut.close();
        }
    }

    private static void runScenario(Workloads.Workload workload, String client, Scenario scenario,
                                    int iterations, int warmup, Warmup.Mode warmupMode,
                                    int warmupMaxSeconds, int concurrency, Driver.Mode mode,
                                    String asyncMode, int progressSeconds, boolean metrics,
                                    PrintStream metricsOut, CpuTimeSource cpuSource, URI endpoint,
                                    Path resultsFile) throws Exception {
        // Warm up in the same shape as the measurement: at concurrency N the pipeline has to warm N
        // threads (or N in-flight slots) and fill an N-connection pool, and a single-threaded warmup
        // would leave that work inside the measured window. In quiesce mode this also keeps going
        // until the JIT stops compiling, so the window measures steady-state code.
        Warmup.Result warmupResult =
            Warmup.run(workload, scenario, mode, warmupMode, warmup, concurrency, warmupMaxSeconds);
        System.out.printf("=== warmup %s: %s%n", scenario.cliName, warmupResult.summary());
        if (!warmupResult.quiesced() && warmupMode == Warmup.Mode.QUIESCE) {
            System.out.printf("=== WARNING %s: JVM had not settled; per-operation CPU from this run"
                              + " is not steady-state%n", scenario.cliName);
        }
        workload.resetMetrics();

        // The progress thread reads worker-local counters, so nothing about reporting touches the
        // measured path.
        Driver.Progress progress = new Driver.Progress(mode == Driver.Mode.INFLIGHT ? 1 : concurrency);
        ServerStats serverBefore = ServerStats.fetch(endpoint);
        long compileBefore = JvmActivity.compilationMillis();
        JvmActivity.Gc gcBefore = JvmActivity.gc();
        long appCpuBefore = AppCpuMeter.snapshotNanos();
        CpuTimeSource.Snapshot cpuBefore = cpuSource.snapshot();
        long start = System.nanoTime();
        Thread reporter = startProgressReporter(scenario, iterations, progressSeconds, progress, start);

        Driver.Samples samples = Driver.run(workload, scenario, mode, iterations, concurrency, progress);

        long wallNanos = System.nanoTime() - start;
        CpuTimeSource.Snapshot cpu = cpuSource.snapshot().minus(cpuBefore);
        long appCpuNanos = appCpuBefore < 0 ? -1 : AppCpuMeter.snapshotNanos() - appCpuBefore;
        JvmActivity.Gc gc = JvmActivity.gc().minus(gcBefore);
        long compileMillis = compileBefore < 0 ? -1 : JvmActivity.compilationMillis() - compileBefore;
        ServerStats server = ServerStats.fetch(endpoint).minus(serverBefore);
        if (reporter != null) {
            reporter.interrupt();
            reporter.join();
        }
        Driver.LatencyStats latency = samples.stats();

        // Independent check on the warmup gate. A quiet stretch before the window does not guarantee
        // a quiet window: compilation tails off in bursts, and async runs have been observed settling
        // by the warmup's criterion and then compiling for hundreds of milliseconds anyway. Code being
        // recompiled mid-window is code whose speed is changing, so say so rather than let a
        // "settled=true" imply more than it can.
        boolean steadyState = compileMillis >= 0
                              && compileMillis <= Math.max(50L, (long) (wallNanos / 1e6 * 0.02));
        if (compileMillis > 0 && !steadyState) {
            double wallSeconds = wallNanos / 1e9;
            String advice = wallSeconds < 5
                            // Compilation tails off in bursts, so a short window is disproportionately
                            // likely to contain one. Raising the operation count both dilutes the
                            // burst and gives it somewhere to finish.
                            ? String.format(Locale.US, "; the window is only %.1fs — raise --iterations",
                                            wallSeconds)
                            : "";
            System.out.printf("=== WARNING %s: %d ms of JIT compilation inside the measured window"
                              + " (%.1f%% of wall); code was still changing, so per-operation numbers"
                              + " are not steady-state%s%n",
                              scenario.cliName, compileMillis,
                              compileMillis / (wallNanos / 1e6) * 100, advice);
        }

        double wallSec = wallNanos / 1e9;
        double cpuSec = cpu.totalNanos() / 1e9;
        boolean split = cpuSource.hasUserSystemSplit();
        double userSec = split ? cpu.userNanos() / 1e9 : 0.0;
        String cpuSplit = split
                          ? String.format(Locale.US, " cpu_user_ms=%.0f cpu_sys_ms=%.0f",
                                          cpu.userNanos() / 1e6, cpu.systemNanos() / 1e6)
                          : "";
        String userRate = split
                          ? String.format(Locale.US, " ops_per_user_cpu_sec=%.1f",
                                          userSec > 0 ? iterations / userSec : 0.0)
                          : "";
        // app_cpu_us_per_op is the headline efficiency metric: process CPU includes the compiler, VM
        // and GC threads, whose cost is fixed per JVM rather than per operation. `unattributed_ms` is
        // process minus application CPU, i.e. what the JVM itself spent — printed rather than
        // silently dropped so the size of the correction is always visible.
        String appCpu = appCpuNanos < 0
                        ? " app_cpu_ms=unavailable"
                        : String.format(Locale.US,
                                        " app_cpu_ms=%.0f app_cpu_us_per_op=%.1f unattributed_ms=%.0f",
                                        appCpuNanos / 1e6, appCpuNanos / 1e3 / iterations,
                                        (cpu.totalNanos() - appCpuNanos) / 1e6);
        System.out.printf(Locale.US,
                          "RESULT client=%s transport=%s scenario=%s iterations=%d concurrency=%d"
                          + " async_mode=%s wall_ms=%.0f ops_per_wall_sec=%.1f cpu_ms=%.0f%s"
                          + " ops_per_cpu_sec=%.1f%s cpu_us_per_op=%.1f%s avg_us_per_op=%.1f"
                          + " mean_lat_us=%.1f p50_us=%.1f p90_us=%.1f p99_us=%.1f p999_us=%.1f"
                          + " max_us=%.1f jit_ms=%d gc_ms=%d gc_count=%d steady_state=%s"
                          + " warmup_ops=%d warmup_settled=%s%s%n",
                          client, workload.transport(), scenario.cliName, iterations, concurrency,
                          asyncMode, wallSec * 1e3, iterations / wallSec,
                          cpuSec * 1e3, cpuSplit, cpuSec > 0 ? iterations / cpuSec : 0.0, userRate,
                          cpu.totalNanos() / 1e3 / iterations, appCpu,
                          wallNanos / 1e3 / iterations,
                          latency.meanUs(), latency.p50Us(), latency.p90Us(), latency.p99Us(),
                          latency.p999Us(), latency.maxUs(),
                          compileMillis, gc.millis(), gc.count(), steadyState,
                          warmupResult.ops(), warmupResult.quiesced(),
                          server.summary(iterations));

        if (resultsFile != null) {
            appendResultsCsv(resultsFile, client, workload.transport(), scenario, iterations,
                             concurrency, asyncMode, wallNanos, cpu, split, latency, server,
                             appCpuNanos, compileMillis, gc, steadyState, warmupResult);
        }

        if (metrics) {
            metricsOut.printf("--- metrics client=%s scenario=%s iterations=%,d ---%n",
                              client, scenario.cliName, iterations);
            workload.printMetrics(metricsOut);
        }
    }

    private static final String CSV_HEADER =
        "client,scenario,iterations,wall_ms,ops_per_wall_sec,cpu_ms,cpu_user_ms,cpu_sys_ms,"
        // `commit` is the harness build; `sdk_commit` is the SDK inside it, which is the variable
        // under test when two jars are compared and is not always the same revision.
        + "ops_per_cpu_sec,ops_per_user_cpu_sec,avg_us_per_op,phase,commit,sdk_commit,"
        // Added after the columns above so existing readers keyed by name keep working.
        // `avg_us_per_op` is wall/iterations, which above concurrency 1 is the reciprocal of
        // throughput and not a latency; `mean_lat_us` and the percentiles are the real latencies.
        // `cpu_us_per_op` is the efficiency metric that concurrency does not distort.
        + "concurrency,async_mode,transport,cpu_us_per_op,mean_lat_us,p50_us,p90_us,p99_us,p999_us,"
        + "max_us,server_cpu_ms,server_requests,server_saturated,"
        // `app_cpu_us_per_op` is the efficiency metric to compare across runs: unlike cpu_us_per_op
        // it excludes compiler/VM/GC threads, whose cost is fixed per JVM. `jit_ms` and
        // `warmup_settled` say whether the window was steady-state at all.
        + "app_cpu_ms,app_cpu_us_per_op,unattributed_cpu_ms,jit_ms,gc_ms,gc_count,steady_state,"
        + "warmup_ops,warmup_settled";

    /**
     * Append one CSV row per RESULT line to {@code file}, creating it with a header row first if
     * it does not exist (or is empty). Columns mirror the RESULT fields; the user/system columns
     * are left empty when the bound CPU source has no split, and the server columns are left empty
     * when the endpoint is not the mock server.
     */
    private static void appendResultsCsv(Path file, String client, String transport, Scenario scenario,
                                         int iterations, int concurrency, String asyncMode,
                                         long wallNanos, CpuTimeSource.Snapshot cpu, boolean split,
                                         Driver.LatencyStats latency, ServerStats server,
                                         long appCpuNanos, long compileMillis, JvmActivity.Gc gc,
                                         boolean steadyState, Warmup.Result warmup)
            throws IOException {
        double wallSec = wallNanos / 1e9;
        double cpuSec = cpu.totalNanos() / 1e9;
        double userSec = split ? cpu.userNanos() / 1e9 : 0.0;
        BuildProvenance provenance = BuildProvenance.get();
        String row = String.format(Locale.US,
                                   "%s,%s,%d,%.0f,%.1f,%.0f,%s,%s,%.1f,%s,%.1f,%s,%s,%s,"
                                   + "%d,%s,%s,%.1f,%.1f,%.1f,%.1f,%.1f,%.1f,%.1f,%s,%s,%s,"
                                   + "%s,%s,%s,%s,%d,%d,%s,%d,%s",
                                   client, scenario.cliName, iterations,
                                   wallSec * 1e3, iterations / wallSec, cpuSec * 1e3,
                                   split ? String.format(Locale.US, "%.0f", cpu.userNanos() / 1e6) : "",
                                   split ? String.format(Locale.US, "%.0f", cpu.systemNanos() / 1e6) : "",
                                   cpuSec > 0 ? iterations / cpuSec : 0.0,
                                   split && userSec > 0
                                       ? String.format(Locale.US, "%.1f", iterations / userSec) : "",
                                   wallNanos / 1e3 / iterations,
                                   provenance.phase(), provenance.shortCommit(),
                                   provenance.shortSdkCommit(),
                                   concurrency, asyncMode, transport,
                                   cpu.totalNanos() / 1e3 / iterations,
                                   latency.meanUs(), latency.p50Us(), latency.p90Us(),
                                   latency.p99Us(), latency.p999Us(), latency.maxUs(),
                                   server.unavailable() ? "" : Long.toString(server.cpuNanos() / 1_000_000L),
                                   server.unavailable() ? "" : Long.toString(server.requests()),
                                   server.unavailable() ? "" : Boolean.toString(server.saturated()),
                                   appCpuNanos < 0 ? ""
                                       : String.format(Locale.US, "%.0f", appCpuNanos / 1e6),
                                   appCpuNanos < 0 ? ""
                                       : String.format(Locale.US, "%.1f",
                                                       appCpuNanos / 1e3 / iterations),
                                   appCpuNanos < 0 ? ""
                                       : String.format(Locale.US, "%.0f",
                                                       (cpu.totalNanos() - appCpuNanos) / 1e6),
                                   compileMillis < 0 ? "" : Long.toString(compileMillis),
                                   gc.millis(), gc.count(), Boolean.toString(steadyState),
                                   warmup.ops(), Boolean.toString(warmup.quiesced()));
        boolean needsHeader = !Files.exists(file) || Files.size(file) == 0;
        String content = needsHeader ? CSV_HEADER + "\n" + row + "\n" : row + "\n";
        Files.writeString(file, content, StandardOpenOption.CREATE, StandardOpenOption.APPEND);
    }

    /**
     * Daemon thread that prints progress from worker-local counters. Separate from the workers on
     * purpose: doing this inline previously cost tens of microseconds per operation, more than the
     * smallest operation being measured.
     */
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
                    double elapsedSec = (System.nanoTime() - start) / 1e9;
                    double rate = elapsedSec > 0 ? done / elapsedSec : 0;
                    double etaSec = rate > 0 ? (iterations - done) / rate : 0;
                    System.out.printf("progress %s %,d/%,d (%.1f%%) %.0f ops/s eta %s%n",
                                      scenario.cliName, done, iterations,
                                      100.0 * done / iterations, rate, formatDuration(etaSec));
                }
            } catch (InterruptedException e) {
                // Normal shutdown at the end of the scenario.
            }
        }, "bench-progress");
        t.setDaemon(true);
        t.start();
        return t;
    }

    /** Poll the readiness endpoint so a just-launched server has time to come up. */
    private static void waitForServer(URI endpoint) throws Exception {
        URI ping = endpoint.resolve("/ping");
        try (HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(1)).build()) {
            HttpRequest req = HttpRequest.newBuilder(ping).timeout(Duration.ofSeconds(2)).GET().build();
            long deadline = System.nanoTime() + Duration.ofSeconds(30).toNanos();
            while (true) {
                try {
                    http.send(req, HttpResponse.BodyHandlers.discarding());
                    return;
                } catch (Exception e) {
                    if (System.nanoTime() > deadline) {
                        System.err.println("Server at " + endpoint + " not reachable after 30s: " + e);
                        System.exit(1);
                    }
                    Thread.sleep(200);
                }
            }
        }
    }

    private static String names(List<Scenario> scenarios) {
        StringBuilder sb = new StringBuilder();
        for (Scenario s : scenarios) {
            if (sb.length() > 0) {
                sb.append(',');
            }
            sb.append(s.cliName);
        }
        return sb.toString();
    }

    private static String formatDuration(double seconds) {
        if (seconds >= 3600) {
            return String.format(Locale.US, "%.1fh", seconds / 3600);
        }
        if (seconds >= 60) {
            return String.format(Locale.US, "%.1fm", seconds / 60);
        }
        return String.format(Locale.US, "%.0fs", seconds);
    }
}
