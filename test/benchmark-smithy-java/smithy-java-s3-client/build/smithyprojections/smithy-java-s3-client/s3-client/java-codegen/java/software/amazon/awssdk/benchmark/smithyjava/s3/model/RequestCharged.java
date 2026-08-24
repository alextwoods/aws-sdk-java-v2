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

/**
 * If present, indicates that the requester was successfully charged for the request. For more information, see <a href="https://docs.aws.amazon.com/AmazonS3/latest/userguide/RequesterPaysBuckets.html">Using
 * Requester Pays buckets for storage transfers and usage</a> in the <i>Amazon Simple Storage Service user guide</i>.
 *
 * <p>This functionality is not supported for directory buckets.
 */
@SmithyGenerated
public sealed interface RequestCharged extends SmithyEnum, SerializableShape {
    RequestCharged REQUESTER = new RequesterType();
    List<RequestCharged> $TYPES = List.of(REQUESTER);

    Schema $SCHEMA = Schema.createEnum(ShapeId.from("com.amazonaws.s3#RequestCharged"),
        Set.of(REQUESTER.getValue()), RequestCharged.class
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
    static RequestCharged unknown(String value) {
        return new $Unknown(value);
    }

    /**
     * Returns an unmodifiable list containing the constants of this enum type, in the order declared.
     */
    static List<RequestCharged> values() {
        return $TYPES;
    }

    /**
     * Returns a {@link RequestCharged} constant with the specified value.
     *
     * @param value value to create {@code RequestCharged} from.
     * @throws IllegalArgumentException if value does not match a known value.
     */
    static RequestCharged from(String value) {
        return switch (value) {
            case "requester" -> REQUESTER;
            default -> throw new IllegalArgumentException("Unknown value: " + value);
        };
    }

    final class RequesterType implements RequestCharged {
        private RequesterType() {}

        @Override
        public String getValue() {
            return "requester";
        }

        @Override
        public String toString() {
            return ToStringSerializer.serialize(this);
        }

    }

    record $Unknown(String value) implements RequestCharged {
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

        private final class $Hidden implements RequestCharged {
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
     * Builder for {@link RequestCharged}.
     */
    final class Builder implements ShapeBuilder<RequestCharged> {
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
        public RequestCharged build() {
            return switch (value) {
                case "requester" -> REQUESTER;
                default -> new $Unknown(value);
            };
        }

        @Override
        public Builder deserialize(ShapeDeserializer de) {
            return value(de.readString($SCHEMA));
        }
    }
}
