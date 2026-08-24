package software.amazon.awssdk.benchmark.smithyjava.s3.model;

import java.time.Instant;
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
 * Container for all response elements.
 */
@SmithyGenerated
public final class CopyPartResult implements SerializableStruct {

    public static final Schema $SCHEMA = Schemas7.COPY_PART_RESULT;
    private static final Schema $SCHEMA_E_TAG = $SCHEMA.member("ETag");
    private static final Schema $SCHEMA_LAST_MODIFIED = $SCHEMA.member("LastModified");
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

    public static final ShapeId $ID = $SCHEMA.id();

    private final transient String eTag;
    private final transient Instant lastModified;
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

    private CopyPartResult(Builder builder) {
        this.eTag = builder.eTag;
        this.lastModified = builder.lastModified;
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
    }

    /**
     * Entity tag of the object.
     */
    public String getETag() {
        return eTag;
    }

    /**
     * Date and time at which the object was uploaded.
     */
    public Instant getLastModified() {
        return lastModified;
    }

    /**
     * The Base64 encoded, 32-bit <code>CRC32</code> checksum of the part. This checksum is present if the multipart
     * upload request was created with the <code>CRC32</code> checksum algorithm. For more information, see <a href="https://docs.aws.amazon.com/AmazonS3/latest/userguide/checking-object-integrity.html">Checking
     * object integrity</a> in the <i>Amazon S3 User Guide</i>.
     */
    public String getChecksumcrC32() {
        return checksumcrC32;
    }

    /**
     * The Base64 encoded, 32-bit <code>CRC32C</code> checksum of the part. This checksum is present if the multipart
     * upload request was created with the <code>CRC32C</code> checksum algorithm. For more information, see <a href="https://docs.aws.amazon.com/AmazonS3/latest/userguide/checking-object-integrity.html">Checking
     * object integrity</a> in the <i>Amazon S3 User Guide</i>.
     */
    public String getChecksumcrC32C() {
        return checksumcrC32C;
    }

    /**
     * The Base64 encoded, 64-bit <code>CRC64NVME</code> checksum of the part. This checksum is present if the multipart
     * upload request was created with the <code>CRC64NVME</code> checksum algorithm. For more information, see <a href="https://docs.aws.amazon.com/AmazonS3/latest/userguide/checking-object-integrity.html">
     * Checking object integrity</a> in the <i>Amazon S3 User Guide</i>.
     */
    public String getChecksumcrC64nvmE() {
        return checksumcrC64nvmE;
    }

    /**
     * The Base64 encoded, 160-bit <code>SHA1</code> digest of the part. This checksum is present if the multipart
     * upload request was created with the <code>SHA1</code> checksum algorithm. For more information, see <a href="https://docs.aws.amazon.com/AmazonS3/latest/userguide/checking-object-integrity.html">Checking
     * object integrity</a> in the <i>Amazon S3 User Guide</i>.
     */
    public String getChecksumshA1() {
        return checksumshA1;
    }

    /**
     * The Base64 encoded, 256-bit <code>SHA256</code> digest of the part. This checksum is present if the multipart
     * upload request was created with the <code>SHA256</code> checksum algorithm. For more information, see <a href="https://docs.aws.amazon.com/AmazonS3/latest/userguide/checking-object-integrity.html">Checking
     * object integrity</a> in the <i>Amazon S3 User Guide</i>.
     */
    public String getChecksumshA256() {
        return checksumshA256;
    }

    /**
     * The Base64 encoded, 512-bit <code>SHA512</code> digest of the part. This checksum is present if the multipart
     * upload request was created with the <code>SHA512</code> checksum algorithm. For more information, see <a href="https://docs.aws.amazon.com/AmazonS3/latest/userguide/checking-object-integrity.html">Checking
     * object integrity</a> in the <i>Amazon S3 User Guide</i>.
     */
    public String getChecksumshA512() {
        return checksumshA512;
    }

    /**
     * The Base64 encoded, 128-bit <code>MD5</code> digest of the part. This checksum is present if the multipart upload
     * request was created with the <code>MD5</code> checksum algorithm. For more information, see <a href="https://docs.aws.amazon.com/AmazonS3/latest/userguide/checking-object-integrity.html">Checking object
     * integrity</a> in the <i>Amazon S3 User Guide</i>.
     */
    public String getChecksummD5() {
        return checksummD5;
    }

