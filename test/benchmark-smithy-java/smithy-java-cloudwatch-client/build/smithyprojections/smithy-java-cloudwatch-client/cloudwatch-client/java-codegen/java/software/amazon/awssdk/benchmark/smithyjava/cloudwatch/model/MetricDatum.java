package software.amazon.awssdk.benchmark.smithyjava.cloudwatch.model;

import java.time.Instant;
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
 * Encapsulates the information sent to either create a metric or add new values to be aggregated into an existing
 * metric.
 */
@SmithyGenerated
public final class MetricDatum implements SerializableStruct {

    public static final Schema $SCHEMA = Schemas.METRIC_DATUM;
    private static final Schema $SCHEMA_METRIC_NAME = $SCHEMA.member("MetricName");
    private static final Schema $SCHEMA_DIMENSIONS = $SCHEMA.member("Dimensions");
    private static final Schema $SCHEMA_TIMESTAMP = $SCHEMA.member("Timestamp");
    private static final Schema $SCHEMA_VALUE = $SCHEMA.member("Value");
    private static final Schema $SCHEMA_STATISTIC_VALUES = $SCHEMA.member("StatisticValues");
    private static final Schema $SCHEMA_VALUES = $SCHEMA.member("Values");
    private static final Schema $SCHEMA_COUNTS = $SCHEMA.member("Counts");
    private static final Schema $SCHEMA_UNIT = $SCHEMA.member("Unit");
    private static final Schema $SCHEMA_STORAGE_RESOLUTION = $SCHEMA.member("StorageResolution");

    public static final ShapeId $ID = $SCHEMA.id();

    private final transient String metricName;
    private final transient List<Dimension> dimensions;
    private final transient Instant timestamp;
    private final transient Double value;
    private final transient StatisticSet statisticValues;
    private final transient List<Double> values;
    private final transient List<Double> counts;
    private final transient StandardUnit unit;
    private final transient Integer storageResolution;

    private MetricDatum(Builder builder) {
        this.metricName = builder.metricName;
        this.dimensions = builder.dimensions == null ? null : Collections.unmodifiableList(builder.dimensions);
        this.timestamp = builder.timestamp;
        this.value = builder.value;
        this.statisticValues = builder.statisticValues;
        this.values = builder.values == null ? null : Collections.unmodifiableList(builder.values);
        this.counts = builder.counts == null ? null : Collections.unmodifiableList(builder.counts);
        this.unit = builder.unit;
        this.storageResolution = builder.storageResolution;
    }

    /**
     * The name of the metric.
     */
    public String getMetricName() {
        return metricName;
    }

    /**
     * The dimensions associated with the metric.
     */
    public List<Dimension> getDimensions() {
        if (dimensions == null) {
            return Collections.emptyList();
        }
        return dimensions;
    }

    public boolean hasDimensions() {
        return dimensions != null;
    }

    /**
     * The time the metric data was received, expressed as the number of milliseconds since Jan 1, 1970 00:00:00 UTC.
     */
    public Instant getTimestamp() {
        return timestamp;
    }

    /**
     * The value for the metric.
     *
     * <p>Although the parameter accepts numbers of type Double, CloudWatch rejects values that are either too small or
     * too large. Values must be in the range of -2^360 to 2^360. In addition, special values (for example, NaN,
     * +Infinity, -Infinity) are not supported.
     */
    public Double getValue() {
        return value;
    }

    /**
     * The statistical values for the metric.
     */
    public StatisticSet getStatisticValues() {
        return statisticValues;
    }

    /**
     * Array of numbers representing the values for the metric during the period. Each unique value is listed just once
     * in this array, and the corresponding number in the <code>Counts</code> array specifies the number of times that
     * value occurred during the period. You can include up to 150 unique values in each <code>PutMetricData</code>
     * action that specifies a <code>Values</code> array.
     *
     * <p>Although the <code>Values</code> array accepts numbers of type <code>Double</code>, CloudWatch rejects values
     * that are either too small or too large. Values must be in the range of -2^360 to 2^360. In addition, special
     * values (for example, NaN, +Infinity, -Infinity) are not supported.
     */
    public List<Double> getValues() {
        if (values == null) {
            return Collections.emptyList();
        }
        return values;
    }

    public boolean hasValues() {
        return values != null;
    }

