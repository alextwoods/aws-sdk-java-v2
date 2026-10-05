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

package software.amazon.awssdk.http;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletionException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;

/**
 * The async SPI contract (Requirements 12.1, 12.5, 12.8) exercised through a minimal in-memory transport built on
 * {@link AsyncExchangeCompletion}: demand-driven request and response bodies, one terminal signal, and cancellation
 * that fails the exchange as {@link HttpFailureCategory#CANCELLED} exactly once.
 */
class AsyncHttpSpiTest {
    /** Records handler signals. */
    static final class RecordingHandler implements AsyncHttpResponseHandler {
        final List<String> signals = new ArrayList<>();
        final ChunkCollector body = new ChunkCollector();
        volatile SdkHttpResponse headers;
        volatile Throwable error;

        @Override
        public synchronized void onHeaders(SdkHttpResponse response) {
            signals.add("headers");
            headers = response;
        }

        @Override
        public synchronized void onStream(Publisher<ByteBuffer> publisher) {
            signals.add("stream");
            publisher.subscribe(body);
        }

        @Override
        public synchronized void onError(Throwable e) {
            signals.add("error");
            error = e;
        }
    }

    /** Requests one chunk at a time, only when told to. */
    static final class ChunkCollector implements Subscriber<ByteBuffer> {
        final ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        final AtomicInteger terminals = new AtomicInteger();
        volatile Subscription subscription;
        volatile Throwable error;
        volatile boolean completed;

        @Override
        public void onSubscribe(Subscription s) {
            subscription = s;
        }

        @Override
        public void onNext(ByteBuffer chunk) {
            byte[] data = new byte[chunk.remaining()];
            chunk.get(data);
            bytes.writeBytes(data);
        }

        @Override
        public void onError(Throwable t) {
            terminals.incrementAndGet();
            error = t;
        }

        @Override
        public void onComplete() {
            terminals.incrementAndGet();
            completed = true;
        }
    }

    /**
     * An in-memory async transport: it reads the request body with demand one chunk at a time, then waits for
     * {@link Exchange#respond} to deliver a response whose body is a bounded {@link StreamingBody} publisher.
     */
    static final class InMemoryAsyncHttpClient implements SdkAsyncHttpClient {
        final List<Exchange> exchanges = new ArrayList<>();

        @Override
        public AsyncHttpExchange execute(SdkHttpRequest request, AsyncHttpResponseHandler handler,
                                         HttpExecutionOptions options) {
            Exchange exchange = new Exchange(handler);
            exchanges.add(exchange);
            exchange.transmit(request);
            return exchange.completion;
        }

        @Override
        public HttpClientCapabilities capabilities() {
            return HttpClientCapabilities.builder().mode(HttpTransportMode.ASYNC)
                                         .addProtocol(Protocol.HTTP1_1, Protocol.HTTP2)
                                         .maxBufferedBytesPerExchange(StreamingBodies.DEFAULT_MAX_CHUNK_SIZE)
                                         .cancellationTimeout(Duration.ofSeconds(1)).build();
        }

        @Override
        public String clientName() {
            return "InMemoryAsync";
        }

        @Override
        public void close() {
            for (Exchange exchange : exchanges) {
                exchange.completion.cancel(CancellationReason.CLIENT_CLOSED);
            }
        }

        final class Exchange {
            final AsyncHttpResponseHandler handler;
            final AsyncExchangeCompletion completion;
            final ByteArrayOutputStream requestBytes = new ByteArrayOutputStream();
            final AtomicInteger maxOutstandingRequestDemand = new AtomicInteger();
            final AtomicReference<Subscription> requestSubscription = new AtomicReference<>();
            final AtomicReference<Subscription> responseUpstream = new AtomicReference<>();
            volatile Subscriber<? super ByteBuffer> responseSubscriber;

            Exchange(AsyncHttpResponseHandler handler) {
                this.handler = handler;
                this.completion = new AsyncExchangeCompletion(clientName(), (reason, failure) -> {
                    Subscription request = requestSubscription.get();
                    if (request != null) {
                        request.cancel();
                    }
                    Subscription upstream = responseUpstream.get();
                    if (upstream != null) {
                        upstream.cancel();
                    }
                    Subscriber<? super ByteBuffer> subscriber = responseSubscriber;
                    if (subscriber != null) {
                        subscriber.onError(failure);
                    } else {
                        handler.onError(failure);
                    }
                });
            }

