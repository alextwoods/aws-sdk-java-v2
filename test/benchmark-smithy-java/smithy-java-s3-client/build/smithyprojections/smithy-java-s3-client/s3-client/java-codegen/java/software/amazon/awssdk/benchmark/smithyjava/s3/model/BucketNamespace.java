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
public sealed interface BucketNamespace extends SmithyEnum, SerializableShape {
    BucketNamespace ACCOUNT_REGIONAL = new AccountRegionalType();
    BucketNamespace GLOBAL = new GlobalType();
    List<BucketNamespace> $TYPES = List.of(ACCOUNT_REGIONAL, GLOBAL);

    Schema $SCHEMA = Schema.createEnum(ShapeId.from("com.amazonaws.s3#BucketNamespace"),
        Set.of(ACCOUNT_REGIONAL.getValue(), GLOBAL.getValue()), BucketNamespace.class
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
    static BucketNamespace unknown(String value) {
        return new $Unknown(value);
    }

    /**
     * Returns an unmodifiable list containing the constants of this enum type, in the order declared.
     */
    static List<BucketNamespace> values() {
        return $TYPES;
    }

    /**
     * Returns a {@link BucketNamespace} constant with the specified value.
     *
     * @param value value to create {@code BucketNamespace} from.
     * @throws IllegalArgumentException if value does not match a known value.
     */
    static BucketNamespace from(String value) {
        return switch (value) {
            case "account-regional" -> ACCOUNT_REGIONAL;
            case "global" -> GLOBAL;
            default -> throw new IllegalArgumentException("Unknown value: " + value);
        };
    }

    final class AccountRegionalType implements BucketNamespace {
        private AccountRegionalType() {}

        @Override
        public String getValue() {
            return "account-regional";
        }

        @Override
        public String toString() {
            return ToStringSerializer.serialize(this);
        }

    }

    final class GlobalType implements BucketNamespace {
        private GlobalType() {}

        @Override
        public String getValue() {
            return "global";
        }

        @Override
        public String toString() {
            return ToStringSerializer.serialize(this);
        }

    }

    record $Unknown(String value) implements BucketNamespace {
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

        private final class $Hidden implements BucketNamespace {
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
     * Builder for {@link BucketNamespace}.
     */
    final class Builder implements ShapeBuilder<BucketNamespace> {
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
        public BucketNamespace build() {
            return switch (value) {
                case "account-regional" -> ACCOUNT_REGIONAL;
                case "global" -> GLOBAL;
                default -> new $Unknown(value);
            };
        }

        @Override
        public Builder deserialize(ShapeDeserializer de) {
            return value(de.readString($SCHEMA));
        }
    }
}
