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
import static software.amazon.awssdk.pipeline.PipelineFixtures.GET_OUTPUT;
import static software.amazon.awssdk.pipeline.PipelineFixtures.GET_THING;
import static software.amazon.awssdk.pipeline.PipelineFixtures.retries;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Queue;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;
import software.amazon.awssdk.auth.api.AuthScheme;
import software.amazon.awssdk.auth.api.AuthSchemeSelector;
import software.amazon.awssdk.auth.api.HttpSigner;
import software.amazon.awssdk.auth.api.ModeledAuth;
import software.amazon.awssdk.auth.api.SignedRequest;
import software.amazon.awssdk.awscore.exception.AwsServiceException;
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
import software.amazon.awssdk.identity.spi.Identity;
import software.amazon.awssdk.identity.spi.IdentityProvider;
import software.amazon.awssdk.identity.spi.IdentityProviders;
import software.amazon.awssdk.identity.spi.ResolveIdentityRequest;
import software.amazon.awssdk.pipeline.PipelineFixtures.HeaderProtocol;
import software.amazon.awssdk.pipeline.PipelineFixtures.ManualExecutor;
import software.amazon.awssdk.pipeline.PipelineFixtures.ManualScheduler;
import software.amazon.awssdk.pipeline.PipelineFixtures.RecordingRetryStrategy;
import software.amazon.awssdk.pipeline.PipelineFixtures.ScriptedHttp;
import software.amazon.awssdk.pipeline.PipelineFixtures.Struct;
import software.amazon.awssdk.retries.BackoffStrategy;
import software.amazon.awssdk.retries.RetryStrategy;
import software.amazon.awssdk.retries.StandardRetryStrategy;

/**
 * Caller cancellation of both drivers (Requirements 8.9, 8.10, 8.11, 10.15, 12.8, 14.10, 14.13): what a successful
 * cancellation cancels, that nothing runs after it, and that cancellation racing a success or a failure yields exactly
 * one outcome, mirrored by the caller future, with every retry token consumed exactly once.
 *
 * <p>The deterministic tests run the async driver on a {@link ManualExecutor}, so every stage runs on the test thread
 * when the test drains the executor, and cancellation happens at an exact point between drains. The race tests use
 * real completion and event-loop threads.
 */
class AwsCallEngineCancellationTest {
    private static final int RACE_ITERATIONS = 300;
    private static final Identity IDENTITY = new Identity() {
    };
    private static final AwsOperation<Struct, Struct> SIGNED_GET = PipelineFixtures.operation(
        "com.example#SignedGet", GET_INPUT, GET_OUTPUT,
        ModeledAuth.builder().operationId("com.example#SignedGet").addServiceScheme(SharedIdentityScheme.ID).build());

    private final ManualExecutor manual = new ManualExecutor();
    private final ManualScheduler scheduler = new ManualScheduler();
    private final HeaderProtocol protocol = new HeaderProtocol();
    private final List<String> trace = new CopyOnWriteArrayList<>();
    private final AtomicReference<CallState<?, ?>> lastCall = new AtomicReference<>();
    private final StageObserver observer = (call, attempt, stage) -> {
        if (stage == PipelineStage.VALIDATE) {
            lastCall.set(call);
        }
        trace.add(stage.name());
    };
    private ExecutorService completion;
    private ExecutorService eventLoop;

    @AfterEach
    void shutDown() {
        if (completion != null) {
            completion.shutdownNow();
            eventLoop.shutdownNow();
        }
    }

    @Test
    void cancelBeforeTheDriverStarts_runsNoStage() {
        RecordingRetryStrategy strategy = new RecordingRetryStrategy(retries(3));
        ControlledHttp http = new ControlledHttp(Runnable::run).respond(200, "unused");
        CompletableFuture<Struct> future = execute(engine(runtime(strategy), http), GET_THING);
        int mark = trace.size();

        assertThat(future.cancel(true)).isTrue();
        manual.runAll();

        assertThat(trace.subList(mark, trace.size())).isEmpty();
        assertThat(strategy.ledger).isEmpty();
        assertThat(http.exchanges).isEmpty();
        assertCancelled(future);
    }

