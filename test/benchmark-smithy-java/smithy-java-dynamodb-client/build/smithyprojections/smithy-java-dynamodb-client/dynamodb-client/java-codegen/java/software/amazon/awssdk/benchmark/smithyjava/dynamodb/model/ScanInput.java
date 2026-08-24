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
 * Represents the input of a <code>Scan</code> operation.
 */
@SmithyGenerated
public final class ScanInput implements SerializableStruct {

    public static final Schema $SCHEMA = Schemas.SCAN_INPUT;
    private static final Schema $SCHEMA_TABLE_NAME = $SCHEMA.member("TableName");
    private static final Schema $SCHEMA_INDEX_NAME = $SCHEMA.member("IndexName");
    private static final Schema $SCHEMA_ATTRIBUTES_TO_GET = $SCHEMA.member("AttributesToGet");
    private static final Schema $SCHEMA_LIMIT = $SCHEMA.member("Limit");
    private static final Schema $SCHEMA_SELECT = $SCHEMA.member("Select");
    private static final Schema $SCHEMA_SCAN_FILTER = $SCHEMA.member("ScanFilter");
    private static final Schema $SCHEMA_CONDITIONAL_OPERATOR = $SCHEMA.member("ConditionalOperator");
    private static final Schema $SCHEMA_EXCLUSIVE_START_KEY = $SCHEMA.member("ExclusiveStartKey");
    private static final Schema $SCHEMA_RETURN_CONSUMED_CAPACITY = $SCHEMA.member("ReturnConsumedCapacity");
    private static final Schema $SCHEMA_TOTAL_SEGMENTS = $SCHEMA.member("TotalSegments");
    private static final Schema $SCHEMA_SEGMENT = $SCHEMA.member("Segment");
    private static final Schema $SCHEMA_PROJECTION_EXPRESSION = $SCHEMA.member("ProjectionExpression");
    private static final Schema $SCHEMA_FILTER_EXPRESSION = $SCHEMA.member("FilterExpression");
    private static final Schema $SCHEMA_EXPRESSION_ATTRIBUTE_NAMES = $SCHEMA.member("ExpressionAttributeNames");
    private static final Schema $SCHEMA_EXPRESSION_ATTRIBUTE_VALUES = $SCHEMA.member("ExpressionAttributeValues");
    private static final Schema $SCHEMA_CONSISTENT_READ = $SCHEMA.member("ConsistentRead");

    public static final ShapeId $ID = $SCHEMA.id();

    private final transient String tableName;
    private final transient String indexName;
    private final transient List<String> attributesToGet;
    private final transient Integer limit;
    private final transient Select select;
    private final transient Map<String, Condition> scanFilter;
    private final transient ConditionalOperator conditionalOperator;
    private final transient Map<String, AttributeValue> exclusiveStartKey;
    private final transient ReturnConsumedCapacity returnConsumedCapacity;
    private final transient Integer totalSegments;
    private final transient Integer segment;
    private final transient String projectionExpression;
    private final transient String filterExpression;
    private final transient Map<String, String> expressionAttributeNames;
    private final transient Map<String, AttributeValue> expressionAttributeValues;
    private final transient Boolean consistentRead;

    private ScanInput(Builder builder) {
        this.tableName = builder.tableName;
        this.indexName = builder.indexName;
        this.attributesToGet = builder.attributesToGet == null ? null : Collections.unmodifiableList(builder.attributesToGet);
        this.limit = builder.limit;
        this.select = builder.select;
        this.scanFilter = builder.scanFilter == null ? null : Collections.unmodifiableMap(builder.scanFilter);
        this.conditionalOperator = builder.conditionalOperator;
        this.exclusiveStartKey = builder.exclusiveStartKey == null ? null : Collections.unmodifiableMap(builder.exclusiveStartKey);
        this.returnConsumedCapacity = builder.returnConsumedCapacity;
        this.totalSegments = builder.totalSegments;
        this.segment = builder.segment;
        this.projectionExpression = builder.projectionExpression;
        this.filterExpression = builder.filterExpression;
        this.expressionAttributeNames = builder.expressionAttributeNames == null ? null : Collections.unmodifiableMap(builder.expressionAttributeNames);
        this.expressionAttributeValues = builder.expressionAttributeValues == null ? null : Collections.unmodifiableMap(builder.expressionAttributeValues);
        this.consistentRead = builder.consistentRead;
    }

