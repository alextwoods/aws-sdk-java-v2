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

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CancellationException;
import net.jqwik.api.Arbitraries;
import net.jqwik.api.Arbitrary;
import net.jqwik.api.Assume;
import net.jqwik.api.Combinators;
import net.jqwik.api.EdgeCasesMode;
import net.jqwik.api.ForAll;
import net.jqwik.api.Property;
import net.jqwik.api.Provide;
import net.jqwik.api.Tuple;
import net.jqwik.api.statistics.Statistics;
import software.amazon.awssdk.awscore.exception.AwsServiceException;
import software.amazon.awssdk.core.ResponseBytes;
import software.amazon.awssdk.core.SdkResponse;
import software.amazon.awssdk.core.async.AsyncResponseTransformer;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.core.exception.SdkException;
import software.amazon.awssdk.core.sync.ResponseTransformer;
import software.amazon.awssdk.endpoints.EndpointResolutionException;
import software.amazon.awssdk.http.CancellationReason;
import software.amazon.awssdk.http.HttpTransportException;
import software.amazon.awssdk.http.HttpTransportMode;
import software.amazon.awssdk.services.dynamodb.model.ResourceNotFoundException;
import software.amazon.awssdk.telemetry.spi.TelemetryOutcome;
import software.amazon.awssdk.test.fault.FaultOutcome.Terminal;
import software.amazon.awssdk.test.fault.PipelineStateOracle.ExchangePrediction;
import software.amazon.awssdk.test.fault.PipelineStateOracle.LedgerEntry;
import software.amazon.awssdk.test.fault.PipelineStateOracle.Prediction;
import software.amazon.awssdk.test.fault.RecordingRetryStrategy.Kind;
import software.amazon.awssdk.test.fault.Scenario.Op;
import software.amazon.awssdk.test.fault.Scenario.Step;
import software.amazon.awssdk.test.fault.Scenario.Wire;

/**
 * Property 63 (Requirements 19.7, 19.8): fault outcomes follow pipeline state. For any bounded, deterministic fault /
 * retry / cancellation trace, the outcome of the real pipeline equals the prediction of an independent pipeline
 * state-machine model ({@link PipelineStateOracle}).
 *
 * <p>Each try generates a {@link Scenario}: an operation (DynamoDB ListTables, MediaStore Data PutObject with a
 * replayable, one-shot or once-failing streaming body, or GetObject with a streaming output), a driver, a retry
 * strategy (1-4 attempts, zero or fixed transient and throttling backoff) and per-attempt scripts of transport faults
 * (successes, malformed successes, modeled, unmodeled, throttling and transient service errors with and without
 * {@code x-amz-retry-after} hints, connection failures, timeouts, partial request and response bodies), identity and
 * signer outcomes and endpoint resolution; or a retryable prefix followed by a cancellation while the call waits for
 * an identity, a transmission, a response body or a retry delay. The trace runs through {@link FaultHarness} (the
 * real {@code AwsCallEngine} of a generated service, every dependency a controllable fake, virtual async time).
 *
 * <p>The property requires the observed terminal outcome and failure (type, transport category, status, error code),
 * attempt, transmission, identity and signing counts, the retry decision delays, the delays requested of the async
 * scheduler (sync: lower bounds of the measured gaps), the retry-token ledger (kinds, attempts, delays,
 * classifications and stop reasons), every exchange's fault, streamed request bytes and cancellation, the unused
 * script, {@code afterExecution} and payload hand-off counts, the one telemetry record, the attempt count in the
 * response or exception metadata, to equal the prediction; and every call to release every resource and break no
 * exactly-once contract (the harness's leaks and violations are empty).
 *
 * <p>Generation is bounded: at most four attempts, sub-50 ms real sync delays (virtual async delays may reach the
 * five-second hint bound), one call per try.
 */
class FaultOutcomeOraclePropertyTest {
    /** A backoff long enough that an interrupted sync call is still sleeping (the RETRY_DELAY cancellation point). */
    private static final Duration SLOW = Duration.ofMinutes(1);

