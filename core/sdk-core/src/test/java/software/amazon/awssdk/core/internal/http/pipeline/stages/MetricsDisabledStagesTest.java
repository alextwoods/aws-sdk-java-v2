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

package software.amazon.awssdk.core.internal.http.pipeline.stages;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static software.amazon.awssdk.core.client.config.SdkClientOption.SYNC_HTTP_CLIENT;

import java.io.ByteArrayInputStream;
import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import software.amazon.awssdk.core.Response;
import software.amazon.awssdk.core.client.config.SdkClientConfiguration;
import software.amazon.awssdk.core.http.ExecutionContext;
import software.amazon.awssdk.core.http.HttpResponseHandler;
import software.amazon.awssdk.core.http.MetricCollectingHttpResponseHandler;
import software.amazon.awssdk.core.interceptor.ExecutionAttributes;
import software.amazon.awssdk.core.interceptor.SdkExecutionAttribute;
import software.amazon.awssdk.core.interceptor.SdkInternalExecutionAttribute;
import software.amazon.awssdk.core.internal.InternalCoreExecutionAttribute;
import software.amazon.awssdk.core.internal.http.HttpClientDependencies;
import software.amazon.awssdk.core.internal.http.RequestExecutionContext;
import software.amazon.awssdk.core.internal.http.pipeline.RequestPipeline;
import software.amazon.awssdk.core.internal.http.timers.TimeoutTracker;
import software.amazon.awssdk.core.internal.util.MetricUtils;
import software.amazon.awssdk.core.metrics.CoreMetric;
import software.amazon.awssdk.http.AbortableInputStream;
import software.amazon.awssdk.http.ExecutableHttpRequest;
import software.amazon.awssdk.http.HttpExecuteRequest;
import software.amazon.awssdk.http.HttpExecuteResponse;
import software.amazon.awssdk.http.SdkHttpClient;
import software.amazon.awssdk.http.SdkHttpFullRequest;
import software.amazon.awssdk.http.SdkHttpFullResponse;
import software.amazon.awssdk.http.SdkHttpMethod;
import software.amazon.awssdk.metrics.MetricCollector;
import software.amazon.awssdk.metrics.NoOpMetricCollector;
import utils.ValidSdkObjects;

/**
 * With a no-op collector nothing reported can be observed, so the pipeline must not do the measurement work either: no
 * request or response rebuilt around byte counters, no counters created, no timing decorators. With a real collector all
 * of it still happens (the end-to-end coverage for that is CoreMetricsTest in codegen-generated-classes-test; these pin
 * the switch itself).
 */
class MetricsDisabledStagesTest {

    @Test
    void collectsMetrics_noOpOrNull_false_realCollector_true() {
        assertThat(MetricUtils.collectsMetrics(null)).isFalse();
        assertThat(MetricUtils.collectsMetrics(NoOpMetricCollector.create())).isFalse();
        assertThat(MetricUtils.collectsMetrics(MetricCollector.create("test"))).isTrue();
    }

    @Test
    void measureAndReport_noOpCollector_runsSupplierOnce_reportsNothing() {
        AtomicInteger calls = new AtomicInteger();
        MetricCollector spy = mock(MetricCollector.class);

        String viaNoOp = MetricUtils.measureAndReport(() -> "r" + calls.incrementAndGet(), NoOpMetricCollector.create(),
                                                      CoreMetric.SIGNING_DURATION);
        String viaReal = MetricUtils.measureAndReport(() -> "r" + calls.incrementAndGet(), spy, CoreMetric.SIGNING_DURATION);

        assertThat(viaNoOp).isEqualTo("r1");
        assertThat(viaReal).isEqualTo("r2");
        verify(spy).reportMetric(any(), any(Duration.class));
    }

    @Test
    void reportDuration_noOpCollector_returnsTheSupplierFutureItself() {
        CompletableFuture<String> future = CompletableFuture.completedFuture("x");
        CompletableFuture<String> result = MetricUtils.reportDuration(() -> future, NoOpMetricCollector.create(),
                                                                       CoreMetric.CREDENTIALS_FETCH_DURATION);
        // No whenComplete stage was chained: the caller gets the supplier's own future.
        assertThat(result).isSameAs(future);
    }

