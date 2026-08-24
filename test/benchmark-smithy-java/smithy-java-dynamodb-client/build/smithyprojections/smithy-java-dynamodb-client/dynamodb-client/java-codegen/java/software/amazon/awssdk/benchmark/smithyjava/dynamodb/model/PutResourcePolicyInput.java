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
public final class PutResourcePolicyInput implements SerializableStruct {

    public static final Schema $SCHEMA = Schemas.PUT_RESOURCE_POLICY_INPUT;
    private static final Schema $SCHEMA_RESOURCE_ARN = $SCHEMA.member("ResourceArn");
    private static final Schema $SCHEMA_POLICY = $SCHEMA.member("Policy");
    private static final Schema $SCHEMA_EXPECTED_REVISION_ID = $SCHEMA.member("ExpectedRevisionId");
    private static final Schema $SCHEMA_CONFIRM_REMOVE_SELF_RESOURCE_ACCESS = $SCHEMA.member("ConfirmRemoveSelfResourceAccess");

    public static final ShapeId $ID = $SCHEMA.id();

    private final transient String resourceArn;
    private final transient String policy;
    private final transient String expectedRevisionId;
    private final transient boolean confirmRemoveSelfResourceAccess;

    private PutResourcePolicyInput(Builder builder) {
        this.resourceArn = builder.resourceArn;
        this.policy = builder.policy;
        this.expectedRevisionId = builder.expectedRevisionId;
        this.confirmRemoveSelfResourceAccess = builder.confirmRemoveSelfResourceAccess;
    }

    /**
     * The Amazon Resource Name (ARN) of the DynamoDB resource to which the policy will be attached. The resources you
     * can specify include tables and streams.
     *
     * <p>You can control index permissions using the base table's policy. To specify the same permission level for your
     * table and its indexes, you can provide both the table and index Amazon Resource Name (ARN)s in the <code>Resource</code>
     * field of a given <code>Statement</code> in your policy document. Alternatively, to specify different permissions
     * for your table, indexes, or both, you can define multiple <code>Statement</code> fields in your policy document.
     */
    public String getResourceArn() {
        return resourceArn;
    }

    /**
     * An Amazon Web Services resource-based policy document in JSON format.
     *
     * <ul>
     *   <li>
     *     The maximum size supported for a resource-based policy document is 20 KB. DynamoDB counts whitespaces
     *     when calculating the size of a policy against this limit.
     *   </li>
     *   <li>
     *     Within a resource-based policy, if the action for a DynamoDB service-linked role (SLR) to replicate data
     *     for a global table is denied, adding or deleting a replica will fail with an error.
     *   </li>
     * </ul>
     *
     * <p>For a full list of all considerations that apply while attaching a resource-based policy, see <a href="https://docs.aws.amazon.com/amazondynamodb/latest/developerguide/rbac-considerations.html">
     * Resource-based policy considerations</a>.
     */
    public String getPolicy() {
        return policy;
    }

    /**
     * A string value that you can use to conditionally update your policy. You can provide the revision ID of your
     * existing policy to make mutating requests against that policy.
     *
     * <p>When you provide an expected revision ID, if the revision ID of the existing policy on the resource doesn't
     * match or if there's no policy attached to the resource, your request will be rejected with a <code>
     * PolicyNotFoundException</code>.
     *
     * <p>To conditionally attach a policy when no policy exists for the resource, specify <code>NO_POLICY</code> for
     * the revision ID.
     */
    public String getExpectedRevisionId() {
        return expectedRevisionId;
    }

    /**
     * Set this parameter to <code>true</code> to confirm that you want to remove your permissions to change the policy
     * of this resource in the future.
     */
    public boolean isConfirmRemoveSelfResourceAccess() {
        return confirmRemoveSelfResourceAccess;
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
        PutResourcePolicyInput that = (PutResourcePolicyInput) other;
        return this.confirmRemoveSelfResourceAccess == that.confirmRemoveSelfResourceAccess
               && Objects.equals(this.resourceArn, that.resourceArn)
               && Objects.equals(this.policy, that.policy)
               && Objects.equals(this.expectedRevisionId, that.expectedRevisionId);
    }

    @Override
    public int hashCode() {
        int $hc = Objects.hashCode(resourceArn);
        $hc = 31 * $hc + Objects.hashCode(policy);
        $hc = 31 * $hc + Objects.hashCode(expectedRevisionId);
        $hc = 31 * $hc + Boolean.hashCode(confirmRemoveSelfResourceAccess);
        return $hc;
    }

    @Override
    public Schema schema() {
        return $SCHEMA;
    }

