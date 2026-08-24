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
 * Represents the input of a <code>BatchGetItem</code> operation.
 */
@SmithyGenerated
public final class BatchGetItemInput implements SerializableStruct {

    public static final Schema $SCHEMA = Schemas.BATCH_GET_ITEM_INPUT;
    private static final Schema $SCHEMA_REQUEST_ITEMS = $SCHEMA.member("RequestItems");
    private static final Schema $SCHEMA_RETURN_CONSUMED_CAPACITY = $SCHEMA.member("ReturnConsumedCapacity");

    public static final ShapeId $ID = $SCHEMA.id();

    private final transient Map<String, KeysAndAttributes> requestItems;
    private final transient ReturnConsumedCapacity returnConsumedCapacity;

    private BatchGetItemInput(Builder builder) {
        this.requestItems = Collections.unmodifiableMap(builder.requestItems);
        this.returnConsumedCapacity = builder.returnConsumedCapacity;
    }

    /**
     * A map of one or more table names or table ARNs and, for each table, a map that describes one or more items to
     * retrieve from that table. Each table name or ARN can be used only once per <code>BatchGetItem</code> request.
     *
     * <p>Each element in the map of items to retrieve consists of the following:
     *
     * <ul>
     *   <li>
     *     <code>ConsistentRead</code> - If <code>true</code>, a strongly consistent read is used; if <code>false</code>
     *     (the default), an eventually consistent read is used.
     *   </li>
     *   <li>
     *     <code>ExpressionAttributeNames</code> - One or more substitution tokens for attribute names in the <code>
     *     ProjectionExpression</code> parameter. The following are some use cases for using <code>
     *     ExpressionAttributeNames</code>:
     *
     *     <ul>
     *       <li>
     *         To access an attribute whose name conflicts with a DynamoDB reserved word.
     *       </li>
     *       <li>
     *         To create a placeholder for repeating occurrences of an attribute name in an expression.
     *       </li>
     *       <li>
     *         To prevent special characters in an attribute name from being misinterpreted in an expression.
     *       </li>
     *     </ul>Use the <b>#</b> character in an expression to dereference an attribute name. For example, consider
     *     the following attribute name:
     *
     *     <ul>
     *       <li>
     *         <code>Percentile</code>
     *       </li>
     *     </ul>The name of this attribute conflicts with a reserved word, so it cannot be used directly in an
     *     expression. (For the complete list of reserved words, see <a href="https://docs.aws.amazon.com/amazondynamodb/latest/developerguide/ReservedWords.html">Reserved Words</a> in the <i>Amazon DynamoDB
     *     Developer Guide</i>). To work around this, you could specify the following for <code>
     *     ExpressionAttributeNames</code>:
     *
     *     <ul>
     *       <li>
     *         <code>{"#P":"Percentile"}</code>
     *       </li>
     *     </ul>You could then use this substitution in an expression, as in this example:
     *
     *     <ul>
     *       <li>
     *         <code>#P = :val</code>
     *       </li>
     *     </ul>Tokens that begin with the <b>:</b> character are <i>expression attribute values</i>, which are
     *     placeholders for the actual value at runtime.For more information about expression attribute names, see <a href="https://docs.aws.amazon.com/amazondynamodb/latest/developerguide/Expressions.AccessingItemAttributes.html">
     *     Accessing Item Attributes</a> in the <i>Amazon DynamoDB Developer Guide</i>.
     *   </li>
     *   <li>
     *     <code>Keys</code> - An array of primary key attribute values that define specific items in the table. For
     *     each primary key, you must provide <i>all</i> of the key attributes. For example, with a simple primary
     *     key, you only need to provide the partition key value. For a composite key, you must provide <i>both</i>
     *     the partition key value and the sort key value.
     *   </li>
     *   <li>
     *     <code>ProjectionExpression</code> - A string that identifies one or more attributes to retrieve from the
     *     table. These attributes can include scalars, sets, or elements of a JSON document. The attributes in the
     *     expression must be separated by commas.If no attribute names are specified, then all attributes are
     *     returned. If any of the requested attributes are not found, they do not appear in the result.For more
     *     information, see <a href="https://docs.aws.amazon.com/amazondynamodb/latest/developerguide/Expressions.AccessingItemAttributes.html">Accessing Item Attributes</a> in the <i>Amazon DynamoDB Developer Guide</i>.
     *   </li>
     *   <li>
     *     <code>AttributesToGet</code> - This is a legacy parameter. Use <code>ProjectionExpression</code> instead.
     *     For more information, see <a href="https://docs.aws.amazon.com/amazondynamodb/latest/developerguide/LegacyConditionalParameters.AttributesToGet.html">AttributesToGet</a> in the <i>Amazon DynamoDB Developer Guide</i>.
     *   </li>
     * </ul>
     */
    public Map<String, KeysAndAttributes> getRequestItems() {
        return requestItems;
    }

