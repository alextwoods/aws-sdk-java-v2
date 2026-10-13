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

package software.amazon.awssdk.schema.serde;

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
import software.amazon.awssdk.schema.PreludeSchemas;
import software.amazon.awssdk.schema.Schema;
import software.amazon.awssdk.schema.ShapeBuilder;
import software.amazon.awssdk.schema.ShapeId;
import software.amazon.awssdk.schema.serde.RegistryTestShapes.Bird;
import software.amazon.awssdk.schema.serde.RegistryTestShapes.NamedBuilder;
import software.amazon.awssdk.schema.serde.RegistryTestShapes.Person;

/**
 * Port of smithy-java {@code core.serde.TypeRegistryTest} (first four methods) plus SDK tests for lazy entries,
 * composition and the empty registry.
 */
class TypeRegistryTest {
    private static final ShapeId FOO = ShapeId.from("smithy.example#Foo");

    // Ported from smithy-java.

    @Test
    void deserializesFromRegistryToSpecificClass() {
        TypeRegistry registry = TypeRegistry.builder().putType(Person.ID, Person.class, Person::builder).build();

        assertThat(registry.createBuilder(FOO, Person.class)).isNull();
        assertThat(registry.createBuilder(Person.ID, Person.class)).isNotNull();
    }

    @Test
    void deserializesFromRegistryWithJustId() {
        TypeRegistry registry = TypeRegistry.builder().putType(Person.ID, Person.class, Person::builder).build();

        assertThat(registry.createBuilder(Person.ID)).isNotNull();
        assertThat(registry.createBuilder(FOO)).isNull();
    }

    @Test
    void throwsIfTypeIsIncompatible() {
        TypeRegistry registry = TypeRegistry.builder().putType(Person.ID, Person.class, Person::builder).build();

        assertThatThrownBy(() -> registry.createBuilder(Person.ID, Bird.class))
            .isInstanceOf(SerializationException.class)
            .hasMessageContaining(Person.ID.toString());
    }

    @Test
    void composesRegistries() {
        TypeRegistry a = TypeRegistry.builder().putType(Person.ID, Person.class, Person::builder).build();
        TypeRegistry b = TypeRegistry.builder().putType(Bird.ID, Bird.class, Bird::builder).build();

        TypeRegistry registry = TypeRegistry.compose(a, b);

        assertThat(registry.createBuilder(Person.ID)).isNotNull();
        assertThat(registry.createBuilder(FOO)).isNull();
        assertThat(registry.createBuilder(Bird.ID)).isNotNull();
    }

    // SDK tests.

    @Test
    void putType_resolvesClassAndNewBuilderPerCall() {
        TypeRegistry registry = TypeRegistry.builder().putType(Person.ID, Person.class, Person::builder).build();

        assertThat(registry.contains(Person.ID)).isTrue();
        assertThat(registry.getShapeClass(Person.ID)).isEqualTo(Person.class);
        ShapeBuilder<?> first = registry.createBuilder(Person.ID);
        assertThat(first).isNotSameAs(registry.createBuilder(Person.ID));
        assertThat(((NamedBuilder<?>) first).name("Phreddie").build()).isInstanceOf(Person.class);
    }

    @Test
    void unregisteredId_returnsNullAndFalse() {
        TypeRegistry registry = TypeRegistry.builder().putType(Person.ID, Person.class, Person::builder).build();

        assertThat(registry.contains(FOO)).isFalse();
        assertThat(registry.getShapeClass(FOO)).isNull();
        assertThat(registry.createBuilder(FOO)).isNull();
        assertThat(registry.createBuilder(null, Person.class)).isNull();
    }

    @Test
    void putLazyType_doesNotInvokeSupplierUntilResolved() {
        AtomicInteger calls = new AtomicInteger();
        TypeRegistry registry = TypeRegistry.builder().putLazyType(Person.ID, counting(calls, Person.SCHEMA)).build();

        assertThat(registry.contains(Person.ID)).isTrue();
        assertThat(calls).hasValue(0);

        assertThat(registry.getShapeClass(Person.ID)).isEqualTo(Person.class);
        assertThat(registry.createBuilder(Person.ID, Person.class).sdkSchema()).isSameAs(Person.SCHEMA);
        assertThat(registry.createBuilder(Person.ID)).isInstanceOf(NamedBuilder.class);
        assertThat(calls).hasValue(1);
    }

