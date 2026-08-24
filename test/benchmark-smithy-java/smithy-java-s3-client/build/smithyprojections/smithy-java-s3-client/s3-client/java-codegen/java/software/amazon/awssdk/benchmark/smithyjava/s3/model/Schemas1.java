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
import software.amazon.smithy.model.traits.RequiredTrait;
import software.amazon.smithy.model.traits.SensitiveTrait;
import software.amazon.smithy.model.traits.TimestampFormatTrait;
import software.amazon.smithy.model.traits.UnitTypeTrait;
import software.amazon.smithy.model.traits.XmlFlattenedTrait;
import software.amazon.smithy.model.traits.XmlNameTrait;
import software.amazon.smithy.rulesengine.traits.ContextParamTrait;

/**
 * Defines schemas for shapes in the model package.
 */
final class Schemas1 {
    static final Schema CREATE_MULTIPART_UPLOAD_INPUT = Schema.structureBuilder(ShapeId.from("com.amazonaws.s3#CreateMultipartUploadRequest"))
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
             .putMember("ContentDisposition", Schemas.CONTENT_DISPOSITION,
                     new HttpHeaderTrait("Content-Disposition"))
             .putMember("ContentEncoding", Schemas.CONTENT_ENCODING,
                     new HttpHeaderTrait("Content-Encoding"))
             .putMember("ContentLanguage", Schemas.CONTENT_LANGUAGE,
                     new HttpHeaderTrait("Content-Language"))
             .putMember("ContentType", Schemas.CONTENT_TYPE,
                     new HttpHeaderTrait("Content-Type"))
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
             .putMember("ChecksumAlgorithm", ChecksumAlgorithm.$SCHEMA,
                     new HttpHeaderTrait("x-amz-checksum-algorithm"))
             .putMember("ChecksumType", ChecksumType.$SCHEMA,
                     new HttpHeaderTrait("x-amz-checksum-type"))
             .builderSupplier(CreateMultipartUploadInput::builder)
             .shapeClass(CreateMultipartUploadInput.class)
             .build();

    static final Schema CREATE_MULTIPART_UPLOAD_OUTPUT = Schema.structureBuilder(ShapeId.from("com.amazonaws.s3#CreateMultipartUploadOutput"),
            new XmlNameTrait("InitiateMultipartUploadResult"))
             .putMember("AbortDate", Schemas.ABORT_DATE,
                     new HttpHeaderTrait("x-amz-abort-date"))
             .putMember("AbortRuleId", Schemas.ABORT_RULE_ID,
                     new HttpHeaderTrait("x-amz-abort-rule-id"))
             .putMember("Bucket", Schemas.BUCKET_NAME,
                     new XmlNameTrait("Bucket"))
             .putMember("Key", Schemas.OBJECT_KEY)
             .putMember("UploadId", Schemas.MULTIPART_UPLOAD_ID)
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
             .putMember("ChecksumAlgorithm", ChecksumAlgorithm.$SCHEMA,
                     new HttpHeaderTrait("x-amz-checksum-algorithm"))
             .putMember("ChecksumType", ChecksumType.$SCHEMA,
                     new HttpHeaderTrait("x-amz-checksum-type"))
             .builderSupplier(CreateMultipartUploadOutput::builder)
             .shapeClass(CreateMultipartUploadOutput.class)
             .build();

    static final Schema CREATE_SESSION_INPUT = Schema.structureBuilder(ShapeId.from("com.amazonaws.s3#CreateSessionRequest"))
             .putMember("SessionMode", SessionMode.$SCHEMA,
                     new HttpHeaderTrait("x-amz-create-session-mode"))
             .putMember("Bucket", Schemas.BUCKET_NAME,
                     new ContextParamTrait.Provider().createTrait(
                         ShapeId.from("smithy.rules#contextParam"),
                         Node.objectNodeBuilder()
                             .withMember("name", "Bucket")
                             .build()
                     ),
                     new RequiredTrait(),
                     new HttpLabelTrait())
             .putMember("ServerSideEncryption", ServerSideEncryption.$SCHEMA,
                     new HttpHeaderTrait("x-amz-server-side-encryption"))
             .putMember("SSEKMSKeyId", Schemas.SSEKMS_KEY_ID,
                     new HttpHeaderTrait("x-amz-server-side-encryption-aws-kms-key-id"))
             .putMember("SSEKMSEncryptionContext", Schemas.SSEKMS_ENCRYPTION_CONTEXT,
                     new HttpHeaderTrait("x-amz-server-side-encryption-context"))
             .putMember("BucketKeyEnabled", Schemas.BUCKET_KEY_ENABLED,
                     new HttpHeaderTrait("x-amz-server-side-encryption-bucket-key-enabled"))
             .builderSupplier(CreateSessionInput::builder)
             .shapeClass(CreateSessionInput.class)
             .build();

