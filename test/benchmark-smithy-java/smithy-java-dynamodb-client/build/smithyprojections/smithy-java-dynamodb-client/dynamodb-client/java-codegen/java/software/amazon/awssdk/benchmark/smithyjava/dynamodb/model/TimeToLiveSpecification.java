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
 * Represents the settings used to enable or disable Time to Live (TTL) for the specified table.
 */
@SmithyGenerated
public final class TimeToLiveSpecification implements SerializableStruct {

    public static final Schema $SCHEMA = Schemas.TIME_TO_LIVE_SPECIFICATION;
    private static final Schema $SCHEMA_ENABLED = $SCHEMA.member("Enabled");
    private static final Schema $SCHEMA_ATTRIBUTE_NAME = $SCHEMA.member("AttributeName");

    public static final ShapeId $ID = $SCHEMA.id();

    private final transient boolean enabled;
    private final transient String attributeName;

    private TimeToLiveSpecification(Builder builder) {
        this.enabled = builder.enabled;
        this.attributeName = builder.attributeName;
    }

    /**
     * Indicates whether TTL is to be enabled (true) or disabled (false) on the table.
     */
    public boolean isEnabled() {
        return enabled;
    }

    /**
     * The name of the TTL attribute used to store the expiration time for items in the table.
     */
    public String getAttributeName() {
        return attributeName;
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
        TimeToLiveSpecification that = (TimeToLiveSpecification) other;
        return this.enabled == that.enabled
               && Objects.equals(this.attributeName, that.attributeName);
    }

    @Override
    public int hashCode() {
        int $hc = Boolean.hashCode(enabled);
        $hc = 31 * $hc + Objects.hashCode(attributeName);
        return $hc;
    }

    @Override
    public Schema schema() {
        return $SCHEMA;
    }

    @Override
    public void serializeMembers(ShapeSerializer serializer) {
        serializer.writeBoolean($SCHEMA_ENABLED, enabled);
        serializer.writeString($SCHEMA_ATTRIBUTE_NAME, attributeName);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T getMemberValue(Schema member) {
        return switch (member.memberIndex()) {
            case 0 -> (T) SchemaUtils.validateSameMember($SCHEMA_ENABLED, member, enabled);
            case 1 -> (T) SchemaUtils.validateSameMember($SCHEMA_ATTRIBUTE_NAME, member, attributeName);
            default -> throw new IllegalArgumentException("Attempted to get non-existent member: " + member.id());
        };
    }

    /**
     * Create a new builder containing all the current property values of this object.
     *
     * <p><strong>Note:</strong> This method performs only a shallow copy of the original properties.
     *
     * @return a builder for {@link TimeToLiveSpecification}.
     */
    public Builder toBuilder() {
        var builder = new Builder();
        builder.enabled(this.enabled);
        builder.attributeName(this.attributeName);
        return builder;
    }

    /**
     * @return returns a new Builder.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for {@link TimeToLiveSpecification}.
     */
    public static final class Builder implements ShapeBuilder<TimeToLiveSpecification> {
        private final PresenceTracker tracker = PresenceTracker.of($SCHEMA);
        private boolean enabled;
        private String attributeName;

        private Builder() {}

        @Override
        public Schema schema() {
            return $SCHEMA;
        }

        /**
         * Indicates whether TTL is to be enabled (true) or disabled (false) on the table.
         *
         * <p><strong>Required</strong>
         * @return this builder.
         */
        public Builder enabled(boolean enabled) {
            this.enabled = enabled;
            tracker.setMember($SCHEMA_ENABLED);
            return this;
        }

        /**
         * The name of the TTL attribute used to store the expiration time for items in the table.
         *
         * <p><strong>Required</strong>
         * @return this builder.
         */
        public Builder attributeName(String attributeName) {
            this.attributeName = Objects.requireNonNull(attributeName, "attributeName cannot be null");
            tracker.setMember($SCHEMA_ATTRIBUTE_NAME);
            return this;
        }

        @Override
        public TimeToLiveSpecification build() {
            tracker.validate();
            return new TimeToLiveSpecification(this);
        }

        @Override
        @SuppressWarnings("unchecked")
        public void setMemberValue(Schema member, Object value) {
            switch (member.memberIndex()) {
                case 0 -> enabled((boolean) SchemaUtils.validateSameMember($SCHEMA_ENABLED, member, value));
                case 1 -> attributeName((String) SchemaUtils.validateSameMember($SCHEMA_ATTRIBUTE_NAME, member, value));
                default -> ShapeBuilder.super.setMemberValue(member, value);
            }
        }

        @Override
        public ShapeBuilder<TimeToLiveSpecification> errorCorrection() {
            if (tracker.allSet()) {
                return this;
            }
            if (!tracker.checkMember($SCHEMA_ENABLED)) {
                tracker.setMember($SCHEMA_ENABLED);
            }
            if (!tracker.checkMember($SCHEMA_ATTRIBUTE_NAME)) {
                attributeName("");
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
                    case 0 -> builder.enabled(de.readBoolean(member));
                    case 1 -> builder.attributeName(de.readString(member));
                    default -> throw new IllegalArgumentException("Unexpected member: " + member.memberName());
                }
            }
        }
    }
}
