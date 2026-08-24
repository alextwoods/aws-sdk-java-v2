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
 * An error associated with a statement in a PartiQL batch that was run.
 */
@SmithyGenerated
public final class BatchStatementError implements SerializableStruct {

    public static final Schema $SCHEMA = Schemas.BATCH_STATEMENT_ERROR;
    private static final Schema $SCHEMA_CODE = $SCHEMA.member("Code");
    private static final Schema $SCHEMA_MESSAGE = $SCHEMA.member("Message");
    private static final Schema $SCHEMA_ITEM = $SCHEMA.member("Item");

    public static final ShapeId $ID = $SCHEMA.id();

    private final transient BatchStatementErrorCodeEnum code;
    private final transient String message;
    private final transient Map<String, AttributeValue> item;

    private BatchStatementError(Builder builder) {
        this.code = builder.code;
        this.message = builder.message;
        this.item = builder.item == null ? null : Collections.unmodifiableMap(builder.item);
    }

    /**
     * The error code associated with the failed PartiQL batch statement.
     */
    public BatchStatementErrorCodeEnum getCode() {
        return code;
    }

    /**
     * The error message associated with the PartiQL batch response.
     */
    public String getMessage() {
        return message;
    }

    /**
     * The item which caused the condition check to fail. This will be set if ReturnValuesOnConditionCheckFailure is
     * specified as <code>ALL_OLD</code>.
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
        BatchStatementError that = (BatchStatementError) other;
        return Objects.equals(this.message, that.message)
               && Objects.equals(this.code, that.code)
               && Objects.equals(this.item, that.item);
    }

    @Override
    public int hashCode() {
        int $hc = Objects.hashCode(code);
        $hc = 31 * $hc + Objects.hashCode(message);
        $hc = 31 * $hc + Objects.hashCode(item);
        return $hc;
    }

    @Override
    public Schema schema() {
        return $SCHEMA;
    }

    @Override
    public void serializeMembers(ShapeSerializer serializer) {
        if (code != null) {
            serializer.writeString($SCHEMA_CODE, code.getValue());
        }
        if (message != null) {
            serializer.writeString($SCHEMA_MESSAGE, message);
        }
        if (item != null) {
            serializer.writeMap($SCHEMA_ITEM, item, item.size(), SharedSerde.AttributeMapSerializer.INSTANCE);
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T getMemberValue(Schema member) {
        return switch (member.memberIndex()) {
            case 0 -> (T) SchemaUtils.validateSameMember($SCHEMA_CODE, member, code);
            case 1 -> (T) SchemaUtils.validateSameMember($SCHEMA_MESSAGE, member, message);
            case 2 -> (T) SchemaUtils.validateSameMember($SCHEMA_ITEM, member, item);
            default -> throw new IllegalArgumentException("Attempted to get non-existent member: " + member.id());
        };
    }

    /**
     * Create a new builder containing all the current property values of this object.
     *
     * <p><strong>Note:</strong> This method performs only a shallow copy of the original properties.
     *
     * @return a builder for {@link BatchStatementError}.
     */
    public Builder toBuilder() {
        var builder = new Builder();
        builder.code(this.code);
        builder.message(this.message);
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
     * Builder for {@link BatchStatementError}.
     */
    public static final class Builder implements ShapeBuilder<BatchStatementError> {
        private BatchStatementErrorCodeEnum code;
        private String message;
        private Map<String, AttributeValue> item;

        private Builder() {}

        @Override
        public Schema schema() {
            return $SCHEMA;
        }

        /**
         * The error code associated with the failed PartiQL batch statement.
         *
         * @return this builder.
         */
        public Builder code(BatchStatementErrorCodeEnum code) {
            this.code = code;
            return this;
        }

        /**
         * The error message associated with the PartiQL batch response.
         *
         * @return this builder.
         */
        public Builder message(String message) {
            this.message = message;
            return this;
        }

        /**
         * The item which caused the condition check to fail. This will be set if ReturnValuesOnConditionCheckFailure is
         * specified as <code>ALL_OLD</code>.
         *
         * @return this builder.
         */
        public Builder item(Map<String, AttributeValue> item) {
            this.item = item;
            return this;
        }

        @Override
        public BatchStatementError build() {
            return new BatchStatementError(this);
        }

        @Override
        @SuppressWarnings("unchecked")
        public void setMemberValue(Schema member, Object value) {
            switch (member.memberIndex()) {
                case 0 -> code((BatchStatementErrorCodeEnum) SchemaUtils.validateSameMember($SCHEMA_CODE, member, value));
                case 1 -> message((String) SchemaUtils.validateSameMember($SCHEMA_MESSAGE, member, value));
                case 2 -> item((Map<String, AttributeValue>) SchemaUtils.validateSameMember($SCHEMA_ITEM, member, value));
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
                    case 0 -> builder.code(BatchStatementErrorCodeEnum.builder().deserializeMember(de, member).build());
                    case 1 -> builder.message(de.readString(member));
                    case 2 -> builder.item(SharedSerde.deserializeAttributeMap(member, de));
                    default -> throw new IllegalArgumentException("Unexpected member: " + member.memberName());
                }
            }
        }
    }
}
