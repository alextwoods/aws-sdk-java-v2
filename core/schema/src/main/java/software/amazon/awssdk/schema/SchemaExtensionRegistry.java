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

import java.util.ArrayList;
import java.util.List;
import java.util.ServiceLoader;
import software.amazon.awssdk.annotations.SdkInternalApi;

/**
 * The {@link SchemaExtensionProvider}s found by {@link ServiceLoader}, indexed by {@link SchemaExtensionKey#id}.
 *
 * <p>{@link #instance()} holds the registry for the class loader of {@link SchemaExtensionProvider}. It runs discovery
 * on first use, once. A failed discovery is not cached, so every later call reports the same error instead of a
 * {@link NoClassDefFoundError}.
 */
@SdkInternalApi
final class SchemaExtensionRegistry {
    private static final Object LOCK = new Object();
    private static volatile SchemaExtensionRegistry instance;

    private final SchemaExtensionProvider<?>[] providersByKey;

    /**
     * Creates a registry from the given providers.
     *
     * @throws IllegalStateException if a provider returns a null key, or two providers return the same key
     */
    SchemaExtensionRegistry(Iterable<? extends SchemaExtensionProvider<?>> providers) {
        // Read each key once, so a provider that returns different keys cannot defeat the duplicate check.
        List<SchemaExtensionProvider<?>> providerList = new ArrayList<>();
        List<SchemaExtensionKey<?>> keys = new ArrayList<>();
        int size = 0;
        for (SchemaExtensionProvider<?> provider : providers) {
            SchemaExtensionKey<?> key = provider.key();
            if (key == null) {
                throw new IllegalStateException("SchemaExtensionProvider " + provider.getClass().getName()
                                                + " returned a null key");
            }
            providerList.add(provider);
            keys.add(key);
            size = Math.max(size, key.id + 1);
        }

        SchemaExtensionProvider<?>[] result = new SchemaExtensionProvider<?>[size];
        for (int i = 0; i < providerList.size(); i++) {
            SchemaExtensionProvider<?> provider = providerList.get(i);
            SchemaExtensionKey<?> key = keys.get(i);
            SchemaExtensionProvider<?> existing = result[key.id];
            if (existing != null) {
                throw new IllegalStateException("More than one SchemaExtensionProvider is registered for " + key + ": "
                                                + existing.getClass().getName() + " and "
                                                + provider.getClass().getName());
            }
            result[key.id] = provider;
        }
        this.providersByKey = result;
    }

    /**
     * Returns the registry for the class loader of {@link SchemaExtensionProvider}, running discovery on first use.
     *
     * @throws IllegalStateException if discovery finds a provider with a null key or two providers for one key
     */
    static SchemaExtensionRegistry instance() {
        SchemaExtensionRegistry result = instance;
        if (result == null) {
            synchronized (LOCK) {
                result = instance;
                if (result == null) {
                    result = load(SchemaExtensionProvider.class.getClassLoader());
                    instance = result;
                }
            }
        }
        return result;
    }

    /**
     * Creates a registry from the providers that {@link ServiceLoader} finds through the given class loader.
     *
     * @throws IllegalStateException if a provider returns a null key, or two providers return the same key
     */
    static SchemaExtensionRegistry load(ClassLoader classLoader) {
        List<SchemaExtensionProvider<?>> discovered = new ArrayList<>();
        for (SchemaExtensionProvider<?> provider : ServiceLoader.load(SchemaExtensionProvider.class, classLoader)) {
            discovered.add(provider);
        }
        return new SchemaExtensionRegistry(discovered);
    }

    /**
     * Returns the provider for a key, or null if none is registered.
     */
    @SuppressWarnings("unchecked")
    <T> SchemaExtensionProvider<T> providerFor(SchemaExtensionKey<T> key) {
        int id = key.id;
        return id < providersByKey.length ? (SchemaExtensionProvider<T>) providersByKey[id] : null;
    }

    /**
     * Returns one more than the largest key ID with a provider: the size of a schema's extension array.
     */
    int size() {
        return providersByKey.length;
    }
}
