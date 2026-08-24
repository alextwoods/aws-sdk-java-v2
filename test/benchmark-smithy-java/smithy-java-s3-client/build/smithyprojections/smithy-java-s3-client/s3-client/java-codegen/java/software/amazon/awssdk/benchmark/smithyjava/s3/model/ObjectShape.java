package software.amazon.awssdk.benchmark.smithyjava.s3.model;

import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import software.amazon.smithy.java.core.schema.Schema;
import software.amazon.smithy.java.core.schema.SchemaUtils;
import software.amazon.smithy.java.core.schema.SerializableStruct;
import software.amazon.smithy.java.core.schema.ShapeBuilder;
import software.amazon.smithy.java.core.serde.ShapeDeserializer;
import software.amazon.smithy.java.core.serde.ShapeSerializer;
import software.amazon.smithy.java.core.serde.ToStringSerializer;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.utils.SmithyGenerated;

/**
 * An object consists of data and its descriptive metadata.
 */
@SmithyGenerated
public final class ObjectShape implements SerializableStruct {

    public static final Schema $SCHEMA = Schemas4.OBJECT;
    private static final Schema $SCHEMA_KEY = $SCHEMA.member("Key");
    private static final Schema $SCHEMA_LAST_MODIFIED = $SCHEMA.member("LastModified");
    private static final Schema $SCHEMA_E_TAG = $SCHEMA.member("ETag");
    private static final Schema $SCHEMA_CHECKSUM_ALGORITHM = $SCHEMA.member("ChecksumAlgorithm");
    private static final Schema $SCHEMA_CHECKSUM_TYPE = $SCHEMA.member("ChecksumType");
    private static final Schema $SCHEMA_SIZE = $SCHEMA.member("Size");
    private static final Schema $SCHEMA_STORAGE_CLASS = $SCHEMA.member("StorageClass");
    private static final Schema $SCHEMA_OWNER = $SCHEMA.member("Owner");
    private static final Schema $SCHEMA_RESTORE_STATUS = $SCHEMA.member("RestoreStatus");

    public static final ShapeId $ID = $SCHEMA.id();

    private final transient String key;
    private final transient Instant lastModified;
    private final transient String eTag;
    private final transient List<ChecksumAlgorithm> checksumAlgorithm;
    private final transient ChecksumType checksumType;
    private final transient Long size;
    private final transient ObjectStorageClass storageClass;
    private final transient Owner owner;
    private final transient RestoreStatus restoreStatus;

    private ObjectShape(Builder builder) {
        this.key = builder.key;
        this.lastModified = builder.lastModified;
        this.eTag = builder.eTag;
        this.checksumAlgorithm = builder.checksumAlgorithm == null ? null : Collections.unmodifiableList(builder.checksumAlgorithm);
        this.checksumType = builder.checksumType;
        this.size = builder.size;
        this.storageClass = builder.storageClass;
        this.owner = builder.owner;
        this.restoreStatus = builder.restoreStatus;
    }

    /**
     * The name that you assign to an object. You use the object key to retrieve the object.
     */
    public String getKey() {
        return key;
    }

    /**
     * Creation date of the object.
     */
    public Instant getLastModified() {
        return lastModified;
    }

    /**
     * The entity tag is a hash of the object. The ETag reflects changes only to the contents of an object, not its
     * metadata. The ETag may or may not be an MD5 digest of the object data. Whether or not it is depends on how the
     * object was created and how it is encrypted as described below:
     *
     * <ul>
     *   <li>
     *     Objects created by the PUT Object, POST Object, or Copy operation, or through the Amazon Web Services
     *     Management Console, and are encrypted by SSE-S3 or plaintext, have ETags that are an MD5 digest of their
     *     object data.
     *   </li>
     *   <li>
     *     Objects created by the PUT Object, POST Object, or Copy operation, or through the Amazon Web Services
     *     Management Console, and are encrypted by SSE-C or SSE-KMS, have ETags that are not an MD5 digest of their
     *     object data.
     *   </li>
     *   <li>
     *     If an object is created by either the Multipart Upload or Part Copy operation, the ETag is not an MD5
     *     digest, regardless of the method of encryption. If an object is larger than 16 MB, the Amazon Web
     *     Services Management Console will upload or copy that object as a Multipart Upload, and therefore the ETag
     *     will not be an MD5 digest.
     *   </li>
     * </ul>
     *
     * <p><b>Directory buckets</b> - MD5 is not supported by directory buckets.
     */
    public String getETag() {
        return eTag;
    }

