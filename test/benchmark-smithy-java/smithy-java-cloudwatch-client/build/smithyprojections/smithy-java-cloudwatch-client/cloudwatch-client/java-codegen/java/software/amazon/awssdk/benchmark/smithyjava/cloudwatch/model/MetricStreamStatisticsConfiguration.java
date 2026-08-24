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

/**
 * By default, a metric stream always sends the <code>MAX</code>, <code>MIN</code>, <code>SUM</code>, and
 * <code>SAMPLECOUNT</code> statistics for each metric that is streamed. This structure contains information for one
 * metric that includes additional statistics in the stream. For more information about statistics, see CloudWatch,
 * listed in <a href="https://docs.aws.amazon.com/AmazonCloudWatch/latest/monitoring/Statistics-definitions.html">
 * CloudWatch statistics definitions</a>.
 */
@SmithyGenerated
public final class MetricStreamStatisticsConfiguration implements SerializableStruct {

    public static final Schema $SCHEMA = Schemas.METRIC_STREAM_STATISTICS_CONFIGURATION;
    private static final Schema $SCHEMA_INCLUDE_METRICS = $SCHEMA.member("IncludeMetrics");
    private static final Schema $SCHEMA_ADDITIONAL_STATISTICS = $SCHEMA.member("AdditionalStatistics");

    public static final ShapeId $ID = $SCHEMA.id();

    private final transient List<MetricStreamStatisticsMetric> includeMetrics;
    private final transient List<String> additionalStatistics;

    private MetricStreamStatisticsConfiguration(Builder builder) {
        this.includeMetrics = builder.includeMetrics == null ? null : Collections.unmodifiableList(builder.includeMetrics);
        this.additionalStatistics = builder.additionalStatistics == null ? null : Collections.unmodifiableList(builder.additionalStatistics);
    }

    /**
     * An array of metric name and namespace pairs that stream the additional statistics listed in the value of the <code>
     * AdditionalStatistics</code> parameter. There can be as many as 100 pairs in the array.
     *
     * <p>All metrics that match the combination of metric name and namespace will be streamed with the additional
     * statistics, no matter their dimensions.
     */
    public List<MetricStreamStatisticsMetric> getIncludeMetrics() {
        if (includeMetrics == null) {
            return Collections.emptyList();
        }
        return includeMetrics;
    }

    public boolean hasIncludeMetrics() {
        return includeMetrics != null;
    }

    /**
     * The list of additional statistics that are to be streamed for the metrics listed in the <code>IncludeMetrics</code>
     * array in this structure. This list can include as many as 20 statistics.
     *
     * <p>If the <code>OutputFormat</code> for the stream is <code>opentelemetry1.0</code> or <code>opentelemetry0.7</code>
     * , the only valid values are <code>p<i>??</i></code> percentile statistics such as <code>p90</code>, <code>p99</code>
     * and so on.
     *
     * <p>If the <code>OutputFormat</code> for the stream is <code>json</code>, the valid values include the
     * abbreviations for all of the statistics listed in <a href="https://docs.aws.amazon.com/AmazonCloudWatch/latest/monitoring/Statistics-definitions.html"> CloudWatch statistics definitions</a>. For example, this
     * includes <code>tm98, </code><code>wm90</code>, <code>PR(:300)</code>, and so on.
     */
    public List<String> getAdditionalStatistics() {
        if (additionalStatistics == null) {
            return Collections.emptyList();
        }
        return additionalStatistics;
    }

    public boolean hasAdditionalStatistics() {
        return additionalStatistics != null;
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
        MetricStreamStatisticsConfiguration that = (MetricStreamStatisticsConfiguration) other;
        return Objects.equals(this.includeMetrics, that.includeMetrics)
               && Objects.equals(this.additionalStatistics, that.additionalStatistics);
    }

    @Override
    public int hashCode() {
        int $hc = Objects.hashCode(includeMetrics);
        $hc = 31 * $hc + Objects.hashCode(additionalStatistics);
        return $hc;
    }

    @Override
    public Schema schema() {
        return $SCHEMA;
    }

