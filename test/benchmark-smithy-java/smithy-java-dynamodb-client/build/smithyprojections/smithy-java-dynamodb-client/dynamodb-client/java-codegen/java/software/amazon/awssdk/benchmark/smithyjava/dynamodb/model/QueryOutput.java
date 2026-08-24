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
 * Represents the output of a <code>Query</code> operation.
 */
@SmithyGenerated
public final class QueryOutput implements SerializableStruct {

    public static final Schema $SCHEMA = Schemas.QUERY_OUTPUT;
    private static final Schema $SCHEMA_ITEMS = $SCHEMA.member("Items");
    private static final Schema $SCHEMA_COUNT = $SCHEMA.member("Count");
    private static final Schema $SCHEMA_SCANNED_COUNT = $SCHEMA.member("ScannedCount");
    private static final Schema $SCHEMA_LAST_EVALUATED_KEY = $SCHEMA.member("LastEvaluatedKey");
    private static final Schema $SCHEMA_CONSUMED_CAPACITY = $SCHEMA.member("ConsumedCapacity");

    public static final ShapeId $ID = $SCHEMA.id();

    private final transient List<Map<String, AttributeValue>> items;
    private final transient int count;
    private final transient int scannedCount;
    private final transient Map<String, AttributeValue> lastEvaluatedKey;
    private final transient ConsumedCapacity consumedCapacity;

    private QueryOutput(Builder builder) {
        this.items = builder.items == null ? null : Collections.unmodifiableList(builder.items);
        this.count = builder.count;
        this.scannedCount = builder.scannedCount;
        this.lastEvaluatedKey = builder.lastEvaluatedKey == null ? null : Collections.unmodifiableMap(builder.lastEvaluatedKey);
        this.consumedCapacity = builder.consumedCapacity;
    }

    /**
     * An array of item attributes that match the query criteria. Each element in this array consists of an attribute
     * name and the value for that attribute.
     */
    public List<Map<String, AttributeValue>> getItems() {
        if (items == null) {
            return Collections.emptyList();
        }
        return items;
    }

    public boolean hasItems() {
        return items != null;
    }

    /**
     * The number of items in the response.
     *
     * <p>If you used a <code>QueryFilter</code> in the request, then <code>Count</code> is the number of items returned
     * after the filter was applied, and <code>ScannedCount</code> is the number of matching items before the filter was
     * applied.
     *
     * <p>If you did not use a filter in the request, then <code>Count</code> and <code>ScannedCount</code> are the
     * same.
     */
    public int getCount() {
        return count;
    }

    /**
     * The number of items evaluated, before any <code>QueryFilter</code> is applied. A high <code>ScannedCount</code>
     * value with few, or no, <code>Count</code> results indicates an inefficient <code>Query</code> operation. For more
     * information, see <a href="https://docs.aws.amazon.com/amazondynamodb/latest/developerguide/Scan.html#Scan.Count">Count and ScannedCount</a> in the <i>Amazon DynamoDB Developer Guide</i>.
     *
     * <p>If you did not use a filter in the request, then <code>ScannedCount</code> is the same as <code>Count</code>.
     */
    public int getScannedCount() {
        return scannedCount;
    }

    /**
     * The primary key of the item where the operation stopped, inclusive of the previous result set. Use this value to
     * start a new operation, excluding this value in the new request.
     *
     * <p>If <code>LastEvaluatedKey</code> is empty, then the "last page" of results has been processed and there is no
     * more data to be retrieved.
     *
     * <p>If <code>LastEvaluatedKey</code> is not empty, it does not necessarily mean that there is more data in the
     * result set. The only way to know when you have reached the end of the result set is when <code>LastEvaluatedKey</code>
     * is empty.
     */
    public Map<String, AttributeValue> getLastEvaluatedKey() {
        if (lastEvaluatedKey == null) {
            return Collections.emptyMap();
        }
        return lastEvaluatedKey;
    }

    public boolean hasLastEvaluatedKey() {
        return lastEvaluatedKey != null;
    }

