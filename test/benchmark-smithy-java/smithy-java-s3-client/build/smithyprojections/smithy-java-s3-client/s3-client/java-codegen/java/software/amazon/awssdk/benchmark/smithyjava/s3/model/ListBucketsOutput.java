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
public final class ListBucketsOutput implements SerializableStruct {

    public static final Schema $SCHEMA = Schemas4.LIST_BUCKETS_OUTPUT;
    private static final Schema $SCHEMA_BUCKETS = $SCHEMA.member("Buckets");
    private static final Schema $SCHEMA_OWNER = $SCHEMA.member("Owner");
    private static final Schema $SCHEMA_CONTINUATION_TOKEN = $SCHEMA.member("ContinuationToken");
    private static final Schema $SCHEMA_PREFIX = $SCHEMA.member("Prefix");

    public static final ShapeId $ID = $SCHEMA.id();

    private final transient List<Bucket> buckets;
    private final transient Owner owner;
    private final transient String continuationToken;
    private final transient String prefix;

    private ListBucketsOutput(Builder builder) {
        this.buckets = builder.buckets == null ? null : Collections.unmodifiableList(builder.buckets);
        this.owner = builder.owner;
        this.continuationToken = builder.continuationToken;
        this.prefix = builder.prefix;
    }

    /**
     * The list of buckets owned by the requester.
     */
    public List<Bucket> getBuckets() {
        if (buckets == null) {
            return Collections.emptyList();
        }
        return buckets;
    }

    public boolean hasBuckets() {
        return buckets != null;
    }

    /**
     * The owner of the buckets listed.
     */
    public Owner getOwner() {
        return owner;
    }

    /**
     * <code>ContinuationToken</code> is included in the response when there are more buckets that can be listed with
     * pagination. The next <code>ListBuckets</code> request to Amazon S3 can be continued with this
     * <code>ContinuationToken</code>. <code>ContinuationToken</code> is obfuscated and is not a real bucket.
     */
    public String getContinuationToken() {
        return continuationToken;
    }

    /**
     * If <code>Prefix</code> was sent with the request, it is included in the response.
     *
     * <p>All bucket names in the response begin with the specified bucket name prefix.
     */
    public String getPrefix() {
        return prefix;
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
        ListBucketsOutput that = (ListBucketsOutput) other;
        return Objects.equals(this.continuationToken, that.continuationToken)
               && Objects.equals(this.prefix, that.prefix)
               && Objects.equals(this.owner, that.owner)
               && Objects.equals(this.buckets, that.buckets);
    }

    @Override
    public int hashCode() {
        int $hc = Objects.hashCode(buckets);
        $hc = 31 * $hc + Objects.hashCode(owner);
        $hc = 31 * $hc + Objects.hashCode(continuationToken);
        $hc = 31 * $hc + Objects.hashCode(prefix);
        return $hc;
    }

    @Override
    public Schema schema() {
        return $SCHEMA;
    }

    @Override
    public void serializeMembers(ShapeSerializer serializer) {
        if (buckets != null) {
            serializer.writeList($SCHEMA_BUCKETS, buckets, buckets.size(), SharedSerde.BucketsSerializer.INSTANCE);
        }
        if (owner != null) {
            serializer.writeStruct($SCHEMA_OWNER, owner);
        }
        if (continuationToken != null) {
            serializer.writeString($SCHEMA_CONTINUATION_TOKEN, continuationToken);
        }
        if (prefix != null) {
            serializer.writeString($SCHEMA_PREFIX, prefix);
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T getMemberValue(Schema member) {
        return switch (member.memberIndex()) {
            case 0 -> (T) SchemaUtils.validateSameMember($SCHEMA_BUCKETS, member, buckets);
            case 1 -> (T) SchemaUtils.validateSameMember($SCHEMA_OWNER, member, owner);
            case 2 -> (T) SchemaUtils.validateSameMember($SCHEMA_CONTINUATION_TOKEN, member, continuationToken);
            case 3 -> (T) SchemaUtils.validateSameMember($SCHEMA_PREFIX, member, prefix);
            default -> throw new IllegalArgumentException("Attempted to get non-existent member: " + member.id());
        };
    }

    /**
     * Create a new builder containing all the current property values of this object.
     *
     * <p><strong>Note:</strong> This method performs only a shallow copy of the original properties.
     *
     * @return a builder for {@link ListBucketsOutput}.
     */
    public Builder toBuilder() {
        var builder = new Builder();
        builder.buckets(this.buckets);
        builder.owner(this.owner);
        builder.continuationToken(this.continuationToken);
        builder.prefix(this.prefix);
        return builder;
    }

    /**
     * @return returns a new Builder.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for {@link ListBucketsOutput}.
     */
    public static final class Builder implements ShapeBuilder<ListBucketsOutput> {
        private List<Bucket> buckets;
        private Owner owner;
        private String continuationToken;
        private String prefix;

        private Builder() {}

        @Override
        public Schema schema() {
            return $SCHEMA;
        }

        /**
         * The list of buckets owned by the requester.
         *
         * @return this builder.
         */
        public Builder buckets(List<Bucket> buckets) {
            this.buckets = buckets;
            return this;
        }

        /**
         * The owner of the buckets listed.
         *
         * @return this builder.
         */
        public Builder owner(Owner owner) {
            this.owner = owner;
            return this;
        }

        /**
         * <code>ContinuationToken</code> is included in the response when there are more buckets that can be listed with
         * pagination. The next <code>ListBuckets</code> request to Amazon S3 can be continued with this
         * <code>ContinuationToken</code>. <code>ContinuationToken</code> is obfuscated and is not a real bucket.
         *
         * @return this builder.
         */
        public Builder continuationToken(String continuationToken) {
            this.continuationToken = continuationToken;
            return this;
        }

        /**
         * If <code>Prefix</code> was sent with the request, it is included in the response.
         *
         * <p>All bucket names in the response begin with the specified bucket name prefix.
         *
         * @return this builder.
         */
        public Builder prefix(String prefix) {
            this.prefix = prefix;
            return this;
        }

        @Override
        public ListBucketsOutput build() {
            return new ListBucketsOutput(this);
        }

        @Override
        @SuppressWarnings("unchecked")
        public void setMemberValue(Schema member, Object value) {
            switch (member.memberIndex()) {
                case 0 -> buckets((List<Bucket>) SchemaUtils.validateSameMember($SCHEMA_BUCKETS, member, value));
                case 1 -> owner((Owner) SchemaUtils.validateSameMember($SCHEMA_OWNER, member, value));
                case 2 -> continuationToken((String) SchemaUtils.validateSameMember($SCHEMA_CONTINUATION_TOKEN, member, value));
                case 3 -> prefix((String) SchemaUtils.validateSameMember($SCHEMA_PREFIX, member, value));
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
                    case 0 -> builder.buckets(SharedSerde.deserializeBuckets(member, de));
                    case 1 -> builder.owner(Owner.builder().deserializeMember(de, member).build());
                    case 2 -> builder.continuationToken(de.readString(member));
                    case 3 -> builder.prefix(de.readString(member));
                    default -> throw new IllegalArgumentException("Unexpected member: " + member.memberName());
                }
            }
        }
    }
}
