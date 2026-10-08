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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static software.amazon.awssdk.test.concurrency.GeneratedSyncClientConcurrencyTest.callOf;
import static software.amazon.awssdk.test.concurrency.GeneratedSyncClientConcurrencyTest.id;
import static software.amazon.awssdk.test.concurrency.GeneratedSyncClientConcurrencyTest.payload;
import static software.amazon.awssdk.test.concurrency.GeneratedSyncClientConcurrencyTest.s;
import static software.amazon.awssdk.test.concurrency.GeneratedSyncClientConcurrencyTest.table;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.ForkJoinWorkerThread;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import software.amazon.awssdk.pipeline.ExecutionInterceptor;
import software.amazon.awssdk.pipeline.HookContext;
import software.amazon.awssdk.services.dynamodb.DynamoDbAsyncClient;
import software.amazon.awssdk.test.concurrency.support.Clients;
import software.amazon.awssdk.test.concurrency.support.FakeDynamoDb;

/**
 * Concurrency suite of one shared generated async client (Requirements 8.13, 19.9, 19.10): {@value #SIMULTANEOUS}
 * simultaneous calls, {@value #TOTAL} total calls, a bounded {@value #TIMEOUT_SECONDS}-second run. The transport holds
 * every exchange in an {@link AsyncOverlapGate} until {@value #SIMULTANEOUS} calls are in flight inside it together,
 * then answers them on its fake event loop; continuations run on a completion executor of
 * {@value #COMPLETION_THREADS} threads (fewer than the simultaneous calls, so no thread per call is needed). Every
 * call must complete with the answer to its own request, and no SDK work may run on the event loop or the common
 * fork-join pool.
 */
class GeneratedAsyncClientConcurrencyTest {
    static final int SIMULTANEOUS = 8;
    static final int TOTAL = 64;
    static final int TIMEOUT_SECONDS = 60;
    static final int COMPLETION_THREADS = 3;

    private final FakeDynamoDb http = FakeDynamoDb.async();
    private final ExecutorService completion = Clients.completionExecutor("async-client-completion",
                                                                          COMPLETION_THREADS);
    private final Queue<Thread> hookThreads = new ConcurrentLinkedQueue<>();
    private final ExecutionInterceptor threadRecorder = new ExecutionInterceptor() {
        @Override
        public void beforeDeserialization(HookContext.BeforeDeserialization context) {
            hookThreads.add(Thread.currentThread());
        }

        @Override
        public void afterExecution(HookContext.AfterExecution context) {
            hookThreads.add(Thread.currentThread());
        }
    };

    @AfterEach
    void shutdown() {
        http.shutdown();
        Clients.shutdown(completion);
    }

    @Test
    @Timeout(value = TIMEOUT_SECONDS + 10, unit = TimeUnit.SECONDS)
    void sharedAsyncClient_overlappingGetItemCalls_eachCompletesWithItsOwnResponse() {
        ConcurrencyPlan plan = ConcurrencyPlan.of("async-client-get", SIMULTANEOUS, TOTAL,
                                                  Duration.ofSeconds(TIMEOUT_SECONDS));
        AsyncOverlapGate gate = plan.overlapGate();
        http.gate(r -> gate.arrive());

        try (DynamoDbAsyncClient client = Clients.async(http).asyncCompletionExecutor(completion)
                                                 .addExecutionInterceptor(threadRecorder).build()) {
            List<String> echoes = ConcurrentRun.runAsync(plan, call -> client.getItem(
                r -> r.tableName(table(call)).key(Map.of("id", s(id(call))))).thenApply(g -> g.item().get("echo").s()),
                                                         gate::failPending);
            for (int i = 0; i < TOTAL; i++) {
                ConcurrentRun.Call call = callOf(plan, i);
                assertEquals(table(call) + "/" + id(call), echoes.get(i), "the response of " + call);
            }
        }
        assertEquals(plan.rounds(), gate.trips(), "every round had all simultaneous calls in flight together");
        assertEquals(SIMULTANEOUS, gate.maxPending());
        assertEquals(SIMULTANEOUS, http.maxInFlight());
        assertEquals(TOTAL, http.count("GetItem"));
        assertEquals(0, http.openExchanges(), "every exchange terminated");
        assertSdkWorkOffTheEventLoopAndCommonPool();
    }

    @Test
    @Timeout(value = TIMEOUT_SECONDS + 10, unit = TimeUnit.SECONDS)
    void sharedAsyncClient_overlappingPutThenGetChains_storeAndReadEveryItemExactlyOnce() {
        ConcurrencyPlan plan = ConcurrencyPlan.of("async-client-put-get", SIMULTANEOUS, TOTAL,
                                                  Duration.ofSeconds(TIMEOUT_SECONDS));
        AsyncOverlapGate gate = plan.overlapGate();
        http.gate(r -> gate.arrive());

        try (DynamoDbAsyncClient client = Clients.async(http).asyncCompletionExecutor(completion)
                                                 .addExecutionInterceptor(threadRecorder).build()) {
            List<String> payloads = ConcurrentRun.runAsync(plan, call -> client.putItem(
                    r -> r.tableName("shared").item(Map.of("id", s(id(call)), "payload", s(payload(call)))))
                .thenCompose(put -> client.getItem(r -> r.tableName("shared").key(Map.of("id", s(id(call))))))
                .thenApply(g -> g.item().get("payload").s()), gate::failPending);
            for (int i = 0; i < TOTAL; i++) {
                assertEquals(payload(callOf(plan, i)), payloads.get(i));
            }
        }
        // The puts of a round fill one wave and the gets of the round the next one.
        assertEquals(2 * plan.rounds(), gate.trips());
        assertEquals(TOTAL, http.count("PutItem"));
        assertEquals(TOTAL, http.count("GetItem"));
        assertEquals(TOTAL, http.storedItems());
        assertEquals(0, http.openExchanges());
        assertSdkWorkOffTheEventLoopAndCommonPool();
    }

    private void assertSdkWorkOffTheEventLoopAndCommonPool() {
        assertFalse(hookThreads.isEmpty());
        for (Thread thread : hookThreads) {
            assertFalse(thread.getName().startsWith(FakeDynamoDb.EVENT_LOOP), "SDK work on the event loop: " + thread);
            assertFalse(thread instanceof ForkJoinWorkerThread worker && worker.getPool() == ForkJoinPool.commonPool(),
                        "SDK work on the common pool: " + thread);
            assertTrue(thread.getName().startsWith("async-client-completion"), "SDK work on " + thread);
        }
    }
}
