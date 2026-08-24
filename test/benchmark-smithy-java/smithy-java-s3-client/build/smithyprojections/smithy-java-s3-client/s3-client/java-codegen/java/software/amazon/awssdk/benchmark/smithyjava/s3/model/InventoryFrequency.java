package software.amazon.awssdk.benchmark.smithyjava.s3.model;

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
public sealed interface InventoryFrequency extends SmithyEnum, SerializableShape {
    InventoryFrequency DAILY = new DailyType();
    InventoryFrequency WEEKLY = new WeeklyType();
    List<InventoryFrequency> $TYPES = List.of(DAILY, WEEKLY);

    Schema $SCHEMA = Schema.createEnum(ShapeId.from("com.amazonaws.s3#InventoryFrequency"),
        Set.of(DAILY.getValue(), WEEKLY.getValue()), InventoryFrequency.class
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
    static InventoryFrequency unknown(String value) {
        return new $Unknown(value);
    }

    /**
     * Returns an unmodifiable list containing the constants of this enum type, in the order declared.
     */
    static List<InventoryFrequency> values() {
        return $TYPES;
    }

    /**
     * Returns a {@link InventoryFrequency} constant with the specified value.
     *
     * @param value value to create {@code InventoryFrequency} from.
     * @throws IllegalArgumentException if value does not match a known value.
     */
    static InventoryFrequency from(String value) {
        return switch (value) {
            case "Daily" -> DAILY;
            case "Weekly" -> WEEKLY;
            default -> throw new IllegalArgumentException("Unknown value: " + value);
        };
    }

    final class DailyType implements InventoryFrequency {
        private DailyType() {}

        @Override
        public String getValue() {
            return "Daily";
        }

        @Override
        public String toString() {
            return ToStringSerializer.serialize(this);
        }

    }

    final class WeeklyType implements InventoryFrequency {
        private WeeklyType() {}

        @Override
        public String getValue() {
            return "Weekly";
        }

        @Override
        public String toString() {
            return ToStringSerializer.serialize(this);
        }

    }

    record $Unknown(String value) implements InventoryFrequency {
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

        private final class $Hidden implements InventoryFrequency {
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
     * Builder for {@link InventoryFrequency}.
     */
    final class Builder implements ShapeBuilder<InventoryFrequency> {
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
        public InventoryFrequency build() {
            return switch (value) {
                case "Daily" -> DAILY;
                case "Weekly" -> WEEKLY;
                default -> new $Unknown(value);
            };
        }

        @Override
        public Builder deserialize(ShapeDeserializer de) {
            return value(de.readString($SCHEMA));
        }
    }
}
