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

@SmithyGenerated
public final class DescribeAlarmsForMetricInput implements SerializableStruct {

    public static final Schema $SCHEMA = Schemas.DESCRIBE_ALARMS_FOR_METRIC_INPUT;
    private static final Schema $SCHEMA_METRIC_NAME = $SCHEMA.member("MetricName");
    private static final Schema $SCHEMA_NAMESPACE = $SCHEMA.member("Namespace");
    private static final Schema $SCHEMA_STATISTIC = $SCHEMA.member("Statistic");
    private static final Schema $SCHEMA_EXTENDED_STATISTIC = $SCHEMA.member("ExtendedStatistic");
    private static final Schema $SCHEMA_DIMENSIONS = $SCHEMA.member("Dimensions");
    private static final Schema $SCHEMA_PERIOD = $SCHEMA.member("Period");
    private static final Schema $SCHEMA_UNIT = $SCHEMA.member("Unit");

    public static final ShapeId $ID = $SCHEMA.id();

    private final transient String metricName;
    private final transient String namespace;
    private final transient Statistic statistic;
    private final transient String extendedStatistic;
    private final transient List<Dimension> dimensions;
    private final transient Integer period;
    private final transient StandardUnit unit;

    private DescribeAlarmsForMetricInput(Builder builder) {
        this.metricName = builder.metricName;
        this.namespace = builder.namespace;
        this.statistic = builder.statistic;
        this.extendedStatistic = builder.extendedStatistic;
        this.dimensions = builder.dimensions == null ? null : Collections.unmodifiableList(builder.dimensions);
        this.period = builder.period;
        this.unit = builder.unit;
    }

    /**
     * The name of the metric.
     */
    public String getMetricName() {
        return metricName;
    }

    /**
     * The namespace of the metric.
     */
    public String getNamespace() {
        return namespace;
    }

    /**
     * The statistic for the metric, other than percentiles. For percentile statistics, use
     * <code>ExtendedStatistics</code>.
     */
    public Statistic getStatistic() {
        return statistic;
    }

    /**
     * The percentile statistic for the metric. Specify a value between p0.0 and p100.
     */
    public String getExtendedStatistic() {
        return extendedStatistic;
    }

    /**
     * The dimensions associated with the metric. If the metric has any associated dimensions, you must specify them in
     * order for the call to succeed.
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
     * The period, in seconds, over which the statistic is applied.
     */
    public Integer getPeriod() {
        return period;
    }

