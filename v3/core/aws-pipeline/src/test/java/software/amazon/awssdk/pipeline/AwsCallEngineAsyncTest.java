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
import static org.assertj.core.api.Assertions.fail;
import static software.amazon.awssdk.pipeline.PipelineFixtures.GET_INPUT;
import static software.amazon.awssdk.pipeline.PipelineFixtures.GET_OUTPUT;
import static software.amazon.awssdk.pipeline.PipelineFixtures.GET_THING;
import static software.amazon.awssdk.pipeline.PipelineFixtures.PUT_INPUT;
import static software.amazon.awssdk.pipeline.PipelineFixtures.PUT_THING;
import static software.amazon.awssdk.pipeline.PipelineFixtures.retries;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.Queue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ForkJoinWorkerThread;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.reactivestreams.Publisher;
import org.reactivestreams.Subscription;
import software.amazon.awssdk.auth.api.AuthSchemeSelector;
import software.amazon.awssdk.auth.api.ModeledAuth;
import software.amazon.awssdk.auth.api.NoAuthAuthScheme;
import software.amazon.awssdk.awscore.AwsRequestOverrideConfiguration;
import software.amazon.awssdk.awscore.exception.AwsServiceException;
import software.amazon.awssdk.core.client.config.SdkClientConfiguration;
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
import software.amazon.awssdk.pipeline.PipelineFixtures.HeaderProtocol;
import software.amazon.awssdk.pipeline.PipelineFixtures.RecordingRetryStrategy;
import software.amazon.awssdk.pipeline.PipelineFixtures.Struct;
import software.amazon.awssdk.retries.BackoffStrategy;
import software.amazon.awssdk.retries.RetryScheduler;
import software.amazon.awssdk.retries.RetryStrategy;
import software.amazon.awssdk.retries.StandardRetryStrategy;

/**
 * The async driver of {@link AwsCallEngine} over the shared stages, against a scripted transport that signals on its
 * own event-loop thread and a single-threaded completion executor.
 */
class AwsCallEngineAsyncTest {
    private static final String COMPLETION_THREAD = "sdk-completion";
    private static final String EVENT_LOOP_THREAD = "fake-event-loop";

    private final ExecutorService completion = Executors.newSingleThreadExecutor(
        r -> new Thread(r, COMPLETION_THREAD));
    private final ExecutorService eventLoop = Executors.newSingleThreadExecutor(
        r -> new Thread(r, EVENT_LOOP_THREAD));
    private final FakeScheduler scheduler = new FakeScheduler();
    private final FakeAsyncHttp http = new FakeAsyncHttp();
    private final HeaderProtocol protocol = new HeaderProtocol();

    @AfterEach
    void shutDown() {
        completion.shutdownNow();
        eventLoop.shutdownNow();
    }

    @Test
    void executeAsync_success_serializesTransmitsOnceAndDeserializes() throws Exception {
        AwsCallEngine engine = engine(runtime(retries(3)).build());
        http.respond(200, "v1");

        Struct output = await(execute(engine, GET_THING, "a"));

        assertThat((String) output.getMemberValue(GET_OUTPUT.member("Value"))).isEqualTo("v1");
        assertThat(http.transmitted).hasSize(1);
        SdkHttpRequest sent = http.transmitted.get(0);
        assertThat(sent.host()).isEqualTo("things.example.com");
        assertThat(sent.headers().firstValue("X-Name")).contains("a");
        assertThat(sent.body().asUtf8String()).isEqualTo("a");
    }

    @Test
    void executeAsync_returnsTheFutureIncompleteWhileTheTransportIsBlocked() throws Exception {
        AwsCallEngine engine = engine(runtime(retries(3)).build());
        http.hold(200, "v1");

        CompletableFuture<Struct> future = execute(engine, GET_THING, "a");
        assertThat(http.executed.await(5, TimeUnit.SECONDS)).isTrue();
        settle();

        assertThat(future).isNotDone();
        http.release();
        assertThat((String) await(future).getMemberValue(GET_OUTPUT.member("Value"))).isEqualTo("v1");
    }

    @Test
    void executeAsync_continuationsRunOnTheConfiguredExecutor() throws Exception {
        AwsCallEngine engine = engine(runtime(retries(3)).build());
        http.hold(503, null).respond(200, "v2");
        CompletableFuture<Struct> future = execute(engine, GET_THING, "a");
        AtomicReference<Thread> completedOn = new AtomicReference<>();
        CompletableFuture<Struct> dependent =
            future.whenComplete((output, error) -> completedOn.set(Thread.currentThread()));
        assertThat(http.executed.await(5, TimeUnit.SECONDS)).isTrue();

        http.release();
        await(dependent);

        assertThat(protocol.codecThreads).hasSize(2).allSatisfy(AwsCallEngineAsyncTest::isCompletionThread);
        assertThat(http.executeThreads).hasSize(2).allSatisfy(AwsCallEngineAsyncTest::isCompletionThread);
        isCompletionThread(completedOn.get());
    }

