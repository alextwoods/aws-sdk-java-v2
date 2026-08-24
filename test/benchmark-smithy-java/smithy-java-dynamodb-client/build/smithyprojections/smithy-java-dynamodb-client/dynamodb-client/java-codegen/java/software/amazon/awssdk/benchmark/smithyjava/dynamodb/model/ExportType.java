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
public sealed interface ExportType extends SmithyEnum, SerializableShape {
    ExportType FULL_EXPORT = new FullExportType();
    ExportType INCREMENTAL_EXPORT = new IncrementalExportType();
    List<ExportType> $TYPES = List.of(FULL_EXPORT, INCREMENTAL_EXPORT);

    Schema $SCHEMA = Schema.createEnum(ShapeId.from("com.amazonaws.dynamodb#ExportType"),
        Set.of(FULL_EXPORT.getValue(), INCREMENTAL_EXPORT.getValue()), ExportType.class
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
    static ExportType unknown(String value) {
        return new $Unknown(value);
    }

    /**
     * Returns an unmodifiable list containing the constants of this enum type, in the order declared.
     */
    static List<ExportType> values() {
        return $TYPES;
    }

    /**
     * Returns a {@link ExportType} constant with the specified value.
     *
     * @param value value to create {@code ExportType} from.
     * @throws IllegalArgumentException if value does not match a known value.
     */
    static ExportType from(String value) {
        return switch (value) {
            case "FULL_EXPORT" -> FULL_EXPORT;
            case "INCREMENTAL_EXPORT" -> INCREMENTAL_EXPORT;
            default -> throw new IllegalArgumentException("Unknown value: " + value);
        };
    }

    final class FullExportType implements ExportType {
        private FullExportType() {}

        @Override
        public String getValue() {
            return "FULL_EXPORT";
        }

        @Override
        public String toString() {
            return ToStringSerializer.serialize(this);
        }

    }

    final class IncrementalExportType implements ExportType {
        private IncrementalExportType() {}

        @Override
        public String getValue() {
            return "INCREMENTAL_EXPORT";
        }

        @Override
        public String toString() {
            return ToStringSerializer.serialize(this);
        }

    }

    record $Unknown(String value) implements ExportType {
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

        private final class $Hidden implements ExportType {
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
     * Builder for {@link ExportType}.
     */
    final class Builder implements ShapeBuilder<ExportType> {
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
        public ExportType build() {
            return switch (value) {
                case "FULL_EXPORT" -> FULL_EXPORT;
                case "INCREMENTAL_EXPORT" -> INCREMENTAL_EXPORT;
                default -> new $Unknown(value);
            };
        }

        @Override
        public Builder deserialize(ShapeDeserializer de) {
            return value(de.readString($SCHEMA));
        }
    }
}