    /**
     * The name of the table containing the requested items or if you provide <code>IndexName</code>, the name of the
     * table to which that index belongs.
     *
     * <p>You can also provide the Amazon Resource Name (ARN) of the table in this parameter.
     */
    public String getTableName() {
        return tableName;
    }

    /**
     * The name of a secondary index to scan. This index can be any local secondary index or global secondary index.
     * Note that if you use the <code>IndexName</code> parameter, you must also provide <code>TableName</code>.
     */
    public String getIndexName() {
        return indexName;
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
     * Working with Queries</a> in the <i>Amazon DynamoDB Developer Guide</i>.
     */
    public Integer getLimit() {
        return limit;
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
     *     that are projected into that index, the operation reads only the index and not the table. If any of the
     *     requested attributes are not projected into the local secondary index, DynamoDB fetches each of these
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
     * This is a legacy parameter. Use <code>FilterExpression</code> instead. For more information, see <a href="https://docs.aws.amazon.com/amazondynamodb/latest/developerguide/LegacyConditionalParameters.ScanFilter.html">ScanFilter</a>
     * in the <i>Amazon DynamoDB Developer Guide</i>.
     */
    public Map<String, Condition> getScanFilter() {
        if (scanFilter == null) {
            return Collections.emptyMap();
        }
        return scanFilter;
    }

    public boolean hasScanFilter() {
        return scanFilter != null;
    }

    /**
     * This is a legacy parameter. Use <code>FilterExpression</code> instead. For more information, see <a href="https://docs.aws.amazon.com/amazondynamodb/latest/developerguide/LegacyConditionalParameters.ConditionalOperator.html">
     * ConditionalOperator</a> in the <i>Amazon DynamoDB Developer Guide</i>.
     */
    public ConditionalOperator getConditionalOperator() {
        return conditionalOperator;
    }

    /**
     * The primary key of the first item that this operation will evaluate. Use the value that was returned for <code>
     * LastEvaluatedKey</code> in the previous operation.
     *
     * <p>The data type for <code>ExclusiveStartKey</code> must be String, Number or Binary. No set data types are
     * allowed.
     *
     * <p>In a parallel scan, a <code>Scan</code> request that includes <code>ExclusiveStartKey</code> must specify the
     * same segment whose previous <code>Scan</code> returned the corresponding value of <code>LastEvaluatedKey</code>.
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
     * For a parallel <code>Scan</code> request, <code>TotalSegments</code> represents the total number of segments into
     * which the <code>Scan</code> operation will be divided. The value of <code>TotalSegments</code> corresponds to the
     * number of application workers that will perform the parallel scan. For example, if you want to use four
     * application threads to scan a table or an index, specify a <code>TotalSegments</code> value of 4.
     *
     * <p>The value for <code>TotalSegments</code> must be greater than or equal to 1, and less than or equal to
     * 1000000. If you specify a <code>TotalSegments</code> value of 1, the <code>Scan</code> operation will be
     * sequential rather than parallel.
     *
     * <p>If you specify <code>TotalSegments</code>, you must also specify <code>Segment</code>.
     */
    public Integer getTotalSegments() {
        return totalSegments;
    }

    /**
     * For a parallel <code>Scan</code> request, <code>Segment</code> identifies an individual segment to be scanned by
     * an application worker.
     *
     * <p>Segment IDs are zero-based, so the first segment is always 0. For example, if you want to use four application
     * threads to scan a table or an index, then the first thread specifies a <code>Segment</code> value of 0, the
     * second thread specifies 1, and so on.
     *
     * <p>The value of <code>LastEvaluatedKey</code> returned from a parallel <code>Scan</code> request must be used as <code>
     * ExclusiveStartKey</code> with the same segment ID in a subsequent <code>Scan</code> operation.
     *
     * <p>The value for <code>Segment</code> must be greater than or equal to 0, and less than the value provided for <code>
     * TotalSegments</code>.
     *
     * <p>If you provide <code>Segment</code>, you must also provide <code>TotalSegments</code>.
     */
    public Integer getSegment() {
        return segment;
    }

    /**
     * A string that identifies one or more attributes to retrieve from the specified table or index. These attributes
     * can include scalars, sets, or elements of a JSON document. The attributes in the expression must be separated by
     * commas.
     *
     * <p>If no attribute names are specified, then all attributes will be returned. If any of the requested attributes
     * are not found, they will not appear in the result.
     *
     * <p>For more information, see <a href="https://docs.aws.amazon.com/amazondynamodb/latest/developerguide/Expressions.AccessingItemAttributes.html">Specifying Item Attributes</a> in the <i>Amazon DynamoDB Developer Guide</i>.
     */
    public String getProjectionExpression() {
        return projectionExpression;
    }

    /**
     * A string that contains conditions that DynamoDB applies after the <code>Scan</code> operation, but before the
     * data is returned to you. Items that do not satisfy the <code>FilterExpression</code> criteria are not returned.
     *
     * <p>A <code>FilterExpression</code> is applied after the items have already been read; the process of filtering
     * does not consume any additional read capacity units.
     *
     * <p>For more information, see <a href="https://docs.aws.amazon.com/amazondynamodb/latest/developerguide/Scan.html#Scan.FilterExpression">Filter Expressions</a> in the <i>Amazon DynamoDB Developer Guide</i>.
     */
    public String getFilterExpression() {
        return filterExpression;
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
     * A Boolean value that determines the read consistency model during the scan:
     *
     * <ul>
     *   <li>
     *     If <code>ConsistentRead</code> is <code>false</code>, then the data returned from <code>Scan</code> might
     *     not contain the results from other recently completed write operations (<code>PutItem</code>, <code>
     *     UpdateItem</code>, or <code>DeleteItem</code>).
     *   </li>
     *   <li>
     *     If <code>ConsistentRead</code> is <code>true</code>, then all of the write operations that completed
     *     before the <code>Scan</code> began are guaranteed to be contained in the <code>Scan</code> response.
     *   </li>
     * </ul>
     *
     * <p>The default setting for <code>ConsistentRead</code> is <code>false</code>.
     *
     * <p>The <code>ConsistentRead</code> parameter is not supported on global secondary indexes. If you scan a global
     * secondary index with <code>ConsistentRead</code> set to true, you will receive a <code>ValidationException</code>
     * .
     */
    public Boolean isConsistentRead() {
        return consistentRead;
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
        ScanInput that = (ScanInput) other;
        return Objects.equals(this.consistentRead, that.consistentRead)
               && Objects.equals(this.limit, that.limit)
               && Objects.equals(this.totalSegments, that.totalSegments)
               && Objects.equals(this.segment, that.segment)
               && Objects.equals(this.tableName, that.tableName)
               && Objects.equals(this.indexName, that.indexName)
               && Objects.equals(this.projectionExpression, that.projectionExpression)
               && Objects.equals(this.filterExpression, that.filterExpression)
               && Objects.equals(this.select, that.select)
               && Objects.equals(this.conditionalOperator, that.conditionalOperator)
               && Objects.equals(this.returnConsumedCapacity, that.returnConsumedCapacity)
               && Objects.equals(this.attributesToGet, that.attributesToGet)
               && Objects.equals(this.scanFilter, that.scanFilter)
               && Objects.equals(this.exclusiveStartKey, that.exclusiveStartKey)
               && Objects.equals(this.expressionAttributeNames, that.expressionAttributeNames)
               && Objects.equals(this.expressionAttributeValues, that.expressionAttributeValues);
    }

    @Override
    public int hashCode() {
        int $hc = Objects.hashCode(tableName);
        $hc = 31 * $hc + Objects.hashCode(indexName);
        $hc = 31 * $hc + Objects.hashCode(attributesToGet);
        $hc = 31 * $hc + Objects.hashCode(limit);
        $hc = 31 * $hc + Objects.hashCode(select);
        $hc = 31 * $hc + Objects.hashCode(scanFilter);
        $hc = 31 * $hc + Objects.hashCode(conditionalOperator);
        $hc = 31 * $hc + Objects.hashCode(exclusiveStartKey);
        $hc = 31 * $hc + Objects.hashCode(returnConsumedCapacity);
        $hc = 31 * $hc + Objects.hashCode(totalSegments);
        $hc = 31 * $hc + Objects.hashCode(segment);
        $hc = 31 * $hc + Objects.hashCode(projectionExpression);
        $hc = 31 * $hc + Objects.hashCode(filterExpression);
        $hc = 31 * $hc + Objects.hashCode(expressionAttributeNames);
        $hc = 31 * $hc + Objects.hashCode(expressionAttributeValues);
        $hc = 31 * $hc + Objects.hashCode(consistentRead);
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
        if (attributesToGet != null) {
            serializer.writeList($SCHEMA_ATTRIBUTES_TO_GET, attributesToGet, attributesToGet.size(), SharedSerde.AttributeNameListSerializer.INSTANCE);
        }
        if (limit != null) {
            serializer.writeInteger($SCHEMA_LIMIT, limit);
        }
        if (select != null) {
            serializer.writeString($SCHEMA_SELECT, select.getValue());
        }
        if (scanFilter != null) {
            serializer.writeMap($SCHEMA_SCAN_FILTER, scanFilter, scanFilter.size(), SharedSerde.FilterConditionMapSerializer.INSTANCE);
        }
        if (conditionalOperator != null) {
            serializer.writeString($SCHEMA_CONDITIONAL_OPERATOR, conditionalOperator.getValue());
        }
        if (exclusiveStartKey != null) {
            serializer.writeMap($SCHEMA_EXCLUSIVE_START_KEY, exclusiveStartKey, exclusiveStartKey.size(), SharedSerde.KeySerializer.INSTANCE);
        }
        if (returnConsumedCapacity != null) {
            serializer.writeString($SCHEMA_RETURN_CONSUMED_CAPACITY, returnConsumedCapacity.getValue());
        }
        if (totalSegments != null) {
            serializer.writeInteger($SCHEMA_TOTAL_SEGMENTS, totalSegments);
        }
        if (segment != null) {
            serializer.writeInteger($SCHEMA_SEGMENT, segment);
        }
        if (projectionExpression != null) {
            serializer.writeString($SCHEMA_PROJECTION_EXPRESSION, projectionExpression);
        }
        if (filterExpression != null) {
            serializer.writeString($SCHEMA_FILTER_EXPRESSION, filterExpression);
        }
        if (expressionAttributeNames != null) {
            serializer.writeMap($SCHEMA_EXPRESSION_ATTRIBUTE_NAMES, expressionAttributeNames, expressionAttributeNames.size(), SharedSerde.ExpressionAttributeNameMapSerializer.INSTANCE);
        }
        if (expressionAttributeValues != null) {
            serializer.writeMap($SCHEMA_EXPRESSION_ATTRIBUTE_VALUES, expressionAttributeValues, expressionAttributeValues.size(), SharedSerde.ExpressionAttributeValueMapSerializer.INSTANCE);
        }
        if (consistentRead != null) {
            serializer.writeBoolean($SCHEMA_CONSISTENT_READ, consistentRead);
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T getMemberValue(Schema member) {
        return switch (member.memberIndex()) {
            case 0 -> (T) SchemaUtils.validateSameMember($SCHEMA_TABLE_NAME, member, tableName);
            case 1 -> (T) SchemaUtils.validateSameMember($SCHEMA_INDEX_NAME, member, indexName);
            case 2 -> (T) SchemaUtils.validateSameMember($SCHEMA_ATTRIBUTES_TO_GET, member, attributesToGet);
            case 3 -> (T) SchemaUtils.validateSameMember($SCHEMA_LIMIT, member, limit);
            case 4 -> (T) SchemaUtils.validateSameMember($SCHEMA_SELECT, member, select);
            case 5 -> (T) SchemaUtils.validateSameMember($SCHEMA_SCAN_FILTER, member, scanFilter);
            case 6 -> (T) SchemaUtils.validateSameMember($SCHEMA_CONDITIONAL_OPERATOR, member, conditionalOperator);
            case 7 -> (T) SchemaUtils.validateSameMember($SCHEMA_EXCLUSIVE_START_KEY, member, exclusiveStartKey);
            case 8 -> (T) SchemaUtils.validateSameMember($SCHEMA_RETURN_CONSUMED_CAPACITY, member, returnConsumedCapacity);
            case 9 -> (T) SchemaUtils.validateSameMember($SCHEMA_TOTAL_SEGMENTS, member, totalSegments);
            case 10 -> (T) SchemaUtils.validateSameMember($SCHEMA_SEGMENT, member, segment);
            case 11 -> (T) SchemaUtils.validateSameMember($SCHEMA_PROJECTION_EXPRESSION, member, projectionExpression);
            case 12 -> (T) SchemaUtils.validateSameMember($SCHEMA_FILTER_EXPRESSION, member, filterExpression);
            case 13 -> (T) SchemaUtils.validateSameMember($SCHEMA_EXPRESSION_ATTRIBUTE_NAMES, member, expressionAttributeNames);
            case 14 -> (T) SchemaUtils.validateSameMember($SCHEMA_EXPRESSION_ATTRIBUTE_VALUES, member, expressionAttributeValues);
            case 15 -> (T) SchemaUtils.validateSameMember($SCHEMA_CONSISTENT_READ, member, consistentRead);
            default -> throw new IllegalArgumentException("Attempted to get non-existent member: " + member.id());
        };
    }

    /**
     * Create a new builder containing all the current property values of this object.
     *
     * <p><strong>Note:</strong> This method performs only a shallow copy of the original properties.
     *
     * @return a builder for {@link ScanInput}.
     */
    public Builder toBuilder() {
        var builder = new Builder();
        builder.tableName(this.tableName);
        builder.indexName(this.indexName);
        builder.attributesToGet(this.attributesToGet);
        builder.limit(this.limit);
        builder.select(this.select);
        builder.scanFilter(this.scanFilter);
        builder.conditionalOperator(this.conditionalOperator);
        builder.exclusiveStartKey(this.exclusiveStartKey);
        builder.returnConsumedCapacity(this.returnConsumedCapacity);
        builder.totalSegments(this.totalSegments);
        builder.segment(this.segment);
        builder.projectionExpression(this.projectionExpression);
        builder.filterExpression(this.filterExpression);
        builder.expressionAttributeNames(this.expressionAttributeNames);
        builder.expressionAttributeValues(this.expressionAttributeValues);
        builder.consistentRead(this.consistentRead);
        return builder;
    }

    /**
     * @return returns a new Builder.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for {@link ScanInput}.
     */
    public static final class Builder implements ShapeBuilder<ScanInput> {
        private final PresenceTracker tracker = PresenceTracker.of($SCHEMA);
        private String tableName;
        private String indexName;
        private List<String> attributesToGet;
        private Integer limit;
        private Select select;
        private Map<String, Condition> scanFilter;
        private ConditionalOperator conditionalOperator;
        private Map<String, AttributeValue> exclusiveStartKey;
        private ReturnConsumedCapacity returnConsumedCapacity;
        private Integer totalSegments;
        private Integer segment;
        private String projectionExpression;
        private String filterExpression;
        private Map<String, String> expressionAttributeNames;
        private Map<String, AttributeValue> expressionAttributeValues;
        private Boolean consistentRead;

        private Builder() {}

        @Override
        public Schema schema() {
            return $SCHEMA;
        }

        /**
         * The name of the table containing the requested items or if you provide <code>IndexName</code>, the name of the
         * table to which that index belongs.
         *
         * <p>You can also provide the Amazon Resource Name (ARN) of the table in this parameter.
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
         * The name of a secondary index to scan. This index can be any local secondary index or global secondary index.
         * Note that if you use the <code>IndexName</code> parameter, you must also provide <code>TableName</code>.
         *
         * @return this builder.
         */
        public Builder indexName(String indexName) {
            this.indexName = indexName;
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
         * Working with Queries</a> in the <i>Amazon DynamoDB Developer Guide</i>.
         *
         * @return this builder.
         */
        public Builder limit(Integer limit) {
            this.limit = limit;
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
         *     that are projected into that index, the operation reads only the index and not the table. If any of the
         *     requested attributes are not projected into the local secondary index, DynamoDB fetches each of these
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
         * This is a legacy parameter. Use <code>FilterExpression</code> instead. For more information, see <a href="https://docs.aws.amazon.com/amazondynamodb/latest/developerguide/LegacyConditionalParameters.ScanFilter.html">ScanFilter</a>
         * in the <i>Amazon DynamoDB Developer Guide</i>.
         *
         * @return this builder.
         */
        public Builder scanFilter(Map<String, Condition> scanFilter) {
            this.scanFilter = scanFilter;
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
         * The primary key of the first item that this operation will evaluate. Use the value that was returned for <code>
         * LastEvaluatedKey</code> in the previous operation.
         *
         * <p>The data type for <code>ExclusiveStartKey</code> must be String, Number or Binary. No set data types are
         * allowed.
         *
         * <p>In a parallel scan, a <code>Scan</code> request that includes <code>ExclusiveStartKey</code> must specify the
         * same segment whose previous <code>Scan</code> returned the corresponding value of <code>LastEvaluatedKey</code>.
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
         * For a parallel <code>Scan</code> request, <code>TotalSegments</code> represents the total number of segments into
         * which the <code>Scan</code> operation will be divided. The value of <code>TotalSegments</code> corresponds to the
         * number of application workers that will perform the parallel scan. For example, if you want to use four
         * application threads to scan a table or an index, specify a <code>TotalSegments</code> value of 4.
         *
         * <p>The value for <code>TotalSegments</code> must be greater than or equal to 1, and less than or equal to
         * 1000000. If you specify a <code>TotalSegments</code> value of 1, the <code>Scan</code> operation will be
         * sequential rather than parallel.
         *
         * <p>If you specify <code>TotalSegments</code>, you must also specify <code>Segment</code>.
         *
         * @return this builder.
         */
        public Builder totalSegments(Integer totalSegments) {
            this.totalSegments = totalSegments;
            return this;
        }

        /**
         * For a parallel <code>Scan</code> request, <code>Segment</code> identifies an individual segment to be scanned by
         * an application worker.
         *
         * <p>Segment IDs are zero-based, so the first segment is always 0. For example, if you want to use four application
         * threads to scan a table or an index, then the first thread specifies a <code>Segment</code> value of 0, the
         * second thread specifies 1, and so on.
         *
         * <p>The value of <code>LastEvaluatedKey</code> returned from a parallel <code>Scan</code> request must be used as <code>
         * ExclusiveStartKey</code> with the same segment ID in a subsequent <code>Scan</code> operation.
         *
         * <p>The value for <code>Segment</code> must be greater than or equal to 0, and less than the value provided for <code>
         * TotalSegments</code>.
         *
         * <p>If you provide <code>Segment</code>, you must also provide <code>TotalSegments</code>.
         *
         * @return this builder.
         */
        public Builder segment(Integer segment) {
            this.segment = segment;
            return this;
        }

        /**
         * A string that identifies one or more attributes to retrieve from the specified table or index. These attributes
         * can include scalars, sets, or elements of a JSON document. The attributes in the expression must be separated by
         * commas.
         *
         * <p>If no attribute names are specified, then all attributes will be returned. If any of the requested attributes
         * are not found, they will not appear in the result.
         *
         * <p>For more information, see <a href="https://docs.aws.amazon.com/amazondynamodb/latest/developerguide/Expressions.AccessingItemAttributes.html">Specifying Item Attributes</a> in the <i>Amazon DynamoDB Developer Guide</i>.
         *
         * @return this builder.
         */
        public Builder projectionExpression(String projectionExpression) {
            this.projectionExpression = projectionExpression;
            return this;
        }

        /**
         * A string that contains conditions that DynamoDB applies after the <code>Scan</code> operation, but before the
         * data is returned to you. Items that do not satisfy the <code>FilterExpression</code> criteria are not returned.
         *
         * <p>A <code>FilterExpression</code> is applied after the items have already been read; the process of filtering
         * does not consume any additional read capacity units.
         *
         * <p>For more information, see <a href="https://docs.aws.amazon.com/amazondynamodb/latest/developerguide/Scan.html#Scan.FilterExpression">Filter Expressions</a> in the <i>Amazon DynamoDB Developer Guide</i>.
         *
         * @return this builder.
         */
        public Builder filterExpression(String filterExpression) {
            this.filterExpression = filterExpression;
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
         * A Boolean value that determines the read consistency model during the scan:
         *
         * <ul>
         *   <li>
         *     If <code>ConsistentRead</code> is <code>false</code>, then the data returned from <code>Scan</code> might
         *     not contain the results from other recently completed write operations (<code>PutItem</code>, <code>
         *     UpdateItem</code>, or <code>DeleteItem</code>).
         *   </li>
         *   <li>
         *     If <code>ConsistentRead</code> is <code>true</code>, then all of the write operations that completed
         *     before the <code>Scan</code> began are guaranteed to be contained in the <code>Scan</code> response.
         *   </li>
         * </ul>
         *
         * <p>The default setting for <code>ConsistentRead</code> is <code>false</code>.
         *
         * <p>The <code>ConsistentRead</code> parameter is not supported on global secondary indexes. If you scan a global
         * secondary index with <code>ConsistentRead</code> set to true, you will receive a <code>ValidationException</code>
         * .
         *
         * @return this builder.
         */
        public Builder consistentRead(Boolean consistentRead) {
            this.consistentRead = consistentRead;
            return this;
        }

        @Override
        public ScanInput build() {
            tracker.validate();
            return new ScanInput(this);
        }

        @Override
        @SuppressWarnings("unchecked")
        public void setMemberValue(Schema member, Object value) {
            switch (member.memberIndex()) {
                case 0 -> tableName((String) SchemaUtils.validateSameMember($SCHEMA_TABLE_NAME, member, value));
                case 1 -> indexName((String) SchemaUtils.validateSameMember($SCHEMA_INDEX_NAME, member, value));
                case 2 -> attributesToGet((List<String>) SchemaUtils.validateSameMember($SCHEMA_ATTRIBUTES_TO_GET, member, value));
                case 3 -> limit((Integer) SchemaUtils.validateSameMember($SCHEMA_LIMIT, member, value));
                case 4 -> select((Select) SchemaUtils.validateSameMember($SCHEMA_SELECT, member, value));
                case 5 -> scanFilter((Map<String, Condition>) SchemaUtils.validateSameMember($SCHEMA_SCAN_FILTER, member, value));
                case 6 -> conditionalOperator((ConditionalOperator) SchemaUtils.validateSameMember($SCHEMA_CONDITIONAL_OPERATOR, member, value));
                case 7 -> exclusiveStartKey((Map<String, AttributeValue>) SchemaUtils.validateSameMember($SCHEMA_EXCLUSIVE_START_KEY, member, value));
                case 8 -> returnConsumedCapacity((ReturnConsumedCapacity) SchemaUtils.validateSameMember($SCHEMA_RETURN_CONSUMED_CAPACITY, member, value));
                case 9 -> totalSegments((Integer) SchemaUtils.validateSameMember($SCHEMA_TOTAL_SEGMENTS, member, value));
                case 10 -> segment((Integer) SchemaUtils.validateSameMember($SCHEMA_SEGMENT, member, value));
                case 11 -> projectionExpression((String) SchemaUtils.validateSameMember($SCHEMA_PROJECTION_EXPRESSION, member, value));
                case 12 -> filterExpression((String) SchemaUtils.validateSameMember($SCHEMA_FILTER_EXPRESSION, member, value));
                case 13 -> expressionAttributeNames((Map<String, String>) SchemaUtils.validateSameMember($SCHEMA_EXPRESSION_ATTRIBUTE_NAMES, member, value));
                case 14 -> expressionAttributeValues((Map<String, AttributeValue>) SchemaUtils.validateSameMember($SCHEMA_EXPRESSION_ATTRIBUTE_VALUES, member, value));
                case 15 -> consistentRead((Boolean) SchemaUtils.validateSameMember($SCHEMA_CONSISTENT_READ, member, value));
                default -> ShapeBuilder.super.setMemberValue(member, value);
            }
        }

        @Override
        public ShapeBuilder<ScanInput> errorCorrection() {
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
                    case 2 -> builder.attributesToGet(SharedSerde.deserializeAttributeNameList(member, de));
                    case 3 -> builder.limit(de.readInteger(member));
                    case 4 -> builder.select(Select.builder().deserializeMember(de, member).build());
                    case 5 -> builder.scanFilter(SharedSerde.deserializeFilterConditionMap(member, de));
                    case 6 -> builder.conditionalOperator(ConditionalOperator.builder().deserializeMember(de, member).build());
                    case 7 -> builder.exclusiveStartKey(SharedSerde.deserializeKey(member, de));
                    case 8 -> builder.returnConsumedCapacity(ReturnConsumedCapacity.builder().deserializeMember(de, member).build());
                    case 9 -> builder.totalSegments(de.readInteger(member));
                    case 10 -> builder.segment(de.readInteger(member));
                    case 11 -> builder.projectionExpression(de.readString(member));
                    case 12 -> builder.filterExpression(de.readString(member));
                    case 13 -> builder.expressionAttributeNames(SharedSerde.deserializeExpressionAttributeNameMap(member, de));
                    case 14 -> builder.expressionAttributeValues(SharedSerde.deserializeExpressionAttributeValueMap(member, de));
                    case 15 -> builder.consistentRead(de.readBoolean(member));
                    default -> throw new IllegalArgumentException("Unexpected member: " + member.memberName());
                }
            }
        }
    }
}
