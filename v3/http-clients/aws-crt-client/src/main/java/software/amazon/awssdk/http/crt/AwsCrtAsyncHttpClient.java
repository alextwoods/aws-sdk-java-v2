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

package software.amazon.awssdk.http.crt;

import java.time.Duration;
import java.util.Objects;
import java.util.function.Consumer;
import software.amazon.awssdk.http.AsyncHttpExchange;
import software.amazon.awssdk.http.AsyncHttpResponseHandler;
import software.amazon.awssdk.http.HttpClientCapabilities;
import software.amazon.awssdk.http.HttpExecutionOptions;
import software.amazon.awssdk.http.HttpTransportMode;
import software.amazon.awssdk.http.SdkHttpRequest;
import software.amazon.awssdk.http.SdkAsyncHttpClient;
import software.amazon.awssdk.http.crt.internal.CrtAsyncExchange;
import software.amazon.awssdk.http.crt.internal.CrtClientCore;
import software.amazon.awssdk.http.crt.internal.CrtNativeAvailability;
import software.amazon.awssdk.http.crt.internal.CrtSettings;
import software.amazon.awssdk.http.provider.HttpClientConfiguration;

/**
 * The native V3 asynchronous HTTP/1.1 transport over the AWS Common Runtime (V2 name {@code AwsCrtAsyncHttpClient}),
 * implemented directly on the CRT connection manager without V2 adapters.
 *
 * <p>Capabilities ({@link #capabilities()}): mode {@link HttpTransportMode#ASYNC}; protocol HTTP/1.1; options
 * {@code connectionTimeout}, {@code connectionAcquisitionTimeout} (also per exchange), {@code tlsNegotiationTimeout},
 * {@code trustAllCertificates}, {@code proxyConfiguration} (with credentials), {@code maxConnections}
 * ({@code maxConcurrency}), {@code connectionMaxIdleTime} and {@code tcpKeepAlive}. Every other option, including
 * read/write timeouts, custom key/trust managers and HTTP/2, is rejected at construction by provider selection. The
 * buffer bound ({@link HttpClientCapabilities#maxBufferedBytesPerExchange()}) is {@code readBufferSizeInBytes}
 * (default 16 MiB, as in V2), the CRT's initial per-stream window, which is reopened only by consumed bytes. The
 * cancellation timeout is 2 seconds. Handler and body signals run on the CRT event loop and never block; the response
 * body publisher delivers exactly the demanded chunks, and the request body publisher is asked for one chunk at a
 * time, only when the connection has written the previous one. {@link #close()} cancels exchanges in flight with
 * {@link software.amazon.awssdk.http.CancellationReason#CLIENT_CLOSED}.
 *
 * <p>Construction checks that the CRT native library loads and otherwise fails with a
 * {@link software.amazon.awssdk.core.exception.SdkClientConfigurationException}. {@link #close()} releases every
 * native resource the client created exactly once.
 */
public final class AwsCrtAsyncHttpClient implements SdkAsyncHttpClient {
    /** The {@link #clientName()}, as in V2. */
    public static final String CLIENT_NAME = "AwsCommonRuntime";

    private final CrtClientCore core;
    private final HttpClientCapabilities capabilities;

    private AwsCrtAsyncHttpClient(CrtSettings settings, Consumer<AutoCloseable> registrar) {
        settings.validate();
        CrtNativeAvailability.require(settings.nativeProbe, "aws-crt-client (async)");
        this.core = new CrtClientCore(CLIENT_NAME, settings, registrar);
        this.capabilities = CrtSettings.capabilities(HttpTransportMode.ASYNC, core.readBufferSize());
    }

    public static Builder builder() {
        return new Builder();
    }

    /** A client with the V2 defaults. */
    public static AwsCrtAsyncHttpClient create() {
        return builder().build();
    }

    @Override
    public AsyncHttpExchange execute(SdkHttpRequest request, AsyncHttpResponseHandler handler,
                                     HttpExecutionOptions options) {
        Objects.requireNonNull(request, "request");
        Objects.requireNonNull(handler, "handler");
        Objects.requireNonNull(options, "options");
        return new CrtAsyncExchange(core, request, handler).start(options);
    }

