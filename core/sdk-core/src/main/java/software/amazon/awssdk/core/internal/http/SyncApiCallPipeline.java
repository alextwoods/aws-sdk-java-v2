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

package software.amazon.awssdk.core.internal.http;

import static software.amazon.awssdk.core.internal.http.timers.TimerUtils.resolveTimeoutInMillis;
import static software.amazon.awssdk.core.internal.http.timers.TimerUtils.timeSyncTaskIfNeeded;
import static software.amazon.awssdk.utils.FunctionalUtils.invokeSafely;
import static software.amazon.awssdk.utils.FunctionalUtils.runAndLogError;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;
import java.util.function.Function;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.annotations.ThreadSafe;
import software.amazon.awssdk.core.ClientType;
import software.amazon.awssdk.core.Response;
import software.amazon.awssdk.core.SdkRequest;
import software.amazon.awssdk.core.SdkResponse;
import software.amazon.awssdk.core.client.config.SdkClientConfiguration;
import software.amazon.awssdk.core.client.config.SdkClientOption;
import software.amazon.awssdk.core.client.handler.ClientExecutionParams;
import software.amazon.awssdk.core.client.protocol.ClientTransport;
import software.amazon.awssdk.core.client.protocol.HttpMessageExchange;
import software.amazon.awssdk.core.exception.AbortedException;
import software.amazon.awssdk.core.exception.ApiCallAttemptTimeoutException;
import software.amazon.awssdk.core.exception.ApiCallTimeoutException;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.core.exception.SdkException;
import software.amazon.awssdk.core.exception.SdkInterruptedException;
import software.amazon.awssdk.core.http.ExecutionContext;
import software.amazon.awssdk.core.interceptor.ExecutionAttributes;
import software.amazon.awssdk.core.interceptor.InterceptorContext;
import software.amazon.awssdk.core.interceptor.SdkExecutionAttribute;
import software.amazon.awssdk.core.interceptor.SdkInternalExecutionAttribute;
import software.amazon.awssdk.core.internal.InternalCoreExecutionAttribute;
import software.amazon.awssdk.core.internal.http.pipeline.stages.utils.ExceptionReportingUtils;
import software.amazon.awssdk.core.internal.http.pipeline.stages.utils.RetryableStageHelper;
import software.amazon.awssdk.core.internal.http.timers.TimeoutTracker;
import software.amazon.awssdk.core.internal.http.transport.SdkHttpClientTransport;
import software.amazon.awssdk.core.internal.http.transport.TransportExecutionAttribute;
import software.amazon.awssdk.core.internal.metrics.BytesReadTrackingInputStream;
import software.amazon.awssdk.core.internal.metrics.RequestBodyMetrics;
import software.amazon.awssdk.core.internal.metrics.SdkErrorType;
import software.amazon.awssdk.core.internal.util.MetricUtils;
import software.amazon.awssdk.core.internal.util.ThrowableUtils;
import software.amazon.awssdk.core.io.ReleasableInputStream;
import software.amazon.awssdk.core.metrics.CoreMetric;
import software.amazon.awssdk.core.sync.ResponseTransformer;
import software.amazon.awssdk.http.Abortable;
import software.amazon.awssdk.http.AbortableInputStream;
import software.amazon.awssdk.http.ContentStreamProvider;
import software.amazon.awssdk.http.SdkHttpClient;
import software.amazon.awssdk.http.SdkHttpFullRequest;
import software.amazon.awssdk.http.SdkHttpFullResponse;
import software.amazon.awssdk.http.SdkHttpRequest;
import software.amazon.awssdk.metrics.MetricCollector;
import software.amazon.awssdk.metrics.NoOpMetricCollector;
import software.amazon.awssdk.schema.ApiOperation;
import software.amazon.awssdk.schema.serde.TypeRegistry;
import software.amazon.awssdk.utils.Logger;
import software.amazon.awssdk.utils.Validate;