    /**
     * The unit for the metric.
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
        DescribeAlarmsForMetricInput that = (DescribeAlarmsForMetricInput) other;
        return Objects.equals(this.period, that.period)
               && Objects.equals(this.metricName, that.metricName)
               && Objects.equals(this.namespace, that.namespace)
               && Objects.equals(this.extendedStatistic, that.extendedStatistic)
               && Objects.equals(this.statistic, that.statistic)
               && Objects.equals(this.unit, that.unit)
               && Objects.equals(this.dimensions, that.dimensions);
    }

    @Override
    public int hashCode() {
        int $hc = Objects.hashCode(metricName);
        $hc = 31 * $hc + Objects.hashCode(namespace);
        $hc = 31 * $hc + Objects.hashCode(statistic);
        $hc = 31 * $hc + Objects.hashCode(extendedStatistic);
        $hc = 31 * $hc + Objects.hashCode(dimensions);
        $hc = 31 * $hc + Objects.hashCode(period);
        $hc = 31 * $hc + Objects.hashCode(unit);
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
        if (namespace != null) {
            serializer.writeString($SCHEMA_NAMESPACE, namespace);
        }
        if (statistic != null) {
            serializer.writeString($SCHEMA_STATISTIC, statistic.getValue());
        }
        if (extendedStatistic != null) {
            serializer.writeString($SCHEMA_EXTENDED_STATISTIC, extendedStatistic);
        }
        if (dimensions != null) {
            serializer.writeList($SCHEMA_DIMENSIONS, dimensions, dimensions.size(), SharedSerde.DimensionsSerializer.INSTANCE);
        }
        if (period != null) {
            serializer.writeInteger($SCHEMA_PERIOD, period);
        }
        if (unit != null) {
            serializer.writeString($SCHEMA_UNIT, unit.getValue());
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T getMemberValue(Schema member) {
        return switch (member.memberIndex()) {
            case 0 -> (T) SchemaUtils.validateSameMember($SCHEMA_METRIC_NAME, member, metricName);
            case 1 -> (T) SchemaUtils.validateSameMember($SCHEMA_NAMESPACE, member, namespace);
            case 2 -> (T) SchemaUtils.validateSameMember($SCHEMA_STATISTIC, member, statistic);
            case 3 -> (T) SchemaUtils.validateSameMember($SCHEMA_EXTENDED_STATISTIC, member, extendedStatistic);
            case 4 -> (T) SchemaUtils.validateSameMember($SCHEMA_DIMENSIONS, member, dimensions);
            case 5 -> (T) SchemaUtils.validateSameMember($SCHEMA_PERIOD, member, period);
            case 6 -> (T) SchemaUtils.validateSameMember($SCHEMA_UNIT, member, unit);
            default -> throw new IllegalArgumentException("Attempted to get non-existent member: " + member.id());
        };
    }

    /**
     * Create a new builder containing all the current property values of this object.
     *
     * <p><strong>Note:</strong> This method performs only a shallow copy of the original properties.
     *
     * @return a builder for {@link DescribeAlarmsForMetricInput}.
     */
    public Builder toBuilder() {
        var builder = new Builder();
        builder.metricName(this.metricName);
        builder.namespace(this.namespace);
        builder.statistic(this.statistic);
        builder.extendedStatistic(this.extendedStatistic);
        builder.dimensions(this.dimensions);
        builder.period(this.period);
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
     * Builder for {@link DescribeAlarmsForMetricInput}.
     */
    public static final class Builder implements ShapeBuilder<DescribeAlarmsForMetricInput> {
        private final PresenceTracker tracker = PresenceTracker.of($SCHEMA);
        private String metricName;
        private String namespace;
        private Statistic statistic;
        private String extendedStatistic;
        private List<Dimension> dimensions;
        private Integer period;
        private StandardUnit unit;

        private Builder() {
            // Tell the tracker to assume clientOptional members are present.
            tracker.setMember($SCHEMA_METRIC_NAME);
            tracker.setMember($SCHEMA_NAMESPACE);
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
         * The statistic for the metric, other than percentiles. For percentile statistics, use
         * <code>ExtendedStatistics</code>.
         *
         * @return this builder.
         */
        public Builder statistic(Statistic statistic) {
            this.statistic = statistic;
            return this;
        }

        /**
         * The percentile statistic for the metric. Specify a value between p0.0 and p100.
         *
         * @return this builder.
         */
        public Builder extendedStatistic(String extendedStatistic) {
            this.extendedStatistic = extendedStatistic;
            return this;
        }

        /**
         * The dimensions associated with the metric. If the metric has any associated dimensions, you must specify them in
         * order for the call to succeed.
         *
         * @return this builder.
         */
        public Builder dimensions(List<Dimension> dimensions) {
            this.dimensions = dimensions;
            return this;
        }

        /**
         * The period, in seconds, over which the statistic is applied.
         *
         * @return this builder.
         */
        public Builder period(Integer period) {
            this.period = period;
            return this;
        }

        /**
         * The unit for the metric.
         *
         * @return this builder.
         */
        public Builder unit(StandardUnit unit) {
            this.unit = unit;
            return this;
        }

        @Override
        public DescribeAlarmsForMetricInput build() {
            tracker.validate();
            return new DescribeAlarmsForMetricInput(this);
        }

        @Override
        @SuppressWarnings("unchecked")
        public void setMemberValue(Schema member, Object value) {
            switch (member.memberIndex()) {
                case 0 -> metricName((String) SchemaUtils.validateSameMember($SCHEMA_METRIC_NAME, member, value));
                case 1 -> namespace((String) SchemaUtils.validateSameMember($SCHEMA_NAMESPACE, member, value));
                case 2 -> statistic((Statistic) SchemaUtils.validateSameMember($SCHEMA_STATISTIC, member, value));
                case 3 -> extendedStatistic((String) SchemaUtils.validateSameMember($SCHEMA_EXTENDED_STATISTIC, member, value));
                case 4 -> dimensions((List<Dimension>) SchemaUtils.validateSameMember($SCHEMA_DIMENSIONS, member, value));
                case 5 -> period((Integer) SchemaUtils.validateSameMember($SCHEMA_PERIOD, member, value));
                case 6 -> unit((StandardUnit) SchemaUtils.validateSameMember($SCHEMA_UNIT, member, value));
                default -> ShapeBuilder.super.setMemberValue(member, value);
            }
        }

        @Override
        public ShapeBuilder<DescribeAlarmsForMetricInput> errorCorrection() {
            if (tracker.allSet()) {
                return this;
            }
            if (!tracker.checkMember($SCHEMA_METRIC_NAME)) {
                metricName("");
            }
            if (!tracker.checkMember($SCHEMA_NAMESPACE)) {
                namespace("");
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
                    case 1 -> builder.namespace(de.readString(member));
                    case 2 -> builder.statistic(Statistic.builder().deserializeMember(de, member).build());
                    case 3 -> builder.extendedStatistic(de.readString(member));
                    case 4 -> builder.dimensions(SharedSerde.deserializeDimensions(member, de));
                    case 5 -> builder.period(de.readInteger(member));
                    case 6 -> builder.unit(StandardUnit.builder().deserializeMember(de, member).build());
                    default -> throw new IllegalArgumentException("Unexpected member: " + member.memberName());
                }
            }
        }
    }
}
