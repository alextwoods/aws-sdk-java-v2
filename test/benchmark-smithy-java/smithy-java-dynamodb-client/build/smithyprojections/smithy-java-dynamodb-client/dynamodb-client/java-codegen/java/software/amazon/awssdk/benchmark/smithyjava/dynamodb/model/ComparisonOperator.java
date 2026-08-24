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
public sealed interface ComparisonOperator extends SmithyEnum, SerializableShape {
    ComparisonOperator EQ = new EqType();
    ComparisonOperator NE = new NeType();
    ComparisonOperator IN = new InType();
    ComparisonOperator LE = new LeType();
    ComparisonOperator LT = new LtType();
    ComparisonOperator GE = new GeType();
    ComparisonOperator GT = new GtType();
    ComparisonOperator BETWEEN = new BetweenType();
    ComparisonOperator NOT_NULL = new NotNullType();
    ComparisonOperator NULL = new NullType();
    ComparisonOperator CONTAINS = new ContainsType();
    ComparisonOperator NOT_CONTAINS = new NotContainsType();
    ComparisonOperator BEGINS_WITH = new BeginsWithType();
    List<ComparisonOperator> $TYPES = List.of(EQ, NE, IN, LE, LT, GE, GT, BETWEEN, NOT_NULL, NULL, CONTAINS, NOT_CONTAINS, BEGINS_WITH);

    Schema $SCHEMA = Schema.createEnum(ShapeId.from("com.amazonaws.dynamodb#ComparisonOperator"),
        Set.of(EQ.getValue(), NE.getValue(), IN.getValue(), LE.getValue(), LT.getValue(), GE.getValue(), GT.getValue(), BETWEEN.getValue(), NOT_NULL.getValue(), NULL.getValue(), CONTAINS.getValue(), NOT_CONTAINS.getValue(), BEGINS_WITH.getValue()), ComparisonOperator.class
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
    static ComparisonOperator unknown(String value) {
        return new $Unknown(value);
    }

    /**
     * Returns an unmodifiable list containing the constants of this enum type, in the order declared.
     */
    static List<ComparisonOperator> values() {
        return $TYPES;
    }

    /**
     * Returns a {@link ComparisonOperator} constant with the specified value.
     *
     * @param value value to create {@code ComparisonOperator} from.
     * @throws IllegalArgumentException if value does not match a known value.
     */
    static ComparisonOperator from(String value) {
        return switch (value) {
            case "EQ" -> EQ;
            case "NE" -> NE;
            case "IN" -> IN;
            case "LE" -> LE;
            case "LT" -> LT;
            case "GE" -> GE;
            case "GT" -> GT;
            case "BETWEEN" -> BETWEEN;
            case "NOT_NULL" -> NOT_NULL;
            case "NULL" -> NULL;
            case "CONTAINS" -> CONTAINS;
            case "NOT_CONTAINS" -> NOT_CONTAINS;
            case "BEGINS_WITH" -> BEGINS_WITH;
            default -> throw new IllegalArgumentException("Unknown value: " + value);
        };
    }

    final class EqType implements ComparisonOperator {
        private EqType() {}

        @Override
        public String getValue() {
            return "EQ";
        }

        @Override
        public String toString() {
            return ToStringSerializer.serialize(this);
        }

    }

    final class NeType implements ComparisonOperator {
        private NeType() {}

        @Override
        public String getValue() {
            return "NE";
        }

        @Override
        public String toString() {
            return ToStringSerializer.serialize(this);
        }

    }

    final class InType implements ComparisonOperator {
        private InType() {}

        @Override
        public String getValue() {
            return "IN";
        }

        @Override
        public String toString() {
            return ToStringSerializer.serialize(this);
        }

    }

    final class LeType implements ComparisonOperator {
        private LeType() {}

        @Override
        public String getValue() {
            return "LE";
        }

        @Override
        public String toString() {
            return ToStringSerializer.serialize(this);
        }

    }

    final class LtType implements ComparisonOperator {
        private LtType() {}

        @Override
        public String getValue() {
            return "LT";
        }

        @Override
        public String toString() {
            return ToStringSerializer.serialize(this);
        }

    }

    final class GeType implements ComparisonOperator {
        private GeType() {}

        @Override
        public String getValue() {
            return "GE";
        }

        @Override
        public String toString() {
            return ToStringSerializer.serialize(this);
        }

    }

    final class GtType implements ComparisonOperator {
        private GtType() {}

        @Override
        public String getValue() {
            return "GT";
        }

        @Override
        public String toString() {
            return ToStringSerializer.serialize(this);
        }

    }

    final class BetweenType implements ComparisonOperator {
        private BetweenType() {}

        @Override
        public String getValue() {
            return "BETWEEN";
        }

        @Override
        public String toString() {
            return ToStringSerializer.serialize(this);
        }

    }

    final class NotNullType implements ComparisonOperator {
        private NotNullType() {}

        @Override
        public String getValue() {
            return "NOT_NULL";
        }

        @Override
        public String toString() {
            return ToStringSerializer.serialize(this);
        }

    }

    final class NullType implements ComparisonOperator {
        private NullType() {}

        @Override
        public String getValue() {
            return "NULL";
        }

        @Override
        public String toString() {
            return ToStringSerializer.serialize(this);
        }

    }

    final class ContainsType implements ComparisonOperator {
        private ContainsType() {}

        @Override
        public String getValue() {
            return "CONTAINS";
        }

        @Override
        public String toString() {
            return ToStringSerializer.serialize(this);
        }

    }

    final class NotContainsType implements ComparisonOperator {
        private NotContainsType() {}

        @Override
        public String getValue() {
            return "NOT_CONTAINS";
        }

        @Override
        public String toString() {
            return ToStringSerializer.serialize(this);
        }

    }

    final class BeginsWithType implements ComparisonOperator {
        private BeginsWithType() {}

        @Override
        public String getValue() {
            return "BEGINS_WITH";
        }

        @Override
        public String toString() {
            return ToStringSerializer.serialize(this);
        }

    }

    record $Unknown(String value) implements ComparisonOperator {
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

        private final class $Hidden implements ComparisonOperator {
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
     * Builder for {@link ComparisonOperator}.
     */
    final class Builder implements ShapeBuilder<ComparisonOperator> {
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
        public ComparisonOperator build() {
            return switch (value) {
                case "EQ" -> EQ;
                case "NE" -> NE;
                case "IN" -> IN;
                case "LE" -> LE;
                case "LT" -> LT;
                case "GE" -> GE;
                case "GT" -> GT;
                case "BETWEEN" -> BETWEEN;
                case "NOT_NULL" -> NOT_NULL;
                case "NULL" -> NULL;
                case "CONTAINS" -> CONTAINS;
                case "NOT_CONTAINS" -> NOT_CONTAINS;
                case "BEGINS_WITH" -> BEGINS_WITH;
                default -> new $Unknown(value);
            };
        }

        @Override
        public Builder deserialize(ShapeDeserializer de) {
            return value(de.readString($SCHEMA));
        }
    }
}
