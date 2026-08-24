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
public sealed interface MetadataDirective extends SmithyEnum, SerializableShape {
    MetadataDirective COPY = new CopyType();
    MetadataDirective REPLACE = new ReplaceType();
    List<MetadataDirective> $TYPES = List.of(COPY, REPLACE);

    Schema $SCHEMA = Schema.createEnum(ShapeId.from("com.amazonaws.s3#MetadataDirective"),
        Set.of(COPY.getValue(), REPLACE.getValue()), MetadataDirective.class
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
    static MetadataDirective unknown(String value) {
        return new $Unknown(value);
    }

    /**
     * Returns an unmodifiable list containing the constants of this enum type, in the order declared.
     */
    static List<MetadataDirective> values() {
        return $TYPES;
    }

    /**
     * Returns a {@link MetadataDirective} constant with the specified value.
     *
     * @param value value to create {@code MetadataDirective} from.
     * @throws IllegalArgumentException if value does not match a known value.
     */
    static MetadataDirective from(String value) {
        return switch (value) {
            case "COPY" -> COPY;
            case "REPLACE" -> REPLACE;
            default -> throw new IllegalArgumentException("Unknown value: " + value);
        };
    }

    final class CopyType implements MetadataDirective {
        private CopyType() {}

        @Override
        public String getValue() {
            return "COPY";
        }

        @Override
        public String toString() {
            return ToStringSerializer.serialize(this);
        }

    }

    final class ReplaceType implements MetadataDirective {
        private ReplaceType() {}

        @Override
        public String getValue() {
            return "REPLACE";
        }

        @Override
        public String toString() {
            return ToStringSerializer.serialize(this);
        }

    }

    record $Unknown(String value) implements MetadataDirective {
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

        private final class $Hidden implements MetadataDirective {
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
     * Builder for {@link MetadataDirective}.
     */
    final class Builder implements ShapeBuilder<MetadataDirective> {
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
        public MetadataDirective build() {
            return switch (value) {
                case "COPY" -> COPY;
                case "REPLACE" -> REPLACE;
                default -> new $Unknown(value);
            };
        }

        @Override
        public Builder deserialize(ShapeDeserializer de) {
            return value(de.readString($SCHEMA));
        }
    }
}
