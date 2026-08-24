package software.amazon.awssdk.benchmark.smithyjava.s3.model;

import software.amazon.smithy.java.core.schema.Schema;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.model.traits.DefaultTrait;
import software.amazon.smithy.model.traits.HostLabelTrait;
import software.amazon.smithy.model.traits.HttpHeaderTrait;
import software.amazon.smithy.model.traits.HttpLabelTrait;
import software.amazon.smithy.model.traits.HttpPayloadTrait;
import software.amazon.smithy.model.traits.HttpPrefixHeadersTrait;
import software.amazon.smithy.model.traits.HttpQueryTrait;
import software.amazon.smithy.model.traits.RequiredTrait;
import software.amazon.smithy.model.traits.UnitTypeTrait;
import software.amazon.smithy.model.traits.XmlNameTrait;
import software.amazon.smithy.rulesengine.traits.ContextParamTrait;

/**
 * Defines schemas for shapes in the model package.
 */
final class Schemas7 {
    static final Schema SSEKMS_ENCRYPTION = Schema.structureBuilder(ShapeId.from("com.amazonaws.s3#SSEKMSEncryption"),
            new XmlNameTrait("SSE-KMS"))
             .putMember("KMSKeyArn", Schemas6.NON_EMPTY_KMS_KEY_ARN_STRING,
                     new RequiredTrait())
             .putMember("BucketKeyEnabled", Schemas.BUCKET_KEY_ENABLED)
             .builderSupplier(SSEKMSEncryption::builder)
             .shapeClass(SSEKMSEncryption.class)
             .build();

    static final Schema OBJECT_ENCRYPTION = Schema.unionBuilder(ShapeId.from("com.amazonaws.s3#ObjectEncryption"))
             .putMember("SSEKMS", Schemas7.SSEKMS_ENCRYPTION,
                     new XmlNameTrait("SSE-KMS"))
             .builderSupplier(ObjectEncryption::builder)
             .shapeClass(ObjectEncryption.class)
             .build();

    static final Schema UPDATE_OBJECT_ENCRYPTION_INPUT = Schema.structureBuilder(ShapeId.from("com.amazonaws.s3#UpdateObjectEncryptionRequest"))
             .putMember("Bucket", Schemas.BUCKET_NAME,
                     new ContextParamTrait.Provider().createTrait(
                         ShapeId.from("smithy.rules#contextParam"),
                         Node.objectNodeBuilder()
                             .withMember("name", "Bucket")
                             .build()
                     ),
                     new RequiredTrait(),
                     new HttpLabelTrait())
             .putMember("Key", Schemas.OBJECT_KEY,
                     new RequiredTrait(),
                     new HttpLabelTrait())
             .putMember("VersionId", Schemas.OBJECT_VERSION_ID,
                     new HttpQueryTrait("versionId"))
             .putMember("ObjectEncryption", Schemas7.OBJECT_ENCRYPTION,
                     new RequiredTrait(),
                     new HttpPayloadTrait())
             .putMember("RequestPayer", RequestPayer.$SCHEMA,
                     new HttpHeaderTrait("x-amz-request-payer"))
             .putMember("ExpectedBucketOwner", Schemas.ACCOUNT_ID,
                     new HttpHeaderTrait("x-amz-expected-bucket-owner"))
             .putMember("ContentMD5", Schemas.CONTENT_MD5,
                     new HttpHeaderTrait("Content-MD5"))
             .putMember("ChecksumAlgorithm", ChecksumAlgorithm.$SCHEMA,
                     new HttpHeaderTrait("x-amz-sdk-checksum-algorithm"))
             .builderSupplier(UpdateObjectEncryptionInput::builder)
             .shapeClass(UpdateObjectEncryptionInput.class)
             .build();

    static final Schema UPDATE_OBJECT_ENCRYPTION_OUTPUT = Schema.structureBuilder(ShapeId.from("com.amazonaws.s3#UpdateObjectEncryptionResponse"))
             .putMember("RequestCharged", RequestCharged.$SCHEMA,
                     new HttpHeaderTrait("x-amz-request-charged"))
             .builderSupplier(UpdateObjectEncryptionOutput::builder)
             .shapeClass(UpdateObjectEncryptionOutput.class)
             .build();

