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

/**
 * Encoding type used by Amazon S3 to encode the <a href="https://docs.aws.amazon.com/AmazonS3/latest/userguide/object-keys.html">object keys</a> in the response. Responses are encoded only in
 * UTF-8. An object key can contain any Unicode character. However, the XML 1.0 parser can't parse certain characters,
 * such as characters with an ASCII value from 0 to 10. For characters that aren't supported in XML 1.0, you can add
 * this parameter to request that Amazon S3 encode the keys in the response. For more information about characters to
 * avoid in object key names, see <a href="https://docs.aws.amazon.com/AmazonS3/latest/userguide/object-keys.html#object-key-guidelines">Object key naming guidelines</a>.
 *
 * <p>When using the URL encoding type, non-ASCII characters that are used in an object's key name will be
 * percent-encoded according to UTF-8 code values. For example, the object <code>test_file(3).png</code> will appear as <code>
 * test_file%283%29.png</code>.
 */
@SmithyGenerated
public sealed interface EncodingType extends SmithyEnum, SerializableShape {
    EncodingType URL = new UrlType();
    List<EncodingType> $TYPES = List.of(URL);

    Schema $SCHEMA = Schema.createEnum(ShapeId.from("com.amazonaws.s3#EncodingType"),
        Set.of(URL.getValue()), EncodingType.class
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
    static EncodingType unknown(String value) {
        return new $Unknown(value);
    }

    /**
     * Returns an unmodifiable list containing the constants of this enum type, in the order declared.
     */
    static List<EncodingType> values() {
        return $TYPES;
    }

    /**
     * Returns a {@link EncodingType} constant with the specified value.
     *
     * @param value value to create {@code EncodingType} from.
     * @throws IllegalArgumentException if value does not match a known value.
     */
    static EncodingType from(String value) {
        return switch (value) {
            case "url" -> URL;
            default -> throw new IllegalArgumentException("Unknown value: " + value);
        };
    }

    final class UrlType implements EncodingType {
        private UrlType() {}

        @Override
        public String getValue() {
            return "url";
        }

        @Override
        public String toString() {
            return ToStringSerializer.serialize(this);
        }

    }

    record $Unknown(String value) implements EncodingType {
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

        private final class $Hidden implements EncodingType {
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
     * Builder for {@link EncodingType}.
     */
    final class Builder implements ShapeBuilder<EncodingType> {
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
        public EncodingType build() {
            return switch (value) {
                case "url" -> URL;
                default -> new $Unknown(value);
            };
        }

        @Override
        public Builder deserialize(ShapeDeserializer de) {
            return value(de.readString($SCHEMA));
        }
    }
}
