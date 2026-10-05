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
import static software.amazon.awssdk.pipeline.PipelineFixtures.GET_OUTPUT;
import static software.amazon.awssdk.pipeline.PipelineFixtures.GET_THING;
import static software.amazon.awssdk.pipeline.PipelineFixtures.PUT_THING;
import static software.amazon.awssdk.pipeline.PipelineFixtures.firstToken;

import java.lang.management.ManagementFactory;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;
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
import net.jqwik.api.lifecycle.AddLifecycleHook;
import net.jqwik.api.lifecycle.LifecycleContext;
import net.jqwik.api.lifecycle.SkipExecutionHook;
import net.jqwik.api.statistics.Statistics;
import software.amazon.awssdk.auth.api.SignedRequest;
import software.amazon.awssdk.core.exception.SdkServiceException;
import software.amazon.awssdk.http.HttpBody;
import software.amazon.awssdk.http.SdkHttpRequest;
import software.amazon.awssdk.http.SdkHttpResponse;
import software.amazon.awssdk.pipeline.PipelineFixtures.CountingProtocol;
import software.amazon.awssdk.pipeline.PipelineFixtures.Driver;
import software.amazon.awssdk.pipeline.PipelineFixtures.ManualExecutor;
import software.amazon.awssdk.pipeline.PipelineFixtures.ScriptedHttp;
import software.amazon.awssdk.pipeline.PipelineFixtures.Struct;
import software.amazon.awssdk.retries.BackoffStrategy;
import software.amazon.awssdk.retries.RetryScheduler;
import software.amazon.awssdk.retries.RetryStrategy;
import software.amazon.awssdk.retries.StandardRetryStrategy;
import software.amazon.smithy.java.core.schema.SerializableStruct;
import software.amazon.smithy.model.shapes.ShapeId;

/**
 * Property 33: Inactive extensions cost no extension work (Requirements 10.10, 20.21, 21.5).
 *
 * <p>For any call with no applicable interceptor or telemetry publisher, extension callbacks, extension-only timers
 * and allocations whose sole purpose is extension dispatch are zero. Two properties, each with a positive control so
 * the instrumentation cannot pass vacuously:
 *
 * <ul>
 *   <li>{@link #inactiveExtensionsDoNoWorkOnCalls}: one real call per try through either driver, with a generated
 *       outcome script (success, a retryable 503, a non-retryable 400; maxAttempts 1-4) and generated <em>inert</em>
 *       extensions: interceptors that implement no hook, plugins whose {@code appliesTo} is false for the operation
 *       (they would activate telemetry, metadata and every hook if consulted further), a plugin that applies but
 *       contributes nothing, and recording interceptors on exactly the hooks the scripted path never reaches. Half the
 *       tries add <em>active</em> extensions: recording interceptors on any hook subset, an applicable plugin
 *       activating the response-metadata and telemetry stages, and a retry backoff. The zero oracle: the
 *       {@link StageObserver} reports no hook or feature stage beyond what the active extensions predict, the
 *       plan's dispatch is {@link InterceptorDispatch#NONE} when no interceptor is registered, no inert interceptor
 *       is called, inapplicable plugins are consulted once at plan time and never configured, and without backoff
 *       the retry scheduler is never asked for a timer. The positive control: each active recording interceptor is
 *       called, each activated stage is entered and each backoff timer is requested exactly as often as a reference
 *       count of the scripted path predicts.</li>
 *   <li>{@link #inactiveHookDispatchAllocatesNothing}: for a plan built with any generated interceptor
 *       configuration, every dispatch method of a hook no interceptor implements allocates nothing, measured as the
 *       calling thread's allocated bytes over {@value #ITERATIONS} invocations of the dispatch method alone (bound:
 *       less than one byte per invocation; any object is at least 16). The control: every dispatch method of an
 *       implemented hook, whose interceptors retain each {@link HookContext} so it escapes, allocates at least one
 *       object per invocation and calls each implementer once per invocation. Skipped when the JVM cannot measure
 *       per-thread allocation.</li>
 * </ul>
 *
 * <p>Everything is deterministic: the async driver completes on a {@link ManualExecutor} drained on the test thread,
 * backoff timers complete at once, and allocation is a byte count, never a timing. JIT escape analysis only removes
 * allocations, so it cannot make the zero bound flaky, and the control's retained contexts cannot be removed. An
 * interceptor that implements no hook still makes the plan's dispatch a non-{@link InterceptorDispatch#NONE}
 * instance (built once per plan), so for it the oracle is an empty set of active hooks and zero per-call
 * allocation, not the {@code NONE} identity. A generated "interceptor implementing a hook set" in the allocation
 * property is modeled as one single-hook interceptor per hook in the set, which yields the same per-hook dispatch
 * arrays.
 */
