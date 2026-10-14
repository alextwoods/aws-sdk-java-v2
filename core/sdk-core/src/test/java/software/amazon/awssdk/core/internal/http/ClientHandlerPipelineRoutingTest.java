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
import static software.amazon.awssdk.core.internal.http.ProtocolStepsTestSupport.OPERATION;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.core.async.AsyncResponseTransformer;
import software.amazon.awssdk.core.client.config.SdkAdvancedAsyncClientOption;
import software.amazon.awssdk.core.client.config.SdkClientConfiguration;
import software.amazon.awssdk.core.client.config.SdkClientOption;
import software.amazon.awssdk.core.client.handler.AsyncClientHandler;
import software.amazon.awssdk.core.client.handler.ClientExecutionParams;
import software.amazon.awssdk.core.client.handler.SdkAsyncClientHandler;
import software.amazon.awssdk.core.client.handler.SdkSyncClientHandler;
import software.amazon.awssdk.core.client.handler.SyncClientHandler;
import software.amazon.awssdk.core.client.protocol.AsyncClientTransport;
import software.amazon.awssdk.core.client.protocol.AsyncResponseHandoff;
import software.amazon.awssdk.core.client.protocol.ClientTransport;
import software.amazon.awssdk.core.client.protocol.HttpMessageExchange;
import software.amazon.awssdk.core.client.protocol.MessageExchange;
import software.amazon.awssdk.core.exception.SdkServiceException;
import software.amazon.awssdk.core.interceptor.Context;
import software.amazon.awssdk.core.interceptor.ExecutionAttributes;
import software.amazon.awssdk.core.interceptor.ExecutionInterceptor;
import software.amazon.awssdk.core.interceptor.SdkExecutionAttribute;
import software.amazon.awssdk.core.internal.http.ApiCallStagesTestSupport.HttpStubProtocol;
import software.amazon.awssdk.core.internal.http.ProtocolStepsTestSupport.TestRequest;
import software.amazon.awssdk.core.internal.http.ProtocolStepsTestSupport.TestResponse;
import software.amazon.awssdk.core.metrics.CoreMetric;
import software.amazon.awssdk.core.sync.ResponseTransformer;
import software.amazon.awssdk.http.AbortableInputStream;
import software.amazon.awssdk.http.ExecutableHttpRequest;
import software.amazon.awssdk.http.HttpExecuteRequest;
import software.amazon.awssdk.http.HttpExecuteResponse;
import software.amazon.awssdk.http.SdkHttpClient;
import software.amazon.awssdk.http.SdkHttpFullRequest;
import software.amazon.awssdk.http.SdkHttpFullResponse;
import software.amazon.awssdk.http.SdkHttpMethod;
import software.amazon.awssdk.http.async.AsyncExecuteRequest;
import software.amazon.awssdk.http.async.SdkAsyncHttpClient;
import software.amazon.awssdk.metrics.MetricCollector;
import software.amazon.awssdk.retries.DefaultRetryStrategy;
import software.amazon.awssdk.schema.ApiOperation;
import software.amazon.awssdk.schema.SerializableStruct;
import software.amazon.awssdk.schema.serde.TypeRegistry;
import software.amazon.awssdk.utils.IoUtils;
import utils.HttpTestUtils;

/**
 * Tests that the SDK client handlers run a call with an {@link ApiOperation} on {@link SyncApiCallPipeline} or
 * {@link AsyncApiCallPipeline}, every other call on the legacy pipeline, and report {@code API_CALL_SUCCESSFUL} once.
 * The aws-core handlers extend these handlers and call them for every call, so they route the same way.
 */
class ClientHandlerPipelineRoutingTest {
    private static final long TIMEOUT_SECONDS = 10;

    private final EchoProtocol protocol = new EchoProtocol();
    private final SyncTransport syncTransport = new SyncTransport();
    private final AsyncTransport asyncTransport = new AsyncTransport();
    private final RecordingHttpClient httpClient = new RecordingHttpClient();
    private final RecordingAsyncHttpClient asyncHttpClient = new RecordingAsyncHttpClient();
    private final OperationRecordingInterceptor interceptor = new OperationRecordingInterceptor();
    private final List<AutoCloseable> handlers = new ArrayList<>();

