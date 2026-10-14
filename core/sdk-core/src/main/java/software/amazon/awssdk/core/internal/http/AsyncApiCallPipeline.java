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

import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static software.amazon.awssdk.core.internal.http.timers.TimerUtils.resolveTimeoutInMillis;
import static software.amazon.awssdk.core.internal.http.timers.TimerUtils.timeAsyncTaskIfNeeded;
import static software.amazon.awssdk.http.Header.CONTENT_LENGTH;
import static software.amazon.awssdk.http.Header.TRANSFER_ENCODING;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.ByteBuffer;
import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.Executor;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Function;
import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.annotations.ThreadSafe;
import software.amazon.awssdk.core.ClientType;
import software.amazon.awssdk.core.Response;
import software.amazon.awssdk.core.SdkRequest;
import software.amazon.awssdk.core.SdkResponse;
import software.amazon.awssdk.core.async.AsyncRequestBody;
import software.amazon.awssdk.core.async.AsyncResponseTransformer;
import software.amazon.awssdk.core.async.SdkPublisher;
import software.amazon.awssdk.core.client.config.SdkAdvancedAsyncClientOption;
import software.amazon.awssdk.core.client.config.SdkClientConfiguration;
import software.amazon.awssdk.core.client.config.SdkClientOption;
import software.amazon.awssdk.core.client.handler.ClientExecutionParams;
import software.amazon.awssdk.core.client.protocol.AsyncClientTransport;
import software.amazon.awssdk.core.client.protocol.HttpMessageExchange;
import software.amazon.awssdk.core.client.protocol.OwnedResponseBody;
import software.amazon.awssdk.core.exception.ApiCallAttemptTimeoutException;
import software.amazon.awssdk.core.exception.ApiCallTimeoutException;
import software.amazon.awssdk.core.exception.SdkException;
import software.amazon.awssdk.core.http.ExecutionContext;
import software.amazon.awssdk.core.interceptor.ExecutionAttributes;
import software.amazon.awssdk.core.interceptor.ExecutionInterceptorChain;
import software.amazon.awssdk.core.interceptor.InterceptorContext;
import software.amazon.awssdk.core.interceptor.SdkExecutionAttribute;
import software.amazon.awssdk.core.interceptor.SdkInternalExecutionAttribute;
import software.amazon.awssdk.core.internal.InternalCoreExecutionAttribute;
import software.amazon.awssdk.core.internal.http.pipeline.stages.utils.ExceptionReportingUtils;
import software.amazon.awssdk.core.internal.http.pipeline.stages.utils.RetryableStageHelper;
import software.amazon.awssdk.core.internal.http.timers.NoOpTimeoutTracker;
import software.amazon.awssdk.core.internal.http.timers.TimeoutTracker;
import software.amazon.awssdk.core.internal.http.transport.ResponseBodyAggregator;
import software.amazon.awssdk.core.internal.http.transport.SdkAsyncHttpClientTransport;
import software.amazon.awssdk.core.internal.http.transport.TransportExecutionAttribute;
import software.amazon.awssdk.core.internal.metrics.RequestBodyMetrics;
import software.amazon.awssdk.core.internal.metrics.SdkErrorType;
import software.amazon.awssdk.core.internal.util.MetricUtils;
import software.amazon.awssdk.core.internal.util.ThrowableUtils;
import software.amazon.awssdk.core.metrics.CoreMetric;
import software.amazon.awssdk.http.AbortableInputStream;
import software.amazon.awssdk.http.ContentStreamProvider;
import software.amazon.awssdk.http.SdkHttpFullRequest;
import software.amazon.awssdk.http.SdkHttpFullResponse;
import software.amazon.awssdk.http.SdkHttpMethod;
import software.amazon.awssdk.http.SdkHttpRequest;
import software.amazon.awssdk.http.SdkHttpResponse;
import software.amazon.awssdk.http.async.SdkAsyncHttpClient;
import software.amazon.awssdk.metrics.MetricCollector;
import software.amazon.awssdk.metrics.NoOpMetricCollector;
import software.amazon.awssdk.schema.ApiOperation;
import software.amazon.awssdk.schema.serde.TypeRegistry;
import software.amazon.awssdk.utils.CompletableFutureUtils;
import software.amazon.awssdk.utils.IoUtils;
import software.amazon.awssdk.utils.Logger;
import software.amazon.awssdk.utils.Validate;
import software.amazon.awssdk.utils.async.SimplePublisher;

