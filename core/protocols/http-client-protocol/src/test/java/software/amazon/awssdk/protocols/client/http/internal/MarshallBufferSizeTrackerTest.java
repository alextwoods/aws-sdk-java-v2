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

package software.amazon.awssdk.protocols.client.http.internal;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.schema.ApiOperation;
import software.amazon.awssdk.schema.ApiService;
import software.amazon.awssdk.schema.Schema;
import software.amazon.awssdk.schema.SerializableStruct;
import software.amazon.awssdk.schema.ShapeId;
import software.amazon.awssdk.schema.SimpleApiOperation;

class MarshallBufferSizeTrackerTest {
    private static final Schema SERVICE_SCHEMA = Schema.createService(ShapeId.fromParts("smithy.example", "Service"));
    private static final ApiService SERVICE = () -> SERVICE_SCHEMA;
    private static final Schema STRUCT = Schema.structureBuilder(ShapeId.fromParts("smithy.example", "Struct")).build();

    @Test
    void unknownOperationUsesTheDefault() {
        assertThat(new MarshallBufferSizeTracker().initialCapacity(operation("A")))
            .isEqualTo(MarshallBufferSizeTracker.DEFAULT_CAPACITY);
    }

    @Test
    void hintFollowsTheLastRecordedSize() {
        MarshallBufferSizeTracker hints = new MarshallBufferSizeTracker();
        ApiOperation<?, ?> op = operation("A");

        hints.record(op, 8000);
        assertThat(hints.initialCapacity(op)).isEqualTo(9000);

        // A smaller body lowers the hint again, so one large request does not stick.
        hints.record(op, 400);
        assertThat(hints.initialCapacity(op)).isEqualTo(450);
    }

    @Test
    void hintIsClamped() {
        assertThat(MarshallBufferSizeTracker.hintFor(0)).isEqualTo(MarshallBufferSizeTracker.MIN_CAPACITY);
        assertThat(MarshallBufferSizeTracker.hintFor(-5)).isEqualTo(MarshallBufferSizeTracker.MIN_CAPACITY);
        assertThat(MarshallBufferSizeTracker.hintFor(Integer.MAX_VALUE)).isEqualTo(MarshallBufferSizeTracker.MAX_CAPACITY);
        assertThat(MarshallBufferSizeTracker.hintFor(100)).isEqualTo(112);
    }

    @Test
    void operationsAreTrackedByIdentity() {
        MarshallBufferSizeTracker hints = new MarshallBufferSizeTracker();
        ApiOperation<?, ?> first = operation("A");
        ApiOperation<?, ?> sameName = operation("A");

        hints.record(first, 2000);

        assertThat(hints.initialCapacity(first)).isEqualTo(2250);
        assertThat(hints.initialCapacity(sameName)).isEqualTo(MarshallBufferSizeTracker.DEFAULT_CAPACITY);
    }

    @Test
    void manyOperationsKeepTheirHintsAcrossTableGrowth() {
        MarshallBufferSizeTracker hints = new MarshallBufferSizeTracker();
        List<ApiOperation<?, ?>> operations = new ArrayList<>();
        for (int i = 0; i < 200; i++) {
            ApiOperation<?, ?> op = operation("Op" + i);
            operations.add(op);
            hints.record(op, 1000 + i * 8);
        }
        for (int i = 0; i < operations.size(); i++) {
            assertThat(hints.initialCapacity(operations.get(i))).isEqualTo(MarshallBufferSizeTracker.hintFor(1000 + i * 8));
        }
    }

    @Test
    void tableStopsGrowingAtTheOperationLimit() {
        MarshallBufferSizeTracker hints = new MarshallBufferSizeTracker();
        for (int i = 0; i < MarshallBufferSizeTracker.MAX_OPERATIONS; i++) {
            hints.record(operation("Op" + i), 1000);
        }
        ApiOperation<?, ?> extra = operation("Extra");
        hints.record(extra, 5000);
        assertThat(hints.initialCapacity(extra)).isEqualTo(MarshallBufferSizeTracker.DEFAULT_CAPACITY);
    }

    @Test
    void concurrentRecordsOfNewOperationsAreAllKept() throws Exception {
        MarshallBufferSizeTracker hints = new MarshallBufferSizeTracker();
        int threads = 8;
        int perThread = 50;
        List<List<ApiOperation<?, ?>>> operations = new ArrayList<>();
        for (int t = 0; t < threads; t++) {
            List<ApiOperation<?, ?>> mine = new ArrayList<>();
            for (int i = 0; i < perThread; i++) {
                mine.add(operation("T" + t + "Op" + i));
            }
            operations.add(mine);
        }
        ExecutorService executor = Executors.newFixedThreadPool(threads);
        try {
            CountDownLatch start = new CountDownLatch(1);
            List<Future<?>> futures = new ArrayList<>();
            for (List<ApiOperation<?, ?>> mine : operations) {
                futures.add(executor.submit(() -> {
                    start.await();
                    for (ApiOperation<?, ?> op : mine) {
                        hints.record(op, 4000);
                    }
                    return null;
                }));
            }
            start.countDown();
            for (Future<?> future : futures) {
                future.get(30, TimeUnit.SECONDS);
            }
        } finally {
            executor.shutdownNow();
        }
        // Adding an operation is a compare-and-set of the whole table, so no new operation is lost.
        for (List<ApiOperation<?, ?>> mine : operations) {
            for (ApiOperation<?, ?> op : mine) {
                assertThat(hints.initialCapacity(op)).isEqualTo(4500);
            }
        }
    }

    private static ApiOperation<SerializableStruct, SerializableStruct> operation(String name) {
        return SimpleApiOperation.<SerializableStruct, SerializableStruct>builder()
                                 .schema(Schema.createOperation(ShapeId.fromParts("smithy.example", name)))
                                 .inputSchema(() -> STRUCT)
                                 .outputSchema(() -> STRUCT)
                                 .service(SERVICE)
                                 .build();
    }
}