    @Test
    void putLazyType_concurrentFirstLookups_publishOneSchema() throws Exception {
        int threads = 8;
        // Each call returns a distinct schema instance with the registered ID, so a second publication would show.
        Supplier<Schema> distinctSchemas = () -> Schema.structureBuilder(Person.ID)
                                                       .putMember("name", PreludeSchemas.STRING)
                                                       .builderSupplier(Person::builder)
                                                       .shapeClass(Person.class)
                                                       .build();
        TypeRegistry registry = TypeRegistry.builder().putLazyType(Person.ID, distinctSchemas).build();
        ExecutorService executor = Executors.newFixedThreadPool(threads);
        try {
            CountDownLatch start = new CountDownLatch(1);
            List<Future<Schema>> results = new ArrayList<>();
            for (int i = 0; i < threads; i++) {
                results.add(executor.submit(() -> {
                    start.await();
                    return registry.createBuilder(Person.ID).sdkSchema();
                }));
            }
            start.countDown();
            Schema first = results.get(0).get(10, TimeUnit.SECONDS);
            for (Future<Schema> result : results) {
                assertThat(result.get(10, TimeUnit.SECONDS)).isSameAs(first);
            }
        } finally {
            executor.shutdownNow();
        }
    }

    @Test
    void putLazyType_invalidSchema_throwsNamingIdAndCachesNothing() {
        AtomicInteger calls = new AtomicInteger();
        TypeRegistry nullSchema = TypeRegistry.builder().putLazyType(Person.ID, counting(calls, null)).build();
        TypeRegistry wrongId = TypeRegistry.builder().putLazyType(Person.ID, () -> Bird.SCHEMA).build();
        TypeRegistry noClass = TypeRegistry.builder().putLazyType(Person.ID, () -> Schema.structureBuilder(Person.ID)
                                                                                         .build()).build();
        TypeRegistry noBuilder = TypeRegistry.builder()
                                             .putLazyType(Person.ID, () -> Schema.structureBuilder(Person.ID)
                                                                                 .shapeClass(Person.class)
                                                                                 .build())
                                             .build();

        assertThatThrownBy(() -> nullSchema.getShapeClass(Person.ID))
            .isInstanceOf(IllegalStateException.class).hasMessageContaining(Person.ID.toString());
        assertThatThrownBy(() -> nullSchema.createBuilder(Person.ID)).isInstanceOf(IllegalStateException.class);
        assertThat(calls).hasValue(2);
        assertThatThrownBy(() -> wrongId.createBuilder(Person.ID))
            .isInstanceOf(IllegalStateException.class).hasMessageContaining(Bird.ID.toString());
        assertThatThrownBy(() -> noClass.getShapeClass(Person.ID))
            .isInstanceOf(IllegalStateException.class).hasMessageContaining("SerializableStruct");
        assertThat(noBuilder.getShapeClass(Person.ID)).isEqualTo(Person.class);
        assertThatThrownBy(() -> noBuilder.createBuilder(Person.ID))
            .isInstanceOf(IllegalStateException.class).hasMessageContaining("no builder supplier");
    }

    @Test
    void putAgain_replacesEntry() {
        TypeRegistry registry = TypeRegistry.builder()
                                            .putType(Person.ID, Person.class, Person::builder)
                                            .putLazyType(Person.ID, () -> Person.SCHEMA)
                                            .putType(Bird.ID, Bird.class, Bird::builder)
                                            .build();

        assertThat(registry.getShapeClass(Person.ID)).isEqualTo(Person.class);
        assertThat(registry.createBuilder(Bird.ID).sdkSchema()).isSameAs(Bird.SCHEMA);
    }

