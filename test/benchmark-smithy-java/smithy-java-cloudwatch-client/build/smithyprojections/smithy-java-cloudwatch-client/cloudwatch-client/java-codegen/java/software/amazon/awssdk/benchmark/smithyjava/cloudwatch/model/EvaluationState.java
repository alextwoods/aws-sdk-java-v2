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
public sealed interface EvaluationState extends SmithyEnum, SerializableShape {
    EvaluationState PARTIAL_DATA = new PartialDataType();
    EvaluationState EVALUATION_FAILURE = new EvaluationFailureType();
    EvaluationState EVALUATION_ERROR = new EvaluationErrorType();
    List<EvaluationState> $TYPES = List.of(PARTIAL_DATA, EVALUATION_FAILURE, EVALUATION_ERROR);

    Schema $SCHEMA = Schema.createEnum(ShapeId.from("com.amazonaws.cloudwatch#EvaluationState"),
        Set.of(PARTIAL_DATA.getValue(), EVALUATION_FAILURE.getValue(), EVALUATION_ERROR.getValue()), EvaluationState.class
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
    static EvaluationState unknown(String value) {
        return new $Unknown(value);
    }

    /**
     * Returns an unmodifiable list containing the constants of this enum type, in the order declared.
     */
    static List<EvaluationState> values() {
        return $TYPES;
    }

    /**
     * Returns a {@link EvaluationState} constant with the specified value.
     *
     * @param value value to create {@code EvaluationState} from.
     * @throws IllegalArgumentException if value does not match a known value.
     */
    static EvaluationState from(String value) {
        return switch (value) {
            case "PARTIAL_DATA" -> PARTIAL_DATA;
            case "EVALUATION_FAILURE" -> EVALUATION_FAILURE;
            case "EVALUATION_ERROR" -> EVALUATION_ERROR;
            default -> throw new IllegalArgumentException("Unknown value: " + value);
        };
    }

    final class PartialDataType implements EvaluationState {
        private PartialDataType() {}

        @Override
        public String getValue() {
            return "PARTIAL_DATA";
        }

        @Override
        public String toString() {
            return ToStringSerializer.serialize(this);
        }

    }

    final class EvaluationFailureType implements EvaluationState {
        private EvaluationFailureType() {}

        @Override
        public String getValue() {
            return "EVALUATION_FAILURE";
        }

        @Override
        public String toString() {
            return ToStringSerializer.serialize(this);
        }

    }

    final class EvaluationErrorType implements EvaluationState {
        private EvaluationErrorType() {}

        @Override
        public String getValue() {
            return "EVALUATION_ERROR";
        }

        @Override
        public String toString() {
            return ToStringSerializer.serialize(this);
        }

    }

    record $Unknown(String value) implements EvaluationState {
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

        private final class $Hidden implements EvaluationState {
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
     * Builder for {@link EvaluationState}.
     */
    final class Builder implements ShapeBuilder<EvaluationState> {
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
        public EvaluationState build() {
            return switch (value) {
                case "PARTIAL_DATA" -> PARTIAL_DATA;
                case "EVALUATION_FAILURE" -> EVALUATION_FAILURE;
                case "EVALUATION_ERROR" -> EVALUATION_ERROR;
                default -> new $Unknown(value);
            };
        }

        @Override
        public Builder deserialize(ShapeDeserializer de) {
            return value(de.readString($SCHEMA));
        }
    }
}
