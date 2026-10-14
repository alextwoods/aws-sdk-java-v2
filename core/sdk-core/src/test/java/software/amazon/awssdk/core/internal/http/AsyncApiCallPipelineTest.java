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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.fail;
import static software.amazon.awssdk.core.internal.http.ApiCallStagesTestSupport.CLIENT_ENDPOINT;
import static software.amazon.awssdk.core.internal.http.ApiCallStagesTestSupport.OPERATION_PATH;
import static software.amazon.awssdk.core.internal.http.ApiCallStagesTestSupport.UUID_PATTERN;
import static software.amazon.awssdk.core.internal.http.ApiCallStagesTestSupport.baseConfig;
import static software.amazon.awssdk.core.internal.http.ApiCallStagesTestSupport.dependencies;
import static software.amazon.awssdk.core.internal.http.ApiCallStagesTestSupport.marshalledRequest;
import static software.amazon.awssdk.core.internal.http.ProtocolStepsTestSupport.OPERATION;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.function.Supplier;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.reactivestreams.Publisher;
import software.amazon.awssdk.core.ResponseBytes;
import software.amazon.awssdk.core.SdkRequest;
import software.amazon.awssdk.core.SdkResponse;
import software.amazon.awssdk.core.SelectedAuthScheme;
import software.amazon.awssdk.core.async.AsyncRequestBody;
import software.amazon.awssdk.core.async.AsyncResponseTransformer;
import software.amazon.awssdk.core.async.SdkPublisher;
import software.amazon.awssdk.core.client.config.SdkAdvancedAsyncClientOption;
import software.amazon.awssdk.core.client.config.SdkClientConfiguration;
import software.amazon.awssdk.core.client.config.SdkClientOption;
import software.amazon.awssdk.core.client.handler.ClientExecutionParams;
import software.amazon.awssdk.core.client.protocol.AsyncClientTransport;
import software.amazon.awssdk.core.client.protocol.AsyncResponseHandoff;
import software.amazon.awssdk.core.client.protocol.HttpMessageExchange;
import software.amazon.awssdk.core.client.protocol.MessageExchange;
import software.amazon.awssdk.core.exception.ApiCallAttemptTimeoutException;
import software.amazon.awssdk.core.exception.ApiCallTimeoutException;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.core.exception.SdkServiceException;
import software.amazon.awssdk.core.http.ExecutionContext;
import software.amazon.awssdk.core.interceptor.Context;
import software.amazon.awssdk.core.interceptor.ExecutionAttributes;
import software.amazon.awssdk.core.interceptor.ExecutionInterceptor;
import software.amazon.awssdk.core.interceptor.ExecutionInterceptorChain;
import software.amazon.awssdk.core.interceptor.InterceptorContext;
import software.amazon.awssdk.core.interceptor.SdkExecutionAttribute;
import software.amazon.awssdk.core.interceptor.SdkInternalExecutionAttribute;
import software.amazon.awssdk.core.internal.InternalCoreExecutionAttribute;
import software.amazon.awssdk.core.internal.http.ApiCallStagesTestSupport.HttpStubProtocol;
import software.amazon.awssdk.core.internal.http.ProtocolStepsTestSupport.TestRequest;
import software.amazon.awssdk.core.internal.http.ProtocolStepsTestSupport.TestResponse;
import software.amazon.awssdk.core.internal.http.transport.SdkAsyncHttpClientTransport;
import software.amazon.awssdk.core.internal.http.transport.TransportExecutionAttribute;
import software.amazon.awssdk.core.internal.retry.SdkDefaultRetryStrategy;
import software.amazon.awssdk.core.internal.util.MetricUtils;
import software.amazon.awssdk.core.metrics.CoreMetric;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.http.AbortableInputStream;
import software.amazon.awssdk.http.ContentStreamProvider;
import software.amazon.awssdk.http.HttpMetric;
import software.amazon.awssdk.http.SdkHttpFullRequest;
import software.amazon.awssdk.http.SdkHttpFullResponse;
import software.amazon.awssdk.http.SdkHttpRequest;
import software.amazon.awssdk.http.SdkHttpResponse;
import software.amazon.awssdk.http.async.AsyncExecuteRequest;
import software.amazon.awssdk.http.async.SdkAsyncHttpClient;
import software.amazon.awssdk.http.auth.spi.scheme.AuthSchemeOption;
import software.amazon.awssdk.http.auth.spi.signer.AsyncSignRequest;
import software.amazon.awssdk.http.auth.spi.signer.AsyncSignedRequest;
import software.amazon.awssdk.http.auth.spi.signer.HttpSigner;
import software.amazon.awssdk.http.auth.spi.signer.SignRequest;
import software.amazon.awssdk.http.auth.spi.signer.SignedRequest;
import software.amazon.awssdk.identity.spi.AwsCredentialsIdentity;
import software.amazon.awssdk.metrics.MetricCollection;
import software.amazon.awssdk.metrics.MetricCollector;
import software.amazon.awssdk.metrics.NoOpMetricCollector;
import software.amazon.awssdk.retries.api.BackoffStrategy;
import software.amazon.awssdk.schema.ApiOperation;
import software.amazon.awssdk.schema.SerializableStruct;
import software.amazon.awssdk.schema.serde.TypeRegistry;
import software.amazon.awssdk.utils.IoUtils;

/**
 * Tests for {@link AsyncApiCallPipeline} with a stub operation, protocol and transport: outputs, hook order, retries,
 * failure reporting and exception wrapping, timeouts, cancellation, signing, metrics, the default transport and the
 * response body hooks. The full comparison of hook sequences with the legacy pipeline is
 * {@code ApiCallPipelineInterceptorOrderTest} (task 3.29).
 */
class AsyncApiCallPipelineTest {
    private static final long TIMEOUT_SECONDS = 10;
    private static final AwsCredentialsIdentity IDENTITY = AwsCredentialsIdentity.create("akid", "secret");
    private static final List<String> SUCCESS_HOOKS = Arrays.asList(
        "beforeExecution", "modifyRequest", "beforeMarshalling", "afterMarshalling", "modifyAsyncHttpContent",
        "modifyHttpContent", "modifyHttpRequest", "beforeTransmission", "afterTransmission", "modifyHttpResponse",
        "modifyHttpResponseContent", "beforeUnmarshalling", "modifyAsyncHttpResponseContent", "afterUnmarshalling",
        "modifyResponse", "afterExecution");
    private static final List<String> ATTEMPT_HOOKS = Arrays.asList(
        "beforeTransmission", "afterTransmission", "modifyHttpResponse", "modifyHttpResponseContent",
        "beforeUnmarshalling", "modifyAsyncHttpResponseContent");

