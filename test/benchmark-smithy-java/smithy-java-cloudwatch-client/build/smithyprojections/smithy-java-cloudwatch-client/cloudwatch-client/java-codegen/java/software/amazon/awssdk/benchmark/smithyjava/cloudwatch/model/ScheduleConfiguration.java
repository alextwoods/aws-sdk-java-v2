package software.amazon.awssdk.benchmark.smithyjava.cloudwatch.model;

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
 * Contains the schedule expression and time-range offsets that define when a scheduled query runs and what time range
 * each execution covers.
 */
@SmithyGenerated
public final class ScheduleConfiguration implements SerializableStruct {

    public static final Schema $SCHEMA = Schemas.SCHEDULE_CONFIGURATION;
    private static final Schema $SCHEMA_SCHEDULE_EXPRESSION = $SCHEMA.member("ScheduleExpression");
    private static final Schema $SCHEMA_START_TIME_OFFSET = $SCHEMA.member("StartTimeOffset");
    private static final Schema $SCHEMA_END_TIME_OFFSET = $SCHEMA.member("EndTimeOffset");

    public static final ShapeId $ID = $SCHEMA.id();

    private final transient String scheduleExpression;
    private final transient Long startTimeOffset;
    private final transient Long endTimeOffset;

    private ScheduleConfiguration(Builder builder) {
        this.scheduleExpression = builder.scheduleExpression;
        this.startTimeOffset = builder.startTimeOffset;
        this.endTimeOffset = builder.endTimeOffset;
    }

    /**
     * The schedule expression that defines how often the underlying CloudWatch Logs scheduled query runs. Specify a
     * <code>rate()</code> expression, for example <code>rate(5 minutes)</code>.
     */
    public String getScheduleExpression() {
        return scheduleExpression;
    }

    /**
     * The offset, in seconds, before the scheduled execution time at which the query time range begins. For example, an
     * offset of 360 (6 minutes) on a query running at 12:05:00 starts the query time range at 11:59:00.
     */
    public Long getStartTimeOffset() {
        return startTimeOffset;
    }

    /**
     * The offset, in seconds, before the scheduled execution time at which the query time range ends. Must be
     * non-negative and less than <code>StartTimeOffset</code>. The default is 0.
     */
    public Long getEndTimeOffset() {
        return endTimeOffset;
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
        ScheduleConfiguration that = (ScheduleConfiguration) other;
        return Objects.equals(this.startTimeOffset, that.startTimeOffset)
               && Objects.equals(this.endTimeOffset, that.endTimeOffset)
               && Objects.equals(this.scheduleExpression, that.scheduleExpression);
    }

    @Override
    public int hashCode() {
        int $hc = Objects.hashCode(scheduleExpression);
        $hc = 31 * $hc + Objects.hashCode(startTimeOffset);
        $hc = 31 * $hc + Objects.hashCode(endTimeOffset);
        return $hc;
    }

    @Override
    public Schema schema() {
        return $SCHEMA;
    }

