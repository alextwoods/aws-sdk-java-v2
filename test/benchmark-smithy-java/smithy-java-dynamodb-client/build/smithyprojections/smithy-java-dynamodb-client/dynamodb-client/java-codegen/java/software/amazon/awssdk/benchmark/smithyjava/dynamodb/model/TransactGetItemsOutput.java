package software.amazon.awssdk.benchmark.smithyjava.dynamodb.model;

import java.util.Collections;
import java.util.List;
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
public final class TransactGetItemsOutput implements SerializableStruct {

    public static final Schema $SCHEMA = Schemas.TRANSACT_GET_ITEMS_OUTPUT;
    private static final Schema $SCHEMA_CONSUMED_CAPACITY = $SCHEMA.member("ConsumedCapacity");
    private static final Schema $SCHEMA_RESPONSES = $SCHEMA.member("Responses");

    public static final ShapeId $ID = $SCHEMA.id();

    private final transient List<ConsumedCapacity> consumedCapacity;
    private final transient List<ItemResponse> responses;

    private TransactGetItemsOutput(Builder builder) {
        this.consumedCapacity = builder.consumedCapacity == null ? null : Collections.unmodifiableList(builder.consumedCapacity);
        this.responses = builder.responses == null ? null : Collections.unmodifiableList(builder.responses);
    }

    /**
     * If the <i>ReturnConsumedCapacity</i> value was <code>TOTAL</code>, this is an array of <code>ConsumedCapacity</code>
     * objects, one for each table addressed by <code>TransactGetItem</code> objects in the <i>TransactItems</i>
     * parameter. These <code>ConsumedCapacity</code> objects report the read-capacity units consumed by the <code>
     * TransactGetItems</code> call in that table.
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
     * An ordered array of up to 100 <code>ItemResponse</code> objects, each of which corresponds to the <code>
     * TransactGetItem</code> object in the same position in the <i>TransactItems</i> array. Each <code>ItemResponse</code>
     * object contains a Map of the name-value pairs that are the projected attributes of the requested item.
     *
     * <p>If a requested item could not be retrieved, the corresponding <code>ItemResponse</code> object is Null, or if
     * the requested item has no projected attributes, the corresponding <code>ItemResponse</code> object is an empty
     * Map.
     */
    public List<ItemResponse> getResponses() {
        if (responses == null) {
            return Collections.emptyList();
        }
        return responses;
    }

    public boolean hasResponses() {
        return responses != null;
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
        TransactGetItemsOutput that = (TransactGetItemsOutput) other;
        return Objects.equals(this.consumedCapacity, that.consumedCapacity)
               && Objects.equals(this.responses, that.responses);
    }

    @Override
    public int hashCode() {
        int $hc = Objects.hashCode(consumedCapacity);
        $hc = 31 * $hc + Objects.hashCode(responses);
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
        if (responses != null) {
            serializer.writeList($SCHEMA_RESPONSES, responses, responses.size(), SharedSerde.ItemResponseListSerializer.INSTANCE);
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T getMemberValue(Schema member) {
        return switch (member.memberIndex()) {
            case 0 -> (T) SchemaUtils.validateSameMember($SCHEMA_CONSUMED_CAPACITY, member, consumedCapacity);
            case 1 -> (T) SchemaUtils.validateSameMember($SCHEMA_RESPONSES, member, responses);
            default -> throw new IllegalArgumentException("Attempted to get non-existent member: " + member.id());
        };
    }

    /**
     * Create a new builder containing all the current property values of this object.
     *
     * <p><strong>Note:</strong> This method performs only a shallow copy of the original properties.
     *
     * @return a builder for {@link TransactGetItemsOutput}.
     */
    public Builder toBuilder() {
        var builder = new Builder();
        builder.consumedCapacity(this.consumedCapacity);
        builder.responses(this.responses);
        return builder;
    }

    /**
     * @return returns a new Builder.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for {@link TransactGetItemsOutput}.
     */
    public static final class Builder implements ShapeBuilder<TransactGetItemsOutput> {
        private List<ConsumedCapacity> consumedCapacity;
        private List<ItemResponse> responses;

        private Builder() {}

        @Override
        public Schema schema() {
            return $SCHEMA;
        }

        /**
         * If the <i>ReturnConsumedCapacity</i> value was <code>TOTAL</code>, this is an array of <code>ConsumedCapacity</code>
         * objects, one for each table addressed by <code>TransactGetItem</code> objects in the <i>TransactItems</i>
         * parameter. These <code>ConsumedCapacity</code> objects report the read-capacity units consumed by the <code>
         * TransactGetItems</code> call in that table.
         *
         * @return this builder.
         */
        public Builder consumedCapacity(List<ConsumedCapacity> consumedCapacity) {
            this.consumedCapacity = consumedCapacity;
            return this;
        }

        /**
         * An ordered array of up to 100 <code>ItemResponse</code> objects, each of which corresponds to the <code>
         * TransactGetItem</code> object in the same position in the <i>TransactItems</i> array. Each <code>ItemResponse</code>
         * object contains a Map of the name-value pairs that are the projected attributes of the requested item.
         *
         * <p>If a requested item could not be retrieved, the corresponding <code>ItemResponse</code> object is Null, or if
         * the requested item has no projected attributes, the corresponding <code>ItemResponse</code> object is an empty
         * Map.
         *
         * @return this builder.
         */
        public Builder responses(List<ItemResponse> responses) {
            this.responses = responses;
            return this;
        }

        @Override
        public TransactGetItemsOutput build() {
            return new TransactGetItemsOutput(this);
        }

        @Override
        @SuppressWarnings("unchecked")
        public void setMemberValue(Schema member, Object value) {
            switch (member.memberIndex()) {
                case 0 -> consumedCapacity((List<ConsumedCapacity>) SchemaUtils.validateSameMember($SCHEMA_CONSUMED_CAPACITY, member, value));
                case 1 -> responses((List<ItemResponse>) SchemaUtils.validateSameMember($SCHEMA_RESPONSES, member, value));
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
                    case 1 -> builder.responses(SharedSerde.deserializeItemResponseList(member, de));
                    default -> throw new IllegalArgumentException("Unexpected member: " + member.memberName());
                }
            }
        }
    }
}
