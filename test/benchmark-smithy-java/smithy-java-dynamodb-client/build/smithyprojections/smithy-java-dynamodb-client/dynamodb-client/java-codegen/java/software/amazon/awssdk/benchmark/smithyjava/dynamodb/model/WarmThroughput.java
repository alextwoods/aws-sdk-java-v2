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
 * Provides visibility into the number of read and write operations your table or secondary index can instantaneously
 * support. The settings can be modified using the <code>UpdateTable</code> operation to meet the throughput
 * requirements of an upcoming peak event.
 */
@SmithyGenerated
public final class WarmThroughput implements SerializableStruct {

    public static final Schema $SCHEMA = Schemas.WARM_THROUGHPUT;
    private static final Schema $SCHEMA_READ_UNITS_PER_SECOND = $SCHEMA.member("ReadUnitsPerSecond");
    private static final Schema $SCHEMA_WRITE_UNITS_PER_SECOND = $SCHEMA.member("WriteUnitsPerSecond");

    public static final ShapeId $ID = $SCHEMA.id();

    private final transient Long readUnitsPerSecond;
    private final transient Long writeUnitsPerSecond;

    private WarmThroughput(Builder builder) {
        this.readUnitsPerSecond = builder.readUnitsPerSecond;
        this.writeUnitsPerSecond = builder.writeUnitsPerSecond;
    }

    /**
     * Represents the number of read operations your base table can instantaneously support.
     */
    public Long getReadUnitsPerSecond() {
        return readUnitsPerSecond;
    }

    /**
     * Represents the number of write operations your base table can instantaneously support.
     */
    public Long getWriteUnitsPerSecond() {
        return writeUnitsPerSecond;
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
        WarmThroughput that = (WarmThroughput) other;
        return Objects.equals(this.readUnitsPerSecond, that.readUnitsPerSecond)
               && Objects.equals(this.writeUnitsPerSecond, that.writeUnitsPerSecond);
    }

    @Override
    public int hashCode() {
        int $hc = Objects.hashCode(readUnitsPerSecond);
        $hc = 31 * $hc + Objects.hashCode(writeUnitsPerSecond);
        return $hc;
    }

    @Override
    public Schema schema() {
        return $SCHEMA;
    }

    @Override
    public void serializeMembers(ShapeSerializer serializer) {
        if (readUnitsPerSecond != null) {
            serializer.writeLong($SCHEMA_READ_UNITS_PER_SECOND, readUnitsPerSecond);
        }
        if (writeUnitsPerSecond != null) {
            serializer.writeLong($SCHEMA_WRITE_UNITS_PER_SECOND, writeUnitsPerSecond);
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T getMemberValue(Schema member) {
        return switch (member.memberIndex()) {
            case 0 -> (T) SchemaUtils.validateSameMember($SCHEMA_READ_UNITS_PER_SECOND, member, readUnitsPerSecond);
            case 1 -> (T) SchemaUtils.validateSameMember($SCHEMA_WRITE_UNITS_PER_SECOND, member, writeUnitsPerSecond);
            default -> throw new IllegalArgumentException("Attempted to get non-existent member: " + member.id());
        };
    }

    /**
     * Create a new builder containing all the current property values of this object.
     *
     * <p><strong>Note:</strong> This method performs only a shallow copy of the original properties.
     *
     * @return a builder for {@link WarmThroughput}.
     */
    public Builder toBuilder() {
        var builder = new Builder();
        builder.readUnitsPerSecond(this.readUnitsPerSecond);
        builder.writeUnitsPerSecond(this.writeUnitsPerSecond);
        return builder;
    }

    /**
     * @return returns a new Builder.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for {@link WarmThroughput}.
     */
    public static final class Builder implements ShapeBuilder<WarmThroughput> {
        private Long readUnitsPerSecond;
        private Long writeUnitsPerSecond;

        private Builder() {}

        @Override
        public Schema schema() {
            return $SCHEMA;
        }

        /**
         * Represents the number of read operations your base table can instantaneously support.
         *
         * @return this builder.
         */
        public Builder readUnitsPerSecond(Long readUnitsPerSecond) {
            this.readUnitsPerSecond = readUnitsPerSecond;
            return this;
        }

        /**
         * Represents the number of write operations your base table can instantaneously support.
         *
         * @return this builder.
         */
        public Builder writeUnitsPerSecond(Long writeUnitsPerSecond) {
            this.writeUnitsPerSecond = writeUnitsPerSecond;
            return this;
        }

        @Override
        public WarmThroughput build() {
            return new WarmThroughput(this);
        }

        @Override
        @SuppressWarnings("unchecked")
        public void setMemberValue(Schema member, Object value) {
            switch (member.memberIndex()) {
                case 0 -> readUnitsPerSecond((Long) SchemaUtils.validateSameMember($SCHEMA_READ_UNITS_PER_SECOND, member, value));
                case 1 -> writeUnitsPerSecond((Long) SchemaUtils.validateSameMember($SCHEMA_WRITE_UNITS_PER_SECOND, member, value));
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
                    case 0 -> builder.readUnitsPerSecond(de.readLong(member));
                    case 1 -> builder.writeUnitsPerSecond(de.readLong(member));
                    default -> throw new IllegalArgumentException("Unexpected member: " + member.memberName());
                }
            }
        }
    }
}
