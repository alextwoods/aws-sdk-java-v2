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

package software.amazon.awssdk.http.smithy;

import static software.amazon.awssdk.http.HttpMetric.HTTP_CLIENT_NAME;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.time.Duration;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import javax.net.ssl.SSLException;
import javax.net.ssl.SSLHandshakeException;
import software.amazon.awssdk.annotations.SdkPublicApi;
import software.amazon.awssdk.http.AbortableInputStream;
import software.amazon.awssdk.http.ExecutableHttpRequest;
import software.amazon.awssdk.http.HttpExecuteRequest;
import software.amazon.awssdk.http.HttpExecuteResponse;
import software.amazon.awssdk.http.SdkHttpClient;
import software.amazon.awssdk.http.SdkHttpConfigurationOption;
import software.amazon.awssdk.http.SdkHttpMethod;
import software.amazon.awssdk.http.SdkHttpRequest;
import software.amazon.awssdk.http.SdkHttpResponse;
import software.amazon.awssdk.metrics.MetricCollector;
import software.amazon.awssdk.metrics.NoOpMetricCollector;
import software.amazon.awssdk.utils.AttributeMap;
import software.amazon.awssdk.utils.IoUtils;
import software.amazon.awssdk.utils.Logger;
import software.amazon.smithy.java.http.api.HttpHeaders;
import software.amazon.smithy.java.http.api.HttpRequest;
import software.amazon.smithy.java.http.api.HttpResponse;
import software.amazon.smithy.java.http.api.ModifiableHttpRequest;
import software.amazon.smithy.java.io.datastream.DataStream;
import software.amazon.smithy.java.io.uri.SmithyUri;

/**
 * A synchronous {@link SdkHttpClient} backed by smithy-java's HTTP client.
 *
 * <p>This is not one of the SDK's default HTTP clients and is not discovered by {@code ServiceLoader}. It is
 * used only when passed explicitly:
 *
 * <pre class="brush: java">
 * DynamoDbClient.builder()
 *               .httpClient(SmithyHttpClient.create())
 *               .build();
 * </pre>
 *
 * <p><b>Requirements and caveats.</b> smithy-java publishes Java 21 bytecode, so this client requires Java 21
 * even though the rest of the SDK targets Java 8. Its fastest transport is a native epoll reactor that works
 * only on Linux with Netty's native epoll library present; elsewhere smithy falls back to a portable socket
 * transport, which is correct but does not carry the same performance characteristics. The epoll bridge also
 * reaches into Netty package-private API, so it cannot be used on the module path.
 *
 * <p>smithy's client is blocking by design, which lines up exactly with this synchronous SPI. The asynchronous
 * counterpart, {@link SmithyAsyncHttpClient}, has to bridge threads and carries a caveat of its own.
 */
@SdkPublicApi
public final class SmithyHttpClient implements SdkHttpClient {

    private static final String CLIENT_NAME = "SmithySync";
    private static final Logger log = Logger.loggerFor(SmithyHttpClient.class);

    private final software.amazon.smithy.java.http.client.HttpClient smithyClient;

    private SmithyHttpClient(DefaultBuilder builder, AttributeMap options) {
        this.smithyClient = SmithyClientOptions.apply(software.amazon.smithy.java.http.client.HttpClient.builder(),
                                                      builder.http2Enabled,
                                                      options)
                                               .build();
    }

    public static Builder builder() {
        return new DefaultBuilder();
    }

    /**
     * Creates a client with default settings.
     */
    public static SdkHttpClient create() {
        return new DefaultBuilder().build();
    }

    @Override
    public ExecutableHttpRequest prepareRequest(HttpExecuteRequest request) {
        MetricCollector metricCollector = request.metricCollector().orElseGet(NoOpMetricCollector::create);
        metricCollector.reportMetric(HTTP_CLIENT_NAME, clientName());

        HttpRequest smithyRequest = toSmithyRequest(request);
        boolean headRequest = request.httpRequest().method() == SdkHttpMethod.HEAD;

        // Held so abort() can tear down an in-flight or completed exchange from another thread.
        AtomicReference<HttpResponse> inFlight = new AtomicReference<>();
        AtomicBoolean aborted = new AtomicBoolean(false);

        return new ExecutableHttpRequest() {
            @Override
            public HttpExecuteResponse call() throws IOException {
                HttpResponse response;
                try {
                    response = smithyClient.send(smithyRequest);
                } catch (IOException e) {
                    throw unwrapHandshakeFailure(e);
                }
                inFlight.set(response);
                if (aborted.get()) {
                    // Lost a race with abort(); abort() could not have closed this response because it was
                    // not published yet, so close it here rather than handing back a response nobody owns.
                    closeQuietly(response);
                    throw new IOException("Request aborted");
                }
                return toSdkResponse(response, headRequest);
            }

            @Override
            public void abort() {
                aborted.set(true);
                closeQuietly(inFlight.getAndSet(null));
            }
        };
    }

