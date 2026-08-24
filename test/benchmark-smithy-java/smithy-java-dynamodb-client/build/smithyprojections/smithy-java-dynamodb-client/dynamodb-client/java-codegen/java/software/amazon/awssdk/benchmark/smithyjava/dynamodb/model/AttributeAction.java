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
public sealed interface AttributeAction extends SmithyEnum, SerializableShape {
    AttributeAction ADD = new AddType();
    AttributeAction PUT = new PutType();
    AttributeAction DELETE = new DeleteType();
    List<AttributeAction> $TYPES = List.of(ADD, PUT, DELETE);

    Schema $SCHEMA = Schema.createEnum(ShapeId.from("com.amazonaws.dynamodb#AttributeAction"),
        Set.of(ADD.getValue(), PUT.getValue(), DELETE.getValue()), AttributeAction.class
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
    static AttributeAction unknown(String value) {
        return new $Unknown(value);
    }

    /**
     * Returns an unmodifiable list containing the constants of this enum type, in the order declared.
     */
    static List<AttributeAction> values() {
        return $TYPES;
    }

    /**
     * Returns a {@link AttributeAction} constant with the specified value.
     *
     * @param value value to create {@code AttributeAction} from.
     * @throws IllegalArgumentException if value does not match a known value.
     */
    static AttributeAction from(String value) {
        return switch (value) {
            case "ADD" -> ADD;
            case "PUT" -> PUT;
            case "DELETE" -> DELETE;
            default -> throw new IllegalArgumentException("Unknown value: " + value);
        };
    }

    final class AddType implements AttributeAction {
        private AddType() {}

        @Override
        public String getValue() {
            return "ADD";
        }

        @Override
        public String toString() {
            return ToStringSerializer.serialize(this);
        }

    }

    final class PutType implements AttributeAction {
        private PutType() {}

        @Override
        public String getValue() {
            return "PUT";
        }

        @Override
        public String toString() {
            return ToStringSerializer.serialize(this);
        }

    }

    final class DeleteType implements AttributeAction {
        private DeleteType() {}

        @Override
        public String getValue() {
            return "DELETE";
        }

        @Override
        public String toString() {
            return ToStringSerializer.serialize(this);
        }

    }

    record $Unknown(String value) implements AttributeAction {
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

        private final class $Hidden implements AttributeAction {
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
     * Builder for {@link AttributeAction}.
     */
    final class Builder implements ShapeBuilder<AttributeAction> {
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
        public AttributeAction build() {
            return switch (value) {
                case "ADD" -> ADD;
                case "PUT" -> PUT;
                case "DELETE" -> DELETE;
                default -> new $Unknown(value);
            };
        }

        @Override
        public Builder deserialize(ShapeDeserializer de) {
            return value(de.readString($SCHEMA));
        }
    }
}
