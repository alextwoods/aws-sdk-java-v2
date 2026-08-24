package software.amazon.awssdk.benchmark.smithyjava.dynamodb.model;

import java.util.Collections;
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

/**
 * Represents the input of a <code>DeleteItem</code> operation.
 */
@SmithyGenerated
public final class DeleteItemInput implements SerializableStruct {

    public static final Schema $SCHEMA = Schemas.DELETE_ITEM_INPUT;
    private static final Schema $SCHEMA_TABLE_NAME = $SCHEMA.member("TableName");
    private static final Schema $SCHEMA_KEY = $SCHEMA.member("Key");
    private static final Schema $SCHEMA_EXPECTED = $SCHEMA.member("Expected");
    private static final Schema $SCHEMA_CONDITIONAL_OPERATOR = $SCHEMA.member("ConditionalOperator");
    private static final Schema $SCHEMA_RETURN_VALUES = $SCHEMA.member("ReturnValues");
    private static final Schema $SCHEMA_RETURN_CONSUMED_CAPACITY = $SCHEMA.member("ReturnConsumedCapacity");
    private static final Schema $SCHEMA_RETURN_ITEM_COLLECTION_METRICS = $SCHEMA.member("ReturnItemCollectionMetrics");
    private static final Schema $SCHEMA_CONDITION_EXPRESSION = $SCHEMA.member("ConditionExpression");
    private static final Schema $SCHEMA_EXPRESSION_ATTRIBUTE_NAMES = $SCHEMA.member("ExpressionAttributeNames");
    private static final Schema $SCHEMA_EXPRESSION_ATTRIBUTE_VALUES = $SCHEMA.member("ExpressionAttributeValues");
    private static final Schema $SCHEMA_RETURN_VALUES_ON_CONDITION_CHECK_FAILURE = $SCHEMA.member("ReturnValuesOnConditionCheckFailure");

    public static final ShapeId $ID = $SCHEMA.id();

    private final transient String tableName;
    private final transient Map<String, AttributeValue> key;
    private final transient Map<String, ExpectedAttributeValue> expected;
    private final transient ConditionalOperator conditionalOperator;
    private final transient ReturnValue returnValues;
    private final transient ReturnConsumedCapacity returnConsumedCapacity;
    private final transient ReturnItemCollectionMetrics returnItemCollectionMetrics;
    private final transient String conditionExpression;
    private final transient Map<String, String> expressionAttributeNames;
    private final transient Map<String, AttributeValue> expressionAttributeValues;
    private final transient ReturnValuesOnConditionCheckFailure returnValuesOnConditionCheckFailure;

    private DeleteItemInput(Builder builder) {
        this.tableName = builder.tableName;
        this.key = Collections.unmodifiableMap(builder.key);
        this.expected = builder.expected == null ? null : Collections.unmodifiableMap(builder.expected);
        this.conditionalOperator = builder.conditionalOperator;
        this.returnValues = builder.returnValues;
        this.returnConsumedCapacity = builder.returnConsumedCapacity;
        this.returnItemCollectionMetrics = builder.returnItemCollectionMetrics;
        this.conditionExpression = builder.conditionExpression;
        this.expressionAttributeNames = builder.expressionAttributeNames == null ? null : Collections.unmodifiableMap(builder.expressionAttributeNames);
        this.expressionAttributeValues = builder.expressionAttributeValues == null ? null : Collections.unmodifiableMap(builder.expressionAttributeValues);
        this.returnValuesOnConditionCheckFailure = builder.returnValuesOnConditionCheckFailure;
    }

    /**
     * The name of the table from which to delete the item. You can also provide the Amazon Resource Name (ARN) of the
     * table in this parameter.
     */
    public String getTableName() {
        return tableName;
    }

    /**
     * A map of attribute names to <code>AttributeValue</code> objects, representing the primary key of the item to
     * delete.
     *
     * <p>For the primary key, you must provide all of the key attributes. For example, with a simple primary key, you
     * only need to provide a value for the partition key. For a composite primary key, you must provide values for both
     * the partition key and the sort key.
     */
    public Map<String, AttributeValue> getKey() {
        return key;
    }

    public boolean hasKey() {
        return true;
    }

