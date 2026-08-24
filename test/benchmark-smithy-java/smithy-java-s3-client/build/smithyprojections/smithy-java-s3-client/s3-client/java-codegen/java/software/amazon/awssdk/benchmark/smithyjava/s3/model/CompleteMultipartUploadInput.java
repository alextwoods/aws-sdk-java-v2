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
public final class CompleteMultipartUploadInput implements SerializableStruct {

    public static final Schema $SCHEMA = Schemas.COMPLETE_MULTIPART_UPLOAD_INPUT;
    private static final Schema $SCHEMA_BUCKET = $SCHEMA.member("Bucket");
    private static final Schema $SCHEMA_KEY = $SCHEMA.member("Key");
    private static final Schema $SCHEMA_MULTIPART_UPLOAD = $SCHEMA.member("MultipartUpload");
    private static final Schema $SCHEMA_UPLOAD_ID = $SCHEMA.member("UploadId");
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
    private static final Schema $SCHEMA_MPU_OBJECT_SIZE = $SCHEMA.member("MpuObjectSize");
    private static final Schema $SCHEMA_REQUEST_PAYER = $SCHEMA.member("RequestPayer");
    private static final Schema $SCHEMA_EXPECTED_BUCKET_OWNER = $SCHEMA.member("ExpectedBucketOwner");
    private static final Schema $SCHEMA_IF_MATCH = $SCHEMA.member("IfMatch");
    private static final Schema $SCHEMA_IF_NONE_MATCH = $SCHEMA.member("IfNoneMatch");
    private static final Schema $SCHEMA_SSE_CUSTOMER_ALGORITHM = $SCHEMA.member("SSECustomerAlgorithm");
    private static final Schema $SCHEMA_SSE_CUSTOMER_KEY = $SCHEMA.member("SSECustomerKey");
    private static final Schema $SCHEMA_SSE_CUSTOMER_KEYM_D5 = $SCHEMA.member("SSECustomerKeyMD5");

    public static final ShapeId $ID = $SCHEMA.id();

    private final transient String bucket;
    private final transient String key;
    private final transient CompletedMultipartUpload multipartUpload;
    private final transient String uploadId;
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
    private final transient Long mpuObjectSize;
    private final transient RequestPayer requestPayer;
    private final transient String expectedBucketOwner;
    private final transient String ifMatch;
    private final transient String ifNoneMatch;
    private final transient String sseCustomerAlgorithm;
    private final transient String sseCustomerKey;
    private final transient String sseCustomerKeymD5;

    private CompleteMultipartUploadInput(Builder builder) {
        this.bucket = builder.bucket;
        this.key = builder.key;
        this.multipartUpload = builder.multipartUpload;
        this.uploadId = builder.uploadId;
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
        this.mpuObjectSize = builder.mpuObjectSize;
        this.requestPayer = builder.requestPayer;
        this.expectedBucketOwner = builder.expectedBucketOwner;
        this.ifMatch = builder.ifMatch;
        this.ifNoneMatch = builder.ifNoneMatch;
        this.sseCustomerAlgorithm = builder.sseCustomerAlgorithm;
        this.sseCustomerKey = builder.sseCustomerKey;
        this.sseCustomerKeymD5 = builder.sseCustomerKeymD5;
    }

    /**
     * Name of the bucket to which the multipart upload was initiated.
     *
     * <p><b>Directory buckets</b> - When you use this operation with a directory bucket, you must use
     * virtual-hosted-style requests in the format <code><i>Bucket-name</i>.s3express-<i>zone-id</i>.<i>region-code</i>
     * .amazonaws.com</code>. Path-style requests are not supported. Directory bucket names must be unique in the chosen
     * Zone (Availability Zone or Local Zone). Bucket names must follow the format <code><i>bucket-base-name</i>--<i>
     * zone-id</i>--x-s3</code> (for example, <code><i>amzn-s3-demo-bucket</i>--<i>usw2-az1</i>--x-s3</code>). For
     * information about bucket naming restrictions, see <a href="https://docs.aws.amazon.com/AmazonS3/latest/userguide/directory-bucket-naming-rules.html">Directory bucket naming rules</a> in the <i>Amazon S3 User
     * Guide</i>.
     *
     * <p><b>Access points</b> - When you use this action with an access point for general purpose buckets, you must
     * provide the alias of the access point in place of the bucket name or specify the access point ARN. When you use
     * this action with an access point for directory buckets, you must provide the access point name in place of the
     * bucket name. When using the access point ARN, you must direct requests to the access point hostname. The access
     * point hostname takes the form <i>AccessPointName</i>-<i>AccountId</i>.s3-accesspoint.<i>Region</i>.amazonaws.com.
     * When using this action with an access point through the Amazon Web Services SDKs, you provide the access point
     * ARN in place of the bucket name. For more information about access point ARNs, see <a href="https://docs.aws.amazon.com/AmazonS3/latest/userguide/using-access-points.html">Using access points</a> in
     * the <i>Amazon S3 User Guide</i>.
     *
     * <p>Object Lambda access points are not supported by directory buckets.
     *
     * <p><b>S3 on Outposts</b> - When you use this action with S3 on Outposts, you must direct requests to the S3 on
     * Outposts hostname. The S3 on Outposts hostname takes the form <code><i>AccessPointName</i>-<i>AccountId</i>.<i>
     * outpostID</i>.s3-outposts.<i>Region</i>.amazonaws.com</code>. When you use this action with S3 on Outposts, the
     * destination bucket must be the Outposts access point ARN or the access point alias. For more information about S3
     * on Outposts, see <a href="https://docs.aws.amazon.com/AmazonS3/latest/userguide/S3onOutposts.html">What is S3 on Outposts?</a> in the <i>Amazon S3 User Guide</i>.
     */
    public String getBucket() {
        return bucket;
    }

