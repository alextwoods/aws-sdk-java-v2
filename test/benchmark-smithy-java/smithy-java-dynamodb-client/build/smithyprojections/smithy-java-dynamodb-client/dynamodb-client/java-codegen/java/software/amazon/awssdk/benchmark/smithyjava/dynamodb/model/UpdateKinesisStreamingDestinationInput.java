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
public final class UpdateKinesisStreamingDestinationInput implements SerializableStruct {

    public static final Schema $SCHEMA = Schemas.UPDATE_KINESIS_STREAMING_DESTINATION_INPUT;
    private static final Schema $SCHEMA_TABLE_NAME = $SCHEMA.member("TableName");
    private static final Schema $SCHEMA_STREAM_ARN = $SCHEMA.member("StreamArn");
    private static final Schema $SCHEMA_UPDATE_KINESIS_STREAMING_CONFIGURATION = $SCHEMA.member("UpdateKinesisStreamingConfiguration");

    public static final ShapeId $ID = $SCHEMA.id();

    private final transient String tableName;
    private final transient String streamArn;
    private final transient UpdateKinesisStreamingConfiguration updateKinesisStreamingConfiguration;

    private UpdateKinesisStreamingDestinationInput(Builder builder) {
        this.tableName = builder.tableName;
        this.streamArn = builder.streamArn;
        this.updateKinesisStreamingConfiguration = builder.updateKinesisStreamingConfiguration;
    }

    /**
     * The table name for the Kinesis streaming destination input. You can also provide the ARN of the table in this
     * parameter.
     */
    public String getTableName() {
        return tableName;
    }

    /**
     * The Amazon Resource Name (ARN) for the Kinesis stream input.
     */
    public String getStreamArn() {
        return streamArn;
    }

    /**
     * The command to update the Kinesis stream configuration.
     */
    public UpdateKinesisStreamingConfiguration getUpdateKinesisStreamingConfiguration() {
        return updateKinesisStreamingConfiguration;
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
        UpdateKinesisStreamingDestinationInput that = (UpdateKinesisStreamingDestinationInput) other;
        return Objects.equals(this.tableName, that.tableName)
               && Objects.equals(this.streamArn, that.streamArn)
               && Objects.equals(this.updateKinesisStreamingConfiguration, that.updateKinesisStreamingConfiguration);
    }

    @Override
    public int hashCode() {
        int $hc = Objects.hashCode(tableName);
        $hc = 31 * $hc + Objects.hashCode(streamArn);
        $hc = 31 * $hc + Objects.hashCode(updateKinesisStreamingConfiguration);
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
        if (updateKinesisStreamingConfiguration != null) {
            serializer.writeStruct($SCHEMA_UPDATE_KINESIS_STREAMING_CONFIGURATION, updateKinesisStreamingConfiguration);
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T getMemberValue(Schema member) {
        return switch (member.memberIndex()) {
            case 0 -> (T) SchemaUtils.validateSameMember($SCHEMA_TABLE_NAME, member, tableName);
            case 1 -> (T) SchemaUtils.validateSameMember($SCHEMA_STREAM_ARN, member, streamArn);
            case 2 -> (T) SchemaUtils.validateSameMember($SCHEMA_UPDATE_KINESIS_STREAMING_CONFIGURATION, member, updateKinesisStreamingConfiguration);
            default -> throw new IllegalArgumentException("Attempted to get non-existent member: " + member.id());
        };
    }

    /**
     * Create a new builder containing all the current property values of this object.
     *
     * <p><strong>Note:</strong> This method performs only a shallow copy of the original properties.
     *
     * @return a builder for {@link UpdateKinesisStreamingDestinationInput}.
     */
    public Builder toBuilder() {
        var builder = new Builder();
        builder.tableName(this.tableName);
        builder.streamArn(this.streamArn);
        builder.updateKinesisStreamingConfiguration(this.updateKinesisStreamingConfiguration);
        return builder;
    }

    /**
     * @return returns a new Builder.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for {@link UpdateKinesisStreamingDestinationInput}.
     */
    public static final class Builder implements ShapeBuilder<UpdateKinesisStreamingDestinationInput> {
        private final PresenceTracker tracker = PresenceTracker.of($SCHEMA);
        private String tableName;
        private String streamArn;
        private UpdateKinesisStreamingConfiguration updateKinesisStreamingConfiguration;

        private Builder() {}

        @Override
        public Schema schema() {
            return $SCHEMA;
        }

        /**
         * The table name for the Kinesis streaming destination input. You can also provide the ARN of the table in this
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
         * The Amazon Resource Name (ARN) for the Kinesis stream input.
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
         * The command to update the Kinesis stream configuration.
         *
         * @return this builder.
         */
        public Builder updateKinesisStreamingConfiguration(UpdateKinesisStreamingConfiguration updateKinesisStreamingConfiguration) {
            this.updateKinesisStreamingConfiguration = updateKinesisStreamingConfiguration;
            return this;
        }

        @Override
        public UpdateKinesisStreamingDestinationInput build() {
            tracker.validate();
            return new UpdateKinesisStreamingDestinationInput(this);
        }

        @Override
        @SuppressWarnings("unchecked")
        public void setMemberValue(Schema member, Object value) {
            switch (member.memberIndex()) {
                case 0 -> tableName((String) SchemaUtils.validateSameMember($SCHEMA_TABLE_NAME, member, value));
                case 1 -> streamArn((String) SchemaUtils.validateSameMember($SCHEMA_STREAM_ARN, member, value));
                case 2 -> updateKinesisStreamingConfiguration((UpdateKinesisStreamingConfiguration) SchemaUtils.validateSameMember($SCHEMA_UPDATE_KINESIS_STREAMING_CONFIGURATION, member, value));
                default -> ShapeBuilder.super.setMemberValue(member, value);
            }
        }

        @Override
        public ShapeBuilder<UpdateKinesisStreamingDestinationInput> errorCorrection() {
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
                    case 2 -> builder.updateKinesisStreamingConfiguration(UpdateKinesisStreamingConfiguration.builder().deserializeMember(de, member).build());
                    default -> throw new IllegalArgumentException("Unexpected member: " + member.memberName());
                }
            }
        }
    }
}
