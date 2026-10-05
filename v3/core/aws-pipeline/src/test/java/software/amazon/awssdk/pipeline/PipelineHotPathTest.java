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
import static org.junit.jupiter.api.Assumptions.assumeTrue;
import static software.amazon.awssdk.pipeline.PipelineFixtures.GET_INPUT;
import static software.amazon.awssdk.pipeline.PipelineFixtures.GET_OUTPUT;
import static software.amazon.awssdk.pipeline.PipelineFixtures.GET_THING;
import static software.amazon.awssdk.pipeline.PipelineFixtures.firstToken;
import static software.amazon.awssdk.pipeline.PipelineFixtures.retries;

import java.lang.management.ManagementFactory;
import java.time.Duration;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import software.amazon.awssdk.auth.api.SignedRequest;
import software.amazon.awssdk.core.exception.SdkServiceException;
import software.amazon.awssdk.http.HttpBody;
import software.amazon.awssdk.http.SdkHttpRequest;
import software.amazon.awssdk.http.SdkHttpResponse;
import software.amazon.awssdk.pipeline.PipelineFixtures.Driver;
import software.amazon.awssdk.pipeline.PipelineFixtures.HeaderProtocol;
import software.amazon.awssdk.pipeline.PipelineFixtures.ScriptedHttp;
import software.amazon.awssdk.pipeline.PipelineFixtures.Struct;
import software.amazon.awssdk.protocols.core.ClientProtocol;
import software.amazon.awssdk.protocols.core.OperationProtocol;
import software.amazon.awssdk.retries.BackoffStrategy;
import software.amazon.awssdk.retries.RetryScheduler;
import software.amazon.awssdk.retries.RetryStrategy;
import software.amazon.awssdk.retries.StandardRetryStrategy;
import software.amazon.awssdk.smithy.runtime.schema.OperationSchemas;
import software.amazon.smithy.model.shapes.ShapeId;

/**
 * Hot-path enforcement for real calls through both drivers (task 9.6; Requirements 10.10, 10.11, 20.21, 20.22,
 * 21.5; Property 33). Every check is deterministic: counts, identities and per-thread allocated bytes against a bound
 * with a positive control, never timing.
 *
 * <ul>
 *   <li>No per-call plan construction: plugins, the protocol's operation planning and the runtime's plan cache are
 *       consulted once per client and operation, however many calls run.</li>
 *   <li>Without interceptors, plugins or modeled features, a plan's dispatch is {@link InterceptorDispatch#NONE}, its
 *       active stages are exactly the core stages, and calls enter no hook, telemetry or metadata stage.</li>
 *   <li>Without a retry delay, the async driver schedules no timer.</li>
 *   <li>An inactive hook allocates nothing: {@link InterceptorDispatch#NONE} creates no {@link HookContext}.</li>
 * </ul>
 *
 * The bytecode side of the same rules (one engine, no V2 types, no class-name lookup, reflection only at plan time,
 * no model parsing, no clocks or counters) is {@code PipelineArchitectureTest} in {@code v3/test/architecture-tests}.
 */
class PipelineHotPathTest {
    private static final int CALLS = 25;
    private static final Set<PipelineStage> CORE_STAGES = coreStages();

    private final ScriptedHttp http = new ScriptedHttp();
    private final List<PipelineStage> entered = new CopyOnWriteArrayList<>();

    @ParameterizedTest
    @EnumSource(Driver.class)
    void plansAreBuiltOncePerClientAndOperationAndNeverPerCall(Driver driver) {
        CountingPlugin plugin = new CountingPlugin();
        PlanCountingProtocol protocol = new PlanCountingProtocol();
        AwsOperation<Struct, Struct> other = PipelineFixtures.operation("com.example#GetOther", GET_INPUT, GET_OUTPUT);
        AwsCallEngine engine = engine(driver, runtime(driver, retries(1), protocol).addPlugin(plugin));
        ClientRuntime runtime = engine.runtime();
        PipelinePlan<Struct, Struct> first = runtime.plan(GET_THING);
        assertThat(plugin.planned).as("plugin consultations at plan time").hasValue(1);

        for (int i = 0; i < CALLS; i++) {
            http.respond(200, "v" + i).respond(200, "w" + i);
            // A generated client holds its plan in a final field; looking it up again must return the same plan.
            assertThat(runtime.plan(GET_THING)).isSameAs(first);
            assertThat(call(driver, engine, first)).isInstanceOf(Struct.class);
            assertThat(call(driver, engine, runtime.plan(other))).isInstanceOf(Struct.class);
        }

        assertThat(http.transmissions).isEqualTo(2 * CALLS);
        assertThat(runtime.builtPlans()).as("plans built").isEqualTo(2);
        assertThat(plugin.planned).as("plugin consultations").hasValue(2);
        assertThat(protocol.planned).as("protocol operation plans").hasValue(2);
    }

