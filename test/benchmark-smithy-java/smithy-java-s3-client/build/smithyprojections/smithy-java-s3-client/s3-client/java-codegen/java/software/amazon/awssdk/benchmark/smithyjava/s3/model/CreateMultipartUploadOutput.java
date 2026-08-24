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

@SmithyGenerated
public final class CreateMultipartUploadOutput implements SerializableStruct {

    public static final Schema $SCHEMA = Schemas1.CREATE_MULTIPART_UPLOAD_OUTPUT;
    private static final Schema $SCHEMA_ABORT_DATE = $SCHEMA.member("AbortDate");
    private static final Schema $SCHEMA_ABORT_RULE_ID = $SCHEMA.member("AbortRuleId");
    private static final Schema $SCHEMA_BUCKET = $SCHEMA.member("Bucket");
    private static final Schema $SCHEMA_KEY = $SCHEMA.member("Key");
    private static final Schema $SCHEMA_UPLOAD_ID = $SCHEMA.member("UploadId");
    private static final Schema $SCHEMA_SERVER_SIDE_ENCRYPTION = $SCHEMA.member("ServerSideEncryption");
    private static final Schema $SCHEMA_SSE_CUSTOMER_ALGORITHM = $SCHEMA.member("SSECustomerAlgorithm");
    private static final Schema $SCHEMA_SSE_CUSTOMER_KEYM_D5 = $SCHEMA.member("SSECustomerKeyMD5");
    private static final Schema $SCHEMA_SSEKMS_KEY_ID = $SCHEMA.member("SSEKMSKeyId");
    private static final Schema $SCHEMA_SSEKMS_ENCRYPTION_CONTEXT = $SCHEMA.member("SSEKMSEncryptionContext");
    private static final Schema $SCHEMA_BUCKET_KEY_ENABLED = $SCHEMA.member("BucketKeyEnabled");
    private static final Schema $SCHEMA_REQUEST_CHARGED = $SCHEMA.member("RequestCharged");
    private static final Schema $SCHEMA_CHECKSUM_ALGORITHM = $SCHEMA.member("ChecksumAlgorithm");
    private static final Schema $SCHEMA_CHECKSUM_TYPE = $SCHEMA.member("ChecksumType");

    public static final ShapeId $ID = $SCHEMA.id();

    private final transient Instant abortDate;
    private final transient String abortRuleId;
    private final transient String bucket;
    private final transient String key;
    private final transient String uploadId;
    private final transient ServerSideEncryption serverSideEncryption;
    private final transient String sseCustomerAlgorithm;
    private final transient String sseCustomerKeymD5;
    private final transient String ssekmsKeyId;
    private final transient String ssekmsEncryptionContext;
    private final transient Boolean bucketKeyEnabled;
    private final transient RequestCharged requestCharged;
    private final transient ChecksumAlgorithm checksumAlgorithm;
    private final transient ChecksumType checksumType;

    private CreateMultipartUploadOutput(Builder builder) {
        this.abortDate = builder.abortDate;
        this.abortRuleId = builder.abortRuleId;
        this.bucket = builder.bucket;
        this.key = builder.key;
        this.uploadId = builder.uploadId;
        this.serverSideEncryption = builder.serverSideEncryption;
        this.sseCustomerAlgorithm = builder.sseCustomerAlgorithm;
        this.sseCustomerKeymD5 = builder.sseCustomerKeymD5;
        this.ssekmsKeyId = builder.ssekmsKeyId;
        this.ssekmsEncryptionContext = builder.ssekmsEncryptionContext;
        this.bucketKeyEnabled = builder.bucketKeyEnabled;
        this.requestCharged = builder.requestCharged;
        this.checksumAlgorithm = builder.checksumAlgorithm;
        this.checksumType = builder.checksumType;
    }

    /**
     * If the bucket has a lifecycle rule configured with an action to abort incomplete multipart uploads and the prefix
     * in the lifecycle rule matches the object name in the request, the response includes this header. The header
     * indicates when the initiated multipart upload becomes eligible for an abort operation. For more information, see <a href="https://docs.aws.amazon.com/AmazonS3/latest/dev/mpuoverview.html#mpu-abort-incomplete-mpu-lifecycle-config">
     * Aborting Incomplete Multipart Uploads Using a Bucket Lifecycle Configuration</a> in the <i>Amazon S3 User Guide</i>
     * .
     *
     * <p>The response also includes the <code>x-amz-abort-rule-id</code> header that provides the ID of the lifecycle
     * configuration rule that defines the abort action.
     *
     * <p>This functionality is not supported for directory buckets.
     */
    public Instant getAbortDate() {
        return abortDate;
    }

