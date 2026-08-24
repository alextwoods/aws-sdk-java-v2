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
 * Represents the input of a <code>BatchWriteItem</code> operation.
 */
@SmithyGenerated
public final class BatchWriteItemInput implements SerializableStruct {

    public static final Schema $SCHEMA = Schemas.BATCH_WRITE_ITEM_INPUT;
    private static final Schema $SCHEMA_REQUEST_ITEMS = $SCHEMA.member("RequestItems");
    private static final Schema $SCHEMA_RETURN_CONSUMED_CAPACITY = $SCHEMA.member("ReturnConsumedCapacity");
    private static final Schema $SCHEMA_RETURN_ITEM_COLLECTION_METRICS = $SCHEMA.member("ReturnItemCollectionMetrics");

    public static final ShapeId $ID = $SCHEMA.id();

    private final transient Map<String, List<WriteRequest>> requestItems;
    private final transient ReturnConsumedCapacity returnConsumedCapacity;
    private final transient ReturnItemCollectionMetrics returnItemCollectionMetrics;

    private BatchWriteItemInput(Builder builder) {
        this.requestItems = Collections.unmodifiableMap(builder.requestItems);
        this.returnConsumedCapacity = builder.returnConsumedCapacity;
        this.returnItemCollectionMetrics = builder.returnItemCollectionMetrics;
    }

    /**
     * A map of one or more table names or table ARNs and, for each table, a list of operations to be performed (<code>
     * DeleteRequest</code> or <code>PutRequest</code>). Each element in the map consists of the following:
     *
     * <ul>
     *   <li>
     *     <code>DeleteRequest</code> - Perform a <code>DeleteItem</code> operation on the specified item. The item
     *     to be deleted is identified by a <code>Key</code> subelement:
     *
     *     <ul>
     *       <li>
     *         <code>Key</code> - A map of primary key attribute values that uniquely identify the item. Each
     *         entry in this map consists of an attribute name and an attribute value. For each primary key, you
     *         must provide <i>all</i> of the key attributes. For example, with a simple primary key, you only
     *         need to provide a value for the partition key. For a composite primary key, you must provide
     *         values for <i>both</i> the partition key and the sort key.
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
     *         Requests that contain empty values are rejected with a <code>ValidationException</code>
     *         exception.If you specify any attributes that are part of an index key, then the data types for
     *         those attributes must match those of the schema in the table's attribute definition.
     *       </li>
     *     </ul>
     *   </li>
     * </ul>
     */
    public Map<String, List<WriteRequest>> getRequestItems() {
        return requestItems;
    }

    public boolean hasRequestItems() {
        return true;
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
        BatchWriteItemInput that = (BatchWriteItemInput) other;
        return Objects.equals(this.returnConsumedCapacity, that.returnConsumedCapacity)
               && Objects.equals(this.returnItemCollectionMetrics, that.returnItemCollectionMetrics)
               && Objects.equals(this.requestItems, that.requestItems);
    }

    @Override
    public int hashCode() {
        int $hc = Objects.hashCode(requestItems);
        $hc = 31 * $hc + Objects.hashCode(returnConsumedCapacity);
        $hc = 31 * $hc + Objects.hashCode(returnItemCollectionMetrics);
        return $hc;
    }

    @Override
    public Schema schema() {
        return $SCHEMA;
    }

