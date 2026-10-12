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
import java.util.Objects;
import java.util.function.BiConsumer;
import java.util.function.IntConsumer;
import software.amazon.awssdk.annotations.SdkPublicApi;
import software.amazon.awssdk.schema.Schema;
import software.amazon.awssdk.schema.SerializableStruct;
import software.amazon.awssdk.schema.serde.document.Document;

/**
 * A serializer that runs a callback before each value it writes, for example to write a separator between list
 * elements.
 */
@SdkPublicApi
public final class ListSerializer implements ShapeSerializer {

    private final ShapeSerializer delegate;
    private final IntConsumer beforeEachValue;
    private int position = 0;

    /**
     * @param delegate Delegate that serializes the values.
     * @param beforeEachValue Invoked before each value with the value's position in the list.
     */
    public ListSerializer(ShapeSerializer delegate, IntConsumer beforeEachValue) {
        this.delegate = Objects.requireNonNull(delegate, "delegate is null");
        this.beforeEachValue = Objects.requireNonNull(beforeEachValue, "beforeEachValue is null");
    }

    private void beforeWrite() {
        beforeEachValue.accept(position++);
    }

    /**
     * Returns the position at which the next element will be written.
     *
     * @return the current position.
     */
    public int position() {
        return position;
    }

    @Override
    public void writeStruct(Schema schema, SerializableStruct struct) {
        beforeWrite();
        delegate.writeStruct(schema, struct);
    }

    @Override
    public <T> void writeList(Schema schema, T state, int size, BiConsumer<T, ShapeSerializer> consumer) {
        beforeWrite();
        delegate.writeList(schema, state, size, consumer);
    }

    @Override
    public <T> void writeMap(Schema schema, T state, int size, BiConsumer<T, MapSerializer> consumer) {
        beforeWrite();
        delegate.writeMap(schema, state, size, consumer);
    }

    @Override
    public void writeBoolean(Schema schema, boolean value) {
        beforeWrite();
        delegate.writeBoolean(schema, value);
    }

    @Override
    public void writeShort(Schema schema, short value) {
        beforeWrite();
        delegate.writeShort(schema, value);
    }

    @Override
    public void writeByte(Schema schema, byte value) {
        beforeWrite();
        delegate.writeByte(schema, value);
    }

    @Override
    public void writeInteger(Schema schema, int value) {
        beforeWrite();
        delegate.writeInteger(schema, value);
    }

    @Override
    public void writeLong(Schema schema, long value) {
        beforeWrite();
        delegate.writeLong(schema, value);
    }

    @Override
    public void writeFloat(Schema schema, float value) {
        beforeWrite();
        delegate.writeFloat(schema, value);
    }

    @Override
    public void writeDouble(Schema schema, double value) {
        beforeWrite();
        delegate.writeDouble(schema, value);
    }

    @Override
    public void writeBigInteger(Schema schema, BigInteger value) {
        beforeWrite();
        delegate.writeBigInteger(schema, value);
    }

    @Override
    public void writeBigDecimal(Schema schema, BigDecimal value) {
        beforeWrite();
        delegate.writeBigDecimal(schema, value);
    }

    @Override
    public void writeString(Schema schema, String value) {
        beforeWrite();
        delegate.writeString(schema, value);
    }

    @Override
    public void writeBlob(Schema schema, ByteBuffer value) {
        beforeWrite();
        delegate.writeBlob(schema, value);
    }

    @Override
    public void writeTimestamp(Schema schema, Instant value) {
        beforeWrite();
        delegate.writeTimestamp(schema, value);
    }

    @Override
    public void writeDocument(Schema schema, Document value) {
        beforeWrite();
        delegate.writeDocument(schema, value);
    }

    @Override
    public void writeNull(Schema schema) {
        beforeWrite();
        delegate.writeNull(schema);
    }
}
