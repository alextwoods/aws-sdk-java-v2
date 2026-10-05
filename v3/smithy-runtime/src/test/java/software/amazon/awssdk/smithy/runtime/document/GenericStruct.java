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

package software.amazon.awssdk.smithy.runtime.document;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import software.amazon.smithy.java.core.schema.Schema;
import software.amazon.smithy.java.core.schema.SerializableStruct;
import software.amazon.smithy.java.core.schema.ShapeBuilder;
import software.amazon.smithy.java.core.serde.ShapeDeserializer;
import software.amazon.smithy.java.core.serde.ShapeSerializer;
import software.amazon.smithy.java.core.serde.document.Document;
import software.amazon.smithy.model.shapes.ShapeType;

/**
 * A minimal hand-written typed value of any structure or union schema, standing in for a generated type in
 * {@link DocumentRoundTripPropertyTest}. It serializes and deserializes through the member schemas exactly like
 * generated code (a direct member switch on the member's target kind) and uses no {@link Document} internally, so
 * {@link SchemaDocuments#of} converts it through its serialization events.
 *
 * <p>Member values are plain Java values: {@code Boolean}, the boxed number of the kind ({@code Integer} for intEnums),
 * {@code BigInteger}, {@code BigDecimal}, {@code String} (also enums), {@code byte[]} (blobs), {@code Instant},
 * {@link Document} (document members), {@code List} (elements may be {@code null} when sparse), {@code Map} of
 * string keys (values may be {@code null} when sparse) and {@code GenericStruct} (structures and unions). Present
 * members are kept in schema member order.
 */
final class GenericStruct implements SerializableStruct {

    private final Schema schema;
    private final Map<String, Object> values;

    GenericStruct(Schema schema, Map<String, Object> values) {
        this.schema = schema;
        Map<String, Object> ordered = new LinkedHashMap<>();
        for (Schema member : schema.members()) {
            if (values.containsKey(member.memberName())) {
                ordered.put(member.memberName(), values.get(member.memberName()));
            }
        }
        if (ordered.size() != values.size()) {
            throw new IllegalArgumentException("Unknown members of " + schema.id() + ": " + values.keySet());
        }
        this.values = Collections.unmodifiableMap(ordered);
    }

    /** The present member values, in schema member order. */
    Map<String, Object> values() {
        return values;
    }

    @Override
    public Schema schema() {
        return schema;
    }

