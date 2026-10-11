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
import static software.amazon.awssdk.schema.TestExtensionProviders.COUNTING_KEY;
import static software.amazon.awssdk.schema.TestExtensionProviders.FAILURES;
import static software.amazon.awssdk.schema.TestExtensionProviders.NULL_KEY;
import static software.amazon.awssdk.schema.TestExtensionProviders.SLOW;
import static software.amazon.awssdk.schema.TestExtensionProviders.THROWING_KEY;
import static software.amazon.awssdk.schema.TestExtensionProviders.UNREGISTERED_KEY;
import static software.amazon.awssdk.schema.TestExtensionProviders.invocations;

import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import software.amazon.awssdk.schema.TestExtensionProviders.CountingProvider;
import software.amazon.awssdk.schema.TestExtensionProviders.DuplicateCountingProvider;
import software.amazon.awssdk.schema.TestExtensionProviders.NullKeyProvider;
import software.amazon.awssdk.schema.TestExtensionProviders.NullProvider;
import software.amazon.awssdk.schema.TestExtensionProviders.ThrowingProvider;
import software.amazon.awssdk.schema.TestExtensionProviders.Value;

/**
 * Tests for {@link Schema#getExtension(SchemaExtensionKey)} and provider discovery (Requirement 5).
 */
class SchemaExtensionTest {
    private static final AtomicInteger NEXT_SHAPE = new AtomicInteger();

    /**
     * Returns a new schema instance, so that no other test has cached an extension on it.
     */
    private static Schema freshSchema() {
        return Schema.createString(ShapeId.fromParts("com.example.ext", "S" + NEXT_SHAPE.incrementAndGet()));
    }

    // Requirement 5.2
    @Test
    void registry_isDiscoveredOnceAndReused() {
        SchemaExtensionRegistry registry = SchemaExtensionRegistry.instance();
        SchemaExtensionProvider<Value> provider = registry.providerFor(COUNTING_KEY);

        freshSchema().getExtension(COUNTING_KEY);

        assertThat(SchemaExtensionRegistry.instance()).isSameAs(registry);
        assertThat(provider).isInstanceOf(CountingProvider.class);
        assertThat(SchemaExtensionRegistry.instance().providerFor(COUNTING_KEY)).isSameAs(provider);
        assertThat(registry.providerFor(NULL_KEY)).isInstanceOf(NullProvider.class);
        assertThat(registry.providerFor(THROWING_KEY)).isInstanceOf(ThrowingProvider.class);
    }

    // Requirements 5.3, 5.4
    @Test
    void getExtension_invokesProviderOnceWithSchemaAndCachesInstance() {
        Schema schema = freshSchema();

        Value first = schema.getExtension(COUNTING_KEY);
        Value second = schema.getExtension(COUNTING_KEY);
        Value third = schema.getExtension(COUNTING_KEY);

        assertThat(first.schema).isSameAs(schema);
        assertThat(second).isSameAs(first);
        assertThat(third).isSameAs(first);
        assertThat(invocations(CountingProvider.class, schema)).isEqualTo(1);
    }

    // Requirement 5.3: the cache belongs to the schema instance
    @Test
    void getExtension_cachesSeparatelyPerSchemaInstanceAndKey() {
        Schema a = freshSchema();
        Schema b = Schema.createString(a.id());

        Value valueA = a.getExtension(COUNTING_KEY);
        Value valueB = b.getExtension(COUNTING_KEY);
        Value throwingKeyA = a.getExtension(THROWING_KEY);

        assertThat(valueB).isNotSameAs(valueA);
        assertThat(valueB.schema).isSameAs(b);
        assertThat(throwingKeyA).isNotSameAs(valueA);
        assertThat(a.getExtension(COUNTING_KEY)).isSameAs(valueA);
        assertThat(a.getExtension(THROWING_KEY)).isSameAs(throwingKeyA);
        assertThat(invocations(CountingProvider.class, a)).isEqualTo(1);
        assertThat(invocations(CountingProvider.class, b)).isEqualTo(1);
        assertThat(invocations(ThrowingProvider.class, a)).isEqualTo(1);
    }

    // Requirement 5.5
    @ParameterizedTest
    @ValueSource(ints = {2, 8, 64})
    void getExtension_concurrentFirstCalls_allReturnOneInstance(int threads) throws Exception {
        ExecutorService executor = Executors.newFixedThreadPool(threads);
        try {
            for (int round = 0; round < 10; round++) {
                Schema schema = freshSchema();
                SLOW.add(schema);
                CyclicBarrier barrier = new CyclicBarrier(threads);
                List<Future<Value>> futures = new ArrayList<>();
                for (int i = 0; i < threads; i++) {
                    futures.add(executor.submit(() -> {
                        barrier.await(10, TimeUnit.SECONDS);
                        return schema.getExtension(COUNTING_KEY);
                    }));
                }

                Value expected = futures.get(0).get(10, TimeUnit.SECONDS);
                for (Future<Value> future : futures) {
                    assertThat(future.get(10, TimeUnit.SECONDS)).isSameAs(expected);
                }
                assertThat(expected.schema).isSameAs(schema);
                assertThat(schema.getExtension(COUNTING_KEY)).isSameAs(expected);
                assertThat(invocations(CountingProvider.class, schema)).isBetween(1, threads);
                SLOW.remove(schema);
            }
        } finally {
            executor.shutdownNow();
        }
    }