    static final Schema UPLOAD_PART_INPUT = Schema.structureBuilder(ShapeId.from("com.amazonaws.s3#UploadPartRequest"))
             .putMember("Body", Schemas3.STREAMING_BLOB,
                     new DefaultTrait(Node.from("")),
                     new HttpPayloadTrait())
             .putMember("Bucket", Schemas.BUCKET_NAME,
                     new ContextParamTrait.Provider().createTrait(
                         ShapeId.from("smithy.rules#contextParam"),
                         Node.objectNodeBuilder()
                             .withMember("name", "Bucket")
                             .build()
                     ),
                     new RequiredTrait(),
                     new HttpLabelTrait())
             .putMember("ContentLength", Schemas3.CONTENT_LENGTH,
                     new HttpHeaderTrait("Content-Length"))
             .putMember("ContentMD5", Schemas.CONTENT_MD5,
                     new HttpHeaderTrait("Content-MD5"))
             .putMember("ChecksumAlgorithm", ChecksumAlgorithm.$SCHEMA,
                     new HttpHeaderTrait("x-amz-sdk-checksum-algorithm"))
             .putMember("ChecksumCRC32", Schemas.CHECKSUM_CRC32,
                     new HttpHeaderTrait("x-amz-checksum-crc32"))
             .putMember("ChecksumCRC32C", Schemas.CHECKSUM_CRC32_C,
                     new HttpHeaderTrait("x-amz-checksum-crc32c"))
             .putMember("ChecksumCRC64NVME", Schemas.CHECKSUM_CRC64_NVME,
                     new HttpHeaderTrait("x-amz-checksum-crc64nvme"))
             .putMember("ChecksumSHA1", Schemas.CHECKSUM_SHA1,
                     new HttpHeaderTrait("x-amz-checksum-sha1"))
             .putMember("ChecksumSHA256", Schemas.CHECKSUM_SHA256,
                     new HttpHeaderTrait("x-amz-checksum-sha256"))
             .putMember("ChecksumSHA512", Schemas.CHECKSUM_SHA512,
                     new HttpHeaderTrait("x-amz-checksum-sha512"))
             .putMember("ChecksumMD5", Schemas.CHECKSUM_MD5,
                     new HttpHeaderTrait("x-amz-checksum-md5"))
             .putMember("ChecksumXXHASH64", Schemas.CHECKSUM_XXHASH64,
                     new HttpHeaderTrait("x-amz-checksum-xxhash64"))
             .putMember("ChecksumXXHASH3", Schemas.CHECKSUM_XXHASH3,
                     new HttpHeaderTrait("x-amz-checksum-xxhash3"))
             .putMember("ChecksumXXHASH128", Schemas.CHECKSUM_XXHASH128,
                     new HttpHeaderTrait("x-amz-checksum-xxhash128"))
             .putMember("Key", Schemas.OBJECT_KEY,
                     new ContextParamTrait.Provider().createTrait(
                         ShapeId.from("smithy.rules#contextParam"),
                         Node.objectNodeBuilder()
                             .withMember("name", "Key")
                             .build()
                     ),
                     new RequiredTrait(),
                     new HttpLabelTrait())
             .putMember("PartNumber", Schemas.PART_NUMBER,
                     new RequiredTrait(),
                     new HttpQueryTrait("partNumber"))
             .putMember("UploadId", Schemas.MULTIPART_UPLOAD_ID,
                     new RequiredTrait(),
                     new HttpQueryTrait("uploadId"))
             .putMember("SSECustomerAlgorithm", Schemas.SSE_CUSTOMER_ALGORITHM,
                     new HttpHeaderTrait("x-amz-server-side-encryption-customer-algorithm"))
             .putMember("SSECustomerKey", Schemas.SSE_CUSTOMER_KEY,
                     new HttpHeaderTrait("x-amz-server-side-encryption-customer-key"))
             .putMember("SSECustomerKeyMD5", Schemas.SSE_CUSTOMER_KEY_MD5,
                     new HttpHeaderTrait("x-amz-server-side-encryption-customer-key-MD5"))
             .putMember("RequestPayer", RequestPayer.$SCHEMA,
                     new HttpHeaderTrait("x-amz-request-payer"))
             .putMember("ExpectedBucketOwner", Schemas.ACCOUNT_ID,
                     new HttpHeaderTrait("x-amz-expected-bucket-owner"))
             .builderSupplier(UploadPartInput::builder)
             .shapeClass(UploadPartInput.class)
             .build();

