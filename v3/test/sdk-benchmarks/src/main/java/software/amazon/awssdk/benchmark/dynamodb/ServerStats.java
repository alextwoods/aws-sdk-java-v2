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

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

/**
 * Client-side reader for {@link MockDdbServer}'s {@code /stats} and {@code /last}, ported from racecar's
 * {@code ServerStats} (plus request/response byte counters).
 *
 * <p>Uses the JDK {@code java.net.http} client, one instance for the process, so its class loading and threads are
 * set up once before any measured window ({@link #prewarm(URI)}).
 */
final class ServerStats {

    final long requests;
    final long errors;
    final long bytesIn;
    final long bytesOut;
    final long cpuNanos;
    final int queueSize;
    final int busyThreads;
    final int maxThreads;
    final boolean lowOnThreads;
    final boolean unavailable;

    private ServerStats(long requests, long errors, long bytesIn, long bytesOut, long cpuNanos, int queueSize,
                        int busyThreads, int maxThreads, boolean lowOnThreads, boolean unavailable) {
        this.requests = requests;
        this.errors = errors;
        this.bytesIn = bytesIn;
        this.bytesOut = bytesOut;
        this.cpuNanos = cpuNanos;
        this.queueSize = queueSize;
        this.busyThreads = busyThreads;
        this.maxThreads = maxThreads;
        this.lowOnThreads = lowOnThreads;
        this.unavailable = unavailable;
    }

    static ServerStats notAvailable() {
        return new ServerStats(0, 0, 0, 0, 0, 0, 0, 0, false, true);
    }

    /** Whether the server's handler pool queued work or ran out of threads (not the socket layer; see racecar). */
    boolean saturated() {
        return !unavailable && (queueSize > 0 || lowOnThreads);
    }

    private static final HttpClient CLIENT = HttpClient.newBuilder()
                                                       .version(HttpClient.Version.HTTP_1_1)
                                                       .connectTimeout(Duration.ofSeconds(2)).build();

    static ServerStats fetch(URI endpoint) {
        try {
            HttpResponse<String> resp = CLIENT.send(HttpRequest.newBuilder(endpoint.resolve("/stats"))
                                                               .timeout(Duration.ofSeconds(5)).GET().build(),
                                                    HttpResponse.BodyHandlers.ofString());
            return resp.statusCode() == 200 ? parse(resp.body()) : notAvailable();
        } catch (Exception e) {
            return notAvailable();
        }
    }

    /** The last request body the server received for {@code target}, or null if none. */
    static HttpResponse<byte[]> last(URI endpoint, String target) throws Exception {
        HttpResponse<byte[]> resp = CLIENT.send(HttpRequest.newBuilder(endpoint.resolve("/last?target=" + target))
                                                           .timeout(Duration.ofSeconds(5)).GET().build(),
                                                HttpResponse.BodyHandlers.ofByteArray());
        return resp.statusCode() == 200 ? resp : null;
    }

    static void prewarm(URI endpoint) {
        for (int i = 0; i < 3; i++) {
            fetch(endpoint);
        }
    }

    private static ServerStats parse(String body) {
        long requests = 0;
        long errors = 0;
        long in = 0;
        long out = 0;
        long cpuMs = 0;
        int queue = 0;
        int busy = 0;
        int max = 0;
        boolean low = false;
        for (String line : body.split("\n")) {
            int eq = line.indexOf('=');
            if (eq < 0 || eq == line.length() - 1) {
                continue;
            }
            String value = line.substring(eq + 1).trim();
            switch (line.substring(0, eq)) {
                case "requests" -> requests = Long.parseLong(value);
                case "errors" -> errors = Long.parseLong(value);
                case "bytes_in" -> in = Long.parseLong(value);
                case "bytes_out" -> out = Long.parseLong(value);
                case "cpu_ms" -> cpuMs = Long.parseLong(value);
                case "queue_size" -> queue = Integer.parseInt(value);
                case "busy_threads" -> busy = Integer.parseInt(value);
                case "max_threads" -> max = Integer.parseInt(value);
                case "low_on_threads" -> low = Boolean.parseBoolean(value);
                default -> {
                }
            }
        }
        return new ServerStats(requests, errors, in, out, cpuMs * 1_000_000L, queue, busy, max, low, false);
    }

    /** This reading minus an earlier one; saturation fields are taken from {@code this}. */
    ServerStats minus(ServerStats before) {
        if (unavailable || before.unavailable) {
            return notAvailable();
        }
        return new ServerStats(requests - before.requests, errors - before.errors, bytesIn - before.bytesIn,
                               bytesOut - before.bytesOut, cpuNanos - before.cpuNanos, queueSize, busyThreads,
                               maxThreads, lowOnThreads, false);
    }

    /** RESULT-line fragment; empty when unavailable. */
    String summary(long expectedRequests) {
        if (unavailable) {
            return "";
        }
        StringBuilder sb = new StringBuilder(128);
        sb.append(" server_cpu_ms=").append(cpuNanos / 1_000_000L)
          .append(" server_requests=").append(requests)
          .append(" server_bytes_in=").append(bytesIn)
          .append(" server_bytes_out=").append(bytesOut);
        if (requests != expectedRequests) {
            sb.append(" server_requests_MISMATCH=expected_").append(expectedRequests);
        }
        if (errors != 0) {
            sb.append(" server_errors=").append(errors);
        }
        if (saturated()) {
            sb.append(" server_SATURATED=queue_").append(queueSize)
              .append("_busy_").append(busyThreads).append("_of_").append(maxThreads);
        }
        return sb.toString();
    }
}
