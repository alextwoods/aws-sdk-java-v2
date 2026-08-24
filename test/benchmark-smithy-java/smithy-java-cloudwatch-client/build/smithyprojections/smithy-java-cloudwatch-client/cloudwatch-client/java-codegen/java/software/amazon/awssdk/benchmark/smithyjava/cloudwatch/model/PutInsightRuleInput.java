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
public final class PutInsightRuleInput implements SerializableStruct {

    public static final Schema $SCHEMA = Schemas.PUT_INSIGHT_RULE_INPUT;
    private static final Schema $SCHEMA_RULE_NAME = $SCHEMA.member("RuleName");
    private static final Schema $SCHEMA_RULE_STATE = $SCHEMA.member("RuleState");
    private static final Schema $SCHEMA_RULE_DEFINITION = $SCHEMA.member("RuleDefinition");
    private static final Schema $SCHEMA_TAGS = $SCHEMA.member("Tags");
    private static final Schema $SCHEMA_APPLY_ON_TRANSFORMED_LOGS = $SCHEMA.member("ApplyOnTransformedLogs");

    public static final ShapeId $ID = $SCHEMA.id();

    private final transient String ruleName;
    private final transient String ruleState;
    private final transient String ruleDefinition;
    private final transient List<Tag> tags;
    private final transient Boolean applyOnTransformedLogs;

    private PutInsightRuleInput(Builder builder) {
        this.ruleName = builder.ruleName;
        this.ruleState = builder.ruleState;
        this.ruleDefinition = builder.ruleDefinition;
        this.tags = builder.tags == null ? null : Collections.unmodifiableList(builder.tags);
        this.applyOnTransformedLogs = builder.applyOnTransformedLogs;
    }

    /**
     * A unique name for the rule.
     */
    public String getRuleName() {
        return ruleName;
    }

    /**
     * The state of the rule. Valid values are ENABLED and DISABLED.
     */
    public String getRuleState() {
        return ruleState;
    }

    /**
     * The definition of the rule, as a JSON object. For details on the valid syntax, see <a
     * href="https://docs.aws.amazon.com/AmazonCloudWatch/latest/monitoring/ContributorInsights-RuleSyntax.html">Contributor
     * Insights Rule Syntax</a>.
     */
    public String getRuleDefinition() {
        return ruleDefinition;
    }

    /**
     * A list of key-value pairs to associate with the Contributor Insights rule. You can associate as many as 50 tags
     * with a rule.
     *
     * <p>Tags can help you organize and categorize your resources. You can also use them to scope user permissions, by
     * granting a user permission to access or change only the resources that have certain tag values.
     *
     * <p>To be able to associate tags with a rule, you must have the <code>cloudwatch:TagResource</code> permission in
     * addition to the <code>cloudwatch:PutInsightRule</code> permission.
     *
     * <p>If you are using this operation to update an existing Contributor Insights rule, any tags you specify in this
     * parameter are ignored. To change the tags of an existing rule, use <a href="https://docs.aws.amazon.com/AmazonCloudWatch/latest/APIReference/API_TagResource.html">TagResource</a>.
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
     * Specify <code>true</code> to have this rule evaluate log events after they have been transformed by <a href="https://docs.aws.amazon.com/AmazonCloudWatch/latest/logs/CloudWatch-Logs-Transformation.html">Log
     * transformation</a>. If you specify <code>true</code>, then the log events in log groups that have transformers
     * will be evaluated by Contributor Insights after being transformed. Log groups that don't have transformers will
     * still have their original log events evaluated by Contributor Insights.
     *
     * <p>The default is <code>false</code>
     *
     * <p>If a log group has a transformer, and transformation fails for some log events, those log events won't be
     * evaluated by Contributor Insights. For information about investigating log transformation failures, see <a href="https://docs.aws.amazon.com/AmazonCloudWatch/latest/logs/Transformation-Errors-Metrics.html">
     * Transformation metrics and errors</a>.
     */
    public Boolean isApplyOnTransformedLogs() {
        return applyOnTransformedLogs;
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
        PutInsightRuleInput that = (PutInsightRuleInput) other;
        return Objects.equals(this.applyOnTransformedLogs, that.applyOnTransformedLogs)
               && Objects.equals(this.ruleName, that.ruleName)
               && Objects.equals(this.ruleState, that.ruleState)
               && Objects.equals(this.ruleDefinition, that.ruleDefinition)
               && Objects.equals(this.tags, that.tags);
    }

