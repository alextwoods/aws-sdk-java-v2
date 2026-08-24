package software.amazon.awssdk.benchmark.smithyjava.cloudwatch.model;

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
 * Summary information about an alarm mute rule, including its name, status, and configuration details.
 */
@SmithyGenerated
public final class AlarmMuteRuleSummary implements SerializableStruct {

    public static final Schema $SCHEMA = Schemas.ALARM_MUTE_RULE_SUMMARY;
    private static final Schema $SCHEMA_ALARM_MUTE_RULE_ARN = $SCHEMA.member("AlarmMuteRuleArn");
    private static final Schema $SCHEMA_EXPIRE_DATE = $SCHEMA.member("ExpireDate");
    private static final Schema $SCHEMA_STATUS = $SCHEMA.member("Status");
    private static final Schema $SCHEMA_MUTE_TYPE = $SCHEMA.member("MuteType");
    private static final Schema $SCHEMA_LAST_UPDATED_TIMESTAMP = $SCHEMA.member("LastUpdatedTimestamp");

    public static final ShapeId $ID = $SCHEMA.id();

    private final transient String alarmMuteRuleArn;
    private final transient Instant expireDate;
    private final transient AlarmMuteRuleStatus status;
    private final transient String muteType;
    private final transient Instant lastUpdatedTimestamp;

    private AlarmMuteRuleSummary(Builder builder) {
        this.alarmMuteRuleArn = builder.alarmMuteRuleArn;
        this.expireDate = builder.expireDate;
        this.status = builder.status;
        this.muteType = builder.muteType;
        this.lastUpdatedTimestamp = builder.lastUpdatedTimestamp;
    }

    /**
     * The Amazon Resource Name (ARN) of the alarm mute rule.
     */
    public String getAlarmMuteRuleArn() {
        return alarmMuteRuleArn;
    }

    /**
     * The date and time when the mute rule expires and is no longer evaluated. This field is only present if an
     * expiration date was configured.
     */
    public Instant getExpireDate() {
        return expireDate;
    }

    /**
     * The current status of the alarm mute rule. Valid values are <code>SCHEDULED</code>, <code>ACTIVE</code>, or
     * <code>EXPIRED</code>.
     */
    public AlarmMuteRuleStatus getStatus() {
        return status;
    }

    /**
     * Indicates whether the mute rule is one-time or recurring. Valid values are <code>ONE_TIME</code> or
     * <code>RECURRING</code>.
     */
    public String getMuteType() {
        return muteType;
    }

    /**
     * The date and time when the mute rule was last updated.
     */
    public Instant getLastUpdatedTimestamp() {
        return lastUpdatedTimestamp;
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
        AlarmMuteRuleSummary that = (AlarmMuteRuleSummary) other;
        return Objects.equals(this.alarmMuteRuleArn, that.alarmMuteRuleArn)
               && Objects.equals(this.muteType, that.muteType)
               && Objects.equals(this.status, that.status)
               && Objects.equals(this.expireDate, that.expireDate)
               && Objects.equals(this.lastUpdatedTimestamp, that.lastUpdatedTimestamp);
    }

    @Override
    public int hashCode() {
        int $hc = Objects.hashCode(alarmMuteRuleArn);
        $hc = 31 * $hc + Objects.hashCode(expireDate);
        $hc = 31 * $hc + Objects.hashCode(status);
        $hc = 31 * $hc + Objects.hashCode(muteType);
        $hc = 31 * $hc + Objects.hashCode(lastUpdatedTimestamp);
        return $hc;
    }

    @Override
    public Schema schema() {
        return $SCHEMA;
    }

