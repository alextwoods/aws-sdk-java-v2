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

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.management.ManagementFactory;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * The separate mock-service process of one cell. It serves the cell's {@link ServiceFixture} on loopback over plain
 * HTTP/1.1 and applies the cell's {@link Outcome}: every request succeeds ({@code success}), every request gets the
 * modeled error ({@code error}), or each request independently gets the retryable error with probability 1/2 by a
 * fixed per-request-number coin ({@code retry}, see {@link #injectRetry}).
 *
 * <p>Control endpoints, excluded from every counter: {@code GET /ping} (readiness) and {@code GET /stats}
 * ({@code key=value} lines: requests, successes, modeled_errors, injected_retries, unmatched, bytes_in, bytes_out,
 * cpu_ns). A request that does not match the fixture gets a 400 and is counted as {@code unmatched}, which
 * invalidates the run.
 *
 * <p>Usage: {@code MockServiceMain --cell ID [--port N] [--threads N]}. Port 0 (the default) binds an ephemeral port;
 * once listening it prints exactly one line {@code READY port=<port> pid=<pid> cell=<id> fixture=<provider>
 * encoded_bytes=<n>}, which the orchestrator waits for (so a stale server on a fixed port can never be mistaken for
 * this one).
 */
public final class MockServiceMain {

    private static final byte[] OK = "ok".getBytes(StandardCharsets.UTF_8);
    private static final byte[] UNMATCHED = "{\"__type\":\"UnknownOperationException\"}".getBytes(StandardCharsets.UTF_8);

    private final ServiceFixture fixture;
    private final Outcome outcome;
    private final HttpServer server;
    private final ExecutorService pool;
    private final AtomicLong requests = new AtomicLong();
    private final AtomicLong successes = new AtomicLong();
    private final AtomicLong modeledErrors = new AtomicLong();
    private final AtomicLong injectedRetries = new AtomicLong();
    private final AtomicLong unmatched = new AtomicLong();
    private final AtomicLong bytesIn = new AtomicLong();
    private final AtomicLong bytesOut = new AtomicLong();

    private MockServiceMain(ServiceFixture fixture, Outcome outcome, int port, int threads) throws IOException {
        this.fixture = fixture;
        this.outcome = outcome;
        AtomicInteger n = new AtomicInteger();
        this.pool = Executors.newFixedThreadPool(threads, r -> {
            Thread t = new Thread(r, "mock-service-" + n.incrementAndGet());
            t.setDaemon(true);
            return t;
        });
        // The JDK server leaves Nagle on; with a closed-loop client on loopback that adds delayed-ACK stalls.
        System.setProperty("sun.net.httpserver.nodelay", "true");
        this.server = HttpServer.create(new InetSocketAddress("127.0.0.1", port), 1024);
        this.server.setExecutor(pool);
        this.server.createContext("/", this::handle);
    }

    public static void main(String[] args) throws Exception {
        String cellId = null;
        int port = 0;
        int threads = 64;
        for (int i = 0; i < args.length; i++) {
            switch (args[i]) {
                case "--cell" -> cellId = args[++i];
                case "--port" -> port = Integer.parseInt(args[++i]);
                case "--threads" -> threads = Integer.parseInt(args[++i]);
                default -> throw new IllegalArgumentException("unknown argument: " + args[i]);
            }
        }
        if (cellId == null) {
            throw new IllegalArgumentException("--cell is required");
        }
        BenchmarkCell cell = BenchmarkCell.parse(cellId);
        WorkloadProvider provider = WorkloadProvider.forCell(cell).orElseThrow(
            () -> new IllegalArgumentException("no workload fixture for cell " + cell));
        ServiceFixture fixture = provider.fixture(cell);
        MockServiceMain service = new MockServiceMain(fixture, cell.outcome(), port, Math.max(threads, 2));
        service.server.start();
        System.out.printf("READY port=%d pid=%d cell=%s fixture=%s encoded_bytes=%d%n",
                          service.server.getAddress().getPort(), ProcessHandle.current().pid(), cell.id(),
                          provider.name(), fixture.encodedBytes());
        System.out.flush();
        Thread.currentThread().join();
    }

    private void handle(HttpExchange exchange) throws IOException {
        try (exchange) {
            byte[] body;
            try (InputStream in = exchange.getRequestBody()) {
                body = in.readAllBytes();
            }
            String path = exchange.getRequestURI().getPath();
            String method = exchange.getRequestMethod();
            if ("GET".equals(method) && "/ping".equals(path)) {
                write(exchange, new ServiceFixture.MockResponse(200, "text/plain", OK), false);
                return;
            }
            if ("GET".equals(method) && "/stats".equals(path)) {
                write(exchange, new ServiceFixture.MockResponse(200, "text/plain",
                                                                stats().getBytes(StandardCharsets.UTF_8)), false);
                return;
            }
            String routing = fixture.routingHeaderName() == null ? null
                             : exchange.getRequestHeaders().getFirst(fixture.routingHeaderName());
            if (!fixture.matches(method, path, routing) || !fixture.matchesBody(method, body)) {
                unmatched.incrementAndGet();
                write(exchange, new ServiceFixture.MockResponse(400, "application/json", UNMATCHED), false);
                return;
            }
            long n = requests.incrementAndGet();
            bytesIn.addAndGet(body.length);
            ServiceFixture.MockResponse response = switch (outcome) {
                case SUCCESS -> {
                    successes.incrementAndGet();
                    yield fixture.success(method);
                }
                case MODELED_ERROR -> {
                    modeledErrors.incrementAndGet();
                    yield fixture.modeledError();
                }
                case RETRY -> {
                    if (injectRetry(n)) {
                        injectedRetries.incrementAndGet();
                        yield fixture.retryableError();
                    }
                    successes.incrementAndGet();
                    yield fixture.success(method);
                }
            };
            write(exchange, response, true);
        }
    }

    /**
     * Whether the {@code n}th matching request (1-based, arrival order) gets the retryable error: a fixed fair coin
     * per request number (SplitMix64 of {@code n}). Plain alternation is not used because closed-loop callers in
     * lockstep can systematically land on the failing parity and exhaust their attempts; a per-request-number coin
     * is reproducible yet uncorrelated with the callers' interleaving, so each attempt fails independently with
     * probability 1/2 and an operation costs one failed attempt on average.
     */
    static boolean injectRetry(long n) {
        long z = n * 0x9E3779B97F4A7C15L;
        z = (z ^ (z >>> 30)) * 0xBF58476D1CE4E5B9L;
        z = (z ^ (z >>> 27)) * 0x94D049BB133111EBL;
        z = z ^ (z >>> 31);
        return (z & 1) == 1;
    }

    private void write(HttpExchange exchange, ServiceFixture.MockResponse response, boolean count)
            throws IOException {
        exchange.getResponseHeaders().set("Content-Type", response.contentType());
        response.headers().forEach(exchange.getResponseHeaders()::set);
        // The JDK server reads a length of 0 as "chunked"; -1 sends an empty body with Content-Length: 0.
        exchange.sendResponseHeaders(response.status(), response.body().length == 0 ? -1 : response.body().length);
        try (OutputStream out = exchange.getResponseBody()) {
            out.write(response.body());
        }
        if (count) {
            bytesOut.addAndGet(response.body().length);
        }
    }

    private String stats() {
        long cpu = ((com.sun.management.OperatingSystemMXBean) ManagementFactory.getOperatingSystemMXBean())
            .getProcessCpuTime();
        return "requests=" + requests.get() + "\nsuccesses=" + successes.get() + "\nmodeled_errors="
               + modeledErrors.get() + "\ninjected_retries=" + injectedRetries.get() + "\nunmatched=" + unmatched.get()
               + "\nbytes_in=" + bytesIn.get() + "\nbytes_out=" + bytesOut.get() + "\ncpu_ns=" + cpu + "\n";
    }
}
