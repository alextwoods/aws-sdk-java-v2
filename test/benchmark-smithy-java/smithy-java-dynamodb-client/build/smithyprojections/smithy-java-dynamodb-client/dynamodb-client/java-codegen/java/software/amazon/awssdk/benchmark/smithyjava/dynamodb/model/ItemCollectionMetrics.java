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
 * Information about item collections, if any, that were affected by the operation. <code>ItemCollectionMetrics</code>
 * is only returned if the request asked for it. If the table does not have any local secondary indexes, this
 * information is not returned in the response.
 */
@SmithyGenerated
public final class ItemCollectionMetrics implements SerializableStruct {

    public static final Schema $SCHEMA = Schemas.ITEM_COLLECTION_METRICS;
    private static final Schema $SCHEMA_ITEM_COLLECTION_KEY = $SCHEMA.member("ItemCollectionKey");
    private static final Schema $SCHEMA_SIZE_ESTIMATE_RANGEG_B = $SCHEMA.member("SizeEstimateRangeGB");

    public static final ShapeId $ID = $SCHEMA.id();

    private final transient Map<String, AttributeValue> itemCollectionKey;
    private final transient List<Double> sizeEstimateRangegB;

    private ItemCollectionMetrics(Builder builder) {
        this.itemCollectionKey = builder.itemCollectionKey == null ? null : Collections.unmodifiableMap(builder.itemCollectionKey);
        this.sizeEstimateRangegB = builder.sizeEstimateRangegB == null ? null : Collections.unmodifiableList(builder.sizeEstimateRangegB);
    }

    /**
     * The partition key value of the item collection. This value is the same as the partition key value of the item.
     */
    public Map<String, AttributeValue> getItemCollectionKey() {
        if (itemCollectionKey == null) {
            return Collections.emptyMap();
        }
        return itemCollectionKey;
    }

    public boolean hasItemCollectionKey() {
        return itemCollectionKey != null;
    }

    /**
     * An estimate of item collection size, in gigabytes. This value is a two-element array containing a lower bound and
     * an upper bound for the estimate. The estimate includes the size of all the items in the table, plus the size of
     * all attributes projected into all of the local secondary indexes on that table. Use this estimate to measure
     * whether a local secondary index is approaching its size limit.
     *
     * <p>The estimate is subject to change over time; therefore, do not rely on the precision or accuracy of the
     * estimate.
     */
    public List<Double> getSizeEstimateRangegB() {
        if (sizeEstimateRangegB == null) {
            return Collections.emptyList();
        }
        return sizeEstimateRangegB;
    }

    public boolean hasSizeEstimateRangegB() {
        return sizeEstimateRangegB != null;
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
        ItemCollectionMetrics that = (ItemCollectionMetrics) other;
        return Objects.equals(this.sizeEstimateRangegB, that.sizeEstimateRangegB)
               && Objects.equals(this.itemCollectionKey, that.itemCollectionKey);
    }

    @Override
    public int hashCode() {
        int $hc = Objects.hashCode(itemCollectionKey);
        $hc = 31 * $hc + Objects.hashCode(sizeEstimateRangegB);
        return $hc;
    }

    @Override
    public Schema schema() {
        return $SCHEMA;
    }

    @Override
    public void serializeMembers(ShapeSerializer serializer) {
        if (itemCollectionKey != null) {
            serializer.writeMap($SCHEMA_ITEM_COLLECTION_KEY, itemCollectionKey, itemCollectionKey.size(), SharedSerde.ItemCollectionKeyAttributeMapSerializer.INSTANCE);
        }
        if (sizeEstimateRangegB != null) {
            serializer.writeList($SCHEMA_SIZE_ESTIMATE_RANGEG_B, sizeEstimateRangegB, sizeEstimateRangegB.size(), SharedSerde.ItemCollectionSizeEstimateRangeSerializer.INSTANCE);
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T getMemberValue(Schema member) {
        return switch (member.memberIndex()) {
            case 0 -> (T) SchemaUtils.validateSameMember($SCHEMA_ITEM_COLLECTION_KEY, member, itemCollectionKey);
            case 1 -> (T) SchemaUtils.validateSameMember($SCHEMA_SIZE_ESTIMATE_RANGEG_B, member, sizeEstimateRangegB);
            default -> throw new IllegalArgumentException("Attempted to get non-existent member: " + member.id());
        };
    }

    /**
     * Create a new builder containing all the current property values of this object.
     *
     * <p><strong>Note:</strong> This method performs only a shallow copy of the original properties.
     *
     * @return a builder for {@link ItemCollectionMetrics}.
     */
    public Builder toBuilder() {
        var builder = new Builder();
        builder.itemCollectionKey(this.itemCollectionKey);
        builder.sizeEstimateRangegB(this.sizeEstimateRangegB);
        return builder;
    }

    /**
     * @return returns a new Builder.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for {@link ItemCollectionMetrics}.
     */
    public static final class Builder implements ShapeBuilder<ItemCollectionMetrics> {
        private Map<String, AttributeValue> itemCollectionKey;
        private List<Double> sizeEstimateRangegB;

        private Builder() {}

        @Override
        public Schema schema() {
            return $SCHEMA;
        }

        /**
         * The partition key value of the item collection. This value is the same as the partition key value of the item.
         *
         * @return this builder.
         */
        public Builder itemCollectionKey(Map<String, AttributeValue> itemCollectionKey) {
            this.itemCollectionKey = itemCollectionKey;
            return this;
        }

        /**
         * An estimate of item collection size, in gigabytes. This value is a two-element array containing a lower bound and
         * an upper bound for the estimate. The estimate includes the size of all the items in the table, plus the size of
         * all attributes projected into all of the local secondary indexes on that table. Use this estimate to measure
         * whether a local secondary index is approaching its size limit.
         *
         * <p>The estimate is subject to change over time; therefore, do not rely on the precision or accuracy of the
         * estimate.
         *
         * @return this builder.
         */
        public Builder sizeEstimateRangegB(List<Double> sizeEstimateRangegB) {
            this.sizeEstimateRangegB = sizeEstimateRangegB;
            return this;
        }

        @Override
        public ItemCollectionMetrics build() {
            return new ItemCollectionMetrics(this);
        }

        @Override
        @SuppressWarnings("unchecked")
        public void setMemberValue(Schema member, Object value) {
            switch (member.memberIndex()) {
                case 0 -> itemCollectionKey((Map<String, AttributeValue>) SchemaUtils.validateSameMember($SCHEMA_ITEM_COLLECTION_KEY, member, value));
                case 1 -> sizeEstimateRangegB((List<Double>) SchemaUtils.validateSameMember($SCHEMA_SIZE_ESTIMATE_RANGEG_B, member, value));
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
                    case 0 -> builder.itemCollectionKey(SharedSerde.deserializeItemCollectionKeyAttributeMap(member, de));
                    case 1 -> builder.sizeEstimateRangegB(SharedSerde.deserializeItemCollectionSizeEstimateRange(member, de));
                    default -> throw new IllegalArgumentException("Unexpected member: " + member.memberName());
                }
            }
        }
    }
}
