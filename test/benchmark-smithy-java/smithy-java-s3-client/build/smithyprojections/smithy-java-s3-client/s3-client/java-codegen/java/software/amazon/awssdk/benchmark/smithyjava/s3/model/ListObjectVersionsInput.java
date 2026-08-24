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
public final class ListObjectVersionsInput implements SerializableStruct {

    public static final Schema $SCHEMA = Schemas4.LIST_OBJECT_VERSIONS_INPUT;
    private static final Schema $SCHEMA_BUCKET = $SCHEMA.member("Bucket");
    private static final Schema $SCHEMA_DELIMITER = $SCHEMA.member("Delimiter");
    private static final Schema $SCHEMA_ENCODING_TYPE = $SCHEMA.member("EncodingType");
    private static final Schema $SCHEMA_KEY_MARKER = $SCHEMA.member("KeyMarker");
    private static final Schema $SCHEMA_MAX_KEYS = $SCHEMA.member("MaxKeys");
    private static final Schema $SCHEMA_PREFIX = $SCHEMA.member("Prefix");
    private static final Schema $SCHEMA_VERSION_ID_MARKER = $SCHEMA.member("VersionIdMarker");
    private static final Schema $SCHEMA_EXPECTED_BUCKET_OWNER = $SCHEMA.member("ExpectedBucketOwner");
    private static final Schema $SCHEMA_REQUEST_PAYER = $SCHEMA.member("RequestPayer");
    private static final Schema $SCHEMA_OPTIONAL_OBJECT_ATTRIBUTES = $SCHEMA.member("OptionalObjectAttributes");

    public static final ShapeId $ID = $SCHEMA.id();

    private final transient String bucket;
    private final transient String delimiter;
    private final transient EncodingType encodingType;
    private final transient String keyMarker;
    private final transient Integer maxKeys;
    private final transient String prefix;
    private final transient String versionIdMarker;
    private final transient String expectedBucketOwner;
    private final transient RequestPayer requestPayer;
    private final transient List<OptionalObjectAttributes> optionalObjectAttributes;

    private ListObjectVersionsInput(Builder builder) {
        this.bucket = builder.bucket;
        this.delimiter = builder.delimiter;
        this.encodingType = builder.encodingType;
        this.keyMarker = builder.keyMarker;
        this.maxKeys = builder.maxKeys;
        this.prefix = builder.prefix;
        this.versionIdMarker = builder.versionIdMarker;
        this.expectedBucketOwner = builder.expectedBucketOwner;
        this.requestPayer = builder.requestPayer;
        this.optionalObjectAttributes = builder.optionalObjectAttributes == null ? null : Collections.unmodifiableList(builder.optionalObjectAttributes);
    }

    /**
     * The bucket name that contains the objects.
     */
    public String getBucket() {
        return bucket;
    }

    /**
     * A delimiter is a character that you specify to group keys. All keys that contain the same string between the <code>
     * prefix</code> and the first occurrence of the delimiter are grouped under a single result element in <code>
     * CommonPrefixes</code>. These groups are counted as one result against the <code>max-keys</code> limitation. These
     * keys are not returned elsewhere in the response.
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
     * Specifies the key to start with when listing objects in a bucket.
     */
    public String getKeyMarker() {
        return keyMarker;
    }

    /**
     * Sets the maximum number of keys returned in the response. By default, the action returns up to 1,000 key names.
     * The response might contain fewer keys but will never contain more. If additional keys satisfy the search
     * criteria, but were not returned because <code>max-keys</code> was exceeded, the response contains <code>true</code>
     * . To return the additional keys, see <code>key-marker</code> and <code>version-id-marker</code>.
     */
    public Integer getMaxKeys() {
        return maxKeys;
    }

    /**
     * Use this parameter to select only those keys that begin with the specified prefix. You can use prefixes to
     * separate a bucket into different groupings of keys. (You can think of using <code>prefix</code> to make groups in
     * the same way that you'd use a folder in a file system.) You can use <code>prefix</code> with
     * <code>delimiter</code> to roll up numerous objects into a single result under <code>CommonPrefixes</code>.
     */
    public String getPrefix() {
        return prefix;
    }

