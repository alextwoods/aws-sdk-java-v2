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
 * Represents the input of an <code>UpdateItem</code> operation.
 */
@SmithyGenerated
public final class UpdateItemInput implements SerializableStruct {

    public static final Schema $SCHEMA = Schemas.UPDATE_ITEM_INPUT;
    private static final Schema $SCHEMA_TABLE_NAME = $SCHEMA.member("TableName");
    private static final Schema $SCHEMA_KEY = $SCHEMA.member("Key");
    private static final Schema $SCHEMA_ATTRIBUTE_UPDATES = $SCHEMA.member("AttributeUpdates");
    private static final Schema $SCHEMA_EXPECTED = $SCHEMA.member("Expected");
    private static final Schema $SCHEMA_CONDITIONAL_OPERATOR = $SCHEMA.member("ConditionalOperator");
    private static final Schema $SCHEMA_RETURN_VALUES = $SCHEMA.member("ReturnValues");
    private static final Schema $SCHEMA_RETURN_CONSUMED_CAPACITY = $SCHEMA.member("ReturnConsumedCapacity");
    private static final Schema $SCHEMA_RETURN_ITEM_COLLECTION_METRICS = $SCHEMA.member("ReturnItemCollectionMetrics");
    private static final Schema $SCHEMA_UPDATE_EXPRESSION = $SCHEMA.member("UpdateExpression");
    private static final Schema $SCHEMA_CONDITION_EXPRESSION = $SCHEMA.member("ConditionExpression");
    private static final Schema $SCHEMA_EXPRESSION_ATTRIBUTE_NAMES = $SCHEMA.member("ExpressionAttributeNames");
    private static final Schema $SCHEMA_EXPRESSION_ATTRIBUTE_VALUES = $SCHEMA.member("ExpressionAttributeValues");
    private static final Schema $SCHEMA_RETURN_VALUES_ON_CONDITION_CHECK_FAILURE = $SCHEMA.member("ReturnValuesOnConditionCheckFailure");

    public static final ShapeId $ID = $SCHEMA.id();

    private final transient String tableName;
    private final transient Map<String, AttributeValue> key;
    private final transient Map<String, AttributeValueUpdate> attributeUpdates;
    private final transient Map<String, ExpectedAttributeValue> expected;
    private final transient ConditionalOperator conditionalOperator;
    private final transient ReturnValue returnValues;
    private final transient ReturnConsumedCapacity returnConsumedCapacity;
    private final transient ReturnItemCollectionMetrics returnItemCollectionMetrics;
    private final transient String updateExpression;
    private final transient String conditionExpression;
    private final transient Map<String, String> expressionAttributeNames;
    private final transient Map<String, AttributeValue> expressionAttributeValues;
    private final transient ReturnValuesOnConditionCheckFailure returnValuesOnConditionCheckFailure;

    private UpdateItemInput(Builder builder) {
        this.tableName = builder.tableName;
        this.key = Collections.unmodifiableMap(builder.key);
        this.attributeUpdates = builder.attributeUpdates == null ? null : Collections.unmodifiableMap(builder.attributeUpdates);
        this.expected = builder.expected == null ? null : Collections.unmodifiableMap(builder.expected);
        this.conditionalOperator = builder.conditionalOperator;
        this.returnValues = builder.returnValues;
        this.returnConsumedCapacity = builder.returnConsumedCapacity;
        this.returnItemCollectionMetrics = builder.returnItemCollectionMetrics;
        this.updateExpression = builder.updateExpression;
        this.conditionExpression = builder.conditionExpression;
        this.expressionAttributeNames = builder.expressionAttributeNames == null ? null : Collections.unmodifiableMap(builder.expressionAttributeNames);
        this.expressionAttributeValues = builder.expressionAttributeValues == null ? null : Collections.unmodifiableMap(builder.expressionAttributeValues);
        this.returnValuesOnConditionCheckFailure = builder.returnValuesOnConditionCheckFailure;
    }

    /**
     * The name of the table containing the item to update. You can also provide the Amazon Resource Name (ARN) of the
     * table in this parameter.
     */
    public String getTableName() {
        return tableName;
    }

