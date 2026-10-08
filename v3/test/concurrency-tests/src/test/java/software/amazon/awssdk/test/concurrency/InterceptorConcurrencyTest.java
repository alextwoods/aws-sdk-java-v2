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
import static software.amazon.awssdk.test.concurrency.GeneratedSyncClientConcurrencyTest.id;
import static software.amazon.awssdk.test.concurrency.GeneratedSyncClientConcurrencyTest.s;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.LongAdder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import software.amazon.awssdk.core.context.ContextKey;
import software.amazon.awssdk.http.SdkHttpRequest;
import software.amazon.awssdk.pipeline.ExecutionInterceptor;
import software.amazon.awssdk.pipeline.HookContext;
import software.amazon.awssdk.services.dynamodb.DynamoDbAsyncClient;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.GetItemRequest;
import software.amazon.awssdk.services.dynamodb.model.GetItemResponse;
import software.amazon.awssdk.test.concurrency.support.Clients;
import software.amazon.awssdk.test.concurrency.support.FakeDynamoDb;
import software.amazon.smithy.java.core.schema.SerializableStruct;

/**
 * Concurrency suite of one interceptor instance shared by every call of a client (Requirements 11.x via 19.9,
 * 19.10): {@value #SIMULTANEOUS} simultaneous calls, {@value #TOTAL} total calls, a bounded
 * {@value #TIMEOUT_SECONDS}-second run, sync and async. Interceptors are client constants and keep per-call state
 * only in the typed contexts; the interceptor here stores each call's key in its {@link HookContext#execution()
 * execution context}, stamps it on the transport request in {@code modifyBeforeSigning}, and checks it against the
 * request, the wire and the response in later hooks. Its shared counters count every hook. In the sync suite an
 * {@link OverlapBarrier} inside {@code beforeTransmission} proves {@value #SIMULTANEOUS} calls are inside the
 * interceptor chain together; the async suite holds the exchanges in an {@link AsyncOverlapGate}.
 */
class InterceptorConcurrencyTest {
    static final int SIMULTANEOUS = 8;
    static final int TOTAL = 64;
    static final int TIMEOUT_SECONDS = 60;
    static final String CALL_HEADER = "x-concurrency-call";
    static final List<String> HOOKS = List.of("beforeExecution", "modifyRequest", "beforeAttempt",
                                              "modifyBeforeSigning", "beforeTransmission", "afterTransmission",
                                              "beforeDeserialization", "afterAttempt", "modifyResponse",
                                              "afterExecution");

    private final Queue<String> mismatches = new ConcurrentLinkedQueue<>();
    private final ExecutorService completion = Clients.completionExecutor("interceptor-completion", 3);
    private FakeDynamoDb http;

    @AfterEach
    void shutdown() {
        if (http != null) {
            http.shutdown();
        }
        Clients.shutdown(completion);
    }

    @Test
    @Timeout(value = TIMEOUT_SECONDS + 10, unit = TimeUnit.SECONDS)
    void sharedInterceptor_overlappingSyncCalls_keepsPerCallStateInTheContexts() {
        ConcurrencyPlan plan = ConcurrencyPlan.of("sync-interceptor", SIMULTANEOUS, TOTAL,
                                                  Duration.ofSeconds(TIMEOUT_SECONDS));
        OverlapBarrier overlap = plan.overlapBarrier();
        CallStateInterceptor interceptor = new CallStateInterceptor(overlap::arrive);
        http = FakeDynamoDb.sync().onRequest(this::checkWire);

        try (DynamoDbClient client = Clients.sync(http).addExecutionInterceptor(interceptor).build()) {
            List<String> echoes = ConcurrentRun.run(plan, call -> client.getItem(
                r -> r.tableName("t").key(Map.of("id", s(id(call))))).item().get("echo").s(), overlap::breakBarrier);
            for (int i = 0; i < TOTAL; i++) {
                assertEquals("t/call-" + i, echoes.get(i));
            }
        }
        assertEquals(plan.rounds(), overlap.trips());
        assertEquals(SIMULTANEOUS, overlap.maxInside(), "calls were inside the interceptor chain together");
        interceptor.assertEveryHookRan(TOTAL);
        assertEquals(List.of(), List.copyOf(mismatches), "per-call state crossed between calls");
    }

