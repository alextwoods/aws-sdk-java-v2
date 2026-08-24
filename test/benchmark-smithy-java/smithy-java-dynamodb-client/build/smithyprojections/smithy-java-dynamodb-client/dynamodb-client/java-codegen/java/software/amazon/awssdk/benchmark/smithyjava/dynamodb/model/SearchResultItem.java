package software.amazon.awssdk.benchmark.smithyjava.dynamodb.model;

import java.util.Collections;
import java.util.Map;
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
 * A single result from a <code>SearchVectors</code> operation.
 */
@SmithyGenerated
public final class SearchResultItem implements SerializableStruct {

    public static final Schema $SCHEMA = Schemas.SEARCH_RESULT_ITEM;
    private static final Schema $SCHEMA_ITEM = $SCHEMA.member("Item");
    private static final Schema $SCHEMA_SCORE = $SCHEMA.member("Score");

    public static final ShapeId $ID = $SCHEMA.id();

    private final transient Map<String, AttributeValue> item;
    private final transient double score;

    private SearchResultItem(Builder builder) {
        this.item = builder.item == null ? null : Collections.unmodifiableMap(builder.item);
        this.score = builder.score;
    }

    /**
     * A map of attribute names to <code>AttributeValue</code> objects, representing the projected attributes of the
     * item returned by the vector search.
     */
    public Map<String, AttributeValue> getItem() {
        if (item == null) {
            return Collections.emptyMap();
        }
        return item;
    }

    public boolean hasItem() {
        return item != null;
    }

    /**
     * The similarity score for this item relative to the search vector. The interpretation depends on the distance
     * function configured for the vector index.
     */
    public double getScore() {
        return score;
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
        SearchResultItem that = (SearchResultItem) other;
        return Double.compare(this.score, that.score) == 0
               && Objects.equals(this.item, that.item);
    }

    @Override
    public int hashCode() {
        int $hc = Objects.hashCode(item);
        $hc = 31 * $hc + Double.hashCode(score);
        return $hc;
    }

    @Override
    public Schema schema() {
        return $SCHEMA;
    }

    @Override
    public void serializeMembers(ShapeSerializer serializer) {
        if (item != null) {
            serializer.writeMap($SCHEMA_ITEM, item, item.size(), SharedSerde.AttributeMapSerializer.INSTANCE);
        }
        serializer.writeDouble($SCHEMA_SCORE, score);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T getMemberValue(Schema member) {
        return switch (member.memberIndex()) {
            case 0 -> (T) SchemaUtils.validateSameMember($SCHEMA_ITEM, member, item);
            case 1 -> (T) SchemaUtils.validateSameMember($SCHEMA_SCORE, member, score);
            default -> throw new IllegalArgumentException("Attempted to get non-existent member: " + member.id());
        };
    }

    /**
     * Create a new builder containing all the current property values of this object.
     *
     * <p><strong>Note:</strong> This method performs only a shallow copy of the original properties.
     *
     * @return a builder for {@link SearchResultItem}.
     */
    public Builder toBuilder() {
        var builder = new Builder();
        builder.item(this.item);
        builder.score(this.score);
        return builder;
    }

    /**
     * @return returns a new Builder.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for {@link SearchResultItem}.
     */
    public static final class Builder implements ShapeBuilder<SearchResultItem> {
        private Map<String, AttributeValue> item;
        private double score = 0.0;

        private Builder() {}

        @Override
        public Schema schema() {
            return $SCHEMA;
        }

        /**
         * A map of attribute names to <code>AttributeValue</code> objects, representing the projected attributes of the
         * item returned by the vector search.
         *
         * @return this builder.
         */
        public Builder item(Map<String, AttributeValue> item) {
            this.item = item;
            return this;
        }

        /**
         * The similarity score for this item relative to the search vector. The interpretation depends on the distance
         * function configured for the vector index.
         *
         * @return this builder.
         */
        public Builder score(double score) {
            this.score = score;
            return this;
        }

        @Override
        public SearchResultItem build() {
            return new SearchResultItem(this);
        }

        @Override
        @SuppressWarnings("unchecked")
        public void setMemberValue(Schema member, Object value) {
            switch (member.memberIndex()) {
                case 0 -> item((Map<String, AttributeValue>) SchemaUtils.validateSameMember($SCHEMA_ITEM, member, value));
                case 1 -> score((double) SchemaUtils.validateSameMember($SCHEMA_SCORE, member, value));
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
                    case 0 -> builder.item(SharedSerde.deserializeAttributeMap(member, de));
                    case 1 -> builder.score(de.readDouble(member));
                    default -> throw new IllegalArgumentException("Unexpected member: " + member.memberName());
                }
            }
        }
    }
}
