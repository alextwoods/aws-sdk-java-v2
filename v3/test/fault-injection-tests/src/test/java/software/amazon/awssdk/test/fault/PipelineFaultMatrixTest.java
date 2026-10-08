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

package software.amazon.awssdk.test.fault;

import static org.assertj.core.api.Assertions.assertThat;
import static software.amazon.awssdk.test.fault.ServiceFixtures.DYNAMODB;
import static software.amazon.awssdk.test.fault.ServiceFixtures.MEDIASTORE_DATA;
import static software.amazon.awssdk.test.fault.ServiceFixtures.REQUEST_ID;
import static software.amazon.awssdk.test.fault.ServiceFixtures.getObjectOk;
import static software.amazon.awssdk.test.fault.ServiceFixtures.listTablesOk;
import static software.amazon.awssdk.test.fault.ServiceFixtures.malformedListTables;
import static software.amazon.awssdk.test.fault.ServiceFixtures.putObjectOk;
import static software.amazon.awssdk.test.fault.ServiceFixtures.resourceNotFound;
import static software.amazon.awssdk.test.fault.ServiceFixtures.throttled;
import static software.amazon.awssdk.test.fault.ServiceFixtures.tooManyRequests;
import static software.amazon.awssdk.test.fault.ServiceFixtures.unavailable;
import static software.amazon.awssdk.test.fault.ServiceFixtures.unmodeled400;
import static software.amazon.awssdk.test.fault.TransportFault.connectFailure;
import static software.amazon.awssdk.test.fault.TransportFault.hang;
import static software.amazon.awssdk.test.fault.TransportFault.hangBody;
import static software.amazon.awssdk.test.fault.TransportFault.partialRequest;
import static software.amazon.awssdk.test.fault.TransportFault.partialResponse;
import static software.amazon.awssdk.test.fault.TransportFault.timeout;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CancellationException;
import java.util.function.Consumer;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import software.amazon.awssdk.awscore.exception.AwsServiceException;
import software.amazon.awssdk.core.ResponseBytes;
import software.amazon.awssdk.core.SdkResponse;
import software.amazon.awssdk.core.async.AsyncResponseTransformer;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.core.exception.SdkException;
import software.amazon.awssdk.core.sync.ResponseTransformer;
import software.amazon.awssdk.endpoints.EndpointResolutionException;
import software.amazon.awssdk.http.CancellationReason;
import software.amazon.awssdk.http.HttpFailureCategory;
import software.amazon.awssdk.http.HttpPhase;
import software.amazon.awssdk.http.HttpTransportException;
import software.amazon.awssdk.http.HttpTransportMode;
import software.amazon.awssdk.services.dynamodb.model.ResourceNotFoundException;
import software.amazon.awssdk.services.mediastoredata.model.GetObjectResponse;
import software.amazon.awssdk.telemetry.spi.TelemetryOutcome;
import software.amazon.awssdk.test.fault.FaultOutcome.Terminal;

/**
 * The release fault matrix through the real pipeline (task 18.5; Requirements 19.7, 19.8, 19.14): every case runs a
 * {@link FaultTrace} through {@link FaultHarness}, which builds the client runtime and engine of a generated service
 * (its real protocol codec, models and error registry) with every dependency a controllable fake, on the sync and the
 * async driver. Each case states its expected terminal outcome, failure type, attempt and transmission counts and
 * retry delays; every case additionally requires exactly-once completion (one {@code afterExecution} unless
 * cancelled, one telemetry record agreeing with the outcome, at most one terminal signal and one cancellation per
 * exchange, every retry token consumed once), released resources (every exchange closed or terminated, no open request
 * body, no abandoned retry timer, no cancelled provider-owned identity future) and the attempt count in the response or
 * exception metadata.
 *
 * <p>Delays: async delays are asserted exactly on the virtual clock of {@link VirtualRetryScheduler}; the sync driver
 * sleeps in real time, so its delays are asserted from the retry decisions and as lower bounds of the measured gaps
 * between attempts.
 */
