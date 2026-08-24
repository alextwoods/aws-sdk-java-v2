package software.amazon.awssdk.benchmark.smithyjava.s3.model;

import java.io.Closeable;
import java.time.Instant;
import java.util.Collections;
import java.util.Map;
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
public final class PutObjectInput implements SerializableStruct, Closeable {

    public static final Schema $SCHEMA = Schemas5.PUT_OBJECT_INPUT;
    private static final Schema $SCHEMA_ACL = $SCHEMA.member("ACL");
    private static final Schema $SCHEMA_BODY = $SCHEMA.member("Body");
    private static final Schema $SCHEMA_BUCKET = $SCHEMA.member("Bucket");
    private static final Schema $SCHEMA_CACHE_CONTROL = $SCHEMA.member("CacheControl");
    private static final Schema $SCHEMA_CONTENT_DISPOSITION = $SCHEMA.member("ContentDisposition");
    private static final Schema $SCHEMA_CONTENT_ENCODING = $SCHEMA.member("ContentEncoding");
    private static final Schema $SCHEMA_CONTENT_LANGUAGE = $SCHEMA.member("ContentLanguage");
    private static final Schema $SCHEMA_CONTENT_LENGTH = $SCHEMA.member("ContentLength");
    private static final Schema $SCHEMA_CONTENTM_D5 = $SCHEMA.member("ContentMD5");
    private static final Schema $SCHEMA_CONTENT_TYPE = $SCHEMA.member("ContentType");
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
    private static final Schema $SCHEMA_EXPIRES = $SCHEMA.member("Expires");
    private static final Schema $SCHEMA_IF_MATCH = $SCHEMA.member("IfMatch");
    private static final Schema $SCHEMA_IF_NONE_MATCH = $SCHEMA.member("IfNoneMatch");
    private static final Schema $SCHEMA_GRANT_FULL_CONTROL = $SCHEMA.member("GrantFullControl");
    private static final Schema $SCHEMA_GRANT_READ = $SCHEMA.member("GrantRead");
    private static final Schema $SCHEMA_GRANT_READAC_P = $SCHEMA.member("GrantReadACP");
    private static final Schema $SCHEMA_GRANT_WRITEAC_P = $SCHEMA.member("GrantWriteACP");
    private static final Schema $SCHEMA_KEY = $SCHEMA.member("Key");
    private static final Schema $SCHEMA_WRITE_OFFSET_BYTES = $SCHEMA.member("WriteOffsetBytes");
    private static final Schema $SCHEMA_METADATA = $SCHEMA.member("Metadata");
    private static final Schema $SCHEMA_SERVER_SIDE_ENCRYPTION = $SCHEMA.member("ServerSideEncryption");
    private static final Schema $SCHEMA_STORAGE_CLASS = $SCHEMA.member("StorageClass");
    private static final Schema $SCHEMA_WEBSITE_REDIRECT_LOCATION = $SCHEMA.member("WebsiteRedirectLocation");
    private static final Schema $SCHEMA_SSE_CUSTOMER_ALGORITHM = $SCHEMA.member("SSECustomerAlgorithm");
    private static final Schema $SCHEMA_SSE_CUSTOMER_KEY = $SCHEMA.member("SSECustomerKey");
    private static final Schema $SCHEMA_SSE_CUSTOMER_KEYM_D5 = $SCHEMA.member("SSECustomerKeyMD5");
    private static final Schema $SCHEMA_SSEKMS_KEY_ID = $SCHEMA.member("SSEKMSKeyId");
    private static final Schema $SCHEMA_SSEKMS_ENCRYPTION_CONTEXT = $SCHEMA.member("SSEKMSEncryptionContext");
    private static final Schema $SCHEMA_BUCKET_KEY_ENABLED = $SCHEMA.member("BucketKeyEnabled");
    private static final Schema $SCHEMA_REQUEST_PAYER = $SCHEMA.member("RequestPayer");
    private static final Schema $SCHEMA_TAGGING = $SCHEMA.member("Tagging");
    private static final Schema $SCHEMA_OBJECT_LOCK_MODE = $SCHEMA.member("ObjectLockMode");
    private static final Schema $SCHEMA_OBJECT_LOCK_RETAIN_UNTIL_DATE = $SCHEMA.member("ObjectLockRetainUntilDate");
    private static final Schema $SCHEMA_OBJECT_LOCK_LEGAL_HOLD_STATUS = $SCHEMA.member("ObjectLockLegalHoldStatus");
    private static final Schema $SCHEMA_EXPECTED_BUCKET_OWNER = $SCHEMA.member("ExpectedBucketOwner");

    public static final ShapeId $ID = $SCHEMA.id();

    private final transient ObjectCannedACL acl;
    private final transient DataStream body;
    private final transient String bucket;
    private final transient String cacheControl;
    private final transient String contentDisposition;
    private final transient String contentEncoding;
    private final transient String contentLanguage;
    private final transient Long contentLength;
    private final transient String contentmD5;
    private final transient String contentType;
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
    private final transient String expires;
    private final transient String ifMatch;
    private final transient String ifNoneMatch;
    private final transient String grantFullControl;
    private final transient String grantRead;
    private final transient String grantReadacP;
    private final transient String grantWriteacP;
    private final transient String key;
    private final transient Long writeOffsetBytes;
    private final transient Map<String, String> metadata;
    private final transient ServerSideEncryption serverSideEncryption;
    private final transient StorageClass storageClass;
    private final transient String websiteRedirectLocation;
    private final transient String sseCustomerAlgorithm;
    private final transient String sseCustomerKey;
    private final transient String sseCustomerKeymD5;
    private final transient String ssekmsKeyId;
    private final transient String ssekmsEncryptionContext;
    private final transient Boolean bucketKeyEnabled;
    private final transient RequestPayer requestPayer;
    private final transient String tagging;
    private final transient ObjectLockMode objectLockMode;
    private final transient Instant objectLockRetainUntilDate;
    private final transient ObjectLockLegalHoldStatus objectLockLegalHoldStatus;
    private final transient String expectedBucketOwner;

    private PutObjectInput(Builder builder) {
        this.acl = builder.acl;
        this.body = builder.body;
        this.bucket = builder.bucket;
        this.cacheControl = builder.cacheControl;
        this.contentDisposition = builder.contentDisposition;
        this.contentEncoding = builder.contentEncoding;
        this.contentLanguage = builder.contentLanguage;
        this.contentLength = builder.contentLength;
        this.contentmD5 = builder.contentmD5;
        this.contentType = builder.contentType;
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
        this.expires = builder.expires;
        this.ifMatch = builder.ifMatch;
        this.ifNoneMatch = builder.ifNoneMatch;
        this.grantFullControl = builder.grantFullControl;
        this.grantRead = builder.grantRead;
        this.grantReadacP = builder.grantReadacP;
        this.grantWriteacP = builder.grantWriteacP;
        this.key = builder.key;
        this.writeOffsetBytes = builder.writeOffsetBytes;
        this.metadata = builder.metadata == null ? null : Collections.unmodifiableMap(builder.metadata);
        this.serverSideEncryption = builder.serverSideEncryption;
        this.storageClass = builder.storageClass;
        this.websiteRedirectLocation = builder.websiteRedirectLocation;
        this.sseCustomerAlgorithm = builder.sseCustomerAlgorithm;
        this.sseCustomerKey = builder.sseCustomerKey;
        this.sseCustomerKeymD5 = builder.sseCustomerKeymD5;
        this.ssekmsKeyId = builder.ssekmsKeyId;
        this.ssekmsEncryptionContext = builder.ssekmsEncryptionContext;
        this.bucketKeyEnabled = builder.bucketKeyEnabled;
        this.requestPayer = builder.requestPayer;
        this.tagging = builder.tagging;
        this.objectLockMode = builder.objectLockMode;
        this.objectLockRetainUntilDate = builder.objectLockRetainUntilDate;
        this.objectLockLegalHoldStatus = builder.objectLockLegalHoldStatus;
        this.expectedBucketOwner = builder.expectedBucketOwner;
    }

    /**
     * The canned ACL to apply to the object. For more information, see <a href="https://docs.aws.amazon.com/AmazonS3/latest/dev/acl-overview.html#CannedACL">Canned ACL</a> in the <i>Amazon S3 User Guide</i>
     * .
     *
     * <p>When adding a new object, you can use headers to grant ACL-based permissions to individual Amazon Web Services
     * accounts or to predefined groups defined by Amazon S3. These permissions are then added to the ACL on the object.
     * By default, all objects are private. Only the owner has full access control. For more information, see <a href="https://docs.aws.amazon.com/AmazonS3/latest/dev/acl-overview.html">Access
     * Control List (ACL) Overview</a> and <a href="https://docs.aws.amazon.com/AmazonS3/latest/dev/acl-using-rest-api.html">Managing ACLs Using the REST API</a> in the <i>Amazon S3 User Guide</i>.
     *
     * <p>If the bucket that you're uploading objects to uses the bucket owner enforced setting for S3 Object Ownership,
     * ACLs are disabled and no longer affect permissions. Buckets that use this setting only accept PUT requests that
     * don't specify an ACL or PUT requests that specify bucket owner full control ACLs, such as the <code>
     * bucket-owner-full-control</code> canned ACL or an equivalent form of this ACL expressed in the XML format. PUT
     * requests that contain other ACLs (for example, custom grants to certain Amazon Web Services accounts) fail and
     * return a <code>400</code> error with the error code <code>AccessControlListNotSupported</code>. For more
     * information, see <a href="https://docs.aws.amazon.com/AmazonS3/latest/userguide/about-object-ownership.html"> Controlling ownership of objects and disabling ACLs</a> in the <i>Amazon S3 User Guide</i>.
     *
     * <ul>
     *   <li>
     *     This functionality is not supported for directory buckets.
     *   </li>
     *   <li>
     *     This functionality is not supported for Amazon S3 on Outposts.
     *   </li>
     * </ul>
     */
    public ObjectCannedACL getAcl() {
        return acl;
    }

    /**
     * Object data.
     */
    public DataStream getBody() {
        return body;
    }

    /**
     * The bucket name to which the PUT action was initiated.
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
     * Can be used to specify caching behavior along the request/reply chain. For more information, see <a
     * href="http://www.w3.org/Protocols/rfc2616/rfc2616-sec14.html#sec14.9">http://www.w3.org/Protocols/rfc2616/rfc2616-sec14.html#sec14.9</a>.
     */
    public String getCacheControl() {
        return cacheControl;
    }

