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
public final class ListPartsInput implements SerializableStruct {

    public static final Schema $SCHEMA = Schemas5.LIST_PARTS_INPUT;
    private static final Schema $SCHEMA_BUCKET = $SCHEMA.member("Bucket");
    private static final Schema $SCHEMA_KEY = $SCHEMA.member("Key");
    private static final Schema $SCHEMA_MAX_PARTS = $SCHEMA.member("MaxParts");
    private static final Schema $SCHEMA_PART_NUMBER_MARKER = $SCHEMA.member("PartNumberMarker");
    private static final Schema $SCHEMA_UPLOAD_ID = $SCHEMA.member("UploadId");
    private static final Schema $SCHEMA_REQUEST_PAYER = $SCHEMA.member("RequestPayer");
    private static final Schema $SCHEMA_EXPECTED_BUCKET_OWNER = $SCHEMA.member("ExpectedBucketOwner");
    private static final Schema $SCHEMA_SSE_CUSTOMER_ALGORITHM = $SCHEMA.member("SSECustomerAlgorithm");
    private static final Schema $SCHEMA_SSE_CUSTOMER_KEY = $SCHEMA.member("SSECustomerKey");
    private static final Schema $SCHEMA_SSE_CUSTOMER_KEYM_D5 = $SCHEMA.member("SSECustomerKeyMD5");

    public static final ShapeId $ID = $SCHEMA.id();

    private final transient String bucket;
    private final transient String key;
    private final transient Integer maxParts;
    private final transient String partNumberMarker;
    private final transient String uploadId;
    private final transient RequestPayer requestPayer;
    private final transient String expectedBucketOwner;
    private final transient String sseCustomerAlgorithm;
    private final transient String sseCustomerKey;
    private final transient String sseCustomerKeymD5;

    private ListPartsInput(Builder builder) {
        this.bucket = builder.bucket;
        this.key = builder.key;
        this.maxParts = builder.maxParts;
        this.partNumberMarker = builder.partNumberMarker;
        this.uploadId = builder.uploadId;
        this.requestPayer = builder.requestPayer;
        this.expectedBucketOwner = builder.expectedBucketOwner;
        this.sseCustomerAlgorithm = builder.sseCustomerAlgorithm;
        this.sseCustomerKey = builder.sseCustomerKey;
        this.sseCustomerKeymD5 = builder.sseCustomerKeymD5;
    }

    /**
     * The name of the bucket to which the parts are being uploaded.
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
     * Object key for which the multipart upload was initiated.
     */
    public String getKey() {
        return key;
    }

    /**
     * Sets the maximum number of parts to return.
     */
    public Integer getMaxParts() {
        return maxParts;
    }

    /**
     * Specifies the part after which listing should begin. Only parts with higher part numbers will be listed.
     */
    public String getPartNumberMarker() {
        return partNumberMarker;
    }

    /**
     * Upload ID identifying the multipart upload whose parts are being listed.
     */
    public String getUploadId() {
        return uploadId;
    }

    public RequestPayer getRequestPayer() {
        return requestPayer;
    }

    /**
     * The account ID of the expected bucket owner. If the account ID that you provide does not match the actual owner
     * of the bucket, the request fails with the HTTP status code <code>403 Forbidden</code> (access denied).
     */
    public String getExpectedBucketOwner() {
        return expectedBucketOwner;
    }

    /**
     * The server-side encryption (SSE) algorithm used to encrypt the object. This parameter is needed only when the
     * object was created using a checksum algorithm. For more information, see <a href="https://docs.aws.amazon.com/AmazonS3/latest/dev/ServerSideEncryptionCustomerKeys.html">Protecting data using SSE-C keys</a>
     * in the <i>Amazon S3 User Guide</i>.
     *
     * <p>This functionality is not supported for directory buckets.
     */
    public String getSseCustomerAlgorithm() {
        return sseCustomerAlgorithm;
    }