/**
 * The synchronous schema-based request pipeline (design D.6): one call of an {@link ApiOperation} on a client whose
 * requests are serialized by a {@code ClientProtocol} and sent by a {@link ClientTransport}. One instance is created per
 * client configuration and every call reuses it; it holds no per-call state.
 *
 * <p>Each call is a fixed sequence of direct calls to the shared stages (racecar G1, re-implemented), not a chain of
 * nested {@code RequestPipeline} wrappers. The sequence and its effects are those of the legacy sync client handler and
 * the stage graph {@code AmazonSyncHttpClient} composes, outermost first:
 * <ol>
 *     <li>{@code API_CALL_SUCCESSFUL} reporting ({@code BaseSyncClientHandler.measureApiCallSuccess});</li>
 *     <li>the call's execution context, created by the client handler's factory, which runs {@code beforeExecution}
 *     and {@code modifyRequest};</li>
 *     <li>failure reporting ({@code ExecutionFailureExceptionReportingStage}): {@code modifyException} and
 *     {@code onExecutionFailure} once, then the legacy exception type;</li>
 *     <li>serialization and the HTTP request interceptors ({@link RequestMutationStages#marshall}: {@code beforeMarshalling},
 *     {@code afterMarshalling}, {@code modifyHttpRequest}/{@code modifyHttpContent});</li>
 *     <li>{@code API_CALL_DURATION} and {@code SERVICE_ENDPOINT} ({@code ApiCallMetricCollectionStage});</li>
 *     <li>the API call timeout ({@code ApiCallTimeoutTrackingStage});</li>
 *     <li>request body stream closing ({@code StreamManagingStage});</li>
 *     <li>the request mutations ({@link RequestMutationStages#prepare});</li>
 *     <li>retries ({@link RetryDecisions}, as {@code RetryableStage}), and for each attempt: the attempt metric collector
 *     ({@code ApiCallAttemptMetricCollectionStage}), timeout exception translation
 *     ({@code TimeoutExceptionHandlingStage}), the attempt timeout ({@code ApiCallAttemptTimeoutTrackingStage}),
 *     signing ({@link RequestSigning}), {@code beforeTransmission}, the transport, {@code afterTransmission},
 *     {@code modifyHttpResponse}/{@code modifyHttpResponseContent}, {@code beforeUnmarshalling} and deserialization
 *     with {@code afterUnmarshalling}/{@code modifyResponse} ({@link ProtocolResponseHandling});</li>
 *     <li>{@code afterExecution}.</li>
 * </ol>
 *
 * <p>Requests go through the client's {@link SdkClientOption#CLIENT_TRANSPORT}, or, when none is configured, through an
 * {@link SdkHttpClientTransport} over the client's {@link SdkClientOption#SYNC_HTTP_CLIENT}, created once per pipeline.
 * The timeout trackers reach the transport's HTTP request through {@link TransportExecutionAttribute#ABORTABLE_LISTENER}.
 *
 * <p>When the call's metric collector does not collect metrics ({@link MetricUtils#collectsMetrics}), the attempt and
 * call durations, throughputs, response byte counting, error types and HTTP metrics are neither measured nor reported
 * (racecar H8, re-implemented). A call without a metric collector gets a {@link NoOpMetricCollector}: such a collector
 * is not published by anyone, so nothing published changes.
 */
@SdkInternalApi
@ThreadSafe
public final class SyncApiCallPipeline {
    private static final Logger log = Logger.loggerFor(SyncApiCallPipeline.class);

    private final HttpClientDependencies dependencies;
    private final ClientTransport<SdkHttpFullRequest, SdkHttpFullResponse> transport;
    private final ExecutionAttributesTemplate attributesTemplate;
    private final RequestMutationStages mutationStages;
    private final RequestSigning signing;
    private final ScheduledExecutorService timeoutExecutor;
    private final Duration apiCallTimeout;
    private final Duration apiCallAttemptTimeout;

    private SyncApiCallPipeline(HttpClientDependencies dependencies, TypeRegistry errorRegistry) {
        SdkClientConfiguration config = dependencies.clientConfiguration();
        this.dependencies = dependencies;
        this.transport = transport(config);
        this.attributesTemplate = ExecutionAttributesTemplate.builder(config).errorRegistry(errorRegistry).build();
        this.mutationStages = RequestMutationStages.create(dependencies, ClientType.SYNC);
        this.signing = RequestSigning.create(dependencies);
        this.timeoutExecutor = config.option(SdkClientOption.SCHEDULED_EXECUTOR_SERVICE);
        this.apiCallTimeout = config.option(SdkClientOption.API_CALL_TIMEOUT);
        this.apiCallAttemptTimeout = config.option(SdkClientOption.API_CALL_ATTEMPT_TIMEOUT);
    }

    /**
     * Creates the pipeline of a client configuration. The error registry of each call is the operation's
     * {@link ApiOperation#errorRegistry()}, unless the call's attributes already hold
     * {@link SdkExecutionAttribute#ERROR_REGISTRY}.
     *
     * @param dependencies the client's dependencies; the configuration is read once, here
     * @throws IllegalStateException if the configuration has neither a {@link SdkClientOption#CLIENT_TRANSPORT} nor a
     *     {@link SdkClientOption#SYNC_HTTP_CLIENT}, or its transport does not exchange HTTP messages
     */
    public static SyncApiCallPipeline create(HttpClientDependencies dependencies) {
        return create(dependencies, null);
    }

