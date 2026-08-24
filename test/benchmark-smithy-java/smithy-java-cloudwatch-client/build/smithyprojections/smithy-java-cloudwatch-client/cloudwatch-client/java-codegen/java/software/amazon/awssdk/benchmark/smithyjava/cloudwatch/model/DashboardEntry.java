package software.amazon.awssdk.benchmark.smithyjava.cloudwatch.model;

import java.time.Instant;
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

/**
 * Represents a specific dashboard.
 */
@SmithyGenerated
public final class DashboardEntry implements SerializableStruct {

    public static final Schema $SCHEMA = Schemas.DASHBOARD_ENTRY;
    private static final Schema $SCHEMA_DASHBOARD_NAME = $SCHEMA.member("DashboardName");
    private static final Schema $SCHEMA_DASHBOARD_ARN = $SCHEMA.member("DashboardArn");
    private static final Schema $SCHEMA_LAST_MODIFIED = $SCHEMA.member("LastModified");
    private static final Schema $SCHEMA_SIZE = $SCHEMA.member("Size");

    public static final ShapeId $ID = $SCHEMA.id();

    private final transient String dashboardName;
    private final transient String dashboardArn;
    private final transient Instant lastModified;
    private final transient Long size;

    private DashboardEntry(Builder builder) {
        this.dashboardName = builder.dashboardName;
        this.dashboardArn = builder.dashboardArn;
        this.lastModified = builder.lastModified;
        this.size = builder.size;
    }

    /**
     * The name of the dashboard.
     */
    public String getDashboardName() {
        return dashboardName;
    }

    /**
     * The Amazon Resource Name (ARN) of the dashboard.
     */
    public String getDashboardArn() {
        return dashboardArn;
    }

    /**
     * The time stamp of when the dashboard was last modified, either by an API call or through the console. This number
     * is expressed as the number of milliseconds since Jan 1, 1970 00:00:00 UTC.
     */
    public Instant getLastModified() {
        return lastModified;
    }

    /**
     * The size of the dashboard, in bytes.
     */
    public Long getSize() {
        return size;
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
        DashboardEntry that = (DashboardEntry) other;
        return Objects.equals(this.size, that.size)
               && Objects.equals(this.dashboardName, that.dashboardName)
               && Objects.equals(this.dashboardArn, that.dashboardArn)
               && Objects.equals(this.lastModified, that.lastModified);
    }

    @Override
    public int hashCode() {
        int $hc = Objects.hashCode(dashboardName);
        $hc = 31 * $hc + Objects.hashCode(dashboardArn);
        $hc = 31 * $hc + Objects.hashCode(lastModified);
        $hc = 31 * $hc + Objects.hashCode(size);
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
        if (dashboardArn != null) {
            serializer.writeString($SCHEMA_DASHBOARD_ARN, dashboardArn);
        }
        if (lastModified != null) {
            serializer.writeTimestamp($SCHEMA_LAST_MODIFIED, lastModified);
        }
        if (size != null) {
            serializer.writeLong($SCHEMA_SIZE, size);
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T getMemberValue(Schema member) {
        return switch (member.memberIndex()) {
            case 0 -> (T) SchemaUtils.validateSameMember($SCHEMA_DASHBOARD_NAME, member, dashboardName);
            case 1 -> (T) SchemaUtils.validateSameMember($SCHEMA_DASHBOARD_ARN, member, dashboardArn);
            case 2 -> (T) SchemaUtils.validateSameMember($SCHEMA_LAST_MODIFIED, member, lastModified);
            case 3 -> (T) SchemaUtils.validateSameMember($SCHEMA_SIZE, member, size);
            default -> throw new IllegalArgumentException("Attempted to get non-existent member: " + member.id());
        };
    }

    /**
     * Create a new builder containing all the current property values of this object.
     *
     * <p><strong>Note:</strong> This method performs only a shallow copy of the original properties.
     *
     * @return a builder for {@link DashboardEntry}.
     */
    public Builder toBuilder() {
        var builder = new Builder();
        builder.dashboardName(this.dashboardName);
        builder.dashboardArn(this.dashboardArn);
        builder.lastModified(this.lastModified);
        builder.size(this.size);
        return builder;
    }

    /**
     * @return returns a new Builder.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for {@link DashboardEntry}.
     */
    public static final class Builder implements ShapeBuilder<DashboardEntry> {
        private String dashboardName;
        private String dashboardArn;
        private Instant lastModified;
        private Long size;

        private Builder() {}

        @Override
        public Schema schema() {
            return $SCHEMA;
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
         * The time stamp of when the dashboard was last modified, either by an API call or through the console. This number
         * is expressed as the number of milliseconds since Jan 1, 1970 00:00:00 UTC.
         *
         * @return this builder.
         */
        public Builder lastModified(Instant lastModified) {
            this.lastModified = lastModified;
            return this;
        }

        /**
         * The size of the dashboard, in bytes.
         *
         * @return this builder.
         */
        public Builder size(Long size) {
            this.size = size;
            return this;
        }

        @Override
        public DashboardEntry build() {
            return new DashboardEntry(this);
        }

        @Override
        @SuppressWarnings("unchecked")
        public void setMemberValue(Schema member, Object value) {
            switch (member.memberIndex()) {
                case 0 -> dashboardName((String) SchemaUtils.validateSameMember($SCHEMA_DASHBOARD_NAME, member, value));
                case 1 -> dashboardArn((String) SchemaUtils.validateSameMember($SCHEMA_DASHBOARD_ARN, member, value));
                case 2 -> lastModified((Instant) SchemaUtils.validateSameMember($SCHEMA_LAST_MODIFIED, member, value));
                case 3 -> size((Long) SchemaUtils.validateSameMember($SCHEMA_SIZE, member, value));
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
                    case 0 -> builder.dashboardName(de.readString(member));
                    case 1 -> builder.dashboardArn(de.readString(member));
                    case 2 -> builder.lastModified(de.readTimestamp(member));
                    case 3 -> builder.size(de.readLong(member));
                    default -> throw new IllegalArgumentException("Unexpected member: " + member.memberName());
                }
            }
        }
    }
}
