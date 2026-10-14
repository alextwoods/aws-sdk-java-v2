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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static software.amazon.awssdk.core.internal.http.ApiCallStagesTestSupport.attributesWithProtocol;

import java.net.URI;
import java.util.function.Consumer;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import software.amazon.awssdk.core.ClientEndpointProvider;
import software.amazon.awssdk.core.http.NoopTestRequest;
import software.amazon.awssdk.core.interceptor.ExecutionAttributes;
import software.amazon.awssdk.core.interceptor.SdkExecutionAttribute;
import software.amazon.awssdk.core.interceptor.SdkInternalExecutionAttribute;
import software.amazon.awssdk.core.internal.http.ApiCallStagesTestSupport.HttpStubProtocol;
import software.amazon.awssdk.core.internal.http.pipeline.stages.EndpointResolutionStage;
import software.amazon.awssdk.core.metrics.CoreMetric;
import software.amazon.awssdk.endpoints.Endpoint;
import software.amazon.awssdk.http.SdkHttpFullRequest;
import software.amazon.awssdk.http.SdkHttpMethod;
import software.amazon.awssdk.metrics.MetricCollector;
import software.amazon.awssdk.metrics.NoOpMetricCollector;

/**
 * Compares {@link EndpointResolution} (applied through {@code updateServiceEndpoint}) with the legacy
 * {@link EndpointResolutionStage} for client endpoint paths, resolved paths and interceptor changes.
 */
class EndpointResolutionTest {

