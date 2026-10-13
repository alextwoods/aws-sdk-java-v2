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
import java.net.URI;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.Test;
import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;
import software.amazon.awssdk.codecs.commons.ByteBufferOutputStream;
import software.amazon.awssdk.core.async.AsyncRequestBody;
import software.amazon.awssdk.core.client.protocol.AsyncResponseHandoff;
import software.amazon.awssdk.core.client.protocol.HttpMessageExchange;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.core.interceptor.ExecutionAttributes;
import software.amazon.awssdk.core.interceptor.SdkInternalExecutionAttribute;
import software.amazon.awssdk.core.internal.InternalCoreExecutionAttribute;
import software.amazon.awssdk.core.internal.metrics.RequestBodyMetrics;
import software.amazon.awssdk.http.AbortableInputStream;
import software.amazon.awssdk.http.ContentStreamProvider;
import software.amazon.awssdk.http.SdkHttpExecutionAttribute;
import software.amazon.awssdk.http.SdkHttpExecutionAttributes;
import software.amazon.awssdk.http.SdkHttpFullRequest;
import software.amazon.awssdk.http.SdkHttpFullResponse;
import software.amazon.awssdk.http.SdkHttpMethod;
import software.amazon.awssdk.http.SdkHttpResponse;
import software.amazon.awssdk.http.async.AsyncExecuteRequest;
import software.amazon.awssdk.http.async.SdkAsyncHttpClient;
import software.amazon.awssdk.http.async.SdkAsyncHttpResponseHandler;
import software.amazon.awssdk.metrics.MetricCollector;
import software.amazon.awssdk.utils.IoUtils;

class SdkAsyncHttpClientTransportTest {
    private static final byte[] BODY = "{\"hello\":\"world\"}".getBytes(StandardCharsets.UTF_8);

    private final RecordingAsyncHttpClient client = new RecordingAsyncHttpClient();
    private final SdkAsyncHttpClientTransport transport = SdkAsyncHttpClientTransport.create(client, false);
    private final ExecutionAttributes context = new ExecutionAttributes();

    @Test
    void messageExchange_isHttp() {
        assertThat(transport.messageExchange()).isSameAs(HttpMessageExchange.INSTANCE);
    }

    @Test
    void send_passesMethodUriHeadersAndBodyThroughUnchanged() {
        SdkHttpFullRequest request = request().contentStreamProvider(() -> new ByteArrayInputStream(BODY)).build();

        transport.send(context, request, null);

        AsyncExecuteRequest sent = client.lastRequest;
        assertThat(sent.request()).isSameAs(request);
        assertThat(sent.request().method()).isEqualTo(SdkHttpMethod.POST);
        assertThat(sent.request().getUri()).isEqualTo(URI.create("https://example.com/a%2Fb/c?x=1&x=2&y"));
        assertThat(sent.request().headers()).isEqualTo(request.headers()).doesNotContainKey("Content-Length");
        assertThat(sent.requestContentPublisher().contentLength()).contains((long) BODY.length);
        assertThat(concat(collect(sent.requestContentPublisher()))).isEqualTo(BODY);
    }

    @Test
    void send_singleBufferBody_isPublishedWithoutACopy() {
        ByteBufferOutputStream out = new ByteBufferOutputStream(64);
        out.write(BODY, 0, BODY.length);
        ContentStreamProvider body = out.toContentStreamProvider();
        context.putAttribute(InternalCoreExecutionAttribute.REQUEST_BODY_METRICS, new RequestBodyMetrics());

        transport.send(context, request().contentStreamProvider(body).build(), null);

        List<ByteBuffer> published = collect(client.lastRequest.requestContentPublisher());
        assertThat(published).hasSize(1);
        assertThat(published.get(0).array()).isSameAs(out.array());
        assertThat(published.get(0).remaining()).isEqualTo(BODY.length);
    }

    @Test
    void send_streamingRequestBody_sendsItInsteadOfTheRequestContent() {
        AsyncRequestBody streaming = AsyncRequestBody.fromBytes(BODY);
        context.putAttribute(TransportExecutionAttribute.ASYNC_REQUEST_BODY, streaming);

        transport.send(context, request().build(), null);

        assertThat(client.lastRequest.requestContentPublisher().contentLength()).contains((long) BODY.length);
        assertThat(concat(collect(client.lastRequest.requestContentPublisher()))).isEqualTo(BODY);
    }

