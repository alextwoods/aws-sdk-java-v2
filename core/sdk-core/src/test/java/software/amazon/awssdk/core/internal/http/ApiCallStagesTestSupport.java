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

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import software.amazon.awssdk.codecs.commons.Codec;
import software.amazon.awssdk.core.ClientEndpointProvider;
import software.amazon.awssdk.core.ClientType;
import software.amazon.awssdk.core.SdkRequest;
import software.amazon.awssdk.core.client.config.SdkClientConfiguration;
import software.amazon.awssdk.core.client.config.SdkClientOption;
import software.amazon.awssdk.core.client.protocol.ClientProtocol;
import software.amazon.awssdk.core.client.protocol.HttpMessageExchange;
import software.amazon.awssdk.core.client.protocol.MessageExchange;
import software.amazon.awssdk.core.http.ExecutionContext;
import software.amazon.awssdk.core.interceptor.ExecutionAttributes;
import software.amazon.awssdk.core.interceptor.ExecutionInterceptor;
import software.amazon.awssdk.core.interceptor.ExecutionInterceptorChain;
import software.amazon.awssdk.core.interceptor.InterceptorContext;
import software.amazon.awssdk.core.interceptor.SdkExecutionAttribute;
import software.amazon.awssdk.core.internal.http.pipeline.stages.ApplyTransactionIdStage;
import software.amazon.awssdk.core.internal.http.pipeline.stages.ApplyUserAgentStage;
import software.amazon.awssdk.core.internal.http.pipeline.stages.AuthSchemeResolutionStage;
import software.amazon.awssdk.core.internal.http.pipeline.stages.CompressRequestStage;
import software.amazon.awssdk.core.internal.http.pipeline.stages.EndpointResolutionStage;
import software.amazon.awssdk.core.internal.http.pipeline.stages.HttpChecksumStage;
import software.amazon.awssdk.core.internal.http.pipeline.stages.MakeRequestImmutableStage;
import software.amazon.awssdk.core.internal.http.pipeline.stages.MakeRequestMutableStage;
import software.amazon.awssdk.core.internal.http.pipeline.stages.MergeCustomHeadersStage;
import software.amazon.awssdk.core.internal.http.pipeline.stages.MergeCustomQueryParamsStage;
import software.amazon.awssdk.core.internal.http.pipeline.stages.QueryParametersToBodyStage;
import software.amazon.awssdk.core.internal.http.pipeline.stages.utils.RetryableStageHelper;
import software.amazon.awssdk.core.internal.retry.SdkDefaultRetryStrategy;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.endpoints.Endpoint;
import software.amazon.awssdk.http.ContentStreamProvider;
import software.amazon.awssdk.http.SdkHttpFullRequest;
import software.amazon.awssdk.http.SdkHttpFullResponse;
import software.amazon.awssdk.http.SdkHttpMethod;
import software.amazon.awssdk.metrics.MetricCollector;
import software.amazon.awssdk.retries.api.BackoffStrategy;
import software.amazon.awssdk.schema.ApiOperation;
import software.amazon.awssdk.schema.SerializableStruct;
import software.amazon.awssdk.schema.ShapeId;
import software.amazon.awssdk.schema.serde.TypeRegistry;
import software.amazon.awssdk.utils.IoUtils;
import software.amazon.awssdk.utils.http.SdkHttpUtils;

/**
 * Shared fixtures for the tests of the new pipeline path's shared stages: a client configuration, call contexts, an HTTP
 * protocol stub whose {@code updateServiceEndpoint} joins paths as {@code HttpClientProtocol} does, the legacy mutation
 * stage chain to compare with, and request comparison.
 */
final class ApiCallStagesTestSupport {
    static final URI CLIENT_ENDPOINT = URI.create("https://svc.us-east-1.amazonaws.com");
    static final String OPERATION_PATH = "/service/ThingService/operation/GetThing";
    static final String CLIENT_USER_AGENT = "aws-sdk-java/2.0.0 test-client";
    static final String UUID_PATTERN = "[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}";

    private ApiCallStagesTestSupport() {
    }