    static Stream<Arguments> cases() {
        return Stream.of(
            Arguments.of("no paths", "https://svc.example.com", "https://r.example.com", noChange()),
            Arguments.of("resolved path", "https://svc.example.com", "https://r.example.com/prefix", noChange()),
            Arguments.of("resolved path with trailing slash", "https://svc.example.com", "https://r.example.com/p/",
                         noChange()),
            Arguments.of("same path", "https://svc.example.com/v1", "https://r.example.com:8443/v1", noChange()),
            Arguments.of("same path with trailing slash", "https://svc.example.com/v1/", "http://r.example.com/v1/",
                         noChange()),
            Arguments.of("different paths", "https://svc.example.com/v1", "https://r.example.com/v2/", noChange()),
            Arguments.of("interceptor path outside the client path", "https://svc.example.com/v1", "https://r.example.com/v1",
                         (Consumer<SdkHttpFullRequest.Builder>) r -> r.encodedPath("/v1x/foo")),
            Arguments.of("interceptor path with the client path elsewhere", "https://svc.example.com/v1",
                         "https://r.example.com/v2", (Consumer<SdkHttpFullRequest.Builder>) r -> r.encodedPath("/a/v1/b")),
            Arguments.of("interceptor host", "https://svc.example.com/v1", "https://r.example.com/v2",
                         (Consumer<SdkHttpFullRequest.Builder>) r -> r.host("moved.example.com")),
            Arguments.of("interceptor port", "https://svc.example.com", "https://r.example.com",
                         (Consumer<SdkHttpFullRequest.Builder>) r -> r.port(9999)),
            Arguments.of("interceptor scheme", "https://svc.example.com", "https://r.example.com/p",
                         (Consumer<SdkHttpFullRequest.Builder>) r -> r.protocol("http"))
        );
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("cases")
    void resolveAndApply_produceTheLegacyStageUri(String name, String clientEndpoint, String resolved,
                                                  Consumer<SdkHttpFullRequest.Builder> interceptorChange) throws Exception {
        URI client = URI.create(clientEndpoint);
        Endpoint endpoint = Endpoint.builder().url(URI.create(resolved)).putHeader("x-e", "1").build();
        SdkHttpFullRequest request = request(client, interceptorChange);

        RequestExecutionContext legacyContext = context(client, endpoint, request);
        SdkHttpFullRequest expected = new EndpointResolutionStage(null).execute(request.toBuilder(), legacyContext).build();

        RequestExecutionContext newContext = context(client, endpoint, request);
        SdkHttpFullRequest.Builder builder = request.toBuilder();
        Endpoint toApply = EndpointResolution.resolve(builder, newContext);
        SdkHttpFullRequest actual = EndpointResolution.apply(builder.build(), toApply, newContext.executionAttributes());

        assertThat(actual.getUri()).isEqualTo(expected.getUri());
        assertThat(actual.headers()).isEqualTo(expected.headers());
        assertThat(newContext.executionAttributes().getAttribute(SdkInternalExecutionAttribute.RESOLVED_ENDPOINT))
            .isSameAs(endpoint);
    }

    @Test
    void resolve_unmodifiedRequest_returnsTheEndpointForTheProtocol() {
        URI client = URI.create("https://svc.example.com");
        Endpoint endpoint = Endpoint.builder().url(URI.create("https://r.example.com/p")).build();
        SdkHttpFullRequest request = request(client, noChange());
        RequestExecutionContext context = context(client, endpoint, request);
        SdkHttpFullRequest.Builder builder = request.toBuilder();

        assertThat(EndpointResolution.resolve(builder, context)).isSameAs(endpoint);
        assertThat(builder.encodedPath()).isEqualTo(ApiCallStagesTestSupport.OPERATION_PATH);
        assertThat(builder.host()).isEqualTo("svc.example.com");
    }

    @Test
    void resolve_skipConditions_leaveTheRequestUnchanged() {
        URI client = URI.create("https://svc.example.com");
        Endpoint endpoint = Endpoint.builder().url(URI.create("https://r.example.com/p")).build();
        SdkHttpFullRequest request = request(client, noChange());
        Stream.<Consumer<ExecutionAttributes>>of(
            a -> a.putAttribute(SdkInternalExecutionAttribute.IS_DISCOVERED_ENDPOINT, true),
            a -> a.putAttribute(SdkInternalExecutionAttribute.SKIP_ENDPOINT_RESOLUTION, true),
            a -> a.putAttribute(SdkInternalExecutionAttribute.RESOLVED_ENDPOINT, endpoint),
            a -> a.putAttribute(SdkInternalExecutionAttribute.ENDPOINT_RESOLVER, null)
        ).forEach(skip -> {
            RequestExecutionContext context = context(client, endpoint, request);
            skip.accept(context.executionAttributes());
            SdkHttpFullRequest.Builder builder = request.toBuilder();

            assertThat(EndpointResolution.resolve(builder, context)).isNull();
            assertThat(builder.build().getUri()).isEqualTo(request.getUri());
        });
    }

    @Test
    void resolve_collectingMetrics_reportsTheResolveDuration() {
        URI client = URI.create("https://svc.example.com");
        Endpoint endpoint = Endpoint.builder().url(URI.create("https://r.example.com")).build();
        SdkHttpFullRequest request = request(client, noChange());
        MetricCollector collector = mock(MetricCollector.class);
        RequestExecutionContext context = context(client, endpoint, request);
        context.executionAttributes().putAttribute(SdkExecutionAttribute.API_CALL_METRIC_COLLECTOR, collector);

        EndpointResolution.resolve(request.toBuilder(), context);

        verify(collector).reportMetric(eq(CoreMetric.ENDPOINT_RESOLVE_DURATION), any());
    }

    @Test
    void resolve_notCollectingMetrics_resolvesWithoutMeasuring() {
        URI client = URI.create("https://svc.example.com");
        Endpoint endpoint = Endpoint.builder().url(URI.create("https://r.example.com")).build();
        SdkHttpFullRequest request = request(client, noChange());
        RequestExecutionContext context = context(client, endpoint, request);
        context.executionAttributes().putAttribute(SdkExecutionAttribute.API_CALL_METRIC_COLLECTOR,
                                                   NoOpMetricCollector.create());

        assertThat(EndpointResolution.resolve(request.toBuilder(), context)).isSameAs(endpoint);
    }

    @Test
    void apply_nullEndpoint_returnsTheRequest() {
        SdkHttpFullRequest request = request(URI.create("https://svc.example.com"), noChange());

        assertThat(EndpointResolution.apply(request, null, new ExecutionAttributes())).isSameAs(request);
    }

    private static Consumer<SdkHttpFullRequest.Builder> noChange() {
        return r -> { };
    }

    private static SdkHttpFullRequest request(URI client, Consumer<SdkHttpFullRequest.Builder> interceptorChange) {
        SdkHttpFullRequest.Builder builder = ApiCallStagesTestSupport.marshalledRequest(client, null).toBuilder()
                                                                     .method(SdkHttpMethod.GET);
        interceptorChange.accept(builder);
        return builder.build();
    }

    private static RequestExecutionContext context(URI client, Endpoint endpoint, SdkHttpFullRequest request) {
        ExecutionAttributes attributes = attributesWithProtocol(new HttpStubProtocol());
        attributes.putAttribute(SdkInternalExecutionAttribute.ENDPOINT_RESOLVER, (r, a) -> endpoint);
        attributes.putAttribute(SdkInternalExecutionAttribute.CLIENT_ENDPOINT_PROVIDER,
                                ClientEndpointProvider.create(client, false));
        attributes.putAttribute(SdkInternalExecutionAttribute.HTTP_REQUEST_URI_BEFORE_MODIFY,
                                ApiCallStagesTestSupport.marshalledRequest(client, null).getUri());
        return ApiCallStagesTestSupport.context(NoopTestRequest.builder().build(), attributes, request);
    }
}