    /**
     * Specifies presentational information for the object. For more information, see <a
     * href="https://www.rfc-editor.org/rfc/rfc6266#section-4">https://www.rfc-editor.org/rfc/rfc6266#section-4</a>.
     */
    public String getContentDisposition() {
        return contentDisposition;
    }

    /**
     * Specifies what content encodings have been applied to the object and thus what decoding mechanisms must be
     * applied to obtain the media-type referenced by the Content-Type header field. For more information, see <a
     * href="https://www.rfc-editor.org/rfc/rfc9110.html#field.content-encoding">https://www.rfc-editor.org/rfc/rfc9110.html#field.content-encoding</a>.
     */
    public String getContentEncoding() {
        return contentEncoding;
    }

    /**
     * The language the content is in.
     */
    public String getContentLanguage() {
        return contentLanguage;
    }

    /**
     * Size of the body in bytes. This parameter is useful when the size of the body cannot be determined automatically.
     * For more information, see <a
     * href="https://www.rfc-editor.org/rfc/rfc9110.html#name-content-length">https://www.rfc-editor.org/rfc/rfc9110.html#name-content-length</a>.
     */
    public Long getContentLength() {
        return contentLength;
    }

    /**
     * The Base64 encoded 128-bit <code>MD5</code> digest of the message (without the headers) according to RFC 1864.
     * This header can be used as a message integrity check to verify that the data is the same data that was originally
     * sent. Although it is optional, we recommend using the Content-MD5 mechanism as an end-to-end integrity check. For
     * more information about REST request authentication, see <a href="https://docs.aws.amazon.com/AmazonS3/latest/dev/RESTAuthentication.html">REST Authentication</a>.
     *
     * <p>The <code>Content-MD5</code> or <code>x-amz-sdk-checksum-algorithm</code> header is required for any request
     * to upload an object with a retention period configured using Amazon S3 Object Lock. For more information, see <a href="https://docs.aws.amazon.com/AmazonS3/latest/userguide/object-lock-managing.html#object-lock-put-object">
     * Uploading objects to an Object Lock enabled bucket </a> in the <i>Amazon S3 User Guide</i>.
     *
     * <p>This functionality is not supported for directory buckets.
     */
    public String getContentmD5() {
        return contentmD5;
    }

    /**
     * A standard MIME type describing the format of the contents. For more information, see <a
     * href="https://www.rfc-editor.org/rfc/rfc9110.html#name-content-type">https://www.rfc-editor.org/rfc/rfc9110.html#name-content-type</a>.
     */
    public String getContentType() {
        return contentType;
    }

    /**
     * Indicates the algorithm used to create the checksum for the object when you use the SDK. This header will not
     * provide any additional functionality if you don't use the SDK. When you send this header, there must be a
     * corresponding <code>x-amz-checksum-<i>algorithm</i></code> or <code>x-amz-trailer</code> header sent. Otherwise,
     * Amazon S3 fails the request with the HTTP status code <code>400 Bad Request</code>.
     *
     * <p>For the <code>x-amz-checksum-<i>algorithm</i></code> header, replace <code><i>algorithm</i></code> with the
     * supported algorithm from the following list:
     *
     * <ul>
     *   <li>
     *     <code>CRC32</code>
     *   </li>
     *   <li>
     *     <code>CRC32C</code>
     *   </li>
     *   <li>
     *     <code>CRC64NVME</code>
     *   </li>
     *   <li>
     *     <code>MD5</code>
     *   </li>
     *   <li>
     *     <code>SHA1</code>
     *   </li>
     *   <li>
     *     <code>SHA256</code>
     *   </li>
     *   <li>
     *     <code>SHA512</code>
     *   </li>
     *   <li>
     *     <code>XXHASH3</code>
     *   </li>
     *   <li>
     *     <code>XXHASH64</code>
     *   </li>
     *   <li>
     *     <code>XXHASH128</code>
     *   </li>
     * </ul>
     *
     * <p>For more information, see <a href="https://docs.aws.amazon.com/AmazonS3/latest/userguide/checking-object-integrity.html">Checking object integrity</a> in the <i>Amazon S3 User Guide</i>.
     *
     * <p>If the individual checksum value you provide through <code>x-amz-checksum-<i>algorithm</i></code> doesn't
     * match the checksum algorithm you set through <code>x-amz-sdk-checksum-algorithm</code>, Amazon S3 fails the
     * request with a <code>BadDigest</code> error.
     *
     * <p>The <code>Content-MD5</code> or <code>x-amz-sdk-checksum-algorithm</code> header is required for any request
     * to upload an object with a retention period configured using Amazon S3 Object Lock. For more information, see <a href="https://docs.aws.amazon.com/AmazonS3/latest/userguide/object-lock-managing.html#object-lock-put-object">
     * Uploading objects to an Object Lock enabled bucket </a> in the <i>Amazon S3 User Guide</i>.
     *
     * <p>For directory buckets, when you use Amazon Web Services SDKs, <code>CRC32</code> is the default checksum
     * algorithm that's used for performance.
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
     * The date and time at which the object is no longer cacheable. For more information, see <a
     * href="https://www.rfc-editor.org/rfc/rfc7234#section-5.3">https://www.rfc-editor.org/rfc/rfc7234#section-5.3</a>.
     */
    public String getExpires() {
        return expires;
    }

    /**
     * Uploads the object only if the ETag (entity tag) value provided during the WRITE operation matches the ETag of
     * the object in S3. If the ETag values do not match, the operation returns a <code>412 Precondition Failed</code>
     * error.
     *
     * <p>If a conflicting operation occurs during the upload S3 returns a <code>409 ConditionalRequestConflict</code>
     * response. On a 409 failure you should fetch the object's ETag and retry the upload.
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
     * response. On a 409 failure you should retry the upload.
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
     * Gives the grantee READ, READ_ACP, and WRITE_ACP permissions on the object.
     *
     * <ul>
     *   <li>
     *     This functionality is not supported for directory buckets.
     *   </li>
     *   <li>
     *     This functionality is not supported for Amazon S3 on Outposts.
     *   </li>
     * </ul>
     */
    public String getGrantFullControl() {
        return grantFullControl;
    }

    /**
     * Allows grantee to read the object data and its metadata.
     *
     * <ul>
     *   <li>
     *     This functionality is not supported for directory buckets.
     *   </li>
     *   <li>
     *     This functionality is not supported for Amazon S3 on Outposts.
     *   </li>
     * </ul>
     */
    public String getGrantRead() {
        return grantRead;
    }

    /**
     * Allows grantee to read the object ACL.
     *
     * <ul>
     *   <li>
     *     This functionality is not supported for directory buckets.
     *   </li>
     *   <li>
     *     This functionality is not supported for Amazon S3 on Outposts.
     *   </li>
     * </ul>
     */
    public String getGrantReadacP() {
        return grantReadacP;
    }

    /**
     * Allows grantee to write the ACL for the applicable object.
     *
     * <ul>
     *   <li>
     *     This functionality is not supported for directory buckets.
     *   </li>
     *   <li>
     *     This functionality is not supported for Amazon S3 on Outposts.
     *   </li>
     * </ul>
     */
    public String getGrantWriteacP() {
        return grantWriteacP;
    }

    /**
     * Object key for which the PUT action was initiated.
     */
    public String getKey() {
        return key;
    }

    /**
     * Specifies the offset for appending data to existing objects in bytes. The offset must be equal to the size of
     * the existing object being appended to. If no object exists, setting this header to 0 will create a new object.
     *
     * <p>This functionality is only supported for objects in the Amazon S3 Express One Zone storage class in directory
     * buckets.
     */
    public Long getWriteOffsetBytes() {
        return writeOffsetBytes;
    }

    /**
     * A map of metadata to store with the object in S3.
     */
    public Map<String, String> getMetadata() {
        if (metadata == null) {
            return Collections.emptyMap();
        }
        return metadata;
    }

    public boolean hasMetadata() {
        return metadata != null;
    }

    /**
     * The server-side encryption algorithm that was used when you store this object in Amazon S3 or Amazon FSx.
     *
     * <ul>
     *   <li>
     *     <b>General purpose buckets </b> - You have four mutually exclusive options to protect data using
     *     server-side encryption in Amazon S3, depending on how you choose to manage the encryption keys.
     *     Specifically, the encryption key options are Amazon S3 managed keys (SSE-S3), Amazon Web Services KMS
     *     keys (SSE-KMS or DSSE-KMS), and customer-provided keys (SSE-C). Amazon S3 encrypts data with server-side
     *     encryption by using Amazon S3 managed keys (SSE-S3) by default. You can optionally tell Amazon S3 to
     *     encrypt data at rest by using server-side encryption with other key options. For more information, see <a href="https://docs.aws.amazon.com/AmazonS3/latest/dev/UsingServerSideEncryption.html">
     *     Using Server-Side Encryption</a> in the <i>Amazon S3 User Guide</i>.
     *   </li>
     *   <li>
     *     <b>Directory buckets </b> - For directory buckets, there are only two supported options for server-side
     *     encryption: server-side encryption with Amazon S3 managed keys (SSE-S3) (<code>AES256</code>) and
     *     server-side encryption with KMS keys (SSE-KMS) (<code>aws:kms</code>). We recommend that the bucket's
     *     default encryption uses the desired encryption configuration and you don't override the bucket default
     *     encryption in your <code>CreateSession</code> requests or <code>PUT</code> object requests. Then, new
     *     objects are automatically encrypted with the desired encryption settings. For more information, see <a href="https://docs.aws.amazon.com/AmazonS3/latest/userguide/s3-express-serv-side-encryption.html">
     *     Protecting data with server-side encryption</a> in the <i>Amazon S3 User Guide</i>. For more information
     *     about the encryption overriding behaviors in directory buckets, see <a href="https://docs.aws.amazon.com/AmazonS3/latest/userguide/s3-express-specifying-kms-encryption.html">Specifying server-side encryption
     *     with KMS for new object uploads</a>. In the Zonal endpoint API calls (except <a href="https://docs.aws.amazon.com/AmazonS3/latest/API/API_CopyObject.html">CopyObject</a> and <a href="https://docs.aws.amazon.com/AmazonS3/latest/API/API_UploadPartCopy.html">
     *     UploadPartCopy</a>) using the REST API, the encryption request headers must match the encryption settings
     *     that are specified in the <code>CreateSession</code> request. You can't override the values of the
     *     encryption settings (<code>x-amz-server-side-encryption</code>, <code>
     *     x-amz-server-side-encryption-aws-kms-key-id</code>, <code>x-amz-server-side-encryption-context</code>,
     *     and <code>x-amz-server-side-encryption-bucket-key-enabled</code>) that are specified in the <code>
     *     CreateSession</code> request. You don't need to explicitly specify these encryption settings values in
     *     Zonal endpoint API calls, and Amazon S3 will use the encryption settings values from the <code>
     *     CreateSession</code> request to protect new objects in the directory bucket. When you use the CLI or the
     *     Amazon Web Services SDKs, for <code>CreateSession</code>, the session token refreshes automatically to
     *     avoid service interruptions when a session expires. The CLI or the Amazon Web Services SDKs use the
     *     bucket's default encryption configuration for the <code>CreateSession</code> request. It's not supported
     *     to override the encryption settings values in the <code>CreateSession</code> request. So in the Zonal
     *     endpoint API calls (except <a href="https://docs.aws.amazon.com/AmazonS3/latest/API/API_CopyObject.html">CopyObject</a> and <a href="https://docs.aws.amazon.com/AmazonS3/latest/API/API_UploadPartCopy.html">UploadPartCopy</a>), the encryption request headers
     *     must match the default encryption configuration of the directory bucket.
     *   </li>
     *   <li>
     *     <b>S3 access points for Amazon FSx </b> - When accessing data stored in Amazon FSx file systems using S3
     *     access points, the only valid server side encryption option is <code>aws:fsx</code>. All Amazon FSx file
     *     systems have encryption configured by default and are encrypted at rest. Data is automatically encrypted
     *     before being written to the file system, and automatically decrypted as it is read. These processes are
     *     handled transparently by Amazon FSx.
     *   </li>
     * </ul>
     */
    public ServerSideEncryption getServerSideEncryption() {
        return serverSideEncryption;
    }