    /**
     * Array of numbers that is used along with the <code>Values</code> array. Each number in the <code>Count</code>
     * array is the number of times the corresponding value in the <code>Values</code> array occurred during the period.
     *
     * <p>If you omit the <code>Counts</code> array, the default of 1 is used as the value for each count. If you
     * include a <code>Counts</code> array, it must include the same amount of values as the <code>Values</code> array.
     */
    public List<Double> getCounts() {
        if (counts == null) {
            return Collections.emptyList();
        }
        return counts;
    }

    public boolean hasCounts() {
        return counts != null;
    }

    /**
     * When you are using a <code>Put</code> operation, this defines what unit you want to use when storing the metric.
     *
     * <p>In a <code>Get</code> operation, this displays the unit that is used for the metric.
     */
    public StandardUnit getUnit() {
        return unit;
    }

    /**
     * Valid values are 1 and 60. Setting this to 1 specifies this metric as a high-resolution metric, so that
     * CloudWatch stores the metric with sub-minute resolution down to one second. Setting this to 60 specifies this
     * metric as a regular-resolution metric, which CloudWatch stores at 1-minute resolution. Currently, high resolution
     * is available only for custom metrics. For more information about high-resolution metrics, see <a href="https://docs.aws.amazon.com/AmazonCloudWatch/latest/monitoring/publishingMetrics.html#high-resolution-metrics">High-Resolution
     * Metrics</a> in the <i>Amazon CloudWatch User Guide</i>.
     *
     * <p>This field is optional, if you do not specify it the default of 60 is used.
     */
    public Integer getStorageResolution() {
        return storageResolution;
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
        MetricDatum that = (MetricDatum) other;
        return Objects.equals(this.storageResolution, that.storageResolution)
               && Objects.equals(this.value, that.value)
               && Objects.equals(this.metricName, that.metricName)
               && Objects.equals(this.unit, that.unit)
               && Objects.equals(this.timestamp, that.timestamp)
               && Objects.equals(this.statisticValues, that.statisticValues)
               && Objects.equals(this.dimensions, that.dimensions)
               && Objects.equals(this.values, that.values)
               && Objects.equals(this.counts, that.counts);
    }

    @Override
    public int hashCode() {
        int $hc = Objects.hashCode(metricName);
        $hc = 31 * $hc + Objects.hashCode(dimensions);
        $hc = 31 * $hc + Objects.hashCode(timestamp);
        $hc = 31 * $hc + Objects.hashCode(value);
        $hc = 31 * $hc + Objects.hashCode(statisticValues);
        $hc = 31 * $hc + Objects.hashCode(values);
        $hc = 31 * $hc + Objects.hashCode(counts);
        $hc = 31 * $hc + Objects.hashCode(unit);
        $hc = 31 * $hc + Objects.hashCode(storageResolution);
        return $hc;
    }

    @Override
    public Schema schema() {
        return $SCHEMA;
    }

