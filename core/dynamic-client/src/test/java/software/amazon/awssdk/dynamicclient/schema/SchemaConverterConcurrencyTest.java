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

package software.amazon.awssdk.dynamicclient.schema;

import static org.assertj.core.api.Assertions.assertThat;
import static software.amazon.awssdk.dynamicclient.schema.TestModels.rec;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import software.amazon.awssdk.schema.Schema;
import software.amazon.smithy.model.shapes.ShapeId;

/**
 * Parallel requests to one converter get the same schema instances (Requirement 25.9).
 */
@Timeout(60)
class SchemaConverterConcurrencyTest {
    private static final int THREADS = 8;
    private static final int ROUNDS = 25;
    private static final ShapeId[] SHAPES = {
        rec("Root"), rec("TreeNode"), rec("TreeNodeList"), rec("MutualB"), rec("MutualA"), rec("Expr"), rec("MapNode"),
        rec("SelfRef")
    };

    private ExecutorService executor;

    @BeforeEach
    void startExecutor() {
        executor = Executors.newFixedThreadPool(THREADS);
    }

    @AfterEach
    void stopExecutor() throws InterruptedException {
        executor.shutdownNow();
        assertThat(executor.awaitTermination(10, TimeUnit.SECONDS)).isTrue();
    }

    @Test
    void getSchema_parallelRequests_returnTheSameInstanceToEveryThread() throws Exception {
        for (int round = 0; round < ROUNDS; round++) {
            SchemaConverter converter = new SchemaConverter(TestModels.recursive());
            CountDownLatch start = new CountDownLatch(1);
            List<Future<Schema[]>> futures = new ArrayList<>();
            for (int t = 0; t < THREADS; t++) {
                int offset = t;
                futures.add(executor.submit(() -> {
                    start.await();
                    // Each thread asks for the shapes in a different order, so conversions start from different
                    // points of the recursive groups.
                    Schema[] result = new Schema[SHAPES.length];
                    for (int i = 0; i < SHAPES.length; i++) {
                        int index = (i + offset) % SHAPES.length;
                        result[index] = converter.getSchema(SHAPES[index]);
                    }
                    return result;
                }));
            }
            start.countDown();

            List<Schema[]> results = new ArrayList<>();
            for (Future<Schema[]> future : futures) {
                results.add(future.get(10, TimeUnit.SECONDS));
            }
            for (int i = 0; i < SHAPES.length; i++) {
                Schema expected = converter.getSchema(SHAPES[i]);
                for (Schema[] result : results) {
                    assertThat(result[i]).as("round %s, %s", round, SHAPES[i]).isSameAs(expected);
                }
            }
            Schema tree = converter.getSchema(rec("TreeNode"));
            assertThat(tree.member("children").memberTarget().listMember().memberTarget()).isSameAs(tree);
            Schema a = converter.getSchema(rec("MutualA"));
            assertThat(a.member("b").memberTarget().member("a").memberTarget()).isSameAs(a);
        }
    }
}
