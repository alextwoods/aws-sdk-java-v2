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

package software.amazon.awssdk.protocols.query;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import java.util.Objects;
import java.util.function.BiConsumer;
import software.amazon.awssdk.protocols.core.serde.TimestampFormats;
import software.amazon.smithy.java.core.schema.Schema;
import software.amazon.smithy.java.core.schema.SerializableStruct;
import software.amazon.smithy.java.core.schema.TraitKey;
import software.amazon.smithy.java.core.serde.MapSerializer;
import software.amazon.smithy.java.core.serde.ShapeSerializer;
import software.amazon.smithy.java.core.serde.document.Document;
import software.amazon.smithy.java.core.serde.event.EventStream;
import software.amazon.smithy.java.io.datastream.DataStream;
import software.amazon.smithy.model.traits.TimestampFormatTrait;
import software.amazon.smithy.model.traits.XmlNameTrait;

/**
 * Serializes an operation input into the {@code application/x-www-form-urlencoded} body of the query protocols:
 * {@code Action=<operation>&Version=<service version>} followed by one {@code key=value} pair per scalar, in member
 * order. Keys are dot paths ({@code Outer.Inner}); list items are {@code Key.member.N} (or {@code Key.<xmlName>.N},
 * or {@code Key.N} when flattened) counted from 1; map entries are {@code Key.entry.N.key}/{@code .value} (renamed by
 * the key and value members' {@code xmlName}, or {@code Key.N.key} when flattened). {@link QueryFormRules} decides
 * member keys, flattening and empty lists.
 *
 * <p>Timestamps honor {@code timestampFormat} and default to {@code date-time}; blobs are base64; floats are
 * {@code NaN}, {@code Infinity} or {@code -Infinity} when not finite. Keys and values are percent-encoded as UTF-8,
 * leaving only RFC 3986 unreserved characters ({@code A-Z a-z 0-9 - . _ ~}) as is. Null values (sparse collections)
 * are omitted. Documents are not supported by the query protocols.
 */
public final class QueryFormSerializer {
    private QueryFormSerializer() {
    }

    /**
     * The form body of {@code input} for operation {@code action} of a service at {@code version}.
     *
     * @throws IllegalArgumentException if {@code input} contains a document, which the query protocols do not support
     */
    public static String serialize(String action, String version, SerializableStruct input, QueryFormRules rules) {
        Objects.requireNonNull(action, "action");
        Objects.requireNonNull(version, "version");
        Objects.requireNonNull(input, "input");
        Objects.requireNonNull(rules, "rules");
        Form form = new Form();
        form.add("Action", action);
        form.add("Version", version);
        input.serializeMembers(new StructSerializer(form, rules, ""));
        return form.out.toString();
    }