    /**
     * This is a legacy parameter. Use <code>ConditionExpression</code> instead. For more information, see <a href="https://docs.aws.amazon.com/amazondynamodb/latest/developerguide/LegacyConditionalParameters.Expected.html">Expected</a>
     * in the <i>Amazon DynamoDB Developer Guide</i>.
     */
    public Map<String, ExpectedAttributeValue> getExpected() {
        if (expected == null) {
            return Collections.emptyMap();
        }
        return expected;
    }

    public boolean hasExpected() {
        return expected != null;
    }

    /**
     * This is a legacy parameter. Use <code>ConditionExpression</code> instead. For more information, see <a href="https://docs.aws.amazon.com/amazondynamodb/latest/developerguide/LegacyConditionalParameters.ConditionalOperator.html">
     * ConditionalOperator</a> in the <i>Amazon DynamoDB Developer Guide</i>.
     */
    public ConditionalOperator getConditionalOperator() {
        return conditionalOperator;
    }

    /**
     * Use <code>ReturnValues</code> if you want to get the item attributes as they appeared before they were deleted.
     * For <code>DeleteItem</code>, the valid values are:
     *
     * <ul>
     *   <li>
     *     <code>NONE</code> - If <code>ReturnValues</code> is not specified, or if its value is <code>NONE</code>,
     *     then nothing is returned. (This setting is the default for <code>ReturnValues</code>.)
     *   </li>
     *   <li>
     *     <code>ALL_OLD</code> - The content of the old item is returned.
     *   </li>
     * </ul>
     *
     * <p>There is no additional cost associated with requesting a return value aside from the small network and
     * processing overhead of receiving a larger response. No read capacity units are consumed.
     *
     * <p>The <code>ReturnValues</code> parameter is used by several DynamoDB operations; however, <code>DeleteItem</code>
     * does not recognize any values other than <code>NONE</code> or <code>ALL_OLD</code>.
     */
    public ReturnValue getReturnValues() {
        return returnValues;
    }

    public ReturnConsumedCapacity getReturnConsumedCapacity() {
        return returnConsumedCapacity;
    }

    /**
     * Determines whether item collection metrics are returned. If set to <code>SIZE</code>, the response includes
     * statistics about item collections, if any, that were modified during the operation are returned in the response.
     * If set to <code>NONE</code> (the default), no statistics are returned.
     */
    public ReturnItemCollectionMetrics getReturnItemCollectionMetrics() {
        return returnItemCollectionMetrics;
    }

    /**
     * A condition that must be satisfied in order for a conditional <code>DeleteItem</code> to succeed.
     *
     * <p>An expression can contain any of the following:
     *
     * <ul>
     *   <li>
     *     Functions: <code>attribute_exists | attribute_not_exists | attribute_type | contains | begins_with | size</code>
     *     These function names are case-sensitive.
     *   </li>
     *   <li>
     *     Comparison operators: <code>= | &lt;&gt; | &lt; | &gt; | &lt;= | &gt;= | BETWEEN | IN </code>
     *   </li>
     *   <li>
     *      Logical operators: <code>AND | OR | NOT</code>
     *   </li>
     * </ul>
     *
     * <p>For more information about condition expressions, see <a href="https://docs.aws.amazon.com/amazondynamodb/latest/developerguide/Expressions.SpecifyingConditions.html">Condition Expressions</a> in the <i>Amazon DynamoDB
     * Developer Guide</i>.
     */
    public String getConditionExpression() {
        return conditionExpression;
    }

