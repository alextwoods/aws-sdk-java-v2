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
public sealed interface ContributorInsightsStatus extends SmithyEnum, SerializableShape {
    ContributorInsightsStatus ENABLING = new EnablingType();
    ContributorInsightsStatus ENABLED = new EnabledType();
    ContributorInsightsStatus DISABLING = new DisablingType();
    ContributorInsightsStatus DISABLED = new DisabledType();
    ContributorInsightsStatus FAILED = new FailedType();
    List<ContributorInsightsStatus> $TYPES = List.of(ENABLING, ENABLED, DISABLING, DISABLED, FAILED);

    Schema $SCHEMA = Schema.createEnum(ShapeId.from("com.amazonaws.dynamodb#ContributorInsightsStatus"),
        Set.of(ENABLING.getValue(), ENABLED.getValue(), DISABLING.getValue(), DISABLED.getValue(), FAILED.getValue()), ContributorInsightsStatus.class
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
    static ContributorInsightsStatus unknown(String value) {
        return new $Unknown(value);
    }

    /**
     * Returns an unmodifiable list containing the constants of this enum type, in the order declared.
     */
    static List<ContributorInsightsStatus> values() {
        return $TYPES;
    }

    /**
     * Returns a {@link ContributorInsightsStatus} constant with the specified value.
     *
     * @param value value to create {@code ContributorInsightsStatus} from.
     * @throws IllegalArgumentException if value does not match a known value.
     */
    static ContributorInsightsStatus from(String value) {
        return switch (value) {
            case "ENABLING" -> ENABLING;
            case "ENABLED" -> ENABLED;
            case "DISABLING" -> DISABLING;
            case "DISABLED" -> DISABLED;
            case "FAILED" -> FAILED;
            default -> throw new IllegalArgumentException("Unknown value: " + value);
        };
    }

    final class EnablingType implements ContributorInsightsStatus {
        private EnablingType() {}

        @Override
        public String getValue() {
            return "ENABLING";
        }

        @Override
        public String toString() {
            return ToStringSerializer.serialize(this);
        }

    }

    final class EnabledType implements ContributorInsightsStatus {
        private EnabledType() {}

        @Override
        public String getValue() {
            return "ENABLED";
        }

        @Override
        public String toString() {
            return ToStringSerializer.serialize(this);
        }

    }

    final class DisablingType implements ContributorInsightsStatus {
        private DisablingType() {}

        @Override
        public String getValue() {
            return "DISABLING";
        }

        @Override
        public String toString() {
            return ToStringSerializer.serialize(this);
        }

    }

    final class DisabledType implements ContributorInsightsStatus {
        private DisabledType() {}

        @Override
        public String getValue() {
            return "DISABLED";
        }

        @Override
        public String toString() {
            return ToStringSerializer.serialize(this);
        }

    }

    final class FailedType implements ContributorInsightsStatus {
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

    record $Unknown(String value) implements ContributorInsightsStatus {
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

        private final class $Hidden implements ContributorInsightsStatus {
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
     * Builder for {@link ContributorInsightsStatus}.
     */
    final class Builder implements ShapeBuilder<ContributorInsightsStatus> {
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
        public ContributorInsightsStatus build() {
            return switch (value) {
                case "ENABLING" -> ENABLING;
                case "ENABLED" -> ENABLED;
                case "DISABLING" -> DISABLING;
                case "DISABLED" -> DISABLED;
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
