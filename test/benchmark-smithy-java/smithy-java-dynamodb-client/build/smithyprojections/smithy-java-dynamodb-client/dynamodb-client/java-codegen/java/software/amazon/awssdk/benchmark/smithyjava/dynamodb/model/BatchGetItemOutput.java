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
 * Represents the output of a <code>BatchGetItem</code> operation.
 */
@SmithyGenerated
public final class BatchGetItemOutput implements SerializableStruct {

    public static final Schema $SCHEMA = Schemas.BATCH_GET_ITEM_OUTPUT;
    private static final Schema $SCHEMA_RESPONSES = $SCHEMA.member("Responses");
    private static final Schema $SCHEMA_UNPROCESSED_KEYS = $SCHEMA.member("UnprocessedKeys");
    private static final Schema $SCHEMA_CONSUMED_CAPACITY = $SCHEMA.member("ConsumedCapacity");

    public static final ShapeId $ID = $SCHEMA.id();

    private final transient Map<String, List<Map<String, AttributeValue>>> responses;
    private final transient Map<String, KeysAndAttributes> unprocessedKeys;
    private final transient List<ConsumedCapacity> consumedCapacity;

    private BatchGetItemOutput(Builder builder) {
        this.responses = builder.responses == null ? null : Collections.unmodifiableMap(builder.responses);
        this.unprocessedKeys = builder.unprocessedKeys == null ? null : Collections.unmodifiableMap(builder.unprocessedKeys);
        this.consumedCapacity = builder.consumedCapacity == null ? null : Collections.unmodifiableList(builder.consumedCapacity);
    }

    /**
     * A map of table name or table ARN to a list of items. Each object in <code>Responses</code> consists of a table
     * name or ARN, along with a map of attribute data consisting of the data type and attribute value.
     */
    public Map<String, List<Map<String, AttributeValue>>> getResponses() {
        if (responses == null) {
            return Collections.emptyMap();
        }
        return responses;
    }

    public boolean hasResponses() {
        return responses != null;
    }

    /**
     * A map of tables and their respective keys that were not processed with the current response. The <code>
     * UnprocessedKeys</code> value is in the same form as <code>RequestItems</code>, so the value can be provided
     * directly to a subsequent <code>BatchGetItem</code> operation. For more information, see <code>RequestItems</code>
     * in the Request Parameters section.
     *
     * <p>Each element consists of:
     *
     * <ul>
     *   <li>
     *     <code>Keys</code> - An array of primary key attribute values that define specific items in the table.
     *   </li>
     *   <li>
     *     <code>ProjectionExpression</code> - One or more attributes to be retrieved from the table or index. By
     *     default, all attributes are returned. If a requested attribute is not found, it does not appear in the
     *     result.
     *   </li>
     *   <li>
     *     <code>ConsistentRead</code> - The consistency of a read operation. If set to <code>true</code>, then a
     *     strongly consistent read is used; otherwise, an eventually consistent read is used.
     *   </li>
     * </ul>
     *
     * <p>If there are no unprocessed keys remaining, the response contains an empty <code>UnprocessedKeys</code> map.
     */
    public Map<String, KeysAndAttributes> getUnprocessedKeys() {
        if (unprocessedKeys == null) {
            return Collections.emptyMap();
        }
        return unprocessedKeys;
    }

    public boolean hasUnprocessedKeys() {
        return unprocessedKeys != null;
    }

