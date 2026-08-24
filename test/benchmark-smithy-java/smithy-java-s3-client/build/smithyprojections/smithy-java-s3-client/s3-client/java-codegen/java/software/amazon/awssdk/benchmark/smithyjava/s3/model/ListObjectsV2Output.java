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
public final class ListObjectsV2Output implements SerializableStruct {

    public static final Schema $SCHEMA = Schemas4.LIST_OBJECTS_V2_OUTPUT;
    private static final Schema $SCHEMA_IS_TRUNCATED = $SCHEMA.member("IsTruncated");
    private static final Schema $SCHEMA_CONTENTS = $SCHEMA.member("Contents");
    private static final Schema $SCHEMA_NAME = $SCHEMA.member("Name");
    private static final Schema $SCHEMA_PREFIX = $SCHEMA.member("Prefix");
    private static final Schema $SCHEMA_DELIMITER = $SCHEMA.member("Delimiter");
    private static final Schema $SCHEMA_MAX_KEYS = $SCHEMA.member("MaxKeys");
    private static final Schema $SCHEMA_COMMON_PREFIXES = $SCHEMA.member("CommonPrefixes");
    private static final Schema $SCHEMA_ENCODING_TYPE = $SCHEMA.member("EncodingType");
    private static final Schema $SCHEMA_KEY_COUNT = $SCHEMA.member("KeyCount");
    private static final Schema $SCHEMA_CONTINUATION_TOKEN = $SCHEMA.member("ContinuationToken");
    private static final Schema $SCHEMA_NEXT_CONTINUATION_TOKEN = $SCHEMA.member("NextContinuationToken");
    private static final Schema $SCHEMA_START_AFTER = $SCHEMA.member("StartAfter");
    private static final Schema $SCHEMA_REQUEST_CHARGED = $SCHEMA.member("RequestCharged");

    public static final ShapeId $ID = $SCHEMA.id();

    private final transient Boolean isTruncated;
    private final transient List<ObjectShape> contents;
    private final transient String name;
    private final transient String prefix;
    private final transient String delimiter;
    private final transient Integer maxKeys;
    private final transient List<CommonPrefix> commonPrefixes;
    private final transient EncodingType encodingType;
    private final transient Integer keyCount;
    private final transient String continuationToken;
    private final transient String nextContinuationToken;
    private final transient String startAfter;
    private final transient RequestCharged requestCharged;

    private ListObjectsV2Output(Builder builder) {
        this.isTruncated = builder.isTruncated;
        this.contents = builder.contents == null ? null : Collections.unmodifiableList(builder.contents);
        this.name = builder.name;
        this.prefix = builder.prefix;
        this.delimiter = builder.delimiter;
        this.maxKeys = builder.maxKeys;
        this.commonPrefixes = builder.commonPrefixes == null ? null : Collections.unmodifiableList(builder.commonPrefixes);
        this.encodingType = builder.encodingType;
        this.keyCount = builder.keyCount;
        this.continuationToken = builder.continuationToken;
        this.nextContinuationToken = builder.nextContinuationToken;
        this.startAfter = builder.startAfter;
        this.requestCharged = builder.requestCharged;
    }

    /**
     * Set to <code>false</code> if all of the results were returned. Set to <code>true</code> if more keys are
     * available to return. If the number of results exceeds that specified by <code>MaxKeys</code>, all of the results
     * might not be returned.
     */
    public Boolean isIsTruncated() {
        return isTruncated;
    }

    /**
     * Metadata about each object returned.
     */
    public List<ObjectShape> getContents() {
        if (contents == null) {
            return Collections.emptyList();
        }
        return contents;
    }

    public boolean hasContents() {
        return contents != null;
    }

    /**
     * The bucket name.
     */
    public String getName() {
        return name;
    }

    /**
     * Keys that begin with the indicated prefix.
     *
     * <p><b>Directory buckets</b> - For directory buckets, only prefixes that end in a delimiter (<code>/</code>) are
     * supported.
     */
    public String getPrefix() {
        return prefix;
    }

    /**
     * Causes keys that contain the same string between the <code>prefix</code> and the first occurrence of the
     * delimiter to be rolled up into a single result element in the <code>CommonPrefixes</code> collection. These
     * rolled-up keys are not returned elsewhere in the response. Each rolled-up result counts as only one return
     * against the <code>MaxKeys</code> value.
     *
     * <p><b>Directory buckets</b> - For directory buckets, <code>/</code> is the only supported delimiter.
     */
    public String getDelimiter() {
        return delimiter;
    }

