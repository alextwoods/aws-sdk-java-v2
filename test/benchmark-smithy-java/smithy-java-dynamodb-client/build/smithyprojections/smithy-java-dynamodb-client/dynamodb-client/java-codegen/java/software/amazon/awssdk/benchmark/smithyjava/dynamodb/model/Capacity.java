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
 * Represents the amount of provisioned throughput capacity consumed on a table or an index.
 */
@SmithyGenerated
public final class Capacity implements SerializableStruct {

    public static final Schema $SCHEMA = Schemas.CAPACITY;
    private static final Schema $SCHEMA_READ_CAPACITY_UNITS = $SCHEMA.member("ReadCapacityUnits");
    private static final Schema $SCHEMA_WRITE_CAPACITY_UNITS = $SCHEMA.member("WriteCapacityUnits");
    private static final Schema $SCHEMA_CAPACITY_UNITS = $SCHEMA.member("CapacityUnits");

    public static final ShapeId $ID = $SCHEMA.id();

    private final transient Double readCapacityUnits;
    private final transient Double writeCapacityUnits;
    private final transient Double capacityUnits;

    private Capacity(Builder builder) {
        this.readCapacityUnits = builder.readCapacityUnits;
        this.writeCapacityUnits = builder.writeCapacityUnits;
        this.capacityUnits = builder.capacityUnits;
    }

    /**
     * The total number of read capacity units consumed on a table or an index.
     */
    public Double getReadCapacityUnits() {
        return readCapacityUnits;
    }

    /**
     * The total number of write capacity units consumed on a table or an index.
     */
    public Double getWriteCapacityUnits() {
        return writeCapacityUnits;
    }

    /**
     * The total number of capacity units consumed on a table or an index.
     */
    public Double getCapacityUnits() {
        return capacityUnits;
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
        Capacity that = (Capacity) other;
        return Objects.equals(this.readCapacityUnits, that.readCapacityUnits)
               && Objects.equals(this.writeCapacityUnits, that.writeCapacityUnits)
               && Objects.equals(this.capacityUnits, that.capacityUnits);
    }

    @Override
    public int hashCode() {
        int $hc = Objects.hashCode(readCapacityUnits);
        $hc = 31 * $hc + Objects.hashCode(writeCapacityUnits);
        $hc = 31 * $hc + Objects.hashCode(capacityUnits);
        return $hc;
    }

    @Override
    public Schema schema() {
        return $SCHEMA;
    }

    @Override
    public void serializeMembers(ShapeSerializer serializer) {
        if (readCapacityUnits != null) {
            serializer.writeDouble($SCHEMA_READ_CAPACITY_UNITS, readCapacityUnits);
        }
        if (writeCapacityUnits != null) {
            serializer.writeDouble($SCHEMA_WRITE_CAPACITY_UNITS, writeCapacityUnits);
        }
        if (capacityUnits != null) {
            serializer.writeDouble($SCHEMA_CAPACITY_UNITS, capacityUnits);
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T getMemberValue(Schema member) {
        return switch (member.memberIndex()) {
            case 0 -> (T) SchemaUtils.validateSameMember($SCHEMA_READ_CAPACITY_UNITS, member, readCapacityUnits);
            case 1 -> (T) SchemaUtils.validateSameMember($SCHEMA_WRITE_CAPACITY_UNITS, member, writeCapacityUnits);
            case 2 -> (T) SchemaUtils.validateSameMember($SCHEMA_CAPACITY_UNITS, member, capacityUnits);
            default -> throw new IllegalArgumentException("Attempted to get non-existent member: " + member.id());
        };
    }

    /**
     * Create a new builder containing all the current property values of this object.
     *
     * <p><strong>Note:</strong> This method performs only a shallow copy of the original properties.
     *
     * @return a builder for {@link Capacity}.
     */
    public Builder toBuilder() {
        var builder = new Builder();
        builder.readCapacityUnits(this.readCapacityUnits);
        builder.writeCapacityUnits(this.writeCapacityUnits);
        builder.capacityUnits(this.capacityUnits);
        return builder;
    }

    /**
     * @return returns a new Builder.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for {@link Capacity}.
     */
    public static final class Builder implements ShapeBuilder<Capacity> {
        private Double readCapacityUnits;
        private Double writeCapacityUnits;
        private Double capacityUnits;

        private Builder() {}

        @Override
        public Schema schema() {
            return $SCHEMA;
        }

        /**
         * The total number of read capacity units consumed on a table or an index.
         *
         * @return this builder.
         */
        public Builder readCapacityUnits(Double readCapacityUnits) {
            this.readCapacityUnits = readCapacityUnits;
            return this;
        }

        /**
         * The total number of write capacity units consumed on a table or an index.
         *
         * @return this builder.
         */
        public Builder writeCapacityUnits(Double writeCapacityUnits) {
            this.writeCapacityUnits = writeCapacityUnits;
            return this;
        }

        /**
         * The total number of capacity units consumed on a table or an index.
         *
         * @return this builder.
         */
        public Builder capacityUnits(Double capacityUnits) {
            this.capacityUnits = capacityUnits;
            return this;
        }

        @Override
        public Capacity build() {
            return new Capacity(this);
        }

        @Override
        @SuppressWarnings("unchecked")
        public void setMemberValue(Schema member, Object value) {
            switch (member.memberIndex()) {
                case 0 -> readCapacityUnits((Double) SchemaUtils.validateSameMember($SCHEMA_READ_CAPACITY_UNITS, member, value));
                case 1 -> writeCapacityUnits((Double) SchemaUtils.validateSameMember($SCHEMA_WRITE_CAPACITY_UNITS, member, value));
                case 2 -> capacityUnits((Double) SchemaUtils.validateSameMember($SCHEMA_CAPACITY_UNITS, member, value));
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
                    case 0 -> builder.readCapacityUnits(de.readDouble(member));
                    case 1 -> builder.writeCapacityUnits(de.readDouble(member));
                    case 2 -> builder.capacityUnits(de.readDouble(member));
                    default -> throw new IllegalArgumentException("Unexpected member: " + member.memberName());
                }
            }
        }
    }
}
