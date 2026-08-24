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
public final class ExecuteStatementOutput implements SerializableStruct {

    public static final Schema $SCHEMA = Schemas.EXECUTE_STATEMENT_OUTPUT;
    private static final Schema $SCHEMA_ITEMS = $SCHEMA.member("Items");
    private static final Schema $SCHEMA_NEXT_TOKEN = $SCHEMA.member("NextToken");
    private static final Schema $SCHEMA_CONSUMED_CAPACITY = $SCHEMA.member("ConsumedCapacity");
    private static final Schema $SCHEMA_LAST_EVALUATED_KEY = $SCHEMA.member("LastEvaluatedKey");

    public static final ShapeId $ID = $SCHEMA.id();

    private final transient List<Map<String, AttributeValue>> items;
    private final transient String nextToken;
    private final transient ConsumedCapacity consumedCapacity;
    private final transient Map<String, AttributeValue> lastEvaluatedKey;

    private ExecuteStatementOutput(Builder builder) {
        this.items = builder.items == null ? null : Collections.unmodifiableList(builder.items);
        this.nextToken = builder.nextToken;
        this.consumedCapacity = builder.consumedCapacity;
        this.lastEvaluatedKey = builder.lastEvaluatedKey == null ? null : Collections.unmodifiableMap(builder.lastEvaluatedKey);
    }

    /**
     * If a read operation was used, this property will contain the result of the read operation; a map of attribute
     * names and their values. For the write operations this value will be empty.
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
     * If the response of a read request exceeds the response payload limit DynamoDB will set this value in the
     * response. If set, you can use that this value in the subsequent request to get the remaining results.
     */
    public String getNextToken() {
        return nextToken;
    }

    public ConsumedCapacity getConsumedCapacity() {
        return consumedCapacity;
    }

    /**
     * The primary key of the item where the operation stopped, inclusive of the previous result set. Use this value to
     * start a new operation, excluding this value in the new request. If <code>LastEvaluatedKey</code> is empty, then
     * the "last page" of results has been processed and there is no more data to be retrieved. If
     * <code>LastEvaluatedKey</code> is not empty, it does not necessarily mean that there is more data in the result
     * set. The only way to know when you have reached the end of the result set is when <code>LastEvaluatedKey</code>
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
        ExecuteStatementOutput that = (ExecuteStatementOutput) other;
        return Objects.equals(this.nextToken, that.nextToken)
               && Objects.equals(this.consumedCapacity, that.consumedCapacity)
               && Objects.equals(this.items, that.items)
               && Objects.equals(this.lastEvaluatedKey, that.lastEvaluatedKey);
    }

    @Override
    public int hashCode() {
        int $hc = Objects.hashCode(items);
        $hc = 31 * $hc + Objects.hashCode(nextToken);
        $hc = 31 * $hc + Objects.hashCode(consumedCapacity);
        $hc = 31 * $hc + Objects.hashCode(lastEvaluatedKey);
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
        if (nextToken != null) {
            serializer.writeString($SCHEMA_NEXT_TOKEN, nextToken);
        }
        if (consumedCapacity != null) {
            serializer.writeStruct($SCHEMA_CONSUMED_CAPACITY, consumedCapacity);
        }
        if (lastEvaluatedKey != null) {
            serializer.writeMap($SCHEMA_LAST_EVALUATED_KEY, lastEvaluatedKey, lastEvaluatedKey.size(), SharedSerde.KeySerializer.INSTANCE);
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T getMemberValue(Schema member) {
        return switch (member.memberIndex()) {
            case 0 -> (T) SchemaUtils.validateSameMember($SCHEMA_ITEMS, member, items);
            case 1 -> (T) SchemaUtils.validateSameMember($SCHEMA_NEXT_TOKEN, member, nextToken);
            case 2 -> (T) SchemaUtils.validateSameMember($SCHEMA_CONSUMED_CAPACITY, member, consumedCapacity);
            case 3 -> (T) SchemaUtils.validateSameMember($SCHEMA_LAST_EVALUATED_KEY, member, lastEvaluatedKey);
            default -> throw new IllegalArgumentException("Attempted to get non-existent member: " + member.id());
        };
    }

    /**
     * Create a new builder containing all the current property values of this object.
     *
     * <p><strong>Note:</strong> This method performs only a shallow copy of the original properties.
     *
     * @return a builder for {@link ExecuteStatementOutput}.
     */
    public Builder toBuilder() {
        var builder = new Builder();
        builder.items(this.items);
        builder.nextToken(this.nextToken);
        builder.consumedCapacity(this.consumedCapacity);
        builder.lastEvaluatedKey(this.lastEvaluatedKey);
        return builder;
    }