    /**
     * Creates the pipeline of a client configuration whose operations share one error registry (a generated client's
     * {@code <Svc>Operations.ERROR_REGISTRY}), seeded once per client as {@link SdkExecutionAttribute#ERROR_REGISTRY}.
     *
     * @param dependencies the client's dependencies; the configuration is read once, here
     * @param errorRegistry the service's error registry, or null to use each operation's
     */
    public static SyncApiCallPipeline create(HttpClientDependencies dependencies, TypeRegistry errorRegistry) {
        return new SyncApiCallPipeline(Validate.paramNotNull(dependencies, "dependencies"), errorRegistry);
    }

    /**
     * The transport the pipeline sends requests through.
     */
    ClientTransport<SdkHttpFullRequest, SdkHttpFullResponse> transport() {
        return transport;
    }

    /**
     * Calls an operation with a non-streaming output.
     *
     * @param operation the operation; it is put in the call's attributes as {@link SdkExecutionAttribute#API_OPERATION}
     *     before the execution context is created
     * @param params the call's parameters: the input, execution attributes, metric collector, host prefix and
     *     discovered endpoint are read; marshallers and response handlers are not used
     * @param contextFactory creates the call's execution context from {@code params} and runs {@code beforeExecution}
     *     and {@code modifyRequest} (the client handler's {@code invokeInterceptorsAndCreateExecutionContext}); it must
     *     use {@code params.executionAttributes()} as the call's attributes
     * @param <InputT> the input type
     * @param <OutputT> the output type
     * @return the output returned by the {@code modifyResponse} interceptors
     */
    public <InputT extends SdkRequest, OutputT extends SdkResponse> OutputT execute(
        ApiOperation<?, ?> operation,
        ClientExecutionParams<InputT, OutputT> params,
        Function<ClientExecutionParams<InputT, OutputT>, ExecutionContext> contextFactory) {
        return measureApiCallSuccess(operation, params, contextFactory, null);
    }

    /**
     * Calls an operation with a streaming output: the output and the unread response body go to the transformer, as
     * with the legacy {@code BaseSyncClientHandler.execute(params, responseTransformer)}.
     *
     * @param operation the operation
     * @param params the call's parameters
     * @param contextFactory creates the call's execution context, as for {@link #execute(ApiOperation,
     *     ClientExecutionParams, Function)}
     * @param responseTransformer receives the output and the response body of a successful response
     * @param <InputT> the input type
     * @param <OutputT> the output type
     * @param <ReturnT> the type the transformer returns
     * @return the transformer's result
     */
    public <InputT extends SdkRequest, OutputT extends SdkResponse, ReturnT> ReturnT execute(
        ApiOperation<?, ?> operation,
        ClientExecutionParams<InputT, OutputT> params,
        Function<ClientExecutionParams<InputT, OutputT>, ExecutionContext> contextFactory,
        ResponseTransformer<OutputT, ReturnT> responseTransformer) {
        return measureApiCallSuccess(operation, params, contextFactory,
                                     Validate.paramNotNull(responseTransformer, "responseTransformer"));
    }

    /**
     * Same as {@code BaseSyncClientHandler.measureApiCallSuccess}.
     */
    private <InputT extends SdkRequest, OutputT extends SdkResponse, ReturnT> ReturnT measureApiCallSuccess(
        ApiOperation<?, ?> operation,
        ClientExecutionParams<InputT, OutputT> params,
        Function<ClientExecutionParams<InputT, OutputT>, ExecutionContext> contextFactory,
        ResponseTransformer<?, ReturnT> transformer) {
        Validate.paramNotNull(operation, "operation");
        Validate.paramNotNull(params, "params");
        Validate.paramNotNull(contextFactory, "contextFactory");
        if (params.getMetricCollector() == null) {
            params.withMetricCollector(NoOpMetricCollector.create());
        }
        MetricCollector callerCollector = params.getMetricCollector();
        try {
            ReturnT result = call(operation, params, contextFactory, transformer);
            callerCollector.reportMetric(CoreMetric.API_CALL_SUCCESSFUL, true);
            return result;
        } catch (RuntimeException e) {
            callerCollector.reportMetric(CoreMetric.API_CALL_SUCCESSFUL, false);
            throw e;
        }
    }

