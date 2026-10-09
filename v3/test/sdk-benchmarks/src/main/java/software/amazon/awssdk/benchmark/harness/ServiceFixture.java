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

import java.util.Map;

/**
 * The mock-service half of a cell's workload: the precomputed wire responses the separate mock-service process
 * serves, and how it recognizes the cell's operation. Built identically, from the cell alone, in both processes.
 */
public interface ServiceFixture {

    /**
     * A precomputed HTTP response.
     *
     * @param headers extra response headers (e.g. {@code smithy-protocol}, {@code X-Amzn-ErrorType}), beyond
     *     {@code Content-Type} and the length
     */
    record MockResponse(int status, String contentType, byte[] body, Map<String, String> headers) {
        public MockResponse {
            headers = Map.copyOf(headers);
        }

        public MockResponse(int status, String contentType, byte[] body) {
            this(status, contentType, body, Map.of());
        }
    }

    /** True if a request with this method, path and protocol routing header belongs to the cell's operation. */
    boolean matches(String method, String path, String routingHeader);

    /**
     * True if a request {@link #matches matching} by method, path and header also has a body of the cell's operation;
     * protocols that route by body (the query protocols' {@code Action}) override it.
     */
    default boolean matchesBody(byte[] body) {
        return true;
    }

    /**
     * {@link #matchesBody(byte[])} for a request with {@code method}; a streaming fixture whose logical operation is
     * an upload and a download checks each request's body (the exact uploaded content, an empty download request).
     */
    default boolean matchesBody(String method, byte[] body) {
        return matchesBody(body);
    }

    /** The protocol's routing header name (e.g. {@code X-Amz-Target}), or {@code null} if it routes by path. */
    String routingHeaderName();

    MockResponse success();

    /**
     * The success response to a request with {@code method}; a streaming fixture whose logical operation is an
     * upload ({@code PUT}) and a download ({@code GET}) answers each with its own response.
     */
    default MockResponse success(String method) {
        return success();
    }

    /** A modeled, non-retryable service error. */
    MockResponse modeledError();

    /** A retryable 5xx service error. */
    MockResponse retryableError();

    /**
     * Encoded bytes of one logical operation (final request body + final response body); asserted to lie in the
     * cell's {@link PayloadClass} when the fixture is built.
     */
    long encodedBytes();

    /**
     * Matching requests of one successful logical operation, excluding injected retries; the validity rules compare
     * the mock's request counter with {@code operations * requestsPerOperation()}.
     */
    default int requestsPerOperation() {
        return 1;
    }
}
