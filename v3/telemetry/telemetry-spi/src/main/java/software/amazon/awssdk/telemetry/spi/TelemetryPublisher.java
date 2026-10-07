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

import software.amazon.awssdk.metrics.MetricPublisher;
import software.amazon.awssdk.telemetry.spi.internal.MetricPublisherBridge;

/**
 * The Telemetry SPI (Requirements 21.2-21.5): receives the terminal record of every attempt and every call of the
 * clients it is configured on ({@link TelemetryConfiguration}).
 *
 * <p>For each call, the pipeline invokes {@link #attemptCompleted} exactly once per attempt, in attempt order, then
 * {@link #callCompleted} exactly once, on the call's driver thread (the caller's thread for a sync call, the
 * completion executor for an async call) before the caller observes the result. Calls run concurrently, so
 * implementations must be thread safe; they should be fast and non-blocking, handing exports to their own threads.
 *
 * <p>A callback that throws never changes the call's result (Requirement 21.14): the failure is delivered to the
 * configuration's {@link TelemetryFailureListener} (Requirement 21.15), and the next callback still runs. Without a
 * configured publisher the pipeline makes no callback and keeps no timer or counter (Requirement 21.5). The SDK never
 * closes a publisher it did not create.
 */
public interface TelemetryPublisher {
    /**
     * The terminal record of one attempt. The default does nothing.
     *
     * @param attempt the attempt's record
     */
    default void attemptCompleted(AttemptRecord attempt) {
    }

    /**
     * The terminal record of one call.
     *
     * @param call the call's record, including its attempts' records
     */
    void callCompleted(CallRecord call);

    /**
     * A publisher that delivers each call to {@code publisher} as one V2-shaped {@code ApiCall}
     * {@link software.amazon.awssdk.metrics.MetricCollection} with one {@code ApiCallAttempt} child per attempt:
     * {@link software.amazon.awssdk.core.metrics.CoreMetric} values (service ID, operation name, success, call
     * duration, retry count, error type; per attempt the attempt duration and error type) and the
     * {@link TelemetryMetric} values (HTTP client name, HTTP status code, retry mode).
     *
     * @param publisher the V2-compatible publisher
     * @return the bridging publisher
     */
    static TelemetryPublisher fromMetricPublisher(MetricPublisher publisher) {
        return new MetricPublisherBridge(publisher);
    }
}
