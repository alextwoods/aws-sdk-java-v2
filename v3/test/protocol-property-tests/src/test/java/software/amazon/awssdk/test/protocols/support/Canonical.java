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

package software.amazon.awssdk.test.protocols.support;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.function.BiConsumer;
import software.amazon.smithy.java.core.schema.Schema;
import software.amazon.smithy.java.core.schema.SerializableStruct;
import software.amazon.smithy.java.core.schema.ShapeBuilder;
import software.amazon.smithy.java.core.serde.MapSerializer;
import software.amazon.smithy.java.core.serde.ShapeDeserializer;
import software.amazon.smithy.java.core.serde.ShapeSerializer;
import software.amazon.smithy.java.core.serde.document.Document;
import software.amazon.smithy.java.core.serde.event.EventStream;
import software.amazon.smithy.java.core.serde.event.EventStreamReader;

/**
 * The oracle of the SERDE round-trip properties: a canonical value tree, built by the generators independently of
 * every codec, and its two schema-guided bridges to modeled values (a {@link ShapeDeserializer} that builds a value
 * from a tree, and a {@link ShapeSerializer} that reads a value back into a tree). Neither bridge touches a wire
 * format, so a tree comparison checks the protocol codecs alone.
 *
 * <p>Tree nodes, compared with {@link Object#equals}:
 * <ul>
 *   <li>{@link Struct}: structure or union members by name (member order is not significant); a union has exactly
 *       one entry;</li>
 *   <li>{@link java.util.List}: list elements in order, {@link #NULL} for a sparse null;</li>
 *   <li>{@link Entries}: map entries in order (the order is compared), {@link #NULL} for a sparse null value;</li>
 *   <li>{@link Blob}: exact bytes; {@link Instant}: exact instant; {@link Doc}: a document;</li>
 *   <li>{@link Events}: event stream events in order, each a {@link Struct} of the event union;</li>
 *   <li>the boxed Java value of every other simple type ({@link Float}/{@link Double} equality distinguishes
 *       {@code -0.0} and equates {@code NaN}s), strings for enums and integers for intEnums, and
 *       {@link BigDecimal}s normalized by {@link #decimal} (scale is not a modeled value).</li>
 * </ul>
 */
public final class Canonical {
    /** A sparse null. */
    public static final Object NULL = new Object() {
        @Override
        public String toString() {
            return "null";
        }
    };

    private Canonical() {
    }

    /** A structure or union value. */
    public record Struct(SortedMap<String, Object> members) {
        public Struct(Map<String, Object> members) {
            this(new TreeMap<>(members));
        }
    }

    /** A map value, as ordered entries. */
    public record Entries(List<Entry> entries) {
        public Entries {
            entries = List.copyOf(entries);
        }
    }

    public record Entry(String key, Object value) {
    }

    /** Blob bytes, rendered as hex. */
    public record Blob(String hex) {
        public static Blob of(byte[] bytes) {
            return new Blob(HexFormat.of().formatHex(bytes));
        }

        public static Blob of(ByteBuffer buffer) {
            byte[] bytes = new byte[buffer.remaining()];
            buffer.duplicate().get(bytes);
            return of(bytes);
        }

        public byte[] bytes() {
            return HexFormat.of().parseHex(hex);
        }
    }

    /**
     * A document value: {@link Struct} for objects (keys unordered), lists, strings, booleans and numbers normalized
     * to {@link BigDecimal}s. JSON documents carry no numeric type, so a document number is compared by value.
     */
    public record Doc(Object value) {
    }

    /** The events of an event stream, in order. */
    public record Events(List<Object> events) {
        public Events {
            events = List.copyOf(events);
        }
    }

    /** {@code value} without trailing zeros (zero as {@link BigDecimal#ZERO}). */
    public static BigDecimal decimal(BigDecimal value) {
        return value.signum() == 0 ? BigDecimal.ZERO : value.stripTrailingZeros();
    }

    // Tree -> modeled value.

    /** Builds {@code tree} with {@code builder}. */
    public static <T extends SerializableStruct> T build(ShapeBuilder<T> builder, Struct tree,
                                                         EventWriters writers) {
        return builder.deserialize(new TreeDeserializer(tree, writers)).build();
    }

