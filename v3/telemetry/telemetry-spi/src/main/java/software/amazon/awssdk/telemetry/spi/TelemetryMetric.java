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

import software.amazon.awssdk.metrics.MetricCategory;
import software.amazon.awssdk.metrics.MetricLevel;
import software.amazon.awssdk.metrics.SdkMetric;

/**
 * The metrics a {@link TelemetryPublisher#fromMetricPublisher bridged} {@code MetricPublisher} receives besides
 * {@link software.amazon.awssdk.core.metrics.CoreMetric}. {@link #HTTP_CLIENT_NAME} and {@link #HTTP_STATUS_CODE}
 * have the names, types, levels and category of V2's {@code HttpMetric} constants, so publishers that match metrics
 * by name (such as the CloudWatch publisher's detailed metrics) see the V2 names.
 */
public final class TelemetryMetric {
    /** The client name of the attempt's HTTP implementation. */
    public static final SdkMetric<String> HTTP_CLIENT_NAME =
        SdkMetric.create("HttpClientName", String.class, MetricLevel.INFO, MetricCategory.HTTP_CLIENT);

    /** The HTTP status code of the attempt's response, when one was received. */
    public static final SdkMetric<Integer> HTTP_STATUS_CODE =
        SdkMetric.create("HttpStatusCode", Integer.class, MetricLevel.TRACE, MetricCategory.HTTP_CLIENT);

    /** The retry mode of the client ({@code standard}, {@code adaptive}, or a custom strategy's class name). */
    public static final SdkMetric<String> RETRY_MODE =
        SdkMetric.create("RetryMode", String.class, MetricLevel.INFO, MetricCategory.CORE);

    private TelemetryMetric() {
    }
}