    @Override
    public void serializeMembers(ShapeSerializer serializer) {
        if (metricName != null) {
            serializer.writeString($SCHEMA_METRIC_NAME, metricName);
        }
        if (dimensions != null) {
            serializer.writeList($SCHEMA_DIMENSIONS, dimensions, dimensions.size(), SharedSerde.DimensionsSerializer.INSTANCE);
        }
        if (timestamp != null) {
            serializer.writeTimestamp($SCHEMA_TIMESTAMP, timestamp);
        }
        if (value != null) {
            serializer.writeDouble($SCHEMA_VALUE, value);
        }
        if (statisticValues != null) {
            serializer.writeStruct($SCHEMA_STATISTIC_VALUES, statisticValues);
        }
        if (values != null) {
            serializer.writeList($SCHEMA_VALUES, values, values.size(), SharedSerde.ValuesSerializer.INSTANCE);
        }
        if (counts != null) {
            serializer.writeList($SCHEMA_COUNTS, counts, counts.size(), SharedSerde.CountsSerializer.INSTANCE);
        }
        if (unit != null) {
            serializer.writeString($SCHEMA_UNIT, unit.getValue());
        }
        if (storageResolution != null) {
            serializer.writeInteger($SCHEMA_STORAGE_RESOLUTION, storageResolution);
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T getMemberValue(Schema member) {
        return switch (member.memberIndex()) {
            case 0 -> (T) SchemaUtils.validateSameMember($SCHEMA_METRIC_NAME, member, metricName);
            case 1 -> (T) SchemaUtils.validateSameMember($SCHEMA_DIMENSIONS, member, dimensions);
            case 2 -> (T) SchemaUtils.validateSameMember($SCHEMA_TIMESTAMP, member, timestamp);
            case 3 -> (T) SchemaUtils.validateSameMember($SCHEMA_VALUE, member, value);
            case 4 -> (T) SchemaUtils.validateSameMember($SCHEMA_STATISTIC_VALUES, member, statisticValues);
            case 5 -> (T) SchemaUtils.validateSameMember($SCHEMA_VALUES, member, values);
            case 6 -> (T) SchemaUtils.validateSameMember($SCHEMA_COUNTS, member, counts);
            case 7 -> (T) SchemaUtils.validateSameMember($SCHEMA_UNIT, member, unit);
            case 8 -> (T) SchemaUtils.validateSameMember($SCHEMA_STORAGE_RESOLUTION, member, storageResolution);
            default -> throw new IllegalArgumentException("Attempted to get non-existent member: " + member.id());
        };
    }

    /**
     * Create a new builder containing all the current property values of this object.
     *
     * <p><strong>Note:</strong> This method performs only a shallow copy of the original properties.
     *
     * @return a builder for {@link MetricDatum}.
     */
    public Builder toBuilder() {
        var builder = new Builder();
        builder.metricName(this.metricName);
        builder.dimensions(this.dimensions);
        builder.timestamp(this.timestamp);
        builder.value(this.value);
        builder.statisticValues(this.statisticValues);
        builder.values(this.values);
        builder.counts(this.counts);
        builder.unit(this.unit);
        builder.storageResolution(this.storageResolution);
        return builder;
    }

    /**
     * @return returns a new Builder.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for {@link MetricDatum}.
     */
    public static final class Builder implements ShapeBuilder<MetricDatum> {
        private final PresenceTracker tracker = PresenceTracker.of($SCHEMA);
        private String metricName;
        private List<Dimension> dimensions;
        private Instant timestamp;
        private Double value;
        private StatisticSet statisticValues;
        private List<Double> values;
        private List<Double> counts;
        private StandardUnit unit;
        private Integer storageResolution;

        private Builder() {
            // Tell the tracker to assume clientOptional members are present.
            tracker.setMember($SCHEMA_METRIC_NAME);
        }

        @Override
        public Schema schema() {
            return $SCHEMA;
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

        /**
         * The dimensions associated with the metric.
         *
         * @return this builder.
         */
        public Builder dimensions(List<Dimension> dimensions) {
            this.dimensions = dimensions;
            return this;
        }

        /**
         * The time the metric data was received, expressed as the number of milliseconds since Jan 1, 1970 00:00:00 UTC.
         *
         * @return this builder.
         */
        public Builder timestamp(Instant timestamp) {
            this.timestamp = timestamp;
            return this;
        }

        /**
         * The value for the metric.
         *
         * <p>Although the parameter accepts numbers of type Double, CloudWatch rejects values that are either too small or
         * too large. Values must be in the range of -2^360 to 2^360. In addition, special values (for example, NaN,
         * +Infinity, -Infinity) are not supported.
         *
         * @return this builder.
         */
        public Builder value(Double value) {
            this.value = value;
            return this;
        }

        /**
         * The statistical values for the metric.
         *
         * @return this builder.
         */
        public Builder statisticValues(StatisticSet statisticValues) {
            this.statisticValues = statisticValues;
            return this;
        }

        /**
         * Array of numbers representing the values for the metric during the period. Each unique value is listed just once
         * in this array, and the corresponding number in the <code>Counts</code> array specifies the number of times that
         * value occurred during the period. You can include up to 150 unique values in each <code>PutMetricData</code>
         * action that specifies a <code>Values</code> array.
         *
         * <p>Although the <code>Values</code> array accepts numbers of type <code>Double</code>, CloudWatch rejects values
         * that are either too small or too large. Values must be in the range of -2^360 to 2^360. In addition, special
         * values (for example, NaN, +Infinity, -Infinity) are not supported.
         *
         * @return this builder.
         */
        public Builder values(List<Double> values) {
            this.values = values;
            return this;
        }

        /**
         * Array of numbers that is used along with the <code>Values</code> array. Each number in the <code>Count</code>
         * array is the number of times the corresponding value in the <code>Values</code> array occurred during the period.
         *
         * <p>If you omit the <code>Counts</code> array, the default of 1 is used as the value for each count. If you
         * include a <code>Counts</code> array, it must include the same amount of values as the <code>Values</code> array.
         *
         * @return this builder.
         */
        public Builder counts(List<Double> counts) {
            this.counts = counts;
            return this;
        }

        /**
         * When you are using a <code>Put</code> operation, this defines what unit you want to use when storing the metric.
         *
         * <p>In a <code>Get</code> operation, this displays the unit that is used for the metric.
         *
         * @return this builder.
         */
        public Builder unit(StandardUnit unit) {
            this.unit = unit;
            return this;
        }

        /**
         * Valid values are 1 and 60. Setting this to 1 specifies this metric as a high-resolution metric, so that
         * CloudWatch stores the metric with sub-minute resolution down to one second. Setting this to 60 specifies this
         * metric as a regular-resolution metric, which CloudWatch stores at 1-minute resolution. Currently, high resolution
         * is available only for custom metrics. For more information about high-resolution metrics, see <a href="https://docs.aws.amazon.com/AmazonCloudWatch/latest/monitoring/publishingMetrics.html#high-resolution-metrics">High-Resolution
         * Metrics</a> in the <i>Amazon CloudWatch User Guide</i>.
         *
         * <p>This field is optional, if you do not specify it the default of 60 is used.
         *
         * @return this builder.
         */
        public Builder storageResolution(Integer storageResolution) {
            this.storageResolution = storageResolution;
            return this;
        }

        @Override
        public MetricDatum build() {
            tracker.validate();
            return new MetricDatum(this);
        }

        @Override
        @SuppressWarnings("unchecked")
        public void setMemberValue(Schema member, Object value) {
            switch (member.memberIndex()) {
                case 0 -> metricName((String) SchemaUtils.validateSameMember($SCHEMA_METRIC_NAME, member, value));
                case 1 -> dimensions((List<Dimension>) SchemaUtils.validateSameMember($SCHEMA_DIMENSIONS, member, value));
                case 2 -> timestamp((Instant) SchemaUtils.validateSameMember($SCHEMA_TIMESTAMP, member, value));
                case 3 -> value((Double) SchemaUtils.validateSameMember($SCHEMA_VALUE, member, value));
                case 4 -> statisticValues((StatisticSet) SchemaUtils.validateSameMember($SCHEMA_STATISTIC_VALUES, member, value));
                case 5 -> values((List<Double>) SchemaUtils.validateSameMember($SCHEMA_VALUES, member, value));
                case 6 -> counts((List<Double>) SchemaUtils.validateSameMember($SCHEMA_COUNTS, member, value));
                case 7 -> unit((StandardUnit) SchemaUtils.validateSameMember($SCHEMA_UNIT, member, value));
                case 8 -> storageResolution((Integer) SchemaUtils.validateSameMember($SCHEMA_STORAGE_RESOLUTION, member, value));
                default -> ShapeBuilder.super.setMemberValue(member, value);
            }
        }

        @Override
        public ShapeBuilder<MetricDatum> errorCorrection() {
            if (tracker.allSet()) {
                return this;
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
                    case 0 -> builder.metricName(de.readString(member));
                    case 1 -> builder.dimensions(SharedSerde.deserializeDimensions(member, de));
                    case 2 -> builder.timestamp(de.readTimestamp(member));
                    case 3 -> builder.value(de.readDouble(member));
                    case 4 -> builder.statisticValues(StatisticSet.builder().deserializeMember(de, member).build());
                    case 5 -> builder.values(SharedSerde.deserializeValues(member, de));
                    case 6 -> builder.counts(SharedSerde.deserializeCounts(member, de));
                    case 7 -> builder.unit(StandardUnit.builder().deserializeMember(de, member).build());
                    case 8 -> builder.storageResolution(de.readInteger(member));
                    default -> throw new IllegalArgumentException("Unexpected member: " + member.memberName());
                }
            }
        }
    }
}
