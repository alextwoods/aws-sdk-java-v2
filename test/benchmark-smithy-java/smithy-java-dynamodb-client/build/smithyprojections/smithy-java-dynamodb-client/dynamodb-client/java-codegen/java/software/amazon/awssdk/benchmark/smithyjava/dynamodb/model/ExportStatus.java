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
public sealed interface ExportStatus extends SmithyEnum, SerializableShape {
    ExportStatus IN_PROGRESS = new InProgressType();
    ExportStatus COMPLETED = new CompletedType();
    ExportStatus FAILED = new FailedType();
    List<ExportStatus> $TYPES = List.of(IN_PROGRESS, COMPLETED, FAILED);

    Schema $SCHEMA = Schema.createEnum(ShapeId.from("com.amazonaws.dynamodb#ExportStatus"),
        Set.of(IN_PROGRESS.getValue(), COMPLETED.getValue(), FAILED.getValue()), ExportStatus.class
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
    static ExportStatus unknown(String value) {
        return new $Unknown(value);
    }

    /**
     * Returns an unmodifiable list containing the constants of this enum type, in the order declared.
     */
    static List<ExportStatus> values() {
        return $TYPES;
    }

    /**
     * Returns a {@link ExportStatus} constant with the specified value.
     *
     * @param value value to create {@code ExportStatus} from.
     * @throws IllegalArgumentException if value does not match a known value.
     */
    static ExportStatus from(String value) {
        return switch (value) {
            case "IN_PROGRESS" -> IN_PROGRESS;
            case "COMPLETED" -> COMPLETED;
            case "FAILED" -> FAILED;
            default -> throw new IllegalArgumentException("Unknown value: " + value);
        };
    }

    final class InProgressType implements ExportStatus {
        private InProgressType() {}

        @Override
        public String getValue() {
            return "IN_PROGRESS";
        }

        @Override
        public String toString() {
            return ToStringSerializer.serialize(this);
        }

    }

    final class CompletedType implements ExportStatus {
        private CompletedType() {}

        @Override
        public String getValue() {
            return "COMPLETED";
        }

        @Override
        public String toString() {
            return ToStringSerializer.serialize(this);
        }

    }

    final class FailedType implements ExportStatus {
        private FailedType() {}

        @Override
        public String getValue() {
            return "FAILED";
        }

        @Override
        public String toString() {
            return ToStringSerializer.serialize(this);
        }

    }

    record $Unknown(String value) implements ExportStatus {
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

        private final class $Hidden implements ExportStatus {
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
     * Builder for {@link ExportStatus}.
     */
    final class Builder implements ShapeBuilder<ExportStatus> {
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
        public ExportStatus build() {
            return switch (value) {
                case "IN_PROGRESS" -> IN_PROGRESS;
                case "COMPLETED" -> COMPLETED;
                case "FAILED" -> FAILED;
                default -> new $Unknown(value);
            };
        }

        @Override
        public Builder deserialize(ShapeDeserializer de) {
            return value(de.readString($SCHEMA));
        }
    }
}
