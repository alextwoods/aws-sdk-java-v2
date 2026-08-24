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
public sealed interface BucketAbacStatus extends SmithyEnum, SerializableShape {
    BucketAbacStatus ENABLED = new EnabledType();
    BucketAbacStatus DISABLED = new DisabledType();
    List<BucketAbacStatus> $TYPES = List.of(ENABLED, DISABLED);

    Schema $SCHEMA = Schema.createEnum(ShapeId.from("com.amazonaws.s3#BucketAbacStatus"),
        Set.of(ENABLED.getValue(), DISABLED.getValue()), BucketAbacStatus.class
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
    static BucketAbacStatus unknown(String value) {
        return new $Unknown(value);
    }

    /**
     * Returns an unmodifiable list containing the constants of this enum type, in the order declared.
     */
    static List<BucketAbacStatus> values() {
        return $TYPES;
    }

    /**
     * Returns a {@link BucketAbacStatus} constant with the specified value.
     *
     * @param value value to create {@code BucketAbacStatus} from.
     * @throws IllegalArgumentException if value does not match a known value.
     */
    static BucketAbacStatus from(String value) {
        return switch (value) {
            case "Enabled" -> ENABLED;
            case "Disabled" -> DISABLED;
            default -> throw new IllegalArgumentException("Unknown value: " + value);
        };
    }

    final class EnabledType implements BucketAbacStatus {
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

    final class DisabledType implements BucketAbacStatus {
        private DisabledType() {}

        @Override
        public String getValue() {
            return "Disabled";
        }

        @Override
        public String toString() {
            return ToStringSerializer.serialize(this);
        }

    }

    record $Unknown(String value) implements BucketAbacStatus {
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

        private final class $Hidden implements BucketAbacStatus {
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
     * Builder for {@link BucketAbacStatus}.
     */
    final class Builder implements ShapeBuilder<BucketAbacStatus> {
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
        public BucketAbacStatus build() {
            return switch (value) {
                case "Enabled" -> ENABLED;
                case "Disabled" -> DISABLED;
                default -> new $Unknown(value);
            };
        }

        @Override
        public Builder deserialize(ShapeDeserializer de) {
            return value(de.readString($SCHEMA));
        }
    }
}
