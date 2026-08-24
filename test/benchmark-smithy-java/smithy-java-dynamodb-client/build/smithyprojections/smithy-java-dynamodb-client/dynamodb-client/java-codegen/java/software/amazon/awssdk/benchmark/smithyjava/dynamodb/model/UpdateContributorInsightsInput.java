package software.amazon.awssdk.benchmark.smithyjava.dynamodb.model;

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
public final class UpdateContributorInsightsInput implements SerializableStruct {

    public static final Schema $SCHEMA = Schemas.UPDATE_CONTRIBUTOR_INSIGHTS_INPUT;
    private static final Schema $SCHEMA_TABLE_NAME = $SCHEMA.member("TableName");
    private static final Schema $SCHEMA_INDEX_NAME = $SCHEMA.member("IndexName");
    private static final Schema $SCHEMA_CONTRIBUTOR_INSIGHTS_ACTION = $SCHEMA.member("ContributorInsightsAction");
    private static final Schema $SCHEMA_CONTRIBUTOR_INSIGHTS_MODE = $SCHEMA.member("ContributorInsightsMode");

    public static final ShapeId $ID = $SCHEMA.id();

    private final transient String tableName;
    private final transient String indexName;
    private final transient ContributorInsightsAction contributorInsightsAction;
    private final transient ContributorInsightsMode contributorInsightsMode;

    private UpdateContributorInsightsInput(Builder builder) {
        this.tableName = builder.tableName;
        this.indexName = builder.indexName;
        this.contributorInsightsAction = builder.contributorInsightsAction;
        this.contributorInsightsMode = builder.contributorInsightsMode;
    }

    /**
     * The name of the table. You can also provide the Amazon Resource Name (ARN) of the table in this parameter.
     */
    public String getTableName() {
        return tableName;
    }

    /**
     * The global secondary index name, if applicable.
     */
    public String getIndexName() {
        return indexName;
    }

    /**
     * Represents the contributor insights action.
     */
    public ContributorInsightsAction getContributorInsightsAction() {
        return contributorInsightsAction;
    }

