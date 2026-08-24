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
public sealed interface CompressionType extends SmithyEnum, SerializableShape {
    CompressionType NONE = new NoneType();
    CompressionType GZIP = new GzipType();
    CompressionType BZIP2 = new Bzip2Type();
    List<CompressionType> $TYPES = List.of(NONE, GZIP, BZIP2);

    Schema $SCHEMA = Schema.createEnum(ShapeId.from("com.amazonaws.s3#CompressionType"),
        Set.of(NONE.getValue(), GZIP.getValue(), BZIP2.getValue()), CompressionType.class
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
    static CompressionType unknown(String value) {
        return new $Unknown(value);
    }

    /**
     * Returns an unmodifiable list containing the constants of this enum type, in the order declared.
     */
    static List<CompressionType> values() {
        return $TYPES;
    }

    /**
     * Returns a {@link CompressionType} constant with the specified value.
     *
     * @param value value to create {@code CompressionType} from.
     * @throws IllegalArgumentException if value does not match a known value.
     */
    static CompressionType from(String value) {
        return switch (value) {
            case "NONE" -> NONE;
            case "GZIP" -> GZIP;
            case "BZIP2" -> BZIP2;
            default -> throw new IllegalArgumentException("Unknown value: " + value);
        };
    }

    final class NoneType implements CompressionType {
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

    final class GzipType implements CompressionType {
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

    final class Bzip2Type implements CompressionType {
        private Bzip2Type() {}

        @Override
        public String getValue() {
            return "BZIP2";
        }

        @Override
        public String toString() {
            return ToStringSerializer.serialize(this);
        }

    }

    record $Unknown(String value) implements CompressionType {
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

        private final class $Hidden implements CompressionType {
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
     * Builder for {@link CompressionType}.
     */
    final class Builder implements ShapeBuilder<CompressionType> {
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
        public CompressionType build() {
            return switch (value) {
                case "NONE" -> NONE;
                case "GZIP" -> GZIP;
                case "BZIP2" -> BZIP2;
                default -> new $Unknown(value);
            };
        }

        @Override
        public Builder deserialize(ShapeDeserializer de) {
            return value(de.readString($SCHEMA));
        }
    }
}
