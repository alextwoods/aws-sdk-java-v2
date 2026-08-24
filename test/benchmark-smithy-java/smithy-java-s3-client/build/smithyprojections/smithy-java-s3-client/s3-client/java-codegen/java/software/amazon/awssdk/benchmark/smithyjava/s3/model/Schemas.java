package software.amazon.awssdk.benchmark.smithyjava.s3.model;

import software.amazon.smithy.java.core.schema.Schema;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.model.traits.ErrorTrait;
import software.amazon.smithy.model.traits.HttpErrorTrait;
import software.amazon.smithy.model.traits.HttpHeaderTrait;
import software.amazon.smithy.model.traits.HttpLabelTrait;
import software.amazon.smithy.model.traits.HttpPayloadTrait;
import software.amazon.smithy.model.traits.HttpPrefixHeadersTrait;
import software.amazon.smithy.model.traits.HttpQueryTrait;
import software.amazon.smithy.model.traits.LengthTrait;
import software.amazon.smithy.model.traits.PatternTrait;
import software.amazon.smithy.model.traits.RequiredTrait;
import software.amazon.smithy.model.traits.SensitiveTrait;
import software.amazon.smithy.model.traits.TimestampFormatTrait;
import software.amazon.smithy.model.traits.UnitTypeTrait;
import software.amazon.smithy.model.traits.XmlAttributeTrait;
import software.amazon.smithy.model.traits.XmlFlattenedTrait;
import software.amazon.smithy.model.traits.XmlNameTrait;
import software.amazon.smithy.model.traits.XmlNamespaceTrait;
import software.amazon.smithy.rulesengine.traits.ContextParamTrait;

/**
 * Defines schemas for shapes in the model package.
 */
final class Schemas {
    static final Schema ABAC_STATUS = Schema.structureBuilder(ShapeId.from("com.amazonaws.s3#AbacStatus"))
             .putMember("Status", BucketAbacStatus.$SCHEMA)
             .builderSupplier(AbacStatus::builder)
             .shapeClass(AbacStatus.class)
             .build();

    static final Schema ABORT_DATE = Schema.createTimestamp(ShapeId.from("com.amazonaws.s3#AbortDate"));
    static final Schema DAYS_AFTER_INITIATION = Schema.createInteger(ShapeId.from("com.amazonaws.s3#DaysAfterInitiation"));
    static final Schema ABORT_INCOMPLETE_MULTIPART_UPLOAD = Schema.structureBuilder(ShapeId.from("com.amazonaws.s3#AbortIncompleteMultipartUpload"))
             .putMember("DaysAfterInitiation", Schemas.DAYS_AFTER_INITIATION)
             .builderSupplier(AbortIncompleteMultipartUpload::builder)
             .shapeClass(AbortIncompleteMultipartUpload.class)
             .build();

