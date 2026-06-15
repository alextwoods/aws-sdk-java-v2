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

package software.amazon.awssdk.bridge.smithyjava.serde;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import software.amazon.awssdk.annotations.SdkProtectedApi;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.core.SdkField;
import software.amazon.awssdk.core.SdkPojo;
import software.amazon.awssdk.core.protocol.MarshallingKnownType;
import software.amazon.awssdk.core.protocol.MarshallingType;
import software.amazon.smithy.java.core.schema.Schema;
import software.amazon.smithy.java.core.schema.SerializableStruct;
import software.amazon.smithy.java.core.serde.ShapeSerializer;

/**
 * Serializes an AWS SDK v2 {@link SdkPojo} into a smithy-java {@link ShapeSerializer}, driven by a
 * <b>precompiled plan</b> built once from the shape's static {@code $SCHEMA} and {@code SDK_FIELDS}.
 *
 * <p>A generated POJO that implements {@code SerializableStruct} holds the plan in a
 * {@code private static final} field and calls {@link Plan#serialize} from its
 * {@code serializeMembers}. So compared with the runtime {@link BridgeStruct}:
 *
 * <ul>
 *   <li><b>No per-call wrapper allocation.</b> The POJO <i>is</i> the {@code SerializableStruct};
 *       nested shapes are {@code SerializableStruct}s too, serialized by direct cast — no
 *       {@code BridgeStruct.of(...)} allocation per nested struct / map entry / list element.</li>
 *   <li><b>No per-call plan lookup.</b> The plan is a static field, not resolved from a
 *       {@code ClassValue} on the Schema each call.</li>
 *   <li><b>Monomorphic dispatch.</b> Each member's write op is an {@code int} type code resolved
 *       once; the hot loop is an int-switch (a jump table), and list/map element schemas are
 *       resolved at compile time, not per call.</li>
 * </ul>
 *
 * <p>The plan pairs each {@code SDK_FIELDS} member with its smithy member {@link Schema} by index:
 * {@code $SCHEMA} is built from the same {@code SDK_FIELDS} (see {@link SdkSchemaFactory}), so
 * member <i>i</i> in {@code SDK_FIELDS} is member <i>i</i> in the schema.
 */
@SdkProtectedApi
public final class SdkPojoSerializer {

    private static final int STRING = 0;
    private static final int INTEGER = 1;
    private static final int LONG = 2;
    private static final int SHORT = 3;
    private static final int BYTE = 4;
    private static final int FLOAT = 5;
    private static final int DOUBLE = 6;
    private static final int BIG_DECIMAL = 7;
    private static final int BOOLEAN = 8;
    private static final int INSTANT = 9;
    private static final int BLOB = 10;
    private static final int STRUCT = 11;
    private static final int LIST = 12;
    private static final int MAP = 13;
    private static final int UNSUPPORTED = -1;

    private SdkPojoSerializer() {
    }

    /**
     * Compile a serialize plan once for a shape. Member order is the {@code SDK_FIELDS} order,
     * which equals {@code $SCHEMA}'s member order, so members are paired by index.
     */
    public static Plan compile(Schema schema, List<SdkField<?>> sdkFields) {
        List<Schema> members = schema.members();
        List<MemberWriter> writers = new ArrayList<>(sdkFields.size());
        for (int i = 0; i < sdkFields.size(); i++) {
            SdkField<?> field = sdkFields.get(i);
            Schema member = members.get(i);
            int type = typeCode(field.marshallingType());
            if (type >= 0) {
                writers.add(new MemberWriter(field, member, type));
            }
        }
        return new Plan(writers.toArray(new MemberWriter[0]));
    }

    /**
     * A precompiled, immutable serialize plan for one shape. Held in a static field by the
     * generated POJO; thread-safe and shared across all instances of that shape.
     */
    public static final class Plan {
        private final MemberWriter[] writers;

        private Plan(MemberWriter[] writers) {
            this.writers = writers;
        }

        /** Serialize {@code pojo}'s members — the generated {@code serializeMembers} body. */
        public void serialize(ShapeSerializer serializer, SdkPojo pojo) {
            for (MemberWriter w : writers) {
                Object value = w.getter.getValueOrDefault(pojo);
                if (value != null) {
                    w.write(serializer, value);
                }
            }
        }

        /** Look up a single member value — the generated {@code getMemberValue} body. */
        @SuppressWarnings("unchecked")
        public <T> T getMemberValue(SdkPojo pojo, Schema member) {
            String name = member.memberName();
            for (MemberWriter w : writers) {
                if (w.getter.memberName().equals(name)) {
                    return (T) w.getter.getValueOrDefault(pojo);
                }
            }
            return null;
        }
    }

    private static final class MemberWriter {
        private final SdkField<?> getter;
        private final Schema member;
        private final int type;
        private final Schema keyMember;
        private final Schema valueMember;

        MemberWriter(SdkField<?> getter, Schema member, int type) {
            this.getter = getter;
            this.member = member;
            this.type = type;
            this.keyMember = type == MAP ? member.member("key") : null;
            switch (type) {
                case LIST:
                    this.valueMember = member.member("member");
                    break;
                case MAP:
                    this.valueMember = member.member("value");
                    break;
                default:
                    this.valueMember = null;
                    break;
            }
        }

