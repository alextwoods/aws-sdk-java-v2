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
public sealed interface Tier extends SmithyEnum, SerializableShape {
    Tier STANDARD = new StandardType();
    Tier BULK = new BulkType();
    Tier EXPEDITED = new ExpeditedType();
    List<Tier> $TYPES = List.of(STANDARD, BULK, EXPEDITED);

    Schema $SCHEMA = Schema.createEnum(ShapeId.from("com.amazonaws.s3#Tier"),
        Set.of(STANDARD.getValue(), BULK.getValue(), EXPEDITED.getValue()), Tier.class
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
    static Tier unknown(String value) {
        return new $Unknown(value);
    }

    /**
     * Returns an unmodifiable list containing the constants of this enum type, in the order declared.
     */
    static List<Tier> values() {
        return $TYPES;
    }

    /**
     * Returns a {@link Tier} constant with the specified value.
     *
     * @param value value to create {@code Tier} from.
     * @throws IllegalArgumentException if value does not match a known value.
     */
    static Tier from(String value) {
        return switch (value) {
            case "Standard" -> STANDARD;
            case "Bulk" -> BULK;
            case "Expedited" -> EXPEDITED;
            default -> throw new IllegalArgumentException("Unknown value: " + value);
        };
    }

    final class StandardType implements Tier {
        private StandardType() {}

        @Override
        public String getValue() {
            return "Standard";
        }

        @Override
        public String toString() {
            return ToStringSerializer.serialize(this);
        }

    }

    final class BulkType implements Tier {
        private BulkType() {}

        @Override
        public String getValue() {
            return "Bulk";
        }

        @Override
        public String toString() {
            return ToStringSerializer.serialize(this);
        }

    }

    final class ExpeditedType implements Tier {
        private ExpeditedType() {}

        @Override
        public String getValue() {
            return "Expedited";
        }

        @Override
        public String toString() {
            return ToStringSerializer.serialize(this);
        }

    }

    record $Unknown(String value) implements Tier {
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

        private final class $Hidden implements Tier {
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
     * Builder for {@link Tier}.
     */
    final class Builder implements ShapeBuilder<Tier> {
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
        public Tier build() {
            return switch (value) {
                case "Standard" -> STANDARD;
                case "Bulk" -> BULK;
                case "Expedited" -> EXPEDITED;
                default -> new $Unknown(value);
            };
        }

        @Override
        public Builder deserialize(ShapeDeserializer de) {
            return value(de.readString($SCHEMA));
        }
    }
}
