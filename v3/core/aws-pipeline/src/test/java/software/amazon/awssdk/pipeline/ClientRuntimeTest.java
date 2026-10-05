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
import static software.amazon.awssdk.pipeline.PipelineFixtures.GET_THING;
import static software.amazon.awssdk.pipeline.PipelineFixtures.PUT_THING;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.core.SdkRequestOverrideConfiguration;
import software.amazon.awssdk.core.client.config.SdkClientConfiguration;
import software.amazon.awssdk.core.context.ContextKey;
import software.amazon.awssdk.core.resource.ManagedResource;
import software.amazon.awssdk.http.SdkHttpRequest;
import software.amazon.awssdk.http.SdkHttpResponse;
import software.amazon.awssdk.identity.spi.Identity;
import software.amazon.awssdk.pipeline.PipelineFixtures.CountingProtocol;
import software.amazon.awssdk.pipeline.feature.IdempotencyTokenFeature;
import software.amazon.smithy.java.core.schema.SerializableStruct;
import software.amazon.smithy.model.shapes.ShapeId;

class ClientRuntimeTest {

    @Test
    void plan_isBuiltOncePerClientAndOperationUnderConcurrentCalls() throws Exception {
        CountingProtocol protocol = new CountingProtocol();
        ClientRuntime runtime = PipelineFixtures.runtimeBuilder(protocol).build();
        int threads = 8;
        CyclicBarrier start = new CyclicBarrier(threads);
        ExecutorService executor = Executors.newFixedThreadPool(threads);
        try {
            List<Future<List<PipelinePlan<?, ?>>>> results = new ArrayList<>();
            for (int t = 0; t < threads; t++) {
                results.add(executor.submit(() -> {
                    start.await();
                    List<PipelinePlan<?, ?>> seen = new ArrayList<>();
                    for (int i = 0; i < 500; i++) {
                        seen.add(runtime.plan(i % 2 == 0 ? GET_THING : PUT_THING));
                    }
                    return seen;
                }));
            }
            PipelinePlan<?, ?> get = runtime.plan(GET_THING);
            PipelinePlan<?, ?> put = runtime.plan(PUT_THING);
            for (Future<List<PipelinePlan<?, ?>>> result : results) {
                for (PipelinePlan<?, ?> plan : result.get()) {
                    assertThat(plan).isIn(get, put);
                }
            }
        } finally {
            executor.shutdownNow();
        }
        assertThat(protocol.count(GET_THING)).isEqualTo(1);
        assertThat(protocol.count(PUT_THING)).isEqualTo(1);
        assertThat(runtime.builtPlans()).isEqualTo(2);
    }

    @Test
    void plan_isPerClient() {
        CountingProtocol protocol = new CountingProtocol();
        ClientRuntime first = PipelineFixtures.runtimeBuilder(protocol).build();
        ClientRuntime second = PipelineFixtures.runtimeBuilder(protocol).build();

        PipelinePlan<?, ?> firstPlan = first.plan(GET_THING);
        PipelinePlan<?, ?> secondPlan = second.plan(GET_THING);

        assertThat(firstPlan).isNotSameAs(secondPlan);
        assertThat(firstPlan.runtime()).isSameAs(first);
        assertThat(secondPlan.runtime()).isSameAs(second);
        assertThat(first.plan(GET_THING)).isSameAs(firstPlan);
        assertThat(protocol.count(GET_THING)).isEqualTo(2);
    }

