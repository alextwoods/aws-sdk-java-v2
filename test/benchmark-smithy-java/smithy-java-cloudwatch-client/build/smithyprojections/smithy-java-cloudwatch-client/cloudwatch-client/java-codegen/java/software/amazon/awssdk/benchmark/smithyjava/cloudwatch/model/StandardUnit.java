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
public sealed interface StandardUnit extends SmithyEnum, SerializableShape {
    StandardUnit SECONDS = new SecondsType();
    StandardUnit MICROSECONDS = new MicrosecondsType();
    StandardUnit MILLISECONDS = new MillisecondsType();
    StandardUnit BYTES = new BytesType();
    StandardUnit KILOBYTES = new KilobytesType();
    StandardUnit MEGABYTES = new MegabytesType();
    StandardUnit GIGABYTES = new GigabytesType();
    StandardUnit TERABYTES = new TerabytesType();
    StandardUnit BITS = new BitsType();
    StandardUnit KILOBITS = new KilobitsType();
    StandardUnit MEGABITS = new MegabitsType();
    StandardUnit GIGABITS = new GigabitsType();
    StandardUnit TERABITS = new TerabitsType();
    StandardUnit PERCENT = new PercentType();
    StandardUnit COUNT = new CountType();
    StandardUnit BYTES_SECOND = new BytesSecondType();
    StandardUnit KILOBYTES_SECOND = new KilobytesSecondType();
    StandardUnit MEGABYTES_SECOND = new MegabytesSecondType();
    StandardUnit GIGABYTES_SECOND = new GigabytesSecondType();
    StandardUnit TERABYTES_SECOND = new TerabytesSecondType();
    StandardUnit BITS_SECOND = new BitsSecondType();
    StandardUnit KILOBITS_SECOND = new KilobitsSecondType();
    StandardUnit MEGABITS_SECOND = new MegabitsSecondType();
    StandardUnit GIGABITS_SECOND = new GigabitsSecondType();
    StandardUnit TERABITS_SECOND = new TerabitsSecondType();
    StandardUnit COUNT_SECOND = new CountSecondType();
    StandardUnit NONE = new NoneType();
    List<StandardUnit> $TYPES = List.of(SECONDS, MICROSECONDS, MILLISECONDS, BYTES, KILOBYTES, MEGABYTES, GIGABYTES, TERABYTES, BITS, KILOBITS, MEGABITS, GIGABITS, TERABITS, PERCENT, COUNT, BYTES_SECOND, KILOBYTES_SECOND, MEGABYTES_SECOND, GIGABYTES_SECOND, TERABYTES_SECOND, BITS_SECOND, KILOBITS_SECOND, MEGABITS_SECOND, GIGABITS_SECOND, TERABITS_SECOND, COUNT_SECOND, NONE);

