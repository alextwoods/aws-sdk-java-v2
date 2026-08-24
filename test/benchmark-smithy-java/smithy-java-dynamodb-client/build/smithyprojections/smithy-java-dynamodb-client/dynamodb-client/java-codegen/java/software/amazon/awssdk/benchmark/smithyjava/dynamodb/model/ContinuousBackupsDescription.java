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
 * Represents the continuous backups and point in time recovery settings on the table.
 */
@SmithyGenerated
public final class ContinuousBackupsDescription implements SerializableStruct {

    public static final Schema $SCHEMA = Schemas.CONTINUOUS_BACKUPS_DESCRIPTION;
    private static final Schema $SCHEMA_CONTINUOUS_BACKUPS_STATUS = $SCHEMA.member("ContinuousBackupsStatus");
    private static final Schema $SCHEMA_POINT_IN_TIME_RECOVERY_DESCRIPTION = $SCHEMA.member("PointInTimeRecoveryDescription");

    public static final ShapeId $ID = $SCHEMA.id();

    private final transient ContinuousBackupsStatus continuousBackupsStatus;
    private final transient PointInTimeRecoveryDescription pointInTimeRecoveryDescription;

    private ContinuousBackupsDescription(Builder builder) {
        this.continuousBackupsStatus = builder.continuousBackupsStatus;
        this.pointInTimeRecoveryDescription = builder.pointInTimeRecoveryDescription;
    }

    /**
     * <code>ContinuousBackupsStatus</code> can be one of the following states: ENABLED, DISABLED
     */
    public ContinuousBackupsStatus getContinuousBackupsStatus() {
        return continuousBackupsStatus;
    }

    /**
     * The description of the point in time recovery settings applied to the table.
     */
    public PointInTimeRecoveryDescription getPointInTimeRecoveryDescription() {
        return pointInTimeRecoveryDescription;
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
        ContinuousBackupsDescription that = (ContinuousBackupsDescription) other;
        return Objects.equals(this.continuousBackupsStatus, that.continuousBackupsStatus)
               && Objects.equals(this.pointInTimeRecoveryDescription, that.pointInTimeRecoveryDescription);
    }

    @Override
    public int hashCode() {
        int $hc = Objects.hashCode(continuousBackupsStatus);
        $hc = 31 * $hc + Objects.hashCode(pointInTimeRecoveryDescription);
        return $hc;
    }

    @Override
    public Schema schema() {
        return $SCHEMA;
    }

    @Override
    public void serializeMembers(ShapeSerializer serializer) {
        serializer.writeString($SCHEMA_CONTINUOUS_BACKUPS_STATUS, continuousBackupsStatus.getValue());
        if (pointInTimeRecoveryDescription != null) {
            serializer.writeStruct($SCHEMA_POINT_IN_TIME_RECOVERY_DESCRIPTION, pointInTimeRecoveryDescription);
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T getMemberValue(Schema member) {
        return switch (member.memberIndex()) {
            case 0 -> (T) SchemaUtils.validateSameMember($SCHEMA_CONTINUOUS_BACKUPS_STATUS, member, continuousBackupsStatus);
            case 1 -> (T) SchemaUtils.validateSameMember($SCHEMA_POINT_IN_TIME_RECOVERY_DESCRIPTION, member, pointInTimeRecoveryDescription);
            default -> throw new IllegalArgumentException("Attempted to get non-existent member: " + member.id());
        };
    }

    /**
     * Create a new builder containing all the current property values of this object.
     *
     * <p><strong>Note:</strong> This method performs only a shallow copy of the original properties.
     *
     * @return a builder for {@link ContinuousBackupsDescription}.
     */
    public Builder toBuilder() {
        var builder = new Builder();
        builder.continuousBackupsStatus(this.continuousBackupsStatus);
        builder.pointInTimeRecoveryDescription(this.pointInTimeRecoveryDescription);
        return builder;
    }

    /**
     * @return returns a new Builder.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for {@link ContinuousBackupsDescription}.
     */
    public static final class Builder implements ShapeBuilder<ContinuousBackupsDescription> {
        private final PresenceTracker tracker = PresenceTracker.of($SCHEMA);
        private ContinuousBackupsStatus continuousBackupsStatus;
        private PointInTimeRecoveryDescription pointInTimeRecoveryDescription;

        private Builder() {}

        @Override
        public Schema schema() {
            return $SCHEMA;
        }

        /**
         * <code>ContinuousBackupsStatus</code> can be one of the following states: ENABLED, DISABLED
         *
         * <p><strong>Required</strong>
         * @return this builder.
         */
        public Builder continuousBackupsStatus(ContinuousBackupsStatus continuousBackupsStatus) {
            this.continuousBackupsStatus = Objects.requireNonNull(continuousBackupsStatus, "continuousBackupsStatus cannot be null");
            tracker.setMember($SCHEMA_CONTINUOUS_BACKUPS_STATUS);
            return this;
        }

        /**
         * The description of the point in time recovery settings applied to the table.
         *
         * @return this builder.
         */
        public Builder pointInTimeRecoveryDescription(PointInTimeRecoveryDescription pointInTimeRecoveryDescription) {
            this.pointInTimeRecoveryDescription = pointInTimeRecoveryDescription;
            return this;
        }

        @Override
        public ContinuousBackupsDescription build() {
            tracker.validate();
            return new ContinuousBackupsDescription(this);
        }

        @Override
        @SuppressWarnings("unchecked")
        public void setMemberValue(Schema member, Object value) {
            switch (member.memberIndex()) {
                case 0 -> continuousBackupsStatus((ContinuousBackupsStatus) SchemaUtils.validateSameMember($SCHEMA_CONTINUOUS_BACKUPS_STATUS, member, value));
                case 1 -> pointInTimeRecoveryDescription((PointInTimeRecoveryDescription) SchemaUtils.validateSameMember($SCHEMA_POINT_IN_TIME_RECOVERY_DESCRIPTION, member, value));
                default -> ShapeBuilder.super.setMemberValue(member, value);
            }
        }

        @Override
        public ShapeBuilder<ContinuousBackupsDescription> errorCorrection() {
            if (tracker.allSet()) {
                return this;
            }
            if (!tracker.checkMember($SCHEMA_CONTINUOUS_BACKUPS_STATUS)) {
                continuousBackupsStatus(ContinuousBackupsStatus.unknown(""));
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
                    case 0 -> builder.continuousBackupsStatus(ContinuousBackupsStatus.builder().deserializeMember(de, member).build());
                    case 1 -> builder.pointInTimeRecoveryDescription(PointInTimeRecoveryDescription.builder().deserializeMember(de, member).build());
                    default -> throw new IllegalArgumentException("Unexpected member: " + member.memberName());
                }
            }
        }
    }
}