    @Test
    void cancelWhileADrainIsQueued_runsNoFurtherStageOrHookAndDropsTheResponse() {
        RecordingRetryStrategy strategy = new RecordingRetryStrategy(retries(3));
        List<String> hooks = new CopyOnWriteArrayList<>();
        ControlledHttp http = new ControlledHttp(Runnable::run).hold();
        AwsCallEngine engine = engine(runtime(strategy).addInterceptor(new HookLog(hooks)), http);
        CompletableFuture<Struct> future = execute(engine, GET_THING);
        manual.runAll();
        // The response arrives and queues a drain that has not run yet.
        http.exchanges.get(0).respond(200, "v");
        assertThat(manual.tasks).hasSize(1);
        int mark = trace.size();
        int hookMark = hooks.size();

        assertThat(future.cancel(true)).isTrue();
        manual.runAll();

        assertThat(trace.subList(mark, trace.size())).isEmpty();
        assertThat(hooks.subList(hookMark, hooks.size())).isEmpty();
        assertThat(hooks).doesNotContain("afterTransmission", "afterAttempt", "afterExecution");
        assertThat(protocol.codecThreads).isEmpty();
        assertThat(strategy.ledger).containsExactly("acquire#1", "failure#1");
        assertCancelled(future);
    }

    @Test
    void cancelDuringIdentity_stopsWaitingWithoutCancellingTheSharedProviderFuture() throws Exception {
        RecordingRetryStrategy strategy = new RecordingRetryStrategy(retries(3));
        CompletableFuture<Identity> shared = new CompletableFuture<>();
        ControlledHttp http = new ControlledHttp(Runnable::run).respond(200, "other");
        AwsCallEngine engine = engine(signedRuntime(strategy, shared), http);
        CompletableFuture<Struct> cancelled = execute(engine, SIGNED_GET);
        manual.runAll();
        assertThat(trace).endsWith(PipelineStage.RESOLVE_IDENTITY.name());
        int mark = trace.size();

        assertThat(cancelled.cancel(true)).isTrue();
        manual.runAll();

        assertThat(shared).isNotDone();
        assertThat(trace.subList(mark, trace.size())).isEmpty();
        assertThat(strategy.ledger).containsExactly("acquire#1", "failure#1");
        assertCancelled(cancelled);

        // Another call waiting on the same provider future still completes once it resolves.
        CompletableFuture<Struct> other = execute(engine, SIGNED_GET);
        manual.runAll();
        shared.complete(IDENTITY);
        manual.runAll();

        assertThat(other.get(0, TimeUnit.SECONDS).<String>getMemberValue(GET_OUTPUT.member("Value")))
            .isEqualTo("other");
        assertThat(http.exchanges).hasSize(1);
        assertThat(lastCall.get().terminal().status()).isEqualTo(TerminalState.Status.SUCCEEDED);
    }

    @Test
    void cancelDuringTransmission_cancelsTheExchangeOnceAndStartsNoFurtherAttempt() {
        RecordingRetryStrategy strategy = new RecordingRetryStrategy(retries(3));
        ControlledHttp http = new ControlledHttp(Runnable::run).hold().respond(200, "unused");
        CompletableFuture<Struct> future = execute(engine(runtime(strategy), http), GET_THING);
        manual.runAll();
        int mark = trace.size();

        assertThat(future.cancel(true)).isTrue();
        assertThat(future.cancel(true)).isFalse();
        manual.runAll();

        assertThat(http.exchanges).hasSize(1);
        assertThat(http.exchanges.get(0).cancelled).containsExactly(CancellationReason.CALLER_CANCELLED);
        assertThat(trace.subList(mark, trace.size())).isEmpty();
        assertThat(strategy.ledger).containsExactly("acquire#1", "failure#1");
        assertCancelled(future);
    }

