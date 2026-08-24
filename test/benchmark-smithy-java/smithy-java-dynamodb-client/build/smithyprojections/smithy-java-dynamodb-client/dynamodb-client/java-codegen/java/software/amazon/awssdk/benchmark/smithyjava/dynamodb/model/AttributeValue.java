package software.amazon.awssdk.benchmark.smithyjava.dynamodb.model;

import java.nio.ByteBuffer;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import software.amazon.smithy.java.core.schema.Schema;
import software.amazon.smithy.java.core.schema.SchemaUtils;
import software.amazon.smithy.java.core.schema.SerializableStruct;
import software.amazon.smithy.java.core.schema.ShapeBuilder;
import software.amazon.smithy.java.core.serde.ShapeDeserializer;
import software.amazon.smithy.java.core.serde.ShapeSerializer;
import software.amazon.smithy.java.core.serde.ToStringSerializer;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.utils.SmithyGenerated;

/**
 * Represents the data for an attribute.
 *
 * <p>Each attribute value is described as a name-value pair. The name is the data type, and the value is the data
 * itself.
 *
 * <p>For more information, see <a href="https://docs.aws.amazon.com/amazondynamodb/latest/developerguide/HowItWorks.NamingRulesDataTypes.html#HowItWorks.DataTypes">Data Types</a> in the <i>Amazon DynamoDB Developer Guide</i>.
 */
@SmithyGenerated
public sealed interface AttributeValue extends SerializableStruct {
    Schema $SCHEMA = Schemas.ATTRIBUTE_VALUE;

    ShapeId $ID = $SCHEMA.id();

    <T> T getValue();

    @Override
    default Schema schema() {
        return $SCHEMA;
    }

    @Override
    default <T> T getMemberValue(Schema member) {
        return SchemaUtils.validateMemberInSchema($SCHEMA, member, getValue());
    }

    /**
     * An attribute of type String. For example:
     *
     * <p><code>"S": "Hello"</code>
     */
    @SmithyGenerated
    record SMember(String s) implements AttributeValue {
        private static final Schema $SCHEMA_S = $SCHEMA.member("S");
        public SMember {
            Objects.requireNonNull(s, "Union value cannot be null");
        }
        @Override
        public void serializeMembers(ShapeSerializer serializer) {
            serializer.writeString($SCHEMA_S, s);
        }

        /**
         * An attribute of type String. For example:
         *
         * <p><code>"S": "Hello"</code>
         */
        @Override
        public String getValue() {
            return s;
        }

        @Override
        public String toString() {
            return ToStringSerializer.serialize(this);
        }

    }

    /**
     * An attribute of type Number. For example:
     *
     * <p><code>"N": "123.45"</code>
     *
     * <p>Numbers are sent across the network to DynamoDB as strings, to maximize compatibility across languages and
     * libraries. However, DynamoDB treats them as number type attributes for mathematical operations.
     */
    @SmithyGenerated
    record NMember(String n) implements AttributeValue {
        private static final Schema $SCHEMA_N = $SCHEMA.member("N");
        public NMember {
            Objects.requireNonNull(n, "Union value cannot be null");
        }
        @Override
        public void serializeMembers(ShapeSerializer serializer) {
            serializer.writeString($SCHEMA_N, n);
        }

        /**
         * An attribute of type Number. For example:
         *
         * <p><code>"N": "123.45"</code>
         *
         * <p>Numbers are sent across the network to DynamoDB as strings, to maximize compatibility across languages and
         * libraries. However, DynamoDB treats them as number type attributes for mathematical operations.
         */
        @Override
        public String getValue() {
            return n;
        }

        @Override
        public String toString() {
            return ToStringSerializer.serialize(this);
        }

    }

    /**
     * An attribute of type Binary. For example:
     *
     * <p><code>"B": "dGhpcyB0ZXh0IGlzIGJhc2U2NC1lbmNvZGVk"</code>
     */
    @SmithyGenerated
    record BMember(ByteBuffer b) implements AttributeValue {
        private static final Schema $SCHEMA_B = $SCHEMA.member("B");
        public BMember {
            Objects.requireNonNull(b, "Union value cannot be null");
        }
        @Override
        public void serializeMembers(ShapeSerializer serializer) {
            serializer.writeBlob($SCHEMA_B, b);
        }

