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
public final class ListObjectsOutput implements SerializableStruct {

    public static final Schema $SCHEMA = Schemas4.LIST_OBJECTS_OUTPUT;
    private static final Schema $SCHEMA_IS_TRUNCATED = $SCHEMA.member("IsTruncated");
    private static final Schema $SCHEMA_MARKER = $SCHEMA.member("Marker");
    private static final Schema $SCHEMA_NEXT_MARKER = $SCHEMA.member("NextMarker");
    private static final Schema $SCHEMA_CONTENTS = $SCHEMA.member("Contents");
    private static final Schema $SCHEMA_NAME = $SCHEMA.member("Name");
    private static final Schema $SCHEMA_PREFIX = $SCHEMA.member("Prefix");
    private static final Schema $SCHEMA_DELIMITER = $SCHEMA.member("Delimiter");
    private static final Schema $SCHEMA_MAX_KEYS = $SCHEMA.member("MaxKeys");
    private static final Schema $SCHEMA_COMMON_PREFIXES = $SCHEMA.member("CommonPrefixes");
    private static final Schema $SCHEMA_ENCODING_TYPE = $SCHEMA.member("EncodingType");
    private static final Schema $SCHEMA_REQUEST_CHARGED = $SCHEMA.member("RequestCharged");

    public static final ShapeId $ID = $SCHEMA.id();

    private final transient Boolean isTruncated;
    private final transient String marker;
    private final transient String nextMarker;
    private final transient List<ObjectShape> contents;
    private final transient String name;
    private final transient String prefix;
    private final transient String delimiter;
    private final transient Integer maxKeys;
    private final transient List<CommonPrefix> commonPrefixes;
    private final transient EncodingType encodingType;
    private final transient RequestCharged requestCharged;

    private ListObjectsOutput(Builder builder) {
        this.isTruncated = builder.isTruncated;
        this.marker = builder.marker;
        this.nextMarker = builder.nextMarker;
        this.contents = builder.contents == null ? null : Collections.unmodifiableList(builder.contents);
        this.name = builder.name;
        this.prefix = builder.prefix;
        this.delimiter = builder.delimiter;
        this.maxKeys = builder.maxKeys;
        this.commonPrefixes = builder.commonPrefixes == null ? null : Collections.unmodifiableList(builder.commonPrefixes);
        this.encodingType = builder.encodingType;
        this.requestCharged = builder.requestCharged;
    }

    /**
     * A flag that indicates whether Amazon S3 returned all of the results that satisfied the search criteria.
     */
    public Boolean isIsTruncated() {
        return isTruncated;
    }

    /**
     * Indicates where in the bucket listing begins. Marker is included in the response if it was sent with the request.
     */
    public String getMarker() {
        return marker;
    }

