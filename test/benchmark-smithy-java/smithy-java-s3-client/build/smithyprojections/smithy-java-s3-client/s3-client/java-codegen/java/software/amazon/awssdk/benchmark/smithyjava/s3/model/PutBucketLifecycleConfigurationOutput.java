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

@SmithyGenerated
public final class PutBucketLifecycleConfigurationOutput implements SerializableStruct {

    public static final Schema $SCHEMA = Schemas5.PUT_BUCKET_LIFECYCLE_CONFIGURATION_OUTPUT;
    private static final Schema $SCHEMA_TRANSITION_DEFAULT_MINIMUM_OBJECT_SIZE = $SCHEMA.member("TransitionDefaultMinimumObjectSize");

    public static final ShapeId $ID = $SCHEMA.id();

    private final transient TransitionDefaultMinimumObjectSize transitionDefaultMinimumObjectSize;

    private PutBucketLifecycleConfigurationOutput(Builder builder) {
        this.transitionDefaultMinimumObjectSize = builder.transitionDefaultMinimumObjectSize;
    }

    /**
     * Indicates which default minimum object size behavior is applied to the lifecycle configuration.
     *
     * <p>This parameter applies to general purpose buckets only. It is not supported for directory bucket lifecycle
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
        PutBucketLifecycleConfigurationOutput that = (PutBucketLifecycleConfigurationOutput) other;
        return Objects.equals(this.transitionDefaultMinimumObjectSize, that.transitionDefaultMinimumObjectSize);
    }

    @Override
    public int hashCode() {
        int $hc = Objects.hashCode(transitionDefaultMinimumObjectSize);
        return $hc;
    }

    @Override
    public Schema schema() {
        return $SCHEMA;
    }

    @Override
    public void serializeMembers(ShapeSerializer serializer) {
        if (transitionDefaultMinimumObjectSize != null) {
            serializer.writeString($SCHEMA_TRANSITION_DEFAULT_MINIMUM_OBJECT_SIZE, transitionDefaultMinimumObjectSize.getValue());
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T getMemberValue(Schema member) {
        return switch (member.memberIndex()) {
            case 0 -> (T) SchemaUtils.validateSameMember($SCHEMA_TRANSITION_DEFAULT_MINIMUM_OBJECT_SIZE, member, transitionDefaultMinimumObjectSize);
            default -> throw new IllegalArgumentException("Attempted to get non-existent member: " + member.id());
        };
    }

    /**
     * Create a new builder containing all the current property values of this object.
     *
     * <p><strong>Note:</strong> This method performs only a shallow copy of the original properties.
     *
     * @return a builder for {@link PutBucketLifecycleConfigurationOutput}.
     */
    public Builder toBuilder() {
        var builder = new Builder();
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
     * Builder for {@link PutBucketLifecycleConfigurationOutput}.
     */
    public static final class Builder implements ShapeBuilder<PutBucketLifecycleConfigurationOutput> {
        private TransitionDefaultMinimumObjectSize transitionDefaultMinimumObjectSize;

        private Builder() {}

        @Override
        public Schema schema() {
            return $SCHEMA;
        }

        /**
         * Indicates which default minimum object size behavior is applied to the lifecycle configuration.
         *
         * <p>This parameter applies to general purpose buckets only. It is not supported for directory bucket lifecycle
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
        public PutBucketLifecycleConfigurationOutput build() {
            return new PutBucketLifecycleConfigurationOutput(this);
        }

        @Override
        @SuppressWarnings("unchecked")
        public void setMemberValue(Schema member, Object value) {
            switch (member.memberIndex()) {
                case 0 -> transitionDefaultMinimumObjectSize((TransitionDefaultMinimumObjectSize) SchemaUtils.validateSameMember($SCHEMA_TRANSITION_DEFAULT_MINIMUM_OBJECT_SIZE, member, value));
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
                    case 0 -> builder.transitionDefaultMinimumObjectSize(TransitionDefaultMinimumObjectSize.builder().deserializeMember(de, member).build());
                    default -> throw new IllegalArgumentException("Unexpected member: " + member.memberName());
                }
            }
        }
    }
}
