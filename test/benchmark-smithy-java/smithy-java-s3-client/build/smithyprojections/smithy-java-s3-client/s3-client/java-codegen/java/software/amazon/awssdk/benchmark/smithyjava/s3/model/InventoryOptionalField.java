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
public sealed interface InventoryOptionalField extends SmithyEnum, SerializableShape {
    InventoryOptionalField SIZE = new SizeType();
    InventoryOptionalField LAST_MODIFIED_DATE = new LastModifiedDateType();
    InventoryOptionalField STORAGE_CLASS = new StorageClassType();
    InventoryOptionalField E_TAG = new ETagType();
    InventoryOptionalField IS_MULTIPART_UPLOADED = new IsMultipartUploadedType();
    InventoryOptionalField REPLICATION_STATUS = new ReplicationStatusType();
    InventoryOptionalField ENCRYPTION_STATUS = new EncryptionStatusType();
    InventoryOptionalField OBJECT_LOCK_RETAIN_UNTIL_DATE = new ObjectLockRetainUntilDateType();
    InventoryOptionalField OBJECT_LOCK_MODE = new ObjectLockModeType();
    InventoryOptionalField OBJECT_LOCK_LEGAL_HOLD_STATUS = new ObjectLockLegalHoldStatusType();
    InventoryOptionalField INTELLIGENT_TIERING_ACCESS_TIER = new IntelligentTieringAccessTierType();
    InventoryOptionalField BUCKET_KEY_STATUS = new BucketKeyStatusType();
    InventoryOptionalField CHECKSUM_ALGORITHM = new ChecksumAlgorithmType();
    InventoryOptionalField OBJECT_ACCESS_CONTROL_LIST = new ObjectAccessControlListType();
    InventoryOptionalField OBJECT_OWNER = new ObjectOwnerType();
    InventoryOptionalField LIFECYCLE_EXPIRATION_DATE = new LifecycleExpirationDateType();
    List<InventoryOptionalField> $TYPES = List.of(SIZE, LAST_MODIFIED_DATE, STORAGE_CLASS, E_TAG, IS_MULTIPART_UPLOADED, REPLICATION_STATUS, ENCRYPTION_STATUS, OBJECT_LOCK_RETAIN_UNTIL_DATE, OBJECT_LOCK_MODE, OBJECT_LOCK_LEGAL_HOLD_STATUS, INTELLIGENT_TIERING_ACCESS_TIER, BUCKET_KEY_STATUS, CHECKSUM_ALGORITHM, OBJECT_ACCESS_CONTROL_LIST, OBJECT_OWNER, LIFECYCLE_EXPIRATION_DATE);

