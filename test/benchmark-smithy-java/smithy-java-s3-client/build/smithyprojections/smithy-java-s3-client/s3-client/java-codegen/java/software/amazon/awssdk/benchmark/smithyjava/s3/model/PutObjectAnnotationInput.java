package software.amazon.awssdk.benchmark.smithyjava.s3.model;

import java.io.Closeable;
import java.util.Objects;
import software.amazon.smithy.java.core.schema.PresenceTracker;
import software.amazon.smithy.java.core.schema.Schema;
import software.amazon.smithy.java.core.schema.SchemaUtils;
import software.amazon.smithy.java.core.schema.SerializableStruct;
import software.amazon.smithy.java.core.schema.ShapeBuilder;
import software.amazon.smithy.java.core.serde.ShapeDeserializer;
import software.amazon.smithy.java.core.serde.ShapeSerializer;
import software.amazon.smithy.java.core.serde.ToStringSerializer;
import software.amazon.smithy.java.io.datastream.DataStream;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.utils.SmithyGenerated;

@SmithyGenerated
public final class PutObjectAnnotationInput implements SerializableStruct, Closeable {

    public static final Schema $SCHEMA = Schemas6.PUT_OBJECT_ANNOTATION_INPUT;
    private static final Schema $SCHEMA_BUCKET = $SCHEMA.member("Bucket");
    private static final Schema $SCHEMA_KEY = $SCHEMA.member("Key");
    private static final Schema $SCHEMA_VERSION_ID = $SCHEMA.member("VersionId");
    private static final Schema $SCHEMA_ANNOTATION_NAME = $SCHEMA.member("AnnotationName");
    private static final Schema $SCHEMA_ANNOTATION_PAYLOAD = $SCHEMA.member("AnnotationPayload");
    private static final Schema $SCHEMA_OBJECT_IF_MATCH = $SCHEMA.member("ObjectIfMatch");
    private static final Schema $SCHEMA_CHECKSUM_ALGORITHM = $SCHEMA.member("ChecksumAlgorithm");
    private static final Schema $SCHEMA_CHECKSUMCR_C32 = $SCHEMA.member("ChecksumCRC32");
    private static final Schema $SCHEMA_CHECKSUMCR_C32_C = $SCHEMA.member("ChecksumCRC32C");
    private static final Schema $SCHEMA_CHECKSUMCR_C64NVM_E = $SCHEMA.member("ChecksumCRC64NVME");
    private static final Schema $SCHEMA_CHECKSUMSH_A1 = $SCHEMA.member("ChecksumSHA1");
    private static final Schema $SCHEMA_CHECKSUMSH_A256 = $SCHEMA.member("ChecksumSHA256");
    private static final Schema $SCHEMA_CHECKSUMSH_A512 = $SCHEMA.member("ChecksumSHA512");
    private static final Schema $SCHEMA_CHECKSUMM_D5 = $SCHEMA.member("ChecksumMD5");
    private static final Schema $SCHEMA_CHECKSUMXXHAS_H64 = $SCHEMA.member("ChecksumXXHASH64");
    private static final Schema $SCHEMA_CHECKSUMXXHAS_H3 = $SCHEMA.member("ChecksumXXHASH3");
    private static final Schema $SCHEMA_CHECKSUMXXHAS_H128 = $SCHEMA.member("ChecksumXXHASH128");
    private static final Schema $SCHEMA_CONTENTM_D5 = $SCHEMA.member("ContentMD5");
    private static final Schema $SCHEMA_REQUEST_PAYER = $SCHEMA.member("RequestPayer");
    private static final Schema $SCHEMA_EXPECTED_BUCKET_OWNER = $SCHEMA.member("ExpectedBucketOwner");

    public static final ShapeId $ID = $SCHEMA.id();

