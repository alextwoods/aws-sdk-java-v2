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
public sealed interface ExistingObjectReplicationStatus extends SmithyEnum, SerializableShape {
    ExistingObjectReplicationStatus ENABLED = new EnabledType();
    ExistingObjectReplicationStatus DISABLED = new DisabledType();
    List<ExistingObjectReplicationStatus> $TYPES = List.of(ENABLED, DISABLED);

    Schema $SCHEMA = Schema.createEnum(ShapeId.from("com.amazonaws.s3#ExistingObjectReplicationStatus"),
        Set.of(ENABLED.getValue(), DISABLED.getValue()), ExistingObjectReplicationStatus.class
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
    static ExistingObjectReplicationStatus unknown(String value) {
        return new $Unknown(value);
    }

    /**
     * Returns an unmodifiable list containing the constants of this enum type, in the order declared.
     */
    static List<ExistingObjectReplicationStatus> values() {
        return $TYPES;
    }

    /**
     * Returns a {@link ExistingObjectReplicationStatus} constant with the specified value.
     *
     * @param value value to create {@code ExistingObjectReplicationStatus} from.
     * @throws IllegalArgumentException if value does not match a known value.
     */
    static ExistingObjectReplicationStatus from(String value) {
        return switch (value) {
            case "Enabled" -> ENABLED;
            case "Disabled" -> DISABLED;
            default -> throw new IllegalArgumentException("Unknown value: " + value);
        };
    }

    final class EnabledType implements ExistingObjectReplicationStatus {
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

    final class DisabledType implements ExistingObjectReplicationStatus {
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

    record $Unknown(String value) implements ExistingObjectReplicationStatus {
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

        private final class $Hidden implements ExistingObjectReplicationStatus {
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
     * Builder for {@link ExistingObjectReplicationStatus}.
     */
    final class Builder implements ShapeBuilder<ExistingObjectReplicationStatus> {
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
        public ExistingObjectReplicationStatus build() {
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
