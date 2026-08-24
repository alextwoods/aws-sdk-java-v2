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
public sealed interface ConditionalOperator extends SmithyEnum, SerializableShape {
    ConditionalOperator AND = new AndType();
    ConditionalOperator OR = new OrType();
    List<ConditionalOperator> $TYPES = List.of(AND, OR);

    Schema $SCHEMA = Schema.createEnum(ShapeId.from("com.amazonaws.dynamodb#ConditionalOperator"),
        Set.of(AND.getValue(), OR.getValue()), ConditionalOperator.class
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
    static ConditionalOperator unknown(String value) {
        return new $Unknown(value);
    }

    /**
     * Returns an unmodifiable list containing the constants of this enum type, in the order declared.
     */
    static List<ConditionalOperator> values() {
        return $TYPES;
    }

    /**
     * Returns a {@link ConditionalOperator} constant with the specified value.
     *
     * @param value value to create {@code ConditionalOperator} from.
     * @throws IllegalArgumentException if value does not match a known value.
     */
    static ConditionalOperator from(String value) {
        return switch (value) {
            case "AND" -> AND;
            case "OR" -> OR;
            default -> throw new IllegalArgumentException("Unknown value: " + value);
        };
    }

    final class AndType implements ConditionalOperator {
        private AndType() {}

        @Override
        public String getValue() {
            return "AND";
        }

        @Override
        public String toString() {
            return ToStringSerializer.serialize(this);
        }

    }

    final class OrType implements ConditionalOperator {
        private OrType() {}

        @Override
        public String getValue() {
            return "OR";
        }

        @Override
        public String toString() {
            return ToStringSerializer.serialize(this);
        }

    }

    record $Unknown(String value) implements ConditionalOperator {
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

        private final class $Hidden implements ConditionalOperator {
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
     * Builder for {@link ConditionalOperator}.
     */
    final class Builder implements ShapeBuilder<ConditionalOperator> {
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
        public ConditionalOperator build() {
            return switch (value) {
                case "AND" -> AND;
                case "OR" -> OR;
                default -> new $Unknown(value);
            };
        }

        @Override
        public Builder deserialize(ShapeDeserializer de) {
            return value(de.readString($SCHEMA));
        }
    }
}
