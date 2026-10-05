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

package software.amazon.awssdk.http.urlconnection;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.InetSocketAddress;
import java.net.MalformedURLException;
import java.net.Proxy;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.cert.X509Certificate;
import java.time.Duration;
import java.util.Base64;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.KeyManager;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import software.amazon.awssdk.http.HttpClientCapabilities;
import software.amazon.awssdk.http.HttpClientOption;
import software.amazon.awssdk.http.HttpExecutionOptions;
import software.amazon.awssdk.http.HttpTransportMode;
import software.amazon.awssdk.http.Protocol;
import software.amazon.awssdk.http.SdkHttpRequest;
import software.amazon.awssdk.http.SdkSyncHttpClient;
import software.amazon.awssdk.http.SyncHttpExchange;
import software.amazon.awssdk.http.provider.HttpClientConfiguration;

/**
 * The native V3 sync transport over the JDK's {@link HttpURLConnection} (Requirements 12.1, 12.9, 16.9). It has no
 * engine dependency and no connection pool of its own: connections are reused by the JDK's keep-alive cache
 * ({@code http.keepAlive}, {@code http.maxConnections} system properties).
 *
 * <p>Supported options ({@link #CAPABILITIES}), with their V2 builder names: {@code connectionTimeout},
 * {@code socketTimeout} ({@link HttpClientOption#READ_TIMEOUT}, also overridable per exchange through
 * {@link HttpExecutionOptions#readTimeout()}), {@code tlsKeyManagersProvider}, {@code tlsTrustManagersProvider},
 * {@code trustAllCertificates}, {@code proxyConfiguration} with credentials and non-proxy hosts. Everything else
 * fails construction with the option's V2 name: connection acquisition, write and TLS negotiation timeouts, every
 * pool option ({@code maxConnections}, {@code maxPendingConnectionAcquires}, {@code connectionTimeToLive},
 * {@code connectionMaxIdleTime}, {@code useIdleConnectionReaper}), {@code tcpKeepAlive},
 * {@code expectContinueEnabled}, proxy settings from the environment, the protocol version and every HTTP/2 option.
 * Only HTTP/1.1 is negotiated. Without a {@link ProxyConfiguration} connections are direct
 * ({@link Proxy#NO_PROXY}): the JDK {@link java.net.ProxySelector} and {@code http.proxyHost} properties are not
 * consulted, unlike V2, because proxy settings from the environment are an unsupported option. A per-exchange {@link HttpExecutionOptions#writeTimeout()} or
 * {@link HttpExecutionOptions#connectionAcquisitionTimeout()} fails {@link #prepare} for the same reason.
 *
 * <p>Header, target and method fidelity limits are documented on {@link UrlConnectionRequestPlan}; stream-close,
 * abort and cancellation behavior on {@link UrlConnectionExchange}; failure categories on
 * {@link UrlConnectionFailures}.
 *
 * <p>Buffering: request bodies are copied through one {@value #COPY_BUFFER_SIZE}-byte buffer into the JDK's fixed-length
 * or chunked ({@value #CHUNK_SIZE}-byte chunks) streaming mode, never buffered whole; responses are read through the
 * JDK's buffered socket stream. The documented bound is {@link #MAX_BUFFERED_BYTES} per direction.
 *
 * <p>Thread safe. {@link #close()} is idempotent and fails exchanges in flight with {@code CANCELLED}.
 */
public final class UrlConnectionHttpClient implements SdkSyncHttpClient {
    /** {@link #clientName()}. */
    public static final String CLIENT_NAME = "UrlConnection";

    static final int COPY_BUFFER_SIZE = 8 * 1024;
    static final int CHUNK_SIZE = 8 * 1024;
    /** The documented per-direction buffer bound, in bytes. */
    public static final long MAX_BUFFERED_BYTES = 64 * 1024;
    /** The documented cancellation timeout (see {@link UrlConnectionExchange} for the blocked-read limit). */
    public static final Duration CANCELLATION_TIMEOUT = Duration.ofSeconds(2);

    /** Supported options (see the class documentation). */
    public static final Set<HttpClientOption> SUPPORTED_OPTIONS = Set.of(
        HttpClientOption.CONNECTION_TIMEOUT, HttpClientOption.READ_TIMEOUT, HttpClientOption.TLS_KEY_MANAGERS,
        HttpClientOption.TLS_TRUST_MANAGERS, HttpClientOption.TRUST_ALL_CERTIFICATES, HttpClientOption.PROXY,
        HttpClientOption.PROXY_CREDENTIALS, HttpClientOption.NON_PROXY_HOSTS);

