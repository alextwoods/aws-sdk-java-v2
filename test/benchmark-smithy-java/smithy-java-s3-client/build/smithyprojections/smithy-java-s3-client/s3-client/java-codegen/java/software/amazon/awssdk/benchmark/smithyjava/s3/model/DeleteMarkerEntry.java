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
 * Information about the delete marker.
 */
@SmithyGenerated
public final class DeleteMarkerEntry implements SerializableStruct {

    public static final Schema $SCHEMA = Schemas4.DELETE_MARKER_ENTRY;
    private static final Schema $SCHEMA_OWNER = $SCHEMA.member("Owner");
    private static final Schema $SCHEMA_KEY = $SCHEMA.member("Key");
    private static final Schema $SCHEMA_VERSION_ID = $SCHEMA.member("VersionId");
    private static final Schema $SCHEMA_IS_LATEST = $SCHEMA.member("IsLatest");
    private static final Schema $SCHEMA_LAST_MODIFIED = $SCHEMA.member("LastModified");

    public static final ShapeId $ID = $SCHEMA.id();

    private final transient Owner owner;
    private final transient String key;
    private final transient String versionId;
    private final transient Boolean isLatest;
    private final transient Instant lastModified;

    private DeleteMarkerEntry(Builder builder) {
        this.owner = builder.owner;
        this.key = builder.key;
        this.versionId = builder.versionId;
        this.isLatest = builder.isLatest;
        this.lastModified = builder.lastModified;
    }

    /**
     * The account that created the delete marker.
     */
    public Owner getOwner() {
        return owner;
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
        DeleteMarkerEntry that = (DeleteMarkerEntry) other;
        return Objects.equals(this.isLatest, that.isLatest)
               && Objects.equals(this.key, that.key)
               && Objects.equals(this.versionId, that.versionId)
               && Objects.equals(this.lastModified, that.lastModified)
               && Objects.equals(this.owner, that.owner);
    }

    @Override
    public int hashCode() {
        int $hc = Objects.hashCode(owner);
        $hc = 31 * $hc + Objects.hashCode(key);
        $hc = 31 * $hc + Objects.hashCode(versionId);
        $hc = 31 * $hc + Objects.hashCode(isLatest);
        $hc = 31 * $hc + Objects.hashCode(lastModified);
        return $hc;
    }

    @Override
    public Schema schema() {
        return $SCHEMA;
    }

    @Override
    public void serializeMembers(ShapeSerializer serializer) {
        if (owner != null) {
            serializer.writeStruct($SCHEMA_OWNER, owner);
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
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T getMemberValue(Schema member) {
        return switch (member.memberIndex()) {
            case 0 -> (T) SchemaUtils.validateSameMember($SCHEMA_OWNER, member, owner);
            case 1 -> (T) SchemaUtils.validateSameMember($SCHEMA_KEY, member, key);
            case 2 -> (T) SchemaUtils.validateSameMember($SCHEMA_VERSION_ID, member, versionId);
            case 3 -> (T) SchemaUtils.validateSameMember($SCHEMA_IS_LATEST, member, isLatest);
            case 4 -> (T) SchemaUtils.validateSameMember($SCHEMA_LAST_MODIFIED, member, lastModified);
            default -> throw new IllegalArgumentException("Attempted to get non-existent member: " + member.id());
        };
    }

    /**
     * Create a new builder containing all the current property values of this object.
     *
     * <p><strong>Note:</strong> This method performs only a shallow copy of the original properties.
     *
     * @return a builder for {@link DeleteMarkerEntry}.
     */
    public Builder toBuilder() {
        var builder = new Builder();
        builder.owner(this.owner);
        builder.key(this.key);
        builder.versionId(this.versionId);
        builder.isLatest(this.isLatest);
        builder.lastModified(this.lastModified);
        return builder;
    }

    /**
     * @return returns a new Builder.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for {@link DeleteMarkerEntry}.
     */
    public static final class Builder implements ShapeBuilder<DeleteMarkerEntry> {
        private Owner owner;
        private String key;
        private String versionId;
        private Boolean isLatest;
        private Instant lastModified;

        private Builder() {}

        @Override
        public Schema schema() {
            return $SCHEMA;
        }

        /**
         * The account that created the delete marker.
         *
         * @return this builder.
         */
        public Builder owner(Owner owner) {
            this.owner = owner;
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

        @Override
        public DeleteMarkerEntry build() {
            return new DeleteMarkerEntry(this);
        }

        @Override
        @SuppressWarnings("unchecked")
        public void setMemberValue(Schema member, Object value) {
            switch (member.memberIndex()) {
                case 0 -> owner((Owner) SchemaUtils.validateSameMember($SCHEMA_OWNER, member, value));
                case 1 -> key((String) SchemaUtils.validateSameMember($SCHEMA_KEY, member, value));
                case 2 -> versionId((String) SchemaUtils.validateSameMember($SCHEMA_VERSION_ID, member, value));
                case 3 -> isLatest((Boolean) SchemaUtils.validateSameMember($SCHEMA_IS_LATEST, member, value));
                case 4 -> lastModified((Instant) SchemaUtils.validateSameMember($SCHEMA_LAST_MODIFIED, member, value));
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
                    case 0 -> builder.owner(Owner.builder().deserializeMember(de, member).build());
                    case 1 -> builder.key(de.readString(member));
                    case 2 -> builder.versionId(de.readString(member));
                    case 3 -> builder.isLatest(de.readBoolean(member));
                    case 4 -> builder.lastModified(de.readTimestamp(member));
                    default -> throw new IllegalArgumentException("Unexpected member: " + member.memberName());
                }
            }
        }
    }
}
