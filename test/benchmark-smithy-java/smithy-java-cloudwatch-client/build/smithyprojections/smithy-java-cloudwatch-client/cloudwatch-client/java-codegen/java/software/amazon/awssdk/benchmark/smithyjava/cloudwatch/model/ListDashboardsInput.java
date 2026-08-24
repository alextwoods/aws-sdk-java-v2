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
public final class ListDashboardsInput implements SerializableStruct {

    public static final Schema $SCHEMA = Schemas.LIST_DASHBOARDS_INPUT;
    private static final Schema $SCHEMA_DASHBOARD_NAME_PREFIX = $SCHEMA.member("DashboardNamePrefix");
    private static final Schema $SCHEMA_NEXT_TOKEN = $SCHEMA.member("NextToken");

    public static final ShapeId $ID = $SCHEMA.id();

    private final transient String dashboardNamePrefix;
    private final transient String nextToken;

    private ListDashboardsInput(Builder builder) {
        this.dashboardNamePrefix = builder.dashboardNamePrefix;
        this.nextToken = builder.nextToken;
    }

    /**
     * If you specify this parameter, only the dashboards with names starting with the specified string are listed. The
     * maximum length is 255, and valid characters are A-Z, a-z, 0-9, ".", "-", and "_".
     */
    public String getDashboardNamePrefix() {
        return dashboardNamePrefix;
    }

    /**
     * The token returned by a previous call to indicate that there is more data available.
     */
    public String getNextToken() {
        return nextToken;
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
        ListDashboardsInput that = (ListDashboardsInput) other;
        return Objects.equals(this.dashboardNamePrefix, that.dashboardNamePrefix)
               && Objects.equals(this.nextToken, that.nextToken);
    }

    @Override
    public int hashCode() {
        int $hc = Objects.hashCode(dashboardNamePrefix);
        $hc = 31 * $hc + Objects.hashCode(nextToken);
        return $hc;
    }

    @Override
    public Schema schema() {
        return $SCHEMA;
    }

    @Override
    public void serializeMembers(ShapeSerializer serializer) {
        if (dashboardNamePrefix != null) {
            serializer.writeString($SCHEMA_DASHBOARD_NAME_PREFIX, dashboardNamePrefix);
        }
        if (nextToken != null) {
            serializer.writeString($SCHEMA_NEXT_TOKEN, nextToken);
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T getMemberValue(Schema member) {
        return switch (member.memberIndex()) {
            case 0 -> (T) SchemaUtils.validateSameMember($SCHEMA_DASHBOARD_NAME_PREFIX, member, dashboardNamePrefix);
            case 1 -> (T) SchemaUtils.validateSameMember($SCHEMA_NEXT_TOKEN, member, nextToken);
            default -> throw new IllegalArgumentException("Attempted to get non-existent member: " + member.id());
        };
    }

    /**
     * Create a new builder containing all the current property values of this object.
     *
     * <p><strong>Note:</strong> This method performs only a shallow copy of the original properties.
     *
     * @return a builder for {@link ListDashboardsInput}.
     */
    public Builder toBuilder() {
        var builder = new Builder();
        builder.dashboardNamePrefix(this.dashboardNamePrefix);
        builder.nextToken(this.nextToken);
        return builder;
    }

    /**
     * @return returns a new Builder.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for {@link ListDashboardsInput}.
     */
    public static final class Builder implements ShapeBuilder<ListDashboardsInput> {
        private String dashboardNamePrefix;
        private String nextToken;

        private Builder() {}

        @Override
        public Schema schema() {
            return $SCHEMA;
        }

        /**
         * If you specify this parameter, only the dashboards with names starting with the specified string are listed. The
         * maximum length is 255, and valid characters are A-Z, a-z, 0-9, ".", "-", and "_".
         *
         * @return this builder.
         */
        public Builder dashboardNamePrefix(String dashboardNamePrefix) {
            this.dashboardNamePrefix = dashboardNamePrefix;
            return this;
        }

        /**
         * The token returned by a previous call to indicate that there is more data available.
         *
         * @return this builder.
         */
        public Builder nextToken(String nextToken) {
            this.nextToken = nextToken;
            return this;
        }

        @Override
        public ListDashboardsInput build() {
            return new ListDashboardsInput(this);
        }

        @Override
        @SuppressWarnings("unchecked")
        public void setMemberValue(Schema member, Object value) {
            switch (member.memberIndex()) {
                case 0 -> dashboardNamePrefix((String) SchemaUtils.validateSameMember($SCHEMA_DASHBOARD_NAME_PREFIX, member, value));
                case 1 -> nextToken((String) SchemaUtils.validateSameMember($SCHEMA_NEXT_TOKEN, member, value));
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
                    case 0 -> builder.dashboardNamePrefix(de.readString(member));
                    case 1 -> builder.nextToken(de.readString(member));
                    default -> throw new IllegalArgumentException("Unexpected member: " + member.memberName());
                }
            }
        }
    }
}
