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

@SmithyGenerated
public final class PutBucketLifecycleConfigurationInput implements SerializableStruct {

    public static final Schema $SCHEMA = Schemas5.PUT_BUCKET_LIFECYCLE_CONFIGURATION_INPUT;
    private static final Schema $SCHEMA_BUCKET = $SCHEMA.member("Bucket");
    private static final Schema $SCHEMA_CHECKSUM_ALGORITHM = $SCHEMA.member("ChecksumAlgorithm");
    private static final Schema $SCHEMA_LIFECYCLE_CONFIGURATION = $SCHEMA.member("LifecycleConfiguration");
    private static final Schema $SCHEMA_EXPECTED_BUCKET_OWNER = $SCHEMA.member("ExpectedBucketOwner");
    private static final Schema $SCHEMA_TRANSITION_DEFAULT_MINIMUM_OBJECT_SIZE = $SCHEMA.member("TransitionDefaultMinimumObjectSize");

    public static final ShapeId $ID = $SCHEMA.id();

    private final transient String bucket;
    private final transient ChecksumAlgorithm checksumAlgorithm;
    private final transient BucketLifecycleConfiguration lifecycleConfiguration;
    private final transient String expectedBucketOwner;
    private final transient TransitionDefaultMinimumObjectSize transitionDefaultMinimumObjectSize;

    private PutBucketLifecycleConfigurationInput(Builder builder) {
        this.bucket = builder.bucket;
        this.checksumAlgorithm = builder.checksumAlgorithm;
        this.lifecycleConfiguration = builder.lifecycleConfiguration;
        this.expectedBucketOwner = builder.expectedBucketOwner;
        this.transitionDefaultMinimumObjectSize = builder.transitionDefaultMinimumObjectSize;
    }

    /**
     * The name of the bucket for which to set the configuration.
     */
    public String getBucket() {
        return bucket;
    }

    /**
     * Indicates the algorithm used to create the checksum for the request when you use the SDK. This header will not
     * provide any additional functionality if you don't use the SDK. When you send this header, there must be a
     * corresponding <code>x-amz-checksum</code> or <code>x-amz-trailer</code> header sent. Otherwise, Amazon S3 fails
     * the request with the HTTP status code <code>400 Bad Request</code>. For more information, see <a href="https://docs.aws.amazon.com/AmazonS3/latest/userguide/checking-object-integrity.html">Checking object
     * integrity</a> in the <i>Amazon S3 User Guide</i>.
     *
     * <p>If you provide an individual checksum, Amazon S3 ignores any provided <code>ChecksumAlgorithm</code>
     * parameter.
     */
    public ChecksumAlgorithm getChecksumAlgorithm() {
        return checksumAlgorithm;
    }

    /**
     * Container for lifecycle rules. You can add as many as 1,000 rules.
     */
    public BucketLifecycleConfiguration getLifecycleConfiguration() {
        return lifecycleConfiguration;
    }

    /**
     * The account ID of the expected bucket owner. If the account ID that you provide does not match the actual owner
     * of the bucket, the request fails with the HTTP status code <code>403 Forbidden</code> (access denied).
     *
     * <p>This parameter applies to general purpose buckets only. It is not supported for directory bucket lifecycle
     * configurations.
     */
    public String getExpectedBucketOwner() {
        return expectedBucketOwner;
    }

    /**
     * Indicates which default minimum object size behavior is applied to the lifecycle configuration.
     *
     * <p>This parameter applies to general purpose buckets only. It is not supported for directory bucket lifecycle
     * configurations.
     *
     * <ul>
     *   <li>
     *     <code>all_storage_classes_128K</code> - Objects smaller than 128 KB will not transition to any storage
     *     class by default.
     *   </li>
     *   <li>
     *     <code>varies_by_storage_class</code> - Objects smaller than 128 KB will transition to Glacier Flexible
     *     Retrieval or Glacier Deep Archive storage classes. By default, all other storage classes will prevent
     *     transitions smaller than 128 KB.
     *   </li>
     * </ul>
     *
     * <p>To customize the minimum object size for any transition you can add a filter that specifies a custom <code>
     * ObjectSizeGreaterThan</code> or <code>ObjectSizeLessThan</code> in the body of your transition rule. Custom
     * filters always take precedence over the default transition behavior.
     */
    public TransitionDefaultMinimumObjectSize getTransitionDefaultMinimumObjectSize() {
        return transitionDefaultMinimumObjectSize;
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
        PutBucketLifecycleConfigurationInput that = (PutBucketLifecycleConfigurationInput) other;
        return Objects.equals(this.bucket, that.bucket)
               && Objects.equals(this.expectedBucketOwner, that.expectedBucketOwner)
               && Objects.equals(this.checksumAlgorithm, that.checksumAlgorithm)
               && Objects.equals(this.transitionDefaultMinimumObjectSize, that.transitionDefaultMinimumObjectSize)
               && Objects.equals(this.lifecycleConfiguration, that.lifecycleConfiguration);
    }