    /**
     * Object key for which the multipart upload was initiated.
     */
    public String getKey() {
        return key;
    }

    /**
     * The container for the multipart upload request information.
     */
    public CompletedMultipartUpload getMultipartUpload() {
        return multipartUpload;
    }

    /**
     * ID for the initiated multipart upload.
     */
    public String getUploadId() {
        return uploadId;
    }

    /**
     * This header can be used as a data integrity check to verify that the data received is the same data that was
     * originally sent. This header specifies the Base64 encoded, 32-bit <code>CRC32</code> checksum of the object. For
     * more information, see <a href="https://docs.aws.amazon.com/AmazonS3/latest/userguide/checking-object-integrity.html">Checking object integrity</a> in the <i>Amazon S3 User Guide</i>.
     */
    public String getChecksumcrC32() {
        return checksumcrC32;
    }

    /**
     * This header can be used as a data integrity check to verify that the data received is the same data that was
     * originally sent. This header specifies the Base64 encoded, 32-bit <code>CRC32C</code> checksum of the object. For
     * more information, see <a href="https://docs.aws.amazon.com/AmazonS3/latest/userguide/checking-object-integrity.html">Checking object integrity</a> in the <i>Amazon S3 User Guide</i>.
     */
    public String getChecksumcrC32C() {
        return checksumcrC32C;
    }

    /**
     * This header can be used as a data integrity check to verify that the data received is the same data that was
     * originally sent. This header specifies the Base64 encoded, 64-bit <code>CRC64NVME</code> checksum of the object.
     * The <code>CRC64NVME</code> checksum is always a full object checksum. For more information, see <a
     * href="https://docs.aws.amazon.com/AmazonS3/latest/userguide/checking-object-integrity.html">Checking object
     * integrity in the Amazon S3 User Guide</a>.
     */
    public String getChecksumcrC64nvmE() {
        return checksumcrC64nvmE;
    }

    /**
     * This header can be used as a data integrity check to verify that the data received is the same data that was
     * originally sent. This header specifies the Base64 encoded, 160-bit <code>SHA1</code> digest of the object. For
     * more information, see <a href="https://docs.aws.amazon.com/AmazonS3/latest/userguide/checking-object-integrity.html">Checking object integrity</a> in the <i>Amazon S3 User Guide</i>.
     */
    public String getChecksumshA1() {
        return checksumshA1;
    }

    /**
     * This header can be used as a data integrity check to verify that the data received is the same data that was
     * originally sent. This header specifies the Base64 encoded, 256-bit <code>SHA256</code> digest of the object. For
     * more information, see <a href="https://docs.aws.amazon.com/AmazonS3/latest/userguide/checking-object-integrity.html">Checking object integrity</a> in the <i>Amazon S3 User Guide</i>.
     */
    public String getChecksumshA256() {
        return checksumshA256;
    }

    /**
     * This header can be used as a data integrity check to verify that the data received is the same data that was
     * originally sent. This header specifies the Base64 encoded, 512-bit <code>SHA512</code> digest of the object. For
     * more information, see <a
     * href="https://docs.aws.amazon.com/AmazonS3/latest/userguide/checking-object-integrity.html">Checking object
     * integrity in the Amazon S3 User Guide</a>.
     */
    public String getChecksumshA512() {
        return checksumshA512;
    }

    /**
     * This header can be used as a data integrity check to verify that the data received is the same data that was
     * originally sent. This header specifies the Base64 encoded, 128-bit <code>MD5</code> digest of the object. For
     * more information, see <a
     * href="https://docs.aws.amazon.com/AmazonS3/latest/userguide/checking-object-integrity.html">Checking object
     * integrity in the Amazon S3 User Guide</a>.
     */
    public String getChecksummD5() {
        return checksummD5;
    }

    /**
     * This header can be used as a data integrity check to verify that the data received is the same data that was
     * originally sent. This header specifies the Base64 encoded, 64-bit <code>XXHASH64</code> checksum of the object.
     * For more information, see <a
     * href="https://docs.aws.amazon.com/AmazonS3/latest/userguide/checking-object-integrity.html">Checking object
     * integrity in the Amazon S3 User Guide</a>.
     */
    public String getChecksumxxhasH64() {
        return checksumxxhasH64;
    }

    /**
     * This header can be used as a data integrity check to verify that the data received is the same data that was
     * originally sent. This header specifies the Base64 encoded, 64-bit <code>XXHASH3</code> checksum of the object.
     * For more information, see <a
     * href="https://docs.aws.amazon.com/AmazonS3/latest/userguide/checking-object-integrity.html">Checking object
     * integrity in the Amazon S3 User Guide</a>.
     */
    public String getChecksumxxhasH3() {
        return checksumxxhasH3;
    }