    @AfterEach
    void tearDown() throws Exception {
        for (AutoCloseable handler : handlers) {
            handler.close();
        }
    }

    @Test
    void withOperation_setsTheOperationItsNameAndTheApiOperationAttribute() {
        ClientExecutionParams<TestRequest, TestResponse> params =
            new ClientExecutionParams<TestRequest, TestResponse>().withOperation(OPERATION);

        assertThat(params.getOperation()).isSameAs(OPERATION);
        assertThat(params.getOperationName()).isEqualTo("GetThing");
        assertThat(params.executionAttributes().getAttribute(SdkExecutionAttribute.API_OPERATION)).isSameAs(OPERATION);
    }

    @Test
    void withOperation_null_isRejected() {
        assertThatThrownBy(() -> new ClientExecutionParams<TestRequest, TestResponse>().withOperation(null))
            .isInstanceOf(NullPointerException.class)
            .hasMessageContaining("operation");
    }

    @Test
    void syncExecute_withOperation_runsOnTheSchemaBasedPipeline() {
        SyncClientHandler handler = syncHandler(syncConfig());

        TestResponse output = handler.execute(newPathParams());

        assertThat(output.value()).isEqualTo("from-transport");
        assertThat(syncTransport.requests).hasSize(1);
        assertThat(httpClient.requests).isEmpty();
        assertThat(interceptor.operations).containsExactly(OPERATION);
    }

    @Test
    void syncExecute_withoutOperation_runsOnTheLegacyPipeline() {
        SyncClientHandler handler = syncHandler(syncConfig());

        TestResponse output = handler.execute(legacyParams());

        assertThat(output.value()).isEqualTo("from-http-client");
        assertThat(httpClient.requests).hasSize(1);
        assertThat(syncTransport.requests).isEmpty();
        assertThat(interceptor.operations).containsExactly((ApiOperation<?, ?>) null);
    }

    @Test
    void syncExecute_withOperationAndResponseTransformer_runsOnTheSchemaBasedPipeline() {
        SyncClientHandler handler = syncHandler(syncConfig());
        protocol.readBody = false;
        ResponseTransformer<TestResponse, String> transformer =
            (response, body) -> response.value() + ":" + IoUtils.toUtf8String(body);

        String result = handler.execute(newPathParams(), transformer);

        assertThat(result).isEqualTo("headers-only:from-transport");
        assertThat(syncTransport.requests).hasSize(1);
        assertThat(httpClient.requests).isEmpty();
    }

    @Test
    void syncExecute_withOperation_reportsApiCallSuccessfulOnce() {
        SyncClientHandler handler = syncHandler(syncConfig());
        MetricCollector collector = MetricCollector.create("ApiCall");

        handler.execute(newPathParams().withMetricCollector(collector));

        assertThat(collector.collect().metricValues(CoreMetric.API_CALL_SUCCESSFUL)).containsExactly(true);
    }

    @Test
    void syncExecute_withOperationAndRequestConfiguration_usesThatConfigurationsPipeline() {
        SdkClientConfiguration clientConfig = syncConfig();
        SyncClientHandler handler = syncHandler(clientConfig);
        SyncTransport requestTransport = new SyncTransport();
        SdkClientConfiguration requestConfig =
            clientConfig.toBuilder().option(SdkClientOption.CLIENT_TRANSPORT, requestTransport).build();

        handler.execute(newPathParams().withRequestConfiguration(requestConfig));

        assertThat(requestTransport.requests).hasSize(1);
        assertThat(syncTransport.requests).isEmpty();
    }

    @Test
    void syncExecute_withOperationAndAnUnusableTransport_throwsAndReportsTheFailure() {
        SdkClientConfiguration config = syncConfig().toBuilder()
                                                    .option(SdkClientOption.CLIENT_TRANSPORT, new OtherExchangeTransport())
                                                    .build();
        SyncClientHandler handler = syncHandler(config);
        MetricCollector collector = MetricCollector.create("ApiCall");

        assertThatThrownBy(() -> handler.execute(newPathParams().withMetricCollector(collector)))
            .isInstanceOf(IllegalStateException.class);

        assertThat(collector.collect().metricValues(CoreMetric.API_CALL_SUCCESSFUL)).containsExactly(false);
        assertThat(interceptor.operations).isEmpty();
    }

