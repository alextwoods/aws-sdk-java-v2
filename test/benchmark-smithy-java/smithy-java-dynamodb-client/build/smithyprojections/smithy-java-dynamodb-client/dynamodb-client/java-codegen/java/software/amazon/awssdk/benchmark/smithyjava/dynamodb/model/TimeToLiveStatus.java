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
public sealed interface TimeToLiveStatus extends SmithyEnum, SerializableShape {
    TimeToLiveStatus ENABLING = new EnablingType();
    TimeToLiveStatus DISABLING = new DisablingType();
    TimeToLiveStatus ENABLED = new EnabledType();
    TimeToLiveStatus DISABLED = new DisabledType();
    List<TimeToLiveStatus> $TYPES = List.of(ENABLING, DISABLING, ENABLED, DISABLED);

    Schema $SCHEMA = Schema.createEnum(ShapeId.from("com.amazonaws.dynamodb#TimeToLiveStatus"),
        Set.of(ENABLING.getValue(), DISABLING.getValue(), ENABLED.getValue(), DISABLED.getValue()), TimeToLiveStatus.class
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
    static TimeToLiveStatus unknown(String value) {
        return new $Unknown(value);
    }

    /**
     * Returns an unmodifiable list containing the constants of this enum type, in the order declared.
     */
    static List<TimeToLiveStatus> values() {
        return $TYPES;
    }

    /**
     * Returns a {@link TimeToLiveStatus} constant with the specified value.
     *
     * @param value value to create {@code TimeToLiveStatus} from.
     * @throws IllegalArgumentException if value does not match a known value.
     */
    static TimeToLiveStatus from(String value) {
        return switch (value) {
            case "ENABLING" -> ENABLING;
            case "DISABLING" -> DISABLING;
            case "ENABLED" -> ENABLED;
            case "DISABLED" -> DISABLED;
            default -> throw new IllegalArgumentException("Unknown value: " + value);
        };
    }

    final class EnablingType implements TimeToLiveStatus {
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

    final class DisablingType implements TimeToLiveStatus {
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

    final class EnabledType implements TimeToLiveStatus {
        private EnabledType() {}

        @Override
        public String getValue() {
            return "ENABLED";
        }

        @Override
        public String toString() {
            return ToStringSerializer.serialize(this);
        }

    }

    final class DisabledType implements TimeToLiveStatus {
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

    record $Unknown(String value) implements TimeToLiveStatus {
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

        private final class $Hidden implements TimeToLiveStatus {
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
     * Builder for {@link TimeToLiveStatus}.
     */
    final class Builder implements ShapeBuilder<TimeToLiveStatus> {
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
        public TimeToLiveStatus build() {
            return switch (value) {
                case "ENABLING" -> ENABLING;
                case "DISABLING" -> DISABLING;
                case "ENABLED" -> ENABLED;
                case "DISABLED" -> DISABLED;
                default -> new $Unknown(value);
            };
        }

        @Override
        public Builder deserialize(ShapeDeserializer de) {
            return value(de.readString($SCHEMA));
        }
    }
}