    /**
     * By default, Amazon S3 uses the STANDARD Storage Class to store newly created objects. The STANDARD storage class
     * provides high durability and high availability. Depending on performance needs, you can specify a different
     * Storage Class. For more information, see <a href="https://docs.aws.amazon.com/AmazonS3/latest/dev/storage-class-intro.html">Storage Classes</a> in the <i>Amazon S3 User Guide</i>.
     *
     * <ul>
     *   <li>
     *     Directory buckets only support <code>EXPRESS_ONEZONE</code> (the S3 Express One Zone storage class) in
     *     Availability Zones and <code>ONEZONE_IA</code> (the S3 One Zone-Infrequent Access storage class) in
     *     Dedicated Local Zones.
     *   </li>
     *   <li>
     *     Amazon S3 on Outposts only uses the OUTPOSTS Storage Class.
     *   </li>
     * </ul>
     */
    public StorageClass getStorageClass() {
        return storageClass;
    }

    /**
     * If the bucket is configured as a website, redirects requests for this object to another object in the same bucket
     * or to an external URL. Amazon S3 stores the value of this header in the object metadata. For information about
     * object metadata, see <a href="https://docs.aws.amazon.com/AmazonS3/latest/dev/UsingMetadata.html">Object Key and Metadata</a> in the <i>Amazon S3 User Guide</i>.
     *
     * <p>In the following example, the request header sets the redirect to an object (anotherPage.html) in the same
     * bucket:
     *
     * <p><code>x-amz-website-redirect-location: /anotherPage.html</code>
     *
     * <p>In the following example, the request header sets the object redirect to another website:
     *
     * <p><code>x-amz-website-redirect-location: http://www.example.com/</code>
     *
     * <p>For more information about website hosting in Amazon S3, see <a href="https://docs.aws.amazon.com/AmazonS3/latest/dev/WebsiteHosting.html">Hosting Websites on Amazon S3</a> and <a href="https://docs.aws.amazon.com/AmazonS3/latest/dev/how-to-page-redirect.html">How
     * to Configure Website Page Redirects</a> in the <i>Amazon S3 User Guide</i>.
     *
     * <p>This functionality is not supported for directory buckets.
     */
    public String getWebsiteRedirectLocation() {
        return websiteRedirectLocation;
    }

    /**
     * Specifies the algorithm to use when encrypting the object (for example, <code>AES256</code>).
     *
     * <p>This functionality is not supported for directory buckets.
     */
    public String getSseCustomerAlgorithm() {
        return sseCustomerAlgorithm;
    }

    /**
     * Specifies the customer-provided encryption key for Amazon S3 to use in encrypting data. This value is used to
     * store the object and then it is discarded; Amazon S3 does not store the encryption key. The key must be
     * appropriate for use with the algorithm specified in the <code>x-amz-server-side-encryption-customer-algorithm</code>
     * header.
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

    /**
     * Specifies the KMS key ID (Key ID, Key ARN, or Key Alias) to use for object encryption. If the KMS key doesn't
     * exist in the same account that's issuing the command, you must use the full Key ARN not the Key ID.
     *
     * <p><b>General purpose buckets</b> - If you specify <code>x-amz-server-side-encryption</code> with <code>aws:kms</code>
     * or <code>aws:kms:dsse</code>, this header specifies the ID (Key ID, Key ARN, or Key Alias) of the KMS key to use.
     * If you specify <code>x-amz-server-side-encryption:aws:kms</code> or <code>
     * x-amz-server-side-encryption:aws:kms:dsse</code>, but do not provide <code>
     * x-amz-server-side-encryption-aws-kms-key-id</code>, Amazon S3 uses the Amazon Web Services managed key (<code>
     * aws/s3</code>) to protect the data.
     *
     * <p><b>Directory buckets</b> - To encrypt data using SSE-KMS, it's recommended to specify the <code>
     * x-amz-server-side-encryption</code> header to <code>aws:kms</code>. Then, the <code>
     * x-amz-server-side-encryption-aws-kms-key-id</code> header implicitly uses the bucket's default KMS customer
     * managed key ID. If you want to explicitly set the <code> x-amz-server-side-encryption-aws-kms-key-id</code>
     * header, it must match the bucket's default customer managed key (using key ID or ARN, not alias). Your SSE-KMS
     * configuration can only support 1 <a href="https://docs.aws.amazon.com/kms/latest/developerguide/concepts.html#customer-cmk">customer managed key</a> per directory bucket's lifetime. The <a href="https://docs.aws.amazon.com/kms/latest/developerguide/concepts.html#aws-managed-cmk">Amazon Web
     * Services managed key</a> (<code>aws/s3</code>) isn't supported.
     *
     * <p>Incorrect key specification results in an HTTP <code>400 Bad Request</code> error.
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
     * Key Management Service (KMS) keys (SSE-KMS).
     *
     * <p><b>General purpose buckets</b> - Setting this header to <code>true</code> causes Amazon S3 to use an S3 Bucket
     * Key for object encryption with SSE-KMS. Also, specifying this header with a PUT action doesn't affect
     * bucket-level settings for S3 Bucket Key.
     *
     * <p><b>Directory buckets</b> - S3 Bucket Keys are always enabled for <code>GET</code> and <code>PUT</code>
     * operations in a directory bucket and can’t be disabled. S3 Bucket Keys aren't supported, when you copy SSE-KMS
     * encrypted objects from general purpose buckets to directory buckets, from directory buckets to general purpose
     * buckets, or between directory buckets, through <a href="https://docs.aws.amazon.com/AmazonS3/latest/API/API_CopyObject.html">CopyObject</a>, <a href="https://docs.aws.amazon.com/AmazonS3/latest/API/API_UploadPartCopy.html">UploadPartCopy</a>, <a href="https://docs.aws.amazon.com/AmazonS3/latest/userguide/directory-buckets-objects-Batch-Ops">the Copy operation in
     * Batch Operations</a>, or <a href="https://docs.aws.amazon.com/AmazonS3/latest/userguide/create-import-job">the import jobs</a>. In this case, Amazon S3 makes a call to KMS every time a copy
     * request is made for a KMS-encrypted object.
     */
    public Boolean isBucketKeyEnabled() {
        return bucketKeyEnabled;
    }

    public RequestPayer getRequestPayer() {
        return requestPayer;
    }

    /**
     * The tag-set for the object. The tag-set must be encoded as URL Query parameters. (For example, "Key1=Value1")
     *
     * <p>This functionality is not supported for directory buckets.
     */
    public String getTagging() {
        return tagging;
    }

    /**
     * The Object Lock mode that you want to apply to this object.
     *
     * <p>This functionality is not supported for directory buckets.
     */
    public ObjectLockMode getObjectLockMode() {
        return objectLockMode;
    }

    /**
     * The date and time when you want this object's Object Lock to expire. Must be formatted as a timestamp parameter.
     *
     * <p>This functionality is not supported for directory buckets.
     */
    public Instant getObjectLockRetainUntilDate() {
        return objectLockRetainUntilDate;
    }

