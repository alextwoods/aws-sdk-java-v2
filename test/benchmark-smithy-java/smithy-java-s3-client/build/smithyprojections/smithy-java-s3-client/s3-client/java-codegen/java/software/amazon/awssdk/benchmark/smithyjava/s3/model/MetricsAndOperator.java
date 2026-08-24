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
 * A conjunction (logical AND) of predicates, which is used in evaluating a metrics filter. The operator must have at
 * least two predicates, and an object must match all of the predicates in order for the filter to apply.
 */
@SmithyGenerated
public final class MetricsAndOperator implements SerializableStruct {

    public static final Schema $SCHEMA = Schemas2.METRICS_AND_OPERATOR;
    private static final Schema $SCHEMA_PREFIX = $SCHEMA.member("Prefix");
    private static final Schema $SCHEMA_TAGS = $SCHEMA.member("Tags");
    private static final Schema $SCHEMA_ACCESS_POINT_ARN = $SCHEMA.member("AccessPointArn");

    public static final ShapeId $ID = $SCHEMA.id();

    private final transient String prefix;
    private final transient List<Tag> tags;
    private final transient String accessPointArn;

    private MetricsAndOperator(Builder builder) {
        this.prefix = builder.prefix;
        this.tags = builder.tags == null ? null : Collections.unmodifiableList(builder.tags);
        this.accessPointArn = builder.accessPointArn;
    }

    /**
     * The prefix used when evaluating an AND predicate.
     */
    public String getPrefix() {
        return prefix;
    }

    /**
     * The list of tags used when evaluating an AND predicate.
     *
     * <p><code>Tag</code> filters are not supported for directory buckets.
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
     * The access point ARN used when evaluating an <code>AND</code> predicate.
     */
    public String getAccessPointArn() {
        return accessPointArn;
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
        MetricsAndOperator that = (MetricsAndOperator) other;
        return Objects.equals(this.prefix, that.prefix)
               && Objects.equals(this.accessPointArn, that.accessPointArn)
               && Objects.equals(this.tags, that.tags);
    }

    @Override
    public int hashCode() {
        int $hc = Objects.hashCode(prefix);
        $hc = 31 * $hc + Objects.hashCode(tags);
        $hc = 31 * $hc + Objects.hashCode(accessPointArn);
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
        if (accessPointArn != null) {
            serializer.writeString($SCHEMA_ACCESS_POINT_ARN, accessPointArn);
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T getMemberValue(Schema member) {
        return switch (member.memberIndex()) {
            case 0 -> (T) SchemaUtils.validateSameMember($SCHEMA_PREFIX, member, prefix);
            case 1 -> (T) SchemaUtils.validateSameMember($SCHEMA_TAGS, member, tags);
            case 2 -> (T) SchemaUtils.validateSameMember($SCHEMA_ACCESS_POINT_ARN, member, accessPointArn);
            default -> throw new IllegalArgumentException("Attempted to get non-existent member: " + member.id());
        };
    }

    /**
     * Create a new builder containing all the current property values of this object.
     *
     * <p><strong>Note:</strong> This method performs only a shallow copy of the original properties.
     *
     * @return a builder for {@link MetricsAndOperator}.
     */
    public Builder toBuilder() {
        var builder = new Builder();
        builder.prefix(this.prefix);
        builder.tags(this.tags);
        builder.accessPointArn(this.accessPointArn);
        return builder;
    }

    /**
     * @return returns a new Builder.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for {@link MetricsAndOperator}.
     */
    public static final class Builder implements ShapeBuilder<MetricsAndOperator> {
        private String prefix;
        private List<Tag> tags;
        private String accessPointArn;

        private Builder() {}

        @Override
        public Schema schema() {
            return $SCHEMA;
        }

        /**
         * The prefix used when evaluating an AND predicate.
         *
         * @return this builder.
         */
        public Builder prefix(String prefix) {
            this.prefix = prefix;
            return this;
        }

        /**
         * The list of tags used when evaluating an AND predicate.
         *
         * <p><code>Tag</code> filters are not supported for directory buckets.
         *
         * @return this builder.
         */
        public Builder tags(List<Tag> tags) {
            this.tags = tags;
            return this;
        }

        /**
         * The access point ARN used when evaluating an <code>AND</code> predicate.
         *
         * @return this builder.
         */
        public Builder accessPointArn(String accessPointArn) {
            this.accessPointArn = accessPointArn;
            return this;
        }

        @Override
        public MetricsAndOperator build() {
            return new MetricsAndOperator(this);
        }

        @Override
        @SuppressWarnings("unchecked")
        public void setMemberValue(Schema member, Object value) {
            switch (member.memberIndex()) {
                case 0 -> prefix((String) SchemaUtils.validateSameMember($SCHEMA_PREFIX, member, value));
                case 1 -> tags((List<Tag>) SchemaUtils.validateSameMember($SCHEMA_TAGS, member, value));
                case 2 -> accessPointArn((String) SchemaUtils.validateSameMember($SCHEMA_ACCESS_POINT_ARN, member, value));
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
                    case 2 -> builder.accessPointArn(de.readString(member));
                    default -> throw new IllegalArgumentException("Unexpected member: " + member.memberName());
                }
            }
        }
    }
}
