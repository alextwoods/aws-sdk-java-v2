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

@SmithyGenerated
public final class GetAlarmMuteRuleOutput implements SerializableStruct {

    public static final Schema $SCHEMA = Schemas.GET_ALARM_MUTE_RULE_OUTPUT;
    private static final Schema $SCHEMA_NAME = $SCHEMA.member("Name");
    private static final Schema $SCHEMA_ALARM_MUTE_RULE_ARN = $SCHEMA.member("AlarmMuteRuleArn");
    private static final Schema $SCHEMA_DESCRIPTION = $SCHEMA.member("Description");
    private static final Schema $SCHEMA_RULE = $SCHEMA.member("Rule");
    private static final Schema $SCHEMA_MUTE_TARGETS = $SCHEMA.member("MuteTargets");
    private static final Schema $SCHEMA_START_DATE = $SCHEMA.member("StartDate");
    private static final Schema $SCHEMA_EXPIRE_DATE = $SCHEMA.member("ExpireDate");
    private static final Schema $SCHEMA_STATUS = $SCHEMA.member("Status");
    private static final Schema $SCHEMA_LAST_UPDATED_TIMESTAMP = $SCHEMA.member("LastUpdatedTimestamp");
    private static final Schema $SCHEMA_MUTE_TYPE = $SCHEMA.member("MuteType");

    public static final ShapeId $ID = $SCHEMA.id();

    private final transient String name;
    private final transient String alarmMuteRuleArn;
    private final transient String description;
    private final transient Rule rule;
    private final transient MuteTargets muteTargets;
    private final transient Instant startDate;
    private final transient Instant expireDate;
    private final transient AlarmMuteRuleStatus status;
    private final transient Instant lastUpdatedTimestamp;
    private final transient String muteType;

    private GetAlarmMuteRuleOutput(Builder builder) {
        this.name = builder.name;
        this.alarmMuteRuleArn = builder.alarmMuteRuleArn;
        this.description = builder.description;
        this.rule = builder.rule;
        this.muteTargets = builder.muteTargets;
        this.startDate = builder.startDate;
        this.expireDate = builder.expireDate;
        this.status = builder.status;
        this.lastUpdatedTimestamp = builder.lastUpdatedTimestamp;
        this.muteType = builder.muteType;
    }

    /**
     * The name of the alarm mute rule.
     */
    public String getName() {
        return name;
    }

    /**
     * The Amazon Resource Name (ARN) of the alarm mute rule.
     */
    public String getAlarmMuteRuleArn() {
        return alarmMuteRuleArn;
    }

    /**
     * The description of the alarm mute rule.
     */
    public String getDescription() {
        return description;
    }

    /**
     * The configuration that defines when and how long alarms are muted.
     */
    public Rule getRule() {
        return rule;
    }

    /**
     * Specifies which alarms this rule applies to.
     */
    public MuteTargets getMuteTargets() {
        return muteTargets;
    }

    /**
     * The date and time when the mute rule becomes active. If not set, the rule is active immediately.
     */
    public Instant getStartDate() {
        return startDate;
    }

    /**
     * The date and time when the mute rule expires and is no longer evaluated.
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
     * The date and time when the mute rule was last updated.
     */
    public Instant getLastUpdatedTimestamp() {
        return lastUpdatedTimestamp;
    }

    /**
     * Indicates whether the mute rule is one-time or recurring. Valid values are <code>ONE_TIME</code> or
     * <code>RECURRING</code>.
     */
    public String getMuteType() {
        return muteType;
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
        GetAlarmMuteRuleOutput that = (GetAlarmMuteRuleOutput) other;
        return Objects.equals(this.name, that.name)
               && Objects.equals(this.alarmMuteRuleArn, that.alarmMuteRuleArn)
               && Objects.equals(this.description, that.description)
               && Objects.equals(this.muteType, that.muteType)
               && Objects.equals(this.status, that.status)
               && Objects.equals(this.startDate, that.startDate)
               && Objects.equals(this.expireDate, that.expireDate)
               && Objects.equals(this.lastUpdatedTimestamp, that.lastUpdatedTimestamp)
               && Objects.equals(this.rule, that.rule)
               && Objects.equals(this.muteTargets, that.muteTargets);
    }

