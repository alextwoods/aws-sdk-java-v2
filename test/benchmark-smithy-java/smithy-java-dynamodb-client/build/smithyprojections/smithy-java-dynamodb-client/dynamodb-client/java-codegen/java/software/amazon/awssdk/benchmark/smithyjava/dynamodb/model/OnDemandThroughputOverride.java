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
 * Overrides the on-demand throughput settings for this replica table. If you don't specify a value for this parameter,
 * it uses the source table's on-demand throughput settings.
 */
@SmithyGenerated
public final class OnDemandThroughputOverride implements SerializableStruct {

    public static final Schema $SCHEMA = Schemas.ON_DEMAND_THROUGHPUT_OVERRIDE;
    private static final Schema $SCHEMA_MAX_READ_REQUEST_UNITS = $SCHEMA.member("MaxReadRequestUnits");

    public static final ShapeId $ID = $SCHEMA.id();

    private final transient Long maxReadRequestUnits;

    private OnDemandThroughputOverride(Builder builder) {
        this.maxReadRequestUnits = builder.maxReadRequestUnits;
    }

    /**
     * Maximum number of read request units for the specified replica table.
     */
    public Long getMaxReadRequestUnits() {
        return maxReadRequestUnits;
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
        OnDemandThroughputOverride that = (OnDemandThroughputOverride) other;
        return Objects.equals(this.maxReadRequestUnits, that.maxReadRequestUnits);
    }

    @Override
    public int hashCode() {
        int $hc = Objects.hashCode(maxReadRequestUnits);
        return $hc;
    }

    @Override
    public Schema schema() {
        return $SCHEMA;
    }

    @Override
    public void serializeMembers(ShapeSerializer serializer) {
        if (maxReadRequestUnits != null) {
            serializer.writeLong($SCHEMA_MAX_READ_REQUEST_UNITS, maxReadRequestUnits);
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T getMemberValue(Schema member) {
        return switch (member.memberIndex()) {
            case 0 -> (T) SchemaUtils.validateSameMember($SCHEMA_MAX_READ_REQUEST_UNITS, member, maxReadRequestUnits);
            default -> throw new IllegalArgumentException("Attempted to get non-existent member: " + member.id());
        };
    }

    /**
     * Create a new builder containing all the current property values of this object.
     *
     * <p><strong>Note:</strong> This method performs only a shallow copy of the original properties.
     *
     * @return a builder for {@link OnDemandThroughputOverride}.
     */
    public Builder toBuilder() {
        var builder = new Builder();
        builder.maxReadRequestUnits(this.maxReadRequestUnits);
        return builder;
    }

    /**
     * @return returns a new Builder.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for {@link OnDemandThroughputOverride}.
     */
    public static final class Builder implements ShapeBuilder<OnDemandThroughputOverride> {
        private Long maxReadRequestUnits;

        private Builder() {}

        @Override
        public Schema schema() {
            return $SCHEMA;
        }

        /**
         * Maximum number of read request units for the specified replica table.
         *
         * @return this builder.
         */
        public Builder maxReadRequestUnits(Long maxReadRequestUnits) {
            this.maxReadRequestUnits = maxReadRequestUnits;
            return this;
        }

        @Override
        public OnDemandThroughputOverride build() {
            return new OnDemandThroughputOverride(this);
        }

        @Override
        @SuppressWarnings("unchecked")
        public void setMemberValue(Schema member, Object value) {
            switch (member.memberIndex()) {
                case 0 -> maxReadRequestUnits((Long) SchemaUtils.validateSameMember($SCHEMA_MAX_READ_REQUEST_UNITS, member, value));
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
                    case 0 -> builder.maxReadRequestUnits(de.readLong(member));
                    default -> throw new IllegalArgumentException("Unexpected member: " + member.memberName());
                }
            }
        }
    }
}