    /**
     * The algorithm that was used to create a checksum of the object.
     */
    public List<ChecksumAlgorithm> getChecksumAlgorithm() {
        if (checksumAlgorithm == null) {
            return Collections.emptyList();
        }
        return checksumAlgorithm;
    }

    public boolean hasChecksumAlgorithm() {
        return checksumAlgorithm != null;
    }

    /**
     * The checksum type that is used to calculate the object’s checksum value. For more information, see <a href="https://docs.aws.amazon.com/AmazonS3/latest/userguide/checking-object-integrity.html">Checking
     * object integrity</a> in the <i>Amazon S3 User Guide</i>.
     */
    public ChecksumType getChecksumType() {
        return checksumType;
    }

    /**
     * Size in bytes of the object
     */
    public Long getSize() {
        return size;
    }

    /**
     * The class of storage used to store the object.
     *
     * <p><b>Directory buckets</b> - Directory buckets only support <code>EXPRESS_ONEZONE</code> (the S3 Express One
     * Zone storage class) in Availability Zones and <code>ONEZONE_IA</code> (the S3 One Zone-Infrequent Access storage
     * class) in Dedicated Local Zones.
     */
    public ObjectStorageClass getStorageClass() {
        return storageClass;
    }

    /**
     * The owner of the object
     *
     * <p><b>Directory buckets</b> - The bucket owner is returned as the object owner.
     */
    public Owner getOwner() {
        return owner;
    }

    /**
     * Specifies the restoration status of an object. Objects in certain storage classes must be restored before they
     * can be retrieved. For more information about these storage classes and how to work with archived objects, see <a href="https://docs.aws.amazon.com/AmazonS3/latest/userguide/archived-objects.html">
     * Working with archived objects</a> in the <i>Amazon S3 User Guide</i>.
     *
     * <p>This functionality is not supported for directory buckets. Directory buckets only support <code>
     * EXPRESS_ONEZONE</code> (the S3 Express One Zone storage class) in Availability Zones and <code>ONEZONE_IA</code>
     * (the S3 One Zone-Infrequent Access storage class) in Dedicated Local Zones.
     */
    public RestoreStatus getRestoreStatus() {
        return restoreStatus;
    }

    @Override
    public String toString() {
        return ToStringSerializer.serialize(this);
    }

    @Override
    public boolean equals(Object other) {
        if (other == this) {
            return true;
        }
        if (other == null || getClass() != other.getClass()) {
            return false;
        }
        ObjectShape that = (ObjectShape) other;
        return Objects.equals(this.size, that.size)
               && Objects.equals(this.key, that.key)
               && Objects.equals(this.eTag, that.eTag)
               && Objects.equals(this.checksumType, that.checksumType)
               && Objects.equals(this.storageClass, that.storageClass)
               && Objects.equals(this.lastModified, that.lastModified)
               && Objects.equals(this.owner, that.owner)
               && Objects.equals(this.restoreStatus, that.restoreStatus)
               && Objects.equals(this.checksumAlgorithm, that.checksumAlgorithm);
    }

