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

package software.amazon.awssdk.http.apache.internal;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * A resolved HTTP proxy: endpoint, optional basic credentials, and the hosts that bypass it ({@code *} wildcards,
 * matched case-insensitively, as in the JDK's {@code http.nonProxyHosts}). {@link #toString()} never renders the
 * credentials.
 */
public final class ProxySettings {
    private final String host;
    private final int port;
    private final String username;
    private final String password;
    private final Set<String> nonProxyHosts;
    private final List<Pattern> bypass;

    public ProxySettings(String host, int port, String username, String password, Set<String> nonProxyHosts) {
        this.host = Objects.requireNonNull(host, "host");
        if (port <= 0 || port > 65535) {
            throw new IllegalArgumentException("The proxy port is out of range: " + port);
        }
        this.port = port;
        this.username = username;
        this.password = password;
        this.nonProxyHosts = Set.copyOf(nonProxyHosts);
        this.bypass = this.nonProxyHosts.stream().map(ProxySettings::toPattern).toList();
    }

    public String host() {
        return host;
    }

    public int port() {
        return port;
    }

    public Set<String> nonProxyHosts() {
        return nonProxyHosts;
    }

    /** True if requests to {@code targetHost} go through this proxy. */
    public boolean appliesTo(String targetHost) {
        String h = targetHost.toLowerCase(Locale.ROOT);
        return bypass.stream().noneMatch(p -> p.matcher(h).matches());
    }

    /** The {@code Proxy-Authorization} value for basic credentials, or {@code null} without credentials. */
    String basicAuthorization() {
        if (username == null) {
            return null;
        }
        String token = username + ":" + (password == null ? "" : password);
        return "Basic " + Base64.getEncoder().encodeToString(token.getBytes(StandardCharsets.UTF_8));
    }

    private static Pattern toPattern(String hostPattern) {
        String[] parts = hostPattern.trim().toLowerCase(Locale.ROOT).split("\\*", -1);
        StringBuilder regex = new StringBuilder();
        for (int i = 0; i < parts.length; i++) {
            if (i > 0) {
                regex.append(".*");
            }
            if (!parts[i].isEmpty()) {
                regex.append(Pattern.quote(parts[i]));
            }
        }
        return Pattern.compile(regex.toString());
    }

    /** Endpoint and bypass list; credentials are only reported as present or absent. */
    @Override
    public String toString() {
        return "ProxySettings[" + host + ":" + port + ", credentials=" + (username != null) + ", nonProxyHosts="
               + nonProxyHosts + "]";
    }
}
