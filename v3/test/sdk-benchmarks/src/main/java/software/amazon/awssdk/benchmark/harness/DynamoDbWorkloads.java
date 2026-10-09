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

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import software.amazon.awssdk.dynamic.client.DynamicAsyncClient;
import software.amazon.awssdk.dynamic.client.DynamicClient;
import software.amazon.awssdk.dynamic.client.DynamicServiceException;
import software.amazon.awssdk.http.SdkAsyncHttpClient;
import software.amazon.awssdk.http.SdkSyncHttpClient;
import software.amazon.awssdk.identity.providers.StaticCredentialsProvider;
import software.amazon.awssdk.identity.spi.AwsCredentialsIdentity;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.retries.BackoffStrategy;
import software.amazon.awssdk.retries.RetryStrategy;
import software.amazon.awssdk.retries.StandardRetryStrategy;
import software.amazon.awssdk.services.dynamodb.DynamoDbAsyncClient;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.PutItemRequest;
import software.amazon.awssdk.services.dynamodb.model.PutItemResponse;
import software.amazon.awssdk.services.dynamodb.model.ResourceNotFoundException;
import software.amazon.awssdk.services.dynamodb.model.ReturnValue;
import software.amazon.smithy.java.core.serde.document.Document;
import software.amazon.smithy.model.shapes.ShapeId;

/**
 * The awsJson1.0 fixture: DynamoDB {@code PutItem} with {@code ReturnValues=ALL_OLD}, on the generated DynamoDB
 * clients and the Dynamic Client over the same Smithy model. The item carries one string attribute padded so that
 * the request (the item) plus the success response (the old item) hit the payload class's target size; the error
 * response is the modeled {@code ResourceNotFoundException}, and the retry response the retryable modeled
 * {@code InternalServerError} (HTTP 500).
 *
 * <p>Every client is fully built once per process with static credentials, a fixed region, the endpoint override, the
 * cell's transport, and the benchmark retry strategy: no backoff, no circuit breaker, {@link #MAX_ATTEMPTS} attempts,
 * identical in every cell so the outcome, not the strategy, is the variable.
 */
final class DynamoDbWorkloads implements WorkloadProvider {

    static final String TABLE = "benchmark-table";
    static final String KEY = "benchmark-key";
    static final String TARGET = "DynamoDB_20120810.PutItem";
    static final String CONTENT_TYPE = "application/x-amz-json-1.0";
    static final ShapeId SERVICE = ShapeId.from("com.amazonaws.dynamodb#DynamoDB_20120810");
    static final String NOT_FOUND = "com.amazonaws.dynamodb#ResourceNotFoundException";
    /** See {@link Outcome#RETRY}: each attempt of a retry cell fails with probability 1/2. */
    static final int MAX_ATTEMPTS = 64;
    /** The default model location relative to the module directory (the build's {@code aws.codegen.modelDirectory}). */
    static final String DEFAULT_MODEL = "../../../.kiro/reference/aws-models/dynamodb/smithy/model.json";

    @Override
    public String name() {
        return "dynamodb-putitem";
    }

    @Override
    public boolean supports(BenchmarkCell cell) {
        return cell.protocol() == BenchProtocol.AWS_JSON_1_0 && cell.kind() == WorkloadKind.FINITE;
    }

    @Override
    public Fixture fixture(BenchmarkCell cell) {
        return new Fixture(cell.payload());
    }

    @Override
    public CellWorkload create(BenchmarkCell cell, URI endpoint, Options options) {
        Fixture fixture = fixture(cell);
        AutoCloseable transport = cell.transport().create(cell.concurrency());
        try {
            return switch (cell.path()) {
                case GENERATED_SYNC -> new GeneratedSync(cell, fixture, endpoint, (SdkSyncHttpClient) transport);
                case GENERATED_ASYNC -> new GeneratedAsync(cell, fixture, endpoint, (SdkAsyncHttpClient) transport);
                case DYNAMIC_SYNC -> new DynamicSync(cell, fixture, endpoint, (SdkSyncHttpClient) transport,
                                                     model(options));
                case DYNAMIC_ASYNC -> new DynamicAsync(cell, fixture, endpoint, (SdkAsyncHttpClient) transport,
                                                       model(options));
            };
        } catch (RuntimeException e) {
            closeQuietly(transport);
            throw e;
        }
    }

    private static Path model(Options options) {
        return options.models().getOrDefault("dynamodb", Path.of(DEFAULT_MODEL));
    }

    static RetryStrategy retryStrategy() {
        return StandardRetryStrategy.builder().maxAttempts(MAX_ATTEMPTS).backoffStrategy(BackoffStrategy.none())
                                    .throttlingBackoffStrategy(BackoffStrategy.none()).circuitBreakerEnabled(false)
                                    .build();
    }

