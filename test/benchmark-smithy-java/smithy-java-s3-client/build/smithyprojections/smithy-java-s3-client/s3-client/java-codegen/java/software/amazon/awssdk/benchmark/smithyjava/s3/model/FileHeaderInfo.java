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
public sealed interface FileHeaderInfo extends SmithyEnum, SerializableShape {
    FileHeaderInfo USE = new UseType();
    FileHeaderInfo IGNORE = new IgnoreType();
    FileHeaderInfo NONE = new NoneType();
    List<FileHeaderInfo> $TYPES = List.of(USE, IGNORE, NONE);

    Schema $SCHEMA = Schema.createEnum(ShapeId.from("com.amazonaws.s3#FileHeaderInfo"),
        Set.of(USE.getValue(), IGNORE.getValue(), NONE.getValue()), FileHeaderInfo.class
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
    static FileHeaderInfo unknown(String value) {
        return new $Unknown(value);
    }

    /**
     * Returns an unmodifiable list containing the constants of this enum type, in the order declared.
     */
    static List<FileHeaderInfo> values() {
        return $TYPES;
    }

    /**
     * Returns a {@link FileHeaderInfo} constant with the specified value.
     *
     * @param value value to create {@code FileHeaderInfo} from.
     * @throws IllegalArgumentException if value does not match a known value.
     */
    static FileHeaderInfo from(String value) {
        return switch (value) {
            case "USE" -> USE;
            case "IGNORE" -> IGNORE;
            case "NONE" -> NONE;
            default -> throw new IllegalArgumentException("Unknown value: " + value);
        };
    }

    final class UseType implements FileHeaderInfo {
        private UseType() {}

        @Override
        public String getValue() {
            return "USE";
        }

        @Override
        public String toString() {
            return ToStringSerializer.serialize(this);
        }

    }

    final class IgnoreType implements FileHeaderInfo {
        private IgnoreType() {}

        @Override
        public String getValue() {
            return "IGNORE";
        }

        @Override
        public String toString() {
            return ToStringSerializer.serialize(this);
        }

    }

    final class NoneType implements FileHeaderInfo {
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

    record $Unknown(String value) implements FileHeaderInfo {
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

        private final class $Hidden implements FileHeaderInfo {
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
     * Builder for {@link FileHeaderInfo}.
     */
    final class Builder implements ShapeBuilder<FileHeaderInfo> {
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
        public FileHeaderInfo build() {
            return switch (value) {
                case "USE" -> USE;
                case "IGNORE" -> IGNORE;
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
