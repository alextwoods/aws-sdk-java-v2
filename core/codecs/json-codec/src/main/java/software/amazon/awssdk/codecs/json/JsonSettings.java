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

package software.amazon.awssdk.codecs.json;

import java.util.Objects;
import java.util.ServiceLoader;
import software.amazon.awssdk.annotations.SdkPublicApi;
import software.amazon.awssdk.codecs.commons.CodecSettings;
import software.amazon.awssdk.codecs.commons.TimestampFormatter;
import software.amazon.awssdk.codecs.json.internal.SmithyJsonSerdeProvider;

// CHECKSTYLE:OFF - Keeps the smithy-java (and design A.8) name; "Settings" is not a plural of a value type.
/**
 * Settings of the {@link JsonCodec}. In addition to the shared {@link CodecSettings}:
 * <ul>
 *     <li>{@link #useJsonName()}: whether member field names come from the {@code jsonName} trait (off by
 *     default).</li>
 *     <li>{@link #useTimestampFormat()} and {@link #defaultTimestampFormat()}: whether the {@code timestampFormat}
 *     trait is honored (off by default), and the format used otherwise ({@code epoch-seconds} by default).</li>
 *     <li>{@link #forbidUnknownUnionMembers()}: whether a union member that is not in the union's schema is rejected
 *     instead of being reported as an unknown member (off by default). A {@code __type} member of a union is always
 *     ignored.</li>
 *     <li>{@link #defaultNamespace()}: the namespace of relative shape IDs in documents (none by default).</li>
 *     <li>{@link #serializeTypeInDocuments()}: whether structure documents are written with a {@code __type} member
 *     (on by default).</li>
 *     <li>{@link #prettyPrint()}: whether output is indented (off by default).</li>
 *     <li>{@link #useStringForArbitraryPrecision()}: whether {@code bigInteger} and {@code bigDecimal} values are
 *     written and read as JSON strings instead of numbers (off by default).</li>
 *     <li>{@link #provider()}: the {@link JsonSerdeProvider} that creates serializers and deserializers.</li>
 * </ul>
 *
 * <p>Settings are immutable. {@link #REST_JSON_1} and {@link #AWS_JSON} are the presets of the AWS JSON protocols.
 *
 * <p>Port of smithy-java {@code software.amazon.smithy.java.json.JsonSettings}, extending the SDK
 * {@link CodecSettings}.
 */
@SdkPublicApi
public final class JsonSettings extends CodecSettings {
    // CHECKSTYLE:ON

    /**
     * The settings of the {@code aws.protocols#restJson1} protocol: {@code jsonName} honored, {@code timestampFormat}
     * honored with an {@code epoch-seconds} default.
     */
    public static final JsonSettings REST_JSON_1 = builder().useJsonName(true).useTimestampFormat(true).build();

    /**
     * The settings of the {@code aws.protocols#awsJson1_0} and {@code aws.protocols#awsJson1_1} protocols:
     * {@code jsonName} ignored, {@code timestampFormat} honored with an {@code epoch-seconds} default.
     */
    public static final JsonSettings AWS_JSON = builder().useJsonName(false).useTimestampFormat(true).build();

    private static final Object PROVIDER_LOCK = new Object();
    private static volatile JsonSerdeProvider discoveredProvider;

    private final boolean useJsonName;
    private final boolean serializeTypeInDocuments;
    private final boolean prettyPrint;
    private final boolean useStringForArbitraryPrecision;
    // Null means the default provider, resolved on first use.
    private final JsonSerdeProvider provider;
    private final TimestampResolver timestampResolver;
    private final JsonFieldMapper fieldMapper;

