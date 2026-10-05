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

package software.amazon.awssdk.http.nio.netty;

import io.netty.handler.codec.http2.Http2SecurityUtil;
import io.netty.handler.ssl.ApplicationProtocolConfig;
import io.netty.handler.ssl.ApplicationProtocolNames;
import io.netty.handler.ssl.OpenSsl;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.SupportedCipherSuiteFilter;
import java.time.Duration;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import javax.net.ssl.KeyManager;
import javax.net.ssl.SSLException;
import javax.net.ssl.TrustManager;
import software.amazon.awssdk.core.exception.SdkClientConfigurationException;
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
import software.amazon.awssdk.http.nio.netty.internal.ChannelSetup;
import software.amazon.awssdk.http.nio.netty.internal.ClientConfig;
import software.amazon.awssdk.http.nio.netty.internal.Endpoint;
import software.amazon.awssdk.http.nio.netty.internal.EndpointPool;
import software.amazon.awssdk.http.nio.netty.internal.NettyExchange;

/**
 * The native V3 Netty NIO async transport (V2 name and builder names).
 *
 * <p>Protocols: HTTP/1.1, and HTTP/2 over TLS negotiated by ALPN when {@code protocol(HTTP2)} is set (a server that
 * does not select {@code h2} is spoken to in HTTP/1.1; plaintext HTTP/2 is not supported, as in V2 without prior
 * knowledge).
 *
 * <p>Buffer bound ({@link HttpClientCapabilities#maxBufferedBytesPerExchange()}): response data is read only on
 * demand, one read of at most {@link ChannelSetup#MAX_READ_BYTES} bytes at a time, and the request body has one chunk
 * in flight, so an HTTP/1.1 exchange buffers at most {@link #HTTP1_MAX_BUFFERED_BYTES} per direction (one read, plus
 * one TLS record and decoder remainder, or one request chunk of at most 64 KiB for SDK bodies). For HTTP/2 the
 * response bound is the stream window ({@link Http2Configuration#initialWindowSize()}), whichever is larger.
 *
 * <p>Cancellation ({@link #CANCELLATION_TIMEOUT}): {@link AsyncHttpExchange#cancel} completes the exchange at once and
 * hands the teardown (close of the connection or reset of the stream, cancellation of the request-body subscription,
 * the error signal) to the channel's event loop, which runs it as its next task.
 *
 * <p>Event loops: nothing in this client blocks an event loop. Host names are resolved on SDK resolver threads; the
 * pool lock guards only bookkeeping. The only foreign code run on an event loop is the response handler, the
 * response-body subscriber and the request-body subscription given to {@link #execute}.
 *
 * <p>Ownership: an event loop group supplied with {@code eventLoopGroup} is the caller's and is never shut down; one
 * built by the client (default or {@code eventLoopGroupBuilder}) is shut down by {@link #close()}.
 */
public final class NettyNioAsyncHttpClient implements SdkAsyncHttpClient {
    /** {@link #clientName()}. */
    public static final String CLIENT_NAME = "NettyNioAsyncHttpClient";
    /** The HTTP/1.1 per-exchange, per-direction buffer bound, in bytes. */
    public static final long HTTP1_MAX_BUFFERED_BYTES = 256 * 1024;
    /** The documented cancellation timeout. */
    public static final Duration CANCELLATION_TIMEOUT = Duration.ofSeconds(2);

    /** The options this transport supports (declared exactly; see {@link Builder}). */
    static final Set<HttpClientOption> OPTIONS = Set.of(
        HttpClientOption.CONNECTION_TIMEOUT, HttpClientOption.CONNECTION_ACQUISITION_TIMEOUT,
        HttpClientOption.READ_TIMEOUT, HttpClientOption.WRITE_TIMEOUT, HttpClientOption.TLS_NEGOTIATION_TIMEOUT,
        HttpClientOption.TLS_KEY_MANAGERS, HttpClientOption.TLS_TRUST_MANAGERS, HttpClientOption.MAX_CONNECTIONS,
        HttpClientOption.MAX_PENDING_CONNECTION_ACQUIRES, HttpClientOption.CONNECTION_TIME_TO_LIVE,
        HttpClientOption.CONNECTION_MAX_IDLE_TIME, HttpClientOption.TCP_KEEP_ALIVE, HttpClientOption.PROTOCOL_VERSION,
        HttpClientOption.HTTP2_MAX_STREAMS, HttpClientOption.HTTP2_INITIAL_WINDOW_SIZE);

