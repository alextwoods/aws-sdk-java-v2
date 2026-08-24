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
 * This structure defines the metric to be returned, along with the statistics, period, and units.
 */
@SmithyGenerated
public final class MetricStat implements SerializableStruct {

    public static final Schema $SCHEMA = Schemas.METRIC_STAT;
    private static final Schema $SCHEMA_METRIC = $SCHEMA.member("Metric");
    private static final Schema $SCHEMA_PERIOD = $SCHEMA.member("Period");
    private static final Schema $SCHEMA_STAT = $SCHEMA.member("Stat");
    private static final Schema $SCHEMA_UNIT = $SCHEMA.member("Unit");

    public static final ShapeId $ID = $SCHEMA.id();

    private final transient Metric metric;
    private final transient Integer period;
    private final transient String stat;
    private final transient StandardUnit unit;

    private MetricStat(Builder builder) {
        this.metric = builder.metric;
        this.period = builder.period;
        this.stat = builder.stat;
        this.unit = builder.unit;
    }

    /**
     * The metric to return, including the metric name, namespace, and dimensions.
     */
    public Metric getMetric() {
        return metric;
    }

    /**
     * The granularity, in seconds, of the returned data points. For metrics with regular resolution, a period can be as
     * short as one minute (60 seconds) and must be a multiple of 60. For high-resolution metrics that are collected at
     * intervals of less than one minute, the period can be 1, 5, 10, 20, 30, 60, or any multiple of 60. High-resolution
     * metrics are those metrics stored by a <code>PutMetricData</code> call that includes a <code>StorageResolution</code>
     * of 1 second.
     *
     * <p>If the <code>StartTime</code> parameter specifies a time stamp that is greater than 3 hours ago, you must
     * specify the period as follows or no data points in that time range is returned:
     *
     * <ul>
     *   <li>
     *     Start time between 3 hours and 15 days ago - Use a multiple of 60 seconds (1 minute).
     *   </li>
     *   <li>
     *     Start time between 15 and 63 days ago - Use a multiple of 300 seconds (5 minutes).
     *   </li>
     *   <li>
     *     Start time greater than 63 days ago - Use a multiple of 3600 seconds (1 hour).
     *   </li>
     * </ul>
     */
    public Integer getPeriod() {
        return period;
    }

    /**
     * The statistic to return. It can include any CloudWatch statistic or extended statistic.
     */
    public String getStat() {
        return stat;
    }

    /**
     * When you are using a <code>Put</code> operation, this defines what unit you want to use when storing the metric.
     *
     * <p>In a <code>Get</code> operation, if you omit <code>Unit</code> then all data that was collected with any unit
     * is returned, along with the corresponding units that were specified when the data was reported to CloudWatch. If
     * you specify a unit, the operation returns only data that was collected with that unit specified. If you specify a
     * unit that does not match the data collected, the results of the operation are null. CloudWatch does not perform
     * unit conversions.
     */
    public StandardUnit getUnit() {
        return unit;
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
        MetricStat that = (MetricStat) other;
        return Objects.equals(this.period, that.period)
               && Objects.equals(this.stat, that.stat)
               && Objects.equals(this.unit, that.unit)
               && Objects.equals(this.metric, that.metric);
    }

    @Override
    public int hashCode() {
        int $hc = Objects.hashCode(metric);
        $hc = 31 * $hc + Objects.hashCode(period);
        $hc = 31 * $hc + Objects.hashCode(stat);
        $hc = 31 * $hc + Objects.hashCode(unit);
        return $hc;
    }

    @Override
    public Schema schema() {
        return $SCHEMA;
    }

