package software.amazon.awssdk.benchmark.smithyjava.s3.model;

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
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.utils.SmithyGenerated;

@SmithyGenerated
public final class CreateMultipartUploadInput implements SerializableStruct {

    public static final Schema $SCHEMA = Schemas1.CREATE_MULTIPART_UPLOAD_INPUT;
    private static final Schema $SCHEMA_ACL = $SCHEMA.member("ACL");
    private static final Schema $SCHEMA_BUCKET = $SCHEMA.member("Bucket");
    private static final Schema $SCHEMA_CACHE_CONTROL = $SCHEMA.member("CacheControl");
    private static final Schema $SCHEMA_CONTENT_DISPOSITION = $SCHEMA.member("ContentDisposition");
    private static final Schema $SCHEMA_CONTENT_ENCODING = $SCHEMA.member("ContentEncoding");
    private static final Schema $SCHEMA_CONTENT_LANGUAGE = $SCHEMA.member("ContentLanguage");
    private static final Schema $SCHEMA_CONTENT_TYPE = $SCHEMA.member("ContentType");
    private static final Schema $SCHEMA_EXPIRES = $SCHEMA.member("Expires");
    private static final Schema $SCHEMA_GRANT_FULL_CONTROL = $SCHEMA.member("GrantFullControl");
    private static final Schema $SCHEMA_GRANT_READ = $SCHEMA.member("GrantRead");
    private static final Schema $SCHEMA_GRANT_READAC_P = $SCHEMA.member("GrantReadACP");
    private static final Schema $SCHEMA_GRANT_WRITEAC_P = $SCHEMA.member("GrantWriteACP");
    private static final Schema $SCHEMA_KEY = $SCHEMA.member("Key");
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
    private static final Schema $SCHEMA_CHECKSUM_ALGORITHM = $SCHEMA.member("ChecksumAlgorithm");
    private static final Schema $SCHEMA_CHECKSUM_TYPE = $SCHEMA.member("ChecksumType");

    public static final ShapeId $ID = $SCHEMA.id();

    private final transient ObjectCannedACL acl;
    private final transient String bucket;
    private final transient String cacheControl;
    private final transient String contentDisposition;
    private final transient String contentEncoding;
    private final transient String contentLanguage;
    private final transient String contentType;
    private final transient String expires;
    private final transient String grantFullControl;
    private final transient String grantRead;
    private final transient String grantReadacP;
    private final transient String grantWriteacP;
    private final transient String key;
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
    private final transient ChecksumAlgorithm checksumAlgorithm;
    private final transient ChecksumType checksumType;

    private CreateMultipartUploadInput(Builder builder) {
        this.acl = builder.acl;
        this.bucket = builder.bucket;
        this.cacheControl = builder.cacheControl;
        this.contentDisposition = builder.contentDisposition;
        this.contentEncoding = builder.contentEncoding;
        this.contentLanguage = builder.contentLanguage;
        this.contentType = builder.contentType;
        this.expires = builder.expires;
        this.grantFullControl = builder.grantFullControl;
        this.grantRead = builder.grantRead;
        this.grantReadacP = builder.grantReadacP;
        this.grantWriteacP = builder.grantWriteacP;
        this.key = builder.key;
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
        this.checksumAlgorithm = builder.checksumAlgorithm;
        this.checksumType = builder.checksumType;
    }

    /**
     * The canned ACL to apply to the object. Amazon S3 supports a set of predefined ACLs, known as <i>canned ACLs</i>.
     * Each canned ACL has a predefined set of grantees and permissions. For more information, see <a href="https://docs.aws.amazon.com/AmazonS3/latest/dev/acl-overview.html#CannedACL">Canned ACL</a> in
     * the <i>Amazon S3 User Guide</i>.
     *
     * <p>By default, all objects are private. Only the owner has full access control. When uploading an object, you can
     * grant access permissions to individual Amazon Web Services accounts or to predefined groups defined by Amazon S3.
     * These permissions are then added to the access control list (ACL) on the new object. For more information, see <a href="https://docs.aws.amazon.com/AmazonS3/latest/dev/S3_ACLs_UsingACLs.html">
     * Using ACLs</a>. One way to grant the permissions using the request headers is to specify a canned ACL with the <code>
     * x-amz-acl</code> request header.
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
     * The name of the bucket where the multipart upload is initiated and where the object is uploaded.
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
     * Specifies caching behavior along the request/reply chain.
     */
    public String getCacheControl() {
        return cacheControl;
    }

    /**
     * Specifies presentational information for the object.
     */
    public String getContentDisposition() {
        return contentDisposition;
    }

    /**
     * Specifies what content encodings have been applied to the object and thus what decoding mechanisms must be
     * applied to obtain the media-type referenced by the Content-Type header field.
     *
     * <p>For directory buckets, only the <code>aws-chunked</code> value is supported in this header field.
     */
    public String getContentEncoding() {
        return contentEncoding;
    }

    /**
     * The language that the content is in.
     */
    public String getContentLanguage() {
        return contentLanguage;
    }

    /**
     * A standard MIME type describing the format of the object data.
     */
    public String getContentType() {
        return contentType;
    }

    /**
     * The date and time at which the object is no longer cacheable.
     */
    public String getExpires() {
        return expires;
    }

