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

import java.util.Objects;

/**
 * An immutable HTTP response: status code, ordered header fields and body (Requirement 12.2).
 *
 * <p>A sync transport returns the body as a one-shot {@link HttpBody.Kind#STREAMING} body backed by the connection
 * ({@link SyncHttpExchange}); an async transport reports status and headers with an empty body to
 * {@link AsyncHttpResponseHandler#onHeaders} and the body separately. The pipeline owns the body until it transfers it
 * to a response transformer. Instances are immutable.
 */
public final class SdkHttpResponse {
    private final int statusCode;
    private final HttpHeaders headers;
    private final HttpBody body;

    private SdkHttpResponse(Builder builder) {
        this.statusCode = builder.statusCode;
        this.headers = builder.headers.build();
        this.body = builder.body;
    }

    private SdkHttpResponse(int statusCode, HttpHeaders headers, HttpBody body) {
        this.statusCode = statusCode;
        this.headers = headers;
        this.body = body;
    }

    public static Builder builder() {
        return new Builder();
    }

    /** The status code. */
    public int statusCode() {
        return statusCode;
    }

    /** True for a 2xx status code. */
    public boolean isSuccessful() {
        return statusCode >= 200 && statusCode < 300;
    }

    /** The header fields, in order. */
    public HttpHeaders headers() {
        return headers;
    }

    /** The body. */
    public HttpBody body() {
        return body;
    }

    /**
     * This response's status code and headers with an {@link HttpBody#empty() empty} body: the view a modeled response
     * or error retains, so it never holds (or keeps open) the payload. Returns this response if its body is already
     * empty; otherwise a new response sharing these immutable headers, without copying them.
     */
    public SdkHttpResponse withoutBody() {
        return body.kind() == HttpBody.Kind.EMPTY ? this : new SdkHttpResponse(statusCode, headers, HttpBody.empty());
    }

    public Builder toBuilder() {
        Builder builder = new Builder();
        builder.statusCode = statusCode;
        builder.headers = headers.toBuilder();
        builder.body = body;
        return builder;
    }

    @Override
    public boolean equals(Object o) {
        return this == o || o instanceof SdkHttpResponse other && statusCode == other.statusCode
                            && headers.equals(other.headers) && body.equals(other.body);
    }

    @Override
    public int hashCode() {
        return Objects.hash(statusCode, headers, body);
    }

    @Override
    public String toString() {
        return "SdkHttpResponse[" + statusCode + ", " + headers + ", " + body + "]";
    }

    /** Mutable builder of {@link SdkHttpResponse}; not thread safe. */
    public static final class Builder {
        private int statusCode = 200;
        private HttpHeaders.Builder headers = HttpHeaders.builder();
        private HttpBody body = HttpBody.empty();

        private Builder() {
        }

        public Builder statusCode(int statusCode) {
            if (statusCode < 100 || statusCode > 999) {
                throw new IllegalArgumentException("Not an HTTP status code: " + statusCode);
            }
            this.statusCode = statusCode;
            return this;
        }

        public Builder addHeader(String name, String value) {
            headers.add(name, value);
            return this;
        }

        public Builder headers(HttpHeaders headers) {
            this.headers = headers.toBuilder();
            return this;
        }

        public Builder body(HttpBody body) {
            this.body = Objects.requireNonNull(body, "body");
            return this;
        }

        public SdkHttpResponse build() {
            return new SdkHttpResponse(this);
        }
    }
}
