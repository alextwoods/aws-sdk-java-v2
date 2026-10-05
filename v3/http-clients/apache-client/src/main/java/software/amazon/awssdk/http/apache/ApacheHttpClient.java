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

package software.amazon.awssdk.http.apache;

import java.time.Duration;
import java.util.Objects;
import java.util.function.Consumer;
import software.amazon.awssdk.http.HttpClientCapabilities;
import software.amazon.awssdk.http.HttpClientOption;
import software.amazon.awssdk.http.HttpExecutionOptions;
import software.amazon.awssdk.http.HttpTransportMode;
import software.amazon.awssdk.http.Protocol;
import software.amazon.awssdk.http.SdkHttpRequest;
import software.amazon.awssdk.http.SdkSyncHttpClient;
import software.amazon.awssdk.http.SyncHttpExchange;
import software.amazon.awssdk.http.apache.internal.ApacheEngine;

/**
 * The native V3 sync transport over Apache HttpClient 4 (Requirements 12.1-12.10), with the builder names of the V2
 * {@code ApacheHttpClient}. It drives Apache's connection manager directly (lease, connect, tunnel, TLS upgrade,
 * send, receive), not {@code HttpClientBuilder}, so no {@link java.net.URI} is ever built from the request and every
 * step has an exact {@link software.amazon.awssdk.http.HttpPhase}; see
 * {@link software.amazon.awssdk.http.apache.internal.ApacheFailureMapper} for the failure table.
 *
 * <p>Capabilities ({@link #CAPABILITIES}): HTTP/1.1 only; the connection, acquisition and read (socket) timeouts
 * (the latter two also per exchange through {@link HttpExecutionOptions}); TLS key and trust managers; an explicit
 * plain-HTTP proxy with basic credentials (sent preemptively) and non-proxy hosts, tunnelling HTTPS with
 * {@code CONNECT}; pool size, time to live, idle time and the idle reaper; TCP keep-alive; {@code Expect:
 * 100-continue}. Every other {@link HttpClientOption} is rejected at construction by name. Write and TLS-handshake
 * timeouts, trust-all, proxy settings from the environment and HTTP/2 are not supported, as in V2.
 *
 * <p>Buffer bound (Requirement 12.6): {@value ApacheEngine#MAX_BUFFERED_BYTES_PER_EXCHANGE} bytes per exchange in
 * each direction (8 KiB session buffers, a 2 KiB chunk cache and an 8 KiB copy buffer, plus margin).
 *
 * <p>Cancellation: {@link SyncHttpExchange#abort()} cancels a pending lease, closes a connecting socket or shuts the
 * leased connection down, so a blocked call fails with {@code CANCELLED} within
 * {@link HttpClientCapabilities#cancellationTimeout()} (2 s).
 *
 * <p>Connections: a response body read to its end returns the connection to the pool (keep-alive capped by
 * {@code connectionMaxIdleTime}); a body closed early, an abort, or a failure shuts the connection down instead of
 * draining it (Requirement 16.9). {@link #close()} is idempotent, aborts exchanges in flight with
 * {@code CLIENT_CLOSED}, stops the idle reaper and shuts the pool down.
 *
 * <p>The request is sent as built: {@code METHOD rawTarget HTTP/1.1} (absolute form only through a plain proxy) and
 * every header in order, with repeats and case. The transport adds only what is absent: {@code Host},
 * {@code Content-Length} or {@code Transfer-Encoding: chunked}, {@code Expect: 100-continue} when enabled and
 * {@code Proxy-Authorization}. There are no redirects, retries, cookies, compression or default {@code User-Agent}.
 */
public final class ApacheHttpClient implements SdkSyncHttpClient {
    /** The client name, as in V2. */
    public static final String CLIENT_NAME = "Apache";
    /** The documented cancellation timeout. */
    public static final Duration CANCELLATION_TIMEOUT = Duration.ofSeconds(2);
    /** What every Apache 4 client supports. */
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
                              .maxBufferedBytesPerExchange(ApacheEngine.MAX_BUFFERED_BYTES_PER_EXCHANGE)
                              .cancellationTimeout(CANCELLATION_TIMEOUT)
                              .build();

    private final ApacheEngine engine;

    private ApacheHttpClient(ApacheEngine engine) {
        this.engine = engine;
    }

    /** A builder with V2's defaults. */
    public static Builder builder() {
        return new DefaultBuilder(new ApacheSettings());
    }

    /** A client with V2's defaults. */
    public static ApacheHttpClient create() {
        return builder().build();
    }

    /** Validates {@code settings}, then acquires the pool and the reaper, registering each with {@code registrar}. */
    static ApacheHttpClient create(ApacheSettings settings, Consumer<AutoCloseable> registrar) {
        return new ApacheHttpClient(ApacheEngine.create(CLIENT_NAME, settings.toEngineSettings(), registrar));
    }

