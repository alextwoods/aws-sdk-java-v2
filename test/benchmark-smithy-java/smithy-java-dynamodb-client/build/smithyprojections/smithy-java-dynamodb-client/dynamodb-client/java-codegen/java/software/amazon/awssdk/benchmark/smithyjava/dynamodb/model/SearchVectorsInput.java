package software.amazon.awssdk.benchmark.smithyjava.dynamodb.model;

import java.util.Collections;
import java.util.List;
import java.util.Map;
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

@SmithyGenerated
public final class SearchVectorsInput implements SerializableStruct {

    public static final Schema $SCHEMA = Schemas.SEARCH_VECTORS_INPUT;
    private static final Schema $SCHEMA_TABLE_NAME = $SCHEMA.member("TableName");
    private static final Schema $SCHEMA_INDEX_NAME = $SCHEMA.member("IndexName");
    private static final Schema $SCHEMA_RETURN_CONSUMED_CAPACITY = $SCHEMA.member("ReturnConsumedCapacity");
    private static final Schema $SCHEMA_EXPRESSION_ATTRIBUTE_NAMES = $SCHEMA.member("ExpressionAttributeNames");
    private static final Schema $SCHEMA_EXPRESSION_ATTRIBUTE_VALUES = $SCHEMA.member("ExpressionAttributeValues");
    private static final Schema $SCHEMA_PROJECTION_EXPRESSION = $SCHEMA.member("ProjectionExpression");
    private static final Schema $SCHEMA_SEARCH_VECTOR = $SCHEMA.member("SearchVector");
    private static final Schema $SCHEMA_SEARCH_CONDITION_EXPRESSION = $SCHEMA.member("SearchConditionExpression");
    private static final Schema $SCHEMA_TOP_K = $SCHEMA.member("TopK");

    public static final ShapeId $ID = $SCHEMA.id();

    private final transient String tableName;
    private final transient String indexName;
    private final transient ReturnConsumedCapacity returnConsumedCapacity;
    private final transient Map<String, String> expressionAttributeNames;
    private final transient Map<String, AttributeValue> expressionAttributeValues;
    private final transient String projectionExpression;
    private final transient List<AttributeValue> searchVector;
    private final transient String searchConditionExpression;
    private final transient int topK;

    private SearchVectorsInput(Builder builder) {
        this.tableName = builder.tableName;
        this.indexName = builder.indexName;
        this.returnConsumedCapacity = builder.returnConsumedCapacity;
        this.expressionAttributeNames = builder.expressionAttributeNames == null ? null : Collections.unmodifiableMap(builder.expressionAttributeNames);
        this.expressionAttributeValues = builder.expressionAttributeValues == null ? null : Collections.unmodifiableMap(builder.expressionAttributeValues);
        this.projectionExpression = builder.projectionExpression;
        this.searchVector = Collections.unmodifiableList(builder.searchVector);
        this.searchConditionExpression = builder.searchConditionExpression;
        this.topK = builder.topK;
    }

    /**
     * The name or Amazon Resource Name (ARN) of the table containing the vector index.
     */
    public String getTableName() {
        return tableName;
    }

    /**
     * The name of the vector index to search. The index must be in the <code>ACTIVE</code> state.
     */
    public String getIndexName() {
        return indexName;
    }

    public ReturnConsumedCapacity getReturnConsumedCapacity() {
        return returnConsumedCapacity;
    }

    /**
     * One or more substitution tokens for attribute names in an expression. Use the <code>#</code> character in an
     * expression to dereference an attribute name.
     */
    public Map<String, String> getExpressionAttributeNames() {
        if (expressionAttributeNames == null) {
            return Collections.emptyMap();
        }
        return expressionAttributeNames;
    }

    public boolean hasExpressionAttributeNames() {
        return expressionAttributeNames != null;
    }

    /**
     * One or more values that can be substituted in an expression. Use the <code>:</code> character in an expression to
     * dereference an attribute value.
     */
    public Map<String, AttributeValue> getExpressionAttributeValues() {
        if (expressionAttributeValues == null) {
            return Collections.emptyMap();
        }
        return expressionAttributeValues;
    }

    public boolean hasExpressionAttributeValues() {
        return expressionAttributeValues != null;
    }

    /**
     * A string that identifies one or more attributes to retrieve from the index. Separate attribute names with commas.
     * If not specified, the operation returns all attributes projected into the vector index.
     *
     * <p>Only attributes projected into the vector index can be retrieved.
     */
    public String getProjectionExpression() {
        return projectionExpression;
    }

