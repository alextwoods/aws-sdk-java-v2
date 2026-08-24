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
public sealed interface ObjectLockMode extends SmithyEnum, SerializableShape {
    ObjectLockMode GOVERNANCE = new GovernanceType();
    ObjectLockMode COMPLIANCE = new ComplianceType();
    List<ObjectLockMode> $TYPES = List.of(GOVERNANCE, COMPLIANCE);

    Schema $SCHEMA = Schema.createEnum(ShapeId.from("com.amazonaws.s3#ObjectLockMode"),
        Set.of(GOVERNANCE.getValue(), COMPLIANCE.getValue()), ObjectLockMode.class
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
    static ObjectLockMode unknown(String value) {
        return new $Unknown(value);
    }

    /**
     * Returns an unmodifiable list containing the constants of this enum type, in the order declared.
     */
    static List<ObjectLockMode> values() {
        return $TYPES;
    }

    /**
     * Returns a {@link ObjectLockMode} constant with the specified value.
     *
     * @param value value to create {@code ObjectLockMode} from.
     * @throws IllegalArgumentException if value does not match a known value.
     */
    static ObjectLockMode from(String value) {
        return switch (value) {
            case "GOVERNANCE" -> GOVERNANCE;
            case "COMPLIANCE" -> COMPLIANCE;
            default -> throw new IllegalArgumentException("Unknown value: " + value);
        };
    }

    final class GovernanceType implements ObjectLockMode {
        private GovernanceType() {}

        @Override
        public String getValue() {
            return "GOVERNANCE";
        }

        @Override
        public String toString() {
            return ToStringSerializer.serialize(this);
        }

    }

    final class ComplianceType implements ObjectLockMode {
        private ComplianceType() {}

        @Override
        public String getValue() {
            return "COMPLIANCE";
        }

        @Override
        public String toString() {
            return ToStringSerializer.serialize(this);
        }

    }

    record $Unknown(String value) implements ObjectLockMode {
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

        private final class $Hidden implements ObjectLockMode {
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
     * Builder for {@link ObjectLockMode}.
     */
    final class Builder implements ShapeBuilder<ObjectLockMode> {
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
        public ObjectLockMode build() {
            return switch (value) {
                case "GOVERNANCE" -> GOVERNANCE;
                case "COMPLIANCE" -> COMPLIANCE;
                default -> new $Unknown(value);
            };
        }

        @Override
        public Builder deserialize(ShapeDeserializer de) {
            return value(de.readString($SCHEMA));
        }
    }
}
