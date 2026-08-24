package software.amazon.awssdk.benchmark.smithyjava.s3.model;

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
public final class ListBucketIntelligentTieringConfigurationsOutput implements SerializableStruct {

    public static final Schema $SCHEMA = Schemas4.LIST_BUCKET_INTELLIGENT_TIERING_CONFIGURATIONS_OUTPUT;
    private static final Schema $SCHEMA_IS_TRUNCATED = $SCHEMA.member("IsTruncated");
    private static final Schema $SCHEMA_CONTINUATION_TOKEN = $SCHEMA.member("ContinuationToken");
    private static final Schema $SCHEMA_NEXT_CONTINUATION_TOKEN = $SCHEMA.member("NextContinuationToken");
    private static final Schema $SCHEMA_INTELLIGENT_TIERING_CONFIGURATION_LIST = $SCHEMA.member("IntelligentTieringConfigurationList");

    public static final ShapeId $ID = $SCHEMA.id();

    private final transient Boolean isTruncated;
    private final transient String continuationToken;
    private final transient String nextContinuationToken;
    private final transient List<IntelligentTieringConfiguration> intelligentTieringConfigurationList;

    private ListBucketIntelligentTieringConfigurationsOutput(Builder builder) {
        this.isTruncated = builder.isTruncated;
        this.continuationToken = builder.continuationToken;
        this.nextContinuationToken = builder.nextContinuationToken;
        this.intelligentTieringConfigurationList = builder.intelligentTieringConfigurationList == null ? null : Collections.unmodifiableList(builder.intelligentTieringConfigurationList);
    }

    /**
     * Indicates whether the returned list of analytics configurations is complete. A value of <code>true</code>
     * indicates that the list is not complete and the <code>NextContinuationToken</code> will be provided for a
     * subsequent request.
     */
    public Boolean isIsTruncated() {
        return isTruncated;
    }

    /**
     * The <code>ContinuationToken</code> that represents a placeholder from where this request should begin.
     */
    public String getContinuationToken() {
        return continuationToken;
    }

    /**
     * The marker used to continue this inventory configuration listing. Use the <code>NextContinuationToken</code> from
     * this response to continue the listing in a subsequent request. The continuation token is an opaque value that
     * Amazon S3 understands.
     */
    public String getNextContinuationToken() {
        return nextContinuationToken;
    }

    /**
     * The list of S3 Intelligent-Tiering configurations for a bucket.
     */
    public List<IntelligentTieringConfiguration> getIntelligentTieringConfigurationList() {
        if (intelligentTieringConfigurationList == null) {
            return Collections.emptyList();
        }
        return intelligentTieringConfigurationList;
    }

    public boolean hasIntelligentTieringConfigurationList() {
        return intelligentTieringConfigurationList != null;
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
        ListBucketIntelligentTieringConfigurationsOutput that = (ListBucketIntelligentTieringConfigurationsOutput) other;
        return Objects.equals(this.isTruncated, that.isTruncated)
               && Objects.equals(this.continuationToken, that.continuationToken)
               && Objects.equals(this.nextContinuationToken, that.nextContinuationToken)
               && Objects.equals(this.intelligentTieringConfigurationList, that.intelligentTieringConfigurationList);
    }

    @Override
    public int hashCode() {
        int $hc = Objects.hashCode(isTruncated);
        $hc = 31 * $hc + Objects.hashCode(continuationToken);
        $hc = 31 * $hc + Objects.hashCode(nextContinuationToken);
        $hc = 31 * $hc + Objects.hashCode(intelligentTieringConfigurationList);
        return $hc;
    }

    @Override
    public Schema schema() {
        return $SCHEMA;
    }

