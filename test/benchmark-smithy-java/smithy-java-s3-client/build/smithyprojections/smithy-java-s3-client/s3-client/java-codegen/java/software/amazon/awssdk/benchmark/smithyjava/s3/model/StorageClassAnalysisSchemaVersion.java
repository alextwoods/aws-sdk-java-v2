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
public sealed interface StorageClassAnalysisSchemaVersion extends SmithyEnum, SerializableShape {
    StorageClassAnalysisSchemaVersion V_1 = new V1Type();
    List<StorageClassAnalysisSchemaVersion> $TYPES = List.of(V_1);

    Schema $SCHEMA = Schema.createEnum(ShapeId.from("com.amazonaws.s3#StorageClassAnalysisSchemaVersion"),
        Set.of(V_1.getValue()), StorageClassAnalysisSchemaVersion.class
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
    static StorageClassAnalysisSchemaVersion unknown(String value) {
        return new $Unknown(value);
    }

    /**
     * Returns an unmodifiable list containing the constants of this enum type, in the order declared.
     */
    static List<StorageClassAnalysisSchemaVersion> values() {
        return $TYPES;
    }

    /**
     * Returns a {@link StorageClassAnalysisSchemaVersion} constant with the specified value.
     *
     * @param value value to create {@code StorageClassAnalysisSchemaVersion} from.
     * @throws IllegalArgumentException if value does not match a known value.
     */
    static StorageClassAnalysisSchemaVersion from(String value) {
        return switch (value) {
            case "V_1" -> V_1;
            default -> throw new IllegalArgumentException("Unknown value: " + value);
        };
    }

    final class V1Type implements StorageClassAnalysisSchemaVersion {
        private V1Type() {}

        @Override
        public String getValue() {
            return "V_1";
        }

        @Override
        public String toString() {
            return ToStringSerializer.serialize(this);
        }

    }

    record $Unknown(String value) implements StorageClassAnalysisSchemaVersion {
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

        private final class $Hidden implements StorageClassAnalysisSchemaVersion {
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
     * Builder for {@link StorageClassAnalysisSchemaVersion}.
     */
    final class Builder implements ShapeBuilder<StorageClassAnalysisSchemaVersion> {
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
        public StorageClassAnalysisSchemaVersion build() {
            return switch (value) {
                case "V_1" -> V_1;
                default -> new $Unknown(value);
            };
        }

        @Override
        public Builder deserialize(ShapeDeserializer de) {
            return value(de.readString($SCHEMA));
        }
    }
}
