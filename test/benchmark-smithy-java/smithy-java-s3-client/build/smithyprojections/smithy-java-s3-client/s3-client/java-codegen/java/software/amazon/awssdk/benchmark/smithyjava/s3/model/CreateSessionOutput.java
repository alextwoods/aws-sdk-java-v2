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

@SmithyGenerated
public final class CreateSessionOutput implements SerializableStruct {

    public static final Schema $SCHEMA = Schemas1.CREATE_SESSION_OUTPUT;
    private static final Schema $SCHEMA_SERVER_SIDE_ENCRYPTION = $SCHEMA.member("ServerSideEncryption");
    private static final Schema $SCHEMA_SSEKMS_KEY_ID = $SCHEMA.member("SSEKMSKeyId");
    private static final Schema $SCHEMA_SSEKMS_ENCRYPTION_CONTEXT = $SCHEMA.member("SSEKMSEncryptionContext");
    private static final Schema $SCHEMA_BUCKET_KEY_ENABLED = $SCHEMA.member("BucketKeyEnabled");
    private static final Schema $SCHEMA_CREDENTIALS = $SCHEMA.member("Credentials");

    public static final ShapeId $ID = $SCHEMA.id();

    private final transient ServerSideEncryption serverSideEncryption;
    private final transient String ssekmsKeyId;
    private final transient String ssekmsEncryptionContext;
    private final transient Boolean bucketKeyEnabled;
    private final transient SessionCredentials credentials;

    private CreateSessionOutput(Builder builder) {
        this.serverSideEncryption = builder.serverSideEncryption;
        this.ssekmsKeyId = builder.ssekmsKeyId;
        this.ssekmsEncryptionContext = builder.ssekmsEncryptionContext;
        this.bucketKeyEnabled = builder.bucketKeyEnabled;
        this.credentials = builder.credentials;
    }

    /**
     * The server-side encryption algorithm used when you store objects in the directory bucket.
     *
     * <p>When accessing data stored in Amazon FSx file systems using S3 access points, the only valid server side
     * encryption option is <code>aws:fsx</code>.
     */
    public ServerSideEncryption getServerSideEncryption() {
        return serverSideEncryption;
    }

    /**
     * If you specify <code>x-amz-server-side-encryption</code> with <code>aws:kms</code>, this header indicates the ID
     * of the KMS symmetric encryption customer managed key that was used for object encryption.
     */
    public String getSsekmsKeyId() {
        return ssekmsKeyId;
    }

    /**
     * If present, indicates the Amazon Web Services KMS Encryption Context to use for object encryption. The value of
     * this header is a Base64 encoded string of a UTF-8 encoded JSON, which contains the encryption context as
     * key-value pairs. This value is stored as object metadata and automatically gets passed on to Amazon Web Services
     * KMS for future <code>GetObject</code> operations on this object.
     */
    public String getSsekmsEncryptionContext() {
        return ssekmsEncryptionContext;
    }

    /**
     * Indicates whether to use an S3 Bucket Key for server-side encryption with KMS keys (SSE-KMS).
     */
    public Boolean isBucketKeyEnabled() {
        return bucketKeyEnabled;
    }

    /**
     * The established temporary security credentials for the created session.
     */
    public SessionCredentials getCredentials() {
        return credentials;
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
        CreateSessionOutput that = (CreateSessionOutput) other;
        return Objects.equals(this.bucketKeyEnabled, that.bucketKeyEnabled)
               && Objects.equals(this.ssekmsKeyId, that.ssekmsKeyId)
               && Objects.equals(this.ssekmsEncryptionContext, that.ssekmsEncryptionContext)
               && Objects.equals(this.serverSideEncryption, that.serverSideEncryption)
               && Objects.equals(this.credentials, that.credentials);
    }

    @Override
    public int hashCode() {
        int $hc = Objects.hashCode(serverSideEncryption);
        $hc = 31 * $hc + Objects.hashCode(ssekmsKeyId);
        $hc = 31 * $hc + Objects.hashCode(ssekmsEncryptionContext);
        $hc = 31 * $hc + Objects.hashCode(bucketKeyEnabled);
        $hc = 31 * $hc + Objects.hashCode(credentials);
        return $hc;
    }

    @Override
    public Schema schema() {
        return $SCHEMA;
    }

