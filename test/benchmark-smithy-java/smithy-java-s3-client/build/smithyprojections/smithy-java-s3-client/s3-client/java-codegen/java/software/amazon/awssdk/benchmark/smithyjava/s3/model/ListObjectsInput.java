package software.amazon.awssdk.benchmark.smithyjava.s3.model;

import java.util.Collections;
import java.util.List;
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
public final class ListObjectsInput implements SerializableStruct {

    public static final Schema $SCHEMA = Schemas4.LIST_OBJECTS_INPUT;
    private static final Schema $SCHEMA_BUCKET = $SCHEMA.member("Bucket");
    private static final Schema $SCHEMA_DELIMITER = $SCHEMA.member("Delimiter");
    private static final Schema $SCHEMA_ENCODING_TYPE = $SCHEMA.member("EncodingType");
    private static final Schema $SCHEMA_MARKER = $SCHEMA.member("Marker");
    private static final Schema $SCHEMA_MAX_KEYS = $SCHEMA.member("MaxKeys");
    private static final Schema $SCHEMA_PREFIX = $SCHEMA.member("Prefix");
    private static final Schema $SCHEMA_REQUEST_PAYER = $SCHEMA.member("RequestPayer");
    private static final Schema $SCHEMA_EXPECTED_BUCKET_OWNER = $SCHEMA.member("ExpectedBucketOwner");
    private static final Schema $SCHEMA_OPTIONAL_OBJECT_ATTRIBUTES = $SCHEMA.member("OptionalObjectAttributes");

    public static final ShapeId $ID = $SCHEMA.id();

    private final transient String bucket;
    private final transient String delimiter;
    private final transient EncodingType encodingType;
    private final transient String marker;
    private final transient Integer maxKeys;
    private final transient String prefix;
    private final transient RequestPayer requestPayer;
    private final transient String expectedBucketOwner;
    private final transient List<OptionalObjectAttributes> optionalObjectAttributes;

    private ListObjectsInput(Builder builder) {
        this.bucket = builder.bucket;
        this.delimiter = builder.delimiter;
        this.encodingType = builder.encodingType;
        this.marker = builder.marker;
        this.maxKeys = builder.maxKeys;
        this.prefix = builder.prefix;
        this.requestPayer = builder.requestPayer;
        this.expectedBucketOwner = builder.expectedBucketOwner;
        this.optionalObjectAttributes = builder.optionalObjectAttributes == null ? null : Collections.unmodifiableList(builder.optionalObjectAttributes);
    }

    /**
     * The name of the bucket containing the objects.
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
     * A delimiter is a character that you use to group keys.
     *
     * <p><code>CommonPrefixes</code> is filtered out from results if it is not lexicographically greater than the
     * key-marker.
     */
    public String getDelimiter() {
        return delimiter;
    }

    public EncodingType getEncodingType() {
        return encodingType;
    }

    /**
     * Marker is where you want Amazon S3 to start listing from. Amazon S3 starts listing after this specified key.
     * Marker can be any key in the bucket.
     */
    public String getMarker() {
        return marker;
    }

    /**
     * Sets the maximum number of keys returned in the response. By default, the action returns up to 1,000 key names.
     * The response might contain fewer keys but will never contain more.
     */
    public Integer getMaxKeys() {
        return maxKeys;
    }

    /**
     * Limits the response to keys that begin with the specified prefix.
     */
    public String getPrefix() {
        return prefix;
    }

    /**
     * Confirms that the requester knows that she or he will be charged for the list objects request. Bucket owners need
     * not specify this parameter in their requests.
     */
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
     * Specifies the optional fields that you want returned in the response. Fields that you do not specify are not
     * returned.
     */
    public List<OptionalObjectAttributes> getOptionalObjectAttributes() {
        if (optionalObjectAttributes == null) {
            return Collections.emptyList();
        }
        return optionalObjectAttributes;
    }

