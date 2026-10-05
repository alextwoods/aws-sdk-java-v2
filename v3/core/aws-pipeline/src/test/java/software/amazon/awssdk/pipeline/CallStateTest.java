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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.core.SdkRequestOverrideConfiguration;
import software.amazon.awssdk.core.context.ContextKey;
import software.amazon.awssdk.http.CancellationReason;
import software.amazon.awssdk.pipeline.PipelineFixtures.CountingProtocol;
import software.amazon.awssdk.pipeline.PipelineFixtures.Struct;
import software.amazon.awssdk.retries.RetryStrategy;
import software.amazon.awssdk.retries.RetryToken;
import software.amazon.awssdk.retries.StandardRetryStrategy;

class CallStateTest {
    private final PipelinePlan<Struct, Struct> plan = PipelineFixtures.runtime().plan(GET_THING);

    private CallState<Struct, Struct> newCall() {
        return plan.newCall(PipelineFixtures.request(GET_INPUT, "a"), plan.runtime().callOptions());
    }

    @Test
    void startAttempt_requiresTheTokenOfExactlyTheNextAttempt() {
        CallState<Struct, Struct> call = newCall();
        RetryStrategy strategy = plan.runtime().retryStrategy();
        RetryToken first = PipelineFixtures.firstToken(plan);

        AttemptState one = call.startAttempt(first);
        assertThatThrownBy(() -> call.startAttempt(first))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("has not completed");
        one.complete();
        // The token of attempt 1 cannot authorize a second attempt.
        assertThatThrownBy(() -> call.startAttempt(first)).isInstanceOf(IllegalArgumentException.class);

        RetryToken second = PipelineFixtures.retryToken(strategy, first);
        assertThatThrownBy(() -> call.startAttempt(first)).isInstanceOf(IllegalStateException.class);
        AttemptState two = call.startAttempt(second);

        assertThat(two.number()).isEqualTo(2);
        assertThat(two.token()).isSameAs(second);
        assertThat(call.attemptsStarted()).isEqualTo(2);
        assertThat(call.retryToken()).containsSame(second);
        assertThat(call.currentAttempt()).containsSame(two);
    }

    @Test
    void replaceRequest_isOnlyAllowedBeforeTheFirstAttempt() {
        CallState<Struct, Struct> call = newCall();
        Struct replacement = PipelineFixtures.request(GET_INPUT, "b");

        call.replaceRequest(replacement);
        assertThat(call.request()).isSameAs(replacement);
        call.startAttempt(PipelineFixtures.firstToken(plan));

        assertThatThrownBy(() -> call.replaceRequest(PipelineFixtures.request(GET_INPUT, "c")))
            .isInstanceOf(IllegalStateException.class);
        assertThat(call.request()).isSameAs(replacement);
    }

