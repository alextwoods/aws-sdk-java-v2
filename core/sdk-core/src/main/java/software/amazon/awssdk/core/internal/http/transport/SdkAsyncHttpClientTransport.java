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

import static software.amazon.awssdk.http.Header.CONTENT_LENGTH;

import java.nio.ByteBuffer;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.annotations.ThreadSafe;
import software.amazon.awssdk.core.async.AsyncRequestBody;
import software.amazon.awssdk.core.async.EmptyPublisher;
import software.amazon.awssdk.core.client.protocol.AsyncClientTransport;
import software.amazon.awssdk.core.client.protocol.AsyncResponseHandoff;
import software.amazon.awssdk.core.client.protocol.HttpMessageExchange;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.core.interceptor.ExecutionAttributes;
import software.amazon.awssdk.core.interceptor.SdkInternalExecutionAttribute;
import software.amazon.awssdk.core.internal.InternalCoreExecutionAttribute;
import software.amazon.awssdk.core.internal.http.async.SimpleHttpContentPublisher;
import software.amazon.awssdk.core.internal.metrics.BytesReadTrackingPublisher;
import software.amazon.awssdk.core.internal.metrics.BytesWrittenTrackingPublisher;
import software.amazon.awssdk.core.internal.metrics.RequestBodyMetrics;
import software.amazon.awssdk.http.SdkHttpExecutionAttributes;
import software.amazon.awssdk.http.SdkHttpFullRequest;
import software.amazon.awssdk.http.SdkHttpFullResponse;
import software.amazon.awssdk.http.SdkHttpResponse;
import software.amazon.awssdk.http.async.AsyncExecuteRequest;
import software.amazon.awssdk.http.async.SdkAsyncHttpClient;
import software.amazon.awssdk.http.async.SdkAsyncHttpResponseHandler;
import software.amazon.awssdk.http.async.SdkHttpContentPublisher;
import software.amazon.awssdk.utils.CompletableFutureUtils;
import software.amazon.awssdk.utils.Validate;

/**
 * An {@link AsyncClientTransport} that sends requests through an {@link SdkAsyncHttpClient}.
 *
 * <p>This is the request-sending part of the legacy {@code MakeAsyncHttpRequestStage}. The request's method, URI,
 * headers and body go to the HTTP client unchanged, and the response's status, headers and body bytes come back
 * unchanged. Unlike the legacy stage, it does not add a {@code Content-Length} header; the protocol sets it.
 *
 * <ul>
 *     <li><b>Request body.</b> The {@link TransportExecutionAttribute#ASYNC_REQUEST_BODY} streaming body when set, and
 *     otherwise the request's content, published by {@link SimpleHttpContentPublisher}, which publishes a
 *     {@code contentAsByteBufferOrNull()} buffer without copying it.</li>
 *     <li><b>Response body.</b> With a {@link AsyncResponseHandoff} that accepts the headers, the body publisher is
 *     handed off and the future completes with the headers-only response. Otherwise the body is copied once into one
 *     array (presized from {@code Content-Length}), the response content is a stream over it, and the array is recorded
 *     as the call's {@link OwnedResponseBody} so the protocol can parse it without another copy.</li>
 *     <li><b>Metrics.</b> The transport records the attempt start and headers-read times and the request and response
 *     byte counts through the attributes listed in {@link TransportExecutionAttribute}; reporting the metrics stays in
 *     the pipeline.</li>
 *     <li><b>Failures.</b> The future completes exceptionally with the exception the legacy retry stage reports: an
 *     {@code SdkException} or {@code Error} unchanged, anything else as
 *     {@code SdkClientException("Unable to execute HTTP request: <message>", cause)}.</li>
 *     <li><b>Cancellation.</b> Completing the returned future exceptionally (for example on a timeout) fails the HTTP
 *     client's future, which cancels the request, and cancels a body that is being buffered.</li>
 *     <li><b>Threads.</b> Nothing blocks. The future is completed on the HTTP client's thread; moving completion to the
 *     future-completion executor is the pipeline's job.</li>
 *     <li><b>Close.</b> {@link #close()} closes the HTTP client only if the transport was created to own it.</li>
 * </ul>
 */
@SdkInternalApi
@ThreadSafe
public final class SdkAsyncHttpClientTransport implements AsyncClientTransport<SdkHttpFullRequest, SdkHttpFullResponse> {
    private final SdkAsyncHttpClient httpClient;
    private final boolean closeHttpClient;
    private final AtomicBoolean closed = new AtomicBoolean();

    private SdkAsyncHttpClientTransport(SdkAsyncHttpClient httpClient, boolean closeHttpClient) {
        this.httpClient = Validate.paramNotNull(httpClient, "httpClient");
        this.closeHttpClient = closeHttpClient;
    }

