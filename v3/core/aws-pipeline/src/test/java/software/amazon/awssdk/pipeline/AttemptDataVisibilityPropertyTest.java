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

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import java.util.function.Supplier;
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
import software.amazon.awssdk.auth.api.AuthScheme;
import software.amazon.awssdk.auth.api.AuthSchemeSelector;
import software.amazon.awssdk.auth.api.HttpSigner;
import software.amazon.awssdk.auth.api.ModeledAuth;
import software.amazon.awssdk.auth.api.SelectedAuthScheme;
import software.amazon.awssdk.auth.api.SignedRequest;
import software.amazon.awssdk.core.client.config.SdkClientConfiguration;
import software.amazon.awssdk.core.context.ContextKey;
import software.amazon.awssdk.endpoints.Endpoint;
import software.amazon.awssdk.http.AsyncHttpExchange;
import software.amazon.awssdk.http.AsyncHttpResponseHandler;
import software.amazon.awssdk.http.HttpBody;
import software.amazon.awssdk.http.HttpClientCapabilities;
import software.amazon.awssdk.http.HttpExecutionOptions;
import software.amazon.awssdk.http.HttpFailureCategory;
import software.amazon.awssdk.http.HttpPhase;
import software.amazon.awssdk.http.HttpTransportException;
import software.amazon.awssdk.http.SdkAsyncHttpClient;
import software.amazon.awssdk.http.SdkHttpRequest;
import software.amazon.awssdk.http.SdkHttpResponse;
import software.amazon.awssdk.http.SdkSyncHttpClient;
import software.amazon.awssdk.http.SyncHttpExchange;
import software.amazon.awssdk.identity.spi.Identity;
import software.amazon.awssdk.identity.spi.IdentityProvider;
import software.amazon.awssdk.identity.spi.IdentityProviders;
import software.amazon.awssdk.identity.spi.ResolveIdentityRequest;
import software.amazon.awssdk.pipeline.PipelineFixtures.CountingTimer;
import software.amazon.awssdk.pipeline.PipelineFixtures.Driver;
import software.amazon.awssdk.pipeline.PipelineFixtures.ManualExecutor;
import software.amazon.awssdk.pipeline.PipelineFixtures.ManualScheduler;
import software.amazon.awssdk.pipeline.PipelineFixtures.ScriptedHttp;
import software.amazon.awssdk.pipeline.PipelineFixtures.Struct;
import software.amazon.smithy.java.core.schema.SerializableStruct;
import software.amazon.smithy.model.shapes.ShapeId;

/**
 * Property 32: Attempt data visibility is forward-only and isolated (Requirements 10.9, 11.7).
 *
 * <p>For any number of attempts, the endpoint, auth scheme, identity and attempt-context values are unavailable
 * before the stage that produces them, visible to the later stages of the same attempt, and invisible in every other
 * attempt and call. Each try runs two calls of a generated scenario, one after the other, on one engine and plan, with
 * interceptors at a generated subset of the 16 hooks. At every invocation, each interceptor runs its generated typed
 * reads and writes of three execution keys and three attempt keys (one of each is shared by name across the scopes,
 * since keys are identities; a generated subset is declared in the plan's layout, the rest live in overflow storage).
 * Checks, against an oracle computed before the calls run:
 * <ul>
 *   <li>the hook invocations are the oracle's: per attempt, the hooks its scripted outcome reaches, and the terminal
 *       hooks of the call's success or failure;</li>
 *   <li>every read returns what the reference storage predicts: one execution map per call, which every later hook
 *       of the call reads, across attempts; one fresh attempt map per attempt, which only the later hooks of that
 *       attempt read; writes of the wrong scope are rejected and change nothing;</li>
 *   <li>the attempt context of an attempt that ended (an earlier attempt, the last attempt read from the call's
 *       terminal hooks, or an attempt of the earlier call) is closed and rejects reads;</li>
 *   <li>the pipeline's own attempt data, read through the {@link HookContext.Attempt} accessors at every attempt
 *       hook and through the {@link AttemptState} at every attempt stage point ({@link StageObserver}), is absent
 *       before the stage that produces it (endpoint, then auth scheme, then identity), present from the next stage
 *       of the attempt onward unless its producing stage failed, and fresh per attempt: one instance per attempt, never
 *       the instance of another attempt or call, and the identity is the one the provider resolved for that
 *       attempt;</li>
 *   <li>each attempt has its own context object and each call its own execution context.</li>
 * </ul>
 *
 * <p>The oracle is written from the requirements and the design ("Stage order and retry ownership",
 * "{@code AttemptState}", "Typed contexts"), not from the engine: hook reachability per outcome, the producing stage of
 * each pipeline value as an explicit set of earlier stage points, and plain maps per scope. The async driver runs on a
 * {@link ManualExecutor} and {@link ManualScheduler} drained on the test thread, with identities and responses
 * delivered inline, so each try is deterministic and its jqwik seed reproduces it.
 */
class AttemptDataVisibilityPropertyTest {
    private static final String SCHEME_ID = "test#visibility";
    private static final AwsOperation<Struct, Struct> UNSIGNED_GET = operation("UnsignedGetThing", false);
    private static final AwsOperation<Struct, Struct> SIGNED_GET = operation("SignedGetThing", true);
    /** Calls per try, run one after the other on one plan. */
    private static final int CALLS = 2;
    private static final int MAX_ATTEMPTS = 4;
    /** Bounds the async drain loop; a call needs a few rounds per attempt. */
    private static final int MAX_DRAINS = 100;

    /** The execution keys; {@code Shared} has the name of an attempt key too. */
    private static final List<ContextKey<?>> EXECUTION_KEYS = List.of(
        ContextKey.execution("Trace", String.class),
        ContextKey.execution("Count", Integer.class),
        ContextKey.execution("Shared", String.class));
    private static final List<ContextKey<?>> ATTEMPT_KEYS = List.of(
        ContextKey.attempt("Trace", String.class),
        ContextKey.attempt("Count", Integer.class),
        ContextKey.attempt("Shared", String.class));
    private static final List<ContextKey<?>> ALL_KEYS = concat(EXECUTION_KEYS, ATTEMPT_KEYS);

