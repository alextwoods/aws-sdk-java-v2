package software.amazon.awssdk.benchmark.smithyjava.s3.model;

import java.time.Instant;
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
public final class DeleteObjectInput implements SerializableStruct {

    public static final Schema $SCHEMA = Schemas1.DELETE_OBJECT_INPUT;
    private static final Schema $SCHEMA_BUCKET = $SCHEMA.member("Bucket");
    private static final Schema $SCHEMA_KEY = $SCHEMA.member("Key");
    private static final Schema $SCHEMA_MFA = $SCHEMA.member("MFA");
    private static final Schema $SCHEMA_VERSION_ID = $SCHEMA.member("VersionId");
    private static final Schema $SCHEMA_REQUEST_PAYER = $SCHEMA.member("RequestPayer");
    private static final Schema $SCHEMA_BYPASS_GOVERNANCE_RETENTION = $SCHEMA.member("BypassGovernanceRetention");
    private static final Schema $SCHEMA_EXPECTED_BUCKET_OWNER = $SCHEMA.member("ExpectedBucketOwner");
    private static final Schema $SCHEMA_IF_MATCH = $SCHEMA.member("IfMatch");
    private static final Schema $SCHEMA_IF_MATCH_LAST_MODIFIED_TIME = $SCHEMA.member("IfMatchLastModifiedTime");
    private static final Schema $SCHEMA_IF_MATCH_SIZE = $SCHEMA.member("IfMatchSize");

    public static final ShapeId $ID = $SCHEMA.id();

    private final transient String bucket;
    private final transient String key;
    private final transient String mfa;
    private final transient String versionId;
    private final transient RequestPayer requestPayer;
    private final transient Boolean bypassGovernanceRetention;
    private final transient String expectedBucketOwner;
    private final transient String ifMatch;
    private final transient Instant ifMatchLastModifiedTime;
    private final transient Long ifMatchSize;

    private DeleteObjectInput(Builder builder) {
        this.bucket = builder.bucket;
        this.key = builder.key;
        this.mfa = builder.mfa;
        this.versionId = builder.versionId;
        this.requestPayer = builder.requestPayer;
        this.bypassGovernanceRetention = builder.bypassGovernanceRetention;
        this.expectedBucketOwner = builder.expectedBucketOwner;
        this.ifMatch = builder.ifMatch;
        this.ifMatchLastModifiedTime = builder.ifMatchLastModifiedTime;
        this.ifMatchSize = builder.ifMatchSize;
    }

    /**
     * The bucket name of the bucket containing the object.
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
     * Key name of the object to delete.
     */
    public String getKey() {
        return key;
    }

    /**
     * The concatenation of the authentication device's serial number, a space, and the value that is displayed on your
     * authentication device. Required to permanently delete a versioned object if versioning is configured with MFA
     * delete enabled.
     *
     * <p>This functionality is not supported for directory buckets.
     */
    public String getMfa() {
        return mfa;
    }

    /**
     * Version ID used to reference a specific version of the object.
     *
     * <p>For directory buckets in this API operation, only the <code>null</code> value of the version ID is supported.
     */
    public String getVersionId() {
        return versionId;
    }

    public RequestPayer getRequestPayer() {
        return requestPayer;
    }

    /**
     * Indicates whether S3 Object Lock should bypass Governance-mode restrictions to process this operation. To use
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
     * Deletes the object if the ETag (entity tag) value provided during the delete operation matches the ETag of the
     * object in S3. If the ETag values do not match, the operation returns a <code>412 Precondition Failed</code>
     * error.
     *
     * <p>Expects the ETag value as a string. <code>If-Match</code> does accept a string value of an '&#42;' (asterisk)
     * character to denote a match of any ETag.
     *
     * <p>For more information about conditional requests, see <a href="https://tools.ietf.org/html/rfc7232">RFC 7232</a>.
     */
    public String getIfMatch() {
        return ifMatch;
    }

