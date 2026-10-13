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

package software.amazon.awssdk.codecs.commons;

import java.util.Objects;
import software.amazon.awssdk.annotations.SdkPublicApi;
import software.amazon.awssdk.schema.traits.TimestampFormatTrait;

/**
 * The settings shared by every {@link Codec}: how timestamps are formatted, the default namespace, and how unknown union
 * members are handled. Each codec extends this class with its own settings (for example whether the {@code jsonName} or
 * {@code xmlName} trait is honored) and protocol presets.
 *
 * <p>Settings are immutable. Two settings objects are equal when they have the same class and the same values.
 *
 * <p>This type is an SDK addition. smithy-java declares these settings separately on each codec's settings or builder
 * type.
 */
@SdkPublicApi
public abstract class CodecSettings {
    private final boolean useTimestampFormat;
    private final TimestampFormatTrait.Format defaultTimestampFormat;
    private final String defaultNamespace;
    private final boolean forbidUnknownUnionMembers;

    /**
     * Creates settings from the values of a builder.
     *
     * @param builder the builder holding the values
     */
    protected CodecSettings(Builder<?, ?> builder) {
        this.useTimestampFormat = builder.useTimestampFormat;
        this.defaultTimestampFormat = builder.defaultTimestampFormat;
        this.defaultNamespace = builder.defaultNamespace;
        this.forbidUnknownUnionMembers = builder.forbidUnknownUnionMembers;
    }

    /**
     * Returns whether a {@code timestampFormat} trait on a member or its target overrides
     * {@link #defaultTimestampFormat()}. The default is {@code false}.
     *
     * @return whether the {@code timestampFormat} trait is honored
     */
    public boolean useTimestampFormat() {
        return useTimestampFormat;
    }

    /**
     * Returns the format of timestamps that have no honored {@code timestampFormat} trait. The default is
     * {@link TimestampFormatTrait.Format#EPOCH_SECONDS}. Never {@link TimestampFormatTrait.Format#UNKNOWN}.
     *
     * @return the default timestamp format
     */
    public TimestampFormatTrait.Format defaultTimestampFormat() {
        return defaultTimestampFormat;
    }

    /**
     * Returns the default namespace, or {@code null} if none is set (the default). Each codec documents how it uses the
     * value: for example, the namespace used to resolve relative shape names in documents, or the default XML
     * namespace.
     *
     * @return the default namespace, or {@code null}
     */
    public String defaultNamespace() {
        return defaultNamespace;
    }

    /**
     * Returns whether a union member that is not in the union's schema is rejected with a serialization error. When
     * {@code false} (the default), it is kept as an unknown variant.
     *
     * @return whether unknown union members are rejected
     */
    public boolean forbidUnknownUnionMembers() {
        return forbidUnknownUnionMembers;
    }

    /**
     * Returns whether {@code o} has the same class and the same shared settings. Subclasses that add settings must
     * override this method, call it, and compare their own settings.
     *
     * @param o the object to compare with
     * @return whether the objects are equal
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        CodecSettings that = (CodecSettings) o;
        return useTimestampFormat == that.useTimestampFormat
               && forbidUnknownUnionMembers == that.forbidUnknownUnionMembers
               && defaultTimestampFormat == that.defaultTimestampFormat
               && Objects.equals(defaultNamespace, that.defaultNamespace);
    }

    /**
     * Returns a hash code of the shared settings. Subclasses that add settings must override this method and combine its
     * result with their own settings.
     *
     * @return the hash code
     */
    @Override
    public int hashCode() {
        int result = Boolean.hashCode(useTimestampFormat);
        result = 31 * result + defaultTimestampFormat.hashCode();
        result = 31 * result + Objects.hashCode(defaultNamespace);
        result = 31 * result + Boolean.hashCode(forbidUnknownUnionMembers);
        return result;
    }

