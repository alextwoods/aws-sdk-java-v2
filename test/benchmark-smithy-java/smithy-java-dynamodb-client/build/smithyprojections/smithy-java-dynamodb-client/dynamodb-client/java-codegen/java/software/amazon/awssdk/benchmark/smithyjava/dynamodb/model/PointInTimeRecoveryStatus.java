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
public sealed interface PointInTimeRecoveryStatus extends SmithyEnum, SerializableShape {
    PointInTimeRecoveryStatus ENABLED = new EnabledType();
    PointInTimeRecoveryStatus DISABLED = new DisabledType();
    List<PointInTimeRecoveryStatus> $TYPES = List.of(ENABLED, DISABLED);

    Schema $SCHEMA = Schema.createEnum(ShapeId.from("com.amazonaws.dynamodb#PointInTimeRecoveryStatus"),
        Set.of(ENABLED.getValue(), DISABLED.getValue()), PointInTimeRecoveryStatus.class
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
    static PointInTimeRecoveryStatus unknown(String value) {
        return new $Unknown(value);
    }

    /**
     * Returns an unmodifiable list containing the constants of this enum type, in the order declared.
     */
    static List<PointInTimeRecoveryStatus> values() {
        return $TYPES;
    }

    /**
     * Returns a {@link PointInTimeRecoveryStatus} constant with the specified value.
     *
     * @param value value to create {@code PointInTimeRecoveryStatus} from.
     * @throws IllegalArgumentException if value does not match a known value.
     */
    static PointInTimeRecoveryStatus from(String value) {
        return switch (value) {
            case "ENABLED" -> ENABLED;
            case "DISABLED" -> DISABLED;
            default -> throw new IllegalArgumentException("Unknown value: " + value);
        };
    }

    final class EnabledType implements PointInTimeRecoveryStatus {
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

    final class DisabledType implements PointInTimeRecoveryStatus {
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

    record $Unknown(String value) implements PointInTimeRecoveryStatus {
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

        private final class $Hidden implements PointInTimeRecoveryStatus {
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
     * Builder for {@link PointInTimeRecoveryStatus}.
     */
    final class Builder implements ShapeBuilder<PointInTimeRecoveryStatus> {
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
        public PointInTimeRecoveryStatus build() {
            return switch (value) {
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