class InactiveExtensionPropertyTest {
    /** Dispatch invocations measured per hook and try. */
    private static final int ITERATIONS = 1_000;
    private static final int WARMUP = 200;
    /** The smallest heap object HotSpot allocates, in bytes. */
    private static final long MIN_OBJECT_BYTES = 16;
    private static final Set<PipelineStage> FEATURE_STAGES =
        PipelineArbitraries.enumSet(Set.of(PipelineStage.RESPONSE_METADATA, PipelineStage.TELEMETRY));
    private static final Set<PipelineStage> HOOK_STAGES = PipelineArbitraries.HOOK_STAGES;
    private static final Set<PipelineStage> CORE_STAGES = coreStages();

    /** The scripted outcome of one attempt. */
    enum Outcome {
        SUCCESS(200),
        /** Retryable. */
        UNAVAILABLE(503),
        /** Not retryable. */
        BAD_REQUEST(400);

        final int status;

        Outcome(int status) {
            this.status = status;
        }
    }

    /** The reference count of a scripted path: how many attempts ran, and whether the call succeeded. */
    record Path(int attempts, boolean succeeded) {
        static Path of(List<Outcome> outcomes) {
            for (int i = 0; i < outcomes.size(); i++) {
                Outcome outcome = outcomes.get(i);
                if (outcome == Outcome.SUCCESS) {
                    return new Path(i + 1, true);
                }
                if (outcome == Outcome.BAD_REQUEST) {
                    return new Path(i + 1, false);
                }
            }
            // Every attempt was retryable and maxAttempts (one per outcome) is exhausted.
            return new Path(outcomes.size(), false);
        }

        int failedAttempts() {
            return attempts - (succeeded ? 1 : 0);
        }

        int retries() {
            return attempts - 1;
        }

        /**
         * How many times a call on this path reaches the hook or feature stage point {@code stage}, from the design's
         * lifecycle: the execution hooks and telemetry once, every attempt's hooks and response metadata per attempt
         * (each scripted attempt receives an HTTP response), {@code modifyAttemptException} per failed attempt, and
         * {@code modifyResponse} or {@code modifyException} once by terminal outcome.
         */
        int occurrences(PipelineStage stage) {
            return switch (stage) {
                case BEFORE_EXECUTION, MODIFY_REQUEST, AFTER_EXECUTION, TELEMETRY -> 1;
                case BEFORE_ATTEMPT, AFTER_SERIALIZATION, MODIFY_BEFORE_SIGNING, MODIFY_AFTER_SIGNING,
                     BEFORE_TRANSMISSION, AFTER_TRANSMISSION, MODIFY_HTTP_RESPONSE, MODIFY_RESPONSE_BODY,
                     BEFORE_DESERIALIZATION, RESPONSE_METADATA, AFTER_ATTEMPT -> attempts;
                case MODIFY_ATTEMPT_EXCEPTION -> failedAttempts();
                case MODIFY_RESPONSE -> succeeded ? 1 : 0;
                case MODIFY_EXCEPTION -> succeeded ? 0 : 1;
                default -> throw new IllegalArgumentException(stage + " is not modeled");
            };
        }

        /** The hooks this path never reaches; never empty (one of the terminal modify hooks is always off path). */
        Set<PipelineStage> offPathHooks() {
            EnumSet<PipelineStage> off = EnumSet.noneOf(PipelineStage.class);
            HOOK_STAGES.stream().filter(hook -> occurrences(hook) == 0).forEach(off::add);
            return Collections.unmodifiableSet(off);
        }
    }

    /**
     * Extensions that must cost nothing on the call.
     *
     * @param hookless interceptors implementing no hook
     * @param inapplicable plugins whose {@code appliesTo} is false for the called operation
     * @param idlePlugin whether a plugin applies but contributes nothing
     * @param offPath whether recording interceptors are attached to every hook the path never reaches
     */
    record Inert(int hookless, int inapplicable, boolean idlePlugin, boolean offPath) {
    }

