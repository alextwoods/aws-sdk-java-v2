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
public sealed interface VectorDistanceFunction extends SmithyEnum, SerializableShape {
    VectorDistanceFunction COSINE = new CosineType();
    VectorDistanceFunction DOT_PRODUCT = new DotProductType();
    VectorDistanceFunction EUCLIDEAN = new EuclideanType();
    List<VectorDistanceFunction> $TYPES = List.of(COSINE, DOT_PRODUCT, EUCLIDEAN);

    Schema $SCHEMA = Schema.createEnum(ShapeId.from("com.amazonaws.dynamodb#VectorDistanceFunction"),
        Set.of(COSINE.getValue(), DOT_PRODUCT.getValue(), EUCLIDEAN.getValue()), VectorDistanceFunction.class
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
    static VectorDistanceFunction unknown(String value) {
        return new $Unknown(value);
    }

    /**
     * Returns an unmodifiable list containing the constants of this enum type, in the order declared.
     */
    static List<VectorDistanceFunction> values() {
        return $TYPES;
    }

    /**
     * Returns a {@link VectorDistanceFunction} constant with the specified value.
     *
     * @param value value to create {@code VectorDistanceFunction} from.
     * @throws IllegalArgumentException if value does not match a known value.
     */
    static VectorDistanceFunction from(String value) {
        return switch (value) {
            case "COSINE" -> COSINE;
            case "DOT_PRODUCT" -> DOT_PRODUCT;
            case "EUCLIDEAN" -> EUCLIDEAN;
            default -> throw new IllegalArgumentException("Unknown value: " + value);
        };
    }

    final class CosineType implements VectorDistanceFunction {
        private CosineType() {}

        @Override
        public String getValue() {
            return "COSINE";
        }

        @Override
        public String toString() {
            return ToStringSerializer.serialize(this);
        }

    }

    final class DotProductType implements VectorDistanceFunction {
        private DotProductType() {}

        @Override
        public String getValue() {
            return "DOT_PRODUCT";
        }

        @Override
        public String toString() {
            return ToStringSerializer.serialize(this);
        }

    }

    final class EuclideanType implements VectorDistanceFunction {
        private EuclideanType() {}

        @Override
        public String getValue() {
            return "EUCLIDEAN";
        }

        @Override
        public String toString() {
            return ToStringSerializer.serialize(this);
        }

    }

    record $Unknown(String value) implements VectorDistanceFunction {
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

        private final class $Hidden implements VectorDistanceFunction {
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
     * Builder for {@link VectorDistanceFunction}.
     */
    final class Builder implements ShapeBuilder<VectorDistanceFunction> {
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
        public VectorDistanceFunction build() {
            return switch (value) {
                case "COSINE" -> COSINE;
                case "DOT_PRODUCT" -> DOT_PRODUCT;
                case "EUCLIDEAN" -> EUCLIDEAN;
                default -> new $Unknown(value);
            };
        }

        @Override
        public Builder deserialize(ShapeDeserializer de) {
            return value(de.readString($SCHEMA));
        }
    }
}
