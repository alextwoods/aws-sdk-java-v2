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

package software.amazon.awssdk.benchmark.smithyserde;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.zip.CRC32;
import software.amazon.awssdk.http.AbortableInputStream;
import software.amazon.awssdk.http.ExecutableHttpRequest;
import software.amazon.awssdk.http.HttpExecuteRequest;
import software.amazon.awssdk.http.HttpExecuteResponse;
import software.amazon.awssdk.http.SdkHttpClient;
import software.amazon.awssdk.http.SdkHttpResponse;

/**
 * An in-process {@link SdkHttpClient} that answers each DynamoDB operation with the same canned response
 * bytes {@code MockDdbServer} serves, keyed the same way (on {@code X-Amz-Target}).
 *
 * <p>It does the transport's share of work and no more: reads the whole request body, as a real client
 * writing it to a socket would, and hands back a fresh stream over a precomputed body with the headers
 * DynamoDB sends, {@code x-amz-crc32} included, so v2's CRC32 check runs as it does against the service.
 * Nothing here depends on which serde produced the request, so both arms pay exactly the same cost.
 */
final class CannedDynamoDbHttpClient implements SdkHttpClient {

    private static final String TARGET_PREFIX = "DynamoDB_20120810.";

    private final Map<String, Canned> responses = new HashMap<>();
    private final byte[] drain = new byte[16 * 1024];

    CannedDynamoDbHttpClient() {
        put("GetItem", BenchmarkItems.getItemResponseJson());
        put("PutItem", BenchmarkItems.putItemResponseJson());
        put("BatchGetItem", BenchmarkItems.batchGetItemResponseJson());
        put("BatchWriteItem", BenchmarkItems.batchWriteItemResponseJson());
    }

    private void put(String operation, String json) {
        byte[] body = json.getBytes(StandardCharsets.UTF_8);
        CRC32 crc = new CRC32();
        crc.update(body);
        SdkHttpResponse headers = SdkHttpResponse.builder()
                                                 .statusCode(200)
                                                 .statusText("OK")
                                                 .putHeader("x-amzn-RequestId", "BENCHMARKREQUESTID0000000000000000000000000000")
                                                 .putHeader("x-amz-crc32", Long.toString(crc.getValue()))
                                                 .putHeader("Content-Type", "application/x-amz-json-1.0")
                                                 .putHeader("Content-Length", Integer.toString(body.length))
                                                 .build();
        responses.put(TARGET_PREFIX + operation, new Canned(headers, body));
    }

    @Override
    public ExecutableHttpRequest prepareRequest(HttpExecuteRequest request) {
        String target = request.httpRequest().firstMatchingHeader("X-Amz-Target").orElse(null);
        Canned canned = responses.get(target);
        if (canned == null) {
            throw new IllegalStateException("No canned response for " + target);
        }
        request.contentStreamProvider().ifPresent(p -> drain(p.newStream()));
        return new ExecutableHttpRequest() {
            @Override
            public HttpExecuteResponse call() {
                return HttpExecuteResponse.builder()
                                          .response(canned.headers)
                                          .responseBody(AbortableInputStream.create(new ByteArrayInputStream(canned.body)))
                                          .build();
            }

            @Override
            public void abort() {
            }
        };
    }

    private void drain(InputStream in) {
        try (InputStream stream = in) {
            while (stream.read(drain) != -1) {
                // discard, as a socket write would
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @Override
    public void close() {
    }

    @Override
    public String clientName() {
        return "CannedDynamoDb";
    }

    private static final class Canned {
        private final SdkHttpResponse headers;
        private final byte[] body;

        private Canned(SdkHttpResponse headers, byte[] body) {
            this.headers = headers;
            this.body = body;
        }
    }
}
