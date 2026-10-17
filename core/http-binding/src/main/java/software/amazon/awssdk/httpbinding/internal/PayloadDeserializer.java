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

package software.amazon.awssdk.httpbinding.internal;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.codecs.commons.Codec;
import software.amazon.awssdk.schema.Schema;
import software.amazon.awssdk.schema.serde.ShapeDeserializer;
import software.amazon.awssdk.schema.serde.document.Document;

/**
 * Reads the {@code @httpPayload} member from the whole body. Blobs and strings are the raw bytes; structures, unions,
 * documents, lists, maps and the other scalars are read through the codec.
 *
 * <p>Port of smithy-java {@code PayloadDeserializer}, without the {@code MemberSubsetCodec} direct-builder path.
 * Every codec read closes its deserializer, so trailing content after the payload is rejected by codecs that check it
 * (JSON).
 */
@SdkInternalApi
final class PayloadDeserializer implements ShapeDeserializer {
    private final Codec payloadCodec;
    private final ByteBuffer body;

    PayloadDeserializer(Codec payloadCodec, ByteBuffer body) {
        this.payloadCodec = payloadCodec;
        this.body = body;
    }

    private ShapeDeserializer createDeserializer() {
        return payloadCodec.createDeserializer(body.duplicate());
    }

    @Override
    public boolean readBoolean(Schema schema) {
        try (ShapeDeserializer deser = createDeserializer()) {
            return deser.readBoolean(schema);
        }
    }

    @Override
    public ByteBuffer readBlob(Schema schema) {
        return body.duplicate();
    }

    @Override
    public byte readByte(Schema schema) {
        try (ShapeDeserializer deser = createDeserializer()) {
            return deser.readByte(schema);
        }
    }

    @Override
    public short readShort(Schema schema) {
        try (ShapeDeserializer deser = createDeserializer()) {
            return deser.readShort(schema);
        }
    }

    @Override
    public int readInteger(Schema schema) {
        try (ShapeDeserializer deser = createDeserializer()) {
            return deser.readInteger(schema);
        }
    }

    @Override
    public long readLong(Schema schema) {
        try (ShapeDeserializer deser = createDeserializer()) {
            return deser.readLong(schema);
        }
    }

    @Override
    public float readFloat(Schema schema) {
        try (ShapeDeserializer deser = createDeserializer()) {
            return deser.readFloat(schema);
        }
    }

    @Override
    public double readDouble(Schema schema) {
        try (ShapeDeserializer deser = createDeserializer()) {
            return deser.readDouble(schema);
        }
    }

    @Override
    public BigInteger readBigInteger(Schema schema) {
        try (ShapeDeserializer deser = createDeserializer()) {
            return deser.readBigInteger(schema);
        }
    }

    @Override
    public BigDecimal readBigDecimal(Schema schema) {
        try (ShapeDeserializer deser = createDeserializer()) {
            return deser.readBigDecimal(schema);
        }
    }

    @Override
    public String readString(Schema schema) {
        ByteBuffer b = body.duplicate();
        if (b.hasArray()) {
            return new String(b.array(), b.arrayOffset() + b.position(), b.remaining(), StandardCharsets.UTF_8);
        }
        return StandardCharsets.UTF_8.decode(b).toString();
    }

    @Override
    public Document readDocument() {
        try (ShapeDeserializer deser = createDeserializer()) {
            return deser.readDocument();
        }
    }

    @Override
    public Instant readTimestamp(Schema schema) {
        try (ShapeDeserializer deser = createDeserializer()) {
            return deser.readTimestamp(schema);
        }
    }

    @Override
    public <T> void readStruct(Schema schema, T state, StructMemberConsumer<T> consumer) {
        try (ShapeDeserializer deser = createDeserializer()) {
            deser.readStruct(schema, state, consumer);
        }
    }

    @Override
    public <T> void readList(Schema schema, T state, ListMemberConsumer<T> consumer) {
        try (ShapeDeserializer deser = createDeserializer()) {
            deser.readList(schema, state, consumer);
        }
    }

    @Override
    public <T> void readStringMap(Schema schema, T state, MapMemberConsumer<String, T> consumer) {
        try (ShapeDeserializer deser = createDeserializer()) {
            deser.readStringMap(schema, state, consumer);
        }
    }

    @Override
    public boolean isNull() {
        return false;
    }
}
