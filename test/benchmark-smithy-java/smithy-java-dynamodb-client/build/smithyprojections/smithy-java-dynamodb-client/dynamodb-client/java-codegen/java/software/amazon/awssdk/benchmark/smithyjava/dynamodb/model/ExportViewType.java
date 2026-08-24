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
public sealed interface ExportViewType extends SmithyEnum, SerializableShape {
    ExportViewType NEW_IMAGE = new NewImageType();
    ExportViewType NEW_AND_OLD_IMAGES = new NewAndOldImagesType();
    List<ExportViewType> $TYPES = List.of(NEW_IMAGE, NEW_AND_OLD_IMAGES);

    Schema $SCHEMA = Schema.createEnum(ShapeId.from("com.amazonaws.dynamodb#ExportViewType"),
        Set.of(NEW_IMAGE.getValue(), NEW_AND_OLD_IMAGES.getValue()), ExportViewType.class
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
    static ExportViewType unknown(String value) {
        return new $Unknown(value);
    }

    /**
     * Returns an unmodifiable list containing the constants of this enum type, in the order declared.
     */
    static List<ExportViewType> values() {
        return $TYPES;
    }

    /**
     * Returns a {@link ExportViewType} constant with the specified value.
     *
     * @param value value to create {@code ExportViewType} from.
     * @throws IllegalArgumentException if value does not match a known value.
     */
    static ExportViewType from(String value) {
        return switch (value) {
            case "NEW_IMAGE" -> NEW_IMAGE;
            case "NEW_AND_OLD_IMAGES" -> NEW_AND_OLD_IMAGES;
            default -> throw new IllegalArgumentException("Unknown value: " + value);
        };
    }

    final class NewImageType implements ExportViewType {
        private NewImageType() {}

        @Override
        public String getValue() {
            return "NEW_IMAGE";
        }

        @Override
        public String toString() {
            return ToStringSerializer.serialize(this);
        }

    }

    final class NewAndOldImagesType implements ExportViewType {
        private NewAndOldImagesType() {}

        @Override
        public String getValue() {
            return "NEW_AND_OLD_IMAGES";
        }

        @Override
        public String toString() {
            return ToStringSerializer.serialize(this);
        }

    }

    record $Unknown(String value) implements ExportViewType {
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

        private final class $Hidden implements ExportViewType {
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
     * Builder for {@link ExportViewType}.
     */
    final class Builder implements ShapeBuilder<ExportViewType> {
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
        public ExportViewType build() {
            return switch (value) {
                case "NEW_IMAGE" -> NEW_IMAGE;
                case "NEW_AND_OLD_IMAGES" -> NEW_AND_OLD_IMAGES;
                default -> new $Unknown(value);
            };
        }

        @Override
        public Builder deserialize(ShapeDeserializer de) {
            return value(de.readString($SCHEMA));
        }
    }
}