    @Override
    public void serializeMembers(ShapeSerializer serializer) {
        if (includeMetrics != null) {
            serializer.writeList($SCHEMA_INCLUDE_METRICS, includeMetrics, includeMetrics.size(), SharedSerde.MetricStreamStatisticsIncludeMetricsSerializer.INSTANCE);
        }
        if (additionalStatistics != null) {
            serializer.writeList($SCHEMA_ADDITIONAL_STATISTICS, additionalStatistics, additionalStatistics.size(), SharedSerde.MetricStreamStatisticsAdditionalStatisticsSerializer.INSTANCE);
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T getMemberValue(Schema member) {
        return switch (member.memberIndex()) {
            case 0 -> (T) SchemaUtils.validateSameMember($SCHEMA_INCLUDE_METRICS, member, includeMetrics);
            case 1 -> (T) SchemaUtils.validateSameMember($SCHEMA_ADDITIONAL_STATISTICS, member, additionalStatistics);
            default -> throw new IllegalArgumentException("Attempted to get non-existent member: " + member.id());
        };
    }

    /**
     * Create a new builder containing all the current property values of this object.
     *
     * <p><strong>Note:</strong> This method performs only a shallow copy of the original properties.
     *
     * @return a builder for {@link MetricStreamStatisticsConfiguration}.
     */
    public Builder toBuilder() {
        var builder = new Builder();
        builder.includeMetrics(this.includeMetrics);
        builder.additionalStatistics(this.additionalStatistics);
        return builder;
    }

    /**
     * @return returns a new Builder.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for {@link MetricStreamStatisticsConfiguration}.
     */
    public static final class Builder implements ShapeBuilder<MetricStreamStatisticsConfiguration> {
        private final PresenceTracker tracker = PresenceTracker.of($SCHEMA);
        private List<MetricStreamStatisticsMetric> includeMetrics;
        private List<String> additionalStatistics;

        private Builder() {
            // Tell the tracker to assume clientOptional members are present.
            tracker.setMember($SCHEMA_INCLUDE_METRICS);
            tracker.setMember($SCHEMA_ADDITIONAL_STATISTICS);
        }

        @Override
        public Schema schema() {
            return $SCHEMA;
        }

        /**
         * An array of metric name and namespace pairs that stream the additional statistics listed in the value of the <code>
         * AdditionalStatistics</code> parameter. There can be as many as 100 pairs in the array.
         *
         * <p>All metrics that match the combination of metric name and namespace will be streamed with the additional
         * statistics, no matter their dimensions.
         *
         * <p><strong>Required</strong>
         * @return this builder.
         */
        public Builder includeMetrics(List<MetricStreamStatisticsMetric> includeMetrics) {
            this.includeMetrics = Objects.requireNonNull(includeMetrics, "includeMetrics cannot be null");
            tracker.setMember($SCHEMA_INCLUDE_METRICS);
            return this;
        }

        /**
         * The list of additional statistics that are to be streamed for the metrics listed in the <code>IncludeMetrics</code>
         * array in this structure. This list can include as many as 20 statistics.
         *
         * <p>If the <code>OutputFormat</code> for the stream is <code>opentelemetry1.0</code> or <code>opentelemetry0.7</code>
         * , the only valid values are <code>p<i>??</i></code> percentile statistics such as <code>p90</code>, <code>p99</code>
         * and so on.
         *
         * <p>If the <code>OutputFormat</code> for the stream is <code>json</code>, the valid values include the
         * abbreviations for all of the statistics listed in <a href="https://docs.aws.amazon.com/AmazonCloudWatch/latest/monitoring/Statistics-definitions.html"> CloudWatch statistics definitions</a>. For example, this
         * includes <code>tm98, </code><code>wm90</code>, <code>PR(:300)</code>, and so on.
         *
         * <p><strong>Required</strong>
         * @return this builder.
         */
        public Builder additionalStatistics(List<String> additionalStatistics) {
            this.additionalStatistics = Objects.requireNonNull(additionalStatistics, "additionalStatistics cannot be null");
            tracker.setMember($SCHEMA_ADDITIONAL_STATISTICS);
            return this;
        }

        @Override
        public MetricStreamStatisticsConfiguration build() {
            tracker.validate();
            return new MetricStreamStatisticsConfiguration(this);
        }

        @Override
        @SuppressWarnings("unchecked")
        public void setMemberValue(Schema member, Object value) {
            switch (member.memberIndex()) {
                case 0 -> includeMetrics((List<MetricStreamStatisticsMetric>) SchemaUtils.validateSameMember($SCHEMA_INCLUDE_METRICS, member, value));
                case 1 -> additionalStatistics((List<String>) SchemaUtils.validateSameMember($SCHEMA_ADDITIONAL_STATISTICS, member, value));
                default -> ShapeBuilder.super.setMemberValue(member, value);
            }
        }

        @Override
        public ShapeBuilder<MetricStreamStatisticsConfiguration> errorCorrection() {
            if (tracker.allSet()) {
                return this;
            }
            if (!tracker.checkMember($SCHEMA_INCLUDE_METRICS)) {
                includeMetrics(Collections.emptyList());
            }
            if (!tracker.checkMember($SCHEMA_ADDITIONAL_STATISTICS)) {
                additionalStatistics(Collections.emptyList());
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
                    case 0 -> builder.includeMetrics(SharedSerde.deserializeMetricStreamStatisticsIncludeMetrics(member, de));
                    case 1 -> builder.additionalStatistics(SharedSerde.deserializeMetricStreamStatisticsAdditionalStatistics(member, de));
                    default -> throw new IllegalArgumentException("Unexpected member: " + member.memberName());
                }
            }
        }
    }
}