    /**
     * The capacity units consumed by the <code>Query</code> operation. The data returned includes the total provisioned
     * throughput consumed, along with statistics for the table and any indexes involved in the operation. <code>
     * ConsumedCapacity</code> is only returned if the <code>ReturnConsumedCapacity</code> parameter was specified. For
     * more information, see <a href="https://docs.aws.amazon.com/amazondynamodb/latest/developerguide/read-write-operations.html#read-operation-consumption">Capacity unit consumption for read operations</a> in the <i>Amazon DynamoDB Developer
     * Guide</i>.
     */
    public ConsumedCapacity getConsumedCapacity() {
        return consumedCapacity;
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
        QueryOutput that = (QueryOutput) other;
        return this.count == that.count
               && this.scannedCount == that.scannedCount
               && Objects.equals(this.consumedCapacity, that.consumedCapacity)
               && Objects.equals(this.items, that.items)
               && Objects.equals(this.lastEvaluatedKey, that.lastEvaluatedKey);
    }

    @Override
    public int hashCode() {
        int $hc = Objects.hashCode(items);
        $hc = 31 * $hc + Integer.hashCode(count);
        $hc = 31 * $hc + Integer.hashCode(scannedCount);
        $hc = 31 * $hc + Objects.hashCode(lastEvaluatedKey);
        $hc = 31 * $hc + Objects.hashCode(consumedCapacity);
        return $hc;
    }

    @Override
    public Schema schema() {
        return $SCHEMA;
    }

