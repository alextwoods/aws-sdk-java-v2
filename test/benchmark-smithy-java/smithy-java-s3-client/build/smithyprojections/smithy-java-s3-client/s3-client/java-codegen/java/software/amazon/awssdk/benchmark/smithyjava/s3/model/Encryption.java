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
 * Contains the type of server-side encryption used.
 */
@SmithyGenerated
public final class Encryption implements SerializableStruct {

    public static final Schema $SCHEMA = Schemas6.ENCRYPTION;
    private static final Schema $SCHEMA_ENCRYPTION_TYPE = $SCHEMA.member("EncryptionType");
    private static final Schema $SCHEMA_KMS_KEY_ID = $SCHEMA.member("KMSKeyId");
    private static final Schema $SCHEMA_KMS_CONTEXT = $SCHEMA.member("KMSContext");

    public static final ShapeId $ID = $SCHEMA.id();

    private final transient ServerSideEncryption encryptionType;
    private final transient String kmsKeyId;
    private final transient String kmsContext;

    private Encryption(Builder builder) {
        this.encryptionType = builder.encryptionType;
        this.kmsKeyId = builder.kmsKeyId;
        this.kmsContext = builder.kmsContext;
    }

    /**
     * The server-side encryption algorithm used when storing job results in Amazon S3 (for example, AES256,
     * <code>aws:kms</code>).
     */
    public ServerSideEncryption getEncryptionType() {
        return encryptionType;
    }

    /**
     * If the encryption type is <code>aws:kms</code>, this optional value specifies the ID of the symmetric encryption
     * customer managed key to use for encryption of job results. Amazon S3 only supports symmetric encryption KMS keys.
     * For more information, see <a href="https://docs.aws.amazon.com/kms/latest/developerguide/symmetric-asymmetric.html">Asymmetric keys in KMS</a> in the <i>Amazon Web Services Key Management Service
     * Developer Guide</i>.
     */
    public String getKmsKeyId() {
        return kmsKeyId;
    }

    /**
     * If the encryption type is <code>aws:kms</code>, this optional value can be used to specify the encryption context
     * for the restore results.
     */
    public String getKmsContext() {
        return kmsContext;
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
        Encryption that = (Encryption) other;
        return Objects.equals(this.kmsKeyId, that.kmsKeyId)
               && Objects.equals(this.kmsContext, that.kmsContext)
               && Objects.equals(this.encryptionType, that.encryptionType);
    }

    @Override
    public int hashCode() {
        int $hc = Objects.hashCode(encryptionType);
        $hc = 31 * $hc + Objects.hashCode(kmsKeyId);
        $hc = 31 * $hc + Objects.hashCode(kmsContext);
        return $hc;
    }

    @Override
    public Schema schema() {
        return $SCHEMA;
    }

    @Override
    public void serializeMembers(ShapeSerializer serializer) {
        serializer.writeString($SCHEMA_ENCRYPTION_TYPE, encryptionType.getValue());
        if (kmsKeyId != null) {
            serializer.writeString($SCHEMA_KMS_KEY_ID, kmsKeyId);
        }
        if (kmsContext != null) {
            serializer.writeString($SCHEMA_KMS_CONTEXT, kmsContext);
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T getMemberValue(Schema member) {
        return switch (member.memberIndex()) {
            case 0 -> (T) SchemaUtils.validateSameMember($SCHEMA_ENCRYPTION_TYPE, member, encryptionType);
            case 1 -> (T) SchemaUtils.validateSameMember($SCHEMA_KMS_KEY_ID, member, kmsKeyId);
            case 2 -> (T) SchemaUtils.validateSameMember($SCHEMA_KMS_CONTEXT, member, kmsContext);
            default -> throw new IllegalArgumentException("Attempted to get non-existent member: " + member.id());
        };
    }

    /**
     * Create a new builder containing all the current property values of this object.
     *
     * <p><strong>Note:</strong> This method performs only a shallow copy of the original properties.
     *
     * @return a builder for {@link Encryption}.
     */
    public Builder toBuilder() {
        var builder = new Builder();
        builder.encryptionType(this.encryptionType);
        builder.kmsKeyId(this.kmsKeyId);
        builder.kmsContext(this.kmsContext);
        return builder;
    }

    /**
     * @return returns a new Builder.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for {@link Encryption}.
     */
    public static final class Builder implements ShapeBuilder<Encryption> {
        private final PresenceTracker tracker = PresenceTracker.of($SCHEMA);
        private ServerSideEncryption encryptionType;
        private String kmsKeyId;
        private String kmsContext;

        private Builder() {}

        @Override
        public Schema schema() {
            return $SCHEMA;
        }

        /**
         * The server-side encryption algorithm used when storing job results in Amazon S3 (for example, AES256,
         * <code>aws:kms</code>).
         *
         * <p><strong>Required</strong>
         * @return this builder.
         */
        public Builder encryptionType(ServerSideEncryption encryptionType) {
            this.encryptionType = Objects.requireNonNull(encryptionType, "encryptionType cannot be null");
            tracker.setMember($SCHEMA_ENCRYPTION_TYPE);
            return this;
        }

        /**
         * If the encryption type is <code>aws:kms</code>, this optional value specifies the ID of the symmetric encryption
         * customer managed key to use for encryption of job results. Amazon S3 only supports symmetric encryption KMS keys.
         * For more information, see <a href="https://docs.aws.amazon.com/kms/latest/developerguide/symmetric-asymmetric.html">Asymmetric keys in KMS</a> in the <i>Amazon Web Services Key Management Service
         * Developer Guide</i>.
         *
         * @return this builder.
         */
        public Builder kmsKeyId(String kmsKeyId) {
            this.kmsKeyId = kmsKeyId;
            return this;
        }

        /**
         * If the encryption type is <code>aws:kms</code>, this optional value can be used to specify the encryption context
         * for the restore results.
         *
         * @return this builder.
         */
        public Builder kmsContext(String kmsContext) {
            this.kmsContext = kmsContext;
            return this;
        }

        @Override
        public Encryption build() {
            tracker.validate();
            return new Encryption(this);
        }

        @Override
        @SuppressWarnings("unchecked")
        public void setMemberValue(Schema member, Object value) {
            switch (member.memberIndex()) {
                case 0 -> encryptionType((ServerSideEncryption) SchemaUtils.validateSameMember($SCHEMA_ENCRYPTION_TYPE, member, value));
                case 1 -> kmsKeyId((String) SchemaUtils.validateSameMember($SCHEMA_KMS_KEY_ID, member, value));
                case 2 -> kmsContext((String) SchemaUtils.validateSameMember($SCHEMA_KMS_CONTEXT, member, value));
                default -> ShapeBuilder.super.setMemberValue(member, value);
            }
        }

        @Override
        public ShapeBuilder<Encryption> errorCorrection() {
            if (tracker.allSet()) {
                return this;
            }
            if (!tracker.checkMember($SCHEMA_ENCRYPTION_TYPE)) {
                encryptionType(ServerSideEncryption.unknown(""));
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
                    case 0 -> builder.encryptionType(ServerSideEncryption.builder().deserializeMember(de, member).build());
                    case 1 -> builder.kmsKeyId(de.readString(member));
                    case 2 -> builder.kmsContext(de.readString(member));
                    default -> throw new IllegalArgumentException("Unexpected member: " + member.memberName());
                }
            }
        }
    }
}
