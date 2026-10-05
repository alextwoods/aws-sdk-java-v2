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
import static software.amazon.awssdk.pipeline.PipelineFixtures.PUT_THING;
import static software.amazon.awssdk.pipeline.PipelineFixtures.retries;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;
import software.amazon.awssdk.auth.api.AuthScheme;
import software.amazon.awssdk.auth.api.AuthSchemeSelector;
import software.amazon.awssdk.auth.api.HttpSigner;
import software.amazon.awssdk.auth.api.ModeledAuth;
import software.amazon.awssdk.auth.api.SelectedAuthScheme;
import software.amazon.awssdk.auth.api.SignedRequest;
import software.amazon.awssdk.core.context.ContextKey;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.endpoints.Endpoint;
import software.amazon.awssdk.http.HttpBody;
import software.amazon.awssdk.http.HttpFailureCategory;
import software.amazon.awssdk.http.HttpPhase;
import software.amazon.awssdk.http.HttpTransportException;
import software.amazon.awssdk.http.SdkHttpRequest;
import software.amazon.awssdk.http.SdkHttpResponse;
import software.amazon.awssdk.http.StreamingBodies;
import software.amazon.awssdk.identity.spi.Identity;
import software.amazon.awssdk.identity.spi.IdentityProvider;
import software.amazon.awssdk.identity.spi.IdentityProviders;
import software.amazon.awssdk.identity.spi.ResolveIdentityRequest;
import software.amazon.awssdk.pipeline.PipelineFixtures.Driver;
import software.amazon.awssdk.pipeline.PipelineFixtures.RecordingRetryStrategy;
import software.amazon.awssdk.pipeline.PipelineFixtures.ScriptedHttp;
import software.amazon.awssdk.pipeline.PipelineFixtures.Struct;
import software.amazon.awssdk.retries.RetryStrategy;
import software.amazon.smithy.java.core.schema.SerializableStruct;
import software.amazon.smithy.model.shapes.ShapeId;

/**
 * Native {@link ExecutionInterceptor} dispatch through both drivers of {@link AwsCallEngine} (Requirements 11.1 to
 * 11.11, 11.13, 11.14): exact contexts, per-scope invocation counts and registration order, prefiltered hooks,
 * mutation composition (including the signature over a pre-sign mutation), context lifetimes, replacement
 * validation, thrown-cause identity and plan-time plugin contributions. The async driver runs inline.
 */
class InterceptorDispatchTest {
    private static final Identity IDENTITY = new Identity() {
    };
    private static final AwsOperation<Struct, Struct> SIGNED_GET = PipelineFixtures.operation(
        "com.example#SignedGet", GET_INPUT, GET_OUTPUT,
        ModeledAuth.builder().operationId("com.example#SignedGet").addServiceScheme(HeaderSigningScheme.ID).build());

    private static final List<String> ATTEMPT_HOOKS =
        List.of("beforeAttempt", "afterSerialization", "modifyBeforeSigning", "modifyAfterSigning",
                "beforeTransmission", "afterTransmission", "modifyHttpResponse", "modifyResponseBody",
                "beforeDeserialization");

    private static final ContextKey<String> EXECUTION_VALUE = ContextKey.execution("TestExecution", String.class);
    private static final ContextKey<String> EXECUTION_FROM_ATTEMPT =
        ContextKey.execution("TestExecutionFromAttempt", String.class);
    private static final ContextKey<String> ATTEMPT_VALUE = ContextKey.attempt("TestAttempt", String.class);

    private final ScriptedHttp http = new ScriptedHttp();
    private final List<String> log = new CopyOnWriteArrayList<>();

