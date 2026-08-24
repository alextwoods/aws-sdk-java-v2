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
import software.amazon.smithy.model.traits.LengthTrait;
import software.amazon.smithy.utils.SmithyGenerated;

@SmithyGenerated
public sealed interface MetricStreamOutputFormat extends SmithyEnum, SerializableShape {
    MetricStreamOutputFormat JSON = new JsonType();
    MetricStreamOutputFormat OPEN_TELEMETRY_0_7 = new OpenTelemetry07Type();
    MetricStreamOutputFormat OPEN_TELEMETRY_1_0 = new OpenTelemetry10Type();
    List<MetricStreamOutputFormat> $TYPES = List.of(JSON, OPEN_TELEMETRY_0_7, OPEN_TELEMETRY_1_0);

    Schema $SCHEMA = Schema.createEnum(ShapeId.from("com.amazonaws.cloudwatch#MetricStreamOutputFormat"),
        Set.of(JSON.getValue(), OPEN_TELEMETRY_0_7.getValue(), OPEN_TELEMETRY_1_0.getValue()), MetricStreamOutputFormat.class,
            LengthTrait.builder().min(1L).max(255L).build()
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
    static MetricStreamOutputFormat unknown(String value) {
        return new $Unknown(value);
    }

    /**
     * Returns an unmodifiable list containing the constants of this enum type, in the order declared.
     */
    static List<MetricStreamOutputFormat> values() {
        return $TYPES;
    }

    /**
     * Returns a {@link MetricStreamOutputFormat} constant with the specified value.
     *
     * @param value value to create {@code MetricStreamOutputFormat} from.
     * @throws IllegalArgumentException if value does not match a known value.
     */
    static MetricStreamOutputFormat from(String value) {
        return switch (value) {
            case "json" -> JSON;
            case "opentelemetry0.7" -> OPEN_TELEMETRY_0_7;
            case "opentelemetry1.0" -> OPEN_TELEMETRY_1_0;
            default -> throw new IllegalArgumentException("Unknown value: " + value);
        };
    }

    final class JsonType implements MetricStreamOutputFormat {
        private JsonType() {}

        @Override
        public String getValue() {
            return "json";
        }

        @Override
        public String toString() {
            return ToStringSerializer.serialize(this);
        }

    }

    final class OpenTelemetry07Type implements MetricStreamOutputFormat {
        private OpenTelemetry07Type() {}

        @Override
        public String getValue() {
            return "opentelemetry0.7";
        }

        @Override
        public String toString() {
            return ToStringSerializer.serialize(this);
        }

    }

    final class OpenTelemetry10Type implements MetricStreamOutputFormat {
        private OpenTelemetry10Type() {}

        @Override
        public String getValue() {
            return "opentelemetry1.0";
        }

        @Override
        public String toString() {
            return ToStringSerializer.serialize(this);
        }

    }

    record $Unknown(String value) implements MetricStreamOutputFormat {
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

        private final class $Hidden implements MetricStreamOutputFormat {
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
     * Builder for {@link MetricStreamOutputFormat}.
     */
    final class Builder implements ShapeBuilder<MetricStreamOutputFormat> {
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
        public MetricStreamOutputFormat build() {
            return switch (value) {
                case "json" -> JSON;
                case "opentelemetry0.7" -> OPEN_TELEMETRY_0_7;
                case "opentelemetry1.0" -> OPEN_TELEMETRY_1_0;
                default -> new $Unknown(value);
            };
        }

        @Override
        public Builder deserialize(ShapeDeserializer de) {
            return value(de.readString($SCHEMA));
        }
    }
}
