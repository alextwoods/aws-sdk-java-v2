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

package software.amazon.awssdk.enhanced.dynamodb.functionaltests;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;
import software.amazon.awssdk.awscore.exception.AwsServiceException;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbAsyncTable;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedAsyncClient;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.Key;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.enhanced.dynamodb.internal.json.JsonNodeParser;
import software.amazon.awssdk.enhanced.dynamodb.model.Page;
import software.amazon.awssdk.enhanced.dynamodb.model.QueryConditional;
import software.amazon.awssdk.enhanced.dynamodb.model.QueryEnhancedRequest;
import software.amazon.awssdk.enhanced.dynamodb.testing.Collect;
import software.amazon.awssdk.enhanced.dynamodb.testing.EnhancedApi.TableApi;
import software.amazon.awssdk.enhanced.dynamodb.testing.EnhancedApi;
import software.amazon.awssdk.enhanced.dynamodb.testing.FakeDynamoDbHttp.Fault;
import software.amazon.awssdk.enhanced.dynamodb.testing.FakeDynamoDbHttp;
import software.amazon.awssdk.enhanced.dynamodb.testing.InMemoryDynamoDb;
import software.amazon.awssdk.enhanced.dynamodb.testing.models.Order;
import software.amazon.awssdk.http.CancellationReason;
import software.amazon.awssdk.identity.spi.AwsCredentialsIdentity;
import software.amazon.awssdk.identity.spi.IdentityProvider;
import software.amazon.awssdk.identity.spi.ResolveIdentityRequest;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.dynamodb.DynamoDbAsyncClient;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.ConditionalCheckFailedException;
import software.amazon.awssdk.services.dynamodb.model.ProvisionedThroughputExceededException;

/**
 * Deterministic fault injection under the DynamoDB Enhanced Client (Requirements 19.7, 19.8). Every case runs through
 * the enhanced sync and async clients over the real generated DynamoDB clients, with faults injected at the HTTP
 * transport, and verifies the terminal outcome seen by the enhanced API, the number of HTTP attempts, the applied
 * retry delay (where a hint applies), cancellation propagation and that every HTTP exchange was released. The
 * clients retry up to {@value #MAX_ATTEMPTS} attempts with no backoff, so attempt counts and delays are exact.
 */
class EnhancedClientFaultInjectionTest {
    private static final int MAX_ATTEMPTS = 3;
    private static final Key KEY = Key.builder().partitionValue("c1").sortValue(1).build();

    enum Mode { SYNC, ASYNC }

    private InMemoryDynamoDb service;
    private FakeDynamoDbHttp http;
    private AutoCloseable client;

    @AfterEach
    void close() throws Exception {
        if (client != null) {
            client.close();
        }
    }

    private TableApi<Order> orders(Mode mode) {
        return orders(mode, FakeDynamoDbHttp.CREDENTIALS, Region.of("us-east-1"));
    }

    private TableApi<Order> orders(Mode mode, IdentityProvider<AwsCredentialsIdentity> credentials, Region region) {
        return orders(mode, credentials, region, FakeDynamoDbHttp.ENDPOINT);
    }

    private TableApi<Order> orders(Mode mode, IdentityProvider<AwsCredentialsIdentity> credentials, Region region,
                                   java.net.URI endpointOverride) {
        service = new InMemoryDynamoDb();
        EnhancedApi api;
        if (mode == Mode.SYNC) {
            http = FakeDynamoDbHttp.sync(service);
            DynamoDbClient sync = DynamoDbClient.builder().region(region).credentialsProvider(credentials)
                                                .endpointOverride(endpointOverride).httpClient(http)
                                                .retryStrategy(FakeDynamoDbHttp.retries(MAX_ATTEMPTS)).build();
            client = sync;
            api = EnhancedApi.sync(DynamoDbEnhancedClient.builder().dynamoDbClient(sync).build());
        } else {
            http = FakeDynamoDbHttp.async(service);
            DynamoDbAsyncClient async = DynamoDbAsyncClient.builder().region(region).credentialsProvider(credentials)
                                                           .endpointOverride(endpointOverride)
                                                           .httpClient(http)
                                                           .retryStrategy(FakeDynamoDbHttp.retries(MAX_ATTEMPTS))
                                                           .build();
            client = async;
            api = EnhancedApi.async(DynamoDbEnhancedAsyncClient.builder().dynamoDbClient(async).build());
        }
        return api.table("orders", TableSchema.fromBean(Order.class));
    }

