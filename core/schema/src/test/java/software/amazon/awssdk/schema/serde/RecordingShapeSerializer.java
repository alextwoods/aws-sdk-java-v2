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
import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;
import software.amazon.awssdk.schema.Schema;
import software.amazon.awssdk.schema.SerializableStruct;
import software.amazon.awssdk.schema.serde.document.Document;

/**
 * Test serializer that records each write call, in order, as a {@link Call} holding the method name, the schema and
 * the value. It overrides only the abstract methods, so the interface defaults stay in effect.
 */
public final class RecordingShapeSerializer implements ShapeSerializer {
    private final List<Call> calls = new ArrayList<>();
    private int flushCount;

    public List<Call> calls() {
        return calls;
    }

    public int flushCount() {
        return flushCount;
    }

    @Override
    public void flush() {
        flushCount++;
    }

    private void record(String method, Schema schema, Object value) {
        calls.add(new Call(method, schema, value));
    }

    @Override
    public void writeStruct(Schema schema, SerializableStruct struct) {
        record("writeStruct", schema, struct);
    }

    @Override
    public <T> void writeList(Schema schema, T listState, int size, BiConsumer<T, ShapeSerializer> consumer) {
        record("writeList", schema, listState);
    }

    @Override
    public <T> void writeMap(Schema schema, T mapState, int size, BiConsumer<T, MapSerializer> consumer) {
        record("writeMap", schema, mapState);
    }

    @Override
    public void writeBoolean(Schema schema, boolean value) {
        record("writeBoolean", schema, value);
    }

    @Override
    public void writeByte(Schema schema, byte value) {
        record("writeByte", schema, value);
    }

    @Override
    public void writeShort(Schema schema, short value) {
        record("writeShort", schema, value);
    }

    @Override
    public void writeInteger(Schema schema, int value) {
        record("writeInteger", schema, value);
    }

    @Override
    public void writeLong(Schema schema, long value) {
        record("writeLong", schema, value);
    }

    @Override
    public void writeFloat(Schema schema, float value) {
        record("writeFloat", schema, value);
    }

    @Override
    public void writeDouble(Schema schema, double value) {
        record("writeDouble", schema, value);
    }

    @Override
    public void writeBigInteger(Schema schema, BigInteger value) {
        record("writeBigInteger", schema, value);
    }

    @Override
    public void writeBigDecimal(Schema schema, BigDecimal value) {
        record("writeBigDecimal", schema, value);
    }

    @Override
    public void writeString(Schema schema, String value) {
        record("writeString", schema, value);
    }

    @Override
    public void writeBlob(Schema schema, ByteBuffer value) {
        record("writeBlob", schema, value);
    }

    @Override
    public void writeTimestamp(Schema schema, Instant value) {
        record("writeTimestamp", schema, value);
    }

    @Override
    public void writeDocument(Schema schema, Document value) {
        record("writeDocument", schema, value);
    }

    @Override
    public void writeNull(Schema schema) {
        record("writeNull", schema, null);
    }

    /**
     * One recorded write call.
     */
    public static final class Call {
        private final String method;
        private final Schema schema;
        private final Object value;

        Call(String method, Schema schema, Object value) {
            this.method = method;
            this.schema = schema;
            this.value = value;
        }

        public String method() {
            return method;
        }

        public Schema schema() {
            return schema;
        }

        public Object value() {
            return value;
        }
    }
}
