package software.amazon.awssdk.benchmark.smithyjava.cloudwatch.model;

import java.time.Instant;
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
 * A <code>GetMetricData</code> call returns an array of <code>MetricDataResult</code> structures. Each of these
 * structures includes the data points for that metric, along with the timestamps of those data points and other
 * identifying information.
 */
@SmithyGenerated
public final class MetricDataResult implements SerializableStruct {

    public static final Schema $SCHEMA = Schemas.METRIC_DATA_RESULT;
    private static final Schema $SCHEMA_ID = $SCHEMA.member("Id");
    private static final Schema $SCHEMA_LABEL = $SCHEMA.member("Label");
    private static final Schema $SCHEMA_TIMESTAMPS = $SCHEMA.member("Timestamps");
    private static final Schema $SCHEMA_VALUES = $SCHEMA.member("Values");
    private static final Schema $SCHEMA_STATUS_CODE = $SCHEMA.member("StatusCode");
    private static final Schema $SCHEMA_MESSAGES = $SCHEMA.member("Messages");

    public static final ShapeId $ID = $SCHEMA.id();

    private final transient String id;
    private final transient String label;
    private final transient List<Instant> timestamps;
    private final transient List<Double> values;
    private final transient StatusCode statusCode;
    private final transient List<MessageData> messages;

    private MetricDataResult(Builder builder) {
        this.id = builder.id;
        this.label = builder.label;
        this.timestamps = builder.timestamps == null ? null : Collections.unmodifiableList(builder.timestamps);
        this.values = builder.values == null ? null : Collections.unmodifiableList(builder.values);
        this.statusCode = builder.statusCode;
        this.messages = builder.messages == null ? null : Collections.unmodifiableList(builder.messages);
    }

    /**
     * The short name you specified to represent this metric.
     */
    public String getId() {
        return id;
    }

    /**
     * The human-readable label associated with the data.
     */
    public String getLabel() {
        return label;
    }

    /**
     * The timestamps for the data points, formatted in Unix timestamp format. The number of timestamps always matches
     * the number of values and the value for Timestamps[x] is Values[x].
     */
    public List<Instant> getTimestamps() {
        if (timestamps == null) {
            return Collections.emptyList();
        }
        return timestamps;
    }

    public boolean hasTimestamps() {
        return timestamps != null;
    }

    /**
     * The data points for the metric corresponding to <code>Timestamps</code>. The number of values always matches the
     * number of timestamps and the timestamp for Values[x] is Timestamps[x].
     */
    public List<Double> getValues() {
        if (values == null) {
            return Collections.emptyList();
        }
        return values;
    }

    public boolean hasValues() {
        return values != null;
    }

    /**
     * The status of the returned data. <code>Complete</code> indicates that all data points in the requested time range
     * were returned. <code>PartialData</code> means that an incomplete set of data points were returned. You can use
     * the <code>NextToken</code> value that was returned and repeat your request to get more data points.
     * <code>NextToken</code> is not returned if you are performing a math expression. <code>InternalError</code>
     * indicates that an error occurred. Retry your request using <code>NextToken</code>, if present.
     */
    public StatusCode getStatusCode() {
        return statusCode;
    }

    /**
     * A list of messages with additional information about the data returned.
     */
    public List<MessageData> getMessages() {
        if (messages == null) {
            return Collections.emptyList();
        }
        return messages;
    }

    public boolean hasMessages() {
        return messages != null;
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
        MetricDataResult that = (MetricDataResult) other;
        return Objects.equals(this.id, that.id)
               && Objects.equals(this.label, that.label)
               && Objects.equals(this.statusCode, that.statusCode)
               && Objects.equals(this.timestamps, that.timestamps)
               && Objects.equals(this.values, that.values)
               && Objects.equals(this.messages, that.messages);
    }

    @Override
    public int hashCode() {
        int $hc = Objects.hashCode(id);
        $hc = 31 * $hc + Objects.hashCode(label);
        $hc = 31 * $hc + Objects.hashCode(timestamps);
        $hc = 31 * $hc + Objects.hashCode(values);
        $hc = 31 * $hc + Objects.hashCode(statusCode);
        $hc = 31 * $hc + Objects.hashCode(messages);
        return $hc;
    }

    @Override
    public Schema schema() {
        return $SCHEMA;
    }