    static final Schema SESSION_EXPIRATION = Schema.createTimestamp(ShapeId.from("com.amazonaws.s3#SessionExpiration"));
    static final Schema SESSION_CREDENTIAL_VALUE = Schema.createString(ShapeId.from("com.amazonaws.s3#SessionCredentialValue"),
            new SensitiveTrait());
    static final Schema SESSION_CREDENTIALS = Schema.structureBuilder(ShapeId.from("com.amazonaws.s3#SessionCredentials"))
             .putMember("AccessKeyId", Schemas.ACCESS_KEY_ID_VALUE,
                     new XmlNameTrait("AccessKeyId"),
                     new RequiredTrait())
             .putMember("SecretAccessKey", Schemas1.SESSION_CREDENTIAL_VALUE,
                     new XmlNameTrait("SecretAccessKey"),
                     new RequiredTrait())
             .putMember("SessionToken", Schemas1.SESSION_CREDENTIAL_VALUE,
                     new XmlNameTrait("SessionToken"),
                     new RequiredTrait())
             .putMember("Expiration", Schemas1.SESSION_EXPIRATION,
                     new XmlNameTrait("Expiration"),
                     new RequiredTrait())
             .builderSupplier(SessionCredentials::builder)
             .shapeClass(SessionCredentials.class)
             .build();

    static final Schema CREATE_SESSION_OUTPUT = Schema.structureBuilder(ShapeId.from("com.amazonaws.s3#CreateSessionOutput"),
            new XmlNameTrait("CreateSessionResult"))
             .putMember("ServerSideEncryption", ServerSideEncryption.$SCHEMA,
                     new HttpHeaderTrait("x-amz-server-side-encryption"))
             .putMember("SSEKMSKeyId", Schemas.SSEKMS_KEY_ID,
                     new HttpHeaderTrait("x-amz-server-side-encryption-aws-kms-key-id"))
             .putMember("SSEKMSEncryptionContext", Schemas.SSEKMS_ENCRYPTION_CONTEXT,
                     new HttpHeaderTrait("x-amz-server-side-encryption-context"))
             .putMember("BucketKeyEnabled", Schemas.BUCKET_KEY_ENABLED,
                     new HttpHeaderTrait("x-amz-server-side-encryption-bucket-key-enabled"))
             .putMember("Credentials", Schemas1.SESSION_CREDENTIALS,
                     new XmlNameTrait("Credentials"),
                     new RequiredTrait())
             .builderSupplier(CreateSessionOutput::builder)
             .shapeClass(CreateSessionOutput.class)
             .build();

    static final Schema NO_SUCH_BUCKET = Schema.structureBuilder(ShapeId.from("com.amazonaws.s3#NoSuchBucket"),
            new ErrorTrait("client"),
            new HttpErrorTrait(404)).builderSupplier(NoSuchBucket::builder)
             .shapeClass(NoSuchBucket.class)
             .build();

    static final Schema DELETE_BUCKET_INPUT = Schema.structureBuilder(ShapeId.from("com.amazonaws.s3#DeleteBucketRequest"))
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
             .builderSupplier(DeleteBucketInput::builder)
             .shapeClass(DeleteBucketInput.class)
             .build();

    static final Schema DELETE_BUCKET_OUTPUT = Schema.structureBuilder(ShapeId.from("com.amazonaws.s3#DeleteBucketOutput"),
            new UnitTypeTrait()).builderSupplier(DeleteBucketOutput::builder)
             .shapeClass(DeleteBucketOutput.class)
             .build();

    static final Schema ANALYTICS_ID = Schema.createString(ShapeId.from("com.amazonaws.s3#AnalyticsId"));
    static final Schema DELETE_BUCKET_ANALYTICS_CONFIGURATION_INPUT = Schema.structureBuilder(ShapeId.from("com.amazonaws.s3#DeleteBucketAnalyticsConfigurationRequest"))
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
             .putMember("ExpectedBucketOwner", Schemas.ACCOUNT_ID,
                     new HttpHeaderTrait("x-amz-expected-bucket-owner"))
             .builderSupplier(DeleteBucketAnalyticsConfigurationInput::builder)
             .shapeClass(DeleteBucketAnalyticsConfigurationInput.class)
             .build();

    static final Schema DELETE_BUCKET_ANALYTICS_CONFIGURATION_OUTPUT = Schema.structureBuilder(ShapeId.from("com.amazonaws.s3#DeleteBucketAnalyticsConfigurationOutput"),
            new UnitTypeTrait()).builderSupplier(DeleteBucketAnalyticsConfigurationOutput::builder)
             .shapeClass(DeleteBucketAnalyticsConfigurationOutput.class)
             .build();

    static final Schema DELETE_BUCKET_CORS_INPUT = Schema.structureBuilder(ShapeId.from("com.amazonaws.s3#DeleteBucketCorsRequest"))
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
             .builderSupplier(DeleteBucketCorsInput::builder)
             .shapeClass(DeleteBucketCorsInput.class)
             .build();

