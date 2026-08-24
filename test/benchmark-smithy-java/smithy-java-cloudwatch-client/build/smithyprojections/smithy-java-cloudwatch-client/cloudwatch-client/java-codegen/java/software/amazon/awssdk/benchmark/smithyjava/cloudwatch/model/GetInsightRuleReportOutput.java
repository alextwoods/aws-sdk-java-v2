package software.amazon.awssdk.benchmark.smithyjava.cloudwatch.model;

import java.util.Collections;
import java.util.List;
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

@SmithyGenerated
public final class GetInsightRuleReportOutput implements SerializableStruct {

    public static final Schema $SCHEMA = Schemas.GET_INSIGHT_RULE_REPORT_OUTPUT;
    private static final Schema $SCHEMA_KEY_LABELS = $SCHEMA.member("KeyLabels");
    private static final Schema $SCHEMA_AGGREGATION_STATISTIC = $SCHEMA.member("AggregationStatistic");
    private static final Schema $SCHEMA_AGGREGATE_VALUE = $SCHEMA.member("AggregateValue");
    private static final Schema $SCHEMA_APPROXIMATE_UNIQUE_COUNT = $SCHEMA.member("ApproximateUniqueCount");
    private static final Schema $SCHEMA_CONTRIBUTORS = $SCHEMA.member("Contributors");
    private static final Schema $SCHEMA_METRIC_DATAPOINTS = $SCHEMA.member("MetricDatapoints");

    public static final ShapeId $ID = $SCHEMA.id();

    private final transient List<String> keyLabels;
    private final transient String aggregationStatistic;
    private final transient Double aggregateValue;
    private final transient Long approximateUniqueCount;
    private final transient List<InsightRuleContributor> contributors;
    private final transient List<InsightRuleMetricDatapoint> metricDatapoints;

    private GetInsightRuleReportOutput(Builder builder) {
        this.keyLabels = builder.keyLabels == null ? null : Collections.unmodifiableList(builder.keyLabels);
        this.aggregationStatistic = builder.aggregationStatistic;
        this.aggregateValue = builder.aggregateValue;
        this.approximateUniqueCount = builder.approximateUniqueCount;
        this.contributors = builder.contributors == null ? null : Collections.unmodifiableList(builder.contributors);
        this.metricDatapoints = builder.metricDatapoints == null ? null : Collections.unmodifiableList(builder.metricDatapoints);
    }

    /**
     * An array of the strings used as the keys for this rule. The keys are the dimensions used to classify
     * contributors. If the rule contains more than one key, then each unique combination of values for the keys is
     * counted as a unique contributor.
     */
    public List<String> getKeyLabels() {
        if (keyLabels == null) {
            return Collections.emptyList();
        }
        return keyLabels;
    }

    public boolean hasKeyLabels() {
        return keyLabels != null;
    }

    /**
     * Specifies whether this rule aggregates contributor data by COUNT or SUM.
     */
    public String getAggregationStatistic() {
        return aggregationStatistic;
    }

    /**
     * The sum of the values from all individual contributors that match the rule.
     */
    public Double getAggregateValue() {
        return aggregateValue;
    }

    /**
     * An approximate count of the unique contributors found by this rule in this time period.
     */
    public Long getApproximateUniqueCount() {
        return approximateUniqueCount;
    }

    /**
     * An array of the unique contributors found by this rule in this time period. If the rule contains multiple keys,
     * each combination of values for the keys counts as a unique contributor.
     */
    public List<InsightRuleContributor> getContributors() {
        if (contributors == null) {
            return Collections.emptyList();
        }
        return contributors;
    }

    public boolean hasContributors() {
        return contributors != null;
    }

    /**
     * A time series of metric data points that matches the time period in the rule request.
     */
    public List<InsightRuleMetricDatapoint> getMetricDatapoints() {
        if (metricDatapoints == null) {
            return Collections.emptyList();
        }
        return metricDatapoints;
    }