    /**
     * Creates a transport over an asynchronous HTTP client.
     *
     * @param httpClient the HTTP client to send requests through
     * @param closeHttpClient whether {@link #close()} closes the HTTP client. Pass {@code true} only for a client the SDK
     *     created for this transport. Pass {@code false} for a client supplied by the user, and for a client whose
     *     lifetime is already managed elsewhere (for example the client configuration's {@code ASYNC_HTTP_CLIENT},
     *     which the configuration closes)
     * @return the transport
     */
    public static SdkAsyncHttpClientTransport create(SdkAsyncHttpClient httpClient, boolean closeHttpClient) {
        return new SdkAsyncHttpClientTransport(httpClient, closeHttpClient);
    }

    @Override
    public HttpMessageExchange messageExchange() {
        return HttpMessageExchange.INSTANCE;
    }

    @Override
    public CompletableFuture<SdkHttpFullResponse> send(ExecutionAttributes context,
                                                       SdkHttpFullRequest request,
                                                       AsyncResponseHandoff<SdkHttpFullResponse> streamingHandoff) {
        CompletableFuture<SdkHttpFullResponse> result = new CompletableFuture<>();
        try {
            OwnedResponseBody.detach(context);
            ResponseHandler responseHandler = new ResponseHandler(context, streamingHandoff, result);
            AsyncExecuteRequest.Builder executeRequest =
                AsyncExecuteRequest.builder()
                                   .request(request)
                                   .requestContentPublisher(requestContent(context, request))
                                   .responseHandler(responseHandler)
                                   .fullDuplex(Boolean.TRUE.equals(
                                       context.getAttribute(SdkInternalExecutionAttribute.IS_FULL_DUPLEX)))
                                   .metricCollector(context.getAttribute(TransportExecutionAttribute.HTTP_METRIC_COLLECTOR));
            SdkHttpExecutionAttributes httpExecutionAttributes =
                context.getAttribute(SdkInternalExecutionAttribute.SDK_HTTP_EXECUTION_ATTRIBUTES);
            if (httpExecutionAttributes != null) {
                executeRequest.httpExecutionAttributes(httpExecutionAttributes);
            }

            context.putAttribute(SdkInternalExecutionAttribute.API_CALL_ATTEMPT_START_NANO_TIME, System.nanoTime());
            CompletableFuture<Void> executeFuture = httpClient.execute(executeRequest.build());

            // Failing or cancelling the returned future cancels the HTTP request.
            CompletableFutureUtils.forwardExceptionTo(result, executeFuture);
            executeFuture.whenComplete((ignored, failure) -> responseHandler.onExecuteComplete(failure));
        } catch (Throwable t) {
            result.completeExceptionally(TransportFailures.asyncFailure(t));
        }
        return result;
    }

    @Override
    public void close() {
        if (closeHttpClient && closed.compareAndSet(false, true)) {
            httpClient.close();
        }
    }

    private static SdkHttpContentPublisher requestContent(ExecutionAttributes context, SdkHttpFullRequest request) {
        AsyncRequestBody streamingBody = context.getAttribute(TransportExecutionAttribute.ASYNC_REQUEST_BODY);
        SdkHttpContentPublisher content =
            streamingBody == null
            ? new SimpleHttpContentPublisher(request)
            : new ContentPublisher(streamingBody, streamingBody.contentLength().orElse(null));

        RequestBodyMetrics metrics = context.getAttribute(InternalCoreExecutionAttribute.REQUEST_BODY_METRICS);
        if (metrics == null) {
            return content;
        }
        return new ContentPublisher(new BytesWrittenTrackingPublisher(content, metrics), content.contentLength().orElse(null));
    }

    private static long declaredLength(SdkHttpResponse response) {
        Optional<String> header = response.firstMatchingHeader(CONTENT_LENGTH);
        if (header.isPresent()) {
            try {
                return Long.parseLong(header.get());
            } catch (NumberFormatException e) {
                // Only a sizing hint; the HTTP client validates the framing.
            }
        }
        return -1;
    }

    private static SdkHttpFullResponse withoutBody(SdkHttpResponse response) {
        if (response instanceof SdkHttpFullResponse) {
            SdkHttpFullResponse full = (SdkHttpFullResponse) response;
            return full.content().isPresent() ? full.toBuilder().content(null).build() : full;
        }
        SdkHttpFullResponse.Builder builder = SdkHttpFullResponse.builder()
                                                                 .statusCode(response.statusCode())
                                                                 .statusText(response.statusText().orElse(null));
        response.forEachHeader(builder::putHeader);
        return builder.build();
    }