    public boolean hasOptionalObjectAttributes() {
        return optionalObjectAttributes != null;
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
        ListObjectsInput that = (ListObjectsInput) other;
        return Objects.equals(this.maxKeys, that.maxKeys)
               && Objects.equals(this.bucket, that.bucket)
               && Objects.equals(this.delimiter, that.delimiter)
               && Objects.equals(this.marker, that.marker)
               && Objects.equals(this.prefix, that.prefix)
               && Objects.equals(this.expectedBucketOwner, that.expectedBucketOwner)
               && Objects.equals(this.encodingType, that.encodingType)
               && Objects.equals(this.requestPayer, that.requestPayer)
               && Objects.equals(this.optionalObjectAttributes, that.optionalObjectAttributes);
    }

    @Override
    public int hashCode() {
        int $hc = Objects.hashCode(bucket);
        $hc = 31 * $hc + Objects.hashCode(delimiter);
        $hc = 31 * $hc + Objects.hashCode(encodingType);
        $hc = 31 * $hc + Objects.hashCode(marker);
        $hc = 31 * $hc + Objects.hashCode(maxKeys);
        $hc = 31 * $hc + Objects.hashCode(prefix);
        $hc = 31 * $hc + Objects.hashCode(requestPayer);
        $hc = 31 * $hc + Objects.hashCode(expectedBucketOwner);
        $hc = 31 * $hc + Objects.hashCode(optionalObjectAttributes);
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
        if (marker != null) {
            serializer.writeString($SCHEMA_MARKER, marker);
        }
        if (maxKeys != null) {
            serializer.writeInteger($SCHEMA_MAX_KEYS, maxKeys);
        }
        if (prefix != null) {
            serializer.writeString($SCHEMA_PREFIX, prefix);
        }
        if (requestPayer != null) {
            serializer.writeString($SCHEMA_REQUEST_PAYER, requestPayer.getValue());
        }
        if (expectedBucketOwner != null) {
            serializer.writeString($SCHEMA_EXPECTED_BUCKET_OWNER, expectedBucketOwner);
        }
        if (optionalObjectAttributes != null) {
            serializer.writeList($SCHEMA_OPTIONAL_OBJECT_ATTRIBUTES, optionalObjectAttributes, optionalObjectAttributes.size(), SharedSerde.OptionalObjectAttributesListSerializer.INSTANCE);
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T getMemberValue(Schema member) {
        return switch (member.memberIndex()) {
            case 0 -> (T) SchemaUtils.validateSameMember($SCHEMA_BUCKET, member, bucket);
            case 1 -> (T) SchemaUtils.validateSameMember($SCHEMA_DELIMITER, member, delimiter);
            case 2 -> (T) SchemaUtils.validateSameMember($SCHEMA_ENCODING_TYPE, member, encodingType);
            case 3 -> (T) SchemaUtils.validateSameMember($SCHEMA_MARKER, member, marker);
            case 4 -> (T) SchemaUtils.validateSameMember($SCHEMA_MAX_KEYS, member, maxKeys);
            case 5 -> (T) SchemaUtils.validateSameMember($SCHEMA_PREFIX, member, prefix);
            case 6 -> (T) SchemaUtils.validateSameMember($SCHEMA_REQUEST_PAYER, member, requestPayer);
            case 7 -> (T) SchemaUtils.validateSameMember($SCHEMA_EXPECTED_BUCKET_OWNER, member, expectedBucketOwner);
            case 8 -> (T) SchemaUtils.validateSameMember($SCHEMA_OPTIONAL_OBJECT_ATTRIBUTES, member, optionalObjectAttributes);
            default -> throw new IllegalArgumentException("Attempted to get non-existent member: " + member.id());
        };
    }

    /**
     * Create a new builder containing all the current property values of this object.
     *
     * <p><strong>Note:</strong> This method performs only a shallow copy of the original properties.
     *
     * @return a builder for {@link ListObjectsInput}.
     */
    public Builder toBuilder() {
        var builder = new Builder();
        builder.bucket(this.bucket);
        builder.delimiter(this.delimiter);
        builder.encodingType(this.encodingType);
        builder.marker(this.marker);
        builder.maxKeys(this.maxKeys);
        builder.prefix(this.prefix);
        builder.requestPayer(this.requestPayer);
        builder.expectedBucketOwner(this.expectedBucketOwner);
        builder.optionalObjectAttributes(this.optionalObjectAttributes);
        return builder;
    }

    /**
     * @return returns a new Builder.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for {@link ListObjectsInput}.
     */
    public static final class Builder implements ShapeBuilder<ListObjectsInput> {
        private final PresenceTracker tracker = PresenceTracker.of($SCHEMA);
        private String bucket;
        private String delimiter;
        private EncodingType encodingType;
        private String marker;
        private Integer maxKeys;
        private String prefix;
        private RequestPayer requestPayer;
        private String expectedBucketOwner;
        private List<OptionalObjectAttributes> optionalObjectAttributes;

        private Builder() {}

        @Override
        public Schema schema() {
            return $SCHEMA;
        }

        /**
         * The name of the bucket containing the objects.
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
         * A delimiter is a character that you use to group keys.
         *
         * <p><code>CommonPrefixes</code> is filtered out from results if it is not lexicographically greater than the
         * key-marker.
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
         * Marker is where you want Amazon S3 to start listing from. Amazon S3 starts listing after this specified key.
         * Marker can be any key in the bucket.
         *
         * @return this builder.
         */
        public Builder marker(String marker) {
            this.marker = marker;
            return this;
        }

        /**
         * Sets the maximum number of keys returned in the response. By default, the action returns up to 1,000 key names.
         * The response might contain fewer keys but will never contain more.
         *
         * @return this builder.
         */
        public Builder maxKeys(Integer maxKeys) {
            this.maxKeys = maxKeys;
            return this;
        }

        /**
         * Limits the response to keys that begin with the specified prefix.
         *
         * @return this builder.
         */
        public Builder prefix(String prefix) {
            this.prefix = prefix;
            return this;
        }

        /**
         * Confirms that the requester knows that she or he will be charged for the list objects request. Bucket owners need
         * not specify this parameter in their requests.
         *
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
         * Specifies the optional fields that you want returned in the response. Fields that you do not specify are not
         * returned.
         *
         * @return this builder.
         */
        public Builder optionalObjectAttributes(List<OptionalObjectAttributes> optionalObjectAttributes) {
            this.optionalObjectAttributes = optionalObjectAttributes;
            return this;
        }

        @Override
        public ListObjectsInput build() {
            tracker.validate();
            return new ListObjectsInput(this);
        }

        @Override
        @SuppressWarnings("unchecked")
        public void setMemberValue(Schema member, Object value) {
            switch (member.memberIndex()) {
                case 0 -> bucket((String) SchemaUtils.validateSameMember($SCHEMA_BUCKET, member, value));
                case 1 -> delimiter((String) SchemaUtils.validateSameMember($SCHEMA_DELIMITER, member, value));
                case 2 -> encodingType((EncodingType) SchemaUtils.validateSameMember($SCHEMA_ENCODING_TYPE, member, value));
                case 3 -> marker((String) SchemaUtils.validateSameMember($SCHEMA_MARKER, member, value));
                case 4 -> maxKeys((Integer) SchemaUtils.validateSameMember($SCHEMA_MAX_KEYS, member, value));
                case 5 -> prefix((String) SchemaUtils.validateSameMember($SCHEMA_PREFIX, member, value));
                case 6 -> requestPayer((RequestPayer) SchemaUtils.validateSameMember($SCHEMA_REQUEST_PAYER, member, value));
                case 7 -> expectedBucketOwner((String) SchemaUtils.validateSameMember($SCHEMA_EXPECTED_BUCKET_OWNER, member, value));
                case 8 -> optionalObjectAttributes((List<OptionalObjectAttributes>) SchemaUtils.validateSameMember($SCHEMA_OPTIONAL_OBJECT_ATTRIBUTES, member, value));
                default -> ShapeBuilder.super.setMemberValue(member, value);
            }
        }

        @Override
        public ShapeBuilder<ListObjectsInput> errorCorrection() {
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
                    case 3 -> builder.marker(de.readString(member));
                    case 4 -> builder.maxKeys(de.readInteger(member));
                    case 5 -> builder.prefix(de.readString(member));
                    case 6 -> builder.requestPayer(RequestPayer.builder().deserializeMember(de, member).build());
                    case 7 -> builder.expectedBucketOwner(de.readString(member));
                    case 8 -> builder.optionalObjectAttributes(SharedSerde.deserializeOptionalObjectAttributesList(member, de));
                    default -> throw new IllegalArgumentException("Unexpected member: " + member.memberName());
                }
            }
        }
    }
}
