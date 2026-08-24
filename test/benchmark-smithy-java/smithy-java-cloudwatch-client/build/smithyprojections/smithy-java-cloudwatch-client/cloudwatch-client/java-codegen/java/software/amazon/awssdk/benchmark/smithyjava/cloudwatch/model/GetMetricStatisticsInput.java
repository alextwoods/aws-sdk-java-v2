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

@SmithyGenerated
public final class GetMetricStatisticsInput implements SerializableStruct {

    public static final Schema $SCHEMA = Schemas.GET_METRIC_STATISTICS_INPUT;
    private static final Schema $SCHEMA_NAMESPACE = $SCHEMA.member("Namespace");
    private static final Schema $SCHEMA_METRIC_NAME = $SCHEMA.member("MetricName");
    private static final Schema $SCHEMA_DIMENSIONS = $SCHEMA.member("Dimensions");
    private static final Schema $SCHEMA_START_TIME = $SCHEMA.member("StartTime");
    private static final Schema $SCHEMA_END_TIME = $SCHEMA.member("EndTime");
    private static final Schema $SCHEMA_PERIOD = $SCHEMA.member("Period");
    private static final Schema $SCHEMA_STATISTICS = $SCHEMA.member("Statistics");
    private static final Schema $SCHEMA_EXTENDED_STATISTICS = $SCHEMA.member("ExtendedStatistics");
    private static final Schema $SCHEMA_UNIT = $SCHEMA.member("Unit");

    public static final ShapeId $ID = $SCHEMA.id();

    private final transient String namespace;
    private final transient String metricName;
    private final transient List<Dimension> dimensions;
    private final transient Instant startTime;
    private final transient Instant endTime;
    private final transient Integer period;
    private final transient List<Statistic> statistics;
    private final transient List<String> extendedStatistics;
    private final transient StandardUnit unit;

    private GetMetricStatisticsInput(Builder builder) {
        this.namespace = builder.namespace;
        this.metricName = builder.metricName;
        this.dimensions = builder.dimensions == null ? null : Collections.unmodifiableList(builder.dimensions);
        this.startTime = builder.startTime;
        this.endTime = builder.endTime;
        this.period = builder.period;
        this.statistics = builder.statistics == null ? null : Collections.unmodifiableList(builder.statistics);
        this.extendedStatistics = builder.extendedStatistics == null ? null : Collections.unmodifiableList(builder.extendedStatistics);
        this.unit = builder.unit;
    }

    /**
     * The namespace of the metric, with or without spaces.
     */
    public String getNamespace() {
        return namespace;
    }

    /**
     * The name of the metric, with or without spaces.
     */
    public String getMetricName() {
        return metricName;
    }

