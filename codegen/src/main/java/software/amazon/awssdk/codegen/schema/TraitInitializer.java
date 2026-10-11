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
import software.amazon.awssdk.annotations.SdkProtectedApi;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.model.traits.Trait;

/**
 * Renders the Java expression that creates one trait's runtime value in generated Schema code (port of smithy-java
 * {@code TraitInitializer}, Requirement 9.8). The expression must evaluate to a
 * {@code software.amazon.awssdk.schema.traits.Trait}.
 *
 * <p>Initializers are registered per trait ShapeId through {@link SchemaCodegenExtension}. A trait with no registered
 * initializer is rendered as a {@code GenericTrait} (Requirement 9.7).
 *
 * @param <T> the smithy-model trait class the initializer reads. Initializers that read only {@link Trait#toNode()} use
 *     {@link Trait}.
 */
@SdkProtectedApi
public interface TraitInitializer<T extends Trait> {
    /**
     * @return the ShapeId of the trait this initializer renders
     */
    ShapeId traitId();

    /**
     * @return the smithy-model class that {@link #render} accepts
     */
    Class<T> traitClass();

    /**
     * Renders the runtime value of a trait.
     *
     * @param trait the model trait, whose ShapeId is {@link #traitId()}
     * @param context rendering helpers for shape IDs and document values
     * @return a Java expression of a runtime trait type
     */
    CodeBlock render(T trait, TraitRenderContext context);
}
