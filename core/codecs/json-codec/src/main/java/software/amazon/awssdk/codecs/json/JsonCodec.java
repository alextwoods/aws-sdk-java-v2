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

import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.util.Objects;
import software.amazon.awssdk.annotations.SdkPublicApi;
import software.amazon.awssdk.codecs.commons.Codec;
import software.amazon.awssdk.schema.SerializableShape;
import software.amazon.awssdk.schema.ShapeBuilder;
import software.amazon.awssdk.schema.serde.SerializationException;
import software.amazon.awssdk.schema.serde.ShapeDeserializer;
import software.amazon.awssdk.schema.serde.ShapeSerializer;
import software.amazon.awssdk.schema.traits.TimestampFormatTrait;

/**
 * A {@link Codec} for JSON (RFC 8259), with a hand-written byte-level reader and writer and no Jackson dependency.
 * <ul>
 *     <li>Numbers are written in their shortest form; a {@code float} or {@code double} holding an integer is written
 *     as that integer. {@code NaN} and the infinities are written and read as the strings {@code "NaN"},
 *     {@code "Infinity"} and {@code "-Infinity"}.</li>
 *     <li>{@code bigInteger} and {@code bigDecimal} values are JSON numbers in plain notation, or strings with
 *     {@link JsonSettings#useStringForArbitraryPrecision()}.</li>
 *     <li>Blobs are base64 strings. Timestamps follow {@link JsonSettings#timestampResolver()}: {@code epoch-seconds}
 *     as a number (with up to nine fraction digits), {@code date-time} and {@code http-date} as strings.</li>
 *     <li>On read, null structure members are treated as absent, unknown members are skipped, and a JSON number read
 *     for a {@code string} member returns the number's literal text.</li>
 * </ul>
 *
 * <p>Malformed input and values that do not match the target schema throw {@link SerializationException}. The codec is
 * immutable and thread-safe; the serializers and deserializers it creates are single-use.
 *
 * <p>Port of smithy-java {@code software.amazon.smithy.java.json.JsonCodec}.
 */
@SdkPublicApi
public final class JsonCodec implements Codec {

    private final JsonSettings settings;

    private JsonCodec(Builder builder) {
        this.settings = builder.settings.build();
    }

    /**
     * Creates a builder. A codec built with no settings uses the {@link JsonSettings#builder()} defaults.
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
    public JsonSettings settings() {
        return settings;
    }

    /**
     * Serializes {@code shape} into a new array-backed buffer.
     *
     * @param shape the shape to serialize
     * @return a buffer holding the serialized shape, positioned at its first byte
     * @throws NullPointerException if {@code shape} is null
     * @throws SerializationException if the shape cannot be serialized
     */
    @Override
    public ByteBuffer serialize(SerializableShape shape) {
        Objects.requireNonNull(shape, "shape must not be null");
        return settings.provider().serialize(shape, settings);
    }

    @Override
    public ShapeSerializer createSerializer(OutputStream sink) {
        return settings.provider().newSerializer(sink, settings);
    }

    /**
     * Creates a deserializer that reads one value from {@code source}, without copying it.
     *
     * @param source the JSON bytes; the caller must not modify them while the deserializer is in use
     * @return a new deserializer
     */
    @Override
    public ShapeDeserializer createDeserializer(byte[] source) {
        return settings.provider().newDeserializer(source, settings);
    }

    /**
     * Creates a deserializer that reads one value from the remaining bytes of {@code source}. A buffer with an
     * accessible array is read without copying; the buffer's position is not changed.
     *
     * @param source the JSON bytes; the caller must not modify them while the deserializer is in use
     * @return a new deserializer
     */
    @Override
    public ShapeDeserializer createDeserializer(ByteBuffer source) {
        return settings.provider().newDeserializer(source, settings);
    }

    /**
     * Deserializes one shape from {@code source} and requires that nothing but whitespace follows it.
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
     * Deserializes one shape from the remaining bytes of {@code source} and requires that nothing but whitespace
     * follows it.
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
        return "JsonCodec(" + settings + ")";
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
     * Builds a {@link JsonCodec}.
     */
    public static final class Builder {
        private final JsonSettings.Builder settings = JsonSettings.builder();

        private Builder() {
        }

        /**
         * Copies every value of {@code settings} into this builder. Later calls to the other setters change only the
         * value they set.
         *
         * @param settings the settings to use
         * @return this builder
         * @throws NullPointerException if {@code settings} is null
         */
        public Builder settings(JsonSettings settings) {
            this.settings.updateBuilder(settings);
            return this;
        }

        /**
         * Sets whether member field names come from the {@code jsonName} trait. Off by default.
         *
         * @param useJsonName whether {@code jsonName} is honored
         * @return this builder
         */
        public Builder useJsonName(boolean useJsonName) {
            settings.useJsonName(useJsonName);
            return this;
        }

        /**
         * Sets whether the {@code timestampFormat} trait is honored. Off by default.
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
         * {@code epoch-seconds}.
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
         * Sets whether a union member that is not in the union's schema is rejected with a
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
         * Sets the default namespace used to resolve relative shape IDs in documents. None unless set.
         *
         * @param defaultNamespace the default namespace, or {@code null} for none
         * @return this builder
         */
        public Builder defaultNamespace(String defaultNamespace) {
            settings.defaultNamespace(defaultNamespace);
            return this;
        }

        /**
         * Sets whether structure documents are written with a {@code __type} member. On by default.
         *
         * @param serializeTypeInDocuments whether the type is written in documents
         * @return this builder
         */
        public Builder serializeTypeInDocuments(boolean serializeTypeInDocuments) {
            settings.serializeTypeInDocuments(serializeTypeInDocuments);
            return this;
        }

        /**
         * Sets whether output is indented. Off by default.
         *
         * @param prettyPrint whether output is pretty-printed
         * @return this builder
         */
        public Builder prettyPrint(boolean prettyPrint) {
            settings.prettyPrint(prettyPrint);
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
            settings.useStringForArbitraryPrecision(useStringForArbitraryPrecision);
            return this;
        }

        /**
         * Uses a custom JSON serde provider.
         *
         * @param provider the JSON serde provider to use
         * @return this builder
         * @throws NullPointerException if {@code provider} is null
         */
        public Builder overrideSerdeProvider(JsonSerdeProvider provider) {
            settings.overrideSerdeProvider(provider);
            return this;
        }

        /**
         * Builds the codec.
         *
         * @return the codec
         */
        public JsonCodec build() {
            return new JsonCodec(this);
        }
    }
}
