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
public sealed interface MFADelete extends SmithyEnum, SerializableShape {
    MFADelete ENABLED = new EnabledType();
    MFADelete DISABLED = new DisabledType();
    List<MFADelete> $TYPES = List.of(ENABLED, DISABLED);

    Schema $SCHEMA = Schema.createEnum(ShapeId.from("com.amazonaws.s3#MFADelete"),
        Set.of(ENABLED.getValue(), DISABLED.getValue()), MFADelete.class
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
    static MFADelete unknown(String value) {
        return new $Unknown(value);
    }

    /**
     * Returns an unmodifiable list containing the constants of this enum type, in the order declared.
     */
    static List<MFADelete> values() {
        return $TYPES;
    }

    /**
     * Returns a {@link MFADelete} constant with the specified value.
     *
     * @param value value to create {@code MFADelete} from.
     * @throws IllegalArgumentException if value does not match a known value.
     */
    static MFADelete from(String value) {
        return switch (value) {
            case "Enabled" -> ENABLED;
            case "Disabled" -> DISABLED;
            default -> throw new IllegalArgumentException("Unknown value: " + value);
        };
    }

    final class EnabledType implements MFADelete {
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

    final class DisabledType implements MFADelete {
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

    record $Unknown(String value) implements MFADelete {
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

        private final class $Hidden implements MFADelete {
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
     * Builder for {@link MFADelete}.
     */
    final class Builder implements ShapeBuilder<MFADelete> {
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
        public MFADelete build() {
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
