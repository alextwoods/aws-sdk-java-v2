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
public sealed interface KeyType extends SmithyEnum, SerializableShape {
    KeyType HASH = new HashType();
    KeyType RANGE = new RangeType();
    List<KeyType> $TYPES = List.of(HASH, RANGE);

    Schema $SCHEMA = Schema.createEnum(ShapeId.from("com.amazonaws.dynamodb#KeyType"),
        Set.of(HASH.getValue(), RANGE.getValue()), KeyType.class
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
    static KeyType unknown(String value) {
        return new $Unknown(value);
    }

    /**
     * Returns an unmodifiable list containing the constants of this enum type, in the order declared.
     */
    static List<KeyType> values() {
        return $TYPES;
    }

    /**
     * Returns a {@link KeyType} constant with the specified value.
     *
     * @param value value to create {@code KeyType} from.
     * @throws IllegalArgumentException if value does not match a known value.
     */
    static KeyType from(String value) {
        return switch (value) {
            case "HASH" -> HASH;
            case "RANGE" -> RANGE;
            default -> throw new IllegalArgumentException("Unknown value: " + value);
        };
    }

    final class HashType implements KeyType {
        private HashType() {}

        @Override
        public String getValue() {
            return "HASH";
        }

        @Override
        public String toString() {
            return ToStringSerializer.serialize(this);
        }

    }

    final class RangeType implements KeyType {
        private RangeType() {}

        @Override
        public String getValue() {
            return "RANGE";
        }

        @Override
        public String toString() {
            return ToStringSerializer.serialize(this);
        }

    }

    record $Unknown(String value) implements KeyType {
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

        private final class $Hidden implements KeyType {
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
     * Builder for {@link KeyType}.
     */
    final class Builder implements ShapeBuilder<KeyType> {
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
        public KeyType build() {
            return switch (value) {
                case "HASH" -> HASH;
                case "RANGE" -> RANGE;
                default -> new $Unknown(value);
            };
        }

        @Override
        public Builder deserialize(ShapeDeserializer de) {
            return value(de.readString($SCHEMA));
        }
    }
}
