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
public sealed interface FilterRuleName extends SmithyEnum, SerializableShape {
    FilterRuleName PREFIX = new PrefixType();
    FilterRuleName SUFFIX = new SuffixType();
    List<FilterRuleName> $TYPES = List.of(PREFIX, SUFFIX);

    Schema $SCHEMA = Schema.createEnum(ShapeId.from("com.amazonaws.s3#FilterRuleName"),
        Set.of(PREFIX.getValue(), SUFFIX.getValue()), FilterRuleName.class
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
    static FilterRuleName unknown(String value) {
        return new $Unknown(value);
    }

    /**
     * Returns an unmodifiable list containing the constants of this enum type, in the order declared.
     */
    static List<FilterRuleName> values() {
        return $TYPES;
    }

    /**
     * Returns a {@link FilterRuleName} constant with the specified value.
     *
     * @param value value to create {@code FilterRuleName} from.
     * @throws IllegalArgumentException if value does not match a known value.
     */
    static FilterRuleName from(String value) {
        return switch (value) {
            case "prefix" -> PREFIX;
            case "suffix" -> SUFFIX;
            default -> throw new IllegalArgumentException("Unknown value: " + value);
        };
    }

    final class PrefixType implements FilterRuleName {
        private PrefixType() {}

        @Override
        public String getValue() {
            return "prefix";
        }

        @Override
        public String toString() {
            return ToStringSerializer.serialize(this);
        }

    }

    final class SuffixType implements FilterRuleName {
        private SuffixType() {}

        @Override
        public String getValue() {
            return "suffix";
        }

        @Override
        public String toString() {
            return ToStringSerializer.serialize(this);
        }

    }

    record $Unknown(String value) implements FilterRuleName {
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

        private final class $Hidden implements FilterRuleName {
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
     * Builder for {@link FilterRuleName}.
     */
    final class Builder implements ShapeBuilder<FilterRuleName> {
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
        public FilterRuleName build() {
            return switch (value) {
                case "prefix" -> PREFIX;
                case "suffix" -> SUFFIX;
                default -> new $Unknown(value);
            };
        }

        @Override
        public Builder deserialize(ShapeDeserializer de) {
            return value(de.readString($SCHEMA));
        }
    }
}