@Timeout(120)
class PipelineFaultMatrixTest {
    private static final Duration T = RetrySettings.DEFAULT.backoff();
    private static final Duration TT = RetrySettings.DEFAULT.throttlingBackoff();
    private static final Set<HttpTransportMode> BOTH = EnumSet.allOf(HttpTransportMode.class);
    private static final Set<HttpTransportMode> SYNC = EnumSet.of(HttpTransportMode.SYNC);
    private static final Set<HttpTransportMode> ASYNC = EnumSet.of(HttpTransportMode.ASYNC);
    /** A backoff long enough that a sync call is still sleeping when the test interrupts it. */
    private static final RetrySettings SLOW = new RetrySettings(3, Duration.ofMinutes(1), Duration.ofMinutes(1));

    /** The operation a case calls. */
    enum Op {
        LIST_TABLES, PUT_OBJECT, GET_OBJECT
    }

    /** What a case expects. {@code failure} is a type the observed failure must be an instance of. */
    record Expect(Terminal terminal, Class<? extends Throwable> failure, int attempts, int transmissions,
                  List<Duration> delays, Consumer<FaultOutcome> extra) {
        static Expect succeeded(int attempts, Duration... delays) {
            return new Expect(Terminal.SUCCEEDED, null, attempts, attempts, List.of(delays), o -> { });
        }

        static Expect failed(Class<? extends Throwable> failure, int attempts, int transmissions,
                             Duration... delays) {
            return new Expect(Terminal.FAILED, failure, attempts, transmissions, List.of(delays), o -> { });
        }

        /**
         * Cancelled: the async future with the call's {@link CancellationException}; the interrupted sync caller with
         * an {@link SdkClientException} caused by the {@link InterruptedException} (see {@link #assertCancelled}).
         */
        static Expect cancelled(int attempts, int transmissions, Duration... delays) {
            return new Expect(Terminal.CANCELLED, Throwable.class, attempts, transmissions, List.of(delays), o -> { });
        }

        Expect and(Consumer<FaultOutcome> check) {
            Consumer<FaultOutcome> previous = extra;
            return new Expect(terminal, failure, attempts, transmissions, delays, previous.andThen(check));
        }
    }

    /** One matrix case. */
    record Case(String name, Set<FaultKind> kinds, Op op, FaultTrace trace, Set<HttpTransportMode> modes,
                Expect expect) {
        @Override
        public String toString() {
            return name;
        }
    }

