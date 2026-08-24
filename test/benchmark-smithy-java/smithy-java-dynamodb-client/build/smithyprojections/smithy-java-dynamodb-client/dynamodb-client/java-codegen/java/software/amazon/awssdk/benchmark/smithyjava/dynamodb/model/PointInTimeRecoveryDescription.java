package software.amazon.awssdk.benchmark.smithyjava.dynamodb.model;

import java.time.Instant;
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
 * The description of the point in time settings applied to the table.
 */
@SmithyGenerated
public final class PointInTimeRecoveryDescription implements SerializableStruct {

    public static final Schema $SCHEMA = Schemas.POINT_IN_TIME_RECOVERY_DESCRIPTION;
    private static final Schema $SCHEMA_POINT_IN_TIME_RECOVERY_STATUS = $SCHEMA.member("PointInTimeRecoveryStatus");
    private static final Schema $SCHEMA_RECOVERY_PERIOD_IN_DAYS = $SCHEMA.member("RecoveryPeriodInDays");
    private static final Schema $SCHEMA_EARLIEST_RESTORABLE_DATE_TIME = $SCHEMA.member("EarliestRestorableDateTime");
    private static final Schema $SCHEMA_LATEST_RESTORABLE_DATE_TIME = $SCHEMA.member("LatestRestorableDateTime");

    public static final ShapeId $ID = $SCHEMA.id();

    private final transient PointInTimeRecoveryStatus pointInTimeRecoveryStatus;
    private final transient Integer recoveryPeriodInDays;
    private final transient Instant earliestRestorableDateTime;
    private final transient Instant latestRestorableDateTime;

    private PointInTimeRecoveryDescription(Builder builder) {
        this.pointInTimeRecoveryStatus = builder.pointInTimeRecoveryStatus;
        this.recoveryPeriodInDays = builder.recoveryPeriodInDays;
        this.earliestRestorableDateTime = builder.earliestRestorableDateTime;
        this.latestRestorableDateTime = builder.latestRestorableDateTime;
    }

    /**
     * The current state of point in time recovery:
     *
     * <ul>
     *   <li>
     *     <code>ENABLED</code> - Point in time recovery is enabled.
     *   </li>
     *   <li>
     *     <code>DISABLED</code> - Point in time recovery is disabled.
     *   </li>
     * </ul>
     */
    public PointInTimeRecoveryStatus getPointInTimeRecoveryStatus() {
        return pointInTimeRecoveryStatus;
    }

    /**
     * The number of preceding days for which continuous backups are taken and maintained. Your table data is only
     * recoverable to any point-in-time from within the configured recovery period. This parameter is optional.
     */
    public Integer getRecoveryPeriodInDays() {
        return recoveryPeriodInDays;
    }

    /**
     * Specifies the earliest point in time you can restore your table to. You can restore your table to any point in
     * time during the last 35 days.
     */
    public Instant getEarliestRestorableDateTime() {
        return earliestRestorableDateTime;
    }

    /**
     * <code>LatestRestorableDateTime</code> is typically 5 minutes before the current time.
     */
    public Instant getLatestRestorableDateTime() {
        return latestRestorableDateTime;
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
        PointInTimeRecoveryDescription that = (PointInTimeRecoveryDescription) other;
        return Objects.equals(this.recoveryPeriodInDays, that.recoveryPeriodInDays)
               && Objects.equals(this.pointInTimeRecoveryStatus, that.pointInTimeRecoveryStatus)
               && Objects.equals(this.earliestRestorableDateTime, that.earliestRestorableDateTime)
               && Objects.equals(this.latestRestorableDateTime, that.latestRestorableDateTime);
    }

    @Override
    public int hashCode() {
        int $hc = Objects.hashCode(pointInTimeRecoveryStatus);
        $hc = 31 * $hc + Objects.hashCode(recoveryPeriodInDays);
        $hc = 31 * $hc + Objects.hashCode(earliestRestorableDateTime);
        $hc = 31 * $hc + Objects.hashCode(latestRestorableDateTime);
        return $hc;
    }

    @Override
    public Schema schema() {
        return $SCHEMA;
    }

