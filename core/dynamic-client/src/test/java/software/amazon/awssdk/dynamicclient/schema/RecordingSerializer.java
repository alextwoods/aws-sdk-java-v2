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

package software.amazon.awssdk.dynamicclient.schema;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;
import software.amazon.awssdk.schema.Schema;
import software.amazon.awssdk.schema.SerializableStruct;
import software.amazon.awssdk.schema.serde.MapSerializer;
import software.amazon.awssdk.schema.serde.ShapeSerializer;
import software.amazon.awssdk.schema.serde.document.Document;

/**
 * Records each write as {@code method(schema id)} or {@code method(schema id)=value}. Structures, lists and maps are
 * recorded and then written recursively; documents are recorded without being read.
 */
final class RecordingSerializer implements ShapeSerializer {
    final List<String> events = new ArrayList<>();

    @Override
    public void writeStruct(Schema schema, SerializableStruct struct) {
        events.add("writeStruct(" + schema.id() + ")");
        struct.serializeMembers(this);
    }

    @Override
    public <T> void writeList(Schema schema, T listState, int size, BiConsumer<T, ShapeSerializer> consumer) {
        events.add("writeList(" + schema.id() + ")=" + size);
        consumer.accept(listState, this);
    }

    @Override
    public <T> void writeMap(Schema schema, T mapState, int size, BiConsumer<T, MapSerializer> consumer) {
        events.add("writeMap(" + schema.id() + ")=" + size);
        consumer.accept(mapState, new MapSerializer() {
            @Override
            public <S> void writeEntry(Schema keySchema, String key, S state, BiConsumer<S, ShapeSerializer> values) {
                events.add("entry(" + keySchema.id() + ")=" + key);
                values.accept(state, RecordingSerializer.this);
            }
        });
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
        record("writeBlob", schema, value.remaining());
    }

    @Override
    public void writeTimestamp(Schema schema, Instant value) {
        record("writeTimestamp", schema, value);
    }

    @Override
    public void writeDocument(Schema schema, Document value) {
        record("writeDocument", schema, value.type());
    }

    @Override
    public void writeNull(Schema schema) {
        events.add("writeNull(" + schema.id() + ")");
    }

    private void record(String method, Schema schema, Object value) {
        events.add(method + "(" + schema.id() + ")=" + value);
    }
}
