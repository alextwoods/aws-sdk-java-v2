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

import java.net.URI;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * An explicit HTTP proxy of a {@link UrlConnectionHttpClient}, with the V2 {@code ProxyConfiguration} names.
 *
 * <ul>
 *   <li>{@code endpoint}: an {@code http://host:port} URI ({@link java.net.Proxy.Type#HTTP}); an {@code https} proxy
 *       scheme is rejected because {@code HttpURLConnection} cannot speak TLS to a proxy.</li>
 *   <li>{@code username}/{@code password} ({@code proxyConfiguration.credentials}): sent preemptively as
 *       {@code Proxy-Authorization: Basic} on {@code http} targets. The JDK builds the {@code CONNECT} tunnel of an
 *       {@code https} target itself, without caller headers, and disables Basic for tunnels by default, so an
 *       {@code https} exchange through a proxy with credentials fails with {@code PROXY} instead of silently sending
 *       no credentials.</li>
 *   <li>{@code nonProxyHosts} ({@code proxyConfiguration.nonProxyHosts}): regular expressions, as in V2, matched
 *       against the lower-cased target host; a matching host is connected to directly.</li>
 * </ul>
 * Proxy settings from system properties or the environment ({@code useSystemPropertyValues}) are not supported.
 *
 * <p>{@link #toString()} never includes the password. Instances are immutable.
 */
public final class ProxyConfiguration {
    private final String host;
    private final int port;
    private final String username;
    private final String password;
    private final Set<String> nonProxyHosts;
    private final Pattern[] nonProxyPatterns;

    private ProxyConfiguration(Builder builder) {
        URI endpoint = Objects.requireNonNull(builder.endpoint, "endpoint");
        if (!"http".equalsIgnoreCase(endpoint.getScheme())) {
            throw new IllegalArgumentException("The proxy endpoint must be an http URI; HttpURLConnection cannot "
                                               + "connect to a proxy over TLS: " + endpoint.getScheme());
        }
        if (endpoint.getHost() == null) {
            throw new IllegalArgumentException("The proxy endpoint has no host");
        }
        if ((endpoint.getRawPath() != null && !endpoint.getRawPath().isEmpty() && !"/".equals(endpoint.getRawPath()))
            || endpoint.getRawQuery() != null || endpoint.getRawUserInfo() != null) {
            throw new IllegalArgumentException("The proxy endpoint must be scheme://host[:port] only");
        }
        this.host = endpoint.getHost();
        this.port = endpoint.getPort() < 0 ? 80 : endpoint.getPort();
        if ((builder.username == null) != (builder.password == null)) {
            throw new IllegalArgumentException("Configure both the proxy username and password, or neither");
        }
        this.username = builder.username;
        this.password = builder.password;
        this.nonProxyHosts = Collections.unmodifiableSet(new LinkedHashSet<>(builder.nonProxyHosts));
        this.nonProxyPatterns = nonProxyHosts.stream().map(Pattern::compile).toArray(Pattern[]::new);
    }

    public static Builder builder() {
        return new Builder();
    }

    /** The proxy host. */
    public String host() {
        return host;
    }

    /** The proxy port (80 when the endpoint has none). */
    public int port() {
        return port;
    }

    /** The proxy user name, or {@code null}. */
    public String username() {
        return username;
    }

    /** The proxy password, or {@code null}. */
    public String password() {
        return password;
    }

    /** True if credentials are configured. */
    public boolean hasCredentials() {
        return username != null;
    }

    /** The non-proxy host patterns, in configuration order. */
    public Set<String> nonProxyHosts() {
        return nonProxyHosts;
    }

    /** True if {@code targetHost} is connected to through this proxy. */
    public boolean proxies(String targetHost) {
        String host = targetHost.toLowerCase(Locale.ROOT);
        for (Pattern pattern : nonProxyPatterns) {
            if (pattern.matcher(host).matches()) {
                return false;
            }
        }
        return true;
    }

    @Override
    public String toString() {
        return "ProxyConfiguration[" + host + ":" + port + (username == null ? "" : ", credentials")
               + ", nonProxyHosts=" + nonProxyHosts + "]";
    }

    /** Mutable builder; not thread safe. {@code endpoint} is required. */
    public static final class Builder {
        private URI endpoint;
        private String username;
        private String password;
        private final Set<String> nonProxyHosts = new LinkedHashSet<>();

        private Builder() {
        }

        public Builder endpoint(URI endpoint) {
            this.endpoint = endpoint;
            return this;
        }

        public Builder username(String username) {
            this.username = username;
            return this;
        }

        public Builder password(String password) {
            this.password = password;
            return this;
        }

        /** Replaces the non-proxy host patterns. */
        public Builder nonProxyHosts(Set<String> nonProxyHosts) {
            this.nonProxyHosts.clear();
            nonProxyHosts.forEach(this::addNonProxyHost);
            return this;
        }

        public Builder addNonProxyHost(String nonProxyHost) {
            this.nonProxyHosts.add(Objects.requireNonNull(nonProxyHost, "nonProxyHost"));
            return this;
        }

        public ProxyConfiguration build() {
            return new ProxyConfiguration(this);
        }
    }
}