    @ParameterizedTest
    @EnumSource(Driver.class)
    void contexts_exposeTheExactValuesOfEachHook(Driver driver) {
        Recorder recorder = new Recorder("r", log);
        http.respond(200, "v");
        Struct request = PipelineFixtures.request(GET_INPUT, "a");

        Object outcome = run(driver, signedRuntime(driver, retries(1)).addInterceptor(recorder), SIGNED_GET, request);

        assertThat(outcome).isInstanceOf(Struct.class);
        SerializableStruct output = (SerializableStruct) outcome;
        HookContext first = recorder.contexts.get(0);
        for (HookContext context : recorder.contexts) {
            assertThat(context.operation()).isSameAs(SIGNED_GET);
            assertThat(context.operationSchema()).isSameAs(SIGNED_GET.schemas().operation());
            assertThat(context.options()).isSameAs(first.options());
            assertThat(context.request()).isSameAs(request);
            assertThat(context.execution()).isSameAs(first.execution());
        }
        HookContext.BeforeAttempt beforeAttempt = recorder.only(HookContext.BeforeAttempt.class);
        recorder.contexts.stream().filter(HookContext.Attempt.class::isInstance)
                         .forEach(c -> assertThat(((HookContext.Attempt) c).attempt())
                             .isSameAs(beforeAttempt.attempt()));
        // The attempt context closed with its attempt; what each hook resolved was captured while it ran.
        assertThat(beforeAttempt.attempt().isOpen()).isFalse();
        Resolved atStart = recorder.resolved.get(HookContext.BeforeAttempt.class);
        assertThat(atStart.attempt()).isEqualTo(1);
        assertThat(atStart.endpoint()).isEmpty();
        assertThat(atStart.identity()).isEmpty();
        Resolved serializedWith = recorder.resolved.get(HookContext.AfterSerialization.class);
        Endpoint endpoint = serializedWith.endpoint().orElseThrow();
        assertThat(endpoint.url()).isEqualTo(PipelineFixtures.ENDPOINT);
        assertThat(serializedWith.identity()).containsSame(IDENTITY);
        assertThat(serializedWith.authScheme().orElseThrow().option().schemeId()).isEqualTo(HeaderSigningScheme.ID);
        // Every later hook of the attempt sees the same resolved endpoint the attempt transmitted to.
        assertThat(recorder.resolved.get(HookContext.AfterAttempt.class).endpoint()).containsSame(endpoint);
        assertThat(http.requests.get(0).host()).isEqualTo(endpoint.url().getHost());

        HookContext.AfterSerialization serialized = recorder.only(HookContext.AfterSerialization.class);
        assertThat(recorder.only(HookContext.ModifyBeforeSigning.class).httpRequest())
            .isSameAs(serialized.httpRequest());
        HookContext.BeforeTransmission transmission = recorder.only(HookContext.BeforeTransmission.class);
        assertThat(transmission.signedRequest()).isSameAs(
            recorder.only(HookContext.ModifyAfterSigning.class).signedRequest());
        assertThat(transmission.signedRequest().request()).isSameAs(http.requests.get(0));

        HookContext.AfterTransmission received = recorder.only(HookContext.AfterTransmission.class);
        assertThat(received.failure()).isEmpty();
        assertThat(received.signedRequest()).isSameAs(transmission.signedRequest());
        SdkHttpResponse response = received.httpResponse().orElseThrow();
        assertThat(recorder.only(HookContext.ModifyHttpResponse.class).httpResponse()).isSameAs(response);
        HookContext.ModifyResponseBody body = recorder.only(HookContext.ModifyResponseBody.class);
        assertThat(body.httpResponse()).isSameAs(response);
        assertThat(body.body()).isSameAs(response.body());
        assertThat(recorder.only(HookContext.BeforeDeserialization.class).httpResponse()).isSameAs(response);

        assertThat(recorder.only(HookContext.AfterAttempt.class).response()).containsSame(output);
        assertThat(recorder.only(HookContext.AfterAttempt.class).failure()).isEmpty();
        assertThat(recorder.only(HookContext.ModifyResponse.class).response()).isSameAs(output);
        assertThat(recorder.only(HookContext.AfterExecution.class).response()).containsSame(output);
        assertThat(recorder.only(HookContext.AfterExecution.class).failure()).isEmpty();
    }

    @ParameterizedTest
    @EnumSource(Driver.class)
    void hooks_runOncePerScopeInRegistrationOrder(Driver driver) {
        http.respond(503, null).respond(200, "v");

        Object output = run(driver, runtime(driver, retries(3)).addInterceptor(new Recorder("a", log))
                                                              .addInterceptor(new Recorder("b", log)), GET_THING,
                            PipelineFixtures.request(GET_INPUT, "a"));

        assertThat(output).isInstanceOf(Struct.class);
        List<String> expected = new ArrayList<>();
        expect(expected, "", "beforeExecution", "modifyRequest");
        expect(expected, "#1", ATTEMPT_HOOKS);
        expect(expected, "#1", "modifyAttemptException", "afterAttempt");
        expect(expected, "#2", ATTEMPT_HOOKS);
        expect(expected, "#2", "afterAttempt");
        expect(expected, "", "modifyResponse", "afterExecution");
        assertThat(log).containsExactlyElementsOf(expected);
    }

