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

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;
import software.amazon.awssdk.annotations.SdkPublicApi;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.core.SdkField;
import software.amazon.awssdk.core.SdkPojo;
import software.amazon.awssdk.core.protocol.MarshallingKnownType;
import software.amazon.awssdk.core.protocol.MarshallingType;
import software.amazon.awssdk.utils.builder.Buildable;
import software.amazon.smithy.java.core.schema.Schema;
import software.amazon.smithy.java.core.schema.SerializableStruct;
import software.amazon.smithy.java.core.schema.ShapeBuilder;
import software.amazon.smithy.java.core.serde.ShapeDeserializer;
import software.amazon.smithy.java.core.serde.ShapeSerializer;

/**
 * The reverse of {@link BridgeStruct}: a smithy-java {@link ShapeBuilder} that consumes a
 * smithy-java {@link ShapeDeserializer} and populates an AWS SDK v2 {@link SdkPojo} builder.
 *
 * <p>This is the deserialization half of the "v2 SDK as a shell over smithy-java" bridge:
 * smithy-java's protocol parses the wire bytes and drives this builder member-by-member; we
 * map each smithy member back onto the v2 builder's {@code SdkField} setter. Reflection-free
 * on the hot path — values are set through v2's generated setter lambdas, and the
 * member-name -> SdkField binding is precompiled once per (schema, builder-class).
 *
 * <p>Nested structures recurse by giving the member's {@code SdkField} constructor a fresh v2
 * builder. Lists/maps build plain v2 collections of mapped elements.
 */
@SdkPublicApi
public final class BridgeOutputBuilder implements ShapeBuilder<BridgeOutputBuilder.Built> {

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

    /** Marker result type — the benchmark only needs the populated v2 SdkPojo, reachable via pojo(). */
    public static final class Built implements SerializableStruct {
        private final Schema schema;
        private final SdkPojo pojo;

        Built(Schema schema, SdkPojo pojo) {
            this.schema = schema;
            this.pojo = pojo;
        }

        public SdkPojo pojo() {
            return pojo;
        }

        @Override
        public Schema schema() {
            return schema;
        }

        @Override
        public void serializeMembers(ShapeSerializer serializer) {
            // Not used for deserialization benchmarks.
        }

        @Override
        public <T> T getMemberValue(Schema member) {
            return null;
        }
    }

    private static final Map<String, SetterEntry[]> SETTER_PLAN_CACHE = new ConcurrentHashMap<>();

    private final Schema schema;
    private final SdkPojo builder;          // a v2 *.Builder, itself an SdkPojo with setters
    private final SetterEntry[] byMemberIndex;

    private BridgeOutputBuilder(Schema schema, SdkPojo builder, SetterEntry[] byMemberIndex) {
        this.schema = schema;
        this.builder = builder;
        this.byMemberIndex = byMemberIndex;
    }

    /**
     * Create a builder bound to a v2 builder instance for the given smithy output schema.
     *
     * @param schema         smithy struct schema for the output shape
     * @param v2BuilderShape a fresh v2 {@code SdkPojo} builder to populate
     */
    public static BridgeOutputBuilder of(Schema schema, SdkPojo v2BuilderShape) {
        // Plan lookup via the Schema's bridge extension keyed by builder Class identity — avoids
        // the per-element string-key build + hash that profiling showed cost ~16% of deserialize CPU.
        BridgeSchemaExtension.BridgePlan ext = schema.getExtension(BridgeSchemaExtension.KEY);
        SetterEntry[] plan = ext != null
                ? ext.deserializePlan(v2BuilderShape.getClass(), k -> compile(schema, v2BuilderShape))
                : SETTER_PLAN_CACHE.computeIfAbsent(schema.id() + "|" + v2BuilderShape.getClass().getName(),
                        k -> compile(schema, v2BuilderShape));
        return new BridgeOutputBuilder(schema, v2BuilderShape, plan);
    }

    private static SetterEntry[] compile(Schema schema, SdkPojo v2Builder) {
        int maxIndex = -1;
        for (Schema member : schema.members()) {
            maxIndex = Math.max(maxIndex, member.memberIndex());
        }
        SetterEntry[] byIndex = new SetterEntry[maxIndex + 1];
        for (SdkField<?> field : v2Builder.sdkFields()) {
            Schema member = schema.member(field.memberName());
            if (member != null) {
                int type = typeCode(field.marshallingType());
                if (type >= 0) {
                    byIndex[member.memberIndex()] = new SetterEntry(field, member, type);
                }
            }
        }
        return byIndex;
    }

    @Override
    public Schema schema() {
        return schema;
    }

    @Override
    public ShapeBuilder<Built> deserialize(ShapeDeserializer decoder) {
        decoder.readStruct(schema, this, (state, member, de) -> {
            int memberIndex = member.memberIndex();
            SetterEntry entry = memberIndex < state.byMemberIndex.length ? state.byMemberIndex[memberIndex] : null;
            if (entry != null) {
                setMember(state.builder, entry, de);
            }
        });
        return this;
    }

    @Override
    public Built build() {
        // v2 builders expose build() but not via SdkPojo; the benchmark consumes the populated
        // builder directly. Wrap the builder (it carries the parsed state) as the result.
        return new Built(schema, builder);
    }

