package software.amazon.awssdk.benchmark.smithyjava.s3.model;

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

/**
 * Specifies a metrics configuration for the CloudWatch request metrics (specified by the metrics configuration ID) from
 * an Amazon S3 bucket. If you're updating an existing metrics configuration, note that this is a full replacement of
 * the existing metrics configuration. If you don't include the elements you want to keep, they are erased. For more
 * information, see <a
 * href="https://docs.aws.amazon.com/AmazonS3/latest/API/RESTBucketPUTMetricConfiguration.html">PutBucketMetricsConfiguration</a>.
 */
@SmithyGenerated
public final class MetricsConfiguration implements SerializableStruct {

    public static final Schema $SCHEMA = Schemas2.METRICS_CONFIGURATION;
    private static final Schema $SCHEMA_ID = $SCHEMA.member("Id");
    private static final Schema $SCHEMA_FILTER = $SCHEMA.member("Filter");

    public static final ShapeId $ID = $SCHEMA.id();

    private final transient String id;
    private final transient MetricsFilter filter;

    private MetricsConfiguration(Builder builder) {
        this.id = builder.id;
        this.filter = builder.filter;
    }

    /**
     * The ID used to identify the metrics configuration. The ID has a 64 character limit and can only contain letters,
     * numbers, periods, dashes, and underscores.
     */
    public String getId() {
        return id;
    }

    /**
     * Specifies a metrics configuration filter. The metrics configuration will only include objects that meet the
     * filter's criteria. A filter must be a prefix, an object tag, an access point ARN, or a conjunction
     * (MetricsAndOperator).
     *
     * <p>Metrics configurations for directory buckets do not support tag filters.
     */
    public MetricsFilter getFilter() {
        return filter;
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
        MetricsConfiguration that = (MetricsConfiguration) other;
        return Objects.equals(this.id, that.id)
               && Objects.equals(this.filter, that.filter);
    }

    @Override
    public int hashCode() {
        int $hc = Objects.hashCode(id);
        $hc = 31 * $hc + Objects.hashCode(filter);
        return $hc;
    }

    @Override
    public Schema schema() {
        return $SCHEMA;
    }

    @Override
    public void serializeMembers(ShapeSerializer serializer) {
        serializer.writeString($SCHEMA_ID, id);
        if (filter != null) {
            serializer.writeStruct($SCHEMA_FILTER, filter);
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T getMemberValue(Schema member) {
        return switch (member.memberIndex()) {
            case 0 -> (T) SchemaUtils.validateSameMember($SCHEMA_ID, member, id);
            case 1 -> (T) SchemaUtils.validateSameMember($SCHEMA_FILTER, member, filter);
            default -> throw new IllegalArgumentException("Attempted to get non-existent member: " + member.id());
        };
    }

    /**
     * Create a new builder containing all the current property values of this object.
     *
     * <p><strong>Note:</strong> This method performs only a shallow copy of the original properties.
     *
     * @return a builder for {@link MetricsConfiguration}.
     */
    public Builder toBuilder() {
        var builder = new Builder();
        builder.id(this.id);
        builder.filter(this.filter);
        return builder;
    }

    /**
     * @return returns a new Builder.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for {@link MetricsConfiguration}.
     */
    public static final class Builder implements ShapeBuilder<MetricsConfiguration> {
        private final PresenceTracker tracker = PresenceTracker.of($SCHEMA);
        private String id;
        private MetricsFilter filter;

        private Builder() {}

        @Override
        public Schema schema() {
            return $SCHEMA;
        }

        /**
         * The ID used to identify the metrics configuration. The ID has a 64 character limit and can only contain letters,
         * numbers, periods, dashes, and underscores.
         *
         * <p><strong>Required</strong>
         * @return this builder.
         */
        public Builder id(String id) {
            this.id = Objects.requireNonNull(id, "id cannot be null");
            tracker.setMember($SCHEMA_ID);
            return this;
        }

        /**
         * Specifies a metrics configuration filter. The metrics configuration will only include objects that meet the
         * filter's criteria. A filter must be a prefix, an object tag, an access point ARN, or a conjunction
         * (MetricsAndOperator).
         *
         * <p>Metrics configurations for directory buckets do not support tag filters.
         *
         * @return this builder.
         */
        public Builder filter(MetricsFilter filter) {
            this.filter = filter;
            return this;
        }

        @Override
        public MetricsConfiguration build() {
            tracker.validate();
            return new MetricsConfiguration(this);
        }

        @Override
        @SuppressWarnings("unchecked")
        public void setMemberValue(Schema member, Object value) {
            switch (member.memberIndex()) {
                case 0 -> id((String) SchemaUtils.validateSameMember($SCHEMA_ID, member, value));
                case 1 -> filter((MetricsFilter) SchemaUtils.validateSameMember($SCHEMA_FILTER, member, value));
                default -> ShapeBuilder.super.setMemberValue(member, value);
            }
        }

        @Override
        public ShapeBuilder<MetricsConfiguration> errorCorrection() {
            if (tracker.allSet()) {
                return this;
            }
            if (!tracker.checkMember($SCHEMA_ID)) {
                id("");
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
                    case 0 -> builder.id(de.readString(member));
                    case 1 -> builder.filter(MetricsFilter.builder().deserializeMember(de, member).build());
                    default -> throw new IllegalArgumentException("Unexpected member: " + member.memberName());
                }
            }
        }
    }
}