    /**
     * The server-side encryption (SSE) customer managed key. This parameter is needed only when the object was created
     * using a checksum algorithm. For more information, see <a href="https://docs.aws.amazon.com/AmazonS3/latest/dev/ServerSideEncryptionCustomerKeys.html">Protecting data using SSE-C keys</a> in the <i>Amazon S3
     * User Guide</i>.
     *
     * <p>This functionality is not supported for directory buckets.
     */
    public String getSseCustomerKey() {
        return sseCustomerKey;
    }

    /**
     * The MD5 server-side encryption (SSE) customer managed key. This parameter is needed only when the object was
     * created using a checksum algorithm. For more information, see <a href="https://docs.aws.amazon.com/AmazonS3/latest/dev/ServerSideEncryptionCustomerKeys.html">Protecting data using SSE-C keys</a> in the <i>
     * Amazon S3 User Guide</i>.
     *
     * <p>This functionality is not supported for directory buckets.
     */
    public String getSseCustomerKeymD5() {
        return sseCustomerKeymD5;
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
        ListPartsInput that = (ListPartsInput) other;
        return Objects.equals(this.maxParts, that.maxParts)
               && Objects.equals(this.bucket, that.bucket)
               && Objects.equals(this.key, that.key)
               && Objects.equals(this.partNumberMarker, that.partNumberMarker)
               && Objects.equals(this.uploadId, that.uploadId)
               && Objects.equals(this.expectedBucketOwner, that.expectedBucketOwner)
               && Objects.equals(this.sseCustomerAlgorithm, that.sseCustomerAlgorithm)
               && Objects.equals(this.sseCustomerKey, that.sseCustomerKey)
               && Objects.equals(this.sseCustomerKeymD5, that.sseCustomerKeymD5)
               && Objects.equals(this.requestPayer, that.requestPayer);
    }