    @Override
    public void serializeMembers(ShapeSerializer serializer) {
        if (metric != null) {
            serializer.writeStruct($SCHEMA_METRIC, metric);
        }
        if (period != null) {
            serializer.writeInteger($SCHEMA_PERIOD, period);
        }
        if (stat != null) {
            serializer.writeString($SCHEMA_STAT, stat);
        }
        if (unit != null) {
            serializer.writeString($SCHEMA_UNIT, unit.getValue());
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T getMemberValue(Schema member) {
        return switch (member.memberIndex()) {
            case 0 -> (T) SchemaUtils.validateSameMember($SCHEMA_METRIC, member, metric);
            case 1 -> (T) SchemaUtils.validateSameMember($SCHEMA_PERIOD, member, period);
            case 2 -> (T) SchemaUtils.validateSameMember($SCHEMA_STAT, member, stat);
            case 3 -> (T) SchemaUtils.validateSameMember($SCHEMA_UNIT, member, unit);
            default -> throw new IllegalArgumentException("Attempted to get non-existent member: " + member.id());
        };
    }

    /**
     * Create a new builder containing all the current property values of this object.
     *
     * <p><strong>Note:</strong> This method performs only a shallow copy of the original properties.
     *
     * @return a builder for {@link MetricStat}.
     */
    public Builder toBuilder() {
        var builder = new Builder();
        builder.metric(this.metric);
        builder.period(this.period);
        builder.stat(this.stat);
        builder.unit(this.unit);
        return builder;
    }

    /**
     * @return returns a new Builder.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for {@link MetricStat}.
     */
    public static final class Builder implements ShapeBuilder<MetricStat> {
        private final PresenceTracker tracker = PresenceTracker.of($SCHEMA);
        private Metric metric;
        private Integer period;
        private String stat;
        private StandardUnit unit;

        private Builder() {
            // Tell the tracker to assume clientOptional members are present.
            tracker.setMember($SCHEMA_METRIC);
            tracker.setMember($SCHEMA_PERIOD);
            tracker.setMember($SCHEMA_STAT);
        }

        @Override
        public Schema schema() {
            return $SCHEMA;
        }

        /**
         * The metric to return, including the metric name, namespace, and dimensions.
         *
         * <p><strong>Required</strong>
         * @return this builder.
         */
        public Builder metric(Metric metric) {
            this.metric = Objects.requireNonNull(metric, "metric cannot be null");
            tracker.setMember($SCHEMA_METRIC);
            return this;
        }

        /**
         * The granularity, in seconds, of the returned data points. For metrics with regular resolution, a period can be as
         * short as one minute (60 seconds) and must be a multiple of 60. For high-resolution metrics that are collected at
         * intervals of less than one minute, the period can be 1, 5, 10, 20, 30, 60, or any multiple of 60. High-resolution
         * metrics are those metrics stored by a <code>PutMetricData</code> call that includes a <code>StorageResolution</code>
         * of 1 second.
         *
         * <p>If the <code>StartTime</code> parameter specifies a time stamp that is greater than 3 hours ago, you must
         * specify the period as follows or no data points in that time range is returned:
         *
         * <ul>
         *   <li>
         *     Start time between 3 hours and 15 days ago - Use a multiple of 60 seconds (1 minute).
         *   </li>
         *   <li>
         *     Start time between 15 and 63 days ago - Use a multiple of 300 seconds (5 minutes).
         *   </li>
         *   <li>
         *     Start time greater than 63 days ago - Use a multiple of 3600 seconds (1 hour).
         *   </li>
         * </ul>
         *
         * <p><strong>Required</strong>
         * @return this builder.
         */
        public Builder period(Integer period) {
            this.period = period;
            tracker.setMember($SCHEMA_PERIOD);
            return this;
        }

        /**
         * The statistic to return. It can include any CloudWatch statistic or extended statistic.
         *
         * <p><strong>Required</strong>
         * @return this builder.
         */
        public Builder stat(String stat) {
            this.stat = Objects.requireNonNull(stat, "stat cannot be null");
            tracker.setMember($SCHEMA_STAT);
            return this;
        }

        /**
         * When you are using a <code>Put</code> operation, this defines what unit you want to use when storing the metric.
         *
         * <p>In a <code>Get</code> operation, if you omit <code>Unit</code> then all data that was collected with any unit
         * is returned, along with the corresponding units that were specified when the data was reported to CloudWatch. If
         * you specify a unit, the operation returns only data that was collected with that unit specified. If you specify a
         * unit that does not match the data collected, the results of the operation are null. CloudWatch does not perform
         * unit conversions.
         *
         * @return this builder.
         */
        public Builder unit(StandardUnit unit) {
            this.unit = unit;
            return this;
        }

        @Override
        public MetricStat build() {
            tracker.validate();
            return new MetricStat(this);
        }

        @Override
        @SuppressWarnings("unchecked")
        public void setMemberValue(Schema member, Object value) {
            switch (member.memberIndex()) {
                case 0 -> metric((Metric) SchemaUtils.validateSameMember($SCHEMA_METRIC, member, value));
                case 1 -> period((Integer) SchemaUtils.validateSameMember($SCHEMA_PERIOD, member, value));
                case 2 -> stat((String) SchemaUtils.validateSameMember($SCHEMA_STAT, member, value));
                case 3 -> unit((StandardUnit) SchemaUtils.validateSameMember($SCHEMA_UNIT, member, value));
                default -> ShapeBuilder.super.setMemberValue(member, value);
            }
        }

        @Override
        public ShapeBuilder<MetricStat> errorCorrection() {
            if (tracker.allSet()) {
                return this;
            }
            if (!tracker.checkMember($SCHEMA_METRIC)) {
                tracker.setMember($SCHEMA_METRIC);
            }
            if (!tracker.checkMember($SCHEMA_PERIOD)) {
                tracker.setMember($SCHEMA_PERIOD);
            }
            if (!tracker.checkMember($SCHEMA_STAT)) {
                stat("");
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
                    case 0 -> builder.metric(Metric.builder().deserializeMember(de, member).build());
                    case 1 -> builder.period(de.readInteger(member));
                    case 2 -> builder.stat(de.readString(member));
                    case 3 -> builder.unit(StandardUnit.builder().deserializeMember(de, member).build());
                    default -> throw new IllegalArgumentException("Unexpected member: " + member.memberName());
                }
            }
        }
    }
}
