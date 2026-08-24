package software.amazon.awssdk.benchmark.smithyjava.cloudwatch.model;

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
public sealed interface RecentlyActive extends SmithyEnum, SerializableShape {
    RecentlyActive PT3_H = new Pt3HType();
    List<RecentlyActive> $TYPES = List.of(PT3_H);

    Schema $SCHEMA = Schema.createEnum(ShapeId.from("com.amazonaws.cloudwatch#RecentlyActive"),
        Set.of(PT3_H.getValue()), RecentlyActive.class
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
    static RecentlyActive unknown(String value) {
        return new $Unknown(value);
    }

    /**
     * Returns an unmodifiable list containing the constants of this enum type, in the order declared.
     */
    static List<RecentlyActive> values() {
        return $TYPES;
    }

    /**
     * Returns a {@link RecentlyActive} constant with the specified value.
     *
     * @param value value to create {@code RecentlyActive} from.
     * @throws IllegalArgumentException if value does not match a known value.
     */
    static RecentlyActive from(String value) {
        return switch (value) {
            case "PT3H" -> PT3_H;
            default -> throw new IllegalArgumentException("Unknown value: " + value);
        };
    }

    final class Pt3HType implements RecentlyActive {
        private Pt3HType() {}

        @Override
        public String getValue() {
            return "PT3H";
        }

        @Override
        public String toString() {
            return ToStringSerializer.serialize(this);
        }

    }

    record $Unknown(String value) implements RecentlyActive {
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

        private final class $Hidden implements RecentlyActive {
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
     * Builder for {@link RecentlyActive}.
     */
    final class Builder implements ShapeBuilder<RecentlyActive> {
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
        public RecentlyActive build() {
            return switch (value) {
                case "PT3H" -> PT3_H;
                default -> new $Unknown(value);
            };
        }

        @Override
        public Builder deserialize(ShapeDeserializer de) {
            return value(de.readString($SCHEMA));
        }
    }
}
