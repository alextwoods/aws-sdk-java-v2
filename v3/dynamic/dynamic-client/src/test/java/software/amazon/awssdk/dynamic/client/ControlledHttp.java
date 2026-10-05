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

package software.amazon.awssdk.dynamic.client;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BooleanSupplier;
import java.util.function.Function;
import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;
import software.amazon.awssdk.core.async.AsyncResponseTransformer;
import software.amazon.awssdk.http.AsyncExchangeCompletion;
import software.amazon.awssdk.http.AsyncHttpExchange;
import software.amazon.awssdk.http.AsyncHttpResponseHandler;
import software.amazon.awssdk.http.CancellationReason;
import software.amazon.awssdk.http.HttpBody;
import software.amazon.awssdk.http.HttpClientCapabilities;
import software.amazon.awssdk.http.HttpExecutionOptions;
import software.amazon.awssdk.http.HttpTransportMode;
import software.amazon.awssdk.http.Protocol;
import software.amazon.awssdk.http.SdkAsyncHttpClient;
import software.amazon.awssdk.http.SdkHttpRequest;
import software.amazon.awssdk.http.SdkHttpResponse;
import software.amazon.awssdk.retries.RetryScheduler;
import software.amazon.smithy.java.core.serde.document.Document;

/**
 * A controllable in-memory async transport for the dynamic streaming, paginator, waiter and cancellation tests, as
 * {@code GeneratedAsyncBehaviorTest} uses for generated async clients: exchanges are held until the test answers
 * them, every transport callback runs on one fake event-loop thread, the test pulls request bodies with explicit
 * demand, response streams emit one chunk per unit of demand, and cancelled exchanges are recorded.
 */
final class ControlledHttp implements SdkAsyncHttpClient {
    static final String EVENT_LOOP = "fake-event-loop";
    static final Duration TIMEOUT = Duration.ofSeconds(10);

    final ExecutorService eventLoop = Executors.newSingleThreadExecutor(r -> {
        Thread thread = new Thread(r, EVENT_LOOP);
        thread.setDaemon(true);
        return thread;
    });
    final BlockingQueue<Exchange> exchanges = new LinkedBlockingQueue<>();
    final AtomicInteger calls = new AtomicInteger();
    final List<SdkHttpRequest> requests = new CopyOnWriteArrayList<>();
    /** With a responder, each exchange is answered as soon as it starts. */
    volatile Function<SdkHttpRequest, Reply> auto;

    /** The next started exchange that is not answered automatically. */
    Exchange next() throws InterruptedException {
        Exchange exchange = exchanges.poll(TIMEOUT.toMillis(), TimeUnit.MILLISECONDS);
        assertNotNull(exchange, "no exchange started");
        return exchange;
    }

    @Override
    public AsyncHttpExchange execute(SdkHttpRequest request, AsyncHttpResponseHandler handler,
                                     HttpExecutionOptions options) {
        calls.incrementAndGet();
        requests.add(request);
        Exchange exchange = new Exchange(this, request, handler);
        Function<SdkHttpRequest, Reply> responder = auto;
        if (responder != null) {
            eventLoop.execute(() -> exchange.answer(responder.apply(request)));
        } else {
            exchanges.add(exchange);
        }
        return exchange.completion;
    }

    @Override
    public HttpClientCapabilities capabilities() {
        return HttpClientCapabilities.builder().mode(HttpTransportMode.ASYNC).addProtocol(Protocol.HTTP1_1)
                                     .maxBufferedBytesPerExchange(1 << 20)
                                     .cancellationTimeout(Duration.ofSeconds(1)).build();
    }

    @Override
    public String clientName() {
        return "ControlledHttp";
    }

    @Override
    public void close() {
        eventLoop.shutdownNow();
    }

    /** Waits until {@code condition} holds. */
    static void await(BooleanSupplier condition) throws InterruptedException {
        long deadline = System.nanoTime() + TIMEOUT.toNanos();
        while (!condition.getAsBoolean()) {
            if (System.nanoTime() > deadline) {
                throw new AssertionError("condition not reached within " + TIMEOUT);
            }
            Thread.sleep(1);
        }
    }

    /** Drains the event loop and lets the executors run anything still queued. */
    void settle() throws Exception {
        for (int i = 0; i < 3; i++) {
            eventLoop.submit(() -> { }).get(TIMEOUT.toMillis(), TimeUnit.MILLISECONDS);
            Thread.sleep(20);
        }
    }

    static ByteBuffer utf8(String text) {
        return ByteBuffer.wrap(text.getBytes(StandardCharsets.UTF_8));
    }

    static String text(List<ByteBuffer> chunks) {
        StringBuilder text = new StringBuilder();
        for (ByteBuffer chunk : chunks) {
            text.append(StandardCharsets.UTF_8.decode(chunk.duplicate()));
        }
        return text.toString();
    }

