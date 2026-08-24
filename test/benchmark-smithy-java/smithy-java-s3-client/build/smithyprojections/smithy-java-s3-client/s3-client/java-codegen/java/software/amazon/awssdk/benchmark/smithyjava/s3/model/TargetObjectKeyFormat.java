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
 * Amazon S3 key format for log objects. Only one format, PartitionedPrefix or SimplePrefix, is allowed.
 */
@SmithyGenerated
public final class TargetObjectKeyFormat implements SerializableStruct {

    public static final Schema $SCHEMA = Schemas2.TARGET_OBJECT_KEY_FORMAT;
    private static final Schema $SCHEMA_SIMPLE_PREFIX = $SCHEMA.member("SimplePrefix");
    private static final Schema $SCHEMA_PARTITIONED_PREFIX = $SCHEMA.member("PartitionedPrefix");

    public static final ShapeId $ID = $SCHEMA.id();

    private final transient SimplePrefix simplePrefix;
    private final transient PartitionedPrefix partitionedPrefix;

    private TargetObjectKeyFormat(Builder builder) {
        this.simplePrefix = builder.simplePrefix;
        this.partitionedPrefix = builder.partitionedPrefix;
    }

    /**
     * To use the simple format for S3 keys for log objects. To specify SimplePrefix format, set SimplePrefix to {}.
     */
    public SimplePrefix getSimplePrefix() {
        return simplePrefix;
    }

    /**
     * Partitioned S3 key for log objects.
     */
    public PartitionedPrefix getPartitionedPrefix() {
        return partitionedPrefix;
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
        TargetObjectKeyFormat that = (TargetObjectKeyFormat) other;
        return Objects.equals(this.simplePrefix, that.simplePrefix)
               && Objects.equals(this.partitionedPrefix, that.partitionedPrefix);
    }

    @Override
    public int hashCode() {
        int $hc = Objects.hashCode(simplePrefix);
        $hc = 31 * $hc + Objects.hashCode(partitionedPrefix);
        return $hc;
    }

    @Override
    public Schema schema() {
        return $SCHEMA;
    }

    @Override
    public void serializeMembers(ShapeSerializer serializer) {
        if (simplePrefix != null) {
            serializer.writeStruct($SCHEMA_SIMPLE_PREFIX, simplePrefix);
        }
        if (partitionedPrefix != null) {
            serializer.writeStruct($SCHEMA_PARTITIONED_PREFIX, partitionedPrefix);
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T getMemberValue(Schema member) {
        return switch (member.memberIndex()) {
            case 0 -> (T) SchemaUtils.validateSameMember($SCHEMA_SIMPLE_PREFIX, member, simplePrefix);
            case 1 -> (T) SchemaUtils.validateSameMember($SCHEMA_PARTITIONED_PREFIX, member, partitionedPrefix);
            default -> throw new IllegalArgumentException("Attempted to get non-existent member: " + member.id());
        };
    }

    /**
     * Create a new builder containing all the current property values of this object.
     *
     * <p><strong>Note:</strong> This method performs only a shallow copy of the original properties.
     *
     * @return a builder for {@link TargetObjectKeyFormat}.
     */
    public Builder toBuilder() {
        var builder = new Builder();
        builder.simplePrefix(this.simplePrefix);
        builder.partitionedPrefix(this.partitionedPrefix);
        return builder;
    }

    /**
     * @return returns a new Builder.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for {@link TargetObjectKeyFormat}.
     */
    public static final class Builder implements ShapeBuilder<TargetObjectKeyFormat> {
        private SimplePrefix simplePrefix;
        private PartitionedPrefix partitionedPrefix;

        private Builder() {}

        @Override
        public Schema schema() {
            return $SCHEMA;
        }

        /**
         * To use the simple format for S3 keys for log objects. To specify SimplePrefix format, set SimplePrefix to {}.
         *
         * @return this builder.
         */
        public Builder simplePrefix(SimplePrefix simplePrefix) {
            this.simplePrefix = simplePrefix;
            return this;
        }

        /**
         * Partitioned S3 key for log objects.
         *
         * @return this builder.
         */
        public Builder partitionedPrefix(PartitionedPrefix partitionedPrefix) {
            this.partitionedPrefix = partitionedPrefix;
            return this;
        }

        @Override
        public TargetObjectKeyFormat build() {
            return new TargetObjectKeyFormat(this);
        }

        @Override
        @SuppressWarnings("unchecked")
        public void setMemberValue(Schema member, Object value) {
            switch (member.memberIndex()) {
                case 0 -> simplePrefix((SimplePrefix) SchemaUtils.validateSameMember($SCHEMA_SIMPLE_PREFIX, member, value));
                case 1 -> partitionedPrefix((PartitionedPrefix) SchemaUtils.validateSameMember($SCHEMA_PARTITIONED_PREFIX, member, value));
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
                    case 0 -> builder.simplePrefix(SimplePrefix.builder().deserializeMember(de, member).build());
                    case 1 -> builder.partitionedPrefix(PartitionedPrefix.builder().deserializeMember(de, member).build());
                    default -> throw new IllegalArgumentException("Unexpected member: " + member.memberName());
                }
            }
        }
    }
}
