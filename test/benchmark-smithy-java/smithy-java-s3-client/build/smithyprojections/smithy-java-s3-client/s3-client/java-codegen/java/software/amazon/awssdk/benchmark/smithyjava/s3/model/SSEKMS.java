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
 * Specifies the use of SSE-KMS to encrypt delivered inventory reports.
 */
@SmithyGenerated
public final class SSEKMS implements SerializableStruct {

    public static final Schema $SCHEMA = Schemas2.COM_AMAZONAWS_S3_SSEKMS;
    private static final Schema $SCHEMA_KEY_ID = $SCHEMA.member("KeyId");

    public static final ShapeId $ID = $SCHEMA.id();

    private final transient String keyId;

    private SSEKMS(Builder builder) {
        this.keyId = builder.keyId;
    }

    /**
     * Specifies the ID of the Key Management Service (KMS) symmetric encryption customer managed key to use for
     * encrypting inventory reports.
     */
    public String getKeyId() {
        return keyId;
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
        SSEKMS that = (SSEKMS) other;
        return Objects.equals(this.keyId, that.keyId);
    }

    @Override
    public int hashCode() {
        int $hc = Objects.hashCode(keyId);
        return $hc;
    }

    @Override
    public Schema schema() {
        return $SCHEMA;
    }

    @Override
    public void serializeMembers(ShapeSerializer serializer) {
        serializer.writeString($SCHEMA_KEY_ID, keyId);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T getMemberValue(Schema member) {
        return switch (member.memberIndex()) {
            case 0 -> (T) SchemaUtils.validateSameMember($SCHEMA_KEY_ID, member, keyId);
            default -> throw new IllegalArgumentException("Attempted to get non-existent member: " + member.id());
        };
    }

    /**
     * Create a new builder containing all the current property values of this object.
     *
     * <p><strong>Note:</strong> This method performs only a shallow copy of the original properties.
     *
     * @return a builder for {@link SSEKMS}.
     */
    public Builder toBuilder() {
        var builder = new Builder();
        builder.keyId(this.keyId);
        return builder;
    }

    /**
     * @return returns a new Builder.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for {@link SSEKMS}.
     */
    public static final class Builder implements ShapeBuilder<SSEKMS> {
        private final PresenceTracker tracker = PresenceTracker.of($SCHEMA);
        private String keyId;

        private Builder() {}

        @Override
        public Schema schema() {
            return $SCHEMA;
        }

        /**
         * Specifies the ID of the Key Management Service (KMS) symmetric encryption customer managed key to use for
         * encrypting inventory reports.
         *
         * <p><strong>Required</strong>
         * @return this builder.
         */
        public Builder keyId(String keyId) {
            this.keyId = Objects.requireNonNull(keyId, "keyId cannot be null");
            tracker.setMember($SCHEMA_KEY_ID);
            return this;
        }

        @Override
        public SSEKMS build() {
            tracker.validate();
            return new SSEKMS(this);
        }

        @Override
        @SuppressWarnings("unchecked")
        public void setMemberValue(Schema member, Object value) {
            switch (member.memberIndex()) {
                case 0 -> keyId((String) SchemaUtils.validateSameMember($SCHEMA_KEY_ID, member, value));
                default -> ShapeBuilder.super.setMemberValue(member, value);
            }
        }

        @Override
        public ShapeBuilder<SSEKMS> errorCorrection() {
            if (tracker.allSet()) {
                return this;
            }
            if (!tracker.checkMember($SCHEMA_KEY_ID)) {
                keyId("");
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
                    case 0 -> builder.keyId(de.readString(member));
                    default -> throw new IllegalArgumentException("Unexpected member: " + member.memberName());
                }
            }
        }
    }
}
