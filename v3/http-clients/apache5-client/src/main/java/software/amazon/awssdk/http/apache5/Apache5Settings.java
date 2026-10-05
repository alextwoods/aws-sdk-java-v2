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

import java.security.GeneralSecurityException;
import java.time.Duration;
import java.util.Locale;
import java.util.function.Supplier;
import javax.net.ssl.KeyManager;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import org.apache.hc.client5.http.auth.AuthScope;
import org.apache.hc.client5.http.auth.UsernamePasswordCredentials;
import org.apache.hc.client5.http.impl.DefaultSchemePortResolver;
import org.apache.hc.client5.http.impl.auth.BasicCredentialsProvider;
import org.apache.hc.client5.http.impl.routing.DefaultRoutePlanner;
import org.apache.hc.client5.http.routing.HttpRoutePlanner;
import org.apache.hc.client5.http.ssl.DefaultClientTlsStrategy;
import org.apache.hc.core5.http.HttpHost;
import org.apache.hc.core5.http.protocol.HttpContext;
import org.apache.hc.core5.util.TimeValue;
import org.apache.hc.core5.util.Timeout;
import software.amazon.awssdk.core.exception.SdkClientConfigurationException;
import software.amazon.awssdk.http.HttpClientOption;
import software.amazon.awssdk.http.Protocol;
import software.amazon.awssdk.http.provider.HttpClientConfiguration;

/**
 * The configuration shared by the sync and async Apache 5 builders, with V2's defaults: connection timeout 2 s,
 * acquisition timeout 10 s, read (socket) timeout 30 s, 50 connections, idle time 60 s with the reaper enabled, no
 * time to live, no TCP keep-alive, no {@code Expect: 100-continue}. Not thread safe.
 */
final class Apache5Settings {
    static final Duration DEFAULT_CONNECTION_TIMEOUT = Duration.ofSeconds(2);
    static final Duration DEFAULT_ACQUISITION_TIMEOUT = Duration.ofSeconds(10);
    static final Duration DEFAULT_READ_TIMEOUT = Duration.ofSeconds(30);
    static final Duration DEFAULT_MAX_IDLE_TIME = Duration.ofSeconds(60);
    static final int DEFAULT_MAX_CONNECTIONS = 50;

    Duration connectionTimeout = DEFAULT_CONNECTION_TIMEOUT;
    Duration connectionAcquisitionTimeout = DEFAULT_ACQUISITION_TIMEOUT;
    Duration readTimeout = DEFAULT_READ_TIMEOUT;
    int maxConnections = DEFAULT_MAX_CONNECTIONS;
    Duration connectionTimeToLive;
    Duration connectionMaxIdleTime = DEFAULT_MAX_IDLE_TIME;
    boolean useIdleConnectionReaper = true;
    boolean tcpKeepAlive;
    boolean expectContinueEnabled;
    ProxyConfiguration proxyConfiguration;
    Supplier<KeyManager[]> tlsKeyManagersProvider;
    Supplier<TrustManager[]> tlsTrustManagersProvider;
    Protocol protocol = Protocol.HTTP1_1;

    Apache5Settings copy() {
        Apache5Settings c = new Apache5Settings();
        c.connectionTimeout = connectionTimeout;
        c.connectionAcquisitionTimeout = connectionAcquisitionTimeout;
        c.readTimeout = readTimeout;
        c.maxConnections = maxConnections;
        c.connectionTimeToLive = connectionTimeToLive;
        c.connectionMaxIdleTime = connectionMaxIdleTime;
        c.useIdleConnectionReaper = useIdleConnectionReaper;
        c.tcpKeepAlive = tcpKeepAlive;
        c.expectContinueEnabled = expectContinueEnabled;
        c.proxyConfiguration = proxyConfiguration;
        c.tlsKeyManagersProvider = tlsKeyManagersProvider;
        c.tlsTrustManagersProvider = tlsTrustManagersProvider;
        c.protocol = protocol;
        return c;
    }

