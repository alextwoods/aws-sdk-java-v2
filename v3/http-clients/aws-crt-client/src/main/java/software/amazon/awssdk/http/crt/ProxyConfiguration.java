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

import java.util.Objects;
import java.util.Optional;

/**
 * An explicit HTTP proxy of the CRT clients (V2 {@code software.amazon.awssdk.http.crt.ProxyConfiguration}; the
 * {@code proxyConfiguration} and {@code proxyConfiguration.credentials} options). The proxy is reached over plain
 * HTTP ({@code scheme} {@code http}) or TLS ({@code https}); an {@code https} target is tunnelled with
 * {@code CONNECT}, an {@code http} target is forwarded. System-property and environment proxy settings are not read
 * (V2's {@code useSystemPropertyValues} is not supported). Immutable; {@link #toString()} omits the password.
 */
public final class ProxyConfiguration {
    private final String scheme;
    private final String host;
    private final int port;
    private final String username;
    private final String password;

    private ProxyConfiguration(Builder builder) {
        this.scheme = builder.scheme == null ? "http" : builder.scheme;
        if (!"http".equals(scheme) && !"https".equals(scheme)) {
            throw new IllegalArgumentException("A proxy scheme is http or https: " + scheme);
        }
        this.host = Objects.requireNonNull(builder.host, "host");
        if (builder.port < 1 || builder.port > 65535) {
            throw new IllegalArgumentException("A proxy port is 1-65535: " + builder.port);
        }
        this.port = builder.port;
        if ((builder.username == null) != (builder.password == null)) {
            throw new IllegalArgumentException("Proxy credentials need both a username and a password");
        }
        this.username = builder.username;
        this.password = builder.password;
    }

    public static Builder builder() {
        return new Builder();
    }

    public String scheme() {
        return scheme;
    }

    public String host() {
        return host;
    }

    public int port() {
        return port;
    }

    public Optional<String> username() {
        return Optional.ofNullable(username);
    }

    public Optional<String> password() {
        return Optional.ofNullable(password);
    }

    /** True if basic credentials are configured. */
    public boolean hasCredentials() {
        return username != null;
    }

    @Override
    public String toString() {
        return "ProxyConfiguration[" + scheme + "://" + host + ":" + port + (username == null ? "" : ", credentials")
               + "]";
    }

    /** Builder of a {@link ProxyConfiguration}; {@code host} and {@code port} are required. */
    public static final class Builder {
        private String scheme;
        private String host;
        private int port;
        private String username;
        private String password;

        private Builder() {
        }

        public Builder scheme(String scheme) {
            this.scheme = scheme;
            return this;
        }

        public Builder host(String host) {
            this.host = host;
            return this;
        }

        public Builder port(int port) {
            this.port = port;
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

        public ProxyConfiguration build() {
            return new ProxyConfiguration(this);
        }
    }
}
