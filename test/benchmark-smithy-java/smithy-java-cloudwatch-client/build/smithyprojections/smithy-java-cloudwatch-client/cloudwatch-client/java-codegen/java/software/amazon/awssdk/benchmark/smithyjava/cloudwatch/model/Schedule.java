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
 * Specifies when and how long an alarm mute rule is active.
 *
 * <p>The schedule uses either a cron expression for recurring mute windows or an at expression for one-time mute
 * windows. When the schedule activates, the mute rule mutes alarm actions for the specified duration.
 */
@SmithyGenerated
public final class Schedule implements SerializableStruct {

    public static final Schema $SCHEMA = Schemas.SCHEDULE;
    private static final Schema $SCHEMA_EXPRESSION = $SCHEMA.member("Expression");
    private static final Schema $SCHEMA_DURATION = $SCHEMA.member("Duration");
    private static final Schema $SCHEMA_TIMEZONE = $SCHEMA.member("Timezone");

    public static final ShapeId $ID = $SCHEMA.id();

    private final transient String expression;
    private final transient String duration;
    private final transient String timezone;

    private Schedule(Builder builder) {
        this.expression = builder.expression;
        this.duration = builder.duration;
        this.timezone = builder.timezone;
    }

    /**
     * The schedule expression that defines when the mute rule activates. The expression must be between 1 and 256
     * characters in length.
     *
     * <p>You can use one of two expression formats:
     *
     * <ul>
     *   <li>
     *     <b>Cron expressions</b> - For recurring mute windows. Format: <code>cron(Minutes Hours Day-of-month Month
     *     Day-of-week)</code>Examples:
     *
     *     <ul>
     *       <li>
     *         <code>cron(0 2 &#42; &#42; &#42;)</code> - Activates daily at 2:00 AM
     *       </li>
     *       <li>
     *         <code>cron(0 2 &#42; &#42; SUN)</code> - Activates every Sunday at 2:00 AM for weekly system maintenance
     *       </li>
     *       <li>
     *         <code>cron(0 1 1 &#42; &#42;)</code> - Activates on the first day of each month at 1:00 AM for monthly
     *         database maintenance
     *       </li>
     *       <li>
     *         <code>cron(0 18 &#42; &#42; FRI)</code> - Activates every Friday at 6:00 PM
     *       </li>
     *       <li>
     *         <code>cron(0 23 &#42; &#42; &#42;)</code> - Activates every day at 11:00 PM during nightly backup operations
     *       </li>
     *     </ul>The characters <code>&#42;</code>, <code>-</code>, and <code>,</code> are supported in all fields.
     *     English names can be used for the month (JAN-DEC) and day of week (SUN-SAT) fields.
     *   </li>
     *   <li>
     *     <b>At expressions</b> - For one-time mute windows. Format: <code>at(yyyy-MM-ddThh:mm)</code>Examples:
     *
     *     <ul>
     *       <li>
     *         <code>at(2024-05-10T14:00)</code> - Activates once on May 10, 2024 at 2:00 PM during an active
     *         incident response session
     *       </li>
     *       <li>
     *         <code>at(2024-12-23T00:00)</code> - Activates once on December 23, 2024 at midnight during annual
     *         company shutdown
     *       </li>
     *     </ul>
     *   </li>
     * </ul>
     */
    public String getExpression() {
        return expression;
    }

    /**
     * The length of time that alarms remain muted when the schedule activates. The duration must be between 1 and 50
     * characters in length.
     *
     * <p>Specify the duration using ISO 8601 duration format with a minimum of 1 minute (<code>PT1M</code>) and maximum
     * of 15 days (<code>P15D</code>).
     *
     * <p>Examples:
     *
     * <ul>
     *   <li>
     *     <code>PT4H</code> - 4 hours for weekly system maintenance
     *   </li>
     *   <li>
     *     <code>P2DT12H</code> - 2 days and 12 hours for weekend muting from Friday 6:00 PM to Monday 6:00 AM
     *   </li>
     *   <li>
     *     <code>PT6H</code> - 6 hours for monthly database maintenance
     *   </li>
     *   <li>
     *     <code>PT2H</code> - 2 hours for nightly backup operations
     *   </li>
     *   <li>
     *     <code>P7D</code> - 7 days for annual company shutdown
     *   </li>
     * </ul>
     *
     * <p>The duration begins when the schedule expression time is reached. For recurring schedules, the duration
     * applies to each occurrence.
     */
    public String getDuration() {
        return duration;
    }

