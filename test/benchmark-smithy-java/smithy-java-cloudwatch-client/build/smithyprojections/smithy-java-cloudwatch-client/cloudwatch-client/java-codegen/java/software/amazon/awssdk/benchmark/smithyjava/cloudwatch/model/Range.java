package software.amazon.awssdk.benchmark.smithyjava.cloudwatch.model;

import java.time.Instant;
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
 * Specifies one range of days or times to exclude from use for training an anomaly detection model.
 */
@SmithyGenerated
public final class Range implements SerializableStruct {

    public static final Schema $SCHEMA = Schemas.RANGE;
    private static final Schema $SCHEMA_START_TIME = $SCHEMA.member("StartTime");
    private static final Schema $SCHEMA_END_TIME = $SCHEMA.member("EndTime");

    public static final ShapeId $ID = $SCHEMA.id();

    private final transient Instant startTime;
    private final transient Instant endTime;

    private Range(Builder builder) {
        this.startTime = builder.startTime;
        this.endTime = builder.endTime;
    }

    /**
     * The start time of the range to exclude. The format is <code>yyyy-MM-dd'T'HH:mm:ss</code>. For example,
     * <code>2019-07-01T23:59:59</code>.
     */
    public Instant getStartTime() {
        return startTime;
    }

    /**
     * The end time of the range to exclude. The format is <code>yyyy-MM-dd'T'HH:mm:ss</code>. For example,
     * <code>2019-07-01T23:59:59</code>.
     */
    public Instant getEndTime() {
        return endTime;
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
        Range that = (Range) other;
        return Objects.equals(this.startTime, that.startTime)
               && Objects.equals(this.endTime, that.endTime);
    }

    @Override
    public int hashCode() {
        int $hc = Objects.hashCode(startTime);
        $hc = 31 * $hc + Objects.hashCode(endTime);
        return $hc;
    }

    @Override
    public Schema schema() {
        return $SCHEMA;
    }

    @Override
    public void serializeMembers(ShapeSerializer serializer) {
        if (startTime != null) {
            serializer.writeTimestamp($SCHEMA_START_TIME, startTime);
        }
        if (endTime != null) {
            serializer.writeTimestamp($SCHEMA_END_TIME, endTime);
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T getMemberValue(Schema member) {
        return switch (member.memberIndex()) {
            case 0 -> (T) SchemaUtils.validateSameMember($SCHEMA_START_TIME, member, startTime);
            case 1 -> (T) SchemaUtils.validateSameMember($SCHEMA_END_TIME, member, endTime);
            default -> throw new IllegalArgumentException("Attempted to get non-existent member: " + member.id());
        };
    }

    /**
     * Create a new builder containing all the current property values of this object.
     *
     * <p><strong>Note:</strong> This method performs only a shallow copy of the original properties.
     *
     * @return a builder for {@link Range}.
     */
    public Builder toBuilder() {
        var builder = new Builder();
        builder.startTime(this.startTime);
        builder.endTime(this.endTime);
        return builder;
    }

    /**
     * @return returns a new Builder.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for {@link Range}.
     */
    public static final class Builder implements ShapeBuilder<Range> {
        private final PresenceTracker tracker = PresenceTracker.of($SCHEMA);
        private Instant startTime;
        private Instant endTime;

        private Builder() {
            // Tell the tracker to assume clientOptional members are present.
            tracker.setMember($SCHEMA_START_TIME);
            tracker.setMember($SCHEMA_END_TIME);
        }

        @Override
        public Schema schema() {
            return $SCHEMA;
        }

        /**
         * The start time of the range to exclude. The format is <code>yyyy-MM-dd'T'HH:mm:ss</code>. For example,
         * <code>2019-07-01T23:59:59</code>.
         *
         * <p><strong>Required</strong>
         * @return this builder.
         */
        public Builder startTime(Instant startTime) {
            this.startTime = Objects.requireNonNull(startTime, "startTime cannot be null");
            tracker.setMember($SCHEMA_START_TIME);
            return this;
        }

        /**
         * The end time of the range to exclude. The format is <code>yyyy-MM-dd'T'HH:mm:ss</code>. For example,
         * <code>2019-07-01T23:59:59</code>.
         *
         * <p><strong>Required</strong>
         * @return this builder.
         */
        public Builder endTime(Instant endTime) {
            this.endTime = Objects.requireNonNull(endTime, "endTime cannot be null");
            tracker.setMember($SCHEMA_END_TIME);
            return this;
        }

        @Override
        public Range build() {
            tracker.validate();
            return new Range(this);
        }

        @Override
        @SuppressWarnings("unchecked")
        public void setMemberValue(Schema member, Object value) {
            switch (member.memberIndex()) {
                case 0 -> startTime((Instant) SchemaUtils.validateSameMember($SCHEMA_START_TIME, member, value));
                case 1 -> endTime((Instant) SchemaUtils.validateSameMember($SCHEMA_END_TIME, member, value));
                default -> ShapeBuilder.super.setMemberValue(member, value);
            }
        }

        @Override
        public ShapeBuilder<Range> errorCorrection() {
            if (tracker.allSet()) {
                return this;
            }
            if (!tracker.checkMember($SCHEMA_START_TIME)) {
                startTime(Instant.EPOCH);
            }
            if (!tracker.checkMember($SCHEMA_END_TIME)) {
                endTime(Instant.EPOCH);
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
                    case 0 -> builder.startTime(de.readTimestamp(member));
                    case 1 -> builder.endTime(de.readTimestamp(member));
                    default -> throw new IllegalArgumentException("Unexpected member: " + member.memberName());
                }
            }
        }
    }
}
