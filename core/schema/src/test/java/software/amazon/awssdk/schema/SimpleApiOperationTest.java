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

package software.amazon.awssdk.schema;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.schema.serde.ErrorRegistry;
import software.amazon.awssdk.schema.serde.TypeRegistry;
import software.amazon.awssdk.schema.traits.ErrorTrait;
import software.amazon.awssdk.schema.traits.IdempotencyTokenTrait;
import software.amazon.awssdk.schema.traits.StreamingTrait;

class SimpleApiOperationTest {
    private static final String NS = "com.example";
    private static final ShapeId SIGV4 = ShapeId.from("aws.auth#sigv4");
    private static final ShapeId BEARER = ShapeId.from("smithy.api#httpBearerAuth");
    private static final Schema SERVICE_SCHEMA = Schema.createService(ShapeId.fromParts(NS, "ExampleService"));
    private static final ApiService SERVICE = () -> SERVICE_SCHEMA;
    private static final Schema OPERATION = Schema.createOperation(ShapeId.fromParts(NS, "PutThing"));

    // Error shapes, given to the operation as IDs and resolved through the registry.
    private static final ShapeId NOT_FOUND_ID = ShapeId.fromParts(NS, "NotFound");
    private static final Schema NOT_FOUND = errorSchema(NOT_FOUND_ID);
    private static final ShapeId THROTTLED_ID = ShapeId.fromParts(NS, "Throttled");
    private static final Schema THROTTLED = errorSchema(THROTTLED_ID);
    private static final ErrorRegistry ERRORS = ErrorRegistry.builder()
                                                             .putLazyType(THROTTLED_ID, () -> THROTTLED)
                                                             .putLazyType(NOT_FOUND_ID, () -> NOT_FOUND)
                                                             .build();

    // An input with an idempotency token and a streaming member, and an output with a streaming member.
    private static final Schema STREAMING_BLOB = Schema.createBlob(ShapeId.fromParts(NS, "Body"),
                                                                   StreamingTrait.INSTANCE);
    private static final Schema SPECIAL_INPUT = Schema.structureBuilder(ShapeId.fromParts(NS, "PutThingInput"))
                                                      .putMember("name", PreludeSchemas.STRING)
                                                      .putMember("clientToken", PreludeSchemas.STRING,
                                                                 IdempotencyTokenTrait.INSTANCE)
                                                      .putMember("body", STREAMING_BLOB)
                                                      .build();
    private static final Schema SPECIAL_OUTPUT = Schema.structureBuilder(ShapeId.fromParts(NS, "PutThingOutput"))
                                                       .putMember("body", STREAMING_BLOB)
                                                       .build();

    @Test
    void accessors_returnConfiguredValues() {
        SimpleApiOperation<TestStruct, TestStruct> operation = fullOperation();

        assertThat(operation.schema()).isSameAs(OPERATION);
        assertThat(operation.inputSchema()).isSameAs(TestStruct.SCHEMA);
        assertThat(operation.outputSchema()).isSameAs(TestStruct.SCHEMA);
        assertThat(operation.errorRegistry()).isSameAs(ERRORS);
        assertThat(operation.errorSchemas()).containsExactly(NOT_FOUND, THROTTLED);
        assertThat(operation.effectiveAuthSchemes()).containsExactly(SIGV4, BEARER);
        assertThat(operation.service()).isSameAs(SERVICE);
        assertThat(operation.service().name()).isEqualTo("ExampleService");
        assertThat(operation.name()).isEqualTo("PutThing");
        assertThat(operation.boundResource()).isNull();
        assertThat(operation.inputEventBuilderSupplier()).isNull();
        assertThat(operation.outputEventBuilderSupplier()).isNull();
        assertThat(operation.toString()).isEqualTo("SimpleApiOperation(schema=com.example#PutThing)");
    }

    @Test
    void inputAndOutputBuilders_comeFromSchemas() {
        SimpleApiOperation<TestStruct, TestStruct> operation = fullOperation();

        ShapeBuilder<TestStruct> input = operation.inputBuilder();
        assertThat(input).isInstanceOf(TestStruct.Builder.class).isNotSameAs(operation.inputBuilder());
        assertThat(((TestStruct.Builder) input).name("a").build().name()).isEqualTo("a");
        assertThat(operation.outputBuilder()).isInstanceOf(TestStruct.Builder.class);
    }

