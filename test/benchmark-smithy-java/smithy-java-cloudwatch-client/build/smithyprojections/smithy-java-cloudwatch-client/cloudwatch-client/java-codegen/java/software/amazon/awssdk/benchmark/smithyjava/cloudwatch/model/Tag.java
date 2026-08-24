package software.amazon.awssdk.benchmark.smithyjava.cloudwatch.model;

import java.util.Objects;
import software.amazon.smithy.java.core.schema.PresenceTracker;
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
 * A key-value pair associated with a CloudWatch resource.
 */
@SmithyGenerated
public final class Tag implements SerializableStruct {

    public static final Schema $SCHEMA = Schemas.TAG;
    private static final Schema $SCHEMA_KEY = $SCHEMA.member("Key");
    private static final Schema $SCHEMA_VALUE = $SCHEMA.member("Value");

    public static final ShapeId $ID = $SCHEMA.id();

    private final transient String key;
    private final transient String value;

    private Tag(Builder builder) {
        this.key = builder.key;
        this.value = builder.value;
    }

    /**
     * A string that you can use to assign a value. The combination of tag keys and values can help you organize and
     * categorize your resources.
     */
    public String getKey() {
        return key;
    }

    /**
     * The value for the specified tag key.
     */
    public String getValue() {
        return value;
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
        Tag that = (Tag) other;
        return Objects.equals(this.key, that.key)
               && Objects.equals(this.value, that.value);
    }

    @Override
    public int hashCode() {
        int $hc = Objects.hashCode(key);
        $hc = 31 * $hc + Objects.hashCode(value);
        return $hc;
    }

    @Override
    public Schema schema() {
        return $SCHEMA;
    }

    @Override
    public void serializeMembers(ShapeSerializer serializer) {
        if (key != null) {
            serializer.writeString($SCHEMA_KEY, key);
        }
        if (value != null) {
            serializer.writeString($SCHEMA_VALUE, value);
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T getMemberValue(Schema member) {
        return switch (member.memberIndex()) {
            case 0 -> (T) SchemaUtils.validateSameMember($SCHEMA_KEY, member, key);
            case 1 -> (T) SchemaUtils.validateSameMember($SCHEMA_VALUE, member, value);
            default -> throw new IllegalArgumentException("Attempted to get non-existent member: " + member.id());
        };
    }

    /**
     * Create a new builder containing all the current property values of this object.
     *
     * <p><strong>Note:</strong> This method performs only a shallow copy of the original properties.
     *
     * @return a builder for {@link Tag}.
     */
    public Builder toBuilder() {
        var builder = new Builder();
        builder.key(this.key);
        builder.value(this.value);
        return builder;
    }

    /**
     * @return returns a new Builder.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for {@link Tag}.
     */
    public static final class Builder implements ShapeBuilder<Tag> {
        private final PresenceTracker tracker = PresenceTracker.of($SCHEMA);
        private String key;
        private String value;

        private Builder() {
            // Tell the tracker to assume clientOptional members are present.
            tracker.setMember($SCHEMA_KEY);
            tracker.setMember($SCHEMA_VALUE);
        }

        @Override
        public Schema schema() {
            return $SCHEMA;
        }

        /**
         * A string that you can use to assign a value. The combination of tag keys and values can help you organize and
         * categorize your resources.
         *
         * <p><strong>Required</strong>
         * @return this builder.
         */
        public Builder key(String key) {
            this.key = Objects.requireNonNull(key, "key cannot be null");
            tracker.setMember($SCHEMA_KEY);
            return this;
        }

        /**
         * The value for the specified tag key.
         *
         * <p><strong>Required</strong>
         * @return this builder.
         */
        public Builder value(String value) {
            this.value = Objects.requireNonNull(value, "value cannot be null");
            tracker.setMember($SCHEMA_VALUE);
            return this;
        }

        @Override
        public Tag build() {
            tracker.validate();
            return new Tag(this);
        }

        @Override
        @SuppressWarnings("unchecked")
        public void setMemberValue(Schema member, Object value) {
            switch (member.memberIndex()) {
                case 0 -> key((String) SchemaUtils.validateSameMember($SCHEMA_KEY, member, value));
                case 1 -> value((String) SchemaUtils.validateSameMember($SCHEMA_VALUE, member, value));
                default -> ShapeBuilder.super.setMemberValue(member, value);
            }
        }

        @Override
        public ShapeBuilder<Tag> errorCorrection() {
            if (tracker.allSet()) {
                return this;
            }
            if (!tracker.checkMember($SCHEMA_KEY)) {
                key("");
            }
            if (!tracker.checkMember($SCHEMA_VALUE)) {
                value("");
            }
            return this;
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
                    case 0 -> builder.key(de.readString(member));
                    case 1 -> builder.value(de.readString(member));
                    default -> throw new IllegalArgumentException("Unexpected member: " + member.memberName());
                }
            }
        }
    }
}