    @Test
    void send_passesThePipelineHttpOptionsToTheClient() {
        MetricCollector collector = MetricCollector.create("HttpClient");
        SdkHttpExecutionAttributes httpAttributes = SdkHttpExecutionAttributes.builder()
                                                                              .put(TestHttpAttribute.VALUE, "v")
                                                                              .build();
        context.putAttribute(TransportExecutionAttribute.HTTP_METRIC_COLLECTOR, collector);
        context.putAttribute(SdkInternalExecutionAttribute.IS_FULL_DUPLEX, true);
        context.putAttribute(SdkInternalExecutionAttribute.SDK_HTTP_EXECUTION_ATTRIBUTES, httpAttributes);

        transport.send(context, request().build(), null);

        assertThat(client.lastRequest.metricCollector()).containsSame(collector);
        assertThat(client.lastRequest.fullDuplex()).isTrue();
        // The request builder copies the attributes into its own instance.
        assertThat(client.lastRequest.httpExecutionAttributes().getAttribute(TestHttpAttribute.VALUE)).isEqualTo("v");
    }

    @Test
    void send_multipleChunksWithContentLength_areAggregatedIntoOnePresizedOwnedBuffer() throws IOException {
        CompletableFuture<SdkHttpFullResponse> future = transport.send(context, request().build(), null);

        TestBodyPublisher body = respond(headers(200).putHeader("Content-Length", String.valueOf(BODY.length)).build());
        body.send(ByteBuffer.wrap(BODY, 0, 5));
        body.send(ByteBuffer.wrap(BODY, 5, 7));
        body.send(ByteBuffer.wrap(BODY, 12, BODY.length - 12));
        body.complete();
        client.executeFuture.complete(null);

        SdkHttpFullResponse response = join(future);
        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.headers()).containsEntry("x-amzn-RequestId", Arrays.asList("id"));
        ByteBuffer owned = OwnedResponseBody.ownedBuffer(context, response);
        assertThat(owned.hasArray()).isTrue();
        assertThat(owned.array()).hasSize(BODY.length).isNotSameAs(BODY);
        assertThat(owned.position()).isZero();
        assertThat(owned.remaining()).isEqualTo(BODY.length);
        assertThat(IoUtils.toByteArray(response.content().get())).isEqualTo(BODY);
    }

    @Test
    void send_multipleChunksWithoutContentLength_areAggregatedInOrder() throws IOException {
        byte[] large = new byte[10_000];
        for (int i = 0; i < large.length; i++) {
            large[i] = (byte) i;
        }
        CompletableFuture<SdkHttpFullResponse> future = transport.send(context, request().build(), null);

        TestBodyPublisher body = respond(headers(200).build());
        for (int offset = 0; offset < large.length; offset += 777) {
            body.send(ByteBuffer.wrap(large, offset, Math.min(777, large.length - offset)));
        }
        body.complete();
        client.executeFuture.complete(null);

        SdkHttpFullResponse response = join(future);
        assertThat(toArray(OwnedResponseBody.ownedBuffer(context, response))).isEqualTo(large);
        assertThat(IoUtils.toByteArray(response.content().get())).isEqualTo(large);
    }

    @Test
    void send_emptyBody_completesWithAnEmptyOwnedBody() throws IOException {
        CompletableFuture<SdkHttpFullResponse> future = transport.send(context, request().build(), null);

        TestBodyPublisher body = respond(headers(200).putHeader("Content-Length", "0").build());
        body.complete();
        client.executeFuture.complete(null);

        SdkHttpFullResponse response = join(future);
        assertThat(OwnedResponseBody.ownedBuffer(context, response).remaining()).isZero();
        assertThat(IoUtils.toByteArray(response.content().get())).isEmpty();
    }

    @Test
    void send_clientCompletesWithoutABodyStream_completesWithAnEmptyBody() throws IOException {
        CompletableFuture<SdkHttpFullResponse> future = transport.send(context, request().build(), null);

        client.handler().onHeaders(headers(204).build());
        client.executeFuture.complete(null);

        SdkHttpFullResponse response = join(future);
        assertThat(response.statusCode()).isEqualTo(204);
        assertThat(IoUtils.toByteArray(response.content().get())).isEmpty();
    }

    @Test
    void send_clientCompletesWithoutHeaders_fails() {
        CompletableFuture<SdkHttpFullResponse> future = transport.send(context, request().build(), null);

        client.executeFuture.complete(null);

        assertThatThrownBy(() -> join(future)).hasCauseInstanceOf(SdkClientException.class)
                                              .hasMessageContaining("completed without a response");
    }

    @Test
    void send_errorMidStream_failsWithTheLegacySdkClientException() {
        CompletableFuture<SdkHttpFullResponse> future = transport.send(context, request().build(), null);
        IOException failure = new IOException("connection reset");

        TestBodyPublisher body = respond(headers(200).build());
        body.send(ByteBuffer.wrap(BODY));
        body.error(failure);
        client.handler().onError(failure);
        client.executeFuture.completeExceptionally(failure);

        assertThatThrownBy(() -> join(future))
            .isInstanceOf(CompletionException.class)
            .getCause()
            .isExactlyInstanceOf(SdkClientException.class)
            .hasMessage("Unable to execute HTTP request: connection reset")
            .hasCause(failure)
            .satisfies(e -> assertThat(SdkHttpClientTransportTest.retryableByDefault((SdkClientException) e)).isTrue());
    }

    @Test
    void send_executeFutureFails_failsWithTheMappedException() {
        CompletableFuture<SdkHttpFullResponse> future = transport.send(context, request().build(), null);
        IOException failure = new IOException("refused");

        client.executeFuture.completeExceptionally(new CompletionException(failure));

        assertThatThrownBy(() -> join(future)).getCause()
                                              .isExactlyInstanceOf(SdkClientException.class)
                                              .hasMessage("Unable to execute HTTP request: refused")
                                              .hasCause(failure);
    }

    @Test
    void send_sdkExceptionOrError_isNotWrapped() {
        SdkClientException sdkFailure = SdkClientException.create("from the client");
        CompletableFuture<SdkHttpFullResponse> sdkFuture = transport.send(context, request().build(), null);
        client.executeFuture.completeExceptionally(sdkFailure);
        assertThatThrownBy(() -> join(sdkFuture)).getCause().isSameAs(sdkFailure);

        OutOfMemoryError error = new OutOfMemoryError("test");
        client.executeFuture = new CompletableFuture<>();
        CompletableFuture<SdkHttpFullResponse> errorFuture = transport.send(context, request().build(), null);
        client.executeFuture.completeExceptionally(error);
        assertThatThrownBy(() -> join(errorFuture)).getCause().isSameAs(error);
    }

    @Test
    void send_clientThrowsSynchronously_failsTheFuture() {
        client.throwOnExecute = new IllegalStateException("closed");

        CompletableFuture<SdkHttpFullResponse> future = transport.send(context, request().build(), null);

        assertThatThrownBy(() -> join(future)).getCause()
                                              .isExactlyInstanceOf(SdkClientException.class)
                                              .hasMessage("Unable to execute HTTP request: closed")
                                              .hasCause(client.throwOnExecute);
    }

    @Test
    void send_futureFailedWhileBuffering_cancelsTheBodyAndTheHttpRequest() {
        CompletableFuture<SdkHttpFullResponse> future = transport.send(context, request().build(), null);
        TestBodyPublisher body = respond(headers(200).build());
        body.send(ByteBuffer.wrap(BODY));

        future.completeExceptionally(new RuntimeException("attempt timeout"));

        assertThat(body.cancelled).isTrue();
        assertThat(client.executeFuture).isCompletedExceptionally();
        body.send(ByteBuffer.wrap(BODY));
        body.complete();
        assertThat(OwnedResponseBody.ownedBuffer(context, SdkHttpFullResponse.builder().statusCode(200).build())).isNull();
    }

    @Test
    void send_futureCancelledBeforeTheBody_cancelsTheBodyWhenItArrives() {
        CompletableFuture<SdkHttpFullResponse> future = transport.send(context, request().build(), null);
        client.handler().onHeaders(headers(200).build());

        future.cancel(true);
        TestBodyPublisher body = new TestBodyPublisher();
        client.handler().onStream(body);

        assertThat(body.cancelled).isTrue();
        assertThat(client.executeFuture).isCompletedExceptionally();
    }

    @Test
    void send_handoffAcceptsHeaders_handsOffTheBodyAndCompletesWithHeadersOnly() {
        RecordingHandoff handoff = new RecordingHandoff(true);
        CompletableFuture<SdkHttpFullResponse> future = transport.send(context, request().build(), handoff);

        TestBodyPublisher body = respond(headers(200).putHeader("Content-Length", "3").build());

        SdkHttpFullResponse response = join(future);
        assertThat(handoff.headers).isSameAs(response);
        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.content()).isEmpty();
        assertThat(handoff.stream).isSameAs(body);
        assertThat(OwnedResponseBody.ownedBuffer(context, response)).isNull();

        // The body belongs to the hand-off: later failures go to its subscriber, not to the completed future.
        client.handler().onError(new IOException("late"));
        assertThat(future).isCompletedWithValue(response);
    }

    @Test
    void send_handoffRejectsHeaders_buffersTheBody() throws IOException {
        RecordingHandoff handoff = new RecordingHandoff(false);
        CompletableFuture<SdkHttpFullResponse> future = transport.send(context, request().build(), handoff);

        TestBodyPublisher body = respond(headers(400).build());
        body.send(ByteBuffer.wrap(BODY));
        body.complete();
        client.executeFuture.complete(null);

        SdkHttpFullResponse response = join(future);
        assertThat(handoff.headers.statusCode()).isEqualTo(400);
        assertThat(handoff.stream).isNull();
        assertThat(IoUtils.toByteArray(response.content().get())).isEqualTo(BODY);
    }

    @Test
    void send_handoffThrows_failsAndDiscardsTheBody() {
        IllegalStateException failure = new IllegalStateException("handoff failed");
        AsyncResponseHandoff<SdkHttpFullResponse> handoff = new AsyncResponseHandoff<SdkHttpFullResponse>() {
            @Override
            public boolean onHeaders(SdkHttpFullResponse response) {
                throw failure;
            }

            @Override
            public void onStream(Publisher<ByteBuffer> body) {
            }
        };
        CompletableFuture<SdkHttpFullResponse> future = transport.send(context, request().build(), handoff);

        TestBodyPublisher body = respond(headers(200).build());

        assertThatThrownBy(() -> join(future)).getCause().isSameAs(failure);
        assertThat(body.cancelled).isTrue();
        assertThat(client.executeFuture).isCompletedExceptionally();
    }

    @Test
    void ownedBuffer_contentReplacedOrRead_isNull() throws IOException {
        CompletableFuture<SdkHttpFullResponse> future = transport.send(context, request().build(), null);
        TestBodyPublisher body = respond(headers(200).build());
        body.send(ByteBuffer.wrap(BODY));
        body.complete();
        SdkHttpFullResponse response = join(future);

        SdkHttpFullResponse replaced =
            response.toBuilder().content(AbortableInputStream.create(new ByteArrayInputStream(BODY))).build();
        assertThat(OwnedResponseBody.ownedBuffer(context, replaced)).isNull();
        assertThat(OwnedResponseBody.ownedBuffer(new ExecutionAttributes(), response)).isNull();

        assertThat(response.content().get().read()).isEqualTo(BODY[0]);
        assertThat(OwnedResponseBody.ownedBuffer(context, response)).isNull();
    }

    @Test
    void send_recordsAttemptTimesAndResponseBytesRead() {
        AtomicLong bytesRead = new AtomicLong();
        context.putAttribute(SdkInternalExecutionAttribute.RESPONSE_BYTES_READ, bytesRead);
        long before = System.nanoTime();
        CompletableFuture<SdkHttpFullResponse> future = transport.send(context, request().build(), null);

        TestBodyPublisher body = respond(headers(200).build());
        body.send(ByteBuffer.wrap(BODY));
        body.complete();
        join(future);

        Long start = context.getAttribute(SdkInternalExecutionAttribute.API_CALL_ATTEMPT_START_NANO_TIME);
        Long headersRead = context.getAttribute(SdkInternalExecutionAttribute.HEADERS_READ_END_NANO_TIME);
        assertThat(start).isGreaterThanOrEqualTo(before);
        assertThat(headersRead).isGreaterThanOrEqualTo(start);
        assertThat(bytesRead.get()).isEqualTo(BODY.length);
    }

    @Test
    void send_countsRequestBytesWritten() {
        RequestBodyMetrics metrics = new RequestBodyMetrics();
        context.putAttribute(InternalCoreExecutionAttribute.REQUEST_BODY_METRICS, metrics);

        transport.send(context, request().contentStreamProvider(ContentStreamProvider.fromByteArrayUnsafe(BODY)).build(),
                       null);
        collect(client.lastRequest.requestContentPublisher());

        assertThat(metrics.bytesWritten().get()).isEqualTo(BODY.length);
        assertThat(client.lastRequest.requestContentPublisher().contentLength()).contains((long) BODY.length);
    }

    @Test
    void close_closesTheClientOnlyWhenTheTransportOwnsIt() {
        RecordingAsyncHttpClient owned = new RecordingAsyncHttpClient();
        SdkAsyncHttpClientTransport ownedTransport = SdkAsyncHttpClientTransport.create(owned, true);
        ownedTransport.close();
        ownedTransport.close();
        assertThat(owned.closeCount).isEqualTo(1);

        transport.close();
        assertThat(client.closeCount).isZero();
    }

    @Test
    void create_requiresAClient() {
        assertThatThrownBy(() -> SdkAsyncHttpClientTransport.create(null, true)).isInstanceOf(NullPointerException.class);
    }

    private TestBodyPublisher respond(SdkHttpResponse headers) {
        client.handler().onHeaders(headers);
        TestBodyPublisher body = new TestBodyPublisher();
        client.handler().onStream(body);
        return body;
    }

    private static SdkHttpResponse.Builder headers(int status) {
        return SdkHttpResponse.builder().statusCode(status).putHeader("x-amzn-RequestId", "id");
    }

    private static SdkHttpFullRequest.Builder request() {
        return SdkHttpFullRequest.builder()
                                 .method(SdkHttpMethod.POST)
                                 .uri(URI.create("https://example.com/a%2Fb/c?x=1&x=2&y"))
                                 .putHeader("Content-Type", "application/cbor")
                                 .putHeader("X-Multi", Arrays.asList("a", "b"));
    }

    private static <T> T join(CompletableFuture<T> future) {
        try {
            return future.get(5, TimeUnit.SECONDS);
        } catch (java.util.concurrent.ExecutionException e) {
            throw new CompletionException(e.getCause());
        } catch (Exception e) {
            throw new AssertionError(e);
        }
    }

    private static List<ByteBuffer> collect(Publisher<ByteBuffer> publisher) {
        List<ByteBuffer> received = new ArrayList<>();
        CompletableFuture<Void> done = new CompletableFuture<>();
        publisher.subscribe(new Subscriber<ByteBuffer>() {
            @Override
            public void onSubscribe(Subscription s) {
                s.request(Long.MAX_VALUE);
            }

            @Override
            public void onNext(ByteBuffer buffer) {
                received.add(buffer);
            }

            @Override
            public void onError(Throwable t) {
                done.completeExceptionally(t);
            }

            @Override
            public void onComplete() {
                done.complete(null);
            }
        });
        join(done);
        return received;
    }

    private static byte[] concat(List<ByteBuffer> buffers) {
        int total = buffers.stream().mapToInt(ByteBuffer::remaining).sum();
        ByteBuffer all = ByteBuffer.allocate(total);
        buffers.forEach(b -> all.put(b.duplicate()));
        return all.array();
    }

    private static byte[] toArray(ByteBuffer buffer) {
        byte[] bytes = new byte[buffer.remaining()];
        buffer.duplicate().get(bytes);
        return bytes;
    }

    private static final class RecordingAsyncHttpClient implements SdkAsyncHttpClient {
        private AsyncExecuteRequest lastRequest;
        private CompletableFuture<Void> executeFuture = new CompletableFuture<>();
        private RuntimeException throwOnExecute;
        private int closeCount;

        @Override
        public CompletableFuture<Void> execute(AsyncExecuteRequest request) {
            if (throwOnExecute != null) {
                throw throwOnExecute;
            }
            lastRequest = request;
            return executeFuture;
        }

        SdkAsyncHttpResponseHandler handler() {
            return lastRequest.responseHandler();
        }

        @Override
        public void close() {
            closeCount++;
        }
    }

    /**
     * A body publisher driven by the test. It ignores demand, which the aggregator requests unbounded anyway.
     */
    private static final class TestBodyPublisher implements Publisher<ByteBuffer> {
        private Subscriber<? super ByteBuffer> subscriber;
        private volatile boolean cancelled;

        @Override
        public void subscribe(Subscriber<? super ByteBuffer> s) {
            subscriber = s;
            s.onSubscribe(new Subscription() {
                @Override
                public void request(long n) {
                }

                @Override
                public void cancel() {
                    cancelled = true;
                }
            });
        }

        void send(ByteBuffer buffer) {
            subscriber.onNext(buffer);
        }

        void complete() {
            subscriber.onComplete();
        }

        void error(Throwable t) {
            subscriber.onError(t);
        }
    }

    private static final class TestHttpAttribute<T> extends SdkHttpExecutionAttribute<T> {
        private static final TestHttpAttribute<String> VALUE = new TestHttpAttribute<>(String.class);

        private TestHttpAttribute(Class<T> valueType) {
            super(valueType);
        }
    }

    private static final class RecordingHandoff implements AsyncResponseHandoff<SdkHttpFullResponse> {
        private final boolean acceptStream;
        private SdkHttpFullResponse headers;
        private Publisher<ByteBuffer> stream;

        private RecordingHandoff(boolean acceptStream) {
            this.acceptStream = acceptStream;
        }

        @Override
        public boolean onHeaders(SdkHttpFullResponse response) {
            headers = response;
            return acceptStream;
        }

        @Override
        public void onStream(Publisher<ByteBuffer> body) {
            stream = body;
        }
    }
}
