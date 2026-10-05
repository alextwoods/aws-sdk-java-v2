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

package software.amazon.awssdk.http.crt;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.ServerSocket;
import java.security.MessageDigest;
import java.time.Duration;
import java.util.List;
import java.util.Random;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.http.HttpBody;
import software.amazon.awssdk.http.HttpExecutionOptions;
import software.amazon.awssdk.http.HttpFailureCategory;
import software.amazon.awssdk.http.HttpPhase;
import software.amazon.awssdk.http.HttpTransportException;
import software.amazon.awssdk.http.SdkHttpRequest;
import software.amazon.awssdk.http.SdkHttpResponse;
import software.amazon.awssdk.http.StreamingBodies;
import software.amazon.awssdk.http.SyncHttpExchange;
import software.amazon.awssdk.http.crt.internal.CrtSyncExchange;

class AwsCrtHttpClientTest {
    private static final byte[] LARGE = new byte[4 * 1024 * 1024];
    private static final CountDownLatch HANG = new CountDownLatch(1);
    private static final byte[] MEDIUM = new byte[64 * 1024];
    /** The client port of each request to /medium or /port, in arrival order: a new port is a new connection. */
    private static final List<Integer> CLIENT_PORTS = new java.util.concurrent.CopyOnWriteArrayList<>();
    private static TestServers.Server server;

    @BeforeAll
    static void start() throws IOException {
        new Random(7).nextBytes(LARGE);
        server = TestServers.Server.http(exchange -> {
            String path = exchange.getRequestURI().getRawPath();
            try (exchange) {
                switch (path) {
                    case "/echo" -> {
                        byte[] in = exchange.getRequestBody().readAllBytes();
                        exchange.getResponseHeaders().add("X-Target", exchange.getRequestURI().toString());
                        exchange.getResponseHeaders().add("X-Sha", sha(in));
                        exchange.sendResponseHeaders(200, in.length == 0 ? -1 : in.length);
                        exchange.getResponseBody().write(in);
                    }
                    case "/large" -> {
                        exchange.sendResponseHeaders(200, LARGE.length);
                        exchange.getResponseBody().write(LARGE);
                    }
                    case "/medium" -> {
                        CLIENT_PORTS.add(exchange.getRemoteAddress().getPort());
                        exchange.sendResponseHeaders(200, MEDIUM.length);
                        exchange.getResponseBody().write(MEDIUM);
                    }
                    case "/port" -> {
                        CLIENT_PORTS.add(exchange.getRemoteAddress().getPort());
                        exchange.sendResponseHeaders(200, 2);
                        exchange.getResponseBody().write("ok".getBytes());
                    }
                    case "/hang" -> {
                        HANG.await(30, TimeUnit.SECONDS);
                        exchange.sendResponseHeaders(204, -1);
                    }
                    case "/hang-body" -> {
                        exchange.sendResponseHeaders(200, 10);
                        exchange.getResponseBody().write(new byte[5]);
                        exchange.getResponseBody().flush();
                        HANG.await(30, TimeUnit.SECONDS);
                    }
                    default -> exchange.sendResponseHeaders(404, -1);
                }
            } catch (InterruptedException | IOException e) {
                // The client went away.
            }
        });
    }

    @AfterAll
    static void stop() {
        HANG.countDown();
        server.close();
    }

