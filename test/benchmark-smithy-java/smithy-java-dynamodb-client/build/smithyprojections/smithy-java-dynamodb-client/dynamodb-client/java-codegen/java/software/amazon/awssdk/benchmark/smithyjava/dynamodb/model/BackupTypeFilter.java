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
public sealed interface BackupTypeFilter extends SmithyEnum, SerializableShape {
    BackupTypeFilter USER = new UserType();
    BackupTypeFilter SYSTEM = new SystemType();
    BackupTypeFilter AWS_BACKUP = new AwsBackupType();
    BackupTypeFilter ALL = new AllType();
    List<BackupTypeFilter> $TYPES = List.of(USER, SYSTEM, AWS_BACKUP, ALL);

    Schema $SCHEMA = Schema.createEnum(ShapeId.from("com.amazonaws.dynamodb#BackupTypeFilter"),
        Set.of(USER.getValue(), SYSTEM.getValue(), AWS_BACKUP.getValue(), ALL.getValue()), BackupTypeFilter.class
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
    static BackupTypeFilter unknown(String value) {
        return new $Unknown(value);
    }

    /**
     * Returns an unmodifiable list containing the constants of this enum type, in the order declared.
     */
    static List<BackupTypeFilter> values() {
        return $TYPES;
    }

    /**
     * Returns a {@link BackupTypeFilter} constant with the specified value.
     *
     * @param value value to create {@code BackupTypeFilter} from.
     * @throws IllegalArgumentException if value does not match a known value.
     */
    static BackupTypeFilter from(String value) {
        return switch (value) {
            case "USER" -> USER;
            case "SYSTEM" -> SYSTEM;
            case "AWS_BACKUP" -> AWS_BACKUP;
            case "ALL" -> ALL;
            default -> throw new IllegalArgumentException("Unknown value: " + value);
        };
    }

    final class UserType implements BackupTypeFilter {
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

    final class SystemType implements BackupTypeFilter {
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

    final class AwsBackupType implements BackupTypeFilter {
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

    final class AllType implements BackupTypeFilter {
        private AllType() {}

        @Override
        public String getValue() {
            return "ALL";
        }

        @Override
        public String toString() {
            return ToStringSerializer.serialize(this);
        }

    }

    record $Unknown(String value) implements BackupTypeFilter {
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

        private final class $Hidden implements BackupTypeFilter {
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
     * Builder for {@link BackupTypeFilter}.
     */
    final class Builder implements ShapeBuilder<BackupTypeFilter> {
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
        public BackupTypeFilter build() {
            return switch (value) {
                case "USER" -> USER;
                case "SYSTEM" -> SYSTEM;
                case "AWS_BACKUP" -> AWS_BACKUP;
                case "ALL" -> ALL;
                default -> new $Unknown(value);
            };
        }

        @Override
        public Builder deserialize(ShapeDeserializer de) {
            return value(de.readString($SCHEMA));
        }
    }
}
