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

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.management.ManagementFactory;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Out-of-process canned-response DynamoDB server: racecar's {@code MockDdbServer} ported from Jetty onto the JDK's
 * {@code com.sun.net.httpserver} (V3 carries no Jetty).
 *
 * <p>Same contract as racecar: routes on {@code X-Amz-Target}, reads and discards the request body, returns
 * precomputed bytes per operation (byte-identical to racecar's, see {@code MockServerGoldenTest}) with
 * {@code Content-Type: application/x-amz-json-1.0} and an explicit {@code Content-Length}, and no
 * {@code x-amz-crc32}. Requests without a target get {@code 200 ok} (readiness probe, {@code GET /ping}); unknown
 * targets get a 400. {@code GET /stats} reports {@code key=value} counters and is excluded from them.
 *
 * <p>V3 additions, for pre-timing verification: request and response byte counters, per-operation counts, and
 * {@code GET /last?target=T}, which returns the most recent request body received for target {@code T} (headers
 * {@code x-last-content-type}, {@code x-last-count}). Keeping one reference per target costs a volatile write.
 *
 * <p>Header deviations from Jetty, none of which any client under test reads: the JDK server sends {@code Date} but no
 * {@code Server} header, and writes {@code Content-length} in that case (HTTP header names are case-insensitive).
 *
 * <p>Usage: {@code MockDdbServer [--port N] [--threads N]}; prints {@code READY port=N ...} once listening.
 */
public final class MockDdbServer {

    public static final int DEFAULT_PORT = 19080;
    /** Handler threads; as in racecar, comfortably above any client concurrency. */
    public static final int DEFAULT_MAX_THREADS = 200;
    static final String CONTENT_TYPE = "application/x-amz-json-1.0";
    static final String TARGET_PREFIX = "DynamoDB_20120810.";
    static final String[] OPERATIONS = {"GetItem", "PutItem", "BatchGetItem", "BatchWriteItem"};
    private static final byte[] OK = "ok".getBytes(StandardCharsets.UTF_8);

    private final HttpServer server;
    private final ThreadPoolExecutor pool;
    private final Map<String, byte[]> responses;
    private final Map<String, Captured> lastRequests = new LinkedHashMap<>();
    private final Map<String, AtomicLong> perTarget = new LinkedHashMap<>();
    private final AtomicLong requests = new AtomicLong();
    private final AtomicLong errors = new AtomicLong();
    private final AtomicLong bytesIn = new AtomicLong();
    private final AtomicLong bytesOut = new AtomicLong();
    private final com.sun.management.OperatingSystemMXBean os =
        (com.sun.management.OperatingSystemMXBean) ManagementFactory.getOperatingSystemMXBean();

    private record Captured(byte[] body, String contentType, long count) {
    }

    private MockDdbServer(int port, int maxThreads, Map<String, byte[]> responses) throws IOException {
        this.responses = responses;
        for (String target : responses.keySet()) {
            perTarget.put(target, new AtomicLong());
            lastRequests.put(target, null);
        }
        AtomicInteger n = new AtomicInteger();
        this.pool = new ThreadPoolExecutor(maxThreads, maxThreads, 60, TimeUnit.SECONDS, new LinkedBlockingQueue<>(),
                                           r -> {
                                               Thread t = new Thread(r, "mockddb-" + n.incrementAndGet());
                                               t.setDaemon(true);
                                               return t;
                                           });
        this.pool.prestartAllCoreThreads();
        this.server = HttpServer.create(new InetSocketAddress("127.0.0.1", port), 1024);
        this.server.setExecutor(pool);
        this.server.createContext("/", this::handle);
    }

    /** The canned responses, keyed by full {@code X-Amz-Target}. */
    static Map<String, byte[]> cannedResponses() {
        Map<String, byte[]> responses = new LinkedHashMap<>();
        responses.put(TARGET_PREFIX + "GetItem", DynamoDbItems.utf8(DynamoDbItems.getItemResponseJson()));
        responses.put(TARGET_PREFIX + "PutItem", DynamoDbItems.utf8(DynamoDbItems.putItemResponseJson()));
        responses.put(TARGET_PREFIX + "BatchGetItem", DynamoDbItems.utf8(DynamoDbItems.batchGetItemResponseJson()));
        responses.put(TARGET_PREFIX + "BatchWriteItem", DynamoDbItems.utf8(DynamoDbItems.batchWriteItemResponseJson()));
        return responses;
    }

    /** Starts a server; {@code port} 0 picks an ephemeral port (tests). */
    static MockDdbServer start(int port, int maxThreads, Map<String, byte[]> responses) throws IOException {
        // The JDK server leaves Nagle on by default; with a closed-loop client on loopback that interacts with delayed
        // ACKs and adds tens of milliseconds to responses written in more than one segment. Jetty sets TCP_NODELAY.
        System.setProperty("sun.net.httpserver.nodelay", "true");
        MockDdbServer s = new MockDdbServer(port, maxThreads, responses);
        s.server.start();
        return s;
    }

    int port() {
        return server.getAddress().getPort();
    }

    void stop() {
        server.stop(0);
        pool.shutdownNow();
    }

    public static void main(String[] args) throws Exception {
        int port = DEFAULT_PORT;
        int maxThreads = DEFAULT_MAX_THREADS;
        for (int i = 0; i < args.length; i++) {
            if ("--port".equals(args[i]) && i + 1 < args.length) {
                port = Integer.parseInt(args[++i]);
            } else if ("--threads".equals(args[i]) && i + 1 < args.length) {
                maxThreads = Integer.parseInt(args[++i]);
            } else {
                throw new IllegalArgumentException("unknown argument " + args[i] + " (--port N, --threads N)");
            }
        }
        Map<String, byte[]> responses = cannedResponses();
        MockDdbServer s = start(port, maxThreads, responses);
        System.out.printf("READY port=%d pid=%d threads=%d server=jdk-httpserver scheme=http"
                          + " (GetItem=%dB PutItem=%dB BatchGetItem=%dB BatchWriteItem=%dB)%n",
                          s.port(), ProcessHandle.current().pid(), maxThreads,
                          responses.get(TARGET_PREFIX + "GetItem").length,
                          responses.get(TARGET_PREFIX + "PutItem").length,
                          responses.get(TARGET_PREFIX + "BatchGetItem").length,
                          responses.get(TARGET_PREFIX + "BatchWriteItem").length);
        System.out.flush();
        Thread.currentThread().join();
    }

    private void handle(HttpExchange exchange) throws IOException {
        try (exchange) {
            String path = exchange.getRequestURI().getPath();
            if ("/stats".equals(path)) {
                exchange.getRequestBody().readAllBytes();
                write(exchange, 200, "text/plain", stats().getBytes(StandardCharsets.UTF_8), false);
                return;
            }
            if ("/last".equals(path)) {
                exchange.getRequestBody().readAllBytes();
                writeLast(exchange);
                return;
            }
            byte[] requestBody;
            try (InputStream in = exchange.getRequestBody()) {
                requestBody = in.readAllBytes();
            }
            String target = exchange.getRequestHeaders().getFirst("X-Amz-Target");
            if (target == null) {
                write(exchange, 200, "text/plain", OK, false);
                return;
            }
            bytesIn.addAndGet(requestBody.length);
            byte[] body = responses.get(target);
            if (body == null) {
                errors.incrementAndGet();
                byte[] err = ("{\"__type\":\"com.amazon.coral.service#UnknownOperationException\",\"message\":\""
                              + target + "\"}").getBytes(StandardCharsets.UTF_8);
                write(exchange, 400, CONTENT_TYPE, err, true);
                return;
            }
            long count = perTarget.get(target).incrementAndGet();
            synchronized (lastRequests) {
                lastRequests.put(target, new Captured(requestBody,
                                                      exchange.getRequestHeaders().getFirst("Content-Type"), count));
            }
            requests.incrementAndGet();
            write(exchange, 200, CONTENT_TYPE, body, true);
        }
    }

    private void write(HttpExchange exchange, int status, String contentType, byte[] body, boolean count)
            throws IOException {
        exchange.getResponseHeaders().set("Content-Type", contentType);
        exchange.sendResponseHeaders(status, body.length);
        try (OutputStream out = exchange.getResponseBody()) {
            out.write(body);
        }
        if (count) {
            bytesOut.addAndGet(body.length);
        }
    }

    private void writeLast(HttpExchange exchange) throws IOException {
        String query = exchange.getRequestURI().getQuery();
        String target = query != null && query.startsWith("target=") ? query.substring("target=".length()) : "";
        Captured captured;
        synchronized (lastRequests) {
            captured = lastRequests.get(target);
        }
        if (captured == null) {
            write(exchange, 404, "text/plain", ("no request for target " + target).getBytes(StandardCharsets.UTF_8),
                  false);
            return;
        }
        exchange.getResponseHeaders().set("x-last-content-type", String.valueOf(captured.contentType()));
        exchange.getResponseHeaders().set("x-last-count", Long.toString(captured.count()));
        write(exchange, 200, "application/octet-stream", captured.body(), false);
    }

    private String stats() {
        StringBuilder sb = new StringBuilder(512);
        sb.append("requests=").append(requests.get()).append('\n')
          .append("errors=").append(errors.get()).append('\n')
          .append("bytes_in=").append(bytesIn.get()).append('\n')
          .append("bytes_out=").append(bytesOut.get()).append('\n')
          .append("cpu_ms=").append(os.getProcessCpuTime() / 1_000_000L).append('\n')
          .append("cpu_user_ms=\n")
          .append("cpu_sys_ms=\n")
          .append("threads=").append(pool.getPoolSize()).append('\n')
          .append("busy_threads=").append(pool.getActiveCount()).append('\n')
          .append("max_threads=").append(pool.getMaximumPoolSize()).append('\n')
          .append("queue_size=").append(pool.getQueue().size()).append('\n')
          .append("low_on_threads=").append(pool.getActiveCount() >= pool.getMaximumPoolSize()).append('\n');
        for (Map.Entry<String, AtomicLong> e : perTarget.entrySet()) {
            sb.append("target.").append(e.getKey()).append('=').append(e.getValue().get()).append('\n');
        }
        return sb.toString();
    }
}