    /**
     * This header is returned along with the <code>x-amz-abort-date</code> header. It identifies the applicable
     * lifecycle configuration rule that defines the action to abort incomplete multipart uploads.
     *
     * <p>This functionality is not supported for directory buckets.
     */
    public String getAbortRuleId() {
        return abortRuleId;
    }

    /**
     * The name of the bucket to which the multipart upload was initiated. Does not return the access point ARN or
     * access point alias if used.
     *
     * <p>Access points are not supported by directory buckets.
     */
    public String getBucket() {
        return bucket;
    }

    /**
     * Object key for which the multipart upload was initiated.
     */
    public String getKey() {
        return key;
    }

    /**
     * ID for the initiated multipart upload.
     */
    public String getUploadId() {
        return uploadId;
    }

    /**
     * The server-side encryption algorithm used when you store this object in Amazon S3 or Amazon FSx.
     *
     * <p>When accessing data stored in Amazon FSx file systems using S3 access points, the only valid server side
     * encryption option is <code>aws:fsx</code>.
     */
    public ServerSideEncryption getServerSideEncryption() {
        return serverSideEncryption;
    }

    /**
     * If server-side encryption with a customer-provided encryption key was requested, the response will include this
     * header to confirm the encryption algorithm that's used.
     *
     * <p>This functionality is not supported for directory buckets.
     */
    public String getSseCustomerAlgorithm() {
        return sseCustomerAlgorithm;
    }

    /**
     * If server-side encryption with a customer-provided encryption key was requested, the response will include this
     * header to provide the round-trip message integrity verification of the customer-provided encryption key.
     *
     * <p>This functionality is not supported for directory buckets.
     */
    public String getSseCustomerKeymD5() {
        return sseCustomerKeymD5;
    }

    /**
     * If present, indicates the ID of the KMS key that was used for object encryption.
     */
    public String getSsekmsKeyId() {
        return ssekmsKeyId;
    }

    /**
     * If present, indicates the Amazon Web Services KMS Encryption Context to use for object encryption. The value of
     * this header is a Base64 encoded string of a UTF-8 encoded JSON, which contains the encryption context as
     * key-value pairs.
     */
    public String getSsekmsEncryptionContext() {
        return ssekmsEncryptionContext;
    }

    /**
     * Indicates whether the multipart upload uses an S3 Bucket Key for server-side encryption with Key Management
     * Service (KMS) keys (SSE-KMS).
     */
    public Boolean isBucketKeyEnabled() {
        return bucketKeyEnabled;
    }

    public RequestCharged getRequestCharged() {
        return requestCharged;
    }

    /**
     * The algorithm that was used to create a checksum of the object.
     */
    public ChecksumAlgorithm getChecksumAlgorithm() {
        return checksumAlgorithm;
    }

    /**
     * Indicates the checksum type that you want Amazon S3 to use to calculate the object’s checksum value. For more
     * information, see <a
     * href="https://docs.aws.amazon.com/AmazonS3/latest/userguide/checking-object-integrity.html">Checking object
     * integrity in the Amazon S3 User Guide</a>.
     */
    public ChecksumType getChecksumType() {
        return checksumType;
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
        CreateMultipartUploadOutput that = (CreateMultipartUploadOutput) other;
        return Objects.equals(this.bucketKeyEnabled, that.bucketKeyEnabled)
               && Objects.equals(this.abortRuleId, that.abortRuleId)
               && Objects.equals(this.bucket, that.bucket)
               && Objects.equals(this.key, that.key)
               && Objects.equals(this.uploadId, that.uploadId)
               && Objects.equals(this.sseCustomerAlgorithm, that.sseCustomerAlgorithm)
               && Objects.equals(this.sseCustomerKeymD5, that.sseCustomerKeymD5)
               && Objects.equals(this.ssekmsKeyId, that.ssekmsKeyId)
               && Objects.equals(this.ssekmsEncryptionContext, that.ssekmsEncryptionContext)
               && Objects.equals(this.serverSideEncryption, that.serverSideEncryption)
               && Objects.equals(this.requestCharged, that.requestCharged)
               && Objects.equals(this.checksumAlgorithm, that.checksumAlgorithm)
               && Objects.equals(this.checksumType, that.checksumType)
               && Objects.equals(this.abortDate, that.abortDate);
    }

