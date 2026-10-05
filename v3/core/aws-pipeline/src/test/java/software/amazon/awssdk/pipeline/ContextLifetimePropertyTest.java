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
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.function.BiConsumer;
import java.util.function.Function;
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
import software.amazon.awssdk.core.context.ContextKey;
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

/**
 * Property 38: Execution context survives and attempt context does not (Requirements 11.6, 11.7).
 *
 * <p>For any multi-attempt trace (1-3 attempts of scripted outcomes, retried while retryable) on either driver, and
 * typed writes and reads of String, Integer and {@link Tag} keys of both scopes at generated hook positions, every
 * result matches nested-scope reference storage: one execution map per call that every later hook reads, in every
 * later attempt and through {@code AFTER_EXECUTION}, and a fresh attempt map per attempt that no later attempt sees.
 * Each try runs two calls one after the other on one engine, with their own generated operations. Checks:
 * <ul>
 *   <li>execution values written at any hook persist across attempts and after the call, and never leak into the
 *       other call; attempt values are visible only within their own attempt;</li>
 *   <li>at every hook exactly the current attempt's context is open, so an {@link AttemptContext} retained from an
 *       earlier attempt (of this call or the previous one) is closed and rejects reads and writes with
 *       {@link IllegalStateException}, while its {@link AttemptContext#execution()} still reads its call's values;</li>
 *   <li>reads return the written value with its type, and the same instance for {@link Tag} values.</li>
 * </ul>
 *
 * <p>Property 32 ({@code AttemptDataVisibilityPropertyTest}) checks forward-only visibility of pipeline data and
 * wrong-scope rejection; this property checks lifetimes: which contexts are open at each hook, writes and reads
 * through retained contexts of ended attempts and calls, and execution values after the calls end. The hook path
 * per outcome is the one Property 36 checks; the async driver runs inline, so each try is deterministic.
 */
class ContextLifetimePropertyTest {
    private static final int CALLS = 2;
    private static final int MAX_ATTEMPTS = 3;
    private static final List<ContextKey<?>> EXECUTION_KEYS = List.of(
        ContextKey.execution("Name", String.class), ContextKey.execution("Count", Integer.class),
        ContextKey.execution("Tag", Tag.class));
    /** One per value type of {@link #EXECUTION_KEYS}; {@code Name} shares its name with an execution key. */
    private static final List<ContextKey<?>> ATTEMPT_KEYS = List.of(
        ContextKey.attempt("Name", String.class), ContextKey.attempt("Count", Integer.class),
        ContextKey.attempt("Tag", Tag.class));
    private static final List<ContextKey<?>> ALL_KEYS = List.of(
        EXECUTION_KEYS.get(0), EXECUTION_KEYS.get(1), EXECUTION_KEYS.get(2),
        ATTEMPT_KEYS.get(0), ATTEMPT_KEYS.get(1), ATTEMPT_KEYS.get(2));
    private static final List<PipelineStage> REQUEST_HOOKS = List.of(
        PipelineStage.BEFORE_ATTEMPT, PipelineStage.AFTER_SERIALIZATION, PipelineStage.MODIFY_BEFORE_SIGNING,
        PipelineStage.MODIFY_AFTER_SIGNING, PipelineStage.BEFORE_TRANSMISSION, PipelineStage.AFTER_TRANSMISSION);
    private static final List<PipelineStage> RESPONSE_HOOKS = List.of(
        PipelineStage.MODIFY_HTTP_RESPONSE, PipelineStage.MODIFY_RESPONSE_BODY, PipelineStage.BEFORE_DESERIALIZATION);
    private static final String OK = "ok";
    private static final String NO_TARGET = "no ended attempt";
    private static final String CLOSED = IllegalStateException.class.getSimpleName();

    enum Outcome {
        SUCCESS, SERVICE_UNAVAILABLE, BAD_REQUEST, CONNECTION_RESET;

        boolean retryable() {
            return this == SERVICE_UNAVAILABLE || this == CONNECTION_RESET;
        }
    }

