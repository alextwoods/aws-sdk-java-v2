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

package software.amazon.awssdk.test.concurrency;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;
import software.amazon.awssdk.core.waiters.WaiterOverrideConfiguration;
import software.amazon.awssdk.core.waiters.WaiterResponse;
import software.amazon.awssdk.services.dynamodb.DynamoDbAsyncClient;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.DescribeTableResponse;
import software.amazon.awssdk.services.dynamodb.model.ListTablesRequest;
import software.amazon.awssdk.services.dynamodb.model.ListTablesResponse;
import software.amazon.awssdk.services.dynamodb.waiters.DynamoDbAsyncWaiter;
import software.amazon.awssdk.services.dynamodb.waiters.DynamoDbWaiter;
import software.amazon.awssdk.test.concurrency.support.Clients;
import software.amazon.awssdk.test.concurrency.support.FakeDynamoDb;

/**
 * Concurrency suite of generated paginators and waiters over one shared client, sync and async (Requirements 7.x/8.x
 * paginators and waiters via 19.9, 19.10): {@value #SIMULTANEOUS} simultaneous paginations or waits, {@value #TOTAL}
 * in total, a bounded {@value #TIMEOUT_SECONDS}-second run.
 *
 * <p>Each pagination walks all {@value #TABLES} tables in pages of {@value #PAGE_SIZE} ({@value #PAGES} pages); each
 * wait polls its own table, which reports {@code CREATING} {@value #CREATING_POLLS} times before {@code ACTIVE}
 * ({@value #POLLS} polls). The transport's overlap barrier (sync) or gate (async) holds every page request and every
 * poll until all {@value #SIMULTANEOUS} paginations or waits have one in flight, so the k-th pages (polls) of a round
 * overlap. One shared waiter instance serves every wait. Every pagination must see every table exactly once, in
 * order, and every wait must end on its own table after exactly {@value #POLLS} polls.
 */
class PaginatorWaiterConcurrencyTest {
    static final int SIMULTANEOUS = 8;
    static final int TOTAL = 64;
    static final int TIMEOUT_SECONDS = 60;
    static final int TABLES = 20;
    static final int PAGE_SIZE = 5;
    static final int PAGES = TABLES / PAGE_SIZE;
    static final int CREATING_POLLS = 2;
    static final int POLLS = CREATING_POLLS + 1;
    static final WaiterOverrideConfiguration FAST = WaiterOverrideConfiguration.builder()
                                                                                .minDelay(Duration.ofMillis(1))
                                                                                .maxDelay(Duration.ofMillis(1))
                                                                                .maxAttempts(POLLS + 2)
                                                                                .build();
    static final List<String> ALL_TABLES = IntStream.range(0, TABLES).mapToObj(FakeDynamoDb::tableName).toList();

    private final List<FakeDynamoDb> fakes = new ArrayList<>();
    private final ExecutorService completion = Clients.completionExecutor("paginator-completion", 3);
    private final ScheduledExecutorService delays = Executors.newScheduledThreadPool(2);

    @AfterEach
    void shutdown() {
        fakes.forEach(FakeDynamoDb::shutdown);
        Clients.shutdown(completion);
        Clients.shutdown(delays);
    }

    @Test
    @Timeout(value = TIMEOUT_SECONDS + 10, unit = TimeUnit.SECONDS)
    void syncPaginator_overlappingPaginations_eachSeesEveryPageOnce() {
        ConcurrencyPlan plan = plan("sync-paginator");
        OverlapBarrier overlap = plan.overlapBarrier();
        FakeDynamoDb http = fake(FakeDynamoDb.sync().tables(TABLES).onRequest(r -> overlap.arrive()));

        try (DynamoDbClient client = Clients.sync(http).build()) {
            List<List<String>> listings = ConcurrentRun.run(plan, call -> {
                List<String> names = new ArrayList<>();
                client.listTablesPaginator(ListTablesRequest.builder().limit(PAGE_SIZE).build()).tableNames()
                      .forEach(names::add);
                return names;
            }, overlap::breakBarrier);
            listings.forEach(names -> assertEquals(ALL_TABLES, names));
        }
        assertEquals(plan.rounds() * PAGES, overlap.trips(), "the k-th pages of every round overlapped");
        assertEquals((long) TOTAL * PAGES, http.count("ListTables"), "each page was fetched exactly once");
        assertEquals(0, http.openExchanges());
    }

