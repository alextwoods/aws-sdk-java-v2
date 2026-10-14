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
import static software.amazon.awssdk.core.internal.http.ApiCallStagesTestSupport.CLIENT_ENDPOINT;
import static software.amazon.awssdk.core.internal.http.ApiCallStagesTestSupport.OPERATION_PATH;
import static software.amazon.awssdk.core.internal.http.ApiCallStagesTestSupport.UUID_PATTERN;
import static software.amazon.awssdk.core.internal.http.ApiCallStagesTestSupport.baseConfig;
import static software.amazon.awssdk.core.internal.http.ApiCallStagesTestSupport.dependencies;
import static software.amazon.awssdk.core.internal.http.ProtocolStepsTestSupport.OPERATION;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.function.Supplier;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.core.SdkRequest;
import software.amazon.awssdk.core.SdkResponse;
import software.amazon.awssdk.core.async.AsyncRequestBody;
import software.amazon.awssdk.core.client.config.SdkClientConfiguration;
import software.amazon.awssdk.core.client.config.SdkClientOption;
import software.amazon.awssdk.core.client.handler.ClientExecutionParams;
import software.amazon.awssdk.core.client.protocol.ClientTransport;
import software.amazon.awssdk.core.client.protocol.HttpMessageExchange;
import software.amazon.awssdk.core.client.protocol.MessageExchange;
import software.amazon.awssdk.core.exception.AbortedException;
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
import software.amazon.awssdk.core.internal.InternalCoreExecutionAttribute;
import software.amazon.awssdk.core.internal.http.ApiCallStagesTestSupport.HttpStubProtocol;
import software.amazon.awssdk.core.internal.http.ProtocolStepsTestSupport.TestRequest;
import software.amazon.awssdk.core.internal.http.ProtocolStepsTestSupport.TestResponse;
import software.amazon.awssdk.core.internal.http.transport.SdkHttpClientTransport;
import software.amazon.awssdk.core.internal.http.transport.TransportExecutionAttribute;
import software.amazon.awssdk.core.internal.util.MetricUtils;
import software.amazon.awssdk.core.metrics.CoreMetric;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.core.sync.ResponseTransformer;
import software.amazon.awssdk.http.AbortableInputStream;
import software.amazon.awssdk.http.ExecutableHttpRequest;
import software.amazon.awssdk.http.HttpExecuteRequest;
import software.amazon.awssdk.http.HttpExecuteResponse;
import software.amazon.awssdk.http.HttpMetric;
import software.amazon.awssdk.http.SdkHttpClient;
import software.amazon.awssdk.http.SdkHttpFullRequest;
import software.amazon.awssdk.http.SdkHttpFullResponse;
import software.amazon.awssdk.http.SdkHttpRequest;
import software.amazon.awssdk.http.SdkHttpResponse;
import software.amazon.awssdk.metrics.MetricCollection;
import software.amazon.awssdk.metrics.MetricCollector;
import software.amazon.awssdk.metrics.NoOpMetricCollector;
import software.amazon.awssdk.schema.ApiOperation;
import software.amazon.awssdk.schema.SerializableStruct;
import software.amazon.awssdk.schema.serde.TypeRegistry;
import software.amazon.awssdk.utils.IoUtils;

/**
 * Tests for {@link SyncApiCallPipeline} with a stub operation, protocol and transport: outputs, retries, failure
 * reporting and exception types, timeouts, interrupts, metrics, the default transport and streaming outputs. The full
 * comparison of hook sequences with the legacy pipeline is {@code ApiCallPipelineInterceptorOrderTest} (task 3.29).
 */
class SyncApiCallPipelineTest {
    private static final List<String> SUCCESS_HOOKS = Arrays.asList(
        "beforeExecution", "modifyRequest", "beforeMarshalling", "afterMarshalling", "modifyAsyncHttpContent",
        "modifyHttpContent", "modifyHttpRequest", "beforeTransmission", "afterTransmission", "modifyHttpResponse",
        "modifyHttpResponseContent", "beforeUnmarshalling", "afterUnmarshalling", "modifyResponse", "afterExecution");
    private static final List<String> ATTEMPT_HOOKS = Arrays.asList(
        "beforeTransmission", "afterTransmission", "modifyHttpResponse", "modifyHttpResponseContent",
        "beforeUnmarshalling");

