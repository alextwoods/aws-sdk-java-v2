package software.amazon.awssdk.benchmark.smithyjava.s3.model;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import software.amazon.smithy.java.core.schema.PresenceTracker;
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
 * Describes an Amazon S3 location that will receive the results of the restore request.
 */
@SmithyGenerated
public final class S3Location implements SerializableStruct {

    public static final Schema $SCHEMA = Schemas6.S3_LOCATION;
    private static final Schema $SCHEMA_BUCKET_NAME = $SCHEMA.member("BucketName");
    private static final Schema $SCHEMA_PREFIX = $SCHEMA.member("Prefix");
    private static final Schema $SCHEMA_ENCRYPTION = $SCHEMA.member("Encryption");
    private static final Schema $SCHEMA_CANNEDAC_L = $SCHEMA.member("CannedACL");
    private static final Schema $SCHEMA_ACCESS_CONTROL_LIST = $SCHEMA.member("AccessControlList");
    private static final Schema $SCHEMA_TAGGING = $SCHEMA.member("Tagging");
    private static final Schema $SCHEMA_USER_METADATA = $SCHEMA.member("UserMetadata");
    private static final Schema $SCHEMA_STORAGE_CLASS = $SCHEMA.member("StorageClass");

    public static final ShapeId $ID = $SCHEMA.id();

    private final transient String bucketName;
    private final transient String prefix;
    private final transient Encryption encryption;
    private final transient ObjectCannedACL cannedacL;
    private final transient List<Grant> accessControlList;
    private final transient Tagging tagging;
    private final transient List<MetadataEntry> userMetadata;
    private final transient StorageClass storageClass;

    private S3Location(Builder builder) {
        this.bucketName = builder.bucketName;
        this.prefix = builder.prefix;
        this.encryption = builder.encryption;
        this.cannedacL = builder.cannedacL;
        this.accessControlList = builder.accessControlList == null ? null : Collections.unmodifiableList(builder.accessControlList);
        this.tagging = builder.tagging;
        this.userMetadata = builder.userMetadata == null ? null : Collections.unmodifiableList(builder.userMetadata);
        this.storageClass = builder.storageClass;
    }

    /**
     * The name of the bucket where the restore results will be placed.
     */
    public String getBucketName() {
        return bucketName;
    }

    /**
     * The prefix that is prepended to the restore results for this request.
     */
    public String getPrefix() {
        return prefix;
    }

    public Encryption getEncryption() {
        return encryption;
    }

    /**
     * The canned ACL to apply to the restore results.
     */
    public ObjectCannedACL getCannedacL() {
        return cannedacL;
    }

    /**
     * A list of grants that control access to the staged results.
     */
    public List<Grant> getAccessControlList() {
        if (accessControlList == null) {
            return Collections.emptyList();
        }
        return accessControlList;
    }

    public boolean hasAccessControlList() {
        return accessControlList != null;
    }

    /**
     * The tag-set that is applied to the restore results.
     */
    public Tagging getTagging() {
        return tagging;
    }

    /**
     * A list of metadata to store with the restore results in S3.
     */
    public List<MetadataEntry> getUserMetadata() {
        if (userMetadata == null) {
            return Collections.emptyList();
        }
        return userMetadata;
    }

    public boolean hasUserMetadata() {
        return userMetadata != null;
    }

    /**
     * The class of storage used to store the restore results.
     */
    public StorageClass getStorageClass() {
        return storageClass;
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
        S3Location that = (S3Location) other;
        return Objects.equals(this.bucketName, that.bucketName)
               && Objects.equals(this.prefix, that.prefix)
               && Objects.equals(this.cannedacL, that.cannedacL)
               && Objects.equals(this.storageClass, that.storageClass)
               && Objects.equals(this.encryption, that.encryption)
               && Objects.equals(this.tagging, that.tagging)
               && Objects.equals(this.accessControlList, that.accessControlList)
               && Objects.equals(this.userMetadata, that.userMetadata);
    }

