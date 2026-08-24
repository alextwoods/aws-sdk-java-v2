package software.amazon.awssdk.benchmark.smithyjava.cloudwatch.model;

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
 * This object includes parameters that you can use to provide information to CloudWatch to help it build more accurate
 * anomaly detection models.
 */
@SmithyGenerated
public final class MetricCharacteristics implements SerializableStruct {

    public static final Schema $SCHEMA = Schemas.METRIC_CHARACTERISTICS;
    private static final Schema $SCHEMA_PERIODIC_SPIKES = $SCHEMA.member("PeriodicSpikes");

    public static final ShapeId $ID = $SCHEMA.id();

    private final transient Boolean periodicSpikes;

    private MetricCharacteristics(Builder builder) {
        this.periodicSpikes = builder.periodicSpikes;
    }

    /**
     * Set this parameter to <code>true</code> if values for this metric consistently include spikes that should not be
     * considered to be anomalies. With this set to <code>true</code>, CloudWatch will expect to see spikes that
     * occurred consistently during the model training period, and won't flag future similar spikes as anomalies.
     */
    public Boolean isPeriodicSpikes() {
        return periodicSpikes;
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
        MetricCharacteristics that = (MetricCharacteristics) other;
        return Objects.equals(this.periodicSpikes, that.periodicSpikes);
    }

    @Override
    public int hashCode() {
        int $hc = Objects.hashCode(periodicSpikes);
        return $hc;
    }

    @Override
    public Schema schema() {
        return $SCHEMA;
    }

    @Override
    public void serializeMembers(ShapeSerializer serializer) {
        if (periodicSpikes != null) {
            serializer.writeBoolean($SCHEMA_PERIODIC_SPIKES, periodicSpikes);
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T getMemberValue(Schema member) {
        return switch (member.memberIndex()) {
            case 0 -> (T) SchemaUtils.validateSameMember($SCHEMA_PERIODIC_SPIKES, member, periodicSpikes);
            default -> throw new IllegalArgumentException("Attempted to get non-existent member: " + member.id());
        };
    }

    /**
     * Create a new builder containing all the current property values of this object.
     *
     * <p><strong>Note:</strong> This method performs only a shallow copy of the original properties.
     *
     * @return a builder for {@link MetricCharacteristics}.
     */
    public Builder toBuilder() {
        var builder = new Builder();
        builder.periodicSpikes(this.periodicSpikes);
        return builder;
    }

    /**
     * @return returns a new Builder.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for {@link MetricCharacteristics}.
     */
    public static final class Builder implements ShapeBuilder<MetricCharacteristics> {
        private Boolean periodicSpikes;

        private Builder() {}

        @Override
        public Schema schema() {
            return $SCHEMA;
        }

        /**
         * Set this parameter to <code>true</code> if values for this metric consistently include spikes that should not be
         * considered to be anomalies. With this set to <code>true</code>, CloudWatch will expect to see spikes that
         * occurred consistently during the model training period, and won't flag future similar spikes as anomalies.
         *
         * @return this builder.
         */
        public Builder periodicSpikes(Boolean periodicSpikes) {
            this.periodicSpikes = periodicSpikes;
            return this;
        }

        @Override
        public MetricCharacteristics build() {
            return new MetricCharacteristics(this);
        }

        @Override
        @SuppressWarnings("unchecked")
        public void setMemberValue(Schema member, Object value) {
            switch (member.memberIndex()) {
                case 0 -> periodicSpikes((Boolean) SchemaUtils.validateSameMember($SCHEMA_PERIODIC_SPIKES, member, value));
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
                    case 0 -> builder.periodicSpikes(de.readBoolean(member));
                    default -> throw new IllegalArgumentException("Unexpected member: " + member.memberName());
                }
            }
        }
    }
}
