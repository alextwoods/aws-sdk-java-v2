package software.amazon.awssdk.benchmark.smithyjava.s3.model;

import java.time.Instant;
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
 * Container for the <code>MultipartUpload</code> for the Amazon S3 object.
 */
@SmithyGenerated
public final class MultipartUpload implements SerializableStruct {

    public static final Schema $SCHEMA = Schemas4.MULTIPART_UPLOAD;
    private static final Schema $SCHEMA_UPLOAD_ID = $SCHEMA.member("UploadId");
    private static final Schema $SCHEMA_KEY = $SCHEMA.member("Key");
    private static final Schema $SCHEMA_INITIATED = $SCHEMA.member("Initiated");
    private static final Schema $SCHEMA_STORAGE_CLASS = $SCHEMA.member("StorageClass");
    private static final Schema $SCHEMA_OWNER = $SCHEMA.member("Owner");
    private static final Schema $SCHEMA_INITIATOR = $SCHEMA.member("Initiator");
    private static final Schema $SCHEMA_CHECKSUM_ALGORITHM = $SCHEMA.member("ChecksumAlgorithm");
    private static final Schema $SCHEMA_CHECKSUM_TYPE = $SCHEMA.member("ChecksumType");

    public static final ShapeId $ID = $SCHEMA.id();

    private final transient String uploadId;
    private final transient String key;
    private final transient Instant initiated;
    private final transient StorageClass storageClass;
    private final transient Owner owner;
    private final transient Initiator initiator;
    private final transient ChecksumAlgorithm checksumAlgorithm;
    private final transient ChecksumType checksumType;

    private MultipartUpload(Builder builder) {
        this.uploadId = builder.uploadId;
        this.key = builder.key;
        this.initiated = builder.initiated;
        this.storageClass = builder.storageClass;
        this.owner = builder.owner;
        this.initiator = builder.initiator;
        this.checksumAlgorithm = builder.checksumAlgorithm;
        this.checksumType = builder.checksumType;
    }

    /**
     * Upload ID that identifies the multipart upload.
     */
    public String getUploadId() {
        return uploadId;
    }

    /**
     * Key of the object for which the multipart upload was initiated.
     */
    public String getKey() {
        return key;
    }

    /**
     * Date and time at which the multipart upload was initiated.
     */
    public Instant getInitiated() {
        return initiated;
    }

    /**
     * The class of storage used to store the object.
     *
     * <p><b>Directory buckets</b> - Directory buckets only support <code>EXPRESS_ONEZONE</code> (the S3 Express One
     * Zone storage class) in Availability Zones and <code>ONEZONE_IA</code> (the S3 One Zone-Infrequent Access storage
     * class) in Dedicated Local Zones.
     */
    public StorageClass getStorageClass() {
        return storageClass;
    }

    /**
     * Specifies the owner of the object that is part of the multipart upload.
     *
     * <p><b>Directory buckets</b> - The bucket owner is returned as the object owner for all the objects.
     */
    public Owner getOwner() {
        return owner;
    }

    /**
     * Identifies who initiated the multipart upload.
     */
    public Initiator getInitiator() {
        return initiator;
    }

    /**
     * The algorithm that was used to create a checksum of the object.
     */
    public ChecksumAlgorithm getChecksumAlgorithm() {
        return checksumAlgorithm;
    }

    /**
     * The checksum type that is used to calculate the object’s checksum value. For more information, see <a href="https://docs.aws.amazon.com/AmazonS3/latest/userguide/checking-object-integrity.html">Checking
     * object integrity</a> in the <i>Amazon S3 User Guide</i>.
     */
    public ChecksumType getChecksumType() {
        return checksumType;
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
        MultipartUpload that = (MultipartUpload) other;
        return Objects.equals(this.uploadId, that.uploadId)
               && Objects.equals(this.key, that.key)
               && Objects.equals(this.storageClass, that.storageClass)
               && Objects.equals(this.checksumAlgorithm, that.checksumAlgorithm)
               && Objects.equals(this.checksumType, that.checksumType)
               && Objects.equals(this.initiated, that.initiated)
               && Objects.equals(this.owner, that.owner)
               && Objects.equals(this.initiator, that.initiator);
    }

