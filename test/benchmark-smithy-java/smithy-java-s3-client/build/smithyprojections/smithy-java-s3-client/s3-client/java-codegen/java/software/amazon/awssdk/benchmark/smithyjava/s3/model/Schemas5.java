package software.amazon.awssdk.benchmark.smithyjava.s3.model;

import software.amazon.smithy.java.core.schema.Schema;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.model.traits.DefaultTrait;
import software.amazon.smithy.model.traits.ErrorTrait;
import software.amazon.smithy.model.traits.HttpErrorTrait;
import software.amazon.smithy.model.traits.HttpHeaderTrait;
import software.amazon.smithy.model.traits.HttpLabelTrait;
import software.amazon.smithy.model.traits.HttpPayloadTrait;
import software.amazon.smithy.model.traits.HttpPrefixHeadersTrait;
import software.amazon.smithy.model.traits.HttpQueryTrait;
import software.amazon.smithy.model.traits.RequiredTrait;
import software.amazon.smithy.model.traits.UnitTypeTrait;
import software.amazon.smithy.model.traits.XmlFlattenedTrait;
import software.amazon.smithy.model.traits.XmlNameTrait;
import software.amazon.smithy.rulesengine.traits.ContextParamTrait;

/**
 * Defines schemas for shapes in the model package.
 */
final class Schemas5 {
    static final Schema LIST_OBJECT_VERSIONS_OUTPUT = Schema.structureBuilder(ShapeId.from("com.amazonaws.s3#ListObjectVersionsOutput"),
            new XmlNameTrait("ListVersionsResult"))
             .putMember("IsTruncated", Schemas3.IS_TRUNCATED)
             .putMember("KeyMarker", Schemas4.KEY_MARKER)
             .putMember("VersionIdMarker", Schemas4.VERSION_ID_MARKER)
             .putMember("NextKeyMarker", Schemas4.NEXT_KEY_MARKER)
             .putMember("NextVersionIdMarker", Schemas4.NEXT_VERSION_ID_MARKER)
             .putMember("Versions", Schemas4.OBJECT_VERSION_LIST,
                     new XmlNameTrait("Version"),
                     new XmlFlattenedTrait())
             .putMember("DeleteMarkers", Schemas4.DELETE_MARKERS,
                     new XmlNameTrait("DeleteMarker"),
                     new XmlFlattenedTrait())
             .putMember("Name", Schemas.BUCKET_NAME)
             .putMember("Prefix", Schemas1.PREFIX)
             .putMember("Delimiter", Schemas4.DELIMITER)
             .putMember("MaxKeys", Schemas4.MAX_KEYS)
             .putMember("CommonPrefixes", Schemas4.COMMON_PREFIX_LIST,
                     new XmlFlattenedTrait())
             .putMember("EncodingType", EncodingType.$SCHEMA)
             .putMember("RequestCharged", RequestCharged.$SCHEMA,
                     new HttpHeaderTrait("x-amz-request-charged"))
             .builderSupplier(ListObjectVersionsOutput::builder)
             .shapeClass(ListObjectVersionsOutput.class)
             .build();

    static final Schema LIST_PARTS_INPUT = Schema.structureBuilder(ShapeId.from("com.amazonaws.s3#ListPartsRequest"))
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
             .putMember("MaxParts", Schemas3.MAX_PARTS,
                     new HttpQueryTrait("max-parts"))
             .putMember("PartNumberMarker", Schemas3.PART_NUMBER_MARKER,
                     new HttpQueryTrait("part-number-marker"))
             .putMember("UploadId", Schemas.MULTIPART_UPLOAD_ID,
                     new RequiredTrait(),
                     new HttpQueryTrait("uploadId"))
             .putMember("RequestPayer", RequestPayer.$SCHEMA,
                     new HttpHeaderTrait("x-amz-request-payer"))
             .putMember("ExpectedBucketOwner", Schemas.ACCOUNT_ID,
                     new HttpHeaderTrait("x-amz-expected-bucket-owner"))
             .putMember("SSECustomerAlgorithm", Schemas.SSE_CUSTOMER_ALGORITHM,
                     new HttpHeaderTrait("x-amz-server-side-encryption-customer-algorithm"))
             .putMember("SSECustomerKey", Schemas.SSE_CUSTOMER_KEY,
                     new HttpHeaderTrait("x-amz-server-side-encryption-customer-key"))
             .putMember("SSECustomerKeyMD5", Schemas.SSE_CUSTOMER_KEY_MD5,
                     new HttpHeaderTrait("x-amz-server-side-encryption-customer-key-MD5"))
             .builderSupplier(ListPartsInput::builder)
             .shapeClass(ListPartsInput.class)
             .build();

    static final Schema PART = Schema.structureBuilder(ShapeId.from("com.amazonaws.s3#Part"))
             .putMember("PartNumber", Schemas.PART_NUMBER)
             .putMember("LastModified", Schemas.LAST_MODIFIED)
             .putMember("ETag", Schemas.E_TAG)
             .putMember("Size", Schemas1.SIZE)
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
             .builderSupplier(Part::builder)
             .shapeClass(Part.class)
             .build();

    static final Schema PARTS = Schema.listBuilder(ShapeId.from("com.amazonaws.s3#Parts"))
        .putMember("member", Schemas5.PART)
        .build();

