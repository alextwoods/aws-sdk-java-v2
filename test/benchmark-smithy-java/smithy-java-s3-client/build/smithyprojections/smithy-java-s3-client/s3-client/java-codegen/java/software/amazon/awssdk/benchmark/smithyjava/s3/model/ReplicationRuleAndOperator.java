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
 * A container for specifying rule filters. The filters determine the subset of objects to which the rule applies. This
 * element is required only if you specify more than one filter.
 *
 * <p>For example:
 *
 * <ul>
 *   <li>
 *     If you specify both a <code>Prefix</code> and a <code>Tag</code> filter, wrap these filters in an <code>And</code>
 *     tag.
 *   </li>
 *   <li>
 *     If you specify a filter based on multiple tags, wrap the <code>Tag</code> elements in an <code>And</code>
 *     tag.
 *   </li>
 * </ul>
 */
@SmithyGenerated
public final class ReplicationRuleAndOperator implements SerializableStruct {

    public static final Schema $SCHEMA = Schemas3.REPLICATION_RULE_AND_OPERATOR;
    private static final Schema $SCHEMA_PREFIX = $SCHEMA.member("Prefix");
    private static final Schema $SCHEMA_TAGS = $SCHEMA.member("Tags");

    public static final ShapeId $ID = $SCHEMA.id();

    private final transient String prefix;
    private final transient List<Tag> tags;

    private ReplicationRuleAndOperator(Builder builder) {
        this.prefix = builder.prefix;
        this.tags = builder.tags == null ? null : Collections.unmodifiableList(builder.tags);
    }

    /**
     * An object key name prefix that identifies the subset of objects to which the rule applies.
     */
    public String getPrefix() {
        return prefix;
    }

    /**
     * An array of tags containing key and value pairs.
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
        ReplicationRuleAndOperator that = (ReplicationRuleAndOperator) other;
        return Objects.equals(this.prefix, that.prefix)
               && Objects.equals(this.tags, that.tags);
    }

    @Override
    public int hashCode() {
        int $hc = Objects.hashCode(prefix);
        $hc = 31 * $hc + Objects.hashCode(tags);
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
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T getMemberValue(Schema member) {
        return switch (member.memberIndex()) {
            case 0 -> (T) SchemaUtils.validateSameMember($SCHEMA_PREFIX, member, prefix);
            case 1 -> (T) SchemaUtils.validateSameMember($SCHEMA_TAGS, member, tags);
            default -> throw new IllegalArgumentException("Attempted to get non-existent member: " + member.id());
        };
    }

    /**
     * Create a new builder containing all the current property values of this object.
     *
     * <p><strong>Note:</strong> This method performs only a shallow copy of the original properties.
     *
     * @return a builder for {@link ReplicationRuleAndOperator}.
     */
    public Builder toBuilder() {
        var builder = new Builder();
        builder.prefix(this.prefix);
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
     * Builder for {@link ReplicationRuleAndOperator}.
     */
    public static final class Builder implements ShapeBuilder<ReplicationRuleAndOperator> {
        private String prefix;
        private List<Tag> tags;

        private Builder() {}

        @Override
        public Schema schema() {
            return $SCHEMA;
        }

        /**
         * An object key name prefix that identifies the subset of objects to which the rule applies.
         *
         * @return this builder.
         */
        public Builder prefix(String prefix) {
            this.prefix = prefix;
            return this;
        }

        /**
         * An array of tags containing key and value pairs.
         *
         * @return this builder.
         */
        public Builder tags(List<Tag> tags) {
            this.tags = tags;
            return this;
        }

        @Override
        public ReplicationRuleAndOperator build() {
            return new ReplicationRuleAndOperator(this);
        }

        @Override
        @SuppressWarnings("unchecked")
        public void setMemberValue(Schema member, Object value) {
            switch (member.memberIndex()) {
                case 0 -> prefix((String) SchemaUtils.validateSameMember($SCHEMA_PREFIX, member, value));
                case 1 -> tags((List<Tag>) SchemaUtils.validateSameMember($SCHEMA_TAGS, member, value));
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
                    default -> throw new IllegalArgumentException("Unexpected member: " + member.memberName());
                }
            }
        }
    }
}