    private final transient String bucket;
    private final transient String key;
    private final transient String versionId;
    private final transient String annotationName;
    private final transient DataStream annotationPayload;
    private final transient String objectIfMatch;
    private final transient ChecksumAlgorithm checksumAlgorithm;
    private final transient String checksumcrC32;
    private final transient String checksumcrC32C;
    private final transient String checksumcrC64nvmE;
    private final transient String checksumshA1;
    private final transient String checksumshA256;
    private final transient String checksumshA512;
    private final transient String checksummD5;
    private final transient String checksumxxhasH64;
    private final transient String checksumxxhasH3;
    private final transient String checksumxxhasH128;
    private final transient String contentmD5;
    private final transient RequestPayer requestPayer;
    private final transient String expectedBucketOwner;

    private PutObjectAnnotationInput(Builder builder) {
        this.bucket = builder.bucket;
        this.key = builder.key;
        this.versionId = builder.versionId;
        this.annotationName = builder.annotationName;
        this.annotationPayload = builder.annotationPayload;
        this.objectIfMatch = builder.objectIfMatch;
        this.checksumAlgorithm = builder.checksumAlgorithm;
        this.checksumcrC32 = builder.checksumcrC32;
        this.checksumcrC32C = builder.checksumcrC32C;
        this.checksumcrC64nvmE = builder.checksumcrC64nvmE;
        this.checksumshA1 = builder.checksumshA1;
        this.checksumshA256 = builder.checksumshA256;
        this.checksumshA512 = builder.checksumshA512;
        this.checksummD5 = builder.checksummD5;
        this.checksumxxhasH64 = builder.checksumxxhasH64;
        this.checksumxxhasH3 = builder.checksumxxhasH3;
        this.checksumxxhasH128 = builder.checksumxxhasH128;
        this.contentmD5 = builder.contentmD5;
        this.requestPayer = builder.requestPayer;
        this.expectedBucketOwner = builder.expectedBucketOwner;
    }

    /**
     * The name of the bucket that contains the object.
     */
    public String getBucket() {
        return bucket;
    }

    /**
     * The object key.
     */
    public String getKey() {
        return key;
    }

    /**
     * The version ID of the object to attach the annotation to.
     */
    public String getVersionId() {
        return versionId;
    }

    /**
     * The name of the annotation.
     *
     * <p>Length Constraints: Minimum length of 1. Maximum length of 512 bytes.
     */
    public String getAnnotationName() {
        return annotationName;
    }

    /**
     * The annotation payload. Must be between 1 byte and 1 MiB in size, and must be valid UTF-8 encoded text. If the
     * payload contains invalid UTF-8 bytes, the request fails with HTTP 415 (Unsupported Media Type). To store binary
     * data, encode the payload using Base64 before uploading.
     */
    public DataStream getAnnotationPayload() {
        return annotationPayload;
    }

    /**
     * If specified, the operation only succeeds if the object's ETag matches the provided value.
     */
    public String getObjectIfMatch() {
        return objectIfMatch;
    }

    /**
     * The checksum algorithm to use. Supported values: <code>CRC32</code>, <code>CRC32C</code>, <code>CRC64NVME</code>,
     * <code>SHA1</code>, <code>SHA256</code>, <code>SHA512</code>, <code>MD5</code>, <code>XXHASH64</code>,
     * <code>XXHASH3</code>, <code>XXHASH128</code>.
     */
    public ChecksumAlgorithm getChecksumAlgorithm() {
        return checksumAlgorithm;
    }

    /**
     * Base64-encoded CRC32 checksum of the annotation payload.
     */
    public String getChecksumcrC32() {
        return checksumcrC32;
    }

    /**
     * Base64-encoded CRC32C checksum of the annotation payload.
     */
    public String getChecksumcrC32C() {
        return checksumcrC32C;
    }

    /**
     * Base64-encoded CRC64NVME checksum of the annotation payload.
     */
    public String getChecksumcrC64nvmE() {
        return checksumcrC64nvmE;
    }

    /**
     * Base64-encoded SHA1 checksum of the annotation payload.
     */
    public String getChecksumshA1() {
        return checksumshA1;
    }

