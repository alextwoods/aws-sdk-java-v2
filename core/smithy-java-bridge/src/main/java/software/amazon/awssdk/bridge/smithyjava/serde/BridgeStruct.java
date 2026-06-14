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
import java.util.concurrent.ConcurrentHashMap;
import software.amazon.awssdk.annotations.SdkPublicApi;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.core.SdkField;
import software.amazon.awssdk.core.SdkPojo;
import software.amazon.awssdk.core.protocol.MarshallingKnownType;
import software.amazon.awssdk.core.protocol.MarshallingType;
import software.amazon.smithy.java.core.schema.Schema;
import software.amazon.smithy.java.core.schema.SerializableStruct;
import software.amazon.smithy.java.core.schema.TraitKey;
import software.amazon.smithy.java.core.serde.ShapeSerializer;
import software.amazon.smithy.java.io.datastream.DataStream;
import software.amazon.smithy.model.traits.HttpPayloadTrait;

/**
 * Adapts an AWS SDK v2 {@link SdkPojo} to a smithy-java {@link SerializableStruct} that
 * serializes <b>directly and reflection-free</b> into the smithy-java {@link ShapeSerializer}.
 *
 * <p>This is the bridge's request-serialization path in the "v2 SDK as a shell over
 * smithy-java" architecture. It does the same amount of work a generated smithy-java shape
 * does, reading values from the v2 POJO instead of generated fields:
 *
 * <ul>
 *   <li><b>No JVM reflection.</b> Values are read through v2's {@code SdkField} getters,
 *       which are generated lambdas / method references — not {@code Method.invoke}.</li>
 *   <li><b>No per-call lookups.</b> The {@code member-name -> Schema} resolution comes from a
     *       {@link BridgeSchemaExtension} extension cached on the Schema (the same mechanism the
 *       JSON/XML/CBOR codecs use). The typed write action per member is compiled once per
 *       (schema, pojo-class) into a flat plan and cached.</li>
 * </ul>
 *
 * <p>The hot path ({@link #serializeMembers}) is just: iterate the precompiled plan, read
 * each value via its getter, invoke its captured write action.
 */
@SdkPublicApi
public final class BridgeStruct implements SerializableStruct {

    private static final Map<String, MemberWriter[]> PLAN_CACHE = new ConcurrentHashMap<>();
    private static final TraitKey<HttpPayloadTrait> HTTP_PAYLOAD = TraitKey.get(HttpPayloadTrait.class);
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
    private static final int BLOB_PAYLOAD = 11;
    private static final int STRUCT = 12;
    private static final int LIST = 13;
    private static final int MAP = 14;
    private static final int UNSUPPORTED = -1;

    private final Schema schema;
    private final SdkPojo pojo;
    private final MemberWriter[] plan;

    private BridgeStruct(Schema schema, SdkPojo pojo, MemberWriter[] plan) {
        this.schema = schema;
        this.pojo = pojo;
        this.plan = plan;
    }

    /** Wrap a v2 {@link SdkPojo} for serialization against the given smithy struct schema. */
    public static BridgeStruct of(Schema schema, SdkPojo pojo) {
        // Look up the compiled plan via the Schema's bridge extension, keyed by pojo Class
        // identity (no per-element string-key build + hash — that was ~9% of serialize CPU).
        BridgeSchemaExtension.BridgePlan ext = schema.getExtension(BridgeSchemaExtension.KEY);
        MemberWriter[] plan = ext != null
                ? ext.serializePlan(pojo.getClass(), k -> compile(schema, pojo))
                : PLAN_CACHE.computeIfAbsent(schema.id() + "|" + pojo.getClass().getName(),
                        k -> compile(schema, pojo));
        return new BridgeStruct(schema, pojo, plan);
    }

    @Override
    public Schema schema() {
        return schema;
    }

    @Override
    public void serializeMembers(ShapeSerializer serializer) {
        for (MemberWriter w : plan) {
            Object value = w.getter.getValueOrDefault(pojo);
            if (value != null) {
                w.write(serializer, value);
            }
        }
    }

    @Override
    public <T> T getMemberValue(Schema member) {
        for (MemberWriter w : plan) {
            if (w.getter.memberName().equals(member.memberName())) {
                @SuppressWarnings("unchecked")
                T value = (T) w.getter.getValueOrDefault(pojo);
                return value;
            }
        }
        return null;
    }

    private static final class MemberWriter {
        final SdkField<?> getter;
        final Schema member;
        final int type;
        final Schema keyMember;
        final Schema valueMember;

