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
import static software.amazon.awssdk.pipeline.PipelineFixtures.PUT_INPUT;
import static software.amazon.awssdk.pipeline.PipelineFixtures.retries;
import static software.amazon.awssdk.pipeline.PipelineStage.ACQUIRE_RETRY_TOKEN;
import static software.amazon.awssdk.pipeline.PipelineStage.AFTER_ATTEMPT;
import static software.amazon.awssdk.pipeline.PipelineStage.AFTER_EXECUTION;
import static software.amazon.awssdk.pipeline.PipelineStage.AFTER_SERIALIZATION;
import static software.amazon.awssdk.pipeline.PipelineStage.AFTER_TRANSMISSION;
import static software.amazon.awssdk.pipeline.PipelineStage.BEFORE_ATTEMPT;
import static software.amazon.awssdk.pipeline.PipelineStage.BEFORE_DESERIALIZATION;
import static software.amazon.awssdk.pipeline.PipelineStage.BEFORE_EXECUTION;
import static software.amazon.awssdk.pipeline.PipelineStage.BEFORE_TRANSMISSION;
import static software.amazon.awssdk.pipeline.PipelineStage.CLASSIFY_RETRY;
import static software.amazon.awssdk.pipeline.PipelineStage.DESERIALIZE;
import static software.amazon.awssdk.pipeline.PipelineStage.IDEMPOTENCY_TOKEN;
import static software.amazon.awssdk.pipeline.PipelineStage.MAP_EXCEPTION;
import static software.amazon.awssdk.pipeline.PipelineStage.MODIFY_AFTER_SIGNING;
import static software.amazon.awssdk.pipeline.PipelineStage.MODIFY_ATTEMPT_EXCEPTION;
import static software.amazon.awssdk.pipeline.PipelineStage.MODIFY_BEFORE_SIGNING;
import static software.amazon.awssdk.pipeline.PipelineStage.MODIFY_EXCEPTION;
import static software.amazon.awssdk.pipeline.PipelineStage.MODIFY_HTTP_RESPONSE;
import static software.amazon.awssdk.pipeline.PipelineStage.MODIFY_REQUEST;
import static software.amazon.awssdk.pipeline.PipelineStage.MODIFY_RESPONSE;
import static software.amazon.awssdk.pipeline.PipelineStage.MODIFY_RESPONSE_BODY;
import static software.amazon.awssdk.pipeline.PipelineStage.RESOLVE_ENDPOINT;
import static software.amazon.awssdk.pipeline.PipelineStage.RESOLVE_IDENTITY;
import static software.amazon.awssdk.pipeline.PipelineStage.RESPONSE_METADATA;
import static software.amazon.awssdk.pipeline.PipelineStage.SELECT_AUTH_SCHEME;
import static software.amazon.awssdk.pipeline.PipelineStage.SERIALIZE;
import static software.amazon.awssdk.pipeline.PipelineStage.SIGN;
import static software.amazon.awssdk.pipeline.PipelineStage.TELEMETRY;
import static software.amazon.awssdk.pipeline.PipelineStage.TRANSMIT;
import static software.amazon.awssdk.pipeline.PipelineStage.VALIDATE;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import software.amazon.awssdk.auth.api.AuthScheme;
import software.amazon.awssdk.auth.api.AuthSchemeSelector;
import software.amazon.awssdk.auth.api.HttpSigner;
import software.amazon.awssdk.auth.api.ModeledAuth;
import software.amazon.awssdk.auth.api.NoAuthAuthScheme;
import software.amazon.awssdk.auth.api.SignedRequest;
import software.amazon.awssdk.awscore.exception.AwsServiceException;
import software.amazon.awssdk.core.client.config.SdkClientConfiguration;
import software.amazon.awssdk.core.context.ContextKey;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.http.HttpFailureCategory;
import software.amazon.awssdk.http.HttpPhase;
import software.amazon.awssdk.http.HttpTransportException;
import software.amazon.awssdk.identity.spi.Identity;
import software.amazon.awssdk.identity.spi.IdentityProvider;
import software.amazon.awssdk.identity.spi.IdentityProviders;
import software.amazon.awssdk.identity.spi.ResolveIdentityRequest;
import software.amazon.awssdk.pipeline.PipelineFixtures.Driver;
import software.amazon.awssdk.pipeline.PipelineFixtures.RecordingRetryStrategy;
import software.amazon.awssdk.pipeline.PipelineFixtures.ScriptedHttp;
import software.amazon.awssdk.pipeline.PipelineFixtures.Struct;
import software.amazon.smithy.java.core.schema.Schema;
import software.amazon.smithy.model.shapes.ShapeId;

