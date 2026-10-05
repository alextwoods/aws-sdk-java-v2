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
import software.amazon.awssdk.core.exception.SdkClientConfigurationException;
import software.amazon.awssdk.http.HttpClientOption;
import software.amazon.awssdk.http.apache.internal.EngineSettings;
import software.amazon.awssdk.http.apache.internal.ProxySettings;
import software.amazon.awssdk.http.provider.HttpClientConfiguration;

/**
 * The builder state of {@link ApacheHttpClient}, with V2's defaults: connection timeout 2 s, acquisition timeout
 * 10 s, socket (read) timeout 30 s, 50 connections, idle time 60 s with the reaper enabled, no time to live, no TCP
 * keep-alive, no {@code Expect: 100-continue}. Not thread safe.
 */
final class ApacheSettings {
    static final Duration DEFAULT_CONNECTION_TIMEOUT = Duration.ofSeconds(2);
    static final Duration DEFAULT_ACQUISITION_TIMEOUT = Duration.ofSeconds(10);
    static final Duration DEFAULT_SOCKET_TIMEOUT = Duration.ofSeconds(30);
    static final Duration DEFAULT_MAX_IDLE_TIME = Duration.ofSeconds(60);
    static final int DEFAULT_MAX_CONNECTIONS = 50;

    Duration connectionTimeout = DEFAULT_CONNECTION_TIMEOUT;
    Duration connectionAcquisitionTimeout = DEFAULT_ACQUISITION_TIMEOUT;
    Duration socketTimeout = DEFAULT_SOCKET_TIMEOUT;
    int maxConnections = DEFAULT_MAX_CONNECTIONS;
    Duration connectionTimeToLive;
    Duration connectionMaxIdleTime = DEFAULT_MAX_IDLE_TIME;
    boolean useIdleConnectionReaper = true;
    boolean tcpKeepAlive;
    boolean expectContinueEnabled;
    ProxyConfiguration proxyConfiguration;
    TlsKeyManagersProvider tlsKeyManagersProvider;
    TlsTrustManagersProvider tlsTrustManagersProvider;

    ApacheSettings copy() {
        ApacheSettings c = new ApacheSettings();
        c.connectionTimeout = connectionTimeout;
        c.connectionAcquisitionTimeout = connectionAcquisitionTimeout;
        c.socketTimeout = socketTimeout;
        c.maxConnections = maxConnections;
        c.connectionTimeToLive = connectionTimeToLive;
        c.connectionMaxIdleTime = connectionMaxIdleTime;
        c.useIdleConnectionReaper = useIdleConnectionReaper;
        c.tcpKeepAlive = tcpKeepAlive;
        c.expectContinueEnabled = expectContinueEnabled;
        c.proxyConfiguration = proxyConfiguration;
        c.tlsKeyManagersProvider = tlsKeyManagersProvider;
        c.tlsTrustManagersProvider = tlsTrustManagersProvider;
        return c;
    }

