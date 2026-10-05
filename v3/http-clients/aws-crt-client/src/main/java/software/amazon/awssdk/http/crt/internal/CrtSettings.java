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

package software.amazon.awssdk.http.crt.internal;

import java.time.Duration;
import java.util.EnumSet;
import java.util.Set;
import software.amazon.awssdk.core.exception.SdkClientConfigurationException;
import software.amazon.awssdk.http.HttpClientCapabilities;
import software.amazon.awssdk.http.HttpClientOption;
import software.amazon.awssdk.http.HttpTransportMode;
import software.amazon.awssdk.http.Protocol;
import software.amazon.awssdk.http.crt.ProxyConfiguration;
import software.amazon.awssdk.http.crt.TcpKeepAliveConfiguration;
import software.amazon.awssdk.http.provider.HttpClientConfiguration;

/**
 * The mutable settings shared by the sync and async CRT builders, their V2 defaults, and the capabilities they
 * declare. Not thread safe.
 */
public final class CrtSettings {
    /** V2 {@code AwsCrtHttpClientBase.DEFAULT_STREAM_WINDOW_SIZE}: the initial per-stream read window, 16 MiB. */
    public static final long DEFAULT_READ_BUFFER_SIZE = 16L * 1024 * 1024;
    /** V2 default {@code maxConcurrency} ({@code SdkHttpConfigurationOption.MAX_CONNECTIONS}). */
    public static final int DEFAULT_MAX_CONCURRENCY = 50;
    /** V2 CRT default TLS handshake timeout (10 s, kept for compatibility). */
    public static final Duration DEFAULT_TLS_NEGOTIATION_TIMEOUT = Duration.ofSeconds(10);
    /** V2 default connection timeout. */
    public static final Duration DEFAULT_CONNECTION_TIMEOUT = Duration.ofSeconds(2);
    /** V2 default connection acquisition timeout. */
    public static final Duration DEFAULT_CONNECTION_ACQUISITION_TIMEOUT = Duration.ofSeconds(10);
    /** V2 default connection max idle time. */
    public static final Duration DEFAULT_CONNECTION_MAX_IDLE_TIME = Duration.ofSeconds(60);
    /**
     * The documented cancellation timeout: cancel/abort signals the caller at once on the cancelling thread and the
     * native stream is cancelled asynchronously; this bounds both, with slack for a loaded event loop.
     */
    public static final Duration CANCELLATION_TIMEOUT = Duration.ofSeconds(2);

    /** The options both CRT modes support, exactly. */
    public static final Set<HttpClientOption> SUPPORTED_OPTIONS =
        Set.copyOf(EnumSet.of(HttpClientOption.CONNECTION_TIMEOUT, HttpClientOption.CONNECTION_ACQUISITION_TIMEOUT,
                              HttpClientOption.TLS_NEGOTIATION_TIMEOUT, HttpClientOption.TRUST_ALL_CERTIFICATES,
                              HttpClientOption.PROXY, HttpClientOption.PROXY_CREDENTIALS,
                              HttpClientOption.MAX_CONNECTIONS, HttpClientOption.CONNECTION_MAX_IDLE_TIME,
                              HttpClientOption.TCP_KEEP_ALIVE));

    public Integer maxConcurrency;
    public Duration connectionTimeout;
    public Duration connectionAcquisitionTimeout;
    public Duration connectionMaxIdleTime;
    public Duration tlsNegotiationTimeout;
    public Long readBufferSizeInBytes;
    public ProxyConfiguration proxyConfiguration;
    public TcpKeepAliveConfiguration tcpKeepAliveConfiguration;
    public Boolean postQuantumTlsEnabled;
    public Boolean trustAllCertificates;
    public String trustedCertificatesPem;
    public Integer numEventLoopThreads;
    public CrtNativeAvailability.Probe nativeProbe = CrtNativeAvailability.DEFAULT_PROBE;

    /** The capabilities of a client of {@code mode} with {@code readBufferSize} as its window. */
    public static HttpClientCapabilities capabilities(HttpTransportMode mode, long readBufferSize) {
        return HttpClientCapabilities.builder().mode(mode).addProtocol(Protocol.HTTP1_1)
                                     .options(SUPPORTED_OPTIONS)
                                     .maxBufferedBytesPerExchange(readBufferSize)
                                     .cancellationTimeout(CANCELLATION_TIMEOUT).build();
    }

