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

@SmithyGenerated
public final class GetBucketPolicyStatusOutput implements SerializableStruct {

    public static final Schema $SCHEMA = Schemas3.GET_BUCKET_POLICY_STATUS_OUTPUT;
    private static final Schema $SCHEMA_POLICY_STATUS = $SCHEMA.member("PolicyStatus");

    public static final ShapeId $ID = $SCHEMA.id();

    private final transient PolicyStatus policyStatus;

    private GetBucketPolicyStatusOutput(Builder builder) {
        this.policyStatus = builder.policyStatus;
    }

    /**
     * The policy status for the specified bucket.
     */
    public PolicyStatus getPolicyStatus() {
        return policyStatus;
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
        GetBucketPolicyStatusOutput that = (GetBucketPolicyStatusOutput) other;
        return Objects.equals(this.policyStatus, that.policyStatus);
    }

    @Override
    public int hashCode() {
        int $hc = Objects.hashCode(policyStatus);
        return $hc;
    }

    @Override
    public Schema schema() {
        return $SCHEMA;
    }

    @Override
    public void serializeMembers(ShapeSerializer serializer) {
        if (policyStatus != null) {
            serializer.writeStruct($SCHEMA_POLICY_STATUS, policyStatus);
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T getMemberValue(Schema member) {
        return switch (member.memberIndex()) {
            case 0 -> (T) SchemaUtils.validateSameMember($SCHEMA_POLICY_STATUS, member, policyStatus);
            default -> throw new IllegalArgumentException("Attempted to get non-existent member: " + member.id());
        };
    }

    /**
     * Create a new builder containing all the current property values of this object.
     *
     * <p><strong>Note:</strong> This method performs only a shallow copy of the original properties.
     *
     * @return a builder for {@link GetBucketPolicyStatusOutput}.
     */
    public Builder toBuilder() {
        var builder = new Builder();
        builder.policyStatus(this.policyStatus);
        return builder;
    }

    /**
     * @return returns a new Builder.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for {@link GetBucketPolicyStatusOutput}.
     */
    public static final class Builder implements ShapeBuilder<GetBucketPolicyStatusOutput> {
        private PolicyStatus policyStatus;

        private Builder() {}

        @Override
        public Schema schema() {
            return $SCHEMA;
        }

        /**
         * The policy status for the specified bucket.
         *
         * @return this builder.
         */
        public Builder policyStatus(PolicyStatus policyStatus) {
            this.policyStatus = policyStatus;
            return this;
        }

        @Override
        public GetBucketPolicyStatusOutput build() {
            return new GetBucketPolicyStatusOutput(this);
        }

        @Override
        @SuppressWarnings("unchecked")
        public void setMemberValue(Schema member, Object value) {
            switch (member.memberIndex()) {
                case 0 -> policyStatus((PolicyStatus) SchemaUtils.validateSameMember($SCHEMA_POLICY_STATUS, member, value));
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
                    case 0 -> builder.policyStatus(PolicyStatus.builder().deserializeMember(de, member).build());
                    default -> throw new IllegalArgumentException("Unexpected member: " + member.memberName());
                }
            }
        }
    }
}
