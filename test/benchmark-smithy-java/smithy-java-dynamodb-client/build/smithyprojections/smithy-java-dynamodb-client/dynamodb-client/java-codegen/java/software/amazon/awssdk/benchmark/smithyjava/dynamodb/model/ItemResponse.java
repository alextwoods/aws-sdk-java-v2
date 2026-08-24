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
 * Details for the requested item.
 */
@SmithyGenerated
public final class ItemResponse implements SerializableStruct {

    public static final Schema $SCHEMA = Schemas.ITEM_RESPONSE;
    private static final Schema $SCHEMA_ITEM = $SCHEMA.member("Item");

    public static final ShapeId $ID = $SCHEMA.id();

    private final transient Map<String, AttributeValue> item;

    private ItemResponse(Builder builder) {
        this.item = builder.item == null ? null : Collections.unmodifiableMap(builder.item);
    }

    /**
     * Map of attribute data consisting of the data type and attribute value.
     */
    public Map<String, AttributeValue> getItem() {
        if (item == null) {
            return Collections.emptyMap();
        }
        return item;
    }

    public boolean hasItem() {
        return item != null;
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
        ItemResponse that = (ItemResponse) other;
        return Objects.equals(this.item, that.item);
    }

    @Override
    public int hashCode() {
        int $hc = Objects.hashCode(item);
        return $hc;
    }

    @Override
    public Schema schema() {
        return $SCHEMA;
    }

    @Override
    public void serializeMembers(ShapeSerializer serializer) {
        if (item != null) {
            serializer.writeMap($SCHEMA_ITEM, item, item.size(), SharedSerde.AttributeMapSerializer.INSTANCE);
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T getMemberValue(Schema member) {
        return switch (member.memberIndex()) {
            case 0 -> (T) SchemaUtils.validateSameMember($SCHEMA_ITEM, member, item);
            default -> throw new IllegalArgumentException("Attempted to get non-existent member: " + member.id());
        };
    }

    /**
     * Create a new builder containing all the current property values of this object.
     *
     * <p><strong>Note:</strong> This method performs only a shallow copy of the original properties.
     *
     * @return a builder for {@link ItemResponse}.
     */
    public Builder toBuilder() {
        var builder = new Builder();
        builder.item(this.item);
        return builder;
    }

    /**
     * @return returns a new Builder.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for {@link ItemResponse}.
     */
    public static final class Builder implements ShapeBuilder<ItemResponse> {
        private Map<String, AttributeValue> item;

        private Builder() {}

        @Override
        public Schema schema() {
            return $SCHEMA;
        }

        /**
         * Map of attribute data consisting of the data type and attribute value.
         *
         * @return this builder.
         */
        public Builder item(Map<String, AttributeValue> item) {
            this.item = item;
            return this;
        }

        @Override
        public ItemResponse build() {
            return new ItemResponse(this);
        }

        @Override
        @SuppressWarnings("unchecked")
        public void setMemberValue(Schema member, Object value) {
            switch (member.memberIndex()) {
                case 0 -> item((Map<String, AttributeValue>) SchemaUtils.validateSameMember($SCHEMA_ITEM, member, value));
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
                    case 0 -> builder.item(SharedSerde.deserializeAttributeMap(member, de));
                    default -> throw new IllegalArgumentException("Unexpected member: " + member.memberName());
                }
            }
        }
    }
}
