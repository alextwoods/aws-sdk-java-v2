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
 * Container for <code>TagSet</code> elements.
 */
@SmithyGenerated
public final class Tagging implements SerializableStruct {

    public static final Schema $SCHEMA = Schemas5.TAGGING;
    private static final Schema $SCHEMA_TAG_SET = $SCHEMA.member("TagSet");

    public static final ShapeId $ID = $SCHEMA.id();

    private final transient List<Tag> tagSet;

    private Tagging(Builder builder) {
        this.tagSet = Collections.unmodifiableList(builder.tagSet);
    }

    /**
     * A collection for a set of tags
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
        Tagging that = (Tagging) other;
        return Objects.equals(this.tagSet, that.tagSet);
    }

    @Override
    public int hashCode() {
        int $hc = Objects.hashCode(tagSet);
        return $hc;
    }

    @Override
    public Schema schema() {
        return $SCHEMA;
    }

    @Override
    public void serializeMembers(ShapeSerializer serializer) {
        serializer.writeList($SCHEMA_TAG_SET, tagSet, tagSet.size(), SharedSerde.TagSetSerializer.INSTANCE);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T getMemberValue(Schema member) {
        return switch (member.memberIndex()) {
            case 0 -> (T) SchemaUtils.validateSameMember($SCHEMA_TAG_SET, member, tagSet);
            default -> throw new IllegalArgumentException("Attempted to get non-existent member: " + member.id());
        };
    }

    /**
     * Create a new builder containing all the current property values of this object.
     *
     * <p><strong>Note:</strong> This method performs only a shallow copy of the original properties.
     *
     * @return a builder for {@link Tagging}.
     */
    public Builder toBuilder() {
        var builder = new Builder();
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
     * Builder for {@link Tagging}.
     */
    public static final class Builder implements ShapeBuilder<Tagging> {
        private final PresenceTracker tracker = PresenceTracker.of($SCHEMA);
        private List<Tag> tagSet;

        private Builder() {}

        @Override
        public Schema schema() {
            return $SCHEMA;
        }

        /**
         * A collection for a set of tags
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
        public Tagging build() {
            tracker.validate();
            return new Tagging(this);
        }

        @Override
        @SuppressWarnings("unchecked")
        public void setMemberValue(Schema member, Object value) {
            switch (member.memberIndex()) {
                case 0 -> tagSet((List<Tag>) SchemaUtils.validateSameMember($SCHEMA_TAG_SET, member, value));
                default -> ShapeBuilder.super.setMemberValue(member, value);
            }
        }

        @Override
        public ShapeBuilder<Tagging> errorCorrection() {
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
                    default -> throw new IllegalArgumentException("Unexpected member: " + member.memberName());
                }
            }
        }
    }
}
