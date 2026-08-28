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

import static software.amazon.awssdk.http.Header.CONTENT_LENGTH;

import java.io.InputStream;
import java.nio.ByteBuffer;
import java.time.Duration;
import java.util.Optional;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.core.client.config.SdkClientOption;
import software.amazon.awssdk.core.interceptor.SdkInternalExecutionAttribute;
import software.amazon.awssdk.core.internal.InternalCoreExecutionAttribute;
import software.amazon.awssdk.core.internal.http.HttpClientDependencies;
import software.amazon.awssdk.core.internal.http.InterruptMonitor;
import software.amazon.awssdk.core.internal.http.RequestExecutionContext;
import software.amazon.awssdk.core.internal.http.pipeline.RequestPipeline;
import software.amazon.awssdk.core.internal.metrics.BytesWrittenTrackingInputStream;
import software.amazon.awssdk.core.internal.metrics.RequestBodyMetrics;
import software.amazon.awssdk.core.internal.util.MetricUtils;
import software.amazon.awssdk.core.metrics.CoreMetric;
import software.amazon.awssdk.http.ContentStreamProvider;
import software.amazon.awssdk.http.ExecutableHttpRequest;
import software.amazon.awssdk.http.HttpExecuteRequest;
import software.amazon.awssdk.http.HttpExecuteResponse;
import software.amazon.awssdk.http.SdkHttpClient;
import software.amazon.awssdk.http.SdkHttpFullRequest;
import software.amazon.awssdk.http.SdkHttpFullResponse;
import software.amazon.awssdk.metrics.MetricCollector;
import software.amazon.awssdk.utils.Logger;
import software.amazon.awssdk.utils.Pair;
import software.amazon.awssdk.utils.io.LengthAwareInputStream;

/**
 * Delegate to the HTTP implementation to make an HTTP request and receive the response.
 */