    @Override
    public void serializeMembers(ShapeSerializer serializer) {
        if (isTruncated != null) {
            serializer.writeBoolean($SCHEMA_IS_TRUNCATED, isTruncated);
        }
        if (continuationToken != null) {
            serializer.writeString($SCHEMA_CONTINUATION_TOKEN, continuationToken);
        }
        if (nextContinuationToken != null) {
            serializer.writeString($SCHEMA_NEXT_CONTINUATION_TOKEN, nextContinuationToken);
        }
        if (intelligentTieringConfigurationList != null) {
            serializer.writeList($SCHEMA_INTELLIGENT_TIERING_CONFIGURATION_LIST, intelligentTieringConfigurationList, intelligentTieringConfigurationList.size(), SharedSerde.IntelligentTieringConfigurationListSerializer.INSTANCE);
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T getMemberValue(Schema member) {
        return switch (member.memberIndex()) {
            case 0 -> (T) SchemaUtils.validateSameMember($SCHEMA_IS_TRUNCATED, member, isTruncated);
            case 1 -> (T) SchemaUtils.validateSameMember($SCHEMA_CONTINUATION_TOKEN, member, continuationToken);
            case 2 -> (T) SchemaUtils.validateSameMember($SCHEMA_NEXT_CONTINUATION_TOKEN, member, nextContinuationToken);
            case 3 -> (T) SchemaUtils.validateSameMember($SCHEMA_INTELLIGENT_TIERING_CONFIGURATION_LIST, member, intelligentTieringConfigurationList);
            default -> throw new IllegalArgumentException("Attempted to get non-existent member: " + member.id());
        };
    }

    /**
     * Create a new builder containing all the current property values of this object.
     *
     * <p><strong>Note:</strong> This method performs only a shallow copy of the original properties.
     *
     * @return a builder for {@link ListBucketIntelligentTieringConfigurationsOutput}.
     */
    public Builder toBuilder() {
        var builder = new Builder();
        builder.isTruncated(this.isTruncated);
        builder.continuationToken(this.continuationToken);
        builder.nextContinuationToken(this.nextContinuationToken);
        builder.intelligentTieringConfigurationList(this.intelligentTieringConfigurationList);
        return builder;
    }

    /**
     * @return returns a new Builder.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for {@link ListBucketIntelligentTieringConfigurationsOutput}.
     */
    public static final class Builder implements ShapeBuilder<ListBucketIntelligentTieringConfigurationsOutput> {
        private Boolean isTruncated;
        private String continuationToken;
        private String nextContinuationToken;
        private List<IntelligentTieringConfiguration> intelligentTieringConfigurationList;

        private Builder() {}

        @Override
        public Schema schema() {
            return $SCHEMA;
        }

        /**
         * Indicates whether the returned list of analytics configurations is complete. A value of <code>true</code>
         * indicates that the list is not complete and the <code>NextContinuationToken</code> will be provided for a
         * subsequent request.
         *
         * @return this builder.
         */
        public Builder isTruncated(Boolean isTruncated) {
            this.isTruncated = isTruncated;
            return this;
        }

        /**
         * The <code>ContinuationToken</code> that represents a placeholder from where this request should begin.
         *
         * @return this builder.
         */
        public Builder continuationToken(String continuationToken) {
            this.continuationToken = continuationToken;
            return this;
        }

        /**
         * The marker used to continue this inventory configuration listing. Use the <code>NextContinuationToken</code> from
         * this response to continue the listing in a subsequent request. The continuation token is an opaque value that
         * Amazon S3 understands.
         *
         * @return this builder.
         */
        public Builder nextContinuationToken(String nextContinuationToken) {
            this.nextContinuationToken = nextContinuationToken;
            return this;
        }

        /**
         * The list of S3 Intelligent-Tiering configurations for a bucket.
         *
         * @return this builder.
         */
        public Builder intelligentTieringConfigurationList(List<IntelligentTieringConfiguration> intelligentTieringConfigurationList) {
            this.intelligentTieringConfigurationList = intelligentTieringConfigurationList;
            return this;
        }

        @Override
        public ListBucketIntelligentTieringConfigurationsOutput build() {
            return new ListBucketIntelligentTieringConfigurationsOutput(this);
        }

        @Override
        @SuppressWarnings("unchecked")
        public void setMemberValue(Schema member, Object value) {
            switch (member.memberIndex()) {
                case 0 -> isTruncated((Boolean) SchemaUtils.validateSameMember($SCHEMA_IS_TRUNCATED, member, value));
                case 1 -> continuationToken((String) SchemaUtils.validateSameMember($SCHEMA_CONTINUATION_TOKEN, member, value));
                case 2 -> nextContinuationToken((String) SchemaUtils.validateSameMember($SCHEMA_NEXT_CONTINUATION_TOKEN, member, value));
                case 3 -> intelligentTieringConfigurationList((List<IntelligentTieringConfiguration>) SchemaUtils.validateSameMember($SCHEMA_INTELLIGENT_TIERING_CONFIGURATION_LIST, member, value));
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
                    case 0 -> builder.isTruncated(de.readBoolean(member));
                    case 1 -> builder.continuationToken(de.readString(member));
                    case 2 -> builder.nextContinuationToken(de.readString(member));
                    case 3 -> builder.intelligentTieringConfigurationList(SharedSerde.deserializeIntelligentTieringConfigurationList(member, de));
                    default -> throw new IllegalArgumentException("Unexpected member: " + member.memberName());
                }
            }
        }
    }
}
