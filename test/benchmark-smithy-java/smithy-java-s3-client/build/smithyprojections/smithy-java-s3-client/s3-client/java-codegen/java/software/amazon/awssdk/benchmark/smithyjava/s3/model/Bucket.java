package software.amazon.awssdk.benchmark.smithyjava.s3.model;

import java.time.Instant;
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
 * In terms of implementation, a Bucket is a resource.
 */
@SmithyGenerated
public final class Bucket implements SerializableStruct {

    public static final Schema $SCHEMA = Schemas4.BUCKET;
    private static final Schema $SCHEMA_NAME = $SCHEMA.member("Name");
    private static final Schema $SCHEMA_CREATION_DATE = $SCHEMA.member("CreationDate");
    private static final Schema $SCHEMA_BUCKET_REGION = $SCHEMA.member("BucketRegion");
    private static final Schema $SCHEMA_BUCKET_ARN = $SCHEMA.member("BucketArn");

    public static final ShapeId $ID = $SCHEMA.id();

    private final transient String name;
    private final transient Instant creationDate;
    private final transient String bucketRegion;
    private final transient String bucketArn;

    private Bucket(Builder builder) {
        this.name = builder.name;
        this.creationDate = builder.creationDate;
        this.bucketRegion = builder.bucketRegion;
        this.bucketArn = builder.bucketArn;
    }

    /**
     * The name of the bucket.
     */
    public String getName() {
        return name;
    }

    /**
     * Date the bucket was created. This date can change when making changes to your bucket, such as editing its bucket
     * policy.
     */
    public Instant getCreationDate() {
        return creationDate;
    }

    /**
     * <code>BucketRegion</code> indicates the Amazon Web Services region where the bucket is located. If the request
     * contains at least one valid parameter, it is included in the response.
     */
    public String getBucketRegion() {
        return bucketRegion;
    }

    /**
     * The Amazon Resource Name (ARN) of the S3 bucket. ARNs uniquely identify Amazon Web Services resources across all
     * of Amazon Web Services.
     *
     * <p>This parameter is only supported for S3 directory buckets. For more information, see <a href="https://docs.aws.amazon.com/AmazonS3/latest/userguide/directory-buckets-tagging.html">Using tags with
     * directory buckets</a>.
     */
    public String getBucketArn() {
        return bucketArn;
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
        Bucket that = (Bucket) other;
        return Objects.equals(this.name, that.name)
               && Objects.equals(this.bucketRegion, that.bucketRegion)
               && Objects.equals(this.bucketArn, that.bucketArn)
               && Objects.equals(this.creationDate, that.creationDate);
    }

    @Override
    public int hashCode() {
        int $hc = Objects.hashCode(name);
        $hc = 31 * $hc + Objects.hashCode(creationDate);
        $hc = 31 * $hc + Objects.hashCode(bucketRegion);
        $hc = 31 * $hc + Objects.hashCode(bucketArn);
        return $hc;
    }

    @Override
    public Schema schema() {
        return $SCHEMA;
    }

    @Override
    public void serializeMembers(ShapeSerializer serializer) {
        if (name != null) {
            serializer.writeString($SCHEMA_NAME, name);
        }
        if (creationDate != null) {
            serializer.writeTimestamp($SCHEMA_CREATION_DATE, creationDate);
        }
        if (bucketRegion != null) {
            serializer.writeString($SCHEMA_BUCKET_REGION, bucketRegion);
        }
        if (bucketArn != null) {
            serializer.writeString($SCHEMA_BUCKET_ARN, bucketArn);
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T getMemberValue(Schema member) {
        return switch (member.memberIndex()) {
            case 0 -> (T) SchemaUtils.validateSameMember($SCHEMA_NAME, member, name);
            case 1 -> (T) SchemaUtils.validateSameMember($SCHEMA_CREATION_DATE, member, creationDate);
            case 2 -> (T) SchemaUtils.validateSameMember($SCHEMA_BUCKET_REGION, member, bucketRegion);
            case 3 -> (T) SchemaUtils.validateSameMember($SCHEMA_BUCKET_ARN, member, bucketArn);
            default -> throw new IllegalArgumentException("Attempted to get non-existent member: " + member.id());
        };
    }

    /**
     * Create a new builder containing all the current property values of this object.
     *
     * <p><strong>Note:</strong> This method performs only a shallow copy of the original properties.
     *
     * @return a builder for {@link Bucket}.
     */
    public Builder toBuilder() {
        var builder = new Builder();
        builder.name(this.name);
        builder.creationDate(this.creationDate);
        builder.bucketRegion(this.bucketRegion);
        builder.bucketArn(this.bucketArn);
        return builder;
    }

    /**
     * @return returns a new Builder.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for {@link Bucket}.
     */
    public static final class Builder implements ShapeBuilder<Bucket> {
        private String name;
        private Instant creationDate;
        private String bucketRegion;
        private String bucketArn;

        private Builder() {}

        @Override
        public Schema schema() {
            return $SCHEMA;
        }

        /**
         * The name of the bucket.
         *
         * @return this builder.
         */
        public Builder name(String name) {
            this.name = name;
            return this;
        }

        /**
         * Date the bucket was created. This date can change when making changes to your bucket, such as editing its bucket
         * policy.
         *
         * @return this builder.
         */
        public Builder creationDate(Instant creationDate) {
            this.creationDate = creationDate;
            return this;
        }

        /**
         * <code>BucketRegion</code> indicates the Amazon Web Services region where the bucket is located. If the request
         * contains at least one valid parameter, it is included in the response.
         *
         * @return this builder.
         */
        public Builder bucketRegion(String bucketRegion) {
            this.bucketRegion = bucketRegion;
            return this;
        }

        /**
         * The Amazon Resource Name (ARN) of the S3 bucket. ARNs uniquely identify Amazon Web Services resources across all
         * of Amazon Web Services.
         *
         * <p>This parameter is only supported for S3 directory buckets. For more information, see <a href="https://docs.aws.amazon.com/AmazonS3/latest/userguide/directory-buckets-tagging.html">Using tags with
         * directory buckets</a>.
         *
         * @return this builder.
         */
        public Builder bucketArn(String bucketArn) {
            this.bucketArn = bucketArn;
            return this;
        }

        @Override
        public Bucket build() {
            return new Bucket(this);
        }

        @Override
        @SuppressWarnings("unchecked")
        public void setMemberValue(Schema member, Object value) {
            switch (member.memberIndex()) {
                case 0 -> name((String) SchemaUtils.validateSameMember($SCHEMA_NAME, member, value));
                case 1 -> creationDate((Instant) SchemaUtils.validateSameMember($SCHEMA_CREATION_DATE, member, value));
                case 2 -> bucketRegion((String) SchemaUtils.validateSameMember($SCHEMA_BUCKET_REGION, member, value));
                case 3 -> bucketArn((String) SchemaUtils.validateSameMember($SCHEMA_BUCKET_ARN, member, value));
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
                    case 0 -> builder.name(de.readString(member));
                    case 1 -> builder.creationDate(de.readTimestamp(member));
                    case 2 -> builder.bucketRegion(de.readString(member));
                    case 3 -> builder.bucketArn(de.readString(member));
                    default -> throw new IllegalArgumentException("Unexpected member: " + member.memberName());
                }
            }
        }
    }
}
