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
 * This structure contains a metric namespace and optionally, a list of metric names, to either include in a metric
 * stream or exclude from a metric stream.
 *
 * <p>A metric stream's filters can include up to 1000 total names. This limit applies to the sum of namespace names and
 * metric names in the filters. For example, this could include 10 metric namespace filters with 99 metrics each, or 20
 * namespace filters with 49 metrics specified in each filter.
 */
@SmithyGenerated
public final class MetricStreamFilter implements SerializableStruct {

    public static final Schema $SCHEMA = Schemas.METRIC_STREAM_FILTER;
    private static final Schema $SCHEMA_NAMESPACE = $SCHEMA.member("Namespace");
    private static final Schema $SCHEMA_METRIC_NAMES = $SCHEMA.member("MetricNames");

    public static final ShapeId $ID = $SCHEMA.id();

    private final transient String namespace;
    private final transient List<String> metricNames;

    private MetricStreamFilter(Builder builder) {
        this.namespace = builder.namespace;
        this.metricNames = builder.metricNames == null ? null : Collections.unmodifiableList(builder.metricNames);
    }

    /**
     * The name of the metric namespace for this filter.
     *
     * <p>The namespace can contain only ASCII printable characters (ASCII range 32 through 126). It must contain at
     * least one non-whitespace character.
     */
    public String getNamespace() {
        return namespace;
    }

    /**
     * The names of the metrics to either include or exclude from the metric stream.
     *
     * <p>If you omit this parameter, all metrics in the namespace are included or excluded, depending on whether this
     * filter is specified as an exclude filter or an include filter.
     *
     * <p>Each metric name can contain only ASCII printable characters (ASCII range 32 through 126). Each metric name
     * must contain at least one non-whitespace character.
     */
    public List<String> getMetricNames() {
        if (metricNames == null) {
            return Collections.emptyList();
        }
        return metricNames;
    }

    public boolean hasMetricNames() {
        return metricNames != null;
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
        MetricStreamFilter that = (MetricStreamFilter) other;
        return Objects.equals(this.namespace, that.namespace)
               && Objects.equals(this.metricNames, that.metricNames);
    }

    @Override
    public int hashCode() {
        int $hc = Objects.hashCode(namespace);
        $hc = 31 * $hc + Objects.hashCode(metricNames);
        return $hc;
    }

    @Override
    public Schema schema() {
        return $SCHEMA;
    }

    @Override
    public void serializeMembers(ShapeSerializer serializer) {
        if (namespace != null) {
            serializer.writeString($SCHEMA_NAMESPACE, namespace);
        }
        if (metricNames != null) {
            serializer.writeList($SCHEMA_METRIC_NAMES, metricNames, metricNames.size(), SharedSerde.MetricStreamFilterMetricNamesSerializer.INSTANCE);
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T getMemberValue(Schema member) {
        return switch (member.memberIndex()) {
            case 0 -> (T) SchemaUtils.validateSameMember($SCHEMA_NAMESPACE, member, namespace);
            case 1 -> (T) SchemaUtils.validateSameMember($SCHEMA_METRIC_NAMES, member, metricNames);
            default -> throw new IllegalArgumentException("Attempted to get non-existent member: " + member.id());
        };
    }

    /**
     * Create a new builder containing all the current property values of this object.
     *
     * <p><strong>Note:</strong> This method performs only a shallow copy of the original properties.
     *
     * @return a builder for {@link MetricStreamFilter}.
     */
    public Builder toBuilder() {
        var builder = new Builder();
        builder.namespace(this.namespace);
        builder.metricNames(this.metricNames);
        return builder;
    }

    /**
     * @return returns a new Builder.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for {@link MetricStreamFilter}.
     */
    public static final class Builder implements ShapeBuilder<MetricStreamFilter> {
        private String namespace;
        private List<String> metricNames;

        private Builder() {}

        @Override
        public Schema schema() {
            return $SCHEMA;
        }

        /**
         * The name of the metric namespace for this filter.
         *
         * <p>The namespace can contain only ASCII printable characters (ASCII range 32 through 126). It must contain at
         * least one non-whitespace character.
         *
         * @return this builder.
         */
        public Builder namespace(String namespace) {
            this.namespace = namespace;
            return this;
        }

        /**
         * The names of the metrics to either include or exclude from the metric stream.
         *
         * <p>If you omit this parameter, all metrics in the namespace are included or excluded, depending on whether this
         * filter is specified as an exclude filter or an include filter.
         *
         * <p>Each metric name can contain only ASCII printable characters (ASCII range 32 through 126). Each metric name
         * must contain at least one non-whitespace character.
         *
         * @return this builder.
         */
        public Builder metricNames(List<String> metricNames) {
            this.metricNames = metricNames;
            return this;
        }

        @Override
        public MetricStreamFilter build() {
            return new MetricStreamFilter(this);
        }

        @Override
        @SuppressWarnings("unchecked")
        public void setMemberValue(Schema member, Object value) {
            switch (member.memberIndex()) {
                case 0 -> namespace((String) SchemaUtils.validateSameMember($SCHEMA_NAMESPACE, member, value));
                case 1 -> metricNames((List<String>) SchemaUtils.validateSameMember($SCHEMA_METRIC_NAMES, member, value));
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
                    case 0 -> builder.namespace(de.readString(member));
                    case 1 -> builder.metricNames(SharedSerde.deserializeMetricStreamFilterMetricNames(member, de));
                    default -> throw new IllegalArgumentException("Unexpected member: " + member.memberName());
                }
            }
        }
    }
}
