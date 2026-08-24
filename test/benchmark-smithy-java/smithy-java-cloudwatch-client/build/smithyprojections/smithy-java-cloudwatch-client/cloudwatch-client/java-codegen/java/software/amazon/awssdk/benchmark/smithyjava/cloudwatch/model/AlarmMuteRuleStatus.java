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
public sealed interface AlarmMuteRuleStatus extends SmithyEnum, SerializableShape {
    AlarmMuteRuleStatus SCHEDULED = new ScheduledType();
    AlarmMuteRuleStatus ACTIVE = new ActiveType();
    AlarmMuteRuleStatus EXPIRED = new ExpiredType();
    List<AlarmMuteRuleStatus> $TYPES = List.of(SCHEDULED, ACTIVE, EXPIRED);

    Schema $SCHEMA = Schema.createEnum(ShapeId.from("com.amazonaws.cloudwatch#AlarmMuteRuleStatus"),
        Set.of(SCHEDULED.getValue(), ACTIVE.getValue(), EXPIRED.getValue()), AlarmMuteRuleStatus.class
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
    static AlarmMuteRuleStatus unknown(String value) {
        return new $Unknown(value);
    }

    /**
     * Returns an unmodifiable list containing the constants of this enum type, in the order declared.
     */
    static List<AlarmMuteRuleStatus> values() {
        return $TYPES;
    }

    /**
     * Returns a {@link AlarmMuteRuleStatus} constant with the specified value.
     *
     * @param value value to create {@code AlarmMuteRuleStatus} from.
     * @throws IllegalArgumentException if value does not match a known value.
     */
    static AlarmMuteRuleStatus from(String value) {
        return switch (value) {
            case "SCHEDULED" -> SCHEDULED;
            case "ACTIVE" -> ACTIVE;
            case "EXPIRED" -> EXPIRED;
            default -> throw new IllegalArgumentException("Unknown value: " + value);
        };
    }

    final class ScheduledType implements AlarmMuteRuleStatus {
        private ScheduledType() {}

        @Override
        public String getValue() {
            return "SCHEDULED";
        }

        @Override
        public String toString() {
            return ToStringSerializer.serialize(this);
        }

    }

    final class ActiveType implements AlarmMuteRuleStatus {
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

    final class ExpiredType implements AlarmMuteRuleStatus {
        private ExpiredType() {}

        @Override
        public String getValue() {
            return "EXPIRED";
        }

        @Override
        public String toString() {
            return ToStringSerializer.serialize(this);
        }

    }

    record $Unknown(String value) implements AlarmMuteRuleStatus {
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

        private final class $Hidden implements AlarmMuteRuleStatus {
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
     * Builder for {@link AlarmMuteRuleStatus}.
     */
    final class Builder implements ShapeBuilder<AlarmMuteRuleStatus> {
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
        public AlarmMuteRuleStatus build() {
            return switch (value) {
                case "SCHEDULED" -> SCHEDULED;
                case "ACTIVE" -> ACTIVE;
                case "EXPIRED" -> EXPIRED;
                default -> new $Unknown(value);
            };
        }

        @Override
        public Builder deserialize(ShapeDeserializer de) {
            return value(de.readString($SCHEMA));
        }
    }
}
