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

package software.amazon.awssdk.http.apache5;

import java.time.Duration;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;
import javax.net.ssl.KeyManager;
import javax.net.ssl.TrustManager;
import org.apache.hc.client5.http.config.ConnectionConfig;
import org.apache.hc.client5.http.config.RequestConfig;
import org.apache.hc.client5.http.config.TlsConfig;
import org.apache.hc.client5.http.impl.async.CloseableHttpAsyncClient;
import org.apache.hc.client5.http.impl.async.HttpAsyncClientBuilder;
import org.apache.hc.client5.http.impl.nio.PoolingAsyncClientConnectionManager;
import org.apache.hc.client5.http.impl.nio.PoolingAsyncClientConnectionManagerBuilder;
import org.apache.hc.core5.http2.HttpVersionPolicy;
import org.apache.hc.core5.io.CloseMode;
import org.apache.hc.core5.reactor.IOReactorConfig;
import software.amazon.awssdk.http.AsyncHttpExchange;
import software.amazon.awssdk.http.AsyncHttpResponseHandler;
import software.amazon.awssdk.http.CancellationReason;
import software.amazon.awssdk.http.HttpClientCapabilities;
import software.amazon.awssdk.http.HttpClientOption;
import software.amazon.awssdk.http.HttpExecutionOptions;
import software.amazon.awssdk.http.HttpTransportMode;
import software.amazon.awssdk.http.Protocol;
import software.amazon.awssdk.http.SdkAsyncHttpClient;
import software.amazon.awssdk.http.SdkHttpRequest;

/**
 * The isolated async transport of {@code apache5-client} over the Apache HttpClient 5 async I/O reactor. V2 has no
 * Apache 5 async client; the design ships this mode only if the parity baseline requires it and benchmarks pass, so
 * its provider ({@link Apache5SdkAsyncHttpService}) is not registered for discovery. Build it explicitly.
 *
 * <p>Capabilities ({@link #CAPABILITIES}): HTTP/1.1 and HTTP/2 ({@code PROTOCOL_VERSION}; HTTP/2 is forced, by prior
 * knowledge over {@code http} and ALPN over {@code https}); the connection, acquisition and read timeouts; TLS key
 * and trust managers; an explicit proxy with basic credentials and non-proxy hosts; pool size, time to live, idle
 * time and the idle reaper; TCP keep-alive. {@code Expect: 100-continue} and write or handshake timeouts are not
 * supported.
 *
 * <p>Buffer bound: {@value #MAX_BUFFERED_BYTES} bytes per exchange in each direction. The response body publisher
 * grants the connection at most {@value #RESPONSE_WINDOW} bytes beyond demand when the engine asks for capacity, on
 * top of the protocol's initial 64 KiB window and an I/O buffer; the request body holds at most one body chunk (at
 * most 64 KiB for {@code StreamingBodies} bodies).
 *
 * <p>Cancellation: {@link AsyncHttpExchange#cancel} cancels the engine exchange (closing the HTTP/1.1 connection or
 * resetting the HTTP/2 stream) and the request-body subscription, and signals {@code CANCELLED} at once, well within
 * the documented 2 s. {@link #close()} is idempotent and cancels exchanges in flight with
 * {@link CancellationReason#CLIENT_CLOSED}.
 */
public final class Apache5AsyncHttpClient implements SdkAsyncHttpClient {
    /** The client name. */
    public static final String CLIENT_NAME = "Apache5Async";
    /** The documented per-exchange buffer bound, in bytes. */
    public static final long MAX_BUFFERED_BYTES = 256 * 1024;
    /** The response credit granted beyond demand, in bytes. */
    static final int RESPONSE_WINDOW = 64 * 1024;
    /** The documented cancellation timeout. */
    public static final Duration CANCELLATION_TIMEOUT = Duration.ofSeconds(2);
    /** What every async Apache 5 client supports. */
    public static final HttpClientCapabilities CAPABILITIES =
        HttpClientCapabilities.builder().mode(HttpTransportMode.ASYNC).addProtocol(Protocol.HTTP1_1, Protocol.HTTP2)
                              .addOption(HttpClientOption.CONNECTION_TIMEOUT,
                                         HttpClientOption.CONNECTION_ACQUISITION_TIMEOUT,
                                         HttpClientOption.READ_TIMEOUT,
                                         HttpClientOption.TLS_KEY_MANAGERS,
                                         HttpClientOption.TLS_TRUST_MANAGERS,
                                         HttpClientOption.PROXY,
                                         HttpClientOption.PROXY_CREDENTIALS,
                                         HttpClientOption.NON_PROXY_HOSTS,
                                         HttpClientOption.MAX_CONNECTIONS,
                                         HttpClientOption.CONNECTION_TIME_TO_LIVE,
                                         HttpClientOption.CONNECTION_MAX_IDLE_TIME,
                                         HttpClientOption.REAP_IDLE_CONNECTIONS,
                                         HttpClientOption.TCP_KEEP_ALIVE,
                                         HttpClientOption.PROTOCOL_VERSION)
                              .maxBufferedBytesPerExchange(MAX_BUFFERED_BYTES)
                              .cancellationTimeout(CANCELLATION_TIMEOUT)
                              .build();

