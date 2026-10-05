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
import static software.amazon.awssdk.pipeline.PipelineFixtures.GET_INPUT;
import static software.amazon.awssdk.pipeline.PipelineFixtures.GET_THING;

import java.io.IOException;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import net.jqwik.api.Arbitraries;
import net.jqwik.api.Arbitrary;
import net.jqwik.api.Combinators;
import net.jqwik.api.EdgeCasesMode;
import net.jqwik.api.ForAll;
import net.jqwik.api.Label;
import net.jqwik.api.Property;
import net.jqwik.api.Provide;
import net.jqwik.api.Tuple;
import net.jqwik.api.statistics.Statistics;
import software.amazon.awssdk.auth.api.SignedRequest;
import software.amazon.awssdk.http.HttpBody;
import software.amazon.awssdk.http.HttpFailureCategory;
import software.amazon.awssdk.http.HttpPhase;
import software.amazon.awssdk.http.HttpTransportException;
import software.amazon.awssdk.http.SdkHttpRequest;
import software.amazon.awssdk.http.SdkHttpResponse;
import software.amazon.awssdk.pipeline.PipelineFixtures.Driver;
import software.amazon.awssdk.pipeline.PipelineFixtures.ScriptedHttp;
import software.amazon.awssdk.pipeline.PipelineFixtures.Struct;
import software.amazon.smithy.java.core.schema.SerializableStruct;
import software.amazon.smithy.model.shapes.ShapeId;

/**
 * Property 36: Interceptor invocation counts and order (Requirements 11.3, 11.9, 11.10).
 *
 * <p>For any ordered set of interceptors, each overriding any subset of the 16 hooks, and any per-attempt outcome
 * script, the hooks a call dispatches are exactly those the design's hook table ("Interceptors and plugins", scope and
 * frequency) predicts: each execution hook once per call on its path, each attempt hook once per attempt on that
 * attempt's path, and, within one hook invocation, the overriding interceptors in registration order with the
 * plugin-contributed one first. Each try runs one call through {@link AwsCallEngine} and compares its log of
 * (interceptor, hook, attempt) entries with {@link ScopeOracle}, a list model of the hook table written here, then
 * checks every (interceptor, hook) count against the scope rule alone.
 *
 * <p>A scenario picks the sync or async driver (the async one runs inline: an inline completion executor, immediate
 * retry delays and a transport that answers inline, so each try is deterministic on the test thread); 1-4 interceptors
 * with generated hook subsets, the first optionally contributed by a plugin; and maxAttempts 1-3 with one scripted
 * outcome per attempt: success, a retryable 503, a non-retryable 400 or a retryable connection reset.
 *
 * <p>Interpretation: dispatch selects interceptors by the hooks their class overrides, so an interceptor overriding
 * an arbitrary generated subset is registered as consecutive single-hook recorders ({@link
 * PipelineFixtures#hookRecorder}), which dispatch exactly as one object overriding that subset would; an interceptor
 * overriding every hook is one {@link AllHooks} object, so multi-hook objects are exercised too. The attempt of an
 * entry is the attempt of the stage point the engine reported just before the hook ran (0 for an execution stage).
 */
class InterceptorOrderPropertyTest {
    /** The execution-scoped hooks: once per call, on their path. Every other hook is attempt-scoped. */
    private static final Set<PipelineStage> EXECUTION_HOOKS = EnumSet.of(
        PipelineStage.BEFORE_EXECUTION, PipelineStage.MODIFY_REQUEST, PipelineStage.MODIFY_RESPONSE,
        PipelineStage.MODIFY_EXCEPTION, PipelineStage.AFTER_EXECUTION);
    /** The attempt hooks every attempt runs up to and including transmission. */
    private static final List<PipelineStage> REQUEST_HOOKS = List.of(
        PipelineStage.BEFORE_ATTEMPT, PipelineStage.AFTER_SERIALIZATION, PipelineStage.MODIFY_BEFORE_SIGNING,
        PipelineStage.MODIFY_AFTER_SIGNING, PipelineStage.BEFORE_TRANSMISSION, PipelineStage.AFTER_TRANSMISSION);
    /** The attempt hooks of an attempt that received a response. */
    private static final List<PipelineStage> RESPONSE_HOOKS = List.of(
        PipelineStage.MODIFY_HTTP_RESPONSE, PipelineStage.MODIFY_RESPONSE_BODY, PipelineStage.BEFORE_DESERIALIZATION);