    /**
     * Extensions that do work (the positive control).
     *
     * @param hooks the hooks with one recording interceptor each
     * @param features the feature stages an applicable plugin activates; no such plugin when empty
     * @param backoff whether retries wait a (1 ms) backoff
     */
    record Active(Set<PipelineStage> hooks, Set<PipelineStage> features, boolean backoff) {
        static final Active NONE = new Active(Set.of(), Set.of(), false);

        boolean none() {
            return hooks.isEmpty() && features.isEmpty() && !backoff;
        }
    }

    /** One generated call; maxAttempts is the number of outcomes. */
    record Scenario(Driver driver, List<Outcome> outcomes, Inert inert, Active active) {
        Path path() {
            return Path.of(outcomes);
        }

        Set<PipelineStage> offPathHooks() {
            return inert.offPath() ? path().offPathHooks() : Set.of();
        }

        int registeredInterceptors() {
            return inert.hookless() + active.hooks().size() + offPathHooks().size();
        }
    }

    // Feature: aws-sdk-java-v3, Property 33: Inactive extensions cost no extension work
    // Embedded edge cases are disabled: they are mostly the first value of each generator. The coverage checks below
    // fail the property if inactive-only calls, active controls, any inert kind, retries or failures stop being
    // generated.
    @Property(tries = 500, edgeCases = EdgeCasesMode.NONE)
    @Label("Property 33: inactive extensions do no callback, stage or timer work on calls")
    void inactiveExtensionsDoNoWorkOnCalls(@ForAll("scenarios") Scenario scenario) {
        Path path = scenario.path();
        CallRun run = new CallRun(scenario);
        boolean succeeded = run.call();
        String description = scenario + " on " + path;

        // The call followed the scripted path.
        assertThat(succeeded).as("succeeded: " + description).isEqualTo(path.succeeded());
        assertThat(run.http.transmissions).as("transmissions: " + description).isEqualTo(path.attempts());

        // Plan-time shape: nothing inert is dispatched, activated or configured.
        PipelinePlan<Struct, Struct> plan = run.plan;
        Set<PipelineStage> expectedHooks = union(scenario.active().hooks(), scenario.offPathHooks());
        assertThat(plan.interceptors()).as("interceptors: " + description)
                                       .hasSize(scenario.registeredInterceptors());
        if (scenario.registeredInterceptors() == 0) {
            assertThat(plan.dispatch()).as("dispatch: " + description).isSameAs(InterceptorDispatch.NONE);
        }
        assertThat(plan.dispatch().activeStages()).as("active hooks: " + description).isEqualTo(expectedHooks);
        assertThat(plan.stages().activeStages()).as("active stages: " + description)
                                                .isEqualTo(union(CORE_STAGES, expectedHooks,
                                                                 scenario.active().features()));
        List<ShapeId> contributors = new ArrayList<>();
        if (scenario.inert().idlePlugin()) {
            contributors.add(CountingPlugin.IDLE);
        }
        if (!scenario.active().features().isEmpty()) {
            contributors.add(CountingPlugin.FEATURES);
        }
        assertThat(plan.plugins()).as("contributing plugins: " + description).isEqualTo(contributors);
        for (CountingPlugin plugin : run.plugins) {
            assertThat(plugin.consulted).as("consultations of " + plugin + ": " + description).hasValue(1);
            assertThat(plugin.configured).as("configurations of " + plugin + ": " + description)
                                         .hasValue(plugin.applies ? 1 : 0);
        }

        // Per-call work: exactly what the active extensions predict, and nothing for the inert ones.
        Map<PipelineStage, Integer> expectedEntered = new EnumMap<>(PipelineStage.class);
        for (PipelineStage stage : union(scenario.active().hooks(), scenario.active().features())) {
            if (path.occurrences(stage) > 0) {
                expectedEntered.put(stage, path.occurrences(stage));
            }
        }
        assertThat(run.enteredExtensionStages()).as("extension stages entered: " + description)
                                                .isEqualTo(expectedEntered);
        for (Recorder recorder : run.recorders) {
            int expected = recorder.offPath ? 0 : path.occurrences(recorder.hook);
            assertThat(recorder.calls).as("calls of " + recorder + ": " + description).hasValue(expected);
        }
        int expectedTimers = scenario.driver() == Driver.ASYNC && scenario.active().backoff() ? path.retries() : 0;
        assertThat(run.scheduler.delays).as("retry timers: " + description).hasSize(expectedTimers)
                                        .allSatisfy(d -> assertThat(d).isEqualTo(Duration.ofMillis(1)));

        collectStatistics(scenario, path, run);
    }