    @Override
    public int hashCode() {
        int $hc = Objects.hashCode(bucketName);
        $hc = 31 * $hc + Objects.hashCode(prefix);
        $hc = 31 * $hc + Objects.hashCode(encryption);
        $hc = 31 * $hc + Objects.hashCode(cannedacL);
        $hc = 31 * $hc + Objects.hashCode(accessControlList);
        $hc = 31 * $hc + Objects.hashCode(tagging);
        $hc = 31 * $hc + Objects.hashCode(userMetadata);
        $hc = 31 * $hc + Objects.hashCode(storageClass);
        return $hc;
    }

    @Override
    public Schema schema() {
        return $SCHEMA;
    }

    @Override
    public void serializeMembers(ShapeSerializer serializer) {
        serializer.writeString($SCHEMA_BUCKET_NAME, bucketName);
        serializer.writeString($SCHEMA_PREFIX, prefix);
        if (encryption != null) {
            serializer.writeStruct($SCHEMA_ENCRYPTION, encryption);
        }
        if (cannedacL != null) {
            serializer.writeString($SCHEMA_CANNEDAC_L, cannedacL.getValue());
        }
        if (accessControlList != null) {
            serializer.writeList($SCHEMA_ACCESS_CONTROL_LIST, accessControlList, accessControlList.size(), SharedSerde.GrantsSerializer.INSTANCE);
        }
        if (tagging != null) {
            serializer.writeStruct($SCHEMA_TAGGING, tagging);
        }
        if (userMetadata != null) {
            serializer.writeList($SCHEMA_USER_METADATA, userMetadata, userMetadata.size(), SharedSerde.UserMetadataSerializer.INSTANCE);
        }
        if (storageClass != null) {
            serializer.writeString($SCHEMA_STORAGE_CLASS, storageClass.getValue());
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T getMemberValue(Schema member) {
        return switch (member.memberIndex()) {
            case 0 -> (T) SchemaUtils.validateSameMember($SCHEMA_BUCKET_NAME, member, bucketName);
            case 1 -> (T) SchemaUtils.validateSameMember($SCHEMA_PREFIX, member, prefix);
            case 2 -> (T) SchemaUtils.validateSameMember($SCHEMA_ENCRYPTION, member, encryption);
            case 3 -> (T) SchemaUtils.validateSameMember($SCHEMA_CANNEDAC_L, member, cannedacL);
            case 4 -> (T) SchemaUtils.validateSameMember($SCHEMA_ACCESS_CONTROL_LIST, member, accessControlList);
            case 5 -> (T) SchemaUtils.validateSameMember($SCHEMA_TAGGING, member, tagging);
            case 6 -> (T) SchemaUtils.validateSameMember($SCHEMA_USER_METADATA, member, userMetadata);
            case 7 -> (T) SchemaUtils.validateSameMember($SCHEMA_STORAGE_CLASS, member, storageClass);
            default -> throw new IllegalArgumentException("Attempted to get non-existent member: " + member.id());
        };
    }

    /**
     * Create a new builder containing all the current property values of this object.
     *
     * <p><strong>Note:</strong> This method performs only a shallow copy of the original properties.
     *
     * @return a builder for {@link S3Location}.
     */
    public Builder toBuilder() {
        var builder = new Builder();
        builder.bucketName(this.bucketName);
        builder.prefix(this.prefix);
        builder.encryption(this.encryption);
        builder.cannedacL(this.cannedacL);
        builder.accessControlList(this.accessControlList);
        builder.tagging(this.tagging);
        builder.userMetadata(this.userMetadata);
        builder.storageClass(this.storageClass);
        return builder;
    }

    /**
     * @return returns a new Builder.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for {@link S3Location}.
     */
    public static final class Builder implements ShapeBuilder<S3Location> {
        private final PresenceTracker tracker = PresenceTracker.of($SCHEMA);
        private String bucketName;
        private String prefix;
        private Encryption encryption;
        private ObjectCannedACL cannedacL;
        private List<Grant> accessControlList;
        private Tagging tagging;
        private List<MetadataEntry> userMetadata;
        private StorageClass storageClass;

        private Builder() {}

        @Override
        public Schema schema() {
            return $SCHEMA;
        }

        /**
         * The name of the bucket where the restore results will be placed.
         *
         * <p><strong>Required</strong>
         * @return this builder.
         */
        public Builder bucketName(String bucketName) {
            this.bucketName = Objects.requireNonNull(bucketName, "bucketName cannot be null");
            tracker.setMember($SCHEMA_BUCKET_NAME);
            return this;
        }

        /**
         * The prefix that is prepended to the restore results for this request.
         *
         * <p><strong>Required</strong>
         * @return this builder.
         */
        public Builder prefix(String prefix) {
            this.prefix = Objects.requireNonNull(prefix, "prefix cannot be null");
            tracker.setMember($SCHEMA_PREFIX);
            return this;
        }

        /**
         * @return this builder.
         */
        public Builder encryption(Encryption encryption) {
            this.encryption = encryption;
            return this;
        }

        /**
         * The canned ACL to apply to the restore results.
         *
         * @return this builder.
         */
        public Builder cannedacL(ObjectCannedACL cannedacL) {
            this.cannedacL = cannedacL;
            return this;
        }

        /**
         * A list of grants that control access to the staged results.
         *
         * @return this builder.
         */
        public Builder accessControlList(List<Grant> accessControlList) {
            this.accessControlList = accessControlList;
            return this;
        }

        /**
         * The tag-set that is applied to the restore results.
         *
         * @return this builder.
         */
        public Builder tagging(Tagging tagging) {
            this.tagging = tagging;
            return this;
        }

        /**
         * A list of metadata to store with the restore results in S3.
         *
         * @return this builder.
         */
        public Builder userMetadata(List<MetadataEntry> userMetadata) {
            this.userMetadata = userMetadata;
            return this;
        }

        /**
         * The class of storage used to store the restore results.
         *
         * @return this builder.
         */
        public Builder storageClass(StorageClass storageClass) {
            this.storageClass = storageClass;
            return this;
        }

        @Override
        public S3Location build() {
            tracker.validate();
            return new S3Location(this);
        }

        @Override
        @SuppressWarnings("unchecked")
        public void setMemberValue(Schema member, Object value) {
            switch (member.memberIndex()) {
                case 0 -> bucketName((String) SchemaUtils.validateSameMember($SCHEMA_BUCKET_NAME, member, value));
                case 1 -> prefix((String) SchemaUtils.validateSameMember($SCHEMA_PREFIX, member, value));
                case 2 -> encryption((Encryption) SchemaUtils.validateSameMember($SCHEMA_ENCRYPTION, member, value));
                case 3 -> cannedacL((ObjectCannedACL) SchemaUtils.validateSameMember($SCHEMA_CANNEDAC_L, member, value));
                case 4 -> accessControlList((List<Grant>) SchemaUtils.validateSameMember($SCHEMA_ACCESS_CONTROL_LIST, member, value));
                case 5 -> tagging((Tagging) SchemaUtils.validateSameMember($SCHEMA_TAGGING, member, value));
                case 6 -> userMetadata((List<MetadataEntry>) SchemaUtils.validateSameMember($SCHEMA_USER_METADATA, member, value));
                case 7 -> storageClass((StorageClass) SchemaUtils.validateSameMember($SCHEMA_STORAGE_CLASS, member, value));
                default -> ShapeBuilder.super.setMemberValue(member, value);
            }
        }

        @Override
        public ShapeBuilder<S3Location> errorCorrection() {
            if (tracker.allSet()) {
                return this;
            }
            if (!tracker.checkMember($SCHEMA_BUCKET_NAME)) {
                bucketName("");
            }
            if (!tracker.checkMember($SCHEMA_PREFIX)) {
                prefix("");
            }
            return this;
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
                    case 0 -> builder.bucketName(de.readString(member));
                    case 1 -> builder.prefix(de.readString(member));
                    case 2 -> builder.encryption(Encryption.builder().deserializeMember(de, member).build());
                    case 3 -> builder.cannedacL(ObjectCannedACL.builder().deserializeMember(de, member).build());
                    case 4 -> builder.accessControlList(SharedSerde.deserializeGrants(member, de));
                    case 5 -> builder.tagging(Tagging.builder().deserializeMember(de, member).build());
                    case 6 -> builder.userMetadata(SharedSerde.deserializeUserMetadata(member, de));
                    case 7 -> builder.storageClass(StorageClass.builder().deserializeMember(de, member).build());
                    default -> throw new IllegalArgumentException("Unexpected member: " + member.memberName());
                }
            }
        }
    }
}
