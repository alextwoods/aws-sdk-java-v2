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

package software.amazon.awssdk.dynamicclient;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static software.amazon.awssdk.dynamicclient.CallTestClients.map;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import software.amazon.awssdk.dynamicclient.schema.StructDocument;
import software.amazon.awssdk.schema.Schema;
import software.amazon.awssdk.schema.ShapeBuilder;
import software.amazon.awssdk.schema.ShapeId;
import software.amazon.awssdk.schema.serde.ErrorRegistry;
import software.amazon.awssdk.schema.serde.TypeRegistry;

/**
 * {@link DynamicClient#getOperation(String)} and the {@link DynamicOperation}s it creates (Requirements 28.2, 28.9,
 * 30.1 and 30.6).
 */
class DynamicOperationTest {
    private DynamicClient client;

    @BeforeEach
    void createClient() {
        client = CallTestClients.client(DynamicTestModels.json10(), new RecordingHttpClients.Sync(),
                                        new RecordingHttpClients.Async());
    }

    @AfterEach
    void closeClient() {
        client.close();
    }

    @Test
    void getOperation_sameName_returnsTheSameInstance() {
        DynamicOperation first = client.getOperation("PutItem");

        assertThat(client.getOperation("PutItem")).isSameAs(first);
        assertThat(client.getOperation("Ping")).isNotSameAs(first);
    }

    @Test
    @Timeout(30)
    void getOperation_concurrentFirstCalls_returnTheSameInstance() throws Exception {
        ExecutorService pool = Executors.newFixedThreadPool(8);
        try {
            CountDownLatch start = new CountDownLatch(1);
            List<Callable<DynamicOperation>> tasks = new ArrayList<>();
            for (int i = 0; i < 8; i++) {
                tasks.add(() -> {
                    start.await();
                    return client.getOperation("GetThing");
                });
            }
            List<Future<DynamicOperation>> futures = new ArrayList<>();
            for (Callable<DynamicOperation> task : tasks) {
                futures.add(pool.submit(task));
            }
            start.countDown();
            Set<DynamicOperation> results = ConcurrentHashMap.newKeySet();
            for (Future<DynamicOperation> future : futures) {
                results.add(future.get(10, TimeUnit.SECONDS));
            }
            assertThat(results).containsExactly(client.getOperation("GetThing"));
        } finally {
            pool.shutdownNow();
            pool.awaitTermination(10, TimeUnit.SECONDS);
        }
    }

    @Test
    void getOperation_operation_exposesItsSchemasAuthAndService() {
        DynamicOperation operation = client.getOperation("PutItem");

        assertThat(operation.name()).isEqualTo("PutItem");
        assertThat(operation.schema().id()).isEqualTo(ShapeId.from("test.json10#PutItem"));
        assertThat(operation.inputSchema().id()).isEqualTo(ShapeId.from("test.json10#PutItemInput"));
        assertThat(operation.outputSchema().id()).isEqualTo(ShapeId.from("test.json10#PutItemOutput"));
        assertThat(operation.errorSchemas()).extracting(Schema::id)
                                            .containsExactly(ShapeId.from("test.json10#ItemFault"),
                                                             ShapeId.from("test.json10#ServiceFault"));
        assertThat(operation.effectiveAuthSchemes()).containsExactly(ShapeId.from("aws.auth#sigv4"));
        assertThat(operation.service()).isSameAs(client.service());
        TypeRegistry errors = operation.errorRegistry();
        assertThat(errors).isInstanceOf(ErrorRegistry.class).isSameAs(client.getOperation("PutItem").errorRegistry());
        assertThat(errors.contains(ShapeId.from("test.json10#ItemFault"))).isTrue();
        assertThat(errors.contains(ShapeId.from("test.json10#ServiceFault"))).isTrue();
        assertThat(errors.contains(ShapeId.from("test.json10#PutItemInput"))).isFalse();
        assertThat(errors.getShapeClass(ShapeId.from("test.json10#ItemFault"))).isEqualTo(DocumentException.class);
        assertThat(errors.createBuilder(ShapeId.from("test.json10#ItemFault")))
            .isInstanceOf(DocumentException.Builder.class)
            .isNotSameAs(errors.createBuilder(ShapeId.from("test.json10#ItemFault")));
        assertThat(client.getOperation("Ping").errorRegistry().contains(ShapeId.from("test.json10#ItemFault"))).isFalse();
        assertThat(operation.inputSchema())
            .isSameAs(client.schemaConverter().getSchema(software.amazon.smithy.model.shapes.ShapeId
                                                             .from("test.json10#PutItemInput")));
        assertThat(operation).hasToString("DynamicOperation(schema=test.json10#PutItem)");
    }

    @Test
    void getOperation_operationOfAResource_isFound() {
        assertThat(client.getOperation("GetThing").schema().id()).isEqualTo(ShapeId.from("test.json10#GetThing"));
    }

    @Test
    void getOperation_operationWithoutInput_hasTheUnitSchemas() {
        DynamicOperation ping = client.getOperation("Ping");

        assertThat(ping.inputSchema().id()).isEqualTo(ShapeId.from("smithy.api#Unit"));
        assertThat(ping.outputSchema().id()).isEqualTo(ShapeId.from("smithy.api#Unit"));
    }

    @Test
    void getOperation_unknownName_failsNamingTheOperationAndTheService() {
        assertThatThrownBy(() -> client.getOperation("putItem"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("Operation putItem not found in service test.json10#JsonTen");
        assertThatThrownBy(() -> client.call("Nope", map()))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("Operation Nope not found in service test.json10#JsonTen");
    }

    @Test
    void inputBuilder_setMemberValue_buildsARequestWithTheInputDocument() {
        DynamicOperation operation = client.getOperation("PutItem");
        ShapeBuilder<DynamicRequest> builder = operation.inputBuilder();

        builder.setMemberValue(operation.inputSchema().member("name"), "widget");
        DynamicRequest request = builder.build();

        assertThat(builder.sdkSchema()).isSameAs(operation.inputSchema());
        assertThat(request.sdkSchema()).isSameAs(operation.inputSchema());
        assertThat(request.document().getMember("name").asString()).isEqualTo("widget");
    }

    @Test
    void outputBuilder_build_wrapsTheOutputDocument() {
        DynamicOperation operation = client.getOperation("PutItem");
        ShapeBuilder<DynamicResponse> builder = operation.outputBuilder();

        builder.setMemberValue(operation.outputSchema().member("id"), "i-1");
        DynamicResponse response = builder.build();

        assertThat(builder).isInstanceOf(DynamicResponse.Builder.class);
        assertThat(response.document()).isInstanceOf(StructDocument.class);
        assertThat(response.document().getDiscriminator()).isEqualTo(ShapeId.from("test.json10#PutItemOutput"));
        assertThat(response.document().getMember("id").asString()).isEqualTo("i-1");
    }
}
