package software.amazon.awssdk.benchmark.smithyjava.s3.model;

import java.math.BigDecimal;
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
import software.amazon.smithy.model.traits.LengthTrait;
import software.amazon.smithy.model.traits.RangeTrait;
import software.amazon.smithy.model.traits.RequiredTrait;
import software.amazon.smithy.model.traits.XmlFlattenedTrait;
import software.amazon.smithy.model.traits.XmlNameTrait;
import software.amazon.smithy.rulesengine.traits.ContextParamTrait;

/**
 * Defines schemas for shapes in the model package.
 */
final class Schemas4 {
    static final Schema GET_OBJECT_TAGGING_OUTPUT = Schema.structureBuilder(ShapeId.from("com.amazonaws.s3#GetObjectTaggingOutput"),
            new XmlNameTrait("Tagging"))
             .putMember("VersionId", Schemas.OBJECT_VERSION_ID,
                     new HttpHeaderTrait("x-amz-version-id"))
             .putMember("TagSet", Schemas.TAG_SET,
                     new RequiredTrait())
             .builderSupplier(GetObjectTaggingOutput::builder)
             .shapeClass(GetObjectTaggingOutput.class)
             .build();

    static final Schema GET_OBJECT_TORRENT_INPUT = Schema.structureBuilder(ShapeId.from("com.amazonaws.s3#GetObjectTorrentRequest"))
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
             .putMember("RequestPayer", RequestPayer.$SCHEMA,
                     new HttpHeaderTrait("x-amz-request-payer"))
             .putMember("ExpectedBucketOwner", Schemas.ACCOUNT_ID,
                     new HttpHeaderTrait("x-amz-expected-bucket-owner"))
             .builderSupplier(GetObjectTorrentInput::builder)
             .shapeClass(GetObjectTorrentInput.class)
             .build();

    static final Schema GET_OBJECT_TORRENT_OUTPUT = Schema.structureBuilder(ShapeId.from("com.amazonaws.s3#GetObjectTorrentOutput"))
             .putMember("Body", Schemas3.STREAMING_BLOB,
                     new DefaultTrait(Node.from("")),
                     new HttpPayloadTrait())
             .putMember("RequestCharged", RequestCharged.$SCHEMA,
                     new HttpHeaderTrait("x-amz-request-charged"))
             .builderSupplier(GetObjectTorrentOutput::builder)
             .shapeClass(GetObjectTorrentOutput.class)
             .build();

    static final Schema GET_PUBLIC_ACCESS_BLOCK_INPUT = Schema.structureBuilder(ShapeId.from("com.amazonaws.s3#GetPublicAccessBlockRequest"))
             .putMember("Bucket", Schemas.BUCKET_NAME,
                     new ContextParamTrait.Provider().createTrait(
                         ShapeId.from("smithy.rules#contextParam"),
                         Node.objectNodeBuilder()
                             .withMember("name", "Bucket")
                             .build()
                     ),
                     new RequiredTrait(),
                     new HttpLabelTrait())
             .putMember("ExpectedBucketOwner", Schemas.ACCOUNT_ID,
                     new HttpHeaderTrait("x-amz-expected-bucket-owner"))
             .builderSupplier(GetPublicAccessBlockInput::builder)
             .shapeClass(GetPublicAccessBlockInput.class)
             .build();

    static final Schema SETTING = Schema.createBoolean(ShapeId.from("com.amazonaws.s3#Setting"));
    static final Schema PUBLIC_ACCESS_BLOCK_CONFIGURATION = Schema.structureBuilder(ShapeId.from("com.amazonaws.s3#PublicAccessBlockConfiguration"))
             .putMember("BlockPublicAcls", Schemas4.SETTING,
                     new XmlNameTrait("BlockPublicAcls"))
             .putMember("IgnorePublicAcls", Schemas4.SETTING,
                     new XmlNameTrait("IgnorePublicAcls"))
             .putMember("BlockPublicPolicy", Schemas4.SETTING,
                     new XmlNameTrait("BlockPublicPolicy"))
             .putMember("RestrictPublicBuckets", Schemas4.SETTING,
                     new XmlNameTrait("RestrictPublicBuckets"))
             .builderSupplier(PublicAccessBlockConfiguration::builder)
             .shapeClass(PublicAccessBlockConfiguration.class)
             .build();

    static final Schema GET_PUBLIC_ACCESS_BLOCK_OUTPUT = Schema.structureBuilder(ShapeId.from("com.amazonaws.s3#GetPublicAccessBlockOutput"))
             .putMember("PublicAccessBlockConfiguration", Schemas4.PUBLIC_ACCESS_BLOCK_CONFIGURATION,
                     new HttpPayloadTrait())
             .builderSupplier(GetPublicAccessBlockOutput::builder)
             .shapeClass(GetPublicAccessBlockOutput.class)
             .build();

    static final Schema HEAD_BUCKET_INPUT = Schema.structureBuilder(ShapeId.from("com.amazonaws.s3#HeadBucketRequest"))
             .putMember("Bucket", Schemas.BUCKET_NAME,
                     new ContextParamTrait.Provider().createTrait(
                         ShapeId.from("smithy.rules#contextParam"),
                         Node.objectNodeBuilder()
                             .withMember("name", "Bucket")
                             .build()
                     ),
                     new RequiredTrait(),
                     new HttpLabelTrait())
             .putMember("ExpectedBucketOwner", Schemas.ACCOUNT_ID,
                     new HttpHeaderTrait("x-amz-expected-bucket-owner"))
             .builderSupplier(HeadBucketInput::builder)
             .shapeClass(HeadBucketInput.class)
             .build();

    static final Schema BUCKET_LOCATION_NAME = Schema.createString(ShapeId.from("com.amazonaws.s3#BucketLocationName"));
    static final Schema REGION = Schema.createString(ShapeId.from("com.amazonaws.s3#Region"),
            LengthTrait.builder().min(0L).max(20L).build());
    static final Schema HEAD_BUCKET_OUTPUT = Schema.structureBuilder(ShapeId.from("com.amazonaws.s3#HeadBucketOutput"))
             .putMember("BucketArn", Schemas.S3_REGIONAL_OR_S3_EXPRESS_BUCKET_ARN_STRING,
                     new HttpHeaderTrait("x-amz-bucket-arn"))
             .putMember("BucketLocationType", LocationType.$SCHEMA,
                     new HttpHeaderTrait("x-amz-bucket-location-type"))
             .putMember("BucketLocationName", Schemas4.BUCKET_LOCATION_NAME,
                     new HttpHeaderTrait("x-amz-bucket-location-name"))
             .putMember("BucketRegion", Schemas4.REGION,
                     new HttpHeaderTrait("x-amz-bucket-region"))
             .putMember("AccessPointAlias", Schemas.ACCESS_POINT_ALIAS,
                     new HttpHeaderTrait("x-amz-access-point-alias"))
             .builderSupplier(HeadBucketOutput::builder)
             .shapeClass(HeadBucketOutput.class)
             .build();

