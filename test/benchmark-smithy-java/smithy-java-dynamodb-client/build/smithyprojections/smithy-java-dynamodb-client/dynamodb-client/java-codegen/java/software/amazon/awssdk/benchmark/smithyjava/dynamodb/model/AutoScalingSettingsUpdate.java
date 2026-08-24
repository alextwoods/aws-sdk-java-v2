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

/**
 * Represents the auto scaling settings to be modified for a global table or global secondary index.
 */
@SmithyGenerated
public final class AutoScalingSettingsUpdate implements SerializableStruct {

    public static final Schema $SCHEMA = Schemas.AUTO_SCALING_SETTINGS_UPDATE;
    private static final Schema $SCHEMA_MINIMUM_UNITS = $SCHEMA.member("MinimumUnits");
    private static final Schema $SCHEMA_MAXIMUM_UNITS = $SCHEMA.member("MaximumUnits");
    private static final Schema $SCHEMA_AUTO_SCALING_DISABLED = $SCHEMA.member("AutoScalingDisabled");
    private static final Schema $SCHEMA_AUTO_SCALING_ROLE_ARN = $SCHEMA.member("AutoScalingRoleArn");
    private static final Schema $SCHEMA_SCALING_POLICY_UPDATE = $SCHEMA.member("ScalingPolicyUpdate");

    public static final ShapeId $ID = $SCHEMA.id();

    private final transient Long minimumUnits;
    private final transient Long maximumUnits;
    private final transient Boolean autoScalingDisabled;
    private final transient String autoScalingRoleArn;
    private final transient AutoScalingPolicyUpdate scalingPolicyUpdate;

    private AutoScalingSettingsUpdate(Builder builder) {
        this.minimumUnits = builder.minimumUnits;
        this.maximumUnits = builder.maximumUnits;
        this.autoScalingDisabled = builder.autoScalingDisabled;
        this.autoScalingRoleArn = builder.autoScalingRoleArn;
        this.scalingPolicyUpdate = builder.scalingPolicyUpdate;
    }

    /**
     * The minimum capacity units that a global table or global secondary index should be scaled down to.
     */
    public Long getMinimumUnits() {
        return minimumUnits;
    }

    /**
     * The maximum capacity units that a global table or global secondary index should be scaled up to.
     */
    public Long getMaximumUnits() {
        return maximumUnits;
    }

    /**
     * Disabled auto scaling for this global table or global secondary index.
     */
    public Boolean isAutoScalingDisabled() {
        return autoScalingDisabled;
    }

    /**
     * Role ARN used for configuring auto scaling policy.
     */
    public String getAutoScalingRoleArn() {
        return autoScalingRoleArn;
    }

    /**
     * The scaling policy to apply for scaling target global table or global secondary index capacity units.
     */
    public AutoScalingPolicyUpdate getScalingPolicyUpdate() {
        return scalingPolicyUpdate;
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
        AutoScalingSettingsUpdate that = (AutoScalingSettingsUpdate) other;
        return Objects.equals(this.autoScalingDisabled, that.autoScalingDisabled)
               && Objects.equals(this.minimumUnits, that.minimumUnits)
               && Objects.equals(this.maximumUnits, that.maximumUnits)
               && Objects.equals(this.autoScalingRoleArn, that.autoScalingRoleArn)
               && Objects.equals(this.scalingPolicyUpdate, that.scalingPolicyUpdate);
    }

    @Override
    public int hashCode() {
        int $hc = Objects.hashCode(minimumUnits);
        $hc = 31 * $hc + Objects.hashCode(maximumUnits);
        $hc = 31 * $hc + Objects.hashCode(autoScalingDisabled);
        $hc = 31 * $hc + Objects.hashCode(autoScalingRoleArn);
        $hc = 31 * $hc + Objects.hashCode(scalingPolicyUpdate);
        return $hc;
    }

    @Override
    public Schema schema() {
        return $SCHEMA;
    }