    private <InputT extends SdkRequest, OutputT extends SdkResponse, ReturnT> ReturnT call(
        ApiOperation<?, ?> operation,
        ClientExecutionParams<InputT, OutputT> params,
        Function<ClientExecutionParams<InputT, OutputT>, ExecutionContext> contextFactory,
        ResponseTransformer<?, ReturnT> transformer) {
        // The schema-based attributes are in place before beforeExecution, so every hook sees them.
        ExecutionAttributes seeded = attributesTemplate.applyTo(params.executionAttributes());
        seeded.putAttribute(SdkExecutionAttribute.API_OPERATION, operation);
        if (seeded.getAttribute(SdkExecutionAttribute.ERROR_REGISTRY) == null) {
            seeded.putAttribute(SdkExecutionAttribute.ERROR_REGISTRY, operation.errorRegistry());
        }

        ExecutionContext executionContext = contextFactory.apply(params);
        ExecutionAttributes attributes = executionContext.executionAttributes();
        TypeRegistry errorRegistry = attributes.getAttribute(SdkExecutionAttribute.ERROR_REGISTRY);
        RequestExecutionContext context = RequestExecutionContext.builder()
                                                                 .originalRequest(executionContext.interceptorContext().request())
                                                                 .executionContext(executionContext)
                                                                 .build();
        attributes.putAttribute(TransportExecutionAttribute.ABORTABLE_LISTENER, abortableListener(context));
        ApiCall<ReturnT> apiCall = new ApiCall<>(operation, errorRegistry, transformer);

        // ExecutionFailureExceptionReportingStage, around everything after the execution context exists.
        try {
            SdkHttpFullRequest marshalled = mutationStages.marshall(executionContext, params.hostPrefixExpression(),
                                                                    params.discoveredEndpoint());
            Response<ReturnT> response = measureApiCall(marshalled, context, apiCall);
            ReturnT output = response.response();
            executionContext.interceptorChain().afterExecution(executionContext.interceptorContext(), attributes);
            return output;
        } catch (Exception e) {
            Throwable reported = ExceptionReportingUtils.reportFailureToInterceptors(context, e);
            throw ThrowableUtils.failure(reported);
        }
    }

    /**
     * Same as {@code MakeHttpRequestStage}, which registers the prepared HTTP request with both timeout trackers. The
     * trackers are read when the transport calls the listener, so one listener serves every attempt of the call.
     */
    private static Consumer<Abortable> abortableListener(RequestExecutionContext context) {
        return abortable -> {
            context.apiCallTimeoutTracker().abortable(abortable);
            context.apiCallAttemptTimeoutTracker().abortable(abortable);
        };
    }

    /**
     * Same as {@code ApiCallMetricCollectionStage}, measured only when the call collects metrics.
     */
    private <R> Response<R> measureApiCall(SdkHttpFullRequest marshalled,
                                           RequestExecutionContext context,
                                           ApiCall<R> apiCall) throws Exception {
        MetricCollector metricCollector = context.executionContext().metricCollector();
        boolean measure = MetricUtils.collectsMetrics(metricCollector);
        long callStart = measure ? System.nanoTime() : 0L;
        try {
            return withApiCallTimeout(marshalled, context, apiCall);
        } finally {
            if (measure) {
                metricCollector.reportMetric(CoreMetric.API_CALL_DURATION, Duration.ofNanos(System.nanoTime() - callStart));
                SdkHttpRequest finalRequest = context.executionContext().interceptorContext().httpRequest();
                MetricUtils.collectServiceEndpointMetrics(metricCollector,
                                                          finalRequest != null ? (SdkHttpFullRequest) finalRequest
                                                                               : marshalled);
            }
        }
    }

    /**
     * Same as {@code ApiCallTimeoutTrackingStage}.
     */
    private <R> Response<R> withApiCallTimeout(SdkHttpFullRequest marshalled,
                                               RequestExecutionContext context,
                                               ApiCall<R> apiCall) throws Exception {
        try {
            long timeoutInMillis = resolveTimeoutInMillis(context.requestConfig()::apiCallTimeout, apiCallTimeout);
            TimeoutTracker timeoutTracker = timeSyncTaskIfNeeded(timeoutExecutor, timeoutInMillis, Thread.currentThread());
            Response<R> response;
            try {
                context.apiCallTimeoutTracker(timeoutTracker);
                response = withManagedRequestStream(marshalled, context, apiCall);
            } finally {
                // As in the legacy stage: once cancelled, the tracker can no longer interrupt this thread, and if it
                // already did, hasExecuted() is true and the interrupt flag is cleared below.
                timeoutTracker.cancel();
            }
            if (timeoutTracker.hasExecuted()) {
                Thread.interrupted();
            }
            return response;
        } catch (Exception e) {
            throw translateApiCallTimeoutFailure(context, e);
        }
    }

    /**
     * Same as {@code StreamManagingStage}: the request body's current stream is closed when the call ends, and every
     * attempt reads the body through a stream whose close is ignored.
     */
    private <R> Response<R> withManagedRequestStream(SdkHttpFullRequest marshalled,
                                                     RequestExecutionContext context,
                                                     ApiCall<R> apiCall) throws Exception {
        SdkHttpFullRequest request = marshalled;
        ClosingStreamProvider managedBody = null;
        Optional<ContentStreamProvider> content = request.contentStreamProvider();
        if (content.isPresent()) {
            managedBody = new ClosingStreamProvider(content.get());
            request = request.toBuilder().contentStreamProvider(managedBody).build();
        }
        try {
            InterruptMonitor.checkInterrupted();
            SdkHttpFullRequest prepared = mutationStages.prepare(request, context);
            return executeWithRetries(prepared, context, apiCall);
        } finally {
            if (managedBody != null) {
                managedBody.closeCurrentStream();
            }
        }
    }