        /**
         * An attribute of type Binary. For example:
         *
         * <p><code>"B": "dGhpcyB0ZXh0IGlzIGJhc2U2NC1lbmNvZGVk"</code>
         */
        @Override
        public ByteBuffer getValue() {
            return b;
        }

        @Override
        public String toString() {
            return ToStringSerializer.serialize(this);
        }

    }

    /**
     * An attribute of type String Set. For example:
     *
     * <p><code>"SS": ["Giraffe", "Hippo" ,"Zebra"]</code>
     */
    @SmithyGenerated
    record SsMember(List<String> ss) implements AttributeValue {
        private static final Schema $SCHEMA_SS = $SCHEMA.member("SS");
        public SsMember {
            ss = Collections.unmodifiableList(Objects.requireNonNull(ss, "Union value cannot be null"));
        }
        @Override
        public void serializeMembers(ShapeSerializer serializer) {
            serializer.writeList($SCHEMA_SS, ss, ss.size(), SharedSerde.StringSetAttributeValueSerializer.INSTANCE);
        }

        /**
         * An attribute of type String Set. For example:
         *
         * <p><code>"SS": ["Giraffe", "Hippo" ,"Zebra"]</code>
         */
        @Override
        public List<String> getValue() {
            return ss;
        }

        @Override
        public String toString() {
            return ToStringSerializer.serialize(this);
        }

    }

    /**
     * An attribute of type Number Set. For example:
     *
     * <p><code>"NS": ["42.2", "-19", "7.5", "3.14"]</code>
     *
     * <p>Numbers are sent across the network to DynamoDB as strings, to maximize compatibility across languages and
     * libraries. However, DynamoDB treats them as number type attributes for mathematical operations.
     */
    @SmithyGenerated
    record NsMember(List<String> ns) implements AttributeValue {
        private static final Schema $SCHEMA_NS = $SCHEMA.member("NS");
        public NsMember {
            ns = Collections.unmodifiableList(Objects.requireNonNull(ns, "Union value cannot be null"));
        }
        @Override
        public void serializeMembers(ShapeSerializer serializer) {
            serializer.writeList($SCHEMA_NS, ns, ns.size(), SharedSerde.NumberSetAttributeValueSerializer.INSTANCE);
        }

        /**
         * An attribute of type Number Set. For example:
         *
         * <p><code>"NS": ["42.2", "-19", "7.5", "3.14"]</code>
         *
         * <p>Numbers are sent across the network to DynamoDB as strings, to maximize compatibility across languages and
         * libraries. However, DynamoDB treats them as number type attributes for mathematical operations.
         */
        @Override
        public List<String> getValue() {
            return ns;
        }

        @Override
        public String toString() {
            return ToStringSerializer.serialize(this);
        }

    }

    /**
     * An attribute of type Binary Set. For example:
     *
     * <p><code>"BS": ["U3Vubnk=", "UmFpbnk=", "U25vd3k="]</code>
     */
    @SmithyGenerated
    record BsMember(List<ByteBuffer> bs) implements AttributeValue {
        private static final Schema $SCHEMA_BS = $SCHEMA.member("BS");
        public BsMember {
            bs = Collections.unmodifiableList(Objects.requireNonNull(bs, "Union value cannot be null"));
        }
        @Override
        public void serializeMembers(ShapeSerializer serializer) {
            serializer.writeList($SCHEMA_BS, bs, bs.size(), SharedSerde.BinarySetAttributeValueSerializer.INSTANCE);
        }

        /**
         * An attribute of type Binary Set. For example:
         *
         * <p><code>"BS": ["U3Vubnk=", "UmFpbnk=", "U25vd3k="]</code>
         */
        @Override
        public List<ByteBuffer> getValue() {
            return bs;
        }

        @Override
        public String toString() {
            return ToStringSerializer.serialize(this);
        }

    }

    /**
     * An attribute of type Map. For example:
     *
     * <p><code>"M": {"Name": {"S": "Joe"}, "Age": {"N": "35"}}</code>
     */
    @SmithyGenerated
    record MMember(Map<String, AttributeValue> m) implements AttributeValue {
        private static final Schema $SCHEMA_M = $SCHEMA.member("M");
        public MMember {
            m = Collections.unmodifiableMap(Objects.requireNonNull(m, "Union value cannot be null"));
        }
        @Override
        public void serializeMembers(ShapeSerializer serializer) {
            serializer.writeMap($SCHEMA_M, m, m.size(), SharedSerde.MapAttributeValueSerializer.INSTANCE);
        }

