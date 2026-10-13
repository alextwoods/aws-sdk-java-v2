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

package software.amazon.awssdk.codecs.cbor.internal;

import java.io.OutputStream;
import java.nio.ByteBuffer;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.codecs.cbor.CborSerdeProvider;
import software.amazon.awssdk.codecs.cbor.CborSettings;
import software.amazon.awssdk.schema.SerializableShape;
import software.amazon.awssdk.schema.serde.ShapeDeserializer;
import software.amazon.awssdk.schema.serde.ShapeSerializer;

/**
 * The SDK's built-in {@link CborSerdeProvider} (priority 0, name {@code cbor}). {@link #serialize} writes through a
 * serializer taken from a per-thread pool.
 *
 * <p>Port of smithy-java {@code DefaultCborSerdeProvider}.
 */
@SdkInternalApi
public final class DefaultCborSerdeProvider implements CborSerdeProvider {

    @Override
    public int getPriority() {
        return 0;
    }

    @Override
    public String getName() {
        return "cbor";
    }

    @Override
    public ShapeDeserializer newDeserializer(byte[] source, CborSettings settings) {
        return new CborDeserializer(source, settings);
    }

    @Override
    public ShapeDeserializer newDeserializer(ByteBuffer source, CborSettings settings) {
        return new CborDeserializer(source, settings);
    }

    @Override
    public ShapeSerializer newSerializer(OutputStream sink, CborSettings settings) {
        return new CborSerializer(sink);
    }

    @Override
    public ByteBuffer serialize(SerializableShape shape, CborSettings settings) {
        CborSerializer serializer = CborSerializer.acquire();
        try {
            shape.serialize(serializer);
            return serializer.extractResult();
        } finally {
            CborSerializer.release(serializer);
        }
    }

    @Override
    public String toString() {
        return "DefaultCborSerdeProvider";
    }
}
