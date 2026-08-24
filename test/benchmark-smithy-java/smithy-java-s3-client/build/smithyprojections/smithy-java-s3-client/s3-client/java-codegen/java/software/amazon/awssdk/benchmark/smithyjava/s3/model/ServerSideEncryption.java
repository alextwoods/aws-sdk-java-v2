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
public sealed interface ServerSideEncryption extends SmithyEnum, SerializableShape {
    ServerSideEncryption AES256 = new Aes256Type();
    ServerSideEncryption AWS_FSX = new AwsFsxType();
    ServerSideEncryption AWS_KMS = new AwsKmsType();
    ServerSideEncryption AWS_KMS_DSSE = new AwsKmsDsseType();
    List<ServerSideEncryption> $TYPES = List.of(AES256, AWS_FSX, AWS_KMS, AWS_KMS_DSSE);

    Schema $SCHEMA = Schema.createEnum(ShapeId.from("com.amazonaws.s3#ServerSideEncryption"),
        Set.of(AES256.getValue(), AWS_FSX.getValue(), AWS_KMS.getValue(), AWS_KMS_DSSE.getValue()), ServerSideEncryption.class
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
    static ServerSideEncryption unknown(String value) {
        return new $Unknown(value);
    }

    /**
     * Returns an unmodifiable list containing the constants of this enum type, in the order declared.
     */
    static List<ServerSideEncryption> values() {
        return $TYPES;
    }

    /**
     * Returns a {@link ServerSideEncryption} constant with the specified value.
     *
     * @param value value to create {@code ServerSideEncryption} from.
     * @throws IllegalArgumentException if value does not match a known value.
     */
    static ServerSideEncryption from(String value) {
        return switch (value) {
            case "AES256" -> AES256;
            case "aws:fsx" -> AWS_FSX;
            case "aws:kms" -> AWS_KMS;
            case "aws:kms:dsse" -> AWS_KMS_DSSE;
            default -> throw new IllegalArgumentException("Unknown value: " + value);
        };
    }

    final class Aes256Type implements ServerSideEncryption {
        private Aes256Type() {}

        @Override
        public String getValue() {
            return "AES256";
        }

        @Override
        public String toString() {
            return ToStringSerializer.serialize(this);
        }

    }

    final class AwsFsxType implements ServerSideEncryption {
        private AwsFsxType() {}

        @Override
        public String getValue() {
            return "aws:fsx";
        }

        @Override
        public String toString() {
            return ToStringSerializer.serialize(this);
        }

    }

    final class AwsKmsType implements ServerSideEncryption {
        private AwsKmsType() {}

        @Override
        public String getValue() {
            return "aws:kms";
        }

        @Override
        public String toString() {
            return ToStringSerializer.serialize(this);
        }

    }

    final class AwsKmsDsseType implements ServerSideEncryption {
        private AwsKmsDsseType() {}

        @Override
        public String getValue() {
            return "aws:kms:dsse";
        }

        @Override
        public String toString() {
            return ToStringSerializer.serialize(this);
        }

    }

    record $Unknown(String value) implements ServerSideEncryption {
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

        private final class $Hidden implements ServerSideEncryption {
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
     * Builder for {@link ServerSideEncryption}.
     */
    final class Builder implements ShapeBuilder<ServerSideEncryption> {
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
        public ServerSideEncryption build() {
            return switch (value) {
                case "AES256" -> AES256;
                case "aws:fsx" -> AWS_FSX;
                case "aws:kms" -> AWS_KMS;
                case "aws:kms:dsse" -> AWS_KMS_DSSE;
                default -> new $Unknown(value);
            };
        }

        @Override
        public Builder deserialize(ShapeDeserializer de) {
            return value(de.readString($SCHEMA));
        }
    }
}
