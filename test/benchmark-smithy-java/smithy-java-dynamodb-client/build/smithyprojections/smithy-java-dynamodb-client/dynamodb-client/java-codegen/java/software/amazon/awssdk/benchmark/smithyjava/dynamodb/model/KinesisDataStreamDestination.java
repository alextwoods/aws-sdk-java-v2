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

/**
 * Describes a Kinesis data stream destination.
 */
@SmithyGenerated
public final class KinesisDataStreamDestination implements SerializableStruct {

    public static final Schema $SCHEMA = Schemas.KINESIS_DATA_STREAM_DESTINATION;
    private static final Schema $SCHEMA_STREAM_ARN = $SCHEMA.member("StreamArn");
    private static final Schema $SCHEMA_DESTINATION_STATUS = $SCHEMA.member("DestinationStatus");
    private static final Schema $SCHEMA_DESTINATION_STATUS_DESCRIPTION = $SCHEMA.member("DestinationStatusDescription");
    private static final Schema $SCHEMA_APPROXIMATE_CREATION_DATE_TIME_PRECISION = $SCHEMA.member("ApproximateCreationDateTimePrecision");

    public static final ShapeId $ID = $SCHEMA.id();

    private final transient String streamArn;
    private final transient DestinationStatus destinationStatus;
    private final transient String destinationStatusDescription;
    private final transient ApproximateCreationDateTimePrecision approximateCreationDateTimePrecision;

    private KinesisDataStreamDestination(Builder builder) {
        this.streamArn = builder.streamArn;
        this.destinationStatus = builder.destinationStatus;
        this.destinationStatusDescription = builder.destinationStatusDescription;
        this.approximateCreationDateTimePrecision = builder.approximateCreationDateTimePrecision;
    }

    /**
     * The ARN for a specific Kinesis data stream.
     */
    public String getStreamArn() {
        return streamArn;
    }

    /**
     * The current status of replication.
     */
    public DestinationStatus getDestinationStatus() {
        return destinationStatus;
    }

    /**
     * The human-readable string that corresponds to the replica status.
     */
    public String getDestinationStatusDescription() {
        return destinationStatusDescription;
    }

    /**
     * The precision of the Kinesis data stream timestamp. The values are either <code>MILLISECOND</code> or
     * <code>MICROSECOND</code>.
     */
    public ApproximateCreationDateTimePrecision getApproximateCreationDateTimePrecision() {
        return approximateCreationDateTimePrecision;
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
        KinesisDataStreamDestination that = (KinesisDataStreamDestination) other;
        return Objects.equals(this.streamArn, that.streamArn)
               && Objects.equals(this.destinationStatusDescription, that.destinationStatusDescription)
               && Objects.equals(this.destinationStatus, that.destinationStatus)
               && Objects.equals(this.approximateCreationDateTimePrecision, that.approximateCreationDateTimePrecision);
    }

    @Override
    public int hashCode() {
        int $hc = Objects.hashCode(streamArn);
        $hc = 31 * $hc + Objects.hashCode(destinationStatus);
        $hc = 31 * $hc + Objects.hashCode(destinationStatusDescription);
        $hc = 31 * $hc + Objects.hashCode(approximateCreationDateTimePrecision);
        return $hc;
    }

    @Override
    public Schema schema() {
        return $SCHEMA;
    }