    /**
     * What one operation does. From an execution hook, {@code PUT_ATTEMPT} and {@code GET_ATTEMPT} have no current
     * attempt and go through the retained context of an ended attempt, like {@code RETAINED_PUT} and
     * {@code RETAINED_GET}.
     */
    enum Kind {
        /** {@code execution().put} of an execution key; null removes. */
        PUT_EXECUTION,
        /** An execution key, read through {@code attempt()} from an attempt hook, else through {@code execution()}. */
        GET_EXECUTION,
        /** {@code attempt().put} of an attempt key. */
        PUT_ATTEMPT,
        /** {@code attempt().get} of an attempt key. */
        GET_ATTEMPT,
        /** A read of any key through the retained context of an ended attempt. */
        RETAINED_GET,
        /** A write of an attempt key through the retained context of an ended attempt. */
        RETAINED_PUT,
        /** A read of an execution key through {@code execution()} of the retained context of an ended attempt. */
        RETAINED_EXECUTION_GET
    }

    /** A custom value type; reads must return the written instance. */
    record Tag(String name) {
    }

    /**
     * One operation.
     *
     * @param key an index into {@link #ALL_KEYS} for {@code RETAINED_GET}; otherwise {@code key % 3} indexes the scope
     * @param value of the type of {@code key % 3}; null removes
     * @param back the retained target: the attempt {@code back} before the hook's position (the current attempt, or
     *     for an execution hook the attempt after the last one started); before attempt 1, the previous call's last
     */
    record Op(Kind kind, int key, Object value, int back) {
        ContextKey<?> executionKey() {
            return EXECUTION_KEYS.get(key % 3);
        }

        ContextKey<?> attemptKey() {
            return ATTEMPT_KEYS.get(key % 3);
        }

        @Override
        public String toString() {
            ContextKey<?> k = kind == Kind.RETAINED_GET ? ALL_KEYS.get(key) : kind.name().contains("EXECUTION")
                                                                             ? executionKey() : attemptKey();
            return kind + "(" + k.scope() + " " + k.name() + (kind.name().contains("PUT") ? "=" + value : "")
                   + (kind.name().startsWith("RETAINED") || kind.name().endsWith("ATTEMPT") ? ", back " + back : "")
                   + ")";
        }
    }

    /**
     * One try.
     *
     * @param outcomes per call, the outcome of attempt {@code n} at index {@code n - 1}
     * @param ops per hook with operations, per call, per attempt (index 0 for an execution hook)
     */
    record Scenario(Driver driver, int maxAttempts, List<List<Outcome>> outcomes,
                    Map<PipelineStage, List<List<List<Op>>>> ops) {
        List<Op> ops(PipelineStage hook, int call, int attempt) {
            List<List<List<Op>>> perCall = ops.get(hook);
            return perCall == null ? List.of() : perCall.get(call).get(Math.max(attempt, 1) - 1);
        }
    }

    /** One observed or predicted result. */
    record Obs(String where, Object result) {
    }

    /** At a hook: which captured attempt contexts are open, and whether the hook got its call's and attempt's. */
    record Lifetime(List<List<Integer>> open, boolean sameContexts) {
    }

    /** An access through a retained attempt context: whether it was open, and what the access returned or threw. */
    record Retained(boolean open, String outcome) {
    }

    // Feature: aws-sdk-java-v3, Property 38: Execution context survives and attempt context does not
    // Embedded edge cases are disabled; the coverage checks fail the property if the lifetime cases stop being
    // generated. The tightest case ("an execution value written in an attempt was read later") occurs in about 25% of
    // scenarios against a 20% floor; 2000 tries keep that floor about five standard deviations below the mean, so the
    // check flags a generator regression rather than sampling noise (at 400 tries a run measured 19% and failed).
    @Property(tries = 2000, edgeCases = EdgeCasesMode.NONE)
    @Label("Property 38: Execution context survives and attempt context does not")
    void executionContextSurvivesAndAttemptContextDoesNot(@ForAll("scenarios") Scenario scenario) {
        Oracle oracle = new Oracle(scenario);
        Run run = new Run(scenario, oracle.attemptsPerCall);
        run.run();

        assertThat(run.observed).as(scenario.toString()).containsExactlyElementsOf(oracle.expected);
        for (int i = 0; i < oracle.expected.size(); i++) {
            if (oracle.expected.get(i).result() instanceof Tag tag) {
                assertThat(run.observed.get(i).result()).as(oracle.expected.get(i).where()).isSameAs(tag);
            }
        }

        Coverage coverage = oracle.coverage;
        Statistics.label("driver").collect(scenario.driver());
        collect("retried", oracle.attemptsPerCall.stream().anyMatch(a -> a > 1), 30);
        collect("an execution value written in an attempt was read later", coverage.survived, 20);
        collect("an attempt key written in an earlier attempt read as absent", coverage.isolated, 10);
        collect("a retained context of an earlier attempt of the call was accessed", coverage.retainedSameCall, 15);
        collect("a retained context of the previous call was accessed", coverage.retainedPreviousCall, 15);
        collect("execution() of a closed attempt context was read", coverage.closedExecutionRead, 10);
        collect("the second call read as absent a key the first call held", coverage.notLeaked, 15);
        collect("a Tag instance was read back", coverage.tagRead, 15);
    }

