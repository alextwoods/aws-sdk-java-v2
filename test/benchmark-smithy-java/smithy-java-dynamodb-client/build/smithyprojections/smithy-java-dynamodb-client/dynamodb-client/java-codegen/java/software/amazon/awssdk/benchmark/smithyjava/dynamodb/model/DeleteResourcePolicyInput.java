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

@SmithyGenerated
public final class DeleteResourcePolicyInput implements SerializableStruct {

    public static final Schema $SCHEMA = Schemas.DELETE_RESOURCE_POLICY_INPUT;
    private static final Schema $SCHEMA_RESOURCE_ARN = $SCHEMA.member("ResourceArn");
    private static final Schema $SCHEMA_EXPECTED_REVISION_ID = $SCHEMA.member("ExpectedRevisionId");

    public static final ShapeId $ID = $SCHEMA.id();

    private final transient String resourceArn;
    private final transient String expectedRevisionId;

    private DeleteResourcePolicyInput(Builder builder) {
        this.resourceArn = builder.resourceArn;
        this.expectedRevisionId = builder.expectedRevisionId;
    }

    /**
     * The Amazon Resource Name (ARN) of the DynamoDB resource from which the policy will be removed. The resources you
     * can specify include tables and streams. If you remove the policy of a table, it will also remove the permissions
     * for the table's indexes defined in that policy document. This is because index permissions are defined in the
     * table's policy.
     */
    public String getResourceArn() {
        return resourceArn;
    }

    /**
     * A string value that you can use to conditionally delete your policy. When you provide an expected revision ID, if
     * the revision ID of the existing policy on the resource doesn't match or if there's no policy attached to the
     * resource, the request will fail and return a <code>PolicyNotFoundException</code>.
     */
    public String getExpectedRevisionId() {
        return expectedRevisionId;
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
        DeleteResourcePolicyInput that = (DeleteResourcePolicyInput) other;
        return Objects.equals(this.resourceArn, that.resourceArn)
               && Objects.equals(this.expectedRevisionId, that.expectedRevisionId);
    }

    @Override
    public int hashCode() {
        int $hc = Objects.hashCode(resourceArn);
        $hc = 31 * $hc + Objects.hashCode(expectedRevisionId);
        return $hc;
    }

    @Override
    public Schema schema() {
        return $SCHEMA;
    }

    @Override
    public void serializeMembers(ShapeSerializer serializer) {
        serializer.writeString($SCHEMA_RESOURCE_ARN, resourceArn);
        if (expectedRevisionId != null) {
            serializer.writeString($SCHEMA_EXPECTED_REVISION_ID, expectedRevisionId);
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T getMemberValue(Schema member) {
        return switch (member.memberIndex()) {
            case 0 -> (T) SchemaUtils.validateSameMember($SCHEMA_RESOURCE_ARN, member, resourceArn);
            case 1 -> (T) SchemaUtils.validateSameMember($SCHEMA_EXPECTED_REVISION_ID, member, expectedRevisionId);
            default -> throw new IllegalArgumentException("Attempted to get non-existent member: " + member.id());
        };
    }

    /**
     * Create a new builder containing all the current property values of this object.
     *
     * <p><strong>Note:</strong> This method performs only a shallow copy of the original properties.
     *
     * @return a builder for {@link DeleteResourcePolicyInput}.
     */
    public Builder toBuilder() {
        var builder = new Builder();
        builder.resourceArn(this.resourceArn);
        builder.expectedRevisionId(this.expectedRevisionId);
        return builder;
    }

    /**
     * @return returns a new Builder.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for {@link DeleteResourcePolicyInput}.
     */
    public static final class Builder implements ShapeBuilder<DeleteResourcePolicyInput> {
        private final PresenceTracker tracker = PresenceTracker.of($SCHEMA);
        private String resourceArn;
        private String expectedRevisionId;

        private Builder() {}

        @Override
        public Schema schema() {
            return $SCHEMA;
        }

        /**
         * The Amazon Resource Name (ARN) of the DynamoDB resource from which the policy will be removed. The resources you
         * can specify include tables and streams. If you remove the policy of a table, it will also remove the permissions
         * for the table's indexes defined in that policy document. This is because index permissions are defined in the
         * table's policy.
         *
         * <p><strong>Required</strong>
         * @return this builder.
         */
        public Builder resourceArn(String resourceArn) {
            this.resourceArn = Objects.requireNonNull(resourceArn, "resourceArn cannot be null");
            tracker.setMember($SCHEMA_RESOURCE_ARN);
            return this;
        }

        /**
         * A string value that you can use to conditionally delete your policy. When you provide an expected revision ID, if
         * the revision ID of the existing policy on the resource doesn't match or if there's no policy attached to the
         * resource, the request will fail and return a <code>PolicyNotFoundException</code>.
         *
         * @return this builder.
         */
        public Builder expectedRevisionId(String expectedRevisionId) {
            this.expectedRevisionId = expectedRevisionId;
            return this;
        }

        @Override
        public DeleteResourcePolicyInput build() {
            tracker.validate();
            return new DeleteResourcePolicyInput(this);
        }

        @Override
        @SuppressWarnings("unchecked")
        public void setMemberValue(Schema member, Object value) {
            switch (member.memberIndex()) {
                case 0 -> resourceArn((String) SchemaUtils.validateSameMember($SCHEMA_RESOURCE_ARN, member, value));
                case 1 -> expectedRevisionId((String) SchemaUtils.validateSameMember($SCHEMA_EXPECTED_REVISION_ID, member, value));
                default -> ShapeBuilder.super.setMemberValue(member, value);
            }
        }

        @Override
        public ShapeBuilder<DeleteResourcePolicyInput> errorCorrection() {
            if (tracker.allSet()) {
                return this;
            }
            if (!tracker.checkMember($SCHEMA_RESOURCE_ARN)) {
                resourceArn("");
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
                    case 0 -> builder.resourceArn(de.readString(member));
                    case 1 -> builder.expectedRevisionId(de.readString(member));
                    default -> throw new IllegalArgumentException("Unexpected member: " + member.memberName());
                }
            }
        }
    }
}
