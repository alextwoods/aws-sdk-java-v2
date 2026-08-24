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
public final class PutBucketPolicyInput implements SerializableStruct {

    public static final Schema $SCHEMA = Schemas5.PUT_BUCKET_POLICY_INPUT;
    private static final Schema $SCHEMA_BUCKET = $SCHEMA.member("Bucket");
    private static final Schema $SCHEMA_CONTENTM_D5 = $SCHEMA.member("ContentMD5");
    private static final Schema $SCHEMA_CHECKSUM_ALGORITHM = $SCHEMA.member("ChecksumAlgorithm");
    private static final Schema $SCHEMA_CONFIRM_REMOVE_SELF_BUCKET_ACCESS = $SCHEMA.member("ConfirmRemoveSelfBucketAccess");
    private static final Schema $SCHEMA_POLICY = $SCHEMA.member("Policy");
    private static final Schema $SCHEMA_EXPECTED_BUCKET_OWNER = $SCHEMA.member("ExpectedBucketOwner");

    public static final ShapeId $ID = $SCHEMA.id();

    private final transient String bucket;
    private final transient String contentmD5;
    private final transient ChecksumAlgorithm checksumAlgorithm;
    private final transient Boolean confirmRemoveSelfBucketAccess;
    private final transient String policy;
    private final transient String expectedBucketOwner;

    private PutBucketPolicyInput(Builder builder) {
        this.bucket = builder.bucket;
        this.contentmD5 = builder.contentmD5;
        this.checksumAlgorithm = builder.checksumAlgorithm;
        this.confirmRemoveSelfBucketAccess = builder.confirmRemoveSelfBucketAccess;
        this.policy = builder.policy;
        this.expectedBucketOwner = builder.expectedBucketOwner;
    }

    /**
     * The name of the bucket.
     *
     * <p><b>Directory buckets </b> - When you use this operation with a directory bucket, you must use path-style
     * requests in the format <code>https://s3express-control.<i>region-code</i>.amazonaws.com/<i>bucket-name</i></code>
     * . Virtual-hosted-style requests aren't supported. Directory bucket names must be unique in the chosen Zone
     * (Availability Zone or Local Zone). Bucket names must also follow the format <code><i>bucket-base-name</i>--<i>
     * zone-id</i>--x-s3</code> (for example, <code><i>DOC-EXAMPLE-BUCKET</i>--<i>usw2-az1</i>--x-s3</code>). For
     * information about bucket naming restrictions, see <a href="https://docs.aws.amazon.com/AmazonS3/latest/userguide/directory-bucket-naming-rules.html">Directory bucket naming rules</a> in the <i>Amazon S3 User
     * Guide</i>
     */
    public String getBucket() {
        return bucket;
    }

    /**
     * The MD5 hash of the request body.
     *
     * <p>For requests made using the Amazon Web Services Command Line Interface (CLI) or Amazon Web Services SDKs, this
     * field is calculated automatically.
     *
     * <p>This functionality is not supported for directory buckets.
     */
    public String getContentmD5() {
        return contentmD5;
    }

    /**
     * Indicates the algorithm used to create the checksum for the request when you use the SDK. This header will not
     * provide any additional functionality if you don't use the SDK. When you send this header, there must be a
     * corresponding <code>x-amz-checksum-<i>algorithm</i></code> or <code>x-amz-trailer</code> header sent. Otherwise,
     * Amazon S3 fails the request with the HTTP status code <code>400 Bad Request</code>.
     *
     * <p>For the <code>x-amz-checksum-<i>algorithm</i></code> header, replace <code><i>algorithm</i></code> with the
     * supported algorithm from the following list:
     *
     * <ul>
     *   <li>
     *     <code>CRC32</code>
     *   </li>
     *   <li>
     *     <code>CRC32C</code>
     *   </li>
     *   <li>
     *     <code>CRC64NVME</code>
     *   </li>
     *   <li>
     *     <code>MD5</code>
     *   </li>
     *   <li>
     *     <code>SHA1</code>
     *   </li>
     *   <li>
     *     <code>SHA256</code>
     *   </li>
     *   <li>
     *     <code>SHA512</code>
     *   </li>
     *   <li>
     *     <code>XXHASH3</code>
     *   </li>
     *   <li>
     *     <code>XXHASH64</code>
     *   </li>
     *   <li>
     *     <code>XXHASH128</code>
     *   </li>
     * </ul>
     *
     * <p>For more information, see <a href="https://docs.aws.amazon.com/AmazonS3/latest/userguide/checking-object-integrity.html">Checking object integrity</a> in the <i>Amazon S3 User Guide</i>.
     *
     * <p>If the individual checksum value you provide through <code>x-amz-checksum-<i>algorithm</i></code> doesn't
     * match the checksum algorithm you set through <code>x-amz-sdk-checksum-algorithm</code>, Amazon S3 fails the
     * request with a <code>BadDigest</code> error.
     *
     * <p>For directory buckets, when you use Amazon Web Services SDKs, <code>CRC32</code> is the default checksum
     * algorithm that's used for performance.
     */
    public ChecksumAlgorithm getChecksumAlgorithm() {
        return checksumAlgorithm;
    }

