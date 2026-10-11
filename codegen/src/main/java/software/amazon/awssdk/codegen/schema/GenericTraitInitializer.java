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

package software.amazon.awssdk.codegen.schema;

import com.squareup.javapoet.CodeBlock;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.smithy.model.traits.Trait;

/**
 * Renders a trait that has no registered {@link TraitInitializer} as
 * {@code new GenericTrait(ShapeId.fromParts(ns, name), <Document literal>)} (Requirement 9.7). Port of smithy-java's
 * {@code GenericTraitInitializer}, which instead calls the trait's smithy-model {@code TraitService} at runtime.
 */
@SdkInternalApi
public final class GenericTraitInitializer {
    public static final GenericTraitInitializer INSTANCE = new GenericTraitInitializer();

    private GenericTraitInitializer() {
    }

    /**
     * @param trait any model trait
     * @param context rendering helpers
     * @return a {@code GenericTrait} expression whose value equals the trait's node value
     */
    public CodeBlock render(Trait trait, TraitRenderContext context) {
        return CodeBlock.of("new $T($L, $L)", SchemaRuntimeType.GENERIC_TRAIT, context.shapeId(trait.toShapeId()),
                            context.document(trait.toNode()));
    }
}
