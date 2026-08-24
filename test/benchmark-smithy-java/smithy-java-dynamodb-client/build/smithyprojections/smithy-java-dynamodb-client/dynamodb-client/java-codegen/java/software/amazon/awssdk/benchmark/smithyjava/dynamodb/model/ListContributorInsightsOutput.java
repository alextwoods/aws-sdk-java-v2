package software.amazon.awssdk.benchmark.smithyjava.dynamodb.model;

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
public final class ListContributorInsightsOutput implements SerializableStruct {

    public static final Schema $SCHEMA = Schemas.LIST_CONTRIBUTOR_INSIGHTS_OUTPUT;
    private static final Schema $SCHEMA_CONTRIBUTOR_INSIGHTS_SUMMARIES = $SCHEMA.member("ContributorInsightsSummaries");
    private static final Schema $SCHEMA_NEXT_TOKEN = $SCHEMA.member("NextToken");

    public static final ShapeId $ID = $SCHEMA.id();

    private final transient List<ContributorInsightsSummary> contributorInsightsSummaries;
    private final transient String nextToken;

    private ListContributorInsightsOutput(Builder builder) {
        this.contributorInsightsSummaries = builder.contributorInsightsSummaries == null ? null : Collections.unmodifiableList(builder.contributorInsightsSummaries);
        this.nextToken = builder.nextToken;
    }

    /**
     * A list of ContributorInsightsSummary.
     */
    public List<ContributorInsightsSummary> getContributorInsightsSummaries() {
        if (contributorInsightsSummaries == null) {
            return Collections.emptyList();
        }
        return contributorInsightsSummaries;
    }

    public boolean hasContributorInsightsSummaries() {
        return contributorInsightsSummaries != null;
    }

    /**
     * A token to go to the next page if there is one.
     */
    public String getNextToken() {
        return nextToken;
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
        ListContributorInsightsOutput that = (ListContributorInsightsOutput) other;
        return Objects.equals(this.nextToken, that.nextToken)
               && Objects.equals(this.contributorInsightsSummaries, that.contributorInsightsSummaries);
    }

    @Override
    public int hashCode() {
        int $hc = Objects.hashCode(contributorInsightsSummaries);
        $hc = 31 * $hc + Objects.hashCode(nextToken);
        return $hc;
    }

    @Override
    public Schema schema() {
        return $SCHEMA;
    }

    @Override
    public void serializeMembers(ShapeSerializer serializer) {
        if (contributorInsightsSummaries != null) {
            serializer.writeList($SCHEMA_CONTRIBUTOR_INSIGHTS_SUMMARIES, contributorInsightsSummaries, contributorInsightsSummaries.size(), SharedSerde.ContributorInsightsSummariesSerializer.INSTANCE);
        }
        if (nextToken != null) {
            serializer.writeString($SCHEMA_NEXT_TOKEN, nextToken);
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T getMemberValue(Schema member) {
        return switch (member.memberIndex()) {
            case 0 -> (T) SchemaUtils.validateSameMember($SCHEMA_CONTRIBUTOR_INSIGHTS_SUMMARIES, member, contributorInsightsSummaries);
            case 1 -> (T) SchemaUtils.validateSameMember($SCHEMA_NEXT_TOKEN, member, nextToken);
            default -> throw new IllegalArgumentException("Attempted to get non-existent member: " + member.id());
        };
    }

    /**
     * Create a new builder containing all the current property values of this object.
     *
     * <p><strong>Note:</strong> This method performs only a shallow copy of the original properties.
     *
     * @return a builder for {@link ListContributorInsightsOutput}.
     */
    public Builder toBuilder() {
        var builder = new Builder();
        builder.contributorInsightsSummaries(this.contributorInsightsSummaries);
        builder.nextToken(this.nextToken);
        return builder;
    }

    /**
     * @return returns a new Builder.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for {@link ListContributorInsightsOutput}.
     */
    public static final class Builder implements ShapeBuilder<ListContributorInsightsOutput> {
        private List<ContributorInsightsSummary> contributorInsightsSummaries;
        private String nextToken;

        private Builder() {}

        @Override
        public Schema schema() {
            return $SCHEMA;
        }

        /**
         * A list of ContributorInsightsSummary.
         *
         * @return this builder.
         */
        public Builder contributorInsightsSummaries(List<ContributorInsightsSummary> contributorInsightsSummaries) {
            this.contributorInsightsSummaries = contributorInsightsSummaries;
            return this;
        }

        /**
         * A token to go to the next page if there is one.
         *
         * @return this builder.
         */
        public Builder nextToken(String nextToken) {
            this.nextToken = nextToken;
            return this;
        }

        @Override
        public ListContributorInsightsOutput build() {
            return new ListContributorInsightsOutput(this);
        }

        @Override
        @SuppressWarnings("unchecked")
        public void setMemberValue(Schema member, Object value) {
            switch (member.memberIndex()) {
                case 0 -> contributorInsightsSummaries((List<ContributorInsightsSummary>) SchemaUtils.validateSameMember($SCHEMA_CONTRIBUTOR_INSIGHTS_SUMMARIES, member, value));
                case 1 -> nextToken((String) SchemaUtils.validateSameMember($SCHEMA_NEXT_TOKEN, member, value));
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
                    case 0 -> builder.contributorInsightsSummaries(SharedSerde.deserializeContributorInsightsSummaries(member, de));
                    case 1 -> builder.nextToken(de.readString(member));
                    default -> throw new IllegalArgumentException("Unexpected member: " + member.memberName());
                }
            }
        }
    }
}