    static List<Case> cases() {
        List<Case> cases = new ArrayList<>();
        cases.add(new Case("success", Set.of(), Op.LIST_TABLES, FaultTrace.of(listTablesOk()), BOTH,
                           Expect.succeeded(1)));
        cases.add(new Case("connection failure, retried", Set.of(FaultKind.CONNECTION_FAILURE), Op.LIST_TABLES,
                           FaultTrace.of(connectFailure(), listTablesOk()), BOTH, Expect.succeeded(2, T)));
        cases.add(new Case("connection failure, attempts exhausted", Set.of(FaultKind.CONNECTION_FAILURE),
                           Op.LIST_TABLES, FaultTrace.of(connectFailure(), connectFailure(), connectFailure()), BOTH,
                           Expect.failed(HttpTransportException.class, 3, 3, T, T)
                                 .and(o -> assertCategory(o, HttpFailureCategory.CONNECT))));
        cases.add(new Case("read timeout, retried", Set.of(FaultKind.TIMEOUT), Op.LIST_TABLES,
                           FaultTrace.of(timeout(HttpPhase.RESPONSE_CONSUMPTION), listTablesOk()), BOTH,
                           Expect.succeeded(2, T)));
        cases.add(new Case("connect timeout, attempts exhausted", Set.of(FaultKind.TIMEOUT), Op.LIST_TABLES,
                           FaultTrace.of(timeout(HttpPhase.CONNECT), timeout(HttpPhase.CONNECT),
                                         timeout(HttpPhase.CONNECT)), BOTH,
                           Expect.failed(HttpTransportException.class, 3, 3, T, T)
                                 .and(o -> assertCategory(o, HttpFailureCategory.TIMEOUT))));
        cases.add(new Case("partial response body, retried", Set.of(FaultKind.PARTIAL_RESPONSE_BODY), Op.LIST_TABLES,
                           FaultTrace.of(partialResponse(listTablesOk(), 10), listTablesOk()), BOTH,
                           Expect.succeeded(2, T)));
        cases.add(new Case("partial response body, attempts exhausted", Set.of(FaultKind.PARTIAL_RESPONSE_BODY),
                           Op.LIST_TABLES,
                           FaultTrace.of(partialResponse(listTablesOk(), 10), partialResponse(listTablesOk(), 10),
                                         partialResponse(listTablesOk(), 10)), BOTH,
                           Expect.failed(SdkException.class, 3, 3, T, T)));
        cases.add(new Case("throttling error code, retried with the throttling backoff", Set.of(FaultKind.THROTTLING),
                           Op.LIST_TABLES, FaultTrace.of(throttled(), listTablesOk()), BOTH,
                           Expect.succeeded(2, TT)));
        cases.add(new Case("HTTP 429, attempts exhausted", Set.of(FaultKind.THROTTLING), Op.LIST_TABLES,
                           FaultTrace.of(tooManyRequests(), tooManyRequests(), tooManyRequests()), BOTH,
                           Expect.failed(AwsServiceException.class, 3, 3, TT, TT)
                                 .and(o -> assertThat(((AwsServiceException) o.failure()).statusCode())
                                     .isEqualTo(429))));
        cases.add(new Case("retry-after hint above the backoff, applied", Set.of(FaultKind.RETRY_DELAY_HINT),
                           Op.LIST_TABLES,
                           FaultTrace.of(unavailable().withHeader("x-amz-retry-after", "40"), listTablesOk()), BOTH,
                           Expect.succeeded(2, Duration.ofMillis(40))));
        cases.add(new Case("retry-after hint below the backoff, ignored",
                           Set.of(FaultKind.RETRY_DELAY_HINT, FaultKind.THROTTLING), Op.LIST_TABLES,
                           FaultTrace.of(tooManyRequests().withHeader("x-amz-retry-after", "5"), listTablesOk()), BOTH,
                           Expect.succeeded(2, TT)));
        cases.add(new Case("retry-after hint beyond the extension, capped", Set.of(FaultKind.RETRY_DELAY_HINT),
                           Op.LIST_TABLES,
                           FaultTrace.of(unavailable().withHeader("x-amz-retry-after", "60000"), listTablesOk()),
                           ASYNC, Expect.succeeded(2, T.plusSeconds(5))));
        cases.add(new Case("malformed success, not retried", Set.of(FaultKind.MALFORMED_SUCCESS), Op.LIST_TABLES,
                           FaultTrace.of(malformedListTables(), listTablesOk()), BOTH,
                           Expect.failed(SdkException.class, 1, 1)
                                 .and(o -> assertThat(o.failure()).isNotInstanceOf(AwsServiceException.class))));
        cases.add(new Case("modeled error", Set.of(FaultKind.MODELED_ERROR), Op.LIST_TABLES,
                           FaultTrace.of(resourceNotFound(), listTablesOk()), BOTH,
                           Expect.failed(ResourceNotFoundException.class, 1, 1)
                                 .and(o -> assertServiceError(o, 400, "ResourceNotFoundException"))));
        cases.add(new Case("unmodeled error", Set.of(FaultKind.UNMODELED_ERROR), Op.LIST_TABLES,
                           FaultTrace.of(unmodeled400(), listTablesOk()), BOTH,
                           Expect.failed(AwsServiceException.class, 1, 1)
                                 .and(o -> assertThat(o.failure()).isNotInstanceOf(ResourceNotFoundException.class))
                                 .and(o -> assertServiceError(o, 400, "FaultInjectedMysteryError"))));
        cases.add(new Case("unmodeled 5xx retried, then a modeled error",
                           Set.of(FaultKind.UNMODELED_ERROR, FaultKind.MODELED_ERROR), Op.LIST_TABLES,
                           FaultTrace.of(unavailable(), unavailable(), resourceNotFound()), BOTH,
                           Expect.failed(ResourceNotFoundException.class, 3, 3, T, T)
                                 .and(o -> assertServiceError(o, 400, "ResourceNotFoundException"))));
        cases.add(new Case("mixed transport and service faults, attempts exhausted",
                           Set.of(FaultKind.CONNECTION_FAILURE, FaultKind.THROTTLING), Op.LIST_TABLES,
                           FaultTrace.of(connectFailure(), tooManyRequests(), unavailable()), BOTH,
                           Expect.failed(AwsServiceException.class, 3, 3, T, TT)
                                 .and(o -> assertThat(((AwsServiceException) o.failure()).statusCode())
                                     .isEqualTo(503))));
        cases.add(new Case("one attempt allowed", Set.of(), Op.LIST_TABLES,
                           FaultTrace.of(unavailable(), listTablesOk()).withRetry(RetrySettings.DEFAULT
                                                                                      .withMaxAttempts(1)), BOTH,
                           Expect.failed(AwsServiceException.class, 1, 1)));
        cases.add(new Case("identity failure", Set.of(FaultKind.IDENTITY_FAILURE), Op.LIST_TABLES,
                           FaultTrace.of(listTablesOk()).withIdentities(IdentityStep.FAIL), BOTH,
                           Expect.failed(SdkClientException.class, 1, 0)
                                 .and(o -> assertThat(o.failure()).hasCauseInstanceOf(FaultInjectedException.class))
                                 .and(o -> assertThat(o.identityResolutions()).isEqualTo(1))
                                 .and(o -> assertThat(o.signings()).isZero())));
        cases.add(new Case("endpoint failure", Set.of(FaultKind.ENDPOINT_FAILURE), Op.LIST_TABLES,
                           FaultTrace.of(listTablesOk()).withEndpoint(EndpointMode.FAIL), BOTH,
                           Expect.failed(EndpointResolutionException.class, 1, 0)
                                 .and(o -> assertThat(o.failure()).hasMessageContaining(FaultEndpoints.INJECTED_ERROR))
                                 .and(o -> assertThat(o.identityResolutions()).isZero())));
        cases.add(new Case("signer failure", Set.of(FaultKind.SIGNER_FAILURE), Op.LIST_TABLES,
                           FaultTrace.of(listTablesOk()).withSignings(SignerStep.FAIL), BOTH,
                           Expect.failed(SdkClientException.class, 1, 0)
                                 .and(o -> assertThat(o.failure()).hasCauseInstanceOf(FaultInjectedException.class))));
        cases.add(new Case("partial request body, replayable body retried", Set.of(FaultKind.PARTIAL_REQUEST_BODY),
                           Op.PUT_OBJECT,
                           FaultTrace.of(partialRequest(10), putObjectOk()).withBody(BodyKind.REPLAYABLE), BOTH,
                           Expect.succeeded(2, T)
                                 .and(o -> assertThat(o.exchanges().get(0).requestBytesRead()).isEqualTo(10))
                                 .and(o -> assertThat(o.exchanges().get(1).requestBytesRead())
                                     .isEqualTo(FaultBodies.LENGTH))));
        cases.add(new Case("partial request body, one-shot body not retried", Set.of(FaultKind.PARTIAL_REQUEST_BODY),
                           Op.PUT_OBJECT,
                           FaultTrace.of(partialRequest(10), putObjectOk()).withBody(BodyKind.ONE_SHOT), BOTH,
                           Expect.failed(HttpTransportException.class, 1, 1)
                                 .and(o -> assertCategory(o, HttpFailureCategory.REQUEST_TRANSMISSION))
                                 .and(o -> assertThat(o.unusedTransmissions()).hasSize(1))));
        cases.add(new Case("request body fails mid-stream, replayable body retried",
                           Set.of(FaultKind.REQUEST_BODY_FAILURE), Op.PUT_OBJECT,
                           FaultTrace.of(putObjectOk(), putObjectOk()).withBody(BodyKind.FAILING_ONCE), BOTH,
                           Expect.succeeded(2, T)
                                 .and(o -> assertThat(o.exchanges().get(0).requestBytesRead())
                                     .isEqualTo(FaultBodies.FAIL_AFTER))));
        cases.add(new Case("streaming output success", Set.of(), Op.GET_OBJECT, FaultTrace.of(getObjectOk()), BOTH,
                           Expect.succeeded(1).and(PipelineFaultMatrixTest::assertObjectPayload)
                                 .and(o -> assertHandoffs(o, 1))));
        cases.add(new Case("streaming output 5xx before the payload, retried; the transformer gets the payload once",
                           Set.of(FaultKind.UNMODELED_ERROR), Op.GET_OBJECT,
                           FaultTrace.of(unavailable(), getObjectOk()), BOTH,
                           Expect.succeeded(2, T).and(PipelineFaultMatrixTest::assertObjectPayload)
                                 .and(o -> assertHandoffs(o, 1))));
        cases.add(new Case("streaming output connection failure, retried before the transformer has the payload",
                           Set.of(FaultKind.CONNECTION_FAILURE), Op.GET_OBJECT,
                           FaultTrace.of(connectFailure(), getObjectOk()), BOTH,
                           Expect.succeeded(2, T).and(PipelineFaultMatrixTest::assertObjectPayload)
                                 .and(o -> assertHandoffs(o, 1))));
        cases.add(new Case("partial streaming output body, not retried once the transformer has it",
                           Set.of(FaultKind.PARTIAL_RESPONSE_BODY), Op.GET_OBJECT,
                           FaultTrace.of(partialResponse(getObjectOk(), 10), getObjectOk()), BOTH,
                           Expect.failed(Throwable.class, 1, 1)
                                 .and(o -> assertThat(o.unusedTransmissions()).hasSize(1))
                                 .and(o -> assertHandoffs(o, 1))));
        cases.add(new Case("cancelled while waiting for the identity", Set.of(FaultKind.CANCELLATION),
                           Op.LIST_TABLES,
                           FaultTrace.of(listTablesOk()).withIdentities(IdentityStep.HANG)
                                     .withCancel(CancelPoint.IDENTITY), BOTH,
                           Expect.cancelled(1, 0)));
        cases.add(new Case("cancelled during a retry delay", Set.of(FaultKind.CANCELLATION), Op.LIST_TABLES,
                           FaultTrace.of(unavailable(), listTablesOk()).withRetry(SLOW)
                                     .withCancel(CancelPoint.RETRY_DELAY), BOTH,
                           Expect.cancelled(1, 1, SLOW.backoff())
                                 .and(PipelineFaultMatrixTest::assertTimerCancelledOnceIfAsync)));
        cases.add(new Case("cancelled during transmission", Set.of(FaultKind.CANCELLATION), Op.LIST_TABLES,
                           FaultTrace.of(hang(), listTablesOk()).withCancel(CancelPoint.TRANSMISSION), ASYNC,
                           Expect.cancelled(1, 1)
                                 .and(o -> assertThat(o.exchanges().get(0).cancellations())
                                     .containsExactly(CancellationReason.CALLER_CANCELLED))));
        cases.add(new Case("interrupted during a blocking transmission, not retried", Set.of(FaultKind.CANCELLATION),
                           Op.LIST_TABLES,
                           FaultTrace.of(hang(), listTablesOk()).withCancel(CancelPoint.TRANSMISSION), SYNC,
                           Expect.failed(HttpTransportException.class, 1, 1)
                                 .and(o -> assertCategory(o, HttpFailureCategory.CANCELLED))));
        cases.add(new Case("cancelled while the response body arrives", Set.of(FaultKind.CANCELLATION),
                           Op.LIST_TABLES,
                           FaultTrace.of(hangBody(listTablesOk(), 5), listTablesOk())
                                     .withCancel(CancelPoint.RESPONSE_BODY), ASYNC,
                           Expect.cancelled(1, 1)));
        cases.add(new Case("cancelled while a response transformer consumes the payload",
                           Set.of(FaultKind.CANCELLATION), Op.GET_OBJECT,
                           FaultTrace.of(hangBody(getObjectOk(), 10), getObjectOk())
                                     .withCancel(CancelPoint.RESPONSE_BODY), ASYNC,
                           Expect.cancelled(1, 1)
                                 .and(o -> assertThat(o.exchanges().get(0).bodyCancels()).isEqualTo(1))));
        return cases;
    }