    /**
     * Sets the maximum number of keys returned in the response. By default, the action returns up to 1,000 key names.
     * The response might contain fewer keys but will never contain more.
     */
    public Integer getMaxKeys() {
        return maxKeys;
    }

    /**
     * All of the keys (up to 1,000) that share the same prefix are grouped together. When counting the total numbers of
     * returns by this API operation, this group of keys is considered as one item.
     *
     * <p>A response can contain <code>CommonPrefixes</code> only if you specify a delimiter.
     *
     * <p><code>CommonPrefixes</code> contains all (if there are any) keys between <code>Prefix</code> and the next
     * occurrence of the string specified by a delimiter.
     *
     * <p><code>CommonPrefixes</code> lists keys that act like subdirectories in the directory specified by <code>Prefix</code>
     * .
     *
     * <p>For example, if the prefix is <code>notes/</code> and the delimiter is a slash (<code>/</code>) as in <code>
     * notes/summer/july</code>, the common prefix is <code>notes/summer/</code>. All of the keys that roll up into a
     * common prefix count as a single return when calculating the number of returns.
     *
     * <ul>
     *   <li>
     *     <b>Directory buckets</b> - For directory buckets, only prefixes that end in a delimiter (<code>/</code>)
     *     are supported.
     *   </li>
     *   <li>
     *     <b>Directory buckets </b> - When you query <code>ListObjectsV2</code> with a delimiter during in-progress
     *     multipart uploads, the <code>CommonPrefixes</code> response parameter contains the prefixes that are
     *     associated with the in-progress multipart uploads. For more information about multipart uploads, see <a href="https://docs.aws.amazon.com/AmazonS3/latest/dev/mpuoverview.html">
     *     Multipart Upload Overview</a> in the <i>Amazon S3 User Guide</i>.
     *   </li>
     * </ul>
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
     * Encoding type used by Amazon S3 to encode object key names in the XML response.
     *
     * <p>If you specify the <code>encoding-type</code> request parameter, Amazon S3 includes this element in the
     * response, and returns encoded key name values in the following response elements:
     *
     * <p><code>Delimiter, Prefix, Key,</code> and <code>StartAfter</code>.
     */
    public EncodingType getEncodingType() {
        return encodingType;
    }

    /**
     * <code>KeyCount</code> is the number of keys returned with this request. <code>KeyCount</code> will always be less
     * than or equal to the <code>MaxKeys</code> field. For example, if you ask for 50 keys, your result will include 50
     * keys or fewer.
     */
    public Integer getKeyCount() {
        return keyCount;
    }

    /**
     * If <code>ContinuationToken</code> was sent with the request, it is included in the response. You can use the
     * returned <code>ContinuationToken</code> for pagination of the list response.
     */
    public String getContinuationToken() {
        return continuationToken;
    }

    /**
     * <code>NextContinuationToken</code> is sent when <code>isTruncated</code> is true, which means there are more keys
     * in the bucket that can be listed. The next list requests to Amazon S3 can be continued with this
     * <code>NextContinuationToken</code>. <code>NextContinuationToken</code> is obfuscated and is not a real key
     */
    public String getNextContinuationToken() {
        return nextContinuationToken;
    }

    /**
     * If StartAfter was sent with the request, it is included in the response.
     *
     * <p>This functionality is not supported for directory buckets.
     */
    public String getStartAfter() {
        return startAfter;
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
        ListObjectsV2Output that = (ListObjectsV2Output) other;
        return Objects.equals(this.isTruncated, that.isTruncated)
               && Objects.equals(this.maxKeys, that.maxKeys)
               && Objects.equals(this.keyCount, that.keyCount)
               && Objects.equals(this.name, that.name)
               && Objects.equals(this.prefix, that.prefix)
               && Objects.equals(this.delimiter, that.delimiter)
               && Objects.equals(this.continuationToken, that.continuationToken)
               && Objects.equals(this.nextContinuationToken, that.nextContinuationToken)
               && Objects.equals(this.startAfter, that.startAfter)
               && Objects.equals(this.encodingType, that.encodingType)
               && Objects.equals(this.requestCharged, that.requestCharged)
               && Objects.equals(this.contents, that.contents)
               && Objects.equals(this.commonPrefixes, that.commonPrefixes);
    }

