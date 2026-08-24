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
public sealed interface Protocol extends SmithyEnum, SerializableShape {
    Protocol HTTP = new HttpType();
    Protocol HTTPS = new HttpsType();
    List<Protocol> $TYPES = List.of(HTTP, HTTPS);

    Schema $SCHEMA = Schema.createEnum(ShapeId.from("com.amazonaws.s3#Protocol"),
        Set.of(HTTP.getValue(), HTTPS.getValue()), Protocol.class
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
    static Protocol unknown(String value) {
        return new $Unknown(value);
    }

    /**
     * Returns an unmodifiable list containing the constants of this enum type, in the order declared.
     */
    static List<Protocol> values() {
        return $TYPES;
    }

    /**
     * Returns a {@link Protocol} constant with the specified value.
     *
     * @param value value to create {@code Protocol} from.
     * @throws IllegalArgumentException if value does not match a known value.
     */
    static Protocol from(String value) {
        return switch (value) {
            case "http" -> HTTP;
            case "https" -> HTTPS;
            default -> throw new IllegalArgumentException("Unknown value: " + value);
        };
    }

    final class HttpType implements Protocol {
        private HttpType() {}

        @Override
        public String getValue() {
            return "http";
        }

        @Override
        public String toString() {
            return ToStringSerializer.serialize(this);
        }

    }

    final class HttpsType implements Protocol {
        private HttpsType() {}

        @Override
        public String getValue() {
            return "https";
        }

        @Override
        public String toString() {
            return ToStringSerializer.serialize(this);
        }

    }

    record $Unknown(String value) implements Protocol {
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

        private final class $Hidden implements Protocol {
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
     * Builder for {@link Protocol}.
     */
    final class Builder implements ShapeBuilder<Protocol> {
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
        public Protocol build() {
            return switch (value) {
                case "http" -> HTTP;
                case "https" -> HTTPS;
                default -> new $Unknown(value);
            };
        }

        @Override
        public Builder deserialize(ShapeDeserializer de) {
            return value(de.readString($SCHEMA));
        }
    }
}