    /**
     * If present, the object is deleted only if its modification times matches the provided <code>Timestamp</code>. If
     * the <code>Timestamp</code> values do not match, the operation returns a <code>412 Precondition Failed</code>
     * error. If the <code>Timestamp</code> matches or if the object doesn’t exist, the operation returns a <code>204
     * Success (No Content)</code> response.
     *
     * <p>This functionality is only supported for directory buckets.
     */
    public Instant getIfMatchLastModifiedTime() {
        return ifMatchLastModifiedTime;
    }

    /**
     * If present, the object is deleted only if its size matches the provided size in bytes. If the <code>Size</code>
     * value does not match, the operation returns a <code>412 Precondition Failed</code> error. If the <code>Size</code>
     * matches or if the object doesn’t exist, the operation returns a <code>204 Success (No Content)</code> response.
     *
     * <p>This functionality is only supported for directory buckets.
     *
     * <p>You can use the <code>If-Match</code>, <code>x-amz-if-match-last-modified-time</code> and <code>
     * x-amz-if-match-size</code> conditional headers in conjunction with each-other or individually.
     */
    public Long getIfMatchSize() {
        return ifMatchSize;
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
        DeleteObjectInput that = (DeleteObjectInput) other;
        return Objects.equals(this.bypassGovernanceRetention, that.bypassGovernanceRetention)
               && Objects.equals(this.ifMatchSize, that.ifMatchSize)
               && Objects.equals(this.bucket, that.bucket)
               && Objects.equals(this.key, that.key)
               && Objects.equals(this.mfa, that.mfa)
               && Objects.equals(this.versionId, that.versionId)
               && Objects.equals(this.expectedBucketOwner, that.expectedBucketOwner)
               && Objects.equals(this.ifMatch, that.ifMatch)
               && Objects.equals(this.requestPayer, that.requestPayer)
               && Objects.equals(this.ifMatchLastModifiedTime, that.ifMatchLastModifiedTime);
    }