    static final Schema LIST_PARTS_OUTPUT = Schema.structureBuilder(ShapeId.from("com.amazonaws.s3#ListPartsOutput"),
            new XmlNameTrait("ListPartsResult"))
             .putMember("AbortDate", Schemas.ABORT_DATE,
                     new HttpHeaderTrait("x-amz-abort-date"))
             .putMember("AbortRuleId", Schemas.ABORT_RULE_ID,
                     new HttpHeaderTrait("x-amz-abort-rule-id"))
             .putMember("Bucket", Schemas.BUCKET_NAME)
             .putMember("Key", Schemas.OBJECT_KEY)
             .putMember("UploadId", Schemas.MULTIPART_UPLOAD_ID)
             .putMember("PartNumberMarker", Schemas3.PART_NUMBER_MARKER)
             .putMember("NextPartNumberMarker", Schemas3.NEXT_PART_NUMBER_MARKER)
             .putMember("MaxParts", Schemas3.MAX_PARTS)
             .putMember("IsTruncated", Schemas3.IS_TRUNCATED)
             .putMember("Parts", Schemas5.PARTS,
                     new XmlNameTrait("Part"),
                     new XmlFlattenedTrait())
             .putMember("Initiator", Schemas4.INITIATOR)
             .putMember("Owner", Schemas.OWNER)
             .putMember("StorageClass", StorageClass.$SCHEMA)
             .putMember("RequestCharged", RequestCharged.$SCHEMA,
                     new HttpHeaderTrait("x-amz-request-charged"))
             .putMember("ChecksumAlgorithm", ChecksumAlgorithm.$SCHEMA)
             .putMember("ChecksumType", ChecksumType.$SCHEMA)
             .builderSupplier(ListPartsOutput::builder)
             .shapeClass(ListPartsOutput.class)
             .build();

    static final Schema PUT_BUCKET_ABAC_INPUT = Schema.structureBuilder(ShapeId.from("com.amazonaws.s3#PutBucketAbacRequest"))
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
             .putMember("ExpectedBucketOwner", Schemas.ACCOUNT_ID,
                     new HttpHeaderTrait("x-amz-expected-bucket-owner"))
             .putMember("AbacStatus", Schemas.ABAC_STATUS,
                     new XmlNameTrait("AbacStatus"),
                     new RequiredTrait(),
                     new HttpPayloadTrait())
             .builderSupplier(PutBucketAbacInput::builder)
             .shapeClass(PutBucketAbacInput.class)
             .build();

    static final Schema PUT_BUCKET_ABAC_OUTPUT = Schema.structureBuilder(ShapeId.from("com.amazonaws.s3#PutBucketAbacOutput"),
            new UnitTypeTrait()).builderSupplier(PutBucketAbacOutput::builder)
             .shapeClass(PutBucketAbacOutput.class)
             .build();

    static final Schema PUT_BUCKET_ACCELERATE_CONFIGURATION_INPUT = Schema.structureBuilder(ShapeId.from("com.amazonaws.s3#PutBucketAccelerateConfigurationRequest"))
             .putMember("Bucket", Schemas.BUCKET_NAME,
                     new ContextParamTrait.Provider().createTrait(
                         ShapeId.from("smithy.rules#contextParam"),
                         Node.objectNodeBuilder()
                             .withMember("name", "Bucket")
                             .build()
                     ),
                     new RequiredTrait(),
                     new HttpLabelTrait())
             .putMember("AccelerateConfiguration", Schemas.ACCELERATE_CONFIGURATION,
                     new XmlNameTrait("AccelerateConfiguration"),
                     new RequiredTrait(),
                     new HttpPayloadTrait())
             .putMember("ExpectedBucketOwner", Schemas.ACCOUNT_ID,
                     new HttpHeaderTrait("x-amz-expected-bucket-owner"))
             .putMember("ChecksumAlgorithm", ChecksumAlgorithm.$SCHEMA,
                     new HttpHeaderTrait("x-amz-sdk-checksum-algorithm"))
             .builderSupplier(PutBucketAccelerateConfigurationInput::builder)
             .shapeClass(PutBucketAccelerateConfigurationInput.class)
             .build();

    static final Schema PUT_BUCKET_ACCELERATE_CONFIGURATION_OUTPUT = Schema.structureBuilder(ShapeId.from("com.amazonaws.s3#PutBucketAccelerateConfigurationOutput"),
            new UnitTypeTrait()).builderSupplier(PutBucketAccelerateConfigurationOutput::builder)
             .shapeClass(PutBucketAccelerateConfigurationOutput.class)
             .build();

