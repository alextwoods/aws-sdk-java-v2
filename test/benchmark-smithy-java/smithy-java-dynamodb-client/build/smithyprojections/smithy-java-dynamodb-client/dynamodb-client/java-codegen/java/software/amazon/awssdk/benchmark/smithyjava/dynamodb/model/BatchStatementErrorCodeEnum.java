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
public sealed interface BatchStatementErrorCodeEnum extends SmithyEnum, SerializableShape {
    BatchStatementErrorCodeEnum CONDITIONAL_CHECK_FAILED = new ConditionalCheckFailedType();
    BatchStatementErrorCodeEnum ITEM_COLLECTION_SIZE_LIMIT_EXCEEDED = new ItemCollectionSizeLimitExceededType();
    BatchStatementErrorCodeEnum REQUEST_LIMIT_EXCEEDED = new RequestLimitExceededType();
    BatchStatementErrorCodeEnum VALIDATION_ERROR = new ValidationErrorType();
    BatchStatementErrorCodeEnum PROVISIONED_THROUGHPUT_EXCEEDED = new ProvisionedThroughputExceededType();
    BatchStatementErrorCodeEnum TRANSACTION_CONFLICT = new TransactionConflictType();
    BatchStatementErrorCodeEnum THROTTLING_ERROR = new ThrottlingErrorType();
    BatchStatementErrorCodeEnum INTERNAL_SERVER_ERROR = new InternalServerErrorType();
    BatchStatementErrorCodeEnum RESOURCE_NOT_FOUND = new ResourceNotFoundType();
    BatchStatementErrorCodeEnum ACCESS_DENIED = new AccessDeniedType();
    BatchStatementErrorCodeEnum DUPLICATE_ITEM = new DuplicateItemType();
    List<BatchStatementErrorCodeEnum> $TYPES = List.of(CONDITIONAL_CHECK_FAILED, ITEM_COLLECTION_SIZE_LIMIT_EXCEEDED, REQUEST_LIMIT_EXCEEDED, VALIDATION_ERROR, PROVISIONED_THROUGHPUT_EXCEEDED, TRANSACTION_CONFLICT, THROTTLING_ERROR, INTERNAL_SERVER_ERROR, RESOURCE_NOT_FOUND, ACCESS_DENIED, DUPLICATE_ITEM);

