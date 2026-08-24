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
public sealed interface TableStatus extends SmithyEnum, SerializableShape {
    TableStatus CREATING = new CreatingType();
    TableStatus UPDATING = new UpdatingType();
    TableStatus DELETING = new DeletingType();
    TableStatus ACTIVE = new ActiveType();
    TableStatus INACCESSIBLE_ENCRYPTION_CREDENTIALS = new InaccessibleEncryptionCredentialsType();
    TableStatus ARCHIVING = new ArchivingType();
    TableStatus ARCHIVED = new ArchivedType();
    TableStatus REPLICATION_NOT_AUTHORIZED = new ReplicationNotAuthorizedType();
    List<TableStatus> $TYPES = List.of(CREATING, UPDATING, DELETING, ACTIVE, INACCESSIBLE_ENCRYPTION_CREDENTIALS, ARCHIVING, ARCHIVED, REPLICATION_NOT_AUTHORIZED);

    Schema $SCHEMA = Schema.createEnum(ShapeId.from("com.amazonaws.dynamodb#TableStatus"),
        Set.of(CREATING.getValue(), UPDATING.getValue(), DELETING.getValue(), ACTIVE.getValue(), INACCESSIBLE_ENCRYPTION_CREDENTIALS.getValue(), ARCHIVING.getValue(), ARCHIVED.getValue(), REPLICATION_NOT_AUTHORIZED.getValue()), TableStatus.class
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
    static TableStatus unknown(String value) {
        return new $Unknown(value);
    }

    /**
     * Returns an unmodifiable list containing the constants of this enum type, in the order declared.
     */
    static List<TableStatus> values() {
        return $TYPES;
    }

    /**
     * Returns a {@link TableStatus} constant with the specified value.
     *
     * @param value value to create {@code TableStatus} from.
     * @throws IllegalArgumentException if value does not match a known value.
     */
    static TableStatus from(String value) {
        return switch (value) {
            case "CREATING" -> CREATING;
            case "UPDATING" -> UPDATING;
            case "DELETING" -> DELETING;
            case "ACTIVE" -> ACTIVE;
            case "INACCESSIBLE_ENCRYPTION_CREDENTIALS" -> INACCESSIBLE_ENCRYPTION_CREDENTIALS;
            case "ARCHIVING" -> ARCHIVING;
            case "ARCHIVED" -> ARCHIVED;
            case "REPLICATION_NOT_AUTHORIZED" -> REPLICATION_NOT_AUTHORIZED;
            default -> throw new IllegalArgumentException("Unknown value: " + value);
        };
    }

    final class CreatingType implements TableStatus {
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

    final class UpdatingType implements TableStatus {
        private UpdatingType() {}

        @Override
        public String getValue() {
            return "UPDATING";
        }

        @Override
        public String toString() {
            return ToStringSerializer.serialize(this);
        }

    }

    final class DeletingType implements TableStatus {
        private DeletingType() {}

        @Override
        public String getValue() {
            return "DELETING";
        }

        @Override
        public String toString() {
            return ToStringSerializer.serialize(this);
        }

    }

    final class ActiveType implements TableStatus {
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

    final class InaccessibleEncryptionCredentialsType implements TableStatus {
        private InaccessibleEncryptionCredentialsType() {}

        @Override
        public String getValue() {
            return "INACCESSIBLE_ENCRYPTION_CREDENTIALS";
        }

        @Override
        public String toString() {
            return ToStringSerializer.serialize(this);
        }

    }

    final class ArchivingType implements TableStatus {
        private ArchivingType() {}

        @Override
        public String getValue() {
            return "ARCHIVING";
        }

        @Override
        public String toString() {
            return ToStringSerializer.serialize(this);
        }

    }

    final class ArchivedType implements TableStatus {
        private ArchivedType() {}

        @Override
        public String getValue() {
            return "ARCHIVED";
        }

        @Override
        public String toString() {
            return ToStringSerializer.serialize(this);
        }

    }

    final class ReplicationNotAuthorizedType implements TableStatus {
        private ReplicationNotAuthorizedType() {}

        @Override
        public String getValue() {
            return "REPLICATION_NOT_AUTHORIZED";
        }

        @Override
        public String toString() {
            return ToStringSerializer.serialize(this);
        }

    }

    record $Unknown(String value) implements TableStatus {
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

        private final class $Hidden implements TableStatus {
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
     * Builder for {@link TableStatus}.
     */
    final class Builder implements ShapeBuilder<TableStatus> {
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
        public TableStatus build() {
            return switch (value) {
                case "CREATING" -> CREATING;
                case "UPDATING" -> UPDATING;
                case "DELETING" -> DELETING;
                case "ACTIVE" -> ACTIVE;
                case "INACCESSIBLE_ENCRYPTION_CREDENTIALS" -> INACCESSIBLE_ENCRYPTION_CREDENTIALS;
                case "ARCHIVING" -> ARCHIVING;
                case "ARCHIVED" -> ARCHIVED;
                case "REPLICATION_NOT_AUTHORIZED" -> REPLICATION_NOT_AUTHORIZED;
                default -> new $Unknown(value);
            };
        }

        @Override
        public Builder deserialize(ShapeDeserializer de) {
            return value(de.readString($SCHEMA));
        }
    }
}