    static final Schema UPLOAD_PART_OUTPUT = Schema.structureBuilder(ShapeId.from("com.amazonaws.s3#UploadPartOutput"))
             .putMember("ServerSideEncryption", ServerSideEncryption.$SCHEMA,
                     new HttpHeaderTrait("x-amz-server-side-encryption"))
             .putMember("ETag", Schemas.E_TAG,
                     new HttpHeaderTrait("ETag"))
             .putMember("ChecksumCRC32", Schemas.CHECKSUM_CRC32,
                     new HttpHeaderTrait("x-amz-checksum-crc32"))
             .putMember("ChecksumCRC32C", Schemas.CHECKSUM_CRC32_C,
                     new HttpHeaderTrait("x-amz-checksum-crc32c"))
             .putMember("ChecksumCRC64NVME", Schemas.CHECKSUM_CRC64_NVME,
                     new HttpHeaderTrait("x-amz-checksum-crc64nvme"))
             .putMember("ChecksumSHA1", Schemas.CHECKSUM_SHA1,
                     new HttpHeaderTrait("x-amz-checksum-sha1"))
             .putMember("ChecksumSHA256", Schemas.CHECKSUM_SHA256,
                     new HttpHeaderTrait("x-amz-checksum-sha256"))
             .putMember("ChecksumSHA512", Schemas.CHECKSUM_SHA512,
                     new HttpHeaderTrait("x-amz-checksum-sha512"))
             .putMember("ChecksumMD5", Schemas.CHECKSUM_MD5,
                     new HttpHeaderTrait("x-amz-checksum-md5"))
             .putMember("ChecksumXXHASH64", Schemas.CHECKSUM_XXHASH64,
                     new HttpHeaderTrait("x-amz-checksum-xxhash64"))
             .putMember("ChecksumXXHASH3", Schemas.CHECKSUM_XXHASH3,
                     new HttpHeaderTrait("x-amz-checksum-xxhash3"))
             .putMember("ChecksumXXHASH128", Schemas.CHECKSUM_XXHASH128,
                     new HttpHeaderTrait("x-amz-checksum-xxhash128"))
             .putMember("SSECustomerAlgorithm", Schemas.SSE_CUSTOMER_ALGORITHM,
                     new HttpHeaderTrait("x-amz-server-side-encryption-customer-algorithm"))
             .putMember("SSECustomerKeyMD5", Schemas.SSE_CUSTOMER_KEY_MD5,
                     new HttpHeaderTrait("x-amz-server-side-encryption-customer-key-MD5"))
             .putMember("SSEKMSKeyId", Schemas.SSEKMS_KEY_ID,
                     new HttpHeaderTrait("x-amz-server-side-encryption-aws-kms-key-id"))
             .putMember("BucketKeyEnabled", Schemas.BUCKET_KEY_ENABLED,
                     new HttpHeaderTrait("x-amz-server-side-encryption-bucket-key-enabled"))
             .putMember("RequestCharged", RequestCharged.$SCHEMA,
                     new HttpHeaderTrait("x-amz-request-charged"))
             .builderSupplier(UploadPartOutput::builder)
             .shapeClass(UploadPartOutput.class)
             .build();

