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

@SmithyGenerated
public final class SetAlarmStateInput implements SerializableStruct {

    public static final Schema $SCHEMA = Schemas.SET_ALARM_STATE_INPUT;
    private static final Schema $SCHEMA_ALARM_NAME = $SCHEMA.member("AlarmName");
    private static final Schema $SCHEMA_STATE_VALUE = $SCHEMA.member("StateValue");
    private static final Schema $SCHEMA_STATE_REASON = $SCHEMA.member("StateReason");
    private static final Schema $SCHEMA_STATE_REASON_DATA = $SCHEMA.member("StateReasonData");

    public static final ShapeId $ID = $SCHEMA.id();

    private final transient String alarmName;
    private final transient StateValue stateValue;
    private final transient String stateReason;
    private final transient String stateReasonData;

    private SetAlarmStateInput(Builder builder) {
        this.alarmName = builder.alarmName;
        this.stateValue = builder.stateValue;
        this.stateReason = builder.stateReason;
        this.stateReasonData = builder.stateReasonData;
    }

    /**
     * The name of the alarm.
     */
    public String getAlarmName() {
        return alarmName;
    }

    /**
     * The value of the state.
     */
    public StateValue getStateValue() {
        return stateValue;
    }

    /**
     * The reason that this alarm is set to this specific state, in text format.
     */
    public String getStateReason() {
        return stateReason;
    }

    /**
     * The reason that this alarm is set to this specific state, in JSON format.
     *
     * <p>For SNS or EC2 alarm actions, this is just informational. But for EC2 Auto Scaling or application Auto Scaling
     * alarm actions, the Auto Scaling policy uses the information in this field to take the correct action.
     */
    public String getStateReasonData() {
        return stateReasonData;
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
        SetAlarmStateInput that = (SetAlarmStateInput) other;
        return Objects.equals(this.alarmName, that.alarmName)
               && Objects.equals(this.stateReason, that.stateReason)
               && Objects.equals(this.stateReasonData, that.stateReasonData)
               && Objects.equals(this.stateValue, that.stateValue);
    }

    @Override
    public int hashCode() {
        int $hc = Objects.hashCode(alarmName);
        $hc = 31 * $hc + Objects.hashCode(stateValue);
        $hc = 31 * $hc + Objects.hashCode(stateReason);
        $hc = 31 * $hc + Objects.hashCode(stateReasonData);
        return $hc;
    }

    @Override
    public Schema schema() {
        return $SCHEMA;
    }

