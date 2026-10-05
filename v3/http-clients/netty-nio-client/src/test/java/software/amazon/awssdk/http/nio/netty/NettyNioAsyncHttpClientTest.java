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

package software.amazon.awssdk.http.nio.netty;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpsConfigurator;
import com.sun.net.httpserver.HttpsServer;
import io.netty.channel.EventLoop;
import io.netty.channel.nio.NioEventLoopGroup;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.Random;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;
import software.amazon.awssdk.http.AsyncHttpExchange;
import software.amazon.awssdk.http.CancellationReason;
import software.amazon.awssdk.http.HttpBody;
import software.amazon.awssdk.http.HttpClientCapabilities;
import software.amazon.awssdk.http.HttpClientOption;
import software.amazon.awssdk.http.HttpExecutionOptions;
import software.amazon.awssdk.http.HttpFailureCategory;
import software.amazon.awssdk.http.HttpPhase;
import software.amazon.awssdk.http.HttpTransportException;
import software.amazon.awssdk.http.HttpTransportMode;
import software.amazon.awssdk.http.Protocol;
import software.amazon.awssdk.http.SdkHttpRequest;
import software.amazon.awssdk.http.SdkHttpResponse;
import software.amazon.awssdk.http.StreamingBodies;
import software.amazon.awssdk.http.StreamingBody;
import software.amazon.awssdk.http.nio.netty.internal.ResponseBodyPublisher;

class NettyNioAsyncHttpClientTest {
    private static final long DEADLINE_MS = NettyNioAsyncHttpClient.CANCELLATION_TIMEOUT.toMillis();

    private HttpServer server;
    private final List<String> requests = new CopyOnWriteArrayList<>();
    private final List<Integer> remotePorts = new CopyOnWriteArrayList<>();
    private final List<String> serverErrors = new CopyOnWriteArrayList<>();
    private final CountDownLatch hold = new CountDownLatch(1);
    private NettyNioAsyncHttpClient client;

    @BeforeEach
    void start() throws Exception {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.setExecutor(Executors.newCachedThreadPool());
        server.createContext("/", ex -> {
            try {
                serve(ex);
            } catch (java.io.IOException | RuntimeException e) {
                serverErrors.add(e.toString());
                throw e;
            }
        });
        server.start();
        client = NettyNioAsyncHttpClient.builder().eventLoopGroupBuilder(SdkEventLoopGroup.builder().numberOfThreads(2))
                                        .build();
    }

    @AfterEach
    void stop() {
        hold.countDown();
        client.close();
        server.stop(0);
    }