    private static void collectStatistics(Scenario scenario, Path path, CallRun run) {
        Statistics.label("driver").collect(scenario.driver());
        Statistics.label("extensions").collect(scenario.active().none() ? "inactive only" : "with active control");
        Statistics.label("extensions").coverage(c -> {
            c.check("inactive only").percentage(p -> p >= 30);
            c.check("with active control").percentage(p -> p >= 30);
        });
        Inert inert = scenario.inert();
        boolean nothing = scenario.registeredInterceptors() == 0 && inert.inapplicable() == 0 && !inert.idlePlugin()
                          && scenario.active().none();
        Statistics.label("no extension at all").collect(nothing);
        Statistics.label("no extension at all").coverage(c -> c.check(true).percentage(p -> p >= 5));
        Statistics.label("hookless interceptor").collect(inert.hookless() > 0);
        Statistics.label("hookless interceptor").coverage(c -> c.check(true).percentage(p -> p >= 20));
        Statistics.label("inapplicable plugin").collect(inert.inapplicable() > 0);
        Statistics.label("inapplicable plugin").coverage(c -> c.check(true).percentage(p -> p >= 20));
        Statistics.label("idle plugin").collect(inert.idlePlugin());
        Statistics.label("idle plugin").coverage(c -> c.check(true).percentage(p -> p >= 10));
        Statistics.label("off-path interceptors").collect(inert.offPath());
        Statistics.label("off-path interceptors").coverage(c -> c.check(true).percentage(p -> p >= 20));
        Statistics.label("status").collect(path.succeeded() ? "succeeded" : "failed");
        Statistics.label("status").coverage(c -> {
            c.check("succeeded").percentage(p -> p >= 20);
            c.check("failed").percentage(p -> p >= 15);
        });
        Statistics.label("retried").collect(path.retries() > 0);
        Statistics.label("retried").coverage(c -> c.check(true).percentage(p -> p >= 20));
        Statistics.label("async backoff timers").collect(!run.scheduler.delays.isEmpty());
        Statistics.label("async backoff timers").coverage(c -> c.check(true).percentage(p -> p >= 3));
    }

    @Provide
    Arbitrary<Scenario> scenarios() {
        Arbitrary<Outcome> outcome = Arbitraries.frequency(
            Tuple.of(3, Outcome.SUCCESS), Tuple.of(3, Outcome.UNAVAILABLE), Tuple.of(1, Outcome.BAD_REQUEST));
        Arbitrary<Inert> inert = Combinators.combine(
            Arbitraries.frequency(Tuple.of(3, 0), Tuple.of(1, 1), Tuple.of(1, 2)),
            Arbitraries.frequency(Tuple.of(3, 0), Tuple.of(1, 1), Tuple.of(1, 2)),
            Arbitraries.frequency(Tuple.of(3, false), Tuple.of(1, true)),
            Arbitraries.frequency(Tuple.of(2, false), Tuple.of(1, true)))
            .as(Inert::new);
        Arbitrary<Active> active = Combinators.combine(
            PipelineArbitraries.hookSubsets(),
            Arbitraries.subsetOf(FEATURE_STAGES).map(PipelineArbitraries::enumSet),
            // Backoff is weighted 3:1: timers need an active control, backoff, the async driver and a retry, which
            // at 1:1 sat just above the 3% coverage minimum. Inactive scenarios never back off either way.
            Arbitraries.frequency(Tuple.of(3, true), Tuple.of(1, false)))
            .as(Active::new);
        return Combinators.combine(
            PipelineArbitraries.drivers(),
            outcome.list().ofMinSize(1).ofMaxSize(4),
            inert,
            Arbitraries.frequencyOf(Tuple.of(1, Arbitraries.just(Active.NONE)), Tuple.of(1, active)))
            .as(Scenario::new);
    }

    /**
     * A plan built with any interceptor configuration and inapplicable plugins.
     *
     * @param interceptors per generated interceptor, the hooks it implements (empty: it implements none)
     */
    record DispatchScenario(List<Set<PipelineStage>> interceptors, int inapplicable) {
        Set<PipelineStage> implemented() {
            EnumSet<PipelineStage> hooks = EnumSet.noneOf(PipelineStage.class);
            interceptors.forEach(hooks::addAll);
            return hooks;
        }

