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

package software.amazon.awssdk.http.tck;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static software.amazon.awssdk.http.tck.TckSupport.SHORT_TIMEOUT;
import static software.amazon.awssdk.http.tck.TckSupport.SLACK;
import static software.amazon.awssdk.http.tck.TckSupport.WAIT;
import static software.amazon.awssdk.http.tck.TckSupport.await;
import static software.amazon.awssdk.http.tck.TckSupport.awaitQuietly;
import static software.amazon.awssdk.http.tck.TckSupport.bytes;
import static software.amazon.awssdk.http.tck.TckSupport.get;
import static software.amazon.awssdk.http.tck.TckSupport.require;
import static software.amazon.awssdk.http.tck.TckSupport.requireNo;
import static software.amazon.awssdk.http.tck.TckSupport.requireTestTrust;
import static software.amazon.awssdk.http.tck.TckSupport.serving;
import static software.amazon.awssdk.http.tck.TckSupport.transportFailure;
import static software.amazon.awssdk.http.tck.TckSupport.utf8;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.Timeout;
import software.amazon.awssdk.http.HttpBody;
import software.amazon.awssdk.http.HttpClientCapabilities;
import software.amazon.awssdk.http.HttpClientOption;
import software.amazon.awssdk.http.HttpExecutionOptions;
import software.amazon.awssdk.http.HttpFailureCategory;
import software.amazon.awssdk.http.HttpHeaders;
import software.amazon.awssdk.http.HttpPhase;
import software.amazon.awssdk.http.HttpTransportException;
import software.amazon.awssdk.http.HttpTransportMode;
import software.amazon.awssdk.http.Protocol;
import software.amazon.awssdk.http.SdkHttpRequest;
import software.amazon.awssdk.http.SdkHttpResponse;
import software.amazon.awssdk.http.SdkSyncHttpClient;
import software.amazon.awssdk.http.StreamingBodies;
import software.amazon.awssdk.http.SyncHttpExchange;

