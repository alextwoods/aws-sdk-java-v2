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

import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.util.Objects;
import software.amazon.awssdk.annotations.SdkPublicApi;
import software.amazon.awssdk.codecs.commons.Codec;
import software.amazon.awssdk.schema.SerializableShape;
import software.amazon.awssdk.schema.serde.SerializationException;
import software.amazon.awssdk.schema.serde.ShapeDeserializer;
import software.amazon.awssdk.schema.serde.ShapeSerializer;

/**
 * A {@link Codec} for CBOR (RFC 8949) that follows the
 * <a href="https://smithy.io/2.0/additional-specs/protocols/smithy-rpc-v2.html#shape-serialization">Smithy RPCv2 CBOR
 * shape serialization</a> rules:
 * <ul>
 *     <li>Integers are written in their shortest encoding; {@code float} and {@code double} as single- and
 *     double-precision floats. Floating-point members also accept integers on read.</li>
 *     <li>{@code bigInteger} values are written as integers when they fit in 64 bits and as tag 2/3 bignums
 *     otherwise; {@code bigDecimal} values as tag 4 decimal fractions.</li>
 *     <li>Timestamps are written as tag 1 epoch seconds with millisecond precision: an integer for whole seconds, a
 *     double otherwise. The {@code timestampFormat} trait is ignored.</li>
 *     <li>Structures and unions are written as indefinite-length maps keyed by member name; lists and maps with a
 *     definite length when their size is known. Definite and indefinite lengths are both accepted on read.</li>
 *     <li>On read, null structure members are treated as absent and unknown members are skipped.</li>
 * </ul>
 *
 * <p>Malformed input and values that do not match the target schema throw {@link SerializationException}. The codec is
 * immutable and thread-safe; the serializers and deserializers it creates are single-use.
 *
 * <p>Port of smithy-java {@code software.amazon.smithy.java.cbor.Rpcv2CborCodec}.
 */
@SdkPublicApi
public final class Rpcv2CborCodec implements Codec {
    private final CborSettings settings;

    private Rpcv2CborCodec(Builder builder) {
        this.settings = builder.settings == null ? CborSettings.RPCV2 : builder.settings.build();
    }

    /**
     * Creates a builder. A codec built with no settings uses {@link CborSettings#RPCV2}.
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
    public CborSettings settings() {
        return settings;
    }

    /**
     * Serializes {@code shape} into a new array-backed buffer, using a pooled serializer.
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
     * @param source the CBOR bytes; the caller must not modify them while the deserializer is in use
     * @return a new deserializer
     * @throws SerializationException if the first value is malformed
     */
    @Override
    public ShapeDeserializer createDeserializer(byte[] source) {
        return settings.provider().newDeserializer(source, settings);
    }

    /**
     * Creates a deserializer that reads one value from the remaining bytes of {@code source}. A buffer with an
     * accessible array is read without copying; the buffer's position is not changed.
     *
     * @param source the CBOR bytes; the caller must not modify them while the deserializer is in use
     * @return a new deserializer
     * @throws SerializationException if the first value is malformed
     */
    @Override
    public ShapeDeserializer createDeserializer(ByteBuffer source) {
        return settings.provider().newDeserializer(source, settings);
    }

    @Override
    public String toString() {
        return "Rpcv2CborCodec(" + settings + ")";
    }

    /**
     * Builds an {@link Rpcv2CborCodec}.
     */
    public static final class Builder {
        private CborSettings.Builder settings;

        private Builder() {
        }

        private CborSettings.Builder settings() {
            if (settings == null) {
                settings = CborSettings.builder();
            }
            return settings;
        }

        /**
         * Sets the default namespace used to resolve relative shape IDs in documents. No default namespace is used
         * unless one is set.
         *
         * @param defaultNamespace the default namespace, or {@code null} for none
         * @return this builder
         */
        public Builder defaultNamespace(String defaultNamespace) {
            settings().defaultNamespace(defaultNamespace);
            return this;
        }

        /**
         * Sets whether a union member that is not in the union's schema is rejected with a
         * {@link SerializationException} instead of being reported as an unknown member. Off by default.
         *
         * @param forbidUnknownUnionMembers whether unknown union members are rejected
         * @return this builder
         */
        public Builder forbidUnknownUnionMembers(boolean forbidUnknownUnionMembers) {
            settings().forbidUnknownUnionMembers(forbidUnknownUnionMembers);
            return this;
        }

        /**
         * Uses a custom CBOR serde provider.
         *
         * @param provider the CBOR serde provider to use
         * @return this builder
         * @throws NullPointerException if {@code provider} is null
         */
        public Builder overrideSerdeProvider(CborSerdeProvider provider) {
            settings().overrideSerdeProvider(provider);
            return this;
        }

        /**
         * Copies every value of {@code settings} into this builder. Later calls to the other setters change only the
         * value they set.
         *
         * @param settings the settings to use
         * @return this builder
         * @throws NullPointerException if {@code settings} is null
         */
        public Builder settings(CborSettings settings) {
            settings().updateBuilder(settings);
            return this;
        }

        /**
         * Builds the codec.
         *
         * @return the codec
         */
        public Rpcv2CborCodec build() {
            return new Rpcv2CborCodec(this);
        }
    }
}
