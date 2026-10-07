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

package software.amazon.awssdk.telemetry.spi;

import java.time.Duration;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * The terminal record of one call (Requirements 21.2, 21.4): emitted exactly once per call, after its last attempt's
 * {@link AttemptRecord}, when its outcome is decided and before the caller observes it.
 *
 * <p>It carries only metadata (see {@link AttemptRecord}).
 *
 * @param service the service ID of the client
 * @param operation the operation name
 * @param outcome how the call ended
 * @param attemptCount the number of attempts the call started; zero when it ended before its first attempt
 * @param httpImplementation the client name of the HTTP implementation, or {@code none}
 * @param retryMode the retry mode of the client
 * @param duration the call's duration, from its start to its terminal outcome, including retry delays
 * @param errorType the failure category of a failed call; empty otherwise
 * @param attempts the records of the call's attempts, in attempt order; as many as {@code attemptCount}
 */
public record CallRecord(String service, String operation, TelemetryOutcome outcome, int attemptCount,
                         String httpImplementation, String retryMode, Duration duration, Optional<String> errorType,
                         List<AttemptRecord> attempts) {
    public CallRecord {
        Objects.requireNonNull(service, "service");
        Objects.requireNonNull(operation, "operation");
        Objects.requireNonNull(outcome, "outcome");
        Objects.requireNonNull(httpImplementation, "httpImplementation");
        Objects.requireNonNull(retryMode, "retryMode");
        Objects.requireNonNull(duration, "duration");
        Objects.requireNonNull(errorType, "errorType");
        attempts = List.copyOf(attempts);
        if (attemptCount != attempts.size()) {
            throw new IllegalArgumentException("attemptCount " + attemptCount + " differs from the " + attempts.size()
                                               + " attempt records");
        }
    }
}