    /**
     * Same as {@code RetryableStage}: SDK and I/O failures, and failed responses, go to the retry decision; other
     * exceptions end the call unchanged.
     */
    private <R> Response<R> executeWithRetries(SdkHttpFullRequest prepared,
                                               RequestExecutionContext context,
                                               ApiCall<R> apiCall) throws Exception {
        RetryDecisions retries = RetryDecisions.create(prepared, context, dependencies, ClientType.SYNC);
        sleep(retries.acquireInitialToken());
        while (true) {
            SdkHttpFullRequest attemptRequest = retries.startAttempt();
            Response<R> response;
            try {
                response = executeAttempt(attemptRequest, context, apiCall);
            } catch (SdkException | IOException e) {
                backOffOrFail(retries.attemptFailed(e, retries.suggestedDelay(null)));
                continue;
            }
            retries.responseReceived(response);
            if (response.isSuccess()) {
                retries.attemptSucceeded();
                return response;
            }
            backOffOrFail(retries.attemptFailed(response.exception(), retries.suggestedDelay(response)));
        }
    }

    private static void backOffOrFail(RetryDecisions.Decision decision) throws InterruptedException {
        sleep(decision.delay());
        if (!decision.retry()) {
            throw decision.exception();
        }
    }

    private static void sleep(Duration delay) throws InterruptedException {
        TimeUnit.MILLISECONDS.sleep(delay.toMillis());
    }

    /**
     * Same as {@code ApiCallAttemptMetricCollectionStage}: a new attempt metric collector and fresh request and response
     * byte counters for each attempt; the backoff delay, HTTP metrics and error type are reported only when the attempt
     * collects metrics.
     */
    private <R> Response<R> executeAttempt(SdkHttpFullRequest request,
                                           RequestExecutionContext context,
                                           ApiCall<R> apiCall) throws Exception {
        MetricCollector attemptCollector = MetricUtils.createAttemptMetricsCollector(context);
        context.attemptMetricCollector(attemptCollector);
        boolean measure = MetricUtils.collectsMetrics(attemptCollector);
        ExecutionAttributes attributes = context.executionAttributes();
        if (measure) {
            Duration lastBackoffDelay = attributes.getAttribute(RetryableStageHelper.LAST_BACKOFF_DELAY_DURATION);
            if (lastBackoffDelay != null) {
                attemptCollector.reportMetric(CoreMetric.BACKOFF_DELAY_DURATION, lastBackoffDelay);
            }
        }
        attributes.putAttribute(SdkInternalExecutionAttribute.RESPONSE_BYTES_READ, new AtomicLong(0));
        attributes.putAttribute(InternalCoreExecutionAttribute.REQUEST_BODY_METRICS, new RequestBodyMetrics());
        try {
            Response<R> response = executeAttemptWithTimeout(request, context, apiCall);
            if (measure) {
                MetricUtils.collectHttpMetrics(attemptCollector, response.httpResponse());
                if (!Boolean.TRUE.equals(response.isSuccess()) && response.exception() != null) {
                    reportErrorType(attemptCollector, response.exception());
                }
            }
            return response;
        } catch (Exception e) {
            if (measure) {
                reportErrorType(attemptCollector, e);
            }
            throw e;
        }
    }

    private static void reportErrorType(MetricCollector attemptCollector, Exception e) {
        attemptCollector.reportMetric(CoreMetric.ERROR_TYPE, SdkErrorType.fromException(e).toString());
    }

    /**
     * Same as {@code TimeoutExceptionHandlingStage} around {@code ApiCallAttemptTimeoutTrackingStage}.
     */
    private <R> Response<R> executeAttemptWithTimeout(SdkHttpFullRequest request,
                                                      RequestExecutionContext context,
                                                      ApiCall<R> apiCall) throws Exception {
        try {
            long timeoutInMillis = resolveTimeoutInMillis(context.requestConfig()::apiCallAttemptTimeout,
                                                          apiCallAttemptTimeout);
            TimeoutTracker timeoutTracker = timeSyncTaskIfNeeded(timeoutExecutor, timeoutInMillis, Thread.currentThread());
            Response<R> response;
            try {
                context.apiCallAttemptTimeoutTracker(timeoutTracker);
                response = sendAndHandle(request, context, apiCall);
            } finally {
                timeoutTracker.cancel();
            }
            if (timeoutTracker.hasExecuted()) {
                Thread.interrupted();
            }
            return response;
        } catch (Exception e) {
            throw translateTimeoutCausedFailure(context, translateAttemptTimeoutFailure(context, e));
        }
    }

