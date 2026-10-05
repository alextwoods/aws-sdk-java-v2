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
import static org.junit.jupiter.api.Assertions.assertFalse;
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
import static software.amazon.awssdk.http.tck.TckSupport.requireTestTrust;
import static software.amazon.awssdk.http.tck.TckSupport.serving;
import static software.amazon.awssdk.http.tck.TckSupport.transportFailure;
import static software.amazon.awssdk.http.tck.TckSupport.utf8;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.URI;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.Timeout;
import software.amazon.awssdk.http.AsyncHttpExchange;
import software.amazon.awssdk.http.CancellationReason;
import software.amazon.awssdk.http.HttpBody;
import software.amazon.awssdk.http.HttpClientCapabilities;
import software.amazon.awssdk.http.HttpClientOption;
import software.amazon.awssdk.http.HttpExecutionOptions;
import software.amazon.awssdk.http.HttpFailureCategory;
import software.amazon.awssdk.http.HttpHeaders;
import software.amazon.awssdk.http.HttpTransportMode;
import software.amazon.awssdk.http.Protocol;
import software.amazon.awssdk.http.SdkAsyncHttpClient;
import software.amazon.awssdk.http.SdkHttpRequest;
import software.amazon.awssdk.http.SdkHttpResponse;
import software.amazon.awssdk.http.StreamingBodies;
import software.amazon.awssdk.http.tck.AsyncProbes.Recorder;
import software.amazon.awssdk.http.tck.AsyncProbes.TrackingBody;

