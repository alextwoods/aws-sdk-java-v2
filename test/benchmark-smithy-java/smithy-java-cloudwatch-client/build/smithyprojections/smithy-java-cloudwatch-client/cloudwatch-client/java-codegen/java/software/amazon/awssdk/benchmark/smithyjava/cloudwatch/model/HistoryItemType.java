package software.amazon.awssdk.benchmark.smithyjava.cloudwatch.model;

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
public sealed interface HistoryItemType extends SmithyEnum, SerializableShape {
    HistoryItemType CONFIGURATION_UPDATE = new ConfigurationUpdateType();
    HistoryItemType STATE_UPDATE = new StateUpdateType();
    HistoryItemType ACTION = new ActionType();
    HistoryItemType ALARM_CONTRIBUTOR_STATE_UPDATE = new AlarmContributorStateUpdateType();
    HistoryItemType ALARM_CONTRIBUTOR_ACTION = new AlarmContributorActionType();
    List<HistoryItemType> $TYPES = List.of(CONFIGURATION_UPDATE, STATE_UPDATE, ACTION, ALARM_CONTRIBUTOR_STATE_UPDATE, ALARM_CONTRIBUTOR_ACTION);

    Schema $SCHEMA = Schema.createEnum(ShapeId.from("com.amazonaws.cloudwatch#HistoryItemType"),
        Set.of(CONFIGURATION_UPDATE.getValue(), STATE_UPDATE.getValue(), ACTION.getValue(), ALARM_CONTRIBUTOR_STATE_UPDATE.getValue(), ALARM_CONTRIBUTOR_ACTION.getValue()), HistoryItemType.class
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
    static HistoryItemType unknown(String value) {
        return new $Unknown(value);
    }

    /**
     * Returns an unmodifiable list containing the constants of this enum type, in the order declared.
     */
    static List<HistoryItemType> values() {
        return $TYPES;
    }

    /**
     * Returns a {@link HistoryItemType} constant with the specified value.
     *
     * @param value value to create {@code HistoryItemType} from.
     * @throws IllegalArgumentException if value does not match a known value.
     */
    static HistoryItemType from(String value) {
        return switch (value) {
            case "ConfigurationUpdate" -> CONFIGURATION_UPDATE;
            case "StateUpdate" -> STATE_UPDATE;
            case "Action" -> ACTION;
            case "AlarmContributorStateUpdate" -> ALARM_CONTRIBUTOR_STATE_UPDATE;
            case "AlarmContributorAction" -> ALARM_CONTRIBUTOR_ACTION;
            default -> throw new IllegalArgumentException("Unknown value: " + value);
        };
    }

    final class ConfigurationUpdateType implements HistoryItemType {
        private ConfigurationUpdateType() {}

        @Override
        public String getValue() {
            return "ConfigurationUpdate";
        }

        @Override
        public String toString() {
            return ToStringSerializer.serialize(this);
        }

    }

    final class StateUpdateType implements HistoryItemType {
        private StateUpdateType() {}

        @Override
        public String getValue() {
            return "StateUpdate";
        }

        @Override
        public String toString() {
            return ToStringSerializer.serialize(this);
        }

    }

    final class ActionType implements HistoryItemType {
        private ActionType() {}

        @Override
        public String getValue() {
            return "Action";
        }

        @Override
        public String toString() {
            return ToStringSerializer.serialize(this);
        }

    }

    final class AlarmContributorStateUpdateType implements HistoryItemType {
        private AlarmContributorStateUpdateType() {}

        @Override
        public String getValue() {
            return "AlarmContributorStateUpdate";
        }

        @Override
        public String toString() {
            return ToStringSerializer.serialize(this);
        }

    }

    final class AlarmContributorActionType implements HistoryItemType {
        private AlarmContributorActionType() {}

        @Override
        public String getValue() {
            return "AlarmContributorAction";
        }

        @Override
        public String toString() {
            return ToStringSerializer.serialize(this);
        }

    }

    record $Unknown(String value) implements HistoryItemType {
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

        private final class $Hidden implements HistoryItemType {
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
     * Builder for {@link HistoryItemType}.
     */
    final class Builder implements ShapeBuilder<HistoryItemType> {
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
        public HistoryItemType build() {
            return switch (value) {
                case "ConfigurationUpdate" -> CONFIGURATION_UPDATE;
                case "StateUpdate" -> STATE_UPDATE;
                case "Action" -> ACTION;
                case "AlarmContributorStateUpdate" -> ALARM_CONTRIBUTOR_STATE_UPDATE;
                case "AlarmContributorAction" -> ALARM_CONTRIBUTOR_ACTION;
                default -> new $Unknown(value);
            };
        }

        @Override
        public Builder deserialize(ShapeDeserializer de) {
            return value(de.readString($SCHEMA));
        }
    }
}
