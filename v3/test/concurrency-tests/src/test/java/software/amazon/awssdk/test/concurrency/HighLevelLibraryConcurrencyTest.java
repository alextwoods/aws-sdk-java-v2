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
import static org.junit.jupiter.api.Assertions.assertTrue;
import static software.amazon.awssdk.test.concurrency.GeneratedSyncClientConcurrencyTest.id;
import static software.amazon.awssdk.test.concurrency.GeneratedSyncClientConcurrencyTest.payload;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbAsyncTable;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedAsyncClient;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.Key;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.eventnotifications.s3.model.S3EventNotification;
import software.amazon.awssdk.eventnotifications.s3.model.S3EventNotificationRecord;
import software.amazon.awssdk.policybuilder.iam.IamEffect;
import software.amazon.awssdk.policybuilder.iam.IamPolicy;
import software.amazon.awssdk.policybuilder.iam.IamPolicyReader;
import software.amazon.awssdk.policybuilder.iam.IamPolicyWriter;
import software.amazon.awssdk.services.dynamodb.DynamoDbAsyncClient;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.test.concurrency.support.Clients;
import software.amazon.awssdk.test.concurrency.support.ConcurrencyItem;
import software.amazon.awssdk.test.concurrency.support.FakeDynamoDb;

/**
 * Concurrency suite of the high-level libraries over shared instances (Requirements 16.x via 19.9, 19.10): every test
 * runs {@value #SIMULTANEOUS} simultaneous callers, {@value #TOTAL} total operations and a bounded
 * {@value #TIMEOUT_SECONDS}-second run.
 *
 * <ul>
 *   <li>DynamoDB Enhanced: one enhanced client, table resource and bean schema shared by every caller; each operation
 *       is a {@code putItem} then a {@code getItem} of its own key. The transport's overlap barrier (sync) or gate
 *       (async) holds the requests until all callers are inside the library and the generated client together.</li>
 *   <li>IAM policy builder: one {@link IamPolicyReader} and one {@link IamPolicyWriter} shared by every caller; an
 *       overlap barrier in the first read of the policy stream holds every caller inside {@code read}.</li>
 *   <li>S3 event notifications: the shared JSON reader of {@link S3EventNotification}; an overlap barrier in the
 *       first read of the notification stream holds every caller inside {@code fromJson}.</li>
 * </ul>
 * The S3 Transfer Manager's concurrency suite runs in its own module (S3TransferManagerConcurrencyTest), with the fake
 * S3 there; the SNS message manager's runs with its signing fixtures (SnsMessageManagerConcurrencyTest).
 */
class HighLevelLibraryConcurrencyTest {
    static final int SIMULTANEOUS = 8;
    static final int TOTAL = 64;
    static final int TIMEOUT_SECONDS = 60;
    static final TableSchema<ConcurrencyItem> SCHEMA = TableSchema.fromBean(ConcurrencyItem.class);

    private final ExecutorService completion = Clients.completionExecutor("hll-completion", 3);
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
    void dynamoDbEnhancedSyncTable_overlappingPutThenGet_storesAndReadsEveryItem() {
        ConcurrencyPlan plan = plan("enhanced-sync-table");
        OverlapBarrier overlap = plan.overlapBarrier();
        http = FakeDynamoDb.sync().onRequest(r -> overlap.arrive());

        try (DynamoDbClient client = Clients.sync(http).build()) {
            DynamoDbTable<ConcurrencyItem> table = DynamoDbEnhancedClient.builder().dynamoDbClient(client).build()
                                                                         .table("items", SCHEMA);
            List<String> payloads = ConcurrentRun.run(plan, call -> {
                table.putItem(ConcurrencyItem.of(id(call), payload(call)));
                return table.getItem(Key.builder().partitionValue(id(call)).build()).getPayload();
            }, overlap::breakBarrier);
            assertPayloads(plan, payloads);
        }
        assertEquals(2 * plan.rounds(), overlap.trips(), "the puts, then the gets, of each round overlapped");
        assertStoredOnce();
    }

    @Test
    @Timeout(value = TIMEOUT_SECONDS + 10, unit = TimeUnit.SECONDS)
    void dynamoDbEnhancedAsyncTable_overlappingPutThenGet_storesAndReadsEveryItem() {
        ConcurrencyPlan plan = plan("enhanced-async-table");
        AsyncOverlapGate gate = plan.overlapGate();
        http = FakeDynamoDb.async().gate(r -> gate.arrive());

        try (DynamoDbAsyncClient client = Clients.async(http).asyncCompletionExecutor(completion).build()) {
            DynamoDbAsyncTable<ConcurrencyItem> table =
                DynamoDbEnhancedAsyncClient.builder().dynamoDbClient(client).build().table("items", SCHEMA);
            List<String> payloads = ConcurrentRun.runAsync(plan, call -> table.putItem(
                    ConcurrencyItem.of(id(call), payload(call)))
                .thenCompose(put -> table.getItem(Key.builder().partitionValue(id(call)).build()))
                .thenApply(ConcurrencyItem::getPayload), gate::failPending);
            assertPayloads(plan, payloads);
        }
        assertEquals(2 * plan.rounds(), gate.trips());
        assertStoredOnce();
    }

