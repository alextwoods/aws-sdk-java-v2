package software.amazon.awssdk.benchmark.smithyjava.dynamodb.model;

import java.time.Instant;
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
public final class DescribeContributorInsightsOutput implements SerializableStruct {

    public static final Schema $SCHEMA = Schemas.DESCRIBE_CONTRIBUTOR_INSIGHTS_OUTPUT;
    private static final Schema $SCHEMA_TABLE_NAME = $SCHEMA.member("TableName");
    private static final Schema $SCHEMA_INDEX_NAME = $SCHEMA.member("IndexName");
    private static final Schema $SCHEMA_CONTRIBUTOR_INSIGHTS_RULE_LIST = $SCHEMA.member("ContributorInsightsRuleList");
    private static final Schema $SCHEMA_CONTRIBUTOR_INSIGHTS_STATUS = $SCHEMA.member("ContributorInsightsStatus");
    private static final Schema $SCHEMA_LAST_UPDATE_DATE_TIME = $SCHEMA.member("LastUpdateDateTime");
    private static final Schema $SCHEMA_FAILURE_EXCEPTION = $SCHEMA.member("FailureException");
    private static final Schema $SCHEMA_CONTRIBUTOR_INSIGHTS_MODE = $SCHEMA.member("ContributorInsightsMode");

    public static final ShapeId $ID = $SCHEMA.id();

    private final transient String tableName;
    private final transient String indexName;
    private final transient List<String> contributorInsightsRuleList;
    private final transient ContributorInsightsStatus contributorInsightsStatus;
    private final transient Instant lastUpdateDateTime;
    private final transient FailureException failureException;
    private final transient ContributorInsightsMode contributorInsightsMode;

    private DescribeContributorInsightsOutput(Builder builder) {
        this.tableName = builder.tableName;
        this.indexName = builder.indexName;
        this.contributorInsightsRuleList = builder.contributorInsightsRuleList == null ? null : Collections.unmodifiableList(builder.contributorInsightsRuleList);
        this.contributorInsightsStatus = builder.contributorInsightsStatus;
        this.lastUpdateDateTime = builder.lastUpdateDateTime;
        this.failureException = builder.failureException;
        this.contributorInsightsMode = builder.contributorInsightsMode;
    }

    /**
     * The name of the table being described.
     */
    public String getTableName() {
        return tableName;
    }

    /**
     * The name of the global secondary index being described.
     */
    public String getIndexName() {
        return indexName;
    }

    /**
     * List of names of the associated contributor insights rules.
     */
    public List<String> getContributorInsightsRuleList() {
        if (contributorInsightsRuleList == null) {
            return Collections.emptyList();
        }
        return contributorInsightsRuleList;
    }

    public boolean hasContributorInsightsRuleList() {
        return contributorInsightsRuleList != null;
    }

    /**
     * Current status of contributor insights.
     */
    public ContributorInsightsStatus getContributorInsightsStatus() {
        return contributorInsightsStatus;
    }

    /**
     * Timestamp of the last time the status was changed.
     */
    public Instant getLastUpdateDateTime() {
        return lastUpdateDateTime;
    }

    /**
     * Returns information about the last failure that was encountered.
     *
     * <p>The most common exceptions for a FAILED status are:
     *
     * <ul>
     *   <li>
     *     LimitExceededException - Per-account Amazon CloudWatch Contributor Insights rule limit reached. Please
     *     disable Contributor Insights for other tables/indexes OR disable Contributor Insights rules before
     *     retrying.
     *   </li>
     *   <li>
     *     AccessDeniedException - Amazon CloudWatch Contributor Insights rules cannot be modified due to
     *     insufficient permissions.
     *   </li>
     *   <li>
     *     AccessDeniedException - Failed to create service-linked role for Contributor Insights due to insufficient
     *     permissions.
     *   </li>
     *   <li>
     *     InternalServerError - Failed to create Amazon CloudWatch Contributor Insights rules. Please retry
     *     request.
     *   </li>
     * </ul>
     */
    public FailureException getFailureException() {
        return failureException;
    }

