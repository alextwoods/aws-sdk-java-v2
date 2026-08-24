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
public final class DescribeAnomalyDetectorsInput implements SerializableStruct {

    public static final Schema $SCHEMA = Schemas.DESCRIBE_ANOMALY_DETECTORS_INPUT;
    private static final Schema $SCHEMA_ANOMALY_DETECTOR_IDS = $SCHEMA.member("AnomalyDetectorIds");
    private static final Schema $SCHEMA_NEXT_TOKEN = $SCHEMA.member("NextToken");
    private static final Schema $SCHEMA_MAX_RESULTS = $SCHEMA.member("MaxResults");
    private static final Schema $SCHEMA_NAMESPACE = $SCHEMA.member("Namespace");
    private static final Schema $SCHEMA_METRIC_NAME = $SCHEMA.member("MetricName");
    private static final Schema $SCHEMA_DIMENSIONS = $SCHEMA.member("Dimensions");
    private static final Schema $SCHEMA_ANOMALY_DETECTOR_TYPES = $SCHEMA.member("AnomalyDetectorTypes");

    public static final ShapeId $ID = $SCHEMA.id();

    private final transient List<String> anomalyDetectorIds;
    private final transient String nextToken;
    private final transient Integer maxResults;
    private final transient String namespace;
    private final transient String metricName;
    private final transient List<Dimension> dimensions;
    private final transient List<AnomalyDetectorType> anomalyDetectorTypes;

    private DescribeAnomalyDetectorsInput(Builder builder) {
        this.anomalyDetectorIds = builder.anomalyDetectorIds == null ? null : Collections.unmodifiableList(builder.anomalyDetectorIds);
        this.nextToken = builder.nextToken;
        this.maxResults = builder.maxResults;
        this.namespace = builder.namespace;
        this.metricName = builder.metricName;
        this.dimensions = builder.dimensions == null ? null : Collections.unmodifiableList(builder.dimensions);
        this.anomalyDetectorTypes = builder.anomalyDetectorTypes == null ? null : Collections.unmodifiableList(builder.anomalyDetectorTypes);
    }

    /**
     * Specifies the unique identifiers of the anomaly detectors to describe. You can specify up to 50 identifiers. If
     * you specify this parameter, you cannot also specify the <code>Namespace</code>, <code>MetricName</code>,
     * <code>Dimensions</code>, or <code>AnomalyDetectorTypes</code> metric filters.
     */
    public List<String> getAnomalyDetectorIds() {
        if (anomalyDetectorIds == null) {
            return Collections.emptyList();
        }
        return anomalyDetectorIds;
    }

    public boolean hasAnomalyDetectorIds() {
        return anomalyDetectorIds != null;
    }

    /**
     * Use the token returned by the previous operation to request the next page of results.
     */
    public String getNextToken() {
        return nextToken;
    }

    /**
     * The maximum number of results to return in one operation. The maximum value that you can specify is 100.
     *
     * <p>To retrieve the remaining results, make another call with the returned <code>NextToken</code> value.
     */
    public Integer getMaxResults() {
        return maxResults;
    }

    /**
     * Limits the results to only the anomaly detection models that are associated with the specified namespace.
     */
    public String getNamespace() {
        return namespace;
    }

    /**
     * Limits the results to only the anomaly detection models that are associated with the specified metric name. If
     * there are multiple metrics with this name in different namespaces that have anomaly detection models, they're all
     * returned.
     */
    public String getMetricName() {
        return metricName;
    }

    /**
     * Limits the results to only the anomaly detection models that are associated with the specified metric dimensions.
     * If there are multiple metrics that have these dimensions and have anomaly detection models associated, they're
     * all returned.
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
     * The anomaly detector types to request when using <code>DescribeAnomalyDetectorsInput</code>. If empty, defaults
     * to <code>SINGLE_METRIC</code>.
     */
    public List<AnomalyDetectorType> getAnomalyDetectorTypes() {
        if (anomalyDetectorTypes == null) {
            return Collections.emptyList();
        }
        return anomalyDetectorTypes;
    }