    @Override
    public void serializeMembers(ShapeSerializer serializer) {
        if (id != null) {
            serializer.writeString($SCHEMA_ID, id);
        }
        if (label != null) {
            serializer.writeString($SCHEMA_LABEL, label);
        }
        if (timestamps != null) {
            serializer.writeList($SCHEMA_TIMESTAMPS, timestamps, timestamps.size(), SharedSerde.TimestampsSerializer.INSTANCE);
        }
        if (values != null) {
            serializer.writeList($SCHEMA_VALUES, values, values.size(), SharedSerde.DatapointValuesSerializer.INSTANCE);
        }
        if (statusCode != null) {
            serializer.writeString($SCHEMA_STATUS_CODE, statusCode.getValue());
        }
        if (messages != null) {
            serializer.writeList($SCHEMA_MESSAGES, messages, messages.size(), SharedSerde.MetricDataResultMessagesSerializer.INSTANCE);
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T getMemberValue(Schema member) {
        return switch (member.memberIndex()) {
            case 0 -> (T) SchemaUtils.validateSameMember($SCHEMA_ID, member, id);
            case 1 -> (T) SchemaUtils.validateSameMember($SCHEMA_LABEL, member, label);
            case 2 -> (T) SchemaUtils.validateSameMember($SCHEMA_TIMESTAMPS, member, timestamps);
            case 3 -> (T) SchemaUtils.validateSameMember($SCHEMA_VALUES, member, values);
            case 4 -> (T) SchemaUtils.validateSameMember($SCHEMA_STATUS_CODE, member, statusCode);
            case 5 -> (T) SchemaUtils.validateSameMember($SCHEMA_MESSAGES, member, messages);
            default -> throw new IllegalArgumentException("Attempted to get non-existent member: " + member.id());
        };
    }

    /**
     * Create a new builder containing all the current property values of this object.
     *
     * <p><strong>Note:</strong> This method performs only a shallow copy of the original properties.
     *
     * @return a builder for {@link MetricDataResult}.
     */
    public Builder toBuilder() {
        var builder = new Builder();
        builder.id(this.id);
        builder.label(this.label);
        builder.timestamps(this.timestamps);
        builder.values(this.values);
        builder.statusCode(this.statusCode);
        builder.messages(this.messages);
        return builder;
    }

    /**
     * @return returns a new Builder.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for {@link MetricDataResult}.
     */
    public static final class Builder implements ShapeBuilder<MetricDataResult> {
        private String id;
        private String label;
        private List<Instant> timestamps;
        private List<Double> values;
        private StatusCode statusCode;
        private List<MessageData> messages;

        private Builder() {}

        @Override
        public Schema schema() {
            return $SCHEMA;
        }

        /**
         * The short name you specified to represent this metric.
         *
         * @return this builder.
         */
        public Builder id(String id) {
            this.id = id;
            return this;
        }

        /**
         * The human-readable label associated with the data.
         *
         * @return this builder.
         */
        public Builder label(String label) {
            this.label = label;
            return this;
        }

        /**
         * The timestamps for the data points, formatted in Unix timestamp format. The number of timestamps always matches
         * the number of values and the value for Timestamps[x] is Values[x].
         *
         * @return this builder.
         */
        public Builder timestamps(List<Instant> timestamps) {
            this.timestamps = timestamps;
            return this;
        }

        /**
         * The data points for the metric corresponding to <code>Timestamps</code>. The number of values always matches the
         * number of timestamps and the timestamp for Values[x] is Timestamps[x].
         *
         * @return this builder.
         */
        public Builder values(List<Double> values) {
            this.values = values;
            return this;
        }

        /**
         * The status of the returned data. <code>Complete</code> indicates that all data points in the requested time range
         * were returned. <code>PartialData</code> means that an incomplete set of data points were returned. You can use
         * the <code>NextToken</code> value that was returned and repeat your request to get more data points.
         * <code>NextToken</code> is not returned if you are performing a math expression. <code>InternalError</code>
         * indicates that an error occurred. Retry your request using <code>NextToken</code>, if present.
         *
         * @return this builder.
         */
        public Builder statusCode(StatusCode statusCode) {
            this.statusCode = statusCode;
            return this;
        }

        /**
         * A list of messages with additional information about the data returned.
         *
         * @return this builder.
         */
        public Builder messages(List<MessageData> messages) {
            this.messages = messages;
            return this;
        }

        @Override
        public MetricDataResult build() {
            return new MetricDataResult(this);
        }

        @Override
        @SuppressWarnings("unchecked")
        public void setMemberValue(Schema member, Object value) {
            switch (member.memberIndex()) {
                case 0 -> id((String) SchemaUtils.validateSameMember($SCHEMA_ID, member, value));
                case 1 -> label((String) SchemaUtils.validateSameMember($SCHEMA_LABEL, member, value));
                case 2 -> timestamps((List<Instant>) SchemaUtils.validateSameMember($SCHEMA_TIMESTAMPS, member, value));
                case 3 -> values((List<Double>) SchemaUtils.validateSameMember($SCHEMA_VALUES, member, value));
                case 4 -> statusCode((StatusCode) SchemaUtils.validateSameMember($SCHEMA_STATUS_CODE, member, value));
                case 5 -> messages((List<MessageData>) SchemaUtils.validateSameMember($SCHEMA_MESSAGES, member, value));
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
                    case 0 -> builder.id(de.readString(member));
                    case 1 -> builder.label(de.readString(member));
                    case 2 -> builder.timestamps(SharedSerde.deserializeTimestamps(member, de));
                    case 3 -> builder.values(SharedSerde.deserializeDatapointValues(member, de));
                    case 4 -> builder.statusCode(StatusCode.builder().deserializeMember(de, member).build());
                    case 5 -> builder.messages(SharedSerde.deserializeMetricDataResultMessages(member, de));
                    default -> throw new IllegalArgumentException("Unexpected member: " + member.memberName());
                }
            }
        }
    }
}