/**
 * The asynchronous schema-based request pipeline (design D.6): one call of an {@link ApiOperation} on a client whose
 * requests are serialized by a {@code ClientProtocol} and sent by an {@link AsyncClientTransport}. One instance is created
 * per client configuration and every call reuses it; the per-call state lives in a call object that the returned future
 * outlives.
 *
 * <p>Each call runs the same steps as {@link SyncApiCallPipeline}, through the same shared stages (racecar G2,
 * re-implemented): {@link RequestMutationStages}, {@link RetryDecisions}, {@link RequestSigning},
 * {@link ProtocolResponseHandling} and {@link ExecutionAttributesTemplate}. The steps reproduce the legacy async client
 * handler and the stage graph {@code AmazonAsyncHttpClient} composes, outermost first:
 * <ol>
 *     <li>{@code API_CALL_SUCCESSFUL} reporting ({@code BaseAsyncClientHandler.measureApiCallSuccess});</li>
 *     <li>the call's execution context, created by the client handler's factory, which runs {@code beforeExecution}
 *     and {@code modifyRequest};</li>
 *     <li>failure reporting ({@code AsyncExecutionFailureExceptionReportingStage} and the handler's exception
 *     translation): {@code modifyException} and {@code onExecutionFailure} once, then the returned future fails with a
 *     {@link CompletionException} around the {@link SdkException};</li>
 *     <li>serialization and the HTTP request interceptors ({@link RequestMutationStages#marshall});</li>
 *     <li>{@code API_CALL_DURATION} and {@code SERVICE_ENDPOINT} ({@code AsyncApiCallMetricCollectionStage});</li>
 *     <li>the API call timeout ({@code AsyncApiCallTimeoutTrackingStage});</li>
 *     <li>the request mutations ({@link RequestMutationStages#prepare});</li>
 *     <li>retries ({@link RetryDecisions}, as {@code AsyncRetryableStage}), and for each attempt: the attempt metric
 *     collector ({@code AsyncApiCallAttemptMetricCollectionStage}), signing ({@link RequestSigning#signAsync}),
 *     {@code beforeTransmission}, the transport with the attempt timeout ({@code MakeAsyncHttpRequestStage}),
 *     {@code afterTransmission}, {@code modifyHttpResponse}/{@code modifyHttpResponseContent},
 *     {@code beforeUnmarshalling} and {@code modifyAsyncHttpResponseContent}
 *     ({@code AsyncAfterTransmissionInterceptorCallingResponseHandler}), and deserialization with
 *     {@code afterUnmarshalling}/{@code modifyResponse} ({@link ProtocolResponseHandling});</li>
 *     <li>{@code afterExecution}.</li>
 * </ol>
 *
 * <p>The asynchronous parts are limited to scheduling and future completion. Nothing blocks a thread: the transport
 * buffers each response, signing runs inline when the identity is already resolved (racecar C) and otherwise when its
 * future completes, retries wait on the configuration's {@link SdkClientOption#SCHEDULED_EXECUTOR_SERVICE}, and each
 * attempt's response is handled on the {@link SdkAdvancedAsyncClientOption#FUTURE_COMPLETION_EXECUTOR}, as the legacy
 * stage completes its future there. A call creates the returned future and, per attempt, the transport's future, plus a
 * signing future only when signing is deferred and a timeout future only when an API call timeout is configured (H9:
 * the legacy graph creates about a dozen futures per attempt). Cancelling or failing the returned future cancels the
 * in-flight transport or signing future, a pending retry and the timeouts.
 *
 * <p>Requests go through the client's {@link SdkClientOption#ASYNC_CLIENT_TRANSPORT}, or, when none is configured,
 * through an {@link SdkAsyncHttpClientTransport} over the client's {@link SdkClientOption#ASYNC_HTTP_CLIENT}, created
 * once per pipeline. When the call's metric collector does not collect metrics, nothing is measured (racecar H8, as in
 * {@link SyncApiCallPipeline}).
 *
 * <p>Operations with a streaming output ({@link AsyncResponseTransformer}) are not supported yet: their body hand-off
 * through the transport's {@code AsyncResponseHandoff} is part of task 7.19.
 */
@SdkInternalApi
@ThreadSafe
public final class AsyncApiCallPipeline {
    private static final Logger log = Logger.loggerFor(AsyncApiCallPipeline.class);

    private final HttpClientDependencies dependencies;
    private final AsyncClientTransport<SdkHttpFullRequest, SdkHttpFullResponse> transport;
    private final ExecutionAttributesTemplate attributesTemplate;
    private final RequestMutationStages mutationStages;
    private final RequestSigning signing;
    private final ScheduledExecutorService scheduledExecutor;
    private final Executor futureCompletionExecutor;
    private final Duration apiCallTimeout;
    private final Duration apiCallAttemptTimeout;

    private AsyncApiCallPipeline(HttpClientDependencies dependencies, TypeRegistry errorRegistry) {
        SdkClientConfiguration config = dependencies.clientConfiguration();
        this.dependencies = dependencies;
        this.transport = transport(config);
        this.attributesTemplate = ExecutionAttributesTemplate.builder(config).errorRegistry(errorRegistry).build();
        this.mutationStages = RequestMutationStages.create(dependencies, ClientType.ASYNC);
        this.signing = RequestSigning.create(dependencies);
        this.scheduledExecutor = config.option(SdkClientOption.SCHEDULED_EXECUTOR_SERVICE);
        Validate.validState(scheduledExecutor != null,
                            "The client configuration has no SCHEDULED_EXECUTOR_SERVICE for retries and timeouts");
        this.futureCompletionExecutor = config.option(SdkAdvancedAsyncClientOption.FUTURE_COMPLETION_EXECUTOR);
        this.apiCallTimeout = config.option(SdkClientOption.API_CALL_TIMEOUT);
        this.apiCallAttemptTimeout = config.option(SdkClientOption.API_CALL_ATTEMPT_TIMEOUT);
    }