    /**
     * Specifies the object version you want to start listing from.
     */
    public String getVersionIdMarker() {
        return versionIdMarker;
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
        ListObjectVersionsInput that = (ListObjectVersionsInput) other;
        return Objects.equals(this.maxKeys, that.maxKeys)
               && Objects.equals(this.bucket, that.bucket)
               && Objects.equals(this.delimiter, that.delimiter)
               && Objects.equals(this.keyMarker, that.keyMarker)
               && Objects.equals(this.prefix, that.prefix)
               && Objects.equals(this.versionIdMarker, that.versionIdMarker)
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
        $hc = 31 * $hc + Objects.hashCode(keyMarker);
        $hc = 31 * $hc + Objects.hashCode(maxKeys);
        $hc = 31 * $hc + Objects.hashCode(prefix);
        $hc = 31 * $hc + Objects.hashCode(versionIdMarker);
        $hc = 31 * $hc + Objects.hashCode(expectedBucketOwner);
        $hc = 31 * $hc + Objects.hashCode(requestPayer);
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
        if (keyMarker != null) {
            serializer.writeString($SCHEMA_KEY_MARKER, keyMarker);
        }
        if (maxKeys != null) {
            serializer.writeInteger($SCHEMA_MAX_KEYS, maxKeys);
        }
        if (prefix != null) {
            serializer.writeString($SCHEMA_PREFIX, prefix);
        }
        if (versionIdMarker != null) {
            serializer.writeString($SCHEMA_VERSION_ID_MARKER, versionIdMarker);
        }
        if (expectedBucketOwner != null) {
            serializer.writeString($SCHEMA_EXPECTED_BUCKET_OWNER, expectedBucketOwner);
        }
        if (requestPayer != null) {
            serializer.writeString($SCHEMA_REQUEST_PAYER, requestPayer.getValue());
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
            case 3 -> (T) SchemaUtils.validateSameMember($SCHEMA_KEY_MARKER, member, keyMarker);
            case 4 -> (T) SchemaUtils.validateSameMember($SCHEMA_MAX_KEYS, member, maxKeys);
            case 5 -> (T) SchemaUtils.validateSameMember($SCHEMA_PREFIX, member, prefix);
            case 6 -> (T) SchemaUtils.validateSameMember($SCHEMA_VERSION_ID_MARKER, member, versionIdMarker);
            case 7 -> (T) SchemaUtils.validateSameMember($SCHEMA_EXPECTED_BUCKET_OWNER, member, expectedBucketOwner);
            case 8 -> (T) SchemaUtils.validateSameMember($SCHEMA_REQUEST_PAYER, member, requestPayer);
            case 9 -> (T) SchemaUtils.validateSameMember($SCHEMA_OPTIONAL_OBJECT_ATTRIBUTES, member, optionalObjectAttributes);
            default -> throw new IllegalArgumentException("Attempted to get non-existent member: " + member.id());
        };
    }