    @Property(tries = 1_000, edgeCases = EdgeCasesMode.NONE, maxDiscardRatio = 10)
    void faultOutcomesFollowPipelineState(@ForAll("scenarios") Scenario scenario) {
        Prediction predicted = PipelineStateOracle.predict(scenario);
        Assume.that(predicted.reachable());
        Statistics.label("op").collect(scenario.op(), scenario.mode());
        Statistics.label("cancel").collect(scenario.cancel());
        Statistics.label("terminal").collect(predicted.terminal(), predicted.failure());
        Statistics.label("attempts").collect(predicted.attempts());

        FaultOutcome outcome = run(scenario);
        String description = scenario + "\n" + predicted + "\n" + outcome.describe();

        assertThat(outcome.terminal()).as("terminal: " + description).isEqualTo(predicted.terminal());
        assertFailure(outcome, predicted, description);
        assertThat(outcome.attempts()).as("attempts: " + description).isEqualTo(predicted.attempts());
        assertThat(outcome.transmissions()).as("transmissions: " + description).isEqualTo(predicted.transmissions());
        assertThat(outcome.identityResolutions()).as("identity resolutions: " + description)
                                                 .isEqualTo(predicted.identityResolutions());
        assertThat(outcome.signings()).as("signings: " + description).isEqualTo(predicted.signings());
        assertDelays(outcome, predicted, description);
        assertThat(ledger(outcome)).as("retry token ledger: " + description).isEqualTo(predicted.ledger());
        assertExchanges(outcome, predicted, description);
        assertThat(outcome.unusedTransmissions()).as("unused transmissions: " + description)
                                                 .hasSize(predicted.unusedTransmissions());
        assertExactlyOnce(outcome, predicted, description);
        assertThat(outcome.leaks()).as("leaks: " + description).isEmpty();
        assertThat(outcome.violations()).as("violations: " + description).isEmpty();
        assertMetadata(outcome, description);
    }

    // ---- Generation ----

    @Provide
    Arbitrary<Scenario> scenarios() {
        return Combinators.combine(Arbitraries.of(Op.values()), Arbitraries.of(HttpTransportMode.values()))
                          .as(Tuple::of)
                          .flatMap(t -> scenario(t.get1(), t.get2()));
    }

    private static Arbitrary<Scenario> scenario(Op op, HttpTransportMode mode) {
        boolean async = mode == HttpTransportMode.ASYNC;
        Arbitrary<CancelPoint> cancels = async
            ? Arbitraries.frequency(Tuple.of(8, CancelPoint.NONE), Tuple.of(1, CancelPoint.IDENTITY),
                                    Tuple.of(1, CancelPoint.TRANSMISSION), Tuple.of(1, CancelPoint.RESPONSE_BODY),
                                    Tuple.of(1, CancelPoint.RETRY_DELAY))
            : Arbitraries.frequency(Tuple.of(8, CancelPoint.NONE), Tuple.of(1, CancelPoint.IDENTITY),
                                    Tuple.of(1, CancelPoint.TRANSMISSION), Tuple.of(1, CancelPoint.RETRY_DELAY));
        Arbitrary<BodyKind> bodies = op.streamingInput()
                                     ? Arbitraries.of(BodyKind.REPLAYABLE, BodyKind.ONE_SHOT, BodyKind.FAILING_ONCE)
                                     : Arbitraries.just(BodyKind.NONE);
        return Combinators.combine(cancels, bodies).as(Tuple::of).flatMap(t -> {
            CancelPoint cancel = t.get1();
            BodyKind body = t.get2();
            return cancel == CancelPoint.NONE ? uncancelled(op, mode, body) : cancelled(op, mode, body, cancel);
        });
    }

    /** Free per-attempt scripts, as long as the attempt bound, so no script runs out. */
    private static Arbitrary<Scenario> uncancelled(Op op, HttpTransportMode mode, BodyKind body) {
        return retrySettings().flatMap(retry -> {
            int attempts = retry.maxAttempts();
            Arbitrary<List<Step>> transmissions = step(op, mode, false).list().ofSize(attempts);
            Arbitrary<List<IdentityStep>> identities =
                Arbitraries.frequency(Tuple.of(24, IdentityStep.RESOLVE), Tuple.of(1, IdentityStep.FAIL))
                           .list().ofSize(attempts);
            Arbitrary<List<SignerStep>> signings =
                Arbitraries.frequency(Tuple.of(24, SignerStep.SIGN), Tuple.of(1, SignerStep.FAIL))
                           .list().ofSize(attempts);
            Arbitrary<EndpointMode> endpoint =
                Arbitraries.frequency(Tuple.of(24, EndpointMode.RESOLVE), Tuple.of(1, EndpointMode.FAIL));
            return Combinators.combine(transmissions, identities, signings, endpoint)
                              .as((tx, ids, signs, ep) -> new Scenario(op, mode, tx, ids, signs, ep, body,
                                                                       CancelPoint.NONE, retry));
        });
    }