    @Override
    public void serializeMembers(ShapeSerializer serializer) {
        if (pointInTimeRecoveryStatus != null) {
            serializer.writeString($SCHEMA_POINT_IN_TIME_RECOVERY_STATUS, pointInTimeRecoveryStatus.getValue());
        }
        if (recoveryPeriodInDays != null) {
            serializer.writeInteger($SCHEMA_RECOVERY_PERIOD_IN_DAYS, recoveryPeriodInDays);
        }
        if (earliestRestorableDateTime != null) {
            serializer.writeTimestamp($SCHEMA_EARLIEST_RESTORABLE_DATE_TIME, earliestRestorableDateTime);
        }
        if (latestRestorableDateTime != null) {
            serializer.writeTimestamp($SCHEMA_LATEST_RESTORABLE_DATE_TIME, latestRestorableDateTime);
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T getMemberValue(Schema member) {
        return switch (member.memberIndex()) {
            case 0 -> (T) SchemaUtils.validateSameMember($SCHEMA_POINT_IN_TIME_RECOVERY_STATUS, member, pointInTimeRecoveryStatus);
            case 1 -> (T) SchemaUtils.validateSameMember($SCHEMA_RECOVERY_PERIOD_IN_DAYS, member, recoveryPeriodInDays);
            case 2 -> (T) SchemaUtils.validateSameMember($SCHEMA_EARLIEST_RESTORABLE_DATE_TIME, member, earliestRestorableDateTime);
            case 3 -> (T) SchemaUtils.validateSameMember($SCHEMA_LATEST_RESTORABLE_DATE_TIME, member, latestRestorableDateTime);
            default -> throw new IllegalArgumentException("Attempted to get non-existent member: " + member.id());
        };
    }

    /**
     * Create a new builder containing all the current property values of this object.
     *
     * <p><strong>Note:</strong> This method performs only a shallow copy of the original properties.
     *
     * @return a builder for {@link PointInTimeRecoveryDescription}.
     */
    public Builder toBuilder() {
        var builder = new Builder();
        builder.pointInTimeRecoveryStatus(this.pointInTimeRecoveryStatus);
        builder.recoveryPeriodInDays(this.recoveryPeriodInDays);
        builder.earliestRestorableDateTime(this.earliestRestorableDateTime);
        builder.latestRestorableDateTime(this.latestRestorableDateTime);
        return builder;
    }

    /**
     * @return returns a new Builder.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for {@link PointInTimeRecoveryDescription}.
     */
    public static final class Builder implements ShapeBuilder<PointInTimeRecoveryDescription> {
        private PointInTimeRecoveryStatus pointInTimeRecoveryStatus;
        private Integer recoveryPeriodInDays;
        private Instant earliestRestorableDateTime;
        private Instant latestRestorableDateTime;

        private Builder() {}

        @Override
        public Schema schema() {
            return $SCHEMA;
        }

        /**
         * The current state of point in time recovery:
         *
         * <ul>
         *   <li>
         *     <code>ENABLED</code> - Point in time recovery is enabled.
         *   </li>
         *   <li>
         *     <code>DISABLED</code> - Point in time recovery is disabled.
         *   </li>
         * </ul>
         *
         * @return this builder.
         */
        public Builder pointInTimeRecoveryStatus(PointInTimeRecoveryStatus pointInTimeRecoveryStatus) {
            this.pointInTimeRecoveryStatus = pointInTimeRecoveryStatus;
            return this;
        }

        /**
         * The number of preceding days for which continuous backups are taken and maintained. Your table data is only
         * recoverable to any point-in-time from within the configured recovery period. This parameter is optional.
         *
         * @return this builder.
         */
        public Builder recoveryPeriodInDays(Integer recoveryPeriodInDays) {
            this.recoveryPeriodInDays = recoveryPeriodInDays;
            return this;
        }

        /**
         * Specifies the earliest point in time you can restore your table to. You can restore your table to any point in
         * time during the last 35 days.
         *
         * @return this builder.
         */
        public Builder earliestRestorableDateTime(Instant earliestRestorableDateTime) {
            this.earliestRestorableDateTime = earliestRestorableDateTime;
            return this;
        }

        /**
         * <code>LatestRestorableDateTime</code> is typically 5 minutes before the current time.
         *
         * @return this builder.
         */
        public Builder latestRestorableDateTime(Instant latestRestorableDateTime) {
            this.latestRestorableDateTime = latestRestorableDateTime;
            return this;
        }

        @Override
        public PointInTimeRecoveryDescription build() {
            return new PointInTimeRecoveryDescription(this);
        }

        @Override
        @SuppressWarnings("unchecked")
        public void setMemberValue(Schema member, Object value) {
            switch (member.memberIndex()) {
                case 0 -> pointInTimeRecoveryStatus((PointInTimeRecoveryStatus) SchemaUtils.validateSameMember($SCHEMA_POINT_IN_TIME_RECOVERY_STATUS, member, value));
                case 1 -> recoveryPeriodInDays((Integer) SchemaUtils.validateSameMember($SCHEMA_RECOVERY_PERIOD_IN_DAYS, member, value));
                case 2 -> earliestRestorableDateTime((Instant) SchemaUtils.validateSameMember($SCHEMA_EARLIEST_RESTORABLE_DATE_TIME, member, value));
                case 3 -> latestRestorableDateTime((Instant) SchemaUtils.validateSameMember($SCHEMA_LATEST_RESTORABLE_DATE_TIME, member, value));
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
                    case 0 -> builder.pointInTimeRecoveryStatus(PointInTimeRecoveryStatus.builder().deserializeMember(de, member).build());
                    case 1 -> builder.recoveryPeriodInDays(de.readInteger(member));
                    case 2 -> builder.earliestRestorableDateTime(de.readTimestamp(member));
                    case 3 -> builder.latestRestorableDateTime(de.readTimestamp(member));
                    default -> throw new IllegalArgumentException("Unexpected member: " + member.memberName());
                }
            }
        }
    }
}
