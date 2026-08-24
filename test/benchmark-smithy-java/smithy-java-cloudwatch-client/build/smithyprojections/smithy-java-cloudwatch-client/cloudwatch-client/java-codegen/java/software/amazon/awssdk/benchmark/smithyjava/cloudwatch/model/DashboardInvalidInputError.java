package software.amazon.awssdk.benchmark.smithyjava.cloudwatch.model;

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
 * Some part of the dashboard data is invalid.
 */
@SmithyGenerated
public final class DashboardInvalidInputError extends CloudWatchException {

    public static final Schema $SCHEMA = Schemas.DASHBOARD_INVALID_INPUT_ERROR;
    private static final Schema $SCHEMA_MESSAGE = $SCHEMA.member("message");
    private static final Schema $SCHEMA_DASHBOARD_VALIDATION_MESSAGES = $SCHEMA.member("dashboardValidationMessages");

    public static final ShapeId $ID = $SCHEMA.id();

    private final transient List<DashboardValidationMessage> dashboardValidationMessages;

    private DashboardInvalidInputError(Builder builder) {
        super($SCHEMA, builder.message, builder.$cause, ErrorFault.CLIENT, builder.$captureStackTrace, builder.$deserialized);
        this.dashboardValidationMessages = builder.dashboardValidationMessages == null ? null : Collections.unmodifiableList(builder.dashboardValidationMessages);
    }

    public List<DashboardValidationMessage> getDashboardValidationMessages() {
        if (dashboardValidationMessages == null) {
            return Collections.emptyList();
        }
        return dashboardValidationMessages;
    }

    public boolean hasDashboardValidationMessages() {
        return dashboardValidationMessages != null;
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
        if (dashboardValidationMessages != null) {
            serializer.writeList($SCHEMA_DASHBOARD_VALIDATION_MESSAGES, dashboardValidationMessages, dashboardValidationMessages.size(), SharedSerde.DashboardValidationMessagesSerializer.INSTANCE);
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T getMemberValue(Schema member) {
        return switch (member.memberIndex()) {
            case 0 -> (T) SchemaUtils.validateSameMember($SCHEMA_MESSAGE, member, getMessage());
            case 1 -> (T) SchemaUtils.validateSameMember($SCHEMA_DASHBOARD_VALIDATION_MESSAGES, member, dashboardValidationMessages);
            default -> throw new IllegalArgumentException("Attempted to get non-existent member: " + member.id());
        };
    }

    /**
     * Create a new builder containing all the current property values of this object.
     *
     * <p><strong>Note:</strong> This method performs only a shallow copy of the original properties.
     *
     * @return a builder for {@link DashboardInvalidInputError}.
     */
    public Builder toBuilder() {
        var builder = new Builder();
        builder.message(getMessage());
        builder.dashboardValidationMessages(this.dashboardValidationMessages);
        return builder;
    }

    /**
     * @return returns a new Builder.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for {@link DashboardInvalidInputError}.
     */
    public static final class Builder implements ShapeBuilder<DashboardInvalidInputError> {
        private String message;
        private List<DashboardValidationMessage> dashboardValidationMessages;
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
         * @return this builder.
         */
        public Builder dashboardValidationMessages(List<DashboardValidationMessage> dashboardValidationMessages) {
            this.dashboardValidationMessages = dashboardValidationMessages;
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
        public DashboardInvalidInputError build() {
            return new DashboardInvalidInputError(this);
        }

        @Override
        @SuppressWarnings("unchecked")
        public void setMemberValue(Schema member, Object value) {
            switch (member.memberIndex()) {
                case 0 -> message((String) SchemaUtils.validateSameMember($SCHEMA_MESSAGE, member, value));
                case 1 -> dashboardValidationMessages((List<DashboardValidationMessage>) SchemaUtils.validateSameMember($SCHEMA_DASHBOARD_VALIDATION_MESSAGES, member, value));
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
                    case 1 -> builder.dashboardValidationMessages(SharedSerde.deserializeDashboardValidationMessages(member, de));
                    default -> throw new IllegalArgumentException("Unexpected member: " + member.memberName());
                }
            }
        }
    }
}
