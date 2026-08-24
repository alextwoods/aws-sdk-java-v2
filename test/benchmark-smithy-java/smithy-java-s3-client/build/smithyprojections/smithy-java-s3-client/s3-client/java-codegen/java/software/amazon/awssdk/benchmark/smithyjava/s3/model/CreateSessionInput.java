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
public final class CreateSessionInput implements SerializableStruct {

    public static final Schema $SCHEMA = Schemas1.CREATE_SESSION_INPUT;
    private static final Schema $SCHEMA_SESSION_MODE = $SCHEMA.member("SessionMode");
    private static final Schema $SCHEMA_BUCKET = $SCHEMA.member("Bucket");
    private static final Schema $SCHEMA_SERVER_SIDE_ENCRYPTION = $SCHEMA.member("ServerSideEncryption");
    private static final Schema $SCHEMA_SSEKMS_KEY_ID = $SCHEMA.member("SSEKMSKeyId");
    private static final Schema $SCHEMA_SSEKMS_ENCRYPTION_CONTEXT = $SCHEMA.member("SSEKMSEncryptionContext");
    private static final Schema $SCHEMA_BUCKET_KEY_ENABLED = $SCHEMA.member("BucketKeyEnabled");

    public static final ShapeId $ID = $SCHEMA.id();

    private final transient SessionMode sessionMode;
    private final transient String bucket;
    private final transient ServerSideEncryption serverSideEncryption;
    private final transient String ssekmsKeyId;
    private final transient String ssekmsEncryptionContext;
    private final transient Boolean bucketKeyEnabled;

    private CreateSessionInput(Builder builder) {
        this.sessionMode = builder.sessionMode;
        this.bucket = builder.bucket;
        this.serverSideEncryption = builder.serverSideEncryption;
        this.ssekmsKeyId = builder.ssekmsKeyId;
        this.ssekmsEncryptionContext = builder.ssekmsEncryptionContext;
        this.bucketKeyEnabled = builder.bucketKeyEnabled;
    }

    /**
     * Specifies the mode of the session that will be created, either <code>ReadWrite</code> or <code>ReadOnly</code>.
     * If no session mode is specified, the default behavior attempts to create a session with the maximum allowable
     * privilege. It will first attempt to create a <code>ReadWrite</code> session, and if that is not allowed by
     * permissions, it will attempt to create a <code>ReadOnly</code> session. If neither session type is allowed, the
     * request will return an Access Denied error. A <code>ReadWrite</code> session is capable of executing all the
     * Zonal endpoint API operations on a directory bucket. A <code>ReadOnly</code> session is constrained to execute
     * the following Zonal endpoint API operations: <code>GetObject</code>, <code>HeadObject</code>,
     * <code>ListObjectsV2</code>, <code>GetObjectAttributes</code>, <code>ListParts</code>, and
     * <code>ListMultipartUploads</code>.
     */
    public SessionMode getSessionMode() {
        return sessionMode;
    }

    /**
     * The name of the bucket that you create a session for.
     */
    public String getBucket() {
        return bucket;
    }

    /**
     * The server-side encryption algorithm to use when you store objects in the directory bucket.
     *
     * <p>For directory buckets, there are only two supported options for server-side encryption: server-side encryption
     * with Amazon S3 managed keys (SSE-S3) (<code>AES256</code>) and server-side encryption with KMS keys (SSE-KMS) (<code>
     * aws:kms</code>). By default, Amazon S3 encrypts data with SSE-S3. For more information, see <a href="https://docs.aws.amazon.com/AmazonS3/latest/userguide/serv-side-encryption.html">Protecting data
     * with server-side encryption</a> in the <i>Amazon S3 User Guide</i>.
     *
     * <p><b>S3 access points for Amazon FSx </b> - When accessing data stored in Amazon FSx file systems using S3
     * access points, the only valid server side encryption option is <code>aws:fsx</code>. All Amazon FSx file systems
     * have encryption configured by default and are encrypted at rest. Data is automatically encrypted before being
     * written to the file system, and automatically decrypted as it is read. These processes are handled transparently
     * by Amazon FSx.
     */
    public ServerSideEncryption getServerSideEncryption() {
        return serverSideEncryption;
    }

