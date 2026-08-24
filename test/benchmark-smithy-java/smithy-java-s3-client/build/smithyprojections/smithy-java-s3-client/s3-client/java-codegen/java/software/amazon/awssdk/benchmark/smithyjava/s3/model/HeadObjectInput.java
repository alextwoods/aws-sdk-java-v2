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
public final class HeadObjectInput implements SerializableStruct {

    public static final Schema $SCHEMA = Schemas4.HEAD_OBJECT_INPUT;
    private static final Schema $SCHEMA_BUCKET = $SCHEMA.member("Bucket");
    private static final Schema $SCHEMA_IF_MATCH = $SCHEMA.member("IfMatch");
    private static final Schema $SCHEMA_IF_MODIFIED_SINCE = $SCHEMA.member("IfModifiedSince");
    private static final Schema $SCHEMA_IF_NONE_MATCH = $SCHEMA.member("IfNoneMatch");
    private static final Schema $SCHEMA_IF_UNMODIFIED_SINCE = $SCHEMA.member("IfUnmodifiedSince");
    private static final Schema $SCHEMA_KEY = $SCHEMA.member("Key");
    private static final Schema $SCHEMA_RANGE = $SCHEMA.member("Range");
    private static final Schema $SCHEMA_RESPONSE_CACHE_CONTROL = $SCHEMA.member("ResponseCacheControl");
    private static final Schema $SCHEMA_RESPONSE_CONTENT_DISPOSITION = $SCHEMA.member("ResponseContentDisposition");
    private static final Schema $SCHEMA_RESPONSE_CONTENT_ENCODING = $SCHEMA.member("ResponseContentEncoding");
    private static final Schema $SCHEMA_RESPONSE_CONTENT_LANGUAGE = $SCHEMA.member("ResponseContentLanguage");
    private static final Schema $SCHEMA_RESPONSE_CONTENT_TYPE = $SCHEMA.member("ResponseContentType");
    private static final Schema $SCHEMA_RESPONSE_EXPIRES = $SCHEMA.member("ResponseExpires");
    private static final Schema $SCHEMA_VERSION_ID = $SCHEMA.member("VersionId");
    private static final Schema $SCHEMA_SSE_CUSTOMER_ALGORITHM = $SCHEMA.member("SSECustomerAlgorithm");
    private static final Schema $SCHEMA_SSE_CUSTOMER_KEY = $SCHEMA.member("SSECustomerKey");
    private static final Schema $SCHEMA_SSE_CUSTOMER_KEYM_D5 = $SCHEMA.member("SSECustomerKeyMD5");
    private static final Schema $SCHEMA_REQUEST_PAYER = $SCHEMA.member("RequestPayer");
    private static final Schema $SCHEMA_PART_NUMBER = $SCHEMA.member("PartNumber");
    private static final Schema $SCHEMA_EXPECTED_BUCKET_OWNER = $SCHEMA.member("ExpectedBucketOwner");
    private static final Schema $SCHEMA_CHECKSUM_MODE = $SCHEMA.member("ChecksumMode");

    public static final ShapeId $ID = $SCHEMA.id();

    private final transient String bucket;
    private final transient String ifMatch;
    private final transient Instant ifModifiedSince;
    private final transient String ifNoneMatch;
    private final transient Instant ifUnmodifiedSince;
    private final transient String key;
    private final transient String range;
    private final transient String responseCacheControl;
    private final transient String responseContentDisposition;
    private final transient String responseContentEncoding;
    private final transient String responseContentLanguage;
    private final transient String responseContentType;
    private final transient Instant responseExpires;
    private final transient String versionId;
    private final transient String sseCustomerAlgorithm;
    private final transient String sseCustomerKey;
    private final transient String sseCustomerKeymD5;
    private final transient RequestPayer requestPayer;
    private final transient Integer partNumber;
    private final transient String expectedBucketOwner;
    private final transient ChecksumMode checksumMode;

