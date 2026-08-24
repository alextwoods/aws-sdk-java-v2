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

/**
 * The configuration information for the bucket.
 */
@SmithyGenerated
public final class CreateBucketConfiguration implements SerializableStruct {

    public static final Schema $SCHEMA = Schemas.CREATE_BUCKET_CONFIGURATION;
    private static final Schema $SCHEMA_LOCATION_CONSTRAINT = $SCHEMA.member("LocationConstraint");
    private static final Schema $SCHEMA_LOCATION = $SCHEMA.member("Location");
    private static final Schema $SCHEMA_BUCKET = $SCHEMA.member("Bucket");
    private static final Schema $SCHEMA_TAGS = $SCHEMA.member("Tags");

    public static final ShapeId $ID = $SCHEMA.id();

    private final transient BucketLocationConstraint locationConstraint;
    private final transient LocationInfo location;
    private final transient BucketInfo bucket;
    private final transient List<Tag> tags;

    private CreateBucketConfiguration(Builder builder) {
        this.locationConstraint = builder.locationConstraint;
        this.location = builder.location;
        this.bucket = builder.bucket;
        this.tags = builder.tags == null ? null : Collections.unmodifiableList(builder.tags);
    }

    /**
     * Specifies the Region where the bucket will be created. You might choose a Region to optimize latency, minimize
     * costs, or address regulatory requirements. For example, if you reside in Europe, you will probably find it
     * advantageous to create buckets in the Europe (Ireland) Region.
     *
     * <p>If you don't specify a Region, the bucket is created in the US East (N. Virginia) Region (us-east-1) by
     * default. Configurations using the value <code>EU</code> will create a bucket in <code>eu-west-1</code>.
     *
     * <p>For a list of the valid values for all of the Amazon Web Services Regions, see <a href="https://docs.aws.amazon.com/general/latest/gr/rande.html#s3_region">Regions and Endpoints</a>.
     *
     * <p>This functionality is not supported for directory buckets.
     */
    public BucketLocationConstraint getLocationConstraint() {
        return locationConstraint;
    }

    /**
     * Specifies the location where the bucket will be created.
     *
     * <p><b>Directory buckets </b> - The location type is Availability Zone or Local Zone. To use the Local Zone
     * location type, your account must be enabled for Local Zones. Otherwise, you get an HTTP <code>403 Forbidden</code>
     * error with the error code <code>AccessDenied</code>. To learn more, see <a href="https://docs.aws.amazon.com/AmazonS3/latest/userguide/opt-in-directory-bucket-lz.html">Enable accounts for Local Zones</a> in
     * the <i>Amazon S3 User Guide</i>.
     *
     * <p>This functionality is only supported by directory buckets.
     */
    public LocationInfo getLocation() {
        return location;
    }

    /**
     * Specifies the information about the bucket that will be created.
     *
     * <p>This functionality is only supported by directory buckets.
     */
    public BucketInfo getBucket() {
        return bucket;
    }

    /**
     * An array of tags that you can apply to the bucket that you're creating. Tags are key-value pairs of metadata used
     * to categorize and organize your buckets, track costs, and control access.
     *
     * <p>You must have the <code>s3:TagResource</code> permission to create a general purpose bucket with tags or the <code>
     * s3express:TagResource</code> permission to create a directory bucket with tags.
     *
     * <p>When creating buckets with tags, note that tag-based conditions using <code>aws:ResourceTag</code> and <code>
     * s3:BucketTag</code> condition keys are applicable only after ABAC is enabled on the bucket. To learn more, see <a href="https://docs.aws.amazon.com/AmazonS3/latest/userguide/buckets-tagging-enable-abac.html">
     * Enabling ABAC in general purpose buckets</a>.
     */
    public List<Tag> getTags() {
        if (tags == null) {
            return Collections.emptyList();
        }
        return tags;
    }

    public boolean hasTags() {
        return tags != null;
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
        CreateBucketConfiguration that = (CreateBucketConfiguration) other;
        return Objects.equals(this.locationConstraint, that.locationConstraint)
               && Objects.equals(this.location, that.location)
               && Objects.equals(this.bucket, that.bucket)
               && Objects.equals(this.tags, that.tags);
    }

    @Override
    public int hashCode() {
        int $hc = Objects.hashCode(locationConstraint);
        $hc = 31 * $hc + Objects.hashCode(location);
        $hc = 31 * $hc + Objects.hashCode(bucket);
        $hc = 31 * $hc + Objects.hashCode(tags);
        return $hc;
    }

    @Override
    public Schema schema() {
        return $SCHEMA;
    }

