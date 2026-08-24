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

/**
 * The status of OTel enrichment for the account.
 */
@SmithyGenerated
public sealed interface OTelEnrichmentStatus extends SmithyEnum, SerializableShape {
    OTelEnrichmentStatus RUNNING = new RunningType();
    OTelEnrichmentStatus STOPPED = new StoppedType();
    List<OTelEnrichmentStatus> $TYPES = List.of(RUNNING, STOPPED);

    Schema $SCHEMA = Schema.createEnum(ShapeId.from("com.amazonaws.cloudwatch#OTelEnrichmentStatus"),
        Set.of(RUNNING.getValue(), STOPPED.getValue()), OTelEnrichmentStatus.class
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
    static OTelEnrichmentStatus unknown(String value) {
        return new $Unknown(value);
    }

    /**
     * Returns an unmodifiable list containing the constants of this enum type, in the order declared.
     */
    static List<OTelEnrichmentStatus> values() {
        return $TYPES;
    }

    /**
     * Returns a {@link OTelEnrichmentStatus} constant with the specified value.
     *
     * @param value value to create {@code OTelEnrichmentStatus} from.
     * @throws IllegalArgumentException if value does not match a known value.
     */
    static OTelEnrichmentStatus from(String value) {
        return switch (value) {
            case "Running" -> RUNNING;
            case "Stopped" -> STOPPED;
            default -> throw new IllegalArgumentException("Unknown value: " + value);
        };
    }

    final class RunningType implements OTelEnrichmentStatus {
        private RunningType() {}

        @Override
        public String getValue() {
            return "Running";
        }

        @Override
        public String toString() {
            return ToStringSerializer.serialize(this);
        }

    }

    final class StoppedType implements OTelEnrichmentStatus {
        private StoppedType() {}

        @Override
        public String getValue() {
            return "Stopped";
        }

        @Override
        public String toString() {
            return ToStringSerializer.serialize(this);
        }

    }

    record $Unknown(String value) implements OTelEnrichmentStatus {
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

        private final class $Hidden implements OTelEnrichmentStatus {
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
     * Builder for {@link OTelEnrichmentStatus}.
     */
    final class Builder implements ShapeBuilder<OTelEnrichmentStatus> {
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
        public OTelEnrichmentStatus build() {
            return switch (value) {
                case "Running" -> RUNNING;
                case "Stopped" -> STOPPED;
                default -> new $Unknown(value);
            };
        }

        @Override
        public Builder deserialize(ShapeDeserializer de) {
            return value(de.readString($SCHEMA));
        }
    }
}
