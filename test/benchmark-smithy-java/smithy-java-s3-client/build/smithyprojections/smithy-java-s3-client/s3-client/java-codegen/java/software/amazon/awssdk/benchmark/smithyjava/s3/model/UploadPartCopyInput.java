package software.amazon.awssdk.benchmark.smithyjava.s3.model;

import java.time.Instant;
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
public final class UploadPartCopyInput implements SerializableStruct {

    public static final Schema $SCHEMA = Schemas7.UPLOAD_PART_COPY_INPUT;
    private static final Schema $SCHEMA_BUCKET = $SCHEMA.member("Bucket");
    private static final Schema $SCHEMA_COPY_SOURCE = $SCHEMA.member("CopySource");
    private static final Schema $SCHEMA_COPY_SOURCE_IF_MATCH = $SCHEMA.member("CopySourceIfMatch");
    private static final Schema $SCHEMA_COPY_SOURCE_IF_MODIFIED_SINCE = $SCHEMA.member("CopySourceIfModifiedSince");
    private static final Schema $SCHEMA_COPY_SOURCE_IF_NONE_MATCH = $SCHEMA.member("CopySourceIfNoneMatch");
    private static final Schema $SCHEMA_COPY_SOURCE_IF_UNMODIFIED_SINCE = $SCHEMA.member("CopySourceIfUnmodifiedSince");
    private static final Schema $SCHEMA_COPY_SOURCE_RANGE = $SCHEMA.member("CopySourceRange");
    private static final Schema $SCHEMA_KEY = $SCHEMA.member("Key");
    private static final Schema $SCHEMA_PART_NUMBER = $SCHEMA.member("PartNumber");
    private static final Schema $SCHEMA_UPLOAD_ID = $SCHEMA.member("UploadId");
    private static final Schema $SCHEMA_SSE_CUSTOMER_ALGORITHM = $SCHEMA.member("SSECustomerAlgorithm");
    private static final Schema $SCHEMA_SSE_CUSTOMER_KEY = $SCHEMA.member("SSECustomerKey");
    private static final Schema $SCHEMA_SSE_CUSTOMER_KEYM_D5 = $SCHEMA.member("SSECustomerKeyMD5");
    private static final Schema $SCHEMA_COPY_SOURCESSE_CUSTOMER_ALGORITHM = $SCHEMA.member("CopySourceSSECustomerAlgorithm");
    private static final Schema $SCHEMA_COPY_SOURCESSE_CUSTOMER_KEY = $SCHEMA.member("CopySourceSSECustomerKey");
    private static final Schema $SCHEMA_COPY_SOURCESSE_CUSTOMER_KEYM_D5 = $SCHEMA.member("CopySourceSSECustomerKeyMD5");
    private static final Schema $SCHEMA_REQUEST_PAYER = $SCHEMA.member("RequestPayer");
    private static final Schema $SCHEMA_EXPECTED_BUCKET_OWNER = $SCHEMA.member("ExpectedBucketOwner");
    private static final Schema $SCHEMA_EXPECTED_SOURCE_BUCKET_OWNER = $SCHEMA.member("ExpectedSourceBucketOwner");

    public static final ShapeId $ID = $SCHEMA.id();

    private final transient String bucket;
    private final transient String copySource;
    private final transient String copySourceIfMatch;
    private final transient Instant copySourceIfModifiedSince;
    private final transient String copySourceIfNoneMatch;
    private final transient Instant copySourceIfUnmodifiedSince;
    private final transient String copySourceRange;
    private final transient String key;
    private final transient int partNumber;
    private final transient String uploadId;
    private final transient String sseCustomerAlgorithm;
    private final transient String sseCustomerKey;
    private final transient String sseCustomerKeymD5;
    private final transient String copySourcesseCustomerAlgorithm;
    private final transient String copySourcesseCustomerKey;
    private final transient String copySourcesseCustomerKeymD5;
    private final transient RequestPayer requestPayer;
    private final transient String expectedBucketOwner;
    private final transient String expectedSourceBucketOwner;

    private UploadPartCopyInput(Builder builder) {
        this.bucket = builder.bucket;
        this.copySource = builder.copySource;
        this.copySourceIfMatch = builder.copySourceIfMatch;
        this.copySourceIfModifiedSince = builder.copySourceIfModifiedSince;
        this.copySourceIfNoneMatch = builder.copySourceIfNoneMatch;
        this.copySourceIfUnmodifiedSince = builder.copySourceIfUnmodifiedSince;
        this.copySourceRange = builder.copySourceRange;
        this.key = builder.key;
        this.partNumber = builder.partNumber;
        this.uploadId = builder.uploadId;
        this.sseCustomerAlgorithm = builder.sseCustomerAlgorithm;
        this.sseCustomerKey = builder.sseCustomerKey;
        this.sseCustomerKeymD5 = builder.sseCustomerKeymD5;
        this.copySourcesseCustomerAlgorithm = builder.copySourcesseCustomerAlgorithm;
        this.copySourcesseCustomerKey = builder.copySourcesseCustomerKey;
        this.copySourcesseCustomerKeymD5 = builder.copySourcesseCustomerKeymD5;
        this.requestPayer = builder.requestPayer;
        this.expectedBucketOwner = builder.expectedBucketOwner;
        this.expectedSourceBucketOwner = builder.expectedSourceBucketOwner;
    }