    /** A retryable prefix, then the step the call is cancelled at (or, for RETRY_DELAY, whose retry it waits out). */
    private static Arbitrary<Scenario> cancelled(Op op, HttpTransportMode mode, BodyKind body, CancelPoint cancel) {
        if (cancel == CancelPoint.RETRY_DELAY) {
            return Combinators.combine(Arbitraries.integers().between(2, 4), step(op, mode, true))
                              .as((attempts, first) -> new Scenario(
                                  op, mode, List.of(first, Step.of(Wire.OK)), repeat(IdentityStep.RESOLVE, 2),
                                  repeat(SignerStep.SIGN, 2), EndpointMode.RESOLVE, body, cancel,
                                  new RetrySettings(attempts, SLOW, SLOW)));
        }
        return retrySettings().flatMap(retry -> {
            int maxPrefix = Math.min(retry.maxAttempts() - 1, 2);
            return Arbitraries.integers().between(0, maxPrefix).flatMap(
                prefixLength -> step(op, mode, true).list().ofSize(prefixLength).map(prefix -> {
                    int n = prefix.size();
                    List<Step> tx = new ArrayList<>(prefix);
                    List<IdentityStep> ids = new ArrayList<>(repeat(IdentityStep.RESOLVE, n));
                    switch (cancel) {
                        case IDENTITY -> {
                            ids.add(IdentityStep.HANG);
                            tx.add(Step.of(Wire.OK));
                        }
                        case TRANSMISSION -> {
                            ids.add(IdentityStep.RESOLVE);
                            tx.add(Step.of(Wire.HANG));
                            tx.add(Step.of(Wire.OK));
                        }
                        case RESPONSE_BODY -> {
                            ids.add(IdentityStep.RESOLVE);
                            tx.add(Step.of(Wire.HANG_BODY));
                            tx.add(Step.of(Wire.OK));
                        }
                        default -> throw new IllegalStateException("Not a cancellation point: " + cancel);
                    }
                    return new Scenario(op, mode, tx, ids, repeat(SignerStep.SIGN, n + 1), EndpointMode.RESOLVE,
                                        body, cancel, retry);
                }));
        });
    }

    private static Arbitrary<RetrySettings> retrySettings() {
        return Combinators.combine(Arbitraries.integers().between(1, 4),
                                   Arbitraries.of(Duration.ZERO, Duration.ofMillis(10)),
                                   Arbitraries.of(Duration.ZERO, Duration.ofMillis(30)))
                          .as(RetrySettings::new);
    }

    /** One transmission's fault for {@code op}; {@code retryableOnly} restricts it to documented retryable faults. */
    private static Arbitrary<Step> step(Op op, HttpTransportMode mode, boolean retryableOnly) {
        List<Wire> wires = new ArrayList<>(List.of(Wire.UNAVAILABLE, Wire.TOO_MANY_REQUESTS, Wire.CONNECT_FAILURE,
                                                   Wire.CONNECT_TIMEOUT, Wire.READ_TIMEOUT));
        if (op != Op.GET_OBJECT) {
            wires.add(Wire.PARTIAL_RESPONSE);
        }
        if (op == Op.LIST_TABLES) {
            wires.add(Wire.THROTTLING_CODE);
        }
        if (op == Op.PUT_OBJECT) {
            wires.add(Wire.PARTIAL_REQUEST);
        }
        if (!retryableOnly) {
            wires.add(Wire.OK);
            wires.add(Wire.OK);
            if (op == Op.GET_OBJECT) {
                wires.add(Wire.PARTIAL_RESPONSE);
            }
            if (op == Op.LIST_TABLES) {
                wires.addAll(List.of(Wire.MALFORMED, Wire.MODELED_ERROR, Wire.UNMODELED_ERROR));
            }
        }
        // Real sync sleeps stay short; the async clock is virtual, so a hint may exceed the five-second bound.
        Arbitrary<Integer> hints = (mode == HttpTransportMode.ASYNC ? Arbitraries.of(5, 40, 60_000)
                                                                    : Arbitraries.of(5, 40)).injectNull(0.6);
        return Arbitraries.of(wires).flatMap(wire -> switch (wire) {
            case UNAVAILABLE, THROTTLING_CODE, TOO_MANY_REQUESTS -> hints.map(h -> new Step(wire, h, 0));
            case PARTIAL_REQUEST -> Arbitraries.of(0, 10, FaultBodies.FAIL_AFTER, 30).map(b -> new Step(wire, null, b));
            default -> Arbitraries.just(Step.of(wire));
        });
    }

    private static <T> List<T> repeat(T value, int n) {
        return Collections.nCopies(n, value);
    }

    // ---- Execution ----

