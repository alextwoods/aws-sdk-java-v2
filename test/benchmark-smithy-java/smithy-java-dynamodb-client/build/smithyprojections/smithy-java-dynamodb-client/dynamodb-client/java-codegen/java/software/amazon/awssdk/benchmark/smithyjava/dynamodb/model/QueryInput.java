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

/**
 * Represents the input of a <code>Query</code> operation.
 */
@SmithyGenerated
public final class QueryInput implements SerializableStruct {

    public static final Schema $SCHEMA = Schemas.QUERY_INPUT;
    private static final Schema $SCHEMA_TABLE_NAME = $SCHEMA.member("TableName");
    private static final Schema $SCHEMA_INDEX_NAME = $SCHEMA.member("IndexName");
    private static final Schema $SCHEMA_SELECT = $SCHEMA.member("Select");
    private static final Schema $SCHEMA_ATTRIBUTES_TO_GET = $SCHEMA.member("AttributesToGet");
    private static final Schema $SCHEMA_LIMIT = $SCHEMA.member("Limit");
    private static final Schema $SCHEMA_CONSISTENT_READ = $SCHEMA.member("ConsistentRead");
    private static final Schema $SCHEMA_KEY_CONDITIONS = $SCHEMA.member("KeyConditions");
    private static final Schema $SCHEMA_QUERY_FILTER = $SCHEMA.member("QueryFilter");
    private static final Schema $SCHEMA_CONDITIONAL_OPERATOR = $SCHEMA.member("ConditionalOperator");
    private static final Schema $SCHEMA_SCAN_INDEX_FORWARD = $SCHEMA.member("ScanIndexForward");
    private static final Schema $SCHEMA_EXCLUSIVE_START_KEY = $SCHEMA.member("ExclusiveStartKey");
    private static final Schema $SCHEMA_RETURN_CONSUMED_CAPACITY = $SCHEMA.member("ReturnConsumedCapacity");
    private static final Schema $SCHEMA_PROJECTION_EXPRESSION = $SCHEMA.member("ProjectionExpression");
    private static final Schema $SCHEMA_FILTER_EXPRESSION = $SCHEMA.member("FilterExpression");
    private static final Schema $SCHEMA_KEY_CONDITION_EXPRESSION = $SCHEMA.member("KeyConditionExpression");
    private static final Schema $SCHEMA_EXPRESSION_ATTRIBUTE_NAMES = $SCHEMA.member("ExpressionAttributeNames");
    private static final Schema $SCHEMA_EXPRESSION_ATTRIBUTE_VALUES = $SCHEMA.member("ExpressionAttributeValues");

    public static final ShapeId $ID = $SCHEMA.id();

    private final transient String tableName;
    private final transient String indexName;
    private final transient Select select;
    private final transient List<String> attributesToGet;
    private final transient Integer limit;
    private final transient Boolean consistentRead;
    private final transient Map<String, Condition> keyConditions;
    private final transient Map<String, Condition> queryFilter;
    private final transient ConditionalOperator conditionalOperator;
    private final transient Boolean scanIndexForward;
    private final transient Map<String, AttributeValue> exclusiveStartKey;
    private final transient ReturnConsumedCapacity returnConsumedCapacity;
    private final transient String projectionExpression;
    private final transient String filterExpression;
    private final transient String keyConditionExpression;
    private final transient Map<String, String> expressionAttributeNames;
    private final transient Map<String, AttributeValue> expressionAttributeValues;

    private QueryInput(Builder builder) {
        this.tableName = builder.tableName;
        this.indexName = builder.indexName;
        this.select = builder.select;
        this.attributesToGet = builder.attributesToGet == null ? null : Collections.unmodifiableList(builder.attributesToGet);
        this.limit = builder.limit;
        this.consistentRead = builder.consistentRead;
        this.keyConditions = builder.keyConditions == null ? null : Collections.unmodifiableMap(builder.keyConditions);
        this.queryFilter = builder.queryFilter == null ? null : Collections.unmodifiableMap(builder.queryFilter);
        this.conditionalOperator = builder.conditionalOperator;
        this.scanIndexForward = builder.scanIndexForward;
        this.exclusiveStartKey = builder.exclusiveStartKey == null ? null : Collections.unmodifiableMap(builder.exclusiveStartKey);
        this.returnConsumedCapacity = builder.returnConsumedCapacity;
        this.projectionExpression = builder.projectionExpression;
        this.filterExpression = builder.filterExpression;
        this.keyConditionExpression = builder.keyConditionExpression;
        this.expressionAttributeNames = builder.expressionAttributeNames == null ? null : Collections.unmodifiableMap(builder.expressionAttributeNames);
        this.expressionAttributeValues = builder.expressionAttributeValues == null ? null : Collections.unmodifiableMap(builder.expressionAttributeValues);
    }

    /**
     * The name of the table containing the requested items. You can also provide the Amazon Resource Name (ARN) of the
     * table in this parameter.
     */
    public String getTableName() {
        return tableName;
    }

    /**
     * The name of an index to query. This index can be any local secondary index or global secondary index on the
     * table. Note that if you use the <code>IndexName</code> parameter, you must also provide <code>TableName.</code>
     */
    public String getIndexName() {
        return indexName;
    }