    /**
     * The bucket name.
     *
     * <p><b>Directory buckets</b> - When you use this operation with a directory bucket, you must use
     * virtual-hosted-style requests in the format <code><i>Bucket-name</i>.s3express-<i>zone-id</i>.<i>region-code</i>
     * .amazonaws.com</code>. Path-style requests are not supported. Directory bucket names must be unique in the chosen
     * Zone (Availability Zone or Local Zone). Bucket names must follow the format <code><i>bucket-base-name</i>--<i>
     * zone-id</i>--x-s3</code> (for example, <code><i>amzn-s3-demo-bucket</i>--<i>usw2-az1</i>--x-s3</code>). For
     * information about bucket naming restrictions, see <a href="https://docs.aws.amazon.com/AmazonS3/latest/userguide/directory-bucket-naming-rules.html">Directory bucket naming rules</a> in the <i>Amazon S3 User
     * Guide</i>.
     *
     * <p>Copying objects across different Amazon Web Services Regions isn't supported when the source or destination
     * bucket is in Amazon Web Services Local Zones. The source and destination buckets must have the same parent Amazon
     * Web Services Region. Otherwise, you get an HTTP <code>400 Bad Request</code> error with the error code <code>
     * InvalidRequest</code>.
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
     * Specifies the source object for the copy operation. You specify the value in one of two formats, depending on
     * whether you want to access the source object through an <a href="https://docs.aws.amazon.com/AmazonS3/latest/userguide/access-points.html">access point</a>:
     *
     * <ul>
     *   <li>
     *     For objects not accessed through an access point, specify the name of the source bucket and key of the
     *     source object, separated by a slash (/). For example, to copy the object <code>reports/january.pdf</code>
     *     from the bucket <code>awsexamplebucket</code>, use <code>awsexamplebucket/reports/january.pdf</code>. The
     *     value must be URL-encoded.
     *   </li>
     *   <li>
     *     For objects accessed through access points, specify the Amazon Resource Name (ARN) of the object as
     *     accessed through the access point, in the format <code>arn:aws:s3:&lt;Region&gt;::accesspoint//object/</code>
     *     . For example, to copy the object <code>reports/january.pdf</code> through access point <code>
     *     my-access-point</code> owned by account <code>123456789012</code> in Region <code>us-west-2</code>, use
     *     the URL encoding of <code>
     *     arn:aws:s3:us-west-2:123456789012:accesspoint/my-access-point/object/reports/january.pdf</code>. The
     *     value must be URL encoded.
     *
     *     <ul>
     *       <li>
     *         Amazon S3 supports copy operations using Access points only when the source and destination
     *         buckets are in the same Amazon Web Services Region.
     *       </li>
     *       <li>
     *         Access points are not supported by directory buckets.
     *       </li>
     *     </ul>Alternatively, for objects accessed through Amazon S3 on Outposts, specify the ARN of the object as
     *     accessed in the format <code>arn:aws:s3-outposts:&lt;Region&gt;::outpost//object/</code>. For example, to
     *     copy the object <code>reports/january.pdf</code> through outpost <code>my-outpost</code> owned by account
     *     <code>123456789012</code> in Region <code>us-west-2</code>, use the URL encoding of <code>
     *     arn:aws:s3-outposts:us-west-2:123456789012:outpost/my-outpost/object/reports/january.pdf</code>. The
     *     value must be URL-encoded.
     *   </li>
     * </ul>
     *
     * <p>If your bucket has versioning enabled, you could have multiple versions of the same object. By default, <code>
     * x-amz-copy-source</code> identifies the current version of the source object to copy. To copy a specific version
     * of the source object to copy, append <code>?versionId=</code> to the <code>x-amz-copy-source</code> request
     * header (for example, <code>x-amz-copy-source:
     * /awsexamplebucket/reports/january.pdf?versionId=QUpfdndhfd8438MNFDN93jdnJFkdmqnh893</code>).
     *
     * <p>If the current version is a delete marker and you don't specify a versionId in the <code>x-amz-copy-source</code>
     * request header, Amazon S3 returns a <code>404 Not Found</code> error, because the object does not exist. If you
     * specify versionId in the <code>x-amz-copy-source</code> and the versionId is a delete marker, Amazon S3 returns
     * an HTTP <code>400 Bad Request</code> error, because you are not allowed to specify a delete marker as a version
     * for the <code>x-amz-copy-source</code>.
     *
     * <p><b>Directory buckets</b> - S3 Versioning isn't enabled and supported for directory buckets.
     */
    public String getCopySource() {
        return copySource;
    }

    /**
     * Copies the object if its entity tag (ETag) matches the specified tag.
     *
     * <p>If both of the <code>x-amz-copy-source-if-match</code> and <code>x-amz-copy-source-if-unmodified-since</code>
     * headers are present in the request as follows:
     *
     * <p><code>x-amz-copy-source-if-match</code> condition evaluates to <code>true</code>, and;
     *
     * <p><code>x-amz-copy-source-if-unmodified-since</code> condition evaluates to <code>false</code>;
     *
     * <p>Amazon S3 returns <code>200 OK</code> and copies the data.
     */
    public String getCopySourceIfMatch() {
        return copySourceIfMatch;
    }

    /**
     * Copies the object if it has been modified since the specified time.
     *
     * <p>If both of the <code>x-amz-copy-source-if-none-match</code> and <code>x-amz-copy-source-if-modified-since</code>
     * headers are present in the request as follows:
     *
     * <p><code>x-amz-copy-source-if-none-match</code> condition evaluates to <code>false</code>, and;
     *
     * <p><code>x-amz-copy-source-if-modified-since</code> condition evaluates to <code>true</code>;
     *
     * <p>Amazon S3 returns <code>412 Precondition Failed</code> response code.
     */
    public Instant getCopySourceIfModifiedSince() {
        return copySourceIfModifiedSince;
    }

