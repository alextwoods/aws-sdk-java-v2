package software.amazon.awssdk.benchmark.smithyjava.cloudwatch.model;

import java.util.Collections;
import java.util.List;
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
public final class DescribeInsightRulesOutput implements SerializableStruct {

    public static final Schema $SCHEMA = Schemas.DESCRIBE_INSIGHT_RULES_OUTPUT;
    private static final Schema $SCHEMA_NEXT_TOKEN = $SCHEMA.member("NextToken");
    private static final Schema $SCHEMA_INSIGHT_RULES = $SCHEMA.member("InsightRules");

    public static final ShapeId $ID = $SCHEMA.id();

    private final transient String nextToken;
    private final transient List<InsightRule> insightRules;

    private DescribeInsightRulesOutput(Builder builder) {
        this.nextToken = builder.nextToken;
        this.insightRules = builder.insightRules == null ? null : Collections.unmodifiableList(builder.insightRules);
    }

    /**
     * If this parameter is present, it is a token that marks the start of the next batch of returned results.
     */
    public String getNextToken() {
        return nextToken;
    }

    /**
     * The rules returned by the operation.
     */
    public List<InsightRule> getInsightRules() {
        if (insightRules == null) {
            return Collections.emptyList();
        }
        return insightRules;
    }

    public boolean hasInsightRules() {
        return insightRules != null;
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
        DescribeInsightRulesOutput that = (DescribeInsightRulesOutput) other;
        return Objects.equals(this.nextToken, that.nextToken)
               && Objects.equals(this.insightRules, that.insightRules);
    }

    @Override
    public int hashCode() {
        int $hc = Objects.hashCode(nextToken);
        $hc = 31 * $hc + Objects.hashCode(insightRules);
        return $hc;
    }

    @Override
    public Schema schema() {
        return $SCHEMA;
    }

    @Override
    public void serializeMembers(ShapeSerializer serializer) {
        if (nextToken != null) {
            serializer.writeString($SCHEMA_NEXT_TOKEN, nextToken);
        }
        if (insightRules != null) {
            serializer.writeList($SCHEMA_INSIGHT_RULES, insightRules, insightRules.size(), SharedSerde.InsightRulesSerializer.INSTANCE);
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T getMemberValue(Schema member) {
        return switch (member.memberIndex()) {
            case 0 -> (T) SchemaUtils.validateSameMember($SCHEMA_NEXT_TOKEN, member, nextToken);
            case 1 -> (T) SchemaUtils.validateSameMember($SCHEMA_INSIGHT_RULES, member, insightRules);
            default -> throw new IllegalArgumentException("Attempted to get non-existent member: " + member.id());
        };
    }

    /**
     * Create a new builder containing all the current property values of this object.
     *
     * <p><strong>Note:</strong> This method performs only a shallow copy of the original properties.
     *
     * @return a builder for {@link DescribeInsightRulesOutput}.
     */
    public Builder toBuilder() {
        var builder = new Builder();
        builder.nextToken(this.nextToken);
        builder.insightRules(this.insightRules);
        return builder;
    }

    /**
     * @return returns a new Builder.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for {@link DescribeInsightRulesOutput}.
     */
    public static final class Builder implements ShapeBuilder<DescribeInsightRulesOutput> {
        private String nextToken;
        private List<InsightRule> insightRules;

        private Builder() {}

        @Override
        public Schema schema() {
            return $SCHEMA;
        }

        /**
         * If this parameter is present, it is a token that marks the start of the next batch of returned results.
         *
         * @return this builder.
         */
        public Builder nextToken(String nextToken) {
            this.nextToken = nextToken;
            return this;
        }

        /**
         * The rules returned by the operation.
         *
         * @return this builder.
         */
        public Builder insightRules(List<InsightRule> insightRules) {
            this.insightRules = insightRules;
            return this;
        }

        @Override
        public DescribeInsightRulesOutput build() {
            return new DescribeInsightRulesOutput(this);
        }

        @Override
        @SuppressWarnings("unchecked")
        public void setMemberValue(Schema member, Object value) {
            switch (member.memberIndex()) {
                case 0 -> nextToken((String) SchemaUtils.validateSameMember($SCHEMA_NEXT_TOKEN, member, value));
                case 1 -> insightRules((List<InsightRule>) SchemaUtils.validateSameMember($SCHEMA_INSIGHT_RULES, member, value));
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
                    case 0 -> builder.nextToken(de.readString(member));
                    case 1 -> builder.insightRules(SharedSerde.deserializeInsightRules(member, de));
                    default -> throw new IllegalArgumentException("Unexpected member: " + member.memberName());
                }
            }
        }
    }
}
