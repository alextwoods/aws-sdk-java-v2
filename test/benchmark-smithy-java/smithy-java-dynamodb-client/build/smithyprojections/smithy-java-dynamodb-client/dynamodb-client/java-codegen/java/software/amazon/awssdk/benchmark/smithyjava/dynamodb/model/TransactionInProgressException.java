package software.amazon.awssdk.benchmark.smithyjava.dynamodb.model;

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
 * The transaction with the given request token is already in progress.
 *
 * <p> Recommended Settings
 *
 * <p> This is a general recommendation for handling the <code>TransactionInProgressException</code>. These settings
 * help ensure that the client retries will trigger completion of the ongoing <code>TransactWriteItems</code> request.
 *
 * <ul>
 *   <li>
 *      Set <code>clientExecutionTimeout</code> to a value that allows at least one retry to be processed after 5
 *     seconds have elapsed since the first attempt for the <code>TransactWriteItems</code> operation.
 *   </li>
 *   <li>
 *      Set <code>socketTimeout</code> to a value a little lower than the <code>requestTimeout</code> setting.
 *   </li>
 *   <li>
 *     <code>requestTimeout</code> should be set based on the time taken for the individual retries of a single HTTP
 *     request for your use case, but setting it to 1 second or higher should work well to reduce chances of retries
 *     and <code>TransactionInProgressException</code> errors.
 *   </li>
 *   <li>
 *      Use exponential backoff when retrying and tune backoff if needed.
 *   </li>
 * </ul>
 *
 * <p> Assuming <a href="https://github.com/aws/aws-sdk-java/blob/fd409dee8ae23fb8953e0bb4dbde65536a7e0514/aws-java-sdk-core/src/main/java/com/amazonaws/retry/PredefinedRetryPolicies.java#L97">default retry policy</a>, example timeout settings based on the guidelines above are as follows:
 *
 * <p>Example timeline:
 *
 * <ul>
 *   <li>
 *     0-1000 first attempt
 *   </li>
 *   <li>
 *     1000-1500 first sleep/delay (default retry policy uses 500 ms as base delay for 4xx errors)
 *   </li>
 *   <li>
 *     1500-2500 second attempt
 *   </li>
 *   <li>
 *     2500-3500 second sleep/delay (500 &#42; 2, exponential backoff)
 *   </li>
 *   <li>
 *     3500-4500 third attempt
 *   </li>
 *   <li>
 *     4500-6500 third sleep/delay (500 &#42; 2^2)
 *   </li>
 *   <li>
 *     6500-7500 fourth attempt (this can trigger inline recovery since 5 seconds have elapsed since the first
 *     attempt reached TC)
 *   </li>
 * </ul>
 */
@SmithyGenerated
public final class TransactionInProgressException extends DynamoDBException {

    public static final Schema $SCHEMA = Schemas.TRANSACTION_IN_PROGRESS_EXCEPTION;
    private static final Schema $SCHEMA_MESSAGE = $SCHEMA.member("Message");

    public static final ShapeId $ID = $SCHEMA.id();

    private TransactionInProgressException(Builder builder) {
        super($SCHEMA, builder.message, builder.$cause, ErrorFault.CLIENT, builder.$captureStackTrace, builder.$deserialized);
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
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T getMemberValue(Schema member) {
        return switch (member.memberIndex()) {
            case 0 -> (T) SchemaUtils.validateSameMember($SCHEMA_MESSAGE, member, getMessage());
            default -> throw new IllegalArgumentException("Attempted to get non-existent member: " + member.id());
        };
    }

    /**
     * Create a new builder containing all the current property values of this object.
     *
     * <p><strong>Note:</strong> This method performs only a shallow copy of the original properties.
     *
     * @return a builder for {@link TransactionInProgressException}.
     */
    public Builder toBuilder() {
        var builder = new Builder();
        builder.message(getMessage());
        return builder;
    }

    /**
     * @return returns a new Builder.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for {@link TransactionInProgressException}.
     */
    public static final class Builder implements ShapeBuilder<TransactionInProgressException> {
        private String message;
        private Throwable $cause;
        private Boolean $captureStackTrace;
        private boolean $deserialized;

        private Builder() {}

        @Override
        public Schema schema() {
            return $SCHEMA;
        }

        /**
         * @return this builder.
         */
        public Builder message(String message) {
            this.message = message;
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
        public TransactionInProgressException build() {
            return new TransactionInProgressException(this);
        }

        @Override
        @SuppressWarnings("unchecked")
        public void setMemberValue(Schema member, Object value) {
            switch (member.memberIndex()) {
                case 0 -> message((String) SchemaUtils.validateSameMember($SCHEMA_MESSAGE, member, value));
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
                    default -> throw new IllegalArgumentException("Unexpected member: " + member.memberName());
                }
            }
        }
    }
}