    Schema $SCHEMA = Schema.createEnum(ShapeId.from("com.amazonaws.s3#InventoryOptionalField"),
        Set.of(SIZE.getValue(), LAST_MODIFIED_DATE.getValue(), STORAGE_CLASS.getValue(), E_TAG.getValue(), IS_MULTIPART_UPLOADED.getValue(), REPLICATION_STATUS.getValue(), ENCRYPTION_STATUS.getValue(), OBJECT_LOCK_RETAIN_UNTIL_DATE.getValue(), OBJECT_LOCK_MODE.getValue(), OBJECT_LOCK_LEGAL_HOLD_STATUS.getValue(), INTELLIGENT_TIERING_ACCESS_TIER.getValue(), BUCKET_KEY_STATUS.getValue(), CHECKSUM_ALGORITHM.getValue(), OBJECT_ACCESS_CONTROL_LIST.getValue(), OBJECT_OWNER.getValue(), LIFECYCLE_EXPIRATION_DATE.getValue()), InventoryOptionalField.class
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
    static InventoryOptionalField unknown(String value) {
        return new $Unknown(value);
    }

    /**
     * Returns an unmodifiable list containing the constants of this enum type, in the order declared.
     */
    static List<InventoryOptionalField> values() {
        return $TYPES;
    }

    /**
     * Returns a {@link InventoryOptionalField} constant with the specified value.
     *
     * @param value value to create {@code InventoryOptionalField} from.
     * @throws IllegalArgumentException if value does not match a known value.
     */
    static InventoryOptionalField from(String value) {
        return switch (value) {
            case "Size" -> SIZE;
            case "LastModifiedDate" -> LAST_MODIFIED_DATE;
            case "StorageClass" -> STORAGE_CLASS;
            case "ETag" -> E_TAG;
            case "IsMultipartUploaded" -> IS_MULTIPART_UPLOADED;
            case "ReplicationStatus" -> REPLICATION_STATUS;
            case "EncryptionStatus" -> ENCRYPTION_STATUS;
            case "ObjectLockRetainUntilDate" -> OBJECT_LOCK_RETAIN_UNTIL_DATE;
            case "ObjectLockMode" -> OBJECT_LOCK_MODE;
            case "ObjectLockLegalHoldStatus" -> OBJECT_LOCK_LEGAL_HOLD_STATUS;
            case "IntelligentTieringAccessTier" -> INTELLIGENT_TIERING_ACCESS_TIER;
            case "BucketKeyStatus" -> BUCKET_KEY_STATUS;
            case "ChecksumAlgorithm" -> CHECKSUM_ALGORITHM;
            case "ObjectAccessControlList" -> OBJECT_ACCESS_CONTROL_LIST;
            case "ObjectOwner" -> OBJECT_OWNER;
            case "LifecycleExpirationDate" -> LIFECYCLE_EXPIRATION_DATE;
            default -> throw new IllegalArgumentException("Unknown value: " + value);
        };
    }

    final class SizeType implements InventoryOptionalField {
        private SizeType() {}

        @Override
        public String getValue() {
            return "Size";
        }

        @Override
        public String toString() {
            return ToStringSerializer.serialize(this);
        }

    }

    final class LastModifiedDateType implements InventoryOptionalField {
        private LastModifiedDateType() {}

        @Override
        public String getValue() {
            return "LastModifiedDate";
        }

        @Override
        public String toString() {
            return ToStringSerializer.serialize(this);
        }

    }

    final class StorageClassType implements InventoryOptionalField {
        private StorageClassType() {}

        @Override
        public String getValue() {
            return "StorageClass";
        }

        @Override
        public String toString() {
            return ToStringSerializer.serialize(this);
        }

    }

    final class ETagType implements InventoryOptionalField {
        private ETagType() {}

        @Override
        public String getValue() {
            return "ETag";
        }

        @Override
        public String toString() {
            return ToStringSerializer.serialize(this);
        }

    }

    final class IsMultipartUploadedType implements InventoryOptionalField {
        private IsMultipartUploadedType() {}

        @Override
        public String getValue() {
            return "IsMultipartUploaded";
        }

        @Override
        public String toString() {
            return ToStringSerializer.serialize(this);
        }

    }

    final class ReplicationStatusType implements InventoryOptionalField {
        private ReplicationStatusType() {}

        @Override
        public String getValue() {
            return "ReplicationStatus";
        }

        @Override
        public String toString() {
            return ToStringSerializer.serialize(this);
        }

    }

    final class EncryptionStatusType implements InventoryOptionalField {
        private EncryptionStatusType() {}

        @Override
        public String getValue() {
            return "EncryptionStatus";
        }

        @Override
        public String toString() {
            return ToStringSerializer.serialize(this);
        }

    }

    final class ObjectLockRetainUntilDateType implements InventoryOptionalField {
        private ObjectLockRetainUntilDateType() {}

        @Override
        public String getValue() {
            return "ObjectLockRetainUntilDate";
        }

        @Override
        public String toString() {
            return ToStringSerializer.serialize(this);
        }

    }

    final class ObjectLockModeType implements InventoryOptionalField {
        private ObjectLockModeType() {}

        @Override
        public String getValue() {
            return "ObjectLockMode";
        }

        @Override
        public String toString() {
            return ToStringSerializer.serialize(this);
        }

    }

    final class ObjectLockLegalHoldStatusType implements InventoryOptionalField {
        private ObjectLockLegalHoldStatusType() {}

        @Override
        public String getValue() {
            return "ObjectLockLegalHoldStatus";
        }

        @Override
        public String toString() {
            return ToStringSerializer.serialize(this);
        }

    }

    final class IntelligentTieringAccessTierType implements InventoryOptionalField {
        private IntelligentTieringAccessTierType() {}

        @Override
        public String getValue() {
            return "IntelligentTieringAccessTier";
        }

        @Override
        public String toString() {
            return ToStringSerializer.serialize(this);
        }

    }

    final class BucketKeyStatusType implements InventoryOptionalField {
        private BucketKeyStatusType() {}

        @Override
        public String getValue() {
            return "BucketKeyStatus";
        }

        @Override
        public String toString() {
            return ToStringSerializer.serialize(this);
        }

    }

    final class ChecksumAlgorithmType implements InventoryOptionalField {
        private ChecksumAlgorithmType() {}

        @Override
        public String getValue() {
            return "ChecksumAlgorithm";
        }

        @Override
        public String toString() {
            return ToStringSerializer.serialize(this);
        }

    }

    final class ObjectAccessControlListType implements InventoryOptionalField {
        private ObjectAccessControlListType() {}

        @Override
        public String getValue() {
            return "ObjectAccessControlList";
        }

        @Override
        public String toString() {
            return ToStringSerializer.serialize(this);
        }

    }

    final class ObjectOwnerType implements InventoryOptionalField {
        private ObjectOwnerType() {}

        @Override
        public String getValue() {
            return "ObjectOwner";
        }

        @Override
        public String toString() {
            return ToStringSerializer.serialize(this);
        }

    }

    final class LifecycleExpirationDateType implements InventoryOptionalField {
        private LifecycleExpirationDateType() {}

        @Override
        public String getValue() {
            return "LifecycleExpirationDate";
        }

        @Override
        public String toString() {
            return ToStringSerializer.serialize(this);
        }

    }

    record $Unknown(String value) implements InventoryOptionalField {
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

        private final class $Hidden implements InventoryOptionalField {
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
     * Builder for {@link InventoryOptionalField}.
     */
    final class Builder implements ShapeBuilder<InventoryOptionalField> {
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
        public InventoryOptionalField build() {
            return switch (value) {
                case "Size" -> SIZE;
                case "LastModifiedDate" -> LAST_MODIFIED_DATE;
                case "StorageClass" -> STORAGE_CLASS;
                case "ETag" -> E_TAG;
                case "IsMultipartUploaded" -> IS_MULTIPART_UPLOADED;
                case "ReplicationStatus" -> REPLICATION_STATUS;
                case "EncryptionStatus" -> ENCRYPTION_STATUS;
                case "ObjectLockRetainUntilDate" -> OBJECT_LOCK_RETAIN_UNTIL_DATE;
                case "ObjectLockMode" -> OBJECT_LOCK_MODE;
                case "ObjectLockLegalHoldStatus" -> OBJECT_LOCK_LEGAL_HOLD_STATUS;
                case "IntelligentTieringAccessTier" -> INTELLIGENT_TIERING_ACCESS_TIER;
                case "BucketKeyStatus" -> BUCKET_KEY_STATUS;
                case "ChecksumAlgorithm" -> CHECKSUM_ALGORITHM;
                case "ObjectAccessControlList" -> OBJECT_ACCESS_CONTROL_LIST;
                case "ObjectOwner" -> OBJECT_OWNER;
                case "LifecycleExpirationDate" -> LIFECYCLE_EXPIRATION_DATE;
                default -> new $Unknown(value);
            };
        }

        @Override
        public Builder deserialize(ShapeDeserializer de) {
            return value(de.readString($SCHEMA));
        }
    }
}
