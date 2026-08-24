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
 * Confirms that the requester knows that they will be charged for the request. Bucket owners need not specify this
 * parameter in their requests. If either the source or destination S3 bucket has Requester Pays enabled, the requester
 * will pay for the corresponding charges. For information about downloading objects from Requester Pays buckets, see <a href="https://docs.aws.amazon.com/AmazonS3/latest/dev/ObjectsinRequesterPaysBuckets.html">
 * Downloading Objects in Requester Pays Buckets</a> in the <i>Amazon S3 User Guide</i>.
 *
 * <p>This functionality is not supported for directory buckets.
 */
@SmithyGenerated
public sealed interface RequestPayer extends SmithyEnum, SerializableShape {
    RequestPayer REQUESTER = new RequesterType();
    List<RequestPayer> $TYPES = List.of(REQUESTER);

    Schema $SCHEMA = Schema.createEnum(ShapeId.from("com.amazonaws.s3#RequestPayer"),
        Set.of(REQUESTER.getValue()), RequestPayer.class
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
    static RequestPayer unknown(String value) {
        return new $Unknown(value);
    }

    /**
     * Returns an unmodifiable list containing the constants of this enum type, in the order declared.
     */
    static List<RequestPayer> values() {
        return $TYPES;
    }

    /**
     * Returns a {@link RequestPayer} constant with the specified value.
     *
     * @param value value to create {@code RequestPayer} from.
     * @throws IllegalArgumentException if value does not match a known value.
     */
    static RequestPayer from(String value) {
        return switch (value) {
            case "requester" -> REQUESTER;
            default -> throw new IllegalArgumentException("Unknown value: " + value);
        };
    }

    final class RequesterType implements RequestPayer {
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

    record $Unknown(String value) implements RequestPayer {
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

        private final class $Hidden implements RequestPayer {
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
     * Builder for {@link RequestPayer}.
     */
    final class Builder implements ShapeBuilder<RequestPayer> {
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
        public RequestPayer build() {
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