    @Test
    void onlyOverriddenHooksAreDispatchedAndActive() {
        AtomicInteger calls = new AtomicInteger();
        ExecutionInterceptor partial = new ExecutionInterceptor() {
            @Override
            public void beforeTransmission(HookContext.BeforeTransmission context) {
                calls.incrementAndGet();
            }

            @Override
            public SerializableStruct modifyResponse(HookContext.ModifyResponse context) {
                calls.incrementAndGet();
                return context.response();
            }
        };
        ClientRuntime.Builder runtime = runtime(Driver.SYNC, retries(1)).addInterceptor(partial);
        http.respond(200, "v");

        AwsCallEngine engine = engine(Driver.SYNC, runtime);
        engine.executeSync(engine.runtime().plan(GET_THING), PipelineFixtures.request(GET_INPUT, "a"), null, null);

        assertThat(hookStages(engine.runtime().plan(GET_THING)))
            .containsExactly(PipelineStage.BEFORE_TRANSMISSION, PipelineStage.MODIFY_RESPONSE);
        assertThat(calls).hasValue(2);
    }

    @Test
    void withoutInterceptors_noHookStageIsActive() {
        ClientRuntime runtime = runtime(Driver.SYNC, retries(1)).build();
        PipelinePlan<Struct, Struct> plan = runtime.plan(GET_THING);

        assertThat(runtime.interceptors()).isEmpty();
        assertThat(plan.interceptors()).isEmpty();
        assertThat(plan.dispatch()).isSameAs(InterceptorDispatch.NONE);
        assertThat(hookStages(plan)).isEmpty();
    }

    @ParameterizedTest
    @EnumSource(Driver.class)
    void mutations_composeIntoTheNextStage_andThePreSignMutationIsSigned(Driver driver) {
        Mutator mutator = new Mutator();
        Recorder observer = new Recorder("o", log);
        http.respond(200, "v");

        Object output = run(driver, signedRuntime(driver, retries(1)).addInterceptor(mutator).addInterceptor(observer),
                            SIGNED_GET, PipelineFixtures.request(GET_INPUT, "a"));

        SdkHttpRequest sent = http.requests.get(0);
        assertThat(sent.headers().firstValue("X-Name")).contains("b");
        assertThat(sent.headers().firstValue("X-Extra")).contains("1");
        assertThat(sent.headers().firstValue("Authorization")).contains("X-Name=b;X-Extra=1");
        assertThat(sent.headers().firstValue("X-After")).contains("1");
        // The next interceptor of the same hook sees the replacement.
        assertThat(observer.only(HookContext.ModifyRequest.class).request()).isSameAs(mutator.request);
        assertThat(observer.only(HookContext.ModifyBeforeSigning.class).httpRequest().headers().firstValue("X-Extra"))
            .contains("1");
        SdkHttpResponse deserialized = observer.only(HookContext.BeforeDeserialization.class).httpResponse();
        assertThat(deserialized.headers().firstValue("X-Response")).contains("1");
        assertThat(deserialized.body().asUtf8String()).isEqualTo("replaced");
        assertThat(output).isSameAs(mutator.output);
        assertThat(((Struct) output).<String>getMemberValue(GET_OUTPUT.member("Value"))).isEqualTo("final:replaced");
    }

