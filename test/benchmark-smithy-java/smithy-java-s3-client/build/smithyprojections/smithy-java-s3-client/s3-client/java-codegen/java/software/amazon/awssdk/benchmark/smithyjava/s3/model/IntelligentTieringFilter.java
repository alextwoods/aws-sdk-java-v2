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
 * The <code>Filter</code> is used to identify objects that the S3 Intelligent-Tiering configuration applies to.
 */
@SmithyGenerated
public final class IntelligentTieringFilter implements SerializableStruct {

    public static final Schema $SCHEMA = Schemas2.INTELLIGENT_TIERING_FILTER;
    private static final Schema $SCHEMA_PREFIX = $SCHEMA.member("Prefix");
    private static final Schema $SCHEMA_TAG = $SCHEMA.member("Tag");
    private static final Schema $SCHEMA_AND = $SCHEMA.member("And");

    public static final ShapeId $ID = $SCHEMA.id();

    private final transient String prefix;
    private final transient Tag tag;
    private final transient IntelligentTieringAndOperator and;

    private IntelligentTieringFilter(Builder builder) {
        this.prefix = builder.prefix;
        this.tag = builder.tag;
        this.and = builder.and;
    }

    /**
     * An object key name prefix that identifies the subset of objects to which the rule applies.
     *
     * <p>Replacement must be made for object keys containing special characters (such as carriage returns) when using
     * XML requests. For more information, see <a href="https://docs.aws.amazon.com/AmazonS3/latest/userguide/object-keys.html#object-key-xml-related-constraints"> XML related object key constraints</a>.
     */
    public String getPrefix() {
        return prefix;
    }

    public Tag getTag() {
        return tag;
    }

    /**
     * A conjunction (logical AND) of predicates, which is used in evaluating a metrics filter. The operator must have
     * at least two predicates, and an object must match all of the predicates in order for the filter to apply.
     */
    public IntelligentTieringAndOperator getAnd() {
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
        IntelligentTieringFilter that = (IntelligentTieringFilter) other;
        return Objects.equals(this.prefix, that.prefix)
               && Objects.equals(this.tag, that.tag)
               && Objects.equals(this.and, that.and);
    }

    @Override
    public int hashCode() {
        int $hc = Objects.hashCode(prefix);
        $hc = 31 * $hc + Objects.hashCode(tag);
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
            case 2 -> (T) SchemaUtils.validateSameMember($SCHEMA_AND, member, and);
            default -> throw new IllegalArgumentException("Attempted to get non-existent member: " + member.id());
        };
    }

    /**
     * Create a new builder containing all the current property values of this object.
     *
     * <p><strong>Note:</strong> This method performs only a shallow copy of the original properties.
     *
     * @return a builder for {@link IntelligentTieringFilter}.
     */
    public Builder toBuilder() {
        var builder = new Builder();
        builder.prefix(this.prefix);
        builder.tag(this.tag);
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
     * Builder for {@link IntelligentTieringFilter}.
     */
    public static final class Builder implements ShapeBuilder<IntelligentTieringFilter> {
        private String prefix;
        private Tag tag;
        private IntelligentTieringAndOperator and;

        private Builder() {}

        @Override
        public Schema schema() {
            return $SCHEMA;
        }

        /**
         * An object key name prefix that identifies the subset of objects to which the rule applies.
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
         * @return this builder.
         */
        public Builder tag(Tag tag) {
            this.tag = tag;
            return this;
        }

        /**
         * A conjunction (logical AND) of predicates, which is used in evaluating a metrics filter. The operator must have
         * at least two predicates, and an object must match all of the predicates in order for the filter to apply.
         *
         * @return this builder.
         */
        public Builder and(IntelligentTieringAndOperator and) {
            this.and = and;
            return this;
        }

        @Override
        public IntelligentTieringFilter build() {
            return new IntelligentTieringFilter(this);
        }

        @Override
        @SuppressWarnings("unchecked")
        public void setMemberValue(Schema member, Object value) {
            switch (member.memberIndex()) {
                case 0 -> prefix((String) SchemaUtils.validateSameMember($SCHEMA_PREFIX, member, value));
                case 1 -> tag((Tag) SchemaUtils.validateSameMember($SCHEMA_TAG, member, value));
                case 2 -> and((IntelligentTieringAndOperator) SchemaUtils.validateSameMember($SCHEMA_AND, member, value));
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
                    case 2 -> builder.and(IntelligentTieringAndOperator.builder().deserializeMember(de, member).build());
                    default -> throw new IllegalArgumentException("Unexpected member: " + member.memberName());
                }
            }
        }
    }
}
