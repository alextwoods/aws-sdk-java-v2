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

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

/**
 * An immutable HTTP request: method, target (scheme, host, port, encoded path, ordered query parameters), ordered
 * header fields and body (Requirement 12.2).
 *
 * <p>The target is held as raw protocol components rather than a {@link java.net.URI}, which is never created:
 * the path is already percent-encoded (a protocol encodes each label exactly once, keeping {@code /} in greedy
 * labels), and the query is an ordered list of raw {@link HttpQueryParameter} entries, repeated names allowed, each
 * encoded once when added. {@link #encodedQuery()} and {@link #rawTarget()} join the raw text without re-encoding, so a
 * transport sends exactly the target the protocol marshalled (Requirement 12.2). Header fields keep insertion order,
 * name case and repeated values (Requirement 12.4).
 *
 * <p>HTTP/2 (Requirement 12.3): the pseudo-header fields {@code :method}, {@code :scheme}, {@code :authority} and
 * {@code :path} are derived from the method and target by {@link #pseudoHeaders()}; they cannot be added as header
 * fields, so an HTTP/1.1 and an HTTP/2 transport transmit the same request. Stream, flow-control and connection
 * behavior belong to the transport and are described by {@link HttpClientCapabilities}.
 *
 * <p>Instances are immutable and safe to share across threads; {@link #toBuilder()} is the copy-on-write path signers
 * and interceptors use to derive modified requests. The body is consumed by the transport under the ownership rules of
 * {@link SdkSyncHttpClient} and {@link SdkAsyncHttpClient}.
 */
public final class SdkHttpRequest {
    private final String method;
    private final String scheme;
    private final String host;
    private final int port;
    private final String encodedPath;
    private final List<HttpQueryParameter> queryParameters;
    private final HttpHeaders headers;
    private final HttpBody body;

    private SdkHttpRequest(Builder builder) {
        this.method = Objects.requireNonNull(builder.method, "method");
        this.scheme = Objects.requireNonNull(builder.scheme, "scheme");
        this.host = Objects.requireNonNull(builder.host, "host");
        this.port = builder.port;
        this.encodedPath = builder.encodedPath;
        this.queryParameters = List.copyOf(builder.queryParameters);
        this.headers = builder.headers.build();
        this.body = builder.body;
    }

    public static Builder builder() {
        return new Builder();
    }

    /** The request method, for example {@code POST}. */
    public String method() {
        return method;
    }

    /** The scheme, {@code http} or {@code https}. */
    public String scheme() {
        return scheme;
    }

    /** The host name or IP literal (an IPv6 address in brackets), without port. */
    public String host() {
        return host;
    }

    /** The explicit port, or {@code -1} for the scheme's default port. */
    public int port() {
        return port;
    }

    /** {@code host[:port]}, with the port only if explicit. */
    public String authority() {
        return port < 0 ? host : host + ":" + port;
    }

    /** The percent-encoded path, starting with {@code /}. */
    public String encodedPath() {
        return encodedPath;
    }

    /** The raw query entries, in order; repeated names are separate entries. */
    public List<HttpQueryParameter> queryParameters() {
        return queryParameters;
    }

    /**
     * The raw query string without the leading {@code ?}: the raw entries joined by {@code &} in order, never
     * re-encoded; empty if there are no entries. An entry without a value is rendered as its name only.
     */
    public String encodedQuery() {
        if (queryParameters.isEmpty()) {
            return "";
        }
        StringBuilder out = new StringBuilder();
        for (HttpQueryParameter parameter : queryParameters) {
            if (!out.isEmpty()) {
                out.append('&');
            }
            out.append(parameter.rawName());
            if (parameter.rawValue() != null) {
                out.append('=').append(parameter.rawValue());
            }
        }
        return out.toString();
    }

    /** The request target of an HTTP/1.1 request line: the encoded path, then {@code ?} and the query if any. */
    public String rawTarget() {
        String query = encodedQuery();
        return query.isEmpty() ? encodedPath : encodedPath + "?" + query;
    }

    /**
     * The HTTP/2 pseudo-header fields, in the order they are transmitted: {@code :method}, {@code :scheme},
     * {@code :authority} ({@link #authority()}) and {@code :path} ({@link #rawTarget()}).
     */
    public List<Map.Entry<String, String>> pseudoHeaders() {
        return List.of(Map.entry(":method", method), Map.entry(":scheme", scheme), Map.entry(":authority", authority()),
                       Map.entry(":path", rawTarget()));
    }

    /** The header fields, in order. */
    public HttpHeaders headers() {
        return headers;
    }

    /** The body. */
    public HttpBody body() {
        return body;
    }

    /** A builder holding this request's values. */
    public Builder toBuilder() {
        Builder builder = new Builder();
        builder.method = method;
        builder.scheme = scheme;
        builder.host = host;
        builder.port = port;
        builder.encodedPath = encodedPath;
        builder.queryParameters.addAll(queryParameters);
        builder.headers = headers.toBuilder();
        builder.body = body;
        return builder;
    }

