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
import static software.amazon.awssdk.pipeline.PipelineFixtures.ENDPOINT;
import static software.amazon.awssdk.pipeline.PipelineFixtures.GET_INPUT;
import static software.amazon.awssdk.pipeline.PipelineFixtures.GET_OUTPUT;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.SocketTimeoutException;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Predicate;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
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
import software.amazon.awssdk.auth.api.SignedRequest;
import software.amazon.awssdk.awscore.exception.AwsErrorDetails;
import software.amazon.awssdk.awscore.exception.AwsServiceException;
import software.amazon.awssdk.core.client.config.SdkClientConfiguration;
import software.amazon.awssdk.core.client.config.SdkClientOption;
import software.amazon.awssdk.core.exception.SdkException;
import software.amazon.awssdk.http.AsyncExchangeCompletion;
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
import software.amazon.awssdk.pipeline.PipelineFixtures.Driver;
import software.amazon.awssdk.pipeline.PipelineFixtures.ManualScheduler;
import software.amazon.awssdk.pipeline.PipelineFixtures.ScriptedHttp;
import software.amazon.awssdk.pipeline.PipelineFixtures.Struct;
import software.amazon.awssdk.protocols.core.ClientProtocol;
import software.amazon.awssdk.protocols.core.OperationProtocol;
import software.amazon.awssdk.smithy.runtime.schema.OperationSchemas;
import software.amazon.awssdk.telemetry.spi.AttemptRecord;
import software.amazon.awssdk.telemetry.spi.CallRecord;
import software.amazon.awssdk.telemetry.spi.TelemetryConfiguration;
import software.amazon.awssdk.telemetry.spi.TelemetryFailure;
import software.amazon.awssdk.telemetry.spi.TelemetryOptions;
import software.amazon.awssdk.telemetry.spi.TelemetryPublisher;
import software.amazon.awssdk.telemetry.spi.internal.LoggingTelemetryFailureListener;
import software.amazon.awssdk.utils.Logger;
import software.amazon.awssdk.utils.WarningSuppression;
import software.amazon.smithy.java.core.schema.SerializableStruct;
import software.amazon.smithy.java.core.schema.ShapeBuilder;
import software.amazon.smithy.model.shapes.ShapeId;

/**
 * Property 70: Logging does not duplicate caller-visible failures (Requirements 21.11, 21.12).
 *
 * <p>For any failure returned to the caller, runtime logging emits zero WARN/ERROR events for that failure; for any
 * repeated benign warning key within a suppression scope, at most one equivalent warning is emitted. Four properties:
 * <ul>
 *   <li>{@link #returnedFailuresAreNotLoggedAtWarnOrError}: sync and async calls through the real
 *       {@link AwsCallEngine} that fail with a transport failure, exhausted throttling or server errors, a modeled, an
 *       unmodeled or an undecodable service error, an undecodable output, an identity, signing or endpoint failure,
 *       or (async) a caller cancellation, each after any number of retried attempts, with or without a telemetry
 *       publisher, under every logging level from {@code ALL} to {@code OFF}. The caller receives the failure, and no
 *       logger at all emits a WARN or ERROR event during the call; when DEBUG is enabled the request log shows the
 *       capture works.</li>
 *   <li>{@link #repeatedBenignWarningsAreEmittedOncePerKeyAndScope}: generated sequences of benign warnings over
 *       interleaved {@link WarningSuppression} scopes, with WARN switched on and off and floods of fresh keys past
 *       {@link WarningSuppression#MAX_KEYS}, compared warning by warning with {@link SuppressionModel}, an independent
 *       model of the rule: a key warns iff WARN is enabled, it has not warned in its scope, and the scope holds fewer
 *       than {@code MAX_KEYS} keys; no message is built for a suppressed or disabled warning.</li>
 *   <li>{@link #concurrentEquivalentWarningsAreEmittedOnce}: the same rule under contention: threads emitting every
 *       key of several scopes many times at once, also with scopes filled close to the bound, emit exactly one WARN
 *       per key and scope, never more keys than the bound admits.</li>
 *   <li>{@link #telemetryCallbackFailuresWarnOncePerPublisherCallbackAndChannel}: a real benign-warning emitter end to
 *       end: failing telemetry publishers over sequences of successful and failed calls of one or two clients. Each
 *       client's {@link LoggingTelemetryFailureListener} warns once per publisher class and callback, then logs at
 *       DEBUG, matching a count model of the delivered callbacks; the calls' own failures are still not logged at
 *       WARN or ERROR.</li>
 * </ul>
 *
 * <p>Isolation: {@code java.util.logging} is global. Every capture here is a handler on the root logger that keeps
 * only the records of the test thread (and, for the concurrency property, of its own uniquely named logger), and each
 * suppression property logs through loggers whose names are unique to the try, so a test running concurrently in the
 * same JVM can neither add records to a capture nor change the levels of its loggers. Those unique loggers hand their
 * records to the capture directly, not to the root logger, so their expected warnings do not flood the console. The returned-failure property
 * sets the level of {@code software.amazon.awssdk}, which a concurrent test could only make less verbose, never
 * produce a WARN record on this thread. Captures restore every level they change. The async driver runs its stages and
 * transport inline on the test thread.
 */
class NonDuplicativeLoggingPropertyTest {
    private static final String SDK_LOGGERS = "software.amazon.awssdk";
    private static final String SCHEME_ID = "test#p70";
    private static final String SERVICE = "Things";
    private static final String OPERATION = "com.example#P70Get";
    private static final AwsOperation<Struct, Struct> UNSIGNED_GET = PipelineFixtures.operation(
        OPERATION, GET_INPUT, GET_OUTPUT);
    private static final AwsOperation<Struct, Struct> SIGNED_GET = PipelineFixtures.operation(
        "com.example#P70SignedGet", GET_INPUT, GET_OUTPUT,
        ModeledAuth.builder().operationId("com.example#P70SignedGet").addServiceScheme(SCHEME_ID).build());
    private static final Identity IDENTITY = new Identity() {
    };
    /** JUL levels standing for TRACE, DEBUG, INFO, WARN and ERROR, plus everything and nothing. */
    private static final List<Level> LEVELS = List.of(Level.ALL, Level.FINEST, Level.FINE, Level.INFO, Level.WARNING,
                                                      Level.SEVERE, Level.OFF);