    @Override
    public void serializeMembers(ShapeSerializer serializer) {
        if (serverSideEncryption != null) {
            serializer.writeString($SCHEMA_SERVER_SIDE_ENCRYPTION, serverSideEncryption.getValue());
        }
        if (ssekmsKeyId != null) {
            serializer.writeString($SCHEMA_SSEKMS_KEY_ID, ssekmsKeyId);
        }
        if (ssekmsEncryptionContext != null) {
            serializer.writeString($SCHEMA_SSEKMS_ENCRYPTION_CONTEXT, ssekmsEncryptionContext);
        }
        if (bucketKeyEnabled != null) {
            serializer.writeBoolean($SCHEMA_BUCKET_KEY_ENABLED, bucketKeyEnabled);
        }
        if (credentials != null) {
            serializer.writeStruct($SCHEMA_CREDENTIALS, credentials);
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T getMemberValue(Schema member) {
        return switch (member.memberIndex()) {
            case 0 -> (T) SchemaUtils.validateSameMember($SCHEMA_CREDENTIALS, member, credentials);
            case 1 -> (T) SchemaUtils.validateSameMember($SCHEMA_SERVER_SIDE_ENCRYPTION, member, serverSideEncryption);
            case 2 -> (T) SchemaUtils.validateSameMember($SCHEMA_SSEKMS_KEY_ID, member, ssekmsKeyId);
            case 3 -> (T) SchemaUtils.validateSameMember($SCHEMA_SSEKMS_ENCRYPTION_CONTEXT, member, ssekmsEncryptionContext);
            case 4 -> (T) SchemaUtils.validateSameMember($SCHEMA_BUCKET_KEY_ENABLED, member, bucketKeyEnabled);
            default -> throw new IllegalArgumentException("Attempted to get non-existent member: " + member.id());
        };
    }

    /**
     * Create a new builder containing all the current property values of this object.
     *
     * <p><strong>Note:</strong> This method performs only a shallow copy of the original properties.
     *
     * @return a builder for {@link CreateSessionOutput}.
     */
    public Builder toBuilder() {
        var builder = new Builder();
        builder.serverSideEncryption(this.serverSideEncryption);
        builder.ssekmsKeyId(this.ssekmsKeyId);
        builder.ssekmsEncryptionContext(this.ssekmsEncryptionContext);
        builder.bucketKeyEnabled(this.bucketKeyEnabled);
        builder.credentials(this.credentials);
        return builder;
    }

    /**
     * @return returns a new Builder.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for {@link CreateSessionOutput}.
     */
    public static final class Builder implements ShapeBuilder<CreateSessionOutput> {
        private final PresenceTracker tracker = PresenceTracker.of($SCHEMA);
        private ServerSideEncryption serverSideEncryption;
        private String ssekmsKeyId;
        private String ssekmsEncryptionContext;
        private Boolean bucketKeyEnabled;
        private SessionCredentials credentials;

        private Builder() {}

        @Override
        public Schema schema() {
            return $SCHEMA;
        }

        /**
         * The server-side encryption algorithm used when you store objects in the directory bucket.
         *
         * <p>When accessing data stored in Amazon FSx file systems using S3 access points, the only valid server side
         * encryption option is <code>aws:fsx</code>.
         *
         * @return this builder.
         */
        public Builder serverSideEncryption(ServerSideEncryption serverSideEncryption) {
            this.serverSideEncryption = serverSideEncryption;
            return this;
        }

        /**
         * If you specify <code>x-amz-server-side-encryption</code> with <code>aws:kms</code>, this header indicates the ID
         * of the KMS symmetric encryption customer managed key that was used for object encryption.
         *
         * @return this builder.
         */
        public Builder ssekmsKeyId(String ssekmsKeyId) {
            this.ssekmsKeyId = ssekmsKeyId;
            return this;
        }

        /**
         * If present, indicates the Amazon Web Services KMS Encryption Context to use for object encryption. The value of
         * this header is a Base64 encoded string of a UTF-8 encoded JSON, which contains the encryption context as
         * key-value pairs. This value is stored as object metadata and automatically gets passed on to Amazon Web Services
         * KMS for future <code>GetObject</code> operations on this object.
         *
         * @return this builder.
         */
        public Builder ssekmsEncryptionContext(String ssekmsEncryptionContext) {
            this.ssekmsEncryptionContext = ssekmsEncryptionContext;
            return this;
        }

        /**
         * Indicates whether to use an S3 Bucket Key for server-side encryption with KMS keys (SSE-KMS).
         *
         * @return this builder.
         */
        public Builder bucketKeyEnabled(Boolean bucketKeyEnabled) {
            this.bucketKeyEnabled = bucketKeyEnabled;
            return this;
        }

        /**
         * The established temporary security credentials for the created session.
         *
         * <p><strong>Required</strong>
         * @return this builder.
         */
        public Builder credentials(SessionCredentials credentials) {
            this.credentials = Objects.requireNonNull(credentials, "credentials cannot be null");
            tracker.setMember($SCHEMA_CREDENTIALS);
            return this;
        }

        @Override
        public CreateSessionOutput build() {
            tracker.validate();
            return new CreateSessionOutput(this);
        }

        @Override
        @SuppressWarnings("unchecked")
        public void setMemberValue(Schema member, Object value) {
            switch (member.memberIndex()) {
                case 0 -> credentials((SessionCredentials) SchemaUtils.validateSameMember($SCHEMA_CREDENTIALS, member, value));
                case 1 -> serverSideEncryption((ServerSideEncryption) SchemaUtils.validateSameMember($SCHEMA_SERVER_SIDE_ENCRYPTION, member, value));
                case 2 -> ssekmsKeyId((String) SchemaUtils.validateSameMember($SCHEMA_SSEKMS_KEY_ID, member, value));
                case 3 -> ssekmsEncryptionContext((String) SchemaUtils.validateSameMember($SCHEMA_SSEKMS_ENCRYPTION_CONTEXT, member, value));
                case 4 -> bucketKeyEnabled((Boolean) SchemaUtils.validateSameMember($SCHEMA_BUCKET_KEY_ENABLED, member, value));
                default -> ShapeBuilder.super.setMemberValue(member, value);
            }
        }

        @Override
        public ShapeBuilder<CreateSessionOutput> errorCorrection() {
            if (tracker.allSet()) {
                return this;
            }
            if (!tracker.checkMember($SCHEMA_CREDENTIALS)) {
                tracker.setMember($SCHEMA_CREDENTIALS);
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
                    case 0 -> builder.credentials(SessionCredentials.builder().deserializeMember(de, member).build());
                    case 1 -> builder.serverSideEncryption(ServerSideEncryption.builder().deserializeMember(de, member).build());
                    case 2 -> builder.ssekmsKeyId(de.readString(member));
                    case 3 -> builder.ssekmsEncryptionContext(de.readString(member));
                    case 4 -> builder.bucketKeyEnabled(de.readBoolean(member));
                    default -> throw new IllegalArgumentException("Unexpected member: " + member.memberName());
                }
            }
        }
    }
}
