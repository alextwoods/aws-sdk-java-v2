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
public final class ExecuteTransactionInput implements SerializableStruct {

    public static final Schema $SCHEMA = Schemas.EXECUTE_TRANSACTION_INPUT;
    private static final Schema $SCHEMA_TRANSACT_STATEMENTS = $SCHEMA.member("TransactStatements");
    private static final Schema $SCHEMA_CLIENT_REQUEST_TOKEN = $SCHEMA.member("ClientRequestToken");
    private static final Schema $SCHEMA_RETURN_CONSUMED_CAPACITY = $SCHEMA.member("ReturnConsumedCapacity");

    public static final ShapeId $ID = $SCHEMA.id();

    private final transient List<ParameterizedStatement> transactStatements;
    private final transient String clientRequestToken;
    private final transient ReturnConsumedCapacity returnConsumedCapacity;

    private ExecuteTransactionInput(Builder builder) {
        this.transactStatements = Collections.unmodifiableList(builder.transactStatements);
        this.clientRequestToken = builder.clientRequestToken;
        this.returnConsumedCapacity = builder.returnConsumedCapacity;
    }

    /**
     * The list of PartiQL statements representing the transaction to run.
     */
    public List<ParameterizedStatement> getTransactStatements() {
        return transactStatements;
    }

    public boolean hasTransactStatements() {
        return true;
    }

    /**
     * Set this value to get remaining results, if <code>NextToken</code> was returned in the statement response.
     */
    public String getClientRequestToken() {
        return clientRequestToken;
    }

    /**
     * Determines the level of detail about either provisioned or on-demand throughput consumption that is returned in
     * the response. For more information, see <a
     * href="https://docs.aws.amazon.com/amazondynamodb/latest/APIReference/API_TransactGetItems.html">TransactGetItems</a>
     * and <a
     * href="https://docs.aws.amazon.com/amazondynamodb/latest/APIReference/API_TransactWriteItems.html">TransactWriteItems</a>.
     */
    public ReturnConsumedCapacity getReturnConsumedCapacity() {
        return returnConsumedCapacity;
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
        ExecuteTransactionInput that = (ExecuteTransactionInput) other;
        return Objects.equals(this.clientRequestToken, that.clientRequestToken)
               && Objects.equals(this.returnConsumedCapacity, that.returnConsumedCapacity)
               && Objects.equals(this.transactStatements, that.transactStatements);
    }

    @Override
    public int hashCode() {
        int $hc = Objects.hashCode(transactStatements);
        $hc = 31 * $hc + Objects.hashCode(clientRequestToken);
        $hc = 31 * $hc + Objects.hashCode(returnConsumedCapacity);
        return $hc;
    }

    @Override
    public Schema schema() {
        return $SCHEMA;
    }