    /**
     * Specifies whether a legal hold will be applied to this object. For more information about S3 Object Lock, see <a href="https://docs.aws.amazon.com/AmazonS3/latest/dev/object-lock.html">
     * Object Lock</a> in the <i>Amazon S3 User Guide</i>.
     *
     * <p>This functionality is not supported for directory buckets.
     */
    public ObjectLockLegalHoldStatus getObjectLockLegalHoldStatus() {
        return objectLockLegalHoldStatus;
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
        PutObjectInput that = (PutObjectInput) other;
        return Objects.equals(this.bucketKeyEnabled, that.bucketKeyEnabled)
               && Objects.equals(this.contentLength, that.contentLength)
               && Objects.equals(this.writeOffsetBytes, that.writeOffsetBytes)
               && Objects.equals(this.bucket, that.bucket)
               && Objects.equals(this.cacheControl, that.cacheControl)
               && Objects.equals(this.contentDisposition, that.contentDisposition)
               && Objects.equals(this.contentEncoding, that.contentEncoding)
               && Objects.equals(this.contentLanguage, that.contentLanguage)
               && Objects.equals(this.contentmD5, that.contentmD5)
               && Objects.equals(this.contentType, that.contentType)
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
               && Objects.equals(this.expires, that.expires)
               && Objects.equals(this.ifMatch, that.ifMatch)
               && Objects.equals(this.ifNoneMatch, that.ifNoneMatch)
               && Objects.equals(this.grantFullControl, that.grantFullControl)
               && Objects.equals(this.grantRead, that.grantRead)
               && Objects.equals(this.grantReadacP, that.grantReadacP)
               && Objects.equals(this.grantWriteacP, that.grantWriteacP)
               && Objects.equals(this.key, that.key)
               && Objects.equals(this.websiteRedirectLocation, that.websiteRedirectLocation)
               && Objects.equals(this.sseCustomerAlgorithm, that.sseCustomerAlgorithm)
               && Objects.equals(this.sseCustomerKey, that.sseCustomerKey)
               && Objects.equals(this.sseCustomerKeymD5, that.sseCustomerKeymD5)
               && Objects.equals(this.ssekmsKeyId, that.ssekmsKeyId)
               && Objects.equals(this.ssekmsEncryptionContext, that.ssekmsEncryptionContext)
               && Objects.equals(this.tagging, that.tagging)
               && Objects.equals(this.expectedBucketOwner, that.expectedBucketOwner)
               && Objects.equals(this.acl, that.acl)
               && Objects.equals(this.checksumAlgorithm, that.checksumAlgorithm)
               && Objects.equals(this.serverSideEncryption, that.serverSideEncryption)
               && Objects.equals(this.storageClass, that.storageClass)
               && Objects.equals(this.requestPayer, that.requestPayer)
               && Objects.equals(this.objectLockMode, that.objectLockMode)
               && Objects.equals(this.objectLockLegalHoldStatus, that.objectLockLegalHoldStatus)
               && Objects.equals(this.objectLockRetainUntilDate, that.objectLockRetainUntilDate)
               && Objects.equals(this.body, that.body)
               && Objects.equals(this.metadata, that.metadata);
    }

    @Override
    public int hashCode() {
        int $hc = Objects.hashCode(acl);
        $hc = 31 * $hc + Objects.hashCode(body);
        $hc = 31 * $hc + Objects.hashCode(bucket);
        $hc = 31 * $hc + Objects.hashCode(cacheControl);
        $hc = 31 * $hc + Objects.hashCode(contentDisposition);
        $hc = 31 * $hc + Objects.hashCode(contentEncoding);
        $hc = 31 * $hc + Objects.hashCode(contentLanguage);
        $hc = 31 * $hc + Objects.hashCode(contentLength);
        $hc = 31 * $hc + Objects.hashCode(contentmD5);
        $hc = 31 * $hc + Objects.hashCode(contentType);
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
        $hc = 31 * $hc + Objects.hashCode(expires);
        $hc = 31 * $hc + Objects.hashCode(ifMatch);
        $hc = 31 * $hc + Objects.hashCode(ifNoneMatch);
        $hc = 31 * $hc + Objects.hashCode(grantFullControl);
        $hc = 31 * $hc + Objects.hashCode(grantRead);
        $hc = 31 * $hc + Objects.hashCode(grantReadacP);
        $hc = 31 * $hc + Objects.hashCode(grantWriteacP);
        $hc = 31 * $hc + Objects.hashCode(key);
        $hc = 31 * $hc + Objects.hashCode(writeOffsetBytes);
        $hc = 31 * $hc + Objects.hashCode(metadata);
        $hc = 31 * $hc + Objects.hashCode(serverSideEncryption);
        $hc = 31 * $hc + Objects.hashCode(storageClass);
        $hc = 31 * $hc + Objects.hashCode(websiteRedirectLocation);
        $hc = 31 * $hc + Objects.hashCode(sseCustomerAlgorithm);
        $hc = 31 * $hc + Objects.hashCode(sseCustomerKey);
        $hc = 31 * $hc + Objects.hashCode(sseCustomerKeymD5);
        $hc = 31 * $hc + Objects.hashCode(ssekmsKeyId);
        $hc = 31 * $hc + Objects.hashCode(ssekmsEncryptionContext);
        $hc = 31 * $hc + Objects.hashCode(bucketKeyEnabled);
        $hc = 31 * $hc + Objects.hashCode(requestPayer);
        $hc = 31 * $hc + Objects.hashCode(tagging);
        $hc = 31 * $hc + Objects.hashCode(objectLockMode);
        $hc = 31 * $hc + Objects.hashCode(objectLockRetainUntilDate);
        $hc = 31 * $hc + Objects.hashCode(objectLockLegalHoldStatus);
        $hc = 31 * $hc + Objects.hashCode(expectedBucketOwner);
        return $hc;
    }

    @Override
    public Schema schema() {
        return $SCHEMA;
    }

