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
 * A container for filter information for the selection of S3 objects encrypted with Amazon Web Services KMS.
 */
@SmithyGenerated
public final class SseKmsEncryptedObjects implements SerializableStruct {

    public static final Schema $SCHEMA = Schemas3.SSE_KMS_ENCRYPTED_OBJECTS;
    private static final Schema $SCHEMA_STATUS = $SCHEMA.member("Status");

    public static final ShapeId $ID = $SCHEMA.id();

    private final transient SseKmsEncryptedObjectsStatus status;

    private SseKmsEncryptedObjects(Builder builder) {
        this.status = builder.status;
    }

    /**
     * Specifies whether Amazon S3 replicates objects created with server-side encryption using an Amazon Web Services
     * KMS key stored in Amazon Web Services Key Management Service.
     */
    public SseKmsEncryptedObjectsStatus getStatus() {
        return status;
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
        SseKmsEncryptedObjects that = (SseKmsEncryptedObjects) other;
        return Objects.equals(this.status, that.status);
    }

    @Override
    public int hashCode() {
        int $hc = Objects.hashCode(status);
        return $hc;
    }

    @Override
    public Schema schema() {
        return $SCHEMA;
    }

    @Override
    public void serializeMembers(ShapeSerializer serializer) {
        serializer.writeString($SCHEMA_STATUS, status.getValue());
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T getMemberValue(Schema member) {
        return switch (member.memberIndex()) {
            case 0 -> (T) SchemaUtils.validateSameMember($SCHEMA_STATUS, member, status);
            default -> throw new IllegalArgumentException("Attempted to get non-existent member: " + member.id());
        };
    }

    /**
     * Create a new builder containing all the current property values of this object.
     *
     * <p><strong>Note:</strong> This method performs only a shallow copy of the original properties.
     *
     * @return a builder for {@link SseKmsEncryptedObjects}.
     */
    public Builder toBuilder() {
        var builder = new Builder();
        builder.status(this.status);
        return builder;
    }

    /**
     * @return returns a new Builder.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for {@link SseKmsEncryptedObjects}.
     */
    public static final class Builder implements ShapeBuilder<SseKmsEncryptedObjects> {
        private final PresenceTracker tracker = PresenceTracker.of($SCHEMA);
        private SseKmsEncryptedObjectsStatus status;

        private Builder() {}

        @Override
        public Schema schema() {
            return $SCHEMA;
        }

        /**
         * Specifies whether Amazon S3 replicates objects created with server-side encryption using an Amazon Web Services
         * KMS key stored in Amazon Web Services Key Management Service.
         *
         * <p><strong>Required</strong>
         * @return this builder.
         */
        public Builder status(SseKmsEncryptedObjectsStatus status) {
            this.status = Objects.requireNonNull(status, "status cannot be null");
            tracker.setMember($SCHEMA_STATUS);
            return this;
        }

        @Override
        public SseKmsEncryptedObjects build() {
            tracker.validate();
            return new SseKmsEncryptedObjects(this);
        }

        @Override
        @SuppressWarnings("unchecked")
        public void setMemberValue(Schema member, Object value) {
            switch (member.memberIndex()) {
                case 0 -> status((SseKmsEncryptedObjectsStatus) SchemaUtils.validateSameMember($SCHEMA_STATUS, member, value));
                default -> ShapeBuilder.super.setMemberValue(member, value);
            }
        }

        @Override
        public ShapeBuilder<SseKmsEncryptedObjects> errorCorrection() {
            if (tracker.allSet()) {
                return this;
            }
            if (!tracker.checkMember($SCHEMA_STATUS)) {
                status(SseKmsEncryptedObjectsStatus.unknown(""));
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
                    case 0 -> builder.status(SseKmsEncryptedObjectsStatus.builder().deserializeMember(de, member).build());
                    default -> throw new IllegalArgumentException("Unexpected member: " + member.memberName());
                }
            }
        }
    }
}
