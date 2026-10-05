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

package software.amazon.awssdk.http.urlconnection;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static software.amazon.awssdk.http.urlconnection.TestRequests.get;
import static software.amazon.awssdk.http.urlconnection.TestRequests.open;
import static software.amazon.awssdk.http.urlconnection.TestRequests.readAll;

import java.io.IOException;
import java.io.InputStream;
import java.net.ServerSocket;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.http.HttpBody;
import software.amazon.awssdk.http.HttpExecutionOptions;
import software.amazon.awssdk.http.HttpFailureCategory;
import software.amazon.awssdk.http.HttpPhase;
import software.amazon.awssdk.http.HttpTransportException;
import software.amazon.awssdk.http.SdkHttpResponse;
import software.amazon.awssdk.http.SyncHttpExchange;

/**
 * Stream-close and connection-reuse semantics (server-side connection counting), abort, close idempotence, timeouts
 * and their failure categories, against {@link RawHttpServer}.
 */
class UrlConnectionExchangeTest {
    private static final Duration DEADLINE = Duration.ofSeconds(10);
    private RawHttpServer server;
    private final UrlConnectionHttpClient client =
        UrlConnectionHttpClient.builder().socketTimeout(Duration.ofSeconds(5)).build();

    @AfterEach
    void tearDown() throws IOException {
        client.close();
        if (server != null) {
            server.close();
        }
    }

    private SdkHttpResponse execute(SyncHttpExchange exchange) {
        return exchange.execute();
    }

    private SyncHttpExchange exchange(String path) {
        return client.prepare(get(server.port(), path).build(), HttpExecutionOptions.defaults());
    }

    /** Serves {@code body} (fixed length) for every request, keeping the connection open. */
    private void serve(String body) throws IOException {
        server = new RawHttpServer((request, out) -> {
            out.write(RawHttpServer.response(200, body));
            return true;
        });
    }

    @Test
    void readToEndThenCloseReusesTheConnection() throws Exception {
        serve("hello");
        try (SyncHttpExchange first = exchange("/1")) {
            assertEquals("hello", readAll(execute(first)));
        }
        try (SyncHttpExchange second = exchange("/2")) {
            assertEquals("hello", readAll(execute(second)));
        }
        assertEquals(1, server.connections());
        assertEquals(1, server.take().connection());
        assertEquals(1, server.take().connection());
    }

    @Test
    void readingExactlyContentLengthBytesWithoutEofStillReuses() throws Exception {
        serve("abcdef");
        try (SyncHttpExchange first = exchange("/1")) {
            InputStream in = open(execute(first));
            assertEquals(6, in.readNBytes(6).length);
            in.close();
        }
        try (SyncHttpExchange second = exchange("/2")) {
            readAll(execute(second));
        }
        assertEquals(1, server.connections());
    }

    @Test
    void chunkedResponseReadToEndReusesTheConnection() throws Exception {
        server = new RawHttpServer((request, out) -> {
            out.write(("HTTP/1.1 200 OK\r\nTransfer-Encoding: chunked\r\n\r\n3\r\nabc\r\n2\r\nde\r\n0\r\n\r\n")
                          .getBytes(StandardCharsets.ISO_8859_1));
            return true;
        });
        for (int i = 0; i < 2; i++) {
            try (SyncHttpExchange exchange = exchange("/")) {
                SdkHttpResponse response = execute(exchange);
                assertEquals(HttpBody.Kind.STREAMING, response.body().kind());
                assertEquals("abcde", readAll(response));
            }
        }
        assertEquals(1, server.connections());
    }

    @Test
    void emptyResponsesReleaseTheConnectionForReuse() throws Exception {
        server = new RawHttpServer((request, out) -> {
            out.write("HTTP/1.1 204 No Content\r\n\r\n".getBytes(StandardCharsets.ISO_8859_1));
            return true;
        });
        for (int i = 0; i < 2; i++) {
            try (SyncHttpExchange exchange = exchange("/")) {
                SdkHttpResponse response = execute(exchange);
                assertEquals(204, response.statusCode());
                assertEquals(HttpBody.Kind.EMPTY, response.body().kind());
            }
        }
        assertEquals(1, server.connections());
    }

