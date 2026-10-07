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

import java.io.IOException;
import java.io.UncheckedIOException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.concurrent.CancellationException;
import software.amazon.awssdk.core.exception.SdkServiceException;
import software.amazon.awssdk.core.exception.SdkTimeoutException;
import software.amazon.awssdk.http.HttpTransportException;
import software.amazon.awssdk.http.SdkHttpResponse;
import software.amazon.awssdk.telemetry.spi.AttemptRecord;
import software.amazon.awssdk.telemetry.spi.CallRecord;
import software.amazon.awssdk.telemetry.spi.TelemetryFailure;
import software.amazon.awssdk.telemetry.spi.TelemetryOutcome;
import software.amazon.awssdk.telemetry.spi.TelemetryPublisher;

/**
 * The telemetry state of one call whose plan has {@link PipelineTelemetry} (Requirements 21.2-21.4, 21.14, 21.15):
 * the call's and the current attempt's start times and the attempts' terminal records.
 *
 * <p>{@link AttemptState#complete()} reports each attempt's end exactly once, through {@link #attemptEnded}, which
 * emits its {@link AttemptRecord}; the call's terminal stage reports the call's end through {@link #callEnded}, which
 * emits its {@link CallRecord} once, after every attempt record. Each publisher callback is isolated: a callback
 * that throws is delivered to the configuration's telemetry failure channel and never reaches the call, whose result
 * stays its own; the remaining publishers still run.
 *
 * <p>Confined to the call's driver (attempts end and the call's terminal stage runs on it).
 */
final class CallTelemetry {
    private final PipelineTelemetry plan;
    private final long callStart;
    private final List<AttemptRecord> attempts = new ArrayList<>(2);
    private long attemptStart;
    private boolean recorded;

    CallTelemetry(PipelineTelemetry plan, long callStart) {
        this.plan = plan;
        this.callStart = callStart;
    }

    /** The next attempt starts now. */
    void attemptStarted() {
        attemptStart = System.nanoTime();
    }

    /**
     * The attempt ended: emits its record. Its outcome is {@link TelemetryOutcome#FAILED} if it recorded a failure,
     * else the call's terminal outcome if the call already ended (cancelled or failed under the attempt), else
     * {@link TelemetryOutcome#SUCCEEDED}.
     */
    void attemptEnded(AttemptState attempt, TerminalState<?> terminal) {
        Duration duration = Duration.ofNanos(System.nanoTime() - attemptStart);
        Throwable failure = attempt.failure().orElse(null);
        TelemetryOutcome outcome;
        if (failure != null) {
            outcome = TelemetryOutcome.FAILED;
        } else {
            outcome = outcome(terminal.status());
            if (outcome == TelemetryOutcome.FAILED) {
                failure = terminal.failure().orElse(null);
            }
        }
        OptionalInt status = attempt.response().map(SdkHttpResponse::statusCode).map(OptionalInt::of)
                                    .orElse(OptionalInt.empty());
        AttemptRecord record = new AttemptRecord(plan.service(), plan.operation(), attempt.number(), outcome,
                                                 plan.httpImplementation(), plan.retryMode(), duration, status,
                                                 outcome == TelemetryOutcome.FAILED ? errorType(failure)
                                                                                    : Optional.empty());
        attempts.add(record);
        for (TelemetryPublisher publisher : plan.publishers()) {
            try {
                publisher.attemptCompleted(record);
            } catch (VirtualMachineError fatal) {
                throw fatal;
            } catch (Throwable callbackFailure) {
                report(publisher, TelemetryFailure.Callback.ATTEMPT_COMPLETED, callbackFailure);
            }
        }
    }

    /**
     * The call ended with {@code terminal}'s outcome and its attempts ended: emits its record, at most once.
     */
    void callEnded(TerminalState<?> terminal) {
        if (recorded || !terminal.isTerminal()) {
            return;
        }
        recorded = true;
        Duration duration = Duration.ofNanos(System.nanoTime() - callStart);
        TelemetryOutcome outcome = outcome(terminal.status());
        CallRecord record = new CallRecord(plan.service(), plan.operation(), outcome, attempts.size(),
                                           plan.httpImplementation(), plan.retryMode(), duration,
                                           outcome == TelemetryOutcome.FAILED
                                           ? errorType(terminal.failure().orElse(null)) : Optional.empty(),
                                           attempts);
        for (TelemetryPublisher publisher : plan.publishers()) {
            try {
                publisher.callCompleted(record);
            } catch (VirtualMachineError fatal) {
                throw fatal;
            } catch (Throwable callbackFailure) {
                report(publisher, TelemetryFailure.Callback.CALL_COMPLETED, callbackFailure);
            }
        }
    }

    /** Delivers a callback failure to the telemetry failure channel; a failing channel is ignored. */
    private void report(TelemetryPublisher publisher, TelemetryFailure.Callback callback, Throwable cause) {
        try {
            plan.failureListener().telemetryFailed(new TelemetryFailure(publisher, callback, plan.service(),
                                                                        plan.operation(), cause));
        } catch (VirtualMachineError fatal) {
            throw fatal;
        } catch (Throwable ignored) {
            // The channel is the last resort: its own failure cannot change the call's result either.
        }
    }

    private static TelemetryOutcome outcome(TerminalState.Status status) {
        return switch (status) {
            case FAILED -> TelemetryOutcome.FAILED;
            case CANCELLED -> TelemetryOutcome.CANCELLED;
            case RUNNING, SUCCEEDED, PRESIGNED -> TelemetryOutcome.SUCCEEDED;
        };
    }

    /**
     * The V2 error type of {@code failure} ({@code SdkErrorType}): {@code Throttling} or {@code ServerError} for a
     * service error, {@code ConfiguredTimeout} for a timeout, {@code IO} for a transport or I/O failure, else
     * {@code Other}. Empty for a cancellation, which the outcome already reports.
     */
    static Optional<String> errorType(Throwable failure) {
        if (failure == null || failure instanceof CancellationException) {
            return Optional.empty();
        }
        if (failure instanceof SdkServiceException service) {
            return Optional.of(service.isThrottlingException() ? "Throttling" : "ServerError");
        }
        if (failure instanceof SdkTimeoutException) {
            return Optional.of("ConfiguredTimeout");
        }
        // A bounded walk of the cause chain, so a cyclic chain cannot loop.
        Throwable cause = failure;
        for (int depth = 0; cause != null && depth < 8; depth++, cause = cause.getCause()) {
            if (cause instanceof HttpTransportException || cause instanceof IOException
                || cause instanceof UncheckedIOException) {
                return Optional.of("IO");
            }
        }
        return Optional.of("Other");
    }
}
