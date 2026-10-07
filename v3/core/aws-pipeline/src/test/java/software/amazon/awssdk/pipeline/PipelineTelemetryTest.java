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
import static software.amazon.awssdk.pipeline.PipelineFixtures.ENDPOINT;
import static software.amazon.awssdk.pipeline.PipelineFixtures.GET_INPUT;
import static software.amazon.awssdk.pipeline.PipelineFixtures.GET_THING;
import static software.amazon.awssdk.pipeline.PipelineFixtures.retries;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import software.amazon.awssdk.awscore.exception.AwsServiceException;
import software.amazon.awssdk.core.client.config.SdkClientConfiguration;
import software.amazon.awssdk.core.client.config.SdkClientOption;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.core.exception.SdkServiceException;
import software.amazon.awssdk.core.exception.SdkTimeoutException;
import software.amazon.awssdk.http.AsyncExchangeCompletion;
import software.amazon.awssdk.http.AsyncHttpExchange;
import software.amazon.awssdk.http.AsyncHttpResponseHandler;
import software.amazon.awssdk.http.HttpClientCapabilities;
import software.amazon.awssdk.http.HttpExecutionOptions;
import software.amazon.awssdk.http.SdkAsyncHttpClient;
import software.amazon.awssdk.http.SdkHttpRequest;
import software.amazon.awssdk.pipeline.PipelineFixtures.Driver;
import software.amazon.awssdk.pipeline.PipelineFixtures.HeaderProtocol;
import software.amazon.awssdk.pipeline.PipelineFixtures.ManualScheduler;
import software.amazon.awssdk.pipeline.PipelineFixtures.ScriptedHttp;
import software.amazon.awssdk.pipeline.PipelineFixtures.Struct;
import software.amazon.awssdk.retries.AdaptiveRetryStrategy;
import software.amazon.awssdk.retries.RetryStrategy;
import software.amazon.awssdk.telemetry.spi.AttemptRecord;
import software.amazon.awssdk.telemetry.spi.CallRecord;
import software.amazon.awssdk.telemetry.spi.TelemetryConfiguration;
import software.amazon.awssdk.telemetry.spi.TelemetryFailure;
import software.amazon.awssdk.telemetry.spi.TelemetryOptions;
import software.amazon.awssdk.telemetry.spi.TelemetryOutcome;
import software.amazon.awssdk.telemetry.spi.TelemetryPublisher;

/**
 * The pipeline's telemetry stage over both drivers (Requirements 21.2-21.5, 21.14, 21.15): one terminal record per
 * attempt, in order, then one per call, with every required dimension; callback failures go to the telemetry failure
 * channel and never change the call's result; and a client without a publisher has no telemetry state at all.
 */
class PipelineTelemetryTest {
    private static final String SERVICE = "Things";

    /** Records every callback, in order, as one event list. */
    private static final class RecordingPublisher implements TelemetryPublisher {
        final List<Object> events = new CopyOnWriteArrayList<>();

        @Override
        public void attemptCompleted(AttemptRecord attempt) {
            events.add(attempt);
        }

        @Override
        public void callCompleted(CallRecord call) {
            events.add(call);
        }
    }

    private static ClientRuntime runtime(Driver driver, RetryStrategy retryStrategy, TelemetryConfiguration telemetry) {
        SdkClientConfiguration.Builder configuration = SdkClientConfiguration.builder()
                                                                             .option(SdkClientOption.CLIENT_ENDPOINT,
                                                                                     ENDPOINT);
        if (telemetry != null) {
            configuration.option(TelemetryOptions.TELEMETRY, telemetry);
        }
        ClientRuntime.Builder runtime = ClientRuntime.builder()
                                                     .configuration(configuration.build())
                                                     .protocol(new HeaderProtocol())
                                                     .transportMode(driver.mode)
                                                     .retryStrategy(retryStrategy, "things")
                                                     .telemetryDimensions(SERVICE, "ScriptedHttp");
        if (driver == Driver.ASYNC) {
            runtime.completionExecutor(Runnable::run).retryScheduler(new ManualScheduler());
        }
        return runtime.build();
    }

