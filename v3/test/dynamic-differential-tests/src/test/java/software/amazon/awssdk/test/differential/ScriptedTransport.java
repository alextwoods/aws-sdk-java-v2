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

package software.amazon.awssdk.test.differential;

import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicReference;
import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;
import software.amazon.awssdk.http.AsyncExchangeCompletion;
import software.amazon.awssdk.http.AsyncHttpExchange;
import software.amazon.awssdk.http.AsyncHttpResponseHandler;
import software.amazon.awssdk.http.HttpBody;
import software.amazon.awssdk.http.HttpClientCapabilities;
import software.amazon.awssdk.http.HttpExecutionOptions;
import software.amazon.awssdk.http.HttpTransportMode;
import software.amazon.awssdk.http.Protocol;
import software.amazon.awssdk.http.SdkAsyncHttpClient;
import software.amazon.awssdk.http.SdkHttpRequest;
import software.amazon.awssdk.http.SdkHttpResponse;
import software.amazon.awssdk.http.SdkSyncHttpClient;
import software.amazon.awssdk.http.SyncHttpExchange;
import software.amazon.awssdk.test.differential.Matrix.Reply;

/**
 * The scripted transport both clients of a cell call: it answers the {@code n}th request with the {@code n}th reply
 * (the last one for later requests) and records every request exactly as transmitted, with its complete body.
 *
 * <p>In sync mode the body is buffered from the request and the response body is the reply's chunks concatenated. In
 * async mode the request body is drained on the transport's own event loop before the response is delivered on that
 * loop, one reply chunk (an event stream frame) per unit of demand. Caller-owned: neither client closes it.
 */
final class ScriptedTransport implements SdkSyncHttpClient, SdkAsyncHttpClient {
    private static final String NAME = "DifferentialHttp";

    private final HttpTransportMode mode;
    private final List<Reply> replies;
    private final List<Exchange> exchanges = new CopyOnWriteArrayList<>();
    private final ExecutorService loop = Executors.newSingleThreadExecutor(r -> {
        Thread thread = new Thread(r, "differential-event-loop");
        thread.setDaemon(true);
        return thread;
    });

    /** One transmitted request and its complete body. */
    static final class Exchange {
        final SdkHttpRequest request;
        final ByteArrayOutputStream body = new ByteArrayOutputStream();
        volatile String bodyError;

        Exchange(SdkHttpRequest request) {
            this.request = request;
        }

        byte[] bodyBytes() {
            synchronized (body) {
                return body.toByteArray();
            }
        }
    }

    ScriptedTransport(HttpTransportMode mode, List<Reply> replies) {
        this.mode = mode;
        this.replies = replies;
    }

    List<Exchange> exchanges() {
        return exchanges;
    }

    private Reply reply(int index) {
        return replies.get(Math.min(index, replies.size() - 1));
    }

    private static SdkHttpResponse headers(Reply reply, int number) {
        SdkHttpResponse.Builder response = SdkHttpResponse.builder().statusCode(reply.status())
                                                          .addHeader("x-amzn-RequestId", "req-" + number);
        for (String[] header : reply.headers()) {
            response.addHeader(header[0], header[1]);
        }
        return response.body(HttpBody.empty()).build();
    }

    // -------------------------------------------------------------------------------------------------------------
    // Sync

    @Override
    public SyncHttpExchange prepare(SdkHttpRequest request, HttpExecutionOptions options) {
        return new SyncHttpExchange() {
            @Override
            public SdkHttpResponse execute() {
                Exchange exchange = new Exchange(request);
                int index = exchanges.size();
                exchanges.add(exchange);
                HttpBody body = request.body();
                byte[] bytes = switch (body.kind()) {
                    case EMPTY -> new byte[0];
                    case BYTES -> body.asByteArray();
                    case STREAMING -> body.buffered().asByteArray();
                };
                synchronized (exchange.body) {
                    exchange.body.writeBytes(bytes);
                }
                Reply reply = reply(index);
                byte[] responseBody = reply.body();
                return headers(reply, index + 1).toBuilder()
                                                .body(responseBody.length == 0 ? HttpBody.empty()
                                                                               : HttpBody.fromBytes(responseBody))
                                                .build();
            }

            @Override
            public void abort() {
            }

            @Override
            public void close() {
            }
        };
    }

