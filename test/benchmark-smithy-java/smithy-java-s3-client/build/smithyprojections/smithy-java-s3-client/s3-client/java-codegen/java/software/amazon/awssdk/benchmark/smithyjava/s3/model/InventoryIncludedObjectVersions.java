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
public sealed interface InventoryIncludedObjectVersions extends SmithyEnum, SerializableShape {
    InventoryIncludedObjectVersions ALL = new AllType();
    InventoryIncludedObjectVersions CURRENT = new CurrentType();
    List<InventoryIncludedObjectVersions> $TYPES = List.of(ALL, CURRENT);

    Schema $SCHEMA = Schema.createEnum(ShapeId.from("com.amazonaws.s3#InventoryIncludedObjectVersions"),
        Set.of(ALL.getValue(), CURRENT.getValue()), InventoryIncludedObjectVersions.class
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
    static InventoryIncludedObjectVersions unknown(String value) {
        return new $Unknown(value);
    }

    /**
     * Returns an unmodifiable list containing the constants of this enum type, in the order declared.
     */
    static List<InventoryIncludedObjectVersions> values() {
        return $TYPES;
    }

    /**
     * Returns a {@link InventoryIncludedObjectVersions} constant with the specified value.
     *
     * @param value value to create {@code InventoryIncludedObjectVersions} from.
     * @throws IllegalArgumentException if value does not match a known value.
     */
    static InventoryIncludedObjectVersions from(String value) {
        return switch (value) {
            case "All" -> ALL;
            case "Current" -> CURRENT;
            default -> throw new IllegalArgumentException("Unknown value: " + value);
        };
    }

    final class AllType implements InventoryIncludedObjectVersions {
        private AllType() {}

        @Override
        public String getValue() {
            return "All";
        }

        @Override
        public String toString() {
            return ToStringSerializer.serialize(this);
        }

    }

    final class CurrentType implements InventoryIncludedObjectVersions {
        private CurrentType() {}

        @Override
        public String getValue() {
            return "Current";
        }

        @Override
        public String toString() {
            return ToStringSerializer.serialize(this);
        }

    }

    record $Unknown(String value) implements InventoryIncludedObjectVersions {
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

        private final class $Hidden implements InventoryIncludedObjectVersions {
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
     * Builder for {@link InventoryIncludedObjectVersions}.
     */
    final class Builder implements ShapeBuilder<InventoryIncludedObjectVersions> {
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
        public InventoryIncludedObjectVersions build() {
            return switch (value) {
                case "All" -> ALL;
                case "Current" -> CURRENT;
                default -> new $Unknown(value);
            };
        }

        @Override
        public Builder deserialize(ShapeDeserializer de) {
            return value(de.readString($SCHEMA));
        }
    }
}
