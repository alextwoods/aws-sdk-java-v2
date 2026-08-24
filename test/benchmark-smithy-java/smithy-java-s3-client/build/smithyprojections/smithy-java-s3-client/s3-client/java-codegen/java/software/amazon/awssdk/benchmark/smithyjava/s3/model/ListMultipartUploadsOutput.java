package software.amazon.awssdk.benchmark.smithyjava.s3.model;

import java.util.Collections;
import java.util.List;
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
public final class ListMultipartUploadsOutput implements SerializableStruct {

    public static final Schema $SCHEMA = Schemas4.LIST_MULTIPART_UPLOADS_OUTPUT;
    private static final Schema $SCHEMA_BUCKET = $SCHEMA.member("Bucket");
    private static final Schema $SCHEMA_KEY_MARKER = $SCHEMA.member("KeyMarker");
    private static final Schema $SCHEMA_UPLOAD_ID_MARKER = $SCHEMA.member("UploadIdMarker");
    private static final Schema $SCHEMA_NEXT_KEY_MARKER = $SCHEMA.member("NextKeyMarker");
    private static final Schema $SCHEMA_PREFIX = $SCHEMA.member("Prefix");
    private static final Schema $SCHEMA_DELIMITER = $SCHEMA.member("Delimiter");
    private static final Schema $SCHEMA_NEXT_UPLOAD_ID_MARKER = $SCHEMA.member("NextUploadIdMarker");
    private static final Schema $SCHEMA_MAX_UPLOADS = $SCHEMA.member("MaxUploads");
    private static final Schema $SCHEMA_IS_TRUNCATED = $SCHEMA.member("IsTruncated");
    private static final Schema $SCHEMA_UPLOADS = $SCHEMA.member("Uploads");
    private static final Schema $SCHEMA_COMMON_PREFIXES = $SCHEMA.member("CommonPrefixes");
    private static final Schema $SCHEMA_ENCODING_TYPE = $SCHEMA.member("EncodingType");
    private static final Schema $SCHEMA_REQUEST_CHARGED = $SCHEMA.member("RequestCharged");

    public static final ShapeId $ID = $SCHEMA.id();

    private final transient String bucket;
    private final transient String keyMarker;
    private final transient String uploadIdMarker;
    private final transient String nextKeyMarker;
    private final transient String prefix;
    private final transient String delimiter;
    private final transient String nextUploadIdMarker;
    private final transient Integer maxUploads;
    private final transient Boolean isTruncated;
    private final transient List<MultipartUpload> uploads;
    private final transient List<CommonPrefix> commonPrefixes;
    private final transient EncodingType encodingType;
    private final transient RequestCharged requestCharged;

    private ListMultipartUploadsOutput(Builder builder) {
        this.bucket = builder.bucket;
        this.keyMarker = builder.keyMarker;
        this.uploadIdMarker = builder.uploadIdMarker;
        this.nextKeyMarker = builder.nextKeyMarker;
        this.prefix = builder.prefix;
        this.delimiter = builder.delimiter;
        this.nextUploadIdMarker = builder.nextUploadIdMarker;
        this.maxUploads = builder.maxUploads;
        this.isTruncated = builder.isTruncated;
        this.uploads = builder.uploads == null ? null : Collections.unmodifiableList(builder.uploads);
        this.commonPrefixes = builder.commonPrefixes == null ? null : Collections.unmodifiableList(builder.commonPrefixes);
        this.encodingType = builder.encodingType;
        this.requestCharged = builder.requestCharged;
    }

    /**
     * The name of the bucket to which the multipart upload was initiated. Does not return the access point ARN or
     * access point alias if used.
     */
    public String getBucket() {
        return bucket;
    }

