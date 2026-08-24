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

/**
 * This object contains the information for one metric that is to be streamed with additional statistics.
 */
@SmithyGenerated
public final class MetricStreamStatisticsMetric implements SerializableStruct {

    public static final Schema $SCHEMA = Schemas.METRIC_STREAM_STATISTICS_METRIC;
    private static final Schema $SCHEMA_NAMESPACE = $SCHEMA.member("Namespace");
    private static final Schema $SCHEMA_METRIC_NAME = $SCHEMA.member("MetricName");

    public static final ShapeId $ID = $SCHEMA.id();

    private final transient String namespace;
    private final transient String metricName;

    private MetricStreamStatisticsMetric(Builder builder) {
        this.namespace = builder.namespace;
        this.metricName = builder.metricName;
    }

    /**
     * The namespace of the metric.
     */
    public String getNamespace() {
        return namespace;
    }

    /**
     * The name of the metric.
     */
    public String getMetricName() {
        return metricName;
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
        MetricStreamStatisticsMetric that = (MetricStreamStatisticsMetric) other;
        return Objects.equals(this.namespace, that.namespace)
               && Objects.equals(this.metricName, that.metricName);
    }

    @Override
    public int hashCode() {
        int $hc = Objects.hashCode(namespace);
        $hc = 31 * $hc + Objects.hashCode(metricName);
        return $hc;
    }

    @Override
    public Schema schema() {
        return $SCHEMA;
    }

    @Override
    public void serializeMembers(ShapeSerializer serializer) {
        if (namespace != null) {
            serializer.writeString($SCHEMA_NAMESPACE, namespace);
        }
        if (metricName != null) {
            serializer.writeString($SCHEMA_METRIC_NAME, metricName);
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T getMemberValue(Schema member) {
        return switch (member.memberIndex()) {
            case 0 -> (T) SchemaUtils.validateSameMember($SCHEMA_NAMESPACE, member, namespace);
            case 1 -> (T) SchemaUtils.validateSameMember($SCHEMA_METRIC_NAME, member, metricName);
            default -> throw new IllegalArgumentException("Attempted to get non-existent member: " + member.id());
        };
    }

    /**
     * Create a new builder containing all the current property values of this object.
     *
     * <p><strong>Note:</strong> This method performs only a shallow copy of the original properties.
     *
     * @return a builder for {@link MetricStreamStatisticsMetric}.
     */
    public Builder toBuilder() {
        var builder = new Builder();
        builder.namespace(this.namespace);
        builder.metricName(this.metricName);
        return builder;
    }

    /**
     * @return returns a new Builder.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for {@link MetricStreamStatisticsMetric}.
     */
    public static final class Builder implements ShapeBuilder<MetricStreamStatisticsMetric> {
        private final PresenceTracker tracker = PresenceTracker.of($SCHEMA);
        private String namespace;
        private String metricName;

        private Builder() {
            // Tell the tracker to assume clientOptional members are present.
            tracker.setMember($SCHEMA_NAMESPACE);
            tracker.setMember($SCHEMA_METRIC_NAME);
        }

        @Override
        public Schema schema() {
            return $SCHEMA;
        }

        /**
         * The namespace of the metric.
         *
         * <p><strong>Required</strong>
         * @return this builder.
         */
        public Builder namespace(String namespace) {
            this.namespace = Objects.requireNonNull(namespace, "namespace cannot be null");
            tracker.setMember($SCHEMA_NAMESPACE);
            return this;
        }

        /**
         * The name of the metric.
         *
         * <p><strong>Required</strong>
         * @return this builder.
         */
        public Builder metricName(String metricName) {
            this.metricName = Objects.requireNonNull(metricName, "metricName cannot be null");
            tracker.setMember($SCHEMA_METRIC_NAME);
            return this;
        }

        @Override
        public MetricStreamStatisticsMetric build() {
            tracker.validate();
            return new MetricStreamStatisticsMetric(this);
        }

        @Override
        @SuppressWarnings("unchecked")
        public void setMemberValue(Schema member, Object value) {
            switch (member.memberIndex()) {
                case 0 -> namespace((String) SchemaUtils.validateSameMember($SCHEMA_NAMESPACE, member, value));
                case 1 -> metricName((String) SchemaUtils.validateSameMember($SCHEMA_METRIC_NAME, member, value));
                default -> ShapeBuilder.super.setMemberValue(member, value);
            }
        }

        @Override
        public ShapeBuilder<MetricStreamStatisticsMetric> errorCorrection() {
            if (tracker.allSet()) {
                return this;
            }
            if (!tracker.checkMember($SCHEMA_NAMESPACE)) {
                namespace("");
            }
            if (!tracker.checkMember($SCHEMA_METRIC_NAME)) {
                metricName("");
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
                    case 0 -> builder.namespace(de.readString(member));
                    case 1 -> builder.metricName(de.readString(member));
                    default -> throw new IllegalArgumentException("Unexpected member: " + member.memberName());
                }
            }
        }
    }
}