    @Override
    public int hashCode() {
        int $hc = Objects.hashCode(name);
        $hc = 31 * $hc + Objects.hashCode(alarmMuteRuleArn);
        $hc = 31 * $hc + Objects.hashCode(description);
        $hc = 31 * $hc + Objects.hashCode(rule);
        $hc = 31 * $hc + Objects.hashCode(muteTargets);
        $hc = 31 * $hc + Objects.hashCode(startDate);
        $hc = 31 * $hc + Objects.hashCode(expireDate);
        $hc = 31 * $hc + Objects.hashCode(status);
        $hc = 31 * $hc + Objects.hashCode(lastUpdatedTimestamp);
        $hc = 31 * $hc + Objects.hashCode(muteType);
        return $hc;
    }

    @Override
    public Schema schema() {
        return $SCHEMA;
    }

    @Override
    public void serializeMembers(ShapeSerializer serializer) {
        if (name != null) {
            serializer.writeString($SCHEMA_NAME, name);
        }
        if (alarmMuteRuleArn != null) {
            serializer.writeString($SCHEMA_ALARM_MUTE_RULE_ARN, alarmMuteRuleArn);
        }
        if (description != null) {
            serializer.writeString($SCHEMA_DESCRIPTION, description);
        }
        if (rule != null) {
            serializer.writeStruct($SCHEMA_RULE, rule);
        }
        if (muteTargets != null) {
            serializer.writeStruct($SCHEMA_MUTE_TARGETS, muteTargets);
        }
        if (startDate != null) {
            serializer.writeTimestamp($SCHEMA_START_DATE, startDate);
        }
        if (expireDate != null) {
            serializer.writeTimestamp($SCHEMA_EXPIRE_DATE, expireDate);
        }
        if (status != null) {
            serializer.writeString($SCHEMA_STATUS, status.getValue());
        }
        if (lastUpdatedTimestamp != null) {
            serializer.writeTimestamp($SCHEMA_LAST_UPDATED_TIMESTAMP, lastUpdatedTimestamp);
        }
        if (muteType != null) {
            serializer.writeString($SCHEMA_MUTE_TYPE, muteType);
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T getMemberValue(Schema member) {
        return switch (member.memberIndex()) {
            case 0 -> (T) SchemaUtils.validateSameMember($SCHEMA_NAME, member, name);
            case 1 -> (T) SchemaUtils.validateSameMember($SCHEMA_ALARM_MUTE_RULE_ARN, member, alarmMuteRuleArn);
            case 2 -> (T) SchemaUtils.validateSameMember($SCHEMA_DESCRIPTION, member, description);
            case 3 -> (T) SchemaUtils.validateSameMember($SCHEMA_RULE, member, rule);
            case 4 -> (T) SchemaUtils.validateSameMember($SCHEMA_MUTE_TARGETS, member, muteTargets);
            case 5 -> (T) SchemaUtils.validateSameMember($SCHEMA_START_DATE, member, startDate);
            case 6 -> (T) SchemaUtils.validateSameMember($SCHEMA_EXPIRE_DATE, member, expireDate);
            case 7 -> (T) SchemaUtils.validateSameMember($SCHEMA_STATUS, member, status);
            case 8 -> (T) SchemaUtils.validateSameMember($SCHEMA_LAST_UPDATED_TIMESTAMP, member, lastUpdatedTimestamp);
            case 9 -> (T) SchemaUtils.validateSameMember($SCHEMA_MUTE_TYPE, member, muteType);
            default -> throw new IllegalArgumentException("Attempted to get non-existent member: " + member.id());
        };
    }

    /**
     * Create a new builder containing all the current property values of this object.
     *
     * <p><strong>Note:</strong> This method performs only a shallow copy of the original properties.
     *
     * @return a builder for {@link GetAlarmMuteRuleOutput}.
     */
    public Builder toBuilder() {
        var builder = new Builder();
        builder.name(this.name);
        builder.alarmMuteRuleArn(this.alarmMuteRuleArn);
        builder.description(this.description);
        builder.rule(this.rule);
        builder.muteTargets(this.muteTargets);
        builder.startDate(this.startDate);
        builder.expireDate(this.expireDate);
        builder.status(this.status);
        builder.lastUpdatedTimestamp(this.lastUpdatedTimestamp);
        builder.muteType(this.muteType);
        return builder;
    }

    /**
     * @return returns a new Builder.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for {@link GetAlarmMuteRuleOutput}.
     */
    public static final class Builder implements ShapeBuilder<GetAlarmMuteRuleOutput> {
        private String name;
        private String alarmMuteRuleArn;
        private String description;
        private Rule rule;
        private MuteTargets muteTargets;
        private Instant startDate;
        private Instant expireDate;
        private AlarmMuteRuleStatus status;
        private Instant lastUpdatedTimestamp;
        private String muteType;

        private Builder() {}

        @Override
        public Schema schema() {
            return $SCHEMA;
        }

        /**
         * The name of the alarm mute rule.
         *
         * @return this builder.
         */
        public Builder name(String name) {
            this.name = name;
            return this;
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
         * The description of the alarm mute rule.
         *
         * @return this builder.
         */
        public Builder description(String description) {
            this.description = description;
            return this;
        }

        /**
         * The configuration that defines when and how long alarms are muted.
         *
         * @return this builder.
         */
        public Builder rule(Rule rule) {
            this.rule = rule;
            return this;
        }

        /**
         * Specifies which alarms this rule applies to.
         *
         * @return this builder.
         */
        public Builder muteTargets(MuteTargets muteTargets) {
            this.muteTargets = muteTargets;
            return this;
        }

        /**
         * The date and time when the mute rule becomes active. If not set, the rule is active immediately.
         *
         * @return this builder.
         */
        public Builder startDate(Instant startDate) {
            this.startDate = startDate;
            return this;
        }

        /**
         * The date and time when the mute rule expires and is no longer evaluated.
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
         * The date and time when the mute rule was last updated.
         *
         * @return this builder.
         */
        public Builder lastUpdatedTimestamp(Instant lastUpdatedTimestamp) {
            this.lastUpdatedTimestamp = lastUpdatedTimestamp;
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

        @Override
        public GetAlarmMuteRuleOutput build() {
            return new GetAlarmMuteRuleOutput(this);
        }

        @Override
        @SuppressWarnings("unchecked")
        public void setMemberValue(Schema member, Object value) {
            switch (member.memberIndex()) {
                case 0 -> name((String) SchemaUtils.validateSameMember($SCHEMA_NAME, member, value));
                case 1 -> alarmMuteRuleArn((String) SchemaUtils.validateSameMember($SCHEMA_ALARM_MUTE_RULE_ARN, member, value));
                case 2 -> description((String) SchemaUtils.validateSameMember($SCHEMA_DESCRIPTION, member, value));
                case 3 -> rule((Rule) SchemaUtils.validateSameMember($SCHEMA_RULE, member, value));
                case 4 -> muteTargets((MuteTargets) SchemaUtils.validateSameMember($SCHEMA_MUTE_TARGETS, member, value));
                case 5 -> startDate((Instant) SchemaUtils.validateSameMember($SCHEMA_START_DATE, member, value));
                case 6 -> expireDate((Instant) SchemaUtils.validateSameMember($SCHEMA_EXPIRE_DATE, member, value));
                case 7 -> status((AlarmMuteRuleStatus) SchemaUtils.validateSameMember($SCHEMA_STATUS, member, value));
                case 8 -> lastUpdatedTimestamp((Instant) SchemaUtils.validateSameMember($SCHEMA_LAST_UPDATED_TIMESTAMP, member, value));
                case 9 -> muteType((String) SchemaUtils.validateSameMember($SCHEMA_MUTE_TYPE, member, value));
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
                    case 0 -> builder.name(de.readString(member));
                    case 1 -> builder.alarmMuteRuleArn(de.readString(member));
                    case 2 -> builder.description(de.readString(member));
                    case 3 -> builder.rule(Rule.builder().deserializeMember(de, member).build());
                    case 4 -> builder.muteTargets(MuteTargets.builder().deserializeMember(de, member).build());
                    case 5 -> builder.startDate(de.readTimestamp(member));
                    case 6 -> builder.expireDate(de.readTimestamp(member));
                    case 7 -> builder.status(AlarmMuteRuleStatus.builder().deserializeMember(de, member).build());
                    case 8 -> builder.lastUpdatedTimestamp(de.readTimestamp(member));
                    case 9 -> builder.muteType(de.readString(member));
                    default -> throw new IllegalArgumentException("Unexpected member: " + member.memberName());
                }
            }
        }
    }
}