    Schema $SCHEMA = Schema.createEnum(ShapeId.from("com.amazonaws.cloudwatch#StandardUnit"),
        Set.of(SECONDS.getValue(), MICROSECONDS.getValue(), MILLISECONDS.getValue(), BYTES.getValue(), KILOBYTES.getValue(), MEGABYTES.getValue(), GIGABYTES.getValue(), TERABYTES.getValue(), BITS.getValue(), KILOBITS.getValue(), MEGABITS.getValue(), GIGABITS.getValue(), TERABITS.getValue(), PERCENT.getValue(), COUNT.getValue(), BYTES_SECOND.getValue(), KILOBYTES_SECOND.getValue(), MEGABYTES_SECOND.getValue(), GIGABYTES_SECOND.getValue(), TERABYTES_SECOND.getValue(), BITS_SECOND.getValue(), KILOBITS_SECOND.getValue(), MEGABITS_SECOND.getValue(), GIGABITS_SECOND.getValue(), TERABITS_SECOND.getValue(), COUNT_SECOND.getValue(), NONE.getValue()), StandardUnit.class
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
    static StandardUnit unknown(String value) {
        return new $Unknown(value);
    }

    /**
     * Returns an unmodifiable list containing the constants of this enum type, in the order declared.
     */
    static List<StandardUnit> values() {
        return $TYPES;
    }

    /**
     * Returns a {@link StandardUnit} constant with the specified value.
     *
     * @param value value to create {@code StandardUnit} from.
     * @throws IllegalArgumentException if value does not match a known value.
     */
    static StandardUnit from(String value) {
        return switch (value) {
            case "Seconds" -> SECONDS;
            case "Microseconds" -> MICROSECONDS;
            case "Milliseconds" -> MILLISECONDS;
            case "Bytes" -> BYTES;
            case "Kilobytes" -> KILOBYTES;
            case "Megabytes" -> MEGABYTES;
            case "Gigabytes" -> GIGABYTES;
            case "Terabytes" -> TERABYTES;
            case "Bits" -> BITS;
            case "Kilobits" -> KILOBITS;
            case "Megabits" -> MEGABITS;
            case "Gigabits" -> GIGABITS;
            case "Terabits" -> TERABITS;
            case "Percent" -> PERCENT;
            case "Count" -> COUNT;
            case "Bytes/Second" -> BYTES_SECOND;
            case "Kilobytes/Second" -> KILOBYTES_SECOND;
            case "Megabytes/Second" -> MEGABYTES_SECOND;
            case "Gigabytes/Second" -> GIGABYTES_SECOND;
            case "Terabytes/Second" -> TERABYTES_SECOND;
            case "Bits/Second" -> BITS_SECOND;
            case "Kilobits/Second" -> KILOBITS_SECOND;
            case "Megabits/Second" -> MEGABITS_SECOND;
            case "Gigabits/Second" -> GIGABITS_SECOND;
            case "Terabits/Second" -> TERABITS_SECOND;
            case "Count/Second" -> COUNT_SECOND;
            case "None" -> NONE;
            default -> throw new IllegalArgumentException("Unknown value: " + value);
        };
    }

    final class SecondsType implements StandardUnit {
        private SecondsType() {}

        @Override
        public String getValue() {
            return "Seconds";
        }

        @Override
        public String toString() {
            return ToStringSerializer.serialize(this);
        }

    }

    final class MicrosecondsType implements StandardUnit {
        private MicrosecondsType() {}

        @Override
        public String getValue() {
            return "Microseconds";
        }

        @Override
        public String toString() {
            return ToStringSerializer.serialize(this);
        }

    }

    final class MillisecondsType implements StandardUnit {
        private MillisecondsType() {}

        @Override
        public String getValue() {
            return "Milliseconds";
        }

        @Override
        public String toString() {
            return ToStringSerializer.serialize(this);
        }

    }

    final class BytesType implements StandardUnit {
        private BytesType() {}

        @Override
        public String getValue() {
            return "Bytes";
        }

        @Override
        public String toString() {
            return ToStringSerializer.serialize(this);
        }

    }

    final class KilobytesType implements StandardUnit {
        private KilobytesType() {}

        @Override
        public String getValue() {
            return "Kilobytes";
        }

        @Override
        public String toString() {
            return ToStringSerializer.serialize(this);
        }

    }

    final class MegabytesType implements StandardUnit {
        private MegabytesType() {}

        @Override
        public String getValue() {
            return "Megabytes";
        }

        @Override
        public String toString() {
            return ToStringSerializer.serialize(this);
        }

    }

    final class GigabytesType implements StandardUnit {
        private GigabytesType() {}

        @Override
        public String getValue() {
            return "Gigabytes";
        }

        @Override
        public String toString() {
            return ToStringSerializer.serialize(this);
        }

    }

    final class TerabytesType implements StandardUnit {
        private TerabytesType() {}

        @Override
        public String getValue() {
            return "Terabytes";
        }

        @Override
        public String toString() {
            return ToStringSerializer.serialize(this);
        }

    }

    final class BitsType implements StandardUnit {
        private BitsType() {}

        @Override
        public String getValue() {
            return "Bits";
        }

        @Override
        public String toString() {
            return ToStringSerializer.serialize(this);
        }

    }

    final class KilobitsType implements StandardUnit {
        private KilobitsType() {}

        @Override
        public String getValue() {
            return "Kilobits";
        }

        @Override
        public String toString() {
            return ToStringSerializer.serialize(this);
        }

    }

    final class MegabitsType implements StandardUnit {
        private MegabitsType() {}

        @Override
        public String getValue() {
            return "Megabits";
        }

        @Override
        public String toString() {
            return ToStringSerializer.serialize(this);
        }

    }

    final class GigabitsType implements StandardUnit {
        private GigabitsType() {}

        @Override
        public String getValue() {
            return "Gigabits";
        }

        @Override
        public String toString() {
            return ToStringSerializer.serialize(this);
        }

    }

    final class TerabitsType implements StandardUnit {
        private TerabitsType() {}

        @Override
        public String getValue() {
            return "Terabits";
        }

        @Override
        public String toString() {
            return ToStringSerializer.serialize(this);
        }

    }

    final class PercentType implements StandardUnit {
        private PercentType() {}

        @Override
        public String getValue() {
            return "Percent";
        }

        @Override
        public String toString() {
            return ToStringSerializer.serialize(this);
        }

    }

    final class CountType implements StandardUnit {
        private CountType() {}

        @Override
        public String getValue() {
            return "Count";
        }

        @Override
        public String toString() {
            return ToStringSerializer.serialize(this);
        }

    }

    final class BytesSecondType implements StandardUnit {
        private BytesSecondType() {}

        @Override
        public String getValue() {
            return "Bytes/Second";
        }

        @Override
        public String toString() {
            return ToStringSerializer.serialize(this);
        }

    }

    final class KilobytesSecondType implements StandardUnit {
        private KilobytesSecondType() {}

        @Override
        public String getValue() {
            return "Kilobytes/Second";
        }

        @Override
        public String toString() {
            return ToStringSerializer.serialize(this);
        }

    }

    final class MegabytesSecondType implements StandardUnit {
        private MegabytesSecondType() {}

        @Override
        public String getValue() {
            return "Megabytes/Second";
        }

        @Override
        public String toString() {
            return ToStringSerializer.serialize(this);
        }

    }

    final class GigabytesSecondType implements StandardUnit {
        private GigabytesSecondType() {}

        @Override
        public String getValue() {
            return "Gigabytes/Second";
        }

        @Override
        public String toString() {
            return ToStringSerializer.serialize(this);
        }

    }

    final class TerabytesSecondType implements StandardUnit {
        private TerabytesSecondType() {}

        @Override
        public String getValue() {
            return "Terabytes/Second";
        }

        @Override
        public String toString() {
            return ToStringSerializer.serialize(this);
        }

    }

    final class BitsSecondType implements StandardUnit {
        private BitsSecondType() {}

        @Override
        public String getValue() {
            return "Bits/Second";
        }

        @Override
        public String toString() {
            return ToStringSerializer.serialize(this);
        }

    }

    final class KilobitsSecondType implements StandardUnit {
        private KilobitsSecondType() {}

        @Override
        public String getValue() {
            return "Kilobits/Second";
        }

        @Override
        public String toString() {
            return ToStringSerializer.serialize(this);
        }

    }

    final class MegabitsSecondType implements StandardUnit {
        private MegabitsSecondType() {}

        @Override
        public String getValue() {
            return "Megabits/Second";
        }

        @Override
        public String toString() {
            return ToStringSerializer.serialize(this);
        }

    }

    final class GigabitsSecondType implements StandardUnit {
        private GigabitsSecondType() {}

        @Override
        public String getValue() {
            return "Gigabits/Second";
        }

        @Override
        public String toString() {
            return ToStringSerializer.serialize(this);
        }

    }

    final class TerabitsSecondType implements StandardUnit {
        private TerabitsSecondType() {}

        @Override
        public String getValue() {
            return "Terabits/Second";
        }

        @Override
        public String toString() {
            return ToStringSerializer.serialize(this);
        }

    }

    final class CountSecondType implements StandardUnit {
        private CountSecondType() {}

        @Override
        public String getValue() {
            return "Count/Second";
        }

        @Override
        public String toString() {
            return ToStringSerializer.serialize(this);
        }

    }

    final class NoneType implements StandardUnit {
        private NoneType() {}

        @Override
        public String getValue() {
            return "None";
        }

        @Override
        public String toString() {
            return ToStringSerializer.serialize(this);
        }

    }

    record $Unknown(String value) implements StandardUnit {
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

        private final class $Hidden implements StandardUnit {
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
     * Builder for {@link StandardUnit}.
     */
    final class Builder implements ShapeBuilder<StandardUnit> {
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
        public StandardUnit build() {
            return switch (value) {
                case "Seconds" -> SECONDS;
                case "Microseconds" -> MICROSECONDS;
                case "Milliseconds" -> MILLISECONDS;
                case "Bytes" -> BYTES;
                case "Kilobytes" -> KILOBYTES;
                case "Megabytes" -> MEGABYTES;
                case "Gigabytes" -> GIGABYTES;
                case "Terabytes" -> TERABYTES;
                case "Bits" -> BITS;
                case "Kilobits" -> KILOBITS;
                case "Megabits" -> MEGABITS;
                case "Gigabits" -> GIGABITS;
                case "Terabits" -> TERABITS;
                case "Percent" -> PERCENT;
                case "Count" -> COUNT;
                case "Bytes/Second" -> BYTES_SECOND;
                case "Kilobytes/Second" -> KILOBYTES_SECOND;
                case "Megabytes/Second" -> MEGABYTES_SECOND;
                case "Gigabytes/Second" -> GIGABYTES_SECOND;
                case "Terabytes/Second" -> TERABYTES_SECOND;
                case "Bits/Second" -> BITS_SECOND;
                case "Kilobits/Second" -> KILOBITS_SECOND;
                case "Megabits/Second" -> MEGABITS_SECOND;
                case "Gigabits/Second" -> GIGABITS_SECOND;
                case "Terabits/Second" -> TERABITS_SECOND;
                case "Count/Second" -> COUNT_SECOND;
                case "None" -> NONE;
                default -> new $Unknown(value);
            };
        }

        @Override
        public Builder deserialize(ShapeDeserializer de) {
            return value(de.readString($SCHEMA));
        }
    }
}