    private final TestProtocol protocol = new TestProtocol();
    private final ScriptedTransport transport = new ScriptedTransport();
    private final RecordingInterceptor interceptor = new RecordingInterceptor();
    private final TestSigner signer = new TestSigner();
    private ScheduledThreadPoolExecutor scheduledExecutor;
    private ExecutorService completionExecutor;
    private CompletableFuture<AwsCredentialsIdentity> identity;

    @BeforeEach
    void setUp() {
        scheduledExecutor = new ScheduledThreadPoolExecutor(2);
        scheduledExecutor.setRemoveOnCancelPolicy(true);
    }

    @AfterEach
    void tearDown() {
        scheduledExecutor.shutdownNow();
        if (completionExecutor != null) {
            completionExecutor.shutdownNow();
        }
    }

    @Test
    void success_completesWithTheOutputAndRunsEveryHookOnceInTheLegacyOrder() throws Exception {
        transport.respond(200, "hello");

        TestResponse output = join(pipeline(config()).execute(OPERATION, params(), this::executionContext));

        assertThat(output.value()).isEqualTo("hello");
        assertThat(output.sdkHttpResponse().statusCode()).isEqualTo(200);
        assertThat(interceptor.hooks).isEqualTo(SUCCESS_HOOKS);
        assertThat(interceptor.attributes).as("every hook gets the call's attributes").hasSize(SUCCESS_HOOKS.size());
        assertThat(new HashSet<>(identities(interceptor.attributes))).hasSize(1);

        SdkHttpFullRequest sent = transport.requests.get(0);
        assertThat(sent.getUri().toString()).isEqualTo(CLIENT_ENDPOINT + OPERATION_PATH);
        assertThat(sent.firstMatchingHeader("amz-sdk-request")).hasValue("attempt=1; max=3");
        assertThat(sent.firstMatchingHeader("amz-sdk-invocation-id")).hasValueSatisfying(id -> assertThat(id)
            .matches(UUID_PATTERN));
        assertThat(ApiCallStagesTestSupport.body(sent)).isEqualTo("{}");
    }

    @Test
    void success_hooksSeeTheLegacyAsyncContextsAndAttributes() throws Exception {
        transport.respond(200, "hello");
        interceptor.readAsyncResponseBody = true;

        join(pipeline(config()).execute(OPERATION, params(), this::executionContext));

        ExecutionAttributes attributes = interceptor.attributes.get(0);
        assertThat(attributes.getAttribute(SdkExecutionAttribute.API_OPERATION)).isSameAs(OPERATION);
        assertThat(attributes.getAttribute(SdkExecutionAttribute.CLIENT_PROTOCOL)).isSameAs(protocol);
        assertThat(attributes.getAttribute(SdkExecutionAttribute.ERROR_REGISTRY)).isSameAs(OPERATION.errorRegistry());
        assertThat(interceptor.beforeTransmission.httpRequest().firstMatchingHeader("amz-sdk-request"))
            .hasValue("attempt=1; max=3");
        assertThat(interceptor.attemptAtBeforeTransmission).isEqualTo(1);
        assertThat(interceptor.attemptCollectorAtBeforeTransmission).isNotNull();
        // As in AsyncAfterTransmissionInterceptorCallingResponseHandler: headers first, the body as a publisher later.
        assertThat(interceptor.afterTransmission.httpResponse().statusCode()).isEqualTo(200);
        assertThat(interceptor.afterTransmission.responseBody()).isEmpty();
        assertThat(((SdkHttpFullResponse) interceptor.afterTransmission.httpResponse()).content()).isEmpty();
        assertThat(interceptor.asyncResponseBody).isEqualTo("hello");
        assertThat(interceptor.afterExecution.response()).isInstanceOf(TestResponse.class);
        assertThat(interceptor.afterExecution.httpResponse().statusCode()).isEqualTo(200);
    }

    @Test
    void serviceErrorThenSuccess_retriesAndRunsTheAttemptHooksPerAttempt() throws Exception {
        transport.respond(500, "busy");
        transport.respond(200, "hello");

        TestResponse output = join(pipeline(config()).execute(OPERATION, params(), this::executionContext));

        assertThat(output.value()).isEqualTo("hello");
        assertThat(transport.requests).hasSize(2);
        assertThat(transport.requests.get(1).firstMatchingHeader("amz-sdk-request")).hasValue("attempt=2; max=3");
        List<String> expected = new ArrayList<>(SUCCESS_HOOKS.subList(0, 7));
        expected.addAll(ATTEMPT_HOOKS);
        expected.addAll(SUCCESS_HOOKS.subList(7, SUCCESS_HOOKS.size()));
        assertThat(interceptor.hooks).isEqualTo(expected);
    }

    @Test
    void nonRetryableServiceError_reportsTheFailureOnceAndFailsWithACompletionExceptionAsTheLegacyHandler() {
        transport.respond(400, "bad");

        CompletableFuture<TestResponse> future = pipeline(config()).execute(OPERATION, params(), this::executionContext);

        Throwable cause = failure(future);
        assertThat(cause).isInstanceOf(SdkServiceException.class);
        assertThat(((SdkServiceException) cause).statusCode()).isEqualTo(400);
        // Callbacks see the CompletionException, as on the legacy handler's future.
        assertThat(future.handle((r, t) -> t).join()).isInstanceOf(CompletionException.class).hasCause(cause);
        assertThatThrownBy(future::join).isInstanceOf(CompletionException.class).hasCause(cause);

        assertThat(transport.requests).hasSize(1);
        List<String> expected = new ArrayList<>(SUCCESS_HOOKS.subList(0, 13));
        expected.addAll(Arrays.asList("modifyException", "onExecutionFailure"));
        assertThat(interceptor.hooks).isEqualTo(expected);
        assertThat(interceptor.failure).isSameAs(cause);
    }

