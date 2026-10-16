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

package software.amazon.awssdk.codecs.json.internal;

import java.io.OutputStream;
import java.nio.ByteBuffer;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.codecs.commons.ByteBufferOutputStream;
import software.amazon.awssdk.codecs.json.JsonSerdeProvider;
import software.amazon.awssdk.codecs.json.JsonSettings;
import software.amazon.awssdk.schema.SerializableShape;
import software.amazon.awssdk.schema.serde.ShapeDeserializer;
import software.amazon.awssdk.schema.serde.ShapeSerializer;

/**
 * The SDK's built-in {@link JsonSerdeProvider} (priority 10, name {@code smithy}): the hand-written byte-level
 * serializer and deserializer. {@link #serialize} writes compact output through a serializer taken from a striped
 * pool; pretty-printed output goes through {@link JsonPrettyPrinter}.
 *
 * <p>Port of smithy-java {@code software.amazon.smithy.java.json.smithy.SmithyJsonSerdeProvider}, which delegates
 * {@code prettyPrint} to its Jackson provider instead.
 */
@SdkInternalApi
public final class SmithyJsonSerdeProvider implements JsonSerdeProvider {

    @Override
    public int getPriority() {
        return 10;
    }

    @Override
    public String getName() {
        return "smithy";
    }

    @Override
    public ByteBuffer serialize(SerializableShape shape, JsonSettings settings) {
        if (settings.prettyPrint()) {
            ByteBufferOutputStream out = new ByteBufferOutputStream(256);
            try (ShapeSerializer serializer = newSerializer(out, settings)) {
                shape.serialize(serializer);
            }
            return out.toByteBuffer();
        }
        SmithyJsonSerializer serializer = SmithyJsonSerializer.acquire(settings);
        boolean completed = false;
        try {
            shape.serialize(serializer);
            ByteBuffer result = serializer.extractResult();
            completed = true;
            return result;
        } finally {
            SmithyJsonSerializer.release(serializer, !completed);
        }
    }

    @Override
    public ShapeSerializer newSerializer(OutputStream sink, JsonSettings settings) {
        return new SmithyJsonSerializer(settings.prettyPrint() ? new JsonPrettyPrinter(sink) : sink, settings);
    }

    @Override
    public ShapeDeserializer newDeserializer(byte[] source, JsonSettings settings) {
        return new SmithyJsonDeserializer(source, 0, source.length, settings);
    }

    @Override
    public ShapeDeserializer newDeserializer(ByteBuffer source, JsonSettings settings) {
        if (source.hasArray()) {
            int offset = source.arrayOffset() + source.position();
            int length = source.remaining();
            return new SmithyJsonDeserializer(source.array(), offset, offset + length, settings);
        }
        // Read-only or direct buffer: copy the remaining bytes.
        byte[] bytes = new byte[source.remaining()];
        source.duplicate().get(bytes);
        return new SmithyJsonDeserializer(bytes, 0, bytes.length, settings);
    }

    @Override
    public String toString() {
        return "SmithyJsonSerdeProvider";
    }
}
