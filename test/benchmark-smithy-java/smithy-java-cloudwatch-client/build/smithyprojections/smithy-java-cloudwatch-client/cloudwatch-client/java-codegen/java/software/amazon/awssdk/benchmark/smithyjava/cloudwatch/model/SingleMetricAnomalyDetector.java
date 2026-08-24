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
 * Designates the CloudWatch metric and statistic that provides the time series the anomaly detector uses as input. If
 * you have enabled unified cross-account observability, and this account is a monitoring account, the metric can be in
 * the same account or a source account.
 */
@SmithyGenerated
public final class SingleMetricAnomalyDetector implements SerializableStruct {

    public static final Schema $SCHEMA = Schemas.SINGLE_METRIC_ANOMALY_DETECTOR;
    private static final Schema $SCHEMA_ACCOUNT_ID = $SCHEMA.member("AccountId");
    private static final Schema $SCHEMA_NAMESPACE = $SCHEMA.member("Namespace");
    private static final Schema $SCHEMA_METRIC_NAME = $SCHEMA.member("MetricName");
    private static final Schema $SCHEMA_DIMENSIONS = $SCHEMA.member("Dimensions");
    private static final Schema $SCHEMA_STAT = $SCHEMA.member("Stat");

    public static final ShapeId $ID = $SCHEMA.id();

    private final transient String accountId;
    private final transient String namespace;
    private final transient String metricName;
    private final transient List<Dimension> dimensions;
    private final transient String stat;

    private SingleMetricAnomalyDetector(Builder builder) {
        this.accountId = builder.accountId;
        this.namespace = builder.namespace;
        this.metricName = builder.metricName;
        this.dimensions = builder.dimensions == null ? null : Collections.unmodifiableList(builder.dimensions);
        this.stat = builder.stat;
    }

    /**
     * If the CloudWatch metric that provides the time series that the anomaly detector uses as input is in another
     * account, specify that account ID here. If you omit this parameter, the current account is used.
     */
    public String getAccountId() {
        return accountId;
    }

    /**
     * The namespace of the metric to create the anomaly detection model for.
     */
    public String getNamespace() {
        return namespace;
    }

    /**
     * The name of the metric to create the anomaly detection model for.
     */
    public String getMetricName() {
        return metricName;
    }

    /**
     * The metric dimensions to create the anomaly detection model for.
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
     * The statistic to use for the metric and anomaly detection model.
     */
    public String getStat() {
        return stat;
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
        SingleMetricAnomalyDetector that = (SingleMetricAnomalyDetector) other;
        return Objects.equals(this.accountId, that.accountId)
               && Objects.equals(this.namespace, that.namespace)
               && Objects.equals(this.metricName, that.metricName)
               && Objects.equals(this.stat, that.stat)
               && Objects.equals(this.dimensions, that.dimensions);
    }

    @Override
    public int hashCode() {
        int $hc = Objects.hashCode(accountId);
        $hc = 31 * $hc + Objects.hashCode(namespace);
        $hc = 31 * $hc + Objects.hashCode(metricName);
        $hc = 31 * $hc + Objects.hashCode(dimensions);
        $hc = 31 * $hc + Objects.hashCode(stat);
        return $hc;
    }

    @Override
    public Schema schema() {
        return $SCHEMA;
    }

    @Override
    public void serializeMembers(ShapeSerializer serializer) {
        if (accountId != null) {
            serializer.writeString($SCHEMA_ACCOUNT_ID, accountId);
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
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T getMemberValue(Schema member) {
        return switch (member.memberIndex()) {
            case 0 -> (T) SchemaUtils.validateSameMember($SCHEMA_ACCOUNT_ID, member, accountId);
            case 1 -> (T) SchemaUtils.validateSameMember($SCHEMA_NAMESPACE, member, namespace);
            case 2 -> (T) SchemaUtils.validateSameMember($SCHEMA_METRIC_NAME, member, metricName);
            case 3 -> (T) SchemaUtils.validateSameMember($SCHEMA_DIMENSIONS, member, dimensions);
            case 4 -> (T) SchemaUtils.validateSameMember($SCHEMA_STAT, member, stat);
            default -> throw new IllegalArgumentException("Attempted to get non-existent member: " + member.id());
        };
    }

    /**
     * Create a new builder containing all the current property values of this object.
     *
     * <p><strong>Note:</strong> This method performs only a shallow copy of the original properties.
     *
     * @return a builder for {@link SingleMetricAnomalyDetector}.
     */
    public Builder toBuilder() {
        var builder = new Builder();
        builder.accountId(this.accountId);
        builder.namespace(this.namespace);
        builder.metricName(this.metricName);
        builder.dimensions(this.dimensions);
        builder.stat(this.stat);
        return builder;
    }

    /**
     * @return returns a new Builder.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for {@link SingleMetricAnomalyDetector}.
     */
    public static final class Builder implements ShapeBuilder<SingleMetricAnomalyDetector> {
        private String accountId;
        private String namespace;
        private String metricName;
        private List<Dimension> dimensions;
        private String stat;

        private Builder() {}

        @Override
        public Schema schema() {
            return $SCHEMA;
        }

        /**
         * If the CloudWatch metric that provides the time series that the anomaly detector uses as input is in another
         * account, specify that account ID here. If you omit this parameter, the current account is used.
         *
         * @return this builder.
         */
        public Builder accountId(String accountId) {
            this.accountId = accountId;
            return this;
        }

        /**
         * The namespace of the metric to create the anomaly detection model for.
         *
         * @return this builder.
         */
        public Builder namespace(String namespace) {
            this.namespace = namespace;
            return this;
        }

        /**
         * The name of the metric to create the anomaly detection model for.
         *
         * @return this builder.
         */
        public Builder metricName(String metricName) {
            this.metricName = metricName;
            return this;
        }

        /**
         * The metric dimensions to create the anomaly detection model for.
         *
         * @return this builder.
         */
        public Builder dimensions(List<Dimension> dimensions) {
            this.dimensions = dimensions;
            return this;
        }

        /**
         * The statistic to use for the metric and anomaly detection model.
         *
         * @return this builder.
         */
        public Builder stat(String stat) {
            this.stat = stat;
            return this;
        }

        @Override
        public SingleMetricAnomalyDetector build() {
            return new SingleMetricAnomalyDetector(this);
        }

        @Override
        @SuppressWarnings("unchecked")
        public void setMemberValue(Schema member, Object value) {
            switch (member.memberIndex()) {
                case 0 -> accountId((String) SchemaUtils.validateSameMember($SCHEMA_ACCOUNT_ID, member, value));
                case 1 -> namespace((String) SchemaUtils.validateSameMember($SCHEMA_NAMESPACE, member, value));
                case 2 -> metricName((String) SchemaUtils.validateSameMember($SCHEMA_METRIC_NAME, member, value));
                case 3 -> dimensions((List<Dimension>) SchemaUtils.validateSameMember($SCHEMA_DIMENSIONS, member, value));
                case 4 -> stat((String) SchemaUtils.validateSameMember($SCHEMA_STAT, member, value));
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
                    case 0 -> builder.accountId(de.readString(member));
                    case 1 -> builder.namespace(de.readString(member));
                    case 2 -> builder.metricName(de.readString(member));
                    case 3 -> builder.dimensions(SharedSerde.deserializeDimensions(member, de));
                    case 4 -> builder.stat(de.readString(member));
                    default -> throw new IllegalArgumentException("Unexpected member: " + member.memberName());
                }
            }
        }
    }
}