    private static final Set<PipelineStage> TERMINAL_HOOKS = EnumSet.of(
        PipelineStage.MODIFY_RESPONSE, PipelineStage.MODIFY_EXCEPTION, PipelineStage.AFTER_EXECUTION);
    /** The attempt stage points at which the endpoint is not yet produced: up to and including its stage. */
    private static final Set<PipelineStage> UP_TO_ENDPOINT =
        EnumSet.of(PipelineStage.BEFORE_ATTEMPT, PipelineStage.RESOLVE_ENDPOINT);
    /** ... the auth scheme, selected after the endpoint. */
    private static final Set<PipelineStage> UP_TO_AUTH_SCHEME =
        EnumSet.of(PipelineStage.BEFORE_ATTEMPT, PipelineStage.RESOLVE_ENDPOINT, PipelineStage.SELECT_AUTH_SCHEME);
    /** ... the identity, resolved for the selected scheme. */
    private static final Set<PipelineStage> UP_TO_IDENTITY =
        EnumSet.of(PipelineStage.BEFORE_ATTEMPT, PipelineStage.RESOLVE_ENDPOINT, PipelineStage.SELECT_AUTH_SCHEME,
                   PipelineStage.RESOLVE_IDENTITY);

    private static final String OK = "ok";
    private static final String NO_TARGET = "no ended attempt";
    private static final String WRONG_SCOPE = "rejected IllegalArgumentException";
    private static final String CLOSED = "closed, rejected IllegalStateException";

    /** The scripted outcome of one attempt. */
    enum Outcome {
        SUCCESS,
        /** HTTP 503: retryable. */
        SERVICE_UNAVAILABLE,
        /** HTTP 400: not retryable. */
        BAD_REQUEST,
        /** A connection reset after transmission began: retryable. */
        CONNECTION_RESET,
        /** The identity provider fails with an I/O error: retryable. Signed scenarios only. */
        IDENTITY_UNAVAILABLE;

        boolean retryable() {
            return this == SERVICE_UNAVAILABLE || this == CONNECTION_RESET || this == IDENTITY_UNAVAILABLE;
        }
    }

    /** What one generated operation of an interceptor does. */
    enum OpKind {
        /** {@code execution().put} of an execution key; a null value removes it. */
        PUT_EXECUTION,
        /** Reads an execution key: through the attempt context from an attempt hook, else through execution(). */
        GET_EXECUTION,
        /** {@code attempt().put} of an attempt key; from an execution hook, {@code execution().put}: rejected. */
        PUT_ATTEMPT,
        /** {@code attempt().get} of an attempt key; from an execution hook, {@code execution().get}: rejected. */
        GET_ATTEMPT,
        /** {@code attempt().put} of an execution key: rejected; from an execution hook, a {@link #PUT_EXECUTION}. */
        PUT_EXECUTION_THROUGH_ATTEMPT,
        /** Reads an attempt key through the context of an attempt that ended (see {@link Target}). */
        READ_ENDED_ATTEMPT
    }

    /** Which ended attempt a {@link OpKind#READ_ENDED_ATTEMPT} reads. */
    enum Target {
        /**
         * From an attempt hook, the previous attempt of the call; from a terminal hook, the call's last attempt; none
         * otherwise.
         */
        PREVIOUS_ATTEMPT,
        /** The last attempt of the previous call; none in the first call. */
        PREVIOUS_CALL
    }

    /**
     * One generated operation.
     *
     * @param key the index of the key in the scope the kind uses
     * @param value the value written (an Integer, or "v" + it for a String key); null removes
     */
    record Op(OpKind kind, int key, Integer value, Target target) {
        @Override
        public String toString() {
            return switch (kind) {
                case PUT_EXECUTION, PUT_EXECUTION_THROUGH_ATTEMPT ->
                    kind + "(" + EXECUTION_KEYS.get(key).name() + "=" + value + ")";
                case PUT_ATTEMPT -> kind + "(" + ATTEMPT_KEYS.get(key).name() + "=" + value + ")";
                case GET_EXECUTION -> kind + "(" + EXECUTION_KEYS.get(key).name() + ")";
                case GET_ATTEMPT -> kind + "(" + ATTEMPT_KEYS.get(key).name() + ")";
                case READ_ENDED_ATTEMPT -> kind + "(" + ATTEMPT_KEYS.get(key).name() + ", " + target + ")";
            };
        }
    }

    /**
     * One generated try.
     *
     * @param ops per hook with an interceptor, the operations it runs in attempt {@code n} at index {@code n - 1}
     *     (an execution hook uses index 0)
     * @param calls per call, the outcome of attempt {@code n} at index {@code n - 1}; maxAttempts of them
     */
    record Scenario(Driver driver, boolean signed, boolean endpointUnresolvable, Set<ContextKey<?>> declaredKeys,
                    Map<PipelineStage, List<List<Op>>> ops, List<List<Outcome>> calls) {
        Set<PipelineStage> hooks() {
            return ops.keySet();
        }

        int maxAttempts() {
            return calls.get(0).size();
        }

        Outcome outcome(int call, int attempt) {
            return calls.get(call).get(attempt - 1);
        }

        List<Op> ops(PipelineStage hook, int attempt) {
            return ops.get(hook).get(Math.max(attempt, 1) - 1);
        }

        AwsOperation<Struct, Struct> operation() {
            return signed ? SIGNED_GET : UNSIGNED_GET;
        }
    }

    /** Which pipeline-produced attempt values a stage point or hook must observe. */
    record Visible(boolean endpoint, boolean authScheme, boolean identity) {
    }

    /** A predicted hook invocation; {@code attempt} is 0 for an execution hook, whose {@code visible} is unused. */
    record Point(int call, PipelineStage hook, int attempt, Visible visible) {
        String id() {
            return "call " + call + " " + hook + (attempt == 0 ? "" : "#" + attempt);
        }
    }

    /** The oracle's prediction of one call. */
    record CallPrediction(List<Point> points, int attempts, boolean succeeded) {
    }

    /** An observed hook invocation and the pipeline values it read through its context. */
    record HookSeen(int call, PipelineStage hook, int attempt, AttemptContext attemptContext,
                    ExecutionContext execution, Endpoint endpoint, SelectedAuthScheme<?> authScheme,
                    Identity identity) {
        String id() {
            return "call " + call + " " + hook + (attempt == 0 ? "" : "#" + attempt);
        }
    }

    /** An observed attempt stage point and the pipeline values its attempt state held. */
    record StageSeen(int call, PipelineStage stage, int attempt, Endpoint endpoint, SelectedAuthScheme<?> authScheme,
                     Identity identity) {
    }

    /** The identity the scripted provider resolves for one attempt of one call: a new instance each time. */
    record ScriptedIdentity(int call, int attempt) implements Identity {
    }

