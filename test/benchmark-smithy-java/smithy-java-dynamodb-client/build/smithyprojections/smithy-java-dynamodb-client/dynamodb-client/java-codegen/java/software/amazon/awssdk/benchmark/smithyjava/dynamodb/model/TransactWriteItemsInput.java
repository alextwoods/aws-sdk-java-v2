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
public final class TransactWriteItemsInput implements SerializableStruct {

    public static final Schema $SCHEMA = Schemas.TRANSACT_WRITE_ITEMS_INPUT;
    private static final Schema $SCHEMA_TRANSACT_ITEMS = $SCHEMA.member("TransactItems");
    private static final Schema $SCHEMA_RETURN_CONSUMED_CAPACITY = $SCHEMA.member("ReturnConsumedCapacity");
    private static final Schema $SCHEMA_RETURN_ITEM_COLLECTION_METRICS = $SCHEMA.member("ReturnItemCollectionMetrics");
    private static final Schema $SCHEMA_CLIENT_REQUEST_TOKEN = $SCHEMA.member("ClientRequestToken");

    public static final ShapeId $ID = $SCHEMA.id();

    private final transient List<TransactWriteItem> transactItems;
    private final transient ReturnConsumedCapacity returnConsumedCapacity;
    private final transient ReturnItemCollectionMetrics returnItemCollectionMetrics;
    private final transient String clientRequestToken;

    private TransactWriteItemsInput(Builder builder) {
        this.transactItems = Collections.unmodifiableList(builder.transactItems);
        this.returnConsumedCapacity = builder.returnConsumedCapacity;
        this.returnItemCollectionMetrics = builder.returnItemCollectionMetrics;
        this.clientRequestToken = builder.clientRequestToken;
    }

    /**
     * An ordered array of up to 100 <code>TransactWriteItem</code> objects, each of which contains a
     * <code>ConditionCheck</code>, <code>Put</code>, <code>Update</code>, or <code>Delete</code> object. These can
     * operate on items in different tables, but the tables must reside in the same Amazon Web Services account and
     * Region, and no two of them can operate on the same item.
     */
    public List<TransactWriteItem> getTransactItems() {
        return transactItems;
    }

    public boolean hasTransactItems() {
        return true;
    }

    public ReturnConsumedCapacity getReturnConsumedCapacity() {
        return returnConsumedCapacity;
    }

    /**
     * Determines whether item collection metrics are returned. If set to <code>SIZE</code>, the response includes
     * statistics about item collections (if any), that were modified during the operation and are returned in the
     * response. If set to <code>NONE</code> (the default), no statistics are returned.
     */
    public ReturnItemCollectionMetrics getReturnItemCollectionMetrics() {
        return returnItemCollectionMetrics;
    }

    /**
     * Providing a <code>ClientRequestToken</code> makes the call to <code>TransactWriteItems</code> idempotent, meaning
     * that multiple identical calls have the same effect as one single call.
     *
     * <p>Although multiple identical calls using the same client request token produce the same result on the server
     * (no side effects), the responses to the calls might not be the same. If the <code>ReturnConsumedCapacity</code>
     * parameter is set, then the initial <code>TransactWriteItems</code> call returns the amount of write capacity
     * units consumed in making the changes. Subsequent <code>TransactWriteItems</code> calls with the same client token
     * return the number of read capacity units consumed in reading the item.
     *
     * <p>A client request token is valid for 10 minutes after the first request that uses it is completed. After 10
     * minutes, any request with the same client token is treated as a new request. Do not resubmit the same request
     * with the same client token for more than 10 minutes, or the result might not be idempotent.
     *
     * <p>If you submit a request with the same client token but a change in other parameters within the 10-minute
     * idempotency window, DynamoDB returns an <code>IdempotentParameterMismatch</code> exception.
     */
    public String getClientRequestToken() {
        return clientRequestToken;
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
        TransactWriteItemsInput that = (TransactWriteItemsInput) other;
        return Objects.equals(this.clientRequestToken, that.clientRequestToken)
               && Objects.equals(this.returnConsumedCapacity, that.returnConsumedCapacity)
               && Objects.equals(this.returnItemCollectionMetrics, that.returnItemCollectionMetrics)
               && Objects.equals(this.transactItems, that.transactItems);
    }

    @Override
    public int hashCode() {
        int $hc = Objects.hashCode(transactItems);
        $hc = 31 * $hc + Objects.hashCode(returnConsumedCapacity);
        $hc = 31 * $hc + Objects.hashCode(returnItemCollectionMetrics);
        $hc = 31 * $hc + Objects.hashCode(clientRequestToken);
        return $hc;
    }

    @Override
    public Schema schema() {
        return $SCHEMA;
    }