    /**
     * The mode of CloudWatch Contributor Insights for DynamoDB that determines which events are emitted. Can be set to
     * track all access and throttled events or throttled events only.
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
        DescribeContributorInsightsOutput that = (DescribeContributorInsightsOutput) other;
        return Objects.equals(this.tableName, that.tableName)
               && Objects.equals(this.indexName, that.indexName)
               && Objects.equals(this.contributorInsightsStatus, that.contributorInsightsStatus)
               && Objects.equals(this.contributorInsightsMode, that.contributorInsightsMode)
               && Objects.equals(this.lastUpdateDateTime, that.lastUpdateDateTime)
               && Objects.equals(this.failureException, that.failureException)
               && Objects.equals(this.contributorInsightsRuleList, that.contributorInsightsRuleList);
    }

    @Override
    public int hashCode() {
        int $hc = Objects.hashCode(tableName);
        $hc = 31 * $hc + Objects.hashCode(indexName);
        $hc = 31 * $hc + Objects.hashCode(contributorInsightsRuleList);
        $hc = 31 * $hc + Objects.hashCode(contributorInsightsStatus);
        $hc = 31 * $hc + Objects.hashCode(lastUpdateDateTime);
        $hc = 31 * $hc + Objects.hashCode(failureException);
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
        if (contributorInsightsRuleList != null) {
            serializer.writeList($SCHEMA_CONTRIBUTOR_INSIGHTS_RULE_LIST, contributorInsightsRuleList, contributorInsightsRuleList.size(), SharedSerde.ContributorInsightsRuleListSerializer.INSTANCE);
        }
        if (contributorInsightsStatus != null) {
            serializer.writeString($SCHEMA_CONTRIBUTOR_INSIGHTS_STATUS, contributorInsightsStatus.getValue());
        }
        if (lastUpdateDateTime != null) {
            serializer.writeTimestamp($SCHEMA_LAST_UPDATE_DATE_TIME, lastUpdateDateTime);
        }
        if (failureException != null) {
            serializer.writeStruct($SCHEMA_FAILURE_EXCEPTION, failureException);
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
            case 2 -> (T) SchemaUtils.validateSameMember($SCHEMA_CONTRIBUTOR_INSIGHTS_RULE_LIST, member, contributorInsightsRuleList);
            case 3 -> (T) SchemaUtils.validateSameMember($SCHEMA_CONTRIBUTOR_INSIGHTS_STATUS, member, contributorInsightsStatus);
            case 4 -> (T) SchemaUtils.validateSameMember($SCHEMA_LAST_UPDATE_DATE_TIME, member, lastUpdateDateTime);
            case 5 -> (T) SchemaUtils.validateSameMember($SCHEMA_FAILURE_EXCEPTION, member, failureException);
            case 6 -> (T) SchemaUtils.validateSameMember($SCHEMA_CONTRIBUTOR_INSIGHTS_MODE, member, contributorInsightsMode);
            default -> throw new IllegalArgumentException("Attempted to get non-existent member: " + member.id());
        };
    }

    /**
     * Create a new builder containing all the current property values of this object.
     *
     * <p><strong>Note:</strong> This method performs only a shallow copy of the original properties.
     *
     * @return a builder for {@link DescribeContributorInsightsOutput}.
     */
    public Builder toBuilder() {
        var builder = new Builder();
        builder.tableName(this.tableName);
        builder.indexName(this.indexName);
        builder.contributorInsightsRuleList(this.contributorInsightsRuleList);
        builder.contributorInsightsStatus(this.contributorInsightsStatus);
        builder.lastUpdateDateTime(this.lastUpdateDateTime);
        builder.failureException(this.failureException);
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
     * Builder for {@link DescribeContributorInsightsOutput}.
     */
    public static final class Builder implements ShapeBuilder<DescribeContributorInsightsOutput> {
        private String tableName;
        private String indexName;
        private List<String> contributorInsightsRuleList;
        private ContributorInsightsStatus contributorInsightsStatus;
        private Instant lastUpdateDateTime;
        private FailureException failureException;
        private ContributorInsightsMode contributorInsightsMode;

        private Builder() {}

        @Override
        public Schema schema() {
            return $SCHEMA;
        }

        /**
         * The name of the table being described.
         *
         * @return this builder.
         */
        public Builder tableName(String tableName) {
            this.tableName = tableName;
            return this;
        }

        /**
         * The name of the global secondary index being described.
         *
         * @return this builder.
         */
        public Builder indexName(String indexName) {
            this.indexName = indexName;
            return this;
        }

        /**
         * List of names of the associated contributor insights rules.
         *
         * @return this builder.
         */
        public Builder contributorInsightsRuleList(List<String> contributorInsightsRuleList) {
            this.contributorInsightsRuleList = contributorInsightsRuleList;
            return this;
        }

        /**
         * Current status of contributor insights.
         *
         * @return this builder.
         */
        public Builder contributorInsightsStatus(ContributorInsightsStatus contributorInsightsStatus) {
            this.contributorInsightsStatus = contributorInsightsStatus;
            return this;
        }

        /**
         * Timestamp of the last time the status was changed.
         *
         * @return this builder.
         */
        public Builder lastUpdateDateTime(Instant lastUpdateDateTime) {
            this.lastUpdateDateTime = lastUpdateDateTime;
            return this;
        }

        /**
         * Returns information about the last failure that was encountered.
         *
         * <p>The most common exceptions for a FAILED status are:
         *
         * <ul>
         *   <li>
         *     LimitExceededException - Per-account Amazon CloudWatch Contributor Insights rule limit reached. Please
         *     disable Contributor Insights for other tables/indexes OR disable Contributor Insights rules before
         *     retrying.
         *   </li>
         *   <li>
         *     AccessDeniedException - Amazon CloudWatch Contributor Insights rules cannot be modified due to
         *     insufficient permissions.
         *   </li>
         *   <li>
         *     AccessDeniedException - Failed to create service-linked role for Contributor Insights due to insufficient
         *     permissions.
         *   </li>
         *   <li>
         *     InternalServerError - Failed to create Amazon CloudWatch Contributor Insights rules. Please retry
         *     request.
         *   </li>
         * </ul>
         *
         * @return this builder.
         */
        public Builder failureException(FailureException failureException) {
            this.failureException = failureException;
            return this;
        }

        /**
         * The mode of CloudWatch Contributor Insights for DynamoDB that determines which events are emitted. Can be set to
         * track all access and throttled events or throttled events only.
         *
         * @return this builder.
         */
        public Builder contributorInsightsMode(ContributorInsightsMode contributorInsightsMode) {
            this.contributorInsightsMode = contributorInsightsMode;
            return this;
        }

        @Override
        public DescribeContributorInsightsOutput build() {
            return new DescribeContributorInsightsOutput(this);
        }

        @Override
        @SuppressWarnings("unchecked")
        public void setMemberValue(Schema member, Object value) {
            switch (member.memberIndex()) {
                case 0 -> tableName((String) SchemaUtils.validateSameMember($SCHEMA_TABLE_NAME, member, value));
                case 1 -> indexName((String) SchemaUtils.validateSameMember($SCHEMA_INDEX_NAME, member, value));
                case 2 -> contributorInsightsRuleList((List<String>) SchemaUtils.validateSameMember($SCHEMA_CONTRIBUTOR_INSIGHTS_RULE_LIST, member, value));
                case 3 -> contributorInsightsStatus((ContributorInsightsStatus) SchemaUtils.validateSameMember($SCHEMA_CONTRIBUTOR_INSIGHTS_STATUS, member, value));
                case 4 -> lastUpdateDateTime((Instant) SchemaUtils.validateSameMember($SCHEMA_LAST_UPDATE_DATE_TIME, member, value));
                case 5 -> failureException((FailureException) SchemaUtils.validateSameMember($SCHEMA_FAILURE_EXCEPTION, member, value));
                case 6 -> contributorInsightsMode((ContributorInsightsMode) SchemaUtils.validateSameMember($SCHEMA_CONTRIBUTOR_INSIGHTS_MODE, member, value));
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
                    case 2 -> builder.contributorInsightsRuleList(SharedSerde.deserializeContributorInsightsRuleList(member, de));
                    case 3 -> builder.contributorInsightsStatus(ContributorInsightsStatus.builder().deserializeMember(de, member).build());
                    case 4 -> builder.lastUpdateDateTime(de.readTimestamp(member));
                    case 5 -> builder.failureException(FailureException.builder().deserializeMember(de, member).build());
                    case 6 -> builder.contributorInsightsMode(ContributorInsightsMode.builder().deserializeMember(de, member).build());
                    default -> throw new IllegalArgumentException("Unexpected member: " + member.memberName());
                }
            }
        }
    }
}
