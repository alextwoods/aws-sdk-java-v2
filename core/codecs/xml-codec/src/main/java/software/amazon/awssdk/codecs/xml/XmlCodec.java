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

import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import software.amazon.awssdk.annotations.SdkPublicApi;
import software.amazon.awssdk.codecs.commons.Codec;
import software.amazon.awssdk.codecs.xml.internal.XmlSerdeFactory;
import software.amazon.awssdk.schema.SerializableShape;
import software.amazon.awssdk.schema.ShapeBuilder;
import software.amazon.awssdk.schema.serde.SerializationException;
import software.amazon.awssdk.schema.serde.ShapeDeserializer;
import software.amazon.awssdk.schema.serde.ShapeSerializer;
import software.amazon.awssdk.schema.traits.TimestampFormatTrait;
import software.amazon.awssdk.schema.traits.XmlNamespaceTrait;

/**
 * A {@link Codec} for XML that honors the {@code xmlName}, {@code xmlAttribute}, {@code xmlFlattened} and
 * {@code xmlNamespace} traits:
 * <ul>
 *     <li>A shape is written as an element named by its {@code xmlName} trait or its shape name; members as child
 *     elements (or attributes, with {@code xmlAttribute}) named by their {@code xmlName} trait or member name. No XML
 *     declaration is written unless {@link Builder#xmlDeclaration(boolean)} is set.</li>
 *     <li>Lists wrap each item in a {@code <member>} element (or the list member's {@code xmlName}); maps wrap each
 *     entry in {@code <entry>} with {@code <key>} and {@code <value>} children. With {@code xmlFlattened}, the items or
 *     entries are written directly in the containing structure, named by the member. On read, flattened items may be
 *     interleaved with other members.</li>
 *     <li>Numbers are written in decimal; {@code float} and {@code double} values in their shortest form (an integral
 *     value without a fraction), and {@code NaN}, {@code Infinity} and {@code -Infinity} as those words. Blobs are
 *     base64 text. Timestamps follow {@link XmlSettings#useTimestampFormat()} and
 *     {@link XmlSettings#defaultTimestampFormat()} ({@code date-time} by default).</li>
 *     <li>Text escapes {@code &}, {@code <}, {@code >} and carriage returns; attribute values also escape quotes,
 *     apostrophes, tabs and line feeds.</li>
 *     <li>On read, unknown elements are skipped, an empty body reads as a structure with no members set, and nothing
 *     but whitespace may follow the root element. A root {@code <ErrorResponse>}, {@code <Response>} or {@code <Error>}
 *     element is unwrapped down to the error's members. Documents are not supported: they are not written and read as
 *     {@code null}.</li>
 * </ul>
 *
 * <p>The reader processes no DTDs and resolves no external entities, so XML External Entity (XXE) and entity expansion
 * attacks are not possible.
 *
 * <p>Malformed input and values that do not match the target schema throw {@link SerializationException}. The codec is
 * immutable and thread-safe; the serializers and deserializers it creates are single-use.
 *
 * <p>Port of smithy-java {@code software.amazon.smithy.java.xml.XmlCodec}, configured with {@link XmlSettings}.
 */
@SdkPublicApi
public final class XmlCodec implements Codec {

    private static final byte[] XML_DECLARATION =
        "<?xml version=\"1.0\" encoding=\"UTF-8\"?>".getBytes(StandardCharsets.UTF_8);

    private final XmlSettings settings;
    private final XmlSerdeFactory factory;
    private final boolean xmlDeclaration;

    private XmlCodec(Builder builder) {
        this.settings = builder.settings.build();
        this.xmlDeclaration = builder.xmlDeclaration;
        this.factory = new XmlSerdeFactory(settings, builder.wrapperElements, builder.strictRootElement,
                                           builder.useNative);
    }

    /**
     * Creates a builder. A codec built with no settings uses the {@link XmlSettings#builder()} defaults, which are the
     * {@link XmlSettings#REST_XML} values.
     *
     * @return a new builder
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Returns the settings of this codec.
     *
     * @return the settings
     */
    public XmlSettings settings() {
        return settings;
    }

    /**
     * Returns the top-level wrapper elements this codec skips when deserializing.
     *
     * @return the wrapper element names, in order; empty if none
     */
    public List<String> wrapperElements() {
        return factory.wrapperElements();
    }