    @Test
    void cancelDuringBodyConsumption_cancelsTheSubscriptionAndExchangeOnce() {
        RecordingRetryStrategy strategy = new RecordingRetryStrategy(retries(3));
        ControlledHttp http = new ControlledHttp(Runnable::run).hold();
        CompletableFuture<Struct> future = execute(engine(runtime(strategy), http), GET_THING);
        manual.runAll();
        HeldBody body = http.exchanges.get(0).streamHeld(200, false);
        assertThat(body.requests.get()).isEqualTo(1);
        int mark = trace.size();

        assertThat(future.cancel(true)).isTrue();
        manual.runAll();

        assertThat(body.cancels.get()).isEqualTo(1);
        assertThat(http.exchanges.get(0).cancelled).containsExactly(CancellationReason.CALLER_CANCELLED);
        assertThat(trace.subList(mark, trace.size())).isEmpty();
        assertThat(protocol.codecThreads).isEmpty();
        assertThat(strategy.ledger).containsExactly("acquire#1", "failure#1");
        assertCancelled(future);
    }

    @Test
    void cancelDuringARetryDelay_cancelsTheTimerOnceAndReleasesTheGrantedToken() {
        RecordingRetryStrategy strategy = new RecordingRetryStrategy(
            StandardRetryStrategy.builder().maxAttempts(3)
                                 .backoffStrategy(BackoffStrategy.fixedDelay(Duration.ofMinutes(5)))
                                 .build());
        ControlledHttp http = new ControlledHttp(Runnable::run).respond(503, null).respond(200, "unused");
        CompletableFuture<Struct> future = execute(engine(runtime(strategy), http), GET_THING);
        manual.runAll();
        assertThat(scheduler.timers).hasSize(1);
        int mark = trace.size();

        assertThat(future.cancel(true)).isTrue();
        manual.runAll();

        assertThat(scheduler.timers.get(0).cancels.get()).isEqualTo(1);
        assertThat(http.exchanges).hasSize(1);
        assertThat(trace.subList(mark, trace.size())).isEmpty();
        // The granted token of attempt 2 authorized no attempt; the cancellation released it once.
        assertThat(strategy.ledger).containsExactly("acquire#1", "retry#1", "failure#2");
        assertCancelled(future);
    }

    @Test
    void aThrowingCancellationHandle_doesNotEscapeCancelAndTheOtherHandlesStillRun() {
        RecordingRetryStrategy strategy = new RecordingRetryStrategy(retries(3));
        ControlledHttp http = new ControlledHttp(Runnable::run).hold();
        CompletableFuture<Struct> future = execute(engine(runtime(strategy), http), GET_THING);
        manual.runAll();
        // The subscription's handle was registered last, so it runs first, and throws.
        HeldBody body = http.exchanges.get(0).streamHeld(200, true);

        assertThat(future.cancel(true)).isTrue();
        manual.runAll();

        assertThat(body.cancels.get()).isEqualTo(1);
        assertThat(http.exchanges.get(0).cancelled).containsExactly(CancellationReason.CALLER_CANCELLED);
        Throwable cancellation = assertCancelled(future);
        assertThat(cancellation.getSuppressed()).singleElement()
                                                .satisfies(e -> assertThat(e).hasMessage("subscription cancel"));
        assertThat(strategy.ledger).containsExactly("acquire#1", "failure#1");
    }

    @Test
    void cancelAfterCompletion_returnsFalseAndChangesNothing() throws Exception {
        RecordingRetryStrategy strategy = new RecordingRetryStrategy(retries(1));
        ControlledHttp http = new ControlledHttp(Runnable::run).respond(200, "v").respond(400, null);
        AwsCallEngine engine = engine(runtime(strategy), http);

        CompletableFuture<Struct> succeeded = execute(engine, GET_THING);
        manual.runAll();
        CallState<?, ?> succeededCall = lastCall.get();
        Struct output = succeeded.get(0, TimeUnit.SECONDS);
        CompletableFuture<Struct> failed = execute(engine, GET_THING);
        manual.runAll();
        CallState<?, ?> failedCall = lastCall.get();
        Throwable failure = failure(failed);
        List<String> ledger = List.copyOf(strategy.ledger);

        assertThat(succeeded.cancel(true)).isFalse();
        assertThat(failed.cancel(true)).isFalse();
        manual.runAll();

        assertThat(succeeded.get(0, TimeUnit.SECONDS)).isSameAs(output);
        assertThat(succeededCall.terminal().status()).isEqualTo(TerminalState.Status.SUCCEEDED);
        assertThat(failure(failed)).isSameAs(failure).isInstanceOf(AwsServiceException.class);
        assertThat(failedCall.terminal().status()).isEqualTo(TerminalState.Status.FAILED);
        assertThat(strategy.ledger).isEqualTo(ledger)
                                   .containsExactly("acquire#1", "success#1", "acquire#1", "stop#1");
        assertThat(http.exchanges).allSatisfy(exchange -> assertThat(exchange.cancelled).isEmpty());
        assertThat(manual.tasks).isEmpty();
    }