    @Override
    public void serializeMembers(ShapeSerializer serializer) {
        if (minimumUnits != null) {
            serializer.writeLong($SCHEMA_MINIMUM_UNITS, minimumUnits);
        }
        if (maximumUnits != null) {
            serializer.writeLong($SCHEMA_MAXIMUM_UNITS, maximumUnits);
        }
        if (autoScalingDisabled != null) {
            serializer.writeBoolean($SCHEMA_AUTO_SCALING_DISABLED, autoScalingDisabled);
        }
        if (autoScalingRoleArn != null) {
            serializer.writeString($SCHEMA_AUTO_SCALING_ROLE_ARN, autoScalingRoleArn);
        }
        if (scalingPolicyUpdate != null) {
            serializer.writeStruct($SCHEMA_SCALING_POLICY_UPDATE, scalingPolicyUpdate);
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T getMemberValue(Schema member) {
        return switch (member.memberIndex()) {
            case 0 -> (T) SchemaUtils.validateSameMember($SCHEMA_MINIMUM_UNITS, member, minimumUnits);
            case 1 -> (T) SchemaUtils.validateSameMember($SCHEMA_MAXIMUM_UNITS, member, maximumUnits);
            case 2 -> (T) SchemaUtils.validateSameMember($SCHEMA_AUTO_SCALING_DISABLED, member, autoScalingDisabled);
            case 3 -> (T) SchemaUtils.validateSameMember($SCHEMA_AUTO_SCALING_ROLE_ARN, member, autoScalingRoleArn);
            case 4 -> (T) SchemaUtils.validateSameMember($SCHEMA_SCALING_POLICY_UPDATE, member, scalingPolicyUpdate);
            default -> throw new IllegalArgumentException("Attempted to get non-existent member: " + member.id());
        };
    }

    /**
     * Create a new builder containing all the current property values of this object.
     *
     * <p><strong>Note:</strong> This method performs only a shallow copy of the original properties.
     *
     * @return a builder for {@link AutoScalingSettingsUpdate}.
     */
    public Builder toBuilder() {
        var builder = new Builder();
        builder.minimumUnits(this.minimumUnits);
        builder.maximumUnits(this.maximumUnits);
        builder.autoScalingDisabled(this.autoScalingDisabled);
        builder.autoScalingRoleArn(this.autoScalingRoleArn);
        builder.scalingPolicyUpdate(this.scalingPolicyUpdate);
        return builder;
    }

    /**
     * @return returns a new Builder.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for {@link AutoScalingSettingsUpdate}.
     */
    public static final class Builder implements ShapeBuilder<AutoScalingSettingsUpdate> {
        private Long minimumUnits;
        private Long maximumUnits;
        private Boolean autoScalingDisabled;
        private String autoScalingRoleArn;
        private AutoScalingPolicyUpdate scalingPolicyUpdate;

        private Builder() {}

        @Override
        public Schema schema() {
            return $SCHEMA;
        }

        /**
         * The minimum capacity units that a global table or global secondary index should be scaled down to.
         *
         * @return this builder.
         */
        public Builder minimumUnits(Long minimumUnits) {
            this.minimumUnits = minimumUnits;
            return this;
        }

        /**
         * The maximum capacity units that a global table or global secondary index should be scaled up to.
         *
         * @return this builder.
         */
        public Builder maximumUnits(Long maximumUnits) {
            this.maximumUnits = maximumUnits;
            return this;
        }

        /**
         * Disabled auto scaling for this global table or global secondary index.
         *
         * @return this builder.
         */
        public Builder autoScalingDisabled(Boolean autoScalingDisabled) {
            this.autoScalingDisabled = autoScalingDisabled;
            return this;
        }

        /**
         * Role ARN used for configuring auto scaling policy.
         *
         * @return this builder.
         */
        public Builder autoScalingRoleArn(String autoScalingRoleArn) {
            this.autoScalingRoleArn = autoScalingRoleArn;
            return this;
        }

        /**
         * The scaling policy to apply for scaling target global table or global secondary index capacity units.
         *
         * @return this builder.
         */
        public Builder scalingPolicyUpdate(AutoScalingPolicyUpdate scalingPolicyUpdate) {
            this.scalingPolicyUpdate = scalingPolicyUpdate;
            return this;
        }

        @Override
        public AutoScalingSettingsUpdate build() {
            return new AutoScalingSettingsUpdate(this);
        }

        @Override
        @SuppressWarnings("unchecked")
        public void setMemberValue(Schema member, Object value) {
            switch (member.memberIndex()) {
                case 0 -> minimumUnits((Long) SchemaUtils.validateSameMember($SCHEMA_MINIMUM_UNITS, member, value));
                case 1 -> maximumUnits((Long) SchemaUtils.validateSameMember($SCHEMA_MAXIMUM_UNITS, member, value));
                case 2 -> autoScalingDisabled((Boolean) SchemaUtils.validateSameMember($SCHEMA_AUTO_SCALING_DISABLED, member, value));
                case 3 -> autoScalingRoleArn((String) SchemaUtils.validateSameMember($SCHEMA_AUTO_SCALING_ROLE_ARN, member, value));
                case 4 -> scalingPolicyUpdate((AutoScalingPolicyUpdate) SchemaUtils.validateSameMember($SCHEMA_SCALING_POLICY_UPDATE, member, value));
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
                    case 0 -> builder.minimumUnits(de.readLong(member));
                    case 1 -> builder.maximumUnits(de.readLong(member));
                    case 2 -> builder.autoScalingDisabled(de.readBoolean(member));
                    case 3 -> builder.autoScalingRoleArn(de.readString(member));
                    case 4 -> builder.scalingPolicyUpdate(AutoScalingPolicyUpdate.builder().deserializeMember(de, member).build());
                    default -> throw new IllegalArgumentException("Unexpected member: " + member.memberName());
                }
            }
        }
    }
}
