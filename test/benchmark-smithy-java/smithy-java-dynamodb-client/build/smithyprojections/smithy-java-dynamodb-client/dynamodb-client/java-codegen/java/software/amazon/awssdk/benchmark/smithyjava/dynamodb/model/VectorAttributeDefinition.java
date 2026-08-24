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
 * The definition of a vector attribute for a vector index.
 */
@SmithyGenerated
public final class VectorAttributeDefinition implements SerializableStruct {

    public static final Schema $SCHEMA = Schemas.VECTOR_ATTRIBUTE_DEFINITION;
    private static final Schema $SCHEMA_ATTRIBUTE_NAME = $SCHEMA.member("AttributeName");

    public static final ShapeId $ID = $SCHEMA.id();

    private final transient String attributeName;

    private VectorAttributeDefinition(Builder builder) {
        this.attributeName = builder.attributeName;
    }

    /**
     * The name of the vector attribute.
     */
    public String getAttributeName() {
        return attributeName;
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
        VectorAttributeDefinition that = (VectorAttributeDefinition) other;
        return Objects.equals(this.attributeName, that.attributeName);
    }

    @Override
    public int hashCode() {
        int $hc = Objects.hashCode(attributeName);
        return $hc;
    }

    @Override
    public Schema schema() {
        return $SCHEMA;
    }

    @Override
    public void serializeMembers(ShapeSerializer serializer) {
        serializer.writeString($SCHEMA_ATTRIBUTE_NAME, attributeName);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T getMemberValue(Schema member) {
        return switch (member.memberIndex()) {
            case 0 -> (T) SchemaUtils.validateSameMember($SCHEMA_ATTRIBUTE_NAME, member, attributeName);
            default -> throw new IllegalArgumentException("Attempted to get non-existent member: " + member.id());
        };
    }

    /**
     * Create a new builder containing all the current property values of this object.
     *
     * <p><strong>Note:</strong> This method performs only a shallow copy of the original properties.
     *
     * @return a builder for {@link VectorAttributeDefinition}.
     */
    public Builder toBuilder() {
        var builder = new Builder();
        builder.attributeName(this.attributeName);
        return builder;
    }

    /**
     * @return returns a new Builder.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for {@link VectorAttributeDefinition}.
     */
    public static final class Builder implements ShapeBuilder<VectorAttributeDefinition> {
        private final PresenceTracker tracker = PresenceTracker.of($SCHEMA);
        private String attributeName;

        private Builder() {}

        @Override
        public Schema schema() {
            return $SCHEMA;
        }

        /**
         * The name of the vector attribute.
         *
         * <p><strong>Required</strong>
         * @return this builder.
         */
        public Builder attributeName(String attributeName) {
            this.attributeName = Objects.requireNonNull(attributeName, "attributeName cannot be null");
            tracker.setMember($SCHEMA_ATTRIBUTE_NAME);
            return this;
        }

        @Override
        public VectorAttributeDefinition build() {
            tracker.validate();
            return new VectorAttributeDefinition(this);
        }

        @Override
        @SuppressWarnings("unchecked")
        public void setMemberValue(Schema member, Object value) {
            switch (member.memberIndex()) {
                case 0 -> attributeName((String) SchemaUtils.validateSameMember($SCHEMA_ATTRIBUTE_NAME, member, value));
                default -> ShapeBuilder.super.setMemberValue(member, value);
            }
        }

        @Override
        public ShapeBuilder<VectorAttributeDefinition> errorCorrection() {
            if (tracker.allSet()) {
                return this;
            }
            if (!tracker.checkMember($SCHEMA_ATTRIBUTE_NAME)) {
                attributeName("");
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
                    default -> throw new IllegalArgumentException("Unexpected member: " + member.memberName());
                }
            }
        }
    }
}