    static final Schema NOT_FOUND = Schema.structureBuilder(ShapeId.from("com.amazonaws.s3#NotFound"),
            new ErrorTrait("client")).builderSupplier(NotFound::builder)
             .shapeClass(NotFound.class)
             .build();

    static final Schema HEAD_OBJECT_INPUT = Schema.structureBuilder(ShapeId.from("com.amazonaws.s3#HeadObjectRequest"))
             .putMember("Bucket", Schemas.BUCKET_NAME,
                     new ContextParamTrait.Provider().createTrait(
                         ShapeId.from("smithy.rules#contextParam"),
                         Node.objectNodeBuilder()
                             .withMember("name", "Bucket")
                             .build()
                     ),
                     new RequiredTrait(),
                     new HttpLabelTrait())
             .putMember("IfMatch", Schemas.IF_MATCH,
                     new HttpHeaderTrait("If-Match"))
             .putMember("IfModifiedSince", Schemas3.IF_MODIFIED_SINCE,
                     new HttpHeaderTrait("If-Modified-Since"))
             .putMember("IfNoneMatch", Schemas.IF_NONE_MATCH,
                     new HttpHeaderTrait("If-None-Match"))
             .putMember("IfUnmodifiedSince", Schemas3.IF_UNMODIFIED_SINCE,
                     new HttpHeaderTrait("If-Unmodified-Since"))
             .putMember("Key", Schemas.OBJECT_KEY,
                     new ContextParamTrait.Provider().createTrait(
                         ShapeId.from("smithy.rules#contextParam"),
                         Node.objectNodeBuilder()
                             .withMember("name", "Key")
                             .build()
                     ),
                     new RequiredTrait(),
                     new HttpLabelTrait())
             .putMember("Range", Schemas3.RANGE,
                     new HttpHeaderTrait("Range"))
             .putMember("ResponseCacheControl", Schemas3.RESPONSE_CACHE_CONTROL,
                     new HttpQueryTrait("response-cache-control"))
             .putMember("ResponseContentDisposition", Schemas3.RESPONSE_CONTENT_DISPOSITION,
                     new HttpQueryTrait("response-content-disposition"))
             .putMember("ResponseContentEncoding", Schemas3.RESPONSE_CONTENT_ENCODING,
                     new HttpQueryTrait("response-content-encoding"))
             .putMember("ResponseContentLanguage", Schemas3.RESPONSE_CONTENT_LANGUAGE,
                     new HttpQueryTrait("response-content-language"))
             .putMember("ResponseContentType", Schemas3.RESPONSE_CONTENT_TYPE,
                     new HttpQueryTrait("response-content-type"))
             .putMember("ResponseExpires", Schemas3.RESPONSE_EXPIRES,
                     new HttpQueryTrait("response-expires"))
             .putMember("VersionId", Schemas.OBJECT_VERSION_ID,
                     new HttpQueryTrait("versionId"))
             .putMember("SSECustomerAlgorithm", Schemas.SSE_CUSTOMER_ALGORITHM,
                     new HttpHeaderTrait("x-amz-server-side-encryption-customer-algorithm"))
             .putMember("SSECustomerKey", Schemas.SSE_CUSTOMER_KEY,
                     new HttpHeaderTrait("x-amz-server-side-encryption-customer-key"))
             .putMember("SSECustomerKeyMD5", Schemas.SSE_CUSTOMER_KEY_MD5,
                     new HttpHeaderTrait("x-amz-server-side-encryption-customer-key-MD5"))
             .putMember("RequestPayer", RequestPayer.$SCHEMA,
                     new HttpHeaderTrait("x-amz-request-payer"))
             .putMember("PartNumber", Schemas.PART_NUMBER,
                     new HttpQueryTrait("partNumber"))
             .putMember("ExpectedBucketOwner", Schemas.ACCOUNT_ID,
                     new HttpHeaderTrait("x-amz-expected-bucket-owner"))
             .putMember("ChecksumMode", ChecksumMode.$SCHEMA,
                     new HttpHeaderTrait("x-amz-checksum-mode"))
             .builderSupplier(HeadObjectInput::builder)
             .shapeClass(HeadObjectInput.class)
             .build();

    static final Schema HEAD_OBJECT_OUTPUT = Schema.structureBuilder(ShapeId.from("com.amazonaws.s3#HeadObjectOutput"))
             .putMember("DeleteMarker", Schemas1.DELETE_MARKER,
                     new HttpHeaderTrait("x-amz-delete-marker"))
             .putMember("AcceptRanges", Schemas.ACCEPT_RANGES,
                     new HttpHeaderTrait("accept-ranges"))
             .putMember("Expiration", Schemas.EXPIRATION,
                     new HttpHeaderTrait("x-amz-expiration"))
             .putMember("Restore", Schemas3.RESTORE,
                     new HttpHeaderTrait("x-amz-restore"))
             .putMember("ArchiveStatus", ArchiveStatus.$SCHEMA,
                     new HttpHeaderTrait("x-amz-archive-status"))
             .putMember("LastModified", Schemas.LAST_MODIFIED,
                     new HttpHeaderTrait("Last-Modified"))
             .putMember("ContentLength", Schemas3.CONTENT_LENGTH,
                     new HttpHeaderTrait("Content-Length"))
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
             .putMember("ETag", Schemas.E_TAG,
                     new HttpHeaderTrait("ETag"))
             .putMember("MissingMeta", Schemas3.MISSING_META,
                     new HttpHeaderTrait("x-amz-missing-meta"))
             .putMember("VersionId", Schemas.OBJECT_VERSION_ID,
                     new HttpHeaderTrait("x-amz-version-id"))
             .putMember("CacheControl", Schemas.CACHE_CONTROL,
                     new HttpHeaderTrait("Cache-Control"))
             .putMember("ContentDisposition", Schemas.CONTENT_DISPOSITION,
                     new HttpHeaderTrait("Content-Disposition"))
             .putMember("ContentEncoding", Schemas.CONTENT_ENCODING,
                     new HttpHeaderTrait("Content-Encoding"))
             .putMember("ContentLanguage", Schemas.CONTENT_LANGUAGE,
                     new HttpHeaderTrait("Content-Language"))
             .putMember("ContentType", Schemas.CONTENT_TYPE,
                     new HttpHeaderTrait("Content-Type"))
             .putMember("ContentRange", Schemas3.CONTENT_RANGE,
                     new HttpHeaderTrait("Content-Range"))
             .putMember("Expires", Schemas.EXPIRES,
                     new HttpHeaderTrait("Expires"))
             .putMember("WebsiteRedirectLocation", Schemas.WEBSITE_REDIRECT_LOCATION,
                     new HttpHeaderTrait("x-amz-website-redirect-location"))
             .putMember("ServerSideEncryption", ServerSideEncryption.$SCHEMA,
                     new HttpHeaderTrait("x-amz-server-side-encryption"))
             .putMember("Metadata", Schemas.METADATA,
                     new HttpPrefixHeadersTrait("x-amz-meta-"))
             .putMember("SSECustomerAlgorithm", Schemas.SSE_CUSTOMER_ALGORITHM,
                     new HttpHeaderTrait("x-amz-server-side-encryption-customer-algorithm"))
             .putMember("SSECustomerKeyMD5", Schemas.SSE_CUSTOMER_KEY_MD5,
                     new HttpHeaderTrait("x-amz-server-side-encryption-customer-key-MD5"))
             .putMember("SSEKMSKeyId", Schemas.SSEKMS_KEY_ID,
                     new HttpHeaderTrait("x-amz-server-side-encryption-aws-kms-key-id"))
             .putMember("BucketKeyEnabled", Schemas.BUCKET_KEY_ENABLED,
                     new HttpHeaderTrait("x-amz-server-side-encryption-bucket-key-enabled"))
             .putMember("StorageClass", StorageClass.$SCHEMA,
                     new HttpHeaderTrait("x-amz-storage-class"))
             .putMember("RequestCharged", RequestCharged.$SCHEMA,
                     new HttpHeaderTrait("x-amz-request-charged"))
             .putMember("ReplicationStatus", ReplicationStatus.$SCHEMA,
                     new HttpHeaderTrait("x-amz-replication-status"))
             .putMember("PartsCount", Schemas3.PARTS_COUNT,
                     new HttpHeaderTrait("x-amz-mp-parts-count"))
             .putMember("TagCount", Schemas3.TAG_COUNT,
                     new HttpHeaderTrait("x-amz-tagging-count"))
             .putMember("ObjectLockMode", ObjectLockMode.$SCHEMA,
                     new HttpHeaderTrait("x-amz-object-lock-mode"))
             .putMember("ObjectLockRetainUntilDate", Schemas.OBJECT_LOCK_RETAIN_UNTIL_DATE,
                     new HttpHeaderTrait("x-amz-object-lock-retain-until-date"))
             .putMember("ObjectLockLegalHoldStatus", ObjectLockLegalHoldStatus.$SCHEMA,
                     new HttpHeaderTrait("x-amz-object-lock-legal-hold"))
             .builderSupplier(HeadObjectOutput::builder)
             .shapeClass(HeadObjectOutput.class)
             .build();

