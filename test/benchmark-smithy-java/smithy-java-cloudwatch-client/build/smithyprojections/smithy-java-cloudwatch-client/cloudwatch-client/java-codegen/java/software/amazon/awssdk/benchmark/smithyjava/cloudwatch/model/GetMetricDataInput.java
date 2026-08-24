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
public final class GetMetricDataInput implements SerializableStruct {

    public static final Schema $SCHEMA = Schemas.GET_METRIC_DATA_INPUT;
    private static final Schema $SCHEMA_METRIC_DATA_QUERIES = $SCHEMA.member("MetricDataQueries");
    private static final Schema $SCHEMA_START_TIME = $SCHEMA.member("StartTime");
    private static final Schema $SCHEMA_END_TIME = $SCHEMA.member("EndTime");
    private static final Schema $SCHEMA_NEXT_TOKEN = $SCHEMA.member("NextToken");
    private static final Schema $SCHEMA_SCAN_BY = $SCHEMA.member("ScanBy");
    private static final Schema $SCHEMA_MAX_DATAPOINTS = $SCHEMA.member("MaxDatapoints");
    private static final Schema $SCHEMA_LABEL_OPTIONS = $SCHEMA.member("LabelOptions");

    public static final ShapeId $ID = $SCHEMA.id();

    private final transient List<MetricDataQuery> metricDataQueries;
    private final transient Instant startTime;
    private final transient Instant endTime;
    private final transient String nextToken;
    private final transient ScanBy scanBy;
    private final transient Integer maxDatapoints;
    private final transient LabelOptions labelOptions;

    private GetMetricDataInput(Builder builder) {
        this.metricDataQueries = builder.metricDataQueries == null ? null : Collections.unmodifiableList(builder.metricDataQueries);
        this.startTime = builder.startTime;
        this.endTime = builder.endTime;
        this.nextToken = builder.nextToken;
        this.scanBy = builder.scanBy;
        this.maxDatapoints = builder.maxDatapoints;
        this.labelOptions = builder.labelOptions;
    }

    /**
     * The metric queries to be returned. A single <code>GetMetricData</code> call can include as many as 500
     * <code>MetricDataQuery</code> structures. Each of these structures can specify either a metric to retrieve, a
     * Metrics Insights query, or a math expression to perform on retrieved data.
     */
    public List<MetricDataQuery> getMetricDataQueries() {
        if (metricDataQueries == null) {
            return Collections.emptyList();
        }
        return metricDataQueries;
    }

    public boolean hasMetricDataQueries() {
        return metricDataQueries != null;
    }

    /**
     * The time stamp indicating the earliest data to be returned.
     *
     * <p>The value specified is inclusive; results include data points with the specified time stamp.
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
     * <p>For better performance, specify <code>StartTime</code> and <code>EndTime</code> values that align with the
     * value of the metric's <code>Period</code> and sync up with the beginning and end of an hour. For example, if the <code>
     * Period</code> of a metric is 5 minutes, specifying 12:05 or 12:30 as <code>StartTime</code> can get a faster
     * response from CloudWatch than setting 12:07 or 12:29 as the <code>StartTime</code>.
     */
    public Instant getStartTime() {
        return startTime;
    }

    /**
     * The time stamp indicating the latest data to be returned.
     *
     * <p>The value specified is exclusive; results include data points up to the specified time stamp.
     *
     * <p>For better performance, specify <code>StartTime</code> and <code>EndTime</code> values that align with the
     * value of the metric's <code>Period</code> and sync up with the beginning and end of an hour. For example, if the <code>
     * Period</code> of a metric is 5 minutes, specifying 12:05 or 12:30 as <code>EndTime</code> can get a faster
     * response from CloudWatch than setting 12:07 or 12:29 as the <code>EndTime</code>.
     */
    public Instant getEndTime() {
        return endTime;
    }

    /**
     * Include this value, if it was returned by the previous <code>GetMetricData</code> operation, to get the next set
     * of data points.
     */
    public String getNextToken() {
        return nextToken;
    }

    /**
     * The order in which data points should be returned. <code>TimestampDescending</code> returns the newest data first
     * and paginates when the <code>MaxDatapoints</code> limit is reached. <code>TimestampAscending</code> returns the
     * oldest data first and paginates when the <code>MaxDatapoints</code> limit is reached.
     *
     * <p>If you omit this parameter, the default of <code>TimestampDescending</code> is used.
     */
    public ScanBy getScanBy() {
        return scanBy;
    }

    /**
     * The maximum number of data points the request should return before paginating. If you omit this, the default of
     * 100,800 is used.
     */
    public Integer getMaxDatapoints() {
        return maxDatapoints;
    }

    /**
     * This structure includes the <code>Timezone</code> parameter, which you can use to specify your time zone so that
     * the labels of returned data display the correct time for your time zone.
     */
    public LabelOptions getLabelOptions() {
        return labelOptions;
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
        GetMetricDataInput that = (GetMetricDataInput) other;
        return Objects.equals(this.maxDatapoints, that.maxDatapoints)
               && Objects.equals(this.nextToken, that.nextToken)
               && Objects.equals(this.scanBy, that.scanBy)
               && Objects.equals(this.startTime, that.startTime)
               && Objects.equals(this.endTime, that.endTime)
               && Objects.equals(this.labelOptions, that.labelOptions)
               && Objects.equals(this.metricDataQueries, that.metricDataQueries);
    }