    /**
     * The Base64 encoded, 64-bit <code>XXHASH64</code> checksum of the part. This checksum is present if the multipart
     * upload request was created with the <code>XXHASH64</code> checksum algorithm. For more information, see <a href="https://docs.aws.amazon.com/AmazonS3/latest/userguide/checking-object-integrity.html">
     * Checking object integrity</a> in the <i>Amazon S3 User Guide</i>.
     */
    public String getChecksumxxhasH64() {
        return checksumxxhasH64;
    }

    /**
     * The Base64 encoded, 64-bit <code>XXHASH3</code> checksum of the part. This checksum is present if the multipart
     * upload request was created with the <code>XXHASH3</code> checksum algorithm. For more information, see <a href="https://docs.aws.amazon.com/AmazonS3/latest/userguide/checking-object-integrity.html">
     * Checking object integrity</a> in the <i>Amazon S3 User Guide</i>.
     */
    public String getChecksumxxhasH3() {
        return checksumxxhasH3;
    }

    /**
     * The Base64 encoded, 128-bit <code>XXHASH128</code> checksum of the part. This checksum is present if the
     * multipart upload request was created with the <code>XXHASH128</code> checksum algorithm. For more information,
     * see <a href="https://docs.aws.amazon.com/AmazonS3/latest/userguide/checking-object-integrity.html">Checking object integrity</a> in the <i>Amazon S3 User Guide</i>.
     */
    public String getChecksumxxhasH128() {
        return checksumxxhasH128;
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
        CopyPartResult that = (CopyPartResult) other;
        return Objects.equals(this.eTag, that.eTag)
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
               && Objects.equals(this.lastModified, that.lastModified);
    }

    @Override
    public int hashCode() {
        int $hc = Objects.hashCode(eTag);
        $hc = 31 * $hc + Objects.hashCode(lastModified);
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
        return $hc;
    }

    @Override
    public Schema schema() {
        return $SCHEMA;
    }

