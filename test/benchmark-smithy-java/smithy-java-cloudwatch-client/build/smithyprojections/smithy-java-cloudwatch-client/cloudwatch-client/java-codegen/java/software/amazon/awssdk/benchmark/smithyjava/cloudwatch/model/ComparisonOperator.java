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
public sealed interface ComparisonOperator extends SmithyEnum, SerializableShape {
    ComparisonOperator GREATER_THAN_OR_EQUAL_TO_THRESHOLD = new GreaterThanOrEqualToThresholdType();
    ComparisonOperator GREATER_THAN_THRESHOLD = new GreaterThanThresholdType();
    ComparisonOperator LESS_THAN_THRESHOLD = new LessThanThresholdType();
    ComparisonOperator LESS_THAN_OR_EQUAL_TO_THRESHOLD = new LessThanOrEqualToThresholdType();
    ComparisonOperator LESS_THAN_LOWER_OR_GREATER_THAN_UPPER_THRESHOLD = new LessThanLowerOrGreaterThanUpperThresholdType();
    ComparisonOperator LESS_THAN_LOWER_THRESHOLD = new LessThanLowerThresholdType();
    ComparisonOperator GREATER_THAN_UPPER_THRESHOLD = new GreaterThanUpperThresholdType();
    List<ComparisonOperator> $TYPES = List.of(GREATER_THAN_OR_EQUAL_TO_THRESHOLD, GREATER_THAN_THRESHOLD, LESS_THAN_THRESHOLD, LESS_THAN_OR_EQUAL_TO_THRESHOLD, LESS_THAN_LOWER_OR_GREATER_THAN_UPPER_THRESHOLD, LESS_THAN_LOWER_THRESHOLD, GREATER_THAN_UPPER_THRESHOLD);

    Schema $SCHEMA = Schema.createEnum(ShapeId.from("com.amazonaws.cloudwatch#ComparisonOperator"),
        Set.of(GREATER_THAN_OR_EQUAL_TO_THRESHOLD.getValue(), GREATER_THAN_THRESHOLD.getValue(), LESS_THAN_THRESHOLD.getValue(), LESS_THAN_OR_EQUAL_TO_THRESHOLD.getValue(), LESS_THAN_LOWER_OR_GREATER_THAN_UPPER_THRESHOLD.getValue(), LESS_THAN_LOWER_THRESHOLD.getValue(), GREATER_THAN_UPPER_THRESHOLD.getValue()), ComparisonOperator.class
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
    static ComparisonOperator unknown(String value) {
        return new $Unknown(value);
    }

    /**
     * Returns an unmodifiable list containing the constants of this enum type, in the order declared.
     */
    static List<ComparisonOperator> values() {
        return $TYPES;
    }

    /**
     * Returns a {@link ComparisonOperator} constant with the specified value.
     *
     * @param value value to create {@code ComparisonOperator} from.
     * @throws IllegalArgumentException if value does not match a known value.
     */
    static ComparisonOperator from(String value) {
        return switch (value) {
            case "GreaterThanOrEqualToThreshold" -> GREATER_THAN_OR_EQUAL_TO_THRESHOLD;
            case "GreaterThanThreshold" -> GREATER_THAN_THRESHOLD;
            case "LessThanThreshold" -> LESS_THAN_THRESHOLD;
            case "LessThanOrEqualToThreshold" -> LESS_THAN_OR_EQUAL_TO_THRESHOLD;
            case "LessThanLowerOrGreaterThanUpperThreshold" -> LESS_THAN_LOWER_OR_GREATER_THAN_UPPER_THRESHOLD;
            case "LessThanLowerThreshold" -> LESS_THAN_LOWER_THRESHOLD;
            case "GreaterThanUpperThreshold" -> GREATER_THAN_UPPER_THRESHOLD;
            default -> throw new IllegalArgumentException("Unknown value: " + value);
        };
    }

    final class GreaterThanOrEqualToThresholdType implements ComparisonOperator {
        private GreaterThanOrEqualToThresholdType() {}

        @Override
        public String getValue() {
            return "GreaterThanOrEqualToThreshold";
        }

        @Override
        public String toString() {
            return ToStringSerializer.serialize(this);
        }

    }

    final class GreaterThanThresholdType implements ComparisonOperator {
        private GreaterThanThresholdType() {}

        @Override
        public String getValue() {
            return "GreaterThanThreshold";
        }

        @Override
        public String toString() {
            return ToStringSerializer.serialize(this);
        }

    }

    final class LessThanThresholdType implements ComparisonOperator {
        private LessThanThresholdType() {}

        @Override
        public String getValue() {
            return "LessThanThreshold";
        }

        @Override
        public String toString() {
            return ToStringSerializer.serialize(this);
        }

    }

    final class LessThanOrEqualToThresholdType implements ComparisonOperator {
        private LessThanOrEqualToThresholdType() {}

        @Override
        public String getValue() {
            return "LessThanOrEqualToThreshold";
        }

        @Override
        public String toString() {
            return ToStringSerializer.serialize(this);
        }

    }

    final class LessThanLowerOrGreaterThanUpperThresholdType implements ComparisonOperator {
        private LessThanLowerOrGreaterThanUpperThresholdType() {}

        @Override
        public String getValue() {
            return "LessThanLowerOrGreaterThanUpperThreshold";
        }

        @Override
        public String toString() {
            return ToStringSerializer.serialize(this);
        }

    }

    final class LessThanLowerThresholdType implements ComparisonOperator {
        private LessThanLowerThresholdType() {}

        @Override
        public String getValue() {
            return "LessThanLowerThreshold";
        }

        @Override
        public String toString() {
            return ToStringSerializer.serialize(this);
        }

    }

    final class GreaterThanUpperThresholdType implements ComparisonOperator {
        private GreaterThanUpperThresholdType() {}

        @Override
        public String getValue() {
            return "GreaterThanUpperThreshold";
        }

        @Override
        public String toString() {
            return ToStringSerializer.serialize(this);
        }

    }

    record $Unknown(String value) implements ComparisonOperator {
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

        private final class $Hidden implements ComparisonOperator {
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
     * Builder for {@link ComparisonOperator}.
     */
    final class Builder implements ShapeBuilder<ComparisonOperator> {
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
        public ComparisonOperator build() {
            return switch (value) {
                case "GreaterThanOrEqualToThreshold" -> GREATER_THAN_OR_EQUAL_TO_THRESHOLD;
                case "GreaterThanThreshold" -> GREATER_THAN_THRESHOLD;
                case "LessThanThreshold" -> LESS_THAN_THRESHOLD;
                case "LessThanOrEqualToThreshold" -> LESS_THAN_OR_EQUAL_TO_THRESHOLD;
                case "LessThanLowerOrGreaterThanUpperThreshold" -> LESS_THAN_LOWER_OR_GREATER_THAN_UPPER_THRESHOLD;
                case "LessThanLowerThreshold" -> LESS_THAN_LOWER_THRESHOLD;
                case "GreaterThanUpperThreshold" -> GREATER_THAN_UPPER_THRESHOLD;
                default -> new $Unknown(value);
            };
        }

        @Override
        public Builder deserialize(ShapeDeserializer de) {
            return value(de.readString($SCHEMA));
        }
    }
}