    /** The capabilities of every client of this implementation. */
    public static final HttpClientCapabilities CAPABILITIES = HttpClientCapabilities
        .builder()
        .mode(HttpTransportMode.SYNC)
        .addProtocol(Protocol.HTTP1_1)
        .options(SUPPORTED_OPTIONS)
        .maxBufferedBytesPerExchange(MAX_BUFFERED_BYTES)
        .cancellationTimeout(CANCELLATION_TIMEOUT)
        .build();

    /** V2 defaults ({@code SdkHttpConfigurationOption}). */
    static final Duration DEFAULT_CONNECTION_TIMEOUT = Duration.ofSeconds(2);
    static final Duration DEFAULT_SOCKET_TIMEOUT = Duration.ofSeconds(30);

    private final Duration connectionTimeout;
    private final Duration socketTimeout;
    private final ProxyConfiguration proxy;
    private final String proxyAuthorization;
    private final SSLSocketFactory socketFactory;
    private final HostnameVerifier hostnameVerifier;
    private final boolean allowRestrictedHeaders;
    private final AtomicBoolean closed = new AtomicBoolean();
    private final Set<UrlConnectionExchange> inFlight = java.util.concurrent.ConcurrentHashMap.newKeySet();

    private UrlConnectionHttpClient(Builder builder) {
        this.connectionTimeout = positive(builder.connectionTimeout, DEFAULT_CONNECTION_TIMEOUT, "connectionTimeout");
        this.socketTimeout = positive(builder.socketTimeout, DEFAULT_SOCKET_TIMEOUT, "socketTimeout");
        this.proxy = builder.proxyConfiguration;
        this.proxyAuthorization = proxy != null && proxy.hasCredentials()
                                  ? "Basic " + Base64.getEncoder().encodeToString(
                                      (proxy.username() + ":" + proxy.password()).getBytes(StandardCharsets.UTF_8))
                                  : null;
        if (builder.trustAllCertificates && builder.tlsTrustManagersProvider != null) {
            throw new IllegalArgumentException("A tlsTrustManagersProvider can't be provided if trustAllCertificates "
                                               + "is also set");
        }
        if (builder.tlsKeyManagersProvider != null || builder.tlsTrustManagersProvider != null
            || builder.trustAllCertificates) {
            KeyManager[] keyManagers = builder.tlsKeyManagersProvider == null
                                       ? null : builder.tlsKeyManagersProvider.keyManagers();
            TrustManager[] trustManagers = builder.trustAllCertificates
                                           ? new TrustManager[] {TrustAll.INSTANCE}
                                           : builder.tlsTrustManagersProvider == null
                                             ? null : builder.tlsTrustManagersProvider.trustManagers();
            try {
                SSLContext context = SSLContext.getInstance("TLS");
                context.init(keyManagers, trustManagers, null);
                this.socketFactory = context.getSocketFactory();
            } catch (GeneralSecurityException e) {
                throw new IllegalArgumentException("The TLS configuration is invalid: " + e.getMessage(), e);
            }
        } else {
            this.socketFactory = null;
        }
        this.hostnameVerifier = builder.trustAllCertificates ? (host, session) -> true : null;
        this.allowRestrictedHeaders = Boolean.getBoolean("sun.net.http.allowRestrictedHeaders");
    }

    public static Builder builder() {
        return new Builder();
    }

    /** A client with the V2 defaults: 2 s connection timeout, 30 s socket timeout, JDK TLS defaults, no proxy. */
    public static UrlConnectionHttpClient create() {
        return builder().build();
    }

