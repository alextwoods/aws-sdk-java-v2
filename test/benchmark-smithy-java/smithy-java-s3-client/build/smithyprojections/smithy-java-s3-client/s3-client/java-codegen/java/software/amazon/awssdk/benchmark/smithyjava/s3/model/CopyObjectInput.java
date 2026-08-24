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
public final class CopyObjectInput implements SerializableStruct {

    public static final Schema $SCHEMA = Schemas.COPY_OBJECT_INPUT;
    private static final Schema $SCHEMA_ACL = $SCHEMA.member("ACL");
    private static final Schema $SCHEMA_BUCKET = $SCHEMA.member("Bucket");
    private static final Schema $SCHEMA_CACHE_CONTROL = $SCHEMA.member("CacheControl");
    private static final Schema $SCHEMA_CHECKSUM_ALGORITHM = $SCHEMA.member("ChecksumAlgorithm");
    private static final Schema $SCHEMA_CONTENT_DISPOSITION = $SCHEMA.member("ContentDisposition");
    private static final Schema $SCHEMA_CONTENT_ENCODING = $SCHEMA.member("ContentEncoding");
    private static final Schema $SCHEMA_CONTENT_LANGUAGE = $SCHEMA.member("ContentLanguage");
    private static final Schema $SCHEMA_CONTENT_TYPE = $SCHEMA.member("ContentType");
    private static final Schema $SCHEMA_COPY_SOURCE = $SCHEMA.member("CopySource");
    private static final Schema $SCHEMA_COPY_SOURCE_IF_MATCH = $SCHEMA.member("CopySourceIfMatch");
    private static final Schema $SCHEMA_COPY_SOURCE_IF_MODIFIED_SINCE = $SCHEMA.member("CopySourceIfModifiedSince");
    private static final Schema $SCHEMA_COPY_SOURCE_IF_NONE_MATCH = $SCHEMA.member("CopySourceIfNoneMatch");
    private static final Schema $SCHEMA_COPY_SOURCE_IF_UNMODIFIED_SINCE = $SCHEMA.member("CopySourceIfUnmodifiedSince");
    private static final Schema $SCHEMA_EXPIRES = $SCHEMA.member("Expires");
    private static final Schema $SCHEMA_GRANT_FULL_CONTROL = $SCHEMA.member("GrantFullControl");
    private static final Schema $SCHEMA_GRANT_READ = $SCHEMA.member("GrantRead");
    private static final Schema $SCHEMA_GRANT_READAC_P = $SCHEMA.member("GrantReadACP");
    private static final Schema $SCHEMA_GRANT_WRITEAC_P = $SCHEMA.member("GrantWriteACP");
    private static final Schema $SCHEMA_IF_MATCH = $SCHEMA.member("IfMatch");
    private static final Schema $SCHEMA_IF_NONE_MATCH = $SCHEMA.member("IfNoneMatch");
    private static final Schema $SCHEMA_KEY = $SCHEMA.member("Key");
    private static final Schema $SCHEMA_METADATA = $SCHEMA.member("Metadata");
    private static final Schema $SCHEMA_METADATA_DIRECTIVE = $SCHEMA.member("MetadataDirective");
    private static final Schema $SCHEMA_TAGGING_DIRECTIVE = $SCHEMA.member("TaggingDirective");
    private static final Schema $SCHEMA_ANNOTATION_DIRECTIVE = $SCHEMA.member("AnnotationDirective");
    private static final Schema $SCHEMA_SERVER_SIDE_ENCRYPTION = $SCHEMA.member("ServerSideEncryption");
    private static final Schema $SCHEMA_STORAGE_CLASS = $SCHEMA.member("StorageClass");
    private static final Schema $SCHEMA_WEBSITE_REDIRECT_LOCATION = $SCHEMA.member("WebsiteRedirectLocation");
    private static final Schema $SCHEMA_SSE_CUSTOMER_ALGORITHM = $SCHEMA.member("SSECustomerAlgorithm");
    private static final Schema $SCHEMA_SSE_CUSTOMER_KEY = $SCHEMA.member("SSECustomerKey");
    private static final Schema $SCHEMA_SSE_CUSTOMER_KEYM_D5 = $SCHEMA.member("SSECustomerKeyMD5");
    private static final Schema $SCHEMA_SSEKMS_KEY_ID = $SCHEMA.member("SSEKMSKeyId");
    private static final Schema $SCHEMA_SSEKMS_ENCRYPTION_CONTEXT = $SCHEMA.member("SSEKMSEncryptionContext");
    private static final Schema $SCHEMA_BUCKET_KEY_ENABLED = $SCHEMA.member("BucketKeyEnabled");
    private static final Schema $SCHEMA_COPY_SOURCESSE_CUSTOMER_ALGORITHM = $SCHEMA.member("CopySourceSSECustomerAlgorithm");
    private static final Schema $SCHEMA_COPY_SOURCESSE_CUSTOMER_KEY = $SCHEMA.member("CopySourceSSECustomerKey");
    private static final Schema $SCHEMA_COPY_SOURCESSE_CUSTOMER_KEYM_D5 = $SCHEMA.member("CopySourceSSECustomerKeyMD5");
    private static final Schema $SCHEMA_REQUEST_PAYER = $SCHEMA.member("RequestPayer");
    private static final Schema $SCHEMA_TAGGING = $SCHEMA.member("Tagging");
    private static final Schema $SCHEMA_OBJECT_LOCK_MODE = $SCHEMA.member("ObjectLockMode");
    private static final Schema $SCHEMA_OBJECT_LOCK_RETAIN_UNTIL_DATE = $SCHEMA.member("ObjectLockRetainUntilDate");
    private static final Schema $SCHEMA_OBJECT_LOCK_LEGAL_HOLD_STATUS = $SCHEMA.member("ObjectLockLegalHoldStatus");
    private static final Schema $SCHEMA_EXPECTED_BUCKET_OWNER = $SCHEMA.member("ExpectedBucketOwner");
    private static final Schema $SCHEMA_EXPECTED_SOURCE_BUCKET_OWNER = $SCHEMA.member("ExpectedSourceBucketOwner");

    public static final ShapeId $ID = $SCHEMA.id();

    private final transient ObjectCannedACL acl;
    private final transient String bucket;
    private final transient String cacheControl;
    private final transient ChecksumAlgorithm checksumAlgorithm;
    private final transient String contentDisposition;
    private final transient String contentEncoding;
    private final transient String contentLanguage;
    private final transient String contentType;
    private final transient String copySource;
    private final transient String copySourceIfMatch;
    private final transient Instant copySourceIfModifiedSince;
    private final transient String copySourceIfNoneMatch;
    private final transient Instant copySourceIfUnmodifiedSince;
    private final transient String expires;
    private final transient String grantFullControl;
    private final transient String grantRead;
    private final transient String grantReadacP;
    private final transient String grantWriteacP;
    private final transient String ifMatch;
    private final transient String ifNoneMatch;
    private final transient String key;
    private final transient Map<String, String> metadata;
    private final transient MetadataDirective metadataDirective;
    private final transient TaggingDirective taggingDirective;
    private final transient AnnotationDirective annotationDirective;
    private final transient ServerSideEncryption serverSideEncryption;
    private final transient StorageClass storageClass;
    private final transient String websiteRedirectLocation;
    private final transient String sseCustomerAlgorithm;
    private final transient String sseCustomerKey;
    private final transient String sseCustomerKeymD5;
    private final transient String ssekmsKeyId;
    private final transient String ssekmsEncryptionContext;
    private final transient Boolean bucketKeyEnabled;
    private final transient String copySourcesseCustomerAlgorithm;
    private final transient String copySourcesseCustomerKey;
    private final transient String copySourcesseCustomerKeymD5;
    private final transient RequestPayer requestPayer;
    private final transient String tagging;
    private final transient ObjectLockMode objectLockMode;
    private final transient Instant objectLockRetainUntilDate;
    private final transient ObjectLockLegalHoldStatus objectLockLegalHoldStatus;
    private final transient String expectedBucketOwner;
    private final transient String expectedSourceBucketOwner;

    private CopyObjectInput(Builder builder) {
        this.acl = builder.acl;
        this.bucket = builder.bucket;
        this.cacheControl = builder.cacheControl;
        this.checksumAlgorithm = builder.checksumAlgorithm;
        this.contentDisposition = builder.contentDisposition;
        this.contentEncoding = builder.contentEncoding;
        this.contentLanguage = builder.contentLanguage;
        this.contentType = builder.contentType;
        this.copySource = builder.copySource;
        this.copySourceIfMatch = builder.copySourceIfMatch;
        this.copySourceIfModifiedSince = builder.copySourceIfModifiedSince;
        this.copySourceIfNoneMatch = builder.copySourceIfNoneMatch;
        this.copySourceIfUnmodifiedSince = builder.copySourceIfUnmodifiedSince;
        this.expires = builder.expires;
        this.grantFullControl = builder.grantFullControl;
        this.grantRead = builder.grantRead;
        this.grantReadacP = builder.grantReadacP;
        this.grantWriteacP = builder.grantWriteacP;
        this.ifMatch = builder.ifMatch;
        this.ifNoneMatch = builder.ifNoneMatch;
        this.key = builder.key;
        this.metadata = builder.metadata == null ? null : Collections.unmodifiableMap(builder.metadata);
        this.metadataDirective = builder.metadataDirective;
        this.taggingDirective = builder.taggingDirective;
        this.annotationDirective = builder.annotationDirective;
        this.serverSideEncryption = builder.serverSideEncryption;
        this.storageClass = builder.storageClass;
        this.websiteRedirectLocation = builder.websiteRedirectLocation;
        this.sseCustomerAlgorithm = builder.sseCustomerAlgorithm;
        this.sseCustomerKey = builder.sseCustomerKey;
        this.sseCustomerKeymD5 = builder.sseCustomerKeymD5;
        this.ssekmsKeyId = builder.ssekmsKeyId;
        this.ssekmsEncryptionContext = builder.ssekmsEncryptionContext;
        this.bucketKeyEnabled = builder.bucketKeyEnabled;
        this.copySourcesseCustomerAlgorithm = builder.copySourcesseCustomerAlgorithm;
        this.copySourcesseCustomerKey = builder.copySourcesseCustomerKey;
        this.copySourcesseCustomerKeymD5 = builder.copySourcesseCustomerKeymD5;
        this.requestPayer = builder.requestPayer;
        this.tagging = builder.tagging;
        this.objectLockMode = builder.objectLockMode;
        this.objectLockRetainUntilDate = builder.objectLockRetainUntilDate;
        this.objectLockLegalHoldStatus = builder.objectLockLegalHoldStatus;
        this.expectedBucketOwner = builder.expectedBucketOwner;
        this.expectedSourceBucketOwner = builder.expectedSourceBucketOwner;
    }

    /**
     * The canned access control list (ACL) to apply to the object.
     *
     * <p>When you copy an object, the ACL metadata is not preserved and is set to <code>private</code> by default. Only
     * the owner has full access control. To override the default ACL setting, specify a new ACL when you generate a
     * copy request. For more information, see <a href="https://docs.aws.amazon.com/AmazonS3/latest/dev/S3_ACLs_UsingACLs.html">Using ACLs</a>.
     *
     * <p>If the destination bucket that you're copying objects to uses the bucket owner enforced setting for S3 Object
     * Ownership, ACLs are disabled and no longer affect permissions. Buckets that use this setting only accept <code>
     * PUT</code> requests that don't specify an ACL or <code>PUT</code> requests that specify bucket owner full control
     * ACLs, such as the <code>bucket-owner-full-control</code> canned ACL or an equivalent form of this ACL expressed
     * in the XML format. For more information, see <a href="https://docs.aws.amazon.com/AmazonS3/latest/userguide/about-object-ownership.html">Controlling ownership of objects and disabling ACLs</a> in the <i>
     * Amazon S3 User Guide</i>.
     *
     * <ul>
     *   <li>
     *     If your destination bucket uses the bucket owner enforced setting for Object Ownership, all objects
     *     written to the bucket by any account will be owned by the bucket owner.
     *   </li>
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
     * The name of the destination bucket.
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
     * <p><b>S3 on Outposts</b> - When you use this action with S3 on Outposts, you must use the Outpost bucket access
     * point ARN or the access point alias for the destination bucket. You can only copy objects within the same Outpost
     * bucket. It's not supported to copy objects across different Amazon Web Services Outposts, between buckets on the
     * same Outposts, or between Outposts buckets and any other bucket types. For more information about S3 on Outposts,
     * see <a href="https://docs.aws.amazon.com/AmazonS3/latest/userguide/S3onOutposts.html">What is S3 on Outposts?</a> in the <i>S3 on Outposts guide</i>. When you use this action with S3 on
     * Outposts through the REST API, you must direct requests to the S3 on Outposts hostname, in the format <code><i>
     * AccessPointName</i>-<i>AccountId</i>.<i>outpostID</i>.s3-outposts.<i>Region</i>.amazonaws.com</code>. The
     * hostname isn't required when you use the Amazon Web Services CLI or SDKs.
     */
    public String getBucket() {
        return bucket;
    }