    /**
     * Returns whether this codec rejects a root element whose name does not match the deserialized shape.
     *
     * @return whether the root element name must match
     */
    public boolean strictRootElement() {
        return factory.strictRootElement();
    }

    /**
     * Returns whether {@link #createSerializer(OutputStream)} writes an XML declaration.
     *
     * @return whether an XML declaration is written
     */
    public boolean xmlDeclaration() {
        return xmlDeclaration;
    }

    /**
     * Creates a serializer that writes to {@code sink}. With {@link Builder#xmlDeclaration(boolean)}, the declaration
     * {@code <?xml version="1.0" encoding="UTF-8"?>} is written to {@code sink} first, when the serializer is created.
     *
     * @throws SerializationException if the declaration cannot be written to {@code sink}
     */
    @Override
    public ShapeSerializer createSerializer(OutputStream sink) {
        if (xmlDeclaration) {
            try {
                sink.write(XML_DECLARATION);
            } catch (IOException e) {
                throw new SerializationException("Unable to write the XML declaration", e);
            }
        }
        return factory.createSerializer(sink);
    }

    /**
     * Creates a deserializer that reads one value from the remaining bytes of {@code source}. A buffer with an
     * accessible array is read without copying; the buffer's position is not changed. An empty buffer reads as an
     * empty document: structures, lists and maps are empty, and scalars throw.
     *
     * @param source the XML bytes; the caller must not modify them while the deserializer is in use
     * @return a new deserializer
     */
    @Override
    public ShapeDeserializer createDeserializer(ByteBuffer source) {
        return factory.createDeserializer(source);
    }

    /**
     * Deserializes one shape from {@code source} and closes the deserializer.
     *
     * @throws NullPointerException if an argument is null
     * @throws SerializationException if the input is malformed, does not match the shape, or has trailing content
     */
    @Override
    public <T extends SerializableShape> T deserializeShape(byte[] source, ShapeBuilder<T> builder) {
        Objects.requireNonNull(source, "source must not be null");
        Objects.requireNonNull(builder, "builder must not be null");
        return deserializeAndClose(createDeserializer(source), builder);
    }

    /**
     * Deserializes one shape from the remaining bytes of {@code source} and closes the deserializer.
     *
     * @throws NullPointerException if an argument is null
     * @throws SerializationException if the input is malformed, does not match the shape, or has trailing content
     */
    @Override
    public <T extends SerializableShape> T deserializeShape(ByteBuffer source, ShapeBuilder<T> builder) {
        Objects.requireNonNull(source, "source must not be null");
        Objects.requireNonNull(builder, "builder must not be null");
        return deserializeAndClose(createDeserializer(source), builder);
    }

    @Override
    public String toString() {
        return "XmlCodec(" + settings + ", wrapperElements=" + factory.wrapperElements()
               + ", strictRootElement=" + factory.strictRootElement() + ", xmlDeclaration=" + xmlDeclaration + ")";
    }

    private static <T extends SerializableShape> T deserializeAndClose(ShapeDeserializer deserializer,
                                                                       ShapeBuilder<T> builder) {
        T result;
        try {
            result = builder.deserialize(deserializer).errorCorrection().build();
        } catch (RuntimeException e) {
            closeQuietly(deserializer, e);
            throw e;
        }
        deserializer.close();
        return result;
    }

    private static void closeQuietly(ShapeDeserializer deserializer, RuntimeException failure) {
        try {
            deserializer.close();
        } catch (RuntimeException closeFailure) {
            // Don't hide the original deserialization exception.
            failure.addSuppressed(closeFailure);
        }
    }

    /**
     * Builds an {@link XmlCodec}.
     */
    public static final class Builder {
        private final XmlSettings.Builder settings = XmlSettings.builder();
        private List<String> wrapperElements = Collections.emptyList();
        private boolean strictRootElement = true;
        private boolean xmlDeclaration;
        private boolean useNative = true;

        private Builder() {
        }

        /**
         * Copies every value of {@code settings} into this builder. Later calls to the other settings setters change
         * only the value they set.
         *
         * @param settings the settings to use
         * @return this builder
         * @throws NullPointerException if {@code settings} is null
         */
        public Builder settings(XmlSettings settings) {
            this.settings.updateBuilder(settings);
            return this;
        }

        /**
         * Sets whether element and attribute names come from the {@code xmlName} trait. On by default.
         *
         * @param useXmlName whether {@code xmlName} is honored
         * @return this builder
         */
        public Builder useXmlName(boolean useXmlName) {
            settings.useXmlName(useXmlName);
            return this;
        }