    // Feature: aws-sdk-java-v3, Property 32: Attempt data visibility is forward-only and isolated
    // Embedded edge cases are disabled: they are mostly the first value of each generator. The statistics coverage
    // checks below fail the property if retries, cross-attempt reads, isolation or ended-context reads stop being
    // generated.
    @Property(tries = 400, edgeCases = EdgeCasesMode.NONE)
    @Label("Property 32: Attempt data visibility is forward-only and isolated")
    void attemptDataIsForwardOnlyAndIsolated(@ForAll("scenarios") Scenario scenario) {
        List<CallPrediction> predicted = new ArrayList<>();
        for (int call = 0; call < CALLS; call++) {
            predicted.add(Oracle.predictCall(scenario, call));
        }
        Coverage coverage = new Coverage();
        List<String> expectedResults = Oracle.results(scenario, predicted, coverage);

        Run run = new Run(scenario);
        run.run();

        String description = scenario.toString();
        assertThat(run.layout.executionKeys()).as("declared keys of " + description)
                                              .containsAll(declared(scenario, EXECUTION_KEYS));
        assertThat(run.layout.attemptKeys()).as("declared keys of " + description)
                                            .containsAll(declared(scenario, ATTEMPT_KEYS));
        for (int call = 0; call < CALLS; call++) {
            CallPrediction prediction = predicted.get(call);
            assertThat(run.succeeded.get(call)).as("call " + call + " succeeded, of " + description)
                                               .isEqualTo(prediction.succeeded());
            assertThat(run.lastAttempt.getOrDefault(call, 0)).as("attempts of call " + call + " of " + description)
                                                              .isEqualTo(prediction.attempts());
        }
        List<Point> points = predicted.stream().flatMap(p -> p.points().stream()).toList();
        assertThat(run.hooks).as("hook invocations of " + description).extracting(HookSeen::id)
                             .containsExactlyElementsOf(points.stream().map(Point::id).toList());
        assertContexts(run, description);
        assertHookData(run, points, description);
        assertStageData(run, scenario, description);
        assertFreshPerAttempt(run, description);
        assertThat(run.results).as("typed reads and writes of " + description)
                               .containsExactlyElementsOf(expectedResults);

        Statistics.label("driver").collect(scenario.driver());
        Statistics.label("signed").collect(scenario.signed());
        int attempts = predicted.stream().mapToInt(CallPrediction::attempts).max().orElse(0);
        Statistics.label("most attempts of a call").collect(attempts);
        collect("retried", attempts > 1, 30);
        collect("an execution value was read in another attempt or scope", coverage.crossAttemptRead, 15);
        collect("an attempt value was read later in its attempt", coverage.sameAttemptRead, 15);
        collect("an attempt key written in an earlier attempt read as absent", coverage.isolatedRead, 15);
        collect("an ended attempt context was read", coverage.endedRead, 25);
        // About 5% of tries (a terminal hook, a PREVIOUS_ATTEMPT read and a successful call); a 1% floor still fails
        // if the case stops being generated, without failing randomly seeded runs that land near the mean.
        collect("an ended attempt context was read from a terminal hook after success",
                coverage.endedReadAfterSuccess, 1);
        collect("a hook read pipeline data of a later attempt", coverage.laterAttemptPipelineRead, 15);
        collect("an identity failed", coverage.identityFailed, 3);
    }

    private static void collect(String label, boolean value, double minPercentage) {
        Statistics.label(label).collect(value);
        Statistics.label(label).coverage(c -> c.check(true).percentage(p -> p >= minPercentage));
    }

    @Provide
    Arbitrary<Scenario> scenarios() {
        return Combinators.combine(Arbitraries.of(true, false), Arbitraries.integers().between(1, MAX_ATTEMPTS))
                          .as((signed, attempts) -> Tuple.of(signed, attempts))
                          .flatMap(t -> scenarios(t.get1(), t.get2()));
    }

    private static Arbitrary<Scenario> scenarios(boolean signed, int maxAttempts) {
        Arbitrary<Map<PipelineStage, List<List<Op>>>> ops =
            PipelineArbitraries.hookSubsets().flatMap(hooks -> opsPerAttempt().list().ofSize(hooks.size())
                                                                              .map(lists -> zip(hooks, lists)));
        return Combinators.combine(
            PipelineArbitraries.drivers(),
            Arbitraries.frequency(Tuple.of(1, true), Tuple.of(9, false)),
            Arbitraries.subsetOf(ALL_KEYS),
            ops,
            outcomes(signed).list().ofSize(maxAttempts).list().ofSize(CALLS))
            .as((driver, unresolvable, declared, hookOps, calls) -> new Scenario(
                driver, signed, unresolvable, Collections.unmodifiableSet(declared), hookOps, calls));
    }

    /** The operation lists of one hook: one per possible attempt. */
    private static Arbitrary<List<List<Op>>> opsPerAttempt() {
        return ops().list().ofMaxSize(3).list().ofSize(MAX_ATTEMPTS);
    }

    private static Arbitrary<Op> ops() {
        Arbitrary<OpKind> kinds = Arbitraries.frequency(
            Tuple.of(3, OpKind.PUT_EXECUTION), Tuple.of(3, OpKind.GET_EXECUTION), Tuple.of(3, OpKind.PUT_ATTEMPT),
            Tuple.of(4, OpKind.GET_ATTEMPT), Tuple.of(1, OpKind.PUT_EXECUTION_THROUGH_ATTEMPT),
            Tuple.of(2, OpKind.READ_ENDED_ATTEMPT));
        return Combinators.combine(kinds, Arbitraries.integers().between(0, 2),
                                   Arbitraries.integers().between(0, 9).injectNull(0.15),
                                   Arbitraries.of(Target.class))
                          .as(Op::new);
    }

    private static Arbitrary<Outcome> outcomes(boolean signed) {
        List<Tuple.Tuple2<Integer, Outcome>> frequencies = new ArrayList<>(List.of(
            Tuple.of(3, Outcome.SUCCESS), Tuple.of(3, Outcome.SERVICE_UNAVAILABLE), Tuple.of(1, Outcome.BAD_REQUEST),
            Tuple.of(2, Outcome.CONNECTION_RESET)));
        if (signed) {
            frequencies.add(Tuple.of(2, Outcome.IDENTITY_UNAVAILABLE));
        }
        return Arbitraries.frequency(frequencies);
    }

