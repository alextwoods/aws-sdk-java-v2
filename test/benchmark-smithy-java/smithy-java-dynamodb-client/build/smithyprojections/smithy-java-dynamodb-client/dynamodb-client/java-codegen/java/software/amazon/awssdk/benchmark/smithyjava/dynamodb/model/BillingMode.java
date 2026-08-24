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
public sealed interface BillingMode extends SmithyEnum, SerializableShape {
    BillingMode PROVISIONED = new ProvisionedType();
    BillingMode PAY_PER_REQUEST = new PayPerRequestType();
    List<BillingMode> $TYPES = List.of(PROVISIONED, PAY_PER_REQUEST);

    Schema $SCHEMA = Schema.createEnum(ShapeId.from("com.amazonaws.dynamodb#BillingMode"),
        Set.of(PROVISIONED.getValue(), PAY_PER_REQUEST.getValue()), BillingMode.class
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
    static BillingMode unknown(String value) {
        return new $Unknown(value);
    }

    /**
     * Returns an unmodifiable list containing the constants of this enum type, in the order declared.
     */
    static List<BillingMode> values() {
        return $TYPES;
    }

    /**
     * Returns a {@link BillingMode} constant with the specified value.
     *
     * @param value value to create {@code BillingMode} from.
     * @throws IllegalArgumentException if value does not match a known value.
     */
    static BillingMode from(String value) {
        return switch (value) {
            case "PROVISIONED" -> PROVISIONED;
            case "PAY_PER_REQUEST" -> PAY_PER_REQUEST;
            default -> throw new IllegalArgumentException("Unknown value: " + value);
        };
    }

    final class ProvisionedType implements BillingMode {
        private ProvisionedType() {}

        @Override
        public String getValue() {
            return "PROVISIONED";
        }

        @Override
        public String toString() {
            return ToStringSerializer.serialize(this);
        }

    }

    final class PayPerRequestType implements BillingMode {
        private PayPerRequestType() {}

        @Override
        public String getValue() {
            return "PAY_PER_REQUEST";
        }

        @Override
        public String toString() {
            return ToStringSerializer.serialize(this);
        }

    }

    record $Unknown(String value) implements BillingMode {
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

        private final class $Hidden implements BillingMode {
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
     * Builder for {@link BillingMode}.
     */
    final class Builder implements ShapeBuilder<BillingMode> {
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
        public BillingMode build() {
            return switch (value) {
                case "PROVISIONED" -> PROVISIONED;
                case "PAY_PER_REQUEST" -> PAY_PER_REQUEST;
                default -> new $Unknown(value);
            };
        }

        @Override
        public Builder deserialize(ShapeDeserializer de) {
            return value(de.readString($SCHEMA));
        }
    }
}