    /**
     * One or more substitution tokens for attribute names in an expression. The following are some use cases for using <code>
     * ExpressionAttributeNames</code>:
     *
     * <ul>
     *   <li>
     *     To access an attribute whose name conflicts with a DynamoDB reserved word.
     *   </li>
     *   <li>
     *     To create a placeholder for repeating occurrences of an attribute name in an expression.
     *   </li>
     *   <li>
     *     To prevent special characters in an attribute name from being misinterpreted in an expression.
     *   </li>
     * </ul>
     *
     * <p>Use the <b>#</b> character in an expression to dereference an attribute name. For example, consider the
     * following attribute name:
     *
     * <ul>
     *   <li>
     *     <code>Percentile</code>
     *   </li>
     * </ul>
     *
     * <p>The name of this attribute conflicts with a reserved word, so it cannot be used directly in an expression.
     * (For the complete list of reserved words, see <a href="https://docs.aws.amazon.com/amazondynamodb/latest/developerguide/ReservedWords.html">Reserved Words</a> in the <i>Amazon DynamoDB Developer Guide</i>
     * ). To work around this, you could specify the following for <code>ExpressionAttributeNames</code>:
     *
     * <ul>
     *   <li>
     *     <code>{"#P":"Percentile"}</code>
     *   </li>
     * </ul>
     *
     * <p>You could then use this substitution in an expression, as in this example:
     *
     * <ul>
     *   <li>
     *     <code>#P = :val</code>
     *   </li>
     * </ul>
     *
     * <p>Tokens that begin with the <b>:</b> character are <i>expression attribute values</i>, which are placeholders
     * for the actual value at runtime.
     *
     * <p>For more information on expression attribute names, see <a href="https://docs.aws.amazon.com/amazondynamodb/latest/developerguide/Expressions.AccessingItemAttributes.html">Specifying Item Attributes</a> in the <i>Amazon
     * DynamoDB Developer Guide</i>.
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
     * One or more values that can be substituted in an expression.
     *
     * <p>Use the <b>:</b> (colon) character in an expression to dereference an attribute value. For example, suppose
     * that you wanted to check whether the value of the <i>ProductStatus</i> attribute was one of the following:
     *
     * <p><code>Available | Backordered | Discontinued</code>
     *
     * <p>You would first need to specify <code>ExpressionAttributeValues</code> as follows:
     *
     * <p><code>{ ":avail":{"S":"Available"}, ":back":{"S":"Backordered"}, ":disc":{"S":"Discontinued"} }</code>
     *
     * <p>You could then use these values in an expression, such as this:
     *
     * <p><code>ProductStatus IN (:avail, :back, :disc)</code>
     *
     * <p>For more information on expression attribute values, see <a href="https://docs.aws.amazon.com/amazondynamodb/latest/developerguide/Expressions.SpecifyingConditions.html">Condition Expressions</a> in the <i>Amazon
     * DynamoDB Developer Guide</i>.
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
     * An optional parameter that returns the item attributes for a <code>DeleteItem</code> operation that failed a
     * condition check.
     *
     * <p>There is no additional cost associated with requesting a return value aside from the small network and
     * processing overhead of receiving a larger response. No read capacity units are consumed.
     */
    public ReturnValuesOnConditionCheckFailure getReturnValuesOnConditionCheckFailure() {
        return returnValuesOnConditionCheckFailure;
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
        DeleteItemInput that = (DeleteItemInput) other;
        return Objects.equals(this.tableName, that.tableName)
               && Objects.equals(this.conditionExpression, that.conditionExpression)
               && Objects.equals(this.conditionalOperator, that.conditionalOperator)
               && Objects.equals(this.returnValues, that.returnValues)
               && Objects.equals(this.returnConsumedCapacity, that.returnConsumedCapacity)
               && Objects.equals(this.returnItemCollectionMetrics, that.returnItemCollectionMetrics)
               && Objects.equals(this.returnValuesOnConditionCheckFailure, that.returnValuesOnConditionCheckFailure)
               && Objects.equals(this.key, that.key)
               && Objects.equals(this.expected, that.expected)
               && Objects.equals(this.expressionAttributeNames, that.expressionAttributeNames)
               && Objects.equals(this.expressionAttributeValues, that.expressionAttributeValues);
    }

    @Override
    public int hashCode() {
        int $hc = Objects.hashCode(tableName);
        $hc = 31 * $hc + Objects.hashCode(key);
        $hc = 31 * $hc + Objects.hashCode(expected);
        $hc = 31 * $hc + Objects.hashCode(conditionalOperator);
        $hc = 31 * $hc + Objects.hashCode(returnValues);
        $hc = 31 * $hc + Objects.hashCode(returnConsumedCapacity);
        $hc = 31 * $hc + Objects.hashCode(returnItemCollectionMetrics);
        $hc = 31 * $hc + Objects.hashCode(conditionExpression);
        $hc = 31 * $hc + Objects.hashCode(expressionAttributeNames);
        $hc = 31 * $hc + Objects.hashCode(expressionAttributeValues);
        $hc = 31 * $hc + Objects.hashCode(returnValuesOnConditionCheckFailure);
        return $hc;
    }

    @Override
    public Schema schema() {
        return $SCHEMA;
    }

