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
public sealed interface ContributorInsightsMode extends SmithyEnum, SerializableShape {
    ContributorInsightsMode ACCESSED_AND_THROTTLED_KEYS = new AccessedAndThrottledKeysType();
    ContributorInsightsMode THROTTLED_KEYS = new ThrottledKeysType();
    List<ContributorInsightsMode> $TYPES = List.of(ACCESSED_AND_THROTTLED_KEYS, THROTTLED_KEYS);

    Schema $SCHEMA = Schema.createEnum(ShapeId.from("com.amazonaws.dynamodb#ContributorInsightsMode"),
        Set.of(ACCESSED_AND_THROTTLED_KEYS.getValue(), THROTTLED_KEYS.getValue()), ContributorInsightsMode.class
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
    static ContributorInsightsMode unknown(String value) {
        return new $Unknown(value);
    }

    /**
     * Returns an unmodifiable list containing the constants of this enum type, in the order declared.
     */
    static List<ContributorInsightsMode> values() {
        return $TYPES;
    }

    /**
     * Returns a {@link ContributorInsightsMode} constant with the specified value.
     *
     * @param value value to create {@code ContributorInsightsMode} from.
     * @throws IllegalArgumentException if value does not match a known value.
     */
    static ContributorInsightsMode from(String value) {
        return switch (value) {
            case "ACCESSED_AND_THROTTLED_KEYS" -> ACCESSED_AND_THROTTLED_KEYS;
            case "THROTTLED_KEYS" -> THROTTLED_KEYS;
            default -> throw new IllegalArgumentException("Unknown value: " + value);
        };
    }

    final class AccessedAndThrottledKeysType implements ContributorInsightsMode {
        private AccessedAndThrottledKeysType() {}

        @Override
        public String getValue() {
            return "ACCESSED_AND_THROTTLED_KEYS";
        }

        @Override
        public String toString() {
            return ToStringSerializer.serialize(this);
        }

    }

    final class ThrottledKeysType implements ContributorInsightsMode {
        private ThrottledKeysType() {}

        @Override
        public String getValue() {
            return "THROTTLED_KEYS";
        }

        @Override
        public String toString() {
            return ToStringSerializer.serialize(this);
        }

    }

    record $Unknown(String value) implements ContributorInsightsMode {
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

        private final class $Hidden implements ContributorInsightsMode {
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
     * Builder for {@link ContributorInsightsMode}.
     */
    final class Builder implements ShapeBuilder<ContributorInsightsMode> {
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
        public ContributorInsightsMode build() {
            return switch (value) {
                case "ACCESSED_AND_THROTTLED_KEYS" -> ACCESSED_AND_THROTTLED_KEYS;
                case "THROTTLED_KEYS" -> THROTTLED_KEYS;
                default -> new $Unknown(value);
            };
        }

        @Override
        public Builder deserialize(ShapeDeserializer de) {
            return value(de.readString($SCHEMA));
        }
    }
}
