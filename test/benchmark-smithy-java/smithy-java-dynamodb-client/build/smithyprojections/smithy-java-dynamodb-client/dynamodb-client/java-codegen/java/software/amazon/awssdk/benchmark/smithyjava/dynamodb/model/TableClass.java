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
public sealed interface TableClass extends SmithyEnum, SerializableShape {
    TableClass STANDARD = new StandardType();
    TableClass STANDARD_INFREQUENT_ACCESS = new StandardInfrequentAccessType();
    List<TableClass> $TYPES = List.of(STANDARD, STANDARD_INFREQUENT_ACCESS);

    Schema $SCHEMA = Schema.createEnum(ShapeId.from("com.amazonaws.dynamodb#TableClass"),
        Set.of(STANDARD.getValue(), STANDARD_INFREQUENT_ACCESS.getValue()), TableClass.class
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
    static TableClass unknown(String value) {
        return new $Unknown(value);
    }

    /**
     * Returns an unmodifiable list containing the constants of this enum type, in the order declared.
     */
    static List<TableClass> values() {
        return $TYPES;
    }

    /**
     * Returns a {@link TableClass} constant with the specified value.
     *
     * @param value value to create {@code TableClass} from.
     * @throws IllegalArgumentException if value does not match a known value.
     */
    static TableClass from(String value) {
        return switch (value) {
            case "STANDARD" -> STANDARD;
            case "STANDARD_INFREQUENT_ACCESS" -> STANDARD_INFREQUENT_ACCESS;
            default -> throw new IllegalArgumentException("Unknown value: " + value);
        };
    }

    final class StandardType implements TableClass {
        private StandardType() {}

        @Override
        public String getValue() {
            return "STANDARD";
        }

        @Override
        public String toString() {
            return ToStringSerializer.serialize(this);
        }

    }

    final class StandardInfrequentAccessType implements TableClass {
        private StandardInfrequentAccessType() {}

        @Override
        public String getValue() {
            return "STANDARD_INFREQUENT_ACCESS";
        }

        @Override
        public String toString() {
            return ToStringSerializer.serialize(this);
        }

    }

    record $Unknown(String value) implements TableClass {
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

        private final class $Hidden implements TableClass {
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
     * Builder for {@link TableClass}.
     */
    final class Builder implements ShapeBuilder<TableClass> {
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
        public TableClass build() {
            return switch (value) {
                case "STANDARD" -> STANDARD;
                case "STANDARD_INFREQUENT_ACCESS" -> STANDARD_INFREQUENT_ACCESS;
                default -> new $Unknown(value);
            };
        }

        @Override
        public Builder deserialize(ShapeDeserializer de) {
            return value(de.readString($SCHEMA));
        }
    }
}
