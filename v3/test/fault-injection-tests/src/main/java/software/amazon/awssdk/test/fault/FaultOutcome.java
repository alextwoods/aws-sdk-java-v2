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
import java.util.List;
import java.util.Optional;
import software.amazon.awssdk.http.HttpTransportMode;
import software.amazon.awssdk.telemetry.spi.CallRecord;
import software.amazon.awssdk.telemetry.spi.TelemetryOutcome;

/**
 * Everything observed about one faulted call (Requirement 19.8), for comparison with an expectation or an oracle.
 *
 * @param trace the trace that ran
 * @param mode the driver
 * @param terminal the outcome the caller observed
 * @param result the call's result, if it succeeded
 * @param failure the failure the caller observed, if it failed or was cancelled
 * @param attempts the attempts the pipeline began ({@code beforeAttempt})
 * @param transmissions the transmissions that ran a scripted fault
 * @param identityResolutions the identity resolutions the pipeline started
 * @param signings the signings the pipeline started
 * @param retryDelays the delay of every retry decision, in order: what the driver must apply
 * @param scheduledDelays the delays the async driver requested of the retry scheduler, in order (empty for sync)
 * @param attemptGaps the real time between consecutive attempts (sync: the sleep the driver applied)
 * @param ledger the retry token ledger
 * @param exchanges every exchange of the transport
 * @param afterExecutions how many times {@code afterExecution} ran (one, except for a cancelled call: zero)
 * @param payloadHandoffs how many times the pipeline handed the response to a response transformer
 *     ({@link TransformerProbe#handoffs()}): at most one, and zero unless the call ran a counted streaming output
 * @param telemetry the terminal telemetry call records (exactly one)
 * @param leaks resources left unreleased: exchanges, request bodies, pending timers
 * @param violations contract violations: duplicate completions or signals, unscripted uses, token accounting errors,
 *     a cancelled provider-owned identity future, or a call that did not complete in time
 * @param unusedTransmissions scripted transmissions the call did not reach
 */
public record FaultOutcome(FaultTrace trace, HttpTransportMode mode, Terminal terminal, Object result,
                           Throwable failure, int attempts, int transmissions, int identityResolutions, int signings,
                           List<Duration> retryDelays, List<Duration> scheduledDelays, List<Duration> attemptGaps,
                           List<RecordingRetryStrategy.Entry> ledger, List<ExchangeRecord> exchanges,
                           int afterExecutions, int payloadHandoffs, List<CallRecord> telemetry, List<String> leaks,
                           List<String> violations, List<TransportFault> unusedTransmissions) {

    /** How a call ended, as its caller observed it. */
    public enum Terminal {
        SUCCEEDED,
        FAILED,
        CANCELLED,
        /** The call did not complete within the harness's bound; always a test failure. */
        DID_NOT_COMPLETE
    }

    public FaultOutcome {
        retryDelays = List.copyOf(retryDelays);
        scheduledDelays = List.copyOf(scheduledDelays);
        attemptGaps = List.copyOf(attemptGaps);
        ledger = List.copyOf(ledger);
        exchanges = List.copyOf(exchanges);
        telemetry = List.copyOf(telemetry);
        leaks = List.copyOf(leaks);
        violations = List.copyOf(violations);
        unusedTransmissions = List.copyOf(unusedTransmissions);
    }

    /** The pipeline's own terminal outcome, from its one telemetry record, if it emitted one. */
    public Optional<TelemetryOutcome> pipelineOutcome() {
        return telemetry.size() == 1 ? Optional.of(telemetry.get(0).outcome()) : Optional.empty();
    }

    /** True if the call released every resource and broke no contract. */
    public boolean clean() {
        return leaks.isEmpty() && violations.isEmpty();
    }

    /** A multi-line rendering for assertion messages. */
    public String describe() {
        return "trace " + trace.describe() + " on " + mode
               + "\n  terminal " + terminal + (failure == null ? "" : " " + failure)
               + "\n  attempts " + attempts + ", transmissions " + transmissions + ", identities "
               + identityResolutions + ", signings " + signings
               + "\n  retry delays " + retryDelays + ", scheduled " + scheduledDelays + ", gaps " + attemptGaps
               + "\n  ledger " + ledger
               + "\n  exchanges " + exchanges
               + "\n  afterExecution " + afterExecutions + ", payload handoffs " + payloadHandoffs
               + ", telemetry " + telemetry.stream().map(c -> c.outcome() + "/" + c.attemptCount()).toList()
               + "\n  leaks " + leaks
               + "\n  violations " + violations
               + "\n  unused " + unusedTransmissions.stream().map(TransportFault::label).toList();
    }
}
