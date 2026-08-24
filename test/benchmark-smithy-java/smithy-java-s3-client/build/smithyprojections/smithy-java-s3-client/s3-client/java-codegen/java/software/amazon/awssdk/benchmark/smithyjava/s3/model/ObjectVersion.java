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
 * The version of an object.
 */
@SmithyGenerated
public final class ObjectVersion implements SerializableStruct {

    public static final Schema $SCHEMA = Schemas4.OBJECT_VERSION;
    private static final Schema $SCHEMA_E_TAG = $SCHEMA.member("ETag");
    private static final Schema $SCHEMA_CHECKSUM_ALGORITHM = $SCHEMA.member("ChecksumAlgorithm");
    private static final Schema $SCHEMA_CHECKSUM_TYPE = $SCHEMA.member("ChecksumType");
    private static final Schema $SCHEMA_SIZE = $SCHEMA.member("Size");
    private static final Schema $SCHEMA_STORAGE_CLASS = $SCHEMA.member("StorageClass");
    private static final Schema $SCHEMA_KEY = $SCHEMA.member("Key");
    private static final Schema $SCHEMA_VERSION_ID = $SCHEMA.member("VersionId");
    private static final Schema $SCHEMA_IS_LATEST = $SCHEMA.member("IsLatest");
    private static final Schema $SCHEMA_LAST_MODIFIED = $SCHEMA.member("LastModified");
    private static final Schema $SCHEMA_OWNER = $SCHEMA.member("Owner");
    private static final Schema $SCHEMA_RESTORE_STATUS = $SCHEMA.member("RestoreStatus");

    public static final ShapeId $ID = $SCHEMA.id();

    private final transient String eTag;
    private final transient List<ChecksumAlgorithm> checksumAlgorithm;
    private final transient ChecksumType checksumType;
    private final transient Long size;
    private final transient ObjectVersionStorageClass storageClass;
    private final transient String key;
    private final transient String versionId;
    private final transient Boolean isLatest;
    private final transient Instant lastModified;
    private final transient Owner owner;
    private final transient RestoreStatus restoreStatus;

    private ObjectVersion(Builder builder) {
        this.eTag = builder.eTag;
        this.checksumAlgorithm = builder.checksumAlgorithm == null ? null : Collections.unmodifiableList(builder.checksumAlgorithm);
        this.checksumType = builder.checksumType;
        this.size = builder.size;
        this.storageClass = builder.storageClass;
        this.key = builder.key;
        this.versionId = builder.versionId;
        this.isLatest = builder.isLatest;
        this.lastModified = builder.lastModified;
        this.owner = builder.owner;
        this.restoreStatus = builder.restoreStatus;
    }

    /**
     * The entity tag is an MD5 hash of that version of the object.
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
     * Size in bytes of the object.
     */
    public Long getSize() {
        return size;
    }

    /**
     * The class of storage used to store the object.
     */
    public ObjectVersionStorageClass getStorageClass() {
        return storageClass;
    }

    /**
     * The object key.
     */
    public String getKey() {
        return key;
    }

    /**
     * Version ID of an object.
     */
    public String getVersionId() {
        return versionId;
    }

    /**
     * Specifies whether the object is (true) or is not (false) the latest version of an object.
     */
    public Boolean isIsLatest() {
        return isLatest;
    }

    /**
     * Date and time when the object was last modified.
     */
    public Instant getLastModified() {
        return lastModified;
    }

    /**
     * Specifies the owner of the object.
     */
    public Owner getOwner() {
        return owner;
    }