        /**
         * Sets whether the {@code timestampFormat} trait is honored. On by default.
         *
         * @param useTimestampFormat whether the trait is honored
         * @return this builder
         */
        public Builder useTimestampFormat(boolean useTimestampFormat) {
            settings.useTimestampFormat(useTimestampFormat);
            return this;
        }

        /**
         * Sets the timestamp format used when the {@code timestampFormat} trait is absent or ignored. Defaults to
         * {@code date-time}.
         *
         * @param defaultTimestampFormat the default format
         * @return this builder
         * @throws NullPointerException if {@code defaultTimestampFormat} is null
         * @throws IllegalArgumentException if the format is {@link TimestampFormatTrait.Format#UNKNOWN}
         */
        public Builder defaultTimestampFormat(TimestampFormatTrait.Format defaultTimestampFormat) {
            settings.defaultTimestampFormat(defaultTimestampFormat);
            return this;
        }

        /**
         * Sets whether an element that is not a member of the union being read is rejected with a
         * {@link SerializationException} instead of being reported as an unknown member. Off by default.
         *
         * @param forbid whether unknown union members are rejected
         * @return this builder
         */
        public Builder forbidUnknownUnionMembers(boolean forbid) {
            settings.forbidUnknownUnionMembers(forbid);
            return this;
        }

        /**
         * Sets the XML namespace declared on the root element of a serialized shape that has no {@code xmlNamespace}
         * trait. None unless set.
         *
         * @param defaultNamespace the default namespace, or {@code null} for none
         * @return this builder
         */
        public Builder defaultNamespace(XmlNamespaceTrait defaultNamespace) {
            settings.defaultXmlNamespace(defaultNamespace);
            return this;
        }

        /**
         * Configures wrapper elements to skip during deserialization.
         *
         * <p>When deserializing, these elements are skipped in order at the top level only before reading the actual
         * content. This is useful for protocols like AWS Query where responses are wrapped in elements like
         * {@code <OperationNameResponse>} and {@code <OperationNameResult>}.
         *
         * <p>The elements must match exactly (not by suffix) and are only skipped at the top level, not for nested
         * structures. None by default.
         *
         * @param wrapperElements the list of wrapper element names to skip, in order
         * @return this builder
         * @throws NullPointerException if {@code wrapperElements} or one of its elements is null
         */
        public Builder wrapperElements(List<String> wrapperElements) {
            Objects.requireNonNull(wrapperElements, "wrapperElements must not be null");
            List<String> copy = new ArrayList<>(wrapperElements.size());
            for (String element : wrapperElements) {
                copy.add(Objects.requireNonNull(element, "wrapperElements must not contain null"));
            }
            this.wrapperElements = copy;
            return this;
        }

        /**
         * Sets whether the root element name must match the deserialized shape's expected name.
         *
         * <p>When {@code true} (default), a mismatched root element throws. When {@code false}, the mismatch is
         * tolerated and the children are read regardless; use this for clients, where a service may return a root
         * name that differs from the modeled output shape (e.g. S3's {@code <CopyObjectResult>} for
         * {@code CopyObjectOutput}).
         *
         * @param strictRootElement true to require an exact root element name, false to tolerate a mismatch
         * @return this builder
         */
        public Builder strictRootElement(boolean strictRootElement) {
            this.strictRootElement = strictRootElement;
            return this;
        }

        /**
         * Sets whether each serializer starts its output with the XML declaration
         * {@code <?xml version="1.0" encoding="UTF-8"?>}. Off by default; the earlier SDK restXml marshallers send it.
         *
         * @param xmlDeclaration true to write the declaration
         * @return this builder
         */
        public Builder xmlDeclaration(boolean xmlDeclaration) {
            this.xmlDeclaration = xmlDeclaration;
            return this;
        }

        /**
         * Selects the native implementation ({@code true}, the default) or the StAX ({@code javax.xml.stream})
         * implementation. For tests.
         */
        Builder useNative(boolean useNative) {
            this.useNative = useNative;
            return this;
        }

        /**
         * Builds the codec.
         *
         * @return the codec
         * @throws IllegalArgumentException if a namespace prefix is set without a namespace
         */
        public XmlCodec build() {
            return new XmlCodec(this);
        }
    }
}