    public boolean hasAnomalyDetectorTypes() {
        return anomalyDetectorTypes != null;
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
        DescribeAnomalyDetectorsInput that = (DescribeAnomalyDetectorsInput) other;
        return Objects.equals(this.maxResults, that.maxResults)
               && Objects.equals(this.nextToken, that.nextToken)
               && Objects.equals(this.namespace, that.namespace)
               && Objects.equals(this.metricName, that.metricName)
               && Objects.equals(this.anomalyDetectorIds, that.anomalyDetectorIds)
               && Objects.equals(this.dimensions, that.dimensions)
               && Objects.equals(this.anomalyDetectorTypes, that.anomalyDetectorTypes);
    }

    @Override
    public int hashCode() {
        int $hc = Objects.hashCode(anomalyDetectorIds);
        $hc = 31 * $hc + Objects.hashCode(nextToken);
        $hc = 31 * $hc + Objects.hashCode(maxResults);
        $hc = 31 * $hc + Objects.hashCode(namespace);
        $hc = 31 * $hc + Objects.hashCode(metricName);
        $hc = 31 * $hc + Objects.hashCode(dimensions);
        $hc = 31 * $hc + Objects.hashCode(anomalyDetectorTypes);
        return $hc;
    }

    @Override
    public Schema schema() {
        return $SCHEMA;
    }

