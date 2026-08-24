package software.amazon.awssdk.benchmark.smithyjava.dynamodb.model;

import java.util.Collections;
import java.util.List;
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
 * The entire transaction request was canceled.
 *
 * <p>DynamoDB cancels a <code>TransactWriteItems</code> request under the following circumstances:
 *
 * <ul>
 *   <li>
 *     A condition in one of the condition expressions is not met.
 *   </li>
 *   <li>
 *     A table in the <code>TransactWriteItems</code> request is in a different account or region.
 *   </li>
 *   <li>
 *     More than one action in the <code>TransactWriteItems</code> operation targets the same item.
 *   </li>
 *   <li>
 *     There is insufficient provisioned capacity for the transaction to be completed.
 *   </li>
 *   <li>
 *     An item size becomes too large (larger than 400 KB), or a local secondary index (LSI) becomes too large, or a
 *     similar validation error occurs because of changes made by the transaction.
 *   </li>
 *   <li>
 *     There is a user error, such as an invalid data format.
 *   </li>
 *   <li>
 *      There is an ongoing <code>TransactWriteItems</code> operation that conflicts with a concurrent <code>
 *     TransactWriteItems</code> request. In this case the <code>TransactWriteItems</code> operation fails with a <code>
 *     TransactionCanceledException</code>.
 *   </li>
 * </ul>
 *
 * <p>DynamoDB cancels a <code>TransactGetItems</code> request under the following circumstances:
 *
 * <ul>
 *   <li>
 *     There is an ongoing <code>TransactGetItems</code> operation that conflicts with a concurrent <code>PutItem</code>
 *     , <code>UpdateItem</code>, <code>DeleteItem</code> or <code>TransactWriteItems</code> request. In this case
 *     the <code>TransactGetItems</code> operation fails with a <code>TransactionCanceledException</code>.
 *   </li>
 *   <li>
 *     A table in the <code>TransactGetItems</code> request is in a different account or region.
 *   </li>
 *   <li>
 *     There is insufficient provisioned capacity for the transaction to be completed.
 *   </li>
 *   <li>
 *     There is a user error, such as an invalid data format.
 *   </li>
 * </ul>
 *
 * <p>DynamoDB lists the cancellation reasons on the <code>CancellationReasons</code> property. Transaction cancellation
 * reasons are ordered in the order of requested items, if an item has no error it will have <code>None</code> code and <code>
 * Null</code> message. The <code>None</code> code is returned as the literal string <code>"None"</code>, not a null or
 * absent value; the message field is omitted entirely for an item that has no error. This is important to note when
 * using an SDK that surfaces the code as an optional or nullable type.
 *
 * <p>Cancellation reason codes and possible error messages:
 *
 * <ul>
 *   <li>
 *     No Errors:
 *
 *     <ul>
 *       <li>
 *         Code: <code>None</code>
 *       </li>
 *       <li>
 *         Message: <code>null</code>
 *       </li>
 *     </ul>
 *   </li>
 *   <li>
 *     Conditional Check Failed:
 *
 *     <ul>
 *       <li>
 *         Code: <code>ConditionalCheckFailed</code>
 *       </li>
 *       <li>
 *         Message: The conditional request failed.
 *       </li>
 *     </ul>
 *   </li>
 *   <li>
 *     Item Collection Size Limit Exceeded:
 *
 *     <ul>
 *       <li>
 *         Code: <code>ItemCollectionSizeLimitExceeded</code>
 *       </li>
 *       <li>
 *         Message: Collection size exceeded.
 *       </li>
 *     </ul>
 *   </li>
 *   <li>
 *     Transaction Conflict:
 *
 *     <ul>
 *       <li>
 *         Code: <code>TransactionConflict</code>
 *       </li>
 *       <li>
 *         Message: Transaction is ongoing for the item.
 *       </li>
 *     </ul>
 *   </li>
 *   <li>
 *     Provisioned Throughput Exceeded:
 *
 *     <ul>
 *       <li>
 *         Code: <code>ProvisionedThroughputExceeded</code>
 *       </li>
 *       <li>
 *         Messages:
 *
 *         <ul>
 *           <li>
 *             The level of configured provisioned throughput for the table was exceeded. Consider
 *             increasing your provisioning level with the UpdateTable API.This Message is received when
 *             provisioned throughput is exceeded is on a provisioned DynamoDB table.
 *           </li>
 *           <li>
 *             The level of configured provisioned throughput for one or more global secondary indexes of
 *             the table was exceeded. Consider increasing your provisioning level for the under-provisioned
 *             global secondary indexes with the UpdateTable API.This message is returned when provisioned
 *             throughput is exceeded is on a provisioned GSI.
 *           </li>
 *         </ul>
 *       </li>
 *     </ul>
 *   </li>
 *   <li>
 *     Throttling Error:
 *
 *     <ul>
 *       <li>
 *         Code: <code>ThrottlingError</code>
 *       </li>
 *       <li>
 *         Messages:
 *
 *         <ul>
 *           <li>
 *             Throughput exceeds the current capacity of your table or index. DynamoDB is automatically
 *             scaling your table or index so please try again shortly. If exceptions persist, check if you
 *             have a hot key:
 *             https://docs.aws.amazon.com/amazondynamodb/latest/developerguide/bp-partition-key-design.html.
 *             This message is returned when writes get throttled on an On-Demand table as DynamoDB is
 *             automatically scaling the table.
 *           </li>
 *           <li>
 *             Throughput exceeds the current capacity for one or more global secondary indexes. DynamoDB is
 *             automatically scaling your index so please try again shortly.This message is returned when
 *             writes get throttled on an On-Demand GSI as DynamoDB is automatically scaling the GSI.
 *           </li>
 *         </ul>
 *       </li>
 *     </ul>
 *   </li>
 *   <li>
 *     Validation Error:
 *
 *     <ul>
 *       <li>
 *         Code: <code>ValidationError</code>
 *       </li>
 *       <li>
 *         Messages:
 *
 *         <ul>
 *           <li>
 *             One or more parameter values were invalid.
 *           </li>
 *           <li>
 *             The update expression attempted to update the secondary index key beyond allowed size limits.
 *           </li>
 *           <li>
 *             The update expression attempted to update the secondary index key to unsupported type.
 *           </li>
 *           <li>
 *             An operand in the update expression has an incorrect data type.
 *           </li>
 *           <li>
 *             Item size to update has exceeded the maximum allowed size.
 *           </li>
 *           <li>
 *             Number overflow. Attempting to store a number with magnitude larger than supported range.
 *           </li>
 *           <li>
 *             Type mismatch for attribute to update.
 *           </li>
 *           <li>
 *             Nesting Levels have exceeded supported limits.
 *           </li>
 *           <li>
 *             The document path provided in the update expression is invalid for update.
 *           </li>
 *           <li>
 *             The provided expression refers to an attribute that does not exist in the item.
 *           </li>
 *         </ul>
 *       </li>
 *     </ul>
 *   </li>
 * </ul>
 */