    @Test
    void executeAsync_retryableFailures_eachGrantedTokenAuthorizesOneMoreTransmission() throws Exception {
        AwsCallEngine engine = engine(runtime(retries(3)).build());
        http.respond(503, null).respond(500, null).respond(200, "v3");

        Struct output = await(execute(engine, GET_THING, "a"));

        assertThat((String) output.getMemberValue(GET_OUTPUT.member("Value"))).isEqualTo("v3");
        assertThat(http.transmitted).hasSize(3);
    }

    @Test
    void executeAsync_retriesExhausted_failsWithTheLastServiceFailure() {
        AwsCallEngine engine = engine(runtime(retries(2)).build());
        http.respond(503, null).respond(503, null).respond(200, "unused");

        assertThat(failure(execute(engine, GET_THING, "a")))
            .isInstanceOfSatisfying(AwsServiceException.class, e -> assertThat(e.statusCode()).isEqualTo(503));
        assertThat(http.transmitted).hasSize(2);
    }

    @Test
    void executeAsync_nonRetryableServiceError_failsAfterOneTransmission() {
        AwsCallEngine engine = engine(runtime(retries(3)).build());
        http.respond(400, null).respond(200, "unused");

        assertThat(failure(execute(engine, GET_THING, "a")))
            .isInstanceOfSatisfying(AwsServiceException.class, e -> {
                assertThat(e.statusCode()).isEqualTo(400);
                assertThat(e.awsErrorDetails().errorCode()).isEqualTo("Error400");
            });
        assertThat(http.transmitted).hasSize(1);
    }

    @Test
    void executeAsync_nonSdkFailure_takesTheOneMappingPathWithItsCause() {
        AwsCallEngine engine = engine(runtime(retries(3)).build());
        IllegalStateException boom = new IllegalStateException("boom");
        http.fail(boom);

        assertThat(failure(execute(engine, GET_THING, "a")))
            .isInstanceOf(SdkClientException.class)
            .hasMessageContaining("com.example#GetThing")
            .hasCause(boom);
        assertThat(http.transmitted).hasSize(1);
    }

    @Test
    void executeAsync_idempotencyToken_isInjectedOnceAndStableAcrossRetries() throws Exception {
        AwsCallEngine engine = engine(runtime(retries(3)).build());
        http.respond(503, null).respond(200, "v");

        await(engine.executeAsync(engine.runtime().plan(PUT_THING), PipelineFixtures.request(PUT_INPUT, "a"), null,
                                  null));

        assertThat(http.transmitted).hasSize(2);
        Optional<String> first = http.transmitted.get(0).headers().firstValue("X-Token");
        assertThat(first).isPresent();
        assertThat(http.transmitted.get(1).headers().firstValue("X-Token")).isEqualTo(first);
    }

    @Test
    void executeAsync_overrideHeadersAndQuery_areAppliedToEveryAttempt() throws Exception {
        AwsCallEngine engine = engine(runtime(retries(3)).build());
        http.respond(503, null).respond(200, "v");
        AwsRequestOverrideConfiguration override = AwsRequestOverrideConfiguration.builder()
                                                                                  .putHeader("X-Extra", "1")
                                                                                  .putRawQueryParameter("q", "2")
                                                                                  .build();

        await(engine.executeAsync(engine.runtime().plan(GET_THING), PipelineFixtures.request(GET_INPUT, "a"),
                                  override, null));

        assertThat(http.transmitted).hasSize(2).allSatisfy(request -> {
            assertThat(request.headers().firstValue("X-Extra")).contains("1");
            assertThat(request.encodedQuery()).isEqualTo("q=2");
        });
    }

    @Test
    void executeAsync_noEndpoint_failsBeforeAnyTransmission() {
        AwsCallEngine engine = engine(runtime(retries(3)).configuration(SdkClientConfiguration.empty()).build());

        assertThat(failure(execute(engine, GET_THING, "a")))
            .isInstanceOf(SdkClientException.class)
            .hasMessageContaining("No endpoint is configured");
        assertThat(http.transmitted).isEmpty();
    }