/**
 * The documented lifecycle order, retry-token ownership and single exception-mapping path of {@link AwsCallEngine}
 * (Requirements 10.2, 10.6, 10.7, 10.14, 10.15, 13.11; Property 29), each case run on both drivers. The async driver
 * runs inline: its completion executor and the scripted transport run on the calling thread.
 *
 * <p>A test plugin activates every hook stage and the metadata and telemetry stage points, so the trace shows where
 * each sits; inactive stages are never reported.
 */
class AwsCallEngineLifecycleTest {
    private static final ModeledAuth OPEN = ModeledAuth.builder().operationId("com.example#Open").build();
    private static final AwsOperation<Struct, Struct> OPEN_GET =
        PipelineFixtures.operation("com.example#Open", GET_INPUT, GET_OUTPUT, OPEN);

    private static final List<PipelineStage> PREFIX =
        List.of(VALIDATE, BEFORE_EXECUTION, MODIFY_REQUEST, ACQUIRE_RETRY_TOKEN);
    private static final List<PipelineStage> ATTEMPT =
        List.of(BEFORE_ATTEMPT, RESOLVE_ENDPOINT, SELECT_AUTH_SCHEME, RESOLVE_IDENTITY, SERIALIZE,
                AFTER_SERIALIZATION, MODIFY_BEFORE_SIGNING, SIGN, MODIFY_AFTER_SIGNING, BEFORE_TRANSMISSION, TRANSMIT,
                AFTER_TRANSMISSION, MODIFY_HTTP_RESPONSE, MODIFY_RESPONSE_BODY, BEFORE_DESERIALIZATION, DESERIALIZE,
                RESPONSE_METADATA);
    private static final List<PipelineStage> ATTEMPT_FAILED =
        List.of(MODIFY_ATTEMPT_EXCEPTION, AFTER_ATTEMPT, CLASSIFY_RETRY);
    private static final List<PipelineStage> SUCCEEDED =
        List.of(AFTER_ATTEMPT, MODIFY_RESPONSE, AFTER_EXECUTION, TELEMETRY);
    private static final List<PipelineStage> FAILED =
        List.of(MAP_EXCEPTION, MODIFY_EXCEPTION, AFTER_EXECUTION, TELEMETRY);

    private final ScriptedHttp http = new ScriptedHttp();
    private final List<String> trace = Collections.synchronizedList(new ArrayList<>());

    @ParameterizedTest
    @EnumSource(Driver.class)
    void success_runsEveryStageOnceInTheDocumentedOrder(Driver driver) {
        AwsOperation<Struct, Struct> openPut =
            PipelineFixtures.operation("com.example#OpenPut", PUT_INPUT, GET_OUTPUT, OPEN);
        RecordingRetryStrategy strategy = new RecordingRetryStrategy(retries(3));
        http.respond(200, "v");

        Object outcome = run(driver, runtime(driver, strategy).authSchemeSelector(noAuth()), openPut, PUT_INPUT);

        assertThat(outcome).isInstanceOf(Struct.class);
        assertThat(trace).containsExactlyElementsOf(
            expected().execution(VALIDATE, BEFORE_EXECUTION, MODIFY_REQUEST, IDEMPOTENCY_TOKEN, ACQUIRE_RETRY_TOKEN)
                      .attempt(1, ATTEMPT).attempt(1, AFTER_ATTEMPT)
                      .execution(MODIFY_RESPONSE, AFTER_EXECUTION, TELEMETRY).stages);
        assertThat(strategy.ledger).containsExactly("acquire#1", "success#1");
    }

