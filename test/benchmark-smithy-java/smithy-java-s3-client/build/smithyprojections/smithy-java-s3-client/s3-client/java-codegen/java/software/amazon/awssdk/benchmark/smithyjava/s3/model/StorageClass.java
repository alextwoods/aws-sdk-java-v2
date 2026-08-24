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
public sealed interface StorageClass extends SmithyEnum, SerializableShape {
    StorageClass STANDARD = new StandardType();
    StorageClass REDUCED_REDUNDANCY = new ReducedRedundancyType();
    StorageClass STANDARD_IA = new StandardIaType();
    StorageClass ONEZONE_IA = new OnezoneIaType();
    StorageClass INTELLIGENT_TIERING = new IntelligentTieringType();
    StorageClass GLACIER = new GlacierType();
    StorageClass DEEP_ARCHIVE = new DeepArchiveType();
    StorageClass OUTPOSTS = new OutpostsType();
    StorageClass GLACIER_IR = new GlacierIrType();
    StorageClass SNOW = new SnowType();
    StorageClass EXPRESS_ONEZONE = new ExpressOnezoneType();
    StorageClass FSX_OPENZFS = new FsxOpenzfsType();
    StorageClass FSX_ONTAP = new FsxOntapType();
    List<StorageClass> $TYPES = List.of(STANDARD, REDUCED_REDUNDANCY, STANDARD_IA, ONEZONE_IA, INTELLIGENT_TIERING, GLACIER, DEEP_ARCHIVE, OUTPOSTS, GLACIER_IR, SNOW, EXPRESS_ONEZONE, FSX_OPENZFS, FSX_ONTAP);

    Schema $SCHEMA = Schema.createEnum(ShapeId.from("com.amazonaws.s3#StorageClass"),
        Set.of(STANDARD.getValue(), REDUCED_REDUNDANCY.getValue(), STANDARD_IA.getValue(), ONEZONE_IA.getValue(), INTELLIGENT_TIERING.getValue(), GLACIER.getValue(), DEEP_ARCHIVE.getValue(), OUTPOSTS.getValue(), GLACIER_IR.getValue(), SNOW.getValue(), EXPRESS_ONEZONE.getValue(), FSX_OPENZFS.getValue(), FSX_ONTAP.getValue()), StorageClass.class
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
    static StorageClass unknown(String value) {
        return new $Unknown(value);
    }

    /**
     * Returns an unmodifiable list containing the constants of this enum type, in the order declared.
     */
    static List<StorageClass> values() {
        return $TYPES;
    }

    /**
     * Returns a {@link StorageClass} constant with the specified value.
     *
     * @param value value to create {@code StorageClass} from.
     * @throws IllegalArgumentException if value does not match a known value.
     */
    static StorageClass from(String value) {
        return switch (value) {
            case "STANDARD" -> STANDARD;
            case "REDUCED_REDUNDANCY" -> REDUCED_REDUNDANCY;
            case "STANDARD_IA" -> STANDARD_IA;
            case "ONEZONE_IA" -> ONEZONE_IA;
            case "INTELLIGENT_TIERING" -> INTELLIGENT_TIERING;
            case "GLACIER" -> GLACIER;
            case "DEEP_ARCHIVE" -> DEEP_ARCHIVE;
            case "OUTPOSTS" -> OUTPOSTS;
            case "GLACIER_IR" -> GLACIER_IR;
            case "SNOW" -> SNOW;
            case "EXPRESS_ONEZONE" -> EXPRESS_ONEZONE;
            case "FSX_OPENZFS" -> FSX_OPENZFS;
            case "FSX_ONTAP" -> FSX_ONTAP;
            default -> throw new IllegalArgumentException("Unknown value: " + value);
        };
    }

    final class StandardType implements StorageClass {
        private StandardType() {}

        @Override
        public String getValue() {
            return "STANDARD";
        }

        @Override
        public String toString() {
            return ToStringSerializer.serialize(this);
        }

    }

    final class ReducedRedundancyType implements StorageClass {
        private ReducedRedundancyType() {}

        @Override
        public String getValue() {
            return "REDUCED_REDUNDANCY";
        }

        @Override
        public String toString() {
            return ToStringSerializer.serialize(this);
        }

    }

    final class StandardIaType implements StorageClass {
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

    final class OnezoneIaType implements StorageClass {
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

    final class IntelligentTieringType implements StorageClass {
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

    final class GlacierType implements StorageClass {
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

    final class DeepArchiveType implements StorageClass {
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

    final class OutpostsType implements StorageClass {
        private OutpostsType() {}

        @Override
        public String getValue() {
            return "OUTPOSTS";
        }

        @Override
        public String toString() {
            return ToStringSerializer.serialize(this);
        }

    }

    final class GlacierIrType implements StorageClass {
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

    final class SnowType implements StorageClass {
        private SnowType() {}

        @Override
        public String getValue() {
            return "SNOW";
        }

        @Override
        public String toString() {
            return ToStringSerializer.serialize(this);
        }

    }

    final class ExpressOnezoneType implements StorageClass {
        private ExpressOnezoneType() {}

        @Override
        public String getValue() {
            return "EXPRESS_ONEZONE";
        }

        @Override
        public String toString() {
            return ToStringSerializer.serialize(this);
        }

    }

    final class FsxOpenzfsType implements StorageClass {
        private FsxOpenzfsType() {}

        @Override
        public String getValue() {
            return "FSX_OPENZFS";
        }

        @Override
        public String toString() {
            return ToStringSerializer.serialize(this);
        }

    }

    final class FsxOntapType implements StorageClass {
        private FsxOntapType() {}

        @Override
        public String getValue() {
            return "FSX_ONTAP";
        }

        @Override
        public String toString() {
            return ToStringSerializer.serialize(this);
        }

    }

    record $Unknown(String value) implements StorageClass {
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

        private final class $Hidden implements StorageClass {
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
     * Builder for {@link StorageClass}.
     */
    final class Builder implements ShapeBuilder<StorageClass> {
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
        public StorageClass build() {
            return switch (value) {
                case "STANDARD" -> STANDARD;
                case "REDUCED_REDUNDANCY" -> REDUCED_REDUNDANCY;
                case "STANDARD_IA" -> STANDARD_IA;
                case "ONEZONE_IA" -> ONEZONE_IA;
                case "INTELLIGENT_TIERING" -> INTELLIGENT_TIERING;
                case "GLACIER" -> GLACIER;
                case "DEEP_ARCHIVE" -> DEEP_ARCHIVE;
                case "OUTPOSTS" -> OUTPOSTS;
                case "GLACIER_IR" -> GLACIER_IR;
                case "SNOW" -> SNOW;
                case "EXPRESS_ONEZONE" -> EXPRESS_ONEZONE;
                case "FSX_OPENZFS" -> FSX_OPENZFS;
                case "FSX_ONTAP" -> FSX_ONTAP;
                default -> new $Unknown(value);
            };
        }

        @Override
        public Builder deserialize(ShapeDeserializer de) {
            return value(de.readString($SCHEMA));
        }
    }
}