        /**
         * An attribute of type Map. For example:
         *
         * <p><code>"M": {"Name": {"S": "Joe"}, "Age": {"N": "35"}}</code>
         */
        @Override
        public Map<String, AttributeValue> getValue() {
            return m;
        }

        @Override
        public String toString() {
            return ToStringSerializer.serialize(this);
        }

    }

    /**
     * An attribute of type List. For example:
     *
     * <p><code>"L": [ {"S": "Cookies"} , {"S": "Coffee"}, {"N": "3.14159"}]</code>
     */
    @SmithyGenerated
    record LMember(List<AttributeValue> l) implements AttributeValue {
        private static final Schema $SCHEMA_L = $SCHEMA.member("L");
        public LMember {
            l = Collections.unmodifiableList(Objects.requireNonNull(l, "Union value cannot be null"));
        }
        @Override
        public void serializeMembers(ShapeSerializer serializer) {
            serializer.writeList($SCHEMA_L, l, l.size(), SharedSerde.ListAttributeValueSerializer.INSTANCE);
        }

        /**
         * An attribute of type List. For example:
         *
         * <p><code>"L": [ {"S": "Cookies"} , {"S": "Coffee"}, {"N": "3.14159"}]</code>
         */
        @Override
        public List<AttributeValue> getValue() {
            return l;
        }

        @Override
        public String toString() {
            return ToStringSerializer.serialize(this);
        }

    }

    /**
     * An attribute of type Null. For example:
     *
     * <p><code>"NULL": true</code>
     */
    @SmithyGenerated
    record NullMemberMember(boolean nullMember) implements AttributeValue {
        private static final Schema $SCHEMA_NULL_MEMBER = $SCHEMA.member("NULL");
        @Override
        public void serializeMembers(ShapeSerializer serializer) {
            serializer.writeBoolean($SCHEMA_NULL_MEMBER, nullMember);
        }

        /**
         * An attribute of type Null. For example:
         *
         * <p><code>"NULL": true</code>
         */
        @Override
        @SuppressWarnings("unchecked")
        public Boolean getValue() {
            return nullMember;
        }

        @Override
        public String toString() {
            return ToStringSerializer.serialize(this);
        }

    }

    /**
     * An attribute of type Boolean. For example:
     *
     * <p><code>"BOOL": true</code>
     */
    @SmithyGenerated
    record BoolMember(boolean bool) implements AttributeValue {
        private static final Schema $SCHEMA_BOOL = $SCHEMA.member("BOOL");
        @Override
        public void serializeMembers(ShapeSerializer serializer) {
            serializer.writeBoolean($SCHEMA_BOOL, bool);
        }

        /**
         * An attribute of type Boolean. For example:
         *
         * <p><code>"BOOL": true</code>
         */
        @Override
        @SuppressWarnings("unchecked")
        public Boolean getValue() {
            return bool;
        }

        @Override
        public String toString() {
            return ToStringSerializer.serialize(this);
        }

    }

    record $Unknown(String memberName) implements AttributeValue {
        @Override
        public void serialize(ShapeSerializer serializer) {
            throw new UnsupportedOperationException("Cannot serialize union with unknown member " + this.memberName);
        }

        @Override
        public void serializeMembers(ShapeSerializer serializer) {}

        @Override
        public String getValue() {
            return memberName;
        }

        private record $Hidden() implements AttributeValue {
            @Override
            public void serializeMembers(ShapeSerializer serializer) {}

            @Override
            @SuppressWarnings("unchecked")
            public <T> T getValue() {
                return null;
            }
        }
    }

    interface BuildStage {
        AttributeValue build();
    }

    /**
     * @return returns a new Builder.
     */
    static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for {@link AttributeValue}.
     */
    final class Builder implements ShapeBuilder<AttributeValue>, BuildStage {
        private AttributeValue value;

        private Builder() {}

        @Override
        public Schema schema() {
            return $SCHEMA;
        }

        public BuildStage s(String value) {
            return setValue(new SMember(value));
        }

        public BuildStage n(String value) {
            return setValue(new NMember(value));
        }

        public BuildStage b(ByteBuffer value) {
            return setValue(new BMember(value));
        }

        public BuildStage ss(List<String> value) {
            return setValue(new SsMember(value));
        }