    private final CloseableHttpAsyncClient client;
    private final PoolingAsyncClientConnectionManager connectionManager;
    private final RequestConfig requestConfig;
    private final AtomicBoolean closed = new AtomicBoolean();
    private final Set<Apache5AsyncExchange> inFlight = ConcurrentHashMap.newKeySet();

    private Apache5AsyncHttpClient(Apache5Settings settings, Apache5Settings.Registrar registrar) {
        HttpVersionPolicy policy =
            settings.protocol == Protocol.HTTP2 ? HttpVersionPolicy.FORCE_HTTP_2 : HttpVersionPolicy.FORCE_HTTP_1;
        ConnectionConfig.Builder connectionConfig =
            ConnectionConfig.custom().setConnectTimeout(Apache5Settings.timeout(settings.connectionTimeout))
                            .setSocketTimeout(Apache5Settings.timeout(settings.readTimeout));
        if (settings.connectionTimeToLive != null) {
            connectionConfig.setTimeToLive(Apache5Settings.timeValue(settings.connectionTimeToLive));
        }
        this.connectionManager = registrar.acquired(
            PoolingAsyncClientConnectionManagerBuilder.create()
                                                      .setTlsStrategy(settings.tlsStrategy())
                                                      .setMaxConnTotal(settings.maxConnections)
                                                      .setMaxConnPerRoute(settings.maxConnections)
                                                      .setDefaultConnectionConfig(connectionConfig.build())
                                                      .setDefaultTlsConfig(TlsConfig.custom().setVersionPolicy(policy)
                                                                                    .build())
                                                      .build());
        this.requestConfig = RequestConfig.custom()
                                          .setConnectionRequestTimeout(
                                              Apache5Settings.timeout(settings.connectionAcquisitionTimeout))
                                          .setResponseTimeout(Apache5Settings.timeout(settings.readTimeout))
                                          .setRedirectsEnabled(false)
                                          .setProtocolUpgradeEnabled(false)
                                          .build();
        HttpAsyncClientBuilder builder =
            HttpAsyncClientBuilder.create().setConnectionManager(connectionManager).setConnectionManagerShared(false)
                                  .setVersionPolicy(policy)
                                  .setIOReactorConfig(IOReactorConfig.custom()
                                                                     .setSoKeepAlive(settings.tcpKeepAlive)
                                                                     .setTcpNoDelay(true).build())
                                  .setDefaultRequestConfig(requestConfig)
                                  .disableRedirectHandling().disableAutomaticRetries().disableCookieManagement()
                                  .disableAuthCaching().disableContentCompression();
        if (settings.routePlanner() != null) {
            builder.setRoutePlanner(settings.routePlanner());
        }
        if (settings.credentialsProvider() != null) {
            builder.setDefaultCredentialsProvider(settings.credentialsProvider());
        }
        if (settings.useIdleConnectionReaper) {
            builder.evictIdleConnections(Apache5Settings.timeValue(settings.connectionMaxIdleTime));
        }
        this.client = registrar.acquired(builder.build());
        client.start();
    }

    /** A builder with the sync client's (V2's) defaults, HTTP/1.1. */
    public static Builder builder() {
        return new DefaultBuilder(new Apache5Settings());
    }

    static Apache5AsyncHttpClient create(Apache5Settings settings, Apache5Settings.Registrar registrar) {
        return new Apache5AsyncHttpClient(settings, registrar);
    }

    @Override
    public AsyncHttpExchange execute(SdkHttpRequest request, AsyncHttpResponseHandler handler,
                                     HttpExecutionOptions options) {
        Objects.requireNonNull(request, "request");
        Objects.requireNonNull(handler, "handler");
        Objects.requireNonNull(options, "options");
        if (closed.get()) {
            throw new IllegalStateException("The " + CLIENT_NAME + " HTTP client is closed");
        }
        Apache5AsyncExchange exchange = new Apache5AsyncExchange(this, request, handler, perExchange(options));
        inFlight.add(exchange);
        if (closed.get()) {
            inFlight.remove(exchange);
            throw new IllegalStateException("The " + CLIENT_NAME + " HTTP client is closed");
        }
        try {
            exchange.start();
        } catch (RuntimeException e) {
            exchange.onFailure(e);
        }
        return exchange.completion();
    }

    private RequestConfig perExchange(HttpExecutionOptions options) {
        if (options.connectionAcquisitionTimeout().isEmpty() && options.readTimeout().isEmpty()) {
            return requestConfig;
        }
        RequestConfig.Builder b = RequestConfig.copy(requestConfig);
        options.connectionAcquisitionTimeout().ifPresent(d -> b.setConnectionRequestTimeout(Apache5Settings.timeout(d)));
        options.readTimeout().ifPresent(d -> b.setResponseTimeout(Apache5Settings.timeout(d)));
        return b.build();
    }

