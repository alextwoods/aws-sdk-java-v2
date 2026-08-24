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
public sealed interface Type extends SmithyEnum, SerializableShape {
    Type CANONICAL_USER = new CanonicalUserType();
    Type AMAZON_CUSTOMER_BY_EMAIL = new AmazonCustomerByEmailType();
    Type GROUP = new GroupType();
    List<Type> $TYPES = List.of(CANONICAL_USER, AMAZON_CUSTOMER_BY_EMAIL, GROUP);

    Schema $SCHEMA = Schema.createEnum(ShapeId.from("com.amazonaws.s3#Type"),
        Set.of(CANONICAL_USER.getValue(), AMAZON_CUSTOMER_BY_EMAIL.getValue(), GROUP.getValue()), Type.class
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
    static Type unknown(String value) {
        return new $Unknown(value);
    }

    /**
     * Returns an unmodifiable list containing the constants of this enum type, in the order declared.
     */
    static List<Type> values() {
        return $TYPES;
    }

    /**
     * Returns a {@link Type} constant with the specified value.
     *
     * @param value value to create {@code Type} from.
     * @throws IllegalArgumentException if value does not match a known value.
     */
    static Type from(String value) {
        return switch (value) {
            case "CanonicalUser" -> CANONICAL_USER;
            case "AmazonCustomerByEmail" -> AMAZON_CUSTOMER_BY_EMAIL;
            case "Group" -> GROUP;
            default -> throw new IllegalArgumentException("Unknown value: " + value);
        };
    }

    final class CanonicalUserType implements Type {
        private CanonicalUserType() {}

        @Override
        public String getValue() {
            return "CanonicalUser";
        }

        @Override
        public String toString() {
            return ToStringSerializer.serialize(this);
        }

    }

    final class AmazonCustomerByEmailType implements Type {
        private AmazonCustomerByEmailType() {}

        @Override
        public String getValue() {
            return "AmazonCustomerByEmail";
        }

        @Override
        public String toString() {
            return ToStringSerializer.serialize(this);
        }

    }

    final class GroupType implements Type {
        private GroupType() {}

        @Override
        public String getValue() {
            return "Group";
        }

        @Override
        public String toString() {
            return ToStringSerializer.serialize(this);
        }

    }

    record $Unknown(String value) implements Type {
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

        private final class $Hidden implements Type {
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
     * Builder for {@link Type}.
     */
    final class Builder implements ShapeBuilder<Type> {
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
        public Type build() {
            return switch (value) {
                case "CanonicalUser" -> CANONICAL_USER;
                case "AmazonCustomerByEmail" -> AMAZON_CUSTOMER_BY_EMAIL;
                case "Group" -> GROUP;
                default -> new $Unknown(value);
            };
        }

        @Override
        public Builder deserialize(ShapeDeserializer de) {
            return value(de.readString($SCHEMA));
        }
    }
}
