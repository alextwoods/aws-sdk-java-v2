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
public final class UpdateObjectEncryptionInput implements SerializableStruct {

    public static final Schema $SCHEMA = Schemas7.UPDATE_OBJECT_ENCRYPTION_INPUT;
    private static final Schema $SCHEMA_BUCKET = $SCHEMA.member("Bucket");
    private static final Schema $SCHEMA_KEY = $SCHEMA.member("Key");
    private static final Schema $SCHEMA_VERSION_ID = $SCHEMA.member("VersionId");
    private static final Schema $SCHEMA_OBJECT_ENCRYPTION = $SCHEMA.member("ObjectEncryption");
    private static final Schema $SCHEMA_REQUEST_PAYER = $SCHEMA.member("RequestPayer");
    private static final Schema $SCHEMA_EXPECTED_BUCKET_OWNER = $SCHEMA.member("ExpectedBucketOwner");
    private static final Schema $SCHEMA_CONTENTM_D5 = $SCHEMA.member("ContentMD5");
    private static final Schema $SCHEMA_CHECKSUM_ALGORITHM = $SCHEMA.member("ChecksumAlgorithm");

    public static final ShapeId $ID = $SCHEMA.id();

    private final transient String bucket;
    private final transient String key;
    private final transient String versionId;
    private final transient ObjectEncryption objectEncryption;
    private final transient RequestPayer requestPayer;
    private final transient String expectedBucketOwner;
    private final transient String contentmD5;
    private final transient ChecksumAlgorithm checksumAlgorithm;

    private UpdateObjectEncryptionInput(Builder builder) {
        this.bucket = builder.bucket;
        this.key = builder.key;
        this.versionId = builder.versionId;
        this.objectEncryption = builder.objectEncryption;
        this.requestPayer = builder.requestPayer;
        this.expectedBucketOwner = builder.expectedBucketOwner;
        this.contentmD5 = builder.contentmD5;
        this.checksumAlgorithm = builder.checksumAlgorithm;
    }

    /**
     * The name of the general purpose bucket that contains the specified object key name.
     *
     * <p>When you use this operation with an access point attached to a general purpose bucket, you must either provide
     * the alias of the access point in place of the bucket name or you must specify the access point Amazon Resource
     * Name (ARN). When using the access point ARN, you must direct requests to the access point hostname. The access
     * point hostname takes the form <code><i>AccessPointName</i>-<i>AccountId</i>.s3-accesspoint.<i>Region</i>
     * .amazonaws.com</code>. When using this operation with an access point through the Amazon Web Services SDKs, you
     * provide the access point ARN in place of the bucket name. For more information about access point ARNs, see <a href="https://docs.aws.amazon.com/AmazonS3/latest/userguide/access-points-naming.html">
     * Referencing access points</a> in the <i>Amazon S3 User Guide</i>.
     */
    public String getBucket() {
        return bucket;
    }

    /**
     * The key name of the object that you want to update the server-side encryption type for.
     */
    public String getKey() {
        return key;
    }

    /**
     * The version ID of the object that you want to update the server-side encryption type for.
     */
    public String getVersionId() {
        return versionId;
    }

    /**
     * The updated server-side encryption type for this object. The <code>UpdateObjectEncryption</code> operation
     * supports the SSE-S3 and SSE-KMS encryption types.
     *
     * <p>Valid Values: <code>SSES3</code> | <code>SSEKMS</code>
     */
    public ObjectEncryption getObjectEncryption() {
        return objectEncryption;
    }

    public RequestPayer getRequestPayer() {
        return requestPayer;
    }

    /**
     * The account ID of the expected bucket owner. If the account ID that you provide doesn't match the actual owner of
     * the bucket, the request fails with the HTTP status code <code>403 Forbidden</code> (access denied).
     */
    public String getExpectedBucketOwner() {
        return expectedBucketOwner;
    }

    /**
     * The MD5 hash for the request body. For requests made using the Amazon Web Services Command Line Interface (CLI)
     * or Amazon Web Services SDKs, this field is calculated automatically.
     */
    public String getContentmD5() {
        return contentmD5;
    }

    /**
     * Indicates the algorithm used to create the checksum for the object when you use an Amazon Web Services SDK. This
     * header doesn't provide any additional functionality if you don't use the SDK. When you send this header, there
     * must be a corresponding <code>x-amz-checksum</code> or <code>x-amz-trailer</code> header sent. Otherwise, Amazon
     * S3 fails the request with the HTTP status code <code>400 Bad Request</code>. For more information, see <a href="https://docs.aws.amazon.com/AmazonS3/latest/userguide/checking-object-integrity.html">
     * Checking object integrity </a> in the <i>Amazon S3 User Guide</i>.
     *
     * <p>If you provide an individual checksum, Amazon S3 ignores any provided <code>ChecksumAlgorithm</code>
     * parameter.
     */
    public ChecksumAlgorithm getChecksumAlgorithm() {
        return checksumAlgorithm;
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
        UpdateObjectEncryptionInput that = (UpdateObjectEncryptionInput) other;
        return Objects.equals(this.bucket, that.bucket)
               && Objects.equals(this.key, that.key)
               && Objects.equals(this.versionId, that.versionId)
               && Objects.equals(this.expectedBucketOwner, that.expectedBucketOwner)
               && Objects.equals(this.contentmD5, that.contentmD5)
               && Objects.equals(this.requestPayer, that.requestPayer)
               && Objects.equals(this.checksumAlgorithm, that.checksumAlgorithm)
               && Objects.equals(this.objectEncryption, that.objectEncryption);
    }