    @Override
    public void serializeMembers(ShapeSerializer serializer) {
        if (scheduleExpression != null) {
            serializer.writeString($SCHEMA_SCHEDULE_EXPRESSION, scheduleExpression);
        }
        if (startTimeOffset != null) {
            serializer.writeLong($SCHEMA_START_TIME_OFFSET, startTimeOffset);
        }
        if (endTimeOffset != null) {
            serializer.writeLong($SCHEMA_END_TIME_OFFSET, endTimeOffset);
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T getMemberValue(Schema member) {
        return switch (member.memberIndex()) {
            case 0 -> (T) SchemaUtils.validateSameMember($SCHEMA_SCHEDULE_EXPRESSION, member, scheduleExpression);
            case 1 -> (T) SchemaUtils.validateSameMember($SCHEMA_START_TIME_OFFSET, member, startTimeOffset);
            case 2 -> (T) SchemaUtils.validateSameMember($SCHEMA_END_TIME_OFFSET, member, endTimeOffset);
            default -> throw new IllegalArgumentException("Attempted to get non-existent member: " + member.id());
        };
    }

    /**
     * Create a new builder containing all the current property values of this object.
     *
     * <p><strong>Note:</strong> This method performs only a shallow copy of the original properties.
     *
     * @return a builder for {@link ScheduleConfiguration}.
     */
    public Builder toBuilder() {
        var builder = new Builder();
        builder.scheduleExpression(this.scheduleExpression);
        builder.startTimeOffset(this.startTimeOffset);
        builder.endTimeOffset(this.endTimeOffset);
        return builder;
    }

    /**
     * @return returns a new Builder.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for {@link ScheduleConfiguration}.
     */
    public static final class Builder implements ShapeBuilder<ScheduleConfiguration> {
        private final PresenceTracker tracker = PresenceTracker.of($SCHEMA);
        private String scheduleExpression;
        private Long startTimeOffset;
        private Long endTimeOffset;

        private Builder() {
            // Tell the tracker to assume clientOptional members are present.
            tracker.setMember($SCHEMA_SCHEDULE_EXPRESSION);
            tracker.setMember($SCHEMA_START_TIME_OFFSET);
        }

        @Override
        public Schema schema() {
            return $SCHEMA;
        }

        /**
         * The schedule expression that defines how often the underlying CloudWatch Logs scheduled query runs. Specify a
         * <code>rate()</code> expression, for example <code>rate(5 minutes)</code>.
         *
         * <p><strong>Required</strong>
         * @return this builder.
         */
        public Builder scheduleExpression(String scheduleExpression) {
            this.scheduleExpression = Objects.requireNonNull(scheduleExpression, "scheduleExpression cannot be null");
            tracker.setMember($SCHEMA_SCHEDULE_EXPRESSION);
            return this;
        }

        /**
         * The offset, in seconds, before the scheduled execution time at which the query time range begins. For example, an
         * offset of 360 (6 minutes) on a query running at 12:05:00 starts the query time range at 11:59:00.
         *
         * <p><strong>Required</strong>
         * @return this builder.
         */
        public Builder startTimeOffset(Long startTimeOffset) {
            this.startTimeOffset = startTimeOffset;
            tracker.setMember($SCHEMA_START_TIME_OFFSET);
            return this;
        }

        /**
         * The offset, in seconds, before the scheduled execution time at which the query time range ends. Must be
         * non-negative and less than <code>StartTimeOffset</code>. The default is 0.
         *
         * @return this builder.
         */
        public Builder endTimeOffset(Long endTimeOffset) {
            this.endTimeOffset = endTimeOffset;
            return this;
        }

        @Override
        public ScheduleConfiguration build() {
            tracker.validate();
            return new ScheduleConfiguration(this);
        }

        @Override
        @SuppressWarnings("unchecked")
        public void setMemberValue(Schema member, Object value) {
            switch (member.memberIndex()) {
                case 0 -> scheduleExpression((String) SchemaUtils.validateSameMember($SCHEMA_SCHEDULE_EXPRESSION, member, value));
                case 1 -> startTimeOffset((Long) SchemaUtils.validateSameMember($SCHEMA_START_TIME_OFFSET, member, value));
                case 2 -> endTimeOffset((Long) SchemaUtils.validateSameMember($SCHEMA_END_TIME_OFFSET, member, value));
                default -> ShapeBuilder.super.setMemberValue(member, value);
            }
        }

        @Override
        public ShapeBuilder<ScheduleConfiguration> errorCorrection() {
            if (tracker.allSet()) {
                return this;
            }
            if (!tracker.checkMember($SCHEMA_SCHEDULE_EXPRESSION)) {
                scheduleExpression("");
            }
            if (!tracker.checkMember($SCHEMA_START_TIME_OFFSET)) {
                tracker.setMember($SCHEMA_START_TIME_OFFSET);
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
                    case 0 -> builder.scheduleExpression(de.readString(member));
                    case 1 -> builder.startTimeOffset(de.readLong(member));
                    case 2 -> builder.endTimeOffset(de.readLong(member));
                    default -> throw new IllegalArgumentException("Unexpected member: " + member.memberName());
                }
            }
        }
    }
}
