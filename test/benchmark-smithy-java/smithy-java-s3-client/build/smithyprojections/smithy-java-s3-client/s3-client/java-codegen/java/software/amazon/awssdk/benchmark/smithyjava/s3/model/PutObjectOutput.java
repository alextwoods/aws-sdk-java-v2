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
public final class PutObjectOutput implements SerializableStruct {

    public static final Schema $SCHEMA = Schemas6.PUT_OBJECT_OUTPUT;
    private static final Schema $SCHEMA_EXPIRATION = $SCHEMA.member("Expiration");
    private static final Schema $SCHEMA_E_TAG = $SCHEMA.member("ETag");
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
    private static final Schema $SCHEMA_CHECKSUM_TYPE = $SCHEMA.member("ChecksumType");
    private static final Schema $SCHEMA_SERVER_SIDE_ENCRYPTION = $SCHEMA.member("ServerSideEncryption");
    private static final Schema $SCHEMA_VERSION_ID = $SCHEMA.member("VersionId");
    private static final Schema $SCHEMA_SSE_CUSTOMER_ALGORITHM = $SCHEMA.member("SSECustomerAlgorithm");
    private static final Schema $SCHEMA_SSE_CUSTOMER_KEYM_D5 = $SCHEMA.member("SSECustomerKeyMD5");
    private static final Schema $SCHEMA_SSEKMS_KEY_ID = $SCHEMA.member("SSEKMSKeyId");
    private static final Schema $SCHEMA_SSEKMS_ENCRYPTION_CONTEXT = $SCHEMA.member("SSEKMSEncryptionContext");
    private static final Schema $SCHEMA_BUCKET_KEY_ENABLED = $SCHEMA.member("BucketKeyEnabled");
    private static final Schema $SCHEMA_SIZE = $SCHEMA.member("Size");
    private static final Schema $SCHEMA_REQUEST_CHARGED = $SCHEMA.member("RequestCharged");

    public static final ShapeId $ID = $SCHEMA.id();

    private final transient String expiration;
    private final transient String eTag;
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
    private final transient ChecksumType checksumType;
    private final transient ServerSideEncryption serverSideEncryption;
    private final transient String versionId;
    private final transient String sseCustomerAlgorithm;
    private final transient String sseCustomerKeymD5;
    private final transient String ssekmsKeyId;
    private final transient String ssekmsEncryptionContext;
    private final transient Boolean bucketKeyEnabled;
    private final transient Long size;
    private final transient RequestCharged requestCharged;

    private PutObjectOutput(Builder builder) {
        this.expiration = builder.expiration;
        this.eTag = builder.eTag;
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
        this.checksumType = builder.checksumType;
        this.serverSideEncryption = builder.serverSideEncryption;
        this.versionId = builder.versionId;
        this.sseCustomerAlgorithm = builder.sseCustomerAlgorithm;
        this.sseCustomerKeymD5 = builder.sseCustomerKeymD5;
        this.ssekmsKeyId = builder.ssekmsKeyId;
        this.ssekmsEncryptionContext = builder.ssekmsEncryptionContext;
        this.bucketKeyEnabled = builder.bucketKeyEnabled;
        this.size = builder.size;
        this.requestCharged = builder.requestCharged;
    }

    /**
     * If the expiration is configured for the object (see <a href="https://docs.aws.amazon.com/AmazonS3/latest/API/API_PutBucketLifecycleConfiguration.html">PutBucketLifecycleConfiguration</a>) in the <i>Amazon S3
     * User Guide</i>, the response includes this header. It includes the <code>expiry-date</code> and <code>rule-id</code>
     * key-value pairs that provide information about object expiration. The value of the <code>rule-id</code> is
     * URL-encoded.
     *
     * <p>Object expiration information is not returned in directory buckets and this header returns the value "<code>
     * NotImplemented</code>" in all responses for directory buckets.
     */
    public String getExpiration() {
        return expiration;
    }

