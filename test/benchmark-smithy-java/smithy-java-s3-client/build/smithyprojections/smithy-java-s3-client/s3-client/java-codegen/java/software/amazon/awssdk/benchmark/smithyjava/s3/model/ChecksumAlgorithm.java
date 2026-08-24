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
public sealed interface ChecksumAlgorithm extends SmithyEnum, SerializableShape {
    ChecksumAlgorithm CRC32 = new Crc32Type();
    ChecksumAlgorithm CRC32_C = new Crc32CType();
    ChecksumAlgorithm SHA1 = new Sha1Type();
    ChecksumAlgorithm SHA256 = new Sha256Type();
    ChecksumAlgorithm CRC64_NVME = new Crc64NvmeType();
    ChecksumAlgorithm SHA512 = new Sha512Type();
    ChecksumAlgorithm MD5 = new Md5Type();
    ChecksumAlgorithm XXHASH64 = new Xxhash64Type();
    ChecksumAlgorithm XXHASH3 = new Xxhash3Type();
    ChecksumAlgorithm XXHASH128 = new Xxhash128Type();
    List<ChecksumAlgorithm> $TYPES = List.of(CRC32, CRC32_C, SHA1, SHA256, CRC64_NVME, SHA512, MD5, XXHASH64, XXHASH3, XXHASH128);

    Schema $SCHEMA = Schema.createEnum(ShapeId.from("com.amazonaws.s3#ChecksumAlgorithm"),
        Set.of(CRC32.getValue(), CRC32_C.getValue(), SHA1.getValue(), SHA256.getValue(), CRC64_NVME.getValue(), SHA512.getValue(), MD5.getValue(), XXHASH64.getValue(), XXHASH3.getValue(), XXHASH128.getValue()), ChecksumAlgorithm.class
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
    static ChecksumAlgorithm unknown(String value) {
        return new $Unknown(value);
    }

    /**
     * Returns an unmodifiable list containing the constants of this enum type, in the order declared.
     */
    static List<ChecksumAlgorithm> values() {
        return $TYPES;
    }

    /**
     * Returns a {@link ChecksumAlgorithm} constant with the specified value.
     *
     * @param value value to create {@code ChecksumAlgorithm} from.
     * @throws IllegalArgumentException if value does not match a known value.
     */
    static ChecksumAlgorithm from(String value) {
        return switch (value) {
            case "CRC32" -> CRC32;
            case "CRC32C" -> CRC32_C;
            case "SHA1" -> SHA1;
            case "SHA256" -> SHA256;
            case "CRC64NVME" -> CRC64_NVME;
            case "SHA512" -> SHA512;
            case "MD5" -> MD5;
            case "XXHASH64" -> XXHASH64;
            case "XXHASH3" -> XXHASH3;
            case "XXHASH128" -> XXHASH128;
            default -> throw new IllegalArgumentException("Unknown value: " + value);
        };
    }

    final class Crc32Type implements ChecksumAlgorithm {
        private Crc32Type() {}

        @Override
        public String getValue() {
            return "CRC32";
        }

        @Override
        public String toString() {
            return ToStringSerializer.serialize(this);
        }

    }

    final class Crc32CType implements ChecksumAlgorithm {
        private Crc32CType() {}

        @Override
        public String getValue() {
            return "CRC32C";
        }

        @Override
        public String toString() {
            return ToStringSerializer.serialize(this);
        }

    }

    final class Sha1Type implements ChecksumAlgorithm {
        private Sha1Type() {}

        @Override
        public String getValue() {
            return "SHA1";
        }

        @Override
        public String toString() {
            return ToStringSerializer.serialize(this);
        }

    }

    final class Sha256Type implements ChecksumAlgorithm {
        private Sha256Type() {}

        @Override
        public String getValue() {
            return "SHA256";
        }

        @Override
        public String toString() {
            return ToStringSerializer.serialize(this);
        }

    }

    final class Crc64NvmeType implements ChecksumAlgorithm {
        private Crc64NvmeType() {}

        @Override
        public String getValue() {
            return "CRC64NVME";
        }

        @Override
        public String toString() {
            return ToStringSerializer.serialize(this);
        }

    }

    final class Sha512Type implements ChecksumAlgorithm {
        private Sha512Type() {}

        @Override
        public String getValue() {
            return "SHA512";
        }

        @Override
        public String toString() {
            return ToStringSerializer.serialize(this);
        }

    }

    final class Md5Type implements ChecksumAlgorithm {
        private Md5Type() {}

        @Override
        public String getValue() {
            return "MD5";
        }

        @Override
        public String toString() {
            return ToStringSerializer.serialize(this);
        }

    }

    final class Xxhash64Type implements ChecksumAlgorithm {
        private Xxhash64Type() {}

        @Override
        public String getValue() {
            return "XXHASH64";
        }

        @Override
        public String toString() {
            return ToStringSerializer.serialize(this);
        }

    }

    final class Xxhash3Type implements ChecksumAlgorithm {
        private Xxhash3Type() {}

        @Override
        public String getValue() {
            return "XXHASH3";
        }

        @Override
        public String toString() {
            return ToStringSerializer.serialize(this);
        }

    }

    final class Xxhash128Type implements ChecksumAlgorithm {
        private Xxhash128Type() {}

        @Override
        public String getValue() {
            return "XXHASH128";
        }

        @Override
        public String toString() {
            return ToStringSerializer.serialize(this);
        }

    }

    record $Unknown(String value) implements ChecksumAlgorithm {
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

        private final class $Hidden implements ChecksumAlgorithm {
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
     * Builder for {@link ChecksumAlgorithm}.
     */
    final class Builder implements ShapeBuilder<ChecksumAlgorithm> {
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
        public ChecksumAlgorithm build() {
            return switch (value) {
                case "CRC32" -> CRC32;
                case "CRC32C" -> CRC32_C;
                case "SHA1" -> SHA1;
                case "SHA256" -> SHA256;
                case "CRC64NVME" -> CRC64_NVME;
                case "SHA512" -> SHA512;
                case "MD5" -> MD5;
                case "XXHASH64" -> XXHASH64;
                case "XXHASH3" -> XXHASH3;
                case "XXHASH128" -> XXHASH128;
                default -> new $Unknown(value);
            };
        }

        @Override
        public Builder deserialize(ShapeDeserializer de) {
            return value(de.readString($SCHEMA));
        }
    }
}
