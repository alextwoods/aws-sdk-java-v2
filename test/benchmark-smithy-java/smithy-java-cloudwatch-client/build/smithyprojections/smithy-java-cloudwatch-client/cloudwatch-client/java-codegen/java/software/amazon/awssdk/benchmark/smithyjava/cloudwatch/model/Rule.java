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
 * Defines the schedule configuration for an alarm mute rule.
 *
 * <p>The rule contains a schedule that specifies when and how long alarms should be muted. The schedule can be a
 * recurring pattern using cron expressions or a one-time mute window using at expressions.
 */
@SmithyGenerated
public final class Rule implements SerializableStruct {

    public static final Schema $SCHEMA = Schemas.RULE;
    private static final Schema $SCHEMA_SCHEDULE = $SCHEMA.member("Schedule");

    public static final ShapeId $ID = $SCHEMA.id();

    private final transient Schedule schedule;

    private Rule(Builder builder) {
        this.schedule = builder.schedule;
    }

    /**
     * The schedule configuration that defines when the mute rule activates and how long it remains active.
     */
    public Schedule getSchedule() {
        return schedule;
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
        Rule that = (Rule) other;
        return Objects.equals(this.schedule, that.schedule);
    }

    @Override
    public int hashCode() {
        int $hc = Objects.hashCode(schedule);
        return $hc;
    }

    @Override
    public Schema schema() {
        return $SCHEMA;
    }

    @Override
    public void serializeMembers(ShapeSerializer serializer) {
        if (schedule != null) {
            serializer.writeStruct($SCHEMA_SCHEDULE, schedule);
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T getMemberValue(Schema member) {
        return switch (member.memberIndex()) {
            case 0 -> (T) SchemaUtils.validateSameMember($SCHEMA_SCHEDULE, member, schedule);
            default -> throw new IllegalArgumentException("Attempted to get non-existent member: " + member.id());
        };
    }

    /**
     * Create a new builder containing all the current property values of this object.
     *
     * <p><strong>Note:</strong> This method performs only a shallow copy of the original properties.
     *
     * @return a builder for {@link Rule}.
     */
    public Builder toBuilder() {
        var builder = new Builder();
        builder.schedule(this.schedule);
        return builder;
    }

    /**
     * @return returns a new Builder.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for {@link Rule}.
     */
    public static final class Builder implements ShapeBuilder<Rule> {
        private final PresenceTracker tracker = PresenceTracker.of($SCHEMA);
        private Schedule schedule;

        private Builder() {
            // Tell the tracker to assume clientOptional members are present.
            tracker.setMember($SCHEMA_SCHEDULE);
        }

        @Override
        public Schema schema() {
            return $SCHEMA;
        }

        /**
         * The schedule configuration that defines when the mute rule activates and how long it remains active.
         *
         * <p><strong>Required</strong>
         * @return this builder.
         */
        public Builder schedule(Schedule schedule) {
            this.schedule = Objects.requireNonNull(schedule, "schedule cannot be null");
            tracker.setMember($SCHEMA_SCHEDULE);
            return this;
        }

        @Override
        public Rule build() {
            tracker.validate();
            return new Rule(this);
        }

        @Override
        @SuppressWarnings("unchecked")
        public void setMemberValue(Schema member, Object value) {
            switch (member.memberIndex()) {
                case 0 -> schedule((Schedule) SchemaUtils.validateSameMember($SCHEMA_SCHEDULE, member, value));
                default -> ShapeBuilder.super.setMemberValue(member, value);
            }
        }

        @Override
        public ShapeBuilder<Rule> errorCorrection() {
            if (tracker.allSet()) {
                return this;
            }
            if (!tracker.checkMember($SCHEMA_SCHEDULE)) {
                tracker.setMember($SCHEMA_SCHEDULE);
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
                    case 0 -> builder.schedule(Schedule.builder().deserializeMember(de, member).build());
                    default -> throw new IllegalArgumentException("Unexpected member: " + member.memberName());
                }
            }
        }
    }
}
