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

import software.amazon.awssdk.telemetry.spi.internal.LoggingTelemetryFailureListener;

/**
 * The telemetry failure channel (Requirement 21.15): receives every {@link TelemetryFailure}, once per failed
 * callback, on the thread that ran the callback, after which the call completes with its own result.
 *
 * <p>The channel of a {@link TelemetryConfiguration} without an explicit listener is {@link #logging()}. A listener
 * must be thread safe; one that throws is ignored, so it cannot change a call's result either.
 */
@FunctionalInterface
public interface TelemetryFailureListener {
    /**
     * Receives one telemetry callback failure.
     *
     * @param failure the failure
     */
    void telemetryFailed(TelemetryFailure failure);

    /**
     * The default channel: logs each failure through the SDK logging facade
     * ({@link software.amazon.awssdk.utils.Logger}) named {@code software.amazon.awssdk.telemetry}, the
     * first failure of each publisher and callback at {@code WARN} with its cause, every later one at {@code DEBUG},
     * so a persistently failing publisher logs one warning per listener (its
     * {@link software.amazon.awssdk.utils.WarningSuppression} scope; Requirement 21.12). No application logging
     * implementation is required.
     *
     * @return a new listener with its own suppression scope
     */
    static TelemetryFailureListener logging() {
        return new LoggingTelemetryFailureListener();
    }
}