    /**
     * Specify access permissions explicitly to give the grantee READ, READ_ACP, and WRITE_ACP permissions on the
     * object.
     *
     * <p>By default, all objects are private. Only the owner has full access control. When uploading an object, you can
     * use this header to explicitly grant access permissions to specific Amazon Web Services accounts or groups. This
     * header maps to specific permissions that Amazon S3 supports in an ACL. For more information, see <a href="https://docs.aws.amazon.com/AmazonS3/latest/dev/acl-overview.html">Access
     * Control List (ACL) Overview</a> in the <i>Amazon S3 User Guide</i>.
     *
     * <p>You specify each grantee as a type=value pair, where the type is one of the following:
     *
     * <ul>
     *   <li>
     *     <code>id</code> – if the value specified is the canonical user ID of an Amazon Web Services account
     *   </li>
     *   <li>
     *     <code>uri</code> – if you are granting permissions to a predefined group
     *   </li>
     *   <li>
     *     <code>emailAddress</code> – if the value specified is the email address of an Amazon Web Services account
     *     Using email addresses to specify a grantee is only supported in the following Amazon Web Services
     *     Regions:
     *
     *     <ul>
     *       <li>
     *         US East (N. Virginia)
     *       </li>
     *       <li>
     *         US West (N. California)
     *       </li>
     *       <li>
     *          US West (Oregon)
     *       </li>
     *       <li>
     *          Asia Pacific (Singapore)
     *       </li>
     *       <li>
     *         Asia Pacific (Sydney)
     *       </li>
     *       <li>
     *         Asia Pacific (Tokyo)
     *       </li>
     *       <li>
     *         Europe (Ireland)
     *       </li>
     *       <li>
     *         South America (São Paulo)
     *       </li>
     *     </ul>For a list of all the Amazon S3 supported Regions and endpoints, see <a href="https://docs.aws.amazon.com/general/latest/gr/rande.html#s3_region">Regions and Endpoints</a> in
     *     the Amazon Web Services General Reference.
     *   </li>
     * </ul>
     *
     * <p>For example, the following <code>x-amz-grant-read</code> header grants the Amazon Web Services accounts
     * identified by account IDs permissions to read object data and its metadata:
     *
     * <p><code>x-amz-grant-read: id="11112222333", id="444455556666" </code>
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
     * Specify access permissions explicitly to allow grantee to read the object data and its metadata.
     *
     * <p>By default, all objects are private. Only the owner has full access control. When uploading an object, you can
     * use this header to explicitly grant access permissions to specific Amazon Web Services accounts or groups. This
     * header maps to specific permissions that Amazon S3 supports in an ACL. For more information, see <a href="https://docs.aws.amazon.com/AmazonS3/latest/dev/acl-overview.html">Access
     * Control List (ACL) Overview</a> in the <i>Amazon S3 User Guide</i>.
     *
     * <p>You specify each grantee as a type=value pair, where the type is one of the following:
     *
     * <ul>
     *   <li>
     *     <code>id</code> – if the value specified is the canonical user ID of an Amazon Web Services account
     *   </li>
     *   <li>
     *     <code>uri</code> – if you are granting permissions to a predefined group
     *   </li>
     *   <li>
     *     <code>emailAddress</code> – if the value specified is the email address of an Amazon Web Services account
     *     Using email addresses to specify a grantee is only supported in the following Amazon Web Services
     *     Regions:
     *
     *     <ul>
     *       <li>
     *         US East (N. Virginia)
     *       </li>
     *       <li>
     *         US West (N. California)
     *       </li>
     *       <li>
     *          US West (Oregon)
     *       </li>
     *       <li>
     *          Asia Pacific (Singapore)
     *       </li>
     *       <li>
     *         Asia Pacific (Sydney)
     *       </li>
     *       <li>
     *         Asia Pacific (Tokyo)
     *       </li>
     *       <li>
     *         Europe (Ireland)
     *       </li>
     *       <li>
     *         South America (São Paulo)
     *       </li>
     *     </ul>For a list of all the Amazon S3 supported Regions and endpoints, see <a href="https://docs.aws.amazon.com/general/latest/gr/rande.html#s3_region">Regions and Endpoints</a> in
     *     the Amazon Web Services General Reference.
     *   </li>
     * </ul>
     *
     * <p>For example, the following <code>x-amz-grant-read</code> header grants the Amazon Web Services accounts
     * identified by account IDs permissions to read object data and its metadata:
     *
     * <p><code>x-amz-grant-read: id="11112222333", id="444455556666" </code>
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
     * Specify access permissions explicitly to allows grantee to read the object ACL.
     *
     * <p>By default, all objects are private. Only the owner has full access control. When uploading an object, you can
     * use this header to explicitly grant access permissions to specific Amazon Web Services accounts or groups. This
     * header maps to specific permissions that Amazon S3 supports in an ACL. For more information, see <a href="https://docs.aws.amazon.com/AmazonS3/latest/dev/acl-overview.html">Access
     * Control List (ACL) Overview</a> in the <i>Amazon S3 User Guide</i>.
     *
     * <p>You specify each grantee as a type=value pair, where the type is one of the following:
     *
     * <ul>
     *   <li>
     *     <code>id</code> – if the value specified is the canonical user ID of an Amazon Web Services account
     *   </li>
     *   <li>
     *     <code>uri</code> – if you are granting permissions to a predefined group
     *   </li>
     *   <li>
     *     <code>emailAddress</code> – if the value specified is the email address of an Amazon Web Services account
     *     Using email addresses to specify a grantee is only supported in the following Amazon Web Services
     *     Regions:
     *
     *     <ul>
     *       <li>
     *         US East (N. Virginia)
     *       </li>
     *       <li>
     *         US West (N. California)
     *       </li>
     *       <li>
     *          US West (Oregon)
     *       </li>
     *       <li>
     *          Asia Pacific (Singapore)
     *       </li>
     *       <li>
     *         Asia Pacific (Sydney)
     *       </li>
     *       <li>
     *         Asia Pacific (Tokyo)
     *       </li>
     *       <li>
     *         Europe (Ireland)
     *       </li>
     *       <li>
     *         South America (São Paulo)
     *       </li>
     *     </ul>For a list of all the Amazon S3 supported Regions and endpoints, see <a href="https://docs.aws.amazon.com/general/latest/gr/rande.html#s3_region">Regions and Endpoints</a> in
     *     the Amazon Web Services General Reference.
     *   </li>
     * </ul>
     *
     * <p>For example, the following <code>x-amz-grant-read</code> header grants the Amazon Web Services accounts
     * identified by account IDs permissions to read object data and its metadata:
     *
     * <p><code>x-amz-grant-read: id="11112222333", id="444455556666" </code>
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
     * Specify access permissions explicitly to allows grantee to allow grantee to write the ACL for the applicable
     * object.
     *
     * <p>By default, all objects are private. Only the owner has full access control. When uploading an object, you can
     * use this header to explicitly grant access permissions to specific Amazon Web Services accounts or groups. This
     * header maps to specific permissions that Amazon S3 supports in an ACL. For more information, see <a href="https://docs.aws.amazon.com/AmazonS3/latest/dev/acl-overview.html">Access
     * Control List (ACL) Overview</a> in the <i>Amazon S3 User Guide</i>.
     *
     * <p>You specify each grantee as a type=value pair, where the type is one of the following:
     *
     * <ul>
     *   <li>
     *     <code>id</code> – if the value specified is the canonical user ID of an Amazon Web Services account
     *   </li>
     *   <li>
     *     <code>uri</code> – if you are granting permissions to a predefined group
     *   </li>
     *   <li>
     *     <code>emailAddress</code> – if the value specified is the email address of an Amazon Web Services account
     *     Using email addresses to specify a grantee is only supported in the following Amazon Web Services
     *     Regions:
     *
     *     <ul>
     *       <li>
     *         US East (N. Virginia)
     *       </li>
     *       <li>
     *         US West (N. California)
     *       </li>
     *       <li>
     *          US West (Oregon)
     *       </li>
     *       <li>
     *          Asia Pacific (Singapore)
     *       </li>
     *       <li>
     *         Asia Pacific (Sydney)
     *       </li>
     *       <li>
     *         Asia Pacific (Tokyo)
     *       </li>
     *       <li>
     *         Europe (Ireland)
     *       </li>
     *       <li>
     *         South America (São Paulo)
     *       </li>
     *     </ul>For a list of all the Amazon S3 supported Regions and endpoints, see <a href="https://docs.aws.amazon.com/general/latest/gr/rande.html#s3_region">Regions and Endpoints</a> in
     *     the Amazon Web Services General Reference.
     *   </li>
     * </ul>
     *
     * <p>For example, the following <code>x-amz-grant-read</code> header grants the Amazon Web Services accounts
     * identified by account IDs permissions to read object data and its metadata:
     *
     * <p><code>x-amz-grant-read: id="11112222333", id="444455556666" </code>
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
     * Object key for which the multipart upload is to be initiated.
     */
    public String getKey() {
        return key;
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
     * The server-side encryption algorithm used when you store this object in Amazon S3 or Amazon FSx.
     *
     * <ul>
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
     * or to an external URL. Amazon S3 stores the value of this header in the object metadata.
     *
     * <p>This functionality is not supported for directory buckets.
     */
    public String getWebsiteRedirectLocation() {
        return websiteRedirectLocation;
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
     * appropriate for use with the algorithm specified in the <code>x-amz-server-side-encryption-customer-algorithm</code>
     * header.
     *
     * <p>This functionality is not supported for directory buckets.
     */
    public String getSseCustomerKey() {
        return sseCustomerKey;
    }

    /**
     * Specifies the 128-bit MD5 digest of the customer-provided encryption key according to RFC 1321. Amazon S3 uses
     * this header for a message integrity check to ensure that the encryption key was transmitted without error.
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
     * Specifies the Amazon Web Services KMS Encryption Context to use for object encryption. The value of this header
     * is a Base64 encoded string of a UTF-8 encoded JSON, which contains the encryption context as key-value pairs.
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
     * The tag-set for the object. The tag-set must be encoded as URL Query parameters.
     *
     * <p>This functionality is not supported for directory buckets.
     */
    public String getTagging() {
        return tagging;
    }

    /**
     * Specifies the Object Lock mode that you want to apply to the uploaded object.
     *
     * <p>This functionality is not supported for directory buckets.
     */
    public ObjectLockMode getObjectLockMode() {
        return objectLockMode;
    }

    /**
     * Specifies the date and time when you want the Object Lock to expire.
     *
     * <p>This functionality is not supported for directory buckets.
     */
    public Instant getObjectLockRetainUntilDate() {
        return objectLockRetainUntilDate;
    }

    /**
     * Specifies whether you want to apply a legal hold to the uploaded object.
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

    /**
     * Indicates the algorithm that you want Amazon S3 to use to create the checksum for the object. For more
     * information, see <a href="https://docs.aws.amazon.com/AmazonS3/latest/userguide/checking-object-integrity.html">Checking object integrity</a> in the <i>Amazon S3 User Guide</i>.
     */
    public ChecksumAlgorithm getChecksumAlgorithm() {
        return checksumAlgorithm;
    }

    /**
     * Indicates the checksum type that you want Amazon S3 to use to calculate the object’s checksum value. For more
     * information, see <a
     * href="https://docs.aws.amazon.com/AmazonS3/latest/userguide/checking-object-integrity.html">Checking object
     * integrity in the Amazon S3 User Guide</a>.
     */
    public ChecksumType getChecksumType() {
        return checksumType;
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
        CreateMultipartUploadInput that = (CreateMultipartUploadInput) other;
        return Objects.equals(this.bucketKeyEnabled, that.bucketKeyEnabled)
               && Objects.equals(this.bucket, that.bucket)
               && Objects.equals(this.cacheControl, that.cacheControl)
               && Objects.equals(this.contentDisposition, that.contentDisposition)
               && Objects.equals(this.contentEncoding, that.contentEncoding)
               && Objects.equals(this.contentLanguage, that.contentLanguage)
               && Objects.equals(this.contentType, that.contentType)
               && Objects.equals(this.expires, that.expires)
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
               && Objects.equals(this.serverSideEncryption, that.serverSideEncryption)
               && Objects.equals(this.storageClass, that.storageClass)
               && Objects.equals(this.requestPayer, that.requestPayer)
               && Objects.equals(this.objectLockMode, that.objectLockMode)
               && Objects.equals(this.objectLockLegalHoldStatus, that.objectLockLegalHoldStatus)
               && Objects.equals(this.checksumAlgorithm, that.checksumAlgorithm)
               && Objects.equals(this.checksumType, that.checksumType)
               && Objects.equals(this.objectLockRetainUntilDate, that.objectLockRetainUntilDate)
               && Objects.equals(this.metadata, that.metadata);
    }

    @Override
    public int hashCode() {
        int $hc = Objects.hashCode(acl);
        $hc = 31 * $hc + Objects.hashCode(bucket);
        $hc = 31 * $hc + Objects.hashCode(cacheControl);
        $hc = 31 * $hc + Objects.hashCode(contentDisposition);
        $hc = 31 * $hc + Objects.hashCode(contentEncoding);
        $hc = 31 * $hc + Objects.hashCode(contentLanguage);
        $hc = 31 * $hc + Objects.hashCode(contentType);
        $hc = 31 * $hc + Objects.hashCode(expires);
        $hc = 31 * $hc + Objects.hashCode(grantFullControl);
        $hc = 31 * $hc + Objects.hashCode(grantRead);
        $hc = 31 * $hc + Objects.hashCode(grantReadacP);
        $hc = 31 * $hc + Objects.hashCode(grantWriteacP);
        $hc = 31 * $hc + Objects.hashCode(key);
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
        $hc = 31 * $hc + Objects.hashCode(checksumAlgorithm);
        $hc = 31 * $hc + Objects.hashCode(checksumType);
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
        if (contentType != null) {
            serializer.writeString($SCHEMA_CONTENT_TYPE, contentType);
        }
        if (expires != null) {
            serializer.writeString($SCHEMA_EXPIRES, expires);
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
        if (checksumAlgorithm != null) {
            serializer.writeString($SCHEMA_CHECKSUM_ALGORITHM, checksumAlgorithm.getValue());
        }
        if (checksumType != null) {
            serializer.writeString($SCHEMA_CHECKSUM_TYPE, checksumType.getValue());
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T getMemberValue(Schema member) {
        return switch (member.memberIndex()) {
            case 0 -> (T) SchemaUtils.validateSameMember($SCHEMA_BUCKET, member, bucket);
            case 1 -> (T) SchemaUtils.validateSameMember($SCHEMA_KEY, member, key);
            case 2 -> (T) SchemaUtils.validateSameMember($SCHEMA_ACL, member, acl);
            case 3 -> (T) SchemaUtils.validateSameMember($SCHEMA_CACHE_CONTROL, member, cacheControl);
            case 4 -> (T) SchemaUtils.validateSameMember($SCHEMA_CONTENT_DISPOSITION, member, contentDisposition);
            case 5 -> (T) SchemaUtils.validateSameMember($SCHEMA_CONTENT_ENCODING, member, contentEncoding);
            case 6 -> (T) SchemaUtils.validateSameMember($SCHEMA_CONTENT_LANGUAGE, member, contentLanguage);
            case 7 -> (T) SchemaUtils.validateSameMember($SCHEMA_CONTENT_TYPE, member, contentType);
            case 8 -> (T) SchemaUtils.validateSameMember($SCHEMA_EXPIRES, member, expires);
            case 9 -> (T) SchemaUtils.validateSameMember($SCHEMA_GRANT_FULL_CONTROL, member, grantFullControl);
            case 10 -> (T) SchemaUtils.validateSameMember($SCHEMA_GRANT_READ, member, grantRead);
            case 11 -> (T) SchemaUtils.validateSameMember($SCHEMA_GRANT_READAC_P, member, grantReadacP);
            case 12 -> (T) SchemaUtils.validateSameMember($SCHEMA_GRANT_WRITEAC_P, member, grantWriteacP);
            case 13 -> (T) SchemaUtils.validateSameMember($SCHEMA_METADATA, member, metadata);
            case 14 -> (T) SchemaUtils.validateSameMember($SCHEMA_SERVER_SIDE_ENCRYPTION, member, serverSideEncryption);
            case 15 -> (T) SchemaUtils.validateSameMember($SCHEMA_STORAGE_CLASS, member, storageClass);
            case 16 -> (T) SchemaUtils.validateSameMember($SCHEMA_WEBSITE_REDIRECT_LOCATION, member, websiteRedirectLocation);
            case 17 -> (T) SchemaUtils.validateSameMember($SCHEMA_SSE_CUSTOMER_ALGORITHM, member, sseCustomerAlgorithm);
            case 18 -> (T) SchemaUtils.validateSameMember($SCHEMA_SSE_CUSTOMER_KEY, member, sseCustomerKey);
            case 19 -> (T) SchemaUtils.validateSameMember($SCHEMA_SSE_CUSTOMER_KEYM_D5, member, sseCustomerKeymD5);
            case 20 -> (T) SchemaUtils.validateSameMember($SCHEMA_SSEKMS_KEY_ID, member, ssekmsKeyId);
            case 21 -> (T) SchemaUtils.validateSameMember($SCHEMA_SSEKMS_ENCRYPTION_CONTEXT, member, ssekmsEncryptionContext);
            case 22 -> (T) SchemaUtils.validateSameMember($SCHEMA_BUCKET_KEY_ENABLED, member, bucketKeyEnabled);
            case 23 -> (T) SchemaUtils.validateSameMember($SCHEMA_REQUEST_PAYER, member, requestPayer);
            case 24 -> (T) SchemaUtils.validateSameMember($SCHEMA_TAGGING, member, tagging);
            case 25 -> (T) SchemaUtils.validateSameMember($SCHEMA_OBJECT_LOCK_MODE, member, objectLockMode);
            case 26 -> (T) SchemaUtils.validateSameMember($SCHEMA_OBJECT_LOCK_RETAIN_UNTIL_DATE, member, objectLockRetainUntilDate);
            case 27 -> (T) SchemaUtils.validateSameMember($SCHEMA_OBJECT_LOCK_LEGAL_HOLD_STATUS, member, objectLockLegalHoldStatus);
            case 28 -> (T) SchemaUtils.validateSameMember($SCHEMA_EXPECTED_BUCKET_OWNER, member, expectedBucketOwner);
            case 29 -> (T) SchemaUtils.validateSameMember($SCHEMA_CHECKSUM_ALGORITHM, member, checksumAlgorithm);
            case 30 -> (T) SchemaUtils.validateSameMember($SCHEMA_CHECKSUM_TYPE, member, checksumType);
            default -> throw new IllegalArgumentException("Attempted to get non-existent member: " + member.id());
        };
    }

    /**
     * Create a new builder containing all the current property values of this object.
     *
     * <p><strong>Note:</strong> This method performs only a shallow copy of the original properties.
     *
     * @return a builder for {@link CreateMultipartUploadInput}.
     */
    public Builder toBuilder() {
        var builder = new Builder();
        builder.acl(this.acl);
        builder.bucket(this.bucket);
        builder.cacheControl(this.cacheControl);
        builder.contentDisposition(this.contentDisposition);
        builder.contentEncoding(this.contentEncoding);
        builder.contentLanguage(this.contentLanguage);
        builder.contentType(this.contentType);
        builder.expires(this.expires);
        builder.grantFullControl(this.grantFullControl);
        builder.grantRead(this.grantRead);
        builder.grantReadacP(this.grantReadacP);
        builder.grantWriteacP(this.grantWriteacP);
        builder.key(this.key);
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
        builder.checksumAlgorithm(this.checksumAlgorithm);
        builder.checksumType(this.checksumType);
        return builder;
    }

    /**
     * @return returns a new Builder.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for {@link CreateMultipartUploadInput}.
     */
    public static final class Builder implements ShapeBuilder<CreateMultipartUploadInput> {
        private final PresenceTracker tracker = PresenceTracker.of($SCHEMA);
        private ObjectCannedACL acl;
        private String bucket;
        private String cacheControl;
        private String contentDisposition;
        private String contentEncoding;
        private String contentLanguage;
        private String contentType;
        private String expires;
        private String grantFullControl;
        private String grantRead;
        private String grantReadacP;
        private String grantWriteacP;
        private String key;
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
        private ChecksumAlgorithm checksumAlgorithm;
        private ChecksumType checksumType;

        private Builder() {}

        @Override
        public Schema schema() {
            return $SCHEMA;
        }

        /**
         * The canned ACL to apply to the object. Amazon S3 supports a set of predefined ACLs, known as <i>canned ACLs</i>.
         * Each canned ACL has a predefined set of grantees and permissions. For more information, see <a href="https://docs.aws.amazon.com/AmazonS3/latest/dev/acl-overview.html#CannedACL">Canned ACL</a> in
         * the <i>Amazon S3 User Guide</i>.
         *
         * <p>By default, all objects are private. Only the owner has full access control. When uploading an object, you can
         * grant access permissions to individual Amazon Web Services accounts or to predefined groups defined by Amazon S3.
         * These permissions are then added to the access control list (ACL) on the new object. For more information, see <a href="https://docs.aws.amazon.com/AmazonS3/latest/dev/S3_ACLs_UsingACLs.html">
         * Using ACLs</a>. One way to grant the permissions using the request headers is to specify a canned ACL with the <code>
         * x-amz-acl</code> request header.
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
         * The name of the bucket where the multipart upload is initiated and where the object is uploaded.
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
         * Specifies caching behavior along the request/reply chain.
         *
         * @return this builder.
         */
        public Builder cacheControl(String cacheControl) {
            this.cacheControl = cacheControl;
            return this;
        }

        /**
         * Specifies presentational information for the object.
         *
         * @return this builder.
         */
        public Builder contentDisposition(String contentDisposition) {
            this.contentDisposition = contentDisposition;
            return this;
        }

        /**
         * Specifies what content encodings have been applied to the object and thus what decoding mechanisms must be
         * applied to obtain the media-type referenced by the Content-Type header field.
         *
         * <p>For directory buckets, only the <code>aws-chunked</code> value is supported in this header field.
         *
         * @return this builder.
         */
        public Builder contentEncoding(String contentEncoding) {
            this.contentEncoding = contentEncoding;
            return this;
        }

        /**
         * The language that the content is in.
         *
         * @return this builder.
         */
        public Builder contentLanguage(String contentLanguage) {
            this.contentLanguage = contentLanguage;
            return this;
        }

        /**
         * A standard MIME type describing the format of the object data.
         *
         * @return this builder.
         */
        public Builder contentType(String contentType) {
            this.contentType = contentType;
            return this;
        }

        /**
         * The date and time at which the object is no longer cacheable.
         *
         * @return this builder.
         */
        public Builder expires(String expires) {
            this.expires = expires;
            return this;
        }

        /**
         * Specify access permissions explicitly to give the grantee READ, READ_ACP, and WRITE_ACP permissions on the
         * object.
         *
         * <p>By default, all objects are private. Only the owner has full access control. When uploading an object, you can
         * use this header to explicitly grant access permissions to specific Amazon Web Services accounts or groups. This
         * header maps to specific permissions that Amazon S3 supports in an ACL. For more information, see <a href="https://docs.aws.amazon.com/AmazonS3/latest/dev/acl-overview.html">Access
         * Control List (ACL) Overview</a> in the <i>Amazon S3 User Guide</i>.
         *
         * <p>You specify each grantee as a type=value pair, where the type is one of the following:
         *
         * <ul>
         *   <li>
         *     <code>id</code> – if the value specified is the canonical user ID of an Amazon Web Services account
         *   </li>
         *   <li>
         *     <code>uri</code> – if you are granting permissions to a predefined group
         *   </li>
         *   <li>
         *     <code>emailAddress</code> – if the value specified is the email address of an Amazon Web Services account
         *     Using email addresses to specify a grantee is only supported in the following Amazon Web Services
         *     Regions:
         *
         *     <ul>
         *       <li>
         *         US East (N. Virginia)
         *       </li>
         *       <li>
         *         US West (N. California)
         *       </li>
         *       <li>
         *          US West (Oregon)
         *       </li>
         *       <li>
         *          Asia Pacific (Singapore)
         *       </li>
         *       <li>
         *         Asia Pacific (Sydney)
         *       </li>
         *       <li>
         *         Asia Pacific (Tokyo)
         *       </li>
         *       <li>
         *         Europe (Ireland)
         *       </li>
         *       <li>
         *         South America (São Paulo)
         *       </li>
         *     </ul>For a list of all the Amazon S3 supported Regions and endpoints, see <a href="https://docs.aws.amazon.com/general/latest/gr/rande.html#s3_region">Regions and Endpoints</a> in
         *     the Amazon Web Services General Reference.
         *   </li>
         * </ul>
         *
         * <p>For example, the following <code>x-amz-grant-read</code> header grants the Amazon Web Services accounts
         * identified by account IDs permissions to read object data and its metadata:
         *
         * <p><code>x-amz-grant-read: id="11112222333", id="444455556666" </code>
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
         * Specify access permissions explicitly to allow grantee to read the object data and its metadata.
         *
         * <p>By default, all objects are private. Only the owner has full access control. When uploading an object, you can
         * use this header to explicitly grant access permissions to specific Amazon Web Services accounts or groups. This
         * header maps to specific permissions that Amazon S3 supports in an ACL. For more information, see <a href="https://docs.aws.amazon.com/AmazonS3/latest/dev/acl-overview.html">Access
         * Control List (ACL) Overview</a> in the <i>Amazon S3 User Guide</i>.
         *
         * <p>You specify each grantee as a type=value pair, where the type is one of the following:
         *
         * <ul>
         *   <li>
         *     <code>id</code> – if the value specified is the canonical user ID of an Amazon Web Services account
         *   </li>
         *   <li>
         *     <code>uri</code> – if you are granting permissions to a predefined group
         *   </li>
         *   <li>
         *     <code>emailAddress</code> – if the value specified is the email address of an Amazon Web Services account
         *     Using email addresses to specify a grantee is only supported in the following Amazon Web Services
         *     Regions:
         *
         *     <ul>
         *       <li>
         *         US East (N. Virginia)
         *       </li>
         *       <li>
         *         US West (N. California)
         *       </li>
         *       <li>
         *          US West (Oregon)
         *       </li>
         *       <li>
         *          Asia Pacific (Singapore)
         *       </li>
         *       <li>
         *         Asia Pacific (Sydney)
         *       </li>
         *       <li>
         *         Asia Pacific (Tokyo)
         *       </li>
         *       <li>
         *         Europe (Ireland)
         *       </li>
         *       <li>
         *         South America (São Paulo)
         *       </li>
         *     </ul>For a list of all the Amazon S3 supported Regions and endpoints, see <a href="https://docs.aws.amazon.com/general/latest/gr/rande.html#s3_region">Regions and Endpoints</a> in
         *     the Amazon Web Services General Reference.
         *   </li>
         * </ul>
         *
         * <p>For example, the following <code>x-amz-grant-read</code> header grants the Amazon Web Services accounts
         * identified by account IDs permissions to read object data and its metadata:
         *
         * <p><code>x-amz-grant-read: id="11112222333", id="444455556666" </code>
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
         * Specify access permissions explicitly to allows grantee to read the object ACL.
         *
         * <p>By default, all objects are private. Only the owner has full access control. When uploading an object, you can
         * use this header to explicitly grant access permissions to specific Amazon Web Services accounts or groups. This
         * header maps to specific permissions that Amazon S3 supports in an ACL. For more information, see <a href="https://docs.aws.amazon.com/AmazonS3/latest/dev/acl-overview.html">Access
         * Control List (ACL) Overview</a> in the <i>Amazon S3 User Guide</i>.
         *
         * <p>You specify each grantee as a type=value pair, where the type is one of the following:
         *
         * <ul>
         *   <li>
         *     <code>id</code> – if the value specified is the canonical user ID of an Amazon Web Services account
         *   </li>
         *   <li>
         *     <code>uri</code> – if you are granting permissions to a predefined group
         *   </li>
         *   <li>
         *     <code>emailAddress</code> – if the value specified is the email address of an Amazon Web Services account
         *     Using email addresses to specify a grantee is only supported in the following Amazon Web Services
         *     Regions:
         *
         *     <ul>
         *       <li>
         *         US East (N. Virginia)
         *       </li>
         *       <li>
         *         US West (N. California)
         *       </li>
         *       <li>
         *          US West (Oregon)
         *       </li>
         *       <li>
         *          Asia Pacific (Singapore)
         *       </li>
         *       <li>
         *         Asia Pacific (Sydney)
         *       </li>
         *       <li>
         *         Asia Pacific (Tokyo)
         *       </li>
         *       <li>
         *         Europe (Ireland)
         *       </li>
         *       <li>
         *         South America (São Paulo)
         *       </li>
         *     </ul>For a list of all the Amazon S3 supported Regions and endpoints, see <a href="https://docs.aws.amazon.com/general/latest/gr/rande.html#s3_region">Regions and Endpoints</a> in
         *     the Amazon Web Services General Reference.
         *   </li>
         * </ul>
         *
         * <p>For example, the following <code>x-amz-grant-read</code> header grants the Amazon Web Services accounts
         * identified by account IDs permissions to read object data and its metadata:
         *
         * <p><code>x-amz-grant-read: id="11112222333", id="444455556666" </code>
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
         * Specify access permissions explicitly to allows grantee to allow grantee to write the ACL for the applicable
         * object.
         *
         * <p>By default, all objects are private. Only the owner has full access control. When uploading an object, you can
         * use this header to explicitly grant access permissions to specific Amazon Web Services accounts or groups. This
         * header maps to specific permissions that Amazon S3 supports in an ACL. For more information, see <a href="https://docs.aws.amazon.com/AmazonS3/latest/dev/acl-overview.html">Access
         * Control List (ACL) Overview</a> in the <i>Amazon S3 User Guide</i>.
         *
         * <p>You specify each grantee as a type=value pair, where the type is one of the following:
         *
         * <ul>
         *   <li>
         *     <code>id</code> – if the value specified is the canonical user ID of an Amazon Web Services account
         *   </li>
         *   <li>
         *     <code>uri</code> – if you are granting permissions to a predefined group
         *   </li>
         *   <li>
         *     <code>emailAddress</code> – if the value specified is the email address of an Amazon Web Services account
         *     Using email addresses to specify a grantee is only supported in the following Amazon Web Services
         *     Regions:
         *
         *     <ul>
         *       <li>
         *         US East (N. Virginia)
         *       </li>
         *       <li>
         *         US West (N. California)
         *       </li>
         *       <li>
         *          US West (Oregon)
         *       </li>
         *       <li>
         *          Asia Pacific (Singapore)
         *       </li>
         *       <li>
         *         Asia Pacific (Sydney)
         *       </li>
         *       <li>
         *         Asia Pacific (Tokyo)
         *       </li>
         *       <li>
         *         Europe (Ireland)
         *       </li>
         *       <li>
         *         South America (São Paulo)
         *       </li>
         *     </ul>For a list of all the Amazon S3 supported Regions and endpoints, see <a href="https://docs.aws.amazon.com/general/latest/gr/rande.html#s3_region">Regions and Endpoints</a> in
         *     the Amazon Web Services General Reference.
         *   </li>
         * </ul>
         *
         * <p>For example, the following <code>x-amz-grant-read</code> header grants the Amazon Web Services accounts
         * identified by account IDs permissions to read object data and its metadata:
         *
         * <p><code>x-amz-grant-read: id="11112222333", id="444455556666" </code>
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
         * Object key for which the multipart upload is to be initiated.
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
         * A map of metadata to store with the object in S3.
         *
         * @return this builder.
         */
        public Builder metadata(Map<String, String> metadata) {
            this.metadata = metadata;
            return this;
        }

        /**
         * The server-side encryption algorithm used when you store this object in Amazon S3 or Amazon FSx.
         *
         * <ul>
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
         * or to an external URL. Amazon S3 stores the value of this header in the object metadata.
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
         * Specifies the 128-bit MD5 digest of the customer-provided encryption key according to RFC 1321. Amazon S3 uses
         * this header for a message integrity check to ensure that the encryption key was transmitted without error.
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
         * Specifies the Amazon Web Services KMS Encryption Context to use for object encryption. The value of this header
         * is a Base64 encoded string of a UTF-8 encoded JSON, which contains the encryption context as key-value pairs.
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
         * The tag-set for the object. The tag-set must be encoded as URL Query parameters.
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
         * Specifies the Object Lock mode that you want to apply to the uploaded object.
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
         * Specifies the date and time when you want the Object Lock to expire.
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
         * Specifies whether you want to apply a legal hold to the uploaded object.
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

        /**
         * Indicates the algorithm that you want Amazon S3 to use to create the checksum for the object. For more
         * information, see <a href="https://docs.aws.amazon.com/AmazonS3/latest/userguide/checking-object-integrity.html">Checking object integrity</a> in the <i>Amazon S3 User Guide</i>.
         *
         * @return this builder.
         */
        public Builder checksumAlgorithm(ChecksumAlgorithm checksumAlgorithm) {
            this.checksumAlgorithm = checksumAlgorithm;
            return this;
        }

        /**
         * Indicates the checksum type that you want Amazon S3 to use to calculate the object’s checksum value. For more
         * information, see <a
         * href="https://docs.aws.amazon.com/AmazonS3/latest/userguide/checking-object-integrity.html">Checking object
         * integrity in the Amazon S3 User Guide</a>.
         *
         * @return this builder.
         */
        public Builder checksumType(ChecksumType checksumType) {
            this.checksumType = checksumType;
            return this;
        }

        @Override
        public CreateMultipartUploadInput build() {
            tracker.validate();
            return new CreateMultipartUploadInput(this);
        }

        @Override
        @SuppressWarnings("unchecked")
        public void setMemberValue(Schema member, Object value) {
            switch (member.memberIndex()) {
                case 0 -> bucket((String) SchemaUtils.validateSameMember($SCHEMA_BUCKET, member, value));
                case 1 -> key((String) SchemaUtils.validateSameMember($SCHEMA_KEY, member, value));
                case 2 -> acl((ObjectCannedACL) SchemaUtils.validateSameMember($SCHEMA_ACL, member, value));
                case 3 -> cacheControl((String) SchemaUtils.validateSameMember($SCHEMA_CACHE_CONTROL, member, value));
                case 4 -> contentDisposition((String) SchemaUtils.validateSameMember($SCHEMA_CONTENT_DISPOSITION, member, value));
                case 5 -> contentEncoding((String) SchemaUtils.validateSameMember($SCHEMA_CONTENT_ENCODING, member, value));
                case 6 -> contentLanguage((String) SchemaUtils.validateSameMember($SCHEMA_CONTENT_LANGUAGE, member, value));
                case 7 -> contentType((String) SchemaUtils.validateSameMember($SCHEMA_CONTENT_TYPE, member, value));
                case 8 -> expires((String) SchemaUtils.validateSameMember($SCHEMA_EXPIRES, member, value));
                case 9 -> grantFullControl((String) SchemaUtils.validateSameMember($SCHEMA_GRANT_FULL_CONTROL, member, value));
                case 10 -> grantRead((String) SchemaUtils.validateSameMember($SCHEMA_GRANT_READ, member, value));
                case 11 -> grantReadacP((String) SchemaUtils.validateSameMember($SCHEMA_GRANT_READAC_P, member, value));
                case 12 -> grantWriteacP((String) SchemaUtils.validateSameMember($SCHEMA_GRANT_WRITEAC_P, member, value));
                case 13 -> metadata((Map<String, String>) SchemaUtils.validateSameMember($SCHEMA_METADATA, member, value));
                case 14 -> serverSideEncryption((ServerSideEncryption) SchemaUtils.validateSameMember($SCHEMA_SERVER_SIDE_ENCRYPTION, member, value));
                case 15 -> storageClass((StorageClass) SchemaUtils.validateSameMember($SCHEMA_STORAGE_CLASS, member, value));
                case 16 -> websiteRedirectLocation((String) SchemaUtils.validateSameMember($SCHEMA_WEBSITE_REDIRECT_LOCATION, member, value));
                case 17 -> sseCustomerAlgorithm((String) SchemaUtils.validateSameMember($SCHEMA_SSE_CUSTOMER_ALGORITHM, member, value));
                case 18 -> sseCustomerKey((String) SchemaUtils.validateSameMember($SCHEMA_SSE_CUSTOMER_KEY, member, value));
                case 19 -> sseCustomerKeymD5((String) SchemaUtils.validateSameMember($SCHEMA_SSE_CUSTOMER_KEYM_D5, member, value));
                case 20 -> ssekmsKeyId((String) SchemaUtils.validateSameMember($SCHEMA_SSEKMS_KEY_ID, member, value));
                case 21 -> ssekmsEncryptionContext((String) SchemaUtils.validateSameMember($SCHEMA_SSEKMS_ENCRYPTION_CONTEXT, member, value));
                case 22 -> bucketKeyEnabled((Boolean) SchemaUtils.validateSameMember($SCHEMA_BUCKET_KEY_ENABLED, member, value));
                case 23 -> requestPayer((RequestPayer) SchemaUtils.validateSameMember($SCHEMA_REQUEST_PAYER, member, value));
                case 24 -> tagging((String) SchemaUtils.validateSameMember($SCHEMA_TAGGING, member, value));
                case 25 -> objectLockMode((ObjectLockMode) SchemaUtils.validateSameMember($SCHEMA_OBJECT_LOCK_MODE, member, value));
                case 26 -> objectLockRetainUntilDate((Instant) SchemaUtils.validateSameMember($SCHEMA_OBJECT_LOCK_RETAIN_UNTIL_DATE, member, value));
                case 27 -> objectLockLegalHoldStatus((ObjectLockLegalHoldStatus) SchemaUtils.validateSameMember($SCHEMA_OBJECT_LOCK_LEGAL_HOLD_STATUS, member, value));
                case 28 -> expectedBucketOwner((String) SchemaUtils.validateSameMember($SCHEMA_EXPECTED_BUCKET_OWNER, member, value));
                case 29 -> checksumAlgorithm((ChecksumAlgorithm) SchemaUtils.validateSameMember($SCHEMA_CHECKSUM_ALGORITHM, member, value));
                case 30 -> checksumType((ChecksumType) SchemaUtils.validateSameMember($SCHEMA_CHECKSUM_TYPE, member, value));
                default -> ShapeBuilder.super.setMemberValue(member, value);
            }
        }

        @Override
        public ShapeBuilder<CreateMultipartUploadInput> errorCorrection() {
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
                    case 3 -> builder.cacheControl(de.readString(member));
                    case 4 -> builder.contentDisposition(de.readString(member));
                    case 5 -> builder.contentEncoding(de.readString(member));
                    case 6 -> builder.contentLanguage(de.readString(member));
                    case 7 -> builder.contentType(de.readString(member));
                    case 8 -> builder.expires(de.readString(member));
                    case 9 -> builder.grantFullControl(de.readString(member));
                    case 10 -> builder.grantRead(de.readString(member));
                    case 11 -> builder.grantReadacP(de.readString(member));
                    case 12 -> builder.grantWriteacP(de.readString(member));
                    case 13 -> builder.metadata(SharedSerde.deserializeMetadata(member, de));
                    case 14 -> builder.serverSideEncryption(ServerSideEncryption.builder().deserializeMember(de, member).build());
                    case 15 -> builder.storageClass(StorageClass.builder().deserializeMember(de, member).build());
                    case 16 -> builder.websiteRedirectLocation(de.readString(member));
                    case 17 -> builder.sseCustomerAlgorithm(de.readString(member));
                    case 18 -> builder.sseCustomerKey(de.readString(member));
                    case 19 -> builder.sseCustomerKeymD5(de.readString(member));
                    case 20 -> builder.ssekmsKeyId(de.readString(member));
                    case 21 -> builder.ssekmsEncryptionContext(de.readString(member));
                    case 22 -> builder.bucketKeyEnabled(de.readBoolean(member));
                    case 23 -> builder.requestPayer(RequestPayer.builder().deserializeMember(de, member).build());
                    case 24 -> builder.tagging(de.readString(member));
                    case 25 -> builder.objectLockMode(ObjectLockMode.builder().deserializeMember(de, member).build());
                    case 26 -> builder.objectLockRetainUntilDate(de.readTimestamp(member));
                    case 27 -> builder.objectLockLegalHoldStatus(ObjectLockLegalHoldStatus.builder().deserializeMember(de, member).build());
                    case 28 -> builder.expectedBucketOwner(de.readString(member));
                    case 29 -> builder.checksumAlgorithm(ChecksumAlgorithm.builder().deserializeMember(de, member).build());
                    case 30 -> builder.checksumType(ChecksumType.builder().deserializeMember(de, member).build());
                    default -> throw new IllegalArgumentException("Unexpected member: " + member.memberName());
                }
            }
        }
    }
}