    static Stream<Arguments> matrix() {
        return cases().stream().flatMap(c -> c.modes().stream().map(mode -> Arguments.of(c, mode)));
    }

    @ParameterizedTest(name = "[{1}] {0}")
    @MethodSource("matrix")
    void faultOutcomeMatchesTheExpectation(Case c, HttpTransportMode mode) {
        FaultOutcome outcome = run(c, mode);
        String description = outcome.describe();
        Expect expect = c.expect();

        assertThat(outcome.terminal()).as(description).isEqualTo(expect.terminal());
        if (expect.failure() != null) {
            assertThat(outcome.failure()).as(description).isInstanceOf(expect.failure());
        } else {
            assertThat(outcome.failure()).as(description).isNull();
        }
        assertThat(outcome.attempts()).as("attempts: " + description).isEqualTo(expect.attempts());
        assertThat(outcome.transmissions()).as("transmissions: " + description).isEqualTo(expect.transmissions());
        assertDelays(outcome, expect.delays(), description);
        if (outcome.terminal() == Terminal.CANCELLED) {
            assertCancelled(outcome, description);
        }
        assertExactlyOnce(outcome, description);
        assertThat(outcome.leaks()).as("leaks: " + description).isEmpty();
        assertThat(outcome.violations()).as("violations: " + description).isEmpty();
        assertAttemptMetadata(outcome, description);
        expect.extra().accept(outcome);
    }

