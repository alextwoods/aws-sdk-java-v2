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
public sealed interface OptionalObjectAttributes extends SmithyEnum, SerializableShape {
    OptionalObjectAttributes RESTORE_STATUS = new RestoreStatusType();
    List<OptionalObjectAttributes> $TYPES = List.of(RESTORE_STATUS);

    Schema $SCHEMA = Schema.createEnum(ShapeId.from("com.amazonaws.s3#OptionalObjectAttributes"),
        Set.of(RESTORE_STATUS.getValue()), OptionalObjectAttributes.class
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
    static OptionalObjectAttributes unknown(String value) {
        return new $Unknown(value);
    }

    /**
     * Returns an unmodifiable list containing the constants of this enum type, in the order declared.
     */
    static List<OptionalObjectAttributes> values() {
        return $TYPES;
    }

    /**
     * Returns a {@link OptionalObjectAttributes} constant with the specified value.
     *
     * @param value value to create {@code OptionalObjectAttributes} from.
     * @throws IllegalArgumentException if value does not match a known value.
     */
    static OptionalObjectAttributes from(String value) {
        return switch (value) {
            case "RestoreStatus" -> RESTORE_STATUS;
            default -> throw new IllegalArgumentException("Unknown value: " + value);
        };
    }

    final class RestoreStatusType implements OptionalObjectAttributes {
        private RestoreStatusType() {}

        @Override
        public String getValue() {
            return "RestoreStatus";
        }

        @Override
        public String toString() {
            return ToStringSerializer.serialize(this);
        }

    }

    record $Unknown(String value) implements OptionalObjectAttributes {
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

        private final class $Hidden implements OptionalObjectAttributes {
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
     * Builder for {@link OptionalObjectAttributes}.
     */
    final class Builder implements ShapeBuilder<OptionalObjectAttributes> {
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
        public OptionalObjectAttributes build() {
            return switch (value) {
                case "RestoreStatus" -> RESTORE_STATUS;
                default -> new $Unknown(value);
            };
        }

        @Override
        public Builder deserialize(ShapeDeserializer de) {
            return value(de.readString($SCHEMA));
        }
    }
}
