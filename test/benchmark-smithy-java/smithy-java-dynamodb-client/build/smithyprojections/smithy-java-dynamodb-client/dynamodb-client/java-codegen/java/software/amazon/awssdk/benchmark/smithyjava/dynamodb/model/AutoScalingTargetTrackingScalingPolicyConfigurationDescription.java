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
 * Represents the properties of a target tracking scaling policy.
 */
@SmithyGenerated
public final class AutoScalingTargetTrackingScalingPolicyConfigurationDescription implements SerializableStruct {

    public static final Schema $SCHEMA = Schemas.AUTO_SCALING_TARGET_TRACKING_SCALING_POLICY_CONFIGURATION_DESCRIPTION;
    private static final Schema $SCHEMA_DISABLE_SCALE_IN = $SCHEMA.member("DisableScaleIn");
    private static final Schema $SCHEMA_SCALE_IN_COOLDOWN = $SCHEMA.member("ScaleInCooldown");
    private static final Schema $SCHEMA_SCALE_OUT_COOLDOWN = $SCHEMA.member("ScaleOutCooldown");
    private static final Schema $SCHEMA_TARGET_VALUE = $SCHEMA.member("TargetValue");

    public static final ShapeId $ID = $SCHEMA.id();

    private final transient Boolean disableScaleIn;
    private final transient Integer scaleInCooldown;
    private final transient Integer scaleOutCooldown;
    private final transient double targetValue;

    private AutoScalingTargetTrackingScalingPolicyConfigurationDescription(Builder builder) {
        this.disableScaleIn = builder.disableScaleIn;
        this.scaleInCooldown = builder.scaleInCooldown;
        this.scaleOutCooldown = builder.scaleOutCooldown;
        this.targetValue = builder.targetValue;
    }

    /**
     * Indicates whether scale in by the target tracking policy is disabled. If the value is true, scale in is disabled
     * and the target tracking policy won't remove capacity from the scalable resource. Otherwise, scale in is enabled
     * and the target tracking policy can remove capacity from the scalable resource. The default value is false.
     */
    public Boolean isDisableScaleIn() {
        return disableScaleIn;
    }

    /**
     * The amount of time, in seconds, after a scale in activity completes before another scale in activity can start.
     * The cooldown period is used to block subsequent scale in requests until it has expired. You should scale in
     * conservatively to protect your application's availability. However, if another alarm triggers a scale out policy
     * during the cooldown period after a scale-in, application auto scaling scales out your scalable target
     * immediately.
     */
    public Integer getScaleInCooldown() {
        return scaleInCooldown;
    }

    /**
     * The amount of time, in seconds, after a scale out activity completes before another scale out activity can start.
     * While the cooldown period is in effect, the capacity that has been added by the previous scale out event that
     * initiated the cooldown is calculated as part of the desired capacity for the next scale out. You should
     * continuously (but not excessively) scale out.
     */
    public Integer getScaleOutCooldown() {
        return scaleOutCooldown;
    }

    /**
     * The target value for the metric. The range is 8.515920e-109 to 1.174271e+108 (Base 10) or 2e-360 to 2e360 (Base
     * 2).
     */
    public double getTargetValue() {
        return targetValue;
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
        AutoScalingTargetTrackingScalingPolicyConfigurationDescription that = (AutoScalingTargetTrackingScalingPolicyConfigurationDescription) other;
        return Double.compare(this.targetValue, that.targetValue) == 0
               && Objects.equals(this.disableScaleIn, that.disableScaleIn)
               && Objects.equals(this.scaleInCooldown, that.scaleInCooldown)
               && Objects.equals(this.scaleOutCooldown, that.scaleOutCooldown);
    }

    @Override
    public int hashCode() {
        int $hc = Objects.hashCode(disableScaleIn);
        $hc = 31 * $hc + Objects.hashCode(scaleInCooldown);
        $hc = 31 * $hc + Objects.hashCode(scaleOutCooldown);
        $hc = 31 * $hc + Double.hashCode(targetValue);
        return $hc;
    }

    @Override
    public Schema schema() {
        return $SCHEMA;
    }

