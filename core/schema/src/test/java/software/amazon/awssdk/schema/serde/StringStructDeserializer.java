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
import software.amazon.awssdk.schema.Schema;
import software.amazon.awssdk.schema.serde.document.Document;

/**
 * Test deserializer over a structure whose members are all strings with one value. {@link #readStruct} passes every
 * member of the schema to the consumer, then reports one unknown member named {@code "unknown"}. Reads of other types
 * throw. It overrides only the abstract methods, so the interface defaults stay in effect.
 */
public final class StringStructDeserializer implements ShapeDeserializer {
    private final String value;

    public StringStructDeserializer(String value) {
        this.value = value;
    }

    @Override
    public String readString(Schema schema) {
        return value;
    }

    @Override
    public <T> void readStruct(Schema schema, T state, StructMemberConsumer<T> consumer) {
        for (Schema member : schema.members()) {
            consumer.accept(state, member, this);
        }
        consumer.unknownMember(state, "unknown");
    }

    @Override
    public boolean isNull() {
        return false;
    }

    private static UnsupportedOperationException unsupported() {
        return new UnsupportedOperationException("Not supported by this test deserializer");
    }

    @Override
    public boolean readBoolean(Schema schema) {
        throw unsupported();
    }

    @Override
    public ByteBuffer readBlob(Schema schema) {
        throw unsupported();
    }

    @Override
    public byte readByte(Schema schema) {
        throw unsupported();
    }

    @Override
    public short readShort(Schema schema) {
        throw unsupported();
    }

    @Override
    public int readInteger(Schema schema) {
        throw unsupported();
    }

    @Override
    public long readLong(Schema schema) {
        throw unsupported();
    }

    @Override
    public float readFloat(Schema schema) {
        throw unsupported();
    }

    @Override
    public double readDouble(Schema schema) {
        throw unsupported();
    }

    @Override
    public BigInteger readBigInteger(Schema schema) {
        throw unsupported();
    }

    @Override
    public BigDecimal readBigDecimal(Schema schema) {
        throw unsupported();
    }

    @Override
    public Document readDocument() {
        throw unsupported();
    }

    @Override
    public Instant readTimestamp(Schema schema) {
        throw unsupported();
    }

    @Override
    public <T> void readList(Schema schema, T state, ListMemberConsumer<T> consumer) {
        throw unsupported();
    }

    @Override
    public <T> void readStringMap(Schema schema, T state, MapMemberConsumer<String, T> consumer) {
        throw unsupported();
    }
}