    /**
     * Specifies the caching behavior along the request/reply chain.
     */
    public String getCacheControl() {
        return cacheControl;
    }

    /**
     * Indicates the algorithm that you want Amazon S3 to use to create the checksum for the object. For more
     * information, see <a href="https://docs.aws.amazon.com/AmazonS3/latest/userguide/checking-object-integrity.html">Checking object integrity</a> in the <i>Amazon S3 User Guide</i>.
     *
     * <p>When you copy an object, if the source object has a checksum, that checksum value will be copied to the new
     * object by default. If the <code>CopyObject</code> request does not include this <code>x-amz-checksum-algorithm</code>
     * header, the checksum algorithm will be copied from the source object to the destination object (if it's present
     * on the source object). You can optionally specify a different checksum algorithm to use with the <code>
     * x-amz-checksum-algorithm</code> header. Unrecognized or unsupported values will respond with the HTTP status code
     * <code>400 Bad Request</code>.
     *
     * <p>For directory buckets, when you use Amazon Web Services SDKs, <code>CRC32</code> is the default checksum
     * algorithm that's used for performance.
     */
    public ChecksumAlgorithm getChecksumAlgorithm() {
        return checksumAlgorithm;
    }

    /**
     * Specifies presentational information for the object. Indicates whether an object should be displayed in a web
     * browser or downloaded as a file. It allows specifying the desired filename for the downloaded file.
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
     * The language the content is in.
     */
    public String getContentLanguage() {
        return contentLanguage;
    }

    /**
     * A standard MIME type that describes the format of the object data.
     */
    public String getContentType() {
        return contentType;
    }

    /**
     * Specifies the source object for the copy operation. The source object can be up to 5 GB. If the source object is
     * an object that was uploaded by using a multipart upload, the object copy will be a single part object after the
     * source object is copied to the destination bucket.
     *
     * <p>You specify the value of the copy source in one of two formats, depending on whether you want to access the
     * source object through an <a href="https://docs.aws.amazon.com/AmazonS3/latest/userguide/access-points.html">access point</a>:
     *
     * <ul>
     *   <li>
     *     For objects not accessed through an access point, specify the name of the source bucket and the key of
     *     the source object, separated by a slash (/). For example, to copy the object <code>reports/january.pdf</code>
     *     from the general purpose bucket <code>awsexamplebucket</code>, use <code>
     *     awsexamplebucket/reports/january.pdf</code>. The value must be URL-encoded. To copy the object <code>
     *     reports/january.pdf</code> from the directory bucket <code>awsexamplebucket--use1-az5--x-s3</code>, use <code>
     *     awsexamplebucket--use1-az5--x-s3/reports/january.pdf</code>. The value must be URL-encoded.
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
     * <p>If your source bucket versioning is enabled, the <code>x-amz-copy-source</code> header by default identifies
     * the current version of an object to copy. If the current version is a delete marker, Amazon S3 behaves as if the
     * object was deleted. To copy a different version, use the <code>versionId</code> query parameter. Specifically,
     * append <code>?versionId=</code> to the value (for example, <code>
     * awsexamplebucket/reports/january.pdf?versionId=QUpfdndhfd8438MNFDN93jdnJFkdmqnh893</code>). If you don't specify
     * a version ID, Amazon S3 copies the latest version of the source object.
     *
     * <p>If you enable versioning on the destination bucket, Amazon S3 generates a unique version ID for the copied
     * object. This version ID is different from the version ID of the source object. Amazon S3 returns the version ID
     * of the copied object in the <code>x-amz-version-id</code> response header in the response.
     *
     * <p>If you do not enable versioning or suspend it on the destination bucket, the version ID that Amazon S3
     * generates in the <code>x-amz-version-id</code> response header is always null.
     *
     * <p><b>Directory buckets</b> - S3 Versioning isn't enabled and supported for directory buckets.
     */
    public String getCopySource() {
        return copySource;
    }

    /**
     * Copies the object if its entity tag (ETag) matches the specified tag.
     *
     * <p> If both the <code>x-amz-copy-source-if-match</code> and <code>x-amz-copy-source-if-unmodified-since</code>
     * headers are present in the request and evaluate as follows, Amazon S3 returns <code>200 OK</code> and copies the
     * data:
     *
     * <ul>
     *   <li>
     *     <code>x-amz-copy-source-if-match</code> condition evaluates to true
     *   </li>
     *   <li>
     *     <code>x-amz-copy-source-if-unmodified-since</code> condition evaluates to false
     *   </li>
     * </ul>
     */
    public String getCopySourceIfMatch() {
        return copySourceIfMatch;
    }

    /**
     * Copies the object if it has been modified since the specified time.
     *
     * <p>If both the <code>x-amz-copy-source-if-none-match</code> and <code>x-amz-copy-source-if-modified-since</code>
     * headers are present in the request and evaluate as follows, Amazon S3 returns the <code>412 Precondition Failed</code>
     * response code:
     *
     * <ul>
     *   <li>
     *     <code>x-amz-copy-source-if-none-match</code> condition evaluates to false
     *   </li>
     *   <li>
     *     <code>x-amz-copy-source-if-modified-since</code> condition evaluates to true
     *   </li>
     * </ul>
     */
    public Instant getCopySourceIfModifiedSince() {
        return copySourceIfModifiedSince;
    }

    /**
     * Copies the object if its entity tag (ETag) is different than the specified ETag.
     *
     * <p>If both the <code>x-amz-copy-source-if-none-match</code> and <code>x-amz-copy-source-if-modified-since</code>
     * headers are present in the request and evaluate as follows, Amazon S3 returns the <code>412 Precondition Failed</code>
     * response code:
     *
     * <ul>
     *   <li>
     *     <code>x-amz-copy-source-if-none-match</code> condition evaluates to false
     *   </li>
     *   <li>
     *     <code>x-amz-copy-source-if-modified-since</code> condition evaluates to true
     *   </li>
     * </ul>
     */
    public String getCopySourceIfNoneMatch() {
        return copySourceIfNoneMatch;
    }

    /**
     * Copies the object if it hasn't been modified since the specified time.
     *
     * <p> If both the <code>x-amz-copy-source-if-match</code> and <code>x-amz-copy-source-if-unmodified-since</code>
     * headers are present in the request and evaluate as follows, Amazon S3 returns <code>200 OK</code> and copies the
     * data:
     *
     * <ul>
     *   <li>
     *     <code>x-amz-copy-source-if-match</code> condition evaluates to true
     *   </li>
     *   <li>
     *     <code>x-amz-copy-source-if-unmodified-since</code> condition evaluates to false
     *   </li>
     * </ul>
     */
    public Instant getCopySourceIfUnmodifiedSince() {
        return copySourceIfUnmodifiedSince;
    }

