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
public sealed interface AnnotationDirective extends SmithyEnum, SerializableShape {
    AnnotationDirective COPY = new CopyType();
    AnnotationDirective EXCLUDE = new ExcludeType();
    List<AnnotationDirective> $TYPES = List.of(COPY, EXCLUDE);

    Schema $SCHEMA = Schema.createEnum(ShapeId.from("com.amazonaws.s3#AnnotationDirective"),
        Set.of(COPY.getValue(), EXCLUDE.getValue()), AnnotationDirective.class
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
    static AnnotationDirective unknown(String value) {
        return new $Unknown(value);
    }

    /**
     * Returns an unmodifiable list containing the constants of this enum type, in the order declared.
     */
    static List<AnnotationDirective> values() {
        return $TYPES;
    }

    /**
     * Returns a {@link AnnotationDirective} constant with the specified value.
     *
     * @param value value to create {@code AnnotationDirective} from.
     * @throws IllegalArgumentException if value does not match a known value.
     */
    static AnnotationDirective from(String value) {
        return switch (value) {
            case "COPY" -> COPY;
            case "EXCLUDE" -> EXCLUDE;
            default -> throw new IllegalArgumentException("Unknown value: " + value);
        };
    }

    final class CopyType implements AnnotationDirective {
        private CopyType() {}

        @Override
        public String getValue() {
            return "COPY";
        }

        @Override
        public String toString() {
            return ToStringSerializer.serialize(this);
        }

    }

    final class ExcludeType implements AnnotationDirective {
        private ExcludeType() {}

        @Override
        public String getValue() {
            return "EXCLUDE";
        }

        @Override
        public String toString() {
            return ToStringSerializer.serialize(this);
        }

    }

    record $Unknown(String value) implements AnnotationDirective {
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

        private final class $Hidden implements AnnotationDirective {
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
     * Builder for {@link AnnotationDirective}.
     */
    final class Builder implements ShapeBuilder<AnnotationDirective> {
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
        public AnnotationDirective build() {
            return switch (value) {
                case "COPY" -> COPY;
                case "EXCLUDE" -> EXCLUDE;
                default -> new $Unknown(value);
            };
        }

        @Override
        public Builder deserialize(ShapeDeserializer de) {
            return value(de.readString($SCHEMA));
        }
    }
}