    /**
     * The attributes to be returned in the result. You can retrieve all item attributes, specific item attributes, the
     * count of matching items, or in the case of an index, some or all of the attributes projected into the index.
     *
     * <ul>
     *   <li>
     *     <code>ALL_ATTRIBUTES</code> - Returns all of the item attributes from the specified table or index. If
     *     you query a local secondary index, then for each matching item in the index, DynamoDB fetches the entire
     *     item from the parent table. If the index is configured to project all item attributes, then all of the
     *     data can be obtained from the local secondary index, and no fetching is required.
     *   </li>
     *   <li>
     *     <code>ALL_PROJECTED_ATTRIBUTES</code> - Allowed only when querying an index. Retrieves all attributes
     *     that have been projected into the index. If the index is configured to project all attributes, this
     *     return value is equivalent to specifying <code>ALL_ATTRIBUTES</code>.
     *   </li>
     *   <li>
     *     <code>COUNT</code> - Returns the number of matching items, rather than the matching items themselves.
     *     Note that this uses the same quantity of read capacity units as getting the items, and is subject to the
     *     same item size calculations.
     *   </li>
     *   <li>
     *     <code>SPECIFIC_ATTRIBUTES</code> - Returns only the attributes listed in <code>ProjectionExpression</code>
     *     . This return value is equivalent to specifying <code>ProjectionExpression</code> without specifying any
     *     value for <code>Select</code>.If you query or scan a local secondary index and request only attributes
     *     that are projected into that index, the operation will read only the index and not the table. If any of
     *     the requested attributes are not projected into the local secondary index, DynamoDB fetches each of these
     *     attributes from the parent table. This extra fetching incurs additional throughput cost and latency.If
     *     you query or scan a global secondary index, you can only request attributes that are projected into the
     *     index. Global secondary index queries cannot fetch attributes from the parent table.
     *   </li>
     * </ul>
     *
     * <p>If neither <code>Select</code> nor <code>ProjectionExpression</code> are specified, DynamoDB defaults to <code>
     * ALL_ATTRIBUTES</code> when accessing a table, and <code>ALL_PROJECTED_ATTRIBUTES</code> when accessing an index.
     * You cannot use both <code>Select</code> and <code>ProjectionExpression</code> together in a single request,
     * unless the value for <code>Select</code> is <code>SPECIFIC_ATTRIBUTES</code>. (This usage is equivalent to
     * specifying <code>ProjectionExpression</code> without any value for <code>Select</code>.)
     *
     * <p>If you use the <code>ProjectionExpression</code> parameter, then the value for <code>Select</code> can only be
     * <code>SPECIFIC_ATTRIBUTES</code>. Any other value for <code>Select</code> will return an error.
     */
    public Select getSelect() {
        return select;
    }

    /**
     * This is a legacy parameter. Use <code>ProjectionExpression</code> instead. For more information, see <a href="https://docs.aws.amazon.com/amazondynamodb/latest/developerguide/LegacyConditionalParameters.AttributesToGet.html">
     * AttributesToGet</a> in the <i>Amazon DynamoDB Developer Guide</i>.
     */
    public List<String> getAttributesToGet() {
        if (attributesToGet == null) {
            return Collections.emptyList();
        }
        return attributesToGet;
    }

    public boolean hasAttributesToGet() {
        return attributesToGet != null;
    }

    /**
     * The maximum number of items to evaluate (not necessarily the number of matching items). If DynamoDB processes the
     * number of items up to the limit while processing the results, it stops the operation and returns the matching
     * values up to that point, and a key in <code>LastEvaluatedKey</code> to apply in a subsequent operation, so that
     * you can pick up where you left off. Also, if the processed dataset size exceeds 1 MB before DynamoDB reaches this
     * limit, it stops the operation and returns the matching values up to the limit, and a key in <code>
     * LastEvaluatedKey</code> to apply in a subsequent operation to continue the operation. For more information, see <a href="https://docs.aws.amazon.com/amazondynamodb/latest/developerguide/QueryAndScan.html">
     * Query and Scan</a> in the <i>Amazon DynamoDB Developer Guide</i>.
     */
    public Integer getLimit() {
        return limit;
    }

    /**
     * Determines the read consistency model: If set to <code>true</code>, then the operation uses strongly consistent
     * reads; otherwise, the operation uses eventually consistent reads.
     *
     * <p>Strongly consistent reads are not supported on global secondary indexes. If you query a global secondary index
     * with <code>ConsistentRead</code> set to <code>true</code>, you will receive a <code>ValidationException</code>.
     */
    public Boolean isConsistentRead() {
        return consistentRead;
    }

    /**
     * This is a legacy parameter. Use <code>KeyConditionExpression</code> instead. For more information, see <a href="https://docs.aws.amazon.com/amazondynamodb/latest/developerguide/LegacyConditionalParameters.KeyConditions.html">
     * KeyConditions</a> in the <i>Amazon DynamoDB Developer Guide</i>.
     */
    public Map<String, Condition> getKeyConditions() {
        if (keyConditions == null) {
            return Collections.emptyMap();
        }
        return keyConditions;
    }

    public boolean hasKeyConditions() {
        return keyConditions != null;
    }

    /**
     * This is a legacy parameter. Use <code>FilterExpression</code> instead. For more information, see <a href="https://docs.aws.amazon.com/amazondynamodb/latest/developerguide/LegacyConditionalParameters.QueryFilter.html">QueryFilter</a>
     * in the <i>Amazon DynamoDB Developer Guide</i>.
     */
    public Map<String, Condition> getQueryFilter() {
        if (queryFilter == null) {
            return Collections.emptyMap();
        }
        return queryFilter;
    }

    public boolean hasQueryFilter() {
        return queryFilter != null;
    }

    /**
     * This is a legacy parameter. Use <code>FilterExpression</code> instead. For more information, see <a href="https://docs.aws.amazon.com/amazondynamodb/latest/developerguide/LegacyConditionalParameters.ConditionalOperator.html">
     * ConditionalOperator</a> in the <i>Amazon DynamoDB Developer Guide</i>.
     */
    public ConditionalOperator getConditionalOperator() {
        return conditionalOperator;
    }

    /**
     * Specifies the order for index traversal: If <code>true</code> (default), the traversal is performed in ascending
     * order; if <code>false</code>, the traversal is performed in descending order.
     *
     * <p>Items with the same partition key value are stored in sorted order by sort key. If the sort key data type is
     * Number, the results are stored in numeric order. For type String, the results are stored in order of UTF-8 bytes.
     * For type Binary, DynamoDB treats each byte of the binary data as unsigned.
     *
     * <p>If <code>ScanIndexForward</code> is <code>true</code>, DynamoDB returns the results in the order in which they
     * are stored (by sort key value). This is the default behavior. If <code>ScanIndexForward</code> is <code>false</code>
     * , DynamoDB reads the results in reverse order by sort key value, and then returns the results to the client.
     */
    public Boolean isScanIndexForward() {
        return scanIndexForward;
    }

