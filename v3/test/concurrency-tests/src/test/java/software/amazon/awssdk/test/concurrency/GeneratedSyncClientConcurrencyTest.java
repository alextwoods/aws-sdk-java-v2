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
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.time.Duration;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.GetItemResponse;
import software.amazon.awssdk.test.concurrency.support.Clients;
import software.amazon.awssdk.test.concurrency.support.FakeDynamoDb;

/**
 * Concurrency suite of one shared generated sync client (Requirements 7.12, 19.9, 19.10): {@value #SIMULTANEOUS}
 * simultaneous callers, {@value #TOTAL} total calls in {@value #TOTAL}/{@value #SIMULTANEOUS} rounds, a bounded
 * {@value #TIMEOUT_SECONDS}-second run. An {@link OverlapBarrier} of {@value #SIMULTANEOUS} parties inside the
 * transport's {@code execute} holds every exchange until all simultaneous callers are inside the client's pipeline at
 * the same time, so serialization, endpoint resolution, signing, transmission and deserialization of the calls overlap.
 * Each call carries a unique key, payload and table; every response must be the answer to its own request.
 */
class GeneratedSyncClientConcurrencyTest {
    static final int SIMULTANEOUS = 8;
    static final int TOTAL = 64;
    static final int TIMEOUT_SECONDS = 60;

    private final FakeDynamoDb http = FakeDynamoDb.sync();

    @AfterEach
    void shutdown() {
        http.shutdown();
    }

    @Test
    @Timeout(value = TIMEOUT_SECONDS + 10, unit = TimeUnit.SECONDS)
    void sharedSyncClient_overlappingGetItemCalls_eachReceivesItsOwnResponse() {
        ConcurrencyPlan plan = ConcurrencyPlan.of("sync-client-get", SIMULTANEOUS, TOTAL,
                                                  Duration.ofSeconds(TIMEOUT_SECONDS));
        OverlapBarrier overlap = plan.overlapBarrier();
        http.onRequest(r -> overlap.arrive());

        try (DynamoDbClient client = Clients.sync(http).build()) {
            List<String> echoes = ConcurrentRun.run(plan, call -> {
                GetItemResponse response = client.getItem(r -> r.tableName(table(call))
                                                               .key(Map.of("id", s(id(call)))));
                return response.item().get("echo").s();
            }, overlap::breakBarrier);

            for (int i = 0; i < TOTAL; i++) {
                ConcurrentRun.Call call = callOf(plan, i);
                assertEquals(table(call) + "/" + id(call), echoes.get(i), "the response of " + call);
            }
        }
        assertEquals(plan.rounds(), overlap.trips(), "every round overlapped all simultaneous calls");
        assertEquals(SIMULTANEOUS, overlap.maxInside());
        assertEquals(TOTAL, http.count("GetItem"));
        assertEquals(0, http.openExchanges(), "every exchange was closed");
        assertDistinctSignatures(TOTAL);
    }

    @Test
    @Timeout(value = TIMEOUT_SECONDS + 10, unit = TimeUnit.SECONDS)
    void sharedSyncClient_overlappingPutThenGetCalls_storeAndReadEveryItemExactlyOnce() {
        ConcurrencyPlan plan = ConcurrencyPlan.of("sync-client-put-get", SIMULTANEOUS, TOTAL,
                                                  Duration.ofSeconds(TIMEOUT_SECONDS));
        // Each call is a PutItem then a GetItem: the barrier trips once for the puts and once for the gets of a round.
        OverlapBarrier overlap = plan.overlapBarrier();
        http.onRequest(r -> overlap.arrive());

        try (DynamoDbClient client = Clients.sync(http).build()) {
            List<String> payloads = ConcurrentRun.run(plan, call -> {
                client.putItem(r -> r.tableName("shared").item(Map.of("id", s(id(call)), "payload", s(payload(call)))));
                return client.getItem(r -> r.tableName("shared").key(Map.of("id", s(id(call))))).item()
                             .get("payload").s();
            }, overlap::breakBarrier);

            for (int i = 0; i < TOTAL; i++) {
                assertEquals(payload(callOf(plan, i)), payloads.get(i));
            }
        }
        assertEquals(2 * plan.rounds(), overlap.trips());
        assertEquals(TOTAL, http.count("PutItem"));
        assertEquals(TOTAL, http.count("GetItem"));
        assertEquals(TOTAL, http.storedItems(), "no write was lost or duplicated under another key");
        for (int i = 0; i < TOTAL; i++) {
            ConcurrentRun.Call call = callOf(plan, i);
            assertNotNull(http.item("shared", id(call)), "stored " + call);
        }
        // Every serialized PutItem carries its own key with its own payload (no cross-call serializer state).
        http.received().stream().filter(r -> r.operation().equals("PutItem")).forEach(r -> {
            String index = r.id().substring("call-".length());
            assertEquals(true, r.body().contains("\"payload-" + index + "\""), r.body());
        });
        assertEquals(0, http.openExchanges());
        assertDistinctSignatures(2 * TOTAL);
    }

    /** SigV4 signatures cover the body, so distinct requests must carry distinct signatures. */
    private void assertDistinctSignatures(int expected) {
        Set<String> signatures = new HashSet<>();
        http.received().forEach(r -> signatures.add(r.header("Authorization")));
        assertEquals(expected, signatures.size(), "one distinct signature per request");
    }

    static ConcurrentRun.Call callOf(ConcurrencyPlan plan, int index) {
        return new ConcurrentRun.Call(index % plan.simultaneous(), index / plan.simultaneous(), index);
    }

    static String table(ConcurrentRun.Call call) {
        return "table-lane-" + call.lane();
    }

    static String id(ConcurrentRun.Call call) {
        return "call-" + call.index();
    }

    static String payload(ConcurrentRun.Call call) {
        return "payload-" + call.index();
    }

    static AttributeValue s(String value) {
        return AttributeValue.builder().s(value).build();
    }
}
