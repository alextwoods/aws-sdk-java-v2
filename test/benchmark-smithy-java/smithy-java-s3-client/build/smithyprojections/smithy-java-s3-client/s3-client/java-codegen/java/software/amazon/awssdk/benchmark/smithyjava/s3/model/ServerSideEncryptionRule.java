package software.amazon.awssdk.benchmark.smithyjava.s3.model;

import java.util.Objects;
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
 * Specifies the default server-side encryption configuration.
 *
 * <ul>
 *   <li>
 *     <b>General purpose buckets</b> - If you're specifying a customer managed KMS key, we recommend using a fully
 *     qualified KMS key ARN. If you use a KMS key alias instead, then KMS resolves the key within the requester’s
 *     account. This behavior can result in data that's encrypted with a KMS key that belongs to the requester, and
 *     not the bucket owner.
 *   </li>
 *   <li>
 *     <b>Directory buckets</b> - When you specify an <a href="https://docs.aws.amazon.com/kms/latest/developerguide/concepts.html#customer-cmk">KMS customer managed key</a> for encryption in your
 *     directory bucket, only use the key ID or key ARN. The key alias format of the KMS key isn't supported.
 *   </li>
 * </ul>
 */
@SmithyGenerated
public final class ServerSideEncryptionRule implements SerializableStruct {

    public static final Schema $SCHEMA = Schemas2.SERVER_SIDE_ENCRYPTION_RULE;
    private static final Schema $SCHEMA_APPLY_SERVER_SIDE_ENCRYPTION_BY_DEFAULT = $SCHEMA.member("ApplyServerSideEncryptionByDefault");
    private static final Schema $SCHEMA_BUCKET_KEY_ENABLED = $SCHEMA.member("BucketKeyEnabled");
    private static final Schema $SCHEMA_BLOCKED_ENCRYPTION_TYPES = $SCHEMA.member("BlockedEncryptionTypes");

    public static final ShapeId $ID = $SCHEMA.id();

    private final transient ServerSideEncryptionByDefault applyServerSideEncryptionByDefault;
    private final transient Boolean bucketKeyEnabled;
    private final transient BlockedEncryptionTypes blockedEncryptionTypes;

    private ServerSideEncryptionRule(Builder builder) {
        this.applyServerSideEncryptionByDefault = builder.applyServerSideEncryptionByDefault;
        this.bucketKeyEnabled = builder.bucketKeyEnabled;
        this.blockedEncryptionTypes = builder.blockedEncryptionTypes;
    }

    /**
     * Specifies the default server-side encryption to apply to new objects in the bucket. If a PUT Object request
     * doesn't specify any server-side encryption, this default encryption will be applied.
     */
    public ServerSideEncryptionByDefault getApplyServerSideEncryptionByDefault() {
        return applyServerSideEncryptionByDefault;
    }

    /**
     * Specifies whether Amazon S3 should use an S3 Bucket Key with server-side encryption using KMS (SSE-KMS) for new
     * objects in the bucket. Existing objects are not affected. Setting the <code>BucketKeyEnabled</code> element to <code>
     * true</code> causes Amazon S3 to use an S3 Bucket Key.
     *
     * <ul>
     *   <li>
     *     <b>General purpose buckets</b> - By default, S3 Bucket Key is not enabled. For more information, see <a href="https://docs.aws.amazon.com/AmazonS3/latest/dev/bucket-key.html">
     *     Amazon S3 Bucket Keys</a> in the <i>Amazon S3 User Guide</i>.
     *   </li>
     *   <li>
     *     <b>Directory buckets</b> - S3 Bucket Keys are always enabled for <code>GET</code> and <code>PUT</code>
     *     operations in a directory bucket and can’t be disabled. S3 Bucket Keys aren't supported, when you copy
     *     SSE-KMS encrypted objects from general purpose buckets to directory buckets, from directory buckets to
     *     general purpose buckets, or between directory buckets, through <a href="https://docs.aws.amazon.com/AmazonS3/latest/API/API_CopyObject.html">CopyObject</a>, <a href="https://docs.aws.amazon.com/AmazonS3/latest/API/API_UploadPartCopy.html">UploadPartCopy</a>, <a href="https://docs.aws.amazon.com/AmazonS3/latest/userguide/directory-buckets-objects-Batch-Ops">
     *     the Copy operation in Batch Operations</a>, or <a href="https://docs.aws.amazon.com/AmazonS3/latest/userguide/create-import-job">the import jobs</a>. In this case, Amazon S3 makes a
     *     call to KMS every time a copy request is made for a KMS-encrypted object.
     *   </li>
     * </ul>
     */
    public Boolean isBucketKeyEnabled() {
        return bucketKeyEnabled;
    }

