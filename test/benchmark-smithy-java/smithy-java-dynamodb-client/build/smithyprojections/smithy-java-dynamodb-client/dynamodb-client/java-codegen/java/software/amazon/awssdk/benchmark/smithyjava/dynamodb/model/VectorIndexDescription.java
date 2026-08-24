package software.amazon.awssdk.benchmark.smithyjava.dynamodb.model;

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
 * Contains the current state and configuration of a vector index, including its status, size, item count, and the
 * settings specified when the index was created.
 */
@SmithyGenerated
public final class VectorIndexDescription implements SerializableStruct {

    public static final Schema $SCHEMA = Schemas.VECTOR_INDEX_DESCRIPTION;
    private static final Schema $SCHEMA_INDEX_NAME = $SCHEMA.member("IndexName");
    private static final Schema $SCHEMA_SEARCH_SCHEMA = $SCHEMA.member("SearchSchema");
    private static final Schema $SCHEMA_PROJECTION = $SCHEMA.member("Projection");
    private static final Schema $SCHEMA_VECTOR_ATTRIBUTE = $SCHEMA.member("VectorAttribute");
    private static final Schema $SCHEMA_DIMENSIONS = $SCHEMA.member("Dimensions");
    private static final Schema $SCHEMA_DISTANCE_FUNCTION = $SCHEMA.member("DistanceFunction");
    private static final Schema $SCHEMA_INDEX_STATUS = $SCHEMA.member("IndexStatus");
    private static final Schema $SCHEMA_BACKFILLING = $SCHEMA.member("Backfilling");
    private static final Schema $SCHEMA_INDEX_SIZE_BYTES = $SCHEMA.member("IndexSizeBytes");
    private static final Schema $SCHEMA_ITEM_COUNT = $SCHEMA.member("ItemCount");
    private static final Schema $SCHEMA_INDEX_ARN = $SCHEMA.member("IndexArn");

    public static final ShapeId $ID = $SCHEMA.id();

    private final transient String indexName;
    private final transient List<SearchSchemaElement> searchSchema;
    private final transient Projection projection;
    private final transient VectorAttributeDefinition vectorAttribute;
    private final transient Long dimensions;
    private final transient VectorDistanceFunction distanceFunction;
    private final transient IndexStatus indexStatus;
    private final transient Boolean backfilling;
    private final transient Long indexSizeBytes;
    private final transient Long itemCount;
    private final transient String indexArn;

    private VectorIndexDescription(Builder builder) {
        this.indexName = builder.indexName;
        this.searchSchema = builder.searchSchema == null ? null : Collections.unmodifiableList(builder.searchSchema);
        this.projection = builder.projection;
        this.vectorAttribute = builder.vectorAttribute;
        this.dimensions = builder.dimensions;
        this.distanceFunction = builder.distanceFunction;
        this.indexStatus = builder.indexStatus;
        this.backfilling = builder.backfilling;
        this.indexSizeBytes = builder.indexSizeBytes;
        this.itemCount = builder.itemCount;
        this.indexArn = builder.indexArn;
    }

    /**
     * The name of the vector index.
     */
    public String getIndexName() {
        return indexName;
    }

    /**
     * The search schema that defines partition key and inline filter attributes for the vector index.
     */
    public List<SearchSchemaElement> getSearchSchema() {
        if (searchSchema == null) {
            return Collections.emptyList();
        }
        return searchSchema;
    }

    public boolean hasSearchSchema() {
        return searchSchema != null;
    }

    /**
     * Specifies attributes that are copied (projected) from the table into the vector index.
     */
    public Projection getProjection() {
        return projection;
    }

    /**
     * The vector attribute configuration for the index.
     */
    public VectorAttributeDefinition getVectorAttribute() {
        return vectorAttribute;
    }

    /**
     * The number of dimensions in each vector.
     */
    public Long getDimensions() {
        return dimensions;
    }

    /**
     * The distance function used to calculate similarity between vectors.
     */
    public VectorDistanceFunction getDistanceFunction() {
        return distanceFunction;
    }

    /**
     * The current state of the vector index:
     *
     * <ul>
     *   <li>
     *     <code>CREATING</code> - The index is being created.
     *   </li>
     *   <li>
     *     <code>ACTIVE</code> - The index is ready for use.
     *   </li>
     *   <li>
     *     <code>DELETING</code> - The index is being deleted.
     *   </li>
     * </ul>
     */
    public IndexStatus getIndexStatus() {
        return indexStatus;
    }