    static final Schema DELETE_BUCKET_CORS_OUTPUT = Schema.structureBuilder(ShapeId.from("com.amazonaws.s3#DeleteBucketCorsOutput"),
            new UnitTypeTrait()).builderSupplier(DeleteBucketCorsOutput::builder)
             .shapeClass(DeleteBucketCorsOutput.class)
             .build();

    static final Schema DELETE_BUCKET_ENCRYPTION_INPUT = Schema.structureBuilder(ShapeId.from("com.amazonaws.s3#DeleteBucketEncryptionRequest"))
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
             .builderSupplier(DeleteBucketEncryptionInput::builder)
             .shapeClass(DeleteBucketEncryptionInput.class)
             .build();

    static final Schema DELETE_BUCKET_ENCRYPTION_OUTPUT = Schema.structureBuilder(ShapeId.from("com.amazonaws.s3#DeleteBucketEncryptionOutput"),
            new UnitTypeTrait()).builderSupplier(DeleteBucketEncryptionOutput::builder)
             .shapeClass(DeleteBucketEncryptionOutput.class)
             .build();

    static final Schema INTELLIGENT_TIERING_ID = Schema.createString(ShapeId.from("com.amazonaws.s3#IntelligentTieringId"));
    static final Schema DELETE_BUCKET_INTELLIGENT_TIERING_CONFIGURATION_INPUT = Schema.structureBuilder(ShapeId.from("com.amazonaws.s3#DeleteBucketIntelligentTieringConfigurationRequest"))
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
             .builderSupplier(DeleteBucketIntelligentTieringConfigurationInput::builder)
             .shapeClass(DeleteBucketIntelligentTieringConfigurationInput.class)
             .build();

    static final Schema DELETE_BUCKET_INTELLIGENT_TIERING_CONFIGURATION_OUTPUT = Schema.structureBuilder(ShapeId.from("com.amazonaws.s3#DeleteBucketIntelligentTieringConfigurationOutput"),
            new UnitTypeTrait()).builderSupplier(DeleteBucketIntelligentTieringConfigurationOutput::builder)
             .shapeClass(DeleteBucketIntelligentTieringConfigurationOutput.class)
             .build();

    static final Schema INVENTORY_ID = Schema.createString(ShapeId.from("com.amazonaws.s3#InventoryId"));
    static final Schema DELETE_BUCKET_INVENTORY_CONFIGURATION_INPUT = Schema.structureBuilder(ShapeId.from("com.amazonaws.s3#DeleteBucketInventoryConfigurationRequest"))
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
             .putMember("ExpectedBucketOwner", Schemas.ACCOUNT_ID,
                     new HttpHeaderTrait("x-amz-expected-bucket-owner"))
             .builderSupplier(DeleteBucketInventoryConfigurationInput::builder)
             .shapeClass(DeleteBucketInventoryConfigurationInput.class)
             .build();

    static final Schema DELETE_BUCKET_INVENTORY_CONFIGURATION_OUTPUT = Schema.structureBuilder(ShapeId.from("com.amazonaws.s3#DeleteBucketInventoryConfigurationOutput"),
            new UnitTypeTrait()).builderSupplier(DeleteBucketInventoryConfigurationOutput::builder)
             .shapeClass(DeleteBucketInventoryConfigurationOutput.class)
             .build();

    static final Schema DELETE_BUCKET_LIFECYCLE_INPUT = Schema.structureBuilder(ShapeId.from("com.amazonaws.s3#DeleteBucketLifecycleRequest"))
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
             .builderSupplier(DeleteBucketLifecycleInput::builder)
             .shapeClass(DeleteBucketLifecycleInput.class)
             .build();

    static final Schema DELETE_BUCKET_LIFECYCLE_OUTPUT = Schema.structureBuilder(ShapeId.from("com.amazonaws.s3#DeleteBucketLifecycleOutput"),
            new UnitTypeTrait()).builderSupplier(DeleteBucketLifecycleOutput::builder)
             .shapeClass(DeleteBucketLifecycleOutput.class)
             .build();

    static final Schema DELETE_BUCKET_METADATA_CONFIGURATION_INPUT = Schema.structureBuilder(ShapeId.from("com.amazonaws.s3#DeleteBucketMetadataConfigurationRequest"))
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
             .builderSupplier(DeleteBucketMetadataConfigurationInput::builder)
             .shapeClass(DeleteBucketMetadataConfigurationInput.class)
             .build();

    static final Schema DELETE_BUCKET_METADATA_CONFIGURATION_OUTPUT = Schema.structureBuilder(ShapeId.from("com.amazonaws.s3#DeleteBucketMetadataConfigurationOutput"),
            new UnitTypeTrait()).builderSupplier(DeleteBucketMetadataConfigurationOutput::builder)
             .shapeClass(DeleteBucketMetadataConfigurationOutput.class)
             .build();

    static final Schema DELETE_BUCKET_METADATA_TABLE_CONFIGURATION_INPUT = Schema.structureBuilder(ShapeId.from("com.amazonaws.s3#DeleteBucketMetadataTableConfigurationRequest"))
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
             .builderSupplier(DeleteBucketMetadataTableConfigurationInput::builder)
             .shapeClass(DeleteBucketMetadataTableConfigurationInput.class)
             .build();

