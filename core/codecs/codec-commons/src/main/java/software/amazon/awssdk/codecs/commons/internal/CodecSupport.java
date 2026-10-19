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

package software.amazon.awssdk.codecs.commons.internal;

import java.nio.ByteBuffer;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.codecs.commons.Codec;
import software.amazon.awssdk.schema.Schema;
import software.amazon.awssdk.schema.SerializableShape;
import software.amazon.awssdk.schema.ShapeBuilder;
import software.amazon.awssdk.schema.serde.SerializationException;
import software.amazon.awssdk.schema.serde.ShapeDeserializer;

/**
 * Shared implementation of the {@link Codec#deserializeShape} defaults. Java 8 interfaces cannot have private
 * methods, so the code lives here.
 *
 * <p>Each method creates a deserializer with the codec, reads one shape into the builder, applies
 * {@link ShapeBuilder#errorCorrection()}, builds it and then closes the deserializer, so a deserializer whose
 * {@code close} checks for trailing content rejects it. If reading or building fails, the deserializer is still
 * closed; a failure of that close is added to the original exception as suppressed. No value is returned when any
 * step fails.
 *
 * <p>A {@link SerializationException} whose message does not name the builder's schema ShapeId (for example a
 * syntax error such as an unexpected end of input) is replaced by one that does, with the original as its cause
 * (Requirement 39.4). Other exceptions are thrown unchanged.
 */
@SdkInternalApi
public final class CodecSupport {
    private CodecSupport() {
    }

    /**
     * Deserializes {@code source} with {@code codec} into {@code builder}, as described in the class documentation.
     *
     * @param codec the codec that creates the deserializer
     * @param source the bytes to read
     * @param builder the builder to populate
     * @param <T> the shape type
     * @return the built shape
     */
    public static <T extends SerializableShape> T deserializeShape(Codec codec, byte[] source, ShapeBuilder<T> builder) {
        ShapeDeserializer deserializer;
        try {
            deserializer = codec.createDeserializer(source);
        } catch (RuntimeException e) {
            throw withShapeId(e, builder);
        }
        return deserializeAndClose(deserializer, builder);
    }

    /**
     * Deserializes the remaining bytes of {@code source} with {@code codec} into {@code builder}, as described in the
     * class documentation.
     *
     * @param codec the codec that creates the deserializer
     * @param source the bytes to read
     * @param builder the builder to populate
     * @param <T> the shape type
     * @return the built shape
     */
    public static <T extends SerializableShape> T deserializeShape(Codec codec, ByteBuffer source,
                                                                   ShapeBuilder<T> builder) {
        ShapeDeserializer deserializer;
        try {
            deserializer = codec.createDeserializer(source);
        } catch (RuntimeException e) {
            throw withShapeId(e, builder);
        }
        return deserializeAndClose(deserializer, builder);
    }

    private static <T extends SerializableShape> T deserializeAndClose(ShapeDeserializer deserializer,
                                                                       ShapeBuilder<T> builder) {
        T result;
        try {
            result = builder.deserialize(deserializer).errorCorrection().build();
        } catch (RuntimeException e) {
            closeQuietly(deserializer, e);
            throw withShapeId(e, builder);
        }
        try {
            deserializer.close();
        } catch (RuntimeException e) {
            throw withShapeId(e, builder);
        }
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
     * Returns {@code failure}, or, for a {@link SerializationException} whose message does not name the target
     * ShapeId, a new {@code SerializationException} that does. This adds the context Requirement 39.4 asks for to
     * the codecs' syntax errors, which are raised below the schema level.
     */
    private static RuntimeException withShapeId(RuntimeException failure, ShapeBuilder<?> builder) {
        if (!(failure instanceof SerializationException)) {
            return failure;
        }
        Schema schema = builder.sdkSchema();
        if (schema == null) {
            return failure;
        }
        String shapeId = schema.id().toString();
        String message = failure.getMessage();
        if (message != null && message.contains(shapeId)) {
            return failure;
        }
        return new SerializationException("Unable to deserialize " + shapeId + ": " + message, failure);
    }
}
