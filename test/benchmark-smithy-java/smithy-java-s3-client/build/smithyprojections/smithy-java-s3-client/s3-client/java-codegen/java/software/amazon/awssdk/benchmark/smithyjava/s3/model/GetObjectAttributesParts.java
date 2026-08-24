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

/**
 * A collection of parts associated with a multipart upload.
 */
@SmithyGenerated
public final class GetObjectAttributesParts implements SerializableStruct {

    public static final Schema $SCHEMA = Schemas3.GET_OBJECT_ATTRIBUTES_PARTS;
    private static final Schema $SCHEMA_TOTAL_PARTS_COUNT = $SCHEMA.member("TotalPartsCount");
    private static final Schema $SCHEMA_PART_NUMBER_MARKER = $SCHEMA.member("PartNumberMarker");
    private static final Schema $SCHEMA_NEXT_PART_NUMBER_MARKER = $SCHEMA.member("NextPartNumberMarker");
    private static final Schema $SCHEMA_MAX_PARTS = $SCHEMA.member("MaxParts");
    private static final Schema $SCHEMA_IS_TRUNCATED = $SCHEMA.member("IsTruncated");
    private static final Schema $SCHEMA_PARTS = $SCHEMA.member("Parts");

    public static final ShapeId $ID = $SCHEMA.id();

    private final transient Integer totalPartsCount;
    private final transient String partNumberMarker;
    private final transient String nextPartNumberMarker;
    private final transient Integer maxParts;
    private final transient Boolean isTruncated;
    private final transient List<ObjectPart> parts;

    private GetObjectAttributesParts(Builder builder) {
        this.totalPartsCount = builder.totalPartsCount;
        this.partNumberMarker = builder.partNumberMarker;
        this.nextPartNumberMarker = builder.nextPartNumberMarker;
        this.maxParts = builder.maxParts;
        this.isTruncated = builder.isTruncated;
        this.parts = builder.parts == null ? null : Collections.unmodifiableList(builder.parts);
    }

    /**
     * The total number of parts.
     */
    public Integer getTotalPartsCount() {
        return totalPartsCount;
    }

    /**
     * The marker for the current part.
     */
    public String getPartNumberMarker() {
        return partNumberMarker;
    }

    /**
     * When a list is truncated, this element specifies the last part in the list, as well as the value to use for the
     * <code>PartNumberMarker</code> request parameter in a subsequent request.
     */
    public String getNextPartNumberMarker() {
        return nextPartNumberMarker;
    }

    /**
     * The maximum number of parts allowed in the response.
     */
    public Integer getMaxParts() {
        return maxParts;
    }

    /**
     * Indicates whether the returned list of parts is truncated. A value of <code>true</code> indicates that the list
     * was truncated. A list can be truncated if the number of parts exceeds the limit returned in the
     * <code>MaxParts</code> element.
     */
    public Boolean isIsTruncated() {
        return isTruncated;
    }

    /**
     * A container for elements related to a particular part. A response can contain zero or more <code>Parts</code>
     * elements.
     *
     * <ul>
     *   <li>
     *     <b>General purpose buckets</b> - For <code>GetObjectAttributes</code>, if an additional checksum
     *     (including <code>x-amz-checksum-crc32</code>, <code>x-amz-checksum-crc32c</code>, <code>
     *     x-amz-checksum-sha1</code>, or <code>x-amz-checksum-sha256</code>) isn't applied to the object specified
     *     in the request, the response doesn't return the <code>Part</code> element.
     *   </li>
     *   <li>
     *     <b>Directory buckets</b> - For <code>GetObjectAttributes</code>, regardless of whether an additional
     *     checksum is applied to the object specified in the request, the response returns the <code>Part</code>
     *     element.
     *   </li>
     * </ul>
     */
    public List<ObjectPart> getParts() {
        if (parts == null) {
            return Collections.emptyList();
        }
        return parts;
    }

