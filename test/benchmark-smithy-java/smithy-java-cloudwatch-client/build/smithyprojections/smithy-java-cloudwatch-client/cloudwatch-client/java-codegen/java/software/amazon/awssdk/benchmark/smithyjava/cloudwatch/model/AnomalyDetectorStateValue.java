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
public sealed interface AnomalyDetectorStateValue extends SmithyEnum, SerializableShape {
    AnomalyDetectorStateValue PENDING_TRAINING = new PendingTrainingType();
    AnomalyDetectorStateValue TRAINED_INSUFFICIENT_DATA = new TrainedInsufficientDataType();
    AnomalyDetectorStateValue TRAINED = new TrainedType();
    List<AnomalyDetectorStateValue> $TYPES = List.of(PENDING_TRAINING, TRAINED_INSUFFICIENT_DATA, TRAINED);

    Schema $SCHEMA = Schema.createEnum(ShapeId.from("com.amazonaws.cloudwatch#AnomalyDetectorStateValue"),
        Set.of(PENDING_TRAINING.getValue(), TRAINED_INSUFFICIENT_DATA.getValue(), TRAINED.getValue()), AnomalyDetectorStateValue.class
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
    static AnomalyDetectorStateValue unknown(String value) {
        return new $Unknown(value);
    }

    /**
     * Returns an unmodifiable list containing the constants of this enum type, in the order declared.
     */
    static List<AnomalyDetectorStateValue> values() {
        return $TYPES;
    }

    /**
     * Returns a {@link AnomalyDetectorStateValue} constant with the specified value.
     *
     * @param value value to create {@code AnomalyDetectorStateValue} from.
     * @throws IllegalArgumentException if value does not match a known value.
     */
    static AnomalyDetectorStateValue from(String value) {
        return switch (value) {
            case "PENDING_TRAINING" -> PENDING_TRAINING;
            case "TRAINED_INSUFFICIENT_DATA" -> TRAINED_INSUFFICIENT_DATA;
            case "TRAINED" -> TRAINED;
            default -> throw new IllegalArgumentException("Unknown value: " + value);
        };
    }

    final class PendingTrainingType implements AnomalyDetectorStateValue {
        private PendingTrainingType() {}

        @Override
        public String getValue() {
            return "PENDING_TRAINING";
        }

        @Override
        public String toString() {
            return ToStringSerializer.serialize(this);
        }

    }

    final class TrainedInsufficientDataType implements AnomalyDetectorStateValue {
        private TrainedInsufficientDataType() {}

        @Override
        public String getValue() {
            return "TRAINED_INSUFFICIENT_DATA";
        }

        @Override
        public String toString() {
            return ToStringSerializer.serialize(this);
        }

    }

    final class TrainedType implements AnomalyDetectorStateValue {
        private TrainedType() {}

        @Override
        public String getValue() {
            return "TRAINED";
        }

        @Override
        public String toString() {
            return ToStringSerializer.serialize(this);
        }

    }

    record $Unknown(String value) implements AnomalyDetectorStateValue {
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

        private final class $Hidden implements AnomalyDetectorStateValue {
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
     * Builder for {@link AnomalyDetectorStateValue}.
     */
    final class Builder implements ShapeBuilder<AnomalyDetectorStateValue> {
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
        public AnomalyDetectorStateValue build() {
            return switch (value) {
                case "PENDING_TRAINING" -> PENDING_TRAINING;
                case "TRAINED_INSUFFICIENT_DATA" -> TRAINED_INSUFFICIENT_DATA;
                case "TRAINED" -> TRAINED;
                default -> new $Unknown(value);
            };
        }

        @Override
        public Builder deserialize(ShapeDeserializer de) {
            return value(de.readString($SCHEMA));
        }
    }
}