    /**
     * A client configuration with a standard retry strategy (3 attempts, no backoff), a client endpoint and a client user
     * agent.
     */
    static SdkClientConfiguration.Builder baseConfig() {
        return SdkClientConfiguration.builder()
                                     .option(SdkClientOption.RETRY_STRATEGY,
                                             SdkDefaultRetryStrategy.standardRetryStrategyBuilder()
                                                                    .maxAttempts(3)
                                                                    .backoffStrategy(BackoffStrategy.retryImmediately())
                                                                    .build())
                                     .option(SdkClientOption.CLIENT_ENDPOINT_PROVIDER,
                                             ClientEndpointProvider.create(CLIENT_ENDPOINT, false))
                                     .option(SdkClientOption.ADDITIONAL_HTTP_HEADERS, Collections.emptyMap())
                                     .option(SdkClientOption.CLIENT_USER_AGENT, CLIENT_USER_AGENT);
    }

    static HttpClientDependencies dependencies(SdkClientConfiguration configuration) {
        return HttpClientDependencies.builder().clientConfiguration(configuration).build();
    }

    /**
     * A request as a legacy marshaller produces it: addressed to the endpoint, with the operation path after the
     * endpoint path.
     */
    static SdkHttpFullRequest marshalledRequest(URI endpoint, String body) {
        SdkHttpFullRequest.Builder builder = SdkHttpFullRequest.builder()
                                                               .method(SdkHttpMethod.POST)
                                                               .uri(endpoint)
                                                               .encodedPath(SdkHttpUtils.appendUri(endpoint.getRawPath(),
                                                                                                   OPERATION_PATH))
                                                               .putHeader("Content-Type", "application/cbor");
        if (body != null) {
            byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
            builder.putHeader("Content-Length", String.valueOf(bytes.length))
                   .contentStreamProvider(() -> new ByteArrayInputStream(bytes));
        }
        return builder.build();
    }

    /**
     * A call's context after {@code modifyHttpRequest}: the interceptor context holds the SDK request, the HTTP request
     * and its body.
     */
    static RequestExecutionContext context(SdkRequest request,
                                           ExecutionAttributes attributes,
                                           SdkHttpFullRequest httpRequest,
                                           ExecutionInterceptor... interceptors) {
        InterceptorContext.Builder interceptorContext = InterceptorContext.builder().request(request).httpRequest(httpRequest);
        httpRequest.contentStreamProvider().ifPresent(
            content -> interceptorContext.requestBody(RequestBody.fromContentProvider(
                content, Long.parseLong(httpRequest.firstMatchingHeader("Content-Length").orElse("0")),
                httpRequest.firstMatchingHeader("Content-Type").orElse(""))));
        ExecutionContext executionContext =
            ExecutionContext.builder()
                            .executionAttributes(attributes)
                            .interceptorChain(new ExecutionInterceptorChain(new ArrayList<>(Arrays.asList(interceptors))))
                            .interceptorContext(interceptorContext.build())
                            .metricCollector(MetricCollector.create("ApiCall"))
                            .build();
        return RequestExecutionContext.builder().originalRequest(request).executionContext(executionContext).build();
    }

    /**
     * Runs the legacy mutation stages in the order {@code AmazonSyncHttpClient} composes them, and returns the first
     * attempt's request as {@link RetryableStageHelper#requestToSend()} creates it.
     */
    static SdkHttpFullRequest legacyMutation(SdkHttpFullRequest request,
                                             RequestExecutionContext context,
                                             HttpClientDependencies dependencies,
                                             ClientType clientType) throws Exception {
        SdkHttpFullRequest.Builder builder = new MakeRequestMutableStage().execute(request, context);
        builder = new ApplyTransactionIdStage().execute(builder, context);
        builder = new MergeCustomHeadersStage(dependencies).execute(builder, context);
        builder = new MergeCustomQueryParamsStage().execute(builder, context);
        builder = new QueryParametersToBodyStage().execute(builder, context);
        builder = new CompressRequestStage(dependencies).execute(builder, context);
        builder = new AuthSchemeResolutionStage(dependencies).execute(builder, context);
        builder = new EndpointResolutionStage(dependencies).execute(builder, context);
        builder = new HttpChecksumStage(clientType).execute(builder, context);
        builder = new ApplyUserAgentStage(dependencies).execute(builder, context);
        SdkHttpFullRequest immutable = new MakeRequestImmutableStage().execute(builder, context);
        RetryableStageHelper helper = new RetryableStageHelper(immutable, context, dependencies);
        helper.startingAttempt();
        return helper.requestToSend();
    }

