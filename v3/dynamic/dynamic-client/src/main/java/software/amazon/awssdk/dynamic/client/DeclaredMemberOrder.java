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

package software.amazon.awssdk.dynamic.client;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import software.amazon.awssdk.protocols.core.eventstream.EventStreams;
import software.amazon.awssdk.smithy.runtime.schema.ServiceSchemaIndex;
import software.amazon.smithy.java.core.schema.Schema;
import software.amazon.smithy.java.core.schema.SerializableStruct;
import software.amazon.smithy.java.core.serde.MapSerializer;
import software.amazon.smithy.java.core.serde.ShapeSerializer;
import software.amazon.smithy.java.core.serde.document.Document;
import software.amazon.smithy.java.core.serde.event.EventStream;
import software.amazon.smithy.java.io.datastream.DataStream;

/**
 * Serializes schema-guided documents with structure and union members in modeled declaration order, the order
 * generated types write them in (Requirements 14.16, 19.6).
 *
 * <p>A Smithy Java {@code StructDocument} writes its members in member-index order, and {@code SchemaBuilder} indexes
 * required members without a default before the others, so a document of a structure that declares an optional member
 * before a required one would put the required member first on the wire. That changes the bytes of every ordered
 * format (JSON and CBOR bodies, XML elements, query form fields) and so the payload hash and signature. The
 * {@linkplain #view view} of a struct records each member write of the struct and replays them in the order of
 * {@link ServiceSchemaIndex#members(Schema)} (the modeled order); nested structures and unions, including list
 * elements and map values, are viewed the same way, recursively. Member values themselves are written unchanged. The
 * events of an input event stream are encoded by the protocol apart from the request, so the view of a request is an
 * {@link EventStreams.InputEventView} that views each event the same way.
 *
 * <p>Stateless; each view is used by one serialization at a time.
 */
final class DeclaredMemberOrder {
    private final ServiceSchemaIndex index;

    DeclaredMemberOrder(ServiceSchemaIndex index) {
        this.index = Objects.requireNonNull(index, "index");
    }

    /** {@code struct} writing its members, and those of nested structures and unions, in declaration order. */
    SerializableStruct view(SerializableStruct struct) {
        return struct instanceof View ? struct : new View(struct);
    }

    /** The members of {@code schema} in declaration order, or {@code null} if member-index order is that order. */
    private List<Schema> reordered(Schema schema) {
        Schema root = schema.isMember() ? schema.memberTarget() : schema;
        if (root.members().size() < 2 || index.schema(root.id()).orElse(null) != root) {
            return null;
        }
        List<Schema> declared = index.members(root);
        for (int i = 0; i < declared.size(); i++) {
            if (declared.get(i).memberIndex() != i) {
                return declared;
            }
        }
        return null;
    }

    /**
     * The ordered view of one struct. As a request view it also views each event of its input event stream, which
     * the protocol encodes outside the request's own serialization.
     */
    private final class View implements SerializableStruct, EventStreams.InputEventView {
        private final SerializableStruct delegate;

        private View(SerializableStruct delegate) {
            this.delegate = delegate;
        }

        @Override
        public SerializableStruct viewInputEvent(SerializableStruct event) {
            return view(event);
        }

        @Override
        public Schema schema() {
            return delegate.schema();
        }

        @Override
        public <T> T getMemberValue(Schema member) {
            return delegate.getMemberValue(member);
        }

        @Override
        public void serializeMembers(ShapeSerializer serializer) {
            ShapeSerializer ordering = new Ordering(serializer);
            List<Schema> declared = reordered(delegate.schema());
            if (declared == null) {
                delegate.serializeMembers(ordering);
                return;
            }
            Recorder recorder = new Recorder(delegate.schema().members().size());
            delegate.serializeMembers(recorder);
            for (Schema member : declared) {
                Consumer<ShapeSerializer> write = recorder.writes[member.memberIndex()];
                if (write != null) {
                    write.accept(ordering);
                }
            }
        }

        @Override
        public String toString() {
            return delegate.toString();
        }
    }

    /** Delegates every write, viewing written structs and the elements of written lists and maps. */
    private final class Ordering implements ShapeSerializer {
        private final ShapeSerializer delegate;

        private Ordering(ShapeSerializer delegate) {
            this.delegate = delegate;
        }

        @Override
        public void writeStruct(Schema schema, SerializableStruct struct) {
            delegate.writeStruct(schema, view(struct));
        }

        @Override
        public <T> void writeList(Schema schema, T listState, int size, BiConsumer<T, ShapeSerializer> consumer) {
            delegate.writeList(schema, listState, size, (state, elements) -> consumer.accept(state,
                                                                                            new Ordering(elements)));
        }

        @Override
        public <T> void writeMap(Schema schema, T mapState, int size, BiConsumer<T, MapSerializer> consumer) {
            delegate.writeMap(schema, mapState, size, (state, entries) -> consumer.accept(state,
                                                                                         new OrderingMap(entries)));
        }

        @Override
        public void writeBoolean(Schema schema, boolean value) {
            delegate.writeBoolean(schema, value);
        }

        @Override
        public void writeByte(Schema schema, byte value) {
            delegate.writeByte(schema, value);
        }

        @Override
        public void writeShort(Schema schema, short value) {
            delegate.writeShort(schema, value);
        }

        @Override
        public void writeInteger(Schema schema, int value) {
            delegate.writeInteger(schema, value);
        }

        @Override
        public void writeLong(Schema schema, long value) {
            delegate.writeLong(schema, value);
        }

