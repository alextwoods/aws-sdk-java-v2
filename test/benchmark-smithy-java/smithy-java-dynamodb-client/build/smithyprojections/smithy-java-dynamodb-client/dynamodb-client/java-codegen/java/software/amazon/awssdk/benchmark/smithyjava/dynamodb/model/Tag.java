package software.amazon.awssdk.benchmark.smithyjava.dynamodb.model;

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
 * Describes a tag. A tag is a key-value pair. You can add up to 50 tags to a single DynamoDB table.
 *
 * <p>Amazon Web Services-assigned tag names and values are automatically assigned the <code>aws:</code> prefix, which
 * the user cannot assign. Amazon Web Services-assigned tag names do not count towards the tag limit of 50.
 * User-assigned tag names have the prefix <code>user:</code> in the Cost Allocation Report. You cannot backdate the
 * application of a tag.
 *
 * <p>For an overview on tagging DynamoDB resources, see <a href="https://docs.aws.amazon.com/amazondynamodb/latest/developerguide/Tagging.html">Tagging for DynamoDB</a> in the <i>Amazon DynamoDB Developer
 * Guide</i>.
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
     * The key of the tag. Tag keys are case sensitive. Each DynamoDB table can only have up to one tag with the same
     * key. If you try to add an existing tag (same key), the existing tag value will be updated to the new value.
     */
    public String getKey() {
        return key;
    }

    /**
     * The value of the tag. Tag values are case-sensitive and can be null.
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
        serializer.writeString($SCHEMA_KEY, key);
        serializer.writeString($SCHEMA_VALUE, value);
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

        private Builder() {}

        @Override
        public Schema schema() {
            return $SCHEMA;
        }

        /**
         * The key of the tag. Tag keys are case sensitive. Each DynamoDB table can only have up to one tag with the same
         * key. If you try to add an existing tag (same key), the existing tag value will be updated to the new value.
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
         * The value of the tag. Tag values are case-sensitive and can be null.
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
