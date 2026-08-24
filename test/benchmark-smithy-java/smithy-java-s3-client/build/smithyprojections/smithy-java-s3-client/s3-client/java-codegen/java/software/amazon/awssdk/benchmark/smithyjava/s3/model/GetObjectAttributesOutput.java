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

@SmithyGenerated
public final class GetObjectAttributesOutput implements SerializableStruct {

    public static final Schema $SCHEMA = Schemas3.GET_OBJECT_ATTRIBUTES_OUTPUT;
    private static final Schema $SCHEMA_DELETE_MARKER = $SCHEMA.member("DeleteMarker");
    private static final Schema $SCHEMA_LAST_MODIFIED = $SCHEMA.member("LastModified");
    private static final Schema $SCHEMA_VERSION_ID = $SCHEMA.member("VersionId");
    private static final Schema $SCHEMA_REQUEST_CHARGED = $SCHEMA.member("RequestCharged");
    private static final Schema $SCHEMA_E_TAG = $SCHEMA.member("ETag");
    private static final Schema $SCHEMA_CHECKSUM = $SCHEMA.member("Checksum");
    private static final Schema $SCHEMA_OBJECT_PARTS = $SCHEMA.member("ObjectParts");
    private static final Schema $SCHEMA_STORAGE_CLASS = $SCHEMA.member("StorageClass");
    private static final Schema $SCHEMA_OBJECT_SIZE = $SCHEMA.member("ObjectSize");

    public static final ShapeId $ID = $SCHEMA.id();

    private final transient Boolean deleteMarker;
    private final transient Instant lastModified;
    private final transient String versionId;
    private final transient RequestCharged requestCharged;
    private final transient String eTag;
    private final transient Checksum checksum;
    private final transient GetObjectAttributesParts objectParts;
    private final transient StorageClass storageClass;
    private final transient Long objectSize;

    private GetObjectAttributesOutput(Builder builder) {
        this.deleteMarker = builder.deleteMarker;
        this.lastModified = builder.lastModified;
        this.versionId = builder.versionId;
        this.requestCharged = builder.requestCharged;
        this.eTag = builder.eTag;
        this.checksum = builder.checksum;
        this.objectParts = builder.objectParts;
        this.storageClass = builder.storageClass;
        this.objectSize = builder.objectSize;
    }

    /**
     * Specifies whether the object retrieved was (<code>true</code>) or was not (<code>false</code>) a delete marker.
     * If <code>false</code>, this response header does not appear in the response. To learn more about delete markers,
     * see <a href="https://docs.aws.amazon.com/AmazonS3/latest/userguide/DeleteMarker.html">Working with delete markers</a>.
     *
     * <p>This functionality is not supported for directory buckets.
     */
    public Boolean isDeleteMarker() {
        return deleteMarker;
    }

    /**
     * Date and time when the object was last modified.
     */
    public Instant getLastModified() {
        return lastModified;
    }

    /**
     * The version ID of the object.
     *
     * <p>This functionality is not supported for directory buckets.
     */
    public String getVersionId() {
        return versionId;
    }

    public RequestCharged getRequestCharged() {
        return requestCharged;
    }

    /**
     * An ETag is an opaque identifier assigned by a web server to a specific version of a resource found at a URL.
     */
    public String getETag() {
        return eTag;
    }

    /**
     * The checksum or digest of the object.
     */
    public Checksum getChecksum() {
        return checksum;
    }

    /**
     * A collection of parts associated with a multipart upload.
     */
    public GetObjectAttributesParts getObjectParts() {
        return objectParts;
    }

    /**
     * Provides the storage class information of the object. Amazon S3 returns this header for all objects except for S3
     * Standard storage class objects.
     *
     * <p>For more information, see <a href="https://docs.aws.amazon.com/AmazonS3/latest/dev/storage-class-intro.html">Storage Classes</a>.
     *
     * <p><b>Directory buckets</b> - Directory buckets only support <code>EXPRESS_ONEZONE</code> (the S3 Express One
     * Zone storage class) in Availability Zones and <code>ONEZONE_IA</code> (the S3 One Zone-Infrequent Access storage
     * class) in Dedicated Local Zones.
     */
    public StorageClass getStorageClass() {
        return storageClass;
    }

    /**
     * The size of the object in bytes.
     */
    public Long getObjectSize() {
        return objectSize;
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
        GetObjectAttributesOutput that = (GetObjectAttributesOutput) other;
        return Objects.equals(this.deleteMarker, that.deleteMarker)
               && Objects.equals(this.objectSize, that.objectSize)
               && Objects.equals(this.versionId, that.versionId)
               && Objects.equals(this.eTag, that.eTag)
               && Objects.equals(this.requestCharged, that.requestCharged)
               && Objects.equals(this.storageClass, that.storageClass)
               && Objects.equals(this.lastModified, that.lastModified)
               && Objects.equals(this.checksum, that.checksum)
               && Objects.equals(this.objectParts, that.objectParts);
    }