    @Override
    public void serializeMembers(ShapeSerializer serializer) {
        serializer.writeList($SCHEMA_TRANSACT_ITEMS, transactItems, transactItems.size(), SharedSerde.TransactWriteItemListSerializer.INSTANCE);
        if (returnConsumedCapacity != null) {
            serializer.writeString($SCHEMA_RETURN_CONSUMED_CAPACITY, returnConsumedCapacity.getValue());
        }
        if (returnItemCollectionMetrics != null) {
            serializer.writeString($SCHEMA_RETURN_ITEM_COLLECTION_METRICS, returnItemCollectionMetrics.getValue());
        }
        if (clientRequestToken != null) {
            serializer.writeString($SCHEMA_CLIENT_REQUEST_TOKEN, clientRequestToken);
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T getMemberValue(Schema member) {
        return switch (member.memberIndex()) {
            case 0 -> (T) SchemaUtils.validateSameMember($SCHEMA_TRANSACT_ITEMS, member, transactItems);
            case 1 -> (T) SchemaUtils.validateSameMember($SCHEMA_RETURN_CONSUMED_CAPACITY, member, returnConsumedCapacity);
            case 2 -> (T) SchemaUtils.validateSameMember($SCHEMA_RETURN_ITEM_COLLECTION_METRICS, member, returnItemCollectionMetrics);
            case 3 -> (T) SchemaUtils.validateSameMember($SCHEMA_CLIENT_REQUEST_TOKEN, member, clientRequestToken);
            default -> throw new IllegalArgumentException("Attempted to get non-existent member: " + member.id());
        };
    }

    /**
     * Create a new builder containing all the current property values of this object.
     *
     * <p><strong>Note:</strong> This method performs only a shallow copy of the original properties.
     *
     * @return a builder for {@link TransactWriteItemsInput}.
     */
    public Builder toBuilder() {
        var builder = new Builder();
        builder.transactItems(this.transactItems);
        builder.returnConsumedCapacity(this.returnConsumedCapacity);
        builder.returnItemCollectionMetrics(this.returnItemCollectionMetrics);
        builder.clientRequestToken(this.clientRequestToken);
        return builder;
    }

    /**
     * @return returns a new Builder.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for {@link TransactWriteItemsInput}.
     */
    public static final class Builder implements ShapeBuilder<TransactWriteItemsInput> {
        private final PresenceTracker tracker = PresenceTracker.of($SCHEMA);
        private List<TransactWriteItem> transactItems;
        private ReturnConsumedCapacity returnConsumedCapacity;
        private ReturnItemCollectionMetrics returnItemCollectionMetrics;
        private String clientRequestToken;

        private Builder() {}

        @Override
        public Schema schema() {
            return $SCHEMA;
        }

        /**
         * An ordered array of up to 100 <code>TransactWriteItem</code> objects, each of which contains a
         * <code>ConditionCheck</code>, <code>Put</code>, <code>Update</code>, or <code>Delete</code> object. These can
         * operate on items in different tables, but the tables must reside in the same Amazon Web Services account and
         * Region, and no two of them can operate on the same item.
         *
         * <p><strong>Required</strong>
         * @return this builder.
         */
        public Builder transactItems(List<TransactWriteItem> transactItems) {
            this.transactItems = Objects.requireNonNull(transactItems, "transactItems cannot be null");
            tracker.setMember($SCHEMA_TRANSACT_ITEMS);
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
         * statistics about item collections (if any), that were modified during the operation and are returned in the
         * response. If set to <code>NONE</code> (the default), no statistics are returned.
         *
         * @return this builder.
         */
        public Builder returnItemCollectionMetrics(ReturnItemCollectionMetrics returnItemCollectionMetrics) {
            this.returnItemCollectionMetrics = returnItemCollectionMetrics;
            return this;
        }

        /**
         * Providing a <code>ClientRequestToken</code> makes the call to <code>TransactWriteItems</code> idempotent, meaning
         * that multiple identical calls have the same effect as one single call.
         *
         * <p>Although multiple identical calls using the same client request token produce the same result on the server
         * (no side effects), the responses to the calls might not be the same. If the <code>ReturnConsumedCapacity</code>
         * parameter is set, then the initial <code>TransactWriteItems</code> call returns the amount of write capacity
         * units consumed in making the changes. Subsequent <code>TransactWriteItems</code> calls with the same client token
         * return the number of read capacity units consumed in reading the item.
         *
         * <p>A client request token is valid for 10 minutes after the first request that uses it is completed. After 10
         * minutes, any request with the same client token is treated as a new request. Do not resubmit the same request
         * with the same client token for more than 10 minutes, or the result might not be idempotent.
         *
         * <p>If you submit a request with the same client token but a change in other parameters within the 10-minute
         * idempotency window, DynamoDB returns an <code>IdempotentParameterMismatch</code> exception.
         *
         * @return this builder.
         */
        public Builder clientRequestToken(String clientRequestToken) {
            this.clientRequestToken = clientRequestToken;
            return this;
        }

        @Override
        public TransactWriteItemsInput build() {
            tracker.validate();
            return new TransactWriteItemsInput(this);
        }

        @Override
        @SuppressWarnings("unchecked")
        public void setMemberValue(Schema member, Object value) {
            switch (member.memberIndex()) {
                case 0 -> transactItems((List<TransactWriteItem>) SchemaUtils.validateSameMember($SCHEMA_TRANSACT_ITEMS, member, value));
                case 1 -> returnConsumedCapacity((ReturnConsumedCapacity) SchemaUtils.validateSameMember($SCHEMA_RETURN_CONSUMED_CAPACITY, member, value));
                case 2 -> returnItemCollectionMetrics((ReturnItemCollectionMetrics) SchemaUtils.validateSameMember($SCHEMA_RETURN_ITEM_COLLECTION_METRICS, member, value));
                case 3 -> clientRequestToken((String) SchemaUtils.validateSameMember($SCHEMA_CLIENT_REQUEST_TOKEN, member, value));
                default -> ShapeBuilder.super.setMemberValue(member, value);
            }
        }

        @Override
        public ShapeBuilder<TransactWriteItemsInput> errorCorrection() {
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
                    case 0 -> builder.transactItems(SharedSerde.deserializeTransactWriteItemList(member, de));
                    case 1 -> builder.returnConsumedCapacity(ReturnConsumedCapacity.builder().deserializeMember(de, member).build());
                    case 2 -> builder.returnItemCollectionMetrics(ReturnItemCollectionMetrics.builder().deserializeMember(de, member).build());
                    case 3 -> builder.clientRequestToken(de.readString(member));
                    default -> throw new IllegalArgumentException("Unexpected member: " + member.memberName());
                }
            }
        }
    }
}
