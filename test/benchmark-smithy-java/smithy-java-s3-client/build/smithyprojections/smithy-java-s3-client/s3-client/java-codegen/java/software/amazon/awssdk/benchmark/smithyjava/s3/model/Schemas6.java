package software.amazon.awssdk.benchmark.smithyjava.s3.model;

import software.amazon.smithy.java.core.schema.Schema;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.model.traits.ErrorTrait;
import software.amazon.smithy.model.traits.EventPayloadTrait;
import software.amazon.smithy.model.traits.HttpErrorTrait;
import software.amazon.smithy.model.traits.HttpHeaderTrait;
import software.amazon.smithy.model.traits.HttpLabelTrait;
import software.amazon.smithy.model.traits.HttpPayloadTrait;
import software.amazon.smithy.model.traits.HttpQueryTrait;
import software.amazon.smithy.model.traits.IdempotencyTokenTrait;
import software.amazon.smithy.model.traits.LengthTrait;
import software.amazon.smithy.model.traits.PatternTrait;
import software.amazon.smithy.model.traits.RequiredTrait;
import software.amazon.smithy.model.traits.SensitiveTrait;
import software.amazon.smithy.model.traits.StreamingTrait;
import software.amazon.smithy.model.traits.TimestampFormatTrait;
import software.amazon.smithy.model.traits.UnitTypeTrait;
import software.amazon.smithy.model.traits.XmlNameTrait;
import software.amazon.smithy.rulesengine.traits.ContextParamTrait;

/**
 * Defines schemas for shapes in the model package.
 */
final class Schemas6 {
    static final Schema PUT_OBJECT_OUTPUT = Schema.structureBuilder(ShapeId.from("com.amazonaws.s3#PutObjectOutput"))
             .putMember("Expiration", Schemas.EXPIRATION,
                     new HttpHeaderTrait("x-amz-expiration"))
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
             .putMember("ChecksumType", ChecksumType.$SCHEMA,
                     new HttpHeaderTrait("x-amz-checksum-type"))
             .putMember("ServerSideEncryption", ServerSideEncryption.$SCHEMA,
                     new HttpHeaderTrait("x-amz-server-side-encryption"))
             .putMember("VersionId", Schemas.OBJECT_VERSION_ID,
                     new HttpHeaderTrait("x-amz-version-id"))
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
             .putMember("Size", Schemas1.SIZE,
                     new HttpHeaderTrait("x-amz-object-size"))
             .putMember("RequestCharged", RequestCharged.$SCHEMA,
                     new HttpHeaderTrait("x-amz-request-charged"))
             .builderSupplier(PutObjectOutput::builder)
             .shapeClass(PutObjectOutput.class)
             .build();

    static final Schema TOO_MANY_PARTS = Schema.structureBuilder(ShapeId.from("com.amazonaws.s3#TooManyParts"),
            new ErrorTrait("client"),
            new HttpErrorTrait(400)).builderSupplier(TooManyParts::builder)
             .shapeClass(TooManyParts.class)
             .build();