    @Override
    public int hashCode() {
        int $hc = Objects.hashCode(key);
        $hc = 31 * $hc + Objects.hashCode(lastModified);
        $hc = 31 * $hc + Objects.hashCode(eTag);
        $hc = 31 * $hc + Objects.hashCode(checksumAlgorithm);
        $hc = 31 * $hc + Objects.hashCode(checksumType);
        $hc = 31 * $hc + Objects.hashCode(size);
        $hc = 31 * $hc + Objects.hashCode(storageClass);
        $hc = 31 * $hc + Objects.hashCode(owner);
        $hc = 31 * $hc + Objects.hashCode(restoreStatus);
        return $hc;
    }

    @Override
    public Schema schema() {
        return $SCHEMA;
    }

    @Override
    public void serializeMembers(ShapeSerializer serializer) {
        if (key != null) {
            serializer.writeString($SCHEMA_KEY, key);
        }
        if (lastModified != null) {
            serializer.writeTimestamp($SCHEMA_LAST_MODIFIED, lastModified);
        }
        if (eTag != null) {
            serializer.writeString($SCHEMA_E_TAG, eTag);
        }
        if (checksumAlgorithm != null) {
            serializer.writeList($SCHEMA_CHECKSUM_ALGORITHM, checksumAlgorithm, checksumAlgorithm.size(), SharedSerde.ChecksumAlgorithmListSerializer.INSTANCE);
        }
        if (checksumType != null) {
            serializer.writeString($SCHEMA_CHECKSUM_TYPE, checksumType.getValue());
        }
        if (size != null) {
            serializer.writeLong($SCHEMA_SIZE, size);
        }
        if (storageClass != null) {
            serializer.writeString($SCHEMA_STORAGE_CLASS, storageClass.getValue());
        }
        if (owner != null) {
            serializer.writeStruct($SCHEMA_OWNER, owner);
        }
        if (restoreStatus != null) {
            serializer.writeStruct($SCHEMA_RESTORE_STATUS, restoreStatus);
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T getMemberValue(Schema member) {
        return switch (member.memberIndex()) {
            case 0 -> (T) SchemaUtils.validateSameMember($SCHEMA_KEY, member, key);
            case 1 -> (T) SchemaUtils.validateSameMember($SCHEMA_LAST_MODIFIED, member, lastModified);
            case 2 -> (T) SchemaUtils.validateSameMember($SCHEMA_E_TAG, member, eTag);
            case 3 -> (T) SchemaUtils.validateSameMember($SCHEMA_CHECKSUM_ALGORITHM, member, checksumAlgorithm);
            case 4 -> (T) SchemaUtils.validateSameMember($SCHEMA_CHECKSUM_TYPE, member, checksumType);
            case 5 -> (T) SchemaUtils.validateSameMember($SCHEMA_SIZE, member, size);
            case 6 -> (T) SchemaUtils.validateSameMember($SCHEMA_STORAGE_CLASS, member, storageClass);
            case 7 -> (T) SchemaUtils.validateSameMember($SCHEMA_OWNER, member, owner);
            case 8 -> (T) SchemaUtils.validateSameMember($SCHEMA_RESTORE_STATUS, member, restoreStatus);
            default -> throw new IllegalArgumentException("Attempted to get non-existent member: " + member.id());
        };
    }

    /**
     * Create a new builder containing all the current property values of this object.
     *
     * <p><strong>Note:</strong> This method performs only a shallow copy of the original properties.
     *
     * @return a builder for {@link ObjectShape}.
     */
    public Builder toBuilder() {
        var builder = new Builder();
        builder.key(this.key);
        builder.lastModified(this.lastModified);
        builder.eTag(this.eTag);
        builder.checksumAlgorithm(this.checksumAlgorithm);
        builder.checksumType(this.checksumType);
        builder.size(this.size);
        builder.storageClass(this.storageClass);
        builder.owner(this.owner);
        builder.restoreStatus(this.restoreStatus);
        return builder;
    }

    /**
     * @return returns a new Builder.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for {@link ObjectShape}.
     */
    public static final class Builder implements ShapeBuilder<ObjectShape> {
        private String key;
        private Instant lastModified;
        private String eTag;
        private List<ChecksumAlgorithm> checksumAlgorithm;
        private ChecksumType checksumType;
        private Long size;
        private ObjectStorageClass storageClass;
        private Owner owner;
        private RestoreStatus restoreStatus;

        private Builder() {}

        @Override
        public Schema schema() {
            return $SCHEMA;
        }

        /**
         * The name that you assign to an object. You use the object key to retrieve the object.
         *
         * @return this builder.
         */
        public Builder key(String key) {
            this.key = key;
            return this;
        }

        /**
         * Creation date of the object.
         *
         * @return this builder.
         */
        public Builder lastModified(Instant lastModified) {
            this.lastModified = lastModified;
            return this;
        }

        /**
         * The entity tag is a hash of the object. The ETag reflects changes only to the contents of an object, not its
         * metadata. The ETag may or may not be an MD5 digest of the object data. Whether or not it is depends on how the
         * object was created and how it is encrypted as described below:
         *
         * <ul>
         *   <li>
         *     Objects created by the PUT Object, POST Object, or Copy operation, or through the Amazon Web Services
         *     Management Console, and are encrypted by SSE-S3 or plaintext, have ETags that are an MD5 digest of their
         *     object data.
         *   </li>
         *   <li>
         *     Objects created by the PUT Object, POST Object, or Copy operation, or through the Amazon Web Services
         *     Management Console, and are encrypted by SSE-C or SSE-KMS, have ETags that are not an MD5 digest of their
         *     object data.
         *   </li>
         *   <li>
         *     If an object is created by either the Multipart Upload or Part Copy operation, the ETag is not an MD5
         *     digest, regardless of the method of encryption. If an object is larger than 16 MB, the Amazon Web
         *     Services Management Console will upload or copy that object as a Multipart Upload, and therefore the ETag
         *     will not be an MD5 digest.
         *   </li>
         * </ul>
         *
         * <p><b>Directory buckets</b> - MD5 is not supported by directory buckets.
         *
         * @return this builder.
         */
        public Builder eTag(String eTag) {
            this.eTag = eTag;
            return this;
        }

        /**
         * The algorithm that was used to create a checksum of the object.
         *
         * @return this builder.
         */
        public Builder checksumAlgorithm(List<ChecksumAlgorithm> checksumAlgorithm) {
            this.checksumAlgorithm = checksumAlgorithm;
            return this;
        }

        /**
         * The checksum type that is used to calculate the object’s checksum value. For more information, see <a href="https://docs.aws.amazon.com/AmazonS3/latest/userguide/checking-object-integrity.html">Checking
         * object integrity</a> in the <i>Amazon S3 User Guide</i>.
         *
         * @return this builder.
         */
        public Builder checksumType(ChecksumType checksumType) {
            this.checksumType = checksumType;
            return this;
        }

        /**
         * Size in bytes of the object
         *
         * @return this builder.
         */
        public Builder size(Long size) {
            this.size = size;
            return this;
        }

        /**
         * The class of storage used to store the object.
         *
         * <p><b>Directory buckets</b> - Directory buckets only support <code>EXPRESS_ONEZONE</code> (the S3 Express One
         * Zone storage class) in Availability Zones and <code>ONEZONE_IA</code> (the S3 One Zone-Infrequent Access storage
         * class) in Dedicated Local Zones.
         *
         * @return this builder.
         */
        public Builder storageClass(ObjectStorageClass storageClass) {
            this.storageClass = storageClass;
            return this;
        }

        /**
         * The owner of the object
         *
         * <p><b>Directory buckets</b> - The bucket owner is returned as the object owner.
         *
         * @return this builder.
         */
        public Builder owner(Owner owner) {
            this.owner = owner;
            return this;
        }

        /**
         * Specifies the restoration status of an object. Objects in certain storage classes must be restored before they
         * can be retrieved. For more information about these storage classes and how to work with archived objects, see <a href="https://docs.aws.amazon.com/AmazonS3/latest/userguide/archived-objects.html">
         * Working with archived objects</a> in the <i>Amazon S3 User Guide</i>.
         *
         * <p>This functionality is not supported for directory buckets. Directory buckets only support <code>
         * EXPRESS_ONEZONE</code> (the S3 Express One Zone storage class) in Availability Zones and <code>ONEZONE_IA</code>
         * (the S3 One Zone-Infrequent Access storage class) in Dedicated Local Zones.
         *
         * @return this builder.
         */
        public Builder restoreStatus(RestoreStatus restoreStatus) {
            this.restoreStatus = restoreStatus;
            return this;
        }

        @Override
        public ObjectShape build() {
            return new ObjectShape(this);
        }

        @Override
        @SuppressWarnings("unchecked")
        public void setMemberValue(Schema member, Object value) {
            switch (member.memberIndex()) {
                case 0 -> key((String) SchemaUtils.validateSameMember($SCHEMA_KEY, member, value));
                case 1 -> lastModified((Instant) SchemaUtils.validateSameMember($SCHEMA_LAST_MODIFIED, member, value));
                case 2 -> eTag((String) SchemaUtils.validateSameMember($SCHEMA_E_TAG, member, value));
                case 3 -> checksumAlgorithm((List<ChecksumAlgorithm>) SchemaUtils.validateSameMember($SCHEMA_CHECKSUM_ALGORITHM, member, value));
                case 4 -> checksumType((ChecksumType) SchemaUtils.validateSameMember($SCHEMA_CHECKSUM_TYPE, member, value));
                case 5 -> size((Long) SchemaUtils.validateSameMember($SCHEMA_SIZE, member, value));
                case 6 -> storageClass((ObjectStorageClass) SchemaUtils.validateSameMember($SCHEMA_STORAGE_CLASS, member, value));
                case 7 -> owner((Owner) SchemaUtils.validateSameMember($SCHEMA_OWNER, member, value));
                case 8 -> restoreStatus((RestoreStatus) SchemaUtils.validateSameMember($SCHEMA_RESTORE_STATUS, member, value));
                default -> ShapeBuilder.super.setMemberValue(member, value);
            }
        }

        @Override
        public Builder deserialize(ShapeDeserializer decoder) {
            decoder.readStruct($SCHEMA, this, $InnerDeserializer.INSTANCE);
            return this;
        }

        @Override
        public Builder deserializeMember(ShapeDeserializer decoder, Schema schema) {
            decoder.readStruct(schema.assertMemberTargetIs($SCHEMA), this, $InnerDeserializer.INSTANCE);
            return this;
        }

        private static final class $InnerDeserializer implements ShapeDeserializer.StructMemberConsumer<Builder> {
            private static final $InnerDeserializer INSTANCE = new $InnerDeserializer();

            @Override
            @SuppressWarnings("unchecked")
            public void accept(Builder builder, Schema member, ShapeDeserializer de) {
                switch (member.memberIndex()) {
                    case 0 -> builder.key(de.readString(member));
                    case 1 -> builder.lastModified(de.readTimestamp(member));
                    case 2 -> builder.eTag(de.readString(member));
                    case 3 -> builder.checksumAlgorithm(SharedSerde.deserializeChecksumAlgorithmList(member, de));
                    case 4 -> builder.checksumType(ChecksumType.builder().deserializeMember(de, member).build());
                    case 5 -> builder.size(de.readLong(member));
                    case 6 -> builder.storageClass(ObjectStorageClass.builder().deserializeMember(de, member).build());
                    case 7 -> builder.owner(Owner.builder().deserializeMember(de, member).build());
                    case 8 -> builder.restoreStatus(RestoreStatus.builder().deserializeMember(de, member).build());
                    default -> throw new IllegalArgumentException("Unexpected member: " + member.memberName());
                }
            }
        }
    }
}
