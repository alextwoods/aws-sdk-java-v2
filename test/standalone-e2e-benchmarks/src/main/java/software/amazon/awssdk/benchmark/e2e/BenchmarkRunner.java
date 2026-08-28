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
                                  v1              V1, Apache 4.x
                                  v2-sync         V2 sync, Apache5
                                  v2-sync-apache4 V2 sync, Apache 4.x
                                  v2-async        V2 async, CRT
                                  v2-async-netty  V2 async, Netty
                                  smithy          smithy-java, HTTP/1.1
          --scenario X[,Y...]   small-get, small-put, batch-get, batch-put, or all (default: all)
          --iterations N        measured operations per scenario (default: 10000)
          --warmup N            warmup operations per scenario, unmeasured (default: min(2000, iterations))
          --concurrency N       operations kept in flight (default: 1). Sync clients use N threads;
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
          --progress-seconds N  progress/ETA print interval, 0 disables (default: 5)
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
        int progressSeconds = 5;
        String cpuSourceName = "auto";
        Path resultsFile = null;
        int concurrency = 1;
        String asyncModeName = "inflight";

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

        CpuTimeSource cpuSource = CpuTimeSources.bind(cpuSourceName);
        waitForServer(endpoint);

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

            System.out.printf("=== standalone-e2e-benchmark client=%s transport=%s scenarios=%s"
                              + " iterations=%,d warmup=%,d concurrency=%d driver=%s async-mode=%s"
                              + " endpoint=%s metrics=%s pid=%d%n",
                              client, workload.transport(), names(scenarios), iterations, warmup,
                              concurrency, mode == Driver.Mode.INFLIGHT ? "in-flight" : "threads",
                              effectiveAsyncMode, endpoint,
                              metrics ? (metricsFile == null ? "on" : "on -> " + metricsFile) : "off",
                              ProcessHandle.current().pid());
            System.out.printf("=== cpu-time source: %s user/system split: %s%n",
                              cpuSource.name(), cpuSource.hasUserSystemSplit() ? "yes" : "no");
            System.out.printf("=== build: %s%n", BuildProvenance.get().summary());

            for (Scenario scenario : scenarios) {
                runScenario(workload, client, scenario, iterations, warmup, concurrency, mode,
                            effectiveAsyncMode, progressSeconds, metrics, metricsOut, cpuSource,
                            endpoint, resultsFile);
            }
        }
        System.out.printf("DONE total_wall_s=%.1f%n", (System.nanoTime() - suiteStart) / 1e9);
        if (metricsFile != null) {
            metricsOut.close();
        }
    }

    private static void runScenario(Workloads.Workload workload, String client, Scenario scenario,
                                    int iterations, int warmup, int concurrency, Driver.Mode mode,
                                    String asyncMode, int progressSeconds, boolean metrics,
                                    PrintStream metricsOut, CpuTimeSource cpuSource, URI endpoint,
                                    Path resultsFile) throws Exception {
        // Warm up in the same shape as the measurement: at concurrency N the pipeline has to warm N
        // threads (or N in-flight slots) and fill an N-connection pool, and a single-threaded warmup
        // would leave that work inside the measured window.
        Driver.run(workload, scenario, mode, warmup, concurrency, null);
        workload.resetMetrics();

        // The progress thread reads worker-local counters, so nothing about reporting touches the
        // measured path.
        Driver.Progress progress = new Driver.Progress(mode == Driver.Mode.INFLIGHT ? 1 : concurrency);
        ServerStats serverBefore = ServerStats.fetch(endpoint);
        CpuTimeSource.Snapshot cpuBefore = cpuSource.snapshot();
        long start = System.nanoTime();
        Thread reporter = startProgressReporter(scenario, iterations, progressSeconds, progress, start);

        Driver.Samples samples = Driver.run(workload, scenario, mode, iterations, concurrency, progress);

        long wallNanos = System.nanoTime() - start;
        CpuTimeSource.Snapshot cpu = cpuSource.snapshot().minus(cpuBefore);
        ServerStats server = ServerStats.fetch(endpoint).minus(serverBefore);
        if (reporter != null) {
            reporter.interrupt();
            reporter.join();
        }
        Driver.LatencyStats latency = samples.stats();

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
        System.out.printf(Locale.US,
                          "RESULT client=%s transport=%s scenario=%s iterations=%d concurrency=%d"
                          + " async_mode=%s wall_ms=%.0f ops_per_wall_sec=%.1f cpu_ms=%.0f%s"
                          + " ops_per_cpu_sec=%.1f%s cpu_us_per_op=%.1f avg_us_per_op=%.1f"
                          + " mean_lat_us=%.1f p50_us=%.1f p90_us=%.1f p99_us=%.1f p999_us=%.1f"
                          + " max_us=%.1f%s%n",
                          client, workload.transport(), scenario.cliName, iterations, concurrency,
                          asyncMode, wallSec * 1e3, iterations / wallSec,
                          cpuSec * 1e3, cpuSplit, cpuSec > 0 ? iterations / cpuSec : 0.0, userRate,
                          cpu.totalNanos() / 1e3 / iterations,
                          wallNanos / 1e3 / iterations,
                          latency.meanUs(), latency.p50Us(), latency.p90Us(), latency.p99Us(),
                          latency.p999Us(), latency.maxUs(),
                          server.summary(iterations));

        if (resultsFile != null) {
            appendResultsCsv(resultsFile, client, workload.transport(), scenario, iterations,
                             concurrency, asyncMode, wallNanos, cpu, split, latency, server);
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
        + "max_us,server_cpu_ms,server_requests,server_saturated";

    /**
     * Append one CSV row per RESULT line to {@code file}, creating it with a header row first if
     * it does not exist (or is empty). Columns mirror the RESULT fields; the user/system columns
     * are left empty when the bound CPU source has no split, and the server columns are left empty
     * when the endpoint is not the mock server.
     */
    private static void appendResultsCsv(Path file, String client, String transport, Scenario scenario,
                                         int iterations, int concurrency, String asyncMode,
                                         long wallNanos, CpuTimeSource.Snapshot cpu, boolean split,
                                         Driver.LatencyStats latency, ServerStats server)
            throws IOException {
        double wallSec = wallNanos / 1e9;
        double cpuSec = cpu.totalNanos() / 1e9;
        double userSec = split ? cpu.userNanos() / 1e9 : 0.0;
        BuildProvenance provenance = BuildProvenance.get();
        String row = String.format(Locale.US,
                                   "%s,%s,%d,%.0f,%.1f,%.0f,%s,%s,%.1f,%s,%.1f,%s,%s,%s,"
                                   + "%d,%s,%s,%.1f,%.1f,%.1f,%.1f,%.1f,%.1f,%.1f,%s,%s,%s",
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
                                   server.unavailable() ? "" : Boolean.toString(server.saturated()));
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
