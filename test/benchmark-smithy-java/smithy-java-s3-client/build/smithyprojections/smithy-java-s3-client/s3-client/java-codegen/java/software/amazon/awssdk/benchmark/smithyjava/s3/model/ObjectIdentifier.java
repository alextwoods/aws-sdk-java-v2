package software.amazon.awssdk.benchmark.smithyjava.s3.model;

import java.time.Instant;
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
 * Object Identifier is unique value to identify objects.
 */
@SmithyGenerated
public final class ObjectIdentifier implements SerializableStruct {

    public static final Schema $SCHEMA = Schemas1.OBJECT_IDENTIFIER;
    private static final Schema $SCHEMA_KEY = $SCHEMA.member("Key");
    private static final Schema $SCHEMA_VERSION_ID = $SCHEMA.member("VersionId");
    private static final Schema $SCHEMA_E_TAG = $SCHEMA.member("ETag");
    private static final Schema $SCHEMA_LAST_MODIFIED_TIME = $SCHEMA.member("LastModifiedTime");
    private static final Schema $SCHEMA_SIZE = $SCHEMA.member("Size");

    public static final ShapeId $ID = $SCHEMA.id();

    private final transient String key;
    private final transient String versionId;
    private final transient String eTag;
    private final transient Instant lastModifiedTime;
    private final transient Long size;

    private ObjectIdentifier(Builder builder) {
        this.key = builder.key;
        this.versionId = builder.versionId;
        this.eTag = builder.eTag;
        this.lastModifiedTime = builder.lastModifiedTime;
        this.size = builder.size;
    }

    /**
     * Key name of the object.
     *
     * <p>Replacement must be made for object keys containing special characters (such as carriage returns) when using
     * XML requests. For more information, see <a href="https://docs.aws.amazon.com/AmazonS3/latest/userguide/object-keys.html#object-key-xml-related-constraints"> XML related object key constraints</a>.
     */
    public String getKey() {
        return key;
    }

    /**
     * Version ID for the specific version of the object to delete.
     *
     * <p>This functionality is not supported for directory buckets.
     */
    public String getVersionId() {
        return versionId;
    }

    /**
     * An entity tag (ETag) is an identifier assigned by a web server to a specific version of a resource found at a
     * URL. This header field makes the request method conditional on <code>ETags</code>.
     *
     * <p>Entity tags (ETags) for S3 Express One Zone are random alphanumeric strings unique to the object.
     */
    public String getETag() {
        return eTag;
    }

    /**
     * If present, the objects are deleted only if its modification times matches the provided <code>Timestamp</code>.
     *
     * <p>This functionality is only supported for directory buckets.
     */
    public Instant getLastModifiedTime() {
        return lastModifiedTime;
    }

    /**
     * If present, the objects are deleted only if its size matches the provided size in bytes.
     *
     * <p>This functionality is only supported for directory buckets.
     */
    public Long getSize() {
        return size;
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
        ObjectIdentifier that = (ObjectIdentifier) other;
        return Objects.equals(this.size, that.size)
               && Objects.equals(this.key, that.key)
               && Objects.equals(this.versionId, that.versionId)
               && Objects.equals(this.eTag, that.eTag)
               && Objects.equals(this.lastModifiedTime, that.lastModifiedTime);
    }

    @Override
    public int hashCode() {
        int $hc = Objects.hashCode(key);
        $hc = 31 * $hc + Objects.hashCode(versionId);
        $hc = 31 * $hc + Objects.hashCode(eTag);
        $hc = 31 * $hc + Objects.hashCode(lastModifiedTime);
        $hc = 31 * $hc + Objects.hashCode(size);
        return $hc;
    }

    @Override
    public Schema schema() {
        return $SCHEMA;
    }