    /**
     * Receives the HTTP client's callbacks for one request and completes the transport's future exactly once.
     */
    private static final class ResponseHandler implements SdkAsyncHttpResponseHandler {
        private final ExecutionAttributes context;
        private final AsyncResponseHandoff<SdkHttpFullResponse> streamingHandoff;
        private final CompletableFuture<SdkHttpFullResponse> result;
        private final AtomicBoolean streamReceived = new AtomicBoolean();
        private volatile SdkHttpFullResponse headers;
        private volatile boolean streaming;

        private ResponseHandler(ExecutionAttributes context,
                                AsyncResponseHandoff<SdkHttpFullResponse> streamingHandoff,
                                CompletableFuture<SdkHttpFullResponse> result) {
            this.context = context;
            this.streamingHandoff = streamingHandoff;
            this.result = result;
        }

        @Override
        public void onHeaders(SdkHttpResponse response) {
            context.putAttribute(SdkInternalExecutionAttribute.HEADERS_READ_END_NANO_TIME, System.nanoTime());
            SdkHttpFullResponse headersOnly = withoutBody(response);
            headers = headersOnly;
            if (streamingHandoff != null) {
                try {
                    streaming = streamingHandoff.onHeaders(headersOnly);
                } catch (Throwable t) {
                    result.completeExceptionally(t);
                }
            }
        }

        @Override
        public void onStream(Publisher<ByteBuffer> stream) {
            if (!streamReceived.compareAndSet(false, true)) {
                discard(stream);
                return;
            }
            Publisher<ByteBuffer> body = trackBytesRead(stream);
            SdkHttpFullResponse response = headers;
            if (result.isDone()) {
                // Failed or cancelled before the body arrived (for example the hand-off rejected the headers).
                discard(body);
                return;
            }
            if (response == null) {
                discard(body);
                result.completeExceptionally(
                    SdkClientException.create("Unable to execute HTTP request: response body received before headers"));
                return;
            }
            if (streaming) {
                handOff(response, body);
            } else {
                buffer(response, body);
            }
        }

        @Override
        public void onError(Throwable error) {
            result.completeExceptionally(TransportFailures.asyncFailure(error));
        }

        /**
         * Called when the HTTP client's future completes. A failure fails the transport's future, if it is not complete
         * yet. A client that completes without ever calling {@link #onStream} is treated as having sent an empty body.
         */
        private void onExecuteComplete(Throwable failure) {
            if (failure != null) {
                result.completeExceptionally(TransportFailures.asyncFailure(failure));
            } else if (headers == null) {
                result.completeExceptionally(
                    SdkClientException.create("Unable to execute HTTP request: the HTTP client completed without a response"));
            } else if (!streamReceived.get()) {
                onStream(new EmptyPublisher<>());
            }
        }

        private void handOff(SdkHttpFullResponse response, Publisher<ByteBuffer> body) {
            try {
                streamingHandoff.onStream(body);
                result.complete(response);
            } catch (Throwable t) {
                result.completeExceptionally(t);
            }
        }

        private void buffer(SdkHttpFullResponse response, Publisher<ByteBuffer> body) {
            ResponseBodyAggregator aggregator = new ResponseBodyAggregator(declaredLength(response));
            result.whenComplete((ignored, failure) -> {
                if (failure != null) {
                    aggregator.cancel();
                }
            });
            aggregator.result().whenComplete((owned, failure) -> {
                if (failure != null) {
                    result.completeExceptionally(TransportFailures.asyncFailure(failure));
                } else if (!result.isDone()) {
                    owned.attach(context);
                    result.complete(response.toBuilder().content(owned.content()).build());
                }
            });
            body.subscribe(aggregator);
        }

        private Publisher<ByteBuffer> trackBytesRead(Publisher<ByteBuffer> stream) {
            AtomicLong bytesRead = context.getAttribute(SdkInternalExecutionAttribute.RESPONSE_BYTES_READ);
            return bytesRead == null ? stream : new BytesReadTrackingPublisher(stream, bytesRead);
        }

        /**
         * Subscribes and cancels at once, so that the HTTP client releases a body nobody reads.
         */
        private static void discard(Publisher<ByteBuffer> stream) {
            ResponseBodyAggregator cancelled = new ResponseBodyAggregator(-1);
            cancelled.cancel();
            stream.subscribe(cancelled);
        }
    }

    /**
     * An {@link SdkHttpContentPublisher} over a publisher and a known or unknown content length.
     */
    private static final class ContentPublisher implements SdkHttpContentPublisher {
        private final Publisher<ByteBuffer> publisher;
        private final Long contentLength;

        private ContentPublisher(Publisher<ByteBuffer> publisher, Long contentLength) {
            this.publisher = publisher;
            this.contentLength = contentLength;
        }

        @Override
        public Optional<Long> contentLength() {
            return Optional.ofNullable(contentLength);
        }

        @Override
        public void subscribe(Subscriber<? super ByteBuffer> subscriber) {
            publisher.subscribe(subscriber);
        }
    }
}