    @Override
    public void serializeMembers(ShapeSerializer serializer) {
        if (alarmName != null) {
            serializer.writeString($SCHEMA_ALARM_NAME, alarmName);
        }
        if (stateValue != null) {
            serializer.writeString($SCHEMA_STATE_VALUE, stateValue.getValue());
        }
        if (stateReason != null) {
            serializer.writeString($SCHEMA_STATE_REASON, stateReason);
        }
        if (stateReasonData != null) {
            serializer.writeString($SCHEMA_STATE_REASON_DATA, stateReasonData);
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T getMemberValue(Schema member) {
        return switch (member.memberIndex()) {
            case 0 -> (T) SchemaUtils.validateSameMember($SCHEMA_ALARM_NAME, member, alarmName);
            case 1 -> (T) SchemaUtils.validateSameMember($SCHEMA_STATE_VALUE, member, stateValue);
            case 2 -> (T) SchemaUtils.validateSameMember($SCHEMA_STATE_REASON, member, stateReason);
            case 3 -> (T) SchemaUtils.validateSameMember($SCHEMA_STATE_REASON_DATA, member, stateReasonData);
            default -> throw new IllegalArgumentException("Attempted to get non-existent member: " + member.id());
        };
    }

    /**
     * Create a new builder containing all the current property values of this object.
     *
     * <p><strong>Note:</strong> This method performs only a shallow copy of the original properties.
     *
     * @return a builder for {@link SetAlarmStateInput}.
     */
    public Builder toBuilder() {
        var builder = new Builder();
        builder.alarmName(this.alarmName);
        builder.stateValue(this.stateValue);
        builder.stateReason(this.stateReason);
        builder.stateReasonData(this.stateReasonData);
        return builder;
    }

    /**
     * @return returns a new Builder.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for {@link SetAlarmStateInput}.
     */
    public static final class Builder implements ShapeBuilder<SetAlarmStateInput> {
        private final PresenceTracker tracker = PresenceTracker.of($SCHEMA);
        private String alarmName;
        private StateValue stateValue;
        private String stateReason;
        private String stateReasonData;

        private Builder() {
            // Tell the tracker to assume clientOptional members are present.
            tracker.setMember($SCHEMA_ALARM_NAME);
            tracker.setMember($SCHEMA_STATE_VALUE);
            tracker.setMember($SCHEMA_STATE_REASON);
        }

        @Override
        public Schema schema() {
            return $SCHEMA;
        }

        /**
         * The name of the alarm.
         *
         * <p><strong>Required</strong>
         * @return this builder.
         */
        public Builder alarmName(String alarmName) {
            this.alarmName = Objects.requireNonNull(alarmName, "alarmName cannot be null");
            tracker.setMember($SCHEMA_ALARM_NAME);
            return this;
        }

        /**
         * The value of the state.
         *
         * <p><strong>Required</strong>
         * @return this builder.
         */
        public Builder stateValue(StateValue stateValue) {
            this.stateValue = Objects.requireNonNull(stateValue, "stateValue cannot be null");
            tracker.setMember($SCHEMA_STATE_VALUE);
            return this;
        }

        /**
         * The reason that this alarm is set to this specific state, in text format.
         *
         * <p><strong>Required</strong>
         * @return this builder.
         */
        public Builder stateReason(String stateReason) {
            this.stateReason = Objects.requireNonNull(stateReason, "stateReason cannot be null");
            tracker.setMember($SCHEMA_STATE_REASON);
            return this;
        }

        /**
         * The reason that this alarm is set to this specific state, in JSON format.
         *
         * <p>For SNS or EC2 alarm actions, this is just informational. But for EC2 Auto Scaling or application Auto Scaling
         * alarm actions, the Auto Scaling policy uses the information in this field to take the correct action.
         *
         * @return this builder.
         */
        public Builder stateReasonData(String stateReasonData) {
            this.stateReasonData = stateReasonData;
            return this;
        }

        @Override
        public SetAlarmStateInput build() {
            tracker.validate();
            return new SetAlarmStateInput(this);
        }

        @Override
        @SuppressWarnings("unchecked")
        public void setMemberValue(Schema member, Object value) {
            switch (member.memberIndex()) {
                case 0 -> alarmName((String) SchemaUtils.validateSameMember($SCHEMA_ALARM_NAME, member, value));
                case 1 -> stateValue((StateValue) SchemaUtils.validateSameMember($SCHEMA_STATE_VALUE, member, value));
                case 2 -> stateReason((String) SchemaUtils.validateSameMember($SCHEMA_STATE_REASON, member, value));
                case 3 -> stateReasonData((String) SchemaUtils.validateSameMember($SCHEMA_STATE_REASON_DATA, member, value));
                default -> ShapeBuilder.super.setMemberValue(member, value);
            }
        }

        @Override
        public ShapeBuilder<SetAlarmStateInput> errorCorrection() {
            if (tracker.allSet()) {
                return this;
            }
            if (!tracker.checkMember($SCHEMA_ALARM_NAME)) {
                alarmName("");
            }
            if (!tracker.checkMember($SCHEMA_STATE_VALUE)) {
                stateValue(StateValue.unknown(""));
            }
            if (!tracker.checkMember($SCHEMA_STATE_REASON)) {
                stateReason("");
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
                    case 0 -> builder.alarmName(de.readString(member));
                    case 1 -> builder.stateValue(StateValue.builder().deserializeMember(de, member).build());
                    case 2 -> builder.stateReason(de.readString(member));
                    case 3 -> builder.stateReasonData(de.readString(member));
                    default -> throw new IllegalArgumentException("Unexpected member: " + member.memberName());
                }
            }
        }
    }
}