    public boolean hasRequestItems() {
        return true;
    }

    public ReturnConsumedCapacity getReturnConsumedCapacity() {
        return returnConsumedCapacity;
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
        BatchGetItemInput that = (BatchGetItemInput) other;
        return Objects.equals(this.returnConsumedCapacity, that.returnConsumedCapacity)
               && Objects.equals(this.requestItems, that.requestItems);
    }

    @Override
    public int hashCode() {
        int $hc = Objects.hashCode(requestItems);
        $hc = 31 * $hc + Objects.hashCode(returnConsumedCapacity);
        return $hc;
    }

    @Override
    public Schema schema() {
        return $SCHEMA;
    }

    @Override
    public void serializeMembers(ShapeSerializer serializer) {
        serializer.writeMap($SCHEMA_REQUEST_ITEMS, requestItems, requestItems.size(), SharedSerde.BatchGetRequestMapSerializer.INSTANCE);
        if (returnConsumedCapacity != null) {
            serializer.writeString($SCHEMA_RETURN_CONSUMED_CAPACITY, returnConsumedCapacity.getValue());
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T getMemberValue(Schema member) {
        return switch (member.memberIndex()) {
            case 0 -> (T) SchemaUtils.validateSameMember($SCHEMA_REQUEST_ITEMS, member, requestItems);
            case 1 -> (T) SchemaUtils.validateSameMember($SCHEMA_RETURN_CONSUMED_CAPACITY, member, returnConsumedCapacity);
            default -> throw new IllegalArgumentException("Attempted to get non-existent member: " + member.id());
        };
    }

    /**
     * Create a new builder containing all the current property values of this object.
     *
     * <p><strong>Note:</strong> This method performs only a shallow copy of the original properties.
     *
     * @return a builder for {@link BatchGetItemInput}.
     */
    public Builder toBuilder() {
        var builder = new Builder();
        builder.requestItems(this.requestItems);
        builder.returnConsumedCapacity(this.returnConsumedCapacity);
        return builder;
    }

    /**
     * @return returns a new Builder.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for {@link BatchGetItemInput}.
     */
    public static final class Builder implements ShapeBuilder<BatchGetItemInput> {
        private final PresenceTracker tracker = PresenceTracker.of($SCHEMA);
        private Map<String, KeysAndAttributes> requestItems;
        private ReturnConsumedCapacity returnConsumedCapacity;

        private Builder() {}

        @Override
        public Schema schema() {
            return $SCHEMA;
        }

        /**
         * A map of one or more table names or table ARNs and, for each table, a map that describes one or more items to
         * retrieve from that table. Each table name or ARN can be used only once per <code>BatchGetItem</code> request.
         *
         * <p>Each element in the map of items to retrieve consists of the following:
         *
         * <ul>
         *   <li>
         *     <code>ConsistentRead</code> - If <code>true</code>, a strongly consistent read is used; if <code>false</code>
         *     (the default), an eventually consistent read is used.
         *   </li>
         *   <li>
         *     <code>ExpressionAttributeNames</code> - One or more substitution tokens for attribute names in the <code>
         *     ProjectionExpression</code> parameter. The following are some use cases for using <code>
         *     ExpressionAttributeNames</code>:
         *
         *     <ul>
         *       <li>
         *         To access an attribute whose name conflicts with a DynamoDB reserved word.
         *       </li>
         *       <li>
         *         To create a placeholder for repeating occurrences of an attribute name in an expression.
         *       </li>
         *       <li>
         *         To prevent special characters in an attribute name from being misinterpreted in an expression.
         *       </li>
         *     </ul>Use the <b>#</b> character in an expression to dereference an attribute name. For example, consider
         *     the following attribute name:
         *
         *     <ul>
         *       <li>
         *         <code>Percentile</code>
         *       </li>
         *     </ul>The name of this attribute conflicts with a reserved word, so it cannot be used directly in an
         *     expression. (For the complete list of reserved words, see <a href="https://docs.aws.amazon.com/amazondynamodb/latest/developerguide/ReservedWords.html">Reserved Words</a> in the <i>Amazon DynamoDB
         *     Developer Guide</i>). To work around this, you could specify the following for <code>
         *     ExpressionAttributeNames</code>:
         *
         *     <ul>
         *       <li>
         *         <code>{"#P":"Percentile"}</code>
         *       </li>
         *     </ul>You could then use this substitution in an expression, as in this example:
         *
         *     <ul>
         *       <li>
         *         <code>#P = :val</code>
         *       </li>
         *     </ul>Tokens that begin with the <b>:</b> character are <i>expression attribute values</i>, which are
         *     placeholders for the actual value at runtime.For more information about expression attribute names, see <a href="https://docs.aws.amazon.com/amazondynamodb/latest/developerguide/Expressions.AccessingItemAttributes.html">
         *     Accessing Item Attributes</a> in the <i>Amazon DynamoDB Developer Guide</i>.
         *   </li>
         *   <li>
         *     <code>Keys</code> - An array of primary key attribute values that define specific items in the table. For
         *     each primary key, you must provide <i>all</i> of the key attributes. For example, with a simple primary
         *     key, you only need to provide the partition key value. For a composite key, you must provide <i>both</i>
         *     the partition key value and the sort key value.
         *   </li>
         *   <li>
         *     <code>ProjectionExpression</code> - A string that identifies one or more attributes to retrieve from the
         *     table. These attributes can include scalars, sets, or elements of a JSON document. The attributes in the
         *     expression must be separated by commas.If no attribute names are specified, then all attributes are
         *     returned. If any of the requested attributes are not found, they do not appear in the result.For more
         *     information, see <a href="https://docs.aws.amazon.com/amazondynamodb/latest/developerguide/Expressions.AccessingItemAttributes.html">Accessing Item Attributes</a> in the <i>Amazon DynamoDB Developer Guide</i>.
         *   </li>
         *   <li>
         *     <code>AttributesToGet</code> - This is a legacy parameter. Use <code>ProjectionExpression</code> instead.
         *     For more information, see <a href="https://docs.aws.amazon.com/amazondynamodb/latest/developerguide/LegacyConditionalParameters.AttributesToGet.html">AttributesToGet</a> in the <i>Amazon DynamoDB Developer Guide</i>.
         *   </li>
         * </ul>
         *
         * <p><strong>Required</strong>
         * @return this builder.
         */
        public Builder requestItems(Map<String, KeysAndAttributes> requestItems) {
            this.requestItems = Objects.requireNonNull(requestItems, "requestItems cannot be null");
            tracker.setMember($SCHEMA_REQUEST_ITEMS);
            return this;
        }

        /**
         * @return this builder.
         */
        public Builder returnConsumedCapacity(ReturnConsumedCapacity returnConsumedCapacity) {
            this.returnConsumedCapacity = returnConsumedCapacity;
            return this;
        }

        @Override
        public BatchGetItemInput build() {
            tracker.validate();
            return new BatchGetItemInput(this);
        }

        @Override
        @SuppressWarnings("unchecked")
        public void setMemberValue(Schema member, Object value) {
            switch (member.memberIndex()) {
                case 0 -> requestItems((Map<String, KeysAndAttributes>) SchemaUtils.validateSameMember($SCHEMA_REQUEST_ITEMS, member, value));
                case 1 -> returnConsumedCapacity((ReturnConsumedCapacity) SchemaUtils.validateSameMember($SCHEMA_RETURN_CONSUMED_CAPACITY, member, value));
                default -> ShapeBuilder.super.setMemberValue(member, value);
            }
        }

        @Override
        public ShapeBuilder<BatchGetItemInput> errorCorrection() {
            if (tracker.allSet()) {
                return this;
            }
            if (!tracker.checkMember($SCHEMA_REQUEST_ITEMS)) {
                requestItems(Collections.emptyMap());
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
                    case 0 -> builder.requestItems(SharedSerde.deserializeBatchGetRequestMap(member, de));
                    case 1 -> builder.returnConsumedCapacity(ReturnConsumedCapacity.builder().deserializeMember(de, member).build());
                    default -> throw new IllegalArgumentException("Unexpected member: " + member.memberName());
                }
            }
        }
    }
}
