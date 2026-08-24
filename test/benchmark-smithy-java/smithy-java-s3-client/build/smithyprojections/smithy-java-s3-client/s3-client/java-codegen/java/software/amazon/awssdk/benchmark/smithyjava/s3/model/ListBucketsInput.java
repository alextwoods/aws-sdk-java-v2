package software.amazon.awssdk.benchmark.smithyjava.s3.model;

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
public final class ListBucketsInput implements SerializableStruct {

    public static final Schema $SCHEMA = Schemas4.LIST_BUCKETS_INPUT;
    private static final Schema $SCHEMA_MAX_BUCKETS = $SCHEMA.member("MaxBuckets");
    private static final Schema $SCHEMA_CONTINUATION_TOKEN = $SCHEMA.member("ContinuationToken");
    private static final Schema $SCHEMA_PREFIX = $SCHEMA.member("Prefix");
    private static final Schema $SCHEMA_BUCKET_REGION = $SCHEMA.member("BucketRegion");

    public static final ShapeId $ID = $SCHEMA.id();

    private final transient Integer maxBuckets;
    private final transient String continuationToken;
    private final transient String prefix;
    private final transient String bucketRegion;

    private ListBucketsInput(Builder builder) {
        this.maxBuckets = builder.maxBuckets;
        this.continuationToken = builder.continuationToken;
        this.prefix = builder.prefix;
        this.bucketRegion = builder.bucketRegion;
    }

    /**
     * Maximum number of buckets to be returned in response. When the number is more than the count of buckets that are
     * owned by an Amazon Web Services account, return all the buckets in response.
     */
    public Integer getMaxBuckets() {
        return maxBuckets;
    }

    /**
     * <code>ContinuationToken</code> indicates to Amazon S3 that the list is being continued on this bucket with a
     * token. <code>ContinuationToken</code> is obfuscated and is not a real key. You can use this <code>
     * ContinuationToken</code> for pagination of the list results.
     *
     * <p>Length Constraints: Minimum length of 0. Maximum length of 1024.
     *
     * <p>Required: No.
     *
     * <p>If you specify the <code>bucket-region</code>, <code>prefix</code>, or <code>continuation-token</code> query
     * parameters without using <code>max-buckets</code> to set the maximum number of buckets returned in the response,
     * Amazon S3 applies a default page size of 10,000 and provides a continuation token if there are more buckets.
     */
    public String getContinuationToken() {
        return continuationToken;
    }

    /**
     * Limits the response to bucket names that begin with the specified bucket name prefix.
     */
    public String getPrefix() {
        return prefix;
    }

    /**
     * Limits the response to buckets that are located in the specified Amazon Web Services Region. The Amazon Web
     * Services Region must be expressed according to the Amazon Web Services Region code, such as <code>us-west-2</code>
     * for the US West (Oregon) Region. For a list of the valid values for all of the Amazon Web Services Regions, see <a href="https://docs.aws.amazon.com/general/latest/gr/rande.html#s3_region">
     * Regions and Endpoints</a>.
     *
     * <p>Requests made to a Regional endpoint that is different from the <code>bucket-region</code> parameter are not
     * supported. For example, if you want to limit the response to your buckets in Region <code>us-west-2</code>, the
     * request must be made to an endpoint in Region <code>us-west-2</code>.
     */
    public String getBucketRegion() {
        return bucketRegion;
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
        ListBucketsInput that = (ListBucketsInput) other;
        return Objects.equals(this.maxBuckets, that.maxBuckets)
               && Objects.equals(this.continuationToken, that.continuationToken)
               && Objects.equals(this.prefix, that.prefix)
               && Objects.equals(this.bucketRegion, that.bucketRegion);
    }

    @Override
    public int hashCode() {
        int $hc = Objects.hashCode(maxBuckets);
        $hc = 31 * $hc + Objects.hashCode(continuationToken);
        $hc = 31 * $hc + Objects.hashCode(prefix);
        $hc = 31 * $hc + Objects.hashCode(bucketRegion);
        return $hc;
    }

    @Override
    public Schema schema() {
        return $SCHEMA;
    }

