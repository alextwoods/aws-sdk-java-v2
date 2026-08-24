package software.amazon.awssdk.benchmark.smithyjava.cloudwatch.model;

import java.util.Collections;
import java.util.List;
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

@SmithyGenerated
public final class DeleteAlarmsInput implements SerializableStruct {

    public static final Schema $SCHEMA = Schemas.DELETE_ALARMS_INPUT;
    private static final Schema $SCHEMA_ALARM_NAMES = $SCHEMA.member("AlarmNames");

    public static final ShapeId $ID = $SCHEMA.id();

    private final transient List<String> alarmNames;

    private DeleteAlarmsInput(Builder builder) {
        this.alarmNames = builder.alarmNames == null ? null : Collections.unmodifiableList(builder.alarmNames);
    }

    /**
     * The alarms to be deleted. Do not enclose the alarm names in quote marks.
     */
    public List<String> getAlarmNames() {
        if (alarmNames == null) {
            return Collections.emptyList();
        }
        return alarmNames;
    }

    public boolean hasAlarmNames() {
        return alarmNames != null;
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
        DeleteAlarmsInput that = (DeleteAlarmsInput) other;
        return Objects.equals(this.alarmNames, that.alarmNames);
    }

    @Override
    public int hashCode() {
        int $hc = Objects.hashCode(alarmNames);
        return $hc;
    }

    @Override
    public Schema schema() {
        return $SCHEMA;
    }

    @Override
    public void serializeMembers(ShapeSerializer serializer) {
        if (alarmNames != null) {
            serializer.writeList($SCHEMA_ALARM_NAMES, alarmNames, alarmNames.size(), SharedSerde.AlarmNamesSerializer.INSTANCE);
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T getMemberValue(Schema member) {
        return switch (member.memberIndex()) {
            case 0 -> (T) SchemaUtils.validateSameMember($SCHEMA_ALARM_NAMES, member, alarmNames);
            default -> throw new IllegalArgumentException("Attempted to get non-existent member: " + member.id());
        };
    }

    /**
     * Create a new builder containing all the current property values of this object.
     *
     * <p><strong>Note:</strong> This method performs only a shallow copy of the original properties.
     *
     * @return a builder for {@link DeleteAlarmsInput}.
     */
    public Builder toBuilder() {
        var builder = new Builder();
        builder.alarmNames(this.alarmNames);
        return builder;
    }

    /**
     * @return returns a new Builder.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for {@link DeleteAlarmsInput}.
     */
    public static final class Builder implements ShapeBuilder<DeleteAlarmsInput> {
        private final PresenceTracker tracker = PresenceTracker.of($SCHEMA);
        private List<String> alarmNames;

        private Builder() {
            // Tell the tracker to assume clientOptional members are present.
            tracker.setMember($SCHEMA_ALARM_NAMES);
        }

        @Override
        public Schema schema() {
            return $SCHEMA;
        }

        /**
         * The alarms to be deleted. Do not enclose the alarm names in quote marks.
         *
         * <p><strong>Required</strong>
         * @return this builder.
         */
        public Builder alarmNames(List<String> alarmNames) {
            this.alarmNames = Objects.requireNonNull(alarmNames, "alarmNames cannot be null");
            tracker.setMember($SCHEMA_ALARM_NAMES);
            return this;
        }

        @Override
        public DeleteAlarmsInput build() {
            tracker.validate();
            return new DeleteAlarmsInput(this);
        }

        @Override
        @SuppressWarnings("unchecked")
        public void setMemberValue(Schema member, Object value) {
            switch (member.memberIndex()) {
                case 0 -> alarmNames((List<String>) SchemaUtils.validateSameMember($SCHEMA_ALARM_NAMES, member, value));
                default -> ShapeBuilder.super.setMemberValue(member, value);
            }
        }

        @Override
        public ShapeBuilder<DeleteAlarmsInput> errorCorrection() {
            if (tracker.allSet()) {
                return this;
            }
            if (!tracker.checkMember($SCHEMA_ALARM_NAMES)) {
                alarmNames(Collections.emptyList());
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
                    case 0 -> builder.alarmNames(SharedSerde.deserializeAlarmNames(member, de));
                    default -> throw new IllegalArgumentException("Unexpected member: " + member.memberName());
                }
            }
        }
    }
}
