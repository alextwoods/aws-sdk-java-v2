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
public sealed interface ProjectionType extends SmithyEnum, SerializableShape {
    ProjectionType ALL = new AllType();
    ProjectionType KEYS_ONLY = new KeysOnlyType();
    ProjectionType INCLUDE = new IncludeType();
    List<ProjectionType> $TYPES = List.of(ALL, KEYS_ONLY, INCLUDE);

    Schema $SCHEMA = Schema.createEnum(ShapeId.from("com.amazonaws.dynamodb#ProjectionType"),
        Set.of(ALL.getValue(), KEYS_ONLY.getValue(), INCLUDE.getValue()), ProjectionType.class
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
    static ProjectionType unknown(String value) {
        return new $Unknown(value);
    }

    /**
     * Returns an unmodifiable list containing the constants of this enum type, in the order declared.
     */
    static List<ProjectionType> values() {
        return $TYPES;
    }

    /**
     * Returns a {@link ProjectionType} constant with the specified value.
     *
     * @param value value to create {@code ProjectionType} from.
     * @throws IllegalArgumentException if value does not match a known value.
     */
    static ProjectionType from(String value) {
        return switch (value) {
            case "ALL" -> ALL;
            case "KEYS_ONLY" -> KEYS_ONLY;
            case "INCLUDE" -> INCLUDE;
            default -> throw new IllegalArgumentException("Unknown value: " + value);
        };
    }

    final class AllType implements ProjectionType {
        private AllType() {}

        @Override
        public String getValue() {
            return "ALL";
        }

        @Override
        public String toString() {
            return ToStringSerializer.serialize(this);
        }

    }

    final class KeysOnlyType implements ProjectionType {
        private KeysOnlyType() {}

        @Override
        public String getValue() {
            return "KEYS_ONLY";
        }

        @Override
        public String toString() {
            return ToStringSerializer.serialize(this);
        }

    }

    final class IncludeType implements ProjectionType {
        private IncludeType() {}

        @Override
        public String getValue() {
            return "INCLUDE";
        }

        @Override
        public String toString() {
            return ToStringSerializer.serialize(this);
        }

    }

    record $Unknown(String value) implements ProjectionType {
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

        private final class $Hidden implements ProjectionType {
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
     * Builder for {@link ProjectionType}.
     */
    final class Builder implements ShapeBuilder<ProjectionType> {
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
        public ProjectionType build() {
            return switch (value) {
                case "ALL" -> ALL;
                case "KEYS_ONLY" -> KEYS_ONLY;
                case "INCLUDE" -> INCLUDE;
                default -> new $Unknown(value);
            };
        }

        @Override
        public Builder deserialize(ShapeDeserializer de) {
            return value(de.readString($SCHEMA));
        }
    }
}
