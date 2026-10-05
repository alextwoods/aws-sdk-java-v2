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

import java.io.IOException;
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
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClientBuilder;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManager;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManagerBuilder;
import org.apache.hc.core5.http.ClassicHttpRequest;
import org.apache.hc.core5.http.ClassicHttpResponse;
import org.apache.hc.core5.http.HttpException;
import org.apache.hc.core5.http.impl.io.HttpRequestExecutor;
import org.apache.hc.core5.http.io.HttpClientConnection;
import org.apache.hc.core5.http.io.HttpResponseInformationCallback;
import org.apache.hc.core5.http.io.SocketConfig;
import org.apache.hc.core5.http.protocol.HttpContext;
import org.apache.hc.core5.io.CloseMode;
import software.amazon.awssdk.http.HttpClientCapabilities;
import software.amazon.awssdk.http.HttpClientOption;
import software.amazon.awssdk.http.HttpExecutionOptions;
import software.amazon.awssdk.http.HttpPhase;
import software.amazon.awssdk.http.HttpTransportMode;
import software.amazon.awssdk.http.Protocol;
import software.amazon.awssdk.http.SdkHttpRequest;
import software.amazon.awssdk.http.SdkSyncHttpClient;
import software.amazon.awssdk.http.SyncHttpExchange;

/**
 * The native V3 sync transport over Apache HttpClient 5 classic I/O (Requirements 12.1-12.10), with the builder
 * names of the V2 {@code Apache5HttpClient}.
 *
 * <p>Capabilities ({@link #CAPABILITIES}): HTTP/1.1 only (the classic engine does not negotiate HTTP/2); the
 * connection, acquisition and read timeouts (the latter two also per exchange through {@link HttpExecutionOptions});
 * TLS key and trust managers; an explicit proxy with basic credentials and non-proxy hosts; pool size, time to live,
 * idle time and the idle reaper; TCP keep-alive; {@code Expect: 100-continue}. Every other
 * {@link HttpClientOption} is rejected at construction by name. Write and TLS-handshake timeouts are not supported,
 * as in V2.
 *
 * <p>Buffer bound (Requirement 12.6): {@value #MAX_BUFFERED_BYTES} bytes per exchange in each direction: the engine
 * reads the response into its 8 KiB session buffer ahead of the reader, and writes the request body through a 4 KiB
 * copy buffer plus the 8 KiB session output buffer; the rest is margin.
 *
 * <p>Cancellation: {@link SyncHttpExchange#abort()} cancels a pending lease or closes the socket immediately, so a
 * blocked call fails with {@code CANCELLED} within {@link HttpClientCapabilities#cancellationTimeout()} (2 s).
 *
 * <p>Connections: a response body read to its end and closed returns the connection to the pool; a body closed
 * early, an abort, or a failure discards the connection instead of draining it (Requirement 16.9).
 * {@link #close()} is idempotent, aborts exchanges in flight and closes the pool.
 *
 * <p>The request is sent as built: the method, the raw target ({@link SdkHttpRequest#rawTarget()}, never re-encoded)
 * and every header in order, except that {@code Content-Length} and {@code Transfer-Encoding} are framed by the
 * engine from the body (a {@code Content-Length} header sets the declared body length). The engine adds only
 * {@code Host} when absent; redirects, retries, cookies, compression, authentication caching and the default
 * {@code User-Agent} are disabled.
 */