    /**
     * Returns the shared settings as {@code useTimestampFormat=..., defaultTimestampFormat=..., defaultNamespace=...,
     * forbidUnknownUnionMembers=...}, for use in a subclass {@code toString}.
     *
     * @return the shared settings as text
     */
    protected final String sharedSettingsToString() {
        return "useTimestampFormat=" + useTimestampFormat
               + ", defaultTimestampFormat=" + defaultTimestampFormat
               + ", defaultNamespace=" + defaultNamespace
               + ", forbidUnknownUnionMembers=" + forbidUnknownUnionMembers;
    }

    /**
     * Returns the simple class name followed by the settings in parentheses.
     *
     * @return the settings as text
     */
    @Override
    public String toString() {
        return getClass().getSimpleName() + "(" + sharedSettingsToString() + ")";
    }

    /**
     * Builds a {@link CodecSettings} subclass. Codec settings builders extend this class.
     *
     * @param <S> the settings type built
     * @param <B> the concrete builder type, returned by the setters
     */
    public abstract static class Builder<S extends CodecSettings, B extends Builder<S, B>> {
        private boolean useTimestampFormat;
        private TimestampFormatTrait.Format defaultTimestampFormat = TimestampFormatTrait.Format.EPOCH_SECONDS;
        private String defaultNamespace;
        private boolean forbidUnknownUnionMembers;

        /**
         * Creates a builder with the default values.
         */
        protected Builder() {
        }

        /**
         * Creates a builder holding the shared values of {@code settings}.
         *
         * @param settings the settings to copy
         * @throws NullPointerException if {@code settings} is null
         */
        protected Builder(CodecSettings settings) {
            Objects.requireNonNull(settings, "settings must not be null");
            this.useTimestampFormat = settings.useTimestampFormat;
            this.defaultTimestampFormat = settings.defaultTimestampFormat;
            this.defaultNamespace = settings.defaultNamespace;
            this.forbidUnknownUnionMembers = settings.forbidUnknownUnionMembers;
        }

        /**
         * Sets whether the {@code timestampFormat} trait overrides the default timestamp format.
         *
         * @param useTimestampFormat whether the trait is honored
         * @return this builder
         */
        public B useTimestampFormat(boolean useTimestampFormat) {
            this.useTimestampFormat = useTimestampFormat;
            return self();
        }

        /**
         * Sets the format of timestamps that have no honored {@code timestampFormat} trait.
         *
         * @param defaultTimestampFormat the default format
         * @return this builder
         * @throws NullPointerException if {@code defaultTimestampFormat} is null
         * @throws IllegalArgumentException if {@code defaultTimestampFormat} is
         *     {@link TimestampFormatTrait.Format#UNKNOWN}
         */
        public B defaultTimestampFormat(TimestampFormatTrait.Format defaultTimestampFormat) {
            Objects.requireNonNull(defaultTimestampFormat, "defaultTimestampFormat must not be null");
            if (defaultTimestampFormat == TimestampFormatTrait.Format.UNKNOWN) {
                throw new IllegalArgumentException("defaultTimestampFormat must be a known timestamp format");
            }
            this.defaultTimestampFormat = defaultTimestampFormat;
            return self();
        }

        /**
         * Sets the default namespace.
         *
         * @param defaultNamespace the default namespace, or {@code null} for none
         * @return this builder
         */
        public B defaultNamespace(String defaultNamespace) {
            this.defaultNamespace = defaultNamespace;
            return self();
        }

        /**
         * Sets whether a union member that is not in the union's schema is rejected.
         *
         * @param forbidUnknownUnionMembers whether unknown union members are rejected
         * @return this builder
         */
        public B forbidUnknownUnionMembers(boolean forbidUnknownUnionMembers) {
            this.forbidUnknownUnionMembers = forbidUnknownUnionMembers;
            return self();
        }

        /**
         * Returns this builder as its concrete type.
         *
         * @return this builder
         */
        protected abstract B self();

        /**
         * Builds the settings.
         *
         * @return the settings
         */
        public abstract S build();
    }
}