    /** The scripted outcome of one attempt. */
    enum Outcome {
        SUCCESS,
        /** HTTP 503: retryable. */
        SERVICE_UNAVAILABLE,
        /** HTTP 400: not retryable. */
        BAD_REQUEST,
        /** A connection reset: retryable; no response is received. */
        CONNECTION_RESET
    }

    /**
     * One generated call.
     *
     * @param interceptors the hooks interceptor {@code i} overrides, in dispatch order
     * @param plugin whether interceptor 0 is contributed by a plugin rather than registered on the client
     * @param outcomes the outcome of attempt {@code n} at index {@code n - 1}; one per allowed attempt
     */
    record Scenario(Driver driver, List<Set<PipelineStage>> interceptors, boolean plugin, List<Outcome> outcomes) {
    }

    /** One hook invocation: which interceptor, which hook, and its attempt (0 for an execution hook). */
    record Entry(int interceptor, PipelineStage hook, int attempt) {
        @Override
        public String toString() {
            return interceptor + ":" + hook + (attempt == 0 ? "" : "#" + attempt);
        }
    }

    /** What the oracle predicts: the exact log, the attempts made, and whether the call succeeded. */
    record Prediction(List<Entry> log, int attempts, boolean succeeded) {
    }

    // Feature: aws-sdk-java-v3, Property 36: Interceptor invocation counts and order
    // Embedded edge cases are disabled; the coverage checks below fail the property if retries, failures, plugins,
    // multi-hook objects or any attempt outcome stop being generated.
    @Property(tries = 400, edgeCases = EdgeCasesMode.NONE)
    @Label("Property 36: Interceptor invocation counts and order")
    void hooksRunOncePerScopeInRegistrationOrder(@ForAll("scenarios") Scenario scenario) {
        Prediction predicted = ScopeOracle.predict(scenario);
        Call call = new Call(scenario);
        boolean succeeded = call.run();

        String description = scenario + "\n  log: " + call.log;
        assertThat(succeeded).as("outcome of " + description).isEqualTo(predicted.succeeded());
        assertThat(call.log).as(description).containsExactlyElementsOf(predicted.log());
        assertThat(call.log).as("each hook once per scope: " + description).doesNotHaveDuplicates();
        assertCounts(call.log, scenario, description);

        Statistics.label("driver").collect(scenario.driver());
        Statistics.label("succeeded").collect(predicted.succeeded());
        Statistics.label("succeeded").coverage(c -> {
            c.check(true).percentage(p -> p >= 20);
            c.check(false).percentage(p -> p >= 20);
        });
        Statistics.label("attempts").collect(predicted.attempts());
        Statistics.label("retried").collect(predicted.attempts() > 1);
        Statistics.label("retried").coverage(c -> c.check(true).percentage(p -> p >= 15));
        Statistics.label("interceptors").collect(scenario.interceptors().size());
        Statistics.label("interceptors").coverage(c -> c.check(4).percentage(p -> p >= 10));
        Statistics.label("plugin").collect(scenario.plugin());
        Statistics.label("plugin").coverage(c -> c.check(true).percentage(p -> p >= 25));
        boolean allHooks = scenario.interceptors().contains(PipelineArbitraries.HOOK_STAGES);
        Statistics.label("an interceptor overrides every hook").collect(allHooks);
        Statistics.label("an interceptor overrides every hook").coverage(c -> c.check(true).percentage(p -> p >= 5));
        for (Outcome outcome : Outcome.values()) {
            String label = "an attempt ended " + outcome;
            Statistics.label(label).collect(scenario.outcomes().subList(0, predicted.attempts()).contains(outcome));
            Statistics.label(label).coverage(c -> c.check(true).percentage(p -> p >= 10));
        }
    }

