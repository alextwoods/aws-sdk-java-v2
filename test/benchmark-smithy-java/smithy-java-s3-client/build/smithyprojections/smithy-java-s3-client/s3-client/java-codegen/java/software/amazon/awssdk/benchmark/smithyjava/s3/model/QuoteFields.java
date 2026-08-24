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
public sealed interface QuoteFields extends SmithyEnum, SerializableShape {
    QuoteFields ALWAYS = new AlwaysType();
    QuoteFields ASNEEDED = new AsneededType();
    List<QuoteFields> $TYPES = List.of(ALWAYS, ASNEEDED);

    Schema $SCHEMA = Schema.createEnum(ShapeId.from("com.amazonaws.s3#QuoteFields"),
        Set.of(ALWAYS.getValue(), ASNEEDED.getValue()), QuoteFields.class
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
    static QuoteFields unknown(String value) {
        return new $Unknown(value);
    }

    /**
     * Returns an unmodifiable list containing the constants of this enum type, in the order declared.
     */
    static List<QuoteFields> values() {
        return $TYPES;
    }

    /**
     * Returns a {@link QuoteFields} constant with the specified value.
     *
     * @param value value to create {@code QuoteFields} from.
     * @throws IllegalArgumentException if value does not match a known value.
     */
    static QuoteFields from(String value) {
        return switch (value) {
            case "ALWAYS" -> ALWAYS;
            case "ASNEEDED" -> ASNEEDED;
            default -> throw new IllegalArgumentException("Unknown value: " + value);
        };
    }

    final class AlwaysType implements QuoteFields {
        private AlwaysType() {}

        @Override
        public String getValue() {
            return "ALWAYS";
        }

        @Override
        public String toString() {
            return ToStringSerializer.serialize(this);
        }

    }

    final class AsneededType implements QuoteFields {
        private AsneededType() {}

        @Override
        public String getValue() {
            return "ASNEEDED";
        }

        @Override
        public String toString() {
            return ToStringSerializer.serialize(this);
        }

    }

    record $Unknown(String value) implements QuoteFields {
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

        private final class $Hidden implements QuoteFields {
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
     * Builder for {@link QuoteFields}.
     */
    final class Builder implements ShapeBuilder<QuoteFields> {
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
        public QuoteFields build() {
            return switch (value) {
                case "ALWAYS" -> ALWAYS;
                case "ASNEEDED" -> ASNEEDED;
                default -> new $Unknown(value);
            };
        }

        @Override
        public Builder deserialize(ShapeDeserializer de) {
            return value(de.readString($SCHEMA));
        }
    }
}