        int implementers(PipelineStage hook) {
            return (int) interceptors.stream().filter(hooks -> hooks.contains(hook)).count();
        }
    }

    // Feature: aws-sdk-java-v3, Property 33: Inactive extensions cost no extension work
    @Property(tries = 100, edgeCases = EdgeCasesMode.NONE)
    @Label("Property 33: dispatching a hook no interceptor implements allocates nothing")
    @AddLifecycleHook(RequiresAllocationMeasurement.class)
    void inactiveHookDispatchAllocatesNothing(@ForAll("dispatchScenarios") DispatchScenario scenario) {
        com.sun.management.ThreadMXBean threads = RequiresAllocationMeasurement.threads();
        List<Recorder> recorders = new ArrayList<>();
        List<CountingPlugin> plugins = new ArrayList<>();
        ClientRuntime.Builder runtime = PipelineFixtures.runtimeBuilder(new CountingProtocol());
        for (Set<PipelineStage> hooks : scenario.interceptors()) {
            if (hooks.isEmpty()) {
                runtime.addInterceptor(new HooklessInterceptor());
            }
            for (PipelineStage hook : hooks) {
                Recorder recorder = new Recorder(hook, false);
                recorders.add(recorder);
                runtime.addInterceptor(recorder.interceptor);
            }
        }
        for (int i = 0; i < scenario.inapplicable(); i++) {
            CountingPlugin plugin = CountingPlugin.inapplicable(i);
            plugins.add(plugin);
            runtime.addPlugin(plugin);
        }
        PipelinePlan<Struct, Struct> plan = runtime.build().plan(GET_THING);
        InterceptorDispatch dispatch = plan.dispatch();
        String description = scenario.toString();

        Set<PipelineStage> implemented = scenario.implemented();
        if (scenario.interceptors().isEmpty()) {
            assertThat(dispatch).as(description).isSameAs(InterceptorDispatch.NONE);
        }
        assertThat(dispatch.activeStages()).as(description).isEqualTo(implemented);
        assertThat(plugins).allSatisfy(plugin -> assertThat(plugin.configured).as(description).hasValue(0));

        DispatchFixture fixture = new DispatchFixture(plan);
        for (PipelineStage hook : HOOK_STAGES) {
            long callsBefore = calls(recorders);
            long bytes = allocatedBytes(threads, () -> fixture.drive(hook, dispatch));
            long calls = calls(recorders) - callsBefore;
            String hookDescription = hook + " of " + description;
            if (implemented.contains(hook)) {
                // Positive control: the measurement sees each retained context, and each implementer runs.
                assertThat(bytes).as("bytes of %s active dispatches of %s", ITERATIONS, hookDescription)
                                 .isGreaterThanOrEqualTo(MIN_OBJECT_BYTES * ITERATIONS);
                assertThat(calls).as("callbacks of " + hookDescription)
                                 .isEqualTo((long) (WARMUP + ITERATIONS) * DispatchFixture.methods(hook)
                                            * scenario.implementers(hook));
            } else {
                assertThat(bytes).as("bytes of %s inactive dispatches of %s", ITERATIONS, hookDescription)
                                 .isLessThan(ITERATIONS);
                assertThat(calls).as("callbacks of " + hookDescription).isZero();
            }
        }

        Statistics.label("interceptors").collect(scenario.interceptors().size());
        String shape = scenario.interceptors().isEmpty() ? "none"
                       : implemented.isEmpty() ? "hookless only"
                       : implemented.equals(HOOK_STAGES) ? "every hook" : "some hooks";
        Statistics.label("implemented hooks").collect(shape);
        Statistics.label("implemented hooks").coverage(c -> {
            c.check("none").percentage(p -> p >= 10);
            c.check("some hooks").percentage(p -> p >= 30);
        });
    }

    @Provide
    Arbitrary<DispatchScenario> dispatchScenarios() {
        Arbitrary<List<Set<PipelineStage>>> some = PipelineArbitraries.hookSubsets().list().ofMinSize(1).ofMaxSize(3);
        return Combinators.combine(
            Arbitraries.frequencyOf(Tuple.of(1, Arbitraries.just(List.<Set<PipelineStage>>of())), Tuple.of(4, some)),
            Arbitraries.integers().between(0, 2))
            .as(DispatchScenario::new);
    }