    @Test
    void build_isNotAffectedByLaterBuilderChanges() {
        TypeRegistry.Builder builder = TypeRegistry.builder().putType(Person.ID, Person.class, Person::builder);
        TypeRegistry registry = builder.build();

        builder.putType(Bird.ID, Bird.class, Bird::builder);

        assertThat(registry.contains(Bird.ID)).isFalse();
        assertThat(builder.build().contains(Bird.ID)).isTrue();
    }

    @Test
    void compose_firstRegistryWins() {
        TypeRegistry first = TypeRegistry.builder().putType(Person.ID, Person.class, Person::builder).build();
        // The second registry maps the same ID to another class and builder.
        TypeRegistry second = TypeRegistry.builder()
                                          .putType(Person.ID, Bird.class, Bird::builder)
                                          .putType(Bird.ID, Bird.class, Bird::builder)
                                          .build();

        TypeRegistry composed = TypeRegistry.compose(first, second);

        assertThat(composed.getShapeClass(Person.ID)).isEqualTo(Person.class);
        assertThat(composed.createBuilder(Person.ID).sdkSchema()).isSameAs(Person.SCHEMA);
        assertThat(composed.getShapeClass(Bird.ID)).isEqualTo(Bird.class);
        assertThat(composed.contains(Person.ID)).isTrue();
        assertThat(composed.contains(Bird.ID)).isTrue();
        assertThat(composed.contains(FOO)).isFalse();
        assertThat(composed.getShapeClass(FOO)).isNull();
        assertThat(composed.createBuilder(FOO)).isNull();
        assertThat(TypeRegistry.compose(second, first).getShapeClass(Person.ID)).isEqualTo(Bird.class);
        // Neither input is changed.
        assertThat(first.contains(Bird.ID)).isFalse();
    }

    @Test
    void empty_containsNothingAndIsComposeIdentity() {
        TypeRegistry empty = TypeRegistry.empty();
        TypeRegistry registry = TypeRegistry.builder().putType(Person.ID, Person.class, Person::builder).build();

        assertThat(TypeRegistry.empty()).isSameAs(empty);
        assertThat(empty.contains(Person.ID)).isFalse();
        assertThat(empty.getShapeClass(Person.ID)).isNull();
        assertThat(empty.createBuilder(Person.ID)).isNull();
        assertThat(TypeRegistry.compose(empty, registry)).isSameAs(registry);
        assertThat(TypeRegistry.compose(registry, empty)).isSameAs(registry);
        assertThat(TypeRegistry.compose(TypeRegistry.builder().build(), registry)).isSameAs(registry);
        assertThat(TypeRegistry.builder().build()).isSameAs(empty);
    }

    @Test
    void nullArguments_throwNullPointerException() {
        TypeRegistry.Builder builder = TypeRegistry.builder();

        assertThatThrownBy(() -> builder.putType(null, Person.class, Person::builder))
            .isInstanceOf(NullPointerException.class).hasMessageContaining("id");
        assertThatThrownBy(() -> builder.putType(Person.ID, null, Person::builder))
            .isInstanceOf(NullPointerException.class).hasMessageContaining("type");
        assertThatThrownBy(() -> builder.putType(Person.ID, Person.class, null))
            .isInstanceOf(NullPointerException.class).hasMessageContaining("builder");
        assertThatThrownBy(() -> builder.putLazyType(Person.ID, null))
            .isInstanceOf(NullPointerException.class).hasMessageContaining("schema");
        assertThatThrownBy(() -> TypeRegistry.compose(null, TypeRegistry.empty()))
            .isInstanceOf(NullPointerException.class).hasMessageContaining("first");
        assertThatThrownBy(() -> TypeRegistry.compose(TypeRegistry.empty(), null))
            .isInstanceOf(NullPointerException.class).hasMessageContaining("second");
        assertThatThrownBy(() -> TypeRegistry.empty().createBuilder(Person.ID, null))
            .isInstanceOf(NullPointerException.class).hasMessageContaining("type");
    }

    private static Supplier<Schema> counting(AtomicInteger calls, Schema schema) {
        return () -> {
            calls.incrementAndGet();
            return schema;
        };
    }
}
