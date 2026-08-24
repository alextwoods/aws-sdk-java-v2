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
public final class UploadPartInput implements SerializableStruct, Closeable {

    public static final Schema $SCHEMA = Schemas7.UPLOAD_PART_INPUT;
    private static final Schema $SCHEMA_BODY = $SCHEMA.member("Body");
    private static final Schema $SCHEMA_BUCKET = $SCHEMA.member("Bucket");
    private static final Schema $SCHEMA_CONTENT_LENGTH = $SCHEMA.member("ContentLength");
    private static final Schema $SCHEMA_CONTENTM_D5 = $SCHEMA.member("ContentMD5");
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
    private static final Schema $SCHEMA_KEY = $SCHEMA.member("Key");
    private static final Schema $SCHEMA_PART_NUMBER = $SCHEMA.member("PartNumber");
    private static final Schema $SCHEMA_UPLOAD_ID = $SCHEMA.member("UploadId");
    private static final Schema $SCHEMA_SSE_CUSTOMER_ALGORITHM = $SCHEMA.member("SSECustomerAlgorithm");
    private static final Schema $SCHEMA_SSE_CUSTOMER_KEY = $SCHEMA.member("SSECustomerKey");
    private static final Schema $SCHEMA_SSE_CUSTOMER_KEYM_D5 = $SCHEMA.member("SSECustomerKeyMD5");
    private static final Schema $SCHEMA_REQUEST_PAYER = $SCHEMA.member("RequestPayer");
    private static final Schema $SCHEMA_EXPECTED_BUCKET_OWNER = $SCHEMA.member("ExpectedBucketOwner");

    public static final ShapeId $ID = $SCHEMA.id();

    private final transient DataStream body;
    private final transient String bucket;
    private final transient Long contentLength;
    private final transient String contentmD5;
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
    private final transient String key;
    private final transient int partNumber;
    private final transient String uploadId;
    private final transient String sseCustomerAlgorithm;
    private final transient String sseCustomerKey;
    private final transient String sseCustomerKeymD5;
    private final transient RequestPayer requestPayer;
    private final transient String expectedBucketOwner;

    private UploadPartInput(Builder builder) {
        this.body = builder.body;
        this.bucket = builder.bucket;
        this.contentLength = builder.contentLength;
        this.contentmD5 = builder.contentmD5;
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
        this.key = builder.key;
        this.partNumber = builder.partNumber;
        this.uploadId = builder.uploadId;
        this.sseCustomerAlgorithm = builder.sseCustomerAlgorithm;
        this.sseCustomerKey = builder.sseCustomerKey;
        this.sseCustomerKeymD5 = builder.sseCustomerKeymD5;
        this.requestPayer = builder.requestPayer;
        this.expectedBucketOwner = builder.expectedBucketOwner;
    }

    /**
     * Object data.
     */
    public DataStream getBody() {
        return body;
    }

    /**
     * The name of the bucket to which the multipart upload was initiated.
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
     * Size of the body in bytes. This parameter is useful when the size of the body cannot be determined automatically.
     */
    public Long getContentLength() {
        return contentLength;
    }

    /**
     * The Base64 encoded 128-bit MD5 digest of the part data. This parameter is auto-populated when using the command
     * from the CLI. This parameter is required if object lock parameters are specified.
     *
     * <p>This functionality is not supported for directory buckets.
     */
    public String getContentmD5() {
        return contentmD5;
    }

