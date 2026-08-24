package software.amazon.awssdk.benchmark.smithyjava.dynamodb.model;

import java.util.Collections;
import java.util.List;
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
public final class BatchExecuteStatementOutput implements SerializableStruct {

    public static final Schema $SCHEMA = Schemas.BATCH_EXECUTE_STATEMENT_OUTPUT;
    private static final Schema $SCHEMA_RESPONSES = $SCHEMA.member("Responses");
    private static final Schema $SCHEMA_CONSUMED_CAPACITY = $SCHEMA.member("ConsumedCapacity");

    public static final ShapeId $ID = $SCHEMA.id();

    private final transient List<BatchStatementResponse> responses;
    private final transient List<ConsumedCapacity> consumedCapacity;

    private BatchExecuteStatementOutput(Builder builder) {
        this.responses = builder.responses == null ? null : Collections.unmodifiableList(builder.responses);
        this.consumedCapacity = builder.consumedCapacity == null ? null : Collections.unmodifiableList(builder.consumedCapacity);
    }

    /**
     * The response to each PartiQL statement in the batch. The values of the list are ordered according to the ordering
     * of the request statements.
     */
    public List<BatchStatementResponse> getResponses() {
        if (responses == null) {
            return Collections.emptyList();
        }
        return responses;
    }

    public boolean hasResponses() {
        return responses != null;
    }

    /**
     * The capacity units consumed by the entire operation. The values of the list are ordered according to the ordering
     * of the statements.
     */
    public List<ConsumedCapacity> getConsumedCapacity() {
        if (consumedCapacity == null) {
            return Collections.emptyList();
        }
        return consumedCapacity;
    }

    public boolean hasConsumedCapacity() {
        return consumedCapacity != null;
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
        BatchExecuteStatementOutput that = (BatchExecuteStatementOutput) other;
        return Objects.equals(this.responses, that.responses)
               && Objects.equals(this.consumedCapacity, that.consumedCapacity);
    }

    @Override
    public int hashCode() {
        int $hc = Objects.hashCode(responses);
        $hc = 31 * $hc + Objects.hashCode(consumedCapacity);
        return $hc;
    }

    @Override
    public Schema schema() {
        return $SCHEMA;
    }

    @Override
    public void serializeMembers(ShapeSerializer serializer) {
        if (responses != null) {
            serializer.writeList($SCHEMA_RESPONSES, responses, responses.size(), SharedSerde.PartiQLBatchResponseSerializer.INSTANCE);
        }
        if (consumedCapacity != null) {
            serializer.writeList($SCHEMA_CONSUMED_CAPACITY, consumedCapacity, consumedCapacity.size(), SharedSerde.ConsumedCapacityMultipleSerializer.INSTANCE);
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T getMemberValue(Schema member) {
        return switch (member.memberIndex()) {
            case 0 -> (T) SchemaUtils.validateSameMember($SCHEMA_RESPONSES, member, responses);
            case 1 -> (T) SchemaUtils.validateSameMember($SCHEMA_CONSUMED_CAPACITY, member, consumedCapacity);
            default -> throw new IllegalArgumentException("Attempted to get non-existent member: " + member.id());
        };
    }

    /**
     * Create a new builder containing all the current property values of this object.
     *
     * <p><strong>Note:</strong> This method performs only a shallow copy of the original properties.
     *
     * @return a builder for {@link BatchExecuteStatementOutput}.
     */
    public Builder toBuilder() {
        var builder = new Builder();
        builder.responses(this.responses);
        builder.consumedCapacity(this.consumedCapacity);
        return builder;
    }

    /**
     * @return returns a new Builder.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for {@link BatchExecuteStatementOutput}.
     */
    public static final class Builder implements ShapeBuilder<BatchExecuteStatementOutput> {
        private List<BatchStatementResponse> responses;
        private List<ConsumedCapacity> consumedCapacity;

        private Builder() {}

        @Override
        public Schema schema() {
            return $SCHEMA;
        }

        /**
         * The response to each PartiQL statement in the batch. The values of the list are ordered according to the ordering
         * of the request statements.
         *
         * @return this builder.
         */
        public Builder responses(List<BatchStatementResponse> responses) {
            this.responses = responses;
            return this;
        }

        /**
         * The capacity units consumed by the entire operation. The values of the list are ordered according to the ordering
         * of the statements.
         *
         * @return this builder.
         */
        public Builder consumedCapacity(List<ConsumedCapacity> consumedCapacity) {
            this.consumedCapacity = consumedCapacity;
            return this;
        }

        @Override
        public BatchExecuteStatementOutput build() {
            return new BatchExecuteStatementOutput(this);
        }

        @Override
        @SuppressWarnings("unchecked")
        public void setMemberValue(Schema member, Object value) {
            switch (member.memberIndex()) {
                case 0 -> responses((List<BatchStatementResponse>) SchemaUtils.validateSameMember($SCHEMA_RESPONSES, member, value));
                case 1 -> consumedCapacity((List<ConsumedCapacity>) SchemaUtils.validateSameMember($SCHEMA_CONSUMED_CAPACITY, member, value));
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
                    case 0 -> builder.responses(SharedSerde.deserializePartiQLBatchResponse(member, de));
                    case 1 -> builder.consumedCapacity(SharedSerde.deserializeConsumedCapacityMultiple(member, de));
                    default -> throw new IllegalArgumentException("Unexpected member: " + member.memberName());
                }
            }
        }
    }
}