    static final Schema TOKEN = Schema.createString(ShapeId.from("com.amazonaws.s3#Token"));
    static final Schema LIST_BUCKET_ANALYTICS_CONFIGURATIONS_INPUT = Schema.structureBuilder(ShapeId.from("com.amazonaws.s3#ListBucketAnalyticsConfigurationsRequest"))
             .putMember("Bucket", Schemas.BUCKET_NAME,
                     new ContextParamTrait.Provider().createTrait(
                         ShapeId.from("smithy.rules#contextParam"),
                         Node.objectNodeBuilder()
                             .withMember("name", "Bucket")
                             .build()
                     ),
                     new RequiredTrait(),
                     new HttpLabelTrait())
             .putMember("ContinuationToken", Schemas4.TOKEN,
                     new HttpQueryTrait("continuation-token"))
             .putMember("ExpectedBucketOwner", Schemas.ACCOUNT_ID,
                     new HttpHeaderTrait("x-amz-expected-bucket-owner"))
             .builderSupplier(ListBucketAnalyticsConfigurationsInput::builder)
             .shapeClass(ListBucketAnalyticsConfigurationsInput.class)
             .build();

    static final Schema ANALYTICS_CONFIGURATION_LIST = Schema.listBuilder(ShapeId.from("com.amazonaws.s3#AnalyticsConfigurationList"))
        .putMember("member", Schemas2.ANALYTICS_CONFIGURATION)
        .build();

    static final Schema NEXT_TOKEN = Schema.createString(ShapeId.from("com.amazonaws.s3#NextToken"));
    static final Schema LIST_BUCKET_ANALYTICS_CONFIGURATIONS_OUTPUT = Schema.structureBuilder(ShapeId.from("com.amazonaws.s3#ListBucketAnalyticsConfigurationsOutput"),
            new XmlNameTrait("ListBucketAnalyticsConfigurationResult"))
             .putMember("IsTruncated", Schemas3.IS_TRUNCATED)
             .putMember("ContinuationToken", Schemas4.TOKEN)
             .putMember("NextContinuationToken", Schemas4.NEXT_TOKEN)
             .putMember("AnalyticsConfigurationList", Schemas4.ANALYTICS_CONFIGURATION_LIST,
                     new XmlNameTrait("AnalyticsConfiguration"),
                     new XmlFlattenedTrait())
             .builderSupplier(ListBucketAnalyticsConfigurationsOutput::builder)
             .shapeClass(ListBucketAnalyticsConfigurationsOutput.class)
             .build();

    static final Schema LIST_BUCKET_INTELLIGENT_TIERING_CONFIGURATIONS_INPUT = Schema.structureBuilder(ShapeId.from("com.amazonaws.s3#ListBucketIntelligentTieringConfigurationsRequest"))
             .putMember("Bucket", Schemas.BUCKET_NAME,
                     new ContextParamTrait.Provider().createTrait(
                         ShapeId.from("smithy.rules#contextParam"),
                         Node.objectNodeBuilder()
                             .withMember("name", "Bucket")
                             .build()
                     ),
                     new RequiredTrait(),
                     new HttpLabelTrait())
             .putMember("ContinuationToken", Schemas4.TOKEN,
                     new HttpQueryTrait("continuation-token"))
             .putMember("ExpectedBucketOwner", Schemas.ACCOUNT_ID,
                     new HttpHeaderTrait("x-amz-expected-bucket-owner"))
             .builderSupplier(ListBucketIntelligentTieringConfigurationsInput::builder)
             .shapeClass(ListBucketIntelligentTieringConfigurationsInput.class)
             .build();

    static final Schema INTELLIGENT_TIERING_CONFIGURATION_LIST = Schema.listBuilder(ShapeId.from("com.amazonaws.s3#IntelligentTieringConfigurationList"))
        .putMember("member", Schemas2.INTELLIGENT_TIERING_CONFIGURATION)
        .build();

