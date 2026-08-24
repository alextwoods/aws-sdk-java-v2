package software.amazon.awssdk.benchmark.smithyjava.dynamodb.model;

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

@SmithyGenerated
public final class DeleteResourcePolicyOutput implements SerializableStruct {

    public static final Schema $SCHEMA = Schemas.DELETE_RESOURCE_POLICY_OUTPUT;
    private static final Schema $SCHEMA_REVISION_ID = $SCHEMA.member("RevisionId");

    public static final ShapeId $ID = $SCHEMA.id();

    private final transient String revisionId;

    private DeleteResourcePolicyOutput(Builder builder) {
        this.revisionId = builder.revisionId;
    }

    /**
     * A unique string that represents the revision ID of the policy. If you're comparing revision IDs, make sure to
     * always use string comparison logic.
     *
     * <p>This value will be empty if you make a request against a resource without a policy.
     */
    public String getRevisionId() {
        return revisionId;
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
        DeleteResourcePolicyOutput that = (DeleteResourcePolicyOutput) other;
        return Objects.equals(this.revisionId, that.revisionId);
    }

    @Override
    public int hashCode() {
        int $hc = Objects.hashCode(revisionId);
        return $hc;
    }

    @Override
    public Schema schema() {
        return $SCHEMA;
    }

    @Override
    public void serializeMembers(ShapeSerializer serializer) {
        if (revisionId != null) {
            serializer.writeString($SCHEMA_REVISION_ID, revisionId);
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T getMemberValue(Schema member) {
        return switch (member.memberIndex()) {
            case 0 -> (T) SchemaUtils.validateSameMember($SCHEMA_REVISION_ID, member, revisionId);
            default -> throw new IllegalArgumentException("Attempted to get non-existent member: " + member.id());
        };
    }

    /**
     * Create a new builder containing all the current property values of this object.
     *
     * <p><strong>Note:</strong> This method performs only a shallow copy of the original properties.
     *
     * @return a builder for {@link DeleteResourcePolicyOutput}.
     */
    public Builder toBuilder() {
        var builder = new Builder();
        builder.revisionId(this.revisionId);
        return builder;
    }

    /**
     * @return returns a new Builder.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for {@link DeleteResourcePolicyOutput}.
     */
    public static final class Builder implements ShapeBuilder<DeleteResourcePolicyOutput> {
        private String revisionId;

        private Builder() {}

        @Override
        public Schema schema() {
            return $SCHEMA;
        }

        /**
         * A unique string that represents the revision ID of the policy. If you're comparing revision IDs, make sure to
         * always use string comparison logic.
         *
         * <p>This value will be empty if you make a request against a resource without a policy.
         *
         * @return this builder.
         */
        public Builder revisionId(String revisionId) {
            this.revisionId = revisionId;
            return this;
        }

        @Override
        public DeleteResourcePolicyOutput build() {
            return new DeleteResourcePolicyOutput(this);
        }

        @Override
        @SuppressWarnings("unchecked")
        public void setMemberValue(Schema member, Object value) {
            switch (member.memberIndex()) {
                case 0 -> revisionId((String) SchemaUtils.validateSameMember($SCHEMA_REVISION_ID, member, value));
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
                    case 0 -> builder.revisionId(de.readString(member));
                    default -> throw new IllegalArgumentException("Unexpected member: " + member.memberName());
                }
            }
        }
    }
}