    /**
     * When the response is truncated (the <code>IsTruncated</code> element value in the response is <code>true</code>),
     * you can use the key name in this field as the <code>marker</code> parameter in the subsequent request to get the
     * next set of objects. Amazon S3 lists objects in alphabetical order.
     *
     * <p>This element is returned only if you have the <code>delimiter</code> request parameter specified. If the
     * response does not include the <code>NextMarker</code> element and it is truncated, you can use the value of the
     * last <code>Key</code> element in the response as the <code>marker</code> parameter in the subsequent request to
     * get the next set of object keys.
     */
    public String getNextMarker() {
        return nextMarker;
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
     */
    public String getPrefix() {
        return prefix;
    }

    /**
     * Causes keys that contain the same string between the prefix and the first occurrence of the delimiter to be
     * rolled up into a single result element in the <code>CommonPrefixes</code> collection. These rolled-up keys are
     * not returned elsewhere in the response. Each rolled-up result counts as only one return against the
     * <code>MaxKeys</code> value.
     */
    public String getDelimiter() {
        return delimiter;
    }

    /**
     * The maximum number of keys returned in the response body.
     */
    public Integer getMaxKeys() {
        return maxKeys;
    }

    /**
     * All of the keys (up to 1,000) rolled up in a common prefix count as a single return when calculating the number
     * of returns.
     *
     * <p>A response can contain <code>CommonPrefixes</code> only if you specify a delimiter.
     *
     * <p><code>CommonPrefixes</code> contains all (if there are any) keys between <code>Prefix</code> and the next
     * occurrence of the string specified by the delimiter.
     *
     * <p><code>CommonPrefixes</code> lists keys that act like subdirectories in the directory specified by <code>Prefix</code>
     * .
     *
     * <p>For example, if the prefix is <code>notes/</code> and the delimiter is a slash (<code>/</code>), as in <code>
     * notes/summer/july</code>, the common prefix is <code>notes/summer/</code>. All of the keys that roll up into a
     * common prefix count as a single return when calculating the number of returns.
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
     * Encoding type used by Amazon S3 to encode the <a href="https://docs.aws.amazon.com/AmazonS3/latest/userguide/object-keys.html">object keys</a> in the response. Responses are encoded only in
     * UTF-8. An object key can contain any Unicode character. However, the XML 1.0 parser can't parse certain
     * characters, such as characters with an ASCII value from 0 to 10. For characters that aren't supported in XML 1.0,
     * you can add this parameter to request that Amazon S3 encode the keys in the response. For more information about
     * characters to avoid in object key names, see <a href="https://docs.aws.amazon.com/AmazonS3/latest/userguide/object-keys.html#object-key-guidelines">Object key naming guidelines</a>.
     *
     * <p>When using the URL encoding type, non-ASCII characters that are used in an object's key name will be
     * percent-encoded according to UTF-8 code values. For example, the object <code>test_file(3).png</code> will appear
     * as <code>test_file%283%29.png</code>.
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
        ListObjectsOutput that = (ListObjectsOutput) other;
        return Objects.equals(this.isTruncated, that.isTruncated)
               && Objects.equals(this.maxKeys, that.maxKeys)
               && Objects.equals(this.marker, that.marker)
               && Objects.equals(this.nextMarker, that.nextMarker)
               && Objects.equals(this.name, that.name)
               && Objects.equals(this.prefix, that.prefix)
               && Objects.equals(this.delimiter, that.delimiter)
               && Objects.equals(this.encodingType, that.encodingType)
               && Objects.equals(this.requestCharged, that.requestCharged)
               && Objects.equals(this.contents, that.contents)
               && Objects.equals(this.commonPrefixes, that.commonPrefixes);
    }

    @Override
    public int hashCode() {
        int $hc = Objects.hashCode(isTruncated);
        $hc = 31 * $hc + Objects.hashCode(marker);
        $hc = 31 * $hc + Objects.hashCode(nextMarker);
        $hc = 31 * $hc + Objects.hashCode(contents);
        $hc = 31 * $hc + Objects.hashCode(name);
        $hc = 31 * $hc + Objects.hashCode(prefix);
        $hc = 31 * $hc + Objects.hashCode(delimiter);
        $hc = 31 * $hc + Objects.hashCode(maxKeys);
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
        if (isTruncated != null) {
            serializer.writeBoolean($SCHEMA_IS_TRUNCATED, isTruncated);
        }
        if (marker != null) {
            serializer.writeString($SCHEMA_MARKER, marker);
        }
        if (nextMarker != null) {
            serializer.writeString($SCHEMA_NEXT_MARKER, nextMarker);
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
        if (requestCharged != null) {
            serializer.writeString($SCHEMA_REQUEST_CHARGED, requestCharged.getValue());
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T getMemberValue(Schema member) {
        return switch (member.memberIndex()) {
            case 0 -> (T) SchemaUtils.validateSameMember($SCHEMA_IS_TRUNCATED, member, isTruncated);
            case 1 -> (T) SchemaUtils.validateSameMember($SCHEMA_MARKER, member, marker);
            case 2 -> (T) SchemaUtils.validateSameMember($SCHEMA_NEXT_MARKER, member, nextMarker);
            case 3 -> (T) SchemaUtils.validateSameMember($SCHEMA_CONTENTS, member, contents);
            case 4 -> (T) SchemaUtils.validateSameMember($SCHEMA_NAME, member, name);
            case 5 -> (T) SchemaUtils.validateSameMember($SCHEMA_PREFIX, member, prefix);
            case 6 -> (T) SchemaUtils.validateSameMember($SCHEMA_DELIMITER, member, delimiter);
            case 7 -> (T) SchemaUtils.validateSameMember($SCHEMA_MAX_KEYS, member, maxKeys);
            case 8 -> (T) SchemaUtils.validateSameMember($SCHEMA_COMMON_PREFIXES, member, commonPrefixes);
            case 9 -> (T) SchemaUtils.validateSameMember($SCHEMA_ENCODING_TYPE, member, encodingType);
            case 10 -> (T) SchemaUtils.validateSameMember($SCHEMA_REQUEST_CHARGED, member, requestCharged);
            default -> throw new IllegalArgumentException("Attempted to get non-existent member: " + member.id());
        };
    }

    /**
     * Create a new builder containing all the current property values of this object.
     *
     * <p><strong>Note:</strong> This method performs only a shallow copy of the original properties.
     *
     * @return a builder for {@link ListObjectsOutput}.
     */
    public Builder toBuilder() {
        var builder = new Builder();
        builder.isTruncated(this.isTruncated);
        builder.marker(this.marker);
        builder.nextMarker(this.nextMarker);
        builder.contents(this.contents);
        builder.name(this.name);
        builder.prefix(this.prefix);
        builder.delimiter(this.delimiter);
        builder.maxKeys(this.maxKeys);
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
     * Builder for {@link ListObjectsOutput}.
     */
    public static final class Builder implements ShapeBuilder<ListObjectsOutput> {
        private Boolean isTruncated;
        private String marker;
        private String nextMarker;
        private List<ObjectShape> contents;
        private String name;
        private String prefix;
        private String delimiter;
        private Integer maxKeys;
        private List<CommonPrefix> commonPrefixes;
        private EncodingType encodingType;
        private RequestCharged requestCharged;

        private Builder() {}

        @Override
        public Schema schema() {
            return $SCHEMA;
        }

        /**
         * A flag that indicates whether Amazon S3 returned all of the results that satisfied the search criteria.
         *
         * @return this builder.
         */
        public Builder isTruncated(Boolean isTruncated) {
            this.isTruncated = isTruncated;
            return this;
        }

        /**
         * Indicates where in the bucket listing begins. Marker is included in the response if it was sent with the request.
         *
         * @return this builder.
         */
        public Builder marker(String marker) {
            this.marker = marker;
            return this;
        }

        /**
         * When the response is truncated (the <code>IsTruncated</code> element value in the response is <code>true</code>),
         * you can use the key name in this field as the <code>marker</code> parameter in the subsequent request to get the
         * next set of objects. Amazon S3 lists objects in alphabetical order.
         *
         * <p>This element is returned only if you have the <code>delimiter</code> request parameter specified. If the
         * response does not include the <code>NextMarker</code> element and it is truncated, you can use the value of the
         * last <code>Key</code> element in the response as the <code>marker</code> parameter in the subsequent request to
         * get the next set of object keys.
         *
         * @return this builder.
         */
        public Builder nextMarker(String nextMarker) {
            this.nextMarker = nextMarker;
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
         * @return this builder.
         */
        public Builder prefix(String prefix) {
            this.prefix = prefix;
            return this;
        }

        /**
         * Causes keys that contain the same string between the prefix and the first occurrence of the delimiter to be
         * rolled up into a single result element in the <code>CommonPrefixes</code> collection. These rolled-up keys are
         * not returned elsewhere in the response. Each rolled-up result counts as only one return against the
         * <code>MaxKeys</code> value.
         *
         * @return this builder.
         */
        public Builder delimiter(String delimiter) {
            this.delimiter = delimiter;
            return this;
        }

        /**
         * The maximum number of keys returned in the response body.
         *
         * @return this builder.
         */
        public Builder maxKeys(Integer maxKeys) {
            this.maxKeys = maxKeys;
            return this;
        }

        /**
         * All of the keys (up to 1,000) rolled up in a common prefix count as a single return when calculating the number
         * of returns.
         *
         * <p>A response can contain <code>CommonPrefixes</code> only if you specify a delimiter.
         *
         * <p><code>CommonPrefixes</code> contains all (if there are any) keys between <code>Prefix</code> and the next
         * occurrence of the string specified by the delimiter.
         *
         * <p><code>CommonPrefixes</code> lists keys that act like subdirectories in the directory specified by <code>Prefix</code>
         * .
         *
         * <p>For example, if the prefix is <code>notes/</code> and the delimiter is a slash (<code>/</code>), as in <code>
         * notes/summer/july</code>, the common prefix is <code>notes/summer/</code>. All of the keys that roll up into a
         * common prefix count as a single return when calculating the number of returns.
         *
         * @return this builder.
         */
        public Builder commonPrefixes(List<CommonPrefix> commonPrefixes) {
            this.commonPrefixes = commonPrefixes;
            return this;
        }

        /**
         * Encoding type used by Amazon S3 to encode the <a href="https://docs.aws.amazon.com/AmazonS3/latest/userguide/object-keys.html">object keys</a> in the response. Responses are encoded only in
         * UTF-8. An object key can contain any Unicode character. However, the XML 1.0 parser can't parse certain
         * characters, such as characters with an ASCII value from 0 to 10. For characters that aren't supported in XML 1.0,
         * you can add this parameter to request that Amazon S3 encode the keys in the response. For more information about
         * characters to avoid in object key names, see <a href="https://docs.aws.amazon.com/AmazonS3/latest/userguide/object-keys.html#object-key-guidelines">Object key naming guidelines</a>.
         *
         * <p>When using the URL encoding type, non-ASCII characters that are used in an object's key name will be
         * percent-encoded according to UTF-8 code values. For example, the object <code>test_file(3).png</code> will appear
         * as <code>test_file%283%29.png</code>.
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
        public ListObjectsOutput build() {
            return new ListObjectsOutput(this);
        }

        @Override
        @SuppressWarnings("unchecked")
        public void setMemberValue(Schema member, Object value) {
            switch (member.memberIndex()) {
                case 0 -> isTruncated((Boolean) SchemaUtils.validateSameMember($SCHEMA_IS_TRUNCATED, member, value));
                case 1 -> marker((String) SchemaUtils.validateSameMember($SCHEMA_MARKER, member, value));
                case 2 -> nextMarker((String) SchemaUtils.validateSameMember($SCHEMA_NEXT_MARKER, member, value));
                case 3 -> contents((List<ObjectShape>) SchemaUtils.validateSameMember($SCHEMA_CONTENTS, member, value));
                case 4 -> name((String) SchemaUtils.validateSameMember($SCHEMA_NAME, member, value));
                case 5 -> prefix((String) SchemaUtils.validateSameMember($SCHEMA_PREFIX, member, value));
                case 6 -> delimiter((String) SchemaUtils.validateSameMember($SCHEMA_DELIMITER, member, value));
                case 7 -> maxKeys((Integer) SchemaUtils.validateSameMember($SCHEMA_MAX_KEYS, member, value));
                case 8 -> commonPrefixes((List<CommonPrefix>) SchemaUtils.validateSameMember($SCHEMA_COMMON_PREFIXES, member, value));
                case 9 -> encodingType((EncodingType) SchemaUtils.validateSameMember($SCHEMA_ENCODING_TYPE, member, value));
                case 10 -> requestCharged((RequestCharged) SchemaUtils.validateSameMember($SCHEMA_REQUEST_CHARGED, member, value));
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
                    case 1 -> builder.marker(de.readString(member));
                    case 2 -> builder.nextMarker(de.readString(member));
                    case 3 -> builder.contents(SharedSerde.deserializeObjectList(member, de));
                    case 4 -> builder.name(de.readString(member));
                    case 5 -> builder.prefix(de.readString(member));
                    case 6 -> builder.delimiter(de.readString(member));
                    case 7 -> builder.maxKeys(de.readInteger(member));
                    case 8 -> builder.commonPrefixes(SharedSerde.deserializeCommonPrefixList(member, de));
                    case 9 -> builder.encodingType(EncodingType.builder().deserializeMember(de, member).build());
                    case 10 -> builder.requestCharged(RequestCharged.builder().deserializeMember(de, member).build());
                    default -> throw new IllegalArgumentException("Unexpected member: " + member.memberName());
                }
            }
        }
    }
}
