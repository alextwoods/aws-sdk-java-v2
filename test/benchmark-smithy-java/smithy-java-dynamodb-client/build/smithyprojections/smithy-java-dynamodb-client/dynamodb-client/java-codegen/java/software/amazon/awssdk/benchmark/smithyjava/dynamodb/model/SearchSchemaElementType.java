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
public sealed interface SearchSchemaElementType extends SmithyEnum, SerializableShape {
    SearchSchemaElementType HASH = new HashType();
    SearchSchemaElementType INLINE_FILTER = new InlineFilterType();
    List<SearchSchemaElementType> $TYPES = List.of(HASH, INLINE_FILTER);

    Schema $SCHEMA = Schema.createEnum(ShapeId.from("com.amazonaws.dynamodb#SearchSchemaElementType"),
        Set.of(HASH.getValue(), INLINE_FILTER.getValue()), SearchSchemaElementType.class
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
    static SearchSchemaElementType unknown(String value) {
        return new $Unknown(value);
    }

    /**
     * Returns an unmodifiable list containing the constants of this enum type, in the order declared.
     */
    static List<SearchSchemaElementType> values() {
        return $TYPES;
    }

    /**
     * Returns a {@link SearchSchemaElementType} constant with the specified value.
     *
     * @param value value to create {@code SearchSchemaElementType} from.
     * @throws IllegalArgumentException if value does not match a known value.
     */
    static SearchSchemaElementType from(String value) {
        return switch (value) {
            case "HASH" -> HASH;
            case "INLINE_FILTER" -> INLINE_FILTER;
            default -> throw new IllegalArgumentException("Unknown value: " + value);
        };
    }

    final class HashType implements SearchSchemaElementType {
        private HashType() {}

        @Override
        public String getValue() {
            return "HASH";
        }

        @Override
        public String toString() {
            return ToStringSerializer.serialize(this);
        }

    }

    final class InlineFilterType implements SearchSchemaElementType {
        private InlineFilterType() {}

        @Override
        public String getValue() {
            return "INLINE_FILTER";
        }

        @Override
        public String toString() {
            return ToStringSerializer.serialize(this);
        }

    }

    record $Unknown(String value) implements SearchSchemaElementType {
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

        private final class $Hidden implements SearchSchemaElementType {
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
     * Builder for {@link SearchSchemaElementType}.
     */
    final class Builder implements ShapeBuilder<SearchSchemaElementType> {
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
        public SearchSchemaElementType build() {
            return switch (value) {
                case "HASH" -> HASH;
                case "INLINE_FILTER" -> INLINE_FILTER;
                default -> new $Unknown(value);
            };
        }

        @Override
        public Builder deserialize(ShapeDeserializer de) {
            return value(de.readString($SCHEMA));
        }
    }
}
