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
public sealed interface InputFormat extends SmithyEnum, SerializableShape {
    InputFormat DYNAMODB_JSON = new DynamodbJsonType();
    InputFormat ION = new IonType();
    InputFormat CSV = new CsvType();
    List<InputFormat> $TYPES = List.of(DYNAMODB_JSON, ION, CSV);

    Schema $SCHEMA = Schema.createEnum(ShapeId.from("com.amazonaws.dynamodb#InputFormat"),
        Set.of(DYNAMODB_JSON.getValue(), ION.getValue(), CSV.getValue()), InputFormat.class
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
    static InputFormat unknown(String value) {
        return new $Unknown(value);
    }

    /**
     * Returns an unmodifiable list containing the constants of this enum type, in the order declared.
     */
    static List<InputFormat> values() {
        return $TYPES;
    }

    /**
     * Returns a {@link InputFormat} constant with the specified value.
     *
     * @param value value to create {@code InputFormat} from.
     * @throws IllegalArgumentException if value does not match a known value.
     */
    static InputFormat from(String value) {
        return switch (value) {
            case "DYNAMODB_JSON" -> DYNAMODB_JSON;
            case "ION" -> ION;
            case "CSV" -> CSV;
            default -> throw new IllegalArgumentException("Unknown value: " + value);
        };
    }

    final class DynamodbJsonType implements InputFormat {
        private DynamodbJsonType() {}

        @Override
        public String getValue() {
            return "DYNAMODB_JSON";
        }

        @Override
        public String toString() {
            return ToStringSerializer.serialize(this);
        }

    }

    final class IonType implements InputFormat {
        private IonType() {}

        @Override
        public String getValue() {
            return "ION";
        }

        @Override
        public String toString() {
            return ToStringSerializer.serialize(this);
        }

    }

    final class CsvType implements InputFormat {
        private CsvType() {}

        @Override
        public String getValue() {
            return "CSV";
        }

        @Override
        public String toString() {
            return ToStringSerializer.serialize(this);
        }

    }

    record $Unknown(String value) implements InputFormat {
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

        private final class $Hidden implements InputFormat {
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
     * Builder for {@link InputFormat}.
     */
    final class Builder implements ShapeBuilder<InputFormat> {
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
        public InputFormat build() {
            return switch (value) {
                case "DYNAMODB_JSON" -> DYNAMODB_JSON;
                case "ION" -> ION;
                case "CSV" -> CSV;
                default -> new $Unknown(value);
            };
        }

        @Override
        public Builder deserialize(ShapeDeserializer de) {
            return value(de.readString($SCHEMA));
        }
    }
}