    static StaticCredentialsProvider credentials() {
        return StaticCredentialsProvider.create(AwsCredentialsIdentity.create("benchmark-akid", "benchmark-secret"));
    }

    private static void closeQuietly(AutoCloseable c) {
        try {
            c.close();
        } catch (Exception ignored) {
            // best effort while failing construction
        }
    }

    /** The wire bytes of one cell; deterministic in the payload class. */
    static final class Fixture implements ServiceFixture {
        private final PayloadClass payload;
        private final String pad;
        private final byte[] requestJson;
        private final MockResponse success;
        private final MockResponse error;
        private final MockResponse retryable;

        Fixture(PayloadClass payload) {
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

        String pad() {
            return pad;
        }

        byte[] requestJson() {
            return requestJson.clone();
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

        Map<String, AttributeValue> item() {
            Map<String, AttributeValue> item = new LinkedHashMap<>();
            item.put("pk", AttributeValue.fromS(KEY));
            item.put("pad", AttributeValue.fromS(pad));
            return item;
        }

        Document itemDocument() {
            Map<String, Document> item = new LinkedHashMap<>();
            item.put("pk", Document.of(Map.of("S", Document.of(KEY))));
            item.put("pad", Document.of(Map.of("S", Document.of(pad))));
            return Document.of(item);
        }

        PayloadClass payload() {
            return payload;
        }
    }

    private static byte[] utf8(String s) {
        return s.getBytes(StandardCharsets.UTF_8);
    }

    /** Shared outcome checks. */
    private abstract static class Base implements CellWorkload {
        final BenchmarkCell cell;
        final Fixture fixture;
        final AutoCloseable transport;
        final boolean expectError;

        Base(BenchmarkCell cell, Fixture fixture, AutoCloseable transport) {
            this.cell = cell;
            this.fixture = fixture;
            this.transport = transport;
            this.expectError = cell.outcome() == Outcome.MODELED_ERROR;
        }

        @Override
        public String transportClass() {
            return transport.getClass().getName();
        }

        /** Maps a thrown error to the expected outcome: the modeled not-found error in error cells only. */
        void checkError(Throwable t) {
            Throwable cause = t instanceof CompletionException && t.getCause() != null ? t.getCause() : t;
            if (expectError && isNotFound(cause)) {
                return;
            }
            throw new UnexpectedOutcomeException(cell, cause);
        }

        abstract boolean isNotFound(Throwable t);

        void unexpectedSuccess() {
            if (expectError) {
                throw new UnexpectedOutcomeException(cell, null);
            }
        }

        void checkPad(String pad) {
            if (!fixture.pad().equals(pad)) {
                throw new IllegalStateException(cell + ": decoded attribute does not match the fixture");
            }
        }

        @Override
        public void close() throws Exception {
            try {
                closeClient();
            } finally {
                transport.close();
            }
        }

        abstract void closeClient() throws Exception;
    }

    private static final class GeneratedSync extends Base {
        private final DynamoDbClient client;
        private final PutItemRequest request;

        GeneratedSync(BenchmarkCell cell, Fixture fixture, URI endpoint, SdkSyncHttpClient http) {
            super(cell, fixture, http);
            this.client = DynamoDbClient.builder().region(Region.US_EAST_1).credentialsProvider(credentials())
                                        .endpointOverride(endpoint).retryStrategy(retryStrategy()).httpClient(http)
                                        .build();
            this.request = PutItemRequest.builder().tableName(TABLE).item(fixture.item())
                                         .returnValues(ReturnValue.ALL_OLD).build();
        }

        @Override
        public void call() {
            invoke();
        }

        private PutItemResponse invoke() {
            PutItemResponse response;
            try {
                response = client.putItem(request);
            } catch (RuntimeException e) {
                checkError(e);
                return null;
            }
            unexpectedSuccess();
            return response;
        }

        @Override
        public CompletableFuture<Void> callAsync() {
            throw new UnsupportedOperationException("sync path");
        }

        @Override
        public boolean isAsync() {
            return false;
        }

        @Override
        boolean isNotFound(Throwable t) {
            return t instanceof ResourceNotFoundException;
        }

        @Override
        public void verify() {
            PutItemResponse response = invoke();
            if (response != null) {
                checkPad(response.attributes().get("pad").s());
            }
        }

        @Override
        void closeClient() {
            client.close();
        }
    }

    private static final class GeneratedAsync extends Base {
        private final DynamoDbAsyncClient client;
        private final PutItemRequest request;

        GeneratedAsync(BenchmarkCell cell, Fixture fixture, URI endpoint, SdkAsyncHttpClient http) {
            super(cell, fixture, http);
            this.client = DynamoDbAsyncClient.builder().region(Region.US_EAST_1).credentialsProvider(credentials())
                                             .endpointOverride(endpoint).retryStrategy(retryStrategy())
                                             .httpClient(http).build();
            this.request = PutItemRequest.builder().tableName(TABLE).item(fixture.item())
                                         .returnValues(ReturnValue.ALL_OLD).build();
        }

        @Override
        public void call() {
            callAsync().join();
        }

        @Override
        public CompletableFuture<Void> callAsync() {
            return client.putItem(request).handle((r, t) -> {
                if (t != null) {
                    checkError(t);
                } else {
                    unexpectedSuccess();
                }
                return null;
            });
        }

        @Override
        public boolean isAsync() {
            return true;
        }

        @Override
        boolean isNotFound(Throwable t) {
            return t instanceof ResourceNotFoundException;
        }

        @Override
        public void verify() {
            try {
                PutItemResponse response = client.putItem(request).join();
                unexpectedSuccess();
                checkPad(response.attributes().get("pad").s());
            } catch (CompletionException e) {
                checkError(e);
            }
        }

        @Override
        void closeClient() {
            client.close();
        }
    }

    private static Document dynamicInput(Fixture fixture) {
        Map<String, Document> input = new LinkedHashMap<>();
        input.put("TableName", Document.of(TABLE));
        input.put("Item", fixture.itemDocument());
        input.put("ReturnValues", Document.of("ALL_OLD"));
        return Document.of(input);
    }

    private static boolean isDynamicNotFound(Throwable t) {
        return t instanceof DynamicServiceException d && NOT_FOUND.equals(d.errorShapeId().toString());
    }

    private static String dynamicPad(Document output) {
        return output.getMember("Attributes").getMember("pad").getMember("S").asString();
    }

    private static final class DynamicSync extends Base {
        private final DynamicClient client;
        private final Document input;

        DynamicSync(BenchmarkCell cell, Fixture fixture, URI endpoint, SdkSyncHttpClient http, Path model) {
            super(cell, fixture, http);
            this.client = DynamicClient.builder().model(model).serviceId(SERVICE).region(Region.US_EAST_1)
                                       .credentialsProvider(credentials()).endpointOverride(endpoint)
                                       .retryStrategy(retryStrategy()).httpClient(http).build();
            this.input = dynamicInput(fixture);
        }

        private Document invoke() {
            Document output;
            try {
                output = client.call("PutItem", input);
            } catch (RuntimeException e) {
                checkError(e);
                return null;
            }
            unexpectedSuccess();
            return output;
        }

        @Override
        public void call() {
            invoke();
        }

        @Override
        public CompletableFuture<Void> callAsync() {
            throw new UnsupportedOperationException("sync path");
        }

        @Override
        public boolean isAsync() {
            return false;
        }

        @Override
        boolean isNotFound(Throwable t) {
            return isDynamicNotFound(t);
        }

        @Override
        public void verify() {
            Document output = invoke();
            if (output != null) {
                checkPad(dynamicPad(output));
            }
        }

        @Override
        void closeClient() {
            client.close();
        }
    }

    private static final class DynamicAsync extends Base {
        private final DynamicAsyncClient client;
        private final Document input;

        DynamicAsync(BenchmarkCell cell, Fixture fixture, URI endpoint, SdkAsyncHttpClient http, Path model) {
            super(cell, fixture, http);
            this.client = DynamicAsyncClient.builder().model(model).serviceId(SERVICE).region(Region.US_EAST_1)
                                            .credentialsProvider(credentials()).endpointOverride(endpoint)
                                            .retryStrategy(retryStrategy()).httpClient(http).build();
            this.input = dynamicInput(fixture);
        }

        @Override
        public void call() {
            callAsync().join();
        }

        @Override
        public CompletableFuture<Void> callAsync() {
            return client.callAsync("PutItem", input).handle((r, t) -> {
                if (t != null) {
                    checkError(t);
                } else {
                    unexpectedSuccess();
                }
                return null;
            });
        }

        @Override
        public boolean isAsync() {
            return true;
        }

        @Override
        boolean isNotFound(Throwable t) {
            return isDynamicNotFound(t);
        }

        @Override
        public void verify() {
            try {
                Document output = client.callAsync("PutItem", input).join();
                unexpectedSuccess();
                checkPad(dynamicPad(output));
            } catch (CompletionException e) {
                checkError(e);
            }
        }

        @Override
        void closeClient() {
            client.close();
        }
    }

}
