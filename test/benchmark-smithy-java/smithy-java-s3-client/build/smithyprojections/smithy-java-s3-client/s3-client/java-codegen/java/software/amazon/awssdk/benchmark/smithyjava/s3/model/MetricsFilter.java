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
 * Specifies a metrics configuration filter. The metrics configuration only includes objects that meet the filter's
 * criteria. A filter must be a prefix, an object tag, an access point ARN, or a conjunction (MetricsAndOperator). For
 * more information, see <a
 * href="https://docs.aws.amazon.com/AmazonS3/latest/API/API_PutBucketMetricsConfiguration.html">PutBucketMetricsConfiguration</a>.
 */
@SmithyGenerated
public sealed interface MetricsFilter extends SerializableStruct {
    Schema $SCHEMA = Schemas2.METRICS_FILTER;

    ShapeId $ID = $SCHEMA.id();

    <T> T getValue();

    @Override
    default Schema schema() {
        return $SCHEMA;
    }

    @Override
    default <T> T getMemberValue(Schema member) {
        return SchemaUtils.validateMemberInSchema($SCHEMA, member, getValue());
    }

    /**
     * The prefix used when evaluating a metrics filter.
     */
    @SmithyGenerated
    record PrefixMember(String prefix) implements MetricsFilter {
        private static final Schema $SCHEMA_PREFIX = $SCHEMA.member("Prefix");
        public PrefixMember {
            Objects.requireNonNull(prefix, "Union value cannot be null");
        }
        @Override
        public void serializeMembers(ShapeSerializer serializer) {
            serializer.writeString($SCHEMA_PREFIX, prefix);
        }

        /**
         * The prefix used when evaluating a metrics filter.
         */
        @Override
        public String getValue() {
            return prefix;
        }

        @Override
        public String toString() {
            return ToStringSerializer.serialize(this);
        }

    }

    /**
     * The tag used when evaluating a metrics filter.
     *
     * <p><code>Tag</code> filters are not supported for directory buckets.
     */
    @SmithyGenerated
    record TagMember(Tag tag) implements MetricsFilter {
        private static final Schema $SCHEMA_TAG = $SCHEMA.member("Tag");
        public TagMember {
            Objects.requireNonNull(tag, "Union value cannot be null");
        }
        @Override
        public void serializeMembers(ShapeSerializer serializer) {
            serializer.writeStruct($SCHEMA_TAG, tag);
        }

        /**
         * The tag used when evaluating a metrics filter.
         *
         * <p><code>Tag</code> filters are not supported for directory buckets.
         */
        @Override
        public Tag getValue() {
            return tag;
        }

        @Override
        public String toString() {
            return ToStringSerializer.serialize(this);
        }

    }

    /**
     * The access point ARN used when evaluating a metrics filter.
     */
    @SmithyGenerated
    record AccessPointArnMember(String accessPointArn) implements MetricsFilter {
        private static final Schema $SCHEMA_ACCESS_POINT_ARN = $SCHEMA.member("AccessPointArn");
        public AccessPointArnMember {
            Objects.requireNonNull(accessPointArn, "Union value cannot be null");
        }
        @Override
        public void serializeMembers(ShapeSerializer serializer) {
            serializer.writeString($SCHEMA_ACCESS_POINT_ARN, accessPointArn);
        }

        /**
         * The access point ARN used when evaluating a metrics filter.
         */
        @Override
        public String getValue() {
            return accessPointArn;
        }

        @Override
        public String toString() {
            return ToStringSerializer.serialize(this);
        }

    }

    /**
     * A conjunction (logical AND) of predicates, which is used in evaluating a metrics filter. The operator must have at
     * least two predicates, and an object must match all of the predicates in order for the filter to apply.
     */
    @SmithyGenerated
    record AndMember(MetricsAndOperator and) implements MetricsFilter {
        private static final Schema $SCHEMA_AND = $SCHEMA.member("And");
        public AndMember {
            Objects.requireNonNull(and, "Union value cannot be null");
        }
        @Override
        public void serializeMembers(ShapeSerializer serializer) {
            serializer.writeStruct($SCHEMA_AND, and);
        }

        /**
         * A conjunction (logical AND) of predicates, which is used in evaluating a metrics filter. The operator must have
         * at least two predicates, and an object must match all of the predicates in order for the filter to apply.
         */
        @Override
        public MetricsAndOperator getValue() {
            return and;
        }

        @Override
        public String toString() {
            return ToStringSerializer.serialize(this);
        }

    }

    record $Unknown(String memberName) implements MetricsFilter {
        @Override
        public void serialize(ShapeSerializer serializer) {
            throw new UnsupportedOperationException("Cannot serialize union with unknown member " + this.memberName);
        }

        @Override
        public void serializeMembers(ShapeSerializer serializer) {}

        @Override
        public String getValue() {
            return memberName;
        }

        private record $Hidden() implements MetricsFilter {
            @Override
            public void serializeMembers(ShapeSerializer serializer) {}

            @Override
            @SuppressWarnings("unchecked")
            public <T> T getValue() {
                return null;
            }
        }
    }

    interface BuildStage {
        MetricsFilter build();
    }

    /**
     * @return returns a new Builder.
     */
    static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for {@link MetricsFilter}.
     */
    final class Builder implements ShapeBuilder<MetricsFilter>, BuildStage {
        private MetricsFilter value;

        private Builder() {}

        @Override
        public Schema schema() {
            return $SCHEMA;
        }

        public BuildStage prefix(String value) {
            return setValue(new PrefixMember(value));
        }

        public BuildStage tag(Tag value) {
            return setValue(new TagMember(value));
        }

        public BuildStage accessPointArn(String value) {
            return setValue(new AccessPointArnMember(value));
        }

        public BuildStage and(MetricsAndOperator value) {
            return setValue(new AndMember(value));
        }

        public BuildStage $unknownMember(String memberName) {
            return setValue(new $Unknown(memberName));
        }

        private BuildStage setValue(MetricsFilter value) {
            if (this.value != null) {
                throw new IllegalArgumentException("Only one value may be set for unions");
            }
            this.value = value;
            return this;
        }

        @Override
        public MetricsFilter build() {
            return Objects.requireNonNull(value, "no union value set");
        }

        @Override
        @SuppressWarnings("unchecked")
        public void setMemberValue(Schema member, Object value) {
            switch (member.memberIndex()) {
                case 0 -> prefix((String) SchemaUtils.validateSameMember(PrefixMember.$SCHEMA_PREFIX, member, value));
                case 1 -> tag((Tag) SchemaUtils.validateSameMember(TagMember.$SCHEMA_TAG, member, value));
                case 2 -> accessPointArn((String) SchemaUtils.validateSameMember(AccessPointArnMember.$SCHEMA_ACCESS_POINT_ARN, member, value));
                case 3 -> and((MetricsAndOperator) SchemaUtils.validateSameMember(AndMember.$SCHEMA_AND, member, value));
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
                    case 2 -> builder.accessPointArn(de.readString(member));
                    case 3 -> builder.and(MetricsAndOperator.builder().deserializeMember(de, member).build());
                    default -> throw new IllegalArgumentException("Unexpected member: " + member.memberName());
                }
            }

            @Override
            public void unknownMember(Builder builder, String memberName) {
                builder.$unknownMember(memberName);
            }
        }
    }
}