    private static AwsCallEngine engine(ClientRuntime runtime, Driver driver, ScriptedHttp http) {
        AwsCallEngine.Builder engine = AwsCallEngine.builder().runtime(runtime);
        return (driver == Driver.SYNC ? engine.syncHttpClient(http) : engine.asyncHttpClient(http)).build();
    }

    /** Runs one GetThing call on {@code driver}: its output, or its surfaced failure. */
    private static Object call(AwsCallEngine engine, Driver driver) throws Exception {
        PipelinePlan<Struct, Struct> plan = engine.runtime().plan(GET_THING);
        Struct input = PipelineFixtures.request(GET_INPUT, "a");
        try {
            if (driver == Driver.SYNC) {
                return engine.executeSync(plan, input, null, null);
            }
            return engine.executeAsync(plan, input, null, null).get(5, TimeUnit.SECONDS);
        } catch (ExecutionException e) {
            return e.getCause();
        } catch (RuntimeException e) {
            return e;
        }
    }

    private static TelemetryConfiguration telemetry(TelemetryPublisher... publishers) {
        TelemetryConfiguration.Builder builder = TelemetryConfiguration.builder();
        for (TelemetryPublisher publisher : publishers) {
            builder.addPublisher(publisher);
        }
        return builder.build();
    }

    @ParameterizedTest
    @EnumSource(Driver.class)
    void aRetriedCallEmitsEachAttemptRecordInOrderThenOneCallRecord(Driver driver) throws Exception {
        RecordingPublisher publisher = new RecordingPublisher();
        ScriptedHttp http = new ScriptedHttp().respond(503, null).respond(200, "ok");
        Object result = call(engine(runtime(driver, retries(3), telemetry(publisher)), driver, http), driver);

        assertThat(result).isInstanceOf(Struct.class);
        assertThat(publisher.events).hasSize(3);
        AttemptRecord first = (AttemptRecord) publisher.events.get(0);
        AttemptRecord second = (AttemptRecord) publisher.events.get(1);
        CallRecord call = (CallRecord) publisher.events.get(2);
        assertThat(first).extracting(AttemptRecord::service, AttemptRecord::operation, AttemptRecord::attemptNumber,
                                     AttemptRecord::outcome, AttemptRecord::httpImplementation,
                                     AttemptRecord::retryMode, AttemptRecord::httpStatusCode,
                                     AttemptRecord::errorType)
                         .containsExactly(SERVICE, "GetThing", 1, TelemetryOutcome.FAILED, "ScriptedHttp",
                                          "standard", OptionalInt.of(503), Optional.of("ServerError"));
        assertThat(second).extracting(AttemptRecord::attemptNumber, AttemptRecord::outcome,
                                      AttemptRecord::httpStatusCode, AttemptRecord::errorType)
                          .containsExactly(2, TelemetryOutcome.SUCCEEDED, OptionalInt.of(200), Optional.empty());
        assertThat(call).extracting(CallRecord::service, CallRecord::operation, CallRecord::outcome,
                                    CallRecord::attemptCount, CallRecord::httpImplementation, CallRecord::retryMode,
                                    CallRecord::errorType, CallRecord::attempts)
                        .containsExactly(SERVICE, "GetThing", TelemetryOutcome.SUCCEEDED, 2, "ScriptedHttp",
                                         "standard", Optional.empty(), List.of(first, second));
        assertThat(call.duration()).isGreaterThanOrEqualTo(first.duration().plus(second.duration()));
        assertThat(first.duration()).isGreaterThan(Duration.ZERO);
    }

