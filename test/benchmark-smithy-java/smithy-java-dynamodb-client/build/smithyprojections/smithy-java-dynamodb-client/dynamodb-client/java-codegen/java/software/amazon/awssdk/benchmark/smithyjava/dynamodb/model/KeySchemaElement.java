package software.amazon.awssdk.benchmark.smithyjava.dynamodb.model;

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
 * Represents <i>a single element</i> of a key schema. A key schema specifies the attributes that make up the primary
 * key of a table, or the key attributes of an index.
 *
 * <p>A <code>KeySchemaElement</code> represents exactly one attribute of the primary key. For example, a simple primary
 * key would be represented by one <code>KeySchemaElement</code> (for the partition key). A composite primary key would
 * require one <code>KeySchemaElement</code> for the partition key, and another <code>KeySchemaElement</code> for the
 * sort key.
 *
 * <p>A <code>KeySchemaElement</code> must be a scalar, top-level attribute (not a nested attribute). The data type must
 * be one of String, Number, or Binary. The attribute cannot be nested within a List or a Map.
 */
@SmithyGenerated
public final class KeySchemaElement implements SerializableStruct {

    public static final Schema $SCHEMA = Schemas.KEY_SCHEMA_ELEMENT;
    private static final Schema $SCHEMA_ATTRIBUTE_NAME = $SCHEMA.member("AttributeName");
    private static final Schema $SCHEMA_KEY_TYPE = $SCHEMA.member("KeyType");

    public static final ShapeId $ID = $SCHEMA.id();

    private final transient String attributeName;
    private final transient KeyType keyType;

    private KeySchemaElement(Builder builder) {
        this.attributeName = builder.attributeName;
        this.keyType = builder.keyType;
    }

    /**
     * The name of a key attribute.
     */
    public String getAttributeName() {
        return attributeName;
    }

    /**
     * The role that this key attribute will assume:
     *
     * <ul>
     *   <li>
     *     <code>HASH</code> - partition key
     *   </li>
     *   <li>
     *     <code>RANGE</code> - sort key
     *   </li>
     * </ul>
     *
     * <p>The partition key of an item is also known as its <i>hash attribute</i>. The term "hash attribute" derives
     * from DynamoDB's usage of an internal hash function to evenly distribute data items across partitions, based on
     * their partition key values.
     *
     * <p>The sort key of an item is also known as its <i>range attribute</i>. The term "range attribute" derives from
     * the way DynamoDB stores items with the same partition key physically close together, in sorted order by the sort
     * key value.
     */
    public KeyType getKeyType() {
        return keyType;
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
        KeySchemaElement that = (KeySchemaElement) other;
        return Objects.equals(this.attributeName, that.attributeName)
               && Objects.equals(this.keyType, that.keyType);
    }

    @Override
    public int hashCode() {
        int $hc = Objects.hashCode(attributeName);
        $hc = 31 * $hc + Objects.hashCode(keyType);
        return $hc;
    }

    @Override
    public Schema schema() {
        return $SCHEMA;
    }

    @Override
    public void serializeMembers(ShapeSerializer serializer) {
        serializer.writeString($SCHEMA_ATTRIBUTE_NAME, attributeName);
        serializer.writeString($SCHEMA_KEY_TYPE, keyType.getValue());
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T getMemberValue(Schema member) {
        return switch (member.memberIndex()) {
            case 0 -> (T) SchemaUtils.validateSameMember($SCHEMA_ATTRIBUTE_NAME, member, attributeName);
            case 1 -> (T) SchemaUtils.validateSameMember($SCHEMA_KEY_TYPE, member, keyType);
            default -> throw new IllegalArgumentException("Attempted to get non-existent member: " + member.id());
        };
    }

    /**
     * Create a new builder containing all the current property values of this object.
     *
     * <p><strong>Note:</strong> This method performs only a shallow copy of the original properties.
     *
     * @return a builder for {@link KeySchemaElement}.
     */
    public Builder toBuilder() {
        var builder = new Builder();
        builder.attributeName(this.attributeName);
        builder.keyType(this.keyType);
        return builder;
    }

    /**
     * @return returns a new Builder.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for {@link KeySchemaElement}.
     */
    public static final class Builder implements ShapeBuilder<KeySchemaElement> {
        private final PresenceTracker tracker = PresenceTracker.of($SCHEMA);
        private String attributeName;
        private KeyType keyType;

        private Builder() {}

        @Override
        public Schema schema() {
            return $SCHEMA;
        }

        /**
         * The name of a key attribute.
         *
         * <p><strong>Required</strong>
         * @return this builder.
         */
        public Builder attributeName(String attributeName) {
            this.attributeName = Objects.requireNonNull(attributeName, "attributeName cannot be null");
            tracker.setMember($SCHEMA_ATTRIBUTE_NAME);
            return this;
        }

        /**
         * The role that this key attribute will assume:
         *
         * <ul>
         *   <li>
         *     <code>HASH</code> - partition key
         *   </li>
         *   <li>
         *     <code>RANGE</code> - sort key
         *   </li>
         * </ul>
         *
         * <p>The partition key of an item is also known as its <i>hash attribute</i>. The term "hash attribute" derives
         * from DynamoDB's usage of an internal hash function to evenly distribute data items across partitions, based on
         * their partition key values.
         *
         * <p>The sort key of an item is also known as its <i>range attribute</i>. The term "range attribute" derives from
         * the way DynamoDB stores items with the same partition key physically close together, in sorted order by the sort
         * key value.
         *
         * <p><strong>Required</strong>
         * @return this builder.
         */
        public Builder keyType(KeyType keyType) {
            this.keyType = Objects.requireNonNull(keyType, "keyType cannot be null");
            tracker.setMember($SCHEMA_KEY_TYPE);
            return this;
        }

        @Override
        public KeySchemaElement build() {
            tracker.validate();
            return new KeySchemaElement(this);
        }

        @Override
        @SuppressWarnings("unchecked")
        public void setMemberValue(Schema member, Object value) {
            switch (member.memberIndex()) {
                case 0 -> attributeName((String) SchemaUtils.validateSameMember($SCHEMA_ATTRIBUTE_NAME, member, value));
                case 1 -> keyType((KeyType) SchemaUtils.validateSameMember($SCHEMA_KEY_TYPE, member, value));
                default -> ShapeBuilder.super.setMemberValue(member, value);
            }
        }

        @Override
        public ShapeBuilder<KeySchemaElement> errorCorrection() {
            if (tracker.allSet()) {
                return this;
            }
            if (!tracker.checkMember($SCHEMA_ATTRIBUTE_NAME)) {
                attributeName("");
            }
            if (!tracker.checkMember($SCHEMA_KEY_TYPE)) {
                keyType(KeyType.unknown(""));
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
                    case 0 -> builder.attributeName(de.readString(member));
                    case 1 -> builder.keyType(KeyType.builder().deserializeMember(de, member).build());
                    default -> throw new IllegalArgumentException("Unexpected member: " + member.memberName());
                }
            }
        }
    }
}
