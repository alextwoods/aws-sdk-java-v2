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
public final class DeleteObjectsInput implements SerializableStruct {

    public static final Schema $SCHEMA = Schemas1.DELETE_OBJECTS_INPUT;
    private static final Schema $SCHEMA_BUCKET = $SCHEMA.member("Bucket");
    private static final Schema $SCHEMA_DELETE = $SCHEMA.member("Delete");
    private static final Schema $SCHEMA_MFA = $SCHEMA.member("MFA");
    private static final Schema $SCHEMA_REQUEST_PAYER = $SCHEMA.member("RequestPayer");
    private static final Schema $SCHEMA_BYPASS_GOVERNANCE_RETENTION = $SCHEMA.member("BypassGovernanceRetention");
    private static final Schema $SCHEMA_EXPECTED_BUCKET_OWNER = $SCHEMA.member("ExpectedBucketOwner");
    private static final Schema $SCHEMA_CHECKSUM_ALGORITHM = $SCHEMA.member("ChecksumAlgorithm");

    public static final ShapeId $ID = $SCHEMA.id();

    private final transient String bucket;
    private final transient Delete delete;
    private final transient String mfa;
    private final transient RequestPayer requestPayer;
    private final transient Boolean bypassGovernanceRetention;
    private final transient String expectedBucketOwner;
    private final transient ChecksumAlgorithm checksumAlgorithm;

    private DeleteObjectsInput(Builder builder) {
        this.bucket = builder.bucket;
        this.delete = builder.delete;
        this.mfa = builder.mfa;
        this.requestPayer = builder.requestPayer;
        this.bypassGovernanceRetention = builder.bypassGovernanceRetention;
        this.expectedBucketOwner = builder.expectedBucketOwner;
        this.checksumAlgorithm = builder.checksumAlgorithm;
    }

    /**
     * The bucket name containing the objects to delete.
     *
     * <p><b>Directory buckets</b> - When you use this operation with a directory bucket, you must use
     * virtual-hosted-style requests in the format <code><i>Bucket-name</i>.s3express-<i>zone-id</i>.<i>region-code</i>
     * .amazonaws.com</code>. Path-style requests are not supported. Directory bucket names must be unique in the chosen
     * Zone (Availability Zone or Local Zone). Bucket names must follow the format <code><i>bucket-base-name</i>--<i>
     * zone-id</i>--x-s3</code> (for example, <code><i>amzn-s3-demo-bucket</i>--<i>usw2-az1</i>--x-s3</code>). For
     * information about bucket naming restrictions, see <a href="https://docs.aws.amazon.com/AmazonS3/latest/userguide/directory-bucket-naming-rules.html">Directory bucket naming rules</a> in the <i>Amazon S3 User
     * Guide</i>.
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
     * <p>Object Lambda access points are not supported by directory buckets.
     *
     * <p><b>S3 on Outposts</b> - When you use this action with S3 on Outposts, you must direct requests to the S3 on
     * Outposts hostname. The S3 on Outposts hostname takes the form <code><i>AccessPointName</i>-<i>AccountId</i>.<i>
     * outpostID</i>.s3-outposts.<i>Region</i>.amazonaws.com</code>. When you use this action with S3 on Outposts, the
     * destination bucket must be the Outposts access point ARN or the access point alias. For more information about S3
     * on Outposts, see <a href="https://docs.aws.amazon.com/AmazonS3/latest/userguide/S3onOutposts.html">What is S3 on Outposts?</a> in the <i>Amazon S3 User Guide</i>.
     */
    public String getBucket() {
        return bucket;
    }

    /**
     * Container for the request.
     */
    public Delete getDelete() {
        return delete;
    }

    /**
     * The concatenation of the authentication device's serial number, a space, and the value that is displayed on your
     * authentication device. Required to permanently delete a versioned object if versioning is configured with MFA
     * delete enabled.
     *
     * <p>When performing the <code>DeleteObjects</code> operation on an MFA delete enabled bucket, which attempts to
     * delete the specified versioned objects, you must include an MFA token. If you don't provide an MFA token, the
     * entire request will fail, even if there are non-versioned objects that you are trying to delete. If you provide
     * an invalid token, whether there are versioned object keys in the request or not, the entire Multi-Object Delete
     * request will fail. For information about MFA Delete, see <a href="https://docs.aws.amazon.com/AmazonS3/latest/dev/Versioning.html#MultiFactorAuthenticationDelete"> MFA Delete</a> in the <i>Amazon S3 User Guide</i>.
     *
     * <p>This functionality is not supported for directory buckets.
     */
    public String getMfa() {
        return mfa;
    }

    public RequestPayer getRequestPayer() {
        return requestPayer;
    }

    /**
     * Specifies whether you want to delete this object even if it has a Governance-type Object Lock in place. To use
     * this header, you must have the <code>s3:BypassGovernanceRetention</code> permission.
     *
     * <p>This functionality is not supported for directory buckets.
     */
    public Boolean isBypassGovernanceRetention() {
        return bypassGovernanceRetention;
    }

