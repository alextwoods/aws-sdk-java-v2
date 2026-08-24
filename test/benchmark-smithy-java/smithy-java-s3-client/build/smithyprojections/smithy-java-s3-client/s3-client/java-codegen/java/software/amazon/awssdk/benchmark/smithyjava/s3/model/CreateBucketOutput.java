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
public final class CreateBucketOutput implements SerializableStruct {

    public static final Schema $SCHEMA = Schemas.CREATE_BUCKET_OUTPUT;
    private static final Schema $SCHEMA_LOCATION = $SCHEMA.member("Location");
    private static final Schema $SCHEMA_BUCKET_ARN = $SCHEMA.member("BucketArn");

    public static final ShapeId $ID = $SCHEMA.id();

    private final transient String location;
    private final transient String bucketArn;

    private CreateBucketOutput(Builder builder) {
        this.location = builder.location;
        this.bucketArn = builder.bucketArn;
    }

    /**
     * A forward slash followed by the name of the bucket for all account regional namespace buckets and all global
     * general purpose buckets created in us-east-1. For example, <code>/amzn-s3-demo-bucket</code>. For global general
     * purpose buckets created in other Amazon Web Services Regions, the Location field is the global endpoint URL. For
     * example, <code>http://amzn-s3-demo-bucket.s3.amazonaws.com/</code>.
     */
    public String getLocation() {
        return location;
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
        CreateBucketOutput that = (CreateBucketOutput) other;
        return Objects.equals(this.location, that.location)
               && Objects.equals(this.bucketArn, that.bucketArn);
    }

    @Override
    public int hashCode() {
        int $hc = Objects.hashCode(location);
        $hc = 31 * $hc + Objects.hashCode(bucketArn);
        return $hc;
    }

    @Override
    public Schema schema() {
        return $SCHEMA;
    }

    @Override
    public void serializeMembers(ShapeSerializer serializer) {
        if (location != null) {
            serializer.writeString($SCHEMA_LOCATION, location);
        }
        if (bucketArn != null) {
            serializer.writeString($SCHEMA_BUCKET_ARN, bucketArn);
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T getMemberValue(Schema member) {
        return switch (member.memberIndex()) {
            case 0 -> (T) SchemaUtils.validateSameMember($SCHEMA_LOCATION, member, location);
            case 1 -> (T) SchemaUtils.validateSameMember($SCHEMA_BUCKET_ARN, member, bucketArn);
            default -> throw new IllegalArgumentException("Attempted to get non-existent member: " + member.id());
        };
    }

    /**
     * Create a new builder containing all the current property values of this object.
     *
     * <p><strong>Note:</strong> This method performs only a shallow copy of the original properties.
     *
     * @return a builder for {@link CreateBucketOutput}.
     */
    public Builder toBuilder() {
        var builder = new Builder();
        builder.location(this.location);
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
     * Builder for {@link CreateBucketOutput}.
     */
    public static final class Builder implements ShapeBuilder<CreateBucketOutput> {
        private String location;
        private String bucketArn;

        private Builder() {}

        @Override
        public Schema schema() {
            return $SCHEMA;
        }

        /**
         * A forward slash followed by the name of the bucket for all account regional namespace buckets and all global
         * general purpose buckets created in us-east-1. For example, <code>/amzn-s3-demo-bucket</code>. For global general
         * purpose buckets created in other Amazon Web Services Regions, the Location field is the global endpoint URL. For
         * example, <code>http://amzn-s3-demo-bucket.s3.amazonaws.com/</code>.
         *
         * @return this builder.
         */
        public Builder location(String location) {
            this.location = location;
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
        public CreateBucketOutput build() {
            return new CreateBucketOutput(this);
        }

        @Override
        @SuppressWarnings("unchecked")
        public void setMemberValue(Schema member, Object value) {
            switch (member.memberIndex()) {
                case 0 -> location((String) SchemaUtils.validateSameMember($SCHEMA_LOCATION, member, value));
                case 1 -> bucketArn((String) SchemaUtils.validateSameMember($SCHEMA_BUCKET_ARN, member, value));
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
                    case 0 -> builder.location(de.readString(member));
                    case 1 -> builder.bucketArn(de.readString(member));
                    default -> throw new IllegalArgumentException("Unexpected member: " + member.memberName());
                }
            }
        }
    }
}
