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

package software.amazon.awssdk.core.metrics;

import java.net.URI;
import java.time.Duration;
import software.amazon.awssdk.metrics.MetricCategory;
import software.amazon.awssdk.metrics.MetricLevel;
import software.amazon.awssdk.metrics.SdkMetric;

/**
 * The core SDK metrics, with V2's names, value types, levels and the {@link MetricCategory#CORE} category. Shipped by
 * {@code telemetry-spi} (V2 shipped it in {@code sdk-core}, which V3's {@code sdk-core} cannot do because the
 * metrics API depends on it).
 *
 * <p>The V3 pipeline reports, through a configured {@link software.amazon.awssdk.metrics.MetricPublisher}: per call
 * {@link #SERVICE_ID}, {@link #OPERATION_NAME}, {@link #API_CALL_SUCCESSFUL}, {@link #API_CALL_DURATION},
 * {@link #RETRY_COUNT} and, for a failed call, {@link #ERROR_TYPE}; per attempt {@link #SERVICE_CALL_DURATION}
 * (the whole attempt, from endpoint resolution to deserialization) and, for a failed attempt, {@link #ERROR_TYPE}.
 * The finer-grained V2 timings (marshalling, signing, credentials and token fetch, endpoint resolution, backoff,
 * time to first/last byte, throughput) and the request IDs and endpoint are declared for source compatibility but not
 * reported: they would need a clock read per stage.
 */
public final class CoreMetric {
    /** The unique ID of the service. */
    public static final SdkMetric<String> SERVICE_ID = metric("ServiceId", String.class, MetricLevel.ERROR);

    /** The name of the service operation being invoked. */
    public static final SdkMetric<String> OPERATION_NAME = metric("OperationName", String.class, MetricLevel.ERROR);

    /** True if the API call succeeded, false otherwise. */
    public static final SdkMetric<Boolean> API_CALL_SUCCESSFUL =
        metric("ApiCallSuccessful", Boolean.class, MetricLevel.ERROR);

    /** The number of retries the SDK performed: the number of attempts minus one. */
    public static final SdkMetric<Integer> RETRY_COUNT = metric("RetryCount", Integer.class, MetricLevel.ERROR);

    /** The endpoint of the service (not reported by V3). */
    public static final SdkMetric<URI> SERVICE_ENDPOINT = metric("ServiceEndpoint", URI.class, MetricLevel.ERROR);

    /** The duration of the API call, including every attempt and retry delay. */
    public static final SdkMetric<Duration> API_CALL_DURATION =
        metric("ApiCallDuration", Duration.class, MetricLevel.INFO);

    /** The duration of fetching signing credentials (not reported by V3). */
    public static final SdkMetric<Duration> CREDENTIALS_FETCH_DURATION =
        metric("CredentialsFetchDuration", Duration.class, MetricLevel.INFO);

    /** The duration of fetching a signing token (not reported by V3). */
    public static final SdkMetric<Duration> TOKEN_FETCH_DURATION =
        metric("TokenFetchDuration", Duration.class, MetricLevel.INFO);

    /** The duration of the retry delay before an attempt (not reported by V3). */
    public static final SdkMetric<Duration> BACKOFF_DELAY_DURATION =
        metric("BackoffDelayDuration", Duration.class, MetricLevel.INFO);

    /** The duration of request marshalling (not reported by V3). */
    public static final SdkMetric<Duration> MARSHALLING_DURATION =
        metric("MarshallingDuration", Duration.class, MetricLevel.INFO);

    /** The duration of request signing (not reported by V3). */
    public static final SdkMetric<Duration> SIGNING_DURATION =
        metric("SigningDuration", Duration.class, MetricLevel.INFO);

    /** The duration of one attempt. */
    public static final SdkMetric<Duration> SERVICE_CALL_DURATION =
        metric("ServiceCallDuration", Duration.class, MetricLevel.INFO);

    /** The duration of response unmarshalling (not reported by V3). */
    public static final SdkMetric<Duration> UNMARSHALLING_DURATION =
        metric("UnmarshallingDuration", Duration.class, MetricLevel.INFO);

    /** The request ID of the service response (not reported by V3; see the response metadata). */
    public static final SdkMetric<String> AWS_REQUEST_ID = metric("AwsRequestId", String.class, MetricLevel.INFO);

    /** The extended request ID of the service response (not reported by V3; see the response metadata). */
    public static final SdkMetric<String> AWS_EXTENDED_REQUEST_ID =
        metric("AwsExtendedRequestId", String.class, MetricLevel.INFO);

    /** The time to the first response byte (not reported by V3). */
    public static final SdkMetric<Duration> TIME_TO_FIRST_BYTE =
        metric("TimeToFirstByte", Duration.class, MetricLevel.TRACE);

    /** The time to the last response byte (not reported by V3). */
    public static final SdkMetric<Duration> TIME_TO_LAST_BYTE =
        metric("TimeToLastByte", Duration.class, MetricLevel.TRACE);

    /** The read throughput in bytes per second (not reported by V3). */
    public static final SdkMetric<Double> READ_THROUGHPUT = metric("ReadThroughput", Double.class, MetricLevel.TRACE);

    /** The write throughput in bytes per second (not reported by V3). */
    public static final SdkMetric<Double> WRITE_THROUGHPUT =
        metric("WriteThroughput", Double.class, MetricLevel.TRACE);

    /** The duration of endpoint resolution (not reported by V3). */
    public static final SdkMetric<Duration> ENDPOINT_RESOLVE_DURATION =
        metric("EndpointResolveDuration", Duration.class, MetricLevel.INFO);

    /**
     * The type of a failure: {@code Throttling}, {@code ServerError}, {@code ConfiguredTimeout}, {@code IO} or
     * {@code Other}, as in V2.
     */
    public static final SdkMetric<String> ERROR_TYPE = metric("ErrorType", String.class, MetricLevel.INFO);

    private CoreMetric() {
    }

    private static <T> SdkMetric<T> metric(String name, Class<T> clzz, MetricLevel level) {
        return SdkMetric.create(name, clzz, level, MetricCategory.CORE);
    }
}
