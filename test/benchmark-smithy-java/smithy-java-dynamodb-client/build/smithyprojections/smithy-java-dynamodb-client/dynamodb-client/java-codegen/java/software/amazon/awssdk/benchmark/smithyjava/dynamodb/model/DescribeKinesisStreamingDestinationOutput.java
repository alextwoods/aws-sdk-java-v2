package software.amazon.awssdk.benchmark.smithyjava.dynamodb.model;

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
public final class DescribeKinesisStreamingDestinationOutput implements SerializableStruct {

    public static final Schema $SCHEMA = Schemas.DESCRIBE_KINESIS_STREAMING_DESTINATION_OUTPUT;
    private static final Schema $SCHEMA_TABLE_NAME = $SCHEMA.member("TableName");
    private static final Schema $SCHEMA_KINESIS_DATA_STREAM_DESTINATIONS = $SCHEMA.member("KinesisDataStreamDestinations");

    public static final ShapeId $ID = $SCHEMA.id();

    private final transient String tableName;
    private final transient List<KinesisDataStreamDestination> kinesisDataStreamDestinations;

    private DescribeKinesisStreamingDestinationOutput(Builder builder) {
        this.tableName = builder.tableName;
        this.kinesisDataStreamDestinations = builder.kinesisDataStreamDestinations == null ? null : Collections.unmodifiableList(builder.kinesisDataStreamDestinations);
    }

    /**
     * The name of the table being described.
     */
    public String getTableName() {
        return tableName;
    }

    /**
     * The list of replica structures for the table being described.
     */
    public List<KinesisDataStreamDestination> getKinesisDataStreamDestinations() {
        if (kinesisDataStreamDestinations == null) {
            return Collections.emptyList();
        }
        return kinesisDataStreamDestinations;
    }

    public boolean hasKinesisDataStreamDestinations() {
        return kinesisDataStreamDestinations != null;
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
        DescribeKinesisStreamingDestinationOutput that = (DescribeKinesisStreamingDestinationOutput) other;
        return Objects.equals(this.tableName, that.tableName)
               && Objects.equals(this.kinesisDataStreamDestinations, that.kinesisDataStreamDestinations);
    }

    @Override
    public int hashCode() {
        int $hc = Objects.hashCode(tableName);
        $hc = 31 * $hc + Objects.hashCode(kinesisDataStreamDestinations);
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
        if (kinesisDataStreamDestinations != null) {
            serializer.writeList($SCHEMA_KINESIS_DATA_STREAM_DESTINATIONS, kinesisDataStreamDestinations, kinesisDataStreamDestinations.size(), SharedSerde.KinesisDataStreamDestinationsSerializer.INSTANCE);
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T getMemberValue(Schema member) {
        return switch (member.memberIndex()) {
            case 0 -> (T) SchemaUtils.validateSameMember($SCHEMA_TABLE_NAME, member, tableName);
            case 1 -> (T) SchemaUtils.validateSameMember($SCHEMA_KINESIS_DATA_STREAM_DESTINATIONS, member, kinesisDataStreamDestinations);
            default -> throw new IllegalArgumentException("Attempted to get non-existent member: " + member.id());
        };
    }

    /**
     * Create a new builder containing all the current property values of this object.
     *
     * <p><strong>Note:</strong> This method performs only a shallow copy of the original properties.
     *
     * @return a builder for {@link DescribeKinesisStreamingDestinationOutput}.
     */
    public Builder toBuilder() {
        var builder = new Builder();
        builder.tableName(this.tableName);
        builder.kinesisDataStreamDestinations(this.kinesisDataStreamDestinations);
        return builder;
    }

    /**
     * @return returns a new Builder.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for {@link DescribeKinesisStreamingDestinationOutput}.
     */
    public static final class Builder implements ShapeBuilder<DescribeKinesisStreamingDestinationOutput> {
        private String tableName;
        private List<KinesisDataStreamDestination> kinesisDataStreamDestinations;

        private Builder() {}

        @Override
        public Schema schema() {
            return $SCHEMA;
        }

        /**
         * The name of the table being described.
         *
         * @return this builder.
         */
        public Builder tableName(String tableName) {
            this.tableName = tableName;
            return this;
        }

        /**
         * The list of replica structures for the table being described.
         *
         * @return this builder.
         */
        public Builder kinesisDataStreamDestinations(List<KinesisDataStreamDestination> kinesisDataStreamDestinations) {
            this.kinesisDataStreamDestinations = kinesisDataStreamDestinations;
            return this;
        }

        @Override
        public DescribeKinesisStreamingDestinationOutput build() {
            return new DescribeKinesisStreamingDestinationOutput(this);
        }

        @Override
        @SuppressWarnings("unchecked")
        public void setMemberValue(Schema member, Object value) {
            switch (member.memberIndex()) {
                case 0 -> tableName((String) SchemaUtils.validateSameMember($SCHEMA_TABLE_NAME, member, value));
                case 1 -> kinesisDataStreamDestinations((List<KinesisDataStreamDestination>) SchemaUtils.validateSameMember($SCHEMA_KINESIS_DATA_STREAM_DESTINATIONS, member, value));
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
                    case 1 -> builder.kinesisDataStreamDestinations(SharedSerde.deserializeKinesisDataStreamDestinations(member, de));
                    default -> throw new IllegalArgumentException("Unexpected member: " + member.memberName());
                }
            }
        }
    }
}
