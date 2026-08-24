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
public sealed interface S3TablesBucketType extends SmithyEnum, SerializableShape {
    S3TablesBucketType AWS = new AwsType();
    S3TablesBucketType CUSTOMER = new CustomerType();
    List<S3TablesBucketType> $TYPES = List.of(AWS, CUSTOMER);

    Schema $SCHEMA = Schema.createEnum(ShapeId.from("com.amazonaws.s3#S3TablesBucketType"),
        Set.of(AWS.getValue(), CUSTOMER.getValue()), S3TablesBucketType.class
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
    static S3TablesBucketType unknown(String value) {
        return new $Unknown(value);
    }

    /**
     * Returns an unmodifiable list containing the constants of this enum type, in the order declared.
     */
    static List<S3TablesBucketType> values() {
        return $TYPES;
    }

    /**
     * Returns a {@link S3TablesBucketType} constant with the specified value.
     *
     * @param value value to create {@code S3TablesBucketType} from.
     * @throws IllegalArgumentException if value does not match a known value.
     */
    static S3TablesBucketType from(String value) {
        return switch (value) {
            case "aws" -> AWS;
            case "customer" -> CUSTOMER;
            default -> throw new IllegalArgumentException("Unknown value: " + value);
        };
    }

    final class AwsType implements S3TablesBucketType {
        private AwsType() {}

        @Override
        public String getValue() {
            return "aws";
        }

        @Override
        public String toString() {
            return ToStringSerializer.serialize(this);
        }

    }

    final class CustomerType implements S3TablesBucketType {
        private CustomerType() {}

        @Override
        public String getValue() {
            return "customer";
        }

        @Override
        public String toString() {
            return ToStringSerializer.serialize(this);
        }

    }

    record $Unknown(String value) implements S3TablesBucketType {
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

        private final class $Hidden implements S3TablesBucketType {
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
     * Builder for {@link S3TablesBucketType}.
     */
    final class Builder implements ShapeBuilder<S3TablesBucketType> {
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
        public S3TablesBucketType build() {
            return switch (value) {
                case "aws" -> AWS;
                case "customer" -> CUSTOMER;
                default -> new $Unknown(value);
            };
        }

        @Override
        public Builder deserialize(ShapeDeserializer de) {
            return value(de.readString($SCHEMA));
        }
    }
}