    @Override
    public void serializeMembers(ShapeSerializer serializer) {
        serializer.writeList($SCHEMA_TRANSACT_STATEMENTS, transactStatements, transactStatements.size(), SharedSerde.ParameterizedStatementsSerializer.INSTANCE);
        if (clientRequestToken != null) {
            serializer.writeString($SCHEMA_CLIENT_REQUEST_TOKEN, clientRequestToken);
        }
        if (returnConsumedCapacity != null) {
            serializer.writeString($SCHEMA_RETURN_CONSUMED_CAPACITY, returnConsumedCapacity.getValue());
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T getMemberValue(Schema member) {
        return switch (member.memberIndex()) {
            case 0 -> (T) SchemaUtils.validateSameMember($SCHEMA_TRANSACT_STATEMENTS, member, transactStatements);
            case 1 -> (T) SchemaUtils.validateSameMember($SCHEMA_CLIENT_REQUEST_TOKEN, member, clientRequestToken);
            case 2 -> (T) SchemaUtils.validateSameMember($SCHEMA_RETURN_CONSUMED_CAPACITY, member, returnConsumedCapacity);
            default -> throw new IllegalArgumentException("Attempted to get non-existent member: " + member.id());
        };
    }

    /**
     * Create a new builder containing all the current property values of this object.
     *
     * <p><strong>Note:</strong> This method performs only a shallow copy of the original properties.
     *
     * @return a builder for {@link ExecuteTransactionInput}.
     */
    public Builder toBuilder() {
        var builder = new Builder();
        builder.transactStatements(this.transactStatements);
        builder.clientRequestToken(this.clientRequestToken);
        builder.returnConsumedCapacity(this.returnConsumedCapacity);
        return builder;
    }

    /**
     * @return returns a new Builder.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for {@link ExecuteTransactionInput}.
     */
    public static final class Builder implements ShapeBuilder<ExecuteTransactionInput> {
        private final PresenceTracker tracker = PresenceTracker.of($SCHEMA);
        private List<ParameterizedStatement> transactStatements;
        private String clientRequestToken;
        private ReturnConsumedCapacity returnConsumedCapacity;

        private Builder() {}

        @Override
        public Schema schema() {
            return $SCHEMA;
        }

        /**
         * The list of PartiQL statements representing the transaction to run.
         *
         * <p><strong>Required</strong>
         * @return this builder.
         */
        public Builder transactStatements(List<ParameterizedStatement> transactStatements) {
            this.transactStatements = Objects.requireNonNull(transactStatements, "transactStatements cannot be null");
            tracker.setMember($SCHEMA_TRANSACT_STATEMENTS);
            return this;
        }

        /**
         * Set this value to get remaining results, if <code>NextToken</code> was returned in the statement response.
         *
         * @return this builder.
         */
        public Builder clientRequestToken(String clientRequestToken) {
            this.clientRequestToken = clientRequestToken;
            return this;
        }

        /**
         * Determines the level of detail about either provisioned or on-demand throughput consumption that is returned in
         * the response. For more information, see <a
         * href="https://docs.aws.amazon.com/amazondynamodb/latest/APIReference/API_TransactGetItems.html">TransactGetItems</a>
         * and <a
         * href="https://docs.aws.amazon.com/amazondynamodb/latest/APIReference/API_TransactWriteItems.html">TransactWriteItems</a>.
         *
         * @return this builder.
         */
        public Builder returnConsumedCapacity(ReturnConsumedCapacity returnConsumedCapacity) {
            this.returnConsumedCapacity = returnConsumedCapacity;
            return this;
        }

        @Override
        public ExecuteTransactionInput build() {
            tracker.validate();
            return new ExecuteTransactionInput(this);
        }

        @Override
        @SuppressWarnings("unchecked")
        public void setMemberValue(Schema member, Object value) {
            switch (member.memberIndex()) {
                case 0 -> transactStatements((List<ParameterizedStatement>) SchemaUtils.validateSameMember($SCHEMA_TRANSACT_STATEMENTS, member, value));
                case 1 -> clientRequestToken((String) SchemaUtils.validateSameMember($SCHEMA_CLIENT_REQUEST_TOKEN, member, value));
                case 2 -> returnConsumedCapacity((ReturnConsumedCapacity) SchemaUtils.validateSameMember($SCHEMA_RETURN_CONSUMED_CAPACITY, member, value));
                default -> ShapeBuilder.super.setMemberValue(member, value);
            }
        }

        @Override
        public ShapeBuilder<ExecuteTransactionInput> errorCorrection() {
            if (tracker.allSet()) {
                return this;
            }
            if (!tracker.checkMember($SCHEMA_TRANSACT_STATEMENTS)) {
                transactStatements(Collections.emptyList());
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
                    case 0 -> builder.transactStatements(SharedSerde.deserializeParameterizedStatements(member, de));
                    case 1 -> builder.clientRequestToken(de.readString(member));
                    case 2 -> builder.returnConsumedCapacity(ReturnConsumedCapacity.builder().deserializeMember(de, member).build());
                    default -> throw new IllegalArgumentException("Unexpected member: " + member.memberName());
                }
            }
        }
    }
}
