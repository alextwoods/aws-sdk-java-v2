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

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.OptionalLong;
import java.util.Set;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.http.HttpBody;
import software.amazon.awssdk.http.HttpHeaders;
import software.amazon.awssdk.http.SdkHttpRequest;

/**
 * What {@code HttpURLConnection} will transmit for a request, or a clear failure when it cannot transmit the request
 * exactly (Requirements 12.2, 12.4). Built by {@link UrlConnectionHttpClient#prepare} before any I/O.
 *
 * <p>Fidelity, from the JDK 21 {@code sun.net.www.protocol.http.HttpURLConnection} source:
 * <ul>
 *   <li>Every caller header is added with {@code addRequestProperty}, one call per entry, so header names keep their
 *       case and repeated names are separate header lines in insertion order, interleaved with other names as given.
 *       Values are never comma-joined.</li>
 *   <li>The JDK owns {@code Host}, {@code Content-Length} and {@code Transfer-Encoding} (restricted headers). A caller
 *       {@code Host} must equal what the JDK sends ({@code host[:port]}, the port only when not the scheme default);
 *       {@code Content-Length} must be one non-negative value equal to the body length; {@code Transfer-Encoding} must
 *       be exactly {@code chunked}. They are then sent by the JDK after the caller headers, so their position moves.
 *       With {@code -Dsun.net.http.allowRestrictedHeaders=true} they are added in place and keep their position.</li>
 *   <li>The other restricted headers ({@code Access-Control-Request-Headers}, {@code Access-Control-Request-Method},
 *       {@code Connection} other than {@code close}, {@code Content-Transfer-Encoding}, {@code Keep-Alive},
 *       {@code Origin}, {@code Trailer}, {@code Upgrade}, {@code Via}, {@code Sec-*}) would be dropped silently by
 *       the JDK, so they fail the request unless restricted headers are allowed.</li>
 *   <li>The JDK appends, when absent: {@code User-Agent} ({@code Java/<version>}), {@code Host}, {@code Accept}
 *       ({@code *}{@code /*}), {@code Connection: keep-alive} (or {@code Proxy-Connection} through a plain proxy),
 *       {@code Content-type: application/x-www-form-urlencoded} for a non-PUT request with a body, and the framing
 *       header. A body without a framing header is sent with {@code Content-Length} when its length is known, else
 *       {@code Transfer-Encoding: chunked}; the body is never buffered whole.</li>
 *   <li>Methods: only {@code GET POST HEAD OPTIONS PUT DELETE TRACE} (the JDK rejects others, such as
 *       {@code PATCH}). {@code GET}, {@code HEAD} and {@code TRACE} cannot carry a body: the JDK turns a {@code GET}
 *       with a body into {@code POST}.</li>
 *   <li>The request target is {@link SdkHttpRequest#rawTarget()} unchanged; the JDK does not re-encode it
 *       (through a plain proxy it sends the absolute form {@code scheme://authority} + raw target).</li>
 *   <li>A global {@link java.net.CookieHandler} or {@link java.net.ResponseCache} installed by the application is
 *       honoured by the JDK and can add {@code Cookie} headers or serve cached responses.</li>
 * </ul>
 */
final class UrlConnectionRequestPlan {
    /** Methods {@code HttpURLConnection.setRequestMethod} accepts. */
    static final Set<String> METHODS = Set.of("GET", "POST", "HEAD", "OPTIONS", "PUT", "DELETE", "TRACE");

    /** Restricted headers the JDK drops silently (lower case), other than the framing and Host headers. */
    static final Set<String> RESTRICTED = Set.of("access-control-request-headers", "access-control-request-method",
                                                 "connection", "content-transfer-encoding", "keep-alive", "origin",
                                                 "trailer", "upgrade", "via");

    private static final Set<String> NO_BODY_METHODS = Set.of("GET", "HEAD", "TRACE");

    /** How the body is framed. */
    enum Framing { NONE, FIXED, CHUNKED }

    final String method;
    final List<Map.Entry<String, String>> headers;
    final Framing framing;
    final long fixedLength;

    private UrlConnectionRequestPlan(String method, List<Map.Entry<String, String>> headers, Framing framing,
                                     long fixedLength) {
        this.method = method;
        this.headers = headers;
        this.framing = framing;
        this.fixedLength = fixedLength;
    }

