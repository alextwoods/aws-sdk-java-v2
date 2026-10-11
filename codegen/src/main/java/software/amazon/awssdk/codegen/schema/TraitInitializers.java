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
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.ServiceLoader;
import java.util.TreeMap;
import software.amazon.awssdk.annotations.SdkProtectedApi;
import software.amazon.smithy.model.node.ExpectationNotMetException;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.model.traits.Trait;

/**
 * The registry of {@link TraitInitializer}s: the SDK's built-in ones ({@link CoreSchemaCodegenExtension}) plus those of
 * every {@link SchemaCodegenExtension} given or discovered. Renders a trait with its initializer, or as a
 * {@code GenericTrait} when it has none (Requirements 9.7, 9.8).
 *
 * <p>Construction fails with a {@link SchemaCodegenException} if two initializers are registered for one trait
 * ShapeId, so the Schema_Generator, which builds the registry before generating, writes no code in that case.
 */
@SdkProtectedApi
public final class TraitInitializers {
    private final Map<ShapeId, TraitInitializer<?>> initializers;

    private TraitInitializers(Map<ShapeId, TraitInitializer<?>> initializers) {
        this.initializers = Collections.unmodifiableMap(initializers);
    }

    /**
     * Builds the registry from the built-in initializers and every {@link SchemaCodegenExtension} that
     * {@link ServiceLoader} finds through the given class loader.
     *
     * @param classLoader the class loader to search, usually the Codegen plugin's
     * @return the registry
     * @throws SchemaCodegenException if two initializers are registered for one trait
     */
    public static TraitInitializers load(ClassLoader classLoader) {
        List<SchemaCodegenExtension> extensions = new ArrayList<>();
        for (SchemaCodegenExtension extension : ServiceLoader.load(SchemaCodegenExtension.class, classLoader)) {
            extensions.add(extension);
        }
        return of(extensions);
    }

    /**
     * Builds the registry from the built-in initializers and the given extensions.
     *
     * @param extensions additional extensions; must not include {@link CoreSchemaCodegenExtension}
     * @return the registry
     * @throws SchemaCodegenException if two initializers are registered for one trait
     */
    public static TraitInitializers of(Collection<? extends SchemaCodegenExtension> extensions) {
        List<SchemaCodegenExtension> all = new ArrayList<>();
        all.add(new CoreSchemaCodegenExtension());
        all.addAll(extensions);

        Map<ShapeId, TraitInitializer<?>> byId = new TreeMap<>();
        List<String> conflicts = new ArrayList<>();
        for (SchemaCodegenExtension extension : all) {
            for (TraitInitializer<?> initializer : extension.traitInitializers()) {
                ShapeId traitId = Objects.requireNonNull(initializer.traitId(), "traitId of " + initializer);
                TraitInitializer<?> existing = byId.putIfAbsent(traitId, initializer);
                if (existing != null) {
                    conflicts.add("trait " + traitId + " has more than one initializer: " + describe(existing) + " and "
                                  + describe(initializer));
                }
            }
        }
        if (!conflicts.isEmpty()) {
            throw new SchemaCodegenException("Conflicting trait initializers: " + String.join("; ", conflicts));
        }
        return new TraitInitializers(byId);
    }

    private static String describe(TraitInitializer<?> initializer) {
        return initializer.getClass().getName();
    }

    /**
     * @param traitId a trait ShapeId
     * @return the registered initializer, or null if the trait is rendered as a {@code GenericTrait}
     */
    public TraitInitializer<?> initializerFor(ShapeId traitId) {
        return initializers.get(traitId);
    }

    /**
     * @return the registered initializers by trait ShapeId, sorted
     */
    public Map<ShapeId, TraitInitializer<?>> initializers() {
        return initializers;
    }

    /**
     * Renders a trait's runtime value with its registered initializer, or as a {@code GenericTrait}.
     *
     * @param trait the model trait
     * @param context rendering helpers
     * @return a Java expression of a runtime trait type
     * @throws SchemaCodegenException if the initializer does not accept the trait's class or value
     */
    public CodeBlock render(Trait trait, TraitRenderContext context) {
        TraitInitializer<?> initializer = initializers.get(trait.toShapeId());
        if (initializer == null) {
            return GenericTraitInitializer.INSTANCE.render(trait, context);
        }
        return render(initializer, trait, context);
    }

    private static <T extends Trait> CodeBlock render(TraitInitializer<T> initializer, Trait trait,
                                                      TraitRenderContext context) {
        if (!initializer.traitClass().isInstance(trait)) {
            throw new SchemaCodegenException("Trait initializer " + describe(initializer) + " for " + trait.toShapeId()
                                             + " accepts " + initializer.traitClass().getName() + ", but the model trait "
                                             + "is a " + trait.getClass().getName());
        }
        try {
            return initializer.render(initializer.traitClass().cast(trait), context);
        } catch (ExpectationNotMetException | SchemaCodegenException e) {
            throw new SchemaCodegenException("Cannot render trait " + trait.toShapeId() + " with value "
                                             + Node.printJson(trait.toNode()) + ": "
                                             + e.getMessage(), e);
        }
    }
}
