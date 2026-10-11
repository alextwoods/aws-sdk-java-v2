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

package software.amazon.awssdk.protocol.smithytests;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import software.amazon.awssdk.http.SdkHttpFullRequest;
import software.amazon.awssdk.http.SdkHttpRequest;
import software.amazon.awssdk.utils.IoUtils;
import software.amazon.awssdk.utils.Validate;

/**
 * A request captured at the transport boundary by a protocol test, before any network connection is opened.
 *
 * <p>The request is immutable. The body is read once when the {@code CapturedRequest} is created.
 */
public final class CapturedRequest {
    private static final byte[] EMPTY = new byte[0];

    private final SdkHttpRequest request;
    private final byte[] body;
    private final List<QueryParameter> queryParameters;

    private CapturedRequest(SdkHttpRequest request, byte[] body) {
        this.request = Validate.paramNotNull(request, "request");
        this.body = body == null ? EMPTY : body.clone();
        this.queryParameters = Collections.unmodifiableList(
            QueryParameter.parseAll(request.encodedQueryParameters().orElse(null)));
    }

    /**
     * Create a captured request from an HTTP request and its body bytes. A null body is treated as empty.
     */
    public static CapturedRequest create(SdkHttpRequest request, byte[] body) {
        return new CapturedRequest(request, body);
    }

    /**
     * Create a captured request from a full HTTP request, reading its content stream (if any) to the end.
     */
    public static CapturedRequest create(SdkHttpFullRequest request) {
        Validate.paramNotNull(request, "request");
        return new CapturedRequest(request, request.contentStreamProvider()
                                                   .map(p -> readAll(p.newStream()))
                                                   .orElse(EMPTY));
    }

    private static byte[] readAll(InputStream stream) {
        try (InputStream in = stream) {
            return IoUtils.toByteArray(in);
        } catch (IOException e) {
            throw new UncheckedIOException("Unable to read the captured request body", e);
        }
    }

    /**
     * The underlying HTTP request (without a body).
     */
    public SdkHttpRequest httpRequest() {
        return request;
    }

    /**
     * The HTTP method name, for example {@code POST}.
     */
    public String method() {
        return request.method().name();
    }

    /**
     * The percent-encoded request path, without the query string. An empty path is reported as {@code /}.
     */
    public String path() {
        String path = request.encodedPath();
        return path == null || path.isEmpty() ? "/" : path;
    }

    /**
     * The request host.
     */
    public String host() {
        return request.host();
    }

    /**
     * The query parameters in request order, percent-decoded.
     */
    public List<QueryParameter> queryParameters() {
        return queryParameters;
    }

    /**
     * The request headers. Look headers up with {@link #headerValues(String)} or {@link #header(String)}, which compare
     * names case-insensitively.
     */
    public Map<String, List<String>> headers() {
        return request.headers();
    }

    /**
     * Every value of the headers whose name equals {@code name}, ignoring case, in request order.
     */
    public List<String> headerValues(String name) {
        return request.matchingHeaders(name);
    }

    /**
     * The values of the header named {@code name} (ignoring case) joined with {@code ", "}, or empty when the header is
     * absent. This is the form the Smithy {@code headers} assertion compares against.
     */
    public Optional<String> header(String name) {
        List<String> values = headerValues(name);
        return values.isEmpty() ? Optional.empty() : Optional.of(String.join(", ", values));
    }

    /**
     * A copy of the body bytes. The array is empty when the request has no body.
     */
    public byte[] body() {
        return body.clone();
    }

    @Override
    public String toString() {
        return method() + " " + path() + request.encodedQueryParameters().map(q -> "?" + q).orElse("")
               + " host=" + host() + " headers=" + headers() + " bodyLength=" + body.length;
    }
}