    /**
     * Entity tag for the uploaded object.
     *
     * <p><b>General purpose buckets </b> - To ensure that data is not corrupted traversing the network, for objects
     * where the ETag is the MD5 digest of the object, you can calculate the MD5 while putting an object to Amazon S3
     * and compare the returned ETag to the calculated MD5 value.
     *
     * <p><b>Directory buckets </b> - The ETag for the object in a directory bucket isn't the MD5 digest of the object.
     */
    public String getETag() {
        return eTag;
    }

    /**
     * The Base64 encoded, 32-bit <code>CRC32 checksum</code> of the object. This checksum is only present if the
     * checksum was uploaded with the object. When you use an API operation on an object that was uploaded using
     * multipart uploads, this value may not be a direct checksum value of the full object. Instead, it's a calculation
     * based on the checksum values of each individual part. For more information about how checksums are calculated
     * with multipart uploads, see <a href="https://docs.aws.amazon.com/AmazonS3/latest/userguide/checking-object-integrity.html#large-object-checksums"> Checking object integrity</a> in the <i>Amazon S3 User Guide</i>.
     */
    public String getChecksumcrC32() {
        return checksumcrC32;
    }

    /**
     * The Base64 encoded, 32-bit <code>CRC32C</code> checksum of the object. This checksum is only present if the
     * checksum was uploaded with the object. When you use an API operation on an object that was uploaded using
     * multipart uploads, this value may not be a direct checksum value of the full object. Instead, it's a calculation
     * based on the checksum values of each individual part. For more information about how checksums are calculated
     * with multipart uploads, see <a href="https://docs.aws.amazon.com/AmazonS3/latest/userguide/checking-object-integrity.html#large-object-checksums"> Checking object integrity</a> in the <i>Amazon S3 User Guide</i>.
     */
    public String getChecksumcrC32C() {
        return checksumcrC32C;
    }

    /**
     * The Base64 encoded, 64-bit <code>CRC64NVME</code> checksum of the object. This header is present if the object
     * was uploaded with the <code>CRC64NVME</code> checksum algorithm, or if it was uploaded without a checksum (and
     * Amazon S3 added the default checksum, <code>CRC64NVME</code>, to the uploaded object). For more information about
     * how checksums are calculated with multipart uploads, see <a
     * href="https://docs.aws.amazon.com/AmazonS3/latest/userguide/checking-object-integrity.html">Checking object
     * integrity in the Amazon S3 User Guide</a>.
     */
    public String getChecksumcrC64nvmE() {
        return checksumcrC64nvmE;
    }

    /**
     * The Base64 encoded, 160-bit <code>SHA1</code> digest of the object. This checksum is only present if the checksum
     * was uploaded with the object. When you use the API operation on an object that was uploaded using multipart
     * uploads, this value may not be a direct checksum value of the full object. Instead, it's a calculation based on
     * the checksum values of each individual part. For more information about how checksums are calculated with
     * multipart uploads, see <a href="https://docs.aws.amazon.com/AmazonS3/latest/userguide/checking-object-integrity.html#large-object-checksums"> Checking object integrity</a> in the <i>Amazon S3 User Guide</i>.
     */
    public String getChecksumshA1() {
        return checksumshA1;
    }

    /**
     * The Base64 encoded, 256-bit <code>SHA256</code> digest of the object. This checksum is only present if the
     * checksum was uploaded with the object. When you use an API operation on an object that was uploaded using
     * multipart uploads, this value may not be a direct checksum value of the full object. Instead, it's a calculation
     * based on the checksum values of each individual part. For more information about how checksums are calculated
     * with multipart uploads, see <a href="https://docs.aws.amazon.com/AmazonS3/latest/userguide/checking-object-integrity.html#large-object-checksums"> Checking object integrity</a> in the <i>Amazon S3 User Guide</i>.
     */
    public String getChecksumshA256() {
        return checksumshA256;
    }

    /**
     * The Base64 encoded, 512-bit <code>SHA512</code> digest of the object. This header is present if the object was
     * uploaded with the <code>SHA512</code> checksum algorithm. For more information, see <a
     * href="https://docs.aws.amazon.com/AmazonS3/latest/userguide/checking-object-integrity.html">Checking object
     * integrity in the Amazon S3 User Guide</a>.
     */
    public String getChecksumshA512() {
        return checksumshA512;
    }

