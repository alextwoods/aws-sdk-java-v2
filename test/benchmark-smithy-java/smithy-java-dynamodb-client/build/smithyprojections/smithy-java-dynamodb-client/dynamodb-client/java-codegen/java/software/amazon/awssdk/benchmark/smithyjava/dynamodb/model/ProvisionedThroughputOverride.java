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
 * Replica-specific provisioned throughput settings. If not specified, uses the source table's provisioned throughput
 * settings.
 */
@SmithyGenerated
public final class ProvisionedThroughputOverride implements SerializableStruct {

    public static final Schema $SCHEMA = Schemas.PROVISIONED_THROUGHPUT_OVERRIDE;
    private static final Schema $SCHEMA_READ_CAPACITY_UNITS = $SCHEMA.member("ReadCapacityUnits");

    public static final ShapeId $ID = $SCHEMA.id();

    private final transient Long readCapacityUnits;

    private ProvisionedThroughputOverride(Builder builder) {
        this.readCapacityUnits = builder.readCapacityUnits;
    }

    /**
     * Replica-specific read capacity units. If not specified, uses the source table's read capacity settings.
     */
    public Long getReadCapacityUnits() {
        return readCapacityUnits;
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
        ProvisionedThroughputOverride that = (ProvisionedThroughputOverride) other;
        return Objects.equals(this.readCapacityUnits, that.readCapacityUnits);
    }

    @Override
    public int hashCode() {
        int $hc = Objects.hashCode(readCapacityUnits);
        return $hc;
    }

    @Override
    public Schema schema() {
        return $SCHEMA;
    }

    @Override
    public void serializeMembers(ShapeSerializer serializer) {
        if (readCapacityUnits != null) {
            serializer.writeLong($SCHEMA_READ_CAPACITY_UNITS, readCapacityUnits);
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T getMemberValue(Schema member) {
        return switch (member.memberIndex()) {
            case 0 -> (T) SchemaUtils.validateSameMember($SCHEMA_READ_CAPACITY_UNITS, member, readCapacityUnits);
            default -> throw new IllegalArgumentException("Attempted to get non-existent member: " + member.id());
        };
    }

    /**
     * Create a new builder containing all the current property values of this object.
     *
     * <p><strong>Note:</strong> This method performs only a shallow copy of the original properties.
     *
     * @return a builder for {@link ProvisionedThroughputOverride}.
     */
    public Builder toBuilder() {
        var builder = new Builder();
        builder.readCapacityUnits(this.readCapacityUnits);
        return builder;
    }

    /**
     * @return returns a new Builder.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for {@link ProvisionedThroughputOverride}.
     */
    public static final class Builder implements ShapeBuilder<ProvisionedThroughputOverride> {
        private Long readCapacityUnits;

        private Builder() {}

        @Override
        public Schema schema() {
            return $SCHEMA;
        }

        /**
         * Replica-specific read capacity units. If not specified, uses the source table's read capacity settings.
         *
         * @return this builder.
         */
        public Builder readCapacityUnits(Long readCapacityUnits) {
            this.readCapacityUnits = readCapacityUnits;
            return this;
        }

        @Override
        public ProvisionedThroughputOverride build() {
            return new ProvisionedThroughputOverride(this);
        }

        @Override
        @SuppressWarnings("unchecked")
        public void setMemberValue(Schema member, Object value) {
            switch (member.memberIndex()) {
                case 0 -> readCapacityUnits((Long) SchemaUtils.validateSameMember($SCHEMA_READ_CAPACITY_UNITS, member, value));
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
                    case 0 -> builder.readCapacityUnits(de.readLong(member));
                    default -> throw new IllegalArgumentException("Unexpected member: " + member.memberName());
                }
            }
        }
    }
}
