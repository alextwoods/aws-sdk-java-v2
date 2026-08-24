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
 * A PartiQL batch statement response..
 */
@SmithyGenerated
public final class BatchStatementResponse implements SerializableStruct {

    public static final Schema $SCHEMA = Schemas.BATCH_STATEMENT_RESPONSE;
    private static final Schema $SCHEMA_ERROR = $SCHEMA.member("Error");
    private static final Schema $SCHEMA_TABLE_NAME = $SCHEMA.member("TableName");
    private static final Schema $SCHEMA_ITEM = $SCHEMA.member("Item");

    public static final ShapeId $ID = $SCHEMA.id();

    private final transient BatchStatementError error;
    private final transient String tableName;
    private final transient Map<String, AttributeValue> item;

    private BatchStatementResponse(Builder builder) {
        this.error = builder.error;
        this.tableName = builder.tableName;
        this.item = builder.item == null ? null : Collections.unmodifiableMap(builder.item);
    }

    /**
     * The error associated with a failed PartiQL batch statement.
     */
    public BatchStatementError getError() {
        return error;
    }

    /**
     * The table name associated with a failed PartiQL batch statement.
     */
    public String getTableName() {
        return tableName;
    }

    /**
     * A DynamoDB item associated with a BatchStatementResponse
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
        BatchStatementResponse that = (BatchStatementResponse) other;
        return Objects.equals(this.tableName, that.tableName)
               && Objects.equals(this.error, that.error)
               && Objects.equals(this.item, that.item);
    }

    @Override
    public int hashCode() {
        int $hc = Objects.hashCode(error);
        $hc = 31 * $hc + Objects.hashCode(tableName);
        $hc = 31 * $hc + Objects.hashCode(item);
        return $hc;
    }

    @Override
    public Schema schema() {
        return $SCHEMA;
    }

    @Override
    public void serializeMembers(ShapeSerializer serializer) {
        if (error != null) {
            serializer.writeStruct($SCHEMA_ERROR, error);
        }
        if (tableName != null) {
            serializer.writeString($SCHEMA_TABLE_NAME, tableName);
        }
        if (item != null) {
            serializer.writeMap($SCHEMA_ITEM, item, item.size(), SharedSerde.AttributeMapSerializer.INSTANCE);
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T getMemberValue(Schema member) {
        return switch (member.memberIndex()) {
            case 0 -> (T) SchemaUtils.validateSameMember($SCHEMA_ERROR, member, error);
            case 1 -> (T) SchemaUtils.validateSameMember($SCHEMA_TABLE_NAME, member, tableName);
            case 2 -> (T) SchemaUtils.validateSameMember($SCHEMA_ITEM, member, item);
            default -> throw new IllegalArgumentException("Attempted to get non-existent member: " + member.id());
        };
    }

    /**
     * Create a new builder containing all the current property values of this object.
     *
     * <p><strong>Note:</strong> This method performs only a shallow copy of the original properties.
     *
     * @return a builder for {@link BatchStatementResponse}.
     */
    public Builder toBuilder() {
        var builder = new Builder();
        builder.error(this.error);
        builder.tableName(this.tableName);
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
     * Builder for {@link BatchStatementResponse}.
     */
    public static final class Builder implements ShapeBuilder<BatchStatementResponse> {
        private BatchStatementError error;
        private String tableName;
        private Map<String, AttributeValue> item;

        private Builder() {}

        @Override
        public Schema schema() {
            return $SCHEMA;
        }

        /**
         * The error associated with a failed PartiQL batch statement.
         *
         * @return this builder.
         */
        public Builder error(BatchStatementError error) {
            this.error = error;
            return this;
        }

        /**
         * The table name associated with a failed PartiQL batch statement.
         *
         * @return this builder.
         */
        public Builder tableName(String tableName) {
            this.tableName = tableName;
            return this;
        }

        /**
         * A DynamoDB item associated with a BatchStatementResponse
         *
         * @return this builder.
         */
        public Builder item(Map<String, AttributeValue> item) {
            this.item = item;
            return this;
        }

        @Override
        public BatchStatementResponse build() {
            return new BatchStatementResponse(this);
        }

        @Override
        @SuppressWarnings("unchecked")
        public void setMemberValue(Schema member, Object value) {
            switch (member.memberIndex()) {
                case 0 -> error((BatchStatementError) SchemaUtils.validateSameMember($SCHEMA_ERROR, member, value));
                case 1 -> tableName((String) SchemaUtils.validateSameMember($SCHEMA_TABLE_NAME, member, value));
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
                    case 0 -> builder.error(BatchStatementError.builder().deserializeMember(de, member).build());
                    case 1 -> builder.tableName(de.readString(member));
                    case 2 -> builder.item(SharedSerde.deserializeAttributeMap(member, de));
                    default -> throw new IllegalArgumentException("Unexpected member: " + member.memberName());
                }
            }
        }
    }
}
