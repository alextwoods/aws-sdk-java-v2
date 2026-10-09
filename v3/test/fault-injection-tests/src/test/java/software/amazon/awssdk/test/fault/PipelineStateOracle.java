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

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import software.amazon.awssdk.http.HttpFailureCategory;
import software.amazon.awssdk.http.HttpTransportMode;
import software.amazon.awssdk.test.fault.FaultOutcome.Terminal;
import software.amazon.awssdk.test.fault.RecordingRetryStrategy.Kind;

/**
 * An independent state-machine model of one AWS pipeline call (Property 63), written from the documented contracts
 * only, never from the pipeline's code: the stage order of {@code AwsCallEngine} (endpoint resolution, identity,
 * signing, transmission, response classification, retry decision and backoff), the default classification table of
 * {@code RetryClassifier.defaults()} (non-replayable request first, then cancellation, throttling, transient service
 * errors, transient transport categories, anything else non-retryable), the token rules of {@code RetryStrategy}
 * (one ACQUIRE, every token consumed once by a refresh, a success or a release), the stop order of the token core
 * (non-retryable before max attempts), the retry-hint bound of {@code RetryHints} ({@code min(max(h, b), b + 5 s)}),
 * the cancellation contract of {@code executeAsync} and of the interrupted sync driver, and the documented behavior of
 * each {@link Scenario.Wire} fault of the kit.
 *
 * <p>The oracle consumes the symbolic {@link Scenario} (the generator's intent), not the wire bytes, so it shares no
 * parsing or classification code with the pipeline.
 */
final class PipelineStateOracle {
    /** The documented bound a valid retry hint may extend the computed backoff by (RetryHints.HINT_EXTENSION). */
    static final Duration HINT_EXTENSION = Duration.ofSeconds(5);

    /** The model's states; one transition per stage of an attempt. */
    enum State {
        START_ATTEMPT, RESOLVE_ENDPOINT, RESOLVE_IDENTITY, SIGN, TRANSMIT, DECIDE, BACKOFF, TERMINAL
    }

    /** The documented classification of one attempt failure. */
    enum Classification {
        THROTTLING, TRANSIENT, NON_RETRYABLE
    }

    /** What the caller must observe as the call's failure. */
    enum FailureShape {
        /** {@code HttpTransportException} of {@link Prediction#category()}. */
        TRANSPORT,
        /** {@code AwsServiceException} of {@link Prediction#status()} and {@link Prediction#errorCode()}. */
        SERVICE,
        /** The modeled {@code ResourceNotFoundException}. */
        MODELED_SERVICE,
        /** A client-side {@code SdkException} that is not a service exception (unreadable 2xx). */
        MALFORMED,
        /** An {@code SdkException} reporting a broken response body. */
        RESPONSE_BODY,
        /** An {@code SdkClientException} caused by the kit's injected identity or signer failure. */
        INJECTED_CLIENT,
        /** An {@code EndpointResolutionException} carrying the rule set's injected error. */
        ENDPOINT,
        /** A failure while the response transformer consumed the payload: not retried, any throwable. */
        TRANSFORM,
        /** Async: the call's {@code CancellationException}; sync: SdkClientException caused by the interrupt. */
        CANCELLED
    }

    /** One predicted retry-token ledger entry ({@code delay} and {@code detail} only where the oracle knows them). */
    record LedgerEntry(Kind kind, int attempt, Duration delay, String detail) {
        @Override
        public String toString() {
            return kind + "#" + attempt + (delay == null ? "" : "(" + delay.toMillis() + "ms)")
                   + (detail == null ? "" : "[" + detail + "]");
        }
    }

    /** What one exchange must have done. {@code bytesRead} is null when the request body is not streamed. */
    record ExchangePrediction(TransportFault fault, Long bytesRead, boolean callerCancelled,
                              boolean bodyCancelled, boolean cancelledAtResponseBody) {
    }

