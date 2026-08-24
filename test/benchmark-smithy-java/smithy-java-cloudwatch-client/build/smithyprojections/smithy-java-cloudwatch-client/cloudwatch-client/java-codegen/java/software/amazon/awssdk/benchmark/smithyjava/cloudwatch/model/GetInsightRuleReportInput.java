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
public final class GetInsightRuleReportInput implements SerializableStruct {

    public static final Schema $SCHEMA = Schemas.GET_INSIGHT_RULE_REPORT_INPUT;
    private static final Schema $SCHEMA_RULE_NAME = $SCHEMA.member("RuleName");
    private static final Schema $SCHEMA_START_TIME = $SCHEMA.member("StartTime");
    private static final Schema $SCHEMA_END_TIME = $SCHEMA.member("EndTime");
    private static final Schema $SCHEMA_PERIOD = $SCHEMA.member("Period");
    private static final Schema $SCHEMA_MAX_CONTRIBUTOR_COUNT = $SCHEMA.member("MaxContributorCount");
    private static final Schema $SCHEMA_METRICS = $SCHEMA.member("Metrics");
    private static final Schema $SCHEMA_ORDER_BY = $SCHEMA.member("OrderBy");

    public static final ShapeId $ID = $SCHEMA.id();

    private final transient String ruleName;
    private final transient Instant startTime;
    private final transient Instant endTime;
    private final transient Integer period;
    private final transient Integer maxContributorCount;
    private final transient List<String> metrics;
    private final transient String orderBy;

    private GetInsightRuleReportInput(Builder builder) {
        this.ruleName = builder.ruleName;
        this.startTime = builder.startTime;
        this.endTime = builder.endTime;
        this.period = builder.period;
        this.maxContributorCount = builder.maxContributorCount;
        this.metrics = builder.metrics == null ? null : Collections.unmodifiableList(builder.metrics);
        this.orderBy = builder.orderBy;
    }

    /**
     * The name of the rule that you want to see data from.
     */
    public String getRuleName() {
        return ruleName;
    }

    /**
     * The start time of the data to use in the report. When used in a raw HTTP Query API, it is formatted as
     * <code>yyyy-MM-dd'T'HH:mm:ss</code>. For example, <code>2019-07-01T23:59:59</code>.
     */
    public Instant getStartTime() {
        return startTime;
    }

    /**
     * The end time of the data to use in the report. When used in a raw HTTP Query API, it is formatted as
     * <code>yyyy-MM-dd'T'HH:mm:ss</code>. For example, <code>2019-07-01T23:59:59</code>.
     */
    public Instant getEndTime() {
        return endTime;
    }

    /**
     * The period, in seconds, to use for the statistics in the <code>InsightRuleMetricDatapoint</code> results.
     */
    public Integer getPeriod() {
        return period;
    }

    /**
     * The maximum number of contributors to include in the report. The range is 1 to 100. If you omit this, the default
     * of 10 is used.
     */
    public Integer getMaxContributorCount() {
        return maxContributorCount;
    }

    /**
     * Specifies which metrics to use for aggregation of contributor values for the report. You can specify one or more
     * of the following metrics:
     *
     * <ul>
     *   <li>
     *     <code>UniqueContributors</code> -- the number of unique contributors for each data point.
     *   </li>
     *   <li>
     *     <code>MaxContributorValue</code> -- the value of the top contributor for each data point. The identity of
     *     the contributor might change for each data point in the graph.If this rule aggregates by COUNT, the top
     *     contributor for each data point is the contributor with the most occurrences in that period. If the rule
     *     aggregates by SUM, the top contributor is the contributor with the highest sum in the log field specified
     *     by the rule's <code>Value</code>, during that period.
     *   </li>
     *   <li>
     *     <code>SampleCount</code> -- the number of data points matched by the rule.
     *   </li>
     *   <li>
     *     <code>Sum</code> -- the sum of the values from all contributors during the time period represented by
     *     that data point.
     *   </li>
     *   <li>
     *     <code>Minimum</code> -- the minimum value from a single observation during the time period represented by
     *     that data point.
     *   </li>
     *   <li>
     *     <code>Maximum</code> -- the maximum value from a single observation during the time period represented by
     *     that data point.
     *   </li>
     *   <li>
     *     <code>Average</code> -- the average value from all contributors during the time period represented by
     *     that data point.
     *   </li>
     * </ul>
     */
    public List<String> getMetrics() {
        if (metrics == null) {
            return Collections.emptyList();
        }
        return metrics;
    }

    public boolean hasMetrics() {
        return metrics != null;
    }