    @Test
    void plan_resolvesFeatureSubplansAndStagesOnce() {
        ClientRuntime runtime = PipelineFixtures.runtime();
        PipelinePlan<?, ?> put = runtime.plan(PUT_THING);
        PipelinePlan<?, ?> get = runtime.plan(GET_THING);

        assertThat(put.idempotencyToken()).isPresent();
        assertThat(put.stages().isActive(PipelineStage.IDEMPOTENCY_TOKEN)).isTrue();
        assertThat(put.idempotencyTokenSlot()).hasValueSatisfying(
            slot -> assertThat(slot.key()).isSameAs(IdempotencyTokenFeature.GENERATED_TOKEN));
        assertThat(put.contextLayout().executionKeys()).containsExactly(IdempotencyTokenFeature.GENERATED_TOKEN);

        assertThat(get.idempotencyToken()).isEmpty();
        assertThat(get.requestChecksum()).isEmpty();
        assertThat(get.responseChecksum()).isEmpty();
        assertThat(get.requestCompression()).isEmpty();
        assertThat(get.endpoint()).isEmpty();
        assertThat(get.contextLayout().executionKeys()).isEmpty();
        assertThat(get.contextLayout().attemptKeys()).containsExactlyElementsOf(PipelineKeys.BUILT_IN);

        // Only core stages are active without features, plugins or interceptors: no hook stage costs anything.
        for (PipelineStage stage : PipelineStage.values()) {
            assertThat(get.stages().isActive(stage)).as(stage.name())
                                                    .isEqualTo(stage.kind() == PipelineStage.Kind.CORE);
        }
        assertThat(get.initialTokenRequest().scope()).isEqualTo("things");
        assertThat(get.initialTokenRequest()).isSameAs(get.initialTokenRequest());
        assertThat(get.protocol().schemas()).isSameAs(GET_THING.schemas());
    }

