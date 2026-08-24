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
public sealed interface S3SseAlgorithm extends SmithyEnum, SerializableShape {
    S3SseAlgorithm AES256 = new Aes256Type();
    S3SseAlgorithm KMS = new KmsType();
    List<S3SseAlgorithm> $TYPES = List.of(AES256, KMS);

    Schema $SCHEMA = Schema.createEnum(ShapeId.from("com.amazonaws.dynamodb#S3SseAlgorithm"),
        Set.of(AES256.getValue(), KMS.getValue()), S3SseAlgorithm.class
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
    static S3SseAlgorithm unknown(String value) {
        return new $Unknown(value);
    }

    /**
     * Returns an unmodifiable list containing the constants of this enum type, in the order declared.
     */
    static List<S3SseAlgorithm> values() {
        return $TYPES;
    }

    /**
     * Returns a {@link S3SseAlgorithm} constant with the specified value.
     *
     * @param value value to create {@code S3SseAlgorithm} from.
     * @throws IllegalArgumentException if value does not match a known value.
     */
    static S3SseAlgorithm from(String value) {
        return switch (value) {
            case "AES256" -> AES256;
            case "KMS" -> KMS;
            default -> throw new IllegalArgumentException("Unknown value: " + value);
        };
    }

    final class Aes256Type implements S3SseAlgorithm {
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

    final class KmsType implements S3SseAlgorithm {
        private KmsType() {}

        @Override
        public String getValue() {
            return "KMS";
        }

        @Override
        public String toString() {
            return ToStringSerializer.serialize(this);
        }

    }

    record $Unknown(String value) implements S3SseAlgorithm {
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

        private final class $Hidden implements S3SseAlgorithm {
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
     * Builder for {@link S3SseAlgorithm}.
     */
    final class Builder implements ShapeBuilder<S3SseAlgorithm> {
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
        public S3SseAlgorithm build() {
            return switch (value) {
                case "AES256" -> AES256;
                case "KMS" -> KMS;
                default -> new $Unknown(value);
            };
        }

        @Override
        public Builder deserialize(ShapeDeserializer de) {
            return value(de.readString($SCHEMA));
        }
    }
}
