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

package software.amazon.awssdk.test.concurrency.support;

import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import software.amazon.awssdk.identity.spi.AwsCredentialsIdentity;
import software.amazon.awssdk.identity.spi.IdentityProvider;
import software.amazon.awssdk.identity.spi.ResolveIdentityRequest;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.retries.BackoffStrategy;
import software.amazon.awssdk.retries.StandardRetryStrategy;
import software.amazon.awssdk.services.dynamodb.DynamoDbAsyncClient;
import software.amazon.awssdk.services.dynamodb.DynamoDbAsyncClientBuilder;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.DynamoDbClientBuilder;

/** Builders of the generated DynamoDB clients over a {@link FakeDynamoDb}, with deterministic credentials. */
public final class Clients {
    /** Static test credentials, so the SigV4 clients never consult the default chain. */
    public static final IdentityProvider<AwsCredentialsIdentity> CREDENTIALS = new IdentityProvider<>() {
        @Override
        public Class<AwsCredentialsIdentity> identityType() {
            return AwsCredentialsIdentity.class;
        }

        @Override
        public CompletableFuture<AwsCredentialsIdentity> resolveIdentity(ResolveIdentityRequest request) {
            return CompletableFuture.completedFuture(
                AwsCredentialsIdentity.create("AKIDCONCURRENCY", "concurrency-test-secret-key"));
        }
    };

    private Clients() {
    }

    /** A sync client builder over {@code http} with static credentials and no retries. */
    public static DynamoDbClientBuilder sync(FakeDynamoDb http) {
        return DynamoDbClient.builder().region(Region.of("us-east-1")).credentialsProvider(CREDENTIALS)
                             .endpointOverride(FakeDynamoDb.ENDPOINT).httpClient(http)
                             .retryStrategy(StandardRetryStrategy.doNotRetry());
    }

    /** An async client builder over {@code http} with static credentials and no retries. */
    public static DynamoDbAsyncClientBuilder async(FakeDynamoDb http) {
        return DynamoDbAsyncClient.builder().region(Region.of("us-east-1")).credentialsProvider(CREDENTIALS)
                                  .endpointOverride(FakeDynamoDb.ENDPOINT).httpClient(http)
                                  .retryStrategy(StandardRetryStrategy.doNotRetry());
    }

    /** A standard strategy of {@code maxAttempts} attempts without backoff, with an explicit retry quota. */
    public static StandardRetryStrategy.Builder retries(int maxAttempts) {
        return StandardRetryStrategy.builder().maxAttempts(maxAttempts).backoffStrategy(BackoffStrategy.none())
                                    .throttlingBackoffStrategy(BackoffStrategy.none());
    }

    /** A completion executor with named daemon threads; the caller shuts it down. */
    public static ExecutorService completionExecutor(String name, int threads) {
        AtomicInteger ids = new AtomicInteger();
        return Executors.newFixedThreadPool(threads, r -> {
            Thread t = new Thread(r, name + "-" + ids.getAndIncrement());
            t.setDaemon(true);
            return t;
        });
    }

    /** Shuts {@code executor} down and waits a bounded time for it. */
    public static void shutdown(ExecutorService executor) {
        executor.shutdownNow();
        try {
            executor.awaitTermination(Duration.ofSeconds(5).toMillis(), TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