    /**
     * The Base64 encoded, 128-bit <code>MD5</code> digest of the object. This header is present if the object was
     * uploaded with the <code>MD5</code> checksum algorithm. For more information, see <a
     * href="https://docs.aws.amazon.com/AmazonS3/latest/userguide/checking-object-integrity.html">Checking object
     * integrity in the Amazon S3 User Guide</a>.
     */
    public String getChecksummD5() {
        return checksummD5;
    }

    /**
     * The Base64 encoded, 64-bit <code>XXHASH64</code> checksum of the object. This header is present if the object was
     * uploaded with the <code>XXHASH64</code> checksum algorithm. For more information, see <a
     * href="https://docs.aws.amazon.com/AmazonS3/latest/userguide/checking-object-integrity.html">Checking object
     * integrity in the Amazon S3 User Guide</a>.
     */
    public String getChecksumxxhasH64() {
        return checksumxxhasH64;
    }

    /**
     * The Base64 encoded, 64-bit <code>XXHASH3</code> checksum of the object. This header is present if the object was
     * uploaded with the <code>XXHASH3</code> checksum algorithm. For more information, see <a
     * href="https://docs.aws.amazon.com/AmazonS3/latest/userguide/checking-object-integrity.html">Checking object
     * integrity in the Amazon S3 User Guide</a>.
     */
    public String getChecksumxxhasH3() {
        return checksumxxhasH3;
    }

    /**
     * The Base64 encoded, 128-bit <code>XXHASH128</code> checksum of the object. This header is present if the object
     * was uploaded with the <code>XXHASH128</code> checksum algorithm. For more information, see <a
     * href="https://docs.aws.amazon.com/AmazonS3/latest/userguide/checking-object-integrity.html">Checking object
     * integrity in the Amazon S3 User Guide</a>.
     */
    public String getChecksumxxhasH128() {
        return checksumxxhasH128;
    }

