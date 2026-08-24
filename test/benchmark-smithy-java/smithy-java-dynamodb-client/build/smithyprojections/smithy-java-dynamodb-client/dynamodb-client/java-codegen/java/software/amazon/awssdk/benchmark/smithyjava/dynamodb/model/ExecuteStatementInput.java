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

@SmithyGenerated
public final class ExecuteStatementInput implements SerializableStruct {

    public static final Schema $SCHEMA = Schemas.EXECUTE_STATEMENT_INPUT;
    private static final Schema $SCHEMA_STATEMENT = $SCHEMA.member("Statement");
    private static final Schema $SCHEMA_PARAMETERS = $SCHEMA.member("Parameters");
    private static final Schema $SCHEMA_CONSISTENT_READ = $SCHEMA.member("ConsistentRead");
    private static final Schema $SCHEMA_NEXT_TOKEN = $SCHEMA.member("NextToken");
    private static final Schema $SCHEMA_RETURN_CONSUMED_CAPACITY = $SCHEMA.member("ReturnConsumedCapacity");
    private static final Schema $SCHEMA_LIMIT = $SCHEMA.member("Limit");
    private static final Schema $SCHEMA_RETURN_VALUES_ON_CONDITION_CHECK_FAILURE = $SCHEMA.member("ReturnValuesOnConditionCheckFailure");

    public static final ShapeId $ID = $SCHEMA.id();

    private final transient String statement;
    private final transient List<AttributeValue> parameters;
    private final transient Boolean consistentRead;
    private final transient String nextToken;
    private final transient ReturnConsumedCapacity returnConsumedCapacity;
    private final transient Integer limit;
    private final transient ReturnValuesOnConditionCheckFailure returnValuesOnConditionCheckFailure;

    private ExecuteStatementInput(Builder builder) {
        this.statement = builder.statement;
        this.parameters = builder.parameters == null ? null : Collections.unmodifiableList(builder.parameters);
        this.consistentRead = builder.consistentRead;
        this.nextToken = builder.nextToken;
        this.returnConsumedCapacity = builder.returnConsumedCapacity;
        this.limit = builder.limit;
        this.returnValuesOnConditionCheckFailure = builder.returnValuesOnConditionCheckFailure;
    }

    /**
     * The PartiQL statement representing the operation to run.
     */
    public String getStatement() {
        return statement;
    }

    /**
     * The parameters for the PartiQL statement, if any.
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
     * The consistency of a read operation. If set to <code>true</code>, then a strongly consistent read is used;
     * otherwise, an eventually consistent read is used.
     */
    public Boolean isConsistentRead() {
        return consistentRead;
    }

    /**
     * Set this value to get remaining results, if <code>NextToken</code> was returned in the statement response.
     */
    public String getNextToken() {
        return nextToken;
    }

    public ReturnConsumedCapacity getReturnConsumedCapacity() {
        return returnConsumedCapacity;
    }

    /**
     * The maximum number of items to evaluate (not necessarily the number of matching items). If DynamoDB processes the
     * number of items up to the limit while processing the results, it stops the operation and returns the matching
     * values up to that point, along with a key in <code>LastEvaluatedKey</code> to apply in a subsequent operation so
     * you can pick up where you left off. Also, if the processed dataset size exceeds 1 MB before DynamoDB reaches this
     * limit, it stops the operation and returns the matching values up to the limit, and a key in
     * <code>LastEvaluatedKey</code> to apply in a subsequent operation to continue the operation.
     */
    public Integer getLimit() {
        return limit;
    }

