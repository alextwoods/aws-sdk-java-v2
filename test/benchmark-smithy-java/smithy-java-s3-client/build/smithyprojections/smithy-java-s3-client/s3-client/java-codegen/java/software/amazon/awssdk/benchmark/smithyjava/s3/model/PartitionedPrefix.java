package software.amazon.awssdk.benchmark.smithyjava.s3.model;

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
 * Amazon S3 keys for log objects are partitioned in the following format:
 *
 * <p><code>
 * [DestinationPrefix][SourceAccountId]/[SourceRegion]/[SourceBucket]/[YYYY]/[MM]/[DD]/[YYYY]-[MM]-[DD]-[hh]-[mm]-[ss]-[UniqueString]</code>
 *
 * <p>PartitionedPrefix defaults to EventTime delivery when server access logs are delivered.
 */
@SmithyGenerated
public final class PartitionedPrefix implements SerializableStruct {

    public static final Schema $SCHEMA = Schemas2.PARTITIONED_PREFIX;
    private static final Schema $SCHEMA_PARTITION_DATE_SOURCE = $SCHEMA.member("PartitionDateSource");

    public static final ShapeId $ID = $SCHEMA.id();

    private final transient PartitionDateSource partitionDateSource;

    private PartitionedPrefix(Builder builder) {
        this.partitionDateSource = builder.partitionDateSource;
    }

    /**
     * Specifies the partition date source for the partitioned prefix. <code>PartitionDateSource</code> can be <code>
     * EventTime</code> or <code>DeliveryTime</code>.
     *
     * <p>For <code>DeliveryTime</code>, the time in the log file names corresponds to the delivery time for the log
     * files.
     *
     * <p> For <code>EventTime</code>, The logs delivered are for a specific day only. The year, month, and day
     * correspond to the day on which the event occurred, and the hour, minutes and seconds are set to 00 in the key.
     */
    public PartitionDateSource getPartitionDateSource() {
        return partitionDateSource;
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
        PartitionedPrefix that = (PartitionedPrefix) other;
        return Objects.equals(this.partitionDateSource, that.partitionDateSource);
    }

    @Override
    public int hashCode() {
        int $hc = Objects.hashCode(partitionDateSource);
        return $hc;
    }

    @Override
    public Schema schema() {
        return $SCHEMA;
    }

    @Override
    public void serializeMembers(ShapeSerializer serializer) {
        if (partitionDateSource != null) {
            serializer.writeString($SCHEMA_PARTITION_DATE_SOURCE, partitionDateSource.getValue());
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T getMemberValue(Schema member) {
        return switch (member.memberIndex()) {
            case 0 -> (T) SchemaUtils.validateSameMember($SCHEMA_PARTITION_DATE_SOURCE, member, partitionDateSource);
            default -> throw new IllegalArgumentException("Attempted to get non-existent member: " + member.id());
        };
    }

    /**
     * Create a new builder containing all the current property values of this object.
     *
     * <p><strong>Note:</strong> This method performs only a shallow copy of the original properties.
     *
     * @return a builder for {@link PartitionedPrefix}.
     */
    public Builder toBuilder() {
        var builder = new Builder();
        builder.partitionDateSource(this.partitionDateSource);
        return builder;
    }

    /**
     * @return returns a new Builder.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for {@link PartitionedPrefix}.
     */
    public static final class Builder implements ShapeBuilder<PartitionedPrefix> {
        private PartitionDateSource partitionDateSource;

        private Builder() {}

        @Override
        public Schema schema() {
            return $SCHEMA;
        }

        /**
         * Specifies the partition date source for the partitioned prefix. <code>PartitionDateSource</code> can be <code>
         * EventTime</code> or <code>DeliveryTime</code>.
         *
         * <p>For <code>DeliveryTime</code>, the time in the log file names corresponds to the delivery time for the log
         * files.
         *
         * <p> For <code>EventTime</code>, The logs delivered are for a specific day only. The year, month, and day
         * correspond to the day on which the event occurred, and the hour, minutes and seconds are set to 00 in the key.
         *
         * @return this builder.
         */
        public Builder partitionDateSource(PartitionDateSource partitionDateSource) {
            this.partitionDateSource = partitionDateSource;
            return this;
        }

        @Override
        public PartitionedPrefix build() {
            return new PartitionedPrefix(this);
        }

        @Override
        @SuppressWarnings("unchecked")
        public void setMemberValue(Schema member, Object value) {
            switch (member.memberIndex()) {
                case 0 -> partitionDateSource((PartitionDateSource) SchemaUtils.validateSameMember($SCHEMA_PARTITION_DATE_SOURCE, member, value));
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
                    case 0 -> builder.partitionDateSource(PartitionDateSource.builder().deserializeMember(de, member).build());
                    default -> throw new IllegalArgumentException("Unexpected member: " + member.memberName());
                }
            }
        }
    }
}
