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
 * Represents a request to perform a <code>DeleteItem</code> operation on an item.
 */
@SmithyGenerated
public final class DeleteRequest implements SerializableStruct {

    public static final Schema $SCHEMA = Schemas.DELETE_REQUEST;
    private static final Schema $SCHEMA_KEY = $SCHEMA.member("Key");

    public static final ShapeId $ID = $SCHEMA.id();

    private final transient Map<String, AttributeValue> key;

    private DeleteRequest(Builder builder) {
        this.key = Collections.unmodifiableMap(builder.key);
    }

    /**
     * A map of attribute name to attribute values, representing the primary key of the item to delete. All of the
     * table's primary key attributes must be specified, and their data types must match those of the table's key
     * schema.
     */
    public Map<String, AttributeValue> getKey() {
        return key;
    }

    public boolean hasKey() {
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
        DeleteRequest that = (DeleteRequest) other;
        return Objects.equals(this.key, that.key);
    }

    @Override
    public int hashCode() {
        int $hc = Objects.hashCode(key);
        return $hc;
    }

    @Override
    public Schema schema() {
        return $SCHEMA;
    }

    @Override
    public void serializeMembers(ShapeSerializer serializer) {
        serializer.writeMap($SCHEMA_KEY, key, key.size(), SharedSerde.KeySerializer.INSTANCE);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T getMemberValue(Schema member) {
        return switch (member.memberIndex()) {
            case 0 -> (T) SchemaUtils.validateSameMember($SCHEMA_KEY, member, key);
            default -> throw new IllegalArgumentException("Attempted to get non-existent member: " + member.id());
        };
    }

    /**
     * Create a new builder containing all the current property values of this object.
     *
     * <p><strong>Note:</strong> This method performs only a shallow copy of the original properties.
     *
     * @return a builder for {@link DeleteRequest}.
     */
    public Builder toBuilder() {
        var builder = new Builder();
        builder.key(this.key);
        return builder;
    }

    /**
     * @return returns a new Builder.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for {@link DeleteRequest}.
     */
    public static final class Builder implements ShapeBuilder<DeleteRequest> {
        private final PresenceTracker tracker = PresenceTracker.of($SCHEMA);
        private Map<String, AttributeValue> key;

        private Builder() {}

        @Override
        public Schema schema() {
            return $SCHEMA;
        }

        /**
         * A map of attribute name to attribute values, representing the primary key of the item to delete. All of the
         * table's primary key attributes must be specified, and their data types must match those of the table's key
         * schema.
         *
         * <p><strong>Required</strong>
         * @return this builder.
         */
        public Builder key(Map<String, AttributeValue> key) {
            this.key = Objects.requireNonNull(key, "key cannot be null");
            tracker.setMember($SCHEMA_KEY);
            return this;
        }

        @Override
        public DeleteRequest build() {
            tracker.validate();
            return new DeleteRequest(this);
        }

        @Override
        @SuppressWarnings("unchecked")
        public void setMemberValue(Schema member, Object value) {
            switch (member.memberIndex()) {
                case 0 -> key((Map<String, AttributeValue>) SchemaUtils.validateSameMember($SCHEMA_KEY, member, value));
                default -> ShapeBuilder.super.setMemberValue(member, value);
            }
        }

        @Override
        public ShapeBuilder<DeleteRequest> errorCorrection() {
            if (tracker.allSet()) {
                return this;
            }
            if (!tracker.checkMember($SCHEMA_KEY)) {
                key(Collections.emptyMap());
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
                    case 0 -> builder.key(SharedSerde.deserializeKey(member, de));
                    default -> throw new IllegalArgumentException("Unexpected member: " + member.memberName());
                }
            }
        }
    }
}