    @ParameterizedTest
    @EnumSource(Driver.class)
    void executionValuesSurviveAttempts_attemptValuesDoNot(Driver driver) {
        List<String> seen = new CopyOnWriteArrayList<>();
        List<AttemptContext> attempts = new CopyOnWriteArrayList<>();
        ExecutionInterceptor interceptor = new ExecutionInterceptor() {
            @Override
            public void beforeExecution(HookContext.BeforeExecution context) {
                context.execution().put(EXECUTION_VALUE, "e");
            }

            @Override
            public void beforeAttempt(HookContext.BeforeAttempt context) {
                AttemptContext attempt = context.attempt();
                attempts.add(attempt);
                seen.add(context.attemptNumber() + ":" + attempt.get(EXECUTION_VALUE) + ":"
                         + attempt.get(EXECUTION_FROM_ATTEMPT) + ":" + attempt.get(ATTEMPT_VALUE));
                attempt.put(ATTEMPT_VALUE, "a" + context.attemptNumber());
                attempt.execution().put(EXECUTION_FROM_ATTEMPT, "x" + context.attemptNumber());
            }

            @Override
            public void beforeTransmission(HookContext.BeforeTransmission context) {
                seen.add(context.attemptNumber() + ":" + context.attempt().get(ATTEMPT_VALUE));
            }

            @Override
            public void afterExecution(HookContext.AfterExecution context) {
                seen.add(context.execution().get(EXECUTION_VALUE) + ":"
                         + context.execution().get(EXECUTION_FROM_ATTEMPT));
            }
        };
        http.respond(503, null).respond(200, "v");

        run(driver, runtime(driver, retries(3)).addInterceptor(interceptor), GET_THING,
            PipelineFixtures.request(GET_INPUT, "a"));

        assertThat(seen).containsExactly("1:e:null:null", "1:a1", "2:e:x1:null", "2:a2", "e:x2");
        assertThat(attempts).hasSize(2).allSatisfy(attempt -> {
            assertThat(attempt.isOpen()).isFalse();
            assertThatThrownBy(() -> attempt.get(ATTEMPT_VALUE)).isInstanceOf(IllegalStateException.class);
        });
    }

    static Stream<Arguments> incompatibleReplacements() {
        List<Arguments> cases = List.of(
            Arguments.of(new WrongInputShape(), "modifyRequest", 0, "com.example#GetThingOutput"),
            Arguments.of(new OneShotBody(), "modifyBeforeSigning", 0, "one-shot"),
            Arguments.of(new MovedTarget(), "modifyAfterSigning", 0, "elsewhere.example.com"),
            Arguments.of(new NullResponse(), "modifyHttpResponse", 1, "null"),
            Arguments.of(new WrongOutputShape(), "modifyResponse", 1, "com.example#GetThingInput"));
        return Arrays.stream(Driver.values())
                     .flatMap(driver -> cases.stream().map(c -> {
                         Object[] values = c.get();
                         return Arguments.of(driver, values[0], values[1], values[2], values[3]);
                     }));
    }

    @ParameterizedTest
    @MethodSource("incompatibleReplacements")
    void incompatibleReplacement_failsNamingTheInterceptorAndHook(Driver driver, ExecutionInterceptor interceptor,
                                                                  String hook, int transmissions, String detail) {
        http.respond(200, "v");

        Object outcome = run(driver, runtime(driver, retries(3)).addInterceptor(interceptor), GET_THING,
                             PipelineFixtures.request(GET_INPUT, "a"));

        assertThat(outcome).isInstanceOfSatisfying(InterceptorException.class, e -> {
            assertThat(e.interceptorClass()).isSameAs(interceptor.getClass());
            assertThat(e.hook()).isEqualTo(hook);
            assertThat(e.operation()).isEqualTo(GET_THING.id());
            assertThat(e).hasNoCause()
                         .hasMessageContaining(interceptor.getClass().getName())
                         .hasMessageContaining(hook)
                         .hasMessageContaining("incompatible")
                         .hasMessageContaining(detail);
        });
        assertThat(http.transmissions).isEqualTo(transmissions);
    }

    static Stream<Arguments> throwingHooks() {
        List<String> hooks = new ArrayList<>(List.of("beforeExecution", "modifyRequest"));
        hooks.addAll(ATTEMPT_HOOKS);
        hooks.addAll(List.of("afterAttempt", "modifyResponse", "afterExecution"));
        return Arrays.stream(Driver.values()).flatMap(driver -> hooks.stream().map(h -> Arguments.of(driver, h)));
    }