    /**
     * The dimensions. If the metric contains multiple dimensions, you must include a value for each dimension.
     * CloudWatch treats each unique combination of dimensions as a separate metric. If a specific combination of
     * dimensions was not published, you can't retrieve statistics for it. You must specify the same dimensions that
     * were used when the metrics were created. For an example, see <a href="https://docs.aws.amazon.com/AmazonCloudWatch/latest/monitoring/cloudwatch_concepts.html#dimension-combinations">Dimension Combinations</a> in the <i>Amazon
     * CloudWatch User Guide</i>. For more information about specifying dimensions, see <a href="https://docs.aws.amazon.com/AmazonCloudWatch/latest/monitoring/publishingMetrics.html">Publishing Metrics</a> in the
     * <i>Amazon CloudWatch User Guide</i>.
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
     * The time stamp that determines the first data point to return. Start times are evaluated relative to the time
     * that CloudWatch receives the request.
     *
     * <p>The value specified is inclusive; results include data points with the specified time stamp. In a raw HTTP
     * query, the time stamp must be in ISO 8601 UTC format (for example, 2016-10-03T23:00:00Z).
     *
     * <p>CloudWatch rounds the specified time stamp as follows:
     *
     * <ul>
     *   <li>
     *     Start time less than 15 days ago - Round down to the nearest whole minute. For example, 12:32:34 is
     *     rounded down to 12:32:00.
     *   </li>
     *   <li>
     *     Start time between 15 and 63 days ago - Round down to the nearest 5-minute clock interval. For example,
     *     12:32:34 is rounded down to 12:30:00.
     *   </li>
     *   <li>
     *     Start time greater than 63 days ago - Round down to the nearest 1-hour clock interval. For example,
     *     12:32:34 is rounded down to 12:00:00.
     *   </li>
     * </ul>
     *
     * <p>If you set <code>Period</code> to 5, 10, 20, or 30, the start time of your request is rounded down to the
     * nearest time that corresponds to even 5-, 10-, 20-, or 30-second divisions of a minute. For example, if you make
     * a query at (HH:mm:ss) 01:05:23 for the previous 10-second period, the start time of your request is rounded down
     * and you receive data from 01:05:10 to 01:05:20. If you make a query at 15:07:17 for the previous 5 minutes of
     * data, using a period of 5 seconds, you receive data timestamped between 15:02:15 and 15:07:15.
     */
    public Instant getStartTime() {
        return startTime;
    }