    /**
     * A client configured by {@code configuration}, whose options must be among {@link #SUPPORTED_OPTIONS}.
     * Accepted values: {@link Duration} for the timeouts; {@link TlsKeyManagersProvider} or {@code KeyManager[]};
     * {@link TlsTrustManagersProvider} or {@code TrustManager[]}; {@link Boolean} for trust-all; a
     * {@link ProxyConfiguration} for {@code PROXY}. {@code PROXY_CREDENTIALS} and {@code NON_PROXY_HOSTS} are read
     * from that {@link ProxyConfiguration}; their own values are ignored, and configuring them requires {@code PROXY}.
     *
     * @throws IllegalArgumentException if a value has the wrong type or the options contradict each other
     */
    public static UrlConnectionHttpClient fromConfiguration(HttpClientConfiguration configuration) {
        Builder b = builder();
        configuration.value(HttpClientOption.CONNECTION_TIMEOUT, Duration.class).ifPresent(b::connectionTimeout);
        configuration.value(HttpClientOption.READ_TIMEOUT, Duration.class).ifPresent(b::socketTimeout);
        configuration.value(HttpClientOption.TLS_KEY_MANAGERS).ifPresent(v -> {
            if (v instanceof KeyManager[] managers) {
                b.tlsKeyManagersProvider(() -> managers.clone());
            } else if (v instanceof TlsKeyManagersProvider provider) {
                b.tlsKeyManagersProvider(provider);
            } else {
                throw wrongType(HttpClientOption.TLS_KEY_MANAGERS, v, "TlsKeyManagersProvider or KeyManager[]");
            }
        });
        configuration.value(HttpClientOption.TLS_TRUST_MANAGERS).ifPresent(v -> {
            if (v instanceof TrustManager[] managers) {
                b.tlsTrustManagersProvider(() -> managers.clone());
            } else if (v instanceof TlsTrustManagersProvider provider) {
                b.tlsTrustManagersProvider(provider);
            } else {
                throw wrongType(HttpClientOption.TLS_TRUST_MANAGERS, v, "TlsTrustManagersProvider or TrustManager[]");
            }
        });
        configuration.value(HttpClientOption.TRUST_ALL_CERTIFICATES, Boolean.class).ifPresent(b::trustAllCertificates);
        ProxyConfiguration proxy = configuration.value(HttpClientOption.PROXY, ProxyConfiguration.class).orElse(null);
        if (proxy == null && (configuration.isConfigured(HttpClientOption.PROXY_CREDENTIALS)
                              || configuration.isConfigured(HttpClientOption.NON_PROXY_HOSTS))) {
            throw new IllegalArgumentException(HttpClientOption.PROXY_CREDENTIALS.configurationName() + " and "
                                               + HttpClientOption.NON_PROXY_HOSTS.configurationName() + " require "
                                               + HttpClientOption.PROXY.configurationName());
        }
        b.proxyConfiguration(proxy);
        return b.build();
    }

    private static IllegalArgumentException wrongType(HttpClientOption option, Object value, String expected) {
        return new IllegalArgumentException(option.configurationName() + " is a " + value.getClass().getName()
                                            + ", not a " + expected);
    }

    @Override
    public SyncHttpExchange prepare(SdkHttpRequest request, HttpExecutionOptions options) {
        Objects.requireNonNull(request, "request");
        Objects.requireNonNull(options, "options");
        if (closed.get()) {
            throw new IllegalStateException(CLIENT_NAME + " HTTP client is closed");
        }
        if (options.writeTimeout().isPresent() || options.connectionAcquisitionTimeout().isPresent()) {
            HttpClientOption option = options.writeTimeout().isPresent() ? HttpClientOption.WRITE_TIMEOUT
                                                                         : HttpClientOption.CONNECTION_ACQUISITION_TIMEOUT;
            throw new IllegalArgumentException(CLIENT_NAME + " does not support the option "
                                               + option.configurationName());
        }
        UrlConnectionRequestPlan plan = UrlConnectionRequestPlan.of(request, allowRestrictedHeaders);
        Duration readTimeout = options.readTimeout().orElse(socketTimeout);
        return new UrlConnectionExchange(this, request, plan, readTimeout);
    }

    /** Opens (without I/O) and configures the connection of {@code request}. */
    @SuppressWarnings("deprecation") // new URL(...) is the only JDK path that keeps the raw target as given
    HttpURLConnection open(SdkHttpRequest request, UrlConnectionRequestPlan plan, Duration readTimeout,
                           boolean proxied) throws IOException {
        URL url;
        try {
            url = new URL(request.scheme(), request.host(), request.port(), request.rawTarget());
        } catch (MalformedURLException e) {
            throw new IllegalArgumentException("The request target is not a valid URL: " + e.getMessage(), e);
        }
        HttpURLConnection connection = (HttpURLConnection) (proxied
            ? url.openConnection(new Proxy(Proxy.Type.HTTP, InetSocketAddress.createUnresolved(proxy.host(),
                                                                                              proxy.port())))
            : url.openConnection(Proxy.NO_PROXY));
        if (connection instanceof javax.net.ssl.HttpsURLConnection https) {
            if (socketFactory != null) {
                https.setSSLSocketFactory(socketFactory);
            }
            if (hostnameVerifier != null) {
                https.setHostnameVerifier(hostnameVerifier);
            }
        }
        connection.setConnectTimeout(millis(connectionTimeout));
        connection.setReadTimeout(millis(readTimeout));
        connection.setInstanceFollowRedirects(false);
        connection.setRequestMethod(plan.method);
        for (var header : plan.headers) {
            connection.addRequestProperty(header.getKey(), header.getValue());
        }
        if (proxied && proxyAuthorization != null && !request.headers().contains("Proxy-Authorization")) {
            connection.addRequestProperty("Proxy-Authorization", proxyAuthorization);
        }
        switch (plan.framing) {
            case FIXED -> {
                connection.setDoOutput(true);
                connection.setFixedLengthStreamingMode(plan.fixedLength);
            }
            case CHUNKED -> {
                connection.setDoOutput(true);
                connection.setChunkedStreamingMode(CHUNK_SIZE);
            }
            case NONE -> {
            }
        }
        return connection;
    }