    /**
     * The model's prediction. {@code reachable} is false when the scenario's cancellation point is never reached (the
     * call ends first) or a script would run out; such a scenario is not a valid trace and is discarded.
     */
    record Prediction(boolean reachable, String why, Terminal terminal, FailureShape failure,
                      HttpFailureCategory category, int status, String errorCode, int attempts, int transmissions,
                      int identityResolutions, int signings, List<Duration> retryDelays,
                      List<Duration> scheduledDelays, List<LedgerEntry> ledger, List<ExchangePrediction> exchanges,
                      int unusedTransmissions, int afterExecutions, int payloadHandoffs, Terminal telemetry,
                      List<State> path) {
        static Prediction unreachable(String why) {
            return new Prediction(false, why, null, null, null, 0, null, 0, 0, 0, 0, List.of(), List.of(), List.of(),
                                  List.of(), 0, 0, 0, null, List.of());
        }

        @Override
        public String toString() {
            if (!reachable) {
                return "unreachable: " + why;
            }
            return "predicted " + terminal + " " + failure + (category == null ? "" : "(" + category + ")")
                   + (status == 0 ? "" : " status " + status) + (errorCode == null ? "" : " " + errorCode)
                   + ", attempts " + attempts + ", transmissions " + transmissions + ", identities "
                   + identityResolutions + ", signings " + signings + ", delays " + retryDelays + ", scheduled "
                   + scheduledDelays + ", ledger " + ledger + ", unused " + unusedTransmissions
                   + ", afterExecution " + afterExecutions + ", handoffs " + payloadHandoffs + ", telemetry "
                   + telemetry + ", path " + path;
        }
    }

    private PipelineStateOracle() {
    }

    /** Runs the model over {@code scenario}. */
    static Prediction predict(Scenario scenario) {
        return new Run(scenario).run();
    }

    /** One run of the state machine. */
    private static final class Run {
        private final Scenario s;
        private final boolean async;
        private final List<State> path = new ArrayList<>();
        private final List<Duration> delays = new ArrayList<>();
        private final List<LedgerEntry> ledger = new ArrayList<>();
        private final List<ExchangePrediction> exchanges = new ArrayList<>();
        private int attempt;
        private int tx;
        private int ids;
        private int signs;
        private int streamsOpened;
        private int handoffs;
        private boolean transmitted;
        // The current attempt's failure.
        private Classification classification;
        private FailureShape failure;
        private HttpFailureCategory category;
        private int status;
        private String errorCode;
        private Integer hint;
        // The terminal state.
        private Terminal terminal;
        private Terminal telemetry;
        private FailureShape terminalFailure;
        // Why the scenario is not a valid trace, and whether its cancellation point was reached.
        private String unreachable;
        private boolean cancelReached;

        Run(Scenario scenario) {
            this.s = scenario;
            this.async = scenario.mode() == HttpTransportMode.ASYNC;
        }

        Prediction run() {
            State state = State.START_ATTEMPT;
            while (state != State.TERMINAL) {
                path.add(state);
                state = switch (state) {
                    case START_ATTEMPT -> startAttempt();
                    case RESOLVE_ENDPOINT -> resolveEndpoint();
                    case RESOLVE_IDENTITY -> resolveIdentity();
                    case SIGN -> sign();
                    case TRANSMIT -> transmit();
                    case DECIDE -> decide();
                    case BACKOFF -> backoff();
                    case TERMINAL -> throw new IllegalStateException();
                };
                if (state == null) {
                    return Prediction.unreachable(unreachable);
                }
            }
            path.add(State.TERMINAL);
            if (s.cancel() != CancelPoint.NONE && !cancelReached) {
                // The call ends before it parks where the caller cancels it: not a valid trace of that point.
                return Prediction.unreachable("the call ends before its cancellation point " + s.cancel());
            }
            List<Duration> scheduled = async ? delays.stream().filter(d -> !d.isZero()).toList() : List.of();
            int afterExecutions = terminal == Terminal.CANCELLED && async ? 0 : 1;
            return new Prediction(true, null, terminal, terminalFailure, category, status, errorCode, attempt, tx,
                                  ids, signs, List.copyOf(delays), scheduled, List.copyOf(ledger),
                                  List.copyOf(exchanges), s.transmissions().size() - tx, afterExecutions, handoffs,
                                  telemetry, List.copyOf(path));
        }

        private State unreachable(String why) {
            unreachable = why;
            return null;
        }

        private State startAttempt() {
            attempt++;
            transmitted = false;
            classification = null;
            failure = null;
            category = null;
            status = 0;
            errorCode = null;
            hint = null;
            if (attempt == 1) {
                ledger.add(new LedgerEntry(Kind.ACQUIRE, 1, null, null));
            }
            return State.RESOLVE_ENDPOINT;
        }

        private State resolveEndpoint() {
            if (s.endpoint() == EndpointMode.FAIL) {
                return attemptFailed(FailureShape.ENDPOINT, Classification.NON_RETRYABLE);
            }
            return State.RESOLVE_IDENTITY;
        }

