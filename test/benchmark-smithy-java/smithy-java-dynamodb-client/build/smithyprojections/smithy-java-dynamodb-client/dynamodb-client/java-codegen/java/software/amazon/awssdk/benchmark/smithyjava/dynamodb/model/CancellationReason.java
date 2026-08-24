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
 * An ordered list of errors for each item in the request which caused the transaction to get cancelled. The values of
 * the list are ordered according to the ordering of the <code>TransactWriteItems</code> request parameter. If no error
 * occurred for the associated item an error with a Null code and Null message will be present.
 */
@SmithyGenerated
public final class CancellationReason implements SerializableStruct {

    public static final Schema $SCHEMA = Schemas.CANCELLATION_REASON;
    private static final Schema $SCHEMA_ITEM = $SCHEMA.member("Item");
    private static final Schema $SCHEMA_CODE = $SCHEMA.member("Code");
    private static final Schema $SCHEMA_MESSAGE = $SCHEMA.member("Message");

    public static final ShapeId $ID = $SCHEMA.id();

    private final transient Map<String, AttributeValue> item;
    private final transient String code;
    private final transient String message;

    private CancellationReason(Builder builder) {
        this.item = builder.item == null ? null : Collections.unmodifiableMap(builder.item);
        this.code = builder.code;
        this.message = builder.message;
    }

    /**
     * Item in the request which caused the transaction to get cancelled.
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

    /**
     * Status code for the result of the cancelled transaction.
     */
    public String getCode() {
        return code;
    }

    /**
     * Cancellation reason message description.
     */
    public String getMessage() {
        return message;
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
        CancellationReason that = (CancellationReason) other;
        return Objects.equals(this.code, that.code)
               && Objects.equals(this.message, that.message)
               && Objects.equals(this.item, that.item);
    }

    @Override
    public int hashCode() {
        int $hc = Objects.hashCode(item);
        $hc = 31 * $hc + Objects.hashCode(code);
        $hc = 31 * $hc + Objects.hashCode(message);
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
        if (code != null) {
            serializer.writeString($SCHEMA_CODE, code);
        }
        if (message != null) {
            serializer.writeString($SCHEMA_MESSAGE, message);
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T getMemberValue(Schema member) {
        return switch (member.memberIndex()) {
            case 0 -> (T) SchemaUtils.validateSameMember($SCHEMA_ITEM, member, item);
            case 1 -> (T) SchemaUtils.validateSameMember($SCHEMA_CODE, member, code);
            case 2 -> (T) SchemaUtils.validateSameMember($SCHEMA_MESSAGE, member, message);
            default -> throw new IllegalArgumentException("Attempted to get non-existent member: " + member.id());
        };
    }

    /**
     * Create a new builder containing all the current property values of this object.
     *
     * <p><strong>Note:</strong> This method performs only a shallow copy of the original properties.
     *
     * @return a builder for {@link CancellationReason}.
     */
    public Builder toBuilder() {
        var builder = new Builder();
        builder.item(this.item);
        builder.code(this.code);
        builder.message(this.message);
        return builder;
    }

    /**
     * @return returns a new Builder.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for {@link CancellationReason}.
     */
    public static final class Builder implements ShapeBuilder<CancellationReason> {
        private Map<String, AttributeValue> item;
        private String code;
        private String message;

        private Builder() {}

        @Override
        public Schema schema() {
            return $SCHEMA;
        }

        /**
         * Item in the request which caused the transaction to get cancelled.
         *
         * @return this builder.
         */
        public Builder item(Map<String, AttributeValue> item) {
            this.item = item;
            return this;
        }

        /**
         * Status code for the result of the cancelled transaction.
         *
         * @return this builder.
         */
        public Builder code(String code) {
            this.code = code;
            return this;
        }

        /**
         * Cancellation reason message description.
         *
         * @return this builder.
         */
        public Builder message(String message) {
            this.message = message;
            return this;
        }

        @Override
        public CancellationReason build() {
            return new CancellationReason(this);
        }

        @Override
        @SuppressWarnings("unchecked")
        public void setMemberValue(Schema member, Object value) {
            switch (member.memberIndex()) {
                case 0 -> item((Map<String, AttributeValue>) SchemaUtils.validateSameMember($SCHEMA_ITEM, member, value));
                case 1 -> code((String) SchemaUtils.validateSameMember($SCHEMA_CODE, member, value));
                case 2 -> message((String) SchemaUtils.validateSameMember($SCHEMA_MESSAGE, member, value));
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
                    case 1 -> builder.code(de.readString(member));
                    case 2 -> builder.message(de.readString(member));
                    default -> throw new IllegalArgumentException("Unexpected member: " + member.memberName());
                }
            }
        }
    }
}
