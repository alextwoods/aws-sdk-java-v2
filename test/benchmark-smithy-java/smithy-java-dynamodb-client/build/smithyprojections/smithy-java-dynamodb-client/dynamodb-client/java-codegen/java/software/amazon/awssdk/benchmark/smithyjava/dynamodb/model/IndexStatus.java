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
public sealed interface IndexStatus extends SmithyEnum, SerializableShape {
    IndexStatus CREATING = new CreatingType();
    IndexStatus UPDATING = new UpdatingType();
    IndexStatus DELETING = new DeletingType();
    IndexStatus ACTIVE = new ActiveType();
    List<IndexStatus> $TYPES = List.of(CREATING, UPDATING, DELETING, ACTIVE);

    Schema $SCHEMA = Schema.createEnum(ShapeId.from("com.amazonaws.dynamodb#IndexStatus"),
        Set.of(CREATING.getValue(), UPDATING.getValue(), DELETING.getValue(), ACTIVE.getValue()), IndexStatus.class
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
    static IndexStatus unknown(String value) {
        return new $Unknown(value);
    }

    /**
     * Returns an unmodifiable list containing the constants of this enum type, in the order declared.
     */
    static List<IndexStatus> values() {
        return $TYPES;
    }

    /**
     * Returns a {@link IndexStatus} constant with the specified value.
     *
     * @param value value to create {@code IndexStatus} from.
     * @throws IllegalArgumentException if value does not match a known value.
     */
    static IndexStatus from(String value) {
        return switch (value) {
            case "CREATING" -> CREATING;
            case "UPDATING" -> UPDATING;
            case "DELETING" -> DELETING;
            case "ACTIVE" -> ACTIVE;
            default -> throw new IllegalArgumentException("Unknown value: " + value);
        };
    }

    final class CreatingType implements IndexStatus {
        private CreatingType() {}

        @Override
        public String getValue() {
            return "CREATING";
        }

        @Override
        public String toString() {
            return ToStringSerializer.serialize(this);
        }

    }

    final class UpdatingType implements IndexStatus {
        private UpdatingType() {}

        @Override
        public String getValue() {
            return "UPDATING";
        }

        @Override
        public String toString() {
            return ToStringSerializer.serialize(this);
        }

    }

    final class DeletingType implements IndexStatus {
        private DeletingType() {}

        @Override
        public String getValue() {
            return "DELETING";
        }

        @Override
        public String toString() {
            return ToStringSerializer.serialize(this);
        }

    }

    final class ActiveType implements IndexStatus {
        private ActiveType() {}

        @Override
        public String getValue() {
            return "ACTIVE";
        }

        @Override
        public String toString() {
            return ToStringSerializer.serialize(this);
        }

    }

    record $Unknown(String value) implements IndexStatus {
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

        private final class $Hidden implements IndexStatus {
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
     * Builder for {@link IndexStatus}.
     */
    final class Builder implements ShapeBuilder<IndexStatus> {
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
        public IndexStatus build() {
            return switch (value) {
                case "CREATING" -> CREATING;
                case "UPDATING" -> UPDATING;
                case "DELETING" -> DELETING;
                case "ACTIVE" -> ACTIVE;
                default -> new $Unknown(value);
            };
        }

        @Override
        public Builder deserialize(ShapeDeserializer de) {
            return value(de.readString($SCHEMA));
        }
    }
}