    @ParameterizedTest
    @MethodSource("throwingHooks")
    void thrownException_failsTheCallWithItAsTheCause(Driver driver, String hook) {
        IllegalStateException thrown = new IllegalStateException("boom in " + hook);
        Thrower thrower = new Thrower(hook, thrown);
        http.respond(200, "v");

        Object outcome = run(driver, runtime(driver, retries(3)).addInterceptor(thrower), GET_THING,
                             PipelineFixtures.request(GET_INPUT, "a"));

        assertThat(outcome).isInstanceOfSatisfying(InterceptorException.class, e -> {
            assertThat(e.interceptorClass()).isSameAs(Thrower.class);
            assertThat(e.hook()).isEqualTo(hook);
            assertThat(e.getCause()).isSameAs(thrown);
            assertThat(e).hasMessageContaining(Thrower.class.getName()).hasMessageContaining(hook)
                         .hasMessageContaining("threw java.lang.IllegalStateException: boom in " + hook);
        });
        // afterExecution observes the failure exactly once (not again after it failed itself); nothing is retried.
        if (hook.equals("afterExecution")) {
            assertThat(thrower.afterExecutionFailures).isEmpty();
        } else {
            assertThat(thrower.afterExecutionFailures).containsExactly((Throwable) outcome);
        }
        assertThat(http.transmissions).isLessThanOrEqualTo(1);
    }

    @ParameterizedTest
    @EnumSource(Driver.class)
    void modifyAttemptException_replacementIsClassifiedAndSurfaced(Driver driver) {
        HttpTransportException reset = HttpTransportException.create(HttpPhase.CONNECT, HttpFailureCategory.Kind.IO,
                                                                      "Connection reset", new IOException("reset"));
        SdkClientException replacement = SdkClientException.create("replaced");
        List<Throwable> attemptFailures = new CopyOnWriteArrayList<>();
        List<Throwable> terminal = new CopyOnWriteArrayList<>();
        ExecutionInterceptor interceptor = new ExecutionInterceptor() {
            @Override
            public Throwable modifyAttemptException(HookContext.ModifyAttemptException context) {
                attemptFailures.add(context.failure());
                return replacement;
            }

            @Override
            public Throwable modifyException(HookContext.ModifyException context) {
                terminal.add(context.failure());
                return context.failure();
            }
        };
        RecordingRetryStrategy strategy = new RecordingRetryStrategy(retries(3));
        http.fail(reset).respond(200, "v");

        Object outcome = run(driver, runtime(driver, strategy).addInterceptor(interceptor), GET_THING,
                             PipelineFixtures.request(GET_INPUT, "a"));

        // The non-retryable replacement, not the transient transport failure, was classified.
        assertThat(attemptFailures).containsExactly(reset);
        assertThat(outcome).isSameAs(replacement);
        assertThat(terminal).containsExactly(replacement);
        assertThat(strategy.ledger).containsExactly("acquire#1", "stop#1");
        assertThat(http.transmissions).isEqualTo(1);
    }

    @Test
    void plugin_contributesInterceptorsAtPlanConstruction() {
        Recorder contributed = new Recorder("plugin", log);
        Recorder client = new Recorder("client", log);
        AtomicInteger configured = new AtomicInteger();
        AwsFeaturePlugin plugin = new AwsFeaturePlugin() {
            @Override
            public ShapeId id() {
                return ShapeId.from("test#AuditPlugin");
            }

            @Override
            public boolean appliesTo(AwsOperation<?, ?> operation) {
                return operation.schemas().input().id().equals(GET_INPUT.id());
            }

            @Override
            public void configure(PipelinePlan.Builder<?, ?> plan) {
                configured.incrementAndGet();
                plan.addInterceptor(contributed);
            }
        };
        AwsCallEngine engine =
            engine(Driver.SYNC, runtime(Driver.SYNC, retries(1)).addPlugin(plugin).addInterceptor(client));
        http.respond(200, "v").respond(200, "v");

        PipelinePlan<Struct, Struct> plan = engine.runtime().plan(GET_THING);
        engine.executeSync(plan, PipelineFixtures.request(GET_INPUT, "a"), null, null);
        engine.executeSync(engine.runtime().plan(GET_THING), PipelineFixtures.request(GET_INPUT, "b"), null, null);

        assertThat(configured).hasValue(1);
        assertThat(plan.interceptors()).containsExactly(contributed, client);
        assertThat(engine.runtime().plan(PUT_THING).interceptors()).containsExactly(client);
        assertThat(log.subList(0, 2)).containsExactly("plugin:beforeExecution", "client:beforeExecution");
        assertThat(log.stream().filter(entry -> entry.endsWith(":afterExecution"))).hasSize(4);
    }

