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

@SmithyGenerated
public final class UploadPartCopyOutput implements SerializableStruct {

    public static final Schema $SCHEMA = Schemas7.UPLOAD_PART_COPY_OUTPUT;
    private static final Schema $SCHEMA_COPY_SOURCE_VERSION_ID = $SCHEMA.member("CopySourceVersionId");
    private static final Schema $SCHEMA_COPY_PART_RESULT = $SCHEMA.member("CopyPartResult");
    private static final Schema $SCHEMA_SERVER_SIDE_ENCRYPTION = $SCHEMA.member("ServerSideEncryption");
    private static final Schema $SCHEMA_SSE_CUSTOMER_ALGORITHM = $SCHEMA.member("SSECustomerAlgorithm");
    private static final Schema $SCHEMA_SSE_CUSTOMER_KEYM_D5 = $SCHEMA.member("SSECustomerKeyMD5");
    private static final Schema $SCHEMA_SSEKMS_KEY_ID = $SCHEMA.member("SSEKMSKeyId");
    private static final Schema $SCHEMA_BUCKET_KEY_ENABLED = $SCHEMA.member("BucketKeyEnabled");
    private static final Schema $SCHEMA_REQUEST_CHARGED = $SCHEMA.member("RequestCharged");

    public static final ShapeId $ID = $SCHEMA.id();

    private final transient String copySourceVersionId;
    private final transient CopyPartResult copyPartResult;
    private final transient ServerSideEncryption serverSideEncryption;
    private final transient String sseCustomerAlgorithm;
    private final transient String sseCustomerKeymD5;
    private final transient String ssekmsKeyId;
    private final transient Boolean bucketKeyEnabled;
    private final transient RequestCharged requestCharged;

    private UploadPartCopyOutput(Builder builder) {
        this.copySourceVersionId = builder.copySourceVersionId;
        this.copyPartResult = builder.copyPartResult;
        this.serverSideEncryption = builder.serverSideEncryption;
        this.sseCustomerAlgorithm = builder.sseCustomerAlgorithm;
        this.sseCustomerKeymD5 = builder.sseCustomerKeymD5;
        this.ssekmsKeyId = builder.ssekmsKeyId;
        this.bucketKeyEnabled = builder.bucketKeyEnabled;
        this.requestCharged = builder.requestCharged;
    }

    /**
     * The version of the source object that was copied, if you have enabled versioning on the source bucket.
     *
     * <p>This functionality is not supported when the source object is in a directory bucket.
     */
    public String getCopySourceVersionId() {
        return copySourceVersionId;
    }

    /**
     * Container for all response elements.
     */
    public CopyPartResult getCopyPartResult() {
        return copyPartResult;
    }

    /**
     * The server-side encryption algorithm used when you store this object in Amazon S3 or Amazon FSx.
     *
     * <p>When accessing data stored in Amazon FSx file systems using S3 access points, the only valid server side
     * encryption option is <code>aws:fsx</code>.
     */
    public ServerSideEncryption getServerSideEncryption() {
        return serverSideEncryption;
    }

    /**
     * If server-side encryption with a customer-provided encryption key was requested, the response will include this
     * header to confirm the encryption algorithm that's used.
     *
     * <p>This functionality is not supported for directory buckets.
     */
    public String getSseCustomerAlgorithm() {
        return sseCustomerAlgorithm;
    }

    /**
     * If server-side encryption with a customer-provided encryption key was requested, the response will include this
     * header to provide the round-trip message integrity verification of the customer-provided encryption key.
     *
     * <p>This functionality is not supported for directory buckets.
     */
    public String getSseCustomerKeymD5() {
        return sseCustomerKeymD5;
    }

    /**
     * If present, indicates the ID of the KMS key that was used for object encryption.
     */
    public String getSsekmsKeyId() {
        return ssekmsKeyId;
    }

    /**
     * Indicates whether the multipart upload uses an S3 Bucket Key for server-side encryption with Key Management
     * Service (KMS) keys (SSE-KMS).
     */
    public Boolean isBucketKeyEnabled() {
        return bucketKeyEnabled;
    }

    public RequestCharged getRequestCharged() {
        return requestCharged;
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
        UploadPartCopyOutput that = (UploadPartCopyOutput) other;
        return Objects.equals(this.bucketKeyEnabled, that.bucketKeyEnabled)
               && Objects.equals(this.copySourceVersionId, that.copySourceVersionId)
               && Objects.equals(this.sseCustomerAlgorithm, that.sseCustomerAlgorithm)
               && Objects.equals(this.sseCustomerKeymD5, that.sseCustomerKeymD5)
               && Objects.equals(this.ssekmsKeyId, that.ssekmsKeyId)
               && Objects.equals(this.serverSideEncryption, that.serverSideEncryption)
               && Objects.equals(this.requestCharged, that.requestCharged)
               && Objects.equals(this.copyPartResult, that.copyPartResult);
    }