    /**
     * This header can be used as a data integrity check to verify that the data received is the same data that was
     * originally sent. This header specifies the Base64 encoded, 128-bit <code>XXHASH128</code> checksum of the object.
     * For more information, see <a
     * href="https://docs.aws.amazon.com/AmazonS3/latest/userguide/checking-object-integrity.html">Checking object
     * integrity in the Amazon S3 User Guide</a>.
     */
    public String getChecksumxxhasH128() {
        return checksumxxhasH128;
    }

    /**
     * This header specifies the checksum type of the object, which determines how part-level checksums are combined to
     * create an object-level checksum for multipart objects. You can use this header as a data integrity check to
     * verify that the checksum type that is received is the same checksum that was specified. If the checksum type
     * doesn’t match the checksum type that was specified for the object during the <code>CreateMultipartUpload</code>
     * request, it’ll result in a <code>BadDigest</code> error. For more information, see Checking object integrity in
     * the Amazon S3 User Guide.
     */
    public ChecksumType getChecksumType() {
        return checksumType;
    }

    /**
     * The expected total object size of the multipart upload request. If there’s a mismatch between the specified
     * object size value and the actual object size value, it results in an <code>HTTP 400 InvalidRequest</code> error.
     */
    public Long getMpuObjectSize() {
        return mpuObjectSize;
    }

    public RequestPayer getRequestPayer() {
        return requestPayer;
    }

    /**
     * The account ID of the expected bucket owner. If the account ID that you provide does not match the actual owner
     * of the bucket, the request fails with the HTTP status code <code>403 Forbidden</code> (access denied).
     */
    public String getExpectedBucketOwner() {
        return expectedBucketOwner;
    }

    /**
     * Uploads the object only if the ETag (entity tag) value provided during the WRITE operation matches the ETag of
     * the object in S3. If the ETag values do not match, the operation returns a <code>412 Precondition Failed</code>
     * error.
     *
     * <p>If a conflicting operation occurs during the upload S3 returns a <code>409 ConditionalRequestConflict</code>
     * response. On a 409 failure you should fetch the object's ETag, re-initiate the multipart upload with <code>
     * CreateMultipartUpload</code>, and re-upload each part.
     *
     * <p>Expects the ETag value as a string.
     *
     * <p>For more information about conditional requests, see <a href="https://tools.ietf.org/html/rfc7232">RFC 7232</a>, or <a href="https://docs.aws.amazon.com/AmazonS3/latest/userguide/conditional-requests.html">Conditional requests</a> in the <i>
     * Amazon S3 User Guide</i>.
     */
    public String getIfMatch() {
        return ifMatch;
    }

    /**
     * Uploads the object only if the object key name does not already exist in the bucket specified. Otherwise, Amazon
     * S3 returns a <code>412 Precondition Failed</code> error.
     *
     * <p>If a conflicting operation occurs during the upload S3 returns a <code>409 ConditionalRequestConflict</code>
     * response. On a 409 failure you should re-initiate the multipart upload with <code>CreateMultipartUpload</code>
     * and re-upload each part.
     *
     * <p>Expects the '&#42;' (asterisk) character.
     *
     * <p>For more information about conditional requests, see <a href="https://tools.ietf.org/html/rfc7232">RFC 7232</a>, or <a href="https://docs.aws.amazon.com/AmazonS3/latest/userguide/conditional-requests.html">Conditional requests</a> in the <i>
     * Amazon S3 User Guide</i>.
     */
    public String getIfNoneMatch() {
        return ifNoneMatch;
    }

    /**
     * The server-side encryption (SSE) algorithm used to encrypt the object. This parameter is required only when the
     * object was created using a checksum algorithm or if your bucket policy requires the use of SSE-C. For more
     * information, see <a href="https://docs.aws.amazon.com/AmazonS3/latest/userguide/ServerSideEncryptionCustomerKeys.html#ssec-require-condition-key">Protecting data using SSE-C keys</a> in the <i>Amazon S3 User Guide</i>.
     *
     * <p>This functionality is not supported for directory buckets.
     */
    public String getSseCustomerAlgorithm() {
        return sseCustomerAlgorithm;
    }

    /**
     * The server-side encryption (SSE) customer managed key. This parameter is needed only when the object was created
     * using a checksum algorithm. For more information, see <a href="https://docs.aws.amazon.com/AmazonS3/latest/dev/ServerSideEncryptionCustomerKeys.html">Protecting data using SSE-C keys</a> in the <i>Amazon S3
     * User Guide</i>.
     *
     * <p>This functionality is not supported for directory buckets.
     */
    public String getSseCustomerKey() {
        return sseCustomerKey;
    }