    private static FaultOutcome run(Scenario scenario) {
        try (FaultHarness harness = FaultHarness.builder(scenario.op() == Op.LIST_TABLES ? DYNAMODB : MEDIASTORE_DATA)
                                                .trace(scenario.trace())
                                                .mode(scenario.mode())
                                                .build()) {
            return switch (scenario.op()) {
                case LIST_TABLES -> harness.execute(ServiceFixtures.LIST_TABLES, ServiceFixtures.listTablesRequest());
                case PUT_OBJECT -> harness.execute(ServiceFixtures.PUT_OBJECT, ServiceFixtures.putObjectRequest());
                case GET_OBJECT -> harness.executeStreaming(ServiceFixtures.GET_OBJECT,
                                                            ServiceFixtures.getObjectRequest(),
                                                            ResponseTransformer.toBytes(),
                                                            AsyncResponseTransformer::toBytes);
            };
        }
    }

    // ---- Comparison ----

    private static void assertFailure(FaultOutcome outcome, Prediction predicted, String description) {
        Throwable failure = outcome.failure();
        if (predicted.failure() == null) {
            assertThat(failure).as("failure: " + description).isNull();
            assertThat(outcome.result()).as("result: " + description).isNotNull();
            if (outcome.result() instanceof ResponseBytes<?> bytes) {
                assertThat(bytes.asString(StandardCharsets.UTF_8)).as(description)
                                                                 .isEqualTo(ServiceFixtures.OBJECT_PAYLOAD);
            }
            return;
        }
        assertThat(failure).as("failure: " + description).isNotNull();
        switch (predicted.failure()) {
            case TRANSPORT -> {
                assertThat(failure).as(description).isInstanceOf(HttpTransportException.class);
                assertThat(((HttpTransportException) failure).category()).as("category: " + description)
                                                                         .isEqualTo(predicted.category());
            }
            case SERVICE, MODELED_SERVICE -> {
                assertThat(failure).as(description).isInstanceOf(AwsServiceException.class);
                if (predicted.failure() == PipelineStateOracle.FailureShape.MODELED_SERVICE) {
                    assertThat(failure).as(description).isInstanceOf(ResourceNotFoundException.class);
                } else {
                    assertThat(failure).as(description).isNotInstanceOf(ResourceNotFoundException.class);
                }
                AwsServiceException service = (AwsServiceException) failure;
                assertThat(service.statusCode()).as("status: " + description).isEqualTo(predicted.status());
                if (predicted.errorCode() != null) {
                    assertThat(service.awsErrorDetails().errorCode()).as("error code: " + description)
                                                                     .isEqualTo(predicted.errorCode());
                }
                assertThat(service.requestId()).as("request ID: " + description).isEqualTo(REQUEST_ID);
            }
            case MALFORMED, RESPONSE_BODY -> assertThat(failure).as(description).isInstanceOf(SdkException.class)
                                                                .isNotInstanceOf(AwsServiceException.class);
            case INJECTED_CLIENT -> assertThat(failure).as(description).isInstanceOf(SdkClientException.class)
                                                       .hasCauseInstanceOf(FaultInjectedException.class);
            case ENDPOINT -> assertThat(failure).as(description).isInstanceOf(EndpointResolutionException.class)
                                                .hasMessageContaining(FaultEndpoints.INJECTED_ERROR);
            case TRANSFORM -> {
                // Any failure: the transformer, not the pipeline, owns the payload's failure.
            }
            case CANCELLED -> {
                if (outcome.mode() == HttpTransportMode.ASYNC) {
                    assertThat(failure).as(description).isInstanceOf(CancellationException.class);
                } else {
                    assertThat(failure).as(description).isInstanceOf(SdkClientException.class)
                                       .hasCauseInstanceOf(InterruptedException.class);
                }
            }
            default -> throw new IllegalStateException("Unknown failure shape " + predicted.failure());
        }
    }

    /** Decision delays exactly; async scheduler requests exactly; sync measured gaps at least the decided delay. */
    private static void assertDelays(FaultOutcome outcome, Prediction predicted, String description) {
        assertThat(outcome.retryDelays()).as("retry decision delays: " + description)
                                         .isEqualTo(predicted.retryDelays());
        assertThat(outcome.scheduledDelays()).as("scheduled delays: " + description)
                                             .isEqualTo(predicted.scheduledDelays());
        if (outcome.mode() == HttpTransportMode.SYNC) {
            List<Duration> gaps = outcome.attemptGaps();
            assertThat(gaps).as("sync gaps: " + description)
                            .hasSize(Math.max(0, predicted.attempts() - 1));
            for (int i = 0; i < gaps.size(); i++) {
                assertThat(gaps.get(i)).as("sync gap " + i + ": " + description)
                                       .isGreaterThanOrEqualTo(predicted.retryDelays().get(i));
            }
        }
    }

