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
public sealed interface BucketLogsPermission extends SmithyEnum, SerializableShape {
    BucketLogsPermission FULL_CONTROL = new FullControlType();
    BucketLogsPermission READ = new ReadType();
    BucketLogsPermission WRITE = new WriteType();
    List<BucketLogsPermission> $TYPES = List.of(FULL_CONTROL, READ, WRITE);

    Schema $SCHEMA = Schema.createEnum(ShapeId.from("com.amazonaws.s3#BucketLogsPermission"),
        Set.of(FULL_CONTROL.getValue(), READ.getValue(), WRITE.getValue()), BucketLogsPermission.class
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
    static BucketLogsPermission unknown(String value) {
        return new $Unknown(value);
    }

    /**
     * Returns an unmodifiable list containing the constants of this enum type, in the order declared.
     */
    static List<BucketLogsPermission> values() {
        return $TYPES;
    }

    /**
     * Returns a {@link BucketLogsPermission} constant with the specified value.
     *
     * @param value value to create {@code BucketLogsPermission} from.
     * @throws IllegalArgumentException if value does not match a known value.
     */
    static BucketLogsPermission from(String value) {
        return switch (value) {
            case "FULL_CONTROL" -> FULL_CONTROL;
            case "READ" -> READ;
            case "WRITE" -> WRITE;
            default -> throw new IllegalArgumentException("Unknown value: " + value);
        };
    }

    final class FullControlType implements BucketLogsPermission {
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

    final class ReadType implements BucketLogsPermission {
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

    final class WriteType implements BucketLogsPermission {
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

    record $Unknown(String value) implements BucketLogsPermission {
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

        private final class $Hidden implements BucketLogsPermission {
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
     * Builder for {@link BucketLogsPermission}.
     */
    final class Builder implements ShapeBuilder<BucketLogsPermission> {
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
        public BucketLogsPermission build() {
            return switch (value) {
                case "FULL_CONTROL" -> FULL_CONTROL;
                case "READ" -> READ;
                case "WRITE" -> WRITE;
                default -> new $Unknown(value);
            };
        }

        @Override
        public Builder deserialize(ShapeDeserializer de) {
            return value(de.readString($SCHEMA));
        }
    }
}