    @Override
    public int hashCode() {
        int $hc = Objects.hashCode(isTruncated);
        $hc = 31 * $hc + Objects.hashCode(contents);
        $hc = 31 * $hc + Objects.hashCode(name);
        $hc = 31 * $hc + Objects.hashCode(prefix);
        $hc = 31 * $hc + Objects.hashCode(delimiter);
        $hc = 31 * $hc + Objects.hashCode(maxKeys);
        $hc = 31 * $hc + Objects.hashCode(commonPrefixes);
        $hc = 31 * $hc + Objects.hashCode(encodingType);
        $hc = 31 * $hc + Objects.hashCode(keyCount);
        $hc = 31 * $hc + Objects.hashCode(continuationToken);
        $hc = 31 * $hc + Objects.hashCode(nextContinuationToken);
        $hc = 31 * $hc + Objects.hashCode(startAfter);
        $hc = 31 * $hc + Objects.hashCode(requestCharged);
        return $hc;
    }

    @Override
    public Schema schema() {
        return $SCHEMA;
    }

    @Override
    public void serializeMembers(ShapeSerializer serializer) {
        if (isTruncated != null) {
            serializer.writeBoolean($SCHEMA_IS_TRUNCATED, isTruncated);
        }
        if (contents != null) {
            serializer.writeList($SCHEMA_CONTENTS, contents, contents.size(), SharedSerde.ObjectListSerializer.INSTANCE);
        }
        if (name != null) {
            serializer.writeString($SCHEMA_NAME, name);
        }
        if (prefix != null) {
            serializer.writeString($SCHEMA_PREFIX, prefix);
        }
        if (delimiter != null) {
            serializer.writeString($SCHEMA_DELIMITER, delimiter);
        }
        if (maxKeys != null) {
            serializer.writeInteger($SCHEMA_MAX_KEYS, maxKeys);
        }
        if (commonPrefixes != null) {
            serializer.writeList($SCHEMA_COMMON_PREFIXES, commonPrefixes, commonPrefixes.size(), SharedSerde.CommonPrefixListSerializer.INSTANCE);
        }
        if (encodingType != null) {
            serializer.writeString($SCHEMA_ENCODING_TYPE, encodingType.getValue());
        }
        if (keyCount != null) {
            serializer.writeInteger($SCHEMA_KEY_COUNT, keyCount);
        }
        if (continuationToken != null) {
            serializer.writeString($SCHEMA_CONTINUATION_TOKEN, continuationToken);
        }
        if (nextContinuationToken != null) {
            serializer.writeString($SCHEMA_NEXT_CONTINUATION_TOKEN, nextContinuationToken);
        }
        if (startAfter != null) {
            serializer.writeString($SCHEMA_START_AFTER, startAfter);
        }
        if (requestCharged != null) {
            serializer.writeString($SCHEMA_REQUEST_CHARGED, requestCharged.getValue());
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T getMemberValue(Schema member) {
        return switch (member.memberIndex()) {
            case 0 -> (T) SchemaUtils.validateSameMember($SCHEMA_IS_TRUNCATED, member, isTruncated);
            case 1 -> (T) SchemaUtils.validateSameMember($SCHEMA_CONTENTS, member, contents);
            case 2 -> (T) SchemaUtils.validateSameMember($SCHEMA_NAME, member, name);
            case 3 -> (T) SchemaUtils.validateSameMember($SCHEMA_PREFIX, member, prefix);
            case 4 -> (T) SchemaUtils.validateSameMember($SCHEMA_DELIMITER, member, delimiter);
            case 5 -> (T) SchemaUtils.validateSameMember($SCHEMA_MAX_KEYS, member, maxKeys);
            case 6 -> (T) SchemaUtils.validateSameMember($SCHEMA_COMMON_PREFIXES, member, commonPrefixes);
            case 7 -> (T) SchemaUtils.validateSameMember($SCHEMA_ENCODING_TYPE, member, encodingType);
            case 8 -> (T) SchemaUtils.validateSameMember($SCHEMA_KEY_COUNT, member, keyCount);
            case 9 -> (T) SchemaUtils.validateSameMember($SCHEMA_CONTINUATION_TOKEN, member, continuationToken);
            case 10 -> (T) SchemaUtils.validateSameMember($SCHEMA_NEXT_CONTINUATION_TOKEN, member, nextContinuationToken);
            case 11 -> (T) SchemaUtils.validateSameMember($SCHEMA_START_AFTER, member, startAfter);
            case 12 -> (T) SchemaUtils.validateSameMember($SCHEMA_REQUEST_CHARGED, member, requestCharged);
            default -> throw new IllegalArgumentException("Attempted to get non-existent member: " + member.id());
        };
    }

    /**
     * Create a new builder containing all the current property values of this object.
     *
     * <p><strong>Note:</strong> This method performs only a shallow copy of the original properties.
     *
     * @return a builder for {@link ListObjectsV2Output}.
     */
    public Builder toBuilder() {
        var builder = new Builder();
        builder.isTruncated(this.isTruncated);
        builder.contents(this.contents);
        builder.name(this.name);
        builder.prefix(this.prefix);
        builder.delimiter(this.delimiter);
        builder.maxKeys(this.maxKeys);
        builder.commonPrefixes(this.commonPrefixes);
        builder.encodingType(this.encodingType);
        builder.keyCount(this.keyCount);
        builder.continuationToken(this.continuationToken);
        builder.nextContinuationToken(this.nextContinuationToken);
        builder.startAfter(this.startAfter);
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
     * Builder for {@link ListObjectsV2Output}.
     */
    public static final class Builder implements ShapeBuilder<ListObjectsV2Output> {
        private Boolean isTruncated;
        private List<ObjectShape> contents;
        private String name;
        private String prefix;
        private String delimiter;
        private Integer maxKeys;
        private List<CommonPrefix> commonPrefixes;
        private EncodingType encodingType;
        private Integer keyCount;
        private String continuationToken;
        private String nextContinuationToken;
        private String startAfter;
        private RequestCharged requestCharged;

        private Builder() {}

        @Override
        public Schema schema() {
            return $SCHEMA;
        }

        /**
         * Set to <code>false</code> if all of the results were returned. Set to <code>true</code> if more keys are
         * available to return. If the number of results exceeds that specified by <code>MaxKeys</code>, all of the results
         * might not be returned.
         *
         * @return this builder.
         */
        public Builder isTruncated(Boolean isTruncated) {
            this.isTruncated = isTruncated;
            return this;
        }

        /**
         * Metadata about each object returned.
         *
         * @return this builder.
         */
        public Builder contents(List<ObjectShape> contents) {
            this.contents = contents;
            return this;
        }

        /**
         * The bucket name.
         *
         * @return this builder.
         */
        public Builder name(String name) {
            this.name = name;
            return this;
        }

        /**
         * Keys that begin with the indicated prefix.
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
         * Causes keys that contain the same string between the <code>prefix</code> and the first occurrence of the
         * delimiter to be rolled up into a single result element in the <code>CommonPrefixes</code> collection. These
         * rolled-up keys are not returned elsewhere in the response. Each rolled-up result counts as only one return
         * against the <code>MaxKeys</code> value.
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
         * All of the keys (up to 1,000) that share the same prefix are grouped together. When counting the total numbers of
         * returns by this API operation, this group of keys is considered as one item.
         *
         * <p>A response can contain <code>CommonPrefixes</code> only if you specify a delimiter.
         *
         * <p><code>CommonPrefixes</code> contains all (if there are any) keys between <code>Prefix</code> and the next
         * occurrence of the string specified by a delimiter.
         *
         * <p><code>CommonPrefixes</code> lists keys that act like subdirectories in the directory specified by <code>Prefix</code>
         * .
         *
         * <p>For example, if the prefix is <code>notes/</code> and the delimiter is a slash (<code>/</code>) as in <code>
         * notes/summer/july</code>, the common prefix is <code>notes/summer/</code>. All of the keys that roll up into a
         * common prefix count as a single return when calculating the number of returns.
         *
         * <ul>
         *   <li>
         *     <b>Directory buckets</b> - For directory buckets, only prefixes that end in a delimiter (<code>/</code>)
         *     are supported.
         *   </li>
         *   <li>
         *     <b>Directory buckets </b> - When you query <code>ListObjectsV2</code> with a delimiter during in-progress
         *     multipart uploads, the <code>CommonPrefixes</code> response parameter contains the prefixes that are
         *     associated with the in-progress multipart uploads. For more information about multipart uploads, see <a href="https://docs.aws.amazon.com/AmazonS3/latest/dev/mpuoverview.html">
         *     Multipart Upload Overview</a> in the <i>Amazon S3 User Guide</i>.
         *   </li>
         * </ul>
         *
         * @return this builder.
         */
        public Builder commonPrefixes(List<CommonPrefix> commonPrefixes) {
            this.commonPrefixes = commonPrefixes;
            return this;
        }

        /**
         * Encoding type used by Amazon S3 to encode object key names in the XML response.
         *
         * <p>If you specify the <code>encoding-type</code> request parameter, Amazon S3 includes this element in the
         * response, and returns encoded key name values in the following response elements:
         *
         * <p><code>Delimiter, Prefix, Key,</code> and <code>StartAfter</code>.
         *
         * @return this builder.
         */
        public Builder encodingType(EncodingType encodingType) {
            this.encodingType = encodingType;
            return this;
        }

        /**
         * <code>KeyCount</code> is the number of keys returned with this request. <code>KeyCount</code> will always be less
         * than or equal to the <code>MaxKeys</code> field. For example, if you ask for 50 keys, your result will include 50
         * keys or fewer.
         *
         * @return this builder.
         */
        public Builder keyCount(Integer keyCount) {
            this.keyCount = keyCount;
            return this;
        }

        /**
         * If <code>ContinuationToken</code> was sent with the request, it is included in the response. You can use the
         * returned <code>ContinuationToken</code> for pagination of the list response.
         *
         * @return this builder.
         */
        public Builder continuationToken(String continuationToken) {
            this.continuationToken = continuationToken;
            return this;
        }

        /**
         * <code>NextContinuationToken</code> is sent when <code>isTruncated</code> is true, which means there are more keys
         * in the bucket that can be listed. The next list requests to Amazon S3 can be continued with this
         * <code>NextContinuationToken</code>. <code>NextContinuationToken</code> is obfuscated and is not a real key
         *
         * @return this builder.
         */
        public Builder nextContinuationToken(String nextContinuationToken) {
            this.nextContinuationToken = nextContinuationToken;
            return this;
        }

        /**
         * If StartAfter was sent with the request, it is included in the response.
         *
         * <p>This functionality is not supported for directory buckets.
         *
         * @return this builder.
         */
        public Builder startAfter(String startAfter) {
            this.startAfter = startAfter;
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
        public ListObjectsV2Output build() {
            return new ListObjectsV2Output(this);
        }

        @Override
        @SuppressWarnings("unchecked")
        public void setMemberValue(Schema member, Object value) {
            switch (member.memberIndex()) {
                case 0 -> isTruncated((Boolean) SchemaUtils.validateSameMember($SCHEMA_IS_TRUNCATED, member, value));
                case 1 -> contents((List<ObjectShape>) SchemaUtils.validateSameMember($SCHEMA_CONTENTS, member, value));
                case 2 -> name((String) SchemaUtils.validateSameMember($SCHEMA_NAME, member, value));
                case 3 -> prefix((String) SchemaUtils.validateSameMember($SCHEMA_PREFIX, member, value));
                case 4 -> delimiter((String) SchemaUtils.validateSameMember($SCHEMA_DELIMITER, member, value));
                case 5 -> maxKeys((Integer) SchemaUtils.validateSameMember($SCHEMA_MAX_KEYS, member, value));
                case 6 -> commonPrefixes((List<CommonPrefix>) SchemaUtils.validateSameMember($SCHEMA_COMMON_PREFIXES, member, value));
                case 7 -> encodingType((EncodingType) SchemaUtils.validateSameMember($SCHEMA_ENCODING_TYPE, member, value));
                case 8 -> keyCount((Integer) SchemaUtils.validateSameMember($SCHEMA_KEY_COUNT, member, value));
                case 9 -> continuationToken((String) SchemaUtils.validateSameMember($SCHEMA_CONTINUATION_TOKEN, member, value));
                case 10 -> nextContinuationToken((String) SchemaUtils.validateSameMember($SCHEMA_NEXT_CONTINUATION_TOKEN, member, value));
                case 11 -> startAfter((String) SchemaUtils.validateSameMember($SCHEMA_START_AFTER, member, value));
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
                    case 0 -> builder.isTruncated(de.readBoolean(member));
                    case 1 -> builder.contents(SharedSerde.deserializeObjectList(member, de));
                    case 2 -> builder.name(de.readString(member));
                    case 3 -> builder.prefix(de.readString(member));
                    case 4 -> builder.delimiter(de.readString(member));
                    case 5 -> builder.maxKeys(de.readInteger(member));
                    case 6 -> builder.commonPrefixes(SharedSerde.deserializeCommonPrefixList(member, de));
                    case 7 -> builder.encodingType(EncodingType.builder().deserializeMember(de, member).build());
                    case 8 -> builder.keyCount(de.readInteger(member));
                    case 9 -> builder.continuationToken(de.readString(member));
                    case 10 -> builder.nextContinuationToken(de.readString(member));
                    case 11 -> builder.startAfter(de.readString(member));
                    case 12 -> builder.requestCharged(RequestCharged.builder().deserializeMember(de, member).build());
                    default -> throw new IllegalArgumentException("Unexpected member: " + member.memberName());
                }
            }
        }
    }
}
