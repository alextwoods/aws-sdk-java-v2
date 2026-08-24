package software.amazon.awssdk.benchmark.smithyjava.dynamodb.model;

import java.util.Collections;
import java.util.List;
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
 * Represents the output of a <code>BatchWriteItem</code> operation.
 */
@SmithyGenerated
public final class BatchWriteItemOutput implements SerializableStruct {

    public static final Schema $SCHEMA = Schemas.BATCH_WRITE_ITEM_OUTPUT;
    private static final Schema $SCHEMA_UNPROCESSED_ITEMS = $SCHEMA.member("UnprocessedItems");
    private static final Schema $SCHEMA_ITEM_COLLECTION_METRICS = $SCHEMA.member("ItemCollectionMetrics");
    private static final Schema $SCHEMA_CONSUMED_CAPACITY = $SCHEMA.member("ConsumedCapacity");

    public static final ShapeId $ID = $SCHEMA.id();

    private final transient Map<String, List<WriteRequest>> unprocessedItems;
    private final transient Map<String, List<ItemCollectionMetrics>> itemCollectionMetrics;
    private final transient List<ConsumedCapacity> consumedCapacity;

    private BatchWriteItemOutput(Builder builder) {
        this.unprocessedItems = builder.unprocessedItems == null ? null : Collections.unmodifiableMap(builder.unprocessedItems);
        this.itemCollectionMetrics = builder.itemCollectionMetrics == null ? null : Collections.unmodifiableMap(builder.itemCollectionMetrics);
        this.consumedCapacity = builder.consumedCapacity == null ? null : Collections.unmodifiableList(builder.consumedCapacity);
    }

    /**
     * A map of tables and requests against those tables that were not processed. The <code>UnprocessedItems</code>
     * value is in the same form as <code>RequestItems</code>, so you can provide this value directly to a subsequent <code>
     * BatchWriteItem</code> operation. For more information, see <code>RequestItems</code> in the Request Parameters
     * section.
     *
     * <p>Each <code>UnprocessedItems</code> entry consists of a table name or table ARN and, for that table, a list of
     * operations to perform (<code>DeleteRequest</code> or <code>PutRequest</code>).
     *
     * <ul>
     *   <li>
     *     <code>DeleteRequest</code> - Perform a <code>DeleteItem</code> operation on the specified item. The item
     *     to be deleted is identified by a <code>Key</code> subelement:
     *
     *     <ul>
     *       <li>
     *         <code>Key</code> - A map of primary key attribute values that uniquely identify the item. Each
     *         entry in this map consists of an attribute name and an attribute value.
     *       </li>
     *     </ul>
     *   </li>
     *   <li>
     *     <code>PutRequest</code> - Perform a <code>PutItem</code> operation on the specified item. The item to be
     *     put is identified by an <code>Item</code> subelement:
     *
     *     <ul>
     *       <li>
     *         <code>Item</code> - A map of attributes and their values. Each entry in this map consists of an
     *         attribute name and an attribute value. Attribute values must not be null; string and binary type
     *         attributes must have lengths greater than zero; and set type attributes must not be empty.
     *         Requests that contain empty values will be rejected with a <code>ValidationException</code>
     *         exception.If you specify any attributes that are part of an index key, then the data types for
     *         those attributes must match those of the schema in the table's attribute definition.
     *       </li>
     *     </ul>
     *   </li>
     * </ul>
     *
     * <p>If there are no unprocessed items remaining, the response contains an empty <code>UnprocessedItems</code> map.
     */
    public Map<String, List<WriteRequest>> getUnprocessedItems() {
        if (unprocessedItems == null) {
            return Collections.emptyMap();
        }
        return unprocessedItems;
    }

    public boolean hasUnprocessedItems() {
        return unprocessedItems != null;
    }

    /**
     * A list of tables that were processed by <code>BatchWriteItem</code> and, for each table, information about any
     * item collections that were affected by individual <code>DeleteItem</code> or <code>PutItem</code> operations.
     *
     * <p>Each entry consists of the following subelements:
     *
     * <ul>
     *   <li>
     *     <code>ItemCollectionKey</code> - The partition key value of the item collection. This is the same as the
     *     partition key value of the item.
     *   </li>
     *   <li>
     *     <code>SizeEstimateRangeGB</code> - An estimate of item collection size, expressed in GB. This is a
     *     two-element array containing a lower bound and an upper bound for the estimate. The estimate includes the
     *     size of all the items in the table, plus the size of all attributes projected into all of the local
     *     secondary indexes on the table. Use this estimate to measure whether a local secondary index is
     *     approaching its size limit.The estimate is subject to change over time; therefore, do not rely on the
     *     precision or accuracy of the estimate.
     *   </li>
     * </ul>
     */
    public Map<String, List<ItemCollectionMetrics>> getItemCollectionMetrics() {
        if (itemCollectionMetrics == null) {
            return Collections.emptyMap();
        }
        return itemCollectionMetrics;
    }

