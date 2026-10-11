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

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import software.amazon.awssdk.utils.Validate;

/**
 * The response a {@link ProtocolTestHarness} returns to the client in a generated response test: the test case
 * {@code code}, {@code headers} and {@code body}.
 */
public final class StubbedResponse {
    private static final byte[] EMPTY = new byte[0];

    private final int statusCode;
    private final Map<String, String> headers;
    private final byte[] body;

    private StubbedResponse(int statusCode, Map<String, String> headers, byte[] body) {
        this.statusCode = statusCode;
        this.headers = Collections.unmodifiableMap(new LinkedHashMap<>(Validate.paramNotNull(headers, "headers")));
        this.body = body == null ? EMPTY : body.clone();
    }

    /**
     * A response with a binary body. A null body is treated as empty.
     */
    public static StubbedResponse create(int statusCode, Map<String, String> headers, byte[] body) {
        return new StubbedResponse(statusCode, headers, body);
    }

    /**
     * A response whose body is the test case {@code body} string, interpreted for {@code bodyMediaType}: base64-decoded
     * for {@code application/cbor} (Smithy test cases carry binary bodies as base64), UTF-8 otherwise. A null body is
     * treated as empty.
     */
    public static StubbedResponse fromTestCase(int statusCode, Map<String, String> headers, String body,
                                               String bodyMediaType) {
        return new StubbedResponse(statusCode, headers, testCaseBodyBytes(body, bodyMediaType));
    }

    static byte[] testCaseBodyBytes(String body, String bodyMediaType) {
        if (body == null || body.isEmpty()) {
            return EMPTY;
        }
        if (BodyComparators.isCbor(bodyMediaType)) {
            return Base64.getDecoder().decode(body);
        }
        return body.getBytes(StandardCharsets.UTF_8);
    }

    public int statusCode() {
        return statusCode;
    }

    public Map<String, String> headers() {
        return headers;
    }

    /**
     * A copy of the body bytes. The array is empty when the response has no body.
     */
    public byte[] body() {
        return body.clone();
    }
}
