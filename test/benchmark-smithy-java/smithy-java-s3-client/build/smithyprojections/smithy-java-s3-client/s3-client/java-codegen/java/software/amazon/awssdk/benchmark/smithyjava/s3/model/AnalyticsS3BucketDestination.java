package software.amazon.awssdk.benchmark.smithyjava.s3.model;

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
 * Contains information about where to publish the analytics results.
 */
@SmithyGenerated
public final class AnalyticsS3BucketDestination implements SerializableStruct {

    public static final Schema $SCHEMA = Schemas1.ANALYTICS_S3_BUCKET_DESTINATION;
    private static final Schema $SCHEMA_FORMAT = $SCHEMA.member("Format");
    private static final Schema $SCHEMA_BUCKET_ACCOUNT_ID = $SCHEMA.member("BucketAccountId");
    private static final Schema $SCHEMA_BUCKET = $SCHEMA.member("Bucket");
    private static final Schema $SCHEMA_PREFIX = $SCHEMA.member("Prefix");

    public static final ShapeId $ID = $SCHEMA.id();

    private final transient AnalyticsS3ExportFileFormat format;
    private final transient String bucketAccountId;
    private final transient String bucket;
    private final transient String prefix;

    private AnalyticsS3BucketDestination(Builder builder) {
        this.format = builder.format;
        this.bucketAccountId = builder.bucketAccountId;
        this.bucket = builder.bucket;
        this.prefix = builder.prefix;
    }

    /**
     * Specifies the file format used when exporting data to Amazon S3.
     */
    public AnalyticsS3ExportFileFormat getFormat() {
        return format;
    }

    /**
     * The account ID that owns the destination S3 bucket. If no account ID is provided, the owner is not validated
     * before exporting data.
     *
     * <p> Although this value is optional, we strongly recommend that you set it to help prevent problems if the
     * destination bucket ownership changes.
     */
    public String getBucketAccountId() {
        return bucketAccountId;
    }

    /**
     * The Amazon Resource Name (ARN) of the bucket to which data is exported.
     */
    public String getBucket() {
        return bucket;
    }

