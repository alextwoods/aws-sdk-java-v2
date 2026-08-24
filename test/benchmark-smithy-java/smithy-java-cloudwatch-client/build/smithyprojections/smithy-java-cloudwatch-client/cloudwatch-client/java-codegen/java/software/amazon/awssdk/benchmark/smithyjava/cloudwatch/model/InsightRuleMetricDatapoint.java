package software.amazon.awssdk.benchmark.smithyjava.cloudwatch.model;

import java.time.Instant;
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
 * One data point from the metric time series returned in a Contributor Insights rule report.
 *
 * <p>For more information, see <a href="https://docs.aws.amazon.com/AmazonCloudWatch/latest/APIReference/API_GetInsightRuleReport.html">GetInsightRuleReport</a>.
 */
@SmithyGenerated
public final class InsightRuleMetricDatapoint implements SerializableStruct {

    public static final Schema $SCHEMA = Schemas.INSIGHT_RULE_METRIC_DATAPOINT;
    private static final Schema $SCHEMA_TIMESTAMP = $SCHEMA.member("Timestamp");
    private static final Schema $SCHEMA_UNIQUE_CONTRIBUTORS = $SCHEMA.member("UniqueContributors");
    private static final Schema $SCHEMA_MAX_CONTRIBUTOR_VALUE = $SCHEMA.member("MaxContributorValue");
    private static final Schema $SCHEMA_SAMPLE_COUNT = $SCHEMA.member("SampleCount");
    private static final Schema $SCHEMA_AVERAGE = $SCHEMA.member("Average");
    private static final Schema $SCHEMA_SUM = $SCHEMA.member("Sum");
    private static final Schema $SCHEMA_MINIMUM = $SCHEMA.member("Minimum");
    private static final Schema $SCHEMA_MAXIMUM = $SCHEMA.member("Maximum");

    public static final ShapeId $ID = $SCHEMA.id();

    private final transient Instant timestamp;
    private final transient Double uniqueContributors;
    private final transient Double maxContributorValue;
    private final transient Double sampleCount;
    private final transient Double average;
    private final transient Double sum;
    private final transient Double minimum;
    private final transient Double maximum;

    private InsightRuleMetricDatapoint(Builder builder) {
        this.timestamp = builder.timestamp;
        this.uniqueContributors = builder.uniqueContributors;
        this.maxContributorValue = builder.maxContributorValue;
        this.sampleCount = builder.sampleCount;
        this.average = builder.average;
        this.sum = builder.sum;
        this.minimum = builder.minimum;
        this.maximum = builder.maximum;
    }

    /**
     * The timestamp of the data point.
     */
    public Instant getTimestamp() {
        return timestamp;
    }

    /**
     * The number of unique contributors who published data during this timestamp.
     *
     * <p>This statistic is returned only if you included it in the <code>Metrics</code> array in your request.
     */
    public Double getUniqueContributors() {
        return uniqueContributors;
    }

    /**
     * The maximum value provided by one contributor during this timestamp. Each timestamp is evaluated separately, so
     * the identity of the max contributor could be different for each timestamp.
     *
     * <p>This statistic is returned only if you included it in the <code>Metrics</code> array in your request.
     */
    public Double getMaxContributorValue() {
        return maxContributorValue;
    }

    /**
     * The number of occurrences that matched the rule during this data point.
     *
     * <p>This statistic is returned only if you included it in the <code>Metrics</code> array in your request.
     */
    public Double getSampleCount() {
        return sampleCount;
    }

    /**
     * The average value from all contributors during the time period represented by that data point.
     *
     * <p>This statistic is returned only if you included it in the <code>Metrics</code> array in your request.
     */
    public Double getAverage() {
        return average;
    }

    /**
     * The sum of the values from all contributors during the time period represented by that data point.
     *
     * <p>This statistic is returned only if you included it in the <code>Metrics</code> array in your request.
     */
    public Double getSum() {
        return sum;
    }

    /**
     * The minimum value from a single contributor during the time period represented by that data point.
     *
     * <p>This statistic is returned only if you included it in the <code>Metrics</code> array in your request.
     */
    public Double getMinimum() {
        return minimum;
    }

    /**
     * The maximum value from a single occurence from a single contributor during the time period represented by that
     * data point.
     *
     * <p>This statistic is returned only if you included it in the <code>Metrics</code> array in your request.
     */
    public Double getMaximum() {
        return maximum;
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
        InsightRuleMetricDatapoint that = (InsightRuleMetricDatapoint) other;
        return Objects.equals(this.uniqueContributors, that.uniqueContributors)
               && Objects.equals(this.maxContributorValue, that.maxContributorValue)
               && Objects.equals(this.sampleCount, that.sampleCount)
               && Objects.equals(this.average, that.average)
               && Objects.equals(this.sum, that.sum)
               && Objects.equals(this.minimum, that.minimum)
               && Objects.equals(this.maximum, that.maximum)
               && Objects.equals(this.timestamp, that.timestamp);
    }