    /** Runs {@code action} {@value #WARMUP} times, then returns the bytes {@value #ITERATIONS} more runs allocate. */
    private static long allocatedBytes(com.sun.management.ThreadMXBean threads, Runnable action) {
        for (int i = 0; i < WARMUP; i++) {
            action.run();
        }
        long before = threads.getCurrentThreadAllocatedBytes();
        for (int i = 0; i < ITERATIONS; i++) {
            action.run();
        }
        return threads.getCurrentThreadAllocatedBytes() - before;
    }

    private static long calls(List<Recorder> recorders) {
        return recorders.stream().mapToLong(recorder -> recorder.calls.get()).sum();
    }

    @SafeVarargs
    private static Set<PipelineStage> union(Set<PipelineStage>... sets) {
        EnumSet<PipelineStage> union = EnumSet.noneOf(PipelineStage.class);
        for (Set<PipelineStage> set : sets) {
            union.addAll(set);
        }
        return Collections.unmodifiableSet(union);
    }

    private static Set<PipelineStage> coreStages() {
        EnumSet<PipelineStage> core = EnumSet.noneOf(PipelineStage.class);
        for (PipelineStage stage : PipelineStage.values()) {
            if (stage.kind() == PipelineStage.Kind.CORE) {
                core.add(stage);
            }
        }
        return Collections.unmodifiableSet(core);
    }

    /** One call of a scenario, with every extension instrumented. */
    private static final class CallRun {
        final ScriptedHttp http = new ScriptedHttp();
        final CountingScheduler scheduler = new CountingScheduler();
        final List<Recorder> recorders = new ArrayList<>();
        final List<CountingPlugin> plugins = new ArrayList<>();
        final PipelinePlan<Struct, Struct> plan;
        private final Scenario scenario;
        private final ManualExecutor executor = new ManualExecutor();
        private final List<PipelineStage> entered = new ArrayList<>();
        private final AwsCallEngine engine;

        CallRun(Scenario scenario) {
            this.scenario = scenario;
            Active active = scenario.active();
            int maxAttempts = scenario.outcomes().size();
            RetryStrategy strategy = active.backoff() ? backoff(maxAttempts) : PipelineFixtures.retries(maxAttempts);
            ClientRuntime.Builder runtime = PipelineFixtures.engineRuntime(strategy, scenario.driver().mode)
                                                            .retryScheduler(scheduler);
            if (scenario.driver() == Driver.ASYNC) {
                runtime.completionExecutor(executor);
            }
            for (int i = 0; i < scenario.inert().hookless(); i++) {
                runtime.addInterceptor(new HooklessInterceptor());
            }
            active.hooks().forEach(hook -> register(runtime, new Recorder(hook, false)));
            scenario.offPathHooks().forEach(hook -> register(runtime, new Recorder(hook, true)));
            for (int i = 0; i < scenario.inert().inapplicable(); i++) {
                plugins.add(CountingPlugin.inapplicable(i));
            }
            if (scenario.inert().idlePlugin()) {
                plugins.add(new CountingPlugin(CountingPlugin.IDLE, true, Set.of()));
            }
            if (!active.features().isEmpty()) {
                plugins.add(new CountingPlugin(CountingPlugin.FEATURES, true, active.features()));
            }
            plugins.forEach(runtime::addPlugin);
            AwsCallEngine.Builder builder = AwsCallEngine.builder().runtime(runtime.build())
                                                         .stageObserver((call, attempt, stage) -> entered.add(stage));
            engine = (scenario.driver() == Driver.SYNC ? builder.syncHttpClient(http) : builder.asyncHttpClient(http))
                .build();
            plan = engine.runtime().plan(GET_THING);
        }

        private void register(ClientRuntime.Builder runtime, Recorder recorder) {
            recorders.add(recorder);
            runtime.addInterceptor(recorder.interceptor);
        }

        /** Runs the call; whether it succeeded. */
        boolean call() {
            for (Outcome outcome : scenario.outcomes()) {
                http.respond(outcome.status, outcome == Outcome.SUCCESS ? "v" : null);
            }
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
            executor.runAll();
            assertThat(future).as("async call of " + scenario + " after draining its executor").isDone();
            return !future.isCompletedExceptionally();
        }

