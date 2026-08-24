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
public sealed interface Permission extends SmithyEnum, SerializableShape {
    Permission FULL_CONTROL = new FullControlType();
    Permission WRITE = new WriteType();
    Permission WRITE_ACP = new WriteAcpType();
    Permission READ = new ReadType();
    Permission READ_ACP = new ReadAcpType();
    List<Permission> $TYPES = List.of(FULL_CONTROL, WRITE, WRITE_ACP, READ, READ_ACP);

    Schema $SCHEMA = Schema.createEnum(ShapeId.from("com.amazonaws.s3#Permission"),
        Set.of(FULL_CONTROL.getValue(), WRITE.getValue(), WRITE_ACP.getValue(), READ.getValue(), READ_ACP.getValue()), Permission.class
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
    static Permission unknown(String value) {
        return new $Unknown(value);
    }

    /**
     * Returns an unmodifiable list containing the constants of this enum type, in the order declared.
     */
    static List<Permission> values() {
        return $TYPES;
    }

    /**
     * Returns a {@link Permission} constant with the specified value.
     *
     * @param value value to create {@code Permission} from.
     * @throws IllegalArgumentException if value does not match a known value.
     */
    static Permission from(String value) {
        return switch (value) {
            case "FULL_CONTROL" -> FULL_CONTROL;
            case "WRITE" -> WRITE;
            case "WRITE_ACP" -> WRITE_ACP;
            case "READ" -> READ;
            case "READ_ACP" -> READ_ACP;
            default -> throw new IllegalArgumentException("Unknown value: " + value);
        };
    }

    final class FullControlType implements Permission {
        private FullControlType() {}

        @Override
        public String getValue() {
            return "FULL_CONTROL";
        }

        @Override
        public String toString() {
            return ToStringSerializer.serialize(this);
        }

    }

    final class WriteType implements Permission {
        private WriteType() {}

        @Override
        public String getValue() {
            return "WRITE";
        }

        @Override
        public String toString() {
            return ToStringSerializer.serialize(this);
        }

    }

    final class WriteAcpType implements Permission {
        private WriteAcpType() {}

        @Override
        public String getValue() {
            return "WRITE_ACP";
        }

        @Override
        public String toString() {
            return ToStringSerializer.serialize(this);
        }

    }

    final class ReadType implements Permission {
        private ReadType() {}

        @Override
        public String getValue() {
            return "READ";
        }

        @Override
        public String toString() {
            return ToStringSerializer.serialize(this);
        }

    }

    final class ReadAcpType implements Permission {
        private ReadAcpType() {}

        @Override
        public String getValue() {
            return "READ_ACP";
        }

        @Override
        public String toString() {
            return ToStringSerializer.serialize(this);
        }

    }

    record $Unknown(String value) implements Permission {
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

        private final class $Hidden implements Permission {
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
     * Builder for {@link Permission}.
     */
    final class Builder implements ShapeBuilder<Permission> {
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
        public Permission build() {
            return switch (value) {
                case "FULL_CONTROL" -> FULL_CONTROL;
                case "WRITE" -> WRITE;
                case "WRITE_ACP" -> WRITE_ACP;
                case "READ" -> READ;
                case "READ_ACP" -> READ_ACP;
                default -> new $Unknown(value);
            };
        }

        @Override
        public Builder deserialize(ShapeDeserializer de) {
            return value(de.readString($SCHEMA));
        }
    }
}
