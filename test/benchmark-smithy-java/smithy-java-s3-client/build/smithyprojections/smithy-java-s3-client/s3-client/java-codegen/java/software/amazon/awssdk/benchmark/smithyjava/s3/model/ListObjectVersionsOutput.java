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
public final class ListObjectVersionsOutput implements SerializableStruct {

    public static final Schema $SCHEMA = Schemas5.LIST_OBJECT_VERSIONS_OUTPUT;
    private static final Schema $SCHEMA_IS_TRUNCATED = $SCHEMA.member("IsTruncated");
    private static final Schema $SCHEMA_KEY_MARKER = $SCHEMA.member("KeyMarker");
    private static final Schema $SCHEMA_VERSION_ID_MARKER = $SCHEMA.member("VersionIdMarker");
    private static final Schema $SCHEMA_NEXT_KEY_MARKER = $SCHEMA.member("NextKeyMarker");
    private static final Schema $SCHEMA_NEXT_VERSION_ID_MARKER = $SCHEMA.member("NextVersionIdMarker");
    private static final Schema $SCHEMA_VERSIONS = $SCHEMA.member("Versions");
    private static final Schema $SCHEMA_DELETE_MARKERS = $SCHEMA.member("DeleteMarkers");
    private static final Schema $SCHEMA_NAME = $SCHEMA.member("Name");
    private static final Schema $SCHEMA_PREFIX = $SCHEMA.member("Prefix");
    private static final Schema $SCHEMA_DELIMITER = $SCHEMA.member("Delimiter");
    private static final Schema $SCHEMA_MAX_KEYS = $SCHEMA.member("MaxKeys");
    private static final Schema $SCHEMA_COMMON_PREFIXES = $SCHEMA.member("CommonPrefixes");
    private static final Schema $SCHEMA_ENCODING_TYPE = $SCHEMA.member("EncodingType");
    private static final Schema $SCHEMA_REQUEST_CHARGED = $SCHEMA.member("RequestCharged");

    public static final ShapeId $ID = $SCHEMA.id();

    private final transient Boolean isTruncated;
    private final transient String keyMarker;
    private final transient String versionIdMarker;
    private final transient String nextKeyMarker;
    private final transient String nextVersionIdMarker;
    private final transient List<ObjectVersion> versions;
    private final transient List<DeleteMarkerEntry> deleteMarkers;
    private final transient String name;
    private final transient String prefix;
    private final transient String delimiter;
    private final transient Integer maxKeys;
    private final transient List<CommonPrefix> commonPrefixes;
    private final transient EncodingType encodingType;
    private final transient RequestCharged requestCharged;

    private ListObjectVersionsOutput(Builder builder) {
        this.isTruncated = builder.isTruncated;
        this.keyMarker = builder.keyMarker;
        this.versionIdMarker = builder.versionIdMarker;
        this.nextKeyMarker = builder.nextKeyMarker;
        this.nextVersionIdMarker = builder.nextVersionIdMarker;
        this.versions = builder.versions == null ? null : Collections.unmodifiableList(builder.versions);
        this.deleteMarkers = builder.deleteMarkers == null ? null : Collections.unmodifiableList(builder.deleteMarkers);
        this.name = builder.name;
        this.prefix = builder.prefix;
        this.delimiter = builder.delimiter;
        this.maxKeys = builder.maxKeys;
        this.commonPrefixes = builder.commonPrefixes == null ? null : Collections.unmodifiableList(builder.commonPrefixes);
        this.encodingType = builder.encodingType;
        this.requestCharged = builder.requestCharged;
    }

    /**
     * A flag that indicates whether Amazon S3 returned all of the results that satisfied the search criteria. If your
     * results were truncated, you can make a follow-up paginated request by using the <code>NextKeyMarker</code> and
     * <code>NextVersionIdMarker</code> response parameters as a starting place in another request to return the rest of
     * the results.
     */
    public Boolean isIsTruncated() {
        return isTruncated;
    }

    /**
     * Marks the last key returned in a truncated response.
     */
    public String getKeyMarker() {
        return keyMarker;
    }

    /**
     * Marks the last version of the key returned in a truncated response.
     */
    public String getVersionIdMarker() {
        return versionIdMarker;
    }

    /**
     * When the number of responses exceeds the value of <code>MaxKeys</code>, <code>NextKeyMarker</code> specifies the
     * first key not returned that satisfies the search criteria. Use this value for the key-marker request parameter in
     * a subsequent request.
     */
    public String getNextKeyMarker() {
        return nextKeyMarker;
    }

    /**
     * When the number of responses exceeds the value of <code>MaxKeys</code>, <code>NextVersionIdMarker</code>
     * specifies the first object version not returned that satisfies the search criteria. Use this value for the
     * <code>version-id-marker</code> request parameter in a subsequent request.
     */
    public String getNextVersionIdMarker() {
        return nextVersionIdMarker;
    }

