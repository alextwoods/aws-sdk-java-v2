package software.amazon.awssdk.benchmark.smithyjava.s3.model;

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
 * Information about the deleted object.
 */
@SmithyGenerated
public final class DeletedObject implements SerializableStruct {

    public static final Schema $SCHEMA = Schemas1.DELETED_OBJECT;
    private static final Schema $SCHEMA_KEY = $SCHEMA.member("Key");
    private static final Schema $SCHEMA_VERSION_ID = $SCHEMA.member("VersionId");
    private static final Schema $SCHEMA_DELETE_MARKER = $SCHEMA.member("DeleteMarker");
    private static final Schema $SCHEMA_DELETE_MARKER_VERSION_ID = $SCHEMA.member("DeleteMarkerVersionId");

    public static final ShapeId $ID = $SCHEMA.id();

    private final transient String key;
    private final transient String versionId;
    private final transient Boolean deleteMarker;
    private final transient String deleteMarkerVersionId;

    private DeletedObject(Builder builder) {
        this.key = builder.key;
        this.versionId = builder.versionId;
        this.deleteMarker = builder.deleteMarker;
        this.deleteMarkerVersionId = builder.deleteMarkerVersionId;
    }

    /**
     * The name of the deleted object.
     */
    public String getKey() {
        return key;
    }

    /**
     * The version ID of the deleted object.
     *
     * <p>This functionality is not supported for directory buckets.
     */
    public String getVersionId() {
        return versionId;
    }

    /**
     * Indicates whether the specified object version that was permanently deleted was (true) or was not (false) a
     * delete marker before deletion. In a simple DELETE, this header indicates whether (true) or not (false) the
     * current version of the object is a delete marker. To learn more about delete markers, see <a href="https://docs.aws.amazon.com/AmazonS3/latest/userguide/DeleteMarker.html">Working with delete
     * markers</a>.
     *
     * <p>This functionality is not supported for directory buckets.
     */
    public Boolean isDeleteMarker() {
        return deleteMarker;
    }

    /**
     * The version ID of the delete marker created as a result of the DELETE operation. If you delete a specific object
     * version, the value returned by this header is the version ID of the object version deleted.
     *
     * <p>This functionality is not supported for directory buckets.
     */
    public String getDeleteMarkerVersionId() {
        return deleteMarkerVersionId;
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
        DeletedObject that = (DeletedObject) other;
        return Objects.equals(this.deleteMarker, that.deleteMarker)
               && Objects.equals(this.key, that.key)
               && Objects.equals(this.versionId, that.versionId)
               && Objects.equals(this.deleteMarkerVersionId, that.deleteMarkerVersionId);
    }

    @Override
    public int hashCode() {
        int $hc = Objects.hashCode(key);
        $hc = 31 * $hc + Objects.hashCode(versionId);
        $hc = 31 * $hc + Objects.hashCode(deleteMarker);
        $hc = 31 * $hc + Objects.hashCode(deleteMarkerVersionId);
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
        if (versionId != null) {
            serializer.writeString($SCHEMA_VERSION_ID, versionId);
        }
        if (deleteMarker != null) {
            serializer.writeBoolean($SCHEMA_DELETE_MARKER, deleteMarker);
        }
        if (deleteMarkerVersionId != null) {
            serializer.writeString($SCHEMA_DELETE_MARKER_VERSION_ID, deleteMarkerVersionId);
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T getMemberValue(Schema member) {
        return switch (member.memberIndex()) {
            case 0 -> (T) SchemaUtils.validateSameMember($SCHEMA_KEY, member, key);
            case 1 -> (T) SchemaUtils.validateSameMember($SCHEMA_VERSION_ID, member, versionId);
            case 2 -> (T) SchemaUtils.validateSameMember($SCHEMA_DELETE_MARKER, member, deleteMarker);
            case 3 -> (T) SchemaUtils.validateSameMember($SCHEMA_DELETE_MARKER_VERSION_ID, member, deleteMarkerVersionId);
            default -> throw new IllegalArgumentException("Attempted to get non-existent member: " + member.id());
        };
    }

    /**
     * Create a new builder containing all the current property values of this object.
     *
     * <p><strong>Note:</strong> This method performs only a shallow copy of the original properties.
     *
     * @return a builder for {@link DeletedObject}.
     */
    public Builder toBuilder() {
        var builder = new Builder();
        builder.key(this.key);
        builder.versionId(this.versionId);
        builder.deleteMarker(this.deleteMarker);
        builder.deleteMarkerVersionId(this.deleteMarkerVersionId);
        return builder;
    }

    /**
     * @return returns a new Builder.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for {@link DeletedObject}.
     */
    public static final class Builder implements ShapeBuilder<DeletedObject> {
        private String key;
        private String versionId;
        private Boolean deleteMarker;
        private String deleteMarkerVersionId;

        private Builder() {}

        @Override
        public Schema schema() {
            return $SCHEMA;
        }

        /**
         * The name of the deleted object.
         *
         * @return this builder.
         */
        public Builder key(String key) {
            this.key = key;
            return this;
        }

        /**
         * The version ID of the deleted object.
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
         * Indicates whether the specified object version that was permanently deleted was (true) or was not (false) a
         * delete marker before deletion. In a simple DELETE, this header indicates whether (true) or not (false) the
         * current version of the object is a delete marker. To learn more about delete markers, see <a href="https://docs.aws.amazon.com/AmazonS3/latest/userguide/DeleteMarker.html">Working with delete
         * markers</a>.
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
         * The version ID of the delete marker created as a result of the DELETE operation. If you delete a specific object
         * version, the value returned by this header is the version ID of the object version deleted.
         *
         * <p>This functionality is not supported for directory buckets.
         *
         * @return this builder.
         */
        public Builder deleteMarkerVersionId(String deleteMarkerVersionId) {
            this.deleteMarkerVersionId = deleteMarkerVersionId;
            return this;
        }

        @Override
        public DeletedObject build() {
            return new DeletedObject(this);
        }

        @Override
        @SuppressWarnings("unchecked")
        public void setMemberValue(Schema member, Object value) {
            switch (member.memberIndex()) {
                case 0 -> key((String) SchemaUtils.validateSameMember($SCHEMA_KEY, member, value));
                case 1 -> versionId((String) SchemaUtils.validateSameMember($SCHEMA_VERSION_ID, member, value));
                case 2 -> deleteMarker((Boolean) SchemaUtils.validateSameMember($SCHEMA_DELETE_MARKER, member, value));
                case 3 -> deleteMarkerVersionId((String) SchemaUtils.validateSameMember($SCHEMA_DELETE_MARKER_VERSION_ID, member, value));
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
                    case 1 -> builder.versionId(de.readString(member));
                    case 2 -> builder.deleteMarker(de.readBoolean(member));
                    case 3 -> builder.deleteMarkerVersionId(de.readString(member));
                    default -> throw new IllegalArgumentException("Unexpected member: " + member.memberName());
                }
            }
        }
    }
}
