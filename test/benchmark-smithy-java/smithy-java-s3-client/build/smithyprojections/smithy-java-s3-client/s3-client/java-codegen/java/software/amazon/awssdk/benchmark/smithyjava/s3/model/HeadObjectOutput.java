package software.amazon.awssdk.benchmark.smithyjava.s3.model;

import java.time.Instant;
import java.util.Collections;
import java.util.Map;
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
public final class HeadObjectOutput implements SerializableStruct {

    public static final Schema $SCHEMA = Schemas4.HEAD_OBJECT_OUTPUT;
    private static final Schema $SCHEMA_DELETE_MARKER = $SCHEMA.member("DeleteMarker");
    private static final Schema $SCHEMA_ACCEPT_RANGES = $SCHEMA.member("AcceptRanges");
    private static final Schema $SCHEMA_EXPIRATION = $SCHEMA.member("Expiration");
    private static final Schema $SCHEMA_RESTORE = $SCHEMA.member("Restore");
    private static final Schema $SCHEMA_ARCHIVE_STATUS = $SCHEMA.member("ArchiveStatus");
    private static final Schema $SCHEMA_LAST_MODIFIED = $SCHEMA.member("LastModified");
    private static final Schema $SCHEMA_CONTENT_LENGTH = $SCHEMA.member("ContentLength");
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
    private static final Schema $SCHEMA_E_TAG = $SCHEMA.member("ETag");
    private static final Schema $SCHEMA_MISSING_META = $SCHEMA.member("MissingMeta");
    private static final Schema $SCHEMA_VERSION_ID = $SCHEMA.member("VersionId");
    private static final Schema $SCHEMA_CACHE_CONTROL = $SCHEMA.member("CacheControl");
    private static final Schema $SCHEMA_CONTENT_DISPOSITION = $SCHEMA.member("ContentDisposition");
    private static final Schema $SCHEMA_CONTENT_ENCODING = $SCHEMA.member("ContentEncoding");
    private static final Schema $SCHEMA_CONTENT_LANGUAGE = $SCHEMA.member("ContentLanguage");
    private static final Schema $SCHEMA_CONTENT_TYPE = $SCHEMA.member("ContentType");
    private static final Schema $SCHEMA_CONTENT_RANGE = $SCHEMA.member("ContentRange");
    private static final Schema $SCHEMA_EXPIRES = $SCHEMA.member("Expires");
    private static final Schema $SCHEMA_WEBSITE_REDIRECT_LOCATION = $SCHEMA.member("WebsiteRedirectLocation");
    private static final Schema $SCHEMA_SERVER_SIDE_ENCRYPTION = $SCHEMA.member("ServerSideEncryption");
    private static final Schema $SCHEMA_METADATA = $SCHEMA.member("Metadata");
    private static final Schema $SCHEMA_SSE_CUSTOMER_ALGORITHM = $SCHEMA.member("SSECustomerAlgorithm");
    private static final Schema $SCHEMA_SSE_CUSTOMER_KEYM_D5 = $SCHEMA.member("SSECustomerKeyMD5");
    private static final Schema $SCHEMA_SSEKMS_KEY_ID = $SCHEMA.member("SSEKMSKeyId");
    private static final Schema $SCHEMA_BUCKET_KEY_ENABLED = $SCHEMA.member("BucketKeyEnabled");
    private static final Schema $SCHEMA_STORAGE_CLASS = $SCHEMA.member("StorageClass");
    private static final Schema $SCHEMA_REQUEST_CHARGED = $SCHEMA.member("RequestCharged");
    private static final Schema $SCHEMA_REPLICATION_STATUS = $SCHEMA.member("ReplicationStatus");
    private static final Schema $SCHEMA_PARTS_COUNT = $SCHEMA.member("PartsCount");
    private static final Schema $SCHEMA_TAG_COUNT = $SCHEMA.member("TagCount");
    private static final Schema $SCHEMA_OBJECT_LOCK_MODE = $SCHEMA.member("ObjectLockMode");
    private static final Schema $SCHEMA_OBJECT_LOCK_RETAIN_UNTIL_DATE = $SCHEMA.member("ObjectLockRetainUntilDate");
    private static final Schema $SCHEMA_OBJECT_LOCK_LEGAL_HOLD_STATUS = $SCHEMA.member("ObjectLockLegalHoldStatus");

    public static final ShapeId $ID = $SCHEMA.id();

    private final transient Boolean deleteMarker;
    private final transient String acceptRanges;
    private final transient String expiration;
    private final transient String restore;
    private final transient ArchiveStatus archiveStatus;
    private final transient Instant lastModified;
    private final transient Long contentLength;
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
    private final transient String eTag;
    private final transient Integer missingMeta;
    private final transient String versionId;
    private final transient String cacheControl;
    private final transient String contentDisposition;
    private final transient String contentEncoding;
    private final transient String contentLanguage;
    private final transient String contentType;
    private final transient String contentRange;
    private final transient String expires;
    private final transient String websiteRedirectLocation;
    private final transient ServerSideEncryption serverSideEncryption;
    private final transient Map<String, String> metadata;
    private final transient String sseCustomerAlgorithm;
    private final transient String sseCustomerKeymD5;
    private final transient String ssekmsKeyId;
    private final transient Boolean bucketKeyEnabled;
    private final transient StorageClass storageClass;
    private final transient RequestCharged requestCharged;
    private final transient ReplicationStatus replicationStatus;
    private final transient Integer partsCount;
    private final transient Integer tagCount;
    private final transient ObjectLockMode objectLockMode;
    private final transient Instant objectLockRetainUntilDate;
    private final transient ObjectLockLegalHoldStatus objectLockLegalHoldStatus;

    private HeadObjectOutput(Builder builder) {
        this.deleteMarker = builder.deleteMarker;
        this.acceptRanges = builder.acceptRanges;
        this.expiration = builder.expiration;
        this.restore = builder.restore;
        this.archiveStatus = builder.archiveStatus;
        this.lastModified = builder.lastModified;
        this.contentLength = builder.contentLength;
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
        this.eTag = builder.eTag;
        this.missingMeta = builder.missingMeta;
        this.versionId = builder.versionId;
        this.cacheControl = builder.cacheControl;
        this.contentDisposition = builder.contentDisposition;
        this.contentEncoding = builder.contentEncoding;
        this.contentLanguage = builder.contentLanguage;
        this.contentType = builder.contentType;
        this.contentRange = builder.contentRange;
        this.expires = builder.expires;
        this.websiteRedirectLocation = builder.websiteRedirectLocation;
        this.serverSideEncryption = builder.serverSideEncryption;
        this.metadata = builder.metadata == null ? null : Collections.unmodifiableMap(builder.metadata);
        this.sseCustomerAlgorithm = builder.sseCustomerAlgorithm;
        this.sseCustomerKeymD5 = builder.sseCustomerKeymD5;
        this.ssekmsKeyId = builder.ssekmsKeyId;
        this.bucketKeyEnabled = builder.bucketKeyEnabled;
        this.storageClass = builder.storageClass;
        this.requestCharged = builder.requestCharged;
        this.replicationStatus = builder.replicationStatus;
        this.partsCount = builder.partsCount;
        this.tagCount = builder.tagCount;
        this.objectLockMode = builder.objectLockMode;
        this.objectLockRetainUntilDate = builder.objectLockRetainUntilDate;
        this.objectLockLegalHoldStatus = builder.objectLockLegalHoldStatus;
    }

