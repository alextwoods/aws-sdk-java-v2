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
 * Represents the settings used to enable point in time recovery.
 */
@SmithyGenerated
public final class PointInTimeRecoverySpecification implements SerializableStruct {

    public static final Schema $SCHEMA = Schemas.POINT_IN_TIME_RECOVERY_SPECIFICATION;
    private static final Schema $SCHEMA_POINT_IN_TIME_RECOVERY_ENABLED = $SCHEMA.member("PointInTimeRecoveryEnabled");
    private static final Schema $SCHEMA_RECOVERY_PERIOD_IN_DAYS = $SCHEMA.member("RecoveryPeriodInDays");

    public static final ShapeId $ID = $SCHEMA.id();

    private final transient boolean pointInTimeRecoveryEnabled;
    private final transient Integer recoveryPeriodInDays;

    private PointInTimeRecoverySpecification(Builder builder) {
        this.pointInTimeRecoveryEnabled = builder.pointInTimeRecoveryEnabled;
        this.recoveryPeriodInDays = builder.recoveryPeriodInDays;
    }

    /**
     * Indicates whether point in time recovery is enabled (true) or disabled (false) on the table.
     */
    public boolean isPointInTimeRecoveryEnabled() {
        return pointInTimeRecoveryEnabled;
    }

    /**
     * The number of preceding days for which continuous backups are taken and maintained. Your table data is only
     * recoverable to any point-in-time from within the configured recovery period. This parameter is optional. If no
     * value is provided, the value will default to 35.
     */
    public Integer getRecoveryPeriodInDays() {
        return recoveryPeriodInDays;
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
        PointInTimeRecoverySpecification that = (PointInTimeRecoverySpecification) other;
        return this.pointInTimeRecoveryEnabled == that.pointInTimeRecoveryEnabled
               && Objects.equals(this.recoveryPeriodInDays, that.recoveryPeriodInDays);
    }

    @Override
    public int hashCode() {
        int $hc = Boolean.hashCode(pointInTimeRecoveryEnabled);
        $hc = 31 * $hc + Objects.hashCode(recoveryPeriodInDays);
        return $hc;
    }

    @Override
    public Schema schema() {
        return $SCHEMA;
    }

    @Override
    public void serializeMembers(ShapeSerializer serializer) {
        serializer.writeBoolean($SCHEMA_POINT_IN_TIME_RECOVERY_ENABLED, pointInTimeRecoveryEnabled);
        if (recoveryPeriodInDays != null) {
            serializer.writeInteger($SCHEMA_RECOVERY_PERIOD_IN_DAYS, recoveryPeriodInDays);
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T getMemberValue(Schema member) {
        return switch (member.memberIndex()) {
            case 0 -> (T) SchemaUtils.validateSameMember($SCHEMA_POINT_IN_TIME_RECOVERY_ENABLED, member, pointInTimeRecoveryEnabled);
            case 1 -> (T) SchemaUtils.validateSameMember($SCHEMA_RECOVERY_PERIOD_IN_DAYS, member, recoveryPeriodInDays);
            default -> throw new IllegalArgumentException("Attempted to get non-existent member: " + member.id());
        };
    }

    /**
     * Create a new builder containing all the current property values of this object.
     *
     * <p><strong>Note:</strong> This method performs only a shallow copy of the original properties.
     *
     * @return a builder for {@link PointInTimeRecoverySpecification}.
     */
    public Builder toBuilder() {
        var builder = new Builder();
        builder.pointInTimeRecoveryEnabled(this.pointInTimeRecoveryEnabled);
        builder.recoveryPeriodInDays(this.recoveryPeriodInDays);
        return builder;
    }

    /**
     * @return returns a new Builder.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for {@link PointInTimeRecoverySpecification}.
     */
    public static final class Builder implements ShapeBuilder<PointInTimeRecoverySpecification> {
        private final PresenceTracker tracker = PresenceTracker.of($SCHEMA);
        private boolean pointInTimeRecoveryEnabled;
        private Integer recoveryPeriodInDays;

        private Builder() {}

        @Override
        public Schema schema() {
            return $SCHEMA;
        }

        /**
         * Indicates whether point in time recovery is enabled (true) or disabled (false) on the table.
         *
         * <p><strong>Required</strong>
         * @return this builder.
         */
        public Builder pointInTimeRecoveryEnabled(boolean pointInTimeRecoveryEnabled) {
            this.pointInTimeRecoveryEnabled = pointInTimeRecoveryEnabled;
            tracker.setMember($SCHEMA_POINT_IN_TIME_RECOVERY_ENABLED);
            return this;
        }

        /**
         * The number of preceding days for which continuous backups are taken and maintained. Your table data is only
         * recoverable to any point-in-time from within the configured recovery period. This parameter is optional. If no
         * value is provided, the value will default to 35.
         *
         * @return this builder.
         */
        public Builder recoveryPeriodInDays(Integer recoveryPeriodInDays) {
            this.recoveryPeriodInDays = recoveryPeriodInDays;
            return this;
        }

        @Override
        public PointInTimeRecoverySpecification build() {
            tracker.validate();
            return new PointInTimeRecoverySpecification(this);
        }

        @Override
        @SuppressWarnings("unchecked")
        public void setMemberValue(Schema member, Object value) {
            switch (member.memberIndex()) {
                case 0 -> pointInTimeRecoveryEnabled((boolean) SchemaUtils.validateSameMember($SCHEMA_POINT_IN_TIME_RECOVERY_ENABLED, member, value));
                case 1 -> recoveryPeriodInDays((Integer) SchemaUtils.validateSameMember($SCHEMA_RECOVERY_PERIOD_IN_DAYS, member, value));
                default -> ShapeBuilder.super.setMemberValue(member, value);
            }
        }

        @Override
        public ShapeBuilder<PointInTimeRecoverySpecification> errorCorrection() {
            if (tracker.allSet()) {
                return this;
            }
            if (!tracker.checkMember($SCHEMA_POINT_IN_TIME_RECOVERY_ENABLED)) {
                tracker.setMember($SCHEMA_POINT_IN_TIME_RECOVERY_ENABLED);
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
                    case 0 -> builder.pointInTimeRecoveryEnabled(de.readBoolean(member));
                    case 1 -> builder.recoveryPeriodInDays(de.readInteger(member));
                    default -> throw new IllegalArgumentException("Unexpected member: " + member.memberName());
                }
            }
        }
    }
}