    // ---------------------------------------------------------------------------------------------------------------
    // Requirement 21.11: a failure returned to the caller is not also logged at WARN or ERROR.
    // ---------------------------------------------------------------------------------------------------------------

    /** The scripted outcome of one attempt; every one of them fails the attempt. */
    enum Outcome {
        /** A connection reset while connecting. */
        CONNECTION_RESET,
        /** The response did not arrive in time. */
        READ_TIMEOUT,
        /** HTTP 429: throttling; repeated, it exhausts the attempts. */
        THROTTLED,
        /** HTTP 503. */
        SERVER_ERROR,
        /** HTTP 404 decoded as a modeled error ({@link ThingNotFoundException}). */
        MODELED_ERROR,
        /** HTTP 400 with an error code the model does not know. */
        UNMODELED_ERROR,
        /** HTTP 500 whose error body cannot be decoded. */
        UNDECODABLE_ERROR,
        /** HTTP 200 whose output cannot be decoded. */
        UNDECODABLE_OUTPUT,
        /** The identity provider fails with an I/O error. Signed only. */
        IDENTITY_UNAVAILABLE,
        /** The identity provider has no identity. Signed only. */
        IDENTITY_MISSING,
        /** The signer fails. Signed only. */
        SIGNING_FAILURE,
        /** The transport never answers and the caller cancels. Async only. */
        HELD;

        /** The text carried by this outcome's failure, which the caller's exception chain must show. */
        String marker() {
            return "p70-" + name();
        }
    }

    /**
     * One generated failing call.
     *
     * @param outcomes the outcome of attempt {@code n} at index {@code n - 1}; maxAttempts is its size
     */
    record FailedCall(Driver driver, Level level, boolean signed, boolean telemetry, boolean endpointUnresolvable,
                      List<Outcome> outcomes) {
        Outcome outcome(int attempt) {
            return outcomes.get(attempt - 1);
        }
    }

    // Feature: aws-sdk-java-v3, Property 70: Logging does not duplicate caller-visible failures
    @Property(tries = 500, edgeCases = EdgeCasesMode.NONE)
    @Label("Property 70: a failure returned to the caller is logged at no WARN or ERROR, at any configured level")
    void returnedFailuresAreNotLoggedAtWarnOrError(@ForAll("failedCalls") FailedCall scenario) {
        long thread = Thread.currentThread().getId();
        FailingCall call = new FailingCall(scenario);
        Throwable returned;
        try (JulCapture capture = new JulCapture(r -> r.getLongThreadID() == thread)
            .level(SDK_LOGGERS, scenario.level())) {
            returned = call.run();
            String description = scenario + "\n  returned: " + returned + "\n  log: " + capture.describe();

            assertThat(returned).as("the caller receives a failure: " + description).isNotNull();
            assertReturnedFailure(scenario, call.lastAttempt, returned, description);
            assertThat(capture.records())
                .as("WARN/ERROR events during a call whose failure the caller receives: " + description)
                .noneMatch(r -> r.getLevel().intValue() >= Level.WARNING.intValue());
            if (scenario.level().intValue() <= Level.FINE.intValue() && call.transmissions > 0) {
                // Non-vacuity: the capture does see the pipeline's (DEBUG) request log of this very call.
                assertThat(capture.records()).as("the DEBUG request log of " + description)
                                             .anyMatch(r -> r.getLoggerName().equals(RequestLogging.LOGGER_NAME));
            }
        }

        Outcome terminal = scenario.endpointUnresolvable() ? null : scenario.outcome(call.lastAttempt);
        Statistics.label("driver").collect(scenario.driver());
        Statistics.label("level").collect(scenario.level().getName());
        Statistics.label("returned failure").collect(terminal == null ? "ENDPOINT_UNRESOLVABLE" : terminal.name());
        Statistics.label("returned failure").coverage(c -> {
            c.check("ENDPOINT_UNRESOLVABLE").percentage(p -> p >= 1);
            for (Outcome outcome : Outcome.values()) {
                c.check(outcome.name()).percentage(p -> p >= 1);
            }
        });
        Statistics.label("retried before failing").collect(call.lastAttempt > 1);
        Statistics.label("retried before failing").coverage(c -> c.check(true).percentage(p -> p >= 10));
        Statistics.label("debug enabled").collect(scenario.level().intValue() <= Level.FINE.intValue());
    }

    /** The caller's failure is the one the last attempt (or the endpoint, or the cancellation) produced. */
    private static void assertReturnedFailure(FailedCall scenario, int lastAttempt, Throwable returned,
                                              String description) {
        if (scenario.endpointUnresolvable()) {
            assertThat(lastAttempt).as(description).isEqualTo(1);
            assertThat(returned).as(description).isInstanceOf(SdkException.class);
            return;
        }
        Outcome terminal = scenario.outcome(lastAttempt);
        if (terminal == Outcome.HELD) {
            assertThat(returned).as(description).isInstanceOf(CancellationException.class);
            return;
        }
        boolean carriesMarker = false;
        for (Throwable t = returned; t != null && !carriesMarker; t = t.getCause()) {
            carriesMarker = t.getMessage() != null && t.getMessage().contains(terminal.marker());
        }
        assertThat(carriesMarker).as("the returned failure carries " + terminal.marker() + ": " + description)
                                 .isTrue();
        if (terminal == Outcome.MODELED_ERROR) {
            assertThat(returned).as(description).isInstanceOf(ThingNotFoundException.class);
        }
    }

    @Provide
    Arbitrary<FailedCall> failedCalls() {
        return Combinators.combine(PipelineArbitraries.drivers(), Arbitraries.of(true, false)).flatAs(
            (driver, signed) -> Combinators.combine(
                Arbitraries.of(LEVELS),
                Arbitraries.of(true, false),
                Arbitraries.frequency(Tuple.of(1, true), Tuple.of(12, false)),
                outcomes(driver, signed).list().ofMinSize(1).ofMaxSize(3))
                .as((level, telemetry, endpointUnresolvable, outcomes) -> new FailedCall(
                    driver, level, signed, telemetry, endpointUnresolvable, outcomes)));
    }