    @Test
    void asyncExecute_withOperation_runsOnTheSchemaBasedPipeline() throws Exception {
        AsyncClientHandler handler = asyncHandler(asyncConfig());
        MetricCollector collector = MetricCollector.create("ApiCall");

        TestResponse output = handler.execute(newPathParams().withMetricCollector(collector))
                                     .get(TIMEOUT_SECONDS, TimeUnit.SECONDS);

        assertThat(output.value()).isEqualTo("from-transport");
        assertThat(asyncTransport.requests).hasSize(1);
        assertThat(asyncHttpClient.requests).isEmpty();
        assertThat(interceptor.operations).containsExactly(OPERATION);
        assertThat(collector.collect().metricValues(CoreMetric.API_CALL_SUCCESSFUL)).containsExactly(true);
    }

    @Test
    void asyncExecute_withoutOperation_runsOnTheLegacyPipeline() {
        AsyncClientHandler handler = asyncHandler(asyncConfig());

        CompletableFuture<TestResponse> future = handler.execute(legacyParams());

        assertThatThrownBy(() -> future.get(TIMEOUT_SECONDS, TimeUnit.SECONDS))
            .hasRootCauseMessage(RecordingAsyncHttpClient.FAILURE);
        assertThat(asyncHttpClient.requests).hasSize(1);
        assertThat(asyncTransport.requests).isEmpty();
    }

    @Test
    void asyncExecute_withOperationAndResponseTransformerAndMarshaller_runsOnTheLegacyPipeline() {
        AsyncClientHandler handler = asyncHandler(asyncConfig());
        ClientExecutionParams<TestRequest, TestResponse> params = legacyParams().withOperation(OPERATION);

        CompletableFuture<?> future = handler.execute(params, AsyncResponseTransformer.toBytes());

        assertThatThrownBy(() -> future.get(TIMEOUT_SECONDS, TimeUnit.SECONDS))
            .hasRootCauseMessage(RecordingAsyncHttpClient.FAILURE);
        assertThat(asyncHttpClient.requests).hasSize(1);
        assertThat(asyncTransport.requests).isEmpty();
        assertThat(interceptor.operations).as("the legacy path also exposes the operation")
                                          .containsExactly(OPERATION);
    }

    @Test
    void asyncExecute_withOperationAndResponseTransformerWithoutMarshaller_failsAsUnsupported() {
        AsyncClientHandler handler = asyncHandler(asyncConfig());
        MetricCollector collector = MetricCollector.create("ApiCall");

        CompletableFuture<?> future = handler.execute(newPathParams().withMetricCollector(collector),
                                                      AsyncResponseTransformer.toBytes());

        assertThatThrownBy(() -> future.get(TIMEOUT_SECONDS, TimeUnit.SECONDS))
            .hasCauseInstanceOf(UnsupportedOperationException.class)
            .hasMessageContaining("streaming output");
        assertThat(asyncTransport.requests).isEmpty();
        assertThat(asyncHttpClient.requests).isEmpty();
        assertThat(interceptor.operations).isEmpty();
        assertThat(collector.collect().metricValues(CoreMetric.API_CALL_SUCCESSFUL)).containsExactly(false);
    }

    @Test
    void asyncExecute_withOperationAndAnUnusableTransport_failsTheFutureAndReportsTheFailure() {
        SdkClientConfiguration config = asyncConfig().toBuilder()
                                                     .option(SdkClientOption.ASYNC_CLIENT_TRANSPORT,
                                                             new OtherExchangeAsyncTransport())
                                                     .build();
        AsyncClientHandler handler = asyncHandler(config);
        MetricCollector collector = MetricCollector.create("ApiCall");

        CompletableFuture<TestResponse> future = handler.execute(newPathParams().withMetricCollector(collector));

        assertThatThrownBy(() -> future.get(TIMEOUT_SECONDS, TimeUnit.SECONDS))
            .hasCauseInstanceOf(IllegalStateException.class);
        assertThat(collector.collect().metricValues(CoreMetric.API_CALL_SUCCESSFUL)).containsExactly(false);
    }