    static final Schema BUCKET_NAME = Schema.createString(ShapeId.from("com.amazonaws.s3#BucketName"));
    static final Schema ACCOUNT_ID = Schema.createString(ShapeId.from("com.amazonaws.s3#AccountId"));
    static final Schema IF_MATCH_INITIATED_TIME = Schema.createTimestamp(ShapeId.from("com.amazonaws.s3#IfMatchInitiatedTime"),
            new TimestampFormatTrait("http-date"));
    static final Schema OBJECT_KEY = Schema.createString(ShapeId.from("com.amazonaws.s3#ObjectKey"),
            LengthTrait.builder().min(1L).build());
    static final Schema MULTIPART_UPLOAD_ID = Schema.createString(ShapeId.from("com.amazonaws.s3#MultipartUploadId"));
    static final Schema ABORT_MULTIPART_UPLOAD_INPUT = Schema.structureBuilder(ShapeId.from("com.amazonaws.s3#AbortMultipartUploadRequest"))
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
                     new ContextParamTrait.Provider().createTrait(
                         ShapeId.from("smithy.rules#contextParam"),
                         Node.objectNodeBuilder()
                             .withMember("name", "Key")
                             .build()
                     ),
                     new RequiredTrait(),
                     new HttpLabelTrait())
             .putMember("UploadId", Schemas.MULTIPART_UPLOAD_ID,
                     new RequiredTrait(),
                     new HttpQueryTrait("uploadId"))
             .putMember("RequestPayer", RequestPayer.$SCHEMA,
                     new HttpHeaderTrait("x-amz-request-payer"))
             .putMember("ExpectedBucketOwner", Schemas.ACCOUNT_ID,
                     new HttpHeaderTrait("x-amz-expected-bucket-owner"))
             .putMember("IfMatchInitiatedTime", Schemas.IF_MATCH_INITIATED_TIME,
                     new HttpHeaderTrait("x-amz-if-match-initiated-time"))
             .builderSupplier(AbortMultipartUploadInput::builder)
             .shapeClass(AbortMultipartUploadInput.class)
             .build();

    static final Schema ABORT_MULTIPART_UPLOAD_OUTPUT = Schema.structureBuilder(ShapeId.from("com.amazonaws.s3#AbortMultipartUploadOutput"))
             .putMember("RequestCharged", RequestCharged.$SCHEMA,
                     new HttpHeaderTrait("x-amz-request-charged"))
             .builderSupplier(AbortMultipartUploadOutput::builder)
             .shapeClass(AbortMultipartUploadOutput.class)
             .build();

    static final Schema NO_SUCH_UPLOAD = Schema.structureBuilder(ShapeId.from("com.amazonaws.s3#NoSuchUpload"),
            new ErrorTrait("client"),
            new HttpErrorTrait(404)).builderSupplier(NoSuchUpload::builder)
             .shapeClass(NoSuchUpload.class)
             .build();

    static final Schema ABORT_RULE_ID = Schema.createString(ShapeId.from("com.amazonaws.s3#AbortRuleId"));
    static final Schema ACCELERATE_CONFIGURATION = Schema.structureBuilder(ShapeId.from("com.amazonaws.s3#AccelerateConfiguration"))
             .putMember("Status", BucketAccelerateStatus.$SCHEMA)
             .builderSupplier(AccelerateConfiguration::builder)
             .shapeClass(AccelerateConfiguration.class)
             .build();

    static final Schema ACCEPT_RANGES = Schema.createString(ShapeId.from("com.amazonaws.s3#AcceptRanges"));
    static final Schema DISPLAY_NAME = Schema.createString(ShapeId.from("com.amazonaws.s3#DisplayName"));
    static final Schema EMAIL_ADDRESS = Schema.createString(ShapeId.from("com.amazonaws.s3#EmailAddress"));
    static final Schema COM_AMAZONAWS_S3_ID = Schema.createString(ShapeId.from("com.amazonaws.s3#ID"));
    static final Schema COM_AMAZONAWS_S3_URI = Schema.createString(ShapeId.from("com.amazonaws.s3#URI"));
    static final Schema GRANTEE = Schema.structureBuilder(ShapeId.from("com.amazonaws.s3#Grantee"))
             .putMember("DisplayName", Schemas.DISPLAY_NAME)
             .putMember("EmailAddress", Schemas.EMAIL_ADDRESS)
             .putMember("ID", Schemas.COM_AMAZONAWS_S3_ID)
             .putMember("URI", Schemas.COM_AMAZONAWS_S3_URI)
             .putMember("Type", Type.$SCHEMA,
                     new XmlNameTrait("xsi:type"),
                     new RequiredTrait(),
                     new XmlAttributeTrait())
             .builderSupplier(Grantee::builder)
             .shapeClass(Grantee.class)
             .build();

    static final Schema GRANT = Schema.structureBuilder(ShapeId.from("com.amazonaws.s3#Grant"))
             .putMember("Grantee", Schemas.GRANTEE,
                     XmlNamespaceTrait.builder().uri("http://www.w3.org/2001/XMLSchema-instance").prefix("xsi").build())
             .putMember("Permission", Permission.$SCHEMA)
             .builderSupplier(Grant::builder)
             .shapeClass(Grant.class)
             .build();

    static final Schema GRANTS = Schema.listBuilder(ShapeId.from("com.amazonaws.s3#Grants"))
        .putMember("member", Schemas.GRANT,
                new XmlNameTrait("Grant"))
        .build();

    static final Schema OWNER = Schema.structureBuilder(ShapeId.from("com.amazonaws.s3#Owner"))
             .putMember("DisplayName", Schemas.DISPLAY_NAME)
             .putMember("ID", Schemas.COM_AMAZONAWS_S3_ID)
             .builderSupplier(Owner::builder)
             .shapeClass(Owner.class)
             .build();

    static final Schema ACCESS_CONTROL_POLICY = Schema.structureBuilder(ShapeId.from("com.amazonaws.s3#AccessControlPolicy"))
             .putMember("Grants", Schemas.GRANTS,
                     new XmlNameTrait("AccessControlList"))
             .putMember("Owner", Schemas.OWNER)
             .builderSupplier(AccessControlPolicy::builder)
             .shapeClass(AccessControlPolicy.class)
             .build();

    static final Schema ACCESS_CONTROL_TRANSLATION = Schema.structureBuilder(ShapeId.from("com.amazonaws.s3#AccessControlTranslation"))
             .putMember("Owner", OwnerOverride.$SCHEMA,
                     new RequiredTrait())
             .builderSupplier(AccessControlTranslation::builder)
             .shapeClass(AccessControlTranslation.class)
             .build();

    static final Schema ACCESS_DENIED = Schema.structureBuilder(ShapeId.from("com.amazonaws.s3#AccessDenied"),
            new ErrorTrait("client"),
            new HttpErrorTrait(403)).builderSupplier(AccessDenied::builder)
             .shapeClass(AccessDenied.class)
             .build();

    static final Schema ACCESS_KEY_ID_VALUE = Schema.createString(ShapeId.from("com.amazonaws.s3#AccessKeyIdValue"));
    static final Schema ACCESS_POINT_ALIAS = Schema.createBoolean(ShapeId.from("com.amazonaws.s3#AccessPointAlias"));
    static final Schema ACCESS_POINT_ARN = Schema.createString(ShapeId.from("com.amazonaws.s3#AccessPointArn"));
    static final Schema ALLOWED_HEADER = Schema.createString(ShapeId.from("com.amazonaws.s3#AllowedHeader"));
    static final Schema ALLOWED_HEADERS = Schema.listBuilder(ShapeId.from("com.amazonaws.s3#AllowedHeaders"))
        .putMember("member", Schemas.ALLOWED_HEADER)
        .build();

    static final Schema ALLOWED_METHOD = Schema.createString(ShapeId.from("com.amazonaws.s3#AllowedMethod"));
    static final Schema ALLOWED_METHODS = Schema.listBuilder(ShapeId.from("com.amazonaws.s3#AllowedMethods"))
        .putMember("member", Schemas.ALLOWED_METHOD)
        .build();

    static final Schema ALLOWED_ORIGIN = Schema.createString(ShapeId.from("com.amazonaws.s3#AllowedOrigin"));
    static final Schema ALLOWED_ORIGINS = Schema.listBuilder(ShapeId.from("com.amazonaws.s3#AllowedOrigins"))
        .putMember("member", Schemas.ALLOWED_ORIGIN)
        .build();

    static final Schema ALLOW_QUOTED_RECORD_DELIMITER = Schema.createBoolean(ShapeId.from("com.amazonaws.s3#AllowQuotedRecordDelimiter"));
    static final Schema CHECKSUM_CRC32 = Schema.createString(ShapeId.from("com.amazonaws.s3#ChecksumCRC32"));
    static final Schema CHECKSUM_CRC32_C = Schema.createString(ShapeId.from("com.amazonaws.s3#ChecksumCRC32C"));
    static final Schema CHECKSUM_CRC64_NVME = Schema.createString(ShapeId.from("com.amazonaws.s3#ChecksumCRC64NVME"));
    static final Schema CHECKSUM_MD5 = Schema.createString(ShapeId.from("com.amazonaws.s3#ChecksumMD5"));
    static final Schema CHECKSUM_SHA1 = Schema.createString(ShapeId.from("com.amazonaws.s3#ChecksumSHA1"));
    static final Schema CHECKSUM_SHA256 = Schema.createString(ShapeId.from("com.amazonaws.s3#ChecksumSHA256"));
    static final Schema CHECKSUM_SHA512 = Schema.createString(ShapeId.from("com.amazonaws.s3#ChecksumSHA512"));
    static final Schema CHECKSUM_XXHASH128 = Schema.createString(ShapeId.from("com.amazonaws.s3#ChecksumXXHASH128"));
    static final Schema CHECKSUM_XXHASH3 = Schema.createString(ShapeId.from("com.amazonaws.s3#ChecksumXXHASH3"));
    static final Schema CHECKSUM_XXHASH64 = Schema.createString(ShapeId.from("com.amazonaws.s3#ChecksumXXHASH64"));
    static final Schema IF_MATCH = Schema.createString(ShapeId.from("com.amazonaws.s3#IfMatch"));
    static final Schema IF_NONE_MATCH = Schema.createString(ShapeId.from("com.amazonaws.s3#IfNoneMatch"));
    static final Schema MPU_OBJECT_SIZE = Schema.createLong(ShapeId.from("com.amazonaws.s3#MpuObjectSize"));
    static final Schema E_TAG = Schema.createString(ShapeId.from("com.amazonaws.s3#ETag"));
    static final Schema PART_NUMBER = Schema.createInteger(ShapeId.from("com.amazonaws.s3#PartNumber"));
    static final Schema COMPLETED_PART = Schema.structureBuilder(ShapeId.from("com.amazonaws.s3#CompletedPart"))
             .putMember("ETag", Schemas.E_TAG)
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
             .putMember("PartNumber", Schemas.PART_NUMBER)
             .builderSupplier(CompletedPart::builder)
             .shapeClass(CompletedPart.class)
             .build();

    static final Schema COMPLETED_PART_LIST = Schema.listBuilder(ShapeId.from("com.amazonaws.s3#CompletedPartList"))
        .putMember("member", Schemas.COMPLETED_PART)
        .build();

    static final Schema COMPLETED_MULTIPART_UPLOAD = Schema.structureBuilder(ShapeId.from("com.amazonaws.s3#CompletedMultipartUpload"))
             .putMember("Parts", Schemas.COMPLETED_PART_LIST,
                     new XmlNameTrait("Part"),
                     new XmlFlattenedTrait())
             .builderSupplier(CompletedMultipartUpload::builder)
             .shapeClass(CompletedMultipartUpload.class)
             .build();

    static final Schema SSE_CUSTOMER_ALGORITHM = Schema.createString(ShapeId.from("com.amazonaws.s3#SSECustomerAlgorithm"));
    static final Schema SSE_CUSTOMER_KEY = Schema.createString(ShapeId.from("com.amazonaws.s3#SSECustomerKey"),
            new SensitiveTrait());
    static final Schema SSE_CUSTOMER_KEY_MD5 = Schema.createString(ShapeId.from("com.amazonaws.s3#SSECustomerKeyMD5"));
    static final Schema COMPLETE_MULTIPART_UPLOAD_INPUT = Schema.structureBuilder(ShapeId.from("com.amazonaws.s3#CompleteMultipartUploadRequest"))
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
                     new ContextParamTrait.Provider().createTrait(
                         ShapeId.from("smithy.rules#contextParam"),
                         Node.objectNodeBuilder()
                             .withMember("name", "Key")
                             .build()
                     ),
                     new RequiredTrait(),
                     new HttpLabelTrait())
             .putMember("MultipartUpload", Schemas.COMPLETED_MULTIPART_UPLOAD,
                     new XmlNameTrait("CompleteMultipartUpload"),
                     new HttpPayloadTrait())
             .putMember("UploadId", Schemas.MULTIPART_UPLOAD_ID,
                     new RequiredTrait(),
                     new HttpQueryTrait("uploadId"))
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
             .putMember("ChecksumType", ChecksumType.$SCHEMA,
                     new HttpHeaderTrait("x-amz-checksum-type"))
             .putMember("MpuObjectSize", Schemas.MPU_OBJECT_SIZE,
                     new HttpHeaderTrait("x-amz-mp-object-size"))
             .putMember("RequestPayer", RequestPayer.$SCHEMA,
                     new HttpHeaderTrait("x-amz-request-payer"))
             .putMember("ExpectedBucketOwner", Schemas.ACCOUNT_ID,
                     new HttpHeaderTrait("x-amz-expected-bucket-owner"))
             .putMember("IfMatch", Schemas.IF_MATCH,
                     new HttpHeaderTrait("If-Match"))
             .putMember("IfNoneMatch", Schemas.IF_NONE_MATCH,
                     new HttpHeaderTrait("If-None-Match"))
             .putMember("SSECustomerAlgorithm", Schemas.SSE_CUSTOMER_ALGORITHM,
                     new HttpHeaderTrait("x-amz-server-side-encryption-customer-algorithm"))
             .putMember("SSECustomerKey", Schemas.SSE_CUSTOMER_KEY,
                     new HttpHeaderTrait("x-amz-server-side-encryption-customer-key"))
             .putMember("SSECustomerKeyMD5", Schemas.SSE_CUSTOMER_KEY_MD5,
                     new HttpHeaderTrait("x-amz-server-side-encryption-customer-key-MD5"))
             .builderSupplier(CompleteMultipartUploadInput::builder)
             .shapeClass(CompleteMultipartUploadInput.class)
             .build();

    static final Schema BUCKET_KEY_ENABLED = Schema.createBoolean(ShapeId.from("com.amazonaws.s3#BucketKeyEnabled"));
    static final Schema EXPIRATION = Schema.createString(ShapeId.from("com.amazonaws.s3#Expiration"));
    static final Schema LOCATION = Schema.createString(ShapeId.from("com.amazonaws.s3#Location"));
    static final Schema SSEKMS_KEY_ID = Schema.createString(ShapeId.from("com.amazonaws.s3#SSEKMSKeyId"),
            new SensitiveTrait());
    static final Schema OBJECT_VERSION_ID = Schema.createString(ShapeId.from("com.amazonaws.s3#ObjectVersionId"));
    static final Schema COMPLETE_MULTIPART_UPLOAD_OUTPUT = Schema.structureBuilder(ShapeId.from("com.amazonaws.s3#CompleteMultipartUploadOutput"),
            new XmlNameTrait("CompleteMultipartUploadResult"))
             .putMember("Location", Schemas.LOCATION)
             .putMember("Bucket", Schemas.BUCKET_NAME)
             .putMember("Key", Schemas.OBJECT_KEY)
             .putMember("Expiration", Schemas.EXPIRATION,
                     new HttpHeaderTrait("x-amz-expiration"))
             .putMember("ETag", Schemas.E_TAG)
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
             .putMember("ChecksumType", ChecksumType.$SCHEMA)
             .putMember("ServerSideEncryption", ServerSideEncryption.$SCHEMA,
                     new HttpHeaderTrait("x-amz-server-side-encryption"))
             .putMember("VersionId", Schemas.OBJECT_VERSION_ID,
                     new HttpHeaderTrait("x-amz-version-id"))
             .putMember("SSEKMSKeyId", Schemas.SSEKMS_KEY_ID,
                     new HttpHeaderTrait("x-amz-server-side-encryption-aws-kms-key-id"))
             .putMember("BucketKeyEnabled", Schemas.BUCKET_KEY_ENABLED,
                     new HttpHeaderTrait("x-amz-server-side-encryption-bucket-key-enabled"))
             .putMember("RequestCharged", RequestCharged.$SCHEMA,
                     new HttpHeaderTrait("x-amz-request-charged"))
             .builderSupplier(CompleteMultipartUploadOutput::builder)
             .shapeClass(CompleteMultipartUploadOutput.class)
             .build();

    static final Schema CACHE_CONTROL = Schema.createString(ShapeId.from("com.amazonaws.s3#CacheControl"));
    static final Schema CONTENT_DISPOSITION = Schema.createString(ShapeId.from("com.amazonaws.s3#ContentDisposition"));
    static final Schema CONTENT_ENCODING = Schema.createString(ShapeId.from("com.amazonaws.s3#ContentEncoding"));
    static final Schema CONTENT_LANGUAGE = Schema.createString(ShapeId.from("com.amazonaws.s3#ContentLanguage"));
    static final Schema CONTENT_TYPE = Schema.createString(ShapeId.from("com.amazonaws.s3#ContentType"));
    static final Schema COPY_SOURCE = Schema.createString(ShapeId.from("com.amazonaws.s3#CopySource"),
            new PatternTrait("^\\/?.+\\/.+$"));
    static final Schema COPY_SOURCE_IF_MATCH = Schema.createString(ShapeId.from("com.amazonaws.s3#CopySourceIfMatch"));
    static final Schema COPY_SOURCE_IF_MODIFIED_SINCE = Schema.createTimestamp(ShapeId.from("com.amazonaws.s3#CopySourceIfModifiedSince"));
    static final Schema COPY_SOURCE_IF_NONE_MATCH = Schema.createString(ShapeId.from("com.amazonaws.s3#CopySourceIfNoneMatch"));
    static final Schema COPY_SOURCE_IF_UNMODIFIED_SINCE = Schema.createTimestamp(ShapeId.from("com.amazonaws.s3#CopySourceIfUnmodifiedSince"));
    static final Schema COPY_SOURCE_SSE_CUSTOMER_ALGORITHM = Schema.createString(ShapeId.from("com.amazonaws.s3#CopySourceSSECustomerAlgorithm"));
    static final Schema COPY_SOURCE_SSE_CUSTOMER_KEY = Schema.createString(ShapeId.from("com.amazonaws.s3#CopySourceSSECustomerKey"),
            new SensitiveTrait());
    static final Schema COPY_SOURCE_SSE_CUSTOMER_KEY_MD5 = Schema.createString(ShapeId.from("com.amazonaws.s3#CopySourceSSECustomerKeyMD5"));
    static final Schema EXPIRES = Schema.createString(ShapeId.from("com.amazonaws.s3#Expires"));
    static final Schema GRANT_FULL_CONTROL = Schema.createString(ShapeId.from("com.amazonaws.s3#GrantFullControl"));
    static final Schema GRANT_READ = Schema.createString(ShapeId.from("com.amazonaws.s3#GrantRead"));
    static final Schema GRANT_READ_ACP = Schema.createString(ShapeId.from("com.amazonaws.s3#GrantReadACP"));
    static final Schema GRANT_WRITE_ACP = Schema.createString(ShapeId.from("com.amazonaws.s3#GrantWriteACP"));
    static final Schema METADATA_KEY = Schema.createString(ShapeId.from("com.amazonaws.s3#MetadataKey"));
    static final Schema METADATA_VALUE = Schema.createString(ShapeId.from("com.amazonaws.s3#MetadataValue"));
    static final Schema METADATA = Schema.mapBuilder(ShapeId.from("com.amazonaws.s3#Metadata"))
        .putMember("key", Schemas.METADATA_KEY)
        .putMember("value", Schemas.METADATA_VALUE)
        .build();

    static final Schema OBJECT_LOCK_RETAIN_UNTIL_DATE = Schema.createTimestamp(ShapeId.from("com.amazonaws.s3#ObjectLockRetainUntilDate"),
            new TimestampFormatTrait("date-time"));
    static final Schema SSEKMS_ENCRYPTION_CONTEXT = Schema.createString(ShapeId.from("com.amazonaws.s3#SSEKMSEncryptionContext"),
            new SensitiveTrait());
    static final Schema TAGGING_HEADER = Schema.createString(ShapeId.from("com.amazonaws.s3#TaggingHeader"));
    static final Schema WEBSITE_REDIRECT_LOCATION = Schema.createString(ShapeId.from("com.amazonaws.s3#WebsiteRedirectLocation"));
    static final Schema COPY_OBJECT_INPUT = Schema.structureBuilder(ShapeId.from("com.amazonaws.s3#CopyObjectRequest"))
             .putMember("ACL", ObjectCannedACL.$SCHEMA,
                     new HttpHeaderTrait("x-amz-acl"))
             .putMember("Bucket", Schemas.BUCKET_NAME,
                     new ContextParamTrait.Provider().createTrait(
                         ShapeId.from("smithy.rules#contextParam"),
                         Node.objectNodeBuilder()
                             .withMember("name", "Bucket")
                             .build()
                     ),
                     new RequiredTrait(),
                     new HttpLabelTrait())
             .putMember("CacheControl", Schemas.CACHE_CONTROL,
                     new HttpHeaderTrait("Cache-Control"))
             .putMember("ChecksumAlgorithm", ChecksumAlgorithm.$SCHEMA,
                     new HttpHeaderTrait("x-amz-checksum-algorithm"))
             .putMember("ContentDisposition", Schemas.CONTENT_DISPOSITION,
                     new HttpHeaderTrait("Content-Disposition"))
             .putMember("ContentEncoding", Schemas.CONTENT_ENCODING,
                     new HttpHeaderTrait("Content-Encoding"))
             .putMember("ContentLanguage", Schemas.CONTENT_LANGUAGE,
                     new HttpHeaderTrait("Content-Language"))
             .putMember("ContentType", Schemas.CONTENT_TYPE,
                     new HttpHeaderTrait("Content-Type"))
             .putMember("CopySource", Schemas.COPY_SOURCE,
                     new HttpHeaderTrait("x-amz-copy-source"),
                     new ContextParamTrait.Provider().createTrait(
                         ShapeId.from("smithy.rules#contextParam"),
                         Node.objectNodeBuilder()
                             .withMember("name", "CopySource")
                             .build()
                     ),
                     new RequiredTrait())
             .putMember("CopySourceIfMatch", Schemas.COPY_SOURCE_IF_MATCH,
                     new HttpHeaderTrait("x-amz-copy-source-if-match"))
             .putMember("CopySourceIfModifiedSince", Schemas.COPY_SOURCE_IF_MODIFIED_SINCE,
                     new HttpHeaderTrait("x-amz-copy-source-if-modified-since"))
             .putMember("CopySourceIfNoneMatch", Schemas.COPY_SOURCE_IF_NONE_MATCH,
                     new HttpHeaderTrait("x-amz-copy-source-if-none-match"))
             .putMember("CopySourceIfUnmodifiedSince", Schemas.COPY_SOURCE_IF_UNMODIFIED_SINCE,
                     new HttpHeaderTrait("x-amz-copy-source-if-unmodified-since"))
             .putMember("Expires", Schemas.EXPIRES,
                     new HttpHeaderTrait("Expires"))
             .putMember("GrantFullControl", Schemas.GRANT_FULL_CONTROL,
                     new HttpHeaderTrait("x-amz-grant-full-control"))
             .putMember("GrantRead", Schemas.GRANT_READ,
                     new HttpHeaderTrait("x-amz-grant-read"))
             .putMember("GrantReadACP", Schemas.GRANT_READ_ACP,
                     new HttpHeaderTrait("x-amz-grant-read-acp"))
             .putMember("GrantWriteACP", Schemas.GRANT_WRITE_ACP,
                     new HttpHeaderTrait("x-amz-grant-write-acp"))
             .putMember("IfMatch", Schemas.IF_MATCH,
                     new HttpHeaderTrait("If-Match"))
             .putMember("IfNoneMatch", Schemas.IF_NONE_MATCH,
                     new HttpHeaderTrait("If-None-Match"))
             .putMember("Key", Schemas.OBJECT_KEY,
                     new ContextParamTrait.Provider().createTrait(
                         ShapeId.from("smithy.rules#contextParam"),
                         Node.objectNodeBuilder()
                             .withMember("name", "Key")
                             .build()
                     ),
                     new RequiredTrait(),
                     new HttpLabelTrait())
             .putMember("Metadata", Schemas.METADATA,
                     new HttpPrefixHeadersTrait("x-amz-meta-"))
             .putMember("MetadataDirective", MetadataDirective.$SCHEMA,
                     new HttpHeaderTrait("x-amz-metadata-directive"))
             .putMember("TaggingDirective", TaggingDirective.$SCHEMA,
                     new HttpHeaderTrait("x-amz-tagging-directive"))
             .putMember("AnnotationDirective", AnnotationDirective.$SCHEMA,
                     new HttpHeaderTrait("x-amz-object-annotation-directive"))
             .putMember("ServerSideEncryption", ServerSideEncryption.$SCHEMA,
                     new HttpHeaderTrait("x-amz-server-side-encryption"))
             .putMember("StorageClass", StorageClass.$SCHEMA,
                     new HttpHeaderTrait("x-amz-storage-class"))
             .putMember("WebsiteRedirectLocation", Schemas.WEBSITE_REDIRECT_LOCATION,
                     new HttpHeaderTrait("x-amz-website-redirect-location"))
             .putMember("SSECustomerAlgorithm", Schemas.SSE_CUSTOMER_ALGORITHM,
                     new HttpHeaderTrait("x-amz-server-side-encryption-customer-algorithm"))
             .putMember("SSECustomerKey", Schemas.SSE_CUSTOMER_KEY,
                     new HttpHeaderTrait("x-amz-server-side-encryption-customer-key"))
             .putMember("SSECustomerKeyMD5", Schemas.SSE_CUSTOMER_KEY_MD5,
                     new HttpHeaderTrait("x-amz-server-side-encryption-customer-key-MD5"))
             .putMember("SSEKMSKeyId", Schemas.SSEKMS_KEY_ID,
                     new HttpHeaderTrait("x-amz-server-side-encryption-aws-kms-key-id"))
             .putMember("SSEKMSEncryptionContext", Schemas.SSEKMS_ENCRYPTION_CONTEXT,
                     new HttpHeaderTrait("x-amz-server-side-encryption-context"))
             .putMember("BucketKeyEnabled", Schemas.BUCKET_KEY_ENABLED,
                     new HttpHeaderTrait("x-amz-server-side-encryption-bucket-key-enabled"))
             .putMember("CopySourceSSECustomerAlgorithm", Schemas.COPY_SOURCE_SSE_CUSTOMER_ALGORITHM,
                     new HttpHeaderTrait("x-amz-copy-source-server-side-encryption-customer-algorithm"))
             .putMember("CopySourceSSECustomerKey", Schemas.COPY_SOURCE_SSE_CUSTOMER_KEY,
                     new HttpHeaderTrait("x-amz-copy-source-server-side-encryption-customer-key"))
             .putMember("CopySourceSSECustomerKeyMD5", Schemas.COPY_SOURCE_SSE_CUSTOMER_KEY_MD5,
                     new HttpHeaderTrait("x-amz-copy-source-server-side-encryption-customer-key-MD5"))
             .putMember("RequestPayer", RequestPayer.$SCHEMA,
                     new HttpHeaderTrait("x-amz-request-payer"))
             .putMember("Tagging", Schemas.TAGGING_HEADER,
                     new HttpHeaderTrait("x-amz-tagging"))
             .putMember("ObjectLockMode", ObjectLockMode.$SCHEMA,
                     new HttpHeaderTrait("x-amz-object-lock-mode"))
             .putMember("ObjectLockRetainUntilDate", Schemas.OBJECT_LOCK_RETAIN_UNTIL_DATE,
                     new HttpHeaderTrait("x-amz-object-lock-retain-until-date"))
             .putMember("ObjectLockLegalHoldStatus", ObjectLockLegalHoldStatus.$SCHEMA,
                     new HttpHeaderTrait("x-amz-object-lock-legal-hold"))
             .putMember("ExpectedBucketOwner", Schemas.ACCOUNT_ID,
                     new HttpHeaderTrait("x-amz-expected-bucket-owner"))
             .putMember("ExpectedSourceBucketOwner", Schemas.ACCOUNT_ID,
                     new HttpHeaderTrait("x-amz-source-expected-bucket-owner"))
             .builderSupplier(CopyObjectInput::builder)
             .shapeClass(CopyObjectInput.class)
             .build();

    static final Schema LAST_MODIFIED = Schema.createTimestamp(ShapeId.from("com.amazonaws.s3#LastModified"));
    static final Schema COPY_OBJECT_RESULT = Schema.structureBuilder(ShapeId.from("com.amazonaws.s3#CopyObjectResult"))
             .putMember("ETag", Schemas.E_TAG)
             .putMember("LastModified", Schemas.LAST_MODIFIED)
             .putMember("ChecksumType", ChecksumType.$SCHEMA)
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
             .builderSupplier(CopyObjectResult::builder)
             .shapeClass(CopyObjectResult.class)
             .build();

    static final Schema COPY_SOURCE_VERSION_ID = Schema.createString(ShapeId.from("com.amazonaws.s3#CopySourceVersionId"));
    static final Schema COPY_OBJECT_OUTPUT = Schema.structureBuilder(ShapeId.from("com.amazonaws.s3#CopyObjectOutput"))
             .putMember("CopyObjectResult", Schemas.COPY_OBJECT_RESULT,
                     new HttpPayloadTrait())
             .putMember("Expiration", Schemas.EXPIRATION,
                     new HttpHeaderTrait("x-amz-expiration"))
             .putMember("CopySourceVersionId", Schemas.COPY_SOURCE_VERSION_ID,
                     new HttpHeaderTrait("x-amz-copy-source-version-id"))
             .putMember("VersionId", Schemas.OBJECT_VERSION_ID,
                     new HttpHeaderTrait("x-amz-version-id"))
             .putMember("ServerSideEncryption", ServerSideEncryption.$SCHEMA,
                     new HttpHeaderTrait("x-amz-server-side-encryption"))
             .putMember("SSECustomerAlgorithm", Schemas.SSE_CUSTOMER_ALGORITHM,
                     new HttpHeaderTrait("x-amz-server-side-encryption-customer-algorithm"))
             .putMember("SSECustomerKeyMD5", Schemas.SSE_CUSTOMER_KEY_MD5,
                     new HttpHeaderTrait("x-amz-server-side-encryption-customer-key-MD5"))
             .putMember("SSEKMSKeyId", Schemas.SSEKMS_KEY_ID,
                     new HttpHeaderTrait("x-amz-server-side-encryption-aws-kms-key-id"))
             .putMember("SSEKMSEncryptionContext", Schemas.SSEKMS_ENCRYPTION_CONTEXT,
                     new HttpHeaderTrait("x-amz-server-side-encryption-context"))
             .putMember("BucketKeyEnabled", Schemas.BUCKET_KEY_ENABLED,
                     new HttpHeaderTrait("x-amz-server-side-encryption-bucket-key-enabled"))
             .putMember("RequestCharged", RequestCharged.$SCHEMA,
                     new HttpHeaderTrait("x-amz-request-charged"))
             .builderSupplier(CopyObjectOutput::builder)
             .shapeClass(CopyObjectOutput.class)
             .build();

    static final Schema OBJECT_NOT_IN_ACTIVE_TIER_ERROR = Schema.structureBuilder(ShapeId.from("com.amazonaws.s3#ObjectNotInActiveTierError"),
            new ErrorTrait("client"),
            new HttpErrorTrait(403)).builderSupplier(ObjectNotInActiveTierError::builder)
             .shapeClass(ObjectNotInActiveTierError.class)
             .build();

    static final Schema BUCKET_ALREADY_EXISTS = Schema.structureBuilder(ShapeId.from("com.amazonaws.s3#BucketAlreadyExists"),
            new ErrorTrait("client"),
            new HttpErrorTrait(409)).builderSupplier(BucketAlreadyExists::builder)
             .shapeClass(BucketAlreadyExists.class)
             .build();

    static final Schema BUCKET_ALREADY_OWNED_BY_YOU = Schema.structureBuilder(ShapeId.from("com.amazonaws.s3#BucketAlreadyOwnedByYou"),
            new ErrorTrait("client"),
            new HttpErrorTrait(409)).builderSupplier(BucketAlreadyOwnedByYou::builder)
             .shapeClass(BucketAlreadyOwnedByYou.class)
             .build();

    static final Schema BUCKET_INFO = Schema.structureBuilder(ShapeId.from("com.amazonaws.s3#BucketInfo"))
             .putMember("DataRedundancy", DataRedundancy.$SCHEMA)
             .putMember("Type", BucketType.$SCHEMA)
             .builderSupplier(BucketInfo::builder)
             .shapeClass(BucketInfo.class)
             .build();

    static final Schema LOCATION_NAME_AS_STRING = Schema.createString(ShapeId.from("com.amazonaws.s3#LocationNameAsString"));
    static final Schema LOCATION_INFO = Schema.structureBuilder(ShapeId.from("com.amazonaws.s3#LocationInfo"))
             .putMember("Type", LocationType.$SCHEMA)
             .putMember("Name", Schemas.LOCATION_NAME_AS_STRING)
             .builderSupplier(LocationInfo::builder)
             .shapeClass(LocationInfo.class)
             .build();

    static final Schema VALUE = Schema.createString(ShapeId.from("com.amazonaws.s3#Value"));
    static final Schema TAG = Schema.structureBuilder(ShapeId.from("com.amazonaws.s3#Tag"))
             .putMember("Key", Schemas.OBJECT_KEY,
                     new RequiredTrait())
             .putMember("Value", Schemas.VALUE,
                     new RequiredTrait())
             .builderSupplier(Tag::builder)
             .shapeClass(Tag.class)
             .build();

    static final Schema TAG_SET = Schema.listBuilder(ShapeId.from("com.amazonaws.s3#TagSet"))
        .putMember("member", Schemas.TAG,
                new XmlNameTrait("Tag"))
        .build();

    static final Schema CREATE_BUCKET_CONFIGURATION = Schema.structureBuilder(ShapeId.from("com.amazonaws.s3#CreateBucketConfiguration"))
             .putMember("LocationConstraint", BucketLocationConstraint.$SCHEMA)
             .putMember("Location", Schemas.LOCATION_INFO)
             .putMember("Bucket", Schemas.BUCKET_INFO)
             .putMember("Tags", Schemas.TAG_SET)
             .builderSupplier(CreateBucketConfiguration::builder)
             .shapeClass(CreateBucketConfiguration.class)
             .build();

    static final Schema GRANT_WRITE = Schema.createString(ShapeId.from("com.amazonaws.s3#GrantWrite"));
    static final Schema OBJECT_LOCK_ENABLED_FOR_BUCKET = Schema.createBoolean(ShapeId.from("com.amazonaws.s3#ObjectLockEnabledForBucket"));
    static final Schema CREATE_BUCKET_INPUT = Schema.structureBuilder(ShapeId.from("com.amazonaws.s3#CreateBucketRequest"))
             .putMember("ACL", BucketCannedACL.$SCHEMA,
                     new HttpHeaderTrait("x-amz-acl"))
             .putMember("Bucket", Schemas.BUCKET_NAME,
                     new ContextParamTrait.Provider().createTrait(
                         ShapeId.from("smithy.rules#contextParam"),
                         Node.objectNodeBuilder()
                             .withMember("name", "Bucket")
                             .build()
                     ),
                     new RequiredTrait(),
                     new HttpLabelTrait())
             .putMember("CreateBucketConfiguration", Schemas.CREATE_BUCKET_CONFIGURATION,
                     new XmlNameTrait("CreateBucketConfiguration"),
                     new HttpPayloadTrait())
             .putMember("GrantFullControl", Schemas.GRANT_FULL_CONTROL,
                     new HttpHeaderTrait("x-amz-grant-full-control"))
             .putMember("GrantRead", Schemas.GRANT_READ,
                     new HttpHeaderTrait("x-amz-grant-read"))
             .putMember("GrantReadACP", Schemas.GRANT_READ_ACP,
                     new HttpHeaderTrait("x-amz-grant-read-acp"))
             .putMember("GrantWrite", Schemas.GRANT_WRITE,
                     new HttpHeaderTrait("x-amz-grant-write"))
             .putMember("GrantWriteACP", Schemas.GRANT_WRITE_ACP,
                     new HttpHeaderTrait("x-amz-grant-write-acp"))
             .putMember("ObjectLockEnabledForBucket", Schemas.OBJECT_LOCK_ENABLED_FOR_BUCKET,
                     new HttpHeaderTrait("x-amz-bucket-object-lock-enabled"))
             .putMember("ObjectOwnership", ObjectOwnership.$SCHEMA,
                     new HttpHeaderTrait("x-amz-object-ownership"))
             .putMember("BucketNamespace", BucketNamespace.$SCHEMA,
                     new HttpHeaderTrait("x-amz-bucket-namespace"))
             .builderSupplier(CreateBucketInput::builder)
             .shapeClass(CreateBucketInput.class)
             .build();

    static final Schema S3_REGIONAL_OR_S3_EXPRESS_BUCKET_ARN_STRING = Schema.createString(ShapeId.from("com.amazonaws.s3#S3RegionalOrS3ExpressBucketArnString"),
            LengthTrait.builder().min(1L).max(128L).build(),
            new PatternTrait("^arn:[^:]+:(s3|s3express):"));
    static final Schema CREATE_BUCKET_OUTPUT = Schema.structureBuilder(ShapeId.from("com.amazonaws.s3#CreateBucketOutput"))
             .putMember("Location", Schemas.LOCATION,
                     new HttpHeaderTrait("Location"))
             .putMember("BucketArn", Schemas.S3_REGIONAL_OR_S3_EXPRESS_BUCKET_ARN_STRING,
                     new HttpHeaderTrait("x-amz-bucket-arn"))
             .builderSupplier(CreateBucketOutput::builder)
             .shapeClass(CreateBucketOutput.class)
             .build();

    static final Schema CONTENT_MD5 = Schema.createString(ShapeId.from("com.amazonaws.s3#ContentMD5"));
    static final Schema KMS_KEY_ARN = Schema.createString(ShapeId.from("com.amazonaws.s3#KmsKeyArn"));
    static final Schema METADATA_TABLE_ENCRYPTION_CONFIGURATION = Schema.structureBuilder(ShapeId.from("com.amazonaws.s3#MetadataTableEncryptionConfiguration"))
             .putMember("SseAlgorithm", TableSseAlgorithm.$SCHEMA,
                     new RequiredTrait())
             .putMember("KmsKeyArn", Schemas.KMS_KEY_ARN)
             .builderSupplier(MetadataTableEncryptionConfiguration::builder)
             .shapeClass(MetadataTableEncryptionConfiguration.class)
             .build();

    static final Schema ROLE = Schema.createString(ShapeId.from("com.amazonaws.s3#Role"));
    static final Schema ANNOTATION_TABLE_CONFIGURATION = Schema.structureBuilder(ShapeId.from("com.amazonaws.s3#AnnotationTableConfiguration"))
             .putMember("ConfigurationState", AnnotationConfigurationState.$SCHEMA,
                     new RequiredTrait())
             .putMember("EncryptionConfiguration", Schemas.METADATA_TABLE_ENCRYPTION_CONFIGURATION)
             .putMember("Role", Schemas.ROLE)
             .builderSupplier(AnnotationTableConfiguration::builder)
             .shapeClass(AnnotationTableConfiguration.class)
             .build();

    static final Schema INVENTORY_TABLE_CONFIGURATION = Schema.structureBuilder(ShapeId.from("com.amazonaws.s3#InventoryTableConfiguration"))
             .putMember("ConfigurationState", InventoryConfigurationState.$SCHEMA,
                     new RequiredTrait())
             .putMember("EncryptionConfiguration", Schemas.METADATA_TABLE_ENCRYPTION_CONFIGURATION)
             .builderSupplier(InventoryTableConfiguration::builder)
             .shapeClass(InventoryTableConfiguration.class)
             .build();

    static final Schema RECORD_EXPIRATION_DAYS = Schema.createInteger(ShapeId.from("com.amazonaws.s3#RecordExpirationDays"));
    static final Schema RECORD_EXPIRATION = Schema.structureBuilder(ShapeId.from("com.amazonaws.s3#RecordExpiration"))
             .putMember("Expiration", ExpirationState.$SCHEMA,
                     new RequiredTrait())
             .putMember("Days", Schemas.RECORD_EXPIRATION_DAYS)
             .builderSupplier(RecordExpiration::builder)
             .shapeClass(RecordExpiration.class)
             .build();

    static final Schema JOURNAL_TABLE_CONFIGURATION = Schema.structureBuilder(ShapeId.from("com.amazonaws.s3#JournalTableConfiguration"))
             .putMember("RecordExpiration", Schemas.RECORD_EXPIRATION,
                     new RequiredTrait())
             .putMember("EncryptionConfiguration", Schemas.METADATA_TABLE_ENCRYPTION_CONFIGURATION)
             .builderSupplier(JournalTableConfiguration::builder)
             .shapeClass(JournalTableConfiguration.class)
             .build();

    static final Schema METADATA_CONFIGURATION = Schema.structureBuilder(ShapeId.from("com.amazonaws.s3#MetadataConfiguration"))
             .putMember("JournalTableConfiguration", Schemas.JOURNAL_TABLE_CONFIGURATION,
                     new RequiredTrait())
             .putMember("InventoryTableConfiguration", Schemas.INVENTORY_TABLE_CONFIGURATION)
             .putMember("AnnotationTableConfiguration", Schemas.ANNOTATION_TABLE_CONFIGURATION)
             .builderSupplier(MetadataConfiguration::builder)
             .shapeClass(MetadataConfiguration.class)
             .build();

    static final Schema CREATE_BUCKET_METADATA_CONFIGURATION_INPUT = Schema.structureBuilder(ShapeId.from("com.amazonaws.s3#CreateBucketMetadataConfigurationRequest"))
             .putMember("Bucket", Schemas.BUCKET_NAME,
                     new ContextParamTrait.Provider().createTrait(
                         ShapeId.from("smithy.rules#contextParam"),
                         Node.objectNodeBuilder()
                             .withMember("name", "Bucket")
                             .build()
                     ),
                     new RequiredTrait(),
                     new HttpLabelTrait())
             .putMember("ContentMD5", Schemas.CONTENT_MD5,
                     new HttpHeaderTrait("Content-MD5"))
             .putMember("ChecksumAlgorithm", ChecksumAlgorithm.$SCHEMA,
                     new HttpHeaderTrait("x-amz-sdk-checksum-algorithm"))
             .putMember("MetadataConfiguration", Schemas.METADATA_CONFIGURATION,
                     new XmlNameTrait("MetadataConfiguration"),
                     new RequiredTrait(),
                     new HttpPayloadTrait())
             .putMember("ExpectedBucketOwner", Schemas.ACCOUNT_ID,
                     new HttpHeaderTrait("x-amz-expected-bucket-owner"))
             .builderSupplier(CreateBucketMetadataConfigurationInput::builder)
             .shapeClass(CreateBucketMetadataConfigurationInput.class)
             .build();

    static final Schema CREATE_BUCKET_METADATA_CONFIGURATION_OUTPUT = Schema.structureBuilder(ShapeId.from("com.amazonaws.s3#CreateBucketMetadataConfigurationOutput"),
            new UnitTypeTrait()).builderSupplier(CreateBucketMetadataConfigurationOutput::builder)
             .shapeClass(CreateBucketMetadataConfigurationOutput.class)
             .build();

    static final Schema S3_TABLES_BUCKET_ARN = Schema.createString(ShapeId.from("com.amazonaws.s3#S3TablesBucketArn"));
    static final Schema S3_TABLES_NAME = Schema.createString(ShapeId.from("com.amazonaws.s3#S3TablesName"));
    static final Schema S3_TABLES_DESTINATION = Schema.structureBuilder(ShapeId.from("com.amazonaws.s3#S3TablesDestination"))
             .putMember("TableBucketArn", Schemas.S3_TABLES_BUCKET_ARN,
                     new RequiredTrait())
             .putMember("TableName", Schemas.S3_TABLES_NAME,
                     new RequiredTrait())
             .builderSupplier(S3TablesDestination::builder)
             .shapeClass(S3TablesDestination.class)
             .build();

    static final Schema METADATA_TABLE_CONFIGURATION = Schema.structureBuilder(ShapeId.from("com.amazonaws.s3#MetadataTableConfiguration"))
             .putMember("S3TablesDestination", Schemas.S3_TABLES_DESTINATION,
                     new RequiredTrait())
             .builderSupplier(MetadataTableConfiguration::builder)
             .shapeClass(MetadataTableConfiguration.class)
             .build();

    static final Schema CREATE_BUCKET_METADATA_TABLE_CONFIGURATION_INPUT = Schema.structureBuilder(ShapeId.from("com.amazonaws.s3#CreateBucketMetadataTableConfigurationRequest"))
             .putMember("Bucket", Schemas.BUCKET_NAME,
                     new ContextParamTrait.Provider().createTrait(
                         ShapeId.from("smithy.rules#contextParam"),
                         Node.objectNodeBuilder()
                             .withMember("name", "Bucket")
                             .build()
                     ),
                     new RequiredTrait(),
                     new HttpLabelTrait())
             .putMember("ContentMD5", Schemas.CONTENT_MD5,
                     new HttpHeaderTrait("Content-MD5"))
             .putMember("ChecksumAlgorithm", ChecksumAlgorithm.$SCHEMA,
                     new HttpHeaderTrait("x-amz-sdk-checksum-algorithm"))
             .putMember("MetadataTableConfiguration", Schemas.METADATA_TABLE_CONFIGURATION,
                     new XmlNameTrait("MetadataTableConfiguration"),
                     new RequiredTrait(),
                     new HttpPayloadTrait())
             .putMember("ExpectedBucketOwner", Schemas.ACCOUNT_ID,
                     new HttpHeaderTrait("x-amz-expected-bucket-owner"))
             .builderSupplier(CreateBucketMetadataTableConfigurationInput::builder)
             .shapeClass(CreateBucketMetadataTableConfigurationInput.class)
             .build();

    static final Schema CREATE_BUCKET_METADATA_TABLE_CONFIGURATION_OUTPUT = Schema.structureBuilder(ShapeId.from("com.amazonaws.s3#CreateBucketMetadataTableConfigurationOutput"),
            new UnitTypeTrait()).builderSupplier(CreateBucketMetadataTableConfigurationOutput::builder)
             .shapeClass(CreateBucketMetadataTableConfigurationOutput.class)
             .build();

    private Schemas() {}
}
