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
public sealed interface MultiRegionConsistency extends SmithyEnum, SerializableShape {
    MultiRegionConsistency EVENTUAL = new EventualType();
    MultiRegionConsistency STRONG = new StrongType();
    List<MultiRegionConsistency> $TYPES = List.of(EVENTUAL, STRONG);

    Schema $SCHEMA = Schema.createEnum(ShapeId.from("com.amazonaws.dynamodb#MultiRegionConsistency"),
        Set.of(EVENTUAL.getValue(), STRONG.getValue()), MultiRegionConsistency.class
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
    static MultiRegionConsistency unknown(String value) {
        return new $Unknown(value);
    }

    /**
     * Returns an unmodifiable list containing the constants of this enum type, in the order declared.
     */
    static List<MultiRegionConsistency> values() {
        return $TYPES;
    }

    /**
     * Returns a {@link MultiRegionConsistency} constant with the specified value.
     *
     * @param value value to create {@code MultiRegionConsistency} from.
     * @throws IllegalArgumentException if value does not match a known value.
     */
    static MultiRegionConsistency from(String value) {
        return switch (value) {
            case "EVENTUAL" -> EVENTUAL;
            case "STRONG" -> STRONG;
            default -> throw new IllegalArgumentException("Unknown value: " + value);
        };
    }

    final class EventualType implements MultiRegionConsistency {
        private EventualType() {}

        @Override
        public String getValue() {
            return "EVENTUAL";
        }

        @Override
        public String toString() {
            return ToStringSerializer.serialize(this);
        }

    }

    final class StrongType implements MultiRegionConsistency {
        private StrongType() {}

        @Override
        public String getValue() {
            return "STRONG";
        }

        @Override
        public String toString() {
            return ToStringSerializer.serialize(this);
        }

    }

    record $Unknown(String value) implements MultiRegionConsistency {
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

        private final class $Hidden implements MultiRegionConsistency {
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
     * Builder for {@link MultiRegionConsistency}.
     */
    final class Builder implements ShapeBuilder<MultiRegionConsistency> {
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
        public MultiRegionConsistency build() {
            return switch (value) {
                case "EVENTUAL" -> EVENTUAL;
                case "STRONG" -> STRONG;
                default -> new $Unknown(value);
            };
        }

        @Override
        public Builder deserialize(ShapeDeserializer de) {
            return value(de.readString($SCHEMA));
        }
    }
}
