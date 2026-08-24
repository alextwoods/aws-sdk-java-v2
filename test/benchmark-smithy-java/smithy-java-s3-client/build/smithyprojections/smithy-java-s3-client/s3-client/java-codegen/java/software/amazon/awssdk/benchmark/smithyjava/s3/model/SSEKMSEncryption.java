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
 * If <code>SSEKMS</code> is specified for <code>ObjectEncryption</code>, this data type specifies the Amazon Web
 * Services KMS key Amazon Resource Name (ARN) to use and whether to use an S3 Bucket Key for server-side encryption
 * using Key Management Service (KMS) keys (SSE-KMS).
 */
@SmithyGenerated
public final class SSEKMSEncryption implements SerializableStruct {

    public static final Schema $SCHEMA = Schemas7.SSEKMS_ENCRYPTION;
    private static final Schema $SCHEMA_KMS_KEY_ARN = $SCHEMA.member("KMSKeyArn");
    private static final Schema $SCHEMA_BUCKET_KEY_ENABLED = $SCHEMA.member("BucketKeyEnabled");

    public static final ShapeId $ID = $SCHEMA.id();

    private final transient String kmsKeyArn;
    private final transient Boolean bucketKeyEnabled;

    private SSEKMSEncryption(Builder builder) {
        this.kmsKeyArn = builder.kmsKeyArn;
        this.bucketKeyEnabled = builder.bucketKeyEnabled;
    }

    /**
     * Specifies the Amazon Web Services KMS key Amazon Resource Name (ARN) to use for the updated server-side
     * encryption type. Required if <code>ObjectEncryption</code> specifies <code>SSEKMS</code>.
     *
     * <p>You must specify the full Amazon Web Services KMS key ARN. The KMS key ID and KMS key alias aren't supported.
     *
     * <p>Pattern: (<code>arn:aws[-a-z0-9]&#42;:kms:[-a-z0-9]&#42;:[0-9]{12}:key/.+</code>)
     */
    public String getKmsKeyArn() {
        return kmsKeyArn;
    }

    /**
     * Specifies whether Amazon S3 should use an S3 Bucket Key for object encryption with server-side encryption using
     * Key Management Service (KMS) keys (SSE-KMS). If this value isn't specified, it defaults to <code>false</code>.
     * Setting this value to <code>true</code> causes Amazon S3 to use an S3 Bucket Key for object encryption with
     * SSE-KMS. For more information, see <a href="https://docs.aws.amazon.com/AmazonS3/latest/userguide/bucket-key.html"> Using Amazon S3 Bucket Keys</a> in the <i>Amazon S3 User Guide</i>.
     *
     * <p>Valid Values: <code>true</code> | <code>false</code>
     */
    public Boolean isBucketKeyEnabled() {
        return bucketKeyEnabled;
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
        SSEKMSEncryption that = (SSEKMSEncryption) other;
        return Objects.equals(this.bucketKeyEnabled, that.bucketKeyEnabled)
               && Objects.equals(this.kmsKeyArn, that.kmsKeyArn);
    }

    @Override
    public int hashCode() {
        int $hc = Objects.hashCode(kmsKeyArn);
        $hc = 31 * $hc + Objects.hashCode(bucketKeyEnabled);
        return $hc;
    }

    @Override
    public Schema schema() {
        return $SCHEMA;
    }

