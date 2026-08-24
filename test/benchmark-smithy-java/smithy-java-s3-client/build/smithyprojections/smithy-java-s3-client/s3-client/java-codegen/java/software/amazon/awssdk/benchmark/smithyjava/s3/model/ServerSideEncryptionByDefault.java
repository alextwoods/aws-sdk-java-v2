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
 * Describes the default server-side encryption to apply to new objects in the bucket. If a PUT Object request doesn't
 * specify any server-side encryption, this default encryption will be applied. For more information, see <a href="https://docs.aws.amazon.com/AmazonS3/latest/API/RESTBucketPUTencryption.html">
 * PutBucketEncryption</a>.
 *
 * <ul>
 *   <li>
 *     <b>General purpose buckets</b> - If you don't specify a customer managed key at configuration, Amazon S3
 *     automatically creates an Amazon Web Services KMS key (<code>aws/s3</code>) in your Amazon Web Services
 *     account the first time that you add an object encrypted with SSE-KMS to a bucket. By default, Amazon S3 uses
 *     this KMS key for SSE-KMS.
 *   </li>
 *   <li>
 *     <b>Directory buckets</b> - Your SSE-KMS configuration can only support 1 <a href="https://docs.aws.amazon.com/kms/latest/developerguide/concepts.html#customer-cmk">customer managed key</a> per
 *     directory bucket's lifetime. The <a href="https://docs.aws.amazon.com/kms/latest/developerguide/concepts.html#aws-managed-cmk">Amazon Web Services managed key</a> (<code>aws/s3</code>) isn't
 *     supported.
 *   </li>
 *   <li>
 *     <b>Directory buckets</b> - For directory buckets, there are only two supported options for server-side
 *     encryption: SSE-S3 and SSE-KMS.
 *   </li>
 * </ul>
 */
@SmithyGenerated
public final class ServerSideEncryptionByDefault implements SerializableStruct {

    public static final Schema $SCHEMA = Schemas2.SERVER_SIDE_ENCRYPTION_BY_DEFAULT;
    private static final Schema $SCHEMA_SSE_ALGORITHM = $SCHEMA.member("SSEAlgorithm");
    private static final Schema $SCHEMA_KMS_MASTER_KEYI_D = $SCHEMA.member("KMSMasterKeyID");

    public static final ShapeId $ID = $SCHEMA.id();

    private final transient ServerSideEncryption sseAlgorithm;
    private final transient String kmsMasterKeyiD;

    private ServerSideEncryptionByDefault(Builder builder) {
        this.sseAlgorithm = builder.sseAlgorithm;
        this.kmsMasterKeyiD = builder.kmsMasterKeyiD;
    }

    /**
     * Server-side encryption algorithm to use for the default encryption.
     *
     * <p>For directory buckets, there are only two supported values for server-side encryption: <code>AES256</code> and
     * <code>aws:kms</code>.
     */
    public ServerSideEncryption getSseAlgorithm() {
        return sseAlgorithm;
    }