    /**
     * The date and time at which the object is no longer cacheable.
     */
    public String getExpires() {
        return expires;
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
     * Copies the object if the entity tag (ETag) of the destination object matches the specified tag. If the ETag
     * values do not match, the operation returns a <code>412 Precondition Failed</code> error. If a concurrent
     * operation occurs during the upload S3 returns a <code>409 ConditionalRequestConflict</code> response. On a 409
     * failure you should fetch the object's ETag and retry the upload.
     *
     * <p>Expects the ETag value as a string.
     *
     * <p>For more information about conditional requests, see <a href="https://tools.ietf.org/html/rfc7232">RFC 7232</a>.
     */
    public String getIfMatch() {
        return ifMatch;
    }

    /**
     * Copies the object only if the object key name at the destination does not already exist in the bucket specified.
     * Otherwise, Amazon S3 returns a <code>412 Precondition Failed</code> error. If a concurrent operation occurs
     * during the upload S3 returns a <code>409 ConditionalRequestConflict</code> response. On a 409 failure you should
     * retry the upload.
     *
     * <p>Expects the '&#42;' (asterisk) character.
     *
     * <p>For more information about conditional requests, see <a href="https://tools.ietf.org/html/rfc7232">RFC 7232</a>.
     */
    public String getIfNoneMatch() {
        return ifNoneMatch;
    }

    /**
     * The key of the destination object.
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
     * Specifies whether the metadata is copied from the source object or replaced with metadata that's provided in the
     * request. When copying an object, you can preserve all metadata (the default) or specify new metadata. If this
     * header isn’t specified, <code>COPY</code> is the default behavior.
     *
     * <p><b>General purpose bucket</b> - For general purpose buckets, when you grant permissions, you can use the <code>
     * s3:x-amz-metadata-directive</code> condition key to enforce certain metadata behavior when objects are uploaded.
     * For more information, see <a href="https://docs.aws.amazon.com/AmazonS3/latest/dev/amazon-s3-policy-keys.html">Amazon S3 condition key examples</a> in the <i>Amazon S3 User Guide</i>.
     *
     * <p><code>x-amz-website-redirect-location</code> is unique to each object and is not copied when using the <code>
     * x-amz-metadata-directive</code> header. To copy the value, you must specify <code>x-amz-website-redirect-location</code>
     * in the request header.
     */
    public MetadataDirective getMetadataDirective() {
        return metadataDirective;
    }

    /**
     * Specifies whether the object tag-set is copied from the source object or replaced with the tag-set that's
     * provided in the request.
     *
     * <p>The default value is <code>COPY</code>.
     *
     * <p><b>Directory buckets</b> - For directory buckets in a <code>CopyObject</code> operation, only the empty
     * tag-set is supported. Any requests that attempt to write non-empty tags into directory buckets will receive a <code>
     * 501 Not Implemented</code> status code. When the destination bucket is a directory bucket, you will receive a <code>
     * 501 Not Implemented</code> response in any of the following situations:
     *
     * <ul>
     *   <li>
     *     When you attempt to <code>COPY</code> the tag-set from an S3 source object that has non-empty tags.
     *   </li>
     *   <li>
     *     When you attempt to <code>REPLACE</code> the tag-set of a source object and set a non-empty value to <code>
     *     x-amz-tagging</code>.
     *   </li>
     *   <li>
     *     When you don't set the <code>x-amz-tagging-directive</code> header and the source object has non-empty
     *     tags. This is because the default value of <code>x-amz-tagging-directive</code> is <code>COPY</code>.
     *   </li>
     * </ul>
     *
     * <p>Because only the empty tag-set is supported for directory buckets in a <code>CopyObject</code> operation, the
     * following situations are allowed:
     *
     * <ul>
     *   <li>
     *     When you attempt to <code>COPY</code> the tag-set from a directory bucket source object that has no tags
     *     to a general purpose bucket. It copies an empty tag-set to the destination object.
     *   </li>
     *   <li>
     *     When you attempt to <code>REPLACE</code> the tag-set of a directory bucket source object and set the <code>
     *     x-amz-tagging</code> value of the directory bucket destination object to empty.
     *   </li>
     *   <li>
     *     When you attempt to <code>REPLACE</code> the tag-set of a general purpose bucket source object that has
     *     non-empty tags and set the <code>x-amz-tagging</code> value of the directory bucket destination object to
     *     empty.
     *   </li>
     *   <li>
     *     When you attempt to <code>REPLACE</code> the tag-set of a directory bucket source object and don't set
     *     the <code>x-amz-tagging</code> value of the directory bucket destination object. This is because the
     *     default value of <code>x-amz-tagging</code> is the empty value.
     *   </li>
     * </ul>
     */
    public TaggingDirective getTaggingDirective() {
        return taggingDirective;
    }

    /**
     * Specifies whether you want to copy annotations from the source object or exclude them. If this header isn't
     * specified, <code>COPY</code> is the default behavior.
     *
     * <p>Valid Values: <code>COPY | EXCLUDE</code>
     *
     * <p>You can specify this directive as either an HTTP header (<code>x-amz-object-annotation-directive</code>) or as
     * a query string parameter. Use the query string form when generating presigned URLs that need to control
     * annotation copy behavior.
     *
     * <p>When set to <code>COPY</code>, you must have <code>s3:GetObjectAnnotation</code> permission on the source
     * object and <code>s3:PutObjectAnnotation</code> permission on the destination. Each annotation copied is billed as
     * a separate PUT request. If annotations on the source are modified during the copy, Amazon S3 returns a retryable
     * error.
     *
     * <p>For directory buckets, annotations are not supported. Use <code>EXCLUDE</code> to copy objects to directory
     * buckets without errors. If you specify <code>COPY</code> for a directory bucket, the request returns HTTP 501
     * (Not Implemented).
     *
     * <p>When you copy objects using multipart upload (for example, when the Amazon Web Services CLI or Amazon Web
     * Services SDKs use Transfer Manager for objects larger than approximately 8 MB), annotations are not copied by
     * default. To include annotations, specify <code>--copy-props default</code> in the Amazon Web Services CLI or the
     * equivalent SDK configuration. With this opt-in, the SDK reads source annotations, completes the multipart upload,
     * and then writes each annotation to the destination. Between the upload completion and the last annotation write,
     * the destination object exists without all its annotations.
     */
    public AnnotationDirective getAnnotationDirective() {
        return annotationDirective;
    }

    /**
     * The server-side encryption algorithm used when storing this object in Amazon S3. Unrecognized or unsupported
     * values won’t write a destination object and will receive a <code>400 Bad Request</code> response.
     *
     * <p>Amazon S3 automatically encrypts all new objects that are copied to an S3 bucket. When copying an object, if
     * you don't specify encryption information in your copy request, the encryption setting of the target object is set
     * to the default encryption configuration of the destination bucket. By default, all buckets have a base level of
     * encryption configuration that uses server-side encryption with Amazon S3 managed keys (SSE-S3). If the
     * destination bucket has a different default encryption configuration, Amazon S3 uses the corresponding encryption
     * key to encrypt the target object copy.
     *
     * <p>With server-side encryption, Amazon S3 encrypts your data as it writes your data to disks in its data centers
     * and decrypts the data when you access it. For more information about server-side encryption, see <a href="https://docs.aws.amazon.com/AmazonS3/latest/dev/serv-side-encryption.html">Using
     * Server-Side Encryption</a> in the <i>Amazon S3 User Guide</i>.
     *
     * <p><b>General purpose buckets </b>
     *
     * <ul>
     *   <li>
     *     For general purpose buckets, there are the following supported options for server-side encryption:
     *     server-side encryption with Key Management Service (KMS) keys (SSE-KMS), dual-layer server-side
     *     encryption with Amazon Web Services KMS keys (DSSE-KMS), and server-side encryption with
     *     customer-provided encryption keys (SSE-C). Amazon S3 uses the corresponding KMS key, or a
     *     customer-provided key to encrypt the target object copy.
     *   </li>
     *   <li>
     *     When you perform a <code>CopyObject</code> operation, if you want to use a different type of encryption
     *     setting for the target object, you can specify appropriate encryption-related headers to encrypt the
     *     target object with an Amazon S3 managed key, a KMS key, or a customer-provided key. If the encryption
     *     setting in your request is different from the default encryption configuration of the destination bucket,
     *     the encryption setting in your request takes precedence.
     *   </li>
     * </ul>
     *
     * <p><b>Directory buckets </b>
     *
     * <ul>
     *   <li>
     *     For directory buckets, there are only two supported options for server-side encryption: server-side
     *     encryption with Amazon S3 managed keys (SSE-S3) (<code>AES256</code>) and server-side encryption with KMS
     *     keys (SSE-KMS) (<code>aws:kms</code>). We recommend that the bucket's default encryption uses the desired
     *     encryption configuration and you don't override the bucket default encryption in your <code>CreateSession</code>
     *     requests or <code>PUT</code> object requests. Then, new objects are automatically encrypted with the
     *     desired encryption settings. For more information, see <a href="https://docs.aws.amazon.com/AmazonS3/latest/userguide/s3-express-serv-side-encryption.html">Protecting data with server-side encryption</a>
     *     in the <i>Amazon S3 User Guide</i>. For more information about the encryption overriding behaviors in
     *     directory buckets, see <a href="https://docs.aws.amazon.com/AmazonS3/latest/userguide/s3-express-specifying-kms-encryption.html">Specifying server-side encryption with KMS for new object uploads</a>.
     *   </li>
     *   <li>
     *     To encrypt new object copies to a directory bucket with SSE-KMS, we recommend you specify SSE-KMS as the
     *     directory bucket's default encryption configuration with a KMS key (specifically, a <a href="https://docs.aws.amazon.com/kms/latest/developerguide/concepts.html#customer-cmk">customer managed
     *     key</a>). The <a href="https://docs.aws.amazon.com/kms/latest/developerguide/concepts.html#aws-managed-cmk">Amazon Web Services managed key</a> (<code>aws/s3</code>) isn't supported. Your SSE-KMS
     *     configuration can only support 1 <a href="https://docs.aws.amazon.com/kms/latest/developerguide/concepts.html#customer-cmk">customer managed key</a> per directory bucket for the lifetime of the
     *     bucket. After you specify a customer managed key for SSE-KMS, you can't override the customer managed key
     *     for the bucket's SSE-KMS configuration. Then, when you perform a <code>CopyObject</code> operation and
     *     want to specify server-side encryption settings for new object copies with SSE-KMS in the
     *     encryption-related request headers, you must ensure the encryption key is the same customer managed key
     *     that you specified for the directory bucket's default encryption configuration.
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
     * If the <code>x-amz-storage-class</code> header is not used, the copied object will be stored in the <code>
     * STANDARD</code> Storage Class by default. The <code>STANDARD</code> storage class provides high durability and
     * high availability. Depending on performance needs, you can specify a different Storage Class.
     *
     * <ul>
     *   <li>
     *     <b>Directory buckets </b> - Directory buckets only support <code>EXPRESS_ONEZONE</code> (the S3 Express
     *     One Zone storage class) in Availability Zones and <code>ONEZONE_IA</code> (the S3 One Zone-Infrequent
     *     Access storage class) in Dedicated Local Zones. Unsupported storage class values won't write a
     *     destination object and will respond with the HTTP status code <code>400 Bad Request</code>.
     *   </li>
     *   <li>
     *     <b>Amazon S3 on Outposts </b> - S3 on Outposts only uses the <code>OUTPOSTS</code> Storage Class.
     *   </li>
     * </ul>
     *
     * <p>You can use the <code>CopyObject</code> action to change the storage class of an object that is already stored
     * in Amazon S3 by using the <code>x-amz-storage-class</code> header. For more information, see <a href="https://docs.aws.amazon.com/AmazonS3/latest/dev/storage-class-intro.html">Storage Classes</a>
     * in the <i>Amazon S3 User Guide</i>.
     *
     * <p>Before using an object as a source object for the copy operation, you must restore a copy of it if it meets
     * any of the following conditions:
     *
     * <ul>
     *   <li>
     *     The storage class of the source object is <code>GLACIER</code> or <code>DEEP_ARCHIVE</code>.
     *   </li>
     *   <li>
     *     The storage class of the source object is <code>INTELLIGENT_TIERING</code> and it's <a href="https://docs.aws.amazon.com/AmazonS3/latest/userguide/intelligent-tiering-overview.html#intel-tiering-tier-definition">S3
     *     Intelligent-Tiering access tier</a> is <code>Archive Access</code> or <code>Deep Archive Access</code>.
     *   </li>
     * </ul>
     *
     * <p>For more information, see <a href="https://docs.aws.amazon.com/AmazonS3/latest/API/API_RestoreObject.html">RestoreObject</a> and <a href="https://docs.aws.amazon.com/AmazonS3/latest/dev/CopyingObjectsExamples.html">Copying Objects</a> in the <i>Amazon S3 User Guide</i>.
     */
    public StorageClass getStorageClass() {
        return storageClass;
    }

    /**
     * If the destination bucket is configured as a website, redirects requests for this object copy to another object
     * in the same bucket or to an external URL. Amazon S3 stores the value of this header in the object metadata. This
     * value is unique to each object and is not copied when using the <code>x-amz-metadata-directive</code> header.
     * Instead, you may opt to provide this header in combination with the <code>x-amz-metadata-directive</code> header.
     *
     * <p>This functionality is not supported for directory buckets.
     */
    public String getWebsiteRedirectLocation() {
        return websiteRedirectLocation;
    }

    /**
     * Specifies the algorithm to use when encrypting the object (for example, <code>AES256</code>).
     *
     * <p>When you perform a <code>CopyObject</code> operation, if you want to use a different type of encryption
     * setting for the target object, you can specify appropriate encryption-related headers to encrypt the target
     * object with an Amazon S3 managed key, a KMS key, or a customer-provided key. If the encryption setting in your
     * request is different from the default encryption configuration of the destination bucket, the encryption setting
     * in your request takes precedence.
     *
     * <p>This functionality is not supported when the destination bucket is a directory bucket.
     */
    public String getSseCustomerAlgorithm() {
        return sseCustomerAlgorithm;
    }

    /**
     * Specifies the customer-provided encryption key for Amazon S3 to use in encrypting data. This value is used to
     * store the object and then it is discarded. Amazon S3 does not store the encryption key. The key must be
     * appropriate for use with the algorithm specified in the <code>x-amz-server-side-encryption-customer-algorithm</code>
     * header.
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
     * Specifies the KMS key ID (Key ID, Key ARN, or Key Alias) to use for object encryption. All GET and PUT requests
     * for an object protected by KMS will fail if they're not made via SSL or using SigV4. For information about
     * configuring any of the officially supported Amazon Web Services SDKs and Amazon Web Services CLI, see <a href="https://docs.aws.amazon.com/AmazonS3/latest/dev/UsingAWSSDK.html#specify-signature-version">
     * Specifying the Signature Version in Request Authentication</a> in the <i>Amazon S3 User Guide</i>.
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
     * Specifies the Amazon Web Services KMS Encryption Context as an additional encryption context to use for the
     * destination object encryption. The value of this header is a base64-encoded UTF-8 string holding JSON with the
     * encryption context key-value pairs.
     *
     * <p><b>General purpose buckets</b> - This value must be explicitly added to specify encryption context for <code>
     * CopyObject</code> requests if you want an additional encryption context for your destination object. The
     * additional encryption context of the source object won't be copied to the destination object. For more
     * information, see <a href="https://docs.aws.amazon.com/AmazonS3/latest/userguide/UsingKMSEncryption.html#encryption-context">Encryption context</a> in the <i>Amazon S3 User Guide</i>.
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
     * Key Management Service (KMS) keys (SSE-KMS). If a target object uses SSE-KMS, you can enable an S3 Bucket Key for
     * the object.
     *
     * <p>Setting this header to <code>true</code> causes Amazon S3 to use an S3 Bucket Key for object encryption with
     * SSE-KMS. Specifying this header with a COPY action doesn’t affect bucket-level settings for S3 Bucket Key.
     *
     * <p>For more information, see <a href="https://docs.aws.amazon.com/AmazonS3/latest/dev/bucket-key.html">Amazon S3 Bucket Keys</a> in the <i>Amazon S3 User Guide</i>.
     *
     * <p><b>Directory buckets</b> - S3 Bucket Keys aren't supported, when you copy SSE-KMS encrypted objects from
     * general purpose buckets to directory buckets, from directory buckets to general purpose buckets, or between
     * directory buckets, through <a href="https://docs.aws.amazon.com/AmazonS3/latest/API/API_CopyObject.html">CopyObject</a>. In this case, Amazon S3 makes a call to KMS every time a copy
     * request is made for a KMS-encrypted object.
     */
    public Boolean isBucketKeyEnabled() {
        return bucketKeyEnabled;
    }

    /**
     * Specifies the algorithm to use when decrypting the source object (for example, <code>AES256</code>).
     *
     * <p>If the source object for the copy is stored in Amazon S3 using SSE-C, you must provide the necessary
     * encryption information in your request so that Amazon S3 can decrypt the object for copying.
     *
     * <p>This functionality is not supported when the source object is in a directory bucket.
     */
    public String getCopySourcesseCustomerAlgorithm() {
        return copySourcesseCustomerAlgorithm;
    }

    /**
     * Specifies the customer-provided encryption key for Amazon S3 to use to decrypt the source object. The encryption
     * key provided in this header must be the same one that was used when the source object was created.
     *
     * <p>If the source object for the copy is stored in Amazon S3 using SSE-C, you must provide the necessary
     * encryption information in your request so that Amazon S3 can decrypt the object for copying.
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
     * <p>If the source object for the copy is stored in Amazon S3 using SSE-C, you must provide the necessary
     * encryption information in your request so that Amazon S3 can decrypt the object for copying.
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
     * The tag-set for the object copy in the destination bucket. This value must be used in conjunction with the <code>
     * x-amz-tagging-directive</code> if you choose <code>REPLACE</code> for the <code>x-amz-tagging-directive</code>.
     * If you choose <code>COPY</code> for the <code>x-amz-tagging-directive</code>, you don't need to set the <code>
     * x-amz-tagging</code> header, because the tag-set will be copied from the source object directly. The tag-set must
     * be encoded as URL Query parameters.
     *
     * <p>The default value is the empty value.
     *
     * <p><b>Directory buckets</b> - For directory buckets in a <code>CopyObject</code> operation, only the empty
     * tag-set is supported. Any requests that attempt to write non-empty tags into directory buckets will receive a <code>
     * 501 Not Implemented</code> status code. When the destination bucket is a directory bucket, you will receive a <code>
     * 501 Not Implemented</code> response in any of the following situations:
     *
     * <ul>
     *   <li>
     *     When you attempt to <code>COPY</code> the tag-set from an S3 source object that has non-empty tags.
     *   </li>
     *   <li>
     *     When you attempt to <code>REPLACE</code> the tag-set of a source object and set a non-empty value to <code>
     *     x-amz-tagging</code>.
     *   </li>
     *   <li>
     *     When you don't set the <code>x-amz-tagging-directive</code> header and the source object has non-empty
     *     tags. This is because the default value of <code>x-amz-tagging-directive</code> is <code>COPY</code>.
     *   </li>
     * </ul>
     *
     * <p>Because only the empty tag-set is supported for directory buckets in a <code>CopyObject</code> operation, the
     * following situations are allowed:
     *
     * <ul>
     *   <li>
     *     When you attempt to <code>COPY</code> the tag-set from a directory bucket source object that has no tags
     *     to a general purpose bucket. It copies an empty tag-set to the destination object.
     *   </li>
     *   <li>
     *     When you attempt to <code>REPLACE</code> the tag-set of a directory bucket source object and set the <code>
     *     x-amz-tagging</code> value of the directory bucket destination object to empty.
     *   </li>
     *   <li>
     *     When you attempt to <code>REPLACE</code> the tag-set of a general purpose bucket source object that has
     *     non-empty tags and set the <code>x-amz-tagging</code> value of the directory bucket destination object to
     *     empty.
     *   </li>
     *   <li>
     *     When you attempt to <code>REPLACE</code> the tag-set of a directory bucket source object and don't set
     *     the <code>x-amz-tagging</code> value of the directory bucket destination object. This is because the
     *     default value of <code>x-amz-tagging</code> is the empty value.
     *   </li>
     * </ul>
     */
    public String getTagging() {
        return tagging;
    }

    /**
     * The Object Lock mode that you want to apply to the object copy.
     *
     * <p>This functionality is not supported for directory buckets.
     */
    public ObjectLockMode getObjectLockMode() {
        return objectLockMode;
    }

    /**
     * The date and time when you want the Object Lock of the object copy to expire.
     *
     * <p>This functionality is not supported for directory buckets.
     */
    public Instant getObjectLockRetainUntilDate() {
        return objectLockRetainUntilDate;
    }

    /**
     * Specifies whether you want to apply a legal hold to the object copy.
     *
     * <p>This functionality is not supported for directory buckets.
     */
    public ObjectLockLegalHoldStatus getObjectLockLegalHoldStatus() {
        return objectLockLegalHoldStatus;
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
        CopyObjectInput that = (CopyObjectInput) other;
        return Objects.equals(this.bucketKeyEnabled, that.bucketKeyEnabled)
               && Objects.equals(this.bucket, that.bucket)
               && Objects.equals(this.cacheControl, that.cacheControl)
               && Objects.equals(this.contentDisposition, that.contentDisposition)
               && Objects.equals(this.contentEncoding, that.contentEncoding)
               && Objects.equals(this.contentLanguage, that.contentLanguage)
               && Objects.equals(this.contentType, that.contentType)
               && Objects.equals(this.copySource, that.copySource)
               && Objects.equals(this.copySourceIfMatch, that.copySourceIfMatch)
               && Objects.equals(this.copySourceIfNoneMatch, that.copySourceIfNoneMatch)
               && Objects.equals(this.expires, that.expires)
               && Objects.equals(this.grantFullControl, that.grantFullControl)
               && Objects.equals(this.grantRead, that.grantRead)
               && Objects.equals(this.grantReadacP, that.grantReadacP)
               && Objects.equals(this.grantWriteacP, that.grantWriteacP)
               && Objects.equals(this.ifMatch, that.ifMatch)
               && Objects.equals(this.ifNoneMatch, that.ifNoneMatch)
               && Objects.equals(this.key, that.key)
               && Objects.equals(this.websiteRedirectLocation, that.websiteRedirectLocation)
               && Objects.equals(this.sseCustomerAlgorithm, that.sseCustomerAlgorithm)
               && Objects.equals(this.sseCustomerKey, that.sseCustomerKey)
               && Objects.equals(this.sseCustomerKeymD5, that.sseCustomerKeymD5)
               && Objects.equals(this.ssekmsKeyId, that.ssekmsKeyId)
               && Objects.equals(this.ssekmsEncryptionContext, that.ssekmsEncryptionContext)
               && Objects.equals(this.copySourcesseCustomerAlgorithm, that.copySourcesseCustomerAlgorithm)
               && Objects.equals(this.copySourcesseCustomerKey, that.copySourcesseCustomerKey)
               && Objects.equals(this.copySourcesseCustomerKeymD5, that.copySourcesseCustomerKeymD5)
               && Objects.equals(this.tagging, that.tagging)
               && Objects.equals(this.expectedBucketOwner, that.expectedBucketOwner)
               && Objects.equals(this.expectedSourceBucketOwner, that.expectedSourceBucketOwner)
               && Objects.equals(this.acl, that.acl)
               && Objects.equals(this.checksumAlgorithm, that.checksumAlgorithm)
               && Objects.equals(this.metadataDirective, that.metadataDirective)
               && Objects.equals(this.taggingDirective, that.taggingDirective)
               && Objects.equals(this.annotationDirective, that.annotationDirective)
               && Objects.equals(this.serverSideEncryption, that.serverSideEncryption)
               && Objects.equals(this.storageClass, that.storageClass)
               && Objects.equals(this.requestPayer, that.requestPayer)
               && Objects.equals(this.objectLockMode, that.objectLockMode)
               && Objects.equals(this.objectLockLegalHoldStatus, that.objectLockLegalHoldStatus)
               && Objects.equals(this.copySourceIfModifiedSince, that.copySourceIfModifiedSince)
               && Objects.equals(this.copySourceIfUnmodifiedSince, that.copySourceIfUnmodifiedSince)
               && Objects.equals(this.objectLockRetainUntilDate, that.objectLockRetainUntilDate)
               && Objects.equals(this.metadata, that.metadata);
    }

    @Override
    public int hashCode() {
        int $hc = Objects.hashCode(acl);
        $hc = 31 * $hc + Objects.hashCode(bucket);
        $hc = 31 * $hc + Objects.hashCode(cacheControl);
        $hc = 31 * $hc + Objects.hashCode(checksumAlgorithm);
        $hc = 31 * $hc + Objects.hashCode(contentDisposition);
        $hc = 31 * $hc + Objects.hashCode(contentEncoding);
        $hc = 31 * $hc + Objects.hashCode(contentLanguage);
        $hc = 31 * $hc + Objects.hashCode(contentType);
        $hc = 31 * $hc + Objects.hashCode(copySource);
        $hc = 31 * $hc + Objects.hashCode(copySourceIfMatch);
        $hc = 31 * $hc + Objects.hashCode(copySourceIfModifiedSince);
        $hc = 31 * $hc + Objects.hashCode(copySourceIfNoneMatch);
        $hc = 31 * $hc + Objects.hashCode(copySourceIfUnmodifiedSince);
        $hc = 31 * $hc + Objects.hashCode(expires);
        $hc = 31 * $hc + Objects.hashCode(grantFullControl);
        $hc = 31 * $hc + Objects.hashCode(grantRead);
        $hc = 31 * $hc + Objects.hashCode(grantReadacP);
        $hc = 31 * $hc + Objects.hashCode(grantWriteacP);
        $hc = 31 * $hc + Objects.hashCode(ifMatch);
        $hc = 31 * $hc + Objects.hashCode(ifNoneMatch);
        $hc = 31 * $hc + Objects.hashCode(key);
        $hc = 31 * $hc + Objects.hashCode(metadata);
        $hc = 31 * $hc + Objects.hashCode(metadataDirective);
        $hc = 31 * $hc + Objects.hashCode(taggingDirective);
        $hc = 31 * $hc + Objects.hashCode(annotationDirective);
        $hc = 31 * $hc + Objects.hashCode(serverSideEncryption);
        $hc = 31 * $hc + Objects.hashCode(storageClass);
        $hc = 31 * $hc + Objects.hashCode(websiteRedirectLocation);
        $hc = 31 * $hc + Objects.hashCode(sseCustomerAlgorithm);
        $hc = 31 * $hc + Objects.hashCode(sseCustomerKey);
        $hc = 31 * $hc + Objects.hashCode(sseCustomerKeymD5);
        $hc = 31 * $hc + Objects.hashCode(ssekmsKeyId);
        $hc = 31 * $hc + Objects.hashCode(ssekmsEncryptionContext);
        $hc = 31 * $hc + Objects.hashCode(bucketKeyEnabled);
        $hc = 31 * $hc + Objects.hashCode(copySourcesseCustomerAlgorithm);
        $hc = 31 * $hc + Objects.hashCode(copySourcesseCustomerKey);
        $hc = 31 * $hc + Objects.hashCode(copySourcesseCustomerKeymD5);
        $hc = 31 * $hc + Objects.hashCode(requestPayer);
        $hc = 31 * $hc + Objects.hashCode(tagging);
        $hc = 31 * $hc + Objects.hashCode(objectLockMode);
        $hc = 31 * $hc + Objects.hashCode(objectLockRetainUntilDate);
        $hc = 31 * $hc + Objects.hashCode(objectLockLegalHoldStatus);
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
        if (acl != null) {
            serializer.writeString($SCHEMA_ACL, acl.getValue());
        }
        serializer.writeString($SCHEMA_BUCKET, bucket);
        if (cacheControl != null) {
            serializer.writeString($SCHEMA_CACHE_CONTROL, cacheControl);
        }
        if (checksumAlgorithm != null) {
            serializer.writeString($SCHEMA_CHECKSUM_ALGORITHM, checksumAlgorithm.getValue());
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
        if (ifMatch != null) {
            serializer.writeString($SCHEMA_IF_MATCH, ifMatch);
        }
        if (ifNoneMatch != null) {
            serializer.writeString($SCHEMA_IF_NONE_MATCH, ifNoneMatch);
        }
        serializer.writeString($SCHEMA_KEY, key);
        if (metadata != null) {
            serializer.writeMap($SCHEMA_METADATA, metadata, metadata.size(), SharedSerde.MetadataSerializer.INSTANCE);
        }
        if (metadataDirective != null) {
            serializer.writeString($SCHEMA_METADATA_DIRECTIVE, metadataDirective.getValue());
        }
        if (taggingDirective != null) {
            serializer.writeString($SCHEMA_TAGGING_DIRECTIVE, taggingDirective.getValue());
        }
        if (annotationDirective != null) {
            serializer.writeString($SCHEMA_ANNOTATION_DIRECTIVE, annotationDirective.getValue());
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
            case 3 -> (T) SchemaUtils.validateSameMember($SCHEMA_ACL, member, acl);
            case 4 -> (T) SchemaUtils.validateSameMember($SCHEMA_CACHE_CONTROL, member, cacheControl);
            case 5 -> (T) SchemaUtils.validateSameMember($SCHEMA_CHECKSUM_ALGORITHM, member, checksumAlgorithm);
            case 6 -> (T) SchemaUtils.validateSameMember($SCHEMA_CONTENT_DISPOSITION, member, contentDisposition);
            case 7 -> (T) SchemaUtils.validateSameMember($SCHEMA_CONTENT_ENCODING, member, contentEncoding);
            case 8 -> (T) SchemaUtils.validateSameMember($SCHEMA_CONTENT_LANGUAGE, member, contentLanguage);
            case 9 -> (T) SchemaUtils.validateSameMember($SCHEMA_CONTENT_TYPE, member, contentType);
            case 10 -> (T) SchemaUtils.validateSameMember($SCHEMA_COPY_SOURCE_IF_MATCH, member, copySourceIfMatch);
            case 11 -> (T) SchemaUtils.validateSameMember($SCHEMA_COPY_SOURCE_IF_MODIFIED_SINCE, member, copySourceIfModifiedSince);
            case 12 -> (T) SchemaUtils.validateSameMember($SCHEMA_COPY_SOURCE_IF_NONE_MATCH, member, copySourceIfNoneMatch);
            case 13 -> (T) SchemaUtils.validateSameMember($SCHEMA_COPY_SOURCE_IF_UNMODIFIED_SINCE, member, copySourceIfUnmodifiedSince);
            case 14 -> (T) SchemaUtils.validateSameMember($SCHEMA_EXPIRES, member, expires);
            case 15 -> (T) SchemaUtils.validateSameMember($SCHEMA_GRANT_FULL_CONTROL, member, grantFullControl);
            case 16 -> (T) SchemaUtils.validateSameMember($SCHEMA_GRANT_READ, member, grantRead);
            case 17 -> (T) SchemaUtils.validateSameMember($SCHEMA_GRANT_READAC_P, member, grantReadacP);
            case 18 -> (T) SchemaUtils.validateSameMember($SCHEMA_GRANT_WRITEAC_P, member, grantWriteacP);
            case 19 -> (T) SchemaUtils.validateSameMember($SCHEMA_IF_MATCH, member, ifMatch);
            case 20 -> (T) SchemaUtils.validateSameMember($SCHEMA_IF_NONE_MATCH, member, ifNoneMatch);
            case 21 -> (T) SchemaUtils.validateSameMember($SCHEMA_METADATA, member, metadata);
            case 22 -> (T) SchemaUtils.validateSameMember($SCHEMA_METADATA_DIRECTIVE, member, metadataDirective);
            case 23 -> (T) SchemaUtils.validateSameMember($SCHEMA_TAGGING_DIRECTIVE, member, taggingDirective);
            case 24 -> (T) SchemaUtils.validateSameMember($SCHEMA_ANNOTATION_DIRECTIVE, member, annotationDirective);
            case 25 -> (T) SchemaUtils.validateSameMember($SCHEMA_SERVER_SIDE_ENCRYPTION, member, serverSideEncryption);
            case 26 -> (T) SchemaUtils.validateSameMember($SCHEMA_STORAGE_CLASS, member, storageClass);
            case 27 -> (T) SchemaUtils.validateSameMember($SCHEMA_WEBSITE_REDIRECT_LOCATION, member, websiteRedirectLocation);
            case 28 -> (T) SchemaUtils.validateSameMember($SCHEMA_SSE_CUSTOMER_ALGORITHM, member, sseCustomerAlgorithm);
            case 29 -> (T) SchemaUtils.validateSameMember($SCHEMA_SSE_CUSTOMER_KEY, member, sseCustomerKey);
            case 30 -> (T) SchemaUtils.validateSameMember($SCHEMA_SSE_CUSTOMER_KEYM_D5, member, sseCustomerKeymD5);
            case 31 -> (T) SchemaUtils.validateSameMember($SCHEMA_SSEKMS_KEY_ID, member, ssekmsKeyId);
            case 32 -> (T) SchemaUtils.validateSameMember($SCHEMA_SSEKMS_ENCRYPTION_CONTEXT, member, ssekmsEncryptionContext);
            case 33 -> (T) SchemaUtils.validateSameMember($SCHEMA_BUCKET_KEY_ENABLED, member, bucketKeyEnabled);
            case 34 -> (T) SchemaUtils.validateSameMember($SCHEMA_COPY_SOURCESSE_CUSTOMER_ALGORITHM, member, copySourcesseCustomerAlgorithm);
            case 35 -> (T) SchemaUtils.validateSameMember($SCHEMA_COPY_SOURCESSE_CUSTOMER_KEY, member, copySourcesseCustomerKey);
            case 36 -> (T) SchemaUtils.validateSameMember($SCHEMA_COPY_SOURCESSE_CUSTOMER_KEYM_D5, member, copySourcesseCustomerKeymD5);
            case 37 -> (T) SchemaUtils.validateSameMember($SCHEMA_REQUEST_PAYER, member, requestPayer);
            case 38 -> (T) SchemaUtils.validateSameMember($SCHEMA_TAGGING, member, tagging);
            case 39 -> (T) SchemaUtils.validateSameMember($SCHEMA_OBJECT_LOCK_MODE, member, objectLockMode);
            case 40 -> (T) SchemaUtils.validateSameMember($SCHEMA_OBJECT_LOCK_RETAIN_UNTIL_DATE, member, objectLockRetainUntilDate);
            case 41 -> (T) SchemaUtils.validateSameMember($SCHEMA_OBJECT_LOCK_LEGAL_HOLD_STATUS, member, objectLockLegalHoldStatus);
            case 42 -> (T) SchemaUtils.validateSameMember($SCHEMA_EXPECTED_BUCKET_OWNER, member, expectedBucketOwner);
            case 43 -> (T) SchemaUtils.validateSameMember($SCHEMA_EXPECTED_SOURCE_BUCKET_OWNER, member, expectedSourceBucketOwner);
            default -> throw new IllegalArgumentException("Attempted to get non-existent member: " + member.id());
        };
    }

    /**
     * Create a new builder containing all the current property values of this object.
     *
     * <p><strong>Note:</strong> This method performs only a shallow copy of the original properties.
     *
     * @return a builder for {@link CopyObjectInput}.
     */
    public Builder toBuilder() {
        var builder = new Builder();
        builder.acl(this.acl);
        builder.bucket(this.bucket);
        builder.cacheControl(this.cacheControl);
        builder.checksumAlgorithm(this.checksumAlgorithm);
        builder.contentDisposition(this.contentDisposition);
        builder.contentEncoding(this.contentEncoding);
        builder.contentLanguage(this.contentLanguage);
        builder.contentType(this.contentType);
        builder.copySource(this.copySource);
        builder.copySourceIfMatch(this.copySourceIfMatch);
        builder.copySourceIfModifiedSince(this.copySourceIfModifiedSince);
        builder.copySourceIfNoneMatch(this.copySourceIfNoneMatch);
        builder.copySourceIfUnmodifiedSince(this.copySourceIfUnmodifiedSince);
        builder.expires(this.expires);
        builder.grantFullControl(this.grantFullControl);
        builder.grantRead(this.grantRead);
        builder.grantReadacP(this.grantReadacP);
        builder.grantWriteacP(this.grantWriteacP);
        builder.ifMatch(this.ifMatch);
        builder.ifNoneMatch(this.ifNoneMatch);
        builder.key(this.key);
        builder.metadata(this.metadata);
        builder.metadataDirective(this.metadataDirective);
        builder.taggingDirective(this.taggingDirective);
        builder.annotationDirective(this.annotationDirective);
        builder.serverSideEncryption(this.serverSideEncryption);
        builder.storageClass(this.storageClass);
        builder.websiteRedirectLocation(this.websiteRedirectLocation);
        builder.sseCustomerAlgorithm(this.sseCustomerAlgorithm);
        builder.sseCustomerKey(this.sseCustomerKey);
        builder.sseCustomerKeymD5(this.sseCustomerKeymD5);
        builder.ssekmsKeyId(this.ssekmsKeyId);
        builder.ssekmsEncryptionContext(this.ssekmsEncryptionContext);
        builder.bucketKeyEnabled(this.bucketKeyEnabled);
        builder.copySourcesseCustomerAlgorithm(this.copySourcesseCustomerAlgorithm);
        builder.copySourcesseCustomerKey(this.copySourcesseCustomerKey);
        builder.copySourcesseCustomerKeymD5(this.copySourcesseCustomerKeymD5);
        builder.requestPayer(this.requestPayer);
        builder.tagging(this.tagging);
        builder.objectLockMode(this.objectLockMode);
        builder.objectLockRetainUntilDate(this.objectLockRetainUntilDate);
        builder.objectLockLegalHoldStatus(this.objectLockLegalHoldStatus);
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
     * Builder for {@link CopyObjectInput}.
     */
    public static final class Builder implements ShapeBuilder<CopyObjectInput> {
        private final PresenceTracker tracker = PresenceTracker.of($SCHEMA);
        private ObjectCannedACL acl;
        private String bucket;
        private String cacheControl;
        private ChecksumAlgorithm checksumAlgorithm;
        private String contentDisposition;
        private String contentEncoding;
        private String contentLanguage;
        private String contentType;
        private String copySource;
        private String copySourceIfMatch;
        private Instant copySourceIfModifiedSince;
        private String copySourceIfNoneMatch;
        private Instant copySourceIfUnmodifiedSince;
        private String expires;
        private String grantFullControl;
        private String grantRead;
        private String grantReadacP;
        private String grantWriteacP;
        private String ifMatch;
        private String ifNoneMatch;
        private String key;
        private Map<String, String> metadata;
        private MetadataDirective metadataDirective;
        private TaggingDirective taggingDirective;
        private AnnotationDirective annotationDirective;
        private ServerSideEncryption serverSideEncryption;
        private StorageClass storageClass;
        private String websiteRedirectLocation;
        private String sseCustomerAlgorithm;
        private String sseCustomerKey;
        private String sseCustomerKeymD5;
        private String ssekmsKeyId;
        private String ssekmsEncryptionContext;
        private Boolean bucketKeyEnabled;
        private String copySourcesseCustomerAlgorithm;
        private String copySourcesseCustomerKey;
        private String copySourcesseCustomerKeymD5;
        private RequestPayer requestPayer;
        private String tagging;
        private ObjectLockMode objectLockMode;
        private Instant objectLockRetainUntilDate;
        private ObjectLockLegalHoldStatus objectLockLegalHoldStatus;
        private String expectedBucketOwner;
        private String expectedSourceBucketOwner;

        private Builder() {}

        @Override
        public Schema schema() {
            return $SCHEMA;
        }

        /**
         * The canned access control list (ACL) to apply to the object.
         *
         * <p>When you copy an object, the ACL metadata is not preserved and is set to <code>private</code> by default. Only
         * the owner has full access control. To override the default ACL setting, specify a new ACL when you generate a
         * copy request. For more information, see <a href="https://docs.aws.amazon.com/AmazonS3/latest/dev/S3_ACLs_UsingACLs.html">Using ACLs</a>.
         *
         * <p>If the destination bucket that you're copying objects to uses the bucket owner enforced setting for S3 Object
         * Ownership, ACLs are disabled and no longer affect permissions. Buckets that use this setting only accept <code>
         * PUT</code> requests that don't specify an ACL or <code>PUT</code> requests that specify bucket owner full control
         * ACLs, such as the <code>bucket-owner-full-control</code> canned ACL or an equivalent form of this ACL expressed
         * in the XML format. For more information, see <a href="https://docs.aws.amazon.com/AmazonS3/latest/userguide/about-object-ownership.html">Controlling ownership of objects and disabling ACLs</a> in the <i>
         * Amazon S3 User Guide</i>.
         *
         * <ul>
         *   <li>
         *     If your destination bucket uses the bucket owner enforced setting for Object Ownership, all objects
         *     written to the bucket by any account will be owned by the bucket owner.
         *   </li>
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
         * The name of the destination bucket.
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
         * <p><b>S3 on Outposts</b> - When you use this action with S3 on Outposts, you must use the Outpost bucket access
         * point ARN or the access point alias for the destination bucket. You can only copy objects within the same Outpost
         * bucket. It's not supported to copy objects across different Amazon Web Services Outposts, between buckets on the
         * same Outposts, or between Outposts buckets and any other bucket types. For more information about S3 on Outposts,
         * see <a href="https://docs.aws.amazon.com/AmazonS3/latest/userguide/S3onOutposts.html">What is S3 on Outposts?</a> in the <i>S3 on Outposts guide</i>. When you use this action with S3 on
         * Outposts through the REST API, you must direct requests to the S3 on Outposts hostname, in the format <code><i>
         * AccessPointName</i>-<i>AccountId</i>.<i>outpostID</i>.s3-outposts.<i>Region</i>.amazonaws.com</code>. The
         * hostname isn't required when you use the Amazon Web Services CLI or SDKs.
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
         * Specifies the caching behavior along the request/reply chain.
         *
         * @return this builder.
         */
        public Builder cacheControl(String cacheControl) {
            this.cacheControl = cacheControl;
            return this;
        }

        /**
         * Indicates the algorithm that you want Amazon S3 to use to create the checksum for the object. For more
         * information, see <a href="https://docs.aws.amazon.com/AmazonS3/latest/userguide/checking-object-integrity.html">Checking object integrity</a> in the <i>Amazon S3 User Guide</i>.
         *
         * <p>When you copy an object, if the source object has a checksum, that checksum value will be copied to the new
         * object by default. If the <code>CopyObject</code> request does not include this <code>x-amz-checksum-algorithm</code>
         * header, the checksum algorithm will be copied from the source object to the destination object (if it's present
         * on the source object). You can optionally specify a different checksum algorithm to use with the <code>
         * x-amz-checksum-algorithm</code> header. Unrecognized or unsupported values will respond with the HTTP status code
         * <code>400 Bad Request</code>.
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
         * Specifies presentational information for the object. Indicates whether an object should be displayed in a web
         * browser or downloaded as a file. It allows specifying the desired filename for the downloaded file.
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
         * The language the content is in.
         *
         * @return this builder.
         */
        public Builder contentLanguage(String contentLanguage) {
            this.contentLanguage = contentLanguage;
            return this;
        }

        /**
         * A standard MIME type that describes the format of the object data.
         *
         * @return this builder.
         */
        public Builder contentType(String contentType) {
            this.contentType = contentType;
            return this;
        }

        /**
         * Specifies the source object for the copy operation. The source object can be up to 5 GB. If the source object is
         * an object that was uploaded by using a multipart upload, the object copy will be a single part object after the
         * source object is copied to the destination bucket.
         *
         * <p>You specify the value of the copy source in one of two formats, depending on whether you want to access the
         * source object through an <a href="https://docs.aws.amazon.com/AmazonS3/latest/userguide/access-points.html">access point</a>:
         *
         * <ul>
         *   <li>
         *     For objects not accessed through an access point, specify the name of the source bucket and the key of
         *     the source object, separated by a slash (/). For example, to copy the object <code>reports/january.pdf</code>
         *     from the general purpose bucket <code>awsexamplebucket</code>, use <code>
         *     awsexamplebucket/reports/january.pdf</code>. The value must be URL-encoded. To copy the object <code>
         *     reports/january.pdf</code> from the directory bucket <code>awsexamplebucket--use1-az5--x-s3</code>, use <code>
         *     awsexamplebucket--use1-az5--x-s3/reports/january.pdf</code>. The value must be URL-encoded.
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
         * <p>If your source bucket versioning is enabled, the <code>x-amz-copy-source</code> header by default identifies
         * the current version of an object to copy. If the current version is a delete marker, Amazon S3 behaves as if the
         * object was deleted. To copy a different version, use the <code>versionId</code> query parameter. Specifically,
         * append <code>?versionId=</code> to the value (for example, <code>
         * awsexamplebucket/reports/january.pdf?versionId=QUpfdndhfd8438MNFDN93jdnJFkdmqnh893</code>). If you don't specify
         * a version ID, Amazon S3 copies the latest version of the source object.
         *
         * <p>If you enable versioning on the destination bucket, Amazon S3 generates a unique version ID for the copied
         * object. This version ID is different from the version ID of the source object. Amazon S3 returns the version ID
         * of the copied object in the <code>x-amz-version-id</code> response header in the response.
         *
         * <p>If you do not enable versioning or suspend it on the destination bucket, the version ID that Amazon S3
         * generates in the <code>x-amz-version-id</code> response header is always null.
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
         * <p> If both the <code>x-amz-copy-source-if-match</code> and <code>x-amz-copy-source-if-unmodified-since</code>
         * headers are present in the request and evaluate as follows, Amazon S3 returns <code>200 OK</code> and copies the
         * data:
         *
         * <ul>
         *   <li>
         *     <code>x-amz-copy-source-if-match</code> condition evaluates to true
         *   </li>
         *   <li>
         *     <code>x-amz-copy-source-if-unmodified-since</code> condition evaluates to false
         *   </li>
         * </ul>
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
         * <p>If both the <code>x-amz-copy-source-if-none-match</code> and <code>x-amz-copy-source-if-modified-since</code>
         * headers are present in the request and evaluate as follows, Amazon S3 returns the <code>412 Precondition Failed</code>
         * response code:
         *
         * <ul>
         *   <li>
         *     <code>x-amz-copy-source-if-none-match</code> condition evaluates to false
         *   </li>
         *   <li>
         *     <code>x-amz-copy-source-if-modified-since</code> condition evaluates to true
         *   </li>
         * </ul>
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
         * <p>If both the <code>x-amz-copy-source-if-none-match</code> and <code>x-amz-copy-source-if-modified-since</code>
         * headers are present in the request and evaluate as follows, Amazon S3 returns the <code>412 Precondition Failed</code>
         * response code:
         *
         * <ul>
         *   <li>
         *     <code>x-amz-copy-source-if-none-match</code> condition evaluates to false
         *   </li>
         *   <li>
         *     <code>x-amz-copy-source-if-modified-since</code> condition evaluates to true
         *   </li>
         * </ul>
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
         * <p> If both the <code>x-amz-copy-source-if-match</code> and <code>x-amz-copy-source-if-unmodified-since</code>
         * headers are present in the request and evaluate as follows, Amazon S3 returns <code>200 OK</code> and copies the
         * data:
         *
         * <ul>
         *   <li>
         *     <code>x-amz-copy-source-if-match</code> condition evaluates to true
         *   </li>
         *   <li>
         *     <code>x-amz-copy-source-if-unmodified-since</code> condition evaluates to false
         *   </li>
         * </ul>
         *
         * @return this builder.
         */
        public Builder copySourceIfUnmodifiedSince(Instant copySourceIfUnmodifiedSince) {
            this.copySourceIfUnmodifiedSince = copySourceIfUnmodifiedSince;
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
         * Copies the object if the entity tag (ETag) of the destination object matches the specified tag. If the ETag
         * values do not match, the operation returns a <code>412 Precondition Failed</code> error. If a concurrent
         * operation occurs during the upload S3 returns a <code>409 ConditionalRequestConflict</code> response. On a 409
         * failure you should fetch the object's ETag and retry the upload.
         *
         * <p>Expects the ETag value as a string.
         *
         * <p>For more information about conditional requests, see <a href="https://tools.ietf.org/html/rfc7232">RFC 7232</a>.
         *
         * @return this builder.
         */
        public Builder ifMatch(String ifMatch) {
            this.ifMatch = ifMatch;
            return this;
        }

        /**
         * Copies the object only if the object key name at the destination does not already exist in the bucket specified.
         * Otherwise, Amazon S3 returns a <code>412 Precondition Failed</code> error. If a concurrent operation occurs
         * during the upload S3 returns a <code>409 ConditionalRequestConflict</code> response. On a 409 failure you should
         * retry the upload.
         *
         * <p>Expects the '&#42;' (asterisk) character.
         *
         * <p>For more information about conditional requests, see <a href="https://tools.ietf.org/html/rfc7232">RFC 7232</a>.
         *
         * @return this builder.
         */
        public Builder ifNoneMatch(String ifNoneMatch) {
            this.ifNoneMatch = ifNoneMatch;
            return this;
        }

        /**
         * The key of the destination object.
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
         * Specifies whether the metadata is copied from the source object or replaced with metadata that's provided in the
         * request. When copying an object, you can preserve all metadata (the default) or specify new metadata. If this
         * header isn’t specified, <code>COPY</code> is the default behavior.
         *
         * <p><b>General purpose bucket</b> - For general purpose buckets, when you grant permissions, you can use the <code>
         * s3:x-amz-metadata-directive</code> condition key to enforce certain metadata behavior when objects are uploaded.
         * For more information, see <a href="https://docs.aws.amazon.com/AmazonS3/latest/dev/amazon-s3-policy-keys.html">Amazon S3 condition key examples</a> in the <i>Amazon S3 User Guide</i>.
         *
         * <p><code>x-amz-website-redirect-location</code> is unique to each object and is not copied when using the <code>
         * x-amz-metadata-directive</code> header. To copy the value, you must specify <code>x-amz-website-redirect-location</code>
         * in the request header.
         *
         * @return this builder.
         */
        public Builder metadataDirective(MetadataDirective metadataDirective) {
            this.metadataDirective = metadataDirective;
            return this;
        }

        /**
         * Specifies whether the object tag-set is copied from the source object or replaced with the tag-set that's
         * provided in the request.
         *
         * <p>The default value is <code>COPY</code>.
         *
         * <p><b>Directory buckets</b> - For directory buckets in a <code>CopyObject</code> operation, only the empty
         * tag-set is supported. Any requests that attempt to write non-empty tags into directory buckets will receive a <code>
         * 501 Not Implemented</code> status code. When the destination bucket is a directory bucket, you will receive a <code>
         * 501 Not Implemented</code> response in any of the following situations:
         *
         * <ul>
         *   <li>
         *     When you attempt to <code>COPY</code> the tag-set from an S3 source object that has non-empty tags.
         *   </li>
         *   <li>
         *     When you attempt to <code>REPLACE</code> the tag-set of a source object and set a non-empty value to <code>
         *     x-amz-tagging</code>.
         *   </li>
         *   <li>
         *     When you don't set the <code>x-amz-tagging-directive</code> header and the source object has non-empty
         *     tags. This is because the default value of <code>x-amz-tagging-directive</code> is <code>COPY</code>.
         *   </li>
         * </ul>
         *
         * <p>Because only the empty tag-set is supported for directory buckets in a <code>CopyObject</code> operation, the
         * following situations are allowed:
         *
         * <ul>
         *   <li>
         *     When you attempt to <code>COPY</code> the tag-set from a directory bucket source object that has no tags
         *     to a general purpose bucket. It copies an empty tag-set to the destination object.
         *   </li>
         *   <li>
         *     When you attempt to <code>REPLACE</code> the tag-set of a directory bucket source object and set the <code>
         *     x-amz-tagging</code> value of the directory bucket destination object to empty.
         *   </li>
         *   <li>
         *     When you attempt to <code>REPLACE</code> the tag-set of a general purpose bucket source object that has
         *     non-empty tags and set the <code>x-amz-tagging</code> value of the directory bucket destination object to
         *     empty.
         *   </li>
         *   <li>
         *     When you attempt to <code>REPLACE</code> the tag-set of a directory bucket source object and don't set
         *     the <code>x-amz-tagging</code> value of the directory bucket destination object. This is because the
         *     default value of <code>x-amz-tagging</code> is the empty value.
         *   </li>
         * </ul>
         *
         * @return this builder.
         */
        public Builder taggingDirective(TaggingDirective taggingDirective) {
            this.taggingDirective = taggingDirective;
            return this;
        }

        /**
         * Specifies whether you want to copy annotations from the source object or exclude them. If this header isn't
         * specified, <code>COPY</code> is the default behavior.
         *
         * <p>Valid Values: <code>COPY | EXCLUDE</code>
         *
         * <p>You can specify this directive as either an HTTP header (<code>x-amz-object-annotation-directive</code>) or as
         * a query string parameter. Use the query string form when generating presigned URLs that need to control
         * annotation copy behavior.
         *
         * <p>When set to <code>COPY</code>, you must have <code>s3:GetObjectAnnotation</code> permission on the source
         * object and <code>s3:PutObjectAnnotation</code> permission on the destination. Each annotation copied is billed as
         * a separate PUT request. If annotations on the source are modified during the copy, Amazon S3 returns a retryable
         * error.
         *
         * <p>For directory buckets, annotations are not supported. Use <code>EXCLUDE</code> to copy objects to directory
         * buckets without errors. If you specify <code>COPY</code> for a directory bucket, the request returns HTTP 501
         * (Not Implemented).
         *
         * <p>When you copy objects using multipart upload (for example, when the Amazon Web Services CLI or Amazon Web
         * Services SDKs use Transfer Manager for objects larger than approximately 8 MB), annotations are not copied by
         * default. To include annotations, specify <code>--copy-props default</code> in the Amazon Web Services CLI or the
         * equivalent SDK configuration. With this opt-in, the SDK reads source annotations, completes the multipart upload,
         * and then writes each annotation to the destination. Between the upload completion and the last annotation write,
         * the destination object exists without all its annotations.
         *
         * @return this builder.
         */
        public Builder annotationDirective(AnnotationDirective annotationDirective) {
            this.annotationDirective = annotationDirective;
            return this;
        }

        /**
         * The server-side encryption algorithm used when storing this object in Amazon S3. Unrecognized or unsupported
         * values won’t write a destination object and will receive a <code>400 Bad Request</code> response.
         *
         * <p>Amazon S3 automatically encrypts all new objects that are copied to an S3 bucket. When copying an object, if
         * you don't specify encryption information in your copy request, the encryption setting of the target object is set
         * to the default encryption configuration of the destination bucket. By default, all buckets have a base level of
         * encryption configuration that uses server-side encryption with Amazon S3 managed keys (SSE-S3). If the
         * destination bucket has a different default encryption configuration, Amazon S3 uses the corresponding encryption
         * key to encrypt the target object copy.
         *
         * <p>With server-side encryption, Amazon S3 encrypts your data as it writes your data to disks in its data centers
         * and decrypts the data when you access it. For more information about server-side encryption, see <a href="https://docs.aws.amazon.com/AmazonS3/latest/dev/serv-side-encryption.html">Using
         * Server-Side Encryption</a> in the <i>Amazon S3 User Guide</i>.
         *
         * <p><b>General purpose buckets </b>
         *
         * <ul>
         *   <li>
         *     For general purpose buckets, there are the following supported options for server-side encryption:
         *     server-side encryption with Key Management Service (KMS) keys (SSE-KMS), dual-layer server-side
         *     encryption with Amazon Web Services KMS keys (DSSE-KMS), and server-side encryption with
         *     customer-provided encryption keys (SSE-C). Amazon S3 uses the corresponding KMS key, or a
         *     customer-provided key to encrypt the target object copy.
         *   </li>
         *   <li>
         *     When you perform a <code>CopyObject</code> operation, if you want to use a different type of encryption
         *     setting for the target object, you can specify appropriate encryption-related headers to encrypt the
         *     target object with an Amazon S3 managed key, a KMS key, or a customer-provided key. If the encryption
         *     setting in your request is different from the default encryption configuration of the destination bucket,
         *     the encryption setting in your request takes precedence.
         *   </li>
         * </ul>
         *
         * <p><b>Directory buckets </b>
         *
         * <ul>
         *   <li>
         *     For directory buckets, there are only two supported options for server-side encryption: server-side
         *     encryption with Amazon S3 managed keys (SSE-S3) (<code>AES256</code>) and server-side encryption with KMS
         *     keys (SSE-KMS) (<code>aws:kms</code>). We recommend that the bucket's default encryption uses the desired
         *     encryption configuration and you don't override the bucket default encryption in your <code>CreateSession</code>
         *     requests or <code>PUT</code> object requests. Then, new objects are automatically encrypted with the
         *     desired encryption settings. For more information, see <a href="https://docs.aws.amazon.com/AmazonS3/latest/userguide/s3-express-serv-side-encryption.html">Protecting data with server-side encryption</a>
         *     in the <i>Amazon S3 User Guide</i>. For more information about the encryption overriding behaviors in
         *     directory buckets, see <a href="https://docs.aws.amazon.com/AmazonS3/latest/userguide/s3-express-specifying-kms-encryption.html">Specifying server-side encryption with KMS for new object uploads</a>.
         *   </li>
         *   <li>
         *     To encrypt new object copies to a directory bucket with SSE-KMS, we recommend you specify SSE-KMS as the
         *     directory bucket's default encryption configuration with a KMS key (specifically, a <a href="https://docs.aws.amazon.com/kms/latest/developerguide/concepts.html#customer-cmk">customer managed
         *     key</a>). The <a href="https://docs.aws.amazon.com/kms/latest/developerguide/concepts.html#aws-managed-cmk">Amazon Web Services managed key</a> (<code>aws/s3</code>) isn't supported. Your SSE-KMS
         *     configuration can only support 1 <a href="https://docs.aws.amazon.com/kms/latest/developerguide/concepts.html#customer-cmk">customer managed key</a> per directory bucket for the lifetime of the
         *     bucket. After you specify a customer managed key for SSE-KMS, you can't override the customer managed key
         *     for the bucket's SSE-KMS configuration. Then, when you perform a <code>CopyObject</code> operation and
         *     want to specify server-side encryption settings for new object copies with SSE-KMS in the
         *     encryption-related request headers, you must ensure the encryption key is the same customer managed key
         *     that you specified for the directory bucket's default encryption configuration.
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
         * If the <code>x-amz-storage-class</code> header is not used, the copied object will be stored in the <code>
         * STANDARD</code> Storage Class by default. The <code>STANDARD</code> storage class provides high durability and
         * high availability. Depending on performance needs, you can specify a different Storage Class.
         *
         * <ul>
         *   <li>
         *     <b>Directory buckets </b> - Directory buckets only support <code>EXPRESS_ONEZONE</code> (the S3 Express
         *     One Zone storage class) in Availability Zones and <code>ONEZONE_IA</code> (the S3 One Zone-Infrequent
         *     Access storage class) in Dedicated Local Zones. Unsupported storage class values won't write a
         *     destination object and will respond with the HTTP status code <code>400 Bad Request</code>.
         *   </li>
         *   <li>
         *     <b>Amazon S3 on Outposts </b> - S3 on Outposts only uses the <code>OUTPOSTS</code> Storage Class.
         *   </li>
         * </ul>
         *
         * <p>You can use the <code>CopyObject</code> action to change the storage class of an object that is already stored
         * in Amazon S3 by using the <code>x-amz-storage-class</code> header. For more information, see <a href="https://docs.aws.amazon.com/AmazonS3/latest/dev/storage-class-intro.html">Storage Classes</a>
         * in the <i>Amazon S3 User Guide</i>.
         *
         * <p>Before using an object as a source object for the copy operation, you must restore a copy of it if it meets
         * any of the following conditions:
         *
         * <ul>
         *   <li>
         *     The storage class of the source object is <code>GLACIER</code> or <code>DEEP_ARCHIVE</code>.
         *   </li>
         *   <li>
         *     The storage class of the source object is <code>INTELLIGENT_TIERING</code> and it's <a href="https://docs.aws.amazon.com/AmazonS3/latest/userguide/intelligent-tiering-overview.html#intel-tiering-tier-definition">S3
         *     Intelligent-Tiering access tier</a> is <code>Archive Access</code> or <code>Deep Archive Access</code>.
         *   </li>
         * </ul>
         *
         * <p>For more information, see <a href="https://docs.aws.amazon.com/AmazonS3/latest/API/API_RestoreObject.html">RestoreObject</a> and <a href="https://docs.aws.amazon.com/AmazonS3/latest/dev/CopyingObjectsExamples.html">Copying Objects</a> in the <i>Amazon S3 User Guide</i>.
         *
         * @return this builder.
         */
        public Builder storageClass(StorageClass storageClass) {
            this.storageClass = storageClass;
            return this;
        }

        /**
         * If the destination bucket is configured as a website, redirects requests for this object copy to another object
         * in the same bucket or to an external URL. Amazon S3 stores the value of this header in the object metadata. This
         * value is unique to each object and is not copied when using the <code>x-amz-metadata-directive</code> header.
         * Instead, you may opt to provide this header in combination with the <code>x-amz-metadata-directive</code> header.
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
         * <p>When you perform a <code>CopyObject</code> operation, if you want to use a different type of encryption
         * setting for the target object, you can specify appropriate encryption-related headers to encrypt the target
         * object with an Amazon S3 managed key, a KMS key, or a customer-provided key. If the encryption setting in your
         * request is different from the default encryption configuration of the destination bucket, the encryption setting
         * in your request takes precedence.
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
         * store the object and then it is discarded. Amazon S3 does not store the encryption key. The key must be
         * appropriate for use with the algorithm specified in the <code>x-amz-server-side-encryption-customer-algorithm</code>
         * header.
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
         * Specifies the KMS key ID (Key ID, Key ARN, or Key Alias) to use for object encryption. All GET and PUT requests
         * for an object protected by KMS will fail if they're not made via SSL or using SigV4. For information about
         * configuring any of the officially supported Amazon Web Services SDKs and Amazon Web Services CLI, see <a href="https://docs.aws.amazon.com/AmazonS3/latest/dev/UsingAWSSDK.html#specify-signature-version">
         * Specifying the Signature Version in Request Authentication</a> in the <i>Amazon S3 User Guide</i>.
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
         * Specifies the Amazon Web Services KMS Encryption Context as an additional encryption context to use for the
         * destination object encryption. The value of this header is a base64-encoded UTF-8 string holding JSON with the
         * encryption context key-value pairs.
         *
         * <p><b>General purpose buckets</b> - This value must be explicitly added to specify encryption context for <code>
         * CopyObject</code> requests if you want an additional encryption context for your destination object. The
         * additional encryption context of the source object won't be copied to the destination object. For more
         * information, see <a href="https://docs.aws.amazon.com/AmazonS3/latest/userguide/UsingKMSEncryption.html#encryption-context">Encryption context</a> in the <i>Amazon S3 User Guide</i>.
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
         * Key Management Service (KMS) keys (SSE-KMS). If a target object uses SSE-KMS, you can enable an S3 Bucket Key for
         * the object.
         *
         * <p>Setting this header to <code>true</code> causes Amazon S3 to use an S3 Bucket Key for object encryption with
         * SSE-KMS. Specifying this header with a COPY action doesn’t affect bucket-level settings for S3 Bucket Key.
         *
         * <p>For more information, see <a href="https://docs.aws.amazon.com/AmazonS3/latest/dev/bucket-key.html">Amazon S3 Bucket Keys</a> in the <i>Amazon S3 User Guide</i>.
         *
         * <p><b>Directory buckets</b> - S3 Bucket Keys aren't supported, when you copy SSE-KMS encrypted objects from
         * general purpose buckets to directory buckets, from directory buckets to general purpose buckets, or between
         * directory buckets, through <a href="https://docs.aws.amazon.com/AmazonS3/latest/API/API_CopyObject.html">CopyObject</a>. In this case, Amazon S3 makes a call to KMS every time a copy
         * request is made for a KMS-encrypted object.
         *
         * @return this builder.
         */
        public Builder bucketKeyEnabled(Boolean bucketKeyEnabled) {
            this.bucketKeyEnabled = bucketKeyEnabled;
            return this;
        }

        /**
         * Specifies the algorithm to use when decrypting the source object (for example, <code>AES256</code>).
         *
         * <p>If the source object for the copy is stored in Amazon S3 using SSE-C, you must provide the necessary
         * encryption information in your request so that Amazon S3 can decrypt the object for copying.
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
         * key provided in this header must be the same one that was used when the source object was created.
         *
         * <p>If the source object for the copy is stored in Amazon S3 using SSE-C, you must provide the necessary
         * encryption information in your request so that Amazon S3 can decrypt the object for copying.
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
         * <p>If the source object for the copy is stored in Amazon S3 using SSE-C, you must provide the necessary
         * encryption information in your request so that Amazon S3 can decrypt the object for copying.
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
         * The tag-set for the object copy in the destination bucket. This value must be used in conjunction with the <code>
         * x-amz-tagging-directive</code> if you choose <code>REPLACE</code> for the <code>x-amz-tagging-directive</code>.
         * If you choose <code>COPY</code> for the <code>x-amz-tagging-directive</code>, you don't need to set the <code>
         * x-amz-tagging</code> header, because the tag-set will be copied from the source object directly. The tag-set must
         * be encoded as URL Query parameters.
         *
         * <p>The default value is the empty value.
         *
         * <p><b>Directory buckets</b> - For directory buckets in a <code>CopyObject</code> operation, only the empty
         * tag-set is supported. Any requests that attempt to write non-empty tags into directory buckets will receive a <code>
         * 501 Not Implemented</code> status code. When the destination bucket is a directory bucket, you will receive a <code>
         * 501 Not Implemented</code> response in any of the following situations:
         *
         * <ul>
         *   <li>
         *     When you attempt to <code>COPY</code> the tag-set from an S3 source object that has non-empty tags.
         *   </li>
         *   <li>
         *     When you attempt to <code>REPLACE</code> the tag-set of a source object and set a non-empty value to <code>
         *     x-amz-tagging</code>.
         *   </li>
         *   <li>
         *     When you don't set the <code>x-amz-tagging-directive</code> header and the source object has non-empty
         *     tags. This is because the default value of <code>x-amz-tagging-directive</code> is <code>COPY</code>.
         *   </li>
         * </ul>
         *
         * <p>Because only the empty tag-set is supported for directory buckets in a <code>CopyObject</code> operation, the
         * following situations are allowed:
         *
         * <ul>
         *   <li>
         *     When you attempt to <code>COPY</code> the tag-set from a directory bucket source object that has no tags
         *     to a general purpose bucket. It copies an empty tag-set to the destination object.
         *   </li>
         *   <li>
         *     When you attempt to <code>REPLACE</code> the tag-set of a directory bucket source object and set the <code>
         *     x-amz-tagging</code> value of the directory bucket destination object to empty.
         *   </li>
         *   <li>
         *     When you attempt to <code>REPLACE</code> the tag-set of a general purpose bucket source object that has
         *     non-empty tags and set the <code>x-amz-tagging</code> value of the directory bucket destination object to
         *     empty.
         *   </li>
         *   <li>
         *     When you attempt to <code>REPLACE</code> the tag-set of a directory bucket source object and don't set
         *     the <code>x-amz-tagging</code> value of the directory bucket destination object. This is because the
         *     default value of <code>x-amz-tagging</code> is the empty value.
         *   </li>
         * </ul>
         *
         * @return this builder.
         */
        public Builder tagging(String tagging) {
            this.tagging = tagging;
            return this;
        }

        /**
         * The Object Lock mode that you want to apply to the object copy.
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
         * The date and time when you want the Object Lock of the object copy to expire.
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
         * Specifies whether you want to apply a legal hold to the object copy.
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
        public CopyObjectInput build() {
            tracker.validate();
            return new CopyObjectInput(this);
        }

        @Override
        @SuppressWarnings("unchecked")
        public void setMemberValue(Schema member, Object value) {
            switch (member.memberIndex()) {
                case 0 -> bucket((String) SchemaUtils.validateSameMember($SCHEMA_BUCKET, member, value));
                case 1 -> copySource((String) SchemaUtils.validateSameMember($SCHEMA_COPY_SOURCE, member, value));
                case 2 -> key((String) SchemaUtils.validateSameMember($SCHEMA_KEY, member, value));
                case 3 -> acl((ObjectCannedACL) SchemaUtils.validateSameMember($SCHEMA_ACL, member, value));
                case 4 -> cacheControl((String) SchemaUtils.validateSameMember($SCHEMA_CACHE_CONTROL, member, value));
                case 5 -> checksumAlgorithm((ChecksumAlgorithm) SchemaUtils.validateSameMember($SCHEMA_CHECKSUM_ALGORITHM, member, value));
                case 6 -> contentDisposition((String) SchemaUtils.validateSameMember($SCHEMA_CONTENT_DISPOSITION, member, value));
                case 7 -> contentEncoding((String) SchemaUtils.validateSameMember($SCHEMA_CONTENT_ENCODING, member, value));
                case 8 -> contentLanguage((String) SchemaUtils.validateSameMember($SCHEMA_CONTENT_LANGUAGE, member, value));
                case 9 -> contentType((String) SchemaUtils.validateSameMember($SCHEMA_CONTENT_TYPE, member, value));
                case 10 -> copySourceIfMatch((String) SchemaUtils.validateSameMember($SCHEMA_COPY_SOURCE_IF_MATCH, member, value));
                case 11 -> copySourceIfModifiedSince((Instant) SchemaUtils.validateSameMember($SCHEMA_COPY_SOURCE_IF_MODIFIED_SINCE, member, value));
                case 12 -> copySourceIfNoneMatch((String) SchemaUtils.validateSameMember($SCHEMA_COPY_SOURCE_IF_NONE_MATCH, member, value));
                case 13 -> copySourceIfUnmodifiedSince((Instant) SchemaUtils.validateSameMember($SCHEMA_COPY_SOURCE_IF_UNMODIFIED_SINCE, member, value));
                case 14 -> expires((String) SchemaUtils.validateSameMember($SCHEMA_EXPIRES, member, value));
                case 15 -> grantFullControl((String) SchemaUtils.validateSameMember($SCHEMA_GRANT_FULL_CONTROL, member, value));
                case 16 -> grantRead((String) SchemaUtils.validateSameMember($SCHEMA_GRANT_READ, member, value));
                case 17 -> grantReadacP((String) SchemaUtils.validateSameMember($SCHEMA_GRANT_READAC_P, member, value));
                case 18 -> grantWriteacP((String) SchemaUtils.validateSameMember($SCHEMA_GRANT_WRITEAC_P, member, value));
                case 19 -> ifMatch((String) SchemaUtils.validateSameMember($SCHEMA_IF_MATCH, member, value));
                case 20 -> ifNoneMatch((String) SchemaUtils.validateSameMember($SCHEMA_IF_NONE_MATCH, member, value));
                case 21 -> metadata((Map<String, String>) SchemaUtils.validateSameMember($SCHEMA_METADATA, member, value));
                case 22 -> metadataDirective((MetadataDirective) SchemaUtils.validateSameMember($SCHEMA_METADATA_DIRECTIVE, member, value));
                case 23 -> taggingDirective((TaggingDirective) SchemaUtils.validateSameMember($SCHEMA_TAGGING_DIRECTIVE, member, value));
                case 24 -> annotationDirective((AnnotationDirective) SchemaUtils.validateSameMember($SCHEMA_ANNOTATION_DIRECTIVE, member, value));
                case 25 -> serverSideEncryption((ServerSideEncryption) SchemaUtils.validateSameMember($SCHEMA_SERVER_SIDE_ENCRYPTION, member, value));
                case 26 -> storageClass((StorageClass) SchemaUtils.validateSameMember($SCHEMA_STORAGE_CLASS, member, value));
                case 27 -> websiteRedirectLocation((String) SchemaUtils.validateSameMember($SCHEMA_WEBSITE_REDIRECT_LOCATION, member, value));
                case 28 -> sseCustomerAlgorithm((String) SchemaUtils.validateSameMember($SCHEMA_SSE_CUSTOMER_ALGORITHM, member, value));
                case 29 -> sseCustomerKey((String) SchemaUtils.validateSameMember($SCHEMA_SSE_CUSTOMER_KEY, member, value));
                case 30 -> sseCustomerKeymD5((String) SchemaUtils.validateSameMember($SCHEMA_SSE_CUSTOMER_KEYM_D5, member, value));
                case 31 -> ssekmsKeyId((String) SchemaUtils.validateSameMember($SCHEMA_SSEKMS_KEY_ID, member, value));
                case 32 -> ssekmsEncryptionContext((String) SchemaUtils.validateSameMember($SCHEMA_SSEKMS_ENCRYPTION_CONTEXT, member, value));
                case 33 -> bucketKeyEnabled((Boolean) SchemaUtils.validateSameMember($SCHEMA_BUCKET_KEY_ENABLED, member, value));
                case 34 -> copySourcesseCustomerAlgorithm((String) SchemaUtils.validateSameMember($SCHEMA_COPY_SOURCESSE_CUSTOMER_ALGORITHM, member, value));
                case 35 -> copySourcesseCustomerKey((String) SchemaUtils.validateSameMember($SCHEMA_COPY_SOURCESSE_CUSTOMER_KEY, member, value));
                case 36 -> copySourcesseCustomerKeymD5((String) SchemaUtils.validateSameMember($SCHEMA_COPY_SOURCESSE_CUSTOMER_KEYM_D5, member, value));
                case 37 -> requestPayer((RequestPayer) SchemaUtils.validateSameMember($SCHEMA_REQUEST_PAYER, member, value));
                case 38 -> tagging((String) SchemaUtils.validateSameMember($SCHEMA_TAGGING, member, value));
                case 39 -> objectLockMode((ObjectLockMode) SchemaUtils.validateSameMember($SCHEMA_OBJECT_LOCK_MODE, member, value));
                case 40 -> objectLockRetainUntilDate((Instant) SchemaUtils.validateSameMember($SCHEMA_OBJECT_LOCK_RETAIN_UNTIL_DATE, member, value));
                case 41 -> objectLockLegalHoldStatus((ObjectLockLegalHoldStatus) SchemaUtils.validateSameMember($SCHEMA_OBJECT_LOCK_LEGAL_HOLD_STATUS, member, value));
                case 42 -> expectedBucketOwner((String) SchemaUtils.validateSameMember($SCHEMA_EXPECTED_BUCKET_OWNER, member, value));
                case 43 -> expectedSourceBucketOwner((String) SchemaUtils.validateSameMember($SCHEMA_EXPECTED_SOURCE_BUCKET_OWNER, member, value));
                default -> ShapeBuilder.super.setMemberValue(member, value);
            }
        }

        @Override
        public ShapeBuilder<CopyObjectInput> errorCorrection() {
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
                    case 3 -> builder.acl(ObjectCannedACL.builder().deserializeMember(de, member).build());
                    case 4 -> builder.cacheControl(de.readString(member));
                    case 5 -> builder.checksumAlgorithm(ChecksumAlgorithm.builder().deserializeMember(de, member).build());
                    case 6 -> builder.contentDisposition(de.readString(member));
                    case 7 -> builder.contentEncoding(de.readString(member));
                    case 8 -> builder.contentLanguage(de.readString(member));
                    case 9 -> builder.contentType(de.readString(member));
                    case 10 -> builder.copySourceIfMatch(de.readString(member));
                    case 11 -> builder.copySourceIfModifiedSince(de.readTimestamp(member));
                    case 12 -> builder.copySourceIfNoneMatch(de.readString(member));
                    case 13 -> builder.copySourceIfUnmodifiedSince(de.readTimestamp(member));
                    case 14 -> builder.expires(de.readString(member));
                    case 15 -> builder.grantFullControl(de.readString(member));
                    case 16 -> builder.grantRead(de.readString(member));
                    case 17 -> builder.grantReadacP(de.readString(member));
                    case 18 -> builder.grantWriteacP(de.readString(member));
                    case 19 -> builder.ifMatch(de.readString(member));
                    case 20 -> builder.ifNoneMatch(de.readString(member));
                    case 21 -> builder.metadata(SharedSerde.deserializeMetadata(member, de));
                    case 22 -> builder.metadataDirective(MetadataDirective.builder().deserializeMember(de, member).build());
                    case 23 -> builder.taggingDirective(TaggingDirective.builder().deserializeMember(de, member).build());
                    case 24 -> builder.annotationDirective(AnnotationDirective.builder().deserializeMember(de, member).build());
                    case 25 -> builder.serverSideEncryption(ServerSideEncryption.builder().deserializeMember(de, member).build());
                    case 26 -> builder.storageClass(StorageClass.builder().deserializeMember(de, member).build());
                    case 27 -> builder.websiteRedirectLocation(de.readString(member));
                    case 28 -> builder.sseCustomerAlgorithm(de.readString(member));
                    case 29 -> builder.sseCustomerKey(de.readString(member));
                    case 30 -> builder.sseCustomerKeymD5(de.readString(member));
                    case 31 -> builder.ssekmsKeyId(de.readString(member));
                    case 32 -> builder.ssekmsEncryptionContext(de.readString(member));
                    case 33 -> builder.bucketKeyEnabled(de.readBoolean(member));
                    case 34 -> builder.copySourcesseCustomerAlgorithm(de.readString(member));
                    case 35 -> builder.copySourcesseCustomerKey(de.readString(member));
                    case 36 -> builder.copySourcesseCustomerKeymD5(de.readString(member));
                    case 37 -> builder.requestPayer(RequestPayer.builder().deserializeMember(de, member).build());
                    case 38 -> builder.tagging(de.readString(member));
                    case 39 -> builder.objectLockMode(ObjectLockMode.builder().deserializeMember(de, member).build());
                    case 40 -> builder.objectLockRetainUntilDate(de.readTimestamp(member));
                    case 41 -> builder.objectLockLegalHoldStatus(ObjectLockLegalHoldStatus.builder().deserializeMember(de, member).build());
                    case 42 -> builder.expectedBucketOwner(de.readString(member));
                    case 43 -> builder.expectedSourceBucketOwner(de.readString(member));
                    default -> throw new IllegalArgumentException("Unexpected member: " + member.memberName());
                }
            }
        }
    }
}