    /**
     * The account ID of the expected bucket owner. If the account ID that you provide does not match the actual owner
     * of the bucket, the request fails with the HTTP status code <code>403 Forbidden</code> (access denied).
     */
    public String getExpectedBucketOwner() {
        return expectedBucketOwner;
    }

    /**
     * Indicates the algorithm used to create the checksum for the object when you use the SDK. This header will not
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
        DeleteObjectsInput that = (DeleteObjectsInput) other;
        return Objects.equals(this.bypassGovernanceRetention, that.bypassGovernanceRetention)
               && Objects.equals(this.bucket, that.bucket)
               && Objects.equals(this.mfa, that.mfa)
               && Objects.equals(this.expectedBucketOwner, that.expectedBucketOwner)
               && Objects.equals(this.requestPayer, that.requestPayer)
               && Objects.equals(this.checksumAlgorithm, that.checksumAlgorithm)
               && Objects.equals(this.delete, that.delete);
    }

    @Override
    public int hashCode() {
        int $hc = Objects.hashCode(bucket);
        $hc = 31 * $hc + Objects.hashCode(delete);
        $hc = 31 * $hc + Objects.hashCode(mfa);
        $hc = 31 * $hc + Objects.hashCode(requestPayer);
        $hc = 31 * $hc + Objects.hashCode(bypassGovernanceRetention);
        $hc = 31 * $hc + Objects.hashCode(expectedBucketOwner);
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
        if (delete != null) {
            serializer.writeStruct($SCHEMA_DELETE, delete);
        }
        if (mfa != null) {
            serializer.writeString($SCHEMA_MFA, mfa);
        }
        if (requestPayer != null) {
            serializer.writeString($SCHEMA_REQUEST_PAYER, requestPayer.getValue());
        }
        if (bypassGovernanceRetention != null) {
            serializer.writeBoolean($SCHEMA_BYPASS_GOVERNANCE_RETENTION, bypassGovernanceRetention);
        }
        if (expectedBucketOwner != null) {
            serializer.writeString($SCHEMA_EXPECTED_BUCKET_OWNER, expectedBucketOwner);
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
            case 1 -> (T) SchemaUtils.validateSameMember($SCHEMA_DELETE, member, delete);
            case 2 -> (T) SchemaUtils.validateSameMember($SCHEMA_MFA, member, mfa);
            case 3 -> (T) SchemaUtils.validateSameMember($SCHEMA_REQUEST_PAYER, member, requestPayer);
            case 4 -> (T) SchemaUtils.validateSameMember($SCHEMA_BYPASS_GOVERNANCE_RETENTION, member, bypassGovernanceRetention);
            case 5 -> (T) SchemaUtils.validateSameMember($SCHEMA_EXPECTED_BUCKET_OWNER, member, expectedBucketOwner);
            case 6 -> (T) SchemaUtils.validateSameMember($SCHEMA_CHECKSUM_ALGORITHM, member, checksumAlgorithm);
            default -> throw new IllegalArgumentException("Attempted to get non-existent member: " + member.id());
        };
    }

    /**
     * Create a new builder containing all the current property values of this object.
     *
     * <p><strong>Note:</strong> This method performs only a shallow copy of the original properties.
     *
     * @return a builder for {@link DeleteObjectsInput}.
     */
    public Builder toBuilder() {
        var builder = new Builder();
        builder.bucket(this.bucket);
        builder.delete(this.delete);
        builder.mfa(this.mfa);
        builder.requestPayer(this.requestPayer);
        builder.bypassGovernanceRetention(this.bypassGovernanceRetention);
        builder.expectedBucketOwner(this.expectedBucketOwner);
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
     * Builder for {@link DeleteObjectsInput}.
     */
    public static final class Builder implements ShapeBuilder<DeleteObjectsInput> {
        private final PresenceTracker tracker = PresenceTracker.of($SCHEMA);
        private String bucket;
        private Delete delete;
        private String mfa;
        private RequestPayer requestPayer;
        private Boolean bypassGovernanceRetention;
        private String expectedBucketOwner;
        private ChecksumAlgorithm checksumAlgorithm;

        private Builder() {}

        @Override
        public Schema schema() {
            return $SCHEMA;
        }

        /**
         * The bucket name containing the objects to delete.
         *
         * <p><b>Directory buckets</b> - When you use this operation with a directory bucket, you must use
         * virtual-hosted-style requests in the format <code><i>Bucket-name</i>.s3express-<i>zone-id</i>.<i>region-code</i>
         * .amazonaws.com</code>. Path-style requests are not supported. Directory bucket names must be unique in the chosen
         * Zone (Availability Zone or Local Zone). Bucket names must follow the format <code><i>bucket-base-name</i>--<i>
         * zone-id</i>--x-s3</code> (for example, <code><i>amzn-s3-demo-bucket</i>--<i>usw2-az1</i>--x-s3</code>). For
         * information about bucket naming restrictions, see <a href="https://docs.aws.amazon.com/AmazonS3/latest/userguide/directory-bucket-naming-rules.html">Directory bucket naming rules</a> in the <i>Amazon S3 User
         * Guide</i>.
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
         * <p>Object Lambda access points are not supported by directory buckets.
         *
         * <p><b>S3 on Outposts</b> - When you use this action with S3 on Outposts, you must direct requests to the S3 on
         * Outposts hostname. The S3 on Outposts hostname takes the form <code><i>AccessPointName</i>-<i>AccountId</i>.<i>
         * outpostID</i>.s3-outposts.<i>Region</i>.amazonaws.com</code>. When you use this action with S3 on Outposts, the
         * destination bucket must be the Outposts access point ARN or the access point alias. For more information about S3
         * on Outposts, see <a href="https://docs.aws.amazon.com/AmazonS3/latest/userguide/S3onOutposts.html">What is S3 on Outposts?</a> in the <i>Amazon S3 User Guide</i>.
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
         * Container for the request.
         *
         * <p><strong>Required</strong>
         * @return this builder.
         */
        public Builder delete(Delete delete) {
            this.delete = Objects.requireNonNull(delete, "delete cannot be null");
            tracker.setMember($SCHEMA_DELETE);
            return this;
        }

        /**
         * The concatenation of the authentication device's serial number, a space, and the value that is displayed on your
         * authentication device. Required to permanently delete a versioned object if versioning is configured with MFA
         * delete enabled.
         *
         * <p>When performing the <code>DeleteObjects</code> operation on an MFA delete enabled bucket, which attempts to
         * delete the specified versioned objects, you must include an MFA token. If you don't provide an MFA token, the
         * entire request will fail, even if there are non-versioned objects that you are trying to delete. If you provide
         * an invalid token, whether there are versioned object keys in the request or not, the entire Multi-Object Delete
         * request will fail. For information about MFA Delete, see <a href="https://docs.aws.amazon.com/AmazonS3/latest/dev/Versioning.html#MultiFactorAuthenticationDelete"> MFA Delete</a> in the <i>Amazon S3 User Guide</i>.
         *
         * <p>This functionality is not supported for directory buckets.
         *
         * @return this builder.
         */
        public Builder mfa(String mfa) {
            this.mfa = mfa;
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
         * Specifies whether you want to delete this object even if it has a Governance-type Object Lock in place. To use
         * this header, you must have the <code>s3:BypassGovernanceRetention</code> permission.
         *
         * <p>This functionality is not supported for directory buckets.
         *
         * @return this builder.
         */
        public Builder bypassGovernanceRetention(Boolean bypassGovernanceRetention) {
            this.bypassGovernanceRetention = bypassGovernanceRetention;
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

        /**
         * Indicates the algorithm used to create the checksum for the object when you use the SDK. This header will not
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
        public DeleteObjectsInput build() {
            tracker.validate();
            return new DeleteObjectsInput(this);
        }

        @Override
        @SuppressWarnings("unchecked")
        public void setMemberValue(Schema member, Object value) {
            switch (member.memberIndex()) {
                case 0 -> bucket((String) SchemaUtils.validateSameMember($SCHEMA_BUCKET, member, value));
                case 1 -> delete((Delete) SchemaUtils.validateSameMember($SCHEMA_DELETE, member, value));
                case 2 -> mfa((String) SchemaUtils.validateSameMember($SCHEMA_MFA, member, value));
                case 3 -> requestPayer((RequestPayer) SchemaUtils.validateSameMember($SCHEMA_REQUEST_PAYER, member, value));
                case 4 -> bypassGovernanceRetention((Boolean) SchemaUtils.validateSameMember($SCHEMA_BYPASS_GOVERNANCE_RETENTION, member, value));
                case 5 -> expectedBucketOwner((String) SchemaUtils.validateSameMember($SCHEMA_EXPECTED_BUCKET_OWNER, member, value));
                case 6 -> checksumAlgorithm((ChecksumAlgorithm) SchemaUtils.validateSameMember($SCHEMA_CHECKSUM_ALGORITHM, member, value));
                default -> ShapeBuilder.super.setMemberValue(member, value);
            }
        }

        @Override
        public ShapeBuilder<DeleteObjectsInput> errorCorrection() {
            if (tracker.allSet()) {
                return this;
            }
            if (!tracker.checkMember($SCHEMA_BUCKET)) {
                bucket("");
            }
            if (!tracker.checkMember($SCHEMA_DELETE)) {
                tracker.setMember($SCHEMA_DELETE);
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
                    case 1 -> builder.delete(Delete.builder().deserializeMember(de, member).build());
                    case 2 -> builder.mfa(de.readString(member));
                    case 3 -> builder.requestPayer(RequestPayer.builder().deserializeMember(de, member).build());
                    case 4 -> builder.bypassGovernanceRetention(de.readBoolean(member));
                    case 5 -> builder.expectedBucketOwner(de.readString(member));
                    case 6 -> builder.checksumAlgorithm(ChecksumAlgorithm.builder().deserializeMember(de, member).build());
                    default -> throw new IllegalArgumentException("Unexpected member: " + member.memberName());
                }
            }
        }
    }
}