    @Test
    void clientError_isNotRetriedAndKeepsItsTypeMessageAndCause() {
        IllegalArgumentException cause = new IllegalArgumentException("root");
        transport.fail(() -> SdkClientException.create("client failure", cause));

        Throwable failure = failure(pipeline(config()).execute(OPERATION, params(), this::executionContext));

        assertThat(failure).isExactlyInstanceOf(SdkClientException.class)
                           .hasMessageContaining("client failure")
                           .hasCause(cause);
        assertThat(transport.requests).hasSize(1);
        assertThat(interceptor.count("onExecutionFailure")).isEqualTo(1);
        assertThat(interceptor.hooks).doesNotContain("afterTransmission", "afterExecution");
    }

    @Test
    void nonSdkExceptionFromTheTransport_goesToTheRetryDecisionAsInTheLegacyAsyncRetryStage() {
        IllegalStateException bug = new IllegalStateException("transport bug");
        transport.fail(() -> bug);

        Throwable failure = failure(pipeline(config()).execute(OPERATION, params(), this::executionContext));

        assertThat(failure).isExactlyInstanceOf(SdkClientException.class)
                           .hasMessageContaining("Unable to execute HTTP request: transport bug")
                           .hasCause(bug);
        assertThat(transport.requests).hasSize(1);
        assertThat(interceptor.count("onExecutionFailure")).isEqualTo(1);
    }

    @Test
    void serializationFailure_isReportedOnceAndNothingIsSent() {
        SdkClientException failure = SdkClientException.create("Unable to marshall GetThing: boom");
        protocol.serialize = endpoint -> {
            throw failure;
        };

        assertThat(failure(pipeline(config()).execute(OPERATION, params(), this::executionContext))).isSameAs(failure);

        assertThat(transport.requests).isEmpty();
        assertThat(interceptor.hooks).containsExactly("beforeExecution", "modifyRequest", "beforeMarshalling",
                                                      "modifyException", "onExecutionFailure");
    }

    @Test
    void contextFactoryFailure_failsWithTheFactoryExceptionWithoutReportingIt() {
        RuntimeException failure = new RuntimeException("factory failed");
        MetricCollector collector = MetricCollector.create("ApiCall");

        CompletableFuture<TestResponse> future = pipeline(config()).execute(OPERATION, params().withMetricCollector(collector),
                                                                            p -> {
                                                                                throw failure;
                                                                            });

        assertThat(failure(future)).isSameAs(failure);
        assertThat(interceptor.hooks).isEmpty();
        assertThat(collector.collect().metricValues(CoreMetric.API_CALL_SUCCESSFUL)).containsExactly(false);
    }

    @Test
    void retriesExhausted_reportsTheFailureOnceWithTheLegacyExceptionTypeAndCause() {
        IOException cause = new IOException("connection reset");
        for (int i = 0; i < 3; i++) {
            transport.fail(() -> SdkClientException.create("Unable to execute HTTP request: connection reset", cause));
        }

        Throwable failure = failure(pipeline(config()).execute(OPERATION, params(), this::executionContext));

        assertThat(failure).isExactlyInstanceOf(SdkClientException.class).hasCause(cause);
        assertThat(failure.getSuppressed()).hasSize(2);
        assertThat(transport.requests).hasSize(3);
        assertThat(interceptor.count("beforeTransmission")).isEqualTo(3);
        assertThat(interceptor.count("modifyException")).isEqualTo(1);
        assertThat(interceptor.count("onExecutionFailure")).isEqualTo(1);
        assertThat(interceptor.hooks.get(interceptor.hooks.size() - 1)).isEqualTo("onExecutionFailure");
        assertThat(interceptor.failure).isInstanceOf(SdkClientException.class).hasCause(cause);
    }

    @Test
    void serviceErrorsExhausted_failsWithTheLastServiceException() {
        for (int i = 0; i < 3; i++) {
            transport.respond(503, "busy");
        }

        Throwable failure = failure(pipeline(config()).execute(OPERATION, params(), this::executionContext));

        assertThat(failure).isInstanceOf(SdkServiceException.class);
        assertThat(((SdkServiceException) failure).statusCode()).isEqualTo(503);
        assertThat(transport.requests).hasSize(3);
        assertThat(interceptor.count("onExecutionFailure")).isEqualTo(1);
    }

    @Test
    void afterExecutionFailure_isReportedOnceAndWrappedAsAnSdkException() {
        transport.respond(200, "hello");
        RuntimeException failure = new RuntimeException("Uh oh!");
        interceptor.afterExecutionFailure = failure;

        Throwable thrown = failure(pipeline(config()).execute(OPERATION, params(), this::executionContext));

        assertThat(thrown).isExactlyInstanceOf(SdkClientException.class).hasCause(failure);
        assertThat(interceptor.failure).isSameAs(failure);
        assertThat(interceptor.hooks.subList(interceptor.hooks.size() - 3, interceptor.hooks.size()))
            .containsExactly("afterExecution", "modifyException", "onExecutionFailure");
    }

    @Test
    void attemptTimeout_cancelsEachAttemptAndFailsWithApiCallAttemptTimeoutException() {
        PendingHttpClient httpClient = new PendingHttpClient();
        SdkClientConfiguration config = httpClientConfig(httpClient)
            .option(SdkClientOption.API_CALL_ATTEMPT_TIMEOUT, Duration.ofMillis(100))
            .build();

        Throwable failure = failure(pipeline(config).execute(OPERATION, params(), this::executionContext));

        assertThat(failure).isInstanceOf(ApiCallAttemptTimeoutException.class);
        assertThat(httpClient.executions).hasSize(3);
        assertThat(httpClient.executions).allSatisfy(execution -> assertThat(execution).isCompletedExceptionally());
        assertThat(interceptor.count("onExecutionFailure")).isEqualTo(1);
    }

    @Test
    void apiCallTimeout_cancelsTheAttemptAndFailsWithApiCallTimeoutExceptionAsTheLegacyAsyncStage() {
        PendingHttpClient httpClient = new PendingHttpClient();
        SdkClientConfiguration config = httpClientConfig(httpClient)
            .option(SdkClientOption.API_CALL_TIMEOUT, Duration.ofMillis(200))
            .build();
        MetricCollector collector = MetricCollector.create("ApiCall");

        Throwable failure = failure(pipeline(config).execute(OPERATION, params().withMetricCollector(collector),
                                                             this::executionContext));

        assertThat(failure).isInstanceOf(ApiCallTimeoutException.class);
        assertThat(httpClient.executions).hasSize(1);
        assertThat(httpClient.executions.get(0)).isCompletedExceptionally();
        // AsyncApiCallTimeoutTrackingStage completes the call around the failure reporting stage.
        assertThat(interceptor.hooks).doesNotContain("modifyException", "onExecutionFailure");
        MetricCollection call = collector.collect();
        assertThat(call.metricValues(CoreMetric.API_CALL_SUCCESSFUL)).containsExactly(false);
        assertThat(call.metricValues(CoreMetric.API_CALL_DURATION)).hasSize(1);
    }

