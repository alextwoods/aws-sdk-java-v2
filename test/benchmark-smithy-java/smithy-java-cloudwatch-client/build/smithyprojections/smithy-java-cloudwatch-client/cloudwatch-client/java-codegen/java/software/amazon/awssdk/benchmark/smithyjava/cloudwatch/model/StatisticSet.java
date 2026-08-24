package software.amazon.awssdk.benchmark.smithyjava.cloudwatch.model;

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
 * Represents a set of statistics that describes a specific metric.
 */
@SmithyGenerated
public final class StatisticSet implements SerializableStruct {

    public static final Schema $SCHEMA = Schemas.STATISTIC_SET;
    private static final Schema $SCHEMA_SAMPLE_COUNT = $SCHEMA.member("SampleCount");
    private static final Schema $SCHEMA_SUM = $SCHEMA.member("Sum");
    private static final Schema $SCHEMA_MINIMUM = $SCHEMA.member("Minimum");
    private static final Schema $SCHEMA_MAXIMUM = $SCHEMA.member("Maximum");

    public static final ShapeId $ID = $SCHEMA.id();

    private final transient Double sampleCount;
    private final transient Double sum;
    private final transient Double minimum;
    private final transient Double maximum;

    private StatisticSet(Builder builder) {
        this.sampleCount = builder.sampleCount;
        this.sum = builder.sum;
        this.minimum = builder.minimum;
        this.maximum = builder.maximum;
    }

    /**
     * The number of samples used for the statistic set.
     */
    public Double getSampleCount() {
        return sampleCount;
    }

    /**
     * The sum of values for the sample set.
     */
    public Double getSum() {
        return sum;
    }

    /**
     * The minimum value of the sample set.
     */
    public Double getMinimum() {
        return minimum;
    }

    /**
     * The maximum value of the sample set.
     */
    public Double getMaximum() {
        return maximum;
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
        StatisticSet that = (StatisticSet) other;
        return Objects.equals(this.sampleCount, that.sampleCount)
               && Objects.equals(this.sum, that.sum)
               && Objects.equals(this.minimum, that.minimum)
               && Objects.equals(this.maximum, that.maximum);
    }

    @Override
    public int hashCode() {
        int $hc = Objects.hashCode(sampleCount);
        $hc = 31 * $hc + Objects.hashCode(sum);
        $hc = 31 * $hc + Objects.hashCode(minimum);
        $hc = 31 * $hc + Objects.hashCode(maximum);
        return $hc;
    }

    @Override
    public Schema schema() {
        return $SCHEMA;
    }

