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
 * The container element for Object Lock configuration parameters.
 */
@SmithyGenerated
public final class ObjectLockConfiguration implements SerializableStruct {

    public static final Schema $SCHEMA = Schemas3.OBJECT_LOCK_CONFIGURATION;
    private static final Schema $SCHEMA_OBJECT_LOCK_ENABLED = $SCHEMA.member("ObjectLockEnabled");
    private static final Schema $SCHEMA_RULE = $SCHEMA.member("Rule");

    public static final ShapeId $ID = $SCHEMA.id();

    private final transient ObjectLockEnabled objectLockEnabled;
    private final transient ObjectLockRule rule;

    private ObjectLockConfiguration(Builder builder) {
        this.objectLockEnabled = builder.objectLockEnabled;
        this.rule = builder.rule;
    }

    /**
     * Indicates whether this bucket has an Object Lock configuration enabled. Enable <code>ObjectLockEnabled</code>
     * when you apply <code>ObjectLockConfiguration</code> to a bucket.
     */
    public ObjectLockEnabled getObjectLockEnabled() {
        return objectLockEnabled;
    }

    /**
     * Specifies the Object Lock rule for the specified object. Enable the this rule when you apply
     * <code>ObjectLockConfiguration</code> to a bucket. Bucket settings require both a mode and a period. The period
     * can be either <code>Days</code> or <code>Years</code> but you must select one. You cannot specify
     * <code>Days</code> and <code>Years</code> at the same time.
     */
    public ObjectLockRule getRule() {
        return rule;
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
        ObjectLockConfiguration that = (ObjectLockConfiguration) other;
        return Objects.equals(this.objectLockEnabled, that.objectLockEnabled)
               && Objects.equals(this.rule, that.rule);
    }

    @Override
    public int hashCode() {
        int $hc = Objects.hashCode(objectLockEnabled);
        $hc = 31 * $hc + Objects.hashCode(rule);
        return $hc;
    }

    @Override
    public Schema schema() {
        return $SCHEMA;
    }

    @Override
    public void serializeMembers(ShapeSerializer serializer) {
        if (objectLockEnabled != null) {
            serializer.writeString($SCHEMA_OBJECT_LOCK_ENABLED, objectLockEnabled.getValue());
        }
        if (rule != null) {
            serializer.writeStruct($SCHEMA_RULE, rule);
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T getMemberValue(Schema member) {
        return switch (member.memberIndex()) {
            case 0 -> (T) SchemaUtils.validateSameMember($SCHEMA_OBJECT_LOCK_ENABLED, member, objectLockEnabled);
            case 1 -> (T) SchemaUtils.validateSameMember($SCHEMA_RULE, member, rule);
            default -> throw new IllegalArgumentException("Attempted to get non-existent member: " + member.id());
        };
    }

    /**
     * Create a new builder containing all the current property values of this object.
     *
     * <p><strong>Note:</strong> This method performs only a shallow copy of the original properties.
     *
     * @return a builder for {@link ObjectLockConfiguration}.
     */
    public Builder toBuilder() {
        var builder = new Builder();
        builder.objectLockEnabled(this.objectLockEnabled);
        builder.rule(this.rule);
        return builder;
    }

    /**
     * @return returns a new Builder.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for {@link ObjectLockConfiguration}.
     */
    public static final class Builder implements ShapeBuilder<ObjectLockConfiguration> {
        private ObjectLockEnabled objectLockEnabled;
        private ObjectLockRule rule;

        private Builder() {}

        @Override
        public Schema schema() {
            return $SCHEMA;
        }

        /**
         * Indicates whether this bucket has an Object Lock configuration enabled. Enable <code>ObjectLockEnabled</code>
         * when you apply <code>ObjectLockConfiguration</code> to a bucket.
         *
         * @return this builder.
         */
        public Builder objectLockEnabled(ObjectLockEnabled objectLockEnabled) {
            this.objectLockEnabled = objectLockEnabled;
            return this;
        }

        /**
         * Specifies the Object Lock rule for the specified object. Enable the this rule when you apply
         * <code>ObjectLockConfiguration</code> to a bucket. Bucket settings require both a mode and a period. The period
         * can be either <code>Days</code> or <code>Years</code> but you must select one. You cannot specify
         * <code>Days</code> and <code>Years</code> at the same time.
         *
         * @return this builder.
         */
        public Builder rule(ObjectLockRule rule) {
            this.rule = rule;
            return this;
        }

        @Override
        public ObjectLockConfiguration build() {
            return new ObjectLockConfiguration(this);
        }

        @Override
        @SuppressWarnings("unchecked")
        public void setMemberValue(Schema member, Object value) {
            switch (member.memberIndex()) {
                case 0 -> objectLockEnabled((ObjectLockEnabled) SchemaUtils.validateSameMember($SCHEMA_OBJECT_LOCK_ENABLED, member, value));
                case 1 -> rule((ObjectLockRule) SchemaUtils.validateSameMember($SCHEMA_RULE, member, value));
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
                    case 0 -> builder.objectLockEnabled(ObjectLockEnabled.builder().deserializeMember(de, member).build());
                    case 1 -> builder.rule(ObjectLockRule.builder().deserializeMember(de, member).build());
                    default -> throw new IllegalArgumentException("Unexpected member: " + member.memberName());
                }
            }
        }
    }
}
