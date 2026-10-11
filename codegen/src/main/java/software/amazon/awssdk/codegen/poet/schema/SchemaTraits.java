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

package software.amazon.awssdk.codegen.poet.schema;

import com.squareup.javapoet.CodeBlock;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.codegen.schema.TraitInclusionPolicy;
import software.amazon.awssdk.codegen.schema.TraitInitializers;
import software.amazon.awssdk.codegen.schema.TraitRenderContext;
import software.amazon.smithy.model.shapes.Shape;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.model.traits.Trait;

/**
 * Renders the included traits a shape or member declares itself (Requirements 9.9, 10.4), with the Trait_Initializer
 * registry, and caches the result per shape so the size estimate and the generated code use the same expressions.
 */
@SdkInternalApi
final class SchemaTraits {
    private final TraitInclusionPolicy policy;
    private final TraitInitializers initializers;
    private final TraitRenderContext context;
    private final Map<ShapeId, List<CodeBlock>> cache = new HashMap<>();

    SchemaTraits(TraitInclusionPolicy policy, TraitInitializers initializers, TraitRenderContext context) {
        this.policy = Objects.requireNonNull(policy, "policy");
        this.initializers = Objects.requireNonNull(initializers, "initializers");
        this.context = Objects.requireNonNull(context, "context");
    }

    /**
     * @param shape a shape or member
     * @return the rendered traits, sorted by trait ShapeId
     */
    List<CodeBlock> forShape(Shape shape) {
        List<CodeBlock> result = cache.get(shape.getId());
        if (result == null) {
            List<CodeBlock> rendered = new ArrayList<>();
            for (Trait trait : policy.includedTraits(shape)) {
                rendered.add(initializers.render(trait, context));
            }
            result = Collections.unmodifiableList(rendered);
            cache.put(shape.getId(), result);
        }
        return result;
    }

    /**
     * Renders traits as trailing varargs: {@code , a, b}, or nothing.
     */
    static CodeBlock arguments(List<CodeBlock> traits) {
        CodeBlock.Builder builder = CodeBlock.builder();
        for (CodeBlock trait : traits) {
            builder.add(", $L", trait);
        }
        return builder.build();
    }
}
