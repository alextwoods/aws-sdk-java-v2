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
 * The container element for an Object Lock rule.
 */
@SmithyGenerated
public final class ObjectLockRule implements SerializableStruct {

    public static final Schema $SCHEMA = Schemas3.OBJECT_LOCK_RULE;
    private static final Schema $SCHEMA_DEFAULT_RETENTION = $SCHEMA.member("DefaultRetention");

    public static final ShapeId $ID = $SCHEMA.id();

    private final transient DefaultRetention defaultRetention;

    private ObjectLockRule(Builder builder) {
        this.defaultRetention = builder.defaultRetention;
    }

    /**
     * The default Object Lock retention mode and period that you want to apply to new objects placed in the specified
     * bucket. Bucket settings require both a mode and a period. The period can be either <code>Days</code> or
     * <code>Years</code> but you must select one. You cannot specify <code>Days</code> and <code>Years</code> at the
     * same time.
     */
    public DefaultRetention getDefaultRetention() {
        return defaultRetention;
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
        ObjectLockRule that = (ObjectLockRule) other;
        return Objects.equals(this.defaultRetention, that.defaultRetention);
    }

    @Override
    public int hashCode() {
        int $hc = Objects.hashCode(defaultRetention);
        return $hc;
    }

    @Override
    public Schema schema() {
        return $SCHEMA;
    }

    @Override
    public void serializeMembers(ShapeSerializer serializer) {
        if (defaultRetention != null) {
            serializer.writeStruct($SCHEMA_DEFAULT_RETENTION, defaultRetention);
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T getMemberValue(Schema member) {
        return switch (member.memberIndex()) {
            case 0 -> (T) SchemaUtils.validateSameMember($SCHEMA_DEFAULT_RETENTION, member, defaultRetention);
            default -> throw new IllegalArgumentException("Attempted to get non-existent member: " + member.id());
        };
    }

    /**
     * Create a new builder containing all the current property values of this object.
     *
     * <p><strong>Note:</strong> This method performs only a shallow copy of the original properties.
     *
     * @return a builder for {@link ObjectLockRule}.
     */
    public Builder toBuilder() {
        var builder = new Builder();
        builder.defaultRetention(this.defaultRetention);
        return builder;
    }

    /**
     * @return returns a new Builder.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for {@link ObjectLockRule}.
     */
    public static final class Builder implements ShapeBuilder<ObjectLockRule> {
        private DefaultRetention defaultRetention;

        private Builder() {}

        @Override
        public Schema schema() {
            return $SCHEMA;
        }

        /**
         * The default Object Lock retention mode and period that you want to apply to new objects placed in the specified
         * bucket. Bucket settings require both a mode and a period. The period can be either <code>Days</code> or
         * <code>Years</code> but you must select one. You cannot specify <code>Days</code> and <code>Years</code> at the
         * same time.
         *
         * @return this builder.
         */
        public Builder defaultRetention(DefaultRetention defaultRetention) {
            this.defaultRetention = defaultRetention;
            return this;
        }

        @Override
        public ObjectLockRule build() {
            return new ObjectLockRule(this);
        }

        @Override
        @SuppressWarnings("unchecked")
        public void setMemberValue(Schema member, Object value) {
            switch (member.memberIndex()) {
                case 0 -> defaultRetention((DefaultRetention) SchemaUtils.validateSameMember($SCHEMA_DEFAULT_RETENTION, member, value));
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
                    case 0 -> builder.defaultRetention(DefaultRetention.builder().deserializeMember(de, member).build());
                    default -> throw new IllegalArgumentException("Unexpected member: " + member.memberName());
                }
            }
        }
    }
}
