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
public final class ListMultipartUploadsInput implements SerializableStruct {

    public static final Schema $SCHEMA = Schemas4.LIST_MULTIPART_UPLOADS_INPUT;
    private static final Schema $SCHEMA_BUCKET = $SCHEMA.member("Bucket");
    private static final Schema $SCHEMA_DELIMITER = $SCHEMA.member("Delimiter");
    private static final Schema $SCHEMA_ENCODING_TYPE = $SCHEMA.member("EncodingType");
    private static final Schema $SCHEMA_KEY_MARKER = $SCHEMA.member("KeyMarker");
    private static final Schema $SCHEMA_MAX_UPLOADS = $SCHEMA.member("MaxUploads");
    private static final Schema $SCHEMA_PREFIX = $SCHEMA.member("Prefix");
    private static final Schema $SCHEMA_UPLOAD_ID_MARKER = $SCHEMA.member("UploadIdMarker");
    private static final Schema $SCHEMA_EXPECTED_BUCKET_OWNER = $SCHEMA.member("ExpectedBucketOwner");
    private static final Schema $SCHEMA_REQUEST_PAYER = $SCHEMA.member("RequestPayer");

    public static final ShapeId $ID = $SCHEMA.id();

    private final transient String bucket;
    private final transient String delimiter;
    private final transient EncodingType encodingType;
    private final transient String keyMarker;
    private final transient Integer maxUploads;
    private final transient String prefix;
    private final transient String uploadIdMarker;
    private final transient String expectedBucketOwner;
    private final transient RequestPayer requestPayer;

    private ListMultipartUploadsInput(Builder builder) {
        this.bucket = builder.bucket;
        this.delimiter = builder.delimiter;
        this.encodingType = builder.encodingType;
        this.keyMarker = builder.keyMarker;
        this.maxUploads = builder.maxUploads;
        this.prefix = builder.prefix;
        this.uploadIdMarker = builder.uploadIdMarker;
        this.expectedBucketOwner = builder.expectedBucketOwner;
        this.requestPayer = builder.requestPayer;
    }

    /**
     * The name of the bucket to which the multipart upload was initiated.
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
     * Character you use to group keys.
     *
     * <p>All keys that contain the same string between the prefix, if specified, and the first occurrence of the
     * delimiter after the prefix are grouped under a single result element, <code>CommonPrefixes</code>. If you don't
     * specify the prefix parameter, then the substring starts at the beginning of the key. The keys that are grouped
     * under <code>CommonPrefixes</code> result element are not returned elsewhere in the response.
     *
     * <p><code>CommonPrefixes</code> is filtered out from results if it is not lexicographically greater than the
     * key-marker.
     *
     * <p><b>Directory buckets</b> - For directory buckets, <code>/</code> is the only supported delimiter.
     */
    public String getDelimiter() {
        return delimiter;
    }

    public EncodingType getEncodingType() {
        return encodingType;
    }

    /**
     * Specifies the multipart upload after which listing should begin.
     *
     * <ul>
     *   <li>
     *     <b>General purpose buckets</b> - For general purpose buckets, <code>key-marker</code> is an object key.
     *     Together with <code>upload-id-marker</code>, this parameter specifies the multipart upload after which
     *     listing should begin.If <code>upload-id-marker</code> is not specified, only the keys lexicographically
     *     greater than the specified <code>key-marker</code> will be included in the list.If <code>upload-id-marker</code>
     *     is specified, any multipart uploads for a key equal to the <code>key-marker</code> might also be
     *     included, provided those multipart uploads have upload IDs lexicographically greater than the specified <code>
     *     upload-id-marker</code>.
     *   </li>
     *   <li>
     *     <b>Directory buckets</b> - For directory buckets, <code>key-marker</code> is obfuscated and isn't a real
     *     object key. The <code>upload-id-marker</code> parameter isn't supported by directory buckets. To list the
     *     additional multipart uploads, you only need to set the value of <code>key-marker</code> to the <code>
     *     NextKeyMarker</code> value from the previous response. In the <code>ListMultipartUploads</code> response,
     *     the multipart uploads aren't sorted lexicographically based on the object keys.
     *
     *     <pre>{@code
     *       </p>
     *            </li>
     *         </ul>
     *      </note>
     *
     *     }</pre>
     *   </li>
     * </ul>
     */
    public String getKeyMarker() {
        return keyMarker;
    }

