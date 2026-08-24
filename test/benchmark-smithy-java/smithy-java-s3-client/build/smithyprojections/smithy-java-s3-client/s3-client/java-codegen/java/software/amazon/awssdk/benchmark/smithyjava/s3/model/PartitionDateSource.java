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
public sealed interface PartitionDateSource extends SmithyEnum, SerializableShape {
    PartitionDateSource EVENT_TIME = new EventTimeType();
    PartitionDateSource DELIVERY_TIME = new DeliveryTimeType();
    List<PartitionDateSource> $TYPES = List.of(EVENT_TIME, DELIVERY_TIME);

    Schema $SCHEMA = Schema.createEnum(ShapeId.from("com.amazonaws.s3#PartitionDateSource"),
        Set.of(EVENT_TIME.getValue(), DELIVERY_TIME.getValue()), PartitionDateSource.class
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
    static PartitionDateSource unknown(String value) {
        return new $Unknown(value);
    }

    /**
     * Returns an unmodifiable list containing the constants of this enum type, in the order declared.
     */
    static List<PartitionDateSource> values() {
        return $TYPES;
    }

    /**
     * Returns a {@link PartitionDateSource} constant with the specified value.
     *
     * @param value value to create {@code PartitionDateSource} from.
     * @throws IllegalArgumentException if value does not match a known value.
     */
    static PartitionDateSource from(String value) {
        return switch (value) {
            case "EventTime" -> EVENT_TIME;
            case "DeliveryTime" -> DELIVERY_TIME;
            default -> throw new IllegalArgumentException("Unknown value: " + value);
        };
    }

    final class EventTimeType implements PartitionDateSource {
        private EventTimeType() {}

        @Override
        public String getValue() {
            return "EventTime";
        }

        @Override
        public String toString() {
            return ToStringSerializer.serialize(this);
        }

    }

    final class DeliveryTimeType implements PartitionDateSource {
        private DeliveryTimeType() {}

        @Override
        public String getValue() {
            return "DeliveryTime";
        }

        @Override
        public String toString() {
            return ToStringSerializer.serialize(this);
        }

    }

    record $Unknown(String value) implements PartitionDateSource {
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

        private final class $Hidden implements PartitionDateSource {
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
     * Builder for {@link PartitionDateSource}.
     */
    final class Builder implements ShapeBuilder<PartitionDateSource> {
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
        public PartitionDateSource build() {
            return switch (value) {
                case "EventTime" -> EVENT_TIME;
                case "DeliveryTime" -> DELIVERY_TIME;
                default -> new $Unknown(value);
            };
        }

        @Override
        public Builder deserialize(ShapeDeserializer de) {
            return value(de.readString($SCHEMA));
        }
    }
}