    /**
     * Base64-encoded SHA256 checksum of the annotation payload.
     */
    public String getChecksumshA256() {
        return checksumshA256;
    }

    /**
     * Base64-encoded SHA512 checksum of the annotation payload.
     */
    public String getChecksumshA512() {
        return checksumshA512;
    }

    /**
     * Base64-encoded MD5 checksum of the annotation payload.
     */
    public String getChecksummD5() {
        return checksummD5;
    }

    /**
     * Base64-encoded XXHASH64 checksum of the annotation payload.
     */
    public String getChecksumxxhasH64() {
        return checksumxxhasH64;
    }

    /**
     * Base64-encoded XXHASH3 checksum of the annotation payload.
     */
    public String getChecksumxxhasH3() {
        return checksumxxhasH3;
    }

    /**
     * Base64-encoded XXHASH128 checksum of the annotation payload.
     */
    public String getChecksumxxhasH128() {
        return checksumxxhasH128;
    }

    /**
     * Base64-encoded MD5 digest of the message.
     */
    public String getContentmD5() {
        return contentmD5;
    }

    public RequestPayer getRequestPayer() {
        return requestPayer;
    }

    /**
     * The account ID of the expected bucket owner. If the bucket is owned by a different account, the request fails
     * with an HTTP 403 (Access Denied) error.
     */
    public String getExpectedBucketOwner() {
        return expectedBucketOwner;
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
        PutObjectAnnotationInput that = (PutObjectAnnotationInput) other;
        return Objects.equals(this.bucket, that.bucket)
               && Objects.equals(this.key, that.key)
               && Objects.equals(this.versionId, that.versionId)
               && Objects.equals(this.annotationName, that.annotationName)
               && Objects.equals(this.objectIfMatch, that.objectIfMatch)
               && Objects.equals(this.checksumcrC32, that.checksumcrC32)
               && Objects.equals(this.checksumcrC32C, that.checksumcrC32C)
               && Objects.equals(this.checksumcrC64nvmE, that.checksumcrC64nvmE)
               && Objects.equals(this.checksumshA1, that.checksumshA1)
               && Objects.equals(this.checksumshA256, that.checksumshA256)
               && Objects.equals(this.checksumshA512, that.checksumshA512)
               && Objects.equals(this.checksummD5, that.checksummD5)
               && Objects.equals(this.checksumxxhasH64, that.checksumxxhasH64)
               && Objects.equals(this.checksumxxhasH3, that.checksumxxhasH3)
               && Objects.equals(this.checksumxxhasH128, that.checksumxxhasH128)
               && Objects.equals(this.contentmD5, that.contentmD5)
               && Objects.equals(this.expectedBucketOwner, that.expectedBucketOwner)
               && Objects.equals(this.checksumAlgorithm, that.checksumAlgorithm)
               && Objects.equals(this.requestPayer, that.requestPayer)
               && Objects.equals(this.annotationPayload, that.annotationPayload);
    }

    @Override
    public int hashCode() {
        int $hc = Objects.hashCode(bucket);
        $hc = 31 * $hc + Objects.hashCode(key);
        $hc = 31 * $hc + Objects.hashCode(versionId);
        $hc = 31 * $hc + Objects.hashCode(annotationName);
        $hc = 31 * $hc + Objects.hashCode(annotationPayload);
        $hc = 31 * $hc + Objects.hashCode(objectIfMatch);
        $hc = 31 * $hc + Objects.hashCode(checksumAlgorithm);
        $hc = 31 * $hc + Objects.hashCode(checksumcrC32);
        $hc = 31 * $hc + Objects.hashCode(checksumcrC32C);
        $hc = 31 * $hc + Objects.hashCode(checksumcrC64nvmE);
        $hc = 31 * $hc + Objects.hashCode(checksumshA1);
        $hc = 31 * $hc + Objects.hashCode(checksumshA256);
        $hc = 31 * $hc + Objects.hashCode(checksumshA512);
        $hc = 31 * $hc + Objects.hashCode(checksummD5);
        $hc = 31 * $hc + Objects.hashCode(checksumxxhasH64);
        $hc = 31 * $hc + Objects.hashCode(checksumxxhasH3);
        $hc = 31 * $hc + Objects.hashCode(checksumxxhasH128);
        $hc = 31 * $hc + Objects.hashCode(contentmD5);
        $hc = 31 * $hc + Objects.hashCode(requestPayer);
        $hc = 31 * $hc + Objects.hashCode(expectedBucketOwner);
        return $hc;
    }

