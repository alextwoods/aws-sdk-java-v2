package software.amazon.awssdk.benchmark.smithyjava.cloudwatch.model;

import java.time.Instant;
import java.util.Collections;
import java.util.Map;
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

/**
 * Encapsulates the statistical data that CloudWatch computes from metric data.
 */
@SmithyGenerated
public final class Datapoint implements SerializableStruct {

    public static final Schema $SCHEMA = Schemas.DATAPOINT;
    private static final Schema $SCHEMA_TIMESTAMP = $SCHEMA.member("Timestamp");
    private static final Schema $SCHEMA_SAMPLE_COUNT = $SCHEMA.member("SampleCount");
    private static final Schema $SCHEMA_AVERAGE = $SCHEMA.member("Average");
    private static final Schema $SCHEMA_SUM = $SCHEMA.member("Sum");
    private static final Schema $SCHEMA_MINIMUM = $SCHEMA.member("Minimum");
    private static final Schema $SCHEMA_MAXIMUM = $SCHEMA.member("Maximum");
    private static final Schema $SCHEMA_UNIT = $SCHEMA.member("Unit");
    private static final Schema $SCHEMA_EXTENDED_STATISTICS = $SCHEMA.member("ExtendedStatistics");

    public static final ShapeId $ID = $SCHEMA.id();

    private final transient Instant timestamp;
    private final transient Double sampleCount;
    private final transient Double average;
    private final transient Double sum;
    private final transient Double minimum;
    private final transient Double maximum;
    private final transient StandardUnit unit;
    private final transient Map<String, Double> extendedStatistics;

    private Datapoint(Builder builder) {
        this.timestamp = builder.timestamp;
        this.sampleCount = builder.sampleCount;
        this.average = builder.average;
        this.sum = builder.sum;
        this.minimum = builder.minimum;
        this.maximum = builder.maximum;
        this.unit = builder.unit;
        this.extendedStatistics = builder.extendedStatistics == null ? null : Collections.unmodifiableMap(builder.extendedStatistics);
    }

    /**
     * The time stamp used for the data point.
     */
    public Instant getTimestamp() {
        return timestamp;
    }

    /**
     * The number of metric values that contributed to the aggregate value of this data point.
     */
    public Double getSampleCount() {
        return sampleCount;
    }

    /**
     * The average of the metric values that correspond to the data point.
     */
    public Double getAverage() {
        return average;
    }

    /**
     * The sum of the metric values for the data point.
     */
    public Double getSum() {
        return sum;
    }

    /**
     * The minimum metric value for the data point.
     */
    public Double getMinimum() {
        return minimum;
    }

    /**
     * The maximum metric value for the data point.
     */
    public Double getMaximum() {
        return maximum;
    }

    /**
     * The standard unit for the data point.
     */
    public StandardUnit getUnit() {
        return unit;
    }

    /**
     * The percentile statistic for the data point.
     */
    public Map<String, Double> getExtendedStatistics() {
        if (extendedStatistics == null) {
            return Collections.emptyMap();
        }
        return extendedStatistics;
    }

    public boolean hasExtendedStatistics() {
        return extendedStatistics != null;
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
        Datapoint that = (Datapoint) other;
        return Objects.equals(this.sampleCount, that.sampleCount)
               && Objects.equals(this.average, that.average)
               && Objects.equals(this.sum, that.sum)
               && Objects.equals(this.minimum, that.minimum)
               && Objects.equals(this.maximum, that.maximum)
               && Objects.equals(this.unit, that.unit)
               && Objects.equals(this.timestamp, that.timestamp)
               && Objects.equals(this.extendedStatistics, that.extendedStatistics);
    }