    /**
     * Amazon Web Services Key Management Service (KMS) customer managed key ID to use for the default encryption.
     *
     * <ul>
     *   <li>
     *     <b>General purpose buckets</b> - This parameter is allowed if and only if <code>SSEAlgorithm</code> is
     *     set to <code>aws:kms</code> or <code>aws:kms:dsse</code>.
     *   </li>
     *   <li>
     *     <b>Directory buckets</b> - This parameter is allowed if and only if <code>SSEAlgorithm</code> is set to <code>
     *     aws:kms</code>.
     *   </li>
     * </ul>
     *
     * <p>You can specify the key ID, key alias, or the Amazon Resource Name (ARN) of the KMS key.
     *
     * <ul>
     *   <li>
     *     Key ID: <code>1234abcd-12ab-34cd-56ef-1234567890ab</code>
     *   </li>
     *   <li>
     *     Key ARN: <code>arn:aws:kms:us-east-2:111122223333:key/1234abcd-12ab-34cd-56ef-1234567890ab</code>
     *   </li>
     *   <li>
     *     Key Alias: <code>alias/alias-name</code>
     *   </li>
     * </ul>
     *
     * <p>If you are using encryption with cross-account or Amazon Web Services service operations, you must use a fully
     * qualified KMS key ARN. For more information, see <a href="https://docs.aws.amazon.com/AmazonS3/latest/dev/bucket-encryption.html#bucket-encryption-update-bucket-policy">Using encryption for cross-account operations</a>.
     *
     * <ul>
     *   <li>
     *     <b>General purpose buckets</b> - If you're specifying a customer managed KMS key, we recommend using a
     *     fully qualified KMS key ARN. If you use a KMS key alias instead, then KMS resolves the key within the
     *     requester’s account. This behavior can result in data that's encrypted with a KMS key that belongs to the
     *     requester, and not the bucket owner. Also, if you use a key ID, you can run into a LogDestination
     *     undeliverable error when creating a VPC flow log.
     *   </li>
     *   <li>
     *     <b>Directory buckets</b> - When you specify an <a href="https://docs.aws.amazon.com/kms/latest/developerguide/concepts.html#customer-cmk">KMS customer managed key</a> for encryption in your
     *     directory bucket, only use the key ID or key ARN. The key alias format of the KMS key isn't supported.
     *   </li>
     * </ul>
     *
     * <p>Amazon S3 only supports symmetric encryption KMS keys. For more information, see <a href="https://docs.aws.amazon.com/kms/latest/developerguide/symmetric-asymmetric.html">Asymmetric keys in Amazon
     * Web Services KMS</a> in the <i>Amazon Web Services Key Management Service Developer Guide</i>.
     */
    public String getKmsMasterKeyiD() {
        return kmsMasterKeyiD;
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
        ServerSideEncryptionByDefault that = (ServerSideEncryptionByDefault) other;
        return Objects.equals(this.kmsMasterKeyiD, that.kmsMasterKeyiD)
               && Objects.equals(this.sseAlgorithm, that.sseAlgorithm);
    }

    @Override
    public int hashCode() {
        int $hc = Objects.hashCode(sseAlgorithm);
        $hc = 31 * $hc + Objects.hashCode(kmsMasterKeyiD);
        return $hc;
    }

    @Override
    public Schema schema() {
        return $SCHEMA;
    }

