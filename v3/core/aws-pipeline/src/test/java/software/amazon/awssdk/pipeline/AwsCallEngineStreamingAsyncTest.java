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

package software.amazon.awssdk.pipeline;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static software.amazon.awssdk.pipeline.PipelineFixtures.GET_INPUT;
import static software.amazon.awssdk.pipeline.PipelineFixtures.GET_THING;
import static software.amazon.awssdk.pipeline.PipelineFixtures.STREAM_OUTPUT;
import static software.amazon.awssdk.pipeline.PipelineFixtures.STREAM_THING;
import static software.amazon.awssdk.pipeline.PipelineFixtures.retries;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;
import software.amazon.awssdk.awscore.exception.AwsServiceException;
import software.amazon.awssdk.core.ResponseBytes;
import software.amazon.awssdk.core.async.AsyncResponseTransformer;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.http.AsyncExchangeCompletion;
import software.amazon.awssdk.http.AsyncHttpExchange;
import software.amazon.awssdk.http.AsyncHttpResponseHandler;
import software.amazon.awssdk.http.CancellationReason;
import software.amazon.awssdk.http.HttpBody;
import software.amazon.awssdk.http.HttpClientCapabilities;
import software.amazon.awssdk.http.HttpExecutionOptions;
import software.amazon.awssdk.http.HttpTransportMode;
import software.amazon.awssdk.http.SdkAsyncHttpClient;
import software.amazon.awssdk.http.SdkHttpRequest;
import software.amazon.awssdk.http.SdkHttpResponse;
import software.amazon.awssdk.pipeline.PipelineFixtures.Struct;

/** The async driver's streaming-output path: the transformer consumes the live payload under its own demand. */
class AwsCallEngineStreamingAsyncTest {
    private static final List<String> CHUNKS = List.of("streamed ", "payload ", "bytes");

    private final ExecutorService completion = Executors.newSingleThreadExecutor(r -> new Thread(r, "completion"));
    private final ExecutorService eventLoop = Executors.newSingleThreadExecutor(r -> new Thread(r, "event-loop"));
    private final FakeAsyncHttp http = new FakeAsyncHttp();
    private final AwsCallEngine engine = AwsCallEngine.builder()
                                                      .runtime(PipelineFixtures.engineRuntime(
                                                                                   retries(3), HttpTransportMode.ASYNC)
                                                                               .completionExecutor(completion)
                                                                               .retryScheduler(
                                                                                   d -> CompletableFuture
                                                                                       .completedFuture(null))
                                                                               .build())
                                                      .asyncHttpClient(http)
                                                      .build();

    @AfterEach
    void shutDown() {
        completion.shutdownNow();
        eventLoop.shutdownNow();
    }

    @Test
    void transformer_receivesTheOutputAndTheExactPayload_underBoundedDemand() throws Exception {
        ChunkedBody body = http.stream(200, "v1");

        ResponseBytes<Struct> result = execute(AsyncResponseTransformer.toBytes()).get(5, TimeUnit.SECONDS);

        assertThat((String) result.response().getMemberValue(STREAM_OUTPUT.member("Value"))).isEqualTo("v1");
        assertThat(result.asUtf8String()).isEqualTo(String.join("", CHUNKS));
        assertThat(body.requests).isNotEmpty().allMatch(n -> n < Long.MAX_VALUE);
        assertThat(body.requests.stream().mapToLong(Long::longValue).sum()).isLessThanOrEqualTo(CHUNKS.size() + 1);
        assertThat(http.transmitted).hasSize(1);
    }

    @Test
    void errorResponse_isRetried_beforeTheTransformerRuns() throws Exception {
        http.stream(503, null);
        http.stream(200, "v2");

        ResponseBytes<Struct> result = execute(AsyncResponseTransformer.toBytes()).get(5, TimeUnit.SECONDS);

        assertThat((String) result.response().getMemberValue(STREAM_OUTPUT.member("Value"))).isEqualTo("v2");
        assertThat(result.asUtf8String()).isEqualTo(String.join("", CHUNKS));
        assertThat(http.transmitted).hasSize(2);
    }

