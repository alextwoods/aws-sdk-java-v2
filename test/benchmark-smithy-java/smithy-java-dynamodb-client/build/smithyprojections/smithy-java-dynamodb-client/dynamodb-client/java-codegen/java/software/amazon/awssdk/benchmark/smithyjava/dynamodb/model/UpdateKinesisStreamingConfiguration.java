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
 * Enables updating the configuration for Kinesis Streaming.
 */
@SmithyGenerated
public final class UpdateKinesisStreamingConfiguration implements SerializableStruct {

    public static final Schema $SCHEMA = Schemas.UPDATE_KINESIS_STREAMING_CONFIGURATION;
    private static final Schema $SCHEMA_APPROXIMATE_CREATION_DATE_TIME_PRECISION = $SCHEMA.member("ApproximateCreationDateTimePrecision");

    public static final ShapeId $ID = $SCHEMA.id();

    private final transient ApproximateCreationDateTimePrecision approximateCreationDateTimePrecision;

    private UpdateKinesisStreamingConfiguration(Builder builder) {
        this.approximateCreationDateTimePrecision = builder.approximateCreationDateTimePrecision;
    }

    /**
     * Enables updating the precision of Kinesis data stream timestamp.
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
        UpdateKinesisStreamingConfiguration that = (UpdateKinesisStreamingConfiguration) other;
        return Objects.equals(this.approximateCreationDateTimePrecision, that.approximateCreationDateTimePrecision);
    }

    @Override
    public int hashCode() {
        int $hc = Objects.hashCode(approximateCreationDateTimePrecision);
        return $hc;
    }

    @Override
    public Schema schema() {
        return $SCHEMA;
    }

    @Override
    public void serializeMembers(ShapeSerializer serializer) {
        if (approximateCreationDateTimePrecision != null) {
            serializer.writeString($SCHEMA_APPROXIMATE_CREATION_DATE_TIME_PRECISION, approximateCreationDateTimePrecision.getValue());
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T getMemberValue(Schema member) {
        return switch (member.memberIndex()) {
            case 0 -> (T) SchemaUtils.validateSameMember($SCHEMA_APPROXIMATE_CREATION_DATE_TIME_PRECISION, member, approximateCreationDateTimePrecision);
            default -> throw new IllegalArgumentException("Attempted to get non-existent member: " + member.id());
        };
    }

    /**
     * Create a new builder containing all the current property values of this object.
     *
     * <p><strong>Note:</strong> This method performs only a shallow copy of the original properties.
     *
     * @return a builder for {@link UpdateKinesisStreamingConfiguration}.
     */
    public Builder toBuilder() {
        var builder = new Builder();
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
     * Builder for {@link UpdateKinesisStreamingConfiguration}.
     */
    public static final class Builder implements ShapeBuilder<UpdateKinesisStreamingConfiguration> {
        private ApproximateCreationDateTimePrecision approximateCreationDateTimePrecision;

        private Builder() {}

        @Override
        public Schema schema() {
            return $SCHEMA;
        }

        /**
         * Enables updating the precision of Kinesis data stream timestamp.
         *
         * @return this builder.
         */
        public Builder approximateCreationDateTimePrecision(ApproximateCreationDateTimePrecision approximateCreationDateTimePrecision) {
            this.approximateCreationDateTimePrecision = approximateCreationDateTimePrecision;
            return this;
        }

        @Override
        public UpdateKinesisStreamingConfiguration build() {
            return new UpdateKinesisStreamingConfiguration(this);
        }

        @Override
        @SuppressWarnings("unchecked")
        public void setMemberValue(Schema member, Object value) {
            switch (member.memberIndex()) {
                case 0 -> approximateCreationDateTimePrecision((ApproximateCreationDateTimePrecision) SchemaUtils.validateSameMember($SCHEMA_APPROXIMATE_CREATION_DATE_TIME_PRECISION, member, value));
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
                    case 0 -> builder.approximateCreationDateTimePrecision(ApproximateCreationDateTimePrecision.builder().deserializeMember(de, member).build());
                    default -> throw new IllegalArgumentException("Unexpected member: " + member.memberName());
                }
            }
        }
    }
}
