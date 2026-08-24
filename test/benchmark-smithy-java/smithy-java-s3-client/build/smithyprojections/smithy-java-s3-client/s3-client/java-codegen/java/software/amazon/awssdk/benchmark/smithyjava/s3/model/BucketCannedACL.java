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
public sealed interface BucketCannedACL extends SmithyEnum, SerializableShape {
    BucketCannedACL PRIVATE = new PrivateType();
    BucketCannedACL PUBLIC_READ = new PublicReadType();
    BucketCannedACL PUBLIC_READ_WRITE = new PublicReadWriteType();
    BucketCannedACL AUTHENTICATED_READ = new AuthenticatedReadType();
    List<BucketCannedACL> $TYPES = List.of(PRIVATE, PUBLIC_READ, PUBLIC_READ_WRITE, AUTHENTICATED_READ);

    Schema $SCHEMA = Schema.createEnum(ShapeId.from("com.amazonaws.s3#BucketCannedACL"),
        Set.of(PRIVATE.getValue(), PUBLIC_READ.getValue(), PUBLIC_READ_WRITE.getValue(), AUTHENTICATED_READ.getValue()), BucketCannedACL.class
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
    static BucketCannedACL unknown(String value) {
        return new $Unknown(value);
    }

    /**
     * Returns an unmodifiable list containing the constants of this enum type, in the order declared.
     */
    static List<BucketCannedACL> values() {
        return $TYPES;
    }

    /**
     * Returns a {@link BucketCannedACL} constant with the specified value.
     *
     * @param value value to create {@code BucketCannedACL} from.
     * @throws IllegalArgumentException if value does not match a known value.
     */
    static BucketCannedACL from(String value) {
        return switch (value) {
            case "private" -> PRIVATE;
            case "public-read" -> PUBLIC_READ;
            case "public-read-write" -> PUBLIC_READ_WRITE;
            case "authenticated-read" -> AUTHENTICATED_READ;
            default -> throw new IllegalArgumentException("Unknown value: " + value);
        };
    }

    final class PrivateType implements BucketCannedACL {
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

    final class PublicReadType implements BucketCannedACL {
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

    final class PublicReadWriteType implements BucketCannedACL {
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

    final class AuthenticatedReadType implements BucketCannedACL {
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

    record $Unknown(String value) implements BucketCannedACL {
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

        private final class $Hidden implements BucketCannedACL {
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
     * Builder for {@link BucketCannedACL}.
     */
    final class Builder implements ShapeBuilder<BucketCannedACL> {
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
        public BucketCannedACL build() {
            return switch (value) {
                case "private" -> PRIVATE;
                case "public-read" -> PUBLIC_READ;
                case "public-read-write" -> PUBLIC_READ_WRITE;
                case "authenticated-read" -> AUTHENTICATED_READ;
                default -> new $Unknown(value);
            };
        }

        @Override
        public Builder deserialize(ShapeDeserializer de) {
            return value(de.readString($SCHEMA));
        }
    }
}
