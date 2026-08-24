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
public sealed interface BucketAccelerateStatus extends SmithyEnum, SerializableShape {
    BucketAccelerateStatus ENABLED = new EnabledType();
    BucketAccelerateStatus SUSPENDED = new SuspendedType();
    List<BucketAccelerateStatus> $TYPES = List.of(ENABLED, SUSPENDED);

    Schema $SCHEMA = Schema.createEnum(ShapeId.from("com.amazonaws.s3#BucketAccelerateStatus"),
        Set.of(ENABLED.getValue(), SUSPENDED.getValue()), BucketAccelerateStatus.class
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
    static BucketAccelerateStatus unknown(String value) {
        return new $Unknown(value);
    }

    /**
     * Returns an unmodifiable list containing the constants of this enum type, in the order declared.
     */
    static List<BucketAccelerateStatus> values() {
        return $TYPES;
    }

    /**
     * Returns a {@link BucketAccelerateStatus} constant with the specified value.
     *
     * @param value value to create {@code BucketAccelerateStatus} from.
     * @throws IllegalArgumentException if value does not match a known value.
     */
    static BucketAccelerateStatus from(String value) {
        return switch (value) {
            case "Enabled" -> ENABLED;
            case "Suspended" -> SUSPENDED;
            default -> throw new IllegalArgumentException("Unknown value: " + value);
        };
    }

    final class EnabledType implements BucketAccelerateStatus {
        private EnabledType() {}

        @Override
        public String getValue() {
            return "Enabled";
        }

        @Override
        public String toString() {
            return ToStringSerializer.serialize(this);
        }

    }

    final class SuspendedType implements BucketAccelerateStatus {
        private SuspendedType() {}

        @Override
        public String getValue() {
            return "Suspended";
        }

        @Override
        public String toString() {
            return ToStringSerializer.serialize(this);
        }

    }

    record $Unknown(String value) implements BucketAccelerateStatus {
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

        private final class $Hidden implements BucketAccelerateStatus {
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
     * Builder for {@link BucketAccelerateStatus}.
     */
    final class Builder implements ShapeBuilder<BucketAccelerateStatus> {
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
        public BucketAccelerateStatus build() {
            return switch (value) {
                case "Enabled" -> ENABLED;
                case "Suspended" -> SUSPENDED;
                default -> new $Unknown(value);
            };
        }

        @Override
        public Builder deserialize(ShapeDeserializer de) {
            return value(de.readString($SCHEMA));
        }
    }
}