    private final ClientConfig config;
    private final SdkEventLoopGroup group;
    private final boolean ownsGroup;
    private final ChannelSetup setup;
    private final HttpClientCapabilities capabilities;
    private final Map<Endpoint, EndpointPool> pools = new ConcurrentHashMap<>();
    private final Set<AsyncHttpExchange> inFlight = ConcurrentHashMap.newKeySet();
    private final AtomicBoolean closed = new AtomicBoolean();

    private NettyNioAsyncHttpClient(Builder builder, SdkEventLoopGroup group, boolean ownsGroup) {
        this.group = group;
        this.ownsGroup = ownsGroup;
        this.config = new ClientConfig(CLIENT_NAME, group.eventLoopGroup(), builder.maxConcurrency,
                                       builder.maxPendingConnectionAcquires, builder.connectionAcquisitionTimeout,
                                       builder.connectionTimeout, builder.readTimeout, builder.writeTimeout,
                                       builder.tlsNegotiationTimeout, builder.connectionTimeToLive,
                                       builder.connectionMaxIdleTime, builder.tcpKeepAlive, builder.protocol,
                                       builder.http2Configuration.maxStreams(),
                                       builder.http2Configuration.initialWindowSize(), sslContext(builder));
        this.setup = new ChannelSetup(config);
        this.capabilities = capabilities(builder.protocol == Protocol.HTTP2
                                         ? builder.http2Configuration.initialWindowSize() : 0);
    }

    public static Builder builder() {
        return new Builder();
    }

    /** A client with the defaults. */
    public static SdkAsyncHttpClient create() {
        return builder().build();
    }

    /** The capabilities of a client whose HTTP/2 window is {@code http2Window} (0 when HTTP/2 is off). */
    static HttpClientCapabilities capabilities(long http2Window) {
        return HttpClientCapabilities.builder().mode(HttpTransportMode.ASYNC)
                                     .addProtocol(Protocol.HTTP1_1, Protocol.HTTP2).options(OPTIONS)
                                     .maxBufferedBytesPerExchange(Math.max(HTTP1_MAX_BUFFERED_BYTES, http2Window))
                                     .cancellationTimeout(CANCELLATION_TIMEOUT).build();
    }

    @Override
    public AsyncHttpExchange execute(SdkHttpRequest request, AsyncHttpResponseHandler handler,
                                     HttpExecutionOptions options) {
        Objects.requireNonNull(request, "request");
        Objects.requireNonNull(handler, "handler");
        Objects.requireNonNull(options, "options");
        if (closed.get()) {
            throw new IllegalStateException(CLIENT_NAME + " is closed");
        }
        Endpoint endpoint = Endpoint.of(request);
        EndpointPool pool = pools.computeIfAbsent(endpoint, e -> new EndpointPool(e, config, setup));
        NettyExchange exchange = new NettyExchange(
            CLIENT_NAME, request, handler, pool,
            options.connectionAcquisitionTimeout().orElse(config.connectionAcquisitionTimeout()),
            options.readTimeout().orElse(config.readTimeout()), options.writeTimeout().orElse(config.writeTimeout()));
        AsyncHttpExchange handle = exchange.exchange();
        inFlight.add(handle);
        handle.completion().whenComplete((r, e) -> inFlight.remove(handle));
        exchange.start(config.group());
        if (closed.get()) {
            handle.cancel(CancellationReason.CLIENT_CLOSED);
        }
        return handle;
    }

    @Override
    public HttpClientCapabilities capabilities() {
        return capabilities;
    }

    @Override
    public String clientName() {
        return CLIENT_NAME;
    }

    /** Open connections to {@code scheme://host:port}, for tests. */
    int openConnections(boolean secure, String host, int port) {
        EndpointPool pool = pools.get(new Endpoint(secure, host, port));
        return pool == null ? 0 : pool.openConnections();
    }

    /**
     * The connection pools and resolver threads as one resource, registered as SDK-owned by the provider; closing it
     * closes every connection and fails waiting acquisitions. Idempotent.
     */
    AutoCloseable connectionPools() {
        return this::closeConnections;
    }

    private void closeConnections() {
        pools.values().forEach(EndpointPool::close);
        setup.close();
    }

    /** The event loop group, for tests. */
    SdkEventLoopGroup eventLoopGroup() {
        return group;
    }

    /**
     * Cancels exchanges in flight ({@link CancellationReason#CLIENT_CLOSED}), closes every connection, stops the
     * resolver threads and, if the client built its event loop group, starts its shutdown. Idempotent; never waits.
     */
    @Override
    public void close() {
        if (!closed.compareAndSet(false, true)) {
            return;
        }
        for (AsyncHttpExchange exchange : inFlight) {
            exchange.cancel(CancellationReason.CLIENT_CLOSED);
        }
        closeConnections();
        if (ownsGroup) {
            group.close();
        }
    }

