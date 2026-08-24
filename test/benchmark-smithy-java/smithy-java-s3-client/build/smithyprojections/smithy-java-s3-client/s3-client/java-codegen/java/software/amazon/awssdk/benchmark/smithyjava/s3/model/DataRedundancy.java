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
public sealed interface DataRedundancy extends SmithyEnum, SerializableShape {
    DataRedundancy SINGLE_AVAILABILITY_ZONE = new SingleAvailabilityZoneType();
    DataRedundancy SINGLE_LOCAL_ZONE = new SingleLocalZoneType();
    List<DataRedundancy> $TYPES = List.of(SINGLE_AVAILABILITY_ZONE, SINGLE_LOCAL_ZONE);

    Schema $SCHEMA = Schema.createEnum(ShapeId.from("com.amazonaws.s3#DataRedundancy"),
        Set.of(SINGLE_AVAILABILITY_ZONE.getValue(), SINGLE_LOCAL_ZONE.getValue()), DataRedundancy.class
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
    static DataRedundancy unknown(String value) {
        return new $Unknown(value);
    }

    /**
     * Returns an unmodifiable list containing the constants of this enum type, in the order declared.
     */
    static List<DataRedundancy> values() {
        return $TYPES;
    }

    /**
     * Returns a {@link DataRedundancy} constant with the specified value.
     *
     * @param value value to create {@code DataRedundancy} from.
     * @throws IllegalArgumentException if value does not match a known value.
     */
    static DataRedundancy from(String value) {
        return switch (value) {
            case "SingleAvailabilityZone" -> SINGLE_AVAILABILITY_ZONE;
            case "SingleLocalZone" -> SINGLE_LOCAL_ZONE;
            default -> throw new IllegalArgumentException("Unknown value: " + value);
        };
    }

    final class SingleAvailabilityZoneType implements DataRedundancy {
        private SingleAvailabilityZoneType() {}

        @Override
        public String getValue() {
            return "SingleAvailabilityZone";
        }

        @Override
        public String toString() {
            return ToStringSerializer.serialize(this);
        }

    }

    final class SingleLocalZoneType implements DataRedundancy {
        private SingleLocalZoneType() {}

        @Override
        public String getValue() {
            return "SingleLocalZone";
        }

        @Override
        public String toString() {
            return ToStringSerializer.serialize(this);
        }

    }

    record $Unknown(String value) implements DataRedundancy {
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

        private final class $Hidden implements DataRedundancy {
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
     * Builder for {@link DataRedundancy}.
     */
    final class Builder implements ShapeBuilder<DataRedundancy> {
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
        public DataRedundancy build() {
            return switch (value) {
                case "SingleAvailabilityZone" -> SINGLE_AVAILABILITY_ZONE;
                case "SingleLocalZone" -> SINGLE_LOCAL_ZONE;
                default -> new $Unknown(value);
            };
        }

        @Override
        public Builder deserialize(ShapeDeserializer de) {
            return value(de.readString($SCHEMA));
        }
    }
}
