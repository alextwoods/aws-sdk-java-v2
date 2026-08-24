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
 * Contains the configuration of a vector index as it existed at the time a backup was created.
 */
@SmithyGenerated
public final class VectorIndexInfo implements SerializableStruct {

    public static final Schema $SCHEMA = Schemas.VECTOR_INDEX_INFO;
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
    private final transient Long dimensions;
    private final transient VectorDistanceFunction distanceFunction;

    private VectorIndexInfo(Builder builder) {
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
    public Long getDimensions() {
        return dimensions;
    }

    /**
     * The distance function used to calculate similarity between vectors.
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
        VectorIndexInfo that = (VectorIndexInfo) other;
        return Objects.equals(this.dimensions, that.dimensions)
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
        $hc = 31 * $hc + Objects.hashCode(dimensions);
        $hc = 31 * $hc + Objects.hashCode(distanceFunction);
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
        if (vectorAttribute != null) {
            serializer.writeStruct($SCHEMA_VECTOR_ATTRIBUTE, vectorAttribute);
        }
        if (searchSchema != null) {
            serializer.writeList($SCHEMA_SEARCH_SCHEMA, searchSchema, searchSchema.size(), SharedSerde.SearchSchemaSerializer.INSTANCE);
        }
        if (projection != null) {
            serializer.writeStruct($SCHEMA_PROJECTION, projection);
        }
        if (dimensions != null) {
            serializer.writeLong($SCHEMA_DIMENSIONS, dimensions);
        }
        if (distanceFunction != null) {
            serializer.writeString($SCHEMA_DISTANCE_FUNCTION, distanceFunction.getValue());
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T getMemberValue(Schema member) {
        return switch (member.memberIndex()) {
            case 0 -> (T) SchemaUtils.validateSameMember($SCHEMA_INDEX_NAME, member, indexName);
            case 1 -> (T) SchemaUtils.validateSameMember($SCHEMA_VECTOR_ATTRIBUTE, member, vectorAttribute);
            case 2 -> (T) SchemaUtils.validateSameMember($SCHEMA_SEARCH_SCHEMA, member, searchSchema);
            case 3 -> (T) SchemaUtils.validateSameMember($SCHEMA_PROJECTION, member, projection);
            case 4 -> (T) SchemaUtils.validateSameMember($SCHEMA_DIMENSIONS, member, dimensions);
            case 5 -> (T) SchemaUtils.validateSameMember($SCHEMA_DISTANCE_FUNCTION, member, distanceFunction);
            default -> throw new IllegalArgumentException("Attempted to get non-existent member: " + member.id());
        };
    }

    /**
     * Create a new builder containing all the current property values of this object.
     *
     * <p><strong>Note:</strong> This method performs only a shallow copy of the original properties.
     *
     * @return a builder for {@link VectorIndexInfo}.
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
     * Builder for {@link VectorIndexInfo}.
     */
    public static final class Builder implements ShapeBuilder<VectorIndexInfo> {
        private String indexName;
        private VectorAttributeDefinition vectorAttribute;
        private List<SearchSchemaElement> searchSchema;
        private Projection projection;
        private Long dimensions;
        private VectorDistanceFunction distanceFunction;

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
         * The vector attribute configuration for the index.
         *
         * @return this builder.
         */
        public Builder vectorAttribute(VectorAttributeDefinition vectorAttribute) {
            this.vectorAttribute = vectorAttribute;
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

        @Override
        public VectorIndexInfo build() {
            return new VectorIndexInfo(this);
        }

        @Override
        @SuppressWarnings("unchecked")
        public void setMemberValue(Schema member, Object value) {
            switch (member.memberIndex()) {
                case 0 -> indexName((String) SchemaUtils.validateSameMember($SCHEMA_INDEX_NAME, member, value));
                case 1 -> vectorAttribute((VectorAttributeDefinition) SchemaUtils.validateSameMember($SCHEMA_VECTOR_ATTRIBUTE, member, value));
                case 2 -> searchSchema((List<SearchSchemaElement>) SchemaUtils.validateSameMember($SCHEMA_SEARCH_SCHEMA, member, value));
                case 3 -> projection((Projection) SchemaUtils.validateSameMember($SCHEMA_PROJECTION, member, value));
                case 4 -> dimensions((Long) SchemaUtils.validateSameMember($SCHEMA_DIMENSIONS, member, value));
                case 5 -> distanceFunction((VectorDistanceFunction) SchemaUtils.validateSameMember($SCHEMA_DISTANCE_FUNCTION, member, value));
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
                    case 1 -> builder.vectorAttribute(VectorAttributeDefinition.builder().deserializeMember(de, member).build());
                    case 2 -> builder.searchSchema(SharedSerde.deserializeSearchSchema(member, de));
                    case 3 -> builder.projection(Projection.builder().deserializeMember(de, member).build());
                    case 4 -> builder.dimensions(de.readLong(member));
                    case 5 -> builder.distanceFunction(VectorDistanceFunction.builder().deserializeMember(de, member).build());
                    default -> throw new IllegalArgumentException("Unexpected member: " + member.memberName());
                }
            }
        }
    }
}