    /** The JDK's {@code Host} value for {@code request}. */
    static String jdkHost(SdkHttpRequest request) {
        int defaultPort = "https".equalsIgnoreCase(request.scheme()) ? 443 : 80;
        int port = request.port();
        String host = request.host();
        if (host.indexOf(':') >= 0 && !host.startsWith("[")) {
            host = "[" + host + "]";
        }
        return port != -1 && port != defaultPort ? host + ":" + port : host;
    }

    static UrlConnectionRequestPlan of(SdkHttpRequest request, boolean allowRestricted) {
        String method = request.method();
        if (!METHODS.contains(method)) {
            throw unsupported("the method " + method + "; HttpURLConnection supports only " + sorted(METHODS));
        }
        HttpBody body = request.body();
        OptionalLong bodyLength = body.contentLength();
        boolean hasBody = body.kind() == HttpBody.Kind.STREAMING
                          || body.kind() == HttpBody.Kind.BYTES && bodyLength.orElse(0) > 0;

        HttpHeaders headers = request.headers();
        List<Map.Entry<String, String>> out = new ArrayList<>(headers.size());
        String contentLength = null;
        String transferEncoding = null;
        boolean host = false;
        for (int i = 0; i < headers.size(); i++) {
            String name = headers.name(i);
            String value = headers.value(i);
            String lower = name.toLowerCase(Locale.ROOT);
            switch (lower) {
                case "host" -> {
                    if (host) {
                        throw unsupported("a repeated Host header");
                    }
                    host = true;
                    String expected = jdkHost(request);
                    if (!allowRestricted && !value.equalsIgnoreCase(expected)) {
                        throw unsupported("the Host header '" + value + "'; the JDK always sends Host: " + expected);
                    }
                }
                case "content-length" -> {
                    if (contentLength != null) {
                        throw unsupported("a repeated Content-Length header");
                    }
                    contentLength = value;
                }
                case "transfer-encoding" -> {
                    if (transferEncoding != null || !"chunked".equalsIgnoreCase(value.trim())) {
                        throw unsupported("Transfer-Encoding: " + value + "; only a single 'chunked' is supported");
                    }
                    transferEncoding = value;
                }
                default -> {
                    if (!allowRestricted && (lower.startsWith("sec-") || RESTRICTED.contains(lower)
                                                                         && !("connection".equals(lower)
                                                                              && "close".equalsIgnoreCase(value)))) {
                        throw unsupported("the restricted header " + name + ", which HttpURLConnection drops "
                                          + "(allowed only with -Dsun.net.http.allowRestrictedHeaders=true)");
                    }
                }
            }
            boolean jdkOwned = "host".equals(lower) || "content-length".equals(lower)
                               || "transfer-encoding".equals(lower);
            if (!jdkOwned || allowRestricted) {
                out.add(Map.entry(name, value));
            }
        }

        Framing framing;
        long fixed = -1;
        if (contentLength != null && transferEncoding != null) {
            throw unsupported("both Content-Length and Transfer-Encoding");
        }
        if (contentLength != null) {
            try {
                fixed = Long.parseLong(contentLength.trim());
            } catch (NumberFormatException e) {
                fixed = -1;
            }
            if (fixed < 0) {
                throw unsupported("Content-Length: " + contentLength);
            }
            long actual = hasBody ? bodyLength.orElse(fixed) : 0;
            if (actual != fixed) {
                throw unsupported("Content-Length: " + contentLength + " for a body of " + actual + " bytes");
            }
            framing = Framing.FIXED;
        } else if (transferEncoding != null) {
            framing = Framing.CHUNKED;
        } else if (hasBody) {
            if (bodyLength.isPresent()) {
                framing = Framing.FIXED;
                fixed = bodyLength.getAsLong();
            } else {
                framing = Framing.CHUNKED;
            }
        } else {
            framing = Framing.NONE;
        }
        if (framing != Framing.NONE && NO_BODY_METHODS.contains(method)) {
            throw unsupported("a body or framing header on " + method + "; HttpURLConnection sends a GET with a body "
                              + "as POST and rejects bodies on HEAD and TRACE");
        }
        return new UrlConnectionRequestPlan(method, List.copyOf(out), framing, fixed);
    }

    private static String sorted(Set<String> values) {
        return String.join(" ", values.stream().sorted().toList());
    }

    private static SdkClientException unsupported(String what) {
        return SdkClientException.create(UrlConnectionHttpClient.CLIENT_NAME + " cannot transmit " + what
                                         + ". Use another HTTP client for this request.");
    }
}