    @ParameterizedTest
    @EnumSource(Driver.class)
    void retry_classifiesTheFailedAttemptOnceAndTheGrantedTokenStartsExactlyOneAttempt(Driver driver) {
        RecordingRetryStrategy strategy = new RecordingRetryStrategy(retries(3));
        http.respond(503, null).respond(200, "v");

        Object outcome = run(driver, runtime(driver, strategy).authSchemeSelector(noAuth()), OPEN_GET, GET_INPUT);

        assertThat(outcome).isInstanceOf(Struct.class);
        assertThat(trace).containsExactlyElementsOf(
            expected().execution(PREFIX).attempt(1, ATTEMPT).attempt(1, ATTEMPT_FAILED)
                      .attempt(2, ATTEMPT).attempt(2, AFTER_ATTEMPT)
                      .execution(MODIFY_RESPONSE, AFTER_EXECUTION, TELEMETRY).stages);
        assertThat(strategy.ledger).containsExactly("acquire#1", "retry#1", "success#2");
        assertThat(http.transmissions).isEqualTo(2);
    }

    @ParameterizedTest
    @EnumSource(Driver.class)
    void retriesExhausted_takeTheOneMappingPathAfterTheLastClassification(Driver driver) {
        RecordingRetryStrategy strategy = new RecordingRetryStrategy(retries(2));
        http.respond(503, null).respond(503, null);

        Object outcome = run(driver, runtime(driver, strategy).authSchemeSelector(noAuth()), OPEN_GET, GET_INPUT);

        assertThat(outcome).isInstanceOfSatisfying(AwsServiceException.class, e -> {
            assertThat(e.statusCode()).isEqualTo(503);
            assertThat(e).hasNoCause();
        });
        assertThat(trace).containsExactlyElementsOf(
            expected().execution(PREFIX).attempt(1, ATTEMPT).attempt(1, ATTEMPT_FAILED)
                      .attempt(2, ATTEMPT).attempt(2, ATTEMPT_FAILED).execution(FAILED).stages);
        assertThat(strategy.ledger).containsExactly("acquire#1", "retry#1", "stop#2");
    }

    @ParameterizedTest
    @EnumSource(Driver.class)
    void endpointFailure_skipsAuthSerializationSigningAndTransmission(Driver driver) {
        RecordingRetryStrategy strategy = new RecordingRetryStrategy(retries(3));
        ClientRuntime.Builder runtime = runtime(driver, strategy).authSchemeSelector(noAuth())
                                                                 .configuration(SdkClientConfiguration.empty());

        Object outcome = run(driver, runtime, OPEN_GET, GET_INPUT);

        assertThat(outcome).isInstanceOfSatisfying(SdkClientException.class, e -> {
            assertThat(e).hasMessageContaining("No endpoint is configured").hasNoCause();
        });
        assertThat(trace).containsExactlyElementsOf(
            expected().execution(PREFIX).attempt(1, BEFORE_ATTEMPT, RESOLVE_ENDPOINT).attempt(1, ATTEMPT_FAILED)
                      .execution(FAILED).stages);
        assertThat(strategy.ledger).containsExactly("acquire#1", "stop#1");
        assertThat(http.transmissions).isZero();
    }

    @ParameterizedTest
    @EnumSource(Driver.class)
    void identityFailure_skipsSerializationSigningAndTransmission(Driver driver) {
        SdkClientException noCredentials = SdkClientException.create("No credentials");
        AwsOperation<Struct, Struct> signed = PipelineFixtures.operation(
            "com.example#Signed", GET_INPUT, GET_OUTPUT,
            ModeledAuth.builder().operationId("com.example#Signed").addServiceScheme(FailingScheme.ID).build());
        RecordingRetryStrategy strategy = new RecordingRetryStrategy(retries(3));
        ClientRuntime.Builder runtime = runtime(driver, strategy)
            .authSchemeSelector(AuthSchemeSelector.create(List.of(new FailingScheme(noCredentials))));

        Object outcome = run(driver, runtime, signed, GET_INPUT);

        assertThat(outcome).isSameAs(noCredentials);
        assertThat(trace).containsExactlyElementsOf(
            expected().execution(PREFIX)
                      .attempt(1, BEFORE_ATTEMPT, RESOLVE_ENDPOINT, SELECT_AUTH_SCHEME, RESOLVE_IDENTITY)
                      .attempt(1, ATTEMPT_FAILED).execution(FAILED).stages);
        assertThat(strategy.ledger).containsExactly("acquire#1", "stop#1");
        assertThat(http.transmissions).isZero();
    }

