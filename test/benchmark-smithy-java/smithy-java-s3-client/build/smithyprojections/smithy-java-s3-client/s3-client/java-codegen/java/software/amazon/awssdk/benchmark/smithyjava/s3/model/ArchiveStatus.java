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
public sealed interface ArchiveStatus extends SmithyEnum, SerializableShape {
    ArchiveStatus ARCHIVE_ACCESS = new ArchiveAccessType();
    ArchiveStatus DEEP_ARCHIVE_ACCESS = new DeepArchiveAccessType();
    List<ArchiveStatus> $TYPES = List.of(ARCHIVE_ACCESS, DEEP_ARCHIVE_ACCESS);

    Schema $SCHEMA = Schema.createEnum(ShapeId.from("com.amazonaws.s3#ArchiveStatus"),
        Set.of(ARCHIVE_ACCESS.getValue(), DEEP_ARCHIVE_ACCESS.getValue()), ArchiveStatus.class
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
    static ArchiveStatus unknown(String value) {
        return new $Unknown(value);
    }

    /**
     * Returns an unmodifiable list containing the constants of this enum type, in the order declared.
     */
    static List<ArchiveStatus> values() {
        return $TYPES;
    }

    /**
     * Returns a {@link ArchiveStatus} constant with the specified value.
     *
     * @param value value to create {@code ArchiveStatus} from.
     * @throws IllegalArgumentException if value does not match a known value.
     */
    static ArchiveStatus from(String value) {
        return switch (value) {
            case "ARCHIVE_ACCESS" -> ARCHIVE_ACCESS;
            case "DEEP_ARCHIVE_ACCESS" -> DEEP_ARCHIVE_ACCESS;
            default -> throw new IllegalArgumentException("Unknown value: " + value);
        };
    }

    final class ArchiveAccessType implements ArchiveStatus {
        private ArchiveAccessType() {}

        @Override
        public String getValue() {
            return "ARCHIVE_ACCESS";
        }

        @Override
        public String toString() {
            return ToStringSerializer.serialize(this);
        }

    }

    final class DeepArchiveAccessType implements ArchiveStatus {
        private DeepArchiveAccessType() {}

        @Override
        public String getValue() {
            return "DEEP_ARCHIVE_ACCESS";
        }

        @Override
        public String toString() {
            return ToStringSerializer.serialize(this);
        }

    }

    record $Unknown(String value) implements ArchiveStatus {
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

        private final class $Hidden implements ArchiveStatus {
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
     * Builder for {@link ArchiveStatus}.
     */
    final class Builder implements ShapeBuilder<ArchiveStatus> {
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
        public ArchiveStatus build() {
            return switch (value) {
                case "ARCHIVE_ACCESS" -> ARCHIVE_ACCESS;
                case "DEEP_ARCHIVE_ACCESS" -> DEEP_ARCHIVE_ACCESS;
                default -> new $Unknown(value);
            };
        }

        @Override
        public Builder deserialize(ShapeDeserializer de) {
            return value(de.readString($SCHEMA));
        }
    }
}