    /**
     * The search vector to compare against the indexed vectors. Each element is a 32-bit IEEE-754 floating point
     * number, provided in DynamoDB list format.
     *
     * <p>The number of dimensions must match the number of dimensions configured for the vector index.
     */
    public List<AttributeValue> getSearchVector() {
        return searchVector;
    }

    public boolean hasSearchVector() {
        return true;
    }

    /**
     * A condition expression used to filter the vector search results. The expression can reference attributes defined
     * in the vector index search schema, including <code>HASH</code> and <code>INLINE_FILTER</code> key elements.
     *
     * <p>Only the equality operator (<code>=</code>) is supported for <code>HASH</code> attributes. Comparison and
     * range operators are supported for <code>INLINE_FILTER</code> attributes. Only top-level attributes from the
     * search schema can be referenced.
     */
    public String getSearchConditionExpression() {
        return searchConditionExpression;
    }

    /**
     * The number of most similar results to return.
     */
    public int getTopK() {
        return topK;
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
        SearchVectorsInput that = (SearchVectorsInput) other;
        return this.topK == that.topK
               && Objects.equals(this.tableName, that.tableName)
               && Objects.equals(this.indexName, that.indexName)
               && Objects.equals(this.projectionExpression, that.projectionExpression)
               && Objects.equals(this.searchConditionExpression, that.searchConditionExpression)
               && Objects.equals(this.returnConsumedCapacity, that.returnConsumedCapacity)
               && Objects.equals(this.searchVector, that.searchVector)
               && Objects.equals(this.expressionAttributeNames, that.expressionAttributeNames)
               && Objects.equals(this.expressionAttributeValues, that.expressionAttributeValues);
    }

    @Override
    public int hashCode() {
        int $hc = Objects.hashCode(tableName);
        $hc = 31 * $hc + Objects.hashCode(indexName);
        $hc = 31 * $hc + Objects.hashCode(returnConsumedCapacity);
        $hc = 31 * $hc + Objects.hashCode(expressionAttributeNames);
        $hc = 31 * $hc + Objects.hashCode(expressionAttributeValues);
        $hc = 31 * $hc + Objects.hashCode(projectionExpression);
        $hc = 31 * $hc + Objects.hashCode(searchVector);
        $hc = 31 * $hc + Objects.hashCode(searchConditionExpression);
        $hc = 31 * $hc + Integer.hashCode(topK);
        return $hc;
    }

    @Override
    public Schema schema() {
        return $SCHEMA;
    }