    @Override
    public int hashCode() {
        int $hc = Objects.hashCode(deleteMarker);
        $hc = 31 * $hc + Objects.hashCode(lastModified);
        $hc = 31 * $hc + Objects.hashCode(versionId);
        $hc = 31 * $hc + Objects.hashCode(requestCharged);
        $hc = 31 * $hc + Objects.hashCode(eTag);
        $hc = 31 * $hc + Objects.hashCode(checksum);
        $hc = 31 * $hc + Objects.hashCode(objectParts);
        $hc = 31 * $hc + Objects.hashCode(storageClass);
        $hc = 31 * $hc + Objects.hashCode(objectSize);
        return $hc;
    }

    @Override
    public Schema schema() {
        return $SCHEMA;
    }

    @Override
    public void serializeMembers(ShapeSerializer serializer) {
        if (deleteMarker != null) {
            serializer.writeBoolean($SCHEMA_DELETE_MARKER, deleteMarker);
        }
        if (lastModified != null) {
            serializer.writeTimestamp($SCHEMA_LAST_MODIFIED, lastModified);
        }
        if (versionId != null) {
            serializer.writeString($SCHEMA_VERSION_ID, versionId);
        }
        if (requestCharged != null) {
            serializer.writeString($SCHEMA_REQUEST_CHARGED, requestCharged.getValue());
        }
        if (eTag != null) {
            serializer.writeString($SCHEMA_E_TAG, eTag);
        }
        if (checksum != null) {
            serializer.writeStruct($SCHEMA_CHECKSUM, checksum);
        }
        if (objectParts != null) {
            serializer.writeStruct($SCHEMA_OBJECT_PARTS, objectParts);
        }
        if (storageClass != null) {
            serializer.writeString($SCHEMA_STORAGE_CLASS, storageClass.getValue());
        }
        if (objectSize != null) {
            serializer.writeLong($SCHEMA_OBJECT_SIZE, objectSize);
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T getMemberValue(Schema member) {
        return switch (member.memberIndex()) {
            case 0 -> (T) SchemaUtils.validateSameMember($SCHEMA_DELETE_MARKER, member, deleteMarker);
            case 1 -> (T) SchemaUtils.validateSameMember($SCHEMA_LAST_MODIFIED, member, lastModified);
            case 2 -> (T) SchemaUtils.validateSameMember($SCHEMA_VERSION_ID, member, versionId);
            case 3 -> (T) SchemaUtils.validateSameMember($SCHEMA_REQUEST_CHARGED, member, requestCharged);
            case 4 -> (T) SchemaUtils.validateSameMember($SCHEMA_E_TAG, member, eTag);
            case 5 -> (T) SchemaUtils.validateSameMember($SCHEMA_CHECKSUM, member, checksum);
            case 6 -> (T) SchemaUtils.validateSameMember($SCHEMA_OBJECT_PARTS, member, objectParts);
            case 7 -> (T) SchemaUtils.validateSameMember($SCHEMA_STORAGE_CLASS, member, storageClass);
            case 8 -> (T) SchemaUtils.validateSameMember($SCHEMA_OBJECT_SIZE, member, objectSize);
            default -> throw new IllegalArgumentException("Attempted to get non-existent member: " + member.id());
        };
    }

    /**
     * Create a new builder containing all the current property values of this object.
     *
     * <p><strong>Note:</strong> This method performs only a shallow copy of the original properties.
     *
     * @return a builder for {@link GetObjectAttributesOutput}.
     */
    public Builder toBuilder() {
        var builder = new Builder();
        builder.deleteMarker(this.deleteMarker);
        builder.lastModified(this.lastModified);
        builder.versionId(this.versionId);
        builder.requestCharged(this.requestCharged);
        builder.eTag(this.eTag);
        builder.checksum(this.checksum);
        builder.objectParts(this.objectParts);
        builder.storageClass(this.storageClass);
        builder.objectSize(this.objectSize);
        return builder;
    }

    /**
     * @return returns a new Builder.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for {@link GetObjectAttributesOutput}.
     */
    public static final class Builder implements ShapeBuilder<GetObjectAttributesOutput> {
        private Boolean deleteMarker;
        private Instant lastModified;
        private String versionId;
        private RequestCharged requestCharged;
        private String eTag;
        private Checksum checksum;
        private GetObjectAttributesParts objectParts;
        private StorageClass storageClass;
        private Long objectSize;

        private Builder() {}

        @Override
        public Schema schema() {
            return $SCHEMA;
        }

        /**
         * Specifies whether the object retrieved was (<code>true</code>) or was not (<code>false</code>) a delete marker.
         * If <code>false</code>, this response header does not appear in the response. To learn more about delete markers,
         * see <a href="https://docs.aws.amazon.com/AmazonS3/latest/userguide/DeleteMarker.html">Working with delete markers</a>.
         *
         * <p>This functionality is not supported for directory buckets.
         *
         * @return this builder.
         */
        public Builder deleteMarker(Boolean deleteMarker) {
            this.deleteMarker = deleteMarker;
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
         * The version ID of the object.
         *
         * <p>This functionality is not supported for directory buckets.
         *
         * @return this builder.
         */
        public Builder versionId(String versionId) {
            this.versionId = versionId;
            return this;
        }

        /**
         * @return this builder.
         */
        public Builder requestCharged(RequestCharged requestCharged) {
            this.requestCharged = requestCharged;
            return this;
        }

        /**
         * An ETag is an opaque identifier assigned by a web server to a specific version of a resource found at a URL.
         *
         * @return this builder.
         */
        public Builder eTag(String eTag) {
            this.eTag = eTag;
            return this;
        }

        /**
         * The checksum or digest of the object.
         *
         * @return this builder.
         */
        public Builder checksum(Checksum checksum) {
            this.checksum = checksum;
            return this;
        }

        /**
         * A collection of parts associated with a multipart upload.
         *
         * @return this builder.
         */
        public Builder objectParts(GetObjectAttributesParts objectParts) {
            this.objectParts = objectParts;
            return this;
        }

        /**
         * Provides the storage class information of the object. Amazon S3 returns this header for all objects except for S3
         * Standard storage class objects.
         *
         * <p>For more information, see <a href="https://docs.aws.amazon.com/AmazonS3/latest/dev/storage-class-intro.html">Storage Classes</a>.
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
         * The size of the object in bytes.
         *
         * @return this builder.
         */
        public Builder objectSize(Long objectSize) {
            this.objectSize = objectSize;
            return this;
        }

        @Override
        public GetObjectAttributesOutput build() {
            return new GetObjectAttributesOutput(this);
        }

        @Override
        @SuppressWarnings("unchecked")
        public void setMemberValue(Schema member, Object value) {
            switch (member.memberIndex()) {
                case 0 -> deleteMarker((Boolean) SchemaUtils.validateSameMember($SCHEMA_DELETE_MARKER, member, value));
                case 1 -> lastModified((Instant) SchemaUtils.validateSameMember($SCHEMA_LAST_MODIFIED, member, value));
                case 2 -> versionId((String) SchemaUtils.validateSameMember($SCHEMA_VERSION_ID, member, value));
                case 3 -> requestCharged((RequestCharged) SchemaUtils.validateSameMember($SCHEMA_REQUEST_CHARGED, member, value));
                case 4 -> eTag((String) SchemaUtils.validateSameMember($SCHEMA_E_TAG, member, value));
                case 5 -> checksum((Checksum) SchemaUtils.validateSameMember($SCHEMA_CHECKSUM, member, value));
                case 6 -> objectParts((GetObjectAttributesParts) SchemaUtils.validateSameMember($SCHEMA_OBJECT_PARTS, member, value));
                case 7 -> storageClass((StorageClass) SchemaUtils.validateSameMember($SCHEMA_STORAGE_CLASS, member, value));
                case 8 -> objectSize((Long) SchemaUtils.validateSameMember($SCHEMA_OBJECT_SIZE, member, value));
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
                    case 0 -> builder.deleteMarker(de.readBoolean(member));
                    case 1 -> builder.lastModified(de.readTimestamp(member));
                    case 2 -> builder.versionId(de.readString(member));
                    case 3 -> builder.requestCharged(RequestCharged.builder().deserializeMember(de, member).build());
                    case 4 -> builder.eTag(de.readString(member));
                    case 5 -> builder.checksum(Checksum.builder().deserializeMember(de, member).build());
                    case 6 -> builder.objectParts(GetObjectAttributesParts.builder().deserializeMember(de, member).build());
                    case 7 -> builder.storageClass(StorageClass.builder().deserializeMember(de, member).build());
                    case 8 -> builder.objectSize(de.readLong(member));
                    default -> throw new IllegalArgumentException("Unexpected member: " + member.memberName());
                }
            }
        }
    }
}