    /**
     * One attempt, from signing to the deserialized response: {@code SigningStage},
     * {@code BeforeTransmissionExecutionInterceptorsStage}, {@code MakeHttpRequestStage},
     * {@code AfterTransmissionExecutionInterceptorsStage}, {@code BeforeUnmarshallingExecutionInterceptorsStage} and
     * {@code HandleResponseStage}.
     */
    private <R> Response<R> sendAndHandle(SdkHttpFullRequest request,
                                          RequestExecutionContext context,
                                          ApiCall<R> apiCall) throws Exception {
        SdkHttpFullRequest signed = signing.sign(request, context);
        ExecutionContext executionContext = context.executionContext();
        ExecutionAttributes attributes = executionContext.executionAttributes();

        InterruptMonitor.checkInterrupted();
        executionContext.interceptorChain().beforeTransmission(executionContext.interceptorContext(), attributes);

        InterruptMonitor.checkInterrupted();
        SdkHttpFullResponse transmitted = send(signed, context);

        SdkHttpFullResponse response = afterTransmission(transmitted, executionContext);

        executionContext.interceptorChain().beforeUnmarshalling(executionContext.interceptorContext(), attributes);
        InterruptMonitor.checkInterrupted(response);

        return handleResponse(signed, response, context, apiCall);
    }

    /**
     * The transport call of {@code MakeHttpRequestStage}: the HTTP metric collector goes to the transport, which records
     * the attempt start and headers-read times; {@code SERVICE_CALL_DURATION} and {@code TIME_TO_FIRST_BYTE} are their
     * difference, reported when the attempt collects metrics. The pipeline records the times itself as well, for a
     * transport that does not.
     */
    private SdkHttpFullResponse send(SdkHttpFullRequest request, RequestExecutionContext context) {
        ExecutionAttributes attributes = context.executionAttributes();
        attributes.putAttribute(TransportExecutionAttribute.HTTP_METRIC_COLLECTOR,
                                MetricUtils.createHttpMetricsCollector(context));
        long sendStart = System.nanoTime();
        attributes.putAttribute(SdkInternalExecutionAttribute.API_CALL_ATTEMPT_START_NANO_TIME, sendStart);

        SdkHttpFullResponse response = transport.send(attributes, request);

        Long headersRead = attributes.getAttribute(SdkInternalExecutionAttribute.HEADERS_READ_END_NANO_TIME);
        if (headersRead == null || headersRead < sendStart) {
            headersRead = System.nanoTime();
            attributes.putAttribute(SdkInternalExecutionAttribute.HEADERS_READ_END_NANO_TIME, headersRead);
        }
        MetricCollector attemptCollector = context.attemptMetricCollector();
        if (MetricUtils.collectsMetrics(attemptCollector)) {
            long attemptStart = attributes.getAttribute(SdkInternalExecutionAttribute.API_CALL_ATTEMPT_START_NANO_TIME);
            Duration executeDuration = Duration.ofNanos(headersRead - attemptStart);
            attemptCollector.reportMetric(CoreMetric.SERVICE_CALL_DURATION, executeDuration);
            attemptCollector.reportMetric(CoreMetric.TIME_TO_FIRST_BYTE, executeDuration);
        }
        return response;
    }

    /**
     * Same as {@code AfterTransmissionExecutionInterceptorsStage}. The response is copied with the interceptors' body
     * only when an interceptor replaced the response or its body; otherwise it already holds that body.
     */
    private static SdkHttpFullResponse afterTransmission(SdkHttpFullResponse transmitted,
                                                         ExecutionContext executionContext) throws InterruptedException {
        InterruptMonitor.checkInterrupted(transmitted);
        ExecutionAttributes attributes = executionContext.executionAttributes();
        AbortableInputStream transmittedBody = transmitted.content().orElse(null);
        InterceptorContext interceptorContext =
            executionContext.interceptorContext().copy(b -> b.httpResponse(transmitted).responseBody(transmittedBody));
        executionContext.interceptorChain().afterTransmission(interceptorContext, attributes);
        interceptorContext = executionContext.interceptorChain().modifyHttpResponse(interceptorContext, attributes);
        executionContext.interceptorContext(interceptorContext);

        SdkHttpFullResponse response = (SdkHttpFullResponse) interceptorContext.httpResponse();
        InterruptMonitor.checkInterrupted(response);
        Optional<InputStream> body = interceptorContext.responseBody();
        if (body.isPresent() && (response != transmitted || body.get() != transmittedBody)) {
            response = response.toBuilder().content(AbortableInputStream.create(body.get())).build();
        }
        return response;
    }

