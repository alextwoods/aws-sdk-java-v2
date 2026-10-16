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

package software.amazon.awssdk.codecs.xml;

import java.util.Objects;
import software.amazon.awssdk.annotations.SdkPublicApi;
import software.amazon.awssdk.codecs.commons.CodecSettings;
import software.amazon.awssdk.schema.traits.TimestampFormatTrait;
import software.amazon.awssdk.schema.traits.XmlNamespaceTrait;

// CHECKSTYLE:OFF - Keeps the design A.8 name; "Settings" is not a plural of a value type.
/**
 * Settings of the {@link XmlCodec}. In addition to the shared {@link CodecSettings}:
 * <ul>
 *     <li>{@link #useXmlName()}: whether element and attribute names come from the {@code xmlName} trait (on by
 *     default). When off, members use their member names and shapes their shape names.</li>
 *     <li>{@link #useTimestampFormat()} and {@link #defaultTimestampFormat()}: whether the {@code timestampFormat}
 *     trait is honored (on by default), and the format used otherwise ({@code date-time} by default).</li>
 *     <li>{@link #defaultNamespace()} and {@link #defaultNamespacePrefix()}: the XML namespace URI, and its optional
 *     prefix, declared on the root element of a serialized shape that has no {@code xmlNamespace} trait (none by
 *     default).</li>
 *     <li>{@link #forbidUnknownUnionMembers()}: whether an element that is not a member of the union being read is
 *     rejected instead of being reported as an unknown member (off by default).</li>
 * </ul>
 *
 * <p>Settings are immutable. {@link #REST_XML} and {@link #AWS_QUERY_RESPONSE} are the presets of the AWS XML
 * protocols.
 *
 * <p>This type is an SDK addition: smithy-java configures its XML codec with builder fields only. It gives the XML
 * codec the same settings shape as the JSON and CBOR codecs.
 */
@SdkPublicApi
public final class XmlSettings extends CodecSettings {
    // CHECKSTYLE:ON

    /**
     * The settings of the {@code aws.protocols#restXml} protocol: {@code xmlName} honored, {@code timestampFormat}
     * honored with a {@code date-time} default. The protocol sets the service's XML namespace as the default namespace.
     */
    public static final XmlSettings REST_XML = builder().build();

    /**
     * The settings of the XML responses of the {@code aws.protocols#awsQuery} and {@code aws.protocols#ec2Query}
     * protocols: {@code xmlName} honored, {@code timestampFormat} honored with a {@code date-time} default.
     */
    public static final XmlSettings AWS_QUERY_RESPONSE = builder().build();

    private final boolean useXmlName;
    private final String defaultNamespacePrefix;

    private XmlSettings(Builder builder) {
        super(builder);
        if (builder.defaultNamespacePrefix != null && defaultNamespace() == null) {
            throw new IllegalArgumentException("defaultNamespacePrefix requires a defaultNamespace");
        }
        this.useXmlName = builder.useXmlName;
        this.defaultNamespacePrefix = builder.defaultNamespacePrefix;
    }

    /**
     * Creates a builder with the default values: {@code xmlName} and {@code timestampFormat} honored, {@code date-time}
     * default timestamp format, no default namespace, unknown union members reported.
     *
     * @return a new builder
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Returns whether element and attribute names come from the {@code xmlName} trait.
     *
     * @return whether {@code xmlName} is honored
     */
    public boolean useXmlName() {
        return useXmlName;
    }

    /**
     * Returns the prefix of the {@link #defaultNamespace()}, or {@code null} to declare it as the default
     * ({@code xmlns="..."}) namespace.
     *
     * @return the namespace prefix, or {@code null}
     */
    public String defaultNamespacePrefix() {
        return defaultNamespacePrefix;
    }

    /**
     * Returns the {@link #defaultNamespace()} and {@link #defaultNamespacePrefix()} as an {@code xmlNamespace} trait, or
     * {@code null} if no default namespace is set.
     *
     * @return the default namespace, or {@code null}
     */
    public XmlNamespaceTrait defaultXmlNamespace() {
        String uri = defaultNamespace();
        return uri == null ? null : new XmlNamespaceTrait(uri, defaultNamespacePrefix);
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
        XmlSettings that = (XmlSettings) o;
        return useXmlName == that.useXmlName && Objects.equals(defaultNamespacePrefix, that.defaultNamespacePrefix);
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + Boolean.hashCode(useXmlName);
        result = 31 * result + Objects.hashCode(defaultNamespacePrefix);
        return result;
    }

    @Override
    public String toString() {
        return "XmlSettings(" + sharedSettingsToString()
               + ", useXmlName=" + useXmlName
               + ", defaultNamespacePrefix=" + defaultNamespacePrefix + ")";
    }

    /**
     * Builds {@link XmlSettings}.
     */
    public static final class Builder extends CodecSettings.Builder<XmlSettings, Builder> {
        private boolean useXmlName = true;
        private String defaultNamespacePrefix;

        private Builder() {
            useTimestampFormat(true);
            defaultTimestampFormat(TimestampFormatTrait.Format.DATE_TIME);
        }

        private Builder(XmlSettings settings) {
            super(settings);
            copyXmlSettings(settings);
        }

        /**
         * Sets whether element and attribute names come from the {@code xmlName} trait. On by default.
         *
         * @param useXmlName whether {@code xmlName} is honored
         * @return this builder
         */
        public Builder useXmlName(boolean useXmlName) {
            this.useXmlName = useXmlName;
            return this;
        }

        /**
         * Sets the prefix of the {@link #defaultNamespace(String) default namespace}. None by default, which declares
         * the default ({@code xmlns="..."}) namespace.
         *
         * @param defaultNamespacePrefix the namespace prefix, or {@code null} for none
         * @return this builder
         */
        public Builder defaultNamespacePrefix(String defaultNamespacePrefix) {
            this.defaultNamespacePrefix = defaultNamespacePrefix;
            return this;
        }

        /**
         * Sets the default namespace URI and prefix from an {@code xmlNamespace} trait.
         *
         * @param defaultNamespace the namespace, or {@code null} to clear the default namespace and its prefix
         * @return this builder
         */
        public Builder defaultXmlNamespace(XmlNamespaceTrait defaultNamespace) {
            if (defaultNamespace == null) {
                defaultNamespace((String) null);
                this.defaultNamespacePrefix = null;
            } else {
                defaultNamespace(defaultNamespace.getUri());
                this.defaultNamespacePrefix = defaultNamespace.getPrefix().orElse(null);
            }
            return this;
        }

        /**
         * Copies every setting of {@code settings} into this builder.
         *
         * @param settings the settings to copy
         * @return this builder
         * @throws NullPointerException if {@code settings} is null
         */
        public Builder updateBuilder(XmlSettings settings) {
            Objects.requireNonNull(settings, "settings must not be null");
            useTimestampFormat(settings.useTimestampFormat());
            defaultTimestampFormat(settings.defaultTimestampFormat());
            defaultNamespace(settings.defaultNamespace());
            forbidUnknownUnionMembers(settings.forbidUnknownUnionMembers());
            copyXmlSettings(settings);
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
         * @throws IllegalArgumentException if a namespace prefix is set without a namespace
         */
        @Override
        public XmlSettings build() {
            return new XmlSettings(this);
        }

        private void copyXmlSettings(XmlSettings settings) {
            this.useXmlName = settings.useXmlName;
            this.defaultNamespacePrefix = settings.defaultNamespacePrefix;
        }
    }
}
