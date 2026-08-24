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
public final class PutObjectRetentionInput implements SerializableStruct {

    public static final Schema $SCHEMA = Schemas6.PUT_OBJECT_RETENTION_INPUT;
    private static final Schema $SCHEMA_BUCKET = $SCHEMA.member("Bucket");
    private static final Schema $SCHEMA_KEY = $SCHEMA.member("Key");
    private static final Schema $SCHEMA_RETENTION = $SCHEMA.member("Retention");
    private static final Schema $SCHEMA_REQUEST_PAYER = $SCHEMA.member("RequestPayer");
    private static final Schema $SCHEMA_VERSION_ID = $SCHEMA.member("VersionId");
    private static final Schema $SCHEMA_BYPASS_GOVERNANCE_RETENTION = $SCHEMA.member("BypassGovernanceRetention");
    private static final Schema $SCHEMA_CONTENTM_D5 = $SCHEMA.member("ContentMD5");
    private static final Schema $SCHEMA_CHECKSUM_ALGORITHM = $SCHEMA.member("ChecksumAlgorithm");
    private static final Schema $SCHEMA_EXPECTED_BUCKET_OWNER = $SCHEMA.member("ExpectedBucketOwner");

    public static final ShapeId $ID = $SCHEMA.id();

    private final transient String bucket;
    private final transient String key;
    private final transient ObjectLockRetention retention;
    private final transient RequestPayer requestPayer;
    private final transient String versionId;
    private final transient Boolean bypassGovernanceRetention;
    private final transient String contentmD5;
    private final transient ChecksumAlgorithm checksumAlgorithm;
    private final transient String expectedBucketOwner;

    private PutObjectRetentionInput(Builder builder) {
        this.bucket = builder.bucket;
        this.key = builder.key;
        this.retention = builder.retention;
        this.requestPayer = builder.requestPayer;
        this.versionId = builder.versionId;
        this.bypassGovernanceRetention = builder.bypassGovernanceRetention;
        this.contentmD5 = builder.contentmD5;
        this.checksumAlgorithm = builder.checksumAlgorithm;
        this.expectedBucketOwner = builder.expectedBucketOwner;
    }

    /**
     * The bucket name that contains the object you want to apply this Object Retention configuration to.
     *
     * <p><b>Access points</b> - When you use this action with an access point for general purpose buckets, you must
     * provide the alias of the access point in place of the bucket name or specify the access point ARN. When you use
     * this action with an access point for directory buckets, you must provide the access point name in place of the
     * bucket name. When using the access point ARN, you must direct requests to the access point hostname. The access
     * point hostname takes the form <i>AccessPointName</i>-<i>AccountId</i>.s3-accesspoint.<i>Region</i>.amazonaws.com.
     * When using this action with an access point through the Amazon Web Services SDKs, you provide the access point
     * ARN in place of the bucket name. For more information about access point ARNs, see <a href="https://docs.aws.amazon.com/AmazonS3/latest/userguide/using-access-points.html">Using access points</a> in
     * the <i>Amazon S3 User Guide</i>.
     */
    public String getBucket() {
        return bucket;
    }

    /**
     * The key name for the object that you want to apply this Object Retention configuration to.
     */
    public String getKey() {
        return key;
    }

    /**
     * The container element for the Object Retention configuration.
     */
    public ObjectLockRetention getRetention() {
        return retention;
    }

    public RequestPayer getRequestPayer() {
        return requestPayer;
    }

    /**
     * The version ID for the object that you want to apply this Object Retention configuration to.
     */
    public String getVersionId() {
        return versionId;
    }

    /**
     * Indicates whether this action should bypass Governance-mode restrictions.
     */
    public Boolean isBypassGovernanceRetention() {
        return bypassGovernanceRetention;
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
        PutObjectRetentionInput that = (PutObjectRetentionInput) other;
        return Objects.equals(this.bypassGovernanceRetention, that.bypassGovernanceRetention)
               && Objects.equals(this.bucket, that.bucket)
               && Objects.equals(this.key, that.key)
               && Objects.equals(this.versionId, that.versionId)
               && Objects.equals(this.contentmD5, that.contentmD5)
               && Objects.equals(this.expectedBucketOwner, that.expectedBucketOwner)
               && Objects.equals(this.requestPayer, that.requestPayer)
               && Objects.equals(this.checksumAlgorithm, that.checksumAlgorithm)
               && Objects.equals(this.retention, that.retention);
    }