    /**
     * The time zone to use when evaluating the schedule expression. The time zone must be between 1 and 50 characters
     * in length.
     *
     * <p>Specify the time zone using standard timezone identifiers (for example, <code>America/New_York</code>, <code>
     * Europe/London</code>, or <code>Asia/Tokyo</code>).
     *
     * <p>If you don't specify a time zone, UTC is used by default. The time zone affects how cron and at expressions
     * are interpreted, as well as start and expire dates you specify
     *
     * <p>Examples:
     *
     * <ul>
     *   <li>
     *     <code>America/New_York</code> - Eastern Time (US)
     *   </li>
     *   <li>
     *     <code>America/Los_Angeles</code> - Pacific Time (US)
     *   </li>
     *   <li>
     *     <code>Europe/London</code> - British Time
     *   </li>
     *   <li>
     *     <code>Asia/Tokyo</code> - Japan Standard Time
     *   </li>
     *   <li>
     *     <code>UTC</code> - Coordinated Universal Time
     *   </li>
     * </ul>
     */
    public String getTimezone() {
        return timezone;
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
        Schedule that = (Schedule) other;
        return Objects.equals(this.expression, that.expression)
               && Objects.equals(this.duration, that.duration)
               && Objects.equals(this.timezone, that.timezone);
    }

    @Override
    public int hashCode() {
        int $hc = Objects.hashCode(expression);
        $hc = 31 * $hc + Objects.hashCode(duration);
        $hc = 31 * $hc + Objects.hashCode(timezone);
        return $hc;
    }

    @Override
    public Schema schema() {
        return $SCHEMA;
    }

