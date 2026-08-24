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
public sealed interface GlobalTableSettingsReplicationMode extends SmithyEnum, SerializableShape {
    GlobalTableSettingsReplicationMode ENABLED = new EnabledType();
    GlobalTableSettingsReplicationMode DISABLED = new DisabledType();
    GlobalTableSettingsReplicationMode ENABLED_WITH_OVERRIDES = new EnabledWithOverridesType();
    List<GlobalTableSettingsReplicationMode> $TYPES = List.of(ENABLED, DISABLED, ENABLED_WITH_OVERRIDES);

    Schema $SCHEMA = Schema.createEnum(ShapeId.from("com.amazonaws.dynamodb#GlobalTableSettingsReplicationMode"),
        Set.of(ENABLED.getValue(), DISABLED.getValue(), ENABLED_WITH_OVERRIDES.getValue()), GlobalTableSettingsReplicationMode.class
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
    static GlobalTableSettingsReplicationMode unknown(String value) {
        return new $Unknown(value);
    }

    /**
     * Returns an unmodifiable list containing the constants of this enum type, in the order declared.
     */
    static List<GlobalTableSettingsReplicationMode> values() {
        return $TYPES;
    }

    /**
     * Returns a {@link GlobalTableSettingsReplicationMode} constant with the specified value.
     *
     * @param value value to create {@code GlobalTableSettingsReplicationMode} from.
     * @throws IllegalArgumentException if value does not match a known value.
     */
    static GlobalTableSettingsReplicationMode from(String value) {
        return switch (value) {
            case "ENABLED" -> ENABLED;
            case "DISABLED" -> DISABLED;
            case "ENABLED_WITH_OVERRIDES" -> ENABLED_WITH_OVERRIDES;
            default -> throw new IllegalArgumentException("Unknown value: " + value);
        };
    }

    final class EnabledType implements GlobalTableSettingsReplicationMode {
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

    final class DisabledType implements GlobalTableSettingsReplicationMode {
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

    final class EnabledWithOverridesType implements GlobalTableSettingsReplicationMode {
        private EnabledWithOverridesType() {}

        @Override
        public String getValue() {
            return "ENABLED_WITH_OVERRIDES";
        }

        @Override
        public String toString() {
            return ToStringSerializer.serialize(this);
        }

    }

    record $Unknown(String value) implements GlobalTableSettingsReplicationMode {
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

        private final class $Hidden implements GlobalTableSettingsReplicationMode {
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
     * Builder for {@link GlobalTableSettingsReplicationMode}.
     */
    final class Builder implements ShapeBuilder<GlobalTableSettingsReplicationMode> {
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
        public GlobalTableSettingsReplicationMode build() {
            return switch (value) {
                case "ENABLED" -> ENABLED;
                case "DISABLED" -> DISABLED;
                case "ENABLED_WITH_OVERRIDES" -> ENABLED_WITH_OVERRIDES;
                default -> new $Unknown(value);
            };
        }

        @Override
        public Builder deserialize(ShapeDeserializer de) {
            return value(de.readString($SCHEMA));
        }
    }
}