    @Override
    public boolean equals(Object o) {
        return this == o || o instanceof SdkHttpRequest other && port == other.port && method.equals(other.method)
                            && scheme.equals(other.scheme) && host.equals(other.host)
                            && encodedPath.equals(other.encodedPath) && queryParameters.equals(other.queryParameters)
                            && headers.equals(other.headers) && body.equals(other.body);
    }

    @Override
    public int hashCode() {
        return Objects.hash(method, scheme, host, port, encodedPath, queryParameters, headers, body);
    }

    /** Renders the method, authority, path, header names and body kind; query values and header values may be
     * sensitive and are omitted. */
    @Override
    public String toString() {
        return "SdkHttpRequest[" + method + " " + scheme + "://" + authority() + encodedPath + ", " + headers + ", "
               + body + "]";
    }

    /** Mutable builder of {@link SdkHttpRequest}; not thread safe. */
    public static final class Builder {
        private String method;
        private String scheme = "https";
        private String host;
        private int port = -1;
        private String encodedPath = "/";
        private final List<HttpQueryParameter> queryParameters = new ArrayList<>();
        private HttpHeaders.Builder headers = HttpHeaders.builder();
        private HttpBody body = HttpBody.empty();

        private Builder() {
        }

        /**
         * The method; stored upper case.
         *
         * @throws IllegalArgumentException if the method is empty or not an HTTP token (for example it has a space or
         *     line break)
         */
        public Builder method(String method) {
            Objects.requireNonNull(method, "method");
            RawTargets.requireMethod(method);
            this.method = method.toUpperCase(Locale.ROOT);
            return this;
        }

        /** The scheme; {@code http} or {@code https} (case-insensitive, stored lower case). */
        public Builder scheme(String scheme) {
            String lower = Objects.requireNonNull(scheme, "scheme").toLowerCase(Locale.ROOT);
            if (!lower.equals("http") && !lower.equals("https")) {
                throw new IllegalArgumentException("Unsupported scheme " + scheme);
            }
            this.scheme = lower;
            return this;
        }

        /**
         * The host as it appears in a URI ({@link java.net.URI#getHost()}): a name or IPv4 address, or an IPv6 literal
         * in brackets, such as {@code [::1]}; kept exactly.
         *
         * @throws IllegalArgumentException if the host is empty, has a space, control or non-ASCII character or one
         *     of {@code / ? # @ [ ] \}, or has a {@code :} outside brackets
         */
        public Builder host(String host) {
            Objects.requireNonNull(host, "host");
            RawTargets.requireHost(host);
            this.host = host;
            return this;
        }

        /** The explicit port, or {@code -1} for the scheme default. */
        public Builder port(int port) {
            if (port < -1 || port > 65535) {
                throw new IllegalArgumentException("Not a port: " + port);
            }
            this.port = port;
            return this;
        }

        /**
         * The already percent-encoded path, kept exactly; an empty path is {@code /}.
         *
         * @throws IllegalArgumentException if the path does not start with {@code /}, or has a space, control or
         *     non-ASCII character, a {@code ?} or {@code #}, or a malformed percent escape
         */
        public Builder encodedPath(String encodedPath) {
            Objects.requireNonNull(encodedPath, "encodedPath");
            if (encodedPath.isEmpty()) {
                this.encodedPath = "/";
            } else if (encodedPath.charAt(0) != '/') {
                throw new IllegalArgumentException("A request path starts with /");
            } else {
                RawTargets.requirePath(encodedPath);
                this.encodedPath = encodedPath;
            }
            return this;
        }

        /**
         * Appends a query entry of the decoded {@code name} and {@code value} ({@code null} for a name-only entry),
         * each encoded once ({@link HttpQueryParameter#of}).
         */
        public Builder addQueryParameter(String name, String value) {
            queryParameters.add(HttpQueryParameter.of(name, value));
            return this;
        }

        /** Appends a query entry of already encoded text, kept exactly ({@link HttpQueryParameter#raw}). */
        public Builder addRawQueryParameter(String rawName, String rawValue) {
            queryParameters.add(HttpQueryParameter.raw(rawName, rawValue));
            return this;
        }

        /** Appends a query entry. */
        public Builder addQueryParameter(HttpQueryParameter parameter) {
            queryParameters.add(Objects.requireNonNull(parameter, "parameter"));
            return this;
        }

        /** Removes every query parameter. */
        public Builder clearQueryParameters() {
            queryParameters.clear();
            return this;
        }

        /** Appends a header field. */
        public Builder addHeader(String name, String value) {
            headers.add(name, value);
            return this;
        }

        /** Replaces every field named {@code name} with one {@code name: value} field. */
        public Builder putHeader(String name, String value) {
            headers.set(name, value);
            return this;
        }

        /** Removes every field named {@code name}. */
        public Builder removeHeader(String name) {
            headers.remove(name);
            return this;
        }

        /** True if a header named {@code name} (case-insensitive) is present. */
        public boolean hasHeader(String name) {
            return headers.contains(name);
        }

        /** Replaces every header field. */
        public Builder headers(HttpHeaders headers) {
            this.headers = headers.toBuilder();
            return this;
        }

        public Builder body(HttpBody body) {
            this.body = Objects.requireNonNull(body, "body");
            return this;
        }

        public SdkHttpRequest build() {
            return new SdkHttpRequest(this);
        }
    }
}
