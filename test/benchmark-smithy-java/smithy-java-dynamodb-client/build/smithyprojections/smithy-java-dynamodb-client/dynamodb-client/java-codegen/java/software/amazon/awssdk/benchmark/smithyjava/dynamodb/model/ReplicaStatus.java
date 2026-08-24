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
public sealed interface ReplicaStatus extends SmithyEnum, SerializableShape {
    ReplicaStatus CREATING = new CreatingType();
    ReplicaStatus CREATION_FAILED = new CreationFailedType();
    ReplicaStatus UPDATING = new UpdatingType();
    ReplicaStatus DELETING = new DeletingType();
    ReplicaStatus ACTIVE = new ActiveType();
    ReplicaStatus REGION_DISABLED = new RegionDisabledType();
    ReplicaStatus INACCESSIBLE_ENCRYPTION_CREDENTIALS = new InaccessibleEncryptionCredentialsType();
    ReplicaStatus ARCHIVING = new ArchivingType();
    ReplicaStatus ARCHIVED = new ArchivedType();
    ReplicaStatus REPLICATION_NOT_AUTHORIZED = new ReplicationNotAuthorizedType();
    List<ReplicaStatus> $TYPES = List.of(CREATING, CREATION_FAILED, UPDATING, DELETING, ACTIVE, REGION_DISABLED, INACCESSIBLE_ENCRYPTION_CREDENTIALS, ARCHIVING, ARCHIVED, REPLICATION_NOT_AUTHORIZED);

    Schema $SCHEMA = Schema.createEnum(ShapeId.from("com.amazonaws.dynamodb#ReplicaStatus"),
        Set.of(CREATING.getValue(), CREATION_FAILED.getValue(), UPDATING.getValue(), DELETING.getValue(), ACTIVE.getValue(), REGION_DISABLED.getValue(), INACCESSIBLE_ENCRYPTION_CREDENTIALS.getValue(), ARCHIVING.getValue(), ARCHIVED.getValue(), REPLICATION_NOT_AUTHORIZED.getValue()), ReplicaStatus.class
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
    static ReplicaStatus unknown(String value) {
        return new $Unknown(value);
    }

    /**
     * Returns an unmodifiable list containing the constants of this enum type, in the order declared.
     */
    static List<ReplicaStatus> values() {
        return $TYPES;
    }

    /**
     * Returns a {@link ReplicaStatus} constant with the specified value.
     *
     * @param value value to create {@code ReplicaStatus} from.
     * @throws IllegalArgumentException if value does not match a known value.
     */
    static ReplicaStatus from(String value) {
        return switch (value) {
            case "CREATING" -> CREATING;
            case "CREATION_FAILED" -> CREATION_FAILED;
            case "UPDATING" -> UPDATING;
            case "DELETING" -> DELETING;
            case "ACTIVE" -> ACTIVE;
            case "REGION_DISABLED" -> REGION_DISABLED;
            case "INACCESSIBLE_ENCRYPTION_CREDENTIALS" -> INACCESSIBLE_ENCRYPTION_CREDENTIALS;
            case "ARCHIVING" -> ARCHIVING;
            case "ARCHIVED" -> ARCHIVED;
            case "REPLICATION_NOT_AUTHORIZED" -> REPLICATION_NOT_AUTHORIZED;
            default -> throw new IllegalArgumentException("Unknown value: " + value);
        };
    }

    final class CreatingType implements ReplicaStatus {
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

    final class CreationFailedType implements ReplicaStatus {
        private CreationFailedType() {}

        @Override
        public String getValue() {
            return "CREATION_FAILED";
        }

        @Override
        public String toString() {
            return ToStringSerializer.serialize(this);
        }

    }

    final class UpdatingType implements ReplicaStatus {
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

    final class DeletingType implements ReplicaStatus {
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

    final class ActiveType implements ReplicaStatus {
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

    final class RegionDisabledType implements ReplicaStatus {
        private RegionDisabledType() {}

        @Override
        public String getValue() {
            return "REGION_DISABLED";
        }

        @Override
        public String toString() {
            return ToStringSerializer.serialize(this);
        }

    }

    final class InaccessibleEncryptionCredentialsType implements ReplicaStatus {
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

    final class ArchivingType implements ReplicaStatus {
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

    final class ArchivedType implements ReplicaStatus {
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

    final class ReplicationNotAuthorizedType implements ReplicaStatus {
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

    record $Unknown(String value) implements ReplicaStatus {
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

        private final class $Hidden implements ReplicaStatus {
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
     * Builder for {@link ReplicaStatus}.
     */
    final class Builder implements ShapeBuilder<ReplicaStatus> {
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
        public ReplicaStatus build() {
            return switch (value) {
                case "CREATING" -> CREATING;
                case "CREATION_FAILED" -> CREATION_FAILED;
                case "UPDATING" -> UPDATING;
                case "DELETING" -> DELETING;
                case "ACTIVE" -> ACTIVE;
                case "REGION_DISABLED" -> REGION_DISABLED;
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
