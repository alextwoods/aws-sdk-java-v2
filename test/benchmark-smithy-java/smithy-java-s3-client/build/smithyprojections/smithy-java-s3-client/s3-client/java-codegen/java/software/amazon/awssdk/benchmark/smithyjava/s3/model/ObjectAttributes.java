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
public sealed interface ObjectAttributes extends SmithyEnum, SerializableShape {
    ObjectAttributes ETAG = new EtagType();
    ObjectAttributes CHECKSUM = new ChecksumType();
    ObjectAttributes OBJECT_PARTS = new ObjectPartsType();
    ObjectAttributes STORAGE_CLASS = new StorageClassType();
    ObjectAttributes OBJECT_SIZE = new ObjectSizeType();
    List<ObjectAttributes> $TYPES = List.of(ETAG, CHECKSUM, OBJECT_PARTS, STORAGE_CLASS, OBJECT_SIZE);

    Schema $SCHEMA = Schema.createEnum(ShapeId.from("com.amazonaws.s3#ObjectAttributes"),
        Set.of(ETAG.getValue(), CHECKSUM.getValue(), OBJECT_PARTS.getValue(), STORAGE_CLASS.getValue(), OBJECT_SIZE.getValue()), ObjectAttributes.class
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
    static ObjectAttributes unknown(String value) {
        return new $Unknown(value);
    }

    /**
     * Returns an unmodifiable list containing the constants of this enum type, in the order declared.
     */
    static List<ObjectAttributes> values() {
        return $TYPES;
    }

    /**
     * Returns a {@link ObjectAttributes} constant with the specified value.
     *
     * @param value value to create {@code ObjectAttributes} from.
     * @throws IllegalArgumentException if value does not match a known value.
     */
    static ObjectAttributes from(String value) {
        return switch (value) {
            case "ETag" -> ETAG;
            case "Checksum" -> CHECKSUM;
            case "ObjectParts" -> OBJECT_PARTS;
            case "StorageClass" -> STORAGE_CLASS;
            case "ObjectSize" -> OBJECT_SIZE;
            default -> throw new IllegalArgumentException("Unknown value: " + value);
        };
    }

    final class EtagType implements ObjectAttributes {
        private EtagType() {}

        @Override
        public String getValue() {
            return "ETag";
        }

        @Override
        public String toString() {
            return ToStringSerializer.serialize(this);
        }

    }

    final class ChecksumType implements ObjectAttributes {
        private ChecksumType() {}

        @Override
        public String getValue() {
            return "Checksum";
        }

        @Override
        public String toString() {
            return ToStringSerializer.serialize(this);
        }

    }

    final class ObjectPartsType implements ObjectAttributes {
        private ObjectPartsType() {}

        @Override
        public String getValue() {
            return "ObjectParts";
        }

        @Override
        public String toString() {
            return ToStringSerializer.serialize(this);
        }

    }

    final class StorageClassType implements ObjectAttributes {
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

    final class ObjectSizeType implements ObjectAttributes {
        private ObjectSizeType() {}

        @Override
        public String getValue() {
            return "ObjectSize";
        }

        @Override
        public String toString() {
            return ToStringSerializer.serialize(this);
        }

    }

    record $Unknown(String value) implements ObjectAttributes {
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

        private final class $Hidden implements ObjectAttributes {
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
     * Builder for {@link ObjectAttributes}.
     */
    final class Builder implements ShapeBuilder<ObjectAttributes> {
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
        public ObjectAttributes build() {
            return switch (value) {
                case "ETag" -> ETAG;
                case "Checksum" -> CHECKSUM;
                case "ObjectParts" -> OBJECT_PARTS;
                case "StorageClass" -> STORAGE_CLASS;
                case "ObjectSize" -> OBJECT_SIZE;
                default -> new $Unknown(value);
            };
        }

        @Override
        public Builder deserialize(ShapeDeserializer de) {
            return value(de.readString($SCHEMA));
        }
    }
}