    /** Every fault kind of Requirement 19.7 is injected on both drivers. */
    @Test
    void matrixCoversEveryRequiredFaultKindOnBothDrivers() {
        Set<FaultKind> required = EnumSet.range(FaultKind.CONNECTION_FAILURE, FaultKind.CANCELLATION);
        for (HttpTransportMode mode : HttpTransportMode.values()) {
            Set<FaultKind> covered = EnumSet.noneOf(FaultKind.class);
            cases().stream().filter(c -> c.modes().contains(mode)).forEach(c -> covered.addAll(c.kinds()));
            assertThat(covered).as(mode.toString()).containsAll(required);
        }
    }

    private static FaultOutcome run(Case c, HttpTransportMode mode) {
        try (FaultHarness harness = FaultHarness.builder(c.op() == Op.LIST_TABLES ? DYNAMODB : MEDIASTORE_DATA)
                                                .trace(c.trace())
                                                .mode(mode)
                                                .build()) {
            return switch (c.op()) {
                case LIST_TABLES -> harness.execute(ServiceFixtures.LIST_TABLES, ServiceFixtures.listTablesRequest());
                case PUT_OBJECT -> harness.execute(ServiceFixtures.PUT_OBJECT, ServiceFixtures.putObjectRequest());
                case GET_OBJECT -> harness.executeStreaming(ServiceFixtures.GET_OBJECT,
                                                            ServiceFixtures.getObjectRequest(),
                                                            ResponseTransformer.toBytes(),
                                                            AsyncResponseTransformer::toBytes);
            };
        }
    }