/**
 * The sync HTTP SPI TCK (Requirement 12). A concrete subclass per transport supplies the {@link TransportUnderTest};
 * every check reads the capabilities the built client declares and is skipped, with its reason, when it depends on an
 * undeclared option or a documented {@link Deviation}. See {@link TckSupport} for the gating model.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@Timeout(value = 30, unit = TimeUnit.SECONDS)
abstract class SyncHttpClientTck {

    private final List<AutoCloseable> resources = Collections.synchronizedList(new ArrayList<>());
    private final ExecutorService threads = Executors.newCachedThreadPool(r -> {
        Thread t = new Thread(r, "sync-tck-caller");
        t.setDaemon(true);
        return t;
    });
    private SdkSyncHttpClient client;
    private HttpClientCapabilities capabilities;

    /** The transport under test. */
    abstract TransportUnderTest<SdkSyncHttpClient> transport();

    @BeforeAll
    void createSharedClient() {
        client = transport().create(ClientSettings.defaults());
        capabilities = client.capabilities();
    }

    @AfterAll
    void closeSharedClient() {
        client.close();
        threads.shutdownNow();
    }

    @AfterEach
    void closeResources() throws Exception {
        List<AutoCloseable> toClose;
        synchronized (resources) {
            toClose = new ArrayList<>(resources);
            resources.clear();
        }
        Collections.reverse(toClose);
        for (AutoCloseable resource : toClose) {
            resource.close();
        }
    }

    // ---------------------------------------------------------------------------------------------------------------
    // Declared capabilities

    @Test
    void capabilities_declareSyncModeHttp11AndPositiveBounds() {
        assertEquals(HttpTransportMode.SYNC, transport().mode());
        assertEquals(HttpTransportMode.SYNC, capabilities.mode());
        assertTrue(capabilities.supports(Protocol.HTTP1_1), "every launch transport speaks HTTP/1.1");
        assertTrue(capabilities.maxBufferedBytesPerExchange() > 0);
        assertTrue(capabilities.cancellationTimeout().compareTo(Duration.ZERO) > 0);
    }

    // ---------------------------------------------------------------------------------------------------------------
    // Request representation

    @Test
    void request_methodRawTargetAndOrderedRepeatedHeaders_arePreservedOnTheWire() throws Exception {
        TckServer server = server(serving("ok"));
        for (String method : List.of("GET", "DELETE", "OPTIONS")) {
            SdkHttpRequest request = SdkHttpRequest.builder().method(method).scheme("http").host("localhost")
                                                   .port(server.port()).encodedPath("/tck/a%2Fb/%7Ec")
                                                   .addRawQueryParameter("x", "1").addRawQueryParameter("x", "2")
                                                   .addRawQueryParameter("y", "%20z")
                                                   .addHeader("X-Tck-Repeat", "one")
                                                   .addHeader("x-tck-other", "two")
                                                   .addHeader("X-TCK-REPEAT", "three")
                                                   .build();
            assertEquals("ok", utf8(call(client, request)));
            TckServer.Head head = server.take();
            assertEquals(method, head.method());
            assertEquals("/tck/a%2Fb/%7Ec?x=1&x=2&y=%20z", head.target());
            assertEquals(List.of("X-Tck-Repeat: one", "x-tck-other: two", "X-TCK-REPEAT: three"),
                         tckHeaderLines(head.headerLines()), "header order, repeats and name case");
        }
    }

    @Test
    void request_knownLengthBodies_areSentWithTheirContentLength() throws Exception {
        TckServer server = server(serving("ok"));
        byte[] payload = bytes(10_000);
        for (HttpBody body : List.of(HttpBody.fromBytes(payload),
                                     HttpBody.streaming(StreamingBodies.fromInputStream(
                                         new ByteArrayInputStream(payload), payload.length, "application/x-tck")))) {
            call(client, put(server, body));
            assertEquals(String.valueOf(payload.length), server.take().header("Content-Length"));
            assertArrayEquals(payload, server.takeBody());
        }
    }

    @Test
    void request_unknownLengthBody_isStreamedChunkedOrFailsFastWhereDocumented() throws Exception {
        TckServer server = server(serving("ok"));
        byte[] payload = bytes(70_000);
        SdkHttpRequest request = put(server, HttpBody.streaming(StreamingBodies.fromInputStream(
            new ByteArrayInputStream(payload), -1, "application/x-tck")));
        if (transport().documents(Deviation.UNKNOWN_LENGTH_REQUEST_BODY_FAILS_FAST)) {
            try (SyncHttpExchange exchange = client.prepare(request, HttpExecutionOptions.defaults())) {
                transportFailure(assertThrows(RuntimeException.class, exchange::execute),
                                 HttpFailureCategory.REQUEST_TRANSMISSION);
            }
            return;
        }
        call(client, request);
        TckServer.Head head = server.take();
        assertEquals("chunked", head.header("Transfer-Encoding").toLowerCase(Locale.ROOT));
        assertArrayEquals(payload, server.takeBody());
    }

    // ---------------------------------------------------------------------------------------------------------------
    // Response representation

    @Test
    void response_statusRepeatedHeadersAndStreamingBody_arePreserved() throws Exception {
        TckServer server = server(ex -> {
            ex.out().write(TckServer.ascii("HTTP/1.1 201 Created\r\nX-Tck-R: 1\r\nx-tck-other: 2\r\nX-TCK-R: 3\r\n"
                                           + "Transfer-Encoding: chunked\r\n\r\n5\r\nhello\r\n6\r\n world\r\n0\r\n\r\n"));
            return true;
        });
        try (SyncHttpExchange exchange = client.prepare(get(server, "/").build(), HttpExecutionOptions.defaults())) {
            SdkHttpResponse response = exchange.execute();
            assertEquals(201, response.statusCode());
            assertEquals(List.of("X-Tck-R: 1", "x-tck-other: 2", "X-TCK-R: 3"), tckHeaderLines(response.headers()));
            assertEquals(HttpBody.Kind.STREAMING, response.body().kind());
            assertEquals("hello world", utf8(readAll(response)));
        }
    }

    // ---------------------------------------------------------------------------------------------------------------
    // Cancellation (universal)

    @Test
    void cancel_beforeExecute_failsCancelledWithoutConnecting() throws Exception {
        TckServer server = server(serving("never"));
        try (SyncHttpExchange exchange = client.prepare(get(server, "/").build(), HttpExecutionOptions.defaults())) {
            exchange.abort();
            exchange.abort();
            transportFailure(assertThrows(RuntimeException.class, exchange::execute), HttpFailureCategory.CANCELLED);
        }
        assertEquals(0, server.connections());
    }

    @Test
    void cancel_whilePendingAcquisition_failsCancelledWithinTheCancellationTimeout() throws Exception {
        require(transport(), capabilities, HttpClientOption.MAX_CONNECTIONS);
        CountDownLatch received = new CountDownLatch(1);
        CountDownLatch release = new CountDownLatch(1);
        TckServer server = server(holdingFirst(received, release));
        SdkSyncHttpClient pool = client(ClientSettings.defaults().maxConnections(1));
        SyncHttpExchange holder = track(pool.prepare(get(server, "/hold").build(), HttpExecutionOptions.defaults()));
        CompletableFuture<Throwable> held = inBackground(() -> readAll(holder.execute()));
        await(received);
        SyncHttpExchange pending = track(pool.prepare(get(server, "/pending").build(), HttpExecutionOptions.defaults()));
        CompletableFuture<Throwable> result = inBackground(pending::execute);
        Thread.sleep(200);
        long start = System.nanoTime();
        pending.abort();
        transportFailure(within(result, start), HttpFailureCategory.CANCELLED);
        release.countDown();
        assertEquals(null, held.get(WAIT.toMillis(), TimeUnit.MILLISECONDS));
        assertEquals(1, server.connections(), "the pending exchange never connected");
    }

    @Test
    void cancel_duringRequestTransmission_failsCancelledWithinTheCancellationTimeout() throws Exception {
        CountDownLatch received = new CountDownLatch(1);
        CountDownLatch release = new CountDownLatch(1);
        TckServer server = server(ex -> {
            received.countDown();
            awaitQuietly(release);
            return false;
        });
        try (SyncHttpExchange exchange = client.prepare(put(server, HttpBody.fromBytes(new byte[16 * 1024 * 1024])),
                                                        HttpExecutionOptions.defaults())) {
            CompletableFuture<Throwable> result = inBackground(exchange::execute);
            await(received);
            Thread.sleep(200);
            long start = System.nanoTime();
            exchange.abort();
            transportFailure(within(result, start), HttpFailureCategory.CANCELLED);
        } finally {
            release.countDown();
        }
    }

    @Test
    void cancel_duringABlockedResponseRead_failsCancelledWithinTheCancellationTimeout() throws Exception {
        requireNo(transport(), Deviation.ABORT_CANNOT_INTERRUPT_BLOCKED_BODY_READ);
        CountDownLatch release = new CountDownLatch(1);
        TckServer server = server(partialBodyThenHold(release));
        try (SyncHttpExchange exchange = client.prepare(get(server, "/").build(), HttpExecutionOptions.defaults())) {
            InputStream in = open(exchange.execute());
            assertEquals(10, in.readNBytes(10).length);
            CompletableFuture<Throwable> result = inBackground(in::read);
            Thread.sleep(200);
            long start = System.nanoTime();
            exchange.abort();
            HttpTransportException e = transportFailure(within(result, start), HttpFailureCategory.CANCELLED);
            assertEquals(HttpPhase.RESPONSE_CONSUMPTION, e.phase());
        } finally {
            release.countDown();
        }
    }

    @Test
    void cancel_betweenResponseReads_failsTheNextReadCancelled() throws Exception {
        CountDownLatch release = new CountDownLatch(1);
        TckServer server = server(partialBodyThenHold(release));
        try (SyncHttpExchange exchange = client.prepare(get(server, "/").build(), HttpExecutionOptions.defaults())) {
            InputStream in = open(exchange.execute());
            assertEquals(10, in.readNBytes(10).length);
            exchange.abort();
            long start = System.nanoTime();
            transportFailure(within(inBackground(in::read), start), HttpFailureCategory.CANCELLED);
        } finally {
            release.countDown();
        }
    }

    @Test
    void cancel_racingTheEndOfTheBody_endsInExactlyOneOutcome() throws Exception {
        byte[] payload = bytes(4096);
        TckServer server = server(ex -> {
            ex.out().write(TckServer.response(200, payload));
            return true;
        });
        for (int i = 0; i < 10; i++) {
            SyncHttpExchange exchange = client.prepare(get(server, "/" + i).build(), HttpExecutionOptions.defaults());
            try (exchange) {
                InputStream in = open(exchange.execute());
                CountDownLatch go = new CountDownLatch(1);
                CompletableFuture<Throwable> aborter = inBackground(() -> {
                    go.await();
                    exchange.abort();
                    return null;
                });
                go.countDown();
                try {
                    assertArrayEquals(payload, in.readAllBytes(), "a completed read returns the whole body");
                } catch (IOException | RuntimeException e) {
                    transportFailure(e, HttpFailureCategory.CANCELLED);
                }
                assertEquals(null, aborter.get(WAIT.toMillis(), TimeUnit.MILLISECONDS));
                in.close();
            }
        }
        assertArrayEquals(payload, call(client, get(server, "/after").build()), "the client works after the races");
    }

    // ---------------------------------------------------------------------------------------------------------------
    // Failure categories

    @Test
    void error_refusedConnect_isConnect() throws Exception {
        try (SyncHttpExchange exchange = client.prepare(get("http", TckSupport.refusedPort(), "/").build(),
                                                        HttpExecutionOptions.defaults())) {
            transportFailure(assertThrows(RuntimeException.class, exchange::execute), HttpFailureCategory.CONNECT);
        }
    }

    @Test
    void error_readTimeout_isTimeout() throws Exception {
        require(transport(), capabilities, HttpClientOption.READ_TIMEOUT);
        CountDownLatch release = new CountDownLatch(1);
        TckServer server = server(silentUntil(release));
        SdkSyncHttpClient timed = client(ClientSettings.defaults().readTimeout(SHORT_TIMEOUT));
        try (SyncHttpExchange exchange = timed.prepare(get(server, "/").build(), HttpExecutionOptions.defaults())) {
            long start = System.nanoTime();
            transportFailure(assertThrows(RuntimeException.class, exchange::execute), HttpFailureCategory.TIMEOUT);
            assertElapsedBelow(start, SHORT_TIMEOUT.plus(SLACK));
        } finally {
            release.countDown();
        }
    }

    @Test
    void error_prematureClose_isResponseConsumption() throws Exception {
        TckServer server = server(ex -> {
            ex.out().write(TckServer.ascii("HTTP/1.1 200 TCK\r\nContent-Length: 100\r\n\r\n0123456789"));
            return false;
        });
        try (SyncHttpExchange exchange = client.prepare(get(server, "/").build(), HttpExecutionOptions.defaults())) {
            InputStream in = open(exchange.execute());
            transportFailure(assertThrows(Exception.class, in::readAllBytes), HttpFailureCategory.RESPONSE_CONSUMPTION);
        }
    }

    @Test
    void error_untrustedCertificate_isTls() throws Exception {
        TckServer server = track(TckServer.tls(TckTls.get(), serving("secure")));
        try (SyncHttpExchange exchange = client.prepare(get("https", server.port(), "/").host("127.0.0.1").build(),
                                                        HttpExecutionOptions.defaults())) {
            transportFailure(assertThrows(RuntimeException.class, exchange::execute), HttpFailureCategory.TLS);
        }
    }

    @Test
    void error_untrustedCertificateOnAHostWithAFurtherRefusingAddress_isTls() throws Exception {
        TckSupport.requireMultiAddressLocalhost();
        requireNo(transport(), Deviation.TLS_FAILURE_MASKED_BY_A_LATER_ADDRESS);
        TckServer server = track(TckServer.tls(TckTls.get(), serving("secure")));
        try (SyncHttpExchange exchange = client.prepare(get("https", server.port(), "/").build(),
                                                        HttpExecutionOptions.defaults())) {
            transportFailure(assertThrows(RuntimeException.class, exchange::execute), HttpFailureCategory.TLS);
        }
    }

    // ---------------------------------------------------------------------------------------------------------------
    // TLS, proxy, timeouts (capability gated)

    @Test
    void tls_roundTripWithTestTrust_succeeds() throws Exception {
        requireTestTrust(transport(), capabilities);
        TckServer server = track(TckServer.tls(TckTls.get(), serving("secure")));
        SdkSyncHttpClient trusting = client(ClientSettings.defaults().trust(TckTls.get()));
        assertEquals("secure", utf8(call(trusting, get("https", server.port(), "/tls").build())));
        assertEquals("/tls", server.take().target());
    }

    @Test
    void proxy_plainHttpRequest_isSentThroughTheProxy() throws Exception {
        require(transport(), capabilities, HttpClientOption.PROXY);
        TckServer proxy = server(serving("via-proxy"));
        SdkSyncHttpClient proxied = client(ClientSettings.defaults()
                                                         .proxy(URI.create("http://localhost:" + proxy.port())));
        SdkHttpRequest request = get("http", 8080, "/target").host("tck-target.invalid").build();
        assertEquals("via-proxy", utf8(call(proxied, request)));
        assertEquals("http://tck-target.invalid:8080/target", proxy.take().target());
    }

    @Test
    void timeout_perExchangeReadTimeout_overridesTheClientSetting() throws Exception {
        require(transport(), capabilities, HttpClientOption.READ_TIMEOUT);
        CountDownLatch release = new CountDownLatch(1);
        TckServer server = server(silentUntil(release));
        try (SyncHttpExchange exchange = client.prepare(
            get(server, "/").build(), HttpExecutionOptions.builder().readTimeout(SHORT_TIMEOUT).build())) {
            long start = System.nanoTime();
            transportFailure(assertThrows(RuntimeException.class, exchange::execute), HttpFailureCategory.TIMEOUT);
            assertElapsedBelow(start, SHORT_TIMEOUT.plus(SLACK));
        } finally {
            release.countDown();
        }
    }

    @Test
    void timeout_connectionAcquisition_isTimeoutInTheAcquisitionPhase() throws Exception {
        require(transport(), capabilities, HttpClientOption.MAX_CONNECTIONS);
        require(transport(), capabilities, HttpClientOption.CONNECTION_ACQUISITION_TIMEOUT);
        CountDownLatch received = new CountDownLatch(1);
        CountDownLatch release = new CountDownLatch(1);
        TckServer server = server(holdingFirst(received, release));
        SdkSyncHttpClient pool = client(ClientSettings.defaults().maxConnections(1));
        SyncHttpExchange holder = track(pool.prepare(get(server, "/hold").build(), HttpExecutionOptions.defaults()));
        CompletableFuture<Throwable> held = inBackground(() -> readAll(holder.execute()));
        try {
            await(received);
            try (SyncHttpExchange waiting = pool.prepare(get(server, "/wait").build(), HttpExecutionOptions
                .builder().connectionAcquisitionTimeout(SHORT_TIMEOUT).build())) {
                long start = System.nanoTime();
                HttpTransportException e = transportFailure(assertThrows(RuntimeException.class, waiting::execute),
                                                            HttpFailureCategory.TIMEOUT);
                assertEquals(HttpPhase.CONNECTION_ACQUISITION, e.phase());
                assertElapsedBelow(start, SHORT_TIMEOUT.plus(SLACK));
            }
        } finally {
            release.countDown();
        }
        assertEquals(null, held.get(WAIT.toMillis(), TimeUnit.MILLISECONDS));
    }

    // ---------------------------------------------------------------------------------------------------------------
    // Ownership (universal)

    @Test
    void ownership_readToEndAndClose_reusesTheConnection() throws Exception {
        TckServer server = server(serving("reuse"));
        assertEquals("reuse", utf8(call(client, get(server, "/1").build())));
        assertEquals("reuse", utf8(call(client, get(server, "/2").build())));
        assertEquals(1, server.connections());
    }

    @Test
    void ownership_earlyClose_discardsTheConnection() throws Exception {
        requireNo(transport(), Deviation.JDK_KEEP_ALIVE_CLEANER_MAY_REUSE_EARLY_CLOSED_CONNECTION);
        byte[] large = bytes(256 * 1024);
        TckServer server = server(ex -> {
            ex.body();
            ex.out().write(TckServer.response(200, ex.head().target().equals("/large") ? large : bytes(10)));
            return true;
        });
        try (SyncHttpExchange first = client.prepare(get(server, "/large").build(), HttpExecutionOptions.defaults())) {
            InputStream in = open(first.execute());
            assertTrue(in.read() >= 0);
            in.close();
        }
        assertEquals(10, call(client, get(server, "/small").build()).length);
        assertEquals(2, server.connections(), "an early-closed connection is discarded, not drained and reused");
    }

    @Test
    void ownership_abortAfterTheResponse_discardsTheConnection() throws Exception {
        requireNo(transport(), Deviation.RELEASES_FULLY_RECEIVED_RESPONSE_FOR_REUSE);
        TckServer server = server(serving("x".repeat(1000)));
        try (SyncHttpExchange first = client.prepare(get(server, "/1").build(), HttpExecutionOptions.defaults())) {
            first.execute();
            first.abort();
        }
        assertEquals(1000, call(client, get(server, "/2").build()).length);
        assertEquals(2, server.connections());
    }

    @Test
    void ownership_closeIsIdempotent() throws Exception {
        TckServer server = server(serving("x".repeat(100)));
        SdkSyncHttpClient own = transport().create(ClientSettings.defaults());
        SyncHttpExchange exchange = own.prepare(get(server, "/").build(), HttpExecutionOptions.defaults());
        InputStream in = open(exchange.execute());
        in.close();
        in.close();
        exchange.close();
        exchange.close();
        exchange.abort();
        own.close();
        own.close();
    }

    @Test
    void ownership_closedClient_rejectsNewExchanges() throws Exception {
        TckServer server = server(serving("never"));
        SdkSyncHttpClient own = transport().create(ClientSettings.defaults());
        own.close();
        assertThrows(RuntimeException.class, () -> {
            try (SyncHttpExchange exchange = own.prepare(get(server, "/").build(), HttpExecutionOptions.defaults())) {
                exchange.execute();
            }
        });
        assertEquals(0, server.connections());
    }

    // ---------------------------------------------------------------------------------------------------------------
    // Concurrency (design: >= 2 simultaneous, more total than simultaneous, a barrier proving overlap, bounded)

    @Test
    void concurrency_overlappingExchanges_allComplete() throws Exception {
        int parallel = 2;
        int perCaller = 3;
        CyclicBarrier overlap = new CyclicBarrier(parallel);
        AtomicInteger inFlight = new AtomicInteger();
        AtomicInteger maxInFlight = new AtomicInteger();
        TckServer server = server(ex -> {
            ex.body();
            maxInFlight.accumulateAndGet(inFlight.incrementAndGet(), Math::max);
            overlap.await(5, TimeUnit.SECONDS);
            inFlight.decrementAndGet();
            ex.out().write(TckServer.response(200, ex.head().target()));
            return true;
        });
        List<CompletableFuture<Throwable>> callers = new ArrayList<>();
        for (int c = 0; c < parallel; c++) {
            int caller = c;
            callers.add(inBackground(() -> {
                for (int i = 0; i < perCaller; i++) {
                    String path = "/" + caller + "/" + i;
                    assertEquals(path, utf8(call(client, get(server, path).build())));
                }
                return null;
            }));
        }
        for (CompletableFuture<Throwable> caller : callers) {
            Throwable failure = caller.get(20, TimeUnit.SECONDS);
            if (failure != null) {
                throw new AssertionError("a concurrent caller failed", failure);
            }
        }
        assertEquals(parallel, maxInFlight.get(), "the barrier proved overlapping exchanges");
    }

    // ---------------------------------------------------------------------------------------------------------------
    // Helpers

    private <T extends AutoCloseable> T track(T resource) {
        resources.add(resource);
        return resource;
    }

    private TckServer server(TckServer.Handler handler) throws IOException {
        return track(TckServer.plain(handler));
    }

    private SdkSyncHttpClient client(ClientSettings settings) {
        return track(transport().create(settings));
    }

    private static SdkHttpRequest put(TckServer server, HttpBody body) {
        return get(server, "/upload").method("PUT").body(body).build();
    }

    private static byte[] call(SdkSyncHttpClient client, SdkHttpRequest request) throws IOException {
        try (SyncHttpExchange exchange = client.prepare(request, HttpExecutionOptions.defaults())) {
            return readAll(exchange.execute());
        }
    }

    private static InputStream open(SdkHttpResponse response) {
        assertEquals(HttpBody.Kind.STREAMING, response.body().kind());
        return response.body().streamingBody().openStream();
    }

    private static byte[] readAll(SdkHttpResponse response) throws IOException {
        if (response.body().kind() == HttpBody.Kind.EMPTY) {
            return new byte[0];
        }
        try (InputStream in = response.body().streamingBody().openStream()) {
            return in.readAllBytes();
        }
    }

    private static List<String> tckHeaderLines(List<String> lines) {
        List<String> out = new ArrayList<>();
        for (String line : lines) {
            if (line.toLowerCase(Locale.ROOT).startsWith("x-tck")) {
                int colon = line.indexOf(':');
                out.add(line.substring(0, colon) + ": " + line.substring(colon + 1).trim());
            }
        }
        return out;
    }

    private static List<String> tckHeaderLines(HttpHeaders headers) {
        List<String> lines = new ArrayList<>();
        for (int i = 0; i < headers.size(); i++) {
            lines.add(headers.name(i) + ": " + headers.value(i));
        }
        return tckHeaderLines(lines);
    }

    /** Runs {@code call} on a caller thread; the future holds its failure, or null. */
    private CompletableFuture<Throwable> inBackground(Callable<?> call) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                call.call();
                return null;
            } catch (Throwable t) {
                return t;
            }
        }, threads);
    }

    /** The failure of {@code result}, which must arrive within the declared cancellation timeout of {@code start}. */
    private Throwable within(CompletableFuture<Throwable> result, long start) throws Exception {
        Duration limit = capabilities.cancellationTimeout();
        Throwable failure;
        try {
            failure = result.get(limit.toMillis() + 250, TimeUnit.MILLISECONDS);
        } catch (TimeoutException e) {
            return fail(transport().name() + " did not terminate within the declared cancellation timeout " + limit);
        }
        assertElapsedBelow(start, limit.plusMillis(250));
        if (failure == null) {
            return fail("the cancelled operation completed normally");
        }
        return failure;
    }

    private static void assertElapsedBelow(long startNanos, Duration limit) {
        Duration elapsed = Duration.ofNanos(System.nanoTime() - startNanos);
        assertTrue(elapsed.compareTo(limit) <= 0, () -> "took " + elapsed + ", limit " + limit);
    }

    /** Holds the first request (until {@code release}) and answers the others. */
    private static TckServer.Handler holdingFirst(CountDownLatch received, CountDownLatch release) {
        AtomicInteger count = new AtomicInteger();
        return ex -> {
            ex.body();
            if (count.incrementAndGet() == 1) {
                received.countDown();
                awaitQuietly(release);
            }
            ex.out().write(TckServer.response(200, "held"));
            return true;
        };
    }

    /** Sends the head and 10 of 1000 body bytes, then holds until {@code release}. */
    private static TckServer.Handler partialBodyThenHold(CountDownLatch release) {
        return ex -> {
            ex.body();
            ex.out().write(TckServer.ascii("HTTP/1.1 200 TCK\r\nContent-Length: 1000\r\n\r\n0123456789"));
            ex.out().flush();
            awaitQuietly(release);
            return false;
        };
    }

    /** Reads the request and sends nothing until {@code release}. */
    static TckServer.Handler silentUntil(CountDownLatch release) {
        return ex -> {
            ex.body();
            awaitQuietly(release);
            return false;
        };
    }
}