    @Test
    void headResponsesAreEmptyDespiteTheirContentLengthAndReuseTheConnection() throws Exception {
        server = new RawHttpServer((request, out) -> {
            out.write("HTTP/1.1 200 OK\r\nContent-Length: 100\r\n\r\n".getBytes(StandardCharsets.ISO_8859_1));
            return true;
        });
        for (int i = 0; i < 2; i++) {
            try (SyncHttpExchange exchange = client.prepare(
                     TestRequests.request("HEAD", "http", server.port(), "/").build(),
                     HttpExecutionOptions.defaults())) {
                SdkHttpResponse response = execute(exchange);
                assertEquals(HttpBody.Kind.EMPTY, response.body().kind());
                assertEquals("100", response.headers().firstValue("Content-Length").orElseThrow());
            }
        }
        assertEquals(1, server.connections());
    }

    @Test
    void earlyCloseOfABufferedRemainderDiscardsTheConnection() throws Exception {
        serve("x".repeat(100));
        try (SyncHttpExchange first = exchange("/1")) {
            InputStream in = open(execute(first));
            assertEquals('x', in.read());
            in.close();
        }
        try (SyncHttpExchange second = exchange("/2")) {
            readAll(execute(second));
        }
        assertEquals(2, server.connections());
    }

    @Test
    void earlyCloseOfALargeUnreceivedBodyDiscardsWithoutDraining() throws Exception {
        CountDownLatch release = new CountDownLatch(1);
        server = new RawHttpServer((request, out) -> {
            if (request.requestLine().startsWith("GET /big")) {
                out.write("HTTP/1.1 200 OK\r\nContent-Length: 10000000\r\n\r\n".getBytes(StandardCharsets.ISO_8859_1));
                out.write(new byte[16 * 1024]);
                out.flush();
                awaitQuietly(release);
                return false;
            }
            out.write(RawHttpServer.response(200, "small"));
            return true;
        });
        assertTimeoutPreemptively(DEADLINE, () -> {
            try (SyncHttpExchange first = exchange("/big")) {
                InputStream in = open(execute(first));
                assertEquals(1024, in.readNBytes(1024).length);
                in.close();
            }
            try (SyncHttpExchange second = exchange("/small")) {
                assertEquals("small", readAll(execute(second)));
            }
        });
        release.countDown();
        assertEquals(2, server.connections());
    }

    @Test
    void closingTheExchangeWithoutOpeningTheBodyDiscardsTheConnection() throws Exception {
        serve("x".repeat(100));
        try (SyncHttpExchange first = exchange("/1")) {
            assertEquals(200, execute(first).statusCode());
        }
        try (SyncHttpExchange second = exchange("/2")) {
            readAll(execute(second));
        }
        assertEquals(2, server.connections());
    }

    @Test
    void abortAfterTheResponseDiscardsAndFailsLaterReads() throws Exception {
        serve("x".repeat(100));
        try (SyncHttpExchange first = exchange("/1")) {
            InputStream in = open(execute(first));
            first.abort();
            HttpTransportException e = assertThrows(HttpTransportException.class, in::read);
            assertEquals(HttpFailureCategory.CANCELLED, e.category());
            assertEquals(HttpPhase.RESPONSE_CONSUMPTION, e.phase());
            in.close();
        }
        try (SyncHttpExchange second = exchange("/2")) {
            readAll(execute(second));
        }
        assertEquals(2, server.connections());
    }

    @Test
    void abortBeforeExecuteFailsWithoutConnecting() throws Exception {
        serve("x");
        try (SyncHttpExchange exchange = exchange("/")) {
            exchange.abort();
            exchange.abort();
            HttpTransportException e = assertThrows(HttpTransportException.class, exchange::execute);
            assertEquals(HttpFailureCategory.CANCELLED, e.category());
        }
        assertEquals(0, server.connections());
    }

    @Test
    void abortFromAnotherThreadReleasesABlockedExecuteWithinTheCancellationTimeout() throws Exception {
        CountDownLatch received = new CountDownLatch(1);
        CountDownLatch release = new CountDownLatch(1);
        server = new RawHttpServer((request, out) -> {
            received.countDown();
            awaitQuietly(release);
            return false;
        });
        try (SyncHttpExchange exchange = exchange("/")) {
            CompletableFuture<Throwable> result = CompletableFuture.supplyAsync(() -> {
                try {
                    exchange.execute();
                    return null;
                } catch (Throwable t) {
                    return t;
                }
            });
            assertTrue(received.await(5, TimeUnit.SECONDS));
            long start = System.nanoTime();
            exchange.abort();
            Throwable failure = result.get(client.capabilities().cancellationTimeout().toMillis(),
                                           TimeUnit.MILLISECONDS);
            assertTrue(Duration.ofNanos(System.nanoTime() - start).compareTo(Duration.ofSeconds(2)) < 0);
            HttpTransportException e = (HttpTransportException) failure;
            assertEquals(HttpFailureCategory.CANCELLED, e.category());
            assertEquals(HttpPhase.RESPONSE_CONSUMPTION, e.phase());
        } finally {
            release.countDown();
        }
    }