    /**
     * The event stream writers created while building input values: an event stream member's tree becomes a fresh
     * {@link EventStream} writer, whose events are written by {@link #writeAll} once the request body is being
     * consumed.
     */
    public static final class EventWriters {
        private final List<Map.Entry<EventStream<SerializableStruct>, List<SerializableStruct>>> streams =
            new ArrayList<>();

        void add(EventStream<SerializableStruct> stream, List<SerializableStruct> events) {
            streams.add(Map.entry(stream, events));
        }

        public boolean isEmpty() {
            return streams.isEmpty();
        }

        /** Writes every recorded stream's events, then closes it. */
        public void writeAll() {
            for (Map.Entry<EventStream<SerializableStruct>, List<SerializableStruct>> stream : streams) {
                for (SerializableStruct event : stream.getValue()) {
                    stream.getKey().asWriter().write(event);
                }
                stream.getKey().close();
            }
        }
    }

    private static final class TreeDeserializer implements ShapeDeserializer {
        private final Object node;
        private final EventWriters writers;

        TreeDeserializer(Object node, EventWriters writers) {
            this.node = node;
            this.writers = writers;
        }

        @Override
        public boolean readBoolean(Schema schema) {
            return (Boolean) node;
        }

        @Override
        public ByteBuffer readBlob(Schema schema) {
            return ByteBuffer.wrap(((Blob) node).bytes());
        }

        @Override
        public byte readByte(Schema schema) {
            return (Byte) node;
        }

        @Override
        public short readShort(Schema schema) {
            return (Short) node;
        }

        @Override
        public int readInteger(Schema schema) {
            return (Integer) node;
        }

        @Override
        public long readLong(Schema schema) {
            return (Long) node;
        }

        @Override
        public float readFloat(Schema schema) {
            return (Float) node;
        }

        @Override
        public double readDouble(Schema schema) {
            return (Double) node;
        }

        @Override
        public BigInteger readBigInteger(Schema schema) {
            return (BigInteger) node;
        }

        @Override
        public BigDecimal readBigDecimal(Schema schema) {
            return (BigDecimal) node;
        }

        @Override
        public String readString(Schema schema) {
            return (String) node;
        }

        @Override
        public Document readDocument() {
            return toDocument(((Doc) node).value());
        }

        @Override
        public Instant readTimestamp(Schema schema) {
            return (Instant) node;
        }

        @Override
        public <T> void readStruct(Schema schema, T state, StructMemberConsumer<T> consumer) {
            for (Map.Entry<String, Object> member : ((Struct) node).members().entrySet()) {
                Schema memberSchema = schema.member(member.getKey());
                if (memberSchema == null) {
                    throw new IllegalArgumentException("No member " + member.getKey() + " in " + schema.id());
                }
                consumer.accept(state, memberSchema, new TreeDeserializer(member.getValue(), writers));
            }
        }

        @Override
        public <T> void readList(Schema schema, T state, ListMemberConsumer<T> consumer) {
            for (Object element : (List<?>) node) {
                consumer.accept(state, new TreeDeserializer(element, writers));
            }
        }

        @Override
        public <T> void readStringMap(Schema schema, T state, MapMemberConsumer<String, T> consumer) {
            for (Entry entry : ((Entries) node).entries()) {
                consumer.accept(state, entry.key(), new TreeDeserializer(entry.value(), writers));
            }
        }

        @Override
        public EventStream<? extends SerializableStruct> readEventStream(Schema schema) {
            Schema union = schema.isMember() ? schema.memberTarget() : schema;
            List<SerializableStruct> events = new ArrayList<>();
            for (Object event : ((Events) node).events()) {
                events.add(build(software.amazon.smithy.java.dynamicschemas.SchemaConverter
                                     .createDocumentBuilder(union), (Struct) event, writers));
            }
            EventStream<SerializableStruct> stream = EventStream.newWriter();
            writers.add(stream, events);
            return stream;
        }

        @Override
        public boolean isNull() {
            return node == NULL;
        }
    }