    private static Map<PipelineStage, List<List<Op>>> zip(Set<PipelineStage> hooks, List<List<List<Op>>> lists) {
        Map<PipelineStage, List<List<Op>>> zipped = new LinkedHashMap<>();
        int i = 0;
        for (PipelineStage hook : hooks) {
            zipped.put(hook, lists.get(i++));
        }
        return Collections.unmodifiableMap(zipped);
    }

    // ---- Checks of the observed pipeline data ----

    /**
     * Every hook received its call's one execution context and its attempt's one attempt context; contexts are
     * distinct per call and per attempt; after the calls every attempt context is closed and rejects reads.
     */
    private static void assertContexts(Run run, String description) {
        assertThat(run.executions).as("execution contexts of " + description).hasSize(CALLS);
        assertThat(run.executions.get(0)).as("execution contexts of " + description)
                                         .isNotSameAs(run.executions.get(1));
        Set<AttemptContext> distinct = Collections.newSetFromMap(new IdentityHashMap<>());
        distinct.addAll(run.contexts.values());
        assertThat(distinct).as("attempt contexts of " + description).hasSameSizeAs(run.contexts.values());
        for (HookSeen seen : run.hooks) {
            String at = seen.id() + " of " + description;
            assertThat(seen.execution()).as("execution context at " + at).isSameAs(run.executions.get(seen.call()));
            if (seen.attempt() > 0) {
                assertThat(seen.attemptContext()).as("attempt context at " + at)
                                                 .isSameAs(run.contexts.get(List.of(seen.call(), seen.attempt())));
            }
        }
        for (Map.Entry<List<Integer>, AttemptContext> entry : run.contexts.entrySet()) {
            AttemptContext context = entry.getValue();
            String at = "attempt context " + entry.getKey() + " after the calls of " + description;
            assertThat(context.isOpen()).as(at).isFalse();
            assertThatThrownBy(() -> context.get(ATTEMPT_KEYS.get(0))).as(at)
                                                                      .isInstanceOf(IllegalStateException.class);
            assertThatThrownBy(() -> context.get(EXECUTION_KEYS.get(0))).as(at)
                                                                        .isInstanceOf(IllegalStateException.class);
        }
    }

    /** What each attempt hook read through the {@link HookContext.Attempt} accessors. */
    private static void assertHookData(Run run, List<Point> points, String description) {
        for (int i = 0; i < points.size(); i++) {
            Point point = points.get(i);
            HookSeen seen = run.hooks.get(i);
            if (point.attempt() == 0) {
                continue;
            }
            assertVisible(run, point.call(), point.attempt(), point.visible(), seen.endpoint(), seen.authScheme(),
                          seen.identity(), "hook " + seen.id() + " of " + description);
        }
    }

    /** What the attempt state held at each attempt stage point, predicted from the producing stages alone. */
    private static void assertStageData(Run run, Scenario scenario, String description) {
        assertThat(run.stages).as("attempt stage points of " + description).isNotEmpty();
        for (StageSeen seen : run.stages) {
            Visible visible = Oracle.visible(scenario, seen.call(), seen.attempt(), seen.stage());
            assertVisible(run, seen.call(), seen.attempt(), visible, seen.endpoint(), seen.authScheme(),
                          seen.identity(),
                          "stage " + seen.stage() + "#" + seen.attempt() + " of call " + seen.call() + " of "
                          + description);
        }
    }

    private static void assertVisible(Run run, int call, int attempt, Visible visible, Endpoint endpoint,
                                      SelectedAuthScheme<?> authScheme, Identity identity, String at) {
        assertThat(endpoint != null).as("endpoint visible at " + at).isEqualTo(visible.endpoint());
        assertThat(authScheme != null).as("auth scheme visible at " + at).isEqualTo(visible.authScheme());
        assertThat(identity != null).as("identity visible at " + at).isEqualTo(visible.identity());
        if (endpoint != null) {
            assertThat(endpoint.url()).as("endpoint at " + at).isEqualTo(PipelineFixtures.ENDPOINT);
        }
        if (authScheme != null) {
            assertThat(authScheme.option().schemeId()).as("auth scheme at " + at).isEqualTo(SCHEME_ID);
        }
        if (identity != null) {
            assertThat(identity).as("identity at " + at).isSameAs(run.identities.get(List.of(call, attempt)));
        }
    }

    /**
     * Within an attempt every stage and hook sees one instance of each pipeline value, and no attempt (of either call)
     * sees an instance another attempt saw.
     */
    private static void assertFreshPerAttempt(Run run, String description) {
        List<Map<List<Integer>, Object>> perValue = List.of(new HashMap<>(), new HashMap<>(), new HashMap<>());
        for (StageSeen seen : run.stages) {
            List<Integer> attempt = List.of(seen.call(), seen.attempt());
            remember(perValue.get(0), attempt, seen.endpoint(), "endpoint", description);
            remember(perValue.get(1), attempt, seen.authScheme(), "auth scheme", description);
            remember(perValue.get(2), attempt, seen.identity(), "identity", description);
        }
        for (HookSeen seen : run.hooks) {
            if (seen.attempt() > 0) {
                List<Integer> attempt = List.of(seen.call(), seen.attempt());
                remember(perValue.get(0), attempt, seen.endpoint(), "endpoint", description);
                remember(perValue.get(1), attempt, seen.authScheme(), "auth scheme", description);
                remember(perValue.get(2), attempt, seen.identity(), "identity", description);
            }
        }
        for (Map<List<Integer>, Object> values : perValue) {
            Set<Object> distinct = Collections.newSetFromMap(new IdentityHashMap<>());
            distinct.addAll(values.values());
            assertThat(distinct).as("instances per attempt " + values.keySet() + " of " + description)
                                .hasSameSizeAs(values.values());
        }
    }

    private static void remember(Map<List<Integer>, Object> values, List<Integer> attempt, Object value, String name,
                               String description) {
        if (value == null) {
            return;
        }
        Object first = values.putIfAbsent(attempt, value);
        if (first != null) {
            assertThat(value).as("one " + name + " in attempt " + attempt + " of " + description).isSameAs(first);
        }
    }

    private static List<ContextKey<?>> declared(Scenario scenario, List<ContextKey<?>> keys) {
        return keys.stream().filter(scenario.declaredKeys()::contains).toList();
    }

    // ---- The oracle ----

    /** Which rare situations a try's prediction contains; for the coverage checks. */
    private static final class Coverage {
        boolean crossAttemptRead;
        boolean sameAttemptRead;
        boolean isolatedRead;
        boolean endedRead;
        boolean endedReadAfterSuccess;
        boolean laterAttemptPipelineRead;
        boolean identityFailed;
    }