    /**
     * Creates the pipeline of a client configuration. The error registry of each call is the operation's
     * {@link ApiOperation#errorRegistry()}, unless the call's attributes already hold
     * {@link SdkExecutionAttribute#ERROR_REGISTRY}.
     *
     * @param dependencies the client's dependencies; the configuration is read once, here
     * @throws IllegalStateException if the configuration has neither an {@link SdkClientOption#ASYNC_CLIENT_TRANSPORT}
     *     nor an {@link SdkClientOption#ASYNC_HTTP_CLIENT}, its transport does not exchange HTTP messages, or it has no
     *     {@link SdkClientOption#SCHEDULED_EXECUTOR_SERVICE}
     */
    public static AsyncApiCallPipeline create(HttpClientDependencies dependencies) {
        return create(dependencies, null);
    }

    /**
     * Creates the pipeline of a client configuration whose operations share one error registry (a generated client's
     * {@code <Svc>Operations.ERROR_REGISTRY}), seeded once per client as {@link SdkExecutionAttribute#ERROR_REGISTRY}.
     *
     * @param dependencies the client's dependencies; the configuration is read once, here
     * @param errorRegistry the service's error registry, or null to use each operation's
     */
    public static AsyncApiCallPipeline create(HttpClientDependencies dependencies, TypeRegistry errorRegistry) {
        return new AsyncApiCallPipeline(Validate.paramNotNull(dependencies, "dependencies"), errorRegistry);
    }