    @Override
    public void serializeMembers(ShapeSerializer serializer) {
        serializer.writeString($SCHEMA_TABLE_NAME, tableName);
        serializer.writeMap($SCHEMA_KEY, key, key.size(), SharedSerde.KeySerializer.INSTANCE);
        if (expected != null) {
            serializer.writeMap($SCHEMA_EXPECTED, expected, expected.size(), SharedSerde.ExpectedAttributeMapSerializer.INSTANCE);
        }
        if (conditionalOperator != null) {
            serializer.writeString($SCHEMA_CONDITIONAL_OPERATOR, conditionalOperator.getValue());
        }
        if (returnValues != null) {
            serializer.writeString($SCHEMA_RETURN_VALUES, returnValues.getValue());
        }
        if (returnConsumedCapacity != null) {
            serializer.writeString($SCHEMA_RETURN_CONSUMED_CAPACITY, returnConsumedCapacity.getValue());
        }
        if (returnItemCollectionMetrics != null) {
            serializer.writeString($SCHEMA_RETURN_ITEM_COLLECTION_METRICS, returnItemCollectionMetrics.getValue());
        }
        if (conditionExpression != null) {
            serializer.writeString($SCHEMA_CONDITION_EXPRESSION, conditionExpression);
        }
        if (expressionAttributeNames != null) {
            serializer.writeMap($SCHEMA_EXPRESSION_ATTRIBUTE_NAMES, expressionAttributeNames, expressionAttributeNames.size(), SharedSerde.ExpressionAttributeNameMapSerializer.INSTANCE);
        }
        if (expressionAttributeValues != null) {
            serializer.writeMap($SCHEMA_EXPRESSION_ATTRIBUTE_VALUES, expressionAttributeValues, expressionAttributeValues.size(), SharedSerde.ExpressionAttributeValueMapSerializer.INSTANCE);
        }
        if (returnValuesOnConditionCheckFailure != null) {
            serializer.writeString($SCHEMA_RETURN_VALUES_ON_CONDITION_CHECK_FAILURE, returnValuesOnConditionCheckFailure.getValue());
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T getMemberValue(Schema member) {
        return switch (member.memberIndex()) {
            case 0 -> (T) SchemaUtils.validateSameMember($SCHEMA_TABLE_NAME, member, tableName);
            case 1 -> (T) SchemaUtils.validateSameMember($SCHEMA_KEY, member, key);
            case 2 -> (T) SchemaUtils.validateSameMember($SCHEMA_EXPECTED, member, expected);
            case 3 -> (T) SchemaUtils.validateSameMember($SCHEMA_CONDITIONAL_OPERATOR, member, conditionalOperator);
            case 4 -> (T) SchemaUtils.validateSameMember($SCHEMA_RETURN_VALUES, member, returnValues);
            case 5 -> (T) SchemaUtils.validateSameMember($SCHEMA_RETURN_CONSUMED_CAPACITY, member, returnConsumedCapacity);
            case 6 -> (T) SchemaUtils.validateSameMember($SCHEMA_RETURN_ITEM_COLLECTION_METRICS, member, returnItemCollectionMetrics);
            case 7 -> (T) SchemaUtils.validateSameMember($SCHEMA_CONDITION_EXPRESSION, member, conditionExpression);
            case 8 -> (T) SchemaUtils.validateSameMember($SCHEMA_EXPRESSION_ATTRIBUTE_NAMES, member, expressionAttributeNames);
            case 9 -> (T) SchemaUtils.validateSameMember($SCHEMA_EXPRESSION_ATTRIBUTE_VALUES, member, expressionAttributeValues);
            case 10 -> (T) SchemaUtils.validateSameMember($SCHEMA_RETURN_VALUES_ON_CONDITION_CHECK_FAILURE, member, returnValuesOnConditionCheckFailure);
            default -> throw new IllegalArgumentException("Attempted to get non-existent member: " + member.id());
        };
    }

    /**
     * Create a new builder containing all the current property values of this object.
     *
     * <p><strong>Note:</strong> This method performs only a shallow copy of the original properties.
     *
     * @return a builder for {@link DeleteItemInput}.
     */
    public Builder toBuilder() {
        var builder = new Builder();
        builder.tableName(this.tableName);
        builder.key(this.key);
        builder.expected(this.expected);
        builder.conditionalOperator(this.conditionalOperator);
        builder.returnValues(this.returnValues);
        builder.returnConsumedCapacity(this.returnConsumedCapacity);
        builder.returnItemCollectionMetrics(this.returnItemCollectionMetrics);
        builder.conditionExpression(this.conditionExpression);
        builder.expressionAttributeNames(this.expressionAttributeNames);
        builder.expressionAttributeValues(this.expressionAttributeValues);
        builder.returnValuesOnConditionCheckFailure(this.returnValuesOnConditionCheckFailure);
        return builder;
    }

    /**
     * @return returns a new Builder.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for {@link DeleteItemInput}.
     */
    public static final class Builder implements ShapeBuilder<DeleteItemInput> {
        private final PresenceTracker tracker = PresenceTracker.of($SCHEMA);
        private String tableName;
        private Map<String, AttributeValue> key;
        private Map<String, ExpectedAttributeValue> expected;
        private ConditionalOperator conditionalOperator;
        private ReturnValue returnValues;
        private ReturnConsumedCapacity returnConsumedCapacity;
        private ReturnItemCollectionMetrics returnItemCollectionMetrics;
        private String conditionExpression;
        private Map<String, String> expressionAttributeNames;
        private Map<String, AttributeValue> expressionAttributeValues;
        private ReturnValuesOnConditionCheckFailure returnValuesOnConditionCheckFailure;

        private Builder() {}

        @Override
        public Schema schema() {
            return $SCHEMA;
        }

        /**
         * The name of the table from which to delete the item. You can also provide the Amazon Resource Name (ARN) of the
         * table in this parameter.
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
         * A map of attribute names to <code>AttributeValue</code> objects, representing the primary key of the item to
         * delete.
         *
         * <p>For the primary key, you must provide all of the key attributes. For example, with a simple primary key, you
         * only need to provide a value for the partition key. For a composite primary key, you must provide values for both
         * the partition key and the sort key.
         *
         * <p><strong>Required</strong>
         * @return this builder.
         */
        public Builder key(Map<String, AttributeValue> key) {
            this.key = Objects.requireNonNull(key, "key cannot be null");
            tracker.setMember($SCHEMA_KEY);
            return this;
        }

        /**
         * This is a legacy parameter. Use <code>ConditionExpression</code> instead. For more information, see <a href="https://docs.aws.amazon.com/amazondynamodb/latest/developerguide/LegacyConditionalParameters.Expected.html">Expected</a>
         * in the <i>Amazon DynamoDB Developer Guide</i>.
         *
         * @return this builder.
         */
        public Builder expected(Map<String, ExpectedAttributeValue> expected) {
            this.expected = expected;
            return this;
        }

        /**
         * This is a legacy parameter. Use <code>ConditionExpression</code> instead. For more information, see <a href="https://docs.aws.amazon.com/amazondynamodb/latest/developerguide/LegacyConditionalParameters.ConditionalOperator.html">
         * ConditionalOperator</a> in the <i>Amazon DynamoDB Developer Guide</i>.
         *
         * @return this builder.
         */
        public Builder conditionalOperator(ConditionalOperator conditionalOperator) {
            this.conditionalOperator = conditionalOperator;
            return this;
        }

        /**
         * Use <code>ReturnValues</code> if you want to get the item attributes as they appeared before they were deleted.
         * For <code>DeleteItem</code>, the valid values are:
         *
         * <ul>
         *   <li>
         *     <code>NONE</code> - If <code>ReturnValues</code> is not specified, or if its value is <code>NONE</code>,
         *     then nothing is returned. (This setting is the default for <code>ReturnValues</code>.)
         *   </li>
         *   <li>
         *     <code>ALL_OLD</code> - The content of the old item is returned.
         *   </li>
         * </ul>
         *
         * <p>There is no additional cost associated with requesting a return value aside from the small network and
         * processing overhead of receiving a larger response. No read capacity units are consumed.
         *
         * <p>The <code>ReturnValues</code> parameter is used by several DynamoDB operations; however, <code>DeleteItem</code>
         * does not recognize any values other than <code>NONE</code> or <code>ALL_OLD</code>.
         *
         * @return this builder.
         */
        public Builder returnValues(ReturnValue returnValues) {
            this.returnValues = returnValues;
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
         * Determines whether item collection metrics are returned. If set to <code>SIZE</code>, the response includes
         * statistics about item collections, if any, that were modified during the operation are returned in the response.
         * If set to <code>NONE</code> (the default), no statistics are returned.
         *
         * @return this builder.
         */
        public Builder returnItemCollectionMetrics(ReturnItemCollectionMetrics returnItemCollectionMetrics) {
            this.returnItemCollectionMetrics = returnItemCollectionMetrics;
            return this;
        }

        /**
         * A condition that must be satisfied in order for a conditional <code>DeleteItem</code> to succeed.
         *
         * <p>An expression can contain any of the following:
         *
         * <ul>
         *   <li>
         *     Functions: <code>attribute_exists | attribute_not_exists | attribute_type | contains | begins_with | size</code>
         *     These function names are case-sensitive.
         *   </li>
         *   <li>
         *     Comparison operators: <code>= | &lt;&gt; | &lt; | &gt; | &lt;= | &gt;= | BETWEEN | IN </code>
         *   </li>
         *   <li>
         *      Logical operators: <code>AND | OR | NOT</code>
         *   </li>
         * </ul>
         *
         * <p>For more information about condition expressions, see <a href="https://docs.aws.amazon.com/amazondynamodb/latest/developerguide/Expressions.SpecifyingConditions.html">Condition Expressions</a> in the <i>Amazon DynamoDB
         * Developer Guide</i>.
         *
         * @return this builder.
         */
        public Builder conditionExpression(String conditionExpression) {
            this.conditionExpression = conditionExpression;
            return this;
        }

        /**
         * One or more substitution tokens for attribute names in an expression. The following are some use cases for using <code>
         * ExpressionAttributeNames</code>:
         *
         * <ul>
         *   <li>
         *     To access an attribute whose name conflicts with a DynamoDB reserved word.
         *   </li>
         *   <li>
         *     To create a placeholder for repeating occurrences of an attribute name in an expression.
         *   </li>
         *   <li>
         *     To prevent special characters in an attribute name from being misinterpreted in an expression.
         *   </li>
         * </ul>
         *
         * <p>Use the <b>#</b> character in an expression to dereference an attribute name. For example, consider the
         * following attribute name:
         *
         * <ul>
         *   <li>
         *     <code>Percentile</code>
         *   </li>
         * </ul>
         *
         * <p>The name of this attribute conflicts with a reserved word, so it cannot be used directly in an expression.
         * (For the complete list of reserved words, see <a href="https://docs.aws.amazon.com/amazondynamodb/latest/developerguide/ReservedWords.html">Reserved Words</a> in the <i>Amazon DynamoDB Developer Guide</i>
         * ). To work around this, you could specify the following for <code>ExpressionAttributeNames</code>:
         *
         * <ul>
         *   <li>
         *     <code>{"#P":"Percentile"}</code>
         *   </li>
         * </ul>
         *
         * <p>You could then use this substitution in an expression, as in this example:
         *
         * <ul>
         *   <li>
         *     <code>#P = :val</code>
         *   </li>
         * </ul>
         *
         * <p>Tokens that begin with the <b>:</b> character are <i>expression attribute values</i>, which are placeholders
         * for the actual value at runtime.
         *
         * <p>For more information on expression attribute names, see <a href="https://docs.aws.amazon.com/amazondynamodb/latest/developerguide/Expressions.AccessingItemAttributes.html">Specifying Item Attributes</a> in the <i>Amazon
         * DynamoDB Developer Guide</i>.
         *
         * @return this builder.
         */
        public Builder expressionAttributeNames(Map<String, String> expressionAttributeNames) {
            this.expressionAttributeNames = expressionAttributeNames;
            return this;
        }

        /**
         * One or more values that can be substituted in an expression.
         *
         * <p>Use the <b>:</b> (colon) character in an expression to dereference an attribute value. For example, suppose
         * that you wanted to check whether the value of the <i>ProductStatus</i> attribute was one of the following:
         *
         * <p><code>Available | Backordered | Discontinued</code>
         *
         * <p>You would first need to specify <code>ExpressionAttributeValues</code> as follows:
         *
         * <p><code>{ ":avail":{"S":"Available"}, ":back":{"S":"Backordered"}, ":disc":{"S":"Discontinued"} }</code>
         *
         * <p>You could then use these values in an expression, such as this:
         *
         * <p><code>ProductStatus IN (:avail, :back, :disc)</code>
         *
         * <p>For more information on expression attribute values, see <a href="https://docs.aws.amazon.com/amazondynamodb/latest/developerguide/Expressions.SpecifyingConditions.html">Condition Expressions</a> in the <i>Amazon
         * DynamoDB Developer Guide</i>.
         *
         * @return this builder.
         */
        public Builder expressionAttributeValues(Map<String, AttributeValue> expressionAttributeValues) {
            this.expressionAttributeValues = expressionAttributeValues;
            return this;
        }

        /**
         * An optional parameter that returns the item attributes for a <code>DeleteItem</code> operation that failed a
         * condition check.
         *
         * <p>There is no additional cost associated with requesting a return value aside from the small network and
         * processing overhead of receiving a larger response. No read capacity units are consumed.
         *
         * @return this builder.
         */
        public Builder returnValuesOnConditionCheckFailure(ReturnValuesOnConditionCheckFailure returnValuesOnConditionCheckFailure) {
            this.returnValuesOnConditionCheckFailure = returnValuesOnConditionCheckFailure;
            return this;
        }

        @Override
        public DeleteItemInput build() {
            tracker.validate();
            return new DeleteItemInput(this);
        }

        @Override
        @SuppressWarnings("unchecked")
        public void setMemberValue(Schema member, Object value) {
            switch (member.memberIndex()) {
                case 0 -> tableName((String) SchemaUtils.validateSameMember($SCHEMA_TABLE_NAME, member, value));
                case 1 -> key((Map<String, AttributeValue>) SchemaUtils.validateSameMember($SCHEMA_KEY, member, value));
                case 2 -> expected((Map<String, ExpectedAttributeValue>) SchemaUtils.validateSameMember($SCHEMA_EXPECTED, member, value));
                case 3 -> conditionalOperator((ConditionalOperator) SchemaUtils.validateSameMember($SCHEMA_CONDITIONAL_OPERATOR, member, value));
                case 4 -> returnValues((ReturnValue) SchemaUtils.validateSameMember($SCHEMA_RETURN_VALUES, member, value));
                case 5 -> returnConsumedCapacity((ReturnConsumedCapacity) SchemaUtils.validateSameMember($SCHEMA_RETURN_CONSUMED_CAPACITY, member, value));
                case 6 -> returnItemCollectionMetrics((ReturnItemCollectionMetrics) SchemaUtils.validateSameMember($SCHEMA_RETURN_ITEM_COLLECTION_METRICS, member, value));
                case 7 -> conditionExpression((String) SchemaUtils.validateSameMember($SCHEMA_CONDITION_EXPRESSION, member, value));
                case 8 -> expressionAttributeNames((Map<String, String>) SchemaUtils.validateSameMember($SCHEMA_EXPRESSION_ATTRIBUTE_NAMES, member, value));
                case 9 -> expressionAttributeValues((Map<String, AttributeValue>) SchemaUtils.validateSameMember($SCHEMA_EXPRESSION_ATTRIBUTE_VALUES, member, value));
                case 10 -> returnValuesOnConditionCheckFailure((ReturnValuesOnConditionCheckFailure) SchemaUtils.validateSameMember($SCHEMA_RETURN_VALUES_ON_CONDITION_CHECK_FAILURE, member, value));
                default -> ShapeBuilder.super.setMemberValue(member, value);
            }
        }

        @Override
        public ShapeBuilder<DeleteItemInput> errorCorrection() {
            if (tracker.allSet()) {
                return this;
            }
            if (!tracker.checkMember($SCHEMA_TABLE_NAME)) {
                tableName("");
            }
            if (!tracker.checkMember($SCHEMA_KEY)) {
                key(Collections.emptyMap());
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
                    case 1 -> builder.key(SharedSerde.deserializeKey(member, de));
                    case 2 -> builder.expected(SharedSerde.deserializeExpectedAttributeMap(member, de));
                    case 3 -> builder.conditionalOperator(ConditionalOperator.builder().deserializeMember(de, member).build());
                    case 4 -> builder.returnValues(ReturnValue.builder().deserializeMember(de, member).build());
                    case 5 -> builder.returnConsumedCapacity(ReturnConsumedCapacity.builder().deserializeMember(de, member).build());
                    case 6 -> builder.returnItemCollectionMetrics(ReturnItemCollectionMetrics.builder().deserializeMember(de, member).build());
                    case 7 -> builder.conditionExpression(de.readString(member));
                    case 8 -> builder.expressionAttributeNames(SharedSerde.deserializeExpressionAttributeNameMap(member, de));
                    case 9 -> builder.expressionAttributeValues(SharedSerde.deserializeExpressionAttributeValueMap(member, de));
                    case 10 -> builder.returnValuesOnConditionCheckFailure(ReturnValuesOnConditionCheckFailure.builder().deserializeMember(de, member).build());
                    default -> throw new IllegalArgumentException("Unexpected member: " + member.memberName());
                }
            }
        }
    }
}
