package software.amazon.awssdk.benchmark.smithyjava.dynamodb.model;

import java.util.Collections;
import java.util.Map;
import software.amazon.smithy.java.core.error.ErrorFault;
import software.amazon.smithy.java.core.schema.Schema;
import software.amazon.smithy.java.core.schema.SchemaUtils;
import software.amazon.smithy.java.core.schema.ShapeBuilder;
import software.amazon.smithy.java.core.serde.ShapeDeserializer;
import software.amazon.smithy.java.core.serde.ShapeSerializer;
import software.amazon.smithy.java.core.serde.ToStringSerializer;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.utils.SmithyGenerated;

/**
 * A condition specified in the operation failed to be evaluated.
 */
@SmithyGenerated
public final class ConditionalCheckFailedException extends DynamoDBException {

    public static final Schema $SCHEMA = Schemas.CONDITIONAL_CHECK_FAILED_EXCEPTION;
    private static final Schema $SCHEMA_MESSAGE = $SCHEMA.member("message");
    private static final Schema $SCHEMA_ITEM = $SCHEMA.member("Item");

    public static final ShapeId $ID = $SCHEMA.id();

    private final transient Map<String, AttributeValue> item;

    private ConditionalCheckFailedException(Builder builder) {
        super($SCHEMA, builder.message, builder.$cause, ErrorFault.CLIENT, builder.$captureStackTrace, builder.$deserialized);
        this.item = builder.item == null ? null : Collections.unmodifiableMap(builder.item);
    }

    /**
     * Item which caused the <code>ConditionalCheckFailedException</code>.
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
    public void serializeMembers(ShapeSerializer serializer) {
        if (getMessage() != null) {
            serializer.writeString($SCHEMA_MESSAGE, getMessage());
        }
        if (item != null) {
            serializer.writeMap($SCHEMA_ITEM, item, item.size(), SharedSerde.AttributeMapSerializer.INSTANCE);
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T getMemberValue(Schema member) {
        return switch (member.memberIndex()) {
            case 0 -> (T) SchemaUtils.validateSameMember($SCHEMA_MESSAGE, member, getMessage());
            case 1 -> (T) SchemaUtils.validateSameMember($SCHEMA_ITEM, member, item);
            default -> throw new IllegalArgumentException("Attempted to get non-existent member: " + member.id());
        };
    }

    /**
     * Create a new builder containing all the current property values of this object.
     *
     * <p><strong>Note:</strong> This method performs only a shallow copy of the original properties.
     *
     * @return a builder for {@link ConditionalCheckFailedException}.
     */
    public Builder toBuilder() {
        var builder = new Builder();
        builder.message(getMessage());
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
     * Builder for {@link ConditionalCheckFailedException}.
     */
    public static final class Builder implements ShapeBuilder<ConditionalCheckFailedException> {
        private String message;
        private Map<String, AttributeValue> item;
        private Throwable $cause;
        private Boolean $captureStackTrace;
        private boolean $deserialized;

        private Builder() {}

        @Override
        public Schema schema() {
            return $SCHEMA;
        }

        /**
         * The conditional request failed.
         *
         * @return this builder.
         */
        public Builder message(String message) {
            this.message = message;
            return this;
        }

        /**
         * Item which caused the <code>ConditionalCheckFailedException</code>.
         *
         * @return this builder.
         */
        public Builder item(Map<String, AttributeValue> item) {
            this.item = item;
            return this;
        }

        public Builder withStackTrace() {
            this.$captureStackTrace = true;
            return this;
        }

        public Builder withoutStackTrace() {
            this.$captureStackTrace = false;
            return this;
        }

        public Builder withCause(Throwable cause) {
            this.$cause = cause;
            return this;
        }

        @Override
        public ConditionalCheckFailedException build() {
            return new ConditionalCheckFailedException(this);
        }

        @Override
        @SuppressWarnings("unchecked")
        public void setMemberValue(Schema member, Object value) {
            switch (member.memberIndex()) {
                case 0 -> message((String) SchemaUtils.validateSameMember($SCHEMA_MESSAGE, member, value));
                case 1 -> item((Map<String, AttributeValue>) SchemaUtils.validateSameMember($SCHEMA_ITEM, member, value));
                default -> ShapeBuilder.super.setMemberValue(member, value);
            }
        }

        @Override
        public Builder deserialize(ShapeDeserializer decoder) {
            this.$deserialized = true;
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
                    case 0 -> builder.message(de.readString(member));
                    case 1 -> builder.item(SharedSerde.deserializeAttributeMap(member, de));
                    default -> throw new IllegalArgumentException("Unexpected member: " + member.memberName());
                }
            }
        }
    }
}