    private void serve(HttpExchange exchange) throws java.io.IOException {
        remotePorts.add(exchange.getRemoteAddress().getPort());
        String target = exchange.getRequestURI().toString();
        requests.add(exchange.getRequestMethod() + " " + target + " " + exchange.getRequestHeaders().get("X-rep"));
        byte[] in = exchange.getRequestBody().readAllBytes();
        if (target.startsWith("/hold")) {
            try {
                hold.await(10, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
        byte[] out;
        if (target.startsWith("/big")) {
            out = new byte[4 * 1024 * 1024];
            new Random(1).nextBytes(out);
        } else if (target.startsWith("/medium")) {
            out = new byte[128 * 1024];
        } else if (target.startsWith("/echo")) {
            out = in;
        } else {
            out = ("ok " + in.length).getBytes(StandardCharsets.UTF_8);
        }
        if (target.startsWith("/close")) {
            exchange.getResponseHeaders().add("Connection", "close");
        }
        exchange.getResponseHeaders().add("X-Out", "a");
        exchange.getResponseHeaders().add("X-Out", "b");
        exchange.sendResponseHeaders(200, out.length == 0 ? -1 : out.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(out);
        }
    }

    private SdkHttpRequest.Builder get(String path) {
        return SdkHttpRequest.builder().method("GET").scheme("http").host("127.0.0.1")
                             .port(server.getAddress().getPort()).encodedPath(path);
    }

    private TestSupport.Recorder run(SdkHttpRequest request) {
        TestSupport.Recorder recorder = new TestSupport.Recorder();
        client.execute(request, recorder, HttpExecutionOptions.defaults());
        return recorder;
    }

    private static HttpTransportException failure(TestSupport.Recorder recorder) throws Exception {
        return assertInstanceOf(HttpTransportException.class, recorder.error.get(10, TimeUnit.SECONDS));
    }

    @Test
    void capabilitiesDeclareExactlyTheSupportedOptions() {
        HttpClientCapabilities caps = client.capabilities();
        assertEquals(HttpTransportMode.ASYNC, caps.mode());
        assertEquals(java.util.Set.of(Protocol.HTTP1_1, Protocol.HTTP2), caps.protocols());
        assertEquals(NettyNioAsyncHttpClient.OPTIONS, caps.options());
        assertTrue(caps.unsupported(java.util.Set.of(HttpClientOption.PROXY, HttpClientOption.EXPECT_CONTINUE,
                                                     HttpClientOption.TRUST_ALL_CERTIFICATES))
                       .containsAll(List.of(HttpClientOption.PROXY, HttpClientOption.EXPECT_CONTINUE)));
        assertEquals(NettyNioAsyncHttpClient.HTTP1_MAX_BUFFERED_BYTES, caps.maxBufferedBytesPerExchange());
        assertEquals(NettyNioAsyncHttpClient.CANCELLATION_TIMEOUT, caps.cancellationTimeout());
        assertEquals("NettyNioAsyncHttpClient", client.clientName());
    }

    @Test
    void requestTargetAndRepeatedHeadersAreSentExactly() throws Exception {
        SdkHttpRequest request = get("/a%2Fb/c").addRawQueryParameter("x", "1%202").addRawQueryParameter("x", "3")
                                                .addRawQueryParameter("flag", null)
                                                .addHeader("X-Rep", "one").addHeader("X-Rep", "two").build();
        TestSupport.Recorder recorder = run(request);
        SdkHttpResponse response = recorder.headers.get(10, TimeUnit.SECONDS);
        assertEquals("ok 0", new String(recorder.body.get(10, TimeUnit.SECONDS), StandardCharsets.UTF_8));
        assertEquals(200, response.statusCode());
        assertEquals(List.of("a", "b"), response.headers().allValues("X-Out"));
        assertEquals("GET /a%2Fb/c?x=1%202&x=3&flag [one, two]", requests.get(0));
        assertEquals(List.of("onHeaders", "onStream", "onSubscribe"), recorder.signals.subList(0, 3));
        assertEquals("onComplete", recorder.signals.get(recorder.signals.size() - 1));
        assertTrue(recorder.threads.stream().allMatch(t -> t.startsWith("aws-java-sdk-NettyEventLoop")),
                   recorder.threads::toString);
        recorder.stream.get().subscribe(new Subscriber<>() {
            @Override
            public void onSubscribe(Subscription s) {
            }

            @Override
            public void onNext(ByteBuffer b) {
            }

            @Override
            public void onError(Throwable t) {
                recorder.signals.add("secondRejected");
            }

            @Override
            public void onComplete() {
            }
        });
        assertTrue(recorder.signals.contains("secondRejected"));
    }

    @Test
    void demandDrivenBodiesStayWithinTheBufferBound() throws Exception {
        byte[] payload = new byte[3 * 1024 * 1024 + 17];
        new Random(2).nextBytes(payload);
        SdkHttpRequest echo = get("/echo").method("POST")
                                          .body(HttpBody.streaming(StreamingBodies.fromBytes(payload, null))).build();
        TestSupport.Recorder recorder = run(echo);
        assertArrayEquals(payload, recorder.body.get(30, TimeUnit.SECONDS));

        TestSupport.Recorder big = run(get("/big").build());
        byte[] body = big.body.get(30, TimeUnit.SECONDS);
        assertEquals(4 * 1024 * 1024, body.length);
        ResponseBodyPublisher publisher = (ResponseBodyPublisher) big.stream.get();
        assertTrue(publisher.maxQueuedBytes() <= client.capabilities().maxBufferedBytesPerExchange(),
                   () -> "queued " + publisher.maxQueuedBytes());
        assertTrue(big.signals.stream().filter("onNext"::equals).count() > 1);
    }

    @Test
    void unknownLengthBodyIsChunked() throws Exception {
        byte[] payload = "streamed without a length".getBytes(StandardCharsets.UTF_8);
        StreamingBody unknown = StreamingBodies.fromInputStream(new java.io.ByteArrayInputStream(payload), -1, null);
        TestSupport.Recorder recorder = run(get("/echo").method("PUT").body(HttpBody.streaming(unknown)).build());
        assertArrayEquals(payload, recorder.body.get(10, TimeUnit.SECONDS));
    }

    @Test
    void noSdkCodeBlocksAnEventLoop() throws Exception {
        // Watchdog: a probe task on every event loop measures how late it runs while exchanges are active.
        AtomicLong worstDelayNanos = new AtomicLong();
        AtomicBoolean running = new AtomicBoolean(true);
        List<EventLoop> loops = new java.util.ArrayList<>();
        client.eventLoopGroup().eventLoopGroup().forEach(e -> loops.add((EventLoop) e));
        Thread watchdog = new Thread(() -> {
            while (running.get()) {
                for (EventLoop loop : loops) {
                    long submitted = System.nanoTime();
                    loop.execute(() -> worstDelayNanos.accumulateAndGet(System.nanoTime() - submitted, Math::max));
                }
                try {
                    Thread.sleep(5);
                } catch (InterruptedException e) {
                    return;
                }
            }
        });
        watchdog.start();
        try {
            List<TestSupport.Recorder> recorders = new java.util.ArrayList<>();
            for (int i = 0; i < 40; i++) {
                long before = System.nanoTime();
                // A host name, so resolution happens; it must not run on the caller or an event loop.
                recorders.add(run(SdkHttpRequest.builder().method("GET").scheme("http").host("localhost")
                                                .port(server.getAddress().getPort()).encodedPath("/medium").build()));
                assertTrue(System.nanoTime() - before < TimeUnit.MILLISECONDS.toNanos(200), "execute blocked");
            }
            for (TestSupport.Recorder r : recorders) {
                try {
                    r.body.get(60, TimeUnit.SECONDS);
                } catch (ExecutionException e) {
                    throw new AssertionError("server errors: " + serverErrors + " signals " + r.signals, e);
                }
                assertTrue(r.threads.stream().allMatch(t -> t.startsWith("aws-java-sdk-NettyEventLoop")),
                           r.threads::toString);
            }
        } finally {
            running.set(false);
            watchdog.join();
        }
        assertTrue(worstDelayNanos.get() < TimeUnit.MILLISECONDS.toNanos(250),
                   () -> "an event loop was blocked for " + worstDelayNanos.get() / 1_000_000 + " ms");
    }

    @Test
    void cancellationDuringAcquisitionMeetsTheDeadline() throws Exception {
        client.close();
        client = NettyNioAsyncHttpClient.builder().maxConcurrency(1).build();
        TestSupport.Recorder first = run(get("/hold").build());
        TestSupport.Recorder second = new TestSupport.Recorder();
        AsyncHttpExchange waiting = client.execute(get("/x").build(), second, HttpExecutionOptions.defaults());
        Thread.sleep(200);
        long start = System.nanoTime();
        assertTrue(waiting.cancel(CancellationReason.CALLER_CANCELLED));
        HttpTransportException e = failure(second);
        assertTrue(waiting.completion().isCompletedExceptionally());
        assertTrue(elapsedMs(start) < DEADLINE_MS);
        assertEquals(HttpFailureCategory.CANCELLED, e.category());
        assertEquals(HttpPhase.CONNECTION_ACQUISITION, e.phase());
        assertEquals(List.of("onError"), second.signals);
        assertTrue(!waiting.cancel(CancellationReason.CALLER_CANCELLED));
        hold.countDown();
        first.body.get(10, TimeUnit.SECONDS);
    }

    @Test
    void cancellationDuringWriteMeetsTheDeadline() throws Exception {
        CountDownLatch firstChunk = new CountDownLatch(1);
        CountDownLatch bodyCancelled = new CountDownLatch(1);
        org.reactivestreams.Publisher<ByteBuffer> source = subscriber -> subscriber.onSubscribe(
            new Subscription() {
                private boolean sent;

                @Override
                public void request(long n) {
                    if (!sent) {
                        sent = true;
                        subscriber.onNext(ByteBuffer.wrap(new byte[10]));
                        firstChunk.countDown();
                    }
                }

                @Override
                public void cancel() {
                    bodyCancelled.countDown();
                }
            });
        StreamingBody stalled = StreamingBodies.of(1000, null, false, InputStream::nullInputStream, source);
        TestSupport.Recorder recorder = new TestSupport.Recorder();
        AsyncHttpExchange exchange = client.execute(get("/echo").method("POST").body(HttpBody.streaming(stalled))
                                                                .build(), recorder, HttpExecutionOptions.defaults());
        assertTrue(firstChunk.await(10, TimeUnit.SECONDS));
        long start = System.nanoTime();
        assertTrue(exchange.cancel(CancellationReason.API_CALL_ATTEMPT_TIMEOUT));
        HttpTransportException e = failure(recorder);
        assertTrue(bodyCancelled.await(DEADLINE_MS, TimeUnit.MILLISECONDS));
        assertTrue(elapsedMs(start) < DEADLINE_MS);
        assertEquals(HttpFailureCategory.CANCELLED, e.category());
        assertEquals(HttpPhase.REQUEST_TRANSMISSION, e.phase());
    }

    @Test
    void cancellationDuringReadMeetsTheDeadlineAndDiscardsTheConnection() throws Exception {
        try (ServerSocket raw = new ServerSocket(0, 50, java.net.InetAddress.getByName("127.0.0.1"))) {
            CountDownLatch closedByClient = new CountDownLatch(1);
            Thread t = new Thread(() -> {
                try (Socket s = raw.accept()) {
                    readHead(s.getInputStream());
                    s.getOutputStream().write("HTTP/1.1 200 OK\r\nContent-Length: 1000\r\n\r\npartial"
                                                  .getBytes(StandardCharsets.US_ASCII));
                    s.getOutputStream().flush();
                    if (s.getInputStream().read() == -1) {
                        closedByClient.countDown();
                    }
                } catch (Exception ignored) {
                    closedByClient.countDown();
                }
            });
            t.start();
            TestSupport.Recorder recorder = new TestSupport.Recorder();
            AsyncHttpExchange exchange = client.execute(get("/").port(raw.getLocalPort()).build(), recorder,
                                                        HttpExecutionOptions.defaults());
            recorder.stream.get(10, TimeUnit.SECONDS);
            long start = System.nanoTime();
            assertTrue(exchange.cancel(CancellationReason.CALLER_CANCELLED));
            Throwable bodyError = recorder.error.get(DEADLINE_MS, TimeUnit.MILLISECONDS);
            assertTrue(closedByClient.await(DEADLINE_MS, TimeUnit.MILLISECONDS));
            assertTrue(elapsedMs(start) < DEADLINE_MS);
            HttpTransportException e = assertInstanceOf(HttpTransportException.class, bodyError);
            assertEquals(HttpFailureCategory.CANCELLED, e.category());
            assertEquals(HttpPhase.RESPONSE_CONSUMPTION, e.phase());
            assertTrue(recorder.signals.contains("bodyError") && !recorder.signals.contains("onError"));
            t.join(5000);
        }
    }

    @Test
    void timeoutsAreCategorizedByPhase() throws Exception {
        try (ServerSocket silent = new ServerSocket(0, 50, java.net.InetAddress.getByName("127.0.0.1"))) {
            Thread t = new Thread(() -> {
                try (Socket s = silent.accept()) {
                    s.getInputStream().read(new byte[1024]);
                    Thread.sleep(3000);
                } catch (Exception ignored) {
                    // closed
                }
            });
            t.start();
            TestSupport.Recorder recorder = new TestSupport.Recorder();
            client.execute(get("/").port(silent.getLocalPort()).build(), recorder,
                           HttpExecutionOptions.builder().readTimeout(Duration.ofMillis(300)).build());
            HttpTransportException read = failure(recorder);
            assertEquals(HttpFailureCategory.TIMEOUT, read.category());
            assertEquals(HttpPhase.RESPONSE_CONSUMPTION, read.phase());
            t.interrupt();
        }

        client.close();
        client = NettyNioAsyncHttpClient.builder().maxConcurrency(1).build();
        TestSupport.Recorder busy = run(get("/hold").build());
        TestSupport.Recorder waiter = new TestSupport.Recorder();
        client.execute(get("/x").build(), waiter,
                       HttpExecutionOptions.builder().connectionAcquisitionTimeout(Duration.ofMillis(200)).build());
        HttpTransportException acquire = failure(waiter);
        assertEquals(HttpFailureCategory.TIMEOUT, acquire.category());
        assertEquals(HttpPhase.CONNECTION_ACQUISITION, acquire.phase());
        hold.countDown();
        busy.body.get(10, TimeUnit.SECONDS);

        int closedPort;
        try (ServerSocket s = new ServerSocket(0)) {
            closedPort = s.getLocalPort();
        }
        HttpTransportException refused = failure(run(get("/").port(closedPort).build()));
        assertEquals(HttpFailureCategory.CONNECT, refused.category());
        assertTrue(refused.retryable());
    }

    @Test
    void tlsUsesTheConfiguredTrustManagersAndRejectsUntrustedServers() throws Exception {
        HttpsServer https = HttpsServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        https.setHttpsConfigurator(new HttpsConfigurator(TestSupport.serverSslContext()));
        https.createContext("/", this::serve);
        https.start();
        javax.net.ssl.TrustManager[] trust = TestSupport.trustManagers().getTrustManagers();
        try (NettyNioAsyncHttpClient trusting = NettyNioAsyncHttpClient.builder()
                 .tlsTrustManagersProvider(() -> trust).build()) {
            SdkHttpRequest request = SdkHttpRequest.builder().method("GET").scheme("https").host("localhost")
                                                   .port(https.getAddress().getPort()).encodedPath("/tls").build();
            TestSupport.Recorder ok = new TestSupport.Recorder();
            trusting.execute(request, ok, HttpExecutionOptions.defaults());
            assertEquals("ok 0", new String(ok.body.get(10, TimeUnit.SECONDS), StandardCharsets.UTF_8));

            TestSupport.Recorder rejected = new TestSupport.Recorder();
            client.execute(request, rejected, HttpExecutionOptions.defaults());
            HttpTransportException e = failure(rejected);
            assertEquals(HttpFailureCategory.TLS, e.category());
            assertEquals(HttpPhase.TLS_HANDSHAKE, e.phase());
        } finally {
            https.stop(0);
        }
    }

    @Test
    void connectionsAreReusedUnlessClosedOrCancelled() throws Exception {
        run(get("/one").build()).body.get(10, TimeUnit.SECONDS);
        run(get("/two").build()).body.get(10, TimeUnit.SECONDS);
        assertEquals(remotePorts.get(0), remotePorts.get(1), "keep-alive connection reused");
        run(get("/close").build()).body.get(10, TimeUnit.SECONDS);
        run(get("/three").build()).body.get(10, TimeUnit.SECONDS);
        assertNotEquals(remotePorts.get(2), remotePorts.get(3), "Connection: close discards the connection");

        TestSupport.Recorder partial = new TestSupport.Recorder(1, false);
        AsyncHttpExchange exchange = client.execute(get("/big").build(), partial, HttpExecutionOptions.defaults());
        partial.stream.get(10, TimeUnit.SECONDS);
        exchange.cancel(CancellationReason.RESPONSE_ABANDONED);
        run(get("/four").build()).body.get(10, TimeUnit.SECONDS);
        assertNotEquals(remotePorts.get(4), remotePorts.get(5), "a cancelled exchange discards its connection");
    }

    @Test
    void closeIsIdempotentCancelsInFlightAndShutsDownOnlyOwnedGroups() throws Exception {
        TestSupport.Recorder inFlight = run(get("/hold").build());
        inFlight.headers.isDone();
        Thread.sleep(200);
        client.close();
        client.close();
        HttpTransportException e = failure(inFlight);
        assertEquals(HttpFailureCategory.CANCELLED, e.category());
        assertTrue(client.eventLoopGroup().eventLoopGroup().terminationFuture().await(10, TimeUnit.SECONDS));
        assertThrows(IllegalStateException.class,
                     () -> client.execute(get("/").build(), new TestSupport.Recorder(), HttpExecutionOptions.defaults()));

        NioEventLoopGroup callerGroup = new NioEventLoopGroup(1);
        try {
            NettyNioAsyncHttpClient borrowing = NettyNioAsyncHttpClient.builder()
                                                                       .eventLoopGroup(SdkEventLoopGroup.create(callerGroup))
                                                                       .build();
            TestSupport.Recorder r = new TestSupport.Recorder();
            borrowing.execute(get("/").build(), r, HttpExecutionOptions.defaults());
            r.body.get(10, TimeUnit.SECONDS);
            borrowing.close();
            assertTrue(!callerGroup.isShuttingDown(), "a caller-owned group is not shut down");
        } finally {
            callerGroup.shutdownGracefully(0, 1, TimeUnit.SECONDS);
        }
    }

    @Test
    void failedConstructionShutsDownTheCreatedGroup() {
        List<SdkEventLoopGroup> created = new CopyOnWriteArrayList<>();
        NettyNioAsyncHttpClient.Builder builder = NettyNioAsyncHttpClient.builder().tlsTrustManagersProvider(() -> {
            throw new IllegalStateException("no trust store");
        });
        assertThrows(IllegalStateException.class, () -> builder.build(created::add));
        assertEquals(1, created.size());
        assertTrue(created.get(0).eventLoopGroup().isShuttingDown());
    }

    private static long elapsedMs(long startNanos) {
        return TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startNanos);
    }

    private static void readHead(InputStream in) throws java.io.IOException {
        int state = 0;
        while (state < 4) {
            int b = in.read();
            if (b < 0) {
                return;
            }
            state = (b == '\r' && (state == 0 || state == 2)) || (b == '\n' && (state == 1 || state == 3))
                    ? state + 1 : 0;
        }
    }
}
