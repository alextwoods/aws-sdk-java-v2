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
 * The configuration specifies details about how the anomaly detection model is to be trained, including time ranges to
 * exclude from use for training the model and the time zone to use for the metric.
 */
@SmithyGenerated
public final class AnomalyDetectorConfiguration implements SerializableStruct {

    public static final Schema $SCHEMA = Schemas.ANOMALY_DETECTOR_CONFIGURATION;
    private static final Schema $SCHEMA_EXCLUDED_TIME_RANGES = $SCHEMA.member("ExcludedTimeRanges");
    private static final Schema $SCHEMA_METRIC_TIMEZONE = $SCHEMA.member("MetricTimezone");

    public static final ShapeId $ID = $SCHEMA.id();

    private final transient List<Range> excludedTimeRanges;
    private final transient String metricTimezone;

    private AnomalyDetectorConfiguration(Builder builder) {
        this.excludedTimeRanges = builder.excludedTimeRanges == null ? null : Collections.unmodifiableList(builder.excludedTimeRanges);
        this.metricTimezone = builder.metricTimezone;
    }

    /**
     * An array of time ranges to exclude from use when the anomaly detection model is trained. Use this to make sure
     * that events that could cause unusual values for the metric, such as deployments, aren't used when CloudWatch
     * creates the model.
     */
    public List<Range> getExcludedTimeRanges() {
        if (excludedTimeRanges == null) {
            return Collections.emptyList();
        }
        return excludedTimeRanges;
    }

    public boolean hasExcludedTimeRanges() {
        return excludedTimeRanges != null;
    }

    /**
     * The time zone to use for the metric. This is useful to enable the model to automatically account for daylight
     * savings time changes if the metric is sensitive to such time changes.
     *
     * <p>To specify a time zone, use the name of the time zone as specified in the standard tz database. For more
     * information, see <a href="https://en.wikipedia.org/wiki/Tz_database">tz database</a>.
     */
    public String getMetricTimezone() {
        return metricTimezone;
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
        AnomalyDetectorConfiguration that = (AnomalyDetectorConfiguration) other;
        return Objects.equals(this.metricTimezone, that.metricTimezone)
               && Objects.equals(this.excludedTimeRanges, that.excludedTimeRanges);
    }

    @Override
    public int hashCode() {
        int $hc = Objects.hashCode(excludedTimeRanges);
        $hc = 31 * $hc + Objects.hashCode(metricTimezone);
        return $hc;
    }

    @Override
    public Schema schema() {
        return $SCHEMA;
    }

    @Override
    public void serializeMembers(ShapeSerializer serializer) {
        if (excludedTimeRanges != null) {
            serializer.writeList($SCHEMA_EXCLUDED_TIME_RANGES, excludedTimeRanges, excludedTimeRanges.size(), SharedSerde.AnomalyDetectorExcludedTimeRangesSerializer.INSTANCE);
        }
        if (metricTimezone != null) {
            serializer.writeString($SCHEMA_METRIC_TIMEZONE, metricTimezone);
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T getMemberValue(Schema member) {
        return switch (member.memberIndex()) {
            case 0 -> (T) SchemaUtils.validateSameMember($SCHEMA_EXCLUDED_TIME_RANGES, member, excludedTimeRanges);
            case 1 -> (T) SchemaUtils.validateSameMember($SCHEMA_METRIC_TIMEZONE, member, metricTimezone);
            default -> throw new IllegalArgumentException("Attempted to get non-existent member: " + member.id());
        };
    }

    /**
     * Create a new builder containing all the current property values of this object.
     *
     * <p><strong>Note:</strong> This method performs only a shallow copy of the original properties.
     *
     * @return a builder for {@link AnomalyDetectorConfiguration}.
     */
    public Builder toBuilder() {
        var builder = new Builder();
        builder.excludedTimeRanges(this.excludedTimeRanges);
        builder.metricTimezone(this.metricTimezone);
        return builder;
    }

    /**
     * @return returns a new Builder.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for {@link AnomalyDetectorConfiguration}.
     */
    public static final class Builder implements ShapeBuilder<AnomalyDetectorConfiguration> {
        private List<Range> excludedTimeRanges;
        private String metricTimezone;

        private Builder() {}

        @Override
        public Schema schema() {
            return $SCHEMA;
        }

        /**
         * An array of time ranges to exclude from use when the anomaly detection model is trained. Use this to make sure
         * that events that could cause unusual values for the metric, such as deployments, aren't used when CloudWatch
         * creates the model.
         *
         * @return this builder.
         */
        public Builder excludedTimeRanges(List<Range> excludedTimeRanges) {
            this.excludedTimeRanges = excludedTimeRanges;
            return this;
        }

        /**
         * The time zone to use for the metric. This is useful to enable the model to automatically account for daylight
         * savings time changes if the metric is sensitive to such time changes.
         *
         * <p>To specify a time zone, use the name of the time zone as specified in the standard tz database. For more
         * information, see <a href="https://en.wikipedia.org/wiki/Tz_database">tz database</a>.
         *
         * @return this builder.
         */
        public Builder metricTimezone(String metricTimezone) {
            this.metricTimezone = metricTimezone;
            return this;
        }

        @Override
        public AnomalyDetectorConfiguration build() {
            return new AnomalyDetectorConfiguration(this);
        }

        @Override
        @SuppressWarnings("unchecked")
        public void setMemberValue(Schema member, Object value) {
            switch (member.memberIndex()) {
                case 0 -> excludedTimeRanges((List<Range>) SchemaUtils.validateSameMember($SCHEMA_EXCLUDED_TIME_RANGES, member, value));
                case 1 -> metricTimezone((String) SchemaUtils.validateSameMember($SCHEMA_METRIC_TIMEZONE, member, value));
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
                    case 0 -> builder.excludedTimeRanges(SharedSerde.deserializeAnomalyDetectorExcludedTimeRanges(member, de));
                    case 1 -> builder.metricTimezone(de.readString(member));
                    default -> throw new IllegalArgumentException("Unexpected member: " + member.memberName());
                }
            }
        }
    }
}