    CloseableHttpAsyncClient engine() {
        return client;
    }

    boolean isClosed() {
        return closed.get();
    }

    void untrack(Apache5AsyncExchange exchange) {
        inFlight.remove(exchange);
    }

    int inFlightCount() {
        return inFlight.size();
    }

    @Override
    public HttpClientCapabilities capabilities() {
        return CAPABILITIES;
    }

    @Override
    public String clientName() {
        return CLIENT_NAME;
    }

    @Override
    public void close() {
        if (!closed.compareAndSet(false, true)) {
            return;
        }
        for (Apache5AsyncExchange exchange : inFlight) {
            exchange.completion().cancel(CancellationReason.CLIENT_CLOSED);
        }
        client.close(CloseMode.IMMEDIATE);
        connectionManager.close(CloseMode.IMMEDIATE);
    }

    @Override
    public String toString() {
        return "Apache5AsyncHttpClient[" + (closed.get() ? "closed" : "open") + "]";
    }

    /** Builder of {@link Apache5AsyncHttpClient}, with the sync builder's V2 names; not thread safe. */
    public interface Builder {
        Builder socketTimeout(Duration socketTimeout);

        Builder connectionTimeout(Duration connectionTimeout);

        Builder connectionAcquisitionTimeout(Duration connectionAcquisitionTimeout);

        Builder maxConnections(Integer maxConnections);

        Builder proxyConfiguration(ProxyConfiguration proxyConfiguration);

        Builder connectionTimeToLive(Duration connectionTimeToLive);

        Builder connectionMaxIdleTime(Duration maxIdleConnectionTimeout);

        Builder useIdleConnectionReaper(Boolean useConnectionReaper);

        Builder tcpKeepAlive(Boolean keepConnectionAlive);

        Builder tlsKeyManagersProvider(Supplier<KeyManager[]> tlsKeyManagersProvider);

        Builder tlsTrustManagersProvider(Supplier<TrustManager[]> tlsTrustManagersProvider);

        /** The protocol version; default {@link Protocol#HTTP1_1}. */
        Builder protocol(Protocol protocol);

        /** A new started client; the caller owns and closes it. */
        Apache5AsyncHttpClient build();
    }

    private static final class DefaultBuilder implements Builder {
        private final Apache5Settings settings;
        private final Apache5HttpClient.DefaultBuilder shared;

        DefaultBuilder(Apache5Settings settings) {
            this.settings = settings;
            this.shared = new Apache5HttpClient.DefaultBuilder(settings);
        }

        @Override
        public Builder socketTimeout(Duration socketTimeout) {
            shared.socketTimeout(socketTimeout);
            return this;
        }

        @Override
        public Builder connectionTimeout(Duration connectionTimeout) {
            shared.connectionTimeout(connectionTimeout);
            return this;
        }

        @Override
        public Builder connectionAcquisitionTimeout(Duration connectionAcquisitionTimeout) {
            shared.connectionAcquisitionTimeout(connectionAcquisitionTimeout);
            return this;
        }

        @Override
        public Builder maxConnections(Integer maxConnections) {
            shared.maxConnections(maxConnections);
            return this;
        }

        @Override
        public Builder proxyConfiguration(ProxyConfiguration proxyConfiguration) {
            shared.proxyConfiguration(proxyConfiguration);
            return this;
        }

        @Override
        public Builder connectionTimeToLive(Duration connectionTimeToLive) {
            shared.connectionTimeToLive(connectionTimeToLive);
            return this;
        }

        @Override
        public Builder connectionMaxIdleTime(Duration maxIdleConnectionTimeout) {
            shared.connectionMaxIdleTime(maxIdleConnectionTimeout);
            return this;
        }

        @Override
        public Builder useIdleConnectionReaper(Boolean useConnectionReaper) {
            shared.useIdleConnectionReaper(useConnectionReaper);
            return this;
        }

        @Override
        public Builder tcpKeepAlive(Boolean keepConnectionAlive) {
            shared.tcpKeepAlive(keepConnectionAlive);
            return this;
        }

        @Override
        public Builder tlsKeyManagersProvider(Supplier<KeyManager[]> tlsKeyManagersProvider) {
            shared.tlsKeyManagersProvider(tlsKeyManagersProvider);
            return this;
        }

        @Override
        public Builder tlsTrustManagersProvider(Supplier<TrustManager[]> tlsTrustManagersProvider) {
            shared.tlsTrustManagersProvider(tlsTrustManagersProvider);
            return this;
        }

        @Override
        public Builder protocol(Protocol protocol) {
            settings.protocol = Objects.requireNonNull(protocol, "protocol");
            return this;
        }

        @Override
        public Apache5AsyncHttpClient build() {
            return new Apache5AsyncHttpClient(settings.copy(), Apache5Settings.Registrar.NONE);
        }
    }
}