    /**
     * The reference model: the hooks each attempt reaches for its scripted outcome, the stages that produce each
     * pipeline value, and plain maps per scope.
     */
    static final class Oracle {
        private Oracle() {
        }

        /** The hook invocations of one call, its attempt count, and whether it succeeds. */
        static CallPrediction predictCall(Scenario scenario, int call) {
            List<Point> points = new ArrayList<>();
            addExecutionHook(points, scenario, call, PipelineStage.BEFORE_EXECUTION);
            addExecutionHook(points, scenario, call, PipelineStage.MODIFY_REQUEST);
            int attempt = 0;
            boolean succeeded;
            while (true) {
                attempt++;
                Outcome outcome = scenario.outcome(call, attempt);
                for (PipelineStage hook : attemptHooks(scenario.endpointUnresolvable(), outcome)) {
                    if (scenario.hooks().contains(hook)) {
                        points.add(new Point(call, hook, attempt, visible(scenario, call, attempt, hook)));
                    }
                }
                if (!scenario.endpointUnresolvable() && outcome == Outcome.SUCCESS) {
                    succeeded = true;
                    break;
                }
                // An unresolvable endpoint is a client condition, not retryable.
                if (scenario.endpointUnresolvable() || !outcome.retryable() || attempt == scenario.maxAttempts()) {
                    succeeded = false;
                    break;
                }
            }
            addExecutionHook(points, scenario, call,
                             succeeded ? PipelineStage.MODIFY_RESPONSE : PipelineStage.MODIFY_EXCEPTION);
            addExecutionHook(points, scenario, call, PipelineStage.AFTER_EXECUTION);
            return new CallPrediction(List.copyOf(points), attempt, succeeded);
        }

        private static void addExecutionHook(List<Point> points, Scenario scenario, int call, PipelineStage hook) {
            if (scenario.hooks().contains(hook)) {
                points.add(new Point(call, hook, 0, null));
            }
        }

        /** The attempt hooks an attempt reaches, in order, by where its outcome ends it. */
        private static List<PipelineStage> attemptHooks(boolean endpointUnresolvable, Outcome outcome) {
            List<PipelineStage> hooks = new ArrayList<>(List.of(PipelineStage.BEFORE_ATTEMPT));
            if (endpointUnresolvable || outcome == Outcome.IDENTITY_UNAVAILABLE) {
                hooks.addAll(List.of(PipelineStage.MODIFY_ATTEMPT_EXCEPTION, PipelineStage.AFTER_ATTEMPT));
                return hooks;
            }
            hooks.addAll(List.of(PipelineStage.AFTER_SERIALIZATION, PipelineStage.MODIFY_BEFORE_SIGNING,
                                 PipelineStage.MODIFY_AFTER_SIGNING, PipelineStage.BEFORE_TRANSMISSION,
                                 PipelineStage.AFTER_TRANSMISSION));
            if (outcome == Outcome.CONNECTION_RESET) {
                hooks.addAll(List.of(PipelineStage.MODIFY_ATTEMPT_EXCEPTION, PipelineStage.AFTER_ATTEMPT));
                return hooks;
            }
            hooks.addAll(List.of(PipelineStage.MODIFY_HTTP_RESPONSE, PipelineStage.MODIFY_RESPONSE_BODY,
                                 PipelineStage.BEFORE_DESERIALIZATION));
            if (outcome != Outcome.SUCCESS) {
                hooks.add(PipelineStage.MODIFY_ATTEMPT_EXCEPTION);
            }
            hooks.add(PipelineStage.AFTER_ATTEMPT);
            return hooks;
        }

        /**
         * The pipeline values visible at attempt stage point or hook {@code stage}: each once its producing stage has
         * run and succeeded in this attempt, never before.
         */
        static Visible visible(Scenario scenario, int call, int attempt, PipelineStage stage) {
            boolean endpoint = !scenario.endpointUnresolvable();
            boolean signed = endpoint && scenario.signed();
            boolean identity = signed && scenario.outcome(call, attempt) != Outcome.IDENTITY_UNAVAILABLE;
            return new Visible(endpoint && !UP_TO_ENDPOINT.contains(stage),
                               signed && !UP_TO_AUTH_SCHEME.contains(stage),
                               identity && !UP_TO_IDENTITY.contains(stage));
        }

        /** The result of every generated operation, in invocation order, from reference maps per scope. */
        static List<String> results(Scenario scenario, List<CallPrediction> calls, Coverage coverage) {
            List<String> results = new ArrayList<>();
            // (call, attempt, attempt key index) of every non-null attempt write so far.
            Set<List<Integer>> attemptWrites = new HashSet<>();
            for (int call = 0; call < CALLS; call++) {
                CallPrediction prediction = calls.get(call);
                Map<Integer, Object> execution = new HashMap<>();
                Map<Integer, Integer> executionWrittenAt = new HashMap<>();
                Map<Integer, Map<Integer, Object>> attempts = new HashMap<>();
                for (Point point : prediction.points()) {
                    int attempt = point.attempt();
                    boolean attemptHook = attempt > 0;
                    if (attemptHook && attempt > 1
                        && (point.visible().endpoint() || point.hook() == PipelineStage.BEFORE_ATTEMPT)) {
                        coverage.laterAttemptPipelineRead = true;
                    }
                    if (attemptHook && scenario.outcome(call, attempt) == Outcome.IDENTITY_UNAVAILABLE) {
                        coverage.identityFailed = true;
                    }
                    Map<Integer, Object> current =
                        attemptHook ? attempts.computeIfAbsent(attempt, ignored -> new HashMap<>()) : null;
                    List<Op> ops = scenario.ops(point.hook(), attempt);
                    for (int i = 0; i < ops.size(); i++) {
                        Op op = ops.get(i);
                        String result;
                        switch (op.kind()) {
                            case PUT_EXECUTION -> {
                                store(execution, EXECUTION_KEYS, op);
                                executionWrittenAt.put(op.key(), attempt);
                                result = OK;
                            }
                            case PUT_EXECUTION_THROUGH_ATTEMPT -> {
                                if (attemptHook) {
                                    result = WRONG_SCOPE;
                                } else {
                                    store(execution, EXECUTION_KEYS, op);
                                    executionWrittenAt.put(op.key(), attempt);
                                    result = OK;
                                }
                            }
                            case GET_EXECUTION -> {
                                Object value = execution.get(op.key());
                                if (value != null && executionWrittenAt.get(op.key()) != attempt) {
                                    coverage.crossAttemptRead = true;
                                }
                                result = read(value);
                            }
                            case PUT_ATTEMPT -> {
                                if (attemptHook) {
                                    store(current, ATTEMPT_KEYS, op);
                                    if (op.value() != null) {
                                        attemptWrites.add(List.of(call, attempt, op.key()));
                                    }
                                    result = OK;
                                } else {
                                    result = WRONG_SCOPE;
                                }
                            }
                            case GET_ATTEMPT -> {
                                if (attemptHook) {
                                    Object value = current.get(op.key());
                                    if (value != null) {
                                        coverage.sameAttemptRead = true;
                                    } else if (writtenElsewhere(attemptWrites, call, attempt, op.key())) {
                                        coverage.isolatedRead = true;
                                    }
                                    result = read(value);
                                } else {
                                    result = WRONG_SCOPE;
                                }
                            }
                            case READ_ENDED_ATTEMPT -> {
                                boolean target = endedTarget(op.target(), call, point.hook(), attempt,
                                                             call == 0 ? 0 : calls.get(call - 1).attempts(),
                                                             prediction.attempts()) != null;
                                if (target) {
                                    coverage.endedRead = true;
                                    if (TERMINAL_HOOKS.contains(point.hook()) && prediction.succeeded()
                                        && op.target() == Target.PREVIOUS_ATTEMPT) {
                                        coverage.endedReadAfterSuccess = true;
                                    }
                                }
                                // Every attempt a target can name has ended, so its context is closed.
                                result = target ? CLOSED : NO_TARGET;
                            }
                            default -> throw new IllegalStateException(op.toString());
                        }
                        results.add(label(point.id(), i, op) + result);
                    }
                }
            }
            return List.copyOf(results);
        }