    /**
     * Settings from a provider configuration. Value types: {@link Duration} for timeouts, time to live and idle time;
     * {@link Integer} for {@code MAX_CONNECTIONS}; {@link Boolean} for the reaper, TCP keep-alive and
     * {@code Expect: 100-continue}; {@link ProxyConfiguration} for {@code PROXY} (which also carries
     * {@code PROXY_CREDENTIALS} and {@code NON_PROXY_HOSTS}); {@link TlsKeyManagersProvider} and
     * {@link TlsTrustManagersProvider} for the TLS managers. Reads values only; acquires nothing.
     *
     * @throws SdkClientConfigurationException naming the option for a value of the wrong type or range
     */
    static ApacheSettings from(HttpClientConfiguration configuration) {
        ApacheSettings s = new ApacheSettings();
        s.connectionTimeout = positive(configuration, HttpClientOption.CONNECTION_TIMEOUT, s.connectionTimeout);
        s.connectionAcquisitionTimeout =
            positive(configuration, HttpClientOption.CONNECTION_ACQUISITION_TIMEOUT, s.connectionAcquisitionTimeout);
        s.socketTimeout = positive(configuration, HttpClientOption.READ_TIMEOUT, s.socketTimeout);
        s.connectionTimeToLive = positive(configuration, HttpClientOption.CONNECTION_TIME_TO_LIVE, null);
        s.connectionMaxIdleTime =
            positive(configuration, HttpClientOption.CONNECTION_MAX_IDLE_TIME, s.connectionMaxIdleTime);
        Integer max = value(configuration, HttpClientOption.MAX_CONNECTIONS, Integer.class);
        if (max != null) {
            s.maxConnections = requirePositive(HttpClientOption.MAX_CONNECTIONS, max);
        }
        s.useIdleConnectionReaper = bool(configuration, HttpClientOption.REAP_IDLE_CONNECTIONS, true);
        s.tcpKeepAlive = bool(configuration, HttpClientOption.TCP_KEEP_ALIVE, false);
        s.expectContinueEnabled = bool(configuration, HttpClientOption.EXPECT_CONTINUE, false);
        s.proxyConfiguration = value(configuration, HttpClientOption.PROXY, ProxyConfiguration.class);
        if (s.proxyConfiguration == null && (configuration.isConfigured(HttpClientOption.PROXY_CREDENTIALS)
                                             || configuration.isConfigured(HttpClientOption.NON_PROXY_HOSTS))) {
            throw invalid(HttpClientOption.PROXY, "proxy credentials and non-proxy hosts are part of the "
                                                  + "ProxyConfiguration value of");
        }
        s.tlsKeyManagersProvider = value(configuration, HttpClientOption.TLS_KEY_MANAGERS,
                                         TlsKeyManagersProvider.class);
        s.tlsTrustManagersProvider = value(configuration, HttpClientOption.TLS_TRUST_MANAGERS,
                                           TlsTrustManagersProvider.class);
        return s;
    }

    /** The resolved engine settings; calls the TLS providers once. */
    EngineSettings toEngineSettings() {
        ProxySettings proxy = null;
        if (proxyConfiguration != null) {
            proxy = new ProxySettings(proxyConfiguration.host(), proxyConfiguration.port(),
                                      proxyConfiguration.username().orElse(null),
                                      proxyConfiguration.password().orElse(null),
                                      proxyConfiguration.nonProxyHosts());
        }
        return new EngineSettings(connectionTimeout, socketTimeout, connectionAcquisitionTimeout,
                                  connectionMaxIdleTime,
                                  connectionTimeToLive == null ? Duration.ZERO : connectionTimeToLive,
                                  maxConnections, tcpKeepAlive, useIdleConnectionReaper, expectContinueEnabled,
                                  tlsKeyManagersProvider == null ? null : tlsKeyManagersProvider.keyManagers(),
                                  tlsTrustManagersProvider == null ? null : tlsTrustManagersProvider.trustManagers(),
                                  false, proxy, proxy);
    }

    private static Duration positive(HttpClientConfiguration c, HttpClientOption option, Duration fallback) {
        Duration d = value(c, option, Duration.class);
        return d == null ? fallback : requirePositive(option, d);
    }

    private static boolean bool(HttpClientConfiguration c, HttpClientOption option, boolean fallback) {
        Boolean b = value(c, option, Boolean.class);
        return b == null ? fallback : b;
    }

    private static <T> T value(HttpClientConfiguration c, HttpClientOption option, Class<T> type) {
        Object v = c.value(option).orElse(null);
        if (v == null) {
            return null;
        }
        if (!type.isInstance(v)) {
            throw invalid(option, "expected a " + type.getSimpleName() + " value for");
        }
        return type.cast(v);
    }

    static Duration requirePositive(HttpClientOption option, Duration d) {
        if (d == null || d.isNegative() || d.isZero()) {
            throw invalid(option, "expected a positive duration for");
        }
        return d;
    }

    static int requirePositive(HttpClientOption option, Integer n) {
        if (n == null || n <= 0) {
            throw invalid(option, "expected a positive value for");
        }
        return n;
    }

    static SdkClientConfigurationException invalid(HttpClientOption option, String problem) {
        return SdkClientConfigurationException.builder().option(option.configurationName())
                                              .message("Invalid Apache HTTP client configuration: " + problem + " "
                                                       + option.configurationName() + ".")
                                              .build();
    }
}