    @Test
    @Timeout(value = TIMEOUT_SECONDS + 10, unit = TimeUnit.SECONDS)
    void iamPolicyReaderAndWriter_sharedInstances_roundTripEveryCallersPolicy() {
        ConcurrencyPlan plan = plan("iam-policy-reader-writer");
        OverlapBarrier overlap = plan.overlapBarrier();
        IamPolicyReader reader = IamPolicyReader.create();
        IamPolicyWriter writer = IamPolicyWriter.create();

        List<IamPolicy> written = ConcurrentRun.run(plan, call -> {
            IamPolicy policy = policy(call);
            String json = writer.writeToString(policy);
            IamPolicy read = reader.read(new OverlapStream(json, overlap));
            assertEquals(policy, read, "the round trip of " + call);
            assertEquals(json, writer.writeToString(read));
            return read;
        }, overlap::breakBarrier);

        for (int i = 0; i < TOTAL; i++) {
            assertEquals(policy(GeneratedSyncClientConcurrencyTest.callOf(plan, i)), written.get(i));
        }
        assertEquals(plan.rounds(), overlap.trips(), "every round's readers were inside read together");
    }

    @Test
    @Timeout(value = TIMEOUT_SECONDS + 10, unit = TimeUnit.SECONDS)
    void s3EventNotificationReader_overlappingParses_eachReturnsItsOwnRecord() {
        ConcurrencyPlan plan = plan("s3-event-notifications");
        OverlapBarrier overlap = plan.overlapBarrier();

        List<S3EventNotification> parsed = ConcurrentRun.run(plan, call -> {
            S3EventNotification notification = S3EventNotification.fromJson(new OverlapStream(event(call), overlap));
            assertEquals(notification, S3EventNotification.fromJson(notification.toJson()), "the round trip");
            return notification;
        }, overlap::breakBarrier);

        for (int i = 0; i < TOTAL; i++) {
            S3EventNotificationRecord record = parsed.get(i).getRecords().get(0);
            assertEquals("bucket-" + i % SIMULTANEOUS, record.getS3().getBucket().getName());
            assertEquals("key-" + i, record.getS3().getObject().getKey());
            assertEquals(Long.valueOf(i), record.getS3().getObject().getSizeAsLong());
        }
        assertEquals(plan.rounds(), overlap.trips());
    }

    private void assertPayloads(ConcurrencyPlan plan, List<String> payloads) {
        for (int i = 0; i < TOTAL; i++) {
            assertEquals(payload(GeneratedSyncClientConcurrencyTest.callOf(plan, i)), payloads.get(i));
        }
    }

    private void assertStoredOnce() {
        assertEquals(TOTAL, http.count("PutItem"));
        assertEquals(TOTAL, http.count("GetItem"));
        assertEquals(TOTAL, http.storedItems());
        for (int i = 0; i < TOTAL; i++) {
            String item = http.item("items", "call-" + i);
            assertTrue(item.contains("\"payload-" + i + "\""), item);
        }
        assertEquals(0, http.openExchanges());
    }

    private static IamPolicy policy(ConcurrentRun.Call call) {
        return IamPolicy.builder()
                        .id("policy-" + call.index())
                        .addStatement(s -> s.sid("lane" + call.lane() + "round" + call.round())
                                            .effect(IamEffect.ALLOW)
                                            .addAction("dynamodb:GetItem")
                                            .addResource("arn:aws:dynamodb:us-east-1:123456789012:table/t-"
                                                         + call.index()))
                        .build();
    }

    private static String event(ConcurrentRun.Call call) {
        return "{\"Records\":[{\"eventVersion\":\"2.1\",\"eventSource\":\"aws:s3\",\"awsRegion\":\"us-east-1\","
               + "\"eventTime\":\"2024-01-01T00:00:00.000Z\",\"eventName\":\"ObjectCreated:Put\","
               + "\"s3\":{\"s3SchemaVersion\":\"1.0\",\"bucket\":{\"name\":\"bucket-" + call.lane() + "\"},"
               + "\"object\":{\"key\":\"key-" + call.index() + "\",\"size\":" + call.index() + "}}}]}";
    }

    private static ConcurrencyPlan plan(String component) {
        return ConcurrencyPlan.of(component, SIMULTANEOUS, TOTAL, Duration.ofSeconds(TIMEOUT_SECONDS));
    }

    /** A UTF-8 stream whose first read arrives at the overlap barrier, inside the library that reads it. */
    private static final class OverlapStream extends InputStream {
        private final ByteArrayInputStream delegate;
        private final OverlapBarrier overlap;
        private boolean arrived;

        OverlapStream(String text, OverlapBarrier overlap) {
            this.delegate = new ByteArrayInputStream(text.getBytes(StandardCharsets.UTF_8));
            this.overlap = overlap;
        }

        @Override
        public int read() throws IOException {
            arrive();
            return delegate.read();
        }

        @Override
        public int read(byte[] b, int off, int len) throws IOException {
            arrive();
            return delegate.read(b, off, len);
        }

        private void arrive() {
            if (!arrived) {
                arrived = true;
                overlap.arrive();
            }
        }
    }
}