        private static boolean writtenElsewhere(Set<List<Integer>> writes, int call, int attempt, int key) {
            for (List<Integer> write : writes) {
                if (write.get(2) == key && (write.get(0) != call || write.get(1) != attempt)) {
                    return true;
                }
            }
            return false;
        }

        private static void store(Map<Integer, Object> scope, List<ContextKey<?>> keys, Op op) {
            Object value = value(keys.get(op.key()), op.value());
            if (value == null) {
                scope.remove(op.key());
            } else {
                scope.put(op.key(), value);
            }
        }

        private static String read(Object value) {
            return "read " + render(value) + (value != null ? " (contains)" : "");
        }
    }

    /**
     * The (call, attempt) whose context a {@link OpKind#READ_ENDED_ATTEMPT} reads, or null if none: the semantics of
     * the operation, shared by the oracle (with predicted attempt counts) and the interceptors (with observed ones).
     */
    private static List<Integer> endedTarget(Target target, int call, PipelineStage hook, int attempt,
                                             int previousCallAttempts, int callAttempts) {
        if (target == Target.PREVIOUS_CALL) {
            return call == 0 ? null : List.of(call - 1, previousCallAttempts);
        }
        if (attempt > 1) {
            return List.of(call, attempt - 1);
        }
        return attempt == 0 && TERMINAL_HOOKS.contains(hook) ? List.of(call, callAttempts) : null;
    }

    private static String label(String point, int index, Op op) {
        return point + " op " + index + " " + op + ": ";
    }

    private static String render(Object value) {
        return value == null ? "null" : value.getClass().getSimpleName() + " " + value;
    }

    /** The value an operation writes under {@code key}: {@code n} for an Integer key, "v" + n for a String key. */
    private static Object value(ContextKey<?> key, Integer n) {
        if (n == null) {
            return null;
        }
        return key.valueType() == Integer.class ? n : "v" + n;
    }

    // ---- The calls ----

    /**
     * The two calls of a scenario on one engine: scripted identity provider and transport (one outcome per attempt,
     * the attempt read from the stage observer), interceptors that run the generated operations, and what they saw.
     */
    private static final class Run implements SdkSyncHttpClient, SdkAsyncHttpClient {
        final List<String> results = new ArrayList<>();
        final List<HookSeen> hooks = new ArrayList<>();
        final List<StageSeen> stages = new ArrayList<>();
        /** Per (call, attempt), the attempt context the stage observer saw first. */
        final Map<List<Integer>, AttemptContext> contexts = new LinkedHashMap<>();
        final Map<Integer, Integer> lastAttempt = new HashMap<>();
        final List<ExecutionContext> executions = new ArrayList<>();
        final Map<List<Integer>, Identity> identities = new HashMap<>();
        final List<Boolean> succeeded = new ArrayList<>();
        ContextLayout layout;
        private final Scenario scenario;
        private final boolean async;
        private final ManualExecutor executor = new ManualExecutor();
        private final ManualScheduler scheduler = new ManualScheduler();
        private CallState<?, ?> currentCall;
        private int call;
        private int attempt;

        Run(Scenario scenario) {
            this.scenario = scenario;
            this.async = scenario.driver() == Driver.ASYNC;
        }

        void run() {
            ClientRuntime.Builder runtime =
                PipelineFixtures.engineRuntime(PipelineFixtures.retries(scenario.maxAttempts()),
                                               scenario.driver().mode);
            if (scenario.endpointUnresolvable()) {
                runtime.configuration(SdkClientConfiguration.empty());
            }
            if (scenario.signed()) {
                runtime.authSchemeSelector(AuthSchemeSelector.create(List.of(new ScriptedScheme(this::identity))));
            }
            for (PipelineStage hook : scenario.hooks()) {
                runtime.addInterceptor(interceptor(hook, context -> invoked(hook, context)));
            }
            if (!scenario.declaredKeys().isEmpty()) {
                runtime.addPlugin(new DeclaredKeys(scenario.declaredKeys()));
            }
            if (async) {
                runtime.completionExecutor(executor).retryScheduler(scheduler);
            }
            AwsCallEngine.Builder builder = AwsCallEngine.builder().runtime(runtime.build())
                                                         .stageObserver(this::entered);
            AwsCallEngine engine = (async ? builder.asyncHttpClient(this) : builder.syncHttpClient(this)).build();
            PipelinePlan<Struct, Struct> plan = engine.runtime().plan(scenario.operation());
            layout = plan.contextLayout();
            for (call = 0; call < CALLS; call++) {
                attempt = 0;
                succeeded.add(execute(engine, plan));
            }
        }

