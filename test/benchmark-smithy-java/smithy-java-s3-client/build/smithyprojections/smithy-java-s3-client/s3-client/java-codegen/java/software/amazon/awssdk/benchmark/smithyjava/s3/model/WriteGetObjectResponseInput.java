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
public final class WriteGetObjectResponseInput implements SerializableStruct, Closeable {

    public static final Schema $SCHEMA = Schemas7.WRITE_GET_OBJECT_RESPONSE_INPUT;
    private static final Schema $SCHEMA_REQUEST_ROUTE = $SCHEMA.member("RequestRoute");
    private static final Schema $SCHEMA_REQUEST_TOKEN = $SCHEMA.member("RequestToken");
    private static final Schema $SCHEMA_BODY = $SCHEMA.member("Body");
    private static final Schema $SCHEMA_STATUS_CODE = $SCHEMA.member("StatusCode");
    private static final Schema $SCHEMA_ERROR_CODE = $SCHEMA.member("ErrorCode");
    private static final Schema $SCHEMA_ERROR_MESSAGE = $SCHEMA.member("ErrorMessage");
    private static final Schema $SCHEMA_ACCEPT_RANGES = $SCHEMA.member("AcceptRanges");
    private static final Schema $SCHEMA_CACHE_CONTROL = $SCHEMA.member("CacheControl");
    private static final Schema $SCHEMA_CONTENT_DISPOSITION = $SCHEMA.member("ContentDisposition");
    private static final Schema $SCHEMA_CONTENT_ENCODING = $SCHEMA.member("ContentEncoding");
    private static final Schema $SCHEMA_CONTENT_LANGUAGE = $SCHEMA.member("ContentLanguage");
    private static final Schema $SCHEMA_CONTENT_LENGTH = $SCHEMA.member("ContentLength");
    private static final Schema $SCHEMA_CONTENT_RANGE = $SCHEMA.member("ContentRange");
    private static final Schema $SCHEMA_CONTENT_TYPE = $SCHEMA.member("ContentType");
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
    private static final Schema $SCHEMA_DELETE_MARKER = $SCHEMA.member("DeleteMarker");
    private static final Schema $SCHEMA_E_TAG = $SCHEMA.member("ETag");
    private static final Schema $SCHEMA_EXPIRES = $SCHEMA.member("Expires");
    private static final Schema $SCHEMA_EXPIRATION = $SCHEMA.member("Expiration");
    private static final Schema $SCHEMA_LAST_MODIFIED = $SCHEMA.member("LastModified");
    private static final Schema $SCHEMA_MISSING_META = $SCHEMA.member("MissingMeta");
    private static final Schema $SCHEMA_METADATA = $SCHEMA.member("Metadata");
    private static final Schema $SCHEMA_OBJECT_LOCK_MODE = $SCHEMA.member("ObjectLockMode");
    private static final Schema $SCHEMA_OBJECT_LOCK_LEGAL_HOLD_STATUS = $SCHEMA.member("ObjectLockLegalHoldStatus");
    private static final Schema $SCHEMA_OBJECT_LOCK_RETAIN_UNTIL_DATE = $SCHEMA.member("ObjectLockRetainUntilDate");
    private static final Schema $SCHEMA_PARTS_COUNT = $SCHEMA.member("PartsCount");
    private static final Schema $SCHEMA_REPLICATION_STATUS = $SCHEMA.member("ReplicationStatus");
    private static final Schema $SCHEMA_REQUEST_CHARGED = $SCHEMA.member("RequestCharged");
    private static final Schema $SCHEMA_RESTORE = $SCHEMA.member("Restore");
    private static final Schema $SCHEMA_SERVER_SIDE_ENCRYPTION = $SCHEMA.member("ServerSideEncryption");
    private static final Schema $SCHEMA_SSE_CUSTOMER_ALGORITHM = $SCHEMA.member("SSECustomerAlgorithm");
    private static final Schema $SCHEMA_SSEKMS_KEY_ID = $SCHEMA.member("SSEKMSKeyId");
    private static final Schema $SCHEMA_SSE_CUSTOMER_KEYM_D5 = $SCHEMA.member("SSECustomerKeyMD5");
    private static final Schema $SCHEMA_STORAGE_CLASS = $SCHEMA.member("StorageClass");
    private static final Schema $SCHEMA_TAG_COUNT = $SCHEMA.member("TagCount");
    private static final Schema $SCHEMA_VERSION_ID = $SCHEMA.member("VersionId");
    private static final Schema $SCHEMA_BUCKET_KEY_ENABLED = $SCHEMA.member("BucketKeyEnabled");

    public static final ShapeId $ID = $SCHEMA.id();

    private final transient String requestRoute;
    private final transient String requestToken;
    private final transient DataStream body;
    private final transient Integer statusCode;
    private final transient String errorCode;
    private final transient String errorMessage;
    private final transient String acceptRanges;
    private final transient String cacheControl;
    private final transient String contentDisposition;
    private final transient String contentEncoding;
    private final transient String contentLanguage;
    private final transient Long contentLength;
    private final transient String contentRange;
    private final transient String contentType;
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
    private final transient Boolean deleteMarker;
    private final transient String eTag;
    private final transient String expires;
    private final transient String expiration;
    private final transient Instant lastModified;
    private final transient Integer missingMeta;
    private final transient Map<String, String> metadata;
    private final transient ObjectLockMode objectLockMode;
    private final transient ObjectLockLegalHoldStatus objectLockLegalHoldStatus;
    private final transient Instant objectLockRetainUntilDate;
    private final transient Integer partsCount;
    private final transient ReplicationStatus replicationStatus;
    private final transient RequestCharged requestCharged;
    private final transient String restore;
    private final transient ServerSideEncryption serverSideEncryption;
    private final transient String sseCustomerAlgorithm;
    private final transient String ssekmsKeyId;
    private final transient String sseCustomerKeymD5;
    private final transient StorageClass storageClass;
    private final transient Integer tagCount;
    private final transient String versionId;
    private final transient Boolean bucketKeyEnabled;

    private WriteGetObjectResponseInput(Builder builder) {
        this.requestRoute = builder.requestRoute;
        this.requestToken = builder.requestToken;
        this.body = builder.body;
        this.statusCode = builder.statusCode;
        this.errorCode = builder.errorCode;
        this.errorMessage = builder.errorMessage;
        this.acceptRanges = builder.acceptRanges;
        this.cacheControl = builder.cacheControl;
        this.contentDisposition = builder.contentDisposition;
        this.contentEncoding = builder.contentEncoding;
        this.contentLanguage = builder.contentLanguage;
        this.contentLength = builder.contentLength;
        this.contentRange = builder.contentRange;
        this.contentType = builder.contentType;
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
        this.deleteMarker = builder.deleteMarker;
        this.eTag = builder.eTag;
        this.expires = builder.expires;
        this.expiration = builder.expiration;
        this.lastModified = builder.lastModified;
        this.missingMeta = builder.missingMeta;
        this.metadata = builder.metadata == null ? null : Collections.unmodifiableMap(builder.metadata);
        this.objectLockMode = builder.objectLockMode;
        this.objectLockLegalHoldStatus = builder.objectLockLegalHoldStatus;
        this.objectLockRetainUntilDate = builder.objectLockRetainUntilDate;
        this.partsCount = builder.partsCount;
        this.replicationStatus = builder.replicationStatus;
        this.requestCharged = builder.requestCharged;
        this.restore = builder.restore;
        this.serverSideEncryption = builder.serverSideEncryption;
        this.sseCustomerAlgorithm = builder.sseCustomerAlgorithm;
        this.ssekmsKeyId = builder.ssekmsKeyId;
        this.sseCustomerKeymD5 = builder.sseCustomerKeymD5;
        this.storageClass = builder.storageClass;
        this.tagCount = builder.tagCount;
        this.versionId = builder.versionId;
        this.bucketKeyEnabled = builder.bucketKeyEnabled;
    }