    static final Schema LIST_BUCKET_INTELLIGENT_TIERING_CONFIGURATIONS_OUTPUT = Schema.structureBuilder(ShapeId.from("com.amazonaws.s3#ListBucketIntelligentTieringConfigurationsOutput"))
             .putMember("IsTruncated", Schemas3.IS_TRUNCATED)
             .putMember("ContinuationToken", Schemas4.TOKEN)
             .putMember("NextContinuationToken", Schemas4.NEXT_TOKEN)
             .putMember("IntelligentTieringConfigurationList", Schemas4.INTELLIGENT_TIERING_CONFIGURATION_LIST,
                     new XmlNameTrait("IntelligentTieringConfiguration"),
                     new XmlFlattenedTrait())
             .builderSupplier(ListBucketIntelligentTieringConfigurationsOutput::builder)
             .shapeClass(ListBucketIntelligentTieringConfigurationsOutput.class)
             .build();

    static final Schema LIST_BUCKET_INVENTORY_CONFIGURATIONS_INPUT = Schema.structureBuilder(ShapeId.from("com.amazonaws.s3#ListBucketInventoryConfigurationsRequest"))
             .putMember("Bucket", Schemas.BUCKET_NAME,
                     new ContextParamTrait.Provider().createTrait(
                         ShapeId.from("smithy.rules#contextParam"),
                         Node.objectNodeBuilder()
                             .withMember("name", "Bucket")
                             .build()
                     ),
                     new RequiredTrait(),
                     new HttpLabelTrait())
             .putMember("ContinuationToken", Schemas4.TOKEN,
                     new HttpQueryTrait("continuation-token"))
             .putMember("ExpectedBucketOwner", Schemas.ACCOUNT_ID,
                     new HttpHeaderTrait("x-amz-expected-bucket-owner"))
             .builderSupplier(ListBucketInventoryConfigurationsInput::builder)
             .shapeClass(ListBucketInventoryConfigurationsInput.class)
             .build();

    static final Schema INVENTORY_CONFIGURATION_LIST = Schema.listBuilder(ShapeId.from("com.amazonaws.s3#InventoryConfigurationList"))
        .putMember("member", Schemas2.INVENTORY_CONFIGURATION)
        .build();

    static final Schema LIST_BUCKET_INVENTORY_CONFIGURATIONS_OUTPUT = Schema.structureBuilder(ShapeId.from("com.amazonaws.s3#ListBucketInventoryConfigurationsOutput"),
            new XmlNameTrait("ListInventoryConfigurationsResult"))
             .putMember("ContinuationToken", Schemas4.TOKEN)
             .putMember("InventoryConfigurationList", Schemas4.INVENTORY_CONFIGURATION_LIST,
                     new XmlNameTrait("InventoryConfiguration"),
                     new XmlFlattenedTrait())
             .putMember("IsTruncated", Schemas3.IS_TRUNCATED)
             .putMember("NextContinuationToken", Schemas4.NEXT_TOKEN)
             .builderSupplier(ListBucketInventoryConfigurationsOutput::builder)
             .shapeClass(ListBucketInventoryConfigurationsOutput.class)
             .build();

    static final Schema LIST_BUCKET_METRICS_CONFIGURATIONS_INPUT = Schema.structureBuilder(ShapeId.from("com.amazonaws.s3#ListBucketMetricsConfigurationsRequest"))
             .putMember("Bucket", Schemas.BUCKET_NAME,
                     new ContextParamTrait.Provider().createTrait(
                         ShapeId.from("smithy.rules#contextParam"),
                         Node.objectNodeBuilder()
                             .withMember("name", "Bucket")
                             .build()
                     ),
                     new RequiredTrait(),
                     new HttpLabelTrait())
             .putMember("ContinuationToken", Schemas4.TOKEN,
                     new HttpQueryTrait("continuation-token"))
             .putMember("ExpectedBucketOwner", Schemas.ACCOUNT_ID,
                     new HttpHeaderTrait("x-amz-expected-bucket-owner"))
             .builderSupplier(ListBucketMetricsConfigurationsInput::builder)
             .shapeClass(ListBucketMetricsConfigurationsInput.class)
             .build();

    static final Schema METRICS_CONFIGURATION_LIST = Schema.listBuilder(ShapeId.from("com.amazonaws.s3#MetricsConfigurationList"))
        .putMember("member", Schemas2.METRICS_CONFIGURATION)
        .build();

    static final Schema LIST_BUCKET_METRICS_CONFIGURATIONS_OUTPUT = Schema.structureBuilder(ShapeId.from("com.amazonaws.s3#ListBucketMetricsConfigurationsOutput"),
            new XmlNameTrait("ListMetricsConfigurationsResult"))
             .putMember("IsTruncated", Schemas3.IS_TRUNCATED)
             .putMember("ContinuationToken", Schemas4.TOKEN)
             .putMember("NextContinuationToken", Schemas4.NEXT_TOKEN)
             .putMember("MetricsConfigurationList", Schemas4.METRICS_CONFIGURATION_LIST,
                     new XmlNameTrait("MetricsConfiguration"),
                     new XmlFlattenedTrait())
             .builderSupplier(ListBucketMetricsConfigurationsOutput::builder)
             .shapeClass(ListBucketMetricsConfigurationsOutput.class)
             .build();

    static final Schema BUCKET_REGION = Schema.createString(ShapeId.from("com.amazonaws.s3#BucketRegion"));
    static final Schema MAX_BUCKETS = Schema.createInteger(ShapeId.from("com.amazonaws.s3#MaxBuckets"),
            RangeTrait.builder().min(new BigDecimal("1")).max(new BigDecimal("10000")).build());
    static final Schema LIST_BUCKETS_INPUT = Schema.structureBuilder(ShapeId.from("com.amazonaws.s3#ListBucketsRequest"))
             .putMember("MaxBuckets", Schemas4.MAX_BUCKETS,
                     new HttpQueryTrait("max-buckets"))
             .putMember("ContinuationToken", Schemas4.TOKEN,
                     new HttpQueryTrait("continuation-token"))
             .putMember("Prefix", Schemas1.PREFIX,
                     new HttpQueryTrait("prefix"))
             .putMember("BucketRegion", Schemas4.BUCKET_REGION,
                     new HttpQueryTrait("bucket-region"))
             .builderSupplier(ListBucketsInput::builder)
             .shapeClass(ListBucketsInput.class)
             .build();

    static final Schema CREATION_DATE = Schema.createTimestamp(ShapeId.from("com.amazonaws.s3#CreationDate"));
    static final Schema BUCKET = Schema.structureBuilder(ShapeId.from("com.amazonaws.s3#Bucket"))
             .putMember("Name", Schemas.BUCKET_NAME)
             .putMember("CreationDate", Schemas4.CREATION_DATE)
             .putMember("BucketRegion", Schemas4.BUCKET_REGION)
             .putMember("BucketArn", Schemas.S3_REGIONAL_OR_S3_EXPRESS_BUCKET_ARN_STRING)
             .builderSupplier(Bucket::builder)
             .shapeClass(Bucket.class)
             .build();

    static final Schema BUCKETS = Schema.listBuilder(ShapeId.from("com.amazonaws.s3#Buckets"))
        .putMember("member", Schemas4.BUCKET,
                new XmlNameTrait("Bucket"))
        .build();

