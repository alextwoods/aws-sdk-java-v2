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

package software.amazon.awssdk.schema.traits;

import software.amazon.awssdk.annotations.SdkProtectedApi;
import software.amazon.awssdk.schema.ShapeId;
import software.amazon.awssdk.schema.serde.SerializationException;
import software.amazon.awssdk.schema.serde.document.Document;
import software.amazon.awssdk.utils.Validate;

/**
 * The trait factory registry: creates runtime traits from a trait ID and a document value.
 *
 * <p>The registry holds a factory for every typed trait in this package, plus the {@link TraitFactory}
 * implementations that {@link java.util.ServiceLoader} finds through the class loader of {@link TraitFactory}. It is
 * built on first use. If two factories exist for the same trait ID, building it fails with an
 * {@link IllegalStateException} that names the trait ID and both factory classes; the failure is not cached, so every
 * later call fails the same way.
 */
@SdkProtectedApi
public final class TraitFactories {
    private static final Object LOCK = new Object();
    private static volatile TraitFactoryRegistry registry;

    private TraitFactories() {
    }

    /**
     * Creates a trait from its ID and value. Returns the typed trait when a factory is registered for the ID, and a
     * {@link GenericTrait} holding the ID and value otherwise.
     *
     * @param traitId the trait's shape ID
     * @param value the trait value, in the form of the trait's value in the Smithy JSON AST; null represents a Smithy
     *              {@code null} value
     * @return the trait
     * @throws SerializationException if a factory is registered and the value does not match the trait's modeled shape.
     *                                The message names the trait ID.
     * @throws IllegalStateException if the registry cannot be built because two factories exist for one trait ID
     * @throws NullPointerException if {@code traitId} is null
     */
    public static Trait create(ShapeId traitId, Document value) {
        Validate.paramNotNull(traitId, "traitId");
        return registry().create(traitId, value);
    }

    /**
     * Returns the factory registered for a trait ID.
     *
     * @param traitId the trait's shape ID
     * @return the factory, or null if the trait has no typed runtime class
     * @throws IllegalStateException if the registry cannot be built because two factories exist for one trait ID
     * @throws NullPointerException if {@code traitId} is null
     */
    public static TraitFactory<?> factoryFor(ShapeId traitId) {
        Validate.paramNotNull(traitId, "traitId");
        return registry().factoryFor(traitId);
    }

    private static TraitFactoryRegistry registry() {
        TraitFactoryRegistry result = registry;
        if (result == null) {
            synchronized (LOCK) {
                result = registry;
                if (result == null) {
                    result = TraitFactoryRegistry.load(TraitFactory.class.getClassLoader());
                    registry = result;
                }
            }
        }
        return result;
    }
}