    /**
     * Copies the object if its entity tag (ETag) is different than the specified ETag.
     *
     * <p>If both of the <code>x-amz-copy-source-if-none-match</code> and <code>x-amz-copy-source-if-modified-since</code>
     * headers are present in the request as follows:
     *
     * <p><code>x-amz-copy-source-if-none-match</code> condition evaluates to <code>false</code>, and;
     *
     * <p><code>x-amz-copy-source-if-modified-since</code> condition evaluates to <code>true</code>;
     *
     * <p>Amazon S3 returns <code>412 Precondition Failed</code> response code.
     */
    public String getCopySourceIfNoneMatch() {
        return copySourceIfNoneMatch;
    }

    /**
     * Copies the object if it hasn't been modified since the specified time.
     *
     * <p>If both of the <code>x-amz-copy-source-if-match</code> and <code>x-amz-copy-source-if-unmodified-since</code>
     * headers are present in the request as follows:
     *
     * <p><code>x-amz-copy-source-if-match</code> condition evaluates to <code>true</code>, and;
     *
     * <p><code>x-amz-copy-source-if-unmodified-since</code> condition evaluates to <code>false</code>;
     *
     * <p>Amazon S3 returns <code>200 OK</code> and copies the data.
     */
    public Instant getCopySourceIfUnmodifiedSince() {
        return copySourceIfUnmodifiedSince;
    }

    /**
     * The range of bytes to copy from the source object. The range value must use the form bytes=first-last, where the
     * first and last are the zero-based byte offsets to copy. For example, bytes=0-9 indicates that you want to copy
     * the first 10 bytes of the source. You can copy a range only if the source object is greater than 5 MB.
     */
    public String getCopySourceRange() {
        return copySourceRange;
    }

    /**
     * Object key for which the multipart upload was initiated.
     */
    public String getKey() {
        return key;
    }

    /**
     * Part number of part being copied. This is a positive integer between 1 and 10,000.
     */
    public int getPartNumber() {
        return partNumber;
    }

    /**
     * Upload ID identifying the multipart upload whose part is being copied.
     */
    public String getUploadId() {
        return uploadId;
    }

    /**
     * Specifies the algorithm to use when encrypting the object (for example, AES256).
     *
     * <p>This functionality is not supported when the destination bucket is a directory bucket.
     */
    public String getSseCustomerAlgorithm() {
        return sseCustomerAlgorithm;
    }

    /**
     * Specifies the customer-provided encryption key for Amazon S3 to use in encrypting data. This value is used to
     * store the object and then it is discarded; Amazon S3 does not store the encryption key. The key must be
     * appropriate for use with the algorithm specified in the <code>x-amz-server-side-encryption-customer-algorithm</code>
     * header. This must be the same encryption key specified in the initiate multipart upload request.
     *
     * <p>This functionality is not supported when the destination bucket is a directory bucket.
     */
    public String getSseCustomerKey() {
        return sseCustomerKey;
    }

    /**
     * Specifies the 128-bit MD5 digest of the encryption key according to RFC 1321. Amazon S3 uses this header for a
     * message integrity check to ensure that the encryption key was transmitted without error.
     *
     * <p>This functionality is not supported when the destination bucket is a directory bucket.
     */
    public String getSseCustomerKeymD5() {
        return sseCustomerKeymD5;
    }

    /**
     * Specifies the algorithm to use when decrypting the source object (for example, <code>AES256</code>).
     *
     * <p>This functionality is not supported when the source object is in a directory bucket.
     */
    public String getCopySourcesseCustomerAlgorithm() {
        return copySourcesseCustomerAlgorithm;
    }

    /**
     * Specifies the customer-provided encryption key for Amazon S3 to use to decrypt the source object. The encryption
     * key provided in this header must be one that was used when the source object was created.
     *
     * <p>This functionality is not supported when the source object is in a directory bucket.
     */
    public String getCopySourcesseCustomerKey() {
        return copySourcesseCustomerKey;
    }

    /**
     * Specifies the 128-bit MD5 digest of the encryption key according to RFC 1321. Amazon S3 uses this header for a
     * message integrity check to ensure that the encryption key was transmitted without error.
     *
     * <p>This functionality is not supported when the source object is in a directory bucket.
     */
    public String getCopySourcesseCustomerKeymD5() {
        return copySourcesseCustomerKeymD5;
    }

    public RequestPayer getRequestPayer() {
        return requestPayer;
    }

    /**
     * The account ID of the expected destination bucket owner. If the account ID that you provide does not match the
     * actual owner of the destination bucket, the request fails with the HTTP status code <code>403 Forbidden</code>
     * (access denied).
     */
    public String getExpectedBucketOwner() {
        return expectedBucketOwner;
    }