    /**
     * The time stamp that determines the last data point to return.
     *
     * <p>The value specified is exclusive; results include data points up to the specified time stamp. In a raw HTTP
     * query, the time stamp must be in ISO 8601 UTC format (for example, 2016-10-10T23:00:00Z).
     */
    public Instant getEndTime() {
        return endTime;
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
     * The metric statistics, other than percentile. For percentile statistics, use <code>ExtendedStatistics</code>.
     * When calling <code>GetMetricStatistics</code>, you must specify either <code>Statistics</code> or
     * <code>ExtendedStatistics</code>, but not both.
     */
    public List<Statistic> getStatistics() {
        if (statistics == null) {
            return Collections.emptyList();
        }
        return statistics;
    }

    public boolean hasStatistics() {
        return statistics != null;
    }

    /**
     * The percentile statistics. Specify values between p0.0 and p100. When calling <code>GetMetricStatistics</code>,
     * you must specify either <code>Statistics</code> or <code>ExtendedStatistics</code>, but not both. Percentile
     * statistics are not available for metrics when any of the metric values are negative numbers.
     */
    public List<String> getExtendedStatistics() {
        if (extendedStatistics == null) {
            return Collections.emptyList();
        }
        return extendedStatistics;
    }

    public boolean hasExtendedStatistics() {
        return extendedStatistics != null;
    }

    /**
     * The unit for a given metric. If you omit <code>Unit</code>, all data that was collected with any unit is
     * returned, along with the corresponding units that were specified when the data was reported to CloudWatch. If you
     * specify a unit, the operation returns only data that was collected with that unit specified. If you specify a
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
        GetMetricStatisticsInput that = (GetMetricStatisticsInput) other;
        return Objects.equals(this.period, that.period)
               && Objects.equals(this.namespace, that.namespace)
               && Objects.equals(this.metricName, that.metricName)
               && Objects.equals(this.unit, that.unit)
               && Objects.equals(this.startTime, that.startTime)
               && Objects.equals(this.endTime, that.endTime)
               && Objects.equals(this.dimensions, that.dimensions)
               && Objects.equals(this.statistics, that.statistics)
               && Objects.equals(this.extendedStatistics, that.extendedStatistics);
    }

    @Override
    public int hashCode() {
        int $hc = Objects.hashCode(namespace);
        $hc = 31 * $hc + Objects.hashCode(metricName);
        $hc = 31 * $hc + Objects.hashCode(dimensions);
        $hc = 31 * $hc + Objects.hashCode(startTime);
        $hc = 31 * $hc + Objects.hashCode(endTime);
        $hc = 31 * $hc + Objects.hashCode(period);
        $hc = 31 * $hc + Objects.hashCode(statistics);
        $hc = 31 * $hc + Objects.hashCode(extendedStatistics);
        $hc = 31 * $hc + Objects.hashCode(unit);
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
        if (dimensions != null) {
            serializer.writeList($SCHEMA_DIMENSIONS, dimensions, dimensions.size(), SharedSerde.DimensionsSerializer.INSTANCE);
        }
        if (startTime != null) {
            serializer.writeTimestamp($SCHEMA_START_TIME, startTime);
        }
        if (endTime != null) {
            serializer.writeTimestamp($SCHEMA_END_TIME, endTime);
        }
        if (period != null) {
            serializer.writeInteger($SCHEMA_PERIOD, period);
        }
        if (statistics != null) {
            serializer.writeList($SCHEMA_STATISTICS, statistics, statistics.size(), SharedSerde.StatisticsSerializer.INSTANCE);
        }
        if (extendedStatistics != null) {
            serializer.writeList($SCHEMA_EXTENDED_STATISTICS, extendedStatistics, extendedStatistics.size(), SharedSerde.ExtendedStatisticsSerializer.INSTANCE);
        }
        if (unit != null) {
            serializer.writeString($SCHEMA_UNIT, unit.getValue());
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T getMemberValue(Schema member) {
        return switch (member.memberIndex()) {
            case 0 -> (T) SchemaUtils.validateSameMember($SCHEMA_NAMESPACE, member, namespace);
            case 1 -> (T) SchemaUtils.validateSameMember($SCHEMA_METRIC_NAME, member, metricName);
            case 2 -> (T) SchemaUtils.validateSameMember($SCHEMA_START_TIME, member, startTime);
            case 3 -> (T) SchemaUtils.validateSameMember($SCHEMA_END_TIME, member, endTime);
            case 4 -> (T) SchemaUtils.validateSameMember($SCHEMA_PERIOD, member, period);
            case 5 -> (T) SchemaUtils.validateSameMember($SCHEMA_DIMENSIONS, member, dimensions);
            case 6 -> (T) SchemaUtils.validateSameMember($SCHEMA_STATISTICS, member, statistics);
            case 7 -> (T) SchemaUtils.validateSameMember($SCHEMA_EXTENDED_STATISTICS, member, extendedStatistics);
            case 8 -> (T) SchemaUtils.validateSameMember($SCHEMA_UNIT, member, unit);
            default -> throw new IllegalArgumentException("Attempted to get non-existent member: " + member.id());
        };
    }

    /**
     * Create a new builder containing all the current property values of this object.
     *
     * <p><strong>Note:</strong> This method performs only a shallow copy of the original properties.
     *
     * @return a builder for {@link GetMetricStatisticsInput}.
     */
    public Builder toBuilder() {
        var builder = new Builder();
        builder.namespace(this.namespace);
        builder.metricName(this.metricName);
        builder.dimensions(this.dimensions);
        builder.startTime(this.startTime);
        builder.endTime(this.endTime);
        builder.period(this.period);
        builder.statistics(this.statistics);
        builder.extendedStatistics(this.extendedStatistics);
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
     * Builder for {@link GetMetricStatisticsInput}.
     */
    public static final class Builder implements ShapeBuilder<GetMetricStatisticsInput> {
        private final PresenceTracker tracker = PresenceTracker.of($SCHEMA);
        private String namespace;
        private String metricName;
        private List<Dimension> dimensions;
        private Instant startTime;
        private Instant endTime;
        private Integer period;
        private List<Statistic> statistics;
        private List<String> extendedStatistics;
        private StandardUnit unit;

        private Builder() {
            // Tell the tracker to assume clientOptional members are present.
            tracker.setMember($SCHEMA_NAMESPACE);
            tracker.setMember($SCHEMA_METRIC_NAME);
            tracker.setMember($SCHEMA_START_TIME);
            tracker.setMember($SCHEMA_END_TIME);
            tracker.setMember($SCHEMA_PERIOD);
        }

        @Override
        public Schema schema() {
            return $SCHEMA;
        }

        /**
         * The namespace of the metric, with or without spaces.
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
         * The name of the metric, with or without spaces.
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
         * The dimensions. If the metric contains multiple dimensions, you must include a value for each dimension.
         * CloudWatch treats each unique combination of dimensions as a separate metric. If a specific combination of
         * dimensions was not published, you can't retrieve statistics for it. You must specify the same dimensions that
         * were used when the metrics were created. For an example, see <a href="https://docs.aws.amazon.com/AmazonCloudWatch/latest/monitoring/cloudwatch_concepts.html#dimension-combinations">Dimension Combinations</a> in the <i>Amazon
         * CloudWatch User Guide</i>. For more information about specifying dimensions, see <a href="https://docs.aws.amazon.com/AmazonCloudWatch/latest/monitoring/publishingMetrics.html">Publishing Metrics</a> in the
         * <i>Amazon CloudWatch User Guide</i>.
         *
         * @return this builder.
         */
        public Builder dimensions(List<Dimension> dimensions) {
            this.dimensions = dimensions;
            return this;
        }

        /**
         * The time stamp that determines the first data point to return. Start times are evaluated relative to the time
         * that CloudWatch receives the request.
         *
         * <p>The value specified is inclusive; results include data points with the specified time stamp. In a raw HTTP
         * query, the time stamp must be in ISO 8601 UTC format (for example, 2016-10-03T23:00:00Z).
         *
         * <p>CloudWatch rounds the specified time stamp as follows:
         *
         * <ul>
         *   <li>
         *     Start time less than 15 days ago - Round down to the nearest whole minute. For example, 12:32:34 is
         *     rounded down to 12:32:00.
         *   </li>
         *   <li>
         *     Start time between 15 and 63 days ago - Round down to the nearest 5-minute clock interval. For example,
         *     12:32:34 is rounded down to 12:30:00.
         *   </li>
         *   <li>
         *     Start time greater than 63 days ago - Round down to the nearest 1-hour clock interval. For example,
         *     12:32:34 is rounded down to 12:00:00.
         *   </li>
         * </ul>
         *
         * <p>If you set <code>Period</code> to 5, 10, 20, or 30, the start time of your request is rounded down to the
         * nearest time that corresponds to even 5-, 10-, 20-, or 30-second divisions of a minute. For example, if you make
         * a query at (HH:mm:ss) 01:05:23 for the previous 10-second period, the start time of your request is rounded down
         * and you receive data from 01:05:10 to 01:05:20. If you make a query at 15:07:17 for the previous 5 minutes of
         * data, using a period of 5 seconds, you receive data timestamped between 15:02:15 and 15:07:15.
         *
         * <p><strong>Required</strong>
         * @return this builder.
         */
        public Builder startTime(Instant startTime) {
            this.startTime = Objects.requireNonNull(startTime, "startTime cannot be null");
            tracker.setMember($SCHEMA_START_TIME);
            return this;
        }

        /**
         * The time stamp that determines the last data point to return.
         *
         * <p>The value specified is exclusive; results include data points up to the specified time stamp. In a raw HTTP
         * query, the time stamp must be in ISO 8601 UTC format (for example, 2016-10-10T23:00:00Z).
         *
         * <p><strong>Required</strong>
         * @return this builder.
         */
        public Builder endTime(Instant endTime) {
            this.endTime = Objects.requireNonNull(endTime, "endTime cannot be null");
            tracker.setMember($SCHEMA_END_TIME);
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
         * The metric statistics, other than percentile. For percentile statistics, use <code>ExtendedStatistics</code>.
         * When calling <code>GetMetricStatistics</code>, you must specify either <code>Statistics</code> or
         * <code>ExtendedStatistics</code>, but not both.
         *
         * @return this builder.
         */
        public Builder statistics(List<Statistic> statistics) {
            this.statistics = statistics;
            return this;
        }

        /**
         * The percentile statistics. Specify values between p0.0 and p100. When calling <code>GetMetricStatistics</code>,
         * you must specify either <code>Statistics</code> or <code>ExtendedStatistics</code>, but not both. Percentile
         * statistics are not available for metrics when any of the metric values are negative numbers.
         *
         * @return this builder.
         */
        public Builder extendedStatistics(List<String> extendedStatistics) {
            this.extendedStatistics = extendedStatistics;
            return this;
        }

        /**
         * The unit for a given metric. If you omit <code>Unit</code>, all data that was collected with any unit is
         * returned, along with the corresponding units that were specified when the data was reported to CloudWatch. If you
         * specify a unit, the operation returns only data that was collected with that unit specified. If you specify a
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
        public GetMetricStatisticsInput build() {
            tracker.validate();
            return new GetMetricStatisticsInput(this);
        }

        @Override
        @SuppressWarnings("unchecked")
        public void setMemberValue(Schema member, Object value) {
            switch (member.memberIndex()) {
                case 0 -> namespace((String) SchemaUtils.validateSameMember($SCHEMA_NAMESPACE, member, value));
                case 1 -> metricName((String) SchemaUtils.validateSameMember($SCHEMA_METRIC_NAME, member, value));
                case 2 -> startTime((Instant) SchemaUtils.validateSameMember($SCHEMA_START_TIME, member, value));
                case 3 -> endTime((Instant) SchemaUtils.validateSameMember($SCHEMA_END_TIME, member, value));
                case 4 -> period((Integer) SchemaUtils.validateSameMember($SCHEMA_PERIOD, member, value));
                case 5 -> dimensions((List<Dimension>) SchemaUtils.validateSameMember($SCHEMA_DIMENSIONS, member, value));
                case 6 -> statistics((List<Statistic>) SchemaUtils.validateSameMember($SCHEMA_STATISTICS, member, value));
                case 7 -> extendedStatistics((List<String>) SchemaUtils.validateSameMember($SCHEMA_EXTENDED_STATISTICS, member, value));
                case 8 -> unit((StandardUnit) SchemaUtils.validateSameMember($SCHEMA_UNIT, member, value));
                default -> ShapeBuilder.super.setMemberValue(member, value);
            }
        }

        @Override
        public ShapeBuilder<GetMetricStatisticsInput> errorCorrection() {
            if (tracker.allSet()) {
                return this;
            }
            if (!tracker.checkMember($SCHEMA_NAMESPACE)) {
                namespace("");
            }
            if (!tracker.checkMember($SCHEMA_METRIC_NAME)) {
                metricName("");
            }
            if (!tracker.checkMember($SCHEMA_START_TIME)) {
                startTime(Instant.EPOCH);
            }
            if (!tracker.checkMember($SCHEMA_END_TIME)) {
                endTime(Instant.EPOCH);
            }
            if (!tracker.checkMember($SCHEMA_PERIOD)) {
                tracker.setMember($SCHEMA_PERIOD);
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
                    case 2 -> builder.startTime(de.readTimestamp(member));
                    case 3 -> builder.endTime(de.readTimestamp(member));
                    case 4 -> builder.period(de.readInteger(member));
                    case 5 -> builder.dimensions(SharedSerde.deserializeDimensions(member, de));
                    case 6 -> builder.statistics(SharedSerde.deserializeStatistics(member, de));
                    case 7 -> builder.extendedStatistics(SharedSerde.deserializeExtendedStatistics(member, de));
                    case 8 -> builder.unit(StandardUnit.builder().deserializeMember(de, member).build());
                    default -> throw new IllegalArgumentException("Unexpected member: " + member.memberName());
                }
            }
        }
    }
}