    static final Schema PUT_BUCKET_ACL_INPUT = Schema.structureBuilder(ShapeId.from("com.amazonaws.s3#PutBucketAclRequest"))
             .putMember("ACL", BucketCannedACL.$SCHEMA,
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
             .putMember("ExpectedBucketOwner", Schemas.ACCOUNT_ID,
                     new HttpHeaderTrait("x-amz-expected-bucket-owner"))
             .builderSupplier(PutBucketAclInput::builder)
             .shapeClass(PutBucketAclInput.class)
             .build();

    static final Schema PUT_BUCKET_ACL_OUTPUT = Schema.structureBuilder(ShapeId.from("com.amazonaws.s3#PutBucketAclOutput"),
            new UnitTypeTrait()).builderSupplier(PutBucketAclOutput::builder)
             .shapeClass(PutBucketAclOutput.class)
             .build();

    static final Schema PUT_BUCKET_ANALYTICS_CONFIGURATION_INPUT = Schema.structureBuilder(ShapeId.from("com.amazonaws.s3#PutBucketAnalyticsConfigurationRequest"))
             .putMember("Bucket", Schemas.BUCKET_NAME,
                     new ContextParamTrait.Provider().createTrait(
                         ShapeId.from("smithy.rules#contextParam"),
                         Node.objectNodeBuilder()
                             .withMember("name", "Bucket")
                             .build()
                     ),
                     new RequiredTrait(),
                     new HttpLabelTrait())
             .putMember("Id", Schemas1.ANALYTICS_ID,
                     new RequiredTrait(),
                     new HttpQueryTrait("id"))
             .putMember("AnalyticsConfiguration", Schemas2.ANALYTICS_CONFIGURATION,
                     new XmlNameTrait("AnalyticsConfiguration"),
                     new RequiredTrait(),
                     new HttpPayloadTrait())
             .putMember("ExpectedBucketOwner", Schemas.ACCOUNT_ID,
                     new HttpHeaderTrait("x-amz-expected-bucket-owner"))
             .builderSupplier(PutBucketAnalyticsConfigurationInput::builder)
             .shapeClass(PutBucketAnalyticsConfigurationInput.class)
             .build();

    static final Schema PUT_BUCKET_ANALYTICS_CONFIGURATION_OUTPUT = Schema.structureBuilder(ShapeId.from("com.amazonaws.s3#PutBucketAnalyticsConfigurationOutput"),
            new UnitTypeTrait()).builderSupplier(PutBucketAnalyticsConfigurationOutput::builder)
             .shapeClass(PutBucketAnalyticsConfigurationOutput.class)
             .build();

    static final Schema CORS_CONFIGURATION = Schema.structureBuilder(ShapeId.from("com.amazonaws.s3#CORSConfiguration"))
             .putMember("CORSRules", Schemas2.CORS_RULES,
                     new XmlNameTrait("CORSRule"),
                     new RequiredTrait(),
                     new XmlFlattenedTrait())
             .builderSupplier(CORSConfiguration::builder)
             .shapeClass(CORSConfiguration.class)
             .build();

    static final Schema PUT_BUCKET_CORS_INPUT = Schema.structureBuilder(ShapeId.from("com.amazonaws.s3#PutBucketCorsRequest"))
             .putMember("Bucket", Schemas.BUCKET_NAME,
                     new ContextParamTrait.Provider().createTrait(
                         ShapeId.from("smithy.rules#contextParam"),
                         Node.objectNodeBuilder()
                             .withMember("name", "Bucket")
                             .build()
                     ),
                     new RequiredTrait(),
                     new HttpLabelTrait())
             .putMember("CORSConfiguration", Schemas5.CORS_CONFIGURATION,
                     new XmlNameTrait("CORSConfiguration"),
                     new RequiredTrait(),
                     new HttpPayloadTrait())
             .putMember("ContentMD5", Schemas.CONTENT_MD5,
                     new HttpHeaderTrait("Content-MD5"))
             .putMember("ChecksumAlgorithm", ChecksumAlgorithm.$SCHEMA,
                     new HttpHeaderTrait("x-amz-sdk-checksum-algorithm"))
             .putMember("ExpectedBucketOwner", Schemas.ACCOUNT_ID,
                     new HttpHeaderTrait("x-amz-expected-bucket-owner"))
             .builderSupplier(PutBucketCorsInput::builder)
             .shapeClass(PutBucketCorsInput.class)
             .build();

    static final Schema PUT_BUCKET_CORS_OUTPUT = Schema.structureBuilder(ShapeId.from("com.amazonaws.s3#PutBucketCorsOutput"),
            new UnitTypeTrait()).builderSupplier(PutBucketCorsOutput::builder)
             .shapeClass(PutBucketCorsOutput.class)
             .build();

    static final Schema PUT_BUCKET_ENCRYPTION_INPUT = Schema.structureBuilder(ShapeId.from("com.amazonaws.s3#PutBucketEncryptionRequest"))
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
             .putMember("ServerSideEncryptionConfiguration", Schemas2.SERVER_SIDE_ENCRYPTION_CONFIGURATION,
                     new XmlNameTrait("ServerSideEncryptionConfiguration"),
                     new RequiredTrait(),
                     new HttpPayloadTrait())
             .putMember("ExpectedBucketOwner", Schemas.ACCOUNT_ID,
                     new HttpHeaderTrait("x-amz-expected-bucket-owner"))
             .builderSupplier(PutBucketEncryptionInput::builder)
             .shapeClass(PutBucketEncryptionInput.class)
             .build();

    static final Schema PUT_BUCKET_ENCRYPTION_OUTPUT = Schema.structureBuilder(ShapeId.from("com.amazonaws.s3#PutBucketEncryptionOutput"),
            new UnitTypeTrait()).builderSupplier(PutBucketEncryptionOutput::builder)
             .shapeClass(PutBucketEncryptionOutput.class)
             .build();

    static final Schema PUT_BUCKET_INTELLIGENT_TIERING_CONFIGURATION_INPUT = Schema.structureBuilder(ShapeId.from("com.amazonaws.s3#PutBucketIntelligentTieringConfigurationRequest"))
             .putMember("Bucket", Schemas.BUCKET_NAME,
                     new ContextParamTrait.Provider().createTrait(
                         ShapeId.from("smithy.rules#contextParam"),
                         Node.objectNodeBuilder()
                             .withMember("name", "Bucket")
                             .build()
                     ),
                     new RequiredTrait(),
                     new HttpLabelTrait())
             .putMember("Id", Schemas1.INTELLIGENT_TIERING_ID,
                     new RequiredTrait(),
                     new HttpQueryTrait("id"))
             .putMember("ExpectedBucketOwner", Schemas.ACCOUNT_ID,
                     new HttpHeaderTrait("x-amz-expected-bucket-owner"))
             .putMember("IntelligentTieringConfiguration", Schemas2.INTELLIGENT_TIERING_CONFIGURATION,
                     new XmlNameTrait("IntelligentTieringConfiguration"),
                     new RequiredTrait(),
                     new HttpPayloadTrait())
             .builderSupplier(PutBucketIntelligentTieringConfigurationInput::builder)
             .shapeClass(PutBucketIntelligentTieringConfigurationInput.class)
             .build();

    static final Schema PUT_BUCKET_INTELLIGENT_TIERING_CONFIGURATION_OUTPUT = Schema.structureBuilder(ShapeId.from("com.amazonaws.s3#PutBucketIntelligentTieringConfigurationOutput"),
            new UnitTypeTrait()).builderSupplier(PutBucketIntelligentTieringConfigurationOutput::builder)
             .shapeClass(PutBucketIntelligentTieringConfigurationOutput.class)
             .build();

    static final Schema PUT_BUCKET_INVENTORY_CONFIGURATION_INPUT = Schema.structureBuilder(ShapeId.from("com.amazonaws.s3#PutBucketInventoryConfigurationRequest"))
             .putMember("Bucket", Schemas.BUCKET_NAME,
                     new ContextParamTrait.Provider().createTrait(
                         ShapeId.from("smithy.rules#contextParam"),
                         Node.objectNodeBuilder()
                             .withMember("name", "Bucket")
                             .build()
                     ),
                     new RequiredTrait(),
                     new HttpLabelTrait())
             .putMember("Id", Schemas1.INVENTORY_ID,
                     new RequiredTrait(),
                     new HttpQueryTrait("id"))
             .putMember("InventoryConfiguration", Schemas2.INVENTORY_CONFIGURATION,
                     new XmlNameTrait("InventoryConfiguration"),
                     new RequiredTrait(),
                     new HttpPayloadTrait())
             .putMember("ExpectedBucketOwner", Schemas.ACCOUNT_ID,
                     new HttpHeaderTrait("x-amz-expected-bucket-owner"))
             .builderSupplier(PutBucketInventoryConfigurationInput::builder)
             .shapeClass(PutBucketInventoryConfigurationInput.class)
             .build();

    static final Schema PUT_BUCKET_INVENTORY_CONFIGURATION_OUTPUT = Schema.structureBuilder(ShapeId.from("com.amazonaws.s3#PutBucketInventoryConfigurationOutput"),
            new UnitTypeTrait()).builderSupplier(PutBucketInventoryConfigurationOutput::builder)
             .shapeClass(PutBucketInventoryConfigurationOutput.class)
             .build();

    static final Schema BUCKET_LIFECYCLE_CONFIGURATION = Schema.structureBuilder(ShapeId.from("com.amazonaws.s3#BucketLifecycleConfiguration"))
             .putMember("Rules", Schemas2.LIFECYCLE_RULES,
                     new XmlNameTrait("Rule"),
                     new RequiredTrait(),
                     new XmlFlattenedTrait())
             .builderSupplier(BucketLifecycleConfiguration::builder)
             .shapeClass(BucketLifecycleConfiguration.class)
             .build();

    static final Schema PUT_BUCKET_LIFECYCLE_CONFIGURATION_INPUT = Schema.structureBuilder(ShapeId.from("com.amazonaws.s3#PutBucketLifecycleConfigurationRequest"))
             .putMember("Bucket", Schemas.BUCKET_NAME,
                     new ContextParamTrait.Provider().createTrait(
                         ShapeId.from("smithy.rules#contextParam"),
                         Node.objectNodeBuilder()
                             .withMember("name", "Bucket")
                             .build()
                     ),
                     new RequiredTrait(),
                     new HttpLabelTrait())
             .putMember("ChecksumAlgorithm", ChecksumAlgorithm.$SCHEMA,
                     new HttpHeaderTrait("x-amz-sdk-checksum-algorithm"))
             .putMember("LifecycleConfiguration", Schemas5.BUCKET_LIFECYCLE_CONFIGURATION,
                     new XmlNameTrait("LifecycleConfiguration"),
                     new HttpPayloadTrait())
             .putMember("ExpectedBucketOwner", Schemas.ACCOUNT_ID,
                     new HttpHeaderTrait("x-amz-expected-bucket-owner"))
             .putMember("TransitionDefaultMinimumObjectSize", TransitionDefaultMinimumObjectSize.$SCHEMA,
                     new HttpHeaderTrait("x-amz-transition-default-minimum-object-size"))
             .builderSupplier(PutBucketLifecycleConfigurationInput::builder)
             .shapeClass(PutBucketLifecycleConfigurationInput.class)
             .build();

    static final Schema PUT_BUCKET_LIFECYCLE_CONFIGURATION_OUTPUT = Schema.structureBuilder(ShapeId.from("com.amazonaws.s3#PutBucketLifecycleConfigurationOutput"))
             .putMember("TransitionDefaultMinimumObjectSize", TransitionDefaultMinimumObjectSize.$SCHEMA,
                     new HttpHeaderTrait("x-amz-transition-default-minimum-object-size"))
             .builderSupplier(PutBucketLifecycleConfigurationOutput::builder)
             .shapeClass(PutBucketLifecycleConfigurationOutput.class)
             .build();

    static final Schema BUCKET_LOGGING_STATUS = Schema.structureBuilder(ShapeId.from("com.amazonaws.s3#BucketLoggingStatus"))
             .putMember("LoggingEnabled", Schemas2.LOGGING_ENABLED)
             .builderSupplier(BucketLoggingStatus::builder)
             .shapeClass(BucketLoggingStatus.class)
             .build();

    static final Schema PUT_BUCKET_LOGGING_INPUT = Schema.structureBuilder(ShapeId.from("com.amazonaws.s3#PutBucketLoggingRequest"))
             .putMember("Bucket", Schemas.BUCKET_NAME,
                     new ContextParamTrait.Provider().createTrait(
                         ShapeId.from("smithy.rules#contextParam"),
                         Node.objectNodeBuilder()
                             .withMember("name", "Bucket")
                             .build()
                     ),
                     new RequiredTrait(),
                     new HttpLabelTrait())
             .putMember("BucketLoggingStatus", Schemas5.BUCKET_LOGGING_STATUS,
                     new XmlNameTrait("BucketLoggingStatus"),
                     new RequiredTrait(),
                     new HttpPayloadTrait())
             .putMember("ContentMD5", Schemas.CONTENT_MD5,
                     new HttpHeaderTrait("Content-MD5"))
             .putMember("ChecksumAlgorithm", ChecksumAlgorithm.$SCHEMA,
                     new HttpHeaderTrait("x-amz-sdk-checksum-algorithm"))
             .putMember("ExpectedBucketOwner", Schemas.ACCOUNT_ID,
                     new HttpHeaderTrait("x-amz-expected-bucket-owner"))
             .builderSupplier(PutBucketLoggingInput::builder)
             .shapeClass(PutBucketLoggingInput.class)
             .build();

    static final Schema PUT_BUCKET_LOGGING_OUTPUT = Schema.structureBuilder(ShapeId.from("com.amazonaws.s3#PutBucketLoggingOutput"),
            new UnitTypeTrait()).builderSupplier(PutBucketLoggingOutput::builder)
             .shapeClass(PutBucketLoggingOutput.class)
             .build();

    static final Schema PUT_BUCKET_METRICS_CONFIGURATION_INPUT = Schema.structureBuilder(ShapeId.from("com.amazonaws.s3#PutBucketMetricsConfigurationRequest"))
             .putMember("Bucket", Schemas.BUCKET_NAME,
                     new ContextParamTrait.Provider().createTrait(
                         ShapeId.from("smithy.rules#contextParam"),
                         Node.objectNodeBuilder()
                             .withMember("name", "Bucket")
                             .build()
                     ),
                     new RequiredTrait(),
                     new HttpLabelTrait())
             .putMember("Id", Schemas1.METRICS_ID,
                     new RequiredTrait(),
                     new HttpQueryTrait("id"))
             .putMember("MetricsConfiguration", Schemas2.METRICS_CONFIGURATION,
                     new XmlNameTrait("MetricsConfiguration"),
                     new RequiredTrait(),
                     new HttpPayloadTrait())
             .putMember("ExpectedBucketOwner", Schemas.ACCOUNT_ID,
                     new HttpHeaderTrait("x-amz-expected-bucket-owner"))
             .builderSupplier(PutBucketMetricsConfigurationInput::builder)
             .shapeClass(PutBucketMetricsConfigurationInput.class)
             .build();

    static final Schema PUT_BUCKET_METRICS_CONFIGURATION_OUTPUT = Schema.structureBuilder(ShapeId.from("com.amazonaws.s3#PutBucketMetricsConfigurationOutput"),
            new UnitTypeTrait()).builderSupplier(PutBucketMetricsConfigurationOutput::builder)
             .shapeClass(PutBucketMetricsConfigurationOutput.class)
             .build();

    static final Schema NOTIFICATION_CONFIGURATION = Schema.structureBuilder(ShapeId.from("com.amazonaws.s3#NotificationConfiguration"))
             .putMember("TopicConfigurations", Schemas2.TOPIC_CONFIGURATION_LIST,
                     new XmlNameTrait("TopicConfiguration"),
                     new XmlFlattenedTrait())
             .putMember("QueueConfigurations", Schemas2.QUEUE_CONFIGURATION_LIST,
                     new XmlNameTrait("QueueConfiguration"),
                     new XmlFlattenedTrait())
             .putMember("LambdaFunctionConfigurations", Schemas2.LAMBDA_FUNCTION_CONFIGURATION_LIST,
                     new XmlNameTrait("CloudFunctionConfiguration"),
                     new XmlFlattenedTrait())
             .putMember("EventBridgeConfiguration", Schemas2.EVENT_BRIDGE_CONFIGURATION)
             .builderSupplier(NotificationConfiguration::builder)
             .shapeClass(NotificationConfiguration.class)
             .build();

    static final Schema SKIP_VALIDATION = Schema.createBoolean(ShapeId.from("com.amazonaws.s3#SkipValidation"));
    static final Schema PUT_BUCKET_NOTIFICATION_CONFIGURATION_INPUT = Schema.structureBuilder(ShapeId.from("com.amazonaws.s3#PutBucketNotificationConfigurationRequest"))
             .putMember("Bucket", Schemas.BUCKET_NAME,
                     new ContextParamTrait.Provider().createTrait(
                         ShapeId.from("smithy.rules#contextParam"),
                         Node.objectNodeBuilder()
                             .withMember("name", "Bucket")
                             .build()
                     ),
                     new RequiredTrait(),
                     new HttpLabelTrait())
             .putMember("NotificationConfiguration", Schemas5.NOTIFICATION_CONFIGURATION,
                     new XmlNameTrait("NotificationConfiguration"),
                     new RequiredTrait(),
                     new HttpPayloadTrait())
             .putMember("ExpectedBucketOwner", Schemas.ACCOUNT_ID,
                     new HttpHeaderTrait("x-amz-expected-bucket-owner"))
             .putMember("SkipDestinationValidation", Schemas5.SKIP_VALIDATION,
                     new HttpHeaderTrait("x-amz-skip-destination-validation"))
             .builderSupplier(PutBucketNotificationConfigurationInput::builder)
             .shapeClass(PutBucketNotificationConfigurationInput.class)
             .build();

    static final Schema PUT_BUCKET_NOTIFICATION_CONFIGURATION_OUTPUT = Schema.structureBuilder(ShapeId.from("com.amazonaws.s3#PutBucketNotificationConfigurationOutput"),
            new UnitTypeTrait()).builderSupplier(PutBucketNotificationConfigurationOutput::builder)
             .shapeClass(PutBucketNotificationConfigurationOutput.class)
             .build();

    static final Schema PUT_BUCKET_OWNERSHIP_CONTROLS_INPUT = Schema.structureBuilder(ShapeId.from("com.amazonaws.s3#PutBucketOwnershipControlsRequest"))
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
             .putMember("ExpectedBucketOwner", Schemas.ACCOUNT_ID,
                     new HttpHeaderTrait("x-amz-expected-bucket-owner"))
             .putMember("OwnershipControls", Schemas2.OWNERSHIP_CONTROLS,
                     new XmlNameTrait("OwnershipControls"),
                     new RequiredTrait(),
                     new HttpPayloadTrait())
             .putMember("ChecksumAlgorithm", ChecksumAlgorithm.$SCHEMA,
                     new HttpHeaderTrait("x-amz-sdk-checksum-algorithm"))
             .builderSupplier(PutBucketOwnershipControlsInput::builder)
             .shapeClass(PutBucketOwnershipControlsInput.class)
             .build();

    static final Schema PUT_BUCKET_OWNERSHIP_CONTROLS_OUTPUT = Schema.structureBuilder(ShapeId.from("com.amazonaws.s3#PutBucketOwnershipControlsOutput"),
            new UnitTypeTrait()).builderSupplier(PutBucketOwnershipControlsOutput::builder)
             .shapeClass(PutBucketOwnershipControlsOutput.class)
             .build();

    static final Schema CONFIRM_REMOVE_SELF_BUCKET_ACCESS = Schema.createBoolean(ShapeId.from("com.amazonaws.s3#ConfirmRemoveSelfBucketAccess"));
    static final Schema PUT_BUCKET_POLICY_INPUT = Schema.structureBuilder(ShapeId.from("com.amazonaws.s3#PutBucketPolicyRequest"))
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
             .putMember("ConfirmRemoveSelfBucketAccess", Schemas5.CONFIRM_REMOVE_SELF_BUCKET_ACCESS,
                     new HttpHeaderTrait("x-amz-confirm-remove-self-bucket-access"))
             .putMember("Policy", Schemas2.POLICY,
                     new RequiredTrait(),
                     new HttpPayloadTrait())
             .putMember("ExpectedBucketOwner", Schemas.ACCOUNT_ID,
                     new HttpHeaderTrait("x-amz-expected-bucket-owner"))
             .builderSupplier(PutBucketPolicyInput::builder)
             .shapeClass(PutBucketPolicyInput.class)
             .build();

    static final Schema PUT_BUCKET_POLICY_OUTPUT = Schema.structureBuilder(ShapeId.from("com.amazonaws.s3#PutBucketPolicyOutput"),
            new UnitTypeTrait()).builderSupplier(PutBucketPolicyOutput::builder)
             .shapeClass(PutBucketPolicyOutput.class)
             .build();

    static final Schema OBJECT_LOCK_TOKEN = Schema.createString(ShapeId.from("com.amazonaws.s3#ObjectLockToken"));
    static final Schema PUT_BUCKET_REPLICATION_INPUT = Schema.structureBuilder(ShapeId.from("com.amazonaws.s3#PutBucketReplicationRequest"))
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
             .putMember("ReplicationConfiguration", Schemas3.REPLICATION_CONFIGURATION,
                     new XmlNameTrait("ReplicationConfiguration"),
                     new RequiredTrait(),
                     new HttpPayloadTrait())
             .putMember("Token", Schemas5.OBJECT_LOCK_TOKEN,
                     new HttpHeaderTrait("x-amz-bucket-object-lock-token"))
             .putMember("ExpectedBucketOwner", Schemas.ACCOUNT_ID,
                     new HttpHeaderTrait("x-amz-expected-bucket-owner"))
             .builderSupplier(PutBucketReplicationInput::builder)
             .shapeClass(PutBucketReplicationInput.class)
             .build();

    static final Schema PUT_BUCKET_REPLICATION_OUTPUT = Schema.structureBuilder(ShapeId.from("com.amazonaws.s3#PutBucketReplicationOutput"),
            new UnitTypeTrait()).builderSupplier(PutBucketReplicationOutput::builder)
             .shapeClass(PutBucketReplicationOutput.class)
             .build();

    static final Schema REQUEST_PAYMENT_CONFIGURATION = Schema.structureBuilder(ShapeId.from("com.amazonaws.s3#RequestPaymentConfiguration"))
             .putMember("Payer", Payer.$SCHEMA,
                     new RequiredTrait())
             .builderSupplier(RequestPaymentConfiguration::builder)
             .shapeClass(RequestPaymentConfiguration.class)
             .build();

    static final Schema PUT_BUCKET_REQUEST_PAYMENT_INPUT = Schema.structureBuilder(ShapeId.from("com.amazonaws.s3#PutBucketRequestPaymentRequest"))
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
             .putMember("RequestPaymentConfiguration", Schemas5.REQUEST_PAYMENT_CONFIGURATION,
                     new XmlNameTrait("RequestPaymentConfiguration"),
                     new RequiredTrait(),
                     new HttpPayloadTrait())
             .putMember("ExpectedBucketOwner", Schemas.ACCOUNT_ID,
                     new HttpHeaderTrait("x-amz-expected-bucket-owner"))
             .builderSupplier(PutBucketRequestPaymentInput::builder)
             .shapeClass(PutBucketRequestPaymentInput.class)
             .build();

    static final Schema PUT_BUCKET_REQUEST_PAYMENT_OUTPUT = Schema.structureBuilder(ShapeId.from("com.amazonaws.s3#PutBucketRequestPaymentOutput"),
            new UnitTypeTrait()).builderSupplier(PutBucketRequestPaymentOutput::builder)
             .shapeClass(PutBucketRequestPaymentOutput.class)
             .build();

    static final Schema TAGGING = Schema.structureBuilder(ShapeId.from("com.amazonaws.s3#Tagging"))
             .putMember("TagSet", Schemas.TAG_SET,
                     new RequiredTrait())
             .builderSupplier(Tagging::builder)
             .shapeClass(Tagging.class)
             .build();

    static final Schema PUT_BUCKET_TAGGING_INPUT = Schema.structureBuilder(ShapeId.from("com.amazonaws.s3#PutBucketTaggingRequest"))
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
             .putMember("Tagging", Schemas5.TAGGING,
                     new XmlNameTrait("Tagging"),
                     new RequiredTrait(),
                     new HttpPayloadTrait())
             .putMember("ExpectedBucketOwner", Schemas.ACCOUNT_ID,
                     new HttpHeaderTrait("x-amz-expected-bucket-owner"))
             .builderSupplier(PutBucketTaggingInput::builder)
             .shapeClass(PutBucketTaggingInput.class)
             .build();

    static final Schema PUT_BUCKET_TAGGING_OUTPUT = Schema.structureBuilder(ShapeId.from("com.amazonaws.s3#PutBucketTaggingOutput"),
            new UnitTypeTrait()).builderSupplier(PutBucketTaggingOutput::builder)
             .shapeClass(PutBucketTaggingOutput.class)
             .build();

    static final Schema VERSIONING_CONFIGURATION = Schema.structureBuilder(ShapeId.from("com.amazonaws.s3#VersioningConfiguration"))
             .putMember("MFADelete", MFADelete.$SCHEMA,
                     new XmlNameTrait("MfaDelete"))
             .putMember("Status", BucketVersioningStatus.$SCHEMA)
             .builderSupplier(VersioningConfiguration::builder)
             .shapeClass(VersioningConfiguration.class)
             .build();

    static final Schema PUT_BUCKET_VERSIONING_INPUT = Schema.structureBuilder(ShapeId.from("com.amazonaws.s3#PutBucketVersioningRequest"))
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
             .putMember("MFA", Schemas1.COM_AMAZONAWS_S3_MFA,
                     new HttpHeaderTrait("x-amz-mfa"))
             .putMember("VersioningConfiguration", Schemas5.VERSIONING_CONFIGURATION,
                     new XmlNameTrait("VersioningConfiguration"),
                     new RequiredTrait(),
                     new HttpPayloadTrait())
             .putMember("ExpectedBucketOwner", Schemas.ACCOUNT_ID,
                     new HttpHeaderTrait("x-amz-expected-bucket-owner"))
             .builderSupplier(PutBucketVersioningInput::builder)
             .shapeClass(PutBucketVersioningInput.class)
             .build();

    static final Schema PUT_BUCKET_VERSIONING_OUTPUT = Schema.structureBuilder(ShapeId.from("com.amazonaws.s3#PutBucketVersioningOutput"),
            new UnitTypeTrait()).builderSupplier(PutBucketVersioningOutput::builder)
             .shapeClass(PutBucketVersioningOutput.class)
             .build();

    static final Schema WEBSITE_CONFIGURATION = Schema.structureBuilder(ShapeId.from("com.amazonaws.s3#WebsiteConfiguration"))
             .putMember("ErrorDocument", Schemas3.ERROR_DOCUMENT)
             .putMember("IndexDocument", Schemas3.INDEX_DOCUMENT)
             .putMember("RedirectAllRequestsTo", Schemas3.REDIRECT_ALL_REQUESTS_TO)
             .putMember("RoutingRules", Schemas3.ROUTING_RULES)
             .builderSupplier(WebsiteConfiguration::builder)
             .shapeClass(WebsiteConfiguration.class)
             .build();

    static final Schema PUT_BUCKET_WEBSITE_INPUT = Schema.structureBuilder(ShapeId.from("com.amazonaws.s3#PutBucketWebsiteRequest"))
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
             .putMember("WebsiteConfiguration", Schemas5.WEBSITE_CONFIGURATION,
                     new XmlNameTrait("WebsiteConfiguration"),
                     new RequiredTrait(),
                     new HttpPayloadTrait())
             .putMember("ExpectedBucketOwner", Schemas.ACCOUNT_ID,
                     new HttpHeaderTrait("x-amz-expected-bucket-owner"))
             .builderSupplier(PutBucketWebsiteInput::builder)
             .shapeClass(PutBucketWebsiteInput.class)
             .build();

    static final Schema PUT_BUCKET_WEBSITE_OUTPUT = Schema.structureBuilder(ShapeId.from("com.amazonaws.s3#PutBucketWebsiteOutput"),
            new UnitTypeTrait()).builderSupplier(PutBucketWebsiteOutput::builder)
             .shapeClass(PutBucketWebsiteOutput.class)
             .build();

    static final Schema ENCRYPTION_TYPE_MISMATCH = Schema.structureBuilder(ShapeId.from("com.amazonaws.s3#EncryptionTypeMismatch"),
            new ErrorTrait("client"),
            new HttpErrorTrait(400)).builderSupplier(EncryptionTypeMismatch::builder)
             .shapeClass(EncryptionTypeMismatch.class)
             .build();

    static final Schema INVALID_REQUEST = Schema.structureBuilder(ShapeId.from("com.amazonaws.s3#InvalidRequest"),
            new ErrorTrait("client"),
            new HttpErrorTrait(400)).builderSupplier(InvalidRequest::builder)
             .shapeClass(InvalidRequest.class)
             .build();

    static final Schema INVALID_WRITE_OFFSET = Schema.structureBuilder(ShapeId.from("com.amazonaws.s3#InvalidWriteOffset"),
            new ErrorTrait("client"),
            new HttpErrorTrait(400)).builderSupplier(InvalidWriteOffset::builder)
             .shapeClass(InvalidWriteOffset.class)
             .build();

    static final Schema WRITE_OFFSET_BYTES = Schema.createLong(ShapeId.from("com.amazonaws.s3#WriteOffsetBytes"));
    static final Schema PUT_OBJECT_INPUT = Schema.structureBuilder(ShapeId.from("com.amazonaws.s3#PutObjectRequest"))
             .putMember("ACL", ObjectCannedACL.$SCHEMA,
                     new HttpHeaderTrait("x-amz-acl"))
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
             .putMember("CacheControl", Schemas.CACHE_CONTROL,
                     new HttpHeaderTrait("Cache-Control"))
             .putMember("ContentDisposition", Schemas.CONTENT_DISPOSITION,
                     new HttpHeaderTrait("Content-Disposition"))
             .putMember("ContentEncoding", Schemas.CONTENT_ENCODING,
                     new HttpHeaderTrait("Content-Encoding"))
             .putMember("ContentLanguage", Schemas.CONTENT_LANGUAGE,
                     new HttpHeaderTrait("Content-Language"))
             .putMember("ContentLength", Schemas3.CONTENT_LENGTH,
                     new HttpHeaderTrait("Content-Length"))
             .putMember("ContentMD5", Schemas.CONTENT_MD5,
                     new HttpHeaderTrait("Content-MD5"))
             .putMember("ContentType", Schemas.CONTENT_TYPE,
                     new HttpHeaderTrait("Content-Type"))
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
             .putMember("Expires", Schemas.EXPIRES,
                     new HttpHeaderTrait("Expires"))
             .putMember("IfMatch", Schemas.IF_MATCH,
                     new HttpHeaderTrait("If-Match"))
             .putMember("IfNoneMatch", Schemas.IF_NONE_MATCH,
                     new HttpHeaderTrait("If-None-Match"))
             .putMember("GrantFullControl", Schemas.GRANT_FULL_CONTROL,
                     new HttpHeaderTrait("x-amz-grant-full-control"))
             .putMember("GrantRead", Schemas.GRANT_READ,
                     new HttpHeaderTrait("x-amz-grant-read"))
             .putMember("GrantReadACP", Schemas.GRANT_READ_ACP,
                     new HttpHeaderTrait("x-amz-grant-read-acp"))
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
             .putMember("WriteOffsetBytes", Schemas5.WRITE_OFFSET_BYTES,
                     new HttpHeaderTrait("x-amz-write-offset-bytes"))
             .putMember("Metadata", Schemas.METADATA,
                     new HttpPrefixHeadersTrait("x-amz-meta-"))
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
             .builderSupplier(PutObjectInput::builder)
             .shapeClass(PutObjectInput.class)
             .build();

    private Schemas5() {}
}
