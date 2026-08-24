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
public sealed interface ReturnValuesOnConditionCheckFailure extends SmithyEnum, SerializableShape {
    ReturnValuesOnConditionCheckFailure ALL_OLD = new AllOldType();
    ReturnValuesOnConditionCheckFailure NONE = new NoneType();
    List<ReturnValuesOnConditionCheckFailure> $TYPES = List.of(ALL_OLD, NONE);

    Schema $SCHEMA = Schema.createEnum(ShapeId.from("com.amazonaws.dynamodb#ReturnValuesOnConditionCheckFailure"),
        Set.of(ALL_OLD.getValue(), NONE.getValue()), ReturnValuesOnConditionCheckFailure.class
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
    static ReturnValuesOnConditionCheckFailure unknown(String value) {
        return new $Unknown(value);
    }

    /**
     * Returns an unmodifiable list containing the constants of this enum type, in the order declared.
     */
    static List<ReturnValuesOnConditionCheckFailure> values() {
        return $TYPES;
    }

    /**
     * Returns a {@link ReturnValuesOnConditionCheckFailure} constant with the specified value.
     *
     * @param value value to create {@code ReturnValuesOnConditionCheckFailure} from.
     * @throws IllegalArgumentException if value does not match a known value.
     */
    static ReturnValuesOnConditionCheckFailure from(String value) {
        return switch (value) {
            case "ALL_OLD" -> ALL_OLD;
            case "NONE" -> NONE;
            default -> throw new IllegalArgumentException("Unknown value: " + value);
        };
    }

    final class AllOldType implements ReturnValuesOnConditionCheckFailure {
        private AllOldType() {}

        @Override
        public String getValue() {
            return "ALL_OLD";
        }

        @Override
        public String toString() {
            return ToStringSerializer.serialize(this);
        }

    }

    final class NoneType implements ReturnValuesOnConditionCheckFailure {
        private NoneType() {}

        @Override
        public String getValue() {
            return "NONE";
        }

        @Override
        public String toString() {
            return ToStringSerializer.serialize(this);
        }

    }

    record $Unknown(String value) implements ReturnValuesOnConditionCheckFailure {
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

        private final class $Hidden implements ReturnValuesOnConditionCheckFailure {
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
     * Builder for {@link ReturnValuesOnConditionCheckFailure}.
     */
    final class Builder implements ShapeBuilder<ReturnValuesOnConditionCheckFailure> {
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
        public ReturnValuesOnConditionCheckFailure build() {
            return switch (value) {
                case "ALL_OLD" -> ALL_OLD;
                case "NONE" -> NONE;
                default -> new $Unknown(value);
            };
        }

        @Override
        public Builder deserialize(ShapeDeserializer de) {
            return value(de.readString($SCHEMA));
        }
    }
}
