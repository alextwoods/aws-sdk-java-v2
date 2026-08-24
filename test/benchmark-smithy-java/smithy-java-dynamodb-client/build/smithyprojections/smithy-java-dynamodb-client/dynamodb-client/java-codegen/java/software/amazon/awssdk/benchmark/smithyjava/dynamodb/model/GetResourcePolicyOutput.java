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
public final class GetResourcePolicyOutput implements SerializableStruct {

    public static final Schema $SCHEMA = Schemas.GET_RESOURCE_POLICY_OUTPUT;
    private static final Schema $SCHEMA_POLICY = $SCHEMA.member("Policy");
    private static final Schema $SCHEMA_REVISION_ID = $SCHEMA.member("RevisionId");

    public static final ShapeId $ID = $SCHEMA.id();

    private final transient String policy;
    private final transient String revisionId;

    private GetResourcePolicyOutput(Builder builder) {
        this.policy = builder.policy;
        this.revisionId = builder.revisionId;
    }

    /**
     * The resource-based policy document attached to the resource, which can be a table or stream, in JSON format.
     */
    public String getPolicy() {
        return policy;
    }

    /**
     * A unique string that represents the revision ID of the policy. If you're comparing revision IDs, make sure to
     * always use string comparison logic.
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
        GetResourcePolicyOutput that = (GetResourcePolicyOutput) other;
        return Objects.equals(this.policy, that.policy)
               && Objects.equals(this.revisionId, that.revisionId);
    }

    @Override
    public int hashCode() {
        int $hc = Objects.hashCode(policy);
        $hc = 31 * $hc + Objects.hashCode(revisionId);
        return $hc;
    }

    @Override
    public Schema schema() {
        return $SCHEMA;
    }

    @Override
    public void serializeMembers(ShapeSerializer serializer) {
        if (policy != null) {
            serializer.writeString($SCHEMA_POLICY, policy);
        }
        if (revisionId != null) {
            serializer.writeString($SCHEMA_REVISION_ID, revisionId);
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T getMemberValue(Schema member) {
        return switch (member.memberIndex()) {
            case 0 -> (T) SchemaUtils.validateSameMember($SCHEMA_POLICY, member, policy);
            case 1 -> (T) SchemaUtils.validateSameMember($SCHEMA_REVISION_ID, member, revisionId);
            default -> throw new IllegalArgumentException("Attempted to get non-existent member: " + member.id());
        };
    }

    /**
     * Create a new builder containing all the current property values of this object.
     *
     * <p><strong>Note:</strong> This method performs only a shallow copy of the original properties.
     *
     * @return a builder for {@link GetResourcePolicyOutput}.
     */
    public Builder toBuilder() {
        var builder = new Builder();
        builder.policy(this.policy);
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
     * Builder for {@link GetResourcePolicyOutput}.
     */
    public static final class Builder implements ShapeBuilder<GetResourcePolicyOutput> {
        private String policy;
        private String revisionId;

        private Builder() {}

        @Override
        public Schema schema() {
            return $SCHEMA;
        }

        /**
         * The resource-based policy document attached to the resource, which can be a table or stream, in JSON format.
         *
         * @return this builder.
         */
        public Builder policy(String policy) {
            this.policy = policy;
            return this;
        }

        /**
         * A unique string that represents the revision ID of the policy. If you're comparing revision IDs, make sure to
         * always use string comparison logic.
         *
         * @return this builder.
         */
        public Builder revisionId(String revisionId) {
            this.revisionId = revisionId;
            return this;
        }

        @Override
        public GetResourcePolicyOutput build() {
            return new GetResourcePolicyOutput(this);
        }

        @Override
        @SuppressWarnings("unchecked")
        public void setMemberValue(Schema member, Object value) {
            switch (member.memberIndex()) {
                case 0 -> policy((String) SchemaUtils.validateSameMember($SCHEMA_POLICY, member, value));
                case 1 -> revisionId((String) SchemaUtils.validateSameMember($SCHEMA_REVISION_ID, member, value));
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
                    case 0 -> builder.policy(de.readString(member));
                    case 1 -> builder.revisionId(de.readString(member));
                    default -> throw new IllegalArgumentException("Unexpected member: " + member.memberName());
                }
            }
        }
    }
}
