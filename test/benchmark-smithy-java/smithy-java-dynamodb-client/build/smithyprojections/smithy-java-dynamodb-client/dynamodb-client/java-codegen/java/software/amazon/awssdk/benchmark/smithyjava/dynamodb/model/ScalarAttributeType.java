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
public sealed interface ScalarAttributeType extends SmithyEnum, SerializableShape {
    ScalarAttributeType S = new SType();
    ScalarAttributeType N = new NType();
    ScalarAttributeType B = new BType();
    List<ScalarAttributeType> $TYPES = List.of(S, N, B);

    Schema $SCHEMA = Schema.createEnum(ShapeId.from("com.amazonaws.dynamodb#ScalarAttributeType"),
        Set.of(S.getValue(), N.getValue(), B.getValue()), ScalarAttributeType.class
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
    static ScalarAttributeType unknown(String value) {
        return new $Unknown(value);
    }

    /**
     * Returns an unmodifiable list containing the constants of this enum type, in the order declared.
     */
    static List<ScalarAttributeType> values() {
        return $TYPES;
    }

    /**
     * Returns a {@link ScalarAttributeType} constant with the specified value.
     *
     * @param value value to create {@code ScalarAttributeType} from.
     * @throws IllegalArgumentException if value does not match a known value.
     */
    static ScalarAttributeType from(String value) {
        return switch (value) {
            case "S" -> S;
            case "N" -> N;
            case "B" -> B;
            default -> throw new IllegalArgumentException("Unknown value: " + value);
        };
    }

    final class SType implements ScalarAttributeType {
        private SType() {}

        @Override
        public String getValue() {
            return "S";
        }

        @Override
        public String toString() {
            return ToStringSerializer.serialize(this);
        }

    }

    final class NType implements ScalarAttributeType {
        private NType() {}

        @Override
        public String getValue() {
            return "N";
        }

        @Override
        public String toString() {
            return ToStringSerializer.serialize(this);
        }

    }

    final class BType implements ScalarAttributeType {
        private BType() {}

        @Override
        public String getValue() {
            return "B";
        }

        @Override
        public String toString() {
            return ToStringSerializer.serialize(this);
        }

    }

    record $Unknown(String value) implements ScalarAttributeType {
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

        private final class $Hidden implements ScalarAttributeType {
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
     * Builder for {@link ScalarAttributeType}.
     */
    final class Builder implements ShapeBuilder<ScalarAttributeType> {
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
        public ScalarAttributeType build() {
            return switch (value) {
                case "S" -> S;
                case "N" -> N;
                case "B" -> B;
                default -> new $Unknown(value);
            };
        }

        @Override
        public Builder deserialize(ShapeDeserializer de) {
            return value(de.readString($SCHEMA));
        }
    }
}