    // Helpers.

    private static void expect(List<String> expected, String suffix, String... hooks) {
        expect(expected, suffix, List.of(hooks));
    }

    /** Each hook, with both recorders {@code a} then {@code b}. */
    private static void expect(List<String> expected, String suffix, List<String> hooks) {
        for (String hook : hooks) {
            expected.add("a:" + hook + suffix);
            expected.add("b:" + hook + suffix);
        }
    }

    private static List<PipelineStage> hookStages(PipelinePlan<?, ?> plan) {
        return plan.stages().activeStages().stream().filter(stage -> stage.kind() == PipelineStage.Kind.HOOK)
                   .collect(Collectors.toList());
    }

    /** Runs one call; its output, or the failure it surfaced to the caller. */
    private Object run(Driver driver, ClientRuntime.Builder runtime, AwsOperation<Struct, Struct> operation,
                       Struct request) {
        AwsCallEngine engine = engine(driver, runtime);
        PipelinePlan<Struct, Struct> plan = engine.runtime().plan(operation);
        try {
            if (driver == Driver.SYNC) {
                return engine.executeSync(plan, request, null, null);
            }
            CompletableFuture<Struct> future = engine.executeAsync(plan, request, null, null);
            try {
                return future.get(5, TimeUnit.SECONDS);
            } catch (ExecutionException e) {
                return e.getCause();
            }
        } catch (Throwable failure) {
            return failure;
        }
    }

    private AwsCallEngine engine(Driver driver, ClientRuntime.Builder runtime) {
        AwsCallEngine.Builder builder = AwsCallEngine.builder().runtime(runtime.build());
        return (driver == Driver.SYNC ? builder.syncHttpClient(http) : builder.asyncHttpClient(http)).build();
    }

    private static ClientRuntime.Builder runtime(Driver driver, RetryStrategy retry) {
        ClientRuntime.Builder runtime = PipelineFixtures.engineRuntime(retry, driver.mode);
        if (driver == Driver.ASYNC) {
            runtime.completionExecutor(Runnable::run).retryScheduler(delay -> CompletableFuture.completedFuture(null));
        }
        return runtime;
    }

    private static ClientRuntime.Builder signedRuntime(Driver driver, RetryStrategy retry) {
        return runtime(driver, retry)
            .authSchemeSelector(AuthSchemeSelector.create(List.of(new HeaderSigningScheme())));
    }

    private static String member(SerializableStruct struct, String member) {
        return struct.getMemberValue(struct.schema().member(member));
    }

    /** The attempt number and resolved values an attempt hook observed while it ran. */
    private record Resolved(int attempt, Optional<Endpoint> endpoint, Optional<Identity> identity,
                            Optional<SelectedAuthScheme<?>> authScheme) {
    }

    /** Records every hook and its context; returns every value unchanged. */
    private static final class Recorder implements ExecutionInterceptor {
        final List<HookContext> contexts = new CopyOnWriteArrayList<>();
        /** Per attempt-hook context type, what the hook's attempt had resolved when it last ran. */
        final Map<Class<?>, Resolved> resolved = new ConcurrentHashMap<>();
        private final String name;
        private final List<String> log;

        Recorder(String name, List<String> log) {
            this.name = name;
            this.log = log;
        }

        <T extends HookContext> T only(Class<T> type) {
            List<T> matching = contexts.stream().filter(type::isInstance).map(type::cast).collect(Collectors.toList());
            assertThat(matching).as(type.getSimpleName()).hasSize(1);
            return matching.get(0);
        }

        private void record(String hook, HookContext context) {
            contexts.add(context);
            String suffix = "";
            if (context instanceof HookContext.Attempt attempt) {
                suffix = "#" + attempt.attemptNumber();
                resolved.put(context.getClass(), new Resolved(attempt.attemptNumber(), attempt.endpoint(),
                                                              attempt.identity(), attempt.authScheme()));
            }
            log.add(name + ":" + hook + suffix);
        }

        @Override
        public void beforeExecution(HookContext.BeforeExecution context) {
            record("beforeExecution", context);
        }

        @Override
        public SerializableStruct modifyRequest(HookContext.ModifyRequest context) {
            record("modifyRequest", context);
            return context.request();
        }

