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
 * Represents the warm throughput value (in read units per second and write units per second) of the table. Warm
 * throughput is applicable for DynamoDB Standard-IA tables and specifies the minimum provisioned capacity maintained
 * for immediate data access.
 */
@SmithyGenerated
public final class TableWarmThroughputDescription implements SerializableStruct {

    public static final Schema $SCHEMA = Schemas.TABLE_WARM_THROUGHPUT_DESCRIPTION;
    private static final Schema $SCHEMA_READ_UNITS_PER_SECOND = $SCHEMA.member("ReadUnitsPerSecond");
    private static final Schema $SCHEMA_WRITE_UNITS_PER_SECOND = $SCHEMA.member("WriteUnitsPerSecond");
    private static final Schema $SCHEMA_STATUS = $SCHEMA.member("Status");

    public static final ShapeId $ID = $SCHEMA.id();

    private final transient Long readUnitsPerSecond;
    private final transient Long writeUnitsPerSecond;
    private final transient TableStatus status;

    private TableWarmThroughputDescription(Builder builder) {
        this.readUnitsPerSecond = builder.readUnitsPerSecond;
        this.writeUnitsPerSecond = builder.writeUnitsPerSecond;
        this.status = builder.status;
    }

    /**
     * Represents the base table's warm throughput value in read units per second.
     */
    public Long getReadUnitsPerSecond() {
        return readUnitsPerSecond;
    }

    /**
     * Represents the base table's warm throughput value in write units per second.
     */
    public Long getWriteUnitsPerSecond() {
        return writeUnitsPerSecond;
    }

    /**
     * Represents warm throughput value of the base table.
     */
    public TableStatus getStatus() {
        return status;
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
        TableWarmThroughputDescription that = (TableWarmThroughputDescription) other;
        return Objects.equals(this.readUnitsPerSecond, that.readUnitsPerSecond)
               && Objects.equals(this.writeUnitsPerSecond, that.writeUnitsPerSecond)
               && Objects.equals(this.status, that.status);
    }

    @Override
    public int hashCode() {
        int $hc = Objects.hashCode(readUnitsPerSecond);
        $hc = 31 * $hc + Objects.hashCode(writeUnitsPerSecond);
        $hc = 31 * $hc + Objects.hashCode(status);
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
        if (status != null) {
            serializer.writeString($SCHEMA_STATUS, status.getValue());
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T getMemberValue(Schema member) {
        return switch (member.memberIndex()) {
            case 0 -> (T) SchemaUtils.validateSameMember($SCHEMA_READ_UNITS_PER_SECOND, member, readUnitsPerSecond);
            case 1 -> (T) SchemaUtils.validateSameMember($SCHEMA_WRITE_UNITS_PER_SECOND, member, writeUnitsPerSecond);
            case 2 -> (T) SchemaUtils.validateSameMember($SCHEMA_STATUS, member, status);
            default -> throw new IllegalArgumentException("Attempted to get non-existent member: " + member.id());
        };
    }

    /**
     * Create a new builder containing all the current property values of this object.
     *
     * <p><strong>Note:</strong> This method performs only a shallow copy of the original properties.
     *
     * @return a builder for {@link TableWarmThroughputDescription}.
     */
    public Builder toBuilder() {
        var builder = new Builder();
        builder.readUnitsPerSecond(this.readUnitsPerSecond);
        builder.writeUnitsPerSecond(this.writeUnitsPerSecond);
        builder.status(this.status);
        return builder;
    }

    /**
     * @return returns a new Builder.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for {@link TableWarmThroughputDescription}.
     */
    public static final class Builder implements ShapeBuilder<TableWarmThroughputDescription> {
        private Long readUnitsPerSecond;
        private Long writeUnitsPerSecond;
        private TableStatus status;

        private Builder() {}

        @Override
        public Schema schema() {
            return $SCHEMA;
        }

        /**
         * Represents the base table's warm throughput value in read units per second.
         *
         * @return this builder.
         */
        public Builder readUnitsPerSecond(Long readUnitsPerSecond) {
            this.readUnitsPerSecond = readUnitsPerSecond;
            return this;
        }

        /**
         * Represents the base table's warm throughput value in write units per second.
         *
         * @return this builder.
         */
        public Builder writeUnitsPerSecond(Long writeUnitsPerSecond) {
            this.writeUnitsPerSecond = writeUnitsPerSecond;
            return this;
        }

        /**
         * Represents warm throughput value of the base table.
         *
         * @return this builder.
         */
        public Builder status(TableStatus status) {
            this.status = status;
            return this;
        }

        @Override
        public TableWarmThroughputDescription build() {
            return new TableWarmThroughputDescription(this);
        }

        @Override
        @SuppressWarnings("unchecked")
        public void setMemberValue(Schema member, Object value) {
            switch (member.memberIndex()) {
                case 0 -> readUnitsPerSecond((Long) SchemaUtils.validateSameMember($SCHEMA_READ_UNITS_PER_SECOND, member, value));
                case 1 -> writeUnitsPerSecond((Long) SchemaUtils.validateSameMember($SCHEMA_WRITE_UNITS_PER_SECOND, member, value));
                case 2 -> status((TableStatus) SchemaUtils.validateSameMember($SCHEMA_STATUS, member, value));
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
                    case 2 -> builder.status(TableStatus.builder().deserializeMember(de, member).build());
                    default -> throw new IllegalArgumentException("Unexpected member: " + member.memberName());
                }
            }
        }
    }
}