    @Override
    public void serializeMembers(ShapeSerializer serializer) {
        serializer.writeString($SCHEMA_KEY, key);
        if (versionId != null) {
            serializer.writeString($SCHEMA_VERSION_ID, versionId);
        }
        if (eTag != null) {
            serializer.writeString($SCHEMA_E_TAG, eTag);
        }
        if (lastModifiedTime != null) {
            serializer.writeTimestamp($SCHEMA_LAST_MODIFIED_TIME, lastModifiedTime);
        }
        if (size != null) {
            serializer.writeLong($SCHEMA_SIZE, size);
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T getMemberValue(Schema member) {
        return switch (member.memberIndex()) {
            case 0 -> (T) SchemaUtils.validateSameMember($SCHEMA_KEY, member, key);
            case 1 -> (T) SchemaUtils.validateSameMember($SCHEMA_VERSION_ID, member, versionId);
            case 2 -> (T) SchemaUtils.validateSameMember($SCHEMA_E_TAG, member, eTag);
            case 3 -> (T) SchemaUtils.validateSameMember($SCHEMA_LAST_MODIFIED_TIME, member, lastModifiedTime);
            case 4 -> (T) SchemaUtils.validateSameMember($SCHEMA_SIZE, member, size);
            default -> throw new IllegalArgumentException("Attempted to get non-existent member: " + member.id());
        };
    }

    /**
     * Create a new builder containing all the current property values of this object.
     *
     * <p><strong>Note:</strong> This method performs only a shallow copy of the original properties.
     *
     * @return a builder for {@link ObjectIdentifier}.
     */
    public Builder toBuilder() {
        var builder = new Builder();
        builder.key(this.key);
        builder.versionId(this.versionId);
        builder.eTag(this.eTag);
        builder.lastModifiedTime(this.lastModifiedTime);
        builder.size(this.size);
        return builder;
    }

    /**
     * @return returns a new Builder.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for {@link ObjectIdentifier}.
     */
    public static final class Builder implements ShapeBuilder<ObjectIdentifier> {
        private final PresenceTracker tracker = PresenceTracker.of($SCHEMA);
        private String key;
        private String versionId;
        private String eTag;
        private Instant lastModifiedTime;
        private Long size;

        private Builder() {}

        @Override
        public Schema schema() {
            return $SCHEMA;
        }

        /**
         * Key name of the object.
         *
         * <p>Replacement must be made for object keys containing special characters (such as carriage returns) when using
         * XML requests. For more information, see <a href="https://docs.aws.amazon.com/AmazonS3/latest/userguide/object-keys.html#object-key-xml-related-constraints"> XML related object key constraints</a>.
         *
         * <p><strong>Required</strong>
         * @return this builder.
         */
        public Builder key(String key) {
            this.key = Objects.requireNonNull(key, "key cannot be null");
            tracker.setMember($SCHEMA_KEY);
            return this;
        }

        /**
         * Version ID for the specific version of the object to delete.
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
         * An entity tag (ETag) is an identifier assigned by a web server to a specific version of a resource found at a
         * URL. This header field makes the request method conditional on <code>ETags</code>.
         *
         * <p>Entity tags (ETags) for S3 Express One Zone are random alphanumeric strings unique to the object.
         *
         * @return this builder.
         */
        public Builder eTag(String eTag) {
            this.eTag = eTag;
            return this;
        }

        /**
         * If present, the objects are deleted only if its modification times matches the provided <code>Timestamp</code>.
         *
         * <p>This functionality is only supported for directory buckets.
         *
         * @return this builder.
         */
        public Builder lastModifiedTime(Instant lastModifiedTime) {
            this.lastModifiedTime = lastModifiedTime;
            return this;
        }

        /**
         * If present, the objects are deleted only if its size matches the provided size in bytes.
         *
         * <p>This functionality is only supported for directory buckets.
         *
         * @return this builder.
         */
        public Builder size(Long size) {
            this.size = size;
            return this;
        }

        @Override
        public ObjectIdentifier build() {
            tracker.validate();
            return new ObjectIdentifier(this);
        }

        @Override
        @SuppressWarnings("unchecked")
        public void setMemberValue(Schema member, Object value) {
            switch (member.memberIndex()) {
                case 0 -> key((String) SchemaUtils.validateSameMember($SCHEMA_KEY, member, value));
                case 1 -> versionId((String) SchemaUtils.validateSameMember($SCHEMA_VERSION_ID, member, value));
                case 2 -> eTag((String) SchemaUtils.validateSameMember($SCHEMA_E_TAG, member, value));
                case 3 -> lastModifiedTime((Instant) SchemaUtils.validateSameMember($SCHEMA_LAST_MODIFIED_TIME, member, value));
                case 4 -> size((Long) SchemaUtils.validateSameMember($SCHEMA_SIZE, member, value));
                default -> ShapeBuilder.super.setMemberValue(member, value);
            }
        }

        @Override
        public ShapeBuilder<ObjectIdentifier> errorCorrection() {
            if (tracker.allSet()) {
                return this;
            }
            if (!tracker.checkMember($SCHEMA_KEY)) {
                key("");
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
                    case 0 -> builder.key(de.readString(member));
                    case 1 -> builder.versionId(de.readString(member));
                    case 2 -> builder.eTag(de.readString(member));
                    case 3 -> builder.lastModifiedTime(de.readTimestamp(member));
                    case 4 -> builder.size(de.readLong(member));
                    default -> throw new IllegalArgumentException("Unexpected member: " + member.memberName());
                }
            }
        }
    }
}