    @Test
    void abortDuringABlockedBodyReadDoesNotBlockAndFailsTheReadWhenItReturns() throws Exception {
        CountDownLatch more = new CountDownLatch(1);
        server = new RawHttpServer((request, out) -> {
            out.write("HTTP/1.1 200 OK\r\nContent-Length: 10\r\n\r\nab".getBytes(StandardCharsets.ISO_8859_1));
            out.flush();
            awaitQuietly(more);
            out.write("cdefghij".getBytes(StandardCharsets.ISO_8859_1));
            return true;
        });
        try (SyncHttpExchange exchange = exchange("/")) {
            InputStream in = open(execute(exchange));
            assertEquals(2, in.readNBytes(2).length);
            CompletableFuture<Throwable> read = CompletableFuture.supplyAsync(() -> {
                try {
                    in.read();
                    return null;
                } catch (Throwable t) {
                    return t;
                }
            });
            Thread.sleep(200);
            assertTimeoutPreemptively(Duration.ofSeconds(1), exchange::abort);
            more.countDown();
            HttpTransportException e = (HttpTransportException) read.get(5, TimeUnit.SECONDS);
            assertEquals(HttpFailureCategory.CANCELLED, e.category());
        }
    }

    @Test
    void closingTheClientCancelsExchangesInFlight() throws Exception {
        CountDownLatch received = new CountDownLatch(1);
        CountDownLatch release = new CountDownLatch(1);
        server = new RawHttpServer((request, out) -> {
            received.countDown();
            awaitQuietly(release);
            return false;
        });
        try {
            SyncHttpExchange exchange = exchange("/");
            CompletableFuture<Throwable> result = CompletableFuture.supplyAsync(() -> {
                try {
                    exchange.execute();
                    return null;
                } catch (Throwable t) {
                    return t;
                }
            });
            assertTrue(received.await(5, TimeUnit.SECONDS));
            client.close();
            HttpTransportException e = (HttpTransportException) result.get(2, TimeUnit.SECONDS);
            assertEquals(HttpFailureCategory.CANCELLED, e.category());
            exchange.close();
        } finally {
            release.countDown();
        }
    }

    @Test
    void closeIsIdempotentAndExecuteRunsAtMostOnce() throws Exception {
        serve("ok");
        SyncHttpExchange exchange = exchange("/");
        SdkHttpResponse response = execute(exchange);
        assertThrows(IllegalStateException.class, exchange::execute);
        InputStream in = open(response);
        exchange.close();
        exchange.close();
        in.close();
        exchange.abort();
        assertThrows(java.io.IOException.class, in::read);

        SyncHttpExchange closedFirst = exchange("/");
        closedFirst.close();
        assertThrows(IllegalStateException.class, closedFirst::execute);

        client.close();
        client.close();
        assertThrows(IllegalStateException.class, () -> exchange("/"));
    }

    @Test
    void readTimeoutWaitingForTheStatusIsATimeoutInResponseConsumption() throws Exception {
        CountDownLatch release = new CountDownLatch(1);
        server = new RawHttpServer((request, out) -> {
            awaitQuietly(release);
            return false;
        });
        try (UrlConnectionHttpClient fast = UrlConnectionHttpClient.builder().socketTimeout(Duration.ofMillis(300))
                                                                    .build();
             SyncHttpExchange exchange = fast.prepare(get(server.port(), "/").build(),
                                                      HttpExecutionOptions.defaults())) {
            long start = System.nanoTime();
            HttpTransportException e = assertThrows(HttpTransportException.class, exchange::execute);
            assertTrue(Duration.ofNanos(System.nanoTime() - start).compareTo(Duration.ofSeconds(3)) < 0);
            assertEquals(HttpFailureCategory.TIMEOUT, e.category());
            assertEquals(HttpPhase.RESPONSE_CONSUMPTION, e.phase());
            assertTrue(e.retryable());
        } finally {
            release.countDown();
        }
    }

    @Test
    void perExchangeReadTimeoutOverridesTheClientDefault() throws Exception {
        CountDownLatch release = new CountDownLatch(1);
        server = new RawHttpServer((request, out) -> {
            awaitQuietly(release);
            return false;
        });
        try (SyncHttpExchange exchange = client.prepare(
                 get(server.port(), "/").build(),
                 HttpExecutionOptions.builder().readTimeout(Duration.ofMillis(200)).build())) {
            HttpTransportException e = assertTimeoutPreemptively(
                Duration.ofSeconds(3), () -> assertThrows(HttpTransportException.class, exchange::execute));
            assertEquals(HttpFailureCategory.TIMEOUT, e.category());
        } finally {
            release.countDown();
        }
    }