    /** A canned response: a buffered JSON body, or a stream that completes the exchange when it terminates. */
    record Reply(int status, Map<String, String> headers, String json, LoopPublisher stream) {
        static Reply json(int status, String json) {
            return new Reply(status, Map.of("Content-Type", "application/json"), json, null);
        }

        static Reply error(int status, String errorType) {
            return new Reply(status, Map.of("Content-Type", "application/json", "X-Amzn-ErrorType", errorType), "{}",
                             null);
        }

        static Reply stream(LoopPublisher stream, String contentType) {
            return new Reply(200, Map.of("X-Kind", "k", "Content-Type", contentType), null, stream);
        }
    }

    /** One held exchange: the test pulls its request body and answers or fails it on the event loop. */
    static final class Exchange {
        final ControlledHttp http;
        final SdkHttpRequest request;
        final AsyncHttpResponseHandler handler;
        final AsyncExchangeCompletion completion;
        final AtomicInteger cancels = new AtomicInteger();
        final List<CancellationReason> reasons = new CopyOnWriteArrayList<>();
        final List<ByteBuffer> bodyChunks = new CopyOnWriteArrayList<>();
        final AtomicBoolean bodyComplete = new AtomicBoolean();
        final AtomicReference<Throwable> bodyError = new AtomicReference<>();
        final AtomicLong pulled = new AtomicLong();
        private Subscription body;

        Exchange(ControlledHttp http, SdkHttpRequest request, AsyncHttpResponseHandler handler) {
            this.http = http;
            this.request = request;
            this.handler = handler;
            // As the SPI requires of a transport, a cancelled exchange reports its failure to the handler and
            // cancels the transport's request-body subscription.
            this.completion = new AsyncExchangeCompletion("ControlledHttp", (reason, failure) -> {
                cancels.incrementAndGet();
                reasons.add(reason);
                http.eventLoop.execute(() -> {
                    if (body != null) {
                        body.cancel();
                    }
                });
                handler.onError(failure);
            });
        }

        /** Requests {@code n} more request-body chunks on the event loop, subscribing first if needed. */
        void pullBody(long n) throws Exception {
            onLoop(() -> {
                if (body == null) {
                    assertEquals(HttpBody.Kind.STREAMING, request.body().kind());
                    request.body().streamingBody().subscribe(new Subscriber<ByteBuffer>() {
                        @Override
                        public void onSubscribe(Subscription s) {
                            body = s;
                        }

                        @Override
                        public void onNext(ByteBuffer chunk) {
                            bodyChunks.add(chunk);
                        }

                        @Override
                        public void onError(Throwable t) {
                            bodyError.set(t);
                        }

                        @Override
                        public void onComplete() {
                            bodyComplete.set(true);
                        }
                    });
                }
                pulled.addAndGet(n);
                body.request(n);
            });
        }

        String bodyText() {
            return text(bodyChunks);
        }

        void respond(Reply reply) throws Exception {
            onLoop(() -> answer(reply));
        }

        /** Runs on the event loop. */
        void answer(Reply reply) {
            if (completion.isDone()) {
                return;
            }
            SdkHttpResponse.Builder headers = SdkHttpResponse.builder().statusCode(reply.status());
            reply.headers().forEach(headers::addHeader);
            handler.onHeaders(headers.body(HttpBody.empty()).build());
            if (reply.stream() != null) {
                reply.stream().onTerminal = completion::complete;
                handler.onStream(reply.stream());
            } else {
                handler.onStream(new LoopPublisher(http.eventLoop, List.of(utf8(reply.json()))));
                completion.complete();
            }
        }

        private void onLoop(Runnable task) throws Exception {
            http.eventLoop.submit(task).get(TIMEOUT.toMillis(), TimeUnit.MILLISECONDS);
        }
    }

    /** A request-body source emitting one chunk per unit of demand on the requesting thread. */
    static final class SourcePublisher implements Publisher<ByteBuffer> {
        final List<String> chunks;
        final AtomicLong requested = new AtomicLong();
        final AtomicInteger delivered = new AtomicInteger();
        final AtomicBoolean cancelled = new AtomicBoolean();

        SourcePublisher(String... chunks) {
            this.chunks = List.of(chunks);
        }

        @Override
        public void subscribe(Subscriber<? super ByteBuffer> subscriber) {
            subscriber.onSubscribe(new Subscription() {
                private boolean done;

                @Override
                public void request(long n) {
                    requested.addAndGet(n);
                    for (long i = 0; i < n && !done && !cancelled.get(); i++) {
                        int index = delivered.get();
                        if (index == chunks.size()) {
                            break;
                        }
                        delivered.incrementAndGet();
                        subscriber.onNext(utf8(chunks.get(index)));
                    }
                    if (!done && !cancelled.get() && delivered.get() == chunks.size()) {
                        done = true;
                        subscriber.onComplete();
                    }
                }

                @Override
                public void cancel() {
                    cancelled.set(true);
                }
            });
        }
    }

