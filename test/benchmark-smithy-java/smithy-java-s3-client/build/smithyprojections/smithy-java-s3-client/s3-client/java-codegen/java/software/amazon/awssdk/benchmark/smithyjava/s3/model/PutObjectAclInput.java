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
public final class PutObjectAclInput implements SerializableStruct {

    public static final Schema $SCHEMA = Schemas6.PUT_OBJECT_ACL_INPUT;
    private static final Schema $SCHEMA_ACL = $SCHEMA.member("ACL");
    private static final Schema $SCHEMA_ACCESS_CONTROL_POLICY = $SCHEMA.member("AccessControlPolicy");
    private static final Schema $SCHEMA_BUCKET = $SCHEMA.member("Bucket");
    private static final Schema $SCHEMA_CONTENTM_D5 = $SCHEMA.member("ContentMD5");
    private static final Schema $SCHEMA_CHECKSUM_ALGORITHM = $SCHEMA.member("ChecksumAlgorithm");
    private static final Schema $SCHEMA_GRANT_FULL_CONTROL = $SCHEMA.member("GrantFullControl");
    private static final Schema $SCHEMA_GRANT_READ = $SCHEMA.member("GrantRead");
    private static final Schema $SCHEMA_GRANT_READAC_P = $SCHEMA.member("GrantReadACP");
    private static final Schema $SCHEMA_GRANT_WRITE = $SCHEMA.member("GrantWrite");
    private static final Schema $SCHEMA_GRANT_WRITEAC_P = $SCHEMA.member("GrantWriteACP");
    private static final Schema $SCHEMA_KEY = $SCHEMA.member("Key");
    private static final Schema $SCHEMA_REQUEST_PAYER = $SCHEMA.member("RequestPayer");
    private static final Schema $SCHEMA_VERSION_ID = $SCHEMA.member("VersionId");
    private static final Schema $SCHEMA_EXPECTED_BUCKET_OWNER = $SCHEMA.member("ExpectedBucketOwner");

    public static final ShapeId $ID = $SCHEMA.id();

    private final transient ObjectCannedACL acl;
    private final transient AccessControlPolicy accessControlPolicy;
    private final transient String bucket;
    private final transient String contentmD5;
    private final transient ChecksumAlgorithm checksumAlgorithm;
    private final transient String grantFullControl;
    private final transient String grantRead;
    private final transient String grantReadacP;
    private final transient String grantWrite;
    private final transient String grantWriteacP;
    private final transient String key;
    private final transient RequestPayer requestPayer;
    private final transient String versionId;
    private final transient String expectedBucketOwner;

    private PutObjectAclInput(Builder builder) {
        this.acl = builder.acl;
        this.accessControlPolicy = builder.accessControlPolicy;
        this.bucket = builder.bucket;
        this.contentmD5 = builder.contentmD5;
        this.checksumAlgorithm = builder.checksumAlgorithm;
        this.grantFullControl = builder.grantFullControl;
        this.grantRead = builder.grantRead;
        this.grantReadacP = builder.grantReadacP;
        this.grantWrite = builder.grantWrite;
        this.grantWriteacP = builder.grantWriteacP;
        this.key = builder.key;
        this.requestPayer = builder.requestPayer;
        this.versionId = builder.versionId;
        this.expectedBucketOwner = builder.expectedBucketOwner;
    }

    /**
     * The canned ACL to apply to the object. For more information, see <a
     * href="https://docs.aws.amazon.com/AmazonS3/latest/dev/acl-overview.html#CannedACL">Canned ACL</a>.
     */
    public ObjectCannedACL getAcl() {
        return acl;
    }

    /**
     * Contains the elements that set the ACL permissions for an object per grantee.
     */
    public AccessControlPolicy getAccessControlPolicy() {
        return accessControlPolicy;
    }

    /**
     * The bucket name that contains the object to which you want to attach the ACL.
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
     * The Base64 encoded 128-bit <code>MD5</code> digest of the data. This header must be used as a message integrity
     * check to verify that the request body was not corrupted in transit. For more information, go to <a href="http://www.ietf.org/rfc/rfc1864.txt">RFC 1864.&gt;</a>
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
     * Allows grantee the read, write, read ACP, and write ACP permissions on the bucket.
     *
     * <p>This functionality is not supported for Amazon S3 on Outposts.
     */
    public String getGrantFullControl() {
        return grantFullControl;
    }

    /**
     * Allows grantee to list the objects in the bucket.
     *
     * <p>This functionality is not supported for Amazon S3 on Outposts.
     */
    public String getGrantRead() {
        return grantRead;
    }

    /**
     * Allows grantee to read the bucket ACL.
     *
     * <p>This functionality is not supported for Amazon S3 on Outposts.
     */
    public String getGrantReadacP() {
        return grantReadacP;
    }