    /** Creates the table and one item directly in the service, so only the call under test goes over HTTP. */
    private void seed() {
        service.execute("CreateTable", JsonNodeParser.create().parse(
            "{\"TableName\":\"orders\",\"KeySchema\":[{\"AttributeName\":\"customerId\",\"KeyType\":\"HASH\"},"
            + "{\"AttributeName\":\"orderNumber\",\"KeyType\":\"RANGE\"}]}"));
        for (int n = 1; n <= 3; n++) {
            service.execute("PutItem", JsonNodeParser.create().parse(
                "{\"TableName\":\"orders\",\"Item\":{\"customerId\":{\"S\":\"c1\"},\"orderNumber\":{\"N\":\"" + n
                + "\"},\"status\":{\"S\":\"OPEN\"},\"amount\":{\"N\":\"" + (10 * n) + "\"},\"version\":{\"N\":\"1\"}}}"));
        }
    }

    private void assertReleased() {
        assertThat(http.openExchanges()).as("open HTTP exchanges").isZero();
    }

    @ParameterizedTest
    @EnumSource(Mode.class)
    void retryableTransportFaultsAreRetriedBelowTheEnhancedCall(Mode mode) {
        TableApi<Order> orders = orders(mode);
        seed();
        http.inject(Fault.connectionFailure(), Fault.partialRequestBody());
        assertThat(orders.getItem(KEY).getAmount()).isEqualTo(10);
        assertThat(http.requests()).hasSize(3);

        http.inject(Fault.timeout());
        assertThat(orders.getItem(KEY).getAmount()).isEqualTo(10);
        assertThat(http.requests()).hasSize(5);
        assertReleased();
    }

    @ParameterizedTest
    @EnumSource(Mode.class)
    void exhaustedConnectionFailuresSurfaceAsAClientFailureAfterMaxAttempts(Mode mode) {
        TableApi<Order> orders = orders(mode);
        seed();
        http.inject(Fault.connectionFailure(), Fault.connectionFailure(), Fault.connectionFailure());
        assertThatThrownBy(() -> orders.getItem(KEY)).isInstanceOf(SdkClientException.class);
        assertThat(http.requests()).hasSize(MAX_ATTEMPTS);
        assertReleased();
    }

    @ParameterizedTest
    @EnumSource(Mode.class)
    void partialResponseBodyIsRetried(Mode mode) {
        TableApi<Order> orders = orders(mode);
        seed();
        http.inject(Fault.partialResponseBody());
        assertThat(orders.getItem(KEY).getAmount()).isEqualTo(10);
        assertThat(http.requests()).hasSize(2);
        assertReleased();
    }

    @ParameterizedTest
    @EnumSource(Mode.class)
    void throttlingAndServerErrorsAreRetriedThenSucceed(Mode mode) {
        TableApi<Order> orders = orders(mode);
        seed();
        http.inject(Fault.throttling(), Fault.internalServerError());
        assertThat(orders.queryItems(QueryEnhancedRequest.builder()
                                                         .queryConditional(QueryConditional.keyEqualTo(
                                                             k -> k.partitionValue("c1")))
                                                         .build()))
            .extracting(Order::getOrderNumber).containsExactly(1, 2, 3);
        assertThat(http.operations()).containsExactly("Query", "Query", "Query");
        assertReleased();
    }

    @ParameterizedTest
    @EnumSource(Mode.class)
    void persistentThrottlingSurfacesTheModeledThrottlingException(Mode mode) {
        TableApi<Order> orders = orders(mode);
        seed();
        http.inject(Fault.throttling(), Fault.throttling(), Fault.throttling());
        assertThatThrownBy(() -> orders.getItem(KEY)).isInstanceOf(ProvisionedThroughputExceededException.class);
        assertThat(http.requests()).hasSize(MAX_ATTEMPTS);
        assertReleased();
    }

