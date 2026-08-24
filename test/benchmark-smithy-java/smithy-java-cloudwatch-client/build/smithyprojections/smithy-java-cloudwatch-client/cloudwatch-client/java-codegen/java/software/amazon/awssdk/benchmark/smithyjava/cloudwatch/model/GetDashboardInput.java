package software.amazon.awssdk.benchmark.smithyjava.cloudwatch.model;

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
public final class GetDashboardInput implements SerializableStruct {

    public static final Schema $SCHEMA = Schemas.GET_DASHBOARD_INPUT;
    private static final Schema $SCHEMA_DASHBOARD_NAME = $SCHEMA.member("DashboardName");

    public static final ShapeId $ID = $SCHEMA.id();

    private final transient String dashboardName;

    private GetDashboardInput(Builder builder) {
        this.dashboardName = builder.dashboardName;
    }

    /**
     * The name of the dashboard to be described.
     */
    public String getDashboardName() {
        return dashboardName;
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
        GetDashboardInput that = (GetDashboardInput) other;
        return Objects.equals(this.dashboardName, that.dashboardName);
    }

    @Override
    public int hashCode() {
        int $hc = Objects.hashCode(dashboardName);
        return $hc;
    }

    @Override
    public Schema schema() {
        return $SCHEMA;
    }

    @Override
    public void serializeMembers(ShapeSerializer serializer) {
        if (dashboardName != null) {
            serializer.writeString($SCHEMA_DASHBOARD_NAME, dashboardName);
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T getMemberValue(Schema member) {
        return switch (member.memberIndex()) {
            case 0 -> (T) SchemaUtils.validateSameMember($SCHEMA_DASHBOARD_NAME, member, dashboardName);
            default -> throw new IllegalArgumentException("Attempted to get non-existent member: " + member.id());
        };
    }

    /**
     * Create a new builder containing all the current property values of this object.
     *
     * <p><strong>Note:</strong> This method performs only a shallow copy of the original properties.
     *
     * @return a builder for {@link GetDashboardInput}.
     */
    public Builder toBuilder() {
        var builder = new Builder();
        builder.dashboardName(this.dashboardName);
        return builder;
    }

    /**
     * @return returns a new Builder.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for {@link GetDashboardInput}.
     */
    public static final class Builder implements ShapeBuilder<GetDashboardInput> {
        private final PresenceTracker tracker = PresenceTracker.of($SCHEMA);
        private String dashboardName;

        private Builder() {
            // Tell the tracker to assume clientOptional members are present.
            tracker.setMember($SCHEMA_DASHBOARD_NAME);
        }

        @Override
        public Schema schema() {
            return $SCHEMA;
        }

        /**
         * The name of the dashboard to be described.
         *
         * <p><strong>Required</strong>
         * @return this builder.
         */
        public Builder dashboardName(String dashboardName) {
            this.dashboardName = Objects.requireNonNull(dashboardName, "dashboardName cannot be null");
            tracker.setMember($SCHEMA_DASHBOARD_NAME);
            return this;
        }

        @Override
        public GetDashboardInput build() {
            tracker.validate();
            return new GetDashboardInput(this);
        }

        @Override
        @SuppressWarnings("unchecked")
        public void setMemberValue(Schema member, Object value) {
            switch (member.memberIndex()) {
                case 0 -> dashboardName((String) SchemaUtils.validateSameMember($SCHEMA_DASHBOARD_NAME, member, value));
                default -> ShapeBuilder.super.setMemberValue(member, value);
            }
        }

        @Override
        public ShapeBuilder<GetDashboardInput> errorCorrection() {
            if (tracker.allSet()) {
                return this;
            }
            if (!tracker.checkMember($SCHEMA_DASHBOARD_NAME)) {
                dashboardName("");
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
                    case 0 -> builder.dashboardName(de.readString(member));
                    default -> throw new IllegalArgumentException("Unexpected member: " + member.memberName());
                }
            }
        }
    }
}
