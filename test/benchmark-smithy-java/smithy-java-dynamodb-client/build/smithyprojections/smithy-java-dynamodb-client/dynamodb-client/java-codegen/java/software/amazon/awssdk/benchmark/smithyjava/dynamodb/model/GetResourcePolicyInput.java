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
public final class GetResourcePolicyInput implements SerializableStruct {

    public static final Schema $SCHEMA = Schemas.GET_RESOURCE_POLICY_INPUT;
    private static final Schema $SCHEMA_RESOURCE_ARN = $SCHEMA.member("ResourceArn");

    public static final ShapeId $ID = $SCHEMA.id();

    private final transient String resourceArn;

    private GetResourcePolicyInput(Builder builder) {
        this.resourceArn = builder.resourceArn;
    }

    /**
     * The Amazon Resource Name (ARN) of the DynamoDB resource to which the policy is attached. The resources you can
     * specify include tables and streams.
     */
    public String getResourceArn() {
        return resourceArn;
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
        GetResourcePolicyInput that = (GetResourcePolicyInput) other;
        return Objects.equals(this.resourceArn, that.resourceArn);
    }

    @Override
    public int hashCode() {
        int $hc = Objects.hashCode(resourceArn);
        return $hc;
    }

    @Override
    public Schema schema() {
        return $SCHEMA;
    }

    @Override
    public void serializeMembers(ShapeSerializer serializer) {
        serializer.writeString($SCHEMA_RESOURCE_ARN, resourceArn);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T getMemberValue(Schema member) {
        return switch (member.memberIndex()) {
            case 0 -> (T) SchemaUtils.validateSameMember($SCHEMA_RESOURCE_ARN, member, resourceArn);
            default -> throw new IllegalArgumentException("Attempted to get non-existent member: " + member.id());
        };
    }

    /**
     * Create a new builder containing all the current property values of this object.
     *
     * <p><strong>Note:</strong> This method performs only a shallow copy of the original properties.
     *
     * @return a builder for {@link GetResourcePolicyInput}.
     */
    public Builder toBuilder() {
        var builder = new Builder();
        builder.resourceArn(this.resourceArn);
        return builder;
    }

    /**
     * @return returns a new Builder.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for {@link GetResourcePolicyInput}.
     */
    public static final class Builder implements ShapeBuilder<GetResourcePolicyInput> {
        private final PresenceTracker tracker = PresenceTracker.of($SCHEMA);
        private String resourceArn;

        private Builder() {}

        @Override
        public Schema schema() {
            return $SCHEMA;
        }

        /**
         * The Amazon Resource Name (ARN) of the DynamoDB resource to which the policy is attached. The resources you can
         * specify include tables and streams.
         *
         * <p><strong>Required</strong>
         * @return this builder.
         */
        public Builder resourceArn(String resourceArn) {
            this.resourceArn = Objects.requireNonNull(resourceArn, "resourceArn cannot be null");
            tracker.setMember($SCHEMA_RESOURCE_ARN);
            return this;
        }

        @Override
        public GetResourcePolicyInput build() {
            tracker.validate();
            return new GetResourcePolicyInput(this);
        }

        @Override
        @SuppressWarnings("unchecked")
        public void setMemberValue(Schema member, Object value) {
            switch (member.memberIndex()) {
                case 0 -> resourceArn((String) SchemaUtils.validateSameMember($SCHEMA_RESOURCE_ARN, member, value));
                default -> ShapeBuilder.super.setMemberValue(member, value);
            }
        }

        @Override
        public ShapeBuilder<GetResourcePolicyInput> errorCorrection() {
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
                    default -> throw new IllegalArgumentException("Unexpected member: " + member.memberName());
                }
            }
        }
    }
}
