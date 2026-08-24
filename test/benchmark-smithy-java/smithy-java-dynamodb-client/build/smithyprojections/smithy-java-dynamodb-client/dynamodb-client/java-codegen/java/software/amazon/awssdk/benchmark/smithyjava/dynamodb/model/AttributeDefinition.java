package software.amazon.awssdk.benchmark.smithyjava.dynamodb.model;

import java.util.Objects;
import software.amazon.smithy.java.core.schema.PresenceTracker;
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
 * Represents an attribute for describing the schema for the table and indexes.
 */
@SmithyGenerated
public final class AttributeDefinition implements SerializableStruct {

    public static final Schema $SCHEMA = Schemas.ATTRIBUTE_DEFINITION;
    private static final Schema $SCHEMA_ATTRIBUTE_NAME = $SCHEMA.member("AttributeName");
    private static final Schema $SCHEMA_ATTRIBUTE_TYPE = $SCHEMA.member("AttributeType");

    public static final ShapeId $ID = $SCHEMA.id();

    private final transient String attributeName;
    private final transient ScalarAttributeType attributeType;

    private AttributeDefinition(Builder builder) {
        this.attributeName = builder.attributeName;
        this.attributeType = builder.attributeType;
    }

    /**
     * A name for the attribute.
     */
    public String getAttributeName() {
        return attributeName;
    }

    /**
     * The data type for the attribute, where:
     *
     * <ul>
     *   <li>
     *     <code>S</code> - the attribute is of type String
     *   </li>
     *   <li>
     *     <code>N</code> - the attribute is of type Number
     *   </li>
     *   <li>
     *     <code>B</code> - the attribute is of type Binary
     *   </li>
     * </ul>
     */
    public ScalarAttributeType getAttributeType() {
        return attributeType;
    }

    @Override
    public String toString() {
        return ToStringSerializer.serialize(this);
    }

    @Override
    public boolean equals(Object other) {
        if (other == this) {
            return true;
        }
        if (other == null || getClass() != other.getClass()) {
            return false;
        }
        AttributeDefinition that = (AttributeDefinition) other;
        return Objects.equals(this.attributeName, that.attributeName)
               && Objects.equals(this.attributeType, that.attributeType);
    }

    @Override
    public int hashCode() {
        int $hc = Objects.hashCode(attributeName);
        $hc = 31 * $hc + Objects.hashCode(attributeType);
        return $hc;
    }

    @Override
    public Schema schema() {
        return $SCHEMA;
    }

    @Override
    public void serializeMembers(ShapeSerializer serializer) {
        serializer.writeString($SCHEMA_ATTRIBUTE_NAME, attributeName);
        serializer.writeString($SCHEMA_ATTRIBUTE_TYPE, attributeType.getValue());
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T getMemberValue(Schema member) {
        return switch (member.memberIndex()) {
            case 0 -> (T) SchemaUtils.validateSameMember($SCHEMA_ATTRIBUTE_NAME, member, attributeName);
            case 1 -> (T) SchemaUtils.validateSameMember($SCHEMA_ATTRIBUTE_TYPE, member, attributeType);
            default -> throw new IllegalArgumentException("Attempted to get non-existent member: " + member.id());
        };
    }

    /**
     * Create a new builder containing all the current property values of this object.
     *
     * <p><strong>Note:</strong> This method performs only a shallow copy of the original properties.
     *
     * @return a builder for {@link AttributeDefinition}.
     */
    public Builder toBuilder() {
        var builder = new Builder();
        builder.attributeName(this.attributeName);
        builder.attributeType(this.attributeType);
        return builder;
    }

    /**
     * @return returns a new Builder.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for {@link AttributeDefinition}.
     */
    public static final class Builder implements ShapeBuilder<AttributeDefinition> {
        private final PresenceTracker tracker = PresenceTracker.of($SCHEMA);
        private String attributeName;
        private ScalarAttributeType attributeType;

        private Builder() {}

        @Override
        public Schema schema() {
            return $SCHEMA;
        }

        /**
         * A name for the attribute.
         *
         * <p><strong>Required</strong>
         * @return this builder.
         */
        public Builder attributeName(String attributeName) {
            this.attributeName = Objects.requireNonNull(attributeName, "attributeName cannot be null");
            tracker.setMember($SCHEMA_ATTRIBUTE_NAME);
            return this;
        }

        /**
         * The data type for the attribute, where:
         *
         * <ul>
         *   <li>
         *     <code>S</code> - the attribute is of type String
         *   </li>
         *   <li>
         *     <code>N</code> - the attribute is of type Number
         *   </li>
         *   <li>
         *     <code>B</code> - the attribute is of type Binary
         *   </li>
         * </ul>
         *
         * <p><strong>Required</strong>
         * @return this builder.
         */
        public Builder attributeType(ScalarAttributeType attributeType) {
            this.attributeType = Objects.requireNonNull(attributeType, "attributeType cannot be null");
            tracker.setMember($SCHEMA_ATTRIBUTE_TYPE);
            return this;
        }

        @Override
        public AttributeDefinition build() {
            tracker.validate();
            return new AttributeDefinition(this);
        }

        @Override
        @SuppressWarnings("unchecked")
        public void setMemberValue(Schema member, Object value) {
            switch (member.memberIndex()) {
                case 0 -> attributeName((String) SchemaUtils.validateSameMember($SCHEMA_ATTRIBUTE_NAME, member, value));
                case 1 -> attributeType((ScalarAttributeType) SchemaUtils.validateSameMember($SCHEMA_ATTRIBUTE_TYPE, member, value));
                default -> ShapeBuilder.super.setMemberValue(member, value);
            }
        }

        @Override
        public ShapeBuilder<AttributeDefinition> errorCorrection() {
            if (tracker.allSet()) {
                return this;
            }
            if (!tracker.checkMember($SCHEMA_ATTRIBUTE_NAME)) {
                attributeName("");
            }
            if (!tracker.checkMember($SCHEMA_ATTRIBUTE_TYPE)) {
                attributeType(ScalarAttributeType.unknown(""));
            }
            return this;
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
                    case 0 -> builder.attributeName(de.readString(member));
                    case 1 -> builder.attributeType(ScalarAttributeType.builder().deserializeMember(de, member).build());
                    default -> throw new IllegalArgumentException("Unexpected member: " + member.memberName());
                }
            }
        }
    }
}