    private static void collect(String label, boolean value, double minPercentage) {
        Statistics.label(label).collect(value);
        Statistics.label(label).coverage(c -> c.check(true).percentage(p -> p >= minPercentage));
    }

    @Provide
    Arbitrary<Scenario> scenarios() {
        Arbitrary<Outcome> outcomes = Arbitraries.frequency(
            Tuple.of(3, Outcome.SUCCESS), Tuple.of(3, Outcome.SERVICE_UNAVAILABLE), Tuple.of(1, Outcome.BAD_REQUEST),
            Tuple.of(2, Outcome.CONNECTION_RESET));
        Arbitrary<Kind> kinds = Arbitraries.frequency(
            Tuple.of(3, Kind.PUT_EXECUTION), Tuple.of(3, Kind.GET_EXECUTION), Tuple.of(3, Kind.PUT_ATTEMPT),
            Tuple.of(3, Kind.GET_ATTEMPT), Tuple.of(1, Kind.RETAINED_GET), Tuple.of(1, Kind.RETAINED_PUT),
            Tuple.of(1, Kind.RETAINED_EXECUTION_GET));
        Arbitrary<Op> op = Combinators.combine(kinds, Arbitraries.integers().between(0, 5),
                                               Arbitraries.integers().between(0, 9).injectNull(0.1),
                                               Arbitraries.integers().between(1, MAX_ATTEMPTS))
                                      .as((kind, key, n, back) -> new Op(kind, key, value(key % 3, n), back));
        Arbitrary<List<List<List<Op>>>> perHook = op.list().ofMaxSize(2).list().ofSize(MAX_ATTEMPTS).list()
                                                    .ofSize(CALLS);
        Arbitrary<Map<PipelineStage, List<List<List<Op>>>>> ops = PipelineArbitraries.hookSubsets().flatMap(
            hooks -> perHook.list().ofSize(hooks.size()).map(lists -> {
                Map<PipelineStage, List<List<List<Op>>>> zipped = new LinkedHashMap<>();
                int i = 0;
                for (PipelineStage hook : hooks) {
                    zipped.put(hook, lists.get(i++));
                }
                return Collections.unmodifiableMap(zipped);
            }));
        return Arbitraries.integers().between(1, MAX_ATTEMPTS).flatMap(max -> Combinators.combine(
            PipelineArbitraries.drivers(), outcomes.list().ofSize(max).list().ofSize(CALLS), ops)
            .as((driver, scripts, hookOps) -> new Scenario(driver, max, scripts, hookOps)));
    }

    /** A new instance of value type {@code type} (String, Integer, Tag), or null. */
    private static Object value(int type, Integer n) {
        if (n == null) {
            return null;
        }
        return switch (type) {
            case 0 -> "s" + n;
            case 1 -> 1000 + n;
            default -> new Tag("t" + n);
        };
    }

    /** The retained target of {@code back} from {@code position}: an earlier attempt, or the previous call's last. */
    private static List<Integer> target(int call, int position, int back, int previousCallAttempts) {
        if (position - back >= 1) {
            return List.of(call, position - back);
        }
        return call > 0 ? List.of(call - 1, previousCallAttempts) : null;
    }

    private static String id(int call, PipelineStage hook, int attempt) {
        return "call " + call + " " + hook + (attempt == 0 ? "" : "#" + attempt);
    }

    private static final class Coverage {
        boolean survived;
        boolean isolated;
        boolean retainedSameCall;
        boolean retainedPreviousCall;
        boolean closedExecutionRead;
        boolean notLeaked;
        boolean tagRead;
    }