    @Test
    void cancellingTheReturnedFuture_cancelsTheInFlightTransportFuture() throws Exception {
        CompletableFuture<SdkHttpFullResponse> inFlight = transport.pending();

        CompletableFuture<TestResponse> future = pipeline(config()).execute(OPERATION, params(), this::executionContext);
        assertThat(future).isNotDone();
        future.cancel(true);

        assertThat(inFlight).isCompletedExceptionally();
        assertThatThrownBy(inFlight::join).isInstanceOf(CancellationException.class);
        assertThat(transport.requests).hasSize(1);
        assertThat(interceptor.hooks).doesNotContain("afterTransmission", "modifyException", "onExecutionFailure");
    }

    @Test
    void cancellingTheReturnedFutureDuringABackoff_cancelsTheRetry() throws Exception {
        transport.respond(500, "busy");
        SdkClientConfiguration config =
            config().toBuilder()
                    .option(SdkClientOption.RETRY_STRATEGY,
                            SdkDefaultRetryStrategy.standardRetryStrategyBuilder()
                                                   .maxAttempts(3)
                                                   .backoffStrategy(BackoffStrategy.fixedDelayWithoutJitter(
                                                       Duration.ofSeconds(30)))
                                                   .build())
                    .build();

        CompletableFuture<TestResponse> future = pipeline(config).execute(OPERATION, params(), this::executionContext);
        assertThat(future).isNotDone();
        assertThat(scheduledExecutor.getQueue()).as("the retry is scheduled").hasSize(1);
        future.cancel(true);

        assertThat(scheduledExecutor.getQueue()).as("the retry is cancelled").isEmpty();
        assertThat(transport.requests).hasSize(1);
    }

    @Test
    void resolvedIdentity_signsInlineSoTheWholeCallCompletesOnTheCallingThread() {
        identity = CompletableFuture.completedFuture(IDENTITY);
        transport.respond(200, "hello");

        CompletableFuture<TestResponse> future = pipeline(config()).execute(OPERATION, params(), this::executionContext);

        assertThat(future).isCompletedWithValueMatching(output -> "hello".equals(output.value()));
        assertThat(signer.syncSignThreads).containsExactly(Thread.currentThread());
        assertThat(transport.requests.get(0).firstMatchingHeader("Authorization")).hasValue("signed-by-akid");
        assertThat(interceptor.beforeTransmission.httpRequest().firstMatchingHeader("Authorization"))
            .hasValue("signed-by-akid");
    }

    @Test
    void asyncRequestBody_isSignedAsynchronouslyAndSentThroughTheTransport() throws Exception {
        identity = CompletableFuture.completedFuture(IDENTITY);
        AsyncRequestBody body = AsyncRequestBody.fromString("streamed");
        interceptor.asyncRequestBody = body;
        transport.respond(200, "hello");

        CompletableFuture<TestResponse> future = pipeline(config()).execute(OPERATION, params(), this::executionContext);

        assertThat(future).isNotDone();
        assertThat(transport.requests).as("sent only once signed").isEmpty();
        AsyncSignRequest<? extends AwsCredentialsIdentity> signRequest = signer.asyncSignRequests.get(0);
        assertThat(signRequest.payload()).hasValue(body);
        signer.asyncSignature.complete(AsyncSignedRequest.builder()
                                                         .request(signRequest.request().toBuilder()
                                                                             .putHeader("Authorization", "async-signed")
                                                                             .build())
                                                         .payload(signRequest.payload().orElse(null))
                                                         .build());

        assertThat(join(future).value()).isEqualTo("hello");
        assertThat(transport.requests.get(0).firstMatchingHeader("Authorization")).hasValue("async-signed");
        assertThat(transport.attributes.get(0).getAttribute(TransportExecutionAttribute.ASYNC_REQUEST_BODY)).isSameAs(body);
        assertThat(signer.syncSignThreads).isEmpty();
    }

    @Test
    void requestWithoutContentLength_getsTheBodyLengthAsInMakeAsyncHttpRequestStage() throws Exception {
        protocol.serialize = endpoint -> marshalledRequest(endpoint, null).toBuilder()
                                                                          .encodedPath(OPERATION_PATH)
                                                                          .contentStreamProvider(
                                                                              ContentStreamProvider.fromUtf8String("{\"a\":1}"))
                                                                          .build();
        transport.respond(200, "hello");

        join(pipeline(config()).execute(OPERATION, params(), this::executionContext));

        SdkHttpFullRequest sent = transport.requests.get(0);
        assertThat(sent.firstMatchingHeader("Content-Length")).hasValue("7");
        assertThat(sent.contentStreamProvider().get().contentAsByteBufferOrNull()).as("read once, sent from memory")
                                                                                 .isNotNull();
        assertThat(ApiCallStagesTestSupport.body(sent)).isEqualTo("{\"a\":1}");
    }

    @Test
    void futureCompletionExecutor_handlesEachResponse() throws Exception {
        completionExecutor = Executors.newSingleThreadExecutor(r -> new Thread(r, "future-completion"));
        transport.respond(200, "hello");
        SdkClientConfiguration config =
            config().toBuilder().option(SdkAdvancedAsyncClientOption.FUTURE_COMPLETION_EXECUTOR, completionExecutor).build();

        join(pipeline(config).execute(OPERATION, params(), this::executionContext));

        assertThat(interceptor.threads.get("afterTransmission")).isEqualTo("future-completion");
        assertThat(interceptor.threads.get("afterExecution")).isEqualTo("future-completion");
    }

