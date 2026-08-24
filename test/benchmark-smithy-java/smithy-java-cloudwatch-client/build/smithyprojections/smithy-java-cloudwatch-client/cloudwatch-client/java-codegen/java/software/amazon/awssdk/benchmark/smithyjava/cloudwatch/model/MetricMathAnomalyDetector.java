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

/**
 * Indicates the CloudWatch math expression that provides the time series the anomaly detector uses as input. The
 * designated math expression must return a single time series.
 */
@SmithyGenerated
public final class MetricMathAnomalyDetector implements SerializableStruct {

    public static final Schema $SCHEMA = Schemas.METRIC_MATH_ANOMALY_DETECTOR;
    private static final Schema $SCHEMA_METRIC_DATA_QUERIES = $SCHEMA.member("MetricDataQueries");

    public static final ShapeId $ID = $SCHEMA.id();

    private final transient List<MetricDataQuery> metricDataQueries;

    private MetricMathAnomalyDetector(Builder builder) {
        this.metricDataQueries = builder.metricDataQueries == null ? null : Collections.unmodifiableList(builder.metricDataQueries);
    }

    /**
     * An array of metric data query structures that enables you to create an anomaly detector based on the result of a
     * metric math expression. Each item in <code>MetricDataQueries</code> gets a metric or performs a math expression.
     * One item in <code>MetricDataQueries</code> is the expression that provides the time series that the anomaly
     * detector uses as input. Designate the expression by setting <code>ReturnData</code> to <code>true</code> for this
     * object in the array. For all other expressions and metrics, set <code>ReturnData</code> to <code>false</code>.
     * The designated expression must return a single time series.
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
        MetricMathAnomalyDetector that = (MetricMathAnomalyDetector) other;
        return Objects.equals(this.metricDataQueries, that.metricDataQueries);
    }

    @Override
    public int hashCode() {
        int $hc = Objects.hashCode(metricDataQueries);
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
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T getMemberValue(Schema member) {
        return switch (member.memberIndex()) {
            case 0 -> (T) SchemaUtils.validateSameMember($SCHEMA_METRIC_DATA_QUERIES, member, metricDataQueries);
            default -> throw new IllegalArgumentException("Attempted to get non-existent member: " + member.id());
        };
    }

    /**
     * Create a new builder containing all the current property values of this object.
     *
     * <p><strong>Note:</strong> This method performs only a shallow copy of the original properties.
     *
     * @return a builder for {@link MetricMathAnomalyDetector}.
     */
    public Builder toBuilder() {
        var builder = new Builder();
        builder.metricDataQueries(this.metricDataQueries);
        return builder;
    }

    /**
     * @return returns a new Builder.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for {@link MetricMathAnomalyDetector}.
     */
    public static final class Builder implements ShapeBuilder<MetricMathAnomalyDetector> {
        private List<MetricDataQuery> metricDataQueries;

        private Builder() {}

        @Override
        public Schema schema() {
            return $SCHEMA;
        }

        /**
         * An array of metric data query structures that enables you to create an anomaly detector based on the result of a
         * metric math expression. Each item in <code>MetricDataQueries</code> gets a metric or performs a math expression.
         * One item in <code>MetricDataQueries</code> is the expression that provides the time series that the anomaly
         * detector uses as input. Designate the expression by setting <code>ReturnData</code> to <code>true</code> for this
         * object in the array. For all other expressions and metrics, set <code>ReturnData</code> to <code>false</code>.
         * The designated expression must return a single time series.
         *
         * @return this builder.
         */
        public Builder metricDataQueries(List<MetricDataQuery> metricDataQueries) {
            this.metricDataQueries = metricDataQueries;
            return this;
        }

        @Override
        public MetricMathAnomalyDetector build() {
            return new MetricMathAnomalyDetector(this);
        }

        @Override
        @SuppressWarnings("unchecked")
        public void setMemberValue(Schema member, Object value) {
            switch (member.memberIndex()) {
                case 0 -> metricDataQueries((List<MetricDataQuery>) SchemaUtils.validateSameMember($SCHEMA_METRIC_DATA_QUERIES, member, value));
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
                    case 0 -> builder.metricDataQueries(SharedSerde.deserializeMetricDataQueries(member, de));
                    default -> throw new IllegalArgumentException("Unexpected member: " + member.memberName());
                }
            }
        }
    }
}