    /**
     * Route prefix to the HTTP URL generated.
     */
    public String getRequestRoute() {
        return requestRoute;
    }

    /**
     * A single use encrypted token that maps <code>WriteGetObjectResponse</code> to the end user <code>GetObject</code>
     * request.
     */
    public String getRequestToken() {
        return requestToken;
    }

    /**
     * The object data.
     */
    public DataStream getBody() {
        return body;
    }

    /**
     * The integer status code for an HTTP response of a corresponding <code>GetObject</code> request. The following is
     * a list of status codes.
     *
     * <ul>
     *   <li>
     *     <code>200 - OK</code>
     *   </li>
     *   <li>
     *     <code>206 - Partial Content</code>
     *   </li>
     *   <li>
     *     <code>304 - Not Modified</code>
     *   </li>
     *   <li>
     *     <code>400 - Bad Request</code>
     *   </li>
     *   <li>
     *     <code>401 - Unauthorized</code>
     *   </li>
     *   <li>
     *     <code>403 - Forbidden</code>
     *   </li>
     *   <li>
     *     <code>404 - Not Found</code>
     *   </li>
     *   <li>
     *     <code>405 - Method Not Allowed</code>
     *   </li>
     *   <li>
     *     <code>409 - Conflict</code>
     *   </li>
     *   <li>
     *     <code>411 - Length Required</code>
     *   </li>
     *   <li>
     *     <code>412 - Precondition Failed</code>
     *   </li>
     *   <li>
     *     <code>416 - Range Not Satisfiable</code>
     *   </li>
     *   <li>
     *     <code>500 - Internal Server Error</code>
     *   </li>
     *   <li>
     *     <code>503 - Service Unavailable</code>
     *   </li>
     * </ul>
     */
    public Integer getStatusCode() {
        return statusCode;
    }

    /**
     * A string that uniquely identifies an error condition. Returned in the &lt;Code> tag of the error XML response for
     * a corresponding <code>GetObject</code> call. Cannot be used with a successful <code>StatusCode</code> header or
     * when the transformed object is provided in the body. All error codes from S3 are sentence-cased. The regular
     * expression (regex) value is <code>"^[A-Z][a-zA-Z]+$"</code>.
     */
    public String getErrorCode() {
        return errorCode;
    }

    /**
     * Contains a generic description of the error condition. Returned in the &lt;Message> tag of the error XML response
     * for a corresponding <code>GetObject</code> call. Cannot be used with a successful <code>StatusCode</code> header
     * or when the transformed object is provided in body.
     */
    public String getErrorMessage() {
        return errorMessage;
    }

