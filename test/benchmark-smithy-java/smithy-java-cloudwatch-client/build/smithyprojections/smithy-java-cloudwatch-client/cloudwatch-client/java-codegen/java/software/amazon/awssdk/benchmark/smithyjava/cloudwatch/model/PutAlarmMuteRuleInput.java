package software.amazon.awssdk.benchmark.smithyjava.cloudwatch.model;

import java.time.Instant;
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
public final class PutAlarmMuteRuleInput implements SerializableStruct {

    public static final Schema $SCHEMA = Schemas.PUT_ALARM_MUTE_RULE_INPUT;
    private static final Schema $SCHEMA_NAME = $SCHEMA.member("Name");
    private static final Schema $SCHEMA_DESCRIPTION = $SCHEMA.member("Description");
    private static final Schema $SCHEMA_RULE = $SCHEMA.member("Rule");
    private static final Schema $SCHEMA_MUTE_TARGETS = $SCHEMA.member("MuteTargets");
    private static final Schema $SCHEMA_TAGS = $SCHEMA.member("Tags");
    private static final Schema $SCHEMA_START_DATE = $SCHEMA.member("StartDate");
    private static final Schema $SCHEMA_EXPIRE_DATE = $SCHEMA.member("ExpireDate");

    public static final ShapeId $ID = $SCHEMA.id();

    private final transient String name;
    private final transient String description;
    private final transient Rule rule;
    private final transient MuteTargets muteTargets;
    private final transient List<Tag> tags;
    private final transient Instant startDate;
    private final transient Instant expireDate;

    private PutAlarmMuteRuleInput(Builder builder) {
        this.name = builder.name;
        this.description = builder.description;
        this.rule = builder.rule;
        this.muteTargets = builder.muteTargets;
        this.tags = builder.tags == null ? null : Collections.unmodifiableList(builder.tags);
        this.startDate = builder.startDate;
        this.expireDate = builder.expireDate;
    }

    /**
     * The name of the alarm mute rule. This name must be unique within your Amazon Web Services account and region.
     */
    public String getName() {
        return name;
    }

    /**
     * A description of the alarm mute rule that helps you identify its purpose.
     */
    public String getDescription() {
        return description;
    }

    /**
     * The configuration that defines when and how long alarms should be muted.
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
     * A list of key-value pairs to associate with the alarm mute rule. You can use tags to categorize and manage your
     * mute rules.
     */
    public List<Tag> getTags() {
        if (tags == null) {
            return Collections.emptyList();
        }
        return tags;
    }

    public boolean hasTags() {
        return tags != null;
    }

    /**
     * The date and time after which the mute rule takes effect, specified as a timestamp in ISO 8601 format (for
     * example, <code>2026-04-15T08:00:00Z</code>). If not specified, the mute rule takes effect immediately upon
     * creation and the mutes are applied as per the schedule expression.
     */
    public Instant getStartDate() {
        return startDate;
    }

    /**
     * The date and time when the mute rule expires and is no longer evaluated, specified as a timestamp in ISO 8601
     * format (for example, <code>2026-12-31T23:59:59Z</code>). After this time, the rule status becomes EXPIRED and
     * will no longer mute the targeted alarms.
     */
    public Instant getExpireDate() {
        return expireDate;
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
        PutAlarmMuteRuleInput that = (PutAlarmMuteRuleInput) other;
        return Objects.equals(this.name, that.name)
               && Objects.equals(this.description, that.description)
               && Objects.equals(this.startDate, that.startDate)
               && Objects.equals(this.expireDate, that.expireDate)
               && Objects.equals(this.rule, that.rule)
               && Objects.equals(this.muteTargets, that.muteTargets)
               && Objects.equals(this.tags, that.tags);
    }