    @Override
    public SyncHttpExchange prepare(SdkHttpRequest request, HttpExecutionOptions options) {
        Objects.requireNonNull(request, "request");
        Objects.requireNonNull(options, "options");
        options.connectionAcquisitionTimeout()
               .ifPresent(d -> ApacheSettings.requirePositive(HttpClientOption.CONNECTION_ACQUISITION_TIMEOUT, d));
        options.readTimeout().ifPresent(d -> ApacheSettings.requirePositive(HttpClientOption.READ_TIMEOUT, d));
        return engine.prepare(request, options);
    }

    /** The pool's leased and available connection counts, for tests. */
    int[] poolStats() {
        int available = engine.availableConnections();
        return new int[] {engine.pooledConnections() - available, available};
    }

    ApacheEngine engine() {
        return engine;
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
        engine.close();
    }

    @Override
    public String toString() {
        return "ApacheHttpClient[" + (engine.isClosed() ? "closed" : "open") + "]";
    }

    /** Builder of {@link ApacheHttpClient}, named as the V2 {@code ApacheHttpClient.Builder}; not thread safe. */
    public interface Builder {
        /** Time between response bytes; default 30 s. */
        Builder socketTimeout(Duration socketTimeout);

        /** Time to establish a connection; default 2 s. */
        Builder connectionTimeout(Duration connectionTimeout);

        /** Time to wait for a pooled connection; default 10 s. */
        Builder connectionAcquisitionTimeout(Duration connectionAcquisitionTimeout);

        /** The maximum number of pooled connections; default 50. */
        Builder maxConnections(Integer maxConnections);

        /** An explicit HTTP proxy. */
        Builder proxyConfiguration(ProxyConfiguration proxyConfiguration);

        /** {@code Expect: 100-continue} for non-empty request bodies; default false. */
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
        Builder tlsKeyManagersProvider(TlsKeyManagersProvider tlsKeyManagersProvider);

        /** Trust managers replacing the JDK defaults. */
        Builder tlsTrustManagersProvider(TlsTrustManagersProvider tlsTrustManagersProvider);

        /** A new client; the caller owns and closes it. */
        ApacheHttpClient build();
    }

    static final class DefaultBuilder implements Builder {
        private final ApacheSettings settings;

        DefaultBuilder(ApacheSettings settings) {
            this.settings = settings;
        }

        @Override
        public Builder socketTimeout(Duration socketTimeout) {
            settings.socketTimeout = ApacheSettings.requirePositive(HttpClientOption.READ_TIMEOUT, socketTimeout);
            return this;
        }

        @Override
        public Builder connectionTimeout(Duration connectionTimeout) {
            settings.connectionTimeout =
                ApacheSettings.requirePositive(HttpClientOption.CONNECTION_TIMEOUT, connectionTimeout);
            return this;
        }

        @Override
        public Builder connectionAcquisitionTimeout(Duration connectionAcquisitionTimeout) {
            settings.connectionAcquisitionTimeout =
                ApacheSettings.requirePositive(HttpClientOption.CONNECTION_ACQUISITION_TIMEOUT,
                                               connectionAcquisitionTimeout);
            return this;
        }

        @Override
        public Builder maxConnections(Integer maxConnections) {
            settings.maxConnections = ApacheSettings.requirePositive(HttpClientOption.MAX_CONNECTIONS, maxConnections);
            return this;
        }

        @Override
        public Builder proxyConfiguration(ProxyConfiguration proxyConfiguration) {
            settings.proxyConfiguration = proxyConfiguration;
            return this;
        }

        @Override
        public Builder expectContinueEnabled(Boolean expectContinueEnabled) {
            settings.expectContinueEnabled = Boolean.TRUE.equals(expectContinueEnabled);
            return this;
        }

        @Override
        public Builder connectionTimeToLive(Duration connectionTimeToLive) {
            settings.connectionTimeToLive = connectionTimeToLive == null ? null
                : ApacheSettings.requirePositive(HttpClientOption.CONNECTION_TIME_TO_LIVE, connectionTimeToLive);
            return this;
        }

        @Override
        public Builder connectionMaxIdleTime(Duration maxIdleConnectionTimeout) {
            settings.connectionMaxIdleTime =
                ApacheSettings.requirePositive(HttpClientOption.CONNECTION_MAX_IDLE_TIME, maxIdleConnectionTimeout);
            return this;
        }

        @Override
        public Builder useIdleConnectionReaper(Boolean useConnectionReaper) {
            settings.useIdleConnectionReaper = !Boolean.FALSE.equals(useConnectionReaper);
            return this;
        }

        @Override
        public Builder tcpKeepAlive(Boolean keepConnectionAlive) {
            settings.tcpKeepAlive = Boolean.TRUE.equals(keepConnectionAlive);
            return this;
        }

        @Override
        public Builder tlsKeyManagersProvider(TlsKeyManagersProvider tlsKeyManagersProvider) {
            settings.tlsKeyManagersProvider = tlsKeyManagersProvider;
            return this;
        }

        @Override
        public Builder tlsTrustManagersProvider(TlsTrustManagersProvider tlsTrustManagersProvider) {
            settings.tlsTrustManagersProvider = tlsTrustManagersProvider;
            return this;
        }

        @Override
        public ApacheHttpClient build() {
            return create(settings.copy(), resource -> { });
        }
    }
}