        @Override
        public void beforeAttempt(HookContext.BeforeAttempt context) {
            record("beforeAttempt", context);
        }

        @Override
        public void afterSerialization(HookContext.AfterSerialization context) {
            record("afterSerialization", context);
        }

        @Override
        public SdkHttpRequest modifyBeforeSigning(HookContext.ModifyBeforeSigning context) {
            record("modifyBeforeSigning", context);
            return context.httpRequest();
        }

        @Override
        public SignedRequest modifyAfterSigning(HookContext.ModifyAfterSigning context) {
            record("modifyAfterSigning", context);
            return context.signedRequest();
        }

        @Override
        public void beforeTransmission(HookContext.BeforeTransmission context) {
            record("beforeTransmission", context);
        }

        @Override
        public void afterTransmission(HookContext.AfterTransmission context) {
            record("afterTransmission", context);
        }

        @Override
        public SdkHttpResponse modifyHttpResponse(HookContext.ModifyHttpResponse context) {
            record("modifyHttpResponse", context);
            return context.httpResponse();
        }

        @Override
        public HttpBody modifyResponseBody(HookContext.ModifyResponseBody context) {
            record("modifyResponseBody", context);
            return context.body();
        }

        @Override
        public void beforeDeserialization(HookContext.BeforeDeserialization context) {
            record("beforeDeserialization", context);
        }

        @Override
        public Throwable modifyAttemptException(HookContext.ModifyAttemptException context) {
            record("modifyAttemptException", context);
            return context.failure();
        }

        @Override
        public void afterAttempt(HookContext.AfterAttempt context) {
            record("afterAttempt", context);
        }

        @Override
        public SerializableStruct modifyResponse(HookContext.ModifyResponse context) {
            record("modifyResponse", context);
            return context.response();
        }

        @Override
        public Throwable modifyException(HookContext.ModifyException context) {
            record("modifyException", context);
            return context.failure();
        }

        @Override
        public void afterExecution(HookContext.AfterExecution context) {
            record("afterExecution", context);
        }
    }

    /** Replaces the request, the pre- and post-signing transport request, the response, its body and the output. */
    private static final class Mutator implements ExecutionInterceptor {
        private volatile SerializableStruct request;
        private volatile SerializableStruct output;

        @Override
        public SerializableStruct modifyRequest(HookContext.ModifyRequest context) {
            request = PipelineFixtures.request(GET_INPUT, "b");
            return request;
        }

        @Override
        public SdkHttpRequest modifyBeforeSigning(HookContext.ModifyBeforeSigning context) {
            return context.httpRequest().toBuilder().putHeader("X-Extra", "1").build();
        }

        @Override
        public SignedRequest modifyAfterSigning(HookContext.ModifyAfterSigning context) {
            SignedRequest signed = context.signedRequest();
            return new SignedRequest(signed.request().toBuilder().putHeader("X-After", "1").build(),
                                     signed.signature());
        }

        @Override
        public SdkHttpResponse modifyHttpResponse(HookContext.ModifyHttpResponse context) {
            return context.httpResponse().toBuilder().addHeader("X-Response", "1").build();
        }

        @Override
        public HttpBody modifyResponseBody(HookContext.ModifyResponseBody context) {
            return HttpBody.fromUtf8("replaced");
        }

        @Override
        public SerializableStruct modifyResponse(HookContext.ModifyResponse context) {
            output = new Struct(GET_OUTPUT, Map.of("Value", "final:" + member(context.response(), "Value")));
            return output;
        }
    }

    /** Throws {@code thrown} from {@code hook}; records the failures {@code afterExecution} observes. */
    private static final class Thrower implements ExecutionInterceptor {
        final List<Throwable> afterExecutionFailures = new CopyOnWriteArrayList<>();
        private final String hook;
        private final RuntimeException thrown;

        Thrower(String hook, RuntimeException thrown) {
            this.hook = hook;
            this.thrown = thrown;
        }

        private void maybeThrow(String current) {
            if (hook.equals(current)) {
                throw thrown;
            }
        }

        @Override
        public void beforeExecution(HookContext.BeforeExecution context) {
            maybeThrow("beforeExecution");
        }

        @Override
        public SerializableStruct modifyRequest(HookContext.ModifyRequest context) {
            maybeThrow("modifyRequest");
            return context.request();
        }