    private SdkClientConfiguration syncConfig() {
        return baseHandlerConfig().option(SdkClientOption.CLIENT_TRANSPORT, syncTransport)
                                  .option(SdkClientOption.SYNC_HTTP_CLIENT, httpClient)
                                  .build();
    }

    private SdkClientConfiguration asyncConfig() {
        return baseHandlerConfig().option(SdkClientOption.ASYNC_CLIENT_TRANSPORT, asyncTransport)
                                  .option(SdkClientOption.ASYNC_HTTP_CLIENT, asyncHttpClient)
                                  .option(SdkAdvancedAsyncClientOption.FUTURE_COMPLETION_EXECUTOR, Runnable::run)
                                  .build();
    }

    private SdkClientConfiguration.Builder baseHandlerConfig() {
        List<ExecutionInterceptor> interceptors = new ArrayList<>();
        interceptors.add(interceptor);
        return HttpTestUtils.testClientConfiguration().toBuilder()
                            .option(SdkClientOption.EXECUTION_INTERCEPTORS, interceptors)
                            .option(SdkClientOption.RETRY_STRATEGY, DefaultRetryStrategy.doNotRetry())
                            .option(SdkClientOption.CLIENT_PROTOCOL, protocol);
    }

    private SyncClientHandler syncHandler(SdkClientConfiguration config) {
        SyncClientHandler handler = new SdkSyncClientHandler(config) {
        };
        handlers.add(handler);
        return handler;
    }

    private AsyncClientHandler asyncHandler(SdkClientConfiguration config) {
        AsyncClientHandler handler = new SdkAsyncClientHandler(config);
        handlers.add(handler);
        return handler;
    }

    private static ClientExecutionParams<TestRequest, TestResponse> newPathParams() {
        return new ClientExecutionParams<TestRequest, TestResponse>().withOperation(OPERATION)
                                                                     .withInput(new TestRequest("name"));
    }

    /**
     * A call with a marshaller and response handlers, as generated clients make on the legacy pipeline.
     */
    private static ClientExecutionParams<TestRequest, TestResponse> legacyParams() {
        return new ClientExecutionParams<TestRequest, TestResponse>()
            .withInput(new TestRequest("name"))
            .withOperationName("GetThing")
            .withMarshaller(request -> SdkHttpFullRequest.builder()
                                                         .method(SdkHttpMethod.POST)
                                                         .protocol("http")
                                                         .host("localhost")
                                                         .port(8080)
                                                         .encodedPath("/")
                                                         .build())
            .withResponseHandler((response, attributes) -> TestResponse.builder().value(read(response)).build())
            .withErrorResponseHandler((response, attributes) -> SdkServiceException.builder()
                                                                                   .statusCode(response.statusCode())
                                                                                   .build());
    }

    private static SdkHttpFullResponse response(String body) {
        return SdkHttpFullResponse.builder()
                                  .statusCode(200)
                                  .content(AbortableInputStream.create(
                                      new ByteArrayInputStream(body.getBytes(StandardCharsets.UTF_8))))
                                  .build();
    }