    @Override
    public Schema schema() {
        return $SCHEMA;
    }

    @Override
    public void serializeMembers(ShapeSerializer serializer) {
        serializer.writeString($SCHEMA_BUCKET, bucket);
        serializer.writeString($SCHEMA_KEY, key);
        if (versionId != null) {
            serializer.writeString($SCHEMA_VERSION_ID, versionId);
        }
        serializer.writeString($SCHEMA_ANNOTATION_NAME, annotationName);
        serializer.writeDataStream($SCHEMA_ANNOTATION_PAYLOAD, annotationPayload);
        if (objectIfMatch != null) {
            serializer.writeString($SCHEMA_OBJECT_IF_MATCH, objectIfMatch);
        }
        if (checksumAlgorithm != null) {
            serializer.writeString($SCHEMA_CHECKSUM_ALGORITHM, checksumAlgorithm.getValue());
        }
        if (checksumcrC32 != null) {
            serializer.writeString($SCHEMA_CHECKSUMCR_C32, checksumcrC32);
        }
        if (checksumcrC32C != null) {
            serializer.writeString($SCHEMA_CHECKSUMCR_C32_C, checksumcrC32C);
        }
        if (checksumcrC64nvmE != null) {
            serializer.writeString($SCHEMA_CHECKSUMCR_C64NVM_E, checksumcrC64nvmE);
        }
        if (checksumshA1 != null) {
            serializer.writeString($SCHEMA_CHECKSUMSH_A1, checksumshA1);
        }
        if (checksumshA256 != null) {
            serializer.writeString($SCHEMA_CHECKSUMSH_A256, checksumshA256);
        }
        if (checksumshA512 != null) {
            serializer.writeString($SCHEMA_CHECKSUMSH_A512, checksumshA512);
        }
        if (checksummD5 != null) {
            serializer.writeString($SCHEMA_CHECKSUMM_D5, checksummD5);
        }
        if (checksumxxhasH64 != null) {
            serializer.writeString($SCHEMA_CHECKSUMXXHAS_H64, checksumxxhasH64);
        }
        if (checksumxxhasH3 != null) {
            serializer.writeString($SCHEMA_CHECKSUMXXHAS_H3, checksumxxhasH3);
        }
        if (checksumxxhasH128 != null) {
            serializer.writeString($SCHEMA_CHECKSUMXXHAS_H128, checksumxxhasH128);
        }
        if (contentmD5 != null) {
            serializer.writeString($SCHEMA_CONTENTM_D5, contentmD5);
        }
        if (requestPayer != null) {
            serializer.writeString($SCHEMA_REQUEST_PAYER, requestPayer.getValue());
        }
        if (expectedBucketOwner != null) {
            serializer.writeString($SCHEMA_EXPECTED_BUCKET_OWNER, expectedBucketOwner);
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T getMemberValue(Schema member) {
        return switch (member.memberIndex()) {
            case 0 -> (T) SchemaUtils.validateSameMember($SCHEMA_BUCKET, member, bucket);
            case 1 -> (T) SchemaUtils.validateSameMember($SCHEMA_KEY, member, key);
            case 2 -> (T) SchemaUtils.validateSameMember($SCHEMA_ANNOTATION_NAME, member, annotationName);
            case 3 -> (T) SchemaUtils.validateSameMember($SCHEMA_ANNOTATION_PAYLOAD, member, annotationPayload);
            case 4 -> (T) SchemaUtils.validateSameMember($SCHEMA_VERSION_ID, member, versionId);
            case 5 -> (T) SchemaUtils.validateSameMember($SCHEMA_OBJECT_IF_MATCH, member, objectIfMatch);
            case 6 -> (T) SchemaUtils.validateSameMember($SCHEMA_CHECKSUM_ALGORITHM, member, checksumAlgorithm);
            case 7 -> (T) SchemaUtils.validateSameMember($SCHEMA_CHECKSUMCR_C32, member, checksumcrC32);
            case 8 -> (T) SchemaUtils.validateSameMember($SCHEMA_CHECKSUMCR_C32_C, member, checksumcrC32C);
            case 9 -> (T) SchemaUtils.validateSameMember($SCHEMA_CHECKSUMCR_C64NVM_E, member, checksumcrC64nvmE);
            case 10 -> (T) SchemaUtils.validateSameMember($SCHEMA_CHECKSUMSH_A1, member, checksumshA1);
            case 11 -> (T) SchemaUtils.validateSameMember($SCHEMA_CHECKSUMSH_A256, member, checksumshA256);
            case 12 -> (T) SchemaUtils.validateSameMember($SCHEMA_CHECKSUMSH_A512, member, checksumshA512);
            case 13 -> (T) SchemaUtils.validateSameMember($SCHEMA_CHECKSUMM_D5, member, checksummD5);
            case 14 -> (T) SchemaUtils.validateSameMember($SCHEMA_CHECKSUMXXHAS_H64, member, checksumxxhasH64);
            case 15 -> (T) SchemaUtils.validateSameMember($SCHEMA_CHECKSUMXXHAS_H3, member, checksumxxhasH3);
            case 16 -> (T) SchemaUtils.validateSameMember($SCHEMA_CHECKSUMXXHAS_H128, member, checksumxxhasH128);
            case 17 -> (T) SchemaUtils.validateSameMember($SCHEMA_CONTENTM_D5, member, contentmD5);
            case 18 -> (T) SchemaUtils.validateSameMember($SCHEMA_REQUEST_PAYER, member, requestPayer);
            case 19 -> (T) SchemaUtils.validateSameMember($SCHEMA_EXPECTED_BUCKET_OWNER, member, expectedBucketOwner);
            default -> throw new IllegalArgumentException("Attempted to get non-existent member: " + member.id());
        };
    }

    /**
     * Closes the underlying stream.
     */
    @Override
    public void close() {
        if (annotationPayload != null) {
            annotationPayload.close();
        }
    }

    /**
     * Create a new builder containing all the current property values of this object.
     *
     * <p><strong>Note:</strong> This method performs only a shallow copy of the original properties.
     *
     * @return a builder for {@link PutObjectAnnotationInput}.
     */
    public Builder toBuilder() {
        var builder = new Builder();
        builder.bucket(this.bucket);
        builder.key(this.key);
        builder.versionId(this.versionId);
        builder.annotationName(this.annotationName);
        builder.annotationPayload(this.annotationPayload);
        builder.objectIfMatch(this.objectIfMatch);
        builder.checksumAlgorithm(this.checksumAlgorithm);
        builder.checksumcrC32(this.checksumcrC32);
        builder.checksumcrC32C(this.checksumcrC32C);
        builder.checksumcrC64nvmE(this.checksumcrC64nvmE);
        builder.checksumshA1(this.checksumshA1);
        builder.checksumshA256(this.checksumshA256);
        builder.checksumshA512(this.checksumshA512);
        builder.checksummD5(this.checksummD5);
        builder.checksumxxhasH64(this.checksumxxhasH64);
        builder.checksumxxhasH3(this.checksumxxhasH3);
        builder.checksumxxhasH128(this.checksumxxhasH128);
        builder.contentmD5(this.contentmD5);
        builder.requestPayer(this.requestPayer);
        builder.expectedBucketOwner(this.expectedBucketOwner);
        return builder;
    }

    /**
     * @return returns a new Builder.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for {@link PutObjectAnnotationInput}.
     */
    public static final class Builder implements ShapeBuilder<PutObjectAnnotationInput> {
        private final PresenceTracker tracker = PresenceTracker.of($SCHEMA);
        private String bucket;
        private String key;
        private String versionId;
        private String annotationName;
        private DataStream annotationPayload;
        private String objectIfMatch;
        private ChecksumAlgorithm checksumAlgorithm;
        private String checksumcrC32;
        private String checksumcrC32C;
        private String checksumcrC64nvmE;
        private String checksumshA1;
        private String checksumshA256;
        private String checksumshA512;
        private String checksummD5;
        private String checksumxxhasH64;
        private String checksumxxhasH3;
        private String checksumxxhasH128;
        private String contentmD5;
        private RequestPayer requestPayer;
        private String expectedBucketOwner;

        private Builder() {}

        @Override
        public Schema schema() {
            return $SCHEMA;
        }

        /**
         * The name of the bucket that contains the object.
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
         * The object key.
         *
         * <p><strong>Required</strong>
         * @return this builder.
         */
        public Builder key(String key) {
            this.key = Objects.requireNonNull(key, "key cannot be null");
            tracker.setMember($SCHEMA_KEY);
            return this;
        }

        /**
         * The version ID of the object to attach the annotation to.
         *
         * @return this builder.
         */
        public Builder versionId(String versionId) {
            this.versionId = versionId;
            return this;
        }

        /**
         * The name of the annotation.
         *
         * <p>Length Constraints: Minimum length of 1. Maximum length of 512 bytes.
         *
         * <p><strong>Required</strong>
         * @return this builder.
         */
        public Builder annotationName(String annotationName) {
            this.annotationName = Objects.requireNonNull(annotationName, "annotationName cannot be null");
            tracker.setMember($SCHEMA_ANNOTATION_NAME);
            return this;
        }

        /**
         * The annotation payload. Must be between 1 byte and 1 MiB in size, and must be valid UTF-8 encoded text. If the
         * payload contains invalid UTF-8 bytes, the request fails with HTTP 415 (Unsupported Media Type). To store binary
         * data, encode the payload using Base64 before uploading.
         *
         * <p><strong>Required</strong>
         * @return this builder.
         */
        public Builder annotationPayload(DataStream annotationPayload) {
            this.annotationPayload = Objects.requireNonNull(annotationPayload, "annotationPayload cannot be null");
            tracker.setMember($SCHEMA_ANNOTATION_PAYLOAD);
            return this;
        }

        /**
         * If specified, the operation only succeeds if the object's ETag matches the provided value.
         *
         * @return this builder.
         */
        public Builder objectIfMatch(String objectIfMatch) {
            this.objectIfMatch = objectIfMatch;
            return this;
        }

        /**
         * The checksum algorithm to use. Supported values: <code>CRC32</code>, <code>CRC32C</code>, <code>CRC64NVME</code>,
         * <code>SHA1</code>, <code>SHA256</code>, <code>SHA512</code>, <code>MD5</code>, <code>XXHASH64</code>,
         * <code>XXHASH3</code>, <code>XXHASH128</code>.
         *
         * @return this builder.
         */
        public Builder checksumAlgorithm(ChecksumAlgorithm checksumAlgorithm) {
            this.checksumAlgorithm = checksumAlgorithm;
            return this;
        }

        /**
         * Base64-encoded CRC32 checksum of the annotation payload.
         *
         * @return this builder.
         */
        public Builder checksumcrC32(String checksumcrC32) {
            this.checksumcrC32 = checksumcrC32;
            return this;
        }

        /**
         * Base64-encoded CRC32C checksum of the annotation payload.
         *
         * @return this builder.
         */
        public Builder checksumcrC32C(String checksumcrC32C) {
            this.checksumcrC32C = checksumcrC32C;
            return this;
        }

        /**
         * Base64-encoded CRC64NVME checksum of the annotation payload.
         *
         * @return this builder.
         */
        public Builder checksumcrC64nvmE(String checksumcrC64nvmE) {
            this.checksumcrC64nvmE = checksumcrC64nvmE;
            return this;
        }

        /**
         * Base64-encoded SHA1 checksum of the annotation payload.
         *
         * @return this builder.
         */
        public Builder checksumshA1(String checksumshA1) {
            this.checksumshA1 = checksumshA1;
            return this;
        }

        /**
         * Base64-encoded SHA256 checksum of the annotation payload.
         *
         * @return this builder.
         */
        public Builder checksumshA256(String checksumshA256) {
            this.checksumshA256 = checksumshA256;
            return this;
        }

        /**
         * Base64-encoded SHA512 checksum of the annotation payload.
         *
         * @return this builder.
         */
        public Builder checksumshA512(String checksumshA512) {
            this.checksumshA512 = checksumshA512;
            return this;
        }

        /**
         * Base64-encoded MD5 checksum of the annotation payload.
         *
         * @return this builder.
         */
        public Builder checksummD5(String checksummD5) {
            this.checksummD5 = checksummD5;
            return this;
        }

        /**
         * Base64-encoded XXHASH64 checksum of the annotation payload.
         *
         * @return this builder.
         */
        public Builder checksumxxhasH64(String checksumxxhasH64) {
            this.checksumxxhasH64 = checksumxxhasH64;
            return this;
        }

        /**
         * Base64-encoded XXHASH3 checksum of the annotation payload.
         *
         * @return this builder.
         */
        public Builder checksumxxhasH3(String checksumxxhasH3) {
            this.checksumxxhasH3 = checksumxxhasH3;
            return this;
        }

        /**
         * Base64-encoded XXHASH128 checksum of the annotation payload.
         *
         * @return this builder.
         */
        public Builder checksumxxhasH128(String checksumxxhasH128) {
            this.checksumxxhasH128 = checksumxxhasH128;
            return this;
        }

        /**
         * Base64-encoded MD5 digest of the message.
         *
         * @return this builder.
         */
        public Builder contentmD5(String contentmD5) {
            this.contentmD5 = contentmD5;
            return this;
        }

        /**
         * @return this builder.
         */
        public Builder requestPayer(RequestPayer requestPayer) {
            this.requestPayer = requestPayer;
            return this;
        }

        /**
         * The account ID of the expected bucket owner. If the bucket is owned by a different account, the request fails
         * with an HTTP 403 (Access Denied) error.
         *
         * @return this builder.
         */
        public Builder expectedBucketOwner(String expectedBucketOwner) {
            this.expectedBucketOwner = expectedBucketOwner;
            return this;
        }

        @Override
        public PutObjectAnnotationInput build() {
            tracker.validate();
            return new PutObjectAnnotationInput(this);
        }

        @Override
        @SuppressWarnings("unchecked")
        public void setMemberValue(Schema member, Object value) {
            switch (member.memberIndex()) {
                case 0 -> bucket((String) SchemaUtils.validateSameMember($SCHEMA_BUCKET, member, value));
                case 1 -> key((String) SchemaUtils.validateSameMember($SCHEMA_KEY, member, value));
                case 2 -> annotationName((String) SchemaUtils.validateSameMember($SCHEMA_ANNOTATION_NAME, member, value));
                case 3 -> annotationPayload((DataStream) SchemaUtils.validateSameMember($SCHEMA_ANNOTATION_PAYLOAD, member, value));
                case 4 -> versionId((String) SchemaUtils.validateSameMember($SCHEMA_VERSION_ID, member, value));
                case 5 -> objectIfMatch((String) SchemaUtils.validateSameMember($SCHEMA_OBJECT_IF_MATCH, member, value));
                case 6 -> checksumAlgorithm((ChecksumAlgorithm) SchemaUtils.validateSameMember($SCHEMA_CHECKSUM_ALGORITHM, member, value));
                case 7 -> checksumcrC32((String) SchemaUtils.validateSameMember($SCHEMA_CHECKSUMCR_C32, member, value));
                case 8 -> checksumcrC32C((String) SchemaUtils.validateSameMember($SCHEMA_CHECKSUMCR_C32_C, member, value));
                case 9 -> checksumcrC64nvmE((String) SchemaUtils.validateSameMember($SCHEMA_CHECKSUMCR_C64NVM_E, member, value));
                case 10 -> checksumshA1((String) SchemaUtils.validateSameMember($SCHEMA_CHECKSUMSH_A1, member, value));
                case 11 -> checksumshA256((String) SchemaUtils.validateSameMember($SCHEMA_CHECKSUMSH_A256, member, value));
                case 12 -> checksumshA512((String) SchemaUtils.validateSameMember($SCHEMA_CHECKSUMSH_A512, member, value));
                case 13 -> checksummD5((String) SchemaUtils.validateSameMember($SCHEMA_CHECKSUMM_D5, member, value));
                case 14 -> checksumxxhasH64((String) SchemaUtils.validateSameMember($SCHEMA_CHECKSUMXXHAS_H64, member, value));
                case 15 -> checksumxxhasH3((String) SchemaUtils.validateSameMember($SCHEMA_CHECKSUMXXHAS_H3, member, value));
                case 16 -> checksumxxhasH128((String) SchemaUtils.validateSameMember($SCHEMA_CHECKSUMXXHAS_H128, member, value));
                case 17 -> contentmD5((String) SchemaUtils.validateSameMember($SCHEMA_CONTENTM_D5, member, value));
                case 18 -> requestPayer((RequestPayer) SchemaUtils.validateSameMember($SCHEMA_REQUEST_PAYER, member, value));
                case 19 -> expectedBucketOwner((String) SchemaUtils.validateSameMember($SCHEMA_EXPECTED_BUCKET_OWNER, member, value));
                default -> ShapeBuilder.super.setMemberValue(member, value);
            }
        }

        @Override
        public ShapeBuilder<PutObjectAnnotationInput> errorCorrection() {
            if (tracker.allSet()) {
                return this;
            }
            if (!tracker.checkMember($SCHEMA_BUCKET)) {
                bucket("");
            }
            if (!tracker.checkMember($SCHEMA_KEY)) {
                key("");
            }
            if (!tracker.checkMember($SCHEMA_ANNOTATION_NAME)) {
                annotationName("");
            }
            if (!tracker.checkMember($SCHEMA_ANNOTATION_PAYLOAD)) {
                annotationPayload(DataStream.ofEmpty());
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
                    case 1 -> builder.key(de.readString(member));
                    case 2 -> builder.annotationName(de.readString(member));
                    case 3 -> builder.annotationPayload(de.readDataStream(member));
                    case 4 -> builder.versionId(de.readString(member));
                    case 5 -> builder.objectIfMatch(de.readString(member));
                    case 6 -> builder.checksumAlgorithm(ChecksumAlgorithm.builder().deserializeMember(de, member).build());
                    case 7 -> builder.checksumcrC32(de.readString(member));
                    case 8 -> builder.checksumcrC32C(de.readString(member));
                    case 9 -> builder.checksumcrC64nvmE(de.readString(member));
                    case 10 -> builder.checksumshA1(de.readString(member));
                    case 11 -> builder.checksumshA256(de.readString(member));
                    case 12 -> builder.checksumshA512(de.readString(member));
                    case 13 -> builder.checksummD5(de.readString(member));
                    case 14 -> builder.checksumxxhasH64(de.readString(member));
                    case 15 -> builder.checksumxxhasH3(de.readString(member));
                    case 16 -> builder.checksumxxhasH128(de.readString(member));
                    case 17 -> builder.contentmD5(de.readString(member));
                    case 18 -> builder.requestPayer(RequestPayer.builder().deserializeMember(de, member).build());
                    case 19 -> builder.expectedBucketOwner(de.readString(member));
                    default -> throw new IllegalArgumentException("Unexpected member: " + member.memberName());
                }
            }
        }
    }
}