    @Test
    void retriesExhausted_surfacesTheServiceError_withoutPreparingTheTransformer() {
        http.stream(503, null);
        http.stream(503, null);
        http.stream(503, null);
        HoldingTransformer transformer = new HoldingTransformer();

        assertThat(failure(execute(transformer)))
            .isInstanceOfSatisfying(AwsServiceException.class, e -> assertThat(e.statusCode()).isEqualTo(503));
        assertThat(transformer.prepared).isZero();
    }

    @Test
    void transformerFailure_isMappedOnce_andNotRetried() {
        http.stream(200, "v1");
        http.stream(200, "unused");
        IllegalStateException boom = new IllegalStateException("disk full");
        HoldingTransformer transformer = new HoldingTransformer();
        transformer.failOnFirstChunk = boom;

        assertThat(failure(execute(transformer)))
            .isInstanceOf(SdkClientException.class)
            .hasMessageContaining("com.example#StreamThing")
            .hasCause(boom);
        assertThat(http.transmitted).hasSize(1);
        assertThat(transformer.prepared).isEqualTo(1);
    }

    @Test
    void cancellingTheFuture_cancelsThePayloadSubscription_andTellsTheTransformer() throws Exception {
        ChunkedBody body = http.stream(200, "v1");
        HoldingTransformer transformer = new HoldingTransformer();

        CompletableFuture<String> future = execute(transformer);
        assertThat(transformer.firstChunk.await(5, TimeUnit.SECONDS)).isTrue();
        // The transformer asked for one chunk and holds: the transport delivered no more than it asked for.
        assertThat(body.delivered).isEqualTo(1);
        assertThat(body.requests).allMatch(n -> n < Long.MAX_VALUE);

        assertThat(future.cancel(true)).isTrue();

        assertThat(body.cancelled.await(5, TimeUnit.SECONDS)).isTrue();
        assertThat(transformer.failure.get(5, TimeUnit.SECONDS)).isInstanceOf(CancellationException.class);
        assertThat(future).isCancelled();
        assertThat(http.cancelled).contains(CancellationReason.CALLER_CANCELLED);
    }

    @Test
    void nonStreamingPlan_isRejectedBeforeTheCallStarts() {
        assertThatThrownBy(() -> engine.executeAsync(engine.runtime().plan(GET_THING),
                                                     PipelineFixtures.request(GET_INPUT, "a"), null, null,
                                                     AsyncResponseTransformer.toBytes()))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("no streaming blob output member");
        assertThat(http.transmitted).isEmpty();
    }

    private <R> CompletableFuture<R> execute(AsyncResponseTransformer<Struct, R> transformer) {
        return engine.executeAsync(engine.runtime().plan(STREAM_THING), PipelineFixtures.request(GET_INPUT, "a"),
                                   null, null, transformer);
    }

    private static Throwable failure(CompletableFuture<?> future) {
        try {
            future.get(5, TimeUnit.SECONDS);
        } catch (ExecutionException e) {
            return e.getCause();
        } catch (Exception e) {
            throw new AssertionError(e);
        }
        throw new AssertionError("The call succeeded");
    }

    /** A transformer that requests one chunk at a time and holds after the first, or fails on it. */
    private static final class HoldingTransformer implements AsyncResponseTransformer<Struct, String> {
        private final CountDownLatch firstChunk = new CountDownLatch(1);
        private final CompletableFuture<Throwable> failure = new CompletableFuture<>();
        private volatile RuntimeException failOnFirstChunk;
        private volatile int prepared;
        private CompletableFuture<String> result;

        @Override
        public CompletableFuture<String> prepare() {
            prepared++;
            result = new CompletableFuture<>();
            return result;
        }

        @Override
        public void onResponse(Struct response) {
        }

