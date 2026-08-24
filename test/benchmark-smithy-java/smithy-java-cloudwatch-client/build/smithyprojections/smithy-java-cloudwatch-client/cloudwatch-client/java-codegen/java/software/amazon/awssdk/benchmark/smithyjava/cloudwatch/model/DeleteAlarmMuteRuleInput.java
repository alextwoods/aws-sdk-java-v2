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
public final class DeleteAlarmMuteRuleInput implements SerializableStruct {

    public static final Schema $SCHEMA = Schemas.DELETE_ALARM_MUTE_RULE_INPUT;
    private static final Schema $SCHEMA_ALARM_MUTE_RULE_NAME = $SCHEMA.member("AlarmMuteRuleName");

    public static final ShapeId $ID = $SCHEMA.id();

    private final transient String alarmMuteRuleName;

    private DeleteAlarmMuteRuleInput(Builder builder) {
        this.alarmMuteRuleName = builder.alarmMuteRuleName;
    }

    /**
     * The name of the alarm mute rule to delete.
     */
    public String getAlarmMuteRuleName() {
        return alarmMuteRuleName;
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
        DeleteAlarmMuteRuleInput that = (DeleteAlarmMuteRuleInput) other;
        return Objects.equals(this.alarmMuteRuleName, that.alarmMuteRuleName);
    }

    @Override
    public int hashCode() {
        int $hc = Objects.hashCode(alarmMuteRuleName);
        return $hc;
    }

    @Override
    public Schema schema() {
        return $SCHEMA;
    }

    @Override
    public void serializeMembers(ShapeSerializer serializer) {
        if (alarmMuteRuleName != null) {
            serializer.writeString($SCHEMA_ALARM_MUTE_RULE_NAME, alarmMuteRuleName);
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T getMemberValue(Schema member) {
        return switch (member.memberIndex()) {
            case 0 -> (T) SchemaUtils.validateSameMember($SCHEMA_ALARM_MUTE_RULE_NAME, member, alarmMuteRuleName);
            default -> throw new IllegalArgumentException("Attempted to get non-existent member: " + member.id());
        };
    }

    /**
     * Create a new builder containing all the current property values of this object.
     *
     * <p><strong>Note:</strong> This method performs only a shallow copy of the original properties.
     *
     * @return a builder for {@link DeleteAlarmMuteRuleInput}.
     */
    public Builder toBuilder() {
        var builder = new Builder();
        builder.alarmMuteRuleName(this.alarmMuteRuleName);
        return builder;
    }

    /**
     * @return returns a new Builder.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for {@link DeleteAlarmMuteRuleInput}.
     */
    public static final class Builder implements ShapeBuilder<DeleteAlarmMuteRuleInput> {
        private final PresenceTracker tracker = PresenceTracker.of($SCHEMA);
        private String alarmMuteRuleName;

        private Builder() {
            // Tell the tracker to assume clientOptional members are present.
            tracker.setMember($SCHEMA_ALARM_MUTE_RULE_NAME);
        }

        @Override
        public Schema schema() {
            return $SCHEMA;
        }

        /**
         * The name of the alarm mute rule to delete.
         *
         * <p><strong>Required</strong>
         * @return this builder.
         */
        public Builder alarmMuteRuleName(String alarmMuteRuleName) {
            this.alarmMuteRuleName = Objects.requireNonNull(alarmMuteRuleName, "alarmMuteRuleName cannot be null");
            tracker.setMember($SCHEMA_ALARM_MUTE_RULE_NAME);
            return this;
        }

        @Override
        public DeleteAlarmMuteRuleInput build() {
            tracker.validate();
            return new DeleteAlarmMuteRuleInput(this);
        }

        @Override
        @SuppressWarnings("unchecked")
        public void setMemberValue(Schema member, Object value) {
            switch (member.memberIndex()) {
                case 0 -> alarmMuteRuleName((String) SchemaUtils.validateSameMember($SCHEMA_ALARM_MUTE_RULE_NAME, member, value));
                default -> ShapeBuilder.super.setMemberValue(member, value);
            }
        }

        @Override
        public ShapeBuilder<DeleteAlarmMuteRuleInput> errorCorrection() {
            if (tracker.allSet()) {
                return this;
            }
            if (!tracker.checkMember($SCHEMA_ALARM_MUTE_RULE_NAME)) {
                alarmMuteRuleName("");
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
                    case 0 -> builder.alarmMuteRuleName(de.readString(member));
                    default -> throw new IllegalArgumentException("Unexpected member: " + member.memberName());
                }
            }
        }
    }
}
