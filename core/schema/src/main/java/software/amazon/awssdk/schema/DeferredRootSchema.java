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

import java.util.List;
import java.util.function.Supplier;
import software.amazon.awssdk.annotations.SdkInternalApi;

/**
 * A root schema with at least one member that targets a {@link SchemaBuilder}. Its member schemas are built on first
 * access, once, so every caller sees the same member instances. {@link #resolve()} replaces it, in its builder, with
 * a {@link ResolvedRootSchema} that shares those member instances.
 *
 * <p>Port of smithy-java {@code software.amazon.smithy.java.core.schema.DeferredRootSchema}.
 */
@SdkInternalApi
final class DeferredRootSchema extends Schema {
    private final List<MemberSchemaBuilder> memberBuilders;
    private final SchemaBuilder schemaBuilder;
    private volatile SchemaBuilder.Members members;

    DeferredRootSchema(ShapeType type, ShapeId id, TraitMap traits, List<MemberSchemaBuilder> memberBuilders,
                       Supplier<? extends ShapeBuilder<?>> builderSupplier, Class<?> shapeClass,
                       SchemaBuilder schemaBuilder) {
        super(type, id, traits, builderSupplier, shapeClass);
        this.memberBuilders = memberBuilders;
        this.schemaBuilder = schemaBuilder;
    }

    @Override
    SchemaBuilder.Members memberData() {
        SchemaBuilder.Members result = members;
        if (result == null) {
            synchronized (memberBuilders) {
                result = members;
                if (result == null) {
                    result = SchemaBuilder.buildMembers(type(), memberBuilders);
                    members = result;
                }
            }
        }
        return result;
    }

    /**
     * Returns the resolved schema. Every call returns the same instance, which {@link SchemaBuilder#build()} also
     * returns from then on.
     */
    @Override
    public Schema resolve() {
        return schemaBuilder.resolve(this, memberData());
    }
}
