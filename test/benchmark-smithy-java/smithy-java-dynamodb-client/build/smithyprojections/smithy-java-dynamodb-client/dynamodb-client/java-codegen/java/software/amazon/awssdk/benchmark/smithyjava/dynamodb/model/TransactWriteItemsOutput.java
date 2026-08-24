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

@SmithyGenerated
public final class TransactWriteItemsOutput implements SerializableStruct {

    public static final Schema $SCHEMA = Schemas.TRANSACT_WRITE_ITEMS_OUTPUT;
    private static final Schema $SCHEMA_CONSUMED_CAPACITY = $SCHEMA.member("ConsumedCapacity");
    private static final Schema $SCHEMA_ITEM_COLLECTION_METRICS = $SCHEMA.member("ItemCollectionMetrics");

    public static final ShapeId $ID = $SCHEMA.id();

    private final transient List<ConsumedCapacity> consumedCapacity;
    private final transient Map<String, List<ItemCollectionMetrics>> itemCollectionMetrics;

    private TransactWriteItemsOutput(Builder builder) {
        this.consumedCapacity = builder.consumedCapacity == null ? null : Collections.unmodifiableList(builder.consumedCapacity);
        this.itemCollectionMetrics = builder.itemCollectionMetrics == null ? null : Collections.unmodifiableMap(builder.itemCollectionMetrics);
    }

    /**
     * The capacity units consumed by the entire <code>TransactWriteItems</code> operation. The values of the list are
     * ordered according to the ordering of the <code>TransactItems</code> request parameter.
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

    /**
     * A list of tables that were processed by <code>TransactWriteItems</code> and, for each table, information about
     * any item collections that were affected by individual <code>UpdateItem</code>, <code>PutItem</code>, or
     * <code>DeleteItem</code> operations.
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
        TransactWriteItemsOutput that = (TransactWriteItemsOutput) other;
        return Objects.equals(this.consumedCapacity, that.consumedCapacity)
               && Objects.equals(this.itemCollectionMetrics, that.itemCollectionMetrics);
    }

    @Override
    public int hashCode() {
        int $hc = Objects.hashCode(consumedCapacity);
        $hc = 31 * $hc + Objects.hashCode(itemCollectionMetrics);
        return $hc;
    }

    @Override
    public Schema schema() {
        return $SCHEMA;
    }

    @Override
    public void serializeMembers(ShapeSerializer serializer) {
        if (consumedCapacity != null) {
            serializer.writeList($SCHEMA_CONSUMED_CAPACITY, consumedCapacity, consumedCapacity.size(), SharedSerde.ConsumedCapacityMultipleSerializer.INSTANCE);
        }
        if (itemCollectionMetrics != null) {
            serializer.writeMap($SCHEMA_ITEM_COLLECTION_METRICS, itemCollectionMetrics, itemCollectionMetrics.size(), SharedSerde.ItemCollectionMetricsPerTableSerializer.INSTANCE);
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T getMemberValue(Schema member) {
        return switch (member.memberIndex()) {
            case 0 -> (T) SchemaUtils.validateSameMember($SCHEMA_CONSUMED_CAPACITY, member, consumedCapacity);
            case 1 -> (T) SchemaUtils.validateSameMember($SCHEMA_ITEM_COLLECTION_METRICS, member, itemCollectionMetrics);
            default -> throw new IllegalArgumentException("Attempted to get non-existent member: " + member.id());
        };
    }

    /**
     * Create a new builder containing all the current property values of this object.
     *
     * <p><strong>Note:</strong> This method performs only a shallow copy of the original properties.
     *
     * @return a builder for {@link TransactWriteItemsOutput}.
     */
    public Builder toBuilder() {
        var builder = new Builder();
        builder.consumedCapacity(this.consumedCapacity);
        builder.itemCollectionMetrics(this.itemCollectionMetrics);
        return builder;
    }

    /**
     * @return returns a new Builder.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for {@link TransactWriteItemsOutput}.
     */
    public static final class Builder implements ShapeBuilder<TransactWriteItemsOutput> {
        private List<ConsumedCapacity> consumedCapacity;
        private Map<String, List<ItemCollectionMetrics>> itemCollectionMetrics;

        private Builder() {}

        @Override
        public Schema schema() {
            return $SCHEMA;
        }

        /**
         * The capacity units consumed by the entire <code>TransactWriteItems</code> operation. The values of the list are
         * ordered according to the ordering of the <code>TransactItems</code> request parameter.
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

        /**
         * A list of tables that were processed by <code>TransactWriteItems</code> and, for each table, information about
         * any item collections that were affected by individual <code>UpdateItem</code>, <code>PutItem</code>, or
         * <code>DeleteItem</code> operations.
         *
         * @return this builder.
         */
        public Builder itemCollectionMetrics(Map<String, List<ItemCollectionMetrics>> itemCollectionMetrics) {
            this.itemCollectionMetrics = itemCollectionMetrics;
            return this;
        }

        @Override
        public TransactWriteItemsOutput build() {
            return new TransactWriteItemsOutput(this);
        }

        @Override
        @SuppressWarnings("unchecked")
        public void setMemberValue(Schema member, Object value) {
            switch (member.memberIndex()) {
                case 0 -> consumedCapacity((List<ConsumedCapacity>) SchemaUtils.validateSameMember($SCHEMA_CONSUMED_CAPACITY, member, value));
                case 1 -> itemCollectionMetrics((Map<String, List<ItemCollectionMetrics>>) SchemaUtils.validateSameMember($SCHEMA_ITEM_COLLECTION_METRICS, member, value));
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
                    case 0 -> builder.consumedCapacity(SharedSerde.deserializeConsumedCapacityMultiple(member, de));
                    case 1 -> builder.itemCollectionMetrics(SharedSerde.deserializeItemCollectionMetricsPerTable(member, de));
                    default -> throw new IllegalArgumentException("Unexpected member: " + member.memberName());
                }
            }
        }
    }
}
