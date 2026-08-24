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
public final class PutObjectLockConfigurationInput implements SerializableStruct {

    public static final Schema $SCHEMA = Schemas6.PUT_OBJECT_LOCK_CONFIGURATION_INPUT;
    private static final Schema $SCHEMA_BUCKET = $SCHEMA.member("Bucket");
    private static final Schema $SCHEMA_OBJECT_LOCK_CONFIGURATION = $SCHEMA.member("ObjectLockConfiguration");
    private static final Schema $SCHEMA_REQUEST_PAYER = $SCHEMA.member("RequestPayer");
    private static final Schema $SCHEMA_TOKEN = $SCHEMA.member("Token");
    private static final Schema $SCHEMA_CONTENTM_D5 = $SCHEMA.member("ContentMD5");
    private static final Schema $SCHEMA_CHECKSUM_ALGORITHM = $SCHEMA.member("ChecksumAlgorithm");
    private static final Schema $SCHEMA_EXPECTED_BUCKET_OWNER = $SCHEMA.member("ExpectedBucketOwner");

    public static final ShapeId $ID = $SCHEMA.id();

    private final transient String bucket;
    private final transient ObjectLockConfiguration objectLockConfiguration;
    private final transient RequestPayer requestPayer;
    private final transient String token;
    private final transient String contentmD5;
    private final transient ChecksumAlgorithm checksumAlgorithm;
    private final transient String expectedBucketOwner;

    private PutObjectLockConfigurationInput(Builder builder) {
        this.bucket = builder.bucket;
        this.objectLockConfiguration = builder.objectLockConfiguration;
        this.requestPayer = builder.requestPayer;
        this.token = builder.token;
        this.contentmD5 = builder.contentmD5;
        this.checksumAlgorithm = builder.checksumAlgorithm;
        this.expectedBucketOwner = builder.expectedBucketOwner;
    }

    /**
     * The bucket whose Object Lock configuration you want to create or replace.
     */
    public String getBucket() {
        return bucket;
    }

    /**
     * The Object Lock configuration that you want to apply to the specified bucket.
     */
    public ObjectLockConfiguration getObjectLockConfiguration() {
        return objectLockConfiguration;
    }

    public RequestPayer getRequestPayer() {
        return requestPayer;
    }

    /**
     * A token to allow Object Lock to be enabled for an existing bucket.
     */
    public String getToken() {
        return token;
    }

    /**
     * The MD5 hash for the request body.
     *
     * <p>For requests made using the Amazon Web Services Command Line Interface (CLI) or Amazon Web Services SDKs, this
     * field is calculated automatically.
     */
    public String getContentmD5() {
        return contentmD5;
    }

    /**
     * Indicates the algorithm used to create the checksum for the object when you use the SDK. This header will not
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
     * The account ID of the expected bucket owner. If the account ID that you provide does not match the actual owner
     * of the bucket, the request fails with the HTTP status code <code>403 Forbidden</code> (access denied).
     */
    public String getExpectedBucketOwner() {
        return expectedBucketOwner;
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
        PutObjectLockConfigurationInput that = (PutObjectLockConfigurationInput) other;
        return Objects.equals(this.bucket, that.bucket)
               && Objects.equals(this.token, that.token)
               && Objects.equals(this.contentmD5, that.contentmD5)
               && Objects.equals(this.expectedBucketOwner, that.expectedBucketOwner)
               && Objects.equals(this.requestPayer, that.requestPayer)
               && Objects.equals(this.checksumAlgorithm, that.checksumAlgorithm)
               && Objects.equals(this.objectLockConfiguration, that.objectLockConfiguration);
    }

    @Override
    public int hashCode() {
        int $hc = Objects.hashCode(bucket);
        $hc = 31 * $hc + Objects.hashCode(objectLockConfiguration);
        $hc = 31 * $hc + Objects.hashCode(requestPayer);
        $hc = 31 * $hc + Objects.hashCode(token);
        $hc = 31 * $hc + Objects.hashCode(contentmD5);
        $hc = 31 * $hc + Objects.hashCode(checksumAlgorithm);
        $hc = 31 * $hc + Objects.hashCode(expectedBucketOwner);
        return $hc;
    }

    @Override
    public Schema schema() {
        return $SCHEMA;
    }

