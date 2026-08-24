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
public sealed interface RestoreRequestType extends SmithyEnum, SerializableShape {
    RestoreRequestType SELECT = new SelectType();
    List<RestoreRequestType> $TYPES = List.of(SELECT);

    Schema $SCHEMA = Schema.createEnum(ShapeId.from("com.amazonaws.s3#RestoreRequestType"),
        Set.of(SELECT.getValue()), RestoreRequestType.class
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
    static RestoreRequestType unknown(String value) {
        return new $Unknown(value);
    }

    /**
     * Returns an unmodifiable list containing the constants of this enum type, in the order declared.
     */
    static List<RestoreRequestType> values() {
        return $TYPES;
    }

    /**
     * Returns a {@link RestoreRequestType} constant with the specified value.
     *
     * @param value value to create {@code RestoreRequestType} from.
     * @throws IllegalArgumentException if value does not match a known value.
     */
    static RestoreRequestType from(String value) {
        return switch (value) {
            case "SELECT" -> SELECT;
            default -> throw new IllegalArgumentException("Unknown value: " + value);
        };
    }

    final class SelectType implements RestoreRequestType {
        private SelectType() {}

        @Override
        public String getValue() {
            return "SELECT";
        }

        @Override
        public String toString() {
            return ToStringSerializer.serialize(this);
        }

    }

    record $Unknown(String value) implements RestoreRequestType {
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

        private final class $Hidden implements RestoreRequestType {
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
     * Builder for {@link RestoreRequestType}.
     */
    final class Builder implements ShapeBuilder<RestoreRequestType> {
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
        public RestoreRequestType build() {
            return switch (value) {
                case "SELECT" -> SELECT;
                default -> new $Unknown(value);
            };
        }

        @Override
        public Builder deserialize(ShapeDeserializer de) {
            return value(de.readString($SCHEMA));
        }
    }
}