    /**
     * Container for version information.
     */
    public List<ObjectVersion> getVersions() {
        if (versions == null) {
            return Collections.emptyList();
        }
        return versions;
    }

    public boolean hasVersions() {
        return versions != null;
    }

    /**
     * Container for an object that is a delete marker. To learn more about delete markers, see <a
     * href="https://docs.aws.amazon.com/AmazonS3/latest/userguide/DeleteMarker.html">Working with delete markers</a>.
     */
    public List<DeleteMarkerEntry> getDeleteMarkers() {
        if (deleteMarkers == null) {
            return Collections.emptyList();
        }
        return deleteMarkers;
    }

    public boolean hasDeleteMarkers() {
        return deleteMarkers != null;
    }

    /**
     * The bucket name.
     */
    public String getName() {
        return name;
    }

    /**
     * Selects objects that start with the value supplied by this parameter.
     */
    public String getPrefix() {
        return prefix;
    }

    /**
     * The delimiter grouping the included keys. A delimiter is a character that you specify to group keys. All keys
     * that contain the same string between the prefix and the first occurrence of the delimiter are grouped under a
     * single result element in <code>CommonPrefixes</code>. These groups are counted as one result against the
     * <code>max-keys</code> limitation. These keys are not returned elsewhere in the response.
     */
    public String getDelimiter() {
        return delimiter;
    }

    /**
     * Specifies the maximum number of objects to return.
     */
    public Integer getMaxKeys() {
        return maxKeys;
    }