    @Test
    void builderOfSchemaWithoutBuilderSupplier_throwsNamingSchemas() {
        SimpleApiOperation<TestStruct, TestStruct> operation = baseBuilder().inputSchema(() -> SPECIAL_INPUT).build();

        assertThatThrownBy(operation::inputBuilder)
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining(SPECIAL_INPUT.id().toString())
            .hasMessageContaining(OPERATION.id().toString());
    }

    @Test
    void lists_areUnmodifiableAndCopied() {
        ShapeId[] auth = {SIGV4};
        SimpleApiOperation<TestStruct, TestStruct> operation = baseBuilder().effectiveAuthSchemes(auth)
                                                                            .errorIds(NOT_FOUND_ID)
                                                                            .errorRegistry(ERRORS)
                                                                            .build();
        auth[0] = BEARER;

        assertThat(operation.effectiveAuthSchemes()).containsExactly(SIGV4);
        assertThatThrownBy(() -> operation.effectiveAuthSchemes().add(BEARER))
            .isInstanceOf(UnsupportedOperationException.class);
        assertThatThrownBy(() -> operation.errorSchemas().clear()).isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void defaults_noErrorsEmptyRegistryNoAuth() {
        SimpleApiOperation<TestStruct, TestStruct> operation = baseBuilder().build();

        assertThat(operation.errorSchemas()).isEmpty();
        assertThat(operation.errorRegistry()).isSameAs(TypeRegistry.empty());
        assertThat(operation.effectiveAuthSchemes()).isEmpty();
    }

    @Test
    void schemaSuppliers_areInvokedOnFirstUseOnly() {
        AtomicInteger inputCalls = new AtomicInteger();
        AtomicInteger outputCalls = new AtomicInteger();
        AtomicInteger errorCalls = new AtomicInteger();
        ErrorRegistry registry = ErrorRegistry.builder()
                                              .putLazyType(NOT_FOUND_ID, counting(errorCalls, NOT_FOUND))
                                              .build();
        SimpleApiOperation<TestStruct, TestStruct> operation =
            SimpleApiOperation.<TestStruct, TestStruct>builder()
                              .schema(OPERATION)
                              .inputSchema(counting(inputCalls, TestStruct.SCHEMA))
                              .outputSchema(counting(outputCalls, TestStruct.SCHEMA))
                              .errorIds(NOT_FOUND_ID)
                              .errorRegistry(registry)
                              .service(SERVICE)
                              .build();

        assertThat(inputCalls).hasValue(0);
        assertThat(outputCalls).hasValue(0);
        assertThat(errorCalls).hasValue(0);

        for (int i = 0; i < 3; i++) {
            operation.inputSchema();
            operation.inputBuilder();
            operation.outputSchema();
            operation.outputBuilder();
            operation.errorSchemas();
        }

        assertThat(inputCalls).hasValue(1);
        assertThat(outputCalls).hasValue(1);
        assertThat(errorCalls).hasValue(1);
        assertThat(operation.errorSchemas()).isSameAs(operation.errorSchemas());
    }

    @Test
    void concurrentFirstCalls_publishOneSchema() throws Exception {
        int threads = 8;
        // Each call returns a distinct schema instance, so a second publication would show.
        Supplier<Schema> distinct = () -> Schema.structureBuilder(TestStruct.SCHEMA.id()).build();
        SimpleApiOperation<TestStruct, TestStruct> operation = baseBuilder().inputSchema(distinct).build();
        ExecutorService executor = Executors.newFixedThreadPool(threads);
        try {
            CountDownLatch start = new CountDownLatch(1);
            List<Future<Schema>> results = new ArrayList<>();
            for (int i = 0; i < threads; i++) {
                results.add(executor.submit(() -> {
                    start.await();
                    return operation.inputSchema();
                }));
            }
            start.countDown();
            Schema first = results.get(0).get(10, TimeUnit.SECONDS);
            for (Future<Schema> result : results) {
                assertThat(result.get(10, TimeUnit.SECONDS)).isSameAs(first);
            }
            assertThat(operation.inputSchema()).isSameAs(first);
        } finally {
            executor.shutdownNow();
        }
    }

    @Test
    void schemaSupplierReturningNull_throwsAndIsRetried() {
        AtomicInteger calls = new AtomicInteger();
        SimpleApiOperation<TestStruct, TestStruct> operation = baseBuilder().outputSchema(counting(calls, null))
                                                                            .build();

        assertThatThrownBy(operation::outputSchema)
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("output")
            .hasMessageContaining(OPERATION.id().toString());
        assertThatThrownBy(operation::outputBuilder).isInstanceOf(IllegalStateException.class);
        assertThat(calls).hasValue(2);
    }

    @Test
    void errorSchemas_withPlainTypeRegistry_useBuilderSchemas() {
        TypeRegistry registry = TypeRegistry.builder()
                                            .putType(TestStruct.SCHEMA.id(), TestStruct.class, TestStruct::builder)
                                            .build();
        SimpleApiOperation<TestStruct, TestStruct> operation = baseBuilder().errorIds(TestStruct.SCHEMA.id())
                                                                            .errorRegistry(registry)
                                                                            .build();

        assertThat(operation.errorSchemas()).containsExactly(TestStruct.SCHEMA);
    }

    @Test
    void errorIdMissingFromRegistry_throwsNamingOperationAndError() {
        ShapeId missing = ShapeId.fromParts(NS, "Missing");
        SimpleApiOperation<TestStruct, TestStruct> operation = baseBuilder().errorIds(NOT_FOUND_ID, missing)
                                                                            .errorRegistry(ERRORS)
                                                                            .build();

        assertThatThrownBy(operation::errorSchemas)
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining(OPERATION.id().toString())
            .hasMessageContaining(missing.toString());
    }

    @Test
    void defaultMemberLookups_findSpecialMembers() {
        SimpleApiOperation<TestStruct, TestStruct> special = baseBuilder().inputSchema(() -> SPECIAL_INPUT)
                                                                          .outputSchema(() -> SPECIAL_OUTPUT)
                                                                          .build();
        SimpleApiOperation<TestStruct, TestStruct> plain = baseBuilder().build();

        assertThat(special.idempotencyTokenMember()).isSameAs(SPECIAL_INPUT.member("clientToken"));
        assertThat(special.inputStreamMember()).isSameAs(SPECIAL_INPUT.member("body"));
        assertThat(special.outputStreamMember()).isSameAs(SPECIAL_OUTPUT.member("body"));
        assertThat(plain.idempotencyTokenMember()).isNull();
        assertThat(plain.inputStreamMember()).isNull();
        assertThat(plain.outputStreamMember()).isNull();
    }

    @Test
    void build_validatesRequiredValues() {
        assertThatThrownBy(() -> baseBuilder().schema(null).build())
            .isInstanceOf(NullPointerException.class).hasMessageContaining("schema");
        assertThatThrownBy(() -> baseBuilder().inputSchema(null).build())
            .isInstanceOf(NullPointerException.class).hasMessageContaining("inputSchema");
        assertThatThrownBy(() -> baseBuilder().outputSchema(null).build())
            .isInstanceOf(NullPointerException.class).hasMessageContaining("outputSchema");
        assertThatThrownBy(() -> baseBuilder().service(null).build())
            .isInstanceOf(NullPointerException.class).hasMessageContaining("service");
        assertThatThrownBy(() -> baseBuilder().schema(SERVICE_SCHEMA).build())
            .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("operation");
        assertThatThrownBy(() -> baseBuilder().errorIds(NOT_FOUND_ID, null))
            .isInstanceOf(NullPointerException.class).hasMessageContaining("errorIds[1]");
        assertThatThrownBy(() -> baseBuilder().effectiveAuthSchemes((ShapeId[]) null))
            .isInstanceOf(NullPointerException.class).hasMessageContaining("effectiveAuthSchemes");
        assertThatThrownBy(() -> baseBuilder().errorRegistry(null))
            .isInstanceOf(NullPointerException.class).hasMessageContaining("errorRegistry");
    }

    private static SimpleApiOperation<TestStruct, TestStruct> fullOperation() {
        return baseBuilder().errorIds(NOT_FOUND_ID, THROTTLED_ID)
                            .errorRegistry(ERRORS)
                            .effectiveAuthSchemes(SIGV4, BEARER)
                            .build();
    }

    private static SimpleApiOperation.Builder<TestStruct, TestStruct> baseBuilder() {
        return SimpleApiOperation.<TestStruct, TestStruct>builder()
                                 .schema(OPERATION)
                                 .inputSchema(() -> TestStruct.SCHEMA)
                                 .outputSchema(() -> TestStruct.SCHEMA)
                                 .service(SERVICE);
    }

    private static Schema errorSchema(ShapeId id) {
        // Shaped like a generated exception: the registry needs a SerializableStruct shape class and a builder.
        return Schema.structureBuilder(id, new ErrorTrait("client"))
                     .shapeClass(TestStruct.class)
                     .builderSupplier(TestStruct::builder)
                     .build();
    }

    private static Supplier<Schema> counting(AtomicInteger calls, Schema schema) {
        return () -> {
            calls.incrementAndGet();
            return schema;
        };
    }
}