    @Override
    public void serializeMembers(ShapeSerializer serializer) {
        if (maxBuckets != null) {
            serializer.writeInteger($SCHEMA_MAX_BUCKETS, maxBuckets);
        }
        if (continuationToken != null) {
            serializer.writeString($SCHEMA_CONTINUATION_TOKEN, continuationToken);
        }
        if (prefix != null) {
            serializer.writeString($SCHEMA_PREFIX, prefix);
        }
        if (bucketRegion != null) {
            serializer.writeString($SCHEMA_BUCKET_REGION, bucketRegion);
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T getMemberValue(Schema member) {
        return switch (member.memberIndex()) {
            case 0 -> (T) SchemaUtils.validateSameMember($SCHEMA_MAX_BUCKETS, member, maxBuckets);
            case 1 -> (T) SchemaUtils.validateSameMember($SCHEMA_CONTINUATION_TOKEN, member, continuationToken);
            case 2 -> (T) SchemaUtils.validateSameMember($SCHEMA_PREFIX, member, prefix);
            case 3 -> (T) SchemaUtils.validateSameMember($SCHEMA_BUCKET_REGION, member, bucketRegion);
            default -> throw new IllegalArgumentException("Attempted to get non-existent member: " + member.id());
        };
    }

    /**
     * Create a new builder containing all the current property values of this object.
     *
     * <p><strong>Note:</strong> This method performs only a shallow copy of the original properties.
     *
     * @return a builder for {@link ListBucketsInput}.
     */
    public Builder toBuilder() {
        var builder = new Builder();
        builder.maxBuckets(this.maxBuckets);
        builder.continuationToken(this.continuationToken);
        builder.prefix(this.prefix);
        builder.bucketRegion(this.bucketRegion);
        return builder;
    }

    /**
     * @return returns a new Builder.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for {@link ListBucketsInput}.
     */
    public static final class Builder implements ShapeBuilder<ListBucketsInput> {
        private Integer maxBuckets;
        private String continuationToken;
        private String prefix;
        private String bucketRegion;

        private Builder() {}

        @Override
        public Schema schema() {
            return $SCHEMA;
        }

        /**
         * Maximum number of buckets to be returned in response. When the number is more than the count of buckets that are
         * owned by an Amazon Web Services account, return all the buckets in response.
         *
         * @return this builder.
         */
        public Builder maxBuckets(Integer maxBuckets) {
            this.maxBuckets = maxBuckets;
            return this;
        }

        /**
         * <code>ContinuationToken</code> indicates to Amazon S3 that the list is being continued on this bucket with a
         * token. <code>ContinuationToken</code> is obfuscated and is not a real key. You can use this <code>
         * ContinuationToken</code> for pagination of the list results.
         *
         * <p>Length Constraints: Minimum length of 0. Maximum length of 1024.
         *
         * <p>Required: No.
         *
         * <p>If you specify the <code>bucket-region</code>, <code>prefix</code>, or <code>continuation-token</code> query
         * parameters without using <code>max-buckets</code> to set the maximum number of buckets returned in the response,
         * Amazon S3 applies a default page size of 10,000 and provides a continuation token if there are more buckets.
         *
         * @return this builder.
         */
        public Builder continuationToken(String continuationToken) {
            this.continuationToken = continuationToken;
            return this;
        }

        /**
         * Limits the response to bucket names that begin with the specified bucket name prefix.
         *
         * @return this builder.
         */
        public Builder prefix(String prefix) {
            this.prefix = prefix;
            return this;
        }

        /**
         * Limits the response to buckets that are located in the specified Amazon Web Services Region. The Amazon Web
         * Services Region must be expressed according to the Amazon Web Services Region code, such as <code>us-west-2</code>
         * for the US West (Oregon) Region. For a list of the valid values for all of the Amazon Web Services Regions, see <a href="https://docs.aws.amazon.com/general/latest/gr/rande.html#s3_region">
         * Regions and Endpoints</a>.
         *
         * <p>Requests made to a Regional endpoint that is different from the <code>bucket-region</code> parameter are not
         * supported. For example, if you want to limit the response to your buckets in Region <code>us-west-2</code>, the
         * request must be made to an endpoint in Region <code>us-west-2</code>.
         *
         * @return this builder.
         */
        public Builder bucketRegion(String bucketRegion) {
            this.bucketRegion = bucketRegion;
            return this;
        }

        @Override
        public ListBucketsInput build() {
            return new ListBucketsInput(this);
        }

        @Override
        @SuppressWarnings("unchecked")
        public void setMemberValue(Schema member, Object value) {
            switch (member.memberIndex()) {
                case 0 -> maxBuckets((Integer) SchemaUtils.validateSameMember($SCHEMA_MAX_BUCKETS, member, value));
                case 1 -> continuationToken((String) SchemaUtils.validateSameMember($SCHEMA_CONTINUATION_TOKEN, member, value));
                case 2 -> prefix((String) SchemaUtils.validateSameMember($SCHEMA_PREFIX, member, value));
                case 3 -> bucketRegion((String) SchemaUtils.validateSameMember($SCHEMA_BUCKET_REGION, member, value));
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
                    case 0 -> builder.maxBuckets(de.readInteger(member));
                    case 1 -> builder.continuationToken(de.readString(member));
                    case 2 -> builder.prefix(de.readString(member));
                    case 3 -> builder.bucketRegion(de.readString(member));
                    default -> throw new IllegalArgumentException("Unexpected member: " + member.memberName());
                }
            }
        }
    }
}
