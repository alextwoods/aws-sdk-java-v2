package software.amazon.awssdk.benchmark.smithyjava.s3.model;

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
 * The container element for a bucket's policy status.
 */
@SmithyGenerated
public final class PolicyStatus implements SerializableStruct {

    public static final Schema $SCHEMA = Schemas3.POLICY_STATUS;
    private static final Schema $SCHEMA_IS_PUBLIC = $SCHEMA.member("IsPublic");

    public static final ShapeId $ID = $SCHEMA.id();

    private final transient Boolean isPublic;

    private PolicyStatus(Builder builder) {
        this.isPublic = builder.isPublic;
    }

    /**
     * The policy status for this bucket. <code>TRUE</code> indicates that this bucket is public. <code>FALSE</code>
     * indicates that the bucket is not public.
     */
    public Boolean isIsPublic() {
        return isPublic;
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
        PolicyStatus that = (PolicyStatus) other;
        return Objects.equals(this.isPublic, that.isPublic);
    }

    @Override
    public int hashCode() {
        int $hc = Objects.hashCode(isPublic);
        return $hc;
    }

    @Override
    public Schema schema() {
        return $SCHEMA;
    }

    @Override
    public void serializeMembers(ShapeSerializer serializer) {
        if (isPublic != null) {
            serializer.writeBoolean($SCHEMA_IS_PUBLIC, isPublic);
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T getMemberValue(Schema member) {
        return switch (member.memberIndex()) {
            case 0 -> (T) SchemaUtils.validateSameMember($SCHEMA_IS_PUBLIC, member, isPublic);
            default -> throw new IllegalArgumentException("Attempted to get non-existent member: " + member.id());
        };
    }

    /**
     * Create a new builder containing all the current property values of this object.
     *
     * <p><strong>Note:</strong> This method performs only a shallow copy of the original properties.
     *
     * @return a builder for {@link PolicyStatus}.
     */
    public Builder toBuilder() {
        var builder = new Builder();
        builder.isPublic(this.isPublic);
        return builder;
    }

    /**
     * @return returns a new Builder.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for {@link PolicyStatus}.
     */
    public static final class Builder implements ShapeBuilder<PolicyStatus> {
        private Boolean isPublic;

        private Builder() {}

        @Override
        public Schema schema() {
            return $SCHEMA;
        }

        /**
         * The policy status for this bucket. <code>TRUE</code> indicates that this bucket is public. <code>FALSE</code>
         * indicates that the bucket is not public.
         *
         * @return this builder.
         */
        public Builder isPublic(Boolean isPublic) {
            this.isPublic = isPublic;
            return this;
        }

        @Override
        public PolicyStatus build() {
            return new PolicyStatus(this);
        }

        @Override
        @SuppressWarnings("unchecked")
        public void setMemberValue(Schema member, Object value) {
            switch (member.memberIndex()) {
                case 0 -> isPublic((Boolean) SchemaUtils.validateSameMember($SCHEMA_IS_PUBLIC, member, value));
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
                    case 0 -> builder.isPublic(de.readBoolean(member));
                    default -> throw new IllegalArgumentException("Unexpected member: " + member.memberName());
                }
            }
        }
    }
}
