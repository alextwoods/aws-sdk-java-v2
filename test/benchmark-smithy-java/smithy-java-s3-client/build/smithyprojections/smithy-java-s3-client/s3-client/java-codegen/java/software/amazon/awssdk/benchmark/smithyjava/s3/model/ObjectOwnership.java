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
 * The container element for object ownership for a bucket's ownership controls.
 *
 * <p><code>BucketOwnerPreferred</code> - Objects uploaded to the bucket change ownership to the bucket owner if the
 * objects are uploaded with the <code>bucket-owner-full-control</code> canned ACL.
 *
 * <p><code>ObjectWriter</code> - The uploading account will own the object if the object is uploaded with the <code>
 * bucket-owner-full-control</code> canned ACL.
 *
 * <p><code>BucketOwnerEnforced</code> - Access control lists (ACLs) are disabled and no longer affect permissions. The
 * bucket owner automatically owns and has full control over every object in the bucket. The bucket only accepts PUT
 * requests that don't specify an ACL or specify bucket owner full control ACLs (such as the predefined <code>
 * bucket-owner-full-control</code> canned ACL or a custom ACL in XML format that grants the same permissions).
 *
 * <p>By default, <code>ObjectOwnership</code> is set to <code>BucketOwnerEnforced</code> and ACLs are disabled. We
 * recommend keeping ACLs disabled, except in uncommon use cases where you must control access for each object
 * individually. For more information about S3 Object Ownership, see <a href="https://docs.aws.amazon.com/AmazonS3/latest/userguide/about-object-ownership.html">Controlling ownership of objects and disabling
 * ACLs for your bucket</a> in the <i>Amazon S3 User Guide</i>.
 *
 * <p>This functionality is not supported for directory buckets. Directory buckets use the bucket owner enforced setting
 * for S3 Object Ownership.
 */
@SmithyGenerated
public sealed interface ObjectOwnership extends SmithyEnum, SerializableShape {
    ObjectOwnership BUCKET_OWNER_PREFERRED = new BucketOwnerPreferredType();
    ObjectOwnership OBJECT_WRITER = new ObjectWriterType();
    ObjectOwnership BUCKET_OWNER_ENFORCED = new BucketOwnerEnforcedType();
    List<ObjectOwnership> $TYPES = List.of(BUCKET_OWNER_PREFERRED, OBJECT_WRITER, BUCKET_OWNER_ENFORCED);

    Schema $SCHEMA = Schema.createEnum(ShapeId.from("com.amazonaws.s3#ObjectOwnership"),
        Set.of(BUCKET_OWNER_PREFERRED.getValue(), OBJECT_WRITER.getValue(), BUCKET_OWNER_ENFORCED.getValue()), ObjectOwnership.class
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
    static ObjectOwnership unknown(String value) {
        return new $Unknown(value);
    }

    /**
     * Returns an unmodifiable list containing the constants of this enum type, in the order declared.
     */
    static List<ObjectOwnership> values() {
        return $TYPES;
    }

    /**
     * Returns a {@link ObjectOwnership} constant with the specified value.
     *
     * @param value value to create {@code ObjectOwnership} from.
     * @throws IllegalArgumentException if value does not match a known value.
     */
    static ObjectOwnership from(String value) {
        return switch (value) {
            case "BucketOwnerPreferred" -> BUCKET_OWNER_PREFERRED;
            case "ObjectWriter" -> OBJECT_WRITER;
            case "BucketOwnerEnforced" -> BUCKET_OWNER_ENFORCED;
            default -> throw new IllegalArgumentException("Unknown value: " + value);
        };
    }

    final class BucketOwnerPreferredType implements ObjectOwnership {
        private BucketOwnerPreferredType() {}

        @Override
        public String getValue() {
            return "BucketOwnerPreferred";
        }

        @Override
        public String toString() {
            return ToStringSerializer.serialize(this);
        }

    }

    final class ObjectWriterType implements ObjectOwnership {
        private ObjectWriterType() {}

        @Override
        public String getValue() {
            return "ObjectWriter";
        }

        @Override
        public String toString() {
            return ToStringSerializer.serialize(this);
        }

    }

    final class BucketOwnerEnforcedType implements ObjectOwnership {
        private BucketOwnerEnforcedType() {}

        @Override
        public String getValue() {
            return "BucketOwnerEnforced";
        }

        @Override
        public String toString() {
            return ToStringSerializer.serialize(this);
        }

    }

    record $Unknown(String value) implements ObjectOwnership {
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

        private final class $Hidden implements ObjectOwnership {
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
     * Builder for {@link ObjectOwnership}.
     */
    final class Builder implements ShapeBuilder<ObjectOwnership> {
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
        public ObjectOwnership build() {
            return switch (value) {
                case "BucketOwnerPreferred" -> BUCKET_OWNER_PREFERRED;
                case "ObjectWriter" -> OBJECT_WRITER;
                case "BucketOwnerEnforced" -> BUCKET_OWNER_ENFORCED;
                default -> new $Unknown(value);
            };
        }

        @Override
        public Builder deserialize(ShapeDeserializer de) {
            return value(de.readString($SCHEMA));
        }
    }
}