    @Test
    void plugins_configureEachApplicablePlanOnceInRegistrationOrder() {
        ContextKey<String> traceKey = ContextKey.attempt("Trace", String.class);
        PlanAttribute<String> header = PlanAttribute.create("Header", String.class);
        List<String> events = new ArrayList<>();
        AtomicInteger configured = new AtomicInteger();
        AtomicReference<PipelinePlan.Builder<?, ?>> captured = new AtomicReference<>();
        AwsFeaturePlugin tracing = plugin("test#Tracing", op -> true, builder -> {
            configured.incrementAndGet();
            events.add("tracing:" + builder.operation().id().getName());
            builder.declare(traceKey);
            builder.activate(PipelineStage.BEFORE_TRANSMISSION);
            builder.attribute(header, "x-trace");
            captured.set(builder);
        });
        AwsFeaturePlugin putOnly = plugin("test#PutOnly", op -> op == PUT_THING,
                                          builder -> events.add("putOnly:" + builder.operation().id().getName()));
        ClientRuntime runtime = PipelineFixtures.runtimeBuilder(new CountingProtocol())
                                                .addPlugin(tracing)
                                                .addPlugin(putOnly)
                                                .build();

        PipelinePlan<?, ?> put = runtime.plan(PUT_THING);
        runtime.plan(PUT_THING);
        PipelinePlan<?, ?> get = runtime.plan(GET_THING);
        runtime.plan(GET_THING);

        assertThat(events).containsExactly("tracing:PutThing", "putOnly:PutThing", "tracing:GetThing");
        assertThat(configured).hasValue(2);
        assertThat(put.plugins()).containsExactly(ShapeId.from("test#Tracing"), ShapeId.from("test#PutOnly"));
        assertThat(get.plugins()).containsExactly(ShapeId.from("test#Tracing"));
        assertThat(get.stages().isActive(PipelineStage.BEFORE_TRANSMISSION)).isTrue();
        assertThat(get.stages().isActive(PipelineStage.AFTER_TRANSMISSION)).isFalse();
        assertThat(get.attribute(header)).contains("x-trace");
        assertThat(get.attribute(PlanAttribute.create("Header", String.class))).isEmpty();
        assertThat(get.contextLayout().slot(traceKey)).isPresent();
        // The plan-time builder is unusable once its plan is built.
        assertThatThrownBy(() -> captured.get().declare(ContextKey.attempt("Late", String.class)))
            .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void addPlugin_whenIdRepeats_rejects() {
        AwsFeaturePlugin plugin = plugin("test#Same", op -> true, builder -> { });
        ClientRuntime.Builder builder = PipelineFixtures.runtimeBuilder(new CountingProtocol()).addPlugin(plugin);

        assertThatThrownBy(() -> builder.addPlugin(plugin("test#Same", op -> true, b -> { })))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("test#Same");
    }

    @Test
    void plan_holdsNoCustomerDataAndIsImmutable() {
        Set<Class<?>> perCall = Set.of(SerializableStruct.class, SdkHttpRequest.class, SdkHttpResponse.class,
                                       Identity.class, CallState.class, AttemptState.class, ExecutionContext.class,
                                       AttemptContext.class, CancellationScope.class, TerminalState.class,
                                       software.amazon.awssdk.core.override.CallOptions.class);
        for (Class<?> type : List.of(PipelinePlan.class, ClientRuntime.class, AwsOperation.class,
                                     ContextLayout.class, StageMetadata.class)) {
            for (Field field : type.getDeclaredFields()) {
                if (Modifier.isStatic(field.getModifiers())) {
                    continue;
                }
                assertThat(Modifier.isFinal(field.getModifiers())).as(type.getSimpleName() + "." + field.getName())
                                                                  .isTrue();
                for (Class<?> forbidden : perCall) {
                    // CallOptions of a call without overrides is the client's, not customer data.
                    boolean clientDefault = type == ClientRuntime.class && field.getName().equals("defaultCallOptions");
                    if (!clientDefault) {
                        assertThat(forbidden.isAssignableFrom(field.getType()))
                            .as(type.getSimpleName() + "." + field.getName() + " holds " + forbidden.getSimpleName())
                            .isFalse();
                    }
                }
            }
        }
    }

    @Test
    void callOptions_withoutOverrides_isTheSharedClientInstance() {
        ClientRuntime runtime = PipelineFixtures.runtime();
        ContextKey<String> key = ContextKey.execution("Tenant", String.class);

        assertThat(runtime.callOptions(null, null)).isSameAs(runtime.callOptions());
        assertThat(runtime.callOptions(SdkRequestOverrideConfiguration.builder().build(), null))
            .isSameAs(runtime.callOptions());
        assertThat(runtime.callOptions(null, SdkRequestOverrideConfiguration.builder()
                                                                            .putExecutionAttribute(key, "a")
                                                                            .build())
                          .executionAttribute(key)).contains("a");
        assertThat(runtime.callOptions().executionAttributes()).isEmpty();
    }

    @Test
    void close_closesSdkOwnedResourcesOnceAndLeavesCallerOwnedOpen() {
        AtomicInteger sdkCloses = new AtomicInteger();
        AtomicInteger callerCloses = new AtomicInteger();
        AutoCloseable sdkOwned = sdkCloses::incrementAndGet;
        AutoCloseable callerOwned = callerCloses::incrementAndGet;
        SdkClientConfiguration configuration =
            SdkClientConfiguration.builder()
                                  .addResource(ManagedResource.sdkOwned(sdkOwned))
                                  .addResource(ManagedResource.callerOwned(callerOwned))
                                  .build();
        ClientRuntime runtime = PipelineFixtures.runtimeBuilder(new CountingProtocol())
                                                .configuration(configuration)
                                                .build();

        runtime.close();
        runtime.close();

        assertThat(sdkCloses).hasValue(1);
        assertThat(callerCloses).hasValue(0);
        assertThat(runtime.resources().isClosed()).isTrue();
    }

    @Test
    void build_whenRequiredComponentMissing_rejects() {
        assertThatThrownBy(() -> ClientRuntime.builder().configuration(SdkClientConfiguration.empty()).build())
            .isInstanceOf(NullPointerException.class)
            .hasMessageContaining("protocol");
    }

    private static AwsFeaturePlugin plugin(String id, java.util.function.Predicate<AwsOperation<?, ?>> applies,
                                           java.util.function.Consumer<PipelinePlan.Builder<?, ?>> configure) {
        ShapeId shapeId = ShapeId.from(id);
        return new AwsFeaturePlugin() {
            @Override
            public ShapeId id() {
                return shapeId;
            }

            @Override
            public boolean appliesTo(AwsOperation<?, ?> operation) {
                return applies.test(operation);
            }

            @Override
            public void configure(PipelinePlan.Builder<?, ?> plan) {
                configure.accept(plan);
            }
        };
    }
}