    /** A value in the execution map and the attempt that wrote it (0 for an execution hook). */
    private record Written(Object value, int attempt) {
    }

    /**
     * The reference model: the hook path of each outcome (as in Property 36), one execution map per call, and a fresh
     * attempt map per attempt; every ended attempt's context is closed.
     */
    private static final class Oracle {
        final List<Obs> expected = new ArrayList<>();
        final List<Integer> attemptsPerCall = new ArrayList<>();
        final Coverage coverage = new Coverage();
        private final Scenario scenario;
        private final List<Map<ContextKey<?>, Written>> executions = new ArrayList<>();
        private Map<ContextKey<?>, Object> attemptValues;
        /** The attempt keys written in an earlier attempt of the current call. */
        private final Set<ContextKey<?>> writtenEarlier = new HashSet<>();
        private final Set<ContextKey<?>> writtenNow = new HashSet<>();

        Oracle(Scenario scenario) {
            this.scenario = scenario;
            for (int call = 0; call < CALLS; call++) {
                predictCall(call);
            }
            for (int call = 0; call < CALLS; call++) {
                for (ContextKey<?> key : EXECUTION_KEYS) {
                    expected.add(new Obs("call " + call + " after the calls: " + key.name(),
                                         executionValue(call, key)));
                }
            }
            for (int call = 0; call < CALLS; call++) {
                for (int attempt = 1; attempt <= attemptsPerCall.get(call); attempt++) {
                    expected.add(new Obs("attempt " + List.of(call, attempt) + " after the calls",
                                         new Retained(false, CLOSED)));
                }
            }
            expected.add(new Obs("distinct contexts", true));
        }

        private void predictCall(int call) {
            executions.add(new HashMap<>());
            attemptsPerCall.add(0);
            writtenEarlier.clear();
            visit(call, PipelineStage.BEFORE_EXECUTION, 0);
            visit(call, PipelineStage.MODIFY_REQUEST, 0);
            boolean succeeded;
            for (int attempt = 1; ; attempt++) {
                Outcome outcome = scenario.outcomes().get(call).get(attempt - 1);
                attemptsPerCall.set(call, attempt);
                attemptValues = new HashMap<>();
                writtenNow.clear();
                List<PipelineStage> hooks = new ArrayList<>(REQUEST_HOOKS);
                if (outcome != Outcome.CONNECTION_RESET) {
                    hooks.addAll(RESPONSE_HOOKS);
                }
                if (outcome != Outcome.SUCCESS) {
                    hooks.add(PipelineStage.MODIFY_ATTEMPT_EXCEPTION);
                }
                hooks.add(PipelineStage.AFTER_ATTEMPT);
                hooks.forEach(hook -> visit(call, hook, attemptsPerCall.get(call)));
                writtenEarlier.addAll(writtenNow);
                if (outcome == Outcome.SUCCESS || !outcome.retryable() || attempt == scenario.maxAttempts()) {
                    succeeded = outcome == Outcome.SUCCESS;
                    break;
                }
            }
            visit(call, succeeded ? PipelineStage.MODIFY_RESPONSE : PipelineStage.MODIFY_EXCEPTION, 0);
            visit(call, PipelineStage.AFTER_EXECUTION, 0);
            expected.add(new Obs("call " + call + " succeeded", succeeded));
        }

        private void visit(int call, PipelineStage hook, int attempt) {
            String id = id(call, hook, attempt);
            expected.add(new Obs(id, new Lifetime(attempt > 0 ? List.of(List.of(call, attempt)) : List.of(), true)));
            int position = attempt > 0 ? attempt : attemptsPerCall.get(call) + 1;
            List<Op> ops = scenario.ops(hook, call, attempt);
            for (int i = 0; i < ops.size(); i++) {
                expected.add(new Obs(id + " [" + i + "] " + ops.get(i), result(call, attempt, position, ops.get(i))));
            }
        }

