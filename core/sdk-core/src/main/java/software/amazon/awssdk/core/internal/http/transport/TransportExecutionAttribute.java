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

package software.amazon.awssdk.core.internal.http.transport;

import java.util.function.Consumer;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.core.async.AsyncRequestBody;
import software.amazon.awssdk.core.interceptor.ExecutionAttribute;
import software.amazon.awssdk.http.Abortable;
import software.amazon.awssdk.metrics.MetricCollector;

/**
 * Per-call inputs that the request pipeline gives the SDK HTTP client transports through {@code ExecutionAttributes}.
 *
 * <p>The transport API only carries the request, so the parts of the legacy {@code MakeHttpRequestStage} and
 * {@code MakeAsyncHttpRequestStage} that need pipeline state travel here. Every attribute is optional: when one is
 * absent, the transport skips the matching step.
 *
 * <p>The transports also read these existing attributes when present: {@code SdkInternalExecutionAttribute
 * .SDK_HTTP_EXECUTION_ATTRIBUTES}, {@code IS_FULL_DUPLEX} and {@code RESPONSE_BYTES_READ}, and
 * {@code InternalCoreExecutionAttribute.REQUEST_BODY_METRICS}. They write
 * {@code SdkInternalExecutionAttribute.API_CALL_ATTEMPT_START_NANO_TIME} before sending and
 * {@code HEADERS_READ_END_NANO_TIME} when the response headers arrive, as the legacy stages do.
 */
@SdkInternalApi
public final class TransportExecutionAttribute {
    /**
     * The metric collector given to the HTTP client for the attempt's HTTP metrics (the legacy stages pass
     * {@code MetricUtils.createHttpMetricsCollector(context)}).
     */
    public static final ExecutionAttribute<MetricCollector> HTTP_METRIC_COLLECTOR =
        new ExecutionAttribute<>("TransportHttpMetricCollector");

    /**
     * Receives the prepared synchronous request before it executes, so that the pipeline's call and attempt timeout
     * trackers can abort it (the legacy {@code MakeHttpRequestStage} registers it with both trackers). The asynchronous
     * transport gives it an {@code Abortable} that fails the HTTP client's future, which the asynchronous pipeline sets
     * for streaming outputs to abort a handed-off body when the attempt or the call ends.
     */
    public static final ExecutionAttribute<Consumer<Abortable>> ABORTABLE_LISTENER =
        new ExecutionAttribute<>("TransportAbortableListener");

    /**
     * The streaming request body of an asynchronous call. When set, the asynchronous transport sends it instead of the
     * request's content stream provider (the legacy {@code RequestExecutionContext.requestProvider()}).
     */
    public static final ExecutionAttribute<AsyncRequestBody> ASYNC_REQUEST_BODY =
        new ExecutionAttribute<>("TransportAsyncRequestBody");

    private TransportExecutionAttribute() {
    }
}