    @Override
    public void serializeMembers(ShapeSerializer serializer) {
        serializer.writeString($SCHEMA_SSE_ALGORITHM, sseAlgorithm.getValue());
        if (kmsMasterKeyiD != null) {
            serializer.writeString($SCHEMA_KMS_MASTER_KEYI_D, kmsMasterKeyiD);
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T getMemberValue(Schema member) {
        return switch (member.memberIndex()) {
            case 0 -> (T) SchemaUtils.validateSameMember($SCHEMA_SSE_ALGORITHM, member, sseAlgorithm);
            case 1 -> (T) SchemaUtils.validateSameMember($SCHEMA_KMS_MASTER_KEYI_D, member, kmsMasterKeyiD);
            default -> throw new IllegalArgumentException("Attempted to get non-existent member: " + member.id());
        };
    }

    /**
     * Create a new builder containing all the current property values of this object.
     *
     * <p><strong>Note:</strong> This method performs only a shallow copy of the original properties.
     *
     * @return a builder for {@link ServerSideEncryptionByDefault}.
     */
    public Builder toBuilder() {
        var builder = new Builder();
        builder.sseAlgorithm(this.sseAlgorithm);
        builder.kmsMasterKeyiD(this.kmsMasterKeyiD);
        return builder;
    }

    /**
     * @return returns a new Builder.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for {@link ServerSideEncryptionByDefault}.
     */
    public static final class Builder implements ShapeBuilder<ServerSideEncryptionByDefault> {
        private final PresenceTracker tracker = PresenceTracker.of($SCHEMA);
        private ServerSideEncryption sseAlgorithm;
        private String kmsMasterKeyiD;

        private Builder() {}

        @Override
        public Schema schema() {
            return $SCHEMA;
        }

        /**
         * Server-side encryption algorithm to use for the default encryption.
         *
         * <p>For directory buckets, there are only two supported values for server-side encryption: <code>AES256</code> and
         * <code>aws:kms</code>.
         *
         * <p><strong>Required</strong>
         * @return this builder.
         */
        public Builder sseAlgorithm(ServerSideEncryption sseAlgorithm) {
            this.sseAlgorithm = Objects.requireNonNull(sseAlgorithm, "sseAlgorithm cannot be null");
            tracker.setMember($SCHEMA_SSE_ALGORITHM);
            return this;
        }

        /**
         * Amazon Web Services Key Management Service (KMS) customer managed key ID to use for the default encryption.
         *
         * <ul>
         *   <li>
         *     <b>General purpose buckets</b> - This parameter is allowed if and only if <code>SSEAlgorithm</code> is
         *     set to <code>aws:kms</code> or <code>aws:kms:dsse</code>.
         *   </li>
         *   <li>
         *     <b>Directory buckets</b> - This parameter is allowed if and only if <code>SSEAlgorithm</code> is set to <code>
         *     aws:kms</code>.
         *   </li>
         * </ul>
         *
         * <p>You can specify the key ID, key alias, or the Amazon Resource Name (ARN) of the KMS key.
         *
         * <ul>
         *   <li>
         *     Key ID: <code>1234abcd-12ab-34cd-56ef-1234567890ab</code>
         *   </li>
         *   <li>
         *     Key ARN: <code>arn:aws:kms:us-east-2:111122223333:key/1234abcd-12ab-34cd-56ef-1234567890ab</code>
         *   </li>
         *   <li>
         *     Key Alias: <code>alias/alias-name</code>
         *   </li>
         * </ul>
         *
         * <p>If you are using encryption with cross-account or Amazon Web Services service operations, you must use a fully
         * qualified KMS key ARN. For more information, see <a href="https://docs.aws.amazon.com/AmazonS3/latest/dev/bucket-encryption.html#bucket-encryption-update-bucket-policy">Using encryption for cross-account operations</a>.
         *
         * <ul>
         *   <li>
         *     <b>General purpose buckets</b> - If you're specifying a customer managed KMS key, we recommend using a
         *     fully qualified KMS key ARN. If you use a KMS key alias instead, then KMS resolves the key within the
         *     requester’s account. This behavior can result in data that's encrypted with a KMS key that belongs to the
         *     requester, and not the bucket owner. Also, if you use a key ID, you can run into a LogDestination
         *     undeliverable error when creating a VPC flow log.
         *   </li>
         *   <li>
         *     <b>Directory buckets</b> - When you specify an <a href="https://docs.aws.amazon.com/kms/latest/developerguide/concepts.html#customer-cmk">KMS customer managed key</a> for encryption in your
         *     directory bucket, only use the key ID or key ARN. The key alias format of the KMS key isn't supported.
         *   </li>
         * </ul>
         *
         * <p>Amazon S3 only supports symmetric encryption KMS keys. For more information, see <a href="https://docs.aws.amazon.com/kms/latest/developerguide/symmetric-asymmetric.html">Asymmetric keys in Amazon
         * Web Services KMS</a> in the <i>Amazon Web Services Key Management Service Developer Guide</i>.
         *
         * @return this builder.
         */
        public Builder kmsMasterKeyiD(String kmsMasterKeyiD) {
            this.kmsMasterKeyiD = kmsMasterKeyiD;
            return this;
        }

        @Override
        public ServerSideEncryptionByDefault build() {
            tracker.validate();
            return new ServerSideEncryptionByDefault(this);
        }

        @Override
        @SuppressWarnings("unchecked")
        public void setMemberValue(Schema member, Object value) {
            switch (member.memberIndex()) {
                case 0 -> sseAlgorithm((ServerSideEncryption) SchemaUtils.validateSameMember($SCHEMA_SSE_ALGORITHM, member, value));
                case 1 -> kmsMasterKeyiD((String) SchemaUtils.validateSameMember($SCHEMA_KMS_MASTER_KEYI_D, member, value));
                default -> ShapeBuilder.super.setMemberValue(member, value);
            }
        }

        @Override
        public ShapeBuilder<ServerSideEncryptionByDefault> errorCorrection() {
            if (tracker.allSet()) {
                return this;
            }
            if (!tracker.checkMember($SCHEMA_SSE_ALGORITHM)) {
                sseAlgorithm(ServerSideEncryption.unknown(""));
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
                    case 0 -> builder.sseAlgorithm(ServerSideEncryption.builder().deserializeMember(de, member).build());
                    case 1 -> builder.kmsMasterKeyiD(de.readString(member));
                    default -> throw new IllegalArgumentException("Unexpected member: " + member.memberName());
                }
            }
        }
    }
}