    @Override
    public int hashCode() {
        int $hc = Objects.hashCode(ruleName);
        $hc = 31 * $hc + Objects.hashCode(ruleState);
        $hc = 31 * $hc + Objects.hashCode(ruleDefinition);
        $hc = 31 * $hc + Objects.hashCode(tags);
        $hc = 31 * $hc + Objects.hashCode(applyOnTransformedLogs);
        return $hc;
    }

    @Override
    public Schema schema() {
        return $SCHEMA;
    }

    @Override
    public void serializeMembers(ShapeSerializer serializer) {
        if (ruleName != null) {
            serializer.writeString($SCHEMA_RULE_NAME, ruleName);
        }
        if (ruleState != null) {
            serializer.writeString($SCHEMA_RULE_STATE, ruleState);
        }
        if (ruleDefinition != null) {
            serializer.writeString($SCHEMA_RULE_DEFINITION, ruleDefinition);
        }
        if (tags != null) {
            serializer.writeList($SCHEMA_TAGS, tags, tags.size(), SharedSerde.TagListSerializer.INSTANCE);
        }
        if (applyOnTransformedLogs != null) {
            serializer.writeBoolean($SCHEMA_APPLY_ON_TRANSFORMED_LOGS, applyOnTransformedLogs);
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T getMemberValue(Schema member) {
        return switch (member.memberIndex()) {
            case 0 -> (T) SchemaUtils.validateSameMember($SCHEMA_RULE_NAME, member, ruleName);
            case 1 -> (T) SchemaUtils.validateSameMember($SCHEMA_RULE_DEFINITION, member, ruleDefinition);
            case 2 -> (T) SchemaUtils.validateSameMember($SCHEMA_RULE_STATE, member, ruleState);
            case 3 -> (T) SchemaUtils.validateSameMember($SCHEMA_TAGS, member, tags);
            case 4 -> (T) SchemaUtils.validateSameMember($SCHEMA_APPLY_ON_TRANSFORMED_LOGS, member, applyOnTransformedLogs);
            default -> throw new IllegalArgumentException("Attempted to get non-existent member: " + member.id());
        };
    }

    /**
     * Create a new builder containing all the current property values of this object.
     *
     * <p><strong>Note:</strong> This method performs only a shallow copy of the original properties.
     *
     * @return a builder for {@link PutInsightRuleInput}.
     */
    public Builder toBuilder() {
        var builder = new Builder();
        builder.ruleName(this.ruleName);
        builder.ruleState(this.ruleState);
        builder.ruleDefinition(this.ruleDefinition);
        builder.tags(this.tags);
        builder.applyOnTransformedLogs(this.applyOnTransformedLogs);
        return builder;
    }

    /**
     * @return returns a new Builder.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for {@link PutInsightRuleInput}.
     */
    public static final class Builder implements ShapeBuilder<PutInsightRuleInput> {
        private final PresenceTracker tracker = PresenceTracker.of($SCHEMA);
        private String ruleName;
        private String ruleState;
        private String ruleDefinition;
        private List<Tag> tags;
        private Boolean applyOnTransformedLogs;

        private Builder() {
            // Tell the tracker to assume clientOptional members are present.
            tracker.setMember($SCHEMA_RULE_NAME);
            tracker.setMember($SCHEMA_RULE_DEFINITION);
        }

        @Override
        public Schema schema() {
            return $SCHEMA;
        }

        /**
         * A unique name for the rule.
         *
         * <p><strong>Required</strong>
         * @return this builder.
         */
        public Builder ruleName(String ruleName) {
            this.ruleName = Objects.requireNonNull(ruleName, "ruleName cannot be null");
            tracker.setMember($SCHEMA_RULE_NAME);
            return this;
        }

        /**
         * The state of the rule. Valid values are ENABLED and DISABLED.
         *
         * @return this builder.
         */
        public Builder ruleState(String ruleState) {
            this.ruleState = ruleState;
            return this;
        }

        /**
         * The definition of the rule, as a JSON object. For details on the valid syntax, see <a
         * href="https://docs.aws.amazon.com/AmazonCloudWatch/latest/monitoring/ContributorInsights-RuleSyntax.html">Contributor
         * Insights Rule Syntax</a>.
         *
         * <p><strong>Required</strong>
         * @return this builder.
         */
        public Builder ruleDefinition(String ruleDefinition) {
            this.ruleDefinition = Objects.requireNonNull(ruleDefinition, "ruleDefinition cannot be null");
            tracker.setMember($SCHEMA_RULE_DEFINITION);
            return this;
        }

        /**
         * A list of key-value pairs to associate with the Contributor Insights rule. You can associate as many as 50 tags
         * with a rule.
         *
         * <p>Tags can help you organize and categorize your resources. You can also use them to scope user permissions, by
         * granting a user permission to access or change only the resources that have certain tag values.
         *
         * <p>To be able to associate tags with a rule, you must have the <code>cloudwatch:TagResource</code> permission in
         * addition to the <code>cloudwatch:PutInsightRule</code> permission.
         *
         * <p>If you are using this operation to update an existing Contributor Insights rule, any tags you specify in this
         * parameter are ignored. To change the tags of an existing rule, use <a href="https://docs.aws.amazon.com/AmazonCloudWatch/latest/APIReference/API_TagResource.html">TagResource</a>.
         *
         * @return this builder.
         */
        public Builder tags(List<Tag> tags) {
            this.tags = tags;
            return this;
        }

        /**
         * Specify <code>true</code> to have this rule evaluate log events after they have been transformed by <a href="https://docs.aws.amazon.com/AmazonCloudWatch/latest/logs/CloudWatch-Logs-Transformation.html">Log
         * transformation</a>. If you specify <code>true</code>, then the log events in log groups that have transformers
         * will be evaluated by Contributor Insights after being transformed. Log groups that don't have transformers will
         * still have their original log events evaluated by Contributor Insights.
         *
         * <p>The default is <code>false</code>
         *
         * <p>If a log group has a transformer, and transformation fails for some log events, those log events won't be
         * evaluated by Contributor Insights. For information about investigating log transformation failures, see <a href="https://docs.aws.amazon.com/AmazonCloudWatch/latest/logs/Transformation-Errors-Metrics.html">
         * Transformation metrics and errors</a>.
         *
         * @return this builder.
         */
        public Builder applyOnTransformedLogs(Boolean applyOnTransformedLogs) {
            this.applyOnTransformedLogs = applyOnTransformedLogs;
            return this;
        }

        @Override
        public PutInsightRuleInput build() {
            tracker.validate();
            return new PutInsightRuleInput(this);
        }

        @Override
        @SuppressWarnings("unchecked")
        public void setMemberValue(Schema member, Object value) {
            switch (member.memberIndex()) {
                case 0 -> ruleName((String) SchemaUtils.validateSameMember($SCHEMA_RULE_NAME, member, value));
                case 1 -> ruleDefinition((String) SchemaUtils.validateSameMember($SCHEMA_RULE_DEFINITION, member, value));
                case 2 -> ruleState((String) SchemaUtils.validateSameMember($SCHEMA_RULE_STATE, member, value));
                case 3 -> tags((List<Tag>) SchemaUtils.validateSameMember($SCHEMA_TAGS, member, value));
                case 4 -> applyOnTransformedLogs((Boolean) SchemaUtils.validateSameMember($SCHEMA_APPLY_ON_TRANSFORMED_LOGS, member, value));
                default -> ShapeBuilder.super.setMemberValue(member, value);
            }
        }

        @Override
        public ShapeBuilder<PutInsightRuleInput> errorCorrection() {
            if (tracker.allSet()) {
                return this;
            }
            if (!tracker.checkMember($SCHEMA_RULE_NAME)) {
                ruleName("");
            }
            if (!tracker.checkMember($SCHEMA_RULE_DEFINITION)) {
                ruleDefinition("");
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
                    case 0 -> builder.ruleName(de.readString(member));
                    case 1 -> builder.ruleDefinition(de.readString(member));
                    case 2 -> builder.ruleState(de.readString(member));
                    case 3 -> builder.tags(SharedSerde.deserializeTagList(member, de));
                    case 4 -> builder.applyOnTransformedLogs(de.readBoolean(member));
                    default -> throw new IllegalArgumentException("Unexpected member: " + member.memberName());
                }
            }
        }
    }
}
