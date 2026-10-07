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

package software.amazon.awssdk.telemetry.spi.internal;

import java.util.Objects;
import software.amazon.awssdk.core.metrics.CoreMetric;
import software.amazon.awssdk.metrics.MetricCollector;
import software.amazon.awssdk.metrics.MetricPublisher;
import software.amazon.awssdk.telemetry.spi.AttemptRecord;
import software.amazon.awssdk.telemetry.spi.CallRecord;
import software.amazon.awssdk.telemetry.spi.TelemetryMetric;
import software.amazon.awssdk.telemetry.spi.TelemetryOutcome;
import software.amazon.awssdk.telemetry.spi.TelemetryPublisher;

/**
 * {@link TelemetryPublisher#fromMetricPublisher}: each call record becomes one V2-shaped {@code ApiCall}
 * collection with one {@code ApiCallAttempt} child per attempt, published once the call completes, as V2 does. Not
 * a public API.
 */
public final class MetricPublisherBridge implements TelemetryPublisher {
    /** The V2 name of a call's collection. */
    public static final String API_CALL = "ApiCall";
    /** The V2 name of an attempt's collection. */
    public static final String API_CALL_ATTEMPT = "ApiCallAttempt";

    private final MetricPublisher publisher;

    public MetricPublisherBridge(MetricPublisher publisher) {
        this.publisher = Objects.requireNonNull(publisher, "publisher");
    }

    /** The V2-compatible publisher this bridge delivers to. */
    public MetricPublisher publisher() {
        return publisher;
    }

    @Override
    public void callCompleted(CallRecord call) {
        MetricCollector collector = MetricCollector.create(API_CALL);
        collector.reportMetric(CoreMetric.SERVICE_ID, call.service());
        collector.reportMetric(CoreMetric.OPERATION_NAME, call.operation());
        collector.reportMetric(CoreMetric.API_CALL_SUCCESSFUL, call.outcome() == TelemetryOutcome.SUCCEEDED);
        collector.reportMetric(CoreMetric.API_CALL_DURATION, call.duration());
        collector.reportMetric(CoreMetric.RETRY_COUNT, Math.max(0, call.attemptCount() - 1));
        collector.reportMetric(TelemetryMetric.RETRY_MODE, call.retryMode());
        call.errorType().ifPresent(type -> collector.reportMetric(CoreMetric.ERROR_TYPE, type));
        for (AttemptRecord attempt : call.attempts()) {
            MetricCollector child = collector.createChild(API_CALL_ATTEMPT);
            child.reportMetric(CoreMetric.SERVICE_CALL_DURATION, attempt.duration());
            child.reportMetric(TelemetryMetric.HTTP_CLIENT_NAME, attempt.httpImplementation());
            attempt.httpStatusCode().ifPresent(status -> child.reportMetric(TelemetryMetric.HTTP_STATUS_CODE, status));
            attempt.errorType().ifPresent(type -> child.reportMetric(CoreMetric.ERROR_TYPE, type));
        }
        publisher.publish(collector.collect());
    }

    @Override
    public String toString() {
        return "MetricPublisherBridge(" + publisher.getClass().getName() + ")";
    }
}