    private static Document toDocument(Object value) {
        if (value instanceof Struct struct) {
            Map<String, Document> members = new java.util.LinkedHashMap<>();
            struct.members().forEach((k, v) -> members.put(k, toDocument(v)));
            return Document.of(members);
        } else if (value instanceof List<?> list) {
            return Document.of(list.stream().map(Canonical::toDocument).toList());
        } else if (value instanceof String s) {
            return Document.of(s);
        } else if (value instanceof Boolean b) {
            return Document.of(b);
        } else if (value instanceof BigDecimal d) {
            // An integral number is an integer document (a normalized BigDecimal would be written with an exponent,
            // which JSON documents read back as a double: document numbers carry no numeric type).
            if (d.scale() <= 0 && d.abs().compareTo(BigDecimal.valueOf(Long.MAX_VALUE)) <= 0) {
                return Document.of(d.longValueExact());
            }
            return Document.of(d);
        }
        throw new IllegalArgumentException("Not a document tree: " + value);
    }

    /**
     * The document tree of {@code document}, read through {@link Document#serializeContents} (a JSON document's
     * {@link Document#type()} is its document schema's type, so it does not name the value's kind).
     */
    private static Object fromDocument(Document document) {
        ValueCollector collector = new ValueCollector();
        document.serializeContents(collector);
        return documentTree(collector.value);
    }

    private static Object documentTree(Object value) {
        if (value instanceof Struct struct) {
            SortedMap<String, Object> members = new TreeMap<>();
            struct.members().forEach((k, v) -> members.put(k, documentTree(v)));
            return new Struct(members);
        } else if (value instanceof Entries entries) {
            SortedMap<String, Object> members = new TreeMap<>();
            entries.entries().forEach(e -> members.put(e.key(), documentTree(e.value())));
            return new Struct(members);
        } else if (value instanceof List<?> list) {
            return list.stream().map(Canonical::documentTree).toList();
        } else if (value instanceof Doc doc) {
            return doc.value();
        } else if (value instanceof BigDecimal || value instanceof String || value instanceof Boolean) {
            return value;
        } else if (value instanceof Number number) {
            return decimal(new BigDecimal(number.toString()));
        }
        throw new IllegalArgumentException("Unexpected document value " + value);
    }

    // Modeled value -> tree.

    /** The tree of {@code value}; event streams are read to their end. */
    public static Struct tree(SerializableStruct value) {
        StructCollector collector = new StructCollector();
        value.serializeMembers(collector);
        return new Struct(collector.members);
    }

    /** Collects one value written to it. */
    private static final class ValueCollector implements ShapeSerializer {
        private Object value;

        @Override
        public void writeStruct(Schema schema, SerializableStruct struct) {
            value = tree(struct);
        }

        @Override
        public <T> void writeList(Schema schema, T listState, int size, BiConsumer<T, ShapeSerializer> consumer) {
            ListCollector list = new ListCollector();
            consumer.accept(listState, list);
            value = list.elements;
        }

        @Override
        public <T> void writeMap(Schema schema, T mapState, int size, BiConsumer<T, MapSerializer> consumer) {
            List<Entry> entries = new ArrayList<>();
            consumer.accept(mapState, new MapSerializer() {
                @Override
                public <S> void writeEntry(Schema keySchema, String key, S state,
                                           BiConsumer<S, ShapeSerializer> valueSerializer) {
                    ValueCollector entry = new ValueCollector();
                    valueSerializer.accept(state, entry);
                    entries.add(new Entry(key, entry.value));
                }
            });
            value = new Entries(entries);
        }

        @Override
        public void writeBoolean(Schema schema, boolean v) {
            value = v;
        }

        @Override
        public void writeByte(Schema schema, byte v) {
            value = v;
        }

        @Override
        public void writeShort(Schema schema, short v) {
            value = v;
        }

        @Override
        public void writeInteger(Schema schema, int v) {
            value = v;
        }

        @Override
        public void writeLong(Schema schema, long v) {
            value = v;
        }

        @Override
        public void writeFloat(Schema schema, float v) {
            value = v;
        }

        @Override
        public void writeDouble(Schema schema, double v) {
            value = v;
        }

        @Override
        public void writeBigInteger(Schema schema, BigInteger v) {
            value = v;
        }

        @Override
        public void writeBigDecimal(Schema schema, BigDecimal v) {
            value = decimal(v);
        }

        @Override
        public void writeString(Schema schema, String v) {
            value = v;
        }

        @Override
        public void writeBlob(Schema schema, ByteBuffer v) {
            value = Blob.of(v);
        }

