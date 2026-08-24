package software.amazon.awssdk.benchmark.smithyjava.cloudwatch.model;

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
public sealed interface StatusCode extends SmithyEnum, SerializableShape {
    StatusCode COMPLETE = new CompleteType();
    StatusCode INTERNAL_ERROR = new InternalErrorType();
    StatusCode PARTIAL_DATA = new PartialDataType();
    StatusCode FORBIDDEN = new ForbiddenType();
    List<StatusCode> $TYPES = List.of(COMPLETE, INTERNAL_ERROR, PARTIAL_DATA, FORBIDDEN);

    Schema $SCHEMA = Schema.createEnum(ShapeId.from("com.amazonaws.cloudwatch#StatusCode"),
        Set.of(COMPLETE.getValue(), INTERNAL_ERROR.getValue(), PARTIAL_DATA.getValue(), FORBIDDEN.getValue()), StatusCode.class
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
    static StatusCode unknown(String value) {
        return new $Unknown(value);
    }

    /**
     * Returns an unmodifiable list containing the constants of this enum type, in the order declared.
     */
    static List<StatusCode> values() {
        return $TYPES;
    }

    /**
     * Returns a {@link StatusCode} constant with the specified value.
     *
     * @param value value to create {@code StatusCode} from.
     * @throws IllegalArgumentException if value does not match a known value.
     */
    static StatusCode from(String value) {
        return switch (value) {
            case "Complete" -> COMPLETE;
            case "InternalError" -> INTERNAL_ERROR;
            case "PartialData" -> PARTIAL_DATA;
            case "Forbidden" -> FORBIDDEN;
            default -> throw new IllegalArgumentException("Unknown value: " + value);
        };
    }

    final class CompleteType implements StatusCode {
        private CompleteType() {}

        @Override
        public String getValue() {
            return "Complete";
        }

        @Override
        public String toString() {
            return ToStringSerializer.serialize(this);
        }

    }

    final class InternalErrorType implements StatusCode {
        private InternalErrorType() {}

        @Override
        public String getValue() {
            return "InternalError";
        }

        @Override
        public String toString() {
            return ToStringSerializer.serialize(this);
        }

    }

    final class PartialDataType implements StatusCode {
        private PartialDataType() {}

        @Override
        public String getValue() {
            return "PartialData";
        }

        @Override
        public String toString() {
            return ToStringSerializer.serialize(this);
        }

    }

    final class ForbiddenType implements StatusCode {
        private ForbiddenType() {}

        @Override
        public String getValue() {
            return "Forbidden";
        }

        @Override
        public String toString() {
            return ToStringSerializer.serialize(this);
        }

    }

    record $Unknown(String value) implements StatusCode {
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

        private final class $Hidden implements StatusCode {
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
     * Builder for {@link StatusCode}.
     */
    final class Builder implements ShapeBuilder<StatusCode> {
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
        public StatusCode build() {
            return switch (value) {
                case "Complete" -> COMPLETE;
                case "InternalError" -> INTERNAL_ERROR;
                case "PartialData" -> PARTIAL_DATA;
                case "Forbidden" -> FORBIDDEN;
                default -> new $Unknown(value);
            };
        }

        @Override
        public Builder deserialize(ShapeDeserializer de) {
            return value(de.readString($SCHEMA));
        }
    }
}