    static final Schema PUT_OBJECT_ACL_INPUT = Schema.structureBuilder(ShapeId.from("com.amazonaws.s3#PutObjectAclRequest"))
             .putMember("ACL", ObjectCannedACL.$SCHEMA,
                     new HttpHeaderTrait("x-amz-acl"))
             .putMember("AccessControlPolicy", Schemas.ACCESS_CONTROL_POLICY,
                     new XmlNameTrait("AccessControlPolicy"),
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
             .putMember("ContentMD5", Schemas.CONTENT_MD5,
                     new HttpHeaderTrait("Content-MD5"))
             .putMember("ChecksumAlgorithm", ChecksumAlgorithm.$SCHEMA,
                     new HttpHeaderTrait("x-amz-sdk-checksum-algorithm"))
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
             .putMember("Key", Schemas.OBJECT_KEY,
                     new ContextParamTrait.Provider().createTrait(
                         ShapeId.from("smithy.rules#contextParam"),
                         Node.objectNodeBuilder()
                             .withMember("name", "Key")
                             .build()
                     ),
                     new RequiredTrait(),
                     new HttpLabelTrait())
             .putMember("RequestPayer", RequestPayer.$SCHEMA,
                     new HttpHeaderTrait("x-amz-request-payer"))
             .putMember("VersionId", Schemas.OBJECT_VERSION_ID,
                     new HttpQueryTrait("versionId"))
             .putMember("ExpectedBucketOwner", Schemas.ACCOUNT_ID,
                     new HttpHeaderTrait("x-amz-expected-bucket-owner"))
             .builderSupplier(PutObjectAclInput::builder)
             .shapeClass(PutObjectAclInput.class)
             .build();

    static final Schema PUT_OBJECT_ACL_OUTPUT = Schema.structureBuilder(ShapeId.from("com.amazonaws.s3#PutObjectAclOutput"))
             .putMember("RequestCharged", RequestCharged.$SCHEMA,
                     new HttpHeaderTrait("x-amz-request-charged"))
             .builderSupplier(PutObjectAclOutput::builder)
             .shapeClass(PutObjectAclOutput.class)
             .build();

    static final Schema ANNOTATION_LIMIT_EXCEEDED = Schema.structureBuilder(ShapeId.from("com.amazonaws.s3#AnnotationLimitExceeded"),
            new ErrorTrait("client"),
            new HttpErrorTrait(400)).builderSupplier(AnnotationLimitExceeded::builder)
             .shapeClass(AnnotationLimitExceeded.class)
             .build();

    static final Schema ANNOTATION_NAME_TOO_LONG = Schema.structureBuilder(ShapeId.from("com.amazonaws.s3#AnnotationNameTooLong"),
            new ErrorTrait("client"),
            new HttpErrorTrait(400)).builderSupplier(AnnotationNameTooLong::builder)
             .shapeClass(AnnotationNameTooLong.class)
             .build();

    static final Schema INVALID_ANNOTATION_NAME = Schema.structureBuilder(ShapeId.from("com.amazonaws.s3#InvalidAnnotationName"),
            new ErrorTrait("client"),
            new HttpErrorTrait(400)).builderSupplier(InvalidAnnotationName::builder)
             .shapeClass(InvalidAnnotationName.class)
             .build();

    static final Schema PUT_OBJECT_ANNOTATION_INPUT = Schema.structureBuilder(ShapeId.from("com.amazonaws.s3#PutObjectAnnotationRequest"))
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
             .putMember("VersionId", Schemas.OBJECT_VERSION_ID,
                     new HttpQueryTrait("versionId"))
             .putMember("AnnotationName", Schemas1.ANNOTATION_NAME,
                     new RequiredTrait(),
                     new HttpQueryTrait("annotationName"))
             .putMember("AnnotationPayload", Schemas3.STREAMING_BLOB,
                     new RequiredTrait(),
                     new HttpPayloadTrait())
             .putMember("ObjectIfMatch", Schemas1.OBJECT_IF_MATCH,
                     new HttpHeaderTrait("x-amz-object-if-match"))
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
             .putMember("ContentMD5", Schemas.CONTENT_MD5,
                     new HttpHeaderTrait("Content-MD5"))
             .putMember("RequestPayer", RequestPayer.$SCHEMA,
                     new HttpHeaderTrait("x-amz-request-payer"))
             .putMember("ExpectedBucketOwner", Schemas.ACCOUNT_ID,
                     new HttpHeaderTrait("x-amz-expected-bucket-owner"))
             .builderSupplier(PutObjectAnnotationInput::builder)
             .shapeClass(PutObjectAnnotationInput.class)
             .build();

    static final Schema PUT_OBJECT_ANNOTATION_OUTPUT = Schema.structureBuilder(ShapeId.from("com.amazonaws.s3#PutObjectAnnotationOutput"))
             .putMember("Key", Schemas.OBJECT_KEY)
             .putMember("AnnotationName", Schemas1.ANNOTATION_NAME)
             .putMember("ObjectVersionId", Schemas.OBJECT_VERSION_ID,
                     new HttpHeaderTrait("x-amz-object-version-id"))
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
             .putMember("ChecksumType", ChecksumType.$SCHEMA,
                     new HttpHeaderTrait("x-amz-checksum-type"))
             .putMember("ServerSideEncryption", ServerSideEncryption.$SCHEMA,
                     new HttpHeaderTrait("x-amz-server-side-encryption"))
             .putMember("RequestCharged", RequestCharged.$SCHEMA,
                     new HttpHeaderTrait("x-amz-request-charged"))
             .builderSupplier(PutObjectAnnotationOutput::builder)
             .shapeClass(PutObjectAnnotationOutput.class)
             .build();

    static final Schema UNSUPPORTED_MEDIA_TYPE = Schema.structureBuilder(ShapeId.from("com.amazonaws.s3#UnsupportedMediaType"),
            new ErrorTrait("client"),
            new HttpErrorTrait(415)).builderSupplier(UnsupportedMediaType::builder)
             .shapeClass(UnsupportedMediaType.class)
             .build();

    static final Schema PUT_OBJECT_LEGAL_HOLD_INPUT = Schema.structureBuilder(ShapeId.from("com.amazonaws.s3#PutObjectLegalHoldRequest"))
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
             .putMember("LegalHold", Schemas3.OBJECT_LOCK_LEGAL_HOLD,
                     new XmlNameTrait("LegalHold"),
                     new HttpPayloadTrait())
             .putMember("RequestPayer", RequestPayer.$SCHEMA,
                     new HttpHeaderTrait("x-amz-request-payer"))
             .putMember("VersionId", Schemas.OBJECT_VERSION_ID,
                     new HttpQueryTrait("versionId"))
             .putMember("ContentMD5", Schemas.CONTENT_MD5,
                     new HttpHeaderTrait("Content-MD5"))
             .putMember("ChecksumAlgorithm", ChecksumAlgorithm.$SCHEMA,
                     new HttpHeaderTrait("x-amz-sdk-checksum-algorithm"))
             .putMember("ExpectedBucketOwner", Schemas.ACCOUNT_ID,
                     new HttpHeaderTrait("x-amz-expected-bucket-owner"))
             .builderSupplier(PutObjectLegalHoldInput::builder)
             .shapeClass(PutObjectLegalHoldInput.class)
             .build();

    static final Schema PUT_OBJECT_LEGAL_HOLD_OUTPUT = Schema.structureBuilder(ShapeId.from("com.amazonaws.s3#PutObjectLegalHoldOutput"))
             .putMember("RequestCharged", RequestCharged.$SCHEMA,
                     new HttpHeaderTrait("x-amz-request-charged"))
             .builderSupplier(PutObjectLegalHoldOutput::builder)
             .shapeClass(PutObjectLegalHoldOutput.class)
             .build();

    static final Schema PUT_OBJECT_LOCK_CONFIGURATION_INPUT = Schema.structureBuilder(ShapeId.from("com.amazonaws.s3#PutObjectLockConfigurationRequest"))
             .putMember("Bucket", Schemas.BUCKET_NAME,
                     new ContextParamTrait.Provider().createTrait(
                         ShapeId.from("smithy.rules#contextParam"),
                         Node.objectNodeBuilder()
                             .withMember("name", "Bucket")
                             .build()
                     ),
                     new RequiredTrait(),
                     new HttpLabelTrait())
             .putMember("ObjectLockConfiguration", Schemas3.OBJECT_LOCK_CONFIGURATION,
                     new XmlNameTrait("ObjectLockConfiguration"),
                     new HttpPayloadTrait())
             .putMember("RequestPayer", RequestPayer.$SCHEMA,
                     new HttpHeaderTrait("x-amz-request-payer"))
             .putMember("Token", Schemas5.OBJECT_LOCK_TOKEN,
                     new HttpHeaderTrait("x-amz-bucket-object-lock-token"))
             .putMember("ContentMD5", Schemas.CONTENT_MD5,
                     new HttpHeaderTrait("Content-MD5"))
             .putMember("ChecksumAlgorithm", ChecksumAlgorithm.$SCHEMA,
                     new HttpHeaderTrait("x-amz-sdk-checksum-algorithm"))
             .putMember("ExpectedBucketOwner", Schemas.ACCOUNT_ID,
                     new HttpHeaderTrait("x-amz-expected-bucket-owner"))
             .builderSupplier(PutObjectLockConfigurationInput::builder)
             .shapeClass(PutObjectLockConfigurationInput.class)
             .build();

    static final Schema PUT_OBJECT_LOCK_CONFIGURATION_OUTPUT = Schema.structureBuilder(ShapeId.from("com.amazonaws.s3#PutObjectLockConfigurationOutput"))
             .putMember("RequestCharged", RequestCharged.$SCHEMA,
                     new HttpHeaderTrait("x-amz-request-charged"))
             .builderSupplier(PutObjectLockConfigurationOutput::builder)
             .shapeClass(PutObjectLockConfigurationOutput.class)
             .build();

    static final Schema PUT_OBJECT_RETENTION_INPUT = Schema.structureBuilder(ShapeId.from("com.amazonaws.s3#PutObjectRetentionRequest"))
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
             .putMember("Retention", Schemas3.OBJECT_LOCK_RETENTION,
                     new XmlNameTrait("Retention"),
                     new HttpPayloadTrait())
             .putMember("RequestPayer", RequestPayer.$SCHEMA,
                     new HttpHeaderTrait("x-amz-request-payer"))
             .putMember("VersionId", Schemas.OBJECT_VERSION_ID,
                     new HttpQueryTrait("versionId"))
             .putMember("BypassGovernanceRetention", Schemas1.BYPASS_GOVERNANCE_RETENTION,
                     new HttpHeaderTrait("x-amz-bypass-governance-retention"))
             .putMember("ContentMD5", Schemas.CONTENT_MD5,
                     new HttpHeaderTrait("Content-MD5"))
             .putMember("ChecksumAlgorithm", ChecksumAlgorithm.$SCHEMA,
                     new HttpHeaderTrait("x-amz-sdk-checksum-algorithm"))
             .putMember("ExpectedBucketOwner", Schemas.ACCOUNT_ID,
                     new HttpHeaderTrait("x-amz-expected-bucket-owner"))
             .builderSupplier(PutObjectRetentionInput::builder)
             .shapeClass(PutObjectRetentionInput.class)
             .build();

    static final Schema PUT_OBJECT_RETENTION_OUTPUT = Schema.structureBuilder(ShapeId.from("com.amazonaws.s3#PutObjectRetentionOutput"))
             .putMember("RequestCharged", RequestCharged.$SCHEMA,
                     new HttpHeaderTrait("x-amz-request-charged"))
             .builderSupplier(PutObjectRetentionOutput::builder)
             .shapeClass(PutObjectRetentionOutput.class)
             .build();

    static final Schema PUT_OBJECT_TAGGING_INPUT = Schema.structureBuilder(ShapeId.from("com.amazonaws.s3#PutObjectTaggingRequest"))
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
             .putMember("ContentMD5", Schemas.CONTENT_MD5,
                     new HttpHeaderTrait("Content-MD5"))
             .putMember("ChecksumAlgorithm", ChecksumAlgorithm.$SCHEMA,
                     new HttpHeaderTrait("x-amz-sdk-checksum-algorithm"))
             .putMember("Tagging", Schemas5.TAGGING,
                     new XmlNameTrait("Tagging"),
                     new RequiredTrait(),
                     new HttpPayloadTrait())
             .putMember("ExpectedBucketOwner", Schemas.ACCOUNT_ID,
                     new HttpHeaderTrait("x-amz-expected-bucket-owner"))
             .putMember("RequestPayer", RequestPayer.$SCHEMA,
                     new HttpHeaderTrait("x-amz-request-payer"))
             .builderSupplier(PutObjectTaggingInput::builder)
             .shapeClass(PutObjectTaggingInput.class)
             .build();

    static final Schema PUT_OBJECT_TAGGING_OUTPUT = Schema.structureBuilder(ShapeId.from("com.amazonaws.s3#PutObjectTaggingOutput"))
             .putMember("VersionId", Schemas.OBJECT_VERSION_ID,
                     new HttpHeaderTrait("x-amz-version-id"))
             .builderSupplier(PutObjectTaggingOutput::builder)
             .shapeClass(PutObjectTaggingOutput.class)
             .build();

    static final Schema PUT_PUBLIC_ACCESS_BLOCK_INPUT = Schema.structureBuilder(ShapeId.from("com.amazonaws.s3#PutPublicAccessBlockRequest"))
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
             .putMember("PublicAccessBlockConfiguration", Schemas4.PUBLIC_ACCESS_BLOCK_CONFIGURATION,
                     new XmlNameTrait("PublicAccessBlockConfiguration"),
                     new RequiredTrait(),
                     new HttpPayloadTrait())
             .putMember("ExpectedBucketOwner", Schemas.ACCOUNT_ID,
                     new HttpHeaderTrait("x-amz-expected-bucket-owner"))
             .builderSupplier(PutPublicAccessBlockInput::builder)
             .shapeClass(PutPublicAccessBlockInput.class)
             .build();

    static final Schema PUT_PUBLIC_ACCESS_BLOCK_OUTPUT = Schema.structureBuilder(ShapeId.from("com.amazonaws.s3#PutPublicAccessBlockOutput"),
            new UnitTypeTrait()).builderSupplier(PutPublicAccessBlockOutput::builder)
             .shapeClass(PutPublicAccessBlockOutput.class)
             .build();

    static final Schema IDEMPOTENCY_PARAMETER_MISMATCH = Schema.structureBuilder(ShapeId.from("com.amazonaws.s3#IdempotencyParameterMismatch"),
            new ErrorTrait("client"),
            new HttpErrorTrait(400)).builderSupplier(IdempotencyParameterMismatch::builder)
             .shapeClass(IdempotencyParameterMismatch.class)
             .build();

    static final Schema CLIENT_TOKEN = Schema.createString(ShapeId.from("com.amazonaws.s3#ClientToken"));
    static final Schema RENAME_SOURCE = Schema.createString(ShapeId.from("com.amazonaws.s3#RenameSource"),
            new PatternTrait("^\\/?.+\\/.+$"));
    static final Schema RENAME_SOURCE_IF_MATCH = Schema.createString(ShapeId.from("com.amazonaws.s3#RenameSourceIfMatch"));
    static final Schema RENAME_SOURCE_IF_MODIFIED_SINCE = Schema.createTimestamp(ShapeId.from("com.amazonaws.s3#RenameSourceIfModifiedSince"),
            new TimestampFormatTrait("http-date"));
    static final Schema RENAME_SOURCE_IF_NONE_MATCH = Schema.createString(ShapeId.from("com.amazonaws.s3#RenameSourceIfNoneMatch"));
    static final Schema RENAME_SOURCE_IF_UNMODIFIED_SINCE = Schema.createTimestamp(ShapeId.from("com.amazonaws.s3#RenameSourceIfUnmodifiedSince"),
            new TimestampFormatTrait("http-date"));
    static final Schema RENAME_OBJECT_INPUT = Schema.structureBuilder(ShapeId.from("com.amazonaws.s3#RenameObjectRequest"))
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
             .putMember("RenameSource", Schemas6.RENAME_SOURCE,
                     new HttpHeaderTrait("x-amz-rename-source"),
                     new RequiredTrait())
             .putMember("DestinationIfMatch", Schemas.IF_MATCH,
                     new HttpHeaderTrait("If-Match"))
             .putMember("DestinationIfNoneMatch", Schemas.IF_NONE_MATCH,
                     new HttpHeaderTrait("If-None-Match"))
             .putMember("DestinationIfModifiedSince", Schemas3.IF_MODIFIED_SINCE,
                     new HttpHeaderTrait("If-Modified-Since"))
             .putMember("DestinationIfUnmodifiedSince", Schemas3.IF_UNMODIFIED_SINCE,
                     new HttpHeaderTrait("If-Unmodified-Since"))
             .putMember("SourceIfMatch", Schemas6.RENAME_SOURCE_IF_MATCH,
                     new HttpHeaderTrait("x-amz-rename-source-if-match"))
             .putMember("SourceIfNoneMatch", Schemas6.RENAME_SOURCE_IF_NONE_MATCH,
                     new HttpHeaderTrait("x-amz-rename-source-if-none-match"))
             .putMember("SourceIfModifiedSince", Schemas6.RENAME_SOURCE_IF_MODIFIED_SINCE,
                     new HttpHeaderTrait("x-amz-rename-source-if-modified-since"))
             .putMember("SourceIfUnmodifiedSince", Schemas6.RENAME_SOURCE_IF_UNMODIFIED_SINCE,
                     new HttpHeaderTrait("x-amz-rename-source-if-unmodified-since"))
             .putMember("ClientToken", Schemas6.CLIENT_TOKEN,
                     new HttpHeaderTrait("x-amz-client-token"),
                     new IdempotencyTokenTrait())
             .builderSupplier(RenameObjectInput::builder)
             .shapeClass(RenameObjectInput.class)
             .build();

    static final Schema RENAME_OBJECT_OUTPUT = Schema.structureBuilder(ShapeId.from("com.amazonaws.s3#RenameObjectOutput")).builderSupplier(RenameObjectOutput::builder)
             .shapeClass(RenameObjectOutput.class)
             .build();

    static final Schema OBJECT_ALREADY_IN_ACTIVE_TIER_ERROR = Schema.structureBuilder(ShapeId.from("com.amazonaws.s3#ObjectAlreadyInActiveTierError"),
            new ErrorTrait("client"),
            new HttpErrorTrait(403)).builderSupplier(ObjectAlreadyInActiveTierError::builder)
             .shapeClass(ObjectAlreadyInActiveTierError.class)
             .build();

    static final Schema DESCRIPTION = Schema.createString(ShapeId.from("com.amazonaws.s3#Description"));
    static final Schema GLACIER_JOB_PARAMETERS = Schema.structureBuilder(ShapeId.from("com.amazonaws.s3#GlacierJobParameters"))
             .putMember("Tier", Tier.$SCHEMA,
                     new RequiredTrait())
             .builderSupplier(GlacierJobParameters::builder)
             .shapeClass(GlacierJobParameters.class)
             .build();

    static final Schema KMS_CONTEXT = Schema.createString(ShapeId.from("com.amazonaws.s3#KMSContext"));
    static final Schema ENCRYPTION = Schema.structureBuilder(ShapeId.from("com.amazonaws.s3#Encryption"))
             .putMember("EncryptionType", ServerSideEncryption.$SCHEMA,
                     new RequiredTrait())
             .putMember("KMSKeyId", Schemas.SSEKMS_KEY_ID)
             .putMember("KMSContext", Schemas6.KMS_CONTEXT)
             .builderSupplier(Encryption::builder)
             .shapeClass(Encryption.class)
             .build();

    static final Schema LOCATION_PREFIX = Schema.createString(ShapeId.from("com.amazonaws.s3#LocationPrefix"));
    static final Schema METADATA_ENTRY = Schema.structureBuilder(ShapeId.from("com.amazonaws.s3#MetadataEntry"))
             .putMember("Name", Schemas.METADATA_KEY)
             .putMember("Value", Schemas.METADATA_VALUE)
             .builderSupplier(MetadataEntry::builder)
             .shapeClass(MetadataEntry.class)
             .build();

    static final Schema USER_METADATA = Schema.listBuilder(ShapeId.from("com.amazonaws.s3#UserMetadata"))
        .putMember("member", Schemas6.METADATA_ENTRY,
                new XmlNameTrait("MetadataEntry"))
        .build();

    static final Schema S3_LOCATION = Schema.structureBuilder(ShapeId.from("com.amazonaws.s3#S3Location"))
             .putMember("BucketName", Schemas.BUCKET_NAME,
                     new RequiredTrait())
             .putMember("Prefix", Schemas6.LOCATION_PREFIX,
                     new RequiredTrait())
             .putMember("Encryption", Schemas6.ENCRYPTION)
             .putMember("CannedACL", ObjectCannedACL.$SCHEMA)
             .putMember("AccessControlList", Schemas.GRANTS)
             .putMember("Tagging", Schemas5.TAGGING)
             .putMember("UserMetadata", Schemas6.USER_METADATA)
             .putMember("StorageClass", StorageClass.$SCHEMA)
             .builderSupplier(S3Location::builder)
             .shapeClass(S3Location.class)
             .build();

    static final Schema OUTPUT_LOCATION = Schema.structureBuilder(ShapeId.from("com.amazonaws.s3#OutputLocation"))
             .putMember("S3", Schemas6.S3_LOCATION)
             .builderSupplier(OutputLocation::builder)
             .shapeClass(OutputLocation.class)
             .build();

    static final Schema EXPRESSION = Schema.createString(ShapeId.from("com.amazonaws.s3#Expression"));
    static final Schema COMMENTS = Schema.createString(ShapeId.from("com.amazonaws.s3#Comments"));
    static final Schema FIELD_DELIMITER = Schema.createString(ShapeId.from("com.amazonaws.s3#FieldDelimiter"));
    static final Schema QUOTE_CHARACTER = Schema.createString(ShapeId.from("com.amazonaws.s3#QuoteCharacter"));
    static final Schema QUOTE_ESCAPE_CHARACTER = Schema.createString(ShapeId.from("com.amazonaws.s3#QuoteEscapeCharacter"));
    static final Schema RECORD_DELIMITER = Schema.createString(ShapeId.from("com.amazonaws.s3#RecordDelimiter"));
    static final Schema CSV_INPUT = Schema.structureBuilder(ShapeId.from("com.amazonaws.s3#CSVInput"))
             .putMember("FileHeaderInfo", FileHeaderInfo.$SCHEMA)
             .putMember("Comments", Schemas6.COMMENTS)
             .putMember("QuoteEscapeCharacter", Schemas6.QUOTE_ESCAPE_CHARACTER)
             .putMember("RecordDelimiter", Schemas6.RECORD_DELIMITER)
             .putMember("FieldDelimiter", Schemas6.FIELD_DELIMITER)
             .putMember("QuoteCharacter", Schemas6.QUOTE_CHARACTER)
             .putMember("AllowQuotedRecordDelimiter", Schemas.ALLOW_QUOTED_RECORD_DELIMITER)
             .builderSupplier(CSVInput::builder)
             .shapeClass(CSVInput.class)
             .build();

    static final Schema JSON_INPUT = Schema.structureBuilder(ShapeId.from("com.amazonaws.s3#JSONInput"))
             .putMember("Type", JSONType.$SCHEMA)
             .builderSupplier(JSONInput::builder)
             .shapeClass(JSONInput.class)
             .build();

    static final Schema PARQUET_INPUT = Schema.structureBuilder(ShapeId.from("com.amazonaws.s3#ParquetInput")).builderSupplier(ParquetInput::builder)
             .shapeClass(ParquetInput.class)
             .build();

    static final Schema INPUT_SERIALIZATION = Schema.structureBuilder(ShapeId.from("com.amazonaws.s3#InputSerialization"))
             .putMember("CSV", Schemas6.CSV_INPUT)
             .putMember("CompressionType", CompressionType.$SCHEMA)
             .putMember("JSON", Schemas6.JSON_INPUT)
             .putMember("Parquet", Schemas6.PARQUET_INPUT)
             .builderSupplier(InputSerialization::builder)
             .shapeClass(InputSerialization.class)
             .build();

    static final Schema CSV_OUTPUT = Schema.structureBuilder(ShapeId.from("com.amazonaws.s3#CSVOutput"))
             .putMember("QuoteFields", QuoteFields.$SCHEMA)
             .putMember("QuoteEscapeCharacter", Schemas6.QUOTE_ESCAPE_CHARACTER)
             .putMember("RecordDelimiter", Schemas6.RECORD_DELIMITER)
             .putMember("FieldDelimiter", Schemas6.FIELD_DELIMITER)
             .putMember("QuoteCharacter", Schemas6.QUOTE_CHARACTER)
             .builderSupplier(CSVOutput::builder)
             .shapeClass(CSVOutput.class)
             .build();

    static final Schema JSON_OUTPUT = Schema.structureBuilder(ShapeId.from("com.amazonaws.s3#JSONOutput"))
             .putMember("RecordDelimiter", Schemas6.RECORD_DELIMITER)
             .builderSupplier(JSONOutput::builder)
             .shapeClass(JSONOutput.class)
             .build();

    static final Schema OUTPUT_SERIALIZATION = Schema.structureBuilder(ShapeId.from("com.amazonaws.s3#OutputSerialization"))
             .putMember("CSV", Schemas6.CSV_OUTPUT)
             .putMember("JSON", Schemas6.JSON_OUTPUT)
             .builderSupplier(OutputSerialization::builder)
             .shapeClass(OutputSerialization.class)
             .build();

    static final Schema SELECT_PARAMETERS = Schema.structureBuilder(ShapeId.from("com.amazonaws.s3#SelectParameters"))
             .putMember("InputSerialization", Schemas6.INPUT_SERIALIZATION,
                     new RequiredTrait())
             .putMember("ExpressionType", ExpressionType.$SCHEMA,
                     new RequiredTrait())
             .putMember("Expression", Schemas6.EXPRESSION,
                     new RequiredTrait())
             .putMember("OutputSerialization", Schemas6.OUTPUT_SERIALIZATION,
                     new RequiredTrait())
             .builderSupplier(SelectParameters::builder)
             .shapeClass(SelectParameters.class)
             .build();

    static final Schema RESTORE_REQUEST = Schema.structureBuilder(ShapeId.from("com.amazonaws.s3#RestoreRequest"))
             .putMember("Days", Schemas2.DAYS)
             .putMember("GlacierJobParameters", Schemas6.GLACIER_JOB_PARAMETERS)
             .putMember("Type", RestoreRequestType.$SCHEMA)
             .putMember("Tier", Tier.$SCHEMA)
             .putMember("Description", Schemas6.DESCRIPTION)
             .putMember("SelectParameters", Schemas6.SELECT_PARAMETERS)
             .putMember("OutputLocation", Schemas6.OUTPUT_LOCATION)
             .builderSupplier(RestoreRequest::builder)
             .shapeClass(RestoreRequest.class)
             .build();

    static final Schema RESTORE_OBJECT_INPUT = Schema.structureBuilder(ShapeId.from("com.amazonaws.s3#RestoreObjectRequest"))
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
             .putMember("RestoreRequest", Schemas6.RESTORE_REQUEST,
                     new XmlNameTrait("RestoreRequest"),
                     new HttpPayloadTrait())
             .putMember("RequestPayer", RequestPayer.$SCHEMA,
                     new HttpHeaderTrait("x-amz-request-payer"))
             .putMember("ChecksumAlgorithm", ChecksumAlgorithm.$SCHEMA,
                     new HttpHeaderTrait("x-amz-sdk-checksum-algorithm"))
             .putMember("ExpectedBucketOwner", Schemas.ACCOUNT_ID,
                     new HttpHeaderTrait("x-amz-expected-bucket-owner"))
             .builderSupplier(RestoreObjectInput::builder)
             .shapeClass(RestoreObjectInput.class)
             .build();

    static final Schema RESTORE_OUTPUT_PATH = Schema.createString(ShapeId.from("com.amazonaws.s3#RestoreOutputPath"));
    static final Schema RESTORE_OBJECT_OUTPUT = Schema.structureBuilder(ShapeId.from("com.amazonaws.s3#RestoreObjectOutput"))
             .putMember("RequestCharged", RequestCharged.$SCHEMA,
                     new HttpHeaderTrait("x-amz-request-charged"))
             .putMember("RestoreOutputPath", Schemas6.RESTORE_OUTPUT_PATH,
                     new HttpHeaderTrait("x-amz-restore-output-path"))
             .builderSupplier(RestoreObjectOutput::builder)
             .shapeClass(RestoreObjectOutput.class)
             .build();

    static final Schema ENABLE_REQUEST_PROGRESS = Schema.createBoolean(ShapeId.from("com.amazonaws.s3#EnableRequestProgress"));
    static final Schema REQUEST_PROGRESS = Schema.structureBuilder(ShapeId.from("com.amazonaws.s3#RequestProgress"))
             .putMember("Enabled", Schemas6.ENABLE_REQUEST_PROGRESS)
             .builderSupplier(RequestProgress::builder)
             .shapeClass(RequestProgress.class)
             .build();

    static final Schema END = Schema.createLong(ShapeId.from("com.amazonaws.s3#End"));
    static final Schema START = Schema.createLong(ShapeId.from("com.amazonaws.s3#Start"));
    static final Schema SCAN_RANGE = Schema.structureBuilder(ShapeId.from("com.amazonaws.s3#ScanRange"))
             .putMember("Start", Schemas6.START)
             .putMember("End", Schemas6.END)
             .builderSupplier(ScanRange::builder)
             .shapeClass(ScanRange.class)
             .build();

    static final Schema SELECT_OBJECT_CONTENT_INPUT = Schema.structureBuilder(ShapeId.from("com.amazonaws.s3#SelectObjectContentRequest"))
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
             .putMember("SSECustomerAlgorithm", Schemas.SSE_CUSTOMER_ALGORITHM,
                     new HttpHeaderTrait("x-amz-server-side-encryption-customer-algorithm"))
             .putMember("SSECustomerKey", Schemas.SSE_CUSTOMER_KEY,
                     new HttpHeaderTrait("x-amz-server-side-encryption-customer-key"))
             .putMember("SSECustomerKeyMD5", Schemas.SSE_CUSTOMER_KEY_MD5,
                     new HttpHeaderTrait("x-amz-server-side-encryption-customer-key-MD5"))
             .putMember("Expression", Schemas6.EXPRESSION,
                     new RequiredTrait())
             .putMember("ExpressionType", ExpressionType.$SCHEMA,
                     new RequiredTrait())
             .putMember("RequestProgress", Schemas6.REQUEST_PROGRESS)
             .putMember("InputSerialization", Schemas6.INPUT_SERIALIZATION,
                     new RequiredTrait())
             .putMember("OutputSerialization", Schemas6.OUTPUT_SERIALIZATION,
                     new RequiredTrait())
             .putMember("ScanRange", Schemas6.SCAN_RANGE)
             .putMember("ExpectedBucketOwner", Schemas.ACCOUNT_ID,
                     new HttpHeaderTrait("x-amz-expected-bucket-owner"))
             .builderSupplier(SelectObjectContentInput::builder)
             .shapeClass(SelectObjectContentInput.class)
             .build();

    static final Schema CONTINUATION_EVENT = Schema.structureBuilder(ShapeId.from("com.amazonaws.s3#ContinuationEvent")).builderSupplier(ContinuationEvent::builder)
             .shapeClass(ContinuationEvent.class)
             .build();

    static final Schema END_EVENT = Schema.structureBuilder(ShapeId.from("com.amazonaws.s3#EndEvent")).builderSupplier(EndEvent::builder)
             .shapeClass(EndEvent.class)
             .build();

    static final Schema BYTES_PROCESSED = Schema.createLong(ShapeId.from("com.amazonaws.s3#BytesProcessed"));
    static final Schema BYTES_RETURNED = Schema.createLong(ShapeId.from("com.amazonaws.s3#BytesReturned"));
    static final Schema BYTES_SCANNED = Schema.createLong(ShapeId.from("com.amazonaws.s3#BytesScanned"));
    static final Schema PROGRESS = Schema.structureBuilder(ShapeId.from("com.amazonaws.s3#Progress"))
             .putMember("BytesScanned", Schemas6.BYTES_SCANNED)
             .putMember("BytesProcessed", Schemas6.BYTES_PROCESSED)
             .putMember("BytesReturned", Schemas6.BYTES_RETURNED)
             .builderSupplier(Progress::builder)
             .shapeClass(Progress.class)
             .build();

    static final Schema PROGRESS_EVENT = Schema.structureBuilder(ShapeId.from("com.amazonaws.s3#ProgressEvent"))
             .putMember("Details", Schemas6.PROGRESS,
                     new EventPayloadTrait())
             .builderSupplier(ProgressEvent::builder)
             .shapeClass(ProgressEvent.class)
             .build();

    static final Schema BODY = Schema.createBlob(ShapeId.from("com.amazonaws.s3#Body"));
    static final Schema RECORDS_EVENT = Schema.structureBuilder(ShapeId.from("com.amazonaws.s3#RecordsEvent"))
             .putMember("Payload", Schemas6.BODY,
                     new EventPayloadTrait())
             .builderSupplier(RecordsEvent::builder)
             .shapeClass(RecordsEvent.class)
             .build();

    static final Schema STATS = Schema.structureBuilder(ShapeId.from("com.amazonaws.s3#Stats"))
             .putMember("BytesScanned", Schemas6.BYTES_SCANNED)
             .putMember("BytesProcessed", Schemas6.BYTES_PROCESSED)
             .putMember("BytesReturned", Schemas6.BYTES_RETURNED)
             .builderSupplier(Stats::builder)
             .shapeClass(Stats.class)
             .build();

    static final Schema STATS_EVENT = Schema.structureBuilder(ShapeId.from("com.amazonaws.s3#StatsEvent"))
             .putMember("Details", Schemas6.STATS,
                     new EventPayloadTrait())
             .builderSupplier(StatsEvent::builder)
             .shapeClass(StatsEvent.class)
             .build();

    static final Schema SELECT_OBJECT_CONTENT_EVENT_STREAM = Schema.unionBuilder(ShapeId.from("com.amazonaws.s3#SelectObjectContentEventStream"),
            new StreamingTrait())
             .putMember("Records", Schemas6.RECORDS_EVENT)
             .putMember("Stats", Schemas6.STATS_EVENT)
             .putMember("Progress", Schemas6.PROGRESS_EVENT)
             .putMember("Cont", Schemas6.CONTINUATION_EVENT)
             .putMember("End", Schemas6.END_EVENT)
             .builderSupplier(SelectObjectContentEventStream::builder)
             .shapeClass(SelectObjectContentEventStream.class)
             .build();

    static final Schema SELECT_OBJECT_CONTENT_OUTPUT = Schema.structureBuilder(ShapeId.from("com.amazonaws.s3#SelectObjectContentOutput"))
             .putMember("Payload", Schemas6.SELECT_OBJECT_CONTENT_EVENT_STREAM,
                     new HttpPayloadTrait())
             .builderSupplier(SelectObjectContentOutput::builder)
             .shapeClass(SelectObjectContentOutput.class)
             .build();

    static final Schema ANNOTATION_TABLE_CONFIGURATION_UPDATES = Schema.structureBuilder(ShapeId.from("com.amazonaws.s3#AnnotationTableConfigurationUpdates"))
             .putMember("ConfigurationState", AnnotationConfigurationState.$SCHEMA,
                     new RequiredTrait())
             .putMember("EncryptionConfiguration", Schemas.METADATA_TABLE_ENCRYPTION_CONFIGURATION)
             .putMember("Role", Schemas.ROLE)
             .builderSupplier(AnnotationTableConfigurationUpdates::builder)
             .shapeClass(AnnotationTableConfigurationUpdates.class)
             .build();

    static final Schema UPDATE_BUCKET_METADATA_ANNOTATION_TABLE_CONFIGURATION_INPUT = Schema.structureBuilder(ShapeId.from("com.amazonaws.s3#UpdateBucketMetadataAnnotationTableConfigurationRequest"))
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
             .putMember("AnnotationTableConfiguration", Schemas6.ANNOTATION_TABLE_CONFIGURATION_UPDATES,
                     new XmlNameTrait("AnnotationTableConfiguration"),
                     new RequiredTrait(),
                     new HttpPayloadTrait())
             .putMember("ExpectedBucketOwner", Schemas.ACCOUNT_ID,
                     new HttpHeaderTrait("x-amz-expected-bucket-owner"))
             .builderSupplier(UpdateBucketMetadataAnnotationTableConfigurationInput::builder)
             .shapeClass(UpdateBucketMetadataAnnotationTableConfigurationInput.class)
             .build();

    static final Schema UPDATE_BUCKET_METADATA_ANNOTATION_TABLE_CONFIGURATION_OUTPUT = Schema.structureBuilder(ShapeId.from("com.amazonaws.s3#UpdateBucketMetadataAnnotationTableConfigurationOutput"),
            new UnitTypeTrait()).builderSupplier(UpdateBucketMetadataAnnotationTableConfigurationOutput::builder)
             .shapeClass(UpdateBucketMetadataAnnotationTableConfigurationOutput.class)
             .build();

    static final Schema INVENTORY_TABLE_CONFIGURATION_UPDATES = Schema.structureBuilder(ShapeId.from("com.amazonaws.s3#InventoryTableConfigurationUpdates"))
             .putMember("ConfigurationState", InventoryConfigurationState.$SCHEMA,
                     new RequiredTrait())
             .putMember("EncryptionConfiguration", Schemas.METADATA_TABLE_ENCRYPTION_CONFIGURATION)
             .builderSupplier(InventoryTableConfigurationUpdates::builder)
             .shapeClass(InventoryTableConfigurationUpdates.class)
             .build();

    static final Schema UPDATE_BUCKET_METADATA_INVENTORY_TABLE_CONFIGURATION_INPUT = Schema.structureBuilder(ShapeId.from("com.amazonaws.s3#UpdateBucketMetadataInventoryTableConfigurationRequest"))
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
             .putMember("InventoryTableConfiguration", Schemas6.INVENTORY_TABLE_CONFIGURATION_UPDATES,
                     new XmlNameTrait("InventoryTableConfiguration"),
                     new RequiredTrait(),
                     new HttpPayloadTrait())
             .putMember("ExpectedBucketOwner", Schemas.ACCOUNT_ID,
                     new HttpHeaderTrait("x-amz-expected-bucket-owner"))
             .builderSupplier(UpdateBucketMetadataInventoryTableConfigurationInput::builder)
             .shapeClass(UpdateBucketMetadataInventoryTableConfigurationInput.class)
             .build();

    static final Schema UPDATE_BUCKET_METADATA_INVENTORY_TABLE_CONFIGURATION_OUTPUT = Schema.structureBuilder(ShapeId.from("com.amazonaws.s3#UpdateBucketMetadataInventoryTableConfigurationOutput"),
            new UnitTypeTrait()).builderSupplier(UpdateBucketMetadataInventoryTableConfigurationOutput::builder)
             .shapeClass(UpdateBucketMetadataInventoryTableConfigurationOutput.class)
             .build();

    static final Schema JOURNAL_TABLE_CONFIGURATION_UPDATES = Schema.structureBuilder(ShapeId.from("com.amazonaws.s3#JournalTableConfigurationUpdates"))
             .putMember("RecordExpiration", Schemas.RECORD_EXPIRATION,
                     new RequiredTrait())
             .builderSupplier(JournalTableConfigurationUpdates::builder)
             .shapeClass(JournalTableConfigurationUpdates.class)
             .build();

    static final Schema UPDATE_BUCKET_METADATA_JOURNAL_TABLE_CONFIGURATION_INPUT = Schema.structureBuilder(ShapeId.from("com.amazonaws.s3#UpdateBucketMetadataJournalTableConfigurationRequest"))
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
             .putMember("JournalTableConfiguration", Schemas6.JOURNAL_TABLE_CONFIGURATION_UPDATES,
                     new XmlNameTrait("JournalTableConfiguration"),
                     new RequiredTrait(),
                     new HttpPayloadTrait())
             .putMember("ExpectedBucketOwner", Schemas.ACCOUNT_ID,
                     new HttpHeaderTrait("x-amz-expected-bucket-owner"))
             .builderSupplier(UpdateBucketMetadataJournalTableConfigurationInput::builder)
             .shapeClass(UpdateBucketMetadataJournalTableConfigurationInput.class)
             .build();

    static final Schema UPDATE_BUCKET_METADATA_JOURNAL_TABLE_CONFIGURATION_OUTPUT = Schema.structureBuilder(ShapeId.from("com.amazonaws.s3#UpdateBucketMetadataJournalTableConfigurationOutput"),
            new UnitTypeTrait()).builderSupplier(UpdateBucketMetadataJournalTableConfigurationOutput::builder)
             .shapeClass(UpdateBucketMetadataJournalTableConfigurationOutput.class)
             .build();

    static final Schema NON_EMPTY_KMS_KEY_ARN_STRING = Schema.createString(ShapeId.from("com.amazonaws.s3#NonEmptyKmsKeyArnString"),
            LengthTrait.builder().min(20L).max(2048L).build(),
            new PatternTrait("^arn:aws[a-zA-Z0-9-]*:kms:[a-z0-9-]+:[0-9]{12}:key/[a-zA-Z0-9-]+$"),
            new SensitiveTrait());

    private Schemas6() {}
}
