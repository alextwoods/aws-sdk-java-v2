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
public final class ListDirectoryBucketsOutput implements SerializableStruct {

    public static final Schema $SCHEMA = Schemas4.LIST_DIRECTORY_BUCKETS_OUTPUT;
    private static final Schema $SCHEMA_BUCKETS = $SCHEMA.member("Buckets");
    private static final Schema $SCHEMA_CONTINUATION_TOKEN = $SCHEMA.member("ContinuationToken");

    public static final ShapeId $ID = $SCHEMA.id();

    private final transient List<Bucket> buckets;
    private final transient String continuationToken;

    private ListDirectoryBucketsOutput(Builder builder) {
        this.buckets = builder.buckets == null ? null : Collections.unmodifiableList(builder.buckets);
        this.continuationToken = builder.continuationToken;
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
     * If <code>ContinuationToken</code> was sent with the request, it is included in the response. You can use the
     * returned <code>ContinuationToken</code> for pagination of the list response.
     */
    public String getContinuationToken() {
        return continuationToken;
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
        ListDirectoryBucketsOutput that = (ListDirectoryBucketsOutput) other;
        return Objects.equals(this.continuationToken, that.continuationToken)
               && Objects.equals(this.buckets, that.buckets);
    }

    @Override
    public int hashCode() {
        int $hc = Objects.hashCode(buckets);
        $hc = 31 * $hc + Objects.hashCode(continuationToken);
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
        if (continuationToken != null) {
            serializer.writeString($SCHEMA_CONTINUATION_TOKEN, continuationToken);
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T getMemberValue(Schema member) {
        return switch (member.memberIndex()) {
            case 0 -> (T) SchemaUtils.validateSameMember($SCHEMA_BUCKETS, member, buckets);
            case 1 -> (T) SchemaUtils.validateSameMember($SCHEMA_CONTINUATION_TOKEN, member, continuationToken);
            default -> throw new IllegalArgumentException("Attempted to get non-existent member: " + member.id());
        };
    }

    /**
     * Create a new builder containing all the current property values of this object.
     *
     * <p><strong>Note:</strong> This method performs only a shallow copy of the original properties.
     *
     * @return a builder for {@link ListDirectoryBucketsOutput}.
     */
    public Builder toBuilder() {
        var builder = new Builder();
        builder.buckets(this.buckets);
        builder.continuationToken(this.continuationToken);
        return builder;
    }

    /**
     * @return returns a new Builder.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for {@link ListDirectoryBucketsOutput}.
     */
    public static final class Builder implements ShapeBuilder<ListDirectoryBucketsOutput> {
        private List<Bucket> buckets;
        private String continuationToken;

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
         * If <code>ContinuationToken</code> was sent with the request, it is included in the response. You can use the
         * returned <code>ContinuationToken</code> for pagination of the list response.
         *
         * @return this builder.
         */
        public Builder continuationToken(String continuationToken) {
            this.continuationToken = continuationToken;
            return this;
        }

        @Override
        public ListDirectoryBucketsOutput build() {
            return new ListDirectoryBucketsOutput(this);
        }

        @Override
        @SuppressWarnings("unchecked")
        public void setMemberValue(Schema member, Object value) {
            switch (member.memberIndex()) {
                case 0 -> buckets((List<Bucket>) SchemaUtils.validateSameMember($SCHEMA_BUCKETS, member, value));
                case 1 -> continuationToken((String) SchemaUtils.validateSameMember($SCHEMA_CONTINUATION_TOKEN, member, value));
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
                    case 1 -> builder.continuationToken(de.readString(member));
                    default -> throw new IllegalArgumentException("Unexpected member: " + member.memberName());
                }
            }
        }
    }
}