    @Override
    public int hashCode() {
        int $hc = Objects.hashCode(timestamp);
        $hc = 31 * $hc + Objects.hashCode(uniqueContributors);
        $hc = 31 * $hc + Objects.hashCode(maxContributorValue);
        $hc = 31 * $hc + Objects.hashCode(sampleCount);
        $hc = 31 * $hc + Objects.hashCode(average);
        $hc = 31 * $hc + Objects.hashCode(sum);
        $hc = 31 * $hc + Objects.hashCode(minimum);
        $hc = 31 * $hc + Objects.hashCode(maximum);
        return $hc;
    }

    @Override
    public Schema schema() {
        return $SCHEMA;
    }

    @Override
    public void serializeMembers(ShapeSerializer serializer) {
        if (timestamp != null) {
            serializer.writeTimestamp($SCHEMA_TIMESTAMP, timestamp);
        }
        if (uniqueContributors != null) {
            serializer.writeDouble($SCHEMA_UNIQUE_CONTRIBUTORS, uniqueContributors);
        }
        if (maxContributorValue != null) {
            serializer.writeDouble($SCHEMA_MAX_CONTRIBUTOR_VALUE, maxContributorValue);
        }
        if (sampleCount != null) {
            serializer.writeDouble($SCHEMA_SAMPLE_COUNT, sampleCount);
        }
        if (average != null) {
            serializer.writeDouble($SCHEMA_AVERAGE, average);
        }
        if (sum != null) {
            serializer.writeDouble($SCHEMA_SUM, sum);
        }
        if (minimum != null) {
            serializer.writeDouble($SCHEMA_MINIMUM, minimum);
        }
        if (maximum != null) {
            serializer.writeDouble($SCHEMA_MAXIMUM, maximum);
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T getMemberValue(Schema member) {
        return switch (member.memberIndex()) {
            case 0 -> (T) SchemaUtils.validateSameMember($SCHEMA_TIMESTAMP, member, timestamp);
            case 1 -> (T) SchemaUtils.validateSameMember($SCHEMA_UNIQUE_CONTRIBUTORS, member, uniqueContributors);
            case 2 -> (T) SchemaUtils.validateSameMember($SCHEMA_MAX_CONTRIBUTOR_VALUE, member, maxContributorValue);
            case 3 -> (T) SchemaUtils.validateSameMember($SCHEMA_SAMPLE_COUNT, member, sampleCount);
            case 4 -> (T) SchemaUtils.validateSameMember($SCHEMA_AVERAGE, member, average);
            case 5 -> (T) SchemaUtils.validateSameMember($SCHEMA_SUM, member, sum);
            case 6 -> (T) SchemaUtils.validateSameMember($SCHEMA_MINIMUM, member, minimum);
            case 7 -> (T) SchemaUtils.validateSameMember($SCHEMA_MAXIMUM, member, maximum);
            default -> throw new IllegalArgumentException("Attempted to get non-existent member: " + member.id());
        };
    }

    /**
     * Create a new builder containing all the current property values of this object.
     *
     * <p><strong>Note:</strong> This method performs only a shallow copy of the original properties.
     *
     * @return a builder for {@link InsightRuleMetricDatapoint}.
     */
    public Builder toBuilder() {
        var builder = new Builder();
        builder.timestamp(this.timestamp);
        builder.uniqueContributors(this.uniqueContributors);
        builder.maxContributorValue(this.maxContributorValue);
        builder.sampleCount(this.sampleCount);
        builder.average(this.average);
        builder.sum(this.sum);
        builder.minimum(this.minimum);
        builder.maximum(this.maximum);
        return builder;
    }

    /**
     * @return returns a new Builder.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for {@link InsightRuleMetricDatapoint}.
     */
    public static final class Builder implements ShapeBuilder<InsightRuleMetricDatapoint> {
        private final PresenceTracker tracker = PresenceTracker.of($SCHEMA);
        private Instant timestamp;
        private Double uniqueContributors;
        private Double maxContributorValue;
        private Double sampleCount;
        private Double average;
        private Double sum;
        private Double minimum;
        private Double maximum;

        private Builder() {
            // Tell the tracker to assume clientOptional members are present.
            tracker.setMember($SCHEMA_TIMESTAMP);
        }

        @Override
        public Schema schema() {
            return $SCHEMA;
        }

        /**
         * The timestamp of the data point.
         *
         * <p><strong>Required</strong>
         * @return this builder.
         */
        public Builder timestamp(Instant timestamp) {
            this.timestamp = Objects.requireNonNull(timestamp, "timestamp cannot be null");
            tracker.setMember($SCHEMA_TIMESTAMP);
            return this;
        }

        /**
         * The number of unique contributors who published data during this timestamp.
         *
         * <p>This statistic is returned only if you included it in the <code>Metrics</code> array in your request.
         *
         * @return this builder.
         */
        public Builder uniqueContributors(Double uniqueContributors) {
            this.uniqueContributors = uniqueContributors;
            return this;
        }

        /**
         * The maximum value provided by one contributor during this timestamp. Each timestamp is evaluated separately, so
         * the identity of the max contributor could be different for each timestamp.
         *
         * <p>This statistic is returned only if you included it in the <code>Metrics</code> array in your request.
         *
         * @return this builder.
         */
        public Builder maxContributorValue(Double maxContributorValue) {
            this.maxContributorValue = maxContributorValue;
            return this;
        }

        /**
         * The number of occurrences that matched the rule during this data point.
         *
         * <p>This statistic is returned only if you included it in the <code>Metrics</code> array in your request.
         *
         * @return this builder.
         */
        public Builder sampleCount(Double sampleCount) {
            this.sampleCount = sampleCount;
            return this;
        }

        /**
         * The average value from all contributors during the time period represented by that data point.
         *
         * <p>This statistic is returned only if you included it in the <code>Metrics</code> array in your request.
         *
         * @return this builder.
         */
        public Builder average(Double average) {
            this.average = average;
            return this;
        }

        /**
         * The sum of the values from all contributors during the time period represented by that data point.
         *
         * <p>This statistic is returned only if you included it in the <code>Metrics</code> array in your request.
         *
         * @return this builder.
         */
        public Builder sum(Double sum) {
            this.sum = sum;
            return this;
        }

        /**
         * The minimum value from a single contributor during the time period represented by that data point.
         *
         * <p>This statistic is returned only if you included it in the <code>Metrics</code> array in your request.
         *
         * @return this builder.
         */
        public Builder minimum(Double minimum) {
            this.minimum = minimum;
            return this;
        }

        /**
         * The maximum value from a single occurence from a single contributor during the time period represented by that
         * data point.
         *
         * <p>This statistic is returned only if you included it in the <code>Metrics</code> array in your request.
         *
         * @return this builder.
         */
        public Builder maximum(Double maximum) {
            this.maximum = maximum;
            return this;
        }

        @Override
        public InsightRuleMetricDatapoint build() {
            tracker.validate();
            return new InsightRuleMetricDatapoint(this);
        }

        @Override
        @SuppressWarnings("unchecked")
        public void setMemberValue(Schema member, Object value) {
            switch (member.memberIndex()) {
                case 0 -> timestamp((Instant) SchemaUtils.validateSameMember($SCHEMA_TIMESTAMP, member, value));
                case 1 -> uniqueContributors((Double) SchemaUtils.validateSameMember($SCHEMA_UNIQUE_CONTRIBUTORS, member, value));
                case 2 -> maxContributorValue((Double) SchemaUtils.validateSameMember($SCHEMA_MAX_CONTRIBUTOR_VALUE, member, value));
                case 3 -> sampleCount((Double) SchemaUtils.validateSameMember($SCHEMA_SAMPLE_COUNT, member, value));
                case 4 -> average((Double) SchemaUtils.validateSameMember($SCHEMA_AVERAGE, member, value));
                case 5 -> sum((Double) SchemaUtils.validateSameMember($SCHEMA_SUM, member, value));
                case 6 -> minimum((Double) SchemaUtils.validateSameMember($SCHEMA_MINIMUM, member, value));
                case 7 -> maximum((Double) SchemaUtils.validateSameMember($SCHEMA_MAXIMUM, member, value));
                default -> ShapeBuilder.super.setMemberValue(member, value);
            }
        }

        @Override
        public ShapeBuilder<InsightRuleMetricDatapoint> errorCorrection() {
            if (tracker.allSet()) {
                return this;
            }
            if (!tracker.checkMember($SCHEMA_TIMESTAMP)) {
                timestamp(Instant.EPOCH);
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
                    case 0 -> builder.timestamp(de.readTimestamp(member));
                    case 1 -> builder.uniqueContributors(de.readDouble(member));
                    case 2 -> builder.maxContributorValue(de.readDouble(member));
                    case 3 -> builder.sampleCount(de.readDouble(member));
                    case 4 -> builder.average(de.readDouble(member));
                    case 5 -> builder.sum(de.readDouble(member));
                    case 6 -> builder.minimum(de.readDouble(member));
                    case 7 -> builder.maximum(de.readDouble(member));
                    default -> throw new IllegalArgumentException("Unexpected member: " + member.memberName());
                }
            }
        }
    }
}