/**
 * The async HTTP SPI TCK (Requirement 12). A concrete subclass per transport supplies the {@link TransportUnderTest};
 * capability-dependent checks are gated as in {@link SyncHttpClientTck} (see {@link TckSupport}). Every exchange is
 * observed through {@link AsyncProbes}, and every check ends by asserting the universal async rules on each exchange
 * it started: handler signal order, no response chunk without demand, and exactly one terminal signal accompanying the
 * completion of {@link AsyncHttpExchange#completion()}.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@Timeout(value = 30, unit = TimeUnit.SECONDS)
abstract class AsyncHttpClientTck {

    private final List<AutoCloseable> resources = Collections.synchronizedList(new ArrayList<>());
    private SdkAsyncHttpClient client;
    private HttpClientCapabilities capabilities;

    /** The transport under test. */
    abstract TransportUnderTest<SdkAsyncHttpClient> transport();

    @BeforeAll
    void createSharedClient() {
        client = transport().create(ClientSettings.defaults());
        capabilities = client.capabilities();
    }

    @AfterAll
    void closeSharedClient() {
        client.close();
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
    void capabilities_declareAsyncModeHttp11AndPositiveBounds() {
        assertEquals(HttpTransportMode.ASYNC, transport().mode());
        assertEquals(HttpTransportMode.ASYNC, capabilities.mode());
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
        byte[] payload = bytes(100_000);
        for (HttpBody body : List.of(HttpBody.fromBytes(payload),
                                     HttpBody.streaming(StreamingBodies.fromBytes(payload, "application/x-tck")))) {
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
            Run run = start(client, request, true);
            transportFailure(run.failure(), HttpFailureCategory.REQUEST_TRANSMISSION);
            run.assertOneTerminal();
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
        Run run = start(client, get(server, "/").build(), true);
        SdkHttpResponse response = run.recorder.headers.get(WAIT.toMillis(), TimeUnit.MILLISECONDS);
        assertEquals(201, response.statusCode());
        assertEquals(List.of("X-Tck-R: 1", "x-tck-other: 2", "X-TCK-R: 3"), tckHeaderLines(response.headers()));
        assertEquals("hello world", utf8(run.body()));
        run.assertOneTerminal();
    }

    // ---------------------------------------------------------------------------------------------------------------
    // Demand (universal)

    @Test
    void demand_noResponseChunkWithoutDemand_andNeverMoreThanRequested() throws Exception {
        byte[] payload = bytes(64 * 1024);
        TckServer server = server(ex -> {
            ex.body();
            ex.out().write(TckServer.response(200, payload));
            return true;
        });
        Run run = start(client, get(server, "/").build(), false);
        await(run.recorder.body.subscribed);
        Thread.sleep(300);
        assertEquals(0, run.recorder.body.chunks.get(), "no onNext without demand");
        run.recorder.body.request(1);
        long deadline = System.nanoTime() + WAIT.toNanos();
        while (run.recorder.body.chunks.get() == 0 && System.nanoTime() < deadline) {
            Thread.sleep(10);
        }
        Thread.sleep(200);
        assertEquals(1, run.recorder.body.chunks.get(), "exactly one chunk for a demand of one");
        run.recorder.body.request(Long.MAX_VALUE);
        run.recorder.body.complete.get(WAIT.toMillis(), TimeUnit.MILLISECONDS);
        assertArrayEquals(payload, run.recorder.body.bytes());
        run.assertOneTerminal();
    }

    // ---------------------------------------------------------------------------------------------------------------
    // Cancellation (universal)

    @Test
    void cancel_beforeAcquisition_failsCancelledWithOneTerminalSignal() throws Exception {
        TckServer server = server(serving("never"));
        for (int i = 0; i < 5; i++) {
            Recorder recorder = new Recorder(true);
            AsyncHttpExchange exchange = client.execute(get(server, "/" + i).build(), recorder,
                                                        HttpExecutionOptions.defaults());
            long start = System.nanoTime();
            assertTrue(exchange.cancel(CancellationReason.CALLER_CANCELLED), "the first cancel wins");
            assertFalse(exchange.cancel(CancellationReason.CALLER_CANCELLED), "a second cancel is a no-op");
            Run run = new Run(exchange, recorder);
            transportFailure(within(run, start), HttpFailureCategory.CANCELLED);
            run.assertOneTerminal();
        }
    }

    @Test
    void cancel_whilePendingAcquisition_failsCancelledWithinTheCancellationTimeout() throws Exception {
        require(transport(), capabilities, HttpClientOption.MAX_CONNECTIONS);
        CountDownLatch received = new CountDownLatch(1);
        CountDownLatch release = new CountDownLatch(1);
        TckServer server = server(holdingFirst(received, release));
        SdkAsyncHttpClient pool = client(ClientSettings.defaults().maxConnections(1));
        Run holder = start(pool, get(server, "/hold").build(), true);
        await(received);
        Run pending = start(pool, get(server, "/pending").build(), true);
        Thread.sleep(200);
        long start = System.nanoTime();
        assertTrue(pending.exchange.cancel(CancellationReason.CALLER_CANCELLED));
        transportFailure(within(pending, start), HttpFailureCategory.CANCELLED);
        pending.assertOneTerminal();
        release.countDown();
        assertEquals("held", utf8(holder.body()));
        holder.assertOneTerminal();
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
        TrackingBody body = new TrackingBody(256L * 1024 * 1024, 16 * 1024, true);
        try {
            Run run = start(client, put(server, HttpBody.streaming(body)), true);
            await(received);
            Thread.sleep(200);
            long start = System.nanoTime();
            assertTrue(run.exchange.cancel(CancellationReason.CALLER_CANCELLED));
            transportFailure(within(run, start), HttpFailureCategory.CANCELLED);
            assertEventually(body.cancelled::get, "the request-body subscription is cancelled with the exchange");
            run.assertOneTerminal();
        } finally {
            release.countDown();
        }
    }

    @Test
    void cancel_duringResponseDemand_failsTheBodySubscriberCancelled() throws Exception {
        CountDownLatch release = new CountDownLatch(1);
        TckServer server = server(partialBodyThenHold(release));
        try {
            Run run = start(client, get(server, "/").build(), false);
            await(run.recorder.body.subscribed);
            run.recorder.body.request(1);
            assertEventually(() -> run.recorder.body.received.get() == 10, "the first 10 body bytes arrive");
            run.recorder.body.request(1);
            Thread.sleep(200);
            long start = System.nanoTime();
            assertTrue(run.exchange.cancel(CancellationReason.CALLER_CANCELLED));
            transportFailure(within(run, start), HttpFailureCategory.CANCELLED);
            Throwable bodyError = run.recorder.body.error.get(WAIT.toMillis(), TimeUnit.MILLISECONDS);
            transportFailure(bodyError, HttpFailureCategory.CANCELLED);
            assertFalse(run.recorder.handlerError.isDone(), "after onStream failures go to the body subscriber");
            run.assertOneTerminal();
        } finally {
            release.countDown();
        }
    }

    @Test
    void cancel_racingTheTerminalSignal_endsInExactlyOneOutcome() throws Exception {
        byte[] payload = bytes(4096);
        TckServer server = server(ex -> {
            ex.body();
            ex.out().write(TckServer.response(200, payload));
            return true;
        });
        for (int i = 0; i < 20; i++) {
            Run run = start(client, get(server, "/" + i).build(), true);
            int delayMicros = i * 150;
            CompletableFuture<Boolean> cancelled = CompletableFuture.supplyAsync(() -> {
                long until = System.nanoTime() + TimeUnit.MICROSECONDS.toNanos(delayMicros);
                while (System.nanoTime() < until) {
                    Thread.onSpinWait();
                }
                return run.exchange.cancel(CancellationReason.CALLER_CANCELLED);
            });
            boolean won = cancelled.get(WAIT.toMillis(), TimeUnit.MILLISECONDS);
            Throwable failure = settle(run);
            if (won) {
                transportFailure(failure, HttpFailureCategory.CANCELLED);
            } else {
                assertEquals(null, failure, "a cancel that lost found the exchange completed");
                assertArrayEquals(payload, run.recorder.body.bytes());
            }
            run.assertOneTerminal();
        }
        assertArrayEquals(payload, call(client, get(server, "/after").build()), "the client works after the races");
    }

    // ---------------------------------------------------------------------------------------------------------------
    // Failure categories

    @Test
    void error_refusedConnect_isConnect() throws Exception {
        Run run = start(client, get("http", TckSupport.refusedPort(), "/").build(), true);
        transportFailure(run.failure(), HttpFailureCategory.CONNECT);
        transportFailure(run.recorder.handlerError.get(WAIT.toMillis(), TimeUnit.MILLISECONDS),
                         HttpFailureCategory.CONNECT);
        run.assertOneTerminal();
    }

    @Test
    void error_untrustedCertificate_isTls() throws Exception {
        TckServer server = track(TckServer.tls(TckTls.get(), serving("secure")));
        Run run = start(client, get("https", server.port(), "/").host("127.0.0.1").build(), true);
        transportFailure(run.failure(), HttpFailureCategory.TLS);
        run.assertOneTerminal();
    }

    @Test
    void error_prematureClose_isResponseConsumption() throws Exception {
        TckServer server = server(ex -> {
            ex.body();
            ex.out().write(TckServer.ascii("HTTP/1.1 200 TCK\r\nContent-Length: 100\r\n\r\n0123456789"));
            return false;
        });
        Run run = start(client, get(server, "/").build(), true);
        transportFailure(run.failure(), HttpFailureCategory.RESPONSE_CONSUMPTION);
        run.assertOneTerminal();
    }

    // ---------------------------------------------------------------------------------------------------------------
    // TLS, proxy, timeouts (capability gated)

    @Test
    void tls_roundTripWithTestTrust_succeeds() throws Exception {
        requireTestTrust(transport(), capabilities);
        TckServer server = track(TckServer.tls(TckTls.get(), serving("secure")));
        SdkAsyncHttpClient trusting = client(ClientSettings.defaults().trust(TckTls.get()));
        assertEquals("secure", utf8(call(trusting, get("https", server.port(), "/tls").build())));
        assertEquals("/tls", server.take().target());
    }

    @Test
    void proxy_plainHttpRequest_isSentThroughTheProxy() throws Exception {
        require(transport(), capabilities, HttpClientOption.PROXY);
        TckServer proxy = server(serving("via-proxy"));
        SdkAsyncHttpClient proxied = client(ClientSettings.defaults()
                                                          .proxy(URI.create("http://localhost:" + proxy.port())));
        SdkHttpRequest request = get("http", 8080, "/target").host("tck-target.invalid").build();
        assertEquals("via-proxy", utf8(call(proxied, request)));
        assertEquals("http://tck-target.invalid:8080/target", proxy.take().target());
    }

    @Test
    void timeout_readTimeout_isTimeout() throws Exception {
        require(transport(), capabilities, HttpClientOption.READ_TIMEOUT);
        CountDownLatch release = new CountDownLatch(1);
        TckServer server = server(SyncHttpClientTck.silentUntil(release));
        SdkAsyncHttpClient timed = client(ClientSettings.defaults().readTimeout(SHORT_TIMEOUT));
        try {
            long start = System.nanoTime();
            Run run = start(timed, get(server, "/").build(), true);
            transportFailure(run.failure(), HttpFailureCategory.TIMEOUT);
            assertElapsedBelow(start, SHORT_TIMEOUT.plus(SLACK));
            run.assertOneTerminal();
        } finally {
            release.countDown();
        }
    }

    // ---------------------------------------------------------------------------------------------------------------
    // Ownership (universal)

    @Test
    void ownership_consumedToCompletion_reusesTheConnection() throws Exception {
        TckServer server = server(serving("reuse"));
        assertEquals("reuse", utf8(call(client, get(server, "/1").build())));
        assertEquals("reuse", utf8(call(client, get(server, "/2").build())));
        assertEquals(1, server.connections());
    }

    @Test
    void ownership_cancelledBodySubscription_discardsTheConnection() throws Exception {
        byte[] large = bytes(4 * 1024 * 1024);
        TckServer server = server(ex -> {
            ex.body();
            ex.out().write(TckServer.response(200, ex.head().target().equals("/large") ? large : bytes(10)));
            return true;
        });
        Run first = start(client, get(server, "/large").build(), false);
        await(first.recorder.body.subscribed);
        first.recorder.body.request(1);
        assertEventually(() -> first.recorder.body.chunks.get() == 1, "the first chunk arrives");
        first.recorder.body.cancel();
        first.exchange.completion().handle((v, t) -> null).get(WAIT.toMillis(), TimeUnit.MILLISECONDS);
        assertEquals(List.of(), first.recorder.violations);
        assertEquals(10, call(client, get(server, "/small").build()).length);
        assertEquals(2, server.connections(), "a cancelled body subscription discards the connection");
    }

    @Test
    void ownership_closeIsIdempotent_andCancelsExchangesInFlight() throws Exception {
        CountDownLatch release = new CountDownLatch(1);
        TckServer server = server(SyncHttpClientTck.silentUntil(release));
        SdkAsyncHttpClient own = transport().create(ClientSettings.defaults());
        try {
            Run run = start(own, get(server, "/").build(), true);
            server.take();
            long start = System.nanoTime();
            own.close();
            own.close();
            transportFailure(within(run, start), HttpFailureCategory.CANCELLED);
            run.assertOneTerminal();
        } finally {
            release.countDown();
        }
    }

    @Test
    void ownership_closedClient_rejectsExecute() throws Exception {
        TckServer server = server(serving("never"));
        SdkAsyncHttpClient own = transport().create(ClientSettings.defaults());
        own.close();
        assertThrows(IllegalStateException.class,
                     () -> own.execute(get(server, "/").build(), new Recorder(true), HttpExecutionOptions.defaults()));
        assertEquals(0, server.connections());
    }

    // ---------------------------------------------------------------------------------------------------------------
    // Concurrency (design: >= 2 simultaneous, more total than simultaneous, a barrier proving overlap, bounded)

    @Test
    void concurrency_overlappingExchanges_allComplete() throws Exception {
        int parallel = 2;
        int rounds = 3;
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
        for (int round = 0; round < rounds; round++) {
            List<Run> runs = new ArrayList<>();
            for (int c = 0; c < parallel; c++) {
                runs.add(start(client, get(server, "/" + round + "/" + c).build(), true));
            }
            for (int c = 0; c < parallel; c++) {
                assertEquals("/" + round + "/" + c, utf8(runs.get(c).body()));
                runs.get(c).assertOneTerminal();
            }
        }
        assertEquals(parallel, maxInFlight.get(), "the barrier proved overlapping exchanges");
    }

    // ---------------------------------------------------------------------------------------------------------------
    // Helpers

    /** One started exchange and its recorder. */
    final class Run {
        final AsyncHttpExchange exchange;
        final Recorder recorder;

        Run(AsyncHttpExchange exchange, Recorder recorder) {
            this.exchange = exchange;
            this.recorder = recorder;
        }

        /** The response body, after the exchange completed normally. */
        byte[] body() throws Exception {
            Throwable failure = settle(this);
            if (failure != null) {
                throw new AssertionError("the exchange failed", failure);
            }
            return recorder.body.bytes();
        }

        /** The exchange's failure (it must fail within {@link TckSupport#WAIT}). */
        Throwable failure() throws Exception {
            Throwable failure = settle(this);
            if (failure == null) {
                return fail("the exchange completed normally");
            }
            return failure;
        }

        /** Exactly one terminal signal accompanies the completion, and no protocol rule was broken. */
        void assertOneTerminal() throws InterruptedException {
            long deadline = System.nanoTime() + WAIT.toNanos();
            while (recorder.terminals.get() == 0 && System.nanoTime() < deadline) {
                Thread.sleep(5);
            }
            Thread.sleep(20);
            assertTrue(exchange.completion().isDone(), "the completion is done");
            assertEquals(1, recorder.terminals.get(), "exactly one terminal signal");
            assertEquals(List.of(), recorder.violations, "async protocol violations");
        }
    }

    private Run start(SdkAsyncHttpClient target, SdkHttpRequest request, boolean autoDemand) {
        Recorder recorder = new Recorder(autoDemand);
        return new Run(target.execute(request, recorder, HttpExecutionOptions.defaults()), recorder);
    }

    /** Waits for the completion; returns its failure (unwrapped) or null. */
    private static Throwable settle(Run run) throws Exception {
        try {
            run.exchange.completion().get(WAIT.toMillis(), TimeUnit.MILLISECONDS);
            return null;
        } catch (ExecutionException e) {
            return e.getCause();
        }
    }

    /** The failure of {@code run}, which must arrive within the declared cancellation timeout of {@code start}. */
    private Throwable within(Run run, long start) throws Exception {
        Duration limit = capabilities.cancellationTimeout();
        try {
            run.exchange.completion().get(limit.toMillis() + 250, TimeUnit.MILLISECONDS);
            return fail("the cancelled exchange completed normally");
        } catch (TimeoutException e) {
            return fail(transport().name() + " did not terminate within the declared cancellation timeout " + limit);
        } catch (ExecutionException e) {
            assertElapsedBelow(start, limit.plusMillis(250));
            return e.getCause();
        }
    }

    private <T extends AutoCloseable> T track(T resource) {
        resources.add(resource);
        return resource;
    }

    private TckServer server(TckServer.Handler handler) throws IOException {
        return track(TckServer.plain(handler));
    }

    private SdkAsyncHttpClient client(ClientSettings settings) {
        return track(transport().create(settings));
    }

    private static SdkHttpRequest put(TckServer server, HttpBody body) {
        return get(server, "/upload").method("PUT").body(body).build();
    }

    private byte[] call(SdkAsyncHttpClient target, SdkHttpRequest request) throws Exception {
        Run run = start(target, request, true);
        byte[] body = run.body();
        run.assertOneTerminal();
        return body;
    }

    private static void assertEventually(java.util.function.BooleanSupplier condition, String what)
        throws InterruptedException {
        long deadline = System.nanoTime() + WAIT.toNanos();
        while (!condition.getAsBoolean()) {
            if (System.nanoTime() > deadline) {
                fail("timed out: " + what);
            }
            Thread.sleep(5);
        }
    }

    private static void assertElapsedBelow(long startNanos, Duration limit) {
        Duration elapsed = Duration.ofNanos(System.nanoTime() - startNanos);
        assertTrue(elapsed.compareTo(limit) <= 0, () -> "took " + elapsed + ", limit " + limit);
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
}
