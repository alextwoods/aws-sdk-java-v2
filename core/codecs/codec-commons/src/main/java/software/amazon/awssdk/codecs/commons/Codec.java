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

import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import software.amazon.awssdk.annotations.SdkPublicApi;
import software.amazon.awssdk.schema.SerializableShape;
import software.amazon.awssdk.schema.ShapeBuilder;
import software.amazon.awssdk.schema.serde.SerializationException;
import software.amazon.awssdk.schema.serde.ShapeDeserializer;
import software.amazon.awssdk.schema.serde.ShapeSerializer;
import software.amazon.awssdk.utils.SdkAutoCloseable;

/**
 * Serializes shapes to, and deserializes shapes from, the bytes of one data format (for example JSON or CBOR) with fixed
 * {@link CodecSettings}.
 *
 * <p>A codec writes and reads only the encoded payload. It adds no HTTP message, header, query string or other protocol
 * framing; client protocols do that.
 *
 * <p>Port of smithy-java {@code software.amazon.smithy.java.core.serde.Codec}. Third parties can implement this interface
 * to add a codec for another format.
 *
 * <p><b>Thread safety.</b> Implementations must be immutable after construction and safe to share between threads:
 * concurrent calls on one codec must each produce the result they would produce alone. The serializers and deserializers
 * a codec creates are single-use and must not be shared between threads.
 */
@SdkPublicApi
public interface Codec extends SdkAutoCloseable {

    /**
     * Releases any resources held by this codec. The default does nothing.
     */
    @Override
    default void close() {
    }

    /**
     * Creates a serializer that writes one shape to {@code sink}. Closing the serializer flushes everything written to the
     * sink; it does not close the sink.
     *
     * @param sink where to write the serialized shape
     * @return a new serializer
     */
    ShapeSerializer createSerializer(OutputStream sink);

    /**
     * Serializes {@code shape} into a buffer.
     *
     * <p>The default writes through {@link #createSerializer(OutputStream)} into a {@link ByteBufferOutputStream}, so it
     * produces the same bytes as the serializer path:
     *
     * <pre>{@code
     * ByteBufferOutputStream out = new ByteBufferOutputStream();
     * try (ShapeSerializer serializer = codec.createSerializer(out)) {
     *     shape.serialize(serializer);
     * }
     * return out.toByteBuffer();
     * }</pre>
     *
     * Implementations may override it with a faster path that produces the same bytes.
     *
     * <p>The returned buffer may not {@linkplain ByteBuffer#hasArray() have an accessible backing array} and, if it does,
     * the data may not {@linkplain ByteBuffer#arrayOffset() start at offset 0}. Read heap buffers with this idiom:
     *
     * <pre>{@code
     * if (buffer.hasArray()) {
     *     int pos = buffer.arrayOffset() + buffer.position();
     *     int len = buffer.remaining();
     *     doSomethingWithBuffer(buffer.array(), pos, len);
     * } else {
     *     // use ByteBuffer retrieval methods
     * }
     * }</pre>
     *
     * @param shape the shape to serialize
     * @return a buffer holding the serialized shape, positioned at its first byte
     * @throws NullPointerException if {@code shape} is null
     * @throws SerializationException if the shape cannot be serialized
     */
    default ByteBuffer serialize(SerializableShape shape) {
        Objects.requireNonNull(shape, "shape must not be null");
        ByteBufferOutputStream out = new ByteBufferOutputStream();
        try (ShapeSerializer serializer = createSerializer(out)) {
            shape.serialize(serializer);
        }
        return out.toByteBuffer();
    }

    /**
     * Creates a deserializer that reads one shape from {@code source}. The default wraps the array, without copying, and
     * calls {@link #createDeserializer(ByteBuffer)}.
     *
     * @param source the bytes to read; the caller must not modify them while the deserializer is in use
     * @return a new deserializer
     */
    default ShapeDeserializer createDeserializer(byte[] source) {
        return createDeserializer(ByteBuffer.wrap(source));
    }

    /**
     * Creates a deserializer that reads one shape from the remaining bytes of {@code source}, from its position to its
     * limit. Implementations may read the buffer's backing array directly instead of copying it.
     *
     * @param source the bytes to read; the caller must not modify them while the deserializer is in use
     * @return a new deserializer
     */
    ShapeDeserializer createDeserializer(ByteBuffer source);

    /**
     * Serializes {@code shape} and decodes the bytes as UTF-8. Use this only with formats whose output is UTF-8 text.
     *
     * @param shape the shape to serialize
     * @return the serialized shape as a string
     * @throws NullPointerException if {@code shape} is null
     * @throws SerializationException if the shape cannot be serialized
     */
    default String serializeToString(SerializableShape shape) {
        Objects.requireNonNull(shape, "shape must not be null");
        ByteBufferOutputStream out = new ByteBufferOutputStream();
        try (ShapeSerializer serializer = createSerializer(out)) {
            shape.serialize(serializer);
        }
        return new String(out.array(), 0, out.size(), StandardCharsets.UTF_8);
    }

    /**
     * Deserializes {@code source} into {@code builder}, applies {@link ShapeBuilder#errorCorrection()} and builds the
     * shape.
     *
     * @param source the bytes to read
     * @param builder the builder to populate
     * @param <T> the shape type
     * @return the built shape
     * @throws NullPointerException if {@code source} or {@code builder} is null
     * @throws SerializationException if the bytes cannot be read into the shape; no shape is returned
     */
    default <T extends SerializableShape> T deserializeShape(byte[] source, ShapeBuilder<T> builder) {
        Objects.requireNonNull(source, "source must not be null");
        Objects.requireNonNull(builder, "builder must not be null");
        return builder.deserialize(createDeserializer(source)).errorCorrection().build();
    }

    /**
     * Deserializes the remaining bytes of {@code source} into {@code builder}, applies
     * {@link ShapeBuilder#errorCorrection()} and builds the shape.
     *
     * @param source the bytes to read
     * @param builder the builder to populate
     * @param <T> the shape type
     * @return the built shape
     * @throws NullPointerException if {@code source} or {@code builder} is null
     * @throws SerializationException if the bytes cannot be read into the shape; no shape is returned
     */
    default <T extends SerializableShape> T deserializeShape(ByteBuffer source, ShapeBuilder<T> builder) {
        Objects.requireNonNull(source, "source must not be null");
        Objects.requireNonNull(builder, "builder must not be null");
        return builder.deserialize(createDeserializer(source)).errorCorrection().build();
    }

    /**
     * Deserializes the UTF-8 bytes of {@code source} into {@code builder}, applies
     * {@link ShapeBuilder#errorCorrection()} and builds the shape. Use this only with formats whose input is UTF-8
     * text.
     *
     * @param source the text to read
     * @param builder the builder to populate
     * @param <T> the shape type
     * @return the built shape
     * @throws NullPointerException if {@code source} or {@code builder} is null
     * @throws SerializationException if the text cannot be read into the shape; no shape is returned
     */
    default <T extends SerializableShape> T deserializeShape(String source, ShapeBuilder<T> builder) {
        Objects.requireNonNull(source, "source must not be null");
        return deserializeShape(source.getBytes(StandardCharsets.UTF_8), builder);
    }
}