    /**
     * The key at or after which the listing began.
     */
    public String getKeyMarker() {
        return keyMarker;
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
     * When a list is truncated, this element specifies the value that should be used for the key-marker request
     * parameter in a subsequent request.
     */
    public String getNextKeyMarker() {
        return nextKeyMarker;
    }

    /**
     * When a prefix is provided in the request, this field contains the specified prefix. The result contains only keys
     * starting with the specified prefix.
     *
     * <p><b>Directory buckets</b> - For directory buckets, only prefixes that end in a delimiter (<code>/</code>) are
     * supported.
     */
    public String getPrefix() {
        return prefix;
    }

    /**
     * Contains the delimiter you specified in the request. If you don't specify a delimiter in your request, this
     * element is absent from the response.
     *
     * <p><b>Directory buckets</b> - For directory buckets, <code>/</code> is the only supported delimiter.
     */
    public String getDelimiter() {
        return delimiter;
    }

    /**
     * When a list is truncated, this element specifies the value that should be used for the <code>upload-id-marker</code>
     * request parameter in a subsequent request.
     *
     * <p>This functionality is not supported for directory buckets.
     */
    public String getNextUploadIdMarker() {
        return nextUploadIdMarker;
    }

    /**
     * Maximum number of multipart uploads that could have been included in the response.
     */
    public Integer getMaxUploads() {
        return maxUploads;
    }

    /**
     * Indicates whether the returned list of multipart uploads is truncated. A value of true indicates that the list
     * was truncated. The list can be truncated if the number of multipart uploads exceeds the limit allowed or
     * specified by max uploads.
     */
    public Boolean isIsTruncated() {
        return isTruncated;
    }

    /**
     * Container for elements related to a particular multipart upload. A response can contain zero or more
     * <code>Upload</code> elements.
     */
    public List<MultipartUpload> getUploads() {
        if (uploads == null) {
            return Collections.emptyList();
        }
        return uploads;
    }

    public boolean hasUploads() {
        return uploads != null;
    }

    /**
     * If you specify a delimiter in the request, then the result returns each distinct key prefix containing the
     * delimiter in a <code>CommonPrefixes</code> element. The distinct key prefixes are returned in the <code>Prefix</code>
     * child element.
     *
     * <p><b>Directory buckets</b> - For directory buckets, only prefixes that end in a delimiter (<code>/</code>) are
     * supported.
     */
    public List<CommonPrefix> getCommonPrefixes() {
        if (commonPrefixes == null) {
            return Collections.emptyList();
        }
        return commonPrefixes;
    }

    public boolean hasCommonPrefixes() {
        return commonPrefixes != null;
    }

    /**
     * Encoding type used by Amazon S3 to encode object keys in the response.
     *
     * <p>If you specify the <code>encoding-type</code> request parameter, Amazon S3 includes this element in the
     * response, and returns encoded key name values in the following response elements:
     *
     * <p><code>Delimiter</code>, <code>KeyMarker</code>, <code>Prefix</code>, <code>NextKeyMarker</code>, <code>Key</code>
     * .
     */
    public EncodingType getEncodingType() {
        return encodingType;
    }

    public RequestCharged getRequestCharged() {
        return requestCharged;
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
        ListMultipartUploadsOutput that = (ListMultipartUploadsOutput) other;
        return Objects.equals(this.isTruncated, that.isTruncated)
               && Objects.equals(this.maxUploads, that.maxUploads)
               && Objects.equals(this.bucket, that.bucket)
               && Objects.equals(this.keyMarker, that.keyMarker)
               && Objects.equals(this.uploadIdMarker, that.uploadIdMarker)
               && Objects.equals(this.nextKeyMarker, that.nextKeyMarker)
               && Objects.equals(this.prefix, that.prefix)
               && Objects.equals(this.delimiter, that.delimiter)
               && Objects.equals(this.nextUploadIdMarker, that.nextUploadIdMarker)
               && Objects.equals(this.encodingType, that.encodingType)
               && Objects.equals(this.requestCharged, that.requestCharged)
               && Objects.equals(this.uploads, that.uploads)
               && Objects.equals(this.commonPrefixes, that.commonPrefixes);
    }

    @Override
    public int hashCode() {
        int $hc = Objects.hashCode(bucket);
        $hc = 31 * $hc + Objects.hashCode(keyMarker);
        $hc = 31 * $hc + Objects.hashCode(uploadIdMarker);
        $hc = 31 * $hc + Objects.hashCode(nextKeyMarker);
        $hc = 31 * $hc + Objects.hashCode(prefix);
        $hc = 31 * $hc + Objects.hashCode(delimiter);
        $hc = 31 * $hc + Objects.hashCode(nextUploadIdMarker);
        $hc = 31 * $hc + Objects.hashCode(maxUploads);
        $hc = 31 * $hc + Objects.hashCode(isTruncated);
        $hc = 31 * $hc + Objects.hashCode(uploads);
        $hc = 31 * $hc + Objects.hashCode(commonPrefixes);
        $hc = 31 * $hc + Objects.hashCode(encodingType);
        $hc = 31 * $hc + Objects.hashCode(requestCharged);
        return $hc;
    }

    @Override
    public Schema schema() {
        return $SCHEMA;
    }

    @Override
    public void serializeMembers(ShapeSerializer serializer) {
        if (bucket != null) {
            serializer.writeString($SCHEMA_BUCKET, bucket);
        }
        if (keyMarker != null) {
            serializer.writeString($SCHEMA_KEY_MARKER, keyMarker);
        }
        if (uploadIdMarker != null) {
            serializer.writeString($SCHEMA_UPLOAD_ID_MARKER, uploadIdMarker);
        }
        if (nextKeyMarker != null) {
            serializer.writeString($SCHEMA_NEXT_KEY_MARKER, nextKeyMarker);
        }
        if (prefix != null) {
            serializer.writeString($SCHEMA_PREFIX, prefix);
        }
        if (delimiter != null) {
            serializer.writeString($SCHEMA_DELIMITER, delimiter);
        }
        if (nextUploadIdMarker != null) {
            serializer.writeString($SCHEMA_NEXT_UPLOAD_ID_MARKER, nextUploadIdMarker);
        }
        if (maxUploads != null) {
            serializer.writeInteger($SCHEMA_MAX_UPLOADS, maxUploads);
        }
        if (isTruncated != null) {
            serializer.writeBoolean($SCHEMA_IS_TRUNCATED, isTruncated);
        }
        if (uploads != null) {
            serializer.writeList($SCHEMA_UPLOADS, uploads, uploads.size(), SharedSerde.MultipartUploadListSerializer.INSTANCE);
        }
        if (commonPrefixes != null) {
            serializer.writeList($SCHEMA_COMMON_PREFIXES, commonPrefixes, commonPrefixes.size(), SharedSerde.CommonPrefixListSerializer.INSTANCE);
        }
        if (encodingType != null) {
            serializer.writeString($SCHEMA_ENCODING_TYPE, encodingType.getValue());
        }
        if (requestCharged != null) {
            serializer.writeString($SCHEMA_REQUEST_CHARGED, requestCharged.getValue());
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T getMemberValue(Schema member) {
        return switch (member.memberIndex()) {
            case 0 -> (T) SchemaUtils.validateSameMember($SCHEMA_BUCKET, member, bucket);
            case 1 -> (T) SchemaUtils.validateSameMember($SCHEMA_KEY_MARKER, member, keyMarker);
            case 2 -> (T) SchemaUtils.validateSameMember($SCHEMA_UPLOAD_ID_MARKER, member, uploadIdMarker);
            case 3 -> (T) SchemaUtils.validateSameMember($SCHEMA_NEXT_KEY_MARKER, member, nextKeyMarker);
            case 4 -> (T) SchemaUtils.validateSameMember($SCHEMA_PREFIX, member, prefix);
            case 5 -> (T) SchemaUtils.validateSameMember($SCHEMA_DELIMITER, member, delimiter);
            case 6 -> (T) SchemaUtils.validateSameMember($SCHEMA_NEXT_UPLOAD_ID_MARKER, member, nextUploadIdMarker);
            case 7 -> (T) SchemaUtils.validateSameMember($SCHEMA_MAX_UPLOADS, member, maxUploads);
            case 8 -> (T) SchemaUtils.validateSameMember($SCHEMA_IS_TRUNCATED, member, isTruncated);
            case 9 -> (T) SchemaUtils.validateSameMember($SCHEMA_UPLOADS, member, uploads);
            case 10 -> (T) SchemaUtils.validateSameMember($SCHEMA_COMMON_PREFIXES, member, commonPrefixes);
            case 11 -> (T) SchemaUtils.validateSameMember($SCHEMA_ENCODING_TYPE, member, encodingType);
            case 12 -> (T) SchemaUtils.validateSameMember($SCHEMA_REQUEST_CHARGED, member, requestCharged);
            default -> throw new IllegalArgumentException("Attempted to get non-existent member: " + member.id());
        };
    }

    /**
     * Create a new builder containing all the current property values of this object.
     *
     * <p><strong>Note:</strong> This method performs only a shallow copy of the original properties.
     *
     * @return a builder for {@link ListMultipartUploadsOutput}.
     */
    public Builder toBuilder() {
        var builder = new Builder();
        builder.bucket(this.bucket);
        builder.keyMarker(this.keyMarker);
        builder.uploadIdMarker(this.uploadIdMarker);
        builder.nextKeyMarker(this.nextKeyMarker);
        builder.prefix(this.prefix);
        builder.delimiter(this.delimiter);
        builder.nextUploadIdMarker(this.nextUploadIdMarker);
        builder.maxUploads(this.maxUploads);
        builder.isTruncated(this.isTruncated);
        builder.uploads(this.uploads);
        builder.commonPrefixes(this.commonPrefixes);
        builder.encodingType(this.encodingType);
        builder.requestCharged(this.requestCharged);
        return builder;
    }

    /**
     * @return returns a new Builder.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for {@link ListMultipartUploadsOutput}.
     */
    public static final class Builder implements ShapeBuilder<ListMultipartUploadsOutput> {
        private String bucket;
        private String keyMarker;
        private String uploadIdMarker;
        private String nextKeyMarker;
        private String prefix;
        private String delimiter;
        private String nextUploadIdMarker;
        private Integer maxUploads;
        private Boolean isTruncated;
        private List<MultipartUpload> uploads;
        private List<CommonPrefix> commonPrefixes;
        private EncodingType encodingType;
        private RequestCharged requestCharged;

        private Builder() {}

        @Override
        public Schema schema() {
            return $SCHEMA;
        }

        /**
         * The name of the bucket to which the multipart upload was initiated. Does not return the access point ARN or
         * access point alias if used.
         *
         * @return this builder.
         */
        public Builder bucket(String bucket) {
            this.bucket = bucket;
            return this;
        }

        /**
         * The key at or after which the listing began.
         *
         * @return this builder.
         */
        public Builder keyMarker(String keyMarker) {
            this.keyMarker = keyMarker;
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
         * When a list is truncated, this element specifies the value that should be used for the key-marker request
         * parameter in a subsequent request.
         *
         * @return this builder.
         */
        public Builder nextKeyMarker(String nextKeyMarker) {
            this.nextKeyMarker = nextKeyMarker;
            return this;
        }

        /**
         * When a prefix is provided in the request, this field contains the specified prefix. The result contains only keys
         * starting with the specified prefix.
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
         * Contains the delimiter you specified in the request. If you don't specify a delimiter in your request, this
         * element is absent from the response.
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
         * When a list is truncated, this element specifies the value that should be used for the <code>upload-id-marker</code>
         * request parameter in a subsequent request.
         *
         * <p>This functionality is not supported for directory buckets.
         *
         * @return this builder.
         */
        public Builder nextUploadIdMarker(String nextUploadIdMarker) {
            this.nextUploadIdMarker = nextUploadIdMarker;
            return this;
        }

        /**
         * Maximum number of multipart uploads that could have been included in the response.
         *
         * @return this builder.
         */
        public Builder maxUploads(Integer maxUploads) {
            this.maxUploads = maxUploads;
            return this;
        }

        /**
         * Indicates whether the returned list of multipart uploads is truncated. A value of true indicates that the list
         * was truncated. The list can be truncated if the number of multipart uploads exceeds the limit allowed or
         * specified by max uploads.
         *
         * @return this builder.
         */
        public Builder isTruncated(Boolean isTruncated) {
            this.isTruncated = isTruncated;
            return this;
        }

        /**
         * Container for elements related to a particular multipart upload. A response can contain zero or more
         * <code>Upload</code> elements.
         *
         * @return this builder.
         */
        public Builder uploads(List<MultipartUpload> uploads) {
            this.uploads = uploads;
            return this;
        }

        /**
         * If you specify a delimiter in the request, then the result returns each distinct key prefix containing the
         * delimiter in a <code>CommonPrefixes</code> element. The distinct key prefixes are returned in the <code>Prefix</code>
         * child element.
         *
         * <p><b>Directory buckets</b> - For directory buckets, only prefixes that end in a delimiter (<code>/</code>) are
         * supported.
         *
         * @return this builder.
         */
        public Builder commonPrefixes(List<CommonPrefix> commonPrefixes) {
            this.commonPrefixes = commonPrefixes;
            return this;
        }

        /**
         * Encoding type used by Amazon S3 to encode object keys in the response.
         *
         * <p>If you specify the <code>encoding-type</code> request parameter, Amazon S3 includes this element in the
         * response, and returns encoded key name values in the following response elements:
         *
         * <p><code>Delimiter</code>, <code>KeyMarker</code>, <code>Prefix</code>, <code>NextKeyMarker</code>, <code>Key</code>
         * .
         *
         * @return this builder.
         */
        public Builder encodingType(EncodingType encodingType) {
            this.encodingType = encodingType;
            return this;
        }

        /**
         * @return this builder.
         */
        public Builder requestCharged(RequestCharged requestCharged) {
            this.requestCharged = requestCharged;
            return this;
        }

        @Override
        public ListMultipartUploadsOutput build() {
            return new ListMultipartUploadsOutput(this);
        }

        @Override
        @SuppressWarnings("unchecked")
        public void setMemberValue(Schema member, Object value) {
            switch (member.memberIndex()) {
                case 0 -> bucket((String) SchemaUtils.validateSameMember($SCHEMA_BUCKET, member, value));
                case 1 -> keyMarker((String) SchemaUtils.validateSameMember($SCHEMA_KEY_MARKER, member, value));
                case 2 -> uploadIdMarker((String) SchemaUtils.validateSameMember($SCHEMA_UPLOAD_ID_MARKER, member, value));
                case 3 -> nextKeyMarker((String) SchemaUtils.validateSameMember($SCHEMA_NEXT_KEY_MARKER, member, value));
                case 4 -> prefix((String) SchemaUtils.validateSameMember($SCHEMA_PREFIX, member, value));
                case 5 -> delimiter((String) SchemaUtils.validateSameMember($SCHEMA_DELIMITER, member, value));
                case 6 -> nextUploadIdMarker((String) SchemaUtils.validateSameMember($SCHEMA_NEXT_UPLOAD_ID_MARKER, member, value));
                case 7 -> maxUploads((Integer) SchemaUtils.validateSameMember($SCHEMA_MAX_UPLOADS, member, value));
                case 8 -> isTruncated((Boolean) SchemaUtils.validateSameMember($SCHEMA_IS_TRUNCATED, member, value));
                case 9 -> uploads((List<MultipartUpload>) SchemaUtils.validateSameMember($SCHEMA_UPLOADS, member, value));
                case 10 -> commonPrefixes((List<CommonPrefix>) SchemaUtils.validateSameMember($SCHEMA_COMMON_PREFIXES, member, value));
                case 11 -> encodingType((EncodingType) SchemaUtils.validateSameMember($SCHEMA_ENCODING_TYPE, member, value));
                case 12 -> requestCharged((RequestCharged) SchemaUtils.validateSameMember($SCHEMA_REQUEST_CHARGED, member, value));
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
                    case 0 -> builder.bucket(de.readString(member));
                    case 1 -> builder.keyMarker(de.readString(member));
                    case 2 -> builder.uploadIdMarker(de.readString(member));
                    case 3 -> builder.nextKeyMarker(de.readString(member));
                    case 4 -> builder.prefix(de.readString(member));
                    case 5 -> builder.delimiter(de.readString(member));
                    case 6 -> builder.nextUploadIdMarker(de.readString(member));
                    case 7 -> builder.maxUploads(de.readInteger(member));
                    case 8 -> builder.isTruncated(de.readBoolean(member));
                    case 9 -> builder.uploads(SharedSerde.deserializeMultipartUploadList(member, de));
                    case 10 -> builder.commonPrefixes(SharedSerde.deserializeCommonPrefixList(member, de));
                    case 11 -> builder.encodingType(EncodingType.builder().deserializeMember(de, member).build());
                    case 12 -> builder.requestCharged(RequestCharged.builder().deserializeMember(de, member).build());
                    default -> throw new IllegalArgumentException("Unexpected member: " + member.memberName());
                }
            }
        }
    }
}