    @Override
    public void serializeMembers(ShapeSerializer serializer) {
        if (sampleCount != null) {
            serializer.writeDouble($SCHEMA_SAMPLE_COUNT, sampleCount);
        }
        if (sum != null) {
            serializer.writeDouble($SCHEMA_SUM, sum);
        }
        if (minimum != null) {
            serializer.writeDouble($SCHEMA_MINIMUM, minimum);
        }
        if (maximum != null) {
            serializer.writeDouble($SCHEMA_MAXIMUM, maximum);
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T getMemberValue(Schema member) {
        return switch (member.memberIndex()) {
            case 0 -> (T) SchemaUtils.validateSameMember($SCHEMA_SAMPLE_COUNT, member, sampleCount);
            case 1 -> (T) SchemaUtils.validateSameMember($SCHEMA_SUM, member, sum);
            case 2 -> (T) SchemaUtils.validateSameMember($SCHEMA_MINIMUM, member, minimum);
            case 3 -> (T) SchemaUtils.validateSameMember($SCHEMA_MAXIMUM, member, maximum);
            default -> throw new IllegalArgumentException("Attempted to get non-existent member: " + member.id());
        };
    }

    /**
     * Create a new builder containing all the current property values of this object.
     *
     * <p><strong>Note:</strong> This method performs only a shallow copy of the original properties.
     *
     * @return a builder for {@link StatisticSet}.
     */
    public Builder toBuilder() {
        var builder = new Builder();
        builder.sampleCount(this.sampleCount);
        builder.sum(this.sum);
        builder.minimum(this.minimum);
        builder.maximum(this.maximum);
        return builder;
    }

    /**
     * @return returns a new Builder.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for {@link StatisticSet}.
     */
    public static final class Builder implements ShapeBuilder<StatisticSet> {
        private final PresenceTracker tracker = PresenceTracker.of($SCHEMA);
        private Double sampleCount;
        private Double sum;
        private Double minimum;
        private Double maximum;

        private Builder() {
            // Tell the tracker to assume clientOptional members are present.
            tracker.setMember($SCHEMA_SAMPLE_COUNT);
            tracker.setMember($SCHEMA_SUM);
            tracker.setMember($SCHEMA_MINIMUM);
            tracker.setMember($SCHEMA_MAXIMUM);
        }

        @Override
        public Schema schema() {
            return $SCHEMA;
        }

        /**
         * The number of samples used for the statistic set.
         *
         * <p><strong>Required</strong>
         * @return this builder.
         */
        public Builder sampleCount(Double sampleCount) {
            this.sampleCount = sampleCount;
            tracker.setMember($SCHEMA_SAMPLE_COUNT);
            return this;
        }

        /**
         * The sum of values for the sample set.
         *
         * <p><strong>Required</strong>
         * @return this builder.
         */
        public Builder sum(Double sum) {
            this.sum = sum;
            tracker.setMember($SCHEMA_SUM);
            return this;
        }

        /**
         * The minimum value of the sample set.
         *
         * <p><strong>Required</strong>
         * @return this builder.
         */
        public Builder minimum(Double minimum) {
            this.minimum = minimum;
            tracker.setMember($SCHEMA_MINIMUM);
            return this;
        }

        /**
         * The maximum value of the sample set.
         *
         * <p><strong>Required</strong>
         * @return this builder.
         */
        public Builder maximum(Double maximum) {
            this.maximum = maximum;
            tracker.setMember($SCHEMA_MAXIMUM);
            return this;
        }

        @Override
        public StatisticSet build() {
            tracker.validate();
            return new StatisticSet(this);
        }

        @Override
        @SuppressWarnings("unchecked")
        public void setMemberValue(Schema member, Object value) {
            switch (member.memberIndex()) {
                case 0 -> sampleCount((Double) SchemaUtils.validateSameMember($SCHEMA_SAMPLE_COUNT, member, value));
                case 1 -> sum((Double) SchemaUtils.validateSameMember($SCHEMA_SUM, member, value));
                case 2 -> minimum((Double) SchemaUtils.validateSameMember($SCHEMA_MINIMUM, member, value));
                case 3 -> maximum((Double) SchemaUtils.validateSameMember($SCHEMA_MAXIMUM, member, value));
                default -> ShapeBuilder.super.setMemberValue(member, value);
            }
        }

        @Override
        public ShapeBuilder<StatisticSet> errorCorrection() {
            if (tracker.allSet()) {
                return this;
            }
            if (!tracker.checkMember($SCHEMA_SAMPLE_COUNT)) {
                tracker.setMember($SCHEMA_SAMPLE_COUNT);
            }
            if (!tracker.checkMember($SCHEMA_SUM)) {
                tracker.setMember($SCHEMA_SUM);
            }
            if (!tracker.checkMember($SCHEMA_MINIMUM)) {
                tracker.setMember($SCHEMA_MINIMUM);
            }
            if (!tracker.checkMember($SCHEMA_MAXIMUM)) {
                tracker.setMember($SCHEMA_MAXIMUM);
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
                    case 0 -> builder.sampleCount(de.readDouble(member));
                    case 1 -> builder.sum(de.readDouble(member));
                    case 2 -> builder.minimum(de.readDouble(member));
                    case 3 -> builder.maximum(de.readDouble(member));
                    default -> throw new IllegalArgumentException("Unexpected member: " + member.memberName());
                }
            }
        }
    }
}