    @Override
    public int hashCode() {
        int $hc = Objects.hashCode(bucket);
        $hc = 31 * $hc + Objects.hashCode(key);
        $hc = 31 * $hc + Objects.hashCode(retention);
        $hc = 31 * $hc + Objects.hashCode(requestPayer);
        $hc = 31 * $hc + Objects.hashCode(versionId);
        $hc = 31 * $hc + Objects.hashCode(bypassGovernanceRetention);
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
        serializer.writeString($SCHEMA_KEY, key);
        if (retention != null) {
            serializer.writeStruct($SCHEMA_RETENTION, retention);
        }
        if (requestPayer != null) {
            serializer.writeString($SCHEMA_REQUEST_PAYER, requestPayer.getValue());
        }
        if (versionId != null) {
            serializer.writeString($SCHEMA_VERSION_ID, versionId);
        }
        if (bypassGovernanceRetention != null) {
            serializer.writeBoolean($SCHEMA_BYPASS_GOVERNANCE_RETENTION, bypassGovernanceRetention);
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
            case 1 -> (T) SchemaUtils.validateSameMember($SCHEMA_KEY, member, key);
            case 2 -> (T) SchemaUtils.validateSameMember($SCHEMA_RETENTION, member, retention);
            case 3 -> (T) SchemaUtils.validateSameMember($SCHEMA_REQUEST_PAYER, member, requestPayer);
            case 4 -> (T) SchemaUtils.validateSameMember($SCHEMA_VERSION_ID, member, versionId);
            case 5 -> (T) SchemaUtils.validateSameMember($SCHEMA_BYPASS_GOVERNANCE_RETENTION, member, bypassGovernanceRetention);
            case 6 -> (T) SchemaUtils.validateSameMember($SCHEMA_CONTENTM_D5, member, contentmD5);
            case 7 -> (T) SchemaUtils.validateSameMember($SCHEMA_CHECKSUM_ALGORITHM, member, checksumAlgorithm);
            case 8 -> (T) SchemaUtils.validateSameMember($SCHEMA_EXPECTED_BUCKET_OWNER, member, expectedBucketOwner);
            default -> throw new IllegalArgumentException("Attempted to get non-existent member: " + member.id());
        };
    }