    @Override
    public void serializeMembers(ShapeSerializer serializer) {
        if (acl != null) {
            serializer.writeString($SCHEMA_ACL, acl.getValue());
        }
        serializer.writeDataStream($SCHEMA_BODY, body);
        serializer.writeString($SCHEMA_BUCKET, bucket);
        if (cacheControl != null) {
            serializer.writeString($SCHEMA_CACHE_CONTROL, cacheControl);
        }
        if (contentDisposition != null) {
            serializer.writeString($SCHEMA_CONTENT_DISPOSITION, contentDisposition);
        }
        if (contentEncoding != null) {
            serializer.writeString($SCHEMA_CONTENT_ENCODING, contentEncoding);
        }
        if (contentLanguage != null) {
            serializer.writeString($SCHEMA_CONTENT_LANGUAGE, contentLanguage);
        }
        if (contentLength != null) {
            serializer.writeLong($SCHEMA_CONTENT_LENGTH, contentLength);
        }
        if (contentmD5 != null) {
            serializer.writeString($SCHEMA_CONTENTM_D5, contentmD5);
        }
        if (contentType != null) {
            serializer.writeString($SCHEMA_CONTENT_TYPE, contentType);
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
        if (expires != null) {
            serializer.writeString($SCHEMA_EXPIRES, expires);
        }
        if (ifMatch != null) {
            serializer.writeString($SCHEMA_IF_MATCH, ifMatch);
        }
        if (ifNoneMatch != null) {
            serializer.writeString($SCHEMA_IF_NONE_MATCH, ifNoneMatch);
        }
        if (grantFullControl != null) {
            serializer.writeString($SCHEMA_GRANT_FULL_CONTROL, grantFullControl);
        }
        if (grantRead != null) {
            serializer.writeString($SCHEMA_GRANT_READ, grantRead);
        }
        if (grantReadacP != null) {
            serializer.writeString($SCHEMA_GRANT_READAC_P, grantReadacP);
        }
        if (grantWriteacP != null) {
            serializer.writeString($SCHEMA_GRANT_WRITEAC_P, grantWriteacP);
        }
        serializer.writeString($SCHEMA_KEY, key);
        if (writeOffsetBytes != null) {
            serializer.writeLong($SCHEMA_WRITE_OFFSET_BYTES, writeOffsetBytes);
        }
        if (metadata != null) {
            serializer.writeMap($SCHEMA_METADATA, metadata, metadata.size(), SharedSerde.MetadataSerializer.INSTANCE);
        }
        if (serverSideEncryption != null) {
            serializer.writeString($SCHEMA_SERVER_SIDE_ENCRYPTION, serverSideEncryption.getValue());
        }
        if (storageClass != null) {
            serializer.writeString($SCHEMA_STORAGE_CLASS, storageClass.getValue());
        }
        if (websiteRedirectLocation != null) {
            serializer.writeString($SCHEMA_WEBSITE_REDIRECT_LOCATION, websiteRedirectLocation);
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
        if (ssekmsKeyId != null) {
            serializer.writeString($SCHEMA_SSEKMS_KEY_ID, ssekmsKeyId);
        }
        if (ssekmsEncryptionContext != null) {
            serializer.writeString($SCHEMA_SSEKMS_ENCRYPTION_CONTEXT, ssekmsEncryptionContext);
        }
        if (bucketKeyEnabled != null) {
            serializer.writeBoolean($SCHEMA_BUCKET_KEY_ENABLED, bucketKeyEnabled);
        }
        if (requestPayer != null) {
            serializer.writeString($SCHEMA_REQUEST_PAYER, requestPayer.getValue());
        }
        if (tagging != null) {
            serializer.writeString($SCHEMA_TAGGING, tagging);
        }
        if (objectLockMode != null) {
            serializer.writeString($SCHEMA_OBJECT_LOCK_MODE, objectLockMode.getValue());
        }
        if (objectLockRetainUntilDate != null) {
            serializer.writeTimestamp($SCHEMA_OBJECT_LOCK_RETAIN_UNTIL_DATE, objectLockRetainUntilDate);
        }
        if (objectLockLegalHoldStatus != null) {
            serializer.writeString($SCHEMA_OBJECT_LOCK_LEGAL_HOLD_STATUS, objectLockLegalHoldStatus.getValue());
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
            case 2 -> (T) SchemaUtils.validateSameMember($SCHEMA_ACL, member, acl);
            case 3 -> (T) SchemaUtils.validateSameMember($SCHEMA_BODY, member, body);
            case 4 -> (T) SchemaUtils.validateSameMember($SCHEMA_CACHE_CONTROL, member, cacheControl);
            case 5 -> (T) SchemaUtils.validateSameMember($SCHEMA_CONTENT_DISPOSITION, member, contentDisposition);
            case 6 -> (T) SchemaUtils.validateSameMember($SCHEMA_CONTENT_ENCODING, member, contentEncoding);
            case 7 -> (T) SchemaUtils.validateSameMember($SCHEMA_CONTENT_LANGUAGE, member, contentLanguage);
            case 8 -> (T) SchemaUtils.validateSameMember($SCHEMA_CONTENT_LENGTH, member, contentLength);
            case 9 -> (T) SchemaUtils.validateSameMember($SCHEMA_CONTENTM_D5, member, contentmD5);
            case 10 -> (T) SchemaUtils.validateSameMember($SCHEMA_CONTENT_TYPE, member, contentType);
            case 11 -> (T) SchemaUtils.validateSameMember($SCHEMA_CHECKSUM_ALGORITHM, member, checksumAlgorithm);
            case 12 -> (T) SchemaUtils.validateSameMember($SCHEMA_CHECKSUMCR_C32, member, checksumcrC32);
            case 13 -> (T) SchemaUtils.validateSameMember($SCHEMA_CHECKSUMCR_C32_C, member, checksumcrC32C);
            case 14 -> (T) SchemaUtils.validateSameMember($SCHEMA_CHECKSUMCR_C64NVM_E, member, checksumcrC64nvmE);
            case 15 -> (T) SchemaUtils.validateSameMember($SCHEMA_CHECKSUMSH_A1, member, checksumshA1);
            case 16 -> (T) SchemaUtils.validateSameMember($SCHEMA_CHECKSUMSH_A256, member, checksumshA256);
            case 17 -> (T) SchemaUtils.validateSameMember($SCHEMA_CHECKSUMSH_A512, member, checksumshA512);
            case 18 -> (T) SchemaUtils.validateSameMember($SCHEMA_CHECKSUMM_D5, member, checksummD5);
            case 19 -> (T) SchemaUtils.validateSameMember($SCHEMA_CHECKSUMXXHAS_H64, member, checksumxxhasH64);
            case 20 -> (T) SchemaUtils.validateSameMember($SCHEMA_CHECKSUMXXHAS_H3, member, checksumxxhasH3);
            case 21 -> (T) SchemaUtils.validateSameMember($SCHEMA_CHECKSUMXXHAS_H128, member, checksumxxhasH128);
            case 22 -> (T) SchemaUtils.validateSameMember($SCHEMA_EXPIRES, member, expires);
            case 23 -> (T) SchemaUtils.validateSameMember($SCHEMA_IF_MATCH, member, ifMatch);
            case 24 -> (T) SchemaUtils.validateSameMember($SCHEMA_IF_NONE_MATCH, member, ifNoneMatch);
            case 25 -> (T) SchemaUtils.validateSameMember($SCHEMA_GRANT_FULL_CONTROL, member, grantFullControl);
            case 26 -> (T) SchemaUtils.validateSameMember($SCHEMA_GRANT_READ, member, grantRead);
            case 27 -> (T) SchemaUtils.validateSameMember($SCHEMA_GRANT_READAC_P, member, grantReadacP);
            case 28 -> (T) SchemaUtils.validateSameMember($SCHEMA_GRANT_WRITEAC_P, member, grantWriteacP);
            case 29 -> (T) SchemaUtils.validateSameMember($SCHEMA_WRITE_OFFSET_BYTES, member, writeOffsetBytes);
            case 30 -> (T) SchemaUtils.validateSameMember($SCHEMA_METADATA, member, metadata);
            case 31 -> (T) SchemaUtils.validateSameMember($SCHEMA_SERVER_SIDE_ENCRYPTION, member, serverSideEncryption);
            case 32 -> (T) SchemaUtils.validateSameMember($SCHEMA_STORAGE_CLASS, member, storageClass);
            case 33 -> (T) SchemaUtils.validateSameMember($SCHEMA_WEBSITE_REDIRECT_LOCATION, member, websiteRedirectLocation);
            case 34 -> (T) SchemaUtils.validateSameMember($SCHEMA_SSE_CUSTOMER_ALGORITHM, member, sseCustomerAlgorithm);
            case 35 -> (T) SchemaUtils.validateSameMember($SCHEMA_SSE_CUSTOMER_KEY, member, sseCustomerKey);
            case 36 -> (T) SchemaUtils.validateSameMember($SCHEMA_SSE_CUSTOMER_KEYM_D5, member, sseCustomerKeymD5);
            case 37 -> (T) SchemaUtils.validateSameMember($SCHEMA_SSEKMS_KEY_ID, member, ssekmsKeyId);
            case 38 -> (T) SchemaUtils.validateSameMember($SCHEMA_SSEKMS_ENCRYPTION_CONTEXT, member, ssekmsEncryptionContext);
            case 39 -> (T) SchemaUtils.validateSameMember($SCHEMA_BUCKET_KEY_ENABLED, member, bucketKeyEnabled);
            case 40 -> (T) SchemaUtils.validateSameMember($SCHEMA_REQUEST_PAYER, member, requestPayer);
            case 41 -> (T) SchemaUtils.validateSameMember($SCHEMA_TAGGING, member, tagging);
            case 42 -> (T) SchemaUtils.validateSameMember($SCHEMA_OBJECT_LOCK_MODE, member, objectLockMode);
            case 43 -> (T) SchemaUtils.validateSameMember($SCHEMA_OBJECT_LOCK_RETAIN_UNTIL_DATE, member, objectLockRetainUntilDate);
            case 44 -> (T) SchemaUtils.validateSameMember($SCHEMA_OBJECT_LOCK_LEGAL_HOLD_STATUS, member, objectLockLegalHoldStatus);
            case 45 -> (T) SchemaUtils.validateSameMember($SCHEMA_EXPECTED_BUCKET_OWNER, member, expectedBucketOwner);
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
     * @return a builder for {@link PutObjectInput}.
     */
    public Builder toBuilder() {
        var builder = new Builder();
        builder.acl(this.acl);
        builder.body(this.body);
        builder.bucket(this.bucket);
        builder.cacheControl(this.cacheControl);
        builder.contentDisposition(this.contentDisposition);
        builder.contentEncoding(this.contentEncoding);
        builder.contentLanguage(this.contentLanguage);
        builder.contentLength(this.contentLength);
        builder.contentmD5(this.contentmD5);
        builder.contentType(this.contentType);
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
        builder.expires(this.expires);
        builder.ifMatch(this.ifMatch);
        builder.ifNoneMatch(this.ifNoneMatch);
        builder.grantFullControl(this.grantFullControl);
        builder.grantRead(this.grantRead);
        builder.grantReadacP(this.grantReadacP);
        builder.grantWriteacP(this.grantWriteacP);
        builder.key(this.key);
        builder.writeOffsetBytes(this.writeOffsetBytes);
        builder.metadata(this.metadata);
        builder.serverSideEncryption(this.serverSideEncryption);
        builder.storageClass(this.storageClass);
        builder.websiteRedirectLocation(this.websiteRedirectLocation);
        builder.sseCustomerAlgorithm(this.sseCustomerAlgorithm);
        builder.sseCustomerKey(this.sseCustomerKey);
        builder.sseCustomerKeymD5(this.sseCustomerKeymD5);
        builder.ssekmsKeyId(this.ssekmsKeyId);
        builder.ssekmsEncryptionContext(this.ssekmsEncryptionContext);
        builder.bucketKeyEnabled(this.bucketKeyEnabled);
        builder.requestPayer(this.requestPayer);
        builder.tagging(this.tagging);
        builder.objectLockMode(this.objectLockMode);
        builder.objectLockRetainUntilDate(this.objectLockRetainUntilDate);
        builder.objectLockLegalHoldStatus(this.objectLockLegalHoldStatus);
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
     * Builder for {@link PutObjectInput}.
     */
    public static final class Builder implements ShapeBuilder<PutObjectInput> {
        private final PresenceTracker tracker = PresenceTracker.of($SCHEMA);
        private ObjectCannedACL acl;
        private DataStream body = DataStream.ofEmpty();
        private String bucket;
        private String cacheControl;
        private String contentDisposition;
        private String contentEncoding;
        private String contentLanguage;
        private Long contentLength;
        private String contentmD5;
        private String contentType;
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
        private String expires;
        private String ifMatch;
        private String ifNoneMatch;
        private String grantFullControl;
        private String grantRead;
        private String grantReadacP;
        private String grantWriteacP;
        private String key;
        private Long writeOffsetBytes;
        private Map<String, String> metadata;
        private ServerSideEncryption serverSideEncryption;
        private StorageClass storageClass;
        private String websiteRedirectLocation;
        private String sseCustomerAlgorithm;
        private String sseCustomerKey;
        private String sseCustomerKeymD5;
        private String ssekmsKeyId;
        private String ssekmsEncryptionContext;
        private Boolean bucketKeyEnabled;
        private RequestPayer requestPayer;
        private String tagging;
        private ObjectLockMode objectLockMode;
        private Instant objectLockRetainUntilDate;
        private ObjectLockLegalHoldStatus objectLockLegalHoldStatus;
        private String expectedBucketOwner;

        private Builder() {}

        @Override
        public Schema schema() {
            return $SCHEMA;
        }

        /**
         * The canned ACL to apply to the object. For more information, see <a href="https://docs.aws.amazon.com/AmazonS3/latest/dev/acl-overview.html#CannedACL">Canned ACL</a> in the <i>Amazon S3 User Guide</i>
         * .
         *
         * <p>When adding a new object, you can use headers to grant ACL-based permissions to individual Amazon Web Services
         * accounts or to predefined groups defined by Amazon S3. These permissions are then added to the ACL on the object.
         * By default, all objects are private. Only the owner has full access control. For more information, see <a href="https://docs.aws.amazon.com/AmazonS3/latest/dev/acl-overview.html">Access
         * Control List (ACL) Overview</a> and <a href="https://docs.aws.amazon.com/AmazonS3/latest/dev/acl-using-rest-api.html">Managing ACLs Using the REST API</a> in the <i>Amazon S3 User Guide</i>.
         *
         * <p>If the bucket that you're uploading objects to uses the bucket owner enforced setting for S3 Object Ownership,
         * ACLs are disabled and no longer affect permissions. Buckets that use this setting only accept PUT requests that
         * don't specify an ACL or PUT requests that specify bucket owner full control ACLs, such as the <code>
         * bucket-owner-full-control</code> canned ACL or an equivalent form of this ACL expressed in the XML format. PUT
         * requests that contain other ACLs (for example, custom grants to certain Amazon Web Services accounts) fail and
         * return a <code>400</code> error with the error code <code>AccessControlListNotSupported</code>. For more
         * information, see <a href="https://docs.aws.amazon.com/AmazonS3/latest/userguide/about-object-ownership.html"> Controlling ownership of objects and disabling ACLs</a> in the <i>Amazon S3 User Guide</i>.
         *
         * <ul>
         *   <li>
         *     This functionality is not supported for directory buckets.
         *   </li>
         *   <li>
         *     This functionality is not supported for Amazon S3 on Outposts.
         *   </li>
         * </ul>
         *
         * @return this builder.
         */
        public Builder acl(ObjectCannedACL acl) {
            this.acl = acl;
            return this;
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
         * The bucket name to which the PUT action was initiated.
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
         * Can be used to specify caching behavior along the request/reply chain. For more information, see <a
         * href="http://www.w3.org/Protocols/rfc2616/rfc2616-sec14.html#sec14.9">http://www.w3.org/Protocols/rfc2616/rfc2616-sec14.html#sec14.9</a>.
         *
         * @return this builder.
         */
        public Builder cacheControl(String cacheControl) {
            this.cacheControl = cacheControl;
            return this;
        }

        /**
         * Specifies presentational information for the object. For more information, see <a
         * href="https://www.rfc-editor.org/rfc/rfc6266#section-4">https://www.rfc-editor.org/rfc/rfc6266#section-4</a>.
         *
         * @return this builder.
         */
        public Builder contentDisposition(String contentDisposition) {
            this.contentDisposition = contentDisposition;
            return this;
        }

        /**
         * Specifies what content encodings have been applied to the object and thus what decoding mechanisms must be
         * applied to obtain the media-type referenced by the Content-Type header field. For more information, see <a
         * href="https://www.rfc-editor.org/rfc/rfc9110.html#field.content-encoding">https://www.rfc-editor.org/rfc/rfc9110.html#field.content-encoding</a>.
         *
         * @return this builder.
         */
        public Builder contentEncoding(String contentEncoding) {
            this.contentEncoding = contentEncoding;
            return this;
        }

        /**
         * The language the content is in.
         *
         * @return this builder.
         */
        public Builder contentLanguage(String contentLanguage) {
            this.contentLanguage = contentLanguage;
            return this;
        }

        /**
         * Size of the body in bytes. This parameter is useful when the size of the body cannot be determined automatically.
         * For more information, see <a
         * href="https://www.rfc-editor.org/rfc/rfc9110.html#name-content-length">https://www.rfc-editor.org/rfc/rfc9110.html#name-content-length</a>.
         *
         * @return this builder.
         */
        public Builder contentLength(Long contentLength) {
            this.contentLength = contentLength;
            return this;
        }

        /**
         * The Base64 encoded 128-bit <code>MD5</code> digest of the message (without the headers) according to RFC 1864.
         * This header can be used as a message integrity check to verify that the data is the same data that was originally
         * sent. Although it is optional, we recommend using the Content-MD5 mechanism as an end-to-end integrity check. For
         * more information about REST request authentication, see <a href="https://docs.aws.amazon.com/AmazonS3/latest/dev/RESTAuthentication.html">REST Authentication</a>.
         *
         * <p>The <code>Content-MD5</code> or <code>x-amz-sdk-checksum-algorithm</code> header is required for any request
         * to upload an object with a retention period configured using Amazon S3 Object Lock. For more information, see <a href="https://docs.aws.amazon.com/AmazonS3/latest/userguide/object-lock-managing.html#object-lock-put-object">
         * Uploading objects to an Object Lock enabled bucket </a> in the <i>Amazon S3 User Guide</i>.
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
         * A standard MIME type describing the format of the contents. For more information, see <a
         * href="https://www.rfc-editor.org/rfc/rfc9110.html#name-content-type">https://www.rfc-editor.org/rfc/rfc9110.html#name-content-type</a>.
         *
         * @return this builder.
         */
        public Builder contentType(String contentType) {
            this.contentType = contentType;
            return this;
        }

        /**
         * Indicates the algorithm used to create the checksum for the object when you use the SDK. This header will not
         * provide any additional functionality if you don't use the SDK. When you send this header, there must be a
         * corresponding <code>x-amz-checksum-<i>algorithm</i></code> or <code>x-amz-trailer</code> header sent. Otherwise,
         * Amazon S3 fails the request with the HTTP status code <code>400 Bad Request</code>.
         *
         * <p>For the <code>x-amz-checksum-<i>algorithm</i></code> header, replace <code><i>algorithm</i></code> with the
         * supported algorithm from the following list:
         *
         * <ul>
         *   <li>
         *     <code>CRC32</code>
         *   </li>
         *   <li>
         *     <code>CRC32C</code>
         *   </li>
         *   <li>
         *     <code>CRC64NVME</code>
         *   </li>
         *   <li>
         *     <code>MD5</code>
         *   </li>
         *   <li>
         *     <code>SHA1</code>
         *   </li>
         *   <li>
         *     <code>SHA256</code>
         *   </li>
         *   <li>
         *     <code>SHA512</code>
         *   </li>
         *   <li>
         *     <code>XXHASH3</code>
         *   </li>
         *   <li>
         *     <code>XXHASH64</code>
         *   </li>
         *   <li>
         *     <code>XXHASH128</code>
         *   </li>
         * </ul>
         *
         * <p>For more information, see <a href="https://docs.aws.amazon.com/AmazonS3/latest/userguide/checking-object-integrity.html">Checking object integrity</a> in the <i>Amazon S3 User Guide</i>.
         *
         * <p>If the individual checksum value you provide through <code>x-amz-checksum-<i>algorithm</i></code> doesn't
         * match the checksum algorithm you set through <code>x-amz-sdk-checksum-algorithm</code>, Amazon S3 fails the
         * request with a <code>BadDigest</code> error.
         *
         * <p>The <code>Content-MD5</code> or <code>x-amz-sdk-checksum-algorithm</code> header is required for any request
         * to upload an object with a retention period configured using Amazon S3 Object Lock. For more information, see <a href="https://docs.aws.amazon.com/AmazonS3/latest/userguide/object-lock-managing.html#object-lock-put-object">
         * Uploading objects to an Object Lock enabled bucket </a> in the <i>Amazon S3 User Guide</i>.
         *
         * <p>For directory buckets, when you use Amazon Web Services SDKs, <code>CRC32</code> is the default checksum
         * algorithm that's used for performance.
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
         * The date and time at which the object is no longer cacheable. For more information, see <a
         * href="https://www.rfc-editor.org/rfc/rfc7234#section-5.3">https://www.rfc-editor.org/rfc/rfc7234#section-5.3</a>.
         *
         * @return this builder.
         */
        public Builder expires(String expires) {
            this.expires = expires;
            return this;
        }

        /**
         * Uploads the object only if the ETag (entity tag) value provided during the WRITE operation matches the ETag of
         * the object in S3. If the ETag values do not match, the operation returns a <code>412 Precondition Failed</code>
         * error.
         *
         * <p>If a conflicting operation occurs during the upload S3 returns a <code>409 ConditionalRequestConflict</code>
         * response. On a 409 failure you should fetch the object's ETag and retry the upload.
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
         * response. On a 409 failure you should retry the upload.
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
         * Gives the grantee READ, READ_ACP, and WRITE_ACP permissions on the object.
         *
         * <ul>
         *   <li>
         *     This functionality is not supported for directory buckets.
         *   </li>
         *   <li>
         *     This functionality is not supported for Amazon S3 on Outposts.
         *   </li>
         * </ul>
         *
         * @return this builder.
         */
        public Builder grantFullControl(String grantFullControl) {
            this.grantFullControl = grantFullControl;
            return this;
        }

        /**
         * Allows grantee to read the object data and its metadata.
         *
         * <ul>
         *   <li>
         *     This functionality is not supported for directory buckets.
         *   </li>
         *   <li>
         *     This functionality is not supported for Amazon S3 on Outposts.
         *   </li>
         * </ul>
         *
         * @return this builder.
         */
        public Builder grantRead(String grantRead) {
            this.grantRead = grantRead;
            return this;
        }

        /**
         * Allows grantee to read the object ACL.
         *
         * <ul>
         *   <li>
         *     This functionality is not supported for directory buckets.
         *   </li>
         *   <li>
         *     This functionality is not supported for Amazon S3 on Outposts.
         *   </li>
         * </ul>
         *
         * @return this builder.
         */
        public Builder grantReadacP(String grantReadacP) {
            this.grantReadacP = grantReadacP;
            return this;
        }

        /**
         * Allows grantee to write the ACL for the applicable object.
         *
         * <ul>
         *   <li>
         *     This functionality is not supported for directory buckets.
         *   </li>
         *   <li>
         *     This functionality is not supported for Amazon S3 on Outposts.
         *   </li>
         * </ul>
         *
         * @return this builder.
         */
        public Builder grantWriteacP(String grantWriteacP) {
            this.grantWriteacP = grantWriteacP;
            return this;
        }

        /**
         * Object key for which the PUT action was initiated.
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
         * Specifies the offset for appending data to existing objects in bytes. The offset must be equal to the size of
         * the existing object being appended to. If no object exists, setting this header to 0 will create a new object.
         *
         * <p>This functionality is only supported for objects in the Amazon S3 Express One Zone storage class in directory
         * buckets.
         *
         * @return this builder.
         */
        public Builder writeOffsetBytes(Long writeOffsetBytes) {
            this.writeOffsetBytes = writeOffsetBytes;
            return this;
        }

        /**
         * A map of metadata to store with the object in S3.
         *
         * @return this builder.
         */
        public Builder metadata(Map<String, String> metadata) {
            this.metadata = metadata;
            return this;
        }

        /**
         * The server-side encryption algorithm that was used when you store this object in Amazon S3 or Amazon FSx.
         *
         * <ul>
         *   <li>
         *     <b>General purpose buckets </b> - You have four mutually exclusive options to protect data using
         *     server-side encryption in Amazon S3, depending on how you choose to manage the encryption keys.
         *     Specifically, the encryption key options are Amazon S3 managed keys (SSE-S3), Amazon Web Services KMS
         *     keys (SSE-KMS or DSSE-KMS), and customer-provided keys (SSE-C). Amazon S3 encrypts data with server-side
         *     encryption by using Amazon S3 managed keys (SSE-S3) by default. You can optionally tell Amazon S3 to
         *     encrypt data at rest by using server-side encryption with other key options. For more information, see <a href="https://docs.aws.amazon.com/AmazonS3/latest/dev/UsingServerSideEncryption.html">
         *     Using Server-Side Encryption</a> in the <i>Amazon S3 User Guide</i>.
         *   </li>
         *   <li>
         *     <b>Directory buckets </b> - For directory buckets, there are only two supported options for server-side
         *     encryption: server-side encryption with Amazon S3 managed keys (SSE-S3) (<code>AES256</code>) and
         *     server-side encryption with KMS keys (SSE-KMS) (<code>aws:kms</code>). We recommend that the bucket's
         *     default encryption uses the desired encryption configuration and you don't override the bucket default
         *     encryption in your <code>CreateSession</code> requests or <code>PUT</code> object requests. Then, new
         *     objects are automatically encrypted with the desired encryption settings. For more information, see <a href="https://docs.aws.amazon.com/AmazonS3/latest/userguide/s3-express-serv-side-encryption.html">
         *     Protecting data with server-side encryption</a> in the <i>Amazon S3 User Guide</i>. For more information
         *     about the encryption overriding behaviors in directory buckets, see <a href="https://docs.aws.amazon.com/AmazonS3/latest/userguide/s3-express-specifying-kms-encryption.html">Specifying server-side encryption
         *     with KMS for new object uploads</a>. In the Zonal endpoint API calls (except <a href="https://docs.aws.amazon.com/AmazonS3/latest/API/API_CopyObject.html">CopyObject</a> and <a href="https://docs.aws.amazon.com/AmazonS3/latest/API/API_UploadPartCopy.html">
         *     UploadPartCopy</a>) using the REST API, the encryption request headers must match the encryption settings
         *     that are specified in the <code>CreateSession</code> request. You can't override the values of the
         *     encryption settings (<code>x-amz-server-side-encryption</code>, <code>
         *     x-amz-server-side-encryption-aws-kms-key-id</code>, <code>x-amz-server-side-encryption-context</code>,
         *     and <code>x-amz-server-side-encryption-bucket-key-enabled</code>) that are specified in the <code>
         *     CreateSession</code> request. You don't need to explicitly specify these encryption settings values in
         *     Zonal endpoint API calls, and Amazon S3 will use the encryption settings values from the <code>
         *     CreateSession</code> request to protect new objects in the directory bucket. When you use the CLI or the
         *     Amazon Web Services SDKs, for <code>CreateSession</code>, the session token refreshes automatically to
         *     avoid service interruptions when a session expires. The CLI or the Amazon Web Services SDKs use the
         *     bucket's default encryption configuration for the <code>CreateSession</code> request. It's not supported
         *     to override the encryption settings values in the <code>CreateSession</code> request. So in the Zonal
         *     endpoint API calls (except <a href="https://docs.aws.amazon.com/AmazonS3/latest/API/API_CopyObject.html">CopyObject</a> and <a href="https://docs.aws.amazon.com/AmazonS3/latest/API/API_UploadPartCopy.html">UploadPartCopy</a>), the encryption request headers
         *     must match the default encryption configuration of the directory bucket.
         *   </li>
         *   <li>
         *     <b>S3 access points for Amazon FSx </b> - When accessing data stored in Amazon FSx file systems using S3
         *     access points, the only valid server side encryption option is <code>aws:fsx</code>. All Amazon FSx file
         *     systems have encryption configured by default and are encrypted at rest. Data is automatically encrypted
         *     before being written to the file system, and automatically decrypted as it is read. These processes are
         *     handled transparently by Amazon FSx.
         *   </li>
         * </ul>
         *
         * @return this builder.
         */
        public Builder serverSideEncryption(ServerSideEncryption serverSideEncryption) {
            this.serverSideEncryption = serverSideEncryption;
            return this;
        }

        /**
         * By default, Amazon S3 uses the STANDARD Storage Class to store newly created objects. The STANDARD storage class
         * provides high durability and high availability. Depending on performance needs, you can specify a different
         * Storage Class. For more information, see <a href="https://docs.aws.amazon.com/AmazonS3/latest/dev/storage-class-intro.html">Storage Classes</a> in the <i>Amazon S3 User Guide</i>.
         *
         * <ul>
         *   <li>
         *     Directory buckets only support <code>EXPRESS_ONEZONE</code> (the S3 Express One Zone storage class) in
         *     Availability Zones and <code>ONEZONE_IA</code> (the S3 One Zone-Infrequent Access storage class) in
         *     Dedicated Local Zones.
         *   </li>
         *   <li>
         *     Amazon S3 on Outposts only uses the OUTPOSTS Storage Class.
         *   </li>
         * </ul>
         *
         * @return this builder.
         */
        public Builder storageClass(StorageClass storageClass) {
            this.storageClass = storageClass;
            return this;
        }

        /**
         * If the bucket is configured as a website, redirects requests for this object to another object in the same bucket
         * or to an external URL. Amazon S3 stores the value of this header in the object metadata. For information about
         * object metadata, see <a href="https://docs.aws.amazon.com/AmazonS3/latest/dev/UsingMetadata.html">Object Key and Metadata</a> in the <i>Amazon S3 User Guide</i>.
         *
         * <p>In the following example, the request header sets the redirect to an object (anotherPage.html) in the same
         * bucket:
         *
         * <p><code>x-amz-website-redirect-location: /anotherPage.html</code>
         *
         * <p>In the following example, the request header sets the object redirect to another website:
         *
         * <p><code>x-amz-website-redirect-location: http://www.example.com/</code>
         *
         * <p>For more information about website hosting in Amazon S3, see <a href="https://docs.aws.amazon.com/AmazonS3/latest/dev/WebsiteHosting.html">Hosting Websites on Amazon S3</a> and <a href="https://docs.aws.amazon.com/AmazonS3/latest/dev/how-to-page-redirect.html">How
         * to Configure Website Page Redirects</a> in the <i>Amazon S3 User Guide</i>.
         *
         * <p>This functionality is not supported for directory buckets.
         *
         * @return this builder.
         */
        public Builder websiteRedirectLocation(String websiteRedirectLocation) {
            this.websiteRedirectLocation = websiteRedirectLocation;
            return this;
        }

        /**
         * Specifies the algorithm to use when encrypting the object (for example, <code>AES256</code>).
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
         * appropriate for use with the algorithm specified in the <code>x-amz-server-side-encryption-customer-algorithm</code>
         * header.
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
         * Specifies the KMS key ID (Key ID, Key ARN, or Key Alias) to use for object encryption. If the KMS key doesn't
         * exist in the same account that's issuing the command, you must use the full Key ARN not the Key ID.
         *
         * <p><b>General purpose buckets</b> - If you specify <code>x-amz-server-side-encryption</code> with <code>aws:kms</code>
         * or <code>aws:kms:dsse</code>, this header specifies the ID (Key ID, Key ARN, or Key Alias) of the KMS key to use.
         * If you specify <code>x-amz-server-side-encryption:aws:kms</code> or <code>
         * x-amz-server-side-encryption:aws:kms:dsse</code>, but do not provide <code>
         * x-amz-server-side-encryption-aws-kms-key-id</code>, Amazon S3 uses the Amazon Web Services managed key (<code>
         * aws/s3</code>) to protect the data.
         *
         * <p><b>Directory buckets</b> - To encrypt data using SSE-KMS, it's recommended to specify the <code>
         * x-amz-server-side-encryption</code> header to <code>aws:kms</code>. Then, the <code>
         * x-amz-server-side-encryption-aws-kms-key-id</code> header implicitly uses the bucket's default KMS customer
         * managed key ID. If you want to explicitly set the <code> x-amz-server-side-encryption-aws-kms-key-id</code>
         * header, it must match the bucket's default customer managed key (using key ID or ARN, not alias). Your SSE-KMS
         * configuration can only support 1 <a href="https://docs.aws.amazon.com/kms/latest/developerguide/concepts.html#customer-cmk">customer managed key</a> per directory bucket's lifetime. The <a href="https://docs.aws.amazon.com/kms/latest/developerguide/concepts.html#aws-managed-cmk">Amazon Web
         * Services managed key</a> (<code>aws/s3</code>) isn't supported.
         *
         * <p>Incorrect key specification results in an HTTP <code>400 Bad Request</code> error.
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
         * Key Management Service (KMS) keys (SSE-KMS).
         *
         * <p><b>General purpose buckets</b> - Setting this header to <code>true</code> causes Amazon S3 to use an S3 Bucket
         * Key for object encryption with SSE-KMS. Also, specifying this header with a PUT action doesn't affect
         * bucket-level settings for S3 Bucket Key.
         *
         * <p><b>Directory buckets</b> - S3 Bucket Keys are always enabled for <code>GET</code> and <code>PUT</code>
         * operations in a directory bucket and can’t be disabled. S3 Bucket Keys aren't supported, when you copy SSE-KMS
         * encrypted objects from general purpose buckets to directory buckets, from directory buckets to general purpose
         * buckets, or between directory buckets, through <a href="https://docs.aws.amazon.com/AmazonS3/latest/API/API_CopyObject.html">CopyObject</a>, <a href="https://docs.aws.amazon.com/AmazonS3/latest/API/API_UploadPartCopy.html">UploadPartCopy</a>, <a href="https://docs.aws.amazon.com/AmazonS3/latest/userguide/directory-buckets-objects-Batch-Ops">the Copy operation in
         * Batch Operations</a>, or <a href="https://docs.aws.amazon.com/AmazonS3/latest/userguide/create-import-job">the import jobs</a>. In this case, Amazon S3 makes a call to KMS every time a copy
         * request is made for a KMS-encrypted object.
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
        public Builder requestPayer(RequestPayer requestPayer) {
            this.requestPayer = requestPayer;
            return this;
        }

        /**
         * The tag-set for the object. The tag-set must be encoded as URL Query parameters. (For example, "Key1=Value1")
         *
         * <p>This functionality is not supported for directory buckets.
         *
         * @return this builder.
         */
        public Builder tagging(String tagging) {
            this.tagging = tagging;
            return this;
        }

        /**
         * The Object Lock mode that you want to apply to this object.
         *
         * <p>This functionality is not supported for directory buckets.
         *
         * @return this builder.
         */
        public Builder objectLockMode(ObjectLockMode objectLockMode) {
            this.objectLockMode = objectLockMode;
            return this;
        }

        /**
         * The date and time when you want this object's Object Lock to expire. Must be formatted as a timestamp parameter.
         *
         * <p>This functionality is not supported for directory buckets.
         *
         * @return this builder.
         */
        public Builder objectLockRetainUntilDate(Instant objectLockRetainUntilDate) {
            this.objectLockRetainUntilDate = objectLockRetainUntilDate;
            return this;
        }

        /**
         * Specifies whether a legal hold will be applied to this object. For more information about S3 Object Lock, see <a href="https://docs.aws.amazon.com/AmazonS3/latest/dev/object-lock.html">
         * Object Lock</a> in the <i>Amazon S3 User Guide</i>.
         *
         * <p>This functionality is not supported for directory buckets.
         *
         * @return this builder.
         */
        public Builder objectLockLegalHoldStatus(ObjectLockLegalHoldStatus objectLockLegalHoldStatus) {
            this.objectLockLegalHoldStatus = objectLockLegalHoldStatus;
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
        public PutObjectInput build() {
            tracker.validate();
            return new PutObjectInput(this);
        }

        @Override
        @SuppressWarnings("unchecked")
        public void setMemberValue(Schema member, Object value) {
            switch (member.memberIndex()) {
                case 0 -> bucket((String) SchemaUtils.validateSameMember($SCHEMA_BUCKET, member, value));
                case 1 -> key((String) SchemaUtils.validateSameMember($SCHEMA_KEY, member, value));
                case 2 -> acl((ObjectCannedACL) SchemaUtils.validateSameMember($SCHEMA_ACL, member, value));
                case 3 -> body((DataStream) SchemaUtils.validateSameMember($SCHEMA_BODY, member, value));
                case 4 -> cacheControl((String) SchemaUtils.validateSameMember($SCHEMA_CACHE_CONTROL, member, value));
                case 5 -> contentDisposition((String) SchemaUtils.validateSameMember($SCHEMA_CONTENT_DISPOSITION, member, value));
                case 6 -> contentEncoding((String) SchemaUtils.validateSameMember($SCHEMA_CONTENT_ENCODING, member, value));
                case 7 -> contentLanguage((String) SchemaUtils.validateSameMember($SCHEMA_CONTENT_LANGUAGE, member, value));
                case 8 -> contentLength((Long) SchemaUtils.validateSameMember($SCHEMA_CONTENT_LENGTH, member, value));
                case 9 -> contentmD5((String) SchemaUtils.validateSameMember($SCHEMA_CONTENTM_D5, member, value));
                case 10 -> contentType((String) SchemaUtils.validateSameMember($SCHEMA_CONTENT_TYPE, member, value));
                case 11 -> checksumAlgorithm((ChecksumAlgorithm) SchemaUtils.validateSameMember($SCHEMA_CHECKSUM_ALGORITHM, member, value));
                case 12 -> checksumcrC32((String) SchemaUtils.validateSameMember($SCHEMA_CHECKSUMCR_C32, member, value));
                case 13 -> checksumcrC32C((String) SchemaUtils.validateSameMember($SCHEMA_CHECKSUMCR_C32_C, member, value));
                case 14 -> checksumcrC64nvmE((String) SchemaUtils.validateSameMember($SCHEMA_CHECKSUMCR_C64NVM_E, member, value));
                case 15 -> checksumshA1((String) SchemaUtils.validateSameMember($SCHEMA_CHECKSUMSH_A1, member, value));
                case 16 -> checksumshA256((String) SchemaUtils.validateSameMember($SCHEMA_CHECKSUMSH_A256, member, value));
                case 17 -> checksumshA512((String) SchemaUtils.validateSameMember($SCHEMA_CHECKSUMSH_A512, member, value));
                case 18 -> checksummD5((String) SchemaUtils.validateSameMember($SCHEMA_CHECKSUMM_D5, member, value));
                case 19 -> checksumxxhasH64((String) SchemaUtils.validateSameMember($SCHEMA_CHECKSUMXXHAS_H64, member, value));
                case 20 -> checksumxxhasH3((String) SchemaUtils.validateSameMember($SCHEMA_CHECKSUMXXHAS_H3, member, value));
                case 21 -> checksumxxhasH128((String) SchemaUtils.validateSameMember($SCHEMA_CHECKSUMXXHAS_H128, member, value));
                case 22 -> expires((String) SchemaUtils.validateSameMember($SCHEMA_EXPIRES, member, value));
                case 23 -> ifMatch((String) SchemaUtils.validateSameMember($SCHEMA_IF_MATCH, member, value));
                case 24 -> ifNoneMatch((String) SchemaUtils.validateSameMember($SCHEMA_IF_NONE_MATCH, member, value));
                case 25 -> grantFullControl((String) SchemaUtils.validateSameMember($SCHEMA_GRANT_FULL_CONTROL, member, value));
                case 26 -> grantRead((String) SchemaUtils.validateSameMember($SCHEMA_GRANT_READ, member, value));
                case 27 -> grantReadacP((String) SchemaUtils.validateSameMember($SCHEMA_GRANT_READAC_P, member, value));
                case 28 -> grantWriteacP((String) SchemaUtils.validateSameMember($SCHEMA_GRANT_WRITEAC_P, member, value));
                case 29 -> writeOffsetBytes((Long) SchemaUtils.validateSameMember($SCHEMA_WRITE_OFFSET_BYTES, member, value));
                case 30 -> metadata((Map<String, String>) SchemaUtils.validateSameMember($SCHEMA_METADATA, member, value));
                case 31 -> serverSideEncryption((ServerSideEncryption) SchemaUtils.validateSameMember($SCHEMA_SERVER_SIDE_ENCRYPTION, member, value));
                case 32 -> storageClass((StorageClass) SchemaUtils.validateSameMember($SCHEMA_STORAGE_CLASS, member, value));
                case 33 -> websiteRedirectLocation((String) SchemaUtils.validateSameMember($SCHEMA_WEBSITE_REDIRECT_LOCATION, member, value));
                case 34 -> sseCustomerAlgorithm((String) SchemaUtils.validateSameMember($SCHEMA_SSE_CUSTOMER_ALGORITHM, member, value));
                case 35 -> sseCustomerKey((String) SchemaUtils.validateSameMember($SCHEMA_SSE_CUSTOMER_KEY, member, value));
                case 36 -> sseCustomerKeymD5((String) SchemaUtils.validateSameMember($SCHEMA_SSE_CUSTOMER_KEYM_D5, member, value));
                case 37 -> ssekmsKeyId((String) SchemaUtils.validateSameMember($SCHEMA_SSEKMS_KEY_ID, member, value));
                case 38 -> ssekmsEncryptionContext((String) SchemaUtils.validateSameMember($SCHEMA_SSEKMS_ENCRYPTION_CONTEXT, member, value));
                case 39 -> bucketKeyEnabled((Boolean) SchemaUtils.validateSameMember($SCHEMA_BUCKET_KEY_ENABLED, member, value));
                case 40 -> requestPayer((RequestPayer) SchemaUtils.validateSameMember($SCHEMA_REQUEST_PAYER, member, value));
                case 41 -> tagging((String) SchemaUtils.validateSameMember($SCHEMA_TAGGING, member, value));
                case 42 -> objectLockMode((ObjectLockMode) SchemaUtils.validateSameMember($SCHEMA_OBJECT_LOCK_MODE, member, value));
                case 43 -> objectLockRetainUntilDate((Instant) SchemaUtils.validateSameMember($SCHEMA_OBJECT_LOCK_RETAIN_UNTIL_DATE, member, value));
                case 44 -> objectLockLegalHoldStatus((ObjectLockLegalHoldStatus) SchemaUtils.validateSameMember($SCHEMA_OBJECT_LOCK_LEGAL_HOLD_STATUS, member, value));
                case 45 -> expectedBucketOwner((String) SchemaUtils.validateSameMember($SCHEMA_EXPECTED_BUCKET_OWNER, member, value));
                default -> ShapeBuilder.super.setMemberValue(member, value);
            }
        }

        @Override
        public ShapeBuilder<PutObjectInput> errorCorrection() {
            if (tracker.allSet()) {
                return this;
            }
            if (!tracker.checkMember($SCHEMA_BUCKET)) {
                bucket("");
            }
            if (!tracker.checkMember($SCHEMA_KEY)) {
                key("");
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
                    case 2 -> builder.acl(ObjectCannedACL.builder().deserializeMember(de, member).build());
                    case 3 -> builder.body(de.readDataStream(member));
                    case 4 -> builder.cacheControl(de.readString(member));
                    case 5 -> builder.contentDisposition(de.readString(member));
                    case 6 -> builder.contentEncoding(de.readString(member));
                    case 7 -> builder.contentLanguage(de.readString(member));
                    case 8 -> builder.contentLength(de.readLong(member));
                    case 9 -> builder.contentmD5(de.readString(member));
                    case 10 -> builder.contentType(de.readString(member));
                    case 11 -> builder.checksumAlgorithm(ChecksumAlgorithm.builder().deserializeMember(de, member).build());
                    case 12 -> builder.checksumcrC32(de.readString(member));
                    case 13 -> builder.checksumcrC32C(de.readString(member));
                    case 14 -> builder.checksumcrC64nvmE(de.readString(member));
                    case 15 -> builder.checksumshA1(de.readString(member));
                    case 16 -> builder.checksumshA256(de.readString(member));
                    case 17 -> builder.checksumshA512(de.readString(member));
                    case 18 -> builder.checksummD5(de.readString(member));
                    case 19 -> builder.checksumxxhasH64(de.readString(member));
                    case 20 -> builder.checksumxxhasH3(de.readString(member));
                    case 21 -> builder.checksumxxhasH128(de.readString(member));
                    case 22 -> builder.expires(de.readString(member));
                    case 23 -> builder.ifMatch(de.readString(member));
                    case 24 -> builder.ifNoneMatch(de.readString(member));
                    case 25 -> builder.grantFullControl(de.readString(member));
                    case 26 -> builder.grantRead(de.readString(member));
                    case 27 -> builder.grantReadacP(de.readString(member));
                    case 28 -> builder.grantWriteacP(de.readString(member));
                    case 29 -> builder.writeOffsetBytes(de.readLong(member));
                    case 30 -> builder.metadata(SharedSerde.deserializeMetadata(member, de));
                    case 31 -> builder.serverSideEncryption(ServerSideEncryption.builder().deserializeMember(de, member).build());
                    case 32 -> builder.storageClass(StorageClass.builder().deserializeMember(de, member).build());
                    case 33 -> builder.websiteRedirectLocation(de.readString(member));
                    case 34 -> builder.sseCustomerAlgorithm(de.readString(member));
                    case 35 -> builder.sseCustomerKey(de.readString(member));
                    case 36 -> builder.sseCustomerKeymD5(de.readString(member));
                    case 37 -> builder.ssekmsKeyId(de.readString(member));
                    case 38 -> builder.ssekmsEncryptionContext(de.readString(member));
                    case 39 -> builder.bucketKeyEnabled(de.readBoolean(member));
                    case 40 -> builder.requestPayer(RequestPayer.builder().deserializeMember(de, member).build());
                    case 41 -> builder.tagging(de.readString(member));
                    case 42 -> builder.objectLockMode(ObjectLockMode.builder().deserializeMember(de, member).build());
                    case 43 -> builder.objectLockRetainUntilDate(de.readTimestamp(member));
                    case 44 -> builder.objectLockLegalHoldStatus(ObjectLockLegalHoldStatus.builder().deserializeMember(de, member).build());
                    case 45 -> builder.expectedBucketOwner(de.readString(member));
                    default -> throw new IllegalArgumentException("Unexpected member: " + member.memberName());
                }
            }
        }
    }
}