    @Test
    void metricsEnabled_reportsTheLegacyCallAndAttemptMetrics() throws Exception {
        transport.respond(500, "busy");
        transport.respond(200, "hello");
        MetricCollector collector = MetricCollector.create("ApiCall");

        join(pipeline(config()).execute(OPERATION, params().withMetricCollector(collector), this::executionContext));

        MetricCollection call = collector.collect();
        assertThat(call.metricValues(CoreMetric.API_CALL_SUCCESSFUL)).containsExactly(true);
        assertThat(call.metricValues(CoreMetric.API_CALL_DURATION)).hasSize(1);
        assertThat(call.metricValues(CoreMetric.MARSHALLING_DURATION)).hasSize(1);
        assertThat(call.metricValues(CoreMetric.RETRY_COUNT)).containsExactly(1);
        assertThat(call.metricValues(CoreMetric.SERVICE_ENDPOINT)).containsExactly(CLIENT_ENDPOINT);

        List<MetricCollection> attempts = call.children();
        assertThat(attempts).hasSize(2);
        assertThat(attempts.get(0).metricValues(HttpMetric.HTTP_STATUS_CODE)).containsExactly(500);
        assertThat(attempts.get(0).metricValues(CoreMetric.ERROR_TYPE)).containsExactly("ServerError");
        MetricCollection success = attempts.get(1);
        assertThat(success.metricValues(HttpMetric.HTTP_STATUS_CODE)).containsExactly(200);
        assertThat(success.metricValues(CoreMetric.BACKOFF_DELAY_DURATION)).hasSize(1);
        assertThat(success.metricValues(CoreMetric.SERVICE_CALL_DURATION)).hasSize(1);
        assertThat(success.metricValues(CoreMetric.TIME_TO_FIRST_BYTE)).hasSize(1);
        assertThat(success.metricValues(CoreMetric.TIME_TO_LAST_BYTE)).hasSize(1);
        assertThat(success.metricValues(CoreMetric.READ_THROUGHPUT)).hasSize(1);
        assertThat(success.metricValues(CoreMetric.UNMARSHALLING_DURATION)).hasSize(1);
        assertThat(success.children()).as("HTTP client collector").hasSize(1);
        assertThat(transport.attributes.get(1).getAttribute(TransportExecutionAttribute.HTTP_METRIC_COLLECTOR))
            .isNotInstanceOf(NoOpMetricCollector.class);
    }

    @Test
    void metricsEnabled_transportFailureIsAnIoErrorAsOnTheLegacyAsyncPipeline() throws Exception {
        transport.fail(() -> SdkClientException.create("Unable to execute HTTP request: reset", new IOException("reset")));
        transport.respond(200, "hello");
        MetricCollector collector = MetricCollector.create("ApiCall");

        join(pipeline(config()).execute(OPERATION, params().withMetricCollector(collector), this::executionContext));

        MetricCollection failedAttempt = collector.collect().children().get(0);
        assertThat(failedAttempt.metricValues(CoreMetric.ERROR_TYPE)).containsExactly("IO");
        assertThat(failedAttempt.metricValues(CoreMetric.SERVICE_CALL_DURATION)).hasSize(1);
        assertThat(failedAttempt.metricValues(CoreMetric.TIME_TO_FIRST_BYTE)).isEmpty();
    }

    @Test
    void metricsDisabled_usesNoOpCollectorsAndDeserializesTheTransmittedBody() throws Exception {
        transport.respond(200, "hello");
        ClientExecutionParams<TestRequest, TestResponse> params = params();

        join(pipeline(config()).execute(OPERATION, params, this::executionContext));

        assertThat(params.getMetricCollector()).isInstanceOf(NoOpMetricCollector.class);
        ExecutionAttributes attributes = transport.attributes.get(0);
        assertThat(MetricUtils.collectsMetrics(attributes.getAttribute(SdkExecutionAttribute.API_CALL_ATTEMPT_METRIC_COLLECTOR)))
            .isFalse();
        assertThat(attributes.getAttribute(TransportExecutionAttribute.HTTP_METRIC_COLLECTOR))
            .isInstanceOf(NoOpMetricCollector.class);
        assertThat(protocol.deserializedBodies).containsExactly(transport.responseBodies.get(0));
        // The attributes the legacy attempt stage sets are still there, as interceptors see them on the legacy path.
        assertThat(attributes.getAttribute(InternalCoreExecutionAttribute.REQUEST_BODY_METRICS)).isNotNull();
        assertThat(attributes.getAttribute(SdkInternalExecutionAttribute.RESPONSE_BYTES_READ)).isNotNull();
    }

    @Test
    void replacedResponsePublisher_isDeserialized() throws Exception {
        transport.respond(200, "hello");
        interceptor.responsePublisher = original -> SdkPublisher.adapt(original).map(AsyncApiCallPipelineTest::upperCase);

        TestResponse output = join(pipeline(config()).execute(OPERATION, params(), this::executionContext));

        assertThat(output.value()).isEqualTo("HELLO");
    }

    @Test
    void noConfiguredTransport_sendsThroughTheAsyncHttpClientWithItsBufferedBody() throws Exception {
        RespondingHttpClient httpClient = new RespondingHttpClient();
        interceptor.readAsyncResponseBody = true;
        SdkClientConfiguration config = httpClientConfig(httpClient).build();
        AsyncApiCallPipeline pipeline = pipeline(config);

        TestResponse output = join(pipeline.execute(OPERATION, params(), this::executionContext));

        assertThat(pipeline.transport()).isInstanceOf(SdkAsyncHttpClientTransport.class);
        assertThat(output.value()).isEqualTo("from-http-client");
        assertThat(interceptor.asyncResponseBody).isEqualTo("from-http-client");
        assertThat(httpClient.requests).hasSize(1);
        assertThat(httpClient.requests.get(0).request().getUri().toString()).isEqualTo(CLIENT_ENDPOINT + OPERATION_PATH);

        pipeline.transport().close();
        assertThat(httpClient.closed).as("the configuration owns the HTTP client").isFalse();
    }

    @Test
    void noConfiguredTransport_replacedResponsePublisherReadsTheTransportBuffer() throws Exception {
        RespondingHttpClient httpClient = new RespondingHttpClient();
        interceptor.responsePublisher = original -> SdkPublisher.adapt(original).map(AsyncApiCallPipelineTest::upperCase);

        TestResponse output = join(pipeline(httpClientConfig(httpClient).build())
                                       .execute(OPERATION, params(), this::executionContext));

        assertThat(output.value()).isEqualTo("FROM-HTTP-CLIENT");
    }

