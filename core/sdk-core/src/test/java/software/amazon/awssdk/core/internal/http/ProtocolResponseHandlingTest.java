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
import static software.amazon.awssdk.core.internal.http.ProtocolStepsTestSupport.executionContext;
import static software.amazon.awssdk.core.internal.http.ProtocolStepsTestSupport.response;
import static software.amazon.awssdk.core.internal.http.ProtocolStepsTestSupport.serializedRequest;

import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.core.Response;
import software.amazon.awssdk.core.exception.NonRetryableException;
import software.amazon.awssdk.core.exception.RetryableException;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.core.exception.SdkServiceException;
import software.amazon.awssdk.core.http.ExecutionContext;
import software.amazon.awssdk.core.interceptor.SdkExecutionAttribute;
import software.amazon.awssdk.core.internal.http.ProtocolStepsTestSupport.RecordingInterceptor;
import software.amazon.awssdk.core.internal.http.ProtocolStepsTestSupport.StubProtocol;
import software.amazon.awssdk.core.internal.http.ProtocolStepsTestSupport.TestRequest;
import software.amazon.awssdk.core.internal.http.ProtocolStepsTestSupport.TestResponse;
import software.amazon.awssdk.core.internal.http.ProtocolStepsTestSupport.TrackingStream;
import software.amazon.awssdk.core.metrics.CoreMetric;
import software.amazon.awssdk.core.sync.ResponseTransformer;
import software.amazon.awssdk.http.AbortableInputStream;
import software.amazon.awssdk.http.SdkHttpFullRequest;
import software.amazon.awssdk.http.SdkHttpFullResponse;
import software.amazon.awssdk.metrics.MetricCollector;
import software.amazon.awssdk.schema.serde.TypeRegistry;
import software.amazon.awssdk.utils.IoUtils;

class ProtocolResponseHandlingTest {
    private static final TypeRegistry ERRORS = TypeRegistry.empty();
    private static final SdkHttpFullRequest REQUEST = serializedRequest(ProtocolMarshalling.UNRESOLVED_URI, "{}");