    private JsonSettings(Builder builder) {
        super(builder);
        this.useJsonName = builder.useJsonName;
        this.serializeTypeInDocuments = builder.serializeTypeInDocuments;
        this.prettyPrint = builder.prettyPrint;
        this.useStringForArbitraryPrecision = builder.useStringForArbitraryPrecision;
        this.provider = builder.provider;
        TimestampFormatter defaultFormat = TimestampFormatter.of(defaultTimestampFormat());
        this.timestampResolver = useTimestampFormat()
                                 ? new TimestampResolver.UseTimestampFormatTrait(defaultFormat)
                                 : new TimestampResolver.StaticFormat(defaultFormat);
        this.fieldMapper = useJsonName ? JsonFieldMapper.UseJsonNameTrait.INSTANCE
                                       : JsonFieldMapper.UseMemberName.INSTANCE;
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
     * Returns whether member field names come from the {@code jsonName} trait.
     *
     * @return whether {@code jsonName} is honored
     */
    public boolean useJsonName() {
        return useJsonName;
    }

    /**
     * Returns the resolver of timestamp formats selected by {@link #useTimestampFormat()} and
     * {@link #defaultTimestampFormat()}.
     *
     * @return the timestamp resolver
     */
    public TimestampResolver timestampResolver() {
        return timestampResolver;
    }

    /**
     * Returns the field mapper selected by {@link #useJsonName()}.
     *
     * @return the field mapper
     */
    public JsonFieldMapper fieldMapper() {
        return fieldMapper;
    }

    /**
     * Returns whether structure documents are written with a {@code __type} member.
     *
     * @return whether the type is written in documents
     */
    public boolean serializeTypeInDocuments() {
        return serializeTypeInDocuments;
    }

    /**
     * Returns whether output is indented.
     *
     * @return whether output is pretty-printed
     */
    public boolean prettyPrint() {
        return prettyPrint;
    }

    /**
     * Returns whether {@code bigInteger} and {@code bigDecimal} values are written and read as JSON strings.
     *
     * @return whether arbitrary-precision numbers are strings
     */
    public boolean useStringForArbitraryPrecision() {
        return useStringForArbitraryPrecision;
    }

    /**
     * Returns the provider that creates JSON serializers and deserializers: the one set on the builder, otherwise the
     * registered provider with the highest priority, otherwise the SDK's built-in provider.
     *
     * @return the serde provider
     */
    public JsonSerdeProvider provider() {
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
        JsonSettings that = (JsonSettings) o;
        return useJsonName == that.useJsonName
               && serializeTypeInDocuments == that.serializeTypeInDocuments
               && prettyPrint == that.prettyPrint
               && useStringForArbitraryPrecision == that.useStringForArbitraryPrecision
               && Objects.equals(provider, that.provider);
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + Boolean.hashCode(useJsonName);
        result = 31 * result + Boolean.hashCode(serializeTypeInDocuments);
        result = 31 * result + Boolean.hashCode(prettyPrint);
        result = 31 * result + Boolean.hashCode(useStringForArbitraryPrecision);
        result = 31 * result + Objects.hashCode(provider);
        return result;
    }

    @Override
    public String toString() {
        return "JsonSettings(" + sharedSettingsToString()
               + ", useJsonName=" + useJsonName
               + ", serializeTypeInDocuments=" + serializeTypeInDocuments
               + ", prettyPrint=" + prettyPrint
               + ", useStringForArbitraryPrecision=" + useStringForArbitraryPrecision
               + ", provider=" + (provider == null ? "default" : provider) + ")";
    }

    private static JsonSerdeProvider defaultProvider() {
        JsonSerdeProvider result = discoveredProvider;
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

    private static JsonSerdeProvider discoverProvider() {
        JsonSerdeProvider selected = null;
        for (JsonSerdeProvider candidate : ServiceLoader.load(JsonSerdeProvider.class,
                                                              JsonSerdeProvider.class.getClassLoader())) {
            if (selected == null || candidate.getPriority() > selected.getPriority()) {
                selected = candidate;
            }
        }
        return selected != null ? selected : new SmithyJsonSerdeProvider();
    }

    /**
     * Builds {@link JsonSettings}.
     */
    public static final class Builder extends CodecSettings.Builder<JsonSettings, Builder> {
        private boolean useJsonName;
        private boolean serializeTypeInDocuments = true;
        private boolean prettyPrint;
        private boolean useStringForArbitraryPrecision;
        private JsonSerdeProvider provider;

        private Builder() {
        }

        private Builder(JsonSettings settings) {
            super(settings);
            copyJsonSettings(settings);
        }

        /**
         * Sets whether member field names come from the {@code jsonName} trait. Off by default.
         *
         * @param useJsonName whether {@code jsonName} is honored
         * @return this builder
         */
        public Builder useJsonName(boolean useJsonName) {
            this.useJsonName = useJsonName;
            return this;
        }

        /**
         * Sets whether structure documents are written with a {@code __type} member. On by default.
         *
         * @param serializeTypeInDocuments whether the type is written in documents
         * @return this builder
         */
        public Builder serializeTypeInDocuments(boolean serializeTypeInDocuments) {
            this.serializeTypeInDocuments = serializeTypeInDocuments;
            return this;
        }

        /**
         * Sets whether output is indented. Off by default.
         *
         * @param prettyPrint whether output is pretty-printed
         * @return this builder
         */
        public Builder prettyPrint(boolean prettyPrint) {
            this.prettyPrint = prettyPrint;
            return this;
        }

        /**
         * Sets whether {@code bigInteger} and {@code bigDecimal} values are written and read as JSON strings. Off by
         * default.
         *
         * @param useStringForArbitraryPrecision whether arbitrary-precision numbers are strings
         * @return this builder
         */
        public Builder useStringForArbitraryPrecision(boolean useStringForArbitraryPrecision) {
            this.useStringForArbitraryPrecision = useStringForArbitraryPrecision;
            return this;
        }

        /**
         * Uses a custom JSON serde provider instead of the default one.
         *
         * @param provider the provider to use
         * @return this builder
         * @throws NullPointerException if {@code provider} is null
         */
        Builder overrideSerdeProvider(JsonSerdeProvider provider) {
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
        public Builder updateBuilder(JsonSettings settings) {
            Objects.requireNonNull(settings, "settings must not be null");
            useTimestampFormat(settings.useTimestampFormat());
            defaultTimestampFormat(settings.defaultTimestampFormat());
            defaultNamespace(settings.defaultNamespace());
            forbidUnknownUnionMembers(settings.forbidUnknownUnionMembers());
            copyJsonSettings(settings);
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
        public JsonSettings build() {
            return new JsonSettings(this);
        }

        private void copyJsonSettings(JsonSettings settings) {
            this.useJsonName = settings.useJsonName;
            this.serializeTypeInDocuments = settings.serializeTypeInDocuments;
            this.prettyPrint = settings.prettyPrint;
            this.useStringForArbitraryPrecision = settings.useStringForArbitraryPrecision;
            this.provider = settings.provider;
        }
    }
}
