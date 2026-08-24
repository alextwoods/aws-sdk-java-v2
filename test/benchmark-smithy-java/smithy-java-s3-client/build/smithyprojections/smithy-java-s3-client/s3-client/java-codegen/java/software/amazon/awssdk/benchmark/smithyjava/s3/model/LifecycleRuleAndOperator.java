package software.amazon.awssdk.benchmark.smithyjava.s3.model;

import java.util.Collections;
import java.util.List;
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
 * This is used in a Lifecycle Rule Filter to apply a logical AND to two or more predicates. The Lifecycle Rule will
 * apply to any object matching all of the predicates configured inside the And operator.
 */
@SmithyGenerated
public final class LifecycleRuleAndOperator implements SerializableStruct {

    public static final Schema $SCHEMA = Schemas2.LIFECYCLE_RULE_AND_OPERATOR;
    private static final Schema $SCHEMA_PREFIX = $SCHEMA.member("Prefix");
    private static final Schema $SCHEMA_TAGS = $SCHEMA.member("Tags");
    private static final Schema $SCHEMA_OBJECT_SIZE_GREATER_THAN = $SCHEMA.member("ObjectSizeGreaterThan");
    private static final Schema $SCHEMA_OBJECT_SIZE_LESS_THAN = $SCHEMA.member("ObjectSizeLessThan");

    public static final ShapeId $ID = $SCHEMA.id();

    private final transient String prefix;
    private final transient List<Tag> tags;
    private final transient Long objectSizeGreaterThan;
    private final transient Long objectSizeLessThan;

    private LifecycleRuleAndOperator(Builder builder) {
        this.prefix = builder.prefix;
        this.tags = builder.tags == null ? null : Collections.unmodifiableList(builder.tags);
        this.objectSizeGreaterThan = builder.objectSizeGreaterThan;
        this.objectSizeLessThan = builder.objectSizeLessThan;
    }

    /**
     * Prefix identifying one or more objects to which the rule applies.
     */
    public String getPrefix() {
        return prefix;
    }

    /**
     * All of these tags must exist in the object's tag set in order for the rule to apply.
     */
    public List<Tag> getTags() {
        if (tags == null) {
            return Collections.emptyList();
        }
        return tags;
    }

    public boolean hasTags() {
        return tags != null;
    }

    /**
     * Minimum object size to which the rule applies.
     */
    public Long getObjectSizeGreaterThan() {
        return objectSizeGreaterThan;
    }

    /**
     * Maximum object size to which the rule applies.
     */
    public Long getObjectSizeLessThan() {
        return objectSizeLessThan;
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
        LifecycleRuleAndOperator that = (LifecycleRuleAndOperator) other;
        return Objects.equals(this.objectSizeGreaterThan, that.objectSizeGreaterThan)
               && Objects.equals(this.objectSizeLessThan, that.objectSizeLessThan)
               && Objects.equals(this.prefix, that.prefix)
               && Objects.equals(this.tags, that.tags);
    }

    @Override
    public int hashCode() {
        int $hc = Objects.hashCode(prefix);
        $hc = 31 * $hc + Objects.hashCode(tags);
        $hc = 31 * $hc + Objects.hashCode(objectSizeGreaterThan);
        $hc = 31 * $hc + Objects.hashCode(objectSizeLessThan);
        return $hc;
    }

    @Override
    public Schema schema() {
        return $SCHEMA;
    }