    @Override
    public String toString() {
        return CLIENT_NAME + "[" + config.protocol() + (closed.get() ? ", closed" : "") + "]";
    }

    private static SslContext sslContext(Builder builder) {
        try {
            SslContextBuilder ssl = SslContextBuilder.forClient().endpointIdentificationAlgorithm("HTTPS");
            if (builder.sslProvider == SslProvider.OPENSSL) {
                if (!OpenSsl.isAvailable()) {
                    throw SdkClientConfigurationException.builder()
                        .message("sslProvider OPENSSL needs netty-tcnative on the class path").build();
                }
                ssl.sslProvider(io.netty.handler.ssl.SslProvider.OPENSSL);
            } else {
                ssl.sslProvider(io.netty.handler.ssl.SslProvider.JDK);
            }
            TrustManager[] trust = builder.tlsTrustManagersProvider == null
                                   ? null : builder.tlsTrustManagersProvider.trustManagers();
            if (trust != null && trust.length > 0) {
                ssl.trustManager(trust[0]);
            }
            KeyManager[] keys = builder.tlsKeyManagersProvider == null
                                ? null : builder.tlsKeyManagersProvider.keyManagers();
            if (keys != null && keys.length > 0) {
                ssl.keyManager(keys[0]);
            }
            if (builder.protocol == Protocol.HTTP2) {
                ssl.ciphers(Http2SecurityUtil.CIPHERS, SupportedCipherSuiteFilter.INSTANCE)
                   .applicationProtocolConfig(new ApplicationProtocolConfig(
                       ApplicationProtocolConfig.Protocol.ALPN,
                       ApplicationProtocolConfig.SelectorFailureBehavior.NO_ADVERTISE,
                       ApplicationProtocolConfig.SelectedListenerFailureBehavior.ACCEPT,
                       ApplicationProtocolNames.HTTP_2, ApplicationProtocolNames.HTTP_1_1));
            }
            return ssl.build();
        } catch (SSLException e) {
            throw SdkClientConfigurationException.builder().message("Cannot build the TLS context: " + e)
                                                               .cause(e).build();
        }
    }

    /**
     * Builder with the V2 {@code NettyNioAsyncHttpClient.Builder} names. Not thread safe. Defaults are V2's:
     * maxConcurrency 50, maxPendingConnectionAcquires 10000, connectionTimeout 2 s, connectionAcquisitionTimeout
     * 10 s, readTimeout and writeTimeout 30 s, tlsNegotiationTimeout 5 s, connectionMaxIdleTime 60 s, no
     * connectionTimeToLive, tcpKeepAlive off, HTTP/1.1, JDK TLS.
     *
     * <p>Supported (and declared in {@link HttpClientCapabilities#options()}): the setters below. Not supported:
     * proxies ({@code proxyConfiguration}), trust-all certificates, idle-connection reaper toggling (idle connections
     * are always closed after {@code connectionMaxIdleTime}), {@code Expect: 100-continue}, native epoll and the
     * non-blocking DNS resolver option (host names are always resolved off the event loops).
     */
    public static final class Builder {
        private int maxConcurrency = 50;
        private int maxPendingConnectionAcquires = 10_000;
        private Duration connectionTimeout = Duration.ofSeconds(2);
        private Duration connectionAcquisitionTimeout = Duration.ofSeconds(10);
        private Duration readTimeout = Duration.ofSeconds(30);
        private Duration writeTimeout = Duration.ofSeconds(30);
        private Duration tlsNegotiationTimeout = Duration.ofSeconds(5);
        private Duration connectionTimeToLive;
        private Duration connectionMaxIdleTime = Duration.ofSeconds(60);
        private boolean tcpKeepAlive;
        private Protocol protocol = Protocol.HTTP1_1;
        private Http2Configuration http2Configuration = Http2Configuration.defaults();
        private SslProvider sslProvider = SslProvider.JDK;
        private TlsKeyManagersProvider tlsKeyManagersProvider;
        private TlsTrustManagersProvider tlsTrustManagersProvider;
        private SdkEventLoopGroup eventLoopGroup;
        private SdkEventLoopGroup.Builder eventLoopGroupBuilder;

        private Builder() {
        }

        /** Maximum concurrent exchanges (HTTP/1.1 connections, or HTTP/2 streams) per endpoint. */
        public Builder maxConcurrency(Integer maxConcurrency) {
            this.maxConcurrency = positive(maxConcurrency, "maxConcurrency");
            return this;
        }

        /** Maximum exchanges waiting for a connection per endpoint; beyond it acquisition fails at once. */
        public Builder maxPendingConnectionAcquires(Integer maxPendingConnectionAcquires) {
            this.maxPendingConnectionAcquires = positive(maxPendingConnectionAcquires, "maxPendingConnectionAcquires");
            return this;
        }