            void transmit(SdkHttpRequest request) {
                completion.enter(HttpPhase.REQUEST_TRANSMISSION);
                if (request.body().kind() != HttpBody.Kind.STREAMING) {
                    completion.enter(HttpPhase.RESPONSE_CONSUMPTION);
                    return;
                }
                request.body().streamingBody().subscribe(new Subscriber<ByteBuffer>() {
                    @Override
                    public void onSubscribe(Subscription s) {
                        requestSubscription.set(s);
                        maxOutstandingRequestDemand.set(1);
                        s.request(1); // the "socket" accepts one chunk at a time
                    }

                    @Override
                    public void onNext(ByteBuffer chunk) {
                        byte[] data = new byte[chunk.remaining()];
                        chunk.get(data);
                        requestBytes.writeBytes(data);
                        requestSubscription.get().request(1);
                    }

                    @Override
                    public void onError(Throwable t) {
                        if (completion.fail(t)) {
                            handler.onError(completion.asTransportException(t));
                        }
                    }

                    @Override
                    public void onComplete() {
                        completion.enter(HttpPhase.RESPONSE_CONSUMPTION);
                    }
                });
            }

            void respond(int status, byte[] body) {
                if (completion.isDone()) {
                    return;
                }
                handler.onHeaders(SdkHttpResponse.builder().statusCode(status).build());
                StreamingBody source = StreamingBodies.fromBytes(body, null);
                handler.onStream(subscriber -> {
                    responseSubscriber = subscriber;
                    source.subscribe(new Subscriber<ByteBuffer>() {
                        @Override
                        public void onSubscribe(Subscription s) {
                            responseUpstream.set(s);
                            subscriber.onSubscribe(s);
                        }

                        @Override
                        public void onNext(ByteBuffer chunk) {
                            subscriber.onNext(chunk);
                        }

                        @Override
                        public void onError(Throwable t) {
                            if (completion.fail(t)) {
                                subscriber.onError(completion.asTransportException(t));
                            }
                        }

                        @Override
                        public void onComplete() {
                            if (completion.complete()) {
                                subscriber.onComplete();
                            }
                        }
                    });
                });
            }
        }
    }

    private static SdkHttpRequest put(byte[] body) {
        return SdkHttpRequest.builder().method("PUT").host("h")
                             .body(HttpBody.streaming(StreamingBodies.fromBytes(body, null))).build();
    }

    @Test
    void bodiesMoveOnlyOnDemandAndTheExchangeCompletesOnce() {
        byte[] requestBody = new byte[3 * StreamingBodies.DEFAULT_MAX_CHUNK_SIZE + 5];
        byte[] responseBody = new byte[2 * StreamingBodies.DEFAULT_MAX_CHUNK_SIZE + 1];
        responseBody[responseBody.length - 1] = 7;
        InMemoryAsyncHttpClient client = new InMemoryAsyncHttpClient();
        RecordingHandler handler = new RecordingHandler();
        AsyncHttpExchange exchange = client.execute(put(requestBody), handler, HttpExecutionOptions.defaults());
        InMemoryAsyncHttpClient.Exchange state = client.exchanges.get(0);
        assertArrayEquals(requestBody, state.requestBytes.toByteArray());
        assertSame(exchange.completion(), exchange.completion());

        state.respond(200, responseBody);
        assertEquals(List.of("headers", "stream"), handler.signals);
        assertEquals(0, handler.body.bytes.size(), "no chunk without demand");
        handler.body.subscription.request(1);
        assertEquals(StreamingBodies.DEFAULT_MAX_CHUNK_SIZE, handler.body.bytes.size());
        assertFalse(exchange.completion().isDone());
        handler.body.subscription.request(Long.MAX_VALUE);
        assertArrayEquals(responseBody, handler.body.bytes.toByteArray());
        assertTrue(handler.body.completed);
        assertEquals(1, handler.body.terminals.get());
        assertNull(exchange.completion().join());
        assertFalse(exchange.cancel(CancellationReason.CALLER_CANCELLED), "cancel after terminal");
        assertEquals(1, handler.body.terminals.get());
    }

    @Test
    void cancelBeforeTheResponseFailsTheHandlerOnceAsCancelled() {
        InMemoryAsyncHttpClient client = new InMemoryAsyncHttpClient();
        RecordingHandler handler = new RecordingHandler();
        AsyncHttpExchange exchange = client.execute(SdkHttpRequest.builder().method("GET").host("h").build(), handler,
                                                    HttpExecutionOptions.defaults());
        assertTrue(exchange.cancel(CancellationReason.API_CALL_ATTEMPT_TIMEOUT));
        assertFalse(exchange.cancel(CancellationReason.CALLER_CANCELLED));
        client.exchanges.get(0).respond(200, new byte[1]);
        assertEquals(List.of("error"), handler.signals);
        HttpTransportException error = assertInstanceOf(HttpTransportException.class, handler.error);
        assertEquals(HttpFailureCategory.CANCELLED, error.category());
        assertEquals(HttpPhase.RESPONSE_CONSUMPTION, error.phase());
        assertEquals("InMemoryAsync", error.clientName().orElseThrow());
        CompletionException thrown = org.junit.jupiter.api.Assertions.assertThrows(
            CompletionException.class, () -> exchange.completion().join());
        assertSame(error, thrown.getCause());
    }

