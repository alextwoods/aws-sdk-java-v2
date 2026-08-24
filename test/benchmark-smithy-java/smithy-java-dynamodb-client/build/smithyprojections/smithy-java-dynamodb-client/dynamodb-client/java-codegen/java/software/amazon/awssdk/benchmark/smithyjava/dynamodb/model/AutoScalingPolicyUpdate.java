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
 * Represents the auto scaling policy to be modified.
 */
@SmithyGenerated
public final class AutoScalingPolicyUpdate implements SerializableStruct {

    public static final Schema $SCHEMA = Schemas.AUTO_SCALING_POLICY_UPDATE;
    private static final Schema $SCHEMA_POLICY_NAME = $SCHEMA.member("PolicyName");
    private static final Schema $SCHEMA_TARGET_TRACKING_SCALING_POLICY_CONFIGURATION = $SCHEMA.member("TargetTrackingScalingPolicyConfiguration");

    public static final ShapeId $ID = $SCHEMA.id();

    private final transient String policyName;
    private final transient AutoScalingTargetTrackingScalingPolicyConfigurationUpdate targetTrackingScalingPolicyConfiguration;

    private AutoScalingPolicyUpdate(Builder builder) {
        this.policyName = builder.policyName;
        this.targetTrackingScalingPolicyConfiguration = builder.targetTrackingScalingPolicyConfiguration;
    }

    /**
     * The name of the scaling policy.
     */
    public String getPolicyName() {
        return policyName;
    }

    /**
     * Represents a target tracking scaling policy configuration.
     */
    public AutoScalingTargetTrackingScalingPolicyConfigurationUpdate getTargetTrackingScalingPolicyConfiguration() {
        return targetTrackingScalingPolicyConfiguration;
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
        AutoScalingPolicyUpdate that = (AutoScalingPolicyUpdate) other;
        return Objects.equals(this.policyName, that.policyName)
               && Objects.equals(this.targetTrackingScalingPolicyConfiguration, that.targetTrackingScalingPolicyConfiguration);
    }

    @Override
    public int hashCode() {
        int $hc = Objects.hashCode(policyName);
        $hc = 31 * $hc + Objects.hashCode(targetTrackingScalingPolicyConfiguration);
        return $hc;
    }

    @Override
    public Schema schema() {
        return $SCHEMA;
    }

    @Override
    public void serializeMembers(ShapeSerializer serializer) {
        if (policyName != null) {
            serializer.writeString($SCHEMA_POLICY_NAME, policyName);
        }
        if (targetTrackingScalingPolicyConfiguration != null) {
            serializer.writeStruct($SCHEMA_TARGET_TRACKING_SCALING_POLICY_CONFIGURATION, targetTrackingScalingPolicyConfiguration);
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T getMemberValue(Schema member) {
        return switch (member.memberIndex()) {
            case 0 -> (T) SchemaUtils.validateSameMember($SCHEMA_TARGET_TRACKING_SCALING_POLICY_CONFIGURATION, member, targetTrackingScalingPolicyConfiguration);
            case 1 -> (T) SchemaUtils.validateSameMember($SCHEMA_POLICY_NAME, member, policyName);
            default -> throw new IllegalArgumentException("Attempted to get non-existent member: " + member.id());
        };
    }

    /**
     * Create a new builder containing all the current property values of this object.
     *
     * <p><strong>Note:</strong> This method performs only a shallow copy of the original properties.
     *
     * @return a builder for {@link AutoScalingPolicyUpdate}.
     */
    public Builder toBuilder() {
        var builder = new Builder();
        builder.policyName(this.policyName);
        builder.targetTrackingScalingPolicyConfiguration(this.targetTrackingScalingPolicyConfiguration);
        return builder;
    }

    /**
     * @return returns a new Builder.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for {@link AutoScalingPolicyUpdate}.
     */
    public static final class Builder implements ShapeBuilder<AutoScalingPolicyUpdate> {
        private final PresenceTracker tracker = PresenceTracker.of($SCHEMA);
        private String policyName;
        private AutoScalingTargetTrackingScalingPolicyConfigurationUpdate targetTrackingScalingPolicyConfiguration;

        private Builder() {}

        @Override
        public Schema schema() {
            return $SCHEMA;
        }

        /**
         * The name of the scaling policy.
         *
         * @return this builder.
         */
        public Builder policyName(String policyName) {
            this.policyName = policyName;
            return this;
        }

        /**
         * Represents a target tracking scaling policy configuration.
         *
         * <p><strong>Required</strong>
         * @return this builder.
         */
        public Builder targetTrackingScalingPolicyConfiguration(AutoScalingTargetTrackingScalingPolicyConfigurationUpdate targetTrackingScalingPolicyConfiguration) {
            this.targetTrackingScalingPolicyConfiguration = Objects.requireNonNull(targetTrackingScalingPolicyConfiguration, "targetTrackingScalingPolicyConfiguration cannot be null");
            tracker.setMember($SCHEMA_TARGET_TRACKING_SCALING_POLICY_CONFIGURATION);
            return this;
        }

        @Override
        public AutoScalingPolicyUpdate build() {
            tracker.validate();
            return new AutoScalingPolicyUpdate(this);
        }

        @Override
        @SuppressWarnings("unchecked")
        public void setMemberValue(Schema member, Object value) {
            switch (member.memberIndex()) {
                case 0 -> targetTrackingScalingPolicyConfiguration((AutoScalingTargetTrackingScalingPolicyConfigurationUpdate) SchemaUtils.validateSameMember($SCHEMA_TARGET_TRACKING_SCALING_POLICY_CONFIGURATION, member, value));
                case 1 -> policyName((String) SchemaUtils.validateSameMember($SCHEMA_POLICY_NAME, member, value));
                default -> ShapeBuilder.super.setMemberValue(member, value);
            }
        }

        @Override
        public ShapeBuilder<AutoScalingPolicyUpdate> errorCorrection() {
            if (tracker.allSet()) {
                return this;
            }
            if (!tracker.checkMember($SCHEMA_TARGET_TRACKING_SCALING_POLICY_CONFIGURATION)) {
                tracker.setMember($SCHEMA_TARGET_TRACKING_SCALING_POLICY_CONFIGURATION);
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
                    case 0 -> builder.targetTrackingScalingPolicyConfiguration(AutoScalingTargetTrackingScalingPolicyConfigurationUpdate.builder().deserializeMember(de, member).build());
                    case 1 -> builder.policyName(de.readString(member));
                    default -> throw new IllegalArgumentException("Unexpected member: " + member.memberName());
                }
            }
        }
    }
}