    @Override
    public int hashCode() {
        int $hc = Objects.hashCode(bucket);
        $hc = 31 * $hc + Objects.hashCode(key);
        $hc = 31 * $hc + Objects.hashCode(versionId);
        $hc = 31 * $hc + Objects.hashCode(objectEncryption);
        $hc = 31 * $hc + Objects.hashCode(requestPayer);
        $hc = 31 * $hc + Objects.hashCode(expectedBucketOwner);
        $hc = 31 * $hc + Objects.hashCode(contentmD5);
        $hc = 31 * $hc + Objects.hashCode(checksumAlgorithm);
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
        if (versionId != null) {
            serializer.writeString($SCHEMA_VERSION_ID, versionId);
        }
        if (objectEncryption != null) {
            serializer.writeStruct($SCHEMA_OBJECT_ENCRYPTION, objectEncryption);
        }
        if (requestPayer != null) {
            serializer.writeString($SCHEMA_REQUEST_PAYER, requestPayer.getValue());
        }
        if (expectedBucketOwner != null) {
            serializer.writeString($SCHEMA_EXPECTED_BUCKET_OWNER, expectedBucketOwner);
        }
        if (contentmD5 != null) {
            serializer.writeString($SCHEMA_CONTENTM_D5, contentmD5);
        }
        if (checksumAlgorithm != null) {
            serializer.writeString($SCHEMA_CHECKSUM_ALGORITHM, checksumAlgorithm.getValue());
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T getMemberValue(Schema member) {
        return switch (member.memberIndex()) {
            case 0 -> (T) SchemaUtils.validateSameMember($SCHEMA_BUCKET, member, bucket);
            case 1 -> (T) SchemaUtils.validateSameMember($SCHEMA_KEY, member, key);
            case 2 -> (T) SchemaUtils.validateSameMember($SCHEMA_OBJECT_ENCRYPTION, member, objectEncryption);
            case 3 -> (T) SchemaUtils.validateSameMember($SCHEMA_VERSION_ID, member, versionId);
            case 4 -> (T) SchemaUtils.validateSameMember($SCHEMA_REQUEST_PAYER, member, requestPayer);
            case 5 -> (T) SchemaUtils.validateSameMember($SCHEMA_EXPECTED_BUCKET_OWNER, member, expectedBucketOwner);
            case 6 -> (T) SchemaUtils.validateSameMember($SCHEMA_CONTENTM_D5, member, contentmD5);
            case 7 -> (T) SchemaUtils.validateSameMember($SCHEMA_CHECKSUM_ALGORITHM, member, checksumAlgorithm);
            default -> throw new IllegalArgumentException("Attempted to get non-existent member: " + member.id());
        };
    }

    /**
     * Create a new builder containing all the current property values of this object.
     *
     * <p><strong>Note:</strong> This method performs only a shallow copy of the original properties.
     *
     * @return a builder for {@link UpdateObjectEncryptionInput}.
     */
    public Builder toBuilder() {
        var builder = new Builder();
        builder.bucket(this.bucket);
        builder.key(this.key);
        builder.versionId(this.versionId);
        builder.objectEncryption(this.objectEncryption);
        builder.requestPayer(this.requestPayer);
        builder.expectedBucketOwner(this.expectedBucketOwner);
        builder.contentmD5(this.contentmD5);
        builder.checksumAlgorithm(this.checksumAlgorithm);
        return builder;
    }

    /**
     * @return returns a new Builder.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for {@link UpdateObjectEncryptionInput}.
     */
    public static final class Builder implements ShapeBuilder<UpdateObjectEncryptionInput> {
        private final PresenceTracker tracker = PresenceTracker.of($SCHEMA);
        private String bucket;
        private String key;
        private String versionId;
        private ObjectEncryption objectEncryption;
        private RequestPayer requestPayer;
        private String expectedBucketOwner;
        private String contentmD5;
        private ChecksumAlgorithm checksumAlgorithm;

        private Builder() {}

        @Override
        public Schema schema() {
            return $SCHEMA;
        }

        /**
         * The name of the general purpose bucket that contains the specified object key name.
         *
         * <p>When you use this operation with an access point attached to a general purpose bucket, you must either provide
         * the alias of the access point in place of the bucket name or you must specify the access point Amazon Resource
         * Name (ARN). When using the access point ARN, you must direct requests to the access point hostname. The access
         * point hostname takes the form <code><i>AccessPointName</i>-<i>AccountId</i>.s3-accesspoint.<i>Region</i>
         * .amazonaws.com</code>. When using this operation with an access point through the Amazon Web Services SDKs, you
         * provide the access point ARN in place of the bucket name. For more information about access point ARNs, see <a href="https://docs.aws.amazon.com/AmazonS3/latest/userguide/access-points-naming.html">
         * Referencing access points</a> in the <i>Amazon S3 User Guide</i>.
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
         * The key name of the object that you want to update the server-side encryption type for.
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
         * The version ID of the object that you want to update the server-side encryption type for.
         *
         * @return this builder.
         */
        public Builder versionId(String versionId) {
            this.versionId = versionId;
            return this;
        }

        /**
         * The updated server-side encryption type for this object. The <code>UpdateObjectEncryption</code> operation
         * supports the SSE-S3 and SSE-KMS encryption types.
         *
         * <p>Valid Values: <code>SSES3</code> | <code>SSEKMS</code>
         *
         * <p><strong>Required</strong>
         * @return this builder.
         */
        public Builder objectEncryption(ObjectEncryption objectEncryption) {
            this.objectEncryption = Objects.requireNonNull(objectEncryption, "objectEncryption cannot be null");
            tracker.setMember($SCHEMA_OBJECT_ENCRYPTION);
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
         * The account ID of the expected bucket owner. If the account ID that you provide doesn't match the actual owner of
         * the bucket, the request fails with the HTTP status code <code>403 Forbidden</code> (access denied).
         *
         * @return this builder.
         */
        public Builder expectedBucketOwner(String expectedBucketOwner) {
            this.expectedBucketOwner = expectedBucketOwner;
            return this;
        }

        /**
         * The MD5 hash for the request body. For requests made using the Amazon Web Services Command Line Interface (CLI)
         * or Amazon Web Services SDKs, this field is calculated automatically.
         *
         * @return this builder.
         */
        public Builder contentmD5(String contentmD5) {
            this.contentmD5 = contentmD5;
            return this;
        }

        /**
         * Indicates the algorithm used to create the checksum for the object when you use an Amazon Web Services SDK. This
         * header doesn't provide any additional functionality if you don't use the SDK. When you send this header, there
         * must be a corresponding <code>x-amz-checksum</code> or <code>x-amz-trailer</code> header sent. Otherwise, Amazon
         * S3 fails the request with the HTTP status code <code>400 Bad Request</code>. For more information, see <a href="https://docs.aws.amazon.com/AmazonS3/latest/userguide/checking-object-integrity.html">
         * Checking object integrity </a> in the <i>Amazon S3 User Guide</i>.
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

        @Override
        public UpdateObjectEncryptionInput build() {
            tracker.validate();
            return new UpdateObjectEncryptionInput(this);
        }

        @Override
        @SuppressWarnings("unchecked")
        public void setMemberValue(Schema member, Object value) {
            switch (member.memberIndex()) {
                case 0 -> bucket((String) SchemaUtils.validateSameMember($SCHEMA_BUCKET, member, value));
                case 1 -> key((String) SchemaUtils.validateSameMember($SCHEMA_KEY, member, value));
                case 2 -> objectEncryption((ObjectEncryption) SchemaUtils.validateSameMember($SCHEMA_OBJECT_ENCRYPTION, member, value));
                case 3 -> versionId((String) SchemaUtils.validateSameMember($SCHEMA_VERSION_ID, member, value));
                case 4 -> requestPayer((RequestPayer) SchemaUtils.validateSameMember($SCHEMA_REQUEST_PAYER, member, value));
                case 5 -> expectedBucketOwner((String) SchemaUtils.validateSameMember($SCHEMA_EXPECTED_BUCKET_OWNER, member, value));
                case 6 -> contentmD5((String) SchemaUtils.validateSameMember($SCHEMA_CONTENTM_D5, member, value));
                case 7 -> checksumAlgorithm((ChecksumAlgorithm) SchemaUtils.validateSameMember($SCHEMA_CHECKSUM_ALGORITHM, member, value));
                default -> ShapeBuilder.super.setMemberValue(member, value);
            }
        }

        @Override
        public ShapeBuilder<UpdateObjectEncryptionInput> errorCorrection() {
            if (tracker.allSet()) {
                return this;
            }
            if (!tracker.checkMember($SCHEMA_BUCKET)) {
                bucket("");
            }
            if (!tracker.checkMember($SCHEMA_KEY)) {
                key("");
            }
            if (!tracker.checkMember($SCHEMA_OBJECT_ENCRYPTION)) {
                tracker.setMember($SCHEMA_OBJECT_ENCRYPTION);
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
                    case 2 -> builder.objectEncryption(ObjectEncryption.builder().deserializeMember(de, member).build());
                    case 3 -> builder.versionId(de.readString(member));
                    case 4 -> builder.requestPayer(RequestPayer.builder().deserializeMember(de, member).build());
                    case 5 -> builder.expectedBucketOwner(de.readString(member));
                    case 6 -> builder.contentmD5(de.readString(member));
                    case 7 -> builder.checksumAlgorithm(ChecksumAlgorithm.builder().deserializeMember(de, member).build());
                    default -> throw new IllegalArgumentException("Unexpected member: " + member.memberName());
                }
            }
        }
    }
}