    static final Schema COPY_SOURCE_RANGE = Schema.createString(ShapeId.from("com.amazonaws.s3#CopySourceRange"));
    static final Schema UPLOAD_PART_COPY_INPUT = Schema.structureBuilder(ShapeId.from("com.amazonaws.s3#UploadPartCopyRequest"))
             .putMember("Bucket", Schemas.BUCKET_NAME,
                     new ContextParamTrait.Provider().createTrait(
                         ShapeId.from("smithy.rules#contextParam"),
                         Node.objectNodeBuilder()
                             .withMember("name", "Bucket")
                             .build()
                     ),
                     new RequiredTrait(),
                     new HttpLabelTrait())
             .putMember("CopySource", Schemas.COPY_SOURCE,
                     new HttpHeaderTrait("x-amz-copy-source"),
                     new RequiredTrait())
             .putMember("CopySourceIfMatch", Schemas.COPY_SOURCE_IF_MATCH,
                     new HttpHeaderTrait("x-amz-copy-source-if-match"))
             .putMember("CopySourceIfModifiedSince", Schemas.COPY_SOURCE_IF_MODIFIED_SINCE,
                     new HttpHeaderTrait("x-amz-copy-source-if-modified-since"))
             .putMember("CopySourceIfNoneMatch", Schemas.COPY_SOURCE_IF_NONE_MATCH,
                     new HttpHeaderTrait("x-amz-copy-source-if-none-match"))
             .putMember("CopySourceIfUnmodifiedSince", Schemas.COPY_SOURCE_IF_UNMODIFIED_SINCE,
                     new HttpHeaderTrait("x-amz-copy-source-if-unmodified-since"))
             .putMember("CopySourceRange", Schemas7.COPY_SOURCE_RANGE,
                     new HttpHeaderTrait("x-amz-copy-source-range"))
             .putMember("Key", Schemas.OBJECT_KEY,
                     new RequiredTrait(),
                     new HttpLabelTrait())
             .putMember("PartNumber", Schemas.PART_NUMBER,
                     new RequiredTrait(),
                     new HttpQueryTrait("partNumber"))
             .putMember("UploadId", Schemas.MULTIPART_UPLOAD_ID,
                     new RequiredTrait(),
                     new HttpQueryTrait("uploadId"))
             .putMember("SSECustomerAlgorithm", Schemas.SSE_CUSTOMER_ALGORITHM,
                     new HttpHeaderTrait("x-amz-server-side-encryption-customer-algorithm"))
             .putMember("SSECustomerKey", Schemas.SSE_CUSTOMER_KEY,
                     new HttpHeaderTrait("x-amz-server-side-encryption-customer-key"))
             .putMember("SSECustomerKeyMD5", Schemas.SSE_CUSTOMER_KEY_MD5,
                     new HttpHeaderTrait("x-amz-server-side-encryption-customer-key-MD5"))
             .putMember("CopySourceSSECustomerAlgorithm", Schemas.COPY_SOURCE_SSE_CUSTOMER_ALGORITHM,
                     new HttpHeaderTrait("x-amz-copy-source-server-side-encryption-customer-algorithm"))
             .putMember("CopySourceSSECustomerKey", Schemas.COPY_SOURCE_SSE_CUSTOMER_KEY,
                     new HttpHeaderTrait("x-amz-copy-source-server-side-encryption-customer-key"))
             .putMember("CopySourceSSECustomerKeyMD5", Schemas.COPY_SOURCE_SSE_CUSTOMER_KEY_MD5,
                     new HttpHeaderTrait("x-amz-copy-source-server-side-encryption-customer-key-MD5"))
             .putMember("RequestPayer", RequestPayer.$SCHEMA,
                     new HttpHeaderTrait("x-amz-request-payer"))
             .putMember("ExpectedBucketOwner", Schemas.ACCOUNT_ID,
                     new HttpHeaderTrait("x-amz-expected-bucket-owner"))
             .putMember("ExpectedSourceBucketOwner", Schemas.ACCOUNT_ID,
                     new HttpHeaderTrait("x-amz-source-expected-bucket-owner"))
             .builderSupplier(UploadPartCopyInput::builder)
             .shapeClass(UploadPartCopyInput.class)
             .build();