    /**
     * The account ID of the expected source bucket owner. If the account ID that you provide does not match the actual
     * owner of the source bucket, the request fails with the HTTP status code <code>403 Forbidden</code> (access
     * denied).
     */
    public String getExpectedSourceBucketOwner() {
        return expectedSourceBucketOwner;
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
        UploadPartCopyInput that = (UploadPartCopyInput) other;
        return this.partNumber == that.partNumber
               && Objects.equals(this.bucket, that.bucket)
               && Objects.equals(this.copySource, that.copySource)
               && Objects.equals(this.copySourceIfMatch, that.copySourceIfMatch)
               && Objects.equals(this.copySourceIfNoneMatch, that.copySourceIfNoneMatch)
               && Objects.equals(this.copySourceRange, that.copySourceRange)
               && Objects.equals(this.key, that.key)
               && Objects.equals(this.uploadId, that.uploadId)
               && Objects.equals(this.sseCustomerAlgorithm, that.sseCustomerAlgorithm)
               && Objects.equals(this.sseCustomerKey, that.sseCustomerKey)
               && Objects.equals(this.sseCustomerKeymD5, that.sseCustomerKeymD5)
               && Objects.equals(this.copySourcesseCustomerAlgorithm, that.copySourcesseCustomerAlgorithm)
               && Objects.equals(this.copySourcesseCustomerKey, that.copySourcesseCustomerKey)
               && Objects.equals(this.copySourcesseCustomerKeymD5, that.copySourcesseCustomerKeymD5)
               && Objects.equals(this.expectedBucketOwner, that.expectedBucketOwner)
               && Objects.equals(this.expectedSourceBucketOwner, that.expectedSourceBucketOwner)
               && Objects.equals(this.requestPayer, that.requestPayer)
               && Objects.equals(this.copySourceIfModifiedSince, that.copySourceIfModifiedSince)
               && Objects.equals(this.copySourceIfUnmodifiedSince, that.copySourceIfUnmodifiedSince);
    }

    @Override
    public int hashCode() {
        int $hc = Objects.hashCode(bucket);
        $hc = 31 * $hc + Objects.hashCode(copySource);
        $hc = 31 * $hc + Objects.hashCode(copySourceIfMatch);
        $hc = 31 * $hc + Objects.hashCode(copySourceIfModifiedSince);
        $hc = 31 * $hc + Objects.hashCode(copySourceIfNoneMatch);
        $hc = 31 * $hc + Objects.hashCode(copySourceIfUnmodifiedSince);
        $hc = 31 * $hc + Objects.hashCode(copySourceRange);
        $hc = 31 * $hc + Objects.hashCode(key);
        $hc = 31 * $hc + Integer.hashCode(partNumber);
        $hc = 31 * $hc + Objects.hashCode(uploadId);
        $hc = 31 * $hc + Objects.hashCode(sseCustomerAlgorithm);
        $hc = 31 * $hc + Objects.hashCode(sseCustomerKey);
        $hc = 31 * $hc + Objects.hashCode(sseCustomerKeymD5);
        $hc = 31 * $hc + Objects.hashCode(copySourcesseCustomerAlgorithm);
        $hc = 31 * $hc + Objects.hashCode(copySourcesseCustomerKey);
        $hc = 31 * $hc + Objects.hashCode(copySourcesseCustomerKeymD5);
        $hc = 31 * $hc + Objects.hashCode(requestPayer);
        $hc = 31 * $hc + Objects.hashCode(expectedBucketOwner);
        $hc = 31 * $hc + Objects.hashCode(expectedSourceBucketOwner);
        return $hc;
    }

    @Override
    public Schema schema() {
        return $SCHEMA;
    }