    static String sha(byte[] data) {
        try {
            return java.util.HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(data));
        } catch (java.security.NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

    @Test
    void streamingUploadAndDownloadRoundTrip() throws Exception {
        byte[] upload = new byte[1024 * 1024 + 17];
        new Random(3).nextBytes(upload);
        try (AwsCrtHttpClient client = AwsCrtHttpClient.create()) {
            SdkHttpRequest request = server.request("http", "127.0.0.1", "PUT", "/echo")
                                           .addRawQueryParameter("a%20b", "c%2Fd")
                                           .body(HttpBody.streaming(StreamingBodies.fromInputStream(
                                               new ByteArrayInputStream(upload), upload.length, null)))
                                           .build();
            try (SyncHttpExchange exchange = client.prepare(request, HttpExecutionOptions.defaults())) {
                SdkHttpResponse response = exchange.execute();
                assertEquals(200, response.statusCode());
                assertEquals(sha(upload), response.headers().firstValue("X-Sha").orElseThrow());
                assertEquals("/echo?a%20b=c%2Fd", response.headers().firstValue("X-Target").orElseThrow());
                assertArrayEquals(upload, TestServers.readAll(response.body()));
            }
            try (SyncHttpExchange exchange = client.prepare(server.get("/large").build(),
                                                            HttpExecutionOptions.defaults())) {
                assertArrayEquals(LARGE, TestServers.readAll(exchange.execute().body()));
            }
        }
    }

    @Test
    void requestIsSentExactlyAndRepeatedResponseHeadersKeepOrder() throws Exception {
        try (TestServers.RawServer raw = new TestServers.RawServer();
             AwsCrtHttpClient client = AwsCrtHttpClient.create()) {
            SdkHttpRequest request = SdkHttpRequest.builder().method("POST").scheme("http").host("127.0.0.1")
                                                   .port(raw.port()).encodedPath("/p%2Fq/r")
                                                   .addRawQueryParameter("x", "1").addRawQueryParameter("x", "2")
                                                   .addRawQueryParameter("flag", null)
                                                   .addHeader("X-B", "1").addHeader("x-a", "2")
                                                   .addHeader("X-B", "3")
                                                   .body(HttpBody.fromUtf8("hello")).build();
            try (SyncHttpExchange exchange = client.prepare(request, HttpExecutionOptions.defaults())) {
                SdkHttpResponse response = exchange.execute();
                assertEquals(List.of("a", "b", "c"), response.headers().allValues("x-rep"));
                assertArrayEquals("ok".getBytes(), TestServers.readAll(response.body()));
            }
            String head = raw.lastHead.get();
            String authority = "127.0.0.1:" + raw.port();
            assertEquals("POST /p%2Fq/r?x=1&x=2&flag HTTP/1.1\r\nHost: " + authority
                         + "\r\nX-B: 1\r\nx-a: 2\r\nX-B: 3\r\nContent-Length: 5\r\n\r\n", head);
        }
    }

    @Test
    void responseBufferingIsBoundedByTheReadWindow() throws Exception {
        long window = 64 * 1024;
        try (AwsCrtHttpClient client = AwsCrtHttpClient.builder().readBufferSizeInBytes(window).build()) {
            assertEquals(window, client.capabilities().maxBufferedBytesPerExchange());
            try (SyncHttpExchange exchange = client.prepare(server.get("/large").build(),
                                                            HttpExecutionOptions.defaults())) {
                InputStream in = exchange.execute().body().streamingBody().openStream();
                assertTrue(in.read() >= 0);
                Thread.sleep(500);
                long buffered = ((CrtSyncExchange) exchange).maxBufferedBytes();
                assertTrue(buffered <= window, "buffered " + buffered);
                byte[] rest = in.readAllBytes();
                assertEquals(LARGE.length - 1, rest.length);
                assertTrue(((CrtSyncExchange) exchange).maxBufferedBytes() <= window);
            }
        }
    }

    /**
     * Regression: the CRT used to return the connection to the pool as soon as it had received the whole response,
     * so an early close after that reused it. The window (16 MiB default) holds the whole 64 KiB body, and the test
     * waits until it has all arrived before closing early.
     */
    @Test
    void earlyCloseAfterTheWholeBodyArrivedDiscardsTheConnection() throws Exception {
        try (AwsCrtHttpClient client = AwsCrtHttpClient.builder().maxConcurrency(1).build()) {
            CLIENT_PORTS.clear();
            try (SyncHttpExchange exchange = client.prepare(server.get("/medium").build(),
                                                            HttpExecutionOptions.defaults())) {
                InputStream in = exchange.execute().body().streamingBody().openStream();
                assertTrue(in.read() >= 0);
                long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(5);
                while (in.available() < MEDIUM.length - 1 && System.nanoTime() < deadline) {
                    Thread.sleep(10);
                }
                assertEquals(MEDIUM.length - 1, in.available(), "the whole body arrived before the early close");
                in.close();
            }
            assertArrayEquals("ok".getBytes(), readPort(client));
            assertEquals(2, CLIENT_PORTS.size());
            assertTrue(!CLIENT_PORTS.get(0).equals(CLIENT_PORTS.get(1)),
                       "an early-closed connection is discarded, not reused: " + CLIENT_PORTS);
        }
    }

    /** A body read to its end (and an exchange closed after that) returns the connection for reuse. */
    @Test
    void fullyReadResponsesReuseTheConnection() throws Exception {
        try (AwsCrtHttpClient client = AwsCrtHttpClient.builder().maxConcurrency(1).build()) {
            CLIENT_PORTS.clear();
            try (SyncHttpExchange exchange = client.prepare(server.get("/medium").build(),
                                                            HttpExecutionOptions.defaults())) {
                InputStream in = exchange.execute().body().streamingBody().openStream();
                assertEquals(MEDIUM.length, in.readNBytes(MEDIUM.length).length);
                // Closed without having seen -1: still fully read.
            }
            assertArrayEquals("ok".getBytes(), readPort(client));
            assertArrayEquals("ok".getBytes(), readPort(client));
            assertEquals(3, CLIENT_PORTS.size());
            assertEquals(1, new java.util.HashSet<>(CLIENT_PORTS).size(), "one reused connection: " + CLIENT_PORTS);
        }
    }

    private static byte[] readPort(AwsCrtHttpClient client) throws IOException {
        try (SyncHttpExchange exchange = client.prepare(server.get("/port").build(), HttpExecutionOptions.defaults())) {
            return TestServers.readAll(exchange.execute().body());
        }
    }

    @Test
    void abortDuringExecuteFailsWithinTheCancellationTimeout() throws Exception {
        try (AwsCrtHttpClient client = AwsCrtHttpClient.create()) {
            SyncHttpExchange exchange = client.prepare(server.get("/hang").build(), HttpExecutionOptions.defaults());
            CompletableFuture<Throwable> result = CompletableFuture.supplyAsync(() -> {
                try {
                    exchange.execute();
                    return null;
                } catch (Throwable t) {
                    return t;
                }
            });
            Thread.sleep(300);
            long start = System.nanoTime();
            exchange.abort();
            Throwable t = result.get(client.capabilities().cancellationTimeout().toMillis(), TimeUnit.MILLISECONDS);
            assertTrue(Duration.ofNanos(System.nanoTime() - start).compareTo(client.capabilities()
                                                                                    .cancellationTimeout()) < 0);
            HttpTransportException e = (HttpTransportException) t;
            assertEquals(HttpFailureCategory.CANCELLED, e.category());
            exchange.close();
        }
    }

    @Test
    void abortDuringBodyReadFailsTheRead() throws Exception {
        try (AwsCrtHttpClient client = AwsCrtHttpClient.create();
             SyncHttpExchange exchange = client.prepare(server.get("/hang-body").build(),
                                                        HttpExecutionOptions.defaults())) {
            InputStream in = exchange.execute().body().streamingBody().openStream();
            assertEquals(5, in.readNBytes(5).length);
            CompletableFuture.delayedExecutor(200, TimeUnit.MILLISECONDS).execute(exchange::abort);
            HttpTransportException e = assertThrows(HttpTransportException.class, in::read);
            assertEquals(HttpFailureCategory.CANCELLED, e.category());
            assertEquals(HttpPhase.RESPONSE_CONSUMPTION, e.phase());
        }
    }

    @Test
    void acquisitionTimeoutIsATimeoutInTheAcquisitionPhase() throws Exception {
        try (AwsCrtHttpClient client = AwsCrtHttpClient.builder().maxConcurrency(1)
                                                       .connectionAcquisitionTimeout(Duration.ofMillis(300)).build()) {
            SyncHttpExchange hanging = client.prepare(server.get("/hang").build(), HttpExecutionOptions.defaults());
            CompletableFuture.runAsync(() -> {
                try {
                    hanging.execute();
                } catch (RuntimeException e) {
                    // aborted below
                }
            });
            Thread.sleep(300);
            try (SyncHttpExchange second = client.prepare(server.get("/echo").build(),
                                                          HttpExecutionOptions.defaults())) {
                HttpTransportException e = assertThrows(HttpTransportException.class, second::execute);
                assertEquals(HttpFailureCategory.TIMEOUT, e.category());
                assertEquals(HttpPhase.CONNECTION_ACQUISITION, e.phase());
            }
            try (SyncHttpExchange third = client.prepare(
                server.get("/echo").build(),
                HttpExecutionOptions.builder().connectionAcquisitionTimeout(Duration.ofMillis(100)).build())) {
                HttpTransportException e = assertThrows(HttpTransportException.class, third::execute);
                assertEquals(HttpFailureCategory.TIMEOUT, e.category());
                assertEquals(HttpPhase.CONNECTION_ACQUISITION, e.phase());
            }
            hanging.abort();
            hanging.close();
        }
    }

    @Test
    void refusedConnectionIsAConnectFailure() throws Exception {
        int port;
        try (ServerSocket s = new ServerSocket(0)) {
            port = s.getLocalPort();
        }
        try (AwsCrtHttpClient client = AwsCrtHttpClient.create();
             SyncHttpExchange exchange = client.prepare(
                 SdkHttpRequest.builder().method("GET").scheme("http").host("127.0.0.1").port(port)
                               .encodedPath("/").build(), HttpExecutionOptions.defaults())) {
            HttpTransportException e = assertThrows(HttpTransportException.class, exchange::execute);
            assertEquals(HttpFailureCategory.CONNECT, e.category());
            assertEquals(AwsCrtHttpClient.CLIENT_NAME, e.clientName().orElseThrow());
        }
    }

    @Test
    void closedClientRejectsNewExchangesAndCancelsInFlightOnes() throws Exception {
        AwsCrtHttpClient client = AwsCrtHttpClient.create();
        SyncHttpExchange exchange = client.prepare(server.get("/hang").build(), HttpExecutionOptions.defaults());
        CompletableFuture<Throwable> result = CompletableFuture.supplyAsync(() -> {
            try {
                exchange.execute();
                return null;
            } catch (Throwable t) {
                return t;
            }
        });
        Thread.sleep(300);
        client.close();
        client.close();
        Throwable t = result.get(2, TimeUnit.SECONDS);
        assertEquals(HttpFailureCategory.CANCELLED, ((HttpTransportException) t).category());
        assertThrows(IllegalStateException.class,
                     () -> client.prepare(server.get("/echo").build(), HttpExecutionOptions.defaults()));
    }
}