    @Override
    public void serializeMembers(ShapeSerializer serializer) {
        if (expression != null) {
            serializer.writeString($SCHEMA_EXPRESSION, expression);
        }
        if (duration != null) {
            serializer.writeString($SCHEMA_DURATION, duration);
        }
        if (timezone != null) {
            serializer.writeString($SCHEMA_TIMEZONE, timezone);
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T getMemberValue(Schema member) {
        return switch (member.memberIndex()) {
            case 0 -> (T) SchemaUtils.validateSameMember($SCHEMA_EXPRESSION, member, expression);
            case 1 -> (T) SchemaUtils.validateSameMember($SCHEMA_DURATION, member, duration);
            case 2 -> (T) SchemaUtils.validateSameMember($SCHEMA_TIMEZONE, member, timezone);
            default -> throw new IllegalArgumentException("Attempted to get non-existent member: " + member.id());
        };
    }

    /**
     * Create a new builder containing all the current property values of this object.
     *
     * <p><strong>Note:</strong> This method performs only a shallow copy of the original properties.
     *
     * @return a builder for {@link Schedule}.
     */
    public Builder toBuilder() {
        var builder = new Builder();
        builder.expression(this.expression);
        builder.duration(this.duration);
        builder.timezone(this.timezone);
        return builder;
    }

    /**
     * @return returns a new Builder.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for {@link Schedule}.
     */
    public static final class Builder implements ShapeBuilder<Schedule> {
        private final PresenceTracker tracker = PresenceTracker.of($SCHEMA);
        private String expression;
        private String duration;
        private String timezone;

        private Builder() {
            // Tell the tracker to assume clientOptional members are present.
            tracker.setMember($SCHEMA_EXPRESSION);
            tracker.setMember($SCHEMA_DURATION);
        }

        @Override
        public Schema schema() {
            return $SCHEMA;
        }

        /**
         * The schedule expression that defines when the mute rule activates. The expression must be between 1 and 256
         * characters in length.
         *
         * <p>You can use one of two expression formats:
         *
         * <ul>
         *   <li>
         *     <b>Cron expressions</b> - For recurring mute windows. Format: <code>cron(Minutes Hours Day-of-month Month
         *     Day-of-week)</code>Examples:
         *
         *     <ul>
         *       <li>
         *         <code>cron(0 2 &#42; &#42; &#42;)</code> - Activates daily at 2:00 AM
         *       </li>
         *       <li>
         *         <code>cron(0 2 &#42; &#42; SUN)</code> - Activates every Sunday at 2:00 AM for weekly system maintenance
         *       </li>
         *       <li>
         *         <code>cron(0 1 1 &#42; &#42;)</code> - Activates on the first day of each month at 1:00 AM for monthly
         *         database maintenance
         *       </li>
         *       <li>
         *         <code>cron(0 18 &#42; &#42; FRI)</code> - Activates every Friday at 6:00 PM
         *       </li>
         *       <li>
         *         <code>cron(0 23 &#42; &#42; &#42;)</code> - Activates every day at 11:00 PM during nightly backup operations
         *       </li>
         *     </ul>The characters <code>&#42;</code>, <code>-</code>, and <code>,</code> are supported in all fields.
         *     English names can be used for the month (JAN-DEC) and day of week (SUN-SAT) fields.
         *   </li>
         *   <li>
         *     <b>At expressions</b> - For one-time mute windows. Format: <code>at(yyyy-MM-ddThh:mm)</code>Examples:
         *
         *     <ul>
         *       <li>
         *         <code>at(2024-05-10T14:00)</code> - Activates once on May 10, 2024 at 2:00 PM during an active
         *         incident response session
         *       </li>
         *       <li>
         *         <code>at(2024-12-23T00:00)</code> - Activates once on December 23, 2024 at midnight during annual
         *         company shutdown
         *       </li>
         *     </ul>
         *   </li>
         * </ul>
         *
         * <p><strong>Required</strong>
         * @return this builder.
         */
        public Builder expression(String expression) {
            this.expression = Objects.requireNonNull(expression, "expression cannot be null");
            tracker.setMember($SCHEMA_EXPRESSION);
            return this;
        }

        /**
         * The length of time that alarms remain muted when the schedule activates. The duration must be between 1 and 50
         * characters in length.
         *
         * <p>Specify the duration using ISO 8601 duration format with a minimum of 1 minute (<code>PT1M</code>) and maximum
         * of 15 days (<code>P15D</code>).
         *
         * <p>Examples:
         *
         * <ul>
         *   <li>
         *     <code>PT4H</code> - 4 hours for weekly system maintenance
         *   </li>
         *   <li>
         *     <code>P2DT12H</code> - 2 days and 12 hours for weekend muting from Friday 6:00 PM to Monday 6:00 AM
         *   </li>
         *   <li>
         *     <code>PT6H</code> - 6 hours for monthly database maintenance
         *   </li>
         *   <li>
         *     <code>PT2H</code> - 2 hours for nightly backup operations
         *   </li>
         *   <li>
         *     <code>P7D</code> - 7 days for annual company shutdown
         *   </li>
         * </ul>
         *
         * <p>The duration begins when the schedule expression time is reached. For recurring schedules, the duration
         * applies to each occurrence.
         *
         * <p><strong>Required</strong>
         * @return this builder.
         */
        public Builder duration(String duration) {
            this.duration = Objects.requireNonNull(duration, "duration cannot be null");
            tracker.setMember($SCHEMA_DURATION);
            return this;
        }

        /**
         * The time zone to use when evaluating the schedule expression. The time zone must be between 1 and 50 characters
         * in length.
         *
         * <p>Specify the time zone using standard timezone identifiers (for example, <code>America/New_York</code>, <code>
         * Europe/London</code>, or <code>Asia/Tokyo</code>).
         *
         * <p>If you don't specify a time zone, UTC is used by default. The time zone affects how cron and at expressions
         * are interpreted, as well as start and expire dates you specify
         *
         * <p>Examples:
         *
         * <ul>
         *   <li>
         *     <code>America/New_York</code> - Eastern Time (US)
         *   </li>
         *   <li>
         *     <code>America/Los_Angeles</code> - Pacific Time (US)
         *   </li>
         *   <li>
         *     <code>Europe/London</code> - British Time
         *   </li>
         *   <li>
         *     <code>Asia/Tokyo</code> - Japan Standard Time
         *   </li>
         *   <li>
         *     <code>UTC</code> - Coordinated Universal Time
         *   </li>
         * </ul>
         *
         * @return this builder.
         */
        public Builder timezone(String timezone) {
            this.timezone = timezone;
            return this;
        }

        @Override
        public Schedule build() {
            tracker.validate();
            return new Schedule(this);
        }

        @Override
        @SuppressWarnings("unchecked")
        public void setMemberValue(Schema member, Object value) {
            switch (member.memberIndex()) {
                case 0 -> expression((String) SchemaUtils.validateSameMember($SCHEMA_EXPRESSION, member, value));
                case 1 -> duration((String) SchemaUtils.validateSameMember($SCHEMA_DURATION, member, value));
                case 2 -> timezone((String) SchemaUtils.validateSameMember($SCHEMA_TIMEZONE, member, value));
                default -> ShapeBuilder.super.setMemberValue(member, value);
            }
        }

        @Override
        public ShapeBuilder<Schedule> errorCorrection() {
            if (tracker.allSet()) {
                return this;
            }
            if (!tracker.checkMember($SCHEMA_EXPRESSION)) {
                expression("");
            }
            if (!tracker.checkMember($SCHEMA_DURATION)) {
                duration("");
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
                    case 0 -> builder.expression(de.readString(member));
                    case 1 -> builder.duration(de.readString(member));
                    case 2 -> builder.timezone(de.readString(member));
                    default -> throw new IllegalArgumentException("Unexpected member: " + member.memberName());
                }
            }
        }
    }
}
