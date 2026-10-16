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
import software.amazon.awssdk.annotations.SdkPublicApi;
import software.amazon.awssdk.codecs.commons.ByteBufferOutputStream;
import software.amazon.awssdk.schema.SerializableShape;
import software.amazon.awssdk.schema.serde.ShapeDeserializer;
import software.amazon.awssdk.schema.serde.ShapeSerializer;

/**
 * Creates the JSON serializers and deserializers that {@link JsonCodec} uses.
 *
 * <p>Implementations are discovered with {@link java.util.ServiceLoader} through
 * {@code META-INF/services/software.amazon.awssdk.codecs.json.JsonSerdeProvider}; the provider with the highest
 * {@link #getPriority() priority} is the default for {@link JsonSettings}. When none is registered, the SDK's built-in
 * hand-written provider is used. This interface is kept so that another implementation (for example one on the SDK's
 * shaded Jackson core) can be added without an API change. Implementations must be thread-safe.
 *
 * <p>Port of smithy-java {@code software.amazon.smithy.java.json.JsonSerdeProvider}.
 */
@SdkPublicApi
public interface JsonSerdeProvider {

    /**
     * Returns the priority of this provider. The registered provider with the highest priority is the default.
     *
     * @return the priority
     */
    int getPriority();

    /**
     * Returns the name of this provider, for diagnostics.
     *
     * @return the name
     */
    String getName();

    /**
     * Creates a deserializer that reads one value from {@code source}.
     *
     * @param source the JSON bytes; the caller must not modify them while the deserializer is in use
     * @param settings the settings to apply
     * @return a new deserializer
     */
    ShapeDeserializer newDeserializer(byte[] source, JsonSettings settings);

    /**
     * Creates a deserializer that reads one value from the remaining bytes of {@code source}.
     *
     * @param source the JSON bytes; the caller must not modify them while the deserializer is in use
     * @param settings the settings to apply
     * @return a new deserializer
     */
    ShapeDeserializer newDeserializer(ByteBuffer source, JsonSettings settings);

    /**
     * Creates a serializer that writes to {@code sink}. Closing the serializer writes everything to the sink; it does
     * not close the sink.
     *
     * @param sink where to write
     * @param settings the settings to apply
     * @return a new serializer
     */
    ShapeSerializer newSerializer(OutputStream sink, JsonSettings settings);

    /**
     * Serializes {@code shape} into a new buffer. Must produce the same bytes as writing the shape through
     * {@link #newSerializer(OutputStream, JsonSettings)}. The default writes through a {@link ByteBufferOutputStream};
     * providers can override it to avoid the intermediate copy.
     *
     * @param shape the shape to serialize
     * @param settings the settings to apply
     * @return a buffer holding the serialized shape, positioned at its first byte
     */
    default ByteBuffer serialize(SerializableShape shape, JsonSettings settings) {
        ByteBufferOutputStream out = new ByteBufferOutputStream(256);
        try (ShapeSerializer serializer = newSerializer(out, settings)) {
            shape.serialize(serializer);
        }
        return out.toByteBuffer();
    }
}