    @ParameterizedTest
    @EnumSource(Mode.class)
    void retryDelayHintIsAppliedBetweenAttempts(Mode mode) {
        TableApi<Order> orders = orders(mode);
        seed();
        http.inject(Fault.throttlingWithRetryAfter(300));
        assertThat(orders.getItem(KEY).getAmount()).isEqualTo(10);
        List<FakeDynamoDbHttp.Recorded> attempts = http.requests();
        assertThat(attempts).hasSize(2);
        long gapMillis = TimeUnit.NANOSECONDS.toMillis(attempts.get(1).nanoTime() - attempts.get(0).nanoTime());
        // The backoff is zero, so the applied delay is the hint: at least 300 ms and well below the 5 s cap.
        assertThat(gapMillis).isBetween(300L, 4_000L);
        assertReleased();
    }

    @ParameterizedTest
    @EnumSource(Mode.class)
    void malformedSuccessResponseFailsTheEnhancedCall(Mode mode) {
        TableApi<Order> orders = orders(mode);
        seed();
        http.inject(Fault.malformedSuccess(), Fault.malformedSuccess(), Fault.malformedSuccess());
        assertThatThrownBy(() -> orders.getItem(KEY)).isInstanceOf(SdkClientException.class);
        assertThat(http.requests()).hasSizeBetween(1, MAX_ATTEMPTS);
        assertReleased();
    }

    @ParameterizedTest
    @EnumSource(Mode.class)
    void modeledServiceErrorIsNotRetriedAndKeepsItsType(Mode mode) {
        TableApi<Order> orders = orders(mode);
        seed();
        // A real failed condition: the versioned-record extension expects version 1, the service has version 1, but
        // the write carries version 5.
        Order stale = Order.of("c1", 1, "OPEN", 99);
        stale.setVersion(5L);
        assertThatThrownBy(() -> orders.putItem(stale)).isInstanceOf(ConditionalCheckFailedException.class);
        assertThat(http.requests()).hasSize(1);
        assertThat(orders.getItem(KEY).getAmount()).isEqualTo(10);
        assertReleased();
    }

    @ParameterizedTest
    @EnumSource(Mode.class)
    void unmodeledServiceErrorSurfacesAsTheServiceBaseException(Mode mode) {
        TableApi<Order> orders = orders(mode);
        seed();
        http.inject(Fault.error(400, "SomethingNewException", "an error this SDK does not model"));
        // V2 maps an unmodeled error code to the service base exception (DynamoDbException); the V3 generated client
        // currently surfaces the AWS base type, its supertype, with the same error details (reported as a V3
        // service-runtime difference, not an enhanced-client one).
        assertThatThrownBy(() -> orders.getItem(KEY))
            .isInstanceOfSatisfying(AwsServiceException.class, e -> {
                assertThat(e).isNotInstanceOf(ConditionalCheckFailedException.class);
                assertThat(e.awsErrorDetails().errorCode()).isEqualTo("SomethingNewException");
                assertThat(e.statusCode()).isEqualTo(400);
            });
        assertThat(http.requests()).hasSize(1);
        assertReleased();
    }

    @ParameterizedTest
    @EnumSource(Mode.class)
    void identityFailureFailsBeforeAnyRequestIsSent(Mode mode) {
        IdentityProvider<AwsCredentialsIdentity> failing = new IdentityProvider<>() {
            @Override
            public Class<AwsCredentialsIdentity> identityType() {
                return AwsCredentialsIdentity.class;
            }

            @Override
            public CompletableFuture<AwsCredentialsIdentity> resolveIdentity(ResolveIdentityRequest request) {
                return CompletableFuture.failedFuture(new IllegalStateException("no credentials for the test"));
            }
        };
        TableApi<Order> orders = orders(mode, failing, Region.of("us-east-1"));
        seed();
        assertThatThrownBy(() -> orders.getItem(KEY)).isInstanceOf(SdkClientException.class)
                                                     .hasStackTraceContaining("no credentials for the test");
        assertThat(http.requests()).isEmpty();
        assertReleased();
    }

