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
public sealed interface DestinationStatus extends SmithyEnum, SerializableShape {
    DestinationStatus ENABLING = new EnablingType();
    DestinationStatus ACTIVE = new ActiveType();
    DestinationStatus DISABLING = new DisablingType();
    DestinationStatus DISABLED = new DisabledType();
    DestinationStatus ENABLE_FAILED = new EnableFailedType();
    DestinationStatus UPDATING = new UpdatingType();
    List<DestinationStatus> $TYPES = List.of(ENABLING, ACTIVE, DISABLING, DISABLED, ENABLE_FAILED, UPDATING);

    Schema $SCHEMA = Schema.createEnum(ShapeId.from("com.amazonaws.dynamodb#DestinationStatus"),
        Set.of(ENABLING.getValue(), ACTIVE.getValue(), DISABLING.getValue(), DISABLED.getValue(), ENABLE_FAILED.getValue(), UPDATING.getValue()), DestinationStatus.class
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
    static DestinationStatus unknown(String value) {
        return new $Unknown(value);
    }

    /**
     * Returns an unmodifiable list containing the constants of this enum type, in the order declared.
     */
    static List<DestinationStatus> values() {
        return $TYPES;
    }

    /**
     * Returns a {@link DestinationStatus} constant with the specified value.
     *
     * @param value value to create {@code DestinationStatus} from.
     * @throws IllegalArgumentException if value does not match a known value.
     */
    static DestinationStatus from(String value) {
        return switch (value) {
            case "ENABLING" -> ENABLING;
            case "ACTIVE" -> ACTIVE;
            case "DISABLING" -> DISABLING;
            case "DISABLED" -> DISABLED;
            case "ENABLE_FAILED" -> ENABLE_FAILED;
            case "UPDATING" -> UPDATING;
            default -> throw new IllegalArgumentException("Unknown value: " + value);
        };
    }

    final class EnablingType implements DestinationStatus {
        private EnablingType() {}

        @Override
        public String getValue() {
            return "ENABLING";
        }

        @Override
        public String toString() {
            return ToStringSerializer.serialize(this);
        }

    }

    final class ActiveType implements DestinationStatus {
        private ActiveType() {}

        @Override
        public String getValue() {
            return "ACTIVE";
        }

        @Override
        public String toString() {
            return ToStringSerializer.serialize(this);
        }

    }

    final class DisablingType implements DestinationStatus {
        private DisablingType() {}

        @Override
        public String getValue() {
            return "DISABLING";
        }

        @Override
        public String toString() {
            return ToStringSerializer.serialize(this);
        }

    }

    final class DisabledType implements DestinationStatus {
        private DisabledType() {}

        @Override
        public String getValue() {
            return "DISABLED";
        }

        @Override
        public String toString() {
            return ToStringSerializer.serialize(this);
        }

    }

    final class EnableFailedType implements DestinationStatus {
        private EnableFailedType() {}

        @Override
        public String getValue() {
            return "ENABLE_FAILED";
        }

        @Override
        public String toString() {
            return ToStringSerializer.serialize(this);
        }

    }

    final class UpdatingType implements DestinationStatus {
        private UpdatingType() {}

        @Override
        public String getValue() {
            return "UPDATING";
        }

        @Override
        public String toString() {
            return ToStringSerializer.serialize(this);
        }

    }

    record $Unknown(String value) implements DestinationStatus {
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

        private final class $Hidden implements DestinationStatus {
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
     * Builder for {@link DestinationStatus}.
     */
    final class Builder implements ShapeBuilder<DestinationStatus> {
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
        public DestinationStatus build() {
            return switch (value) {
                case "ENABLING" -> ENABLING;
                case "ACTIVE" -> ACTIVE;
                case "DISABLING" -> DISABLING;
                case "DISABLED" -> DISABLED;
                case "ENABLE_FAILED" -> ENABLE_FAILED;
                case "UPDATING" -> UPDATING;
                default -> new $Unknown(value);
            };
        }

        @Override
        public Builder deserialize(ShapeDeserializer de) {
            return value(de.readString($SCHEMA));
        }
    }
}