    @Override
    public int hashCode() {
        int $hc = Objects.hashCode(abortDate);
        $hc = 31 * $hc + Objects.hashCode(abortRuleId);
        $hc = 31 * $hc + Objects.hashCode(bucket);
        $hc = 31 * $hc + Objects.hashCode(key);
        $hc = 31 * $hc + Objects.hashCode(uploadId);
        $hc = 31 * $hc + Objects.hashCode(serverSideEncryption);
        $hc = 31 * $hc + Objects.hashCode(sseCustomerAlgorithm);
        $hc = 31 * $hc + Objects.hashCode(sseCustomerKeymD5);
        $hc = 31 * $hc + Objects.hashCode(ssekmsKeyId);
        $hc = 31 * $hc + Objects.hashCode(ssekmsEncryptionContext);
        $hc = 31 * $hc + Objects.hashCode(bucketKeyEnabled);
        $hc = 31 * $hc + Objects.hashCode(requestCharged);
        $hc = 31 * $hc + Objects.hashCode(checksumAlgorithm);
        $hc = 31 * $hc + Objects.hashCode(checksumType);
        return $hc;
    }

    @Override
    public Schema schema() {
        return $SCHEMA;
    }

    @Override
    public void serializeMembers(ShapeSerializer serializer) {
        if (abortDate != null) {
            serializer.writeTimestamp($SCHEMA_ABORT_DATE, abortDate);
        }
        if (abortRuleId != null) {
            serializer.writeString($SCHEMA_ABORT_RULE_ID, abortRuleId);
        }
        if (bucket != null) {
            serializer.writeString($SCHEMA_BUCKET, bucket);
        }
        if (key != null) {
            serializer.writeString($SCHEMA_KEY, key);
        }
        if (uploadId != null) {
            serializer.writeString($SCHEMA_UPLOAD_ID, uploadId);
        }
        if (serverSideEncryption != null) {
            serializer.writeString($SCHEMA_SERVER_SIDE_ENCRYPTION, serverSideEncryption.getValue());
        }
        if (sseCustomerAlgorithm != null) {
            serializer.writeString($SCHEMA_SSE_CUSTOMER_ALGORITHM, sseCustomerAlgorithm);
        }
        if (sseCustomerKeymD5 != null) {
            serializer.writeString($SCHEMA_SSE_CUSTOMER_KEYM_D5, sseCustomerKeymD5);
        }
        if (ssekmsKeyId != null) {
            serializer.writeString($SCHEMA_SSEKMS_KEY_ID, ssekmsKeyId);
        }
        if (ssekmsEncryptionContext != null) {
            serializer.writeString($SCHEMA_SSEKMS_ENCRYPTION_CONTEXT, ssekmsEncryptionContext);
        }
        if (bucketKeyEnabled != null) {
            serializer.writeBoolean($SCHEMA_BUCKET_KEY_ENABLED, bucketKeyEnabled);
        }
        if (requestCharged != null) {
            serializer.writeString($SCHEMA_REQUEST_CHARGED, requestCharged.getValue());
        }
        if (checksumAlgorithm != null) {
            serializer.writeString($SCHEMA_CHECKSUM_ALGORITHM, checksumAlgorithm.getValue());
        }
        if (checksumType != null) {
            serializer.writeString($SCHEMA_CHECKSUM_TYPE, checksumType.getValue());
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T getMemberValue(Schema member) {
        return switch (member.memberIndex()) {
            case 0 -> (T) SchemaUtils.validateSameMember($SCHEMA_ABORT_DATE, member, abortDate);
            case 1 -> (T) SchemaUtils.validateSameMember($SCHEMA_ABORT_RULE_ID, member, abortRuleId);
            case 2 -> (T) SchemaUtils.validateSameMember($SCHEMA_BUCKET, member, bucket);
            case 3 -> (T) SchemaUtils.validateSameMember($SCHEMA_KEY, member, key);
            case 4 -> (T) SchemaUtils.validateSameMember($SCHEMA_UPLOAD_ID, member, uploadId);
            case 5 -> (T) SchemaUtils.validateSameMember($SCHEMA_SERVER_SIDE_ENCRYPTION, member, serverSideEncryption);
            case 6 -> (T) SchemaUtils.validateSameMember($SCHEMA_SSE_CUSTOMER_ALGORITHM, member, sseCustomerAlgorithm);
            case 7 -> (T) SchemaUtils.validateSameMember($SCHEMA_SSE_CUSTOMER_KEYM_D5, member, sseCustomerKeymD5);
            case 8 -> (T) SchemaUtils.validateSameMember($SCHEMA_SSEKMS_KEY_ID, member, ssekmsKeyId);
            case 9 -> (T) SchemaUtils.validateSameMember($SCHEMA_SSEKMS_ENCRYPTION_CONTEXT, member, ssekmsEncryptionContext);
            case 10 -> (T) SchemaUtils.validateSameMember($SCHEMA_BUCKET_KEY_ENABLED, member, bucketKeyEnabled);
            case 11 -> (T) SchemaUtils.validateSameMember($SCHEMA_REQUEST_CHARGED, member, requestCharged);
            case 12 -> (T) SchemaUtils.validateSameMember($SCHEMA_CHECKSUM_ALGORITHM, member, checksumAlgorithm);
            case 13 -> (T) SchemaUtils.validateSameMember($SCHEMA_CHECKSUM_TYPE, member, checksumType);
            default -> throw new IllegalArgumentException("Attempted to get non-existent member: " + member.id());
        };
    }

    /**
     * Create a new builder containing all the current property values of this object.
     *
     * <p><strong>Note:</strong> This method performs only a shallow copy of the original properties.
     *
     * @return a builder for {@link CreateMultipartUploadOutput}.
     */
    public Builder toBuilder() {
        var builder = new Builder();
        builder.abortDate(this.abortDate);
        builder.abortRuleId(this.abortRuleId);
        builder.bucket(this.bucket);
        builder.key(this.key);
        builder.uploadId(this.uploadId);
        builder.serverSideEncryption(this.serverSideEncryption);
        builder.sseCustomerAlgorithm(this.sseCustomerAlgorithm);
        builder.sseCustomerKeymD5(this.sseCustomerKeymD5);
        builder.ssekmsKeyId(this.ssekmsKeyId);
        builder.ssekmsEncryptionContext(this.ssekmsEncryptionContext);
        builder.bucketKeyEnabled(this.bucketKeyEnabled);
        builder.requestCharged(this.requestCharged);
        builder.checksumAlgorithm(this.checksumAlgorithm);
        builder.checksumType(this.checksumType);
        return builder;
    }

    /**
     * @return returns a new Builder.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for {@link CreateMultipartUploadOutput}.
     */
    public static final class Builder implements ShapeBuilder<CreateMultipartUploadOutput> {
        private Instant abortDate;
        private String abortRuleId;
        private String bucket;
        private String key;
        private String uploadId;
        private ServerSideEncryption serverSideEncryption;
        private String sseCustomerAlgorithm;
        private String sseCustomerKeymD5;
        private String ssekmsKeyId;
        private String ssekmsEncryptionContext;
        private Boolean bucketKeyEnabled;
        private RequestCharged requestCharged;
        private ChecksumAlgorithm checksumAlgorithm;
        private ChecksumType checksumType;

        private Builder() {}

        @Override
        public Schema schema() {
            return $SCHEMA;
        }

        /**
         * If the bucket has a lifecycle rule configured with an action to abort incomplete multipart uploads and the prefix
         * in the lifecycle rule matches the object name in the request, the response includes this header. The header
         * indicates when the initiated multipart upload becomes eligible for an abort operation. For more information, see <a href="https://docs.aws.amazon.com/AmazonS3/latest/dev/mpuoverview.html#mpu-abort-incomplete-mpu-lifecycle-config">
         * Aborting Incomplete Multipart Uploads Using a Bucket Lifecycle Configuration</a> in the <i>Amazon S3 User Guide</i>
         * .
         *
         * <p>The response also includes the <code>x-amz-abort-rule-id</code> header that provides the ID of the lifecycle
         * configuration rule that defines the abort action.
         *
         * <p>This functionality is not supported for directory buckets.
         *
         * @return this builder.
         */
        public Builder abortDate(Instant abortDate) {
            this.abortDate = abortDate;
            return this;
        }

        /**
         * This header is returned along with the <code>x-amz-abort-date</code> header. It identifies the applicable
         * lifecycle configuration rule that defines the action to abort incomplete multipart uploads.
         *
         * <p>This functionality is not supported for directory buckets.
         *
         * @return this builder.
         */
        public Builder abortRuleId(String abortRuleId) {
            this.abortRuleId = abortRuleId;
            return this;
        }

        /**
         * The name of the bucket to which the multipart upload was initiated. Does not return the access point ARN or
         * access point alias if used.
         *
         * <p>Access points are not supported by directory buckets.
         *
         * @return this builder.
         */
        public Builder bucket(String bucket) {
            this.bucket = bucket;
            return this;
        }

        /**
         * Object key for which the multipart upload was initiated.
         *
         * @return this builder.
         */
        public Builder key(String key) {
            this.key = key;
            return this;
        }

        /**
         * ID for the initiated multipart upload.
         *
         * @return this builder.
         */
        public Builder uploadId(String uploadId) {
            this.uploadId = uploadId;
            return this;
        }

        /**
         * The server-side encryption algorithm used when you store this object in Amazon S3 or Amazon FSx.
         *
         * <p>When accessing data stored in Amazon FSx file systems using S3 access points, the only valid server side
         * encryption option is <code>aws:fsx</code>.
         *
         * @return this builder.
         */
        public Builder serverSideEncryption(ServerSideEncryption serverSideEncryption) {
            this.serverSideEncryption = serverSideEncryption;
            return this;
        }

        /**
         * If server-side encryption with a customer-provided encryption key was requested, the response will include this
         * header to confirm the encryption algorithm that's used.
         *
         * <p>This functionality is not supported for directory buckets.
         *
         * @return this builder.
         */
        public Builder sseCustomerAlgorithm(String sseCustomerAlgorithm) {
            this.sseCustomerAlgorithm = sseCustomerAlgorithm;
            return this;
        }

        /**
         * If server-side encryption with a customer-provided encryption key was requested, the response will include this
         * header to provide the round-trip message integrity verification of the customer-provided encryption key.
         *
         * <p>This functionality is not supported for directory buckets.
         *
         * @return this builder.
         */
        public Builder sseCustomerKeymD5(String sseCustomerKeymD5) {
            this.sseCustomerKeymD5 = sseCustomerKeymD5;
            return this;
        }

        /**
         * If present, indicates the ID of the KMS key that was used for object encryption.
         *
         * @return this builder.
         */
        public Builder ssekmsKeyId(String ssekmsKeyId) {
            this.ssekmsKeyId = ssekmsKeyId;
            return this;
        }

        /**
         * If present, indicates the Amazon Web Services KMS Encryption Context to use for object encryption. The value of
         * this header is a Base64 encoded string of a UTF-8 encoded JSON, which contains the encryption context as
         * key-value pairs.
         *
         * @return this builder.
         */
        public Builder ssekmsEncryptionContext(String ssekmsEncryptionContext) {
            this.ssekmsEncryptionContext = ssekmsEncryptionContext;
            return this;
        }

        /**
         * Indicates whether the multipart upload uses an S3 Bucket Key for server-side encryption with Key Management
         * Service (KMS) keys (SSE-KMS).
         *
         * @return this builder.
         */
        public Builder bucketKeyEnabled(Boolean bucketKeyEnabled) {
            this.bucketKeyEnabled = bucketKeyEnabled;
            return this;
        }

        /**
         * @return this builder.
         */
        public Builder requestCharged(RequestCharged requestCharged) {
            this.requestCharged = requestCharged;
            return this;
        }

        /**
         * The algorithm that was used to create a checksum of the object.
         *
         * @return this builder.
         */
        public Builder checksumAlgorithm(ChecksumAlgorithm checksumAlgorithm) {
            this.checksumAlgorithm = checksumAlgorithm;
            return this;
        }

        /**
         * Indicates the checksum type that you want Amazon S3 to use to calculate the object’s checksum value. For more
         * information, see <a
         * href="https://docs.aws.amazon.com/AmazonS3/latest/userguide/checking-object-integrity.html">Checking object
         * integrity in the Amazon S3 User Guide</a>.
         *
         * @return this builder.
         */
        public Builder checksumType(ChecksumType checksumType) {
            this.checksumType = checksumType;
            return this;
        }

        @Override
        public CreateMultipartUploadOutput build() {
            return new CreateMultipartUploadOutput(this);
        }

        @Override
        @SuppressWarnings("unchecked")
        public void setMemberValue(Schema member, Object value) {
            switch (member.memberIndex()) {
                case 0 -> abortDate((Instant) SchemaUtils.validateSameMember($SCHEMA_ABORT_DATE, member, value));
                case 1 -> abortRuleId((String) SchemaUtils.validateSameMember($SCHEMA_ABORT_RULE_ID, member, value));
                case 2 -> bucket((String) SchemaUtils.validateSameMember($SCHEMA_BUCKET, member, value));
                case 3 -> key((String) SchemaUtils.validateSameMember($SCHEMA_KEY, member, value));
                case 4 -> uploadId((String) SchemaUtils.validateSameMember($SCHEMA_UPLOAD_ID, member, value));
                case 5 -> serverSideEncryption((ServerSideEncryption) SchemaUtils.validateSameMember($SCHEMA_SERVER_SIDE_ENCRYPTION, member, value));
                case 6 -> sseCustomerAlgorithm((String) SchemaUtils.validateSameMember($SCHEMA_SSE_CUSTOMER_ALGORITHM, member, value));
                case 7 -> sseCustomerKeymD5((String) SchemaUtils.validateSameMember($SCHEMA_SSE_CUSTOMER_KEYM_D5, member, value));
                case 8 -> ssekmsKeyId((String) SchemaUtils.validateSameMember($SCHEMA_SSEKMS_KEY_ID, member, value));
                case 9 -> ssekmsEncryptionContext((String) SchemaUtils.validateSameMember($SCHEMA_SSEKMS_ENCRYPTION_CONTEXT, member, value));
                case 10 -> bucketKeyEnabled((Boolean) SchemaUtils.validateSameMember($SCHEMA_BUCKET_KEY_ENABLED, member, value));
                case 11 -> requestCharged((RequestCharged) SchemaUtils.validateSameMember($SCHEMA_REQUEST_CHARGED, member, value));
                case 12 -> checksumAlgorithm((ChecksumAlgorithm) SchemaUtils.validateSameMember($SCHEMA_CHECKSUM_ALGORITHM, member, value));
                case 13 -> checksumType((ChecksumType) SchemaUtils.validateSameMember($SCHEMA_CHECKSUM_TYPE, member, value));
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
                    case 0 -> builder.abortDate(de.readTimestamp(member));
                    case 1 -> builder.abortRuleId(de.readString(member));
                    case 2 -> builder.bucket(de.readString(member));
                    case 3 -> builder.key(de.readString(member));
                    case 4 -> builder.uploadId(de.readString(member));
                    case 5 -> builder.serverSideEncryption(ServerSideEncryption.builder().deserializeMember(de, member).build());
                    case 6 -> builder.sseCustomerAlgorithm(de.readString(member));
                    case 7 -> builder.sseCustomerKeymD5(de.readString(member));
                    case 8 -> builder.ssekmsKeyId(de.readString(member));
                    case 9 -> builder.ssekmsEncryptionContext(de.readString(member));
                    case 10 -> builder.bucketKeyEnabled(de.readBoolean(member));
                    case 11 -> builder.requestCharged(RequestCharged.builder().deserializeMember(de, member).build());
                    case 12 -> builder.checksumAlgorithm(ChecksumAlgorithm.builder().deserializeMember(de, member).build());
                    case 13 -> builder.checksumType(ChecksumType.builder().deserializeMember(de, member).build());
                    default -> throw new IllegalArgumentException("Unexpected member: " + member.memberName());
                }
            }
        }
    }
}