    @Override
    public String clientName() {
        return CLIENT_NAME;
    }

    @Override
    public void close() {
        IoUtils.closeQuietly(smithyClient, null);
    }

    private static HttpRequest toSmithyRequest(HttpExecuteRequest request) {
        SdkHttpRequest sdkRequest = request.httpRequest();
        ModifiableHttpRequest smithyRequest = HttpRequest.create()
                                                        .setMethod(sdkRequest.method().name())
                                                        .setUri(SmithyUri.of(sdkRequest.getUri()));
        // forEachHeader rather than headers(): the latter deep-copies every header on every request, which is
        // exactly the per-request cost this client exists to avoid.
        sdkRequest.forEachHeader(smithyRequest.headers()::setHeader);
        smithyRequest.setBody(toDataStream(request));
        return smithyRequest;
    }

    /**
     * Wraps the request body so smithy can write it.
     *
     * <p>Prefers the provider's already-buffered representation when it has one: the SDK marshals into a byte
     * array, and handing smithy a {@code ByteBuffer} lets it write straight to the socket rather than pumping
     * an {@code InputStream}. Falls back to a stream, supplying the length from {@code Content-Length} so the
     * request can use a fixed length rather than chunked encoding.
     */
    private static DataStream toDataStream(HttpExecuteRequest request) {
        if (!request.contentStreamProvider().isPresent()) {
            return DataStream.ofEmpty();
        }
        ByteBuffer buffered = request.contentStreamProvider().get().contentAsByteBufferOrNull();
        if (buffered != null) {
            return DataStream.ofByteBuffer(buffered);
        }
        InputStream body = request.contentStreamProvider().get().newStream();
        long contentLength = request.httpRequest()
                                    .firstMatchingHeader("Content-Length")
                                    .map(Long::parseLong)
                                    .orElse(-1L);
        return contentLength >= 0
               ? DataStream.ofInputStream(body, null, contentLength)
               : DataStream.ofInputStream(body);
    }

    private static HttpExecuteResponse toSdkResponse(HttpResponse response, boolean headRequest) {
        SdkHttpResponse.Builder sdkResponse = SdkHttpResponse.builder().statusCode(response.statusCode());
        HttpHeaders headers = response.headers();
        headers.forEachEntry(sdkResponse::appendHeader);

        HttpExecuteResponse.Builder builder = HttpExecuteResponse.builder().response(sdkResponse.build());

        if (headRequest) {
            // A HEAD response carries no body even when it reports a Content-Length, so there is nothing to
            // expose and nothing to read. Reading it would block waiting for bytes that never arrive.
            closeQuietly(response);
        } else {
            builder.responseBody(toResponseBody(response));
        }

        return builder.build();
    }

    /**
     * Exposes the response body, ensuring that closing it releases smithy's connection back to the pool.
     *
     * <p>A body of known zero length is drained and released immediately, so the caller never holds a
     * connection open for a response that has nothing in it.
     */
    private static AbortableInputStream toResponseBody(HttpResponse response) {
        DataStream body = response.body();
        if (body.contentLength() == 0) {
            closeQuietly(response);
            return AbortableInputStream.create(new ByteArrayInputStream(new byte[0]));
        }

        InputStream stream = body.asInputStream();
        // close() has to reach the response too: the stream alone does not release the pooled connection.
        InputStream releasing = new ConnectionReleasingInputStream(stream, response);
        return AbortableInputStream.create(releasing, () -> closeQuietly(response));
    }

    private static void closeQuietly(AutoCloseable closeable) {
        IoUtils.closeQuietlyV2(closeable, log);
    }

    /**
     * Surfaces a TLS failure as a handshake failure rather than as the connect failure smithy wraps it in.
     *
     * <p>smithy reports a failure to reach any resolved address as a generic {@link IOException}. Callers above
     * the HTTP layer, including the SDK's retry policy, distinguish a certificate problem from a connectivity
     * problem by exception type, so the TLS failure is pulled back out of the cause chain.
     *
     * <p>Any {@link SSLException} counts, not just {@link SSLHandshakeException}: an untrusted issuer arrives as a
     * handshake exception, but a name mismatch arrives as {@link javax.net.ssl.SSLPeerUnverifiedException}, and both
     * are certificate problems that must not be reported as connectivity problems. A non-handshake SSL failure is
     * rewrapped rather than returned as-is so callers can keep matching on the one type. Suppressed exceptions are
     * searched too, since a multi-address connect attempt can attach failures either way.
     */
    private static IOException unwrapHandshakeFailure(IOException e) {
        SSLException ssl = findSslFailure(e, 0);
        if (ssl == null) {
            return e;
        }
        if (ssl instanceof SSLHandshakeException) {
            return ssl;
        }
        SSLHandshakeException wrapped = new SSLHandshakeException(ssl.getMessage());
        wrapped.initCause(ssl);
        return wrapped;
    }