    public boolean hasItemCollectionMetrics() {
        return itemCollectionMetrics != null;
    }

    /**
     * The capacity units consumed by the entire <code>BatchWriteItem</code> operation.
     *
     * <p>Each element consists of:
     *
     * <ul>
     *   <li>
     *     <code>TableName</code> - The table that consumed the provisioned throughput.
     *   </li>
     *   <li>
     *     <code>CapacityUnits</code> - The total number of capacity units consumed.
     *   </li>
     * </ul>
     *
     * <p>If the table has vector indexes, each element also includes a <code>VectorIndexes</code> field with <code>
     * VectorWriteRequestBytes</code> consumed for each affected vector index.
     */
    public List<ConsumedCapacity> getConsumedCapacity() {
        if (consumedCapacity == null) {
            return Collections.emptyList();
        }
        return consumedCapacity;
    }

    public boolean hasConsumedCapacity() {
        return consumedCapacity != null;
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
        BatchWriteItemOutput that = (BatchWriteItemOutput) other;
        return Objects.equals(this.consumedCapacity, that.consumedCapacity)
               && Objects.equals(this.unprocessedItems, that.unprocessedItems)
               && Objects.equals(this.itemCollectionMetrics, that.itemCollectionMetrics);
    }

    @Override
    public int hashCode() {
        int $hc = Objects.hashCode(unprocessedItems);
        $hc = 31 * $hc + Objects.hashCode(itemCollectionMetrics);
        $hc = 31 * $hc + Objects.hashCode(consumedCapacity);
        return $hc;
    }

    @Override
    public Schema schema() {
        return $SCHEMA;
    }

