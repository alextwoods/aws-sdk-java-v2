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

package software.amazon.awssdk.codecs.cbor;

import java.util.Objects;
import java.util.ServiceLoader;
import software.amazon.awssdk.annotations.SdkPublicApi;
import software.amazon.awssdk.codecs.cbor.internal.DefaultCborSerdeProvider;
import software.amazon.awssdk.codecs.commons.CodecSettings;

// CHECKSTYLE:OFF - Keeps the smithy-java (and design A.8) name; "Settings" is not a plural of a value type.
/**
 * Settings of the {@link Rpcv2CborCodec}.
 *
 * <p>The CBOR codec follows the Smithy RPCv2 CBOR specification: timestamps are always written and read as tag 1
 * epoch seconds, so {@link #useTimestampFormat()} and {@link #defaultTimestampFormat()} are ignored. The codec uses:
 * <ul>
 *     <li>{@link #defaultNamespace()}: the namespace of relative shape IDs in documents (none by default).</li>
 *     <li>{@link #forbidUnknownUnionMembers()}: whether a union member that is not in the union's schema is rejected
 *     instead of being reported as an unknown member (off by default). An unrecognized {@code __type} member of a
 *     union is always ignored.</li>
 *     <li>{@link #provider()}: the {@link CborSerdeProvider} that creates serializers and deserializers.</li>
 * </ul>
 *
 * <p>Settings are immutable. Port of smithy-java {@code software.amazon.smithy.java.cbor.CborSettings}, extending the
 * SDK {@link CodecSettings}.
 */
@SdkPublicApi
public final class CborSettings extends CodecSettings {
    // CHECKSTYLE:ON
    /**
     * The settings of the {@code smithy.protocols#rpcv2Cbor} protocol: the shared defaults and the default
     * {@link CborSerdeProvider}.
     */
    public static final CborSettings RPCV2 = builder().build();

    private static final Object PROVIDER_LOCK = new Object();
    private static volatile CborSerdeProvider discoveredProvider;

    // Null means the default provider, resolved on first use.
    private final CborSerdeProvider provider;

    private CborSettings(Builder builder) {
        super(builder);
        this.provider = builder.provider;
    }

    /**
     * Returns the default settings, {@link #RPCV2}.
     *
     * @return the default settings
     */
    public static CborSettings defaultSettings() {
        return RPCV2;
    }

    /**
     * Creates a builder with the default values.
     *
     * @return a new builder
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Returns the provider that creates CBOR serializers and deserializers: the one set on the builder, otherwise the
     * registered provider with the highest priority, otherwise the SDK's built-in provider.
     *
     * @return the serde provider
     */
    public CborSerdeProvider provider() {
        return provider != null ? provider : defaultProvider();
    }

    /**
     * Returns a builder holding these settings.
     *
     * @return a new builder
     */
    public Builder toBuilder() {
        return new Builder(this);
    }

    @Override
    public boolean equals(Object o) {
        if (!super.equals(o)) {
            return false;
        }
        return Objects.equals(provider, ((CborSettings) o).provider);
    }

    @Override
    public int hashCode() {
        return 31 * super.hashCode() + Objects.hashCode(provider);
    }

    @Override
    public String toString() {
        return "CborSettings(" + sharedSettingsToString() + ", provider=" + (provider == null ? "default" : provider)
               + ")";
    }

    private static CborSerdeProvider defaultProvider() {
        CborSerdeProvider result = discoveredProvider;
        if (result == null) {
            synchronized (PROVIDER_LOCK) {
                result = discoveredProvider;
                if (result == null) {
                    result = discoverProvider();
                    discoveredProvider = result;
                }
            }
        }
        return result;
    }

    private static CborSerdeProvider discoverProvider() {
        CborSerdeProvider selected = null;
        for (CborSerdeProvider candidate : ServiceLoader.load(CborSerdeProvider.class,
                                                              CborSerdeProvider.class.getClassLoader())) {
            if (selected == null || candidate.getPriority() > selected.getPriority()) {
                selected = candidate;
            }
        }
        return selected != null ? selected : new DefaultCborSerdeProvider();
    }

    /**
     * Builds {@link CborSettings}.
     */
    public static final class Builder extends CodecSettings.Builder<CborSettings, Builder> {
        private CborSerdeProvider provider;

        private Builder() {
        }

        private Builder(CborSettings settings) {
            super(settings);
            this.provider = settings.provider;
        }

        /**
         * Uses a custom CBOR serde provider instead of the default one.
         *
         * @param provider the provider to use
         * @return this builder
         * @throws NullPointerException if {@code provider} is null
         */
        Builder overrideSerdeProvider(CborSerdeProvider provider) {
            this.provider = Objects.requireNonNull(provider, "provider must not be null");
            return this;
        }

        /**
         * Copies every setting of {@code settings} into this builder.
         *
         * @param settings the settings to copy
         * @return this builder
         * @throws NullPointerException if {@code settings} is null
         */
        public Builder updateBuilder(CborSettings settings) {
            Objects.requireNonNull(settings, "settings must not be null");
            useTimestampFormat(settings.useTimestampFormat());
            defaultTimestampFormat(settings.defaultTimestampFormat());
            defaultNamespace(settings.defaultNamespace());
            forbidUnknownUnionMembers(settings.forbidUnknownUnionMembers());
            this.provider = settings.provider;
            return this;
        }

        @Override
        protected Builder self() {
            return this;
        }

        /**
         * Builds the settings.
         *
         * @return the settings
         */
        @Override
        public CborSettings build() {
            return new CborSettings(this);
        }
    }
}