        private boolean execute(AwsCallEngine engine, PipelinePlan<Struct, Struct> plan) {
            Struct request = PipelineFixtures.request(GET_INPUT, "a");
            if (!async) {
                try {
                    engine.executeSync(plan, request, null, null);
                    return true;
                } catch (RuntimeException failure) {
                    return false;
                }
            }
            CompletableFuture<Struct> future = engine.executeAsync(plan, request, null, null);
            for (int drain = 0; !future.isDone(); drain++) {
                assertThat(drain).as("drains of call " + call + " of " + scenario).isLessThan(MAX_DRAINS);
                executor.runAll();
                for (CountingTimer timer : scheduler.timers) {
                    timer.complete(null);
                }
            }
            executor.runAll();
            return !future.isCompletedExceptionally();
        }

        private void entered(CallState<?, ?> callState, AttemptState attemptState, PipelineStage stage) {
            if (callState != currentCall) {
                currentCall = callState;
                executions.add(callState.executionContext());
            }
            if (attemptState == null) {
                return;
            }
            attempt = attemptState.number();
            List<Integer> id = List.of(call, attempt);
            contexts.putIfAbsent(id, attemptState.context());
            lastAttempt.merge(call, attempt, Math::max);
            stages.add(new StageSeen(call, stage, attempt, attemptState.endpoint().orElse(null),
                                     attemptState.authScheme().orElse(null), attemptState.identity().orElse(null)));
        }

        /** One hook invocation: records what it sees, then runs its operations, recording each result. */
        private void invoked(PipelineStage hook, HookContext context) {
            HookContext.Attempt attemptHook = context instanceof HookContext.Attempt a ? a : null;
            int number = attemptHook == null ? 0 : attemptHook.attemptNumber();
            hooks.add(new HookSeen(call, hook, number, attemptHook == null ? null : attemptHook.attempt(),
                                   context.execution(),
                                   attemptHook == null ? null : attemptHook.endpoint().orElse(null),
                                   attemptHook == null ? null : attemptHook.authScheme().orElse(null),
                                   attemptHook == null ? null : attemptHook.identity().orElse(null)));
            if (number > scenario.maxAttempts()) {
                // More attempts than allowed: the hook timeline check reports it.
                return;
            }
            List<Op> ops = scenario.ops(hook, number);
            for (int i = 0; i < ops.size(); i++) {
                String id = "call " + call + " " + hook + (number == 0 ? "" : "#" + number);
                results.add(label(id, i, ops.get(i)) + perform(hook, context, attemptHook, number, ops.get(i)));
            }
        }

        private String perform(PipelineStage hook, HookContext context, HookContext.Attempt attemptHook, int number,
                               Op op) {
            try {
                ContextKey<?> executionKey = EXECUTION_KEYS.get(op.key());
                ContextKey<?> attemptKey = ATTEMPT_KEYS.get(op.key());
                switch (op.kind()) {
                    case PUT_EXECUTION -> {
                        put(context.execution(), executionKey, op.value());
                        return OK;
                    }
                    case PUT_EXECUTION_THROUGH_ATTEMPT -> {
                        if (attemptHook != null) {
                            put(attemptHook.attempt(), executionKey, op.value());
                        } else {
                            put(context.execution(), executionKey, op.value());
                        }
                        return OK;
                    }
                    case GET_EXECUTION -> {
                        return attemptHook != null
                               ? read(attemptHook.attempt().get(executionKey),
                                      attemptHook.attempt().contains(executionKey))
                               : read(context.execution().get(executionKey),
                                      context.execution().contains(executionKey));
                    }
                    case PUT_ATTEMPT -> {
                        if (attemptHook != null) {
                            put(attemptHook.attempt(), attemptKey, op.value());
                        } else {
                            put(context.execution(), attemptKey, op.value());
                        }
                        return OK;
                    }
                    case GET_ATTEMPT -> {
                        return attemptHook != null
                               ? read(attemptHook.attempt().get(attemptKey), attemptHook.attempt().contains(attemptKey))
                               : read(context.execution().get(attemptKey), context.execution().contains(attemptKey));
                    }
                    case READ_ENDED_ATTEMPT -> {
                        List<Integer> target = endedTarget(op.target(), call, hook, number,
                                                           lastAttempt.getOrDefault(call - 1, 0),
                                                           lastAttempt.getOrDefault(call, 0));
                        AttemptContext ended = target == null ? null : contexts.get(target);
                        return ended == null ? NO_TARGET : readEnded(ended, attemptKey);
                    }
                    default -> throw new IllegalStateException(op.toString());
                }
            } catch (RuntimeException rejected) {
                return "rejected " + rejected.getClass().getSimpleName();
            }
        }

        private static String readEnded(AttemptContext ended, ContextKey<?> key) {
            String state = ended.isOpen() ? "open" : "closed";
            try {
                return state + ", read " + render(ended.get(key));
            } catch (RuntimeException rejected) {
                return state + ", rejected " + rejected.getClass().getSimpleName();
            }
        }

        private static String read(Object value, boolean contains) {
            return "read " + render(value) + (contains ? " (contains)" : "");
        }

        private static <T> void put(ExecutionContext context, ContextKey<T> key, Integer n) {
            context.put(key, key.cast(value(key, n)));
        }

        private static <T> void put(AttemptContext context, ContextKey<T> key, Integer n) {
            context.put(key, key.cast(value(key, n)));
        }

        /** The scripted identity of the current attempt: a new instance, or the outcome's I/O failure. */
        private CompletableFuture<Identity> identity() {
            if (scenario.outcome(call, attempt) == Outcome.IDENTITY_UNAVAILABLE) {
                return CompletableFuture.failedFuture(
                    new UncheckedIOException(new IOException("The credentials endpoint is unreachable")));
            }
            Identity identity = new ScriptedIdentity(call, attempt);
            identities.put(List.of(call, attempt), identity);
            return CompletableFuture.completedFuture(identity);
        }

        /** A transport scripted with the current attempt's outcome. */
        private ScriptedHttp transmission() {
            ScriptedHttp http = new ScriptedHttp();
            return switch (scenario.outcome(call, attempt)) {
                case SERVICE_UNAVAILABLE -> http.respond(503, null);
                case BAD_REQUEST -> http.respond(400, null);
                case CONNECTION_RESET -> http.fail(HttpTransportException.create(
                    HttpPhase.CONNECT, HttpFailureCategory.Kind.IO, "Connection reset", new IOException("reset")));
                case SUCCESS, IDENTITY_UNAVAILABLE -> http.respond(200, "v");
            };
        }

