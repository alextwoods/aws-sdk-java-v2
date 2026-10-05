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

import java.net.URI;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/**
 * An explicit HTTP proxy of the Apache 5 clients, named as the V2 {@code apache5.ProxyConfiguration}. It carries the
 * {@link software.amazon.awssdk.http.HttpClientOption#PROXY}, {@code PROXY_CREDENTIALS} and {@code NON_PROXY_HOSTS}
 * options. Proxy settings from system properties or environment variables (V2 {@code useSystemPropertyValues},
 * {@code useEnvironmentVariableValues}) and NTLM are not supported yet; the clients do not declare
 * {@code PROXY_FROM_ENVIRONMENT}, so configuring it fails construction.
 *
 * <p>Instances are immutable; {@link #toString()} never prints the password.
 */
public final class ProxyConfiguration {
    private final URI endpoint;
    private final String username;
    private final String password;
    private final Set<String> nonProxyHosts;

    private ProxyConfiguration(Builder builder) {
        this.endpoint = Objects.requireNonNull(builder.endpoint, "endpoint");
        String scheme = endpoint.getScheme();
        if (scheme == null || !(scheme.equalsIgnoreCase("http") || scheme.equalsIgnoreCase("https"))) {
            throw new IllegalArgumentException("The proxy endpoint scheme must be http or https: " + scheme);
        }
        if (endpoint.getHost() == null) {
            throw new IllegalArgumentException("The proxy endpoint has no host");
        }
        if ((builder.username == null) != (builder.password == null)) {
            throw new IllegalArgumentException("The proxy username and password are set together");
        }
        this.username = builder.username;
        this.password = builder.password;
        this.nonProxyHosts = Collections.unmodifiableSet(new LinkedHashSet<>(builder.nonProxyHosts));
    }

    public static Builder builder() {
        return new Builder();
    }

    /** The proxy endpoint, {@code http} or {@code https}. */
    public URI endpoint() {
        return endpoint;
    }

    /** The scheme of the proxy endpoint, lower case. */
    public String scheme() {
        return endpoint.getScheme().toLowerCase(Locale.ROOT);
    }

    /** The proxy host. */
    public String host() {
        return endpoint.getHost();
    }

    /** The proxy port, defaulted by scheme. */
    public int port() {
        return endpoint.getPort() >= 0 ? endpoint.getPort() : scheme().equals("https") ? 443 : 80;
    }

    /** The basic-authentication username, if credentials are set. */
    public Optional<String> username() {
        return Optional.ofNullable(username);
    }

    /** The basic-authentication password, if credentials are set. */
    public Optional<String> password() {
        return Optional.ofNullable(password);
    }

    /** Hosts that bypass the proxy: exact names or {@code *.suffix} patterns, case insensitive. */
    public Set<String> nonProxyHosts() {
        return nonProxyHosts;
    }

    /** True if {@code host} bypasses the proxy. */
    public boolean bypasses(String host) {
        String h = host.toLowerCase(Locale.ROOT);
        for (String pattern : nonProxyHosts) {
            String p = pattern.toLowerCase(Locale.ROOT);
            if (p.equals(h) || p.startsWith("*") && h.endsWith(p.substring(1))) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean equals(Object o) {
        return this == o || o instanceof ProxyConfiguration other && endpoint.equals(other.endpoint)
                            && Objects.equals(username, other.username) && Objects.equals(password, other.password)
                            && nonProxyHosts.equals(other.nonProxyHosts);
    }

    @Override
    public int hashCode() {
        return Objects.hash(endpoint, username, password, nonProxyHosts);
    }

    @Override
    public String toString() {
        return "ProxyConfiguration[" + scheme() + "://" + host() + ":" + port()
               + (username == null ? "" : ", credentials") + ", nonProxyHosts=" + nonProxyHosts + "]";
    }

    /** Mutable builder; not thread safe. The endpoint is required. */
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

        /** Replaces the hosts that bypass the proxy. */
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