    @Override
    public void serializeMembers(ShapeSerializer serializer) {
        if (anomalyDetectorIds != null) {
            serializer.writeList($SCHEMA_ANOMALY_DETECTOR_IDS, anomalyDetectorIds, anomalyDetectorIds.size(), SharedSerde.AnomalyDetectorIdsSerializer.INSTANCE);
        }
        if (nextToken != null) {
            serializer.writeString($SCHEMA_NEXT_TOKEN, nextToken);
        }
        if (maxResults != null) {
            serializer.writeInteger($SCHEMA_MAX_RESULTS, maxResults);
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
        if (anomalyDetectorTypes != null) {
            serializer.writeList($SCHEMA_ANOMALY_DETECTOR_TYPES, anomalyDetectorTypes, anomalyDetectorTypes.size(), SharedSerde.AnomalyDetectorTypesSerializer.INSTANCE);
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T getMemberValue(Schema member) {
        return switch (member.memberIndex()) {
            case 0 -> (T) SchemaUtils.validateSameMember($SCHEMA_ANOMALY_DETECTOR_IDS, member, anomalyDetectorIds);
            case 1 -> (T) SchemaUtils.validateSameMember($SCHEMA_NEXT_TOKEN, member, nextToken);
            case 2 -> (T) SchemaUtils.validateSameMember($SCHEMA_MAX_RESULTS, member, maxResults);
            case 3 -> (T) SchemaUtils.validateSameMember($SCHEMA_NAMESPACE, member, namespace);
            case 4 -> (T) SchemaUtils.validateSameMember($SCHEMA_METRIC_NAME, member, metricName);
            case 5 -> (T) SchemaUtils.validateSameMember($SCHEMA_DIMENSIONS, member, dimensions);
            case 6 -> (T) SchemaUtils.validateSameMember($SCHEMA_ANOMALY_DETECTOR_TYPES, member, anomalyDetectorTypes);
            default -> throw new IllegalArgumentException("Attempted to get non-existent member: " + member.id());
        };
    }

    /**
     * Create a new builder containing all the current property values of this object.
     *
     * <p><strong>Note:</strong> This method performs only a shallow copy of the original properties.
     *
     * @return a builder for {@link DescribeAnomalyDetectorsInput}.
     */
    public Builder toBuilder() {
        var builder = new Builder();
        builder.anomalyDetectorIds(this.anomalyDetectorIds);
        builder.nextToken(this.nextToken);
        builder.maxResults(this.maxResults);
        builder.namespace(this.namespace);
        builder.metricName(this.metricName);
        builder.dimensions(this.dimensions);
        builder.anomalyDetectorTypes(this.anomalyDetectorTypes);
        return builder;
    }

    /**
     * @return returns a new Builder.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for {@link DescribeAnomalyDetectorsInput}.
     */
    public static final class Builder implements ShapeBuilder<DescribeAnomalyDetectorsInput> {
        private List<String> anomalyDetectorIds;
        private String nextToken;
        private Integer maxResults;
        private String namespace;
        private String metricName;
        private List<Dimension> dimensions;
        private List<AnomalyDetectorType> anomalyDetectorTypes;

        private Builder() {}

        @Override
        public Schema schema() {
            return $SCHEMA;
        }

        /**
         * Specifies the unique identifiers of the anomaly detectors to describe. You can specify up to 50 identifiers. If
         * you specify this parameter, you cannot also specify the <code>Namespace</code>, <code>MetricName</code>,
         * <code>Dimensions</code>, or <code>AnomalyDetectorTypes</code> metric filters.
         *
         * @return this builder.
         */
        public Builder anomalyDetectorIds(List<String> anomalyDetectorIds) {
            this.anomalyDetectorIds = anomalyDetectorIds;
            return this;
        }

        /**
         * Use the token returned by the previous operation to request the next page of results.
         *
         * @return this builder.
         */
        public Builder nextToken(String nextToken) {
            this.nextToken = nextToken;
            return this;
        }

        /**
         * The maximum number of results to return in one operation. The maximum value that you can specify is 100.
         *
         * <p>To retrieve the remaining results, make another call with the returned <code>NextToken</code> value.
         *
         * @return this builder.
         */
        public Builder maxResults(Integer maxResults) {
            this.maxResults = maxResults;
            return this;
        }

        /**
         * Limits the results to only the anomaly detection models that are associated with the specified namespace.
         *
         * @return this builder.
         */
        public Builder namespace(String namespace) {
            this.namespace = namespace;
            return this;
        }

        /**
         * Limits the results to only the anomaly detection models that are associated with the specified metric name. If
         * there are multiple metrics with this name in different namespaces that have anomaly detection models, they're all
         * returned.
         *
         * @return this builder.
         */
        public Builder metricName(String metricName) {
            this.metricName = metricName;
            return this;
        }

        /**
         * Limits the results to only the anomaly detection models that are associated with the specified metric dimensions.
         * If there are multiple metrics that have these dimensions and have anomaly detection models associated, they're
         * all returned.
         *
         * @return this builder.
         */
        public Builder dimensions(List<Dimension> dimensions) {
            this.dimensions = dimensions;
            return this;
        }

        /**
         * The anomaly detector types to request when using <code>DescribeAnomalyDetectorsInput</code>. If empty, defaults
         * to <code>SINGLE_METRIC</code>.
         *
         * @return this builder.
         */
        public Builder anomalyDetectorTypes(List<AnomalyDetectorType> anomalyDetectorTypes) {
            this.anomalyDetectorTypes = anomalyDetectorTypes;
            return this;
        }

        @Override
        public DescribeAnomalyDetectorsInput build() {
            return new DescribeAnomalyDetectorsInput(this);
        }

        @Override
        @SuppressWarnings("unchecked")
        public void setMemberValue(Schema member, Object value) {
            switch (member.memberIndex()) {
                case 0 -> anomalyDetectorIds((List<String>) SchemaUtils.validateSameMember($SCHEMA_ANOMALY_DETECTOR_IDS, member, value));
                case 1 -> nextToken((String) SchemaUtils.validateSameMember($SCHEMA_NEXT_TOKEN, member, value));
                case 2 -> maxResults((Integer) SchemaUtils.validateSameMember($SCHEMA_MAX_RESULTS, member, value));
                case 3 -> namespace((String) SchemaUtils.validateSameMember($SCHEMA_NAMESPACE, member, value));
                case 4 -> metricName((String) SchemaUtils.validateSameMember($SCHEMA_METRIC_NAME, member, value));
                case 5 -> dimensions((List<Dimension>) SchemaUtils.validateSameMember($SCHEMA_DIMENSIONS, member, value));
                case 6 -> anomalyDetectorTypes((List<AnomalyDetectorType>) SchemaUtils.validateSameMember($SCHEMA_ANOMALY_DETECTOR_TYPES, member, value));
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
                    case 0 -> builder.anomalyDetectorIds(SharedSerde.deserializeAnomalyDetectorIds(member, de));
                    case 1 -> builder.nextToken(de.readString(member));
                    case 2 -> builder.maxResults(de.readInteger(member));
                    case 3 -> builder.namespace(de.readString(member));
                    case 4 -> builder.metricName(de.readString(member));
                    case 5 -> builder.dimensions(SharedSerde.deserializeDimensions(member, de));
                    case 6 -> builder.anomalyDetectorTypes(SharedSerde.deserializeAnomalyDetectorTypes(member, de));
                    default -> throw new IllegalArgumentException("Unexpected member: " + member.memberName());
                }
            }
        }
    }
}