    @Override
    public void serializeMembers(ShapeSerializer serializer) {
        serializer.writeString($SCHEMA_TABLE_NAME, tableName);
        serializer.writeString($SCHEMA_INDEX_NAME, indexName);
        if (returnConsumedCapacity != null) {
            serializer.writeString($SCHEMA_RETURN_CONSUMED_CAPACITY, returnConsumedCapacity.getValue());
        }
        if (expressionAttributeNames != null) {
            serializer.writeMap($SCHEMA_EXPRESSION_ATTRIBUTE_NAMES, expressionAttributeNames, expressionAttributeNames.size(), SharedSerde.ExpressionAttributeNameMapSerializer.INSTANCE);
        }
        if (expressionAttributeValues != null) {
            serializer.writeMap($SCHEMA_EXPRESSION_ATTRIBUTE_VALUES, expressionAttributeValues, expressionAttributeValues.size(), SharedSerde.ExpressionAttributeValueMapSerializer.INSTANCE);
        }
        if (projectionExpression != null) {
            serializer.writeString($SCHEMA_PROJECTION_EXPRESSION, projectionExpression);
        }
        serializer.writeList($SCHEMA_SEARCH_VECTOR, searchVector, searchVector.size(), SharedSerde.SearchVectorListSerializer.INSTANCE);
        if (searchConditionExpression != null) {
            serializer.writeString($SCHEMA_SEARCH_CONDITION_EXPRESSION, searchConditionExpression);
        }
        serializer.writeInteger($SCHEMA_TOP_K, topK);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T getMemberValue(Schema member) {
        return switch (member.memberIndex()) {
            case 0 -> (T) SchemaUtils.validateSameMember($SCHEMA_TABLE_NAME, member, tableName);
            case 1 -> (T) SchemaUtils.validateSameMember($SCHEMA_INDEX_NAME, member, indexName);
            case 2 -> (T) SchemaUtils.validateSameMember($SCHEMA_SEARCH_VECTOR, member, searchVector);
            case 3 -> (T) SchemaUtils.validateSameMember($SCHEMA_TOP_K, member, topK);
            case 4 -> (T) SchemaUtils.validateSameMember($SCHEMA_RETURN_CONSUMED_CAPACITY, member, returnConsumedCapacity);
            case 5 -> (T) SchemaUtils.validateSameMember($SCHEMA_EXPRESSION_ATTRIBUTE_NAMES, member, expressionAttributeNames);
            case 6 -> (T) SchemaUtils.validateSameMember($SCHEMA_EXPRESSION_ATTRIBUTE_VALUES, member, expressionAttributeValues);
            case 7 -> (T) SchemaUtils.validateSameMember($SCHEMA_PROJECTION_EXPRESSION, member, projectionExpression);
            case 8 -> (T) SchemaUtils.validateSameMember($SCHEMA_SEARCH_CONDITION_EXPRESSION, member, searchConditionExpression);
            default -> throw new IllegalArgumentException("Attempted to get non-existent member: " + member.id());
        };
    }

    /**
     * Create a new builder containing all the current property values of this object.
     *
     * <p><strong>Note:</strong> This method performs only a shallow copy of the original properties.
     *
     * @return a builder for {@link SearchVectorsInput}.
     */
    public Builder toBuilder() {
        var builder = new Builder();
        builder.tableName(this.tableName);
        builder.indexName(this.indexName);
        builder.returnConsumedCapacity(this.returnConsumedCapacity);
        builder.expressionAttributeNames(this.expressionAttributeNames);
        builder.expressionAttributeValues(this.expressionAttributeValues);
        builder.projectionExpression(this.projectionExpression);
        builder.searchVector(this.searchVector);
        builder.searchConditionExpression(this.searchConditionExpression);
        builder.topK(this.topK);
        return builder;
    }

    /**
     * @return returns a new Builder.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for {@link SearchVectorsInput}.
     */
    public static final class Builder implements ShapeBuilder<SearchVectorsInput> {
        private final PresenceTracker tracker = PresenceTracker.of($SCHEMA);
        private String tableName;
        private String indexName;
        private ReturnConsumedCapacity returnConsumedCapacity;
        private Map<String, String> expressionAttributeNames;
        private Map<String, AttributeValue> expressionAttributeValues;
        private String projectionExpression;
        private List<AttributeValue> searchVector;
        private String searchConditionExpression;
        private int topK;

        private Builder() {}

        @Override
        public Schema schema() {
            return $SCHEMA;
        }

        /**
         * The name or Amazon Resource Name (ARN) of the table containing the vector index.
         *
         * <p><strong>Required</strong>
         * @return this builder.
         */
        public Builder tableName(String tableName) {
            this.tableName = Objects.requireNonNull(tableName, "tableName cannot be null");
            tracker.setMember($SCHEMA_TABLE_NAME);
            return this;
        }

        /**
         * The name of the vector index to search. The index must be in the <code>ACTIVE</code> state.
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
         * @return this builder.
         */
        public Builder returnConsumedCapacity(ReturnConsumedCapacity returnConsumedCapacity) {
            this.returnConsumedCapacity = returnConsumedCapacity;
            return this;
        }

        /**
         * One or more substitution tokens for attribute names in an expression. Use the <code>#</code> character in an
         * expression to dereference an attribute name.
         *
         * @return this builder.
         */
        public Builder expressionAttributeNames(Map<String, String> expressionAttributeNames) {
            this.expressionAttributeNames = expressionAttributeNames;
            return this;
        }

        /**
         * One or more values that can be substituted in an expression. Use the <code>:</code> character in an expression to
         * dereference an attribute value.
         *
         * @return this builder.
         */
        public Builder expressionAttributeValues(Map<String, AttributeValue> expressionAttributeValues) {
            this.expressionAttributeValues = expressionAttributeValues;
            return this;
        }

        /**
         * A string that identifies one or more attributes to retrieve from the index. Separate attribute names with commas.
         * If not specified, the operation returns all attributes projected into the vector index.
         *
         * <p>Only attributes projected into the vector index can be retrieved.
         *
         * @return this builder.
         */
        public Builder projectionExpression(String projectionExpression) {
            this.projectionExpression = projectionExpression;
            return this;
        }

        /**
         * The search vector to compare against the indexed vectors. Each element is a 32-bit IEEE-754 floating point
         * number, provided in DynamoDB list format.
         *
         * <p>The number of dimensions must match the number of dimensions configured for the vector index.
         *
         * <p><strong>Required</strong>
         * @return this builder.
         */
        public Builder searchVector(List<AttributeValue> searchVector) {
            this.searchVector = Objects.requireNonNull(searchVector, "searchVector cannot be null");
            tracker.setMember($SCHEMA_SEARCH_VECTOR);
            return this;
        }

        /**
         * A condition expression used to filter the vector search results. The expression can reference attributes defined
         * in the vector index search schema, including <code>HASH</code> and <code>INLINE_FILTER</code> key elements.
         *
         * <p>Only the equality operator (<code>=</code>) is supported for <code>HASH</code> attributes. Comparison and
         * range operators are supported for <code>INLINE_FILTER</code> attributes. Only top-level attributes from the
         * search schema can be referenced.
         *
         * @return this builder.
         */
        public Builder searchConditionExpression(String searchConditionExpression) {
            this.searchConditionExpression = searchConditionExpression;
            return this;
        }

        /**
         * The number of most similar results to return.
         *
         * <p><strong>Required</strong>
         * @return this builder.
         */
        public Builder topK(int topK) {
            this.topK = topK;
            tracker.setMember($SCHEMA_TOP_K);
            return this;
        }

        @Override
        public SearchVectorsInput build() {
            tracker.validate();
            return new SearchVectorsInput(this);
        }

        @Override
        @SuppressWarnings("unchecked")
        public void setMemberValue(Schema member, Object value) {
            switch (member.memberIndex()) {
                case 0 -> tableName((String) SchemaUtils.validateSameMember($SCHEMA_TABLE_NAME, member, value));
                case 1 -> indexName((String) SchemaUtils.validateSameMember($SCHEMA_INDEX_NAME, member, value));
                case 2 -> searchVector((List<AttributeValue>) SchemaUtils.validateSameMember($SCHEMA_SEARCH_VECTOR, member, value));
                case 3 -> topK((int) SchemaUtils.validateSameMember($SCHEMA_TOP_K, member, value));
                case 4 -> returnConsumedCapacity((ReturnConsumedCapacity) SchemaUtils.validateSameMember($SCHEMA_RETURN_CONSUMED_CAPACITY, member, value));
                case 5 -> expressionAttributeNames((Map<String, String>) SchemaUtils.validateSameMember($SCHEMA_EXPRESSION_ATTRIBUTE_NAMES, member, value));
                case 6 -> expressionAttributeValues((Map<String, AttributeValue>) SchemaUtils.validateSameMember($SCHEMA_EXPRESSION_ATTRIBUTE_VALUES, member, value));
                case 7 -> projectionExpression((String) SchemaUtils.validateSameMember($SCHEMA_PROJECTION_EXPRESSION, member, value));
                case 8 -> searchConditionExpression((String) SchemaUtils.validateSameMember($SCHEMA_SEARCH_CONDITION_EXPRESSION, member, value));
                default -> ShapeBuilder.super.setMemberValue(member, value);
            }
        }

        @Override
        public ShapeBuilder<SearchVectorsInput> errorCorrection() {
            if (tracker.allSet()) {
                return this;
            }
            if (!tracker.checkMember($SCHEMA_TABLE_NAME)) {
                tableName("");
            }
            if (!tracker.checkMember($SCHEMA_INDEX_NAME)) {
                indexName("");
            }
            if (!tracker.checkMember($SCHEMA_SEARCH_VECTOR)) {
                searchVector(Collections.emptyList());
            }
            if (!tracker.checkMember($SCHEMA_TOP_K)) {
                tracker.setMember($SCHEMA_TOP_K);
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
                    case 0 -> builder.tableName(de.readString(member));
                    case 1 -> builder.indexName(de.readString(member));
                    case 2 -> builder.searchVector(SharedSerde.deserializeSearchVectorList(member, de));
                    case 3 -> builder.topK(de.readInteger(member));
                    case 4 -> builder.returnConsumedCapacity(ReturnConsumedCapacity.builder().deserializeMember(de, member).build());
                    case 5 -> builder.expressionAttributeNames(SharedSerde.deserializeExpressionAttributeNameMap(member, de));
                    case 6 -> builder.expressionAttributeValues(SharedSerde.deserializeExpressionAttributeValueMap(member, de));
                    case 7 -> builder.projectionExpression(de.readString(member));
                    case 8 -> builder.searchConditionExpression(de.readString(member));
                    default -> throw new IllegalArgumentException("Unexpected member: " + member.memberName());
                }
            }
        }
    }
}
