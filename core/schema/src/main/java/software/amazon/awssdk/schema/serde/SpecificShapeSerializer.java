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
 * A serializer that expects one specific kind of shape and fails when any other kind is written.
 *
 * <p>Every write method throws the exception returned by {@link #throwForInvalidState(String, Schema)}. Subclasses
 * override the write methods for the shapes they accept.
 */
@SdkPublicApi
public abstract class SpecificShapeSerializer implements ShapeSerializer {

    /**
     * Invoked when an unexpected shape is written.
     *
     * @param message A description of the unexpected write.
     * @param schema Schema of the unexpected shape.
     * @return the exception to throw. The default is an {@link IllegalStateException} with the given message.
     */
    protected RuntimeException throwForInvalidState(String message, Schema schema) {
        return new IllegalStateException(message);
    }

    private RuntimeException throwForInvalidState(Schema schema) {
        return throwForInvalidState("Unexpected schema type: " + schema, schema);
    }

    @Override
    public void writeStruct(Schema schema, SerializableStruct struct) {
        throw throwForInvalidState(schema);
    }

    @Override
    public <T> void writeList(Schema schema, T listState, int size, BiConsumer<T, ShapeSerializer> consumer) {
        throw throwForInvalidState(schema);
    }

    @Override
    public <T> void writeMap(Schema schema, T mapState, int size, BiConsumer<T, MapSerializer> consumer) {
        throw throwForInvalidState(schema);
    }

    @Override
    public void writeBoolean(Schema schema, boolean value) {
        throw throwForInvalidState(schema);
    }

    @Override
    public void writeShort(Schema schema, short value) {
        throw throwForInvalidState(schema);
    }

    @Override
    public void writeByte(Schema schema, byte value) {
        throw throwForInvalidState(schema);
    }

    @Override
    public void writeInteger(Schema schema, int value) {
        throw throwForInvalidState(schema);
    }

    @Override
    public void writeLong(Schema schema, long value) {
        throw throwForInvalidState(schema);
    }

    @Override
    public void writeFloat(Schema schema, float value) {
        throw throwForInvalidState(schema);
    }

    @Override
    public void writeDouble(Schema schema, double value) {
        throw throwForInvalidState(schema);
    }

    @Override
    public void writeBigInteger(Schema schema, BigInteger value) {
        throw throwForInvalidState(schema);
    }

    @Override
    public void writeBigDecimal(Schema schema, BigDecimal value) {
        throw throwForInvalidState(schema);
    }

    @Override
    public void writeString(Schema schema, String value) {
        throw throwForInvalidState(schema);
    }

    @Override
    public void writeBlob(Schema schema, ByteBuffer value) {
        throw throwForInvalidState(schema);
    }

    @Override
    public void writeTimestamp(Schema schema, Instant value) {
        throw throwForInvalidState(schema);
    }

    @Override
    public void writeDocument(Schema schema, Document value) {
        throw throwForInvalidState(schema);
    }

    @Override
    public void writeNull(Schema schema) {
        throw throwForInvalidState("Unexpected null value written for " + schema, schema);
    }
}