    @Test
    void readTimeoutInTheBodyIsATimeoutInResponseConsumption() throws Exception {
        CountDownLatch release = new CountDownLatch(1);
        server = new RawHttpServer((request, out) -> {
            out.write("HTTP/1.1 200 OK\r\nContent-Length: 10\r\n\r\nab".getBytes(StandardCharsets.ISO_8859_1));
            out.flush();
            awaitQuietly(release);
            return false;
        });
        try (SyncHttpExchange exchange = client.prepare(
                 get(server.port(), "/").build(),
                 HttpExecutionOptions.builder().readTimeout(Duration.ofMillis(300)).build())) {
            InputStream in = open(execute(exchange));
            in.readNBytes(2);
            HttpTransportException e = assertThrows(HttpTransportException.class, in::read);
            assertEquals(HttpFailureCategory.TIMEOUT, e.category());
            assertEquals(HttpPhase.RESPONSE_CONSUMPTION, e.phase());
        } finally {
            release.countDown();
        }
    }

    @Test
    void refusedConnectionIsAConnectFailure() throws Exception {
        int port;
        try (ServerSocket s = new ServerSocket(0)) {
            port = s.getLocalPort();
        }
        try (SyncHttpExchange exchange = client.prepare(get(port, "/").build(), HttpExecutionOptions.defaults())) {
            HttpTransportException e = assertThrows(HttpTransportException.class, exchange::execute);
            assertEquals(HttpFailureCategory.CONNECT, e.category());
            assertEquals(HttpPhase.CONNECT, e.phase());
            assertEquals("UrlConnection", e.clientName().orElseThrow());
        }
    }

    @Test
    void malformedStatusLineIsAProtocolFailure() throws Exception {
        server = new RawHttpServer((request, out) -> {
            out.write("NOT-HTTP garbage\r\n\r\n".getBytes(StandardCharsets.ISO_8859_1));
            return false;
        });
        try (SyncHttpExchange exchange = exchange("/")) {
            HttpTransportException e = assertThrows(HttpTransportException.class, exchange::execute);
            assertEquals(HttpFailureCategory.PROTOCOL, e.category(), e::getMessage);
        }
    }

    @Test
    void connectionClosedBeforeTheDeclaredLengthIsAResponseConsumptionFailure() throws Exception {
        server = new RawHttpServer((request, out) -> {
            out.write("HTTP/1.1 200 OK\r\nContent-Length: 10\r\n\r\nab".getBytes(StandardCharsets.ISO_8859_1));
            return false;
        });
        try (SyncHttpExchange exchange = exchange("/")) {
            InputStream in = open(execute(exchange));
            HttpTransportException e = assertThrows(HttpTransportException.class, in::readAllBytes);
            assertEquals(HttpFailureCategory.RESPONSE_CONSUMPTION, e.category());
        }
    }

    @Test
    void failingRequestBodySourceIsARequestTransmissionFailure() throws Exception {
        serve("ok");
        InputStream failing = new InputStream() {
            @Override
            public int read() throws IOException {
                throw new IOException("source failed");
            }
        };
        try (SyncHttpExchange exchange = client.prepare(
                 TestRequests.request("PUT", "http", server.port(), "/")
                             .body(HttpBody.streaming(software.amazon.awssdk.http.StreamingBodies
                                                          .fromInputStream(failing, 10, null)))
                             .build(), HttpExecutionOptions.defaults())) {
            HttpTransportException e = assertThrows(HttpTransportException.class, exchange::execute);
            assertEquals(HttpFailureCategory.REQUEST_TRANSMISSION, e.category());
            assertEquals(HttpPhase.REQUEST_TRANSMISSION, e.phase());
        }
    }

    @Test
    void errorResponsesExposeTheirBody() throws Exception {
        server = new RawHttpServer((request, out) -> {
            out.write(RawHttpServer.response(503, "busy"));
            return true;
        });
        try (SyncHttpExchange exchange = exchange("/")) {
            SdkHttpResponse response = execute(exchange);
            assertEquals(503, response.statusCode());
            assertEquals("busy", readAll(response));
        }
    }

    private static void awaitQuietly(CountDownLatch latch) {
        try {
            latch.await(20, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