    /** A response stream emitting one chunk per unit of demand, always on the event loop. */
    static final class LoopPublisher implements Publisher<ByteBuffer> {
        final ExecutorService eventLoop;
        final List<ByteBuffer> chunks;
        final AtomicInteger delivered = new AtomicInteger();
        final AtomicBoolean cancelled = new AtomicBoolean();
        volatile Runnable onTerminal = () -> { };

        LoopPublisher(ExecutorService eventLoop, List<ByteBuffer> chunks) {
            this.eventLoop = eventLoop;
            this.chunks = List.copyOf(chunks);
        }

        static LoopPublisher ofText(ExecutorService eventLoop, String... chunks) {
            List<ByteBuffer> buffers = new ArrayList<>();
            for (String chunk : chunks) {
                buffers.add(utf8(chunk));
            }
            return new LoopPublisher(eventLoop, buffers);
        }

        @Override
        public void subscribe(Subscriber<? super ByteBuffer> subscriber) {
            subscriber.onSubscribe(new Subscription() {
                private boolean done;

                @Override
                public void request(long n) {
                    eventLoop.execute(() -> {
                        for (long i = 0; i < n && !done && !cancelled.get() && delivered.get() < chunks.size(); i++) {
                            subscriber.onNext(chunks.get(delivered.getAndIncrement()).duplicate());
                        }
                        if (!done && !cancelled.get() && delivered.get() == chunks.size()) {
                            done = true;
                            subscriber.onComplete();
                            onTerminal.run();
                        }
                    });
                }

                @Override
                public void cancel() {
                    cancelled.set(true);
                }
            });
        }
    }

    /** A response transformer that requests only what the test asks for. */
    static final class DemandTransformer implements AsyncResponseTransformer<Document, String> {
        final List<ByteBuffer> received = new CopyOnWriteArrayList<>();
        final List<Thread> callbackThreads = new CopyOnWriteArrayList<>();
        final AtomicReference<Document> response = new AtomicReference<>();
        final AtomicReference<Throwable> exception = new AtomicReference<>();
        volatile Subscription subscription;
        private volatile CompletableFuture<String> result;

        @Override
        public CompletableFuture<String> prepare() {
            result = new CompletableFuture<>();
            return result;
        }

        @Override
        public void onResponse(Document output) {
            callbackThreads.add(Thread.currentThread());
            response.set(output);
        }

        @Override
        public void onStream(Publisher<ByteBuffer> publisher) {
            callbackThreads.add(Thread.currentThread());
            publisher.subscribe(new Subscriber<ByteBuffer>() {
                @Override
                public void onSubscribe(Subscription s) {
                    subscription = s;
                }

                @Override
                public void onNext(ByteBuffer chunk) {
                    received.add(chunk);
                }

                @Override
                public void onError(Throwable t) {
                    result.completeExceptionally(t);
                }

                @Override
                public void onComplete() {
                    result.complete(text(received));
                }
            });
        }

        @Override
        public void exceptionOccurred(Throwable error) {
            exception.set(error);
            CompletableFuture<String> current = result;
            if (current != null) {
                current.completeExceptionally(error);
            }
        }
    }

    /** A page subscriber that requests only what the test asks for. */
    static final class PageSubscriber implements Subscriber<Document> {
        final List<Document> pages = new CopyOnWriteArrayList<>();
        final List<Thread> threads = new CopyOnWriteArrayList<>();
        final AtomicReference<String> terminal = new AtomicReference<>();
        final AtomicReference<Throwable> error = new AtomicReference<>();
        private final CompletableFuture<Subscription> subscription = new CompletableFuture<>();

        Subscription subscription() throws Exception {
            return subscription.get(TIMEOUT.toMillis(), TimeUnit.MILLISECONDS);
        }

        @Override
        public void onSubscribe(Subscription s) {
            subscription.complete(s);
        }

        @Override
        public void onNext(Document page) {
            threads.add(Thread.currentThread());
            pages.add(page);
        }

        @Override
        public void onError(Throwable t) {
            error.set(t);
            terminal.set("error");
        }

        @Override
        public void onComplete() {
            terminal.set("complete");
        }
    }

    /** A retry scheduler whose delays complete only when the test completes them. */
    static final class HeldScheduler implements RetryScheduler {
        final BlockingQueue<CompletableFuture<Void>> delays = new LinkedBlockingQueue<>();
        final List<Duration> requested = new CopyOnWriteArrayList<>();

        @Override
        public CompletableFuture<Void> delay(Duration delay) {
            requested.add(delay);
            CompletableFuture<Void> future = new CompletableFuture<>();
            delays.add(future);
            return future;
        }

        CompletableFuture<Void> next() throws InterruptedException {
            CompletableFuture<Void> delay = delays.poll(TIMEOUT.toMillis(), TimeUnit.MILLISECONDS);
            assertNotNull(delay, "no delay was scheduled");
            return delay;
        }
    }
}
