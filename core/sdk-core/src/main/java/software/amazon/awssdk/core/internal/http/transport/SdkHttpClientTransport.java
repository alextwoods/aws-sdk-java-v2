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

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.annotations.ThreadSafe;
import software.amazon.awssdk.core.client.protocol.ClientTransport;
import software.amazon.awssdk.core.client.protocol.HttpMessageExchange;
import software.amazon.awssdk.core.interceptor.ExecutionAttributes;
import software.amazon.awssdk.core.interceptor.SdkInternalExecutionAttribute;
import software.amazon.awssdk.core.internal.InternalCoreExecutionAttribute;
import software.amazon.awssdk.core.internal.metrics.BytesWrittenTrackingInputStream;
import software.amazon.awssdk.core.internal.metrics.RequestBodyMetrics;
import software.amazon.awssdk.http.Abortable;
import software.amazon.awssdk.http.ContentStreamProvider;
import software.amazon.awssdk.http.ExecutableHttpRequest;
import software.amazon.awssdk.http.HttpExecuteRequest;
import software.amazon.awssdk.http.HttpExecuteResponse;
import software.amazon.awssdk.http.SdkHttpClient;
import software.amazon.awssdk.http.SdkHttpFullRequest;
import software.amazon.awssdk.http.SdkHttpFullResponse;
import software.amazon.awssdk.http.SdkHttpResponse;
import software.amazon.awssdk.utils.Logger;
import software.amazon.awssdk.utils.Validate;
import software.amazon.awssdk.utils.io.LengthAwareInputStream;

/**
 * A {@link ClientTransport} that sends requests through an {@link SdkHttpClient}.
 *
 * <p>This is the request-sending part of the legacy {@code MakeHttpRequestStage}. The request's method, URI, headers
 * and body go to the HTTP client unchanged, and the response's status, headers and body stream come back unchanged; the
 * body stream is not read here.
 *
 * <ul>
 *     <li><b>Zero copy.</b> A body whose {@link ContentStreamProvider#contentAsByteBufferOrNull()} returns a buffer is
 *     given to the HTTP client as the same provider, so a client that supports the contract (Apache5) sends that
 *     buffer without copying it. The legacy stage wraps every body in a counting, length-checking stream, which hides
 *     the buffer. Here the body is only wrapped when it has no buffer, or when its {@code Content-Length} header does
 *     not match the buffer (so the legacy length check still fails the request).</li>
 *     <li><b>Metrics.</b> The transport records the attempt start and headers-read times and the request bytes written,
 *     through the attributes listed in {@link TransportExecutionAttribute}; reporting the metrics stays in the
 *     pipeline.</li>
 *     <li><b>Failures.</b> An {@link IOException} from the HTTP client becomes
 *     {@code SdkClientException("Unable to execute HTTP request: <message>", cause)}, as the legacy retry stage reports
 *     it. Runtime exceptions propagate unchanged, as in the legacy pipeline.</li>
 *     <li><b>Close.</b> {@link #close()} closes the HTTP client only if the transport was created to own it.</li>
 * </ul>
 */
@SdkInternalApi
@ThreadSafe
public final class SdkHttpClientTransport implements ClientTransport<SdkHttpFullRequest, SdkHttpFullResponse> {
    private static final Logger LOG = Logger.loggerFor(SdkHttpClientTransport.class);

    private final SdkHttpClient httpClient;
    private final boolean closeHttpClient;
    private final AtomicBoolean closed = new AtomicBoolean();

    private SdkHttpClientTransport(SdkHttpClient httpClient, boolean closeHttpClient) {
        this.httpClient = Validate.paramNotNull(httpClient, "httpClient");
        this.closeHttpClient = closeHttpClient;
    }

    /**
     * Creates a transport over an HTTP client.
     *
     * @param httpClient the HTTP client to send requests through
     * @param closeHttpClient whether {@link #close()} closes the HTTP client. Pass {@code true} only for a client the SDK
     *     created for this transport. Pass {@code false} for a client supplied by the user, and for a client whose
     *     lifetime is already managed elsewhere (for example the client configuration's {@code SYNC_HTTP_CLIENT}, which
     *     the configuration closes)
     * @return the transport
     */
    public static SdkHttpClientTransport create(SdkHttpClient httpClient, boolean closeHttpClient) {
        return new SdkHttpClientTransport(httpClient, closeHttpClient);
    }

    @Override
    public HttpMessageExchange messageExchange() {
        return HttpMessageExchange.INSTANCE;
    }

