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
public sealed interface InputCompressionType extends SmithyEnum, SerializableShape {
    InputCompressionType GZIP = new GzipType();
    InputCompressionType ZSTD = new ZstdType();
    InputCompressionType NONE = new NoneType();
    List<InputCompressionType> $TYPES = List.of(GZIP, ZSTD, NONE);

    Schema $SCHEMA = Schema.createEnum(ShapeId.from("com.amazonaws.dynamodb#InputCompressionType"),
        Set.of(GZIP.getValue(), ZSTD.getValue(), NONE.getValue()), InputCompressionType.class
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
    static InputCompressionType unknown(String value) {
        return new $Unknown(value);
    }

    /**
     * Returns an unmodifiable list containing the constants of this enum type, in the order declared.
     */
    static List<InputCompressionType> values() {
        return $TYPES;
    }

    /**
     * Returns a {@link InputCompressionType} constant with the specified value.
     *
     * @param value value to create {@code InputCompressionType} from.
     * @throws IllegalArgumentException if value does not match a known value.
     */
    static InputCompressionType from(String value) {
        return switch (value) {
            case "GZIP" -> GZIP;
            case "ZSTD" -> ZSTD;
            case "NONE" -> NONE;
            default -> throw new IllegalArgumentException("Unknown value: " + value);
        };
    }

    final class GzipType implements InputCompressionType {
        private GzipType() {}

        @Override
        public String getValue() {
            return "GZIP";
        }

        @Override
        public String toString() {
            return ToStringSerializer.serialize(this);
        }

    }

    final class ZstdType implements InputCompressionType {
        private ZstdType() {}

        @Override
        public String getValue() {
            return "ZSTD";
        }

        @Override
        public String toString() {
            return ToStringSerializer.serialize(this);
        }

    }

    final class NoneType implements InputCompressionType {
        private NoneType() {}

        @Override
        public String getValue() {
            return "NONE";
        }

        @Override
        public String toString() {
            return ToStringSerializer.serialize(this);
        }

    }

    record $Unknown(String value) implements InputCompressionType {
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

        private final class $Hidden implements InputCompressionType {
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
     * Builder for {@link InputCompressionType}.
     */
    final class Builder implements ShapeBuilder<InputCompressionType> {
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
        public InputCompressionType build() {
            return switch (value) {
                case "GZIP" -> GZIP;
                case "ZSTD" -> ZSTD;
                case "NONE" -> NONE;
                default -> new $Unknown(value);
            };
        }

        @Override
        public Builder deserialize(ShapeDeserializer de) {
            return value(de.readString($SCHEMA));
        }
    }
}
