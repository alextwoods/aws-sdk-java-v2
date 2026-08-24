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
public final class PutDashboardInput implements SerializableStruct {

    public static final Schema $SCHEMA = Schemas.PUT_DASHBOARD_INPUT;
    private static final Schema $SCHEMA_DASHBOARD_NAME = $SCHEMA.member("DashboardName");
    private static final Schema $SCHEMA_DASHBOARD_BODY = $SCHEMA.member("DashboardBody");
    private static final Schema $SCHEMA_TAGS = $SCHEMA.member("Tags");

    public static final ShapeId $ID = $SCHEMA.id();

    private final transient String dashboardName;
    private final transient String dashboardBody;
    private final transient List<Tag> tags;

    private PutDashboardInput(Builder builder) {
        this.dashboardName = builder.dashboardName;
        this.dashboardBody = builder.dashboardBody;
        this.tags = builder.tags == null ? null : Collections.unmodifiableList(builder.tags);
    }

    /**
     * The name of the dashboard. If a dashboard with this name already exists, this call modifies that dashboard,
     * replacing its current contents. Otherwise, a new dashboard is created. The maximum length is 255, and valid
     * characters are A-Z, a-z, 0-9, "-", and "_". This parameter is required.
     */
    public String getDashboardName() {
        return dashboardName;
    }

    /**
     * The detailed information about the dashboard in JSON format, including the widgets to include and their location
     * on the dashboard. This parameter is required.
     *
     * <p>For more information about the syntax, see <a href="https://docs.aws.amazon.com/AmazonCloudWatch/latest/monitoring/CloudWatch-Dashboard-Body-Structure.html">Dashboard Body Structure and Syntax</a>.
     */
    public String getDashboardBody() {
        return dashboardBody;
    }

    /**
     * A list of key-value pairs to associate with the dashboard. You can associate as many as 50 tags with a dashboard.
     *
     * <p>Tags can help you organize and categorize your dashboards. You can also use them to scope user permissions by
     * granting a user permission to access or change only dashboards with certain tag values.
     *
     * <p>You can use this parameter only when creating a new dashboard. If you specify <code>Tags</code> when updating
     * an existing dashboard, the tag updates are ignored. To add or update tags on an existing dashboard, use <a href="https://docs.aws.amazon.com/AmazonCloudWatch/latest/APIReference/API_TagResource.html">
     * TagResource</a>. To remove tags, use <a href="https://docs.aws.amazon.com/AmazonCloudWatch/latest/APIReference/API_UntagResource.html">UntagResource</a>.
     */
    public List<Tag> getTags() {
        if (tags == null) {
            return Collections.emptyList();
        }
        return tags;
    }

    public boolean hasTags() {
        return tags != null;
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
        PutDashboardInput that = (PutDashboardInput) other;
        return Objects.equals(this.dashboardName, that.dashboardName)
               && Objects.equals(this.dashboardBody, that.dashboardBody)
               && Objects.equals(this.tags, that.tags);
    }