    @Test
    void cancelRacingSuccess_hasExactlyOneOutcomeMirroredByTheFuture() throws Exception {
        race(retries(1), http -> http.respond(200, "v"), (call, future) -> {
            assertThat(call.terminal().status()).isEqualTo(TerminalState.Status.SUCCEEDED);
            assertThat(future.get(0, TimeUnit.SECONDS)).isSameAs(call.terminal().output().orElseThrow());
        });
    }

    @Test
    void cancelRacingRetriesAndFailure_hasExactlyOneOutcomeMirroredByTheFuture() throws Exception {
        // Without the circuit breaker, so every iteration retries once rather than draining a shared quota.
        RetryStrategy twoAttempts = StandardRetryStrategy.builder()
                                                         .maxAttempts(2)
                                                         .backoffStrategy(BackoffStrategy.none())
                                                         .throttlingBackoffStrategy(BackoffStrategy.none())
                                                         .circuitBreakerEnabled(false)
                                                         .build();
        race(twoAttempts, http -> http.respond(503, null).respond(503, null), (call, future) -> {
            assertThat(call.terminal().status()).isEqualTo(TerminalState.Status.FAILED);
            assertThat(failure(future)).isSameAs(call.terminal().failure().orElseThrow())
                                       .isInstanceOf(AwsServiceException.class);
        });
    }

    @Test
    void syncInterruptionWhileWaitingForASharedIdentity_cancelsTheCallButNotTheProviderFuture() {
        RecordingRetryStrategy strategy = new RecordingRetryStrategy(retries(3));
        CompletableFuture<Identity> shared = new CompletableFuture<>();
        ScriptedHttp http = new ScriptedHttp();
        ClientRuntime runtime = PipelineFixtures.engineRuntime(strategy, HttpTransportMode.SYNC, protocol)
                                                .authSchemeSelector(selector(shared))
                                                .build();
        AwsCallEngine engine = AwsCallEngine.builder().runtime(runtime).syncHttpClient(http)
                                            .stageObserver(observer).build();

        Thread.currentThread().interrupt();
        try {
            assertThatThrownBy(() -> engine.executeSync(runtime.plan(SIGNED_GET),
                                                        PipelineFixtures.request(GET_INPUT, "a"), null, null))
                .isInstanceOf(SdkClientException.class)
                .hasCauseInstanceOf(InterruptedException.class);
            assertThat(Thread.currentThread().isInterrupted()).isTrue();
        } finally {
            Thread.interrupted();
        }

        assertThat(shared).isNotDone();
        assertThat(http.transmissions).isZero();
        assertThat(lastCall.get().terminal().status()).isEqualTo(TerminalState.Status.CANCELLED);
        assertThat(strategy.ledger).containsExactly("acquire#1", "failure#1");
    }