        private Object result(int call, int attempt, int position, Op op) {
            Map<ContextKey<?>, Written> execution = executions.get(call);
            Kind kind = op.kind();
            if (attempt == 0 && (kind == Kind.PUT_ATTEMPT || kind == Kind.GET_ATTEMPT)) {
                kind = kind == Kind.PUT_ATTEMPT ? Kind.RETAINED_PUT : Kind.RETAINED_GET;
            }
            switch (kind) {
                case PUT_EXECUTION -> {
                    if (op.value() == null) {
                        execution.remove(op.executionKey());
                    } else {
                        execution.put(op.executionKey(), new Written(op.value(), attempt));
                    }
                    return OK;
                }
                case GET_EXECUTION -> {
                    Written written = execution.get(op.executionKey());
                    coverage.survived |= written != null && written.attempt() > 0
                                         && (attempt == 0 || attempt > written.attempt());
                    coverage.notLeaked |= call > 0 && written == null && executionValue(0, op.executionKey()) != null;
                    return read(written == null ? null : written.value());
                }
                case PUT_ATTEMPT -> {
                    writtenNow.add(op.attemptKey());
                    if (op.value() == null) {
                        attemptValues.remove(op.attemptKey());
                    } else {
                        attemptValues.put(op.attemptKey(), op.value());
                    }
                    return OK;
                }
                case GET_ATTEMPT -> {
                    Object value = attemptValues.get(op.attemptKey());
                    coverage.isolated |= value == null && writtenEarlier.contains(op.attemptKey());
                    return read(value);
                }
                default -> {
                    List<Integer> target = target(call, position, op.back(), call == 0 ? 0 : attemptsPerCall.get(0));
                    if (target == null) {
                        return NO_TARGET;
                    }
                    coverage.retainedSameCall |= target.get(0) == call;
                    coverage.retainedPreviousCall |= target.get(0) != call;
                    if (kind == Kind.RETAINED_EXECUTION_GET) {
                        coverage.closedExecutionRead = true;
                        return read(executionValue(target.get(0), op.executionKey()));
                    }
                    return new Retained(false, CLOSED);
                }
            }
        }

        private Object read(Object value) {
            coverage.tagRead |= value instanceof Tag;
            return value;
        }

        private Object executionValue(int call, ContextKey<?> key) {
            Written written = executions.get(call).get(key);
            return written == null ? null : written.value();
        }
    }

    /** The two calls of a scenario on one engine, with one interceptor that runs the generated operations. */
    private static final class Run {
        final List<Obs> observed = new ArrayList<>();
        private final Scenario scenario;
        private final List<Integer> predictedAttempts;
        private final Map<List<Integer>, AttemptContext> attempts = new LinkedHashMap<>();
        private final List<ExecutionContext> executions = new ArrayList<>();
        private final List<Integer> attemptsPerCall = new ArrayList<>();
        private int call;

        Run(Scenario scenario, List<Integer> predictedAttempts) {
            this.scenario = scenario;
            this.predictedAttempts = predictedAttempts;
        }

        void run() {
            Driver driver = scenario.driver();
            ClientRuntime.Builder runtime =
                PipelineFixtures.engineRuntime(PipelineFixtures.retries(scenario.maxAttempts()), driver.mode)
                                .addInterceptor(new ContextHooks(this::invoked));
            if (driver == Driver.ASYNC) {
                runtime.completionExecutor(Runnable::run)
                       .retryScheduler(delay -> CompletableFuture.completedFuture(null));
            }
            // The outcomes the predicted attempts consume; an unpredicted extra transmission fails the call.
            ScriptedHttp http = new ScriptedHttp();
            for (int c = 0; c < CALLS; c++) {
                for (Outcome outcome : scenario.outcomes().get(c).subList(0, predictedAttempts.get(c))) {
                    switch (outcome) {
                        case SUCCESS -> http.respond(200, "v");
                        case SERVICE_UNAVAILABLE -> http.respond(503, null);
                        case BAD_REQUEST -> http.respond(400, null);
                        case CONNECTION_RESET -> http.fail(HttpTransportException.create(
                            HttpPhase.CONNECT, HttpFailureCategory.Kind.IO, "Connection reset",
                            new IOException("reset")));
                    }
                }
            }
            AwsCallEngine.Builder builder = AwsCallEngine.builder().runtime(runtime.build());
            AwsCallEngine engine = (driver == Driver.SYNC ? builder.syncHttpClient(http)
                                                          : builder.asyncHttpClient(http)).build();
            PipelinePlan<Struct, Struct> plan = engine.runtime().plan(GET_THING);
            for (call = 0; call < CALLS; call++) {
                attemptsPerCall.add(0);
                observed.add(new Obs("call " + call + " succeeded", execute(engine, plan)));
            }
            for (int c = 0; c < CALLS; c++) {
                for (ContextKey<?> key : EXECUTION_KEYS) {
                    observed.add(new Obs("call " + c + " after the calls: " + key.name(),
                                         executions.get(c).get(key)));
                }
            }
            attempts.forEach((id, context) -> observed.add(
                new Obs("attempt " + id + " after the calls", access(context, c -> c.get(ATTEMPT_KEYS.get(0))))));
            Set<Object> distinct = Collections.newSetFromMap(new IdentityHashMap<>());
            distinct.addAll(attempts.values());
            distinct.addAll(executions);
            observed.add(new Obs("distinct contexts", distinct.size() == attempts.size() + CALLS));
        }

