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
 * An anomaly detection model associated with a particular CloudWatch metric, statistic, or metric math expression. You
 * can use the model to display a band of expected, normal values when the metric is graphed.
 *
 * <p>If you have enabled unified cross-account observability, and this account is a monitoring account, the metric can
 * be in the same account or a source account.
 */
@SmithyGenerated
public final class AnomalyDetector implements SerializableStruct {

    public static final Schema $SCHEMA = Schemas.ANOMALY_DETECTOR;
    private static final Schema $SCHEMA_ANOMALY_DETECTOR_ID = $SCHEMA.member("AnomalyDetectorId");
    private static final Schema $SCHEMA_NAMESPACE = $SCHEMA.member("Namespace");
    private static final Schema $SCHEMA_METRIC_NAME = $SCHEMA.member("MetricName");
    private static final Schema $SCHEMA_DIMENSIONS = $SCHEMA.member("Dimensions");
    private static final Schema $SCHEMA_STAT = $SCHEMA.member("Stat");
    private static final Schema $SCHEMA_CONFIGURATION = $SCHEMA.member("Configuration");
    private static final Schema $SCHEMA_STATE_VALUE = $SCHEMA.member("StateValue");
    private static final Schema $SCHEMA_METRIC_CHARACTERISTICS = $SCHEMA.member("MetricCharacteristics");
    private static final Schema $SCHEMA_SINGLE_METRIC_ANOMALY_DETECTOR = $SCHEMA.member("SingleMetricAnomalyDetector");
    private static final Schema $SCHEMA_METRIC_MATH_ANOMALY_DETECTOR = $SCHEMA.member("MetricMathAnomalyDetector");

    public static final ShapeId $ID = $SCHEMA.id();

    private final transient String anomalyDetectorId;
    private final transient String namespace;
    private final transient String metricName;
    private final transient List<Dimension> dimensions;
    private final transient String stat;
    private final transient AnomalyDetectorConfiguration configuration;
    private final transient AnomalyDetectorStateValue stateValue;
    private final transient MetricCharacteristics metricCharacteristics;
    private final transient SingleMetricAnomalyDetector singleMetricAnomalyDetector;
    private final transient MetricMathAnomalyDetector metricMathAnomalyDetector;

    private AnomalyDetector(Builder builder) {
        this.anomalyDetectorId = builder.anomalyDetectorId;
        this.namespace = builder.namespace;
        this.metricName = builder.metricName;
        this.dimensions = builder.dimensions == null ? null : Collections.unmodifiableList(builder.dimensions);
        this.stat = builder.stat;
        this.configuration = builder.configuration;
        this.stateValue = builder.stateValue;
        this.metricCharacteristics = builder.metricCharacteristics;
        this.singleMetricAnomalyDetector = builder.singleMetricAnomalyDetector;
        this.metricMathAnomalyDetector = builder.metricMathAnomalyDetector;
    }

    /**
     * The unique identifier of the anomaly detector.
     *
     * <p>The identifier does not restrict access to a specific anomaly detector in an IAM policy. Permissions for
     * anomaly detector operations apply to all anomaly detectors in the account.
     */
    public String getAnomalyDetectorId() {
        return anomalyDetectorId;
    }

    /**
     * The namespace of the metric associated with the anomaly detection model.
     *
     * @deprecated Use SingleMetricAnomalyDetector.Namespace property.
     */
    @Deprecated
    public String getNamespace() {
        return namespace;
    }

    /**
     * The name of the metric associated with the anomaly detection model.
     *
     * @deprecated Use SingleMetricAnomalyDetector.MetricName property.
     */
    @Deprecated
    public String getMetricName() {
        return metricName;
    }

    /**
     * The metric dimensions associated with the anomaly detection model.
     *
     * @deprecated Use SingleMetricAnomalyDetector.Dimensions property.
     */
    @Deprecated
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
     * The statistic associated with the anomaly detection model.
     *
     * @deprecated Use SingleMetricAnomalyDetector.Stat property.
     */
    @Deprecated
    public String getStat() {
        return stat;
    }

    /**
     * The configuration specifies details about how the anomaly detection model is to be trained, including time ranges
     * to exclude from use for training the model, and the time zone to use for the metric.
     */
    public AnomalyDetectorConfiguration getConfiguration() {
        return configuration;
    }

    /**
     * The current status of the anomaly detector's training.
     */
    public AnomalyDetectorStateValue getStateValue() {
        return stateValue;
    }

