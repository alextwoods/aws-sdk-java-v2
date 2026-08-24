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
 * The <code>Filter</code> is used to identify objects that a Lifecycle Rule applies to. A <code>Filter</code> can have
 * exactly one of <code>Prefix</code>, <code>Tag</code>, <code>ObjectSizeGreaterThan</code>,
 * <code>ObjectSizeLessThan</code>, or <code>And</code> specified. If the <code>Filter</code> element is left empty, the
 * Lifecycle Rule applies to all objects in the bucket.
 */
@SmithyGenerated
public final class LifecycleRuleFilter implements SerializableStruct {

    public static final Schema $SCHEMA = Schemas2.LIFECYCLE_RULE_FILTER;
    private static final Schema $SCHEMA_PREFIX = $SCHEMA.member("Prefix");
    private static final Schema $SCHEMA_TAG = $SCHEMA.member("Tag");
    private static final Schema $SCHEMA_OBJECT_SIZE_GREATER_THAN = $SCHEMA.member("ObjectSizeGreaterThan");
    private static final Schema $SCHEMA_OBJECT_SIZE_LESS_THAN = $SCHEMA.member("ObjectSizeLessThan");
    private static final Schema $SCHEMA_AND = $SCHEMA.member("And");

    public static final ShapeId $ID = $SCHEMA.id();

    private final transient String prefix;
    private final transient Tag tag;
    private final transient Long objectSizeGreaterThan;
    private final transient Long objectSizeLessThan;
    private final transient LifecycleRuleAndOperator and;

    private LifecycleRuleFilter(Builder builder) {
        this.prefix = builder.prefix;
        this.tag = builder.tag;
        this.objectSizeGreaterThan = builder.objectSizeGreaterThan;
        this.objectSizeLessThan = builder.objectSizeLessThan;
        this.and = builder.and;
    }

    /**
     * Prefix identifying one or more objects to which the rule applies.
     *
     * <p>Replacement must be made for object keys containing special characters (such as carriage returns) when using
     * XML requests. For more information, see <a href="https://docs.aws.amazon.com/AmazonS3/latest/userguide/object-keys.html#object-key-xml-related-constraints"> XML related object key constraints</a>.
     */
    public String getPrefix() {
        return prefix;
    }