    /**
     * Specifies whether to track all access and throttled events or throttled events only for the DynamoDB table or
     * index.
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
        UpdateContributorInsightsInput that = (UpdateContributorInsightsInput) other;
        return Objects.equals(this.tableName, that.tableName)
               && Objects.equals(this.indexName, that.indexName)
               && Objects.equals(this.contributorInsightsAction, that.contributorInsightsAction)
               && Objects.equals(this.contributorInsightsMode, that.contributorInsightsMode);
    }

    @Override
    public int hashCode() {
        int $hc = Objects.hashCode(tableName);
        $hc = 31 * $hc + Objects.hashCode(indexName);
        $hc = 31 * $hc + Objects.hashCode(contributorInsightsAction);
        $hc = 31 * $hc + Objects.hashCode(contributorInsightsMode);
        return $hc;
    }

    @Override
    public Schema schema() {
        return $SCHEMA;
    }

    @Override
    public void serializeMembers(ShapeSerializer serializer) {
        serializer.writeString($SCHEMA_TABLE_NAME, tableName);
        if (indexName != null) {
            serializer.writeString($SCHEMA_INDEX_NAME, indexName);
        }
        serializer.writeString($SCHEMA_CONTRIBUTOR_INSIGHTS_ACTION, contributorInsightsAction.getValue());
        if (contributorInsightsMode != null) {
            serializer.writeString($SCHEMA_CONTRIBUTOR_INSIGHTS_MODE, contributorInsightsMode.getValue());
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T getMemberValue(Schema member) {
        return switch (member.memberIndex()) {
            case 0 -> (T) SchemaUtils.validateSameMember($SCHEMA_TABLE_NAME, member, tableName);
            case 1 -> (T) SchemaUtils.validateSameMember($SCHEMA_CONTRIBUTOR_INSIGHTS_ACTION, member, contributorInsightsAction);
            case 2 -> (T) SchemaUtils.validateSameMember($SCHEMA_INDEX_NAME, member, indexName);
            case 3 -> (T) SchemaUtils.validateSameMember($SCHEMA_CONTRIBUTOR_INSIGHTS_MODE, member, contributorInsightsMode);
            default -> throw new IllegalArgumentException("Attempted to get non-existent member: " + member.id());
        };
    }

    /**
     * Create a new builder containing all the current property values of this object.
     *
     * <p><strong>Note:</strong> This method performs only a shallow copy of the original properties.
     *
     * @return a builder for {@link UpdateContributorInsightsInput}.
     */
    public Builder toBuilder() {
        var builder = new Builder();
        builder.tableName(this.tableName);
        builder.indexName(this.indexName);
        builder.contributorInsightsAction(this.contributorInsightsAction);
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
     * Builder for {@link UpdateContributorInsightsInput}.
     */
    public static final class Builder implements ShapeBuilder<UpdateContributorInsightsInput> {
        private final PresenceTracker tracker = PresenceTracker.of($SCHEMA);
        private String tableName;
        private String indexName;
        private ContributorInsightsAction contributorInsightsAction;
        private ContributorInsightsMode contributorInsightsMode;

        private Builder() {}

        @Override
        public Schema schema() {
            return $SCHEMA;
        }

        /**
         * The name of the table. You can also provide the Amazon Resource Name (ARN) of the table in this parameter.
         *
         * <p><strong>Required</strong>
         * @return this builder.
         */
        public Builder tableName(String tableName) {
            this.tableName = Objects.requireNonNull(tableName, "tableName cannot be null");
            tracker.setMember($SCHEMA_TABLE_NAME);
            return this;
        }

        /**
         * The global secondary index name, if applicable.
         *
         * @return this builder.
         */
        public Builder indexName(String indexName) {
            this.indexName = indexName;
            return this;
        }

        /**
         * Represents the contributor insights action.
         *
         * <p><strong>Required</strong>
         * @return this builder.
         */
        public Builder contributorInsightsAction(ContributorInsightsAction contributorInsightsAction) {
            this.contributorInsightsAction = Objects.requireNonNull(contributorInsightsAction, "contributorInsightsAction cannot be null");
            tracker.setMember($SCHEMA_CONTRIBUTOR_INSIGHTS_ACTION);
            return this;
        }

        /**
         * Specifies whether to track all access and throttled events or throttled events only for the DynamoDB table or
         * index.
         *
         * @return this builder.
         */
        public Builder contributorInsightsMode(ContributorInsightsMode contributorInsightsMode) {
            this.contributorInsightsMode = contributorInsightsMode;
            return this;
        }

        @Override
        public UpdateContributorInsightsInput build() {
            tracker.validate();
            return new UpdateContributorInsightsInput(this);
        }

        @Override
        @SuppressWarnings("unchecked")
        public void setMemberValue(Schema member, Object value) {
            switch (member.memberIndex()) {
                case 0 -> tableName((String) SchemaUtils.validateSameMember($SCHEMA_TABLE_NAME, member, value));
                case 1 -> contributorInsightsAction((ContributorInsightsAction) SchemaUtils.validateSameMember($SCHEMA_CONTRIBUTOR_INSIGHTS_ACTION, member, value));
                case 2 -> indexName((String) SchemaUtils.validateSameMember($SCHEMA_INDEX_NAME, member, value));
                case 3 -> contributorInsightsMode((ContributorInsightsMode) SchemaUtils.validateSameMember($SCHEMA_CONTRIBUTOR_INSIGHTS_MODE, member, value));
                default -> ShapeBuilder.super.setMemberValue(member, value);
            }
        }

        @Override
        public ShapeBuilder<UpdateContributorInsightsInput> errorCorrection() {
            if (tracker.allSet()) {
                return this;
            }
            if (!tracker.checkMember($SCHEMA_TABLE_NAME)) {
                tableName("");
            }
            if (!tracker.checkMember($SCHEMA_CONTRIBUTOR_INSIGHTS_ACTION)) {
                contributorInsightsAction(ContributorInsightsAction.unknown(""));
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
                    case 0 -> builder.tableName(de.readString(member));
                    case 1 -> builder.contributorInsightsAction(ContributorInsightsAction.builder().deserializeMember(de, member).build());
                    case 2 -> builder.indexName(de.readString(member));
                    case 3 -> builder.contributorInsightsMode(ContributorInsightsMode.builder().deserializeMember(de, member).build());
                    default -> throw new IllegalArgumentException("Unexpected member: " + member.memberName());
                }
            }
        }
    }
}
