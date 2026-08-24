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
public sealed interface ObjectLockLegalHoldStatus extends SmithyEnum, SerializableShape {
    ObjectLockLegalHoldStatus ON = new OnType();
    ObjectLockLegalHoldStatus OFF = new OffType();
    List<ObjectLockLegalHoldStatus> $TYPES = List.of(ON, OFF);

    Schema $SCHEMA = Schema.createEnum(ShapeId.from("com.amazonaws.s3#ObjectLockLegalHoldStatus"),
        Set.of(ON.getValue(), OFF.getValue()), ObjectLockLegalHoldStatus.class
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
    static ObjectLockLegalHoldStatus unknown(String value) {
        return new $Unknown(value);
    }

    /**
     * Returns an unmodifiable list containing the constants of this enum type, in the order declared.
     */
    static List<ObjectLockLegalHoldStatus> values() {
        return $TYPES;
    }

    /**
     * Returns a {@link ObjectLockLegalHoldStatus} constant with the specified value.
     *
     * @param value value to create {@code ObjectLockLegalHoldStatus} from.
     * @throws IllegalArgumentException if value does not match a known value.
     */
    static ObjectLockLegalHoldStatus from(String value) {
        return switch (value) {
            case "ON" -> ON;
            case "OFF" -> OFF;
            default -> throw new IllegalArgumentException("Unknown value: " + value);
        };
    }

    final class OnType implements ObjectLockLegalHoldStatus {
        private OnType() {}

        @Override
        public String getValue() {
            return "ON";
        }

        @Override
        public String toString() {
            return ToStringSerializer.serialize(this);
        }

    }

    final class OffType implements ObjectLockLegalHoldStatus {
        private OffType() {}

        @Override
        public String getValue() {
            return "OFF";
        }

        @Override
        public String toString() {
            return ToStringSerializer.serialize(this);
        }

    }

    record $Unknown(String value) implements ObjectLockLegalHoldStatus {
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

        private final class $Hidden implements ObjectLockLegalHoldStatus {
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
     * Builder for {@link ObjectLockLegalHoldStatus}.
     */
    final class Builder implements ShapeBuilder<ObjectLockLegalHoldStatus> {
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
        public ObjectLockLegalHoldStatus build() {
            return switch (value) {
                case "ON" -> ON;
                case "OFF" -> OFF;
                default -> new $Unknown(value);
            };
        }

        @Override
        public Builder deserialize(ShapeDeserializer de) {
            return value(de.readString($SCHEMA));
        }
    }
}
