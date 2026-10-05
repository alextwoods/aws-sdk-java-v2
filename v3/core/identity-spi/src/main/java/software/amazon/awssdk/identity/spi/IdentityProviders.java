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

package software.amazon.awssdk.identity.spi;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import software.amazon.awssdk.utils.ToString;
import software.amazon.awssdk.utils.builder.CopyableBuilder;
import software.amazon.awssdk.utils.builder.ToCopyableBuilder;

/**
 * The identity providers one client (or one call) can resolve identities with, keyed by
 * {@link IdentityProvider#identityType() identity type}, with the V2 name and members.
 *
 * <p>An auth scheme asks for a provider of the identity type it signs with. The provider registered for exactly that type
 * wins; otherwise the first-registered provider whose identity type is a subtype of it is used (a session-credentials
 * provider can serve an auth scheme that accepts any AWS credentials). Registering a second provider for one identity
 * type replaces the first. Immutable.
 */
public final class IdentityProviders implements ToCopyableBuilder<IdentityProviders.Builder, IdentityProviders> {
    private static final IdentityProviders EMPTY = new IdentityProviders(new Builder());

    private final Map<Class<?>, IdentityProvider<?>> providers;

    private IdentityProviders(Builder builder) {
        this.providers = Collections.unmodifiableMap(new LinkedHashMap<>(builder.providers));
    }

    /** No identity providers. */
    public static IdentityProviders empty() {
        return EMPTY;
    }

    public static Builder builder() {
        return new Builder();
    }

    /** The provider of {@code identityType} identities, per the class description, if one is registered. */
    @SuppressWarnings("unchecked")
    public <T extends Identity> Optional<IdentityProvider<? extends T>> identityProvider(Class<T> identityType) {
        Objects.requireNonNull(identityType, "identityType");
        IdentityProvider<?> exact = providers.get(identityType);
        if (exact != null) {
            return Optional.of((IdentityProvider<? extends T>) exact);
        }
        for (Map.Entry<Class<?>, IdentityProvider<?>> entry : providers.entrySet()) {
            if (identityType.isAssignableFrom(entry.getKey())) {
                return Optional.of((IdentityProvider<? extends T>) entry.getValue());
            }
        }
        return Optional.empty();
    }

    /** Every registered provider, in registration order. */
    public List<IdentityProvider<?>> providers() {
        return List.copyOf(providers.values());
    }

    @Override
    public Builder toBuilder() {
        Builder builder = new Builder();
        builder.providers.putAll(providers);
        return builder;
    }

    @Override
    public String toString() {
        List<String> types = new ArrayList<>();
        providers.keySet().forEach(type -> types.add(type.getSimpleName()));
        return ToString.builder("IdentityProviders").add("identityTypes", types).build();
    }

    /** Builder of {@link IdentityProviders}. */
    public static final class Builder implements CopyableBuilder<Builder, IdentityProviders> {
        private final Map<Class<?>, IdentityProvider<?>> providers = new LinkedHashMap<>();

        private Builder() {
        }

        /** Registers {@code provider} for its identity type, replacing an earlier provider of that type. */
        public Builder putIdentityProvider(IdentityProvider<?> provider) {
            Objects.requireNonNull(provider, "provider");
            providers.put(Objects.requireNonNull(provider.identityType(), "identityType"), provider);
            return this;
        }

        @Override
        public IdentityProviders build() {
            return new IdentityProviders(this);
        }
    }
}