    /**
     * Specifies whether the index is currently backfilling. During backfill, <code>SearchVectors</code> operations
     * might return incomplete results.
     */
    public Boolean isBackfilling() {
        return backfilling;
    }

    /**
     * The total size of the vector index, in bytes. Amazon DynamoDB updates this value approximately every six hours.
     * Recent changes might not be reflected in this value.
     */
    public Long getIndexSizeBytes() {
        return indexSizeBytes;
    }

    /**
     * The number of items indexed in the vector index. Amazon DynamoDB updates this value approximately every six
     * hours. Recent changes might not be reflected in this value.
     */
    public Long getItemCount() {
        return itemCount;
    }

    /**
     * The Amazon Resource Name (ARN) that uniquely identifies the vector index.
     */
    public String getIndexArn() {
        return indexArn;
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
        VectorIndexDescription that = (VectorIndexDescription) other;
        return Objects.equals(this.backfilling, that.backfilling)
               && Objects.equals(this.dimensions, that.dimensions)
               && Objects.equals(this.indexSizeBytes, that.indexSizeBytes)
               && Objects.equals(this.itemCount, that.itemCount)
               && Objects.equals(this.indexName, that.indexName)
               && Objects.equals(this.indexArn, that.indexArn)
               && Objects.equals(this.distanceFunction, that.distanceFunction)
               && Objects.equals(this.indexStatus, that.indexStatus)
               && Objects.equals(this.projection, that.projection)
               && Objects.equals(this.vectorAttribute, that.vectorAttribute)
               && Objects.equals(this.searchSchema, that.searchSchema);
    }

    @Override
    public int hashCode() {
        int $hc = Objects.hashCode(indexName);
        $hc = 31 * $hc + Objects.hashCode(searchSchema);
        $hc = 31 * $hc + Objects.hashCode(projection);
        $hc = 31 * $hc + Objects.hashCode(vectorAttribute);
        $hc = 31 * $hc + Objects.hashCode(dimensions);
        $hc = 31 * $hc + Objects.hashCode(distanceFunction);
        $hc = 31 * $hc + Objects.hashCode(indexStatus);
        $hc = 31 * $hc + Objects.hashCode(backfilling);
        $hc = 31 * $hc + Objects.hashCode(indexSizeBytes);
        $hc = 31 * $hc + Objects.hashCode(itemCount);
        $hc = 31 * $hc + Objects.hashCode(indexArn);
        return $hc;
    }

    @Override
    public Schema schema() {
        return $SCHEMA;
    }