    @Override
    public void serializeMembers(ShapeSerializer serializer) {
        if (alarmMuteRuleArn != null) {
            serializer.writeString($SCHEMA_ALARM_MUTE_RULE_ARN, alarmMuteRuleArn);
        }
        if (expireDate != null) {
            serializer.writeTimestamp($SCHEMA_EXPIRE_DATE, expireDate);
        }
        if (status != null) {
            serializer.writeString($SCHEMA_STATUS, status.getValue());
        }
        if (muteType != null) {
            serializer.writeString($SCHEMA_MUTE_TYPE, muteType);
        }
        if (lastUpdatedTimestamp != null) {
            serializer.writeTimestamp($SCHEMA_LAST_UPDATED_TIMESTAMP, lastUpdatedTimestamp);
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T getMemberValue(Schema member) {
        return switch (member.memberIndex()) {
            case 0 -> (T) SchemaUtils.validateSameMember($SCHEMA_ALARM_MUTE_RULE_ARN, member, alarmMuteRuleArn);
            case 1 -> (T) SchemaUtils.validateSameMember($SCHEMA_EXPIRE_DATE, member, expireDate);
            case 2 -> (T) SchemaUtils.validateSameMember($SCHEMA_STATUS, member, status);
            case 3 -> (T) SchemaUtils.validateSameMember($SCHEMA_MUTE_TYPE, member, muteType);
            case 4 -> (T) SchemaUtils.validateSameMember($SCHEMA_LAST_UPDATED_TIMESTAMP, member, lastUpdatedTimestamp);
            default -> throw new IllegalArgumentException("Attempted to get non-existent member: " + member.id());
        };
    }

    /**
     * Create a new builder containing all the current property values of this object.
     *
     * <p><strong>Note:</strong> This method performs only a shallow copy of the original properties.
     *
     * @return a builder for {@link AlarmMuteRuleSummary}.
     */
    public Builder toBuilder() {
        var builder = new Builder();
        builder.alarmMuteRuleArn(this.alarmMuteRuleArn);
        builder.expireDate(this.expireDate);
        builder.status(this.status);
        builder.muteType(this.muteType);
        builder.lastUpdatedTimestamp(this.lastUpdatedTimestamp);
        return builder;
    }

    /**
     * @return returns a new Builder.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for {@link AlarmMuteRuleSummary}.
     */
    public static final class Builder implements ShapeBuilder<AlarmMuteRuleSummary> {
        private String alarmMuteRuleArn;
        private Instant expireDate;
        private AlarmMuteRuleStatus status;
        private String muteType;
        private Instant lastUpdatedTimestamp;

        private Builder() {}

        @Override
        public Schema schema() {
            return $SCHEMA;
        }

        /**
         * The Amazon Resource Name (ARN) of the alarm mute rule.
         *
         * @return this builder.
         */
        public Builder alarmMuteRuleArn(String alarmMuteRuleArn) {
            this.alarmMuteRuleArn = alarmMuteRuleArn;
            return this;
        }

        /**
         * The date and time when the mute rule expires and is no longer evaluated. This field is only present if an
         * expiration date was configured.
         *
         * @return this builder.
         */
        public Builder expireDate(Instant expireDate) {
            this.expireDate = expireDate;
            return this;
        }

        /**
         * The current status of the alarm mute rule. Valid values are <code>SCHEDULED</code>, <code>ACTIVE</code>, or
         * <code>EXPIRED</code>.
         *
         * @return this builder.
         */
        public Builder status(AlarmMuteRuleStatus status) {
            this.status = status;
            return this;
        }

        /**
         * Indicates whether the mute rule is one-time or recurring. Valid values are <code>ONE_TIME</code> or
         * <code>RECURRING</code>.
         *
         * @return this builder.
         */
        public Builder muteType(String muteType) {
            this.muteType = muteType;
            return this;
        }

        /**
         * The date and time when the mute rule was last updated.
         *
         * @return this builder.
         */
        public Builder lastUpdatedTimestamp(Instant lastUpdatedTimestamp) {
            this.lastUpdatedTimestamp = lastUpdatedTimestamp;
            return this;
        }

        @Override
        public AlarmMuteRuleSummary build() {
            return new AlarmMuteRuleSummary(this);
        }

        @Override
        @SuppressWarnings("unchecked")
        public void setMemberValue(Schema member, Object value) {
            switch (member.memberIndex()) {
                case 0 -> alarmMuteRuleArn((String) SchemaUtils.validateSameMember($SCHEMA_ALARM_MUTE_RULE_ARN, member, value));
                case 1 -> expireDate((Instant) SchemaUtils.validateSameMember($SCHEMA_EXPIRE_DATE, member, value));
                case 2 -> status((AlarmMuteRuleStatus) SchemaUtils.validateSameMember($SCHEMA_STATUS, member, value));
                case 3 -> muteType((String) SchemaUtils.validateSameMember($SCHEMA_MUTE_TYPE, member, value));
                case 4 -> lastUpdatedTimestamp((Instant) SchemaUtils.validateSameMember($SCHEMA_LAST_UPDATED_TIMESTAMP, member, value));
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
                    case 0 -> builder.alarmMuteRuleArn(de.readString(member));
                    case 1 -> builder.expireDate(de.readTimestamp(member));
                    case 2 -> builder.status(AlarmMuteRuleStatus.builder().deserializeMember(de, member).build());
                    case 3 -> builder.muteType(de.readString(member));
                    case 4 -> builder.lastUpdatedTimestamp(de.readTimestamp(member));
                    default -> throw new IllegalArgumentException("Unexpected member: " + member.memberName());
                }
            }
        }
    }
}