    /**
     * Determines what statistic to use to rank the contributors. Valid values are <code>Sum</code> and
     * <code>Maximum</code>.
     */
    public String getOrderBy() {
        return orderBy;
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
        GetInsightRuleReportInput that = (GetInsightRuleReportInput) other;
        return Objects.equals(this.period, that.period)
               && Objects.equals(this.maxContributorCount, that.maxContributorCount)
               && Objects.equals(this.ruleName, that.ruleName)
               && Objects.equals(this.orderBy, that.orderBy)
               && Objects.equals(this.startTime, that.startTime)
               && Objects.equals(this.endTime, that.endTime)
               && Objects.equals(this.metrics, that.metrics);
    }

    @Override
    public int hashCode() {
        int $hc = Objects.hashCode(ruleName);
        $hc = 31 * $hc + Objects.hashCode(startTime);
        $hc = 31 * $hc + Objects.hashCode(endTime);
        $hc = 31 * $hc + Objects.hashCode(period);
        $hc = 31 * $hc + Objects.hashCode(maxContributorCount);
        $hc = 31 * $hc + Objects.hashCode(metrics);
        $hc = 31 * $hc + Objects.hashCode(orderBy);
        return $hc;
    }

    @Override
    public Schema schema() {
        return $SCHEMA;
    }