    /**
     * A bucket-level setting for Amazon S3 general purpose buckets used to prevent the upload of new objects encrypted
     * with the specified server-side encryption type. For example, blocking an encryption type will block <code>
     * PutObject</code>, <code>CopyObject</code>, <code>PostObject</code>, multipart upload, and replication requests to
     * the bucket for objects with the specified encryption type. However, you can continue to read and list any
     * pre-existing objects already encrypted with the specified encryption type. For more information, see <a href="https://docs.aws.amazon.com/AmazonS3/latest/userguide/blocking-unblocking-s3-c-encryption-gpb.html">Blocking
     * or unblocking SSE-C for a general purpose bucket</a>.
     *
     * <p>Currently, this parameter only supports blocking or unblocking server-side encryption with customer-provided
     * keys (SSE-C). For more information about SSE-C, see <a href="https://docs.aws.amazon.com/AmazonS3/latest/userguide/ServerSideEncryptionCustomerKeys.html">Using server-side encryption with customer-provided keys
     * (SSE-C)</a>.
     */
    public BlockedEncryptionTypes getBlockedEncryptionTypes() {
        return blockedEncryptionTypes;
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
        ServerSideEncryptionRule that = (ServerSideEncryptionRule) other;
        return Objects.equals(this.bucketKeyEnabled, that.bucketKeyEnabled)
               && Objects.equals(this.applyServerSideEncryptionByDefault, that.applyServerSideEncryptionByDefault)
               && Objects.equals(this.blockedEncryptionTypes, that.blockedEncryptionTypes);
    }

    @Override
    public int hashCode() {
        int $hc = Objects.hashCode(applyServerSideEncryptionByDefault);
        $hc = 31 * $hc + Objects.hashCode(bucketKeyEnabled);
        $hc = 31 * $hc + Objects.hashCode(blockedEncryptionTypes);
        return $hc;
    }

    @Override
    public Schema schema() {
        return $SCHEMA;
    }