    @Test
    void executeAsync_noAuthOperation_resolvesItsIdentityAndIsSentUnsigned() throws Exception {
        ModeledAuth auth = ModeledAuth.builder().operationId("com.example#Open").build();
        AwsOperation<Struct, Struct> open = PipelineFixtures.operation("com.example#Open", GET_INPUT, GET_OUTPUT,
                                                                       auth);
        AwsCallEngine engine = engine(runtime(retries(1))
                                          .authSchemeSelector(AuthSchemeSelector.create(
                                              List.of(NoAuthAuthScheme.create())))
                                          .build());
        http.respond(200, "v");

        await(execute(engine, open, "a"));

        assertThat(http.transmitted).hasSize(1);
        assertThat(http.transmitted.get(0).headers().contains("Authorization")).isFalse();
    }

    @Test
    void executeAsync_callerCancellation_cancelsTheActiveExchangeAndStartsNoFurtherAttempt() throws Exception {
        RecordingRetryStrategy strategy = new RecordingRetryStrategy(retries(3));
        AwsCallEngine engine = engine(runtime(strategy).build());
        http.hold(503, null).respond(200, "unused");
        CompletableFuture<Struct> future = execute(engine, GET_THING, "a");
        assertThat(http.executed.await(5, TimeUnit.SECONDS)).isTrue();

        assertThat(future.cancel(true)).isTrue();
        http.release();
        settle();

        assertThat(future).isCancelled();
        assertThat(http.cancelled).containsExactly(CancellationReason.CALLER_CANCELLED);
        assertThat(http.transmitted).hasSize(1);
        // The cancellation released the attempt's token once; the cancelled exchange's failure is not classified.
        assertThat(strategy.ledger).containsExactly("acquire#1", "failure#1");
    }

    @Test
    void executeAsync_callerCancellationDuringARetryDelay_cancelsTheDelayAndStartsNoFurtherAttempt() throws Exception {
        RecordingRetryStrategy slow = new RecordingRetryStrategy(
            StandardRetryStrategy.builder().maxAttempts(3)
                                 .backoffStrategy(BackoffStrategy.fixedDelay(Duration.ofMinutes(5)))
                                 .build());
        AwsCallEngine engine = engine(runtime(slow).build());
        http.respond(503, null).respond(200, "unused");
        CompletableFuture<Struct> future = execute(engine, GET_THING, "a");
        assertThat(scheduler.scheduled.await(5, TimeUnit.SECONDS)).isTrue();

        assertThat(future.cancel(true)).isTrue();
        settle();

        assertThat(scheduler.delays).containsExactly(Duration.ofMinutes(5));
        assertThat(scheduler.pending.get()).isCancelled();
        assertThat(http.transmitted).hasSize(1);
        // The granted token of attempt 2 authorized no attempt; the cancellation released it once.
        assertThat(slow.ledger).containsExactly("acquire#1", "retry#1", "failure#2");
    }

    @Test
    void engine_requiresTheTransportAndExecutorsOfItsRuntimesMode() {
        assertThatThrownBy(() -> AwsCallEngine.builder().runtime(runtime(retries(1)).build()).build())
            .isInstanceOf(NullPointerException.class)
            .hasMessageContaining("asyncHttpClient");
        ClientRuntime withoutExecutor = PipelineFixtures.engineRuntime(retries(1), HttpTransportMode.ASYNC)
                                                        .retryScheduler(scheduler)
                                                        .build();
        assertThatThrownBy(() -> AwsCallEngine.builder().runtime(withoutExecutor).asyncHttpClient(http).build())
            .isInstanceOf(NullPointerException.class)
            .hasMessageContaining("completionExecutor");

        AwsCallEngine async = engine(runtime(retries(1)).build());
        assertThatThrownBy(() -> async.executeSync(async.runtime().plan(GET_THING),
                                                   PipelineFixtures.request(GET_INPUT, "a"), null, null))
            .isInstanceOf(IllegalStateException.class);
        // executeAsync on a sync runtime is checked in AwsCallEngineSyncTest.
        assertThat(http.transmitted).isEmpty();
    }

    private static CompletableFuture<Struct> execute(AwsCallEngine engine, AwsOperation<Struct, Struct> operation,
                                                     String name) {
        return engine.executeAsync(engine.runtime().plan(operation), PipelineFixtures.request(GET_INPUT, name), null,
                                   null);
    }

    private AwsCallEngine engine(ClientRuntime runtime) {
        return AwsCallEngine.builder().runtime(runtime).asyncHttpClient(http).build();
    }

    private ClientRuntime.Builder runtime(RetryStrategy retryStrategy) {
        return PipelineFixtures.engineRuntime(retryStrategy, HttpTransportMode.ASYNC, protocol)
                               .completionExecutor(completion)
                               .retryScheduler(scheduler);
    }

    private static <T> T await(CompletableFuture<T> future) throws Exception {
        return future.get(5, TimeUnit.SECONDS);
    }