    /**
     * If you specify <code>x-amz-server-side-encryption</code> with <code>aws:kms</code>, you must specify the <code>
     * x-amz-server-side-encryption-aws-kms-key-id</code> header with the ID (Key ID or Key ARN) of the KMS symmetric
     * encryption customer managed key to use. Otherwise, you get an HTTP <code>400 Bad Request</code> error. Only use
     * the key ID or key ARN. The key alias format of the KMS key isn't supported. Also, if the KMS key doesn't exist in
     * the same account that't issuing the command, you must use the full Key ARN not the Key ID.
     *
     * <p>Your SSE-KMS configuration can only support 1 <a href="https://docs.aws.amazon.com/kms/latest/developerguide/concepts.html#customer-cmk">customer managed key</a> per directory bucket's lifetime. The
     * <a href="https://docs.aws.amazon.com/kms/latest/developerguide/concepts.html#aws-managed-cmk">Amazon Web Services managed key</a> (<code>aws/s3</code>) isn't supported.
     */
    public String getSsekmsKeyId() {
        return ssekmsKeyId;
    }

    /**
     * Specifies the Amazon Web Services KMS Encryption Context as an additional encryption context to use for object
     * encryption. The value of this header is a Base64 encoded string of a UTF-8 encoded JSON, which contains the
     * encryption context as key-value pairs. This value is stored as object metadata and automatically gets passed on
     * to Amazon Web Services KMS for future <code>GetObject</code> operations on this object.
     *
     * <p><b>General purpose buckets</b> - This value must be explicitly added during <code>CopyObject</code> operations
     * if you want an additional encryption context for your object. For more information, see <a href="https://docs.aws.amazon.com/AmazonS3/latest/userguide/UsingKMSEncryption.html#encryption-context">Encryption context</a>
     * in the <i>Amazon S3 User Guide</i>.
     *
     * <p><b>Directory buckets</b> - You can optionally provide an explicit encryption context value. The value must
     * match the default encryption context - the bucket Amazon Resource Name (ARN). An additional encryption context
     * value is not supported.
     */
    public String getSsekmsEncryptionContext() {
        return ssekmsEncryptionContext;
    }