    @Override
    public SdkHttpFullResponse send(ExecutionAttributes context, SdkHttpFullRequest request) {
        ContentStreamProvider provider = request.contentStreamProvider().orElse(null);
        Long contentLength = provider == null ? null : contentLength(request);
        ByteBuffer zeroCopyBody = provider == null ? null : zeroCopyBuffer(provider, contentLength);
        RequestBodyMetrics bodyMetrics = context.getAttribute(InternalCoreExecutionAttribute.REQUEST_BODY_METRICS);

        ContentStreamProvider body = provider == null || zeroCopyBody != null
                                     ? provider
                                     : trackingBody(request, provider, contentLength, bodyMetrics);
        ExecutableHttpRequest executable =
            httpClient.prepareRequest(HttpExecuteRequest.builder()
                                                        .request(request)
                                                        .contentStreamProvider(body)
                                                        .metricCollector(context.getAttribute(
                                                            TransportExecutionAttribute.HTTP_METRIC_COLLECTOR))
                                                        .build());

        Consumer<Abortable> abortableListener = context.getAttribute(TransportExecutionAttribute.ABORTABLE_LISTENER);
        if (abortableListener != null) {
            abortableListener.accept(executable);
        }

        context.putAttribute(SdkInternalExecutionAttribute.API_CALL_ATTEMPT_START_NANO_TIME, System.nanoTime());
        HttpExecuteResponse executeResponse;
        try {
            executeResponse = executable.call();
        } catch (IOException e) {
            throw TransportFailures.httpRequestFailure(e);
        }
        long headersRead = System.nanoTime();
        context.putAttribute(SdkInternalExecutionAttribute.HEADERS_READ_END_NANO_TIME, headersRead);
        if (zeroCopyBody != null && bodyMetrics != null) {
            recordZeroCopyBodyWritten(bodyMetrics, zeroCopyBody.remaining(), headersRead);
        }

        return toFullResponse(executeResponse);
    }

    @Override
    public void close() {
        if (closeHttpClient && closed.compareAndSet(false, true)) {
            httpClient.close();
        }
    }

    /**
     * The legacy body wrapper, for a body without a usable buffer: counts the bytes the HTTP client reads (when metrics
     * are collected) and fails the request if the body is shorter than its {@code Content-Length}.
     */
    private static ContentStreamProvider trackingBody(SdkHttpFullRequest request,
                                                      ContentStreamProvider provider,
                                                      Long contentLength,
                                                      RequestBodyMetrics metrics) {
        return () -> {
            InputStream stream = provider.newStream();
            if (metrics != null) {
                stream = new BytesWrittenTrackingInputStream(stream, metrics);
            }
            if (contentLength == null) {
                LOG.debug(() -> String.format("Request contains a body but does not have a Content-Length header. Not "
                                              + "validating the amount of data sent to the service: %s", request));
                return stream;
            }
            return new LengthAwareInputStream(stream, contentLength);
        };
    }

    /**
     * The HTTP client sent the whole buffer of a zero-copy body by the time the response headers arrived. A single
     * write has no measurable duration, so first and last byte times are equal and {@code WRITE_THROUGHPUT} is not
     * reported for it, as for a single-buffer body on the legacy asynchronous path.
     */
    private static void recordZeroCopyBodyWritten(RequestBodyMetrics metrics, int bytes, long now) {
        if (bytes > 0) {
            metrics.firstByteWrittenNanoTime().compareAndSet(0, now);
            metrics.bytesWritten().addAndGet(bytes);
            metrics.lastByteWrittenNanoTime().set(now);
        }
    }

    /**
     * Returns the provider's buffer if the body can be sent from it as is: the provider has one, and it holds exactly
     * the declared {@code Content-Length} bytes when that header is set. A mismatch takes the legacy stream path, which
     * truncates a longer body and fails a shorter one.
     */
    private static ByteBuffer zeroCopyBuffer(ContentStreamProvider provider, Long contentLength) {
        ByteBuffer buffer = provider.contentAsByteBufferOrNull();
        if (buffer == null) {
            return null;
        }
        if (contentLength != null && contentLength != buffer.remaining()) {
            return null;
        }
        return buffer;
    }

    private static SdkHttpFullResponse toFullResponse(HttpExecuteResponse executeResponse) {
        SdkHttpResponse httpResponse = executeResponse.httpResponse();
        SdkHttpFullResponse.Builder builder;
        if (httpResponse instanceof SdkHttpFullResponse) {
            builder = ((SdkHttpFullResponse) httpResponse).toBuilder();
        } else {
            builder = SdkHttpFullResponse.builder()
                                         .statusCode(httpResponse.statusCode())
                                         .statusText(httpResponse.statusText().orElse(null));
            httpResponse.forEachHeader(builder::putHeader);
        }
        return builder.content(executeResponse.responseBody().orElse(null)).build();
    }

    /**
     * Returns the parsed {@code Content-Length} header, or null if it is absent or malformed.
     */
    private static Long contentLength(SdkHttpFullRequest request) {
        Optional<String> header = request.firstMatchingHeader(CONTENT_LENGTH);
        if (header.isPresent()) {
            try {
                return Long.parseLong(header.get());
            } catch (NumberFormatException e) {
                LOG.warn(() -> "Unable to parse 'Content-Length' header. Treating it as non existent.");
            }
        }
        return null;
    }
}
