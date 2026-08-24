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
public sealed interface Select extends SmithyEnum, SerializableShape {
    Select ALL_ATTRIBUTES = new AllAttributesType();
    Select ALL_PROJECTED_ATTRIBUTES = new AllProjectedAttributesType();
    Select SPECIFIC_ATTRIBUTES = new SpecificAttributesType();
    Select COUNT = new CountType();
    List<Select> $TYPES = List.of(ALL_ATTRIBUTES, ALL_PROJECTED_ATTRIBUTES, SPECIFIC_ATTRIBUTES, COUNT);

    Schema $SCHEMA = Schema.createEnum(ShapeId.from("com.amazonaws.dynamodb#Select"),
        Set.of(ALL_ATTRIBUTES.getValue(), ALL_PROJECTED_ATTRIBUTES.getValue(), SPECIFIC_ATTRIBUTES.getValue(), COUNT.getValue()), Select.class
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
    static Select unknown(String value) {
        return new $Unknown(value);
    }

    /**
     * Returns an unmodifiable list containing the constants of this enum type, in the order declared.
     */
    static List<Select> values() {
        return $TYPES;
    }

    /**
     * Returns a {@link Select} constant with the specified value.
     *
     * @param value value to create {@code Select} from.
     * @throws IllegalArgumentException if value does not match a known value.
     */
    static Select from(String value) {
        return switch (value) {
            case "ALL_ATTRIBUTES" -> ALL_ATTRIBUTES;
            case "ALL_PROJECTED_ATTRIBUTES" -> ALL_PROJECTED_ATTRIBUTES;
            case "SPECIFIC_ATTRIBUTES" -> SPECIFIC_ATTRIBUTES;
            case "COUNT" -> COUNT;
            default -> throw new IllegalArgumentException("Unknown value: " + value);
        };
    }

    final class AllAttributesType implements Select {
        private AllAttributesType() {}

        @Override
        public String getValue() {
            return "ALL_ATTRIBUTES";
        }

        @Override
        public String toString() {
            return ToStringSerializer.serialize(this);
        }

    }

    final class AllProjectedAttributesType implements Select {
        private AllProjectedAttributesType() {}

        @Override
        public String getValue() {
            return "ALL_PROJECTED_ATTRIBUTES";
        }

        @Override
        public String toString() {
            return ToStringSerializer.serialize(this);
        }

    }

    final class SpecificAttributesType implements Select {
        private SpecificAttributesType() {}

        @Override
        public String getValue() {
            return "SPECIFIC_ATTRIBUTES";
        }

        @Override
        public String toString() {
            return ToStringSerializer.serialize(this);
        }

    }

    final class CountType implements Select {
        private CountType() {}

        @Override
        public String getValue() {
            return "COUNT";
        }

        @Override
        public String toString() {
            return ToStringSerializer.serialize(this);
        }

    }

    record $Unknown(String value) implements Select {
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

        private final class $Hidden implements Select {
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
     * Builder for {@link Select}.
     */
    final class Builder implements ShapeBuilder<Select> {
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
        public Select build() {
            return switch (value) {
                case "ALL_ATTRIBUTES" -> ALL_ATTRIBUTES;
                case "ALL_PROJECTED_ATTRIBUTES" -> ALL_PROJECTED_ATTRIBUTES;
                case "SPECIFIC_ATTRIBUTES" -> SPECIFIC_ATTRIBUTES;
                case "COUNT" -> COUNT;
                default -> new $Unknown(value);
            };
        }

        @Override
        public Builder deserialize(ShapeDeserializer de) {
            return value(de.readString($SCHEMA));
        }
    }
}