    @Override
    public int hashCode() {
        int $hc = Objects.hashCode(dashboardName);
        $hc = 31 * $hc + Objects.hashCode(dashboardBody);
        $hc = 31 * $hc + Objects.hashCode(tags);
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
        if (dashboardBody != null) {
            serializer.writeString($SCHEMA_DASHBOARD_BODY, dashboardBody);
        }
        if (tags != null) {
            serializer.writeList($SCHEMA_TAGS, tags, tags.size(), SharedSerde.TagListSerializer.INSTANCE);
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T getMemberValue(Schema member) {
        return switch (member.memberIndex()) {
            case 0 -> (T) SchemaUtils.validateSameMember($SCHEMA_DASHBOARD_NAME, member, dashboardName);
            case 1 -> (T) SchemaUtils.validateSameMember($SCHEMA_DASHBOARD_BODY, member, dashboardBody);
            case 2 -> (T) SchemaUtils.validateSameMember($SCHEMA_TAGS, member, tags);
            default -> throw new IllegalArgumentException("Attempted to get non-existent member: " + member.id());
        };
    }

    /**
     * Create a new builder containing all the current property values of this object.
     *
     * <p><strong>Note:</strong> This method performs only a shallow copy of the original properties.
     *
     * @return a builder for {@link PutDashboardInput}.
     */
    public Builder toBuilder() {
        var builder = new Builder();
        builder.dashboardName(this.dashboardName);
        builder.dashboardBody(this.dashboardBody);
        builder.tags(this.tags);
        return builder;
    }

    /**
     * @return returns a new Builder.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for {@link PutDashboardInput}.
     */
    public static final class Builder implements ShapeBuilder<PutDashboardInput> {
        private final PresenceTracker tracker = PresenceTracker.of($SCHEMA);
        private String dashboardName;
        private String dashboardBody;
        private List<Tag> tags;

        private Builder() {
            // Tell the tracker to assume clientOptional members are present.
            tracker.setMember($SCHEMA_DASHBOARD_NAME);
            tracker.setMember($SCHEMA_DASHBOARD_BODY);
        }

        @Override
        public Schema schema() {
            return $SCHEMA;
        }

        /**
         * The name of the dashboard. If a dashboard with this name already exists, this call modifies that dashboard,
         * replacing its current contents. Otherwise, a new dashboard is created. The maximum length is 255, and valid
         * characters are A-Z, a-z, 0-9, "-", and "_". This parameter is required.
         *
         * <p><strong>Required</strong>
         * @return this builder.
         */
        public Builder dashboardName(String dashboardName) {
            this.dashboardName = Objects.requireNonNull(dashboardName, "dashboardName cannot be null");
            tracker.setMember($SCHEMA_DASHBOARD_NAME);
            return this;
        }

        /**
         * The detailed information about the dashboard in JSON format, including the widgets to include and their location
         * on the dashboard. This parameter is required.
         *
         * <p>For more information about the syntax, see <a href="https://docs.aws.amazon.com/AmazonCloudWatch/latest/monitoring/CloudWatch-Dashboard-Body-Structure.html">Dashboard Body Structure and Syntax</a>.
         *
         * <p><strong>Required</strong>
         * @return this builder.
         */
        public Builder dashboardBody(String dashboardBody) {
            this.dashboardBody = Objects.requireNonNull(dashboardBody, "dashboardBody cannot be null");
            tracker.setMember($SCHEMA_DASHBOARD_BODY);
            return this;
        }

        /**
         * A list of key-value pairs to associate with the dashboard. You can associate as many as 50 tags with a dashboard.
         *
         * <p>Tags can help you organize and categorize your dashboards. You can also use them to scope user permissions by
         * granting a user permission to access or change only dashboards with certain tag values.
         *
         * <p>You can use this parameter only when creating a new dashboard. If you specify <code>Tags</code> when updating
         * an existing dashboard, the tag updates are ignored. To add or update tags on an existing dashboard, use <a href="https://docs.aws.amazon.com/AmazonCloudWatch/latest/APIReference/API_TagResource.html">
         * TagResource</a>. To remove tags, use <a href="https://docs.aws.amazon.com/AmazonCloudWatch/latest/APIReference/API_UntagResource.html">UntagResource</a>.
         *
         * @return this builder.
         */
        public Builder tags(List<Tag> tags) {
            this.tags = tags;
            return this;
        }

        @Override
        public PutDashboardInput build() {
            tracker.validate();
            return new PutDashboardInput(this);
        }

        @Override
        @SuppressWarnings("unchecked")
        public void setMemberValue(Schema member, Object value) {
            switch (member.memberIndex()) {
                case 0 -> dashboardName((String) SchemaUtils.validateSameMember($SCHEMA_DASHBOARD_NAME, member, value));
                case 1 -> dashboardBody((String) SchemaUtils.validateSameMember($SCHEMA_DASHBOARD_BODY, member, value));
                case 2 -> tags((List<Tag>) SchemaUtils.validateSameMember($SCHEMA_TAGS, member, value));
                default -> ShapeBuilder.super.setMemberValue(member, value);
            }
        }

        @Override
        public ShapeBuilder<PutDashboardInput> errorCorrection() {
            if (tracker.allSet()) {
                return this;
            }
            if (!tracker.checkMember($SCHEMA_DASHBOARD_NAME)) {
                dashboardName("");
            }
            if (!tracker.checkMember($SCHEMA_DASHBOARD_BODY)) {
                dashboardBody("");
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
                    case 1 -> builder.dashboardBody(de.readString(member));
                    case 2 -> builder.tags(SharedSerde.deserializeTagList(member, de));
                    default -> throw new IllegalArgumentException("Unexpected member: " + member.memberName());
                }
            }
        }
    }
}