    Schema $SCHEMA = Schema.createEnum(ShapeId.from("com.amazonaws.dynamodb#BatchStatementErrorCodeEnum"),
        Set.of(CONDITIONAL_CHECK_FAILED.getValue(), ITEM_COLLECTION_SIZE_LIMIT_EXCEEDED.getValue(), REQUEST_LIMIT_EXCEEDED.getValue(), VALIDATION_ERROR.getValue(), PROVISIONED_THROUGHPUT_EXCEEDED.getValue(), TRANSACTION_CONFLICT.getValue(), THROTTLING_ERROR.getValue(), INTERNAL_SERVER_ERROR.getValue(), RESOURCE_NOT_FOUND.getValue(), ACCESS_DENIED.getValue(), DUPLICATE_ITEM.getValue()), BatchStatementErrorCodeEnum.class
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
    static BatchStatementErrorCodeEnum unknown(String value) {
        return new $Unknown(value);
    }

    /**
     * Returns an unmodifiable list containing the constants of this enum type, in the order declared.
     */
    static List<BatchStatementErrorCodeEnum> values() {
        return $TYPES;
    }

    /**
     * Returns a {@link BatchStatementErrorCodeEnum} constant with the specified value.
     *
     * @param value value to create {@code BatchStatementErrorCodeEnum} from.
     * @throws IllegalArgumentException if value does not match a known value.
     */
    static BatchStatementErrorCodeEnum from(String value) {
        return switch (value) {
            case "ConditionalCheckFailed" -> CONDITIONAL_CHECK_FAILED;
            case "ItemCollectionSizeLimitExceeded" -> ITEM_COLLECTION_SIZE_LIMIT_EXCEEDED;
            case "RequestLimitExceeded" -> REQUEST_LIMIT_EXCEEDED;
            case "ValidationError" -> VALIDATION_ERROR;
            case "ProvisionedThroughputExceeded" -> PROVISIONED_THROUGHPUT_EXCEEDED;
            case "TransactionConflict" -> TRANSACTION_CONFLICT;
            case "ThrottlingError" -> THROTTLING_ERROR;
            case "InternalServerError" -> INTERNAL_SERVER_ERROR;
            case "ResourceNotFound" -> RESOURCE_NOT_FOUND;
            case "AccessDenied" -> ACCESS_DENIED;
            case "DuplicateItem" -> DUPLICATE_ITEM;
            default -> throw new IllegalArgumentException("Unknown value: " + value);
        };
    }

    final class ConditionalCheckFailedType implements BatchStatementErrorCodeEnum {
        private ConditionalCheckFailedType() {}

        @Override
        public String getValue() {
            return "ConditionalCheckFailed";
        }

        @Override
        public String toString() {
            return ToStringSerializer.serialize(this);
        }

    }

    final class ItemCollectionSizeLimitExceededType implements BatchStatementErrorCodeEnum {
        private ItemCollectionSizeLimitExceededType() {}

        @Override
        public String getValue() {
            return "ItemCollectionSizeLimitExceeded";
        }

        @Override
        public String toString() {
            return ToStringSerializer.serialize(this);
        }

    }

    final class RequestLimitExceededType implements BatchStatementErrorCodeEnum {
        private RequestLimitExceededType() {}

        @Override
        public String getValue() {
            return "RequestLimitExceeded";
        }

        @Override
        public String toString() {
            return ToStringSerializer.serialize(this);
        }

    }

    final class ValidationErrorType implements BatchStatementErrorCodeEnum {
        private ValidationErrorType() {}

        @Override
        public String getValue() {
            return "ValidationError";
        }

        @Override
        public String toString() {
            return ToStringSerializer.serialize(this);
        }

    }

    final class ProvisionedThroughputExceededType implements BatchStatementErrorCodeEnum {
        private ProvisionedThroughputExceededType() {}

        @Override
        public String getValue() {
            return "ProvisionedThroughputExceeded";
        }

        @Override
        public String toString() {
            return ToStringSerializer.serialize(this);
        }

    }

    final class TransactionConflictType implements BatchStatementErrorCodeEnum {
        private TransactionConflictType() {}

        @Override
        public String getValue() {
            return "TransactionConflict";
        }

        @Override
        public String toString() {
            return ToStringSerializer.serialize(this);
        }

    }

    final class ThrottlingErrorType implements BatchStatementErrorCodeEnum {
        private ThrottlingErrorType() {}

        @Override
        public String getValue() {
            return "ThrottlingError";
        }

        @Override
        public String toString() {
            return ToStringSerializer.serialize(this);
        }

    }

    final class InternalServerErrorType implements BatchStatementErrorCodeEnum {
        private InternalServerErrorType() {}

        @Override
        public String getValue() {
            return "InternalServerError";
        }

        @Override
        public String toString() {
            return ToStringSerializer.serialize(this);
        }

    }

    final class ResourceNotFoundType implements BatchStatementErrorCodeEnum {
        private ResourceNotFoundType() {}

        @Override
        public String getValue() {
            return "ResourceNotFound";
        }

        @Override
        public String toString() {
            return ToStringSerializer.serialize(this);
        }

    }

    final class AccessDeniedType implements BatchStatementErrorCodeEnum {
        private AccessDeniedType() {}

        @Override
        public String getValue() {
            return "AccessDenied";
        }

        @Override
        public String toString() {
            return ToStringSerializer.serialize(this);
        }

    }

    final class DuplicateItemType implements BatchStatementErrorCodeEnum {
        private DuplicateItemType() {}

        @Override
        public String getValue() {
            return "DuplicateItem";
        }

        @Override
        public String toString() {
            return ToStringSerializer.serialize(this);
        }

    }

    record $Unknown(String value) implements BatchStatementErrorCodeEnum {
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

        private final class $Hidden implements BatchStatementErrorCodeEnum {
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
     * Builder for {@link BatchStatementErrorCodeEnum}.
     */
    final class Builder implements ShapeBuilder<BatchStatementErrorCodeEnum> {
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
        public BatchStatementErrorCodeEnum build() {
            return switch (value) {
                case "ConditionalCheckFailed" -> CONDITIONAL_CHECK_FAILED;
                case "ItemCollectionSizeLimitExceeded" -> ITEM_COLLECTION_SIZE_LIMIT_EXCEEDED;
                case "RequestLimitExceeded" -> REQUEST_LIMIT_EXCEEDED;
                case "ValidationError" -> VALIDATION_ERROR;
                case "ProvisionedThroughputExceeded" -> PROVISIONED_THROUGHPUT_EXCEEDED;
                case "TransactionConflict" -> TRANSACTION_CONFLICT;
                case "ThrottlingError" -> THROTTLING_ERROR;
                case "InternalServerError" -> INTERNAL_SERVER_ERROR;
                case "ResourceNotFound" -> RESOURCE_NOT_FOUND;
                case "AccessDenied" -> ACCESS_DENIED;
                case "DuplicateItem" -> DUPLICATE_ITEM;
                default -> new $Unknown(value);
            };
        }

        @Override
        public Builder deserialize(ShapeDeserializer de) {
            return value(de.readString($SCHEMA));
        }
    }
}
