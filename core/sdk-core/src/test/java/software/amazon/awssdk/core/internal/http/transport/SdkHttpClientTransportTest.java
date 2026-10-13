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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.codecs.commons.ByteBufferOutputStream;
import software.amazon.awssdk.core.client.protocol.HttpMessageExchange;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.core.interceptor.ExecutionAttributes;
import software.amazon.awssdk.core.interceptor.SdkInternalExecutionAttribute;
import software.amazon.awssdk.core.internal.InternalCoreExecutionAttribute;
import software.amazon.awssdk.core.internal.metrics.RequestBodyMetrics;
import software.amazon.awssdk.core.internal.retry.SdkDefaultRetrySetting;
import software.amazon.awssdk.core.retry.RetryPolicyContext;
import software.amazon.awssdk.core.retry.conditions.RetryOnExceptionsCondition;
import software.amazon.awssdk.http.Abortable;
import software.amazon.awssdk.http.AbortableInputStream;
import software.amazon.awssdk.http.ContentStreamProvider;
import software.amazon.awssdk.http.ExecutableHttpRequest;
import software.amazon.awssdk.http.HttpExecuteRequest;
import software.amazon.awssdk.http.HttpExecuteResponse;
import software.amazon.awssdk.http.SdkHttpClient;
import software.amazon.awssdk.http.SdkHttpFullRequest;
import software.amazon.awssdk.http.SdkHttpFullResponse;
import software.amazon.awssdk.http.SdkHttpMethod;
import software.amazon.awssdk.http.SdkHttpResponse;
import software.amazon.awssdk.metrics.MetricCollector;
import software.amazon.awssdk.utils.IoUtils;

class SdkHttpClientTransportTest {
    private static final byte[] BODY = "{\"hello\":\"world\"}".getBytes(StandardCharsets.UTF_8);

    @Test
    void messageExchange_isHttp() {
        assertThat(SdkHttpClientTransport.create(new RecordingHttpClient(), false).messageExchange())
            .isSameAs(HttpMessageExchange.INSTANCE);
    }

    @Test
    void send_passesMethodUriHeadersAndBodyThroughUnchanged() throws IOException {
        RecordingHttpClient client = new RecordingHttpClient();
        ContentStreamProvider body = ContentStreamProvider.fromByteArrayUnsafe(BODY);
        SdkHttpFullRequest request = request().contentStreamProvider(body).build();

        SdkHttpClientTransport.create(client, false).send(new ExecutionAttributes(), request);

        HttpExecuteRequest sent = client.lastRequest;
        assertThat(sent.httpRequest()).isSameAs(request);
        assertThat(sent.httpRequest().method()).isEqualTo(SdkHttpMethod.POST);
        assertThat(sent.httpRequest().getUri()).isEqualTo(URI.create("https://example.com/a%2Fb/c?x=1&x=2&y"));
        assertThat(sent.httpRequest().headers()).isEqualTo(request.headers());
        assertThat(sent.contentStreamProvider()).containsSame(body);
        assertThat(IoUtils.toByteArray(sent.contentStreamProvider().get().newStream())).isEqualTo(BODY);
    }

    @Test
    void send_withoutBody_sendsNoBody() {
        RecordingHttpClient client = new RecordingHttpClient();
        SdkHttpFullRequest request = request().method(SdkHttpMethod.GET).build();

        SdkHttpClientTransport.create(client, false).send(new ExecutionAttributes(), request);

        assertThat(client.lastRequest.contentStreamProvider()).isEmpty();
    }

    @Test
    void send_singleBufferBody_reachesTheClientWithoutACopy() {
        RecordingHttpClient client = new RecordingHttpClient();
        ByteBufferOutputStream out = new ByteBufferOutputStream(64);
        out.write(BODY, 0, BODY.length);
        ContentStreamProvider body = out.toContentStreamProvider();
        ByteBuffer original = body.contentAsByteBufferOrNull();
        SdkHttpFullRequest request = request().putHeader("Content-Length", String.valueOf(BODY.length))
                                              .contentStreamProvider(body)
                                              .build();

        SdkHttpClientTransport.create(client, false).send(new ExecutionAttributes(), request);

        ContentStreamProvider sent = client.lastRequest.contentStreamProvider().get();
        assertThat(sent).isSameAs(body);
        ByteBuffer sentBuffer = sent.contentAsByteBufferOrNull();
        assertThat(sentBuffer.hasArray()).isTrue();
        assertThat(sentBuffer.isReadOnly()).isFalse();
        assertThat(sentBuffer.array()).isSameAs(original.array()).isSameAs(out.array());
        assertThat(sentBuffer.arrayOffset() + sentBuffer.position()).isEqualTo(original.arrayOffset() + original.position());
        assertThat(sentBuffer.remaining()).isEqualTo(BODY.length);
    }