    /**
     * Specifies the restoration status of an object. Objects in certain storage classes must be restored before they
     * can be retrieved. For more information about these storage classes and how to work with archived objects, see <a href="https://docs.aws.amazon.com/AmazonS3/latest/userguide/archived-objects.html">
     * Working with archived objects</a> in the <i>Amazon S3 User Guide</i>.
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
        ObjectVersion that = (ObjectVersion) other;
        return Objects.equals(this.isLatest, that.isLatest)
               && Objects.equals(this.size, that.size)
               && Objects.equals(this.eTag, that.eTag)
               && Objects.equals(this.key, that.key)
               && Objects.equals(this.versionId, that.versionId)
               && Objects.equals(this.checksumType, that.checksumType)
               && Objects.equals(this.storageClass, that.storageClass)
               && Objects.equals(this.lastModified, that.lastModified)
               && Objects.equals(this.owner, that.owner)
               && Objects.equals(this.restoreStatus, that.restoreStatus)
               && Objects.equals(this.checksumAlgorithm, that.checksumAlgorithm);
    }

    @Override
    public int hashCode() {
        int $hc = Objects.hashCode(eTag);
        $hc = 31 * $hc + Objects.hashCode(checksumAlgorithm);
        $hc = 31 * $hc + Objects.hashCode(checksumType);
        $hc = 31 * $hc + Objects.hashCode(size);
        $hc = 31 * $hc + Objects.hashCode(storageClass);
        $hc = 31 * $hc + Objects.hashCode(key);
        $hc = 31 * $hc + Objects.hashCode(versionId);
        $hc = 31 * $hc + Objects.hashCode(isLatest);
        $hc = 31 * $hc + Objects.hashCode(lastModified);
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
        if (key != null) {
            serializer.writeString($SCHEMA_KEY, key);
        }
        if (versionId != null) {
            serializer.writeString($SCHEMA_VERSION_ID, versionId);
        }
        if (isLatest != null) {
            serializer.writeBoolean($SCHEMA_IS_LATEST, isLatest);
        }
        if (lastModified != null) {
            serializer.writeTimestamp($SCHEMA_LAST_MODIFIED, lastModified);
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
            case 0 -> (T) SchemaUtils.validateSameMember($SCHEMA_E_TAG, member, eTag);
            case 1 -> (T) SchemaUtils.validateSameMember($SCHEMA_CHECKSUM_ALGORITHM, member, checksumAlgorithm);
            case 2 -> (T) SchemaUtils.validateSameMember($SCHEMA_CHECKSUM_TYPE, member, checksumType);
            case 3 -> (T) SchemaUtils.validateSameMember($SCHEMA_SIZE, member, size);
            case 4 -> (T) SchemaUtils.validateSameMember($SCHEMA_STORAGE_CLASS, member, storageClass);
            case 5 -> (T) SchemaUtils.validateSameMember($SCHEMA_KEY, member, key);
            case 6 -> (T) SchemaUtils.validateSameMember($SCHEMA_VERSION_ID, member, versionId);
            case 7 -> (T) SchemaUtils.validateSameMember($SCHEMA_IS_LATEST, member, isLatest);
            case 8 -> (T) SchemaUtils.validateSameMember($SCHEMA_LAST_MODIFIED, member, lastModified);
            case 9 -> (T) SchemaUtils.validateSameMember($SCHEMA_OWNER, member, owner);
            case 10 -> (T) SchemaUtils.validateSameMember($SCHEMA_RESTORE_STATUS, member, restoreStatus);
            default -> throw new IllegalArgumentException("Attempted to get non-existent member: " + member.id());
        };
    }

    /**
     * Create a new builder containing all the current property values of this object.
     *
     * <p><strong>Note:</strong> This method performs only a shallow copy of the original properties.
     *
     * @return a builder for {@link ObjectVersion}.
     */
    public Builder toBuilder() {
        var builder = new Builder();
        builder.eTag(this.eTag);
        builder.checksumAlgorithm(this.checksumAlgorithm);
        builder.checksumType(this.checksumType);
        builder.size(this.size);
        builder.storageClass(this.storageClass);
        builder.key(this.key);
        builder.versionId(this.versionId);
        builder.isLatest(this.isLatest);
        builder.lastModified(this.lastModified);
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
     * Builder for {@link ObjectVersion}.
     */
    public static final class Builder implements ShapeBuilder<ObjectVersion> {
        private String eTag;
        private List<ChecksumAlgorithm> checksumAlgorithm;
        private ChecksumType checksumType;
        private Long size;
        private ObjectVersionStorageClass storageClass;
        private String key;
        private String versionId;
        private Boolean isLatest;
        private Instant lastModified;
        private Owner owner;
        private RestoreStatus restoreStatus;

        private Builder() {}

        @Override
        public Schema schema() {
            return $SCHEMA;
        }

        /**
         * The entity tag is an MD5 hash of that version of the object.
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
         * Size in bytes of the object.
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
         * @return this builder.
         */
        public Builder storageClass(ObjectVersionStorageClass storageClass) {
            this.storageClass = storageClass;
            return this;
        }

        /**
         * The object key.
         *
         * @return this builder.
         */
        public Builder key(String key) {
            this.key = key;
            return this;
        }

        /**
         * Version ID of an object.
         *
         * @return this builder.
         */
        public Builder versionId(String versionId) {
            this.versionId = versionId;
            return this;
        }

        /**
         * Specifies whether the object is (true) or is not (false) the latest version of an object.
         *
         * @return this builder.
         */
        public Builder isLatest(Boolean isLatest) {
            this.isLatest = isLatest;
            return this;
        }

        /**
         * Date and time when the object was last modified.
         *
         * @return this builder.
         */
        public Builder lastModified(Instant lastModified) {
            this.lastModified = lastModified;
            return this;
        }

        /**
         * Specifies the owner of the object.
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
         * @return this builder.
         */
        public Builder restoreStatus(RestoreStatus restoreStatus) {
            this.restoreStatus = restoreStatus;
            return this;
        }

        @Override
        public ObjectVersion build() {
            return new ObjectVersion(this);
        }

        @Override
        @SuppressWarnings("unchecked")
        public void setMemberValue(Schema member, Object value) {
            switch (member.memberIndex()) {
                case 0 -> eTag((String) SchemaUtils.validateSameMember($SCHEMA_E_TAG, member, value));
                case 1 -> checksumAlgorithm((List<ChecksumAlgorithm>) SchemaUtils.validateSameMember($SCHEMA_CHECKSUM_ALGORITHM, member, value));
                case 2 -> checksumType((ChecksumType) SchemaUtils.validateSameMember($SCHEMA_CHECKSUM_TYPE, member, value));
                case 3 -> size((Long) SchemaUtils.validateSameMember($SCHEMA_SIZE, member, value));
                case 4 -> storageClass((ObjectVersionStorageClass) SchemaUtils.validateSameMember($SCHEMA_STORAGE_CLASS, member, value));
                case 5 -> key((String) SchemaUtils.validateSameMember($SCHEMA_KEY, member, value));
                case 6 -> versionId((String) SchemaUtils.validateSameMember($SCHEMA_VERSION_ID, member, value));
                case 7 -> isLatest((Boolean) SchemaUtils.validateSameMember($SCHEMA_IS_LATEST, member, value));
                case 8 -> lastModified((Instant) SchemaUtils.validateSameMember($SCHEMA_LAST_MODIFIED, member, value));
                case 9 -> owner((Owner) SchemaUtils.validateSameMember($SCHEMA_OWNER, member, value));
                case 10 -> restoreStatus((RestoreStatus) SchemaUtils.validateSameMember($SCHEMA_RESTORE_STATUS, member, value));
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
                    case 0 -> builder.eTag(de.readString(member));
                    case 1 -> builder.checksumAlgorithm(SharedSerde.deserializeChecksumAlgorithmList(member, de));
                    case 2 -> builder.checksumType(ChecksumType.builder().deserializeMember(de, member).build());
                    case 3 -> builder.size(de.readLong(member));
                    case 4 -> builder.storageClass(ObjectVersionStorageClass.builder().deserializeMember(de, member).build());
                    case 5 -> builder.key(de.readString(member));
                    case 6 -> builder.versionId(de.readString(member));
                    case 7 -> builder.isLatest(de.readBoolean(member));
                    case 8 -> builder.lastModified(de.readTimestamp(member));
                    case 9 -> builder.owner(Owner.builder().deserializeMember(de, member).build());
                    case 10 -> builder.restoreStatus(RestoreStatus.builder().deserializeMember(de, member).build());
                    default -> throw new IllegalArgumentException("Unexpected member: " + member.memberName());
                }
            }
        }
    }
}
