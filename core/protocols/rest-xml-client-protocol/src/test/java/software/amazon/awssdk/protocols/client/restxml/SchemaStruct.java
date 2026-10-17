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

package software.amazon.awssdk.protocols.client.restxml;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import software.amazon.awssdk.schema.Schema;
import software.amazon.awssdk.schema.SerializableStruct;
import software.amazon.awssdk.schema.ShapeBuilder;
import software.amazon.awssdk.schema.serde.ShapeDeserializer;
import software.amazon.awssdk.schema.serde.ShapeSerializer;
import software.amazon.awssdk.schema.serde.document.Document;

/**
 * A structure or union whose member values are held by member index and written and read by member type, standing in
 * for a generated model class in the codec tests. Lists are {@link List}s, maps are string-keyed {@link Map}s, blobs
 * are {@link ByteBuffer}s, enums are strings, intEnums are integers and nested structures are {@code SchemaStruct}s.
 * Copied from the cbor-codec tests, plus enum and intEnum members.
 */
final class SchemaStruct implements SerializableStruct {
    private final Schema schema;
    private final Object[] values;

    private SchemaStruct(Schema schema, Object[] values) {
        this.schema = schema;
        this.values = values.clone();
    }

    static Builder builder(Schema schema) {
        return new Builder(schema);
    }

    Object get(String member) {
        return values[schema.member(member).memberIndex()];
    }

    @Override
    public Schema sdkSchema() {
        return schema;
    }

    @Override
    public void serializeMembers(ShapeSerializer serializer) {
        for (Schema member : schema.members()) {
            Object value = values[member.memberIndex()];
            if (value != null) {
                write(serializer, member, value);
            }
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T getMemberValue(Schema member) {
        return (T) values[member.memberIndex()];
    }

    @SuppressWarnings("unchecked")
    static void write(ShapeSerializer serializer, Schema schema, Object value) {
        switch (schema.type()) {
            case BOOLEAN:
                serializer.writeBoolean(schema, (Boolean) value);
                break;
            case BYTE:
                serializer.writeByte(schema, (Byte) value);
                break;
            case SHORT:
                serializer.writeShort(schema, (Short) value);
                break;
            case INT_ENUM:
            case INTEGER:
                serializer.writeInteger(schema, (Integer) value);
                break;
            case LONG:
                serializer.writeLong(schema, (Long) value);
                break;
            case FLOAT:
                serializer.writeFloat(schema, (Float) value);
                break;
            case DOUBLE:
                serializer.writeDouble(schema, (Double) value);
                break;
            case BIG_INTEGER:
                serializer.writeBigInteger(schema, (BigInteger) value);
                break;
            case BIG_DECIMAL:
                serializer.writeBigDecimal(schema, (BigDecimal) value);
                break;
            case ENUM:
            case STRING:
                serializer.writeString(schema, (String) value);
                break;
            case BLOB:
                serializer.writeBlob(schema, (ByteBuffer) value);
                break;
            case TIMESTAMP:
                serializer.writeTimestamp(schema, (Instant) value);
                break;
            case DOCUMENT:
                serializer.writeDocument(schema, (Document) value);
                break;
            case LIST:
                List<Object> list = (List<Object>) value;
                serializer.writeList(schema, list, list.size(), (elements, s) -> {
                    for (Object element : elements) {
                        if (element == null) {
                            s.writeNull(schema.listMember());
                        } else {
                            write(s, schema.listMember(), element);
                        }
                    }
                });
                break;
            case MAP:
                Map<String, Object> map = (Map<String, Object>) value;
                serializer.writeMap(schema, map, map.size(), (entries, ms) -> {
                    for (Map.Entry<String, Object> entry : entries.entrySet()) {
                        ms.writeEntry(schema.mapKeyMember(), entry.getKey(), entry.getValue(),
                                      (v, s) -> {
                                          if (v == null) {
                                              s.writeNull(schema.mapValueMember());
                                          } else {
                                              write(s, schema.mapValueMember(), v);
                                          }
                                      });
                    }
                });
                break;
            case STRUCTURE:
            case UNION:
                serializer.writeStruct(schema, (SchemaStruct) value);
                break;
            default:
                throw new IllegalArgumentException("Unsupported type " + schema.type());
        }
    }

    static Object read(ShapeDeserializer de, Schema schema) {
        switch (schema.type()) {
            case BOOLEAN:
                return de.readBoolean(schema);
            case BYTE:
                return de.readByte(schema);
            case SHORT:
                return de.readShort(schema);
            case INT_ENUM:
            case INTEGER:
                return de.readInteger(schema);
            case LONG:
                return de.readLong(schema);
            case FLOAT:
                return de.readFloat(schema);
            case DOUBLE:
                return de.readDouble(schema);
            case BIG_INTEGER:
                return de.readBigInteger(schema);
            case BIG_DECIMAL:
                return de.readBigDecimal(schema);
            case ENUM:
            case STRING:
                return de.readString(schema);
            case BLOB:
                return de.readBlob(schema);
            case TIMESTAMP:
                return de.readTimestamp(schema);
            case DOCUMENT:
                return de.readDocument();
            case LIST:
                List<Object> list = new ArrayList<>();
                de.readList(schema, list, (l, d) -> l.add(d.isNull() ? d.readNull() : read(d, schema.listMember())));
                return list;
            case MAP:
                Map<String, Object> map = new LinkedHashMap<>();
                de.readStringMap(schema, map, (m, k, d) -> m.put(k, d.isNull() ? d.readNull()
                                                                         : read(d, schema.mapValueMember())));
                return map;
            case STRUCTURE:
            case UNION:
                return builder(schema.isMember() ? schema.memberTarget() : schema).deserialize(de).build();
            default:
                throw new IllegalArgumentException("Unsupported type " + schema.type());
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof SchemaStruct)) {
            return false;
        }
        SchemaStruct that = (SchemaStruct) o;
        return schema == that.schema && Arrays.equals(values, that.values);
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(values);
    }

    @Override
    public String toString() {
        return schema.id() + Arrays.toString(values);
    }

    static final class Builder implements ShapeBuilder<SchemaStruct> {
        private final Schema schema;
        private final Object[] values;
        private final List<String> unknownMembers = new ArrayList<>();

        private Builder(Schema schema) {
            this.schema = schema;
            this.values = new Object[schema.members().size()];
        }

        Builder set(String member, Object value) {
            values[schema.member(member).memberIndex()] = value;
            return this;
        }

        List<String> unknownMembers() {
            return unknownMembers;
        }

        @Override
        public Schema sdkSchema() {
            return schema;
        }

        @Override
        public Builder deserialize(ShapeDeserializer decoder) {
            decoder.readStruct(schema, this, new ShapeDeserializer.StructMemberConsumer<Builder>() {
                @Override
                public void accept(Builder builder, Schema member, ShapeDeserializer de) {
                    builder.values[member.memberIndex()] = read(de, member);
                }

                @Override
                public void unknownMember(Builder builder, String memberName) {
                    builder.unknownMembers.add(memberName);
                }
            });
            return this;
        }

        @Override
        public SchemaStruct build() {
            return new SchemaStruct(schema, values);
        }
    }
}
