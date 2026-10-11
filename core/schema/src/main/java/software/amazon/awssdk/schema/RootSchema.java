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

import java.util.Collections;
import java.util.Set;
import java.util.function.Supplier;
import software.amazon.awssdk.annotations.SdkInternalApi;

/**
 * A schema for a non-member shape whose members, if any, are all built.
 *
 * <p>Port of smithy-java {@code software.amazon.smithy.java.core.schema.RootSchema}.
 */
@SdkInternalApi
final class RootSchema extends Schema {
    private final SchemaBuilder.Members members;
    private final Set<String> stringEnumValues;
    private final Set<Integer> intEnumValues;

    /**
     * Creates a schema for a simple, operation, resource or service shape.
     */
    RootSchema(ShapeType type, ShapeId id, TraitMap traits) {
        this(type, id, traits, Collections.<String>emptySet(), Collections.<Integer>emptySet(), null);
    }

    /**
     * Creates an enum or int enum schema. The value sets must already be unmodifiable.
     */
    RootSchema(ShapeType type, ShapeId id, TraitMap traits, Set<String> stringEnumValues, Set<Integer> intEnumValues,
               Class<?> shapeClass) {
        super(type, id, traits, null, shapeClass);
        this.members = SchemaBuilder.Members.NONE;
        this.stringEnumValues = stringEnumValues;
        this.intEnumValues = intEnumValues;
    }

    /**
     * Creates a structure, union, list or map schema.
     */
    RootSchema(ShapeType type, ShapeId id, TraitMap traits, SchemaBuilder.Members members,
               Supplier<? extends ShapeBuilder<?>> builderSupplier, Class<?> shapeClass) {
        super(type, id, traits, builderSupplier, shapeClass);
        this.members = members;
        this.stringEnumValues = Collections.emptySet();
        this.intEnumValues = Collections.emptySet();
    }

    @Override
    SchemaBuilder.Members memberData() {
        return members;
    }

    @Override
    public Set<String> stringEnumValues() {
        return stringEnumValues;
    }

    @Override
    public Set<Integer> intEnumValues() {
        return intEnumValues;
    }
}