    @Test
    void attemptStage_noOpCollector_createsNoByteCounters() throws Exception {
        RequestExecutionContext context = context(NoOpMetricCollector.create());
        ApiCallAttemptMetricCollectionStage<String> stage = new ApiCallAttemptMetricCollectionStage<>(
            (request, ctx) -> Response.<String>builder().response("ok").isSuccess(true).build());

        stage.execute(request(), context);

        assertThat(context.attemptMetricCollector()).isSameAs(NoOpMetricCollector.create());
        assertThat(context.executionAttributes().getAttribute(SdkInternalExecutionAttribute.RESPONSE_BYTES_READ)).isNull();
        assertThat(context.executionAttributes().getAttribute(InternalCoreExecutionAttribute.REQUEST_BODY_METRICS)).isNull();
    }

    @Test
    void attemptStage_realCollector_createsByteCounters() throws Exception {
        RequestExecutionContext context = context(MetricCollector.create("ApiCall"));
        ApiCallAttemptMetricCollectionStage<String> stage = new ApiCallAttemptMetricCollectionStage<>(
            (request, ctx) -> Response.<String>builder().response("ok").isSuccess(true).build());

        stage.execute(request(), context);

        assertThat(context.executionAttributes().getAttribute(SdkInternalExecutionAttribute.RESPONSE_BYTES_READ)).isNotNull();
        assertThat(context.executionAttributes().getAttribute(InternalCoreExecutionAttribute.REQUEST_BODY_METRICS)).isNotNull();
    }

    @Test
    void makeHttpRequestStage_noOpCollector_passesRequestThroughWithoutRebuildingIt() throws Exception {
        SdkHttpClient client = mock(SdkHttpClient.class);
        ExecutableHttpRequest executable = mock(ExecutableHttpRequest.class);
        when(client.prepareRequest(any())).thenReturn(executable);
        when(executable.call()).thenReturn(HttpExecuteResponse.builder()
                                                              .response(SdkHttpFullResponse.builder().statusCode(200).build())
                                                              .build());
        MakeHttpRequestStage stage = new MakeHttpRequestStage(dependencies(client));
        SdkHttpFullRequest request = request().toBuilder()
                                              .method(SdkHttpMethod.PUT)
                                              .contentStreamProvider(() -> new ByteArrayInputStream(new byte[4]))
                                              .build();
        RequestExecutionContext context = context(NoOpMetricCollector.create());
        context.attemptMetricCollector(NoOpMetricCollector.create());

        stage.execute(request, context);

        ArgumentCaptor<HttpExecuteRequest> captor = ArgumentCaptor.forClass(HttpExecuteRequest.class);
        verify(client).prepareRequest(captor.capture());
        // The very same request instance reached the HTTP client, and the provider it was handed still enforces the
        // Content-Length (that is a correctness check, not a metric) without a byte-counting stream underneath.
        assertThat(captor.getValue().httpRequest()).isSameAs(request);
        assertThat(captor.getValue().contentStreamProvider()).isPresent();
        assertThat(captor.getValue().contentStreamProvider().get().newStream())
            .isNotInstanceOf(software.amazon.awssdk.core.internal.metrics.BytesWrittenTrackingInputStream.class);
        assertThat(context.executionAttributes().getAttribute(SdkInternalExecutionAttribute.API_CALL_ATTEMPT_START_NANO_TIME))
            .isNull();
        assertThat(context.executionAttributes().getAttribute(SdkInternalExecutionAttribute.HEADERS_READ_END_NANO_TIME))
            .isNull();
    }

    @Test
    void makeHttpRequestStage_realCollector_recordsTimingAttributes() throws Exception {
        SdkHttpClient client = mock(SdkHttpClient.class);
        ExecutableHttpRequest executable = mock(ExecutableHttpRequest.class);
        when(client.prepareRequest(any())).thenReturn(executable);
        when(executable.call()).thenReturn(HttpExecuteResponse.builder()
                                                              .response(SdkHttpFullResponse.builder().statusCode(200).build())
                                                              .build());
        MakeHttpRequestStage stage = new MakeHttpRequestStage(dependencies(client));
        MetricCollector collector = MetricCollector.create("ApiCall");
        RequestExecutionContext context = context(collector);
        context.attemptMetricCollector(collector.createChild("ApiCallAttempt"));
        context.executionAttributes().putAttribute(InternalCoreExecutionAttribute.REQUEST_BODY_METRICS,
                                                   new software.amazon.awssdk.core.internal.metrics.RequestBodyMetrics());

        stage.execute(request(), context);

        assertThat(context.executionAttributes().getAttribute(SdkInternalExecutionAttribute.API_CALL_ATTEMPT_START_NANO_TIME))
            .isNotNull();
        assertThat(context.executionAttributes().getAttribute(SdkInternalExecutionAttribute.HEADERS_READ_END_NANO_TIME))
            .isNotNull();
        assertThat(context.attemptMetricCollector().collect().metricValues(CoreMetric.SERVICE_CALL_DURATION)).hasSize(1);
    }