        void write(ShapeSerializer s, Object v) {
            switch (type) {
                case STRING:
                    s.writeString(member, (String) v);
                    break;
                case INTEGER:
                    s.writeInteger(member, (Integer) v);
                    break;
                case LONG:
                    s.writeLong(member, (Long) v);
                    break;
                case SHORT:
                    s.writeShort(member, (Short) v);
                    break;
                case BYTE:
                    s.writeByte(member, (Byte) v);
                    break;
                case FLOAT:
                    s.writeFloat(member, (Float) v);
                    break;
                case DOUBLE:
                    s.writeDouble(member, (Double) v);
                    break;
                case BIG_DECIMAL:
                    s.writeBigDecimal(member, (java.math.BigDecimal) v);
                    break;
                case BOOLEAN:
                    s.writeBoolean(member, (Boolean) v);
                    break;
                case INSTANT:
                    s.writeTimestamp(member, (java.time.Instant) v);
                    break;
                case BLOB:
                    s.writeBlob(member, ((SdkBytes) v).asByteBuffer());
                    break;
                case STRUCT:
                    // Nested shape is itself a SerializableStruct (generated) — serialize by direct
                    // cast, no wrapper allocation.
                    s.writeStruct(member, (SerializableStruct) v);
                    break;
                case LIST:
                    writeList(s, member, valueMember, (List<?>) v);
                    break;
                case MAP:
                    writeMap(s, member, keyMember, valueMember, (Map<?, ?>) v);
                    break;
                default:
                    break;
            }
        }
    }

    private static int typeCode(MarshallingType<?> type) {
        MarshallingKnownType knownType = type.getKnownType();
        if (knownType == null) {
            return UNSUPPORTED;
        }
        switch (knownType) {
            case STRING: return STRING;
            case INTEGER: return INTEGER;
            case LONG: return LONG;
            case SHORT: return SHORT;
            case BYTE: return BYTE;
            case FLOAT: return FLOAT;
            case DOUBLE: return DOUBLE;
            case BIG_DECIMAL: return BIG_DECIMAL;
            case BOOLEAN: return BOOLEAN;
            case INSTANT: return INSTANT;
            case SDK_BYTES: return BLOB;
            case SDK_POJO: return STRUCT;
            case LIST: return LIST;
            case MAP: return MAP;
            default: return UNSUPPORTED;
        }
    }

    /**
     * Public entry point for generated {@code serializeMembers}: write a list member given only its
     * member {@link Schema}. The element schema is resolved via the fast {@code listMember()}
     * accessor (cached on the schema), so the generated call site stays a one-liner.
     */
    public static void writeList(ShapeSerializer s, Schema member, List<?> list) {
        writeList(s, member, member.listMember(), list);
    }

    /** Public entry point for generated {@code serializeMembers}: write a map member by schema. */
    public static void writeMap(ShapeSerializer s, Schema member, Map<?, ?> map) {
        writeMap(s, member, member.mapKeyMember(), member.mapValueMember(), map);
    }

    private static void writeList(ShapeSerializer s, Schema member, Schema element, List<?> list) {
        s.writeList(member, list, list.size(), (lst, ser) -> {
            for (Object e : lst) {
                writeElement(ser, element, e);
            }
        });
    }

    private static void writeMap(ShapeSerializer s, Schema member, Schema key, Schema value, Map<?, ?> map) {
        s.writeMap(member, map, map.size(), (m, mapSer) -> {
            for (Map.Entry<?, ?> e : m.entrySet()) {
                mapSer.writeEntry(key, String.valueOf(e.getKey()), value,
                        (vs, vser) -> writeElement(vser, vs, e.getValue()));
            }
        });
    }

    /** Public element writer for generated code / fallback members (dispatches on value type). */
    public static void writeElement(ShapeSerializer s, Schema schema, Object value) {
        if (value == null) {
            return;
        }
        if (value instanceof String v) {
            s.writeString(schema, v);
        } else if (value instanceof Integer v) {
            s.writeInteger(schema, v);
        } else if (value instanceof Long v) {
            s.writeLong(schema, v);
        } else if (value instanceof Boolean v) {
            s.writeBoolean(schema, v);
        } else if (value instanceof Double v) {
            s.writeDouble(schema, v);
        } else if (value instanceof java.math.BigDecimal v) {
            s.writeBigDecimal(schema, v);
        } else if (value instanceof SdkBytes v) {
            s.writeBlob(schema, v.asByteBuffer());
        } else if (value instanceof SerializableStruct v) {
            // Nested generated struct — serialize by direct cast, no wrapper allocation.
            s.writeStruct(schema, v);
        } else if (value instanceof List<?> v) {
            writeList(s, schema, schema.member("member"), v);
        } else if (value instanceof Map<?, ?> v) {
            writeMap(s, schema, schema.member("key"), schema.member("value"), v);
        } else {
            s.writeString(schema, String.valueOf(value));
        }
    }
}
