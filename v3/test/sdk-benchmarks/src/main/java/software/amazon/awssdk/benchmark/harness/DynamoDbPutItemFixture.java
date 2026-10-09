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

/**
 * The SDK-free wire half of the awsJson1.0 fixture (DynamoDB {@code PutItem} with {@code ReturnValues=ALL_OLD}): the
 * padded item, the exact request body every arm must send, and the responses the mock service serves. Deterministic
 * in the payload class, so the mock-service process, the V3 client process and every reference client process build
 * the identical bytes from the cell alone. The SDK-typed request builders live with each SDK's workloads
 * ({@code DynamoDbWorkloads} for V3).
 */
public class DynamoDbPutItemFixture implements ServiceFixture {

    public static final String TABLE = "benchmark-table";
    public static final String KEY = "benchmark-key";
    public static final String TARGET = "DynamoDB_20120810.PutItem";
    public static final String CONTENT_TYPE = "application/x-amz-json-1.0";
    public static final String NOT_FOUND = "com.amazonaws.dynamodb#ResourceNotFoundException";
    /** See {@code Outcome#RETRY}: each attempt of a retry cell fails with probability 1/2. */
    public static final int MAX_ATTEMPTS = 64;

    private final PayloadClass payload;
    private final String pad;
    private final byte[] requestJson;
    private final MockResponse success;
    private final MockResponse error;
    private final MockResponse retryable;

    public DynamoDbPutItemFixture(PayloadClass payload) {
        this.payload = payload;
        int overhead = requestJson("").length + successJson("").length;
        this.pad = "x".repeat(Math.max(1, (payload.targetBytes() - overhead) / 2));
        this.requestJson = requestJson(pad);
        this.success = new MockResponse(200, CONTENT_TYPE, successJson(pad));
        this.error = new MockResponse(400, CONTENT_TYPE, utf8("{\"__type\":\"" + NOT_FOUND
                                                              + "\",\"message\":\"Requested resource not found\"}"));
        this.retryable = new MockResponse(500, CONTENT_TYPE, utf8(
            "{\"__type\":\"com.amazonaws.dynamodb#InternalServerError\",\"message\":\"injected retry\"}"));
        for (long bytes : new long[] {encodedBytes(), requestJson.length + error.body().length}) {
            if (!payload.contains(bytes)) {
                throw new IllegalStateException("fixture of " + bytes + " encoded bytes is not " + payload.id());
            }
        }
    }

    private static byte[] requestJson(String pad) {
        return utf8("{\"TableName\":\"" + TABLE + "\",\"Item\":" + itemJson(pad) + ",\"ReturnValues\":\"ALL_OLD\"}");
    }

    private static byte[] successJson(String pad) {
        return utf8("{\"Attributes\":" + itemJson(pad) + "}");
    }

    private static String itemJson(String pad) {
        return "{\"pk\":{\"S\":\"" + KEY + "\"},\"pad\":{\"S\":\"" + pad + "\"}}";
    }

    private static byte[] utf8(String s) {
        return s.getBytes(StandardCharsets.UTF_8);
    }

    /** The {@code pad} attribute's string value. */
    public String pad() {
        return pad;
    }

    /** The exact request body of one attempt. */
    public byte[] requestJson() {
        return requestJson.clone();
    }

    public PayloadClass payload() {
        return payload;
    }

    @Override
    public boolean matches(String method, String path, String routingHeader) {
        return "POST".equals(method) && "/".equals(path) && TARGET.equals(routingHeader);
    }

    @Override
    public String routingHeaderName() {
        return "X-Amz-Target";
    }

    @Override
    public MockResponse success() {
        return success;
    }

    @Override
    public MockResponse modeledError() {
        return error;
    }

    @Override
    public MockResponse retryableError() {
        return retryable;
    }

    @Override
    public long encodedBytes() {
        return requestJson.length + (long) success.body().length;
    }
}