    static final Schema LIST_BUCKETS_OUTPUT = Schema.structureBuilder(ShapeId.from("com.amazonaws.s3#ListBucketsOutput"),
            new XmlNameTrait("ListAllMyBucketsResult"))
             .putMember("Buckets", Schemas4.BUCKETS)
             .putMember("Owner", Schemas.OWNER)
             .putMember("ContinuationToken", Schemas4.NEXT_TOKEN)
             .putMember("Prefix", Schemas1.PREFIX)
             .builderSupplier(ListBucketsOutput::builder)
             .shapeClass(ListBucketsOutput.class)
             .build();

    static final Schema DIRECTORY_BUCKET_TOKEN = Schema.createString(ShapeId.from("com.amazonaws.s3#DirectoryBucketToken"),
            LengthTrait.builder().min(0L).max(1024L).build());
    static final Schema MAX_DIRECTORY_BUCKETS = Schema.createInteger(ShapeId.from("com.amazonaws.s3#MaxDirectoryBuckets"),
            RangeTrait.builder().min(new BigDecimal("0")).max(new BigDecimal("1000")).build());
    static final Schema LIST_DIRECTORY_BUCKETS_INPUT = Schema.structureBuilder(ShapeId.from("com.amazonaws.s3#ListDirectoryBucketsRequest"))
             .putMember("ContinuationToken", Schemas4.DIRECTORY_BUCKET_TOKEN,
                     new HttpQueryTrait("continuation-token"))
             .putMember("MaxDirectoryBuckets", Schemas4.MAX_DIRECTORY_BUCKETS,
                     new HttpQueryTrait("max-directory-buckets"))
             .builderSupplier(ListDirectoryBucketsInput::builder)
             .shapeClass(ListDirectoryBucketsInput.class)
             .build();

    static final Schema LIST_DIRECTORY_BUCKETS_OUTPUT = Schema.structureBuilder(ShapeId.from("com.amazonaws.s3#ListDirectoryBucketsOutput"),
            new XmlNameTrait("ListAllMyDirectoryBucketsResult"))
             .putMember("Buckets", Schemas4.BUCKETS)
             .putMember("ContinuationToken", Schemas4.DIRECTORY_BUCKET_TOKEN)
             .builderSupplier(ListDirectoryBucketsOutput::builder)
             .shapeClass(ListDirectoryBucketsOutput.class)
             .build();

    static final Schema DELIMITER = Schema.createString(ShapeId.from("com.amazonaws.s3#Delimiter"));
    static final Schema KEY_MARKER = Schema.createString(ShapeId.from("com.amazonaws.s3#KeyMarker"));
    static final Schema MAX_UPLOADS = Schema.createInteger(ShapeId.from("com.amazonaws.s3#MaxUploads"));
    static final Schema UPLOAD_ID_MARKER = Schema.createString(ShapeId.from("com.amazonaws.s3#UploadIdMarker"));
    static final Schema LIST_MULTIPART_UPLOADS_INPUT = Schema.structureBuilder(ShapeId.from("com.amazonaws.s3#ListMultipartUploadsRequest"))
             .putMember("Bucket", Schemas.BUCKET_NAME,
                     new ContextParamTrait.Provider().createTrait(
                         ShapeId.from("smithy.rules#contextParam"),
                         Node.objectNodeBuilder()
                             .withMember("name", "Bucket")
                             .build()
                     ),
                     new RequiredTrait(),
                     new HttpLabelTrait())
             .putMember("Delimiter", Schemas4.DELIMITER,
                     new HttpQueryTrait("delimiter"))
             .putMember("EncodingType", EncodingType.$SCHEMA,
                     new HttpQueryTrait("encoding-type"))
             .putMember("KeyMarker", Schemas4.KEY_MARKER,
                     new HttpQueryTrait("key-marker"))
             .putMember("MaxUploads", Schemas4.MAX_UPLOADS,
                     new HttpQueryTrait("max-uploads"))
             .putMember("Prefix", Schemas1.PREFIX,
                     new ContextParamTrait.Provider().createTrait(
                         ShapeId.from("smithy.rules#contextParam"),
                         Node.objectNodeBuilder()
                             .withMember("name", "Prefix")
                             .build()
                     ),
                     new HttpQueryTrait("prefix"))
             .putMember("UploadIdMarker", Schemas4.UPLOAD_ID_MARKER,
                     new HttpQueryTrait("upload-id-marker"))
             .putMember("ExpectedBucketOwner", Schemas.ACCOUNT_ID,
                     new HttpHeaderTrait("x-amz-expected-bucket-owner"))
             .putMember("RequestPayer", RequestPayer.$SCHEMA,
                     new HttpHeaderTrait("x-amz-request-payer"))
             .builderSupplier(ListMultipartUploadsInput::builder)
             .shapeClass(ListMultipartUploadsInput.class)
             .build();

    static final Schema COMMON_PREFIX = Schema.structureBuilder(ShapeId.from("com.amazonaws.s3#CommonPrefix"))
             .putMember("Prefix", Schemas1.PREFIX)
             .builderSupplier(CommonPrefix::builder)
             .shapeClass(CommonPrefix.class)
             .build();

    static final Schema COMMON_PREFIX_LIST = Schema.listBuilder(ShapeId.from("com.amazonaws.s3#CommonPrefixList"))
        .putMember("member", Schemas4.COMMON_PREFIX)
        .build();

    static final Schema NEXT_KEY_MARKER = Schema.createString(ShapeId.from("com.amazonaws.s3#NextKeyMarker"));
    static final Schema NEXT_UPLOAD_ID_MARKER = Schema.createString(ShapeId.from("com.amazonaws.s3#NextUploadIdMarker"));
    static final Schema INITIATED = Schema.createTimestamp(ShapeId.from("com.amazonaws.s3#Initiated"));
    static final Schema INITIATOR = Schema.structureBuilder(ShapeId.from("com.amazonaws.s3#Initiator"))
             .putMember("ID", Schemas.COM_AMAZONAWS_S3_ID)
             .putMember("DisplayName", Schemas.DISPLAY_NAME)
             .builderSupplier(Initiator::builder)
             .shapeClass(Initiator.class)
             .build();

    static final Schema MULTIPART_UPLOAD = Schema.structureBuilder(ShapeId.from("com.amazonaws.s3#MultipartUpload"))
             .putMember("UploadId", Schemas.MULTIPART_UPLOAD_ID)
             .putMember("Key", Schemas.OBJECT_KEY)
             .putMember("Initiated", Schemas4.INITIATED)
             .putMember("StorageClass", StorageClass.$SCHEMA)
             .putMember("Owner", Schemas.OWNER)
             .putMember("Initiator", Schemas4.INITIATOR)
             .putMember("ChecksumAlgorithm", ChecksumAlgorithm.$SCHEMA)
             .putMember("ChecksumType", ChecksumType.$SCHEMA)
             .builderSupplier(MultipartUpload::builder)
             .shapeClass(MultipartUpload.class)
             .build();

