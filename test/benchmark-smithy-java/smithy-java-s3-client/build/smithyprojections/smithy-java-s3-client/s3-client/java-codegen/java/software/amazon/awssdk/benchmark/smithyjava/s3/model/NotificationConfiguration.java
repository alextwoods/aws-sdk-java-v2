package software.amazon.awssdk.benchmark.smithyjava.s3.model;

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
 * A container for specifying the notification configuration of the bucket. If this element is empty, notifications are
 * turned off for the bucket.
 */
@SmithyGenerated
public final class NotificationConfiguration implements SerializableStruct {

    public static final Schema $SCHEMA = Schemas5.NOTIFICATION_CONFIGURATION;
    private static final Schema $SCHEMA_TOPIC_CONFIGURATIONS = $SCHEMA.member("TopicConfigurations");
    private static final Schema $SCHEMA_QUEUE_CONFIGURATIONS = $SCHEMA.member("QueueConfigurations");
    private static final Schema $SCHEMA_LAMBDA_FUNCTION_CONFIGURATIONS = $SCHEMA.member("LambdaFunctionConfigurations");
    private static final Schema $SCHEMA_EVENT_BRIDGE_CONFIGURATION = $SCHEMA.member("EventBridgeConfiguration");

    public static final ShapeId $ID = $SCHEMA.id();

    private final transient List<TopicConfiguration> topicConfigurations;
    private final transient List<QueueConfiguration> queueConfigurations;
    private final transient List<LambdaFunctionConfiguration> lambdaFunctionConfigurations;
    private final transient EventBridgeConfiguration eventBridgeConfiguration;

    private NotificationConfiguration(Builder builder) {
        this.topicConfigurations = builder.topicConfigurations == null ? null : Collections.unmodifiableList(builder.topicConfigurations);
        this.queueConfigurations = builder.queueConfigurations == null ? null : Collections.unmodifiableList(builder.queueConfigurations);
        this.lambdaFunctionConfigurations = builder.lambdaFunctionConfigurations == null ? null : Collections.unmodifiableList(builder.lambdaFunctionConfigurations);
        this.eventBridgeConfiguration = builder.eventBridgeConfiguration;
    }

    /**
     * The topic to which notifications are sent and the events for which notifications are generated.
     */
    public List<TopicConfiguration> getTopicConfigurations() {
        if (topicConfigurations == null) {
            return Collections.emptyList();
        }
        return topicConfigurations;
    }

    public boolean hasTopicConfigurations() {
        return topicConfigurations != null;
    }

    /**
     * The Amazon Simple Queue Service queues to publish messages to and the events for which to publish messages.
     */
    public List<QueueConfiguration> getQueueConfigurations() {
        if (queueConfigurations == null) {
            return Collections.emptyList();
        }
        return queueConfigurations;
    }

    public boolean hasQueueConfigurations() {
        return queueConfigurations != null;
    }

    /**
     * Describes the Lambda functions to invoke and the events for which to invoke them.
     */
    public List<LambdaFunctionConfiguration> getLambdaFunctionConfigurations() {
        if (lambdaFunctionConfigurations == null) {
            return Collections.emptyList();
        }
        return lambdaFunctionConfigurations;
    }

    public boolean hasLambdaFunctionConfigurations() {
        return lambdaFunctionConfigurations != null;
    }

    /**
     * Enables delivery of events to Amazon EventBridge.
     */
    public EventBridgeConfiguration getEventBridgeConfiguration() {
        return eventBridgeConfiguration;
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
        NotificationConfiguration that = (NotificationConfiguration) other;
        return Objects.equals(this.eventBridgeConfiguration, that.eventBridgeConfiguration)
               && Objects.equals(this.topicConfigurations, that.topicConfigurations)
               && Objects.equals(this.queueConfigurations, that.queueConfigurations)
               && Objects.equals(this.lambdaFunctionConfigurations, that.lambdaFunctionConfigurations);
    }

    @Override
    public int hashCode() {
        int $hc = Objects.hashCode(topicConfigurations);
        $hc = 31 * $hc + Objects.hashCode(queueConfigurations);
        $hc = 31 * $hc + Objects.hashCode(lambdaFunctionConfigurations);
        $hc = 31 * $hc + Objects.hashCode(eventBridgeConfiguration);
        return $hc;
    }

    @Override
    public Schema schema() {
        return $SCHEMA;
    }

