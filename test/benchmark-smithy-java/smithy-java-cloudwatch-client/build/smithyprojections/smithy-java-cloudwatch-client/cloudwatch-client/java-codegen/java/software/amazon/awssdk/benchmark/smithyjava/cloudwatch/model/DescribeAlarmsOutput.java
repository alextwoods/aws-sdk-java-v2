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
public final class DescribeAlarmsOutput implements SerializableStruct {

    public static final Schema $SCHEMA = Schemas.DESCRIBE_ALARMS_OUTPUT;
    private static final Schema $SCHEMA_COMPOSITE_ALARMS = $SCHEMA.member("CompositeAlarms");
    private static final Schema $SCHEMA_METRIC_ALARMS = $SCHEMA.member("MetricAlarms");
    private static final Schema $SCHEMA_LOG_ALARMS = $SCHEMA.member("LogAlarms");
    private static final Schema $SCHEMA_NEXT_TOKEN = $SCHEMA.member("NextToken");

    public static final ShapeId $ID = $SCHEMA.id();

    private final transient List<CompositeAlarm> compositeAlarms;
    private final transient List<MetricAlarm> metricAlarms;
    private final transient List<LogAlarm> logAlarms;
    private final transient String nextToken;

    private DescribeAlarmsOutput(Builder builder) {
        this.compositeAlarms = builder.compositeAlarms == null ? null : Collections.unmodifiableList(builder.compositeAlarms);
        this.metricAlarms = builder.metricAlarms == null ? null : Collections.unmodifiableList(builder.metricAlarms);
        this.logAlarms = builder.logAlarms == null ? null : Collections.unmodifiableList(builder.logAlarms);
        this.nextToken = builder.nextToken;
    }

    /**
     * The information about any composite alarms returned by the operation.
     */
    public List<CompositeAlarm> getCompositeAlarms() {
        if (compositeAlarms == null) {
            return Collections.emptyList();
        }
        return compositeAlarms;
    }

    public boolean hasCompositeAlarms() {
        return compositeAlarms != null;
    }

    /**
     * The information about any metric alarms returned by the operation.
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

    /**
     * The information about any log alarms returned by the operation.
     */
    public List<LogAlarm> getLogAlarms() {
        if (logAlarms == null) {
            return Collections.emptyList();
        }
        return logAlarms;
    }

    public boolean hasLogAlarms() {
        return logAlarms != null;
    }

    /**
     * The token that marks the start of the next batch of returned results.
     */
    public String getNextToken() {
        return nextToken;
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
        DescribeAlarmsOutput that = (DescribeAlarmsOutput) other;
        return Objects.equals(this.nextToken, that.nextToken)
               && Objects.equals(this.compositeAlarms, that.compositeAlarms)
               && Objects.equals(this.metricAlarms, that.metricAlarms)
               && Objects.equals(this.logAlarms, that.logAlarms);
    }

    @Override
    public int hashCode() {
        int $hc = Objects.hashCode(compositeAlarms);
        $hc = 31 * $hc + Objects.hashCode(metricAlarms);
        $hc = 31 * $hc + Objects.hashCode(logAlarms);
        $hc = 31 * $hc + Objects.hashCode(nextToken);
        return $hc;
    }

    @Override
    public Schema schema() {
        return $SCHEMA;
    }