    static final Schema MULTIPART_UPLOAD_LIST = Schema.listBuilder(ShapeId.from("com.amazonaws.s3#MultipartUploadList"))
        .putMember("member", Schemas4.MULTIPART_UPLOAD)
        .build();

    static final Schema LIST_MULTIPART_UPLOADS_OUTPUT = Schema.structureBuilder(ShapeId.from("com.amazonaws.s3#ListMultipartUploadsOutput"),
            new XmlNameTrait("ListMultipartUploadsResult"))
             .putMember("Bucket", Schemas.BUCKET_NAME)
             .putMember("KeyMarker", Schemas4.KEY_MARKER)
             .putMember("UploadIdMarker", Schemas4.UPLOAD_ID_MARKER)
             .putMember("NextKeyMarker", Schemas4.NEXT_KEY_MARKER)
             .putMember("Prefix", Schemas1.PREFIX)
             .putMember("Delimiter", Schemas4.DELIMITER)
             .putMember("NextUploadIdMarker", Schemas4.NEXT_UPLOAD_ID_MARKER)
             .putMember("MaxUploads", Schemas4.MAX_UPLOADS)
             .putMember("IsTruncated", Schemas3.IS_TRUNCATED)
             .putMember("Uploads", Schemas4.MULTIPART_UPLOAD_LIST,
                     new XmlNameTrait("Upload"),
                     new XmlFlattenedTrait())
             .putMember("CommonPrefixes", Schemas4.COMMON_PREFIX_LIST,
                     new XmlFlattenedTrait())
             .putMember("EncodingType", EncodingType.$SCHEMA)
             .putMember("RequestCharged", RequestCharged.$SCHEMA,
                     new HttpHeaderTrait("x-amz-request-charged"))
             .builderSupplier(ListMultipartUploadsOutput::builder)
             .shapeClass(ListMultipartUploadsOutput.class)
             .build();

    static final Schema INVALID_PREFIX = Schema.structureBuilder(ShapeId.from("com.amazonaws.s3#InvalidPrefix"),
            new ErrorTrait("client"),
            new HttpErrorTrait(400)).builderSupplier(InvalidPrefix::builder)
             .shapeClass(InvalidPrefix.class)
             .build();

    static final Schema ANNOTATION_PREFIX = Schema.createString(ShapeId.from("com.amazonaws.s3#AnnotationPrefix"));
    static final Schema MAX_ANNOTATION_RESULTS = Schema.createInteger(ShapeId.from("com.amazonaws.s3#MaxAnnotationResults"),
            RangeTrait.builder().min(new BigDecimal("1")).max(new BigDecimal("1000")).build());
    static final Schema LIST_OBJECT_ANNOTATIONS_INPUT = Schema.structureBuilder(ShapeId.from("com.amazonaws.s3#ListObjectAnnotationsRequest"))
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
             .putMember("MaxAnnotationResults", Schemas4.MAX_ANNOTATION_RESULTS,
                     new HttpQueryTrait("max-annotation-results"))
             .putMember("AnnotationPrefix", Schemas4.ANNOTATION_PREFIX,
                     new HttpQueryTrait("annotation-prefix"))
             .putMember("ContinuationToken", Schemas4.TOKEN,
                     new HttpQueryTrait("continuation-token"))
             .putMember("RequestPayer", RequestPayer.$SCHEMA,
                     new HttpHeaderTrait("x-amz-request-payer"))
             .putMember("ExpectedBucketOwner", Schemas.ACCOUNT_ID,
                     new HttpHeaderTrait("x-amz-expected-bucket-owner"))
             .builderSupplier(ListObjectAnnotationsInput::builder)
             .shapeClass(ListObjectAnnotationsInput.class)
             .build();

    static final Schema ANNOTATION_COUNT = Schema.createInteger(ShapeId.from("com.amazonaws.s3#AnnotationCount"));
    static final Schema CHECKSUM_ALGORITHM_LIST = Schema.listBuilder(ShapeId.from("com.amazonaws.s3#ChecksumAlgorithmList"))
        .putMember("member", ChecksumAlgorithm.$SCHEMA)
        .build();

    static final Schema ANNOTATION_ENTRY = Schema.structureBuilder(ShapeId.from("com.amazonaws.s3#AnnotationEntry"))
             .putMember("AnnotationName", Schemas1.ANNOTATION_NAME,
                     new RequiredTrait())
             .putMember("LastModified", Schemas.LAST_MODIFIED,
                     new RequiredTrait())
             .putMember("ETag", Schemas.E_TAG)
             .putMember("ChecksumAlgorithm", Schemas4.CHECKSUM_ALGORITHM_LIST,
                     new XmlFlattenedTrait())
             .putMember("Size", Schemas1.SIZE,
                     new RequiredTrait())
             .putMember("ReplicationStatus", ReplicationStatus.$SCHEMA)
             .builderSupplier(AnnotationEntry::builder)
             .shapeClass(AnnotationEntry.class)
             .build();

    static final Schema ANNOTATION_LIST = Schema.listBuilder(ShapeId.from("com.amazonaws.s3#AnnotationList"))
        .putMember("member", Schemas4.ANNOTATION_ENTRY,
                new XmlNameTrait("AnnotationEntry"))
        .build();

    static final Schema LIST_OBJECT_ANNOTATIONS_OUTPUT = Schema.structureBuilder(ShapeId.from("com.amazonaws.s3#ListObjectAnnotationsOutput"))
             .putMember("Annotations", Schemas4.ANNOTATION_LIST)
             .putMember("Bucket", Schemas.BUCKET_NAME)
             .putMember("Key", Schemas.OBJECT_KEY)
             .putMember("ObjectVersionId", Schemas.OBJECT_VERSION_ID,
                     new HttpHeaderTrait("x-amz-object-version-id"))
             .putMember("AnnotationPrefix", Schemas4.ANNOTATION_PREFIX)
             .putMember("MaxAnnotationResults", Schemas4.MAX_ANNOTATION_RESULTS)
             .putMember("AnnotationCount", Schemas4.ANNOTATION_COUNT)
             .putMember("ContinuationToken", Schemas4.TOKEN)
             .putMember("NextContinuationToken", Schemas4.NEXT_TOKEN)
             .putMember("RequestCharged", RequestCharged.$SCHEMA,
                     new HttpHeaderTrait("x-amz-request-charged"))
             .builderSupplier(ListObjectAnnotationsOutput::builder)
             .shapeClass(ListObjectAnnotationsOutput.class)
             .build();

