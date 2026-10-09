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

package software.amazon.awssdk.benchmark.reference.v2;

import java.net.URI;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.awscore.retry.AwsRetryStrategy;
import software.amazon.awssdk.benchmark.harness.CellId;
import software.amazon.awssdk.benchmark.harness.CellWorkload;
import software.amazon.awssdk.benchmark.harness.DynamoDbPutItemFixture;
import software.amazon.awssdk.benchmark.harness.Outcome;
import software.amazon.awssdk.benchmark.harness.PayloadClass;
import software.amazon.awssdk.http.SdkHttpClient;
import software.amazon.awssdk.http.async.SdkAsyncHttpClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.retries.api.BackoffStrategy;
import software.amazon.awssdk.retries.api.RetryStrategy;
import software.amazon.awssdk.services.dynamodb.DynamoDbAsyncClient;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.PutItemRequest;
import software.amazon.awssdk.services.dynamodb.model.PutItemResponse;
import software.amazon.awssdk.services.dynamodb.model.ResourceNotFoundException;
import software.amazon.awssdk.services.dynamodb.model.ReturnValue;

/**
 * The awsJson1.0 fixture on the released V2 DynamoDB clients: {@code PutItem} with {@code ReturnValues=ALL_OLD} of
 * the shared {@link DynamoDbPutItemFixture} item, so V2 sends the fixture's exact request bytes (the validity rules'
 * wire-bytes and unmatched-request checks apply to this arm unchanged). Each client is built once per process like
 * the V3 arm's: static credentials, a fixed region, the endpoint override, the cell's explicit transport and the
 * benchmark retry strategy (V2's standard AWS retry conditions, {@link DynamoDbPutItemFixture#MAX_ATTEMPTS} attempts,
 * no backoff, no circuit breaker).
 *
 * <p>Shared: test/sdk-benchmarks-racecar-reference compiles this source against the Racecar jars (Racecar keeps
 * V2's client API), hence public.
 */
public final class V2DynamoDbWorkloads {

    private V2DynamoDbWorkloads() {
    }

    public static CellWorkload create(CellId cell, URI endpoint) {
        DynamoDbPutItemFixture fixture = new DynamoDbPutItemFixture(PayloadClass.fromId(cell.payload()));
        boolean expectError = Outcome.fromId(cell.outcome()) == Outcome.MODELED_ERROR;
        if (V2Transports.isAsync(cell.transport())) {
            SdkAsyncHttpClient http = V2Transports.async(cell.transport(), cell.concurrency());
            try {
                return new GeneratedAsync(cell, fixture, expectError, endpoint, http);
            } catch (RuntimeException e) {
                http.close();
                throw e;
            }
        }
        SdkHttpClient http = V2Transports.sync(cell.transport(), cell.concurrency());
        try {
            return new GeneratedSync(cell, fixture, expectError, endpoint, http);
        } catch (RuntimeException e) {
            http.close();
            throw e;
        }
    }

    public static RetryStrategy retryStrategy() {
        return AwsRetryStrategy.standardRetryStrategy().toBuilder()
                               .maxAttempts(DynamoDbPutItemFixture.MAX_ATTEMPTS)
                               .backoffStrategy(BackoffStrategy.retryImmediately())
                               .throttlingBackoffStrategy(BackoffStrategy.retryImmediately())
                               .circuitBreakerEnabled(false)
                               .build();
    }

    public static StaticCredentialsProvider credentials() {
        return StaticCredentialsProvider.create(AwsBasicCredentials.create("benchmark-akid", "benchmark-secret"));
    }

    public static PutItemRequest request(DynamoDbPutItemFixture fixture) {
        Map<String, AttributeValue> item = new LinkedHashMap<>();
        item.put("pk", AttributeValue.fromS(DynamoDbPutItemFixture.KEY));
        item.put("pad", AttributeValue.fromS(fixture.pad()));
        return PutItemRequest.builder().tableName(DynamoDbPutItemFixture.TABLE).item(item)
                             .returnValues(ReturnValue.ALL_OLD).build();
    }

    /** Shared outcome checks, as in the V3 arm. */
    private abstract static class Base implements CellWorkload {
        final CellId cell;
        final DynamoDbPutItemFixture fixture;
        final boolean expectError;
        final AutoCloseable transport;
        final PutItemRequest request;

        Base(CellId cell, DynamoDbPutItemFixture fixture, boolean expectError, AutoCloseable transport) {
            this.cell = cell;
            this.fixture = fixture;
            this.expectError = expectError;
            this.transport = transport;
            this.request = request(fixture);
        }

        @Override
        public String transportClass() {
            return transport.getClass().getName();
        }

        void checkError(Throwable t) {
            Throwable cause = t instanceof CompletionException && t.getCause() != null ? t.getCause() : t;
            if (expectError && cause instanceof ResourceNotFoundException) {
                return;
            }
            throw new IllegalStateException(cell + ": unexpected outcome " + cause, cause);
        }

        void unexpectedSuccess() {
            if (expectError) {
                throw new IllegalStateException(cell + ": unexpected outcome: success in an error cell");
            }
        }

        void checkPad(PutItemResponse response) {
            if (!fixture.pad().equals(response.attributes().get("pad").s())) {
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

        abstract void closeClient();
    }

    private static final class GeneratedSync extends Base {
        private final DynamoDbClient client;

        GeneratedSync(CellId cell, DynamoDbPutItemFixture fixture, boolean expectError, URI endpoint,
                      SdkHttpClient http) {
            super(cell, fixture, expectError, http);
            this.client = DynamoDbClient.builder().region(Region.US_EAST_1).credentialsProvider(credentials())
                                        .endpointOverride(endpoint).httpClient(http)
                                        .overrideConfiguration(o -> o.retryStrategy(retryStrategy())).build();
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
        public void verify() {
            PutItemResponse response = invoke();
            if (response != null) {
                checkPad(response);
            }
        }

        @Override
        void closeClient() {
            client.close();
        }
    }

    private static final class GeneratedAsync extends Base {
        private final DynamoDbAsyncClient client;

        GeneratedAsync(CellId cell, DynamoDbPutItemFixture fixture, boolean expectError, URI endpoint,
                       SdkAsyncHttpClient http) {
            super(cell, fixture, expectError, http);
            this.client = DynamoDbAsyncClient.builder().region(Region.US_EAST_1).credentialsProvider(credentials())
                                             .endpointOverride(endpoint).httpClient(http)
                                             .overrideConfiguration(o -> o.retryStrategy(retryStrategy())).build();
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
        public void verify() {
            try {
                PutItemResponse response = client.putItem(request).join();
                unexpectedSuccess();
                checkPad(response);
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