    @Override
    public void serializeMembers(ShapeSerializer serializer) {
        if (eTag != null) {
            serializer.writeString($SCHEMA_E_TAG, eTag);
        }
        if (lastModified != null) {
            serializer.writeTimestamp($SCHEMA_LAST_MODIFIED, lastModified);
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
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T getMemberValue(Schema member) {
        return switch (member.memberIndex()) {
            case 0 -> (T) SchemaUtils.validateSameMember($SCHEMA_E_TAG, member, eTag);
            case 1 -> (T) SchemaUtils.validateSameMember($SCHEMA_LAST_MODIFIED, member, lastModified);
            case 2 -> (T) SchemaUtils.validateSameMember($SCHEMA_CHECKSUMCR_C32, member, checksumcrC32);
            case 3 -> (T) SchemaUtils.validateSameMember($SCHEMA_CHECKSUMCR_C32_C, member, checksumcrC32C);
            case 4 -> (T) SchemaUtils.validateSameMember($SCHEMA_CHECKSUMCR_C64NVM_E, member, checksumcrC64nvmE);
            case 5 -> (T) SchemaUtils.validateSameMember($SCHEMA_CHECKSUMSH_A1, member, checksumshA1);
            case 6 -> (T) SchemaUtils.validateSameMember($SCHEMA_CHECKSUMSH_A256, member, checksumshA256);
            case 7 -> (T) SchemaUtils.validateSameMember($SCHEMA_CHECKSUMSH_A512, member, checksumshA512);
            case 8 -> (T) SchemaUtils.validateSameMember($SCHEMA_CHECKSUMM_D5, member, checksummD5);
            case 9 -> (T) SchemaUtils.validateSameMember($SCHEMA_CHECKSUMXXHAS_H64, member, checksumxxhasH64);
            case 10 -> (T) SchemaUtils.validateSameMember($SCHEMA_CHECKSUMXXHAS_H3, member, checksumxxhasH3);
            case 11 -> (T) SchemaUtils.validateSameMember($SCHEMA_CHECKSUMXXHAS_H128, member, checksumxxhasH128);
            default -> throw new IllegalArgumentException("Attempted to get non-existent member: " + member.id());
        };
    }

    /**
     * Create a new builder containing all the current property values of this object.
     *
     * <p><strong>Note:</strong> This method performs only a shallow copy of the original properties.
     *
     * @return a builder for {@link CopyPartResult}.
     */
    public Builder toBuilder() {
        var builder = new Builder();
        builder.eTag(this.eTag);
        builder.lastModified(this.lastModified);
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
        return builder;
    }

    /**
     * @return returns a new Builder.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for {@link CopyPartResult}.
     */
    public static final class Builder implements ShapeBuilder<CopyPartResult> {
        private String eTag;
        private Instant lastModified;
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

        private Builder() {}

        @Override
        public Schema schema() {
            return $SCHEMA;
        }

        /**
         * Entity tag of the object.
         *
         * @return this builder.
         */
        public Builder eTag(String eTag) {
            this.eTag = eTag;
            return this;
        }

        /**
         * Date and time at which the object was uploaded.
         *
         * @return this builder.
         */
        public Builder lastModified(Instant lastModified) {
            this.lastModified = lastModified;
            return this;
        }

        /**
         * The Base64 encoded, 32-bit <code>CRC32</code> checksum of the part. This checksum is present if the multipart
         * upload request was created with the <code>CRC32</code> checksum algorithm. For more information, see <a href="https://docs.aws.amazon.com/AmazonS3/latest/userguide/checking-object-integrity.html">Checking
         * object integrity</a> in the <i>Amazon S3 User Guide</i>.
         *
         * @return this builder.
         */
        public Builder checksumcrC32(String checksumcrC32) {
            this.checksumcrC32 = checksumcrC32;
            return this;
        }

        /**
         * The Base64 encoded, 32-bit <code>CRC32C</code> checksum of the part. This checksum is present if the multipart
         * upload request was created with the <code>CRC32C</code> checksum algorithm. For more information, see <a href="https://docs.aws.amazon.com/AmazonS3/latest/userguide/checking-object-integrity.html">Checking
         * object integrity</a> in the <i>Amazon S3 User Guide</i>.
         *
         * @return this builder.
         */
        public Builder checksumcrC32C(String checksumcrC32C) {
            this.checksumcrC32C = checksumcrC32C;
            return this;
        }

        /**
         * The Base64 encoded, 64-bit <code>CRC64NVME</code> checksum of the part. This checksum is present if the multipart
         * upload request was created with the <code>CRC64NVME</code> checksum algorithm. For more information, see <a href="https://docs.aws.amazon.com/AmazonS3/latest/userguide/checking-object-integrity.html">
         * Checking object integrity</a> in the <i>Amazon S3 User Guide</i>.
         *
         * @return this builder.
         */
        public Builder checksumcrC64nvmE(String checksumcrC64nvmE) {
            this.checksumcrC64nvmE = checksumcrC64nvmE;
            return this;
        }

        /**
         * The Base64 encoded, 160-bit <code>SHA1</code> digest of the part. This checksum is present if the multipart
         * upload request was created with the <code>SHA1</code> checksum algorithm. For more information, see <a href="https://docs.aws.amazon.com/AmazonS3/latest/userguide/checking-object-integrity.html">Checking
         * object integrity</a> in the <i>Amazon S3 User Guide</i>.
         *
         * @return this builder.
         */
        public Builder checksumshA1(String checksumshA1) {
            this.checksumshA1 = checksumshA1;
            return this;
        }

        /**
         * The Base64 encoded, 256-bit <code>SHA256</code> digest of the part. This checksum is present if the multipart
         * upload request was created with the <code>SHA256</code> checksum algorithm. For more information, see <a href="https://docs.aws.amazon.com/AmazonS3/latest/userguide/checking-object-integrity.html">Checking
         * object integrity</a> in the <i>Amazon S3 User Guide</i>.
         *
         * @return this builder.
         */
        public Builder checksumshA256(String checksumshA256) {
            this.checksumshA256 = checksumshA256;
            return this;
        }

        /**
         * The Base64 encoded, 512-bit <code>SHA512</code> digest of the part. This checksum is present if the multipart
         * upload request was created with the <code>SHA512</code> checksum algorithm. For more information, see <a href="https://docs.aws.amazon.com/AmazonS3/latest/userguide/checking-object-integrity.html">Checking
         * object integrity</a> in the <i>Amazon S3 User Guide</i>.
         *
         * @return this builder.
         */
        public Builder checksumshA512(String checksumshA512) {
            this.checksumshA512 = checksumshA512;
            return this;
        }

        /**
         * The Base64 encoded, 128-bit <code>MD5</code> digest of the part. This checksum is present if the multipart upload
         * request was created with the <code>MD5</code> checksum algorithm. For more information, see <a href="https://docs.aws.amazon.com/AmazonS3/latest/userguide/checking-object-integrity.html">Checking object
         * integrity</a> in the <i>Amazon S3 User Guide</i>.
         *
         * @return this builder.
         */
        public Builder checksummD5(String checksummD5) {
            this.checksummD5 = checksummD5;
            return this;
        }

        /**
         * The Base64 encoded, 64-bit <code>XXHASH64</code> checksum of the part. This checksum is present if the multipart
         * upload request was created with the <code>XXHASH64</code> checksum algorithm. For more information, see <a href="https://docs.aws.amazon.com/AmazonS3/latest/userguide/checking-object-integrity.html">
         * Checking object integrity</a> in the <i>Amazon S3 User Guide</i>.
         *
         * @return this builder.
         */
        public Builder checksumxxhasH64(String checksumxxhasH64) {
            this.checksumxxhasH64 = checksumxxhasH64;
            return this;
        }

        /**
         * The Base64 encoded, 64-bit <code>XXHASH3</code> checksum of the part. This checksum is present if the multipart
         * upload request was created with the <code>XXHASH3</code> checksum algorithm. For more information, see <a href="https://docs.aws.amazon.com/AmazonS3/latest/userguide/checking-object-integrity.html">
         * Checking object integrity</a> in the <i>Amazon S3 User Guide</i>.
         *
         * @return this builder.
         */
        public Builder checksumxxhasH3(String checksumxxhasH3) {
            this.checksumxxhasH3 = checksumxxhasH3;
            return this;
        }

        /**
         * The Base64 encoded, 128-bit <code>XXHASH128</code> checksum of the part. This checksum is present if the
         * multipart upload request was created with the <code>XXHASH128</code> checksum algorithm. For more information,
         * see <a href="https://docs.aws.amazon.com/AmazonS3/latest/userguide/checking-object-integrity.html">Checking object integrity</a> in the <i>Amazon S3 User Guide</i>.
         *
         * @return this builder.
         */
        public Builder checksumxxhasH128(String checksumxxhasH128) {
            this.checksumxxhasH128 = checksumxxhasH128;
            return this;
        }

        @Override
        public CopyPartResult build() {
            return new CopyPartResult(this);
        }

        @Override
        @SuppressWarnings("unchecked")
        public void setMemberValue(Schema member, Object value) {
            switch (member.memberIndex()) {
                case 0 -> eTag((String) SchemaUtils.validateSameMember($SCHEMA_E_TAG, member, value));
                case 1 -> lastModified((Instant) SchemaUtils.validateSameMember($SCHEMA_LAST_MODIFIED, member, value));
                case 2 -> checksumcrC32((String) SchemaUtils.validateSameMember($SCHEMA_CHECKSUMCR_C32, member, value));
                case 3 -> checksumcrC32C((String) SchemaUtils.validateSameMember($SCHEMA_CHECKSUMCR_C32_C, member, value));
                case 4 -> checksumcrC64nvmE((String) SchemaUtils.validateSameMember($SCHEMA_CHECKSUMCR_C64NVM_E, member, value));
                case 5 -> checksumshA1((String) SchemaUtils.validateSameMember($SCHEMA_CHECKSUMSH_A1, member, value));
                case 6 -> checksumshA256((String) SchemaUtils.validateSameMember($SCHEMA_CHECKSUMSH_A256, member, value));
                case 7 -> checksumshA512((String) SchemaUtils.validateSameMember($SCHEMA_CHECKSUMSH_A512, member, value));
                case 8 -> checksummD5((String) SchemaUtils.validateSameMember($SCHEMA_CHECKSUMM_D5, member, value));
                case 9 -> checksumxxhasH64((String) SchemaUtils.validateSameMember($SCHEMA_CHECKSUMXXHAS_H64, member, value));
                case 10 -> checksumxxhasH3((String) SchemaUtils.validateSameMember($SCHEMA_CHECKSUMXXHAS_H3, member, value));
                case 11 -> checksumxxhasH128((String) SchemaUtils.validateSameMember($SCHEMA_CHECKSUMXXHAS_H128, member, value));
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
                    case 0 -> builder.eTag(de.readString(member));
                    case 1 -> builder.lastModified(de.readTimestamp(member));
                    case 2 -> builder.checksumcrC32(de.readString(member));
                    case 3 -> builder.checksumcrC32C(de.readString(member));
                    case 4 -> builder.checksumcrC64nvmE(de.readString(member));
                    case 5 -> builder.checksumshA1(de.readString(member));
                    case 6 -> builder.checksumshA256(de.readString(member));
                    case 7 -> builder.checksumshA512(de.readString(member));
                    case 8 -> builder.checksummD5(de.readString(member));
                    case 9 -> builder.checksumxxhasH64(de.readString(member));
                    case 10 -> builder.checksumxxhasH3(de.readString(member));
                    case 11 -> builder.checksumxxhasH128(de.readString(member));
                    default -> throw new IllegalArgumentException("Unexpected member: " + member.memberName());
                }
            }
        }
    }
}
