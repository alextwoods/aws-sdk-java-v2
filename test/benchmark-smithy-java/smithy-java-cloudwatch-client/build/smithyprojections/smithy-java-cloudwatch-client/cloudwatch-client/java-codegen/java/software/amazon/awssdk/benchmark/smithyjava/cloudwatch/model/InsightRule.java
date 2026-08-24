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
 * This structure contains the definition for a Contributor Insights rule. For more information about this rule, see<a href="https://docs.aws.amazon.com/AmazonCloudWatch/latest/monitoring/ContributorInsights.html">
 * Using Constributor Insights to analyze high-cardinality data</a> in the <i>Amazon CloudWatch User Guide</i>.
 */
@SmithyGenerated
public final class InsightRule implements SerializableStruct {

    public static final Schema $SCHEMA = Schemas.INSIGHT_RULE;
    private static final Schema $SCHEMA_NAME = $SCHEMA.member("Name");
    private static final Schema $SCHEMA_STATE = $SCHEMA.member("State");
    private static final Schema $SCHEMA_SCHEMA_MEMBER = $SCHEMA.member("Schema");
    private static final Schema $SCHEMA_DEFINITION = $SCHEMA.member("Definition");
    private static final Schema $SCHEMA_MANAGED_RULE = $SCHEMA.member("ManagedRule");
    private static final Schema $SCHEMA_APPLY_ON_TRANSFORMED_LOGS = $SCHEMA.member("ApplyOnTransformedLogs");

    public static final ShapeId $ID = $SCHEMA.id();

    private final transient String name;
    private final transient String state;
    private final transient String schemaMember;
    private final transient String definition;
    private final transient Boolean managedRule;
    private final transient Boolean applyOnTransformedLogs;

    private InsightRule(Builder builder) {
        this.name = builder.name;
        this.state = builder.state;
        this.schemaMember = builder.schemaMember;
        this.definition = builder.definition;
        this.managedRule = builder.managedRule;
        this.applyOnTransformedLogs = builder.applyOnTransformedLogs;
    }

    /**
     * The name of the rule.
     */
    public String getName() {
        return name;
    }

    /**
     * Indicates whether the rule is enabled or disabled.
     */
    public String getState() {
        return state;
    }

    /**
     * For rules that you create, this is always <code>{"Name": "CloudWatchLogRule", "Version": 1}</code>. For managed
     * rules, this is <code>{"Name": "ServiceLogRule", "Version": 1}</code>
     */
    public String getSchema() {
        return schemaMember;
    }

    /**
     * The definition of the rule, as a JSON object. The definition contains the keywords used to define contributors,
     * the value to aggregate on if this rule returns a sum instead of a count, and the filters. For details on the
     * valid syntax, see <a
     * href="https://docs.aws.amazon.com/AmazonCloudWatch/latest/monitoring/ContributorInsights-RuleSyntax.html">Contributor
     * Insights Rule Syntax</a>.
     */
    public String getDefinition() {
        return definition;
    }

    /**
     * An optional built-in rule that Amazon Web Services manages.
     */
    public Boolean isManagedRule() {
        return managedRule;
    }

