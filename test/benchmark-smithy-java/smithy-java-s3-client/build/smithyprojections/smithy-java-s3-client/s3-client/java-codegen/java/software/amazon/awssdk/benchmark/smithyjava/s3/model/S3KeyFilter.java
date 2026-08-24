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
 * A container for object key name prefix and suffix filtering rules.
 */
@SmithyGenerated
public final class S3KeyFilter implements SerializableStruct {

    public static final Schema $SCHEMA = Schemas2.S3_KEY_FILTER;
    private static final Schema $SCHEMA_FILTER_RULES = $SCHEMA.member("FilterRules");

    public static final ShapeId $ID = $SCHEMA.id();

    private final transient List<FilterRule> filterRules;

    private S3KeyFilter(Builder builder) {
        this.filterRules = builder.filterRules == null ? null : Collections.unmodifiableList(builder.filterRules);
    }

    public List<FilterRule> getFilterRules() {
        if (filterRules == null) {
            return Collections.emptyList();
        }
        return filterRules;
    }

    public boolean hasFilterRules() {
        return filterRules != null;
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
        S3KeyFilter that = (S3KeyFilter) other;
        return Objects.equals(this.filterRules, that.filterRules);
    }

    @Override
    public int hashCode() {
        int $hc = Objects.hashCode(filterRules);
        return $hc;
    }

    @Override
    public Schema schema() {
        return $SCHEMA;
    }

    @Override
    public void serializeMembers(ShapeSerializer serializer) {
        if (filterRules != null) {
            serializer.writeList($SCHEMA_FILTER_RULES, filterRules, filterRules.size(), SharedSerde.FilterRuleListSerializer.INSTANCE);
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T getMemberValue(Schema member) {
        return switch (member.memberIndex()) {
            case 0 -> (T) SchemaUtils.validateSameMember($SCHEMA_FILTER_RULES, member, filterRules);
            default -> throw new IllegalArgumentException("Attempted to get non-existent member: " + member.id());
        };
    }

    /**
     * Create a new builder containing all the current property values of this object.
     *
     * <p><strong>Note:</strong> This method performs only a shallow copy of the original properties.
     *
     * @return a builder for {@link S3KeyFilter}.
     */
    public Builder toBuilder() {
        var builder = new Builder();
        builder.filterRules(this.filterRules);
        return builder;
    }

    /**
     * @return returns a new Builder.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for {@link S3KeyFilter}.
     */
    public static final class Builder implements ShapeBuilder<S3KeyFilter> {
        private List<FilterRule> filterRules;

        private Builder() {}

        @Override
        public Schema schema() {
            return $SCHEMA;
        }

        /**
         * @return this builder.
         */
        public Builder filterRules(List<FilterRule> filterRules) {
            this.filterRules = filterRules;
            return this;
        }

        @Override
        public S3KeyFilter build() {
            return new S3KeyFilter(this);
        }

        @Override
        @SuppressWarnings("unchecked")
        public void setMemberValue(Schema member, Object value) {
            switch (member.memberIndex()) {
                case 0 -> filterRules((List<FilterRule>) SchemaUtils.validateSameMember($SCHEMA_FILTER_RULES, member, value));
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
                    case 0 -> builder.filterRules(SharedSerde.deserializeFilterRuleList(member, de));
                    default -> throw new IllegalArgumentException("Unexpected member: " + member.memberName());
                }
            }
        }
    }
}
