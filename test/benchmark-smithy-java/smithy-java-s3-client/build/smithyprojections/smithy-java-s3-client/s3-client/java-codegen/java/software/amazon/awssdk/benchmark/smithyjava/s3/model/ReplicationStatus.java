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
public sealed interface ReplicationStatus extends SmithyEnum, SerializableShape {
    ReplicationStatus COMPLETE = new CompleteType();
    ReplicationStatus PENDING = new PendingType();
    ReplicationStatus FAILED = new FailedType();
    ReplicationStatus REPLICA = new ReplicaType();
    ReplicationStatus COMPLETED = new CompletedType();
    List<ReplicationStatus> $TYPES = List.of(COMPLETE, PENDING, FAILED, REPLICA, COMPLETED);

    Schema $SCHEMA = Schema.createEnum(ShapeId.from("com.amazonaws.s3#ReplicationStatus"),
        Set.of(COMPLETE.getValue(), PENDING.getValue(), FAILED.getValue(), REPLICA.getValue(), COMPLETED.getValue()), ReplicationStatus.class
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
    static ReplicationStatus unknown(String value) {
        return new $Unknown(value);
    }

    /**
     * Returns an unmodifiable list containing the constants of this enum type, in the order declared.
     */
    static List<ReplicationStatus> values() {
        return $TYPES;
    }

    /**
     * Returns a {@link ReplicationStatus} constant with the specified value.
     *
     * @param value value to create {@code ReplicationStatus} from.
     * @throws IllegalArgumentException if value does not match a known value.
     */
    static ReplicationStatus from(String value) {
        return switch (value) {
            case "COMPLETE" -> COMPLETE;
            case "PENDING" -> PENDING;
            case "FAILED" -> FAILED;
            case "REPLICA" -> REPLICA;
            case "COMPLETED" -> COMPLETED;
            default -> throw new IllegalArgumentException("Unknown value: " + value);
        };
    }

    final class CompleteType implements ReplicationStatus {
        private CompleteType() {}

        @Override
        public String getValue() {
            return "COMPLETE";
        }

        @Override
        public String toString() {
            return ToStringSerializer.serialize(this);
        }

    }

    final class PendingType implements ReplicationStatus {
        private PendingType() {}

        @Override
        public String getValue() {
            return "PENDING";
        }

        @Override
        public String toString() {
            return ToStringSerializer.serialize(this);
        }

    }

    final class FailedType implements ReplicationStatus {
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

    final class ReplicaType implements ReplicationStatus {
        private ReplicaType() {}

        @Override
        public String getValue() {
            return "REPLICA";
        }

        @Override
        public String toString() {
            return ToStringSerializer.serialize(this);
        }

    }

    final class CompletedType implements ReplicationStatus {
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

    record $Unknown(String value) implements ReplicationStatus {
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

        private final class $Hidden implements ReplicationStatus {
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
     * Builder for {@link ReplicationStatus}.
     */
    final class Builder implements ShapeBuilder<ReplicationStatus> {
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
        public ReplicationStatus build() {
            return switch (value) {
                case "COMPLETE" -> COMPLETE;
                case "PENDING" -> PENDING;
                case "FAILED" -> FAILED;
                case "REPLICA" -> REPLICA;
                case "COMPLETED" -> COMPLETED;
                default -> new $Unknown(value);
            };
        }

        @Override
        public Builder deserialize(ShapeDeserializer de) {
            return value(de.readString($SCHEMA));
        }
    }
}