    private final TestProtocol protocol = new TestProtocol();
    private final ScriptedTransport transport = new ScriptedTransport();
    private final RecordingInterceptor interceptor = new RecordingInterceptor();
    private ScheduledExecutorService timeoutExecutor;

    @AfterEach
    void tearDown() {
        Thread.interrupted();
        if (timeoutExecutor != null) {
            timeoutExecutor.shutdownNow();
        }
    }

    @Test
    void success_returnsTheOutputAndRunsEveryHookOnceInTheLegacyOrder() {
        transport.respond(200, "hello");

        TestResponse output = pipeline(config()).execute(OPERATION, params(), this::executionContext);

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
    void success_hooksSeeTheLegacyContextsAndAttributes() {
        transport.respond(200, "hello");

        pipeline(config()).execute(OPERATION, params(), this::executionContext);

        ExecutionAttributes attributes = interceptor.attributes.get(0);
        assertThat(attributes.getAttribute(SdkExecutionAttribute.API_OPERATION)).isSameAs(OPERATION);
        assertThat(attributes.getAttribute(SdkExecutionAttribute.CLIENT_PROTOCOL)).isSameAs(protocol);
        assertThat(attributes.getAttribute(SdkExecutionAttribute.ERROR_REGISTRY)).isSameAs(OPERATION.errorRegistry());
        assertThat(interceptor.beforeTransmission.httpRequest().firstMatchingHeader("amz-sdk-request"))
            .hasValue("attempt=1; max=3");
        assertThat(interceptor.attemptAtBeforeTransmission).isEqualTo(1);
        assertThat(interceptor.attemptCollectorAtBeforeTransmission).isNotNull();
        assertThat(interceptor.afterTransmission.httpResponse().statusCode()).isEqualTo(200);
        assertThat(interceptor.afterTransmission.responseBody()).isPresent();
        assertThat(interceptor.afterExecution.response()).isInstanceOf(TestResponse.class);
        assertThat(interceptor.afterExecution.httpResponse().statusCode()).isEqualTo(200);
    }

    @Test
    void serviceErrorThenSuccess_retriesAndRunsTheAttemptHooksPerAttempt() {
        transport.respond(500, "busy");
        transport.respond(200, "hello");

        TestResponse output = pipeline(config()).execute(OPERATION, params(), this::executionContext);

        assertThat(output.value()).isEqualTo("hello");
        assertThat(transport.requests).hasSize(2);
        assertThat(transport.requests.get(1).firstMatchingHeader("amz-sdk-request")).hasValue("attempt=2; max=3");
        List<String> expected = new ArrayList<>(SUCCESS_HOOKS.subList(0, 7));
        expected.addAll(ATTEMPT_HOOKS);
        expected.addAll(SUCCESS_HOOKS.subList(7, SUCCESS_HOOKS.size()));
        assertThat(interceptor.hooks).isEqualTo(expected);
    }

    @Test
    void nonRetryableServiceError_reportsTheFailureOnceAndThrowsTheServiceException() {
        transport.respond(400, "bad");

        assertThatThrownBy(() -> pipeline(config()).execute(OPERATION, params(), this::executionContext))
            .isInstanceOf(SdkServiceException.class)
            .satisfies(e -> assertThat(((SdkServiceException) e).statusCode()).isEqualTo(400));

        assertThat(transport.requests).hasSize(1);
        List<String> expected = new ArrayList<>(SUCCESS_HOOKS.subList(0, 12));
        expected.addAll(Arrays.asList("modifyException", "onExecutionFailure"));
        assertThat(interceptor.hooks).isEqualTo(expected);
        assertThat(interceptor.failure).isInstanceOf(SdkServiceException.class);
    }

    @Test
    void clientError_isNotRetriedAndKeepsItsTypeMessageAndCause() {
        IllegalArgumentException cause = new IllegalArgumentException("root");
        transport.fail(() -> SdkClientException.create("client failure", cause));

        assertThatThrownBy(() -> pipeline(config()).execute(OPERATION, params(), this::executionContext))
            .isExactlyInstanceOf(SdkClientException.class)
            .hasMessageContaining("client failure")
            .hasCause(cause);

        assertThat(transport.requests).hasSize(1);
        assertThat(interceptor.count("onExecutionFailure")).isEqualTo(1);
        assertThat(interceptor.hooks).doesNotContain("afterTransmission", "afterExecution");
    }

    @Test
    void nonSdkRuntimeExceptionFromTheTransport_isNotRetriedAndPropagatesUnchanged() {
        IllegalStateException failure = new IllegalStateException("transport bug");
        transport.fail(() -> failure);

        assertThatThrownBy(() -> pipeline(config()).execute(OPERATION, params(), this::executionContext))
            .isSameAs(failure);

        assertThat(transport.requests).hasSize(1);
        assertThat(interceptor.count("onExecutionFailure")).isEqualTo(1);
    }

    @Test
    void serializationFailure_isReportedOnceAndNothingIsSent() {
        SdkClientException failure = SdkClientException.create("Unable to marshall GetThing: boom");
        protocol.serialize = endpoint -> {
            throw failure;
        };

        assertThatThrownBy(() -> pipeline(config()).execute(OPERATION, params(), this::executionContext))
            .isSameAs(failure);

        assertThat(transport.requests).isEmpty();
        assertThat(interceptor.hooks).containsExactly("beforeExecution", "modifyRequest", "beforeMarshalling",
                                                      "modifyException", "onExecutionFailure");
    }

    @Test
    void retriesExhausted_reportsTheFailureOnceWithTheLegacyExceptionTypeAndCause() {
        IOException cause = new IOException("connection reset");
        for (int i = 0; i < 3; i++) {
            transport.fail(() -> SdkClientException.create("Unable to execute HTTP request: connection reset", cause));
        }

        assertThatThrownBy(() -> pipeline(config()).execute(OPERATION, params(), this::executionContext))
            .isExactlyInstanceOf(SdkClientException.class)
            .hasCause(cause)
            .satisfies(e -> assertThat(e.getSuppressed()).hasSize(2));

        assertThat(transport.requests).hasSize(3);
        assertThat(interceptor.count("beforeTransmission")).isEqualTo(3);
        assertThat(interceptor.count("modifyException")).isEqualTo(1);
        assertThat(interceptor.count("onExecutionFailure")).isEqualTo(1);
        assertThat(interceptor.hooks.get(interceptor.hooks.size() - 1)).isEqualTo("onExecutionFailure");
        assertThat(interceptor.failure).isInstanceOf(SdkClientException.class).hasCause(cause);
    }

    @Test
    void serviceErrorsExhausted_throwsTheLastServiceException() {
        for (int i = 0; i < 3; i++) {
            transport.respond(503, "busy");
        }

        assertThatThrownBy(() -> pipeline(config()).execute(OPERATION, params(), this::executionContext))
            .isInstanceOf(SdkServiceException.class)
            .satisfies(e -> assertThat(((SdkServiceException) e).statusCode()).isEqualTo(503));

        assertThat(transport.requests).hasSize(3);
        assertThat(interceptor.count("onExecutionFailure")).isEqualTo(1);
    }

    @Test
    void afterExecutionFailure_isReportedOnce() {
        transport.respond(200, "hello");
        RuntimeException failure = new RuntimeException("Uh oh!");
        interceptor.afterExecutionFailure = failure;

        assertThatThrownBy(() -> pipeline(config()).execute(OPERATION, params(), this::executionContext))
            .isSameAs(failure);

        assertThat(interceptor.hooks.subList(interceptor.hooks.size() - 3, interceptor.hooks.size()))
            .containsExactly("afterExecution", "modifyException", "onExecutionFailure");
    }

    @Test
    void attemptTimeout_abortsEachAttemptAndThrowsApiCallAttemptTimeoutException() {
        BlockingHttpClient httpClient = new BlockingHttpClient();
        SdkClientConfiguration config = timeoutConfig(httpClient)
            .option(SdkClientOption.API_CALL_ATTEMPT_TIMEOUT, Duration.ofMillis(100))
            .build();

        assertThatThrownBy(() -> pipeline(config).execute(OPERATION, params(), this::executionContext))
            .isInstanceOf(ApiCallAttemptTimeoutException.class);

        assertThat(httpClient.calls.get()).isEqualTo(3);
        assertThat(httpClient.aborts.get()).isEqualTo(3);
        assertThat(interceptor.count("onExecutionFailure")).isEqualTo(1);
        assertThat(Thread.currentThread().isInterrupted()).isFalse();
    }

    @Test
    void apiCallTimeout_throwsApiCallTimeoutExceptionWithoutRetrying() {
        BlockingHttpClient httpClient = new BlockingHttpClient();
        SdkClientConfiguration config = timeoutConfig(httpClient)
            .option(SdkClientOption.API_CALL_TIMEOUT, Duration.ofMillis(200))
            .build();

        assertThatThrownBy(() -> pipeline(config).execute(OPERATION, params(), this::executionContext))
            .isInstanceOf(ApiCallTimeoutException.class);

        assertThat(httpClient.calls.get()).isEqualTo(1);
        assertThat(httpClient.aborts.get()).isEqualTo(1);
        assertThat(interceptor.count("onExecutionFailure")).isEqualTo(1);
        assertThat(Thread.currentThread().isInterrupted()).isFalse();
    }

    @Test
    void interruptedThread_throwsAbortedExceptionAndKeepsTheInterruptFlag() {
        transport.respond(200, "hello");
        Thread.currentThread().interrupt();

        assertThatThrownBy(() -> pipeline(config()).execute(OPERATION, params(), this::executionContext))
            .isInstanceOf(AbortedException.class)
            .hasMessageContaining("Thread was interrupted")
            .hasCauseInstanceOf(InterruptedException.class);

        assertThat(Thread.interrupted()).isTrue();
        assertThat(transport.requests).isEmpty();
        assertThat(interceptor.count("onExecutionFailure")).isEqualTo(1);
    }

    @Test
    void metricsEnabled_reportsTheLegacyCallAndAttemptMetrics() {
        transport.respond(500, "busy");
        transport.respond(200, "hello");
        MetricCollector collector = MetricCollector.create("ApiCall");

        pipeline(config()).execute(OPERATION, params().withMetricCollector(collector), this::executionContext);

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
    void metricsDisabled_usesNoOpCollectorsAndDoesNotWrapTheResponseBody() {
        transport.respond(200, "hello");
        ClientExecutionParams<TestRequest, TestResponse> params = params();

        pipeline(config()).execute(OPERATION, params, this::executionContext);

        assertThat(params.getMetricCollector()).isInstanceOf(NoOpMetricCollector.class);
        ExecutionAttributes attributes = transport.attributes.get(0);
        assertThat(MetricUtils.collectsMetrics(attributes.getAttribute(SdkExecutionAttribute.API_CALL_ATTEMPT_METRIC_COLLECTOR)))
            .isFalse();
        assertThat(attributes.getAttribute(TransportExecutionAttribute.HTTP_METRIC_COLLECTOR))
            .isInstanceOf(NoOpMetricCollector.class);
        assertThat(protocol.deserializedBodies).containsExactly(transport.responseBodies.get(0));
        // The attributes the legacy attempt stage sets are still there, as interceptors see them on the legacy path.
        assertThat(attributes.getAttribute(InternalCoreExecutionAttribute.REQUEST_BODY_METRICS)).isNotNull();
    }

    @Test
    void noConfiguredTransport_sendsThroughTheSyncHttpClient() {
        RespondingHttpClient httpClient = new RespondingHttpClient();
        SdkClientConfiguration config = baseConfig().option(SdkClientOption.CLIENT_PROTOCOL, protocol)
                                                    .option(SdkClientOption.SYNC_HTTP_CLIENT, httpClient)
                                                    .build();
        SyncApiCallPipeline pipeline = pipeline(config);

        TestResponse output = pipeline.execute(OPERATION, params(), this::executionContext);

        assertThat(pipeline.transport()).isInstanceOf(SdkHttpClientTransport.class);
        assertThat(output.value()).isEqualTo("from-http-client");
        assertThat(httpClient.requests).hasSize(1);
        assertThat(httpClient.requests.get(0).httpRequest().getUri().toString()).isEqualTo(CLIENT_ENDPOINT + OPERATION_PATH);

        pipeline.transport().close();
        assertThat(httpClient.closed).as("the configuration owns the HTTP client").isFalse();
    }

    @Test
    void configuredTransport_isUsedInsteadOfTheHttpClient() {
        RespondingHttpClient httpClient = new RespondingHttpClient();
        transport.respond(200, "from-transport");
        SdkClientConfiguration config = config().toBuilder().option(SdkClientOption.SYNC_HTTP_CLIENT, httpClient).build();
        SyncApiCallPipeline pipeline = pipeline(config);

        TestResponse output = pipeline.execute(OPERATION, params(), this::executionContext);

        assertThat(pipeline.transport()).isSameAs(transport);
        assertThat(output.value()).isEqualTo("from-transport");
        assertThat(httpClient.requests).isEmpty();
    }

    @Test
    void transportOfAnotherExchangeOrNoTransport_isRejected() {
        ScriptedTransport otherExchange = new ScriptedTransport() {
            @Override
            public MessageExchange<SdkHttpFullRequest, SdkHttpFullResponse> messageExchange() {
                return new MessageExchange<SdkHttpFullRequest, SdkHttpFullResponse>() {
                };
            }
        };
        SdkClientConfiguration mismatched = config().toBuilder().option(SdkClientOption.CLIENT_TRANSPORT, otherExchange)
                                                    .build();
        assertThatThrownBy(() -> pipeline(mismatched)).isInstanceOf(IllegalStateException.class)
                                                      .hasMessageContaining("exchanges");

        SdkClientConfiguration none = baseConfig().option(SdkClientOption.CLIENT_PROTOCOL, protocol).build();
        assertThatThrownBy(() -> pipeline(none)).isInstanceOf(IllegalStateException.class)
                                                .hasMessageContaining("SYNC_HTTP_CLIENT");
    }

    @Test
    void streamingOutput_handsTheOutputAndBodyToTheTransformer() {
        transport.respond(200, "streamed-body");
        protocol.readBody = false;
        List<TestResponse> outputs = new ArrayList<>();
        ResponseTransformer<TestResponse, String> transformer = (response, body) -> {
            outputs.add(response);
            return IoUtils.toUtf8String(body);
        };

        String result = pipeline(config()).execute(OPERATION, params(), this::executionContext, transformer);

        assertThat(result).isEqualTo("streamed-body");
        assertThat(outputs).hasSize(1);
        assertThat(outputs.get(0).value()).isEqualTo("headers-only");
        assertThat(interceptor.hooks).isEqualTo(SUCCESS_HOOKS);
    }

    @Test
    void errorRegistryAttribute_isPassedToTheProtocol() {
        transport.respond(200, "hello");
        TypeRegistry serviceRegistry = TypeRegistry.empty();
        SyncApiCallPipeline pipeline = SyncApiCallPipeline.create(dependencies(config()), serviceRegistry);

        pipeline.execute(OPERATION, params(), this::executionContext);

        assertThat(protocol.errorRegistries).containsExactly(serviceRegistry);
    }

    private SdkClientConfiguration config() {
        return baseConfig().option(SdkClientOption.CLIENT_PROTOCOL, protocol)
                           .option(SdkClientOption.CLIENT_TRANSPORT, transport)
                           .build();
    }

    private SdkClientConfiguration.Builder timeoutConfig(SdkHttpClient httpClient) {
        timeoutExecutor = Executors.newScheduledThreadPool(1);
        return baseConfig().option(SdkClientOption.CLIENT_PROTOCOL, protocol)
                           .option(SdkClientOption.SYNC_HTTP_CLIENT, httpClient)
                           .option(SdkClientOption.SCHEDULED_EXECUTOR_SERVICE, timeoutExecutor);
    }

    private static SyncApiCallPipeline pipeline(SdkClientConfiguration config) {
        return SyncApiCallPipeline.create(dependencies(config));
    }

    private static ClientExecutionParams<TestRequest, TestResponse> params() {
        return new ClientExecutionParams<TestRequest, TestResponse>().withInput(new TestRequest("name"));
    }

    /**
     * The client handler's context factory, as {@code BaseClientHandler.invokeInterceptorsAndCreateExecutionContext}
     * builds it.
     */
    private ExecutionContext executionContext(ClientExecutionParams<TestRequest, TestResponse> params) {
        ExecutionAttributes attributes = params.executionAttributes();
        attributes.putAttribute(InternalCoreExecutionAttribute.EXECUTION_ATTEMPT, 1);
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
     * An HTTP protocol whose output's value is the response body, or {@code "headers-only"} for a streaming output, and
     * whose error responses throw an {@link SdkServiceException} with the status code.
     */
    private static final class TestProtocol extends HttpStubProtocol {
        final List<InputStream> deserializedBodies = new ArrayList<>();
        final List<TypeRegistry> errorRegistries = new ArrayList<>();
        boolean readBody = true;

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
            String value = readBody ? read(response) : "headers-only";
            SdkResponse.Builder builder = TestResponse.builder().value(value).sdkHttpResponse(response);
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
     * A transport that answers each request with the next scripted response or failure.
     */
    private static class ScriptedTransport implements ClientTransport<SdkHttpFullRequest, SdkHttpFullResponse> {
        final Deque<Function<SdkHttpFullRequest, SdkHttpFullResponse>> script = new ArrayDeque<>();
        final List<SdkHttpFullRequest> requests = new ArrayList<>();
        final List<ExecutionAttributes> attributes = new ArrayList<>();
        final List<InputStream> responseBodies = new ArrayList<>();

        void respond(int status, String body) {
            script.add(request -> {
                SdkHttpFullResponse response = httpResponse(status, body);
                responseBodies.add(response.content().get());
                return response;
            });
        }

        void fail(Supplier<RuntimeException> failure) {
            script.add(request -> {
                throw failure.get();
            });
        }

        @Override
        public MessageExchange<SdkHttpFullRequest, SdkHttpFullResponse> messageExchange() {
            return HttpMessageExchange.INSTANCE;
        }

        @Override
        public SdkHttpFullResponse send(ExecutionAttributes context, SdkHttpFullRequest request) {
            requests.add(request);
            attributes.add(context);
            Function<SdkHttpFullRequest, SdkHttpFullResponse> next = script.poll();
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
     * An HTTP client whose requests block until they are aborted or interrupted, then fail as an aborted connection.
     */
    private static final class BlockingHttpClient implements SdkHttpClient {
        final AtomicInteger calls = new AtomicInteger();
        final AtomicInteger aborts = new AtomicInteger();

        @Override
        public ExecutableHttpRequest prepareRequest(HttpExecuteRequest request) {
            CountDownLatch aborted = new CountDownLatch(1);
            return new ExecutableHttpRequest() {
                @Override
                public HttpExecuteResponse call() throws IOException {
                    calls.incrementAndGet();
                    try {
                        if (!aborted.await(10, TimeUnit.SECONDS)) {
                            throw new IOException("Never aborted");
                        }
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        throw new IOException("Interrupted", e);
                    }
                    throw new IOException("Request aborted");
                }

                @Override
                public void abort() {
                    aborts.incrementAndGet();
                    aborted.countDown();
                }
            };
        }

        @Override
        public void close() {
        }
    }

    /**
     * An HTTP client that answers every request with status 200 and the body {@code from-http-client}.
     */
    private static final class RespondingHttpClient implements SdkHttpClient {
        final List<HttpExecuteRequest> requests = new ArrayList<>();
        volatile boolean closed;

        @Override
        public ExecutableHttpRequest prepareRequest(HttpExecuteRequest request) {
            requests.add(request);
            return new ExecutableHttpRequest() {
                @Override
                public HttpExecuteResponse call() {
                    byte[] body = "from-http-client".getBytes(StandardCharsets.UTF_8);
                    return HttpExecuteResponse.builder()
                                              .response(SdkHttpFullResponse.builder().statusCode(200).build())
                                              .responseBody(AbortableInputStream.create(new ByteArrayInputStream(body)))
                                              .build();
                }

                @Override
                public void abort() {
                }
            };
        }

        @Override
        public void close() {
            closed = true;
        }
    }

    /**
     * Records every hook, the attributes it received and selected contexts.
     */
    private static final class RecordingInterceptor implements ExecutionInterceptor {
        final List<String> hooks = new ArrayList<>();
        final List<ExecutionAttributes> attributes = new ArrayList<>();
        Context.BeforeTransmission beforeTransmission;
        Context.AfterTransmission afterTransmission;
        Context.AfterExecution afterExecution;
        Integer attemptAtBeforeTransmission;
        MetricCollector attemptCollectorAtBeforeTransmission;
        Throwable failure;
        RuntimeException afterExecutionFailure;

        int count(String hook) {
            return Collections.frequency(hooks, hook);
        }

        private void record(String hook, ExecutionAttributes executionAttributes) {
            hooks.add(hook);
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
            return context.asyncRequestBody();
        }

        @Override
        public Optional<RequestBody> modifyHttpContent(
            Context.ModifyHttpRequest context, ExecutionAttributes executionAttributes) {
            record("modifyHttpContent", executionAttributes);
            return context.requestBody();
        }

        @Override
        public SdkHttpRequest modifyHttpRequest(Context.ModifyHttpRequest context,
                                                                            ExecutionAttributes executionAttributes) {
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
        public SdkHttpResponse modifyHttpResponse(Context.ModifyHttpResponse context,
                                                                              ExecutionAttributes executionAttributes) {
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
