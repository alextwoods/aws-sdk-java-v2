package software.amazon.awssdk.benchmark.e2e;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

/**
 * Client-side reader for {@link MockDdbServer}'s {@code /stats} endpoint.
 *
 * <p>Exists so a run can answer "was the server the bottleneck?" with data instead of an assumption.
 * At concurrency 1 the server is effectively free and can be ignored; once the client keeps several
 * operations in flight, the server competes for the same cores, and a throughput number can become a
 * measurement of the server. Sampling these counters either side of the measured window turns that
 * into two facts: how much CPU the server burned per operation, and whether it ever queued.
 *
 * <p>All fields are absolute readings. Use {@link #minus(ServerStats)} for the measured window;
 * the saturation fields are instantaneous, so a delta keeps the later (post-window) reading.
 */
final class ServerStats {

    /** Counters over the window (or absolute, for a raw reading). */
    final long requests;
    final long errors;
    final long cpuNanos;
    final long userCpuNanos;
    final long systemCpuNanos;
    final boolean hasSplit;

    /** Instantaneous saturation signals, as observed at the end of the window. */
    final int queueSize;
    final int busyThreads;
    final int maxThreads;
    final boolean lowOnThreads;

    /** True when the endpoint could not be read; all other fields are then meaningless. */
    final boolean unavailable;

    private ServerStats(long requests, long errors, long cpuNanos, long userCpuNanos,
                        long systemCpuNanos, boolean hasSplit, int queueSize, int busyThreads,
                        int maxThreads, boolean lowOnThreads, boolean unavailable) {
        this.requests = requests;
        this.errors = errors;
        this.cpuNanos = cpuNanos;
        this.userCpuNanos = userCpuNanos;
        this.systemCpuNanos = systemCpuNanos;
        this.hasSplit = hasSplit;
        this.queueSize = queueSize;
        this.busyThreads = busyThreads;
        this.maxThreads = maxThreads;
        this.lowOnThreads = lowOnThreads;
        this.unavailable = unavailable;
    }

    static ServerStats notAvailable() {
        return new ServerStats(0, 0, 0, -1, -1, false, 0, 0, 0, false, true);
    }

    boolean unavailable() {
        return unavailable;
    }

    long requests() {
        return requests;
    }

    long cpuNanos() {
        return cpuNanos;
    }

    /**
     * Whether the server queued work or ran out of handler threads during the window. When true, the
     * client's throughput number is partly a measurement of the server and should not be compared
     * against a run where this is false.
     */
    boolean saturated() {
        return !unavailable && (queueSize > 0 || lowOnThreads);
    }

    /**
     * One client for the whole run, for two reasons that both affect the measurement.
     *
     * <p>Building an {@link HttpClient} per call put its class loading and JIT compilation
     * immediately before the measured window, where the compilation landed inside it — observed as
     * hundreds of milliseconds of in-window compilation on short runs. It also spun up and tore down
     * the client's internal selector and executor threads around every window, and threads that die
     * mid-window take their CPU with them (see {@link AppCpuMeter}), so the accounting lost it.
     *
     * <p>Held for process lifetime rather than closed, since the process is a single benchmark run.
     */
    private static volatile HttpClient sharedClient;

    private static HttpClient client() {
        HttpClient c = sharedClient;
        if (c == null) {
            synchronized (ServerStats.class) {
                c = sharedClient;
                if (c == null) {
                    c = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(2)).build();
                    sharedClient = c;
                }
            }
        }
        return c;
    }

    /**
     * Fetch {@code /stats}. Returns {@link #notAvailable()} rather than throwing: these counters are
     * diagnostics, and a benchmark run against DynamoDB Local (or any endpoint that is not this mock
     * server) must still work.
     */
    static ServerStats fetch(URI endpoint) {
        try {
            HttpRequest req = HttpRequest.newBuilder(endpoint.resolve("/stats"))
                                         .timeout(Duration.ofSeconds(5)).GET().build();
            HttpResponse<String> resp = client().send(req, HttpResponse.BodyHandlers.ofString());
            if (resp.statusCode() != 200) {
                return notAvailable();
            }
            return parse(resp.body());
        } catch (Exception e) {
            return notAvailable();
        }
    }

    /**
     * Exercise the stats path once before warmup, so the client construction, class loading and first
     * compilation of this code happen well before any measured window.
     */
    static void prewarm(URI endpoint) {
        for (int i = 0; i < 3; i++) {
            fetch(endpoint);
        }
    }

    private static ServerStats parse(String body) {
        long requests = 0;
        long errors = 0;
        long cpuMs = 0;
        long userMs = -1;
        long sysMs = -1;
        int queueSize = 0;
        int busy = 0;
        int max = 0;
        boolean low = false;
        for (String line : body.split("\n")) {
            int eq = line.indexOf('=');
            if (eq < 0) {
                continue;
            }
            String key = line.substring(0, eq);
            String value = line.substring(eq + 1).trim();
            if (value.isEmpty()) {
                continue;
            }
            switch (key) {
                case "requests": requests = Long.parseLong(value); break;
                case "errors": errors = Long.parseLong(value); break;
                case "cpu_ms": cpuMs = Long.parseLong(value); break;
                case "cpu_user_ms": userMs = Long.parseLong(value); break;
                case "cpu_sys_ms": sysMs = Long.parseLong(value); break;
                case "queue_size": queueSize = Integer.parseInt(value); break;
                case "busy_threads": busy = Integer.parseInt(value); break;
                case "max_threads": max = Integer.parseInt(value); break;
                case "low_on_threads": low = Boolean.parseBoolean(value); break;
                default: break;
            }
        }
        boolean split = userMs >= 0 && sysMs >= 0;
        return new ServerStats(requests, errors, cpuMs * 1_000_000L,
                               split ? userMs * 1_000_000L : -1,
                               split ? sysMs * 1_000_000L : -1,
                               split, queueSize, busy, max, low, false);
    }

    /** This reading minus an earlier one. Saturation fields are taken from {@code this}. */
    ServerStats minus(ServerStats before) {
        if (unavailable || before.unavailable) {
            return notAvailable();
        }
        boolean split = hasSplit && before.hasSplit;
        return new ServerStats(requests - before.requests,
                               errors - before.errors,
                               cpuNanos - before.cpuNanos,
                               split ? userCpuNanos - before.userCpuNanos : -1,
                               split ? systemCpuNanos - before.systemCpuNanos : -1,
                               split, queueSize, busyThreads, maxThreads, lowOnThreads, false);
    }

    /**
     * One-line summary for the RESULT line. Empty when unavailable, so a non-mock endpoint adds
     * nothing to the output.
     */
    String summary(int expectedRequests) {
        if (unavailable) {
            return "";
        }
        StringBuilder sb = new StringBuilder(96);
        sb.append(" server_cpu_ms=").append(cpuNanos / 1_000_000L)
          .append(" server_requests=").append(requests);
        if (requests != expectedRequests) {
            // The client and server disagree about how many operations happened. Retries, dropped
            // connections or a stray second client would all show up here.
            sb.append(" server_requests_MISMATCH=expected_").append(expectedRequests);
        }
        if (errors != 0) {
            sb.append(" server_errors=").append(errors);
        }
        if (queueSize > 0 || lowOnThreads) {
            sb.append(" server_SATURATED=queue_").append(queueSize)
              .append("_busy_").append(busyThreads).append("_of_").append(maxThreads);
        }
        return sb.toString();
    }
}
