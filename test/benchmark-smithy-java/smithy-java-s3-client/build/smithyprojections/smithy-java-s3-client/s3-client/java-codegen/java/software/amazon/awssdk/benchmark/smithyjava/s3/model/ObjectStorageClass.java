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
public sealed interface ObjectStorageClass extends SmithyEnum, SerializableShape {
    ObjectStorageClass STANDARD = new StandardType();
    ObjectStorageClass REDUCED_REDUNDANCY = new ReducedRedundancyType();
    ObjectStorageClass GLACIER = new GlacierType();
    ObjectStorageClass STANDARD_IA = new StandardIaType();
    ObjectStorageClass ONEZONE_IA = new OnezoneIaType();
    ObjectStorageClass INTELLIGENT_TIERING = new IntelligentTieringType();
    ObjectStorageClass DEEP_ARCHIVE = new DeepArchiveType();
    ObjectStorageClass OUTPOSTS = new OutpostsType();
    ObjectStorageClass GLACIER_IR = new GlacierIrType();
    ObjectStorageClass SNOW = new SnowType();
    ObjectStorageClass EXPRESS_ONEZONE = new ExpressOnezoneType();
    ObjectStorageClass FSX_OPENZFS = new FsxOpenzfsType();
    ObjectStorageClass FSX_ONTAP = new FsxOntapType();
    List<ObjectStorageClass> $TYPES = List.of(STANDARD, REDUCED_REDUNDANCY, GLACIER, STANDARD_IA, ONEZONE_IA, INTELLIGENT_TIERING, DEEP_ARCHIVE, OUTPOSTS, GLACIER_IR, SNOW, EXPRESS_ONEZONE, FSX_OPENZFS, FSX_ONTAP);

    Schema $SCHEMA = Schema.createEnum(ShapeId.from("com.amazonaws.s3#ObjectStorageClass"),
        Set.of(STANDARD.getValue(), REDUCED_REDUNDANCY.getValue(), GLACIER.getValue(), STANDARD_IA.getValue(), ONEZONE_IA.getValue(), INTELLIGENT_TIERING.getValue(), DEEP_ARCHIVE.getValue(), OUTPOSTS.getValue(), GLACIER_IR.getValue(), SNOW.getValue(), EXPRESS_ONEZONE.getValue(), FSX_OPENZFS.getValue(), FSX_ONTAP.getValue()), ObjectStorageClass.class
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
    static ObjectStorageClass unknown(String value) {
        return new $Unknown(value);
    }

    /**
     * Returns an unmodifiable list containing the constants of this enum type, in the order declared.
     */
    static List<ObjectStorageClass> values() {
        return $TYPES;
    }

    /**
     * Returns a {@link ObjectStorageClass} constant with the specified value.
     *
     * @param value value to create {@code ObjectStorageClass} from.
     * @throws IllegalArgumentException if value does not match a known value.
     */
    static ObjectStorageClass from(String value) {
        return switch (value) {
            case "STANDARD" -> STANDARD;
            case "REDUCED_REDUNDANCY" -> REDUCED_REDUNDANCY;
            case "GLACIER" -> GLACIER;
            case "STANDARD_IA" -> STANDARD_IA;
            case "ONEZONE_IA" -> ONEZONE_IA;
            case "INTELLIGENT_TIERING" -> INTELLIGENT_TIERING;
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

    final class StandardType implements ObjectStorageClass {
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

    final class ReducedRedundancyType implements ObjectStorageClass {
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

    final class GlacierType implements ObjectStorageClass {
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

    final class StandardIaType implements ObjectStorageClass {
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

    final class OnezoneIaType implements ObjectStorageClass {
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

    final class IntelligentTieringType implements ObjectStorageClass {
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

    final class DeepArchiveType implements ObjectStorageClass {
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

    final class OutpostsType implements ObjectStorageClass {
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

    final class GlacierIrType implements ObjectStorageClass {
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

    final class SnowType implements ObjectStorageClass {
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

    final class ExpressOnezoneType implements ObjectStorageClass {
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

    final class FsxOpenzfsType implements ObjectStorageClass {
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

    final class FsxOntapType implements ObjectStorageClass {
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

    record $Unknown(String value) implements ObjectStorageClass {
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

        private final class $Hidden implements ObjectStorageClass {
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
     * Builder for {@link ObjectStorageClass}.
     */
    final class Builder implements ShapeBuilder<ObjectStorageClass> {
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
        public ObjectStorageClass build() {
            return switch (value) {
                case "STANDARD" -> STANDARD;
                case "REDUCED_REDUNDANCY" -> REDUCED_REDUNDANCY;
                case "GLACIER" -> GLACIER;
                case "STANDARD_IA" -> STANDARD_IA;
                case "ONEZONE_IA" -> ONEZONE_IA;
                case "INTELLIGENT_TIERING" -> INTELLIGENT_TIERING;
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
