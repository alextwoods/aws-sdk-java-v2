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
public sealed interface SessionMode extends SmithyEnum, SerializableShape {
    SessionMode READ_ONLY = new ReadOnlyType();
    SessionMode READ_WRITE = new ReadWriteType();
    List<SessionMode> $TYPES = List.of(READ_ONLY, READ_WRITE);

    Schema $SCHEMA = Schema.createEnum(ShapeId.from("com.amazonaws.s3#SessionMode"),
        Set.of(READ_ONLY.getValue(), READ_WRITE.getValue()), SessionMode.class
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
    static SessionMode unknown(String value) {
        return new $Unknown(value);
    }

    /**
     * Returns an unmodifiable list containing the constants of this enum type, in the order declared.
     */
    static List<SessionMode> values() {
        return $TYPES;
    }

    /**
     * Returns a {@link SessionMode} constant with the specified value.
     *
     * @param value value to create {@code SessionMode} from.
     * @throws IllegalArgumentException if value does not match a known value.
     */
    static SessionMode from(String value) {
        return switch (value) {
            case "ReadOnly" -> READ_ONLY;
            case "ReadWrite" -> READ_WRITE;
            default -> throw new IllegalArgumentException("Unknown value: " + value);
        };
    }

    final class ReadOnlyType implements SessionMode {
        private ReadOnlyType() {}

        @Override
        public String getValue() {
            return "ReadOnly";
        }

        @Override
        public String toString() {
            return ToStringSerializer.serialize(this);
        }

    }

    final class ReadWriteType implements SessionMode {
        private ReadWriteType() {}

        @Override
        public String getValue() {
            return "ReadWrite";
        }

        @Override
        public String toString() {
            return ToStringSerializer.serialize(this);
        }

    }

    record $Unknown(String value) implements SessionMode {
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

        private final class $Hidden implements SessionMode {
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
     * Builder for {@link SessionMode}.
     */
    final class Builder implements ShapeBuilder<SessionMode> {
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
        public SessionMode build() {
            return switch (value) {
                case "ReadOnly" -> READ_ONLY;
                case "ReadWrite" -> READ_WRITE;
                default -> new $Unknown(value);
            };
        }

        @Override
        public Builder deserialize(ShapeDeserializer de) {
            return value(de.readString($SCHEMA));
        }
    }
}