    private HeadObjectInput(Builder builder) {
        this.bucket = builder.bucket;
        this.ifMatch = builder.ifMatch;
        this.ifModifiedSince = builder.ifModifiedSince;
        this.ifNoneMatch = builder.ifNoneMatch;
        this.ifUnmodifiedSince = builder.ifUnmodifiedSince;
        this.key = builder.key;
        this.range = builder.range;
        this.responseCacheControl = builder.responseCacheControl;
        this.responseContentDisposition = builder.responseContentDisposition;
        this.responseContentEncoding = builder.responseContentEncoding;
        this.responseContentLanguage = builder.responseContentLanguage;
        this.responseContentType = builder.responseContentType;
        this.responseExpires = builder.responseExpires;
        this.versionId = builder.versionId;
        this.sseCustomerAlgorithm = builder.sseCustomerAlgorithm;
        this.sseCustomerKey = builder.sseCustomerKey;
        this.sseCustomerKeymD5 = builder.sseCustomerKeymD5;
        this.requestPayer = builder.requestPayer;
        this.partNumber = builder.partNumber;
        this.expectedBucketOwner = builder.expectedBucketOwner;
        this.checksumMode = builder.checksumMode;
    }

    /**
     * The name of the bucket that contains the object.
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
     * Return the object only if its entity tag (ETag) is the same as the one specified; otherwise, return a 412
     * (precondition failed) error.
     *
     * <p>If both of the <code>If-Match</code> and <code>If-Unmodified-Since</code> headers are present in the request
     * as follows:
     *
     * <ul>
     *   <li>
     *     <code>If-Match</code> condition evaluates to <code>true</code>, and;
     *   </li>
     *   <li>
     *     <code>If-Unmodified-Since</code> condition evaluates to <code>false</code>;
     *   </li>
     * </ul>
     *
     * <p>Then Amazon S3 returns <code>200 OK</code> and the data requested.
     *
     * <p>For more information about conditional requests, see <a href="https://tools.ietf.org/html/rfc7232">RFC 7232</a>.
     */
    public String getIfMatch() {
        return ifMatch;
    }

    /**
     * Return the object only if it has been modified since the specified time; otherwise, return a 304 (not modified)
     * error.
     *
     * <p>If both of the <code>If-None-Match</code> and <code>If-Modified-Since</code> headers are present in the
     * request as follows:
     *
     * <ul>
     *   <li>
     *     <code>If-None-Match</code> condition evaluates to <code>false</code>, and;
     *   </li>
     *   <li>
     *     <code>If-Modified-Since</code> condition evaluates to <code>true</code>;
     *   </li>
     * </ul>
     *
     * <p>Then Amazon S3 returns the <code>304 Not Modified</code> response code.
     *
     * <p>For more information about conditional requests, see <a href="https://tools.ietf.org/html/rfc7232">RFC 7232</a>.
     */
    public Instant getIfModifiedSince() {
        return ifModifiedSince;
    }

    /**
     * Return the object only if its entity tag (ETag) is different from the one specified; otherwise, return a 304 (not
     * modified) error.
     *
     * <p>If both of the <code>If-None-Match</code> and <code>If-Modified-Since</code> headers are present in the
     * request as follows:
     *
     * <ul>
     *   <li>
     *     <code>If-None-Match</code> condition evaluates to <code>false</code>, and;
     *   </li>
     *   <li>
     *     <code>If-Modified-Since</code> condition evaluates to <code>true</code>;
     *   </li>
     * </ul>
     *
     * <p>Then Amazon S3 returns the <code>304 Not Modified</code> response code.
     *
     * <p>For more information about conditional requests, see <a href="https://tools.ietf.org/html/rfc7232">RFC 7232</a>.
     */
    public String getIfNoneMatch() {
        return ifNoneMatch;
    }

    /**
     * Return the object only if it has not been modified since the specified time; otherwise, return a 412
     * (precondition failed) error.
     *
     * <p>If both of the <code>If-Match</code> and <code>If-Unmodified-Since</code> headers are present in the request
     * as follows:
     *
     * <ul>
     *   <li>
     *     <code>If-Match</code> condition evaluates to <code>true</code>, and;
     *   </li>
     *   <li>
     *     <code>If-Unmodified-Since</code> condition evaluates to <code>false</code>;
     *   </li>
     * </ul>
     *
     * <p>Then Amazon S3 returns <code>200 OK</code> and the data requested.
     *
     * <p>For more information about conditional requests, see <a href="https://tools.ietf.org/html/rfc7232">RFC 7232</a>.
     */
    public Instant getIfUnmodifiedSince() {
        return ifUnmodifiedSince;
    }

    /**
     * The object key.
     */
    public String getKey() {
        return key;
    }

