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

package software.amazon.awssdk.benchmark.harness;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Map;
import java.util.function.Function;

/**
 * The SDK-free wire bytes of one finite cell of a protocol other than awsJson1.0 (which {@link DynamoDbPutItemFixture}
 * covers), deterministic in the payload class: the exact request body every arm must send, the responses the mock
 * service serves, and how the mock recognizes the operation. The pad is a run of {@code x} whose length is a multiple
 * of {@code granularity} (3 for base64-encoded blobs, so the encoding has no padding) and is solved from the encoded
 * size of the request and success response, which are linear in it. Shared verbatim by the V3 workloads and every
 * reference client ({@code ClientDriver.REFERENCE_SHARED_SOURCES}), so all arms are measured against identical bytes.
 */
public final class PaddedFixture implements ServiceFixture {
    private final Route route;
    private final String pad;
    private final byte[] request;
    private final MockResponse success;
    private final MockResponse error;
    private final MockResponse retryable;

    /** How the mock service recognizes the operation. */
    public record Route(String method, String path, String headerName, String headerValue, byte[] bodyPrefix) {
        public static Route path(String method, String path) {
            return new Route(method, path, null, null, null);
        }
    }

    /**
     * @param error the modeled error response, or {@code null} if the operation models none
     */
    public PaddedFixture(PayloadClass payload, Route route, int granularity, Function<String, byte[]> request,
                         Function<String, MockResponse> success, MockResponse error, MockResponse retryable) {
        Function<Integer, Long> size = n -> {
            String p = "x".repeat(n);
            return (long) request.apply(p).length + success.apply(p).body().length;
        };
        int base = granularity;
        int probe = granularity * 1001;
        long s1 = size.apply(base);
        double slope = (size.apply(probe) - s1) / (double) (probe - base);
        long units = Math.max(0, Math.round((payload.targetBytes() - s1) / slope / granularity));
        this.route = route;
        this.pad = "x".repeat(base + (int) units * granularity);
        this.request = request.apply(pad);
        this.success = success.apply(pad);
        this.error = error;
        this.retryable = retryable;
        if (!payload.contains(encodedBytes())) {
            throw new IllegalStateException("fixture of " + encodedBytes() + " encoded bytes is not " + payload.id());
        }
        if (error != null && !payload.contains(this.request.length + (long) error.body().length)) {
            throw new IllegalStateException("error fixture is not " + payload.id());
        }
    }

    public static byte[] utf8(String s) {
        return s.getBytes(StandardCharsets.UTF_8);
    }

    /** A {@code Content-Type}-only response. */
    public static MockResponse response(int status, String contentType, String body) {
        return new MockResponse(status, contentType, utf8(body));
    }

    public static MockResponse response(int status, String contentType, byte[] body, Map<String, String> headers) {
        return new MockResponse(status, contentType, body, headers);
    }

    /** The padded member's value (a string member, or the UTF-8 content of a blob member). */
    public String pad() {
        return pad;
    }

    /** The exact request body of one attempt. */
    public byte[] request() {
        return request.clone();
    }

    public Route route() {
        return route;
    }

    /** Whether the operation models an error ({@link #modeledError()} throws otherwise). */
    public boolean hasModeledError() {
        return error != null;
    }

    @Override
    public boolean matches(String method, String path, String routingHeader) {
        return route.method().equals(method) && route.path().equals(path)
               && (route.headerName() == null || route.headerValue().equals(routingHeader));
    }

    @Override
    public boolean matchesBody(byte[] body) {
        byte[] prefix = route.bodyPrefix();
        return prefix == null
               || body.length >= prefix.length && Arrays.equals(body, 0, prefix.length, prefix, 0, prefix.length);
    }

    @Override
    public String routingHeaderName() {
        return route.headerName();
    }

    @Override
    public MockResponse success() {
        return success;
    }

    @Override
    public MockResponse modeledError() {
        if (error == null) {
            throw new IllegalStateException("the operation has no modeled error");
        }
        return error;
    }

    @Override
    public MockResponse retryableError() {
        return retryable;
    }

    @Override
    public long encodedBytes() {
        return request.length + (long) success.body().length;
    }
}