    @Override
    public void serializeMembers(ShapeSerializer serializer) {
        if (items != null) {
            serializer.writeList($SCHEMA_ITEMS, items, items.size(), SharedSerde.ItemListSerializer.INSTANCE);
        }
        serializer.writeInteger($SCHEMA_COUNT, count);
        serializer.writeInteger($SCHEMA_SCANNED_COUNT, scannedCount);
        if (lastEvaluatedKey != null) {
            serializer.writeMap($SCHEMA_LAST_EVALUATED_KEY, lastEvaluatedKey, lastEvaluatedKey.size(), SharedSerde.KeySerializer.INSTANCE);
        }
        if (consumedCapacity != null) {
            serializer.writeStruct($SCHEMA_CONSUMED_CAPACITY, consumedCapacity);
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T getMemberValue(Schema member) {
        return switch (member.memberIndex()) {
            case 0 -> (T) SchemaUtils.validateSameMember($SCHEMA_ITEMS, member, items);
            case 1 -> (T) SchemaUtils.validateSameMember($SCHEMA_COUNT, member, count);
            case 2 -> (T) SchemaUtils.validateSameMember($SCHEMA_SCANNED_COUNT, member, scannedCount);
            case 3 -> (T) SchemaUtils.validateSameMember($SCHEMA_LAST_EVALUATED_KEY, member, lastEvaluatedKey);
            case 4 -> (T) SchemaUtils.validateSameMember($SCHEMA_CONSUMED_CAPACITY, member, consumedCapacity);
            default -> throw new IllegalArgumentException("Attempted to get non-existent member: " + member.id());
        };
    }

    /**
     * Create a new builder containing all the current property values of this object.
     *
     * <p><strong>Note:</strong> This method performs only a shallow copy of the original properties.
     *
     * @return a builder for {@link QueryOutput}.
     */
    public Builder toBuilder() {
        var builder = new Builder();
        builder.items(this.items);
        builder.count(this.count);
        builder.scannedCount(this.scannedCount);
        builder.lastEvaluatedKey(this.lastEvaluatedKey);
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
     * Builder for {@link QueryOutput}.
     */
    public static final class Builder implements ShapeBuilder<QueryOutput> {
        private List<Map<String, AttributeValue>> items;
        private int count = 0;
        private int scannedCount = 0;
        private Map<String, AttributeValue> lastEvaluatedKey;
        private ConsumedCapacity consumedCapacity;

        private Builder() {}

        @Override
        public Schema schema() {
            return $SCHEMA;
        }

        /**
         * An array of item attributes that match the query criteria. Each element in this array consists of an attribute
         * name and the value for that attribute.
         *
         * @return this builder.
         */
        public Builder items(List<Map<String, AttributeValue>> items) {
            this.items = items;
            return this;
        }

        /**
         * The number of items in the response.
         *
         * <p>If you used a <code>QueryFilter</code> in the request, then <code>Count</code> is the number of items returned
         * after the filter was applied, and <code>ScannedCount</code> is the number of matching items before the filter was
         * applied.
         *
         * <p>If you did not use a filter in the request, then <code>Count</code> and <code>ScannedCount</code> are the
         * same.
         *
         * @return this builder.
         */
        public Builder count(int count) {
            this.count = count;
            return this;
        }

        /**
         * The number of items evaluated, before any <code>QueryFilter</code> is applied. A high <code>ScannedCount</code>
         * value with few, or no, <code>Count</code> results indicates an inefficient <code>Query</code> operation. For more
         * information, see <a href="https://docs.aws.amazon.com/amazondynamodb/latest/developerguide/Scan.html#Scan.Count">Count and ScannedCount</a> in the <i>Amazon DynamoDB Developer Guide</i>.
         *
         * <p>If you did not use a filter in the request, then <code>ScannedCount</code> is the same as <code>Count</code>.
         *
         * @return this builder.
         */
        public Builder scannedCount(int scannedCount) {
            this.scannedCount = scannedCount;
            return this;
        }

        /**
         * The primary key of the item where the operation stopped, inclusive of the previous result set. Use this value to
         * start a new operation, excluding this value in the new request.
         *
         * <p>If <code>LastEvaluatedKey</code> is empty, then the "last page" of results has been processed and there is no
         * more data to be retrieved.
         *
         * <p>If <code>LastEvaluatedKey</code> is not empty, it does not necessarily mean that there is more data in the
         * result set. The only way to know when you have reached the end of the result set is when <code>LastEvaluatedKey</code>
         * is empty.
         *
         * @return this builder.
         */
        public Builder lastEvaluatedKey(Map<String, AttributeValue> lastEvaluatedKey) {
            this.lastEvaluatedKey = lastEvaluatedKey;
            return this;
        }

        /**
         * The capacity units consumed by the <code>Query</code> operation. The data returned includes the total provisioned
         * throughput consumed, along with statistics for the table and any indexes involved in the operation. <code>
         * ConsumedCapacity</code> is only returned if the <code>ReturnConsumedCapacity</code> parameter was specified. For
         * more information, see <a href="https://docs.aws.amazon.com/amazondynamodb/latest/developerguide/read-write-operations.html#read-operation-consumption">Capacity unit consumption for read operations</a> in the <i>Amazon DynamoDB Developer
         * Guide</i>.
         *
         * @return this builder.
         */
        public Builder consumedCapacity(ConsumedCapacity consumedCapacity) {
            this.consumedCapacity = consumedCapacity;
            return this;
        }

        @Override
        public QueryOutput build() {
            return new QueryOutput(this);
        }

        @Override
        @SuppressWarnings("unchecked")
        public void setMemberValue(Schema member, Object value) {
            switch (member.memberIndex()) {
                case 0 -> items((List<Map<String, AttributeValue>>) SchemaUtils.validateSameMember($SCHEMA_ITEMS, member, value));
                case 1 -> count((int) SchemaUtils.validateSameMember($SCHEMA_COUNT, member, value));
                case 2 -> scannedCount((int) SchemaUtils.validateSameMember($SCHEMA_SCANNED_COUNT, member, value));
                case 3 -> lastEvaluatedKey((Map<String, AttributeValue>) SchemaUtils.validateSameMember($SCHEMA_LAST_EVALUATED_KEY, member, value));
                case 4 -> consumedCapacity((ConsumedCapacity) SchemaUtils.validateSameMember($SCHEMA_CONSUMED_CAPACITY, member, value));
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
                    case 0 -> builder.items(SharedSerde.deserializeItemList(member, de));
                    case 1 -> builder.count(de.readInteger(member));
                    case 2 -> builder.scannedCount(de.readInteger(member));
                    case 3 -> builder.lastEvaluatedKey(SharedSerde.deserializeKey(member, de));
                    case 4 -> builder.consumedCapacity(ConsumedCapacity.builder().deserializeMember(de, member).build());
                    default -> throw new IllegalArgumentException("Unexpected member: " + member.memberName());
                }
            }
        }
    }
}