    /**
     * Allows grantee to create new objects in the bucket.
     *
     * <p>For the bucket and object owners of existing objects, also allows deletions and overwrites of those objects.
     */
    public String getGrantWrite() {
        return grantWrite;
    }

    /**
     * Allows grantee to write the ACL for the applicable bucket.
     *
     * <p>This functionality is not supported for Amazon S3 on Outposts.
     */
    public String getGrantWriteacP() {
        return grantWriteacP;
    }

    /**
     * Key for which the PUT action was initiated.
     */
    public String getKey() {
        return key;
    }

    public RequestPayer getRequestPayer() {
        return requestPayer;
    }

    /**
     * Version ID used to reference a specific version of the object.
     *
     * <p>This functionality is not supported for directory buckets.
     */
    public String getVersionId() {
        return versionId;
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
        PutObjectAclInput that = (PutObjectAclInput) other;
        return Objects.equals(this.bucket, that.bucket)
               && Objects.equals(this.contentmD5, that.contentmD5)
               && Objects.equals(this.grantFullControl, that.grantFullControl)
               && Objects.equals(this.grantRead, that.grantRead)
               && Objects.equals(this.grantReadacP, that.grantReadacP)
               && Objects.equals(this.grantWrite, that.grantWrite)
               && Objects.equals(this.grantWriteacP, that.grantWriteacP)
               && Objects.equals(this.key, that.key)
               && Objects.equals(this.versionId, that.versionId)
               && Objects.equals(this.expectedBucketOwner, that.expectedBucketOwner)
               && Objects.equals(this.acl, that.acl)
               && Objects.equals(this.checksumAlgorithm, that.checksumAlgorithm)
               && Objects.equals(this.requestPayer, that.requestPayer)
               && Objects.equals(this.accessControlPolicy, that.accessControlPolicy);
    }

    @Override
    public int hashCode() {
        int $hc = Objects.hashCode(acl);
        $hc = 31 * $hc + Objects.hashCode(accessControlPolicy);
        $hc = 31 * $hc + Objects.hashCode(bucket);
        $hc = 31 * $hc + Objects.hashCode(contentmD5);
        $hc = 31 * $hc + Objects.hashCode(checksumAlgorithm);
        $hc = 31 * $hc + Objects.hashCode(grantFullControl);
        $hc = 31 * $hc + Objects.hashCode(grantRead);
        $hc = 31 * $hc + Objects.hashCode(grantReadacP);
        $hc = 31 * $hc + Objects.hashCode(grantWrite);
        $hc = 31 * $hc + Objects.hashCode(grantWriteacP);
        $hc = 31 * $hc + Objects.hashCode(key);
        $hc = 31 * $hc + Objects.hashCode(requestPayer);
        $hc = 31 * $hc + Objects.hashCode(versionId);
        $hc = 31 * $hc + Objects.hashCode(expectedBucketOwner);
        return $hc;
    }

    @Override
    public Schema schema() {
        return $SCHEMA;
    }

    @Override
    public void serializeMembers(ShapeSerializer serializer) {
        if (acl != null) {
            serializer.writeString($SCHEMA_ACL, acl.getValue());
        }
        if (accessControlPolicy != null) {
            serializer.writeStruct($SCHEMA_ACCESS_CONTROL_POLICY, accessControlPolicy);
        }
        serializer.writeString($SCHEMA_BUCKET, bucket);
        if (contentmD5 != null) {
            serializer.writeString($SCHEMA_CONTENTM_D5, contentmD5);
        }
        if (checksumAlgorithm != null) {
            serializer.writeString($SCHEMA_CHECKSUM_ALGORITHM, checksumAlgorithm.getValue());
        }
        if (grantFullControl != null) {
            serializer.writeString($SCHEMA_GRANT_FULL_CONTROL, grantFullControl);
        }
        if (grantRead != null) {
            serializer.writeString($SCHEMA_GRANT_READ, grantRead);
        }
        if (grantReadacP != null) {
            serializer.writeString($SCHEMA_GRANT_READAC_P, grantReadacP);
        }
        if (grantWrite != null) {
            serializer.writeString($SCHEMA_GRANT_WRITE, grantWrite);
        }
        if (grantWriteacP != null) {
            serializer.writeString($SCHEMA_GRANT_WRITEAC_P, grantWriteacP);
        }
        serializer.writeString($SCHEMA_KEY, key);
        if (requestPayer != null) {
            serializer.writeString($SCHEMA_REQUEST_PAYER, requestPayer.getValue());
        }
        if (versionId != null) {
            serializer.writeString($SCHEMA_VERSION_ID, versionId);
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
            case 2 -> (T) SchemaUtils.validateSameMember($SCHEMA_ACL, member, acl);
            case 3 -> (T) SchemaUtils.validateSameMember($SCHEMA_ACCESS_CONTROL_POLICY, member, accessControlPolicy);
            case 4 -> (T) SchemaUtils.validateSameMember($SCHEMA_CONTENTM_D5, member, contentmD5);
            case 5 -> (T) SchemaUtils.validateSameMember($SCHEMA_CHECKSUM_ALGORITHM, member, checksumAlgorithm);
            case 6 -> (T) SchemaUtils.validateSameMember($SCHEMA_GRANT_FULL_CONTROL, member, grantFullControl);
            case 7 -> (T) SchemaUtils.validateSameMember($SCHEMA_GRANT_READ, member, grantRead);
            case 8 -> (T) SchemaUtils.validateSameMember($SCHEMA_GRANT_READAC_P, member, grantReadacP);
            case 9 -> (T) SchemaUtils.validateSameMember($SCHEMA_GRANT_WRITE, member, grantWrite);
            case 10 -> (T) SchemaUtils.validateSameMember($SCHEMA_GRANT_WRITEAC_P, member, grantWriteacP);
            case 11 -> (T) SchemaUtils.validateSameMember($SCHEMA_REQUEST_PAYER, member, requestPayer);
            case 12 -> (T) SchemaUtils.validateSameMember($SCHEMA_VERSION_ID, member, versionId);
            case 13 -> (T) SchemaUtils.validateSameMember($SCHEMA_EXPECTED_BUCKET_OWNER, member, expectedBucketOwner);
            default -> throw new IllegalArgumentException("Attempted to get non-existent member: " + member.id());
        };
    }