    @ParameterizedTest
    @EnumSource(Mode.class)
    void endpointFailureFailsBeforeAnyRequestIsSent(Mode mode) {
        // No endpoint override: the modeled endpoint rules reject a region that is not a valid host label.
        TableApi<Order> orders = orders(mode, FakeDynamoDbHttp.CREDENTIALS, Region.of("not a valid region"), null);
        seed();
        assertThatThrownBy(() -> orders.getItem(KEY)).isInstanceOf(SdkClientException.class);
        assertThat(http.requests()).isEmpty();
        assertReleased();
    }

    @Test
    @Timeout(30)
    void cancellingAnEnhancedFutureCancelsTheHttpExchange() throws Exception {
        orders(Mode.ASYNC);
        seed();
        DynamoDbAsyncTable<Order> table = asyncTable();
        http.inject(Fault.hang());
        CompletableFuture<Order> pending = table.getItem(KEY);
        assertThat(http.awaitHangingRequest(Duration.ofSeconds(10))).isTrue();
        assertThat(pending.cancel(true)).isTrue();
        awaitCancellation();
        assertThat(http.cancellations()).containsExactly(CancellationReason.CALLER_CANCELLED);
        assertThat(pending).isCancelled();
        assertReleased();
        assertThat(http.requests()).hasSize(1);
    }

    @Test
    @Timeout(30)
    void cancellingAPageSubscriptionCancelsTheInFlightPageRequest() throws Exception {
        orders(Mode.ASYNC);
        seed();
        DynamoDbAsyncTable<Order> table = asyncTable();
        CompletableFuture<Page<Order>> firstPage = new CompletableFuture<>();
        Subscription[] subscription = new Subscription[1];
        table.query(QueryEnhancedRequest.builder().queryConditional(QueryConditional.keyEqualTo(
                 k -> k.partitionValue("c1"))).limit(1).build())
             .subscribe(new Subscriber<Page<Order>>() {
                 @Override
                 public void onSubscribe(Subscription s) {
                     subscription[0] = s;
                     s.request(1);
                 }

                 @Override
                 public void onNext(Page<Order> page) {
                     firstPage.complete(page);
                 }

                 @Override
                 public void onError(Throwable t) {
                     firstPage.completeExceptionally(t);
                 }

                 @Override
                 public void onComplete() {
                 }
             });
        assertThat(Collect.join(firstPage).items()).extracting(Order::getOrderNumber).containsExactly(1);
        http.inject(Fault.hang());
        subscription[0].request(1);
        assertThat(http.awaitHangingRequest(Duration.ofSeconds(10))).isTrue();
        subscription[0].cancel();
        awaitCancellation();
        assertThat(http.cancellations()).containsExactly(CancellationReason.CALLER_CANCELLED);
        assertReleased();
        assertThat(http.requests()).hasSize(2);
    }

    @Test
    @Timeout(30)
    void cancellingAnItemSubscriptionStopsFetchingPages() {
        orders(Mode.ASYNC);
        seed();
        DynamoDbAsyncTable<Order> table = asyncTable();
        List<Order> first = Collect.join(Collect.first(
            table.query(QueryEnhancedRequest.builder().queryConditional(QueryConditional.keyEqualTo(
                k -> k.partitionValue("c1"))).limit(1).build()).items(), 1));
        assertThat(first).extracting(Order::getOrderNumber).containsExactly(1);
        // One page was enough for the one item requested; no further page was fetched after cancellation.
        assertThat(http.requests()).hasSize(1);
        assertReleased();
    }

    private DynamoDbAsyncTable<Order> asyncTable() {
        return DynamoDbEnhancedAsyncClient.builder().dynamoDbClient((DynamoDbAsyncClient) client).build()
                                          .table("orders", TableSchema.fromBean(Order.class));
    }

    private void awaitCancellation() throws InterruptedException {
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(10);
        while ((http.cancellations().isEmpty() || http.openExchanges() != 0) && System.nanoTime() < deadline) {
            Thread.sleep(10);
        }
    }
}