    @Override
    public int hashCode() {
        int $hc = Objects.hashCode(bucket);
        $hc = 31 * $hc + Objects.hashCode(key);
        $hc = 31 * $hc + Objects.hashCode(maxParts);
        $hc = 31 * $hc + Objects.hashCode(partNumberMarker);
        $hc = 31 * $hc + Objects.hashCode(uploadId);
        $hc = 31 * $hc + Objects.hashCode(requestPayer);
        $hc = 31 * $hc + Objects.hashCode(expectedBucketOwner);
        $hc = 31 * $hc + Objects.hashCode(sseCustomerAlgorithm);
        $hc = 31 * $hc + Objects.hashCode(sseCustomerKey);
        $hc = 31 * $hc + Objects.hashCode(sseCustomerKeymD5);
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
        if (maxParts != null) {
            serializer.writeInteger($SCHEMA_MAX_PARTS, maxParts);
        }
        if (partNumberMarker != null) {
            serializer.writeString($SCHEMA_PART_NUMBER_MARKER, partNumberMarker);
        }
        serializer.writeString($SCHEMA_UPLOAD_ID, uploadId);
        if (requestPayer != null) {
            serializer.writeString($SCHEMA_REQUEST_PAYER, requestPayer.getValue());
        }
        if (expectedBucketOwner != null) {
            serializer.writeString($SCHEMA_EXPECTED_BUCKET_OWNER, expectedBucketOwner);
        }
        if (sseCustomerAlgorithm != null) {
            serializer.writeString($SCHEMA_SSE_CUSTOMER_ALGORITHM, sseCustomerAlgorithm);
        }
        if (sseCustomerKey != null) {
            serializer.writeString($SCHEMA_SSE_CUSTOMER_KEY, sseCustomerKey);
        }
        if (sseCustomerKeymD5 != null) {
            serializer.writeString($SCHEMA_SSE_CUSTOMER_KEYM_D5, sseCustomerKeymD5);
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T getMemberValue(Schema member) {
        return switch (member.memberIndex()) {
            case 0 -> (T) SchemaUtils.validateSameMember($SCHEMA_BUCKET, member, bucket);
            case 1 -> (T) SchemaUtils.validateSameMember($SCHEMA_KEY, member, key);
            case 2 -> (T) SchemaUtils.validateSameMember($SCHEMA_UPLOAD_ID, member, uploadId);
            case 3 -> (T) SchemaUtils.validateSameMember($SCHEMA_MAX_PARTS, member, maxParts);
            case 4 -> (T) SchemaUtils.validateSameMember($SCHEMA_PART_NUMBER_MARKER, member, partNumberMarker);
            case 5 -> (T) SchemaUtils.validateSameMember($SCHEMA_REQUEST_PAYER, member, requestPayer);
            case 6 -> (T) SchemaUtils.validateSameMember($SCHEMA_EXPECTED_BUCKET_OWNER, member, expectedBucketOwner);
            case 7 -> (T) SchemaUtils.validateSameMember($SCHEMA_SSE_CUSTOMER_ALGORITHM, member, sseCustomerAlgorithm);
            case 8 -> (T) SchemaUtils.validateSameMember($SCHEMA_SSE_CUSTOMER_KEY, member, sseCustomerKey);
            case 9 -> (T) SchemaUtils.validateSameMember($SCHEMA_SSE_CUSTOMER_KEYM_D5, member, sseCustomerKeymD5);
            default -> throw new IllegalArgumentException("Attempted to get non-existent member: " + member.id());
        };
    }

    /**
     * Create a new builder containing all the current property values of this object.
     *
     * <p><strong>Note:</strong> This method performs only a shallow copy of the original properties.
     *
     * @return a builder for {@link ListPartsInput}.
     */
    public Builder toBuilder() {
        var builder = new Builder();
        builder.bucket(this.bucket);
        builder.key(this.key);
        builder.maxParts(this.maxParts);
        builder.partNumberMarker(this.partNumberMarker);
        builder.uploadId(this.uploadId);
        builder.requestPayer(this.requestPayer);
        builder.expectedBucketOwner(this.expectedBucketOwner);
        builder.sseCustomerAlgorithm(this.sseCustomerAlgorithm);
        builder.sseCustomerKey(this.sseCustomerKey);
        builder.sseCustomerKeymD5(this.sseCustomerKeymD5);
        return builder;
    }

    /**
     * @return returns a new Builder.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for {@link ListPartsInput}.
     */
    public static final class Builder implements ShapeBuilder<ListPartsInput> {
        private final PresenceTracker tracker = PresenceTracker.of($SCHEMA);
        private String bucket;
        private String key;
        private Integer maxParts;
        private String partNumberMarker;
        private String uploadId;
        private RequestPayer requestPayer;
        private String expectedBucketOwner;
        private String sseCustomerAlgorithm;
        private String sseCustomerKey;
        private String sseCustomerKeymD5;

        private Builder() {}

        @Override
        public Schema schema() {
            return $SCHEMA;
        }

        /**
         * The name of the bucket to which the parts are being uploaded.
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
         * Object key for which the multipart upload was initiated.
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
         * Sets the maximum number of parts to return.
         *
         * @return this builder.
         */
        public Builder maxParts(Integer maxParts) {
            this.maxParts = maxParts;
            return this;
        }

        /**
         * Specifies the part after which listing should begin. Only parts with higher part numbers will be listed.
         *
         * @return this builder.
         */
        public Builder partNumberMarker(String partNumberMarker) {
            this.partNumberMarker = partNumberMarker;
            return this;
        }

        /**
         * Upload ID identifying the multipart upload whose parts are being listed.
         *
         * <p><strong>Required</strong>
         * @return this builder.
         */
        public Builder uploadId(String uploadId) {
            this.uploadId = Objects.requireNonNull(uploadId, "uploadId cannot be null");
            tracker.setMember($SCHEMA_UPLOAD_ID);
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
         * The server-side encryption (SSE) algorithm used to encrypt the object. This parameter is needed only when the
         * object was created using a checksum algorithm. For more information, see <a href="https://docs.aws.amazon.com/AmazonS3/latest/dev/ServerSideEncryptionCustomerKeys.html">Protecting data using SSE-C keys</a>
         * in the <i>Amazon S3 User Guide</i>.
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
         * The server-side encryption (SSE) customer managed key. This parameter is needed only when the object was created
         * using a checksum algorithm. For more information, see <a href="https://docs.aws.amazon.com/AmazonS3/latest/dev/ServerSideEncryptionCustomerKeys.html">Protecting data using SSE-C keys</a> in the <i>Amazon S3
         * User Guide</i>.
         *
         * <p>This functionality is not supported for directory buckets.
         *
         * @return this builder.
         */
        public Builder sseCustomerKey(String sseCustomerKey) {
            this.sseCustomerKey = sseCustomerKey;
            return this;
        }

        /**
         * The MD5 server-side encryption (SSE) customer managed key. This parameter is needed only when the object was
         * created using a checksum algorithm. For more information, see <a href="https://docs.aws.amazon.com/AmazonS3/latest/dev/ServerSideEncryptionCustomerKeys.html">Protecting data using SSE-C keys</a> in the <i>
         * Amazon S3 User Guide</i>.
         *
         * <p>This functionality is not supported for directory buckets.
         *
         * @return this builder.
         */
        public Builder sseCustomerKeymD5(String sseCustomerKeymD5) {
            this.sseCustomerKeymD5 = sseCustomerKeymD5;
            return this;
        }

        @Override
        public ListPartsInput build() {
            tracker.validate();
            return new ListPartsInput(this);
        }

        @Override
        @SuppressWarnings("unchecked")
        public void setMemberValue(Schema member, Object value) {
            switch (member.memberIndex()) {
                case 0 -> bucket((String) SchemaUtils.validateSameMember($SCHEMA_BUCKET, member, value));
                case 1 -> key((String) SchemaUtils.validateSameMember($SCHEMA_KEY, member, value));
                case 2 -> uploadId((String) SchemaUtils.validateSameMember($SCHEMA_UPLOAD_ID, member, value));
                case 3 -> maxParts((Integer) SchemaUtils.validateSameMember($SCHEMA_MAX_PARTS, member, value));
                case 4 -> partNumberMarker((String) SchemaUtils.validateSameMember($SCHEMA_PART_NUMBER_MARKER, member, value));
                case 5 -> requestPayer((RequestPayer) SchemaUtils.validateSameMember($SCHEMA_REQUEST_PAYER, member, value));
                case 6 -> expectedBucketOwner((String) SchemaUtils.validateSameMember($SCHEMA_EXPECTED_BUCKET_OWNER, member, value));
                case 7 -> sseCustomerAlgorithm((String) SchemaUtils.validateSameMember($SCHEMA_SSE_CUSTOMER_ALGORITHM, member, value));
                case 8 -> sseCustomerKey((String) SchemaUtils.validateSameMember($SCHEMA_SSE_CUSTOMER_KEY, member, value));
                case 9 -> sseCustomerKeymD5((String) SchemaUtils.validateSameMember($SCHEMA_SSE_CUSTOMER_KEYM_D5, member, value));
                default -> ShapeBuilder.super.setMemberValue(member, value);
            }
        }

        @Override
        public ShapeBuilder<ListPartsInput> errorCorrection() {
            if (tracker.allSet()) {
                return this;
            }
            if (!tracker.checkMember($SCHEMA_BUCKET)) {
                bucket("");
            }
            if (!tracker.checkMember($SCHEMA_KEY)) {
                key("");
            }
            if (!tracker.checkMember($SCHEMA_UPLOAD_ID)) {
                uploadId("");
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
                    case 2 -> builder.uploadId(de.readString(member));
                    case 3 -> builder.maxParts(de.readInteger(member));
                    case 4 -> builder.partNumberMarker(de.readString(member));
                    case 5 -> builder.requestPayer(RequestPayer.builder().deserializeMember(de, member).build());
                    case 6 -> builder.expectedBucketOwner(de.readString(member));
                    case 7 -> builder.sseCustomerAlgorithm(de.readString(member));
                    case 8 -> builder.sseCustomerKey(de.readString(member));
                    case 9 -> builder.sseCustomerKeymD5(de.readString(member));
                    default -> throw new IllegalArgumentException("Unexpected member: " + member.memberName());
                }
            }
        }
    }
}