    /**
     * Set this parameter to true to confirm that you want to remove your permissions to change this bucket policy in
     * the future.
     *
     * <p>This functionality is not supported for directory buckets.
     */
    public Boolean isConfirmRemoveSelfBucketAccess() {
        return confirmRemoveSelfBucketAccess;
    }

    /**
     * The bucket policy as a JSON document.
     *
     * <p>For directory buckets, the only IAM action supported in the bucket policy is <code>s3express:CreateSession</code>
     * .
     */
    public String getPolicy() {
        return policy;
    }

    /**
     * The account ID of the expected bucket owner. If the account ID that you provide does not match the actual owner
     * of the bucket, the request fails with the HTTP status code <code>403 Forbidden</code> (access denied).
     *
     * <p>For directory buckets, this header is not supported in this API operation. If you specify this header, the
     * request fails with the HTTP status code <code>501 Not Implemented</code>.
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
        PutBucketPolicyInput that = (PutBucketPolicyInput) other;
        return Objects.equals(this.confirmRemoveSelfBucketAccess, that.confirmRemoveSelfBucketAccess)
               && Objects.equals(this.bucket, that.bucket)
               && Objects.equals(this.contentmD5, that.contentmD5)
               && Objects.equals(this.policy, that.policy)
               && Objects.equals(this.expectedBucketOwner, that.expectedBucketOwner)
               && Objects.equals(this.checksumAlgorithm, that.checksumAlgorithm);
    }

    @Override
    public int hashCode() {
        int $hc = Objects.hashCode(bucket);
        $hc = 31 * $hc + Objects.hashCode(contentmD5);
        $hc = 31 * $hc + Objects.hashCode(checksumAlgorithm);
        $hc = 31 * $hc + Objects.hashCode(confirmRemoveSelfBucketAccess);
        $hc = 31 * $hc + Objects.hashCode(policy);
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
        if (contentmD5 != null) {
            serializer.writeString($SCHEMA_CONTENTM_D5, contentmD5);
        }
        if (checksumAlgorithm != null) {
            serializer.writeString($SCHEMA_CHECKSUM_ALGORITHM, checksumAlgorithm.getValue());
        }
        if (confirmRemoveSelfBucketAccess != null) {
            serializer.writeBoolean($SCHEMA_CONFIRM_REMOVE_SELF_BUCKET_ACCESS, confirmRemoveSelfBucketAccess);
        }
        serializer.writeString($SCHEMA_POLICY, policy);
        if (expectedBucketOwner != null) {
            serializer.writeString($SCHEMA_EXPECTED_BUCKET_OWNER, expectedBucketOwner);
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T getMemberValue(Schema member) {
        return switch (member.memberIndex()) {
            case 0 -> (T) SchemaUtils.validateSameMember($SCHEMA_BUCKET, member, bucket);
            case 1 -> (T) SchemaUtils.validateSameMember($SCHEMA_POLICY, member, policy);
            case 2 -> (T) SchemaUtils.validateSameMember($SCHEMA_CONTENTM_D5, member, contentmD5);
            case 3 -> (T) SchemaUtils.validateSameMember($SCHEMA_CHECKSUM_ALGORITHM, member, checksumAlgorithm);
            case 4 -> (T) SchemaUtils.validateSameMember($SCHEMA_CONFIRM_REMOVE_SELF_BUCKET_ACCESS, member, confirmRemoveSelfBucketAccess);
            case 5 -> (T) SchemaUtils.validateSameMember($SCHEMA_EXPECTED_BUCKET_OWNER, member, expectedBucketOwner);
            default -> throw new IllegalArgumentException("Attempted to get non-existent member: " + member.id());
        };
    }

    /**
     * Create a new builder containing all the current property values of this object.
     *
     * <p><strong>Note:</strong> This method performs only a shallow copy of the original properties.
     *
     * @return a builder for {@link PutBucketPolicyInput}.
     */
    public Builder toBuilder() {
        var builder = new Builder();
        builder.bucket(this.bucket);
        builder.contentmD5(this.contentmD5);
        builder.checksumAlgorithm(this.checksumAlgorithm);
        builder.confirmRemoveSelfBucketAccess(this.confirmRemoveSelfBucketAccess);
        builder.policy(this.policy);
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
     * Builder for {@link PutBucketPolicyInput}.
     */
    public static final class Builder implements ShapeBuilder<PutBucketPolicyInput> {
        private final PresenceTracker tracker = PresenceTracker.of($SCHEMA);
        private String bucket;
        private String contentmD5;
        private ChecksumAlgorithm checksumAlgorithm;
        private Boolean confirmRemoveSelfBucketAccess;
        private String policy;
        private String expectedBucketOwner;

        private Builder() {}

        @Override
        public Schema schema() {
            return $SCHEMA;
        }

        /**
         * The name of the bucket.
         *
         * <p><b>Directory buckets </b> - When you use this operation with a directory bucket, you must use path-style
         * requests in the format <code>https://s3express-control.<i>region-code</i>.amazonaws.com/<i>bucket-name</i></code>
         * . Virtual-hosted-style requests aren't supported. Directory bucket names must be unique in the chosen Zone
         * (Availability Zone or Local Zone). Bucket names must also follow the format <code><i>bucket-base-name</i>--<i>
         * zone-id</i>--x-s3</code> (for example, <code><i>DOC-EXAMPLE-BUCKET</i>--<i>usw2-az1</i>--x-s3</code>). For
         * information about bucket naming restrictions, see <a href="https://docs.aws.amazon.com/AmazonS3/latest/userguide/directory-bucket-naming-rules.html">Directory bucket naming rules</a> in the <i>Amazon S3 User
         * Guide</i>
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
         * The MD5 hash of the request body.
         *
         * <p>For requests made using the Amazon Web Services Command Line Interface (CLI) or Amazon Web Services SDKs, this
         * field is calculated automatically.
         *
         * <p>This functionality is not supported for directory buckets.
         *
         * @return this builder.
         */
        public Builder contentmD5(String contentmD5) {
            this.contentmD5 = contentmD5;
            return this;
        }

        /**
         * Indicates the algorithm used to create the checksum for the request when you use the SDK. This header will not
         * provide any additional functionality if you don't use the SDK. When you send this header, there must be a
         * corresponding <code>x-amz-checksum-<i>algorithm</i></code> or <code>x-amz-trailer</code> header sent. Otherwise,
         * Amazon S3 fails the request with the HTTP status code <code>400 Bad Request</code>.
         *
         * <p>For the <code>x-amz-checksum-<i>algorithm</i></code> header, replace <code><i>algorithm</i></code> with the
         * supported algorithm from the following list:
         *
         * <ul>
         *   <li>
         *     <code>CRC32</code>
         *   </li>
         *   <li>
         *     <code>CRC32C</code>
         *   </li>
         *   <li>
         *     <code>CRC64NVME</code>
         *   </li>
         *   <li>
         *     <code>MD5</code>
         *   </li>
         *   <li>
         *     <code>SHA1</code>
         *   </li>
         *   <li>
         *     <code>SHA256</code>
         *   </li>
         *   <li>
         *     <code>SHA512</code>
         *   </li>
         *   <li>
         *     <code>XXHASH3</code>
         *   </li>
         *   <li>
         *     <code>XXHASH64</code>
         *   </li>
         *   <li>
         *     <code>XXHASH128</code>
         *   </li>
         * </ul>
         *
         * <p>For more information, see <a href="https://docs.aws.amazon.com/AmazonS3/latest/userguide/checking-object-integrity.html">Checking object integrity</a> in the <i>Amazon S3 User Guide</i>.
         *
         * <p>If the individual checksum value you provide through <code>x-amz-checksum-<i>algorithm</i></code> doesn't
         * match the checksum algorithm you set through <code>x-amz-sdk-checksum-algorithm</code>, Amazon S3 fails the
         * request with a <code>BadDigest</code> error.
         *
         * <p>For directory buckets, when you use Amazon Web Services SDKs, <code>CRC32</code> is the default checksum
         * algorithm that's used for performance.
         *
         * @return this builder.
         */
        public Builder checksumAlgorithm(ChecksumAlgorithm checksumAlgorithm) {
            this.checksumAlgorithm = checksumAlgorithm;
            return this;
        }

        /**
         * Set this parameter to true to confirm that you want to remove your permissions to change this bucket policy in
         * the future.
         *
         * <p>This functionality is not supported for directory buckets.
         *
         * @return this builder.
         */
        public Builder confirmRemoveSelfBucketAccess(Boolean confirmRemoveSelfBucketAccess) {
            this.confirmRemoveSelfBucketAccess = confirmRemoveSelfBucketAccess;
            return this;
        }

        /**
         * The bucket policy as a JSON document.
         *
         * <p>For directory buckets, the only IAM action supported in the bucket policy is <code>s3express:CreateSession</code>
         * .
         *
         * <p><strong>Required</strong>
         * @return this builder.
         */
        public Builder policy(String policy) {
            this.policy = Objects.requireNonNull(policy, "policy cannot be null");
            tracker.setMember($SCHEMA_POLICY);
            return this;
        }

        /**
         * The account ID of the expected bucket owner. If the account ID that you provide does not match the actual owner
         * of the bucket, the request fails with the HTTP status code <code>403 Forbidden</code> (access denied).
         *
         * <p>For directory buckets, this header is not supported in this API operation. If you specify this header, the
         * request fails with the HTTP status code <code>501 Not Implemented</code>.
         *
         * @return this builder.
         */
        public Builder expectedBucketOwner(String expectedBucketOwner) {
            this.expectedBucketOwner = expectedBucketOwner;
            return this;
        }

        @Override
        public PutBucketPolicyInput build() {
            tracker.validate();
            return new PutBucketPolicyInput(this);
        }

        @Override
        @SuppressWarnings("unchecked")
        public void setMemberValue(Schema member, Object value) {
            switch (member.memberIndex()) {
                case 0 -> bucket((String) SchemaUtils.validateSameMember($SCHEMA_BUCKET, member, value));
                case 1 -> policy((String) SchemaUtils.validateSameMember($SCHEMA_POLICY, member, value));
                case 2 -> contentmD5((String) SchemaUtils.validateSameMember($SCHEMA_CONTENTM_D5, member, value));
                case 3 -> checksumAlgorithm((ChecksumAlgorithm) SchemaUtils.validateSameMember($SCHEMA_CHECKSUM_ALGORITHM, member, value));
                case 4 -> confirmRemoveSelfBucketAccess((Boolean) SchemaUtils.validateSameMember($SCHEMA_CONFIRM_REMOVE_SELF_BUCKET_ACCESS, member, value));
                case 5 -> expectedBucketOwner((String) SchemaUtils.validateSameMember($SCHEMA_EXPECTED_BUCKET_OWNER, member, value));
                default -> ShapeBuilder.super.setMemberValue(member, value);
            }
        }

        @Override
        public ShapeBuilder<PutBucketPolicyInput> errorCorrection() {
            if (tracker.allSet()) {
                return this;
            }
            if (!tracker.checkMember($SCHEMA_BUCKET)) {
                bucket("");
            }
            if (!tracker.checkMember($SCHEMA_POLICY)) {
                policy("");
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
                    case 1 -> builder.policy(de.readString(member));
                    case 2 -> builder.contentmD5(de.readString(member));
                    case 3 -> builder.checksumAlgorithm(ChecksumAlgorithm.builder().deserializeMember(de, member).build());
                    case 4 -> builder.confirmRemoveSelfBucketAccess(de.readBoolean(member));
                    case 5 -> builder.expectedBucketOwner(de.readString(member));
                    default -> throw new IllegalArgumentException("Unexpected member: " + member.memberName());
                }
            }
        }
    }
}