    @Test
    void handleResponseStage_noOpCollector_handsTheResponseThroughUntouched() throws Exception {
        SdkHttpFullResponse httpResponse = SdkHttpFullResponse.builder()
                                                              .statusCode(200)
                                                              .content(AbortableInputStream.create(new ByteArrayInputStream(new byte[8])))
                                                              .build();
        HttpResponseHandler<Response<String>> handler = (response, attrs) -> {
            // Same instance, and the content stream is the original, not a counting wrapper.
            assertThat(response).isSameAs(httpResponse);
            return Response.<String>builder().response("ok").isSuccess(true).build();
        };
        RequestExecutionContext context = context(NoOpMetricCollector.create());
        context.attemptMetricCollector(NoOpMetricCollector.create());

        Response<String> result = new HandleResponseStage<>(handler).execute(httpResponse, context);

        assertThat(result.response()).isEqualTo("ok");
    }

    @Test
    void metricCollectingResponseHandler_noOpCollector_delegatesDirectly() throws Exception {
        HttpResponseHandler<String> delegate = mock(HttpResponseHandler.class);
        when(delegate.handle(any(), any())).thenReturn("ok");
        ExecutionAttributes attrs = new ExecutionAttributes();
        attrs.putAttribute(SdkExecutionAttribute.API_CALL_ATTEMPT_METRIC_COLLECTOR, NoOpMetricCollector.create());
        SdkHttpFullResponse response = SdkHttpFullResponse.builder().statusCode(200).build();

        String result = MetricCollectingHttpResponseHandler.create(CoreMetric.UNMARSHALLING_DURATION, delegate)
                                                           .handle(response, attrs);

        assertThat(result).isEqualTo("ok");
        verify(delegate).handle(response, attrs);
    }

    @Test
    void metricCollectingResponseHandler_realCollector_reportsUnmarshallingDuration() throws Exception {
        HttpResponseHandler<String> delegate = (response, attrs) -> "ok";
        MetricCollector collector = MetricCollector.create("ApiCallAttempt");
        ExecutionAttributes attrs = new ExecutionAttributes();
        attrs.putAttribute(SdkExecutionAttribute.API_CALL_ATTEMPT_METRIC_COLLECTOR, collector);

        MetricCollectingHttpResponseHandler.create(CoreMetric.UNMARSHALLING_DURATION, delegate)
                                           .handle(SdkHttpFullResponse.builder().statusCode(200).build(), attrs);

        assertThat(collector.collect().metricValues(CoreMetric.UNMARSHALLING_DURATION)).hasSize(1);
    }

    private static HttpClientDependencies dependencies(SdkHttpClient client) {
        SdkClientConfiguration config = SdkClientConfiguration.builder().option(SYNC_HTTP_CLIENT, client).build();
        return HttpClientDependencies.builder().clientConfiguration(config).build();
    }

    private static RequestExecutionContext context(MetricCollector apiCallCollector) {
        ExecutionContext executionContext = ExecutionContext.builder()
                                                            .executionAttributes(new ExecutionAttributes())
                                                            .metricCollector(apiCallCollector)
                                                            .build();
        RequestExecutionContext context = RequestExecutionContext.builder()
                                                                 .originalRequest(ValidSdkObjects.sdkRequest())
                                                                 .executionContext(executionContext)
                                                                 .build();
        context.apiCallAttemptTimeoutTracker(mock(TimeoutTracker.class));
        context.apiCallTimeoutTracker(mock(TimeoutTracker.class));
        return context;
    }

    private static SdkHttpFullRequest request() {
        return SdkHttpFullRequest.builder()
                                 .method(SdkHttpMethod.GET)
                                 .host("localhost")
                                 .protocol("https")
                                 .build();
    }
}