    /**
     * This object includes parameters that you can use to provide information about your metric to CloudWatch to help
     * it build more accurate anomaly detection models. Currently, it includes the <code>PeriodicSpikes</code>
     * parameter.
     */
    public MetricCharacteristics getMetricCharacteristics() {
        return metricCharacteristics;
    }

    /**
     * The CloudWatch metric and statistic for this anomaly detector.
     */
    public SingleMetricAnomalyDetector getSingleMetricAnomalyDetector() {
        return singleMetricAnomalyDetector;
    }

    /**
     * The CloudWatch metric math expression for this anomaly detector.
     */
    public MetricMathAnomalyDetector getMetricMathAnomalyDetector() {
        return metricMathAnomalyDetector;
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
        AnomalyDetector that = (AnomalyDetector) other;
        return Objects.equals(this.anomalyDetectorId, that.anomalyDetectorId)
               && Objects.equals(this.namespace, that.namespace)
               && Objects.equals(this.metricName, that.metricName)
               && Objects.equals(this.stat, that.stat)
               && Objects.equals(this.stateValue, that.stateValue)
               && Objects.equals(this.configuration, that.configuration)
               && Objects.equals(this.metricCharacteristics, that.metricCharacteristics)
               && Objects.equals(this.singleMetricAnomalyDetector, that.singleMetricAnomalyDetector)
               && Objects.equals(this.metricMathAnomalyDetector, that.metricMathAnomalyDetector)
               && Objects.equals(this.dimensions, that.dimensions);
    }

    @Override
    public int hashCode() {
        int $hc = Objects.hashCode(anomalyDetectorId);
        $hc = 31 * $hc + Objects.hashCode(namespace);
        $hc = 31 * $hc + Objects.hashCode(metricName);
        $hc = 31 * $hc + Objects.hashCode(dimensions);
        $hc = 31 * $hc + Objects.hashCode(stat);
        $hc = 31 * $hc + Objects.hashCode(configuration);
        $hc = 31 * $hc + Objects.hashCode(stateValue);
        $hc = 31 * $hc + Objects.hashCode(metricCharacteristics);
        $hc = 31 * $hc + Objects.hashCode(singleMetricAnomalyDetector);
        $hc = 31 * $hc + Objects.hashCode(metricMathAnomalyDetector);
        return $hc;
    }

    @Override
    public Schema schema() {
        return $SCHEMA;
    }

