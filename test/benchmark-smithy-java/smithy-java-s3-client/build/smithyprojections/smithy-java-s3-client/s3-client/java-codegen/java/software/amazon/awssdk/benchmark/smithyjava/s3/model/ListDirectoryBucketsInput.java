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
public final class ListDirectoryBucketsInput implements SerializableStruct {

    public static final Schema $SCHEMA = Schemas4.LIST_DIRECTORY_BUCKETS_INPUT;
    private static final Schema $SCHEMA_CONTINUATION_TOKEN = $SCHEMA.member("ContinuationToken");
    private static final Schema $SCHEMA_MAX_DIRECTORY_BUCKETS = $SCHEMA.member("MaxDirectoryBuckets");

    public static final ShapeId $ID = $SCHEMA.id();

    private final transient String continuationToken;
    private final transient Integer maxDirectoryBuckets;

    private ListDirectoryBucketsInput(Builder builder) {
        this.continuationToken = builder.continuationToken;
        this.maxDirectoryBuckets = builder.maxDirectoryBuckets;
    }

    /**
     * <code>ContinuationToken</code> indicates to Amazon S3 that the list is being continued on buckets in this account
     * with a token. <code>ContinuationToken</code> is obfuscated and is not a real bucket name. You can use this
     * <code>ContinuationToken</code> for the pagination of the list results.
     */
    public String getContinuationToken() {
        return continuationToken;
    }

    /**
     * Maximum number of buckets to be returned in response. When the number is more than the count of buckets that are
     * owned by an Amazon Web Services account, return all the buckets in response.
     */
    public Integer getMaxDirectoryBuckets() {
        return maxDirectoryBuckets;
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
        ListDirectoryBucketsInput that = (ListDirectoryBucketsInput) other;
        return Objects.equals(this.maxDirectoryBuckets, that.maxDirectoryBuckets)
               && Objects.equals(this.continuationToken, that.continuationToken);
    }

    @Override
    public int hashCode() {
        int $hc = Objects.hashCode(continuationToken);
        $hc = 31 * $hc + Objects.hashCode(maxDirectoryBuckets);
        return $hc;
    }

    @Override
    public Schema schema() {
        return $SCHEMA;
    }

    @Override
    public void serializeMembers(ShapeSerializer serializer) {
        if (continuationToken != null) {
            serializer.writeString($SCHEMA_CONTINUATION_TOKEN, continuationToken);
        }
        if (maxDirectoryBuckets != null) {
            serializer.writeInteger($SCHEMA_MAX_DIRECTORY_BUCKETS, maxDirectoryBuckets);
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T getMemberValue(Schema member) {
        return switch (member.memberIndex()) {
            case 0 -> (T) SchemaUtils.validateSameMember($SCHEMA_CONTINUATION_TOKEN, member, continuationToken);
            case 1 -> (T) SchemaUtils.validateSameMember($SCHEMA_MAX_DIRECTORY_BUCKETS, member, maxDirectoryBuckets);
            default -> throw new IllegalArgumentException("Attempted to get non-existent member: " + member.id());
        };
    }

    /**
     * Create a new builder containing all the current property values of this object.
     *
     * <p><strong>Note:</strong> This method performs only a shallow copy of the original properties.
     *
     * @return a builder for {@link ListDirectoryBucketsInput}.
     */
    public Builder toBuilder() {
        var builder = new Builder();
        builder.continuationToken(this.continuationToken);
        builder.maxDirectoryBuckets(this.maxDirectoryBuckets);
        return builder;
    }

    /**
     * @return returns a new Builder.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for {@link ListDirectoryBucketsInput}.
     */
    public static final class Builder implements ShapeBuilder<ListDirectoryBucketsInput> {
        private String continuationToken;
        private Integer maxDirectoryBuckets;

        private Builder() {}

        @Override
        public Schema schema() {
            return $SCHEMA;
        }

        /**
         * <code>ContinuationToken</code> indicates to Amazon S3 that the list is being continued on buckets in this account
         * with a token. <code>ContinuationToken</code> is obfuscated and is not a real bucket name. You can use this
         * <code>ContinuationToken</code> for the pagination of the list results.
         *
         * @return this builder.
         */
        public Builder continuationToken(String continuationToken) {
            this.continuationToken = continuationToken;
            return this;
        }

        /**
         * Maximum number of buckets to be returned in response. When the number is more than the count of buckets that are
         * owned by an Amazon Web Services account, return all the buckets in response.
         *
         * @return this builder.
         */
        public Builder maxDirectoryBuckets(Integer maxDirectoryBuckets) {
            this.maxDirectoryBuckets = maxDirectoryBuckets;
            return this;
        }

        @Override
        public ListDirectoryBucketsInput build() {
            return new ListDirectoryBucketsInput(this);
        }

        @Override
        @SuppressWarnings("unchecked")
        public void setMemberValue(Schema member, Object value) {
            switch (member.memberIndex()) {
                case 0 -> continuationToken((String) SchemaUtils.validateSameMember($SCHEMA_CONTINUATION_TOKEN, member, value));
                case 1 -> maxDirectoryBuckets((Integer) SchemaUtils.validateSameMember($SCHEMA_MAX_DIRECTORY_BUCKETS, member, value));
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
                    case 0 -> builder.continuationToken(de.readString(member));
                    case 1 -> builder.maxDirectoryBuckets(de.readInteger(member));
                    default -> throw new IllegalArgumentException("Unexpected member: " + member.memberName());
                }
            }
        }
    }
}