    static final Schema DELETE_BUCKET_METADATA_TABLE_CONFIGURATION_OUTPUT = Schema.structureBuilder(ShapeId.from("com.amazonaws.s3#DeleteBucketMetadataTableConfigurationOutput"),
            new UnitTypeTrait()).builderSupplier(DeleteBucketMetadataTableConfigurationOutput::builder)
             .shapeClass(DeleteBucketMetadataTableConfigurationOutput.class)
             .build();

    static final Schema METRICS_ID = Schema.createString(ShapeId.from("com.amazonaws.s3#MetricsId"));
    static final Schema DELETE_BUCKET_METRICS_CONFIGURATION_INPUT = Schema.structureBuilder(ShapeId.from("com.amazonaws.s3#DeleteBucketMetricsConfigurationRequest"))
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
             .putMember("ExpectedBucketOwner", Schemas.ACCOUNT_ID,
                     new HttpHeaderTrait("x-amz-expected-bucket-owner"))
             .builderSupplier(DeleteBucketMetricsConfigurationInput::builder)
             .shapeClass(DeleteBucketMetricsConfigurationInput.class)
             .build();

    static final Schema DELETE_BUCKET_METRICS_CONFIGURATION_OUTPUT = Schema.structureBuilder(ShapeId.from("com.amazonaws.s3#DeleteBucketMetricsConfigurationOutput"),
            new UnitTypeTrait()).builderSupplier(DeleteBucketMetricsConfigurationOutput::builder)
             .shapeClass(DeleteBucketMetricsConfigurationOutput.class)
             .build();

    static final Schema DELETE_BUCKET_OWNERSHIP_CONTROLS_INPUT = Schema.structureBuilder(ShapeId.from("com.amazonaws.s3#DeleteBucketOwnershipControlsRequest"))
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
             .builderSupplier(DeleteBucketOwnershipControlsInput::builder)
             .shapeClass(DeleteBucketOwnershipControlsInput.class)
             .build();

    static final Schema DELETE_BUCKET_OWNERSHIP_CONTROLS_OUTPUT = Schema.structureBuilder(ShapeId.from("com.amazonaws.s3#DeleteBucketOwnershipControlsOutput"),
            new UnitTypeTrait()).builderSupplier(DeleteBucketOwnershipControlsOutput::builder)
             .shapeClass(DeleteBucketOwnershipControlsOutput.class)
             .build();

    static final Schema DELETE_BUCKET_POLICY_INPUT = Schema.structureBuilder(ShapeId.from("com.amazonaws.s3#DeleteBucketPolicyRequest"))
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
             .builderSupplier(DeleteBucketPolicyInput::builder)
             .shapeClass(DeleteBucketPolicyInput.class)
             .build();

    static final Schema DELETE_BUCKET_POLICY_OUTPUT = Schema.structureBuilder(ShapeId.from("com.amazonaws.s3#DeleteBucketPolicyOutput"),
            new UnitTypeTrait()).builderSupplier(DeleteBucketPolicyOutput::builder)
             .shapeClass(DeleteBucketPolicyOutput.class)
             .build();

    static final Schema DELETE_BUCKET_REPLICATION_INPUT = Schema.structureBuilder(ShapeId.from("com.amazonaws.s3#DeleteBucketReplicationRequest"))
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
             .builderSupplier(DeleteBucketReplicationInput::builder)
             .shapeClass(DeleteBucketReplicationInput.class)
             .build();

    static final Schema DELETE_BUCKET_REPLICATION_OUTPUT = Schema.structureBuilder(ShapeId.from("com.amazonaws.s3#DeleteBucketReplicationOutput"),
            new UnitTypeTrait()).builderSupplier(DeleteBucketReplicationOutput::builder)
             .shapeClass(DeleteBucketReplicationOutput.class)
             .build();

    static final Schema DELETE_BUCKET_TAGGING_INPUT = Schema.structureBuilder(ShapeId.from("com.amazonaws.s3#DeleteBucketTaggingRequest"))
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
             .builderSupplier(DeleteBucketTaggingInput::builder)
             .shapeClass(DeleteBucketTaggingInput.class)
             .build();

    static final Schema DELETE_BUCKET_TAGGING_OUTPUT = Schema.structureBuilder(ShapeId.from("com.amazonaws.s3#DeleteBucketTaggingOutput"),
            new UnitTypeTrait()).builderSupplier(DeleteBucketTaggingOutput::builder)
             .shapeClass(DeleteBucketTaggingOutput.class)
             .build();

    static final Schema DELETE_BUCKET_WEBSITE_INPUT = Schema.structureBuilder(ShapeId.from("com.amazonaws.s3#DeleteBucketWebsiteRequest"))
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
             .builderSupplier(DeleteBucketWebsiteInput::builder)
             .shapeClass(DeleteBucketWebsiteInput.class)
             .build();