    @Override
    public void serializeMembers(ShapeSerializer serializer) {
        if (indexName != null) {
            serializer.writeString($SCHEMA_INDEX_NAME, indexName);
        }
        if (searchSchema != null) {
            serializer.writeList($SCHEMA_SEARCH_SCHEMA, searchSchema, searchSchema.size(), SharedSerde.SearchSchemaSerializer.INSTANCE);
        }
        if (projection != null) {
            serializer.writeStruct($SCHEMA_PROJECTION, projection);
        }
        if (vectorAttribute != null) {
            serializer.writeStruct($SCHEMA_VECTOR_ATTRIBUTE, vectorAttribute);
        }
        if (dimensions != null) {
            serializer.writeLong($SCHEMA_DIMENSIONS, dimensions);
        }
        if (distanceFunction != null) {
            serializer.writeString($SCHEMA_DISTANCE_FUNCTION, distanceFunction.getValue());
        }
        if (indexStatus != null) {
            serializer.writeString($SCHEMA_INDEX_STATUS, indexStatus.getValue());
        }
        if (backfilling != null) {
            serializer.writeBoolean($SCHEMA_BACKFILLING, backfilling);
        }
        if (indexSizeBytes != null) {
            serializer.writeLong($SCHEMA_INDEX_SIZE_BYTES, indexSizeBytes);
        }
        if (itemCount != null) {
            serializer.writeLong($SCHEMA_ITEM_COUNT, itemCount);
        }
        if (indexArn != null) {
            serializer.writeString($SCHEMA_INDEX_ARN, indexArn);
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T getMemberValue(Schema member) {
        return switch (member.memberIndex()) {
            case 0 -> (T) SchemaUtils.validateSameMember($SCHEMA_INDEX_NAME, member, indexName);
            case 1 -> (T) SchemaUtils.validateSameMember($SCHEMA_SEARCH_SCHEMA, member, searchSchema);
            case 2 -> (T) SchemaUtils.validateSameMember($SCHEMA_PROJECTION, member, projection);
            case 3 -> (T) SchemaUtils.validateSameMember($SCHEMA_VECTOR_ATTRIBUTE, member, vectorAttribute);
            case 4 -> (T) SchemaUtils.validateSameMember($SCHEMA_DIMENSIONS, member, dimensions);
            case 5 -> (T) SchemaUtils.validateSameMember($SCHEMA_DISTANCE_FUNCTION, member, distanceFunction);
            case 6 -> (T) SchemaUtils.validateSameMember($SCHEMA_INDEX_STATUS, member, indexStatus);
            case 7 -> (T) SchemaUtils.validateSameMember($SCHEMA_BACKFILLING, member, backfilling);
            case 8 -> (T) SchemaUtils.validateSameMember($SCHEMA_INDEX_SIZE_BYTES, member, indexSizeBytes);
            case 9 -> (T) SchemaUtils.validateSameMember($SCHEMA_ITEM_COUNT, member, itemCount);
            case 10 -> (T) SchemaUtils.validateSameMember($SCHEMA_INDEX_ARN, member, indexArn);
            default -> throw new IllegalArgumentException("Attempted to get non-existent member: " + member.id());
        };
    }

    /**
     * Create a new builder containing all the current property values of this object.
     *
     * <p><strong>Note:</strong> This method performs only a shallow copy of the original properties.
     *
     * @return a builder for {@link VectorIndexDescription}.
     */
    public Builder toBuilder() {
        var builder = new Builder();
        builder.indexName(this.indexName);
        builder.searchSchema(this.searchSchema);
        builder.projection(this.projection);
        builder.vectorAttribute(this.vectorAttribute);
        builder.dimensions(this.dimensions);
        builder.distanceFunction(this.distanceFunction);
        builder.indexStatus(this.indexStatus);
        builder.backfilling(this.backfilling);
        builder.indexSizeBytes(this.indexSizeBytes);
        builder.itemCount(this.itemCount);
        builder.indexArn(this.indexArn);
        return builder;
    }

    /**
     * @return returns a new Builder.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for {@link VectorIndexDescription}.
     */
    public static final class Builder implements ShapeBuilder<VectorIndexDescription> {
        private String indexName;
        private List<SearchSchemaElement> searchSchema;
        private Projection projection;
        private VectorAttributeDefinition vectorAttribute;
        private Long dimensions;
        private VectorDistanceFunction distanceFunction;
        private IndexStatus indexStatus;
        private Boolean backfilling;
        private Long indexSizeBytes;
        private Long itemCount;
        private String indexArn;

        private Builder() {}

        @Override
        public Schema schema() {
            return $SCHEMA;
        }

        /**
         * The name of the vector index.
         *
         * @return this builder.
         */
        public Builder indexName(String indexName) {
            this.indexName = indexName;
            return this;
        }

        /**
         * The search schema that defines partition key and inline filter attributes for the vector index.
         *
         * @return this builder.
         */
        public Builder searchSchema(List<SearchSchemaElement> searchSchema) {
            this.searchSchema = searchSchema;
            return this;
        }

        /**
         * Specifies attributes that are copied (projected) from the table into the vector index.
         *
         * @return this builder.
         */
        public Builder projection(Projection projection) {
            this.projection = projection;
            return this;
        }

        /**
         * The vector attribute configuration for the index.
         *
         * @return this builder.
         */
        public Builder vectorAttribute(VectorAttributeDefinition vectorAttribute) {
            this.vectorAttribute = vectorAttribute;
            return this;
        }

        /**
         * The number of dimensions in each vector.
         *
         * @return this builder.
         */
        public Builder dimensions(Long dimensions) {
            this.dimensions = dimensions;
            return this;
        }

        /**
         * The distance function used to calculate similarity between vectors.
         *
         * @return this builder.
         */
        public Builder distanceFunction(VectorDistanceFunction distanceFunction) {
            this.distanceFunction = distanceFunction;
            return this;
        }

        /**
         * The current state of the vector index:
         *
         * <ul>
         *   <li>
         *     <code>CREATING</code> - The index is being created.
         *   </li>
         *   <li>
         *     <code>ACTIVE</code> - The index is ready for use.
         *   </li>
         *   <li>
         *     <code>DELETING</code> - The index is being deleted.
         *   </li>
         * </ul>
         *
         * @return this builder.
         */
        public Builder indexStatus(IndexStatus indexStatus) {
            this.indexStatus = indexStatus;
            return this;
        }

        /**
         * Specifies whether the index is currently backfilling. During backfill, <code>SearchVectors</code> operations
         * might return incomplete results.
         *
         * @return this builder.
         */
        public Builder backfilling(Boolean backfilling) {
            this.backfilling = backfilling;
            return this;
        }

        /**
         * The total size of the vector index, in bytes. Amazon DynamoDB updates this value approximately every six hours.
         * Recent changes might not be reflected in this value.
         *
         * @return this builder.
         */
        public Builder indexSizeBytes(Long indexSizeBytes) {
            this.indexSizeBytes = indexSizeBytes;
            return this;
        }

        /**
         * The number of items indexed in the vector index. Amazon DynamoDB updates this value approximately every six
         * hours. Recent changes might not be reflected in this value.
         *
         * @return this builder.
         */
        public Builder itemCount(Long itemCount) {
            this.itemCount = itemCount;
            return this;
        }

        /**
         * The Amazon Resource Name (ARN) that uniquely identifies the vector index.
         *
         * @return this builder.
         */
        public Builder indexArn(String indexArn) {
            this.indexArn = indexArn;
            return this;
        }

        @Override
        public VectorIndexDescription build() {
            return new VectorIndexDescription(this);
        }

        @Override
        @SuppressWarnings("unchecked")
        public void setMemberValue(Schema member, Object value) {
            switch (member.memberIndex()) {
                case 0 -> indexName((String) SchemaUtils.validateSameMember($SCHEMA_INDEX_NAME, member, value));
                case 1 -> searchSchema((List<SearchSchemaElement>) SchemaUtils.validateSameMember($SCHEMA_SEARCH_SCHEMA, member, value));
                case 2 -> projection((Projection) SchemaUtils.validateSameMember($SCHEMA_PROJECTION, member, value));
                case 3 -> vectorAttribute((VectorAttributeDefinition) SchemaUtils.validateSameMember($SCHEMA_VECTOR_ATTRIBUTE, member, value));
                case 4 -> dimensions((Long) SchemaUtils.validateSameMember($SCHEMA_DIMENSIONS, member, value));
                case 5 -> distanceFunction((VectorDistanceFunction) SchemaUtils.validateSameMember($SCHEMA_DISTANCE_FUNCTION, member, value));
                case 6 -> indexStatus((IndexStatus) SchemaUtils.validateSameMember($SCHEMA_INDEX_STATUS, member, value));
                case 7 -> backfilling((Boolean) SchemaUtils.validateSameMember($SCHEMA_BACKFILLING, member, value));
                case 8 -> indexSizeBytes((Long) SchemaUtils.validateSameMember($SCHEMA_INDEX_SIZE_BYTES, member, value));
                case 9 -> itemCount((Long) SchemaUtils.validateSameMember($SCHEMA_ITEM_COUNT, member, value));
                case 10 -> indexArn((String) SchemaUtils.validateSameMember($SCHEMA_INDEX_ARN, member, value));
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
                    case 0 -> builder.indexName(de.readString(member));
                    case 1 -> builder.searchSchema(SharedSerde.deserializeSearchSchema(member, de));
                    case 2 -> builder.projection(Projection.builder().deserializeMember(de, member).build());
                    case 3 -> builder.vectorAttribute(VectorAttributeDefinition.builder().deserializeMember(de, member).build());
                    case 4 -> builder.dimensions(de.readLong(member));
                    case 5 -> builder.distanceFunction(VectorDistanceFunction.builder().deserializeMember(de, member).build());
                    case 6 -> builder.indexStatus(IndexStatus.builder().deserializeMember(de, member).build());
                    case 7 -> builder.backfilling(de.readBoolean(member));
                    case 8 -> builder.indexSizeBytes(de.readLong(member));
                    case 9 -> builder.itemCount(de.readLong(member));
                    case 10 -> builder.indexArn(de.readString(member));
                    default -> throw new IllegalArgumentException("Unexpected member: " + member.memberName());
                }
            }
        }
    }
}