        /** How many times each non-core stage point was entered. */
        Map<PipelineStage, Integer> enteredExtensionStages() {
            Map<PipelineStage, Integer> counts = new EnumMap<>(PipelineStage.class);
            entered.stream().filter(stage -> stage.kind() != PipelineStage.Kind.CORE)
                   .forEach(stage -> counts.merge(stage, 1, Integer::sum));
            return counts;
        }

        private static RetryStrategy backoff(int maxAttempts) {
            return StandardRetryStrategy.builder()
                                        .maxAttempts(maxAttempts)
                                        .backoffStrategy(BackoffStrategy.fixedDelay(Duration.ofMillis(1)))
                                        .throttlingBackoffStrategy(BackoffStrategy.none())
                                        .build();
        }
    }

    /** An interceptor implementing exactly one hook, counting its calls and retaining each context it receives. */
    private static final class Recorder {
        final PipelineStage hook;
        final boolean offPath;
        final AtomicInteger calls = new AtomicInteger();
        final ExecutionInterceptor interceptor;
        volatile Object last;

        Recorder(PipelineStage hook, boolean offPath) {
            this.hook = hook;
            this.offPath = offPath;
            this.interceptor = hookInterceptor(hook, context -> {
                last = context;
                calls.incrementAndGet();
            });
        }

        @Override
        public String toString() {
            return "Recorder[" + hook + (offPath ? ", off path" : "") + "]";
        }
    }

    /** Implements no hook. */
    private static final class HooklessInterceptor implements ExecutionInterceptor {
        @Override
        public String toString() {
            return "HooklessInterceptor";
        }
    }

    /**
     * A plugin counting its plan-time consultations and configurations. When configured it activates
     * {@code stages}; an inapplicable one would also activate every feature stage and contribute an interceptor on
     * every hook, so a wrongful configuration shows in the plan, the stage trace and its configuration count.
     */
    private static final class CountingPlugin implements AwsFeaturePlugin {
        static final ShapeId IDLE = ShapeId.from("test#Idle");
        static final ShapeId FEATURES = ShapeId.from("test#Features");

        final AtomicInteger consulted = new AtomicInteger();
        final AtomicInteger configured = new AtomicInteger();
        final boolean applies;
        private final ShapeId id;
        private final Set<PipelineStage> stages;
        private final List<ExecutionInterceptor> interceptors = new ArrayList<>();

        CountingPlugin(ShapeId id, boolean applies, Set<PipelineStage> stages) {
            this.id = id;
            this.applies = applies;
            this.stages = stages;
        }

        /** Applies only to another operation than the one called. */
        static CountingPlugin inapplicable(int index) {
            ShapeId id = ShapeId.from("test#Inapplicable" + index);
            CountingPlugin plugin = new CountingPlugin(id, false, FEATURE_STAGES);
            HOOK_STAGES.forEach(hook -> plugin.interceptors.add(hookInterceptor(hook, context -> {
                throw new AssertionError("An inapplicable plugin's interceptor ran at " + hook);
            })));
            return plugin;
        }

        @Override
        public ShapeId id() {
            return id;
        }

        @Override
        public boolean appliesTo(AwsOperation<?, ?> operation) {
            consulted.incrementAndGet();
            return applies ? operation.id().equals(GET_THING.id()) : operation.id().equals(PUT_THING.id());
        }

        @Override
        public void configure(PipelinePlan.Builder<?, ?> plan) {
            configured.incrementAndGet();
            stages.forEach(plan::activate);
            interceptors.forEach(plan::addInterceptor);
        }

        @Override
        public String toString() {
            return "CountingPlugin[" + id + "]";
        }
    }

    /** Records every requested delay; each completes at once. */
    private static final class CountingScheduler implements RetryScheduler {
        final List<Duration> delays = new CopyOnWriteArrayList<>();

        @Override
        public CompletableFuture<Void> delay(Duration delay) {
            delays.add(delay);
            return CompletableFuture.completedFuture(null);
        }
    }

    /** Skips the allocation property when the JVM cannot measure the current thread's allocated bytes. */
    static final class RequiresAllocationMeasurement implements SkipExecutionHook {
        static com.sun.management.ThreadMXBean threads() {
            return (com.sun.management.ThreadMXBean) ManagementFactory.getThreadMXBean();
        }

        @Override
        public SkipResult shouldBeSkipped(LifecycleContext context) {
            if (!(ManagementFactory.getThreadMXBean() instanceof com.sun.management.ThreadMXBean threads)) {
                return SkipResult.skip("The JVM does not expose per-thread allocated bytes");
            }
            if (!threads.isThreadAllocatedMemorySupported() || !threads.isThreadAllocatedMemoryEnabled()) {
                return SkipResult.skip("Per-thread allocation measurement is unavailable");
            }
            return SkipResult.doNotSkip();
        }
    }