    /** Whether {@code request} goes through the proxy. */
    boolean proxied(SdkHttpRequest request) {
        return proxy != null && proxy.proxies(request.host());
    }

    /** Whether a proxied {@code https} request would need credentials the JDK cannot send in its tunnel. */
    boolean tunnelNeedsCredentials(SdkHttpRequest request) {
        return proxyAuthorization != null && "https".equalsIgnoreCase(request.scheme());
    }

    boolean isClosed() {
        return closed.get();
    }

    void register(UrlConnectionExchange exchange) {
        inFlight.add(exchange);
        if (closed.get()) {
            exchange.abort();
        }
    }

    void unregister(UrlConnectionExchange exchange) {
        inFlight.remove(exchange);
    }

    @Override
    public HttpClientCapabilities capabilities() {
        return CAPABILITIES;
    }

    @Override
    public String clientName() {
        return CLIENT_NAME;
    }

    /** Idempotent; aborts exchanges in flight (they fail with {@code CANCELLED}). */
    @Override
    public void close() {
        if (closed.compareAndSet(false, true)) {
            for (UrlConnectionExchange exchange : inFlight) {
                exchange.abort();
            }
        }
    }

    private static int millis(Duration d) {
        long ms = d.toMillis();
        return ms > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) Math.max(1, ms);
    }

    private static Duration positive(Duration value, Duration dflt, String name) {
        if (value == null) {
            return dflt;
        }
        if (value.isNegative() || value.isZero()) {
            throw new IllegalArgumentException(name + " is not positive: " + value);
        }
        return value;
    }

    /** Builder with the V2 {@code UrlConnectionHttpClient.Builder} names; not thread safe. */
    public static final class Builder {
        private Duration connectionTimeout;
        private Duration socketTimeout;
        private TlsKeyManagersProvider tlsKeyManagersProvider;
        private TlsTrustManagersProvider tlsTrustManagersProvider;
        private boolean trustAllCertificates;
        private ProxyConfiguration proxyConfiguration;

        private Builder() {
        }

        /** Time to establish the TCP connection; default 2 s. */
        public Builder connectionTimeout(Duration connectionTimeout) {
            this.connectionTimeout = connectionTimeout;
            return this;
        }

        /** Time to wait for response bytes (V2 {@code socketTimeout}); default 30 s. */
        public Builder socketTimeout(Duration socketTimeout) {
            this.socketTimeout = socketTimeout;
            return this;
        }

        public Builder tlsKeyManagersProvider(TlsKeyManagersProvider tlsKeyManagersProvider) {
            this.tlsKeyManagersProvider = tlsKeyManagersProvider;
            return this;
        }

        public Builder tlsTrustManagersProvider(TlsTrustManagersProvider tlsTrustManagersProvider) {
            this.tlsTrustManagersProvider = tlsTrustManagersProvider;
            return this;
        }

        /** Disables certificate and host name validation; for testing only. */
        public Builder trustAllCertificates(boolean trustAllCertificates) {
            this.trustAllCertificates = trustAllCertificates;
            return this;
        }

        public Builder proxyConfiguration(ProxyConfiguration proxyConfiguration) {
            this.proxyConfiguration = proxyConfiguration;
            return this;
        }

        public UrlConnectionHttpClient build() {
            return new UrlConnectionHttpClient(this);
        }
    }

    /** Trust manager of {@code trustAllCertificates}. */
    private enum TrustAll implements X509TrustManager {
        INSTANCE;

        @Override
        public void checkClientTrusted(X509Certificate[] chain, String authType) {
        }

        @Override
        public void checkServerTrusted(X509Certificate[] chain, String authType) {
        }

        @Override
        public X509Certificate[] getAcceptedIssuers() {
            return new X509Certificate[0];
        }
    }
}