    @Provide
    Arbitrary<Scenario> scenarios() {
        Arbitrary<Outcome> outcomes = Arbitraries.frequency(
            Tuple.of(3, Outcome.SUCCESS), Tuple.of(3, Outcome.SERVICE_UNAVAILABLE), Tuple.of(1, Outcome.BAD_REQUEST),
            Tuple.of(2, Outcome.CONNECTION_RESET));
        return Combinators.combine(PipelineArbitraries.drivers(),
                                   PipelineArbitraries.hookSubsets().list().ofMinSize(1).ofMaxSize(4),
                                   Arbitraries.of(true, false),
                                   outcomes.list().ofMinSize(1).ofMaxSize(3))
                          .as(Scenario::new);
    }

    /**
     * The scope rule alone, without the walk: per interceptor and hook, an overridden execution hook runs once per call
     * (modifyResponse only on success, modifyException only on failure), an overridden attempt hook once per attempt on
     * whose path it lies, and a hook the interceptor does not override never.
     */
    private static void assertCounts(List<Entry> log, Scenario scenario, String description) {
        int attempts = 0;
        int responses = 0;
        int failed = 0;
        boolean succeeded = false;
        for (Outcome outcome : scenario.outcomes()) {
            attempts++;
            responses += outcome == Outcome.CONNECTION_RESET ? 0 : 1;
            if (outcome == Outcome.SUCCESS) {
                succeeded = true;
                break;
            }
            failed++;
            if (outcome == Outcome.BAD_REQUEST) {
                break;
            }
        }
        for (int i = 0; i < scenario.interceptors().size(); i++) {
            for (PipelineStage hook : PipelineArbitraries.HOOK_STAGES) {
                int expected = !scenario.interceptors().get(i).contains(hook) ? 0 : switch (hook) {
                    case BEFORE_EXECUTION, MODIFY_REQUEST, AFTER_EXECUTION -> 1;
                    case MODIFY_RESPONSE -> succeeded ? 1 : 0;
                    case MODIFY_EXCEPTION -> succeeded ? 0 : 1;
                    case MODIFY_HTTP_RESPONSE, MODIFY_RESPONSE_BODY, BEFORE_DESERIALIZATION -> responses;
                    case MODIFY_ATTEMPT_EXCEPTION -> failed;
                    default -> attempts;
                };
                int interceptor = i;
                List<Integer> seen = log.stream().filter(e -> e.interceptor() == interceptor && e.hook() == hook)
                                        .map(Entry::attempt).toList();
                String at = "interceptor " + i + " at " + hook + " of " + description;
                assertThat(seen).as("count of " + at).hasSize(expected);
                boolean attemptScoped = !EXECUTION_HOOKS.contains(hook);
                assertThat(seen).as("scope of " + at).allMatch(a -> attemptScoped ? a >= 1 : a == 0);
            }
        }
    }

    /**
     * The hook table as a list model: the hook path of a call, each hook expanded into the interceptors that override
     * it, in dispatch order.
     */
    static final class ScopeOracle {
        private ScopeOracle() {
        }

        static Prediction predict(Scenario scenario) {
            List<Entry> log = new ArrayList<>();
            fire(log, scenario, 0, PipelineStage.BEFORE_EXECUTION, PipelineStage.MODIFY_REQUEST);
            int maxAttempts = scenario.outcomes().size();
            for (int attempt = 1; ; attempt++) {
                Outcome outcome = scenario.outcomes().get(attempt - 1);
                fire(log, scenario, attempt, REQUEST_HOOKS.toArray(PipelineStage[]::new));
                if (outcome != Outcome.CONNECTION_RESET) {
                    fire(log, scenario, attempt, RESPONSE_HOOKS.toArray(PipelineStage[]::new));
                }
                if (outcome == Outcome.SUCCESS) {
                    fire(log, scenario, attempt, PipelineStage.AFTER_ATTEMPT);
                    fire(log, scenario, 0, PipelineStage.MODIFY_RESPONSE, PipelineStage.AFTER_EXECUTION);
                    return new Prediction(log, attempt, true);
                }
                fire(log, scenario, attempt, PipelineStage.MODIFY_ATTEMPT_EXCEPTION, PipelineStage.AFTER_ATTEMPT);
                if (outcome == Outcome.BAD_REQUEST || attempt == maxAttempts) {
                    fire(log, scenario, 0, PipelineStage.MODIFY_EXCEPTION, PipelineStage.AFTER_EXECUTION);
                    return new Prediction(log, attempt, false);
                }
            }
        }

