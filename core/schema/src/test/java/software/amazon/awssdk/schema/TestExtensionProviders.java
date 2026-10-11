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

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * {@link SchemaExtensionProvider}s for {@link SchemaExtensionTest}. The counting, null and throwing providers are
 * registered in this module's test {@code META-INF/services}; {@link DuplicateCountingProvider} is registered only
 * through a temporary class loader. Invocation counts are kept per schema instance, so tests that use fresh schemas
 * do not interfere with each other.
 */
public final class TestExtensionProviders {
    /**
     * A key with no provider. Declared first, so its ID is below the provider keys' IDs and inside every schema's
     * extension array.
     */
    static final SchemaExtensionKey<Object> UNREGISTERED_KEY = new SchemaExtensionKey<>();
    static final SchemaExtensionKey<Value> COUNTING_KEY = new SchemaExtensionKey<>();
    static final SchemaExtensionKey<Object> NULL_KEY = new SchemaExtensionKey<>();
    static final SchemaExtensionKey<Value> THROWING_KEY = new SchemaExtensionKey<>();

    /**
     * Schemas for which {@link CountingProvider} pauses, to widen the window in which concurrent first calls overlap.
     */
    static final Set<Schema> SLOW = ConcurrentHashMap.newKeySet();

    /**
     * Schemas for which {@link ThrowingProvider} throws the mapped exception.
     */
    static final Map<Schema, RuntimeException> FAILURES = new ConcurrentHashMap<>();

    private static final Map<String, Map<Schema, AtomicInteger>> INVOCATIONS = new ConcurrentHashMap<>();

    private TestExtensionProviders() {
    }

    /**
     * Returns how often the named provider class was invoked for the schema.
     */
    static int invocations(Class<?> provider, Schema schema) {
        AtomicInteger count = countsFor(provider).get(schema);
        return count == null ? 0 : count.get();
    }

    private static int record(Class<?> provider, Schema schema) {
        return countsFor(provider).computeIfAbsent(schema, s -> new AtomicInteger()).incrementAndGet();
    }

    private static Map<Schema, AtomicInteger> countsFor(Class<?> provider) {
        return INVOCATIONS.computeIfAbsent(provider.getName(), n -> new ConcurrentHashMap<>());
    }

    /**
     * An extension value that records the schema and invocation it was computed for. Every invocation creates a new
     * instance, so identity checks detect a value computed more than once and returned to different callers.
     */
    static final class Value {
        final Schema schema;
        final int invocation;

        Value(Schema schema, int invocation) {
            this.schema = schema;
            this.invocation = invocation;
        }
    }

    public static final class CountingProvider implements SchemaExtensionProvider<Value> {
        @Override
        public SchemaExtensionKey<Value> key() {
            return COUNTING_KEY;
        }

        @Override
        public Value provide(Schema schema) {
            int invocation = record(CountingProvider.class, schema);
            if (SLOW.contains(schema)) {
                try {
                    Thread.sleep(5);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
            return new Value(schema, invocation);
        }
    }

    public static final class NullProvider implements SchemaExtensionProvider<Object> {
        @Override
        public SchemaExtensionKey<Object> key() {
            return NULL_KEY;
        }

        @Override
        public Object provide(Schema schema) {
            record(NullProvider.class, schema);
            return null;
        }
    }

    public static final class ThrowingProvider implements SchemaExtensionProvider<Value> {
        @Override
        public SchemaExtensionKey<Value> key() {
            return THROWING_KEY;
        }

        @Override
        public Value provide(Schema schema) {
            int invocation = record(ThrowingProvider.class, schema);
            RuntimeException failure = FAILURES.get(schema);
            if (failure != null) {
                throw failure;
            }
            return new Value(schema, invocation);
        }
    }

    /**
     * Returns {@link #COUNTING_KEY}, which {@link CountingProvider} already provides.
     */
    public static final class DuplicateCountingProvider implements SchemaExtensionProvider<Value> {
        @Override
        public SchemaExtensionKey<Value> key() {
            return COUNTING_KEY;
        }

        @Override
        public Value provide(Schema schema) {
            return new Value(schema, 0);
        }
    }

    public static final class NullKeyProvider implements SchemaExtensionProvider<Object> {
        @Override
        public SchemaExtensionKey<Object> key() {
            return null;
        }

        @Override
        public Object provide(Schema schema) {
            return null;
        }
    }
}
