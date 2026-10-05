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

package software.amazon.awssdk.protocols.core.eventstream;

import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.util.Objects;
import software.amazon.smithy.java.core.schema.Schema;
import software.amazon.smithy.java.core.schema.SerializableStruct;
import software.amazon.smithy.java.core.serde.Codec;
import software.amazon.smithy.java.core.serde.ShapeDeserializer;
import software.amazon.smithy.java.core.serde.ShapeSerializer;

/**
 * The protocol codec that serializes structured event payloads (JSON for {@code awsJson1_0/1_1} and
 * {@code restJson1}, CBOR for {@code rpcv2Cbor}, XML for {@code restXml}).
 */
public interface EventPayloadCodec {
    /** The {@code :content-type} of structured payloads, for example {@code application/json}. */
    String mediaType();

    /** {@code value} serialized as a complete document whose root has {@code schema}. */
    byte[] serialize(Schema schema, SerializableStruct value);

    /** A deserializer positioned at the root value of the non-empty {@code payload}. */
    ShapeDeserializer deserializer(ByteBuffer payload);

    /** The payload codec of a Smithy Java {@link Codec}. */
    static EventPayloadCodec of(Codec codec, String mediaType) {
        Objects.requireNonNull(codec, "codec");
        Objects.requireNonNull(mediaType, "mediaType");
        return new EventPayloadCodec() {
            @Override
            public String mediaType() {
                return mediaType;
            }

            @Override
            public byte[] serialize(Schema schema, SerializableStruct value) {
                ByteArrayOutputStream out = new ByteArrayOutputStream();
                try (ShapeSerializer serializer = codec.createSerializer(out)) {
                    serializer.writeStruct(schema, value);
                    serializer.flush();
                }
                return out.toByteArray();
            }

            @Override
            public ShapeDeserializer deserializer(ByteBuffer payload) {
                return codec.createDeserializer(payload);
            }
        };
    }
}