    static final Schema DELETE_BUCKET_WEBSITE_OUTPUT = Schema.structureBuilder(ShapeId.from("com.amazonaws.s3#DeleteBucketWebsiteOutput"),
            new UnitTypeTrait()).builderSupplier(DeleteBucketWebsiteOutput::builder)
             .shapeClass(DeleteBucketWebsiteOutput.class)
             .build();

    static final Schema BYPASS_GOVERNANCE_RETENTION = Schema.createBoolean(ShapeId.from("com.amazonaws.s3#BypassGovernanceRetention"));
    static final Schema IF_MATCH_LAST_MODIFIED_TIME = Schema.createTimestamp(ShapeId.from("com.amazonaws.s3#IfMatchLastModifiedTime"),
            new TimestampFormatTrait("http-date"));
    static final Schema IF_MATCH_SIZE = Schema.createLong(ShapeId.from("com.amazonaws.s3#IfMatchSize"));
    static final Schema COM_AMAZONAWS_S3_MFA = Schema.createString(ShapeId.from("com.amazonaws.s3#MFA"));
    static final Schema DELETE_OBJECT_INPUT = Schema.structureBuilder(ShapeId.from("com.amazonaws.s3#DeleteObjectRequest"))
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
             .putMember("MFA", Schemas1.COM_AMAZONAWS_S3_MFA,
                     new HttpHeaderTrait("x-amz-mfa"))
             .putMember("VersionId", Schemas.OBJECT_VERSION_ID,
                     new HttpQueryTrait("versionId"))
             .putMember("RequestPayer", RequestPayer.$SCHEMA,
                     new HttpHeaderTrait("x-amz-request-payer"))
             .putMember("BypassGovernanceRetention", Schemas1.BYPASS_GOVERNANCE_RETENTION,
                     new HttpHeaderTrait("x-amz-bypass-governance-retention"))
             .putMember("ExpectedBucketOwner", Schemas.ACCOUNT_ID,
                     new HttpHeaderTrait("x-amz-expected-bucket-owner"))
             .putMember("IfMatch", Schemas.IF_MATCH,
                     new HttpHeaderTrait("If-Match"))
             .putMember("IfMatchLastModifiedTime", Schemas1.IF_MATCH_LAST_MODIFIED_TIME,
                     new HttpHeaderTrait("x-amz-if-match-last-modified-time"))
             .putMember("IfMatchSize", Schemas1.IF_MATCH_SIZE,
                     new HttpHeaderTrait("x-amz-if-match-size"))
             .builderSupplier(DeleteObjectInput::builder)
             .shapeClass(DeleteObjectInput.class)
             .build();

    static final Schema DELETE_MARKER = Schema.createBoolean(ShapeId.from("com.amazonaws.s3#DeleteMarker"));
    static final Schema DELETE_OBJECT_OUTPUT = Schema.structureBuilder(ShapeId.from("com.amazonaws.s3#DeleteObjectOutput"))
             .putMember("DeleteMarker", Schemas1.DELETE_MARKER,
                     new HttpHeaderTrait("x-amz-delete-marker"))
             .putMember("VersionId", Schemas.OBJECT_VERSION_ID,
                     new HttpHeaderTrait("x-amz-version-id"))
             .putMember("RequestCharged", RequestCharged.$SCHEMA,
                     new HttpHeaderTrait("x-amz-request-charged"))
             .builderSupplier(DeleteObjectOutput::builder)
             .shapeClass(DeleteObjectOutput.class)
             .build();

    static final Schema ANNOTATION_NAME = Schema.createString(ShapeId.from("com.amazonaws.s3#AnnotationName"));
    static final Schema OBJECT_IF_MATCH = Schema.createString(ShapeId.from("com.amazonaws.s3#ObjectIfMatch"));
    static final Schema DELETE_OBJECT_ANNOTATION_INPUT = Schema.structureBuilder(ShapeId.from("com.amazonaws.s3#DeleteObjectAnnotationRequest"))
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
             .putMember("AnnotationName", Schemas1.ANNOTATION_NAME,
                     new RequiredTrait(),
                     new HttpQueryTrait("annotationName"))
             .putMember("VersionId", Schemas.OBJECT_VERSION_ID,
                     new HttpQueryTrait("versionId"))
             .putMember("RequestPayer", RequestPayer.$SCHEMA,
                     new HttpHeaderTrait("x-amz-request-payer"))
             .putMember("ExpectedBucketOwner", Schemas.ACCOUNT_ID,
                     new HttpHeaderTrait("x-amz-expected-bucket-owner"))
             .putMember("ObjectIfMatch", Schemas1.OBJECT_IF_MATCH,
                     new HttpHeaderTrait("x-amz-object-if-match"))
             .builderSupplier(DeleteObjectAnnotationInput::builder)
             .shapeClass(DeleteObjectAnnotationInput.class)
             .build();