    @Test
    void send_streamOnlyBody_sendsTheSameBytesThroughTheLengthCheckingWrapper() throws IOException {
        RecordingHttpClient client = new RecordingHttpClient();
        ContentStreamProvider streamOnly = () -> new ByteArrayInputStream(BODY);
        SdkHttpFullRequest request = request().putHeader("Content-Length", String.valueOf(BODY.length))
                                              .contentStreamProvider(streamOnly)
                                              .build();

        SdkHttpClientTransport.create(client, false).send(new ExecutionAttributes(), request);

        ContentStreamProvider sent = client.lastRequest.contentStreamProvider().get();
        assertThat(sent.contentAsByteBufferOrNull()).isNull();
        assertThat(IoUtils.toByteArray(sent.newStream())).isEqualTo(BODY);
    }

    @Test
    void send_bufferShorterThanContentLength_failsAsTheLegacyLengthCheckDoes() {
        RecordingHttpClient client = new RecordingHttpClient();
        client.readBody = true;
        SdkHttpFullRequest request = request().putHeader("Content-Length", String.valueOf(BODY.length + 1))
                                              .contentStreamProvider(ContentStreamProvider.fromByteArrayUnsafe(BODY))
                                              .build();

        SdkHttpClientTransport transport = SdkHttpClientTransport.create(client, false);

        assertThatThrownBy(() -> transport.send(new ExecutionAttributes(), request))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("fewer bytes than the specified content-length");
        assertThat(client.lastRequest.contentStreamProvider().get().contentAsByteBufferOrNull()).isNull();
    }

    @Test
    void send_returnsStatusHeadersAndBodyStreamUnchanged() {
        RecordingHttpClient client = new RecordingHttpClient();
        AbortableInputStream responseBody = AbortableInputStream.create(new ByteArrayInputStream(BODY));
        client.response = HttpExecuteResponse.builder()
                                             .response(SdkHttpResponse.builder()
                                                                      .statusCode(503)
                                                                      .statusText("Slow Down")
                                                                      .putHeader("x-amzn-RequestId", "id")
                                                                      .putHeader("a", Arrays.asList("1", "2"))
                                                                      .build())
                                             .responseBody(responseBody)
                                             .build();

        SdkHttpFullResponse response =
            SdkHttpClientTransport.create(client, false).send(new ExecutionAttributes(), request().build());

        assertThat(response.statusCode()).isEqualTo(503);
        assertThat(response.statusText()).contains("Slow Down");
        assertThat(response.headers()).isEqualTo(client.response.httpResponse().headers());
        assertThat(response.content()).containsSame(responseBody);
    }

    @Test
    void send_responseWithoutBody_hasNoContent() {
        RecordingHttpClient client = new RecordingHttpClient();
        client.response = HttpExecuteResponse.builder().response(SdkHttpResponse.builder().statusCode(204).build()).build();

        SdkHttpFullResponse response =
            SdkHttpClientTransport.create(client, false).send(new ExecutionAttributes(), request().build());

        assertThat(response.statusCode()).isEqualTo(204);
        assertThat(response.content()).isEmpty();
    }

    @Test
    void send_ioException_becomesTheLegacySdkClientExceptionWithTheSameRetryClassification() {
        RecordingHttpClient client = new RecordingHttpClient();
        IOException failure = new IOException("connection reset");
        client.failure = failure;

        SdkHttpClientTransport transport = SdkHttpClientTransport.create(client, false);

        assertThatThrownBy(() -> transport.send(new ExecutionAttributes(), request().build()))
            .isExactlyInstanceOf(SdkClientException.class)
            .hasMessage("Unable to execute HTTP request: connection reset")
            .hasCause(failure)
            .satisfies(e -> assertThat(retryableByDefault((SdkClientException) e)).isTrue());
    }

    @Test
    void send_runtimeException_propagatesUnchanged() {
        RecordingHttpClient client = new RecordingHttpClient();
        IllegalStateException failure = new IllegalStateException("pool shut down");
        client.runtimeFailure = failure;

        SdkHttpClientTransport transport = SdkHttpClientTransport.create(client, false);

        assertThatThrownBy(() -> transport.send(new ExecutionAttributes(), request().build())).isSameAs(failure);
    }

    @Test
    void send_sdkClientException_propagatesUnchanged() {
        RecordingHttpClient client = new RecordingHttpClient();
        SdkClientException failure = SdkClientException.create("from the client");
        client.runtimeFailure = failure;

        SdkHttpClientTransport transport = SdkHttpClientTransport.create(client, false);

        assertThatThrownBy(() -> transport.send(new ExecutionAttributes(), request().build())).isSameAs(failure);
    }

    @Test
    void send_givesTheClientThePipelineMetricCollectorAndAbortHook() {
        RecordingHttpClient client = new RecordingHttpClient();
        MetricCollector collector = MetricCollector.create("HttpClient");
        List<Abortable> abortables = new ArrayList<>();
        client.registeredAbortables = abortables;
        ExecutionAttributes context = new ExecutionAttributes();
        context.putAttribute(TransportExecutionAttribute.HTTP_METRIC_COLLECTOR, collector);
        context.putAttribute(TransportExecutionAttribute.ABORTABLE_LISTENER, abortables::add);

        SdkHttpClientTransport.create(client, false).send(context, request().build());

        assertThat(client.lastRequest.metricCollector()).containsSame(collector);
        assertThat(abortables).containsExactly(client.lastExecutable);
        assertThat(client.abortableRegisteredBeforeCall).isTrue();
    }