    /**
     * Specifies whether Amazon S3 should use an S3 Bucket Key for object encryption with server-side encryption using
     * KMS keys (SSE-KMS).
     *
     * <p>S3 Bucket Keys are always enabled for <code>GET</code> and <code>PUT</code> operations in a directory bucket
     * and can’t be disabled. S3 Bucket Keys aren't supported, when you copy SSE-KMS encrypted objects from general
     * purpose buckets to directory buckets, from directory buckets to general purpose buckets, or between directory
     * buckets, through <a href="https://docs.aws.amazon.com/AmazonS3/latest/API/API_CopyObject.html">CopyObject</a>, <a href="https://docs.aws.amazon.com/AmazonS3/latest/API/API_UploadPartCopy.html">UploadPartCopy</a>, <a href="https://docs.aws.amazon.com/AmazonS3/latest/userguide/directory-buckets-objects-Batch-Ops">the Copy operation in Batch Operations</a>, or <a href="https://docs.aws.amazon.com/AmazonS3/latest/userguide/create-import-job">
     * the import jobs</a>. In this case, Amazon S3 makes a call to KMS every time a copy request is made for a
     * KMS-encrypted object.
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
        CreateSessionInput that = (CreateSessionInput) other;
        return Objects.equals(this.bucketKeyEnabled, that.bucketKeyEnabled)
               && Objects.equals(this.bucket, that.bucket)
               && Objects.equals(this.ssekmsKeyId, that.ssekmsKeyId)
               && Objects.equals(this.ssekmsEncryptionContext, that.ssekmsEncryptionContext)
               && Objects.equals(this.sessionMode, that.sessionMode)
               && Objects.equals(this.serverSideEncryption, that.serverSideEncryption);
    }

    @Override
    public int hashCode() {
        int $hc = Objects.hashCode(sessionMode);
        $hc = 31 * $hc + Objects.hashCode(bucket);
        $hc = 31 * $hc + Objects.hashCode(serverSideEncryption);
        $hc = 31 * $hc + Objects.hashCode(ssekmsKeyId);
        $hc = 31 * $hc + Objects.hashCode(ssekmsEncryptionContext);
        $hc = 31 * $hc + Objects.hashCode(bucketKeyEnabled);
        return $hc;
    }

    @Override
    public Schema schema() {
        return $SCHEMA;
    }

    @Override
    public void serializeMembers(ShapeSerializer serializer) {
        if (sessionMode != null) {
            serializer.writeString($SCHEMA_SESSION_MODE, sessionMode.getValue());
        }
        serializer.writeString($SCHEMA_BUCKET, bucket);
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
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T getMemberValue(Schema member) {
        return switch (member.memberIndex()) {
            case 0 -> (T) SchemaUtils.validateSameMember($SCHEMA_BUCKET, member, bucket);
            case 1 -> (T) SchemaUtils.validateSameMember($SCHEMA_SESSION_MODE, member, sessionMode);
            case 2 -> (T) SchemaUtils.validateSameMember($SCHEMA_SERVER_SIDE_ENCRYPTION, member, serverSideEncryption);
            case 3 -> (T) SchemaUtils.validateSameMember($SCHEMA_SSEKMS_KEY_ID, member, ssekmsKeyId);
            case 4 -> (T) SchemaUtils.validateSameMember($SCHEMA_SSEKMS_ENCRYPTION_CONTEXT, member, ssekmsEncryptionContext);
            case 5 -> (T) SchemaUtils.validateSameMember($SCHEMA_BUCKET_KEY_ENABLED, member, bucketKeyEnabled);
            default -> throw new IllegalArgumentException("Attempted to get non-existent member: " + member.id());
        };
    }

    /**
     * Create a new builder containing all the current property values of this object.
     *
     * <p><strong>Note:</strong> This method performs only a shallow copy of the original properties.
     *
     * @return a builder for {@link CreateSessionInput}.
     */
    public Builder toBuilder() {
        var builder = new Builder();
        builder.sessionMode(this.sessionMode);
        builder.bucket(this.bucket);
        builder.serverSideEncryption(this.serverSideEncryption);
        builder.ssekmsKeyId(this.ssekmsKeyId);
        builder.ssekmsEncryptionContext(this.ssekmsEncryptionContext);
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
     * Builder for {@link CreateSessionInput}.
     */
    public static final class Builder implements ShapeBuilder<CreateSessionInput> {
        private final PresenceTracker tracker = PresenceTracker.of($SCHEMA);
        private SessionMode sessionMode;
        private String bucket;
        private ServerSideEncryption serverSideEncryption;
        private String ssekmsKeyId;
        private String ssekmsEncryptionContext;
        private Boolean bucketKeyEnabled;

        private Builder() {}

        @Override
        public Schema schema() {
            return $SCHEMA;
        }

        /**
         * Specifies the mode of the session that will be created, either <code>ReadWrite</code> or <code>ReadOnly</code>.
         * If no session mode is specified, the default behavior attempts to create a session with the maximum allowable
         * privilege. It will first attempt to create a <code>ReadWrite</code> session, and if that is not allowed by
         * permissions, it will attempt to create a <code>ReadOnly</code> session. If neither session type is allowed, the
         * request will return an Access Denied error. A <code>ReadWrite</code> session is capable of executing all the
         * Zonal endpoint API operations on a directory bucket. A <code>ReadOnly</code> session is constrained to execute
         * the following Zonal endpoint API operations: <code>GetObject</code>, <code>HeadObject</code>,
         * <code>ListObjectsV2</code>, <code>GetObjectAttributes</code>, <code>ListParts</code>, and
         * <code>ListMultipartUploads</code>.
         *
         * @return this builder.
         */
        public Builder sessionMode(SessionMode sessionMode) {
            this.sessionMode = sessionMode;
            return this;
        }

        /**
         * The name of the bucket that you create a session for.
         *
         * <p><strong>Required</strong>
         * @return this builder.
         */
        public Builder bucket(String bucket) {
            this.bucket = Objects.requireNonNull(bucket, "bucket cannot be null");
            tracker.setMember($SCHEMA_BUCKET);
            return this;
        }

        /**
         * The server-side encryption algorithm to use when you store objects in the directory bucket.
         *
         * <p>For directory buckets, there are only two supported options for server-side encryption: server-side encryption
         * with Amazon S3 managed keys (SSE-S3) (<code>AES256</code>) and server-side encryption with KMS keys (SSE-KMS) (<code>
         * aws:kms</code>). By default, Amazon S3 encrypts data with SSE-S3. For more information, see <a href="https://docs.aws.amazon.com/AmazonS3/latest/userguide/serv-side-encryption.html">Protecting data
         * with server-side encryption</a> in the <i>Amazon S3 User Guide</i>.
         *
         * <p><b>S3 access points for Amazon FSx </b> - When accessing data stored in Amazon FSx file systems using S3
         * access points, the only valid server side encryption option is <code>aws:fsx</code>. All Amazon FSx file systems
         * have encryption configured by default and are encrypted at rest. Data is automatically encrypted before being
         * written to the file system, and automatically decrypted as it is read. These processes are handled transparently
         * by Amazon FSx.
         *
         * @return this builder.
         */
        public Builder serverSideEncryption(ServerSideEncryption serverSideEncryption) {
            this.serverSideEncryption = serverSideEncryption;
            return this;
        }

        /**
         * If you specify <code>x-amz-server-side-encryption</code> with <code>aws:kms</code>, you must specify the <code>
         * x-amz-server-side-encryption-aws-kms-key-id</code> header with the ID (Key ID or Key ARN) of the KMS symmetric
         * encryption customer managed key to use. Otherwise, you get an HTTP <code>400 Bad Request</code> error. Only use
         * the key ID or key ARN. The key alias format of the KMS key isn't supported. Also, if the KMS key doesn't exist in
         * the same account that't issuing the command, you must use the full Key ARN not the Key ID.
         *
         * <p>Your SSE-KMS configuration can only support 1 <a href="https://docs.aws.amazon.com/kms/latest/developerguide/concepts.html#customer-cmk">customer managed key</a> per directory bucket's lifetime. The
         * <a href="https://docs.aws.amazon.com/kms/latest/developerguide/concepts.html#aws-managed-cmk">Amazon Web Services managed key</a> (<code>aws/s3</code>) isn't supported.
         *
         * @return this builder.
         */
        public Builder ssekmsKeyId(String ssekmsKeyId) {
            this.ssekmsKeyId = ssekmsKeyId;
            return this;
        }

        /**
         * Specifies the Amazon Web Services KMS Encryption Context as an additional encryption context to use for object
         * encryption. The value of this header is a Base64 encoded string of a UTF-8 encoded JSON, which contains the
         * encryption context as key-value pairs. This value is stored as object metadata and automatically gets passed on
         * to Amazon Web Services KMS for future <code>GetObject</code> operations on this object.
         *
         * <p><b>General purpose buckets</b> - This value must be explicitly added during <code>CopyObject</code> operations
         * if you want an additional encryption context for your object. For more information, see <a href="https://docs.aws.amazon.com/AmazonS3/latest/userguide/UsingKMSEncryption.html#encryption-context">Encryption context</a>
         * in the <i>Amazon S3 User Guide</i>.
         *
         * <p><b>Directory buckets</b> - You can optionally provide an explicit encryption context value. The value must
         * match the default encryption context - the bucket Amazon Resource Name (ARN). An additional encryption context
         * value is not supported.
         *
         * @return this builder.
         */
        public Builder ssekmsEncryptionContext(String ssekmsEncryptionContext) {
            this.ssekmsEncryptionContext = ssekmsEncryptionContext;
            return this;
        }

        /**
         * Specifies whether Amazon S3 should use an S3 Bucket Key for object encryption with server-side encryption using
         * KMS keys (SSE-KMS).
         *
         * <p>S3 Bucket Keys are always enabled for <code>GET</code> and <code>PUT</code> operations in a directory bucket
         * and can’t be disabled. S3 Bucket Keys aren't supported, when you copy SSE-KMS encrypted objects from general
         * purpose buckets to directory buckets, from directory buckets to general purpose buckets, or between directory
         * buckets, through <a href="https://docs.aws.amazon.com/AmazonS3/latest/API/API_CopyObject.html">CopyObject</a>, <a href="https://docs.aws.amazon.com/AmazonS3/latest/API/API_UploadPartCopy.html">UploadPartCopy</a>, <a href="https://docs.aws.amazon.com/AmazonS3/latest/userguide/directory-buckets-objects-Batch-Ops">the Copy operation in Batch Operations</a>, or <a href="https://docs.aws.amazon.com/AmazonS3/latest/userguide/create-import-job">
         * the import jobs</a>. In this case, Amazon S3 makes a call to KMS every time a copy request is made for a
         * KMS-encrypted object.
         *
         * @return this builder.
         */
        public Builder bucketKeyEnabled(Boolean bucketKeyEnabled) {
            this.bucketKeyEnabled = bucketKeyEnabled;
            return this;
        }

        @Override
        public CreateSessionInput build() {
            tracker.validate();
            return new CreateSessionInput(this);
        }

        @Override
        @SuppressWarnings("unchecked")
        public void setMemberValue(Schema member, Object value) {
            switch (member.memberIndex()) {
                case 0 -> bucket((String) SchemaUtils.validateSameMember($SCHEMA_BUCKET, member, value));
                case 1 -> sessionMode((SessionMode) SchemaUtils.validateSameMember($SCHEMA_SESSION_MODE, member, value));
                case 2 -> serverSideEncryption((ServerSideEncryption) SchemaUtils.validateSameMember($SCHEMA_SERVER_SIDE_ENCRYPTION, member, value));
                case 3 -> ssekmsKeyId((String) SchemaUtils.validateSameMember($SCHEMA_SSEKMS_KEY_ID, member, value));
                case 4 -> ssekmsEncryptionContext((String) SchemaUtils.validateSameMember($SCHEMA_SSEKMS_ENCRYPTION_CONTEXT, member, value));
                case 5 -> bucketKeyEnabled((Boolean) SchemaUtils.validateSameMember($SCHEMA_BUCKET_KEY_ENABLED, member, value));
                default -> ShapeBuilder.super.setMemberValue(member, value);
            }
        }

        @Override
        public ShapeBuilder<CreateSessionInput> errorCorrection() {
            if (tracker.allSet()) {
                return this;
            }
            if (!tracker.checkMember($SCHEMA_BUCKET)) {
                bucket("");
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
                    case 0 -> builder.bucket(de.readString(member));
                    case 1 -> builder.sessionMode(SessionMode.builder().deserializeMember(de, member).build());
                    case 2 -> builder.serverSideEncryption(ServerSideEncryption.builder().deserializeMember(de, member).build());
                    case 3 -> builder.ssekmsKeyId(de.readString(member));
                    case 4 -> builder.ssekmsEncryptionContext(de.readString(member));
                    case 5 -> builder.bucketKeyEnabled(de.readBoolean(member));
                    default -> throw new IllegalArgumentException("Unexpected member: " + member.memberName());
                }
            }
        }
    }
}