    @Override
    public void serializeMembers(ShapeSerializer serializer) {
        if (streamArn != null) {
            serializer.writeString($SCHEMA_STREAM_ARN, streamArn);
        }
        if (destinationStatus != null) {
            serializer.writeString($SCHEMA_DESTINATION_STATUS, destinationStatus.getValue());
        }
        if (destinationStatusDescription != null) {
            serializer.writeString($SCHEMA_DESTINATION_STATUS_DESCRIPTION, destinationStatusDescription);
        }
        if (approximateCreationDateTimePrecision != null) {
            serializer.writeString($SCHEMA_APPROXIMATE_CREATION_DATE_TIME_PRECISION, approximateCreationDateTimePrecision.getValue());
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T getMemberValue(Schema member) {
        return switch (member.memberIndex()) {
            case 0 -> (T) SchemaUtils.validateSameMember($SCHEMA_STREAM_ARN, member, streamArn);
            case 1 -> (T) SchemaUtils.validateSameMember($SCHEMA_DESTINATION_STATUS, member, destinationStatus);
            case 2 -> (T) SchemaUtils.validateSameMember($SCHEMA_DESTINATION_STATUS_DESCRIPTION, member, destinationStatusDescription);
            case 3 -> (T) SchemaUtils.validateSameMember($SCHEMA_APPROXIMATE_CREATION_DATE_TIME_PRECISION, member, approximateCreationDateTimePrecision);
            default -> throw new IllegalArgumentException("Attempted to get non-existent member: " + member.id());
        };
    }

    /**
     * Create a new builder containing all the current property values of this object.
     *
     * <p><strong>Note:</strong> This method performs only a shallow copy of the original properties.
     *
     * @return a builder for {@link KinesisDataStreamDestination}.
     */
    public Builder toBuilder() {
        var builder = new Builder();
        builder.streamArn(this.streamArn);
        builder.destinationStatus(this.destinationStatus);
        builder.destinationStatusDescription(this.destinationStatusDescription);
        builder.approximateCreationDateTimePrecision(this.approximateCreationDateTimePrecision);
        return builder;
    }

    /**
     * @return returns a new Builder.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for {@link KinesisDataStreamDestination}.
     */
    public static final class Builder implements ShapeBuilder<KinesisDataStreamDestination> {
        private String streamArn;
        private DestinationStatus destinationStatus;
        private String destinationStatusDescription;
        private ApproximateCreationDateTimePrecision approximateCreationDateTimePrecision;

        private Builder() {}

        @Override
        public Schema schema() {
            return $SCHEMA;
        }

        /**
         * The ARN for a specific Kinesis data stream.
         *
         * @return this builder.
         */
        public Builder streamArn(String streamArn) {
            this.streamArn = streamArn;
            return this;
        }

        /**
         * The current status of replication.
         *
         * @return this builder.
         */
        public Builder destinationStatus(DestinationStatus destinationStatus) {
            this.destinationStatus = destinationStatus;
            return this;
        }

        /**
         * The human-readable string that corresponds to the replica status.
         *
         * @return this builder.
         */
        public Builder destinationStatusDescription(String destinationStatusDescription) {
            this.destinationStatusDescription = destinationStatusDescription;
            return this;
        }

        /**
         * The precision of the Kinesis data stream timestamp. The values are either <code>MILLISECOND</code> or
         * <code>MICROSECOND</code>.
         *
         * @return this builder.
         */
        public Builder approximateCreationDateTimePrecision(ApproximateCreationDateTimePrecision approximateCreationDateTimePrecision) {
            this.approximateCreationDateTimePrecision = approximateCreationDateTimePrecision;
            return this;
        }

        @Override
        public KinesisDataStreamDestination build() {
            return new KinesisDataStreamDestination(this);
        }

        @Override
        @SuppressWarnings("unchecked")
        public void setMemberValue(Schema member, Object value) {
            switch (member.memberIndex()) {
                case 0 -> streamArn((String) SchemaUtils.validateSameMember($SCHEMA_STREAM_ARN, member, value));
                case 1 -> destinationStatus((DestinationStatus) SchemaUtils.validateSameMember($SCHEMA_DESTINATION_STATUS, member, value));
                case 2 -> destinationStatusDescription((String) SchemaUtils.validateSameMember($SCHEMA_DESTINATION_STATUS_DESCRIPTION, member, value));
                case 3 -> approximateCreationDateTimePrecision((ApproximateCreationDateTimePrecision) SchemaUtils.validateSameMember($SCHEMA_APPROXIMATE_CREATION_DATE_TIME_PRECISION, member, value));
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
                    case 0 -> builder.streamArn(de.readString(member));
                    case 1 -> builder.destinationStatus(DestinationStatus.builder().deserializeMember(de, member).build());
                    case 2 -> builder.destinationStatusDescription(de.readString(member));
                    case 3 -> builder.approximateCreationDateTimePrecision(ApproximateCreationDateTimePrecision.builder().deserializeMember(de, member).build());
                    default -> throw new IllegalArgumentException("Unexpected member: " + member.memberName());
                }
            }
        }
    }
}