    @Override
    public int hashCode() {
        int $hc = Objects.hashCode(copySourceVersionId);
        $hc = 31 * $hc + Objects.hashCode(copyPartResult);
        $hc = 31 * $hc + Objects.hashCode(serverSideEncryption);
        $hc = 31 * $hc + Objects.hashCode(sseCustomerAlgorithm);
        $hc = 31 * $hc + Objects.hashCode(sseCustomerKeymD5);
        $hc = 31 * $hc + Objects.hashCode(ssekmsKeyId);
        $hc = 31 * $hc + Objects.hashCode(bucketKeyEnabled);
        $hc = 31 * $hc + Objects.hashCode(requestCharged);
        return $hc;
    }

    @Override
    public Schema schema() {
        return $SCHEMA;
    }

    @Override
    public void serializeMembers(ShapeSerializer serializer) {
        if (copySourceVersionId != null) {
            serializer.writeString($SCHEMA_COPY_SOURCE_VERSION_ID, copySourceVersionId);
        }
        if (copyPartResult != null) {
            serializer.writeStruct($SCHEMA_COPY_PART_RESULT, copyPartResult);
        }
        if (serverSideEncryption != null) {
            serializer.writeString($SCHEMA_SERVER_SIDE_ENCRYPTION, serverSideEncryption.getValue());
        }
        if (sseCustomerAlgorithm != null) {
            serializer.writeString($SCHEMA_SSE_CUSTOMER_ALGORITHM, sseCustomerAlgorithm);
        }
        if (sseCustomerKeymD5 != null) {
            serializer.writeString($SCHEMA_SSE_CUSTOMER_KEYM_D5, sseCustomerKeymD5);
        }
        if (ssekmsKeyId != null) {
            serializer.writeString($SCHEMA_SSEKMS_KEY_ID, ssekmsKeyId);
        }
        if (bucketKeyEnabled != null) {
            serializer.writeBoolean($SCHEMA_BUCKET_KEY_ENABLED, bucketKeyEnabled);
        }
        if (requestCharged != null) {
            serializer.writeString($SCHEMA_REQUEST_CHARGED, requestCharged.getValue());
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T getMemberValue(Schema member) {
        return switch (member.memberIndex()) {
            case 0 -> (T) SchemaUtils.validateSameMember($SCHEMA_COPY_SOURCE_VERSION_ID, member, copySourceVersionId);
            case 1 -> (T) SchemaUtils.validateSameMember($SCHEMA_COPY_PART_RESULT, member, copyPartResult);
            case 2 -> (T) SchemaUtils.validateSameMember($SCHEMA_SERVER_SIDE_ENCRYPTION, member, serverSideEncryption);
            case 3 -> (T) SchemaUtils.validateSameMember($SCHEMA_SSE_CUSTOMER_ALGORITHM, member, sseCustomerAlgorithm);
            case 4 -> (T) SchemaUtils.validateSameMember($SCHEMA_SSE_CUSTOMER_KEYM_D5, member, sseCustomerKeymD5);
            case 5 -> (T) SchemaUtils.validateSameMember($SCHEMA_SSEKMS_KEY_ID, member, ssekmsKeyId);
            case 6 -> (T) SchemaUtils.validateSameMember($SCHEMA_BUCKET_KEY_ENABLED, member, bucketKeyEnabled);
            case 7 -> (T) SchemaUtils.validateSameMember($SCHEMA_REQUEST_CHARGED, member, requestCharged);
            default -> throw new IllegalArgumentException("Attempted to get non-existent member: " + member.id());
        };
    }

    /**
     * Create a new builder containing all the current property values of this object.
     *
     * <p><strong>Note:</strong> This method performs only a shallow copy of the original properties.
     *
     * @return a builder for {@link UploadPartCopyOutput}.
     */
    public Builder toBuilder() {
        var builder = new Builder();
        builder.copySourceVersionId(this.copySourceVersionId);
        builder.copyPartResult(this.copyPartResult);
        builder.serverSideEncryption(this.serverSideEncryption);
        builder.sseCustomerAlgorithm(this.sseCustomerAlgorithm);
        builder.sseCustomerKeymD5(this.sseCustomerKeymD5);
        builder.ssekmsKeyId(this.ssekmsKeyId);
        builder.bucketKeyEnabled(this.bucketKeyEnabled);
        builder.requestCharged(this.requestCharged);
        return builder;
    }

    /**
     * @return returns a new Builder.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for {@link UploadPartCopyOutput}.
     */
    public static final class Builder implements ShapeBuilder<UploadPartCopyOutput> {
        private String copySourceVersionId;
        private CopyPartResult copyPartResult;
        private ServerSideEncryption serverSideEncryption;
        private String sseCustomerAlgorithm;
        private String sseCustomerKeymD5;
        private String ssekmsKeyId;
        private Boolean bucketKeyEnabled;
        private RequestCharged requestCharged;

        private Builder() {}

        @Override
        public Schema schema() {
            return $SCHEMA;
        }

        /**
         * The version of the source object that was copied, if you have enabled versioning on the source bucket.
         *
         * <p>This functionality is not supported when the source object is in a directory bucket.
         *
         * @return this builder.
         */
        public Builder copySourceVersionId(String copySourceVersionId) {
            this.copySourceVersionId = copySourceVersionId;
            return this;
        }

        /**
         * Container for all response elements.
         *
         * @return this builder.
         */
        public Builder copyPartResult(CopyPartResult copyPartResult) {
            this.copyPartResult = copyPartResult;
            return this;
        }

        /**
         * The server-side encryption algorithm used when you store this object in Amazon S3 or Amazon FSx.
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
         * If server-side encryption with a customer-provided encryption key was requested, the response will include this
         * header to confirm the encryption algorithm that's used.
         *
         * <p>This functionality is not supported for directory buckets.
         *
         * @return this builder.
         */
        public Builder sseCustomerAlgorithm(String sseCustomerAlgorithm) {
            this.sseCustomerAlgorithm = sseCustomerAlgorithm;
            return this;
        }

        /**
         * If server-side encryption with a customer-provided encryption key was requested, the response will include this
         * header to provide the round-trip message integrity verification of the customer-provided encryption key.
         *
         * <p>This functionality is not supported for directory buckets.
         *
         * @return this builder.
         */
        public Builder sseCustomerKeymD5(String sseCustomerKeymD5) {
            this.sseCustomerKeymD5 = sseCustomerKeymD5;
            return this;
        }

        /**
         * If present, indicates the ID of the KMS key that was used for object encryption.
         *
         * @return this builder.
         */
        public Builder ssekmsKeyId(String ssekmsKeyId) {
            this.ssekmsKeyId = ssekmsKeyId;
            return this;
        }

        /**
         * Indicates whether the multipart upload uses an S3 Bucket Key for server-side encryption with Key Management
         * Service (KMS) keys (SSE-KMS).
         *
         * @return this builder.
         */
        public Builder bucketKeyEnabled(Boolean bucketKeyEnabled) {
            this.bucketKeyEnabled = bucketKeyEnabled;
            return this;
        }

        /**
         * @return this builder.
         */
        public Builder requestCharged(RequestCharged requestCharged) {
            this.requestCharged = requestCharged;
            return this;
        }

        @Override
        public UploadPartCopyOutput build() {
            return new UploadPartCopyOutput(this);
        }

        @Override
        @SuppressWarnings("unchecked")
        public void setMemberValue(Schema member, Object value) {
            switch (member.memberIndex()) {
                case 0 -> copySourceVersionId((String) SchemaUtils.validateSameMember($SCHEMA_COPY_SOURCE_VERSION_ID, member, value));
                case 1 -> copyPartResult((CopyPartResult) SchemaUtils.validateSameMember($SCHEMA_COPY_PART_RESULT, member, value));
                case 2 -> serverSideEncryption((ServerSideEncryption) SchemaUtils.validateSameMember($SCHEMA_SERVER_SIDE_ENCRYPTION, member, value));
                case 3 -> sseCustomerAlgorithm((String) SchemaUtils.validateSameMember($SCHEMA_SSE_CUSTOMER_ALGORITHM, member, value));
                case 4 -> sseCustomerKeymD5((String) SchemaUtils.validateSameMember($SCHEMA_SSE_CUSTOMER_KEYM_D5, member, value));
                case 5 -> ssekmsKeyId((String) SchemaUtils.validateSameMember($SCHEMA_SSEKMS_KEY_ID, member, value));
                case 6 -> bucketKeyEnabled((Boolean) SchemaUtils.validateSameMember($SCHEMA_BUCKET_KEY_ENABLED, member, value));
                case 7 -> requestCharged((RequestCharged) SchemaUtils.validateSameMember($SCHEMA_REQUEST_CHARGED, member, value));
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
                    case 0 -> builder.copySourceVersionId(de.readString(member));
                    case 1 -> builder.copyPartResult(CopyPartResult.builder().deserializeMember(de, member).build());
                    case 2 -> builder.serverSideEncryption(ServerSideEncryption.builder().deserializeMember(de, member).build());
                    case 3 -> builder.sseCustomerAlgorithm(de.readString(member));
                    case 4 -> builder.sseCustomerKeymD5(de.readString(member));
                    case 5 -> builder.ssekmsKeyId(de.readString(member));
                    case 6 -> builder.bucketKeyEnabled(de.readBoolean(member));
                    case 7 -> builder.requestCharged(RequestCharged.builder().deserializeMember(de, member).build());
                    default -> throw new IllegalArgumentException("Unexpected member: " + member.memberName());
                }
            }
        }
    }
}