    @Override
    public int hashCode() {
        int $hc = Objects.hashCode(uploadId);
        $hc = 31 * $hc + Objects.hashCode(key);
        $hc = 31 * $hc + Objects.hashCode(initiated);
        $hc = 31 * $hc + Objects.hashCode(storageClass);
        $hc = 31 * $hc + Objects.hashCode(owner);
        $hc = 31 * $hc + Objects.hashCode(initiator);
        $hc = 31 * $hc + Objects.hashCode(checksumAlgorithm);
        $hc = 31 * $hc + Objects.hashCode(checksumType);
        return $hc;
    }

    @Override
    public Schema schema() {
        return $SCHEMA;
    }

    @Override
    public void serializeMembers(ShapeSerializer serializer) {
        if (uploadId != null) {
            serializer.writeString($SCHEMA_UPLOAD_ID, uploadId);
        }
        if (key != null) {
            serializer.writeString($SCHEMA_KEY, key);
        }
        if (initiated != null) {
            serializer.writeTimestamp($SCHEMA_INITIATED, initiated);
        }
        if (storageClass != null) {
            serializer.writeString($SCHEMA_STORAGE_CLASS, storageClass.getValue());
        }
        if (owner != null) {
            serializer.writeStruct($SCHEMA_OWNER, owner);
        }
        if (initiator != null) {
            serializer.writeStruct($SCHEMA_INITIATOR, initiator);
        }
        if (checksumAlgorithm != null) {
            serializer.writeString($SCHEMA_CHECKSUM_ALGORITHM, checksumAlgorithm.getValue());
        }
        if (checksumType != null) {
            serializer.writeString($SCHEMA_CHECKSUM_TYPE, checksumType.getValue());
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T getMemberValue(Schema member) {
        return switch (member.memberIndex()) {
            case 0 -> (T) SchemaUtils.validateSameMember($SCHEMA_UPLOAD_ID, member, uploadId);
            case 1 -> (T) SchemaUtils.validateSameMember($SCHEMA_KEY, member, key);
            case 2 -> (T) SchemaUtils.validateSameMember($SCHEMA_INITIATED, member, initiated);
            case 3 -> (T) SchemaUtils.validateSameMember($SCHEMA_STORAGE_CLASS, member, storageClass);
            case 4 -> (T) SchemaUtils.validateSameMember($SCHEMA_OWNER, member, owner);
            case 5 -> (T) SchemaUtils.validateSameMember($SCHEMA_INITIATOR, member, initiator);
            case 6 -> (T) SchemaUtils.validateSameMember($SCHEMA_CHECKSUM_ALGORITHM, member, checksumAlgorithm);
            case 7 -> (T) SchemaUtils.validateSameMember($SCHEMA_CHECKSUM_TYPE, member, checksumType);
            default -> throw new IllegalArgumentException("Attempted to get non-existent member: " + member.id());
        };
    }

    /**
     * Create a new builder containing all the current property values of this object.
     *
     * <p><strong>Note:</strong> This method performs only a shallow copy of the original properties.
     *
     * @return a builder for {@link MultipartUpload}.
     */
    public Builder toBuilder() {
        var builder = new Builder();
        builder.uploadId(this.uploadId);
        builder.key(this.key);
        builder.initiated(this.initiated);
        builder.storageClass(this.storageClass);
        builder.owner(this.owner);
        builder.initiator(this.initiator);
        builder.checksumAlgorithm(this.checksumAlgorithm);
        builder.checksumType(this.checksumType);
        return builder;
    }

    /**
     * @return returns a new Builder.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for {@link MultipartUpload}.
     */
    public static final class Builder implements ShapeBuilder<MultipartUpload> {
        private String uploadId;
        private String key;
        private Instant initiated;
        private StorageClass storageClass;
        private Owner owner;
        private Initiator initiator;
        private ChecksumAlgorithm checksumAlgorithm;
        private ChecksumType checksumType;

        private Builder() {}

        @Override
        public Schema schema() {
            return $SCHEMA;
        }

        /**
         * Upload ID that identifies the multipart upload.
         *
         * @return this builder.
         */
        public Builder uploadId(String uploadId) {
            this.uploadId = uploadId;
            return this;
        }

        /**
         * Key of the object for which the multipart upload was initiated.
         *
         * @return this builder.
         */
        public Builder key(String key) {
            this.key = key;
            return this;
        }

        /**
         * Date and time at which the multipart upload was initiated.
         *
         * @return this builder.
         */
        public Builder initiated(Instant initiated) {
            this.initiated = initiated;
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
        public Builder storageClass(StorageClass storageClass) {
            this.storageClass = storageClass;
            return this;
        }

        /**
         * Specifies the owner of the object that is part of the multipart upload.
         *
         * <p><b>Directory buckets</b> - The bucket owner is returned as the object owner for all the objects.
         *
         * @return this builder.
         */
        public Builder owner(Owner owner) {
            this.owner = owner;
            return this;
        }

        /**
         * Identifies who initiated the multipart upload.
         *
         * @return this builder.
         */
        public Builder initiator(Initiator initiator) {
            this.initiator = initiator;
            return this;
        }

        /**
         * The algorithm that was used to create a checksum of the object.
         *
         * @return this builder.
         */
        public Builder checksumAlgorithm(ChecksumAlgorithm checksumAlgorithm) {
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

        @Override
        public MultipartUpload build() {
            return new MultipartUpload(this);
        }

        @Override
        @SuppressWarnings("unchecked")
        public void setMemberValue(Schema member, Object value) {
            switch (member.memberIndex()) {
                case 0 -> uploadId((String) SchemaUtils.validateSameMember($SCHEMA_UPLOAD_ID, member, value));
                case 1 -> key((String) SchemaUtils.validateSameMember($SCHEMA_KEY, member, value));
                case 2 -> initiated((Instant) SchemaUtils.validateSameMember($SCHEMA_INITIATED, member, value));
                case 3 -> storageClass((StorageClass) SchemaUtils.validateSameMember($SCHEMA_STORAGE_CLASS, member, value));
                case 4 -> owner((Owner) SchemaUtils.validateSameMember($SCHEMA_OWNER, member, value));
                case 5 -> initiator((Initiator) SchemaUtils.validateSameMember($SCHEMA_INITIATOR, member, value));
                case 6 -> checksumAlgorithm((ChecksumAlgorithm) SchemaUtils.validateSameMember($SCHEMA_CHECKSUM_ALGORITHM, member, value));
                case 7 -> checksumType((ChecksumType) SchemaUtils.validateSameMember($SCHEMA_CHECKSUM_TYPE, member, value));
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
                    case 0 -> builder.uploadId(de.readString(member));
                    case 1 -> builder.key(de.readString(member));
                    case 2 -> builder.initiated(de.readTimestamp(member));
                    case 3 -> builder.storageClass(StorageClass.builder().deserializeMember(de, member).build());
                    case 4 -> builder.owner(Owner.builder().deserializeMember(de, member).build());
                    case 5 -> builder.initiator(Initiator.builder().deserializeMember(de, member).build());
                    case 6 -> builder.checksumAlgorithm(ChecksumAlgorithm.builder().deserializeMember(de, member).build());
                    case 7 -> builder.checksumType(ChecksumType.builder().deserializeMember(de, member).build());
                    default -> throw new IllegalArgumentException("Unexpected member: " + member.memberName());
                }
            }
        }
    }
}