        private State resolveIdentity() {
            if (ids >= s.identities().size()) {
                return unreachable("identity script exhausted at attempt " + attempt);
            }
            IdentityStep step = s.identities().get(ids++);
            return switch (step) {
                case RESOLVE -> State.SIGN;
                case FAIL -> attemptFailed(FailureShape.INJECTED_CLIENT, Classification.NON_RETRYABLE);
                case HANG -> s.cancel() == CancelPoint.IDENTITY
                             // Both drivers: the identity wait is cancelled; the call ends cancelled and releases the
                             // attempt's token without a refresh.
                             ? cancelled(attempt)
                             : unreachable("an identity hangs without a cancellation");
            };
        }

        private State sign() {
            if (signs >= s.signings().size()) {
                return unreachable("signer script exhausted at attempt " + attempt);
            }
            SignerStep step = s.signings().get(signs++);
            return step == SignerStep.SIGN ? State.TRANSMIT
                                           : attemptFailed(FailureShape.INJECTED_CLIENT, Classification.NON_RETRYABLE);
        }

        private State transmit() {
            if (tx >= s.transmissions().size()) {
                return unreachable("transport script exhausted at attempt " + attempt);
            }
            Scenario.Step step = s.transmissions().get(tx++);
            TransportFault fault = step.fault(s.op());
            transmitted = true;
            Scenario.Wire wire = step.wire();

            // The request body: every fault but a failed connection streams it, up to its limit; a FAILING_ONCE
            // body's first opened stream fails once more than FAIL_AFTER bytes are read.
            Long bytesRead = null;
            boolean bodyFailed = false;
            if (s.op().streamingInput()) {
                boolean opens = wire != Scenario.Wire.CONNECT_FAILURE && wire != Scenario.Wire.CONNECT_TIMEOUT;
                long limit = wire == Scenario.Wire.PARTIAL_REQUEST ? step.bytes() : FaultBodies.LENGTH;
                long read = opens ? Math.min(limit, FaultBodies.LENGTH) : 0;
                if (opens) {
                    boolean failing = s.body() == BodyKind.FAILING_ONCE && streamsOpened == 0;
                    streamsOpened++;
                    if (failing && limit > FaultBodies.FAIL_AFTER) {
                        read = FaultBodies.FAIL_AFTER;
                        bodyFailed = true;
                    }
                }
                bytesRead = read;
            }
            if (bodyFailed) {
                exchanges.add(new ExchangePrediction(fault, bytesRead, false, false, false));
                category = HttpFailureCategory.REQUEST_TRANSMISSION;
                return attemptFailed(FailureShape.TRANSPORT, Classification.TRANSIENT);
            }

            boolean callerCancelled = false;
            boolean bodyCancelled = false;
            boolean atResponseBody = false;
            State next;
            switch (wire) {
                case OK -> {
                    if (s.op() == Scenario.Op.GET_OBJECT) {
                        handoffs++;
                    }
                    next = succeeded();
                }
                case MALFORMED -> next = attemptFailed(FailureShape.MALFORMED, Classification.NON_RETRYABLE);
                case MODELED_ERROR -> {
                    status = 400;
                    errorCode = "ResourceNotFoundException";
                    next = attemptFailed(FailureShape.MODELED_SERVICE, Classification.NON_RETRYABLE);
                }
                case UNMODELED_ERROR -> {
                    status = 400;
                    errorCode = "FaultInjectedMysteryError";
                    next = attemptFailed(FailureShape.SERVICE, Classification.NON_RETRYABLE);
                }
                case UNAVAILABLE -> {
                    status = 503;
                    hint = step.hintMillis();
                    next = attemptFailed(FailureShape.SERVICE, Classification.TRANSIENT);
                }
                case THROTTLING_CODE -> {
                    status = 400;
                    errorCode = "ThrottlingException";
                    hint = step.hintMillis();
                    next = attemptFailed(FailureShape.SERVICE, Classification.THROTTLING);
                }
                case TOO_MANY_REQUESTS -> {
                    status = 429;
                    hint = step.hintMillis();
                    next = attemptFailed(FailureShape.SERVICE, Classification.THROTTLING);
                }
                case CONNECT_FAILURE -> {
                    category = HttpFailureCategory.CONNECT;
                    next = attemptFailed(FailureShape.TRANSPORT, Classification.TRANSIENT);
                }
                case CONNECT_TIMEOUT, READ_TIMEOUT -> {
                    category = HttpFailureCategory.TIMEOUT;
                    next = attemptFailed(FailureShape.TRANSPORT, Classification.TRANSIENT);
                }
                case PARTIAL_REQUEST -> {
                    category = HttpFailureCategory.REQUEST_TRANSMISSION;
                    next = attemptFailed(FailureShape.TRANSPORT, Classification.TRANSIENT);
                }
                case PARTIAL_RESPONSE -> {
                    if (s.op() == Scenario.Op.GET_OBJECT) {
                        // The 2xx head hands the payload to the transformer; from then on no failure is retried
                        // and the call ends without a refresh.
                        handoffs++;
                        next = terminalWithoutRefresh(FailureShape.TRANSFORM);
                    } else {
                        next = attemptFailed(FailureShape.RESPONSE_BODY, Classification.TRANSIENT);
                    }
                }
                case HANG -> {
                    if (s.cancel() != CancelPoint.TRANSMISSION) {
                        next = unreachable("a transmission hangs without a cancellation");
                    } else if (async) {
                        callerCancelled = true;
                        next = cancelled(attempt);
                    } else {
                        // The interrupted blocking exchange fails as a CANCELLED transport failure, which the
                        // classifier never retries; the call fails (it is not a pipeline cancellation).
                        cancelReached = true;
                        category = HttpFailureCategory.CANCELLED;
                        next = attemptFailed(FailureShape.TRANSPORT, Classification.NON_RETRYABLE);
                    }
                }
                case HANG_BODY -> {
                    if (s.cancel() != CancelPoint.RESPONSE_BODY || !async) {
                        next = unreachable("a response body hangs without an async cancellation");
                    } else {
                        if (s.op() == Scenario.Op.GET_OBJECT) {
                            handoffs++;
                            bodyCancelled = true;
                        }
                        atResponseBody = true;
                        next = cancelled(attempt);
                    }
                }
                default -> throw new IllegalStateException("Unknown wire " + wire);
            }
            exchanges.add(new ExchangePrediction(fault, bytesRead, callerCancelled, bodyCancelled, atResponseBody));
            return next;
        }

