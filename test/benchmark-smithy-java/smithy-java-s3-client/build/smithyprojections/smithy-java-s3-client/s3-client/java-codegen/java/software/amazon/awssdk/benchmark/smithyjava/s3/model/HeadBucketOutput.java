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
public final class HeadBucketOutput implements SerializableStruct {

    public static final Schema $SCHEMA = Schemas4.HEAD_BUCKET_OUTPUT;
    private static final Schema $SCHEMA_BUCKET_ARN = $SCHEMA.member("BucketArn");
    private static final Schema $SCHEMA_BUCKET_LOCATION_TYPE = $SCHEMA.member("BucketLocationType");
    private static final Schema $SCHEMA_BUCKET_LOCATION_NAME = $SCHEMA.member("BucketLocationName");
    private static final Schema $SCHEMA_BUCKET_REGION = $SCHEMA.member("BucketRegion");
    private static final Schema $SCHEMA_ACCESS_POINT_ALIAS = $SCHEMA.member("AccessPointAlias");

    public static final ShapeId $ID = $SCHEMA.id();

    private final transient String bucketArn;
    private final transient LocationType bucketLocationType;
    private final transient String bucketLocationName;
    private final transient String bucketRegion;
    private final transient Boolean accessPointAlias;

    private HeadBucketOutput(Builder builder) {
        this.bucketArn = builder.bucketArn;
        this.bucketLocationType = builder.bucketLocationType;
        this.bucketLocationName = builder.bucketLocationName;
        this.bucketRegion = builder.bucketRegion;
        this.accessPointAlias = builder.accessPointAlias;
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

    /**
     * The type of location where the bucket is created.
     *
     * <p>This functionality is only supported by directory buckets.
     */
    public LocationType getBucketLocationType() {
        return bucketLocationType;
    }

    /**
     * The name of the location where the bucket will be created.
     *
     * <p>For directory buckets, the Zone ID of the Availability Zone or the Local Zone where the bucket is created. An
     * example Zone ID value for an Availability Zone is <code>usw2-az1</code>.
     *
     * <p>This functionality is only supported by directory buckets.
     */
    public String getBucketLocationName() {
        return bucketLocationName;
    }

    /**
     * The Region that the bucket is located.
     */
    public String getBucketRegion() {
        return bucketRegion;
    }

    /**
     * Indicates whether the bucket name used in the request is an access point alias.
     *
     * <p>For directory buckets, the value of this field is <code>false</code>.
     */
    public Boolean isAccessPointAlias() {
        return accessPointAlias;
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
        HeadBucketOutput that = (HeadBucketOutput) other;
        return Objects.equals(this.accessPointAlias, that.accessPointAlias)
               && Objects.equals(this.bucketArn, that.bucketArn)
               && Objects.equals(this.bucketLocationName, that.bucketLocationName)
               && Objects.equals(this.bucketRegion, that.bucketRegion)
               && Objects.equals(this.bucketLocationType, that.bucketLocationType);
    }

    @Override
    public int hashCode() {
        int $hc = Objects.hashCode(bucketArn);
        $hc = 31 * $hc + Objects.hashCode(bucketLocationType);
        $hc = 31 * $hc + Objects.hashCode(bucketLocationName);
        $hc = 31 * $hc + Objects.hashCode(bucketRegion);
        $hc = 31 * $hc + Objects.hashCode(accessPointAlias);
        return $hc;
    }

    @Override
    public Schema schema() {
        return $SCHEMA;
    }

    @Override
    public void serializeMembers(ShapeSerializer serializer) {
        if (bucketArn != null) {
            serializer.writeString($SCHEMA_BUCKET_ARN, bucketArn);
        }
        if (bucketLocationType != null) {
            serializer.writeString($SCHEMA_BUCKET_LOCATION_TYPE, bucketLocationType.getValue());
        }
        if (bucketLocationName != null) {
            serializer.writeString($SCHEMA_BUCKET_LOCATION_NAME, bucketLocationName);
        }
        if (bucketRegion != null) {
            serializer.writeString($SCHEMA_BUCKET_REGION, bucketRegion);
        }
        if (accessPointAlias != null) {
            serializer.writeBoolean($SCHEMA_ACCESS_POINT_ALIAS, accessPointAlias);
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T getMemberValue(Schema member) {
        return switch (member.memberIndex()) {
            case 0 -> (T) SchemaUtils.validateSameMember($SCHEMA_BUCKET_ARN, member, bucketArn);
            case 1 -> (T) SchemaUtils.validateSameMember($SCHEMA_BUCKET_LOCATION_TYPE, member, bucketLocationType);
            case 2 -> (T) SchemaUtils.validateSameMember($SCHEMA_BUCKET_LOCATION_NAME, member, bucketLocationName);
            case 3 -> (T) SchemaUtils.validateSameMember($SCHEMA_BUCKET_REGION, member, bucketRegion);
            case 4 -> (T) SchemaUtils.validateSameMember($SCHEMA_ACCESS_POINT_ALIAS, member, accessPointAlias);
            default -> throw new IllegalArgumentException("Attempted to get non-existent member: " + member.id());
        };
    }

    /**
     * Create a new builder containing all the current property values of this object.
     *
     * <p><strong>Note:</strong> This method performs only a shallow copy of the original properties.
     *
     * @return a builder for {@link HeadBucketOutput}.
     */
    public Builder toBuilder() {
        var builder = new Builder();
        builder.bucketArn(this.bucketArn);
        builder.bucketLocationType(this.bucketLocationType);
        builder.bucketLocationName(this.bucketLocationName);
        builder.bucketRegion(this.bucketRegion);
        builder.accessPointAlias(this.accessPointAlias);
        return builder;
    }

    /**
     * @return returns a new Builder.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for {@link HeadBucketOutput}.
     */
    public static final class Builder implements ShapeBuilder<HeadBucketOutput> {
        private String bucketArn;
        private LocationType bucketLocationType;
        private String bucketLocationName;
        private String bucketRegion;
        private Boolean accessPointAlias;

        private Builder() {}

        @Override
        public Schema schema() {
            return $SCHEMA;
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

        /**
         * The type of location where the bucket is created.
         *
         * <p>This functionality is only supported by directory buckets.
         *
         * @return this builder.
         */
        public Builder bucketLocationType(LocationType bucketLocationType) {
            this.bucketLocationType = bucketLocationType;
            return this;
        }

        /**
         * The name of the location where the bucket will be created.
         *
         * <p>For directory buckets, the Zone ID of the Availability Zone or the Local Zone where the bucket is created. An
         * example Zone ID value for an Availability Zone is <code>usw2-az1</code>.
         *
         * <p>This functionality is only supported by directory buckets.
         *
         * @return this builder.
         */
        public Builder bucketLocationName(String bucketLocationName) {
            this.bucketLocationName = bucketLocationName;
            return this;
        }

        /**
         * The Region that the bucket is located.
         *
         * @return this builder.
         */
        public Builder bucketRegion(String bucketRegion) {
            this.bucketRegion = bucketRegion;
            return this;
        }

        /**
         * Indicates whether the bucket name used in the request is an access point alias.
         *
         * <p>For directory buckets, the value of this field is <code>false</code>.
         *
         * @return this builder.
         */
        public Builder accessPointAlias(Boolean accessPointAlias) {
            this.accessPointAlias = accessPointAlias;
            return this;
        }

        @Override
        public HeadBucketOutput build() {
            return new HeadBucketOutput(this);
        }

        @Override
        @SuppressWarnings("unchecked")
        public void setMemberValue(Schema member, Object value) {
            switch (member.memberIndex()) {
                case 0 -> bucketArn((String) SchemaUtils.validateSameMember($SCHEMA_BUCKET_ARN, member, value));
                case 1 -> bucketLocationType((LocationType) SchemaUtils.validateSameMember($SCHEMA_BUCKET_LOCATION_TYPE, member, value));
                case 2 -> bucketLocationName((String) SchemaUtils.validateSameMember($SCHEMA_BUCKET_LOCATION_NAME, member, value));
                case 3 -> bucketRegion((String) SchemaUtils.validateSameMember($SCHEMA_BUCKET_REGION, member, value));
                case 4 -> accessPointAlias((Boolean) SchemaUtils.validateSameMember($SCHEMA_ACCESS_POINT_ALIAS, member, value));
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
                    case 0 -> builder.bucketArn(de.readString(member));
                    case 1 -> builder.bucketLocationType(LocationType.builder().deserializeMember(de, member).build());
                    case 2 -> builder.bucketLocationName(de.readString(member));
                    case 3 -> builder.bucketRegion(de.readString(member));
                    case 4 -> builder.accessPointAlias(de.readBoolean(member));
                    default -> throw new IllegalArgumentException("Unexpected member: " + member.memberName());
                }
            }
        }
    }
}