public final class Apache5HttpClient implements SdkSyncHttpClient {
    /** The client name, as in V2. */
    public static final String CLIENT_NAME = "Apache5";
    /** The documented per-exchange buffer bound, in bytes. */
    public static final long MAX_BUFFERED_BYTES = 64 * 1024;
    /** The documented cancellation timeout. */
    public static final Duration CANCELLATION_TIMEOUT = Duration.ofSeconds(2);
    /** What every sync Apache 5 client supports. */
    public static final HttpClientCapabilities CAPABILITIES =
        HttpClientCapabilities.builder().mode(HttpTransportMode.SYNC).addProtocol(Protocol.HTTP1_1)
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
                                         HttpClientOption.EXPECT_CONTINUE)
                              .maxBufferedBytesPerExchange(MAX_BUFFERED_BYTES)
                              .cancellationTimeout(CANCELLATION_TIMEOUT)
                              .build();

    static final String PHASE_ATTRIBUTE = Apache5HttpClient.class.getName() + ".phase";

    private final CloseableHttpClient client;
    private final PoolingHttpClientConnectionManager connectionManager;
    private final RequestConfig requestConfig;
    private final AtomicBoolean closed = new AtomicBoolean();
    private final Set<Apache5SyncExchange> inFlight = ConcurrentHashMap.newKeySet();

    private Apache5HttpClient(Apache5Settings settings, Apache5Settings.Registrar registrar) {
        ConnectionConfig.Builder connectionConfig =
            ConnectionConfig.custom().setConnectTimeout(Apache5Settings.timeout(settings.connectionTimeout))
                            .setSocketTimeout(Apache5Settings.timeout(settings.readTimeout));
        if (settings.connectionTimeToLive != null) {
            connectionConfig.setTimeToLive(Apache5Settings.timeValue(settings.connectionTimeToLive));
        }
        PoolingHttpClientConnectionManagerBuilder manager =
            PoolingHttpClientConnectionManagerBuilder.create()
                                                     .setTlsSocketStrategy(settings.tlsStrategy())
                                                     .setMaxConnTotal(settings.maxConnections)
                                                     .setMaxConnPerRoute(settings.maxConnections)
                                                     .setDefaultConnectionConfig(connectionConfig.build())
                                                     .setDefaultSocketConfig(
                                                         SocketConfig.custom()
                                                                     .setSoKeepAlive(settings.tcpKeepAlive)
                                                                     .setTcpNoDelay(true).build());
        this.connectionManager = registrar.acquired(manager.build());
        this.requestConfig = RequestConfig.custom()
                                          .setConnectionRequestTimeout(
                                              Apache5Settings.timeout(settings.connectionAcquisitionTimeout))
                                          .setResponseTimeout(Apache5Settings.timeout(settings.readTimeout))
                                          .setExpectContinueEnabled(settings.expectContinueEnabled)
                                          .setRedirectsEnabled(false)
                                          .setProtocolUpgradeEnabled(false)
                                          .build();
        HttpClientBuilder builder =
            HttpClientBuilder.create().setConnectionManager(connectionManager).setConnectionManagerShared(false)
                             .setRequestExecutor(new PhaseTrackingExecutor()).setDefaultRequestConfig(requestConfig)
                             .disableRedirectHandling().disableAutomaticRetries().disableCookieManagement()
                             .disableAuthCaching().disableContentCompression().disableDefaultUserAgent();
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
    }

    /** A builder with V2's defaults. */
    public static Builder builder() {
        return new DefaultBuilder(new Apache5Settings());
    }

    /** A client with V2's defaults. */
    public static Apache5HttpClient create() {
        return builder().build();
    }

    static Apache5HttpClient create(Apache5Settings settings, Apache5Settings.Registrar registrar) {
        return new Apache5HttpClient(settings, registrar);
    }

    @Override
    public SyncHttpExchange prepare(SdkHttpRequest request, HttpExecutionOptions options) {
        Objects.requireNonNull(request, "request");
        Objects.requireNonNull(options, "options");
        if (closed.get()) {
            throw new IllegalStateException("The " + CLIENT_NAME + " HTTP client is closed");
        }
        return new Apache5SyncExchange(this, request, perExchange(options));
    }

    private RequestConfig perExchange(HttpExecutionOptions options) {
        if (options.connectionAcquisitionTimeout().isEmpty() && options.readTimeout().isEmpty()) {
            return requestConfig;
        }
        RequestConfig.Builder b = RequestConfig.copy(requestConfig);
        options.connectionAcquisitionTimeout().ifPresent(d -> b.setConnectionRequestTimeout(
            Apache5Settings.timeout(Apache5Settings.requirePositive(HttpClientOption.CONNECTION_ACQUISITION_TIMEOUT,
                                                                    d))));
        options.readTimeout().ifPresent(d -> b.setResponseTimeout(
            Apache5Settings.timeout(Apache5Settings.requirePositive(HttpClientOption.READ_TIMEOUT, d))));
        return b.build();
    }

    CloseableHttpClient engine() {
        return client;
    }

    boolean isClosed() {
        return closed.get();
    }

    /** Tracks {@code exchange} while it holds or waits for a connection; false if the client is closed. */
    boolean track(Apache5SyncExchange exchange) {
        inFlight.add(exchange);
        if (closed.get()) {
            inFlight.remove(exchange);
            return false;
        }
        return true;
    }

    void untrack(Apache5SyncExchange exchange) {
        inFlight.remove(exchange);
    }

    /** The pool's leased and available connection counts, for tests. */
    int[] poolStats() {
        var stats = connectionManager.getTotalStats();
        return new int[] {stats.getLeased(), stats.getAvailable()};
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
        for (Apache5SyncExchange exchange : inFlight) {
            exchange.abort();
        }
        client.close(CloseMode.IMMEDIATE);
        connectionManager.close(CloseMode.IMMEDIATE);
    }

    @Override
    public String toString() {
        return "Apache5HttpClient[" + (closed.get() ? "closed" : "open") + "]";
    }

    /** Records the transmission and response phases on the exchange's tracker. */
    private static final class PhaseTrackingExecutor extends HttpRequestExecutor {
        @Override
        public ClassicHttpResponse execute(ClassicHttpRequest request, HttpClientConnection conn,
                                           HttpResponseInformationCallback informationCallback, HttpContext context)
            throws IOException, HttpException {
            if (context.getAttribute(PHASE_ATTRIBUTE) instanceof Apache5SyncExchange.PhaseTracker tracker) {
                tracker.enter(request.getEntity() == null ? HttpPhase.RESPONSE_CONSUMPTION
                                                          : HttpPhase.REQUEST_TRANSMISSION);
            }
            return super.execute(request, conn, informationCallback, context);
        }
    }

    /** Builder of {@link Apache5HttpClient}, named as the V2 {@code Apache5HttpClient.Builder}; not thread safe. */
    public interface Builder {
        /** Time between response bytes (V2 {@code socketTimeout}); default 30 s. */
        Builder socketTimeout(Duration socketTimeout);

        /** Time to establish a connection; default 2 s. */
        Builder connectionTimeout(Duration connectionTimeout);

        /** Time to wait for a pooled connection; default 10 s. */
        Builder connectionAcquisitionTimeout(Duration connectionAcquisitionTimeout);

        /** The maximum number of pooled connections; default 50. */
        Builder maxConnections(Integer maxConnections);

        /** An explicit proxy. */
        Builder proxyConfiguration(ProxyConfiguration proxyConfiguration);

        /** {@code Expect: 100-continue} for request bodies; default false. */
        Builder expectContinueEnabled(Boolean expectContinueEnabled);

        /** The maximum lifetime of a pooled connection; default unlimited. */
        Builder connectionTimeToLive(Duration connectionTimeToLive);

        /** The maximum idle time of a pooled connection; default 60 s. */
        Builder connectionMaxIdleTime(Duration maxIdleConnectionTimeout);

        /** Background eviction of idle connections; default true. */
        Builder useIdleConnectionReaper(Boolean useConnectionReaper);

        /** TCP keep-alive; default false. */
        Builder tcpKeepAlive(Boolean keepConnectionAlive);

        /** Key managers for client certificates. */
        Builder tlsKeyManagersProvider(Supplier<KeyManager[]> tlsKeyManagersProvider);

        /** Trust managers replacing the JDK defaults. */
        Builder tlsTrustManagersProvider(Supplier<TrustManager[]> tlsTrustManagersProvider);

        /** A new client; the caller owns and closes it. */
        Apache5HttpClient build();
    }

    static final class DefaultBuilder implements Builder {
        private final Apache5Settings settings;

        DefaultBuilder(Apache5Settings settings) {
            this.settings = settings;
        }

        @Override
        public Builder socketTimeout(Duration socketTimeout) {
            settings.readTimeout = Apache5Settings.requirePositive(HttpClientOption.READ_TIMEOUT, socketTimeout);
            return this;
        }

        @Override
        public Builder connectionTimeout(Duration connectionTimeout) {
            settings.connectionTimeout =
                Apache5Settings.requirePositive(HttpClientOption.CONNECTION_TIMEOUT, connectionTimeout);
            return this;
        }

        @Override
        public Builder connectionAcquisitionTimeout(Duration connectionAcquisitionTimeout) {
            settings.connectionAcquisitionTimeout =
                Apache5Settings.requirePositive(HttpClientOption.CONNECTION_ACQUISITION_TIMEOUT,
                                                connectionAcquisitionTimeout);
            return this;
        }

        @Override
        public Builder maxConnections(Integer maxConnections) {
            settings.maxConnections = Apache5Settings.requirePositive(HttpClientOption.MAX_CONNECTIONS, maxConnections);
            return this;
        }

        @Override
        public Builder proxyConfiguration(ProxyConfiguration proxyConfiguration) {
            settings.proxyConfiguration = proxyConfiguration;
            return this;
        }

        @Override
        public Builder expectContinueEnabled(Boolean expectContinueEnabled) {
            settings.expectContinueEnabled = expectContinueEnabled;
            return this;
        }

        @Override
        public Builder connectionTimeToLive(Duration connectionTimeToLive) {
            settings.connectionTimeToLive = connectionTimeToLive == null ? null
                : Apache5Settings.requirePositive(HttpClientOption.CONNECTION_TIME_TO_LIVE, connectionTimeToLive);
            return this;
        }

        @Override
        public Builder connectionMaxIdleTime(Duration maxIdleConnectionTimeout) {
            settings.connectionMaxIdleTime =
                Apache5Settings.requirePositive(HttpClientOption.CONNECTION_MAX_IDLE_TIME, maxIdleConnectionTimeout);
            return this;
        }

        @Override
        public Builder useIdleConnectionReaper(Boolean useConnectionReaper) {
            settings.useIdleConnectionReaper = useConnectionReaper;
            return this;
        }

        @Override
        public Builder tcpKeepAlive(Boolean keepConnectionAlive) {
            settings.tcpKeepAlive = keepConnectionAlive;
            return this;
        }

        @Override
        public Builder tlsKeyManagersProvider(Supplier<KeyManager[]> tlsKeyManagersProvider) {
            settings.tlsKeyManagersProvider = tlsKeyManagersProvider;
            return this;
        }

        @Override
        public Builder tlsTrustManagersProvider(Supplier<TrustManager[]> tlsTrustManagersProvider) {
            settings.tlsTrustManagersProvider = tlsTrustManagersProvider;
            return this;
        }

        @Override
        public Apache5HttpClient build() {
            return new Apache5HttpClient(settings.copy(), Apache5Settings.Registrar.NONE);
        }
    }
}
