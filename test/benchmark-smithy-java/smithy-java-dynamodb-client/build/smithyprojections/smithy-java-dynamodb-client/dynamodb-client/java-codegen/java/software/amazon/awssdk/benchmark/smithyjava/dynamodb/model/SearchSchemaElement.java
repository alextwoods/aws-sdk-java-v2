package software.amazon.awssdk.benchmark.smithyjava.dynamodb.model;

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
 * An element in the search schema of a vector index.
 */
@SmithyGenerated
public final class SearchSchemaElement implements SerializableStruct {

    public static final Schema $SCHEMA = Schemas.SEARCH_SCHEMA_ELEMENT;
    private static final Schema $SCHEMA_ATTRIBUTE_NAME = $SCHEMA.member("AttributeName");
    private static final Schema $SCHEMA_SEARCH_SCHEMA_ELEMENT_TYPE = $SCHEMA.member("SearchSchemaElementType");

    public static final ShapeId $ID = $SCHEMA.id();

    private final transient String attributeName;
    private final transient SearchSchemaElementType searchSchemaElementType;

    private SearchSchemaElement(Builder builder) {
        this.attributeName = builder.attributeName;
        this.searchSchemaElementType = builder.searchSchemaElementType;
    }

    /**
     * The name of the attribute.
     */
    public String getAttributeName() {
        return attributeName;
    }

    /**
     * The role of the attribute in the search schema. Valid values:
     *
     * <ul>
     *   <li>
     *     <code>HASH</code> - A partition key that partitions the vector index for independent scaling. When
     *     specified, you must provide this attribute's value in the <code>SearchConditionExpression</code>.
     *   </li>
     *   <li>
     *     <code>INLINE_FILTER</code> - An attribute projected into the vector index for filtering at the storage
     *     layer during search. Inline filters are optional in the <code>SearchConditionExpression</code>.
     *   </li>
     * </ul>
     */
    public SearchSchemaElementType getSearchSchemaElementType() {
        return searchSchemaElementType;
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
        SearchSchemaElement that = (SearchSchemaElement) other;
        return Objects.equals(this.attributeName, that.attributeName)
               && Objects.equals(this.searchSchemaElementType, that.searchSchemaElementType);
    }

    @Override
    public int hashCode() {
        int $hc = Objects.hashCode(attributeName);
        $hc = 31 * $hc + Objects.hashCode(searchSchemaElementType);
        return $hc;
    }

    @Override
    public Schema schema() {
        return $SCHEMA;
    }

    @Override
    public void serializeMembers(ShapeSerializer serializer) {
        serializer.writeString($SCHEMA_ATTRIBUTE_NAME, attributeName);
        serializer.writeString($SCHEMA_SEARCH_SCHEMA_ELEMENT_TYPE, searchSchemaElementType.getValue());
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T getMemberValue(Schema member) {
        return switch (member.memberIndex()) {
            case 0 -> (T) SchemaUtils.validateSameMember($SCHEMA_ATTRIBUTE_NAME, member, attributeName);
            case 1 -> (T) SchemaUtils.validateSameMember($SCHEMA_SEARCH_SCHEMA_ELEMENT_TYPE, member, searchSchemaElementType);
            default -> throw new IllegalArgumentException("Attempted to get non-existent member: " + member.id());
        };
    }

    /**
     * Create a new builder containing all the current property values of this object.
     *
     * <p><strong>Note:</strong> This method performs only a shallow copy of the original properties.
     *
     * @return a builder for {@link SearchSchemaElement}.
     */
    public Builder toBuilder() {
        var builder = new Builder();
        builder.attributeName(this.attributeName);
        builder.searchSchemaElementType(this.searchSchemaElementType);
        return builder;
    }

    /**
     * @return returns a new Builder.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for {@link SearchSchemaElement}.
     */
    public static final class Builder implements ShapeBuilder<SearchSchemaElement> {
        private final PresenceTracker tracker = PresenceTracker.of($SCHEMA);
        private String attributeName;
        private SearchSchemaElementType searchSchemaElementType;

        private Builder() {}

        @Override
        public Schema schema() {
            return $SCHEMA;
        }

        /**
         * The name of the attribute.
         *
         * <p><strong>Required</strong>
         * @return this builder.
         */
        public Builder attributeName(String attributeName) {
            this.attributeName = Objects.requireNonNull(attributeName, "attributeName cannot be null");
            tracker.setMember($SCHEMA_ATTRIBUTE_NAME);
            return this;
        }

        /**
         * The role of the attribute in the search schema. Valid values:
         *
         * <ul>
         *   <li>
         *     <code>HASH</code> - A partition key that partitions the vector index for independent scaling. When
         *     specified, you must provide this attribute's value in the <code>SearchConditionExpression</code>.
         *   </li>
         *   <li>
         *     <code>INLINE_FILTER</code> - An attribute projected into the vector index for filtering at the storage
         *     layer during search. Inline filters are optional in the <code>SearchConditionExpression</code>.
         *   </li>
         * </ul>
         *
         * <p><strong>Required</strong>
         * @return this builder.
         */
        public Builder searchSchemaElementType(SearchSchemaElementType searchSchemaElementType) {
            this.searchSchemaElementType = Objects.requireNonNull(searchSchemaElementType, "searchSchemaElementType cannot be null");
            tracker.setMember($SCHEMA_SEARCH_SCHEMA_ELEMENT_TYPE);
            return this;
        }

        @Override
        public SearchSchemaElement build() {
            tracker.validate();
            return new SearchSchemaElement(this);
        }

        @Override
        @SuppressWarnings("unchecked")
        public void setMemberValue(Schema member, Object value) {
            switch (member.memberIndex()) {
                case 0 -> attributeName((String) SchemaUtils.validateSameMember($SCHEMA_ATTRIBUTE_NAME, member, value));
                case 1 -> searchSchemaElementType((SearchSchemaElementType) SchemaUtils.validateSameMember($SCHEMA_SEARCH_SCHEMA_ELEMENT_TYPE, member, value));
                default -> ShapeBuilder.super.setMemberValue(member, value);
            }
        }

        @Override
        public ShapeBuilder<SearchSchemaElement> errorCorrection() {
            if (tracker.allSet()) {
                return this;
            }
            if (!tracker.checkMember($SCHEMA_ATTRIBUTE_NAME)) {
                attributeName("");
            }
            if (!tracker.checkMember($SCHEMA_SEARCH_SCHEMA_ELEMENT_TYPE)) {
                searchSchemaElementType(SearchSchemaElementType.unknown(""));
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
                    case 0 -> builder.attributeName(de.readString(member));
                    case 1 -> builder.searchSchemaElementType(SearchSchemaElementType.builder().deserializeMember(de, member).build());
                    default -> throw new IllegalArgumentException("Unexpected member: " + member.memberName());
                }
            }
        }
    }
}