    /**
     * The MD5 server-side encryption (SSE) customer managed key. This parameter is needed only when the object was
     * created using a checksum algorithm. For more information, see <a href="https://docs.aws.amazon.com/AmazonS3/latest/dev/ServerSideEncryptionCustomerKeys.html">Protecting data using SSE-C keys</a> in the <i>
     * Amazon S3 User Guide</i>.
     *
     * <p>This functionality is not supported for directory buckets.
     */
    public String getSseCustomerKeymD5() {
        return sseCustomerKeymD5;
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
        CompleteMultipartUploadInput that = (CompleteMultipartUploadInput) other;
        return Objects.equals(this.mpuObjectSize, that.mpuObjectSize)
               && Objects.equals(this.bucket, that.bucket)
               && Objects.equals(this.key, that.key)
               && Objects.equals(this.uploadId, that.uploadId)
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
               && Objects.equals(this.expectedBucketOwner, that.expectedBucketOwner)
               && Objects.equals(this.ifMatch, that.ifMatch)
               && Objects.equals(this.ifNoneMatch, that.ifNoneMatch)
               && Objects.equals(this.sseCustomerAlgorithm, that.sseCustomerAlgorithm)
               && Objects.equals(this.sseCustomerKey, that.sseCustomerKey)
               && Objects.equals(this.sseCustomerKeymD5, that.sseCustomerKeymD5)
               && Objects.equals(this.checksumType, that.checksumType)
               && Objects.equals(this.requestPayer, that.requestPayer)
               && Objects.equals(this.multipartUpload, that.multipartUpload);
    }