    private static SSLException findSslFailure(Throwable t, int depth) {
        if (t == null || depth > 10) {
            return null;
        }
        if (t instanceof SSLException) {
            return (SSLException) t;
        }
        for (Throwable suppressed : t.getSuppressed()) {
            SSLException found = findSslFailure(suppressed, depth + 1);
            if (found != null) {
                return found;
            }
        }
        return t.getCause() == t ? null : findSslFailure(t.getCause(), depth + 1);
    }

    /**
     * Closes the underlying HTTP response when the body stream is closed, so the connection returns to the
     * pool instead of being held until GC.
     */
    private static final class ConnectionReleasingInputStream extends java.io.FilterInputStream {
        private final HttpResponse response;

        private ConnectionReleasingInputStream(InputStream delegate, HttpResponse response) {
            super(delegate);
            this.response = response;
        }

        @Override
        public void close() throws IOException {
            try {
                super.close();
            } finally {
                closeQuietly(response);
            }
        }
    }

    /**
     * Builder for {@link SmithyHttpClient}.
     */
    public interface Builder extends SdkHttpClient.Builder<SmithyHttpClient.Builder> {

        /**
         * The amount of time to wait for data on an established connection before timing out.
         */
        Builder socketTimeout(Duration socketTimeout);

        /**
         * The amount of time to wait when establishing a connection before timing out.
         */
        Builder connectionTimeout(Duration connectionTimeout);

        /**
         * The amount of time to wait for a connection from the pool before timing out.
         */
        Builder connectionAcquisitionTimeout(Duration connectionAcquisitionTimeout);

        /**
         * The maximum number of connections allowed in the pool.
         */
        Builder maxConnections(Integer maxConnections);

        /**
         * The maximum amount of time a connection may sit idle in the pool.
         */
        Builder connectionMaxIdleTime(Duration connectionMaxIdleTime);

        /**
         * Forces HTTP/1.1 or allows HTTP/2 negotiation. Defaults to smithy's own policy.
         */
        Builder http2Enabled(Boolean http2Enabled);
    }

    private static final class DefaultBuilder implements Builder {
        private final AttributeMap.Builder standardOptions = AttributeMap.builder();
        private Boolean http2Enabled;

        private DefaultBuilder() {
        }

        @Override
        public Builder socketTimeout(Duration socketTimeout) {
            standardOptions.put(SdkHttpConfigurationOption.READ_TIMEOUT, socketTimeout);
            return this;
        }

        @Override
        public Builder connectionTimeout(Duration connectionTimeout) {
            standardOptions.put(SdkHttpConfigurationOption.CONNECTION_TIMEOUT, connectionTimeout);
            return this;
        }

        @Override
        public Builder connectionAcquisitionTimeout(Duration connectionAcquisitionTimeout) {
            standardOptions.put(SdkHttpConfigurationOption.CONNECTION_ACQUIRE_TIMEOUT, connectionAcquisitionTimeout);
            return this;
        }

        @Override
        public Builder maxConnections(Integer maxConnections) {
            standardOptions.put(SdkHttpConfigurationOption.MAX_CONNECTIONS, maxConnections);
            return this;
        }

        @Override
        public Builder connectionMaxIdleTime(Duration connectionMaxIdleTime) {
            standardOptions.put(SdkHttpConfigurationOption.CONNECTION_MAX_IDLE_TIMEOUT, connectionMaxIdleTime);
            return this;
        }

        @Override
        public Builder http2Enabled(Boolean http2Enabled) {
            this.http2Enabled = http2Enabled;
            return this;
        }

        @Override
        public SdkHttpClient buildWithDefaults(AttributeMap serviceDefaults) {
            AttributeMap resolvedOptions = standardOptions.build()
                                                          .merge(serviceDefaults)
                                                          .merge(SdkHttpConfigurationOption.GLOBAL_HTTP_DEFAULTS);
            return new SmithyHttpClient(this, resolvedOptions);
        }
    }
}