    /**
     * An optional parameter that returns the item attributes for an <code>ExecuteStatement</code> operation that failed
     * a condition check.
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
        ExecuteStatementInput that = (ExecuteStatementInput) other;
        return Objects.equals(this.consistentRead, that.consistentRead)
               && Objects.equals(this.limit, that.limit)
               && Objects.equals(this.statement, that.statement)
               && Objects.equals(this.nextToken, that.nextToken)
               && Objects.equals(this.returnConsumedCapacity, that.returnConsumedCapacity)
               && Objects.equals(this.returnValuesOnConditionCheckFailure, that.returnValuesOnConditionCheckFailure)
               && Objects.equals(this.parameters, that.parameters);
    }

    @Override
    public int hashCode() {
        int $hc = Objects.hashCode(statement);
        $hc = 31 * $hc + Objects.hashCode(parameters);
        $hc = 31 * $hc + Objects.hashCode(consistentRead);
        $hc = 31 * $hc + Objects.hashCode(nextToken);
        $hc = 31 * $hc + Objects.hashCode(returnConsumedCapacity);
        $hc = 31 * $hc + Objects.hashCode(limit);
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
        if (nextToken != null) {
            serializer.writeString($SCHEMA_NEXT_TOKEN, nextToken);
        }
        if (returnConsumedCapacity != null) {
            serializer.writeString($SCHEMA_RETURN_CONSUMED_CAPACITY, returnConsumedCapacity.getValue());
        }
        if (limit != null) {
            serializer.writeInteger($SCHEMA_LIMIT, limit);
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
            case 3 -> (T) SchemaUtils.validateSameMember($SCHEMA_NEXT_TOKEN, member, nextToken);
            case 4 -> (T) SchemaUtils.validateSameMember($SCHEMA_RETURN_CONSUMED_CAPACITY, member, returnConsumedCapacity);
            case 5 -> (T) SchemaUtils.validateSameMember($SCHEMA_LIMIT, member, limit);
            case 6 -> (T) SchemaUtils.validateSameMember($SCHEMA_RETURN_VALUES_ON_CONDITION_CHECK_FAILURE, member, returnValuesOnConditionCheckFailure);
            default -> throw new IllegalArgumentException("Attempted to get non-existent member: " + member.id());
        };
    }

    /**
     * Create a new builder containing all the current property values of this object.
     *
     * <p><strong>Note:</strong> This method performs only a shallow copy of the original properties.
     *
     * @return a builder for {@link ExecuteStatementInput}.
     */
    public Builder toBuilder() {
        var builder = new Builder();
        builder.statement(this.statement);
        builder.parameters(this.parameters);
        builder.consistentRead(this.consistentRead);
        builder.nextToken(this.nextToken);
        builder.returnConsumedCapacity(this.returnConsumedCapacity);
        builder.limit(this.limit);
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
     * Builder for {@link ExecuteStatementInput}.
     */
    public static final class Builder implements ShapeBuilder<ExecuteStatementInput> {
        private final PresenceTracker tracker = PresenceTracker.of($SCHEMA);
        private String statement;
        private List<AttributeValue> parameters;
        private Boolean consistentRead;
        private String nextToken;
        private ReturnConsumedCapacity returnConsumedCapacity;
        private Integer limit;
        private ReturnValuesOnConditionCheckFailure returnValuesOnConditionCheckFailure;

        private Builder() {}

        @Override
        public Schema schema() {
            return $SCHEMA;
        }

        /**
         * The PartiQL statement representing the operation to run.
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
         * The parameters for the PartiQL statement, if any.
         *
         * @return this builder.
         */
        public Builder parameters(List<AttributeValue> parameters) {
            this.parameters = parameters;
            return this;
        }

        /**
         * The consistency of a read operation. If set to <code>true</code>, then a strongly consistent read is used;
         * otherwise, an eventually consistent read is used.
         *
         * @return this builder.
         */
        public Builder consistentRead(Boolean consistentRead) {
            this.consistentRead = consistentRead;
            return this;
        }

        /**
         * Set this value to get remaining results, if <code>NextToken</code> was returned in the statement response.
         *
         * @return this builder.
         */
        public Builder nextToken(String nextToken) {
            this.nextToken = nextToken;
            return this;
        }

        /**
         * @return this builder.
         */
        public Builder returnConsumedCapacity(ReturnConsumedCapacity returnConsumedCapacity) {
            this.returnConsumedCapacity = returnConsumedCapacity;
            return this;
        }

        /**
         * The maximum number of items to evaluate (not necessarily the number of matching items). If DynamoDB processes the
         * number of items up to the limit while processing the results, it stops the operation and returns the matching
         * values up to that point, along with a key in <code>LastEvaluatedKey</code> to apply in a subsequent operation so
         * you can pick up where you left off. Also, if the processed dataset size exceeds 1 MB before DynamoDB reaches this
         * limit, it stops the operation and returns the matching values up to the limit, and a key in
         * <code>LastEvaluatedKey</code> to apply in a subsequent operation to continue the operation.
         *
         * @return this builder.
         */
        public Builder limit(Integer limit) {
            this.limit = limit;
            return this;
        }

        /**
         * An optional parameter that returns the item attributes for an <code>ExecuteStatement</code> operation that failed
         * a condition check.
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
        public ExecuteStatementInput build() {
            tracker.validate();
            return new ExecuteStatementInput(this);
        }

        @Override
        @SuppressWarnings("unchecked")
        public void setMemberValue(Schema member, Object value) {
            switch (member.memberIndex()) {
                case 0 -> statement((String) SchemaUtils.validateSameMember($SCHEMA_STATEMENT, member, value));
                case 1 -> parameters((List<AttributeValue>) SchemaUtils.validateSameMember($SCHEMA_PARAMETERS, member, value));
                case 2 -> consistentRead((Boolean) SchemaUtils.validateSameMember($SCHEMA_CONSISTENT_READ, member, value));
                case 3 -> nextToken((String) SchemaUtils.validateSameMember($SCHEMA_NEXT_TOKEN, member, value));
                case 4 -> returnConsumedCapacity((ReturnConsumedCapacity) SchemaUtils.validateSameMember($SCHEMA_RETURN_CONSUMED_CAPACITY, member, value));
                case 5 -> limit((Integer) SchemaUtils.validateSameMember($SCHEMA_LIMIT, member, value));
                case 6 -> returnValuesOnConditionCheckFailure((ReturnValuesOnConditionCheckFailure) SchemaUtils.validateSameMember($SCHEMA_RETURN_VALUES_ON_CONDITION_CHECK_FAILURE, member, value));
                default -> ShapeBuilder.super.setMemberValue(member, value);
            }
        }

        @Override
        public ShapeBuilder<ExecuteStatementInput> errorCorrection() {
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
                    case 3 -> builder.nextToken(de.readString(member));
                    case 4 -> builder.returnConsumedCapacity(ReturnConsumedCapacity.builder().deserializeMember(de, member).build());
                    case 5 -> builder.limit(de.readInteger(member));
                    case 6 -> builder.returnValuesOnConditionCheckFailure(ReturnValuesOnConditionCheckFailure.builder().deserializeMember(de, member).build());
                    default -> throw new IllegalArgumentException("Unexpected member: " + member.memberName());
                }
            }
        }
    }
}