    public boolean hasParts() {
        return parts != null;
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
        GetObjectAttributesParts that = (GetObjectAttributesParts) other;
        return Objects.equals(this.isTruncated, that.isTruncated)
               && Objects.equals(this.totalPartsCount, that.totalPartsCount)
               && Objects.equals(this.maxParts, that.maxParts)
               && Objects.equals(this.partNumberMarker, that.partNumberMarker)
               && Objects.equals(this.nextPartNumberMarker, that.nextPartNumberMarker)
               && Objects.equals(this.parts, that.parts);
    }

    @Override
    public int hashCode() {
        int $hc = Objects.hashCode(totalPartsCount);
        $hc = 31 * $hc + Objects.hashCode(partNumberMarker);
        $hc = 31 * $hc + Objects.hashCode(nextPartNumberMarker);
        $hc = 31 * $hc + Objects.hashCode(maxParts);
        $hc = 31 * $hc + Objects.hashCode(isTruncated);
        $hc = 31 * $hc + Objects.hashCode(parts);
        return $hc;
    }

    @Override
    public Schema schema() {
        return $SCHEMA;
    }

    @Override
    public void serializeMembers(ShapeSerializer serializer) {
        if (totalPartsCount != null) {
            serializer.writeInteger($SCHEMA_TOTAL_PARTS_COUNT, totalPartsCount);
        }
        if (partNumberMarker != null) {
            serializer.writeString($SCHEMA_PART_NUMBER_MARKER, partNumberMarker);
        }
        if (nextPartNumberMarker != null) {
            serializer.writeString($SCHEMA_NEXT_PART_NUMBER_MARKER, nextPartNumberMarker);
        }
        if (maxParts != null) {
            serializer.writeInteger($SCHEMA_MAX_PARTS, maxParts);
        }
        if (isTruncated != null) {
            serializer.writeBoolean($SCHEMA_IS_TRUNCATED, isTruncated);
        }
        if (parts != null) {
            serializer.writeList($SCHEMA_PARTS, parts, parts.size(), SharedSerde.PartsListSerializer.INSTANCE);
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T getMemberValue(Schema member) {
        return switch (member.memberIndex()) {
            case 0 -> (T) SchemaUtils.validateSameMember($SCHEMA_TOTAL_PARTS_COUNT, member, totalPartsCount);
            case 1 -> (T) SchemaUtils.validateSameMember($SCHEMA_PART_NUMBER_MARKER, member, partNumberMarker);
            case 2 -> (T) SchemaUtils.validateSameMember($SCHEMA_NEXT_PART_NUMBER_MARKER, member, nextPartNumberMarker);
            case 3 -> (T) SchemaUtils.validateSameMember($SCHEMA_MAX_PARTS, member, maxParts);
            case 4 -> (T) SchemaUtils.validateSameMember($SCHEMA_IS_TRUNCATED, member, isTruncated);
            case 5 -> (T) SchemaUtils.validateSameMember($SCHEMA_PARTS, member, parts);
            default -> throw new IllegalArgumentException("Attempted to get non-existent member: " + member.id());
        };
    }

    /**
     * Create a new builder containing all the current property values of this object.
     *
     * <p><strong>Note:</strong> This method performs only a shallow copy of the original properties.
     *
     * @return a builder for {@link GetObjectAttributesParts}.
     */
    public Builder toBuilder() {
        var builder = new Builder();
        builder.totalPartsCount(this.totalPartsCount);
        builder.partNumberMarker(this.partNumberMarker);
        builder.nextPartNumberMarker(this.nextPartNumberMarker);
        builder.maxParts(this.maxParts);
        builder.isTruncated(this.isTruncated);
        builder.parts(this.parts);
        return builder;
    }

    /**
     * @return returns a new Builder.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for {@link GetObjectAttributesParts}.
     */
    public static final class Builder implements ShapeBuilder<GetObjectAttributesParts> {
        private Integer totalPartsCount;
        private String partNumberMarker;
        private String nextPartNumberMarker;
        private Integer maxParts;
        private Boolean isTruncated;
        private List<ObjectPart> parts;

        private Builder() {}

        @Override
        public Schema schema() {
            return $SCHEMA;
        }

        /**
         * The total number of parts.
         *
         * @return this builder.
         */
        public Builder totalPartsCount(Integer totalPartsCount) {
            this.totalPartsCount = totalPartsCount;
            return this;
        }

        /**
         * The marker for the current part.
         *
         * @return this builder.
         */
        public Builder partNumberMarker(String partNumberMarker) {
            this.partNumberMarker = partNumberMarker;
            return this;
        }

        /**
         * When a list is truncated, this element specifies the last part in the list, as well as the value to use for the
         * <code>PartNumberMarker</code> request parameter in a subsequent request.
         *
         * @return this builder.
         */
        public Builder nextPartNumberMarker(String nextPartNumberMarker) {
            this.nextPartNumberMarker = nextPartNumberMarker;
            return this;
        }

        /**
         * The maximum number of parts allowed in the response.
         *
         * @return this builder.
         */
        public Builder maxParts(Integer maxParts) {
            this.maxParts = maxParts;
            return this;
        }

        /**
         * Indicates whether the returned list of parts is truncated. A value of <code>true</code> indicates that the list
         * was truncated. A list can be truncated if the number of parts exceeds the limit returned in the
         * <code>MaxParts</code> element.
         *
         * @return this builder.
         */
        public Builder isTruncated(Boolean isTruncated) {
            this.isTruncated = isTruncated;
            return this;
        }

        /**
         * A container for elements related to a particular part. A response can contain zero or more <code>Parts</code>
         * elements.
         *
         * <ul>
         *   <li>
         *     <b>General purpose buckets</b> - For <code>GetObjectAttributes</code>, if an additional checksum
         *     (including <code>x-amz-checksum-crc32</code>, <code>x-amz-checksum-crc32c</code>, <code>
         *     x-amz-checksum-sha1</code>, or <code>x-amz-checksum-sha256</code>) isn't applied to the object specified
         *     in the request, the response doesn't return the <code>Part</code> element.
         *   </li>
         *   <li>
         *     <b>Directory buckets</b> - For <code>GetObjectAttributes</code>, regardless of whether an additional
         *     checksum is applied to the object specified in the request, the response returns the <code>Part</code>
         *     element.
         *   </li>
         * </ul>
         *
         * @return this builder.
         */
        public Builder parts(List<ObjectPart> parts) {
            this.parts = parts;
            return this;
        }

        @Override
        public GetObjectAttributesParts build() {
            return new GetObjectAttributesParts(this);
        }

        @Override
        @SuppressWarnings("unchecked")
        public void setMemberValue(Schema member, Object value) {
            switch (member.memberIndex()) {
                case 0 -> totalPartsCount((Integer) SchemaUtils.validateSameMember($SCHEMA_TOTAL_PARTS_COUNT, member, value));
                case 1 -> partNumberMarker((String) SchemaUtils.validateSameMember($SCHEMA_PART_NUMBER_MARKER, member, value));
                case 2 -> nextPartNumberMarker((String) SchemaUtils.validateSameMember($SCHEMA_NEXT_PART_NUMBER_MARKER, member, value));
                case 3 -> maxParts((Integer) SchemaUtils.validateSameMember($SCHEMA_MAX_PARTS, member, value));
                case 4 -> isTruncated((Boolean) SchemaUtils.validateSameMember($SCHEMA_IS_TRUNCATED, member, value));
                case 5 -> parts((List<ObjectPart>) SchemaUtils.validateSameMember($SCHEMA_PARTS, member, value));
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
                    case 0 -> builder.totalPartsCount(de.readInteger(member));
                    case 1 -> builder.partNumberMarker(de.readString(member));
                    case 2 -> builder.nextPartNumberMarker(de.readString(member));
                    case 3 -> builder.maxParts(de.readInteger(member));
                    case 4 -> builder.isTruncated(de.readBoolean(member));
                    case 5 -> builder.parts(SharedSerde.deserializePartsList(member, de));
                    default -> throw new IllegalArgumentException("Unexpected member: " + member.memberName());
                }
            }
        }
    }
}