    @Override
    public int hashCode() {
        int $hc = Objects.hashCode(metricDataQueries);
        $hc = 31 * $hc + Objects.hashCode(startTime);
        $hc = 31 * $hc + Objects.hashCode(endTime);
        $hc = 31 * $hc + Objects.hashCode(nextToken);
        $hc = 31 * $hc + Objects.hashCode(scanBy);
        $hc = 31 * $hc + Objects.hashCode(maxDatapoints);
        $hc = 31 * $hc + Objects.hashCode(labelOptions);
        return $hc;
    }

    @Override
    public Schema schema() {
        return $SCHEMA;
    }

    @Override
    public void serializeMembers(ShapeSerializer serializer) {
        if (metricDataQueries != null) {
            serializer.writeList($SCHEMA_METRIC_DATA_QUERIES, metricDataQueries, metricDataQueries.size(), SharedSerde.MetricDataQueriesSerializer.INSTANCE);
        }
        if (startTime != null) {
            serializer.writeTimestamp($SCHEMA_START_TIME, startTime);
        }
        if (endTime != null) {
            serializer.writeTimestamp($SCHEMA_END_TIME, endTime);
        }
        if (nextToken != null) {
            serializer.writeString($SCHEMA_NEXT_TOKEN, nextToken);
        }
        if (scanBy != null) {
            serializer.writeString($SCHEMA_SCAN_BY, scanBy.getValue());
        }
        if (maxDatapoints != null) {
            serializer.writeInteger($SCHEMA_MAX_DATAPOINTS, maxDatapoints);
        }
        if (labelOptions != null) {
            serializer.writeStruct($SCHEMA_LABEL_OPTIONS, labelOptions);
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T getMemberValue(Schema member) {
        return switch (member.memberIndex()) {
            case 0 -> (T) SchemaUtils.validateSameMember($SCHEMA_METRIC_DATA_QUERIES, member, metricDataQueries);
            case 1 -> (T) SchemaUtils.validateSameMember($SCHEMA_START_TIME, member, startTime);
            case 2 -> (T) SchemaUtils.validateSameMember($SCHEMA_END_TIME, member, endTime);
            case 3 -> (T) SchemaUtils.validateSameMember($SCHEMA_NEXT_TOKEN, member, nextToken);
            case 4 -> (T) SchemaUtils.validateSameMember($SCHEMA_SCAN_BY, member, scanBy);
            case 5 -> (T) SchemaUtils.validateSameMember($SCHEMA_MAX_DATAPOINTS, member, maxDatapoints);
            case 6 -> (T) SchemaUtils.validateSameMember($SCHEMA_LABEL_OPTIONS, member, labelOptions);
            default -> throw new IllegalArgumentException("Attempted to get non-existent member: " + member.id());
        };
    }

    /**
     * Create a new builder containing all the current property values of this object.
     *
     * <p><strong>Note:</strong> This method performs only a shallow copy of the original properties.
     *
     * @return a builder for {@link GetMetricDataInput}.
     */
    public Builder toBuilder() {
        var builder = new Builder();
        builder.metricDataQueries(this.metricDataQueries);
        builder.startTime(this.startTime);
        builder.endTime(this.endTime);
        builder.nextToken(this.nextToken);
        builder.scanBy(this.scanBy);
        builder.maxDatapoints(this.maxDatapoints);
        builder.labelOptions(this.labelOptions);
        return builder;
    }

    /**
     * @return returns a new Builder.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for {@link GetMetricDataInput}.
     */
    public static final class Builder implements ShapeBuilder<GetMetricDataInput> {
        private final PresenceTracker tracker = PresenceTracker.of($SCHEMA);
        private List<MetricDataQuery> metricDataQueries;
        private Instant startTime;
        private Instant endTime;
        private String nextToken;
        private ScanBy scanBy;
        private Integer maxDatapoints;
        private LabelOptions labelOptions;

        private Builder() {
            // Tell the tracker to assume clientOptional members are present.
            tracker.setMember($SCHEMA_METRIC_DATA_QUERIES);
            tracker.setMember($SCHEMA_START_TIME);
            tracker.setMember($SCHEMA_END_TIME);
        }

        @Override
        public Schema schema() {
            return $SCHEMA;
        }

        /**
         * The metric queries to be returned. A single <code>GetMetricData</code> call can include as many as 500
         * <code>MetricDataQuery</code> structures. Each of these structures can specify either a metric to retrieve, a
         * Metrics Insights query, or a math expression to perform on retrieved data.
         *
         * <p><strong>Required</strong>
         * @return this builder.
         */
        public Builder metricDataQueries(List<MetricDataQuery> metricDataQueries) {
            this.metricDataQueries = Objects.requireNonNull(metricDataQueries, "metricDataQueries cannot be null");
            tracker.setMember($SCHEMA_METRIC_DATA_QUERIES);
            return this;
        }

        /**
         * The time stamp indicating the earliest data to be returned.
         *
         * <p>The value specified is inclusive; results include data points with the specified time stamp.
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
         * <p>For better performance, specify <code>StartTime</code> and <code>EndTime</code> values that align with the
         * value of the metric's <code>Period</code> and sync up with the beginning and end of an hour. For example, if the <code>
         * Period</code> of a metric is 5 minutes, specifying 12:05 or 12:30 as <code>StartTime</code> can get a faster
         * response from CloudWatch than setting 12:07 or 12:29 as the <code>StartTime</code>.
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
         * The time stamp indicating the latest data to be returned.
         *
         * <p>The value specified is exclusive; results include data points up to the specified time stamp.
         *
         * <p>For better performance, specify <code>StartTime</code> and <code>EndTime</code> values that align with the
         * value of the metric's <code>Period</code> and sync up with the beginning and end of an hour. For example, if the <code>
         * Period</code> of a metric is 5 minutes, specifying 12:05 or 12:30 as <code>EndTime</code> can get a faster
         * response from CloudWatch than setting 12:07 or 12:29 as the <code>EndTime</code>.
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
         * Include this value, if it was returned by the previous <code>GetMetricData</code> operation, to get the next set
         * of data points.
         *
         * @return this builder.
         */
        public Builder nextToken(String nextToken) {
            this.nextToken = nextToken;
            return this;
        }

        /**
         * The order in which data points should be returned. <code>TimestampDescending</code> returns the newest data first
         * and paginates when the <code>MaxDatapoints</code> limit is reached. <code>TimestampAscending</code> returns the
         * oldest data first and paginates when the <code>MaxDatapoints</code> limit is reached.
         *
         * <p>If you omit this parameter, the default of <code>TimestampDescending</code> is used.
         *
         * @return this builder.
         */
        public Builder scanBy(ScanBy scanBy) {
            this.scanBy = scanBy;
            return this;
        }

        /**
         * The maximum number of data points the request should return before paginating. If you omit this, the default of
         * 100,800 is used.
         *
         * @return this builder.
         */
        public Builder maxDatapoints(Integer maxDatapoints) {
            this.maxDatapoints = maxDatapoints;
            return this;
        }

        /**
         * This structure includes the <code>Timezone</code> parameter, which you can use to specify your time zone so that
         * the labels of returned data display the correct time for your time zone.
         *
         * @return this builder.
         */
        public Builder labelOptions(LabelOptions labelOptions) {
            this.labelOptions = labelOptions;
            return this;
        }

        @Override
        public GetMetricDataInput build() {
            tracker.validate();
            return new GetMetricDataInput(this);
        }

        @Override
        @SuppressWarnings("unchecked")
        public void setMemberValue(Schema member, Object value) {
            switch (member.memberIndex()) {
                case 0 -> metricDataQueries((List<MetricDataQuery>) SchemaUtils.validateSameMember($SCHEMA_METRIC_DATA_QUERIES, member, value));
                case 1 -> startTime((Instant) SchemaUtils.validateSameMember($SCHEMA_START_TIME, member, value));
                case 2 -> endTime((Instant) SchemaUtils.validateSameMember($SCHEMA_END_TIME, member, value));
                case 3 -> nextToken((String) SchemaUtils.validateSameMember($SCHEMA_NEXT_TOKEN, member, value));
                case 4 -> scanBy((ScanBy) SchemaUtils.validateSameMember($SCHEMA_SCAN_BY, member, value));
                case 5 -> maxDatapoints((Integer) SchemaUtils.validateSameMember($SCHEMA_MAX_DATAPOINTS, member, value));
                case 6 -> labelOptions((LabelOptions) SchemaUtils.validateSameMember($SCHEMA_LABEL_OPTIONS, member, value));
                default -> ShapeBuilder.super.setMemberValue(member, value);
            }
        }

        @Override
        public ShapeBuilder<GetMetricDataInput> errorCorrection() {
            if (tracker.allSet()) {
                return this;
            }
            if (!tracker.checkMember($SCHEMA_METRIC_DATA_QUERIES)) {
                metricDataQueries(Collections.emptyList());
            }
            if (!tracker.checkMember($SCHEMA_START_TIME)) {
                startTime(Instant.EPOCH);
            }
            if (!tracker.checkMember($SCHEMA_END_TIME)) {
                endTime(Instant.EPOCH);
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
                    case 0 -> builder.metricDataQueries(SharedSerde.deserializeMetricDataQueries(member, de));
                    case 1 -> builder.startTime(de.readTimestamp(member));
                    case 2 -> builder.endTime(de.readTimestamp(member));
                    case 3 -> builder.nextToken(de.readString(member));
                    case 4 -> builder.scanBy(ScanBy.builder().deserializeMember(de, member).build());
                    case 5 -> builder.maxDatapoints(de.readInteger(member));
                    case 6 -> builder.labelOptions(LabelOptions.builder().deserializeMember(de, member).build());
                    default -> throw new IllegalArgumentException("Unexpected member: " + member.memberName());
                }
            }
        }
    }
}