    /**
     * Settings from a provider configuration. Value types: {@link Duration} for timeouts, time to live and idle time;
     * {@link Integer} for {@code MAX_CONNECTIONS}; {@link Boolean} for the reaper, TCP keep-alive and
     * {@code Expect: 100-continue}; {@link ProxyConfiguration} for {@code PROXY} (which also carries
     * {@code PROXY_CREDENTIALS} and {@code NON_PROXY_HOSTS}, whose own values are ignored); a
     * {@code Supplier<KeyManager[]>} / {@code Supplier<TrustManager[]>} for the TLS managers; a {@link Protocol} for
     * {@code PROTOCOL_VERSION}. Reads values only; acquires nothing.
     *
     * @throws SdkClientConfigurationException naming the option for a value of the wrong type or range
     */
    static Apache5Settings from(HttpClientConfiguration configuration) {
        Apache5Settings s = new Apache5Settings();
        s.connectionTimeout = positive(configuration, HttpClientOption.CONNECTION_TIMEOUT, s.connectionTimeout);
        s.connectionAcquisitionTimeout =
            positive(configuration, HttpClientOption.CONNECTION_ACQUISITION_TIMEOUT, s.connectionAcquisitionTimeout);
        s.readTimeout = positive(configuration, HttpClientOption.READ_TIMEOUT, s.readTimeout);
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
        s.tlsKeyManagersProvider = supplier(configuration, HttpClientOption.TLS_KEY_MANAGERS);
        s.tlsTrustManagersProvider = supplier(configuration, HttpClientOption.TLS_TRUST_MANAGERS);
        Protocol protocol = value(configuration, HttpClientOption.PROTOCOL_VERSION, Protocol.class);
        if (protocol != null) {
            s.protocol = protocol;
        }
        return s;
    }

    private static Duration positive(HttpClientConfiguration c, HttpClientOption option, Duration fallback) {
        Duration d = value(c, option, Duration.class);
        if (d == null) {
            return fallback;
        }
        return requirePositive(option, d);
    }

    private static boolean bool(HttpClientConfiguration c, HttpClientOption option, boolean fallback) {
        Boolean b = value(c, option, Boolean.class);
        return b == null ? fallback : b;
    }

    @SuppressWarnings("unchecked")
    private static <T> Supplier<T> supplier(HttpClientConfiguration c, HttpClientOption option) {
        return value(c, option, Supplier.class);
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
        if (d.isNegative() || d.isZero()) {
            throw invalid(option, "expected a positive duration for");
        }
        return d;
    }

    static int requirePositive(HttpClientOption option, int n) {
        if (n <= 0) {
            throw invalid(option, "expected a positive value for");
        }
        return n;
    }

    static SdkClientConfigurationException invalid(HttpClientOption option, String problem) {
        return SdkClientConfigurationException.builder().option(option.configurationName())
                                              .message("Invalid Apache 5 HTTP client configuration: " + problem + " "
                                                       + option.configurationName() + ".")
                                              .build();
    }

    static Timeout timeout(Duration d) {
        return Timeout.ofMilliseconds(d.toMillis());
    }

    static TimeValue timeValue(Duration d) {
        return TimeValue.ofMilliseconds(d.toMillis());
    }

    /** The TLS strategy: the configured key and trust managers, or the JDK defaults; host names are verified. */
    DefaultClientTlsStrategy tlsStrategy() {
        if (tlsKeyManagersProvider == null && tlsTrustManagersProvider == null) {
            return DefaultClientTlsStrategy.createSystemDefault();
        }
        try {
            SSLContext context = SSLContext.getInstance("TLS");
            context.init(tlsKeyManagersProvider == null ? null : tlsKeyManagersProvider.get(),
                         tlsTrustManagersProvider == null ? null : tlsTrustManagersProvider.get(), null);
            return new DefaultClientTlsStrategy(context);
        } catch (GeneralSecurityException e) {
            throw SdkClientConfigurationException.builder().option(HttpClientOption.TLS_TRUST_MANAGERS
                                                                       .configurationName())
                                                 .message("The Apache 5 HTTP client TLS context cannot be created.")
                                                 .cause(e).build();
        }
    }

    /** The route planner of the explicit proxy, or {@code null} for direct connections. */
    HttpRoutePlanner routePlanner() {
        if (proxyConfiguration == null) {
            return null;
        }
        ProxyConfiguration proxy = proxyConfiguration;
        HttpHost proxyHost = new HttpHost(proxy.scheme(), proxy.host(), proxy.port());
        return new DefaultRoutePlanner(DefaultSchemePortResolver.INSTANCE) {
            @Override
            protected HttpHost determineProxy(HttpHost target, HttpContext context) {
                return proxy.bypasses(target.getHostName().toLowerCase(Locale.ROOT)) ? null : proxyHost;
            }
        };
    }

    /** The proxy credentials, or {@code null}. */
    BasicCredentialsProvider credentialsProvider() {
        if (proxyConfiguration == null || proxyConfiguration.username().isEmpty()) {
            return null;
        }
        BasicCredentialsProvider provider = new BasicCredentialsProvider();
        provider.setCredentials(new AuthScope(proxyConfiguration.host(), proxyConfiguration.port()),
                                new UsernamePasswordCredentials(proxyConfiguration.username().get(),
                                                                proxyConfiguration.password().get().toCharArray()));
        return provider;
    }

    /** Registers each resource as it is acquired. */
    @FunctionalInterface
    interface Registrar {
        Registrar NONE = new Registrar() {
            @Override
            public <T extends AutoCloseable> T acquired(T resource) {
                return resource;
            }
        };

        <T extends AutoCloseable> T acquired(T resource);
    }
}