    @Override
    public void serializeMembers(ShapeSerializer serializer) {
        if (locationConstraint != null) {
            serializer.writeString($SCHEMA_LOCATION_CONSTRAINT, locationConstraint.getValue());
        }
        if (location != null) {
            serializer.writeStruct($SCHEMA_LOCATION, location);
        }
        if (bucket != null) {
            serializer.writeStruct($SCHEMA_BUCKET, bucket);
        }
        if (tags != null) {
            serializer.writeList($SCHEMA_TAGS, tags, tags.size(), SharedSerde.TagSetSerializer.INSTANCE);
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T getMemberValue(Schema member) {
        return switch (member.memberIndex()) {
            case 0 -> (T) SchemaUtils.validateSameMember($SCHEMA_LOCATION_CONSTRAINT, member, locationConstraint);
            case 1 -> (T) SchemaUtils.validateSameMember($SCHEMA_LOCATION, member, location);
            case 2 -> (T) SchemaUtils.validateSameMember($SCHEMA_BUCKET, member, bucket);
            case 3 -> (T) SchemaUtils.validateSameMember($SCHEMA_TAGS, member, tags);
            default -> throw new IllegalArgumentException("Attempted to get non-existent member: " + member.id());
        };
    }

    /**
     * Create a new builder containing all the current property values of this object.
     *
     * <p><strong>Note:</strong> This method performs only a shallow copy of the original properties.
     *
     * @return a builder for {@link CreateBucketConfiguration}.
     */
    public Builder toBuilder() {
        var builder = new Builder();
        builder.locationConstraint(this.locationConstraint);
        builder.location(this.location);
        builder.bucket(this.bucket);
        builder.tags(this.tags);
        return builder;
    }

    /**
     * @return returns a new Builder.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for {@link CreateBucketConfiguration}.
     */
    public static final class Builder implements ShapeBuilder<CreateBucketConfiguration> {
        private BucketLocationConstraint locationConstraint;
        private LocationInfo location;
        private BucketInfo bucket;
        private List<Tag> tags;

        private Builder() {}

        @Override
        public Schema schema() {
            return $SCHEMA;
        }

        /**
         * Specifies the Region where the bucket will be created. You might choose a Region to optimize latency, minimize
         * costs, or address regulatory requirements. For example, if you reside in Europe, you will probably find it
         * advantageous to create buckets in the Europe (Ireland) Region.
         *
         * <p>If you don't specify a Region, the bucket is created in the US East (N. Virginia) Region (us-east-1) by
         * default. Configurations using the value <code>EU</code> will create a bucket in <code>eu-west-1</code>.
         *
         * <p>For a list of the valid values for all of the Amazon Web Services Regions, see <a href="https://docs.aws.amazon.com/general/latest/gr/rande.html#s3_region">Regions and Endpoints</a>.
         *
         * <p>This functionality is not supported for directory buckets.
         *
         * @return this builder.
         */
        public Builder locationConstraint(BucketLocationConstraint locationConstraint) {
            this.locationConstraint = locationConstraint;
            return this;
        }

        /**
         * Specifies the location where the bucket will be created.
         *
         * <p><b>Directory buckets </b> - The location type is Availability Zone or Local Zone. To use the Local Zone
         * location type, your account must be enabled for Local Zones. Otherwise, you get an HTTP <code>403 Forbidden</code>
         * error with the error code <code>AccessDenied</code>. To learn more, see <a href="https://docs.aws.amazon.com/AmazonS3/latest/userguide/opt-in-directory-bucket-lz.html">Enable accounts for Local Zones</a> in
         * the <i>Amazon S3 User Guide</i>.
         *
         * <p>This functionality is only supported by directory buckets.
         *
         * @return this builder.
         */
        public Builder location(LocationInfo location) {
            this.location = location;
            return this;
        }

        /**
         * Specifies the information about the bucket that will be created.
         *
         * <p>This functionality is only supported by directory buckets.
         *
         * @return this builder.
         */
        public Builder bucket(BucketInfo bucket) {
            this.bucket = bucket;
            return this;
        }

        /**
         * An array of tags that you can apply to the bucket that you're creating. Tags are key-value pairs of metadata used
         * to categorize and organize your buckets, track costs, and control access.
         *
         * <p>You must have the <code>s3:TagResource</code> permission to create a general purpose bucket with tags or the <code>
         * s3express:TagResource</code> permission to create a directory bucket with tags.
         *
         * <p>When creating buckets with tags, note that tag-based conditions using <code>aws:ResourceTag</code> and <code>
         * s3:BucketTag</code> condition keys are applicable only after ABAC is enabled on the bucket. To learn more, see <a href="https://docs.aws.amazon.com/AmazonS3/latest/userguide/buckets-tagging-enable-abac.html">
         * Enabling ABAC in general purpose buckets</a>.
         *
         * @return this builder.
         */
        public Builder tags(List<Tag> tags) {
            this.tags = tags;
            return this;
        }

        @Override
        public CreateBucketConfiguration build() {
            return new CreateBucketConfiguration(this);
        }

        @Override
        @SuppressWarnings("unchecked")
        public void setMemberValue(Schema member, Object value) {
            switch (member.memberIndex()) {
                case 0 -> locationConstraint((BucketLocationConstraint) SchemaUtils.validateSameMember($SCHEMA_LOCATION_CONSTRAINT, member, value));
                case 1 -> location((LocationInfo) SchemaUtils.validateSameMember($SCHEMA_LOCATION, member, value));
                case 2 -> bucket((BucketInfo) SchemaUtils.validateSameMember($SCHEMA_BUCKET, member, value));
                case 3 -> tags((List<Tag>) SchemaUtils.validateSameMember($SCHEMA_TAGS, member, value));
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
                    case 0 -> builder.locationConstraint(BucketLocationConstraint.builder().deserializeMember(de, member).build());
                    case 1 -> builder.location(LocationInfo.builder().deserializeMember(de, member).build());
                    case 2 -> builder.bucket(BucketInfo.builder().deserializeMember(de, member).build());
                    case 3 -> builder.tags(SharedSerde.deserializeTagSet(member, de));
                    default -> throw new IllegalArgumentException("Unexpected member: " + member.memberName());
                }
            }
        }
    }
}