    @ParameterizedTest
    @EnumSource(Driver.class)
    void withoutExtensionsCallsEnterOnlyCoreStagesAndDispatchNothing(Driver driver) {
        AwsCallEngine engine = engine(driver, runtime(driver, retries(1), new HeaderProtocol()));
        PipelinePlan<Struct, Struct> plan = engine.runtime().plan(GET_THING);

        assertThat(plan.dispatch()).isSameAs(InterceptorDispatch.NONE);
        assertThat(plan.interceptors()).isEmpty();
        assertThat(plan.stages().activeStages()).isEqualTo(CORE_STAGES);

        for (int i = 0; i < CALLS; i++) {
            http.respond(200, "v");
            call(driver, engine, plan);
        }
        assertThat(entered).isNotEmpty()
                           .allSatisfy(stage -> assertThat(stage.kind()).isEqualTo(PipelineStage.Kind.CORE));
        assertThat(entered).doesNotContain(PipelineStage.TELEMETRY, PipelineStage.RESPONSE_METADATA);
        assertThat(engine.runtime().builtPlans()).isEqualTo(1);
    }

    @ParameterizedTest
    @EnumSource(Driver.class)
    void anInterceptorActivatesExactlyTheHooksItImplements(Driver driver) {
        // Positive control of the previous test: the observer reports a hook stage once it is active.
        AtomicInteger afterExecution = new AtomicInteger();
        ExecutionInterceptor interceptor = new ExecutionInterceptor() {
            @Override
            public void afterExecution(HookContext.AfterExecution context) {
                afterExecution.incrementAndGet();
            }
        };
        AwsCallEngine engine =
            engine(driver, runtime(driver, retries(1), new HeaderProtocol()).addInterceptor(interceptor));
        PipelinePlan<Struct, Struct> plan = engine.runtime().plan(GET_THING);
        assertThat(plan.dispatch()).isNotSameAs(InterceptorDispatch.NONE);
        assertThat(plan.dispatch().activeStages()).containsExactly(PipelineStage.AFTER_EXECUTION);

        http.respond(200, "v");
        call(driver, engine, plan);

        assertThat(afterExecution).hasValue(1);
        assertThat(entered).filteredOn(stage -> stage.kind() != PipelineStage.Kind.CORE)
                           .containsExactly(PipelineStage.AFTER_EXECUTION);
    }

    @Test
    void asyncCallsWithoutRetryDelayScheduleNoTimer() {
        CountingScheduler scheduler = new CountingScheduler();
        AwsCallEngine engine =
            engine(Driver.ASYNC, runtime(Driver.ASYNC, retries(3), new HeaderProtocol()).retryScheduler(scheduler));
        PipelinePlan<Struct, Struct> plan = engine.runtime().plan(GET_THING);

        for (int i = 0; i < CALLS; i++) {
            // A success, and a retried 503 whose token carries no backoff.
            http.respond(200, "v").respond(503, null).respond(200, "v");
            call(Driver.ASYNC, engine, plan);
            call(Driver.ASYNC, engine, plan);
        }

        assertThat(http.transmissions).isEqualTo(3 * CALLS);
        assertThat(scheduler.delays).as("scheduled retry delays").isEmpty();
    }

    @Test
    void asyncRetryWithBackoffSchedulesExactlyOneTimer() {
        // Positive control of the previous test: the counting scheduler sees a real delay.
        CountingScheduler scheduler = new CountingScheduler();
        RetryStrategy backoff = StandardRetryStrategy.builder()
                                                     .maxAttempts(2)
                                                     .backoffStrategy(BackoffStrategy.fixedDelay(Duration.ofMillis(1)))
                                                     .throttlingBackoffStrategy(BackoffStrategy.none())
                                                     .build();
        AwsCallEngine engine =
            engine(Driver.ASYNC, runtime(Driver.ASYNC, backoff, new HeaderProtocol()).retryScheduler(scheduler));
        http.respond(503, null).respond(200, "v");

        assertThat(call(Driver.ASYNC, engine, engine.runtime().plan(GET_THING))).isInstanceOf(Struct.class);
        assertThat(scheduler.delays).containsExactly(Duration.ofMillis(1));
    }