    /**
     * @return returns a new Builder.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for {@link ExecuteStatementOutput}.
     */
    public static final class Builder implements ShapeBuilder<ExecuteStatementOutput> {
        private List<Map<String, AttributeValue>> items;
        private String nextToken;
        private ConsumedCapacity consumedCapacity;
        private Map<String, AttributeValue> lastEvaluatedKey;

        private Builder() {}

        @Override
        public Schema schema() {
            return $SCHEMA;
        }

        /**
         * If a read operation was used, this property will contain the result of the read operation; a map of attribute
         * names and their values. For the write operations this value will be empty.
         *
         * @return this builder.
         */
        public Builder items(List<Map<String, AttributeValue>> items) {
            this.items = items;
            return this;
        }

        /**
         * If the response of a read request exceeds the response payload limit DynamoDB will set this value in the
         * response. If set, you can use that this value in the subsequent request to get the remaining results.
         *
         * @return this builder.
         */
        public Builder nextToken(String nextToken) {
            this.nextToken = nextToken;
            return this;
        }

        /**
         * @return this builder.
         */
        public Builder consumedCapacity(ConsumedCapacity consumedCapacity) {
            this.consumedCapacity = consumedCapacity;
            return this;
        }

        /**
         * The primary key of the item where the operation stopped, inclusive of the previous result set. Use this value to
         * start a new operation, excluding this value in the new request. If <code>LastEvaluatedKey</code> is empty, then
         * the "last page" of results has been processed and there is no more data to be retrieved. If
         * <code>LastEvaluatedKey</code> is not empty, it does not necessarily mean that there is more data in the result
         * set. The only way to know when you have reached the end of the result set is when <code>LastEvaluatedKey</code>
         * is empty.
         *
         * @return this builder.
         */
        public Builder lastEvaluatedKey(Map<String, AttributeValue> lastEvaluatedKey) {
            this.lastEvaluatedKey = lastEvaluatedKey;
            return this;
        }

        @Override
        public ExecuteStatementOutput build() {
            return new ExecuteStatementOutput(this);
        }

        @Override
        @SuppressWarnings("unchecked")
        public void setMemberValue(Schema member, Object value) {
            switch (member.memberIndex()) {
                case 0 -> items((List<Map<String, AttributeValue>>) SchemaUtils.validateSameMember($SCHEMA_ITEMS, member, value));
                case 1 -> nextToken((String) SchemaUtils.validateSameMember($SCHEMA_NEXT_TOKEN, member, value));
                case 2 -> consumedCapacity((ConsumedCapacity) SchemaUtils.validateSameMember($SCHEMA_CONSUMED_CAPACITY, member, value));
                case 3 -> lastEvaluatedKey((Map<String, AttributeValue>) SchemaUtils.validateSameMember($SCHEMA_LAST_EVALUATED_KEY, member, value));
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
                    case 1 -> builder.nextToken(de.readString(member));
                    case 2 -> builder.consumedCapacity(ConsumedCapacity.builder().deserializeMember(de, member).build());
                    case 3 -> builder.lastEvaluatedKey(SharedSerde.deserializeKey(member, de));
                    default -> throw new IllegalArgumentException("Unexpected member: " + member.memberName());
                }
            }
        }
    }
}