    private static Arbitrary<Outcome> outcomes(Driver driver, boolean signed) {
        List<Tuple.Tuple2<Integer, Outcome>> frequencies = new ArrayList<>(List.of(
            Tuple.of(3, Outcome.CONNECTION_RESET), Tuple.of(2, Outcome.READ_TIMEOUT), Tuple.of(3, Outcome.THROTTLED),
            Tuple.of(3, Outcome.SERVER_ERROR), Tuple.of(2, Outcome.MODELED_ERROR), Tuple.of(2, Outcome.UNMODELED_ERROR),
            Tuple.of(2, Outcome.UNDECODABLE_ERROR), Tuple.of(2, Outcome.UNDECODABLE_OUTPUT)));
        if (signed) {
            // An unavailable identity is retried, so it is returned only from the last attempt: weighted up so the
            // coverage check (>= 1 %) holds for every seed.
            frequencies.add(Tuple.of(5, Outcome.IDENTITY_UNAVAILABLE));
            frequencies.add(Tuple.of(2, Outcome.IDENTITY_MISSING));
            frequencies.add(Tuple.of(2, Outcome.SIGNING_FAILURE));
        }
        if (driver == Driver.ASYNC) {
            frequencies.add(Tuple.of(2, Outcome.HELD));
        }
        return Arbitraries.frequency(frequencies);
    }

    /**
     * Runs one scenario through the real engine: the transport, identity provider and signer act out the outcome of
     * the current attempt (read from the stage observer), and the async driver runs inline, so a held transmission is
     * outstanding when {@code executeAsync} returns and the caller then cancels.
     */
    private static final class FailingCall implements SdkSyncHttpClient, SdkAsyncHttpClient {
        private final FailedCall scenario;
        private int attempt;
        int lastAttempt;
        int transmissions;
        private AsyncExchangeCompletion held;

        FailingCall(FailedCall scenario) {
            this.scenario = scenario;
        }

        /** The failure the caller received, or null if the call succeeded. */
        Throwable run() {
            SdkClientConfiguration.Builder configuration = SdkClientConfiguration.builder();
            if (!scenario.endpointUnresolvable()) {
                configuration.option(SdkClientOption.CLIENT_ENDPOINT, ENDPOINT);
            }
            if (scenario.telemetry()) {
                // A working publisher with the default (logging) failure channel: the telemetry stage runs too.
                configuration.option(TelemetryOptions.TELEMETRY,
                                     TelemetryConfiguration.builder().addPublisher(new QuietPublisher()).build());
            }
            ClientRuntime.Builder runtime = ClientRuntime.builder()
                                                         .configuration(configuration.build())
                                                         .protocol(new MarkerProtocol())
                                                         .transportMode(scenario.driver().mode)
                                                         .retryStrategy(PipelineFixtures.retries(
                                                             scenario.outcomes().size()), "things")
                                                         .telemetryDimensions(SERVICE, clientName());
            if (scenario.signed()) {
                runtime.authSchemeSelector(AuthSchemeSelector.create(List.of(new MarkerScheme(this))));
            }
            boolean async = scenario.driver() == Driver.ASYNC;
            if (async) {
                runtime.completionExecutor(Runnable::run).retryScheduler(new ManualScheduler());
            }
            AwsCallEngine.Builder builder = AwsCallEngine.builder().runtime(runtime.build()).stageObserver(
                (call, attemptState, stage) -> {
                    if (attemptState != null) {
                        attempt = attemptState.number();
                        lastAttempt = Math.max(lastAttempt, attempt);
                    }
                });
            AwsCallEngine engine = (async ? builder.asyncHttpClient(this) : builder.syncHttpClient(this)).build();
            PipelinePlan<Struct, Struct> plan = engine.runtime().plan(scenario.signed() ? SIGNED_GET : UNSIGNED_GET);
            Struct request = PipelineFixtures.request(GET_INPUT, "a");
            if (!async) {
                try {
                    engine.executeSync(plan, request, null, null);
                    return null;
                } catch (RuntimeException failure) {
                    return failure;
                }
            }
            CompletableFuture<Struct> future = engine.executeAsync(plan, request, null, null);
            if (!future.isDone()) {
                assertThat(held).as("only a held transmission leaves " + scenario + " outstanding").isNotNull();
                assertThat(future.cancel(true)).as("the caller cancels " + scenario).isTrue();
            }
            try {
                future.join();
                return null;
            } catch (CompletionException failure) {
                return failure.getCause();
            } catch (CancellationException cancelled) {
                return cancelled;
            }
        }

        Outcome current() {
            return scenario.outcome(attempt);
        }

        /** A transport acting out the current attempt's outcome; identity and signing outcomes never get here. */
        private ScriptedHttp transmission() {
            transmissions++;
            ScriptedHttp http = new ScriptedHttp();
            Outcome outcome = current();
            return switch (outcome) {
                case CONNECTION_RESET -> http.fail(HttpTransportException.create(
                    HttpPhase.CONNECT, HttpFailureCategory.Kind.IO, outcome.marker(), new IOException("reset")));
                case READ_TIMEOUT -> http.fail(HttpTransportException.create(
                    HttpPhase.RESPONSE_CONSUMPTION, HttpFailureCategory.Kind.TIMEOUT, outcome.marker(),
                    new SocketTimeoutException("read timed out")));
                case THROTTLED -> http.respond(429, outcome.marker());
                case SERVER_ERROR -> http.respond(503, outcome.marker());
                case MODELED_ERROR -> http.respond(404, outcome.marker());
                case UNMODELED_ERROR -> http.respond(400, outcome.marker());
                case UNDECODABLE_ERROR -> http.respond(500, outcome.marker());
                case UNDECODABLE_OUTPUT -> http.respond(200, outcome.marker());
                // Were one of these transmitted, the call would succeed and the property would fail.
                case IDENTITY_UNAVAILABLE, IDENTITY_MISSING, SIGNING_FAILURE, HELD -> http.respond(200, "v");
            };
        }

        @Override
        public SyncHttpExchange prepare(SdkHttpRequest request, HttpExecutionOptions options) {
            return transmission().prepare(request, options);
        }

        @Override
        public AsyncHttpExchange execute(SdkHttpRequest request, AsyncHttpResponseHandler handler,
                                         HttpExecutionOptions options) {
            if (current() == Outcome.HELD) {
                transmissions++;
                held = new AsyncExchangeCompletion(clientName(), (reason, failure) -> handler.onError(failure));
                return held;
            }
            return transmission().execute(request, handler, options);
        }

        @Override
        public HttpClientCapabilities capabilities() {
            throw new UnsupportedOperationException();
        }