    /**
     * The prefix to use when exporting data. The prefix is prepended to all results.
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
        AnalyticsS3BucketDestination that = (AnalyticsS3BucketDestination) other;
        return Objects.equals(this.bucketAccountId, that.bucketAccountId)
               && Objects.equals(this.bucket, that.bucket)
               && Objects.equals(this.prefix, that.prefix)
               && Objects.equals(this.format, that.format);
    }

    @Override
    public int hashCode() {
        int $hc = Objects.hashCode(format);
        $hc = 31 * $hc + Objects.hashCode(bucketAccountId);
        $hc = 31 * $hc + Objects.hashCode(bucket);
        $hc = 31 * $hc + Objects.hashCode(prefix);
        return $hc;
    }

    @Override
    public Schema schema() {
        return $SCHEMA;
    }

    @Override
    public void serializeMembers(ShapeSerializer serializer) {
        serializer.writeString($SCHEMA_FORMAT, format.getValue());
        if (bucketAccountId != null) {
            serializer.writeString($SCHEMA_BUCKET_ACCOUNT_ID, bucketAccountId);
        }
        serializer.writeString($SCHEMA_BUCKET, bucket);
        if (prefix != null) {
            serializer.writeString($SCHEMA_PREFIX, prefix);
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T getMemberValue(Schema member) {
        return switch (member.memberIndex()) {
            case 0 -> (T) SchemaUtils.validateSameMember($SCHEMA_FORMAT, member, format);
            case 1 -> (T) SchemaUtils.validateSameMember($SCHEMA_BUCKET, member, bucket);
            case 2 -> (T) SchemaUtils.validateSameMember($SCHEMA_BUCKET_ACCOUNT_ID, member, bucketAccountId);
            case 3 -> (T) SchemaUtils.validateSameMember($SCHEMA_PREFIX, member, prefix);
            default -> throw new IllegalArgumentException("Attempted to get non-existent member: " + member.id());
        };
    }

    /**
     * Create a new builder containing all the current property values of this object.
     *
     * <p><strong>Note:</strong> This method performs only a shallow copy of the original properties.
     *
     * @return a builder for {@link AnalyticsS3BucketDestination}.
     */
    public Builder toBuilder() {
        var builder = new Builder();
        builder.format(this.format);
        builder.bucketAccountId(this.bucketAccountId);
        builder.bucket(this.bucket);
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
     * Builder for {@link AnalyticsS3BucketDestination}.
     */
    public static final class Builder implements ShapeBuilder<AnalyticsS3BucketDestination> {
        private final PresenceTracker tracker = PresenceTracker.of($SCHEMA);
        private AnalyticsS3ExportFileFormat format;
        private String bucketAccountId;
        private String bucket;
        private String prefix;

        private Builder() {}

        @Override
        public Schema schema() {
            return $SCHEMA;
        }

        /**
         * Specifies the file format used when exporting data to Amazon S3.
         *
         * <p><strong>Required</strong>
         * @return this builder.
         */
        public Builder format(AnalyticsS3ExportFileFormat format) {
            this.format = Objects.requireNonNull(format, "format cannot be null");
            tracker.setMember($SCHEMA_FORMAT);
            return this;
        }

        /**
         * The account ID that owns the destination S3 bucket. If no account ID is provided, the owner is not validated
         * before exporting data.
         *
         * <p> Although this value is optional, we strongly recommend that you set it to help prevent problems if the
         * destination bucket ownership changes.
         *
         * @return this builder.
         */
        public Builder bucketAccountId(String bucketAccountId) {
            this.bucketAccountId = bucketAccountId;
            return this;
        }

        /**
         * The Amazon Resource Name (ARN) of the bucket to which data is exported.
         *
         * <p><strong>Required</strong>
         * @return this builder.
         */
        public Builder bucket(String bucket) {
            this.bucket = Objects.requireNonNull(bucket, "bucket cannot be null");
            tracker.setMember($SCHEMA_BUCKET);
            return this;
        }

        /**
         * The prefix to use when exporting data. The prefix is prepended to all results.
         *
         * @return this builder.
         */
        public Builder prefix(String prefix) {
            this.prefix = prefix;
            return this;
        }

        @Override
        public AnalyticsS3BucketDestination build() {
            tracker.validate();
            return new AnalyticsS3BucketDestination(this);
        }

        @Override
        @SuppressWarnings("unchecked")
        public void setMemberValue(Schema member, Object value) {
            switch (member.memberIndex()) {
                case 0 -> format((AnalyticsS3ExportFileFormat) SchemaUtils.validateSameMember($SCHEMA_FORMAT, member, value));
                case 1 -> bucket((String) SchemaUtils.validateSameMember($SCHEMA_BUCKET, member, value));
                case 2 -> bucketAccountId((String) SchemaUtils.validateSameMember($SCHEMA_BUCKET_ACCOUNT_ID, member, value));
                case 3 -> prefix((String) SchemaUtils.validateSameMember($SCHEMA_PREFIX, member, value));
                default -> ShapeBuilder.super.setMemberValue(member, value);
            }
        }

        @Override
        public ShapeBuilder<AnalyticsS3BucketDestination> errorCorrection() {
            if (tracker.allSet()) {
                return this;
            }
            if (!tracker.checkMember($SCHEMA_FORMAT)) {
                format(AnalyticsS3ExportFileFormat.unknown(""));
            }
            if (!tracker.checkMember($SCHEMA_BUCKET)) {
                bucket("");
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
                    case 0 -> builder.format(AnalyticsS3ExportFileFormat.builder().deserializeMember(de, member).build());
                    case 1 -> builder.bucket(de.readString(member));
                    case 2 -> builder.bucketAccountId(de.readString(member));
                    case 3 -> builder.prefix(de.readString(member));
                    default -> throw new IllegalArgumentException("Unexpected member: " + member.memberName());
                }
            }
        }
    }
}