        @Override
        public void onStream(Publisher<ByteBuffer> publisher) {
            publisher.subscribe(new Subscriber<>() {
                @Override
                public void onSubscribe(Subscription subscription) {
                    subscription.request(1);
                }

                @Override
                public void onNext(ByteBuffer chunk) {
                    firstChunk.countDown();
                    if (failOnFirstChunk != null) {
                        result.completeExceptionally(failOnFirstChunk);
                    }
                }

                @Override
                public void onError(Throwable error) {
                    result.completeExceptionally(error);
                }

                @Override
                public void onComplete() {
                    result.complete("done");
                }
            });
        }

        @Override
        public void exceptionOccurred(Throwable error) {
            failure.complete(error);
            result.completeExceptionally(error);
        }
    }

    /** A response body of {@link #CHUNKS} that records every demand signal and delivers at most what is asked. */
    private final class ChunkedBody implements Publisher<ByteBuffer> {
        private final List<Long> requests = new CopyOnWriteArrayList<>();
        private final CountDownLatch cancelled = new CountDownLatch(1);
        private volatile int delivered;

        @Override
        public void subscribe(Subscriber<? super ByteBuffer> subscriber) {
            subscriber.onSubscribe(new Subscription() {
                private long demand;
                private boolean draining;
                private boolean done;

                @Override
                public void request(long n) {
                    requests.add(n);
                    // Demand and delivery are confined to the event loop, as in a transport.
                    eventLoop.execute(() -> {
                        demand += n;
                        drain();
                    });
                }

                private void drain() {
                    if (draining) {
                        return;
                    }
                    draining = true;
                    while (!done && demand > 0 && delivered < CHUNKS.size()) {
                        demand--;
                        String chunk = CHUNKS.get(delivered++);
                        subscriber.onNext(ByteBuffer.wrap(chunk.getBytes(StandardCharsets.UTF_8)));
                    }
                    draining = false;
                    if (!done && delivered == CHUNKS.size()) {
                        done = true;
                        subscriber.onComplete();
                    }
                }

                @Override
                public void cancel() {
                    done = true;
                    cancelled.countDown();
                }
            });
        }
    }

    /** A scripted async transport: headers then a {@link ChunkedBody}, on the event loop. */
    private final class FakeAsyncHttp implements SdkAsyncHttpClient {
        private final Queue<SdkHttpResponse> outcomes = new ConcurrentLinkedQueue<>();
        private final Queue<ChunkedBody> bodies = new ConcurrentLinkedQueue<>();
        private final List<SdkHttpRequest> transmitted = new CopyOnWriteArrayList<>();
        private final List<CancellationReason> cancelled = new ArrayList<>();

        ChunkedBody stream(int status, String value) {
            SdkHttpResponse.Builder response = SdkHttpResponse.builder().statusCode(status).body(HttpBody.empty());
            if (value != null) {
                response.addHeader("X-Value", value);
            }
            ChunkedBody body = new ChunkedBody();
            outcomes.add(response.build());
            bodies.add(body);
            return body;
        }

        @Override
        public AsyncHttpExchange execute(SdkHttpRequest request, AsyncHttpResponseHandler handler,
                                         HttpExecutionOptions options) {
            transmitted.add(request);
            SdkHttpResponse response = outcomes.remove();
            ChunkedBody body = bodies.remove();
            AsyncExchangeCompletion exchange = new AsyncExchangeCompletion(clientName(), (reason, failure) -> {
                synchronized (cancelled) {
                    cancelled.add(reason);
                }
                eventLoop.execute(() -> handler.onError(failure));
            });
            eventLoop.execute(() -> {
                handler.onHeaders(response);
                handler.onStream(body);
            });
            return exchange;
        }

        @Override
        public HttpClientCapabilities capabilities() {
            throw new UnsupportedOperationException();
        }

        @Override
        public String clientName() {
            return "FakeAsyncHttp";
        }

        @Override
        public void close() {
        }
    }
}