    @Test
    @Timeout(value = TIMEOUT_SECONDS + 10, unit = TimeUnit.SECONDS)
    void asyncPaginator_overlappingPaginations_eachSeesEveryPageOnce() {
        ConcurrencyPlan plan = plan("async-paginator");
        AsyncOverlapGate gate = plan.overlapGate();
        FakeDynamoDb http = fake(FakeDynamoDb.async().tables(TABLES).gate(r -> gate.arrive()));

        try (DynamoDbAsyncClient client = Clients.async(http).asyncCompletionExecutor(completion).build()) {
            List<List<String>> listings = ConcurrentRun.runAsync(plan, call -> {
                PageCollector pages = new PageCollector();
                client.listTablesPaginator(ListTablesRequest.builder().limit(PAGE_SIZE).build()).subscribe(pages);
                return pages.result;
            }, gate::failPending);
            listings.forEach(names -> assertEquals(ALL_TABLES, names));
        }
        assertEquals(plan.rounds() * PAGES, gate.trips());
        assertEquals((long) TOTAL * PAGES, http.count("ListTables"));
        assertEquals(0, http.openExchanges());
    }

    @Test
    @Timeout(value = TIMEOUT_SECONDS + 10, unit = TimeUnit.SECONDS)
    void sharedSyncWaiter_overlappingWaits_eachEndsOnItsOwnTable() {
        ConcurrencyPlan plan = plan("sync-waiter");
        OverlapBarrier overlap = plan.overlapBarrier();
        FakeDynamoDb http = fake(FakeDynamoDb.sync().creatingPolls(CREATING_POLLS).onRequest(r -> overlap.arrive()));

        try (DynamoDbClient client = Clients.sync(http).build();
             DynamoDbWaiter waiter = DynamoDbWaiter.builder().client(client).overrideConfiguration(FAST).build()) {
            List<WaiterResponse<DescribeTableResponse>> responses = ConcurrentRun.run(
                plan, call -> waiter.waitUntilTableExists(r -> r.tableName(waitTable(call))), overlap::breakBarrier);
            assertWaits(plan, responses);
        }
        assertEquals(plan.rounds() * POLLS, overlap.trips(), "the k-th polls of every round overlapped");
        assertEquals((long) TOTAL * POLLS, http.count("DescribeTable"));
    }

    @Test
    @Timeout(value = TIMEOUT_SECONDS + 10, unit = TimeUnit.SECONDS)
    void sharedAsyncWaiter_overlappingWaits_eachEndsOnItsOwnTable() {
        ConcurrencyPlan plan = plan("async-waiter");
        AsyncOverlapGate gate = plan.overlapGate();
        FakeDynamoDb http = fake(FakeDynamoDb.async().creatingPolls(CREATING_POLLS).gate(r -> gate.arrive()));

        try (DynamoDbAsyncClient client = Clients.async(http).asyncCompletionExecutor(completion).build();
             DynamoDbAsyncWaiter waiter = DynamoDbAsyncWaiter.builder().client(client).scheduledExecutorService(delays)
                                                             .overrideConfiguration(FAST).build()) {
            List<WaiterResponse<DescribeTableResponse>> responses = ConcurrentRun.runAsync(
                plan, call -> waiter.waitUntilTableExists(r -> r.tableName(waitTable(call))), gate::failPending);
            assertWaits(plan, responses);
        }
        assertEquals(plan.rounds() * POLLS, gate.trips());
        assertEquals((long) TOTAL * POLLS, http.count("DescribeTable"));
    }

    private static void assertWaits(ConcurrencyPlan plan, List<WaiterResponse<DescribeTableResponse>> responses) {
        for (int i = 0; i < TOTAL; i++) {
            WaiterResponse<DescribeTableResponse> response = responses.get(i);
            ConcurrentRun.Call call = GeneratedSyncClientConcurrencyTest.callOf(plan, i);
            DescribeTableResponse table = response.matched().response().orElseThrow();
            assertEquals(waitTable(call), table.table().tableName(), "the wait of " + call);
            assertEquals("ACTIVE", table.table().tableStatusAsString());
            assertEquals(POLLS, response.attemptsExecuted());
            assertEquals((long) POLLS, table.table().itemCount(), "the table's own poll counter");
        }
    }

    private static String waitTable(ConcurrentRun.Call call) {
        return "wait-" + call.index();
    }

    private static ConcurrencyPlan plan(String component) {
        return ConcurrencyPlan.of(component, SIMULTANEOUS, TOTAL, Duration.ofSeconds(TIMEOUT_SECONDS));
    }

    private FakeDynamoDb fake(FakeDynamoDb fake) {
        fakes.add(fake);
        return fake;
    }

    /** Requests one page at a time and collects the table names. */
    private static final class PageCollector implements Subscriber<ListTablesResponse> {
        final CompletableFuture<List<String>> result = new CompletableFuture<>();
        private final List<String> names = new ArrayList<>();
        private Subscription subscription;

        @Override
        public void onSubscribe(Subscription s) {
            subscription = s;
            s.request(1);
        }

        @Override
        public void onNext(ListTablesResponse page) {
            names.addAll(page.tableNames());
            subscription.request(1);
        }

        @Override
        public void onError(Throwable t) {
            result.completeExceptionally(t);
        }

        @Override
        public void onComplete() {
            result.complete(List.copyOf(names));
        }
    }
}
