package software.amazon.awssdk.benchmark.smithyjava.dynamodb.model;

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

@SmithyGenerated
public final class DisableKinesisStreamingDestinationInput implements SerializableStruct {

    public static final Schema $SCHEMA = Schemas.DISABLE_KINESIS_STREAMING_DESTINATION_INPUT;
    private static final Schema $SCHEMA_TABLE_NAME = $SCHEMA.member("TableName");
    private static final Schema $SCHEMA_STREAM_ARN = $SCHEMA.member("StreamArn");
    private static final Schema $SCHEMA_ENABLE_KINESIS_STREAMING_CONFIGURATION = $SCHEMA.member("EnableKinesisStreamingConfiguration");

    public static final ShapeId $ID = $SCHEMA.id();

    private final transient String tableName;
    private final transient String streamArn;
    private final transient EnableKinesisStreamingConfiguration enableKinesisStreamingConfiguration;

    private DisableKinesisStreamingDestinationInput(Builder builder) {
        this.tableName = builder.tableName;
        this.streamArn = builder.streamArn;
        this.enableKinesisStreamingConfiguration = builder.enableKinesisStreamingConfiguration;
    }

    /**
     * The name of the DynamoDB table. You can also provide the Amazon Resource Name (ARN) of the table in this
     * parameter.
     */
    public String getTableName() {
        return tableName;
    }

    /**
     * The ARN for a Kinesis data stream.
     */
    public String getStreamArn() {
        return streamArn;
    }

    /**
     * The source for the Kinesis streaming information that is being enabled.
     */
    public EnableKinesisStreamingConfiguration getEnableKinesisStreamingConfiguration() {
        return enableKinesisStreamingConfiguration;
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
        DisableKinesisStreamingDestinationInput that = (DisableKinesisStreamingDestinationInput) other;
        return Objects.equals(this.tableName, that.tableName)
               && Objects.equals(this.streamArn, that.streamArn)
               && Objects.equals(this.enableKinesisStreamingConfiguration, that.enableKinesisStreamingConfiguration);
    }

    @Override
    public int hashCode() {
        int $hc = Objects.hashCode(tableName);
        $hc = 31 * $hc + Objects.hashCode(streamArn);
        $hc = 31 * $hc + Objects.hashCode(enableKinesisStreamingConfiguration);
        return $hc;
    }

    @Override
    public Schema schema() {
        return $SCHEMA;
    }

    @Override
    public void serializeMembers(ShapeSerializer serializer) {
        serializer.writeString($SCHEMA_TABLE_NAME, tableName);
        serializer.writeString($SCHEMA_STREAM_ARN, streamArn);
        if (enableKinesisStreamingConfiguration != null) {
            serializer.writeStruct($SCHEMA_ENABLE_KINESIS_STREAMING_CONFIGURATION, enableKinesisStreamingConfiguration);
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T getMemberValue(Schema member) {
        return switch (member.memberIndex()) {
            case 0 -> (T) SchemaUtils.validateSameMember($SCHEMA_TABLE_NAME, member, tableName);
            case 1 -> (T) SchemaUtils.validateSameMember($SCHEMA_STREAM_ARN, member, streamArn);
            case 2 -> (T) SchemaUtils.validateSameMember($SCHEMA_ENABLE_KINESIS_STREAMING_CONFIGURATION, member, enableKinesisStreamingConfiguration);
            default -> throw new IllegalArgumentException("Attempted to get non-existent member: " + member.id());
        };
    }

    /**
     * Create a new builder containing all the current property values of this object.
     *
     * <p><strong>Note:</strong> This method performs only a shallow copy of the original properties.
     *
     * @return a builder for {@link DisableKinesisStreamingDestinationInput}.
     */
    public Builder toBuilder() {
        var builder = new Builder();
        builder.tableName(this.tableName);
        builder.streamArn(this.streamArn);
        builder.enableKinesisStreamingConfiguration(this.enableKinesisStreamingConfiguration);
        return builder;
    }

    /**
     * @return returns a new Builder.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for {@link DisableKinesisStreamingDestinationInput}.
     */
    public static final class Builder implements ShapeBuilder<DisableKinesisStreamingDestinationInput> {
        private final PresenceTracker tracker = PresenceTracker.of($SCHEMA);
        private String tableName;
        private String streamArn;
        private EnableKinesisStreamingConfiguration enableKinesisStreamingConfiguration;

        private Builder() {}

        @Override
        public Schema schema() {
            return $SCHEMA;
        }

        /**
         * The name of the DynamoDB table. You can also provide the Amazon Resource Name (ARN) of the table in this
         * parameter.
         *
         * <p><strong>Required</strong>
         * @return this builder.
         */
        public Builder tableName(String tableName) {
            this.tableName = Objects.requireNonNull(tableName, "tableName cannot be null");
            tracker.setMember($SCHEMA_TABLE_NAME);
            return this;
        }

        /**
         * The ARN for a Kinesis data stream.
         *
         * <p><strong>Required</strong>
         * @return this builder.
         */
        public Builder streamArn(String streamArn) {
            this.streamArn = Objects.requireNonNull(streamArn, "streamArn cannot be null");
            tracker.setMember($SCHEMA_STREAM_ARN);
            return this;
        }

        /**
         * The source for the Kinesis streaming information that is being enabled.
         *
         * @return this builder.
         */
        public Builder enableKinesisStreamingConfiguration(EnableKinesisStreamingConfiguration enableKinesisStreamingConfiguration) {
            this.enableKinesisStreamingConfiguration = enableKinesisStreamingConfiguration;
            return this;
        }

        @Override
        public DisableKinesisStreamingDestinationInput build() {
            tracker.validate();
            return new DisableKinesisStreamingDestinationInput(this);
        }

        @Override
        @SuppressWarnings("unchecked")
        public void setMemberValue(Schema member, Object value) {
            switch (member.memberIndex()) {
                case 0 -> tableName((String) SchemaUtils.validateSameMember($SCHEMA_TABLE_NAME, member, value));
                case 1 -> streamArn((String) SchemaUtils.validateSameMember($SCHEMA_STREAM_ARN, member, value));
                case 2 -> enableKinesisStreamingConfiguration((EnableKinesisStreamingConfiguration) SchemaUtils.validateSameMember($SCHEMA_ENABLE_KINESIS_STREAMING_CONFIGURATION, member, value));
                default -> ShapeBuilder.super.setMemberValue(member, value);
            }
        }

        @Override
        public ShapeBuilder<DisableKinesisStreamingDestinationInput> errorCorrection() {
            if (tracker.allSet()) {
                return this;
            }
            if (!tracker.checkMember($SCHEMA_TABLE_NAME)) {
                tableName("");
            }
            if (!tracker.checkMember($SCHEMA_STREAM_ARN)) {
                streamArn("");
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
                    case 0 -> builder.tableName(de.readString(member));
                    case 1 -> builder.streamArn(de.readString(member));
                    case 2 -> builder.enableKinesisStreamingConfiguration(EnableKinesisStreamingConfiguration.builder().deserializeMember(de, member).build());
                    default -> throw new IllegalArgumentException("Unexpected member: " + member.memberName());
                }
            }
        }
    }
}
