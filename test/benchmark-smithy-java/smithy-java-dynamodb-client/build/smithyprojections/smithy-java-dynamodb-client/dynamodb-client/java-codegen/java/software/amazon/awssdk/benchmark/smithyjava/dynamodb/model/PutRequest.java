package software.amazon.awssdk.benchmark.smithyjava.dynamodb.model;

import java.util.Collections;
import java.util.Map;
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

/**
 * Represents a request to perform a <code>PutItem</code> operation on an item.
 */
@SmithyGenerated
public final class PutRequest implements SerializableStruct {

    public static final Schema $SCHEMA = Schemas.PUT_REQUEST;
    private static final Schema $SCHEMA_ITEM = $SCHEMA.member("Item");

    public static final ShapeId $ID = $SCHEMA.id();

    private final transient Map<String, AttributeValue> item;

    private PutRequest(Builder builder) {
        this.item = Collections.unmodifiableMap(builder.item);
    }

    /**
     * A map of attribute name to attribute values, representing the primary key of an item to be processed by
     * <code>PutItem</code>. All of the table's primary key attributes must be specified, and their data types must
     * match those of the table's key schema. If any attributes are present in the item that are part of an index key
     * schema for the table, their types must match the index key schema.
     */
    public Map<String, AttributeValue> getItem() {
        return item;
    }

    public boolean hasItem() {
        return true;
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
        PutRequest that = (PutRequest) other;
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
        serializer.writeMap($SCHEMA_ITEM, item, item.size(), SharedSerde.PutItemInputAttributeMapSerializer.INSTANCE);
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
     * @return a builder for {@link PutRequest}.
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
     * Builder for {@link PutRequest}.
     */
    public static final class Builder implements ShapeBuilder<PutRequest> {
        private final PresenceTracker tracker = PresenceTracker.of($SCHEMA);
        private Map<String, AttributeValue> item;

        private Builder() {}

        @Override
        public Schema schema() {
            return $SCHEMA;
        }

        /**
         * A map of attribute name to attribute values, representing the primary key of an item to be processed by
         * <code>PutItem</code>. All of the table's primary key attributes must be specified, and their data types must
         * match those of the table's key schema. If any attributes are present in the item that are part of an index key
         * schema for the table, their types must match the index key schema.
         *
         * <p><strong>Required</strong>
         * @return this builder.
         */
        public Builder item(Map<String, AttributeValue> item) {
            this.item = Objects.requireNonNull(item, "item cannot be null");
            tracker.setMember($SCHEMA_ITEM);
            return this;
        }

        @Override
        public PutRequest build() {
            tracker.validate();
            return new PutRequest(this);
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
        public ShapeBuilder<PutRequest> errorCorrection() {
            if (tracker.allSet()) {
                return this;
            }
            if (!tracker.checkMember($SCHEMA_ITEM)) {
                item(Collections.emptyMap());
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
                    case 0 -> builder.item(SharedSerde.deserializePutItemInputAttributeMap(member, de));
                    default -> throw new IllegalArgumentException("Unexpected member: " + member.memberName());
                }
            }
        }
    }
}
