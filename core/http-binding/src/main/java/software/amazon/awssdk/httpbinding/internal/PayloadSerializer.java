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
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.codecs.commons.ByteBufferOutputStream;
import software.amazon.awssdk.codecs.commons.Codec;
import software.amazon.awssdk.codecs.commons.TimestampFormatter;
import software.amazon.awssdk.schema.Schema;
import software.amazon.awssdk.schema.SerializableStruct;
import software.amazon.awssdk.schema.serde.MapSerializer;
import software.amazon.awssdk.schema.serde.ShapeSerializer;
import software.amazon.awssdk.schema.serde.document.Document;
import software.amazon.awssdk.schema.traits.TimestampFormatTrait;

/**
 * Writes the single {@code @httpPayload} member.
 *
 * <ul>
 *   <li>Blob payloads are handed to {@link HttpBindingSerializer#setBlobPayload} by reference.</li>
 *   <li>Scalar payloads (string, number, boolean, timestamp) write their text as UTF-8.</li>
 *   <li>Structured payloads (structure, union, document, list, map) are written through the codec, and set the
 *   protocol media type as {@code Content-Type}.</li>
 * </ul>
 *
 * <p>Port of smithy-java {@code PayloadSerializer}. Data streams and event streams are not part of the SDK
 * {@link ShapeSerializer}; streaming payloads are bound by the protocol (task 7.19) and event streams by task 7.20.
 */
@SdkInternalApi
final class PayloadSerializer implements ShapeSerializer {
    private static final byte[] NULL_BYTES = "null".getBytes(StandardCharsets.UTF_8);
    private static final byte[] TRUE_BYTES = "true".getBytes(StandardCharsets.UTF_8);
    private static final byte[] FALSE_BYTES = "false".getBytes(StandardCharsets.UTF_8);

    private final HttpBindingSerializer serializer;
    private final Codec codec;
    private boolean payloadWritten;
    private ByteBufferOutputStream out;

    PayloadSerializer(HttpBindingSerializer serializer, Codec codec) {
        this.serializer = serializer;
        this.codec = codec;
    }

    private ByteBufferOutputStream out() {
        if (out == null) {
            out = new ByteBufferOutputStream();
        }
        return out;
    }

    private void write(byte[] bytes) {
        out().write(bytes, 0, bytes.length);
    }

    private void writeText(String text) {
        write(text.getBytes(StandardCharsets.UTF_8));
    }

    private void writeThroughCodec(Consumer<ShapeSerializer> writer) {
        serializer.writePayloadContentType();
        try (ShapeSerializer codecSerializer = codec.createSerializer(out())) {
            writer.accept(codecSerializer);
        }
    }

    @Override
    public void writeTimestamp(Schema schema, Instant value) {
        writeText(TimestampFormatter.of(schema, TimestampFormatTrait.Format.EPOCH_SECONDS).writeString(value));
    }

    @Override
    public void writeDocument(Schema schema, Document value) {
        writeThroughCodec(ser -> ser.writeDocument(schema, value));
    }

    @Override
    public void writeNull(Schema schema) {
        write(NULL_BYTES);
    }

    @Override
    public void writeStruct(Schema schema, SerializableStruct struct) {
        writeThroughCodec(ser -> ser.writeStruct(schema, struct));
    }

    @Override
    public <T> void writeList(Schema schema, T listState, int size, BiConsumer<T, ShapeSerializer> consumer) {
        writeThroughCodec(ser -> ser.writeList(schema, listState, size, consumer));
    }

    @Override
    public <T> void writeMap(Schema schema, T mapState, int size, BiConsumer<T, MapSerializer> consumer) {
        writeThroughCodec(ser -> ser.writeMap(schema, mapState, size, consumer));
    }

    @Override
    public void writeBoolean(Schema schema, boolean value) {
        write(value ? TRUE_BYTES : FALSE_BYTES);
    }

    @Override
    public void writeByte(Schema schema, byte value) {
        writeText(Byte.toString(value));
    }

    @Override
    public void writeShort(Schema schema, short value) {
        writeText(Short.toString(value));
    }

    @Override
    public void writeInteger(Schema schema, int value) {
        writeText(Integer.toString(value));
    }

    @Override
    public void writeLong(Schema schema, long value) {
        writeText(Long.toString(value));
    }

    @Override
    public void writeFloat(Schema schema, float value) {
        writeText(Float.toString(value));
    }

    @Override
    public void writeDouble(Schema schema, double value) {
        writeText(Double.toString(value));
    }

    @Override
    public void writeBigInteger(Schema schema, BigInteger value) {
        writeText(value.toString());
    }

    @Override
    public void writeBigDecimal(Schema schema, BigDecimal value) {
        writeText(value.toString());
    }

    @Override
    public void writeString(Schema schema, String value) {
        writeText(value);
    }

    @Override
    public void writeBlob(Schema schema, ByteBuffer value) {
        payloadWritten = true;
        serializer.setBlobPayload(schema, value);
    }

    /**
     * Returns whether the payload was handed to the binding serializer directly (a blob).
     */
    boolean isPayloadWritten() {
        return payloadWritten;
    }

    /**
     * Returns the bytes written, or null if nothing was written.
     */
    ByteBufferOutputStream output() {
        return out;
    }
}