        @Override
        public SyncHttpExchange prepare(SdkHttpRequest request, HttpExecutionOptions options) {
            return transmission().prepare(request, options);
        }

        @Override
        public AsyncHttpExchange execute(SdkHttpRequest request, AsyncHttpResponseHandler handler,
                                         HttpExecutionOptions options) {
            return transmission().execute(request, handler, options);
        }

        @Override
        public HttpClientCapabilities capabilities() {
            throw new UnsupportedOperationException();
        }

        @Override
        public String clientName() {
            return "AttemptDataVisibilityPropertyTest";
        }

        @Override
        public void close() {
        }
    }

    /**
     * An interceptor that implements only the hook of {@code hook} and hands its context to {@code onCall}, returning
     * every value unchanged. Each hook has its own class, so a plan activates exactly the hooks it is given.
     */
    private static ExecutionInterceptor interceptor(PipelineStage hook, Consumer<HookContext> onCall) {
        return switch (hook) {
            case BEFORE_EXECUTION -> new ExecutionInterceptor() {
                @Override
                public void beforeExecution(HookContext.BeforeExecution context) {
                    onCall.accept(context);
                }
            };
            case MODIFY_REQUEST -> new ExecutionInterceptor() {
                @Override
                public SerializableStruct modifyRequest(HookContext.ModifyRequest context) {
                    onCall.accept(context);
                    return context.request();
                }
            };
            case BEFORE_ATTEMPT -> new ExecutionInterceptor() {
                @Override
                public void beforeAttempt(HookContext.BeforeAttempt context) {
                    onCall.accept(context);
                }
            };
            case AFTER_SERIALIZATION -> new ExecutionInterceptor() {
                @Override
                public void afterSerialization(HookContext.AfterSerialization context) {
                    onCall.accept(context);
                }
            };
            case MODIFY_BEFORE_SIGNING -> new ExecutionInterceptor() {
                @Override
                public SdkHttpRequest modifyBeforeSigning(HookContext.ModifyBeforeSigning context) {
                    onCall.accept(context);
                    return context.httpRequest();
                }
            };
            case MODIFY_AFTER_SIGNING -> new ExecutionInterceptor() {
                @Override
                public SignedRequest modifyAfterSigning(HookContext.ModifyAfterSigning context) {
                    onCall.accept(context);
                    return context.signedRequest();
                }
            };
            case BEFORE_TRANSMISSION -> new ExecutionInterceptor() {
                @Override
                public void beforeTransmission(HookContext.BeforeTransmission context) {
                    onCall.accept(context);
                }
            };
            case AFTER_TRANSMISSION -> new ExecutionInterceptor() {
                @Override
                public void afterTransmission(HookContext.AfterTransmission context) {
                    onCall.accept(context);
                }
            };
            case MODIFY_HTTP_RESPONSE -> new ExecutionInterceptor() {
                @Override
                public SdkHttpResponse modifyHttpResponse(HookContext.ModifyHttpResponse context) {
                    onCall.accept(context);
                    return context.httpResponse();
                }
            };
            case MODIFY_RESPONSE_BODY -> new ExecutionInterceptor() {
                @Override
                public HttpBody modifyResponseBody(HookContext.ModifyResponseBody context) {
                    onCall.accept(context);
                    return context.body();
                }
            };
            case BEFORE_DESERIALIZATION -> new ExecutionInterceptor() {
                @Override
                public void beforeDeserialization(HookContext.BeforeDeserialization context) {
                    onCall.accept(context);
                }
            };
            case MODIFY_ATTEMPT_EXCEPTION -> new ExecutionInterceptor() {
                @Override
                public Throwable modifyAttemptException(HookContext.ModifyAttemptException context) {
                    onCall.accept(context);
                    return context.failure();
                }
            };
            case AFTER_ATTEMPT -> new ExecutionInterceptor() {
                @Override
                public void afterAttempt(HookContext.AfterAttempt context) {
                    onCall.accept(context);
                }
            };
            case MODIFY_RESPONSE -> new ExecutionInterceptor() {
                @Override
                public SerializableStruct modifyResponse(HookContext.ModifyResponse context) {
                    onCall.accept(context);
                    return context.response();
                }
            };
            case MODIFY_EXCEPTION -> new ExecutionInterceptor() {
                @Override
                public Throwable modifyException(HookContext.ModifyException context) {
                    onCall.accept(context);
                    return context.failure();
                }
            };
            case AFTER_EXECUTION -> new ExecutionInterceptor() {
                @Override
                public void afterExecution(HookContext.AfterExecution context) {
                    onCall.accept(context);
                }
            };
            default -> throw new IllegalArgumentException(hook + " is not a hook stage");
        };
    }

    private static AwsOperation<Struct, Struct> operation(String name, boolean signed) {
        String id = "com.example#" + name;
        ModeledAuth auth = signed ? ModeledAuth.builder().operationId(id).addServiceScheme(SCHEME_ID).build() : null;
        return PipelineFixtures.operation(id, GET_INPUT, GET_OUTPUT, auth);
    }

    private static List<ContextKey<?>> concat(List<ContextKey<?>> first, List<ContextKey<?>> second) {
        List<ContextKey<?>> all = new ArrayList<>(first);
        all.addAll(second);
        return List.copyOf(all);
    }

    /** A scheme whose identity provider returns the scripted identity of the current attempt; signs nothing. */
    private static final class ScriptedScheme implements AuthScheme<Identity> {
        private final Supplier<CompletableFuture<Identity>> identities;

        ScriptedScheme(Supplier<CompletableFuture<Identity>> identities) {
            this.identities = identities;
        }

        @Override
        public String schemeId() {
            return SCHEME_ID;
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
                    return identities.get();
                }
            });
        }

        @Override
        public HttpSigner<Identity> signer() {
            return request -> SignedRequest.unsigned(request.request());
        }
    }

    /** Declares the given keys in the plan's layout, so they use slots instead of overflow storage. */
    private record DeclaredKeys(Set<ContextKey<?>> keys) implements AwsFeaturePlugin {
        @Override
        public ShapeId id() {
            return ShapeId.from("test#DeclaredKeys");
        }

        @Override
        public boolean appliesTo(AwsOperation<?, ?> operation) {
            return true;
        }

        @Override
        public void configure(PipelinePlan.Builder<?, ?> plan) {
            keys.forEach(plan::declare);
        }
    }
}