    /**
     * The primary key of the item to be updated. Each element consists of an attribute name and a value for that
     * attribute.
     *
     * <p>For the primary key, you must provide all of the attributes. For example, with a simple primary key, you only
     * need to provide a value for the partition key. For a composite primary key, you must provide values for both the
     * partition key and the sort key.
     */
    public Map<String, AttributeValue> getKey() {
        return key;
    }

    public boolean hasKey() {
        return true;
    }

    /**
     * This is a legacy parameter. Use <code>UpdateExpression</code> instead. For more information, see <a href="https://docs.aws.amazon.com/amazondynamodb/latest/developerguide/LegacyConditionalParameters.AttributeUpdates.html">
     * AttributeUpdates</a> in the <i>Amazon DynamoDB Developer Guide</i>.
     */
    public Map<String, AttributeValueUpdate> getAttributeUpdates() {
        if (attributeUpdates == null) {
            return Collections.emptyMap();
        }
        return attributeUpdates;
    }

    public boolean hasAttributeUpdates() {
        return attributeUpdates != null;
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
     * Use <code>ReturnValues</code> if you want to get the item attributes as they appear before or after they are
     * successfully updated. For <code>UpdateItem</code>, the valid values are:
     *
     * <ul>
     *   <li>
     *     <code>NONE</code> - If <code>ReturnValues</code> is not specified, or if its value is <code>NONE</code>,
     *     then nothing is returned. (This setting is the default for <code>ReturnValues</code>.)
     *   </li>
     *   <li>
     *     <code>ALL_OLD</code> - Returns all of the attributes of the item, as they appeared before the UpdateItem
     *     operation.
     *   </li>
     *   <li>
     *     <code>UPDATED_OLD</code> - Returns only the updated attributes, as they appeared before the UpdateItem
     *     operation.
     *   </li>
     *   <li>
     *     <code>ALL_NEW</code> - Returns all of the attributes of the item, as they appear after the UpdateItem
     *     operation.
     *   </li>
     *   <li>
     *     <code>UPDATED_NEW</code> - Returns only the updated attributes, as they appear after the UpdateItem
     *     operation.
     *   </li>
     * </ul>
     *
     * <p>There is no additional cost associated with requesting a return value aside from the small network and
     * processing overhead of receiving a larger response. No read capacity units are consumed.
     *
     * <p>The values returned are strongly consistent.
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
     * An expression that defines one or more attributes to be updated, the action to be performed on them, and new
     * values for them.
     *
     * <p>The following action values are available for <code>UpdateExpression</code>.
     *
     * <ul>
     *   <li>
     *     <code>SET</code> - Adds one or more attributes and values to an item. If any of these attributes already
     *     exist, they are replaced by the new values. You can also use <code>SET</code> to add or subtract from an
     *     attribute that is of type Number. For example: <code>SET myNum = myNum + :val</code><code>SET</code>
     *     supports the following functions:
     *
     *     <ul>
     *       <li>
     *         <code>if_not_exists (path, operand)</code> - if the item does not contain an attribute at the
     *         specified path, then <code>if_not_exists</code> evaluates to operand; otherwise, it evaluates to
     *         path. You can use this function to avoid overwriting an attribute that may already be present in
     *         the item.
     *       </li>
     *       <li>
     *         <code>list_append (operand, operand)</code> - evaluates to a list with a new element added to it.
     *         You can append the new element to the start or the end of the list by reversing the order of the
     *         operands.
     *       </li>
     *     </ul>These function names are case-sensitive.
     *   </li>
     *   <li>
     *     <code>REMOVE</code> - Removes one or more attributes from an item.
     *   </li>
     *   <li>
     *     <code>ADD</code> - Adds the specified value to the item, if the attribute does not already exist. If the
     *     attribute does exist, then the behavior of <code>ADD</code> depends on the data type of the attribute:
     *
     *     <ul>
     *       <li>
     *         If the existing attribute is a number, and if <code>Value</code> is also a number, then <code>
     *         Value</code> is mathematically added to the existing attribute. If <code>Value</code> is a
     *         negative number, then it is subtracted from the existing attribute.If you use <code>ADD</code> to
     *         increment or decrement a number value for an item that doesn't exist before the update, DynamoDB
     *         uses <code>0</code> as the initial value.Similarly, if you use <code>ADD</code> for an existing
     *         item to increment or decrement an attribute value that doesn't exist before the update, DynamoDB
     *         uses <code>0</code> as the initial value. For example, suppose that the item you want to update
     *         doesn't have an attribute named <code>itemcount</code>, but you decide to <code>ADD</code> the
     *         number <code>3</code> to this attribute anyway. DynamoDB will create the <code>itemcount</code>
     *         attribute, set its initial value to <code>0</code>, and finally add <code>3</code> to it. The
     *         result will be a new <code>itemcount</code> attribute in the item, with a value of <code>3</code>
     *         .
     *       </li>
     *       <li>
     *         If the existing data type is a set and if <code>Value</code> is also a set, then <code>Value</code>
     *         is added to the existing set. For example, if the attribute value is the set <code>[1,2]</code>,
     *         and the <code>ADD</code> action specified <code>[3]</code>, then the final attribute value is <code>
     *         [1,2,3]</code>. An error occurs if an <code>ADD</code> action is specified for a set attribute
     *         and the attribute type specified does not match the existing set type. Both sets must have the
     *         same primitive data type. For example, if the existing data type is a set of strings, the <code>
     *         Value</code> must also be a set of strings.
     *       </li>
     *     </ul>The <code>ADD</code> action only supports Number and set data types.
     *   </li>
     *   <li>
     *     <code>DELETE</code> - Deletes an element from a set.If a set of values is specified, then those values
     *     are subtracted from the old set. For example, if the attribute value was the set <code>[a,b,c]</code> and
     *     the <code>DELETE</code> action specifies <code>[a,c]</code>, then the final attribute value is <code>[b]</code>
     *     . Specifying an empty set is an error.The <code>DELETE</code> action only supports set data types.
     *   </li>
     * </ul>
     *
     * <p>You can have many actions in a single expression, such as the following: <code>SET a=:value1, b=:value2 DELETE
     * :value3, :value4, :value5</code>
     *
     * <p>For more information on update expressions, see <a href="https://docs.aws.amazon.com/amazondynamodb/latest/developerguide/Expressions.Modifying.html">Modifying Items and Attributes</a> in the <i>Amazon
     * DynamoDB Developer Guide</i>.
     */
    public String getUpdateExpression() {
        return updateExpression;
    }

    /**
     * A condition that must be satisfied in order for a conditional update to succeed.
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
     * <p>For more information about condition expressions, see <a href="https://docs.aws.amazon.com/amazondynamodb/latest/developerguide/Expressions.SpecifyingConditions.html">Specifying Conditions</a> in the <i>Amazon DynamoDB
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
     * .) To work around this, you could specify the following for <code>ExpressionAttributeNames</code>:
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
     * <p>For more information about expression attribute names, see <a href="https://docs.aws.amazon.com/amazondynamodb/latest/developerguide/Expressions.AccessingItemAttributes.html">Specifying Item Attributes</a> in the <i>Amazon
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
     * that you wanted to check whether the value of the <code>ProductStatus</code> attribute was one of the following:
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
     * An optional parameter that returns the item attributes for an <code>UpdateItem</code> operation that failed a
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
        UpdateItemInput that = (UpdateItemInput) other;
        return Objects.equals(this.tableName, that.tableName)
               && Objects.equals(this.updateExpression, that.updateExpression)
               && Objects.equals(this.conditionExpression, that.conditionExpression)
               && Objects.equals(this.conditionalOperator, that.conditionalOperator)
               && Objects.equals(this.returnValues, that.returnValues)
               && Objects.equals(this.returnConsumedCapacity, that.returnConsumedCapacity)
               && Objects.equals(this.returnItemCollectionMetrics, that.returnItemCollectionMetrics)
               && Objects.equals(this.returnValuesOnConditionCheckFailure, that.returnValuesOnConditionCheckFailure)
               && Objects.equals(this.key, that.key)
               && Objects.equals(this.attributeUpdates, that.attributeUpdates)
               && Objects.equals(this.expected, that.expected)
               && Objects.equals(this.expressionAttributeNames, that.expressionAttributeNames)
               && Objects.equals(this.expressionAttributeValues, that.expressionAttributeValues);
    }

    @Override
    public int hashCode() {
        int $hc = Objects.hashCode(tableName);
        $hc = 31 * $hc + Objects.hashCode(key);
        $hc = 31 * $hc + Objects.hashCode(attributeUpdates);
        $hc = 31 * $hc + Objects.hashCode(expected);
        $hc = 31 * $hc + Objects.hashCode(conditionalOperator);
        $hc = 31 * $hc + Objects.hashCode(returnValues);
        $hc = 31 * $hc + Objects.hashCode(returnConsumedCapacity);
        $hc = 31 * $hc + Objects.hashCode(returnItemCollectionMetrics);
        $hc = 31 * $hc + Objects.hashCode(updateExpression);
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
        if (attributeUpdates != null) {
            serializer.writeMap($SCHEMA_ATTRIBUTE_UPDATES, attributeUpdates, attributeUpdates.size(), SharedSerde.AttributeUpdatesSerializer.INSTANCE);
        }
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
        if (updateExpression != null) {
            serializer.writeString($SCHEMA_UPDATE_EXPRESSION, updateExpression);
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
            case 2 -> (T) SchemaUtils.validateSameMember($SCHEMA_ATTRIBUTE_UPDATES, member, attributeUpdates);
            case 3 -> (T) SchemaUtils.validateSameMember($SCHEMA_EXPECTED, member, expected);
            case 4 -> (T) SchemaUtils.validateSameMember($SCHEMA_CONDITIONAL_OPERATOR, member, conditionalOperator);
            case 5 -> (T) SchemaUtils.validateSameMember($SCHEMA_RETURN_VALUES, member, returnValues);
            case 6 -> (T) SchemaUtils.validateSameMember($SCHEMA_RETURN_CONSUMED_CAPACITY, member, returnConsumedCapacity);
            case 7 -> (T) SchemaUtils.validateSameMember($SCHEMA_RETURN_ITEM_COLLECTION_METRICS, member, returnItemCollectionMetrics);
            case 8 -> (T) SchemaUtils.validateSameMember($SCHEMA_UPDATE_EXPRESSION, member, updateExpression);
            case 9 -> (T) SchemaUtils.validateSameMember($SCHEMA_CONDITION_EXPRESSION, member, conditionExpression);
            case 10 -> (T) SchemaUtils.validateSameMember($SCHEMA_EXPRESSION_ATTRIBUTE_NAMES, member, expressionAttributeNames);
            case 11 -> (T) SchemaUtils.validateSameMember($SCHEMA_EXPRESSION_ATTRIBUTE_VALUES, member, expressionAttributeValues);
            case 12 -> (T) SchemaUtils.validateSameMember($SCHEMA_RETURN_VALUES_ON_CONDITION_CHECK_FAILURE, member, returnValuesOnConditionCheckFailure);
            default -> throw new IllegalArgumentException("Attempted to get non-existent member: " + member.id());
        };
    }

    /**
     * Create a new builder containing all the current property values of this object.
     *
     * <p><strong>Note:</strong> This method performs only a shallow copy of the original properties.
     *
     * @return a builder for {@link UpdateItemInput}.
     */
    public Builder toBuilder() {
        var builder = new Builder();
        builder.tableName(this.tableName);
        builder.key(this.key);
        builder.attributeUpdates(this.attributeUpdates);
        builder.expected(this.expected);
        builder.conditionalOperator(this.conditionalOperator);
        builder.returnValues(this.returnValues);
        builder.returnConsumedCapacity(this.returnConsumedCapacity);
        builder.returnItemCollectionMetrics(this.returnItemCollectionMetrics);
        builder.updateExpression(this.updateExpression);
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
     * Builder for {@link UpdateItemInput}.
     */
    public static final class Builder implements ShapeBuilder<UpdateItemInput> {
        private final PresenceTracker tracker = PresenceTracker.of($SCHEMA);
        private String tableName;
        private Map<String, AttributeValue> key;
        private Map<String, AttributeValueUpdate> attributeUpdates;
        private Map<String, ExpectedAttributeValue> expected;
        private ConditionalOperator conditionalOperator;
        private ReturnValue returnValues;
        private ReturnConsumedCapacity returnConsumedCapacity;
        private ReturnItemCollectionMetrics returnItemCollectionMetrics;
        private String updateExpression;
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
         * The name of the table containing the item to update. You can also provide the Amazon Resource Name (ARN) of the
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
         * The primary key of the item to be updated. Each element consists of an attribute name and a value for that
         * attribute.
         *
         * <p>For the primary key, you must provide all of the attributes. For example, with a simple primary key, you only
         * need to provide a value for the partition key. For a composite primary key, you must provide values for both the
         * partition key and the sort key.
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
         * This is a legacy parameter. Use <code>UpdateExpression</code> instead. For more information, see <a href="https://docs.aws.amazon.com/amazondynamodb/latest/developerguide/LegacyConditionalParameters.AttributeUpdates.html">
         * AttributeUpdates</a> in the <i>Amazon DynamoDB Developer Guide</i>.
         *
         * @return this builder.
         */
        public Builder attributeUpdates(Map<String, AttributeValueUpdate> attributeUpdates) {
            this.attributeUpdates = attributeUpdates;
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
         * Use <code>ReturnValues</code> if you want to get the item attributes as they appear before or after they are
         * successfully updated. For <code>UpdateItem</code>, the valid values are:
         *
         * <ul>
         *   <li>
         *     <code>NONE</code> - If <code>ReturnValues</code> is not specified, or if its value is <code>NONE</code>,
         *     then nothing is returned. (This setting is the default for <code>ReturnValues</code>.)
         *   </li>
         *   <li>
         *     <code>ALL_OLD</code> - Returns all of the attributes of the item, as they appeared before the UpdateItem
         *     operation.
         *   </li>
         *   <li>
         *     <code>UPDATED_OLD</code> - Returns only the updated attributes, as they appeared before the UpdateItem
         *     operation.
         *   </li>
         *   <li>
         *     <code>ALL_NEW</code> - Returns all of the attributes of the item, as they appear after the UpdateItem
         *     operation.
         *   </li>
         *   <li>
         *     <code>UPDATED_NEW</code> - Returns only the updated attributes, as they appear after the UpdateItem
         *     operation.
         *   </li>
         * </ul>
         *
         * <p>There is no additional cost associated with requesting a return value aside from the small network and
         * processing overhead of receiving a larger response. No read capacity units are consumed.
         *
         * <p>The values returned are strongly consistent.
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
         * An expression that defines one or more attributes to be updated, the action to be performed on them, and new
         * values for them.
         *
         * <p>The following action values are available for <code>UpdateExpression</code>.
         *
         * <ul>
         *   <li>
         *     <code>SET</code> - Adds one or more attributes and values to an item. If any of these attributes already
         *     exist, they are replaced by the new values. You can also use <code>SET</code> to add or subtract from an
         *     attribute that is of type Number. For example: <code>SET myNum = myNum + :val</code><code>SET</code>
         *     supports the following functions:
         *
         *     <ul>
         *       <li>
         *         <code>if_not_exists (path, operand)</code> - if the item does not contain an attribute at the
         *         specified path, then <code>if_not_exists</code> evaluates to operand; otherwise, it evaluates to
         *         path. You can use this function to avoid overwriting an attribute that may already be present in
         *         the item.
         *       </li>
         *       <li>
         *         <code>list_append (operand, operand)</code> - evaluates to a list with a new element added to it.
         *         You can append the new element to the start or the end of the list by reversing the order of the
         *         operands.
         *       </li>
         *     </ul>These function names are case-sensitive.
         *   </li>
         *   <li>
         *     <code>REMOVE</code> - Removes one or more attributes from an item.
         *   </li>
         *   <li>
         *     <code>ADD</code> - Adds the specified value to the item, if the attribute does not already exist. If the
         *     attribute does exist, then the behavior of <code>ADD</code> depends on the data type of the attribute:
         *
         *     <ul>
         *       <li>
         *         If the existing attribute is a number, and if <code>Value</code> is also a number, then <code>
         *         Value</code> is mathematically added to the existing attribute. If <code>Value</code> is a
         *         negative number, then it is subtracted from the existing attribute.If you use <code>ADD</code> to
         *         increment or decrement a number value for an item that doesn't exist before the update, DynamoDB
         *         uses <code>0</code> as the initial value.Similarly, if you use <code>ADD</code> for an existing
         *         item to increment or decrement an attribute value that doesn't exist before the update, DynamoDB
         *         uses <code>0</code> as the initial value. For example, suppose that the item you want to update
         *         doesn't have an attribute named <code>itemcount</code>, but you decide to <code>ADD</code> the
         *         number <code>3</code> to this attribute anyway. DynamoDB will create the <code>itemcount</code>
         *         attribute, set its initial value to <code>0</code>, and finally add <code>3</code> to it. The
         *         result will be a new <code>itemcount</code> attribute in the item, with a value of <code>3</code>
         *         .
         *       </li>
         *       <li>
         *         If the existing data type is a set and if <code>Value</code> is also a set, then <code>Value</code>
         *         is added to the existing set. For example, if the attribute value is the set <code>[1,2]</code>,
         *         and the <code>ADD</code> action specified <code>[3]</code>, then the final attribute value is <code>
         *         [1,2,3]</code>. An error occurs if an <code>ADD</code> action is specified for a set attribute
         *         and the attribute type specified does not match the existing set type. Both sets must have the
         *         same primitive data type. For example, if the existing data type is a set of strings, the <code>
         *         Value</code> must also be a set of strings.
         *       </li>
         *     </ul>The <code>ADD</code> action only supports Number and set data types.
         *   </li>
         *   <li>
         *     <code>DELETE</code> - Deletes an element from a set.If a set of values is specified, then those values
         *     are subtracted from the old set. For example, if the attribute value was the set <code>[a,b,c]</code> and
         *     the <code>DELETE</code> action specifies <code>[a,c]</code>, then the final attribute value is <code>[b]</code>
         *     . Specifying an empty set is an error.The <code>DELETE</code> action only supports set data types.
         *   </li>
         * </ul>
         *
         * <p>You can have many actions in a single expression, such as the following: <code>SET a=:value1, b=:value2 DELETE
         * :value3, :value4, :value5</code>
         *
         * <p>For more information on update expressions, see <a href="https://docs.aws.amazon.com/amazondynamodb/latest/developerguide/Expressions.Modifying.html">Modifying Items and Attributes</a> in the <i>Amazon
         * DynamoDB Developer Guide</i>.
         *
         * @return this builder.
         */
        public Builder updateExpression(String updateExpression) {
            this.updateExpression = updateExpression;
            return this;
        }

        /**
         * A condition that must be satisfied in order for a conditional update to succeed.
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
         * <p>For more information about condition expressions, see <a href="https://docs.aws.amazon.com/amazondynamodb/latest/developerguide/Expressions.SpecifyingConditions.html">Specifying Conditions</a> in the <i>Amazon DynamoDB
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
         * .) To work around this, you could specify the following for <code>ExpressionAttributeNames</code>:
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
         * <p>For more information about expression attribute names, see <a href="https://docs.aws.amazon.com/amazondynamodb/latest/developerguide/Expressions.AccessingItemAttributes.html">Specifying Item Attributes</a> in the <i>Amazon
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
         * that you wanted to check whether the value of the <code>ProductStatus</code> attribute was one of the following:
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
         * An optional parameter that returns the item attributes for an <code>UpdateItem</code> operation that failed a
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
        public UpdateItemInput build() {
            tracker.validate();
            return new UpdateItemInput(this);
        }

        @Override
        @SuppressWarnings("unchecked")
        public void setMemberValue(Schema member, Object value) {
            switch (member.memberIndex()) {
                case 0 -> tableName((String) SchemaUtils.validateSameMember($SCHEMA_TABLE_NAME, member, value));
                case 1 -> key((Map<String, AttributeValue>) SchemaUtils.validateSameMember($SCHEMA_KEY, member, value));
                case 2 -> attributeUpdates((Map<String, AttributeValueUpdate>) SchemaUtils.validateSameMember($SCHEMA_ATTRIBUTE_UPDATES, member, value));
                case 3 -> expected((Map<String, ExpectedAttributeValue>) SchemaUtils.validateSameMember($SCHEMA_EXPECTED, member, value));
                case 4 -> conditionalOperator((ConditionalOperator) SchemaUtils.validateSameMember($SCHEMA_CONDITIONAL_OPERATOR, member, value));
                case 5 -> returnValues((ReturnValue) SchemaUtils.validateSameMember($SCHEMA_RETURN_VALUES, member, value));
                case 6 -> returnConsumedCapacity((ReturnConsumedCapacity) SchemaUtils.validateSameMember($SCHEMA_RETURN_CONSUMED_CAPACITY, member, value));
                case 7 -> returnItemCollectionMetrics((ReturnItemCollectionMetrics) SchemaUtils.validateSameMember($SCHEMA_RETURN_ITEM_COLLECTION_METRICS, member, value));
                case 8 -> updateExpression((String) SchemaUtils.validateSameMember($SCHEMA_UPDATE_EXPRESSION, member, value));
                case 9 -> conditionExpression((String) SchemaUtils.validateSameMember($SCHEMA_CONDITION_EXPRESSION, member, value));
                case 10 -> expressionAttributeNames((Map<String, String>) SchemaUtils.validateSameMember($SCHEMA_EXPRESSION_ATTRIBUTE_NAMES, member, value));
                case 11 -> expressionAttributeValues((Map<String, AttributeValue>) SchemaUtils.validateSameMember($SCHEMA_EXPRESSION_ATTRIBUTE_VALUES, member, value));
                case 12 -> returnValuesOnConditionCheckFailure((ReturnValuesOnConditionCheckFailure) SchemaUtils.validateSameMember($SCHEMA_RETURN_VALUES_ON_CONDITION_CHECK_FAILURE, member, value));
                default -> ShapeBuilder.super.setMemberValue(member, value);
            }
        }

        @Override
        public ShapeBuilder<UpdateItemInput> errorCorrection() {
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
                    case 2 -> builder.attributeUpdates(SharedSerde.deserializeAttributeUpdates(member, de));
                    case 3 -> builder.expected(SharedSerde.deserializeExpectedAttributeMap(member, de));
                    case 4 -> builder.conditionalOperator(ConditionalOperator.builder().deserializeMember(de, member).build());
                    case 5 -> builder.returnValues(ReturnValue.builder().deserializeMember(de, member).build());
                    case 6 -> builder.returnConsumedCapacity(ReturnConsumedCapacity.builder().deserializeMember(de, member).build());
                    case 7 -> builder.returnItemCollectionMetrics(ReturnItemCollectionMetrics.builder().deserializeMember(de, member).build());
                    case 8 -> builder.updateExpression(de.readString(member));
                    case 9 -> builder.conditionExpression(de.readString(member));
                    case 10 -> builder.expressionAttributeNames(SharedSerde.deserializeExpressionAttributeNameMap(member, de));
                    case 11 -> builder.expressionAttributeValues(SharedSerde.deserializeExpressionAttributeValueMap(member, de));
                    case 12 -> builder.returnValuesOnConditionCheckFailure(ReturnValuesOnConditionCheckFailure.builder().deserializeMember(de, member).build());
                    default -> throw new IllegalArgumentException("Unexpected member: " + member.memberName());
                }
            }
        }
    }
}
