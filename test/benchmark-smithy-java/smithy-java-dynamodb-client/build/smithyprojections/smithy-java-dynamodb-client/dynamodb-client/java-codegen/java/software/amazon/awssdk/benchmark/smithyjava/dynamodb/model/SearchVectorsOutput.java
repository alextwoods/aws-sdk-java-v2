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

@SmithyGenerated
public final class SearchVectorsOutput implements SerializableStruct {

    public static final Schema $SCHEMA = Schemas.SEARCH_VECTORS_OUTPUT;
    private static final Schema $SCHEMA_CONSUMED_CAPACITY = $SCHEMA.member("ConsumedCapacity");
    private static final Schema $SCHEMA_SEARCH_RESULTS = $SCHEMA.member("SearchResults");

    public static final ShapeId $ID = $SCHEMA.id();

    private final transient VectorCapacity consumedCapacity;
    private final transient List<SearchResultItem> searchResults;

    private SearchVectorsOutput(Builder builder) {
        this.consumedCapacity = builder.consumedCapacity;
        this.searchResults = builder.searchResults == null ? null : Collections.unmodifiableList(builder.searchResults);
    }

    /**
     * The capacity units consumed by the <code>SearchVectors</code> operation. Contains
     * <code>VectorSearchRequestBytes</code>, which represents the vector search capacity consumed.
     */
    public VectorCapacity getConsumedCapacity() {
        return consumedCapacity;
    }

    /**
     * A list of items returned by the vector similarity search, sorted by similarity with the most similar item first.
     * Each item contains the projected attributes and a similarity score.
     */
    public List<SearchResultItem> getSearchResults() {
        if (searchResults == null) {
            return Collections.emptyList();
        }
        return searchResults;
    }

    public boolean hasSearchResults() {
        return searchResults != null;
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
        SearchVectorsOutput that = (SearchVectorsOutput) other;
        return Objects.equals(this.consumedCapacity, that.consumedCapacity)
               && Objects.equals(this.searchResults, that.searchResults);
    }

    @Override
    public int hashCode() {
        int $hc = Objects.hashCode(consumedCapacity);
        $hc = 31 * $hc + Objects.hashCode(searchResults);
        return $hc;
    }

    @Override
    public Schema schema() {
        return $SCHEMA;
    }

    @Override
    public void serializeMembers(ShapeSerializer serializer) {
        if (consumedCapacity != null) {
            serializer.writeStruct($SCHEMA_CONSUMED_CAPACITY, consumedCapacity);
        }
        if (searchResults != null) {
            serializer.writeList($SCHEMA_SEARCH_RESULTS, searchResults, searchResults.size(), SharedSerde.SearchResultListSerializer.INSTANCE);
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T getMemberValue(Schema member) {
        return switch (member.memberIndex()) {
            case 0 -> (T) SchemaUtils.validateSameMember($SCHEMA_CONSUMED_CAPACITY, member, consumedCapacity);
            case 1 -> (T) SchemaUtils.validateSameMember($SCHEMA_SEARCH_RESULTS, member, searchResults);
            default -> throw new IllegalArgumentException("Attempted to get non-existent member: " + member.id());
        };
    }

    /**
     * Create a new builder containing all the current property values of this object.
     *
     * <p><strong>Note:</strong> This method performs only a shallow copy of the original properties.
     *
     * @return a builder for {@link SearchVectorsOutput}.
     */
    public Builder toBuilder() {
        var builder = new Builder();
        builder.consumedCapacity(this.consumedCapacity);
        builder.searchResults(this.searchResults);
        return builder;
    }

    /**
     * @return returns a new Builder.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for {@link SearchVectorsOutput}.
     */
    public static final class Builder implements ShapeBuilder<SearchVectorsOutput> {
        private VectorCapacity consumedCapacity;
        private List<SearchResultItem> searchResults;

        private Builder() {}

        @Override
        public Schema schema() {
            return $SCHEMA;
        }

        /**
         * The capacity units consumed by the <code>SearchVectors</code> operation. Contains
         * <code>VectorSearchRequestBytes</code>, which represents the vector search capacity consumed.
         *
         * @return this builder.
         */
        public Builder consumedCapacity(VectorCapacity consumedCapacity) {
            this.consumedCapacity = consumedCapacity;
            return this;
        }

        /**
         * A list of items returned by the vector similarity search, sorted by similarity with the most similar item first.
         * Each item contains the projected attributes and a similarity score.
         *
         * @return this builder.
         */
        public Builder searchResults(List<SearchResultItem> searchResults) {
            this.searchResults = searchResults;
            return this;
        }

        @Override
        public SearchVectorsOutput build() {
            return new SearchVectorsOutput(this);
        }

        @Override
        @SuppressWarnings("unchecked")
        public void setMemberValue(Schema member, Object value) {
            switch (member.memberIndex()) {
                case 0 -> consumedCapacity((VectorCapacity) SchemaUtils.validateSameMember($SCHEMA_CONSUMED_CAPACITY, member, value));
                case 1 -> searchResults((List<SearchResultItem>) SchemaUtils.validateSameMember($SCHEMA_SEARCH_RESULTS, member, value));
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
                    case 0 -> builder.consumedCapacity(VectorCapacity.builder().deserializeMember(de, member).build());
                    case 1 -> builder.searchResults(SharedSerde.deserializeSearchResultList(member, de));
                    default -> throw new IllegalArgumentException("Unexpected member: " + member.memberName());
                }
            }
        }
    }
}