    /**
     * Create a new builder containing all the current property values of this object.
     *
     * <p><strong>Note:</strong> This method performs only a shallow copy of the original properties.
     *
     * @return a builder for {@link PutObjectRetentionInput}.
     */
    public Builder toBuilder() {
        var builder = new Builder();
        builder.bucket(this.bucket);
        builder.key(this.key);
        builder.retention(this.retention);
        builder.requestPayer(this.requestPayer);
        builder.versionId(this.versionId);
        builder.bypassGovernanceRetention(this.bypassGovernanceRetention);
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
     * Builder for {@link PutObjectRetentionInput}.
     */
    public static final class Builder implements ShapeBuilder<PutObjectRetentionInput> {
        private final PresenceTracker tracker = PresenceTracker.of($SCHEMA);
        private String bucket;
        private String key;
        private ObjectLockRetention retention;
        private RequestPayer requestPayer;
        private String versionId;
        private Boolean bypassGovernanceRetention;
        private String contentmD5;
        private ChecksumAlgorithm checksumAlgorithm;
        private String expectedBucketOwner;

        private Builder() {}

        @Override
        public Schema schema() {
            return $SCHEMA;
        }

        /**
         * The bucket name that contains the object you want to apply this Object Retention configuration to.
         *
         * <p><b>Access points</b> - When you use this action with an access point for general purpose buckets, you must
         * provide the alias of the access point in place of the bucket name or specify the access point ARN. When you use
         * this action with an access point for directory buckets, you must provide the access point name in place of the
         * bucket name. When using the access point ARN, you must direct requests to the access point hostname. The access
         * point hostname takes the form <i>AccessPointName</i>-<i>AccountId</i>.s3-accesspoint.<i>Region</i>.amazonaws.com.
         * When using this action with an access point through the Amazon Web Services SDKs, you provide the access point
         * ARN in place of the bucket name. For more information about access point ARNs, see <a href="https://docs.aws.amazon.com/AmazonS3/latest/userguide/using-access-points.html">Using access points</a> in
         * the <i>Amazon S3 User Guide</i>.
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
         * The key name for the object that you want to apply this Object Retention configuration to.
         *
         * <p><strong>Required</strong>
         * @return this builder.
         */
        public Builder key(String key) {
            this.key = Objects.requireNonNull(key, "key cannot be null");
            tracker.setMember($SCHEMA_KEY);
            return this;
        }

        /**
         * The container element for the Object Retention configuration.
         *
         * @return this builder.
         */
        public Builder retention(ObjectLockRetention retention) {
            this.retention = retention;
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
         * The version ID for the object that you want to apply this Object Retention configuration to.
         *
         * @return this builder.
         */
        public Builder versionId(String versionId) {
            this.versionId = versionId;
            return this;
        }

        /**
         * Indicates whether this action should bypass Governance-mode restrictions.
         *
         * @return this builder.
         */
        public Builder bypassGovernanceRetention(Boolean bypassGovernanceRetention) {
            this.bypassGovernanceRetention = bypassGovernanceRetention;
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
        public PutObjectRetentionInput build() {
            tracker.validate();
            return new PutObjectRetentionInput(this);
        }

        @Override
        @SuppressWarnings("unchecked")
        public void setMemberValue(Schema member, Object value) {
            switch (member.memberIndex()) {
                case 0 -> bucket((String) SchemaUtils.validateSameMember($SCHEMA_BUCKET, member, value));
                case 1 -> key((String) SchemaUtils.validateSameMember($SCHEMA_KEY, member, value));
                case 2 -> retention((ObjectLockRetention) SchemaUtils.validateSameMember($SCHEMA_RETENTION, member, value));
                case 3 -> requestPayer((RequestPayer) SchemaUtils.validateSameMember($SCHEMA_REQUEST_PAYER, member, value));
                case 4 -> versionId((String) SchemaUtils.validateSameMember($SCHEMA_VERSION_ID, member, value));
                case 5 -> bypassGovernanceRetention((Boolean) SchemaUtils.validateSameMember($SCHEMA_BYPASS_GOVERNANCE_RETENTION, member, value));
                case 6 -> contentmD5((String) SchemaUtils.validateSameMember($SCHEMA_CONTENTM_D5, member, value));
                case 7 -> checksumAlgorithm((ChecksumAlgorithm) SchemaUtils.validateSameMember($SCHEMA_CHECKSUM_ALGORITHM, member, value));
                case 8 -> expectedBucketOwner((String) SchemaUtils.validateSameMember($SCHEMA_EXPECTED_BUCKET_OWNER, member, value));
                default -> ShapeBuilder.super.setMemberValue(member, value);
            }
        }

        @Override
        public ShapeBuilder<PutObjectRetentionInput> errorCorrection() {
            if (tracker.allSet()) {
                return this;
            }
            if (!tracker.checkMember($SCHEMA_BUCKET)) {
                bucket("");
            }
            if (!tracker.checkMember($SCHEMA_KEY)) {
                key("");
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
                    case 1 -> builder.key(de.readString(member));
                    case 2 -> builder.retention(ObjectLockRetention.builder().deserializeMember(de, member).build());
                    case 3 -> builder.requestPayer(RequestPayer.builder().deserializeMember(de, member).build());
                    case 4 -> builder.versionId(de.readString(member));
                    case 5 -> builder.bypassGovernanceRetention(de.readBoolean(member));
                    case 6 -> builder.contentmD5(de.readString(member));
                    case 7 -> builder.checksumAlgorithm(ChecksumAlgorithm.builder().deserializeMember(de, member).build());
                    case 8 -> builder.expectedBucketOwner(de.readString(member));
                    default -> throw new IllegalArgumentException("Unexpected member: " + member.memberName());
                }
            }
        }
    }
}