        /** Records the attempt's failure; the request-replayability row of the classification table comes first. */
        private State attemptFailed(FailureShape shape, Classification documented) {
            failure = shape;
            boolean replayable = !transmitted || s.body() != BodyKind.ONE_SHOT;
            classification = replayable ? documented : Classification.NON_RETRYABLE;
            return State.DECIDE;
        }

        private State decide() {
            if (classification == Classification.NON_RETRYABLE) {
                ledger.add(new LedgerEntry(Kind.STOP, attempt, null, "NON_RETRYABLE"));
                return failed();
            }
            if (attempt >= s.retry().maxAttempts()) {
                ledger.add(new LedgerEntry(Kind.STOP, attempt, null, "MAX_ATTEMPTS_REACHED"));
                return failed();
            }
            Duration backoff = classification == Classification.THROTTLING ? s.retry().throttlingBackoff()
                                                                            : s.retry().backoff();
            Duration delay = backoff;
            if (hint != null) {
                Duration h = Duration.ofMillis(hint);
                Duration atLeast = h.compareTo(backoff) > 0 ? h : backoff;
                Duration bound = backoff.plus(HINT_EXTENSION);
                delay = atLeast.compareTo(bound) < 0 ? atLeast : bound;
            }
            delays.add(delay);
            ledger.add(new LedgerEntry(Kind.RETRY, attempt, delay, classification.name()));
            return State.BACKOFF;
        }

        private State backoff() {
            if (s.cancel() == CancelPoint.RETRY_DELAY) {
                // Cancelled while waiting: the token the retry granted (attempt + 1) is released, no attempt starts.
                return cancelled(attempt + 1);
            }
            return State.START_ATTEMPT;
        }

        private State succeeded() {
            ledger.add(new LedgerEntry(Kind.SUCCESS, attempt, null, null));
            terminal = Terminal.SUCCEEDED;
            telemetry = Terminal.SUCCEEDED;
            terminalFailure = null;
            return State.TERMINAL;
        }

        private State failed() {
            terminal = Terminal.FAILED;
            telemetry = Terminal.FAILED;
            terminalFailure = failure;
            if (failure != FailureShape.TRANSPORT) {
                category = null;
            }
            return State.TERMINAL;
        }

        private State terminalWithoutRefresh(FailureShape shape) {
            ledger.add(new LedgerEntry(Kind.RELEASE, attempt, null, null));
            terminal = Terminal.FAILED;
            telemetry = Terminal.FAILED;
            terminalFailure = shape;
            return State.TERMINAL;
        }

        private State cancelled(int tokenAttempt) {
            cancelReached = true;
            ledger.add(new LedgerEntry(Kind.RELEASE, tokenAttempt, null, null));
            terminal = Terminal.CANCELLED;
            telemetry = Terminal.CANCELLED;
            terminalFailure = FailureShape.CANCELLED;
            category = null;
            status = 0;
            errorCode = null;
            return State.TERMINAL;
        }
    }
}
