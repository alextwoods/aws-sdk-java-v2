package software.amazon.awssdk.benchmark.smithyjava.s3.model;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import software.amazon.smithy.java.core.schema.PresenceTracker;
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
 * A container for specifying the configuration for Lambda notifications.
 */
@SmithyGenerated
public final class LambdaFunctionConfiguration implements SerializableStruct {

    public static final Schema $SCHEMA = Schemas2.LAMBDA_FUNCTION_CONFIGURATION;
    private static final Schema $SCHEMA_ID = $SCHEMA.member("Id");
    private static final Schema $SCHEMA_LAMBDA_FUNCTION_ARN = $SCHEMA.member("LambdaFunctionArn");
    private static final Schema $SCHEMA_EVENTS = $SCHEMA.member("Events");
    private static final Schema $SCHEMA_FILTER = $SCHEMA.member("Filter");

    public static final ShapeId $ID = $SCHEMA.id();

    private final transient String id;
    private final transient String lambdaFunctionArn;
    private final transient List<Event> events;
    private final transient NotificationConfigurationFilter filter;

    private LambdaFunctionConfiguration(Builder builder) {
        this.id = builder.id;
        this.lambdaFunctionArn = builder.lambdaFunctionArn;
        this.events = Collections.unmodifiableList(builder.events);
        this.filter = builder.filter;
    }

    public String getId() {
        return id;
    }

    /**
     * The Amazon Resource Name (ARN) of the Lambda function that Amazon S3 invokes when the specified event type
     * occurs.
     */
    public String getLambdaFunctionArn() {
        return lambdaFunctionArn;
    }

    /**
     * The Amazon S3 bucket event for which to invoke the Lambda function. For more information, see <a href="https://docs.aws.amazon.com/AmazonS3/latest/dev/NotificationHowTo.html">Supported Event
     * Types</a> in the <i>Amazon S3 User Guide</i>.
     */
    public List<Event> getEvents() {
        return events;
    }

    public boolean hasEvents() {
        return true;
    }

    public NotificationConfigurationFilter getFilter() {
        return filter;
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
        LambdaFunctionConfiguration that = (LambdaFunctionConfiguration) other;
        return Objects.equals(this.id, that.id)
               && Objects.equals(this.lambdaFunctionArn, that.lambdaFunctionArn)
               && Objects.equals(this.filter, that.filter)
               && Objects.equals(this.events, that.events);
    }

