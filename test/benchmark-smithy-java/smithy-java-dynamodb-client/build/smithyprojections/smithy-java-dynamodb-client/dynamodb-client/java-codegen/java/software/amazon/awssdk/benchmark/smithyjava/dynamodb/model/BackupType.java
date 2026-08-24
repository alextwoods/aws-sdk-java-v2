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
public sealed interface BackupType extends SmithyEnum, SerializableShape {
    BackupType USER = new UserType();
    BackupType SYSTEM = new SystemType();
    BackupType AWS_BACKUP = new AwsBackupType();
    List<BackupType> $TYPES = List.of(USER, SYSTEM, AWS_BACKUP);

    Schema $SCHEMA = Schema.createEnum(ShapeId.from("com.amazonaws.dynamodb#BackupType"),
        Set.of(USER.getValue(), SYSTEM.getValue(), AWS_BACKUP.getValue()), BackupType.class
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
    static BackupType unknown(String value) {
        return new $Unknown(value);
    }

    /**
     * Returns an unmodifiable list containing the constants of this enum type, in the order declared.
     */
    static List<BackupType> values() {
        return $TYPES;
    }

    /**
     * Returns a {@link BackupType} constant with the specified value.
     *
     * @param value value to create {@code BackupType} from.
     * @throws IllegalArgumentException if value does not match a known value.
     */
    static BackupType from(String value) {
        return switch (value) {
            case "USER" -> USER;
            case "SYSTEM" -> SYSTEM;
            case "AWS_BACKUP" -> AWS_BACKUP;
            default -> throw new IllegalArgumentException("Unknown value: " + value);
        };
    }

    final class UserType implements BackupType {
        private UserType() {}

        @Override
        public String getValue() {
            return "USER";
        }

        @Override
        public String toString() {
            return ToStringSerializer.serialize(this);
        }

    }

    final class SystemType implements BackupType {
        private SystemType() {}

        @Override
        public String getValue() {
            return "SYSTEM";
        }

        @Override
        public String toString() {
            return ToStringSerializer.serialize(this);
        }

    }

    final class AwsBackupType implements BackupType {
        private AwsBackupType() {}

        @Override
        public String getValue() {
            return "AWS_BACKUP";
        }

        @Override
        public String toString() {
            return ToStringSerializer.serialize(this);
        }

    }

    record $Unknown(String value) implements BackupType {
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

        private final class $Hidden implements BackupType {
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
     * Builder for {@link BackupType}.
     */
    final class Builder implements ShapeBuilder<BackupType> {
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
        public BackupType build() {
            return switch (value) {
                case "USER" -> USER;
                case "SYSTEM" -> SYSTEM;
                case "AWS_BACKUP" -> AWS_BACKUP;
                default -> new $Unknown(value);
            };
        }

        @Override
        public Builder deserialize(ShapeDeserializer de) {
            return value(de.readString($SCHEMA));
        }
    }
}