        @Override
        public void beforeAttempt(HookContext.BeforeAttempt context) {
            maybeThrow("beforeAttempt");
        }

        @Override
        public void afterSerialization(HookContext.AfterSerialization context) {
            maybeThrow("afterSerialization");
        }

        @Override
        public SdkHttpRequest modifyBeforeSigning(HookContext.ModifyBeforeSigning context) {
            maybeThrow("modifyBeforeSigning");
            return context.httpRequest();
        }

        @Override
        public SignedRequest modifyAfterSigning(HookContext.ModifyAfterSigning context) {
            maybeThrow("modifyAfterSigning");
            return context.signedRequest();
        }

        @Override
        public void beforeTransmission(HookContext.BeforeTransmission context) {
            maybeThrow("beforeTransmission");
        }

        @Override
        public void afterTransmission(HookContext.AfterTransmission context) {
            maybeThrow("afterTransmission");
        }

        @Override
        public SdkHttpResponse modifyHttpResponse(HookContext.ModifyHttpResponse context) {
            maybeThrow("modifyHttpResponse");
            return context.httpResponse();
        }

        @Override
        public HttpBody modifyResponseBody(HookContext.ModifyResponseBody context) {
            maybeThrow("modifyResponseBody");
            return context.body();
        }

        @Override
        public void beforeDeserialization(HookContext.BeforeDeserialization context) {
            maybeThrow("beforeDeserialization");
        }

        @Override
        public void afterAttempt(HookContext.AfterAttempt context) {
            maybeThrow("afterAttempt");
        }

        @Override
        public SerializableStruct modifyResponse(HookContext.ModifyResponse context) {
            maybeThrow("modifyResponse");
            return context.response();
        }

        @Override
        public void afterExecution(HookContext.AfterExecution context) {
            context.failure().ifPresent(afterExecutionFailures::add);
            maybeThrow("afterExecution");
        }
    }

    /** Replaces the request with a value of another shape. */
    private static final class WrongInputShape implements ExecutionInterceptor {
        @Override
        public SerializableStruct modifyRequest(HookContext.ModifyRequest context) {
            return new Struct(GET_OUTPUT, Map.of());
        }
    }

    /** Replaces the replayable body with a one-shot stream. */
    private static final class OneShotBody implements ExecutionInterceptor {
        @Override
        public SdkHttpRequest modifyBeforeSigning(HookContext.ModifyBeforeSigning context) {
            byte[] bytes = {1, 2, 3};
            HttpBody body = HttpBody.streaming(
                StreamingBodies.fromInputStream(new ByteArrayInputStream(bytes), bytes.length, null));
            return context.httpRequest().toBuilder().body(body).build();
        }
    }

    /** Retargets the signed request. */
    private static final class MovedTarget implements ExecutionInterceptor {
        @Override
        public SignedRequest modifyAfterSigning(HookContext.ModifyAfterSigning context) {
            return SignedRequest.unsigned(context.signedRequest().request().toBuilder()
                                                 .host("elsewhere.example.com").build());
        }
    }

    /** Replaces the response with nothing. */
    private static final class NullResponse implements ExecutionInterceptor {
        @Override
        public SdkHttpResponse modifyHttpResponse(HookContext.ModifyHttpResponse context) {
            return null;
        }
    }

    /** Replaces the output with a value of another shape. */
    private static final class WrongOutputShape implements ExecutionInterceptor {
        @Override
        public SerializableStruct modifyResponse(HookContext.ModifyResponse context) {
            return PipelineFixtures.request(GET_INPUT, "x");
        }
    }

    /** Signs by recording the headers it covers in {@code Authorization}; resolves {@link #IDENTITY}. */
    private static final class HeaderSigningScheme implements AuthScheme<Identity> {
        static final String ID = "test#headers";

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
                    return CompletableFuture.completedFuture(IDENTITY);
                }
            });
        }

        @Override
        public HttpSigner<Identity> signer() {
            return sign -> {
                SdkHttpRequest request = sign.request();
                String signature = "X-Name=" + request.headers().firstValue("X-Name").orElse("") + ";X-Extra="
                                   + request.headers().firstValue("X-Extra").orElse("");
                return new SignedRequest(request.toBuilder().putHeader("Authorization", signature).build(),
                                         signature);
            };
        }
    }
}
