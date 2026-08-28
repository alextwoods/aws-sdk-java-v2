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
          --client X            SDK under test: v1, v2-sync, v2-async, smithy (required)
          --scenario X[,Y...]   small-get, small-put, batch-get, batch-put, or all (default: all)
          --iterations N        measured operations per scenario (default: 10000)
          --warmup N            warmup operations per scenario, unmeasured (default: min(2000, iterations))
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
        },
        SMALL_PUT("small-put") {
            void run(Workloads.Workload w) throws Exception {
                w.smallPut();
            }
        },
        BATCH_GET("batch-get") {
            void run(Workloads.Workload w) throws Exception {
                w.batchGet();
            }
        },
        BATCH_PUT("batch-put") {
            void run(Workloads.Workload w) throws Exception {
                w.batchPut();
            }
        };

        final String cliName;

        Scenario(String cliName) {
            this.cliName = cliName;
        }

        abstract void run(Workloads.Workload w) throws Exception;

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

        CpuTimeSource cpuSource = CpuTimeSources.bind(cpuSourceName);
        waitForServer(endpoint);

        PrintStream metricsOut = metricsFile == null
                                 ? System.out
                                 : new PrintStream(Files.newOutputStream(metricsFile), true);

        System.out.printf("=== standalone-e2e-benchmark client=%s scenarios=%s iterations=%,d warmup=%,d"
                          + " endpoint=%s metrics=%s pid=%d%n",
                          client, names(scenarios), iterations, warmup, endpoint,
                          metrics ? (metricsFile == null ? "on" : "on -> " + metricsFile) : "off",
                          ProcessHandle.current().pid());
        System.out.printf("=== cpu-time source: %s user/system split: %s%n",
                          cpuSource.name(), cpuSource.hasUserSystemSplit() ? "yes" : "no");
        System.out.printf("=== build: %s%n", BuildProvenance.get().summary());

        long suiteStart = System.nanoTime();
        try (Workloads.Workload workload = Workloads.create(client, endpoint, metrics)) {
            for (Scenario scenario : scenarios) {
                runScenario(workload, client, scenario, iterations, warmup, progressSeconds,
                            metrics, metricsOut, cpuSource, resultsFile);
            }
        }
        System.out.printf("DONE total_wall_s=%.1f%n", (System.nanoTime() - suiteStart) / 1e9);
        if (metricsFile != null) {
            metricsOut.close();
        }
    }

    private static void runScenario(Workloads.Workload workload, String client, Scenario scenario,
                                    int iterations, int warmup, int progressSeconds,
                                    boolean metrics, PrintStream metricsOut,
                                    CpuTimeSource cpuSource, Path resultsFile) throws Exception {
        for (int i = 0; i < warmup; i++) {
            scenario.run(workload);
        }
        workload.resetMetrics();

        // Progress reporting must add nothing to the measured loop when disabled. Both the deadline
        // and the clock read are guarded by `showProgress`, because a `start + Long.MAX_VALUE`
        // deadline silently overflows to a negative value: every iteration then looks due, and the
        // loop prints (and, since System.out auto-flushes on newline, writes) once per operation.
        boolean showProgress = progressSeconds > 0;
        long progressIntervalNanos = showProgress ? progressSeconds * 1_000_000_000L : 0L;
        CpuTimeSource.Snapshot cpuBefore = cpuSource.snapshot();
        long start = System.nanoTime();
        long nextProgress = showProgress ? start + progressIntervalNanos : Long.MAX_VALUE;

        for (int i = 0; i < iterations; i++) {
            scenario.run(workload);
            if (showProgress) {
                long now = System.nanoTime();
                if (now >= nextProgress) {
                    int done = i + 1;
                    double elapsedSec = (now - start) / 1e9;
                    double rate = done / elapsedSec;
                    double etaSec = (iterations - done) / rate;
                    System.out.printf("progress %s %,d/%,d (%.1f%%) %.0f ops/s eta %s%n",
                                      scenario.cliName, done, iterations, 100.0 * done / iterations,
                                      rate, formatDuration(etaSec));
                    nextProgress = now + progressIntervalNanos;
                }
            }
        }

        long wallNanos = System.nanoTime() - start;
        CpuTimeSource.Snapshot cpu = cpuSource.snapshot().minus(cpuBefore);

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
                          "RESULT client=%s scenario=%s iterations=%d wall_ms=%.0f ops_per_wall_sec=%.1f"
                          + " cpu_ms=%.0f%s ops_per_cpu_sec=%.1f%s avg_us_per_op=%.1f%n",
                          client, scenario.cliName, iterations, wallSec * 1e3, iterations / wallSec,
                          cpuSec * 1e3, cpuSplit, cpuSec > 0 ? iterations / cpuSec : 0.0, userRate,
                          wallNanos / 1e3 / iterations);

        if (resultsFile != null) {
            appendResultsCsv(resultsFile, client, scenario, iterations, wallNanos, cpu, split);
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
        + "ops_per_cpu_sec,ops_per_user_cpu_sec,avg_us_per_op,phase,commit,sdk_commit";

    /**
     * Append one CSV row per RESULT line to {@code file}, creating it with a header row first if
     * it does not exist (or is empty). Columns mirror the RESULT fields; the user/system columns
     * are left empty when the bound CPU source has no split.
     */
    private static void appendResultsCsv(Path file, String client, Scenario scenario, int iterations,
                                         long wallNanos, CpuTimeSource.Snapshot cpu, boolean split)
            throws IOException {
        double wallSec = wallNanos / 1e9;
        double cpuSec = cpu.totalNanos() / 1e9;
        double userSec = split ? cpu.userNanos() / 1e9 : 0.0;
        BuildProvenance provenance = BuildProvenance.get();
        String row = String.format(Locale.US, "%s,%s,%d,%.0f,%.1f,%.0f,%s,%s,%.1f,%s,%.1f,%s,%s,%s",
                                   client, scenario.cliName, iterations,
                                   wallSec * 1e3, iterations / wallSec, cpuSec * 1e3,
                                   split ? String.format(Locale.US, "%.0f", cpu.userNanos() / 1e6) : "",
                                   split ? String.format(Locale.US, "%.0f", cpu.systemNanos() / 1e6) : "",
                                   cpuSec > 0 ? iterations / cpuSec : 0.0,
                                   split && userSec > 0
                                       ? String.format(Locale.US, "%.1f", iterations / userSec) : "",
                                   wallNanos / 1e3 / iterations,
                                   provenance.phase(), provenance.shortCommit(),
                                   provenance.shortSdkCommit());
        boolean needsHeader = !Files.exists(file) || Files.size(file) == 0;
        String content = needsHeader ? CSV_HEADER + "\n" + row + "\n" : row + "\n";
        Files.writeString(file, content, StandardOpenOption.CREATE, StandardOpenOption.APPEND);
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