    /**
     * Specifies whether the object retrieved was (true) or was not (false) a Delete Marker. If false, this response
     * header does not appear in the response.
     *
     * <p>This functionality is not supported for directory buckets.
     */
    public Boolean isDeleteMarker() {
        return deleteMarker;
    }

    /**
     * Indicates that a range of bytes was specified.
     */
    public String getAcceptRanges() {
        return acceptRanges;
    }

    /**
     * If the object expiration is configured (see <a href="https://docs.aws.amazon.com/AmazonS3/latest/API/API_PutBucketLifecycleConfiguration.html"><code>PutBucketLifecycleConfiguration</code></a>), the response
     * includes this header. It includes the <code>expiry-date</code> and <code>rule-id</code> key-value pairs providing
     * object expiration information. The value of the <code>rule-id</code> is URL-encoded.
     *
     * <p>Object expiration information is not returned in directory buckets and this header returns the value "<code>
     * NotImplemented</code>" in all responses for directory buckets.
     */
    public String getExpiration() {
        return expiration;
    }

    /**
     * If the object is an archived object (an object whose storage class is GLACIER), the response includes this header
     * if either the archive restoration is in progress (see <a href="https://docs.aws.amazon.com/AmazonS3/latest/API/API_RestoreObject.html">RestoreObject</a> or an archive copy is already
     * restored.
     *
     * <p> If an archive copy is already restored, the header value indicates when Amazon S3 is scheduled to delete the
     * object copy. For example:
     *
     * <p><code>x-amz-restore: ongoing-request="false", expiry-date="Fri, 21 Dec 2012 00:00:00 GMT"</code>
     *
     * <p>If the object restoration is in progress, the header returns the value <code>ongoing-request="true"</code>.
     *
     * <p>For more information about archiving objects, see <a href="https://docs.aws.amazon.com/AmazonS3/latest/dev/object-lifecycle-mgmt.html#lifecycle-transition-general-considerations">Transitioning Objects: General Considerations</a>.
     *
     * <p>This functionality is not supported for directory buckets. Directory buckets only support <code>
     * EXPRESS_ONEZONE</code> (the S3 Express One Zone storage class) in Availability Zones and <code>ONEZONE_IA</code>
     * (the S3 One Zone-Infrequent Access storage class) in Dedicated Local Zones.
     */
    public String getRestore() {
        return restore;
    }

    /**
     * The archive state of the head object.
     *
     * <p>This functionality is not supported for directory buckets.
     */
    public ArchiveStatus getArchiveStatus() {
        return archiveStatus;
    }

    /**
     * Date and time when the object was last modified.
     */
    public Instant getLastModified() {
        return lastModified;
    }