    @Test
    void configuredTransport_isUsedInsteadOfTheHttpClient() throws Exception {
        RespondingHttpClient httpClient = new RespondingHttpClient();
        transport.respond(200, "from-transport");
        SdkClientConfiguration config = config().toBuilder().option(SdkClientOption.ASYNC_HTTP_CLIENT, httpClient).build();
        AsyncApiCallPipeline pipeline = pipeline(config);

        TestResponse output = join(pipeline.execute(OPERATION, params(), this::executionContext));

        assertThat(pipeline.transport()).isSameAs(transport);
        assertThat(output.value()).isEqualTo("from-transport");
        assertThat(httpClient.requests).isEmpty();
    }

    @Test
    void transportOfAnotherExchangeNoTransportOrNoScheduler_isRejected() {
        ScriptedTransport otherExchange = new ScriptedTransport() {
            @Override
            public MessageExchange<SdkHttpFullRequest, SdkHttpFullResponse> messageExchange() {
                return new MessageExchange<SdkHttpFullRequest, SdkHttpFullResponse>() {
                };
            }
        };
        SdkClientConfiguration mismatched =
            config().toBuilder().option(SdkClientOption.ASYNC_CLIENT_TRANSPORT, otherExchange).build();
        assertThatThrownBy(() -> pipeline(mismatched)).isInstanceOf(IllegalStateException.class)
                                                      .hasMessageContaining("exchanges");

        SdkClientConfiguration none = baseConfig().option(SdkClientOption.CLIENT_PROTOCOL, protocol)
                                                  .option(SdkClientOption.SCHEDULED_EXECUTOR_SERVICE, scheduledExecutor)
                                                  .build();
        assertThatThrownBy(() -> pipeline(none)).isInstanceOf(IllegalStateException.class)
                                                .hasMessageContaining("ASYNC_HTTP_CLIENT");

        SdkClientConfiguration noScheduler = baseConfig().option(SdkClientOption.CLIENT_PROTOCOL, protocol)
                                                         .option(SdkClientOption.ASYNC_CLIENT_TRANSPORT, transport)
                                                         .build();
        assertThatThrownBy(() -> pipeline(noScheduler)).isInstanceOf(IllegalStateException.class)
                                                       .hasMessageContaining("SCHEDULED_EXECUTOR_SERVICE");
    }

    @Test
    void streamingOutput_isNotSupportedYet() {
        AsyncResponseTransformer<TestResponse, ResponseBytes<TestResponse>> transformer = AsyncResponseTransformer.toBytes();

        assertThatThrownBy(() -> pipeline(config()).execute(OPERATION, params(), this::executionContext, transformer))
            .isInstanceOf(UnsupportedOperationException.class)
            .hasMessageContaining("streaming output");
        assertThat(interceptor.hooks).isEmpty();
    }

    @Test
    void errorRegistryAttribute_isPassedToTheProtocol() throws Exception {
        transport.respond(200, "hello");
        TypeRegistry serviceRegistry = TypeRegistry.empty();
        AsyncApiCallPipeline pipeline = AsyncApiCallPipeline.create(dependencies(config()), serviceRegistry);

        join(pipeline.execute(OPERATION, params(), this::executionContext));

        assertThat(protocol.errorRegistries).containsExactly(serviceRegistry);
    }

    private SdkClientConfiguration config() {
        return baseConfig().option(SdkClientOption.CLIENT_PROTOCOL, protocol)
                           .option(SdkClientOption.ASYNC_CLIENT_TRANSPORT, transport)
                           .option(SdkClientOption.SCHEDULED_EXECUTOR_SERVICE, scheduledExecutor)
                           .build();
    }

    private SdkClientConfiguration.Builder httpClientConfig(SdkAsyncHttpClient httpClient) {
        return baseConfig().option(SdkClientOption.CLIENT_PROTOCOL, protocol)
                           .option(SdkClientOption.ASYNC_HTTP_CLIENT, httpClient)
                           .option(SdkClientOption.SCHEDULED_EXECUTOR_SERVICE, scheduledExecutor);
    }

    private static AsyncApiCallPipeline pipeline(SdkClientConfiguration config) {
        return AsyncApiCallPipeline.create(dependencies(config));
    }

    private static ClientExecutionParams<TestRequest, TestResponse> params() {
        return new ClientExecutionParams<TestRequest, TestResponse>().withInput(new TestRequest("name"));
    }

    private static <T> T join(CompletableFuture<T> future) throws Exception {
        return future.get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
    }

    /**
     * The cause of the {@code ExecutionException} that {@code get()} throws for a failed call.
     */
    private static Throwable failure(CompletableFuture<?> future) {
        try {
            future.get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
        } catch (ExecutionException e) {
            return e.getCause();
        } catch (Exception e) {
            throw new AssertionError("The call did not fail", e);
        }
        return fail("The call succeeded");
    }

    private static ByteBuffer upperCase(ByteBuffer buffer) {
        byte[] bytes = new byte[buffer.remaining()];
        buffer.get(bytes);
        return ByteBuffer.wrap(new String(bytes, StandardCharsets.UTF_8).toUpperCase(Locale.ROOT)
                                                                          .getBytes(StandardCharsets.UTF_8));
    }

    /**
     * The client handler's context factory, as {@code BaseClientHandler.invokeInterceptorsAndCreateExecutionContext}
     * builds it, with an SRA auth scheme when the test sets an identity.
     */
    private ExecutionContext executionContext(ClientExecutionParams<TestRequest, TestResponse> params) {
        ExecutionAttributes attributes = params.executionAttributes();
        attributes.putAttribute(InternalCoreExecutionAttribute.EXECUTION_ATTEMPT, 1);
        if (identity != null) {
            attributes.putAttribute(SdkInternalExecutionAttribute.AUTH_SCHEMES, Collections.emptyMap());
            attributes.putAttribute(SdkInternalExecutionAttribute.SELECTED_AUTH_SCHEME,
                                    new SelectedAuthScheme<>(identity, signer,
                                                             AuthSchemeOption.builder().schemeId("aws.auth#sigv4").build()));
        }
        ExecutionInterceptorChain chain = new ExecutionInterceptorChain(Collections.singletonList(interceptor));
        InterceptorContext interceptorContext = InterceptorContext.builder().request(params.getInput()).build();
        chain.beforeExecution(interceptorContext, attributes);
        interceptorContext = chain.modifyRequest(interceptorContext, attributes);
        return ExecutionContext.builder()
                               .interceptorChain(chain)
                               .interceptorContext(interceptorContext)
                               .executionAttributes(attributes)
                               .metricCollector(params.getMetricCollector())
                               .build();
    }