    /**
     * Same as {@code HandleResponseStage} with the new path's deserialization step: the response body is counted and
     * the time-to-last-byte and throughput metrics are reported only when the attempt collects metrics.
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    private static <R> Response<R> handleResponse(SdkHttpFullRequest request,
                                                  SdkHttpFullResponse response,
                                                  RequestExecutionContext context,
                                                  ApiCall<R> apiCall) throws Exception {
        MetricCollector attemptCollector = context.attemptMetricCollector();
        boolean measure = MetricUtils.collectsMetrics(attemptCollector);
        SdkHttpFullResponse handled = measure ? trackBytesRead(response, context) : response;
        ExecutionContext executionContext = context.executionContext();
        ApiOperation operation = apiCall.operation;
        Response<R> result;
        if (apiCall.transformer == null) {
            result = (Response<R>) ProtocolResponseHandling.handle(executionContext, operation, apiCall.errorRegistry,
                                                                   request, handled);
        } else {
            result = ProtocolResponseHandling.handleStreaming(executionContext, operation, apiCall.errorRegistry, request,
                                                              handled, (ResponseTransformer) apiCall.transformer);
        }
        if (measure) {
            collectResponseMetrics(context, attemptCollector);
        }
        return result;
    }

    private static SdkHttpFullResponse trackBytesRead(SdkHttpFullResponse response, RequestExecutionContext context) {
        Optional<AbortableInputStream> content = response.content();
        if (!content.isPresent()) {
            return response;
        }
        AtomicLong bytesRead = context.executionAttributes().getAttribute(SdkInternalExecutionAttribute.RESPONSE_BYTES_READ);
        AbortableInputStream tracked = AbortableInputStream.create(new BytesReadTrackingInputStream(content.get(), bytesRead));
        return response.toBuilder().content(tracked).build();
    }

    /**
     * Same as {@code HandleResponseStage.collectMetrics}.
     */
    private static void collectResponseMetrics(RequestExecutionContext context, MetricCollector attemptCollector) {
        ExecutionAttributes attributes = context.executionAttributes();
        long attemptStart = attributes.getAttribute(SdkInternalExecutionAttribute.API_CALL_ATTEMPT_START_NANO_TIME);
        long now = System.nanoTime();
        attemptCollector.reportMetric(CoreMetric.TIME_TO_LAST_BYTE, Duration.ofNanos(now - attemptStart));

        long responseReadStart = MetricUtils.responseHeadersReadEndNanoTime(context).getAsLong();
        long responseBytesRead = MetricUtils.apiCallAttemptResponseBytesRead(context).getAsLong();
        attemptCollector.reportMetric(CoreMetric.READ_THROUGHPUT,
                                      MetricUtils.bytesPerSec(responseBytesRead, responseReadStart, now));

        RequestBodyMetrics bodyMetrics = attributes.getAttribute(InternalCoreExecutionAttribute.REQUEST_BODY_METRICS);
        if (bodyMetrics == null) {
            return;
        }
        long bytesWritten = bodyMetrics.bytesWritten().get();
        long firstByteTime = bodyMetrics.firstByteWrittenNanoTime().get();
        if (bytesWritten > 0 && firstByteTime > 0) {
            long lastByteTime = bodyMetrics.lastByteWrittenNanoTime().get();
            if (firstByteTime != lastByteTime) {
                attemptCollector.reportMetric(CoreMetric.WRITE_THROUGHPUT,
                                              MetricUtils.bytesPerSec(bytesWritten, firstByteTime, lastByteTime));
            }
        }
    }

    /**
     * Same as {@code ApiCallTimeoutTrackingStage.translatePipelineException}.
     */
    private Exception translateApiCallTimeoutFailure(RequestExecutionContext context, Exception e) {
        if (e instanceof InterruptedException) {
            closeInterruptedResponse(e);
            if (apiCallTimerExecuted(context)) {
                Thread.interrupted();
                return ApiCallTimeoutException.create(resolveTimeoutInMillis(context.requestConfig()::apiCallTimeout,
                                                                             apiCallTimeout));
            }
            Thread.currentThread().interrupt();
            return AbortedException.create("Thread was interrupted", e);
        }
        if (apiCallTimerExecuted(context)) {
            // The call already failed: keep that failure, which is more useful than a timeout exception.
            Thread.interrupted();
        }
        return e;
    }

    /**
     * Same as {@code ApiCallAttemptTimeoutTrackingStage.translatePipelineException}.
     */
    private Exception translateAttemptTimeoutFailure(RequestExecutionContext context, Exception e) {
        TimeoutTracker attemptTracker = context.apiCallAttemptTimeoutTracker();
        boolean attemptTimerExecuted = attemptTracker != null && attemptTracker.hasExecuted();
        if (e instanceof InterruptedException) {
            closeInterruptedResponse(e);
            if (attemptTimerExecuted) {
                Thread.interrupted();
                return attemptTimeoutException(context);
            }
            Thread.currentThread().interrupt();
            return AbortedException.create("Thread was interrupted", e);
        }
        if (attemptTimerExecuted) {
            Thread.interrupted();
        }
        return e;
    }