    /**
     * Sets the maximum number of multipart uploads, from 1 to 1,000, to return in the response body. 1,000 is the
     * maximum number of uploads that can be returned in a response.
     */
    public Integer getMaxUploads() {
        return maxUploads;
    }

    /**
     * Lists in-progress uploads only for those keys that begin with the specified prefix. You can use prefixes to
     * separate a bucket into different grouping of keys. (You can think of using <code>prefix</code> to make groups in
     * the same way that you'd use a folder in a file system.)
     *
     * <p><b>Directory buckets</b> - For directory buckets, only prefixes that end in a delimiter (<code>/</code>) are
     * supported.
     */
    public String getPrefix() {
        return prefix;
    }

    /**
     * Together with key-marker, specifies the multipart upload after which listing should begin. If key-marker is not
     * specified, the upload-id-marker parameter is ignored. Otherwise, any multipart uploads for a key equal to the
     * key-marker might be included in the list only if they have an upload ID lexicographically greater than the
     * specified <code>upload-id-marker</code>.
     *
     * <p>This functionality is not supported for directory buckets.
     */
    public String getUploadIdMarker() {
        return uploadIdMarker;
    }

    /**
     * The account ID of the expected bucket owner. If the account ID that you provide does not match the actual owner
     * of the bucket, the request fails with the HTTP status code <code>403 Forbidden</code> (access denied).
     */
    public String getExpectedBucketOwner() {
        return expectedBucketOwner;
    }

    public RequestPayer getRequestPayer() {
        return requestPayer;
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
        ListMultipartUploadsInput that = (ListMultipartUploadsInput) other;
        return Objects.equals(this.maxUploads, that.maxUploads)
               && Objects.equals(this.bucket, that.bucket)
               && Objects.equals(this.delimiter, that.delimiter)
               && Objects.equals(this.keyMarker, that.keyMarker)
               && Objects.equals(this.prefix, that.prefix)
               && Objects.equals(this.uploadIdMarker, that.uploadIdMarker)
               && Objects.equals(this.expectedBucketOwner, that.expectedBucketOwner)
               && Objects.equals(this.encodingType, that.encodingType)
               && Objects.equals(this.requestPayer, that.requestPayer);
    }

    @Override
    public int hashCode() {
        int $hc = Objects.hashCode(bucket);
        $hc = 31 * $hc + Objects.hashCode(delimiter);
        $hc = 31 * $hc + Objects.hashCode(encodingType);
        $hc = 31 * $hc + Objects.hashCode(keyMarker);
        $hc = 31 * $hc + Objects.hashCode(maxUploads);
        $hc = 31 * $hc + Objects.hashCode(prefix);
        $hc = 31 * $hc + Objects.hashCode(uploadIdMarker);
        $hc = 31 * $hc + Objects.hashCode(expectedBucketOwner);
        $hc = 31 * $hc + Objects.hashCode(requestPayer);
        return $hc;
    }

    @Override
    public Schema schema() {
        return $SCHEMA;
    }

