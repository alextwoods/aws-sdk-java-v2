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
 * Throughput exceeds the current throughput quota for your account. For detailed information about why the request was
 * throttled and the ARN of the impacted resource, find the <a
 * href="https://docs.aws.amazon.com/amazondynamodb/latest/APIReference/API_ThrottlingReason.html">ThrottlingReason</a>
 * field in the returned exception. Contact <a href="https://aws.amazon.com/support">Amazon Web Services Support</a> to
 * request a quota increase.
 */
@SmithyGenerated
public final class RequestLimitExceeded extends DynamoDBException {

    public static final Schema $SCHEMA = Schemas.REQUEST_LIMIT_EXCEEDED;
    private static final Schema $SCHEMA_MESSAGE = $SCHEMA.member("message");
    private static final Schema $SCHEMA_THROTTLING_REASONS = $SCHEMA.member("ThrottlingReasons");

    public static final ShapeId $ID = $SCHEMA.id();

    private final transient List<ThrottlingReason> throttlingReasons;

    private RequestLimitExceeded(Builder builder) {
        super($SCHEMA, builder.message, builder.$cause, ErrorFault.CLIENT, builder.$captureStackTrace, builder.$deserialized);
        this.throttlingReasons = builder.throttlingReasons == null ? null : Collections.unmodifiableList(builder.throttlingReasons);
    }

    /**
     * A list of <a
     * href="https://docs.aws.amazon.com/amazondynamodb/latest/APIReference/API_ThrottlingReason.html">ThrottlingReason</a>
     * that provide detailed diagnostic information about why the request was throttled.
     */
    public List<ThrottlingReason> getThrottlingReasons() {
        if (throttlingReasons == null) {
            return Collections.emptyList();
        }
        return throttlingReasons;
    }

    public boolean hasThrottlingReasons() {
        return throttlingReasons != null;
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
        if (throttlingReasons != null) {
            serializer.writeList($SCHEMA_THROTTLING_REASONS, throttlingReasons, throttlingReasons.size(), SharedSerde.ThrottlingReasonListSerializer.INSTANCE);
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T getMemberValue(Schema member) {
        return switch (member.memberIndex()) {
            case 0 -> (T) SchemaUtils.validateSameMember($SCHEMA_MESSAGE, member, getMessage());
            case 1 -> (T) SchemaUtils.validateSameMember($SCHEMA_THROTTLING_REASONS, member, throttlingReasons);
            default -> throw new IllegalArgumentException("Attempted to get non-existent member: " + member.id());
        };
    }

    /**
     * Create a new builder containing all the current property values of this object.
     *
     * <p><strong>Note:</strong> This method performs only a shallow copy of the original properties.
     *
     * @return a builder for {@link RequestLimitExceeded}.
     */
    public Builder toBuilder() {
        var builder = new Builder();
        builder.message(getMessage());
        builder.throttlingReasons(this.throttlingReasons);
        return builder;
    }

    /**
     * @return returns a new Builder.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for {@link RequestLimitExceeded}.
     */
    public static final class Builder implements ShapeBuilder<RequestLimitExceeded> {
        private String message;
        private List<ThrottlingReason> throttlingReasons;
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
         * A list of <a
         * href="https://docs.aws.amazon.com/amazondynamodb/latest/APIReference/API_ThrottlingReason.html">ThrottlingReason</a>
         * that provide detailed diagnostic information about why the request was throttled.
         *
         * @return this builder.
         */
        public Builder throttlingReasons(List<ThrottlingReason> throttlingReasons) {
            this.throttlingReasons = throttlingReasons;
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
        public RequestLimitExceeded build() {
            return new RequestLimitExceeded(this);
        }

        @Override
        @SuppressWarnings("unchecked")
        public void setMemberValue(Schema member, Object value) {
            switch (member.memberIndex()) {
                case 0 -> message((String) SchemaUtils.validateSameMember($SCHEMA_MESSAGE, member, value));
                case 1 -> throttlingReasons((List<ThrottlingReason>) SchemaUtils.validateSameMember($SCHEMA_THROTTLING_REASONS, member, value));
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
                    case 1 -> builder.throttlingReasons(SharedSerde.deserializeThrottlingReasonList(member, de));
                    default -> throw new IllegalArgumentException("Unexpected member: " + member.memberName());
                }
            }
        }
    }
}
