package software.amazon.awssdk.benchmark.smithyjava.dynamodb.model;

import java.util.Collections;
import java.util.List;
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
 * A PartiQL batch statement request.
 */
@SmithyGenerated
public final class BatchStatementRequest implements SerializableStruct {

    public static final Schema $SCHEMA = Schemas.BATCH_STATEMENT_REQUEST;
    private static final Schema $SCHEMA_STATEMENT = $SCHEMA.member("Statement");
    private static final Schema $SCHEMA_PARAMETERS = $SCHEMA.member("Parameters");
    private static final Schema $SCHEMA_CONSISTENT_READ = $SCHEMA.member("ConsistentRead");
    private static final Schema $SCHEMA_RETURN_VALUES_ON_CONDITION_CHECK_FAILURE = $SCHEMA.member("ReturnValuesOnConditionCheckFailure");

    public static final ShapeId $ID = $SCHEMA.id();

    private final transient String statement;
    private final transient List<AttributeValue> parameters;
    private final transient Boolean consistentRead;
    private final transient ReturnValuesOnConditionCheckFailure returnValuesOnConditionCheckFailure;

    private BatchStatementRequest(Builder builder) {
        this.statement = builder.statement;
        this.parameters = builder.parameters == null ? null : Collections.unmodifiableList(builder.parameters);
        this.consistentRead = builder.consistentRead;
        this.returnValuesOnConditionCheckFailure = builder.returnValuesOnConditionCheckFailure;
    }

    /**
     * A valid PartiQL statement.
     */
    public String getStatement() {
        return statement;
    }

    /**
     * The parameters associated with a PartiQL statement in the batch request.
     */
    public List<AttributeValue> getParameters() {
        if (parameters == null) {
            return Collections.emptyList();
        }
        return parameters;
    }

    public boolean hasParameters() {
        return parameters != null;
    }

    /**
     * The read consistency of the PartiQL batch request.
     */
    public Boolean isConsistentRead() {
        return consistentRead;
    }

    /**
     * An optional parameter that returns the item attributes for a PartiQL batch request operation that failed a
     * condition check.
     *
     * <p>There is no additional cost associated with requesting a return value aside from the small network and
     * processing overhead of receiving a larger response. No read capacity units are consumed.
     */
    public ReturnValuesOnConditionCheckFailure getReturnValuesOnConditionCheckFailure() {
        return returnValuesOnConditionCheckFailure;
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
        BatchStatementRequest that = (BatchStatementRequest) other;
        return Objects.equals(this.consistentRead, that.consistentRead)
               && Objects.equals(this.statement, that.statement)
               && Objects.equals(this.returnValuesOnConditionCheckFailure, that.returnValuesOnConditionCheckFailure)
               && Objects.equals(this.parameters, that.parameters);
    }

    @Override
    public int hashCode() {
        int $hc = Objects.hashCode(statement);
        $hc = 31 * $hc + Objects.hashCode(parameters);
        $hc = 31 * $hc + Objects.hashCode(consistentRead);
        $hc = 31 * $hc + Objects.hashCode(returnValuesOnConditionCheckFailure);
        return $hc;
    }

    @Override
    public Schema schema() {
        return $SCHEMA;
    }