    /**
     * Size of the body in bytes.
     */
    public Long getContentLength() {
        return contentLength;
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
     * The Base64 encoded, 64-bit <code>CRC64NVME</code> checksum of the object. For more information, see <a
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
     * The Base64 encoded, 512-bit <code>SHA512</code> digest of the object. For more information, see <a
     * href="https://docs.aws.amazon.com/AmazonS3/latest/userguide/checking-object-integrity.html">Checking object
     * integrity in the Amazon S3 User Guide</a>.
     */
    public String getChecksumshA512() {
        return checksumshA512;
    }

    /**
     * The Base64 encoded, 128-bit <code>MD5</code> digest of the object. For more information, see <a
     * href="https://docs.aws.amazon.com/AmazonS3/latest/userguide/checking-object-integrity.html">Checking object
     * integrity in the Amazon S3 User Guide</a>.
     */
    public String getChecksummD5() {
        return checksummD5;
    }

    /**
     * The Base64 encoded, 64-bit <code>XXHASH64</code> checksum of the object. For more information, see <a
     * href="https://docs.aws.amazon.com/AmazonS3/latest/userguide/checking-object-integrity.html">Checking object
     * integrity in the Amazon S3 User Guide</a>.
     */
    public String getChecksumxxhasH64() {
        return checksumxxhasH64;
    }

    /**
     * The Base64 encoded, 64-bit <code>XXHASH3</code> checksum of the object. For more information, see <a
     * href="https://docs.aws.amazon.com/AmazonS3/latest/userguide/checking-object-integrity.html">Checking object
     * integrity in the Amazon S3 User Guide</a>.
     */
    public String getChecksumxxhasH3() {
        return checksumxxhasH3;
    }

    /**
     * The Base64 encoded, 128-bit <code>XXHASH128</code> checksum of the object. For more information, see <a
     * href="https://docs.aws.amazon.com/AmazonS3/latest/userguide/checking-object-integrity.html">Checking object
     * integrity in the Amazon S3 User Guide</a>.
     */
    public String getChecksumxxhasH128() {
        return checksumxxhasH128;
    }

    /**
     * The checksum type, which determines how part-level checksums are combined to create an object-level checksum for
     * multipart objects. You can use this header response to verify that the checksum type that is received is the same
     * checksum type that was specified in <code>CreateMultipartUpload</code> request. For more information, see <a
     * href="https://docs.aws.amazon.com/AmazonS3/latest/userguide/checking-object-integrity.html">Checking object
     * integrity in the Amazon S3 User Guide</a>.
     */
    public ChecksumType getChecksumType() {
        return checksumType;
    }

    /**
     * An entity tag (ETag) is an opaque identifier assigned by a web server to a specific version of a resource found
     * at a URL.
     */
    public String getETag() {
        return eTag;
    }

    /**
     * This is set to the number of metadata entries not returned in <code>x-amz-meta</code> headers. This can happen if
     * you create metadata using an API like SOAP that supports more flexible metadata than the REST API. For example,
     * using SOAP, you can create metadata whose values are not legal HTTP headers.
     *
     * <p>This functionality is not supported for directory buckets.
     */
    public Integer getMissingMeta() {
        return missingMeta;
    }

    /**
     * Version ID of the object.
     *
     * <p>This functionality is not supported for directory buckets.
     */
    public String getVersionId() {
        return versionId;
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
     * Indicates what content encodings have been applied to the object and thus what decoding mechanisms must be
     * applied to obtain the media-type referenced by the Content-Type header field.
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
     * A standard MIME type describing the format of the object data.
     */
    public String getContentType() {
        return contentType;
    }

    /**
     * The portion of the object returned in the response for a <code>GET</code> request.
     */
    public String getContentRange() {
        return contentRange;
    }

    /**
     * The date and time at which the object is no longer cacheable.
     */
    public String getExpires() {
        return expires;
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
     * The server-side encryption algorithm used when you store this object in Amazon S3 or Amazon FSx.
     *
     * <p>When accessing data stored in Amazon FSx file systems using S3 access points, the only valid server side
     * encryption option is <code>aws:fsx</code>.
     */
    public ServerSideEncryption getServerSideEncryption() {
        return serverSideEncryption;
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
     * Indicates whether the object uses an S3 Bucket Key for server-side encryption with Key Management Service (KMS)
     * keys (SSE-KMS).
     */
    public Boolean isBucketKeyEnabled() {
        return bucketKeyEnabled;
    }

    /**
     * Provides storage class information of the object. Amazon S3 returns this header for all objects except for S3
     * Standard storage class objects.
     *
     * <p>For more information, see <a href="https://docs.aws.amazon.com/AmazonS3/latest/dev/storage-class-intro.html">Storage Classes</a>.
     *
     * <p><b>Directory buckets </b> - Directory buckets only support <code>EXPRESS_ONEZONE</code> (the S3 Express One
     * Zone storage class) in Availability Zones and <code>ONEZONE_IA</code> (the S3 One Zone-Infrequent Access storage
     * class) in Dedicated Local Zones.
     */
    public StorageClass getStorageClass() {
        return storageClass;
    }

    public RequestCharged getRequestCharged() {
        return requestCharged;
    }

    /**
     * Amazon S3 can return this header if your request involves a bucket that is either a source or a destination in a
     * replication rule.
     *
     * <p>In replication, you have a source bucket on which you configure replication and destination bucket or buckets
     * where Amazon S3 stores object replicas. When you request an object (<code>GetObject</code>) or object metadata (<code>
     * HeadObject</code>) from these buckets, Amazon S3 will return the <code>x-amz-replication-status</code> header in
     * the response as follows:
     *
     * <ul>
     *   <li>
     *     <b>If requesting an object from the source bucket</b>, Amazon S3 will return the <code>
     *     x-amz-replication-status</code> header if the object in your request is eligible for replication. For
     *     example, suppose that in your replication configuration, you specify object prefix <code>TaxDocs</code>
     *     requesting Amazon S3 to replicate objects with key prefix <code>TaxDocs</code>. Any objects you upload
     *     with this key name prefix, for example <code>TaxDocs/document1.pdf</code>, are eligible for replication.
     *     For any object request with this key name prefix, Amazon S3 will return the <code>
     *     x-amz-replication-status</code> header with value PENDING, COMPLETED or FAILED indicating object
     *     replication status.
     *   </li>
     *   <li>
     *     <b>If requesting an object from a destination bucket</b>, Amazon S3 will return the <code>
     *     x-amz-replication-status</code> header with value REPLICA if the object in your request is a replica that
     *     Amazon S3 created and there is no replica modification replication in progress.
     *   </li>
     *   <li>
     *     <b>When replicating objects to multiple destination buckets</b>, the <code>x-amz-replication-status</code>
     *     header acts differently. The header of the source object will only return a value of COMPLETED when
     *     replication is successful to all destinations. The header will remain at value PENDING until replication
     *     has completed for all destinations. If one or more destinations fails replication the header will return
     *     FAILED.
     *   </li>
     * </ul>
     *
     * <p>For more information, see <a href="https://docs.aws.amazon.com/AmazonS3/latest/dev/NotificationHowTo.html">Replication</a>.
     *
     * <p>This functionality is not supported for directory buckets.
     */
    public ReplicationStatus getReplicationStatus() {
        return replicationStatus;
    }

    /**
     * The count of parts this object has. This value is only returned if you specify <code>partNumber</code> in your
     * request and the object was uploaded as a multipart upload.
     */
    public Integer getPartsCount() {
        return partsCount;
    }

    /**
     * The number of tags, if any, on the object, when you have the relevant permission to read object tags.
     *
     * <p>You can use <a href="https://docs.aws.amazon.com/AmazonS3/latest/API/API_GetObjectTagging.html">GetObjectTagging</a> to retrieve the tag set associated with an object.
     *
     * <p>This functionality is not supported for directory buckets.
     */
    public Integer getTagCount() {
        return tagCount;
    }

    /**
     * The Object Lock mode, if any, that's in effect for this object. This header is only returned if the requester has
     * the <code>s3:GetObjectRetention</code> permission. For more information about S3 Object Lock, see <a href="https://docs.aws.amazon.com/AmazonS3/latest/dev/object-lock.html">Object Lock</a>
     * .
     *
     * <p>This functionality is not supported for directory buckets.
     */
    public ObjectLockMode getObjectLockMode() {
        return objectLockMode;
    }

    /**
     * The date and time when the Object Lock retention period expires. This header is only returned if the requester
     * has the <code>s3:GetObjectRetention</code> permission.
     *
     * <p>This functionality is not supported for directory buckets.
     */
    public Instant getObjectLockRetainUntilDate() {
        return objectLockRetainUntilDate;
    }

    /**
     * Specifies whether a legal hold is in effect for this object. This header is only returned if the requester has
     * the <code>s3:GetObjectLegalHold</code> permission. This header is not returned if the specified version of this
     * object has never had a legal hold applied. For more information about S3 Object Lock, see <a href="https://docs.aws.amazon.com/AmazonS3/latest/dev/object-lock.html">Object Lock</a>.
     *
     * <p>This functionality is not supported for directory buckets.
     */
    public ObjectLockLegalHoldStatus getObjectLockLegalHoldStatus() {
        return objectLockLegalHoldStatus;
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
        HeadObjectOutput that = (HeadObjectOutput) other;
        return Objects.equals(this.deleteMarker, that.deleteMarker)
               && Objects.equals(this.bucketKeyEnabled, that.bucketKeyEnabled)
               && Objects.equals(this.missingMeta, that.missingMeta)
               && Objects.equals(this.partsCount, that.partsCount)
               && Objects.equals(this.tagCount, that.tagCount)
               && Objects.equals(this.contentLength, that.contentLength)
               && Objects.equals(this.acceptRanges, that.acceptRanges)
               && Objects.equals(this.expiration, that.expiration)
               && Objects.equals(this.restore, that.restore)
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
               && Objects.equals(this.eTag, that.eTag)
               && Objects.equals(this.versionId, that.versionId)
               && Objects.equals(this.cacheControl, that.cacheControl)
               && Objects.equals(this.contentDisposition, that.contentDisposition)
               && Objects.equals(this.contentEncoding, that.contentEncoding)
               && Objects.equals(this.contentLanguage, that.contentLanguage)
               && Objects.equals(this.contentType, that.contentType)
               && Objects.equals(this.contentRange, that.contentRange)
               && Objects.equals(this.expires, that.expires)
               && Objects.equals(this.websiteRedirectLocation, that.websiteRedirectLocation)
               && Objects.equals(this.sseCustomerAlgorithm, that.sseCustomerAlgorithm)
               && Objects.equals(this.sseCustomerKeymD5, that.sseCustomerKeymD5)
               && Objects.equals(this.ssekmsKeyId, that.ssekmsKeyId)
               && Objects.equals(this.archiveStatus, that.archiveStatus)
               && Objects.equals(this.checksumType, that.checksumType)
               && Objects.equals(this.serverSideEncryption, that.serverSideEncryption)
               && Objects.equals(this.storageClass, that.storageClass)
               && Objects.equals(this.requestCharged, that.requestCharged)
               && Objects.equals(this.replicationStatus, that.replicationStatus)
               && Objects.equals(this.objectLockMode, that.objectLockMode)
               && Objects.equals(this.objectLockLegalHoldStatus, that.objectLockLegalHoldStatus)
               && Objects.equals(this.lastModified, that.lastModified)
               && Objects.equals(this.objectLockRetainUntilDate, that.objectLockRetainUntilDate)
               && Objects.equals(this.metadata, that.metadata);
    }

    @Override
    public int hashCode() {
        int $hc = Objects.hashCode(deleteMarker);
        $hc = 31 * $hc + Objects.hashCode(acceptRanges);
        $hc = 31 * $hc + Objects.hashCode(expiration);
        $hc = 31 * $hc + Objects.hashCode(restore);
        $hc = 31 * $hc + Objects.hashCode(archiveStatus);
        $hc = 31 * $hc + Objects.hashCode(lastModified);
        $hc = 31 * $hc + Objects.hashCode(contentLength);
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
        $hc = 31 * $hc + Objects.hashCode(eTag);
        $hc = 31 * $hc + Objects.hashCode(missingMeta);
        $hc = 31 * $hc + Objects.hashCode(versionId);
        $hc = 31 * $hc + Objects.hashCode(cacheControl);
        $hc = 31 * $hc + Objects.hashCode(contentDisposition);
        $hc = 31 * $hc + Objects.hashCode(contentEncoding);
        $hc = 31 * $hc + Objects.hashCode(contentLanguage);
        $hc = 31 * $hc + Objects.hashCode(contentType);
        $hc = 31 * $hc + Objects.hashCode(contentRange);
        $hc = 31 * $hc + Objects.hashCode(expires);
        $hc = 31 * $hc + Objects.hashCode(websiteRedirectLocation);
        $hc = 31 * $hc + Objects.hashCode(serverSideEncryption);
        $hc = 31 * $hc + Objects.hashCode(metadata);
        $hc = 31 * $hc + Objects.hashCode(sseCustomerAlgorithm);
        $hc = 31 * $hc + Objects.hashCode(sseCustomerKeymD5);
        $hc = 31 * $hc + Objects.hashCode(ssekmsKeyId);
        $hc = 31 * $hc + Objects.hashCode(bucketKeyEnabled);
        $hc = 31 * $hc + Objects.hashCode(storageClass);
        $hc = 31 * $hc + Objects.hashCode(requestCharged);
        $hc = 31 * $hc + Objects.hashCode(replicationStatus);
        $hc = 31 * $hc + Objects.hashCode(partsCount);
        $hc = 31 * $hc + Objects.hashCode(tagCount);
        $hc = 31 * $hc + Objects.hashCode(objectLockMode);
        $hc = 31 * $hc + Objects.hashCode(objectLockRetainUntilDate);
        $hc = 31 * $hc + Objects.hashCode(objectLockLegalHoldStatus);
        return $hc;
    }

    @Override
    public Schema schema() {
        return $SCHEMA;
    }

    @Override
    public void serializeMembers(ShapeSerializer serializer) {
        if (deleteMarker != null) {
            serializer.writeBoolean($SCHEMA_DELETE_MARKER, deleteMarker);
        }
        if (acceptRanges != null) {
            serializer.writeString($SCHEMA_ACCEPT_RANGES, acceptRanges);
        }
        if (expiration != null) {
            serializer.writeString($SCHEMA_EXPIRATION, expiration);
        }
        if (restore != null) {
            serializer.writeString($SCHEMA_RESTORE, restore);
        }
        if (archiveStatus != null) {
            serializer.writeString($SCHEMA_ARCHIVE_STATUS, archiveStatus.getValue());
        }
        if (lastModified != null) {
            serializer.writeTimestamp($SCHEMA_LAST_MODIFIED, lastModified);
        }
        if (contentLength != null) {
            serializer.writeLong($SCHEMA_CONTENT_LENGTH, contentLength);
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
        if (eTag != null) {
            serializer.writeString($SCHEMA_E_TAG, eTag);
        }
        if (missingMeta != null) {
            serializer.writeInteger($SCHEMA_MISSING_META, missingMeta);
        }
        if (versionId != null) {
            serializer.writeString($SCHEMA_VERSION_ID, versionId);
        }
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
        if (contentRange != null) {
            serializer.writeString($SCHEMA_CONTENT_RANGE, contentRange);
        }
        if (expires != null) {
            serializer.writeString($SCHEMA_EXPIRES, expires);
        }
        if (websiteRedirectLocation != null) {
            serializer.writeString($SCHEMA_WEBSITE_REDIRECT_LOCATION, websiteRedirectLocation);
        }
        if (serverSideEncryption != null) {
            serializer.writeString($SCHEMA_SERVER_SIDE_ENCRYPTION, serverSideEncryption.getValue());
        }
        if (metadata != null) {
            serializer.writeMap($SCHEMA_METADATA, metadata, metadata.size(), SharedSerde.MetadataSerializer.INSTANCE);
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
        if (storageClass != null) {
            serializer.writeString($SCHEMA_STORAGE_CLASS, storageClass.getValue());
        }
        if (requestCharged != null) {
            serializer.writeString($SCHEMA_REQUEST_CHARGED, requestCharged.getValue());
        }
        if (replicationStatus != null) {
            serializer.writeString($SCHEMA_REPLICATION_STATUS, replicationStatus.getValue());
        }
        if (partsCount != null) {
            serializer.writeInteger($SCHEMA_PARTS_COUNT, partsCount);
        }
        if (tagCount != null) {
            serializer.writeInteger($SCHEMA_TAG_COUNT, tagCount);
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
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T getMemberValue(Schema member) {
        return switch (member.memberIndex()) {
            case 0 -> (T) SchemaUtils.validateSameMember($SCHEMA_DELETE_MARKER, member, deleteMarker);
            case 1 -> (T) SchemaUtils.validateSameMember($SCHEMA_ACCEPT_RANGES, member, acceptRanges);
            case 2 -> (T) SchemaUtils.validateSameMember($SCHEMA_EXPIRATION, member, expiration);
            case 3 -> (T) SchemaUtils.validateSameMember($SCHEMA_RESTORE, member, restore);
            case 4 -> (T) SchemaUtils.validateSameMember($SCHEMA_ARCHIVE_STATUS, member, archiveStatus);
            case 5 -> (T) SchemaUtils.validateSameMember($SCHEMA_LAST_MODIFIED, member, lastModified);
            case 6 -> (T) SchemaUtils.validateSameMember($SCHEMA_CONTENT_LENGTH, member, contentLength);
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
            case 17 -> (T) SchemaUtils.validateSameMember($SCHEMA_CHECKSUM_TYPE, member, checksumType);
            case 18 -> (T) SchemaUtils.validateSameMember($SCHEMA_E_TAG, member, eTag);
            case 19 -> (T) SchemaUtils.validateSameMember($SCHEMA_MISSING_META, member, missingMeta);
            case 20 -> (T) SchemaUtils.validateSameMember($SCHEMA_VERSION_ID, member, versionId);
            case 21 -> (T) SchemaUtils.validateSameMember($SCHEMA_CACHE_CONTROL, member, cacheControl);
            case 22 -> (T) SchemaUtils.validateSameMember($SCHEMA_CONTENT_DISPOSITION, member, contentDisposition);
            case 23 -> (T) SchemaUtils.validateSameMember($SCHEMA_CONTENT_ENCODING, member, contentEncoding);
            case 24 -> (T) SchemaUtils.validateSameMember($SCHEMA_CONTENT_LANGUAGE, member, contentLanguage);
            case 25 -> (T) SchemaUtils.validateSameMember($SCHEMA_CONTENT_TYPE, member, contentType);
            case 26 -> (T) SchemaUtils.validateSameMember($SCHEMA_CONTENT_RANGE, member, contentRange);
            case 27 -> (T) SchemaUtils.validateSameMember($SCHEMA_EXPIRES, member, expires);
            case 28 -> (T) SchemaUtils.validateSameMember($SCHEMA_WEBSITE_REDIRECT_LOCATION, member, websiteRedirectLocation);
            case 29 -> (T) SchemaUtils.validateSameMember($SCHEMA_SERVER_SIDE_ENCRYPTION, member, serverSideEncryption);
            case 30 -> (T) SchemaUtils.validateSameMember($SCHEMA_METADATA, member, metadata);
            case 31 -> (T) SchemaUtils.validateSameMember($SCHEMA_SSE_CUSTOMER_ALGORITHM, member, sseCustomerAlgorithm);
            case 32 -> (T) SchemaUtils.validateSameMember($SCHEMA_SSE_CUSTOMER_KEYM_D5, member, sseCustomerKeymD5);
            case 33 -> (T) SchemaUtils.validateSameMember($SCHEMA_SSEKMS_KEY_ID, member, ssekmsKeyId);
            case 34 -> (T) SchemaUtils.validateSameMember($SCHEMA_BUCKET_KEY_ENABLED, member, bucketKeyEnabled);
            case 35 -> (T) SchemaUtils.validateSameMember($SCHEMA_STORAGE_CLASS, member, storageClass);
            case 36 -> (T) SchemaUtils.validateSameMember($SCHEMA_REQUEST_CHARGED, member, requestCharged);
            case 37 -> (T) SchemaUtils.validateSameMember($SCHEMA_REPLICATION_STATUS, member, replicationStatus);
            case 38 -> (T) SchemaUtils.validateSameMember($SCHEMA_PARTS_COUNT, member, partsCount);
            case 39 -> (T) SchemaUtils.validateSameMember($SCHEMA_TAG_COUNT, member, tagCount);
            case 40 -> (T) SchemaUtils.validateSameMember($SCHEMA_OBJECT_LOCK_MODE, member, objectLockMode);
            case 41 -> (T) SchemaUtils.validateSameMember($SCHEMA_OBJECT_LOCK_RETAIN_UNTIL_DATE, member, objectLockRetainUntilDate);
            case 42 -> (T) SchemaUtils.validateSameMember($SCHEMA_OBJECT_LOCK_LEGAL_HOLD_STATUS, member, objectLockLegalHoldStatus);
            default -> throw new IllegalArgumentException("Attempted to get non-existent member: " + member.id());
        };
    }

    /**
     * Create a new builder containing all the current property values of this object.
     *
     * <p><strong>Note:</strong> This method performs only a shallow copy of the original properties.
     *
     * @return a builder for {@link HeadObjectOutput}.
     */
    public Builder toBuilder() {
        var builder = new Builder();
        builder.deleteMarker(this.deleteMarker);
        builder.acceptRanges(this.acceptRanges);
        builder.expiration(this.expiration);
        builder.restore(this.restore);
        builder.archiveStatus(this.archiveStatus);
        builder.lastModified(this.lastModified);
        builder.contentLength(this.contentLength);
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
        builder.eTag(this.eTag);
        builder.missingMeta(this.missingMeta);
        builder.versionId(this.versionId);
        builder.cacheControl(this.cacheControl);
        builder.contentDisposition(this.contentDisposition);
        builder.contentEncoding(this.contentEncoding);
        builder.contentLanguage(this.contentLanguage);
        builder.contentType(this.contentType);
        builder.contentRange(this.contentRange);
        builder.expires(this.expires);
        builder.websiteRedirectLocation(this.websiteRedirectLocation);
        builder.serverSideEncryption(this.serverSideEncryption);
        builder.metadata(this.metadata);
        builder.sseCustomerAlgorithm(this.sseCustomerAlgorithm);
        builder.sseCustomerKeymD5(this.sseCustomerKeymD5);
        builder.ssekmsKeyId(this.ssekmsKeyId);
        builder.bucketKeyEnabled(this.bucketKeyEnabled);
        builder.storageClass(this.storageClass);
        builder.requestCharged(this.requestCharged);
        builder.replicationStatus(this.replicationStatus);
        builder.partsCount(this.partsCount);
        builder.tagCount(this.tagCount);
        builder.objectLockMode(this.objectLockMode);
        builder.objectLockRetainUntilDate(this.objectLockRetainUntilDate);
        builder.objectLockLegalHoldStatus(this.objectLockLegalHoldStatus);
        return builder;
    }

    /**
     * @return returns a new Builder.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for {@link HeadObjectOutput}.
     */
    public static final class Builder implements ShapeBuilder<HeadObjectOutput> {
        private Boolean deleteMarker;
        private String acceptRanges;
        private String expiration;
        private String restore;
        private ArchiveStatus archiveStatus;
        private Instant lastModified;
        private Long contentLength;
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
        private String eTag;
        private Integer missingMeta;
        private String versionId;
        private String cacheControl;
        private String contentDisposition;
        private String contentEncoding;
        private String contentLanguage;
        private String contentType;
        private String contentRange;
        private String expires;
        private String websiteRedirectLocation;
        private ServerSideEncryption serverSideEncryption;
        private Map<String, String> metadata;
        private String sseCustomerAlgorithm;
        private String sseCustomerKeymD5;
        private String ssekmsKeyId;
        private Boolean bucketKeyEnabled;
        private StorageClass storageClass;
        private RequestCharged requestCharged;
        private ReplicationStatus replicationStatus;
        private Integer partsCount;
        private Integer tagCount;
        private ObjectLockMode objectLockMode;
        private Instant objectLockRetainUntilDate;
        private ObjectLockLegalHoldStatus objectLockLegalHoldStatus;

        private Builder() {}

        @Override
        public Schema schema() {
            return $SCHEMA;
        }

        /**
         * Specifies whether the object retrieved was (true) or was not (false) a Delete Marker. If false, this response
         * header does not appear in the response.
         *
         * <p>This functionality is not supported for directory buckets.
         *
         * @return this builder.
         */
        public Builder deleteMarker(Boolean deleteMarker) {
            this.deleteMarker = deleteMarker;
            return this;
        }

        /**
         * Indicates that a range of bytes was specified.
         *
         * @return this builder.
         */
        public Builder acceptRanges(String acceptRanges) {
            this.acceptRanges = acceptRanges;
            return this;
        }

        /**
         * If the object expiration is configured (see <a href="https://docs.aws.amazon.com/AmazonS3/latest/API/API_PutBucketLifecycleConfiguration.html"><code>PutBucketLifecycleConfiguration</code></a>), the response
         * includes this header. It includes the <code>expiry-date</code> and <code>rule-id</code> key-value pairs providing
         * object expiration information. The value of the <code>rule-id</code> is URL-encoded.
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
         * If the object is an archived object (an object whose storage class is GLACIER), the response includes this header
         * if either the archive restoration is in progress (see <a href="https://docs.aws.amazon.com/AmazonS3/latest/API/API_RestoreObject.html">RestoreObject</a> or an archive copy is already
         * restored.
         *
         * <p> If an archive copy is already restored, the header value indicates when Amazon S3 is scheduled to delete the
         * object copy. For example:
         *
         * <p><code>x-amz-restore: ongoing-request="false", expiry-date="Fri, 21 Dec 2012 00:00:00 GMT"</code>
         *
         * <p>If the object restoration is in progress, the header returns the value <code>ongoing-request="true"</code>.
         *
         * <p>For more information about archiving objects, see <a href="https://docs.aws.amazon.com/AmazonS3/latest/dev/object-lifecycle-mgmt.html#lifecycle-transition-general-considerations">Transitioning Objects: General Considerations</a>.
         *
         * <p>This functionality is not supported for directory buckets. Directory buckets only support <code>
         * EXPRESS_ONEZONE</code> (the S3 Express One Zone storage class) in Availability Zones and <code>ONEZONE_IA</code>
         * (the S3 One Zone-Infrequent Access storage class) in Dedicated Local Zones.
         *
         * @return this builder.
         */
        public Builder restore(String restore) {
            this.restore = restore;
            return this;
        }

        /**
         * The archive state of the head object.
         *
         * <p>This functionality is not supported for directory buckets.
         *
         * @return this builder.
         */
        public Builder archiveStatus(ArchiveStatus archiveStatus) {
            this.archiveStatus = archiveStatus;
            return this;
        }

        /**
         * Date and time when the object was last modified.
         *
         * @return this builder.
         */
        public Builder lastModified(Instant lastModified) {
            this.lastModified = lastModified;
            return this;
        }

        /**
         * Size of the body in bytes.
         *
         * @return this builder.
         */
        public Builder contentLength(Long contentLength) {
            this.contentLength = contentLength;
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
         * The Base64 encoded, 64-bit <code>CRC64NVME</code> checksum of the object. For more information, see <a
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
         * The Base64 encoded, 512-bit <code>SHA512</code> digest of the object. For more information, see <a
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
         * The Base64 encoded, 128-bit <code>MD5</code> digest of the object. For more information, see <a
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
         * The Base64 encoded, 64-bit <code>XXHASH64</code> checksum of the object. For more information, see <a
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
         * The Base64 encoded, 64-bit <code>XXHASH3</code> checksum of the object. For more information, see <a
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
         * The Base64 encoded, 128-bit <code>XXHASH128</code> checksum of the object. For more information, see <a
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
         * The checksum type, which determines how part-level checksums are combined to create an object-level checksum for
         * multipart objects. You can use this header response to verify that the checksum type that is received is the same
         * checksum type that was specified in <code>CreateMultipartUpload</code> request. For more information, see <a
         * href="https://docs.aws.amazon.com/AmazonS3/latest/userguide/checking-object-integrity.html">Checking object
         * integrity in the Amazon S3 User Guide</a>.
         *
         * @return this builder.
         */
        public Builder checksumType(ChecksumType checksumType) {
            this.checksumType = checksumType;
            return this;
        }

        /**
         * An entity tag (ETag) is an opaque identifier assigned by a web server to a specific version of a resource found
         * at a URL.
         *
         * @return this builder.
         */
        public Builder eTag(String eTag) {
            this.eTag = eTag;
            return this;
        }

        /**
         * This is set to the number of metadata entries not returned in <code>x-amz-meta</code> headers. This can happen if
         * you create metadata using an API like SOAP that supports more flexible metadata than the REST API. For example,
         * using SOAP, you can create metadata whose values are not legal HTTP headers.
         *
         * <p>This functionality is not supported for directory buckets.
         *
         * @return this builder.
         */
        public Builder missingMeta(Integer missingMeta) {
            this.missingMeta = missingMeta;
            return this;
        }

        /**
         * Version ID of the object.
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
         * Indicates what content encodings have been applied to the object and thus what decoding mechanisms must be
         * applied to obtain the media-type referenced by the Content-Type header field.
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
         * A standard MIME type describing the format of the object data.
         *
         * @return this builder.
         */
        public Builder contentType(String contentType) {
            this.contentType = contentType;
            return this;
        }

        /**
         * The portion of the object returned in the response for a <code>GET</code> request.
         *
         * @return this builder.
         */
        public Builder contentRange(String contentRange) {
            this.contentRange = contentRange;
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
         * A map of metadata to store with the object in S3.
         *
         * @return this builder.
         */
        public Builder metadata(Map<String, String> metadata) {
            this.metadata = metadata;
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
         * Indicates whether the object uses an S3 Bucket Key for server-side encryption with Key Management Service (KMS)
         * keys (SSE-KMS).
         *
         * @return this builder.
         */
        public Builder bucketKeyEnabled(Boolean bucketKeyEnabled) {
            this.bucketKeyEnabled = bucketKeyEnabled;
            return this;
        }

        /**
         * Provides storage class information of the object. Amazon S3 returns this header for all objects except for S3
         * Standard storage class objects.
         *
         * <p>For more information, see <a href="https://docs.aws.amazon.com/AmazonS3/latest/dev/storage-class-intro.html">Storage Classes</a>.
         *
         * <p><b>Directory buckets </b> - Directory buckets only support <code>EXPRESS_ONEZONE</code> (the S3 Express One
         * Zone storage class) in Availability Zones and <code>ONEZONE_IA</code> (the S3 One Zone-Infrequent Access storage
         * class) in Dedicated Local Zones.
         *
         * @return this builder.
         */
        public Builder storageClass(StorageClass storageClass) {
            this.storageClass = storageClass;
            return this;
        }

        /**
         * @return this builder.
         */
        public Builder requestCharged(RequestCharged requestCharged) {
            this.requestCharged = requestCharged;
            return this;
        }

        /**
         * Amazon S3 can return this header if your request involves a bucket that is either a source or a destination in a
         * replication rule.
         *
         * <p>In replication, you have a source bucket on which you configure replication and destination bucket or buckets
         * where Amazon S3 stores object replicas. When you request an object (<code>GetObject</code>) or object metadata (<code>
         * HeadObject</code>) from these buckets, Amazon S3 will return the <code>x-amz-replication-status</code> header in
         * the response as follows:
         *
         * <ul>
         *   <li>
         *     <b>If requesting an object from the source bucket</b>, Amazon S3 will return the <code>
         *     x-amz-replication-status</code> header if the object in your request is eligible for replication. For
         *     example, suppose that in your replication configuration, you specify object prefix <code>TaxDocs</code>
         *     requesting Amazon S3 to replicate objects with key prefix <code>TaxDocs</code>. Any objects you upload
         *     with this key name prefix, for example <code>TaxDocs/document1.pdf</code>, are eligible for replication.
         *     For any object request with this key name prefix, Amazon S3 will return the <code>
         *     x-amz-replication-status</code> header with value PENDING, COMPLETED or FAILED indicating object
         *     replication status.
         *   </li>
         *   <li>
         *     <b>If requesting an object from a destination bucket</b>, Amazon S3 will return the <code>
         *     x-amz-replication-status</code> header with value REPLICA if the object in your request is a replica that
         *     Amazon S3 created and there is no replica modification replication in progress.
         *   </li>
         *   <li>
         *     <b>When replicating objects to multiple destination buckets</b>, the <code>x-amz-replication-status</code>
         *     header acts differently. The header of the source object will only return a value of COMPLETED when
         *     replication is successful to all destinations. The header will remain at value PENDING until replication
         *     has completed for all destinations. If one or more destinations fails replication the header will return
         *     FAILED.
         *   </li>
         * </ul>
         *
         * <p>For more information, see <a href="https://docs.aws.amazon.com/AmazonS3/latest/dev/NotificationHowTo.html">Replication</a>.
         *
         * <p>This functionality is not supported for directory buckets.
         *
         * @return this builder.
         */
        public Builder replicationStatus(ReplicationStatus replicationStatus) {
            this.replicationStatus = replicationStatus;
            return this;
        }

        /**
         * The count of parts this object has. This value is only returned if you specify <code>partNumber</code> in your
         * request and the object was uploaded as a multipart upload.
         *
         * @return this builder.
         */
        public Builder partsCount(Integer partsCount) {
            this.partsCount = partsCount;
            return this;
        }

        /**
         * The number of tags, if any, on the object, when you have the relevant permission to read object tags.
         *
         * <p>You can use <a href="https://docs.aws.amazon.com/AmazonS3/latest/API/API_GetObjectTagging.html">GetObjectTagging</a> to retrieve the tag set associated with an object.
         *
         * <p>This functionality is not supported for directory buckets.
         *
         * @return this builder.
         */
        public Builder tagCount(Integer tagCount) {
            this.tagCount = tagCount;
            return this;
        }

        /**
         * The Object Lock mode, if any, that's in effect for this object. This header is only returned if the requester has
         * the <code>s3:GetObjectRetention</code> permission. For more information about S3 Object Lock, see <a href="https://docs.aws.amazon.com/AmazonS3/latest/dev/object-lock.html">Object Lock</a>
         * .
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
         * The date and time when the Object Lock retention period expires. This header is only returned if the requester
         * has the <code>s3:GetObjectRetention</code> permission.
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
         * Specifies whether a legal hold is in effect for this object. This header is only returned if the requester has
         * the <code>s3:GetObjectLegalHold</code> permission. This header is not returned if the specified version of this
         * object has never had a legal hold applied. For more information about S3 Object Lock, see <a href="https://docs.aws.amazon.com/AmazonS3/latest/dev/object-lock.html">Object Lock</a>.
         *
         * <p>This functionality is not supported for directory buckets.
         *
         * @return this builder.
         */
        public Builder objectLockLegalHoldStatus(ObjectLockLegalHoldStatus objectLockLegalHoldStatus) {
            this.objectLockLegalHoldStatus = objectLockLegalHoldStatus;
            return this;
        }

        @Override
        public HeadObjectOutput build() {
            return new HeadObjectOutput(this);
        }

        @Override
        @SuppressWarnings("unchecked")
        public void setMemberValue(Schema member, Object value) {
            switch (member.memberIndex()) {
                case 0 -> deleteMarker((Boolean) SchemaUtils.validateSameMember($SCHEMA_DELETE_MARKER, member, value));
                case 1 -> acceptRanges((String) SchemaUtils.validateSameMember($SCHEMA_ACCEPT_RANGES, member, value));
                case 2 -> expiration((String) SchemaUtils.validateSameMember($SCHEMA_EXPIRATION, member, value));
                case 3 -> restore((String) SchemaUtils.validateSameMember($SCHEMA_RESTORE, member, value));
                case 4 -> archiveStatus((ArchiveStatus) SchemaUtils.validateSameMember($SCHEMA_ARCHIVE_STATUS, member, value));
                case 5 -> lastModified((Instant) SchemaUtils.validateSameMember($SCHEMA_LAST_MODIFIED, member, value));
                case 6 -> contentLength((Long) SchemaUtils.validateSameMember($SCHEMA_CONTENT_LENGTH, member, value));
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
                case 17 -> checksumType((ChecksumType) SchemaUtils.validateSameMember($SCHEMA_CHECKSUM_TYPE, member, value));
                case 18 -> eTag((String) SchemaUtils.validateSameMember($SCHEMA_E_TAG, member, value));
                case 19 -> missingMeta((Integer) SchemaUtils.validateSameMember($SCHEMA_MISSING_META, member, value));
                case 20 -> versionId((String) SchemaUtils.validateSameMember($SCHEMA_VERSION_ID, member, value));
                case 21 -> cacheControl((String) SchemaUtils.validateSameMember($SCHEMA_CACHE_CONTROL, member, value));
                case 22 -> contentDisposition((String) SchemaUtils.validateSameMember($SCHEMA_CONTENT_DISPOSITION, member, value));
                case 23 -> contentEncoding((String) SchemaUtils.validateSameMember($SCHEMA_CONTENT_ENCODING, member, value));
                case 24 -> contentLanguage((String) SchemaUtils.validateSameMember($SCHEMA_CONTENT_LANGUAGE, member, value));
                case 25 -> contentType((String) SchemaUtils.validateSameMember($SCHEMA_CONTENT_TYPE, member, value));
                case 26 -> contentRange((String) SchemaUtils.validateSameMember($SCHEMA_CONTENT_RANGE, member, value));
                case 27 -> expires((String) SchemaUtils.validateSameMember($SCHEMA_EXPIRES, member, value));
                case 28 -> websiteRedirectLocation((String) SchemaUtils.validateSameMember($SCHEMA_WEBSITE_REDIRECT_LOCATION, member, value));
                case 29 -> serverSideEncryption((ServerSideEncryption) SchemaUtils.validateSameMember($SCHEMA_SERVER_SIDE_ENCRYPTION, member, value));
                case 30 -> metadata((Map<String, String>) SchemaUtils.validateSameMember($SCHEMA_METADATA, member, value));
                case 31 -> sseCustomerAlgorithm((String) SchemaUtils.validateSameMember($SCHEMA_SSE_CUSTOMER_ALGORITHM, member, value));
                case 32 -> sseCustomerKeymD5((String) SchemaUtils.validateSameMember($SCHEMA_SSE_CUSTOMER_KEYM_D5, member, value));
                case 33 -> ssekmsKeyId((String) SchemaUtils.validateSameMember($SCHEMA_SSEKMS_KEY_ID, member, value));
                case 34 -> bucketKeyEnabled((Boolean) SchemaUtils.validateSameMember($SCHEMA_BUCKET_KEY_ENABLED, member, value));
                case 35 -> storageClass((StorageClass) SchemaUtils.validateSameMember($SCHEMA_STORAGE_CLASS, member, value));
                case 36 -> requestCharged((RequestCharged) SchemaUtils.validateSameMember($SCHEMA_REQUEST_CHARGED, member, value));
                case 37 -> replicationStatus((ReplicationStatus) SchemaUtils.validateSameMember($SCHEMA_REPLICATION_STATUS, member, value));
                case 38 -> partsCount((Integer) SchemaUtils.validateSameMember($SCHEMA_PARTS_COUNT, member, value));
                case 39 -> tagCount((Integer) SchemaUtils.validateSameMember($SCHEMA_TAG_COUNT, member, value));
                case 40 -> objectLockMode((ObjectLockMode) SchemaUtils.validateSameMember($SCHEMA_OBJECT_LOCK_MODE, member, value));
                case 41 -> objectLockRetainUntilDate((Instant) SchemaUtils.validateSameMember($SCHEMA_OBJECT_LOCK_RETAIN_UNTIL_DATE, member, value));
                case 42 -> objectLockLegalHoldStatus((ObjectLockLegalHoldStatus) SchemaUtils.validateSameMember($SCHEMA_OBJECT_LOCK_LEGAL_HOLD_STATUS, member, value));
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
                    case 0 -> builder.deleteMarker(de.readBoolean(member));
                    case 1 -> builder.acceptRanges(de.readString(member));
                    case 2 -> builder.expiration(de.readString(member));
                    case 3 -> builder.restore(de.readString(member));
                    case 4 -> builder.archiveStatus(ArchiveStatus.builder().deserializeMember(de, member).build());
                    case 5 -> builder.lastModified(de.readTimestamp(member));
                    case 6 -> builder.contentLength(de.readLong(member));
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
                    case 17 -> builder.checksumType(ChecksumType.builder().deserializeMember(de, member).build());
                    case 18 -> builder.eTag(de.readString(member));
                    case 19 -> builder.missingMeta(de.readInteger(member));
                    case 20 -> builder.versionId(de.readString(member));
                    case 21 -> builder.cacheControl(de.readString(member));
                    case 22 -> builder.contentDisposition(de.readString(member));
                    case 23 -> builder.contentEncoding(de.readString(member));
                    case 24 -> builder.contentLanguage(de.readString(member));
                    case 25 -> builder.contentType(de.readString(member));
                    case 26 -> builder.contentRange(de.readString(member));
                    case 27 -> builder.expires(de.readString(member));
                    case 28 -> builder.websiteRedirectLocation(de.readString(member));
                    case 29 -> builder.serverSideEncryption(ServerSideEncryption.builder().deserializeMember(de, member).build());
                    case 30 -> builder.metadata(SharedSerde.deserializeMetadata(member, de));
                    case 31 -> builder.sseCustomerAlgorithm(de.readString(member));
                    case 32 -> builder.sseCustomerKeymD5(de.readString(member));
                    case 33 -> builder.ssekmsKeyId(de.readString(member));
                    case 34 -> builder.bucketKeyEnabled(de.readBoolean(member));
                    case 35 -> builder.storageClass(StorageClass.builder().deserializeMember(de, member).build());
                    case 36 -> builder.requestCharged(RequestCharged.builder().deserializeMember(de, member).build());
                    case 37 -> builder.replicationStatus(ReplicationStatus.builder().deserializeMember(de, member).build());
                    case 38 -> builder.partsCount(de.readInteger(member));
                    case 39 -> builder.tagCount(de.readInteger(member));
                    case 40 -> builder.objectLockMode(ObjectLockMode.builder().deserializeMember(de, member).build());
                    case 41 -> builder.objectLockRetainUntilDate(de.readTimestamp(member));
                    case 42 -> builder.objectLockLegalHoldStatus(ObjectLockLegalHoldStatus.builder().deserializeMember(de, member).build());
                    default -> throw new IllegalArgumentException("Unexpected member: " + member.memberName());
                }
            }
        }
    }
}