    @Override
    public void serializeMembers(ShapeSerializer serializer) {
        serializer.writeMap($SCHEMA_REQUEST_ITEMS, requestItems, requestItems.size(), SharedSerde.BatchWriteItemRequestMapSerializer.INSTANCE);
        if (returnConsumedCapacity != null) {
            serializer.writeString($SCHEMA_RETURN_CONSUMED_CAPACITY, returnConsumedCapacity.getValue());
        }
        if (returnItemCollectionMetrics != null) {
            serializer.writeString($SCHEMA_RETURN_ITEM_COLLECTION_METRICS, returnItemCollectionMetrics.getValue());
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T getMemberValue(Schema member) {
        return switch (member.memberIndex()) {
            case 0 -> (T) SchemaUtils.validateSameMember($SCHEMA_REQUEST_ITEMS, member, requestItems);
            case 1 -> (T) SchemaUtils.validateSameMember($SCHEMA_RETURN_CONSUMED_CAPACITY, member, returnConsumedCapacity);
            case 2 -> (T) SchemaUtils.validateSameMember($SCHEMA_RETURN_ITEM_COLLECTION_METRICS, member, returnItemCollectionMetrics);
            default -> throw new IllegalArgumentException("Attempted to get non-existent member: " + member.id());
        };
    }

    /**
     * Create a new builder containing all the current property values of this object.
     *
     * <p><strong>Note:</strong> This method performs only a shallow copy of the original properties.
     *
     * @return a builder for {@link BatchWriteItemInput}.
     */
    public Builder toBuilder() {
        var builder = new Builder();
        builder.requestItems(this.requestItems);
        builder.returnConsumedCapacity(this.returnConsumedCapacity);
        builder.returnItemCollectionMetrics(this.returnItemCollectionMetrics);
        return builder;
    }

    /**
     * @return returns a new Builder.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for {@link BatchWriteItemInput}.
     */
    public static final class Builder implements ShapeBuilder<BatchWriteItemInput> {
        private final PresenceTracker tracker = PresenceTracker.of($SCHEMA);
        private Map<String, List<WriteRequest>> requestItems;
        private ReturnConsumedCapacity returnConsumedCapacity;
        private ReturnItemCollectionMetrics returnItemCollectionMetrics;

        private Builder() {}

        @Override
        public Schema schema() {
            return $SCHEMA;
        }

        /**
         * A map of one or more table names or table ARNs and, for each table, a list of operations to be performed (<code>
         * DeleteRequest</code> or <code>PutRequest</code>). Each element in the map consists of the following:
         *
         * <ul>
         *   <li>
         *     <code>DeleteRequest</code> - Perform a <code>DeleteItem</code> operation on the specified item. The item
         *     to be deleted is identified by a <code>Key</code> subelement:
         *
         *     <ul>
         *       <li>
         *         <code>Key</code> - A map of primary key attribute values that uniquely identify the item. Each
         *         entry in this map consists of an attribute name and an attribute value. For each primary key, you
         *         must provide <i>all</i> of the key attributes. For example, with a simple primary key, you only
         *         need to provide a value for the partition key. For a composite primary key, you must provide
         *         values for <i>both</i> the partition key and the sort key.
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
         *         Requests that contain empty values are rejected with a <code>ValidationException</code>
         *         exception.If you specify any attributes that are part of an index key, then the data types for
         *         those attributes must match those of the schema in the table's attribute definition.
         *       </li>
         *     </ul>
         *   </li>
         * </ul>
         *
         * <p><strong>Required</strong>
         * @return this builder.
         */
        public Builder requestItems(Map<String, List<WriteRequest>> requestItems) {
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

        @Override
        public BatchWriteItemInput build() {
            tracker.validate();
            return new BatchWriteItemInput(this);
        }

        @Override
        @SuppressWarnings("unchecked")
        public void setMemberValue(Schema member, Object value) {
            switch (member.memberIndex()) {
                case 0 -> requestItems((Map<String, List<WriteRequest>>) SchemaUtils.validateSameMember($SCHEMA_REQUEST_ITEMS, member, value));
                case 1 -> returnConsumedCapacity((ReturnConsumedCapacity) SchemaUtils.validateSameMember($SCHEMA_RETURN_CONSUMED_CAPACITY, member, value));
                case 2 -> returnItemCollectionMetrics((ReturnItemCollectionMetrics) SchemaUtils.validateSameMember($SCHEMA_RETURN_ITEM_COLLECTION_METRICS, member, value));
                default -> ShapeBuilder.super.setMemberValue(member, value);
            }
        }

        @Override
        public ShapeBuilder<BatchWriteItemInput> errorCorrection() {
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
                    case 0 -> builder.requestItems(SharedSerde.deserializeBatchWriteItemRequestMap(member, de));
                    case 1 -> builder.returnConsumedCapacity(ReturnConsumedCapacity.builder().deserializeMember(de, member).build());
                    case 2 -> builder.returnItemCollectionMetrics(ReturnItemCollectionMetrics.builder().deserializeMember(de, member).build());
                    default -> throw new IllegalArgumentException("Unexpected member: " + member.memberName());
                }
            }
        }
    }
}
