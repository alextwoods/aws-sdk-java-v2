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

import java.util.List;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;
import software.amazon.awssdk.telemetry.spi.AttemptRecord;
import software.amazon.awssdk.telemetry.spi.CallRecord;
import software.amazon.awssdk.telemetry.spi.TelemetryConfiguration;
import software.amazon.awssdk.telemetry.spi.TelemetryPublisher;

/**
 * A telemetry publisher that keeps every record of one call, recording a second terminal call record as a violation
 * (exactly-once completion of the call's telemetry). Use one probe per call. Thread-safe.
 */
public final class TelemetryProbe implements TelemetryPublisher {
    private final Violations violations;
    private final List<CallRecord> calls = new CopyOnWriteArrayList<>();
    private final List<AttemptRecord> attempts = new CopyOnWriteArrayList<>();

    public TelemetryProbe(Violations violations) {
        this.violations = Objects.requireNonNull(violations, "violations");
    }

    /** A telemetry configuration publishing to this probe only. */
    public TelemetryConfiguration configuration() {
        return TelemetryConfiguration.builder().addPublisher(this).build();
    }

    @Override
    public void attemptCompleted(AttemptRecord attempt) {
        attempts.add(attempt);
    }

    @Override
    public void callCompleted(CallRecord call) {
        calls.add(call);
        if (calls.size() > 1) {
            violations.record("telemetry: " + calls.size() + " terminal call records");
        }
    }

    /** The call records so far; exactly one once a call has ended. */
    public List<CallRecord> calls() {
        return List.copyOf(calls);
    }

    /** The attempt records so far. */
    public List<AttemptRecord> attempts() {
        return List.copyOf(attempts);
    }
}