        private boolean execute(AwsCallEngine engine, PipelinePlan<Struct, Struct> plan) {
            Struct request = PipelineFixtures.request(GET_INPUT, "a");
            if (scenario.driver() == Driver.SYNC) {
                try {
                    engine.executeSync(plan, request, null, null);
                    return true;
                } catch (RuntimeException failure) {
                    return false;
                }
            }
            CompletableFuture<Struct> future = engine.executeAsync(plan, request, null, null);
            assertThat(future).as("inline async call of " + scenario).isDone();
            return !future.isCompletedExceptionally();
        }

        private void invoked(PipelineStage hook, HookContext context) {
            if (executions.size() == call) {
                executions.add(context.execution());
            }
            boolean same = context.execution() == executions.get(call);
            AttemptContext current = context instanceof HookContext.Attempt a ? a.attempt() : null;
            int attempt = 0;
            if (current != null) {
                attempt = current.attemptNumber();
                attemptsPerCall.set(call, Math.max(attemptsPerCall.get(call), attempt));
                AttemptContext first = attempts.putIfAbsent(List.of(call, attempt), current);
                same &= first == null || first == current;
            }
            List<List<Integer>> open = new ArrayList<>();
            attempts.forEach((id, retained) -> {
                if (retained.isOpen()) {
                    open.add(id);
                }
            });
            String id = id(call, hook, attempt);
            observed.add(new Obs(id, new Lifetime(open, same)));
            if (attempt > scenario.maxAttempts()) {
                return;
            }
            int position = attempt > 0 ? attempt : attemptsPerCall.get(call) + 1;
            List<Op> ops = scenario.ops(hook, call, attempt);
            for (int i = 0; i < ops.size(); i++) {
                observed.add(new Obs(id + " [" + i + "] " + ops.get(i), perform(ops.get(i), context, current,
                                                                               position)));
            }
        }

        private Object perform(Op op, HookContext context, AttemptContext current, int position) {
            try {
                Kind kind = op.kind();
                if (current == null && (kind == Kind.PUT_ATTEMPT || kind == Kind.GET_ATTEMPT)) {
                    kind = kind == Kind.PUT_ATTEMPT ? Kind.RETAINED_PUT : Kind.RETAINED_GET;
                }
                return switch (kind) {
                    case PUT_EXECUTION -> {
                        put(context.execution(), op.executionKey(), op.value());
                        yield OK;
                    }
                    case GET_EXECUTION -> current != null ? current.get(op.executionKey())
                                                          : context.execution().get(op.executionKey());
                    case PUT_ATTEMPT -> {
                        put(current, op.attemptKey(), op.value());
                        yield OK;
                    }
                    case GET_ATTEMPT -> current.get(op.attemptKey());
                    case RETAINED_GET -> {
                        ContextKey<?> key = op.kind() == Kind.RETAINED_GET ? ALL_KEYS.get(op.key()) : op.attemptKey();
                        yield retained(op, position, c -> c.get(key));
                    }
                    case RETAINED_PUT -> retained(op, position, c -> {
                        put(c, op.attemptKey(), op.value());
                        return OK;
                    });
                    case RETAINED_EXECUTION_GET -> {
                        AttemptContext target = target(op, position);
                        yield target == null ? NO_TARGET : target.execution().get(op.executionKey());
                    }
                };
            } catch (RuntimeException rejected) {
                return "rejected " + rejected.getClass().getSimpleName();
            }
        }

        private Object retained(Op op, int position, Function<AttemptContext, Object> access) {
            AttemptContext target = target(op, position);
            return target == null ? NO_TARGET : access(target, access);
        }