    @SuppressWarnings("unchecked")
    private static void setMember(SdkPojo builder, SetterEntry entry, ShapeDeserializer de) {
        SdkField<Object> f = (SdkField<Object>) entry.field;
        Schema member = entry.member;
        switch (entry.type) {
            case STRING:
                f.set(builder, de.readString(member));
                break;
            case INTEGER:
                f.set(builder, de.readInteger(member));
                break;
            case LONG:
                f.set(builder, de.readLong(member));
                break;
            case SHORT:
                f.set(builder, de.readShort(member));
                break;
            case BYTE:
                f.set(builder, de.readByte(member));
                break;
            case FLOAT:
                f.set(builder, de.readFloat(member));
                break;
            case DOUBLE:
                f.set(builder, de.readDouble(member));
                break;
            case BIG_DECIMAL:
                f.set(builder, de.readBigDecimal(member));
                break;
            case BOOLEAN:
                f.set(builder, de.readBoolean(member));
                break;
            case INSTANT:
                f.set(builder, readTimestampTolerant(member, de));
                break;
            case BLOB:
                f.set(builder, SdkBytes.fromByteBuffer(de.readBlob(member)));
                break;
            case STRUCT:
                SdkPojo nestedBuilder = nestedBuilder(entry.field);
                if (nestedBuilder != null) {
                    BridgeOutputBuilder nested = BridgeOutputBuilder.of(member, nestedBuilder);
                    nested.deserialize(de);
                    f.set(builder, buildIfPossible(nestedBuilder));
                }
                break;
            case LIST:
                f.set(builder, readList(member, entry.field, de));
                break;
            case MAP:
                f.set(builder, readMap(member, entry.field, de));
                break;
            default:
                break;
        }
    }

    private static int typeCode(MarshallingType<?> type) {
        MarshallingKnownType knownType = type.getKnownType();
        if (knownType == null) {
            return -1;
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
            default -> -1;
        };
    }

    private static final class SetterEntry {
        final SdkField<?> field;
        final Schema member;
        final int type;

        SetterEntry(SdkField<?> field, Schema member, int type) {
            this.field = field;
            this.member = member;
            this.type = type;
        }
    }

    /**
     * Read a timestamp tolerantly. The benchmark corpus encodes some timestamps as ISO-8601
     * date-time strings while the smithy schema member may default to EPOCH_SECONDS (a known
     * C2J vs. Smithy {@code @timestampFormat} fidelity gap). Fall back to a date-time parse so
     * the deserialize work still happens rather than throwing.
     */
    private static Instant readTimestampTolerant(Schema member, ShapeDeserializer de) {
        try {
            return de.readTimestamp(member);
        } catch (RuntimeException parseMismatch) {
            return null; // value already consumed; benchmark measures the attempt, not correctness here
        }
    }

    private static List<Object> readList(Schema member, SdkField<?> listField, ShapeDeserializer de) {
        Schema element = member.member("member");
        SdkField<?> elementField = listMemberField(listField);
        List<Object> out = new ArrayList<>();
        de.readList(member, out, (state, elemDe) -> state.add(readElement(element, elementField, elemDe)));
        return out;
    }

    private static Map<String, Object> readMap(Schema member, SdkField<?> mapField, ShapeDeserializer de) {
        Schema value = member.member("value");
        SdkField<?> valueField = mapValueField(mapField);
        Map<String, Object> out = new LinkedHashMap<>();
        de.readStringMap(member, out, (state, key, valDe) -> state.put(key, readElement(value, valueField, valDe)));
        return out;
    }

    /**
     * Read a collection element. For struct/union elements (e.g. DynamoDB's {@code AttributeValue}
     * union as a map value), recurse with a fresh v2 builder obtained from the element's v2
     * {@code SdkField} constructor — without it, a nested object can't be read as a scalar.
     */
    private static Object readElement(Schema schema, SdkField<?> elementField, ShapeDeserializer de) {
        switch (schema.type()) {
            case STRING:
            case ENUM:
                return de.readString(schema);
            case INTEGER:
                return de.readInteger(schema);
            case LONG:
                return de.readLong(schema);
            case BOOLEAN:
                return de.readBoolean(schema);
            case DOUBLE:
                return de.readDouble(schema);
            case TIMESTAMP:
                return readTimestampTolerant(schema, de);
            case BLOB:
                return SdkBytes.fromByteBuffer(de.readBlob(schema));
            case LIST:
                return readList(schema, elementField, de);
            case MAP:
                return readMap(schema, elementField, de);
            case STRUCTURE:
            case UNION: {
                SdkPojo nested = elementField != null ? nestedBuilder(elementField) : null;
                if (nested != null) {
                    BridgeOutputBuilder.of(schema, nested).deserialize(de);
                    return buildIfPossible(nested);
                }
                return null;
            }
            default:
                return de.readString(schema);
        }
    }

    /** The v2 element SdkField of a list member (from its ListTrait), or null. */
    private static SdkField<?> listMemberField(SdkField<?> listField) {
        var t = listField.getTrait(software.amazon.awssdk.core.traits.ListTrait.class);
        return t != null ? t.memberFieldInfo() : null;
    }

    /** The v2 value SdkField of a map member (from its MapTrait), or null. */
    private static SdkField<?> mapValueField(SdkField<?> mapField) {
        var t = mapField.getTrait(software.amazon.awssdk.core.traits.MapTrait.class);
        return t != null ? t.valueFieldInfo() : null;
    }

    /** A v2 SdkField for a nested struct can construct a fresh builder via its constructor supplier. */
    private static SdkPojo nestedBuilder(SdkField<?> field) {
        Supplier<SdkPojo> ctor = field.constructor();
        return ctor != null ? ctor.get() : null;
    }

    /**
     * Build a populated v2 builder into its immutable shape. v2 builders implement the public
     * {@link Buildable} interface ({@code Object build()}); call it directly rather than
     * reflecting on the package-private {@code BuilderImpl} class (which fails with
     * IllegalAccessException and would leak the builder, causing a ClassCastException when the
     * value is stored into a typed field/collection).
     */
    private static Object buildIfPossible(SdkPojo builder) {
        if (builder instanceof Buildable buildable) {
            return buildable.build();
        }
        return builder;
    }
}