    @ParameterizedTest
    @EnumSource(Driver.class)
    void aFailedCallRecordsTheFailureCategoryAndTheRetryMode(Driver driver) throws Exception {
        RecordingPublisher publisher = new RecordingPublisher();
        ScriptedHttp http = new ScriptedHttp().respond(400, null);
        RetryStrategy adaptive = AdaptiveRetryStrategy.builder().maxAttempts(3).build();
        Object result = call(engine(runtime(driver, adaptive, telemetry(publisher)), driver, http), driver);

        assertThat(result).isInstanceOf(AwsServiceException.class);
        assertThat(publisher.events).hasSize(2);
        AttemptRecord attempt = (AttemptRecord) publisher.events.get(0);
        CallRecord call = (CallRecord) publisher.events.get(1);
        assertThat(attempt.outcome()).isEqualTo(TelemetryOutcome.FAILED);
        assertThat(attempt.retryMode()).isEqualTo("adaptive");
        assertThat(call).extracting(CallRecord::outcome, CallRecord::attemptCount, CallRecord::errorType)
                        .containsExactly(TelemetryOutcome.FAILED, 1, Optional.of("ServerError"));
    }

    @ParameterizedTest
    @EnumSource(Driver.class)
    void callbackFailuresGoToTheFailureChannelAndNeverChangeTheResult(Driver driver) throws Exception {
        RuntimeException attemptBoom = new IllegalStateException("attempt exporter down");
        RuntimeException callBoom = new IllegalStateException("call exporter down");
        TelemetryPublisher failing = new TelemetryPublisher() {
            @Override
            public void attemptCompleted(AttemptRecord attempt) {
                throw attemptBoom;
            }

            @Override
            public void callCompleted(CallRecord call) {
                throw callBoom;
            }
        };
        RecordingPublisher after = new RecordingPublisher();
        List<TelemetryFailure> channel = new CopyOnWriteArrayList<>();
        TelemetryConfiguration telemetry = TelemetryConfiguration.builder().addPublisher(failing).addPublisher(after)
                                                                 .failureListener(channel::add).build();

        Object success = call(engine(runtime(driver, retries(1), telemetry), driver,
                                     new ScriptedHttp().respond(200, "ok")), driver);
        assertThat(success).isInstanceOf(Struct.class);
        assertThat(after.events).hasSize(2);
        assertThat(channel).extracting(TelemetryFailure::callback, TelemetryFailure::cause,
                                       TelemetryFailure::publisher, TelemetryFailure::operation)
                           .containsExactly(
                               org.assertj.core.groups.Tuple.tuple(TelemetryFailure.Callback.ATTEMPT_COMPLETED,
                                                                   attemptBoom, failing, "GetThing"),
                               org.assertj.core.groups.Tuple.tuple(TelemetryFailure.Callback.CALL_COMPLETED,
                                                                   callBoom, failing, "GetThing"));

        // A failed call keeps its own causal exception, even when the channel itself throws.
        TelemetryConfiguration throwingChannel =
            TelemetryConfiguration.builder().addPublisher(failing)
                                  .failureListener(f -> {
                                      throw new IllegalStateException("channel down");
                                  }).build();
        Object failure = call(engine(runtime(driver, retries(1), throwingChannel), driver,
                                     new ScriptedHttp().respond(400, null)), driver);
        assertThat(failure).isInstanceOf(AwsServiceException.class);
        assertThat((Throwable) failure).hasMessageContaining("Service error 400");
        assertThat(((Throwable) failure).getSuppressed()).isEmpty();
    }