    /**
     * Asserts that two requests have the same method, URI, query parameters, headers and body. The invocation ID is
     * random, so only its format is compared.
     */
    static void assertSameRequest(SdkHttpFullRequest actual, SdkHttpFullRequest expected) {
        assertThat(actual.method()).isEqualTo(expected.method());
        assertThat(actual.getUri()).isEqualTo(expected.getUri());
        assertThat(actual.encodedPath()).isEqualTo(expected.encodedPath());
        assertThat(actual.rawQueryParameters()).isEqualTo(expected.rawQueryParameters());
        assertThat(withoutInvocationId(actual.headers())).isEqualTo(withoutInvocationId(expected.headers()));
        assertThat(actual.firstMatchingHeader(ApplyTransactionIdStage.HEADER_SDK_TRANSACTION_ID))
            .hasValueSatisfying(id -> assertThat(id).matches(UUID_PATTERN));
        assertThat(expected.firstMatchingHeader(ApplyTransactionIdStage.HEADER_SDK_TRANSACTION_ID))
            .hasValueSatisfying(id -> assertThat(id).matches(UUID_PATTERN));
        assertThat(body(actual)).isEqualTo(body(expected));
    }

    static String body(SdkHttpFullRequest request) {
        ContentStreamProvider provider = request.contentStreamProvider().orElse(null);
        if (provider == null) {
            return null;
        }
        try (InputStream stream = provider.newStream()) {
            return IoUtils.toUtf8String(stream);
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    private static Map<String, List<String>> withoutInvocationId(Map<String, List<String>> headers) {
        Map<String, List<String>> result = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        result.putAll(headers);
        result.remove(ApplyTransactionIdStage.HEADER_SDK_TRANSACTION_ID);
        return result;
    }

    /**
     * An HTTP protocol that serializes with a function set by the test and moves a request to an endpoint as
     * {@code HttpClientProtocol.updateServiceEndpoint} does.
     */
    static class HttpStubProtocol implements ClientProtocol<SdkHttpFullRequest, SdkHttpFullResponse> {
        java.util.function.Function<URI, SdkHttpFullRequest> serialize = endpoint -> {
            SdkHttpFullRequest.Builder builder = marshalledRequest(endpoint, "{}").toBuilder();
            return builder.encodedPath(OPERATION_PATH).build();
        };
        final List<Endpoint> updatedEndpoints = new ArrayList<>();

        @Override
        public ShapeId id() {
            return ShapeId.from("com.example.test#httpStub");
        }

        @Override
        public Codec payloadCodec() {
            return null;
        }

        @Override
        public MessageExchange<SdkHttpFullRequest, SdkHttpFullResponse> messageExchange() {
            return HttpMessageExchange.INSTANCE;
        }

        @Override
        public <I extends SerializableStruct, O extends SerializableStruct> SdkHttpFullRequest serializeRequest(
            ApiOperation<I, O> operation, I input, ExecutionAttributes context, URI endpoint) {
            return serialize.apply(endpoint);
        }

        @Override
        public SdkHttpFullRequest updateServiceEndpoint(SdkHttpFullRequest request, Endpoint endpoint) {
            updatedEndpoints.add(endpoint);
            URI url = endpoint.url();
            String endpointPath = url.getRawPath() == null ? "" : url.getRawPath();
            return request.toBuilder()
                          .protocol(url.getScheme())
                          .host(url.getHost())
                          .port(url.getPort())
                          .encodedPath(SdkHttpUtils.appendUri(endpointPath, request.encodedPath()))
                          .build();
        }

        @Override
        public <I extends SerializableStruct, O extends SerializableStruct> O deserializeResponse(
            ApiOperation<I, O> operation, ExecutionAttributes context, TypeRegistry errorRegistry,
            SdkHttpFullRequest request, SdkHttpFullResponse response) {
            throw new UnsupportedOperationException();
        }
    }

    /**
     * Attributes holding the protocol, as the new path's attributes do.
     */
    static ExecutionAttributes attributesWithProtocol(ClientProtocol<?, ?> protocol) {
        ExecutionAttributes attributes = new ExecutionAttributes();
        attributes.putAttribute(SdkExecutionAttribute.CLIENT_PROTOCOL, protocol);
        return attributes;
    }
}