    // Requirement 5.6
    @Test
    void getExtension_behavesTheSameForEverySchemaKind() {
        Schema root = freshSchema();
        Schema structure = Schema.structureBuilder(ShapeId.fromParts("com.example.ext", "Struct"))
                                 .putMember("a", PreludeSchemas.STRING)
                                 .build();
        SchemaBuilder nodeBuilder = Schema.structureBuilder(ShapeId.fromParts("com.example.ext", "Node"));
        Schema deferredRoot = nodeBuilder.putMember("value", PreludeSchemas.STRING)
                                         .putMember("next", nodeBuilder)
                                         .build();
        Schema deferredMember = deferredRoot.member("next");
        Schema resolvedRoot = deferredRoot.resolve();

        List<Schema> schemas = Arrays.asList(root, structure, structure.member("a"), deferredRoot, deferredMember,
                                             resolvedRoot);
        assertThat(schemas).extracting(s -> s.getClass().getSimpleName())
                           .containsExactly("RootSchema", "RootSchema", "MemberSchema", "DeferredRootSchema",
                                            "DeferredMemberSchema", "ResolvedRootSchema");

        for (Schema schema : schemas) {
            Value value = schema.getExtension(COUNTING_KEY);
            assertThat(value.schema).as(schema.getClass().getSimpleName()).isSameAs(schema);
            assertThat(schema.getExtension(COUNTING_KEY)).isSameAs(value);
            assertThat(invocations(CountingProvider.class, schema)).isEqualTo(1);
            assertThat(schema.getExtension(NULL_KEY)).isNull();
            assertThat(schema.getExtension(NULL_KEY)).isNull();
            assertThat(invocations(NullProvider.class, schema)).isEqualTo(1);
            assertThat(schema.getExtension(UNREGISTERED_KEY)).isNull();
        }
    }

    // Requirement 5.6: a member schema and its target, and a deferred schema and its resolved form, are distinct
    // instances; resolution keeps the member schema instances, so their extensions are kept too
    @Test
    void getExtension_isNotSharedBetweenMemberAndTargetOrDeferredAndResolved() {
        Schema structure = Schema.structureBuilder(ShapeId.fromParts("com.example.ext", "Pair"))
                                 .putMember("a", PreludeSchemas.STRING)
                                 .build();
        Schema member = structure.member("a");
        SchemaBuilder nodeBuilder = Schema.structureBuilder(ShapeId.fromParts("com.example.ext", "Chain"));
        Schema deferred = nodeBuilder.putMember("next", nodeBuilder).build();
        Value deferredValue = deferred.getExtension(COUNTING_KEY);
        Value deferredMemberValue = deferred.member("next").getExtension(COUNTING_KEY);

        Schema resolved = deferred.resolve();

        assertThat(member.getExtension(COUNTING_KEY)).isNotSameAs(member.memberTarget().getExtension(COUNTING_KEY));
        assertThat(resolved).isNotSameAs(deferred);
        assertThat(resolved.getExtension(COUNTING_KEY)).isNotSameAs(deferredValue);
        assertThat(deferred.getExtension(COUNTING_KEY)).isSameAs(deferredValue);
        assertThat(resolved.member("next")).isSameAs(deferred.member("next"));
        assertThat(resolved.member("next").getExtension(COUNTING_KEY)).isSameAs(deferredMemberValue);
    }

    // Requirement 5.8
    @Test
    void getExtension_nullResult_isCachedAndProviderNotInvokedAgain() {
        Schema schema = freshSchema();

        assertThat(schema.getExtension(NULL_KEY)).isNull();
        assertThat(schema.getExtension(NULL_KEY)).isNull();
        assertThat(schema.getExtension(NULL_KEY)).isNull();

        assertThat(invocations(NullProvider.class, schema)).isEqualTo(1);
    }

