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
 * A container for information about access control for replicas.
 */
@SmithyGenerated
public final class AccessControlTranslation implements SerializableStruct {

    public static final Schema $SCHEMA = Schemas.ACCESS_CONTROL_TRANSLATION;
    private static final Schema $SCHEMA_OWNER = $SCHEMA.member("Owner");

    public static final ShapeId $ID = $SCHEMA.id();

    private final transient OwnerOverride owner;

    private AccessControlTranslation(Builder builder) {
        this.owner = builder.owner;
    }

    /**
     * Specifies the replica ownership. For default and valid values, see <a href="https://docs.aws.amazon.com/AmazonS3/latest/API/RESTBucketPUTreplication.html">PUT bucket replication</a> in the <i>Amazon
     * S3 API Reference</i>.
     */
    public OwnerOverride getOwner() {
        return owner;
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
        AccessControlTranslation that = (AccessControlTranslation) other;
        return Objects.equals(this.owner, that.owner);
    }

    @Override
    public int hashCode() {
        int $hc = Objects.hashCode(owner);
        return $hc;
    }

    @Override
    public Schema schema() {
        return $SCHEMA;
    }

    @Override
    public void serializeMembers(ShapeSerializer serializer) {
        serializer.writeString($SCHEMA_OWNER, owner.getValue());
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T getMemberValue(Schema member) {
        return switch (member.memberIndex()) {
            case 0 -> (T) SchemaUtils.validateSameMember($SCHEMA_OWNER, member, owner);
            default -> throw new IllegalArgumentException("Attempted to get non-existent member: " + member.id());
        };
    }

    /**
     * Create a new builder containing all the current property values of this object.
     *
     * <p><strong>Note:</strong> This method performs only a shallow copy of the original properties.
     *
     * @return a builder for {@link AccessControlTranslation}.
     */
    public Builder toBuilder() {
        var builder = new Builder();
        builder.owner(this.owner);
        return builder;
    }

    /**
     * @return returns a new Builder.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for {@link AccessControlTranslation}.
     */
    public static final class Builder implements ShapeBuilder<AccessControlTranslation> {
        private final PresenceTracker tracker = PresenceTracker.of($SCHEMA);
        private OwnerOverride owner;

        private Builder() {}

        @Override
        public Schema schema() {
            return $SCHEMA;
        }

        /**
         * Specifies the replica ownership. For default and valid values, see <a href="https://docs.aws.amazon.com/AmazonS3/latest/API/RESTBucketPUTreplication.html">PUT bucket replication</a> in the <i>Amazon
         * S3 API Reference</i>.
         *
         * <p><strong>Required</strong>
         * @return this builder.
         */
        public Builder owner(OwnerOverride owner) {
            this.owner = Objects.requireNonNull(owner, "owner cannot be null");
            tracker.setMember($SCHEMA_OWNER);
            return this;
        }

        @Override
        public AccessControlTranslation build() {
            tracker.validate();
            return new AccessControlTranslation(this);
        }

        @Override
        @SuppressWarnings("unchecked")
        public void setMemberValue(Schema member, Object value) {
            switch (member.memberIndex()) {
                case 0 -> owner((OwnerOverride) SchemaUtils.validateSameMember($SCHEMA_OWNER, member, value));
                default -> ShapeBuilder.super.setMemberValue(member, value);
            }
        }

        @Override
        public ShapeBuilder<AccessControlTranslation> errorCorrection() {
            if (tracker.allSet()) {
                return this;
            }
            if (!tracker.checkMember($SCHEMA_OWNER)) {
                owner(OwnerOverride.unknown(""));
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
                    case 0 -> builder.owner(OwnerOverride.builder().deserializeMember(de, member).build());
                    default -> throw new IllegalArgumentException("Unexpected member: " + member.memberName());
                }
            }
        }
    }
}
