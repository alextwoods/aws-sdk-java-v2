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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.ServiceLoader;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.schema.ShapeId;
import software.amazon.awssdk.schema.serde.SerializationException;
import software.amazon.awssdk.schema.serde.document.Document;

/**
 * An immutable map from trait ID to {@link TraitFactory}, built from the SDK's built-in factories plus the factories
 * found by {@link ServiceLoader}. {@link TraitFactories} holds the default instance.
 */
@SdkInternalApi
final class TraitFactoryRegistry {
    private final Map<ShapeId, TraitFactory<?>> factories;

    TraitFactoryRegistry(Iterable<? extends TraitFactory<?>> builtIns, Iterable<? extends TraitFactory<?>> discovered) {
        Map<ShapeId, TraitFactory<?>> result = new HashMap<>();
        register(result, builtIns);
        register(result, discovered);
        this.factories = result;
    }

    /**
     * Creates a registry from the built-in factories and the factories that {@link ServiceLoader} finds through the
     * given class loader.
     *
     * @throws IllegalStateException if two factories are found for one trait ID
     */
    static TraitFactoryRegistry load(ClassLoader classLoader) {
        List<TraitFactory<?>> discovered = new ArrayList<>();
        for (TraitFactory<?> factory : ServiceLoader.load(TraitFactory.class, classLoader)) {
            discovered.add(factory);
        }
        return new TraitFactoryRegistry(BuiltInTraitFactories.all(), discovered);
    }

    TraitFactory<?> factoryFor(ShapeId traitId) {
        return factories.get(traitId);
    }

    Trait create(ShapeId traitId, Document value) {
        TraitFactory<?> factory = factories.get(traitId);
        if (factory == null) {
            return new GenericTrait(traitId, value);
        }
        Trait trait;
        try {
            trait = factory.create(value);
        } catch (SerializationException e) {
            String message = e.getMessage();
            if (message != null && message.contains(traitId.toString())) {
                throw e;
            }
            throw new SerializationException("Invalid value for trait " + traitId + ": " + message, e);
        }
        if (trait == null) {
            throw new IllegalStateException(describe(factory) + " returned null for trait " + traitId);
        }
        return trait;
    }

    private static void register(Map<ShapeId, TraitFactory<?>> result, Iterable<? extends TraitFactory<?>> factories) {
        for (TraitFactory<?> factory : factories) {
            ShapeId traitId = factory.traitId();
            if (traitId == null) {
                throw new IllegalStateException(describe(factory) + " returned a null trait ID");
            }
            TraitFactory<?> existing = result.putIfAbsent(traitId, factory);
            if (existing != null) {
                throw new IllegalStateException("More than one TraitFactory is registered for trait " + traitId + ": "
                                                + describe(existing) + " and " + describe(factory));
            }
        }
    }

    private static String describe(TraitFactory<?> factory) {
        return factory.getClass().getName() + " (creates " + factory.traitClass().getName() + ")";
    }
}
