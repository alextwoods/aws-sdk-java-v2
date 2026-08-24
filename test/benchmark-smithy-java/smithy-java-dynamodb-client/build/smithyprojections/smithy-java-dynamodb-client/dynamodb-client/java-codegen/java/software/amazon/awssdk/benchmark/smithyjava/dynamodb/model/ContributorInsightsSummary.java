package software.amazon.awssdk.benchmark.smithyjava.dynamodb.model;

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
 * Represents a Contributor Insights summary entry.
 */
@SmithyGenerated
public final class ContributorInsightsSummary implements SerializableStruct {

    public static final Schema $SCHEMA = Schemas.CONTRIBUTOR_INSIGHTS_SUMMARY;
    private static final Schema $SCHEMA_TABLE_NAME = $SCHEMA.member("TableName");
    private static final Schema $SCHEMA_INDEX_NAME = $SCHEMA.member("IndexName");
    private static final Schema $SCHEMA_CONTRIBUTOR_INSIGHTS_STATUS = $SCHEMA.member("ContributorInsightsStatus");
    private static final Schema $SCHEMA_CONTRIBUTOR_INSIGHTS_MODE = $SCHEMA.member("ContributorInsightsMode");

    public static final ShapeId $ID = $SCHEMA.id();

    private final transient String tableName;
    private final transient String indexName;
    private final transient ContributorInsightsStatus contributorInsightsStatus;
    private final transient ContributorInsightsMode contributorInsightsMode;

    private ContributorInsightsSummary(Builder builder) {
        this.tableName = builder.tableName;
        this.indexName = builder.indexName;
        this.contributorInsightsStatus = builder.contributorInsightsStatus;
        this.contributorInsightsMode = builder.contributorInsightsMode;
    }

    /**
     * Name of the table associated with the summary.
     */
    public String getTableName() {
        return tableName;
    }

    /**
     * Name of the index associated with the summary, if any.
     */
    public String getIndexName() {
        return indexName;
    }

    /**
     * Describes the current status for contributor insights for the given table and index, if applicable.
     */
    public ContributorInsightsStatus getContributorInsightsStatus() {
        return contributorInsightsStatus;
    }

    /**
     * Indicates the current mode of CloudWatch Contributor Insights, specifying whether it tracks all access and
     * throttled events or throttled events only for the DynamoDB table or index.
     */
    public ContributorInsightsMode getContributorInsightsMode() {
        return contributorInsightsMode;
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
        ContributorInsightsSummary that = (ContributorInsightsSummary) other;
        return Objects.equals(this.tableName, that.tableName)
               && Objects.equals(this.indexName, that.indexName)
               && Objects.equals(this.contributorInsightsStatus, that.contributorInsightsStatus)
               && Objects.equals(this.contributorInsightsMode, that.contributorInsightsMode);
    }

    @Override
    public int hashCode() {
        int $hc = Objects.hashCode(tableName);
        $hc = 31 * $hc + Objects.hashCode(indexName);
        $hc = 31 * $hc + Objects.hashCode(contributorInsightsStatus);
        $hc = 31 * $hc + Objects.hashCode(contributorInsightsMode);
        return $hc;
    }

    @Override
    public Schema schema() {
        return $SCHEMA;
    }

