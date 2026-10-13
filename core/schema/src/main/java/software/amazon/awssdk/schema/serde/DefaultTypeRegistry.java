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

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReferenceFieldUpdater;
import java.util.function.Supplier;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.schema.Schema;
import software.amazon.awssdk.schema.SerializableStruct;
import software.amazon.awssdk.schema.ShapeBuilder;
import software.amazon.awssdk.schema.ShapeId;
import software.amazon.awssdk.utils.Validate;

/**
 * The immutable registry built by {@link TypeRegistry.Builder}, and the entry types shared with {@link ErrorRegistry}.
 */
@SdkInternalApi
final class DefaultTypeRegistry implements TypeRegistry {
    static final DefaultTypeRegistry EMPTY = new DefaultTypeRegistry(Collections.emptyMap());

    private final Map<ShapeId, Entry> entries;

    private DefaultTypeRegistry(Map<ShapeId, Entry> entries) {
        this.entries = entries;
    }

    /**
     * Creates a registry holding a copy of the entries.
     */
    static DefaultTypeRegistry of(Map<ShapeId, Entry> entries) {
        if (entries.isEmpty()) {
            return EMPTY;
        }
        return new DefaultTypeRegistry(Collections.unmodifiableMap(new HashMap<>(entries)));
    }

    /**
     * Returns whether a registry is known to contain no shape IDs, so that composing with it can return the other
     * registry.
     */
    static boolean isEmpty(TypeRegistry registry) {
        if (registry instanceof DefaultTypeRegistry) {
            return ((DefaultTypeRegistry) registry).isEmpty();
        }
        return registry instanceof ErrorRegistry && ((ErrorRegistry) registry).isEmpty();
    }

    static <T extends SerializableStruct> Entry eagerEntry(Class<T> type, Supplier<ShapeBuilder<T>> builder) {
        return new EagerEntry(Validate.paramNotNull(type, "type"), Validate.paramNotNull(builder, "builder"));
    }

    static Entry lazyEntry(ShapeId id, Supplier<Schema> schema) {
        return new LazyEntry(id, Validate.paramNotNull(schema, "schema"));
    }

    boolean isEmpty() {
        return entries.isEmpty();
    }

    @Override
    public Class<? extends SerializableStruct> getShapeClass(ShapeId id) {
        Entry entry = entries.get(id);
        return entry == null ? null : entry.type();
    }

    @Override
    public ShapeBuilder<?> createBuilder(ShapeId id) {
        Entry entry = entries.get(id);
        return entry == null ? null : entry.newBuilder();
    }

    @Override
    public boolean contains(ShapeId id) {
        return entries.containsKey(id);
    }

    /**
     * Returns the schema of a lazy entry, or null if the shape ID is not registered or was registered without a schema.
     */
    Schema getSchema(ShapeId id) {
        Entry entry = entries.get(id);
        return entry == null ? null : entry.schema();
    }

    @Override
    public String toString() {
        return "TypeRegistry" + entries.keySet();
    }

    /**
     * One registered shape.
     */
    interface Entry {
        Class<? extends SerializableStruct> type();

        ShapeBuilder<?> newBuilder();

        /**
         * The schema of the entry, or null for an entry registered without one.
         */
        Schema schema();
    }

    /**
     * An entry registered with its class and builder supplier.
     */
    private static final class EagerEntry implements Entry {
        private final Class<? extends SerializableStruct> type;
        private final Supplier<? extends ShapeBuilder<?>> builder;

        private EagerEntry(Class<? extends SerializableStruct> type, Supplier<? extends ShapeBuilder<?>> builder) {
            this.type = type;
            this.builder = builder;
        }

        @Override
        public Class<? extends SerializableStruct> type() {
            return type;
        }

        @Override
        public ShapeBuilder<?> newBuilder() {
            return builder.get();
        }

        @Override
        public Schema schema() {
            return null;
        }
    }

    /**
     * An entry whose class and builder come from a schema obtained on first use.
     *
     * <p>The schema is published once with {@code compareAndSet}: when threads race on the first lookup, each may
     * invoke the supplier, but all of them, and every later caller, use the schema that was published first. A
     * supplier that throws or returns an invalid schema leaves nothing cached.
     */
    private static final class LazyEntry implements Entry {
        private static final AtomicReferenceFieldUpdater<LazyEntry, Schema> SCHEMA =
            AtomicReferenceFieldUpdater.newUpdater(LazyEntry.class, Schema.class, "schema");

        private final ShapeId id;
        private final Supplier<Schema> supplier;
        private volatile Schema schema;

        private LazyEntry(ShapeId id, Supplier<Schema> supplier) {
            this.id = id;
            this.supplier = supplier;
        }

        @Override
        @SuppressWarnings("unchecked")
        public Class<? extends SerializableStruct> type() {
            // Checked by resolve().
            return (Class<? extends SerializableStruct>) resolve().shapeClass();
        }

        @Override
        public ShapeBuilder<?> newBuilder() {
            ShapeBuilder<?> builder = resolve().shapeBuilder();
            if (builder == null) {
                throw new IllegalStateException("The schema registered for " + id + " has no builder supplier");
            }
            return builder;
        }

        @Override
        public Schema schema() {
            return resolve();
        }

        private Schema resolve() {
            Schema result = schema;
            if (result != null) {
                return result;
            }
            Schema supplied = supplier.get();
            validate(supplied);
            if (SCHEMA.compareAndSet(this, null, supplied)) {
                return supplied;
            }
            return schema;
        }

        private void validate(Schema supplied) {
            if (supplied == null) {
                throw new IllegalStateException("The schema supplier registered for " + id + " returned null");
            }
            if (!id.equals(supplied.id())) {
                throw new IllegalStateException("The schema supplier registered for " + id + " returned the schema of "
                                                + supplied.id());
            }
            Class<?> type = supplied.shapeClass();
            if (type == null || !SerializableStruct.class.isAssignableFrom(type)) {
                throw new IllegalStateException("The schema registered for " + id + " has no shape class that "
                                                + "implements SerializableStruct: " + type);
            }
        }
    }
}
