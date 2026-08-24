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
public sealed interface StateValue extends SmithyEnum, SerializableShape {
    StateValue OK = new OkType();
    StateValue ALARM = new AlarmType();
    StateValue INSUFFICIENT_DATA = new InsufficientDataType();
    List<StateValue> $TYPES = List.of(OK, ALARM, INSUFFICIENT_DATA);

    Schema $SCHEMA = Schema.createEnum(ShapeId.from("com.amazonaws.cloudwatch#StateValue"),
        Set.of(OK.getValue(), ALARM.getValue(), INSUFFICIENT_DATA.getValue()), StateValue.class
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
    static StateValue unknown(String value) {
        return new $Unknown(value);
    }

    /**
     * Returns an unmodifiable list containing the constants of this enum type, in the order declared.
     */
    static List<StateValue> values() {
        return $TYPES;
    }

    /**
     * Returns a {@link StateValue} constant with the specified value.
     *
     * @param value value to create {@code StateValue} from.
     * @throws IllegalArgumentException if value does not match a known value.
     */
    static StateValue from(String value) {
        return switch (value) {
            case "OK" -> OK;
            case "ALARM" -> ALARM;
            case "INSUFFICIENT_DATA" -> INSUFFICIENT_DATA;
            default -> throw new IllegalArgumentException("Unknown value: " + value);
        };
    }

    final class OkType implements StateValue {
        private OkType() {}

        @Override
        public String getValue() {
            return "OK";
        }

        @Override
        public String toString() {
            return ToStringSerializer.serialize(this);
        }

    }

    final class AlarmType implements StateValue {
        private AlarmType() {}

        @Override
        public String getValue() {
            return "ALARM";
        }

        @Override
        public String toString() {
            return ToStringSerializer.serialize(this);
        }

    }

    final class InsufficientDataType implements StateValue {
        private InsufficientDataType() {}

        @Override
        public String getValue() {
            return "INSUFFICIENT_DATA";
        }

        @Override
        public String toString() {
            return ToStringSerializer.serialize(this);
        }

    }

    record $Unknown(String value) implements StateValue {
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

        private final class $Hidden implements StateValue {
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
     * Builder for {@link StateValue}.
     */
    final class Builder implements ShapeBuilder<StateValue> {
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
        public StateValue build() {
            return switch (value) {
                case "OK" -> OK;
                case "ALARM" -> ALARM;
                case "INSUFFICIENT_DATA" -> INSUFFICIENT_DATA;
                default -> new $Unknown(value);
            };
        }

        @Override
        public Builder deserialize(ShapeDeserializer de) {
            return value(de.readString($SCHEMA));
        }
    }
}
