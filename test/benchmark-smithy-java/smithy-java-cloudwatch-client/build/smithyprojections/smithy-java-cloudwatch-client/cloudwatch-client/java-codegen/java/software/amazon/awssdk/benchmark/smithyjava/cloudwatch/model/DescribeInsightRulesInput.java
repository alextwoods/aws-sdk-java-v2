package software.amazon.awssdk.benchmark.smithyjava.cloudwatch.model;

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
public final class DescribeInsightRulesInput implements SerializableStruct {

    public static final Schema $SCHEMA = Schemas.DESCRIBE_INSIGHT_RULES_INPUT;
    private static final Schema $SCHEMA_NEXT_TOKEN = $SCHEMA.member("NextToken");
    private static final Schema $SCHEMA_MAX_RESULTS = $SCHEMA.member("MaxResults");

    public static final ShapeId $ID = $SCHEMA.id();

    private final transient String nextToken;
    private final transient Integer maxResults;

    private DescribeInsightRulesInput(Builder builder) {
        this.nextToken = builder.nextToken;
        this.maxResults = builder.maxResults;
    }

    /**
     * Include this value, if it was returned by the previous operation, to get the next set of rules.
     */
    public String getNextToken() {
        return nextToken;
    }

    /**
     * The maximum number of results to return in one operation. If you omit this parameter, the default of 500 is used.
     */
    public Integer getMaxResults() {
        return maxResults;
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
        DescribeInsightRulesInput that = (DescribeInsightRulesInput) other;
        return Objects.equals(this.maxResults, that.maxResults)
               && Objects.equals(this.nextToken, that.nextToken);
    }

    @Override
    public int hashCode() {
        int $hc = Objects.hashCode(nextToken);
        $hc = 31 * $hc + Objects.hashCode(maxResults);
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
        if (maxResults != null) {
            serializer.writeInteger($SCHEMA_MAX_RESULTS, maxResults);
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T getMemberValue(Schema member) {
        return switch (member.memberIndex()) {
            case 0 -> (T) SchemaUtils.validateSameMember($SCHEMA_NEXT_TOKEN, member, nextToken);
            case 1 -> (T) SchemaUtils.validateSameMember($SCHEMA_MAX_RESULTS, member, maxResults);
            default -> throw new IllegalArgumentException("Attempted to get non-existent member: " + member.id());
        };
    }

    /**
     * Create a new builder containing all the current property values of this object.
     *
     * <p><strong>Note:</strong> This method performs only a shallow copy of the original properties.
     *
     * @return a builder for {@link DescribeInsightRulesInput}.
     */
    public Builder toBuilder() {
        var builder = new Builder();
        builder.nextToken(this.nextToken);
        builder.maxResults(this.maxResults);
        return builder;
    }

    /**
     * @return returns a new Builder.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for {@link DescribeInsightRulesInput}.
     */
    public static final class Builder implements ShapeBuilder<DescribeInsightRulesInput> {
        private String nextToken;
        private Integer maxResults;

        private Builder() {}

        @Override
        public Schema schema() {
            return $SCHEMA;
        }

        /**
         * Include this value, if it was returned by the previous operation, to get the next set of rules.
         *
         * @return this builder.
         */
        public Builder nextToken(String nextToken) {
            this.nextToken = nextToken;
            return this;
        }

        /**
         * The maximum number of results to return in one operation. If you omit this parameter, the default of 500 is used.
         *
         * @return this builder.
         */
        public Builder maxResults(Integer maxResults) {
            this.maxResults = maxResults;
            return this;
        }

        @Override
        public DescribeInsightRulesInput build() {
            return new DescribeInsightRulesInput(this);
        }

        @Override
        @SuppressWarnings("unchecked")
        public void setMemberValue(Schema member, Object value) {
            switch (member.memberIndex()) {
                case 0 -> nextToken((String) SchemaUtils.validateSameMember($SCHEMA_NEXT_TOKEN, member, value));
                case 1 -> maxResults((Integer) SchemaUtils.validateSameMember($SCHEMA_MAX_RESULTS, member, value));
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
                    case 1 -> builder.maxResults(de.readInteger(member));
                    default -> throw new IllegalArgumentException("Unexpected member: " + member.memberName());
                }
            }
        }
    }
}