    @Override
    public void serializeMembers(ShapeSerializer serializer) {
        if (tableName != null) {
            serializer.writeString($SCHEMA_TABLE_NAME, tableName);
        }
        if (indexName != null) {
            serializer.writeString($SCHEMA_INDEX_NAME, indexName);
        }
        if (contributorInsightsStatus != null) {
            serializer.writeString($SCHEMA_CONTRIBUTOR_INSIGHTS_STATUS, contributorInsightsStatus.getValue());
        }
        if (contributorInsightsMode != null) {
            serializer.writeString($SCHEMA_CONTRIBUTOR_INSIGHTS_MODE, contributorInsightsMode.getValue());
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T getMemberValue(Schema member) {
        return switch (member.memberIndex()) {
            case 0 -> (T) SchemaUtils.validateSameMember($SCHEMA_TABLE_NAME, member, tableName);
            case 1 -> (T) SchemaUtils.validateSameMember($SCHEMA_INDEX_NAME, member, indexName);
            case 2 -> (T) SchemaUtils.validateSameMember($SCHEMA_CONTRIBUTOR_INSIGHTS_STATUS, member, contributorInsightsStatus);
            case 3 -> (T) SchemaUtils.validateSameMember($SCHEMA_CONTRIBUTOR_INSIGHTS_MODE, member, contributorInsightsMode);
            default -> throw new IllegalArgumentException("Attempted to get non-existent member: " + member.id());
        };
    }

    /**
     * Create a new builder containing all the current property values of this object.
     *
     * <p><strong>Note:</strong> This method performs only a shallow copy of the original properties.
     *
     * @return a builder for {@link ContributorInsightsSummary}.
     */
    public Builder toBuilder() {
        var builder = new Builder();
        builder.tableName(this.tableName);
        builder.indexName(this.indexName);
        builder.contributorInsightsStatus(this.contributorInsightsStatus);
        builder.contributorInsightsMode(this.contributorInsightsMode);
        return builder;
    }

    /**
     * @return returns a new Builder.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for {@link ContributorInsightsSummary}.
     */
    public static final class Builder implements ShapeBuilder<ContributorInsightsSummary> {
        private String tableName;
        private String indexName;
        private ContributorInsightsStatus contributorInsightsStatus;
        private ContributorInsightsMode contributorInsightsMode;

        private Builder() {}

        @Override
        public Schema schema() {
            return $SCHEMA;
        }

        /**
         * Name of the table associated with the summary.
         *
         * @return this builder.
         */
        public Builder tableName(String tableName) {
            this.tableName = tableName;
            return this;
        }

        /**
         * Name of the index associated with the summary, if any.
         *
         * @return this builder.
         */
        public Builder indexName(String indexName) {
            this.indexName = indexName;
            return this;
        }

        /**
         * Describes the current status for contributor insights for the given table and index, if applicable.
         *
         * @return this builder.
         */
        public Builder contributorInsightsStatus(ContributorInsightsStatus contributorInsightsStatus) {
            this.contributorInsightsStatus = contributorInsightsStatus;
            return this;
        }

        /**
         * Indicates the current mode of CloudWatch Contributor Insights, specifying whether it tracks all access and
         * throttled events or throttled events only for the DynamoDB table or index.
         *
         * @return this builder.
         */
        public Builder contributorInsightsMode(ContributorInsightsMode contributorInsightsMode) {
            this.contributorInsightsMode = contributorInsightsMode;
            return this;
        }

        @Override
        public ContributorInsightsSummary build() {
            return new ContributorInsightsSummary(this);
        }

        @Override
        @SuppressWarnings("unchecked")
        public void setMemberValue(Schema member, Object value) {
            switch (member.memberIndex()) {
                case 0 -> tableName((String) SchemaUtils.validateSameMember($SCHEMA_TABLE_NAME, member, value));
                case 1 -> indexName((String) SchemaUtils.validateSameMember($SCHEMA_INDEX_NAME, member, value));
                case 2 -> contributorInsightsStatus((ContributorInsightsStatus) SchemaUtils.validateSameMember($SCHEMA_CONTRIBUTOR_INSIGHTS_STATUS, member, value));
                case 3 -> contributorInsightsMode((ContributorInsightsMode) SchemaUtils.validateSameMember($SCHEMA_CONTRIBUTOR_INSIGHTS_MODE, member, value));
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
                    case 0 -> builder.tableName(de.readString(member));
                    case 1 -> builder.indexName(de.readString(member));
                    case 2 -> builder.contributorInsightsStatus(ContributorInsightsStatus.builder().deserializeMember(de, member).build());
                    case 3 -> builder.contributorInsightsMode(ContributorInsightsMode.builder().deserializeMember(de, member).build());
                    default -> throw new IllegalArgumentException("Unexpected member: " + member.memberName());
                }
            }
        }
    }
}
