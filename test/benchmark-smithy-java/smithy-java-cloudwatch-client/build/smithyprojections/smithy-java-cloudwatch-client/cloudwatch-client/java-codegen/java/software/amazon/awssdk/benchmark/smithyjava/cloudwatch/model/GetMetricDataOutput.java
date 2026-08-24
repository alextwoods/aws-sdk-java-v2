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
public final class GetMetricDataOutput implements SerializableStruct {

    public static final Schema $SCHEMA = Schemas.GET_METRIC_DATA_OUTPUT;
    private static final Schema $SCHEMA_METRIC_DATA_RESULTS = $SCHEMA.member("MetricDataResults");
    private static final Schema $SCHEMA_NEXT_TOKEN = $SCHEMA.member("NextToken");
    private static final Schema $SCHEMA_MESSAGES = $SCHEMA.member("Messages");

    public static final ShapeId $ID = $SCHEMA.id();

    private final transient List<MetricDataResult> metricDataResults;
    private final transient String nextToken;
    private final transient List<MessageData> messages;

    private GetMetricDataOutput(Builder builder) {
        this.metricDataResults = builder.metricDataResults == null ? null : Collections.unmodifiableList(builder.metricDataResults);
        this.nextToken = builder.nextToken;
        this.messages = builder.messages == null ? null : Collections.unmodifiableList(builder.messages);
    }

    /**
     * The metrics that are returned, including the metric name, namespace, and dimensions.
     */
    public List<MetricDataResult> getMetricDataResults() {
        if (metricDataResults == null) {
            return Collections.emptyList();
        }
        return metricDataResults;
    }

    public boolean hasMetricDataResults() {
        return metricDataResults != null;
    }

    /**
     * A token that marks the next batch of returned results.
     */
    public String getNextToken() {
        return nextToken;
    }

    /**
     * Contains a message about this <code>GetMetricData</code> operation, if the operation results in such a message.
     * An example of a message that might be returned is <code>Maximum number of allowed metrics exceeded</code>. If
     * there is a message, as much of the operation as possible is still executed.
     *
     * <p>A message appears here only if it is related to the global <code>GetMetricData</code> operation. Any message
     * about a specific metric returned by the operation appears in the <code>MetricDataResult</code> object returned
     * for that metric.
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
        GetMetricDataOutput that = (GetMetricDataOutput) other;
        return Objects.equals(this.nextToken, that.nextToken)
               && Objects.equals(this.metricDataResults, that.metricDataResults)
               && Objects.equals(this.messages, that.messages);
    }

    @Override
    public int hashCode() {
        int $hc = Objects.hashCode(metricDataResults);
        $hc = 31 * $hc + Objects.hashCode(nextToken);
        $hc = 31 * $hc + Objects.hashCode(messages);
        return $hc;
    }

    @Override
    public Schema schema() {
        return $SCHEMA;
    }

    @Override
    public void serializeMembers(ShapeSerializer serializer) {
        if (metricDataResults != null) {
            serializer.writeList($SCHEMA_METRIC_DATA_RESULTS, metricDataResults, metricDataResults.size(), SharedSerde.MetricDataResultsSerializer.INSTANCE);
        }
        if (nextToken != null) {
            serializer.writeString($SCHEMA_NEXT_TOKEN, nextToken);
        }
        if (messages != null) {
            serializer.writeList($SCHEMA_MESSAGES, messages, messages.size(), SharedSerde.MetricDataResultMessagesSerializer.INSTANCE);
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T getMemberValue(Schema member) {
        return switch (member.memberIndex()) {
            case 0 -> (T) SchemaUtils.validateSameMember($SCHEMA_METRIC_DATA_RESULTS, member, metricDataResults);
            case 1 -> (T) SchemaUtils.validateSameMember($SCHEMA_NEXT_TOKEN, member, nextToken);
            case 2 -> (T) SchemaUtils.validateSameMember($SCHEMA_MESSAGES, member, messages);
            default -> throw new IllegalArgumentException("Attempted to get non-existent member: " + member.id());
        };
    }

    /**
     * Create a new builder containing all the current property values of this object.
     *
     * <p><strong>Note:</strong> This method performs only a shallow copy of the original properties.
     *
     * @return a builder for {@link GetMetricDataOutput}.
     */
    public Builder toBuilder() {
        var builder = new Builder();
        builder.metricDataResults(this.metricDataResults);
        builder.nextToken(this.nextToken);
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
     * Builder for {@link GetMetricDataOutput}.
     */
    public static final class Builder implements ShapeBuilder<GetMetricDataOutput> {
        private List<MetricDataResult> metricDataResults;
        private String nextToken;
        private List<MessageData> messages;

        private Builder() {}

        @Override
        public Schema schema() {
            return $SCHEMA;
        }

        /**
         * The metrics that are returned, including the metric name, namespace, and dimensions.
         *
         * @return this builder.
         */
        public Builder metricDataResults(List<MetricDataResult> metricDataResults) {
            this.metricDataResults = metricDataResults;
            return this;
        }

        /**
         * A token that marks the next batch of returned results.
         *
         * @return this builder.
         */
        public Builder nextToken(String nextToken) {
            this.nextToken = nextToken;
            return this;
        }

        /**
         * Contains a message about this <code>GetMetricData</code> operation, if the operation results in such a message.
         * An example of a message that might be returned is <code>Maximum number of allowed metrics exceeded</code>. If
         * there is a message, as much of the operation as possible is still executed.
         *
         * <p>A message appears here only if it is related to the global <code>GetMetricData</code> operation. Any message
         * about a specific metric returned by the operation appears in the <code>MetricDataResult</code> object returned
         * for that metric.
         *
         * @return this builder.
         */
        public Builder messages(List<MessageData> messages) {
            this.messages = messages;
            return this;
        }

        @Override
        public GetMetricDataOutput build() {
            return new GetMetricDataOutput(this);
        }

        @Override
        @SuppressWarnings("unchecked")
        public void setMemberValue(Schema member, Object value) {
            switch (member.memberIndex()) {
                case 0 -> metricDataResults((List<MetricDataResult>) SchemaUtils.validateSameMember($SCHEMA_METRIC_DATA_RESULTS, member, value));
                case 1 -> nextToken((String) SchemaUtils.validateSameMember($SCHEMA_NEXT_TOKEN, member, value));
                case 2 -> messages((List<MessageData>) SchemaUtils.validateSameMember($SCHEMA_MESSAGES, member, value));
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
                    case 0 -> builder.metricDataResults(SharedSerde.deserializeMetricDataResults(member, de));
                    case 1 -> builder.nextToken(de.readString(member));
                    case 2 -> builder.messages(SharedSerde.deserializeMetricDataResultMessages(member, de));
                    default -> throw new IllegalArgumentException("Unexpected member: " + member.memberName());
                }
            }
        }
    }
}
