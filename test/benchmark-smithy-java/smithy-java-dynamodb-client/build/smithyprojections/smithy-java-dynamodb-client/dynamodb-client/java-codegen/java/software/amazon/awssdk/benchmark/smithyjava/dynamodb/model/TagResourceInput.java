package software.amazon.awssdk.benchmark.smithyjava.dynamodb.model;

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
public final class TagResourceInput implements SerializableStruct {

    public static final Schema $SCHEMA = Schemas.TAG_RESOURCE_INPUT;
    private static final Schema $SCHEMA_RESOURCE_ARN = $SCHEMA.member("ResourceArn");
    private static final Schema $SCHEMA_TAGS = $SCHEMA.member("Tags");

    public static final ShapeId $ID = $SCHEMA.id();

    private final transient String resourceArn;
    private final transient List<Tag> tags;

    private TagResourceInput(Builder builder) {
        this.resourceArn = builder.resourceArn;
        this.tags = Collections.unmodifiableList(builder.tags);
    }

    /**
     * Identifies the Amazon DynamoDB resource to which tags should be added. This value is an Amazon Resource Name
     * (ARN).
     */
    public String getResourceArn() {
        return resourceArn;
    }

    /**
     * The tags to be assigned to the Amazon DynamoDB resource.
     */
    public List<Tag> getTags() {
        return tags;
    }

    public boolean hasTags() {
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
        TagResourceInput that = (TagResourceInput) other;
        return Objects.equals(this.resourceArn, that.resourceArn)
               && Objects.equals(this.tags, that.tags);
    }

    @Override
    public int hashCode() {
        int $hc = Objects.hashCode(resourceArn);
        $hc = 31 * $hc + Objects.hashCode(tags);
        return $hc;
    }

    @Override
    public Schema schema() {
        return $SCHEMA;
    }

    @Override
    public void serializeMembers(ShapeSerializer serializer) {
        serializer.writeString($SCHEMA_RESOURCE_ARN, resourceArn);
        serializer.writeList($SCHEMA_TAGS, tags, tags.size(), SharedSerde.TagListSerializer.INSTANCE);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T getMemberValue(Schema member) {
        return switch (member.memberIndex()) {
            case 0 -> (T) SchemaUtils.validateSameMember($SCHEMA_RESOURCE_ARN, member, resourceArn);
            case 1 -> (T) SchemaUtils.validateSameMember($SCHEMA_TAGS, member, tags);
            default -> throw new IllegalArgumentException("Attempted to get non-existent member: " + member.id());
        };
    }

    /**
     * Create a new builder containing all the current property values of this object.
     *
     * <p><strong>Note:</strong> This method performs only a shallow copy of the original properties.
     *
     * @return a builder for {@link TagResourceInput}.
     */
    public Builder toBuilder() {
        var builder = new Builder();
        builder.resourceArn(this.resourceArn);
        builder.tags(this.tags);
        return builder;
    }

    /**
     * @return returns a new Builder.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for {@link TagResourceInput}.
     */
    public static final class Builder implements ShapeBuilder<TagResourceInput> {
        private final PresenceTracker tracker = PresenceTracker.of($SCHEMA);
        private String resourceArn;
        private List<Tag> tags;

        private Builder() {}

        @Override
        public Schema schema() {
            return $SCHEMA;
        }

        /**
         * Identifies the Amazon DynamoDB resource to which tags should be added. This value is an Amazon Resource Name
         * (ARN).
         *
         * <p><strong>Required</strong>
         * @return this builder.
         */
        public Builder resourceArn(String resourceArn) {
            this.resourceArn = Objects.requireNonNull(resourceArn, "resourceArn cannot be null");
            tracker.setMember($SCHEMA_RESOURCE_ARN);
            return this;
        }

        /**
         * The tags to be assigned to the Amazon DynamoDB resource.
         *
         * <p><strong>Required</strong>
         * @return this builder.
         */
        public Builder tags(List<Tag> tags) {
            this.tags = Objects.requireNonNull(tags, "tags cannot be null");
            tracker.setMember($SCHEMA_TAGS);
            return this;
        }

        @Override
        public TagResourceInput build() {
            tracker.validate();
            return new TagResourceInput(this);
        }

        @Override
        @SuppressWarnings("unchecked")
        public void setMemberValue(Schema member, Object value) {
            switch (member.memberIndex()) {
                case 0 -> resourceArn((String) SchemaUtils.validateSameMember($SCHEMA_RESOURCE_ARN, member, value));
                case 1 -> tags((List<Tag>) SchemaUtils.validateSameMember($SCHEMA_TAGS, member, value));
                default -> ShapeBuilder.super.setMemberValue(member, value);
            }
        }

        @Override
        public ShapeBuilder<TagResourceInput> errorCorrection() {
            if (tracker.allSet()) {
                return this;
            }
            if (!tracker.checkMember($SCHEMA_RESOURCE_ARN)) {
                resourceArn("");
            }
            if (!tracker.checkMember($SCHEMA_TAGS)) {
                tags(Collections.emptyList());
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
                    case 0 -> builder.resourceArn(de.readString(member));
                    case 1 -> builder.tags(SharedSerde.deserializeTagList(member, de));
                    default -> throw new IllegalArgumentException("Unexpected member: " + member.memberName());
                }
            }
        }
    }
}
