package software.amazon.awssdk.benchmark.smithyjava.dynamodb.model;

import java.util.Collections;
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
 * Represents the output of an <code>UpdateItem</code> operation.
 */
@SmithyGenerated
public final class UpdateItemOutput implements SerializableStruct {

    public static final Schema $SCHEMA = Schemas.UPDATE_ITEM_OUTPUT;
    private static final Schema $SCHEMA_ATTRIBUTES = $SCHEMA.member("Attributes");
    private static final Schema $SCHEMA_CONSUMED_CAPACITY = $SCHEMA.member("ConsumedCapacity");
    private static final Schema $SCHEMA_ITEM_COLLECTION_METRICS = $SCHEMA.member("ItemCollectionMetrics");

    public static final ShapeId $ID = $SCHEMA.id();

    private final transient Map<String, AttributeValue> attributes;
    private final transient ConsumedCapacity consumedCapacity;
    private final transient ItemCollectionMetrics itemCollectionMetrics;

    private UpdateItemOutput(Builder builder) {
        this.attributes = builder.attributes == null ? null : Collections.unmodifiableMap(builder.attributes);
        this.consumedCapacity = builder.consumedCapacity;
        this.itemCollectionMetrics = builder.itemCollectionMetrics;
    }

    /**
     * A map of attribute values as they appear before or after the <code>UpdateItem</code> operation, as determined by
     * the <code>ReturnValues</code> parameter.
     *
     * <p>The <code>Attributes</code> map is only present if the update was successful and <code>ReturnValues</code> was
     * specified as something other than <code>NONE</code> in the request. Each element represents one attribute.
     */
    public Map<String, AttributeValue> getAttributes() {
        if (attributes == null) {
            return Collections.emptyMap();
        }
        return attributes;
    }

    public boolean hasAttributes() {
        return attributes != null;
    }

    /**
     * The capacity units consumed by the <code>UpdateItem</code> operation. The data returned includes the total
     * provisioned throughput consumed, along with statistics for the table and any indexes involved in the operation. <code>
     * ConsumedCapacity</code> is only returned if the <code>ReturnConsumedCapacity</code> parameter was specified. For
     * more information, see <a href="https://docs.aws.amazon.com/amazondynamodb/latest/developerguide/read-write-operations.html#write-operation-consumption">Capacity unity consumption for write operations</a> in the <i>Amazon DynamoDB Developer
     * Guide</i>.
     *
     * <p>If the table has vector indexes, the response includes a <code>VectorIndexes</code> field with <code>
     * VectorWriteRequestBytes</code> consumed for each affected vector index.
     */
    public ConsumedCapacity getConsumedCapacity() {
        return consumedCapacity;
    }

    /**
     * Information about item collections, if any, that were affected by the <code>UpdateItem</code> operation. <code>
     * ItemCollectionMetrics</code> is only returned if the <code>ReturnItemCollectionMetrics</code> parameter was
     * specified. If the table does not have any local secondary indexes, this information is not returned in the
     * response.
     *
     * <p>Each <code>ItemCollectionMetrics</code> element consists of:
     *
     * <ul>
     *   <li>
     *     <code>ItemCollectionKey</code> - The partition key value of the item collection. This is the same as the
     *     partition key value of the item itself.
     *   </li>
     *   <li>
     *     <code>SizeEstimateRangeGB</code> - An estimate of item collection size, in gigabytes. This value is a
     *     two-element array containing a lower bound and an upper bound for the estimate. The estimate includes the
     *     size of all the items in the table, plus the size of all attributes projected into all of the local
     *     secondary indexes on that table. Use this estimate to measure whether a local secondary index is
     *     approaching its size limit.The estimate is subject to change over time; therefore, do not rely on the
     *     precision or accuracy of the estimate.
     *   </li>
     * </ul>
     */
    public ItemCollectionMetrics getItemCollectionMetrics() {
        return itemCollectionMetrics;
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
        UpdateItemOutput that = (UpdateItemOutput) other;
        return Objects.equals(this.consumedCapacity, that.consumedCapacity)
               && Objects.equals(this.itemCollectionMetrics, that.itemCollectionMetrics)
               && Objects.equals(this.attributes, that.attributes);
    }

    @Override
    public int hashCode() {
        int $hc = Objects.hashCode(attributes);
        $hc = 31 * $hc + Objects.hashCode(consumedCapacity);
        $hc = 31 * $hc + Objects.hashCode(itemCollectionMetrics);
        return $hc;
    }

    @Override
    public Schema schema() {
        return $SCHEMA;
    }