    private static List<Integer> identities(List<?> objects) {
        List<Integer> result = new ArrayList<>();
        objects.forEach(o -> result.add(System.identityHashCode(o)));
        return result;
    }

    private static SdkHttpFullResponse httpResponse(int status, String body) {
        return SdkHttpFullResponse.builder()
                                  .statusCode(status)
                                  .putHeader("x-amzn-RequestId", "request-1")
                                  .content(AbortableInputStream.create(
                                      new ByteArrayInputStream(body.getBytes(StandardCharsets.UTF_8))))
                                  .build();
    }

    /**
     * An HTTP protocol whose output's value is the response body, and whose error responses throw an
     * {@link SdkServiceException} with the status code.
     */
    private static final class TestProtocol extends HttpStubProtocol {
        final List<InputStream> deserializedBodies = new CopyOnWriteArrayList<>();
        final List<TypeRegistry> errorRegistries = new CopyOnWriteArrayList<>();

        @Override
        @SuppressWarnings("unchecked")
        public <I extends SerializableStruct,
            O extends SerializableStruct> O deserializeResponse(
            ApiOperation<I, O> operation, ExecutionAttributes context, TypeRegistry errorRegistry,
            SdkHttpFullRequest request, SdkHttpFullResponse response) {
            errorRegistries.add(errorRegistry);
            response.content().ifPresent(deserializedBodies::add);
            if (!response.isSuccessful()) {
                throw SdkServiceException.builder().statusCode(response.statusCode())
                                         .message("Service error " + response.statusCode()).build();
            }
            SdkResponse.Builder builder = TestResponse.builder().value(read(response)).sdkHttpResponse(response);
            return (O) builder.build();
        }

        private static String read(SdkHttpFullResponse response) {
            try {
                return IoUtils.toUtf8String(response.content().get());
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        }
    }

    /**
     * A transport that answers each request with the next scripted response, failure or pending future.
     */
    private static class ScriptedTransport implements AsyncClientTransport<SdkHttpFullRequest, SdkHttpFullResponse> {
        final Deque<Function<SdkHttpFullRequest, CompletableFuture<SdkHttpFullResponse>>> script = new ArrayDeque<>();
        final List<SdkHttpFullRequest> requests = new CopyOnWriteArrayList<>();
        final List<ExecutionAttributes> attributes = new CopyOnWriteArrayList<>();
        final List<InputStream> responseBodies = new CopyOnWriteArrayList<>();

        void respond(int status, String body) {
            script.add(request -> {
                SdkHttpFullResponse response = httpResponse(status, body);
                responseBodies.add(response.content().get());
                return CompletableFuture.completedFuture(response);
            });
        }

        void fail(Supplier<RuntimeException> failure) {
            script.add(request -> {
                CompletableFuture<SdkHttpFullResponse> failed = new CompletableFuture<>();
                failed.completeExceptionally(failure.get());
                return failed;
            });
        }

        CompletableFuture<SdkHttpFullResponse> pending() {
            CompletableFuture<SdkHttpFullResponse> pending = new CompletableFuture<>();
            script.add(request -> pending);
            return pending;
        }

        @Override
        public MessageExchange<SdkHttpFullRequest, SdkHttpFullResponse> messageExchange() {
            return HttpMessageExchange.INSTANCE;
        }

        @Override
        public synchronized CompletableFuture<SdkHttpFullResponse> send(
            ExecutionAttributes context, SdkHttpFullRequest request, AsyncResponseHandoff<SdkHttpFullResponse> handoff) {
            requests.add(request);
            attributes.add(context);
            Function<SdkHttpFullRequest, CompletableFuture<SdkHttpFullResponse>> next = script.poll();
            if (next == null) {
                throw new IllegalStateException("No scripted response for request " + requests.size());
            }
            return next.apply(request);
        }

        @Override
        public void close() {
        }
    }

    /**
     * An async HTTP client whose requests never complete; the transport fails their futures to cancel them.
     */
    private static final class PendingHttpClient implements SdkAsyncHttpClient {
        final List<CompletableFuture<Void>> executions = new CopyOnWriteArrayList<>();

        @Override
        public CompletableFuture<Void> execute(AsyncExecuteRequest request) {
            CompletableFuture<Void> execution = new CompletableFuture<>();
            executions.add(execution);
            return execution;
        }

        @Override
        public void close() {
        }
    }

    /**
     * An async HTTP client that answers every request with status 200 and the body {@code from-http-client}.
     */
    private static final class RespondingHttpClient implements SdkAsyncHttpClient {
        final List<AsyncExecuteRequest> requests = new CopyOnWriteArrayList<>();
        volatile boolean closed;

        @Override
        public CompletableFuture<Void> execute(AsyncExecuteRequest request) {
            requests.add(request);
            byte[] body = "from-http-client".getBytes(StandardCharsets.UTF_8);
            request.responseHandler().onHeaders(SdkHttpFullResponse.builder()
                                                                   .statusCode(200)
                                                                   .putHeader("Content-Length", String.valueOf(body.length))
                                                                   .build());
            request.responseHandler().onStream(SdkPublisher.fromIterable(Collections.singletonList(ByteBuffer.wrap(body))));
            return CompletableFuture.completedFuture(null);
        }

        @Override
        public void close() {
            closed = true;
        }
    }

    /**
     * A signer that signs synchronous bodies inline and completes the signature of an asynchronous body when the test
     * completes {@link #asyncSignature}.
     */
    private static final class TestSigner implements HttpSigner<AwsCredentialsIdentity> {
        final List<Thread> syncSignThreads = new CopyOnWriteArrayList<>();
        final List<AsyncSignRequest<? extends AwsCredentialsIdentity>> asyncSignRequests = new CopyOnWriteArrayList<>();
        final CompletableFuture<AsyncSignedRequest> asyncSignature = new CompletableFuture<>();

        @Override
        public SignedRequest sign(SignRequest<? extends AwsCredentialsIdentity> request) {
            syncSignThreads.add(Thread.currentThread());
            return SignedRequest.builder()
                                .request(request.request().toBuilder()
                                                .putHeader("Authorization", "signed-by-" + request.identity().accessKeyId())
                                                .build())
                                .payload(request.payload().orElse(null))
                                .build();
        }