    /**
     * Indicates the algorithm used to create the checksum for the object when you use the SDK. This header will not
     * provide any additional functionality if you don't use the SDK. When you send this header, there must be a
     * corresponding <code>x-amz-checksum</code> or <code>x-amz-trailer</code> header sent. Otherwise, Amazon S3 fails
     * the request with the HTTP status code <code>400 Bad Request</code>. For more information, see <a href="https://docs.aws.amazon.com/AmazonS3/latest/userguide/checking-object-integrity.html">Checking object
     * integrity</a> in the <i>Amazon S3 User Guide</i>.
     *
     * <p>If you provide an individual checksum, Amazon S3 ignores any provided <code>ChecksumAlgorithm</code>
     * parameter.
     *
     * <p>This checksum algorithm must be the same for all parts and it match the checksum value supplied in the <code>
     * CreateMultipartUpload</code> request.
     */
    public ChecksumAlgorithm getChecksumAlgorithm() {
        return checksumAlgorithm;
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
     * originally sent. This header specifies the Base64 encoded, 64-bit <code>CRC64NVME</code> checksum of the part.
     * For more information, see <a href="https://docs.aws.amazon.com/AmazonS3/latest/userguide/checking-object-integrity.html">Checking object integrity</a> in the <i>Amazon S3 User Guide</i>.
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
     * originally sent. This header specifies the Base64 encoded, 512-bit <code>SHA512</code> digest of the part. For
     * more information, see <a href="https://docs.aws.amazon.com/AmazonS3/latest/userguide/checking-object-integrity.html">Checking object integrity</a> in the <i>Amazon S3 User Guide</i>.
     */
    public String getChecksumshA512() {
        return checksumshA512;
    }

    /**
     * This header can be used as a data integrity check to verify that the data received is the same data that was
     * originally sent. This header specifies the Base64 encoded, 128-bit <code>MD5</code> digest of the part. For more
     * information, see <a href="https://docs.aws.amazon.com/AmazonS3/latest/userguide/checking-object-integrity.html">Checking object integrity</a> in the <i>Amazon S3 User Guide</i>.
     */
    public String getChecksummD5() {
        return checksummD5;
    }

    /**
     * This header can be used as a data integrity check to verify that the data received is the same data that was
     * originally sent. This header specifies the Base64 encoded, 64-bit <code>XXHASH64</code> checksum of the part. For
     * more information, see <a href="https://docs.aws.amazon.com/AmazonS3/latest/userguide/checking-object-integrity.html">Checking object integrity</a> in the <i>Amazon S3 User Guide</i>.
     */
    public String getChecksumxxhasH64() {
        return checksumxxhasH64;
    }

    /**
     * This header can be used as a data integrity check to verify that the data received is the same data that was
     * originally sent. This header specifies the Base64 encoded, 64-bit <code>XXHASH3</code> checksum of the part. For
     * more information, see <a href="https://docs.aws.amazon.com/AmazonS3/latest/userguide/checking-object-integrity.html">Checking object integrity</a> in the <i>Amazon S3 User Guide</i>.
     */
    public String getChecksumxxhasH3() {
        return checksumxxhasH3;
    }

    /**
     * This header can be used as a data integrity check to verify that the data received is the same data that was
     * originally sent. This header specifies the Base64 encoded, 128-bit <code>XXHASH128</code> checksum of the part.
     * For more information, see <a href="https://docs.aws.amazon.com/AmazonS3/latest/userguide/checking-object-integrity.html">Checking object integrity</a> in the <i>Amazon S3 User Guide</i>.
     */
    public String getChecksumxxhasH128() {
        return checksumxxhasH128;
    }

    /**
     * Object key for which the multipart upload was initiated.
     */
    public String getKey() {
        return key;
    }

    /**
     * Part number of part being uploaded. This is a positive integer between 1 and 10,000.
     */
    public int getPartNumber() {
        return partNumber;
    }

    /**
     * Upload ID identifying the multipart upload whose part is being uploaded.
     */
    public String getUploadId() {
        return uploadId;
    }

    /**
     * Specifies the algorithm to use when encrypting the object (for example, AES256).
     *
     * <p>This functionality is not supported for directory buckets.
     */
    public String getSseCustomerAlgorithm() {
        return sseCustomerAlgorithm;
    }

    /**
     * Specifies the customer-provided encryption key for Amazon S3 to use in encrypting data. This value is used to
     * store the object and then it is discarded; Amazon S3 does not store the encryption key. The key must be
     * appropriate for use with the algorithm specified in the <code>x-amz-server-side-encryption-customer-algorithm
     * header</code>. This must be the same encryption key specified in the initiate multipart upload request.
     *
     * <p>This functionality is not supported for directory buckets.
     */
    public String getSseCustomerKey() {
        return sseCustomerKey;
    }

    /**
     * Specifies the 128-bit MD5 digest of the encryption key according to RFC 1321. Amazon S3 uses this header for a
     * message integrity check to ensure that the encryption key was transmitted without error.
     *
     * <p>This functionality is not supported for directory buckets.
     */
    public String getSseCustomerKeymD5() {
        return sseCustomerKeymD5;
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
        UploadPartInput that = (UploadPartInput) other;
        return this.partNumber == that.partNumber
               && Objects.equals(this.contentLength, that.contentLength)
               && Objects.equals(this.bucket, that.bucket)
               && Objects.equals(this.contentmD5, that.contentmD5)
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
               && Objects.equals(this.key, that.key)
               && Objects.equals(this.uploadId, that.uploadId)
               && Objects.equals(this.sseCustomerAlgorithm, that.sseCustomerAlgorithm)
               && Objects.equals(this.sseCustomerKey, that.sseCustomerKey)
               && Objects.equals(this.sseCustomerKeymD5, that.sseCustomerKeymD5)
               && Objects.equals(this.expectedBucketOwner, that.expectedBucketOwner)
               && Objects.equals(this.checksumAlgorithm, that.checksumAlgorithm)
               && Objects.equals(this.requestPayer, that.requestPayer)
               && Objects.equals(this.body, that.body);
    }

    @Override
    public int hashCode() {
        int $hc = Objects.hashCode(body);
        $hc = 31 * $hc + Objects.hashCode(bucket);
        $hc = 31 * $hc + Objects.hashCode(contentLength);
        $hc = 31 * $hc + Objects.hashCode(contentmD5);
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
        $hc = 31 * $hc + Objects.hashCode(key);
        $hc = 31 * $hc + Integer.hashCode(partNumber);
        $hc = 31 * $hc + Objects.hashCode(uploadId);
        $hc = 31 * $hc + Objects.hashCode(sseCustomerAlgorithm);
        $hc = 31 * $hc + Objects.hashCode(sseCustomerKey);
        $hc = 31 * $hc + Objects.hashCode(sseCustomerKeymD5);
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
        serializer.writeDataStream($SCHEMA_BODY, body);
        serializer.writeString($SCHEMA_BUCKET, bucket);
        if (contentLength != null) {
            serializer.writeLong($SCHEMA_CONTENT_LENGTH, contentLength);
        }
        if (contentmD5 != null) {
            serializer.writeString($SCHEMA_CONTENTM_D5, contentmD5);
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
        serializer.writeString($SCHEMA_KEY, key);
        serializer.writeInteger($SCHEMA_PART_NUMBER, partNumber);
        serializer.writeString($SCHEMA_UPLOAD_ID, uploadId);
        if (sseCustomerAlgorithm != null) {
            serializer.writeString($SCHEMA_SSE_CUSTOMER_ALGORITHM, sseCustomerAlgorithm);
        }
        if (sseCustomerKey != null) {
            serializer.writeString($SCHEMA_SSE_CUSTOMER_KEY, sseCustomerKey);
        }
        if (sseCustomerKeymD5 != null) {
            serializer.writeString($SCHEMA_SSE_CUSTOMER_KEYM_D5, sseCustomerKeymD5);
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
            case 2 -> (T) SchemaUtils.validateSameMember($SCHEMA_PART_NUMBER, member, partNumber);
            case 3 -> (T) SchemaUtils.validateSameMember($SCHEMA_UPLOAD_ID, member, uploadId);
            case 4 -> (T) SchemaUtils.validateSameMember($SCHEMA_BODY, member, body);
            case 5 -> (T) SchemaUtils.validateSameMember($SCHEMA_CONTENT_LENGTH, member, contentLength);
            case 6 -> (T) SchemaUtils.validateSameMember($SCHEMA_CONTENTM_D5, member, contentmD5);
            case 7 -> (T) SchemaUtils.validateSameMember($SCHEMA_CHECKSUM_ALGORITHM, member, checksumAlgorithm);
            case 8 -> (T) SchemaUtils.validateSameMember($SCHEMA_CHECKSUMCR_C32, member, checksumcrC32);
            case 9 -> (T) SchemaUtils.validateSameMember($SCHEMA_CHECKSUMCR_C32_C, member, checksumcrC32C);
            case 10 -> (T) SchemaUtils.validateSameMember($SCHEMA_CHECKSUMCR_C64NVM_E, member, checksumcrC64nvmE);
            case 11 -> (T) SchemaUtils.validateSameMember($SCHEMA_CHECKSUMSH_A1, member, checksumshA1);
            case 12 -> (T) SchemaUtils.validateSameMember($SCHEMA_CHECKSUMSH_A256, member, checksumshA256);
            case 13 -> (T) SchemaUtils.validateSameMember($SCHEMA_CHECKSUMSH_A512, member, checksumshA512);
            case 14 -> (T) SchemaUtils.validateSameMember($SCHEMA_CHECKSUMM_D5, member, checksummD5);
            case 15 -> (T) SchemaUtils.validateSameMember($SCHEMA_CHECKSUMXXHAS_H64, member, checksumxxhasH64);
            case 16 -> (T) SchemaUtils.validateSameMember($SCHEMA_CHECKSUMXXHAS_H3, member, checksumxxhasH3);
            case 17 -> (T) SchemaUtils.validateSameMember($SCHEMA_CHECKSUMXXHAS_H128, member, checksumxxhasH128);
            case 18 -> (T) SchemaUtils.validateSameMember($SCHEMA_SSE_CUSTOMER_ALGORITHM, member, sseCustomerAlgorithm);
            case 19 -> (T) SchemaUtils.validateSameMember($SCHEMA_SSE_CUSTOMER_KEY, member, sseCustomerKey);
            case 20 -> (T) SchemaUtils.validateSameMember($SCHEMA_SSE_CUSTOMER_KEYM_D5, member, sseCustomerKeymD5);
            case 21 -> (T) SchemaUtils.validateSameMember($SCHEMA_REQUEST_PAYER, member, requestPayer);
            case 22 -> (T) SchemaUtils.validateSameMember($SCHEMA_EXPECTED_BUCKET_OWNER, member, expectedBucketOwner);
            default -> throw new IllegalArgumentException("Attempted to get non-existent member: " + member.id());
        };
    }

    /**
     * Closes the underlying stream.
     */
    @Override
    public void close() {
        if (body != null) {
            body.close();
        }
    }

    /**
     * Create a new builder containing all the current property values of this object.
     *
     * <p><strong>Note:</strong> This method performs only a shallow copy of the original properties.
     *
     * @return a builder for {@link UploadPartInput}.
     */
    public Builder toBuilder() {
        var builder = new Builder();
        builder.body(this.body);
        builder.bucket(this.bucket);
        builder.contentLength(this.contentLength);
        builder.contentmD5(this.contentmD5);
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
        builder.key(this.key);
        builder.partNumber(this.partNumber);
        builder.uploadId(this.uploadId);
        builder.sseCustomerAlgorithm(this.sseCustomerAlgorithm);
        builder.sseCustomerKey(this.sseCustomerKey);
        builder.sseCustomerKeymD5(this.sseCustomerKeymD5);
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
     * Builder for {@link UploadPartInput}.
     */
    public static final class Builder implements ShapeBuilder<UploadPartInput> {
        private final PresenceTracker tracker = PresenceTracker.of($SCHEMA);
        private DataStream body = DataStream.ofEmpty();
        private String bucket;
        private Long contentLength;
        private String contentmD5;
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
        private String key;
        private int partNumber;
        private String uploadId;
        private String sseCustomerAlgorithm;
        private String sseCustomerKey;
        private String sseCustomerKeymD5;
        private RequestPayer requestPayer;
        private String expectedBucketOwner;

        private Builder() {}

        @Override
        public Schema schema() {
            return $SCHEMA;
        }

        /**
         * Object data.
         *
         * @return this builder.
         */
        public Builder body(DataStream body) {
            this.body = Objects.requireNonNull(body, "body cannot be null");
            return this;
        }

        /**
         * The name of the bucket to which the multipart upload was initiated.
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
         * Size of the body in bytes. This parameter is useful when the size of the body cannot be determined automatically.
         *
         * @return this builder.
         */
        public Builder contentLength(Long contentLength) {
            this.contentLength = contentLength;
            return this;
        }

        /**
         * The Base64 encoded 128-bit MD5 digest of the part data. This parameter is auto-populated when using the command
         * from the CLI. This parameter is required if object lock parameters are specified.
         *
         * <p>This functionality is not supported for directory buckets.
         *
         * @return this builder.
         */
        public Builder contentmD5(String contentmD5) {
            this.contentmD5 = contentmD5;
            return this;
        }

        /**
         * Indicates the algorithm used to create the checksum for the object when you use the SDK. This header will not
         * provide any additional functionality if you don't use the SDK. When you send this header, there must be a
         * corresponding <code>x-amz-checksum</code> or <code>x-amz-trailer</code> header sent. Otherwise, Amazon S3 fails
         * the request with the HTTP status code <code>400 Bad Request</code>. For more information, see <a href="https://docs.aws.amazon.com/AmazonS3/latest/userguide/checking-object-integrity.html">Checking object
         * integrity</a> in the <i>Amazon S3 User Guide</i>.
         *
         * <p>If you provide an individual checksum, Amazon S3 ignores any provided <code>ChecksumAlgorithm</code>
         * parameter.
         *
         * <p>This checksum algorithm must be the same for all parts and it match the checksum value supplied in the <code>
         * CreateMultipartUpload</code> request.
         *
         * @return this builder.
         */
        public Builder checksumAlgorithm(ChecksumAlgorithm checksumAlgorithm) {
            this.checksumAlgorithm = checksumAlgorithm;
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
         * originally sent. This header specifies the Base64 encoded, 64-bit <code>CRC64NVME</code> checksum of the part.
         * For more information, see <a href="https://docs.aws.amazon.com/AmazonS3/latest/userguide/checking-object-integrity.html">Checking object integrity</a> in the <i>Amazon S3 User Guide</i>.
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
         * originally sent. This header specifies the Base64 encoded, 512-bit <code>SHA512</code> digest of the part. For
         * more information, see <a href="https://docs.aws.amazon.com/AmazonS3/latest/userguide/checking-object-integrity.html">Checking object integrity</a> in the <i>Amazon S3 User Guide</i>.
         *
         * @return this builder.
         */
        public Builder checksumshA512(String checksumshA512) {
            this.checksumshA512 = checksumshA512;
            return this;
        }

        /**
         * This header can be used as a data integrity check to verify that the data received is the same data that was
         * originally sent. This header specifies the Base64 encoded, 128-bit <code>MD5</code> digest of the part. For more
         * information, see <a href="https://docs.aws.amazon.com/AmazonS3/latest/userguide/checking-object-integrity.html">Checking object integrity</a> in the <i>Amazon S3 User Guide</i>.
         *
         * @return this builder.
         */
        public Builder checksummD5(String checksummD5) {
            this.checksummD5 = checksummD5;
            return this;
        }

        /**
         * This header can be used as a data integrity check to verify that the data received is the same data that was
         * originally sent. This header specifies the Base64 encoded, 64-bit <code>XXHASH64</code> checksum of the part. For
         * more information, see <a href="https://docs.aws.amazon.com/AmazonS3/latest/userguide/checking-object-integrity.html">Checking object integrity</a> in the <i>Amazon S3 User Guide</i>.
         *
         * @return this builder.
         */
        public Builder checksumxxhasH64(String checksumxxhasH64) {
            this.checksumxxhasH64 = checksumxxhasH64;
            return this;
        }

        /**
         * This header can be used as a data integrity check to verify that the data received is the same data that was
         * originally sent. This header specifies the Base64 encoded, 64-bit <code>XXHASH3</code> checksum of the part. For
         * more information, see <a href="https://docs.aws.amazon.com/AmazonS3/latest/userguide/checking-object-integrity.html">Checking object integrity</a> in the <i>Amazon S3 User Guide</i>.
         *
         * @return this builder.
         */
        public Builder checksumxxhasH3(String checksumxxhasH3) {
            this.checksumxxhasH3 = checksumxxhasH3;
            return this;
        }

        /**
         * This header can be used as a data integrity check to verify that the data received is the same data that was
         * originally sent. This header specifies the Base64 encoded, 128-bit <code>XXHASH128</code> checksum of the part.
         * For more information, see <a href="https://docs.aws.amazon.com/AmazonS3/latest/userguide/checking-object-integrity.html">Checking object integrity</a> in the <i>Amazon S3 User Guide</i>.
         *
         * @return this builder.
         */
        public Builder checksumxxhasH128(String checksumxxhasH128) {
            this.checksumxxhasH128 = checksumxxhasH128;
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
         * Part number of part being uploaded. This is a positive integer between 1 and 10,000.
         *
         * <p><strong>Required</strong>
         * @return this builder.
         */
        public Builder partNumber(int partNumber) {
            this.partNumber = partNumber;
            tracker.setMember($SCHEMA_PART_NUMBER);
            return this;
        }

        /**
         * Upload ID identifying the multipart upload whose part is being uploaded.
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
         * Specifies the algorithm to use when encrypting the object (for example, AES256).
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
         * Specifies the customer-provided encryption key for Amazon S3 to use in encrypting data. This value is used to
         * store the object and then it is discarded; Amazon S3 does not store the encryption key. The key must be
         * appropriate for use with the algorithm specified in the <code>x-amz-server-side-encryption-customer-algorithm
         * header</code>. This must be the same encryption key specified in the initiate multipart upload request.
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
         * Specifies the 128-bit MD5 digest of the encryption key according to RFC 1321. Amazon S3 uses this header for a
         * message integrity check to ensure that the encryption key was transmitted without error.
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

        @Override
        public UploadPartInput build() {
            tracker.validate();
            return new UploadPartInput(this);
        }

        @Override
        @SuppressWarnings("unchecked")
        public void setMemberValue(Schema member, Object value) {
            switch (member.memberIndex()) {
                case 0 -> bucket((String) SchemaUtils.validateSameMember($SCHEMA_BUCKET, member, value));
                case 1 -> key((String) SchemaUtils.validateSameMember($SCHEMA_KEY, member, value));
                case 2 -> partNumber((int) SchemaUtils.validateSameMember($SCHEMA_PART_NUMBER, member, value));
                case 3 -> uploadId((String) SchemaUtils.validateSameMember($SCHEMA_UPLOAD_ID, member, value));
                case 4 -> body((DataStream) SchemaUtils.validateSameMember($SCHEMA_BODY, member, value));
                case 5 -> contentLength((Long) SchemaUtils.validateSameMember($SCHEMA_CONTENT_LENGTH, member, value));
                case 6 -> contentmD5((String) SchemaUtils.validateSameMember($SCHEMA_CONTENTM_D5, member, value));
                case 7 -> checksumAlgorithm((ChecksumAlgorithm) SchemaUtils.validateSameMember($SCHEMA_CHECKSUM_ALGORITHM, member, value));
                case 8 -> checksumcrC32((String) SchemaUtils.validateSameMember($SCHEMA_CHECKSUMCR_C32, member, value));
                case 9 -> checksumcrC32C((String) SchemaUtils.validateSameMember($SCHEMA_CHECKSUMCR_C32_C, member, value));
                case 10 -> checksumcrC64nvmE((String) SchemaUtils.validateSameMember($SCHEMA_CHECKSUMCR_C64NVM_E, member, value));
                case 11 -> checksumshA1((String) SchemaUtils.validateSameMember($SCHEMA_CHECKSUMSH_A1, member, value));
                case 12 -> checksumshA256((String) SchemaUtils.validateSameMember($SCHEMA_CHECKSUMSH_A256, member, value));
                case 13 -> checksumshA512((String) SchemaUtils.validateSameMember($SCHEMA_CHECKSUMSH_A512, member, value));
                case 14 -> checksummD5((String) SchemaUtils.validateSameMember($SCHEMA_CHECKSUMM_D5, member, value));
                case 15 -> checksumxxhasH64((String) SchemaUtils.validateSameMember($SCHEMA_CHECKSUMXXHAS_H64, member, value));
                case 16 -> checksumxxhasH3((String) SchemaUtils.validateSameMember($SCHEMA_CHECKSUMXXHAS_H3, member, value));
                case 17 -> checksumxxhasH128((String) SchemaUtils.validateSameMember($SCHEMA_CHECKSUMXXHAS_H128, member, value));
                case 18 -> sseCustomerAlgorithm((String) SchemaUtils.validateSameMember($SCHEMA_SSE_CUSTOMER_ALGORITHM, member, value));
                case 19 -> sseCustomerKey((String) SchemaUtils.validateSameMember($SCHEMA_SSE_CUSTOMER_KEY, member, value));
                case 20 -> sseCustomerKeymD5((String) SchemaUtils.validateSameMember($SCHEMA_SSE_CUSTOMER_KEYM_D5, member, value));
                case 21 -> requestPayer((RequestPayer) SchemaUtils.validateSameMember($SCHEMA_REQUEST_PAYER, member, value));
                case 22 -> expectedBucketOwner((String) SchemaUtils.validateSameMember($SCHEMA_EXPECTED_BUCKET_OWNER, member, value));
                default -> ShapeBuilder.super.setMemberValue(member, value);
            }
        }

        @Override
        public ShapeBuilder<UploadPartInput> errorCorrection() {
            if (tracker.allSet()) {
                return this;
            }
            if (!tracker.checkMember($SCHEMA_BUCKET)) {
                bucket("");
            }
            if (!tracker.checkMember($SCHEMA_KEY)) {
                key("");
            }
            if (!tracker.checkMember($SCHEMA_PART_NUMBER)) {
                tracker.setMember($SCHEMA_PART_NUMBER);
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
                    case 2 -> builder.partNumber(de.readInteger(member));
                    case 3 -> builder.uploadId(de.readString(member));
                    case 4 -> builder.body(de.readDataStream(member));
                    case 5 -> builder.contentLength(de.readLong(member));
                    case 6 -> builder.contentmD5(de.readString(member));
                    case 7 -> builder.checksumAlgorithm(ChecksumAlgorithm.builder().deserializeMember(de, member).build());
                    case 8 -> builder.checksumcrC32(de.readString(member));
                    case 9 -> builder.checksumcrC32C(de.readString(member));
                    case 10 -> builder.checksumcrC64nvmE(de.readString(member));
                    case 11 -> builder.checksumshA1(de.readString(member));
                    case 12 -> builder.checksumshA256(de.readString(member));
                    case 13 -> builder.checksumshA512(de.readString(member));
                    case 14 -> builder.checksummD5(de.readString(member));
                    case 15 -> builder.checksumxxhasH64(de.readString(member));
                    case 16 -> builder.checksumxxhasH3(de.readString(member));
                    case 17 -> builder.checksumxxhasH128(de.readString(member));
                    case 18 -> builder.sseCustomerAlgorithm(de.readString(member));
                    case 19 -> builder.sseCustomerKey(de.readString(member));
                    case 20 -> builder.sseCustomerKeymD5(de.readString(member));
                    case 21 -> builder.requestPayer(RequestPayer.builder().deserializeMember(de, member).build());
                    case 22 -> builder.expectedBucketOwner(de.readString(member));
                    default -> throw new IllegalArgumentException("Unexpected member: " + member.memberName());
                }
            }
        }
    }
}
