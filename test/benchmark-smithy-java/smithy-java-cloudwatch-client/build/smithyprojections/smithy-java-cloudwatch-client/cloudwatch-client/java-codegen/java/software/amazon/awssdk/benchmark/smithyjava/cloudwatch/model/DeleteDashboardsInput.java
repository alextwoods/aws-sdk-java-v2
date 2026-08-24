package software.amazon.awssdk.benchmark.smithyjava.cloudwatch.model;

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
public final class DeleteDashboardsInput implements SerializableStruct {

    public static final Schema $SCHEMA = Schemas.DELETE_DASHBOARDS_INPUT;
    private static final Schema $SCHEMA_DASHBOARD_NAMES = $SCHEMA.member("DashboardNames");

    public static final ShapeId $ID = $SCHEMA.id();

    private final transient List<String> dashboardNames;

    private DeleteDashboardsInput(Builder builder) {
        this.dashboardNames = builder.dashboardNames == null ? null : Collections.unmodifiableList(builder.dashboardNames);
    }

    /**
     * The dashboards to be deleted. This parameter is required.
     */
    public List<String> getDashboardNames() {
        if (dashboardNames == null) {
            return Collections.emptyList();
        }
        return dashboardNames;
    }

    public boolean hasDashboardNames() {
        return dashboardNames != null;
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
        DeleteDashboardsInput that = (DeleteDashboardsInput) other;
        return Objects.equals(this.dashboardNames, that.dashboardNames);
    }

    @Override
    public int hashCode() {
        int $hc = Objects.hashCode(dashboardNames);
        return $hc;
    }

    @Override
    public Schema schema() {
        return $SCHEMA;
    }

    @Override
    public void serializeMembers(ShapeSerializer serializer) {
        if (dashboardNames != null) {
            serializer.writeList($SCHEMA_DASHBOARD_NAMES, dashboardNames, dashboardNames.size(), SharedSerde.DashboardNamesSerializer.INSTANCE);
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T getMemberValue(Schema member) {
        return switch (member.memberIndex()) {
            case 0 -> (T) SchemaUtils.validateSameMember($SCHEMA_DASHBOARD_NAMES, member, dashboardNames);
            default -> throw new IllegalArgumentException("Attempted to get non-existent member: " + member.id());
        };
    }

    /**
     * Create a new builder containing all the current property values of this object.
     *
     * <p><strong>Note:</strong> This method performs only a shallow copy of the original properties.
     *
     * @return a builder for {@link DeleteDashboardsInput}.
     */
    public Builder toBuilder() {
        var builder = new Builder();
        builder.dashboardNames(this.dashboardNames);
        return builder;
    }

    /**
     * @return returns a new Builder.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for {@link DeleteDashboardsInput}.
     */
    public static final class Builder implements ShapeBuilder<DeleteDashboardsInput> {
        private final PresenceTracker tracker = PresenceTracker.of($SCHEMA);
        private List<String> dashboardNames;

        private Builder() {
            // Tell the tracker to assume clientOptional members are present.
            tracker.setMember($SCHEMA_DASHBOARD_NAMES);
        }

        @Override
        public Schema schema() {
            return $SCHEMA;
        }

        /**
         * The dashboards to be deleted. This parameter is required.
         *
         * <p><strong>Required</strong>
         * @return this builder.
         */
        public Builder dashboardNames(List<String> dashboardNames) {
            this.dashboardNames = Objects.requireNonNull(dashboardNames, "dashboardNames cannot be null");
            tracker.setMember($SCHEMA_DASHBOARD_NAMES);
            return this;
        }

        @Override
        public DeleteDashboardsInput build() {
            tracker.validate();
            return new DeleteDashboardsInput(this);
        }

        @Override
        @SuppressWarnings("unchecked")
        public void setMemberValue(Schema member, Object value) {
            switch (member.memberIndex()) {
                case 0 -> dashboardNames((List<String>) SchemaUtils.validateSameMember($SCHEMA_DASHBOARD_NAMES, member, value));
                default -> ShapeBuilder.super.setMemberValue(member, value);
            }
        }

        @Override
        public ShapeBuilder<DeleteDashboardsInput> errorCorrection() {
            if (tracker.allSet()) {
                return this;
            }
            if (!tracker.checkMember($SCHEMA_DASHBOARD_NAMES)) {
                dashboardNames(Collections.emptyList());
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
                    case 0 -> builder.dashboardNames(SharedSerde.deserializeDashboardNames(member, de));
                    default -> throw new IllegalArgumentException("Unexpected member: " + member.memberName());
                }
            }
        }
    }
}