    /** {@code value} percent-encoded as UTF-8, leaving only RFC 3986 unreserved characters as is. */
    static void encode(StringBuilder out, String value) {
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            if (isUnreserved(c)) {
                out.append(c);
                continue;
            }
            int end = i + 1;
            if (Character.isHighSurrogate(c) && end < value.length() && Character.isLowSurrogate(value.charAt(end))) {
                end++;
            }
            for (byte b : value.substring(i, end).getBytes(StandardCharsets.UTF_8)) {
                out.append('%').append(HEX[(b >> 4) & 0xF]).append(HEX[b & 0xF]);
            }
            i = end - 1;
        }
    }

    private static final char[] HEX = "0123456789ABCDEF".toCharArray();

    private static boolean isUnreserved(char c) {
        return (c >= 'A' && c <= 'Z') || (c >= 'a' && c <= 'z') || (c >= '0' && c <= '9')
               || c == '-' || c == '.' || c == '_' || c == '~';
    }

    private static String partName(Schema member, String defaultName) {
        XmlNameTrait name = member.getDirectTrait(TraitKey.XML_NAME_TRAIT);
        return name != null ? name.getValue() : defaultName;
    }

    /** The body being written. */
    private static final class Form {
        private final StringBuilder out = new StringBuilder(128);

        void add(String key, String value) {
            if (!out.isEmpty()) {
                out.append('&');
            }
            encode(out, key);
            out.append('=');
            encode(out, value);
        }
    }

    /** Writes the members of a structure or union whose key prefix is {@code prefix} (empty, or ending in '.'). */
    private static final class StructSerializer extends Delegating {
        private final String prefix;

        StructSerializer(Form form, QueryFormRules rules, String prefix) {
            super(form, rules);
            this.prefix = prefix;
        }

        @Override
        ShapeSerializer target(Schema member) {
            return new ValueSerializer(form, rules, prefix + rules.memberKey(member), rules.flattened(member));
        }
    }

    /** Writes the items of a list whose item keys are {@code prefix.N}. */
    private static final class ListItemSerializer extends Delegating {
        private final String prefix;
        private int index;

        ListItemSerializer(Form form, QueryFormRules rules, String prefix) {
            super(form, rules);
            this.prefix = prefix;
        }

        @Override
        ShapeSerializer target(Schema member) {
            return new ValueSerializer(form, rules, prefix + "." + ++index, rules.flattened(member));
        }

        @Override
        public void writeNull(Schema schema) {
            // A null item of a sparse list is omitted.
        }
    }

    /** A serializer that forwards every value to the serializer of its key. */
    private abstract static class Delegating implements ShapeSerializer {
        final Form form;
        final QueryFormRules rules;

        Delegating(Form form, QueryFormRules rules) {
            this.form = form;
            this.rules = rules;
        }

        abstract ShapeSerializer target(Schema schema);

        @Override
        public void writeStruct(Schema schema, SerializableStruct struct) {
            target(schema).writeStruct(schema, struct);
        }

        @Override
        public <T> void writeList(Schema schema, T state, int size, BiConsumer<T, ShapeSerializer> consumer) {
            target(schema).writeList(schema, state, size, consumer);
        }

        @Override
        public <T> void writeMap(Schema schema, T state, int size, BiConsumer<T, MapSerializer> consumer) {
            target(schema).writeMap(schema, state, size, consumer);
        }

        @Override
        public void writeBoolean(Schema schema, boolean value) {
            target(schema).writeBoolean(schema, value);
        }

        @Override
        public void writeByte(Schema schema, byte value) {
            target(schema).writeByte(schema, value);
        }

        @Override
        public void writeShort(Schema schema, short value) {
            target(schema).writeShort(schema, value);
        }

        @Override
        public void writeInteger(Schema schema, int value) {
            target(schema).writeInteger(schema, value);
        }

        @Override
        public void writeLong(Schema schema, long value) {
            target(schema).writeLong(schema, value);
        }

        @Override
        public void writeFloat(Schema schema, float value) {
            target(schema).writeFloat(schema, value);
        }

        @Override
        public void writeDouble(Schema schema, double value) {
            target(schema).writeDouble(schema, value);
        }

        @Override
        public void writeBigInteger(Schema schema, BigInteger value) {
            target(schema).writeBigInteger(schema, value);
        }

        @Override
        public void writeBigDecimal(Schema schema, BigDecimal value) {
            target(schema).writeBigDecimal(schema, value);
        }

        @Override
        public void writeString(Schema schema, String value) {
            target(schema).writeString(schema, value);
        }

        @Override
        public void writeBlob(Schema schema, ByteBuffer value) {
            target(schema).writeBlob(schema, value);
        }

        @Override
        public void writeTimestamp(Schema schema, Instant value) {
            target(schema).writeTimestamp(schema, value);
        }

        @Override
        public void writeDocument(Schema schema, Document value) {
            target(schema).writeDocument(schema, value);
        }

        @Override
        public void writeNull(Schema schema) {
            // Absent and null members are omitted.
        }

        @Override
        public void writeDataStream(Schema schema, DataStream value) {
            throw new UnsupportedOperationException("Streaming blob members are not supported by query protocols");
        }

        @Override
        public void writeEventStream(Schema schema, EventStream<? extends SerializableStruct> value) {
            throw new UnsupportedOperationException("Event stream members are not supported by query protocols");
        }
    }

    /** Writes one value at {@code key}. */
    private static final class ValueSerializer implements ShapeSerializer {
        private final Form form;
        private final QueryFormRules rules;
        private final String key;
        private final boolean flattened;

        ValueSerializer(Form form, QueryFormRules rules, String key, boolean flattened) {
            this.form = form;
            this.rules = rules;
            this.key = key;
            this.flattened = flattened;
        }

        @Override
        public void writeStruct(Schema schema, SerializableStruct struct) {
            struct.serializeMembers(new StructSerializer(form, rules, key + "."));
        }

        @Override
        public <T> void writeList(Schema schema, T state, int size, BiConsumer<T, ShapeSerializer> consumer) {
            if (size == 0) {
                if (rules.writesEmptyLists()) {
                    form.add(key, "");
                }
                return;
            }
            String prefix = flattened ? key : key + "." + partName(schema.listMember(), "member");
            consumer.accept(state, new ListItemSerializer(form, rules, prefix));
        }

        @Override
        public <T> void writeMap(Schema schema, T state, int size, BiConsumer<T, MapSerializer> consumer) {
            if (size == 0) {
                return;
            }
            String prefix = flattened ? key : key + ".entry";
            String keyName = partName(schema.mapKeyMember(), "key");
            Schema valueMember = schema.mapValueMember();
            String valueName = partName(valueMember, "value");
            boolean valueFlattened = rules.flattened(valueMember);
            consumer.accept(state, new MapSerializer() {
                private int index;

                @Override
                public <V> void writeEntry(Schema keySchema, String entryKey, V valueState,
                                           BiConsumer<V, ShapeSerializer> valueSerializer) {
                    String entry = prefix + "." + ++index + ".";
                    form.add(entry + keyName, entryKey);
                    valueSerializer.accept(valueState, new ValueSerializer(form, rules, entry + valueName,
                                                                           valueFlattened));
                }
            });
        }

        @Override
        public void writeBoolean(Schema schema, boolean value) {
            form.add(key, Boolean.toString(value));
        }

        @Override
        public void writeByte(Schema schema, byte value) {
            form.add(key, Byte.toString(value));
        }

        @Override
        public void writeShort(Schema schema, short value) {
            form.add(key, Short.toString(value));
        }

        @Override
        public void writeInteger(Schema schema, int value) {
            form.add(key, Integer.toString(value));
        }

        @Override
        public void writeLong(Schema schema, long value) {
            form.add(key, Long.toString(value));
        }

        @Override
        public void writeFloat(Schema schema, float value) {
            // Float.toString renders non-finite values as NaN, Infinity and -Infinity, as the protocols require.
            form.add(key, Float.toString(value));
        }

        @Override
        public void writeDouble(Schema schema, double value) {
            form.add(key, Double.toString(value));
        }

        @Override
        public void writeBigInteger(Schema schema, BigInteger value) {
            form.add(key, value.toString());
        }

        @Override
        public void writeBigDecimal(Schema schema, BigDecimal value) {
            form.add(key, value.toPlainString());
        }

        @Override
        public void writeString(Schema schema, String value) {
            form.add(key, value);
        }

        @Override
        public void writeBlob(Schema schema, ByteBuffer value) {
            ByteBuffer view = value.duplicate();
            byte[] bytes = new byte[view.remaining()];
            view.get(bytes);
            form.add(key, Base64.getEncoder().encodeToString(bytes));
        }

        @Override
        public void writeTimestamp(Schema schema, Instant value) {
            form.add(key, TimestampFormats.write(value, TimestampFormats.formatOf(
                schema, TimestampFormatTrait.Format.DATE_TIME)));
        }

        @Override
        public void writeDocument(Schema schema, Document value) {
            throw new IllegalArgumentException("Document values are not supported by query protocols");
        }

        @Override
        public void writeNull(Schema schema) {
            // Null values are omitted.
        }

        @Override
        public void writeDataStream(Schema schema, DataStream value) {
            throw new UnsupportedOperationException("Streaming blob members are not supported by query protocols");
        }

        @Override
        public void writeEventStream(Schema schema, EventStream<? extends SerializableStruct> value) {
            throw new UnsupportedOperationException("Event stream members are not supported by query protocols");
        }
    }
}
