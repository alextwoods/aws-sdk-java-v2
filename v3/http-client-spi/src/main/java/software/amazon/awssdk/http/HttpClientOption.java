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

package software.amazon.awssdk.http;

/**
 * A configurable transport option, named as on the V2 HTTP client builders, that a transport either supports or
 * rejects at construction (Requirements 12.1, 12.13). {@link HttpClientCapabilities#options()} lists the supported
 * ones; provider selection (task 7.2) fails construction for a configured option outside that set, naming the
 * implementation and {@link #configurationName()}.
 */
public enum HttpClientOption {
    /** Time to establish a TCP connection. */
    CONNECTION_TIMEOUT(Category.TIMEOUT, "connectionTimeout"),
    /** Time to wait for a pooled connection. */
    CONNECTION_ACQUISITION_TIMEOUT(Category.TIMEOUT, "connectionAcquisitionTimeout"),
    /** Time between response bytes (V2 {@code socketTimeout} / {@code readTimeout}). */
    READ_TIMEOUT(Category.TIMEOUT, "readTimeout"),
    /** Time for a request write to make progress. */
    WRITE_TIMEOUT(Category.TIMEOUT, "writeTimeout"),
    /** Time for the TLS handshake. */
    TLS_NEGOTIATION_TIMEOUT(Category.TIMEOUT, "tlsNegotiationTimeout"),

    /** Custom key managers (client certificates). */
    TLS_KEY_MANAGERS(Category.TLS, "tlsKeyManagersProvider"),
    /** Custom trust managers. */
    TLS_TRUST_MANAGERS(Category.TLS, "tlsTrustManagersProvider"),
    /** Disabling certificate validation, for testing only. */
    TRUST_ALL_CERTIFICATES(Category.TLS, "trustAllCertificates"),

    /** An explicit proxy endpoint. */
    PROXY(Category.PROXY, "proxyConfiguration"),
    /** Proxy credentials (basic or NTLM). */
    PROXY_CREDENTIALS(Category.PROXY, "proxyConfiguration.credentials"),
    /** Hosts that bypass the proxy. */
    NON_PROXY_HOSTS(Category.PROXY, "proxyConfiguration.nonProxyHosts"),
    /** Proxy settings from system properties or environment variables. */
    PROXY_FROM_ENVIRONMENT(Category.PROXY, "proxyConfiguration.useSystemPropertyValues"),

    /** The maximum number of pooled connections (or concurrent streams for HTTP/2). */
    MAX_CONNECTIONS(Category.CONNECTION, "maxConnections"),
    /** The maximum number of waiters for a pooled connection. */
    MAX_PENDING_CONNECTION_ACQUIRES(Category.CONNECTION, "maxPendingConnectionAcquires"),
    /** The maximum lifetime of a pooled connection. */
    CONNECTION_TIME_TO_LIVE(Category.CONNECTION, "connectionTimeToLive"),
    /** The maximum idle time of a pooled connection. */
    CONNECTION_MAX_IDLE_TIME(Category.CONNECTION, "connectionMaxIdleTime"),
    /** Background reaping of idle connections. */
    REAP_IDLE_CONNECTIONS(Category.CONNECTION, "useIdleConnectionReaper"),
    /** TCP keep-alive on connections. */
    TCP_KEEP_ALIVE(Category.CONNECTION, "tcpKeepAlive"),
    /** {@code Expect: 100-continue} handling for request bodies. */
    EXPECT_CONTINUE(Category.CONNECTION, "expectContinueEnabled"),

    /** Selecting the protocol version ({@link Protocol}). */
    PROTOCOL_VERSION(Category.PROTOCOL, "protocol"),
    /** The maximum concurrent HTTP/2 streams per connection. */
    HTTP2_MAX_STREAMS(Category.PROTOCOL, "http2Configuration.maxStreams"),
    /** The HTTP/2 initial flow-control window. */
    HTTP2_INITIAL_WINDOW_SIZE(Category.PROTOCOL, "http2Configuration.initialWindowSize");

    private final Category category;
    private final String configurationName;

    HttpClientOption(Category category, String configurationName) {
        this.category = category;
        this.configurationName = configurationName;
    }

    /** The group an option belongs to. */
    public enum Category {
        /** Transport timeouts. */
        TIMEOUT,
        /** TLS. */
        TLS,
        /** Proxies. */
        PROXY,
        /** Connection establishment and pooling. */
        CONNECTION,
        /** Protocol version and HTTP/2 settings. */
        PROTOCOL
    }

    /** The group of this option. */
    public Category category() {
        return category;
    }

    /** The builder property this option is configured by, for diagnostics. */
    public String configurationName() {
        return configurationName;
    }
}
