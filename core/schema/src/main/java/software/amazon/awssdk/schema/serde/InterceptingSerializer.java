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

package software.amazon.awssdk.schema.serde;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.time.Instant;
import java.util.function.BiConsumer;
import software.amazon.awssdk.annotations.SdkPublicApi;
import software.amazon.awssdk.schema.Schema;
import software.amazon.awssdk.schema.SerializableStruct;
import software.amazon.awssdk.schema.serde.document.Document;

/**
 * Intercepts serialization before and after each write.
 *
 * <p>{@link #before(Schema)} returns the {@link ShapeSerializer} that performs the write, so the method acts as a
 * router between serializers based on the given schema. {@link #after(Schema)} runs once the write returns.
 */
@SdkPublicApi
public abstract class InterceptingSerializer implements ShapeSerializer {

    /**
     * Called before writing and returns the serializer to delegate to.
     *
     * @param schema Schema of the shape about to be written.
     * @return the serializer that writes this schema.
     */
    protected abstract ShapeSerializer before(Schema schema);

    /**
     * Called after the delegated serializer returns.
     *
     * @param schema Schema that was serialized.
     */
    protected void after(Schema schema) {
    }

    @Override
    public final void writeStruct(Schema schema, SerializableStruct struct) {
        before(schema).writeStruct(schema, struct);
        after(schema);
    }

    @Override
    public final <T> void writeList(Schema schema, T listState, int size, BiConsumer<T, ShapeSerializer> consumer) {
        before(schema).writeList(schema, listState, size, consumer);
        after(schema);
    }

    @Override
    public final <T> void writeMap(Schema schema, T mapState, int size, BiConsumer<T, MapSerializer> consumer) {
        before(schema).writeMap(schema, mapState, size, consumer);
        after(schema);
    }

    @Override
    public final void writeBoolean(Schema schema, boolean value) {
        before(schema).writeBoolean(schema, value);
        after(schema);
    }

    @Override
    public final void writeShort(Schema schema, short value) {
        before(schema).writeShort(schema, value);
        after(schema);
    }

    @Override
    public final void writeByte(Schema schema, byte value) {
        before(schema).writeByte(schema, value);
        after(schema);
    }

    @Override
    public final void writeInteger(Schema schema, int value) {
        before(schema).writeInteger(schema, value);
        after(schema);
    }

    @Override
    public final void writeLong(Schema schema, long value) {
        before(schema).writeLong(schema, value);
        after(schema);
    }

    @Override
    public final void writeFloat(Schema schema, float value) {
        before(schema).writeFloat(schema, value);
        after(schema);
    }

    @Override
    public final void writeDouble(Schema schema, double value) {
        before(schema).writeDouble(schema, value);
        after(schema);
    }

    @Override
    public final void writeBigInteger(Schema schema, BigInteger value) {
        before(schema).writeBigInteger(schema, value);
        after(schema);
    }

    @Override
    public final void writeBigDecimal(Schema schema, BigDecimal value) {
        before(schema).writeBigDecimal(schema, value);
        after(schema);
    }

    @Override
    public final void writeString(Schema schema, String value) {
        before(schema).writeString(schema, value);
        after(schema);
    }

    @Override
    public final void writeBlob(Schema schema, ByteBuffer value) {
        before(schema).writeBlob(schema, value);
        after(schema);
    }

    @Override
    public final void writeTimestamp(Schema schema, Instant value) {
        before(schema).writeTimestamp(schema, value);
        after(schema);
    }

    @Override
    public final void writeDocument(Schema schema, Document value) {
        before(schema).writeDocument(schema, value);
        after(schema);
    }

    @Override
    public final void writeNull(Schema schema) {
        before(schema).writeNull(schema);
        after(schema);
    }
}
