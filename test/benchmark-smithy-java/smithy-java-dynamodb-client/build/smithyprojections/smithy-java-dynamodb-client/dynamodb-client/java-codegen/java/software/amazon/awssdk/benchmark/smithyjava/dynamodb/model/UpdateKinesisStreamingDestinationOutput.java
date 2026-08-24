package software.amazon.awssdk.benchmark.smithyjava.dynamodb.model;

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
public final class UpdateKinesisStreamingDestinationOutput implements SerializableStruct {

    public static final Schema $SCHEMA = Schemas.UPDATE_KINESIS_STREAMING_DESTINATION_OUTPUT;
    private static final Schema $SCHEMA_TABLE_NAME = $SCHEMA.member("TableName");
    private static final Schema $SCHEMA_STREAM_ARN = $SCHEMA.member("StreamArn");
    private static final Schema $SCHEMA_DESTINATION_STATUS = $SCHEMA.member("DestinationStatus");
    private static final Schema $SCHEMA_UPDATE_KINESIS_STREAMING_CONFIGURATION = $SCHEMA.member("UpdateKinesisStreamingConfiguration");

    public static final ShapeId $ID = $SCHEMA.id();

    private final transient String tableName;
    private final transient String streamArn;
    private final transient DestinationStatus destinationStatus;
    private final transient UpdateKinesisStreamingConfiguration updateKinesisStreamingConfiguration;

    private UpdateKinesisStreamingDestinationOutput(Builder builder) {
        this.tableName = builder.tableName;
        this.streamArn = builder.streamArn;
        this.destinationStatus = builder.destinationStatus;
        this.updateKinesisStreamingConfiguration = builder.updateKinesisStreamingConfiguration;
    }

    /**
     * The table name for the Kinesis streaming destination output.
     */
    public String getTableName() {
        return tableName;
    }

    /**
     * The ARN for the Kinesis stream input.
     */
    public String getStreamArn() {
        return streamArn;
    }

    /**
     * The status of the attempt to update the Kinesis streaming destination output.
     */
    public DestinationStatus getDestinationStatus() {
        return destinationStatus;
    }

    /**
     * The command to update the Kinesis streaming destination configuration.
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
        UpdateKinesisStreamingDestinationOutput that = (UpdateKinesisStreamingDestinationOutput) other;
        return Objects.equals(this.tableName, that.tableName)
               && Objects.equals(this.streamArn, that.streamArn)
               && Objects.equals(this.destinationStatus, that.destinationStatus)
               && Objects.equals(this.updateKinesisStreamingConfiguration, that.updateKinesisStreamingConfiguration);
    }

    @Override
    public int hashCode() {
        int $hc = Objects.hashCode(tableName);
        $hc = 31 * $hc + Objects.hashCode(streamArn);
        $hc = 31 * $hc + Objects.hashCode(destinationStatus);
        $hc = 31 * $hc + Objects.hashCode(updateKinesisStreamingConfiguration);
        return $hc;
    }

    @Override
    public Schema schema() {
        return $SCHEMA;
    }

    @Override
    public void serializeMembers(ShapeSerializer serializer) {
        if (tableName != null) {
            serializer.writeString($SCHEMA_TABLE_NAME, tableName);
        }
        if (streamArn != null) {
            serializer.writeString($SCHEMA_STREAM_ARN, streamArn);
        }
        if (destinationStatus != null) {
            serializer.writeString($SCHEMA_DESTINATION_STATUS, destinationStatus.getValue());
        }
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
            case 2 -> (T) SchemaUtils.validateSameMember($SCHEMA_DESTINATION_STATUS, member, destinationStatus);
            case 3 -> (T) SchemaUtils.validateSameMember($SCHEMA_UPDATE_KINESIS_STREAMING_CONFIGURATION, member, updateKinesisStreamingConfiguration);
            default -> throw new IllegalArgumentException("Attempted to get non-existent member: " + member.id());
        };
    }

    /**
     * Create a new builder containing all the current property values of this object.
     *
     * <p><strong>Note:</strong> This method performs only a shallow copy of the original properties.
     *
     * @return a builder for {@link UpdateKinesisStreamingDestinationOutput}.
     */
    public Builder toBuilder() {
        var builder = new Builder();
        builder.tableName(this.tableName);
        builder.streamArn(this.streamArn);
        builder.destinationStatus(this.destinationStatus);
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
     * Builder for {@link UpdateKinesisStreamingDestinationOutput}.
     */
    public static final class Builder implements ShapeBuilder<UpdateKinesisStreamingDestinationOutput> {
        private String tableName;
        private String streamArn;
        private DestinationStatus destinationStatus;
        private UpdateKinesisStreamingConfiguration updateKinesisStreamingConfiguration;

        private Builder() {}

        @Override
        public Schema schema() {
            return $SCHEMA;
        }

        /**
         * The table name for the Kinesis streaming destination output.
         *
         * @return this builder.
         */
        public Builder tableName(String tableName) {
            this.tableName = tableName;
            return this;
        }

        /**
         * The ARN for the Kinesis stream input.
         *
         * @return this builder.
         */
        public Builder streamArn(String streamArn) {
            this.streamArn = streamArn;
            return this;
        }

        /**
         * The status of the attempt to update the Kinesis streaming destination output.
         *
         * @return this builder.
         */
        public Builder destinationStatus(DestinationStatus destinationStatus) {
            this.destinationStatus = destinationStatus;
            return this;
        }

        /**
         * The command to update the Kinesis streaming destination configuration.
         *
         * @return this builder.
         */
        public Builder updateKinesisStreamingConfiguration(UpdateKinesisStreamingConfiguration updateKinesisStreamingConfiguration) {
            this.updateKinesisStreamingConfiguration = updateKinesisStreamingConfiguration;
            return this;
        }

        @Override
        public UpdateKinesisStreamingDestinationOutput build() {
            return new UpdateKinesisStreamingDestinationOutput(this);
        }

        @Override
        @SuppressWarnings("unchecked")
        public void setMemberValue(Schema member, Object value) {
            switch (member.memberIndex()) {
                case 0 -> tableName((String) SchemaUtils.validateSameMember($SCHEMA_TABLE_NAME, member, value));
                case 1 -> streamArn((String) SchemaUtils.validateSameMember($SCHEMA_STREAM_ARN, member, value));
                case 2 -> destinationStatus((DestinationStatus) SchemaUtils.validateSameMember($SCHEMA_DESTINATION_STATUS, member, value));
                case 3 -> updateKinesisStreamingConfiguration((UpdateKinesisStreamingConfiguration) SchemaUtils.validateSameMember($SCHEMA_UPDATE_KINESIS_STREAMING_CONFIGURATION, member, value));
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
                    case 0 -> builder.tableName(de.readString(member));
                    case 1 -> builder.streamArn(de.readString(member));
                    case 2 -> builder.destinationStatus(DestinationStatus.builder().deserializeMember(de, member).build());
                    case 3 -> builder.updateKinesisStreamingConfiguration(UpdateKinesisStreamingConfiguration.builder().deserializeMember(de, member).build());
                    default -> throw new IllegalArgumentException("Unexpected member: " + member.memberName());
                }
            }
        }
    }
}