    /**
     * Every dispatch method of {@link InterceptorDispatch#NONE} allocates nothing (no {@link HookContext}, lambda,
     * array or iterator), measured as the calling thread's allocated bytes over many calls. Any allocation per
     * iteration is at least 16 bytes, so the bound of one byte per iteration leaves room only for the measurement's
     * own constant overhead. The positive control (an interceptor that retains each context) proves the measurement
     * sees hook contexts. JIT escape analysis can only remove allocations, never add them, so the zero result cannot
     * be flaky; the control retains its contexts so it cannot be optimized away.
     */
    @Test
    void inactiveHooksAllocateNothing() {
        com.sun.management.ThreadMXBean threads = allocationCounter();
        DispatchFixture fixture = new DispatchFixture();
        int iterations = 20_000;
        RetainingInterceptor retaining = new RetainingInterceptor();
        InterceptorDispatch active = InterceptorDispatch.of(List.of(retaining));
        assertThat(InterceptorDispatch.of(List.of())).isSameAs(InterceptorDispatch.NONE);

        // Warm-up: class initialization, linkage of lambdas and method references, first compilation.
        for (int i = 0; i < iterations; i++) {
            fixture.everyHook(InterceptorDispatch.NONE);
            fixture.observeHooks(active);
        }

        long before = threads.getCurrentThreadAllocatedBytes();
        for (int i = 0; i < iterations; i++) {
            fixture.everyHook(InterceptorDispatch.NONE);
        }
        long inactive = threads.getCurrentThreadAllocatedBytes() - before;

        before = threads.getCurrentThreadAllocatedBytes();
        for (int i = 0; i < iterations; i++) {
            fixture.observeHooks(active);
        }
        long control = threads.getCurrentThreadAllocatedBytes() - before;

        assertThat(inactive).as("bytes allocated by %s inactive dispatch iterations", iterations)
                            .isLessThan(iterations);
        assertThat(retaining.contexts).isEqualTo(2L * iterations * DispatchFixture.OBSERVE_HOOKS);
        assertThat(control).as("bytes allocated by %s active dispatch iterations", iterations)
                           .isGreaterThanOrEqualTo(16L * iterations * DispatchFixture.OBSERVE_HOOKS);
    }

    private static com.sun.management.ThreadMXBean allocationCounter() {
        boolean supported = ManagementFactory.getThreadMXBean() instanceof com.sun.management.ThreadMXBean;
        assumeTrue(supported, "The JVM does not expose per-thread allocated bytes");
        com.sun.management.ThreadMXBean threads = (com.sun.management.ThreadMXBean) ManagementFactory.getThreadMXBean();
        assumeTrue(threads.isThreadAllocatedMemorySupported() && threads.isThreadAllocatedMemoryEnabled(),
                   "Per-thread allocation measurement is unavailable");
        return threads;
    }

    private AwsCallEngine engine(Driver driver, ClientRuntime.Builder runtime) {
        AwsCallEngine.Builder builder = AwsCallEngine.builder().runtime(runtime.build())
                                                     .stageObserver((call, attempt, stage) -> entered.add(stage));
        return (driver == Driver.SYNC ? builder.syncHttpClient(http) : builder.asyncHttpClient(http)).build();
    }

    private static ClientRuntime.Builder runtime(Driver driver, RetryStrategy strategy, ClientProtocol protocol) {
        ClientRuntime.Builder runtime = ClientRuntime.builder()
            .configuration(PipelineFixtures.engineRuntime(strategy, driver.mode).build().configuration())
            .protocol(protocol)
            .transportMode(driver.mode)
            .retryStrategy(strategy, "things");
        if (driver == Driver.ASYNC) {
            runtime.completionExecutor(Runnable::run).retryScheduler(new CountingScheduler());
        }
        return runtime;
    }

    /** Runs one call; its output, or the failure it surfaced. */
    private static Object call(Driver driver, AwsCallEngine engine, PipelinePlan<Struct, Struct> plan) {
        Struct request = PipelineFixtures.request(GET_INPUT, "a");
        try {
            if (driver == Driver.SYNC) {
                return engine.executeSync(plan, request, null, null);
            }
            return engine.executeAsync(plan, request, null, null).get(5, TimeUnit.SECONDS);
        } catch (Exception failure) {
            return failure;
        }
    }

    private static Set<PipelineStage> coreStages() {
        Set<PipelineStage> core = EnumSet.noneOf(PipelineStage.class);
        for (PipelineStage stage : PipelineStage.values()) {
            if (stage.kind() == PipelineStage.Kind.CORE) {
                core.add(stage);
            }
        }
        return Collections.unmodifiableSet(core);
    }