    @Test
    @Timeout(value = TIMEOUT_SECONDS + 10, unit = TimeUnit.SECONDS)
    void sharedInterceptor_overlappingAsyncCalls_keepsPerCallStateInTheContexts() {
        ConcurrencyPlan plan = ConcurrencyPlan.of("async-interceptor", SIMULTANEOUS, TOTAL,
                                                  Duration.ofSeconds(TIMEOUT_SECONDS));
        AsyncOverlapGate gate = plan.overlapGate();
        CallStateInterceptor interceptor = new CallStateInterceptor(() -> { });
        http = FakeDynamoDb.async().onRequest(this::checkWire).gate(r -> gate.arrive());

        try (DynamoDbAsyncClient client = Clients.async(http).asyncCompletionExecutor(completion)
                                                 .addExecutionInterceptor(interceptor).build()) {
            List<String> echoes = ConcurrentRun.runAsync(plan, call -> client.getItem(
                r -> r.tableName("t").key(Map.of("id", s(id(call))))).thenApply(g -> g.item().get("echo").s()),
                                                         gate::failPending);
            for (int i = 0; i < TOTAL; i++) {
                assertEquals("t/call-" + i, echoes.get(i));
            }
        }
        assertEquals(plan.rounds(), gate.trips());
        assertEquals(SIMULTANEOUS, gate.maxPending());
        interceptor.assertEveryHookRan(TOTAL);
        assertEquals(List.of(), List.copyOf(mismatches), "per-call state crossed between calls");
    }

    /** The transport sees the header the interceptor stamped for the request's own key. */
    private void checkWire(FakeDynamoDb.Received r) {
        if (!r.id().equals(r.header(CALL_HEADER))) {
            mismatches.add("wire: key " + r.id() + " carried header " + r.header(CALL_HEADER));
        }
    }

    /** Keeps each call's key in its execution context and checks it in every later hook. */
    private final class CallStateInterceptor implements ExecutionInterceptor {
        private final ContextKey<String> callKey = ContextKey.execution("concurrency.call", String.class);
        private final ContextKey<String> attemptKey = ContextKey.attempt("concurrency.attempt", String.class);
        private final Map<String, LongAdder> hooks = new ConcurrentHashMap<>();
        private final Runnable insideTransmission;

        CallStateInterceptor(Runnable insideTransmission) {
            this.insideTransmission = insideTransmission;
        }

        @Override
        public void beforeExecution(HookContext.BeforeExecution context) {
            count("beforeExecution");
            if (context.execution().get(callKey) != null) {
                mismatches.add("beforeExecution: a fresh call already had state " + context.execution().get(callKey));
            }
            context.execution().put(callKey, key(context.request()));
        }

        @Override
        public SerializableStruct modifyRequest(HookContext.ModifyRequest context) {
            count("modifyRequest");
            check("modifyRequest", context, context.execution().get(callKey));
            return context.request();
        }

        @Override
        public void beforeAttempt(HookContext.BeforeAttempt context) {
            count("beforeAttempt");
            check("beforeAttempt", context, context.execution().get(callKey));
            context.attempt().put(attemptKey, context.execution().get(callKey));
        }

        @Override
        public SdkHttpRequest modifyBeforeSigning(HookContext.ModifyBeforeSigning context) {
            count("modifyBeforeSigning");
            check("modifyBeforeSigning", context, context.attempt().get(attemptKey));
            return context.httpRequest().toBuilder().putHeader(CALL_HEADER, context.execution().get(callKey)).build();
        }

        @Override
        public void beforeTransmission(HookContext.BeforeTransmission context) {
            count("beforeTransmission");
            check("beforeTransmission", context, context.attempt().get(attemptKey));
            insideTransmission.run();
        }

        @Override
        public void afterTransmission(HookContext.AfterTransmission context) {
            count("afterTransmission");
            check("afterTransmission", context, context.attempt().get(attemptKey));
        }

        @Override
        public void beforeDeserialization(HookContext.BeforeDeserialization context) {
            count("beforeDeserialization");
            check("beforeDeserialization", context, context.execution().get(callKey));
        }

        @Override
        public void afterAttempt(HookContext.AfterAttempt context) {
            count("afterAttempt");
            check("afterAttempt", context, context.attempt().get(attemptKey));
        }

        @Override
        public SerializableStruct modifyResponse(HookContext.ModifyResponse context) {
            count("modifyResponse");
            String echo = ((GetItemResponse) context.response()).item().get("echo").s();
            if (!echo.equals("t/" + context.execution().get(callKey))) {
                mismatches.add("modifyResponse: call " + context.execution().get(callKey) + " got " + echo);
            }
            return context.response();
        }

        @Override
        public void afterExecution(HookContext.AfterExecution context) {
            count("afterExecution");
            check("afterExecution", context, context.execution().get(callKey));
            if (context.failure().isPresent()) {
                mismatches.add("afterExecution: call failed " + context.failure().get());
            }
        }

        private void count(String hook) {
            hooks.computeIfAbsent(hook, h -> new LongAdder()).increment();
        }

        private void check(String hook, HookContext context, String stored) {
            String own = key(context.request());
            if (!own.equals(stored)) {
                mismatches.add(hook + ": request " + own + " saw state " + stored);
            }
        }

        private String key(SerializableStruct request) {
            return ((GetItemRequest) request).key().get("id").s();
        }

        void assertEveryHookRan(int times) {
            for (String hook : HOOKS) {
                LongAdder count = hooks.get(hook);
                assertEquals(times, count == null ? 0 : count.sum(), hook + " ran once per call");
            }
        }
    }
}