    /**
     * Indicates that a range of bytes was specified.
     */
    public String getAcceptRanges() {
        return acceptRanges;
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
     * The size of the content body in bytes.
     */
    public Long getContentLength() {
        return contentLength;
    }

    /**
     * The portion of the object returned in the response.
     */
    public String getContentRange() {
        return contentRange;
    }

    /**
     * A standard MIME type describing the format of the object data.
     */
    public String getContentType() {
        return contentType;
    }

    /**
     * This header can be used as a data integrity check to verify that the data received is the same data that was
     * originally sent. This specifies the Base64 encoded, 32-bit <code>CRC32</code> checksum of the object returned by
     * the Object Lambda function. This may not match the checksum for the object stored in Amazon S3. Amazon S3 will
     * perform validation of the checksum values only when the original <code>GetObject</code> request required checksum
     * validation. For more information about checksums, see <a href="https://docs.aws.amazon.com/AmazonS3/latest/userguide/checking-object-integrity.html">Checking object integrity</a> in the <i>Amazon S3 User
     * Guide</i>.
     *
     * <p>Only one checksum header can be specified at a time. If you supply multiple checksum headers, this request
     * will fail.
     */
    public String getChecksumcrC32() {
        return checksumcrC32;
    }

    /**
     * This header can be used as a data integrity check to verify that the data received is the same data that was
     * originally sent. This specifies the Base64 encoded, 32-bit <code>CRC32C</code> checksum of the object returned by
     * the Object Lambda function. This may not match the checksum for the object stored in Amazon S3. Amazon S3 will
     * perform validation of the checksum values only when the original <code>GetObject</code> request required checksum
     * validation. For more information about checksums, see <a href="https://docs.aws.amazon.com/AmazonS3/latest/userguide/checking-object-integrity.html">Checking object integrity</a> in the <i>Amazon S3 User
     * Guide</i>.
     *
     * <p>Only one checksum header can be specified at a time. If you supply multiple checksum headers, this request
     * will fail.
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
     * originally sent. This specifies the Base64 encoded, 160-bit <code>SHA1</code> digest of the object returned by
     * the Object Lambda function. This may not match the checksum for the object stored in Amazon S3. Amazon S3 will
     * perform validation of the checksum values only when the original <code>GetObject</code> request required checksum
     * validation. For more information about checksums, see <a href="https://docs.aws.amazon.com/AmazonS3/latest/userguide/checking-object-integrity.html">Checking object integrity</a> in the <i>Amazon S3 User
     * Guide</i>.
     *
     * <p>Only one checksum header can be specified at a time. If you supply multiple checksum headers, this request
     * will fail.
     */
    public String getChecksumshA1() {
        return checksumshA1;
    }

    /**
     * This header can be used as a data integrity check to verify that the data received is the same data that was
     * originally sent. This specifies the Base64 encoded, 256-bit <code>SHA256</code> digest of the object returned by
     * the Object Lambda function. This may not match the checksum for the object stored in Amazon S3. Amazon S3 will
     * perform validation of the checksum values only when the original <code>GetObject</code> request required checksum
     * validation. For more information about checksums, see <a href="https://docs.aws.amazon.com/AmazonS3/latest/userguide/checking-object-integrity.html">Checking object integrity</a> in the <i>Amazon S3 User
     * Guide</i>.
     *
     * <p>Only one checksum header can be specified at a time. If you supply multiple checksum headers, this request
     * will fail.
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
     * Specifies whether an object stored in Amazon S3 is (<code>true</code>) or is not (<code>false</code>) a delete
     * marker. To learn more about delete markers, see <a
     * href="https://docs.aws.amazon.com/AmazonS3/latest/userguide/DeleteMarker.html">Working with delete markers</a>.
     */
    public Boolean isDeleteMarker() {
        return deleteMarker;
    }

    /**
     * An opaque identifier assigned by a web server to a specific version of a resource found at a URL.
     */
    public String getETag() {
        return eTag;
    }

    /**
     * The date and time at which the object is no longer cacheable.
     */
    public String getExpires() {
        return expires;
    }

    /**
     * If the object expiration is configured (see PUT Bucket lifecycle), the response includes this header. It includes
     * the <code>expiry-date</code> and <code>rule-id</code> key-value pairs that provide the object expiration
     * information. The value of the <code>rule-id</code> is URL-encoded.
     */
    public String getExpiration() {
        return expiration;
    }

    /**
     * The date and time that the object was last modified.
     */
    public Instant getLastModified() {
        return lastModified;
    }

    /**
     * Set to the number of metadata entries not returned in <code>x-amz-meta</code> headers. This can happen if you
     * create metadata using an API like SOAP that supports more flexible metadata than the REST API. For example, using
     * SOAP, you can create metadata whose values are not legal HTTP headers.
     */
    public Integer getMissingMeta() {
        return missingMeta;
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
     * Indicates whether an object stored in Amazon S3 has Object Lock enabled. For more information about S3 Object
     * Lock, see <a href="https://docs.aws.amazon.com/AmazonS3/latest/userguide/object-lock.html">Object Lock</a>.
     */
    public ObjectLockMode getObjectLockMode() {
        return objectLockMode;
    }

    /**
     * Indicates whether an object stored in Amazon S3 has an active legal hold.
     */
    public ObjectLockLegalHoldStatus getObjectLockLegalHoldStatus() {
        return objectLockLegalHoldStatus;
    }

    /**
     * The date and time when Object Lock is configured to expire.
     */
    public Instant getObjectLockRetainUntilDate() {
        return objectLockRetainUntilDate;
    }

    /**
     * The count of parts this object has.
     */
    public Integer getPartsCount() {
        return partsCount;
    }

    /**
     * Indicates if request involves bucket that is either a source or destination in a Replication rule. For more
     * information about S3 Replication, see <a
     * href="https://docs.aws.amazon.com/AmazonS3/latest/userguide/replication.html">Replication</a>.
     */
    public ReplicationStatus getReplicationStatus() {
        return replicationStatus;
    }

    public RequestCharged getRequestCharged() {
        return requestCharged;
    }

    /**
     * Provides information about object restoration operation and expiration time of the restored object copy.
     */
    public String getRestore() {
        return restore;
    }

    /**
     * The server-side encryption algorithm used when storing requested object in Amazon S3 or Amazon FSx.
     *
     * <p>When accessing data stored in Amazon FSx file systems using S3 access points, the only valid server side
     * encryption option is <code>aws:fsx</code>.
     */
    public ServerSideEncryption getServerSideEncryption() {
        return serverSideEncryption;
    }

    /**
     * Encryption algorithm used if server-side encryption with a customer-provided encryption key was specified for
     * object stored in Amazon S3.
     */
    public String getSseCustomerAlgorithm() {
        return sseCustomerAlgorithm;
    }

    /**
     * If present, specifies the ID (Key ID, Key ARN, or Key Alias) of the Amazon Web Services Key Management Service
     * (Amazon Web Services KMS) symmetric encryption customer managed key that was used for stored in Amazon S3 object.
     */
    public String getSsekmsKeyId() {
        return ssekmsKeyId;
    }

    /**
     * 128-bit MD5 digest of customer-provided encryption key used in Amazon S3 to encrypt data stored in S3. For more
     * information, see <a
     * href="https://docs.aws.amazon.com/AmazonS3/latest/userguide/ServerSideEncryptionCustomerKeys.html">Protecting
     * data using server-side encryption with customer-provided encryption keys (SSE-C)</a>.
     */
    public String getSseCustomerKeymD5() {
        return sseCustomerKeymD5;
    }

    /**
     * Provides storage class information of the object. Amazon S3 returns this header for all objects except for S3
     * Standard storage class objects.
     *
     * <p>For more information, see <a href="https://docs.aws.amazon.com/AmazonS3/latest/dev/storage-class-intro.html">Storage Classes</a>.
     */
    public StorageClass getStorageClass() {
        return storageClass;
    }

    /**
     * The number of tags, if any, on the object.
     */
    public Integer getTagCount() {
        return tagCount;
    }

    /**
     * An ID used to reference a specific version of the object.
     */
    public String getVersionId() {
        return versionId;
    }

    /**
     * Indicates whether the object stored in Amazon S3 uses an S3 bucket key for server-side encryption with Amazon Web
     * Services KMS (SSE-KMS).
     */
    public Boolean isBucketKeyEnabled() {
        return bucketKeyEnabled;
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
        WriteGetObjectResponseInput that = (WriteGetObjectResponseInput) other;
        return Objects.equals(this.deleteMarker, that.deleteMarker)
               && Objects.equals(this.bucketKeyEnabled, that.bucketKeyEnabled)
               && Objects.equals(this.statusCode, that.statusCode)
               && Objects.equals(this.missingMeta, that.missingMeta)
               && Objects.equals(this.partsCount, that.partsCount)
               && Objects.equals(this.tagCount, that.tagCount)
               && Objects.equals(this.contentLength, that.contentLength)
               && Objects.equals(this.requestRoute, that.requestRoute)
               && Objects.equals(this.requestToken, that.requestToken)
               && Objects.equals(this.errorCode, that.errorCode)
               && Objects.equals(this.errorMessage, that.errorMessage)
               && Objects.equals(this.acceptRanges, that.acceptRanges)
               && Objects.equals(this.cacheControl, that.cacheControl)
               && Objects.equals(this.contentDisposition, that.contentDisposition)
               && Objects.equals(this.contentEncoding, that.contentEncoding)
               && Objects.equals(this.contentLanguage, that.contentLanguage)
               && Objects.equals(this.contentRange, that.contentRange)
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
               && Objects.equals(this.eTag, that.eTag)
               && Objects.equals(this.expires, that.expires)
               && Objects.equals(this.expiration, that.expiration)
               && Objects.equals(this.restore, that.restore)
               && Objects.equals(this.sseCustomerAlgorithm, that.sseCustomerAlgorithm)
               && Objects.equals(this.ssekmsKeyId, that.ssekmsKeyId)
               && Objects.equals(this.sseCustomerKeymD5, that.sseCustomerKeymD5)
               && Objects.equals(this.versionId, that.versionId)
               && Objects.equals(this.objectLockMode, that.objectLockMode)
               && Objects.equals(this.objectLockLegalHoldStatus, that.objectLockLegalHoldStatus)
               && Objects.equals(this.replicationStatus, that.replicationStatus)
               && Objects.equals(this.requestCharged, that.requestCharged)
               && Objects.equals(this.serverSideEncryption, that.serverSideEncryption)
               && Objects.equals(this.storageClass, that.storageClass)
               && Objects.equals(this.lastModified, that.lastModified)
               && Objects.equals(this.objectLockRetainUntilDate, that.objectLockRetainUntilDate)
               && Objects.equals(this.body, that.body)
               && Objects.equals(this.metadata, that.metadata);
    }

    @Override
    public int hashCode() {
        int $hc = Objects.hashCode(requestRoute);
        $hc = 31 * $hc + Objects.hashCode(requestToken);
        $hc = 31 * $hc + Objects.hashCode(body);
        $hc = 31 * $hc + Objects.hashCode(statusCode);
        $hc = 31 * $hc + Objects.hashCode(errorCode);
        $hc = 31 * $hc + Objects.hashCode(errorMessage);
        $hc = 31 * $hc + Objects.hashCode(acceptRanges);
        $hc = 31 * $hc + Objects.hashCode(cacheControl);
        $hc = 31 * $hc + Objects.hashCode(contentDisposition);
        $hc = 31 * $hc + Objects.hashCode(contentEncoding);
        $hc = 31 * $hc + Objects.hashCode(contentLanguage);
        $hc = 31 * $hc + Objects.hashCode(contentLength);
        $hc = 31 * $hc + Objects.hashCode(contentRange);
        $hc = 31 * $hc + Objects.hashCode(contentType);
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
        $hc = 31 * $hc + Objects.hashCode(deleteMarker);
        $hc = 31 * $hc + Objects.hashCode(eTag);
        $hc = 31 * $hc + Objects.hashCode(expires);
        $hc = 31 * $hc + Objects.hashCode(expiration);
        $hc = 31 * $hc + Objects.hashCode(lastModified);
        $hc = 31 * $hc + Objects.hashCode(missingMeta);
        $hc = 31 * $hc + Objects.hashCode(metadata);
        $hc = 31 * $hc + Objects.hashCode(objectLockMode);
        $hc = 31 * $hc + Objects.hashCode(objectLockLegalHoldStatus);
        $hc = 31 * $hc + Objects.hashCode(objectLockRetainUntilDate);
        $hc = 31 * $hc + Objects.hashCode(partsCount);
        $hc = 31 * $hc + Objects.hashCode(replicationStatus);
        $hc = 31 * $hc + Objects.hashCode(requestCharged);
        $hc = 31 * $hc + Objects.hashCode(restore);
        $hc = 31 * $hc + Objects.hashCode(serverSideEncryption);
        $hc = 31 * $hc + Objects.hashCode(sseCustomerAlgorithm);
        $hc = 31 * $hc + Objects.hashCode(ssekmsKeyId);
        $hc = 31 * $hc + Objects.hashCode(sseCustomerKeymD5);
        $hc = 31 * $hc + Objects.hashCode(storageClass);
        $hc = 31 * $hc + Objects.hashCode(tagCount);
        $hc = 31 * $hc + Objects.hashCode(versionId);
        $hc = 31 * $hc + Objects.hashCode(bucketKeyEnabled);
        return $hc;
    }

    @Override
    public Schema schema() {
        return $SCHEMA;
    }

    @Override
    public void serializeMembers(ShapeSerializer serializer) {
        serializer.writeString($SCHEMA_REQUEST_ROUTE, requestRoute);
        serializer.writeString($SCHEMA_REQUEST_TOKEN, requestToken);
        serializer.writeDataStream($SCHEMA_BODY, body);
        if (statusCode != null) {
            serializer.writeInteger($SCHEMA_STATUS_CODE, statusCode);
        }
        if (errorCode != null) {
            serializer.writeString($SCHEMA_ERROR_CODE, errorCode);
        }
        if (errorMessage != null) {
            serializer.writeString($SCHEMA_ERROR_MESSAGE, errorMessage);
        }
        if (acceptRanges != null) {
            serializer.writeString($SCHEMA_ACCEPT_RANGES, acceptRanges);
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
        if (contentLength != null) {
            serializer.writeLong($SCHEMA_CONTENT_LENGTH, contentLength);
        }
        if (contentRange != null) {
            serializer.writeString($SCHEMA_CONTENT_RANGE, contentRange);
        }
        if (contentType != null) {
            serializer.writeString($SCHEMA_CONTENT_TYPE, contentType);
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
        if (deleteMarker != null) {
            serializer.writeBoolean($SCHEMA_DELETE_MARKER, deleteMarker);
        }
        if (eTag != null) {
            serializer.writeString($SCHEMA_E_TAG, eTag);
        }
        if (expires != null) {
            serializer.writeString($SCHEMA_EXPIRES, expires);
        }
        if (expiration != null) {
            serializer.writeString($SCHEMA_EXPIRATION, expiration);
        }
        if (lastModified != null) {
            serializer.writeTimestamp($SCHEMA_LAST_MODIFIED, lastModified);
        }
        if (missingMeta != null) {
            serializer.writeInteger($SCHEMA_MISSING_META, missingMeta);
        }
        if (metadata != null) {
            serializer.writeMap($SCHEMA_METADATA, metadata, metadata.size(), SharedSerde.MetadataSerializer.INSTANCE);
        }
        if (objectLockMode != null) {
            serializer.writeString($SCHEMA_OBJECT_LOCK_MODE, objectLockMode.getValue());
        }
        if (objectLockLegalHoldStatus != null) {
            serializer.writeString($SCHEMA_OBJECT_LOCK_LEGAL_HOLD_STATUS, objectLockLegalHoldStatus.getValue());
        }
        if (objectLockRetainUntilDate != null) {
            serializer.writeTimestamp($SCHEMA_OBJECT_LOCK_RETAIN_UNTIL_DATE, objectLockRetainUntilDate);
        }
        if (partsCount != null) {
            serializer.writeInteger($SCHEMA_PARTS_COUNT, partsCount);
        }
        if (replicationStatus != null) {
            serializer.writeString($SCHEMA_REPLICATION_STATUS, replicationStatus.getValue());
        }
        if (requestCharged != null) {
            serializer.writeString($SCHEMA_REQUEST_CHARGED, requestCharged.getValue());
        }
        if (restore != null) {
            serializer.writeString($SCHEMA_RESTORE, restore);
        }
        if (serverSideEncryption != null) {
            serializer.writeString($SCHEMA_SERVER_SIDE_ENCRYPTION, serverSideEncryption.getValue());
        }
        if (sseCustomerAlgorithm != null) {
            serializer.writeString($SCHEMA_SSE_CUSTOMER_ALGORITHM, sseCustomerAlgorithm);
        }
        if (ssekmsKeyId != null) {
            serializer.writeString($SCHEMA_SSEKMS_KEY_ID, ssekmsKeyId);
        }
        if (sseCustomerKeymD5 != null) {
            serializer.writeString($SCHEMA_SSE_CUSTOMER_KEYM_D5, sseCustomerKeymD5);
        }
        if (storageClass != null) {
            serializer.writeString($SCHEMA_STORAGE_CLASS, storageClass.getValue());
        }
        if (tagCount != null) {
            serializer.writeInteger($SCHEMA_TAG_COUNT, tagCount);
        }
        if (versionId != null) {
            serializer.writeString($SCHEMA_VERSION_ID, versionId);
        }
        if (bucketKeyEnabled != null) {
            serializer.writeBoolean($SCHEMA_BUCKET_KEY_ENABLED, bucketKeyEnabled);
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T getMemberValue(Schema member) {
        return switch (member.memberIndex()) {
            case 0 -> (T) SchemaUtils.validateSameMember($SCHEMA_REQUEST_ROUTE, member, requestRoute);
            case 1 -> (T) SchemaUtils.validateSameMember($SCHEMA_REQUEST_TOKEN, member, requestToken);
            case 2 -> (T) SchemaUtils.validateSameMember($SCHEMA_BODY, member, body);
            case 3 -> (T) SchemaUtils.validateSameMember($SCHEMA_STATUS_CODE, member, statusCode);
            case 4 -> (T) SchemaUtils.validateSameMember($SCHEMA_ERROR_CODE, member, errorCode);
            case 5 -> (T) SchemaUtils.validateSameMember($SCHEMA_ERROR_MESSAGE, member, errorMessage);
            case 6 -> (T) SchemaUtils.validateSameMember($SCHEMA_ACCEPT_RANGES, member, acceptRanges);
            case 7 -> (T) SchemaUtils.validateSameMember($SCHEMA_CACHE_CONTROL, member, cacheControl);
            case 8 -> (T) SchemaUtils.validateSameMember($SCHEMA_CONTENT_DISPOSITION, member, contentDisposition);
            case 9 -> (T) SchemaUtils.validateSameMember($SCHEMA_CONTENT_ENCODING, member, contentEncoding);
            case 10 -> (T) SchemaUtils.validateSameMember($SCHEMA_CONTENT_LANGUAGE, member, contentLanguage);
            case 11 -> (T) SchemaUtils.validateSameMember($SCHEMA_CONTENT_LENGTH, member, contentLength);
            case 12 -> (T) SchemaUtils.validateSameMember($SCHEMA_CONTENT_RANGE, member, contentRange);
            case 13 -> (T) SchemaUtils.validateSameMember($SCHEMA_CONTENT_TYPE, member, contentType);
            case 14 -> (T) SchemaUtils.validateSameMember($SCHEMA_CHECKSUMCR_C32, member, checksumcrC32);
            case 15 -> (T) SchemaUtils.validateSameMember($SCHEMA_CHECKSUMCR_C32_C, member, checksumcrC32C);
            case 16 -> (T) SchemaUtils.validateSameMember($SCHEMA_CHECKSUMCR_C64NVM_E, member, checksumcrC64nvmE);
            case 17 -> (T) SchemaUtils.validateSameMember($SCHEMA_CHECKSUMSH_A1, member, checksumshA1);
            case 18 -> (T) SchemaUtils.validateSameMember($SCHEMA_CHECKSUMSH_A256, member, checksumshA256);
            case 19 -> (T) SchemaUtils.validateSameMember($SCHEMA_CHECKSUMSH_A512, member, checksumshA512);
            case 20 -> (T) SchemaUtils.validateSameMember($SCHEMA_CHECKSUMM_D5, member, checksummD5);
            case 21 -> (T) SchemaUtils.validateSameMember($SCHEMA_CHECKSUMXXHAS_H64, member, checksumxxhasH64);
            case 22 -> (T) SchemaUtils.validateSameMember($SCHEMA_CHECKSUMXXHAS_H3, member, checksumxxhasH3);
            case 23 -> (T) SchemaUtils.validateSameMember($SCHEMA_CHECKSUMXXHAS_H128, member, checksumxxhasH128);
            case 24 -> (T) SchemaUtils.validateSameMember($SCHEMA_DELETE_MARKER, member, deleteMarker);
            case 25 -> (T) SchemaUtils.validateSameMember($SCHEMA_E_TAG, member, eTag);
            case 26 -> (T) SchemaUtils.validateSameMember($SCHEMA_EXPIRES, member, expires);
            case 27 -> (T) SchemaUtils.validateSameMember($SCHEMA_EXPIRATION, member, expiration);
            case 28 -> (T) SchemaUtils.validateSameMember($SCHEMA_LAST_MODIFIED, member, lastModified);
            case 29 -> (T) SchemaUtils.validateSameMember($SCHEMA_MISSING_META, member, missingMeta);
            case 30 -> (T) SchemaUtils.validateSameMember($SCHEMA_METADATA, member, metadata);
            case 31 -> (T) SchemaUtils.validateSameMember($SCHEMA_OBJECT_LOCK_MODE, member, objectLockMode);
            case 32 -> (T) SchemaUtils.validateSameMember($SCHEMA_OBJECT_LOCK_LEGAL_HOLD_STATUS, member, objectLockLegalHoldStatus);
            case 33 -> (T) SchemaUtils.validateSameMember($SCHEMA_OBJECT_LOCK_RETAIN_UNTIL_DATE, member, objectLockRetainUntilDate);
            case 34 -> (T) SchemaUtils.validateSameMember($SCHEMA_PARTS_COUNT, member, partsCount);
            case 35 -> (T) SchemaUtils.validateSameMember($SCHEMA_REPLICATION_STATUS, member, replicationStatus);
            case 36 -> (T) SchemaUtils.validateSameMember($SCHEMA_REQUEST_CHARGED, member, requestCharged);
            case 37 -> (T) SchemaUtils.validateSameMember($SCHEMA_RESTORE, member, restore);
            case 38 -> (T) SchemaUtils.validateSameMember($SCHEMA_SERVER_SIDE_ENCRYPTION, member, serverSideEncryption);
            case 39 -> (T) SchemaUtils.validateSameMember($SCHEMA_SSE_CUSTOMER_ALGORITHM, member, sseCustomerAlgorithm);
            case 40 -> (T) SchemaUtils.validateSameMember($SCHEMA_SSEKMS_KEY_ID, member, ssekmsKeyId);
            case 41 -> (T) SchemaUtils.validateSameMember($SCHEMA_SSE_CUSTOMER_KEYM_D5, member, sseCustomerKeymD5);
            case 42 -> (T) SchemaUtils.validateSameMember($SCHEMA_STORAGE_CLASS, member, storageClass);
            case 43 -> (T) SchemaUtils.validateSameMember($SCHEMA_TAG_COUNT, member, tagCount);
            case 44 -> (T) SchemaUtils.validateSameMember($SCHEMA_VERSION_ID, member, versionId);
            case 45 -> (T) SchemaUtils.validateSameMember($SCHEMA_BUCKET_KEY_ENABLED, member, bucketKeyEnabled);
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
     * @return a builder for {@link WriteGetObjectResponseInput}.
     */
    public Builder toBuilder() {
        var builder = new Builder();
        builder.requestRoute(this.requestRoute);
        builder.requestToken(this.requestToken);
        builder.body(this.body);
        builder.statusCode(this.statusCode);
        builder.errorCode(this.errorCode);
        builder.errorMessage(this.errorMessage);
        builder.acceptRanges(this.acceptRanges);
        builder.cacheControl(this.cacheControl);
        builder.contentDisposition(this.contentDisposition);
        builder.contentEncoding(this.contentEncoding);
        builder.contentLanguage(this.contentLanguage);
        builder.contentLength(this.contentLength);
        builder.contentRange(this.contentRange);
        builder.contentType(this.contentType);
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
        builder.deleteMarker(this.deleteMarker);
        builder.eTag(this.eTag);
        builder.expires(this.expires);
        builder.expiration(this.expiration);
        builder.lastModified(this.lastModified);
        builder.missingMeta(this.missingMeta);
        builder.metadata(this.metadata);
        builder.objectLockMode(this.objectLockMode);
        builder.objectLockLegalHoldStatus(this.objectLockLegalHoldStatus);
        builder.objectLockRetainUntilDate(this.objectLockRetainUntilDate);
        builder.partsCount(this.partsCount);
        builder.replicationStatus(this.replicationStatus);
        builder.requestCharged(this.requestCharged);
        builder.restore(this.restore);
        builder.serverSideEncryption(this.serverSideEncryption);
        builder.sseCustomerAlgorithm(this.sseCustomerAlgorithm);
        builder.ssekmsKeyId(this.ssekmsKeyId);
        builder.sseCustomerKeymD5(this.sseCustomerKeymD5);
        builder.storageClass(this.storageClass);
        builder.tagCount(this.tagCount);
        builder.versionId(this.versionId);
        builder.bucketKeyEnabled(this.bucketKeyEnabled);
        return builder;
    }

    /**
     * @return returns a new Builder.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for {@link WriteGetObjectResponseInput}.
     */
    public static final class Builder implements ShapeBuilder<WriteGetObjectResponseInput> {
        private final PresenceTracker tracker = PresenceTracker.of($SCHEMA);
        private String requestRoute;
        private String requestToken;
        private DataStream body = DataStream.ofEmpty();
        private Integer statusCode;
        private String errorCode;
        private String errorMessage;
        private String acceptRanges;
        private String cacheControl;
        private String contentDisposition;
        private String contentEncoding;
        private String contentLanguage;
        private Long contentLength;
        private String contentRange;
        private String contentType;
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
        private Boolean deleteMarker;
        private String eTag;
        private String expires;
        private String expiration;
        private Instant lastModified;
        private Integer missingMeta;
        private Map<String, String> metadata;
        private ObjectLockMode objectLockMode;
        private ObjectLockLegalHoldStatus objectLockLegalHoldStatus;
        private Instant objectLockRetainUntilDate;
        private Integer partsCount;
        private ReplicationStatus replicationStatus;
        private RequestCharged requestCharged;
        private String restore;
        private ServerSideEncryption serverSideEncryption;
        private String sseCustomerAlgorithm;
        private String ssekmsKeyId;
        private String sseCustomerKeymD5;
        private StorageClass storageClass;
        private Integer tagCount;
        private String versionId;
        private Boolean bucketKeyEnabled;

        private Builder() {}

        @Override
        public Schema schema() {
            return $SCHEMA;
        }

        /**
         * Route prefix to the HTTP URL generated.
         *
         * <p><strong>Required</strong>
         * @return this builder.
         */
        public Builder requestRoute(String requestRoute) {
            this.requestRoute = Objects.requireNonNull(requestRoute, "requestRoute cannot be null");
            tracker.setMember($SCHEMA_REQUEST_ROUTE);
            return this;
        }

        /**
         * A single use encrypted token that maps <code>WriteGetObjectResponse</code> to the end user <code>GetObject</code>
         * request.
         *
         * <p><strong>Required</strong>
         * @return this builder.
         */
        public Builder requestToken(String requestToken) {
            this.requestToken = Objects.requireNonNull(requestToken, "requestToken cannot be null");
            tracker.setMember($SCHEMA_REQUEST_TOKEN);
            return this;
        }

        /**
         * The object data.
         *
         * @return this builder.
         */
        public Builder body(DataStream body) {
            this.body = Objects.requireNonNull(body, "body cannot be null");
            return this;
        }

        /**
         * The integer status code for an HTTP response of a corresponding <code>GetObject</code> request. The following is
         * a list of status codes.
         *
         * <ul>
         *   <li>
         *     <code>200 - OK</code>
         *   </li>
         *   <li>
         *     <code>206 - Partial Content</code>
         *   </li>
         *   <li>
         *     <code>304 - Not Modified</code>
         *   </li>
         *   <li>
         *     <code>400 - Bad Request</code>
         *   </li>
         *   <li>
         *     <code>401 - Unauthorized</code>
         *   </li>
         *   <li>
         *     <code>403 - Forbidden</code>
         *   </li>
         *   <li>
         *     <code>404 - Not Found</code>
         *   </li>
         *   <li>
         *     <code>405 - Method Not Allowed</code>
         *   </li>
         *   <li>
         *     <code>409 - Conflict</code>
         *   </li>
         *   <li>
         *     <code>411 - Length Required</code>
         *   </li>
         *   <li>
         *     <code>412 - Precondition Failed</code>
         *   </li>
         *   <li>
         *     <code>416 - Range Not Satisfiable</code>
         *   </li>
         *   <li>
         *     <code>500 - Internal Server Error</code>
         *   </li>
         *   <li>
         *     <code>503 - Service Unavailable</code>
         *   </li>
         * </ul>
         *
         * @return this builder.
         */
        public Builder statusCode(Integer statusCode) {
            this.statusCode = statusCode;
            return this;
        }

        /**
         * A string that uniquely identifies an error condition. Returned in the &lt;Code> tag of the error XML response for
         * a corresponding <code>GetObject</code> call. Cannot be used with a successful <code>StatusCode</code> header or
         * when the transformed object is provided in the body. All error codes from S3 are sentence-cased. The regular
         * expression (regex) value is <code>"^[A-Z][a-zA-Z]+$"</code>.
         *
         * @return this builder.
         */
        public Builder errorCode(String errorCode) {
            this.errorCode = errorCode;
            return this;
        }

        /**
         * Contains a generic description of the error condition. Returned in the &lt;Message> tag of the error XML response
         * for a corresponding <code>GetObject</code> call. Cannot be used with a successful <code>StatusCode</code> header
         * or when the transformed object is provided in body.
         *
         * @return this builder.
         */
        public Builder errorMessage(String errorMessage) {
            this.errorMessage = errorMessage;
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
         * The size of the content body in bytes.
         *
         * @return this builder.
         */
        public Builder contentLength(Long contentLength) {
            this.contentLength = contentLength;
            return this;
        }

        /**
         * The portion of the object returned in the response.
         *
         * @return this builder.
         */
        public Builder contentRange(String contentRange) {
            this.contentRange = contentRange;
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
         * This header can be used as a data integrity check to verify that the data received is the same data that was
         * originally sent. This specifies the Base64 encoded, 32-bit <code>CRC32</code> checksum of the object returned by
         * the Object Lambda function. This may not match the checksum for the object stored in Amazon S3. Amazon S3 will
         * perform validation of the checksum values only when the original <code>GetObject</code> request required checksum
         * validation. For more information about checksums, see <a href="https://docs.aws.amazon.com/AmazonS3/latest/userguide/checking-object-integrity.html">Checking object integrity</a> in the <i>Amazon S3 User
         * Guide</i>.
         *
         * <p>Only one checksum header can be specified at a time. If you supply multiple checksum headers, this request
         * will fail.
         *
         * @return this builder.
         */
        public Builder checksumcrC32(String checksumcrC32) {
            this.checksumcrC32 = checksumcrC32;
            return this;
        }

        /**
         * This header can be used as a data integrity check to verify that the data received is the same data that was
         * originally sent. This specifies the Base64 encoded, 32-bit <code>CRC32C</code> checksum of the object returned by
         * the Object Lambda function. This may not match the checksum for the object stored in Amazon S3. Amazon S3 will
         * perform validation of the checksum values only when the original <code>GetObject</code> request required checksum
         * validation. For more information about checksums, see <a href="https://docs.aws.amazon.com/AmazonS3/latest/userguide/checking-object-integrity.html">Checking object integrity</a> in the <i>Amazon S3 User
         * Guide</i>.
         *
         * <p>Only one checksum header can be specified at a time. If you supply multiple checksum headers, this request
         * will fail.
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
         * originally sent. This specifies the Base64 encoded, 160-bit <code>SHA1</code> digest of the object returned by
         * the Object Lambda function. This may not match the checksum for the object stored in Amazon S3. Amazon S3 will
         * perform validation of the checksum values only when the original <code>GetObject</code> request required checksum
         * validation. For more information about checksums, see <a href="https://docs.aws.amazon.com/AmazonS3/latest/userguide/checking-object-integrity.html">Checking object integrity</a> in the <i>Amazon S3 User
         * Guide</i>.
         *
         * <p>Only one checksum header can be specified at a time. If you supply multiple checksum headers, this request
         * will fail.
         *
         * @return this builder.
         */
        public Builder checksumshA1(String checksumshA1) {
            this.checksumshA1 = checksumshA1;
            return this;
        }

        /**
         * This header can be used as a data integrity check to verify that the data received is the same data that was
         * originally sent. This specifies the Base64 encoded, 256-bit <code>SHA256</code> digest of the object returned by
         * the Object Lambda function. This may not match the checksum for the object stored in Amazon S3. Amazon S3 will
         * perform validation of the checksum values only when the original <code>GetObject</code> request required checksum
         * validation. For more information about checksums, see <a href="https://docs.aws.amazon.com/AmazonS3/latest/userguide/checking-object-integrity.html">Checking object integrity</a> in the <i>Amazon S3 User
         * Guide</i>.
         *
         * <p>Only one checksum header can be specified at a time. If you supply multiple checksum headers, this request
         * will fail.
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
         * Specifies whether an object stored in Amazon S3 is (<code>true</code>) or is not (<code>false</code>) a delete
         * marker. To learn more about delete markers, see <a
         * href="https://docs.aws.amazon.com/AmazonS3/latest/userguide/DeleteMarker.html">Working with delete markers</a>.
         *
         * @return this builder.
         */
        public Builder deleteMarker(Boolean deleteMarker) {
            this.deleteMarker = deleteMarker;
            return this;
        }

        /**
         * An opaque identifier assigned by a web server to a specific version of a resource found at a URL.
         *
         * @return this builder.
         */
        public Builder eTag(String eTag) {
            this.eTag = eTag;
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
         * If the object expiration is configured (see PUT Bucket lifecycle), the response includes this header. It includes
         * the <code>expiry-date</code> and <code>rule-id</code> key-value pairs that provide the object expiration
         * information. The value of the <code>rule-id</code> is URL-encoded.
         *
         * @return this builder.
         */
        public Builder expiration(String expiration) {
            this.expiration = expiration;
            return this;
        }

        /**
         * The date and time that the object was last modified.
         *
         * @return this builder.
         */
        public Builder lastModified(Instant lastModified) {
            this.lastModified = lastModified;
            return this;
        }

        /**
         * Set to the number of metadata entries not returned in <code>x-amz-meta</code> headers. This can happen if you
         * create metadata using an API like SOAP that supports more flexible metadata than the REST API. For example, using
         * SOAP, you can create metadata whose values are not legal HTTP headers.
         *
         * @return this builder.
         */
        public Builder missingMeta(Integer missingMeta) {
            this.missingMeta = missingMeta;
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
         * Indicates whether an object stored in Amazon S3 has Object Lock enabled. For more information about S3 Object
         * Lock, see <a href="https://docs.aws.amazon.com/AmazonS3/latest/userguide/object-lock.html">Object Lock</a>.
         *
         * @return this builder.
         */
        public Builder objectLockMode(ObjectLockMode objectLockMode) {
            this.objectLockMode = objectLockMode;
            return this;
        }

        /**
         * Indicates whether an object stored in Amazon S3 has an active legal hold.
         *
         * @return this builder.
         */
        public Builder objectLockLegalHoldStatus(ObjectLockLegalHoldStatus objectLockLegalHoldStatus) {
            this.objectLockLegalHoldStatus = objectLockLegalHoldStatus;
            return this;
        }

        /**
         * The date and time when Object Lock is configured to expire.
         *
         * @return this builder.
         */
        public Builder objectLockRetainUntilDate(Instant objectLockRetainUntilDate) {
            this.objectLockRetainUntilDate = objectLockRetainUntilDate;
            return this;
        }

        /**
         * The count of parts this object has.
         *
         * @return this builder.
         */
        public Builder partsCount(Integer partsCount) {
            this.partsCount = partsCount;
            return this;
        }

        /**
         * Indicates if request involves bucket that is either a source or destination in a Replication rule. For more
         * information about S3 Replication, see <a
         * href="https://docs.aws.amazon.com/AmazonS3/latest/userguide/replication.html">Replication</a>.
         *
         * @return this builder.
         */
        public Builder replicationStatus(ReplicationStatus replicationStatus) {
            this.replicationStatus = replicationStatus;
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
         * Provides information about object restoration operation and expiration time of the restored object copy.
         *
         * @return this builder.
         */
        public Builder restore(String restore) {
            this.restore = restore;
            return this;
        }

        /**
         * The server-side encryption algorithm used when storing requested object in Amazon S3 or Amazon FSx.
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
         * Encryption algorithm used if server-side encryption with a customer-provided encryption key was specified for
         * object stored in Amazon S3.
         *
         * @return this builder.
         */
        public Builder sseCustomerAlgorithm(String sseCustomerAlgorithm) {
            this.sseCustomerAlgorithm = sseCustomerAlgorithm;
            return this;
        }

        /**
         * If present, specifies the ID (Key ID, Key ARN, or Key Alias) of the Amazon Web Services Key Management Service
         * (Amazon Web Services KMS) symmetric encryption customer managed key that was used for stored in Amazon S3 object.
         *
         * @return this builder.
         */
        public Builder ssekmsKeyId(String ssekmsKeyId) {
            this.ssekmsKeyId = ssekmsKeyId;
            return this;
        }

        /**
         * 128-bit MD5 digest of customer-provided encryption key used in Amazon S3 to encrypt data stored in S3. For more
         * information, see <a
         * href="https://docs.aws.amazon.com/AmazonS3/latest/userguide/ServerSideEncryptionCustomerKeys.html">Protecting
         * data using server-side encryption with customer-provided encryption keys (SSE-C)</a>.
         *
         * @return this builder.
         */
        public Builder sseCustomerKeymD5(String sseCustomerKeymD5) {
            this.sseCustomerKeymD5 = sseCustomerKeymD5;
            return this;
        }

        /**
         * Provides storage class information of the object. Amazon S3 returns this header for all objects except for S3
         * Standard storage class objects.
         *
         * <p>For more information, see <a href="https://docs.aws.amazon.com/AmazonS3/latest/dev/storage-class-intro.html">Storage Classes</a>.
         *
         * @return this builder.
         */
        public Builder storageClass(StorageClass storageClass) {
            this.storageClass = storageClass;
            return this;
        }

        /**
         * The number of tags, if any, on the object.
         *
         * @return this builder.
         */
        public Builder tagCount(Integer tagCount) {
            this.tagCount = tagCount;
            return this;
        }

        /**
         * An ID used to reference a specific version of the object.
         *
         * @return this builder.
         */
        public Builder versionId(String versionId) {
            this.versionId = versionId;
            return this;
        }

        /**
         * Indicates whether the object stored in Amazon S3 uses an S3 bucket key for server-side encryption with Amazon Web
         * Services KMS (SSE-KMS).
         *
         * @return this builder.
         */
        public Builder bucketKeyEnabled(Boolean bucketKeyEnabled) {
            this.bucketKeyEnabled = bucketKeyEnabled;
            return this;
        }

        @Override
        public WriteGetObjectResponseInput build() {
            tracker.validate();
            return new WriteGetObjectResponseInput(this);
        }

        @Override
        @SuppressWarnings("unchecked")
        public void setMemberValue(Schema member, Object value) {
            switch (member.memberIndex()) {
                case 0 -> requestRoute((String) SchemaUtils.validateSameMember($SCHEMA_REQUEST_ROUTE, member, value));
                case 1 -> requestToken((String) SchemaUtils.validateSameMember($SCHEMA_REQUEST_TOKEN, member, value));
                case 2 -> body((DataStream) SchemaUtils.validateSameMember($SCHEMA_BODY, member, value));
                case 3 -> statusCode((Integer) SchemaUtils.validateSameMember($SCHEMA_STATUS_CODE, member, value));
                case 4 -> errorCode((String) SchemaUtils.validateSameMember($SCHEMA_ERROR_CODE, member, value));
                case 5 -> errorMessage((String) SchemaUtils.validateSameMember($SCHEMA_ERROR_MESSAGE, member, value));
                case 6 -> acceptRanges((String) SchemaUtils.validateSameMember($SCHEMA_ACCEPT_RANGES, member, value));
                case 7 -> cacheControl((String) SchemaUtils.validateSameMember($SCHEMA_CACHE_CONTROL, member, value));
                case 8 -> contentDisposition((String) SchemaUtils.validateSameMember($SCHEMA_CONTENT_DISPOSITION, member, value));
                case 9 -> contentEncoding((String) SchemaUtils.validateSameMember($SCHEMA_CONTENT_ENCODING, member, value));
                case 10 -> contentLanguage((String) SchemaUtils.validateSameMember($SCHEMA_CONTENT_LANGUAGE, member, value));
                case 11 -> contentLength((Long) SchemaUtils.validateSameMember($SCHEMA_CONTENT_LENGTH, member, value));
                case 12 -> contentRange((String) SchemaUtils.validateSameMember($SCHEMA_CONTENT_RANGE, member, value));
                case 13 -> contentType((String) SchemaUtils.validateSameMember($SCHEMA_CONTENT_TYPE, member, value));
                case 14 -> checksumcrC32((String) SchemaUtils.validateSameMember($SCHEMA_CHECKSUMCR_C32, member, value));
                case 15 -> checksumcrC32C((String) SchemaUtils.validateSameMember($SCHEMA_CHECKSUMCR_C32_C, member, value));
                case 16 -> checksumcrC64nvmE((String) SchemaUtils.validateSameMember($SCHEMA_CHECKSUMCR_C64NVM_E, member, value));
                case 17 -> checksumshA1((String) SchemaUtils.validateSameMember($SCHEMA_CHECKSUMSH_A1, member, value));
                case 18 -> checksumshA256((String) SchemaUtils.validateSameMember($SCHEMA_CHECKSUMSH_A256, member, value));
                case 19 -> checksumshA512((String) SchemaUtils.validateSameMember($SCHEMA_CHECKSUMSH_A512, member, value));
                case 20 -> checksummD5((String) SchemaUtils.validateSameMember($SCHEMA_CHECKSUMM_D5, member, value));
                case 21 -> checksumxxhasH64((String) SchemaUtils.validateSameMember($SCHEMA_CHECKSUMXXHAS_H64, member, value));
                case 22 -> checksumxxhasH3((String) SchemaUtils.validateSameMember($SCHEMA_CHECKSUMXXHAS_H3, member, value));
                case 23 -> checksumxxhasH128((String) SchemaUtils.validateSameMember($SCHEMA_CHECKSUMXXHAS_H128, member, value));
                case 24 -> deleteMarker((Boolean) SchemaUtils.validateSameMember($SCHEMA_DELETE_MARKER, member, value));
                case 25 -> eTag((String) SchemaUtils.validateSameMember($SCHEMA_E_TAG, member, value));
                case 26 -> expires((String) SchemaUtils.validateSameMember($SCHEMA_EXPIRES, member, value));
                case 27 -> expiration((String) SchemaUtils.validateSameMember($SCHEMA_EXPIRATION, member, value));
                case 28 -> lastModified((Instant) SchemaUtils.validateSameMember($SCHEMA_LAST_MODIFIED, member, value));
                case 29 -> missingMeta((Integer) SchemaUtils.validateSameMember($SCHEMA_MISSING_META, member, value));
                case 30 -> metadata((Map<String, String>) SchemaUtils.validateSameMember($SCHEMA_METADATA, member, value));
                case 31 -> objectLockMode((ObjectLockMode) SchemaUtils.validateSameMember($SCHEMA_OBJECT_LOCK_MODE, member, value));
                case 32 -> objectLockLegalHoldStatus((ObjectLockLegalHoldStatus) SchemaUtils.validateSameMember($SCHEMA_OBJECT_LOCK_LEGAL_HOLD_STATUS, member, value));
                case 33 -> objectLockRetainUntilDate((Instant) SchemaUtils.validateSameMember($SCHEMA_OBJECT_LOCK_RETAIN_UNTIL_DATE, member, value));
                case 34 -> partsCount((Integer) SchemaUtils.validateSameMember($SCHEMA_PARTS_COUNT, member, value));
                case 35 -> replicationStatus((ReplicationStatus) SchemaUtils.validateSameMember($SCHEMA_REPLICATION_STATUS, member, value));
                case 36 -> requestCharged((RequestCharged) SchemaUtils.validateSameMember($SCHEMA_REQUEST_CHARGED, member, value));
                case 37 -> restore((String) SchemaUtils.validateSameMember($SCHEMA_RESTORE, member, value));
                case 38 -> serverSideEncryption((ServerSideEncryption) SchemaUtils.validateSameMember($SCHEMA_SERVER_SIDE_ENCRYPTION, member, value));
                case 39 -> sseCustomerAlgorithm((String) SchemaUtils.validateSameMember($SCHEMA_SSE_CUSTOMER_ALGORITHM, member, value));
                case 40 -> ssekmsKeyId((String) SchemaUtils.validateSameMember($SCHEMA_SSEKMS_KEY_ID, member, value));
                case 41 -> sseCustomerKeymD5((String) SchemaUtils.validateSameMember($SCHEMA_SSE_CUSTOMER_KEYM_D5, member, value));
                case 42 -> storageClass((StorageClass) SchemaUtils.validateSameMember($SCHEMA_STORAGE_CLASS, member, value));
                case 43 -> tagCount((Integer) SchemaUtils.validateSameMember($SCHEMA_TAG_COUNT, member, value));
                case 44 -> versionId((String) SchemaUtils.validateSameMember($SCHEMA_VERSION_ID, member, value));
                case 45 -> bucketKeyEnabled((Boolean) SchemaUtils.validateSameMember($SCHEMA_BUCKET_KEY_ENABLED, member, value));
                default -> ShapeBuilder.super.setMemberValue(member, value);
            }
        }

        @Override
        public ShapeBuilder<WriteGetObjectResponseInput> errorCorrection() {
            if (tracker.allSet()) {
                return this;
            }
            if (!tracker.checkMember($SCHEMA_REQUEST_ROUTE)) {
                requestRoute("");
            }
            if (!tracker.checkMember($SCHEMA_REQUEST_TOKEN)) {
                requestToken("");
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
                    case 0 -> builder.requestRoute(de.readString(member));
                    case 1 -> builder.requestToken(de.readString(member));
                    case 2 -> builder.body(de.readDataStream(member));
                    case 3 -> builder.statusCode(de.readInteger(member));
                    case 4 -> builder.errorCode(de.readString(member));
                    case 5 -> builder.errorMessage(de.readString(member));
                    case 6 -> builder.acceptRanges(de.readString(member));
                    case 7 -> builder.cacheControl(de.readString(member));
                    case 8 -> builder.contentDisposition(de.readString(member));
                    case 9 -> builder.contentEncoding(de.readString(member));
                    case 10 -> builder.contentLanguage(de.readString(member));
                    case 11 -> builder.contentLength(de.readLong(member));
                    case 12 -> builder.contentRange(de.readString(member));
                    case 13 -> builder.contentType(de.readString(member));
                    case 14 -> builder.checksumcrC32(de.readString(member));
                    case 15 -> builder.checksumcrC32C(de.readString(member));
                    case 16 -> builder.checksumcrC64nvmE(de.readString(member));
                    case 17 -> builder.checksumshA1(de.readString(member));
                    case 18 -> builder.checksumshA256(de.readString(member));
                    case 19 -> builder.checksumshA512(de.readString(member));
                    case 20 -> builder.checksummD5(de.readString(member));
                    case 21 -> builder.checksumxxhasH64(de.readString(member));
                    case 22 -> builder.checksumxxhasH3(de.readString(member));
                    case 23 -> builder.checksumxxhasH128(de.readString(member));
                    case 24 -> builder.deleteMarker(de.readBoolean(member));
                    case 25 -> builder.eTag(de.readString(member));
                    case 26 -> builder.expires(de.readString(member));
                    case 27 -> builder.expiration(de.readString(member));
                    case 28 -> builder.lastModified(de.readTimestamp(member));
                    case 29 -> builder.missingMeta(de.readInteger(member));
                    case 30 -> builder.metadata(SharedSerde.deserializeMetadata(member, de));
                    case 31 -> builder.objectLockMode(ObjectLockMode.builder().deserializeMember(de, member).build());
                    case 32 -> builder.objectLockLegalHoldStatus(ObjectLockLegalHoldStatus.builder().deserializeMember(de, member).build());
                    case 33 -> builder.objectLockRetainUntilDate(de.readTimestamp(member));
                    case 34 -> builder.partsCount(de.readInteger(member));
                    case 35 -> builder.replicationStatus(ReplicationStatus.builder().deserializeMember(de, member).build());
                    case 36 -> builder.requestCharged(RequestCharged.builder().deserializeMember(de, member).build());
                    case 37 -> builder.restore(de.readString(member));
                    case 38 -> builder.serverSideEncryption(ServerSideEncryption.builder().deserializeMember(de, member).build());
                    case 39 -> builder.sseCustomerAlgorithm(de.readString(member));
                    case 40 -> builder.ssekmsKeyId(de.readString(member));
                    case 41 -> builder.sseCustomerKeymD5(de.readString(member));
                    case 42 -> builder.storageClass(StorageClass.builder().deserializeMember(de, member).build());
                    case 43 -> builder.tagCount(de.readInteger(member));
                    case 44 -> builder.versionId(de.readString(member));
                    case 45 -> builder.bucketKeyEnabled(de.readBoolean(member));
                    default -> throw new IllegalArgumentException("Unexpected member: " + member.memberName());
                }
            }
        }
    }
}
