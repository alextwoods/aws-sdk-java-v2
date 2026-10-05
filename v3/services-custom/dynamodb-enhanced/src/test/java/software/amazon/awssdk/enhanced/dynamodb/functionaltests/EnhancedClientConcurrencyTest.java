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

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.IntStream;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbAsyncTable;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedAsyncClient;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.Key;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.enhanced.dynamodb.model.Page;
import software.amazon.awssdk.enhanced.dynamodb.model.QueryConditional;
import software.amazon.awssdk.enhanced.dynamodb.model.UpdateItemEnhancedRequest;
import software.amazon.awssdk.enhanced.dynamodb.testing.Collect;
import software.amazon.awssdk.enhanced.dynamodb.testing.FakeDynamoDbHttp;
import software.amazon.awssdk.enhanced.dynamodb.testing.InMemoryDynamoDb;
import software.amazon.awssdk.enhanced.dynamodb.testing.models.Order;
import software.amazon.awssdk.enhanced.dynamodb.testing.models.Product;
import software.amazon.awssdk.services.dynamodb.DynamoDbAsyncClient;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.ConditionalCheckFailedException;

/**
 * Documented concurrency tests of the DynamoDB Enhanced Client (Requirements 19.9, 19.10).
 *
 * <p>Every test uses {@value #SIMULTANEOUS_CALLS} simultaneous callers (at least two) and
 * {@value #TOTAL_OPERATIONS} total operations (more than the simultaneous-call count), released round by round by a
 * {@link CyclicBarrier} of {@value #SIMULTANEOUS_CALLS} parties so that each round's calls overlap, and completes
 * within the bounded {@value #TIMEOUT_SECONDS}-second timeout. One enhanced client, table resource and table schema
 * are shared by all callers, as the V2 thread-safety contract allows.
 */
class EnhancedClientConcurrencyTest {
    static final int SIMULTANEOUS_CALLS = 8;
    static final int TOTAL_OPERATIONS = 64;
    static final int ROUNDS = TOTAL_OPERATIONS / SIMULTANEOUS_CALLS;
    static final int TIMEOUT_SECONDS = 30;

    private final InMemoryDynamoDb service = new InMemoryDynamoDb();
    private ExecutorService callers;

    @BeforeEach
    void startCallers() {
        callers = Executors.newFixedThreadPool(SIMULTANEOUS_CALLS);
    }

    @AfterEach
    void stopCallers() throws InterruptedException {
        callers.shutdownNow();
        assertThat(callers.awaitTermination(TIMEOUT_SECONDS, TimeUnit.SECONDS)).isTrue();
    }

    @Test
    @Timeout(TIMEOUT_SECONDS)
    void concurrentOptimisticLockingIncrementsLoseNoUpdate() throws Exception {
        FakeDynamoDbHttp http = FakeDynamoDbHttp.sync(service);
        try (DynamoDbClient client = FakeDynamoDbHttp.syncClient(http, 1)) {
            DynamoDbTable<Order> orders = DynamoDbEnhancedClient.builder().dynamoDbClient(client).build()
                                                                .table("orders", TableSchema.fromBean(Order.class));
            orders.createTable();
            orders.putItem(Order.of("shared", 1, "OPEN", 0));
            Key key = Key.builder().partitionValue("shared").sortValue(1).build();
            AtomicInteger conflicts = new AtomicInteger();
            CyclicBarrier barrier = new CyclicBarrier(SIMULTANEOUS_CALLS);

            List<Future<?>> lanes = new ArrayList<>();
            for (int lane = 0; lane < SIMULTANEOUS_CALLS; lane++) {
                lanes.add(callers.submit(() -> {
                    for (int round = 0; round < ROUNDS; round++) {
                        barrier.await(TIMEOUT_SECONDS, TimeUnit.SECONDS);
                        // Read-modify-write retried on a version conflict: the versioned-record extension makes each
                        // update conditional on the version that was read.
                        while (true) {
                            Order current = orders.getItem(key);
                            current.setAmount(current.getAmount() + 1);
                            try {
                                orders.updateItem(current);
                                break;
                            } catch (ConditionalCheckFailedException e) {
                                conflicts.incrementAndGet();
                            }
                        }
                    }
                    return null;
                }));
            }
            for (Future<?> lane : lanes) {
                lane.get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
            }

            Order result = orders.getItem(key);
            assertThat(result.getAmount()).isEqualTo(TOTAL_OPERATIONS);
            assertThat(result.getVersion()).isEqualTo(TOTAL_OPERATIONS + 1L);
            assertThat(http.openExchanges()).isZero();
            // Contended rounds conflict; every conflict was retried rather than lost.
            assertThat(http.operations().stream().filter("UpdateItem"::equals).count())
                .isEqualTo(TOTAL_OPERATIONS + conflicts.get());
        }
    }

