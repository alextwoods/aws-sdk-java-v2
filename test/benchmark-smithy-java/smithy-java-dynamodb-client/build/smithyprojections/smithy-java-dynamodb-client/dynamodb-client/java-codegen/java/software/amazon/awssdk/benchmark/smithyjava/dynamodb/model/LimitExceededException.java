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
 * There is no limit to the number of daily on-demand backups that can be taken.
 *
 * <p>For most purposes, up to 500 simultaneous table operations are allowed per account. These operations include <code>
 * CreateTable</code>, <code>UpdateTable</code>, <code>DeleteTable</code>,<code>UpdateTimeToLive</code>, <code>
 * RestoreTableFromBackup</code>, and <code>RestoreTableToPointInTime</code>.
 *
 * <p>When you are creating a table with one or more secondary indexes, you can have up to 250 such requests running at
 * a time. However, if the table or index specifications are complex, then DynamoDB might temporarily reduce the number
 * of concurrent operations.
 *
 * <p>When importing into DynamoDB, up to 50 simultaneous import table operations are allowed per account.
 *
 * <p>There is a soft account quota of 2,500 tables.
 *
 * <p>GetRecords was called with a value of more than 1000 for the limit request parameter.
 *
 * <p>More than 2 processes are reading from the same streams shard at the same time. Exceeding this limit may result in
 * request throttling.
 */
@SmithyGenerated
public final class LimitExceededException extends DynamoDBException {

    public static final Schema $SCHEMA = Schemas.LIMIT_EXCEEDED_EXCEPTION;
    private static final Schema $SCHEMA_MESSAGE = $SCHEMA.member("message");

    public static final ShapeId $ID = $SCHEMA.id();

    private LimitExceededException(Builder builder) {
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
     * @return a builder for {@link LimitExceededException}.
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
     * Builder for {@link LimitExceededException}.
     */
    public static final class Builder implements ShapeBuilder<LimitExceededException> {
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
         * Too many operations for a given subscriber.
         *
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
        public LimitExceededException build() {
            return new LimitExceededException(this);
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
