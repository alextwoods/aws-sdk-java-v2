package software.amazon.awssdk.benchmark.smithyjava.dynamodb.model;

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
public sealed interface ReturnValue extends SmithyEnum, SerializableShape {
    ReturnValue NONE = new NoneType();
    ReturnValue ALL_OLD = new AllOldType();
    ReturnValue UPDATED_OLD = new UpdatedOldType();
    ReturnValue ALL_NEW = new AllNewType();
    ReturnValue UPDATED_NEW = new UpdatedNewType();
    List<ReturnValue> $TYPES = List.of(NONE, ALL_OLD, UPDATED_OLD, ALL_NEW, UPDATED_NEW);

    Schema $SCHEMA = Schema.createEnum(ShapeId.from("com.amazonaws.dynamodb#ReturnValue"),
        Set.of(NONE.getValue(), ALL_OLD.getValue(), UPDATED_OLD.getValue(), ALL_NEW.getValue(), UPDATED_NEW.getValue()), ReturnValue.class
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
    static ReturnValue unknown(String value) {
        return new $Unknown(value);
    }

    /**
     * Returns an unmodifiable list containing the constants of this enum type, in the order declared.
     */
    static List<ReturnValue> values() {
        return $TYPES;
    }

    /**
     * Returns a {@link ReturnValue} constant with the specified value.
     *
     * @param value value to create {@code ReturnValue} from.
     * @throws IllegalArgumentException if value does not match a known value.
     */
    static ReturnValue from(String value) {
        return switch (value) {
            case "NONE" -> NONE;
            case "ALL_OLD" -> ALL_OLD;
            case "UPDATED_OLD" -> UPDATED_OLD;
            case "ALL_NEW" -> ALL_NEW;
            case "UPDATED_NEW" -> UPDATED_NEW;
            default -> throw new IllegalArgumentException("Unknown value: " + value);
        };
    }

    final class NoneType implements ReturnValue {
        private NoneType() {}

        @Override
        public String getValue() {
            return "NONE";
        }

        @Override
        public String toString() {
            return ToStringSerializer.serialize(this);
        }

    }

    final class AllOldType implements ReturnValue {
        private AllOldType() {}

        @Override
        public String getValue() {
            return "ALL_OLD";
        }

        @Override
        public String toString() {
            return ToStringSerializer.serialize(this);
        }

    }

    final class UpdatedOldType implements ReturnValue {
        private UpdatedOldType() {}

        @Override
        public String getValue() {
            return "UPDATED_OLD";
        }

        @Override
        public String toString() {
            return ToStringSerializer.serialize(this);
        }

    }

    final class AllNewType implements ReturnValue {
        private AllNewType() {}

        @Override
        public String getValue() {
            return "ALL_NEW";
        }

        @Override
        public String toString() {
            return ToStringSerializer.serialize(this);
        }

    }

    final class UpdatedNewType implements ReturnValue {
        private UpdatedNewType() {}

        @Override
        public String getValue() {
            return "UPDATED_NEW";
        }

        @Override
        public String toString() {
            return ToStringSerializer.serialize(this);
        }

    }

    record $Unknown(String value) implements ReturnValue {
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

        private final class $Hidden implements ReturnValue {
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
     * Builder for {@link ReturnValue}.
     */
    final class Builder implements ShapeBuilder<ReturnValue> {
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
        public ReturnValue build() {
            return switch (value) {
                case "NONE" -> NONE;
                case "ALL_OLD" -> ALL_OLD;
                case "UPDATED_OLD" -> UPDATED_OLD;
                case "ALL_NEW" -> ALL_NEW;
                case "UPDATED_NEW" -> UPDATED_NEW;
                default -> new $Unknown(value);
            };
        }

        @Override
        public Builder deserialize(ShapeDeserializer de) {
            return value(de.readString($SCHEMA));
        }
    }
}
