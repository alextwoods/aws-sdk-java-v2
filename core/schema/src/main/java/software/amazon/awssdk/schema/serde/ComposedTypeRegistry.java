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

import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.schema.SerializableStruct;
import software.amazon.awssdk.schema.ShapeBuilder;
import software.amazon.awssdk.schema.ShapeId;
import software.amazon.awssdk.schema.serde.document.Document;
import software.amazon.awssdk.utils.Validate;

/**
 * The result of {@link TypeRegistry#compose(TypeRegistry, TypeRegistry)}: resolves from the first registry when it
 * contains a shape ID, and otherwise from the second.
 */
@SdkInternalApi
final class ComposedTypeRegistry implements TypeRegistry {
    private final TypeRegistry first;
    private final TypeRegistry second;

    ComposedTypeRegistry(TypeRegistry first, TypeRegistry second) {
        this.first = first;
        this.second = second;
    }

    @Override
    public Class<? extends SerializableStruct> getShapeClass(ShapeId id) {
        return first.contains(id) ? first.getShapeClass(id) : second.getShapeClass(id);
    }

    @Override
    public ShapeBuilder<?> createBuilder(ShapeId id) {
        return first.contains(id) ? first.createBuilder(id) : second.createBuilder(id);
    }

    @Override
    public boolean contains(ShapeId id) {
        return first.contains(id) || second.contains(id);
    }

    /**
     * Delegates to the {@code deserialize} of the first registry when it contains the discriminator, and otherwise to
     * the second, so a registry that overrides {@code deserialize} keeps its behavior when composed.
     */
    @Override
    public SerializableStruct deserialize(Document document) {
        Validate.paramNotNull(document, "document");
        ShapeId id = document.expectDiscriminator();
        return first.contains(id) ? first.deserialize(document) : second.deserialize(document);
    }

    @Override
    public String toString() {
        return "TypeRegistry.compose(" + first + ", " + second + ")";
    }
}
