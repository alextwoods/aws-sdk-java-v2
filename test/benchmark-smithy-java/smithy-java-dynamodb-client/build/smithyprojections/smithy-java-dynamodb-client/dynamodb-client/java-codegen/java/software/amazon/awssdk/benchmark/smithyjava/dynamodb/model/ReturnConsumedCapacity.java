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

/**
 * Determines the level of detail about either provisioned or on-demand throughput consumption that is returned in the
 * response:
 *
 * <ul>
 *   <li>
 *     <code>INDEXES</code> - The response includes the aggregate <code>ConsumedCapacity</code> for the operation,
 *     together with <code>ConsumedCapacity</code> for each table and secondary index that was accessed.Note that
 *     some operations, such as <code>GetItem</code> and <code>BatchGetItem</code>, do not access any indexes at
 *     all. In these cases, specifying <code>INDEXES</code> will only return <code>ConsumedCapacity</code>
 *     information for table(s).
 *   </li>
 *   <li>
 *     <code>TOTAL</code> - The response includes only the aggregate <code>ConsumedCapacity</code> for the
 *     operation.
 *   </li>
 *   <li>
 *     <code>NONE</code> - No <code>ConsumedCapacity</code> details are included in the response.
 *   </li>
 * </ul>
 */
@SmithyGenerated
public sealed interface ReturnConsumedCapacity extends SmithyEnum, SerializableShape {
    ReturnConsumedCapacity INDEXES = new IndexesType();
    ReturnConsumedCapacity TOTAL = new TotalType();
    ReturnConsumedCapacity NONE = new NoneType();
    List<ReturnConsumedCapacity> $TYPES = List.of(INDEXES, TOTAL, NONE);

    Schema $SCHEMA = Schema.createEnum(ShapeId.from("com.amazonaws.dynamodb#ReturnConsumedCapacity"),
        Set.of(INDEXES.getValue(), TOTAL.getValue(), NONE.getValue()), ReturnConsumedCapacity.class
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
    static ReturnConsumedCapacity unknown(String value) {
        return new $Unknown(value);
    }

    /**
     * Returns an unmodifiable list containing the constants of this enum type, in the order declared.
     */
    static List<ReturnConsumedCapacity> values() {
        return $TYPES;
    }

    /**
     * Returns a {@link ReturnConsumedCapacity} constant with the specified value.
     *
     * @param value value to create {@code ReturnConsumedCapacity} from.
     * @throws IllegalArgumentException if value does not match a known value.
     */
    static ReturnConsumedCapacity from(String value) {
        return switch (value) {
            case "INDEXES" -> INDEXES;
            case "TOTAL" -> TOTAL;
            case "NONE" -> NONE;
            default -> throw new IllegalArgumentException("Unknown value: " + value);
        };
    }

    final class IndexesType implements ReturnConsumedCapacity {
        private IndexesType() {}

        @Override
        public String getValue() {
            return "INDEXES";
        }

        @Override
        public String toString() {
            return ToStringSerializer.serialize(this);
        }

    }

    final class TotalType implements ReturnConsumedCapacity {
        private TotalType() {}

        @Override
        public String getValue() {
            return "TOTAL";
        }

        @Override
        public String toString() {
            return ToStringSerializer.serialize(this);
        }

    }

    final class NoneType implements ReturnConsumedCapacity {
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

    record $Unknown(String value) implements ReturnConsumedCapacity {
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

        private final class $Hidden implements ReturnConsumedCapacity {
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
     * Builder for {@link ReturnConsumedCapacity}.
     */
    final class Builder implements ShapeBuilder<ReturnConsumedCapacity> {
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
        public ReturnConsumedCapacity build() {
            return switch (value) {
                case "INDEXES" -> INDEXES;
                case "TOTAL" -> TOTAL;
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