    static final Schema COPY_PART_RESULT = Schema.structureBuilder(ShapeId.from("com.amazonaws.s3#CopyPartResult"))
             .putMember("ETag", Schemas.E_TAG)
             .putMember("LastModified", Schemas.LAST_MODIFIED)
             .putMember("ChecksumCRC32", Schemas.CHECKSUM_CRC32)
             .putMember("ChecksumCRC32C", Schemas.CHECKSUM_CRC32_C)
             .putMember("ChecksumCRC64NVME", Schemas.CHECKSUM_CRC64_NVME)
             .putMember("ChecksumSHA1", Schemas.CHECKSUM_SHA1)
             .putMember("ChecksumSHA256", Schemas.CHECKSUM_SHA256)
             .putMember("ChecksumSHA512", Schemas.CHECKSUM_SHA512)
             .putMember("ChecksumMD5", Schemas.CHECKSUM_MD5)
             .putMember("ChecksumXXHASH64", Schemas.CHECKSUM_XXHASH64)
             .putMember("ChecksumXXHASH3", Schemas.CHECKSUM_XXHASH3)
             .putMember("ChecksumXXHASH128", Schemas.CHECKSUM_XXHASH128)
             .builderSupplier(CopyPartResult::builder)
             .shapeClass(CopyPartResult.class)
             .build();

    static final Schema UPLOAD_PART_COPY_OUTPUT = Schema.structureBuilder(ShapeId.from("com.amazonaws.s3#UploadPartCopyOutput"))
             .putMember("CopySourceVersionId", Schemas.COPY_SOURCE_VERSION_ID,
                     new HttpHeaderTrait("x-amz-copy-source-version-id"))
             .putMember("CopyPartResult", Schemas7.COPY_PART_RESULT,
                     new HttpPayloadTrait())
             .putMember("ServerSideEncryption", ServerSideEncryption.$SCHEMA,
                     new HttpHeaderTrait("x-amz-server-side-encryption"))
             .putMember("SSECustomerAlgorithm", Schemas.SSE_CUSTOMER_ALGORITHM,
                     new HttpHeaderTrait("x-amz-server-side-encryption-customer-algorithm"))
             .putMember("SSECustomerKeyMD5", Schemas.SSE_CUSTOMER_KEY_MD5,
                     new HttpHeaderTrait("x-amz-server-side-encryption-customer-key-MD5"))
             .putMember("SSEKMSKeyId", Schemas.SSEKMS_KEY_ID,
                     new HttpHeaderTrait("x-amz-server-side-encryption-aws-kms-key-id"))
             .putMember("BucketKeyEnabled", Schemas.BUCKET_KEY_ENABLED,
                     new HttpHeaderTrait("x-amz-server-side-encryption-bucket-key-enabled"))
             .putMember("RequestCharged", RequestCharged.$SCHEMA,
                     new HttpHeaderTrait("x-amz-request-charged"))
             .builderSupplier(UploadPartCopyOutput::builder)
             .shapeClass(UploadPartCopyOutput.class)
             .build();