    @Test
    @Timeout(TIMEOUT_SECONDS)
    void concurrentAsyncWritesAndPaginatedQueriesSeeEveryItem() throws Exception {
        FakeDynamoDbHttp http = FakeDynamoDbHttp.async(service);
        try (DynamoDbAsyncClient client = FakeDynamoDbHttp.asyncClient(http, 1)) {
            DynamoDbEnhancedAsyncClient enhanced = DynamoDbEnhancedAsyncClient.builder().dynamoDbClient(client).build();
            DynamoDbAsyncTable<Order> orders = enhanced.table("orders", TableSchema.fromBean(Order.class));
            Collect.join(orders.createTable());
            CyclicBarrier barrier = new CyclicBarrier(SIMULTANEOUS_CALLS);

            List<Future<List<CompletableFuture<Void>>>> lanes = new ArrayList<>();
            for (int lane = 0; lane < SIMULTANEOUS_CALLS; lane++) {
                String customer = "lane-" + lane;
                lanes.add(callers.submit(() -> {
                    List<CompletableFuture<Void>> writes = new ArrayList<>();
                    for (int round = 0; round < ROUNDS; round++) {
                        barrier.await(TIMEOUT_SECONDS, TimeUnit.SECONDS);
                        writes.add(orders.putItem(Order.of(customer, round, "OPEN", round)));
                    }
                    return writes;
                }));
            }
            List<CompletableFuture<Void>> writes = new ArrayList<>();
            for (Future<List<CompletableFuture<Void>>> lane : lanes) {
                writes.addAll(lane.get(TIMEOUT_SECONDS, TimeUnit.SECONDS));
            }
            assertThat(writes).hasSize(TOTAL_OPERATIONS);
            Collect.join(CompletableFuture.allOf(writes.toArray(new CompletableFuture[0])));

            // Every lane pages through its partition (one item per page) at the same time.
            barrier.reset();
            List<Future<List<Integer>>> queries = new ArrayList<>();
            for (int lane = 0; lane < SIMULTANEOUS_CALLS; lane++) {
                String customer = "lane-" + lane;
                queries.add(callers.submit(() -> {
                    barrier.await(TIMEOUT_SECONDS, TimeUnit.SECONDS);
                    List<Page<Order>> pages = Collect.join(Collect.all(orders.query(
                        r -> r.queryConditional(QueryConditional.keyEqualTo(k -> k.partitionValue(customer)))
                              .limit(1))));
                    return pages.stream().flatMap(p -> p.items().stream()).map(Order::getOrderNumber).toList();
                }));
            }
            for (Future<List<Integer>> query : queries) {
                assertThat(query.get(TIMEOUT_SECONDS, TimeUnit.SECONDS))
                    .containsExactlyElementsOf(IntStream.range(0, ROUNDS).boxed().toList());
            }
            assertThat(service.items("orders")).hasSize(TOTAL_OPERATIONS);
            assertThat(http.openExchanges()).isZero();
        }
    }

    @Test
    @Timeout(TIMEOUT_SECONDS)
    void concurrentAtomicCounterUpdatesAreAllApplied() throws Exception {
        FakeDynamoDbHttp http = FakeDynamoDbHttp.async(service);
        try (DynamoDbAsyncClient client = FakeDynamoDbHttp.asyncClient(http, 1)) {
            DynamoDbAsyncTable<Product> products =
                DynamoDbEnhancedAsyncClient.builder().dynamoDbClient(client).build()
                                           .table("products", TableSchema.fromImmutableClass(Product.class));
            Collect.join(products.createTable());
            CyclicBarrier barrier = new CyclicBarrier(SIMULTANEOUS_CALLS);
            List<Future<List<CompletableFuture<Product>>>> lanes = new ArrayList<>();
            for (int lane = 0; lane < SIMULTANEOUS_CALLS; lane++) {
                lanes.add(callers.submit(() -> {
                    List<CompletableFuture<Product>> updates = new ArrayList<>();
                    for (int round = 0; round < ROUNDS; round++) {
                        barrier.await(TIMEOUT_SECONDS, TimeUnit.SECONDS);
                        updates.add(products.updateItem(UpdateItemEnhancedRequest.builder(Product.class)
                                                                                 .item(Product.builder().sku("p")
                                                                                              .name("Lamp").build())
                                                                                 .build()));
                    }
                    return updates;
                }));
            }
            List<Long> observed = new ArrayList<>();
            for (Future<List<CompletableFuture<Product>>> lane : lanes) {
                for (CompletableFuture<Product> update : lane.get(TIMEOUT_SECONDS, TimeUnit.SECONDS)) {
                    observed.add(Collect.join(update).views());
                }
            }
            // startValue 10, delta 5: the n-th applied update observes 10 + 5(n - 1); no two observe the same value.
            assertThat(observed).hasSize(TOTAL_OPERATIONS).doesNotHaveDuplicates();
            assertThat(observed.stream().mapToLong(Long::longValue).max().getAsLong())
                .isEqualTo(10 + 5L * (TOTAL_OPERATIONS - 1));
            assertThat(http.openExchanges()).isZero();
        }
    }
}