    @Test
    void cancelDuringTheBodySignalsTheSubscriberOnceAndStopsDelivery() {
        InMemoryAsyncHttpClient client = new InMemoryAsyncHttpClient();
        RecordingHandler handler = new RecordingHandler();
        AsyncHttpExchange exchange = client.execute(SdkHttpRequest.builder().method("GET").host("h").build(), handler,
                                                    HttpExecutionOptions.defaults());
        client.exchanges.get(0).respond(200, new byte[3 * StreamingBodies.DEFAULT_MAX_CHUNK_SIZE]);
        handler.body.subscription.request(1);
        client.close();
        handler.body.subscription.request(10);
        assertEquals(StreamingBodies.DEFAULT_MAX_CHUNK_SIZE, handler.body.bytes.size());
        assertEquals(1, handler.body.terminals.get());
        assertEquals(HttpFailureCategory.CANCELLED,
                     assertInstanceOf(HttpTransportException.class, handler.body.error).category());
        assertEquals(List.of("headers", "stream"), handler.signals);
        assertTrue(exchange.completion().isCompletedExceptionally());
    }

    @Test
    void requestBodyFailureIsCategorizedAsTransmission() {
        InMemoryAsyncHttpClient client = new InMemoryAsyncHttpClient();
        RecordingHandler handler = new RecordingHandler();
        StreamingBody failing = StreamingBodies.fromInputStream(new java.io.InputStream() {
            @Override
            public int read() throws IOException {
                throw new IOException("disk gone");
            }
        }, 10, null);
        AsyncHttpExchange exchange = client.execute(SdkHttpRequest.builder().method("PUT").host("h")
                                                                  .body(HttpBody.streaming(failing)).build(),
                                                    handler, HttpExecutionOptions.defaults());
        assertEquals(List.of("error"), handler.signals);
        HttpTransportException error = assertInstanceOf(HttpTransportException.class, handler.error);
        assertEquals(HttpFailureCategory.REQUEST_TRANSMISSION, error.category());
        assertTrue(exchange.completion().isCompletedExceptionally());
    }

    @Test
    void racingCompleteFailAndCancelHaveExactlyOneWinner() throws Exception {
        ExecutorService pool = Executors.newFixedThreadPool(3);
        try {
            for (int i = 0; i < 500; i++) {
                AtomicInteger cancelActions = new AtomicInteger();
                AsyncExchangeCompletion completion =
                    new AsyncExchangeCompletion("race", (reason, failure) -> cancelActions.incrementAndGet());
                CyclicBarrier start = new CyclicBarrier(3);
                AtomicInteger winners = new AtomicInteger();
                CountDownLatch done = new CountDownLatch(3);
                Runnable[] contenders = {
                    () -> count(winners, completion.complete()),
                    () -> count(winners, completion.fail(new IOException("reset"))),
                    () -> count(winners, completion.cancel(CancellationReason.CALLER_CANCELLED))
                };
                for (Runnable contender : contenders) {
                    pool.execute(() -> {
                        try {
                            start.await();
                            contender.run();
                        } catch (Exception e) {
                            throw new IllegalStateException(e);
                        } finally {
                            done.countDown();
                        }
                    });
                }
                assertTrue(done.await(5, TimeUnit.SECONDS));
                assertEquals(1, winners.get());
                assertTrue(cancelActions.get() <= 1);
                assertTrue(completion.isDone() && completion.completion().isDone());
            }
        } finally {
            pool.shutdownNow();
        }
    }

    private static void count(AtomicInteger winners, boolean won) {
        if (won) {
            winners.incrementAndGet();
        }
    }

    @Test
    void completingTheReturnedFutureDoesNotTerminateTheExchange() {
        AsyncExchangeCompletion completion = new AsyncExchangeCompletion("x", (reason, failure) -> { });
        completion.completion().complete(null);
        assertFalse(completion.isDone());
        assertTrue(completion.cancel(CancellationReason.CALLER_CANCELLED));
        assertFalse(completion.fail(new IOException()));
    }
}