    /**
     * This header specifies the checksum type of the object, which determines how part-level checksums are combined to
     * create an object-level checksum for multipart objects. For <code>PutObject</code> uploads, the checksum type is
     * always <code>FULL_OBJECT</code>. You can use this header as a data integrity check to verify that the checksum
     * type that is received is the same checksum that was specified. For more information, see <a href="https://docs.aws.amazon.com/AmazonS3/latest/userguide/checking-object-integrity.html">Checking object
     * integrity</a> in the <i>Amazon S3 User Guide</i>.
     */
    public ChecksumType getChecksumType() {
        return checksumType;
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
     * Version ID of the object.
     *
     * <p>If you enable versioning for a bucket, Amazon S3 automatically generates a unique version ID for the object
     * being stored. Amazon S3 returns this ID in the response. When you enable versioning for a bucket, if Amazon S3
     * receives multiple write requests for the same object simultaneously, it stores all of the objects. For more
     * information about versioning, see <a href="https://docs.aws.amazon.com/AmazonS3/latest/dev/AddingObjectstoVersioningEnabledBuckets.html">Adding Objects to Versioning-Enabled Buckets</a> in the <i>Amazon S3 User
     * Guide</i>. For information about returning the versioning state of a bucket, see <a href="https://docs.aws.amazon.com/AmazonS3/latest/API/API_GetBucketVersioning.html">GetBucketVersioning</a>.
     *
     * <p>This functionality is not supported for directory buckets.
     */
    public String getVersionId() {
        return versionId;
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
     * If present, indicates the Amazon Web Services KMS Encryption Context to use for object encryption. The value of
     * this header is a Base64 encoded string of a UTF-8 encoded JSON, which contains the encryption context as
     * key-value pairs. This value is stored as object metadata and automatically gets passed on to Amazon Web Services
     * KMS for future <code>GetObject</code> operations on this object.
     */
    public String getSsekmsEncryptionContext() {
        return ssekmsEncryptionContext;
    }

    /**
     * Indicates whether the uploaded object uses an S3 Bucket Key for server-side encryption with Key Management
     * Service (KMS) keys (SSE-KMS).
     */
    public Boolean isBucketKeyEnabled() {
        return bucketKeyEnabled;
    }

    /**
     * The size of the object in bytes. This value is only be present if you append to an object.
     *
     * <p>This functionality is only supported for objects in the Amazon S3 Express One Zone storage class in directory
     * buckets.
     */
    public Long getSize() {
        return size;
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
        PutObjectOutput that = (PutObjectOutput) other;
        return Objects.equals(this.bucketKeyEnabled, that.bucketKeyEnabled)
               && Objects.equals(this.size, that.size)
               && Objects.equals(this.expiration, that.expiration)
               && Objects.equals(this.eTag, that.eTag)
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
               && Objects.equals(this.versionId, that.versionId)
               && Objects.equals(this.sseCustomerAlgorithm, that.sseCustomerAlgorithm)
               && Objects.equals(this.sseCustomerKeymD5, that.sseCustomerKeymD5)
               && Objects.equals(this.ssekmsKeyId, that.ssekmsKeyId)
               && Objects.equals(this.ssekmsEncryptionContext, that.ssekmsEncryptionContext)
               && Objects.equals(this.checksumType, that.checksumType)
               && Objects.equals(this.serverSideEncryption, that.serverSideEncryption)
               && Objects.equals(this.requestCharged, that.requestCharged);
    }

    @Override
    public int hashCode() {
        int $hc = Objects.hashCode(expiration);
        $hc = 31 * $hc + Objects.hashCode(eTag);
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
        $hc = 31 * $hc + Objects.hashCode(checksumType);
        $hc = 31 * $hc + Objects.hashCode(serverSideEncryption);
        $hc = 31 * $hc + Objects.hashCode(versionId);
        $hc = 31 * $hc + Objects.hashCode(sseCustomerAlgorithm);
        $hc = 31 * $hc + Objects.hashCode(sseCustomerKeymD5);
        $hc = 31 * $hc + Objects.hashCode(ssekmsKeyId);
        $hc = 31 * $hc + Objects.hashCode(ssekmsEncryptionContext);
        $hc = 31 * $hc + Objects.hashCode(bucketKeyEnabled);
        $hc = 31 * $hc + Objects.hashCode(size);
        $hc = 31 * $hc + Objects.hashCode(requestCharged);
        return $hc;
    }

    @Override
    public Schema schema() {
        return $SCHEMA;
    }

    @Override
    public void serializeMembers(ShapeSerializer serializer) {
        if (expiration != null) {
            serializer.writeString($SCHEMA_EXPIRATION, expiration);
        }
        if (eTag != null) {
            serializer.writeString($SCHEMA_E_TAG, eTag);
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
        if (checksumType != null) {
            serializer.writeString($SCHEMA_CHECKSUM_TYPE, checksumType.getValue());
        }
        if (serverSideEncryption != null) {
            serializer.writeString($SCHEMA_SERVER_SIDE_ENCRYPTION, serverSideEncryption.getValue());
        }
        if (versionId != null) {
            serializer.writeString($SCHEMA_VERSION_ID, versionId);
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
        if (ssekmsEncryptionContext != null) {
            serializer.writeString($SCHEMA_SSEKMS_ENCRYPTION_CONTEXT, ssekmsEncryptionContext);
        }
        if (bucketKeyEnabled != null) {
            serializer.writeBoolean($SCHEMA_BUCKET_KEY_ENABLED, bucketKeyEnabled);
        }
        if (size != null) {
            serializer.writeLong($SCHEMA_SIZE, size);
        }
        if (requestCharged != null) {
            serializer.writeString($SCHEMA_REQUEST_CHARGED, requestCharged.getValue());
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T getMemberValue(Schema member) {
        return switch (member.memberIndex()) {
            case 0 -> (T) SchemaUtils.validateSameMember($SCHEMA_EXPIRATION, member, expiration);
            case 1 -> (T) SchemaUtils.validateSameMember($SCHEMA_E_TAG, member, eTag);
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
            case 12 -> (T) SchemaUtils.validateSameMember($SCHEMA_CHECKSUM_TYPE, member, checksumType);
            case 13 -> (T) SchemaUtils.validateSameMember($SCHEMA_SERVER_SIDE_ENCRYPTION, member, serverSideEncryption);
            case 14 -> (T) SchemaUtils.validateSameMember($SCHEMA_VERSION_ID, member, versionId);
            case 15 -> (T) SchemaUtils.validateSameMember($SCHEMA_SSE_CUSTOMER_ALGORITHM, member, sseCustomerAlgorithm);
            case 16 -> (T) SchemaUtils.validateSameMember($SCHEMA_SSE_CUSTOMER_KEYM_D5, member, sseCustomerKeymD5);
            case 17 -> (T) SchemaUtils.validateSameMember($SCHEMA_SSEKMS_KEY_ID, member, ssekmsKeyId);
            case 18 -> (T) SchemaUtils.validateSameMember($SCHEMA_SSEKMS_ENCRYPTION_CONTEXT, member, ssekmsEncryptionContext);
            case 19 -> (T) SchemaUtils.validateSameMember($SCHEMA_BUCKET_KEY_ENABLED, member, bucketKeyEnabled);
            case 20 -> (T) SchemaUtils.validateSameMember($SCHEMA_SIZE, member, size);
            case 21 -> (T) SchemaUtils.validateSameMember($SCHEMA_REQUEST_CHARGED, member, requestCharged);
            default -> throw new IllegalArgumentException("Attempted to get non-existent member: " + member.id());
        };
    }

    /**
     * Create a new builder containing all the current property values of this object.
     *
     * <p><strong>Note:</strong> This method performs only a shallow copy of the original properties.
     *
     * @return a builder for {@link PutObjectOutput}.
     */
    public Builder toBuilder() {
        var builder = new Builder();
        builder.expiration(this.expiration);
        builder.eTag(this.eTag);
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
        builder.checksumType(this.checksumType);
        builder.serverSideEncryption(this.serverSideEncryption);
        builder.versionId(this.versionId);
        builder.sseCustomerAlgorithm(this.sseCustomerAlgorithm);
        builder.sseCustomerKeymD5(this.sseCustomerKeymD5);
        builder.ssekmsKeyId(this.ssekmsKeyId);
        builder.ssekmsEncryptionContext(this.ssekmsEncryptionContext);
        builder.bucketKeyEnabled(this.bucketKeyEnabled);
        builder.size(this.size);
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
     * Builder for {@link PutObjectOutput}.
     */
    public static final class Builder implements ShapeBuilder<PutObjectOutput> {
        private String expiration;
        private String eTag;
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
        private ChecksumType checksumType;
        private ServerSideEncryption serverSideEncryption;
        private String versionId;
        private String sseCustomerAlgorithm;
        private String sseCustomerKeymD5;
        private String ssekmsKeyId;
        private String ssekmsEncryptionContext;
        private Boolean bucketKeyEnabled;
        private Long size;
        private RequestCharged requestCharged;

        private Builder() {}

        @Override
        public Schema schema() {
            return $SCHEMA;
        }

        /**
         * If the expiration is configured for the object (see <a href="https://docs.aws.amazon.com/AmazonS3/latest/API/API_PutBucketLifecycleConfiguration.html">PutBucketLifecycleConfiguration</a>) in the <i>Amazon S3
         * User Guide</i>, the response includes this header. It includes the <code>expiry-date</code> and <code>rule-id</code>
         * key-value pairs that provide information about object expiration. The value of the <code>rule-id</code> is
         * URL-encoded.
         *
         * <p>Object expiration information is not returned in directory buckets and this header returns the value "<code>
         * NotImplemented</code>" in all responses for directory buckets.
         *
         * @return this builder.
         */
        public Builder expiration(String expiration) {
            this.expiration = expiration;
            return this;
        }

        /**
         * Entity tag for the uploaded object.
         *
         * <p><b>General purpose buckets </b> - To ensure that data is not corrupted traversing the network, for objects
         * where the ETag is the MD5 digest of the object, you can calculate the MD5 while putting an object to Amazon S3
         * and compare the returned ETag to the calculated MD5 value.
         *
         * <p><b>Directory buckets </b> - The ETag for the object in a directory bucket isn't the MD5 digest of the object.
         *
         * @return this builder.
         */
        public Builder eTag(String eTag) {
            this.eTag = eTag;
            return this;
        }

        /**
         * The Base64 encoded, 32-bit <code>CRC32 checksum</code> of the object. This checksum is only present if the
         * checksum was uploaded with the object. When you use an API operation on an object that was uploaded using
         * multipart uploads, this value may not be a direct checksum value of the full object. Instead, it's a calculation
         * based on the checksum values of each individual part. For more information about how checksums are calculated
         * with multipart uploads, see <a href="https://docs.aws.amazon.com/AmazonS3/latest/userguide/checking-object-integrity.html#large-object-checksums"> Checking object integrity</a> in the <i>Amazon S3 User Guide</i>.
         *
         * @return this builder.
         */
        public Builder checksumcrC32(String checksumcrC32) {
            this.checksumcrC32 = checksumcrC32;
            return this;
        }

        /**
         * The Base64 encoded, 32-bit <code>CRC32C</code> checksum of the object. This checksum is only present if the
         * checksum was uploaded with the object. When you use an API operation on an object that was uploaded using
         * multipart uploads, this value may not be a direct checksum value of the full object. Instead, it's a calculation
         * based on the checksum values of each individual part. For more information about how checksums are calculated
         * with multipart uploads, see <a href="https://docs.aws.amazon.com/AmazonS3/latest/userguide/checking-object-integrity.html#large-object-checksums"> Checking object integrity</a> in the <i>Amazon S3 User Guide</i>.
         *
         * @return this builder.
         */
        public Builder checksumcrC32C(String checksumcrC32C) {
            this.checksumcrC32C = checksumcrC32C;
            return this;
        }

        /**
         * The Base64 encoded, 64-bit <code>CRC64NVME</code> checksum of the object. This header is present if the object
         * was uploaded with the <code>CRC64NVME</code> checksum algorithm, or if it was uploaded without a checksum (and
         * Amazon S3 added the default checksum, <code>CRC64NVME</code>, to the uploaded object). For more information about
         * how checksums are calculated with multipart uploads, see <a
         * href="https://docs.aws.amazon.com/AmazonS3/latest/userguide/checking-object-integrity.html">Checking object
         * integrity in the Amazon S3 User Guide</a>.
         *
         * @return this builder.
         */
        public Builder checksumcrC64nvmE(String checksumcrC64nvmE) {
            this.checksumcrC64nvmE = checksumcrC64nvmE;
            return this;
        }

        /**
         * The Base64 encoded, 160-bit <code>SHA1</code> digest of the object. This checksum is only present if the checksum
         * was uploaded with the object. When you use the API operation on an object that was uploaded using multipart
         * uploads, this value may not be a direct checksum value of the full object. Instead, it's a calculation based on
         * the checksum values of each individual part. For more information about how checksums are calculated with
         * multipart uploads, see <a href="https://docs.aws.amazon.com/AmazonS3/latest/userguide/checking-object-integrity.html#large-object-checksums"> Checking object integrity</a> in the <i>Amazon S3 User Guide</i>.
         *
         * @return this builder.
         */
        public Builder checksumshA1(String checksumshA1) {
            this.checksumshA1 = checksumshA1;
            return this;
        }

        /**
         * The Base64 encoded, 256-bit <code>SHA256</code> digest of the object. This checksum is only present if the
         * checksum was uploaded with the object. When you use an API operation on an object that was uploaded using
         * multipart uploads, this value may not be a direct checksum value of the full object. Instead, it's a calculation
         * based on the checksum values of each individual part. For more information about how checksums are calculated
         * with multipart uploads, see <a href="https://docs.aws.amazon.com/AmazonS3/latest/userguide/checking-object-integrity.html#large-object-checksums"> Checking object integrity</a> in the <i>Amazon S3 User Guide</i>.
         *
         * @return this builder.
         */
        public Builder checksumshA256(String checksumshA256) {
            this.checksumshA256 = checksumshA256;
            return this;
        }

        /**
         * The Base64 encoded, 512-bit <code>SHA512</code> digest of the object. This header is present if the object was
         * uploaded with the <code>SHA512</code> checksum algorithm. For more information, see <a
         * href="https://docs.aws.amazon.com/AmazonS3/latest/userguide/checking-object-integrity.html">Checking object
         * integrity in the Amazon S3 User Guide</a>.
         *
         * @return this builder.
         */
        public Builder checksumshA512(String checksumshA512) {
            this.checksumshA512 = checksumshA512;
            return this;
        }

        /**
         * The Base64 encoded, 128-bit <code>MD5</code> digest of the object. This header is present if the object was
         * uploaded with the <code>MD5</code> checksum algorithm. For more information, see <a
         * href="https://docs.aws.amazon.com/AmazonS3/latest/userguide/checking-object-integrity.html">Checking object
         * integrity in the Amazon S3 User Guide</a>.
         *
         * @return this builder.
         */
        public Builder checksummD5(String checksummD5) {
            this.checksummD5 = checksummD5;
            return this;
        }

        /**
         * The Base64 encoded, 64-bit <code>XXHASH64</code> checksum of the object. This header is present if the object was
         * uploaded with the <code>XXHASH64</code> checksum algorithm. For more information, see <a
         * href="https://docs.aws.amazon.com/AmazonS3/latest/userguide/checking-object-integrity.html">Checking object
         * integrity in the Amazon S3 User Guide</a>.
         *
         * @return this builder.
         */
        public Builder checksumxxhasH64(String checksumxxhasH64) {
            this.checksumxxhasH64 = checksumxxhasH64;
            return this;
        }

        /**
         * The Base64 encoded, 64-bit <code>XXHASH3</code> checksum of the object. This header is present if the object was
         * uploaded with the <code>XXHASH3</code> checksum algorithm. For more information, see <a
         * href="https://docs.aws.amazon.com/AmazonS3/latest/userguide/checking-object-integrity.html">Checking object
         * integrity in the Amazon S3 User Guide</a>.
         *
         * @return this builder.
         */
        public Builder checksumxxhasH3(String checksumxxhasH3) {
            this.checksumxxhasH3 = checksumxxhasH3;
            return this;
        }

        /**
         * The Base64 encoded, 128-bit <code>XXHASH128</code> checksum of the object. This header is present if the object
         * was uploaded with the <code>XXHASH128</code> checksum algorithm. For more information, see <a
         * href="https://docs.aws.amazon.com/AmazonS3/latest/userguide/checking-object-integrity.html">Checking object
         * integrity in the Amazon S3 User Guide</a>.
         *
         * @return this builder.
         */
        public Builder checksumxxhasH128(String checksumxxhasH128) {
            this.checksumxxhasH128 = checksumxxhasH128;
            return this;
        }

        /**
         * This header specifies the checksum type of the object, which determines how part-level checksums are combined to
         * create an object-level checksum for multipart objects. For <code>PutObject</code> uploads, the checksum type is
         * always <code>FULL_OBJECT</code>. You can use this header as a data integrity check to verify that the checksum
         * type that is received is the same checksum that was specified. For more information, see <a href="https://docs.aws.amazon.com/AmazonS3/latest/userguide/checking-object-integrity.html">Checking object
         * integrity</a> in the <i>Amazon S3 User Guide</i>.
         *
         * @return this builder.
         */
        public Builder checksumType(ChecksumType checksumType) {
            this.checksumType = checksumType;
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
         * Version ID of the object.
         *
         * <p>If you enable versioning for a bucket, Amazon S3 automatically generates a unique version ID for the object
         * being stored. Amazon S3 returns this ID in the response. When you enable versioning for a bucket, if Amazon S3
         * receives multiple write requests for the same object simultaneously, it stores all of the objects. For more
         * information about versioning, see <a href="https://docs.aws.amazon.com/AmazonS3/latest/dev/AddingObjectstoVersioningEnabledBuckets.html">Adding Objects to Versioning-Enabled Buckets</a> in the <i>Amazon S3 User
         * Guide</i>. For information about returning the versioning state of a bucket, see <a href="https://docs.aws.amazon.com/AmazonS3/latest/API/API_GetBucketVersioning.html">GetBucketVersioning</a>.
         *
         * <p>This functionality is not supported for directory buckets.
         *
         * @return this builder.
         */
        public Builder versionId(String versionId) {
            this.versionId = versionId;
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
         * Indicates whether the uploaded object uses an S3 Bucket Key for server-side encryption with Key Management
         * Service (KMS) keys (SSE-KMS).
         *
         * @return this builder.
         */
        public Builder bucketKeyEnabled(Boolean bucketKeyEnabled) {
            this.bucketKeyEnabled = bucketKeyEnabled;
            return this;
        }

        /**
         * The size of the object in bytes. This value is only be present if you append to an object.
         *
         * <p>This functionality is only supported for objects in the Amazon S3 Express One Zone storage class in directory
         * buckets.
         *
         * @return this builder.
         */
        public Builder size(Long size) {
            this.size = size;
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
        public PutObjectOutput build() {
            return new PutObjectOutput(this);
        }

        @Override
        @SuppressWarnings("unchecked")
        public void setMemberValue(Schema member, Object value) {
            switch (member.memberIndex()) {
                case 0 -> expiration((String) SchemaUtils.validateSameMember($SCHEMA_EXPIRATION, member, value));
                case 1 -> eTag((String) SchemaUtils.validateSameMember($SCHEMA_E_TAG, member, value));
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
                case 12 -> checksumType((ChecksumType) SchemaUtils.validateSameMember($SCHEMA_CHECKSUM_TYPE, member, value));
                case 13 -> serverSideEncryption((ServerSideEncryption) SchemaUtils.validateSameMember($SCHEMA_SERVER_SIDE_ENCRYPTION, member, value));
                case 14 -> versionId((String) SchemaUtils.validateSameMember($SCHEMA_VERSION_ID, member, value));
                case 15 -> sseCustomerAlgorithm((String) SchemaUtils.validateSameMember($SCHEMA_SSE_CUSTOMER_ALGORITHM, member, value));
                case 16 -> sseCustomerKeymD5((String) SchemaUtils.validateSameMember($SCHEMA_SSE_CUSTOMER_KEYM_D5, member, value));
                case 17 -> ssekmsKeyId((String) SchemaUtils.validateSameMember($SCHEMA_SSEKMS_KEY_ID, member, value));
                case 18 -> ssekmsEncryptionContext((String) SchemaUtils.validateSameMember($SCHEMA_SSEKMS_ENCRYPTION_CONTEXT, member, value));
                case 19 -> bucketKeyEnabled((Boolean) SchemaUtils.validateSameMember($SCHEMA_BUCKET_KEY_ENABLED, member, value));
                case 20 -> size((Long) SchemaUtils.validateSameMember($SCHEMA_SIZE, member, value));
                case 21 -> requestCharged((RequestCharged) SchemaUtils.validateSameMember($SCHEMA_REQUEST_CHARGED, member, value));
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
                    case 0 -> builder.expiration(de.readString(member));
                    case 1 -> builder.eTag(de.readString(member));
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
                    case 12 -> builder.checksumType(ChecksumType.builder().deserializeMember(de, member).build());
                    case 13 -> builder.serverSideEncryption(ServerSideEncryption.builder().deserializeMember(de, member).build());
                    case 14 -> builder.versionId(de.readString(member));
                    case 15 -> builder.sseCustomerAlgorithm(de.readString(member));
                    case 16 -> builder.sseCustomerKeymD5(de.readString(member));
                    case 17 -> builder.ssekmsKeyId(de.readString(member));
                    case 18 -> builder.ssekmsEncryptionContext(de.readString(member));
                    case 19 -> builder.bucketKeyEnabled(de.readBoolean(member));
                    case 20 -> builder.size(de.readLong(member));
                    case 21 -> builder.requestCharged(RequestCharged.builder().deserializeMember(de, member).build());
                    default -> throw new IllegalArgumentException("Unexpected member: " + member.memberName());
                }
            }
        }
    }
}
