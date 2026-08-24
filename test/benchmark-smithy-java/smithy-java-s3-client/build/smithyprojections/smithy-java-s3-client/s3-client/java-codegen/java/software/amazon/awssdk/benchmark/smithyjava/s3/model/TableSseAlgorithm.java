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
public sealed interface TableSseAlgorithm extends SmithyEnum, SerializableShape {
    TableSseAlgorithm AWS_KMS = new AwsKmsType();
    TableSseAlgorithm AES256 = new Aes256Type();
    List<TableSseAlgorithm> $TYPES = List.of(AWS_KMS, AES256);

    Schema $SCHEMA = Schema.createEnum(ShapeId.from("com.amazonaws.s3#TableSseAlgorithm"),
        Set.of(AWS_KMS.getValue(), AES256.getValue()), TableSseAlgorithm.class
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
    static TableSseAlgorithm unknown(String value) {
        return new $Unknown(value);
    }

    /**
     * Returns an unmodifiable list containing the constants of this enum type, in the order declared.
     */
    static List<TableSseAlgorithm> values() {
        return $TYPES;
    }

    /**
     * Returns a {@link TableSseAlgorithm} constant with the specified value.
     *
     * @param value value to create {@code TableSseAlgorithm} from.
     * @throws IllegalArgumentException if value does not match a known value.
     */
    static TableSseAlgorithm from(String value) {
        return switch (value) {
            case "aws:kms" -> AWS_KMS;
            case "AES256" -> AES256;
            default -> throw new IllegalArgumentException("Unknown value: " + value);
        };
    }

    final class AwsKmsType implements TableSseAlgorithm {
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

    final class Aes256Type implements TableSseAlgorithm {
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

    record $Unknown(String value) implements TableSseAlgorithm {
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

        private final class $Hidden implements TableSseAlgorithm {
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
     * Builder for {@link TableSseAlgorithm}.
     */
    final class Builder implements ShapeBuilder<TableSseAlgorithm> {
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
        public TableSseAlgorithm build() {
            return switch (value) {
                case "aws:kms" -> AWS_KMS;
                case "AES256" -> AES256;
                default -> new $Unknown(value);
            };
        }

        @Override
        public Builder deserialize(ShapeDeserializer de) {
            return value(de.readString($SCHEMA));
        }
    }
}