    public boolean hasMetricDatapoints() {
        return metricDatapoints != null;
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
        GetInsightRuleReportOutput that = (GetInsightRuleReportOutput) other;
        return Objects.equals(this.approximateUniqueCount, that.approximateUniqueCount)
               && Objects.equals(this.aggregateValue, that.aggregateValue)
               && Objects.equals(this.aggregationStatistic, that.aggregationStatistic)
               && Objects.equals(this.keyLabels, that.keyLabels)
               && Objects.equals(this.contributors, that.contributors)
               && Objects.equals(this.metricDatapoints, that.metricDatapoints);
    }

    @Override
    public int hashCode() {
        int $hc = Objects.hashCode(keyLabels);
        $hc = 31 * $hc + Objects.hashCode(aggregationStatistic);
        $hc = 31 * $hc + Objects.hashCode(aggregateValue);
        $hc = 31 * $hc + Objects.hashCode(approximateUniqueCount);
        $hc = 31 * $hc + Objects.hashCode(contributors);
        $hc = 31 * $hc + Objects.hashCode(metricDatapoints);
        return $hc;
    }

    @Override
    public Schema schema() {
        return $SCHEMA;
    }

    @Override
    public void serializeMembers(ShapeSerializer serializer) {
        if (keyLabels != null) {
            serializer.writeList($SCHEMA_KEY_LABELS, keyLabels, keyLabels.size(), SharedSerde.InsightRuleContributorKeyLabelsSerializer.INSTANCE);
        }
        if (aggregationStatistic != null) {
            serializer.writeString($SCHEMA_AGGREGATION_STATISTIC, aggregationStatistic);
        }
        if (aggregateValue != null) {
            serializer.writeDouble($SCHEMA_AGGREGATE_VALUE, aggregateValue);
        }
        if (approximateUniqueCount != null) {
            serializer.writeLong($SCHEMA_APPROXIMATE_UNIQUE_COUNT, approximateUniqueCount);
        }
        if (contributors != null) {
            serializer.writeList($SCHEMA_CONTRIBUTORS, contributors, contributors.size(), SharedSerde.InsightRuleContributorsSerializer.INSTANCE);
        }
        if (metricDatapoints != null) {
            serializer.writeList($SCHEMA_METRIC_DATAPOINTS, metricDatapoints, metricDatapoints.size(), SharedSerde.InsightRuleMetricDatapointsSerializer.INSTANCE);
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T getMemberValue(Schema member) {
        return switch (member.memberIndex()) {
            case 0 -> (T) SchemaUtils.validateSameMember($SCHEMA_KEY_LABELS, member, keyLabels);
            case 1 -> (T) SchemaUtils.validateSameMember($SCHEMA_AGGREGATION_STATISTIC, member, aggregationStatistic);
            case 2 -> (T) SchemaUtils.validateSameMember($SCHEMA_AGGREGATE_VALUE, member, aggregateValue);
            case 3 -> (T) SchemaUtils.validateSameMember($SCHEMA_APPROXIMATE_UNIQUE_COUNT, member, approximateUniqueCount);
            case 4 -> (T) SchemaUtils.validateSameMember($SCHEMA_CONTRIBUTORS, member, contributors);
            case 5 -> (T) SchemaUtils.validateSameMember($SCHEMA_METRIC_DATAPOINTS, member, metricDatapoints);
            default -> throw new IllegalArgumentException("Attempted to get non-existent member: " + member.id());
        };
    }

    /**
     * Create a new builder containing all the current property values of this object.
     *
     * <p><strong>Note:</strong> This method performs only a shallow copy of the original properties.
     *
     * @return a builder for {@link GetInsightRuleReportOutput}.
     */
    public Builder toBuilder() {
        var builder = new Builder();
        builder.keyLabels(this.keyLabels);
        builder.aggregationStatistic(this.aggregationStatistic);
        builder.aggregateValue(this.aggregateValue);
        builder.approximateUniqueCount(this.approximateUniqueCount);
        builder.contributors(this.contributors);
        builder.metricDatapoints(this.metricDatapoints);
        return builder;
    }

    /**
     * @return returns a new Builder.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for {@link GetInsightRuleReportOutput}.
     */
    public static final class Builder implements ShapeBuilder<GetInsightRuleReportOutput> {
        private List<String> keyLabels;
        private String aggregationStatistic;
        private Double aggregateValue;
        private Long approximateUniqueCount;
        private List<InsightRuleContributor> contributors;
        private List<InsightRuleMetricDatapoint> metricDatapoints;

        private Builder() {}

        @Override
        public Schema schema() {
            return $SCHEMA;
        }

        /**
         * An array of the strings used as the keys for this rule. The keys are the dimensions used to classify
         * contributors. If the rule contains more than one key, then each unique combination of values for the keys is
         * counted as a unique contributor.
         *
         * @return this builder.
         */
        public Builder keyLabels(List<String> keyLabels) {
            this.keyLabels = keyLabels;
            return this;
        }

        /**
         * Specifies whether this rule aggregates contributor data by COUNT or SUM.
         *
         * @return this builder.
         */
        public Builder aggregationStatistic(String aggregationStatistic) {
            this.aggregationStatistic = aggregationStatistic;
            return this;
        }

        /**
         * The sum of the values from all individual contributors that match the rule.
         *
         * @return this builder.
         */
        public Builder aggregateValue(Double aggregateValue) {
            this.aggregateValue = aggregateValue;
            return this;
        }

        /**
         * An approximate count of the unique contributors found by this rule in this time period.
         *
         * @return this builder.
         */
        public Builder approximateUniqueCount(Long approximateUniqueCount) {
            this.approximateUniqueCount = approximateUniqueCount;
            return this;
        }

        /**
         * An array of the unique contributors found by this rule in this time period. If the rule contains multiple keys,
         * each combination of values for the keys counts as a unique contributor.
         *
         * @return this builder.
         */
        public Builder contributors(List<InsightRuleContributor> contributors) {
            this.contributors = contributors;
            return this;
        }

        /**
         * A time series of metric data points that matches the time period in the rule request.
         *
         * @return this builder.
         */
        public Builder metricDatapoints(List<InsightRuleMetricDatapoint> metricDatapoints) {
            this.metricDatapoints = metricDatapoints;
            return this;
        }

        @Override
        public GetInsightRuleReportOutput build() {
            return new GetInsightRuleReportOutput(this);
        }

        @Override
        @SuppressWarnings("unchecked")
        public void setMemberValue(Schema member, Object value) {
            switch (member.memberIndex()) {
                case 0 -> keyLabels((List<String>) SchemaUtils.validateSameMember($SCHEMA_KEY_LABELS, member, value));
                case 1 -> aggregationStatistic((String) SchemaUtils.validateSameMember($SCHEMA_AGGREGATION_STATISTIC, member, value));
                case 2 -> aggregateValue((Double) SchemaUtils.validateSameMember($SCHEMA_AGGREGATE_VALUE, member, value));
                case 3 -> approximateUniqueCount((Long) SchemaUtils.validateSameMember($SCHEMA_APPROXIMATE_UNIQUE_COUNT, member, value));
                case 4 -> contributors((List<InsightRuleContributor>) SchemaUtils.validateSameMember($SCHEMA_CONTRIBUTORS, member, value));
                case 5 -> metricDatapoints((List<InsightRuleMetricDatapoint>) SchemaUtils.validateSameMember($SCHEMA_METRIC_DATAPOINTS, member, value));
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
                    case 0 -> builder.keyLabels(SharedSerde.deserializeInsightRuleContributorKeyLabels(member, de));
                    case 1 -> builder.aggregationStatistic(de.readString(member));
                    case 2 -> builder.aggregateValue(de.readDouble(member));
                    case 3 -> builder.approximateUniqueCount(de.readLong(member));
                    case 4 -> builder.contributors(SharedSerde.deserializeInsightRuleContributors(member, de));
                    case 5 -> builder.metricDatapoints(SharedSerde.deserializeInsightRuleMetricDatapoints(member, de));
                    default -> throw new IllegalArgumentException("Unexpected member: " + member.memberName());
                }
            }
        }
    }
}