    @Override
    public void serializeMembers(ShapeSerializer serializer) {
        if (disableScaleIn != null) {
            serializer.writeBoolean($SCHEMA_DISABLE_SCALE_IN, disableScaleIn);
        }
        if (scaleInCooldown != null) {
            serializer.writeInteger($SCHEMA_SCALE_IN_COOLDOWN, scaleInCooldown);
        }
        if (scaleOutCooldown != null) {
            serializer.writeInteger($SCHEMA_SCALE_OUT_COOLDOWN, scaleOutCooldown);
        }
        serializer.writeDouble($SCHEMA_TARGET_VALUE, targetValue);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T getMemberValue(Schema member) {
        return switch (member.memberIndex()) {
            case 0 -> (T) SchemaUtils.validateSameMember($SCHEMA_TARGET_VALUE, member, targetValue);
            case 1 -> (T) SchemaUtils.validateSameMember($SCHEMA_DISABLE_SCALE_IN, member, disableScaleIn);
            case 2 -> (T) SchemaUtils.validateSameMember($SCHEMA_SCALE_IN_COOLDOWN, member, scaleInCooldown);
            case 3 -> (T) SchemaUtils.validateSameMember($SCHEMA_SCALE_OUT_COOLDOWN, member, scaleOutCooldown);
            default -> throw new IllegalArgumentException("Attempted to get non-existent member: " + member.id());
        };
    }

    /**
     * Create a new builder containing all the current property values of this object.
     *
     * <p><strong>Note:</strong> This method performs only a shallow copy of the original properties.
     *
     * @return a builder for {@link AutoScalingTargetTrackingScalingPolicyConfigurationDescription}.
     */
    public Builder toBuilder() {
        var builder = new Builder();
        builder.disableScaleIn(this.disableScaleIn);
        builder.scaleInCooldown(this.scaleInCooldown);
        builder.scaleOutCooldown(this.scaleOutCooldown);
        builder.targetValue(this.targetValue);
        return builder;
    }

    /**
     * @return returns a new Builder.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for {@link AutoScalingTargetTrackingScalingPolicyConfigurationDescription}.
     */
    public static final class Builder implements ShapeBuilder<AutoScalingTargetTrackingScalingPolicyConfigurationDescription> {
        private final PresenceTracker tracker = PresenceTracker.of($SCHEMA);
        private Boolean disableScaleIn;
        private Integer scaleInCooldown;
        private Integer scaleOutCooldown;
        private double targetValue;

        private Builder() {}

        @Override
        public Schema schema() {
            return $SCHEMA;
        }

        /**
         * Indicates whether scale in by the target tracking policy is disabled. If the value is true, scale in is disabled
         * and the target tracking policy won't remove capacity from the scalable resource. Otherwise, scale in is enabled
         * and the target tracking policy can remove capacity from the scalable resource. The default value is false.
         *
         * @return this builder.
         */
        public Builder disableScaleIn(Boolean disableScaleIn) {
            this.disableScaleIn = disableScaleIn;
            return this;
        }

        /**
         * The amount of time, in seconds, after a scale in activity completes before another scale in activity can start.
         * The cooldown period is used to block subsequent scale in requests until it has expired. You should scale in
         * conservatively to protect your application's availability. However, if another alarm triggers a scale out policy
         * during the cooldown period after a scale-in, application auto scaling scales out your scalable target
         * immediately.
         *
         * @return this builder.
         */
        public Builder scaleInCooldown(Integer scaleInCooldown) {
            this.scaleInCooldown = scaleInCooldown;
            return this;
        }

        /**
         * The amount of time, in seconds, after a scale out activity completes before another scale out activity can start.
         * While the cooldown period is in effect, the capacity that has been added by the previous scale out event that
         * initiated the cooldown is calculated as part of the desired capacity for the next scale out. You should
         * continuously (but not excessively) scale out.
         *
         * @return this builder.
         */
        public Builder scaleOutCooldown(Integer scaleOutCooldown) {
            this.scaleOutCooldown = scaleOutCooldown;
            return this;
        }

        /**
         * The target value for the metric. The range is 8.515920e-109 to 1.174271e+108 (Base 10) or 2e-360 to 2e360 (Base
         * 2).
         *
         * <p><strong>Required</strong>
         * @return this builder.
         */
        public Builder targetValue(double targetValue) {
            this.targetValue = targetValue;
            tracker.setMember($SCHEMA_TARGET_VALUE);
            return this;
        }

        @Override
        public AutoScalingTargetTrackingScalingPolicyConfigurationDescription build() {
            tracker.validate();
            return new AutoScalingTargetTrackingScalingPolicyConfigurationDescription(this);
        }

        @Override
        @SuppressWarnings("unchecked")
        public void setMemberValue(Schema member, Object value) {
            switch (member.memberIndex()) {
                case 0 -> targetValue((double) SchemaUtils.validateSameMember($SCHEMA_TARGET_VALUE, member, value));
                case 1 -> disableScaleIn((Boolean) SchemaUtils.validateSameMember($SCHEMA_DISABLE_SCALE_IN, member, value));
                case 2 -> scaleInCooldown((Integer) SchemaUtils.validateSameMember($SCHEMA_SCALE_IN_COOLDOWN, member, value));
                case 3 -> scaleOutCooldown((Integer) SchemaUtils.validateSameMember($SCHEMA_SCALE_OUT_COOLDOWN, member, value));
                default -> ShapeBuilder.super.setMemberValue(member, value);
            }
        }

        @Override
        public ShapeBuilder<AutoScalingTargetTrackingScalingPolicyConfigurationDescription> errorCorrection() {
            if (tracker.allSet()) {
                return this;
            }
            if (!tracker.checkMember($SCHEMA_TARGET_VALUE)) {
                tracker.setMember($SCHEMA_TARGET_VALUE);
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
                    case 0 -> builder.targetValue(de.readDouble(member));
                    case 1 -> builder.disableScaleIn(de.readBoolean(member));
                    case 2 -> builder.scaleInCooldown(de.readInteger(member));
                    case 3 -> builder.scaleOutCooldown(de.readInteger(member));
                    default -> throw new IllegalArgumentException("Unexpected member: " + member.memberName());
                }
            }
        }
    }
}