    @Override
    public void serializeMembers(ShapeSerializer serializer) {
        if (unprocessedItems != null) {
            serializer.writeMap($SCHEMA_UNPROCESSED_ITEMS, unprocessedItems, unprocessedItems.size(), SharedSerde.BatchWriteItemRequestMapSerializer.INSTANCE);
        }
        if (itemCollectionMetrics != null) {
            serializer.writeMap($SCHEMA_ITEM_COLLECTION_METRICS, itemCollectionMetrics, itemCollectionMetrics.size(), SharedSerde.ItemCollectionMetricsPerTableSerializer.INSTANCE);
        }
        if (consumedCapacity != null) {
            serializer.writeList($SCHEMA_CONSUMED_CAPACITY, consumedCapacity, consumedCapacity.size(), SharedSerde.ConsumedCapacityMultipleSerializer.INSTANCE);
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T getMemberValue(Schema member) {
        return switch (member.memberIndex()) {
            case 0 -> (T) SchemaUtils.validateSameMember($SCHEMA_UNPROCESSED_ITEMS, member, unprocessedItems);
            case 1 -> (T) SchemaUtils.validateSameMember($SCHEMA_ITEM_COLLECTION_METRICS, member, itemCollectionMetrics);
            case 2 -> (T) SchemaUtils.validateSameMember($SCHEMA_CONSUMED_CAPACITY, member, consumedCapacity);
            default -> throw new IllegalArgumentException("Attempted to get non-existent member: " + member.id());
        };
    }

    /**
     * Create a new builder containing all the current property values of this object.
     *
     * <p><strong>Note:</strong> This method performs only a shallow copy of the original properties.
     *
     * @return a builder for {@link BatchWriteItemOutput}.
     */
    public Builder toBuilder() {
        var builder = new Builder();
        builder.unprocessedItems(this.unprocessedItems);
        builder.itemCollectionMetrics(this.itemCollectionMetrics);
        builder.consumedCapacity(this.consumedCapacity);
        return builder;
    }

    /**
     * @return returns a new Builder.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for {@link BatchWriteItemOutput}.
     */
    public static final class Builder implements ShapeBuilder<BatchWriteItemOutput> {
        private Map<String, List<WriteRequest>> unprocessedItems;
        private Map<String, List<ItemCollectionMetrics>> itemCollectionMetrics;
        private List<ConsumedCapacity> consumedCapacity;

        private Builder() {}

        @Override
        public Schema schema() {
            return $SCHEMA;
        }

        /**
         * A map of tables and requests against those tables that were not processed. The <code>UnprocessedItems</code>
         * value is in the same form as <code>RequestItems</code>, so you can provide this value directly to a subsequent <code>
         * BatchWriteItem</code> operation. For more information, see <code>RequestItems</code> in the Request Parameters
         * section.
         *
         * <p>Each <code>UnprocessedItems</code> entry consists of a table name or table ARN and, for that table, a list of
         * operations to perform (<code>DeleteRequest</code> or <code>PutRequest</code>).
         *
         * <ul>
         *   <li>
         *     <code>DeleteRequest</code> - Perform a <code>DeleteItem</code> operation on the specified item. The item
         *     to be deleted is identified by a <code>Key</code> subelement:
         *
         *     <ul>
         *       <li>
         *         <code>Key</code> - A map of primary key attribute values that uniquely identify the item. Each
         *         entry in this map consists of an attribute name and an attribute value.
         *       </li>
         *     </ul>
         *   </li>
         *   <li>
         *     <code>PutRequest</code> - Perform a <code>PutItem</code> operation on the specified item. The item to be
         *     put is identified by an <code>Item</code> subelement:
         *
         *     <ul>
         *       <li>
         *         <code>Item</code> - A map of attributes and their values. Each entry in this map consists of an
         *         attribute name and an attribute value. Attribute values must not be null; string and binary type
         *         attributes must have lengths greater than zero; and set type attributes must not be empty.
         *         Requests that contain empty values will be rejected with a <code>ValidationException</code>
         *         exception.If you specify any attributes that are part of an index key, then the data types for
         *         those attributes must match those of the schema in the table's attribute definition.
         *       </li>
         *     </ul>
         *   </li>
         * </ul>
         *
         * <p>If there are no unprocessed items remaining, the response contains an empty <code>UnprocessedItems</code> map.
         *
         * @return this builder.
         */
        public Builder unprocessedItems(Map<String, List<WriteRequest>> unprocessedItems) {
            this.unprocessedItems = unprocessedItems;
            return this;
        }

        /**
         * A list of tables that were processed by <code>BatchWriteItem</code> and, for each table, information about any
         * item collections that were affected by individual <code>DeleteItem</code> or <code>PutItem</code> operations.
         *
         * <p>Each entry consists of the following subelements:
         *
         * <ul>
         *   <li>
         *     <code>ItemCollectionKey</code> - The partition key value of the item collection. This is the same as the
         *     partition key value of the item.
         *   </li>
         *   <li>
         *     <code>SizeEstimateRangeGB</code> - An estimate of item collection size, expressed in GB. This is a
         *     two-element array containing a lower bound and an upper bound for the estimate. The estimate includes the
         *     size of all the items in the table, plus the size of all attributes projected into all of the local
         *     secondary indexes on the table. Use this estimate to measure whether a local secondary index is
         *     approaching its size limit.The estimate is subject to change over time; therefore, do not rely on the
         *     precision or accuracy of the estimate.
         *   </li>
         * </ul>
         *
         * @return this builder.
         */
        public Builder itemCollectionMetrics(Map<String, List<ItemCollectionMetrics>> itemCollectionMetrics) {
            this.itemCollectionMetrics = itemCollectionMetrics;
            return this;
        }

        /**
         * The capacity units consumed by the entire <code>BatchWriteItem</code> operation.
         *
         * <p>Each element consists of:
         *
         * <ul>
         *   <li>
         *     <code>TableName</code> - The table that consumed the provisioned throughput.
         *   </li>
         *   <li>
         *     <code>CapacityUnits</code> - The total number of capacity units consumed.
         *   </li>
         * </ul>
         *
         * <p>If the table has vector indexes, each element also includes a <code>VectorIndexes</code> field with <code>
         * VectorWriteRequestBytes</code> consumed for each affected vector index.
         *
         * @return this builder.
         */
        public Builder consumedCapacity(List<ConsumedCapacity> consumedCapacity) {
            this.consumedCapacity = consumedCapacity;
            return this;
        }

        @Override
        public BatchWriteItemOutput build() {
            return new BatchWriteItemOutput(this);
        }

        @Override
        @SuppressWarnings("unchecked")
        public void setMemberValue(Schema member, Object value) {
            switch (member.memberIndex()) {
                case 0 -> unprocessedItems((Map<String, List<WriteRequest>>) SchemaUtils.validateSameMember($SCHEMA_UNPROCESSED_ITEMS, member, value));
                case 1 -> itemCollectionMetrics((Map<String, List<ItemCollectionMetrics>>) SchemaUtils.validateSameMember($SCHEMA_ITEM_COLLECTION_METRICS, member, value));
                case 2 -> consumedCapacity((List<ConsumedCapacity>) SchemaUtils.validateSameMember($SCHEMA_CONSUMED_CAPACITY, member, value));
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
                    case 0 -> builder.unprocessedItems(SharedSerde.deserializeBatchWriteItemRequestMap(member, de));
                    case 1 -> builder.itemCollectionMetrics(SharedSerde.deserializeItemCollectionMetricsPerTable(member, de));
                    case 2 -> builder.consumedCapacity(SharedSerde.deserializeConsumedCapacityMultiple(member, de));
                    default -> throw new IllegalArgumentException("Unexpected member: " + member.memberName());
                }
            }
        }
    }
}
