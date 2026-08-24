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
public sealed interface BackupStatus extends SmithyEnum, SerializableShape {
    BackupStatus CREATING = new CreatingType();
    BackupStatus DELETED = new DeletedType();
    BackupStatus AVAILABLE = new AvailableType();
    List<BackupStatus> $TYPES = List.of(CREATING, DELETED, AVAILABLE);

    Schema $SCHEMA = Schema.createEnum(ShapeId.from("com.amazonaws.dynamodb#BackupStatus"),
        Set.of(CREATING.getValue(), DELETED.getValue(), AVAILABLE.getValue()), BackupStatus.class
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
    static BackupStatus unknown(String value) {
        return new $Unknown(value);
    }

    /**
     * Returns an unmodifiable list containing the constants of this enum type, in the order declared.
     */
    static List<BackupStatus> values() {
        return $TYPES;
    }

    /**
     * Returns a {@link BackupStatus} constant with the specified value.
     *
     * @param value value to create {@code BackupStatus} from.
     * @throws IllegalArgumentException if value does not match a known value.
     */
    static BackupStatus from(String value) {
        return switch (value) {
            case "CREATING" -> CREATING;
            case "DELETED" -> DELETED;
            case "AVAILABLE" -> AVAILABLE;
            default -> throw new IllegalArgumentException("Unknown value: " + value);
        };
    }

    final class CreatingType implements BackupStatus {
        private CreatingType() {}

        @Override
        public String getValue() {
            return "CREATING";
        }

        @Override
        public String toString() {
            return ToStringSerializer.serialize(this);
        }

    }

    final class DeletedType implements BackupStatus {
        private DeletedType() {}

        @Override
        public String getValue() {
            return "DELETED";
        }

        @Override
        public String toString() {
            return ToStringSerializer.serialize(this);
        }

    }

    final class AvailableType implements BackupStatus {
        private AvailableType() {}

        @Override
        public String getValue() {
            return "AVAILABLE";
        }

        @Override
        public String toString() {
            return ToStringSerializer.serialize(this);
        }

    }

    record $Unknown(String value) implements BackupStatus {
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

        private final class $Hidden implements BackupStatus {
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
     * Builder for {@link BackupStatus}.
     */
    final class Builder implements ShapeBuilder<BackupStatus> {
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
        public BackupStatus build() {
            return switch (value) {
                case "CREATING" -> CREATING;
                case "DELETED" -> DELETED;
                case "AVAILABLE" -> AVAILABLE;
                default -> new $Unknown(value);
            };
        }

        @Override
        public Builder deserialize(ShapeDeserializer de) {
            return value(de.readString($SCHEMA));
        }
    }
}