    @Override
    public void serializeMembers(ShapeSerializer serializer) {
        serializer.writeString($SCHEMA_KMS_KEY_ARN, kmsKeyArn);
        if (bucketKeyEnabled != null) {
            serializer.writeBoolean($SCHEMA_BUCKET_KEY_ENABLED, bucketKeyEnabled);
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T getMemberValue(Schema member) {
        return switch (member.memberIndex()) {
            case 0 -> (T) SchemaUtils.validateSameMember($SCHEMA_KMS_KEY_ARN, member, kmsKeyArn);
            case 1 -> (T) SchemaUtils.validateSameMember($SCHEMA_BUCKET_KEY_ENABLED, member, bucketKeyEnabled);
            default -> throw new IllegalArgumentException("Attempted to get non-existent member: " + member.id());
        };
    }

    /**
     * Create a new builder containing all the current property values of this object.
     *
     * <p><strong>Note:</strong> This method performs only a shallow copy of the original properties.
     *
     * @return a builder for {@link SSEKMSEncryption}.
     */
    public Builder toBuilder() {
        var builder = new Builder();
        builder.kmsKeyArn(this.kmsKeyArn);
        builder.bucketKeyEnabled(this.bucketKeyEnabled);
        return builder;
    }

    /**
     * @return returns a new Builder.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for {@link SSEKMSEncryption}.
     */
    public static final class Builder implements ShapeBuilder<SSEKMSEncryption> {
        private final PresenceTracker tracker = PresenceTracker.of($SCHEMA);
        private String kmsKeyArn;
        private Boolean bucketKeyEnabled;

        private Builder() {}

        @Override
        public Schema schema() {
            return $SCHEMA;
        }

        /**
         * Specifies the Amazon Web Services KMS key Amazon Resource Name (ARN) to use for the updated server-side
         * encryption type. Required if <code>ObjectEncryption</code> specifies <code>SSEKMS</code>.
         *
         * <p>You must specify the full Amazon Web Services KMS key ARN. The KMS key ID and KMS key alias aren't supported.
         *
         * <p>Pattern: (<code>arn:aws[-a-z0-9]&#42;:kms:[-a-z0-9]&#42;:[0-9]{12}:key/.+</code>)
         *
         * <p><strong>Required</strong>
         * @return this builder.
         */
        public Builder kmsKeyArn(String kmsKeyArn) {
            this.kmsKeyArn = Objects.requireNonNull(kmsKeyArn, "kmsKeyArn cannot be null");
            tracker.setMember($SCHEMA_KMS_KEY_ARN);
            return this;
        }

        /**
         * Specifies whether Amazon S3 should use an S3 Bucket Key for object encryption with server-side encryption using
         * Key Management Service (KMS) keys (SSE-KMS). If this value isn't specified, it defaults to <code>false</code>.
         * Setting this value to <code>true</code> causes Amazon S3 to use an S3 Bucket Key for object encryption with
         * SSE-KMS. For more information, see <a href="https://docs.aws.amazon.com/AmazonS3/latest/userguide/bucket-key.html"> Using Amazon S3 Bucket Keys</a> in the <i>Amazon S3 User Guide</i>.
         *
         * <p>Valid Values: <code>true</code> | <code>false</code>
         *
         * @return this builder.
         */
        public Builder bucketKeyEnabled(Boolean bucketKeyEnabled) {
            this.bucketKeyEnabled = bucketKeyEnabled;
            return this;
        }

        @Override
        public SSEKMSEncryption build() {
            tracker.validate();
            return new SSEKMSEncryption(this);
        }

        @Override
        @SuppressWarnings("unchecked")
        public void setMemberValue(Schema member, Object value) {
            switch (member.memberIndex()) {
                case 0 -> kmsKeyArn((String) SchemaUtils.validateSameMember($SCHEMA_KMS_KEY_ARN, member, value));
                case 1 -> bucketKeyEnabled((Boolean) SchemaUtils.validateSameMember($SCHEMA_BUCKET_KEY_ENABLED, member, value));
                default -> ShapeBuilder.super.setMemberValue(member, value);
            }
        }

        @Override
        public ShapeBuilder<SSEKMSEncryption> errorCorrection() {
            if (tracker.allSet()) {
                return this;
            }
            if (!tracker.checkMember($SCHEMA_KMS_KEY_ARN)) {
                kmsKeyArn("");
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
                    case 0 -> builder.kmsKeyArn(de.readString(member));
                    case 1 -> builder.bucketKeyEnabled(de.readBoolean(member));
                    default -> throw new IllegalArgumentException("Unexpected member: " + member.memberName());
                }
            }
        }
    }
}
