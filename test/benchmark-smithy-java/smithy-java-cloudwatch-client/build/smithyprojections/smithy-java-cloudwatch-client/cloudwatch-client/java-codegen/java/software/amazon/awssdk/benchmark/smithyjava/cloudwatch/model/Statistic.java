package software.amazon.awssdk.benchmark.smithyjava.cloudwatch.model;

import java.util.List;
import java.util.Objects;
import java.util.Set;
import software.amazon.smithy.java.core.schema.Schema;
import software.amazon.smithy.java.core.schema.SerializableShape;
import software.amazon.smithy.java.core.schema.ShapeBuilder;
import software.amazon.smithy.java.core.schema.SmithyEnum;
import software.amazon.smithy.java.core.serde.ShapeDeserializer;
import software.amazon.smithy.java.core.serde.ShapeSerializer;
import software.amazon.smithy.java.core.serde.ToStringSerializer;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.utils.SmithyGenerated;

@SmithyGenerated
public sealed interface Statistic extends SmithyEnum, SerializableShape {
    Statistic SAMPLE_COUNT = new SampleCountType();
    Statistic AVERAGE = new AverageType();
    Statistic SUM = new SumType();
    Statistic MINIMUM = new MinimumType();
    Statistic MAXIMUM = new MaximumType();
    List<Statistic> $TYPES = List.of(SAMPLE_COUNT, AVERAGE, SUM, MINIMUM, MAXIMUM);

    Schema $SCHEMA = Schema.createEnum(ShapeId.from("com.amazonaws.cloudwatch#Statistic"),
        Set.of(SAMPLE_COUNT.getValue(), AVERAGE.getValue(), SUM.getValue(), MINIMUM.getValue(), MAXIMUM.getValue()), Statistic.class
    );

    ShapeId $ID = $SCHEMA.id();

    String getValue();

    @Override
    default void serialize(ShapeSerializer serializer) {
        serializer.writeString($SCHEMA, getValue());
    }

    /**
     * Create an unknown enum variant with the given value.
     *
     * @param value value for the unknown variant.
     */
    static Statistic unknown(String value) {
        return new $Unknown(value);
    }

    /**
     * Returns an unmodifiable list containing the constants of this enum type, in the order declared.
     */
    static List<Statistic> values() {
        return $TYPES;
    }

    /**
     * Returns a {@link Statistic} constant with the specified value.
     *
     * @param value value to create {@code Statistic} from.
     * @throws IllegalArgumentException if value does not match a known value.
     */
    static Statistic from(String value) {
        return switch (value) {
            case "SampleCount" -> SAMPLE_COUNT;
            case "Average" -> AVERAGE;
            case "Sum" -> SUM;
            case "Minimum" -> MINIMUM;
            case "Maximum" -> MAXIMUM;
            default -> throw new IllegalArgumentException("Unknown value: " + value);
        };
    }

    final class SampleCountType implements Statistic {
        private SampleCountType() {}

        @Override
        public String getValue() {
            return "SampleCount";
        }

        @Override
        public String toString() {
            return ToStringSerializer.serialize(this);
        }

    }

    final class AverageType implements Statistic {
        private AverageType() {}

        @Override
        public String getValue() {
            return "Average";
        }

        @Override
        public String toString() {
            return ToStringSerializer.serialize(this);
        }

    }

    final class SumType implements Statistic {
        private SumType() {}

        @Override
        public String getValue() {
            return "Sum";
        }

        @Override
        public String toString() {
            return ToStringSerializer.serialize(this);
        }

    }

    final class MinimumType implements Statistic {
        private MinimumType() {}

        @Override
        public String getValue() {
            return "Minimum";
        }

        @Override
        public String toString() {
            return ToStringSerializer.serialize(this);
        }

    }

    final class MaximumType implements Statistic {
        private MaximumType() {}

        @Override
        public String getValue() {
            return "Maximum";
        }

        @Override
        public String toString() {
            return ToStringSerializer.serialize(this);
        }

    }

    record $Unknown(String value) implements Statistic {
        public $Unknown {
            Objects.requireNonNull(value, "Value cannot be null");
        }

        @Override
        public String getValue() {
            return value;
        }

        @Override
        public String toString() {
            return ToStringSerializer.serialize(this);
        }

        private final class $Hidden implements Statistic {
            @Override
            public String getValue() {
                return null;
            }
        }
    }

    /**
     * @return returns a new Builder.
     */
    static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for {@link Statistic}.
     */
    final class Builder implements ShapeBuilder<Statistic> {
        private String value;

        private Builder() {}

        @Override
        public Schema schema() {
            return $SCHEMA;
        }

        private Builder value(String value) {
            this.value = Objects.requireNonNull(value, "Enum value cannot be null");
            return this;
        }

        @Override
        public Statistic build() {
            return switch (value) {
                case "SampleCount" -> SAMPLE_COUNT;
                case "Average" -> AVERAGE;
                case "Sum" -> SUM;
                case "Minimum" -> MINIMUM;
                case "Maximum" -> MAXIMUM;
                default -> new $Unknown(value);
            };
        }

        @Override
        public Builder deserialize(ShapeDeserializer de) {
            return value(de.readString($SCHEMA));
        }
    }
}