    @Override
    public void serializeMembers(ShapeSerializer serializer) {
        serializer.writeString($SCHEMA_BUCKET, bucket);
        serializer.writeString($SCHEMA_COPY_SOURCE, copySource);
        if (copySourceIfMatch != null) {
            serializer.writeString($SCHEMA_COPY_SOURCE_IF_MATCH, copySourceIfMatch);
        }
        if (copySourceIfModifiedSince != null) {
            serializer.writeTimestamp($SCHEMA_COPY_SOURCE_IF_MODIFIED_SINCE, copySourceIfModifiedSince);
        }
        if (copySourceIfNoneMatch != null) {
            serializer.writeString($SCHEMA_COPY_SOURCE_IF_NONE_MATCH, copySourceIfNoneMatch);
        }
        if (copySourceIfUnmodifiedSince != null) {
            serializer.writeTimestamp($SCHEMA_COPY_SOURCE_IF_UNMODIFIED_SINCE, copySourceIfUnmodifiedSince);
        }
        if (copySourceRange != null) {
            serializer.writeString($SCHEMA_COPY_SOURCE_RANGE, copySourceRange);
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
        if (copySourcesseCustomerAlgorithm != null) {
            serializer.writeString($SCHEMA_COPY_SOURCESSE_CUSTOMER_ALGORITHM, copySourcesseCustomerAlgorithm);
        }
        if (copySourcesseCustomerKey != null) {
            serializer.writeString($SCHEMA_COPY_SOURCESSE_CUSTOMER_KEY, copySourcesseCustomerKey);
        }
        if (copySourcesseCustomerKeymD5 != null) {
            serializer.writeString($SCHEMA_COPY_SOURCESSE_CUSTOMER_KEYM_D5, copySourcesseCustomerKeymD5);
        }
        if (requestPayer != null) {
            serializer.writeString($SCHEMA_REQUEST_PAYER, requestPayer.getValue());
        }
        if (expectedBucketOwner != null) {
            serializer.writeString($SCHEMA_EXPECTED_BUCKET_OWNER, expectedBucketOwner);
        }
        if (expectedSourceBucketOwner != null) {
            serializer.writeString($SCHEMA_EXPECTED_SOURCE_BUCKET_OWNER, expectedSourceBucketOwner);
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T getMemberValue(Schema member) {
        return switch (member.memberIndex()) {
            case 0 -> (T) SchemaUtils.validateSameMember($SCHEMA_BUCKET, member, bucket);
            case 1 -> (T) SchemaUtils.validateSameMember($SCHEMA_COPY_SOURCE, member, copySource);
            case 2 -> (T) SchemaUtils.validateSameMember($SCHEMA_KEY, member, key);
            case 3 -> (T) SchemaUtils.validateSameMember($SCHEMA_PART_NUMBER, member, partNumber);
            case 4 -> (T) SchemaUtils.validateSameMember($SCHEMA_UPLOAD_ID, member, uploadId);
            case 5 -> (T) SchemaUtils.validateSameMember($SCHEMA_COPY_SOURCE_IF_MATCH, member, copySourceIfMatch);
            case 6 -> (T) SchemaUtils.validateSameMember($SCHEMA_COPY_SOURCE_IF_MODIFIED_SINCE, member, copySourceIfModifiedSince);
            case 7 -> (T) SchemaUtils.validateSameMember($SCHEMA_COPY_SOURCE_IF_NONE_MATCH, member, copySourceIfNoneMatch);
            case 8 -> (T) SchemaUtils.validateSameMember($SCHEMA_COPY_SOURCE_IF_UNMODIFIED_SINCE, member, copySourceIfUnmodifiedSince);
            case 9 -> (T) SchemaUtils.validateSameMember($SCHEMA_COPY_SOURCE_RANGE, member, copySourceRange);
            case 10 -> (T) SchemaUtils.validateSameMember($SCHEMA_SSE_CUSTOMER_ALGORITHM, member, sseCustomerAlgorithm);
            case 11 -> (T) SchemaUtils.validateSameMember($SCHEMA_SSE_CUSTOMER_KEY, member, sseCustomerKey);
            case 12 -> (T) SchemaUtils.validateSameMember($SCHEMA_SSE_CUSTOMER_KEYM_D5, member, sseCustomerKeymD5);
            case 13 -> (T) SchemaUtils.validateSameMember($SCHEMA_COPY_SOURCESSE_CUSTOMER_ALGORITHM, member, copySourcesseCustomerAlgorithm);
            case 14 -> (T) SchemaUtils.validateSameMember($SCHEMA_COPY_SOURCESSE_CUSTOMER_KEY, member, copySourcesseCustomerKey);
            case 15 -> (T) SchemaUtils.validateSameMember($SCHEMA_COPY_SOURCESSE_CUSTOMER_KEYM_D5, member, copySourcesseCustomerKeymD5);
            case 16 -> (T) SchemaUtils.validateSameMember($SCHEMA_REQUEST_PAYER, member, requestPayer);
            case 17 -> (T) SchemaUtils.validateSameMember($SCHEMA_EXPECTED_BUCKET_OWNER, member, expectedBucketOwner);
            case 18 -> (T) SchemaUtils.validateSameMember($SCHEMA_EXPECTED_SOURCE_BUCKET_OWNER, member, expectedSourceBucketOwner);
            default -> throw new IllegalArgumentException("Attempted to get non-existent member: " + member.id());
        };
    }

    /**
     * Create a new builder containing all the current property values of this object.
     *
     * <p><strong>Note:</strong> This method performs only a shallow copy of the original properties.
     *
     * @return a builder for {@link UploadPartCopyInput}.
     */
    public Builder toBuilder() {
        var builder = new Builder();
        builder.bucket(this.bucket);
        builder.copySource(this.copySource);
        builder.copySourceIfMatch(this.copySourceIfMatch);
        builder.copySourceIfModifiedSince(this.copySourceIfModifiedSince);
        builder.copySourceIfNoneMatch(this.copySourceIfNoneMatch);
        builder.copySourceIfUnmodifiedSince(this.copySourceIfUnmodifiedSince);
        builder.copySourceRange(this.copySourceRange);
        builder.key(this.key);
        builder.partNumber(this.partNumber);
        builder.uploadId(this.uploadId);
        builder.sseCustomerAlgorithm(this.sseCustomerAlgorithm);
        builder.sseCustomerKey(this.sseCustomerKey);
        builder.sseCustomerKeymD5(this.sseCustomerKeymD5);
        builder.copySourcesseCustomerAlgorithm(this.copySourcesseCustomerAlgorithm);
        builder.copySourcesseCustomerKey(this.copySourcesseCustomerKey);
        builder.copySourcesseCustomerKeymD5(this.copySourcesseCustomerKeymD5);
        builder.requestPayer(this.requestPayer);
        builder.expectedBucketOwner(this.expectedBucketOwner);
        builder.expectedSourceBucketOwner(this.expectedSourceBucketOwner);
        return builder;
    }

    /**
     * @return returns a new Builder.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for {@link UploadPartCopyInput}.
     */
    public static final class Builder implements ShapeBuilder<UploadPartCopyInput> {
        private final PresenceTracker tracker = PresenceTracker.of($SCHEMA);
        private String bucket;
        private String copySource;
        private String copySourceIfMatch;
        private Instant copySourceIfModifiedSince;
        private String copySourceIfNoneMatch;
        private Instant copySourceIfUnmodifiedSince;
        private String copySourceRange;
        private String key;
        private int partNumber;
        private String uploadId;
        private String sseCustomerAlgorithm;
        private String sseCustomerKey;
        private String sseCustomerKeymD5;
        private String copySourcesseCustomerAlgorithm;
        private String copySourcesseCustomerKey;
        private String copySourcesseCustomerKeymD5;
        private RequestPayer requestPayer;
        private String expectedBucketOwner;
        private String expectedSourceBucketOwner;

        private Builder() {}

        @Override
        public Schema schema() {
            return $SCHEMA;
        }

        /**
         * The bucket name.
         *
         * <p><b>Directory buckets</b> - When you use this operation with a directory bucket, you must use
         * virtual-hosted-style requests in the format <code><i>Bucket-name</i>.s3express-<i>zone-id</i>.<i>region-code</i>
         * .amazonaws.com</code>. Path-style requests are not supported. Directory bucket names must be unique in the chosen
         * Zone (Availability Zone or Local Zone). Bucket names must follow the format <code><i>bucket-base-name</i>--<i>
         * zone-id</i>--x-s3</code> (for example, <code><i>amzn-s3-demo-bucket</i>--<i>usw2-az1</i>--x-s3</code>). For
         * information about bucket naming restrictions, see <a href="https://docs.aws.amazon.com/AmazonS3/latest/userguide/directory-bucket-naming-rules.html">Directory bucket naming rules</a> in the <i>Amazon S3 User
         * Guide</i>.
         *
         * <p>Copying objects across different Amazon Web Services Regions isn't supported when the source or destination
         * bucket is in Amazon Web Services Local Zones. The source and destination buckets must have the same parent Amazon
         * Web Services Region. Otherwise, you get an HTTP <code>400 Bad Request</code> error with the error code <code>
         * InvalidRequest</code>.
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
         * Specifies the source object for the copy operation. You specify the value in one of two formats, depending on
         * whether you want to access the source object through an <a href="https://docs.aws.amazon.com/AmazonS3/latest/userguide/access-points.html">access point</a>:
         *
         * <ul>
         *   <li>
         *     For objects not accessed through an access point, specify the name of the source bucket and key of the
         *     source object, separated by a slash (/). For example, to copy the object <code>reports/january.pdf</code>
         *     from the bucket <code>awsexamplebucket</code>, use <code>awsexamplebucket/reports/january.pdf</code>. The
         *     value must be URL-encoded.
         *   </li>
         *   <li>
         *     For objects accessed through access points, specify the Amazon Resource Name (ARN) of the object as
         *     accessed through the access point, in the format <code>arn:aws:s3:&lt;Region&gt;::accesspoint//object/</code>
         *     . For example, to copy the object <code>reports/january.pdf</code> through access point <code>
         *     my-access-point</code> owned by account <code>123456789012</code> in Region <code>us-west-2</code>, use
         *     the URL encoding of <code>
         *     arn:aws:s3:us-west-2:123456789012:accesspoint/my-access-point/object/reports/january.pdf</code>. The
         *     value must be URL encoded.
         *
         *     <ul>
         *       <li>
         *         Amazon S3 supports copy operations using Access points only when the source and destination
         *         buckets are in the same Amazon Web Services Region.
         *       </li>
         *       <li>
         *         Access points are not supported by directory buckets.
         *       </li>
         *     </ul>Alternatively, for objects accessed through Amazon S3 on Outposts, specify the ARN of the object as
         *     accessed in the format <code>arn:aws:s3-outposts:&lt;Region&gt;::outpost//object/</code>. For example, to
         *     copy the object <code>reports/january.pdf</code> through outpost <code>my-outpost</code> owned by account
         *     <code>123456789012</code> in Region <code>us-west-2</code>, use the URL encoding of <code>
         *     arn:aws:s3-outposts:us-west-2:123456789012:outpost/my-outpost/object/reports/january.pdf</code>. The
         *     value must be URL-encoded.
         *   </li>
         * </ul>
         *
         * <p>If your bucket has versioning enabled, you could have multiple versions of the same object. By default, <code>
         * x-amz-copy-source</code> identifies the current version of the source object to copy. To copy a specific version
         * of the source object to copy, append <code>?versionId=</code> to the <code>x-amz-copy-source</code> request
         * header (for example, <code>x-amz-copy-source:
         * /awsexamplebucket/reports/january.pdf?versionId=QUpfdndhfd8438MNFDN93jdnJFkdmqnh893</code>).
         *
         * <p>If the current version is a delete marker and you don't specify a versionId in the <code>x-amz-copy-source</code>
         * request header, Amazon S3 returns a <code>404 Not Found</code> error, because the object does not exist. If you
         * specify versionId in the <code>x-amz-copy-source</code> and the versionId is a delete marker, Amazon S3 returns
         * an HTTP <code>400 Bad Request</code> error, because you are not allowed to specify a delete marker as a version
         * for the <code>x-amz-copy-source</code>.
         *
         * <p><b>Directory buckets</b> - S3 Versioning isn't enabled and supported for directory buckets.
         *
         * <p><strong>Required</strong>
         * @return this builder.
         */
        public Builder copySource(String copySource) {
            this.copySource = Objects.requireNonNull(copySource, "copySource cannot be null");
            tracker.setMember($SCHEMA_COPY_SOURCE);
            return this;
        }

        /**
         * Copies the object if its entity tag (ETag) matches the specified tag.
         *
         * <p>If both of the <code>x-amz-copy-source-if-match</code> and <code>x-amz-copy-source-if-unmodified-since</code>
         * headers are present in the request as follows:
         *
         * <p><code>x-amz-copy-source-if-match</code> condition evaluates to <code>true</code>, and;
         *
         * <p><code>x-amz-copy-source-if-unmodified-since</code> condition evaluates to <code>false</code>;
         *
         * <p>Amazon S3 returns <code>200 OK</code> and copies the data.
         *
         * @return this builder.
         */
        public Builder copySourceIfMatch(String copySourceIfMatch) {
            this.copySourceIfMatch = copySourceIfMatch;
            return this;
        }

        /**
         * Copies the object if it has been modified since the specified time.
         *
         * <p>If both of the <code>x-amz-copy-source-if-none-match</code> and <code>x-amz-copy-source-if-modified-since</code>
         * headers are present in the request as follows:
         *
         * <p><code>x-amz-copy-source-if-none-match</code> condition evaluates to <code>false</code>, and;
         *
         * <p><code>x-amz-copy-source-if-modified-since</code> condition evaluates to <code>true</code>;
         *
         * <p>Amazon S3 returns <code>412 Precondition Failed</code> response code.
         *
         * @return this builder.
         */
        public Builder copySourceIfModifiedSince(Instant copySourceIfModifiedSince) {
            this.copySourceIfModifiedSince = copySourceIfModifiedSince;
            return this;
        }

        /**
         * Copies the object if its entity tag (ETag) is different than the specified ETag.
         *
         * <p>If both of the <code>x-amz-copy-source-if-none-match</code> and <code>x-amz-copy-source-if-modified-since</code>
         * headers are present in the request as follows:
         *
         * <p><code>x-amz-copy-source-if-none-match</code> condition evaluates to <code>false</code>, and;
         *
         * <p><code>x-amz-copy-source-if-modified-since</code> condition evaluates to <code>true</code>;
         *
         * <p>Amazon S3 returns <code>412 Precondition Failed</code> response code.
         *
         * @return this builder.
         */
        public Builder copySourceIfNoneMatch(String copySourceIfNoneMatch) {
            this.copySourceIfNoneMatch = copySourceIfNoneMatch;
            return this;
        }

        /**
         * Copies the object if it hasn't been modified since the specified time.
         *
         * <p>If both of the <code>x-amz-copy-source-if-match</code> and <code>x-amz-copy-source-if-unmodified-since</code>
         * headers are present in the request as follows:
         *
         * <p><code>x-amz-copy-source-if-match</code> condition evaluates to <code>true</code>, and;
         *
         * <p><code>x-amz-copy-source-if-unmodified-since</code> condition evaluates to <code>false</code>;
         *
         * <p>Amazon S3 returns <code>200 OK</code> and copies the data.
         *
         * @return this builder.
         */
        public Builder copySourceIfUnmodifiedSince(Instant copySourceIfUnmodifiedSince) {
            this.copySourceIfUnmodifiedSince = copySourceIfUnmodifiedSince;
            return this;
        }

        /**
         * The range of bytes to copy from the source object. The range value must use the form bytes=first-last, where the
         * first and last are the zero-based byte offsets to copy. For example, bytes=0-9 indicates that you want to copy
         * the first 10 bytes of the source. You can copy a range only if the source object is greater than 5 MB.
         *
         * @return this builder.
         */
        public Builder copySourceRange(String copySourceRange) {
            this.copySourceRange = copySourceRange;
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
         * Part number of part being copied. This is a positive integer between 1 and 10,000.
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
         * Upload ID identifying the multipart upload whose part is being copied.
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
         * <p>This functionality is not supported when the destination bucket is a directory bucket.
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
         * appropriate for use with the algorithm specified in the <code>x-amz-server-side-encryption-customer-algorithm</code>
         * header. This must be the same encryption key specified in the initiate multipart upload request.
         *
         * <p>This functionality is not supported when the destination bucket is a directory bucket.
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
         * <p>This functionality is not supported when the destination bucket is a directory bucket.
         *
         * @return this builder.
         */
        public Builder sseCustomerKeymD5(String sseCustomerKeymD5) {
            this.sseCustomerKeymD5 = sseCustomerKeymD5;
            return this;
        }

        /**
         * Specifies the algorithm to use when decrypting the source object (for example, <code>AES256</code>).
         *
         * <p>This functionality is not supported when the source object is in a directory bucket.
         *
         * @return this builder.
         */
        public Builder copySourcesseCustomerAlgorithm(String copySourcesseCustomerAlgorithm) {
            this.copySourcesseCustomerAlgorithm = copySourcesseCustomerAlgorithm;
            return this;
        }

        /**
         * Specifies the customer-provided encryption key for Amazon S3 to use to decrypt the source object. The encryption
         * key provided in this header must be one that was used when the source object was created.
         *
         * <p>This functionality is not supported when the source object is in a directory bucket.
         *
         * @return this builder.
         */
        public Builder copySourcesseCustomerKey(String copySourcesseCustomerKey) {
            this.copySourcesseCustomerKey = copySourcesseCustomerKey;
            return this;
        }

        /**
         * Specifies the 128-bit MD5 digest of the encryption key according to RFC 1321. Amazon S3 uses this header for a
         * message integrity check to ensure that the encryption key was transmitted without error.
         *
         * <p>This functionality is not supported when the source object is in a directory bucket.
         *
         * @return this builder.
         */
        public Builder copySourcesseCustomerKeymD5(String copySourcesseCustomerKeymD5) {
            this.copySourcesseCustomerKeymD5 = copySourcesseCustomerKeymD5;
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
         * The account ID of the expected destination bucket owner. If the account ID that you provide does not match the
         * actual owner of the destination bucket, the request fails with the HTTP status code <code>403 Forbidden</code>
         * (access denied).
         *
         * @return this builder.
         */
        public Builder expectedBucketOwner(String expectedBucketOwner) {
            this.expectedBucketOwner = expectedBucketOwner;
            return this;
        }

        /**
         * The account ID of the expected source bucket owner. If the account ID that you provide does not match the actual
         * owner of the source bucket, the request fails with the HTTP status code <code>403 Forbidden</code> (access
         * denied).
         *
         * @return this builder.
         */
        public Builder expectedSourceBucketOwner(String expectedSourceBucketOwner) {
            this.expectedSourceBucketOwner = expectedSourceBucketOwner;
            return this;
        }

        @Override
        public UploadPartCopyInput build() {
            tracker.validate();
            return new UploadPartCopyInput(this);
        }

        @Override
        @SuppressWarnings("unchecked")
        public void setMemberValue(Schema member, Object value) {
            switch (member.memberIndex()) {
                case 0 -> bucket((String) SchemaUtils.validateSameMember($SCHEMA_BUCKET, member, value));
                case 1 -> copySource((String) SchemaUtils.validateSameMember($SCHEMA_COPY_SOURCE, member, value));
                case 2 -> key((String) SchemaUtils.validateSameMember($SCHEMA_KEY, member, value));
                case 3 -> partNumber((int) SchemaUtils.validateSameMember($SCHEMA_PART_NUMBER, member, value));
                case 4 -> uploadId((String) SchemaUtils.validateSameMember($SCHEMA_UPLOAD_ID, member, value));
                case 5 -> copySourceIfMatch((String) SchemaUtils.validateSameMember($SCHEMA_COPY_SOURCE_IF_MATCH, member, value));
                case 6 -> copySourceIfModifiedSince((Instant) SchemaUtils.validateSameMember($SCHEMA_COPY_SOURCE_IF_MODIFIED_SINCE, member, value));
                case 7 -> copySourceIfNoneMatch((String) SchemaUtils.validateSameMember($SCHEMA_COPY_SOURCE_IF_NONE_MATCH, member, value));
                case 8 -> copySourceIfUnmodifiedSince((Instant) SchemaUtils.validateSameMember($SCHEMA_COPY_SOURCE_IF_UNMODIFIED_SINCE, member, value));
                case 9 -> copySourceRange((String) SchemaUtils.validateSameMember($SCHEMA_COPY_SOURCE_RANGE, member, value));
                case 10 -> sseCustomerAlgorithm((String) SchemaUtils.validateSameMember($SCHEMA_SSE_CUSTOMER_ALGORITHM, member, value));
                case 11 -> sseCustomerKey((String) SchemaUtils.validateSameMember($SCHEMA_SSE_CUSTOMER_KEY, member, value));
                case 12 -> sseCustomerKeymD5((String) SchemaUtils.validateSameMember($SCHEMA_SSE_CUSTOMER_KEYM_D5, member, value));
                case 13 -> copySourcesseCustomerAlgorithm((String) SchemaUtils.validateSameMember($SCHEMA_COPY_SOURCESSE_CUSTOMER_ALGORITHM, member, value));
                case 14 -> copySourcesseCustomerKey((String) SchemaUtils.validateSameMember($SCHEMA_COPY_SOURCESSE_CUSTOMER_KEY, member, value));
                case 15 -> copySourcesseCustomerKeymD5((String) SchemaUtils.validateSameMember($SCHEMA_COPY_SOURCESSE_CUSTOMER_KEYM_D5, member, value));
                case 16 -> requestPayer((RequestPayer) SchemaUtils.validateSameMember($SCHEMA_REQUEST_PAYER, member, value));
                case 17 -> expectedBucketOwner((String) SchemaUtils.validateSameMember($SCHEMA_EXPECTED_BUCKET_OWNER, member, value));
                case 18 -> expectedSourceBucketOwner((String) SchemaUtils.validateSameMember($SCHEMA_EXPECTED_SOURCE_BUCKET_OWNER, member, value));
                default -> ShapeBuilder.super.setMemberValue(member, value);
            }
        }

        @Override
        public ShapeBuilder<UploadPartCopyInput> errorCorrection() {
            if (tracker.allSet()) {
                return this;
            }
            if (!tracker.checkMember($SCHEMA_BUCKET)) {
                bucket("");
            }
            if (!tracker.checkMember($SCHEMA_COPY_SOURCE)) {
                copySource("");
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
                    case 1 -> builder.copySource(de.readString(member));
                    case 2 -> builder.key(de.readString(member));
                    case 3 -> builder.partNumber(de.readInteger(member));
                    case 4 -> builder.uploadId(de.readString(member));
                    case 5 -> builder.copySourceIfMatch(de.readString(member));
                    case 6 -> builder.copySourceIfModifiedSince(de.readTimestamp(member));
                    case 7 -> builder.copySourceIfNoneMatch(de.readString(member));
                    case 8 -> builder.copySourceIfUnmodifiedSince(de.readTimestamp(member));
                    case 9 -> builder.copySourceRange(de.readString(member));
                    case 10 -> builder.sseCustomerAlgorithm(de.readString(member));
                    case 11 -> builder.sseCustomerKey(de.readString(member));
                    case 12 -> builder.sseCustomerKeymD5(de.readString(member));
                    case 13 -> builder.copySourcesseCustomerAlgorithm(de.readString(member));
                    case 14 -> builder.copySourcesseCustomerKey(de.readString(member));
                    case 15 -> builder.copySourcesseCustomerKeymD5(de.readString(member));
                    case 16 -> builder.requestPayer(RequestPayer.builder().deserializeMember(de, member).build());
                    case 17 -> builder.expectedBucketOwner(de.readString(member));
                    case 18 -> builder.expectedSourceBucketOwner(de.readString(member));
                    default -> throw new IllegalArgumentException("Unexpected member: " + member.memberName());
                }
            }
        }
    }
}