    @Override
    public int hashCode() {
        int $hc = Objects.hashCode(bucket);
        $hc = 31 * $hc + Objects.hashCode(key);
        $hc = 31 * $hc + Objects.hashCode(mfa);
        $hc = 31 * $hc + Objects.hashCode(versionId);
        $hc = 31 * $hc + Objects.hashCode(requestPayer);
        $hc = 31 * $hc + Objects.hashCode(bypassGovernanceRetention);
        $hc = 31 * $hc + Objects.hashCode(expectedBucketOwner);
        $hc = 31 * $hc + Objects.hashCode(ifMatch);
        $hc = 31 * $hc + Objects.hashCode(ifMatchLastModifiedTime);
        $hc = 31 * $hc + Objects.hashCode(ifMatchSize);
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
        if (mfa != null) {
            serializer.writeString($SCHEMA_MFA, mfa);
        }
        if (versionId != null) {
            serializer.writeString($SCHEMA_VERSION_ID, versionId);
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
        if (ifMatch != null) {
            serializer.writeString($SCHEMA_IF_MATCH, ifMatch);
        }
        if (ifMatchLastModifiedTime != null) {
            serializer.writeTimestamp($SCHEMA_IF_MATCH_LAST_MODIFIED_TIME, ifMatchLastModifiedTime);
        }
        if (ifMatchSize != null) {
            serializer.writeLong($SCHEMA_IF_MATCH_SIZE, ifMatchSize);
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T getMemberValue(Schema member) {
        return switch (member.memberIndex()) {
            case 0 -> (T) SchemaUtils.validateSameMember($SCHEMA_BUCKET, member, bucket);
            case 1 -> (T) SchemaUtils.validateSameMember($SCHEMA_KEY, member, key);
            case 2 -> (T) SchemaUtils.validateSameMember($SCHEMA_MFA, member, mfa);
            case 3 -> (T) SchemaUtils.validateSameMember($SCHEMA_VERSION_ID, member, versionId);
            case 4 -> (T) SchemaUtils.validateSameMember($SCHEMA_REQUEST_PAYER, member, requestPayer);
            case 5 -> (T) SchemaUtils.validateSameMember($SCHEMA_BYPASS_GOVERNANCE_RETENTION, member, bypassGovernanceRetention);
            case 6 -> (T) SchemaUtils.validateSameMember($SCHEMA_EXPECTED_BUCKET_OWNER, member, expectedBucketOwner);
            case 7 -> (T) SchemaUtils.validateSameMember($SCHEMA_IF_MATCH, member, ifMatch);
            case 8 -> (T) SchemaUtils.validateSameMember($SCHEMA_IF_MATCH_LAST_MODIFIED_TIME, member, ifMatchLastModifiedTime);
            case 9 -> (T) SchemaUtils.validateSameMember($SCHEMA_IF_MATCH_SIZE, member, ifMatchSize);
            default -> throw new IllegalArgumentException("Attempted to get non-existent member: " + member.id());
        };
    }

    /**
     * Create a new builder containing all the current property values of this object.
     *
     * <p><strong>Note:</strong> This method performs only a shallow copy of the original properties.
     *
     * @return a builder for {@link DeleteObjectInput}.
     */
    public Builder toBuilder() {
        var builder = new Builder();
        builder.bucket(this.bucket);
        builder.key(this.key);
        builder.mfa(this.mfa);
        builder.versionId(this.versionId);
        builder.requestPayer(this.requestPayer);
        builder.bypassGovernanceRetention(this.bypassGovernanceRetention);
        builder.expectedBucketOwner(this.expectedBucketOwner);
        builder.ifMatch(this.ifMatch);
        builder.ifMatchLastModifiedTime(this.ifMatchLastModifiedTime);
        builder.ifMatchSize(this.ifMatchSize);
        return builder;
    }

    /**
     * @return returns a new Builder.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for {@link DeleteObjectInput}.
     */
    public static final class Builder implements ShapeBuilder<DeleteObjectInput> {
        private final PresenceTracker tracker = PresenceTracker.of($SCHEMA);
        private String bucket;
        private String key;
        private String mfa;
        private String versionId;
        private RequestPayer requestPayer;
        private Boolean bypassGovernanceRetention;
        private String expectedBucketOwner;
        private String ifMatch;
        private Instant ifMatchLastModifiedTime;
        private Long ifMatchSize;

        private Builder() {}

        @Override
        public Schema schema() {
            return $SCHEMA;
        }

        /**
         * The bucket name of the bucket containing the object.
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
         * Key name of the object to delete.
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
         * The concatenation of the authentication device's serial number, a space, and the value that is displayed on your
         * authentication device. Required to permanently delete a versioned object if versioning is configured with MFA
         * delete enabled.
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
         * Version ID used to reference a specific version of the object.
         *
         * <p>For directory buckets in this API operation, only the <code>null</code> value of the version ID is supported.
         *
         * @return this builder.
         */
        public Builder versionId(String versionId) {
            this.versionId = versionId;
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
         * Indicates whether S3 Object Lock should bypass Governance-mode restrictions to process this operation. To use
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
         * Deletes the object if the ETag (entity tag) value provided during the delete operation matches the ETag of the
         * object in S3. If the ETag values do not match, the operation returns a <code>412 Precondition Failed</code>
         * error.
         *
         * <p>Expects the ETag value as a string. <code>If-Match</code> does accept a string value of an '&#42;' (asterisk)
         * character to denote a match of any ETag.
         *
         * <p>For more information about conditional requests, see <a href="https://tools.ietf.org/html/rfc7232">RFC 7232</a>.
         *
         * @return this builder.
         */
        public Builder ifMatch(String ifMatch) {
            this.ifMatch = ifMatch;
            return this;
        }

        /**
         * If present, the object is deleted only if its modification times matches the provided <code>Timestamp</code>. If
         * the <code>Timestamp</code> values do not match, the operation returns a <code>412 Precondition Failed</code>
         * error. If the <code>Timestamp</code> matches or if the object doesn’t exist, the operation returns a <code>204
         * Success (No Content)</code> response.
         *
         * <p>This functionality is only supported for directory buckets.
         *
         * @return this builder.
         */
        public Builder ifMatchLastModifiedTime(Instant ifMatchLastModifiedTime) {
            this.ifMatchLastModifiedTime = ifMatchLastModifiedTime;
            return this;
        }

        /**
         * If present, the object is deleted only if its size matches the provided size in bytes. If the <code>Size</code>
         * value does not match, the operation returns a <code>412 Precondition Failed</code> error. If the <code>Size</code>
         * matches or if the object doesn’t exist, the operation returns a <code>204 Success (No Content)</code> response.
         *
         * <p>This functionality is only supported for directory buckets.
         *
         * <p>You can use the <code>If-Match</code>, <code>x-amz-if-match-last-modified-time</code> and <code>
         * x-amz-if-match-size</code> conditional headers in conjunction with each-other or individually.
         *
         * @return this builder.
         */
        public Builder ifMatchSize(Long ifMatchSize) {
            this.ifMatchSize = ifMatchSize;
            return this;
        }

        @Override
        public DeleteObjectInput build() {
            tracker.validate();
            return new DeleteObjectInput(this);
        }

        @Override
        @SuppressWarnings("unchecked")
        public void setMemberValue(Schema member, Object value) {
            switch (member.memberIndex()) {
                case 0 -> bucket((String) SchemaUtils.validateSameMember($SCHEMA_BUCKET, member, value));
                case 1 -> key((String) SchemaUtils.validateSameMember($SCHEMA_KEY, member, value));
                case 2 -> mfa((String) SchemaUtils.validateSameMember($SCHEMA_MFA, member, value));
                case 3 -> versionId((String) SchemaUtils.validateSameMember($SCHEMA_VERSION_ID, member, value));
                case 4 -> requestPayer((RequestPayer) SchemaUtils.validateSameMember($SCHEMA_REQUEST_PAYER, member, value));
                case 5 -> bypassGovernanceRetention((Boolean) SchemaUtils.validateSameMember($SCHEMA_BYPASS_GOVERNANCE_RETENTION, member, value));
                case 6 -> expectedBucketOwner((String) SchemaUtils.validateSameMember($SCHEMA_EXPECTED_BUCKET_OWNER, member, value));
                case 7 -> ifMatch((String) SchemaUtils.validateSameMember($SCHEMA_IF_MATCH, member, value));
                case 8 -> ifMatchLastModifiedTime((Instant) SchemaUtils.validateSameMember($SCHEMA_IF_MATCH_LAST_MODIFIED_TIME, member, value));
                case 9 -> ifMatchSize((Long) SchemaUtils.validateSameMember($SCHEMA_IF_MATCH_SIZE, member, value));
                default -> ShapeBuilder.super.setMemberValue(member, value);
            }
        }

        @Override
        public ShapeBuilder<DeleteObjectInput> errorCorrection() {
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
                    case 2 -> builder.mfa(de.readString(member));
                    case 3 -> builder.versionId(de.readString(member));
                    case 4 -> builder.requestPayer(RequestPayer.builder().deserializeMember(de, member).build());
                    case 5 -> builder.bypassGovernanceRetention(de.readBoolean(member));
                    case 6 -> builder.expectedBucketOwner(de.readString(member));
                    case 7 -> builder.ifMatch(de.readString(member));
                    case 8 -> builder.ifMatchLastModifiedTime(de.readTimestamp(member));
                    case 9 -> builder.ifMatchSize(de.readLong(member));
                    default -> throw new IllegalArgumentException("Unexpected member: " + member.memberName());
                }
            }
        }
    }
}