    @Test
    void send_recordsAttemptTimes() {
        ExecutionAttributes context = new ExecutionAttributes();
        long before = System.nanoTime();

        SdkHttpClientTransport.create(new RecordingHttpClient(), false).send(context, request().build());

        Long start = context.getAttribute(SdkInternalExecutionAttribute.API_CALL_ATTEMPT_START_NANO_TIME);
        Long headersRead = context.getAttribute(SdkInternalExecutionAttribute.HEADERS_READ_END_NANO_TIME);
        assertThat(start).isGreaterThanOrEqualTo(before);
        assertThat(headersRead).isGreaterThanOrEqualTo(start);
    }

    @Test
    void send_zeroCopyBody_countsBytesWritten() {
        RequestBodyMetrics metrics = new RequestBodyMetrics();
        ExecutionAttributes context = new ExecutionAttributes();
        context.putAttribute(InternalCoreExecutionAttribute.REQUEST_BODY_METRICS, metrics);
        SdkHttpFullRequest request = request().contentStreamProvider(ContentStreamProvider.fromByteArrayUnsafe(BODY)).build();

        SdkHttpClientTransport.create(new RecordingHttpClient(), false).send(context, request);

        assertThat(metrics.bytesWritten().get()).isEqualTo(BODY.length);
        assertThat(metrics.firstByteWrittenNanoTime().get()).isPositive()
                                                            .isEqualTo(metrics.lastByteWrittenNanoTime().get());
    }

    @Test
    void send_streamedBody_countsBytesTheClientReads() {
        RecordingHttpClient client = new RecordingHttpClient();
        client.readBody = true;
        RequestBodyMetrics metrics = new RequestBodyMetrics();
        ExecutionAttributes context = new ExecutionAttributes();
        context.putAttribute(InternalCoreExecutionAttribute.REQUEST_BODY_METRICS, metrics);
        SdkHttpFullRequest request = request().contentStreamProvider(() -> new ByteArrayInputStream(BODY)).build();

        SdkHttpClientTransport.create(client, false).send(context, request);

        assertThat(client.bodyRead).isEqualTo(BODY);
        assertThat(metrics.bytesWritten().get()).isEqualTo(BODY.length);
    }

    @Test
    void close_closesTheClientOnlyWhenTheTransportOwnsIt() {
        RecordingHttpClient owned = new RecordingHttpClient();
        SdkHttpClientTransport ownedTransport = SdkHttpClientTransport.create(owned, true);
        ownedTransport.close();
        ownedTransport.close();
        assertThat(owned.closeCount).isEqualTo(1);

        RecordingHttpClient userClient = new RecordingHttpClient();
        SdkHttpClientTransport.create(userClient, false).close();
        assertThat(userClient.closeCount).isZero();
    }

    @Test
    void create_requiresAClient() {
        assertThatThrownBy(() -> SdkHttpClientTransport.create(null, true)).isInstanceOf(NullPointerException.class);
    }

    static boolean retryableByDefault(SdkClientException exception) {
        return RetryOnExceptionsCondition.create(SdkDefaultRetrySetting.RETRYABLE_EXCEPTIONS)
                                         .shouldRetry(RetryPolicyContext.builder().exception(exception).build());
    }

    private static SdkHttpFullRequest.Builder request() {
        return SdkHttpFullRequest.builder()
                                 .method(SdkHttpMethod.POST)
                                 .uri(URI.create("https://example.com/a%2Fb/c?x=1&x=2&y"))
                                 .putHeader("Content-Type", "application/cbor")
                                 .putHeader("X-Multi", Arrays.asList("a", "b"));
    }

    private static final class RecordingHttpClient implements SdkHttpClient {
        private HttpExecuteRequest lastRequest;
        private ExecutableHttpRequest lastExecutable;
        private HttpExecuteResponse response =
            HttpExecuteResponse.builder().response(SdkHttpResponse.builder().statusCode(200).build()).build();
        private IOException failure;
        private RuntimeException runtimeFailure;
        private boolean readBody;
        private byte[] bodyRead;
        private List<Abortable> registeredAbortables;
        private boolean abortableRegisteredBeforeCall;
        private int closeCount;

        @Override
        public ExecutableHttpRequest prepareRequest(HttpExecuteRequest request) {
            lastRequest = request;
            lastExecutable = new ExecutableHttpRequest() {
                @Override
                public HttpExecuteResponse call() throws IOException {
                    abortableRegisteredBeforeCall = registeredAbortables != null && registeredAbortables.contains(this);
                    if (readBody && request.contentStreamProvider().isPresent()) {
                        try (InputStream in = request.contentStreamProvider().get().newStream()) {
                            bodyRead = IoUtils.toByteArray(in);
                        }
                    }
                    if (failure != null) {
                        throw failure;
                    }
                    if (runtimeFailure != null) {
                        throw runtimeFailure;
                    }
                    return response;
                }

                @Override
                public void abort() {
                }
            };
            return lastExecutable;
        }

        @Override
        public void close() {
            closeCount++;
        }
    }
}