    @Override
    public void serializeMembers(ShapeSerializer serializer) {
        if (attributes != null) {
            serializer.writeMap($SCHEMA_ATTRIBUTES, attributes, attributes.size(), SharedSerde.AttributeMapSerializer.INSTANCE);
        }
        if (consumedCapacity != null) {
            serializer.writeStruct($SCHEMA_CONSUMED_CAPACITY, consumedCapacity);
        }
        if (itemCollectionMetrics != null) {
            serializer.writeStruct($SCHEMA_ITEM_COLLECTION_METRICS, itemCollectionMetrics);
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T getMemberValue(Schema member) {
        return switch (member.memberIndex()) {
            case 0 -> (T) SchemaUtils.validateSameMember($SCHEMA_ATTRIBUTES, member, attributes);
            case 1 -> (T) SchemaUtils.validateSameMember($SCHEMA_CONSUMED_CAPACITY, member, consumedCapacity);
            case 2 -> (T) SchemaUtils.validateSameMember($SCHEMA_ITEM_COLLECTION_METRICS, member, itemCollectionMetrics);
            default -> throw new IllegalArgumentException("Attempted to get non-existent member: " + member.id());
        };
    }

    /**
     * Create a new builder containing all the current property values of this object.
     *
     * <p><strong>Note:</strong> This method performs only a shallow copy of the original properties.
     *
     * @return a builder for {@link UpdateItemOutput}.
     */
    public Builder toBuilder() {
        var builder = new Builder();
        builder.attributes(this.attributes);
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
     * Builder for {@link UpdateItemOutput}.
     */
    public static final class Builder implements ShapeBuilder<UpdateItemOutput> {
        private Map<String, AttributeValue> attributes;
        private ConsumedCapacity consumedCapacity;
        private ItemCollectionMetrics itemCollectionMetrics;

        private Builder() {}

        @Override
        public Schema schema() {
            return $SCHEMA;
        }

        /**
         * A map of attribute values as they appear before or after the <code>UpdateItem</code> operation, as determined by
         * the <code>ReturnValues</code> parameter.
         *
         * <p>The <code>Attributes</code> map is only present if the update was successful and <code>ReturnValues</code> was
         * specified as something other than <code>NONE</code> in the request. Each element represents one attribute.
         *
         * @return this builder.
         */
        public Builder attributes(Map<String, AttributeValue> attributes) {
            this.attributes = attributes;
            return this;
        }

        /**
         * The capacity units consumed by the <code>UpdateItem</code> operation. The data returned includes the total
         * provisioned throughput consumed, along with statistics for the table and any indexes involved in the operation. <code>
         * ConsumedCapacity</code> is only returned if the <code>ReturnConsumedCapacity</code> parameter was specified. For
         * more information, see <a href="https://docs.aws.amazon.com/amazondynamodb/latest/developerguide/read-write-operations.html#write-operation-consumption">Capacity unity consumption for write operations</a> in the <i>Amazon DynamoDB Developer
         * Guide</i>.
         *
         * <p>If the table has vector indexes, the response includes a <code>VectorIndexes</code> field with <code>
         * VectorWriteRequestBytes</code> consumed for each affected vector index.
         *
         * @return this builder.
         */
        public Builder consumedCapacity(ConsumedCapacity consumedCapacity) {
            this.consumedCapacity = consumedCapacity;
            return this;
        }

        /**
         * Information about item collections, if any, that were affected by the <code>UpdateItem</code> operation. <code>
         * ItemCollectionMetrics</code> is only returned if the <code>ReturnItemCollectionMetrics</code> parameter was
         * specified. If the table does not have any local secondary indexes, this information is not returned in the
         * response.
         *
         * <p>Each <code>ItemCollectionMetrics</code> element consists of:
         *
         * <ul>
         *   <li>
         *     <code>ItemCollectionKey</code> - The partition key value of the item collection. This is the same as the
         *     partition key value of the item itself.
         *   </li>
         *   <li>
         *     <code>SizeEstimateRangeGB</code> - An estimate of item collection size, in gigabytes. This value is a
         *     two-element array containing a lower bound and an upper bound for the estimate. The estimate includes the
         *     size of all the items in the table, plus the size of all attributes projected into all of the local
         *     secondary indexes on that table. Use this estimate to measure whether a local secondary index is
         *     approaching its size limit.The estimate is subject to change over time; therefore, do not rely on the
         *     precision or accuracy of the estimate.
         *   </li>
         * </ul>
         *
         * @return this builder.
         */
        public Builder itemCollectionMetrics(ItemCollectionMetrics itemCollectionMetrics) {
            this.itemCollectionMetrics = itemCollectionMetrics;
            return this;
        }

        @Override
        public UpdateItemOutput build() {
            return new UpdateItemOutput(this);
        }

        @Override
        @SuppressWarnings("unchecked")
        public void setMemberValue(Schema member, Object value) {
            switch (member.memberIndex()) {
                case 0 -> attributes((Map<String, AttributeValue>) SchemaUtils.validateSameMember($SCHEMA_ATTRIBUTES, member, value));
                case 1 -> consumedCapacity((ConsumedCapacity) SchemaUtils.validateSameMember($SCHEMA_CONSUMED_CAPACITY, member, value));
                case 2 -> itemCollectionMetrics((ItemCollectionMetrics) SchemaUtils.validateSameMember($SCHEMA_ITEM_COLLECTION_METRICS, member, value));
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
                    case 0 -> builder.attributes(SharedSerde.deserializeAttributeMap(member, de));
                    case 1 -> builder.consumedCapacity(ConsumedCapacity.builder().deserializeMember(de, member).build());
                    case 2 -> builder.itemCollectionMetrics(ItemCollectionMetrics.builder().deserializeMember(de, member).build());
                    default -> throw new IllegalArgumentException("Unexpected member: " + member.memberName());
                }
            }
        }
    }
}