    static final Schema MARKER = Schema.createString(ShapeId.from("com.amazonaws.s3#Marker"));
    static final Schema MAX_KEYS = Schema.createInteger(ShapeId.from("com.amazonaws.s3#MaxKeys"));
    static final Schema OPTIONAL_OBJECT_ATTRIBUTES_LIST = Schema.listBuilder(ShapeId.from("com.amazonaws.s3#OptionalObjectAttributesList"))
        .putMember("member", OptionalObjectAttributes.$SCHEMA)
        .build();

    static final Schema LIST_OBJECTS_INPUT = Schema.structureBuilder(ShapeId.from("com.amazonaws.s3#ListObjectsRequest"))
             .putMember("Bucket", Schemas.BUCKET_NAME,
                     new ContextParamTrait.Provider().createTrait(
                         ShapeId.from("smithy.rules#contextParam"),
                         Node.objectNodeBuilder()
                             .withMember("name", "Bucket")
                             .build()
                     ),
                     new RequiredTrait(),
                     new HttpLabelTrait())
             .putMember("Delimiter", Schemas4.DELIMITER,
                     new HttpQueryTrait("delimiter"))
             .putMember("EncodingType", EncodingType.$SCHEMA,
                     new HttpQueryTrait("encoding-type"))
             .putMember("Marker", Schemas4.MARKER,
                     new HttpQueryTrait("marker"))
             .putMember("MaxKeys", Schemas4.MAX_KEYS,
                     new HttpQueryTrait("max-keys"))
             .putMember("Prefix", Schemas1.PREFIX,
                     new ContextParamTrait.Provider().createTrait(
                         ShapeId.from("smithy.rules#contextParam"),
                         Node.objectNodeBuilder()
                             .withMember("name", "Prefix")
                             .build()
                     ),
                     new HttpQueryTrait("prefix"))
             .putMember("RequestPayer", RequestPayer.$SCHEMA,
                     new HttpHeaderTrait("x-amz-request-payer"))
             .putMember("ExpectedBucketOwner", Schemas.ACCOUNT_ID,
                     new HttpHeaderTrait("x-amz-expected-bucket-owner"))
             .putMember("OptionalObjectAttributes", Schemas4.OPTIONAL_OBJECT_ATTRIBUTES_LIST,
                     new HttpHeaderTrait("x-amz-optional-object-attributes"))
             .builderSupplier(ListObjectsInput::builder)
             .shapeClass(ListObjectsInput.class)
             .build();

    static final Schema IS_RESTORE_IN_PROGRESS = Schema.createBoolean(ShapeId.from("com.amazonaws.s3#IsRestoreInProgress"));
    static final Schema RESTORE_EXPIRY_DATE = Schema.createTimestamp(ShapeId.from("com.amazonaws.s3#RestoreExpiryDate"));
    static final Schema RESTORE_STATUS = Schema.structureBuilder(ShapeId.from("com.amazonaws.s3#RestoreStatus"))
             .putMember("IsRestoreInProgress", Schemas4.IS_RESTORE_IN_PROGRESS)
             .putMember("RestoreExpiryDate", Schemas4.RESTORE_EXPIRY_DATE)
             .builderSupplier(RestoreStatus::builder)
             .shapeClass(RestoreStatus.class)
             .build();

    static final Schema OBJECT = Schema.structureBuilder(ShapeId.from("com.amazonaws.s3#Object"))
             .putMember("Key", Schemas.OBJECT_KEY)
             .putMember("LastModified", Schemas.LAST_MODIFIED)
             .putMember("ETag", Schemas.E_TAG)
             .putMember("ChecksumAlgorithm", Schemas4.CHECKSUM_ALGORITHM_LIST,
                     new XmlFlattenedTrait())
             .putMember("ChecksumType", ChecksumType.$SCHEMA)
             .putMember("Size", Schemas1.SIZE)
             .putMember("StorageClass", ObjectStorageClass.$SCHEMA)
             .putMember("Owner", Schemas.OWNER)
             .putMember("RestoreStatus", Schemas4.RESTORE_STATUS)
             .builderSupplier(ObjectShape::builder)
             .shapeClass(ObjectShape.class)
             .build();

    static final Schema OBJECT_LIST = Schema.listBuilder(ShapeId.from("com.amazonaws.s3#ObjectList"))
        .putMember("member", Schemas4.OBJECT)
        .build();