    @Override
    public void serializeMembers(ShapeSerializer serializer) {
        if (ruleName != null) {
            serializer.writeString($SCHEMA_RULE_NAME, ruleName);
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
        if (maxContributorCount != null) {
            serializer.writeInteger($SCHEMA_MAX_CONTRIBUTOR_COUNT, maxContributorCount);
        }
        if (metrics != null) {
            serializer.writeList($SCHEMA_METRICS, metrics, metrics.size(), SharedSerde.InsightRuleMetricListSerializer.INSTANCE);
        }
        if (orderBy != null) {
            serializer.writeString($SCHEMA_ORDER_BY, orderBy);
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T getMemberValue(Schema member) {
        return switch (member.memberIndex()) {
            case 0 -> (T) SchemaUtils.validateSameMember($SCHEMA_RULE_NAME, member, ruleName);
            case 1 -> (T) SchemaUtils.validateSameMember($SCHEMA_START_TIME, member, startTime);
            case 2 -> (T) SchemaUtils.validateSameMember($SCHEMA_END_TIME, member, endTime);
            case 3 -> (T) SchemaUtils.validateSameMember($SCHEMA_PERIOD, member, period);
            case 4 -> (T) SchemaUtils.validateSameMember($SCHEMA_MAX_CONTRIBUTOR_COUNT, member, maxContributorCount);
            case 5 -> (T) SchemaUtils.validateSameMember($SCHEMA_METRICS, member, metrics);
            case 6 -> (T) SchemaUtils.validateSameMember($SCHEMA_ORDER_BY, member, orderBy);
            default -> throw new IllegalArgumentException("Attempted to get non-existent member: " + member.id());
        };
    }

    /**
     * Create a new builder containing all the current property values of this object.
     *
     * <p><strong>Note:</strong> This method performs only a shallow copy of the original properties.
     *
     * @return a builder for {@link GetInsightRuleReportInput}.
     */
    public Builder toBuilder() {
        var builder = new Builder();
        builder.ruleName(this.ruleName);
        builder.startTime(this.startTime);
        builder.endTime(this.endTime);
        builder.period(this.period);
        builder.maxContributorCount(this.maxContributorCount);
        builder.metrics(this.metrics);
        builder.orderBy(this.orderBy);
        return builder;
    }

    /**
     * @return returns a new Builder.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for {@link GetInsightRuleReportInput}.
     */
    public static final class Builder implements ShapeBuilder<GetInsightRuleReportInput> {
        private final PresenceTracker tracker = PresenceTracker.of($SCHEMA);
        private String ruleName;
        private Instant startTime;
        private Instant endTime;
        private Integer period;
        private Integer maxContributorCount;
        private List<String> metrics;
        private String orderBy;

        private Builder() {
            // Tell the tracker to assume clientOptional members are present.
            tracker.setMember($SCHEMA_RULE_NAME);
            tracker.setMember($SCHEMA_START_TIME);
            tracker.setMember($SCHEMA_END_TIME);
            tracker.setMember($SCHEMA_PERIOD);
        }

        @Override
        public Schema schema() {
            return $SCHEMA;
        }

        /**
         * The name of the rule that you want to see data from.
         *
         * <p><strong>Required</strong>
         * @return this builder.
         */
        public Builder ruleName(String ruleName) {
            this.ruleName = Objects.requireNonNull(ruleName, "ruleName cannot be null");
            tracker.setMember($SCHEMA_RULE_NAME);
            return this;
        }

        /**
         * The start time of the data to use in the report. When used in a raw HTTP Query API, it is formatted as
         * <code>yyyy-MM-dd'T'HH:mm:ss</code>. For example, <code>2019-07-01T23:59:59</code>.
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
         * The end time of the data to use in the report. When used in a raw HTTP Query API, it is formatted as
         * <code>yyyy-MM-dd'T'HH:mm:ss</code>. For example, <code>2019-07-01T23:59:59</code>.
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
         * The period, in seconds, to use for the statistics in the <code>InsightRuleMetricDatapoint</code> results.
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
         * The maximum number of contributors to include in the report. The range is 1 to 100. If you omit this, the default
         * of 10 is used.
         *
         * @return this builder.
         */
        public Builder maxContributorCount(Integer maxContributorCount) {
            this.maxContributorCount = maxContributorCount;
            return this;
        }

        /**
         * Specifies which metrics to use for aggregation of contributor values for the report. You can specify one or more
         * of the following metrics:
         *
         * <ul>
         *   <li>
         *     <code>UniqueContributors</code> -- the number of unique contributors for each data point.
         *   </li>
         *   <li>
         *     <code>MaxContributorValue</code> -- the value of the top contributor for each data point. The identity of
         *     the contributor might change for each data point in the graph.If this rule aggregates by COUNT, the top
         *     contributor for each data point is the contributor with the most occurrences in that period. If the rule
         *     aggregates by SUM, the top contributor is the contributor with the highest sum in the log field specified
         *     by the rule's <code>Value</code>, during that period.
         *   </li>
         *   <li>
         *     <code>SampleCount</code> -- the number of data points matched by the rule.
         *   </li>
         *   <li>
         *     <code>Sum</code> -- the sum of the values from all contributors during the time period represented by
         *     that data point.
         *   </li>
         *   <li>
         *     <code>Minimum</code> -- the minimum value from a single observation during the time period represented by
         *     that data point.
         *   </li>
         *   <li>
         *     <code>Maximum</code> -- the maximum value from a single observation during the time period represented by
         *     that data point.
         *   </li>
         *   <li>
         *     <code>Average</code> -- the average value from all contributors during the time period represented by
         *     that data point.
         *   </li>
         * </ul>
         *
         * @return this builder.
         */
        public Builder metrics(List<String> metrics) {
            this.metrics = metrics;
            return this;
        }

        /**
         * Determines what statistic to use to rank the contributors. Valid values are <code>Sum</code> and
         * <code>Maximum</code>.
         *
         * @return this builder.
         */
        public Builder orderBy(String orderBy) {
            this.orderBy = orderBy;
            return this;
        }

        @Override
        public GetInsightRuleReportInput build() {
            tracker.validate();
            return new GetInsightRuleReportInput(this);
        }

        @Override
        @SuppressWarnings("unchecked")
        public void setMemberValue(Schema member, Object value) {
            switch (member.memberIndex()) {
                case 0 -> ruleName((String) SchemaUtils.validateSameMember($SCHEMA_RULE_NAME, member, value));
                case 1 -> startTime((Instant) SchemaUtils.validateSameMember($SCHEMA_START_TIME, member, value));
                case 2 -> endTime((Instant) SchemaUtils.validateSameMember($SCHEMA_END_TIME, member, value));
                case 3 -> period((Integer) SchemaUtils.validateSameMember($SCHEMA_PERIOD, member, value));
                case 4 -> maxContributorCount((Integer) SchemaUtils.validateSameMember($SCHEMA_MAX_CONTRIBUTOR_COUNT, member, value));
                case 5 -> metrics((List<String>) SchemaUtils.validateSameMember($SCHEMA_METRICS, member, value));
                case 6 -> orderBy((String) SchemaUtils.validateSameMember($SCHEMA_ORDER_BY, member, value));
                default -> ShapeBuilder.super.setMemberValue(member, value);
            }
        }

        @Override
        public ShapeBuilder<GetInsightRuleReportInput> errorCorrection() {
            if (tracker.allSet()) {
                return this;
            }
            if (!tracker.checkMember($SCHEMA_RULE_NAME)) {
                ruleName("");
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
                    case 0 -> builder.ruleName(de.readString(member));
                    case 1 -> builder.startTime(de.readTimestamp(member));
                    case 2 -> builder.endTime(de.readTimestamp(member));
                    case 3 -> builder.period(de.readInteger(member));
                    case 4 -> builder.maxContributorCount(de.readInteger(member));
                    case 5 -> builder.metrics(SharedSerde.deserializeInsightRuleMetricList(member, de));
                    case 6 -> builder.orderBy(de.readString(member));
                    default -> throw new IllegalArgumentException("Unexpected member: " + member.memberName());
                }
            }
        }
    }
}