    @Override
    public int hashCode() {
        int $hc = Objects.hashCode(timestamp);
        $hc = 31 * $hc + Objects.hashCode(sampleCount);
        $hc = 31 * $hc + Objects.hashCode(average);
        $hc = 31 * $hc + Objects.hashCode(sum);
        $hc = 31 * $hc + Objects.hashCode(minimum);
        $hc = 31 * $hc + Objects.hashCode(maximum);
        $hc = 31 * $hc + Objects.hashCode(unit);
        $hc = 31 * $hc + Objects.hashCode(extendedStatistics);
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
        if (unit != null) {
            serializer.writeString($SCHEMA_UNIT, unit.getValue());
        }
        if (extendedStatistics != null) {
            serializer.writeMap($SCHEMA_EXTENDED_STATISTICS, extendedStatistics, extendedStatistics.size(), SharedSerde.DatapointValueMapSerializer.INSTANCE);
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T getMemberValue(Schema member) {
        return switch (member.memberIndex()) {
            case 0 -> (T) SchemaUtils.validateSameMember($SCHEMA_TIMESTAMP, member, timestamp);
            case 1 -> (T) SchemaUtils.validateSameMember($SCHEMA_SAMPLE_COUNT, member, sampleCount);
            case 2 -> (T) SchemaUtils.validateSameMember($SCHEMA_AVERAGE, member, average);
            case 3 -> (T) SchemaUtils.validateSameMember($SCHEMA_SUM, member, sum);
            case 4 -> (T) SchemaUtils.validateSameMember($SCHEMA_MINIMUM, member, minimum);
            case 5 -> (T) SchemaUtils.validateSameMember($SCHEMA_MAXIMUM, member, maximum);
            case 6 -> (T) SchemaUtils.validateSameMember($SCHEMA_UNIT, member, unit);
            case 7 -> (T) SchemaUtils.validateSameMember($SCHEMA_EXTENDED_STATISTICS, member, extendedStatistics);
            default -> throw new IllegalArgumentException("Attempted to get non-existent member: " + member.id());
        };
    }

    /**
     * Create a new builder containing all the current property values of this object.
     *
     * <p><strong>Note:</strong> This method performs only a shallow copy of the original properties.
     *
     * @return a builder for {@link Datapoint}.
     */
    public Builder toBuilder() {
        var builder = new Builder();
        builder.timestamp(this.timestamp);
        builder.sampleCount(this.sampleCount);
        builder.average(this.average);
        builder.sum(this.sum);
        builder.minimum(this.minimum);
        builder.maximum(this.maximum);
        builder.unit(this.unit);
        builder.extendedStatistics(this.extendedStatistics);
        return builder;
    }

    /**
     * @return returns a new Builder.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for {@link Datapoint}.
     */
    public static final class Builder implements ShapeBuilder<Datapoint> {
        private Instant timestamp;
        private Double sampleCount;
        private Double average;
        private Double sum;
        private Double minimum;
        private Double maximum;
        private StandardUnit unit;
        private Map<String, Double> extendedStatistics;

        private Builder() {}

        @Override
        public Schema schema() {
            return $SCHEMA;
        }

        /**
         * The time stamp used for the data point.
         *
         * @return this builder.
         */
        public Builder timestamp(Instant timestamp) {
            this.timestamp = timestamp;
            return this;
        }

        /**
         * The number of metric values that contributed to the aggregate value of this data point.
         *
         * @return this builder.
         */
        public Builder sampleCount(Double sampleCount) {
            this.sampleCount = sampleCount;
            return this;
        }

        /**
         * The average of the metric values that correspond to the data point.
         *
         * @return this builder.
         */
        public Builder average(Double average) {
            this.average = average;
            return this;
        }

        /**
         * The sum of the metric values for the data point.
         *
         * @return this builder.
         */
        public Builder sum(Double sum) {
            this.sum = sum;
            return this;
        }

        /**
         * The minimum metric value for the data point.
         *
         * @return this builder.
         */
        public Builder minimum(Double minimum) {
            this.minimum = minimum;
            return this;
        }

        /**
         * The maximum metric value for the data point.
         *
         * @return this builder.
         */
        public Builder maximum(Double maximum) {
            this.maximum = maximum;
            return this;
        }

        /**
         * The standard unit for the data point.
         *
         * @return this builder.
         */
        public Builder unit(StandardUnit unit) {
            this.unit = unit;
            return this;
        }

        /**
         * The percentile statistic for the data point.
         *
         * @return this builder.
         */
        public Builder extendedStatistics(Map<String, Double> extendedStatistics) {
            this.extendedStatistics = extendedStatistics;
            return this;
        }

        @Override
        public Datapoint build() {
            return new Datapoint(this);
        }

        @Override
        @SuppressWarnings("unchecked")
        public void setMemberValue(Schema member, Object value) {
            switch (member.memberIndex()) {
                case 0 -> timestamp((Instant) SchemaUtils.validateSameMember($SCHEMA_TIMESTAMP, member, value));
                case 1 -> sampleCount((Double) SchemaUtils.validateSameMember($SCHEMA_SAMPLE_COUNT, member, value));
                case 2 -> average((Double) SchemaUtils.validateSameMember($SCHEMA_AVERAGE, member, value));
                case 3 -> sum((Double) SchemaUtils.validateSameMember($SCHEMA_SUM, member, value));
                case 4 -> minimum((Double) SchemaUtils.validateSameMember($SCHEMA_MINIMUM, member, value));
                case 5 -> maximum((Double) SchemaUtils.validateSameMember($SCHEMA_MAXIMUM, member, value));
                case 6 -> unit((StandardUnit) SchemaUtils.validateSameMember($SCHEMA_UNIT, member, value));
                case 7 -> extendedStatistics((Map<String, Double>) SchemaUtils.validateSameMember($SCHEMA_EXTENDED_STATISTICS, member, value));
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
                    case 0 -> builder.timestamp(de.readTimestamp(member));
                    case 1 -> builder.sampleCount(de.readDouble(member));
                    case 2 -> builder.average(de.readDouble(member));
                    case 3 -> builder.sum(de.readDouble(member));
                    case 4 -> builder.minimum(de.readDouble(member));
                    case 5 -> builder.maximum(de.readDouble(member));
                    case 6 -> builder.unit(StandardUnit.builder().deserializeMember(de, member).build());
                    case 7 -> builder.extendedStatistics(SharedSerde.deserializeDatapointValueMap(member, de));
                    default -> throw new IllegalArgumentException("Unexpected member: " + member.memberName());
                }
            }
        }
    }
}
