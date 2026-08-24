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
public sealed interface ActionsSuppressedBy extends SmithyEnum, SerializableShape {
    ActionsSuppressedBy WAIT_PERIOD = new WaitPeriodType();
    ActionsSuppressedBy EXTENSION_PERIOD = new ExtensionPeriodType();
    ActionsSuppressedBy ALARM = new AlarmType();
    List<ActionsSuppressedBy> $TYPES = List.of(WAIT_PERIOD, EXTENSION_PERIOD, ALARM);

    Schema $SCHEMA = Schema.createEnum(ShapeId.from("com.amazonaws.cloudwatch#ActionsSuppressedBy"),
        Set.of(WAIT_PERIOD.getValue(), EXTENSION_PERIOD.getValue(), ALARM.getValue()), ActionsSuppressedBy.class
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
    static ActionsSuppressedBy unknown(String value) {
        return new $Unknown(value);
    }

    /**
     * Returns an unmodifiable list containing the constants of this enum type, in the order declared.
     */
    static List<ActionsSuppressedBy> values() {
        return $TYPES;
    }

    /**
     * Returns a {@link ActionsSuppressedBy} constant with the specified value.
     *
     * @param value value to create {@code ActionsSuppressedBy} from.
     * @throws IllegalArgumentException if value does not match a known value.
     */
    static ActionsSuppressedBy from(String value) {
        return switch (value) {
            case "WaitPeriod" -> WAIT_PERIOD;
            case "ExtensionPeriod" -> EXTENSION_PERIOD;
            case "Alarm" -> ALARM;
            default -> throw new IllegalArgumentException("Unknown value: " + value);
        };
    }

    final class WaitPeriodType implements ActionsSuppressedBy {
        private WaitPeriodType() {}

        @Override
        public String getValue() {
            return "WaitPeriod";
        }

        @Override
        public String toString() {
            return ToStringSerializer.serialize(this);
        }

    }

    final class ExtensionPeriodType implements ActionsSuppressedBy {
        private ExtensionPeriodType() {}

        @Override
        public String getValue() {
            return "ExtensionPeriod";
        }

        @Override
        public String toString() {
            return ToStringSerializer.serialize(this);
        }

    }

    final class AlarmType implements ActionsSuppressedBy {
        private AlarmType() {}

        @Override
        public String getValue() {
            return "Alarm";
        }

        @Override
        public String toString() {
            return ToStringSerializer.serialize(this);
        }

    }

    record $Unknown(String value) implements ActionsSuppressedBy {
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

        private final class $Hidden implements ActionsSuppressedBy {
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
     * Builder for {@link ActionsSuppressedBy}.
     */
    final class Builder implements ShapeBuilder<ActionsSuppressedBy> {
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
        public ActionsSuppressedBy build() {
            return switch (value) {
                case "WaitPeriod" -> WAIT_PERIOD;
                case "ExtensionPeriod" -> EXTENSION_PERIOD;
                case "Alarm" -> ALARM;
                default -> new $Unknown(value);
            };
        }

        @Override
        public Builder deserialize(ShapeDeserializer de) {
            return value(de.readString($SCHEMA));
        }
    }
}