        public BuildStage ns(List<String> value) {
            return setValue(new NsMember(value));
        }

        public BuildStage bs(List<ByteBuffer> value) {
            return setValue(new BsMember(value));
        }

        public BuildStage m(Map<String, AttributeValue> value) {
            return setValue(new MMember(value));
        }

        public BuildStage l(List<AttributeValue> value) {
            return setValue(new LMember(value));
        }

        public BuildStage nullMember(boolean value) {
            return setValue(new NullMemberMember(value));
        }

        public BuildStage bool(boolean value) {
            return setValue(new BoolMember(value));
        }

        public BuildStage $unknownMember(String memberName) {
            return setValue(new $Unknown(memberName));
        }

        private BuildStage setValue(AttributeValue value) {
            if (this.value != null) {
                throw new IllegalArgumentException("Only one value may be set for unions");
            }
            this.value = value;
            return this;
        }

        @Override
        public AttributeValue build() {
            return Objects.requireNonNull(value, "no union value set");
        }

        @Override
        @SuppressWarnings("unchecked")
        public void setMemberValue(Schema member, Object value) {
            switch (member.memberIndex()) {
                case 0 -> s((String) SchemaUtils.validateSameMember(SMember.$SCHEMA_S, member, value));
                case 1 -> n((String) SchemaUtils.validateSameMember(NMember.$SCHEMA_N, member, value));
                case 2 -> b((ByteBuffer) SchemaUtils.validateSameMember(BMember.$SCHEMA_B, member, value));
                case 3 -> ss((List<String>) SchemaUtils.validateSameMember(SsMember.$SCHEMA_SS, member, value));
                case 4 -> ns((List<String>) SchemaUtils.validateSameMember(NsMember.$SCHEMA_NS, member, value));
                case 5 -> bs((List<ByteBuffer>) SchemaUtils.validateSameMember(BsMember.$SCHEMA_BS, member, value));
                case 6 -> m((Map<String, AttributeValue>) SchemaUtils.validateSameMember(MMember.$SCHEMA_M, member, value));
                case 7 -> l((List<AttributeValue>) SchemaUtils.validateSameMember(LMember.$SCHEMA_L, member, value));
                case 8 -> nullMember((boolean) SchemaUtils.validateSameMember(NullMemberMember.$SCHEMA_NULL_MEMBER, member, value));
                case 9 -> bool((boolean) SchemaUtils.validateSameMember(BoolMember.$SCHEMA_BOOL, member, value));
                default -> ShapeBuilder.super.setMemberValue(member, value);
            }
        }

        @Override
        public Builder deserialize(ShapeDeserializer decoder) {
            decoder.readStruct($SCHEMA, this, $InnerDeserializer.INSTANCE);
            return this;
        }

        @Override
        public Builder deserializeMember(ShapeDeserializer decoder, Schema schema) {
            decoder.readStruct(schema.assertMemberTargetIs($SCHEMA), this, $InnerDeserializer.INSTANCE);
            return this;
        }

        private static final class $InnerDeserializer implements ShapeDeserializer.StructMemberConsumer<Builder> {
            private static final $InnerDeserializer INSTANCE = new $InnerDeserializer();

            @Override
            @SuppressWarnings("unchecked")
            public void accept(Builder builder, Schema member, ShapeDeserializer de) {
                switch (member.memberIndex()) {
                    case 0 -> builder.s(de.readString(member));
                    case 1 -> builder.n(de.readString(member));
                    case 2 -> builder.b(de.readBlob(member));
                    case 3 -> builder.ss(SharedSerde.deserializeStringSetAttributeValue(member, de));
                    case 4 -> builder.ns(SharedSerde.deserializeNumberSetAttributeValue(member, de));
                    case 5 -> builder.bs(SharedSerde.deserializeBinarySetAttributeValue(member, de));
                    case 6 -> builder.m(SharedSerde.deserializeMapAttributeValue(member, de));
                    case 7 -> builder.l(SharedSerde.deserializeListAttributeValue(member, de));
                    case 8 -> builder.nullMember(de.readBoolean(member));
                    case 9 -> builder.bool(de.readBoolean(member));
                    default -> throw new IllegalArgumentException("Unexpected member: " + member.memberName());
                }
            }

            @Override
            public void unknownMember(Builder builder, String memberName) {
                builder.$unknownMember(memberName);
            }
        }
    }
}
