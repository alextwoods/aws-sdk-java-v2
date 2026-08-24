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
public final class UntagResourceInput implements SerializableStruct {

    public static final Schema $SCHEMA = Schemas.UNTAG_RESOURCE_INPUT;
    private static final Schema $SCHEMA_RESOURCE_ARN = $SCHEMA.member("ResourceArn");
    private static final Schema $SCHEMA_TAG_KEYS = $SCHEMA.member("TagKeys");

    public static final ShapeId $ID = $SCHEMA.id();

    private final transient String resourceArn;
    private final transient List<String> tagKeys;

    private UntagResourceInput(Builder builder) {
        this.resourceArn = builder.resourceArn;
        this.tagKeys = Collections.unmodifiableList(builder.tagKeys);
    }

    /**
     * The DynamoDB resource that the tags will be removed from. This value is an Amazon Resource Name (ARN).
     */
    public String getResourceArn() {
        return resourceArn;
    }

    /**
     * A list of tag keys. Existing tags of the resource whose keys are members of this list will be removed from the
     * DynamoDB resource.
     */
    public List<String> getTagKeys() {
        return tagKeys;
    }

    public boolean hasTagKeys() {
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
        UntagResourceInput that = (UntagResourceInput) other;
        return Objects.equals(this.resourceArn, that.resourceArn)
               && Objects.equals(this.tagKeys, that.tagKeys);
    }

    @Override
    public int hashCode() {
        int $hc = Objects.hashCode(resourceArn);
        $hc = 31 * $hc + Objects.hashCode(tagKeys);
        return $hc;
    }

    @Override
    public Schema schema() {
        return $SCHEMA;
    }

    @Override
    public void serializeMembers(ShapeSerializer serializer) {
        serializer.writeString($SCHEMA_RESOURCE_ARN, resourceArn);
        serializer.writeList($SCHEMA_TAG_KEYS, tagKeys, tagKeys.size(), SharedSerde.TagKeyListSerializer.INSTANCE);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T getMemberValue(Schema member) {
        return switch (member.memberIndex()) {
            case 0 -> (T) SchemaUtils.validateSameMember($SCHEMA_RESOURCE_ARN, member, resourceArn);
            case 1 -> (T) SchemaUtils.validateSameMember($SCHEMA_TAG_KEYS, member, tagKeys);
            default -> throw new IllegalArgumentException("Attempted to get non-existent member: " + member.id());
        };
    }

    /**
     * Create a new builder containing all the current property values of this object.
     *
     * <p><strong>Note:</strong> This method performs only a shallow copy of the original properties.
     *
     * @return a builder for {@link UntagResourceInput}.
     */
    public Builder toBuilder() {
        var builder = new Builder();
        builder.resourceArn(this.resourceArn);
        builder.tagKeys(this.tagKeys);
        return builder;
    }

    /**
     * @return returns a new Builder.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for {@link UntagResourceInput}.
     */
    public static final class Builder implements ShapeBuilder<UntagResourceInput> {
        private final PresenceTracker tracker = PresenceTracker.of($SCHEMA);
        private String resourceArn;
        private List<String> tagKeys;

        private Builder() {}

        @Override
        public Schema schema() {
            return $SCHEMA;
        }

        /**
         * The DynamoDB resource that the tags will be removed from. This value is an Amazon Resource Name (ARN).
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
         * A list of tag keys. Existing tags of the resource whose keys are members of this list will be removed from the
         * DynamoDB resource.
         *
         * <p><strong>Required</strong>
         * @return this builder.
         */
        public Builder tagKeys(List<String> tagKeys) {
            this.tagKeys = Objects.requireNonNull(tagKeys, "tagKeys cannot be null");
            tracker.setMember($SCHEMA_TAG_KEYS);
            return this;
        }

        @Override
        public UntagResourceInput build() {
            tracker.validate();
            return new UntagResourceInput(this);
        }

        @Override
        @SuppressWarnings("unchecked")
        public void setMemberValue(Schema member, Object value) {
            switch (member.memberIndex()) {
                case 0 -> resourceArn((String) SchemaUtils.validateSameMember($SCHEMA_RESOURCE_ARN, member, value));
                case 1 -> tagKeys((List<String>) SchemaUtils.validateSameMember($SCHEMA_TAG_KEYS, member, value));
                default -> ShapeBuilder.super.setMemberValue(member, value);
            }
        }

        @Override
        public ShapeBuilder<UntagResourceInput> errorCorrection() {
            if (tracker.allSet()) {
                return this;
            }
            if (!tracker.checkMember($SCHEMA_RESOURCE_ARN)) {
                resourceArn("");
            }
            if (!tracker.checkMember($SCHEMA_TAG_KEYS)) {
                tagKeys(Collections.emptyList());
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
                    case 1 -> builder.tagKeys(SharedSerde.deserializeTagKeyList(member, de));
                    default -> throw new IllegalArgumentException("Unexpected member: " + member.memberName());
                }
            }
        }
    }
}
