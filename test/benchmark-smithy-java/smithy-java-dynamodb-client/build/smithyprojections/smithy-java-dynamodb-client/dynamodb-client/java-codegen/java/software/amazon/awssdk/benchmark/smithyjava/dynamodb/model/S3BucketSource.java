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

/**
 * The S3 bucket that is being imported from.
 */
@SmithyGenerated
public final class S3BucketSource implements SerializableStruct {

    public static final Schema $SCHEMA = Schemas.S3_BUCKET_SOURCE;
    private static final Schema $SCHEMA_S3_BUCKET_OWNER = $SCHEMA.member("S3BucketOwner");
    private static final Schema $SCHEMA_S3_BUCKET = $SCHEMA.member("S3Bucket");
    private static final Schema $SCHEMA_S3_KEY_PREFIX = $SCHEMA.member("S3KeyPrefix");

    public static final ShapeId $ID = $SCHEMA.id();

    private final transient String s3BucketOwner;
    private final transient String s3Bucket;
    private final transient String s3KeyPrefix;

    private S3BucketSource(Builder builder) {
        this.s3BucketOwner = builder.s3BucketOwner;
        this.s3Bucket = builder.s3Bucket;
        this.s3KeyPrefix = builder.s3KeyPrefix;
    }

    /**
     * The account number of the S3 bucket that is being imported from. If the bucket is owned by the requester this is
     * optional.
     */
    public String getS3BucketOwner() {
        return s3BucketOwner;
    }

    /**
     * The S3 bucket that is being imported from.
     */
    public String getS3Bucket() {
        return s3Bucket;
    }

    /**
     * The key prefix shared by all S3 Objects that are being imported.
     */
    public String getS3KeyPrefix() {
        return s3KeyPrefix;
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
        S3BucketSource that = (S3BucketSource) other;
        return Objects.equals(this.s3BucketOwner, that.s3BucketOwner)
               && Objects.equals(this.s3Bucket, that.s3Bucket)
               && Objects.equals(this.s3KeyPrefix, that.s3KeyPrefix);
    }

    @Override
    public int hashCode() {
        int $hc = Objects.hashCode(s3BucketOwner);
        $hc = 31 * $hc + Objects.hashCode(s3Bucket);
        $hc = 31 * $hc + Objects.hashCode(s3KeyPrefix);
        return $hc;
    }

    @Override
    public Schema schema() {
        return $SCHEMA;
    }

    @Override
    public void serializeMembers(ShapeSerializer serializer) {
        if (s3BucketOwner != null) {
            serializer.writeString($SCHEMA_S3_BUCKET_OWNER, s3BucketOwner);
        }
        serializer.writeString($SCHEMA_S3_BUCKET, s3Bucket);
        if (s3KeyPrefix != null) {
            serializer.writeString($SCHEMA_S3_KEY_PREFIX, s3KeyPrefix);
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T getMemberValue(Schema member) {
        return switch (member.memberIndex()) {
            case 0 -> (T) SchemaUtils.validateSameMember($SCHEMA_S3_BUCKET, member, s3Bucket);
            case 1 -> (T) SchemaUtils.validateSameMember($SCHEMA_S3_BUCKET_OWNER, member, s3BucketOwner);
            case 2 -> (T) SchemaUtils.validateSameMember($SCHEMA_S3_KEY_PREFIX, member, s3KeyPrefix);
            default -> throw new IllegalArgumentException("Attempted to get non-existent member: " + member.id());
        };
    }

    /**
     * Create a new builder containing all the current property values of this object.
     *
     * <p><strong>Note:</strong> This method performs only a shallow copy of the original properties.
     *
     * @return a builder for {@link S3BucketSource}.
     */
    public Builder toBuilder() {
        var builder = new Builder();
        builder.s3BucketOwner(this.s3BucketOwner);
        builder.s3Bucket(this.s3Bucket);
        builder.s3KeyPrefix(this.s3KeyPrefix);
        return builder;
    }

    /**
     * @return returns a new Builder.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for {@link S3BucketSource}.
     */
    public static final class Builder implements ShapeBuilder<S3BucketSource> {
        private final PresenceTracker tracker = PresenceTracker.of($SCHEMA);
        private String s3BucketOwner;
        private String s3Bucket;
        private String s3KeyPrefix;

        private Builder() {}

        @Override
        public Schema schema() {
            return $SCHEMA;
        }

        /**
         * The account number of the S3 bucket that is being imported from. If the bucket is owned by the requester this is
         * optional.
         *
         * @return this builder.
         */
        public Builder s3BucketOwner(String s3BucketOwner) {
            this.s3BucketOwner = s3BucketOwner;
            return this;
        }

        /**
         * The S3 bucket that is being imported from.
         *
         * <p><strong>Required</strong>
         * @return this builder.
         */
        public Builder s3Bucket(String s3Bucket) {
            this.s3Bucket = Objects.requireNonNull(s3Bucket, "s3Bucket cannot be null");
            tracker.setMember($SCHEMA_S3_BUCKET);
            return this;
        }

        /**
         * The key prefix shared by all S3 Objects that are being imported.
         *
         * @return this builder.
         */
        public Builder s3KeyPrefix(String s3KeyPrefix) {
            this.s3KeyPrefix = s3KeyPrefix;
            return this;
        }

        @Override
        public S3BucketSource build() {
            tracker.validate();
            return new S3BucketSource(this);
        }

        @Override
        @SuppressWarnings("unchecked")
        public void setMemberValue(Schema member, Object value) {
            switch (member.memberIndex()) {
                case 0 -> s3Bucket((String) SchemaUtils.validateSameMember($SCHEMA_S3_BUCKET, member, value));
                case 1 -> s3BucketOwner((String) SchemaUtils.validateSameMember($SCHEMA_S3_BUCKET_OWNER, member, value));
                case 2 -> s3KeyPrefix((String) SchemaUtils.validateSameMember($SCHEMA_S3_KEY_PREFIX, member, value));
                default -> ShapeBuilder.super.setMemberValue(member, value);
            }
        }

        @Override
        public ShapeBuilder<S3BucketSource> errorCorrection() {
            if (tracker.allSet()) {
                return this;
            }
            if (!tracker.checkMember($SCHEMA_S3_BUCKET)) {
                s3Bucket("");
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
                    case 0 -> builder.s3Bucket(de.readString(member));
                    case 1 -> builder.s3BucketOwner(de.readString(member));
                    case 2 -> builder.s3KeyPrefix(de.readString(member));
                    default -> throw new IllegalArgumentException("Unexpected member: " + member.memberName());
                }
            }
        }
    }
}