    @Override
    public void serializeMembers(ShapeSerializer serializer) {
        if (applyServerSideEncryptionByDefault != null) {
            serializer.writeStruct($SCHEMA_APPLY_SERVER_SIDE_ENCRYPTION_BY_DEFAULT, applyServerSideEncryptionByDefault);
        }
        if (bucketKeyEnabled != null) {
            serializer.writeBoolean($SCHEMA_BUCKET_KEY_ENABLED, bucketKeyEnabled);
        }
        if (blockedEncryptionTypes != null) {
            serializer.writeStruct($SCHEMA_BLOCKED_ENCRYPTION_TYPES, blockedEncryptionTypes);
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T getMemberValue(Schema member) {
        return switch (member.memberIndex()) {
            case 0 -> (T) SchemaUtils.validateSameMember($SCHEMA_APPLY_SERVER_SIDE_ENCRYPTION_BY_DEFAULT, member, applyServerSideEncryptionByDefault);
            case 1 -> (T) SchemaUtils.validateSameMember($SCHEMA_BUCKET_KEY_ENABLED, member, bucketKeyEnabled);
            case 2 -> (T) SchemaUtils.validateSameMember($SCHEMA_BLOCKED_ENCRYPTION_TYPES, member, blockedEncryptionTypes);
            default -> throw new IllegalArgumentException("Attempted to get non-existent member: " + member.id());
        };
    }

    /**
     * Create a new builder containing all the current property values of this object.
     *
     * <p><strong>Note:</strong> This method performs only a shallow copy of the original properties.
     *
     * @return a builder for {@link ServerSideEncryptionRule}.
     */
    public Builder toBuilder() {
        var builder = new Builder();
        builder.applyServerSideEncryptionByDefault(this.applyServerSideEncryptionByDefault);
        builder.bucketKeyEnabled(this.bucketKeyEnabled);
        builder.blockedEncryptionTypes(this.blockedEncryptionTypes);
        return builder;
    }

    /**
     * @return returns a new Builder.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for {@link ServerSideEncryptionRule}.
     */
    public static final class Builder implements ShapeBuilder<ServerSideEncryptionRule> {
        private ServerSideEncryptionByDefault applyServerSideEncryptionByDefault;
        private Boolean bucketKeyEnabled;
        private BlockedEncryptionTypes blockedEncryptionTypes;

        private Builder() {}

        @Override
        public Schema schema() {
            return $SCHEMA;
        }

        /**
         * Specifies the default server-side encryption to apply to new objects in the bucket. If a PUT Object request
         * doesn't specify any server-side encryption, this default encryption will be applied.
         *
         * @return this builder.
         */
        public Builder applyServerSideEncryptionByDefault(ServerSideEncryptionByDefault applyServerSideEncryptionByDefault) {
            this.applyServerSideEncryptionByDefault = applyServerSideEncryptionByDefault;
            return this;
        }

        /**
         * Specifies whether Amazon S3 should use an S3 Bucket Key with server-side encryption using KMS (SSE-KMS) for new
         * objects in the bucket. Existing objects are not affected. Setting the <code>BucketKeyEnabled</code> element to <code>
         * true</code> causes Amazon S3 to use an S3 Bucket Key.
         *
         * <ul>
         *   <li>
         *     <b>General purpose buckets</b> - By default, S3 Bucket Key is not enabled. For more information, see <a href="https://docs.aws.amazon.com/AmazonS3/latest/dev/bucket-key.html">
         *     Amazon S3 Bucket Keys</a> in the <i>Amazon S3 User Guide</i>.
         *   </li>
         *   <li>
         *     <b>Directory buckets</b> - S3 Bucket Keys are always enabled for <code>GET</code> and <code>PUT</code>
         *     operations in a directory bucket and can’t be disabled. S3 Bucket Keys aren't supported, when you copy
         *     SSE-KMS encrypted objects from general purpose buckets to directory buckets, from directory buckets to
         *     general purpose buckets, or between directory buckets, through <a href="https://docs.aws.amazon.com/AmazonS3/latest/API/API_CopyObject.html">CopyObject</a>, <a href="https://docs.aws.amazon.com/AmazonS3/latest/API/API_UploadPartCopy.html">UploadPartCopy</a>, <a href="https://docs.aws.amazon.com/AmazonS3/latest/userguide/directory-buckets-objects-Batch-Ops">
         *     the Copy operation in Batch Operations</a>, or <a href="https://docs.aws.amazon.com/AmazonS3/latest/userguide/create-import-job">the import jobs</a>. In this case, Amazon S3 makes a
         *     call to KMS every time a copy request is made for a KMS-encrypted object.
         *   </li>
         * </ul>
         *
         * @return this builder.
         */
        public Builder bucketKeyEnabled(Boolean bucketKeyEnabled) {
            this.bucketKeyEnabled = bucketKeyEnabled;
            return this;
        }

        /**
         * A bucket-level setting for Amazon S3 general purpose buckets used to prevent the upload of new objects encrypted
         * with the specified server-side encryption type. For example, blocking an encryption type will block <code>
         * PutObject</code>, <code>CopyObject</code>, <code>PostObject</code>, multipart upload, and replication requests to
         * the bucket for objects with the specified encryption type. However, you can continue to read and list any
         * pre-existing objects already encrypted with the specified encryption type. For more information, see <a href="https://docs.aws.amazon.com/AmazonS3/latest/userguide/blocking-unblocking-s3-c-encryption-gpb.html">Blocking
         * or unblocking SSE-C for a general purpose bucket</a>.
         *
         * <p>Currently, this parameter only supports blocking or unblocking server-side encryption with customer-provided
         * keys (SSE-C). For more information about SSE-C, see <a href="https://docs.aws.amazon.com/AmazonS3/latest/userguide/ServerSideEncryptionCustomerKeys.html">Using server-side encryption with customer-provided keys
         * (SSE-C)</a>.
         *
         * @return this builder.
         */
        public Builder blockedEncryptionTypes(BlockedEncryptionTypes blockedEncryptionTypes) {
            this.blockedEncryptionTypes = blockedEncryptionTypes;
            return this;
        }

        @Override
        public ServerSideEncryptionRule build() {
            return new ServerSideEncryptionRule(this);
        }

        @Override
        @SuppressWarnings("unchecked")
        public void setMemberValue(Schema member, Object value) {
            switch (member.memberIndex()) {
                case 0 -> applyServerSideEncryptionByDefault((ServerSideEncryptionByDefault) SchemaUtils.validateSameMember($SCHEMA_APPLY_SERVER_SIDE_ENCRYPTION_BY_DEFAULT, member, value));
                case 1 -> bucketKeyEnabled((Boolean) SchemaUtils.validateSameMember($SCHEMA_BUCKET_KEY_ENABLED, member, value));
                case 2 -> blockedEncryptionTypes((BlockedEncryptionTypes) SchemaUtils.validateSameMember($SCHEMA_BLOCKED_ENCRYPTION_TYPES, member, value));
                default -> ShapeBuilder.super.setMemberValue(member, value);
            }
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
                    case 0 -> builder.applyServerSideEncryptionByDefault(ServerSideEncryptionByDefault.builder().deserializeMember(de, member).build());
                    case 1 -> builder.bucketKeyEnabled(de.readBoolean(member));
                    case 2 -> builder.blockedEncryptionTypes(BlockedEncryptionTypes.builder().deserializeMember(de, member).build());
                    default -> throw new IllegalArgumentException("Unexpected member: " + member.memberName());
                }
            }
        }
    }
}