        @Override
        public String clientName() {
            return "NonDuplicativeLoggingPropertyTest";
        }

        @Override
        public void close() {
        }
    }

    /** A modeled service error. */
    static final class ThingNotFoundException extends AwsServiceException {
        private ThingNotFoundException(Builder builder) {
            super(builder);
        }

        static ThingNotFoundException create(int status, String message) {
            Builder builder = new Builder();
            builder.statusCode(status).message(message)
                   .awsErrorDetails(AwsErrorDetails.builder().errorCode("ThingNotFound").serviceName(SERVICE).build());
            return new ThingNotFoundException(builder);
        }

        private static final class Builder extends BuilderImpl {
            @Override
            public ThingNotFoundException build() {
                return new ThingNotFoundException(this);
            }
        }
    }

    /**
     * A protocol whose error and output decoding act out the marker in the response body: 404 is the modeled
     * {@link ThingNotFoundException}; 400, 429 and 503 are unmodeled errors with their codes; a 500 error body and a
     * marker output body cannot be decoded.
     */
    private static final class MarkerProtocol implements ClientProtocol {
        @Override
        public ShapeId protocolId() {
            return ShapeId.from("aws.protocols#awsJson1_0");
        }

        @Override
        public OperationProtocol operation(OperationSchemas operation) {
            return new MarkerOperation(operation);
        }
    }

    private record MarkerOperation(OperationSchemas schemas) implements OperationProtocol {
        @Override
        public SdkHttpRequest serializeRequest(SerializableStruct input, URI endpoint) {
            // A typed local: String.valueOf(input.getMemberValue(...)) would infer the generic result as char[].
            String name = input.getMemberValue(schemas.input().member("Name"));
            return SdkHttpRequest.builder()
                                 .method("POST")
                                 .scheme(endpoint.getScheme())
                                 .host(endpoint.getHost())
                                 .putHeader("X-Name", name)
                                 .body(HttpBody.fromUtf8("{}"))
                                 .build();
        }

        @Override
        public <T extends SerializableStruct> T deserializeResponse(SdkHttpResponse response, ShapeBuilder<T> output) {
            String body = response.body().asUtf8String();
            if (body.startsWith("p70-")) {
                throw new IllegalStateException(body + ": the output cannot be decoded");
            }
            output.setMemberValue(output.schema().member("Value"), body);
            return output.build();
        }

        @Override
        public AwsServiceException deserializeError(SdkHttpResponse response) {
            String body = response.body().asUtf8String();
            int status = response.statusCode();
            return switch (status) {
                case 404 -> ThingNotFoundException.create(status, body);
                case 500 -> throw new IllegalStateException(body + ": the error body cannot be decoded");
                default -> AwsServiceException.builder()
                                              .statusCode(status)
                                              .message(body)
                                              .awsErrorDetails(AwsErrorDetails.builder()
                                                                              .serviceName(SERVICE)
                                                                              .errorCode(status == 429
                                                                                         ? "ThrottlingException"
                                                                                         : "Unrecognized" + status)
                                                                              .build())
                                              .build();
            };
        }
    }

