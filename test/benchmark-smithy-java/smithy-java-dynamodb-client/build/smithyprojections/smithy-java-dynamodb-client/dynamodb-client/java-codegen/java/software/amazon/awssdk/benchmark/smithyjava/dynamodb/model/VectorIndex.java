package software.amazon.awssdk.benchmark.smithyjava.dynamodb.model;

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

/**
 * Contains the configuration settings for a vector index, including the index name, vector attribute, dimensions,
 * distance function, search schema, and projection.
 */
@SmithyGenerated
public final class VectorIndex implements SerializableStruct {

    public static final Schema $SCHEMA = Schemas.VECTOR_INDEX;
    private static final Schema $SCHEMA_INDEX_NAME = $SCHEMA.member("IndexName");
    private static final Schema $SCHEMA_VECTOR_ATTRIBUTE = $SCHEMA.member("VectorAttribute");
    private static final Schema $SCHEMA_SEARCH_SCHEMA = $SCHEMA.member("SearchSchema");
    private static final Schema $SCHEMA_PROJECTION = $SCHEMA.member("Projection");
    private static final Schema $SCHEMA_DIMENSIONS = $SCHEMA.member("Dimensions");
    private static final Schema $SCHEMA_DISTANCE_FUNCTION = $SCHEMA.member("DistanceFunction");

    public static final ShapeId $ID = $SCHEMA.id();

    private final transient String indexName;
    private final transient VectorAttributeDefinition vectorAttribute;
    private final transient List<SearchSchemaElement> searchSchema;
    private final transient Projection projection;
    private final transient long dimensions;
    private final transient VectorDistanceFunction distanceFunction;

    private VectorIndex(Builder builder) {
        this.indexName = builder.indexName;
        this.vectorAttribute = builder.vectorAttribute;
        this.searchSchema = builder.searchSchema == null ? null : Collections.unmodifiableList(builder.searchSchema);
        this.projection = builder.projection;
        this.dimensions = builder.dimensions;
        this.distanceFunction = builder.distanceFunction;
    }

    /**
     * The name of the vector index.
     */
    public String getIndexName() {
        return indexName;
    }