    static final Schema DELETE_OBJECT_ANNOTATION_OUTPUT = Schema.structureBuilder(ShapeId.from("com.amazonaws.s3#DeleteObjectAnnotationOutput"))
             .putMember("ObjectVersionId", Schemas.OBJECT_VERSION_ID,
                     new HttpHeaderTrait("x-amz-object-version-id"))
             .putMember("RequestCharged", RequestCharged.$SCHEMA,
                     new HttpHeaderTrait("x-amz-request-charged"))
             .builderSupplier(DeleteObjectAnnotationOutput::builder)
             .shapeClass(DeleteObjectAnnotationOutput.class)
             .build();

    static final Schema NO_SUCH_KEY = Schema.structureBuilder(ShapeId.from("com.amazonaws.s3#NoSuchKey"),
            new ErrorTrait("client"),
            new HttpErrorTrait(404)).builderSupplier(NoSuchKey::builder)
             .shapeClass(NoSuchKey.class)
             .build();

    static final Schema LAST_MODIFIED_TIME = Schema.createTimestamp(ShapeId.from("com.amazonaws.s3#LastModifiedTime"),
            new TimestampFormatTrait("http-date"));
    static final Schema SIZE = Schema.createLong(ShapeId.from("com.amazonaws.s3#Size"));
    static final Schema OBJECT_IDENTIFIER = Schema.structureBuilder(ShapeId.from("com.amazonaws.s3#ObjectIdentifier"))
             .putMember("Key", Schemas.OBJECT_KEY,
                     new RequiredTrait())
             .putMember("VersionId", Schemas.OBJECT_VERSION_ID)
             .putMember("ETag", Schemas.E_TAG)
             .putMember("LastModifiedTime", Schemas1.LAST_MODIFIED_TIME)
             .putMember("Size", Schemas1.SIZE)
             .builderSupplier(ObjectIdentifier::builder)
             .shapeClass(ObjectIdentifier.class)
             .build();

    static final Schema OBJECT_IDENTIFIER_LIST = Schema.listBuilder(ShapeId.from("com.amazonaws.s3#ObjectIdentifierList"))
        .putMember("member", Schemas1.OBJECT_IDENTIFIER)
        .build();

    static final Schema QUIET = Schema.createBoolean(ShapeId.from("com.amazonaws.s3#Quiet"));
    static final Schema DELETE = Schema.structureBuilder(ShapeId.from("com.amazonaws.s3#Delete"))
             .putMember("Objects", Schemas1.OBJECT_IDENTIFIER_LIST,
                     new XmlNameTrait("Object"),
                     new RequiredTrait(),
                     new XmlFlattenedTrait())
             .putMember("Quiet", Schemas1.QUIET)
             .builderSupplier(Delete::builder)
             .shapeClass(Delete.class)
             .build();

    static final Schema DELETE_OBJECTS_INPUT = Schema.structureBuilder(ShapeId.from("com.amazonaws.s3#DeleteObjectsRequest"))
             .putMember("Bucket", Schemas.BUCKET_NAME,
                     new ContextParamTrait.Provider().createTrait(
                         ShapeId.from("smithy.rules#contextParam"),
                         Node.objectNodeBuilder()
                             .withMember("name", "Bucket")
                             .build()
                     ),
                     new RequiredTrait(),
                     new HttpLabelTrait())
             .putMember("Delete", Schemas1.DELETE,
                     new XmlNameTrait("Delete"),
                     new RequiredTrait(),
                     new HttpPayloadTrait())
             .putMember("MFA", Schemas1.COM_AMAZONAWS_S3_MFA,
                     new HttpHeaderTrait("x-amz-mfa"))
             .putMember("RequestPayer", RequestPayer.$SCHEMA,
                     new HttpHeaderTrait("x-amz-request-payer"))
             .putMember("BypassGovernanceRetention", Schemas1.BYPASS_GOVERNANCE_RETENTION,
                     new HttpHeaderTrait("x-amz-bypass-governance-retention"))
             .putMember("ExpectedBucketOwner", Schemas.ACCOUNT_ID,
                     new HttpHeaderTrait("x-amz-expected-bucket-owner"))
             .putMember("ChecksumAlgorithm", ChecksumAlgorithm.$SCHEMA,
                     new HttpHeaderTrait("x-amz-sdk-checksum-algorithm"))
             .builderSupplier(DeleteObjectsInput::builder)
             .shapeClass(DeleteObjectsInput.class)
             .build();

    static final Schema DELETE_MARKER_VERSION_ID = Schema.createString(ShapeId.from("com.amazonaws.s3#DeleteMarkerVersionId"));
    static final Schema DELETED_OBJECT = Schema.structureBuilder(ShapeId.from("com.amazonaws.s3#DeletedObject"))
             .putMember("Key", Schemas.OBJECT_KEY)
             .putMember("VersionId", Schemas.OBJECT_VERSION_ID)
             .putMember("DeleteMarker", Schemas1.DELETE_MARKER)
             .putMember("DeleteMarkerVersionId", Schemas1.DELETE_MARKER_VERSION_ID)
             .builderSupplier(DeletedObject::builder)
             .shapeClass(DeletedObject.class)
             .build();