    @ParameterizedTest
    @EnumSource(Driver.class)
    void withoutPluginsOrAuth_onlyTheActiveCoreStagesAreReported(Driver driver) {
        http.respond(200, "v");

        runCore(driver, runtime(driver, new RecordingRetryStrategy(retries(1))), GET_THING, GET_INPUT);

        assertThat(trace).containsExactlyElementsOf(
            expected().execution(VALIDATE, ACQUIRE_RETRY_TOKEN)
                      .attempt(1, RESOLVE_ENDPOINT, SELECT_AUTH_SCHEME, SERIALIZE, SIGN, TRANSMIT, DESERIALIZE)
                      .stages);
    }

    @ParameterizedTest
    @EnumSource(Driver.class)
    void transportFailure_surfacesTheSameTypedExceptionWithItsCategory(Driver driver) {
        HttpTransportException reset = HttpTransportException.create(HttpPhase.CONNECT, HttpFailureCategory.Kind.IO,
                                                                      "Connection reset", new IOException("reset"));
        RecordingRetryStrategy strategy = new RecordingRetryStrategy(retries(1));
        http.fail(reset);

        Object outcome = run(driver, runtime(driver, strategy).authSchemeSelector(noAuth()), OPEN_GET, GET_INPUT);

        assertThat(outcome).isSameAs(reset);
        assertThat(reset.category()).isEqualTo(HttpFailureCategory.CONNECT);
        // afterTransmission observes the transport's failure once, before the failed attempt's hooks.
        assertThat(trace).containsExactlyElementsOf(
            expected().execution(PREFIX).attempt(1, ATTEMPT.subList(0, ATTEMPT.indexOf(TRANSMIT) + 1))
                      .attempt(1, AFTER_TRANSMISSION).attempt(1, ATTEMPT_FAILED).execution(FAILED).stages);
        assertThat(strategy.ledger).containsExactly("acquire#1", "stop#1");
    }

    @ParameterizedTest
    @EnumSource(Driver.class)
    void wrappedSdkFailure_isUnwrappedAndNeverWrappedTwice(Driver driver) {
        SdkClientException typed = SdkClientException.create("typed");
        http.fail(new CompletionException(typed));

        Object outcome = run(driver, runtime(driver, new RecordingRetryStrategy(retries(1)))
            .authSchemeSelector(noAuth()), OPEN_GET, GET_INPUT);

        assertThat(outcome).isSameAs(typed);
        assertThat(trace.stream().filter(MAP_EXCEPTION.name()::equals)).hasSize(1);
    }

    @ParameterizedTest
    @EnumSource(Driver.class)
    void nonSdkFailure_isWrappedOnceWithTheOriginalCause(Driver driver) {
        IllegalStateException boom = new IllegalStateException("boom");
        http.fail(boom);

        Object outcome = run(driver, runtime(driver, new RecordingRetryStrategy(retries(1)))
            .authSchemeSelector(noAuth()), OPEN_GET, GET_INPUT);

        assertThat(outcome).isInstanceOfSatisfying(SdkClientException.class, e -> {
            assertThat(e).hasMessageContaining("com.example#Open").hasMessageContaining("boom");
            assertThat(e.getCause()).isSameAs(boom);
        });
        assertThat(boom).hasNoCause();
    }

    @ParameterizedTest
    @EnumSource(Driver.class)
    void error_isNotClassifiedOrMappedAndReleasesTheTokenOnce(Driver driver) {
        FatalError fatal = new FatalError();
        RecordingRetryStrategy strategy = new RecordingRetryStrategy(retries(3));
        http.fail(fatal);

        Object outcome = run(driver, runtime(driver, strategy).authSchemeSelector(noAuth()), OPEN_GET, GET_INPUT);

        assertThat(outcome).isSameAs(fatal);
        assertThat(trace).containsExactlyElementsOf(
            expected().execution(PREFIX).attempt(1, ATTEMPT.subList(0, ATTEMPT.indexOf(TRANSMIT) + 1))
                      .execution(FAILED).stages);
        assertThat(strategy.ledger).containsExactly("acquire#1", "failure#1");
    }