    /** The observed ledger in the oracle's projection: delays of retries, classifications and stop reasons. */
    private static List<LedgerEntry> ledger(FaultOutcome outcome) {
        return outcome.ledger().stream()
                      .map(e -> new LedgerEntry(e.kind(), e.attempt(), e.kind() == Kind.RETRY ? e.delay() : null,
                                                e.kind() == Kind.RETRY || e.kind() == Kind.STOP ? e.detail() : null))
                      .toList();
    }

    private static void assertExchanges(FaultOutcome outcome, Prediction predicted, String description) {
        List<ExchangeRecord> ran = outcome.exchanges().stream().filter(e -> e.fault() != null).toList();
        assertThat(ran.stream().map(ExchangeRecord::fault).toList()).as("exchange faults: " + description)
            .isEqualTo(predicted.exchanges().stream().map(ExchangePrediction::fault).toList());
        for (int i = 0; i < ran.size(); i++) {
            ExchangeRecord exchange = ran.get(i);
            ExchangePrediction expected = predicted.exchanges().get(i);
            String what = "exchange " + i + ": " + description;
            if (expected.bytesRead() != null) {
                assertThat(exchange.requestBytesRead()).as("request bytes of " + what)
                                                       .isEqualTo(expected.bytesRead());
            }
            if (expected.callerCancelled()) {
                assertThat(exchange.cancellations()).as("cancellation of " + what)
                                                    .containsExactly(CancellationReason.CALLER_CANCELLED);
            } else if (expected.cancelledAtResponseBody()) {
                // The cancellation reaches the transport once: the exchange, or the body subscription.
                assertThat(exchange.cancellations().size() + exchange.bodyCancels()).as("cancellation of " + what)
                                                                                    .isGreaterThanOrEqualTo(1);
                assertThat(exchange.cancellations()).as(what).hasSizeLessThanOrEqualTo(1);
                assertThat(exchange.bodyCancels()).as(what).isLessThanOrEqualTo(1);
            } else {
                assertThat(exchange.cancellations()).as("cancellation of " + what).isEmpty();
            }
            if (expected.bodyCancelled()) {
                assertThat(exchange.bodyCancels()).as("body cancellation of " + what).isEqualTo(1);
            }
            if (exchange.mode() == HttpTransportMode.SYNC) {
                assertThat(exchange.closes()).as("closes of " + what).isEqualTo(1);
            } else {
                assertThat(exchange.terminalSignals()).as("terminal signals of " + what).isLessThanOrEqualTo(1);
            }
        }
    }

    private static void assertExactlyOnce(FaultOutcome outcome, Prediction predicted, String description) {
        assertThat(outcome.afterExecutions()).as("afterExecution: " + description)
                                             .isEqualTo(predicted.afterExecutions());
        assertThat(outcome.payloadHandoffs()).as("payload handoffs: " + description)
                                             .isEqualTo(predicted.payloadHandoffs());
        assertThat(outcome.telemetry()).as("telemetry records: " + description).hasSize(1);
        TelemetryOutcome expected = switch (predicted.telemetry()) {
            case SUCCEEDED -> TelemetryOutcome.SUCCEEDED;
            case CANCELLED -> TelemetryOutcome.CANCELLED;
            default -> TelemetryOutcome.FAILED;
        };
        assertThat(outcome.pipelineOutcome()).as("pipeline outcome: " + description).contains(expected);
        assertThat(outcome.telemetry().get(0).attemptCount()).as("telemetry attempts: " + description)
                                                             .isEqualTo(predicted.attempts());
    }

    /** The response or the mapped exception carries the call's attempt count (and the success its request ID). */
    private static void assertMetadata(FaultOutcome outcome, String description) {
        Object result = outcome.result();
        SdkResponse response = result instanceof SdkResponse r ? r
                               : result instanceof ResponseBytes<?> b && b.response() instanceof SdkResponse r ? r
                               : null;
        if (response != null) {
            assertThat(response.sdkResponseMetadata().attemptCount()).as("response attempts: " + description)
                                                                     .hasValue(outcome.attempts());
            assertThat(response.sdkResponseMetadata().requestId()).as("request ID: " + description)
                                                                  .contains(REQUEST_ID);
        } else if (outcome.terminal() == Terminal.FAILED && outcome.failure() instanceof SdkException failure) {
            assertThat(failure.numAttempts()).as("exception attempts: " + description).isEqualTo(outcome.attempts());
        }
    }
}