    static final Schema NEXT_MARKER = Schema.createString(ShapeId.from("com.amazonaws.s3#NextMarker"));
    static final Schema LIST_OBJECTS_OUTPUT = Schema.structureBuilder(ShapeId.from("com.amazonaws.s3#ListObjectsOutput"),
            new XmlNameTrait("ListBucketResult"))
             .putMember("IsTruncated", Schemas3.IS_TRUNCATED)
             .putMember("Marker", Schemas4.MARKER)
             .putMember("NextMarker", Schemas4.NEXT_MARKER)
             .putMember("Contents", Schemas4.OBJECT_LIST,
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
             .builderSupplier(ListObjectsOutput::builder)
             .shapeClass(ListObjectsOutput.class)
             .build();

    static final Schema FETCH_OWNER = Schema.createBoolean(ShapeId.from("com.amazonaws.s3#FetchOwner"));
    static final Schema START_AFTER = Schema.createString(ShapeId.from("com.amazonaws.s3#StartAfter"));
    static final Schema LIST_OBJECTS_V2_INPUT = Schema.structureBuilder(ShapeId.from("com.amazonaws.s3#ListObjectsV2Request"))
             .putMember("Bucket", Schemas.BUCKET_NAME,
                     new ContextParamTrait.Provider().createTrait(
                         ShapeId.from("smithy.rules#contextParam"),
                         Node.objectNodeBuilder()
                             .withMember("name", "Bucket")
                             .build()
                     ),
                     new RequiredTrait(),
                     new HttpLabelTrait())
             .putMember("Delimiter", Schemas4.DELIMITER,
                     new HttpQueryTrait("delimiter"))
             .putMember("EncodingType", EncodingType.$SCHEMA,
                     new HttpQueryTrait("encoding-type"))
             .putMember("MaxKeys", Schemas4.MAX_KEYS,
                     new HttpQueryTrait("max-keys"))
             .putMember("Prefix", Schemas1.PREFIX,
                     new ContextParamTrait.Provider().createTrait(
                         ShapeId.from("smithy.rules#contextParam"),
                         Node.objectNodeBuilder()
                             .withMember("name", "Prefix")
                             .build()
                     ),
                     new HttpQueryTrait("prefix"))
             .putMember("ContinuationToken", Schemas4.TOKEN,
                     new HttpQueryTrait("continuation-token"))
             .putMember("FetchOwner", Schemas4.FETCH_OWNER,
                     new HttpQueryTrait("fetch-owner"))
             .putMember("StartAfter", Schemas4.START_AFTER,
                     new HttpQueryTrait("start-after"))
             .putMember("RequestPayer", RequestPayer.$SCHEMA,
                     new HttpHeaderTrait("x-amz-request-payer"))
             .putMember("ExpectedBucketOwner", Schemas.ACCOUNT_ID,
                     new HttpHeaderTrait("x-amz-expected-bucket-owner"))
             .putMember("OptionalObjectAttributes", Schemas4.OPTIONAL_OBJECT_ATTRIBUTES_LIST,
                     new HttpHeaderTrait("x-amz-optional-object-attributes"))
             .builderSupplier(ListObjectsV2Input::builder)
             .shapeClass(ListObjectsV2Input.class)
             .build();

    static final Schema KEY_COUNT = Schema.createInteger(ShapeId.from("com.amazonaws.s3#KeyCount"));
    static final Schema LIST_OBJECTS_V2_OUTPUT = Schema.structureBuilder(ShapeId.from("com.amazonaws.s3#ListObjectsV2Output"),
            new XmlNameTrait("ListBucketResult"))
             .putMember("IsTruncated", Schemas3.IS_TRUNCATED)
             .putMember("Contents", Schemas4.OBJECT_LIST,
                     new XmlFlattenedTrait())
             .putMember("Name", Schemas.BUCKET_NAME)
             .putMember("Prefix", Schemas1.PREFIX)
             .putMember("Delimiter", Schemas4.DELIMITER)
             .putMember("MaxKeys", Schemas4.MAX_KEYS)
             .putMember("CommonPrefixes", Schemas4.COMMON_PREFIX_LIST,
                     new XmlFlattenedTrait())
             .putMember("EncodingType", EncodingType.$SCHEMA)
             .putMember("KeyCount", Schemas4.KEY_COUNT)
             .putMember("ContinuationToken", Schemas4.TOKEN)
             .putMember("NextContinuationToken", Schemas4.NEXT_TOKEN)
             .putMember("StartAfter", Schemas4.START_AFTER)
             .putMember("RequestCharged", RequestCharged.$SCHEMA,
                     new HttpHeaderTrait("x-amz-request-charged"))
             .builderSupplier(ListObjectsV2Output::builder)
             .shapeClass(ListObjectsV2Output.class)
             .build();

    static final Schema VERSION_ID_MARKER = Schema.createString(ShapeId.from("com.amazonaws.s3#VersionIdMarker"));
    static final Schema LIST_OBJECT_VERSIONS_INPUT = Schema.structureBuilder(ShapeId.from("com.amazonaws.s3#ListObjectVersionsRequest"))
             .putMember("Bucket", Schemas.BUCKET_NAME,
                     new ContextParamTrait.Provider().createTrait(
                         ShapeId.from("smithy.rules#contextParam"),
                         Node.objectNodeBuilder()
                             .withMember("name", "Bucket")
                             .build()
                     ),
                     new RequiredTrait(),
                     new HttpLabelTrait())
             .putMember("Delimiter", Schemas4.DELIMITER,
                     new HttpQueryTrait("delimiter"))
             .putMember("EncodingType", EncodingType.$SCHEMA,
                     new HttpQueryTrait("encoding-type"))
             .putMember("KeyMarker", Schemas4.KEY_MARKER,
                     new HttpQueryTrait("key-marker"))
             .putMember("MaxKeys", Schemas4.MAX_KEYS,
                     new HttpQueryTrait("max-keys"))
             .putMember("Prefix", Schemas1.PREFIX,
                     new ContextParamTrait.Provider().createTrait(
                         ShapeId.from("smithy.rules#contextParam"),
                         Node.objectNodeBuilder()
                             .withMember("name", "Prefix")
                             .build()
                     ),
                     new HttpQueryTrait("prefix"))
             .putMember("VersionIdMarker", Schemas4.VERSION_ID_MARKER,
                     new HttpQueryTrait("version-id-marker"))
             .putMember("ExpectedBucketOwner", Schemas.ACCOUNT_ID,
                     new HttpHeaderTrait("x-amz-expected-bucket-owner"))
             .putMember("RequestPayer", RequestPayer.$SCHEMA,
                     new HttpHeaderTrait("x-amz-request-payer"))
             .putMember("OptionalObjectAttributes", Schemas4.OPTIONAL_OBJECT_ATTRIBUTES_LIST,
                     new HttpHeaderTrait("x-amz-optional-object-attributes"))
             .builderSupplier(ListObjectVersionsInput::builder)
             .shapeClass(ListObjectVersionsInput.class)
             .build();

    static final Schema IS_LATEST = Schema.createBoolean(ShapeId.from("com.amazonaws.s3#IsLatest"));
    static final Schema DELETE_MARKER_ENTRY = Schema.structureBuilder(ShapeId.from("com.amazonaws.s3#DeleteMarkerEntry"))
             .putMember("Owner", Schemas.OWNER)
             .putMember("Key", Schemas.OBJECT_KEY)
             .putMember("VersionId", Schemas.OBJECT_VERSION_ID)
             .putMember("IsLatest", Schemas4.IS_LATEST)
             .putMember("LastModified", Schemas.LAST_MODIFIED)
             .builderSupplier(DeleteMarkerEntry::builder)
             .shapeClass(DeleteMarkerEntry.class)
             .build();

    static final Schema DELETE_MARKERS = Schema.listBuilder(ShapeId.from("com.amazonaws.s3#DeleteMarkers"))
        .putMember("member", Schemas4.DELETE_MARKER_ENTRY)
        .build();

    static final Schema NEXT_VERSION_ID_MARKER = Schema.createString(ShapeId.from("com.amazonaws.s3#NextVersionIdMarker"));
    static final Schema OBJECT_VERSION = Schema.structureBuilder(ShapeId.from("com.amazonaws.s3#ObjectVersion"))
             .putMember("ETag", Schemas.E_TAG)
             .putMember("ChecksumAlgorithm", Schemas4.CHECKSUM_ALGORITHM_LIST,
                     new XmlFlattenedTrait())
             .putMember("ChecksumType", ChecksumType.$SCHEMA)
             .putMember("Size", Schemas1.SIZE)
             .putMember("StorageClass", ObjectVersionStorageClass.$SCHEMA)
             .putMember("Key", Schemas.OBJECT_KEY)
             .putMember("VersionId", Schemas.OBJECT_VERSION_ID)
             .putMember("IsLatest", Schemas4.IS_LATEST)
             .putMember("LastModified", Schemas.LAST_MODIFIED)
             .putMember("Owner", Schemas.OWNER)
             .putMember("RestoreStatus", Schemas4.RESTORE_STATUS)
             .builderSupplier(ObjectVersion::builder)
             .shapeClass(ObjectVersion.class)
             .build();

    static final Schema OBJECT_VERSION_LIST = Schema.listBuilder(ShapeId.from("com.amazonaws.s3#ObjectVersionList"))
        .putMember("member", Schemas4.OBJECT_VERSION)
        .build();

    private Schemas4() {}
}