    /**
     * The retry decisions carry exactly the expected delays; the async driver requested exactly those (non-zero)
     * delays of its scheduler; the sync driver slept at least as long between attempts.
     */
    private static void assertDelays(FaultOutcome outcome, List<Duration> expected, String description) {
        assertThat(outcome.retryDelays()).as("retry decision delays: " + description).isEqualTo(expected);
        if (outcome.mode() == HttpTransportMode.ASYNC) {
            assertThat(outcome.scheduledDelays()).as("scheduled delays: " + description)
                                                 .isEqualTo(expected.stream().filter(d -> !d.isZero()).toList());
        } else {
            List<Duration> gaps = outcome.attemptGaps();
            for (int i = 0; i < gaps.size() && i < expected.size(); i++) {
                assertThat(gaps.get(i)).as("sync gap " + i + ": " + description)
                                       .isGreaterThanOrEqualTo(expected.get(i));
            }
        }
    }

    /**
     * A cancelled async call's future fails with the call's {@link CancellationException}; an interrupted sync call
     * throws an {@link SdkClientException} caused by the {@link InterruptedException}, and the pipeline cancels the call
     * (its telemetry outcome) and restores the caller's interrupt status (checked by the harness).
     */
    private static void assertCancelled(FaultOutcome outcome, String description) {
        if (outcome.mode() == HttpTransportMode.ASYNC) {
            assertThat(outcome.failure()).as(description).isInstanceOf(CancellationException.class);
        } else {
            assertThat(outcome.failure()).as(description).isInstanceOf(SdkClientException.class)
                                         .hasCauseInstanceOf(InterruptedException.class);
        }
    }

