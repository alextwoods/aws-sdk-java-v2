package software.amazon.awssdk.benchmark.smithyjava.s3.model;

import java.util.Objects;
import software.amazon.smithy.java.core.schema.PresenceTracker;
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
 * Specifies the schedule for generating S3 Inventory results.
 */
@SmithyGenerated
public final class InventorySchedule implements SerializableStruct {

    public static final Schema $SCHEMA = Schemas2.INVENTORY_SCHEDULE;
    private static final Schema $SCHEMA_FREQUENCY = $SCHEMA.member("Frequency");

    public static final ShapeId $ID = $SCHEMA.id();

    private final transient InventoryFrequency frequency;

    private InventorySchedule(Builder builder) {
        this.frequency = builder.frequency;
    }

    /**
     * Specifies how frequently inventory results are produced.
     */
    public InventoryFrequency getFrequency() {
        return frequency;
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
        InventorySchedule that = (InventorySchedule) other;
        return Objects.equals(this.frequency, that.frequency);
    }

    @Override
    public int hashCode() {
        int $hc = Objects.hashCode(frequency);
        return $hc;
    }

    @Override
    public Schema schema() {
        return $SCHEMA;
    }

    @Override
    public void serializeMembers(ShapeSerializer serializer) {
        serializer.writeString($SCHEMA_FREQUENCY, frequency.getValue());
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T getMemberValue(Schema member) {
        return switch (member.memberIndex()) {
            case 0 -> (T) SchemaUtils.validateSameMember($SCHEMA_FREQUENCY, member, frequency);
            default -> throw new IllegalArgumentException("Attempted to get non-existent member: " + member.id());
        };
    }

    /**
     * Create a new builder containing all the current property values of this object.
     *
     * <p><strong>Note:</strong> This method performs only a shallow copy of the original properties.
     *
     * @return a builder for {@link InventorySchedule}.
     */
    public Builder toBuilder() {
        var builder = new Builder();
        builder.frequency(this.frequency);
        return builder;
    }

    /**
     * @return returns a new Builder.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for {@link InventorySchedule}.
     */
    public static final class Builder implements ShapeBuilder<InventorySchedule> {
        private final PresenceTracker tracker = PresenceTracker.of($SCHEMA);
        private InventoryFrequency frequency;

        private Builder() {}

        @Override
        public Schema schema() {
            return $SCHEMA;
        }

        /**
         * Specifies how frequently inventory results are produced.
         *
         * <p><strong>Required</strong>
         * @return this builder.
         */
        public Builder frequency(InventoryFrequency frequency) {
            this.frequency = Objects.requireNonNull(frequency, "frequency cannot be null");
            tracker.setMember($SCHEMA_FREQUENCY);
            return this;
        }

        @Override
        public InventorySchedule build() {
            tracker.validate();
            return new InventorySchedule(this);
        }

        @Override
        @SuppressWarnings("unchecked")
        public void setMemberValue(Schema member, Object value) {
            switch (member.memberIndex()) {
                case 0 -> frequency((InventoryFrequency) SchemaUtils.validateSameMember($SCHEMA_FREQUENCY, member, value));
                default -> ShapeBuilder.super.setMemberValue(member, value);
            }
        }

        @Override
        public ShapeBuilder<InventorySchedule> errorCorrection() {
            if (tracker.allSet()) {
                return this;
            }
            if (!tracker.checkMember($SCHEMA_FREQUENCY)) {
                frequency(InventoryFrequency.unknown(""));
            }
            return this;
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
                    case 0 -> builder.frequency(InventoryFrequency.builder().deserializeMember(de, member).build());
                    default -> throw new IllegalArgumentException("Unexpected member: " + member.memberName());
                }
            }
        }
    }
}
