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
public sealed interface AnalyticsS3ExportFileFormat extends SmithyEnum, SerializableShape {
    AnalyticsS3ExportFileFormat CSV = new CsvType();
    List<AnalyticsS3ExportFileFormat> $TYPES = List.of(CSV);

    Schema $SCHEMA = Schema.createEnum(ShapeId.from("com.amazonaws.s3#AnalyticsS3ExportFileFormat"),
        Set.of(CSV.getValue()), AnalyticsS3ExportFileFormat.class
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
    static AnalyticsS3ExportFileFormat unknown(String value) {
        return new $Unknown(value);
    }

    /**
     * Returns an unmodifiable list containing the constants of this enum type, in the order declared.
     */
    static List<AnalyticsS3ExportFileFormat> values() {
        return $TYPES;
    }

    /**
     * Returns a {@link AnalyticsS3ExportFileFormat} constant with the specified value.
     *
     * @param value value to create {@code AnalyticsS3ExportFileFormat} from.
     * @throws IllegalArgumentException if value does not match a known value.
     */
    static AnalyticsS3ExportFileFormat from(String value) {
        return switch (value) {
            case "CSV" -> CSV;
            default -> throw new IllegalArgumentException("Unknown value: " + value);
        };
    }

    final class CsvType implements AnalyticsS3ExportFileFormat {
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

    record $Unknown(String value) implements AnalyticsS3ExportFileFormat {
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

        private final class $Hidden implements AnalyticsS3ExportFileFormat {
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
     * Builder for {@link AnalyticsS3ExportFileFormat}.
     */
    final class Builder implements ShapeBuilder<AnalyticsS3ExportFileFormat> {
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
        public AnalyticsS3ExportFileFormat build() {
            return switch (value) {
                case "CSV" -> CSV;
                default -> new $Unknown(value);
            };
        }

        @Override
        public Builder deserialize(ShapeDeserializer de) {
            return value(de.readString($SCHEMA));
        }
    }
}