        public Builder connectionTimeout(Duration connectionTimeout) {
            this.connectionTimeout = positive(connectionTimeout, "connectionTimeout");
            return this;
        }

        public Builder connectionAcquisitionTimeout(Duration connectionAcquisitionTimeout) {
            this.connectionAcquisitionTimeout = positive(connectionAcquisitionTimeout, "connectionAcquisitionTimeout");
            return this;
        }

        /** Time without response bytes while the client waits for the server (not for the body subscriber). */
        public Builder readTimeout(Duration readTimeout) {
            this.readTimeout = positive(readTimeout, "readTimeout");
            return this;
        }

        /** Time for one write to reach the socket. */
        public Builder writeTimeout(Duration writeTimeout) {
            this.writeTimeout = positive(writeTimeout, "writeTimeout");
            return this;
        }

        public Builder tlsNegotiationTimeout(Duration tlsNegotiationTimeout) {
            this.tlsNegotiationTimeout = positive(tlsNegotiationTimeout, "tlsNegotiationTimeout");
            return this;
        }

        /** Maximum connection lifetime; {@code null} for unbounded. */
        public Builder connectionTimeToLive(Duration connectionTimeToLive) {
            this.connectionTimeToLive = connectionTimeToLive == null ? null
                                                                     : positive(connectionTimeToLive,
                                                                                "connectionTimeToLive");
            return this;
        }

        /** Idle connections are closed after this; {@code null} to keep them until the server closes them. */
        public Builder connectionMaxIdleTime(Duration connectionMaxIdleTime) {
            this.connectionMaxIdleTime = connectionMaxIdleTime == null ? null
                                                                       : positive(connectionMaxIdleTime,
                                                                                  "connectionMaxIdleTime");
            return this;
        }

        public Builder tcpKeepAlive(Boolean tcpKeepAlive) {
            this.tcpKeepAlive = Boolean.TRUE.equals(tcpKeepAlive);
            return this;
        }

        /** {@link Protocol#HTTP1_1} (default) or {@link Protocol#HTTP2} (ALPN over TLS). */
        public Builder protocol(Protocol protocol) {
            this.protocol = Objects.requireNonNull(protocol, "protocol");
            return this;
        }

        public Builder http2Configuration(Http2Configuration http2Configuration) {
            this.http2Configuration = Objects.requireNonNull(http2Configuration, "http2Configuration");
            return this;
        }

        public Builder sslProvider(SslProvider sslProvider) {
            this.sslProvider = Objects.requireNonNull(sslProvider, "sslProvider");
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

        /** A caller-owned group; the client never shuts it down. Exclusive with {@link #eventLoopGroupBuilder}. */
        public Builder eventLoopGroup(SdkEventLoopGroup eventLoopGroup) {
            this.eventLoopGroup = eventLoopGroup;
            return this;
        }

        /** Settings of the group the client creates and shuts down. */
        public Builder eventLoopGroupBuilder(SdkEventLoopGroup.Builder eventLoopGroupBuilder) {
            this.eventLoopGroupBuilder = eventLoopGroupBuilder;
            return this;
        }

        /** Builds the client, creating its event loop group unless one was supplied. */
        public NettyNioAsyncHttpClient build() {
            return build(null);
        }

        /** Builds with an SDK-created group registered by {@code created} as soon as it exists. */
        NettyNioAsyncHttpClient build(java.util.function.Consumer<SdkEventLoopGroup> created) {
            if (eventLoopGroup != null && eventLoopGroupBuilder != null) {
                throw SdkClientConfigurationException.builder()
                    .message("eventLoopGroup and eventLoopGroupBuilder are exclusive").build();
            }
            if (eventLoopGroup != null) {
                return new NettyNioAsyncHttpClient(this, eventLoopGroup, false);
            }
            SdkEventLoopGroup group = (eventLoopGroupBuilder != null ? eventLoopGroupBuilder
                                                                     : SdkEventLoopGroup.builder()).build();
            if (created != null) {
                created.accept(group);
            }
            try {
                return new NettyNioAsyncHttpClient(this, group, true);
            } catch (RuntimeException e) {
                group.close();
                throw e;
            }
        }

        private static int positive(Integer value, String name) {
            if (value == null || value <= 0) {
                throw new IllegalArgumentException(name + " is not positive: " + value);
            }
            return value;
        }

        private static Duration positive(Duration value, String name) {
            if (value == null || value.isNegative() || value.isZero()) {
                throw new IllegalArgumentException(name + " is not positive: " + value);
            }
            return value;
        }
    }
}