    @Override
    public void serializeMembers(ShapeSerializer serializer) {
        if (prefix != null) {
            serializer.writeString($SCHEMA_PREFIX, prefix);
        }
        if (tags != null) {
            serializer.writeList($SCHEMA_TAGS, tags, tags.size(), SharedSerde.TagSetSerializer.INSTANCE);
        }
        if (objectSizeGreaterThan != null) {
            serializer.writeLong($SCHEMA_OBJECT_SIZE_GREATER_THAN, objectSizeGreaterThan);
        }
        if (objectSizeLessThan != null) {
            serializer.writeLong($SCHEMA_OBJECT_SIZE_LESS_THAN, objectSizeLessThan);
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T getMemberValue(Schema member) {
        return switch (member.memberIndex()) {
            case 0 -> (T) SchemaUtils.validateSameMember($SCHEMA_PREFIX, member, prefix);
            case 1 -> (T) SchemaUtils.validateSameMember($SCHEMA_TAGS, member, tags);
            case 2 -> (T) SchemaUtils.validateSameMember($SCHEMA_OBJECT_SIZE_GREATER_THAN, member, objectSizeGreaterThan);
            case 3 -> (T) SchemaUtils.validateSameMember($SCHEMA_OBJECT_SIZE_LESS_THAN, member, objectSizeLessThan);
            default -> throw new IllegalArgumentException("Attempted to get non-existent member: " + member.id());
        };
    }

    /**
     * Create a new builder containing all the current property values of this object.
     *
     * <p><strong>Note:</strong> This method performs only a shallow copy of the original properties.
     *
     * @return a builder for {@link LifecycleRuleAndOperator}.
     */
    public Builder toBuilder() {
        var builder = new Builder();
        builder.prefix(this.prefix);
        builder.tags(this.tags);
        builder.objectSizeGreaterThan(this.objectSizeGreaterThan);
        builder.objectSizeLessThan(this.objectSizeLessThan);
        return builder;
    }

    /**
     * @return returns a new Builder.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for {@link LifecycleRuleAndOperator}.
     */
    public static final class Builder implements ShapeBuilder<LifecycleRuleAndOperator> {
        private String prefix;
        private List<Tag> tags;
        private Long objectSizeGreaterThan;
        private Long objectSizeLessThan;

        private Builder() {}

        @Override
        public Schema schema() {
            return $SCHEMA;
        }

        /**
         * Prefix identifying one or more objects to which the rule applies.
         *
         * @return this builder.
         */
        public Builder prefix(String prefix) {
            this.prefix = prefix;
            return this;
        }

        /**
         * All of these tags must exist in the object's tag set in order for the rule to apply.
         *
         * @return this builder.
         */
        public Builder tags(List<Tag> tags) {
            this.tags = tags;
            return this;
        }

        /**
         * Minimum object size to which the rule applies.
         *
         * @return this builder.
         */
        public Builder objectSizeGreaterThan(Long objectSizeGreaterThan) {
            this.objectSizeGreaterThan = objectSizeGreaterThan;
            return this;
        }

        /**
         * Maximum object size to which the rule applies.
         *
         * @return this builder.
         */
        public Builder objectSizeLessThan(Long objectSizeLessThan) {
            this.objectSizeLessThan = objectSizeLessThan;
            return this;
        }

        @Override
        public LifecycleRuleAndOperator build() {
            return new LifecycleRuleAndOperator(this);
        }

        @Override
        @SuppressWarnings("unchecked")
        public void setMemberValue(Schema member, Object value) {
            switch (member.memberIndex()) {
                case 0 -> prefix((String) SchemaUtils.validateSameMember($SCHEMA_PREFIX, member, value));
                case 1 -> tags((List<Tag>) SchemaUtils.validateSameMember($SCHEMA_TAGS, member, value));
                case 2 -> objectSizeGreaterThan((Long) SchemaUtils.validateSameMember($SCHEMA_OBJECT_SIZE_GREATER_THAN, member, value));
                case 3 -> objectSizeLessThan((Long) SchemaUtils.validateSameMember($SCHEMA_OBJECT_SIZE_LESS_THAN, member, value));
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
                    case 0 -> builder.prefix(de.readString(member));
                    case 1 -> builder.tags(SharedSerde.deserializeTagSet(member, de));
                    case 2 -> builder.objectSizeGreaterThan(de.readLong(member));
                    case 3 -> builder.objectSizeLessThan(de.readLong(member));
                    default -> throw new IllegalArgumentException("Unexpected member: " + member.memberName());
                }
            }
        }
    }
}
