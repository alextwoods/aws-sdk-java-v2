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
public sealed interface ReturnItemCollectionMetrics extends SmithyEnum, SerializableShape {
    ReturnItemCollectionMetrics SIZE = new SizeType();
    ReturnItemCollectionMetrics NONE = new NoneType();
    List<ReturnItemCollectionMetrics> $TYPES = List.of(SIZE, NONE);

    Schema $SCHEMA = Schema.createEnum(ShapeId.from("com.amazonaws.dynamodb#ReturnItemCollectionMetrics"),
        Set.of(SIZE.getValue(), NONE.getValue()), ReturnItemCollectionMetrics.class
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
    static ReturnItemCollectionMetrics unknown(String value) {
        return new $Unknown(value);
    }

    /**
     * Returns an unmodifiable list containing the constants of this enum type, in the order declared.
     */
    static List<ReturnItemCollectionMetrics> values() {
        return $TYPES;
    }

    /**
     * Returns a {@link ReturnItemCollectionMetrics} constant with the specified value.
     *
     * @param value value to create {@code ReturnItemCollectionMetrics} from.
     * @throws IllegalArgumentException if value does not match a known value.
     */
    static ReturnItemCollectionMetrics from(String value) {
        return switch (value) {
            case "SIZE" -> SIZE;
            case "NONE" -> NONE;
            default -> throw new IllegalArgumentException("Unknown value: " + value);
        };
    }

    final class SizeType implements ReturnItemCollectionMetrics {
        private SizeType() {}

        @Override
        public String getValue() {
            return "SIZE";
        }

        @Override
        public String toString() {
            return ToStringSerializer.serialize(this);
        }

    }

    final class NoneType implements ReturnItemCollectionMetrics {
        private NoneType() {}

        @Override
        public String getValue() {
            return "NONE";
        }

        @Override
        public String toString() {
            return ToStringSerializer.serialize(this);
        }

    }

    record $Unknown(String value) implements ReturnItemCollectionMetrics {
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

        private final class $Hidden implements ReturnItemCollectionMetrics {
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
     * Builder for {@link ReturnItemCollectionMetrics}.
     */
    final class Builder implements ShapeBuilder<ReturnItemCollectionMetrics> {
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
        public ReturnItemCollectionMetrics build() {
            return switch (value) {
                case "SIZE" -> SIZE;
                case "NONE" -> NONE;
                default -> new $Unknown(value);
            };
        }

        @Override
        public Builder deserialize(ShapeDeserializer de) {
            return value(de.readString($SCHEMA));
        }
    }
}
