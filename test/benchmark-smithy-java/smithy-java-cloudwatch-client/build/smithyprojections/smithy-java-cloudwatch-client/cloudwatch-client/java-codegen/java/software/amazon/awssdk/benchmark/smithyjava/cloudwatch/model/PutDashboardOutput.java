package software.amazon.awssdk.benchmark.smithyjava.cloudwatch.model;

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
public final class PutDashboardOutput implements SerializableStruct {

    public static final Schema $SCHEMA = Schemas.PUT_DASHBOARD_OUTPUT;
    private static final Schema $SCHEMA_DASHBOARD_VALIDATION_MESSAGES = $SCHEMA.member("DashboardValidationMessages");

    public static final ShapeId $ID = $SCHEMA.id();

    private final transient List<DashboardValidationMessage> dashboardValidationMessages;

    private PutDashboardOutput(Builder builder) {
        this.dashboardValidationMessages = builder.dashboardValidationMessages == null ? null : Collections.unmodifiableList(builder.dashboardValidationMessages);
    }

    /**
     * If the input for <code>PutDashboard</code> was correct and the dashboard was successfully created or modified,
     * this result is empty.
     *
     * <p>If this result includes only warning messages, then the input was valid enough for the dashboard to be created
     * or modified, but some elements of the dashboard might not render.
     *
     * <p>If this result includes error messages, the input was not valid and the operation failed.
     */
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
    public boolean equals(Object other) {
        if (other == this) {
            return true;
        }
        if (other == null || getClass() != other.getClass()) {
            return false;
        }
        PutDashboardOutput that = (PutDashboardOutput) other;
        return Objects.equals(this.dashboardValidationMessages, that.dashboardValidationMessages);
    }

    @Override
    public int hashCode() {
        int $hc = Objects.hashCode(dashboardValidationMessages);
        return $hc;
    }

    @Override
    public Schema schema() {
        return $SCHEMA;
    }

    @Override
    public void serializeMembers(ShapeSerializer serializer) {
        if (dashboardValidationMessages != null) {
            serializer.writeList($SCHEMA_DASHBOARD_VALIDATION_MESSAGES, dashboardValidationMessages, dashboardValidationMessages.size(), SharedSerde.DashboardValidationMessagesSerializer.INSTANCE);
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T getMemberValue(Schema member) {
        return switch (member.memberIndex()) {
            case 0 -> (T) SchemaUtils.validateSameMember($SCHEMA_DASHBOARD_VALIDATION_MESSAGES, member, dashboardValidationMessages);
            default -> throw new IllegalArgumentException("Attempted to get non-existent member: " + member.id());
        };
    }

    /**
     * Create a new builder containing all the current property values of this object.
     *
     * <p><strong>Note:</strong> This method performs only a shallow copy of the original properties.
     *
     * @return a builder for {@link PutDashboardOutput}.
     */
    public Builder toBuilder() {
        var builder = new Builder();
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
     * Builder for {@link PutDashboardOutput}.
     */
    public static final class Builder implements ShapeBuilder<PutDashboardOutput> {
        private List<DashboardValidationMessage> dashboardValidationMessages;

        private Builder() {}

        @Override
        public Schema schema() {
            return $SCHEMA;
        }

        /**
         * If the input for <code>PutDashboard</code> was correct and the dashboard was successfully created or modified,
         * this result is empty.
         *
         * <p>If this result includes only warning messages, then the input was valid enough for the dashboard to be created
         * or modified, but some elements of the dashboard might not render.
         *
         * <p>If this result includes error messages, the input was not valid and the operation failed.
         *
         * @return this builder.
         */
        public Builder dashboardValidationMessages(List<DashboardValidationMessage> dashboardValidationMessages) {
            this.dashboardValidationMessages = dashboardValidationMessages;
            return this;
        }

        @Override
        public PutDashboardOutput build() {
            return new PutDashboardOutput(this);
        }

        @Override
        @SuppressWarnings("unchecked")
        public void setMemberValue(Schema member, Object value) {
            switch (member.memberIndex()) {
                case 0 -> dashboardValidationMessages((List<DashboardValidationMessage>) SchemaUtils.validateSameMember($SCHEMA_DASHBOARD_VALIDATION_MESSAGES, member, value));
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
                    case 0 -> builder.dashboardValidationMessages(SharedSerde.deserializeDashboardValidationMessages(member, de));
                    default -> throw new IllegalArgumentException("Unexpected member: " + member.memberName());
                }
            }
        }
    }
}
