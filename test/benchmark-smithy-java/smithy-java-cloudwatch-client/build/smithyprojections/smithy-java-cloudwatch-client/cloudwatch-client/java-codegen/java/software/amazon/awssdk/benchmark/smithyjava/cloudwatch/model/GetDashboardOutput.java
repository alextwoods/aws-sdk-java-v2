package software.amazon.awssdk.benchmark.smithyjava.cloudwatch.model;

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
public final class GetDashboardOutput implements SerializableStruct {

    public static final Schema $SCHEMA = Schemas.GET_DASHBOARD_OUTPUT;
    private static final Schema $SCHEMA_DASHBOARD_ARN = $SCHEMA.member("DashboardArn");
    private static final Schema $SCHEMA_DASHBOARD_BODY = $SCHEMA.member("DashboardBody");
    private static final Schema $SCHEMA_DASHBOARD_NAME = $SCHEMA.member("DashboardName");

    public static final ShapeId $ID = $SCHEMA.id();

    private final transient String dashboardArn;
    private final transient String dashboardBody;
    private final transient String dashboardName;

    private GetDashboardOutput(Builder builder) {
        this.dashboardArn = builder.dashboardArn;
        this.dashboardBody = builder.dashboardBody;
        this.dashboardName = builder.dashboardName;
    }

    /**
     * The Amazon Resource Name (ARN) of the dashboard.
     */
    public String getDashboardArn() {
        return dashboardArn;
    }

    /**
     * The detailed information about the dashboard, including what widgets are included and their location on the
     * dashboard. For more information about the <code>DashboardBody</code> syntax, see <a
     * href="https://docs.aws.amazon.com/AmazonCloudWatch/latest/monitoring/CloudWatch-Dashboard-Body-Structure.html">Dashboard
     * Body Structure and Syntax</a>.
     */
    public String getDashboardBody() {
        return dashboardBody;
    }

    /**
     * The name of the dashboard.
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
        GetDashboardOutput that = (GetDashboardOutput) other;
        return Objects.equals(this.dashboardArn, that.dashboardArn)
               && Objects.equals(this.dashboardBody, that.dashboardBody)
               && Objects.equals(this.dashboardName, that.dashboardName);
    }

    @Override
    public int hashCode() {
        int $hc = Objects.hashCode(dashboardArn);
        $hc = 31 * $hc + Objects.hashCode(dashboardBody);
        $hc = 31 * $hc + Objects.hashCode(dashboardName);
        return $hc;
    }

    @Override
    public Schema schema() {
        return $SCHEMA;
    }

    @Override
    public void serializeMembers(ShapeSerializer serializer) {
        if (dashboardArn != null) {
            serializer.writeString($SCHEMA_DASHBOARD_ARN, dashboardArn);
        }
        if (dashboardBody != null) {
            serializer.writeString($SCHEMA_DASHBOARD_BODY, dashboardBody);
        }
        if (dashboardName != null) {
            serializer.writeString($SCHEMA_DASHBOARD_NAME, dashboardName);
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T getMemberValue(Schema member) {
        return switch (member.memberIndex()) {
            case 0 -> (T) SchemaUtils.validateSameMember($SCHEMA_DASHBOARD_ARN, member, dashboardArn);
            case 1 -> (T) SchemaUtils.validateSameMember($SCHEMA_DASHBOARD_BODY, member, dashboardBody);
            case 2 -> (T) SchemaUtils.validateSameMember($SCHEMA_DASHBOARD_NAME, member, dashboardName);
            default -> throw new IllegalArgumentException("Attempted to get non-existent member: " + member.id());
        };
    }

    /**
     * Create a new builder containing all the current property values of this object.
     *
     * <p><strong>Note:</strong> This method performs only a shallow copy of the original properties.
     *
     * @return a builder for {@link GetDashboardOutput}.
     */
    public Builder toBuilder() {
        var builder = new Builder();
        builder.dashboardArn(this.dashboardArn);
        builder.dashboardBody(this.dashboardBody);
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
     * Builder for {@link GetDashboardOutput}.
     */
    public static final class Builder implements ShapeBuilder<GetDashboardOutput> {
        private String dashboardArn;
        private String dashboardBody;
        private String dashboardName;

        private Builder() {}

        @Override
        public Schema schema() {
            return $SCHEMA;
        }

        /**
         * The Amazon Resource Name (ARN) of the dashboard.
         *
         * @return this builder.
         */
        public Builder dashboardArn(String dashboardArn) {
            this.dashboardArn = dashboardArn;
            return this;
        }

        /**
         * The detailed information about the dashboard, including what widgets are included and their location on the
         * dashboard. For more information about the <code>DashboardBody</code> syntax, see <a
         * href="https://docs.aws.amazon.com/AmazonCloudWatch/latest/monitoring/CloudWatch-Dashboard-Body-Structure.html">Dashboard
         * Body Structure and Syntax</a>.
         *
         * @return this builder.
         */
        public Builder dashboardBody(String dashboardBody) {
            this.dashboardBody = dashboardBody;
            return this;
        }

        /**
         * The name of the dashboard.
         *
         * @return this builder.
         */
        public Builder dashboardName(String dashboardName) {
            this.dashboardName = dashboardName;
            return this;
        }

        @Override
        public GetDashboardOutput build() {
            return new GetDashboardOutput(this);
        }

        @Override
        @SuppressWarnings("unchecked")
        public void setMemberValue(Schema member, Object value) {
            switch (member.memberIndex()) {
                case 0 -> dashboardArn((String) SchemaUtils.validateSameMember($SCHEMA_DASHBOARD_ARN, member, value));
                case 1 -> dashboardBody((String) SchemaUtils.validateSameMember($SCHEMA_DASHBOARD_BODY, member, value));
                case 2 -> dashboardName((String) SchemaUtils.validateSameMember($SCHEMA_DASHBOARD_NAME, member, value));
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
                    case 0 -> builder.dashboardArn(de.readString(member));
                    case 1 -> builder.dashboardBody(de.readString(member));
                    case 2 -> builder.dashboardName(de.readString(member));
                    default -> throw new IllegalArgumentException("Unexpected member: " + member.memberName());
                }
            }
        }
    }
}