    /**
     * This tag must exist in the object's tag set in order for the rule to apply.
     *
     * <p>This parameter applies to general purpose buckets only. It is not supported for directory bucket lifecycle
     * configurations.
     */
    public Tag getTag() {
        return tag;
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

    public LifecycleRuleAndOperator getAnd() {
        return and;
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
        LifecycleRuleFilter that = (LifecycleRuleFilter) other;
        return Objects.equals(this.objectSizeGreaterThan, that.objectSizeGreaterThan)
               && Objects.equals(this.objectSizeLessThan, that.objectSizeLessThan)
               && Objects.equals(this.prefix, that.prefix)
               && Objects.equals(this.tag, that.tag)
               && Objects.equals(this.and, that.and);
    }

    @Override
    public int hashCode() {
        int $hc = Objects.hashCode(prefix);
        $hc = 31 * $hc + Objects.hashCode(tag);
        $hc = 31 * $hc + Objects.hashCode(objectSizeGreaterThan);
        $hc = 31 * $hc + Objects.hashCode(objectSizeLessThan);
        $hc = 31 * $hc + Objects.hashCode(and);
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
        if (tag != null) {
            serializer.writeStruct($SCHEMA_TAG, tag);
        }
        if (objectSizeGreaterThan != null) {
            serializer.writeLong($SCHEMA_OBJECT_SIZE_GREATER_THAN, objectSizeGreaterThan);
        }
        if (objectSizeLessThan != null) {
            serializer.writeLong($SCHEMA_OBJECT_SIZE_LESS_THAN, objectSizeLessThan);
        }
        if (and != null) {
            serializer.writeStruct($SCHEMA_AND, and);
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T getMemberValue(Schema member) {
        return switch (member.memberIndex()) {
            case 0 -> (T) SchemaUtils.validateSameMember($SCHEMA_PREFIX, member, prefix);
            case 1 -> (T) SchemaUtils.validateSameMember($SCHEMA_TAG, member, tag);
            case 2 -> (T) SchemaUtils.validateSameMember($SCHEMA_OBJECT_SIZE_GREATER_THAN, member, objectSizeGreaterThan);
            case 3 -> (T) SchemaUtils.validateSameMember($SCHEMA_OBJECT_SIZE_LESS_THAN, member, objectSizeLessThan);
            case 4 -> (T) SchemaUtils.validateSameMember($SCHEMA_AND, member, and);
            default -> throw new IllegalArgumentException("Attempted to get non-existent member: " + member.id());
        };
    }

    /**
     * Create a new builder containing all the current property values of this object.
     *
     * <p><strong>Note:</strong> This method performs only a shallow copy of the original properties.
     *
     * @return a builder for {@link LifecycleRuleFilter}.
     */
    public Builder toBuilder() {
        var builder = new Builder();
        builder.prefix(this.prefix);
        builder.tag(this.tag);
        builder.objectSizeGreaterThan(this.objectSizeGreaterThan);
        builder.objectSizeLessThan(this.objectSizeLessThan);
        builder.and(this.and);
        return builder;
    }

    /**
     * @return returns a new Builder.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for {@link LifecycleRuleFilter}.
     */
    public static final class Builder implements ShapeBuilder<LifecycleRuleFilter> {
        private String prefix;
        private Tag tag;
        private Long objectSizeGreaterThan;
        private Long objectSizeLessThan;
        private LifecycleRuleAndOperator and;

        private Builder() {}

        @Override
        public Schema schema() {
            return $SCHEMA;
        }

        /**
         * Prefix identifying one or more objects to which the rule applies.
         *
         * <p>Replacement must be made for object keys containing special characters (such as carriage returns) when using
         * XML requests. For more information, see <a href="https://docs.aws.amazon.com/AmazonS3/latest/userguide/object-keys.html#object-key-xml-related-constraints"> XML related object key constraints</a>.
         *
         * @return this builder.
         */
        public Builder prefix(String prefix) {
            this.prefix = prefix;
            return this;
        }

        /**
         * This tag must exist in the object's tag set in order for the rule to apply.
         *
         * <p>This parameter applies to general purpose buckets only. It is not supported for directory bucket lifecycle
         * configurations.
         *
         * @return this builder.
         */
        public Builder tag(Tag tag) {
            this.tag = tag;
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

        /**
         * @return this builder.
         */
        public Builder and(LifecycleRuleAndOperator and) {
            this.and = and;
            return this;
        }

        @Override
        public LifecycleRuleFilter build() {
            return new LifecycleRuleFilter(this);
        }

        @Override
        @SuppressWarnings("unchecked")
        public void setMemberValue(Schema member, Object value) {
            switch (member.memberIndex()) {
                case 0 -> prefix((String) SchemaUtils.validateSameMember($SCHEMA_PREFIX, member, value));
                case 1 -> tag((Tag) SchemaUtils.validateSameMember($SCHEMA_TAG, member, value));
                case 2 -> objectSizeGreaterThan((Long) SchemaUtils.validateSameMember($SCHEMA_OBJECT_SIZE_GREATER_THAN, member, value));
                case 3 -> objectSizeLessThan((Long) SchemaUtils.validateSameMember($SCHEMA_OBJECT_SIZE_LESS_THAN, member, value));
                case 4 -> and((LifecycleRuleAndOperator) SchemaUtils.validateSameMember($SCHEMA_AND, member, value));
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
                    case 1 -> builder.tag(Tag.builder().deserializeMember(de, member).build());
                    case 2 -> builder.objectSizeGreaterThan(de.readLong(member));
                    case 3 -> builder.objectSizeLessThan(de.readLong(member));
                    case 4 -> builder.and(LifecycleRuleAndOperator.builder().deserializeMember(de, member).build());
                    default -> throw new IllegalArgumentException("Unexpected member: " + member.memberName());
                }
            }
        }
    }
}