    private static String read(SdkHttpFullResponse response) {
        try {
            return IoUtils.toUtf8String(response.content().get());
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /**
     * An HTTP protocol whose output's value is the response body, or {@code "headers-only"} when the body is left for a
     * streaming transformer ({@link #readBody} false).
     */
    private static final class EchoProtocol extends HttpStubProtocol {
        volatile boolean readBody = true;

        @Override
        @SuppressWarnings("unchecked")
        public <I extends SerializableStruct, O extends SerializableStruct> O deserializeResponse(
            ApiOperation<I, O> operation, ExecutionAttributes context, TypeRegistry errorRegistry,
            SdkHttpFullRequest request, SdkHttpFullResponse response) {
            return (O) TestResponse.builder().value(readBody ? read(response) : "headers-only").build();
        }
    }

    private static final class SyncTransport implements ClientTransport<SdkHttpFullRequest, SdkHttpFullResponse> {
        final List<SdkHttpFullRequest> requests = new CopyOnWriteArrayList<>();

        @Override
        public MessageExchange<SdkHttpFullRequest, SdkHttpFullResponse> messageExchange() {
            return HttpMessageExchange.INSTANCE;
        }

        @Override
        public SdkHttpFullResponse send(ExecutionAttributes context, SdkHttpFullRequest request) {
            requests.add(request);
            return response("from-transport");
        }

        @Override
        public void close() {
        }
    }

    private static final class AsyncTransport implements AsyncClientTransport<SdkHttpFullRequest, SdkHttpFullResponse> {
        final List<SdkHttpFullRequest> requests = new CopyOnWriteArrayList<>();

        @Override
        public MessageExchange<SdkHttpFullRequest, SdkHttpFullResponse> messageExchange() {
            return HttpMessageExchange.INSTANCE;
        }

        @Override
        public CompletableFuture<SdkHttpFullResponse> send(ExecutionAttributes context, SdkHttpFullRequest request,
                                                           AsyncResponseHandoff<SdkHttpFullResponse> streamingHandoff) {
            requests.add(request);
            return CompletableFuture.completedFuture(response("from-transport"));
        }

        @Override
        public void close() {
        }
    }

    /**
     * A transport whose message exchange is not HTTP, which the pipelines reject when they are built.
     */
    private static final class OtherExchangeTransport implements ClientTransport<SdkHttpFullRequest, SdkHttpFullResponse> {
        @Override
        public MessageExchange<SdkHttpFullRequest, SdkHttpFullResponse> messageExchange() {
            return new OtherExchange();
        }

        @Override
        public SdkHttpFullResponse send(ExecutionAttributes context, SdkHttpFullRequest request) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void close() {
        }
    }

    private static final class OtherExchangeAsyncTransport
        implements AsyncClientTransport<SdkHttpFullRequest, SdkHttpFullResponse> {
        @Override
        public MessageExchange<SdkHttpFullRequest, SdkHttpFullResponse> messageExchange() {
            return new OtherExchange();
        }

        @Override
        public CompletableFuture<SdkHttpFullResponse> send(ExecutionAttributes context, SdkHttpFullRequest request,
                                                           AsyncResponseHandoff<SdkHttpFullResponse> streamingHandoff) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void close() {
        }
    }

    private static final class OtherExchange implements MessageExchange<SdkHttpFullRequest, SdkHttpFullResponse> {
    }

    private static final class RecordingHttpClient implements SdkHttpClient {
        final List<HttpExecuteRequest> requests = new CopyOnWriteArrayList<>();

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
        }
    }

    /**
     * An async HTTP client that records each request and fails it, so a legacy call ends without a response.
     */
    private static final class RecordingAsyncHttpClient implements SdkAsyncHttpClient {
        static final String FAILURE = "legacy async HTTP client called";
        final List<AsyncExecuteRequest> requests = new CopyOnWriteArrayList<>();

        @Override
        public CompletableFuture<Void> execute(AsyncExecuteRequest request) {
            requests.add(request);
            IllegalStateException failure = new IllegalStateException(FAILURE);
            request.responseHandler().onError(failure);
            CompletableFuture<Void> result = new CompletableFuture<>();
            result.completeExceptionally(failure);
            return result;
        }

        @Override
        public void close() {
        }
    }

    /**
     * Records the {@link SdkExecutionAttribute#API_OPERATION} each call exposes at {@code beforeExecution}.
     */
    private static final class OperationRecordingInterceptor implements ExecutionInterceptor {
        final List<ApiOperation<?, ?>> operations = new CopyOnWriteArrayList<>();

        @Override
        public void beforeExecution(Context.BeforeExecution context, ExecutionAttributes executionAttributes) {
            operations.add(executionAttributes.getAttribute(SdkExecutionAttribute.API_OPERATION));
        }
    }
}