        @Override
        public void writeEventStream(Schema schema, EventStream<? extends SerializableStruct> stream) {
            List<Object> events = new ArrayList<>();
            try (EventStreamReader<? extends SerializableStruct> reader = stream.asReader()) {
                for (SerializableStruct event = reader.read(); event != null; event = reader.read()) {
                    events.add(tree(event));
                }
            }
            value = new Events(events);
        }

        @Override
        public void writeTimestamp(Schema schema, Instant v) {
            value = v;
        }

        @Override
        public void writeDocument(Schema schema, Document v) {
            value = new Doc(fromDocument(v));
        }

        @Override
        public void writeNull(Schema schema) {
            value = NULL;
        }
    }

    /** Collects list elements. */
    private static final class ListCollector extends Delegating {
        private final List<Object> elements = new ArrayList<>();

        @Override
        void accept(Schema schema, Object value) {
            elements.add(value);
        }
    }

    /** Collects structure members by member name. */
    private static final class StructCollector extends Delegating {
        private final SortedMap<String, Object> members = new TreeMap<>();

        @Override
        void accept(Schema schema, Object value) {
            if (members.put(schema.memberName(), value) != null) {
                throw new AssertionError("Member " + schema.memberName() + " was serialized twice");
            }
        }
    }

    /** Collects each written value with a {@link ValueCollector} and hands it to {@link #accept}. */
    private abstract static class Delegating implements ShapeSerializer {
        abstract void accept(Schema schema, Object value);

        private void collect(Schema schema, java.util.function.Consumer<ValueCollector> write) {
            ValueCollector collector = new ValueCollector();
            write.accept(collector);
            accept(schema, collector.value);
        }

        @Override
        public void writeStruct(Schema schema, SerializableStruct struct) {
            collect(schema, c -> c.writeStruct(schema, struct));
        }

        @Override
        public <T> void writeList(Schema schema, T listState, int size, BiConsumer<T, ShapeSerializer> consumer) {
            collect(schema, c -> c.writeList(schema, listState, size, consumer));
        }

        @Override
        public <T> void writeMap(Schema schema, T mapState, int size, BiConsumer<T, MapSerializer> consumer) {
            collect(schema, c -> c.writeMap(schema, mapState, size, consumer));
        }

        @Override
        public void writeBoolean(Schema schema, boolean value) {
            collect(schema, c -> c.writeBoolean(schema, value));
        }

        @Override
        public void writeByte(Schema schema, byte value) {
            collect(schema, c -> c.writeByte(schema, value));
        }

        @Override
        public void writeShort(Schema schema, short value) {
            collect(schema, c -> c.writeShort(schema, value));
        }

        @Override
        public void writeInteger(Schema schema, int value) {
            collect(schema, c -> c.writeInteger(schema, value));
        }

        @Override
        public void writeLong(Schema schema, long value) {
            collect(schema, c -> c.writeLong(schema, value));
        }

        @Override
        public void writeFloat(Schema schema, float value) {
            collect(schema, c -> c.writeFloat(schema, value));
        }

        @Override
        public void writeDouble(Schema schema, double value) {
            collect(schema, c -> c.writeDouble(schema, value));
        }

        @Override
        public void writeBigInteger(Schema schema, BigInteger value) {
            collect(schema, c -> c.writeBigInteger(schema, value));
        }

        @Override
        public void writeBigDecimal(Schema schema, BigDecimal value) {
            collect(schema, c -> c.writeBigDecimal(schema, value));
        }

        @Override
        public void writeString(Schema schema, String value) {
            collect(schema, c -> c.writeString(schema, value));
        }

        @Override
        public void writeBlob(Schema schema, ByteBuffer value) {
            collect(schema, c -> c.writeBlob(schema, value));
        }

        @Override
        public void writeEventStream(Schema schema, EventStream<? extends SerializableStruct> value) {
            collect(schema, c -> c.writeEventStream(schema, value));
        }

        @Override
        public void writeTimestamp(Schema schema, Instant value) {
            collect(schema, c -> c.writeTimestamp(schema, value));
        }

        @Override
        public void writeDocument(Schema schema, Document value) {
            collect(schema, c -> c.writeDocument(schema, value));
        }

        @Override
        public void writeNull(Schema schema) {
            collect(schema, c -> c.writeNull(schema));
        }
    }
}