    /**
     * HeadObject returns only the metadata for an object. If the Range is satisfiable, only the
     * <code>ContentLength</code> is affected in the response. If the Range is not satisfiable, S3 returns a <code>416 -
     * Requested Range Not Satisfiable</code> error.
     */
    public String getRange() {
        return range;
    }

    /**
     * Sets the <code>Cache-Control</code> header of the response.
     */
    public String getResponseCacheControl() {
        return responseCacheControl;
    }

    /**
     * Sets the <code>Content-Disposition</code> header of the response.
     */
    public String getResponseContentDisposition() {
        return responseContentDisposition;
    }

    /**
     * Sets the <code>Content-Encoding</code> header of the response.
     */
    public String getResponseContentEncoding() {
        return responseContentEncoding;
    }

    /**
     * Sets the <code>Content-Language</code> header of the response.
     */
    public String getResponseContentLanguage() {
        return responseContentLanguage;
    }

    /**
     * Sets the <code>Content-Type</code> header of the response.
     */
    public String getResponseContentType() {
        return responseContentType;
    }

    /**
     * Sets the <code>Expires</code> header of the response.
     */
    public Instant getResponseExpires() {
        return responseExpires;
    }

    /**
     * Version ID used to reference a specific version of the object.
     *
     * <p>For directory buckets in this API operation, only the <code>null</code> value of the version ID is supported.
     */
    public String getVersionId() {
        return versionId;
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
     * Part number of the object being read. This is a positive integer between 1 and 10,000. Effectively performs a
     * 'ranged' HEAD request for the part specified. Useful querying about the size of the part and the number of parts
     * in this object.
     */
    public Integer getPartNumber() {
        return partNumber;
    }

    /**
     * The account ID of the expected bucket owner. If the account ID that you provide does not match the actual owner
     * of the bucket, the request fails with the HTTP status code <code>403 Forbidden</code> (access denied).
     */
    public String getExpectedBucketOwner() {
        return expectedBucketOwner;
    }

    /**
     * To retrieve the checksum, this parameter must be enabled.
     *
     * <p><b>General purpose buckets</b> - If you enable checksum mode and the object is uploaded with a <a href="https://docs.aws.amazon.com/AmazonS3/latest/API/API_Checksum.html">checksum</a>
     * and encrypted with an Key Management Service (KMS) key, you must have permission to use the <code>kms:Decrypt</code>
     * action to retrieve the checksum.
     *
     * <p><b>Directory buckets</b> - If you enable <code>ChecksumMode</code> and the object is encrypted with Amazon Web
     * Services Key Management Service (Amazon Web Services KMS), you must also have the <code>kms:GenerateDataKey</code>
     * and <code>kms:Decrypt</code> permissions in IAM identity-based policies and KMS key policies for the KMS key to
     * retrieve the checksum of the object.
     */
    public ChecksumMode getChecksumMode() {
        return checksumMode;
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
        HeadObjectInput that = (HeadObjectInput) other;
        return Objects.equals(this.partNumber, that.partNumber)
               && Objects.equals(this.bucket, that.bucket)
               && Objects.equals(this.ifMatch, that.ifMatch)
               && Objects.equals(this.ifNoneMatch, that.ifNoneMatch)
               && Objects.equals(this.key, that.key)
               && Objects.equals(this.range, that.range)
               && Objects.equals(this.responseCacheControl, that.responseCacheControl)
               && Objects.equals(this.responseContentDisposition, that.responseContentDisposition)
               && Objects.equals(this.responseContentEncoding, that.responseContentEncoding)
               && Objects.equals(this.responseContentLanguage, that.responseContentLanguage)
               && Objects.equals(this.responseContentType, that.responseContentType)
               && Objects.equals(this.versionId, that.versionId)
               && Objects.equals(this.sseCustomerAlgorithm, that.sseCustomerAlgorithm)
               && Objects.equals(this.sseCustomerKey, that.sseCustomerKey)
               && Objects.equals(this.sseCustomerKeymD5, that.sseCustomerKeymD5)
               && Objects.equals(this.expectedBucketOwner, that.expectedBucketOwner)
               && Objects.equals(this.requestPayer, that.requestPayer)
               && Objects.equals(this.checksumMode, that.checksumMode)
               && Objects.equals(this.ifModifiedSince, that.ifModifiedSince)
               && Objects.equals(this.ifUnmodifiedSince, that.ifUnmodifiedSince)
               && Objects.equals(this.responseExpires, that.responseExpires);
    }

    @Override
    public int hashCode() {
        int $hc = Objects.hashCode(bucket);
        $hc = 31 * $hc + Objects.hashCode(ifMatch);
        $hc = 31 * $hc + Objects.hashCode(ifModifiedSince);
        $hc = 31 * $hc + Objects.hashCode(ifNoneMatch);
        $hc = 31 * $hc + Objects.hashCode(ifUnmodifiedSince);
        $hc = 31 * $hc + Objects.hashCode(key);
        $hc = 31 * $hc + Objects.hashCode(range);
        $hc = 31 * $hc + Objects.hashCode(responseCacheControl);
        $hc = 31 * $hc + Objects.hashCode(responseContentDisposition);
        $hc = 31 * $hc + Objects.hashCode(responseContentEncoding);
        $hc = 31 * $hc + Objects.hashCode(responseContentLanguage);
        $hc = 31 * $hc + Objects.hashCode(responseContentType);
        $hc = 31 * $hc + Objects.hashCode(responseExpires);
        $hc = 31 * $hc + Objects.hashCode(versionId);
        $hc = 31 * $hc + Objects.hashCode(sseCustomerAlgorithm);
        $hc = 31 * $hc + Objects.hashCode(sseCustomerKey);
        $hc = 31 * $hc + Objects.hashCode(sseCustomerKeymD5);
        $hc = 31 * $hc + Objects.hashCode(requestPayer);
        $hc = 31 * $hc + Objects.hashCode(partNumber);
        $hc = 31 * $hc + Objects.hashCode(expectedBucketOwner);
        $hc = 31 * $hc + Objects.hashCode(checksumMode);
        return $hc;
    }

    @Override
    public Schema schema() {
        return $SCHEMA;
    }

    @Override
    public void serializeMembers(ShapeSerializer serializer) {
        serializer.writeString($SCHEMA_BUCKET, bucket);
        if (ifMatch != null) {
            serializer.writeString($SCHEMA_IF_MATCH, ifMatch);
        }
        if (ifModifiedSince != null) {
            serializer.writeTimestamp($SCHEMA_IF_MODIFIED_SINCE, ifModifiedSince);
        }
        if (ifNoneMatch != null) {
            serializer.writeString($SCHEMA_IF_NONE_MATCH, ifNoneMatch);
        }
        if (ifUnmodifiedSince != null) {
            serializer.writeTimestamp($SCHEMA_IF_UNMODIFIED_SINCE, ifUnmodifiedSince);
        }
        serializer.writeString($SCHEMA_KEY, key);
        if (range != null) {
            serializer.writeString($SCHEMA_RANGE, range);
        }
        if (responseCacheControl != null) {
            serializer.writeString($SCHEMA_RESPONSE_CACHE_CONTROL, responseCacheControl);
        }
        if (responseContentDisposition != null) {
            serializer.writeString($SCHEMA_RESPONSE_CONTENT_DISPOSITION, responseContentDisposition);
        }
        if (responseContentEncoding != null) {
            serializer.writeString($SCHEMA_RESPONSE_CONTENT_ENCODING, responseContentEncoding);
        }
        if (responseContentLanguage != null) {
            serializer.writeString($SCHEMA_RESPONSE_CONTENT_LANGUAGE, responseContentLanguage);
        }
        if (responseContentType != null) {
            serializer.writeString($SCHEMA_RESPONSE_CONTENT_TYPE, responseContentType);
        }
        if (responseExpires != null) {
            serializer.writeTimestamp($SCHEMA_RESPONSE_EXPIRES, responseExpires);
        }
        if (versionId != null) {
            serializer.writeString($SCHEMA_VERSION_ID, versionId);
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
        if (requestPayer != null) {
            serializer.writeString($SCHEMA_REQUEST_PAYER, requestPayer.getValue());
        }
        if (partNumber != null) {
            serializer.writeInteger($SCHEMA_PART_NUMBER, partNumber);
        }
        if (expectedBucketOwner != null) {
            serializer.writeString($SCHEMA_EXPECTED_BUCKET_OWNER, expectedBucketOwner);
        }
        if (checksumMode != null) {
            serializer.writeString($SCHEMA_CHECKSUM_MODE, checksumMode.getValue());
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T getMemberValue(Schema member) {
        return switch (member.memberIndex()) {
            case 0 -> (T) SchemaUtils.validateSameMember($SCHEMA_BUCKET, member, bucket);
            case 1 -> (T) SchemaUtils.validateSameMember($SCHEMA_KEY, member, key);
            case 2 -> (T) SchemaUtils.validateSameMember($SCHEMA_IF_MATCH, member, ifMatch);
            case 3 -> (T) SchemaUtils.validateSameMember($SCHEMA_IF_MODIFIED_SINCE, member, ifModifiedSince);
            case 4 -> (T) SchemaUtils.validateSameMember($SCHEMA_IF_NONE_MATCH, member, ifNoneMatch);
            case 5 -> (T) SchemaUtils.validateSameMember($SCHEMA_IF_UNMODIFIED_SINCE, member, ifUnmodifiedSince);
            case 6 -> (T) SchemaUtils.validateSameMember($SCHEMA_RANGE, member, range);
            case 7 -> (T) SchemaUtils.validateSameMember($SCHEMA_RESPONSE_CACHE_CONTROL, member, responseCacheControl);
            case 8 -> (T) SchemaUtils.validateSameMember($SCHEMA_RESPONSE_CONTENT_DISPOSITION, member, responseContentDisposition);
            case 9 -> (T) SchemaUtils.validateSameMember($SCHEMA_RESPONSE_CONTENT_ENCODING, member, responseContentEncoding);
            case 10 -> (T) SchemaUtils.validateSameMember($SCHEMA_RESPONSE_CONTENT_LANGUAGE, member, responseContentLanguage);
            case 11 -> (T) SchemaUtils.validateSameMember($SCHEMA_RESPONSE_CONTENT_TYPE, member, responseContentType);
            case 12 -> (T) SchemaUtils.validateSameMember($SCHEMA_RESPONSE_EXPIRES, member, responseExpires);
            case 13 -> (T) SchemaUtils.validateSameMember($SCHEMA_VERSION_ID, member, versionId);
            case 14 -> (T) SchemaUtils.validateSameMember($SCHEMA_SSE_CUSTOMER_ALGORITHM, member, sseCustomerAlgorithm);
            case 15 -> (T) SchemaUtils.validateSameMember($SCHEMA_SSE_CUSTOMER_KEY, member, sseCustomerKey);
            case 16 -> (T) SchemaUtils.validateSameMember($SCHEMA_SSE_CUSTOMER_KEYM_D5, member, sseCustomerKeymD5);
            case 17 -> (T) SchemaUtils.validateSameMember($SCHEMA_REQUEST_PAYER, member, requestPayer);
            case 18 -> (T) SchemaUtils.validateSameMember($SCHEMA_PART_NUMBER, member, partNumber);
            case 19 -> (T) SchemaUtils.validateSameMember($SCHEMA_EXPECTED_BUCKET_OWNER, member, expectedBucketOwner);
            case 20 -> (T) SchemaUtils.validateSameMember($SCHEMA_CHECKSUM_MODE, member, checksumMode);
            default -> throw new IllegalArgumentException("Attempted to get non-existent member: " + member.id());
        };
    }

    /**
     * Create a new builder containing all the current property values of this object.
     *
     * <p><strong>Note:</strong> This method performs only a shallow copy of the original properties.
     *
     * @return a builder for {@link HeadObjectInput}.
     */
    public Builder toBuilder() {
        var builder = new Builder();
        builder.bucket(this.bucket);
        builder.ifMatch(this.ifMatch);
        builder.ifModifiedSince(this.ifModifiedSince);
        builder.ifNoneMatch(this.ifNoneMatch);
        builder.ifUnmodifiedSince(this.ifUnmodifiedSince);
        builder.key(this.key);
        builder.range(this.range);
        builder.responseCacheControl(this.responseCacheControl);
        builder.responseContentDisposition(this.responseContentDisposition);
        builder.responseContentEncoding(this.responseContentEncoding);
        builder.responseContentLanguage(this.responseContentLanguage);
        builder.responseContentType(this.responseContentType);
        builder.responseExpires(this.responseExpires);
        builder.versionId(this.versionId);
        builder.sseCustomerAlgorithm(this.sseCustomerAlgorithm);
        builder.sseCustomerKey(this.sseCustomerKey);
        builder.sseCustomerKeymD5(this.sseCustomerKeymD5);
        builder.requestPayer(this.requestPayer);
        builder.partNumber(this.partNumber);
        builder.expectedBucketOwner(this.expectedBucketOwner);
        builder.checksumMode(this.checksumMode);
        return builder;
    }

    /**
     * @return returns a new Builder.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for {@link HeadObjectInput}.
     */
    public static final class Builder implements ShapeBuilder<HeadObjectInput> {
        private final PresenceTracker tracker = PresenceTracker.of($SCHEMA);
        private String bucket;
        private String ifMatch;
        private Instant ifModifiedSince;
        private String ifNoneMatch;
        private Instant ifUnmodifiedSince;
        private String key;
        private String range;
        private String responseCacheControl;
        private String responseContentDisposition;
        private String responseContentEncoding;
        private String responseContentLanguage;
        private String responseContentType;
        private Instant responseExpires;
        private String versionId;
        private String sseCustomerAlgorithm;
        private String sseCustomerKey;
        private String sseCustomerKeymD5;
        private RequestPayer requestPayer;
        private Integer partNumber;
        private String expectedBucketOwner;
        private ChecksumMode checksumMode;

        private Builder() {}

        @Override
        public Schema schema() {
            return $SCHEMA;
        }

        /**
         * The name of the bucket that contains the object.
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
         * Return the object only if its entity tag (ETag) is the same as the one specified; otherwise, return a 412
         * (precondition failed) error.
         *
         * <p>If both of the <code>If-Match</code> and <code>If-Unmodified-Since</code> headers are present in the request
         * as follows:
         *
         * <ul>
         *   <li>
         *     <code>If-Match</code> condition evaluates to <code>true</code>, and;
         *   </li>
         *   <li>
         *     <code>If-Unmodified-Since</code> condition evaluates to <code>false</code>;
         *   </li>
         * </ul>
         *
         * <p>Then Amazon S3 returns <code>200 OK</code> and the data requested.
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
         * Return the object only if it has been modified since the specified time; otherwise, return a 304 (not modified)
         * error.
         *
         * <p>If both of the <code>If-None-Match</code> and <code>If-Modified-Since</code> headers are present in the
         * request as follows:
         *
         * <ul>
         *   <li>
         *     <code>If-None-Match</code> condition evaluates to <code>false</code>, and;
         *   </li>
         *   <li>
         *     <code>If-Modified-Since</code> condition evaluates to <code>true</code>;
         *   </li>
         * </ul>
         *
         * <p>Then Amazon S3 returns the <code>304 Not Modified</code> response code.
         *
         * <p>For more information about conditional requests, see <a href="https://tools.ietf.org/html/rfc7232">RFC 7232</a>.
         *
         * @return this builder.
         */
        public Builder ifModifiedSince(Instant ifModifiedSince) {
            this.ifModifiedSince = ifModifiedSince;
            return this;
        }

        /**
         * Return the object only if its entity tag (ETag) is different from the one specified; otherwise, return a 304 (not
         * modified) error.
         *
         * <p>If both of the <code>If-None-Match</code> and <code>If-Modified-Since</code> headers are present in the
         * request as follows:
         *
         * <ul>
         *   <li>
         *     <code>If-None-Match</code> condition evaluates to <code>false</code>, and;
         *   </li>
         *   <li>
         *     <code>If-Modified-Since</code> condition evaluates to <code>true</code>;
         *   </li>
         * </ul>
         *
         * <p>Then Amazon S3 returns the <code>304 Not Modified</code> response code.
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
         * Return the object only if it has not been modified since the specified time; otherwise, return a 412
         * (precondition failed) error.
         *
         * <p>If both of the <code>If-Match</code> and <code>If-Unmodified-Since</code> headers are present in the request
         * as follows:
         *
         * <ul>
         *   <li>
         *     <code>If-Match</code> condition evaluates to <code>true</code>, and;
         *   </li>
         *   <li>
         *     <code>If-Unmodified-Since</code> condition evaluates to <code>false</code>;
         *   </li>
         * </ul>
         *
         * <p>Then Amazon S3 returns <code>200 OK</code> and the data requested.
         *
         * <p>For more information about conditional requests, see <a href="https://tools.ietf.org/html/rfc7232">RFC 7232</a>.
         *
         * @return this builder.
         */
        public Builder ifUnmodifiedSince(Instant ifUnmodifiedSince) {
            this.ifUnmodifiedSince = ifUnmodifiedSince;
            return this;
        }

        /**
         * The object key.
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
         * HeadObject returns only the metadata for an object. If the Range is satisfiable, only the
         * <code>ContentLength</code> is affected in the response. If the Range is not satisfiable, S3 returns a <code>416 -
         * Requested Range Not Satisfiable</code> error.
         *
         * @return this builder.
         */
        public Builder range(String range) {
            this.range = range;
            return this;
        }

        /**
         * Sets the <code>Cache-Control</code> header of the response.
         *
         * @return this builder.
         */
        public Builder responseCacheControl(String responseCacheControl) {
            this.responseCacheControl = responseCacheControl;
            return this;
        }

        /**
         * Sets the <code>Content-Disposition</code> header of the response.
         *
         * @return this builder.
         */
        public Builder responseContentDisposition(String responseContentDisposition) {
            this.responseContentDisposition = responseContentDisposition;
            return this;
        }

        /**
         * Sets the <code>Content-Encoding</code> header of the response.
         *
         * @return this builder.
         */
        public Builder responseContentEncoding(String responseContentEncoding) {
            this.responseContentEncoding = responseContentEncoding;
            return this;
        }

        /**
         * Sets the <code>Content-Language</code> header of the response.
         *
         * @return this builder.
         */
        public Builder responseContentLanguage(String responseContentLanguage) {
            this.responseContentLanguage = responseContentLanguage;
            return this;
        }

        /**
         * Sets the <code>Content-Type</code> header of the response.
         *
         * @return this builder.
         */
        public Builder responseContentType(String responseContentType) {
            this.responseContentType = responseContentType;
            return this;
        }

        /**
         * Sets the <code>Expires</code> header of the response.
         *
         * @return this builder.
         */
        public Builder responseExpires(Instant responseExpires) {
            this.responseExpires = responseExpires;
            return this;
        }

        /**
         * Version ID used to reference a specific version of the object.
         *
         * <p>For directory buckets in this API operation, only the <code>null</code> value of the version ID is supported.
         *
         * @return this builder.
         */
        public Builder versionId(String versionId) {
            this.versionId = versionId;
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
         * Part number of the object being read. This is a positive integer between 1 and 10,000. Effectively performs a
         * 'ranged' HEAD request for the part specified. Useful querying about the size of the part and the number of parts
         * in this object.
         *
         * @return this builder.
         */
        public Builder partNumber(Integer partNumber) {
            this.partNumber = partNumber;
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
         * To retrieve the checksum, this parameter must be enabled.
         *
         * <p><b>General purpose buckets</b> - If you enable checksum mode and the object is uploaded with a <a href="https://docs.aws.amazon.com/AmazonS3/latest/API/API_Checksum.html">checksum</a>
         * and encrypted with an Key Management Service (KMS) key, you must have permission to use the <code>kms:Decrypt</code>
         * action to retrieve the checksum.
         *
         * <p><b>Directory buckets</b> - If you enable <code>ChecksumMode</code> and the object is encrypted with Amazon Web
         * Services Key Management Service (Amazon Web Services KMS), you must also have the <code>kms:GenerateDataKey</code>
         * and <code>kms:Decrypt</code> permissions in IAM identity-based policies and KMS key policies for the KMS key to
         * retrieve the checksum of the object.
         *
         * @return this builder.
         */
        public Builder checksumMode(ChecksumMode checksumMode) {
            this.checksumMode = checksumMode;
            return this;
        }

        @Override
        public HeadObjectInput build() {
            tracker.validate();
            return new HeadObjectInput(this);
        }

        @Override
        @SuppressWarnings("unchecked")
        public void setMemberValue(Schema member, Object value) {
            switch (member.memberIndex()) {
                case 0 -> bucket((String) SchemaUtils.validateSameMember($SCHEMA_BUCKET, member, value));
                case 1 -> key((String) SchemaUtils.validateSameMember($SCHEMA_KEY, member, value));
                case 2 -> ifMatch((String) SchemaUtils.validateSameMember($SCHEMA_IF_MATCH, member, value));
                case 3 -> ifModifiedSince((Instant) SchemaUtils.validateSameMember($SCHEMA_IF_MODIFIED_SINCE, member, value));
                case 4 -> ifNoneMatch((String) SchemaUtils.validateSameMember($SCHEMA_IF_NONE_MATCH, member, value));
                case 5 -> ifUnmodifiedSince((Instant) SchemaUtils.validateSameMember($SCHEMA_IF_UNMODIFIED_SINCE, member, value));
                case 6 -> range((String) SchemaUtils.validateSameMember($SCHEMA_RANGE, member, value));
                case 7 -> responseCacheControl((String) SchemaUtils.validateSameMember($SCHEMA_RESPONSE_CACHE_CONTROL, member, value));
                case 8 -> responseContentDisposition((String) SchemaUtils.validateSameMember($SCHEMA_RESPONSE_CONTENT_DISPOSITION, member, value));
                case 9 -> responseContentEncoding((String) SchemaUtils.validateSameMember($SCHEMA_RESPONSE_CONTENT_ENCODING, member, value));
                case 10 -> responseContentLanguage((String) SchemaUtils.validateSameMember($SCHEMA_RESPONSE_CONTENT_LANGUAGE, member, value));
                case 11 -> responseContentType((String) SchemaUtils.validateSameMember($SCHEMA_RESPONSE_CONTENT_TYPE, member, value));
                case 12 -> responseExpires((Instant) SchemaUtils.validateSameMember($SCHEMA_RESPONSE_EXPIRES, member, value));
                case 13 -> versionId((String) SchemaUtils.validateSameMember($SCHEMA_VERSION_ID, member, value));
                case 14 -> sseCustomerAlgorithm((String) SchemaUtils.validateSameMember($SCHEMA_SSE_CUSTOMER_ALGORITHM, member, value));
                case 15 -> sseCustomerKey((String) SchemaUtils.validateSameMember($SCHEMA_SSE_CUSTOMER_KEY, member, value));
                case 16 -> sseCustomerKeymD5((String) SchemaUtils.validateSameMember($SCHEMA_SSE_CUSTOMER_KEYM_D5, member, value));
                case 17 -> requestPayer((RequestPayer) SchemaUtils.validateSameMember($SCHEMA_REQUEST_PAYER, member, value));
                case 18 -> partNumber((Integer) SchemaUtils.validateSameMember($SCHEMA_PART_NUMBER, member, value));
                case 19 -> expectedBucketOwner((String) SchemaUtils.validateSameMember($SCHEMA_EXPECTED_BUCKET_OWNER, member, value));
                case 20 -> checksumMode((ChecksumMode) SchemaUtils.validateSameMember($SCHEMA_CHECKSUM_MODE, member, value));
                default -> ShapeBuilder.super.setMemberValue(member, value);
            }
        }

        @Override
        public ShapeBuilder<HeadObjectInput> errorCorrection() {
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
                    case 2 -> builder.ifMatch(de.readString(member));
                    case 3 -> builder.ifModifiedSince(de.readTimestamp(member));
                    case 4 -> builder.ifNoneMatch(de.readString(member));
                    case 5 -> builder.ifUnmodifiedSince(de.readTimestamp(member));
                    case 6 -> builder.range(de.readString(member));
                    case 7 -> builder.responseCacheControl(de.readString(member));
                    case 8 -> builder.responseContentDisposition(de.readString(member));
                    case 9 -> builder.responseContentEncoding(de.readString(member));
                    case 10 -> builder.responseContentLanguage(de.readString(member));
                    case 11 -> builder.responseContentType(de.readString(member));
                    case 12 -> builder.responseExpires(de.readTimestamp(member));
                    case 13 -> builder.versionId(de.readString(member));
                    case 14 -> builder.sseCustomerAlgorithm(de.readString(member));
                    case 15 -> builder.sseCustomerKey(de.readString(member));
                    case 16 -> builder.sseCustomerKeymD5(de.readString(member));
                    case 17 -> builder.requestPayer(RequestPayer.builder().deserializeMember(de, member).build());
                    case 18 -> builder.partNumber(de.readInteger(member));
                    case 19 -> builder.expectedBucketOwner(de.readString(member));
                    case 20 -> builder.checksumMode(ChecksumMode.builder().deserializeMember(de, member).build());
                    default -> throw new IllegalArgumentException("Unexpected member: " + member.memberName());
                }
            }
        }
    }
}