    /** Runs one call on {@code driver} with every hook stage active; see {@link #runCore}. */
    private Object run(Driver driver, ClientRuntime.Builder runtime, AwsOperation<Struct, Struct> operation,
                       Schema input) {
        return runCore(driver, runtime.addPlugin(new AllHookStages()), operation, input);
    }

    /** Runs one call on {@code driver}; its output, or the failure it surfaced to the caller. */
    private Object runCore(Driver driver, ClientRuntime.Builder runtime, AwsOperation<Struct, Struct> operation,
                           Schema input) {
        AwsCallEngine.Builder builder = AwsCallEngine.builder().runtime(runtime.build()).stageObserver(this::observe);
        AwsCallEngine engine = (driver == Driver.SYNC ? builder.syncHttpClient(http) : builder.asyncHttpClient(http))
            .build();
        PipelinePlan<Struct, Struct> plan = engine.runtime().plan(operation);
        Struct request = PipelineFixtures.request(input, "a");
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

    private void observe(CallState<?, ?> call, AttemptState attempt, PipelineStage stage) {
        trace.add(attempt == null ? stage.name() : stage.name() + "#" + attempt.number());
    }

    private static ClientRuntime.Builder runtime(Driver driver, RecordingRetryStrategy strategy) {
        ClientRuntime.Builder runtime = PipelineFixtures.engineRuntime(strategy, driver.mode);
        if (driver == Driver.ASYNC) {
            runtime.completionExecutor(Runnable::run).retryScheduler(delay -> CompletableFuture.completedFuture(null));
        }
        return runtime;
    }

    private static AuthSchemeSelector noAuth() {
        return AuthSchemeSelector.create(List.of(NoAuthAuthScheme.create()));
    }

    private static Trace expected() {
        return new Trace();
    }

    /** An expected trace: execution-scoped stages by name, attempt-scoped ones with their attempt number. */
    private static final class Trace {
        private final List<String> stages = new ArrayList<>();

        Trace execution(PipelineStage... execution) {
            return execution(List.of(execution));
        }

        Trace execution(List<PipelineStage> execution) {
            execution.forEach(stage -> {
                assertThat(stage.scope()).as(stage.name()).isEqualTo(ContextKey.Scope.EXECUTION);
                stages.add(stage.name());
            });
            return this;
        }

        Trace attempt(int number, PipelineStage... attempt) {
            return attempt(number, List.of(attempt));
        }

        Trace attempt(int number, List<PipelineStage> attempt) {
            attempt.forEach(stage -> {
                assertThat(stage.scope()).as(stage.name()).isEqualTo(ContextKey.Scope.ATTEMPT);
                stages.add(stage.name() + "#" + number);
            });
            return this;
        }
    }

    /** Activates every hook stage and the metadata and telemetry stage points, which have no implementation yet. */
    private static final class AllHookStages implements AwsFeaturePlugin {
        @Override
        public ShapeId id() {
            return ShapeId.from("test#AllHookStages");
        }

        @Override
        public boolean appliesTo(AwsOperation<?, ?> operation) {
            return true;
        }

        @Override
        public void configure(PipelinePlan.Builder<?, ?> plan) {
            for (PipelineStage stage : PipelineStage.values()) {
                if (stage.kind() == PipelineStage.Kind.HOOK) {
                    plan.activate(stage);
                }
            }
            plan.activate(RESPONSE_METADATA);
            plan.activate(TELEMETRY);
        }
    }

    /** A scheme whose identity resolution fails with {@code failure}. */
    private static final class FailingScheme implements AuthScheme<Identity> {
        static final String ID = "test#failing";
        private final RuntimeException failure;

        FailingScheme(RuntimeException failure) {
            this.failure = failure;
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
                    return CompletableFuture.failedFuture(failure);
                }
            });
        }

        @Override
        public HttpSigner<Identity> signer() {
            return request -> SignedRequest.unsigned(request.request());
        }
    }

    private static final class FatalError extends Error {
        private static final long serialVersionUID = 1L;
    }
}
