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

package software.amazon.awssdk.smithy.runtime.registry;

import java.util.Objects;
import java.util.function.Supplier;
import software.amazon.smithy.java.core.schema.Schema;
import software.amazon.smithy.java.core.schema.SerializableStruct;
import software.amazon.smithy.java.core.schema.ShapeBuilder;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.model.shapes.ShapeType;

/**
 * One entry of a {@link SchemaTypeRegistry}: the exact shape ID of a generated aggregate type (a structure, including
 * requests, responses and modeled errors, or a union), its one exported schema, its Java class and the factory of its
 * builder (Requirement 6.7).
 *
 * <p>Generated registries create exactly one entry per type, so an entry is compared by identity: registry
 * composition preserves the entry objects (Requirement 6.8) rather than equal copies.
 *
 * @param <T> the generated type
 */
public final class RegisteredType<T extends SerializableStruct> {

    private final Schema schema;
    private final Class<T> type;
    private final Supplier<? extends ShapeBuilder<T>> builderFactory;

    private RegisteredType(Schema schema, Class<T> type, Supplier<? extends ShapeBuilder<T>> builderFactory) {
        this.schema = schema;
        this.type = type;
        this.builderFactory = builderFactory;
    }

    /**
     * Creates the entry of one generated type.
     *
     * @param schema the type's exported schema; its ID is the entry's shape ID
     * @param type the generated Java class
     * @param builderFactory creates a new builder of the type for each call (generated code passes
     *        {@code Type::builder})
     * @throws IllegalArgumentException if {@code schema} is a member schema or not a structure or union schema
     */
    public static <T extends SerializableStruct> RegisteredType<T> of(Schema schema, Class<T> type,
                                                                      Supplier<? extends ShapeBuilder<T>> builderFactory) {
        Objects.requireNonNull(schema, "schema");
        Objects.requireNonNull(type, "type");
        Objects.requireNonNull(builderFactory, "builderFactory");
        if (schema.isMember() || schema.type() != ShapeType.STRUCTURE && schema.type() != ShapeType.UNION) {
            throw new IllegalArgumentException("Only structure and union schemas are registered, not the "
                                               + schema.type() + " schema " + schema.id() + " of " + type.getName());
        }
        return new RegisteredType<>(schema, type, builderFactory);
    }

    /** The exact absolute shape ID of the type, the ID of its {@link #schema()}. */
    public ShapeId shapeId() {
        return schema.id();
    }

    /** The type's one exported schema. */
    public Schema schema() {
        return schema;
    }

    /** The generated Java class of the type. */
    public Class<T> type() {
        return type;
    }

    /** The builder factory the entry was registered with. */
    public Supplier<? extends ShapeBuilder<T>> builderFactory() {
        return builderFactory;
    }

    /**
     * A new builder of the type, from the registered factory.
     *
     * @throws IllegalStateException if the factory returns {@code null}
     */
    public ShapeBuilder<T> createBuilder() {
        ShapeBuilder<T> builder = builderFactory.get();
        if (builder == null) {
            throw new IllegalStateException("The builder factory of " + shapeId() + " returned null");
        }
        return builder;
    }

    @Override
    public String toString() {
        return "RegisteredType[" + shapeId() + " -> " + type.getName() + "]";
    }
}