    private static Throwable failure(CompletableFuture<?> future) {
        try {
            future.get(5, TimeUnit.SECONDS);
        } catch (ExecutionException e) {
            return e.getCause();
        } catch (InterruptedException | TimeoutException e) {
            throw new AssertionError(e);
        }
        return fail("The call succeeded");
    }

    /** Runs every task queued so far on the event loop and the completion executor, a few rounds deep. */
    private void settle() throws Exception {
        for (int i = 0; i < 3; i++) {
            eventLoop.submit(() -> { }).get(5, TimeUnit.SECONDS);
            completion.submit(() -> { }).get(5, TimeUnit.SECONDS);
        }
    }

    private static void isCompletionThread(Thread thread) {
        assertThat(thread.getName()).isEqualTo(COMPLETION_THREAD);
        assertThat(thread).isNotInstanceOf(ForkJoinWorkerThread.class);
    }

    /** A response body publisher of {@code bytes}, delivered on the first request. */
    private static Publisher<ByteBuffer> bodyOf(byte[] bytes) {
        return subscriber -> subscriber.onSubscribe(new Subscription() {
            private boolean done;

            @Override
            public void request(long n) {
                if (done) {
                    return;
                }
                done = true;
                if (bytes.length > 0) {
                    subscriber.onNext(ByteBuffer.wrap(bytes).asReadOnlyBuffer());
                }
                subscriber.onComplete();
            }

            @Override
            public void cancel() {
                done = true;
            }
        });
    }

    /** Records the requested delays; holds each one until it is cancelled. */
    private static final class FakeScheduler implements RetryScheduler {
        private final List<Duration> delays = new CopyOnWriteArrayList<>();
        private final AtomicReference<CompletableFuture<Void>> pending = new AtomicReference<>();
        private final CountDownLatch scheduled = new CountDownLatch(1);

        @Override
        public CompletableFuture<Void> delay(Duration delay) {
            delays.add(delay);
            CompletableFuture<Void> timer = new CompletableFuture<>();
            pending.set(timer);
            scheduled.countDown();
            return timer;
        }
    }

    /**
     * A scripted async transport: one outcome per exchange, signalled on the event-loop thread. A held outcome waits
     * for {@link #release()}; cancelling an exchange signals its failure to the handler.
     */
    private final class FakeAsyncHttp implements SdkAsyncHttpClient {
        private final Queue<Object> outcomes = new ConcurrentLinkedQueue<>();
        private final List<SdkHttpRequest> transmitted = new CopyOnWriteArrayList<>();
        private final List<Thread> executeThreads = new CopyOnWriteArrayList<>();
        private final List<CancellationReason> cancelled = new CopyOnWriteArrayList<>();
        private final CountDownLatch executed = new CountDownLatch(1);
        private volatile Runnable held;

        FakeAsyncHttp respond(int status, String body) {
            outcomes.add(response(status, body));
            return this;
        }

        FakeAsyncHttp hold(int status, String body) {
            outcomes.add(new Held(response(status, body)));
            return this;
        }

        FakeAsyncHttp fail(RuntimeException failure) {
            outcomes.add(failure);
            return this;
        }

        void release() {
            eventLoop.execute(held);
        }

        private SdkHttpResponse response(int status, String body) {
            return SdkHttpResponse.builder()
                                  .statusCode(status)
                                  .body(body == null ? HttpBody.empty() : HttpBody.fromUtf8(body))
                                  .build();
        }

        @Override
        public AsyncHttpExchange execute(SdkHttpRequest request, AsyncHttpResponseHandler handler,
                                         HttpExecutionOptions options) {
            transmitted.add(request);
            executeThreads.add(Thread.currentThread());
            Object outcome = outcomes.remove();
            AsyncExchangeCompletion exchange = new AsyncExchangeCompletion(clientName(), (reason, failure) -> {
                cancelled.add(reason);
                eventLoop.execute(() -> handler.onError(failure));
            });
            boolean hold = outcome instanceof Held;
            Object result = hold ? ((Held) outcome).response() : outcome;
            Runnable deliver = () -> {
                if (exchange.isDone()) {
                    return;
                }
                if (result instanceof RuntimeException failure) {
                    exchange.fail(failure);
                    handler.onError(failure);
                    return;
                }
                SdkHttpResponse response = (SdkHttpResponse) result;
                handler.onHeaders(response.toBuilder().body(HttpBody.empty()).build());
                byte[] body = response.body().asUtf8String().getBytes(StandardCharsets.UTF_8);
                handler.onStream(bodyOf(body));
                exchange.complete();
            };
            if (hold) {
                held = deliver;
            } else {
                eventLoop.execute(deliver);
            }
            executed.countDown();
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

    private record Held(SdkHttpResponse response) {
    }
}
