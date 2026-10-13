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
import software.amazon.awssdk.annotations.SdkPublicApi;
import software.amazon.awssdk.schema.SerializableShape;
import software.amazon.awssdk.schema.serde.ShapeDeserializer;
import software.amazon.awssdk.schema.serde.ShapeSerializer;

/**
 * Creates the CBOR serializers and deserializers that {@link Rpcv2CborCodec} uses.
 *
 * <p>Implementations are discovered with {@link java.util.ServiceLoader} through
 * {@code META-INF/services/software.amazon.awssdk.codecs.cbor.CborSerdeProvider}; the provider with the highest
 * {@link #getPriority() priority} is the default for {@link CborSettings}. When none is registered, the SDK's built-in
 * provider (priority 0) is used. Implementations must be thread-safe.
 *
 * <p>Port of smithy-java {@code software.amazon.smithy.java.cbor.CborSerdeProvider}.
 */
@SdkPublicApi
public interface CborSerdeProvider {
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
     * @param source the CBOR bytes; the caller must not modify them while the deserializer is in use
     * @param settings the settings to apply
     * @return a new deserializer
     */
    ShapeDeserializer newDeserializer(byte[] source, CborSettings settings);

    /**
     * Creates a deserializer that reads one value from the remaining bytes of {@code source}.
     *
     * @param source the CBOR bytes; the caller must not modify them while the deserializer is in use
     * @param settings the settings to apply
     * @return a new deserializer
     */
    ShapeDeserializer newDeserializer(ByteBuffer source, CborSettings settings);

    /**
     * Creates a serializer that writes to {@code sink}. Closing the serializer flushes everything written to the sink;
     * it does not close the sink.
     *
     * @param sink where to write
     * @param settings the settings to apply
     * @return a new serializer
     */
    ShapeSerializer newSerializer(OutputStream sink, CborSettings settings);

    /**
     * Serializes {@code shape} into a new buffer. Must produce the same bytes as writing the shape through
     * {@link #newSerializer(OutputStream, CborSettings)}.
     *
     * @param shape the shape to serialize
     * @param settings the settings to apply
     * @return a buffer holding the serialized shape, positioned at its first byte
     */
    ByteBuffer serialize(SerializableShape shape, CborSettings settings);
}