    /** A scheme whose identity provider and signer act out the current attempt's identity and signing outcomes. */
    private record MarkerScheme(FailingCall call) implements AuthScheme<Identity> {
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
                    Outcome outcome = call.current();
                    return switch (outcome) {
                        case IDENTITY_UNAVAILABLE -> CompletableFuture.failedFuture(
                            new UncheckedIOException(outcome.marker(), new IOException("endpoint unreachable")));
                        case IDENTITY_MISSING -> CompletableFuture.failedFuture(
                            new IllegalStateException(outcome.marker() + ": no identity is configured"));
                        default -> CompletableFuture.completedFuture(IDENTITY);
                    };
                }
            });
        }

        @Override
        public HttpSigner<Identity> signer() {
            return request -> {
                if (call.current() == Outcome.SIGNING_FAILURE) {
                    throw new IllegalStateException(Outcome.SIGNING_FAILURE.marker() + ": the request cannot be signed");
                }
                return SignedRequest.unsigned(request.request());
            };
        }
    }

    /** A telemetry publisher that never fails. */
    private static final class QuietPublisher implements TelemetryPublisher {
        @Override
        public void attemptCompleted(AttemptRecord attempt) {
        }

        @Override
        public void callCompleted(CallRecord call) {
        }
    }

    // ---------------------------------------------------------------------------------------------------------------
    // Requirement 21.12: a repeated benign warning is emitted at most once per category, operation and scope.
    // ---------------------------------------------------------------------------------------------------------------

    private static final List<String> CATEGORIES = List.of("fallback", "background-cleanup", "deprecated-setting");
    private static final List<String> OPERATIONS = Collections.unmodifiableList(
        java.util.Arrays.asList(null, "GetThing", "PutThing"));
    private static final List<Level> WARN_ENABLED = List.of(Level.ALL, Level.FINE, Level.WARNING);
    private static final List<Level> WARN_DISABLED = List.of(Level.SEVERE, Level.OFF);

    /** One step of a sequential suppression script. */
    sealed interface Step permits Emit, SetLevel, Flood {
    }

    /** One benign warning in scope {@code scope}, with or without a cause. */
    record Emit(int scope, String category, String operation, boolean withCause) implements Step {
    }

    /** Sets the level of the logger the warnings go through. */
    record SetLevel(Level level) implements Step {
    }

    /** {@code count} warnings of fresh, distinct keys in scope {@code scope}. */
    record Flood(int scope, int count) implements Step {
    }

    record Script(int scopes, Level initialLevel, List<Step> steps) {
    }

    /** A key of the model: category and operation, null standing for "not operation specific". */
    record Key(int scope, String category, String operation) {
    }

    /**
     * The independent model of the suppression rule: per scope, the set of keys that warned. A warning is emitted
     * iff WARN is enabled, its key has not warned in its scope, and the scope holds fewer than {@code bound} keys;
     * only an emitted warning is recorded.
     */
    static final class SuppressionModel {
        private final int bound;
        private final Map<Integer, Set<Key>> warned = new HashMap<>();

        SuppressionModel(int bound) {
            this.bound = bound;
        }

        boolean warn(Key key, boolean warnEnabled) {
            Set<Key> scope = warned.computeIfAbsent(key.scope(), s -> new HashSet<>());
            if (!warnEnabled || scope.contains(key) || scope.size() >= bound) {
                return false;
            }
            return scope.add(key);
        }

        boolean warned(Key key) {
            return warned.getOrDefault(key.scope(), Set.of()).contains(key);
        }

        int size(int scope) {
            return warned.getOrDefault(scope, Set.of()).size();
        }
    }

    // Feature: aws-sdk-java-v3, Property 70: Logging does not duplicate caller-visible failures
    @Property(tries = 300, edgeCases = EdgeCasesMode.NONE)
    @Label("Property 70: a repeated benign warning is emitted once per key and scope, as the suppression model says")
    void repeatedBenignWarningsAreEmittedOncePerKeyAndScope(@ForAll("scripts") Script script) {
        String loggerName = uniqueLoggerName("sequential");
        Logger logger = Logger.loggerFor(loggerName);
        List<WarningSuppression> scopes = new ArrayList<>();
        for (int i = 0; i < script.scopes(); i++) {
            scopes.add(WarningSuppression.create());
        }
        SuppressionModel model = new SuppressionModel(WarningSuppression.MAX_KEYS);
        List<String> expectedWarnings = new ArrayList<>();
        AtomicInteger built = new AtomicInteger();
        int flooded = 0;
        int suppressedRepeats = 0;
        int disabledEmits = 0;
        boolean boundReached = false;

        try (JulCapture capture = new JulCapture(r -> loggerName.equals(r.getLoggerName()))
            .isolate(loggerName, script.initialLevel())) {
            boolean warnEnabled = script.initialLevel().intValue() <= Level.WARNING.intValue();
            int seq = 0;
            for (Step step : script.steps()) {
                List<Emit> emits = new ArrayList<>();
                if (step instanceof SetLevel setLevel) {
                    capture.level(loggerName, setLevel.level());
                    warnEnabled = setLevel.level().intValue() <= Level.WARNING.intValue();
                    continue;
                } else if (step instanceof Flood flood) {
                    for (int i = 0; i < flood.count(); i++) {
                        emits.add(new Emit(flood.scope(), "flood", "op-" + flooded++, false));
                    }
                } else {
                    emits.add((Emit) step);
                }
                for (Emit emit : emits) {
                    Key key = new Key(emit.scope(), emit.category(), emit.operation());
                    boolean repeat = model.warned(key);
                    boolean full = model.size(emit.scope()) >= WarningSuppression.MAX_KEYS;
                    boolean expected = model.warn(key, warnEnabled);
                    String message = "benign #" + seq++ + " " + key;
                    Throwable cause = emit.withCause() ? new IllegalStateException("ignored " + key) : null;
                    int builtBefore = built.get();
                    boolean emitted = scopes.get(emit.scope()).warn(logger, emit.category(), emit.operation(), () -> {
                        built.incrementAndGet();
                        return message;
                    }, cause);

                    String at = message + " (warn enabled " + warnEnabled + ", repeat " + repeat + ", scope full "
                                + full + ") in " + script;
                    assertThat(emitted).as("emitted: " + at).isEqualTo(expected);
                    assertThat(built.get() - builtBefore).as("messages built for " + at).isEqualTo(expected ? 1 : 0);
                    assertThat(scopes.get(emit.scope()).emitted(emit.category(), emit.operation()))
                        .as("recorded: " + at).isEqualTo(model.warned(key));
                    if (expected) {
                        expectedWarnings.add(message);
                        LogRecord last = capture.records().get(capture.records().size() - 1);
                        assertThat(last.getThrown()).as("cause of " + at).isSameAs(cause);
                    }
                    suppressedRepeats += repeat ? 1 : 0;
                    disabledEmits += warnEnabled ? 0 : 1;
                    boundReached |= full;
                }
            }

            assertThat(capture.records()).as("every captured record is a WARN: " + script)
                                         .allMatch(r -> r.getLevel() == Level.WARNING);
            assertThat(capture.messages()).as("the warnings of " + script).containsExactlyElementsOf(expectedWarnings);
            assertThat(new HashSet<>(capture.messages())).as("distinct warnings").hasSize(expectedWarnings.size());
            for (int scope = 0; scope < script.scopes(); scope++) {
                assertThat(model.size(scope)).as("keys of scope " + scope)
                                             .isLessThanOrEqualTo(WarningSuppression.MAX_KEYS);
            }
        }

        Statistics.label("scopes").collect(script.scopes());
        Statistics.label("a repeated key was suppressed").collect(suppressedRepeats > 0);
        Statistics.label("a repeated key was suppressed").coverage(c -> c.check(true).percentage(p -> p >= 50));
        Statistics.label("a warning was emitted while WARN was disabled").collect(disabledEmits > 0);
        Statistics.label("a warning was emitted while WARN was disabled")
                  .coverage(c -> c.check(true).percentage(p -> p >= 20));
        Statistics.label("a scope reached the key bound").collect(boundReached);
        Statistics.label("a scope reached the key bound").coverage(c -> c.check(true).percentage(p -> p >= 5));
    }

    @Provide
    Arbitrary<Script> scripts() {
        return Arbitraries.integers().between(1, 3).flatMap(scopes -> {
            Arbitrary<Integer> scope = Arbitraries.integers().between(0, scopes - 1);
            Arbitrary<Step> emit = Combinators.combine(scope, Arbitraries.of(CATEGORIES), Arbitraries.of(OPERATIONS),
                                                       Arbitraries.of(true, false)).as(Emit::new);
            Arbitrary<Step> setLevel = Arbitraries.of(LEVELS).map(SetLevel::new);
            Arbitrary<Integer> floodSize = Arbitraries.frequencyOf(
                Tuple.of(3, Arbitraries.integers().between(1, 40)),
                Tuple.of(2, Arbitraries.integers().between(WarningSuppression.MAX_KEYS - 12,
                                                           WarningSuppression.MAX_KEYS + 12)));
            Arbitrary<Step> flood = Combinators.combine(scope, floodSize).as(Flood::new);
            Arbitrary<Step> step = Arbitraries.frequencyOf(Tuple.of(24, emit), Tuple.of(3, setLevel),
                                                           Tuple.of(1, flood));
            Arbitrary<Level> initial = Arbitraries.frequencyOf(Tuple.of(3, Arbitraries.of(WARN_ENABLED)),
                                                               Tuple.of(1, Arbitraries.of(WARN_DISABLED)));
            return Combinators.combine(initial, step.list().ofMinSize(1).ofMaxSize(60))
                              .as((level, steps) -> new Script(scopes, level, steps));
        });
    }

    /**
     * One concurrent run: {@code threads} threads at once emit, in their own shuffled orders, {@code repetitions}
     * times every key of every scope; scope {@code i} first holds {@code prefill.get(i)} other keys and then has
     * {@code keys.get(i)} new ones.
     */
    record Contention(int threads, int repetitions, List<Integer> keys, List<Integer> prefill, long shuffleSeed) {
    }

    // Feature: aws-sdk-java-v3, Property 70: Logging does not duplicate caller-visible failures
    @Property(tries = 100, edgeCases = EdgeCasesMode.NONE)
    @Label("Property 70: concurrent equivalent benign warnings are emitted exactly once per key and scope")
    void concurrentEquivalentWarningsAreEmittedOnce(@ForAll("contentions") Contention contention) throws Exception {
        String loggerName = uniqueLoggerName("concurrent");
        Logger logger = Logger.loggerFor(loggerName);
        int scopeCount = contention.keys().size();
        List<WarningSuppression> scopes = new ArrayList<>();
        Map<WarningSuppression, Integer> scopeIndex = new IdentityHashMap<>();
        for (int i = 0; i < scopeCount; i++) {
            WarningSuppression scope = WarningSuppression.create();
            scopes.add(scope);
            scopeIndex.put(scope, i);
        }
        List<Key> work = new ArrayList<>();
        for (int scope = 0; scope < scopeCount; scope++) {
            for (int k = 0; k < contention.keys().get(scope); k++) {
                for (int r = 0; r < contention.repetitions(); r++) {
                    work.add(new Key(scope, CATEGORIES.get(k % CATEGORIES.size()), "op-" + k));
                }
            }
        }
        Map<Key, AtomicInteger> emittedByKey = new java.util.concurrent.ConcurrentHashMap<>();
        ExecutorService pool = Executors.newFixedThreadPool(contention.threads());
        try (JulCapture capture = new JulCapture(r -> loggerName.equals(r.getLoggerName()))
            .isolate(loggerName, Level.WARNING)) {
            for (int scope = 0; scope < scopeCount; scope++) {
                for (int p = 0; p < contention.prefill().get(scope); p++) {
                    scopes.get(scope).warn(logger, "prefill", "op-" + p, () -> "prefill");
                }
            }
            int prefillRecords = capture.records().size();
            CountDownLatch start = new CountDownLatch(1);
            List<Future<?>> emitters = new ArrayList<>();
            for (int t = 0; t < contention.threads(); t++) {
                List<Key> order = new ArrayList<>(work);
                Collections.shuffle(order, new Random(contention.shuffleSeed() + t));
                emitters.add(pool.submit(() -> {
                    start.await();
                    for (Key key : order) {
                        if (scopes.get(key.scope()).warn(logger, key.category(), key.operation(),
                                                         () -> "concurrent " + key)) {
                            emittedByKey.computeIfAbsent(key, k -> new AtomicInteger()).incrementAndGet();
                        }
                    }
                    return null;
                }));
            }
            start.countDown();
            for (Future<?> emitter : emitters) {
                emitter.get(30, TimeUnit.SECONDS);
            }

            List<String> warnings = capture.messages().subList(prefillRecords, capture.messages().size());
            String description = contention + ", emitted " + emittedByKey;
            assertThat(emittedByKey.values()).as("emissions per key: " + description)
                                             .allMatch(count -> count.get() == 1);
            assertThat(warnings).as("captured warnings: " + description).doesNotHaveDuplicates()
                                .hasSize(emittedByKey.size());
            for (int scope = 0; scope < scopeCount; scope++) {
                int room = WarningSuppression.MAX_KEYS - contention.prefill().get(scope);
                int expected = Math.min(contention.keys().get(scope), room);
                int finalScope = scope;
                long emittedInScope = emittedByKey.keySet().stream().filter(k -> k.scope() == finalScope).count();
                assertThat(emittedInScope).as("new warnings of scope " + scope + " (room " + room + "): "
                                              + description).isEqualTo(expected);
            }
        } finally {
            pool.shutdownNow();
        }

        boolean nearBound = contention.prefill().stream().anyMatch(p -> p > 0);
        Statistics.label("threads").collect(contention.threads());
        Statistics.label("a scope was filled close to the bound").collect(nearBound);
        Statistics.label("a scope was filled close to the bound")
                  .coverage(c -> c.check(true).percentage(p -> p >= 20));
    }

    @Provide
    Arbitrary<Contention> contentions() {
        return Arbitraries.integers().between(1, 3).flatMap(scopes -> Combinators.combine(
            Arbitraries.integers().between(2, 8),
            Arbitraries.integers().between(1, 4),
            Arbitraries.integers().between(1, 30).list().ofSize(scopes),
            Arbitraries.frequencyOf(Tuple.of(3, Arbitraries.just(0)),
                                    Tuple.of(1, Arbitraries.integers().between(WarningSuppression.MAX_KEYS - 8,
                                                                               WarningSuppression.MAX_KEYS)))
                       .list().ofSize(scopes),
            Arbitraries.longs())
            .as(Contention::new));
    }

    /** The publishers of the telemetry property; each class is one equivalence class of its failures. */
    enum PublisherKind {
        QUIET(false, false), FAILS_ATTEMPT(true, false), FAILS_CALL(false, true), FAILS_BOTH(true, true);

        final boolean failsAttempt;
        final boolean failsCall;

        PublisherKind(boolean failsAttempt, boolean failsCall) {
            this.failsAttempt = failsAttempt;
            this.failsCall = failsCall;
        }

        TelemetryPublisher create() {
            return switch (this) {
                case QUIET -> new QuietPublisher();
                case FAILS_ATTEMPT -> new FailingAttemptPublisher();
                case FAILS_CALL -> new FailingCallPublisher();
                case FAILS_BOTH -> new FailingBothPublisher();
            };
        }

        String className() {
            return create().getClass().getName();
        }
    }

    private static final class FailingAttemptPublisher implements TelemetryPublisher {
        @Override
        public void attemptCompleted(AttemptRecord attempt) {
            throw new IllegalStateException("p70 attempt publisher bug");
        }

        @Override
        public void callCompleted(CallRecord call) {
        }
    }

    private static final class FailingCallPublisher implements TelemetryPublisher {
        @Override
        public void attemptCompleted(AttemptRecord attempt) {
        }

        @Override
        public void callCompleted(CallRecord call) {
            throw new IllegalStateException("p70 call publisher bug");
        }
    }

    private static final class FailingBothPublisher implements TelemetryPublisher {
        @Override
        public void attemptCompleted(AttemptRecord attempt) {
            throw new IllegalStateException("p70 attempt publisher bug");
        }

        @Override
        public void callCompleted(CallRecord call) {
            throw new IllegalStateException("p70 call publisher bug");
        }
    }

    /** The scripted HTTP outcome of one attempt of a telemetry scenario. */
    enum Response {
        OK(200), UNAVAILABLE(503), BAD_REQUEST(400);

        final int status;

        Response(int status) {
            this.status = status;
        }
    }

    /**
     * One call of a telemetry scenario, on client {@code client}, with one response per allowed attempt (three); the
     * call stops at the first non-503 response.
     */
    record TelemetryCall(int client, List<Response> responses) {
        int attempts() {
            for (int i = 0; i < responses.size(); i++) {
                if (responses.get(i) != Response.UNAVAILABLE) {
                    return i + 1;
                }
            }
            return responses.size();
        }

        Response terminal() {
            return responses.get(attempts() - 1);
        }
    }

    record TelemetryScenario(Driver driver, Level level, List<List<PublisherKind>> clients, List<TelemetryCall> calls) {
    }

    // Feature: aws-sdk-java-v3, Property 70: Logging does not duplicate caller-visible failures
    @Property(tries = 200, edgeCases = EdgeCasesMode.NONE)
    @Label("Property 70: telemetry callback failures warn once per publisher class, callback and channel")
    void telemetryCallbackFailuresWarnOncePerPublisherCallbackAndChannel(
        @ForAll("telemetryScenarios") TelemetryScenario scenario) {
        long thread = Thread.currentThread().getId();
        String prefix = uniqueLoggerName("telemetry");
        boolean warnEnabled = scenario.level().intValue() <= Level.WARNING.intValue();
        boolean debugEnabled = scenario.level().intValue() <= Level.FINE.intValue();
        // Model: how many failures of each (client, publisher class#callback) the channels receive.
        Map<String, Integer> failures = new LinkedHashMap<>();
        for (TelemetryCall call : scenario.calls()) {
            List<PublisherKind> publishers = scenario.clients().get(call.client());
            for (PublisherKind kind : publishers) {
                if (kind.failsAttempt) {
                    failures.merge(failureKey(prefix, call.client(), kind, TelemetryFailure.Callback.ATTEMPT_COMPLETED),
                                   call.attempts(), Integer::sum);
                }
                if (kind.failsCall) {
                    failures.merge(failureKey(prefix, call.client(), kind, TelemetryFailure.Callback.CALL_COMPLETED),
                                   1, Integer::sum);
                }
            }
        }

        try (JulCapture capture = new JulCapture(r -> r.getLongThreadID() == thread)
            .level(SDK_LOGGERS, scenario.level())) {
            List<AwsCallEngine> engines = new ArrayList<>();
            List<ScriptedHttp> transports = new ArrayList<>();
            for (int client = 0; client < scenario.clients().size(); client++) {
                String channelLogger = prefix + ".client" + client;
                capture.isolate(channelLogger, scenario.level());
                TelemetryConfiguration.Builder telemetry =
                    TelemetryConfiguration.builder()
                                          .failureListener(new LoggingTelemetryFailureListener(
                                              Logger.loggerFor(channelLogger)));
                scenario.clients().get(client).forEach(kind -> telemetry.addPublisher(kind.create()));
                ScriptedHttp http = new ScriptedHttp();
                transports.add(http);
                engines.add(telemetryEngine(scenario.driver(), telemetry.build(), http));
            }

            for (TelemetryCall call : scenario.calls()) {
                ScriptedHttp http = transports.get(call.client());
                for (int i = 0; i < call.attempts(); i++) {
                    http.respond(call.responses().get(i).status, call.responses().get(i) == Response.OK ? "v" : null);
                }
                Object result = execute(engines.get(call.client()), scenario.driver());
                if (call.terminal() == Response.OK) {
                    assertThat(result).as("the result of " + call + " in " + scenario).isInstanceOf(Struct.class);
                } else {
                    assertThat(result).as("the failure of " + call + " in " + scenario)
                                      .isInstanceOfSatisfying(AwsServiceException.class, e -> assertThat(
                                          e.statusCode()).isEqualTo(call.terminal().status));
                }
            }

            String description = scenario + "\n  model: " + failures + "\n  log: " + capture.describe();
            List<LogRecord> channelRecords = capture.records().stream()
                                                    .filter(r -> r.getLoggerName().startsWith(prefix)).toList();
            assertThat(capture.records().stream().filter(r -> !r.getLoggerName().startsWith(prefix)))
                .as("WARN/ERROR events outside the telemetry channels (the calls' own failures): " + description)
                .noneMatch(r -> r.getLevel().intValue() >= Level.WARNING.intValue());
            for (Map.Entry<String, Integer> entry : failures.entrySet()) {
                String key = entry.getKey();
                List<LogRecord> ofKey = channelRecords.stream().filter(r -> keyOf(r).equals(key)).toList();
                long warns = ofKey.stream().filter(r -> r.getLevel() == Level.WARNING).count();
                long debugs = ofKey.stream().filter(r -> r.getLevel() == Level.FINE).count();
                assertThat(warns).as("WARN events of " + key + ": " + description).isEqualTo(warnEnabled ? 1 : 0);
                assertThat(debugs).as("DEBUG events of " + key + ": " + description)
                                  .isEqualTo(debugEnabled ? entry.getValue() - 1 : 0);
                if (warns == 1) {
                    assertThat(ofKey.get(0).getLevel()).as("the first event of " + key + " is its warning")
                                                       .isEqualTo(Level.WARNING);
                }
            }
            assertThat(channelRecords).as("channel events of no modeled failure: " + description)
                                      .allMatch(r -> failures.containsKey(keyOf(r)));
        }

        long repeated = failures.values().stream().filter(n -> n > 1).count();
        Statistics.label("driver").collect(scenario.driver());
        Statistics.label("clients").collect(scenario.clients().size());
        Statistics.label("a callback failed repeatedly").collect(repeated > 0);
        Statistics.label("a callback failed repeatedly").coverage(c -> c.check(true).percentage(p -> p >= 50));
        Statistics.label("warn enabled").collect(warnEnabled);
    }

    @Provide
    Arbitrary<TelemetryScenario> telemetryScenarios() {
        Arbitrary<List<PublisherKind>> publishers = Arbitraries.of(PublisherKind.class).list().ofMinSize(1)
                                                               .ofMaxSize(4);
        return Combinators.combine(PipelineArbitraries.drivers(), Arbitraries.integers().between(1, 2)).flatAs(
            (driver, clients) -> Combinators.combine(
                Arbitraries.frequencyOf(Tuple.of(4, Arbitraries.of(WARN_ENABLED)),
                                        Tuple.of(1, Arbitraries.of(WARN_DISABLED))),
                publishers.list().ofSize(clients),
                Combinators.combine(Arbitraries.integers().between(0, clients - 1),
                                    Arbitraries.of(Response.class).list().ofSize(3))
                           .as(TelemetryCall::new).list().ofMinSize(1).ofMaxSize(6))
                .as((level, kinds, calls) -> new TelemetryScenario(driver, level, kinds, calls)));
    }

    private static AwsCallEngine telemetryEngine(Driver driver, TelemetryConfiguration telemetry, ScriptedHttp http) {
        ClientRuntime.Builder runtime =
            ClientRuntime.builder()
                         .configuration(SdkClientConfiguration.builder()
                                                              .option(SdkClientOption.CLIENT_ENDPOINT, ENDPOINT)
                                                              .option(TelemetryOptions.TELEMETRY, telemetry)
                                                              .build())
                         .protocol(new PipelineFixtures.HeaderProtocol())
                         .transportMode(driver.mode)
                         .retryStrategy(PipelineFixtures.retries(3), "things")
                         .telemetryDimensions(SERVICE, http.clientName());
        if (driver == Driver.ASYNC) {
            runtime.completionExecutor(Runnable::run).retryScheduler(new ManualScheduler());
        }
        AwsCallEngine.Builder engine = AwsCallEngine.builder().runtime(runtime.build());
        return (driver == Driver.SYNC ? engine.syncHttpClient(http) : engine.asyncHttpClient(http)).build();
    }

    /** Runs one call: its output, or the failure the caller received. */
    private static Object execute(AwsCallEngine engine, Driver driver) {
        PipelinePlan<Struct, Struct> plan = engine.runtime().plan(UNSIGNED_GET);
        Struct input = PipelineFixtures.request(GET_INPUT, "a");
        try {
            if (driver == Driver.SYNC) {
                return engine.executeSync(plan, input, null, null);
            }
            CompletableFuture<Struct> future = engine.executeAsync(plan, input, null, null);
            assertThat(future).as("an inline async call is complete when it returns").isDone();
            return future.join();
        } catch (CompletionException e) {
            return e.getCause();
        } catch (RuntimeException e) {
            return e;
        }
    }

    private static String failureKey(String prefix, int client, PublisherKind kind,
                                     TelemetryFailure.Callback callback) {
        return prefix + ".client" + client + "|" + kind.className() + "#" + callback;
    }

    /** The model key of a channel record: its logger and the publisher class and callback its message names. */
    private static String keyOf(LogRecord record) {
        String message = record.getMessage();
        String publisher = message.substring("Telemetry publisher ".length(), message.indexOf(" failed in "));
        int callbackStart = message.indexOf(" failed in ") + " failed in ".length();
        String callback = message.substring(callbackStart, message.indexOf(' ', callbackStart));
        return record.getLoggerName() + "|" + publisher + "#" + callback;
    }

    private static String uniqueLoggerName(String purpose) {
        return "p70." + purpose + "." + UUID.randomUUID().toString().replace("-", "");
    }

    /**
     * Captures, through a handler on the root {@code java.util.logging} logger (the default backend of the SDK
     * logging facade), every record that {@code accept} keeps, and sets the levels of named loggers, holding them
     * strongly so the levels stick; {@link #close} removes the handler and restores every level.
     */
    private static final class JulCapture implements AutoCloseable {
        private final java.util.logging.Logger root = java.util.logging.Logger.getLogger("");
        private final Map<java.util.logging.Logger, Optional<Level>> previous = new LinkedHashMap<>();
        private final List<java.util.logging.Logger> isolated = new ArrayList<>();
        private final List<LogRecord> records = new CopyOnWriteArrayList<>();
        private final Handler handler;

        JulCapture(Predicate<LogRecord> accept) {
            handler = new Handler() {
                @Override
                public void publish(LogRecord record) {
                    if (accept.test(record)) {
                        records.add(record);
                    }
                }

                @Override
                public void flush() {
                }

                @Override
                public void close() {
                }
            };
            handler.setLevel(Level.ALL);
            root.addHandler(handler);
        }

        JulCapture level(String name, Level level) {
            java.util.logging.Logger logger = java.util.logging.Logger.getLogger(name);
            previous.putIfAbsent(logger, Optional.ofNullable(logger.getLevel()));
            logger.setLevel(level);
            return this;
        }

        /**
         * As {@link #level}, for a logger whose name is unique to this capture, whose records this capture then takes
         * directly instead of through the root logger: its expected warnings reach no other handler (the console).
         */
        JulCapture isolate(String name, Level level) {
            level(name, level);
            java.util.logging.Logger logger = java.util.logging.Logger.getLogger(name);
            if (!isolated.contains(logger)) {
                logger.addHandler(handler);
                logger.setUseParentHandlers(false);
                isolated.add(logger);
            }
            return this;
        }

        List<LogRecord> records() {
            return records;
        }

        List<String> messages() {
            return records.stream().map(LogRecord::getMessage).toList();
        }

        String describe() {
            return records.stream().map(r -> r.getLevel() + " " + r.getLoggerName() + ": " + r.getMessage()).toList()
                          .toString();
        }

        @Override
        public void close() {
            root.removeHandler(handler);
            for (java.util.logging.Logger logger : isolated) {
                logger.removeHandler(handler);
                logger.setUseParentHandlers(true);
            }
            previous.forEach((logger, level) -> logger.setLevel(level.orElse(null)));
        }
    }
}