    /**
     * The vector attribute configuration for the index.
     */
    public VectorAttributeDefinition getVectorAttribute() {
        return vectorAttribute;
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
     * The number of dimensions in each vector.
     */
    public long getDimensions() {
        return dimensions;
    }

    /**
     * The distance function used to calculate similarity between vectors. Valid values: <code>COSINE</code>,
     * <code>EUCLIDEAN</code>, <code>DOT_PRODUCT</code>.
     */
    public VectorDistanceFunction getDistanceFunction() {
        return distanceFunction;
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
        VectorIndex that = (VectorIndex) other;
        return this.dimensions == that.dimensions
               && Objects.equals(this.indexName, that.indexName)
               && Objects.equals(this.distanceFunction, that.distanceFunction)
               && Objects.equals(this.vectorAttribute, that.vectorAttribute)
               && Objects.equals(this.projection, that.projection)
               && Objects.equals(this.searchSchema, that.searchSchema);
    }

    @Override
    public int hashCode() {
        int $hc = Objects.hashCode(indexName);
        $hc = 31 * $hc + Objects.hashCode(vectorAttribute);
        $hc = 31 * $hc + Objects.hashCode(searchSchema);
        $hc = 31 * $hc + Objects.hashCode(projection);
        $hc = 31 * $hc + Long.hashCode(dimensions);
        $hc = 31 * $hc + Objects.hashCode(distanceFunction);
        return $hc;
    }

    @Override
    public Schema schema() {
        return $SCHEMA;
    }

    @Override
    public void serializeMembers(ShapeSerializer serializer) {
        serializer.writeString($SCHEMA_INDEX_NAME, indexName);
        if (vectorAttribute != null) {
            serializer.writeStruct($SCHEMA_VECTOR_ATTRIBUTE, vectorAttribute);
        }
        if (searchSchema != null) {
            serializer.writeList($SCHEMA_SEARCH_SCHEMA, searchSchema, searchSchema.size(), SharedSerde.SearchSchemaSerializer.INSTANCE);
        }
        if (projection != null) {
            serializer.writeStruct($SCHEMA_PROJECTION, projection);
        }
        serializer.writeLong($SCHEMA_DIMENSIONS, dimensions);
        serializer.writeString($SCHEMA_DISTANCE_FUNCTION, distanceFunction.getValue());
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T getMemberValue(Schema member) {
        return switch (member.memberIndex()) {
            case 0 -> (T) SchemaUtils.validateSameMember($SCHEMA_INDEX_NAME, member, indexName);
            case 1 -> (T) SchemaUtils.validateSameMember($SCHEMA_VECTOR_ATTRIBUTE, member, vectorAttribute);
            case 2 -> (T) SchemaUtils.validateSameMember($SCHEMA_PROJECTION, member, projection);
            case 3 -> (T) SchemaUtils.validateSameMember($SCHEMA_DIMENSIONS, member, dimensions);
            case 4 -> (T) SchemaUtils.validateSameMember($SCHEMA_DISTANCE_FUNCTION, member, distanceFunction);
            case 5 -> (T) SchemaUtils.validateSameMember($SCHEMA_SEARCH_SCHEMA, member, searchSchema);
            default -> throw new IllegalArgumentException("Attempted to get non-existent member: " + member.id());
        };
    }

    /**
     * Create a new builder containing all the current property values of this object.
     *
     * <p><strong>Note:</strong> This method performs only a shallow copy of the original properties.
     *
     * @return a builder for {@link VectorIndex}.
     */
    public Builder toBuilder() {
        var builder = new Builder();
        builder.indexName(this.indexName);
        builder.vectorAttribute(this.vectorAttribute);
        builder.searchSchema(this.searchSchema);
        builder.projection(this.projection);
        builder.dimensions(this.dimensions);
        builder.distanceFunction(this.distanceFunction);
        return builder;
    }

    /**
     * @return returns a new Builder.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for {@link VectorIndex}.
     */
    public static final class Builder implements ShapeBuilder<VectorIndex> {
        private final PresenceTracker tracker = PresenceTracker.of($SCHEMA);
        private String indexName;
        private VectorAttributeDefinition vectorAttribute;
        private List<SearchSchemaElement> searchSchema;
        private Projection projection;
        private long dimensions;
        private VectorDistanceFunction distanceFunction;

        private Builder() {}

        @Override
        public Schema schema() {
            return $SCHEMA;
        }

        /**
         * The name of the vector index.
         *
         * <p><strong>Required</strong>
         * @return this builder.
         */
        public Builder indexName(String indexName) {
            this.indexName = Objects.requireNonNull(indexName, "indexName cannot be null");
            tracker.setMember($SCHEMA_INDEX_NAME);
            return this;
        }

        /**
         * The vector attribute configuration for the index.
         *
         * <p><strong>Required</strong>
         * @return this builder.
         */
        public Builder vectorAttribute(VectorAttributeDefinition vectorAttribute) {
            this.vectorAttribute = Objects.requireNonNull(vectorAttribute, "vectorAttribute cannot be null");
            tracker.setMember($SCHEMA_VECTOR_ATTRIBUTE);
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
         * <p><strong>Required</strong>
         * @return this builder.
         */
        public Builder projection(Projection projection) {
            this.projection = Objects.requireNonNull(projection, "projection cannot be null");
            tracker.setMember($SCHEMA_PROJECTION);
            return this;
        }

        /**
         * The number of dimensions in each vector.
         *
         * <p><strong>Required</strong>
         * @return this builder.
         */
        public Builder dimensions(long dimensions) {
            this.dimensions = dimensions;
            tracker.setMember($SCHEMA_DIMENSIONS);
            return this;
        }

        /**
         * The distance function used to calculate similarity between vectors. Valid values: <code>COSINE</code>,
         * <code>EUCLIDEAN</code>, <code>DOT_PRODUCT</code>.
         *
         * <p><strong>Required</strong>
         * @return this builder.
         */
        public Builder distanceFunction(VectorDistanceFunction distanceFunction) {
            this.distanceFunction = Objects.requireNonNull(distanceFunction, "distanceFunction cannot be null");
            tracker.setMember($SCHEMA_DISTANCE_FUNCTION);
            return this;
        }

        @Override
        public VectorIndex build() {
            tracker.validate();
            return new VectorIndex(this);
        }

        @Override
        @SuppressWarnings("unchecked")
        public void setMemberValue(Schema member, Object value) {
            switch (member.memberIndex()) {
                case 0 -> indexName((String) SchemaUtils.validateSameMember($SCHEMA_INDEX_NAME, member, value));
                case 1 -> vectorAttribute((VectorAttributeDefinition) SchemaUtils.validateSameMember($SCHEMA_VECTOR_ATTRIBUTE, member, value));
                case 2 -> projection((Projection) SchemaUtils.validateSameMember($SCHEMA_PROJECTION, member, value));
                case 3 -> dimensions((long) SchemaUtils.validateSameMember($SCHEMA_DIMENSIONS, member, value));
                case 4 -> distanceFunction((VectorDistanceFunction) SchemaUtils.validateSameMember($SCHEMA_DISTANCE_FUNCTION, member, value));
                case 5 -> searchSchema((List<SearchSchemaElement>) SchemaUtils.validateSameMember($SCHEMA_SEARCH_SCHEMA, member, value));
                default -> ShapeBuilder.super.setMemberValue(member, value);
            }
        }

        @Override
        public ShapeBuilder<VectorIndex> errorCorrection() {
            if (tracker.allSet()) {
                return this;
            }
            if (!tracker.checkMember($SCHEMA_INDEX_NAME)) {
                indexName("");
            }
            if (!tracker.checkMember($SCHEMA_VECTOR_ATTRIBUTE)) {
                tracker.setMember($SCHEMA_VECTOR_ATTRIBUTE);
            }
            if (!tracker.checkMember($SCHEMA_PROJECTION)) {
                tracker.setMember($SCHEMA_PROJECTION);
            }
            if (!tracker.checkMember($SCHEMA_DIMENSIONS)) {
                tracker.setMember($SCHEMA_DIMENSIONS);
            }
            if (!tracker.checkMember($SCHEMA_DISTANCE_FUNCTION)) {
                distanceFunction(VectorDistanceFunction.unknown(""));
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
                    case 0 -> builder.indexName(de.readString(member));
                    case 1 -> builder.vectorAttribute(VectorAttributeDefinition.builder().deserializeMember(de, member).build());
                    case 2 -> builder.projection(Projection.builder().deserializeMember(de, member).build());
                    case 3 -> builder.dimensions(de.readLong(member));
                    case 4 -> builder.distanceFunction(VectorDistanceFunction.builder().deserializeMember(de, member).build());
                    case 5 -> builder.searchSchema(SharedSerde.deserializeSearchSchema(member, de));
                    default -> throw new IllegalArgumentException("Unexpected member: " + member.memberName());
                }
            }
        }
    }
}