    @Override
    public void serializeMembers(ShapeSerializer serializer) {
        for (Map.Entry<String, Object> entry : values.entrySet()) {
            write(serializer, schema.member(entry.getKey()), entry.getValue());
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T getMemberValue(Schema member) {
        return (T) values.get(member.memberName());
    }

    @Override
    public String toString() {
        return schema.id().getName() + values;
    }

    /** Writes one member, element or entry value with its member schema. */
    static void write(ShapeSerializer serializer, Schema member, Object value) {
        if (value == null) {
            serializer.writeNull(member);
            return;
        }
        Schema target = member.memberTarget();
        switch (target.type()) {
            case BOOLEAN -> serializer.writeBoolean(member, (Boolean) value);
            case BYTE -> serializer.writeByte(member, (Byte) value);
            case SHORT -> serializer.writeShort(member, (Short) value);
            case INTEGER, INT_ENUM -> serializer.writeInteger(member, (Integer) value);
            case LONG -> serializer.writeLong(member, (Long) value);
            case FLOAT -> serializer.writeFloat(member, (Float) value);
            case DOUBLE -> serializer.writeDouble(member, (Double) value);
            case BIG_INTEGER -> serializer.writeBigInteger(member, (BigInteger) value);
            case BIG_DECIMAL -> serializer.writeBigDecimal(member, (BigDecimal) value);
            case STRING, ENUM -> serializer.writeString(member, (String) value);
            case BLOB -> serializer.writeBlob(member, ByteBuffer.wrap((byte[]) value));
            case TIMESTAMP -> serializer.writeTimestamp(member, (Instant) value);
            case DOCUMENT -> serializer.writeDocument(member, (Document) value);
            case LIST, SET -> {
                List<?> list = (List<?>) value;
                serializer.writeList(member, list, list.size(), (elements, elementSerializer) -> {
                    for (Object element : elements) {
                        write(elementSerializer, member.listMember(), element);
                    }
                });
            }
            case MAP -> {
                Map<?, ?> map = (Map<?, ?>) value;
                serializer.writeMap(member, map, map.size(), (entries, mapSerializer) -> {
                    for (Map.Entry<?, ?> entry : entries.entrySet()) {
                        mapSerializer.writeEntry(member.mapKeyMember(), (String) entry.getKey(), entry.getValue(),
                                                 (v, valueSerializer) -> write(valueSerializer, member.mapValueMember(),
                                                                               v));
                    }
                });
            }
            case STRUCTURE, UNION -> serializer.writeStruct(member, (GenericStruct) value);
            default -> throw new IllegalStateException("Unsupported kind " + target.type());
        }
    }

    /** Reads one member, element or entry value of the member schema {@code member}. */
    static Object read(ShapeDeserializer deserializer, Schema member) {
        Schema target = member.memberTarget();
        return switch (target.type()) {
            case BOOLEAN -> deserializer.readBoolean(member);
            case BYTE -> deserializer.readByte(member);
            case SHORT -> deserializer.readShort(member);
            case INTEGER, INT_ENUM -> deserializer.readInteger(member);
            case LONG -> deserializer.readLong(member);
            case FLOAT -> deserializer.readFloat(member);
            case DOUBLE -> deserializer.readDouble(member);
            case BIG_INTEGER -> deserializer.readBigInteger(member);
            case BIG_DECIMAL -> deserializer.readBigDecimal(member);
            case STRING, ENUM -> deserializer.readString(member);
            case BLOB -> {
                ByteBuffer buffer = deserializer.readBlob(member).duplicate();
                byte[] bytes = new byte[buffer.remaining()];
                buffer.get(bytes);
                yield bytes;
            }
            case TIMESTAMP -> deserializer.readTimestamp(member);
            case DOCUMENT -> deserializer.readDocument();
            case LIST, SET -> {
                List<Object> list = new ArrayList<>();
                deserializer.readList(member, list, (elements, element) -> elements.add(
                    element.isNull() ? element.readNull() : read(element, member.listMember())));
                yield list;
            }
            case MAP -> {
                Map<String, Object> map = new LinkedHashMap<>();
                deserializer.readStringMap(member, map, (entries, key, entry) -> entries.put(
                    key, entry.isNull() ? entry.readNull() : read(entry, member.mapValueMember())));
                yield map;
            }
            case STRUCTURE, UNION -> new Builder(target).deserializeMember(deserializer, member).build();
            default -> throw new IllegalStateException("Unsupported kind " + target.type());
        };
    }

    /** A builder of one structure or union schema. */
    static final class Builder implements ShapeBuilder<GenericStruct> {
        private final Schema schema;
        private final Map<String, Object> values = new HashMap<>();

        Builder(Schema schema) {
            if (schema.type() != ShapeType.STRUCTURE && schema.type() != ShapeType.UNION) {
                throw new IllegalArgumentException("Not an aggregate schema: " + schema.id());
            }
            this.schema = schema;
        }

        @Override
        public Schema schema() {
            return schema;
        }

        @Override
        public ShapeBuilder<GenericStruct> deserialize(ShapeDeserializer decoder) {
            return deserializeMember(decoder, schema);
        }

        @Override
        public ShapeBuilder<GenericStruct> deserializeMember(ShapeDeserializer decoder, Schema readAs) {
            decoder.readStruct(readAs, this, (builder, member, value) -> {
                if (!value.isNull()) {
                    builder.values.put(member.memberName(), read(value, member));
                }
            });
            return this;
        }

        @Override
        public void setMemberValue(Schema member, Object value) {
            values.put(member.memberName(), value);
        }

        @Override
        public GenericStruct build() {
            return new GenericStruct(schema, values);
        }
    }
}