    /** Real call and attempt state of a plan, and one value of each hook's type, created once. */
    private static final class DispatchFixture {
        private final CallState<Struct, Struct> call;
        private final AttemptState attempt;
        private final Struct output = PipelineFixtures.request(GET_OUTPUT, "v");
        private final SdkHttpRequest request = SdkHttpRequest.builder().method("POST").scheme("https")
                                                             .host("things.example.com").build();
        private final SignedRequest signed = SignedRequest.unsigned(request);
        private final SdkHttpResponse response = SdkHttpResponse.builder().statusCode(200).body(HttpBody.empty())
                                                                .build();
        private final RuntimeException failure = SdkServiceException.builder().statusCode(503).build();
        private Object sink;

        DispatchFixture(PipelinePlan<Struct, Struct> plan) {
            call = plan.newCall(PipelineFixtures.request(GET_INPUT, "a"), plan.runtime().callOptions());
            attempt = call.startAttempt(firstToken(plan));
            attempt.unsignedRequest(request);
            attempt.signedRequest(signed);
            attempt.response(response);
        }

        /** How many dispatch methods {@link #drive} calls for {@code hook}. */
        static int methods(PipelineStage hook) {
            return switch (hook) {
                case AFTER_TRANSMISSION, AFTER_ATTEMPT, AFTER_EXECUTION -> 2;
                default -> 1;
            };
        }

        /** Every dispatch method of {@code hook}. */
        void drive(PipelineStage hook, InterceptorDispatch dispatch) {
            switch (hook) {
                case BEFORE_EXECUTION -> dispatch.beforeExecution(call);
                case MODIFY_REQUEST -> sink = dispatch.modifyRequest(call);
                case BEFORE_ATTEMPT -> dispatch.beforeAttempt(call, attempt);
                case AFTER_SERIALIZATION -> dispatch.afterSerialization(call, attempt, request);
                case MODIFY_BEFORE_SIGNING -> sink = dispatch.modifyBeforeSigning(call, attempt, request);
                case MODIFY_AFTER_SIGNING -> sink = dispatch.modifyAfterSigning(call, attempt, signed);
                case BEFORE_TRANSMISSION -> dispatch.beforeTransmission(call, attempt, signed);
                case AFTER_TRANSMISSION -> {
                    dispatch.afterTransmission(call, attempt, response);
                    sink = dispatch.afterTransmissionFailed(call, attempt, failure);
                }
                case MODIFY_HTTP_RESPONSE -> sink = dispatch.modifyHttpResponse(call, attempt, response);
                case MODIFY_RESPONSE_BODY -> sink = dispatch.modifyResponseBody(call, attempt, response);
                case BEFORE_DESERIALIZATION -> dispatch.beforeDeserialization(call, attempt, response);
                case MODIFY_ATTEMPT_EXCEPTION -> sink = dispatch.modifyAttemptException(call, attempt, failure);
                case AFTER_ATTEMPT -> {
                    dispatch.afterAttemptSucceeded(call, attempt, output);
                    sink = dispatch.afterAttemptFailed(call, attempt, failure);
                }
                case MODIFY_RESPONSE -> sink = dispatch.modifyResponse(call, output);
                case MODIFY_EXCEPTION -> sink = dispatch.modifyException(call, failure);
                case AFTER_EXECUTION -> {
                    dispatch.afterExecutionSucceeded(call, output);
                    dispatch.afterExecutionFailed(call, failure);
                }
                default -> throw new IllegalArgumentException(hook + " is not a hook stage");
            }
        }

        @Override
        public String toString() {
            return "DispatchFixture(" + sink + ")";
        }
    }

    /**
     * An interceptor that implements only {@code hook} (a {@link PipelineStage.Kind#HOOK} stage), passes each context
     * to {@code onCall} and returns every value unchanged. Each hook has its own class, so a plan activates exactly
     * the hook stages of the interceptors it is given. (A private copy of {@code PipelineFixtures.hookRecorder} that
     * hands the context over, so a caller can retain it.)
     */
    private static ExecutionInterceptor hookInterceptor(PipelineStage hook, Consumer<Object> onCall) {
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
}
