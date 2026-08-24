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
public final class DescribeAlarmsForMetricOutput implements SerializableStruct {

    public static final Schema $SCHEMA = Schemas.DESCRIBE_ALARMS_FOR_METRIC_OUTPUT;
    private static final Schema $SCHEMA_METRIC_ALARMS = $SCHEMA.member("MetricAlarms");

    public static final ShapeId $ID = $SCHEMA.id();

    private final transient List<MetricAlarm> metricAlarms;

    private DescribeAlarmsForMetricOutput(Builder builder) {
        this.metricAlarms = builder.metricAlarms == null ? null : Collections.unmodifiableList(builder.metricAlarms);
    }

    /**
     * The information for each alarm with the specified metric.
     */
    public List<MetricAlarm> getMetricAlarms() {
        if (metricAlarms == null) {
            return Collections.emptyList();
        }
        return metricAlarms;
    }

    public boolean hasMetricAlarms() {
        return metricAlarms != null;
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
        DescribeAlarmsForMetricOutput that = (DescribeAlarmsForMetricOutput) other;
        return Objects.equals(this.metricAlarms, that.metricAlarms);
    }

    @Override
    public int hashCode() {
        int $hc = Objects.hashCode(metricAlarms);
        return $hc;
    }

    @Override
    public Schema schema() {
        return $SCHEMA;
    }

    @Override
    public void serializeMembers(ShapeSerializer serializer) {
        if (metricAlarms != null) {
            serializer.writeList($SCHEMA_METRIC_ALARMS, metricAlarms, metricAlarms.size(), SharedSerde.MetricAlarmsSerializer.INSTANCE);
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T getMemberValue(Schema member) {
        return switch (member.memberIndex()) {
            case 0 -> (T) SchemaUtils.validateSameMember($SCHEMA_METRIC_ALARMS, member, metricAlarms);
            default -> throw new IllegalArgumentException("Attempted to get non-existent member: " + member.id());
        };
    }

    /**
     * Create a new builder containing all the current property values of this object.
     *
     * <p><strong>Note:</strong> This method performs only a shallow copy of the original properties.
     *
     * @return a builder for {@link DescribeAlarmsForMetricOutput}.
     */
    public Builder toBuilder() {
        var builder = new Builder();
        builder.metricAlarms(this.metricAlarms);
        return builder;
    }

    /**
     * @return returns a new Builder.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for {@link DescribeAlarmsForMetricOutput}.
     */
    public static final class Builder implements ShapeBuilder<DescribeAlarmsForMetricOutput> {
        private List<MetricAlarm> metricAlarms;

        private Builder() {}

        @Override
        public Schema schema() {
            return $SCHEMA;
        }

        /**
         * The information for each alarm with the specified metric.
         *
         * @return this builder.
         */
        public Builder metricAlarms(List<MetricAlarm> metricAlarms) {
            this.metricAlarms = metricAlarms;
            return this;
        }

        @Override
        public DescribeAlarmsForMetricOutput build() {
            return new DescribeAlarmsForMetricOutput(this);
        }

        @Override
        @SuppressWarnings("unchecked")
        public void setMemberValue(Schema member, Object value) {
            switch (member.memberIndex()) {
                case 0 -> metricAlarms((List<MetricAlarm>) SchemaUtils.validateSameMember($SCHEMA_METRIC_ALARMS, member, value));
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
                    case 0 -> builder.metricAlarms(SharedSerde.deserializeMetricAlarms(member, de));
                    default -> throw new IllegalArgumentException("Unexpected member: " + member.memberName());
                }
            }
        }
    }
}