@SdkInternalApi
public class MakeHttpRequestStage
    implements RequestPipeline<SdkHttpFullRequest, Pair<SdkHttpFullRequest, SdkHttpFullResponse>> {
    private static final Logger LOG = Logger.loggerFor(MakeHttpRequestStage.class);

    private final SdkHttpClient sdkHttpClient;

    public MakeHttpRequestStage(HttpClientDependencies dependencies) {
        this.sdkHttpClient = dependencies.clientConfiguration().option(SdkClientOption.SYNC_HTTP_CLIENT);
    }

    /**
     * Returns the response from executing one httpClientSettings request; or null for retry.
     */
    @Override
    public Pair<SdkHttpFullRequest, SdkHttpFullResponse> execute(SdkHttpFullRequest request,
                                                                 RequestExecutionContext context) throws Exception {
        InterruptMonitor.checkInterrupted();
        HttpExecuteResponse executeResponse = executeHttpRequest(request, context);
        // TODO: Plumb through ExecuteResponse instead
        SdkHttpFullResponse httpResponse = (SdkHttpFullResponse) executeResponse.httpResponse();
        return Pair.of(request, httpResponse.toBuilder().content(executeResponse.responseBody().orElse(null)).build());
    }

    private HttpExecuteResponse executeHttpRequest(SdkHttpFullRequest request, RequestExecutionContext context) throws Exception {
        MetricCollector attemptMetricCollector = context.attemptMetricCollector();

        MetricCollector httpMetricCollector = MetricUtils.createHttpMetricsCollector(context);

        request = wrapRequestContentStream(request, context);

        ExecutableHttpRequest requestCallable = sdkHttpClient
            .prepareRequest(HttpExecuteRequest.builder()
                                              .request(request)
                                              .metricCollector(httpMetricCollector)
                                              .contentStreamProvider(request.contentStreamProvider().orElse(null))
                                              .build());

        context.apiCallTimeoutTracker().abortable(requestCallable);
        context.apiCallAttemptTimeoutTracker().abortable(requestCallable);

        long start = updateMetricCollectionAttributes(context);
        Pair<HttpExecuteResponse, Duration> measuredExecute = MetricUtils.measureDurationUnsafe(requestCallable, start);
        Duration executeDuration = measuredExecute.right();
        attemptMetricCollector.reportMetric(CoreMetric.SERVICE_CALL_DURATION, executeDuration);

        attemptMetricCollector.reportMetric(CoreMetric.TIME_TO_FIRST_BYTE, executeDuration);

        context.executionAttributes().putAttribute(SdkInternalExecutionAttribute.HEADERS_READ_END_NANO_TIME,
                                                   start + executeDuration.toNanos());

        return measuredExecute.left();
    }

    private SdkHttpFullRequest wrapRequestContentStream(SdkHttpFullRequest request, RequestExecutionContext context) {
        Optional<ContentStreamProvider> contentStreamProvider = request.contentStreamProvider();
        if (!contentStreamProvider.isPresent()) {
            return request;
        }

        RequestBodyMetrics metrics = context.executionAttributes()
                                            .getAttribute(InternalCoreExecutionAttribute.REQUEST_BODY_METRICS);

        ContentStreamProvider wrapped = new TrackingContentStreamProvider(contentStreamProvider.get(), metrics, request);
        return request.toBuilder().contentStreamProvider(wrapped).build();
    }

    /**
     * Wraps the request's {@link ContentStreamProvider} with write-metrics tracking, while still propagating
     * {@link ContentStreamProvider#contentAsByteBufferOrNull()} so that a buffer-backed body keeps its fast path in
     * the HTTP client (e.g. Apache's single-write {@code ByteArrayEntity}).
     *
     * <p>When the body is consumed as a buffer, the metrics are recorded at hand-out: the HTTP client writes the
     * whole buffer with one bulk write immediately after, so the timestamps are equivalent, and {@code bytesWritten}
     * reports the full body. The one divergence from the streaming path is a transport failure mid-write, where the
     * streaming path would report partial bytes and this reports all of them; for a single {@code write()} call the
     * partial count was never meaningful to begin with.
     */
    private static final class TrackingContentStreamProvider implements ContentStreamProvider {
        private final ContentStreamProvider delegate;
        private final RequestBodyMetrics metrics;
        private final SdkHttpFullRequest request;

        private TrackingContentStreamProvider(ContentStreamProvider delegate,
                                              RequestBodyMetrics metrics,
                                              SdkHttpFullRequest request) {
            this.delegate = delegate;
            this.metrics = metrics;
            this.request = request;
        }

        @Override
        public InputStream newStream() {
            InputStream stream = new BytesWrittenTrackingInputStream(delegate.newStream(), metrics);

            Optional<Long> contentLength = contentLength(request);
            if (!contentLength.isPresent()) {
                LOG.debug(() -> String.format("Request contains a body but does not have a Content-Length header. Not "
                                              + "validating the amount of data sent to the service: %s", request));
                return stream;
            }

            return new LengthAwareInputStream(stream, contentLength.get());
        }

        @Override
        public ByteBuffer contentAsByteBufferOrNull() {
            ByteBuffer buffered = delegate.contentAsByteBufferOrNull();
            if (buffered == null) {
                return null;
            }

            long now = System.nanoTime();
            metrics.firstByteWrittenNanoTime().compareAndSet(0, now);
            metrics.lastByteWrittenNanoTime().set(now);
            metrics.bytesWritten().addAndGet(buffered.remaining());
            return buffered;
        }

        @Override
        public String name() {
            return delegate.name();
        }
    }

    private static long updateMetricCollectionAttributes(RequestExecutionContext context) {
        long now = System.nanoTime();
        context.executionAttributes().putAttribute(SdkInternalExecutionAttribute.API_CALL_ATTEMPT_START_NANO_TIME,
                                                   now);
        return now;
    }

    private static Optional<Long> contentLength(SdkHttpFullRequest request) {
        Optional<String> contentLengthHeader = request.firstMatchingHeader(CONTENT_LENGTH);

        if (contentLengthHeader.isPresent()) {
            try {
                return Optional.of(Long.parseLong(contentLengthHeader.get()));
            } catch (NumberFormatException e) {
                LOG.warn(() -> "Unable to parse 'Content-Length' header. Treating it as non existent.");
            }
        }
        return Optional.empty();
    }
}