        MemberWriter(SdkField<?> getter, Schema member, int type) {
            this.getter = getter;
            this.member = member;
            this.type = type;
            this.keyMember = type == MAP ? member.member("key") : null;
            this.valueMember = switch (type) {
                case LIST -> member.member("member");
                case MAP -> member.member("value");
                default -> null;
            };
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
                case BLOB_PAYLOAD:
                    s.writeDataStream(member, DataStream.ofByteBuffer(((SdkBytes) v).asByteBuffer()));
                    break;
                case STRUCT:
                    s.writeStruct(member, BridgeStruct.of(member, (SdkPojo) v));
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

    /** Compile the plan once: resolve each member Schema (via the cached extension) + its write op. */
    private static MemberWriter[] compile(Schema schema, SdkPojo pojo) {
        // Fast member resolution from the schema extension (JSON/XML/CBOR use the same pattern).
        BridgeSchemaExtension.BridgePlan ext = schema.getExtension(BridgeSchemaExtension.KEY);
        List<MemberWriter> writers = new ArrayList<>();
        for (SdkField<?> field : pojo.sdkFields()) {
            Schema member = ext != null ? ext.member(field.memberName()) : schema.member(field.memberName());
            if (member == null) {
                continue;
            }
            int type = typeCode(member, field.marshallingType());
            if (type >= 0) {
                writers.add(new MemberWriter(field, member, type));
            }
        }
        return writers.toArray(new MemberWriter[0]);
    }

    private static int typeCode(Schema member, MarshallingType<?> type) {
        int code = marshallingTypeCode(type);
        return switch (code) {
            // A blob bound as @httpPayload is handed off by reference via writeDataStream — the
            // zero-alloc path. Writing it via writeBlob(ByteBuffer) instead would make the HTTP
            // binding serializer spin up a fresh JSON serializer (an 8 KiB buffer) per call.
            // Non-payload blobs (header/query) must still go through writeBlob for base64 binding.
            case BLOB -> member.getTrait(HTTP_PAYLOAD) != null ? BLOB_PAYLOAD : BLOB;
            default -> code;
        };
    }

    private static int marshallingTypeCode(MarshallingType<?> type) {
        MarshallingKnownType knownType = type.getKnownType();
        if (knownType == null) {
            return UNSUPPORTED;
        }
        return switch (knownType) {
            case STRING -> STRING;
            case INTEGER -> INTEGER;
            case LONG -> LONG;
            case SHORT -> SHORT;
            case BYTE -> BYTE;
            case FLOAT -> FLOAT;
            case DOUBLE -> DOUBLE;
            case BIG_DECIMAL -> BIG_DECIMAL;
            case BOOLEAN -> BOOLEAN;
            case INSTANT -> INSTANT;
            case SDK_BYTES -> BLOB;
            case SDK_POJO -> STRUCT;
            case LIST -> LIST;
            case MAP -> MAP;
            default -> UNSUPPORTED;
        };
    }

    private static void writeList(ShapeSerializer s, Schema member, Schema element, List<?> list) {
        s.writeList(member, list, list.size(), (lst, ser) -> {
            for (Object e : lst) {
                writeElement(ser, element, e);
            }
        });
    }

    private static void writeMap(ShapeSerializer s, Schema member, Schema keySchema, Schema valueSchema,
                                 Map<?, ?> map) {
        s.writeMap(member, map, map.size(), (m, mapSer) -> {
            for (Map.Entry<?, ?> e : m.entrySet()) {
                mapSer.writeEntry(keySchema, String.valueOf(e.getKey()), valueSchema,
                        (vs, vser) -> writeElement(vser, vs, e.getValue()));
            }
        });
    }

    private static void writeElement(ShapeSerializer s, Schema schema, Object value) {
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
        } else if (value instanceof java.math.BigDecimal v) {
            s.writeBigDecimal(schema, v);
        } else if (value instanceof Double v) {
            s.writeDouble(schema, v);
        } else if (value instanceof SdkBytes v) {
            s.writeBlob(schema, v.asByteArray());
        } else if (value instanceof SdkPojo v) {
            s.writeStruct(schema, BridgeStruct.of(schema, v));
        } else if (value instanceof List<?> v) {
            writeList(s, schema, schema.member("member"), v);
        } else if (value instanceof Map<?, ?> v) {
            writeMap(s, schema, schema.member("key"), schema.member("value"), v);
        } else {
            s.writeString(schema, String.valueOf(value));
        }
    }
}