    /**
     * Create a new builder containing all the current property values of this object.
     *
     * <p><strong>Note:</strong> This method performs only a shallow copy of the original properties.
     *
     * @return a builder for {@link PutObjectAclInput}.
     */
    public Builder toBuilder() {
        var builder = new Builder();
        builder.acl(this.acl);
        builder.accessControlPolicy(this.accessControlPolicy);
        builder.bucket(this.bucket);
        builder.contentmD5(this.contentmD5);
        builder.checksumAlgorithm(this.checksumAlgorithm);
        builder.grantFullControl(this.grantFullControl);
        builder.grantRead(this.grantRead);
        builder.grantReadacP(this.grantReadacP);
        builder.grantWrite(this.grantWrite);
        builder.grantWriteacP(this.grantWriteacP);
        builder.key(this.key);
        builder.requestPayer(this.requestPayer);
        builder.versionId(this.versionId);
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
     * Builder for {@link PutObjectAclInput}.
     */
    public static final class Builder implements ShapeBuilder<PutObjectAclInput> {
        private final PresenceTracker tracker = PresenceTracker.of($SCHEMA);
        private ObjectCannedACL acl;
        private AccessControlPolicy accessControlPolicy;
        private String bucket;
        private String contentmD5;
        private ChecksumAlgorithm checksumAlgorithm;
        private String grantFullControl;
        private String grantRead;
        private String grantReadacP;
        private String grantWrite;
        private String grantWriteacP;
        private String key;
        private RequestPayer requestPayer;
        private String versionId;
        private String expectedBucketOwner;

        private Builder() {}

        @Override
        public Schema schema() {
            return $SCHEMA;
        }

        /**
         * The canned ACL to apply to the object. For more information, see <a
         * href="https://docs.aws.amazon.com/AmazonS3/latest/dev/acl-overview.html#CannedACL">Canned ACL</a>.
         *
         * @return this builder.
         */
        public Builder acl(ObjectCannedACL acl) {
            this.acl = acl;
            return this;
        }

        /**
         * Contains the elements that set the ACL permissions for an object per grantee.
         *
         * @return this builder.
         */
        public Builder accessControlPolicy(AccessControlPolicy accessControlPolicy) {
            this.accessControlPolicy = accessControlPolicy;
            return this;
        }

        /**
         * The bucket name that contains the object to which you want to attach the ACL.
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
         * The Base64 encoded 128-bit <code>MD5</code> digest of the data. This header must be used as a message integrity
         * check to verify that the request body was not corrupted in transit. For more information, go to <a href="http://www.ietf.org/rfc/rfc1864.txt">RFC 1864.&gt;</a>
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
         * Allows grantee the read, write, read ACP, and write ACP permissions on the bucket.
         *
         * <p>This functionality is not supported for Amazon S3 on Outposts.
         *
         * @return this builder.
         */
        public Builder grantFullControl(String grantFullControl) {
            this.grantFullControl = grantFullControl;
            return this;
        }

        /**
         * Allows grantee to list the objects in the bucket.
         *
         * <p>This functionality is not supported for Amazon S3 on Outposts.
         *
         * @return this builder.
         */
        public Builder grantRead(String grantRead) {
            this.grantRead = grantRead;
            return this;
        }

        /**
         * Allows grantee to read the bucket ACL.
         *
         * <p>This functionality is not supported for Amazon S3 on Outposts.
         *
         * @return this builder.
         */
        public Builder grantReadacP(String grantReadacP) {
            this.grantReadacP = grantReadacP;
            return this;
        }

        /**
         * Allows grantee to create new objects in the bucket.
         *
         * <p>For the bucket and object owners of existing objects, also allows deletions and overwrites of those objects.
         *
         * @return this builder.
         */
        public Builder grantWrite(String grantWrite) {
            this.grantWrite = grantWrite;
            return this;
        }

        /**
         * Allows grantee to write the ACL for the applicable bucket.
         *
         * <p>This functionality is not supported for Amazon S3 on Outposts.
         *
         * @return this builder.
         */
        public Builder grantWriteacP(String grantWriteacP) {
            this.grantWriteacP = grantWriteacP;
            return this;
        }

        /**
         * Key for which the PUT action was initiated.
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
         * @return this builder.
         */
        public Builder requestPayer(RequestPayer requestPayer) {
            this.requestPayer = requestPayer;
            return this;
        }

        /**
         * Version ID used to reference a specific version of the object.
         *
         * <p>This functionality is not supported for directory buckets.
         *
         * @return this builder.
         */
        public Builder versionId(String versionId) {
            this.versionId = versionId;
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
        public PutObjectAclInput build() {
            tracker.validate();
            return new PutObjectAclInput(this);
        }

        @Override
        @SuppressWarnings("unchecked")
        public void setMemberValue(Schema member, Object value) {
            switch (member.memberIndex()) {
                case 0 -> bucket((String) SchemaUtils.validateSameMember($SCHEMA_BUCKET, member, value));
                case 1 -> key((String) SchemaUtils.validateSameMember($SCHEMA_KEY, member, value));
                case 2 -> acl((ObjectCannedACL) SchemaUtils.validateSameMember($SCHEMA_ACL, member, value));
                case 3 -> accessControlPolicy((AccessControlPolicy) SchemaUtils.validateSameMember($SCHEMA_ACCESS_CONTROL_POLICY, member, value));
                case 4 -> contentmD5((String) SchemaUtils.validateSameMember($SCHEMA_CONTENTM_D5, member, value));
                case 5 -> checksumAlgorithm((ChecksumAlgorithm) SchemaUtils.validateSameMember($SCHEMA_CHECKSUM_ALGORITHM, member, value));
                case 6 -> grantFullControl((String) SchemaUtils.validateSameMember($SCHEMA_GRANT_FULL_CONTROL, member, value));
                case 7 -> grantRead((String) SchemaUtils.validateSameMember($SCHEMA_GRANT_READ, member, value));
                case 8 -> grantReadacP((String) SchemaUtils.validateSameMember($SCHEMA_GRANT_READAC_P, member, value));
                case 9 -> grantWrite((String) SchemaUtils.validateSameMember($SCHEMA_GRANT_WRITE, member, value));
                case 10 -> grantWriteacP((String) SchemaUtils.validateSameMember($SCHEMA_GRANT_WRITEAC_P, member, value));
                case 11 -> requestPayer((RequestPayer) SchemaUtils.validateSameMember($SCHEMA_REQUEST_PAYER, member, value));
                case 12 -> versionId((String) SchemaUtils.validateSameMember($SCHEMA_VERSION_ID, member, value));
                case 13 -> expectedBucketOwner((String) SchemaUtils.validateSameMember($SCHEMA_EXPECTED_BUCKET_OWNER, member, value));
                default -> ShapeBuilder.super.setMemberValue(member, value);
            }
        }

        @Override
        public ShapeBuilder<PutObjectAclInput> errorCorrection() {
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
                    case 2 -> builder.acl(ObjectCannedACL.builder().deserializeMember(de, member).build());
                    case 3 -> builder.accessControlPolicy(AccessControlPolicy.builder().deserializeMember(de, member).build());
                    case 4 -> builder.contentmD5(de.readString(member));
                    case 5 -> builder.checksumAlgorithm(ChecksumAlgorithm.builder().deserializeMember(de, member).build());
                    case 6 -> builder.grantFullControl(de.readString(member));
                    case 7 -> builder.grantRead(de.readString(member));
                    case 8 -> builder.grantReadacP(de.readString(member));
                    case 9 -> builder.grantWrite(de.readString(member));
                    case 10 -> builder.grantWriteacP(de.readString(member));
                    case 11 -> builder.requestPayer(RequestPayer.builder().deserializeMember(de, member).build());
                    case 12 -> builder.versionId(de.readString(member));
                    case 13 -> builder.expectedBucketOwner(de.readString(member));
                    default -> throw new IllegalArgumentException("Unexpected member: " + member.memberName());
                }
            }
        }
    }
}
