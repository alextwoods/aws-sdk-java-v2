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

@SmithyGenerated
public final class DeleteObjectOutput implements SerializableStruct {

    public static final Schema $SCHEMA = Schemas1.DELETE_OBJECT_OUTPUT;
    private static final Schema $SCHEMA_DELETE_MARKER = $SCHEMA.member("DeleteMarker");
    private static final Schema $SCHEMA_VERSION_ID = $SCHEMA.member("VersionId");
    private static final Schema $SCHEMA_REQUEST_CHARGED = $SCHEMA.member("RequestCharged");

    public static final ShapeId $ID = $SCHEMA.id();

    private final transient Boolean deleteMarker;
    private final transient String versionId;
    private final transient RequestCharged requestCharged;

    private DeleteObjectOutput(Builder builder) {
        this.deleteMarker = builder.deleteMarker;
        this.versionId = builder.versionId;
        this.requestCharged = builder.requestCharged;
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
     * Returns the version ID of the delete marker created as a result of the DELETE operation.
     *
     * <p>This functionality is not supported for directory buckets.
     */
    public String getVersionId() {
        return versionId;
    }

    public RequestCharged getRequestCharged() {
        return requestCharged;
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
        DeleteObjectOutput that = (DeleteObjectOutput) other;
        return Objects.equals(this.deleteMarker, that.deleteMarker)
               && Objects.equals(this.versionId, that.versionId)
               && Objects.equals(this.requestCharged, that.requestCharged);
    }

    @Override
    public int hashCode() {
        int $hc = Objects.hashCode(deleteMarker);
        $hc = 31 * $hc + Objects.hashCode(versionId);
        $hc = 31 * $hc + Objects.hashCode(requestCharged);
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
        if (versionId != null) {
            serializer.writeString($SCHEMA_VERSION_ID, versionId);
        }
        if (requestCharged != null) {
            serializer.writeString($SCHEMA_REQUEST_CHARGED, requestCharged.getValue());
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T getMemberValue(Schema member) {
        return switch (member.memberIndex()) {
            case 0 -> (T) SchemaUtils.validateSameMember($SCHEMA_DELETE_MARKER, member, deleteMarker);
            case 1 -> (T) SchemaUtils.validateSameMember($SCHEMA_VERSION_ID, member, versionId);
            case 2 -> (T) SchemaUtils.validateSameMember($SCHEMA_REQUEST_CHARGED, member, requestCharged);
            default -> throw new IllegalArgumentException("Attempted to get non-existent member: " + member.id());
        };
    }

    /**
     * Create a new builder containing all the current property values of this object.
     *
     * <p><strong>Note:</strong> This method performs only a shallow copy of the original properties.
     *
     * @return a builder for {@link DeleteObjectOutput}.
     */
    public Builder toBuilder() {
        var builder = new Builder();
        builder.deleteMarker(this.deleteMarker);
        builder.versionId(this.versionId);
        builder.requestCharged(this.requestCharged);
        return builder;
    }

    /**
     * @return returns a new Builder.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for {@link DeleteObjectOutput}.
     */
    public static final class Builder implements ShapeBuilder<DeleteObjectOutput> {
        private Boolean deleteMarker;
        private String versionId;
        private RequestCharged requestCharged;

        private Builder() {}

        @Override
        public Schema schema() {
            return $SCHEMA;
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
         * Returns the version ID of the delete marker created as a result of the DELETE operation.
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

        @Override
        public DeleteObjectOutput build() {
            return new DeleteObjectOutput(this);
        }

        @Override
        @SuppressWarnings("unchecked")
        public void setMemberValue(Schema member, Object value) {
            switch (member.memberIndex()) {
                case 0 -> deleteMarker((Boolean) SchemaUtils.validateSameMember($SCHEMA_DELETE_MARKER, member, value));
                case 1 -> versionId((String) SchemaUtils.validateSameMember($SCHEMA_VERSION_ID, member, value));
                case 2 -> requestCharged((RequestCharged) SchemaUtils.validateSameMember($SCHEMA_REQUEST_CHARGED, member, value));
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
                    case 1 -> builder.versionId(de.readString(member));
                    case 2 -> builder.requestCharged(RequestCharged.builder().deserializeMember(de, member).build());
                    default -> throw new IllegalArgumentException("Unexpected member: " + member.memberName());
                }
            }
        }
    }
}