    /** Real call and attempt state, and one value of each hook's type, created once. */
    private static final class DispatchFixture {
        static final int OBSERVE_HOOKS = 5;

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

        DispatchFixture() {
            PipelinePlan<Struct, Struct> plan = PipelineFixtures.runtime().plan(GET_THING);
            call = plan.newCall(PipelineFixtures.request(GET_INPUT, "a"), plan.runtime().callOptions());
            attempt = call.startAttempt(firstToken(plan));
        }

        /** Every dispatch method, in lifecycle order. */
        void everyHook(InterceptorDispatch dispatch) {
            dispatch.beforeExecution(call);
            sink = dispatch.modifyRequest(call);
            dispatch.beforeAttempt(call, attempt);
            dispatch.afterSerialization(call, attempt, request);
            sink = dispatch.modifyBeforeSigning(call, attempt, request);
            sink = dispatch.modifyAfterSigning(call, attempt, signed);
            dispatch.beforeTransmission(call, attempt, signed);
            dispatch.afterTransmission(call, attempt, response);
            sink = dispatch.afterTransmissionFailed(call, attempt, failure);
            sink = dispatch.modifyHttpResponse(call, attempt, response);
            sink = dispatch.modifyResponseBody(call, attempt, response);
            dispatch.beforeDeserialization(call, attempt, response);
            sink = dispatch.modifyAttemptException(call, attempt, failure);
            dispatch.afterAttemptSucceeded(call, attempt, output);
            sink = dispatch.afterAttemptFailed(call, attempt, failure);
            sink = dispatch.modifyResponse(call, output);
            sink = dispatch.modifyException(call, failure);
            dispatch.afterExecutionSucceeded(call, output);
            dispatch.afterExecutionFailed(call, failure);
        }

        /** The {@value #OBSERVE_HOOKS} observe-only hooks {@link RetainingInterceptor} implements. */
        void observeHooks(InterceptorDispatch dispatch) {
            dispatch.beforeExecution(call);
            dispatch.beforeAttempt(call, attempt);
            dispatch.afterSerialization(call, attempt, request);
            dispatch.beforeTransmission(call, attempt, signed);
            dispatch.beforeDeserialization(call, attempt, response);
        }

        @Override
        public String toString() {
            return "DispatchFixture(" + sink + ")";
        }
    }

    /** Retains the last context of each hook it implements, so the contexts escape and must be allocated. */
    private static final class RetainingInterceptor implements ExecutionInterceptor {
        volatile Object last;
        long contexts;

        @Override
        public void beforeExecution(HookContext.BeforeExecution context) {
            retain(context);
        }

        @Override
        public void beforeAttempt(HookContext.BeforeAttempt context) {
            retain(context);
        }

        @Override
        public void afterSerialization(HookContext.AfterSerialization context) {
            retain(context);
        }

        @Override
        public void beforeTransmission(HookContext.BeforeTransmission context) {
            retain(context);
        }

        @Override
        public void beforeDeserialization(HookContext.BeforeDeserialization context) {
            retain(context);
        }

        private void retain(Object context) {
            last = context;
            contexts++;
        }
    }

    /** Counts plan-time consultations; contributes nothing. */
    private static final class CountingPlugin implements AwsFeaturePlugin {
        final AtomicInteger planned = new AtomicInteger();

        @Override
        public ShapeId id() {
            return ShapeId.from("test#Counting");
        }

        @Override
        public boolean appliesTo(AwsOperation<?, ?> operation) {
            planned.incrementAndGet();
            return true;
        }

        @Override
        public void configure(PipelinePlan.Builder<?, ?> plan) {
        }
    }

    /** A {@link HeaderProtocol} that counts the operation plans it is asked for. */
    private static final class PlanCountingProtocol implements ClientProtocol {
        final AtomicInteger planned = new AtomicInteger();
        private final HeaderProtocol delegate = new HeaderProtocol();

        @Override
        public ShapeId protocolId() {
            return delegate.protocolId();
        }

        @Override
        public OperationProtocol operation(OperationSchemas operation) {
            planned.incrementAndGet();
            return delegate.operation(operation);
        }
    }

    /** Records every requested delay; delays complete at once. */
    private static final class CountingScheduler implements RetryScheduler {
        final List<Duration> delays = new CopyOnWriteArrayList<>();

        @Override
        public CompletableFuture<Void> delay(Duration delay) {
            delays.add(delay);
            return CompletableFuture.completedFuture(null);
        }
    }
}
