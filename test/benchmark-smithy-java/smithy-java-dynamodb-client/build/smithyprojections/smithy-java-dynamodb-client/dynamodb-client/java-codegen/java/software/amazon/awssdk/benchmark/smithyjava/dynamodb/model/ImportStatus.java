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
public sealed interface ImportStatus extends SmithyEnum, SerializableShape {
    ImportStatus IN_PROGRESS = new InProgressType();
    ImportStatus COMPLETED = new CompletedType();
    ImportStatus CANCELLING = new CancellingType();
    ImportStatus CANCELLED = new CancelledType();
    ImportStatus FAILED = new FailedType();
    List<ImportStatus> $TYPES = List.of(IN_PROGRESS, COMPLETED, CANCELLING, CANCELLED, FAILED);

    Schema $SCHEMA = Schema.createEnum(ShapeId.from("com.amazonaws.dynamodb#ImportStatus"),
        Set.of(IN_PROGRESS.getValue(), COMPLETED.getValue(), CANCELLING.getValue(), CANCELLED.getValue(), FAILED.getValue()), ImportStatus.class
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
    static ImportStatus unknown(String value) {
        return new $Unknown(value);
    }

    /**
     * Returns an unmodifiable list containing the constants of this enum type, in the order declared.
     */
    static List<ImportStatus> values() {
        return $TYPES;
    }

    /**
     * Returns a {@link ImportStatus} constant with the specified value.
     *
     * @param value value to create {@code ImportStatus} from.
     * @throws IllegalArgumentException if value does not match a known value.
     */
    static ImportStatus from(String value) {
        return switch (value) {
            case "IN_PROGRESS" -> IN_PROGRESS;
            case "COMPLETED" -> COMPLETED;
            case "CANCELLING" -> CANCELLING;
            case "CANCELLED" -> CANCELLED;
            case "FAILED" -> FAILED;
            default -> throw new IllegalArgumentException("Unknown value: " + value);
        };
    }

    final class InProgressType implements ImportStatus {
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

    final class CompletedType implements ImportStatus {
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

    final class CancellingType implements ImportStatus {
        private CancellingType() {}

        @Override
        public String getValue() {
            return "CANCELLING";
        }

        @Override
        public String toString() {
            return ToStringSerializer.serialize(this);
        }

    }

    final class CancelledType implements ImportStatus {
        private CancelledType() {}

        @Override
        public String getValue() {
            return "CANCELLED";
        }

        @Override
        public String toString() {
            return ToStringSerializer.serialize(this);
        }

    }

    final class FailedType implements ImportStatus {
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

    record $Unknown(String value) implements ImportStatus {
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

        private final class $Hidden implements ImportStatus {
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
     * Builder for {@link ImportStatus}.
     */
    final class Builder implements ShapeBuilder<ImportStatus> {
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
        public ImportStatus build() {
            return switch (value) {
                case "IN_PROGRESS" -> IN_PROGRESS;
                case "COMPLETED" -> COMPLETED;
                case "CANCELLING" -> CANCELLING;
                case "CANCELLED" -> CANCELLED;
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