    /**
     * Races a caller's cancellation against a scripted call under {@code retryStrategy}, {@link #RACE_ITERATIONS}
     * times, on real completion and event-loop threads; the cancellation lands at a varying point of the call.
     */
    private void race(RetryStrategy retryStrategy, Consumer<ControlledHttp> script, Outcome notCancelled)
        throws Exception {
        completion = Executors.newSingleThreadExecutor(r -> new Thread(r, "sdk-completion"));
        eventLoop = Executors.newSingleThreadExecutor(r -> new Thread(r, "fake-event-loop"));
        RecordingRetryStrategy strategy = new RecordingRetryStrategy(retryStrategy);
        ControlledHttp http = new ControlledHttp(eventLoop);
        AtomicInteger afterExecution = new AtomicInteger();
        ClientRuntime runtime = PipelineFixtures.engineRuntime(strategy, HttpTransportMode.ASYNC, protocol)
                                                .completionExecutor(completion)
                                                .retryScheduler(scheduler)
                                                .addInterceptor(new ExecutionInterceptor() {
                                                    @Override
                                                    public void afterExecution(HookContext.AfterExecution context) {
                                                        afterExecution.incrementAndGet();
                                                    }
                                                })
                                                .build();
        AtomicInteger stagesEntered = new AtomicInteger();
        AwsCallEngine engine = AwsCallEngine.builder().runtime(runtime).asyncHttpClient(http)
                                            .stageObserver((call, attempt, stage) -> {
                                                if (stage == PipelineStage.VALIDATE) {
                                                    lastCall.set(call);
                                                }
                                                stagesEntered.incrementAndGet();
                                            })
                                            .build();
        PipelinePlan<Struct, Struct> plan = runtime.plan(GET_THING);
        // An uncancelled call measures how many stage points the whole call enters.
        script.accept(http);
        CompletableFuture<Struct> warmUp = engine.executeAsync(plan, PipelineFixtures.request(GET_INPUT, "a"),
                                                               null, null);
        warmUp.handle((value, error) -> null).get(5, TimeUnit.SECONDS);
        settle();
        int callStages = stagesEntered.get();
        Random random = new Random(42);
        int cancelledCount = 0;
        for (int i = 0; i < RACE_ITERATIONS; i++) {
            http.reset();
            strategy.ledger.clear();
            afterExecution.set(0);
            stagesEntered.set(0);
            script.accept(http);
            // The caller cancels once the driver, running concurrently, has entered this many stage points; past
            // the last one it cancels a completed call.
            int cancelAt = random.nextInt(callStages + 2);
            CompletableFuture<Struct> future = engine.executeAsync(plan, PipelineFixtures.request(GET_INPUT, "a"),
                                                                   null, null);
            CallState<?, ?> call = lastCall.get();
            while (stagesEntered.get() < cancelAt && !future.isDone()) {
                Thread.onSpinWait();
            }

            boolean cancelled = future.cancel(true);
            future.handle((value, error) -> null).get(5, TimeUnit.SECONDS);
            settle();

            String iteration = "iteration " + i + ", ledger " + strategy.ledger;
            assertThat(future.isCancelled()).as(iteration).isEqualTo(cancelled);
            if (cancelled) {
                cancelledCount++;
                assertThat(call.terminal().status()).as(iteration).isEqualTo(TerminalState.Status.CANCELLED);
                assertThat(assertCancelled(future)).as(iteration).isSameAs(call.terminal().failure().orElseThrow());
            } else {
                notCancelled.check(call, future);
                assertThat(afterExecution).as(iteration).hasValue(1);
            }
            assertThat(afterExecution.get()).as(iteration).isLessThanOrEqualTo(1);
            int granted = assertEachTokenConsumedOnce(strategy.ledger, iteration);
            assertThat(http.exchanges.size()).as(iteration).isLessThanOrEqualTo(granted);
            assertThat(http.exchanges).as(iteration)
                                      .allSatisfy(exchange -> assertThat(exchange.cancelled.size()).isLessThan(2));
            assertThat(call.currentAttempt()).as(iteration).isEmpty();
        }
        // Both outcomes occur, so the race is exercised from both sides.
        assertThat(cancelledCount).isBetween(1, RACE_ITERATIONS - 1);
    }

    /**
     * Asserts that every token the ledger shows granted ({@code acquire#n}, or {@code retry#n} granting {@code n+1})
     * was consumed exactly once ({@code retry}, {@code stop}, {@code success} or {@code failure}).
     *
     * @return the number of tokens granted
     */
    private static int assertEachTokenConsumedOnce(List<String> ledger, String description) {
        Set<Integer> granted = new HashSet<>();
        Map<Integer, Integer> consumed = new HashMap<>();
        for (String entry : ledger) {
            String[] parts = entry.split("#");
            int attempt = Integer.parseInt(parts[1]);
            if (parts[0].equals("acquire")) {
                granted.add(attempt);
                continue;
            }
            consumed.merge(attempt, 1, Integer::sum);
            if (parts[0].equals("retry")) {
                granted.add(attempt + 1);
            }
        }
        assertThat(consumed.keySet()).as(description).isEqualTo(granted);
        assertThat(consumed.values()).as(description).allSatisfy(count -> assertThat(count).isEqualTo(1));
        return granted.size();
    }