    static final Schema REQUEST_ROUTE = Schema.createString(ShapeId.from("com.amazonaws.s3#RequestRoute"));
    static final Schema REQUEST_TOKEN = Schema.createString(ShapeId.from("com.amazonaws.s3#RequestToken"));
    static final Schema GET_OBJECT_RESPONSE_STATUS_CODE = Schema.createInteger(ShapeId.from("com.amazonaws.s3#GetObjectResponseStatusCode"));
    static final Schema WRITE_GET_OBJECT_RESPONSE_INPUT = Schema.structureBuilder(ShapeId.from("com.amazonaws.s3#WriteGetObjectResponseRequest"))
             .putMember("RequestRoute", Schemas7.REQUEST_ROUTE,
                     new HostLabelTrait(),
                     new HttpHeaderTrait("x-amz-request-route"),
                     new RequiredTrait())
             .putMember("RequestToken", Schemas7.REQUEST_TOKEN,
                     new HttpHeaderTrait("x-amz-request-token"),
                     new RequiredTrait())
             .putMember("Body", Schemas3.STREAMING_BLOB,
                     new DefaultTrait(Node.from("")),
                     new HttpPayloadTrait())
             .putMember("StatusCode", Schemas7.GET_OBJECT_RESPONSE_STATUS_CODE,
                     new HttpHeaderTrait("x-amz-fwd-status"))
             .putMember("ErrorCode", Schemas2.ERROR_CODE,
                     new HttpHeaderTrait("x-amz-fwd-error-code"))
             .putMember("ErrorMessage", Schemas2.ERROR_MESSAGE,
                     new HttpHeaderTrait("x-amz-fwd-error-message"))
             .putMember("AcceptRanges", Schemas.ACCEPT_RANGES,
                     new HttpHeaderTrait("x-amz-fwd-header-accept-ranges"))
             .putMember("CacheControl", Schemas.CACHE_CONTROL,
                     new HttpHeaderTrait("x-amz-fwd-header-Cache-Control"))
             .putMember("ContentDisposition", Schemas.CONTENT_DISPOSITION,
                     new HttpHeaderTrait("x-amz-fwd-header-Content-Disposition"))
             .putMember("ContentEncoding", Schemas.CONTENT_ENCODING,
                     new HttpHeaderTrait("x-amz-fwd-header-Content-Encoding"))
             .putMember("ContentLanguage", Schemas.CONTENT_LANGUAGE,
                     new HttpHeaderTrait("x-amz-fwd-header-Content-Language"))
             .putMember("ContentLength", Schemas3.CONTENT_LENGTH,
                     new HttpHeaderTrait("Content-Length"))
             .putMember("ContentRange", Schemas3.CONTENT_RANGE,
                     new HttpHeaderTrait("x-amz-fwd-header-Content-Range"))
             .putMember("ContentType", Schemas.CONTENT_TYPE,
                     new HttpHeaderTrait("x-amz-fwd-header-Content-Type"))
             .putMember("ChecksumCRC32", Schemas.CHECKSUM_CRC32,
                     new HttpHeaderTrait("x-amz-fwd-header-x-amz-checksum-crc32"))
             .putMember("ChecksumCRC32C", Schemas.CHECKSUM_CRC32_C,
                     new HttpHeaderTrait("x-amz-fwd-header-x-amz-checksum-crc32c"))
             .putMember("ChecksumCRC64NVME", Schemas.CHECKSUM_CRC64_NVME,
                     new HttpHeaderTrait("x-amz-fwd-header-x-amz-checksum-crc64nvme"))
             .putMember("ChecksumSHA1", Schemas.CHECKSUM_SHA1,
                     new HttpHeaderTrait("x-amz-fwd-header-x-amz-checksum-sha1"))
             .putMember("ChecksumSHA256", Schemas.CHECKSUM_SHA256,
                     new HttpHeaderTrait("x-amz-fwd-header-x-amz-checksum-sha256"))
             .putMember("ChecksumSHA512", Schemas.CHECKSUM_SHA512,
                     new HttpHeaderTrait("x-amz-fwd-header-x-amz-checksum-sha512"))
             .putMember("ChecksumMD5", Schemas.CHECKSUM_MD5,
                     new HttpHeaderTrait("x-amz-fwd-header-x-amz-checksum-md5"))
             .putMember("ChecksumXXHASH64", Schemas.CHECKSUM_XXHASH64,
                     new HttpHeaderTrait("x-amz-fwd-header-x-amz-checksum-xxhash64"))
             .putMember("ChecksumXXHASH3", Schemas.CHECKSUM_XXHASH3,
                     new HttpHeaderTrait("x-amz-fwd-header-x-amz-checksum-xxhash3"))
             .putMember("ChecksumXXHASH128", Schemas.CHECKSUM_XXHASH128,
                     new HttpHeaderTrait("x-amz-fwd-header-x-amz-checksum-xxhash128"))
             .putMember("DeleteMarker", Schemas1.DELETE_MARKER,
                     new HttpHeaderTrait("x-amz-fwd-header-x-amz-delete-marker"))
             .putMember("ETag", Schemas.E_TAG,
                     new HttpHeaderTrait("x-amz-fwd-header-ETag"))
             .putMember("Expires", Schemas.EXPIRES,
                     new HttpHeaderTrait("x-amz-fwd-header-Expires"))
             .putMember("Expiration", Schemas.EXPIRATION,
                     new HttpHeaderTrait("x-amz-fwd-header-x-amz-expiration"))
             .putMember("LastModified", Schemas.LAST_MODIFIED,
                     new HttpHeaderTrait("x-amz-fwd-header-Last-Modified"))
             .putMember("MissingMeta", Schemas3.MISSING_META,
                     new HttpHeaderTrait("x-amz-fwd-header-x-amz-missing-meta"))
             .putMember("Metadata", Schemas.METADATA,
                     new HttpPrefixHeadersTrait("x-amz-meta-"))
             .putMember("ObjectLockMode", ObjectLockMode.$SCHEMA,
                     new HttpHeaderTrait("x-amz-fwd-header-x-amz-object-lock-mode"))
             .putMember("ObjectLockLegalHoldStatus", ObjectLockLegalHoldStatus.$SCHEMA,
                     new HttpHeaderTrait("x-amz-fwd-header-x-amz-object-lock-legal-hold"))
             .putMember("ObjectLockRetainUntilDate", Schemas.OBJECT_LOCK_RETAIN_UNTIL_DATE,
                     new HttpHeaderTrait("x-amz-fwd-header-x-amz-object-lock-retain-until-date"))
             .putMember("PartsCount", Schemas3.PARTS_COUNT,
                     new HttpHeaderTrait("x-amz-fwd-header-x-amz-mp-parts-count"))
             .putMember("ReplicationStatus", ReplicationStatus.$SCHEMA,
                     new HttpHeaderTrait("x-amz-fwd-header-x-amz-replication-status"))
             .putMember("RequestCharged", RequestCharged.$SCHEMA,
                     new HttpHeaderTrait("x-amz-fwd-header-x-amz-request-charged"))
             .putMember("Restore", Schemas3.RESTORE,
                     new HttpHeaderTrait("x-amz-fwd-header-x-amz-restore"))
             .putMember("ServerSideEncryption", ServerSideEncryption.$SCHEMA,
                     new HttpHeaderTrait("x-amz-fwd-header-x-amz-server-side-encryption"))
             .putMember("SSECustomerAlgorithm", Schemas.SSE_CUSTOMER_ALGORITHM,
                     new HttpHeaderTrait("x-amz-fwd-header-x-amz-server-side-encryption-customer-algorithm"))
             .putMember("SSEKMSKeyId", Schemas.SSEKMS_KEY_ID,
                     new HttpHeaderTrait("x-amz-fwd-header-x-amz-server-side-encryption-aws-kms-key-id"))
             .putMember("SSECustomerKeyMD5", Schemas.SSE_CUSTOMER_KEY_MD5,
                     new HttpHeaderTrait("x-amz-fwd-header-x-amz-server-side-encryption-customer-key-MD5"))
             .putMember("StorageClass", StorageClass.$SCHEMA,
                     new HttpHeaderTrait("x-amz-fwd-header-x-amz-storage-class"))
             .putMember("TagCount", Schemas3.TAG_COUNT,
                     new HttpHeaderTrait("x-amz-fwd-header-x-amz-tagging-count"))
             .putMember("VersionId", Schemas.OBJECT_VERSION_ID,
                     new HttpHeaderTrait("x-amz-fwd-header-x-amz-version-id"))
             .putMember("BucketKeyEnabled", Schemas.BUCKET_KEY_ENABLED,
                     new HttpHeaderTrait("x-amz-fwd-header-x-amz-server-side-encryption-bucket-key-enabled"))
             .builderSupplier(WriteGetObjectResponseInput::builder)
             .shapeClass(WriteGetObjectResponseInput.class)
             .build();

    static final Schema WRITE_GET_OBJECT_RESPONSE_OUTPUT = Schema.structureBuilder(ShapeId.from("com.amazonaws.s3#WriteGetObjectResponseOutput"),
            new UnitTypeTrait()).builderSupplier(WriteGetObjectResponseOutput::builder)
             .shapeClass(WriteGetObjectResponseOutput.class)
             .build();

    private Schemas7() {}
}