    static final Schema DELETED_OBJECTS = Schema.listBuilder(ShapeId.from("com.amazonaws.s3#DeletedObjects"))
        .putMember("member", Schemas1.DELETED_OBJECT)
        .build();

    static final Schema CODE = Schema.createString(ShapeId.from("com.amazonaws.s3#Code"));
    static final Schema MESSAGE = Schema.createString(ShapeId.from("com.amazonaws.s3#Message"));
    static final Schema ERROR = Schema.structureBuilder(ShapeId.from("com.amazonaws.s3#Error"))
             .putMember("Key", Schemas.OBJECT_KEY)
             .putMember("VersionId", Schemas.OBJECT_VERSION_ID)
             .putMember("Code", Schemas1.CODE)
             .putMember("Message", Schemas1.MESSAGE)
             .builderSupplier(Error::builder)
             .shapeClass(Error.class)
             .build();

    static final Schema ERRORS = Schema.listBuilder(ShapeId.from("com.amazonaws.s3#Errors"))
        .putMember("member", Schemas1.ERROR)
        .build();

    static final Schema DELETE_OBJECTS_OUTPUT = Schema.structureBuilder(ShapeId.from("com.amazonaws.s3#DeleteObjectsOutput"),
            new XmlNameTrait("DeleteResult"))
             .putMember("Deleted", Schemas1.DELETED_OBJECTS,
                     new XmlFlattenedTrait())
             .putMember("RequestCharged", RequestCharged.$SCHEMA,
                     new HttpHeaderTrait("x-amz-request-charged"))
             .putMember("Errors", Schemas1.ERRORS,
                     new XmlNameTrait("Error"),
                     new XmlFlattenedTrait())
             .builderSupplier(DeleteObjectsOutput::builder)
             .shapeClass(DeleteObjectsOutput.class)
             .build();

    static final Schema DELETE_OBJECT_TAGGING_INPUT = Schema.structureBuilder(ShapeId.from("com.amazonaws.s3#DeleteObjectTaggingRequest"))
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
             .putMember("ExpectedBucketOwner", Schemas.ACCOUNT_ID,
                     new HttpHeaderTrait("x-amz-expected-bucket-owner"))
             .builderSupplier(DeleteObjectTaggingInput::builder)
             .shapeClass(DeleteObjectTaggingInput.class)
             .build();

    static final Schema DELETE_OBJECT_TAGGING_OUTPUT = Schema.structureBuilder(ShapeId.from("com.amazonaws.s3#DeleteObjectTaggingOutput"))
             .putMember("VersionId", Schemas.OBJECT_VERSION_ID,
                     new HttpHeaderTrait("x-amz-version-id"))
             .builderSupplier(DeleteObjectTaggingOutput::builder)
             .shapeClass(DeleteObjectTaggingOutput.class)
             .build();

    static final Schema DELETE_PUBLIC_ACCESS_BLOCK_INPUT = Schema.structureBuilder(ShapeId.from("com.amazonaws.s3#DeletePublicAccessBlockRequest"))
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
             .builderSupplier(DeletePublicAccessBlockInput::builder)
             .shapeClass(DeletePublicAccessBlockInput.class)
             .build();

    static final Schema DELETE_PUBLIC_ACCESS_BLOCK_OUTPUT = Schema.structureBuilder(ShapeId.from("com.amazonaws.s3#DeletePublicAccessBlockOutput"),
            new UnitTypeTrait()).builderSupplier(DeletePublicAccessBlockOutput::builder)
             .shapeClass(DeletePublicAccessBlockOutput.class)
             .build();

    static final Schema GET_BUCKET_ABAC_INPUT = Schema.structureBuilder(ShapeId.from("com.amazonaws.s3#GetBucketAbacRequest"))
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
             .builderSupplier(GetBucketAbacInput::builder)
             .shapeClass(GetBucketAbacInput.class)
             .build();

    static final Schema GET_BUCKET_ABAC_OUTPUT = Schema.structureBuilder(ShapeId.from("com.amazonaws.s3#GetBucketAbacOutput"))
             .putMember("AbacStatus", Schemas.ABAC_STATUS,
                     new HttpPayloadTrait())
             .builderSupplier(GetBucketAbacOutput::builder)
             .shapeClass(GetBucketAbacOutput.class)
             .build();

    static final Schema GET_BUCKET_ACCELERATE_CONFIGURATION_INPUT = Schema.structureBuilder(ShapeId.from("com.amazonaws.s3#GetBucketAccelerateConfigurationRequest"))
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
             .putMember("RequestPayer", RequestPayer.$SCHEMA,
                     new HttpHeaderTrait("x-amz-request-payer"))
             .builderSupplier(GetBucketAccelerateConfigurationInput::builder)
             .shapeClass(GetBucketAccelerateConfigurationInput.class)
             .build();