    /**
     * Same as {@code TimeoutExceptionHandlingStage.translatePipelineException}: a failure caused by the API call timeout
     * becomes an {@link InterruptedException}, which no retry catches and the API call timeout step turns into an
     * {@link ApiCallTimeoutException}; one caused by the attempt timeout becomes a retryable
     * {@link ApiCallAttemptTimeoutException}.
     */
    private Exception translateTimeoutCausedFailure(RequestExecutionContext context, Exception e) {
        boolean timeoutCaused = e instanceof InterruptedException
                                || e instanceof IOException
                                || e instanceof AbortedException
                                || Thread.currentThread().isInterrupted()
                                || (e instanceof SdkClientException && context.apiCallAttemptTimeoutTracker().hasExecuted());
        if (!timeoutCaused) {
            return e;
        }
        closeInterruptedResponse(e);
        if (context.apiCallTimeoutTracker().hasExecuted()) {
            return new InterruptedException();
        }
        if (context.apiCallAttemptTimeoutTracker().hasExecuted()) {
            Thread.interrupted();
            return attemptTimeoutException(context);
        }
        if (e instanceof InterruptedException) {
            Thread.currentThread().interrupt();
            return AbortedException.create("Thread was interrupted", e);
        }
        return e;
    }

    private ApiCallAttemptTimeoutException attemptTimeoutException(RequestExecutionContext context) {
        return ApiCallAttemptTimeoutException.create(resolveTimeoutInMillis(context.requestConfig()::apiCallAttemptTimeout,
                                                                            apiCallAttemptTimeout));
    }

    private static boolean apiCallTimerExecuted(RequestExecutionContext context) {
        return context.apiCallTimeoutTracker() != null && context.apiCallTimeoutTracker().hasExecuted();
    }

    private static void closeInterruptedResponse(Exception e) {
        if (e instanceof SdkInterruptedException) {
            ((SdkInterruptedException) e).getResponseStream()
                                         .ifPresent(r -> runAndLogError(log.logger(), "Failed to close the response stream",
                                                                        r::close));
        }
    }

    /**
     * The configured transport, or an SDK HTTP client transport over the configuration's HTTP client. The configuration
     * owns that client and closes it, so the transport does not.
     */
    @SuppressWarnings("unchecked")
    private static ClientTransport<SdkHttpFullRequest, SdkHttpFullResponse> transport(SdkClientConfiguration config) {
        ClientTransport<?, ?> configured = config.option(SdkClientOption.CLIENT_TRANSPORT);
        if (configured == null) {
            SdkHttpClient httpClient = config.option(SdkClientOption.SYNC_HTTP_CLIENT);
            Validate.validState(httpClient != null,
                                "The client configuration has neither a CLIENT_TRANSPORT nor a SYNC_HTTP_CLIENT");
            return SdkHttpClientTransport.create(httpClient, false);
        }
        Validate.validState(HttpMessageExchange.INSTANCE.equals(configured.messageExchange()),
                            "Transport %s exchanges %s, but the request pipeline sends %s", configured,
                            configured.messageExchange(), HttpMessageExchange.INSTANCE);
        return (ClientTransport<SdkHttpFullRequest, SdkHttpFullResponse>) configured;
    }

    /**
     * The per-call values the attempt steps need.
     */
    private static final class ApiCall<R> {
        private final ApiOperation<?, ?> operation;
        private final TypeRegistry errorRegistry;
        private final ResponseTransformer<?, R> transformer;

        private ApiCall(ApiOperation<?, ?> operation, TypeRegistry errorRegistry, ResponseTransformer<?, R> transformer) {
            this.operation = operation;
            this.errorRegistry = errorRegistry;
            this.transformer = transformer;
        }
    }

    /**
     * Same as {@code StreamManagingStage.ClosingStreamProvider}, which is private to that stage.
     */
    private static final class ClosingStreamProvider implements ContentStreamProvider {
        private final ContentStreamProvider wrapped;
        private InputStream currentStream;

        private ClosingStreamProvider(ContentStreamProvider wrapped) {
            this.wrapped = wrapped;
        }

        @Override
        public InputStream newStream() {
            currentStream = wrapped.newStream();
            return ReleasableInputStream.wrap(currentStream).disableClose();
        }

        @Override
        public ByteBuffer contentAsByteBufferOrNull() {
            // The bytes are sent unchanged, and an in-memory buffer has no stream to close.
            return wrapped.contentAsByteBufferOrNull();
        }

        private void closeCurrentStream() {
            if (currentStream != null) {
                invokeSafely(currentStream::close);
                currentStream = null;
            }
        }
    }
}