    @Override
    public void serializeMembers(ShapeSerializer serializer) {
        if (anomalyDetectorId != null) {
            serializer.writeString($SCHEMA_ANOMALY_DETECTOR_ID, anomalyDetectorId);
        }
        if (namespace != null) {
            serializer.writeString($SCHEMA_NAMESPACE, namespace);
        }
        if (metricName != null) {
            serializer.writeString($SCHEMA_METRIC_NAME, metricName);
        }
        if (dimensions != null) {
            serializer.writeList($SCHEMA_DIMENSIONS, dimensions, dimensions.size(), SharedSerde.DimensionsSerializer.INSTANCE);
        }
        if (stat != null) {
            serializer.writeString($SCHEMA_STAT, stat);
        }
        if (configuration != null) {
            serializer.writeStruct($SCHEMA_CONFIGURATION, configuration);
        }
        if (stateValue != null) {
            serializer.writeString($SCHEMA_STATE_VALUE, stateValue.getValue());
        }
        if (metricCharacteristics != null) {
            serializer.writeStruct($SCHEMA_METRIC_CHARACTERISTICS, metricCharacteristics);
        }
        if (singleMetricAnomalyDetector != null) {
            serializer.writeStruct($SCHEMA_SINGLE_METRIC_ANOMALY_DETECTOR, singleMetricAnomalyDetector);
        }
        if (metricMathAnomalyDetector != null) {
            serializer.writeStruct($SCHEMA_METRIC_MATH_ANOMALY_DETECTOR, metricMathAnomalyDetector);
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T getMemberValue(Schema member) {
        return switch (member.memberIndex()) {
            case 0 -> (T) SchemaUtils.validateSameMember($SCHEMA_ANOMALY_DETECTOR_ID, member, anomalyDetectorId);
            case 1 -> (T) SchemaUtils.validateSameMember($SCHEMA_NAMESPACE, member, namespace);
            case 2 -> (T) SchemaUtils.validateSameMember($SCHEMA_METRIC_NAME, member, metricName);
            case 3 -> (T) SchemaUtils.validateSameMember($SCHEMA_DIMENSIONS, member, dimensions);
            case 4 -> (T) SchemaUtils.validateSameMember($SCHEMA_STAT, member, stat);
            case 5 -> (T) SchemaUtils.validateSameMember($SCHEMA_CONFIGURATION, member, configuration);
            case 6 -> (T) SchemaUtils.validateSameMember($SCHEMA_STATE_VALUE, member, stateValue);
            case 7 -> (T) SchemaUtils.validateSameMember($SCHEMA_METRIC_CHARACTERISTICS, member, metricCharacteristics);
            case 8 -> (T) SchemaUtils.validateSameMember($SCHEMA_SINGLE_METRIC_ANOMALY_DETECTOR, member, singleMetricAnomalyDetector);
            case 9 -> (T) SchemaUtils.validateSameMember($SCHEMA_METRIC_MATH_ANOMALY_DETECTOR, member, metricMathAnomalyDetector);
            default -> throw new IllegalArgumentException("Attempted to get non-existent member: " + member.id());
        };
    }

    /**
     * Create a new builder containing all the current property values of this object.
     *
     * <p><strong>Note:</strong> This method performs only a shallow copy of the original properties.
     *
     * @return a builder for {@link AnomalyDetector}.
     */
    public Builder toBuilder() {
        var builder = new Builder();
        builder.anomalyDetectorId(this.anomalyDetectorId);
        builder.namespace(this.namespace);
        builder.metricName(this.metricName);
        builder.dimensions(this.dimensions);
        builder.stat(this.stat);
        builder.configuration(this.configuration);
        builder.stateValue(this.stateValue);
        builder.metricCharacteristics(this.metricCharacteristics);
        builder.singleMetricAnomalyDetector(this.singleMetricAnomalyDetector);
        builder.metricMathAnomalyDetector(this.metricMathAnomalyDetector);
        return builder;
    }

    /**
     * @return returns a new Builder.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for {@link AnomalyDetector}.
     */
    public static final class Builder implements ShapeBuilder<AnomalyDetector> {
        private String anomalyDetectorId;
        private String namespace;
        private String metricName;
        private List<Dimension> dimensions;
        private String stat;
        private AnomalyDetectorConfiguration configuration;
        private AnomalyDetectorStateValue stateValue;
        private MetricCharacteristics metricCharacteristics;
        private SingleMetricAnomalyDetector singleMetricAnomalyDetector;
        private MetricMathAnomalyDetector metricMathAnomalyDetector;

        private Builder() {}

        @Override
        public Schema schema() {
            return $SCHEMA;
        }

        /**
         * The unique identifier of the anomaly detector.
         *
         * <p>The identifier does not restrict access to a specific anomaly detector in an IAM policy. Permissions for
         * anomaly detector operations apply to all anomaly detectors in the account.
         *
         * @return this builder.
         */
        public Builder anomalyDetectorId(String anomalyDetectorId) {
            this.anomalyDetectorId = anomalyDetectorId;
            return this;
        }

        /**
         * The namespace of the metric associated with the anomaly detection model.
         *
         * @return this builder.
         * @deprecated Use SingleMetricAnomalyDetector.Namespace property.
         */
        @Deprecated
        public Builder namespace(String namespace) {
            this.namespace = namespace;
            return this;
        }

        /**
         * The name of the metric associated with the anomaly detection model.
         *
         * @return this builder.
         * @deprecated Use SingleMetricAnomalyDetector.MetricName property.
         */
        @Deprecated
        public Builder metricName(String metricName) {
            this.metricName = metricName;
            return this;
        }

        /**
         * The metric dimensions associated with the anomaly detection model.
         *
         * @return this builder.
         * @deprecated Use SingleMetricAnomalyDetector.Dimensions property.
         */
        @Deprecated
        public Builder dimensions(List<Dimension> dimensions) {
            this.dimensions = dimensions;
            return this;
        }

        /**
         * The statistic associated with the anomaly detection model.
         *
         * @return this builder.
         * @deprecated Use SingleMetricAnomalyDetector.Stat property.
         */
        @Deprecated
        public Builder stat(String stat) {
            this.stat = stat;
            return this;
        }

        /**
         * The configuration specifies details about how the anomaly detection model is to be trained, including time ranges
         * to exclude from use for training the model, and the time zone to use for the metric.
         *
         * @return this builder.
         */
        public Builder configuration(AnomalyDetectorConfiguration configuration) {
            this.configuration = configuration;
            return this;
        }

        /**
         * The current status of the anomaly detector's training.
         *
         * @return this builder.
         */
        public Builder stateValue(AnomalyDetectorStateValue stateValue) {
            this.stateValue = stateValue;
            return this;
        }

        /**
         * This object includes parameters that you can use to provide information about your metric to CloudWatch to help
         * it build more accurate anomaly detection models. Currently, it includes the <code>PeriodicSpikes</code>
         * parameter.
         *
         * @return this builder.
         */
        public Builder metricCharacteristics(MetricCharacteristics metricCharacteristics) {
            this.metricCharacteristics = metricCharacteristics;
            return this;
        }

        /**
         * The CloudWatch metric and statistic for this anomaly detector.
         *
         * @return this builder.
         */
        public Builder singleMetricAnomalyDetector(SingleMetricAnomalyDetector singleMetricAnomalyDetector) {
            this.singleMetricAnomalyDetector = singleMetricAnomalyDetector;
            return this;
        }

        /**
         * The CloudWatch metric math expression for this anomaly detector.
         *
         * @return this builder.
         */
        public Builder metricMathAnomalyDetector(MetricMathAnomalyDetector metricMathAnomalyDetector) {
            this.metricMathAnomalyDetector = metricMathAnomalyDetector;
            return this;
        }

        @Override
        public AnomalyDetector build() {
            return new AnomalyDetector(this);
        }

        @Override
        @SuppressWarnings("unchecked")
        public void setMemberValue(Schema member, Object value) {
            switch (member.memberIndex()) {
                case 0 -> anomalyDetectorId((String) SchemaUtils.validateSameMember($SCHEMA_ANOMALY_DETECTOR_ID, member, value));
                case 1 -> namespace((String) SchemaUtils.validateSameMember($SCHEMA_NAMESPACE, member, value));
                case 2 -> metricName((String) SchemaUtils.validateSameMember($SCHEMA_METRIC_NAME, member, value));
                case 3 -> dimensions((List<Dimension>) SchemaUtils.validateSameMember($SCHEMA_DIMENSIONS, member, value));
                case 4 -> stat((String) SchemaUtils.validateSameMember($SCHEMA_STAT, member, value));
                case 5 -> configuration((AnomalyDetectorConfiguration) SchemaUtils.validateSameMember($SCHEMA_CONFIGURATION, member, value));
                case 6 -> stateValue((AnomalyDetectorStateValue) SchemaUtils.validateSameMember($SCHEMA_STATE_VALUE, member, value));
                case 7 -> metricCharacteristics((MetricCharacteristics) SchemaUtils.validateSameMember($SCHEMA_METRIC_CHARACTERISTICS, member, value));
                case 8 -> singleMetricAnomalyDetector((SingleMetricAnomalyDetector) SchemaUtils.validateSameMember($SCHEMA_SINGLE_METRIC_ANOMALY_DETECTOR, member, value));
                case 9 -> metricMathAnomalyDetector((MetricMathAnomalyDetector) SchemaUtils.validateSameMember($SCHEMA_METRIC_MATH_ANOMALY_DETECTOR, member, value));
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
                    case 0 -> builder.anomalyDetectorId(de.readString(member));
                    case 1 -> builder.namespace(de.readString(member));
                    case 2 -> builder.metricName(de.readString(member));
                    case 3 -> builder.dimensions(SharedSerde.deserializeDimensions(member, de));
                    case 4 -> builder.stat(de.readString(member));
                    case 5 -> builder.configuration(AnomalyDetectorConfiguration.builder().deserializeMember(de, member).build());
                    case 6 -> builder.stateValue(AnomalyDetectorStateValue.builder().deserializeMember(de, member).build());
                    case 7 -> builder.metricCharacteristics(MetricCharacteristics.builder().deserializeMember(de, member).build());
                    case 8 -> builder.singleMetricAnomalyDetector(SingleMetricAnomalyDetector.builder().deserializeMember(de, member).build());
                    case 9 -> builder.metricMathAnomalyDetector(MetricMathAnomalyDetector.builder().deserializeMember(de, member).build());
                    default -> throw new IllegalArgumentException("Unexpected member: " + member.memberName());
                }
            }
        }
    }
}