    /**
     * Displays whether the rule is evaluated on the transformed versions of logs, for log groups that have <a
     * href="https://docs.aws.amazon.com/AmazonCloudWatch/latest/logs/CloudWatch-Logs-Transformation.html">Log
     * transformation</a> enabled. If this is <code>false</code>, log events are evaluated before they are transformed.
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
        InsightRule that = (InsightRule) other;
        return Objects.equals(this.managedRule, that.managedRule)
               && Objects.equals(this.applyOnTransformedLogs, that.applyOnTransformedLogs)
               && Objects.equals(this.name, that.name)
               && Objects.equals(this.state, that.state)
               && Objects.equals(this.schemaMember, that.schemaMember)
               && Objects.equals(this.definition, that.definition);
    }

    @Override
    public int hashCode() {
        int $hc = Objects.hashCode(name);
        $hc = 31 * $hc + Objects.hashCode(state);
        $hc = 31 * $hc + Objects.hashCode(schemaMember);
        $hc = 31 * $hc + Objects.hashCode(definition);
        $hc = 31 * $hc + Objects.hashCode(managedRule);
        $hc = 31 * $hc + Objects.hashCode(applyOnTransformedLogs);
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
        if (state != null) {
            serializer.writeString($SCHEMA_STATE, state);
        }
        if (schemaMember != null) {
            serializer.writeString($SCHEMA_SCHEMA_MEMBER, schemaMember);
        }
        if (definition != null) {
            serializer.writeString($SCHEMA_DEFINITION, definition);
        }
        if (managedRule != null) {
            serializer.writeBoolean($SCHEMA_MANAGED_RULE, managedRule);
        }
        if (applyOnTransformedLogs != null) {
            serializer.writeBoolean($SCHEMA_APPLY_ON_TRANSFORMED_LOGS, applyOnTransformedLogs);
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T getMemberValue(Schema member) {
        return switch (member.memberIndex()) {
            case 0 -> (T) SchemaUtils.validateSameMember($SCHEMA_NAME, member, name);
            case 1 -> (T) SchemaUtils.validateSameMember($SCHEMA_STATE, member, state);
            case 2 -> (T) SchemaUtils.validateSameMember($SCHEMA_SCHEMA_MEMBER, member, schemaMember);
            case 3 -> (T) SchemaUtils.validateSameMember($SCHEMA_DEFINITION, member, definition);
            case 4 -> (T) SchemaUtils.validateSameMember($SCHEMA_MANAGED_RULE, member, managedRule);
            case 5 -> (T) SchemaUtils.validateSameMember($SCHEMA_APPLY_ON_TRANSFORMED_LOGS, member, applyOnTransformedLogs);
            default -> throw new IllegalArgumentException("Attempted to get non-existent member: " + member.id());
        };
    }

    /**
     * Create a new builder containing all the current property values of this object.
     *
     * <p><strong>Note:</strong> This method performs only a shallow copy of the original properties.
     *
     * @return a builder for {@link InsightRule}.
     */
    public Builder toBuilder() {
        var builder = new Builder();
        builder.name(this.name);
        builder.state(this.state);
        builder.schemaMember(this.schemaMember);
        builder.definition(this.definition);
        builder.managedRule(this.managedRule);
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
     * Builder for {@link InsightRule}.
     */
    public static final class Builder implements ShapeBuilder<InsightRule> {
        private final PresenceTracker tracker = PresenceTracker.of($SCHEMA);
        private String name;
        private String state;
        private String schemaMember;
        private String definition;
        private Boolean managedRule;
        private Boolean applyOnTransformedLogs;

        private Builder() {
            // Tell the tracker to assume clientOptional members are present.
            tracker.setMember($SCHEMA_NAME);
            tracker.setMember($SCHEMA_STATE);
            tracker.setMember($SCHEMA_SCHEMA_MEMBER);
            tracker.setMember($SCHEMA_DEFINITION);
        }

        @Override
        public Schema schema() {
            return $SCHEMA;
        }

        /**
         * The name of the rule.
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
         * Indicates whether the rule is enabled or disabled.
         *
         * <p><strong>Required</strong>
         * @return this builder.
         */
        public Builder state(String state) {
            this.state = Objects.requireNonNull(state, "state cannot be null");
            tracker.setMember($SCHEMA_STATE);
            return this;
        }

        /**
         * For rules that you create, this is always <code>{"Name": "CloudWatchLogRule", "Version": 1}</code>. For managed
         * rules, this is <code>{"Name": "ServiceLogRule", "Version": 1}</code>
         *
         * <p><strong>Required</strong>
         * @return this builder.
         */
        public Builder schemaMember(String schemaMember) {
            this.schemaMember = Objects.requireNonNull(schemaMember, "schemaMember cannot be null");
            tracker.setMember($SCHEMA_SCHEMA_MEMBER);
            return this;
        }

        /**
         * The definition of the rule, as a JSON object. The definition contains the keywords used to define contributors,
         * the value to aggregate on if this rule returns a sum instead of a count, and the filters. For details on the
         * valid syntax, see <a
         * href="https://docs.aws.amazon.com/AmazonCloudWatch/latest/monitoring/ContributorInsights-RuleSyntax.html">Contributor
         * Insights Rule Syntax</a>.
         *
         * <p><strong>Required</strong>
         * @return this builder.
         */
        public Builder definition(String definition) {
            this.definition = Objects.requireNonNull(definition, "definition cannot be null");
            tracker.setMember($SCHEMA_DEFINITION);
            return this;
        }

        /**
         * An optional built-in rule that Amazon Web Services manages.
         *
         * @return this builder.
         */
        public Builder managedRule(Boolean managedRule) {
            this.managedRule = managedRule;
            return this;
        }

        /**
         * Displays whether the rule is evaluated on the transformed versions of logs, for log groups that have <a
         * href="https://docs.aws.amazon.com/AmazonCloudWatch/latest/logs/CloudWatch-Logs-Transformation.html">Log
         * transformation</a> enabled. If this is <code>false</code>, log events are evaluated before they are transformed.
         *
         * @return this builder.
         */
        public Builder applyOnTransformedLogs(Boolean applyOnTransformedLogs) {
            this.applyOnTransformedLogs = applyOnTransformedLogs;
            return this;
        }

        @Override
        public InsightRule build() {
            tracker.validate();
            return new InsightRule(this);
        }

        @Override
        @SuppressWarnings("unchecked")
        public void setMemberValue(Schema member, Object value) {
            switch (member.memberIndex()) {
                case 0 -> name((String) SchemaUtils.validateSameMember($SCHEMA_NAME, member, value));
                case 1 -> state((String) SchemaUtils.validateSameMember($SCHEMA_STATE, member, value));
                case 2 -> schemaMember((String) SchemaUtils.validateSameMember($SCHEMA_SCHEMA_MEMBER, member, value));
                case 3 -> definition((String) SchemaUtils.validateSameMember($SCHEMA_DEFINITION, member, value));
                case 4 -> managedRule((Boolean) SchemaUtils.validateSameMember($SCHEMA_MANAGED_RULE, member, value));
                case 5 -> applyOnTransformedLogs((Boolean) SchemaUtils.validateSameMember($SCHEMA_APPLY_ON_TRANSFORMED_LOGS, member, value));
                default -> ShapeBuilder.super.setMemberValue(member, value);
            }
        }

        @Override
        public ShapeBuilder<InsightRule> errorCorrection() {
            if (tracker.allSet()) {
                return this;
            }
            if (!tracker.checkMember($SCHEMA_NAME)) {
                name("");
            }
            if (!tracker.checkMember($SCHEMA_STATE)) {
                state("");
            }
            if (!tracker.checkMember($SCHEMA_SCHEMA_MEMBER)) {
                schemaMember("");
            }
            if (!tracker.checkMember($SCHEMA_DEFINITION)) {
                definition("");
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
                    case 1 -> builder.state(de.readString(member));
                    case 2 -> builder.schemaMember(de.readString(member));
                    case 3 -> builder.definition(de.readString(member));
                    case 4 -> builder.managedRule(de.readBoolean(member));
                    case 5 -> builder.applyOnTransformedLogs(de.readBoolean(member));
                    default -> throw new IllegalArgumentException("Unexpected member: " + member.memberName());
                }
            }
        }
    }
}