        /** Each of {@code hooks} in turn, each run by every interceptor overriding it, in dispatch order. */
        private static void fire(List<Entry> log, Scenario scenario, int attempt, PipelineStage... hooks) {
            for (PipelineStage hook : hooks) {
                for (int i = 0; i < scenario.interceptors().size(); i++) {
                    if (scenario.interceptors().get(i).contains(hook)) {
                        log.add(new Entry(i, hook, attempt));
                    }
                }
            }
        }
    }

    /** One call of a scenario: its engine, its scripted transport, and the log its interceptors write. */
    private static final class Call {
        final List<Entry> log = new CopyOnWriteArrayList<>();
        private final Scenario scenario;
        private volatile int attempt;

        Call(Scenario scenario) {
            this.scenario = scenario;
        }

        /** Runs the call; whether it succeeded. */
        boolean run() {
            Driver driver = scenario.driver();
            ClientRuntime.Builder runtime =
                PipelineFixtures.engineRuntime(PipelineFixtures.retries(scenario.outcomes().size()), driver.mode);
            if (driver == Driver.ASYNC) {
                runtime.completionExecutor(Runnable::run)
                       .retryScheduler(delay -> CompletableFuture.completedFuture(null));
            }
            // The client's interceptors are registered before the plugin, so plugin-first is the engine's ordering.
            int first = scenario.plugin() ? 1 : 0;
            for (int i = first; i < scenario.interceptors().size(); i++) {
                interceptor(i).forEach(runtime::addInterceptor);
            }
            if (scenario.plugin()) {
                runtime.addPlugin(new ContributingPlugin(interceptor(0)));
            }
            ScriptedHttp http = new ScriptedHttp();
            for (Outcome outcome : scenario.outcomes()) {
                switch (outcome) {
                    case SUCCESS -> http.respond(200, "v");
                    case SERVICE_UNAVAILABLE -> http.respond(503, null);
                    case BAD_REQUEST -> http.respond(400, null);
                    case CONNECTION_RESET -> http.fail(HttpTransportException.create(
                        HttpPhase.CONNECT, HttpFailureCategory.Kind.IO, "Connection reset", new IOException("reset")));
                }
            }
            AwsCallEngine.Builder builder = AwsCallEngine.builder().runtime(runtime.build())
                                                         .stageObserver(this::entered);
            AwsCallEngine engine = (driver == Driver.SYNC ? builder.syncHttpClient(http)
                                                          : builder.asyncHttpClient(http)).build();
            PipelinePlan<Struct, Struct> plan = engine.runtime().plan(GET_THING);
            Struct request = PipelineFixtures.request(GET_INPUT, "a");
            if (driver == Driver.SYNC) {
                try {
                    engine.executeSync(plan, request, null, null);
                    return true;
                } catch (RuntimeException failure) {
                    return false;
                }
            }
            CompletableFuture<Struct> future = engine.executeAsync(plan, request, null, null);
            assertThat(future).as("inline async call of " + scenario).isDone();
            try {
                future.get(5, TimeUnit.SECONDS);
                return true;
            } catch (ExecutionException failure) {
                return false;
            } catch (Exception e) {
                throw new IllegalStateException(e);
            }
        }

        /**
         * The interceptor objects of interceptor {@code index}: one {@link AllHooks} if it overrides every hook,
         * otherwise one single-hook recorder per hook it overrides, registered consecutively.
         */
        private List<ExecutionInterceptor> interceptor(int index) {
            Set<PipelineStage> hooks = scenario.interceptors().get(index);
            if (hooks.equals(PipelineArbitraries.HOOK_STAGES)) {
                return List.of(new AllHooks(hook -> record(index, hook)));
            }
            return hooks.stream().map(hook -> PipelineFixtures.hookRecorder(hook, () -> record(index, hook))).toList();
        }