    @Override
    public int hashCode() {
        int $hc = Objects.hashCode(bucket);
        $hc = 31 * $hc + Objects.hashCode(key);
        $hc = 31 * $hc + Objects.hashCode(multipartUpload);
        $hc = 31 * $hc + Objects.hashCode(uploadId);
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
        $hc = 31 * $hc + Objects.hashCode(mpuObjectSize);
        $hc = 31 * $hc + Objects.hashCode(requestPayer);
        $hc = 31 * $hc + Objects.hashCode(expectedBucketOwner);
        $hc = 31 * $hc + Objects.hashCode(ifMatch);
        $hc = 31 * $hc + Objects.hashCode(ifNoneMatch);
        $hc = 31 * $hc + Objects.hashCode(sseCustomerAlgorithm);
        $hc = 31 * $hc + Objects.hashCode(sseCustomerKey);
        $hc = 31 * $hc + Objects.hashCode(sseCustomerKeymD5);
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
        if (multipartUpload != null) {
            serializer.writeStruct($SCHEMA_MULTIPART_UPLOAD, multipartUpload);
        }
        serializer.writeString($SCHEMA_UPLOAD_ID, uploadId);
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
        if (mpuObjectSize != null) {
            serializer.writeLong($SCHEMA_MPU_OBJECT_SIZE, mpuObjectSize);
        }
        if (requestPayer != null) {
            serializer.writeString($SCHEMA_REQUEST_PAYER, requestPayer.getValue());
        }
        if (expectedBucketOwner != null) {
            serializer.writeString($SCHEMA_EXPECTED_BUCKET_OWNER, expectedBucketOwner);
        }
        if (ifMatch != null) {
            serializer.writeString($SCHEMA_IF_MATCH, ifMatch);
        }
        if (ifNoneMatch != null) {
            serializer.writeString($SCHEMA_IF_NONE_MATCH, ifNoneMatch);
        }
        if (sseCustomerAlgorithm != null) {
            serializer.writeString($SCHEMA_SSE_CUSTOMER_ALGORITHM, sseCustomerAlgorithm);
        }
        if (sseCustomerKey != null) {
            serializer.writeString($SCHEMA_SSE_CUSTOMER_KEY, sseCustomerKey);
        }
        if (sseCustomerKeymD5 != null) {
            serializer.writeString($SCHEMA_SSE_CUSTOMER_KEYM_D5, sseCustomerKeymD5);
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T getMemberValue(Schema member) {
        return switch (member.memberIndex()) {
            case 0 -> (T) SchemaUtils.validateSameMember($SCHEMA_BUCKET, member, bucket);
            case 1 -> (T) SchemaUtils.validateSameMember($SCHEMA_KEY, member, key);
            case 2 -> (T) SchemaUtils.validateSameMember($SCHEMA_UPLOAD_ID, member, uploadId);
            case 3 -> (T) SchemaUtils.validateSameMember($SCHEMA_MULTIPART_UPLOAD, member, multipartUpload);
            case 4 -> (T) SchemaUtils.validateSameMember($SCHEMA_CHECKSUMCR_C32, member, checksumcrC32);
            case 5 -> (T) SchemaUtils.validateSameMember($SCHEMA_CHECKSUMCR_C32_C, member, checksumcrC32C);
            case 6 -> (T) SchemaUtils.validateSameMember($SCHEMA_CHECKSUMCR_C64NVM_E, member, checksumcrC64nvmE);
            case 7 -> (T) SchemaUtils.validateSameMember($SCHEMA_CHECKSUMSH_A1, member, checksumshA1);
            case 8 -> (T) SchemaUtils.validateSameMember($SCHEMA_CHECKSUMSH_A256, member, checksumshA256);
            case 9 -> (T) SchemaUtils.validateSameMember($SCHEMA_CHECKSUMSH_A512, member, checksumshA512);
            case 10 -> (T) SchemaUtils.validateSameMember($SCHEMA_CHECKSUMM_D5, member, checksummD5);
            case 11 -> (T) SchemaUtils.validateSameMember($SCHEMA_CHECKSUMXXHAS_H64, member, checksumxxhasH64);
            case 12 -> (T) SchemaUtils.validateSameMember($SCHEMA_CHECKSUMXXHAS_H3, member, checksumxxhasH3);
            case 13 -> (T) SchemaUtils.validateSameMember($SCHEMA_CHECKSUMXXHAS_H128, member, checksumxxhasH128);
            case 14 -> (T) SchemaUtils.validateSameMember($SCHEMA_CHECKSUM_TYPE, member, checksumType);
            case 15 -> (T) SchemaUtils.validateSameMember($SCHEMA_MPU_OBJECT_SIZE, member, mpuObjectSize);
            case 16 -> (T) SchemaUtils.validateSameMember($SCHEMA_REQUEST_PAYER, member, requestPayer);
            case 17 -> (T) SchemaUtils.validateSameMember($SCHEMA_EXPECTED_BUCKET_OWNER, member, expectedBucketOwner);
            case 18 -> (T) SchemaUtils.validateSameMember($SCHEMA_IF_MATCH, member, ifMatch);
            case 19 -> (T) SchemaUtils.validateSameMember($SCHEMA_IF_NONE_MATCH, member, ifNoneMatch);
            case 20 -> (T) SchemaUtils.validateSameMember($SCHEMA_SSE_CUSTOMER_ALGORITHM, member, sseCustomerAlgorithm);
            case 21 -> (T) SchemaUtils.validateSameMember($SCHEMA_SSE_CUSTOMER_KEY, member, sseCustomerKey);
            case 22 -> (T) SchemaUtils.validateSameMember($SCHEMA_SSE_CUSTOMER_KEYM_D5, member, sseCustomerKeymD5);
            default -> throw new IllegalArgumentException("Attempted to get non-existent member: " + member.id());
        };
    }

    /**
     * Create a new builder containing all the current property values of this object.
     *
     * <p><strong>Note:</strong> This method performs only a shallow copy of the original properties.
     *
     * @return a builder for {@link CompleteMultipartUploadInput}.
     */
    public Builder toBuilder() {
        var builder = new Builder();
        builder.bucket(this.bucket);
        builder.key(this.key);
        builder.multipartUpload(this.multipartUpload);
        builder.uploadId(this.uploadId);
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
        builder.mpuObjectSize(this.mpuObjectSize);
        builder.requestPayer(this.requestPayer);
        builder.expectedBucketOwner(this.expectedBucketOwner);
        builder.ifMatch(this.ifMatch);
        builder.ifNoneMatch(this.ifNoneMatch);
        builder.sseCustomerAlgorithm(this.sseCustomerAlgorithm);
        builder.sseCustomerKey(this.sseCustomerKey);
        builder.sseCustomerKeymD5(this.sseCustomerKeymD5);
        return builder;
    }

    /**
     * @return returns a new Builder.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for {@link CompleteMultipartUploadInput}.
     */
    public static final class Builder implements ShapeBuilder<CompleteMultipartUploadInput> {
        private final PresenceTracker tracker = PresenceTracker.of($SCHEMA);
        private String bucket;
        private String key;
        private CompletedMultipartUpload multipartUpload;
        private String uploadId;
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
        private Long mpuObjectSize;
        private RequestPayer requestPayer;
        private String expectedBucketOwner;
        private String ifMatch;
        private String ifNoneMatch;
        private String sseCustomerAlgorithm;
        private String sseCustomerKey;
        private String sseCustomerKeymD5;

        private Builder() {}

        @Override
        public Schema schema() {
            return $SCHEMA;
        }

        /**
         * Name of the bucket to which the multipart upload was initiated.
         *
         * <p><b>Directory buckets</b> - When you use this operation with a directory bucket, you must use
         * virtual-hosted-style requests in the format <code><i>Bucket-name</i>.s3express-<i>zone-id</i>.<i>region-code</i>
         * .amazonaws.com</code>. Path-style requests are not supported. Directory bucket names must be unique in the chosen
         * Zone (Availability Zone or Local Zone). Bucket names must follow the format <code><i>bucket-base-name</i>--<i>
         * zone-id</i>--x-s3</code> (for example, <code><i>amzn-s3-demo-bucket</i>--<i>usw2-az1</i>--x-s3</code>). For
         * information about bucket naming restrictions, see <a href="https://docs.aws.amazon.com/AmazonS3/latest/userguide/directory-bucket-naming-rules.html">Directory bucket naming rules</a> in the <i>Amazon S3 User
         * Guide</i>.
         *
         * <p><b>Access points</b> - When you use this action with an access point for general purpose buckets, you must
         * provide the alias of the access point in place of the bucket name or specify the access point ARN. When you use
         * this action with an access point for directory buckets, you must provide the access point name in place of the
         * bucket name. When using the access point ARN, you must direct requests to the access point hostname. The access
         * point hostname takes the form <i>AccessPointName</i>-<i>AccountId</i>.s3-accesspoint.<i>Region</i>.amazonaws.com.
         * When using this action with an access point through the Amazon Web Services SDKs, you provide the access point
         * ARN in place of the bucket name. For more information about access point ARNs, see <a href="https://docs.aws.amazon.com/AmazonS3/latest/userguide/using-access-points.html">Using access points</a> in
         * the <i>Amazon S3 User Guide</i>.
         *
         * <p>Object Lambda access points are not supported by directory buckets.
         *
         * <p><b>S3 on Outposts</b> - When you use this action with S3 on Outposts, you must direct requests to the S3 on
         * Outposts hostname. The S3 on Outposts hostname takes the form <code><i>AccessPointName</i>-<i>AccountId</i>.<i>
         * outpostID</i>.s3-outposts.<i>Region</i>.amazonaws.com</code>. When you use this action with S3 on Outposts, the
         * destination bucket must be the Outposts access point ARN or the access point alias. For more information about S3
         * on Outposts, see <a href="https://docs.aws.amazon.com/AmazonS3/latest/userguide/S3onOutposts.html">What is S3 on Outposts?</a> in the <i>Amazon S3 User Guide</i>.
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
         * Object key for which the multipart upload was initiated.
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
         * The container for the multipart upload request information.
         *
         * @return this builder.
         */
        public Builder multipartUpload(CompletedMultipartUpload multipartUpload) {
            this.multipartUpload = multipartUpload;
            return this;
        }

        /**
         * ID for the initiated multipart upload.
         *
         * <p><strong>Required</strong>
         * @return this builder.
         */
        public Builder uploadId(String uploadId) {
            this.uploadId = Objects.requireNonNull(uploadId, "uploadId cannot be null");
            tracker.setMember($SCHEMA_UPLOAD_ID);
            return this;
        }

        /**
         * This header can be used as a data integrity check to verify that the data received is the same data that was
         * originally sent. This header specifies the Base64 encoded, 32-bit <code>CRC32</code> checksum of the object. For
         * more information, see <a href="https://docs.aws.amazon.com/AmazonS3/latest/userguide/checking-object-integrity.html">Checking object integrity</a> in the <i>Amazon S3 User Guide</i>.
         *
         * @return this builder.
         */
        public Builder checksumcrC32(String checksumcrC32) {
            this.checksumcrC32 = checksumcrC32;
            return this;
        }

        /**
         * This header can be used as a data integrity check to verify that the data received is the same data that was
         * originally sent. This header specifies the Base64 encoded, 32-bit <code>CRC32C</code> checksum of the object. For
         * more information, see <a href="https://docs.aws.amazon.com/AmazonS3/latest/userguide/checking-object-integrity.html">Checking object integrity</a> in the <i>Amazon S3 User Guide</i>.
         *
         * @return this builder.
         */
        public Builder checksumcrC32C(String checksumcrC32C) {
            this.checksumcrC32C = checksumcrC32C;
            return this;
        }

        /**
         * This header can be used as a data integrity check to verify that the data received is the same data that was
         * originally sent. This header specifies the Base64 encoded, 64-bit <code>CRC64NVME</code> checksum of the object.
         * The <code>CRC64NVME</code> checksum is always a full object checksum. For more information, see <a
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
         * This header can be used as a data integrity check to verify that the data received is the same data that was
         * originally sent. This header specifies the Base64 encoded, 160-bit <code>SHA1</code> digest of the object. For
         * more information, see <a href="https://docs.aws.amazon.com/AmazonS3/latest/userguide/checking-object-integrity.html">Checking object integrity</a> in the <i>Amazon S3 User Guide</i>.
         *
         * @return this builder.
         */
        public Builder checksumshA1(String checksumshA1) {
            this.checksumshA1 = checksumshA1;
            return this;
        }

        /**
         * This header can be used as a data integrity check to verify that the data received is the same data that was
         * originally sent. This header specifies the Base64 encoded, 256-bit <code>SHA256</code> digest of the object. For
         * more information, see <a href="https://docs.aws.amazon.com/AmazonS3/latest/userguide/checking-object-integrity.html">Checking object integrity</a> in the <i>Amazon S3 User Guide</i>.
         *
         * @return this builder.
         */
        public Builder checksumshA256(String checksumshA256) {
            this.checksumshA256 = checksumshA256;
            return this;
        }

        /**
         * This header can be used as a data integrity check to verify that the data received is the same data that was
         * originally sent. This header specifies the Base64 encoded, 512-bit <code>SHA512</code> digest of the object. For
         * more information, see <a
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
         * This header can be used as a data integrity check to verify that the data received is the same data that was
         * originally sent. This header specifies the Base64 encoded, 128-bit <code>MD5</code> digest of the object. For
         * more information, see <a
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
         * This header can be used as a data integrity check to verify that the data received is the same data that was
         * originally sent. This header specifies the Base64 encoded, 64-bit <code>XXHASH64</code> checksum of the object.
         * For more information, see <a
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
         * This header can be used as a data integrity check to verify that the data received is the same data that was
         * originally sent. This header specifies the Base64 encoded, 64-bit <code>XXHASH3</code> checksum of the object.
         * For more information, see <a
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
         * This header can be used as a data integrity check to verify that the data received is the same data that was
         * originally sent. This header specifies the Base64 encoded, 128-bit <code>XXHASH128</code> checksum of the object.
         * For more information, see <a
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
         * create an object-level checksum for multipart objects. You can use this header as a data integrity check to
         * verify that the checksum type that is received is the same checksum that was specified. If the checksum type
         * doesn’t match the checksum type that was specified for the object during the <code>CreateMultipartUpload</code>
         * request, it’ll result in a <code>BadDigest</code> error. For more information, see Checking object integrity in
         * the Amazon S3 User Guide.
         *
         * @return this builder.
         */
        public Builder checksumType(ChecksumType checksumType) {
            this.checksumType = checksumType;
            return this;
        }

        /**
         * The expected total object size of the multipart upload request. If there’s a mismatch between the specified
         * object size value and the actual object size value, it results in an <code>HTTP 400 InvalidRequest</code> error.
         *
         * @return this builder.
         */
        public Builder mpuObjectSize(Long mpuObjectSize) {
            this.mpuObjectSize = mpuObjectSize;
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
         * The account ID of the expected bucket owner. If the account ID that you provide does not match the actual owner
         * of the bucket, the request fails with the HTTP status code <code>403 Forbidden</code> (access denied).
         *
         * @return this builder.
         */
        public Builder expectedBucketOwner(String expectedBucketOwner) {
            this.expectedBucketOwner = expectedBucketOwner;
            return this;
        }

        /**
         * Uploads the object only if the ETag (entity tag) value provided during the WRITE operation matches the ETag of
         * the object in S3. If the ETag values do not match, the operation returns a <code>412 Precondition Failed</code>
         * error.
         *
         * <p>If a conflicting operation occurs during the upload S3 returns a <code>409 ConditionalRequestConflict</code>
         * response. On a 409 failure you should fetch the object's ETag, re-initiate the multipart upload with <code>
         * CreateMultipartUpload</code>, and re-upload each part.
         *
         * <p>Expects the ETag value as a string.
         *
         * <p>For more information about conditional requests, see <a href="https://tools.ietf.org/html/rfc7232">RFC 7232</a>, or <a href="https://docs.aws.amazon.com/AmazonS3/latest/userguide/conditional-requests.html">Conditional requests</a> in the <i>
         * Amazon S3 User Guide</i>.
         *
         * @return this builder.
         */
        public Builder ifMatch(String ifMatch) {
            this.ifMatch = ifMatch;
            return this;
        }

        /**
         * Uploads the object only if the object key name does not already exist in the bucket specified. Otherwise, Amazon
         * S3 returns a <code>412 Precondition Failed</code> error.
         *
         * <p>If a conflicting operation occurs during the upload S3 returns a <code>409 ConditionalRequestConflict</code>
         * response. On a 409 failure you should re-initiate the multipart upload with <code>CreateMultipartUpload</code>
         * and re-upload each part.
         *
         * <p>Expects the '&#42;' (asterisk) character.
         *
         * <p>For more information about conditional requests, see <a href="https://tools.ietf.org/html/rfc7232">RFC 7232</a>, or <a href="https://docs.aws.amazon.com/AmazonS3/latest/userguide/conditional-requests.html">Conditional requests</a> in the <i>
         * Amazon S3 User Guide</i>.
         *
         * @return this builder.
         */
        public Builder ifNoneMatch(String ifNoneMatch) {
            this.ifNoneMatch = ifNoneMatch;
            return this;
        }

        /**
         * The server-side encryption (SSE) algorithm used to encrypt the object. This parameter is required only when the
         * object was created using a checksum algorithm or if your bucket policy requires the use of SSE-C. For more
         * information, see <a href="https://docs.aws.amazon.com/AmazonS3/latest/userguide/ServerSideEncryptionCustomerKeys.html#ssec-require-condition-key">Protecting data using SSE-C keys</a> in the <i>Amazon S3 User Guide</i>.
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
         * The server-side encryption (SSE) customer managed key. This parameter is needed only when the object was created
         * using a checksum algorithm. For more information, see <a href="https://docs.aws.amazon.com/AmazonS3/latest/dev/ServerSideEncryptionCustomerKeys.html">Protecting data using SSE-C keys</a> in the <i>Amazon S3
         * User Guide</i>.
         *
         * <p>This functionality is not supported for directory buckets.
         *
         * @return this builder.
         */
        public Builder sseCustomerKey(String sseCustomerKey) {
            this.sseCustomerKey = sseCustomerKey;
            return this;
        }

        /**
         * The MD5 server-side encryption (SSE) customer managed key. This parameter is needed only when the object was
         * created using a checksum algorithm. For more information, see <a href="https://docs.aws.amazon.com/AmazonS3/latest/dev/ServerSideEncryptionCustomerKeys.html">Protecting data using SSE-C keys</a> in the <i>
         * Amazon S3 User Guide</i>.
         *
         * <p>This functionality is not supported for directory buckets.
         *
         * @return this builder.
         */
        public Builder sseCustomerKeymD5(String sseCustomerKeymD5) {
            this.sseCustomerKeymD5 = sseCustomerKeymD5;
            return this;
        }

        @Override
        public CompleteMultipartUploadInput build() {
            tracker.validate();
            return new CompleteMultipartUploadInput(this);
        }

        @Override
        @SuppressWarnings("unchecked")
        public void setMemberValue(Schema member, Object value) {
            switch (member.memberIndex()) {
                case 0 -> bucket((String) SchemaUtils.validateSameMember($SCHEMA_BUCKET, member, value));
                case 1 -> key((String) SchemaUtils.validateSameMember($SCHEMA_KEY, member, value));
                case 2 -> uploadId((String) SchemaUtils.validateSameMember($SCHEMA_UPLOAD_ID, member, value));
                case 3 -> multipartUpload((CompletedMultipartUpload) SchemaUtils.validateSameMember($SCHEMA_MULTIPART_UPLOAD, member, value));
                case 4 -> checksumcrC32((String) SchemaUtils.validateSameMember($SCHEMA_CHECKSUMCR_C32, member, value));
                case 5 -> checksumcrC32C((String) SchemaUtils.validateSameMember($SCHEMA_CHECKSUMCR_C32_C, member, value));
                case 6 -> checksumcrC64nvmE((String) SchemaUtils.validateSameMember($SCHEMA_CHECKSUMCR_C64NVM_E, member, value));
                case 7 -> checksumshA1((String) SchemaUtils.validateSameMember($SCHEMA_CHECKSUMSH_A1, member, value));
                case 8 -> checksumshA256((String) SchemaUtils.validateSameMember($SCHEMA_CHECKSUMSH_A256, member, value));
                case 9 -> checksumshA512((String) SchemaUtils.validateSameMember($SCHEMA_CHECKSUMSH_A512, member, value));
                case 10 -> checksummD5((String) SchemaUtils.validateSameMember($SCHEMA_CHECKSUMM_D5, member, value));
                case 11 -> checksumxxhasH64((String) SchemaUtils.validateSameMember($SCHEMA_CHECKSUMXXHAS_H64, member, value));
                case 12 -> checksumxxhasH3((String) SchemaUtils.validateSameMember($SCHEMA_CHECKSUMXXHAS_H3, member, value));
                case 13 -> checksumxxhasH128((String) SchemaUtils.validateSameMember($SCHEMA_CHECKSUMXXHAS_H128, member, value));
                case 14 -> checksumType((ChecksumType) SchemaUtils.validateSameMember($SCHEMA_CHECKSUM_TYPE, member, value));
                case 15 -> mpuObjectSize((Long) SchemaUtils.validateSameMember($SCHEMA_MPU_OBJECT_SIZE, member, value));
                case 16 -> requestPayer((RequestPayer) SchemaUtils.validateSameMember($SCHEMA_REQUEST_PAYER, member, value));
                case 17 -> expectedBucketOwner((String) SchemaUtils.validateSameMember($SCHEMA_EXPECTED_BUCKET_OWNER, member, value));
                case 18 -> ifMatch((String) SchemaUtils.validateSameMember($SCHEMA_IF_MATCH, member, value));
                case 19 -> ifNoneMatch((String) SchemaUtils.validateSameMember($SCHEMA_IF_NONE_MATCH, member, value));
                case 20 -> sseCustomerAlgorithm((String) SchemaUtils.validateSameMember($SCHEMA_SSE_CUSTOMER_ALGORITHM, member, value));
                case 21 -> sseCustomerKey((String) SchemaUtils.validateSameMember($SCHEMA_SSE_CUSTOMER_KEY, member, value));
                case 22 -> sseCustomerKeymD5((String) SchemaUtils.validateSameMember($SCHEMA_SSE_CUSTOMER_KEYM_D5, member, value));
                default -> ShapeBuilder.super.setMemberValue(member, value);
            }
        }

        @Override
        public ShapeBuilder<CompleteMultipartUploadInput> errorCorrection() {
            if (tracker.allSet()) {
                return this;
            }
            if (!tracker.checkMember($SCHEMA_BUCKET)) {
                bucket("");
            }
            if (!tracker.checkMember($SCHEMA_KEY)) {
                key("");
            }
            if (!tracker.checkMember($SCHEMA_UPLOAD_ID)) {
                uploadId("");
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
                    case 2 -> builder.uploadId(de.readString(member));
                    case 3 -> builder.multipartUpload(CompletedMultipartUpload.builder().deserializeMember(de, member).build());
                    case 4 -> builder.checksumcrC32(de.readString(member));
                    case 5 -> builder.checksumcrC32C(de.readString(member));
                    case 6 -> builder.checksumcrC64nvmE(de.readString(member));
                    case 7 -> builder.checksumshA1(de.readString(member));
                    case 8 -> builder.checksumshA256(de.readString(member));
                    case 9 -> builder.checksumshA512(de.readString(member));
                    case 10 -> builder.checksummD5(de.readString(member));
                    case 11 -> builder.checksumxxhasH64(de.readString(member));
                    case 12 -> builder.checksumxxhasH3(de.readString(member));
                    case 13 -> builder.checksumxxhasH128(de.readString(member));
                    case 14 -> builder.checksumType(ChecksumType.builder().deserializeMember(de, member).build());
                    case 15 -> builder.mpuObjectSize(de.readLong(member));
                    case 16 -> builder.requestPayer(RequestPayer.builder().deserializeMember(de, member).build());
                    case 17 -> builder.expectedBucketOwner(de.readString(member));
                    case 18 -> builder.ifMatch(de.readString(member));
                    case 19 -> builder.ifNoneMatch(de.readString(member));
                    case 20 -> builder.sseCustomerAlgorithm(de.readString(member));
                    case 21 -> builder.sseCustomerKey(de.readString(member));
                    case 22 -> builder.sseCustomerKeymD5(de.readString(member));
                    default -> throw new IllegalArgumentException("Unexpected member: " + member.memberName());
                }
            }
        }
    }
}