    /** Asserts the future is cancelled with the call's own cancellation, and returns it. */
    private Throwable assertCancelled(CompletableFuture<?> future) {
        assertThat(future).isCancelled();
        // The stored exception itself (join() would wrap it in a new CancellationException).
        Throwable cancellation = future.handle((value, error) -> error).join();
        assertThat(cancellation).isInstanceOf(CancellationException.class);
        CallState<?, ?> call = lastCall.get();
        assertThat(call.terminal().status()).isEqualTo(TerminalState.Status.CANCELLED);
        assertThat(cancellation).isSameAs(call.terminal().failure().orElseThrow());
        assertThat(call.cancellation().isCancelled()).isTrue();
        return cancellation;
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

    private void settle() throws Exception {
        for (int i = 0; i < 5; i++) {
            eventLoop.submit(() -> { }).get(5, TimeUnit.SECONDS);
            completion.submit(() -> { }).get(5, TimeUnit.SECONDS);
        }
    }

    private CompletableFuture<Struct> execute(AwsCallEngine engine, AwsOperation<Struct, Struct> operation) {
        return engine.executeAsync(engine.runtime().plan(operation), PipelineFixtures.request(GET_INPUT, "a"),
                                   null, null);
    }

    private AwsCallEngine engine(ClientRuntime.Builder runtime, ControlledHttp http) {
        return AwsCallEngine.builder().runtime(runtime.build()).asyncHttpClient(http).stageObserver(observer).build();
    }

    private ClientRuntime.Builder runtime(RetryStrategy strategy) {
        return PipelineFixtures.engineRuntime(strategy, HttpTransportMode.ASYNC, protocol)
                               .completionExecutor(manual)
                               .retryScheduler(scheduler);
    }

    private ClientRuntime.Builder signedRuntime(RetryStrategy strategy, CompletableFuture<Identity> shared) {
        return runtime(strategy).authSchemeSelector(selector(shared));
    }

    private static AuthSchemeSelector selector(CompletableFuture<Identity> shared) {
        return AuthSchemeSelector.create(List.of(new SharedIdentityScheme(shared)));
    }

    @FunctionalInterface
    private interface Outcome {
        void check(CallState<?, ?> call, CompletableFuture<Struct> future) throws Exception;
    }

    /**
     * An async transport scripted per exchange: a whole response, delivered through {@code deliveries}, or a held
     * exchange the test drives. A delivery signals the handler only if it terminates the exchange first, and a winning
     * cancellation records its reason and signals the failure, so each exchange signals exactly once.
     */
    private static final class ControlledHttp implements SdkAsyncHttpClient {
        private static final Object HOLD = new Object();
        private final Executor deliveries;
        private final Queue<Object> script = new ConcurrentLinkedQueue<>();
        private final List<ControlledExchange> exchanges = new CopyOnWriteArrayList<>();

        ControlledHttp(Executor deliveries) {
            this.deliveries = deliveries;
        }

        ControlledHttp respond(int status, String body) {
            script.add(response(status, body));
            return this;
        }

        ControlledHttp hold() {
            script.add(HOLD);
            return this;
        }

        void reset() {
            script.clear();
            exchanges.clear();
        }

        @Override
        public AsyncHttpExchange execute(SdkHttpRequest request, AsyncHttpResponseHandler handler,
                                         HttpExecutionOptions options) {
            ControlledExchange exchange = new ControlledExchange(handler, deliveries);
            exchanges.add(exchange);
            Object outcome = script.remove();
            if (outcome != HOLD) {
                exchange.respond((SdkHttpResponse) outcome);
            }
            return exchange.completion;
        }

        @Override
        public HttpClientCapabilities capabilities() {
            throw new UnsupportedOperationException();
        }

        @Override
        public String clientName() {
            return "ControlledHttp";
        }

        @Override
        public void close() {
        }
    }

    private static SdkHttpResponse response(int status, String body) {
        return SdkHttpResponse.builder()
                              .statusCode(status)
                              .body(body == null ? HttpBody.empty() : HttpBody.fromUtf8(body))
                              .build();
    }

    private static final class ControlledExchange {
        private final AsyncHttpResponseHandler handler;
        private final Executor deliveries;
        private final List<CancellationReason> cancelled = new CopyOnWriteArrayList<>();
        private final AsyncExchangeCompletion completion;

        private ControlledExchange(AsyncHttpResponseHandler handler, Executor deliveries) {
            this.handler = handler;
            this.deliveries = deliveries;
            this.completion = new AsyncExchangeCompletion("ControlledHttp", (reason, failure) -> {
                cancelled.add(reason);
                deliveries.execute(() -> handler.onError(failure));
            });
        }

        void respond(int status, String body) {
            respond(response(status, body));
        }

        private void respond(SdkHttpResponse response) {
            deliveries.execute(() -> {
                if (!completion.complete()) {
                    return;
                }
                handler.onHeaders(response.toBuilder().body(HttpBody.empty()).build());
                byte[] bytes = response.body().asUtf8String().getBytes(StandardCharsets.UTF_8);
                handler.onStream(subscriber -> subscriber.onSubscribe(new Subscription() {
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
                }));
            });
        }

        /** Signals headers and a body that delivers one chunk and then holds; the exchange stays active. */
        HeldBody streamHeld(int status, boolean cancelThrows) {
            HeldBody body = new HeldBody(cancelThrows);
            handler.onHeaders(SdkHttpResponse.builder().statusCode(status).body(HttpBody.empty()).build());
            handler.onStream(body);
            return body;
        }
    }

    /** A response body that delivers one chunk on its first request and never completes. */
    private static final class HeldBody implements Publisher<ByteBuffer> {
        private final boolean cancelThrows;
        private final AtomicInteger requests = new AtomicInteger();
        private final AtomicInteger cancels = new AtomicInteger();

        private HeldBody(boolean cancelThrows) {
            this.cancelThrows = cancelThrows;
        }

        @Override
        public void subscribe(Subscriber<? super ByteBuffer> subscriber) {
            subscriber.onSubscribe(new Subscription() {
                @Override
                public void request(long n) {
                    if (requests.getAndIncrement() == 0) {
                        subscriber.onNext(ByteBuffer.wrap("partial".getBytes(StandardCharsets.UTF_8)));
                    }
                }

                @Override
                public void cancel() {
                    cancels.incrementAndGet();
                    if (cancelThrows) {
                        throw new IllegalStateException("subscription cancel");
                    }
                }
            });
        }
    }

    /** Logs the attempt and terminal hooks it observes. */
    private record HookLog(List<String> hooks) implements ExecutionInterceptor {
        @Override
        public void beforeAttempt(HookContext.BeforeAttempt context) {
            hooks.add("beforeAttempt");
        }

        @Override
        public void beforeTransmission(HookContext.BeforeTransmission context) {
            hooks.add("beforeTransmission");
        }

        @Override
        public void afterTransmission(HookContext.AfterTransmission context) {
            hooks.add("afterTransmission");
        }

        @Override
        public void afterAttempt(HookContext.AfterAttempt context) {
            hooks.add("afterAttempt");
        }

        @Override
        public void afterExecution(HookContext.AfterExecution context) {
            hooks.add("afterExecution");
        }
    }

    /** A scheme whose provider hands every call the same, possibly shared, identity future; signs nothing. */
    private static final class SharedIdentityScheme implements AuthScheme<Identity> {
        static final String ID = "test#shared";
        private final CompletableFuture<Identity> shared;

        SharedIdentityScheme(CompletableFuture<Identity> shared) {
            this.shared = shared;
        }

        @Override
        public String schemeId() {
            return ID;
        }

        @Override
        public Class<Identity> identityType() {
            return Identity.class;
        }

        @Override
        public Optional<IdentityProvider<? extends Identity>> identityProvider(IdentityProviders providers) {
            return Optional.of(new IdentityProvider<>() {
                @Override
                public Class<Identity> identityType() {
                    return Identity.class;
                }

                @Override
                public CompletableFuture<Identity> resolveIdentity(ResolveIdentityRequest request) {
                    return shared;
                }
            });
        }

        @Override
        public HttpSigner<Identity> signer() {
            return request -> SignedRequest.unsigned(request.request());
        }
    }
}
