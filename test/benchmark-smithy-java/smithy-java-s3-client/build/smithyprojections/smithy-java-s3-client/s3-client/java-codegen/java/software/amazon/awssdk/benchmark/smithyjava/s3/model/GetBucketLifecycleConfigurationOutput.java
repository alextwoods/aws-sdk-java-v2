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

@SmithyGenerated
public final class GetBucketLifecycleConfigurationOutput implements SerializableStruct {

    public static final Schema $SCHEMA = Schemas2.GET_BUCKET_LIFECYCLE_CONFIGURATION_OUTPUT;
    private static final Schema $SCHEMA_RULES = $SCHEMA.member("Rules");
    private static final Schema $SCHEMA_TRANSITION_DEFAULT_MINIMUM_OBJECT_SIZE = $SCHEMA.member("TransitionDefaultMinimumObjectSize");

    public static final ShapeId $ID = $SCHEMA.id();

    private final transient List<LifecycleRule> rules;
    private final transient TransitionDefaultMinimumObjectSize transitionDefaultMinimumObjectSize;

    private GetBucketLifecycleConfigurationOutput(Builder builder) {
        this.rules = builder.rules == null ? null : Collections.unmodifiableList(builder.rules);
        this.transitionDefaultMinimumObjectSize = builder.transitionDefaultMinimumObjectSize;
    }

    /**
     * Container for a lifecycle rule.
     */
    public List<LifecycleRule> getRules() {
        if (rules == null) {
            return Collections.emptyList();
        }
        return rules;
    }

    public boolean hasRules() {
        return rules != null;
    }

    /**
     * Indicates which default minimum object size behavior is applied to the lifecycle configuration.
     *
     * <p>This parameter applies to general purpose buckets only. It isn't supported for directory bucket lifecycle
     * configurations.
     *
     * <ul>
     *   <li>
     *     <code>all_storage_classes_128K</code> - Objects smaller than 128 KB will not transition to any storage
     *     class by default.
     *   </li>
     *   <li>
     *     <code>varies_by_storage_class</code> - Objects smaller than 128 KB will transition to Glacier Flexible
     *     Retrieval or Glacier Deep Archive storage classes. By default, all other storage classes will prevent
     *     transitions smaller than 128 KB.
     *   </li>
     * </ul>
     *
     * <p>To customize the minimum object size for any transition you can add a filter that specifies a custom <code>
     * ObjectSizeGreaterThan</code> or <code>ObjectSizeLessThan</code> in the body of your transition rule. Custom
     * filters always take precedence over the default transition behavior.
     */
    public TransitionDefaultMinimumObjectSize getTransitionDefaultMinimumObjectSize() {
        return transitionDefaultMinimumObjectSize;
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
        GetBucketLifecycleConfigurationOutput that = (GetBucketLifecycleConfigurationOutput) other;
        return Objects.equals(this.transitionDefaultMinimumObjectSize, that.transitionDefaultMinimumObjectSize)
               && Objects.equals(this.rules, that.rules);
    }

    @Override
    public int hashCode() {
        int $hc = Objects.hashCode(rules);
        $hc = 31 * $hc + Objects.hashCode(transitionDefaultMinimumObjectSize);
        return $hc;
    }

    @Override
    public Schema schema() {
        return $SCHEMA;
    }

    @Override
    public void serializeMembers(ShapeSerializer serializer) {
        if (rules != null) {
            serializer.writeList($SCHEMA_RULES, rules, rules.size(), SharedSerde.LifecycleRulesSerializer.INSTANCE);
        }
        if (transitionDefaultMinimumObjectSize != null) {
            serializer.writeString($SCHEMA_TRANSITION_DEFAULT_MINIMUM_OBJECT_SIZE, transitionDefaultMinimumObjectSize.getValue());
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T getMemberValue(Schema member) {
        return switch (member.memberIndex()) {
            case 0 -> (T) SchemaUtils.validateSameMember($SCHEMA_RULES, member, rules);
            case 1 -> (T) SchemaUtils.validateSameMember($SCHEMA_TRANSITION_DEFAULT_MINIMUM_OBJECT_SIZE, member, transitionDefaultMinimumObjectSize);
            default -> throw new IllegalArgumentException("Attempted to get non-existent member: " + member.id());
        };
    }

    /**
     * Create a new builder containing all the current property values of this object.
     *
     * <p><strong>Note:</strong> This method performs only a shallow copy of the original properties.
     *
     * @return a builder for {@link GetBucketLifecycleConfigurationOutput}.
     */
    public Builder toBuilder() {
        var builder = new Builder();
        builder.rules(this.rules);
        builder.transitionDefaultMinimumObjectSize(this.transitionDefaultMinimumObjectSize);
        return builder;
    }

    /**
     * @return returns a new Builder.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for {@link GetBucketLifecycleConfigurationOutput}.
     */
    public static final class Builder implements ShapeBuilder<GetBucketLifecycleConfigurationOutput> {
        private List<LifecycleRule> rules;
        private TransitionDefaultMinimumObjectSize transitionDefaultMinimumObjectSize;

        private Builder() {}

        @Override
        public Schema schema() {
            return $SCHEMA;
        }

        /**
         * Container for a lifecycle rule.
         *
         * @return this builder.
         */
        public Builder rules(List<LifecycleRule> rules) {
            this.rules = rules;
            return this;
        }

        /**
         * Indicates which default minimum object size behavior is applied to the lifecycle configuration.
         *
         * <p>This parameter applies to general purpose buckets only. It isn't supported for directory bucket lifecycle
         * configurations.
         *
         * <ul>
         *   <li>
         *     <code>all_storage_classes_128K</code> - Objects smaller than 128 KB will not transition to any storage
         *     class by default.
         *   </li>
         *   <li>
         *     <code>varies_by_storage_class</code> - Objects smaller than 128 KB will transition to Glacier Flexible
         *     Retrieval or Glacier Deep Archive storage classes. By default, all other storage classes will prevent
         *     transitions smaller than 128 KB.
         *   </li>
         * </ul>
         *
         * <p>To customize the minimum object size for any transition you can add a filter that specifies a custom <code>
         * ObjectSizeGreaterThan</code> or <code>ObjectSizeLessThan</code> in the body of your transition rule. Custom
         * filters always take precedence over the default transition behavior.
         *
         * @return this builder.
         */
        public Builder transitionDefaultMinimumObjectSize(TransitionDefaultMinimumObjectSize transitionDefaultMinimumObjectSize) {
            this.transitionDefaultMinimumObjectSize = transitionDefaultMinimumObjectSize;
            return this;
        }

        @Override
        public GetBucketLifecycleConfigurationOutput build() {
            return new GetBucketLifecycleConfigurationOutput(this);
        }

        @Override
        @SuppressWarnings("unchecked")
        public void setMemberValue(Schema member, Object value) {
            switch (member.memberIndex()) {
                case 0 -> rules((List<LifecycleRule>) SchemaUtils.validateSameMember($SCHEMA_RULES, member, value));
                case 1 -> transitionDefaultMinimumObjectSize((TransitionDefaultMinimumObjectSize) SchemaUtils.validateSameMember($SCHEMA_TRANSITION_DEFAULT_MINIMUM_OBJECT_SIZE, member, value));
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
                    case 0 -> builder.rules(SharedSerde.deserializeLifecycleRules(member, de));
                    case 1 -> builder.transitionDefaultMinimumObjectSize(TransitionDefaultMinimumObjectSize.builder().deserializeMember(de, member).build());
                    default -> throw new IllegalArgumentException("Unexpected member: " + member.memberName());
                }
            }
        }
    }
}