    /**
     * The primary key of the first item that this operation will evaluate. Use the value that was returned for <code>
     * LastEvaluatedKey</code> in the previous operation.
     *
     * <p>The data type for <code>ExclusiveStartKey</code> must be String, Number, or Binary. No set data types are
     * allowed.
     */
    public Map<String, AttributeValue> getExclusiveStartKey() {
        if (exclusiveStartKey == null) {
            return Collections.emptyMap();
        }
        return exclusiveStartKey;
    }

    public boolean hasExclusiveStartKey() {
        return exclusiveStartKey != null;
    }

    public ReturnConsumedCapacity getReturnConsumedCapacity() {
        return returnConsumedCapacity;
    }

    /**
     * A string that identifies one or more attributes to retrieve from the table. These attributes can include scalars,
     * sets, or elements of a JSON document. The attributes in the expression must be separated by commas.
     *
     * <p>If no attribute names are specified, then all attributes will be returned. If any of the requested attributes
     * are not found, they will not appear in the result.
     *
     * <p>For more information, see <a href="https://docs.aws.amazon.com/amazondynamodb/latest/developerguide/Expressions.AccessingItemAttributes.html">Accessing Item Attributes</a> in the <i>Amazon DynamoDB Developer Guide</i>.
     */
    public String getProjectionExpression() {
        return projectionExpression;
    }

    /**
     * A string that contains conditions that DynamoDB applies after the <code>Query</code> operation, but before the
     * data is returned to you. Items that do not satisfy the <code>FilterExpression</code> criteria are not returned.
     *
     * <p>A <code>FilterExpression</code> does not allow key attributes. You cannot define a filter expression based on
     * a partition key or a sort key.
     *
     * <p>A <code>FilterExpression</code> is applied after the items have already been read; the process of filtering
     * does not consume any additional read capacity units.
     *
     * <p>For more information, see <a href="https://docs.aws.amazon.com/amazondynamodb/latest/developerguide/Query.FilterExpression.html">Filter Expressions</a> in the <i>Amazon DynamoDB Developer Guide</i>.
     */
    public String getFilterExpression() {
        return filterExpression;
    }

    /**
     * The condition that specifies the key values for items to be retrieved by the <code>Query</code> action.
     *
     * <p>The condition must perform an equality test on a single partition key value.
     *
     * <p>The condition can optionally perform one of several comparison tests on a single sort key value. This allows <code>
     * Query</code> to retrieve one item with a given partition key value and sort key value, or several items that have
     * the same partition key value but different sort key values.
     *
     * <p>The partition key equality test is required, and must be specified in the following format:
     *
     * <p><code>partitionKeyName</code><i>=</i><code>:partitionkeyval</code>
     *
     * <p>If you also want to provide a condition for the sort key, it must be combined using <code>AND</code> with the
     * condition for the sort key. Following is an example, using the <b>=</b> comparison operator for the sort key:
     *
     * <p><code>partitionKeyName</code><code>=</code><code>:partitionkeyval</code><code>AND</code><code>sortKeyName</code><code>
     * =</code><code>:sortkeyval</code>
     *
     * <p>Valid comparisons for the sort key condition are as follows:
     *
     * <ul>
     *   <li>
     *     <code>sortKeyName</code><code>=</code><code>:sortkeyval</code> - true if the sort key value is equal to <code>
     *     :sortkeyval</code>.
     *   </li>
     *   <li>
     *     <code>sortKeyName</code><code>&lt;</code><code>:sortkeyval</code> - true if the sort key value is less
     *     than <code>:sortkeyval</code>.
     *   </li>
     *   <li>
     *     <code>sortKeyName</code><code>&lt;=</code><code>:sortkeyval</code> - true if the sort key value is less
     *     than or equal to <code>:sortkeyval</code>.
     *   </li>
     *   <li>
     *     <code>sortKeyName</code><code>&gt;</code><code>:sortkeyval</code> - true if the sort key value is greater
     *     than <code>:sortkeyval</code>.
     *   </li>
     *   <li>
     *     <code>sortKeyName</code><code>&gt;= </code><code>:sortkeyval</code> - true if the sort key value is
     *     greater than or equal to <code>:sortkeyval</code>.
     *   </li>
     *   <li>
     *     <code>sortKeyName</code><code>BETWEEN</code><code>:sortkeyval1</code><code>AND</code><code>:sortkeyval2</code>
     *     - true if the sort key value is greater than or equal to <code>:sortkeyval1</code>, and less than or
     *     equal to <code>:sortkeyval2</code>.
     *   </li>
     *   <li>
     *     <code>begins_with (</code><code>sortKeyName</code>, <code>:sortkeyval</code><code>)</code> - true if the
     *     sort key value begins with a particular operand. (You cannot use this function with a sort key that is of
     *     type Number.) Note that the function name <code>begins_with</code> is case-sensitive.
     *   </li>
     * </ul>
     *
     * <p>Use the <code>ExpressionAttributeValues</code> parameter to replace tokens such as <code>:partitionval</code>
     * and <code>:sortval</code> with actual values at runtime.
     *
     * <p>You can optionally use the <code>ExpressionAttributeNames</code> parameter to replace the names of the
     * partition key and sort key with placeholder tokens. This option might be necessary if an attribute name conflicts
     * with a DynamoDB reserved word. For example, the following <code>KeyConditionExpression</code> parameter causes an
     * error because <i>Size</i> is a reserved word:
     *
     * <ul>
     *   <li>
     *     <code>Size = :myval</code>
     *   </li>
     * </ul>
     *
     * <p>To work around this, define a placeholder (such a <code>#S</code>) to represent the attribute name <i>Size</i>
     * . <code>KeyConditionExpression</code> then is as follows:
     *
     * <ul>
     *   <li>
     *     <code>#S = :myval</code>
     *   </li>
     * </ul>
     *
     * <p>For a list of reserved words, see <a href="https://docs.aws.amazon.com/amazondynamodb/latest/developerguide/ReservedWords.html">Reserved Words</a> in the <i>Amazon DynamoDB Developer Guide</i>.
     *
     * <p>For more information on <code>ExpressionAttributeNames</code> and <code>ExpressionAttributeValues</code>, see <a href="https://docs.aws.amazon.com/amazondynamodb/latest/developerguide/ExpressionPlaceholders.html">
     * Using Placeholders for Attribute Names and Values</a> in the <i>Amazon DynamoDB Developer Guide</i>.
     */
    public String getKeyConditionExpression() {
        return keyConditionExpression;
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
     * <p>For more information on expression attribute values, see <a href="https://docs.aws.amazon.com/amazondynamodb/latest/developerguide/Expressions.SpecifyingConditions.html">Specifying Conditions</a> in the <i>Amazon
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
        QueryInput that = (QueryInput) other;
        return Objects.equals(this.consistentRead, that.consistentRead)
               && Objects.equals(this.scanIndexForward, that.scanIndexForward)
               && Objects.equals(this.limit, that.limit)
               && Objects.equals(this.tableName, that.tableName)
               && Objects.equals(this.indexName, that.indexName)
               && Objects.equals(this.projectionExpression, that.projectionExpression)
               && Objects.equals(this.filterExpression, that.filterExpression)
               && Objects.equals(this.keyConditionExpression, that.keyConditionExpression)
               && Objects.equals(this.select, that.select)
               && Objects.equals(this.conditionalOperator, that.conditionalOperator)
               && Objects.equals(this.returnConsumedCapacity, that.returnConsumedCapacity)
               && Objects.equals(this.attributesToGet, that.attributesToGet)
               && Objects.equals(this.keyConditions, that.keyConditions)
               && Objects.equals(this.queryFilter, that.queryFilter)
               && Objects.equals(this.exclusiveStartKey, that.exclusiveStartKey)
               && Objects.equals(this.expressionAttributeNames, that.expressionAttributeNames)
               && Objects.equals(this.expressionAttributeValues, that.expressionAttributeValues);
    }