    static final Schema GET_BUCKET_ACCELERATE_CONFIGURATION_OUTPUT = Schema.structureBuilder(ShapeId.from("com.amazonaws.s3#GetBucketAccelerateConfigurationOutput"),
            new XmlNameTrait("AccelerateConfiguration"))
             .putMember("Status", BucketAccelerateStatus.$SCHEMA)
             .putMember("RequestCharged", RequestCharged.$SCHEMA,
                     new HttpHeaderTrait("x-amz-request-charged"))
             .builderSupplier(GetBucketAccelerateConfigurationOutput::builder)
             .shapeClass(GetBucketAccelerateConfigurationOutput.class)
             .build();

    static final Schema GET_BUCKET_ACL_INPUT = Schema.structureBuilder(ShapeId.from("com.amazonaws.s3#GetBucketAclRequest"))
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
             .builderSupplier(GetBucketAclInput::builder)
             .shapeClass(GetBucketAclInput.class)
             .build();

    static final Schema GET_BUCKET_ACL_OUTPUT = Schema.structureBuilder(ShapeId.from("com.amazonaws.s3#GetBucketAclOutput"),
            new XmlNameTrait("AccessControlPolicy"))
             .putMember("Owner", Schemas.OWNER)
             .putMember("Grants", Schemas.GRANTS,
                     new XmlNameTrait("AccessControlList"))
             .builderSupplier(GetBucketAclOutput::builder)
             .shapeClass(GetBucketAclOutput.class)
             .build();

    static final Schema GET_BUCKET_ANALYTICS_CONFIGURATION_INPUT = Schema.structureBuilder(ShapeId.from("com.amazonaws.s3#GetBucketAnalyticsConfigurationRequest"))
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
             .putMember("ExpectedBucketOwner", Schemas.ACCOUNT_ID,
                     new HttpHeaderTrait("x-amz-expected-bucket-owner"))
             .builderSupplier(GetBucketAnalyticsConfigurationInput::builder)
             .shapeClass(GetBucketAnalyticsConfigurationInput.class)
             .build();

    static final Schema PREFIX = Schema.createString(ShapeId.from("com.amazonaws.s3#Prefix"));
    static final Schema ANALYTICS_AND_OPERATOR = Schema.structureBuilder(ShapeId.from("com.amazonaws.s3#AnalyticsAndOperator"))
             .putMember("Prefix", Schemas1.PREFIX)
             .putMember("Tags", Schemas.TAG_SET,
                     new XmlNameTrait("Tag"),
                     new XmlFlattenedTrait())
             .builderSupplier(AnalyticsAndOperator::builder)
             .shapeClass(AnalyticsAndOperator.class)
             .build();

    static final Schema ANALYTICS_FILTER = Schema.unionBuilder(ShapeId.from("com.amazonaws.s3#AnalyticsFilter"))
             .putMember("Prefix", Schemas1.PREFIX)
             .putMember("Tag", Schemas.TAG)
             .putMember("And", Schemas1.ANALYTICS_AND_OPERATOR)
             .builderSupplier(AnalyticsFilter::builder)
             .shapeClass(AnalyticsFilter.class)
             .build();

    static final Schema ANALYTICS_S3_BUCKET_DESTINATION = Schema.structureBuilder(ShapeId.from("com.amazonaws.s3#AnalyticsS3BucketDestination"))
             .putMember("Format", AnalyticsS3ExportFileFormat.$SCHEMA,
                     new RequiredTrait())
             .putMember("BucketAccountId", Schemas.ACCOUNT_ID)
             .putMember("Bucket", Schemas.BUCKET_NAME,
                     new RequiredTrait())
             .putMember("Prefix", Schemas1.PREFIX)
             .builderSupplier(AnalyticsS3BucketDestination::builder)
             .shapeClass(AnalyticsS3BucketDestination.class)
             .build();

    static final Schema ANALYTICS_EXPORT_DESTINATION = Schema.structureBuilder(ShapeId.from("com.amazonaws.s3#AnalyticsExportDestination"))
             .putMember("S3BucketDestination", Schemas1.ANALYTICS_S3_BUCKET_DESTINATION,
                     new RequiredTrait())
             .builderSupplier(AnalyticsExportDestination::builder)
             .shapeClass(AnalyticsExportDestination.class)
             .build();

    static final Schema STORAGE_CLASS_ANALYSIS_DATA_EXPORT = Schema.structureBuilder(ShapeId.from("com.amazonaws.s3#StorageClassAnalysisDataExport"))
             .putMember("OutputSchemaVersion", StorageClassAnalysisSchemaVersion.$SCHEMA,
                     new RequiredTrait())
             .putMember("Destination", Schemas1.ANALYTICS_EXPORT_DESTINATION,
                     new RequiredTrait())
             .builderSupplier(StorageClassAnalysisDataExport::builder)
             .shapeClass(StorageClassAnalysisDataExport.class)
             .build();

    private Schemas1() {}
}