@SmithyGenerated
public final class TransactionCanceledException extends DynamoDBException {

    public static final Schema $SCHEMA = Schemas.TRANSACTION_CANCELED_EXCEPTION;
    private static final Schema $SCHEMA_MESSAGE = $SCHEMA.member("Message");
    private static final Schema $SCHEMA_CANCELLATION_REASONS = $SCHEMA.member("CancellationReasons");

    public static final ShapeId $ID = $SCHEMA.id();

    private final transient List<CancellationReason> cancellationReasons;

    private TransactionCanceledException(Builder builder) {
        super($SCHEMA, builder.message, builder.$cause, ErrorFault.CLIENT, builder.$captureStackTrace, builder.$deserialized);
        this.cancellationReasons = builder.cancellationReasons == null ? null : Collections.unmodifiableList(builder.cancellationReasons);
    }

    /**
     * A list of cancellation reasons.
     */
    public List<CancellationReason> getCancellationReasons() {
        if (cancellationReasons == null) {
            return Collections.emptyList();
        }
        return cancellationReasons;
    }

    public boolean hasCancellationReasons() {
        return cancellationReasons != null;
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
        if (cancellationReasons != null) {
            serializer.writeList($SCHEMA_CANCELLATION_REASONS, cancellationReasons, cancellationReasons.size(), SharedSerde.CancellationReasonListSerializer.INSTANCE);
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T getMemberValue(Schema member) {
        return switch (member.memberIndex()) {
            case 0 -> (T) SchemaUtils.validateSameMember($SCHEMA_MESSAGE, member, getMessage());
            case 1 -> (T) SchemaUtils.validateSameMember($SCHEMA_CANCELLATION_REASONS, member, cancellationReasons);
            default -> throw new IllegalArgumentException("Attempted to get non-existent member: " + member.id());
        };
    }

    /**
     * Create a new builder containing all the current property values of this object.
     *
     * <p><strong>Note:</strong> This method performs only a shallow copy of the original properties.
     *
     * @return a builder for {@link TransactionCanceledException}.
     */
    public Builder toBuilder() {
        var builder = new Builder();
        builder.message(getMessage());
        builder.cancellationReasons(this.cancellationReasons);
        return builder;
    }

    /**
     * @return returns a new Builder.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for {@link TransactionCanceledException}.
     */
    public static final class Builder implements ShapeBuilder<TransactionCanceledException> {
        private String message;
        private List<CancellationReason> cancellationReasons;
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

        /**
         * A list of cancellation reasons.
         *
         * @return this builder.
         */
        public Builder cancellationReasons(List<CancellationReason> cancellationReasons) {
            this.cancellationReasons = cancellationReasons;
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
        public TransactionCanceledException build() {
            return new TransactionCanceledException(this);
        }

        @Override
        @SuppressWarnings("unchecked")
        public void setMemberValue(Schema member, Object value) {
            switch (member.memberIndex()) {
                case 0 -> message((String) SchemaUtils.validateSameMember($SCHEMA_MESSAGE, member, value));
                case 1 -> cancellationReasons((List<CancellationReason>) SchemaUtils.validateSameMember($SCHEMA_CANCELLATION_REASONS, member, value));
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
                    case 1 -> builder.cancellationReasons(SharedSerde.deserializeCancellationReasonList(member, de));
                    default -> throw new IllegalArgumentException("Unexpected member: " + member.memberName());
                }
            }
        }
    }
}
