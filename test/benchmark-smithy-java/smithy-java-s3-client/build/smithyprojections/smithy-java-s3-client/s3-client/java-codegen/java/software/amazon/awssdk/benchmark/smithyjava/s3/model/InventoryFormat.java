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
public sealed interface InventoryFormat extends SmithyEnum, SerializableShape {
    InventoryFormat CSV = new CsvType();
    InventoryFormat ORC = new OrcType();
    InventoryFormat PARQUET = new ParquetType();
    List<InventoryFormat> $TYPES = List.of(CSV, ORC, PARQUET);

    Schema $SCHEMA = Schema.createEnum(ShapeId.from("com.amazonaws.s3#InventoryFormat"),
        Set.of(CSV.getValue(), ORC.getValue(), PARQUET.getValue()), InventoryFormat.class
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
    static InventoryFormat unknown(String value) {
        return new $Unknown(value);
    }

    /**
     * Returns an unmodifiable list containing the constants of this enum type, in the order declared.
     */
    static List<InventoryFormat> values() {
        return $TYPES;
    }

    /**
     * Returns a {@link InventoryFormat} constant with the specified value.
     *
     * @param value value to create {@code InventoryFormat} from.
     * @throws IllegalArgumentException if value does not match a known value.
     */
    static InventoryFormat from(String value) {
        return switch (value) {
            case "CSV" -> CSV;
            case "ORC" -> ORC;
            case "Parquet" -> PARQUET;
            default -> throw new IllegalArgumentException("Unknown value: " + value);
        };
    }

    final class CsvType implements InventoryFormat {
        private CsvType() {}

        @Override
        public String getValue() {
            return "CSV";
        }

        @Override
        public String toString() {
            return ToStringSerializer.serialize(this);
        }

    }

    final class OrcType implements InventoryFormat {
        private OrcType() {}

        @Override
        public String getValue() {
            return "ORC";
        }

        @Override
        public String toString() {
            return ToStringSerializer.serialize(this);
        }

    }

    final class ParquetType implements InventoryFormat {
        private ParquetType() {}

        @Override
        public String getValue() {
            return "Parquet";
        }

        @Override
        public String toString() {
            return ToStringSerializer.serialize(this);
        }

    }

    record $Unknown(String value) implements InventoryFormat {
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

        private final class $Hidden implements InventoryFormat {
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
     * Builder for {@link InventoryFormat}.
     */
    final class Builder implements ShapeBuilder<InventoryFormat> {
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
        public InventoryFormat build() {
            return switch (value) {
                case "CSV" -> CSV;
                case "ORC" -> ORC;
                case "Parquet" -> PARQUET;
                default -> new $Unknown(value);
            };
        }

        @Override
        public Builder deserialize(ShapeDeserializer de) {
            return value(de.readString($SCHEMA));
        }
    }
}