    @Override
    public void serializeMembers(ShapeSerializer serializer) {
        if (compositeAlarms != null) {
            serializer.writeList($SCHEMA_COMPOSITE_ALARMS, compositeAlarms, compositeAlarms.size(), SharedSerde.CompositeAlarmsSerializer.INSTANCE);
        }
        if (metricAlarms != null) {
            serializer.writeList($SCHEMA_METRIC_ALARMS, metricAlarms, metricAlarms.size(), SharedSerde.MetricAlarmsSerializer.INSTANCE);
        }
        if (logAlarms != null) {
            serializer.writeList($SCHEMA_LOG_ALARMS, logAlarms, logAlarms.size(), SharedSerde.LogAlarmsSerializer.INSTANCE);
        }
        if (nextToken != null) {
            serializer.writeString($SCHEMA_NEXT_TOKEN, nextToken);
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T getMemberValue(Schema member) {
        return switch (member.memberIndex()) {
            case 0 -> (T) SchemaUtils.validateSameMember($SCHEMA_COMPOSITE_ALARMS, member, compositeAlarms);
            case 1 -> (T) SchemaUtils.validateSameMember($SCHEMA_METRIC_ALARMS, member, metricAlarms);
            case 2 -> (T) SchemaUtils.validateSameMember($SCHEMA_LOG_ALARMS, member, logAlarms);
            case 3 -> (T) SchemaUtils.validateSameMember($SCHEMA_NEXT_TOKEN, member, nextToken);
            default -> throw new IllegalArgumentException("Attempted to get non-existent member: " + member.id());
        };
    }

    /**
     * Create a new builder containing all the current property values of this object.
     *
     * <p><strong>Note:</strong> This method performs only a shallow copy of the original properties.
     *
     * @return a builder for {@link DescribeAlarmsOutput}.
     */
    public Builder toBuilder() {
        var builder = new Builder();
        builder.compositeAlarms(this.compositeAlarms);
        builder.metricAlarms(this.metricAlarms);
        builder.logAlarms(this.logAlarms);
        builder.nextToken(this.nextToken);
        return builder;
    }

    /**
     * @return returns a new Builder.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for {@link DescribeAlarmsOutput}.
     */
    public static final class Builder implements ShapeBuilder<DescribeAlarmsOutput> {
        private List<CompositeAlarm> compositeAlarms;
        private List<MetricAlarm> metricAlarms;
        private List<LogAlarm> logAlarms;
        private String nextToken;

        private Builder() {}

        @Override
        public Schema schema() {
            return $SCHEMA;
        }

        /**
         * The information about any composite alarms returned by the operation.
         *
         * @return this builder.
         */
        public Builder compositeAlarms(List<CompositeAlarm> compositeAlarms) {
            this.compositeAlarms = compositeAlarms;
            return this;
        }

        /**
         * The information about any metric alarms returned by the operation.
         *
         * @return this builder.
         */
        public Builder metricAlarms(List<MetricAlarm> metricAlarms) {
            this.metricAlarms = metricAlarms;
            return this;
        }

        /**
         * The information about any log alarms returned by the operation.
         *
         * @return this builder.
         */
        public Builder logAlarms(List<LogAlarm> logAlarms) {
            this.logAlarms = logAlarms;
            return this;
        }

        /**
         * The token that marks the start of the next batch of returned results.
         *
         * @return this builder.
         */
        public Builder nextToken(String nextToken) {
            this.nextToken = nextToken;
            return this;
        }

        @Override
        public DescribeAlarmsOutput build() {
            return new DescribeAlarmsOutput(this);
        }

        @Override
        @SuppressWarnings("unchecked")
        public void setMemberValue(Schema member, Object value) {
            switch (member.memberIndex()) {
                case 0 -> compositeAlarms((List<CompositeAlarm>) SchemaUtils.validateSameMember($SCHEMA_COMPOSITE_ALARMS, member, value));
                case 1 -> metricAlarms((List<MetricAlarm>) SchemaUtils.validateSameMember($SCHEMA_METRIC_ALARMS, member, value));
                case 2 -> logAlarms((List<LogAlarm>) SchemaUtils.validateSameMember($SCHEMA_LOG_ALARMS, member, value));
                case 3 -> nextToken((String) SchemaUtils.validateSameMember($SCHEMA_NEXT_TOKEN, member, value));
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
                    case 0 -> builder.compositeAlarms(SharedSerde.deserializeCompositeAlarms(member, de));
                    case 1 -> builder.metricAlarms(SharedSerde.deserializeMetricAlarms(member, de));
                    case 2 -> builder.logAlarms(SharedSerde.deserializeLogAlarms(member, de));
                    case 3 -> builder.nextToken(de.readString(member));
                    default -> throw new IllegalArgumentException("Unexpected member: " + member.memberName());
                }
            }
        }
    }
}
