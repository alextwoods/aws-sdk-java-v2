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
public sealed interface ExpirationStatus extends SmithyEnum, SerializableShape {
    ExpirationStatus ENABLED = new EnabledType();
    ExpirationStatus DISABLED = new DisabledType();
    List<ExpirationStatus> $TYPES = List.of(ENABLED, DISABLED);

    Schema $SCHEMA = Schema.createEnum(ShapeId.from("com.amazonaws.s3#ExpirationStatus"),
        Set.of(ENABLED.getValue(), DISABLED.getValue()), ExpirationStatus.class
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
    static ExpirationStatus unknown(String value) {
        return new $Unknown(value);
    }

    /**
     * Returns an unmodifiable list containing the constants of this enum type, in the order declared.
     */
    static List<ExpirationStatus> values() {
        return $TYPES;
    }

    /**
     * Returns a {@link ExpirationStatus} constant with the specified value.
     *
     * @param value value to create {@code ExpirationStatus} from.
     * @throws IllegalArgumentException if value does not match a known value.
     */
    static ExpirationStatus from(String value) {
        return switch (value) {
            case "Enabled" -> ENABLED;
            case "Disabled" -> DISABLED;
            default -> throw new IllegalArgumentException("Unknown value: " + value);
        };
    }

    final class EnabledType implements ExpirationStatus {
        private EnabledType() {}

        @Override
        public String getValue() {
            return "Enabled";
        }

        @Override
        public String toString() {
            return ToStringSerializer.serialize(this);
        }

    }

    final class DisabledType implements ExpirationStatus {
        private DisabledType() {}

        @Override
        public String getValue() {
            return "Disabled";
        }

        @Override
        public String toString() {
            return ToStringSerializer.serialize(this);
        }

    }

    record $Unknown(String value) implements ExpirationStatus {
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

        private final class $Hidden implements ExpirationStatus {
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
     * Builder for {@link ExpirationStatus}.
     */
    final class Builder implements ShapeBuilder<ExpirationStatus> {
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
        public ExpirationStatus build() {
            return switch (value) {
                case "Enabled" -> ENABLED;
                case "Disabled" -> DISABLED;
                default -> new $Unknown(value);
            };
        }

        @Override
        public Builder deserialize(ShapeDeserializer de) {
            return value(de.readString($SCHEMA));
        }
    }
}