    @Override
    public HttpClientCapabilities capabilities() {
        return capabilities;
    }

    @Override
    public String clientName() {
        return CLIENT_NAME;
    }

    /** The native core; for tests of this module. */
    CrtClientCore core() {
        return core;
    }

    @Override
    public void close() {
        core.close();
    }

    /** Builder of an {@link AwsCrtAsyncHttpClient}, with the V2 option names. Not thread safe. */
    public static final class Builder {
        private final CrtSettings settings = new CrtSettings();

        private Builder() {
        }

        /** The maximum number of connections per endpoint (V2 {@code maxConcurrency}; default 50). */
        public Builder maxConcurrency(Integer maxConcurrency) {
            settings.maxConcurrency = maxConcurrency;
            return this;
        }

        /** The TCP connect timeout (default 2 s). */
        public Builder connectionTimeout(Duration connectionTimeout) {
            settings.connectionTimeout = connectionTimeout;
            return this;
        }

        /** The time to wait for a pooled connection (default 10 s). */
        public Builder connectionAcquisitionTimeout(Duration connectionAcquisitionTimeout) {
            settings.connectionAcquisitionTimeout = connectionAcquisitionTimeout;
            return this;
        }

        /** The maximum idle time of a pooled connection (default 60 s). */
        public Builder connectionMaxIdleTime(Duration connectionMaxIdleTime) {
            settings.connectionMaxIdleTime = connectionMaxIdleTime;
            return this;
        }

        /** The TLS handshake timeout (default 10 s, the V2 CRT value). */
        public Builder tlsNegotiationTimeout(Duration tlsNegotiationTimeout) {
            settings.tlsNegotiationTimeout = tlsNegotiationTimeout;
            return this;
        }

        /** The per-stream read window in bytes, which is also the documented buffer bound (default 16 MiB). */
        public Builder readBufferSizeInBytes(Long readBufferSizeInBytes) {
            settings.readBufferSizeInBytes = readBufferSizeInBytes;
            return this;
        }

        public Builder proxyConfiguration(ProxyConfiguration proxyConfiguration) {
            settings.proxyConfiguration = proxyConfiguration;
            return this;
        }

        public Builder tcpKeepAliveConfiguration(TcpKeepAliveConfiguration tcpKeepAliveConfiguration) {
            settings.tcpKeepAliveConfiguration = tcpKeepAliveConfiguration;
            return this;
        }

        /** Post-quantum TLS key exchange; the CRT default is on, and only {@code false} opts out. */
        public Builder postQuantumTlsEnabled(Boolean postQuantumTlsEnabled) {
            settings.postQuantumTlsEnabled = postQuantumTlsEnabled;
            return this;
        }

        /** Disables certificate validation; for testing only. */
        public Builder trustAllCertificates(Boolean trustAllCertificates) {
            settings.trustAllCertificates = trustAllCertificates;
            return this;
        }

        /**
         * Replaces the platform trust store with the PEM certificates given (CRT specific; the CRT cannot use JSSE
         * trust managers, so {@code tlsTrustManagersProvider} is not supported).
         */
        public Builder tlsTrustedCertificatesPem(String pem) {
            settings.trustedCertificatesPem = pem;
            return this;
        }

        /** A private event-loop group of this many threads; unset uses the CRT's shared default group. */
        public Builder numEventLoopThreads(Integer numEventLoopThreads) {
            settings.numEventLoopThreads = numEventLoopThreads;
            return this;
        }

        /** Applies provider-level options (see {@link AwsCrtSdkAsyncHttpService}). */
        public Builder applyConfiguration(HttpClientConfiguration configuration) {
            settings.apply(configuration);
            return this;
        }

        Builder nativeProbe(CrtNativeAvailability.Probe probe) {
            settings.nativeProbe = Objects.requireNonNull(probe, "probe");
            return this;
        }

        public AwsCrtAsyncHttpClient build() {
            return new AwsCrtAsyncHttpClient(settings, r -> { });
        }

        AwsCrtAsyncHttpClient build(Consumer<AutoCloseable> registrar) {
            return new AwsCrtAsyncHttpClient(settings, registrar);
        }
    }
}
