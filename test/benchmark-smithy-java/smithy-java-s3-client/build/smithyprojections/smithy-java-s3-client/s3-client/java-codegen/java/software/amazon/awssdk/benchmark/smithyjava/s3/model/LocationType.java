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
public sealed interface LocationType extends SmithyEnum, SerializableShape {
    LocationType AVAILABILITY_ZONE = new AvailabilityZoneType();
    LocationType LOCAL_ZONE = new LocalZoneType();
    List<LocationType> $TYPES = List.of(AVAILABILITY_ZONE, LOCAL_ZONE);

    Schema $SCHEMA = Schema.createEnum(ShapeId.from("com.amazonaws.s3#LocationType"),
        Set.of(AVAILABILITY_ZONE.getValue(), LOCAL_ZONE.getValue()), LocationType.class
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
    static LocationType unknown(String value) {
        return new $Unknown(value);
    }

    /**
     * Returns an unmodifiable list containing the constants of this enum type, in the order declared.
     */
    static List<LocationType> values() {
        return $TYPES;
    }

    /**
     * Returns a {@link LocationType} constant with the specified value.
     *
     * @param value value to create {@code LocationType} from.
     * @throws IllegalArgumentException if value does not match a known value.
     */
    static LocationType from(String value) {
        return switch (value) {
            case "AvailabilityZone" -> AVAILABILITY_ZONE;
            case "LocalZone" -> LOCAL_ZONE;
            default -> throw new IllegalArgumentException("Unknown value: " + value);
        };
    }

    final class AvailabilityZoneType implements LocationType {
        private AvailabilityZoneType() {}

        @Override
        public String getValue() {
            return "AvailabilityZone";
        }

        @Override
        public String toString() {
            return ToStringSerializer.serialize(this);
        }

    }

    final class LocalZoneType implements LocationType {
        private LocalZoneType() {}

        @Override
        public String getValue() {
            return "LocalZone";
        }

        @Override
        public String toString() {
            return ToStringSerializer.serialize(this);
        }

    }

    record $Unknown(String value) implements LocationType {
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

        private final class $Hidden implements LocationType {
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
     * Builder for {@link LocationType}.
     */
    final class Builder implements ShapeBuilder<LocationType> {
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
        public LocationType build() {
            return switch (value) {
                case "AvailabilityZone" -> AVAILABILITY_ZONE;
                case "LocalZone" -> LOCAL_ZONE;
                default -> new $Unknown(value);
            };
        }

        @Override
        public Builder deserialize(ShapeDeserializer de) {
            return value(de.readString($SCHEMA));
        }
    }
}
