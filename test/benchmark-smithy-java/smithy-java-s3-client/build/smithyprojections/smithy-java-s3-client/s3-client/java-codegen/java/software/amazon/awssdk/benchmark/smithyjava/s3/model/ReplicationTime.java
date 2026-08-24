package software.amazon.awssdk.benchmark.smithyjava.s3.model;

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
 * A container specifying S3 Replication Time Control (S3 RTC) related information, including whether S3 RTC is enabled
 * and the time when all objects and operations on objects must be replicated. Must be specified together with a
 * <code>Metrics</code> block.
 */
@SmithyGenerated
public final class ReplicationTime implements SerializableStruct {

    public static final Schema $SCHEMA = Schemas3.REPLICATION_TIME;
    private static final Schema $SCHEMA_STATUS = $SCHEMA.member("Status");
    private static final Schema $SCHEMA_TIME = $SCHEMA.member("Time");

    public static final ShapeId $ID = $SCHEMA.id();

    private final transient ReplicationTimeStatus status;
    private final transient ReplicationTimeValue time;

    private ReplicationTime(Builder builder) {
        this.status = builder.status;
        this.time = builder.time;
    }

    /**
     * Specifies whether the replication time is enabled.
     */
    public ReplicationTimeStatus getStatus() {
        return status;
    }

    /**
     * A container specifying the time by which replication should be complete for all objects and operations on
     * objects.
     */
    public ReplicationTimeValue getTime() {
        return time;
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
        ReplicationTime that = (ReplicationTime) other;
        return Objects.equals(this.status, that.status)
               && Objects.equals(this.time, that.time);
    }

    @Override
    public int hashCode() {
        int $hc = Objects.hashCode(status);
        $hc = 31 * $hc + Objects.hashCode(time);
        return $hc;
    }

    @Override
    public Schema schema() {
        return $SCHEMA;
    }

    @Override
    public void serializeMembers(ShapeSerializer serializer) {
        serializer.writeString($SCHEMA_STATUS, status.getValue());
        if (time != null) {
            serializer.writeStruct($SCHEMA_TIME, time);
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T getMemberValue(Schema member) {
        return switch (member.memberIndex()) {
            case 0 -> (T) SchemaUtils.validateSameMember($SCHEMA_STATUS, member, status);
            case 1 -> (T) SchemaUtils.validateSameMember($SCHEMA_TIME, member, time);
            default -> throw new IllegalArgumentException("Attempted to get non-existent member: " + member.id());
        };
    }

    /**
     * Create a new builder containing all the current property values of this object.
     *
     * <p><strong>Note:</strong> This method performs only a shallow copy of the original properties.
     *
     * @return a builder for {@link ReplicationTime}.
     */
    public Builder toBuilder() {
        var builder = new Builder();
        builder.status(this.status);
        builder.time(this.time);
        return builder;
    }

    /**
     * @return returns a new Builder.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for {@link ReplicationTime}.
     */
    public static final class Builder implements ShapeBuilder<ReplicationTime> {
        private final PresenceTracker tracker = PresenceTracker.of($SCHEMA);
        private ReplicationTimeStatus status;
        private ReplicationTimeValue time;

        private Builder() {}

        @Override
        public Schema schema() {
            return $SCHEMA;
        }

        /**
         * Specifies whether the replication time is enabled.
         *
         * <p><strong>Required</strong>
         * @return this builder.
         */
        public Builder status(ReplicationTimeStatus status) {
            this.status = Objects.requireNonNull(status, "status cannot be null");
            tracker.setMember($SCHEMA_STATUS);
            return this;
        }

        /**
         * A container specifying the time by which replication should be complete for all objects and operations on
         * objects.
         *
         * <p><strong>Required</strong>
         * @return this builder.
         */
        public Builder time(ReplicationTimeValue time) {
            this.time = Objects.requireNonNull(time, "time cannot be null");
            tracker.setMember($SCHEMA_TIME);
            return this;
        }

        @Override
        public ReplicationTime build() {
            tracker.validate();
            return new ReplicationTime(this);
        }

        @Override
        @SuppressWarnings("unchecked")
        public void setMemberValue(Schema member, Object value) {
            switch (member.memberIndex()) {
                case 0 -> status((ReplicationTimeStatus) SchemaUtils.validateSameMember($SCHEMA_STATUS, member, value));
                case 1 -> time((ReplicationTimeValue) SchemaUtils.validateSameMember($SCHEMA_TIME, member, value));
                default -> ShapeBuilder.super.setMemberValue(member, value);
            }
        }

        @Override
        public ShapeBuilder<ReplicationTime> errorCorrection() {
            if (tracker.allSet()) {
                return this;
            }
            if (!tracker.checkMember($SCHEMA_STATUS)) {
                status(ReplicationTimeStatus.unknown(""));
            }
            if (!tracker.checkMember($SCHEMA_TIME)) {
                tracker.setMember($SCHEMA_TIME);
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
                    case 0 -> builder.status(ReplicationTimeStatus.builder().deserializeMember(de, member).build());
                    case 1 -> builder.time(ReplicationTimeValue.builder().deserializeMember(de, member).build());
                    default -> throw new IllegalArgumentException("Unexpected member: " + member.memberName());
                }
            }
        }
    }
}
