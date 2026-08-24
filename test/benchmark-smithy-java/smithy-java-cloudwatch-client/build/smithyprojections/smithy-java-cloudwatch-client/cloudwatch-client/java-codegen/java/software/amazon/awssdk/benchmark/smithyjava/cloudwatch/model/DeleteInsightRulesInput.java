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
public final class DeleteInsightRulesInput implements SerializableStruct {

    public static final Schema $SCHEMA = Schemas.DELETE_INSIGHT_RULES_INPUT;
    private static final Schema $SCHEMA_RULE_NAMES = $SCHEMA.member("RuleNames");

    public static final ShapeId $ID = $SCHEMA.id();

    private final transient List<String> ruleNames;

    private DeleteInsightRulesInput(Builder builder) {
        this.ruleNames = builder.ruleNames == null ? null : Collections.unmodifiableList(builder.ruleNames);
    }

    /**
     * An array of the rule names to delete. If you need to find out the names of your rules, use <a
     * href="https://docs.aws.amazon.com/AmazonCloudWatch/latest/APIReference/API_DescribeInsightRules.html">DescribeInsightRules</a>.
     */
    public List<String> getRuleNames() {
        if (ruleNames == null) {
            return Collections.emptyList();
        }
        return ruleNames;
    }

    public boolean hasRuleNames() {
        return ruleNames != null;
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
        DeleteInsightRulesInput that = (DeleteInsightRulesInput) other;
        return Objects.equals(this.ruleNames, that.ruleNames);
    }

    @Override
    public int hashCode() {
        int $hc = Objects.hashCode(ruleNames);
        return $hc;
    }

    @Override
    public Schema schema() {
        return $SCHEMA;
    }

    @Override
    public void serializeMembers(ShapeSerializer serializer) {
        if (ruleNames != null) {
            serializer.writeList($SCHEMA_RULE_NAMES, ruleNames, ruleNames.size(), SharedSerde.InsightRuleNamesSerializer.INSTANCE);
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T getMemberValue(Schema member) {
        return switch (member.memberIndex()) {
            case 0 -> (T) SchemaUtils.validateSameMember($SCHEMA_RULE_NAMES, member, ruleNames);
            default -> throw new IllegalArgumentException("Attempted to get non-existent member: " + member.id());
        };
    }

    /**
     * Create a new builder containing all the current property values of this object.
     *
     * <p><strong>Note:</strong> This method performs only a shallow copy of the original properties.
     *
     * @return a builder for {@link DeleteInsightRulesInput}.
     */
    public Builder toBuilder() {
        var builder = new Builder();
        builder.ruleNames(this.ruleNames);
        return builder;
    }

    /**
     * @return returns a new Builder.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for {@link DeleteInsightRulesInput}.
     */
    public static final class Builder implements ShapeBuilder<DeleteInsightRulesInput> {
        private final PresenceTracker tracker = PresenceTracker.of($SCHEMA);
        private List<String> ruleNames;

        private Builder() {
            // Tell the tracker to assume clientOptional members are present.
            tracker.setMember($SCHEMA_RULE_NAMES);
        }

        @Override
        public Schema schema() {
            return $SCHEMA;
        }

        /**
         * An array of the rule names to delete. If you need to find out the names of your rules, use <a
         * href="https://docs.aws.amazon.com/AmazonCloudWatch/latest/APIReference/API_DescribeInsightRules.html">DescribeInsightRules</a>.
         *
         * <p><strong>Required</strong>
         * @return this builder.
         */
        public Builder ruleNames(List<String> ruleNames) {
            this.ruleNames = Objects.requireNonNull(ruleNames, "ruleNames cannot be null");
            tracker.setMember($SCHEMA_RULE_NAMES);
            return this;
        }

        @Override
        public DeleteInsightRulesInput build() {
            tracker.validate();
            return new DeleteInsightRulesInput(this);
        }

        @Override
        @SuppressWarnings("unchecked")
        public void setMemberValue(Schema member, Object value) {
            switch (member.memberIndex()) {
                case 0 -> ruleNames((List<String>) SchemaUtils.validateSameMember($SCHEMA_RULE_NAMES, member, value));
                default -> ShapeBuilder.super.setMemberValue(member, value);
            }
        }

        @Override
        public ShapeBuilder<DeleteInsightRulesInput> errorCorrection() {
            if (tracker.allSet()) {
                return this;
            }
            if (!tracker.checkMember($SCHEMA_RULE_NAMES)) {
                ruleNames(Collections.emptyList());
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
                    case 0 -> builder.ruleNames(SharedSerde.deserializeInsightRuleNames(member, de));
                    default -> throw new IllegalArgumentException("Unexpected member: " + member.memberName());
                }
            }
        }
    }
}