    @Test
    void handle_withSuccessResponse_returnsProtocolOutputWithoutCopying() {
        StubProtocol protocol = new StubProtocol();
        AtomicReference<TestResponse> built = new AtomicReference<>();
        protocol.deserialize = (op, attrs, registry, request, response) -> {
            built.set(builtLikeHttpClientProtocol(response, "v"));
            return built.get();
        };
        ExecutionContext context = executionContext(protocol, new TestRequest("a"), null);
        TrackingStream body = new TrackingStream("payload");
        SdkHttpFullResponse httpResponse = response(200, body);

        Response<TestResponse> result =
            ProtocolResponseHandling.handle(context, OPERATION, ERRORS, REQUEST, httpResponse);

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.httpResponse()).isSameAs(httpResponse);
        assertThat(result.response()).isSameAs(built.get());
        assertThat(result.response().sdkHttpResponse()).isSameAs(httpResponse);
        assertThat(result.response().copies()).isZero();
        assertThat(protocol.operation).isSameAs(OPERATION);
        assertThat(protocol.context).isSameAs(context.executionAttributes());
        assertThat(protocol.errorRegistry).isSameAs(ERRORS);
        assertThat(protocol.request).isSameAs(REQUEST);
        assertThat(protocol.response).isSameAs(httpResponse);
        assertThat(context.interceptorContext().response()).isSameAs(built.get());
        assertThat(body.closed()).isTrue();
    }

    @Test
    void handle_whenProtocolDoesNotSetHttpResponse_attachesIt() {
        StubProtocol protocol = new StubProtocol();
        protocol.deserialize = (op, attrs, registry, request, response) -> TestResponse.builder().value("v").build();
        SdkHttpFullResponse httpResponse = response(200, new TrackingStream(""));

        Response<TestResponse> result = ProtocolResponseHandling.handle(
            executionContext(protocol, new TestRequest("a"), null), OPERATION, ERRORS, REQUEST, httpResponse);

        assertThat(result.response().sdkHttpResponse()).isSameAs(httpResponse);
        assertThat(result.response().value()).isEqualTo("v");
        assertThat(result.response().copies()).isEqualTo(1);
    }

    @Test
    void handle_withAttemptMetricCollector_reportsUnmarshallingDuration() {
        StubProtocol protocol = new StubProtocol();
        protocol.deserialize = (op, attrs, registry, request, response) -> builtLikeHttpClientProtocol(response, "v");
        ExecutionContext context = executionContext(protocol, new TestRequest("a"), null);
        MetricCollector attempt = MetricCollector.create("ApiCallAttempt");
        context.executionAttributes().putAttribute(SdkExecutionAttribute.API_CALL_ATTEMPT_METRIC_COLLECTOR, attempt);

        ProtocolResponseHandling.handle(context, OPERATION, ERRORS, REQUEST, response(200, new TrackingStream("")));

        assertThat(attempt.collect().metricValues(CoreMetric.UNMARSHALLING_DURATION)).hasSize(1);
    }

    @Test
    void handle_whenProtocolThrowsServiceException_returnsFailureWithoutRunningInterceptors() {
        StubProtocol protocol = new StubProtocol();
        SdkServiceException error = SdkServiceException.builder().message("throttled").statusCode(400).build();
        protocol.deserialize = (op, attrs, registry, request, response) -> {
            throw error;
        };
        RecordingInterceptor interceptor = new RecordingInterceptor();
        TrackingStream body = new TrackingStream("{\"__type\":\"Throttled\"}");
        SdkHttpFullResponse httpResponse = response(400, body);

        Response<TestResponse> result = ProtocolResponseHandling.handle(
            executionContext(protocol, new TestRequest("a"), null, interceptor), OPERATION, ERRORS, REQUEST, httpResponse);

        assertThat(result.isSuccess()).isFalse();
        assertThat(result.exception()).isSameAs(error);
        assertThat(result.httpResponse()).isSameAs(httpResponse);
        assertThat(result.response()).isNull();
        assertThat(interceptor.hooks).isEmpty();
        assertThat(body.closed()).isTrue();
    }

    @Test
    void handle_whenProtocolThrowsClientException_propagatesIt() {
        StubProtocol protocol = new StubProtocol();
        SdkClientException failure = SdkClientException.create("Unable to unmarshall GetThing: bad body");
        protocol.deserialize = (op, attrs, registry, request, response) -> {
            throw failure;
        };
        RecordingInterceptor interceptor = new RecordingInterceptor();
        TrackingStream body = new TrackingStream("not cbor");

        assertThatThrownBy(() -> ProtocolResponseHandling.handle(
            executionContext(protocol, new TestRequest("a"), null, interceptor), OPERATION, ERRORS, REQUEST,
            response(200, body)))
            .isSameAs(failure);
        assertThat(interceptor.hooks).isEmpty();
        assertThat(body.closed()).isTrue();
    }

    @Test
    void handle_whenProtocolThrowsNonSdkException_wrapsItLikeTheLegacyHandler() {
        StubProtocol protocol = new StubProtocol();
        IllegalStateException failure = new IllegalStateException("boom");
        protocol.deserialize = (op, attrs, registry, request, response) -> {
            throw failure;
        };

        assertThatThrownBy(() -> ProtocolResponseHandling.handle(
            executionContext(protocol, new TestRequest("a"), null), OPERATION, ERRORS, REQUEST,
            response(200, new TrackingStream(""))))
            .isExactlyInstanceOf(SdkClientException.class)
            .hasMessage("Unable to unmarshall response (boom). Response Code: 200, Response Text: null")
            .hasCause(failure);
        assertThatThrownBy(() -> ProtocolResponseHandling.handle(
            executionContext(protocol, new TestRequest("a"), null), OPERATION, ERRORS, REQUEST,
            response(500, new TrackingStream(""))))
            .isExactlyInstanceOf(SdkClientException.class)
            .hasMessageStartingWith("Unable to unmarshall error response (boom). Response Code: 500")
            .hasCause(failure);
    }

    @Test
    void handle_withInterceptors_runsAfterUnmarshallingThenModifyResponse() {
        StubProtocol protocol = new StubProtocol();
        RecordingInterceptor interceptor = new RecordingInterceptor();
        AtomicReference<TestResponse> built = new AtomicReference<>();
        protocol.deserialize = (op, attrs, registry, request, response) -> {
            interceptor.hooks.add("deserializeResponse");
            built.set(builtLikeHttpClientProtocol(response, "v"));
            return built.get();
        };
        TestResponse replacement = TestResponse.builder().value("replaced").build();
        interceptor.replacement = replacement;
        ExecutionContext context = executionContext(protocol, new TestRequest("a"), null, interceptor);
        SdkHttpFullResponse httpResponse = response(200, new TrackingStream(""));

        Response<TestResponse> result = ProtocolResponseHandling.handle(context, OPERATION, ERRORS, REQUEST, httpResponse);

        assertThat(interceptor.hooks).containsExactly("deserializeResponse", "afterUnmarshalling", "modifyResponse");
        assertThat(interceptor.attributes).allMatch(a -> a == context.executionAttributes());
        assertThat(interceptor.afterUnmarshalling.get(0).response()).isSameAs(built.get());
        assertThat(interceptor.modifyResponse.get(0).response()).isSameAs(built.get());
        assertThat(result.response().value()).isEqualTo("replaced");
        assertThat(result.response().sdkHttpResponse()).isSameAs(httpResponse);
        assertThat(context.interceptorContext().response()).isSameAs(replacement);
    }

    @Test
    void handle_whenInterceptorFails_wrapsItLikeTheLegacyHandler() {
        StubProtocol protocol = new StubProtocol();
        protocol.deserialize = (op, attrs, registry, request, response) -> builtLikeHttpClientProtocol(response, "v");
        RecordingInterceptor interceptor = new RecordingInterceptor();
        IllegalArgumentException failure = new IllegalArgumentException("interceptor failed");
        interceptor.afterUnmarshallingFailure = failure;
        TrackingStream body = new TrackingStream("");

        assertThatThrownBy(() -> ProtocolResponseHandling.handle(
            executionContext(protocol, new TestRequest("a"), null, interceptor), OPERATION, ERRORS, REQUEST,
            response(200, body)))
            .isExactlyInstanceOf(SdkClientException.class)
            .hasMessageStartingWith("Unable to unmarshall response (interceptor failed)")
            .hasCause(failure);
        assertThat(body.closed()).isTrue();
    }

    @Test
    void handle_whenInterceptorThrowsRetryableException_propagatesIt() {
        StubProtocol protocol = new StubProtocol();
        protocol.deserialize = (op, attrs, registry, request, response) -> builtLikeHttpClientProtocol(response, "v");
        RecordingInterceptor interceptor = new RecordingInterceptor();
        RetryableException failure = RetryableException.create("retry me");
        interceptor.afterUnmarshallingFailure = failure;

        assertThatThrownBy(() -> ProtocolResponseHandling.handle(
            executionContext(protocol, new TestRequest("a"), null, interceptor), OPERATION, ERRORS, REQUEST,
            response(200, new TrackingStream(""))))
            .isSameAs(failure);
    }

    @Test
    void handleStreaming_withSuccessResponse_handsOutputAndBodyToTransformer() throws Exception {
        StubProtocol protocol = new StubProtocol();
        protocol.deserialize = (op, attrs, registry, request, response) -> builtLikeHttpClientProtocol(response, "meta");
        RecordingInterceptor interceptor = new RecordingInterceptor();
        TrackingStream body = new TrackingStream("streamed bytes");
        SdkHttpFullResponse httpResponse = response(200, body);
        AtomicReference<TestResponse> seen = new AtomicReference<>();
        ResponseTransformer<TestResponse, String> transformer = (output, stream) -> {
            interceptor.hooks.add("transform");
            seen.set(output);
            return new String(IoUtils.toByteArray(stream), StandardCharsets.UTF_8);
        };

        Response<String> result = ProtocolResponseHandling.handleStreaming(
            executionContext(protocol, new TestRequest("a"), null, interceptor), OPERATION, ERRORS, REQUEST,
            httpResponse, transformer);

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.response()).isEqualTo("streamed bytes");
        assertThat(interceptor.hooks).containsExactly("afterUnmarshalling", "modifyResponse", "transform");
        assertThat(seen.get().value()).isEqualTo("meta");
        assertThat(seen.get().sdkHttpResponse()).isSameAs(httpResponse);
        assertThat(body.closed()).isTrue();
    }

    @Test
    void handleStreaming_whenTransformerNeedsConnectionLeftOpen_leavesBodyOpen() throws Exception {
        StubProtocol protocol = new StubProtocol();
        protocol.deserialize = (op, attrs, registry, request, response) -> builtLikeHttpClientProtocol(response, "v");
        TrackingStream body = new TrackingStream("x");
        ResponseTransformer<TestResponse, String> transformer = new ResponseTransformer<TestResponse, String>() {
            @Override
            public String transform(TestResponse response, AbortableInputStream stream) {
                return "kept";
            }

            @Override
            public boolean needsConnectionLeftOpen() {
                return true;
            }
        };

        Response<String> result = ProtocolResponseHandling.handleStreaming(
            executionContext(protocol, new TestRequest("a"), null), OPERATION, ERRORS, REQUEST, response(200, body),
            transformer);

        assertThat(result.response()).isEqualTo("kept");
        assertThat(body.closed()).isFalse();
    }

    @Test
    void handleStreaming_withErrorResponse_returnsFailureWithoutCallingTransformer() throws Exception {
        StubProtocol protocol = new StubProtocol();
        SdkServiceException error = SdkServiceException.builder().message("missing").statusCode(404).build();
        protocol.deserialize = (op, attrs, registry, request, response) -> {
            throw error;
        };
        TrackingStream body = new TrackingStream("error body");
        ResponseTransformer<TestResponse, String> transformer = (output, stream) -> {
            throw new AssertionError("The transformer must not see an error body");
        };

        Response<String> result = ProtocolResponseHandling.handleStreaming(
            executionContext(protocol, new TestRequest("a"), null), OPERATION, ERRORS, REQUEST, response(404, body),
            transformer);

        assertThat(result.isSuccess()).isFalse();
        assertThat(result.exception()).isSameAs(error);
        assertThat(body.closed()).isTrue();
    }

    @Test
    void handleStreaming_whenTransformerFails_reportsItLikeTheLegacyHandler() {
        StubProtocol protocol = new StubProtocol();
        protocol.deserialize = (op, attrs, registry, request, response) -> builtLikeHttpClientProtocol(response, "v");
        IllegalStateException failure = new IllegalStateException("disk full");
        ResponseTransformer<TestResponse, String> transformer = (output, stream) -> {
            throw failure;
        };
        TrackingStream body = new TrackingStream("x");

        assertThatThrownBy(() -> ProtocolResponseHandling.handleStreaming(
            executionContext(protocol, new TestRequest("a"), null), OPERATION, ERRORS, REQUEST, response(200, body),
            transformer))
            .isExactlyInstanceOf(SdkClientException.class)
            .hasCauseInstanceOf(NonRetryableException.class)
            .hasRootCause(failure);
        assertThat(body.closed()).isTrue();
    }

    /**
     * Builds the output the way {@code HttpClientProtocol.buildOutput} does: the HTTP response is set on the builder.
     */
    private static TestResponse builtLikeHttpClientProtocol(SdkHttpFullResponse response, String value) {
        return (TestResponse) TestResponse.builder().value(value).sdkHttpResponse(response).build();
    }
}
