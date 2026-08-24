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
public sealed interface ScanBy extends SmithyEnum, SerializableShape {
    ScanBy TIMESTAMP_DESCENDING = new TimestampDescendingType();
    ScanBy TIMESTAMP_ASCENDING = new TimestampAscendingType();
    List<ScanBy> $TYPES = List.of(TIMESTAMP_DESCENDING, TIMESTAMP_ASCENDING);

    Schema $SCHEMA = Schema.createEnum(ShapeId.from("com.amazonaws.cloudwatch#ScanBy"),
        Set.of(TIMESTAMP_DESCENDING.getValue(), TIMESTAMP_ASCENDING.getValue()), ScanBy.class
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
    static ScanBy unknown(String value) {
        return new $Unknown(value);
    }

    /**
     * Returns an unmodifiable list containing the constants of this enum type, in the order declared.
     */
    static List<ScanBy> values() {
        return $TYPES;
    }

    /**
     * Returns a {@link ScanBy} constant with the specified value.
     *
     * @param value value to create {@code ScanBy} from.
     * @throws IllegalArgumentException if value does not match a known value.
     */
    static ScanBy from(String value) {
        return switch (value) {
            case "TimestampDescending" -> TIMESTAMP_DESCENDING;
            case "TimestampAscending" -> TIMESTAMP_ASCENDING;
            default -> throw new IllegalArgumentException("Unknown value: " + value);
        };
    }

    final class TimestampDescendingType implements ScanBy {
        private TimestampDescendingType() {}

        @Override
        public String getValue() {
            return "TimestampDescending";
        }

        @Override
        public String toString() {
            return ToStringSerializer.serialize(this);
        }

    }

    final class TimestampAscendingType implements ScanBy {
        private TimestampAscendingType() {}

        @Override
        public String getValue() {
            return "TimestampAscending";
        }

        @Override
        public String toString() {
            return ToStringSerializer.serialize(this);
        }

    }

    record $Unknown(String value) implements ScanBy {
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

        private final class $Hidden implements ScanBy {
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
     * Builder for {@link ScanBy}.
     */
    final class Builder implements ShapeBuilder<ScanBy> {
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
        public ScanBy build() {
            return switch (value) {
                case "TimestampDescending" -> TIMESTAMP_DESCENDING;
                case "TimestampAscending" -> TIMESTAMP_ASCENDING;
                default -> new $Unknown(value);
            };
        }

        @Override
        public Builder deserialize(ShapeDeserializer de) {
            return value(de.readString($SCHEMA));
        }
    }
}