    /**
     * The transport the pipeline sends requests through.
     */
    AsyncClientTransport<SdkHttpFullRequest, SdkHttpFullResponse> transport() {
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
     * @return a future completed with the output returned by the {@code modifyResponse} interceptors. As with the legacy
     *     handler, a failure of the context factory fails it with that exception, and any later failure with a
     *     {@link CompletionException} whose cause is the {@link SdkException} ({@code get()} throws an
     *     {@code ExecutionException} with that cause)
     */
    public <InputT extends SdkRequest, OutputT extends SdkResponse> CompletableFuture<OutputT> execute(
        ApiOperation<?, ?> operation,
        ClientExecutionParams<InputT, OutputT> params,
        Function<ClientExecutionParams<InputT, OutputT>, ExecutionContext> contextFactory) {
        Validate.paramNotNull(operation, "operation");
        Validate.paramNotNull(params, "params");
        Validate.paramNotNull(contextFactory, "contextFactory");
        return measureApiCallSuccess(operation, params, contextFactory);
    }

    /**
     * Calls an operation with a streaming output. Not supported yet: the response body hand-off to an
     * {@link AsyncResponseTransformer} is part of task 7.19.
     *
     * @throws UnsupportedOperationException always, before any interceptor runs
     */
    public <InputT extends SdkRequest, OutputT extends SdkResponse, ReturnT> CompletableFuture<ReturnT> execute(
        ApiOperation<?, ?> operation,
        ClientExecutionParams<InputT, OutputT> params,
        Function<ClientExecutionParams<InputT, OutputT>, ExecutionContext> contextFactory,
        AsyncResponseTransformer<OutputT, ReturnT> asyncResponseTransformer) {
        Validate.paramNotNull(operation, "operation");
        Validate.paramNotNull(params, "params");
        Validate.paramNotNull(contextFactory, "contextFactory");
        Validate.paramNotNull(asyncResponseTransformer, "asyncResponseTransformer");
        throw new UnsupportedOperationException("The asynchronous schema-based request pipeline does not support "
                                                + "operations with a streaming output yet: " + operation.schema().id());
    }

    /**
     * Same as {@code BaseAsyncClientHandler.measureApiCallSuccess} around the context factory; the call reports the
     * metric when it ends.
     */
    private <InputT extends SdkRequest, OutputT extends SdkResponse> CompletableFuture<OutputT> measureApiCallSuccess(
        ApiOperation<?, ?> operation,
        ClientExecutionParams<InputT, OutputT> params,
        Function<ClientExecutionParams<InputT, OutputT>, ExecutionContext> contextFactory) {
        if (params.getMetricCollector() == null) {
            params.withMetricCollector(NoOpMetricCollector.create());
        }
        MetricCollector callerCollector = params.getMetricCollector();
        ExecutionContext executionContext;
        try {
            executionContext = createExecutionContext(operation, params, contextFactory);
        } catch (RuntimeException e) {
            callerCollector.reportMetric(CoreMetric.API_CALL_SUCCESSFUL, false);
            return CompletableFutureUtils.failedFuture(e);
        }
        AsyncCall<OutputT> call = new AsyncCall<>(operation, executionContext, callerCollector);
        return call.start(params.hostPrefixExpression(), params.discoveredEndpoint());
    }

    private <InputT extends SdkRequest, OutputT extends SdkResponse> ExecutionContext createExecutionContext(
        ApiOperation<?, ?> operation,
        ClientExecutionParams<InputT, OutputT> params,
        Function<ClientExecutionParams<InputT, OutputT>, ExecutionContext> contextFactory) {
        // The schema-based attributes are in place before beforeExecution, so every hook sees them.
        ExecutionAttributes seeded = attributesTemplate.applyTo(params.executionAttributes());
        seeded.putAttribute(SdkExecutionAttribute.API_OPERATION, operation);
        if (seeded.getAttribute(SdkExecutionAttribute.ERROR_REGISTRY) == null) {
            seeded.putAttribute(SdkExecutionAttribute.ERROR_REGISTRY, operation.errorRegistry());
        }
        return contextFactory.apply(params);
    }

    /**
     * Runs a task on the future completion executor, as {@code MakeAsyncHttpRequestStage} completes its future there,
     * or on the current thread when there is no executor or it rejects the task.
     */
    private void onFutureCompletionExecutor(Runnable task) {
        Executor executor = futureCompletionExecutor;
        if (executor == null) {
            task.run();
            return;
        }
        try {
            executor.execute(task);
        } catch (Throwable t) {
            log.debug(() -> String.format("Could not complete the service call future on the provided "
                                          + "FUTURE_COMPLETION_EXECUTOR. The future will be completed synchronously by thread"
                                          + " %s. This may be an indication that the executor is being overwhelmed by too"
                                          + " many requests, and it may negatively affect performance. Consider changing "
                                          + "the configuration of the executor to accommodate the load through the client.",
                                          Thread.currentThread().getName()),
                      t);
            task.run();
        }
    }

    /**
     * The configured transport, or an SDK HTTP client transport over the configuration's async HTTP client. The
     * configuration owns that client and closes it, so the transport does not.
     */
    @SuppressWarnings("unchecked")
    private static AsyncClientTransport<SdkHttpFullRequest, SdkHttpFullResponse> transport(SdkClientConfiguration config) {
        AsyncClientTransport<?, ?> configured = config.option(SdkClientOption.ASYNC_CLIENT_TRANSPORT);
        if (configured == null) {
            SdkAsyncHttpClient httpClient = config.option(SdkClientOption.ASYNC_HTTP_CLIENT);
            Validate.validState(httpClient != null,
                                "The client configuration has neither an ASYNC_CLIENT_TRANSPORT nor an ASYNC_HTTP_CLIENT");
            return SdkAsyncHttpClientTransport.create(httpClient, false);
        }
        Validate.validState(HttpMessageExchange.INSTANCE.equals(configured.messageExchange()),
                            "Transport %s exchanges %s, but the request pipeline sends %s", configured,
                            configured.messageExchange(), HttpMessageExchange.INSTANCE);
        return (AsyncClientTransport<SdkHttpFullRequest, SdkHttpFullResponse>) configured;
    }

    /**
     * Same as {@code MakeAsyncHttpRequestStage.getRequestWithContentLength}: a request with a body and no
     * {@code Content-Length} or {@code Transfer-Encoding} gets the body's length. The legacy stage reads a body without
     * an in-memory buffer into memory to publish it ({@code SimpleHttpContentPublisher}); here such a body is read once
     * and put back as a buffer, which the transport then publishes without reading it again.
     */
    private static SdkHttpFullRequest withContentLength(SdkHttpFullRequest request, AsyncRequestBody asyncBody)
            throws IOException {
        if (request.method() == SdkHttpMethod.GET || request.method() == SdkHttpMethod.HEAD
            || request.firstMatchingHeader(CONTENT_LENGTH).isPresent()
            || request.firstMatchingHeader(TRANSFER_ENCODING).isPresent()) {
            return request;
        }
        if (asyncBody != null) {
            Optional<Long> length = asyncBody.contentLength();
            return length.isPresent() ? request.toBuilder().putHeader(CONTENT_LENGTH, String.valueOf(length.get())).build()
                                      : request;
        }
        Optional<ContentStreamProvider> content = request.contentStreamProvider();
        if (!content.isPresent()) {
            return request.toBuilder().putHeader(CONTENT_LENGTH, "0").build();
        }
        ByteBuffer buffer = content.get().contentAsByteBufferOrNull();
        if (buffer != null) {
            return request.toBuilder().putHeader(CONTENT_LENGTH, String.valueOf(buffer.remaining())).build();
        }
        byte[] bytes;
        try (InputStream stream = content.get().newStream()) {
            bytes = IoUtils.toByteArray(stream);
        }
        return request.toBuilder()
                      .contentStreamProvider(ContentStreamProvider.fromByteArrayUnsafe(bytes))
                      .putHeader(CONTENT_LENGTH, String.valueOf(bytes.length))
                      .build();
    }

    private static SdkHttpFullResponse withoutContent(SdkHttpFullResponse response) {
        return response.content().isPresent() ? response.toBuilder().content(null).build() : response;
    }

    /**
     * The response to deserialize: the transmitted response unless an interceptor replaced its headers or its body.
     */
    private static SdkHttpFullResponse withContent(SdkHttpResponse modified,
                                                   SdkHttpFullResponse headersOnly,
                                                   SdkHttpFullResponse transmitted,
                                                   AbortableInputStream content) {
        if (modified == headersOnly && content == transmitted.content().orElse(null)) {
            return transmitted;
        }
        SdkHttpFullResponse.Builder builder;
        if (modified instanceof SdkHttpFullResponse) {
            builder = ((SdkHttpFullResponse) modified).toBuilder();
        } else {
            builder = SdkHttpFullResponse.builder()
                                         .statusCode(modified.statusCode())
                                         .statusText(modified.statusText().orElse(null));
            modified.forEachHeader(builder::putHeader);
        }
        return builder.content(content).build();
    }

    private static Throwable unwrap(Throwable failure) {
        Throwable cause = failure;
        while (cause instanceof CompletionException && cause.getCause() != null) {
            cause = cause.getCause();
        }
        return cause;
    }

    /**
     * The error type of a failed attempt. The legacy async stage classifies the failure's cause, which for a transport
     * failure ({@code SdkClientException("Unable to execute HTTP request", IOException)}) is the I/O exception; the
     * failure itself is classified first, so a timeout is a configured timeout.
     */
    private static void reportErrorType(MetricCollector attemptCollector, Throwable failure) {
        SdkErrorType errorType = SdkErrorType.fromException(failure);
        if (errorType == SdkErrorType.OTHER && failure.getCause() != null) {
            errorType = SdkErrorType.fromException(failure.getCause());
        }
        attemptCollector.reportMetric(CoreMetric.ERROR_TYPE, errorType.toString());
    }

    private static void closeQuietly(SdkHttpFullResponse response) {
        if (response != null) {
            response.content().ifPresent(content -> IoUtils.closeQuietlyV2(content, log));
        }
    }

    /**
     * One call. Its steps run one at a time: each step starts the next directly, from a future's completion, or from a
     * scheduled task, which orders their reads and writes of the call's state. Only the end of the call can race with
     * them (an API call timeout or a cancellation of the returned future); {@link #finished} decides which end wins, and
     * every step stops once the call has ended.
     */
    private final class AsyncCall<R> {
        private final ApiOperation<?, ?> operation;
        private final TypeRegistry errorRegistry;
        private final ExecutionContext executionContext;
        private final ExecutionAttributes attributes;
        private final RequestExecutionContext context;
        private final MetricCollector callerCollector;
        private final CompletableFuture<R> returned = new CompletableFuture<>();
        private final AtomicBoolean finished = new AtomicBoolean();
        private volatile CompletableFuture<?> inFlight;
        private volatile ScheduledFuture<?> scheduled;
        private volatile TimeoutTracker apiCallTimeoutTracker = NoOpTimeoutTracker.INSTANCE;

        // Written by start(). The call metrics are volatile because the end of the call (an API call timeout) may read
        // them on the scheduler's thread; the other fields are written before the step that reads them is scheduled.
        private volatile SdkHttpFullRequest marshalled;
        private volatile boolean measureCall;
        private volatile long callStart;
        private RetryDecisions retries;
        private AsyncRequestBody originalRequestBody;

        private AsyncCall(ApiOperation<?, ?> operation, ExecutionContext executionContext, MetricCollector callerCollector) {
            this.operation = operation;
            this.executionContext = executionContext;
            this.attributes = executionContext.executionAttributes();
            this.errorRegistry = attributes.getAttribute(SdkExecutionAttribute.ERROR_REGISTRY);
            this.callerCollector = callerCollector;
            this.context = RequestExecutionContext.builder()
                                                  .originalRequest(executionContext.interceptorContext().request())
                                                  .executionContext(executionContext)
                                                  .build();
        }

        /**
         * Runs the call up to its first attempt (or the wait before it) and returns the call's future.
         */
        private CompletableFuture<R> start(String hostPrefix, URI discoveredEndpoint) {
            returned.whenComplete((r, t) -> {
                if (t != null) {
                    cancelled(t);
                }
            });
            try {
                marshalled = mutationStages.marshall(executionContext, hostPrefix, discoveredEndpoint);
                context.requestProvider(executionContext.interceptorContext().asyncRequestBody().orElse(null));

                // AsyncApiCallMetricCollectionStage, then AsyncApiCallTimeoutTrackingStage.
                measureCall = MetricUtils.collectsMetrics(executionContext.metricCollector());
                callStart = measureCall ? System.nanoTime() : 0L;
                startApiCallTimeout();

                SdkHttpFullRequest prepared = mutationStages.prepare(marshalled, context);
                retries = RetryDecisions.create(prepared, context, dependencies, ClientType.ASYNC);
                originalRequestBody = context.requestProvider();
                Duration initialDelay = retries.acquireInitialToken();
                if (initialDelay.isZero()) {
                    attempt();
                } else {
                    schedule(this::attempt, initialDelay);
                }
            } catch (Throwable t) {
                fail(t);
            }
            return returned;
        }

        /**
         * Same as {@code AsyncApiCallTimeoutTrackingStage}: the timer fails a signal future, created only when a timeout
         * is configured.
         */
        private void startApiCallTimeout() {
            long timeoutInMillis = resolveTimeoutInMillis(context.requestConfig()::apiCallTimeout, apiCallTimeout);
            if (timeoutInMillis <= 0) {
                context.apiCallTimeoutTracker(NoOpTimeoutTracker.INSTANCE);
                return;
            }
            CompletableFuture<Void> timeoutSignal = new CompletableFuture<>();
            timeoutSignal.whenComplete((ignored, timeout) -> timedOut(timeout));
            TimeoutTracker tracker = timeAsyncTaskIfNeeded(timeoutSignal, scheduledExecutor,
                                                           () -> ApiCallTimeoutException.create(timeoutInMillis),
                                                           timeoutInMillis);
            apiCallTimeoutTracker = tracker;
            context.apiCallTimeoutTracker(tracker);
        }

        /**
         * One attempt, up to signing: {@code AsyncRetryableStage.attemptExecute}, the attempt metric collector of
         * {@code AsyncApiCallAttemptMetricCollectionStage} and {@code AsyncSigningStage}. As in the legacy retry stage,
         * an SDK or I/O exception thrown here goes to the retry decision and any other ends the call.
         */
        private void attempt() {
            if (finished.get()) {
                return;
            }
            try {
                SdkHttpFullRequest request = retries.startAttempt();
                startAttemptMetrics();
                CompletableFuture<SdkHttpFullRequest> signed;
                try {
                    signed = signing.signAsync(request, context);
                } catch (SdkException | IOException e) {
                    attemptFailed(e);
                    return;
                }
                if (signed.isDone() && !signed.isCompletedExceptionally()) {
                    // Signed inline (racecar C): no future is chained.
                    send(signed.getNow(null));
                    return;
                }
                track(signed);
                signed.whenComplete((signedRequest, failure) -> {
                    if (failure != null) {
                        attemptFailedOrFail(failure);
                    } else {
                        send(signedRequest);
                    }
                });
            } catch (Throwable t) {
                fail(t);
            }
        }

        private void startAttemptMetrics() {
            MetricCollector attemptCollector = MetricUtils.createAttemptMetricsCollector(context);
            context.attemptMetricCollector(attemptCollector);
            if (MetricUtils.collectsMetrics(attemptCollector)) {
                Duration lastBackoffDelay = attributes.getAttribute(RetryableStageHelper.LAST_BACKOFF_DELAY_DURATION);
                if (lastBackoffDelay != null) {
                    attemptCollector.reportMetric(CoreMetric.BACKOFF_DELAY_DURATION, lastBackoffDelay);
                }
            }
            attributes.putAttribute(SdkInternalExecutionAttribute.RESPONSE_BYTES_READ, new AtomicLong(0));
            attributes.putAttribute(InternalCoreExecutionAttribute.REQUEST_BODY_METRICS, new RequestBodyMetrics());
        }

        /**
         * {@code AsyncBeforeTransmissionExecutionInterceptorsStage} and {@code MakeAsyncHttpRequestStage}: the request
         * body (the call's asynchronous body, if any), the {@code Content-Length}, the HTTP metric collector, the attempt
         * timeout on the transport's future, and the response handled on the future completion executor.
         */
        private void send(SdkHttpFullRequest signed) {
            if (finished.get()) {
                return;
            }
            SdkHttpFullRequest request;
            CompletableFuture<SdkHttpFullResponse> sent;
            long sendStart;
            try {
                executionContext.interceptorChain().beforeTransmission(executionContext.interceptorContext(), attributes);
                AsyncRequestBody asyncBody = context.requestProvider();
                attributes.putAttribute(TransportExecutionAttribute.ASYNC_REQUEST_BODY, asyncBody);
                attributes.putAttribute(TransportExecutionAttribute.HTTP_METRIC_COLLECTOR,
                                        MetricUtils.createHttpMetricsCollector(context));
                request = withContentLength(signed, asyncBody);
                sendStart = System.nanoTime();
                attributes.putAttribute(SdkInternalExecutionAttribute.API_CALL_ATTEMPT_START_NANO_TIME, sendStart);
                sent = transport.send(attributes, request, null);
            } catch (Throwable t) {
                attemptFailedOrFail(t);
                return;
            }
            track(sent);
            long timeoutInMillis = resolveTimeoutInMillis(context.requestConfig()::apiCallAttemptTimeout,
                                                          apiCallAttemptTimeout);
            context.apiCallAttemptTimeoutTracker(
                timeAsyncTaskIfNeeded(sent, scheduledExecutor, () -> ApiCallAttemptTimeoutException.create(timeoutInMillis),
                                      timeoutInMillis));
            sent.whenComplete((response, failure) -> {
                long completed = System.nanoTime();
                onFutureCompletionExecutor(() -> received(request, sendStart, completed, response, failure));
            });
        }

        private void received(SdkHttpFullRequest request,
                              long sendStart,
                              long completed,
                              SdkHttpFullResponse response,
                              Throwable failure) {
            if (finished.get()) {
                closeQuietly(response);
                return;
            }
            try {
                reportTransportTimes(sendStart, completed, failure == null);
                if (failure != null) {
                    attemptFailedOrFail(failure);
                    return;
                }
                handleResponse(request, response);
            } catch (Throwable t) {
                attemptFailedOrFail(t);
            }
        }

        /**
         * {@code SERVICE_CALL_DURATION} from the attempt start to the transport's completion and
         * {@code TIME_TO_FIRST_BYTE} to the headers, as {@code MakeAsyncHttpRequestStage} reports them. The headers time is
         * the transport's, or the completion time for a transport that does not record it.
         */
        private void reportTransportTimes(long sendStart, long completed, boolean responseReceived) {
            Long headersRead = attributes.getAttribute(SdkInternalExecutionAttribute.HEADERS_READ_END_NANO_TIME);
            if (headersRead == null || headersRead < sendStart) {
                headersRead = responseReceived ? Long.valueOf(completed) : null;
                if (headersRead != null) {
                    attributes.putAttribute(SdkInternalExecutionAttribute.HEADERS_READ_END_NANO_TIME, headersRead);
                }
            }
            MetricCollector attemptCollector = context.attemptMetricCollector();
            if (!MetricUtils.collectsMetrics(attemptCollector)) {
                return;
            }
            long attemptStart = attributes.getAttribute(SdkInternalExecutionAttribute.API_CALL_ATTEMPT_START_NANO_TIME);
            attemptCollector.reportMetric(CoreMetric.SERVICE_CALL_DURATION, Duration.ofNanos(completed - attemptStart));
            if (headersRead != null) {
                attemptCollector.reportMetric(CoreMetric.TIME_TO_FIRST_BYTE, Duration.ofNanos(headersRead - attemptStart));
            }
        }

        /**
         * Same as {@code AsyncAfterTransmissionInterceptorCallingResponseHandler}: {@code afterTransmission},
         * {@code modifyHttpResponse} and {@code beforeUnmarshalling} with the response headers, then
         * {@code modifyAsyncHttpResponseContent} with the body as a publisher. The transport has already buffered the
         * body, so the publisher replays it; only a publisher an interceptor substituted is read into a new buffer.
         */
        private void handleResponse(SdkHttpFullRequest request, SdkHttpFullResponse response) {
            ExecutionInterceptorChain chain = executionContext.interceptorChain();
            SdkHttpFullResponse headersOnly = withoutContent(response);
            InterceptorContext interceptorContext =
                executionContext.interceptorContext().copy(b -> b.httpResponse(headersOnly));
            chain.afterTransmission(interceptorContext, attributes);
            interceptorContext = chain.modifyHttpResponse(interceptorContext, attributes);
            chain.beforeUnmarshalling(interceptorContext, attributes);
            executionContext.interceptorContext(interceptorContext);

            BufferedBody body = new BufferedBody(response, attributes);
            Publisher<ByteBuffer> publisher =
                chain.modifyAsyncHttpResponse(interceptorContext.toBuilder().responsePublisher(body).build(), attributes)
                     .responsePublisher()
                     .orElse(body);
            SdkHttpResponse modified = interceptorContext.httpResponse();
            if (publisher == body) {
                deserialize(request, withContent(modified, headersOnly, response, body.content()));
                return;
            }

            ResponseBodyAggregator aggregator = new ResponseBodyAggregator(-1);
            CompletableFuture<OwnedResponseBody> aggregated = aggregator.result();
            track(aggregated);
            aggregated.whenComplete((owned, failure) -> {
                if (failure != null) {
                    aggregator.cancel();
                    attemptFailedOrFail(failure);
                    return;
                }
                try {
                    owned.attach(attributes);
                    deserialize(request, withContent(modified, headersOnly, response, owned.content()));
                } catch (Throwable t) {
                    attemptFailedOrFail(t);
                }
            });
            publisher.subscribe(aggregator);
        }

        /**
         * {@link ProtocolResponseHandling}, the response metrics of {@code AsyncApiCallAttemptMetricCollectionStage}, and
         * the retry decision of {@code AsyncRetryableStage} for the response.
         */
        @SuppressWarnings({"unchecked", "rawtypes"})
        private void deserialize(SdkHttpFullRequest request, SdkHttpFullResponse response) {
            if (finished.get()) {
                closeQuietly(response);
                return;
            }
            Response<R> result = (Response<R>) ProtocolResponseHandling.handle(executionContext, (ApiOperation) operation,
                                                                               errorRegistry, request, response);
            MetricCollector attemptCollector = context.attemptMetricCollector();
            if (MetricUtils.collectsMetrics(attemptCollector)) {
                MetricUtils.collectHttpMetrics(attemptCollector, result.httpResponse());
                collectResponseMetrics(attemptCollector);
                if (!Boolean.TRUE.equals(result.isSuccess()) && result.exception() != null) {
                    reportErrorType(attemptCollector, result.exception());
                }
            }
            retries.responseReceived(result);
            if (Boolean.TRUE.equals(result.isSuccess())) {
                retries.attemptSucceeded();
                succeed(result.response());
                return;
            }
            decide(retries.attemptFailed(result.exception(), retries.suggestedDelay(result)));
        }

        /**
         * Same as the success branch of {@code AsyncApiCallAttemptMetricCollectionStage}.
         */
        private void collectResponseMetrics(MetricCollector attemptCollector) {
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
         * A failed attempt future, as {@code AsyncRetryableStage} handles it: an exception goes to the retry decision,
         * an {@link Error} ends the call.
         */
        private void attemptFailedOrFail(Throwable t) {
            if (finished.get()) {
                return;
            }
            Throwable failure = unwrap(t);
            if (failure instanceof Error) {
                fail(failure);
                return;
            }
            attemptFailed(failure);
        }

        private void attemptFailed(Throwable failure) {
            try {
                MetricCollector attemptCollector = context.attemptMetricCollector();
                if (MetricUtils.collectsMetrics(attemptCollector)) {
                    reportErrorType(attemptCollector, failure);
                }
                decide(retries.attemptFailed(failure, retries.suggestedDelay(null)));
            } catch (Throwable t) {
                fail(t);
            }
        }

        /**
         * Same as {@code AsyncRetryableStage.maybeAttemptExecute}: a retry waits on the scheduled executor, with the
         * call's original asynchronous body restored; a final failure waits the retry strategy's delay, if any.
         */
        private void decide(RetryDecisions.Decision decision) {
            if (decision.retry()) {
                context.requestProvider(originalRequestBody);
                schedule(this::attempt, decision.delay());
                return;
            }
            SdkException exception = decision.exception();
            if (decision.delay().isZero()) {
                fail(exception);
            } else {
                schedule(() -> fail(exception), decision.delay());
            }
        }

        private void schedule(Runnable task, Duration delay) {
            try {
                ScheduledFuture<?> scheduledTask = scheduledExecutor.schedule(() -> {
                    try {
                        task.run();
                    } catch (Throwable t) {
                        fail(t);
                    }
                }, delay.toMillis(), MILLISECONDS);
                scheduled = scheduledTask;
                if (finished.get()) {
                    scheduledTask.cancel(false);
                }
            } catch (Throwable t) {
                fail(t);
            }
        }

        /**
         * Makes a future the one that ending the call fails, which cancels it; fails it at once if the call has already
         * ended.
         */
        private void track(CompletableFuture<?> future) {
            inFlight = future;
            if (finished.get()) {
                future.completeExceptionally(new CancellationException("The API call has ended"));
            }
        }

        /**
         * {@code AfterExecutionInterceptorsStage}, then the successful end of the call.
         */
        private void succeed(R output) {
            if (!finished.compareAndSet(false, true)) {
                return;
            }
            try {
                executionContext.interceptorChain().afterExecution(executionContext.interceptorContext(), attributes);
            } catch (Throwable t) {
                end(null, t, true);
                return;
            }
            end(output, null, true);
        }

        /**
         * Ends the call with a failure reported to the interceptors.
         */
        private void fail(Throwable failure) {
            if (finished.compareAndSet(false, true)) {
                end(null, failure, true);
            }
        }

        /**
         * Ends the call when the API call timeout fires. As on the legacy pipeline, where the timeout completes the call
         * around the failure reporting stage, the interceptors are not called: an attempt step may be running on another
         * thread.
         */
        private void timedOut(Throwable timeout) {
            if (timeout != null && finished.compareAndSet(false, true)) {
                end(null, timeout, false);
            }
        }

        /**
         * The returned future was completed by its caller (for example cancelled): stops the call. As on the legacy
         * pipeline, the API call metrics are reported, but not {@code API_CALL_SUCCESSFUL}, and no interceptor runs.
         */
        private void cancelled(Throwable cause) {
            if (finished.compareAndSet(false, true)) {
                stop(cause);
                reportCallMetrics();
            }
        }

        /**
         * Same as the completion of {@code AsyncExecutionFailureExceptionReportingStage},
         * {@code AsyncApiCallMetricCollectionStage}, the handler's exception translation and
         * {@code measureApiCallSuccess}, in that order.
         */
        private void end(R output, Throwable failure, boolean reportFailure) {
            stop(failure);
            if (failure == null) {
                reportCallMetrics();
                callerCollector.reportMetric(CoreMetric.API_CALL_SUCCESSFUL, true);
                returned.complete(output);
                return;
            }
            Throwable reported = unwrap(failure);
            if (reportFailure) {
                reported = ExceptionReportingUtils.reportFailureToInterceptors(context, reported);
            }
            SdkException exception = ThrowableUtils.asSdkException(reported);
            reportCallMetrics();
            callerCollector.reportMetric(CoreMetric.API_CALL_SUCCESSFUL, false);
            returned.completeExceptionally(CompletableFutureUtils.errorAsCompletionException(exception));
        }

        private void stop(Throwable cause) {
            apiCallTimeoutTracker.cancel();
            ScheduledFuture<?> scheduledTask = scheduled;
            if (scheduledTask != null) {
                scheduledTask.cancel(false);
            }
            CompletableFuture<?> current = inFlight;
            if (cause != null && current != null) {
                current.completeExceptionally(cause);
            }
        }

        private void reportCallMetrics() {
            if (!measureCall) {
                return;
            }
            MetricCollector metricCollector = executionContext.metricCollector();
            metricCollector.reportMetric(CoreMetric.API_CALL_DURATION, Duration.ofNanos(System.nanoTime() - callStart));
            SdkHttpRequest finalRequest = executionContext.interceptorContext().httpRequest();
            MetricUtils.collectServiceEndpointMetrics(metricCollector,
                                                      finalRequest != null ? (SdkHttpFullRequest) finalRequest : marshalled);
        }
    }

    /**
     * The buffered response body as the publisher {@code modifyAsyncHttpResponseContent} receives: each subscriber gets
     * the body as one buffer. The SDK transport's buffer is published without copying and without reading the response
     * stream; another transport's body is read from its in-memory stream once, and then deserialized from that copy.
     */
    private static final class BufferedBody implements Publisher<ByteBuffer> {
        private final SdkHttpFullResponse response;
        private final ExecutionAttributes attributes;
        private byte[] copied;

        private BufferedBody(SdkHttpFullResponse response, ExecutionAttributes attributes) {
            this.response = response;
            this.attributes = attributes;
        }

        @Override
        public void subscribe(Subscriber<? super ByteBuffer> subscriber) {
            Validate.paramNotNull(subscriber, "subscriber");
            ByteBuffer body;
            try {
                body = body();
            } catch (IOException | RuntimeException e) {
                SimplePublisher<ByteBuffer> failed = new SimplePublisher<>();
                failed.error(e);
                failed.subscribe(subscriber);
                return;
            }
            List<ByteBuffer> chunks = body.hasRemaining() ? Collections.singletonList(body) : Collections.emptyList();
            SdkPublisher.fromIterable(chunks).subscribe(subscriber);
        }

        private synchronized ByteBuffer body() throws IOException {
            if (copied == null) {
                ByteBuffer owned = OwnedResponseBody.ownedBuffer(attributes, response);
                if (owned != null) {
                    return owned;
                }
                Optional<AbortableInputStream> content = response.content();
                copied = content.isPresent() ? IoUtils.toByteArray(content.get()) : new byte[0];
            }
            return ByteBuffer.wrap(copied);
        }

        /**
         * The body to deserialize when no interceptor replaced the publisher.
         */
        private synchronized AbortableInputStream content() {
            if (copied == null) {
                return response.content().orElse(null);
            }
            return AbortableInputStream.create(new ByteArrayInputStream(copied));
        }
    }
}
