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
public sealed interface TransitionStorageClass extends SmithyEnum, SerializableShape {
    TransitionStorageClass GLACIER = new GlacierType();
    TransitionStorageClass STANDARD_IA = new StandardIaType();
    TransitionStorageClass ONEZONE_IA = new OnezoneIaType();
    TransitionStorageClass INTELLIGENT_TIERING = new IntelligentTieringType();
    TransitionStorageClass DEEP_ARCHIVE = new DeepArchiveType();
    TransitionStorageClass GLACIER_IR = new GlacierIrType();
    List<TransitionStorageClass> $TYPES = List.of(GLACIER, STANDARD_IA, ONEZONE_IA, INTELLIGENT_TIERING, DEEP_ARCHIVE, GLACIER_IR);

    Schema $SCHEMA = Schema.createEnum(ShapeId.from("com.amazonaws.s3#TransitionStorageClass"),
        Set.of(GLACIER.getValue(), STANDARD_IA.getValue(), ONEZONE_IA.getValue(), INTELLIGENT_TIERING.getValue(), DEEP_ARCHIVE.getValue(), GLACIER_IR.getValue()), TransitionStorageClass.class
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
    static TransitionStorageClass unknown(String value) {
        return new $Unknown(value);
    }

    /**
     * Returns an unmodifiable list containing the constants of this enum type, in the order declared.
     */
    static List<TransitionStorageClass> values() {
        return $TYPES;
    }

    /**
     * Returns a {@link TransitionStorageClass} constant with the specified value.
     *
     * @param value value to create {@code TransitionStorageClass} from.
     * @throws IllegalArgumentException if value does not match a known value.
     */
    static TransitionStorageClass from(String value) {
        return switch (value) {
            case "GLACIER" -> GLACIER;
            case "STANDARD_IA" -> STANDARD_IA;
            case "ONEZONE_IA" -> ONEZONE_IA;
            case "INTELLIGENT_TIERING" -> INTELLIGENT_TIERING;
            case "DEEP_ARCHIVE" -> DEEP_ARCHIVE;
            case "GLACIER_IR" -> GLACIER_IR;
            default -> throw new IllegalArgumentException("Unknown value: " + value);
        };
    }

    final class GlacierType implements TransitionStorageClass {
        private GlacierType() {}

        @Override
        public String getValue() {
            return "GLACIER";
        }

        @Override
        public String toString() {
            return ToStringSerializer.serialize(this);
        }

    }

    final class StandardIaType implements TransitionStorageClass {
        private StandardIaType() {}

        @Override
        public String getValue() {
            return "STANDARD_IA";
        }

        @Override
        public String toString() {
            return ToStringSerializer.serialize(this);
        }

    }

    final class OnezoneIaType implements TransitionStorageClass {
        private OnezoneIaType() {}

        @Override
        public String getValue() {
            return "ONEZONE_IA";
        }

        @Override
        public String toString() {
            return ToStringSerializer.serialize(this);
        }

    }

    final class IntelligentTieringType implements TransitionStorageClass {
        private IntelligentTieringType() {}

        @Override
        public String getValue() {
            return "INTELLIGENT_TIERING";
        }

        @Override
        public String toString() {
            return ToStringSerializer.serialize(this);
        }

    }

    final class DeepArchiveType implements TransitionStorageClass {
        private DeepArchiveType() {}

        @Override
        public String getValue() {
            return "DEEP_ARCHIVE";
        }

        @Override
        public String toString() {
            return ToStringSerializer.serialize(this);
        }

    }

    final class GlacierIrType implements TransitionStorageClass {
        private GlacierIrType() {}

        @Override
        public String getValue() {
            return "GLACIER_IR";
        }

        @Override
        public String toString() {
            return ToStringSerializer.serialize(this);
        }

    }

    record $Unknown(String value) implements TransitionStorageClass {
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

        private final class $Hidden implements TransitionStorageClass {
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
     * Builder for {@link TransitionStorageClass}.
     */
    final class Builder implements ShapeBuilder<TransitionStorageClass> {
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
        public TransitionStorageClass build() {
            return switch (value) {
                case "GLACIER" -> GLACIER;
                case "STANDARD_IA" -> STANDARD_IA;
                case "ONEZONE_IA" -> ONEZONE_IA;
                case "INTELLIGENT_TIERING" -> INTELLIGENT_TIERING;
                case "DEEP_ARCHIVE" -> DEEP_ARCHIVE;
                case "GLACIER_IR" -> GLACIER_IR;
                default -> new $Unknown(value);
            };
        }

        @Override
        public Builder deserialize(ShapeDeserializer de) {
            return value(de.readString($SCHEMA));
        }
    }
}
