package software.amazon.awssdk.benchmark.smithyjava.dynamodb.model;

import java.util.Collections;
import java.util.List;
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

@SmithyGenerated
public final class TransactGetItemsInput implements SerializableStruct {

    public static final Schema $SCHEMA = Schemas.TRANSACT_GET_ITEMS_INPUT;
    private static final Schema $SCHEMA_TRANSACT_ITEMS = $SCHEMA.member("TransactItems");
    private static final Schema $SCHEMA_RETURN_CONSUMED_CAPACITY = $SCHEMA.member("ReturnConsumedCapacity");

    public static final ShapeId $ID = $SCHEMA.id();

    private final transient List<TransactGetItem> transactItems;
    private final transient ReturnConsumedCapacity returnConsumedCapacity;

    private TransactGetItemsInput(Builder builder) {
        this.transactItems = Collections.unmodifiableList(builder.transactItems);
        this.returnConsumedCapacity = builder.returnConsumedCapacity;
    }

    /**
     * An ordered array of up to 100 <code>TransactGetItem</code> objects, each of which contains a <code>Get</code>
     * structure.
     */
    public List<TransactGetItem> getTransactItems() {
        return transactItems;
    }

    public boolean hasTransactItems() {
        return true;
    }

    /**
     * A value of <code>TOTAL</code> causes consumed capacity information to be returned, and a value of
     * <code>NONE</code> prevents that information from being returned. No other value is valid.
     */
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
        TransactGetItemsInput that = (TransactGetItemsInput) other;
        return Objects.equals(this.returnConsumedCapacity, that.returnConsumedCapacity)
               && Objects.equals(this.transactItems, that.transactItems);
    }

    @Override
    public int hashCode() {
        int $hc = Objects.hashCode(transactItems);
        $hc = 31 * $hc + Objects.hashCode(returnConsumedCapacity);
        return $hc;
    }

    @Override
    public Schema schema() {
        return $SCHEMA;
    }

    @Override
    public void serializeMembers(ShapeSerializer serializer) {
        serializer.writeList($SCHEMA_TRANSACT_ITEMS, transactItems, transactItems.size(), SharedSerde.TransactGetItemListSerializer.INSTANCE);
        if (returnConsumedCapacity != null) {
            serializer.writeString($SCHEMA_RETURN_CONSUMED_CAPACITY, returnConsumedCapacity.getValue());
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T getMemberValue(Schema member) {
        return switch (member.memberIndex()) {
            case 0 -> (T) SchemaUtils.validateSameMember($SCHEMA_TRANSACT_ITEMS, member, transactItems);
            case 1 -> (T) SchemaUtils.validateSameMember($SCHEMA_RETURN_CONSUMED_CAPACITY, member, returnConsumedCapacity);
            default -> throw new IllegalArgumentException("Attempted to get non-existent member: " + member.id());
        };
    }

    /**
     * Create a new builder containing all the current property values of this object.
     *
     * <p><strong>Note:</strong> This method performs only a shallow copy of the original properties.
     *
     * @return a builder for {@link TransactGetItemsInput}.
     */
    public Builder toBuilder() {
        var builder = new Builder();
        builder.transactItems(this.transactItems);
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
     * Builder for {@link TransactGetItemsInput}.
     */
    public static final class Builder implements ShapeBuilder<TransactGetItemsInput> {
        private final PresenceTracker tracker = PresenceTracker.of($SCHEMA);
        private List<TransactGetItem> transactItems;
        private ReturnConsumedCapacity returnConsumedCapacity;

        private Builder() {}

        @Override
        public Schema schema() {
            return $SCHEMA;
        }

        /**
         * An ordered array of up to 100 <code>TransactGetItem</code> objects, each of which contains a <code>Get</code>
         * structure.
         *
         * <p><strong>Required</strong>
         * @return this builder.
         */
        public Builder transactItems(List<TransactGetItem> transactItems) {
            this.transactItems = Objects.requireNonNull(transactItems, "transactItems cannot be null");
            tracker.setMember($SCHEMA_TRANSACT_ITEMS);
            return this;
        }

        /**
         * A value of <code>TOTAL</code> causes consumed capacity information to be returned, and a value of
         * <code>NONE</code> prevents that information from being returned. No other value is valid.
         *
         * @return this builder.
         */
        public Builder returnConsumedCapacity(ReturnConsumedCapacity returnConsumedCapacity) {
            this.returnConsumedCapacity = returnConsumedCapacity;
            return this;
        }

        @Override
        public TransactGetItemsInput build() {
            tracker.validate();
            return new TransactGetItemsInput(this);
        }

        @Override
        @SuppressWarnings("unchecked")
        public void setMemberValue(Schema member, Object value) {
            switch (member.memberIndex()) {
                case 0 -> transactItems((List<TransactGetItem>) SchemaUtils.validateSameMember($SCHEMA_TRANSACT_ITEMS, member, value));
                case 1 -> returnConsumedCapacity((ReturnConsumedCapacity) SchemaUtils.validateSameMember($SCHEMA_RETURN_CONSUMED_CAPACITY, member, value));
                default -> ShapeBuilder.super.setMemberValue(member, value);
            }
        }

        @Override
        public ShapeBuilder<TransactGetItemsInput> errorCorrection() {
            if (tracker.allSet()) {
                return this;
            }
            if (!tracker.checkMember($SCHEMA_TRANSACT_ITEMS)) {
                transactItems(Collections.emptyList());
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
                    case 0 -> builder.transactItems(SharedSerde.deserializeTransactGetItemList(member, de));
                    case 1 -> builder.returnConsumedCapacity(ReturnConsumedCapacity.builder().deserializeMember(de, member).build());
                    default -> throw new IllegalArgumentException("Unexpected member: " + member.memberName());
                }
            }
        }
    }
}