    public long readBufferSize() {
        return readBufferSizeInBytes == null ? DEFAULT_READ_BUFFER_SIZE : readBufferSizeInBytes;
    }

    public int maxConcurrencyOrDefault() {
        return maxConcurrency == null ? DEFAULT_MAX_CONCURRENCY : maxConcurrency;
    }

    public Duration connectionTimeoutOrDefault() {
        return connectionTimeout == null ? DEFAULT_CONNECTION_TIMEOUT : connectionTimeout;
    }

    public Duration acquisitionTimeoutOrDefault() {
        return connectionAcquisitionTimeout == null ? DEFAULT_CONNECTION_ACQUISITION_TIMEOUT
                                                    : connectionAcquisitionTimeout;
    }

    public Duration maxIdleTimeOrDefault() {
        return connectionMaxIdleTime == null ? DEFAULT_CONNECTION_MAX_IDLE_TIME : connectionMaxIdleTime;
    }

    public Duration tlsNegotiationTimeoutOrDefault() {
        return tlsNegotiationTimeout == null ? DEFAULT_TLS_NEGOTIATION_TIMEOUT : tlsNegotiationTimeout;
    }

    /** Validates value ranges, naming the V2 option at fault. */
    public void validate() {
        positive(connectionTimeout, HttpClientOption.CONNECTION_TIMEOUT.configurationName());
        positive(connectionAcquisitionTimeout, HttpClientOption.CONNECTION_ACQUISITION_TIMEOUT.configurationName());
        positive(connectionMaxIdleTime, HttpClientOption.CONNECTION_MAX_IDLE_TIME.configurationName());
        positive(tlsNegotiationTimeout, HttpClientOption.TLS_NEGOTIATION_TIMEOUT.configurationName());
        if (maxConcurrency != null && maxConcurrency < 1) {
            throw invalid("maxConcurrency", "is not positive: " + maxConcurrency);
        }
        if (readBufferSizeInBytes != null && (readBufferSizeInBytes < 1 || readBufferSizeInBytes > Integer.MAX_VALUE)) {
            throw invalid("readBufferSizeInBytes", "must be 1.." + Integer.MAX_VALUE + ": " + readBufferSizeInBytes);
        }
        if (numEventLoopThreads != null && numEventLoopThreads < 1) {
            throw invalid("numEventLoopThreads", "is not positive: " + numEventLoopThreads);
        }
    }

    /** Applies the options of a provider-level {@code configuration}; each value must have the documented type. */
    public void apply(HttpClientConfiguration configuration) {
        try {
            configuration.value(HttpClientOption.CONNECTION_TIMEOUT, Duration.class)
                         .ifPresent(v -> connectionTimeout = v);
            configuration.value(HttpClientOption.CONNECTION_ACQUISITION_TIMEOUT, Duration.class)
                         .ifPresent(v -> connectionAcquisitionTimeout = v);
            configuration.value(HttpClientOption.CONNECTION_MAX_IDLE_TIME, Duration.class)
                         .ifPresent(v -> connectionMaxIdleTime = v);
            configuration.value(HttpClientOption.TLS_NEGOTIATION_TIMEOUT, Duration.class)
                         .ifPresent(v -> tlsNegotiationTimeout = v);
            configuration.value(HttpClientOption.TRUST_ALL_CERTIFICATES, Boolean.class)
                         .ifPresent(v -> trustAllCertificates = v);
            configuration.value(HttpClientOption.MAX_CONNECTIONS, Integer.class).ifPresent(v -> maxConcurrency = v);
            configuration.value(HttpClientOption.PROXY, ProxyConfiguration.class)
                         .ifPresent(v -> proxyConfiguration = v);
            configuration.value(HttpClientOption.PROXY_CREDENTIALS, ProxyConfiguration.class)
                         .ifPresent(v -> proxyConfiguration = v);
            configuration.value(HttpClientOption.TCP_KEEP_ALIVE, TcpKeepAliveConfiguration.class)
                         .ifPresent(v -> tcpKeepAliveConfiguration = v);
        } catch (IllegalArgumentException e) {
            throw SdkClientConfigurationException.builder().message(e.getMessage()).cause(e).build();
        }
    }

    private static void positive(Duration d, String name) {
        if (d != null && (d.isNegative() || d.isZero())) {
            throw invalid(name, "is not positive: " + d);
        }
    }

    private static SdkClientConfigurationException invalid(String option, String problem) {
        return SdkClientConfigurationException.builder().option(option).message(option + " " + problem).build();
    }
}