    /**
     * Exactly-once completion: {@code afterExecution} once (never for a cancelled async call, whose hooks stop; an
     * interrupted sync call surfaces a mapped failure, which its {@code afterExecution} hooks observe), one
     * telemetry record agreeing with the caller's outcome and attempt count, at most one cancellation and terminal
     * signal per exchange (in {@link ExchangeRecord#leak()}), and each sync exchange closed exactly once.
     */
    private static void assertExactlyOnce(FaultOutcome outcome, String description) {
        int expectedAfterExecution = outcome.terminal() == Terminal.CANCELLED
                                     && outcome.mode() == HttpTransportMode.ASYNC ? 0 : 1;
        assertThat(outcome.afterExecutions()).as("afterExecution: " + description).isEqualTo(expectedAfterExecution);
        assertThat(outcome.telemetry()).as("telemetry records: " + description).hasSize(1);
        TelemetryOutcome expected = switch (outcome.terminal()) {
            case SUCCEEDED -> TelemetryOutcome.SUCCEEDED;
            case CANCELLED -> TelemetryOutcome.CANCELLED;
            default -> TelemetryOutcome.FAILED;
        };
        assertThat(outcome.pipelineOutcome()).as("pipeline outcome: " + description).contains(expected);
        assertThat(outcome.telemetry().get(0).attemptCount()).as("telemetry attempts: " + description)
                                                             .isEqualTo(outcome.attempts());
        assertThat(outcome.payloadHandoffs()).as("payload handoffs: " + description).isLessThanOrEqualTo(1);
        for (ExchangeRecord exchange : outcome.exchanges()) {
            assertThat(exchange.cancellations().size()).as("cancellations: " + description).isLessThanOrEqualTo(1);
            if (exchange.mode() == HttpTransportMode.ASYNC) {
                assertThat(exchange.terminalSignals()).as("terminal signals: " + description).isLessThanOrEqualTo(1);
            } else {
                assertThat(exchange.closes()).as("closes: " + description).isEqualTo(1);
            }
        }
    }

    /** The response or the exception carries the call's attempt count (Requirement 21.1). */
    private static void assertAttemptMetadata(FaultOutcome outcome, String description) {
        if (outcome.result() instanceof SdkResponse response) {
            assertThat(response.sdkResponseMetadata().attemptCount()).as("response attempts: " + description)
                                                                     .hasValue(outcome.attempts());
            assertThat(response.sdkResponseMetadata().requestId()).as(description).contains(REQUEST_ID);
        } else if (outcome.result() instanceof ResponseBytes<?> bytes
                   && bytes.response() instanceof SdkResponse response) {
            assertThat(response.sdkResponseMetadata().attemptCount()).as("response attempts: " + description)
                                                                     .hasValue(outcome.attempts());
        } else if (outcome.terminal() == Terminal.FAILED && outcome.failure() instanceof SdkException failure) {
            assertThat(failure.numAttempts()).as("exception attempts: " + description).isEqualTo(outcome.attempts());
        }
    }

    private static void assertCategory(FaultOutcome outcome, HttpFailureCategory category) {
        assertThat(((HttpTransportException) outcome.failure()).category()).as(outcome.describe())
                                                                           .isEqualTo(category);
    }

    private static void assertServiceError(FaultOutcome outcome, int status, String errorCode) {
        AwsServiceException error = (AwsServiceException) outcome.failure();
        assertThat(error.statusCode()).as(outcome.describe()).isEqualTo(status);
        assertThat(error.awsErrorDetails().errorCode()).as(outcome.describe()).isEqualTo(errorCode);
        assertThat(error.requestId()).as(outcome.describe()).isEqualTo(REQUEST_ID);
        assertThat(error.sdkResponseMetadata().attemptCount()).as(outcome.describe()).hasValue(outcome.attempts());
    }

    private static void assertObjectPayload(FaultOutcome outcome) {
        ResponseBytes<?> bytes = (ResponseBytes<?>) outcome.result();
        assertThat(bytes.asString(StandardCharsets.UTF_8)).isEqualTo(ServiceFixtures.OBJECT_PAYLOAD);
        assertThat(bytes.response()).isInstanceOf(GetObjectResponse.class);
    }

    /** The pipeline handed the response to the call's transformer exactly {@code expected} times. */
    private static void assertHandoffs(FaultOutcome outcome, int expected) {
        assertThat(outcome.payloadHandoffs()).as("payload handoffs: " + outcome.describe()).isEqualTo(expected);
    }

    private static void assertTimerCancelledOnceIfAsync(FaultOutcome outcome) {
        if (outcome.mode() == HttpTransportMode.ASYNC) {
            assertThat(outcome.scheduledDelays()).hasSize(1);
        }
        assertThat(outcome.unusedTransmissions()).hasSize(1);
    }
}