        private void record(int interceptor, PipelineStage hook) {
            log.add(new Entry(interceptor, hook, attempt));
        }

        private void entered(CallState<?, ?> call, AttemptState attemptState, PipelineStage stage) {
            attempt = attemptState == null ? 0 : attemptState.number();
        }
    }

    /** Contributes its interceptors to every plan. */
    private record ContributingPlugin(List<ExecutionInterceptor> interceptors) implements AwsFeaturePlugin {
        @Override
        public ShapeId id() {
            return ShapeId.from("test#ContributingPlugin");
        }

        @Override
        public boolean appliesTo(AwsOperation<?, ?> operation) {
            return true;
        }

        @Override
        public void configure(PipelinePlan.Builder<?, ?> plan) {
            interceptors.forEach(plan::addInterceptor);
        }
    }

    /** One object overriding all 16 hooks; reports each invocation and returns every value unchanged. */
    private static final class AllHooks implements ExecutionInterceptor {
        private final Consumer<PipelineStage> onCall;

        AllHooks(Consumer<PipelineStage> onCall) {
            this.onCall = onCall;
        }

        @Override
        public void beforeExecution(HookContext.BeforeExecution context) {
            onCall.accept(PipelineStage.BEFORE_EXECUTION);
        }

        @Override
        public SerializableStruct modifyRequest(HookContext.ModifyRequest context) {
            onCall.accept(PipelineStage.MODIFY_REQUEST);
            return context.request();
        }

        @Override
        public void beforeAttempt(HookContext.BeforeAttempt context) {
            onCall.accept(PipelineStage.BEFORE_ATTEMPT);
        }

        @Override
        public void afterSerialization(HookContext.AfterSerialization context) {
            onCall.accept(PipelineStage.AFTER_SERIALIZATION);
        }

        @Override
        public SdkHttpRequest modifyBeforeSigning(HookContext.ModifyBeforeSigning context) {
            onCall.accept(PipelineStage.MODIFY_BEFORE_SIGNING);
            return context.httpRequest();
        }

        @Override
        public SignedRequest modifyAfterSigning(HookContext.ModifyAfterSigning context) {
            onCall.accept(PipelineStage.MODIFY_AFTER_SIGNING);
            return context.signedRequest();
        }

        @Override
        public void beforeTransmission(HookContext.BeforeTransmission context) {
            onCall.accept(PipelineStage.BEFORE_TRANSMISSION);
        }

        @Override
        public void afterTransmission(HookContext.AfterTransmission context) {
            onCall.accept(PipelineStage.AFTER_TRANSMISSION);
        }

        @Override
        public SdkHttpResponse modifyHttpResponse(HookContext.ModifyHttpResponse context) {
            onCall.accept(PipelineStage.MODIFY_HTTP_RESPONSE);
            return context.httpResponse();
        }

        @Override
        public HttpBody modifyResponseBody(HookContext.ModifyResponseBody context) {
            onCall.accept(PipelineStage.MODIFY_RESPONSE_BODY);
            return context.body();
        }

        @Override
        public void beforeDeserialization(HookContext.BeforeDeserialization context) {
            onCall.accept(PipelineStage.BEFORE_DESERIALIZATION);
        }

        @Override
        public Throwable modifyAttemptException(HookContext.ModifyAttemptException context) {
            onCall.accept(PipelineStage.MODIFY_ATTEMPT_EXCEPTION);
            return context.failure();
        }

        @Override
        public void afterAttempt(HookContext.AfterAttempt context) {
            onCall.accept(PipelineStage.AFTER_ATTEMPT);
        }

        @Override
        public SerializableStruct modifyResponse(HookContext.ModifyResponse context) {
            onCall.accept(PipelineStage.MODIFY_RESPONSE);
            return context.response();
        }

        @Override
        public Throwable modifyException(HookContext.ModifyException context) {
            onCall.accept(PipelineStage.MODIFY_EXCEPTION);
            return context.failure();
        }

        @Override
        public void afterExecution(HookContext.AfterExecution context) {
            onCall.accept(PipelineStage.AFTER_EXECUTION);
        }
    }
}