    @Override
    public void serializeMembers(ShapeSerializer serializer) {
        serializer.writeString($SCHEMA_RESOURCE_ARN, resourceArn);
        serializer.writeString($SCHEMA_POLICY, policy);
        if (expectedRevisionId != null) {
            serializer.writeString($SCHEMA_EXPECTED_REVISION_ID, expectedRevisionId);
        }
        serializer.writeBoolean($SCHEMA_CONFIRM_REMOVE_SELF_RESOURCE_ACCESS, confirmRemoveSelfResourceAccess);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T getMemberValue(Schema member) {
        return switch (member.memberIndex()) {
            case 0 -> (T) SchemaUtils.validateSameMember($SCHEMA_RESOURCE_ARN, member, resourceArn);
            case 1 -> (T) SchemaUtils.validateSameMember($SCHEMA_POLICY, member, policy);
            case 2 -> (T) SchemaUtils.validateSameMember($SCHEMA_EXPECTED_REVISION_ID, member, expectedRevisionId);
            case 3 -> (T) SchemaUtils.validateSameMember($SCHEMA_CONFIRM_REMOVE_SELF_RESOURCE_ACCESS, member, confirmRemoveSelfResourceAccess);
            default -> throw new IllegalArgumentException("Attempted to get non-existent member: " + member.id());
        };
    }

    /**
     * Create a new builder containing all the current property values of this object.
     *
     * <p><strong>Note:</strong> This method performs only a shallow copy of the original properties.
     *
     * @return a builder for {@link PutResourcePolicyInput}.
     */
    public Builder toBuilder() {
        var builder = new Builder();
        builder.resourceArn(this.resourceArn);
        builder.policy(this.policy);
        builder.expectedRevisionId(this.expectedRevisionId);
        builder.confirmRemoveSelfResourceAccess(this.confirmRemoveSelfResourceAccess);
        return builder;
    }

    /**
     * @return returns a new Builder.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for {@link PutResourcePolicyInput}.
     */
    public static final class Builder implements ShapeBuilder<PutResourcePolicyInput> {
        private final PresenceTracker tracker = PresenceTracker.of($SCHEMA);
        private String resourceArn;
        private String policy;
        private String expectedRevisionId;
        private boolean confirmRemoveSelfResourceAccess = false;

        private Builder() {}

        @Override
        public Schema schema() {
            return $SCHEMA;
        }

        /**
         * The Amazon Resource Name (ARN) of the DynamoDB resource to which the policy will be attached. The resources you
         * can specify include tables and streams.
         *
         * <p>You can control index permissions using the base table's policy. To specify the same permission level for your
         * table and its indexes, you can provide both the table and index Amazon Resource Name (ARN)s in the <code>Resource</code>
         * field of a given <code>Statement</code> in your policy document. Alternatively, to specify different permissions
         * for your table, indexes, or both, you can define multiple <code>Statement</code> fields in your policy document.
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
         * An Amazon Web Services resource-based policy document in JSON format.
         *
         * <ul>
         *   <li>
         *     The maximum size supported for a resource-based policy document is 20 KB. DynamoDB counts whitespaces
         *     when calculating the size of a policy against this limit.
         *   </li>
         *   <li>
         *     Within a resource-based policy, if the action for a DynamoDB service-linked role (SLR) to replicate data
         *     for a global table is denied, adding or deleting a replica will fail with an error.
         *   </li>
         * </ul>
         *
         * <p>For a full list of all considerations that apply while attaching a resource-based policy, see <a href="https://docs.aws.amazon.com/amazondynamodb/latest/developerguide/rbac-considerations.html">
         * Resource-based policy considerations</a>.
         *
         * <p><strong>Required</strong>
         * @return this builder.
         */
        public Builder policy(String policy) {
            this.policy = Objects.requireNonNull(policy, "policy cannot be null");
            tracker.setMember($SCHEMA_POLICY);
            return this;
        }

        /**
         * A string value that you can use to conditionally update your policy. You can provide the revision ID of your
         * existing policy to make mutating requests against that policy.
         *
         * <p>When you provide an expected revision ID, if the revision ID of the existing policy on the resource doesn't
         * match or if there's no policy attached to the resource, your request will be rejected with a <code>
         * PolicyNotFoundException</code>.
         *
         * <p>To conditionally attach a policy when no policy exists for the resource, specify <code>NO_POLICY</code> for
         * the revision ID.
         *
         * @return this builder.
         */
        public Builder expectedRevisionId(String expectedRevisionId) {
            this.expectedRevisionId = expectedRevisionId;
            return this;
        }

        /**
         * Set this parameter to <code>true</code> to confirm that you want to remove your permissions to change the policy
         * of this resource in the future.
         *
         * @return this builder.
         */
        public Builder confirmRemoveSelfResourceAccess(boolean confirmRemoveSelfResourceAccess) {
            this.confirmRemoveSelfResourceAccess = confirmRemoveSelfResourceAccess;
            return this;
        }

        @Override
        public PutResourcePolicyInput build() {
            tracker.validate();
            return new PutResourcePolicyInput(this);
        }

        @Override
        @SuppressWarnings("unchecked")
        public void setMemberValue(Schema member, Object value) {
            switch (member.memberIndex()) {
                case 0 -> resourceArn((String) SchemaUtils.validateSameMember($SCHEMA_RESOURCE_ARN, member, value));
                case 1 -> policy((String) SchemaUtils.validateSameMember($SCHEMA_POLICY, member, value));
                case 2 -> expectedRevisionId((String) SchemaUtils.validateSameMember($SCHEMA_EXPECTED_REVISION_ID, member, value));
                case 3 -> confirmRemoveSelfResourceAccess((boolean) SchemaUtils.validateSameMember($SCHEMA_CONFIRM_REMOVE_SELF_RESOURCE_ACCESS, member, value));
                default -> ShapeBuilder.super.setMemberValue(member, value);
            }
        }

        @Override
        public ShapeBuilder<PutResourcePolicyInput> errorCorrection() {
            if (tracker.allSet()) {
                return this;
            }
            if (!tracker.checkMember($SCHEMA_RESOURCE_ARN)) {
                resourceArn("");
            }
            if (!tracker.checkMember($SCHEMA_POLICY)) {
                policy("");
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
                    case 1 -> builder.policy(de.readString(member));
                    case 2 -> builder.expectedRevisionId(de.readString(member));
                    case 3 -> builder.confirmRemoveSelfResourceAccess(de.readBoolean(member));
                    default -> throw new IllegalArgumentException("Unexpected member: " + member.memberName());
                }
            }
        }
    }
}
