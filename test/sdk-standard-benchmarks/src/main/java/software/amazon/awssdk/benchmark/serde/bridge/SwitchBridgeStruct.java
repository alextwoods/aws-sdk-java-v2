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

package software.amazon.awssdk.benchmark.serde.bridge;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.core.SdkField;
import software.amazon.awssdk.core.SdkPojo;
import software.amazon.awssdk.core.protocol.MarshallingType;
import software.amazon.smithy.java.core.schema.Schema;
import software.amazon.smithy.java.core.schema.SerializableStruct;
import software.amazon.smithy.java.core.schema.TraitKey;
import software.amazon.smithy.java.core.serde.ShapeSerializer;
import software.amazon.smithy.java.io.datastream.DataStream;
import software.amazon.smithy.model.traits.HttpPayloadTrait;

/**
 * Variant of {@link BridgeStruct} that attacks the one thing separating the bridge from
 * generated-style code: <b>megamorphic dispatch</b>. Instead of a per-member {@code BiConsumer}
 * lambda (a megamorphic call site), each plan entry carries an {@code int} type code and the
 * hot loop is a {@code switch} — a monomorphic dispatch the JIT handles far better. It also
 * reads values via {@code SdkField.get()} directly, skipping {@code getValueOrDefault}'s
 * per-call {@code DefaultValueTrait} lookup.
 *
 * <p>This is the "can a cache get us to ~generated speed without codegen?" experiment: still
 * zero codegen, still a precompiled cached plan — just a switch instead of lambdas.
 */
final class SwitchBridgeStruct implements SerializableStruct {

    private static final TraitKey<HttpPayloadTrait> HTTP_PAYLOAD = TraitKey.get(HttpPayloadTrait.class);
    private static final Map<String, Entry[]> CACHE = new ConcurrentHashMap<>();

    private static final int T_STRING = 0;
    private static final int T_INTEGER = 1;
    private static final int T_LONG = 2;
    private static final int T_BOOLEAN = 3;
    private static final int T_INSTANT = 4;
    private static final int T_DOUBLE = 5;
    private static final int T_BLOB_PAYLOAD = 6;
    private static final int T_BLOB = 7;
    private static final int T_STRUCT = 8;
    private static final int T_ENUM = 9; // v2 enum -> writeString(value.getValue()) handled via STRING getter
    private static final int T_SKIP = -1;

    private final Schema schema;
    private final SdkPojo pojo;
    private final Entry[] plan;

    private SwitchBridgeStruct(Schema schema, SdkPojo pojo, Entry[] plan) {
        this.schema = schema;
        this.pojo = pojo;
        this.plan = plan;
    }

    static SwitchBridgeStruct of(Schema schema, SdkPojo pojo) {
        Entry[] plan = CACHE.computeIfAbsent(schema.id() + "|" + pojo.getClass().getName(),
                k -> compile(schema, pojo));
        return new SwitchBridgeStruct(schema, pojo, plan);
    }

    @Override
    public Schema schema() {
        return schema;
    }

    @Override
    @SuppressWarnings("unchecked")
    public void serializeMembers(ShapeSerializer s) {
        Entry[] p = plan;
        for (int i = 0; i < p.length; i++) {
            Entry e = p[i];
            Object v = e.field.getValueOrDefault(pojo);
            if (v == null) {
                continue;
            }
            switch (e.type) {
                case T_STRING -> s.writeString(e.member, (String) v);
                case T_INTEGER -> s.writeInteger(e.member, (Integer) v);
                case T_LONG -> s.writeLong(e.member, (Long) v);
                case T_BOOLEAN -> s.writeBoolean(e.member, (Boolean) v);
                case T_INSTANT -> s.writeTimestamp(e.member, (java.time.Instant) v);
                case T_DOUBLE -> s.writeDouble(e.member, (Double) v);
                case T_BLOB_PAYLOAD -> s.writeDataStream(e.member, DataStream.ofByteBuffer(((SdkBytes) v).asByteBuffer()));
                case T_BLOB -> s.writeBlob(e.member, ((SdkBytes) v).asByteBuffer());
                case T_STRUCT -> s.writeStruct(e.member, SwitchBridgeStruct.of(e.member, (SdkPojo) v));
                default -> {
                }
            }
        }
    }

    @Override
    public <T> T getMemberValue(Schema member) {
        for (Entry e : plan) {
            if (e.field.memberName().equals(member.memberName())) {
                @SuppressWarnings("unchecked")
                T v = (T) e.field.getValueOrDefault(pojo);
                return v;
            }
        }
        return null;
    }

    private record Entry(int type, Schema member, SdkField<?> field) {}

    private static Entry[] compile(Schema schema, SdkPojo pojo) {
        List<Entry> entries = new ArrayList<>();
        for (SdkField<?> f : pojo.sdkFields()) {
            Schema member = schema.member(f.memberName());
            if (member == null) {
                continue;
            }
            int type = typeCode(member, f.marshallingType());
            if (type != T_SKIP) {
                entries.add(new Entry(type, member, f));
            }
        }
        return entries.toArray(new Entry[0]);
    }

    private static int typeCode(Schema member, MarshallingType<?> t) {
        if (t == MarshallingType.STRING) {
            return T_STRING;
        } else if (t == MarshallingType.INTEGER) {
            return T_INTEGER;
        } else if (t == MarshallingType.LONG) {
            return T_LONG;
        } else if (t == MarshallingType.BOOLEAN) {
            return T_BOOLEAN;
        } else if (t == MarshallingType.INSTANT) {
            return T_INSTANT;
        } else if (t == MarshallingType.DOUBLE) {
            return T_DOUBLE;
        } else if (t == MarshallingType.SDK_BYTES) {
            return member.getTrait(HTTP_PAYLOAD) != null ? T_BLOB_PAYLOAD : T_BLOB;
        } else if (t == MarshallingType.SDK_POJO) {
            return T_STRUCT;
        }
        // Lists/maps/enums/etc. not exercised by the PutObject hot path — skip for this experiment.
        return T_SKIP;
    }
}