    @Override
    public void serializeMembers(ShapeSerializer serializer) {
        serializer.writeString($SCHEMA_STATEMENT, statement);
        if (parameters != null) {
            serializer.writeList($SCHEMA_PARAMETERS, parameters, parameters.size(), SharedSerde.PreparedStatementParametersSerializer.INSTANCE);
        }
        if (consistentRead != null) {
            serializer.writeBoolean($SCHEMA_CONSISTENT_READ, consistentRead);
        }
        if (returnValuesOnConditionCheckFailure != null) {
            serializer.writeString($SCHEMA_RETURN_VALUES_ON_CONDITION_CHECK_FAILURE, returnValuesOnConditionCheckFailure.getValue());
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T getMemberValue(Schema member) {
        return switch (member.memberIndex()) {
            case 0 -> (T) SchemaUtils.validateSameMember($SCHEMA_STATEMENT, member, statement);
            case 1 -> (T) SchemaUtils.validateSameMember($SCHEMA_PARAMETERS, member, parameters);
            case 2 -> (T) SchemaUtils.validateSameMember($SCHEMA_CONSISTENT_READ, member, consistentRead);
            case 3 -> (T) SchemaUtils.validateSameMember($SCHEMA_RETURN_VALUES_ON_CONDITION_CHECK_FAILURE, member, returnValuesOnConditionCheckFailure);
            default -> throw new IllegalArgumentException("Attempted to get non-existent member: " + member.id());
        };
    }

    /**
     * Create a new builder containing all the current property values of this object.
     *
     * <p><strong>Note:</strong> This method performs only a shallow copy of the original properties.
     *
     * @return a builder for {@link BatchStatementRequest}.
     */
    public Builder toBuilder() {
        var builder = new Builder();
        builder.statement(this.statement);
        builder.parameters(this.parameters);
        builder.consistentRead(this.consistentRead);
        builder.returnValuesOnConditionCheckFailure(this.returnValuesOnConditionCheckFailure);
        return builder;
    }

    /**
     * @return returns a new Builder.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for {@link BatchStatementRequest}.
     */
    public static final class Builder implements ShapeBuilder<BatchStatementRequest> {
        private final PresenceTracker tracker = PresenceTracker.of($SCHEMA);
        private String statement;
        private List<AttributeValue> parameters;
        private Boolean consistentRead;
        private ReturnValuesOnConditionCheckFailure returnValuesOnConditionCheckFailure;

        private Builder() {}

        @Override
        public Schema schema() {
            return $SCHEMA;
        }

        /**
         * A valid PartiQL statement.
         *
         * <p><strong>Required</strong>
         * @return this builder.
         */
        public Builder statement(String statement) {
            this.statement = Objects.requireNonNull(statement, "statement cannot be null");
            tracker.setMember($SCHEMA_STATEMENT);
            return this;
        }

        /**
         * The parameters associated with a PartiQL statement in the batch request.
         *
         * @return this builder.
         */
        public Builder parameters(List<AttributeValue> parameters) {
            this.parameters = parameters;
            return this;
        }

        /**
         * The read consistency of the PartiQL batch request.
         *
         * @return this builder.
         */
        public Builder consistentRead(Boolean consistentRead) {
            this.consistentRead = consistentRead;
            return this;
        }

        /**
         * An optional parameter that returns the item attributes for a PartiQL batch request operation that failed a
         * condition check.
         *
         * <p>There is no additional cost associated with requesting a return value aside from the small network and
         * processing overhead of receiving a larger response. No read capacity units are consumed.
         *
         * @return this builder.
         */
        public Builder returnValuesOnConditionCheckFailure(ReturnValuesOnConditionCheckFailure returnValuesOnConditionCheckFailure) {
            this.returnValuesOnConditionCheckFailure = returnValuesOnConditionCheckFailure;
            return this;
        }

        @Override
        public BatchStatementRequest build() {
            tracker.validate();
            return new BatchStatementRequest(this);
        }

        @Override
        @SuppressWarnings("unchecked")
        public void setMemberValue(Schema member, Object value) {
            switch (member.memberIndex()) {
                case 0 -> statement((String) SchemaUtils.validateSameMember($SCHEMA_STATEMENT, member, value));
                case 1 -> parameters((List<AttributeValue>) SchemaUtils.validateSameMember($SCHEMA_PARAMETERS, member, value));
                case 2 -> consistentRead((Boolean) SchemaUtils.validateSameMember($SCHEMA_CONSISTENT_READ, member, value));
                case 3 -> returnValuesOnConditionCheckFailure((ReturnValuesOnConditionCheckFailure) SchemaUtils.validateSameMember($SCHEMA_RETURN_VALUES_ON_CONDITION_CHECK_FAILURE, member, value));
                default -> ShapeBuilder.super.setMemberValue(member, value);
            }
        }

        @Override
        public ShapeBuilder<BatchStatementRequest> errorCorrection() {
            if (tracker.allSet()) {
                return this;
            }
            if (!tracker.checkMember($SCHEMA_STATEMENT)) {
                statement("");
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
                    case 0 -> builder.statement(de.readString(member));
                    case 1 -> builder.parameters(SharedSerde.deserializePreparedStatementParameters(member, de));
                    case 2 -> builder.consistentRead(de.readBoolean(member));
                    case 3 -> builder.returnValuesOnConditionCheckFailure(ReturnValuesOnConditionCheckFailure.builder().deserializeMember(de, member).build());
                    default -> throw new IllegalArgumentException("Unexpected member: " + member.memberName());
                }
            }
        }
    }
}