    /**
     * The read capacity units consumed by the entire <code>BatchGetItem</code> operation.
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
        BatchGetItemOutput that = (BatchGetItemOutput) other;
        return Objects.equals(this.consumedCapacity, that.consumedCapacity)
               && Objects.equals(this.responses, that.responses)
               && Objects.equals(this.unprocessedKeys, that.unprocessedKeys);
    }

    @Override
    public int hashCode() {
        int $hc = Objects.hashCode(responses);
        $hc = 31 * $hc + Objects.hashCode(unprocessedKeys);
        $hc = 31 * $hc + Objects.hashCode(consumedCapacity);
        return $hc;
    }

    @Override
    public Schema schema() {
        return $SCHEMA;
    }

    @Override
    public void serializeMembers(ShapeSerializer serializer) {
        if (responses != null) {
            serializer.writeMap($SCHEMA_RESPONSES, responses, responses.size(), SharedSerde.BatchGetResponseMapSerializer.INSTANCE);
        }
        if (unprocessedKeys != null) {
            serializer.writeMap($SCHEMA_UNPROCESSED_KEYS, unprocessedKeys, unprocessedKeys.size(), SharedSerde.BatchGetRequestMapSerializer.INSTANCE);
        }
        if (consumedCapacity != null) {
            serializer.writeList($SCHEMA_CONSUMED_CAPACITY, consumedCapacity, consumedCapacity.size(), SharedSerde.ConsumedCapacityMultipleSerializer.INSTANCE);
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T getMemberValue(Schema member) {
        return switch (member.memberIndex()) {
            case 0 -> (T) SchemaUtils.validateSameMember($SCHEMA_RESPONSES, member, responses);
            case 1 -> (T) SchemaUtils.validateSameMember($SCHEMA_UNPROCESSED_KEYS, member, unprocessedKeys);
            case 2 -> (T) SchemaUtils.validateSameMember($SCHEMA_CONSUMED_CAPACITY, member, consumedCapacity);
            default -> throw new IllegalArgumentException("Attempted to get non-existent member: " + member.id());
        };
    }

    /**
     * Create a new builder containing all the current property values of this object.
     *
     * <p><strong>Note:</strong> This method performs only a shallow copy of the original properties.
     *
     * @return a builder for {@link BatchGetItemOutput}.
     */
    public Builder toBuilder() {
        var builder = new Builder();
        builder.responses(this.responses);
        builder.unprocessedKeys(this.unprocessedKeys);
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
     * Builder for {@link BatchGetItemOutput}.
     */
    public static final class Builder implements ShapeBuilder<BatchGetItemOutput> {
        private Map<String, List<Map<String, AttributeValue>>> responses;
        private Map<String, KeysAndAttributes> unprocessedKeys;
        private List<ConsumedCapacity> consumedCapacity;

        private Builder() {}

        @Override
        public Schema schema() {
            return $SCHEMA;
        }

        /**
         * A map of table name or table ARN to a list of items. Each object in <code>Responses</code> consists of a table
         * name or ARN, along with a map of attribute data consisting of the data type and attribute value.
         *
         * @return this builder.
         */
        public Builder responses(Map<String, List<Map<String, AttributeValue>>> responses) {
            this.responses = responses;
            return this;
        }

        /**
         * A map of tables and their respective keys that were not processed with the current response. The <code>
         * UnprocessedKeys</code> value is in the same form as <code>RequestItems</code>, so the value can be provided
         * directly to a subsequent <code>BatchGetItem</code> operation. For more information, see <code>RequestItems</code>
         * in the Request Parameters section.
         *
         * <p>Each element consists of:
         *
         * <ul>
         *   <li>
         *     <code>Keys</code> - An array of primary key attribute values that define specific items in the table.
         *   </li>
         *   <li>
         *     <code>ProjectionExpression</code> - One or more attributes to be retrieved from the table or index. By
         *     default, all attributes are returned. If a requested attribute is not found, it does not appear in the
         *     result.
         *   </li>
         *   <li>
         *     <code>ConsistentRead</code> - The consistency of a read operation. If set to <code>true</code>, then a
         *     strongly consistent read is used; otherwise, an eventually consistent read is used.
         *   </li>
         * </ul>
         *
         * <p>If there are no unprocessed keys remaining, the response contains an empty <code>UnprocessedKeys</code> map.
         *
         * @return this builder.
         */
        public Builder unprocessedKeys(Map<String, KeysAndAttributes> unprocessedKeys) {
            this.unprocessedKeys = unprocessedKeys;
            return this;
        }

        /**
         * The read capacity units consumed by the entire <code>BatchGetItem</code> operation.
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
         * @return this builder.
         */
        public Builder consumedCapacity(List<ConsumedCapacity> consumedCapacity) {
            this.consumedCapacity = consumedCapacity;
            return this;
        }

        @Override
        public BatchGetItemOutput build() {
            return new BatchGetItemOutput(this);
        }

        @Override
        @SuppressWarnings("unchecked")
        public void setMemberValue(Schema member, Object value) {
            switch (member.memberIndex()) {
                case 0 -> responses((Map<String, List<Map<String, AttributeValue>>>) SchemaUtils.validateSameMember($SCHEMA_RESPONSES, member, value));
                case 1 -> unprocessedKeys((Map<String, KeysAndAttributes>) SchemaUtils.validateSameMember($SCHEMA_UNPROCESSED_KEYS, member, value));
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
                    case 0 -> builder.responses(SharedSerde.deserializeBatchGetResponseMap(member, de));
                    case 1 -> builder.unprocessedKeys(SharedSerde.deserializeBatchGetRequestMap(member, de));
                    case 2 -> builder.consumedCapacity(SharedSerde.deserializeConsumedCapacityMultiple(member, de));
                    default -> throw new IllegalArgumentException("Unexpected member: " + member.memberName());
                }
            }
        }
    }
}