    @Override
    public int hashCode() {
        int $hc = Objects.hashCode(tableName);
        $hc = 31 * $hc + Objects.hashCode(indexName);
        $hc = 31 * $hc + Objects.hashCode(select);
        $hc = 31 * $hc + Objects.hashCode(attributesToGet);
        $hc = 31 * $hc + Objects.hashCode(limit);
        $hc = 31 * $hc + Objects.hashCode(consistentRead);
        $hc = 31 * $hc + Objects.hashCode(keyConditions);
        $hc = 31 * $hc + Objects.hashCode(queryFilter);
        $hc = 31 * $hc + Objects.hashCode(conditionalOperator);
        $hc = 31 * $hc + Objects.hashCode(scanIndexForward);
        $hc = 31 * $hc + Objects.hashCode(exclusiveStartKey);
        $hc = 31 * $hc + Objects.hashCode(returnConsumedCapacity);
        $hc = 31 * $hc + Objects.hashCode(projectionExpression);
        $hc = 31 * $hc + Objects.hashCode(filterExpression);
        $hc = 31 * $hc + Objects.hashCode(keyConditionExpression);
        $hc = 31 * $hc + Objects.hashCode(expressionAttributeNames);
        $hc = 31 * $hc + Objects.hashCode(expressionAttributeValues);
        return $hc;
    }

    @Override
    public Schema schema() {
        return $SCHEMA;
    }