        @Override
        public CompletableFuture<AsyncSignedRequest> signAsync(AsyncSignRequest<? extends AwsCredentialsIdentity> request) {
            asyncSignRequests.add(request);
            return asyncSignature;
        }
    }

    /**
     * Records every hook, the attributes it received, its thread and selected contexts.
     */
    private static final class RecordingInterceptor implements ExecutionInterceptor {
        final List<String> hooks = new CopyOnWriteArrayList<>();
        final List<ExecutionAttributes> attributes = new CopyOnWriteArrayList<>();
        final Map<String, String> threads = new ConcurrentHashMap<>();
        volatile Context.BeforeTransmission beforeTransmission;
        volatile Context.AfterTransmission afterTransmission;
        volatile Context.AfterExecution afterExecution;
        volatile Integer attemptAtBeforeTransmission;
        volatile MetricCollector attemptCollectorAtBeforeTransmission;
        volatile Throwable failure;
        volatile RuntimeException afterExecutionFailure;
        volatile AsyncRequestBody asyncRequestBody;
        volatile Function<Publisher<ByteBuffer>, Publisher<ByteBuffer>> responsePublisher;
        volatile boolean readAsyncResponseBody;
        volatile String asyncResponseBody;

        int count(String hook) {
            return Collections.frequency(hooks, hook);
        }

        private void record(String hook, ExecutionAttributes executionAttributes) {
            hooks.add(hook);
            threads.put(hook, Thread.currentThread().getName());
            if (!hook.equals("modifyException") && !hook.equals("onExecutionFailure")) {
                attributes.add(executionAttributes);
            }
        }

        @Override
        public void beforeExecution(Context.BeforeExecution context, ExecutionAttributes executionAttributes) {
            record("beforeExecution", executionAttributes);
        }

        @Override
        public SdkRequest modifyRequest(Context.ModifyRequest context, ExecutionAttributes executionAttributes) {
            record("modifyRequest", executionAttributes);
            return context.request();
        }

        @Override
        public void beforeMarshalling(Context.BeforeMarshalling context, ExecutionAttributes executionAttributes) {
            record("beforeMarshalling", executionAttributes);
        }

        @Override
        public void afterMarshalling(Context.AfterMarshalling context, ExecutionAttributes executionAttributes) {
            record("afterMarshalling", executionAttributes);
        }

        @Override
        public Optional<AsyncRequestBody> modifyAsyncHttpContent(
            Context.ModifyHttpRequest context, ExecutionAttributes executionAttributes) {
            record("modifyAsyncHttpContent", executionAttributes);
            return asyncRequestBody != null ? Optional.of(asyncRequestBody) : context.asyncRequestBody();
        }

        @Override
        public Optional<RequestBody> modifyHttpContent(
            Context.ModifyHttpRequest context, ExecutionAttributes executionAttributes) {
            record("modifyHttpContent", executionAttributes);
            return context.requestBody();
        }

        @Override
        public SdkHttpRequest modifyHttpRequest(Context.ModifyHttpRequest context, ExecutionAttributes executionAttributes) {
            record("modifyHttpRequest", executionAttributes);
            return context.httpRequest();
        }

        @Override
        public void beforeTransmission(Context.BeforeTransmission context, ExecutionAttributes executionAttributes) {
            record("beforeTransmission", executionAttributes);
            beforeTransmission = context;
            attemptAtBeforeTransmission = executionAttributes.getAttribute(InternalCoreExecutionAttribute.EXECUTION_ATTEMPT);
            attemptCollectorAtBeforeTransmission =
                executionAttributes.getAttribute(SdkExecutionAttribute.API_CALL_ATTEMPT_METRIC_COLLECTOR);
        }

        @Override
        public void afterTransmission(Context.AfterTransmission context, ExecutionAttributes executionAttributes) {
            record("afterTransmission", executionAttributes);
            afterTransmission = context;
        }

        @Override
        public SdkHttpResponse modifyHttpResponse(Context.ModifyHttpResponse context, ExecutionAttributes executionAttributes) {
            record("modifyHttpResponse", executionAttributes);
            return context.httpResponse();
        }

        @Override
        public Optional<InputStream> modifyHttpResponseContent(Context.ModifyHttpResponse context,
                                                               ExecutionAttributes executionAttributes) {
            record("modifyHttpResponseContent", executionAttributes);
            return context.responseBody();
        }

        @Override
        public void beforeUnmarshalling(Context.BeforeUnmarshalling context, ExecutionAttributes executionAttributes) {
            record("beforeUnmarshalling", executionAttributes);
        }

        @Override
        public Optional<Publisher<ByteBuffer>> modifyAsyncHttpResponseContent(Context.ModifyHttpResponse context,
                                                                               ExecutionAttributes executionAttributes) {
            record("modifyAsyncHttpResponseContent", executionAttributes);
            Optional<Publisher<ByteBuffer>> publisher = context.responsePublisher();
            if (publisher.isPresent() && readAsyncResponseBody) {
                // Read the publisher as an interceptor that inspects the body without replacing it does.
                StringBuilder body = new StringBuilder();
                SdkPublisher.adapt(publisher.get())
                            .subscribe(b -> body.append(StandardCharsets.UTF_8.decode(b.duplicate())))
                            .join();
                asyncResponseBody = body.toString();
            }
            return responsePublisher != null ? publisher.map(responsePublisher) : publisher;
        }

        @Override
        public void afterUnmarshalling(Context.AfterUnmarshalling context, ExecutionAttributes executionAttributes) {
            record("afterUnmarshalling", executionAttributes);
        }

        @Override
        public SdkResponse modifyResponse(Context.ModifyResponse context, ExecutionAttributes executionAttributes) {
            record("modifyResponse", executionAttributes);
            return context.response();
        }

        @Override
        public void afterExecution(Context.AfterExecution context, ExecutionAttributes executionAttributes) {
            record("afterExecution", executionAttributes);
            afterExecution = context;
            if (afterExecutionFailure != null) {
                throw afterExecutionFailure;
            }
        }

        @Override
        public Throwable modifyException(Context.FailedExecution context, ExecutionAttributes executionAttributes) {
            record("modifyException", executionAttributes);
            return context.exception();
        }

        @Override
        public void onExecutionFailure(Context.FailedExecution context, ExecutionAttributes executionAttributes) {
            record("onExecutionFailure", executionAttributes);
            failure = context.exception();
        }
    }
}
