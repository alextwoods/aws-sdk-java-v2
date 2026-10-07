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

import java.util.Objects;

/**
 * One failed telemetry callback, as delivered to the telemetry failure channel ({@link TelemetryFailureListener};
 * Requirement 21.15). The call it reports on completed with its own result regardless (Requirement 21.14).
 *
 * @param publisher the failing publisher
 * @param callback the callback that failed
 * @param service the service ID of the call
 * @param operation the operation name of the call
 * @param cause what the callback threw
 */
public record TelemetryFailure(TelemetryPublisher publisher, Callback callback, String service, String operation,
                               Throwable cause) {
    public TelemetryFailure {
        Objects.requireNonNull(publisher, "publisher");
        Objects.requireNonNull(callback, "callback");
        Objects.requireNonNull(service, "service");
        Objects.requireNonNull(operation, "operation");
        Objects.requireNonNull(cause, "cause");
    }

    /** A {@link TelemetryPublisher} callback. */
    public enum Callback {
        /** {@link TelemetryPublisher#attemptCompleted}. */
        ATTEMPT_COMPLETED,
        /** {@link TelemetryPublisher#callCompleted}. */
        CALL_COMPLETED
    }

    @Override
    public String toString() {
        return "TelemetryFailure(publisher=" + publisher.getClass().getName() + ", callback=" + callback
               + ", operation=" + service + "." + operation + ", cause=" + cause.getClass().getName() + ")";
    }
}
