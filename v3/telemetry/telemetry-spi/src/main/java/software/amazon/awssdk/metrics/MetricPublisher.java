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

package software.amazon.awssdk.metrics;

/**
 * Receives the collected SDK metrics of each call (V2-compatible; Parity Manifest {@code api:} entries of
 * {@code software.amazon.awssdk.metrics}).
 *
 * <p>Configured through {@code TelemetryConfiguration.Builder#addMetricPublisher} (or a generated client builder's
 * {@code addMetricPublisher}), a publisher receives one {@link MetricCollection} named {@code ApiCall} per completed
 * call, with one {@code ApiCallAttempt} child per attempt. The SDK may invoke it from multiple threads concurrently,
 * so implementations must be thread safe, and should hand any slow work (aggregation, uploads) to their own threads.
 *
 * <p>A publisher that throws does not change the call's result: the failure goes to the client's telemetry failure
 * channel ({@code TelemetryFailureListener}). The SDK never closes a publisher it did not create.
 */
public interface MetricPublisher extends AutoCloseable {
    /**
     * Notifies the publisher of the metrics of one call. After this call returns, the caller may discard
     * {@code metricCollection}.
     *
     * @param metricCollection the collection of metrics
     * @throws IllegalArgumentException if {@code metricCollection} is {@code null}
     */
    void publish(MetricCollection metricCollection);

    /**
     * Publishes every pending metric and releases the publisher's resources, blocking until both are done.
     */
    @Override
    void close();
}
