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
public sealed interface ExpressionType extends SmithyEnum, SerializableShape {
    ExpressionType SQL = new SqlType();
    List<ExpressionType> $TYPES = List.of(SQL);

    Schema $SCHEMA = Schema.createEnum(ShapeId.from("com.amazonaws.s3#ExpressionType"),
        Set.of(SQL.getValue()), ExpressionType.class
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
    static ExpressionType unknown(String value) {
        return new $Unknown(value);
    }

    /**
     * Returns an unmodifiable list containing the constants of this enum type, in the order declared.
     */
    static List<ExpressionType> values() {
        return $TYPES;
    }

    /**
     * Returns a {@link ExpressionType} constant with the specified value.
     *
     * @param value value to create {@code ExpressionType} from.
     * @throws IllegalArgumentException if value does not match a known value.
     */
    static ExpressionType from(String value) {
        return switch (value) {
            case "SQL" -> SQL;
            default -> throw new IllegalArgumentException("Unknown value: " + value);
        };
    }

    final class SqlType implements ExpressionType {
        private SqlType() {}

        @Override
        public String getValue() {
            return "SQL";
        }

        @Override
        public String toString() {
            return ToStringSerializer.serialize(this);
        }

    }

    record $Unknown(String value) implements ExpressionType {
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

        private final class $Hidden implements ExpressionType {
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
     * Builder for {@link ExpressionType}.
     */
    final class Builder implements ShapeBuilder<ExpressionType> {
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
        public ExpressionType build() {
            return switch (value) {
                case "SQL" -> SQL;
                default -> new $Unknown(value);
            };
        }

        @Override
        public Builder deserialize(ShapeDeserializer de) {
            return value(de.readString($SCHEMA));
        }
    }
}