    // -------------------------------------------------------------------------------------------------------------
    // Async

    @Override
    public AsyncHttpExchange execute(SdkHttpRequest request, AsyncHttpResponseHandler handler,
                                     HttpExecutionOptions options) {
        AtomicReference<Subscription> bodySubscription = new AtomicReference<>();
        AsyncExchangeCompletion completion = new AsyncExchangeCompletion(NAME, (reason, failure) -> {
            Subscription subscription = bodySubscription.get();
            if (subscription != null) {
                subscription.cancel();
            }
            handler.onError(failure);
        });
        Exchange exchange = new Exchange(request);
        int index;
        synchronized (exchanges) {
            index = exchanges.size();
            exchanges.add(exchange);
        }
        loop.execute(() -> {
            HttpBody body = request.body();
            switch (body.kind()) {
                case EMPTY -> answer(handler, completion, index);
                case BYTES -> {
                    synchronized (exchange.body) {
                        exchange.body.writeBytes(body.asByteArray());
                    }
                    answer(handler, completion, index);
                }
                case STREAMING -> body.streamingBody().subscribe(new Subscriber<ByteBuffer>() {
                    @Override
                    public void onSubscribe(Subscription subscription) {
                        bodySubscription.set(subscription);
                        subscription.request(Long.MAX_VALUE);
                    }

                    @Override
                    public void onNext(ByteBuffer chunk) {
                        byte[] bytes = Values.bytes(chunk);
                        synchronized (exchange.body) {
                            exchange.body.writeBytes(bytes);
                        }
                    }

                    @Override
                    public void onError(Throwable t) {
                        exchange.bodyError = String.valueOf(t);
                        loop.execute(() -> {
                            handler.onError(t);
                            completion.fail(t);
                        });
                    }

                    @Override
                    public void onComplete() {
                        loop.execute(() -> answer(handler, completion, index));
                    }
                });
            }
        });
        return completion;
    }

    /** Runs on the event loop. */
    private void answer(AsyncHttpResponseHandler handler, AsyncExchangeCompletion completion, int index) {
        if (completion.isDone()) {
            return;
        }
        Reply reply = reply(index);
        handler.onHeaders(headers(reply, index + 1));
        handler.onStream(new ChunkPublisher(reply.chunks(), completion::complete));
    }

    /** A response body emitting one chunk per unit of demand on the event loop, then completing the exchange. */
    private final class ChunkPublisher implements Publisher<ByteBuffer> {
        private final List<byte[]> chunks;
        private final Runnable onTerminal;

        ChunkPublisher(List<byte[]> chunks, Runnable onTerminal) {
            this.chunks = chunks;
            this.onTerminal = onTerminal;
        }

        @Override
        public void subscribe(Subscriber<? super ByteBuffer> subscriber) {
            subscriber.onSubscribe(new Subscription() {
                private int delivered;
                private boolean done;

                @Override
                public void request(long n) {
                    loop.execute(() -> {
                        for (long i = 0; i < n && !done && delivered < chunks.size(); i++) {
                            subscriber.onNext(ByteBuffer.wrap(chunks.get(delivered++)).asReadOnlyBuffer());
                        }
                        if (!done && delivered == chunks.size()) {
                            done = true;
                            subscriber.onComplete();
                            onTerminal.run();
                        }
                    });
                }

                @Override
                public void cancel() {
                    loop.execute(() -> done = true);
                }
            });
        }
    }

    // -------------------------------------------------------------------------------------------------------------

    @Override
    public HttpClientCapabilities capabilities() {
        return HttpClientCapabilities.builder().mode(mode).addProtocol(Protocol.HTTP1_1)
                                     .maxBufferedBytesPerExchange(1 << 20)
                                     .cancellationTimeout(Duration.ofSeconds(1)).build();
    }

    @Override
    public String clientName() {
        return NAME;
    }

    @Override
    public void close() {
        loop.shutdownNow();
    }
}