    @Override
    public int hashCode() {
        int $hc = Objects.hashCode(bucket);
        $hc = 31 * $hc + Objects.hashCode(checksumAlgorithm);
        $hc = 31 * $hc + Objects.hashCode(lifecycleConfiguration);
        $hc = 31 * $hc + Objects.hashCode(expectedBucketOwner);
        $hc = 31 * $hc + Objects.hashCode(transitionDefaultMinimumObjectSize);
        return $hc;
    }

    @Override
    public Schema schema() {
        return $SCHEMA;
    }

    @Override
    public void serializeMembers(ShapeSerializer serializer) {
        serializer.writeString($SCHEMA_BUCKET, bucket);
        if (checksumAlgorithm != null) {
            serializer.writeString($SCHEMA_CHECKSUM_ALGORITHM, checksumAlgorithm.getValue());
        }
        if (lifecycleConfiguration != null) {
            serializer.writeStruct($SCHEMA_LIFECYCLE_CONFIGURATION, lifecycleConfiguration);
        }
        if (expectedBucketOwner != null) {
            serializer.writeString($SCHEMA_EXPECTED_BUCKET_OWNER, expectedBucketOwner);
        }
        if (transitionDefaultMinimumObjectSize != null) {
            serializer.writeString($SCHEMA_TRANSITION_DEFAULT_MINIMUM_OBJECT_SIZE, transitionDefaultMinimumObjectSize.getValue());
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T getMemberValue(Schema member) {
        return switch (member.memberIndex()) {
            case 0 -> (T) SchemaUtils.validateSameMember($SCHEMA_BUCKET, member, bucket);
            case 1 -> (T) SchemaUtils.validateSameMember($SCHEMA_CHECKSUM_ALGORITHM, member, checksumAlgorithm);
            case 2 -> (T) SchemaUtils.validateSameMember($SCHEMA_LIFECYCLE_CONFIGURATION, member, lifecycleConfiguration);
            case 3 -> (T) SchemaUtils.validateSameMember($SCHEMA_EXPECTED_BUCKET_OWNER, member, expectedBucketOwner);
            case 4 -> (T) SchemaUtils.validateSameMember($SCHEMA_TRANSITION_DEFAULT_MINIMUM_OBJECT_SIZE, member, transitionDefaultMinimumObjectSize);
            default -> throw new IllegalArgumentException("Attempted to get non-existent member: " + member.id());
        };
    }

    /**
     * Create a new builder containing all the current property values of this object.
     *
     * <p><strong>Note:</strong> This method performs only a shallow copy of the original properties.
     *
     * @return a builder for {@link PutBucketLifecycleConfigurationInput}.
     */
    public Builder toBuilder() {
        var builder = new Builder();
        builder.bucket(this.bucket);
        builder.checksumAlgorithm(this.checksumAlgorithm);
        builder.lifecycleConfiguration(this.lifecycleConfiguration);
        builder.expectedBucketOwner(this.expectedBucketOwner);
        builder.transitionDefaultMinimumObjectSize(this.transitionDefaultMinimumObjectSize);
        return builder;
    }

    /**
     * @return returns a new Builder.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for {@link PutBucketLifecycleConfigurationInput}.
     */
    public static final class Builder implements ShapeBuilder<PutBucketLifecycleConfigurationInput> {
        private final PresenceTracker tracker = PresenceTracker.of($SCHEMA);
        private String bucket;
        private ChecksumAlgorithm checksumAlgorithm;
        private BucketLifecycleConfiguration lifecycleConfiguration;
        private String expectedBucketOwner;
        private TransitionDefaultMinimumObjectSize transitionDefaultMinimumObjectSize;

        private Builder() {}

        @Override
        public Schema schema() {
            return $SCHEMA;
        }

        /**
         * The name of the bucket for which to set the configuration.
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
         * Indicates the algorithm used to create the checksum for the request when you use the SDK. This header will not
         * provide any additional functionality if you don't use the SDK. When you send this header, there must be a
         * corresponding <code>x-amz-checksum</code> or <code>x-amz-trailer</code> header sent. Otherwise, Amazon S3 fails
         * the request with the HTTP status code <code>400 Bad Request</code>. For more information, see <a href="https://docs.aws.amazon.com/AmazonS3/latest/userguide/checking-object-integrity.html">Checking object
         * integrity</a> in the <i>Amazon S3 User Guide</i>.
         *
         * <p>If you provide an individual checksum, Amazon S3 ignores any provided <code>ChecksumAlgorithm</code>
         * parameter.
         *
         * @return this builder.
         */
        public Builder checksumAlgorithm(ChecksumAlgorithm checksumAlgorithm) {
            this.checksumAlgorithm = checksumAlgorithm;
            return this;
        }

        /**
         * Container for lifecycle rules. You can add as many as 1,000 rules.
         *
         * @return this builder.
         */
        public Builder lifecycleConfiguration(BucketLifecycleConfiguration lifecycleConfiguration) {
            this.lifecycleConfiguration = lifecycleConfiguration;
            return this;
        }

        /**
         * The account ID of the expected bucket owner. If the account ID that you provide does not match the actual owner
         * of the bucket, the request fails with the HTTP status code <code>403 Forbidden</code> (access denied).
         *
         * <p>This parameter applies to general purpose buckets only. It is not supported for directory bucket lifecycle
         * configurations.
         *
         * @return this builder.
         */
        public Builder expectedBucketOwner(String expectedBucketOwner) {
            this.expectedBucketOwner = expectedBucketOwner;
            return this;
        }

        /**
         * Indicates which default minimum object size behavior is applied to the lifecycle configuration.
         *
         * <p>This parameter applies to general purpose buckets only. It is not supported for directory bucket lifecycle
         * configurations.
         *
         * <ul>
         *   <li>
         *     <code>all_storage_classes_128K</code> - Objects smaller than 128 KB will not transition to any storage
         *     class by default.
         *   </li>
         *   <li>
         *     <code>varies_by_storage_class</code> - Objects smaller than 128 KB will transition to Glacier Flexible
         *     Retrieval or Glacier Deep Archive storage classes. By default, all other storage classes will prevent
         *     transitions smaller than 128 KB.
         *   </li>
         * </ul>
         *
         * <p>To customize the minimum object size for any transition you can add a filter that specifies a custom <code>
         * ObjectSizeGreaterThan</code> or <code>ObjectSizeLessThan</code> in the body of your transition rule. Custom
         * filters always take precedence over the default transition behavior.
         *
         * @return this builder.
         */
        public Builder transitionDefaultMinimumObjectSize(TransitionDefaultMinimumObjectSize transitionDefaultMinimumObjectSize) {
            this.transitionDefaultMinimumObjectSize = transitionDefaultMinimumObjectSize;
            return this;
        }

        @Override
        public PutBucketLifecycleConfigurationInput build() {
            tracker.validate();
            return new PutBucketLifecycleConfigurationInput(this);
        }

        @Override
        @SuppressWarnings("unchecked")
        public void setMemberValue(Schema member, Object value) {
            switch (member.memberIndex()) {
                case 0 -> bucket((String) SchemaUtils.validateSameMember($SCHEMA_BUCKET, member, value));
                case 1 -> checksumAlgorithm((ChecksumAlgorithm) SchemaUtils.validateSameMember($SCHEMA_CHECKSUM_ALGORITHM, member, value));
                case 2 -> lifecycleConfiguration((BucketLifecycleConfiguration) SchemaUtils.validateSameMember($SCHEMA_LIFECYCLE_CONFIGURATION, member, value));
                case 3 -> expectedBucketOwner((String) SchemaUtils.validateSameMember($SCHEMA_EXPECTED_BUCKET_OWNER, member, value));
                case 4 -> transitionDefaultMinimumObjectSize((TransitionDefaultMinimumObjectSize) SchemaUtils.validateSameMember($SCHEMA_TRANSITION_DEFAULT_MINIMUM_OBJECT_SIZE, member, value));
                default -> ShapeBuilder.super.setMemberValue(member, value);
            }
        }

        @Override
        public ShapeBuilder<PutBucketLifecycleConfigurationInput> errorCorrection() {
            if (tracker.allSet()) {
                return this;
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
                    case 0 -> builder.bucket(de.readString(member));
                    case 1 -> builder.checksumAlgorithm(ChecksumAlgorithm.builder().deserializeMember(de, member).build());
                    case 2 -> builder.lifecycleConfiguration(BucketLifecycleConfiguration.builder().deserializeMember(de, member).build());
                    case 3 -> builder.expectedBucketOwner(de.readString(member));
                    case 4 -> builder.transitionDefaultMinimumObjectSize(TransitionDefaultMinimumObjectSize.builder().deserializeMember(de, member).build());
                    default -> throw new IllegalArgumentException("Unexpected member: " + member.memberName());
                }
            }
        }
    }
}