    @Override
    public void serializeMembers(ShapeSerializer serializer) {
        serializer.writeString($SCHEMA_BUCKET, bucket);
        if (delimiter != null) {
            serializer.writeString($SCHEMA_DELIMITER, delimiter);
        }
        if (encodingType != null) {
            serializer.writeString($SCHEMA_ENCODING_TYPE, encodingType.getValue());
        }
        if (keyMarker != null) {
            serializer.writeString($SCHEMA_KEY_MARKER, keyMarker);
        }
        if (maxUploads != null) {
            serializer.writeInteger($SCHEMA_MAX_UPLOADS, maxUploads);
        }
        if (prefix != null) {
            serializer.writeString($SCHEMA_PREFIX, prefix);
        }
        if (uploadIdMarker != null) {
            serializer.writeString($SCHEMA_UPLOAD_ID_MARKER, uploadIdMarker);
        }
        if (expectedBucketOwner != null) {
            serializer.writeString($SCHEMA_EXPECTED_BUCKET_OWNER, expectedBucketOwner);
        }
        if (requestPayer != null) {
            serializer.writeString($SCHEMA_REQUEST_PAYER, requestPayer.getValue());
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T getMemberValue(Schema member) {
        return switch (member.memberIndex()) {
            case 0 -> (T) SchemaUtils.validateSameMember($SCHEMA_BUCKET, member, bucket);
            case 1 -> (T) SchemaUtils.validateSameMember($SCHEMA_DELIMITER, member, delimiter);
            case 2 -> (T) SchemaUtils.validateSameMember($SCHEMA_ENCODING_TYPE, member, encodingType);
            case 3 -> (T) SchemaUtils.validateSameMember($SCHEMA_KEY_MARKER, member, keyMarker);
            case 4 -> (T) SchemaUtils.validateSameMember($SCHEMA_MAX_UPLOADS, member, maxUploads);
            case 5 -> (T) SchemaUtils.validateSameMember($SCHEMA_PREFIX, member, prefix);
            case 6 -> (T) SchemaUtils.validateSameMember($SCHEMA_UPLOAD_ID_MARKER, member, uploadIdMarker);
            case 7 -> (T) SchemaUtils.validateSameMember($SCHEMA_EXPECTED_BUCKET_OWNER, member, expectedBucketOwner);
            case 8 -> (T) SchemaUtils.validateSameMember($SCHEMA_REQUEST_PAYER, member, requestPayer);
            default -> throw new IllegalArgumentException("Attempted to get non-existent member: " + member.id());
        };
    }

    /**
     * Create a new builder containing all the current property values of this object.
     *
     * <p><strong>Note:</strong> This method performs only a shallow copy of the original properties.
     *
     * @return a builder for {@link ListMultipartUploadsInput}.
     */
    public Builder toBuilder() {
        var builder = new Builder();
        builder.bucket(this.bucket);
        builder.delimiter(this.delimiter);
        builder.encodingType(this.encodingType);
        builder.keyMarker(this.keyMarker);
        builder.maxUploads(this.maxUploads);
        builder.prefix(this.prefix);
        builder.uploadIdMarker(this.uploadIdMarker);
        builder.expectedBucketOwner(this.expectedBucketOwner);
        builder.requestPayer(this.requestPayer);
        return builder;
    }

    /**
     * @return returns a new Builder.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for {@link ListMultipartUploadsInput}.
     */
    public static final class Builder implements ShapeBuilder<ListMultipartUploadsInput> {
        private final PresenceTracker tracker = PresenceTracker.of($SCHEMA);
        private String bucket;
        private String delimiter;
        private EncodingType encodingType;
        private String keyMarker;
        private Integer maxUploads;
        private String prefix;
        private String uploadIdMarker;
        private String expectedBucketOwner;
        private RequestPayer requestPayer;

        private Builder() {}

        @Override
        public Schema schema() {
            return $SCHEMA;
        }

        /**
         * The name of the bucket to which the multipart upload was initiated.
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
         * Character you use to group keys.
         *
         * <p>All keys that contain the same string between the prefix, if specified, and the first occurrence of the
         * delimiter after the prefix are grouped under a single result element, <code>CommonPrefixes</code>. If you don't
         * specify the prefix parameter, then the substring starts at the beginning of the key. The keys that are grouped
         * under <code>CommonPrefixes</code> result element are not returned elsewhere in the response.
         *
         * <p><code>CommonPrefixes</code> is filtered out from results if it is not lexicographically greater than the
         * key-marker.
         *
         * <p><b>Directory buckets</b> - For directory buckets, <code>/</code> is the only supported delimiter.
         *
         * @return this builder.
         */
        public Builder delimiter(String delimiter) {
            this.delimiter = delimiter;
            return this;
        }

        /**
         * @return this builder.
         */
        public Builder encodingType(EncodingType encodingType) {
            this.encodingType = encodingType;
            return this;
        }

        /**
         * Specifies the multipart upload after which listing should begin.
         *
         * <ul>
         *   <li>
         *     <b>General purpose buckets</b> - For general purpose buckets, <code>key-marker</code> is an object key.
         *     Together with <code>upload-id-marker</code>, this parameter specifies the multipart upload after which
         *     listing should begin.If <code>upload-id-marker</code> is not specified, only the keys lexicographically
         *     greater than the specified <code>key-marker</code> will be included in the list.If <code>upload-id-marker</code>
         *     is specified, any multipart uploads for a key equal to the <code>key-marker</code> might also be
         *     included, provided those multipart uploads have upload IDs lexicographically greater than the specified <code>
         *     upload-id-marker</code>.
         *   </li>
         *   <li>
         *     <b>Directory buckets</b> - For directory buckets, <code>key-marker</code> is obfuscated and isn't a real
         *     object key. The <code>upload-id-marker</code> parameter isn't supported by directory buckets. To list the
         *     additional multipart uploads, you only need to set the value of <code>key-marker</code> to the <code>
         *     NextKeyMarker</code> value from the previous response. In the <code>ListMultipartUploads</code> response,
         *     the multipart uploads aren't sorted lexicographically based on the object keys.
         *
         *     <pre>{@code
         *       </p>
         *            </li>
         *         </ul>
         *      </note>
         *
         *     }</pre>
         *   </li>
         * </ul>
         *
         * @return this builder.
         */
        public Builder keyMarker(String keyMarker) {
            this.keyMarker = keyMarker;
            return this;
        }

        /**
         * Sets the maximum number of multipart uploads, from 1 to 1,000, to return in the response body. 1,000 is the
         * maximum number of uploads that can be returned in a response.
         *
         * @return this builder.
         */
        public Builder maxUploads(Integer maxUploads) {
            this.maxUploads = maxUploads;
            return this;
        }

        /**
         * Lists in-progress uploads only for those keys that begin with the specified prefix. You can use prefixes to
         * separate a bucket into different grouping of keys. (You can think of using <code>prefix</code> to make groups in
         * the same way that you'd use a folder in a file system.)
         *
         * <p><b>Directory buckets</b> - For directory buckets, only prefixes that end in a delimiter (<code>/</code>) are
         * supported.
         *
         * @return this builder.
         */
        public Builder prefix(String prefix) {
            this.prefix = prefix;
            return this;
        }

        /**
         * Together with key-marker, specifies the multipart upload after which listing should begin. If key-marker is not
         * specified, the upload-id-marker parameter is ignored. Otherwise, any multipart uploads for a key equal to the
         * key-marker might be included in the list only if they have an upload ID lexicographically greater than the
         * specified <code>upload-id-marker</code>.
         *
         * <p>This functionality is not supported for directory buckets.
         *
         * @return this builder.
         */
        public Builder uploadIdMarker(String uploadIdMarker) {
            this.uploadIdMarker = uploadIdMarker;
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
         * @return this builder.
         */
        public Builder requestPayer(RequestPayer requestPayer) {
            this.requestPayer = requestPayer;
            return this;
        }

        @Override
        public ListMultipartUploadsInput build() {
            tracker.validate();
            return new ListMultipartUploadsInput(this);
        }

        @Override
        @SuppressWarnings("unchecked")
        public void setMemberValue(Schema member, Object value) {
            switch (member.memberIndex()) {
                case 0 -> bucket((String) SchemaUtils.validateSameMember($SCHEMA_BUCKET, member, value));
                case 1 -> delimiter((String) SchemaUtils.validateSameMember($SCHEMA_DELIMITER, member, value));
                case 2 -> encodingType((EncodingType) SchemaUtils.validateSameMember($SCHEMA_ENCODING_TYPE, member, value));
                case 3 -> keyMarker((String) SchemaUtils.validateSameMember($SCHEMA_KEY_MARKER, member, value));
                case 4 -> maxUploads((Integer) SchemaUtils.validateSameMember($SCHEMA_MAX_UPLOADS, member, value));
                case 5 -> prefix((String) SchemaUtils.validateSameMember($SCHEMA_PREFIX, member, value));
                case 6 -> uploadIdMarker((String) SchemaUtils.validateSameMember($SCHEMA_UPLOAD_ID_MARKER, member, value));
                case 7 -> expectedBucketOwner((String) SchemaUtils.validateSameMember($SCHEMA_EXPECTED_BUCKET_OWNER, member, value));
                case 8 -> requestPayer((RequestPayer) SchemaUtils.validateSameMember($SCHEMA_REQUEST_PAYER, member, value));
                default -> ShapeBuilder.super.setMemberValue(member, value);
            }
        }

        @Override
        public ShapeBuilder<ListMultipartUploadsInput> errorCorrection() {
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
                    case 1 -> builder.delimiter(de.readString(member));
                    case 2 -> builder.encodingType(EncodingType.builder().deserializeMember(de, member).build());
                    case 3 -> builder.keyMarker(de.readString(member));
                    case 4 -> builder.maxUploads(de.readInteger(member));
                    case 5 -> builder.prefix(de.readString(member));
                    case 6 -> builder.uploadIdMarker(de.readString(member));
                    case 7 -> builder.expectedBucketOwner(de.readString(member));
                    case 8 -> builder.requestPayer(RequestPayer.builder().deserializeMember(de, member).build());
                    default -> throw new IllegalArgumentException("Unexpected member: " + member.memberName());
                }
            }
        }
    }
}
