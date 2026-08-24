package software.amazon.awssdk.benchmark.smithyjava.s3.model;

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
 * The container element for an ownership control rule.
 */
@SmithyGenerated
public final class OwnershipControlsRule implements SerializableStruct {

    public static final Schema $SCHEMA = Schemas2.OWNERSHIP_CONTROLS_RULE;
    private static final Schema $SCHEMA_OBJECT_OWNERSHIP = $SCHEMA.member("ObjectOwnership");

    public static final ShapeId $ID = $SCHEMA.id();

    private final transient ObjectOwnership objectOwnership;

    private OwnershipControlsRule(Builder builder) {
        this.objectOwnership = builder.objectOwnership;
    }

    public ObjectOwnership getObjectOwnership() {
        return objectOwnership;
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
        OwnershipControlsRule that = (OwnershipControlsRule) other;
        return Objects.equals(this.objectOwnership, that.objectOwnership);
    }

    @Override
    public int hashCode() {
        int $hc = Objects.hashCode(objectOwnership);
        return $hc;
    }

    @Override
    public Schema schema() {
        return $SCHEMA;
    }

    @Override
    public void serializeMembers(ShapeSerializer serializer) {
        serializer.writeString($SCHEMA_OBJECT_OWNERSHIP, objectOwnership.getValue());
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T getMemberValue(Schema member) {
        return switch (member.memberIndex()) {
            case 0 -> (T) SchemaUtils.validateSameMember($SCHEMA_OBJECT_OWNERSHIP, member, objectOwnership);
            default -> throw new IllegalArgumentException("Attempted to get non-existent member: " + member.id());
        };
    }

    /**
     * Create a new builder containing all the current property values of this object.
     *
     * <p><strong>Note:</strong> This method performs only a shallow copy of the original properties.
     *
     * @return a builder for {@link OwnershipControlsRule}.
     */
    public Builder toBuilder() {
        var builder = new Builder();
        builder.objectOwnership(this.objectOwnership);
        return builder;
    }

    /**
     * @return returns a new Builder.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for {@link OwnershipControlsRule}.
     */
    public static final class Builder implements ShapeBuilder<OwnershipControlsRule> {
        private final PresenceTracker tracker = PresenceTracker.of($SCHEMA);
        private ObjectOwnership objectOwnership;

        private Builder() {}

        @Override
        public Schema schema() {
            return $SCHEMA;
        }

        /**
         * <p><strong>Required</strong>
         * @return this builder.
         */
        public Builder objectOwnership(ObjectOwnership objectOwnership) {
            this.objectOwnership = Objects.requireNonNull(objectOwnership, "objectOwnership cannot be null");
            tracker.setMember($SCHEMA_OBJECT_OWNERSHIP);
            return this;
        }

        @Override
        public OwnershipControlsRule build() {
            tracker.validate();
            return new OwnershipControlsRule(this);
        }

        @Override
        @SuppressWarnings("unchecked")
        public void setMemberValue(Schema member, Object value) {
            switch (member.memberIndex()) {
                case 0 -> objectOwnership((ObjectOwnership) SchemaUtils.validateSameMember($SCHEMA_OBJECT_OWNERSHIP, member, value));
                default -> ShapeBuilder.super.setMemberValue(member, value);
            }
        }

        @Override
        public ShapeBuilder<OwnershipControlsRule> errorCorrection() {
            if (tracker.allSet()) {
                return this;
            }
            if (!tracker.checkMember($SCHEMA_OBJECT_OWNERSHIP)) {
                objectOwnership(ObjectOwnership.unknown(""));
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
                    case 0 -> builder.objectOwnership(ObjectOwnership.builder().deserializeMember(de, member).build());
                    default -> throw new IllegalArgumentException("Unexpected member: " + member.memberName());
                }
            }
        }
    }
}
