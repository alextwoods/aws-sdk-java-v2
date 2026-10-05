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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.time.Duration;
import java.util.List;
import java.util.Random;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;
import software.amazon.awssdk.http.AsyncHttpExchange;
import software.amazon.awssdk.http.AsyncHttpResponseHandler;
import software.amazon.awssdk.http.CancellationReason;
import software.amazon.awssdk.http.HttpBody;
import software.amazon.awssdk.http.HttpExecutionOptions;
import software.amazon.awssdk.http.HttpFailureCategory;
import software.amazon.awssdk.http.HttpPhase;
import software.amazon.awssdk.http.HttpTransportException;
import software.amazon.awssdk.http.SdkHttpRequest;
import software.amazon.awssdk.http.SdkHttpResponse;
import software.amazon.awssdk.http.StreamingBodies;
import software.amazon.awssdk.http.crt.internal.CrtResponsePublisher;

class AwsCrtAsyncHttpClientTest {
    private static final byte[] LARGE = new byte[4 * 1024 * 1024];
    private static final CountDownLatch HANG = new CountDownLatch(1);
    private static TestServers.Server server;

    @BeforeAll
    static void start() throws IOException {
        new Random(11).nextBytes(LARGE);
        server = TestServers.Server.http(exchange -> {
            String path = exchange.getRequestURI().getRawPath();
            try (exchange) {
                switch (path) {
                    case "/echo" -> {
                        byte[] in = exchange.getRequestBody().readAllBytes();
                        exchange.getResponseHeaders().add("X-Sha", AwsCrtHttpClientTest.sha(in));
                        exchange.sendResponseHeaders(200, in.length == 0 ? -1 : in.length);
                        exchange.getResponseBody().write(in);
                    }
                    case "/large" -> {
                        exchange.sendResponseHeaders(200, LARGE.length);
                        exchange.getResponseBody().write(LARGE);
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

    /** Records handler signals; subscribes to the body with a controllable demand. */
    static final class Recorder implements AsyncHttpResponseHandler, Subscriber<ByteBuffer> {
        final List<String> signals = new CopyOnWriteArrayList<>();
        final CompletableFuture<SdkHttpResponse> headers = new CompletableFuture<>();
        final CompletableFuture<byte[]> body = new CompletableFuture<>();
        final CompletableFuture<Throwable> error = new CompletableFuture<>();
        final ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        final AtomicLong outstanding = new AtomicLong();
        final boolean autoRequest;
        volatile Subscription subscription;
        volatile Publisher<ByteBuffer> publisher;

        Recorder(boolean autoRequest) {
            this.autoRequest = autoRequest;
        }

        @Override
        public void onHeaders(SdkHttpResponse response) {
            signals.add("headers");
            headers.complete(response);
        }

        @Override
        public void onStream(Publisher<ByteBuffer> p) {
            signals.add("stream");
            publisher = p;
            p.subscribe(this);
        }

        @Override
        public void onError(Throwable t) {
            signals.add("error");
            error.complete(t);
        }

        @Override
        public void onSubscribe(Subscription s) {
            subscription = s;
            if (autoRequest) {
                request(1);
            }
        }

        void request(long n) {
            outstanding.addAndGet(n);
            subscription.request(n);
        }

        @Override
        public void onNext(ByteBuffer b) {
            if (outstanding.decrementAndGet() < 0) {
                body.completeExceptionally(new AssertionError("onNext without demand"));
            }
            assertTrue(b.isReadOnly());
            byte[] chunk = new byte[b.remaining()];
            b.get(chunk);
            synchronized (bytes) {
                bytes.write(chunk, 0, chunk.length);
            }
            if (autoRequest) {
                request(1);
            }
        }

        @Override
        public void onComplete() {
            signals.add("complete");
            synchronized (bytes) {
                body.complete(bytes.toByteArray());
            }
        }

    }

    private static SdkHttpRequest.Builder get(String path) {
        return server.get(path);
    }

    @Test
    void streamingPublisherUploadAndExactDemandDownload() throws Exception {
        byte[] upload = new byte[512 * 1024 + 3];
        new Random(5).nextBytes(upload);
        try (AwsCrtAsyncHttpClient client = AwsCrtAsyncHttpClient.create()) {
            AtomicInteger requested = new AtomicInteger();
            Publisher<ByteBuffer> source = StreamingBodies.fromBytes(upload, null);
            Publisher<ByteBuffer> counting = s -> source.subscribe(new Subscriber<ByteBuffer>() {
                @Override
                public void onSubscribe(Subscription sub) {
                    s.onSubscribe(new Subscription() {
                        @Override
                        public void request(long n) {
                            assertEquals(1, n, "the transport requests one chunk at a time");
                            requested.incrementAndGet();
                            sub.request(n);
                        }

                        @Override
                        public void cancel() {
                            sub.cancel();
                        }
                    });
                }

                @Override
                public void onNext(ByteBuffer b) {
                    s.onNext(b);
                }

                @Override
                public void onError(Throwable t) {
                    s.onError(t);
                }

                @Override
                public void onComplete() {
                    s.onComplete();
                }
            });
            SdkHttpRequest request = server.request("http", "127.0.0.1", "PUT", "/echo")
                                           .body(HttpBody.streaming(StreamingBodies.of(
                                               upload.length, null, false, () -> {
                                                   throw new UnsupportedOperationException();
                                               }, counting)))
                                           .build();
            Recorder recorder = new Recorder(true);
            AsyncHttpExchange exchange = client.execute(request, recorder, HttpExecutionOptions.defaults());
            exchange.completion().get(10, TimeUnit.SECONDS);
            assertEquals(AwsCrtHttpClientTest.sha(upload),
                         recorder.headers.get().headers().firstValue("X-Sha").orElseThrow());
            assertArrayEquals(upload, recorder.body.get(1, TimeUnit.SECONDS));
            assertEquals(List.of("headers", "stream", "complete"), recorder.signals);
            assertTrue(requested.get() > 1);

            Recorder large = new Recorder(true);
            client.execute(get("/large").build(), large, HttpExecutionOptions.defaults())
                  .completion().get(10, TimeUnit.SECONDS);
            assertArrayEquals(LARGE, large.body.get());
        }
    }

    @Test
    void responseBufferingIsBoundedByTheReadWindowWithoutDemand() throws Exception {
        long window = 64 * 1024;
        try (AwsCrtAsyncHttpClient client = AwsCrtAsyncHttpClient.builder().readBufferSizeInBytes(window).build()) {
            Recorder recorder = new Recorder(false);
            AsyncHttpExchange exchange = client.execute(get("/large").build(), recorder,
                                                        HttpExecutionOptions.defaults());
            recorder.headers.get(5, TimeUnit.SECONDS);
            Thread.sleep(500);
            CrtResponsePublisher publisher = (CrtResponsePublisher) recorder.publisher;
            assertTrue(publisher.maxQueuedBytes() <= window, "queued " + publisher.maxQueuedBytes());
            assertEquals(0, recorder.bytes.size(), "nothing is delivered without demand");
            recorder.request(Long.MAX_VALUE);
            exchange.completion().get(10, TimeUnit.SECONDS);
            assertArrayEquals(LARGE, recorder.body.get());
            assertTrue(publisher.maxQueuedBytes() <= window, "queued " + publisher.maxQueuedBytes());
        }
    }

    @Test
    void cancelBeforeHeadersSignalsOnErrorOnceWithinTheTimeout() throws Exception {
        try (AwsCrtAsyncHttpClient client = AwsCrtAsyncHttpClient.create()) {
            Recorder recorder = new Recorder(true);
            AsyncHttpExchange exchange = client.execute(get("/hang").build(), recorder,
                                                        HttpExecutionOptions.defaults());
            Thread.sleep(300);
            long start = System.nanoTime();
            assertTrue(exchange.cancel(CancellationReason.CALLER_CANCELLED));
            assertFalse(exchange.cancel(CancellationReason.CALLER_CANCELLED));
            Duration timeout = client.capabilities().cancellationTimeout();
            Throwable t = recorder.error.get(timeout.toMillis(), TimeUnit.MILLISECONDS);
            ExecutionException e = org.junit.jupiter.api.Assertions.assertThrows(
                ExecutionException.class, () -> exchange.completion().get(timeout.toMillis(), TimeUnit.MILLISECONDS));
            assertTrue(Duration.ofNanos(System.nanoTime() - start).compareTo(timeout) < 0);
            assertEquals(HttpFailureCategory.CANCELLED, ((HttpTransportException) t).category());
            assertEquals(HttpFailureCategory.CANCELLED, ((HttpTransportException) e.getCause()).category());
            Thread.sleep(200);
            assertEquals(List.of("error"), recorder.signals);
        }
    }

    @Test
    void cancelAfterOnStreamFailsTheBodySubscriber() throws Exception {
        try (AwsCrtAsyncHttpClient client = AwsCrtAsyncHttpClient.create()) {
            Recorder recorder = new Recorder(true);
            AsyncHttpExchange exchange = client.execute(get("/hang-body").build(), recorder,
                                                        HttpExecutionOptions.defaults());
            recorder.headers.get(5, TimeUnit.SECONDS);
            Thread.sleep(200);
            assertTrue(exchange.cancel(CancellationReason.API_CALL_TIMEOUT));
            Throwable t = recorder.error.get(2, TimeUnit.SECONDS);
            assertEquals(HttpFailureCategory.CANCELLED, ((HttpTransportException) t).category());
            assertEquals(HttpPhase.RESPONSE_CONSUMPTION, ((HttpTransportException) t).phase());
            Thread.sleep(200);
            assertEquals(List.of("headers", "stream", "error"), recorder.signals);
        }
    }

    @Test
    void subscriberCancellationDiscardsTheExchange() throws Exception {
        try (AwsCrtAsyncHttpClient client = AwsCrtAsyncHttpClient.create()) {
            Recorder recorder = new Recorder(false);
            AsyncHttpExchange exchange = client.execute(get("/large").build(), recorder,
                                                        HttpExecutionOptions.defaults());
            recorder.headers.get(5, TimeUnit.SECONDS);
            recorder.subscription.cancel();
            ExecutionException e = org.junit.jupiter.api.Assertions.assertThrows(
                ExecutionException.class, () -> exchange.completion().get(2, TimeUnit.SECONDS));
            assertEquals(HttpFailureCategory.CANCELLED, ((HttpTransportException) e.getCause()).category());
        }
    }

    @Test
    void acquisitionTimeoutAndCloseCancellation() throws Exception {
        AwsCrtAsyncHttpClient client = AwsCrtAsyncHttpClient.builder().maxConcurrency(1)
                                                            .connectionAcquisitionTimeout(Duration.ofMillis(300))
                                                            .build();
        Recorder hanging = new Recorder(true);
        AsyncHttpExchange first = client.execute(get("/hang").build(), hanging, HttpExecutionOptions.defaults());
        Thread.sleep(300);
        Recorder second = new Recorder(true);
        client.execute(get("/echo").build(), second, HttpExecutionOptions.defaults());
        HttpTransportException e = (HttpTransportException) second.error.get(5, TimeUnit.SECONDS);
        assertEquals(HttpFailureCategory.TIMEOUT, e.category());
        assertEquals(HttpPhase.CONNECTION_ACQUISITION, e.phase());

        client.close();
        HttpTransportException closed = (HttpTransportException) hanging.error.get(2, TimeUnit.SECONDS);
        assertEquals(HttpFailureCategory.CANCELLED, closed.category());
        assertTrue(first.completion().isCompletedExceptionally());
        org.junit.jupiter.api.Assertions.assertThrows(
            IllegalStateException.class,
            () -> client.execute(get("/echo").build(), new Recorder(true), HttpExecutionOptions.defaults()));
    }

    @Test
    void secondBodySubscriberIsRejected() throws Exception {
        try (AwsCrtAsyncHttpClient client = AwsCrtAsyncHttpClient.create()) {
            Recorder recorder = new Recorder(true);
            client.execute(get("/echo").body(HttpBody.fromUtf8("x")).build(), recorder,
                           HttpExecutionOptions.defaults()).completion().get(5, TimeUnit.SECONDS);
            Recorder other = new Recorder(true);
            recorder.publisher.subscribe(other);
            assertInstanceOf(IllegalStateException.class, other.error.get(1, TimeUnit.SECONDS));
        }
    }
}