    @Test
    void terminalTransition_isOneWayAndSingle() {
        CallState<Struct, Struct> call = newCall();
        AttemptState attempt = call.startAttempt(PipelineFixtures.firstToken(plan));
        Struct output = new Struct(GET_OUTPUT, Map.of("Value", "v"));
        assertThat(call.terminal().status()).isEqualTo(TerminalState.Status.RUNNING);

        assertThat(call.succeed(output)).isTrue();
        assertThat(call.fail(new IllegalStateException())).isFalse();
        assertThat(call.cancel(CancellationReason.CALLER_CANCELLED)).isFalse();
        assertThat(call.succeed(new Struct(GET_OUTPUT, Map.of()))).isFalse();

        assertThat(call.terminal().status()).isEqualTo(TerminalState.Status.SUCCEEDED);
        assertThat(call.terminal().output()).containsSame(output);
        assertThat(call.terminal().failure()).isEmpty();
        assertThat(attempt.isCompleted()).isTrue();
        assertThat(call.cancellation().state()).isEqualTo(CancellationScope.State.CLOSED);
        assertThatThrownBy(() -> call.startAttempt(PipelineFixtures.firstToken(plan)))
            .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void cancel_isTerminalAndCancelsCallAndAttemptWorkOnce() {
        CallState<Struct, Struct> call = newCall();
        AttemptState attempt = call.startAttempt(PipelineFixtures.firstToken(plan));
        List<String> cancelled = new ArrayList<>();
        call.cancellation().register(reason -> cancelled.add("call:" + reason));
        attempt.cancellation().register(reason -> cancelled.add("exchange:" + reason));

        assertThat(call.cancel(CancellationReason.CALLER_CANCELLED)).isTrue();
        assertThat(call.cancel(CancellationReason.CALLER_CANCELLED)).isFalse();
        assertThat(call.fail(new IllegalStateException())).isFalse();

        assertThat(cancelled).containsExactlyInAnyOrder("call:CALLER_CANCELLED", "exchange:CALLER_CANCELLED");
        assertThat(call.terminal().status()).isEqualTo(TerminalState.Status.CANCELLED);
        assertThat(call.terminal().failure()).containsInstanceOf(CancellationException.class);
        assertThat(attempt.cancellation().isCancelled()).isTrue();
        // Late work of the cancelled call is cancelled at once instead of starting.
        AtomicInteger late = new AtomicInteger();
        call.cancellation().register(reason -> late.incrementAndGet());
        assertThat(late).hasValue(1);
        attempt.complete();
        assertThatThrownBy(() -> call.startAttempt(PipelineFixtures.firstToken(plan)))
            .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void racingTerminalTransitions_haveExactlyOneWinner() throws Exception {
        int threads = 6;
        ExecutorService executor = Executors.newFixedThreadPool(threads);
        try {
            for (int round = 0; round < 200; round++) {
                CallState<Struct, Struct> call = newCall();
                CyclicBarrier start = new CyclicBarrier(threads);
                List<Future<Boolean>> results = new ArrayList<>();
                for (int t = 0; t < threads; t++) {
                    int kind = t % 3;
                    results.add(executor.submit(() -> {
                        start.await();
                        return switch (kind) {
                            case 0 -> call.succeed(new Struct(GET_OUTPUT, Map.of()));
                            case 1 -> call.fail(new IllegalStateException());
                            default -> call.cancel(CancellationReason.CALLER_CANCELLED);
                        };
                    }));
                }
                int winners = 0;
                for (Future<Boolean> result : results) {
                    winners += result.get() ? 1 : 0;
                }
                assertThat(winners).isEqualTo(1);
                assertThat(call.terminal().isTerminal()).isTrue();
            }
        } finally {
            executor.shutdownNow();
        }
    }

    @Test
    void concurrentCalls_shareNoMutableState() throws Exception {
        ContextKey<String> tenant = ContextKey.execution("Tenant", String.class);
        ContextKey<String> perAttempt = ContextKey.attempt("PerAttempt", String.class);
        CountingProtocol protocol = new CountingProtocol();
        // One shared strategy (without a retry quota, so every call may retry); each call has its own tokens.
        ClientRuntime runtime = PipelineFixtures.runtimeBuilder(protocol)
                                                .retryStrategy(StandardRetryStrategy.builder()
                                                                                    .maxAttempts(3)
                                                                                    .circuitBreakerEnabled(false)
                                                                                    .build(),
                                                               "concurrent")
                                                .build();
        PipelinePlan<Struct, Struct> shared = runtime.plan(GET_THING);
        int threads = 8;
        int callsPerThread = 100;
        CyclicBarrier start = new CyclicBarrier(threads);
        ExecutorService executor = Executors.newFixedThreadPool(threads);
        try {
            List<Future<Integer>> results = new ArrayList<>();
            for (int t = 0; t < threads; t++) {
                int thread = t;
                results.add(executor.submit(() -> {
                    start.await();
                    int checked = 0;
                    for (int i = 0; i < callsPerThread; i++) {
                        String id = thread + "-" + i;
                        SdkRequestOverrideConfiguration override =
                            SdkRequestOverrideConfiguration.builder().putExecutionAttribute(tenant, id).build();
                        CallState<Struct, Struct> call =
                            shared.newCall(PipelineFixtures.request(GET_INPUT, id), runtime.callOptions(override,
                                                                                                          null));
                        RetryToken token = runtime.retryStrategy().acquireInitialToken(shared.initialTokenRequest())
                                                  .token();
                        for (int attempt = 1; attempt <= 2; attempt++) {
                            AttemptState state = call.startAttempt(token);
                            assertThat(state.context().get(perAttempt)).isNull();
                            state.context().put(perAttempt, id + "#" + attempt);
                            Thread.yield();
                            assertThat(state.context().get(perAttempt)).isEqualTo(id + "#" + attempt);
                            assertThat(state.context().get(tenant)).isEqualTo(id);
                            state.complete();
                            if (attempt == 1) {
                                token = PipelineFixtures.retryToken(runtime.retryStrategy(), token);
                            }
                        }
                        call.retryToken(runtime.retryStrategy().recordSuccess(token));
                        assertThat(call.request().<String>getMemberValue(GET_INPUT.member("Name"))).isEqualTo(id);
                        assertThat(call.succeed(new Struct(GET_OUTPUT, Map.of("Value", id)))).isTrue();
                        checked++;
                    }
                    return checked;
                }));
            }
            int total = 0;
            for (Future<Integer> result : results) {
                total += result.get();
            }
            assertThat(total).isEqualTo(threads * callsPerThread);
        } finally {
            executor.shutdownNow();
        }
        // The shared plan and client state are unchanged by the calls.
        assertThat(runtime.plan(GET_THING)).isSameAs(shared);
        assertThat(protocol.count(GET_THING)).isEqualTo(1);
        assertThat(shared.contextLayout().executionKeys()).isEmpty();
        assertThat(shared.contextLayout().attemptKeys()).containsExactlyElementsOf(PipelineKeys.BUILT_IN);
        assertThat(runtime.callOptions().executionAttributes()).isEmpty();
    }
}
