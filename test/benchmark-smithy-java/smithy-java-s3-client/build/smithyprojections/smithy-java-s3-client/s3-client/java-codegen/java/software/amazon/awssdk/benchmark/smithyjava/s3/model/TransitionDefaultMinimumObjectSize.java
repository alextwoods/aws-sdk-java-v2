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
public sealed interface TransitionDefaultMinimumObjectSize extends SmithyEnum, SerializableShape {
    TransitionDefaultMinimumObjectSize VARIES_BY_STORAGE_CLASS = new VariesByStorageClassType();
    TransitionDefaultMinimumObjectSize ALL_STORAGE_CLASSES_128_K = new AllStorageClasses128KType();
    List<TransitionDefaultMinimumObjectSize> $TYPES = List.of(VARIES_BY_STORAGE_CLASS, ALL_STORAGE_CLASSES_128_K);

    Schema $SCHEMA = Schema.createEnum(ShapeId.from("com.amazonaws.s3#TransitionDefaultMinimumObjectSize"),
        Set.of(VARIES_BY_STORAGE_CLASS.getValue(), ALL_STORAGE_CLASSES_128_K.getValue()), TransitionDefaultMinimumObjectSize.class
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
    static TransitionDefaultMinimumObjectSize unknown(String value) {
        return new $Unknown(value);
    }

    /**
     * Returns an unmodifiable list containing the constants of this enum type, in the order declared.
     */
    static List<TransitionDefaultMinimumObjectSize> values() {
        return $TYPES;
    }

    /**
     * Returns a {@link TransitionDefaultMinimumObjectSize} constant with the specified value.
     *
     * @param value value to create {@code TransitionDefaultMinimumObjectSize} from.
     * @throws IllegalArgumentException if value does not match a known value.
     */
    static TransitionDefaultMinimumObjectSize from(String value) {
        return switch (value) {
            case "varies_by_storage_class" -> VARIES_BY_STORAGE_CLASS;
            case "all_storage_classes_128K" -> ALL_STORAGE_CLASSES_128_K;
            default -> throw new IllegalArgumentException("Unknown value: " + value);
        };
    }

    final class VariesByStorageClassType implements TransitionDefaultMinimumObjectSize {
        private VariesByStorageClassType() {}

        @Override
        public String getValue() {
            return "varies_by_storage_class";
        }

        @Override
        public String toString() {
            return ToStringSerializer.serialize(this);
        }

    }

    final class AllStorageClasses128KType implements TransitionDefaultMinimumObjectSize {
        private AllStorageClasses128KType() {}

        @Override
        public String getValue() {
            return "all_storage_classes_128K";
        }

        @Override
        public String toString() {
            return ToStringSerializer.serialize(this);
        }

    }

    record $Unknown(String value) implements TransitionDefaultMinimumObjectSize {
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

        private final class $Hidden implements TransitionDefaultMinimumObjectSize {
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
     * Builder for {@link TransitionDefaultMinimumObjectSize}.
     */
    final class Builder implements ShapeBuilder<TransitionDefaultMinimumObjectSize> {
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
        public TransitionDefaultMinimumObjectSize build() {
            return switch (value) {
                case "varies_by_storage_class" -> VARIES_BY_STORAGE_CLASS;
                case "all_storage_classes_128K" -> ALL_STORAGE_CLASSES_128_K;
                default -> new $Unknown(value);
            };
        }

        @Override
        public Builder deserialize(ShapeDeserializer de) {
            return value(de.readString($SCHEMA));
        }
    }
}