    @Test
    void anAsyncCallCancelledInFlightEmitsItsCancelledAttemptThenItsCall() {
        RecordingPublisher publisher = new RecordingPublisher();
        HangingHttp http = new HangingHttp();
        AwsCallEngine engine = AwsCallEngine.builder()
                                            .runtime(runtime(Driver.ASYNC, retries(3), telemetry(publisher)))
                                            .asyncHttpClient(http)
                                            .build();
        CompletableFuture<Struct> future =
            engine.executeAsync(engine.runtime().plan(GET_THING), PipelineFixtures.request(GET_INPUT, "a"), null,
                                null);
        assertThat(http.transmissions).isEqualTo(1);
        assertThat(publisher.events).isEmpty();

        assertThat(future.cancel(true)).isTrue();
        assertThatThrownBy(future::join).isInstanceOf(CancellationException.class);
        assertThat(publisher.events).hasSize(2);
        AttemptRecord attempt = (AttemptRecord) publisher.events.get(0);
        CallRecord call = (CallRecord) publisher.events.get(1);
        assertThat(attempt).extracting(AttemptRecord::outcome, AttemptRecord::httpStatusCode, AttemptRecord::errorType)
                           .containsExactly(TelemetryOutcome.CANCELLED, OptionalInt.empty(), Optional.empty());
        assertThat(call).extracting(CallRecord::outcome, CallRecord::attemptCount, CallRecord::errorType)
                        .containsExactly(TelemetryOutcome.CANCELLED, 1, Optional.empty());
    }

    @ParameterizedTest
    @EnumSource(Driver.class)
    void withoutAPublisherNoPlanOrCallHasTelemetryState(Driver driver) throws Exception {
        for (TelemetryConfiguration telemetry : new TelemetryConfiguration[] {null, telemetry()}) {
            ClientRuntime runtime = runtime(driver, retries(3), telemetry);
            assertThat(runtime.telemetryEnabled()).isFalse();
            PipelinePlan<Struct, Struct> plan = runtime.plan(GET_THING);
            assertThat(plan.telemetry()).isNull();
            assertThat(plan.stages().isActive(PipelineStage.TELEMETRY)).isFalse();
            CallState<Struct, Struct> call = plan.newCall(PipelineFixtures.request(GET_INPUT, "a"),
                                                          runtime.callOptions());
            assertThat(call.telemetry()).isNull();
            assertThat(call(engine(runtime, driver, new ScriptedHttp().respond(200, "ok")), driver))
                .isInstanceOf(Struct.class);
        }
        ClientRuntime enabled = runtime(driver, retries(3), telemetry(new RecordingPublisher()));
        assertThat(enabled.telemetryEnabled()).isTrue();
        assertThat(enabled.plan(GET_THING).stages().isActive(PipelineStage.TELEMETRY)).isTrue();
        assertThat(enabled.plan(GET_THING).telemetry()).isNotNull();
    }

    @Test
    void errorTypesFollowV2Categories() {
        assertThat(CallTelemetry.errorType(SdkServiceException.builder().statusCode(500).build()))
            .contains("ServerError");
        assertThat(CallTelemetry.errorType(SdkServiceException.builder().statusCode(429).build()))
            .contains("Throttling");
        assertThat(CallTelemetry.errorType(SdkTimeoutException.builder().scope(SdkTimeoutException.Scope.API_CALL).build())).contains("ConfiguredTimeout");
        assertThat(CallTelemetry.errorType(SdkClientException.create("io", new UncheckedIOException(
            new IOException("reset"))))).contains("IO");
        assertThat(CallTelemetry.errorType(SdkClientException.create("other"))).contains("Other");
        assertThat(CallTelemetry.errorType(new CancellationException())).isEmpty();
        assertThat(CallTelemetry.errorType(null)).isEmpty();
    }

    /** An async transport whose exchanges never complete unless cancelled. */
    private static final class HangingHttp implements SdkAsyncHttpClient {
        int transmissions;

        @Override
        public AsyncHttpExchange execute(SdkHttpRequest request, AsyncHttpResponseHandler handler,
                                         HttpExecutionOptions options) {
            transmissions++;
            return new AsyncExchangeCompletion(clientName(), (reason, failure) -> handler.onError(failure));
        }

        @Override
        public HttpClientCapabilities capabilities() {
            throw new UnsupportedOperationException();
        }

        @Override
        public String clientName() {
            return "Hanging";
        }

        @Override
        public void close() {
        }
    }
}