    /**
     * All of the keys rolled up into a common prefix count as a single return when calculating the number of returns.
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
     * <p><code>KeyMarker, NextKeyMarker, Prefix, Key</code>, and <code>Delimiter</code>.
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
        ListObjectVersionsOutput that = (ListObjectVersionsOutput) other;
        return Objects.equals(this.isTruncated, that.isTruncated)
               && Objects.equals(this.maxKeys, that.maxKeys)
               && Objects.equals(this.keyMarker, that.keyMarker)
               && Objects.equals(this.versionIdMarker, that.versionIdMarker)
               && Objects.equals(this.nextKeyMarker, that.nextKeyMarker)
               && Objects.equals(this.nextVersionIdMarker, that.nextVersionIdMarker)
               && Objects.equals(this.name, that.name)
               && Objects.equals(this.prefix, that.prefix)
               && Objects.equals(this.delimiter, that.delimiter)
               && Objects.equals(this.encodingType, that.encodingType)
               && Objects.equals(this.requestCharged, that.requestCharged)
               && Objects.equals(this.versions, that.versions)
               && Objects.equals(this.deleteMarkers, that.deleteMarkers)
               && Objects.equals(this.commonPrefixes, that.commonPrefixes);
    }

    @Override
    public int hashCode() {
        int $hc = Objects.hashCode(isTruncated);
        $hc = 31 * $hc + Objects.hashCode(keyMarker);
        $hc = 31 * $hc + Objects.hashCode(versionIdMarker);
        $hc = 31 * $hc + Objects.hashCode(nextKeyMarker);
        $hc = 31 * $hc + Objects.hashCode(nextVersionIdMarker);
        $hc = 31 * $hc + Objects.hashCode(versions);
        $hc = 31 * $hc + Objects.hashCode(deleteMarkers);
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
        if (keyMarker != null) {
            serializer.writeString($SCHEMA_KEY_MARKER, keyMarker);
        }
        if (versionIdMarker != null) {
            serializer.writeString($SCHEMA_VERSION_ID_MARKER, versionIdMarker);
        }
        if (nextKeyMarker != null) {
            serializer.writeString($SCHEMA_NEXT_KEY_MARKER, nextKeyMarker);
        }
        if (nextVersionIdMarker != null) {
            serializer.writeString($SCHEMA_NEXT_VERSION_ID_MARKER, nextVersionIdMarker);
        }
        if (versions != null) {
            serializer.writeList($SCHEMA_VERSIONS, versions, versions.size(), SharedSerde.ObjectVersionListSerializer.INSTANCE);
        }
        if (deleteMarkers != null) {
            serializer.writeList($SCHEMA_DELETE_MARKERS, deleteMarkers, deleteMarkers.size(), SharedSerde.DeleteMarkersSerializer.INSTANCE);
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
            case 1 -> (T) SchemaUtils.validateSameMember($SCHEMA_KEY_MARKER, member, keyMarker);
            case 2 -> (T) SchemaUtils.validateSameMember($SCHEMA_VERSION_ID_MARKER, member, versionIdMarker);
            case 3 -> (T) SchemaUtils.validateSameMember($SCHEMA_NEXT_KEY_MARKER, member, nextKeyMarker);
            case 4 -> (T) SchemaUtils.validateSameMember($SCHEMA_NEXT_VERSION_ID_MARKER, member, nextVersionIdMarker);
            case 5 -> (T) SchemaUtils.validateSameMember($SCHEMA_VERSIONS, member, versions);
            case 6 -> (T) SchemaUtils.validateSameMember($SCHEMA_DELETE_MARKERS, member, deleteMarkers);
            case 7 -> (T) SchemaUtils.validateSameMember($SCHEMA_NAME, member, name);
            case 8 -> (T) SchemaUtils.validateSameMember($SCHEMA_PREFIX, member, prefix);
            case 9 -> (T) SchemaUtils.validateSameMember($SCHEMA_DELIMITER, member, delimiter);
            case 10 -> (T) SchemaUtils.validateSameMember($SCHEMA_MAX_KEYS, member, maxKeys);
            case 11 -> (T) SchemaUtils.validateSameMember($SCHEMA_COMMON_PREFIXES, member, commonPrefixes);
            case 12 -> (T) SchemaUtils.validateSameMember($SCHEMA_ENCODING_TYPE, member, encodingType);
            case 13 -> (T) SchemaUtils.validateSameMember($SCHEMA_REQUEST_CHARGED, member, requestCharged);
            default -> throw new IllegalArgumentException("Attempted to get non-existent member: " + member.id());
        };
    }

    /**
     * Create a new builder containing all the current property values of this object.
     *
     * <p><strong>Note:</strong> This method performs only a shallow copy of the original properties.
     *
     * @return a builder for {@link ListObjectVersionsOutput}.
     */
    public Builder toBuilder() {
        var builder = new Builder();
        builder.isTruncated(this.isTruncated);
        builder.keyMarker(this.keyMarker);
        builder.versionIdMarker(this.versionIdMarker);
        builder.nextKeyMarker(this.nextKeyMarker);
        builder.nextVersionIdMarker(this.nextVersionIdMarker);
        builder.versions(this.versions);
        builder.deleteMarkers(this.deleteMarkers);
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
     * Builder for {@link ListObjectVersionsOutput}.
     */
    public static final class Builder implements ShapeBuilder<ListObjectVersionsOutput> {
        private Boolean isTruncated;
        private String keyMarker;
        private String versionIdMarker;
        private String nextKeyMarker;
        private String nextVersionIdMarker;
        private List<ObjectVersion> versions;
        private List<DeleteMarkerEntry> deleteMarkers;
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
         * A flag that indicates whether Amazon S3 returned all of the results that satisfied the search criteria. If your
         * results were truncated, you can make a follow-up paginated request by using the <code>NextKeyMarker</code> and
         * <code>NextVersionIdMarker</code> response parameters as a starting place in another request to return the rest of
         * the results.
         *
         * @return this builder.
         */
        public Builder isTruncated(Boolean isTruncated) {
            this.isTruncated = isTruncated;
            return this;
        }

        /**
         * Marks the last key returned in a truncated response.
         *
         * @return this builder.
         */
        public Builder keyMarker(String keyMarker) {
            this.keyMarker = keyMarker;
            return this;
        }

        /**
         * Marks the last version of the key returned in a truncated response.
         *
         * @return this builder.
         */
        public Builder versionIdMarker(String versionIdMarker) {
            this.versionIdMarker = versionIdMarker;
            return this;
        }

        /**
         * When the number of responses exceeds the value of <code>MaxKeys</code>, <code>NextKeyMarker</code> specifies the
         * first key not returned that satisfies the search criteria. Use this value for the key-marker request parameter in
         * a subsequent request.
         *
         * @return this builder.
         */
        public Builder nextKeyMarker(String nextKeyMarker) {
            this.nextKeyMarker = nextKeyMarker;
            return this;
        }

        /**
         * When the number of responses exceeds the value of <code>MaxKeys</code>, <code>NextVersionIdMarker</code>
         * specifies the first object version not returned that satisfies the search criteria. Use this value for the
         * <code>version-id-marker</code> request parameter in a subsequent request.
         *
         * @return this builder.
         */
        public Builder nextVersionIdMarker(String nextVersionIdMarker) {
            this.nextVersionIdMarker = nextVersionIdMarker;
            return this;
        }

        /**
         * Container for version information.
         *
         * @return this builder.
         */
        public Builder versions(List<ObjectVersion> versions) {
            this.versions = versions;
            return this;
        }

        /**
         * Container for an object that is a delete marker. To learn more about delete markers, see <a
         * href="https://docs.aws.amazon.com/AmazonS3/latest/userguide/DeleteMarker.html">Working with delete markers</a>.
         *
         * @return this builder.
         */
        public Builder deleteMarkers(List<DeleteMarkerEntry> deleteMarkers) {
            this.deleteMarkers = deleteMarkers;
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
         * Selects objects that start with the value supplied by this parameter.
         *
         * @return this builder.
         */
        public Builder prefix(String prefix) {
            this.prefix = prefix;
            return this;
        }

        /**
         * The delimiter grouping the included keys. A delimiter is a character that you specify to group keys. All keys
         * that contain the same string between the prefix and the first occurrence of the delimiter are grouped under a
         * single result element in <code>CommonPrefixes</code>. These groups are counted as one result against the
         * <code>max-keys</code> limitation. These keys are not returned elsewhere in the response.
         *
         * @return this builder.
         */
        public Builder delimiter(String delimiter) {
            this.delimiter = delimiter;
            return this;
        }

        /**
         * Specifies the maximum number of objects to return.
         *
         * @return this builder.
         */
        public Builder maxKeys(Integer maxKeys) {
            this.maxKeys = maxKeys;
            return this;
        }

        /**
         * All of the keys rolled up into a common prefix count as a single return when calculating the number of returns.
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
         * <p><code>KeyMarker, NextKeyMarker, Prefix, Key</code>, and <code>Delimiter</code>.
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
        public ListObjectVersionsOutput build() {
            return new ListObjectVersionsOutput(this);
        }

        @Override
        @SuppressWarnings("unchecked")
        public void setMemberValue(Schema member, Object value) {
            switch (member.memberIndex()) {
                case 0 -> isTruncated((Boolean) SchemaUtils.validateSameMember($SCHEMA_IS_TRUNCATED, member, value));
                case 1 -> keyMarker((String) SchemaUtils.validateSameMember($SCHEMA_KEY_MARKER, member, value));
                case 2 -> versionIdMarker((String) SchemaUtils.validateSameMember($SCHEMA_VERSION_ID_MARKER, member, value));
                case 3 -> nextKeyMarker((String) SchemaUtils.validateSameMember($SCHEMA_NEXT_KEY_MARKER, member, value));
                case 4 -> nextVersionIdMarker((String) SchemaUtils.validateSameMember($SCHEMA_NEXT_VERSION_ID_MARKER, member, value));
                case 5 -> versions((List<ObjectVersion>) SchemaUtils.validateSameMember($SCHEMA_VERSIONS, member, value));
                case 6 -> deleteMarkers((List<DeleteMarkerEntry>) SchemaUtils.validateSameMember($SCHEMA_DELETE_MARKERS, member, value));
                case 7 -> name((String) SchemaUtils.validateSameMember($SCHEMA_NAME, member, value));
                case 8 -> prefix((String) SchemaUtils.validateSameMember($SCHEMA_PREFIX, member, value));
                case 9 -> delimiter((String) SchemaUtils.validateSameMember($SCHEMA_DELIMITER, member, value));
                case 10 -> maxKeys((Integer) SchemaUtils.validateSameMember($SCHEMA_MAX_KEYS, member, value));
                case 11 -> commonPrefixes((List<CommonPrefix>) SchemaUtils.validateSameMember($SCHEMA_COMMON_PREFIXES, member, value));
                case 12 -> encodingType((EncodingType) SchemaUtils.validateSameMember($SCHEMA_ENCODING_TYPE, member, value));
                case 13 -> requestCharged((RequestCharged) SchemaUtils.validateSameMember($SCHEMA_REQUEST_CHARGED, member, value));
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
                    case 1 -> builder.keyMarker(de.readString(member));
                    case 2 -> builder.versionIdMarker(de.readString(member));
                    case 3 -> builder.nextKeyMarker(de.readString(member));
                    case 4 -> builder.nextVersionIdMarker(de.readString(member));
                    case 5 -> builder.versions(SharedSerde.deserializeObjectVersionList(member, de));
                    case 6 -> builder.deleteMarkers(SharedSerde.deserializeDeleteMarkers(member, de));
                    case 7 -> builder.name(de.readString(member));
                    case 8 -> builder.prefix(de.readString(member));
                    case 9 -> builder.delimiter(de.readString(member));
                    case 10 -> builder.maxKeys(de.readInteger(member));
                    case 11 -> builder.commonPrefixes(SharedSerde.deserializeCommonPrefixList(member, de));
                    case 12 -> builder.encodingType(EncodingType.builder().deserializeMember(de, member).build());
                    case 13 -> builder.requestCharged(RequestCharged.builder().deserializeMember(de, member).build());
                    default -> throw new IllegalArgumentException("Unexpected member: " + member.memberName());
                }
            }
        }
    }
}
