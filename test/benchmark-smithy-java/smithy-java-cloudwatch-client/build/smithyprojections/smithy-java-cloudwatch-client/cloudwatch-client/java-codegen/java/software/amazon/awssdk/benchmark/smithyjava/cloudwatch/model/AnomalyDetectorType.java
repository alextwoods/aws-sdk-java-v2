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
public sealed interface AnomalyDetectorType extends SmithyEnum, SerializableShape {
    AnomalyDetectorType SINGLE_METRIC = new SingleMetricType();
    AnomalyDetectorType METRIC_MATH = new MetricMathType();
    List<AnomalyDetectorType> $TYPES = List.of(SINGLE_METRIC, METRIC_MATH);

    Schema $SCHEMA = Schema.createEnum(ShapeId.from("com.amazonaws.cloudwatch#AnomalyDetectorType"),
        Set.of(SINGLE_METRIC.getValue(), METRIC_MATH.getValue()), AnomalyDetectorType.class
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
    static AnomalyDetectorType unknown(String value) {
        return new $Unknown(value);
    }

    /**
     * Returns an unmodifiable list containing the constants of this enum type, in the order declared.
     */
    static List<AnomalyDetectorType> values() {
        return $TYPES;
    }

    /**
     * Returns a {@link AnomalyDetectorType} constant with the specified value.
     *
     * @param value value to create {@code AnomalyDetectorType} from.
     * @throws IllegalArgumentException if value does not match a known value.
     */
    static AnomalyDetectorType from(String value) {
        return switch (value) {
            case "SINGLE_METRIC" -> SINGLE_METRIC;
            case "METRIC_MATH" -> METRIC_MATH;
            default -> throw new IllegalArgumentException("Unknown value: " + value);
        };
    }

    final class SingleMetricType implements AnomalyDetectorType {
        private SingleMetricType() {}

        @Override
        public String getValue() {
            return "SINGLE_METRIC";
        }

        @Override
        public String toString() {
            return ToStringSerializer.serialize(this);
        }

    }

    final class MetricMathType implements AnomalyDetectorType {
        private MetricMathType() {}

        @Override
        public String getValue() {
            return "METRIC_MATH";
        }

        @Override
        public String toString() {
            return ToStringSerializer.serialize(this);
        }

    }

    record $Unknown(String value) implements AnomalyDetectorType {
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

        private final class $Hidden implements AnomalyDetectorType {
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
     * Builder for {@link AnomalyDetectorType}.
     */
    final class Builder implements ShapeBuilder<AnomalyDetectorType> {
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
        public AnomalyDetectorType build() {
            return switch (value) {
                case "SINGLE_METRIC" -> SINGLE_METRIC;
                case "METRIC_MATH" -> METRIC_MATH;
                default -> new $Unknown(value);
            };
        }

        @Override
        public Builder deserialize(ShapeDeserializer de) {
            return value(de.readString($SCHEMA));
        }
    }
}