    // Requirement 5.9
    @Test
    void getExtension_unregisteredKey_returnsNull() {
        Schema schema = freshSchema();
        SchemaExtensionKey<String> beyondRegistry = new SchemaExtensionKey<>();

        assertThat(SchemaExtensionRegistry.instance().providerFor(UNREGISTERED_KEY)).isNull();
        assertThat(UNREGISTERED_KEY.id).isLessThan(SchemaExtensionRegistry.instance().size());
        assertThat(schema.getExtension(UNREGISTERED_KEY)).isNull();
        assertThat(schema.getExtension(beyondRegistry)).isNull();

        // Also after the schema has an extension array.
        schema.getExtension(COUNTING_KEY);
        assertThat(schema.getExtension(UNREGISTERED_KEY)).isNull();
        assertThat(schema.getExtension(beyondRegistry)).isNull();
    }

    @Test
    void getExtension_nullKey_throwsNullPointerException() {
        assertThatThrownBy(() -> freshSchema().getExtension(null))
            .isInstanceOf(NullPointerException.class)
            .hasMessageContaining("key");
    }

    // Requirement 5.10
    @Test
    void getExtension_providerThrows_propagatesUnchangedCachesNothingAndRetries() {
        Schema schema = freshSchema();
        IllegalArgumentException failure = new IllegalArgumentException("boom");
        FAILURES.put(schema, failure);

        assertThatThrownBy(() -> schema.getExtension(THROWING_KEY)).isSameAs(failure);
        assertThatThrownBy(() -> schema.getExtension(THROWING_KEY)).isSameAs(failure);
        assertThat(invocations(ThrowingProvider.class, schema)).isEqualTo(2);

        FAILURES.remove(schema);
        Value value = schema.getExtension(THROWING_KEY);

        assertThat(value.invocation).isEqualTo(3);
        assertThat(schema.getExtension(THROWING_KEY)).isSameAs(value);
        assertThat(invocations(ThrowingProvider.class, schema)).isEqualTo(3);
    }

    // Requirement 5.10: a failure for one key leaves the other cached keys untouched
    @Test
    void getExtension_providerThrows_keepsOtherCachedValues() {
        Schema schema = freshSchema();
        Value counting = schema.getExtension(COUNTING_KEY);
        RuntimeException failure = new IllegalStateException("boom");
        FAILURES.put(schema, failure);

        assertThatThrownBy(() -> schema.getExtension(THROWING_KEY)).isSameAs(failure);

        assertThat(schema.getExtension(COUNTING_KEY)).isSameAs(counting);
        FAILURES.remove(schema);
    }

    // Requirement 5.11
    @Test
    void registry_withTwoProvidersForOneKey_failsNamingKeyAndBothClasses() {
        List<SchemaExtensionProvider<?>> providers = Arrays.asList(new CountingProvider(),
                                                                   new DuplicateCountingProvider());

        assertThatThrownBy(() -> new SchemaExtensionRegistry(providers))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining(COUNTING_KEY.toString())
            .hasMessageContaining(CountingProvider.class.getName())
            .hasMessageContaining(DuplicateCountingProvider.class.getName());
    }

    // Requirement 5.11, through ServiceLoader
    @Test
    void load_withServiceLoaderProviderDuplicatingKey_failsNamingKeyAndBothClasses(@TempDir Path dir)
        throws IOException {
        Path services = dir.resolve("META-INF/services");
        Files.createDirectories(services);
        Files.write(services.resolve(SchemaExtensionProvider.class.getName()),
                    DuplicateCountingProvider.class.getName().getBytes(StandardCharsets.UTF_8));

        try (URLClassLoader loader = new URLClassLoader(new URL[] {dir.toUri().toURL()},
                                                        SchemaExtensionTest.class.getClassLoader())) {
            assertThatThrownBy(() -> SchemaExtensionRegistry.load(loader))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining(COUNTING_KEY.toString())
                .hasMessageContaining(CountingProvider.class.getName())
                .hasMessageContaining(DuplicateCountingProvider.class.getName());
        }
    }

    @Test
    void registry_withNullKey_failsNamingProvider() {
        assertThatThrownBy(() -> new SchemaExtensionRegistry(Collections.singletonList(new NullKeyProvider())))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining(NullKeyProvider.class.getName());
    }

    @Test
    void load_withDefaultClassLoader_findsRegisteredProviders() {
        SchemaExtensionRegistry registry = SchemaExtensionRegistry.load(SchemaExtensionTest.class.getClassLoader());

        assertThat(registry.providerFor(COUNTING_KEY)).isInstanceOf(CountingProvider.class);
        assertThat(registry.providerFor(UNREGISTERED_KEY)).isNull();
        assertThat(registry.providerFor(new SchemaExtensionKey<Object>())).isNull();
    }

    @Test
    void keys_haveUniqueIncreasingIds() {
        SchemaExtensionKey<Object> first = new SchemaExtensionKey<>();
        SchemaExtensionKey<Object> second = new SchemaExtensionKey<>();

        assertThat(second.id).isGreaterThan(first.id);
        assertThat(first.toString()).isEqualTo("SchemaExtensionKey{id=" + first.id + "}");
    }
}