    @Override
    public void serializeMembers(ShapeSerializer serializer) {
        if (topicConfigurations != null) {
            serializer.writeList($SCHEMA_TOPIC_CONFIGURATIONS, topicConfigurations, topicConfigurations.size(), SharedSerde.TopicConfigurationListSerializer.INSTANCE);
        }
        if (queueConfigurations != null) {
            serializer.writeList($SCHEMA_QUEUE_CONFIGURATIONS, queueConfigurations, queueConfigurations.size(), SharedSerde.QueueConfigurationListSerializer.INSTANCE);
        }
        if (lambdaFunctionConfigurations != null) {
            serializer.writeList($SCHEMA_LAMBDA_FUNCTION_CONFIGURATIONS, lambdaFunctionConfigurations, lambdaFunctionConfigurations.size(), SharedSerde.LambdaFunctionConfigurationListSerializer.INSTANCE);
        }
        if (eventBridgeConfiguration != null) {
            serializer.writeStruct($SCHEMA_EVENT_BRIDGE_CONFIGURATION, eventBridgeConfiguration);
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T getMemberValue(Schema member) {
        return switch (member.memberIndex()) {
            case 0 -> (T) SchemaUtils.validateSameMember($SCHEMA_TOPIC_CONFIGURATIONS, member, topicConfigurations);
            case 1 -> (T) SchemaUtils.validateSameMember($SCHEMA_QUEUE_CONFIGURATIONS, member, queueConfigurations);
            case 2 -> (T) SchemaUtils.validateSameMember($SCHEMA_LAMBDA_FUNCTION_CONFIGURATIONS, member, lambdaFunctionConfigurations);
            case 3 -> (T) SchemaUtils.validateSameMember($SCHEMA_EVENT_BRIDGE_CONFIGURATION, member, eventBridgeConfiguration);
            default -> throw new IllegalArgumentException("Attempted to get non-existent member: " + member.id());
        };
    }

    /**
     * Create a new builder containing all the current property values of this object.
     *
     * <p><strong>Note:</strong> This method performs only a shallow copy of the original properties.
     *
     * @return a builder for {@link NotificationConfiguration}.
     */
    public Builder toBuilder() {
        var builder = new Builder();
        builder.topicConfigurations(this.topicConfigurations);
        builder.queueConfigurations(this.queueConfigurations);
        builder.lambdaFunctionConfigurations(this.lambdaFunctionConfigurations);
        builder.eventBridgeConfiguration(this.eventBridgeConfiguration);
        return builder;
    }

    /**
     * @return returns a new Builder.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for {@link NotificationConfiguration}.
     */
    public static final class Builder implements ShapeBuilder<NotificationConfiguration> {
        private List<TopicConfiguration> topicConfigurations;
        private List<QueueConfiguration> queueConfigurations;
        private List<LambdaFunctionConfiguration> lambdaFunctionConfigurations;
        private EventBridgeConfiguration eventBridgeConfiguration;

        private Builder() {}

        @Override
        public Schema schema() {
            return $SCHEMA;
        }

        /**
         * The topic to which notifications are sent and the events for which notifications are generated.
         *
         * @return this builder.
         */
        public Builder topicConfigurations(List<TopicConfiguration> topicConfigurations) {
            this.topicConfigurations = topicConfigurations;
            return this;
        }

        /**
         * The Amazon Simple Queue Service queues to publish messages to and the events for which to publish messages.
         *
         * @return this builder.
         */
        public Builder queueConfigurations(List<QueueConfiguration> queueConfigurations) {
            this.queueConfigurations = queueConfigurations;
            return this;
        }

        /**
         * Describes the Lambda functions to invoke and the events for which to invoke them.
         *
         * @return this builder.
         */
        public Builder lambdaFunctionConfigurations(List<LambdaFunctionConfiguration> lambdaFunctionConfigurations) {
            this.lambdaFunctionConfigurations = lambdaFunctionConfigurations;
            return this;
        }

        /**
         * Enables delivery of events to Amazon EventBridge.
         *
         * @return this builder.
         */
        public Builder eventBridgeConfiguration(EventBridgeConfiguration eventBridgeConfiguration) {
            this.eventBridgeConfiguration = eventBridgeConfiguration;
            return this;
        }

        @Override
        public NotificationConfiguration build() {
            return new NotificationConfiguration(this);
        }

        @Override
        @SuppressWarnings("unchecked")
        public void setMemberValue(Schema member, Object value) {
            switch (member.memberIndex()) {
                case 0 -> topicConfigurations((List<TopicConfiguration>) SchemaUtils.validateSameMember($SCHEMA_TOPIC_CONFIGURATIONS, member, value));
                case 1 -> queueConfigurations((List<QueueConfiguration>) SchemaUtils.validateSameMember($SCHEMA_QUEUE_CONFIGURATIONS, member, value));
                case 2 -> lambdaFunctionConfigurations((List<LambdaFunctionConfiguration>) SchemaUtils.validateSameMember($SCHEMA_LAMBDA_FUNCTION_CONFIGURATIONS, member, value));
                case 3 -> eventBridgeConfiguration((EventBridgeConfiguration) SchemaUtils.validateSameMember($SCHEMA_EVENT_BRIDGE_CONFIGURATION, member, value));
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
                    case 0 -> builder.topicConfigurations(SharedSerde.deserializeTopicConfigurationList(member, de));
                    case 1 -> builder.queueConfigurations(SharedSerde.deserializeQueueConfigurationList(member, de));
                    case 2 -> builder.lambdaFunctionConfigurations(SharedSerde.deserializeLambdaFunctionConfigurationList(member, de));
                    case 3 -> builder.eventBridgeConfiguration(EventBridgeConfiguration.builder().deserializeMember(de, member).build());
                    default -> throw new IllegalArgumentException("Unexpected member: " + member.memberName());
                }
            }
        }
    }
}