        private AttemptContext target(Op op, int position) {
            List<Integer> id = ContextLifetimePropertyTest.target(call, position, op.back(),
                                                                  call == 0 ? 0 : attemptsPerCall.get(0));
            return id == null ? null : attempts.get(id);
        }

        private static Retained access(AttemptContext context, Function<AttemptContext, Object> access) {
            boolean open = context.isOpen();
            try {
                return new Retained(open, "returned " + access.apply(context));
            } catch (RuntimeException rejected) {
                return new Retained(open, rejected.getClass().getSimpleName());
            }
        }

        private static <T> void put(ExecutionContext context, ContextKey<T> key, Object value) {
            context.put(key, key.cast(value));
        }

        private static <T> void put(AttemptContext context, ContextKey<T> key, Object value) {
            context.put(key, key.cast(value));
        }
    }

    /** One interceptor overriding all 16 hooks; reports each invocation with its context, changing nothing. */
    private static final class ContextHooks implements ExecutionInterceptor {
        private final BiConsumer<PipelineStage, HookContext> onHook;

        ContextHooks(BiConsumer<PipelineStage, HookContext> onHook) {
            this.onHook = onHook;
        }

        @Override
        public void beforeExecution(HookContext.BeforeExecution context) {
            onHook.accept(PipelineStage.BEFORE_EXECUTION, context);
        }

        @Override
        public SerializableStruct modifyRequest(HookContext.ModifyRequest context) {
            onHook.accept(PipelineStage.MODIFY_REQUEST, context);
            return context.request();
        }

        @Override
        public void beforeAttempt(HookContext.BeforeAttempt context) {
            onHook.accept(PipelineStage.BEFORE_ATTEMPT, context);
        }

        @Override
        public void afterSerialization(HookContext.AfterSerialization context) {
            onHook.accept(PipelineStage.AFTER_SERIALIZATION, context);
        }

        @Override
        public SdkHttpRequest modifyBeforeSigning(HookContext.ModifyBeforeSigning context) {
            onHook.accept(PipelineStage.MODIFY_BEFORE_SIGNING, context);
            return context.httpRequest();
        }

        @Override
        public SignedRequest modifyAfterSigning(HookContext.ModifyAfterSigning context) {
            onHook.accept(PipelineStage.MODIFY_AFTER_SIGNING, context);
            return context.signedRequest();
        }

        @Override
        public void beforeTransmission(HookContext.BeforeTransmission context) {
            onHook.accept(PipelineStage.BEFORE_TRANSMISSION, context);
        }

        @Override
        public void afterTransmission(HookContext.AfterTransmission context) {
            onHook.accept(PipelineStage.AFTER_TRANSMISSION, context);
        }

        @Override
        public SdkHttpResponse modifyHttpResponse(HookContext.ModifyHttpResponse context) {
            onHook.accept(PipelineStage.MODIFY_HTTP_RESPONSE, context);
            return context.httpResponse();
        }

        @Override
        public HttpBody modifyResponseBody(HookContext.ModifyResponseBody context) {
            onHook.accept(PipelineStage.MODIFY_RESPONSE_BODY, context);
            return context.body();
        }

        @Override
        public void beforeDeserialization(HookContext.BeforeDeserialization context) {
            onHook.accept(PipelineStage.BEFORE_DESERIALIZATION, context);
        }

        @Override
        public Throwable modifyAttemptException(HookContext.ModifyAttemptException context) {
            onHook.accept(PipelineStage.MODIFY_ATTEMPT_EXCEPTION, context);
            return context.failure();
        }

        @Override
        public void afterAttempt(HookContext.AfterAttempt context) {
            onHook.accept(PipelineStage.AFTER_ATTEMPT, context);
        }

        @Override
        public SerializableStruct modifyResponse(HookContext.ModifyResponse context) {
            onHook.accept(PipelineStage.MODIFY_RESPONSE, context);
            return context.response();
        }

        @Override
        public Throwable modifyException(HookContext.ModifyException context) {
            onHook.accept(PipelineStage.MODIFY_EXCEPTION, context);
            return context.failure();
        }

        @Override
        public void afterExecution(HookContext.AfterExecution context) {
            onHook.accept(PipelineStage.AFTER_EXECUTION, context);
        }
    }
}
