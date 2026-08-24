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

@SmithyGenerated
public final class GetObjectTaggingOutput implements SerializableStruct {

    public static final Schema $SCHEMA = Schemas4.GET_OBJECT_TAGGING_OUTPUT;
    private static final Schema $SCHEMA_VERSION_ID = $SCHEMA.member("VersionId");
    private static final Schema $SCHEMA_TAG_SET = $SCHEMA.member("TagSet");

    public static final ShapeId $ID = $SCHEMA.id();

    private final transient String versionId;
    private final transient List<Tag> tagSet;

    private GetObjectTaggingOutput(Builder builder) {
        this.versionId = builder.versionId;
        this.tagSet = Collections.unmodifiableList(builder.tagSet);
    }

    /**
     * The versionId of the object for which you got the tagging information.
     */
    public String getVersionId() {
        return versionId;
    }

    /**
     * Contains the tag set.
     */
    public List<Tag> getTagSet() {
        return tagSet;
    }

    public boolean hasTagSet() {
        return true;
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
        GetObjectTaggingOutput that = (GetObjectTaggingOutput) other;
        return Objects.equals(this.versionId, that.versionId)
               && Objects.equals(this.tagSet, that.tagSet);
    }

    @Override
    public int hashCode() {
        int $hc = Objects.hashCode(versionId);
        $hc = 31 * $hc + Objects.hashCode(tagSet);
        return $hc;
    }

    @Override
    public Schema schema() {
        return $SCHEMA;
    }

    @Override
    public void serializeMembers(ShapeSerializer serializer) {
        if (versionId != null) {
            serializer.writeString($SCHEMA_VERSION_ID, versionId);
        }
        serializer.writeList($SCHEMA_TAG_SET, tagSet, tagSet.size(), SharedSerde.TagSetSerializer.INSTANCE);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T getMemberValue(Schema member) {
        return switch (member.memberIndex()) {
            case 0 -> (T) SchemaUtils.validateSameMember($SCHEMA_TAG_SET, member, tagSet);
            case 1 -> (T) SchemaUtils.validateSameMember($SCHEMA_VERSION_ID, member, versionId);
            default -> throw new IllegalArgumentException("Attempted to get non-existent member: " + member.id());
        };
    }

    /**
     * Create a new builder containing all the current property values of this object.
     *
     * <p><strong>Note:</strong> This method performs only a shallow copy of the original properties.
     *
     * @return a builder for {@link GetObjectTaggingOutput}.
     */
    public Builder toBuilder() {
        var builder = new Builder();
        builder.versionId(this.versionId);
        builder.tagSet(this.tagSet);
        return builder;
    }

    /**
     * @return returns a new Builder.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for {@link GetObjectTaggingOutput}.
     */
    public static final class Builder implements ShapeBuilder<GetObjectTaggingOutput> {
        private final PresenceTracker tracker = PresenceTracker.of($SCHEMA);
        private String versionId;
        private List<Tag> tagSet;

        private Builder() {}

        @Override
        public Schema schema() {
            return $SCHEMA;
        }

        /**
         * The versionId of the object for which you got the tagging information.
         *
         * @return this builder.
         */
        public Builder versionId(String versionId) {
            this.versionId = versionId;
            return this;
        }

        /**
         * Contains the tag set.
         *
         * <p><strong>Required</strong>
         * @return this builder.
         */
        public Builder tagSet(List<Tag> tagSet) {
            this.tagSet = Objects.requireNonNull(tagSet, "tagSet cannot be null");
            tracker.setMember($SCHEMA_TAG_SET);
            return this;
        }

        @Override
        public GetObjectTaggingOutput build() {
            tracker.validate();
            return new GetObjectTaggingOutput(this);
        }

        @Override
        @SuppressWarnings("unchecked")
        public void setMemberValue(Schema member, Object value) {
            switch (member.memberIndex()) {
                case 0 -> tagSet((List<Tag>) SchemaUtils.validateSameMember($SCHEMA_TAG_SET, member, value));
                case 1 -> versionId((String) SchemaUtils.validateSameMember($SCHEMA_VERSION_ID, member, value));
                default -> ShapeBuilder.super.setMemberValue(member, value);
            }
        }

        @Override
        public ShapeBuilder<GetObjectTaggingOutput> errorCorrection() {
            if (tracker.allSet()) {
                return this;
            }
            if (!tracker.checkMember($SCHEMA_TAG_SET)) {
                tagSet(Collections.emptyList());
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
                    case 0 -> builder.tagSet(SharedSerde.deserializeTagSet(member, de));
                    case 1 -> builder.versionId(de.readString(member));
                    default -> throw new IllegalArgumentException("Unexpected member: " + member.memberName());
                }
            }
        }
    }
}
