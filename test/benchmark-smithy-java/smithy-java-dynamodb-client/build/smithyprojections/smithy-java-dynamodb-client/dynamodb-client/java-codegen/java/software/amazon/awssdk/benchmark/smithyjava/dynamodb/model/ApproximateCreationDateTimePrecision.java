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
public sealed interface ApproximateCreationDateTimePrecision extends SmithyEnum, SerializableShape {
    ApproximateCreationDateTimePrecision MILLISECOND = new MillisecondType();
    ApproximateCreationDateTimePrecision MICROSECOND = new MicrosecondType();
    List<ApproximateCreationDateTimePrecision> $TYPES = List.of(MILLISECOND, MICROSECOND);

    Schema $SCHEMA = Schema.createEnum(ShapeId.from("com.amazonaws.dynamodb#ApproximateCreationDateTimePrecision"),
        Set.of(MILLISECOND.getValue(), MICROSECOND.getValue()), ApproximateCreationDateTimePrecision.class
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
    static ApproximateCreationDateTimePrecision unknown(String value) {
        return new $Unknown(value);
    }

    /**
     * Returns an unmodifiable list containing the constants of this enum type, in the order declared.
     */
    static List<ApproximateCreationDateTimePrecision> values() {
        return $TYPES;
    }

    /**
     * Returns a {@link ApproximateCreationDateTimePrecision} constant with the specified value.
     *
     * @param value value to create {@code ApproximateCreationDateTimePrecision} from.
     * @throws IllegalArgumentException if value does not match a known value.
     */
    static ApproximateCreationDateTimePrecision from(String value) {
        return switch (value) {
            case "MILLISECOND" -> MILLISECOND;
            case "MICROSECOND" -> MICROSECOND;
            default -> throw new IllegalArgumentException("Unknown value: " + value);
        };
    }

    final class MillisecondType implements ApproximateCreationDateTimePrecision {
        private MillisecondType() {}

        @Override
        public String getValue() {
            return "MILLISECOND";
        }

        @Override
        public String toString() {
            return ToStringSerializer.serialize(this);
        }

    }

    final class MicrosecondType implements ApproximateCreationDateTimePrecision {
        private MicrosecondType() {}

        @Override
        public String getValue() {
            return "MICROSECOND";
        }

        @Override
        public String toString() {
            return ToStringSerializer.serialize(this);
        }

    }

    record $Unknown(String value) implements ApproximateCreationDateTimePrecision {
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

        private final class $Hidden implements ApproximateCreationDateTimePrecision {
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
     * Builder for {@link ApproximateCreationDateTimePrecision}.
     */
    final class Builder implements ShapeBuilder<ApproximateCreationDateTimePrecision> {
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
        public ApproximateCreationDateTimePrecision build() {
            return switch (value) {
                case "MILLISECOND" -> MILLISECOND;
                case "MICROSECOND" -> MICROSECOND;
                default -> new $Unknown(value);
            };
        }

        @Override
        public Builder deserialize(ShapeDeserializer de) {
            return value(de.readString($SCHEMA));
        }
    }
}