        @Override
        public void writeFloat(Schema schema, float value) {
            delegate.writeFloat(schema, value);
        }

        @Override
        public void writeDouble(Schema schema, double value) {
            delegate.writeDouble(schema, value);
        }

        @Override
        public void writeBigInteger(Schema schema, BigInteger value) {
            delegate.writeBigInteger(schema, value);
        }

        @Override
        public void writeBigDecimal(Schema schema, BigDecimal value) {
            delegate.writeBigDecimal(schema, value);
        }

        @Override
        public void writeString(Schema schema, String value) {
            delegate.writeString(schema, value);
        }

        @Override
        public void writeBlob(Schema schema, ByteBuffer value) {
            delegate.writeBlob(schema, value);
        }

        @Override
        public void writeBlob(Schema schema, byte[] value) {
            delegate.writeBlob(schema, value);
        }

        @Override
        public void writeDataStream(Schema schema, DataStream value) {
            delegate.writeDataStream(schema, value);
        }

        @Override
        public void writeEventStream(Schema schema, EventStream<? extends SerializableStruct> value) {
            delegate.writeEventStream(schema, value);
        }

        @Override
        public void writeTimestamp(Schema schema, Instant value) {
            delegate.writeTimestamp(schema, value);
        }

        @Override
        public void writeDocument(Schema schema, Document value) {
            delegate.writeDocument(schema, value);
        }

        @Override
        public void writeNull(Schema schema) {
            delegate.writeNull(schema);
        }

        @Override
        public void flush() {
            delegate.flush();
        }

        @Override
        public void close() {
            delegate.close();
        }
    }

    /** Delegates every entry, viewing written map values. */
    private final class OrderingMap implements MapSerializer {
        private final MapSerializer delegate;

        private OrderingMap(MapSerializer delegate) {
            this.delegate = delegate;
        }

        @Override
        public <T> void writeEntry(Schema keySchema, String key, T state,
                                   BiConsumer<T, ShapeSerializer> valueSerializer) {
            delegate.writeEntry(keySchema, key, state, (value, serializer) -> valueSerializer.accept(
                value, new Ordering(serializer)));
        }
    }

    /** Records each member write of one struct, by member index, for replay in declaration order. */
    private static final class Recorder implements ShapeSerializer {
        private final Consumer<ShapeSerializer>[] writes;

        @SuppressWarnings("unchecked")
        private Recorder(int members) {
            this.writes = new Consumer[members];
        }

        private void record(Schema member, Consumer<ShapeSerializer> write) {
            int index = member.memberIndex();
            if (index < 0 || index >= writes.length || writes[index] != null) {
                throw new IllegalStateException("Unexpected member write " + member.id());
            }
            writes[index] = write;
        }

        @Override
        public void writeStruct(Schema schema, SerializableStruct struct) {
            record(schema, s -> s.writeStruct(schema, struct));
        }

        @Override
        public <T> void writeList(Schema schema, T listState, int size, BiConsumer<T, ShapeSerializer> consumer) {
            record(schema, s -> s.writeList(schema, listState, size, consumer));
        }

        @Override
        public <T> void writeMap(Schema schema, T mapState, int size, BiConsumer<T, MapSerializer> consumer) {
            record(schema, s -> s.writeMap(schema, mapState, size, consumer));
        }

        @Override
        public void writeBoolean(Schema schema, boolean value) {
            record(schema, s -> s.writeBoolean(schema, value));
        }

        @Override
        public void writeByte(Schema schema, byte value) {
            record(schema, s -> s.writeByte(schema, value));
        }

        @Override
        public void writeShort(Schema schema, short value) {
            record(schema, s -> s.writeShort(schema, value));
        }

        @Override
        public void writeInteger(Schema schema, int value) {
            record(schema, s -> s.writeInteger(schema, value));
        }

        @Override
        public void writeLong(Schema schema, long value) {
            record(schema, s -> s.writeLong(schema, value));
        }

        @Override
        public void writeFloat(Schema schema, float value) {
            record(schema, s -> s.writeFloat(schema, value));
        }

        @Override
        public void writeDouble(Schema schema, double value) {
            record(schema, s -> s.writeDouble(schema, value));
        }

        @Override
        public void writeBigInteger(Schema schema, BigInteger value) {
            record(schema, s -> s.writeBigInteger(schema, value));
        }

        @Override
        public void writeBigDecimal(Schema schema, BigDecimal value) {
            record(schema, s -> s.writeBigDecimal(schema, value));
        }

        @Override
        public void writeString(Schema schema, String value) {
            record(schema, s -> s.writeString(schema, value));
        }

        @Override
        public void writeBlob(Schema schema, ByteBuffer value) {
            record(schema, s -> s.writeBlob(schema, value));
        }

        @Override
        public void writeBlob(Schema schema, byte[] value) {
            record(schema, s -> s.writeBlob(schema, value));
        }

        @Override
        public void writeDataStream(Schema schema, DataStream value) {
            record(schema, s -> s.writeDataStream(schema, value));
        }

        @Override
        public void writeEventStream(Schema schema, EventStream<? extends SerializableStruct> value) {
            record(schema, s -> s.writeEventStream(schema, value));
        }

        @Override
        public void writeTimestamp(Schema schema, Instant value) {
            record(schema, s -> s.writeTimestamp(schema, value));
        }

        @Override
        public void writeDocument(Schema schema, Document value) {
            record(schema, s -> s.writeDocument(schema, value));
        }

        @Override
        public void writeNull(Schema schema) {
            record(schema, s -> s.writeNull(schema));
        }
    }
}