    @Override
    public int hashCode() {
        int $hc = Objects.hashCode(name);
        $hc = 31 * $hc + Objects.hashCode(description);
        $hc = 31 * $hc + Objects.hashCode(rule);
        $hc = 31 * $hc + Objects.hashCode(muteTargets);
        $hc = 31 * $hc + Objects.hashCode(tags);
        $hc = 31 * $hc + Objects.hashCode(startDate);
        $hc = 31 * $hc + Objects.hashCode(expireDate);
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
        if (description != null) {
            serializer.writeString($SCHEMA_DESCRIPTION, description);
        }
        if (rule != null) {
            serializer.writeStruct($SCHEMA_RULE, rule);
        }
        if (muteTargets != null) {
            serializer.writeStruct($SCHEMA_MUTE_TARGETS, muteTargets);
        }
        if (tags != null) {
            serializer.writeList($SCHEMA_TAGS, tags, tags.size(), SharedSerde.TagListSerializer.INSTANCE);
        }
        if (startDate != null) {
            serializer.writeTimestamp($SCHEMA_START_DATE, startDate);
        }
        if (expireDate != null) {
            serializer.writeTimestamp($SCHEMA_EXPIRE_DATE, expireDate);
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T getMemberValue(Schema member) {
        return switch (member.memberIndex()) {
            case 0 -> (T) SchemaUtils.validateSameMember($SCHEMA_NAME, member, name);
            case 1 -> (T) SchemaUtils.validateSameMember($SCHEMA_RULE, member, rule);
            case 2 -> (T) SchemaUtils.validateSameMember($SCHEMA_DESCRIPTION, member, description);
            case 3 -> (T) SchemaUtils.validateSameMember($SCHEMA_MUTE_TARGETS, member, muteTargets);
            case 4 -> (T) SchemaUtils.validateSameMember($SCHEMA_TAGS, member, tags);
            case 5 -> (T) SchemaUtils.validateSameMember($SCHEMA_START_DATE, member, startDate);
            case 6 -> (T) SchemaUtils.validateSameMember($SCHEMA_EXPIRE_DATE, member, expireDate);
            default -> throw new IllegalArgumentException("Attempted to get non-existent member: " + member.id());
        };
    }

    /**
     * Create a new builder containing all the current property values of this object.
     *
     * <p><strong>Note:</strong> This method performs only a shallow copy of the original properties.
     *
     * @return a builder for {@link PutAlarmMuteRuleInput}.
     */
    public Builder toBuilder() {
        var builder = new Builder();
        builder.name(this.name);
        builder.description(this.description);
        builder.rule(this.rule);
        builder.muteTargets(this.muteTargets);
        builder.tags(this.tags);
        builder.startDate(this.startDate);
        builder.expireDate(this.expireDate);
        return builder;
    }

    /**
     * @return returns a new Builder.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for {@link PutAlarmMuteRuleInput}.
     */
    public static final class Builder implements ShapeBuilder<PutAlarmMuteRuleInput> {
        private final PresenceTracker tracker = PresenceTracker.of($SCHEMA);
        private String name;
        private String description;
        private Rule rule;
        private MuteTargets muteTargets;
        private List<Tag> tags;
        private Instant startDate;
        private Instant expireDate;

        private Builder() {
            // Tell the tracker to assume clientOptional members are present.
            tracker.setMember($SCHEMA_NAME);
            tracker.setMember($SCHEMA_RULE);
        }

        @Override
        public Schema schema() {
            return $SCHEMA;
        }

        /**
         * The name of the alarm mute rule. This name must be unique within your Amazon Web Services account and region.
         *
         * <p><strong>Required</strong>
         * @return this builder.
         */
        public Builder name(String name) {
            this.name = Objects.requireNonNull(name, "name cannot be null");
            tracker.setMember($SCHEMA_NAME);
            return this;
        }

        /**
         * A description of the alarm mute rule that helps you identify its purpose.
         *
         * @return this builder.
         */
        public Builder description(String description) {
            this.description = description;
            return this;
        }

        /**
         * The configuration that defines when and how long alarms should be muted.
         *
         * <p><strong>Required</strong>
         * @return this builder.
         */
        public Builder rule(Rule rule) {
            this.rule = Objects.requireNonNull(rule, "rule cannot be null");
            tracker.setMember($SCHEMA_RULE);
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
         * A list of key-value pairs to associate with the alarm mute rule. You can use tags to categorize and manage your
         * mute rules.
         *
         * @return this builder.
         */
        public Builder tags(List<Tag> tags) {
            this.tags = tags;
            return this;
        }

        /**
         * The date and time after which the mute rule takes effect, specified as a timestamp in ISO 8601 format (for
         * example, <code>2026-04-15T08:00:00Z</code>). If not specified, the mute rule takes effect immediately upon
         * creation and the mutes are applied as per the schedule expression.
         *
         * @return this builder.
         */
        public Builder startDate(Instant startDate) {
            this.startDate = startDate;
            return this;
        }

        /**
         * The date and time when the mute rule expires and is no longer evaluated, specified as a timestamp in ISO 8601
         * format (for example, <code>2026-12-31T23:59:59Z</code>). After this time, the rule status becomes EXPIRED and
         * will no longer mute the targeted alarms.
         *
         * @return this builder.
         */
        public Builder expireDate(Instant expireDate) {
            this.expireDate = expireDate;
            return this;
        }

        @Override
        public PutAlarmMuteRuleInput build() {
            tracker.validate();
            return new PutAlarmMuteRuleInput(this);
        }

        @Override
        @SuppressWarnings("unchecked")
        public void setMemberValue(Schema member, Object value) {
            switch (member.memberIndex()) {
                case 0 -> name((String) SchemaUtils.validateSameMember($SCHEMA_NAME, member, value));
                case 1 -> rule((Rule) SchemaUtils.validateSameMember($SCHEMA_RULE, member, value));
                case 2 -> description((String) SchemaUtils.validateSameMember($SCHEMA_DESCRIPTION, member, value));
                case 3 -> muteTargets((MuteTargets) SchemaUtils.validateSameMember($SCHEMA_MUTE_TARGETS, member, value));
                case 4 -> tags((List<Tag>) SchemaUtils.validateSameMember($SCHEMA_TAGS, member, value));
                case 5 -> startDate((Instant) SchemaUtils.validateSameMember($SCHEMA_START_DATE, member, value));
                case 6 -> expireDate((Instant) SchemaUtils.validateSameMember($SCHEMA_EXPIRE_DATE, member, value));
                default -> ShapeBuilder.super.setMemberValue(member, value);
            }
        }

        @Override
        public ShapeBuilder<PutAlarmMuteRuleInput> errorCorrection() {
            if (tracker.allSet()) {
                return this;
            }
            if (!tracker.checkMember($SCHEMA_NAME)) {
                name("");
            }
            if (!tracker.checkMember($SCHEMA_RULE)) {
                tracker.setMember($SCHEMA_RULE);
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
                    case 0 -> builder.name(de.readString(member));
                    case 1 -> builder.rule(Rule.builder().deserializeMember(de, member).build());
                    case 2 -> builder.description(de.readString(member));
                    case 3 -> builder.muteTargets(MuteTargets.builder().deserializeMember(de, member).build());
                    case 4 -> builder.tags(SharedSerde.deserializeTagList(member, de));
                    case 5 -> builder.startDate(de.readTimestamp(member));
                    case 6 -> builder.expireDate(de.readTimestamp(member));
                    default -> throw new IllegalArgumentException("Unexpected member: " + member.memberName());
                }
            }
        }
    }
}