    /**
     * Create a new builder containing all the current property values of this object.
     *
     * <p><strong>Note:</strong> This method performs only a shallow copy of the original properties.
     *
     * @return a builder for {@link ListObjectVersionsInput}.
     */
    public Builder toBuilder() {
        var builder = new Builder();
        builder.bucket(this.bucket);
        builder.delimiter(this.delimiter);
        builder.encodingType(this.encodingType);
        builder.keyMarker(this.keyMarker);
        builder.maxKeys(this.maxKeys);
        builder.prefix(this.prefix);
        builder.versionIdMarker(this.versionIdMarker);
        builder.expectedBucketOwner(this.expectedBucketOwner);
        builder.requestPayer(this.requestPayer);
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
     * Builder for {@link ListObjectVersionsInput}.
     */
    public static final class Builder implements ShapeBuilder<ListObjectVersionsInput> {
        private final PresenceTracker tracker = PresenceTracker.of($SCHEMA);
        private String bucket;
        private String delimiter;
        private EncodingType encodingType;
        private String keyMarker;
        private Integer maxKeys;
        private String prefix;
        private String versionIdMarker;
        private String expectedBucketOwner;
        private RequestPayer requestPayer;
        private List<OptionalObjectAttributes> optionalObjectAttributes;

        private Builder() {}

        @Override
        public Schema schema() {
            return $SCHEMA;
        }

        /**
         * The bucket name that contains the objects.
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
         * A delimiter is a character that you specify to group keys. All keys that contain the same string between the <code>
         * prefix</code> and the first occurrence of the delimiter are grouped under a single result element in <code>
         * CommonPrefixes</code>. These groups are counted as one result against the <code>max-keys</code> limitation. These
         * keys are not returned elsewhere in the response.
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
         * Specifies the key to start with when listing objects in a bucket.
         *
         * @return this builder.
         */
        public Builder keyMarker(String keyMarker) {
            this.keyMarker = keyMarker;
            return this;
        }

        /**
         * Sets the maximum number of keys returned in the response. By default, the action returns up to 1,000 key names.
         * The response might contain fewer keys but will never contain more. If additional keys satisfy the search
         * criteria, but were not returned because <code>max-keys</code> was exceeded, the response contains <code>true</code>
         * . To return the additional keys, see <code>key-marker</code> and <code>version-id-marker</code>.
         *
         * @return this builder.
         */
        public Builder maxKeys(Integer maxKeys) {
            this.maxKeys = maxKeys;
            return this;
        }

        /**
         * Use this parameter to select only those keys that begin with the specified prefix. You can use prefixes to
         * separate a bucket into different groupings of keys. (You can think of using <code>prefix</code> to make groups in
         * the same way that you'd use a folder in a file system.) You can use <code>prefix</code> with
         * <code>delimiter</code> to roll up numerous objects into a single result under <code>CommonPrefixes</code>.
         *
         * @return this builder.
         */
        public Builder prefix(String prefix) {
            this.prefix = prefix;
            return this;
        }

        /**
         * Specifies the object version you want to start listing from.
         *
         * @return this builder.
         */
        public Builder versionIdMarker(String versionIdMarker) {
            this.versionIdMarker = versionIdMarker;
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
        public ListObjectVersionsInput build() {
            tracker.validate();
            return new ListObjectVersionsInput(this);
        }

        @Override
        @SuppressWarnings("unchecked")
        public void setMemberValue(Schema member, Object value) {
            switch (member.memberIndex()) {
                case 0 -> bucket((String) SchemaUtils.validateSameMember($SCHEMA_BUCKET, member, value));
                case 1 -> delimiter((String) SchemaUtils.validateSameMember($SCHEMA_DELIMITER, member, value));
                case 2 -> encodingType((EncodingType) SchemaUtils.validateSameMember($SCHEMA_ENCODING_TYPE, member, value));
                case 3 -> keyMarker((String) SchemaUtils.validateSameMember($SCHEMA_KEY_MARKER, member, value));
                case 4 -> maxKeys((Integer) SchemaUtils.validateSameMember($SCHEMA_MAX_KEYS, member, value));
                case 5 -> prefix((String) SchemaUtils.validateSameMember($SCHEMA_PREFIX, member, value));
                case 6 -> versionIdMarker((String) SchemaUtils.validateSameMember($SCHEMA_VERSION_ID_MARKER, member, value));
                case 7 -> expectedBucketOwner((String) SchemaUtils.validateSameMember($SCHEMA_EXPECTED_BUCKET_OWNER, member, value));
                case 8 -> requestPayer((RequestPayer) SchemaUtils.validateSameMember($SCHEMA_REQUEST_PAYER, member, value));
                case 9 -> optionalObjectAttributes((List<OptionalObjectAttributes>) SchemaUtils.validateSameMember($SCHEMA_OPTIONAL_OBJECT_ATTRIBUTES, member, value));
                default -> ShapeBuilder.super.setMemberValue(member, value);
            }
        }

        @Override
        public ShapeBuilder<ListObjectVersionsInput> errorCorrection() {
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
                    case 4 -> builder.maxKeys(de.readInteger(member));
                    case 5 -> builder.prefix(de.readString(member));
                    case 6 -> builder.versionIdMarker(de.readString(member));
                    case 7 -> builder.expectedBucketOwner(de.readString(member));
                    case 8 -> builder.requestPayer(RequestPayer.builder().deserializeMember(de, member).build());
                    case 9 -> builder.optionalObjectAttributes(SharedSerde.deserializeOptionalObjectAttributesList(member, de));
                    default -> throw new IllegalArgumentException("Unexpected member: " + member.memberName());
                }
            }
        }
    }
}
