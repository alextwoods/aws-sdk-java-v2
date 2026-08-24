package software.amazon.awssdk.benchmark.smithyjava.dynamodb.model;

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
 * A list of requests that can perform update, put, delete, or check operations on multiple items in one or more tables
 * atomically.
 */
@SmithyGenerated
public final class TransactWriteItem implements SerializableStruct {

    public static final Schema $SCHEMA = Schemas.TRANSACT_WRITE_ITEM;
    private static final Schema $SCHEMA_CONDITION_CHECK = $SCHEMA.member("ConditionCheck");
    private static final Schema $SCHEMA_PUT = $SCHEMA.member("Put");
    private static final Schema $SCHEMA_DELETE = $SCHEMA.member("Delete");
    private static final Schema $SCHEMA_UPDATE = $SCHEMA.member("Update");

    public static final ShapeId $ID = $SCHEMA.id();

    private final transient ConditionCheck conditionCheck;
    private final transient Put put;
    private final transient Delete delete;
    private final transient Update update;

    private TransactWriteItem(Builder builder) {
        this.conditionCheck = builder.conditionCheck;
        this.put = builder.put;
        this.delete = builder.delete;
        this.update = builder.update;
    }

    /**
     * A request to perform a check item operation.
     */
    public ConditionCheck getConditionCheck() {
        return conditionCheck;
    }

    /**
     * A request to perform a <code>PutItem</code> operation.
     */
    public Put getPut() {
        return put;
    }

    /**
     * A request to perform a <code>DeleteItem</code> operation.
     */
    public Delete getDelete() {
        return delete;
    }

    /**
     * A request to perform an <code>UpdateItem</code> operation.
     */
    public Update getUpdate() {
        return update;
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
        TransactWriteItem that = (TransactWriteItem) other;
        return Objects.equals(this.conditionCheck, that.conditionCheck)
               && Objects.equals(this.put, that.put)
               && Objects.equals(this.delete, that.delete)
               && Objects.equals(this.update, that.update);
    }

    @Override
    public int hashCode() {
        int $hc = Objects.hashCode(conditionCheck);
        $hc = 31 * $hc + Objects.hashCode(put);
        $hc = 31 * $hc + Objects.hashCode(delete);
        $hc = 31 * $hc + Objects.hashCode(update);
        return $hc;
    }

    @Override
    public Schema schema() {
        return $SCHEMA;
    }

    @Override
    public void serializeMembers(ShapeSerializer serializer) {
        if (conditionCheck != null) {
            serializer.writeStruct($SCHEMA_CONDITION_CHECK, conditionCheck);
        }
        if (put != null) {
            serializer.writeStruct($SCHEMA_PUT, put);
        }
        if (delete != null) {
            serializer.writeStruct($SCHEMA_DELETE, delete);
        }
        if (update != null) {
            serializer.writeStruct($SCHEMA_UPDATE, update);
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T getMemberValue(Schema member) {
        return switch (member.memberIndex()) {
            case 0 -> (T) SchemaUtils.validateSameMember($SCHEMA_CONDITION_CHECK, member, conditionCheck);
            case 1 -> (T) SchemaUtils.validateSameMember($SCHEMA_PUT, member, put);
            case 2 -> (T) SchemaUtils.validateSameMember($SCHEMA_DELETE, member, delete);
            case 3 -> (T) SchemaUtils.validateSameMember($SCHEMA_UPDATE, member, update);
            default -> throw new IllegalArgumentException("Attempted to get non-existent member: " + member.id());
        };
    }

    /**
     * Create a new builder containing all the current property values of this object.
     *
     * <p><strong>Note:</strong> This method performs only a shallow copy of the original properties.
     *
     * @return a builder for {@link TransactWriteItem}.
     */
    public Builder toBuilder() {
        var builder = new Builder();
        builder.conditionCheck(this.conditionCheck);
        builder.put(this.put);
        builder.delete(this.delete);
        builder.update(this.update);
        return builder;
    }

    /**
     * @return returns a new Builder.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for {@link TransactWriteItem}.
     */
    public static final class Builder implements ShapeBuilder<TransactWriteItem> {
        private ConditionCheck conditionCheck;
        private Put put;
        private Delete delete;
        private Update update;

        private Builder() {}

        @Override
        public Schema schema() {
            return $SCHEMA;
        }

        /**
         * A request to perform a check item operation.
         *
         * @return this builder.
         */
        public Builder conditionCheck(ConditionCheck conditionCheck) {
            this.conditionCheck = conditionCheck;
            return this;
        }

        /**
         * A request to perform a <code>PutItem</code> operation.
         *
         * @return this builder.
         */
        public Builder put(Put put) {
            this.put = put;
            return this;
        }

        /**
         * A request to perform a <code>DeleteItem</code> operation.
         *
         * @return this builder.
         */
        public Builder delete(Delete delete) {
            this.delete = delete;
            return this;
        }

        /**
         * A request to perform an <code>UpdateItem</code> operation.
         *
         * @return this builder.
         */
        public Builder update(Update update) {
            this.update = update;
            return this;
        }

        @Override
        public TransactWriteItem build() {
            return new TransactWriteItem(this);
        }

        @Override
        @SuppressWarnings("unchecked")
        public void setMemberValue(Schema member, Object value) {
            switch (member.memberIndex()) {
                case 0 -> conditionCheck((ConditionCheck) SchemaUtils.validateSameMember($SCHEMA_CONDITION_CHECK, member, value));
                case 1 -> put((Put) SchemaUtils.validateSameMember($SCHEMA_PUT, member, value));
                case 2 -> delete((Delete) SchemaUtils.validateSameMember($SCHEMA_DELETE, member, value));
                case 3 -> update((Update) SchemaUtils.validateSameMember($SCHEMA_UPDATE, member, value));
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
                    case 0 -> builder.conditionCheck(ConditionCheck.builder().deserializeMember(de, member).build());
                    case 1 -> builder.put(Put.builder().deserializeMember(de, member).build());
                    case 2 -> builder.delete(Delete.builder().deserializeMember(de, member).build());
                    case 3 -> builder.update(Update.builder().deserializeMember(de, member).build());
                    default -> throw new IllegalArgumentException("Unexpected member: " + member.memberName());
                }
            }
        }
    }
}