    @Override
    public void serializeMembers(ShapeSerializer serializer) {
        serializer.writeString($SCHEMA_BUCKET, bucket);
        if (objectLockConfiguration != null) {
            serializer.writeStruct($SCHEMA_OBJECT_LOCK_CONFIGURATION, objectLockConfiguration);
        }
        if (requestPayer != null) {
            serializer.writeString($SCHEMA_REQUEST_PAYER, requestPayer.getValue());
        }
        if (token != null) {
            serializer.writeString($SCHEMA_TOKEN, token);
        }
        if (contentmD5 != null) {
            serializer.writeString($SCHEMA_CONTENTM_D5, contentmD5);
        }
        if (checksumAlgorithm != null) {
            serializer.writeString($SCHEMA_CHECKSUM_ALGORITHM, checksumAlgorithm.getValue());
        }
        if (expectedBucketOwner != null) {
            serializer.writeString($SCHEMA_EXPECTED_BUCKET_OWNER, expectedBucketOwner);
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T getMemberValue(Schema member) {
        return switch (member.memberIndex()) {
            case 0 -> (T) SchemaUtils.validateSameMember($SCHEMA_BUCKET, member, bucket);
            case 1 -> (T) SchemaUtils.validateSameMember($SCHEMA_OBJECT_LOCK_CONFIGURATION, member, objectLockConfiguration);
            case 2 -> (T) SchemaUtils.validateSameMember($SCHEMA_REQUEST_PAYER, member, requestPayer);
            case 3 -> (T) SchemaUtils.validateSameMember($SCHEMA_TOKEN, member, token);
            case 4 -> (T) SchemaUtils.validateSameMember($SCHEMA_CONTENTM_D5, member, contentmD5);
            case 5 -> (T) SchemaUtils.validateSameMember($SCHEMA_CHECKSUM_ALGORITHM, member, checksumAlgorithm);
            case 6 -> (T) SchemaUtils.validateSameMember($SCHEMA_EXPECTED_BUCKET_OWNER, member, expectedBucketOwner);
            default -> throw new IllegalArgumentException("Attempted to get non-existent member: " + member.id());
        };
    }

    /**
     * Create a new builder containing all the current property values of this object.
     *
     * <p><strong>Note:</strong> This method performs only a shallow copy of the original properties.
     *
     * @return a builder for {@link PutObjectLockConfigurationInput}.
     */
    public Builder toBuilder() {
        var builder = new Builder();
        builder.bucket(this.bucket);
        builder.objectLockConfiguration(this.objectLockConfiguration);
        builder.requestPayer(this.requestPayer);
        builder.token(this.token);
        builder.contentmD5(this.contentmD5);
        builder.checksumAlgorithm(this.checksumAlgorithm);
        builder.expectedBucketOwner(this.expectedBucketOwner);
        return builder;
    }

    /**
     * @return returns a new Builder.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for {@link PutObjectLockConfigurationInput}.
     */
    public static final class Builder implements ShapeBuilder<PutObjectLockConfigurationInput> {
        private final PresenceTracker tracker = PresenceTracker.of($SCHEMA);
        private String bucket;
        private ObjectLockConfiguration objectLockConfiguration;
        private RequestPayer requestPayer;
        private String token;
        private String contentmD5;
        private ChecksumAlgorithm checksumAlgorithm;
        private String expectedBucketOwner;

        private Builder() {}

        @Override
        public Schema schema() {
            return $SCHEMA;
        }

        /**
         * The bucket whose Object Lock configuration you want to create or replace.
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
         * The Object Lock configuration that you want to apply to the specified bucket.
         *
         * @return this builder.
         */
        public Builder objectLockConfiguration(ObjectLockConfiguration objectLockConfiguration) {
            this.objectLockConfiguration = objectLockConfiguration;
            return this;
        }

        /**
         * @return this builder.
         */
        public Builder requestPayer(RequestPayer requestPayer) {
            this.requestPayer = requestPayer;
            return this;
        }

        /**
         * A token to allow Object Lock to be enabled for an existing bucket.
         *
         * @return this builder.
         */
        public Builder token(String token) {
            this.token = token;
            return this;
        }

        /**
         * The MD5 hash for the request body.
         *
         * <p>For requests made using the Amazon Web Services Command Line Interface (CLI) or Amazon Web Services SDKs, this
         * field is calculated automatically.
         *
         * @return this builder.
         */
        public Builder contentmD5(String contentmD5) {
            this.contentmD5 = contentmD5;
            return this;
        }

        /**
         * Indicates the algorithm used to create the checksum for the object when you use the SDK. This header will not
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
         * The account ID of the expected bucket owner. If the account ID that you provide does not match the actual owner
         * of the bucket, the request fails with the HTTP status code <code>403 Forbidden</code> (access denied).
         *
         * @return this builder.
         */
        public Builder expectedBucketOwner(String expectedBucketOwner) {
            this.expectedBucketOwner = expectedBucketOwner;
            return this;
        }

        @Override
        public PutObjectLockConfigurationInput build() {
            tracker.validate();
            return new PutObjectLockConfigurationInput(this);
        }

        @Override
        @SuppressWarnings("unchecked")
        public void setMemberValue(Schema member, Object value) {
            switch (member.memberIndex()) {
                case 0 -> bucket((String) SchemaUtils.validateSameMember($SCHEMA_BUCKET, member, value));
                case 1 -> objectLockConfiguration((ObjectLockConfiguration) SchemaUtils.validateSameMember($SCHEMA_OBJECT_LOCK_CONFIGURATION, member, value));
                case 2 -> requestPayer((RequestPayer) SchemaUtils.validateSameMember($SCHEMA_REQUEST_PAYER, member, value));
                case 3 -> token((String) SchemaUtils.validateSameMember($SCHEMA_TOKEN, member, value));
                case 4 -> contentmD5((String) SchemaUtils.validateSameMember($SCHEMA_CONTENTM_D5, member, value));
                case 5 -> checksumAlgorithm((ChecksumAlgorithm) SchemaUtils.validateSameMember($SCHEMA_CHECKSUM_ALGORITHM, member, value));
                case 6 -> expectedBucketOwner((String) SchemaUtils.validateSameMember($SCHEMA_EXPECTED_BUCKET_OWNER, member, value));
                default -> ShapeBuilder.super.setMemberValue(member, value);
            }
        }

        @Override
        public ShapeBuilder<PutObjectLockConfigurationInput> errorCorrection() {
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
                    case 1 -> builder.objectLockConfiguration(ObjectLockConfiguration.builder().deserializeMember(de, member).build());
                    case 2 -> builder.requestPayer(RequestPayer.builder().deserializeMember(de, member).build());
                    case 3 -> builder.token(de.readString(member));
                    case 4 -> builder.contentmD5(de.readString(member));
                    case 5 -> builder.checksumAlgorithm(ChecksumAlgorithm.builder().deserializeMember(de, member).build());
                    case 6 -> builder.expectedBucketOwner(de.readString(member));
                    default -> throw new IllegalArgumentException("Unexpected member: " + member.memberName());
                }
            }
        }
    }
}