    @Override
    public int hashCode() {
        int $hc = Objects.hashCode(id);
        $hc = 31 * $hc + Objects.hashCode(lambdaFunctionArn);
        $hc = 31 * $hc + Objects.hashCode(events);
        $hc = 31 * $hc + Objects.hashCode(filter);
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
        serializer.writeString($SCHEMA_LAMBDA_FUNCTION_ARN, lambdaFunctionArn);
        serializer.writeList($SCHEMA_EVENTS, events, events.size(), SharedSerde.EventListSerializer.INSTANCE);
        if (filter != null) {
            serializer.writeStruct($SCHEMA_FILTER, filter);
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T getMemberValue(Schema member) {
        return switch (member.memberIndex()) {
            case 0 -> (T) SchemaUtils.validateSameMember($SCHEMA_LAMBDA_FUNCTION_ARN, member, lambdaFunctionArn);
            case 1 -> (T) SchemaUtils.validateSameMember($SCHEMA_EVENTS, member, events);
            case 2 -> (T) SchemaUtils.validateSameMember($SCHEMA_ID, member, id);
            case 3 -> (T) SchemaUtils.validateSameMember($SCHEMA_FILTER, member, filter);
            default -> throw new IllegalArgumentException("Attempted to get non-existent member: " + member.id());
        };
    }

    /**
     * Create a new builder containing all the current property values of this object.
     *
     * <p><strong>Note:</strong> This method performs only a shallow copy of the original properties.
     *
     * @return a builder for {@link LambdaFunctionConfiguration}.
     */
    public Builder toBuilder() {
        var builder = new Builder();
        builder.id(this.id);
        builder.lambdaFunctionArn(this.lambdaFunctionArn);
        builder.events(this.events);
        builder.filter(this.filter);
        return builder;
    }

    /**
     * @return returns a new Builder.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for {@link LambdaFunctionConfiguration}.
     */
    public static final class Builder implements ShapeBuilder<LambdaFunctionConfiguration> {
        private final PresenceTracker tracker = PresenceTracker.of($SCHEMA);
        private String id;
        private String lambdaFunctionArn;
        private List<Event> events;
        private NotificationConfigurationFilter filter;

        private Builder() {}

        @Override
        public Schema schema() {
            return $SCHEMA;
        }

        /**
         * @return this builder.
         */
        public Builder id(String id) {
            this.id = id;
            return this;
        }

        /**
         * The Amazon Resource Name (ARN) of the Lambda function that Amazon S3 invokes when the specified event type
         * occurs.
         *
         * <p><strong>Required</strong>
         * @return this builder.
         */
        public Builder lambdaFunctionArn(String lambdaFunctionArn) {
            this.lambdaFunctionArn = Objects.requireNonNull(lambdaFunctionArn, "lambdaFunctionArn cannot be null");
            tracker.setMember($SCHEMA_LAMBDA_FUNCTION_ARN);
            return this;
        }

        /**
         * The Amazon S3 bucket event for which to invoke the Lambda function. For more information, see <a href="https://docs.aws.amazon.com/AmazonS3/latest/dev/NotificationHowTo.html">Supported Event
         * Types</a> in the <i>Amazon S3 User Guide</i>.
         *
         * <p><strong>Required</strong>
         * @return this builder.
         */
        public Builder events(List<Event> events) {
            this.events = Objects.requireNonNull(events, "events cannot be null");
            tracker.setMember($SCHEMA_EVENTS);
            return this;
        }

        /**
         * @return this builder.
         */
        public Builder filter(NotificationConfigurationFilter filter) {
            this.filter = filter;
            return this;
        }

        @Override
        public LambdaFunctionConfiguration build() {
            tracker.validate();
            return new LambdaFunctionConfiguration(this);
        }

        @Override
        @SuppressWarnings("unchecked")
        public void setMemberValue(Schema member, Object value) {
            switch (member.memberIndex()) {
                case 0 -> lambdaFunctionArn((String) SchemaUtils.validateSameMember($SCHEMA_LAMBDA_FUNCTION_ARN, member, value));
                case 1 -> events((List<Event>) SchemaUtils.validateSameMember($SCHEMA_EVENTS, member, value));
                case 2 -> id((String) SchemaUtils.validateSameMember($SCHEMA_ID, member, value));
                case 3 -> filter((NotificationConfigurationFilter) SchemaUtils.validateSameMember($SCHEMA_FILTER, member, value));
                default -> ShapeBuilder.super.setMemberValue(member, value);
            }
        }

        @Override
        public ShapeBuilder<LambdaFunctionConfiguration> errorCorrection() {
            if (tracker.allSet()) {
                return this;
            }
            if (!tracker.checkMember($SCHEMA_LAMBDA_FUNCTION_ARN)) {
                lambdaFunctionArn("");
            }
            if (!tracker.checkMember($SCHEMA_EVENTS)) {
                events(Collections.emptyList());
            }
            return this;
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
                    case 0 -> builder.lambdaFunctionArn(de.readString(member));
                    case 1 -> builder.events(SharedSerde.deserializeEventList(member, de));
                    case 2 -> builder.id(de.readString(member));
                    case 3 -> builder.filter(NotificationConfigurationFilter.builder().deserializeMember(de, member).build());
                    default -> throw new IllegalArgumentException("Unexpected member: " + member.memberName());
                }
            }
        }
    }
}