    @Override
    public void serializeMembers(ShapeSerializer serializer) {
        serializer.writeString($SCHEMA_TABLE_NAME, tableName);
        if (indexName != null) {
            serializer.writeString($SCHEMA_INDEX_NAME, indexName);
        }
        if (select != null) {
            serializer.writeString($SCHEMA_SELECT, select.getValue());
        }
        if (attributesToGet != null) {
            serializer.writeList($SCHEMA_ATTRIBUTES_TO_GET, attributesToGet, attributesToGet.size(), SharedSerde.AttributeNameListSerializer.INSTANCE);
        }
        if (limit != null) {
            serializer.writeInteger($SCHEMA_LIMIT, limit);
        }
        if (consistentRead != null) {
            serializer.writeBoolean($SCHEMA_CONSISTENT_READ, consistentRead);
        }
        if (keyConditions != null) {
            serializer.writeMap($SCHEMA_KEY_CONDITIONS, keyConditions, keyConditions.size(), SharedSerde.KeyConditionsSerializer.INSTANCE);
        }
        if (queryFilter != null) {
            serializer.writeMap($SCHEMA_QUERY_FILTER, queryFilter, queryFilter.size(), SharedSerde.FilterConditionMapSerializer.INSTANCE);
        }
        if (conditionalOperator != null) {
            serializer.writeString($SCHEMA_CONDITIONAL_OPERATOR, conditionalOperator.getValue());
        }
        if (scanIndexForward != null) {
            serializer.writeBoolean($SCHEMA_SCAN_INDEX_FORWARD, scanIndexForward);
        }
        if (exclusiveStartKey != null) {
            serializer.writeMap($SCHEMA_EXCLUSIVE_START_KEY, exclusiveStartKey, exclusiveStartKey.size(), SharedSerde.KeySerializer.INSTANCE);
        }
        if (returnConsumedCapacity != null) {
            serializer.writeString($SCHEMA_RETURN_CONSUMED_CAPACITY, returnConsumedCapacity.getValue());
        }
        if (projectionExpression != null) {
            serializer.writeString($SCHEMA_PROJECTION_EXPRESSION, projectionExpression);
        }
        if (filterExpression != null) {
            serializer.writeString($SCHEMA_FILTER_EXPRESSION, filterExpression);
        }
        if (keyConditionExpression != null) {
            serializer.writeString($SCHEMA_KEY_CONDITION_EXPRESSION, keyConditionExpression);
        }
        if (expressionAttributeNames != null) {
            serializer.writeMap($SCHEMA_EXPRESSION_ATTRIBUTE_NAMES, expressionAttributeNames, expressionAttributeNames.size(), SharedSerde.ExpressionAttributeNameMapSerializer.INSTANCE);
        }
        if (expressionAttributeValues != null) {
            serializer.writeMap($SCHEMA_EXPRESSION_ATTRIBUTE_VALUES, expressionAttributeValues, expressionAttributeValues.size(), SharedSerde.ExpressionAttributeValueMapSerializer.INSTANCE);
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T getMemberValue(Schema member) {
        return switch (member.memberIndex()) {
            case 0 -> (T) SchemaUtils.validateSameMember($SCHEMA_TABLE_NAME, member, tableName);
            case 1 -> (T) SchemaUtils.validateSameMember($SCHEMA_INDEX_NAME, member, indexName);
            case 2 -> (T) SchemaUtils.validateSameMember($SCHEMA_SELECT, member, select);
            case 3 -> (T) SchemaUtils.validateSameMember($SCHEMA_ATTRIBUTES_TO_GET, member, attributesToGet);
            case 4 -> (T) SchemaUtils.validateSameMember($SCHEMA_LIMIT, member, limit);
            case 5 -> (T) SchemaUtils.validateSameMember($SCHEMA_CONSISTENT_READ, member, consistentRead);
            case 6 -> (T) SchemaUtils.validateSameMember($SCHEMA_KEY_CONDITIONS, member, keyConditions);
            case 7 -> (T) SchemaUtils.validateSameMember($SCHEMA_QUERY_FILTER, member, queryFilter);
            case 8 -> (T) SchemaUtils.validateSameMember($SCHEMA_CONDITIONAL_OPERATOR, member, conditionalOperator);
            case 9 -> (T) SchemaUtils.validateSameMember($SCHEMA_SCAN_INDEX_FORWARD, member, scanIndexForward);
            case 10 -> (T) SchemaUtils.validateSameMember($SCHEMA_EXCLUSIVE_START_KEY, member, exclusiveStartKey);
            case 11 -> (T) SchemaUtils.validateSameMember($SCHEMA_RETURN_CONSUMED_CAPACITY, member, returnConsumedCapacity);
            case 12 -> (T) SchemaUtils.validateSameMember($SCHEMA_PROJECTION_EXPRESSION, member, projectionExpression);
            case 13 -> (T) SchemaUtils.validateSameMember($SCHEMA_FILTER_EXPRESSION, member, filterExpression);
            case 14 -> (T) SchemaUtils.validateSameMember($SCHEMA_KEY_CONDITION_EXPRESSION, member, keyConditionExpression);
            case 15 -> (T) SchemaUtils.validateSameMember($SCHEMA_EXPRESSION_ATTRIBUTE_NAMES, member, expressionAttributeNames);
            case 16 -> (T) SchemaUtils.validateSameMember($SCHEMA_EXPRESSION_ATTRIBUTE_VALUES, member, expressionAttributeValues);
            default -> throw new IllegalArgumentException("Attempted to get non-existent member: " + member.id());
        };
    }

    /**
     * Create a new builder containing all the current property values of this object.
     *
     * <p><strong>Note:</strong> This method performs only a shallow copy of the original properties.
     *
     * @return a builder for {@link QueryInput}.
     */
    public Builder toBuilder() {
        var builder = new Builder();
        builder.tableName(this.tableName);
        builder.indexName(this.indexName);
        builder.select(this.select);
        builder.attributesToGet(this.attributesToGet);
        builder.limit(this.limit);
        builder.consistentRead(this.consistentRead);
        builder.keyConditions(this.keyConditions);
        builder.queryFilter(this.queryFilter);
        builder.conditionalOperator(this.conditionalOperator);
        builder.scanIndexForward(this.scanIndexForward);
        builder.exclusiveStartKey(this.exclusiveStartKey);
        builder.returnConsumedCapacity(this.returnConsumedCapacity);
        builder.projectionExpression(this.projectionExpression);
        builder.filterExpression(this.filterExpression);
        builder.keyConditionExpression(this.keyConditionExpression);
        builder.expressionAttributeNames(this.expressionAttributeNames);
        builder.expressionAttributeValues(this.expressionAttributeValues);
        return builder;
    }

    /**
     * @return returns a new Builder.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for {@link QueryInput}.
     */
    public static final class Builder implements ShapeBuilder<QueryInput> {
        private final PresenceTracker tracker = PresenceTracker.of($SCHEMA);
        private String tableName;
        private String indexName;
        private Select select;
        private List<String> attributesToGet;
        private Integer limit;
        private Boolean consistentRead;
        private Map<String, Condition> keyConditions;
        private Map<String, Condition> queryFilter;
        private ConditionalOperator conditionalOperator;
        private Boolean scanIndexForward;
        private Map<String, AttributeValue> exclusiveStartKey;
        private ReturnConsumedCapacity returnConsumedCapacity;
        private String projectionExpression;
        private String filterExpression;
        private String keyConditionExpression;
        private Map<String, String> expressionAttributeNames;
        private Map<String, AttributeValue> expressionAttributeValues;

        private Builder() {}

        @Override
        public Schema schema() {
            return $SCHEMA;
        }

        /**
         * The name of the table containing the requested items. You can also provide the Amazon Resource Name (ARN) of the
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
         * The name of an index to query. This index can be any local secondary index or global secondary index on the
         * table. Note that if you use the <code>IndexName</code> parameter, you must also provide <code>TableName.</code>
         *
         * @return this builder.
         */
        public Builder indexName(String indexName) {
            this.indexName = indexName;
            return this;
        }

        /**
         * The attributes to be returned in the result. You can retrieve all item attributes, specific item attributes, the
         * count of matching items, or in the case of an index, some or all of the attributes projected into the index.
         *
         * <ul>
         *   <li>
         *     <code>ALL_ATTRIBUTES</code> - Returns all of the item attributes from the specified table or index. If
         *     you query a local secondary index, then for each matching item in the index, DynamoDB fetches the entire
         *     item from the parent table. If the index is configured to project all item attributes, then all of the
         *     data can be obtained from the local secondary index, and no fetching is required.
         *   </li>
         *   <li>
         *     <code>ALL_PROJECTED_ATTRIBUTES</code> - Allowed only when querying an index. Retrieves all attributes
         *     that have been projected into the index. If the index is configured to project all attributes, this
         *     return value is equivalent to specifying <code>ALL_ATTRIBUTES</code>.
         *   </li>
         *   <li>
         *     <code>COUNT</code> - Returns the number of matching items, rather than the matching items themselves.
         *     Note that this uses the same quantity of read capacity units as getting the items, and is subject to the
         *     same item size calculations.
         *   </li>
         *   <li>
         *     <code>SPECIFIC_ATTRIBUTES</code> - Returns only the attributes listed in <code>ProjectionExpression</code>
         *     . This return value is equivalent to specifying <code>ProjectionExpression</code> without specifying any
         *     value for <code>Select</code>.If you query or scan a local secondary index and request only attributes
         *     that are projected into that index, the operation will read only the index and not the table. If any of
         *     the requested attributes are not projected into the local secondary index, DynamoDB fetches each of these
         *     attributes from the parent table. This extra fetching incurs additional throughput cost and latency.If
         *     you query or scan a global secondary index, you can only request attributes that are projected into the
         *     index. Global secondary index queries cannot fetch attributes from the parent table.
         *   </li>
         * </ul>
         *
         * <p>If neither <code>Select</code> nor <code>ProjectionExpression</code> are specified, DynamoDB defaults to <code>
         * ALL_ATTRIBUTES</code> when accessing a table, and <code>ALL_PROJECTED_ATTRIBUTES</code> when accessing an index.
         * You cannot use both <code>Select</code> and <code>ProjectionExpression</code> together in a single request,
         * unless the value for <code>Select</code> is <code>SPECIFIC_ATTRIBUTES</code>. (This usage is equivalent to
         * specifying <code>ProjectionExpression</code> without any value for <code>Select</code>.)
         *
         * <p>If you use the <code>ProjectionExpression</code> parameter, then the value for <code>Select</code> can only be
         * <code>SPECIFIC_ATTRIBUTES</code>. Any other value for <code>Select</code> will return an error.
         *
         * @return this builder.
         */
        public Builder select(Select select) {
            this.select = select;
            return this;
        }

        /**
         * This is a legacy parameter. Use <code>ProjectionExpression</code> instead. For more information, see <a href="https://docs.aws.amazon.com/amazondynamodb/latest/developerguide/LegacyConditionalParameters.AttributesToGet.html">
         * AttributesToGet</a> in the <i>Amazon DynamoDB Developer Guide</i>.
         *
         * @return this builder.
         */
        public Builder attributesToGet(List<String> attributesToGet) {
            this.attributesToGet = attributesToGet;
            return this;
        }

        /**
         * The maximum number of items to evaluate (not necessarily the number of matching items). If DynamoDB processes the
         * number of items up to the limit while processing the results, it stops the operation and returns the matching
         * values up to that point, and a key in <code>LastEvaluatedKey</code> to apply in a subsequent operation, so that
         * you can pick up where you left off. Also, if the processed dataset size exceeds 1 MB before DynamoDB reaches this
         * limit, it stops the operation and returns the matching values up to the limit, and a key in <code>
         * LastEvaluatedKey</code> to apply in a subsequent operation to continue the operation. For more information, see <a href="https://docs.aws.amazon.com/amazondynamodb/latest/developerguide/QueryAndScan.html">
         * Query and Scan</a> in the <i>Amazon DynamoDB Developer Guide</i>.
         *
         * @return this builder.
         */
        public Builder limit(Integer limit) {
            this.limit = limit;
            return this;
        }

        /**
         * Determines the read consistency model: If set to <code>true</code>, then the operation uses strongly consistent
         * reads; otherwise, the operation uses eventually consistent reads.
         *
         * <p>Strongly consistent reads are not supported on global secondary indexes. If you query a global secondary index
         * with <code>ConsistentRead</code> set to <code>true</code>, you will receive a <code>ValidationException</code>.
         *
         * @return this builder.
         */
        public Builder consistentRead(Boolean consistentRead) {
            this.consistentRead = consistentRead;
            return this;
        }

        /**
         * This is a legacy parameter. Use <code>KeyConditionExpression</code> instead. For more information, see <a href="https://docs.aws.amazon.com/amazondynamodb/latest/developerguide/LegacyConditionalParameters.KeyConditions.html">
         * KeyConditions</a> in the <i>Amazon DynamoDB Developer Guide</i>.
         *
         * @return this builder.
         */
        public Builder keyConditions(Map<String, Condition> keyConditions) {
            this.keyConditions = keyConditions;
            return this;
        }

        /**
         * This is a legacy parameter. Use <code>FilterExpression</code> instead. For more information, see <a href="https://docs.aws.amazon.com/amazondynamodb/latest/developerguide/LegacyConditionalParameters.QueryFilter.html">QueryFilter</a>
         * in the <i>Amazon DynamoDB Developer Guide</i>.
         *
         * @return this builder.
         */
        public Builder queryFilter(Map<String, Condition> queryFilter) {
            this.queryFilter = queryFilter;
            return this;
        }

        /**
         * This is a legacy parameter. Use <code>FilterExpression</code> instead. For more information, see <a href="https://docs.aws.amazon.com/amazondynamodb/latest/developerguide/LegacyConditionalParameters.ConditionalOperator.html">
         * ConditionalOperator</a> in the <i>Amazon DynamoDB Developer Guide</i>.
         *
         * @return this builder.
         */
        public Builder conditionalOperator(ConditionalOperator conditionalOperator) {
            this.conditionalOperator = conditionalOperator;
            return this;
        }

        /**
         * Specifies the order for index traversal: If <code>true</code> (default), the traversal is performed in ascending
         * order; if <code>false</code>, the traversal is performed in descending order.
         *
         * <p>Items with the same partition key value are stored in sorted order by sort key. If the sort key data type is
         * Number, the results are stored in numeric order. For type String, the results are stored in order of UTF-8 bytes.
         * For type Binary, DynamoDB treats each byte of the binary data as unsigned.
         *
         * <p>If <code>ScanIndexForward</code> is <code>true</code>, DynamoDB returns the results in the order in which they
         * are stored (by sort key value). This is the default behavior. If <code>ScanIndexForward</code> is <code>false</code>
         * , DynamoDB reads the results in reverse order by sort key value, and then returns the results to the client.
         *
         * @return this builder.
         */
        public Builder scanIndexForward(Boolean scanIndexForward) {
            this.scanIndexForward = scanIndexForward;
            return this;
        }

        /**
         * The primary key of the first item that this operation will evaluate. Use the value that was returned for <code>
         * LastEvaluatedKey</code> in the previous operation.
         *
         * <p>The data type for <code>ExclusiveStartKey</code> must be String, Number, or Binary. No set data types are
         * allowed.
         *
         * @return this builder.
         */
        public Builder exclusiveStartKey(Map<String, AttributeValue> exclusiveStartKey) {
            this.exclusiveStartKey = exclusiveStartKey;
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
         * A string that identifies one or more attributes to retrieve from the table. These attributes can include scalars,
         * sets, or elements of a JSON document. The attributes in the expression must be separated by commas.
         *
         * <p>If no attribute names are specified, then all attributes will be returned. If any of the requested attributes
         * are not found, they will not appear in the result.
         *
         * <p>For more information, see <a href="https://docs.aws.amazon.com/amazondynamodb/latest/developerguide/Expressions.AccessingItemAttributes.html">Accessing Item Attributes</a> in the <i>Amazon DynamoDB Developer Guide</i>.
         *
         * @return this builder.
         */
        public Builder projectionExpression(String projectionExpression) {
            this.projectionExpression = projectionExpression;
            return this;
        }

        /**
         * A string that contains conditions that DynamoDB applies after the <code>Query</code> operation, but before the
         * data is returned to you. Items that do not satisfy the <code>FilterExpression</code> criteria are not returned.
         *
         * <p>A <code>FilterExpression</code> does not allow key attributes. You cannot define a filter expression based on
         * a partition key or a sort key.
         *
         * <p>A <code>FilterExpression</code> is applied after the items have already been read; the process of filtering
         * does not consume any additional read capacity units.
         *
         * <p>For more information, see <a href="https://docs.aws.amazon.com/amazondynamodb/latest/developerguide/Query.FilterExpression.html">Filter Expressions</a> in the <i>Amazon DynamoDB Developer Guide</i>.
         *
         * @return this builder.
         */
        public Builder filterExpression(String filterExpression) {
            this.filterExpression = filterExpression;
            return this;
        }

        /**
         * The condition that specifies the key values for items to be retrieved by the <code>Query</code> action.
         *
         * <p>The condition must perform an equality test on a single partition key value.
         *
         * <p>The condition can optionally perform one of several comparison tests on a single sort key value. This allows <code>
         * Query</code> to retrieve one item with a given partition key value and sort key value, or several items that have
         * the same partition key value but different sort key values.
         *
         * <p>The partition key equality test is required, and must be specified in the following format:
         *
         * <p><code>partitionKeyName</code><i>=</i><code>:partitionkeyval</code>
         *
         * <p>If you also want to provide a condition for the sort key, it must be combined using <code>AND</code> with the
         * condition for the sort key. Following is an example, using the <b>=</b> comparison operator for the sort key:
         *
         * <p><code>partitionKeyName</code><code>=</code><code>:partitionkeyval</code><code>AND</code><code>sortKeyName</code><code>
         * =</code><code>:sortkeyval</code>
         *
         * <p>Valid comparisons for the sort key condition are as follows:
         *
         * <ul>
         *   <li>
         *     <code>sortKeyName</code><code>=</code><code>:sortkeyval</code> - true if the sort key value is equal to <code>
         *     :sortkeyval</code>.
         *   </li>
         *   <li>
         *     <code>sortKeyName</code><code>&lt;</code><code>:sortkeyval</code> - true if the sort key value is less
         *     than <code>:sortkeyval</code>.
         *   </li>
         *   <li>
         *     <code>sortKeyName</code><code>&lt;=</code><code>:sortkeyval</code> - true if the sort key value is less
         *     than or equal to <code>:sortkeyval</code>.
         *   </li>
         *   <li>
         *     <code>sortKeyName</code><code>&gt;</code><code>:sortkeyval</code> - true if the sort key value is greater
         *     than <code>:sortkeyval</code>.
         *   </li>
         *   <li>
         *     <code>sortKeyName</code><code>&gt;= </code><code>:sortkeyval</code> - true if the sort key value is
         *     greater than or equal to <code>:sortkeyval</code>.
         *   </li>
         *   <li>
         *     <code>sortKeyName</code><code>BETWEEN</code><code>:sortkeyval1</code><code>AND</code><code>:sortkeyval2</code>
         *     - true if the sort key value is greater than or equal to <code>:sortkeyval1</code>, and less than or
         *     equal to <code>:sortkeyval2</code>.
         *   </li>
         *   <li>
         *     <code>begins_with (</code><code>sortKeyName</code>, <code>:sortkeyval</code><code>)</code> - true if the
         *     sort key value begins with a particular operand. (You cannot use this function with a sort key that is of
         *     type Number.) Note that the function name <code>begins_with</code> is case-sensitive.
         *   </li>
         * </ul>
         *
         * <p>Use the <code>ExpressionAttributeValues</code> parameter to replace tokens such as <code>:partitionval</code>
         * and <code>:sortval</code> with actual values at runtime.
         *
         * <p>You can optionally use the <code>ExpressionAttributeNames</code> parameter to replace the names of the
         * partition key and sort key with placeholder tokens. This option might be necessary if an attribute name conflicts
         * with a DynamoDB reserved word. For example, the following <code>KeyConditionExpression</code> parameter causes an
         * error because <i>Size</i> is a reserved word:
         *
         * <ul>
         *   <li>
         *     <code>Size = :myval</code>
         *   </li>
         * </ul>
         *
         * <p>To work around this, define a placeholder (such a <code>#S</code>) to represent the attribute name <i>Size</i>
         * . <code>KeyConditionExpression</code> then is as follows:
         *
         * <ul>
         *   <li>
         *     <code>#S = :myval</code>
         *   </li>
         * </ul>
         *
         * <p>For a list of reserved words, see <a href="https://docs.aws.amazon.com/amazondynamodb/latest/developerguide/ReservedWords.html">Reserved Words</a> in the <i>Amazon DynamoDB Developer Guide</i>.
         *
         * <p>For more information on <code>ExpressionAttributeNames</code> and <code>ExpressionAttributeValues</code>, see <a href="https://docs.aws.amazon.com/amazondynamodb/latest/developerguide/ExpressionPlaceholders.html">
         * Using Placeholders for Attribute Names and Values</a> in the <i>Amazon DynamoDB Developer Guide</i>.
         *
         * @return this builder.
         */
        public Builder keyConditionExpression(String keyConditionExpression) {
            this.keyConditionExpression = keyConditionExpression;
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
         * <p>For more information on expression attribute values, see <a href="https://docs.aws.amazon.com/amazondynamodb/latest/developerguide/Expressions.SpecifyingConditions.html">Specifying Conditions</a> in the <i>Amazon
         * DynamoDB Developer Guide</i>.
         *
         * @return this builder.
         */
        public Builder expressionAttributeValues(Map<String, AttributeValue> expressionAttributeValues) {
            this.expressionAttributeValues = expressionAttributeValues;
            return this;
        }

        @Override
        public QueryInput build() {
            tracker.validate();
            return new QueryInput(this);
        }

        @Override
        @SuppressWarnings("unchecked")
        public void setMemberValue(Schema member, Object value) {
            switch (member.memberIndex()) {
                case 0 -> tableName((String) SchemaUtils.validateSameMember($SCHEMA_TABLE_NAME, member, value));
                case 1 -> indexName((String) SchemaUtils.validateSameMember($SCHEMA_INDEX_NAME, member, value));
                case 2 -> select((Select) SchemaUtils.validateSameMember($SCHEMA_SELECT, member, value));
                case 3 -> attributesToGet((List<String>) SchemaUtils.validateSameMember($SCHEMA_ATTRIBUTES_TO_GET, member, value));
                case 4 -> limit((Integer) SchemaUtils.validateSameMember($SCHEMA_LIMIT, member, value));
                case 5 -> consistentRead((Boolean) SchemaUtils.validateSameMember($SCHEMA_CONSISTENT_READ, member, value));
                case 6 -> keyConditions((Map<String, Condition>) SchemaUtils.validateSameMember($SCHEMA_KEY_CONDITIONS, member, value));
                case 7 -> queryFilter((Map<String, Condition>) SchemaUtils.validateSameMember($SCHEMA_QUERY_FILTER, member, value));
                case 8 -> conditionalOperator((ConditionalOperator) SchemaUtils.validateSameMember($SCHEMA_CONDITIONAL_OPERATOR, member, value));
                case 9 -> scanIndexForward((Boolean) SchemaUtils.validateSameMember($SCHEMA_SCAN_INDEX_FORWARD, member, value));
                case 10 -> exclusiveStartKey((Map<String, AttributeValue>) SchemaUtils.validateSameMember($SCHEMA_EXCLUSIVE_START_KEY, member, value));
                case 11 -> returnConsumedCapacity((ReturnConsumedCapacity) SchemaUtils.validateSameMember($SCHEMA_RETURN_CONSUMED_CAPACITY, member, value));
                case 12 -> projectionExpression((String) SchemaUtils.validateSameMember($SCHEMA_PROJECTION_EXPRESSION, member, value));
                case 13 -> filterExpression((String) SchemaUtils.validateSameMember($SCHEMA_FILTER_EXPRESSION, member, value));
                case 14 -> keyConditionExpression((String) SchemaUtils.validateSameMember($SCHEMA_KEY_CONDITION_EXPRESSION, member, value));
                case 15 -> expressionAttributeNames((Map<String, String>) SchemaUtils.validateSameMember($SCHEMA_EXPRESSION_ATTRIBUTE_NAMES, member, value));
                case 16 -> expressionAttributeValues((Map<String, AttributeValue>) SchemaUtils.validateSameMember($SCHEMA_EXPRESSION_ATTRIBUTE_VALUES, member, value));
                default -> ShapeBuilder.super.setMemberValue(member, value);
            }
        }

        @Override
        public ShapeBuilder<QueryInput> errorCorrection() {
            if (tracker.allSet()) {
                return this;
            }
            if (!tracker.checkMember($SCHEMA_TABLE_NAME)) {
                tableName("");
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
                    case 2 -> builder.select(Select.builder().deserializeMember(de, member).build());
                    case 3 -> builder.attributesToGet(SharedSerde.deserializeAttributeNameList(member, de));
                    case 4 -> builder.limit(de.readInteger(member));
                    case 5 -> builder.consistentRead(de.readBoolean(member));
                    case 6 -> builder.keyConditions(SharedSerde.deserializeKeyConditions(member, de));
                    case 7 -> builder.queryFilter(SharedSerde.deserializeFilterConditionMap(member, de));
                    case 8 -> builder.conditionalOperator(ConditionalOperator.builder().deserializeMember(de, member).build());
                    case 9 -> builder.scanIndexForward(de.readBoolean(member));
                    case 10 -> builder.exclusiveStartKey(SharedSerde.deserializeKey(member, de));
                    case 11 -> builder.returnConsumedCapacity(ReturnConsumedCapacity.builder().deserializeMember(de, member).build());
                    case 12 -> builder.projectionExpression(de.readString(member));
                    case 13 -> builder.filterExpression(de.readString(member));
                    case 14 -> builder.keyConditionExpression(de.readString(member));
                    case 15 -> builder.expressionAttributeNames(SharedSerde.deserializeExpressionAttributeNameMap(member, de));
                    case 16 -> builder.expressionAttributeValues(SharedSerde.deserializeExpressionAttributeValueMap(member, de));
                    default -> throw new IllegalArgumentException("Unexpected member: " + member.memberName());
                }
            }
        }
    }
}
