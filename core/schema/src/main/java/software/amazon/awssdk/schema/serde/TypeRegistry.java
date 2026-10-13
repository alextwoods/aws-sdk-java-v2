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

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Supplier;
import software.amazon.awssdk.annotations.SdkPublicApi;
import software.amazon.awssdk.schema.Schema;
import software.amazon.awssdk.schema.SerializableStruct;
import software.amazon.awssdk.schema.ShapeBuilder;
import software.amazon.awssdk.schema.ShapeId;
import software.amazon.awssdk.utils.Validate;

/**
 * A registry from shape IDs to generated structure or union classes and their builders, used to create typed values
 * on demand, for example a modeled exception from an error response's error code.
 *
 * <p>Registries created by {@link #builder()}, {@link #empty()} and {@link #compose(TypeRegistry, TypeRegistry)} are
 * immutable and safe to use from multiple threads. Lookups of a shape ID that is not registered return {@code null}
 * (or {@code false} from {@link #contains(ShapeId)}) and never throw.
 *
 * <p>Port of smithy-java {@code software.amazon.smithy.java.core.serde.TypeRegistry}. Lazy entries are registered with
 * {@link Builder#putLazyType(ShapeId, Supplier)}, which takes one schema supplier instead of smithy-java's class and
 * builder suppliers. Document deserialization ({@code deserialize(Document)}) is added with the full document port.
 */
@SdkPublicApi
public interface TypeRegistry {
    /**
     * Returns the class registered for a shape ID. For a lazy entry, the first call loads the class.
     *
     * @param id the shape ID
     * @return the registered class, or null if the shape ID is not registered
     */
    Class<? extends SerializableStruct> getShapeClass(ShapeId id);

    /**
     * Creates a new builder for the shape registered for a shape ID. For a lazy entry, the first call loads the class.
     *
     * @param id the shape ID
     * @return a new builder, or null if the shape ID is not registered
     */
    ShapeBuilder<?> createBuilder(ShapeId id);

    /**
     * Returns whether a shape ID is registered. This never loads a registered class.
     *
     * @param id the shape ID
     * @return true if the shape ID is registered
     */
    boolean contains(ShapeId id);

    /**
     * Creates a new builder for the shape registered for a shape ID, checking that the shape is of an expected type.
     *
     * @param id the shape ID; null returns null
     * @param type the class the registered class must be assignable to
     * @param <T> the expected shape type
     * @return a new builder, or null if the shape ID is null or not registered
     * @throws SerializationException if the registered class is not assignable to {@code type}
     * @throws NullPointerException if {@code type} is null
     */
    @SuppressWarnings("unchecked")
    default <T extends SerializableStruct> ShapeBuilder<T> createBuilder(ShapeId id, Class<T> type) {
        Validate.paramNotNull(type, "type");
        if (id == null) {
            return null;
        }
        ShapeBuilder<?> builder = createBuilder(id);
        if (builder == null) {
            return null;
        }
        Class<? extends SerializableStruct> registeredType = getShapeClass(id);
        if (registeredType == null || !type.isAssignableFrom(registeredType)) {
            throw new SerializationException("Polymorphic shape " + id + " is not compatible with " + type);
        }
        return (ShapeBuilder<T>) builder;
    }

    /**
     * Returns the registry that contains no shape IDs. It is an identity element for
     * {@link #compose(TypeRegistry, TypeRegistry)}.
     *
     * @return the empty registry; every call returns the same instance
     */
    static TypeRegistry empty() {
        return DefaultTypeRegistry.EMPTY;
    }

    /**
     * Combines two registries. The result resolves a shape ID from {@code first} when {@code first} contains it, and
     * otherwise from {@code second}. Neither registry is changed.
     *
     * <p>When either registry is empty (the {@link #empty()} registry, or one built with no entries), the other one is
     * returned as is.
     *
     * @param first the registry checked first
     * @param second the registry checked when {@code first} does not contain a shape ID
     * @return the combined registry
     * @throws NullPointerException if either registry is null
     */
    static TypeRegistry compose(TypeRegistry first, TypeRegistry second) {
        Validate.paramNotNull(first, "first");
        Validate.paramNotNull(second, "second");
        if (DefaultTypeRegistry.isEmpty(first)) {
            return second;
        }
        if (DefaultTypeRegistry.isEmpty(second)) {
            return first;
        }
        return new ComposedTypeRegistry(first, second);
    }

    /**
     * Creates a builder for a registry.
     *
     * @return a new builder
     */
    static Builder builder() {
        return new Builder();
    }

    /**
     * Builds an immutable {@link TypeRegistry}. Registering a shape ID again replaces its earlier entry. A builder is
     * not safe for concurrent use; the registries it builds are.
     */
    final class Builder {
        private final Map<ShapeId, DefaultTypeRegistry.Entry> entries = new LinkedHashMap<>();

        private Builder() {
        }

        /**
         * Registers a shape with its class and builder supplier.
         *
         * @param id the shape ID
         * @param type the generated class of the shape
         * @param builder creates a new builder for the shape on each call
         * @param <T> the shape type
         * @return this builder
         * @throws NullPointerException if any argument is null
         */
        public <T extends SerializableStruct> Builder putType(ShapeId id, Class<T> type,
                                                              Supplier<ShapeBuilder<T>> builder) {
            Validate.paramNotNull(id, "id");
            entries.put(id, DefaultTypeRegistry.eagerEntry(type, builder));
            return this;
        }

        /**
         * Registers a shape whose class and builder come from its schema, without loading the class. The supplier is
         * invoked when the shape ID is first resolved through {@link TypeRegistry#getShapeClass(ShapeId)} or
         * {@link TypeRegistry#createBuilder(ShapeId)}, not by {@link TypeRegistry#contains(ShapeId)}, and its result is
         * cached. Generated code passes a non-capturing lambda such as {@code () -> QueueDoesNotExistException.SCHEMA},
         * so that building the registry initializes no generated class.
         *
         * <p>When the shape is first resolved, the supplied schema must have the registered shape ID, a
         * {@linkplain Schema#shapeClass() shape class} that implements {@link SerializableStruct}, and a builder
         * supplier; otherwise the lookup throws {@link IllegalStateException} naming the shape ID.
         *
         * @param id the shape ID
         * @param schema supplies the shape's root schema
         * @return this builder
         * @throws NullPointerException if any argument is null
         */
        public Builder putLazyType(ShapeId id, Supplier<Schema> schema) {
            Validate.paramNotNull(id, "id");
            entries.put(id, DefaultTypeRegistry.lazyEntry(id, schema));
            return this;
        }

        /**
         * Creates a registry with the shapes registered so far. Later changes to this builder do not affect it.
         *
         * @return the registry
         */
        public TypeRegistry build() {
            return DefaultTypeRegistry.of(entries);
        }
    }
}
