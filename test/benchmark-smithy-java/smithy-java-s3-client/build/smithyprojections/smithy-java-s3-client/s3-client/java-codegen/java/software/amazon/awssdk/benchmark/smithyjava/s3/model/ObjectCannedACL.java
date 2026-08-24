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
public sealed interface ObjectCannedACL extends SmithyEnum, SerializableShape {
    ObjectCannedACL PRIVATE = new PrivateType();
    ObjectCannedACL PUBLIC_READ = new PublicReadType();
    ObjectCannedACL PUBLIC_READ_WRITE = new PublicReadWriteType();
    ObjectCannedACL AUTHENTICATED_READ = new AuthenticatedReadType();
    ObjectCannedACL AWS_EXEC_READ = new AwsExecReadType();
    ObjectCannedACL BUCKET_OWNER_READ = new BucketOwnerReadType();
    ObjectCannedACL BUCKET_OWNER_FULL_CONTROL = new BucketOwnerFullControlType();
    List<ObjectCannedACL> $TYPES = List.of(PRIVATE, PUBLIC_READ, PUBLIC_READ_WRITE, AUTHENTICATED_READ, AWS_EXEC_READ, BUCKET_OWNER_READ, BUCKET_OWNER_FULL_CONTROL);

    Schema $SCHEMA = Schema.createEnum(ShapeId.from("com.amazonaws.s3#ObjectCannedACL"),
        Set.of(PRIVATE.getValue(), PUBLIC_READ.getValue(), PUBLIC_READ_WRITE.getValue(), AUTHENTICATED_READ.getValue(), AWS_EXEC_READ.getValue(), BUCKET_OWNER_READ.getValue(), BUCKET_OWNER_FULL_CONTROL.getValue()), ObjectCannedACL.class
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
    static ObjectCannedACL unknown(String value) {
        return new $Unknown(value);
    }

    /**
     * Returns an unmodifiable list containing the constants of this enum type, in the order declared.
     */
    static List<ObjectCannedACL> values() {
        return $TYPES;
    }

    /**
     * Returns a {@link ObjectCannedACL} constant with the specified value.
     *
     * @param value value to create {@code ObjectCannedACL} from.
     * @throws IllegalArgumentException if value does not match a known value.
     */
    static ObjectCannedACL from(String value) {
        return switch (value) {
            case "private" -> PRIVATE;
            case "public-read" -> PUBLIC_READ;
            case "public-read-write" -> PUBLIC_READ_WRITE;
            case "authenticated-read" -> AUTHENTICATED_READ;
            case "aws-exec-read" -> AWS_EXEC_READ;
            case "bucket-owner-read" -> BUCKET_OWNER_READ;
            case "bucket-owner-full-control" -> BUCKET_OWNER_FULL_CONTROL;
            default -> throw new IllegalArgumentException("Unknown value: " + value);
        };
    }

    final class PrivateType implements ObjectCannedACL {
        private PrivateType() {}

        @Override
        public String getValue() {
            return "private";
        }

        @Override
        public String toString() {
            return ToStringSerializer.serialize(this);
        }

    }

    final class PublicReadType implements ObjectCannedACL {
        private PublicReadType() {}

        @Override
        public String getValue() {
            return "public-read";
        }

        @Override
        public String toString() {
            return ToStringSerializer.serialize(this);
        }

    }

    final class PublicReadWriteType implements ObjectCannedACL {
        private PublicReadWriteType() {}

        @Override
        public String getValue() {
            return "public-read-write";
        }

        @Override
        public String toString() {
            return ToStringSerializer.serialize(this);
        }

    }

    final class AuthenticatedReadType implements ObjectCannedACL {
        private AuthenticatedReadType() {}

        @Override
        public String getValue() {
            return "authenticated-read";
        }

        @Override
        public String toString() {
            return ToStringSerializer.serialize(this);
        }

    }

    final class AwsExecReadType implements ObjectCannedACL {
        private AwsExecReadType() {}

        @Override
        public String getValue() {
            return "aws-exec-read";
        }

        @Override
        public String toString() {
            return ToStringSerializer.serialize(this);
        }

    }

    final class BucketOwnerReadType implements ObjectCannedACL {
        private BucketOwnerReadType() {}

        @Override
        public String getValue() {
            return "bucket-owner-read";
        }

        @Override
        public String toString() {
            return ToStringSerializer.serialize(this);
        }

    }

    final class BucketOwnerFullControlType implements ObjectCannedACL {
        private BucketOwnerFullControlType() {}

        @Override
        public String getValue() {
            return "bucket-owner-full-control";
        }

        @Override
        public String toString() {
            return ToStringSerializer.serialize(this);
        }

    }

    record $Unknown(String value) implements ObjectCannedACL {
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

        private final class $Hidden implements ObjectCannedACL {
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
     * Builder for {@link ObjectCannedACL}.
     */
    final class Builder implements ShapeBuilder<ObjectCannedACL> {
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
        public ObjectCannedACL build() {
            return switch (value) {
                case "private" -> PRIVATE;
                case "public-read" -> PUBLIC_READ;
                case "public-read-write" -> PUBLIC_READ_WRITE;
                case "authenticated-read" -> AUTHENTICATED_READ;
                case "aws-exec-read" -> AWS_EXEC_READ;
                case "bucket-owner-read" -> BUCKET_OWNER_READ;
                case "bucket-owner-full-control" -> BUCKET_OWNER_FULL_CONTROL;
                default -> new $Unknown(value);
            };
        }

        @Override
        public Builder deserialize(ShapeDeserializer de) {
            return value(de.readString($SCHEMA));
        }
    }
}
