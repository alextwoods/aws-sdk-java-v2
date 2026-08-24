package software.amazon.awssdk.benchmark.smithyjava.s3.model;

import software.amazon.smithy.java.core.schema.Schema;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.model.traits.HttpHeaderTrait;
import software.amazon.smithy.model.traits.HttpLabelTrait;
import software.amazon.smithy.model.traits.HttpPayloadTrait;
import software.amazon.smithy.model.traits.HttpQueryTrait;
import software.amazon.smithy.model.traits.RequiredTrait;
import software.amazon.smithy.model.traits.TimestampFormatTrait;
import software.amazon.smithy.model.traits.XmlFlattenedTrait;
import software.amazon.smithy.model.traits.XmlNameTrait;
import software.amazon.smithy.model.traits.XmlNamespaceTrait;
import software.amazon.smithy.rulesengine.traits.ContextParamTrait;

/**
 * Defines schemas for shapes in the model package.
 */
final class Schemas2 {
    static final Schema STORAGE_CLASS_ANALYSIS = Schema.structureBuilder(ShapeId.from("com.amazonaws.s3#StorageClassAnalysis"))
             .putMember("DataExport", Schemas1.STORAGE_CLASS_ANALYSIS_DATA_EXPORT)
             .builderSupplier(StorageClassAnalysis::builder)
             .shapeClass(StorageClassAnalysis.class)
             .build();

    static final Schema ANALYTICS_CONFIGURATION = Schema.structureBuilder(ShapeId.from("com.amazonaws.s3#AnalyticsConfiguration"))
             .putMember("Id", Schemas1.ANALYTICS_ID,
                     new RequiredTrait())
             .putMember("Filter", Schemas1.ANALYTICS_FILTER)
             .putMember("StorageClassAnalysis", Schemas2.STORAGE_CLASS_ANALYSIS,
                     new RequiredTrait())
             .builderSupplier(AnalyticsConfiguration::builder)
             .shapeClass(AnalyticsConfiguration.class)
             .build();

    static final Schema GET_BUCKET_ANALYTICS_CONFIGURATION_OUTPUT = Schema.structureBuilder(ShapeId.from("com.amazonaws.s3#GetBucketAnalyticsConfigurationOutput"))
             .putMember("AnalyticsConfiguration", Schemas2.ANALYTICS_CONFIGURATION,
                     new HttpPayloadTrait())
             .builderSupplier(GetBucketAnalyticsConfigurationOutput::builder)
             .shapeClass(GetBucketAnalyticsConfigurationOutput.class)
             .build();

    static final Schema GET_BUCKET_CORS_INPUT = Schema.structureBuilder(ShapeId.from("com.amazonaws.s3#GetBucketCorsRequest"))
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
             .builderSupplier(GetBucketCorsInput::builder)
             .shapeClass(GetBucketCorsInput.class)
             .build();

    static final Schema EXPOSE_HEADER = Schema.createString(ShapeId.from("com.amazonaws.s3#ExposeHeader"));
    static final Schema EXPOSE_HEADERS = Schema.listBuilder(ShapeId.from("com.amazonaws.s3#ExposeHeaders"))
        .putMember("member", Schemas2.EXPOSE_HEADER)
        .build();

    static final Schema MAX_AGE_SECONDS = Schema.createInteger(ShapeId.from("com.amazonaws.s3#MaxAgeSeconds"));
    static final Schema CORS_RULE = Schema.structureBuilder(ShapeId.from("com.amazonaws.s3#CORSRule"))
             .putMember("ID", Schemas.COM_AMAZONAWS_S3_ID)
             .putMember("AllowedHeaders", Schemas.ALLOWED_HEADERS,
                     new XmlNameTrait("AllowedHeader"),
                     new XmlFlattenedTrait())
             .putMember("AllowedMethods", Schemas.ALLOWED_METHODS,
                     new XmlNameTrait("AllowedMethod"),
                     new RequiredTrait(),
                     new XmlFlattenedTrait())
             .putMember("AllowedOrigins", Schemas.ALLOWED_ORIGINS,
                     new XmlNameTrait("AllowedOrigin"),
                     new RequiredTrait(),
                     new XmlFlattenedTrait())
             .putMember("ExposeHeaders", Schemas2.EXPOSE_HEADERS,
                     new XmlNameTrait("ExposeHeader"),
                     new XmlFlattenedTrait())
             .putMember("MaxAgeSeconds", Schemas2.MAX_AGE_SECONDS)
             .builderSupplier(CORSRule::builder)
             .shapeClass(CORSRule.class)
             .build();

    static final Schema CORS_RULES = Schema.listBuilder(ShapeId.from("com.amazonaws.s3#CORSRules"))
        .putMember("member", Schemas2.CORS_RULE)
        .build();

    static final Schema GET_BUCKET_CORS_OUTPUT = Schema.structureBuilder(ShapeId.from("com.amazonaws.s3#GetBucketCorsOutput"),
            new XmlNameTrait("CORSConfiguration"))
             .putMember("CORSRules", Schemas2.CORS_RULES,
                     new XmlNameTrait("CORSRule"),
                     new XmlFlattenedTrait())
             .builderSupplier(GetBucketCorsOutput::builder)
             .shapeClass(GetBucketCorsOutput.class)
             .build();

    static final Schema GET_BUCKET_ENCRYPTION_INPUT = Schema.structureBuilder(ShapeId.from("com.amazonaws.s3#GetBucketEncryptionRequest"))
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
             .builderSupplier(GetBucketEncryptionInput::builder)
             .shapeClass(GetBucketEncryptionInput.class)
             .build();

    static final Schema SERVER_SIDE_ENCRYPTION_BY_DEFAULT = Schema.structureBuilder(ShapeId.from("com.amazonaws.s3#ServerSideEncryptionByDefault"))
             .putMember("SSEAlgorithm", ServerSideEncryption.$SCHEMA,
                     new RequiredTrait())
             .putMember("KMSMasterKeyID", Schemas.SSEKMS_KEY_ID)
             .builderSupplier(ServerSideEncryptionByDefault::builder)
             .shapeClass(ServerSideEncryptionByDefault.class)
             .build();

    static final Schema ENCRYPTION_TYPE_LIST = Schema.listBuilder(ShapeId.from("com.amazonaws.s3#EncryptionTypeList"))
        .putMember("member", EncryptionType.$SCHEMA,
                new XmlNameTrait("EncryptionType"))
        .build();

    static final Schema BLOCKED_ENCRYPTION_TYPES = Schema.structureBuilder(ShapeId.from("com.amazonaws.s3#BlockedEncryptionTypes"))
             .putMember("EncryptionType", Schemas2.ENCRYPTION_TYPE_LIST,
                     new XmlFlattenedTrait())
             .builderSupplier(BlockedEncryptionTypes::builder)
             .shapeClass(BlockedEncryptionTypes.class)
             .build();

    static final Schema SERVER_SIDE_ENCRYPTION_RULE = Schema.structureBuilder(ShapeId.from("com.amazonaws.s3#ServerSideEncryptionRule"))
             .putMember("ApplyServerSideEncryptionByDefault", Schemas2.SERVER_SIDE_ENCRYPTION_BY_DEFAULT)
             .putMember("BucketKeyEnabled", Schemas.BUCKET_KEY_ENABLED)
             .putMember("BlockedEncryptionTypes", Schemas2.BLOCKED_ENCRYPTION_TYPES)
             .builderSupplier(ServerSideEncryptionRule::builder)
             .shapeClass(ServerSideEncryptionRule.class)
             .build();

    static final Schema SERVER_SIDE_ENCRYPTION_RULES = Schema.listBuilder(ShapeId.from("com.amazonaws.s3#ServerSideEncryptionRules"))
        .putMember("member", Schemas2.SERVER_SIDE_ENCRYPTION_RULE)
        .build();

    static final Schema SERVER_SIDE_ENCRYPTION_CONFIGURATION = Schema.structureBuilder(ShapeId.from("com.amazonaws.s3#ServerSideEncryptionConfiguration"))
             .putMember("Rules", Schemas2.SERVER_SIDE_ENCRYPTION_RULES,
                     new XmlNameTrait("Rule"),
                     new RequiredTrait(),
                     new XmlFlattenedTrait())
             .builderSupplier(ServerSideEncryptionConfiguration::builder)
             .shapeClass(ServerSideEncryptionConfiguration.class)
             .build();

    static final Schema GET_BUCKET_ENCRYPTION_OUTPUT = Schema.structureBuilder(ShapeId.from("com.amazonaws.s3#GetBucketEncryptionOutput"))
             .putMember("ServerSideEncryptionConfiguration", Schemas2.SERVER_SIDE_ENCRYPTION_CONFIGURATION,
                     new HttpPayloadTrait())
             .builderSupplier(GetBucketEncryptionOutput::builder)
             .shapeClass(GetBucketEncryptionOutput.class)
             .build();

    static final Schema GET_BUCKET_INTELLIGENT_TIERING_CONFIGURATION_INPUT = Schema.structureBuilder(ShapeId.from("com.amazonaws.s3#GetBucketIntelligentTieringConfigurationRequest"))
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
             .builderSupplier(GetBucketIntelligentTieringConfigurationInput::builder)
             .shapeClass(GetBucketIntelligentTieringConfigurationInput.class)
             .build();

    static final Schema INTELLIGENT_TIERING_AND_OPERATOR = Schema.structureBuilder(ShapeId.from("com.amazonaws.s3#IntelligentTieringAndOperator"))
             .putMember("Prefix", Schemas1.PREFIX)
             .putMember("Tags", Schemas.TAG_SET,
                     new XmlNameTrait("Tag"),
                     new XmlFlattenedTrait())
             .builderSupplier(IntelligentTieringAndOperator::builder)
             .shapeClass(IntelligentTieringAndOperator.class)
             .build();

    static final Schema INTELLIGENT_TIERING_FILTER = Schema.structureBuilder(ShapeId.from("com.amazonaws.s3#IntelligentTieringFilter"))
             .putMember("Prefix", Schemas1.PREFIX)
             .putMember("Tag", Schemas.TAG)
             .putMember("And", Schemas2.INTELLIGENT_TIERING_AND_OPERATOR)
             .builderSupplier(IntelligentTieringFilter::builder)
             .shapeClass(IntelligentTieringFilter.class)
             .build();

    static final Schema INTELLIGENT_TIERING_DAYS = Schema.createInteger(ShapeId.from("com.amazonaws.s3#IntelligentTieringDays"));
    static final Schema TIERING = Schema.structureBuilder(ShapeId.from("com.amazonaws.s3#Tiering"))
             .putMember("Days", Schemas2.INTELLIGENT_TIERING_DAYS,
                     new RequiredTrait())
             .putMember("AccessTier", IntelligentTieringAccessTier.$SCHEMA,
                     new RequiredTrait())
             .builderSupplier(Tiering::builder)
             .shapeClass(Tiering.class)
             .build();

    static final Schema TIERING_LIST = Schema.listBuilder(ShapeId.from("com.amazonaws.s3#TieringList"))
        .putMember("member", Schemas2.TIERING)
        .build();

    static final Schema INTELLIGENT_TIERING_CONFIGURATION = Schema.structureBuilder(ShapeId.from("com.amazonaws.s3#IntelligentTieringConfiguration"))
             .putMember("Id", Schemas1.INTELLIGENT_TIERING_ID,
                     new RequiredTrait())
             .putMember("Filter", Schemas2.INTELLIGENT_TIERING_FILTER)
             .putMember("Status", IntelligentTieringStatus.$SCHEMA,
                     new RequiredTrait())
             .putMember("Tierings", Schemas2.TIERING_LIST,
                     new XmlNameTrait("Tiering"),
                     new RequiredTrait(),
                     new XmlFlattenedTrait())
             .builderSupplier(IntelligentTieringConfiguration::builder)
             .shapeClass(IntelligentTieringConfiguration.class)
             .build();

    static final Schema GET_BUCKET_INTELLIGENT_TIERING_CONFIGURATION_OUTPUT = Schema.structureBuilder(ShapeId.from("com.amazonaws.s3#GetBucketIntelligentTieringConfigurationOutput"))
             .putMember("IntelligentTieringConfiguration", Schemas2.INTELLIGENT_TIERING_CONFIGURATION,
                     new HttpPayloadTrait())
             .builderSupplier(GetBucketIntelligentTieringConfigurationOutput::builder)
             .shapeClass(GetBucketIntelligentTieringConfigurationOutput.class)
             .build();

    static final Schema GET_BUCKET_INVENTORY_CONFIGURATION_INPUT = Schema.structureBuilder(ShapeId.from("com.amazonaws.s3#GetBucketInventoryConfigurationRequest"))
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
             .builderSupplier(GetBucketInventoryConfigurationInput::builder)
             .shapeClass(GetBucketInventoryConfigurationInput.class)
             .build();

    static final Schema COM_AMAZONAWS_S3_SSEKMS = Schema.structureBuilder(ShapeId.from("com.amazonaws.s3#SSEKMS"),
            new XmlNameTrait("SSE-KMS"))
             .putMember("KeyId", Schemas.SSEKMS_KEY_ID,
                     new RequiredTrait())
             .builderSupplier(SSEKMS::builder)
             .shapeClass(SSEKMS.class)
             .build();

    static final Schema COM_AMAZONAWS_S3_SSES3 = Schema.structureBuilder(ShapeId.from("com.amazonaws.s3#SSES3"),
            new XmlNameTrait("SSE-S3")).builderSupplier(SSES3::builder)
             .shapeClass(SSES3.class)
             .build();

    static final Schema INVENTORY_ENCRYPTION = Schema.structureBuilder(ShapeId.from("com.amazonaws.s3#InventoryEncryption"))
             .putMember("SSES3", Schemas2.COM_AMAZONAWS_S3_SSES3,
                     new XmlNameTrait("SSE-S3"))
             .putMember("SSEKMS", Schemas2.COM_AMAZONAWS_S3_SSEKMS,
                     new XmlNameTrait("SSE-KMS"))
             .builderSupplier(InventoryEncryption::builder)
             .shapeClass(InventoryEncryption.class)
             .build();

    static final Schema INVENTORY_S3_BUCKET_DESTINATION = Schema.structureBuilder(ShapeId.from("com.amazonaws.s3#InventoryS3BucketDestination"))
             .putMember("AccountId", Schemas.ACCOUNT_ID)
             .putMember("Bucket", Schemas.BUCKET_NAME,
                     new RequiredTrait())
             .putMember("Format", InventoryFormat.$SCHEMA,
                     new RequiredTrait())
             .putMember("Prefix", Schemas1.PREFIX)
             .putMember("Encryption", Schemas2.INVENTORY_ENCRYPTION)
             .builderSupplier(InventoryS3BucketDestination::builder)
             .shapeClass(InventoryS3BucketDestination.class)
             .build();

    static final Schema INVENTORY_DESTINATION = Schema.structureBuilder(ShapeId.from("com.amazonaws.s3#InventoryDestination"))
             .putMember("S3BucketDestination", Schemas2.INVENTORY_S3_BUCKET_DESTINATION,
                     new RequiredTrait())
             .builderSupplier(InventoryDestination::builder)
             .shapeClass(InventoryDestination.class)
             .build();

    static final Schema INVENTORY_FILTER = Schema.structureBuilder(ShapeId.from("com.amazonaws.s3#InventoryFilter"))
             .putMember("Prefix", Schemas1.PREFIX,
                     new RequiredTrait())
             .builderSupplier(InventoryFilter::builder)
             .shapeClass(InventoryFilter.class)
             .build();

    static final Schema IS_ENABLED = Schema.createBoolean(ShapeId.from("com.amazonaws.s3#IsEnabled"));
    static final Schema INVENTORY_OPTIONAL_FIELDS = Schema.listBuilder(ShapeId.from("com.amazonaws.s3#InventoryOptionalFields"))
        .putMember("member", InventoryOptionalField.$SCHEMA,
                new XmlNameTrait("Field"))
        .build();

    static final Schema INVENTORY_SCHEDULE = Schema.structureBuilder(ShapeId.from("com.amazonaws.s3#InventorySchedule"))
             .putMember("Frequency", InventoryFrequency.$SCHEMA,
                     new RequiredTrait())
             .builderSupplier(InventorySchedule::builder)
             .shapeClass(InventorySchedule.class)
             .build();

    static final Schema INVENTORY_CONFIGURATION = Schema.structureBuilder(ShapeId.from("com.amazonaws.s3#InventoryConfiguration"))
             .putMember("Destination", Schemas2.INVENTORY_DESTINATION,
                     new RequiredTrait())
             .putMember("IsEnabled", Schemas2.IS_ENABLED,
                     new RequiredTrait())
             .putMember("Filter", Schemas2.INVENTORY_FILTER)
             .putMember("Id", Schemas1.INVENTORY_ID,
                     new RequiredTrait())
             .putMember("IncludedObjectVersions", InventoryIncludedObjectVersions.$SCHEMA,
                     new RequiredTrait())
             .putMember("OptionalFields", Schemas2.INVENTORY_OPTIONAL_FIELDS)
             .putMember("Schedule", Schemas2.INVENTORY_SCHEDULE,
                     new RequiredTrait())
             .builderSupplier(InventoryConfiguration::builder)
             .shapeClass(InventoryConfiguration.class)
             .build();

    static final Schema GET_BUCKET_INVENTORY_CONFIGURATION_OUTPUT = Schema.structureBuilder(ShapeId.from("com.amazonaws.s3#GetBucketInventoryConfigurationOutput"))
             .putMember("InventoryConfiguration", Schemas2.INVENTORY_CONFIGURATION,
                     new HttpPayloadTrait())
             .builderSupplier(GetBucketInventoryConfigurationOutput::builder)
             .shapeClass(GetBucketInventoryConfigurationOutput.class)
             .build();

    static final Schema GET_BUCKET_LIFECYCLE_CONFIGURATION_INPUT = Schema.structureBuilder(ShapeId.from("com.amazonaws.s3#GetBucketLifecycleConfigurationRequest"))
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
             .builderSupplier(GetBucketLifecycleConfigurationInput::builder)
             .shapeClass(GetBucketLifecycleConfigurationInput.class)
             .build();

    static final Schema DATE = Schema.createTimestamp(ShapeId.from("com.amazonaws.s3#Date"),
            new TimestampFormatTrait("date-time"));
    static final Schema DAYS = Schema.createInteger(ShapeId.from("com.amazonaws.s3#Days"));
    static final Schema EXPIRED_OBJECT_DELETE_MARKER = Schema.createBoolean(ShapeId.from("com.amazonaws.s3#ExpiredObjectDeleteMarker"));
    static final Schema LIFECYCLE_EXPIRATION = Schema.structureBuilder(ShapeId.from("com.amazonaws.s3#LifecycleExpiration"))
             .putMember("Date", Schemas2.DATE)
             .putMember("Days", Schemas2.DAYS)
             .putMember("ExpiredObjectDeleteMarker", Schemas2.EXPIRED_OBJECT_DELETE_MARKER)
             .builderSupplier(LifecycleExpiration::builder)
             .shapeClass(LifecycleExpiration.class)
             .build();

    static final Schema OBJECT_SIZE_GREATER_THAN_BYTES = Schema.createLong(ShapeId.from("com.amazonaws.s3#ObjectSizeGreaterThanBytes"));
    static final Schema OBJECT_SIZE_LESS_THAN_BYTES = Schema.createLong(ShapeId.from("com.amazonaws.s3#ObjectSizeLessThanBytes"));
    static final Schema LIFECYCLE_RULE_AND_OPERATOR = Schema.structureBuilder(ShapeId.from("com.amazonaws.s3#LifecycleRuleAndOperator"))
             .putMember("Prefix", Schemas1.PREFIX)
             .putMember("Tags", Schemas.TAG_SET,
                     new XmlNameTrait("Tag"),
                     new XmlFlattenedTrait())
             .putMember("ObjectSizeGreaterThan", Schemas2.OBJECT_SIZE_GREATER_THAN_BYTES)
             .putMember("ObjectSizeLessThan", Schemas2.OBJECT_SIZE_LESS_THAN_BYTES)
             .builderSupplier(LifecycleRuleAndOperator::builder)
             .shapeClass(LifecycleRuleAndOperator.class)
             .build();

    static final Schema LIFECYCLE_RULE_FILTER = Schema.structureBuilder(ShapeId.from("com.amazonaws.s3#LifecycleRuleFilter"))
             .putMember("Prefix", Schemas1.PREFIX)
             .putMember("Tag", Schemas.TAG)
             .putMember("ObjectSizeGreaterThan", Schemas2.OBJECT_SIZE_GREATER_THAN_BYTES)
             .putMember("ObjectSizeLessThan", Schemas2.OBJECT_SIZE_LESS_THAN_BYTES)
             .putMember("And", Schemas2.LIFECYCLE_RULE_AND_OPERATOR)
             .builderSupplier(LifecycleRuleFilter::builder)
             .shapeClass(LifecycleRuleFilter.class)
             .build();

    static final Schema VERSION_COUNT = Schema.createInteger(ShapeId.from("com.amazonaws.s3#VersionCount"));
    static final Schema NONCURRENT_VERSION_EXPIRATION = Schema.structureBuilder(ShapeId.from("com.amazonaws.s3#NoncurrentVersionExpiration"))
             .putMember("NoncurrentDays", Schemas2.DAYS)
             .putMember("NewerNoncurrentVersions", Schemas2.VERSION_COUNT)
             .builderSupplier(NoncurrentVersionExpiration::builder)
             .shapeClass(NoncurrentVersionExpiration.class)
             .build();

    static final Schema NONCURRENT_VERSION_TRANSITION = Schema.structureBuilder(ShapeId.from("com.amazonaws.s3#NoncurrentVersionTransition"))
             .putMember("NoncurrentDays", Schemas2.DAYS)
             .putMember("StorageClass", TransitionStorageClass.$SCHEMA)
             .putMember("NewerNoncurrentVersions", Schemas2.VERSION_COUNT)
             .builderSupplier(NoncurrentVersionTransition::builder)
             .shapeClass(NoncurrentVersionTransition.class)
             .build();

    static final Schema NONCURRENT_VERSION_TRANSITION_LIST = Schema.listBuilder(ShapeId.from("com.amazonaws.s3#NoncurrentVersionTransitionList"))
        .putMember("member", Schemas2.NONCURRENT_VERSION_TRANSITION)
        .build();

    static final Schema TRANSITION = Schema.structureBuilder(ShapeId.from("com.amazonaws.s3#Transition"))
             .putMember("Date", Schemas2.DATE)
             .putMember("Days", Schemas2.DAYS)
             .putMember("StorageClass", TransitionStorageClass.$SCHEMA)
             .builderSupplier(Transition::builder)
             .shapeClass(Transition.class)
             .build();

    static final Schema TRANSITION_LIST = Schema.listBuilder(ShapeId.from("com.amazonaws.s3#TransitionList"))
        .putMember("member", Schemas2.TRANSITION)
        .build();

    static final Schema LIFECYCLE_RULE = Schema.structureBuilder(ShapeId.from("com.amazonaws.s3#LifecycleRule"))
             .putMember("Expiration", Schemas2.LIFECYCLE_EXPIRATION)
             .putMember("ID", Schemas.COM_AMAZONAWS_S3_ID)
             .putMember("Prefix", Schemas1.PREFIX)
             .putMember("Filter", Schemas2.LIFECYCLE_RULE_FILTER)
             .putMember("Status", ExpirationStatus.$SCHEMA,
                     new RequiredTrait())
             .putMember("Transitions", Schemas2.TRANSITION_LIST,
                     new XmlNameTrait("Transition"),
                     new XmlFlattenedTrait())
             .putMember("NoncurrentVersionTransitions", Schemas2.NONCURRENT_VERSION_TRANSITION_LIST,
                     new XmlNameTrait("NoncurrentVersionTransition"),
                     new XmlFlattenedTrait())
             .putMember("NoncurrentVersionExpiration", Schemas2.NONCURRENT_VERSION_EXPIRATION)
             .putMember("AbortIncompleteMultipartUpload", Schemas.ABORT_INCOMPLETE_MULTIPART_UPLOAD)
             .builderSupplier(LifecycleRule::builder)
             .shapeClass(LifecycleRule.class)
             .build();

    static final Schema LIFECYCLE_RULES = Schema.listBuilder(ShapeId.from("com.amazonaws.s3#LifecycleRules"))
        .putMember("member", Schemas2.LIFECYCLE_RULE)
        .build();

    static final Schema GET_BUCKET_LIFECYCLE_CONFIGURATION_OUTPUT = Schema.structureBuilder(ShapeId.from("com.amazonaws.s3#GetBucketLifecycleConfigurationOutput"),
            new XmlNameTrait("LifecycleConfiguration"))
             .putMember("Rules", Schemas2.LIFECYCLE_RULES,
                     new XmlNameTrait("Rule"),
                     new XmlFlattenedTrait())
             .putMember("TransitionDefaultMinimumObjectSize", TransitionDefaultMinimumObjectSize.$SCHEMA,
                     new HttpHeaderTrait("x-amz-transition-default-minimum-object-size"))
             .builderSupplier(GetBucketLifecycleConfigurationOutput::builder)
             .shapeClass(GetBucketLifecycleConfigurationOutput.class)
             .build();

    static final Schema GET_BUCKET_LOCATION_INPUT = Schema.structureBuilder(ShapeId.from("com.amazonaws.s3#GetBucketLocationRequest"))
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
             .builderSupplier(GetBucketLocationInput::builder)
             .shapeClass(GetBucketLocationInput.class)
             .build();

    static final Schema GET_BUCKET_LOCATION_OUTPUT = Schema.structureBuilder(ShapeId.from("com.amazonaws.s3#GetBucketLocationOutput"),
            new XmlNameTrait("LocationConstraint"))
             .putMember("LocationConstraint", BucketLocationConstraint.$SCHEMA)
             .builderSupplier(GetBucketLocationOutput::builder)
             .shapeClass(GetBucketLocationOutput.class)
             .build();

    static final Schema GET_BUCKET_LOGGING_INPUT = Schema.structureBuilder(ShapeId.from("com.amazonaws.s3#GetBucketLoggingRequest"))
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
             .builderSupplier(GetBucketLoggingInput::builder)
             .shapeClass(GetBucketLoggingInput.class)
             .build();

    static final Schema TARGET_BUCKET = Schema.createString(ShapeId.from("com.amazonaws.s3#TargetBucket"));
    static final Schema TARGET_GRANT = Schema.structureBuilder(ShapeId.from("com.amazonaws.s3#TargetGrant"))
             .putMember("Grantee", Schemas.GRANTEE,
                     XmlNamespaceTrait.builder().uri("http://www.w3.org/2001/XMLSchema-instance").prefix("xsi").build())
             .putMember("Permission", BucketLogsPermission.$SCHEMA)
             .builderSupplier(TargetGrant::builder)
             .shapeClass(TargetGrant.class)
             .build();

    static final Schema TARGET_GRANTS = Schema.listBuilder(ShapeId.from("com.amazonaws.s3#TargetGrants"))
        .putMember("member", Schemas2.TARGET_GRANT,
                new XmlNameTrait("Grant"))
        .build();

    static final Schema PARTITIONED_PREFIX = Schema.structureBuilder(ShapeId.from("com.amazonaws.s3#PartitionedPrefix"),
            new XmlNameTrait("PartitionedPrefix"))
             .putMember("PartitionDateSource", PartitionDateSource.$SCHEMA)
             .builderSupplier(PartitionedPrefix::builder)
             .shapeClass(PartitionedPrefix.class)
             .build();

    static final Schema SIMPLE_PREFIX = Schema.structureBuilder(ShapeId.from("com.amazonaws.s3#SimplePrefix"),
            new XmlNameTrait("SimplePrefix")).builderSupplier(SimplePrefix::builder)
             .shapeClass(SimplePrefix.class)
             .build();

    static final Schema TARGET_OBJECT_KEY_FORMAT = Schema.structureBuilder(ShapeId.from("com.amazonaws.s3#TargetObjectKeyFormat"))
             .putMember("SimplePrefix", Schemas2.SIMPLE_PREFIX,
                     new XmlNameTrait("SimplePrefix"))
             .putMember("PartitionedPrefix", Schemas2.PARTITIONED_PREFIX,
                     new XmlNameTrait("PartitionedPrefix"))
             .builderSupplier(TargetObjectKeyFormat::builder)
             .shapeClass(TargetObjectKeyFormat.class)
             .build();

    static final Schema TARGET_PREFIX = Schema.createString(ShapeId.from("com.amazonaws.s3#TargetPrefix"));
    static final Schema LOGGING_ENABLED = Schema.structureBuilder(ShapeId.from("com.amazonaws.s3#LoggingEnabled"))
             .putMember("TargetBucket", Schemas2.TARGET_BUCKET,
                     new RequiredTrait())
             .putMember("TargetGrants", Schemas2.TARGET_GRANTS)
             .putMember("TargetPrefix", Schemas2.TARGET_PREFIX,
                     new RequiredTrait())
             .putMember("TargetObjectKeyFormat", Schemas2.TARGET_OBJECT_KEY_FORMAT)
             .builderSupplier(LoggingEnabled::builder)
             .shapeClass(LoggingEnabled.class)
             .build();

    static final Schema GET_BUCKET_LOGGING_OUTPUT = Schema.structureBuilder(ShapeId.from("com.amazonaws.s3#GetBucketLoggingOutput"),
            new XmlNameTrait("BucketLoggingStatus"))
             .putMember("LoggingEnabled", Schemas2.LOGGING_ENABLED)
             .builderSupplier(GetBucketLoggingOutput::builder)
             .shapeClass(GetBucketLoggingOutput.class)
             .build();

    static final Schema GET_BUCKET_METADATA_CONFIGURATION_INPUT = Schema.structureBuilder(ShapeId.from("com.amazonaws.s3#GetBucketMetadataConfigurationRequest"))
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
             .builderSupplier(GetBucketMetadataConfigurationInput::builder)
             .shapeClass(GetBucketMetadataConfigurationInput.class)
             .build();

    static final Schema ERROR_CODE = Schema.createString(ShapeId.from("com.amazonaws.s3#ErrorCode"));
    static final Schema ERROR_MESSAGE = Schema.createString(ShapeId.from("com.amazonaws.s3#ErrorMessage"));
    static final Schema ERROR_DETAILS = Schema.structureBuilder(ShapeId.from("com.amazonaws.s3#ErrorDetails"))
             .putMember("ErrorCode", Schemas2.ERROR_CODE)
             .putMember("ErrorMessage", Schemas2.ERROR_MESSAGE)
             .builderSupplier(ErrorDetails::builder)
             .shapeClass(ErrorDetails.class)
             .build();

    static final Schema S3_TABLES_ARN = Schema.createString(ShapeId.from("com.amazonaws.s3#S3TablesArn"));
    static final Schema METADATA_TABLE_STATUS = Schema.createString(ShapeId.from("com.amazonaws.s3#MetadataTableStatus"));
    static final Schema ANNOTATION_TABLE_CONFIGURATION_RESULT = Schema.structureBuilder(ShapeId.from("com.amazonaws.s3#AnnotationTableConfigurationResult"))
             .putMember("ConfigurationState", AnnotationConfigurationState.$SCHEMA,
                     new RequiredTrait())
             .putMember("TableStatus", Schemas2.METADATA_TABLE_STATUS)
             .putMember("Error", Schemas2.ERROR_DETAILS)
             .putMember("TableName", Schemas.S3_TABLES_NAME)
             .putMember("TableArn", Schemas2.S3_TABLES_ARN)
             .putMember("Role", Schemas.ROLE)
             .builderSupplier(AnnotationTableConfigurationResult::builder)
             .shapeClass(AnnotationTableConfigurationResult.class)
             .build();

    static final Schema S3_TABLES_NAMESPACE = Schema.createString(ShapeId.from("com.amazonaws.s3#S3TablesNamespace"));
    static final Schema DESTINATION_RESULT = Schema.structureBuilder(ShapeId.from("com.amazonaws.s3#DestinationResult"))
             .putMember("TableBucketType", S3TablesBucketType.$SCHEMA)
             .putMember("TableBucketArn", Schemas.S3_TABLES_BUCKET_ARN)
             .putMember("TableNamespace", Schemas2.S3_TABLES_NAMESPACE)
             .builderSupplier(DestinationResult::builder)
             .shapeClass(DestinationResult.class)
             .build();

    static final Schema INVENTORY_TABLE_CONFIGURATION_RESULT = Schema.structureBuilder(ShapeId.from("com.amazonaws.s3#InventoryTableConfigurationResult"))
             .putMember("ConfigurationState", InventoryConfigurationState.$SCHEMA,
                     new RequiredTrait())
             .putMember("TableStatus", Schemas2.METADATA_TABLE_STATUS)
             .putMember("Error", Schemas2.ERROR_DETAILS)
             .putMember("TableName", Schemas.S3_TABLES_NAME)
             .putMember("TableArn", Schemas2.S3_TABLES_ARN)
             .builderSupplier(InventoryTableConfigurationResult::builder)
             .shapeClass(InventoryTableConfigurationResult.class)
             .build();

    static final Schema JOURNAL_TABLE_CONFIGURATION_RESULT = Schema.structureBuilder(ShapeId.from("com.amazonaws.s3#JournalTableConfigurationResult"))
             .putMember("TableStatus", Schemas2.METADATA_TABLE_STATUS,
                     new RequiredTrait())
             .putMember("Error", Schemas2.ERROR_DETAILS)
             .putMember("TableName", Schemas.S3_TABLES_NAME,
                     new RequiredTrait())
             .putMember("TableArn", Schemas2.S3_TABLES_ARN)
             .putMember("RecordExpiration", Schemas.RECORD_EXPIRATION,
                     new RequiredTrait())
             .builderSupplier(JournalTableConfigurationResult::builder)
             .shapeClass(JournalTableConfigurationResult.class)
             .build();

    static final Schema METADATA_CONFIGURATION_RESULT = Schema.structureBuilder(ShapeId.from("com.amazonaws.s3#MetadataConfigurationResult"))
             .putMember("DestinationResult", Schemas2.DESTINATION_RESULT,
                     new RequiredTrait())
             .putMember("JournalTableConfigurationResult", Schemas2.JOURNAL_TABLE_CONFIGURATION_RESULT)
             .putMember("InventoryTableConfigurationResult", Schemas2.INVENTORY_TABLE_CONFIGURATION_RESULT)
             .putMember("AnnotationTableConfigurationResult", Schemas2.ANNOTATION_TABLE_CONFIGURATION_RESULT)
             .builderSupplier(MetadataConfigurationResult::builder)
             .shapeClass(MetadataConfigurationResult.class)
             .build();

    static final Schema GET_BUCKET_METADATA_CONFIGURATION_RESULT = Schema.structureBuilder(ShapeId.from("com.amazonaws.s3#GetBucketMetadataConfigurationResult"))
             .putMember("MetadataConfigurationResult", Schemas2.METADATA_CONFIGURATION_RESULT,
                     new RequiredTrait())
             .builderSupplier(GetBucketMetadataConfigurationResult::builder)
             .shapeClass(GetBucketMetadataConfigurationResult.class)
             .build();

    static final Schema GET_BUCKET_METADATA_CONFIGURATION_OUTPUT = Schema.structureBuilder(ShapeId.from("com.amazonaws.s3#GetBucketMetadataConfigurationOutput"))
             .putMember("GetBucketMetadataConfigurationResult", Schemas2.GET_BUCKET_METADATA_CONFIGURATION_RESULT,
                     new HttpPayloadTrait())
             .builderSupplier(GetBucketMetadataConfigurationOutput::builder)
             .shapeClass(GetBucketMetadataConfigurationOutput.class)
             .build();

    static final Schema GET_BUCKET_METADATA_TABLE_CONFIGURATION_INPUT = Schema.structureBuilder(ShapeId.from("com.amazonaws.s3#GetBucketMetadataTableConfigurationRequest"))
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
             .builderSupplier(GetBucketMetadataTableConfigurationInput::builder)
             .shapeClass(GetBucketMetadataTableConfigurationInput.class)
             .build();

    static final Schema S3_TABLES_DESTINATION_RESULT = Schema.structureBuilder(ShapeId.from("com.amazonaws.s3#S3TablesDestinationResult"))
             .putMember("TableBucketArn", Schemas.S3_TABLES_BUCKET_ARN,
                     new RequiredTrait())
             .putMember("TableName", Schemas.S3_TABLES_NAME,
                     new RequiredTrait())
             .putMember("TableArn", Schemas2.S3_TABLES_ARN,
                     new RequiredTrait())
             .putMember("TableNamespace", Schemas2.S3_TABLES_NAMESPACE,
                     new RequiredTrait())
             .builderSupplier(S3TablesDestinationResult::builder)
             .shapeClass(S3TablesDestinationResult.class)
             .build();

    static final Schema METADATA_TABLE_CONFIGURATION_RESULT = Schema.structureBuilder(ShapeId.from("com.amazonaws.s3#MetadataTableConfigurationResult"))
             .putMember("S3TablesDestinationResult", Schemas2.S3_TABLES_DESTINATION_RESULT,
                     new RequiredTrait())
             .builderSupplier(MetadataTableConfigurationResult::builder)
             .shapeClass(MetadataTableConfigurationResult.class)
             .build();

    static final Schema GET_BUCKET_METADATA_TABLE_CONFIGURATION_RESULT = Schema.structureBuilder(ShapeId.from("com.amazonaws.s3#GetBucketMetadataTableConfigurationResult"))
             .putMember("MetadataTableConfigurationResult", Schemas2.METADATA_TABLE_CONFIGURATION_RESULT,
                     new RequiredTrait())
             .putMember("Status", Schemas2.METADATA_TABLE_STATUS,
                     new RequiredTrait())
             .putMember("Error", Schemas2.ERROR_DETAILS)
             .builderSupplier(GetBucketMetadataTableConfigurationResult::builder)
             .shapeClass(GetBucketMetadataTableConfigurationResult.class)
             .build();

    static final Schema GET_BUCKET_METADATA_TABLE_CONFIGURATION_OUTPUT = Schema.structureBuilder(ShapeId.from("com.amazonaws.s3#GetBucketMetadataTableConfigurationOutput"))
             .putMember("GetBucketMetadataTableConfigurationResult", Schemas2.GET_BUCKET_METADATA_TABLE_CONFIGURATION_RESULT,
                     new HttpPayloadTrait())
             .builderSupplier(GetBucketMetadataTableConfigurationOutput::builder)
             .shapeClass(GetBucketMetadataTableConfigurationOutput.class)
             .build();

    static final Schema GET_BUCKET_METRICS_CONFIGURATION_INPUT = Schema.structureBuilder(ShapeId.from("com.amazonaws.s3#GetBucketMetricsConfigurationRequest"))
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
             .builderSupplier(GetBucketMetricsConfigurationInput::builder)
             .shapeClass(GetBucketMetricsConfigurationInput.class)
             .build();

    static final Schema METRICS_AND_OPERATOR = Schema.structureBuilder(ShapeId.from("com.amazonaws.s3#MetricsAndOperator"))
             .putMember("Prefix", Schemas1.PREFIX)
             .putMember("Tags", Schemas.TAG_SET,
                     new XmlNameTrait("Tag"),
                     new XmlFlattenedTrait())
             .putMember("AccessPointArn", Schemas.ACCESS_POINT_ARN)
             .builderSupplier(MetricsAndOperator::builder)
             .shapeClass(MetricsAndOperator.class)
             .build();

    static final Schema METRICS_FILTER = Schema.unionBuilder(ShapeId.from("com.amazonaws.s3#MetricsFilter"))
             .putMember("Prefix", Schemas1.PREFIX)
             .putMember("Tag", Schemas.TAG)
             .putMember("AccessPointArn", Schemas.ACCESS_POINT_ARN)
             .putMember("And", Schemas2.METRICS_AND_OPERATOR)
             .builderSupplier(MetricsFilter::builder)
             .shapeClass(MetricsFilter.class)
             .build();

    static final Schema METRICS_CONFIGURATION = Schema.structureBuilder(ShapeId.from("com.amazonaws.s3#MetricsConfiguration"))
             .putMember("Id", Schemas1.METRICS_ID,
                     new RequiredTrait())
             .putMember("Filter", Schemas2.METRICS_FILTER)
             .builderSupplier(MetricsConfiguration::builder)
             .shapeClass(MetricsConfiguration.class)
             .build();

    static final Schema GET_BUCKET_METRICS_CONFIGURATION_OUTPUT = Schema.structureBuilder(ShapeId.from("com.amazonaws.s3#GetBucketMetricsConfigurationOutput"))
             .putMember("MetricsConfiguration", Schemas2.METRICS_CONFIGURATION,
                     new HttpPayloadTrait())
             .builderSupplier(GetBucketMetricsConfigurationOutput::builder)
             .shapeClass(GetBucketMetricsConfigurationOutput.class)
             .build();

    static final Schema GET_BUCKET_NOTIFICATION_CONFIGURATION_INPUT = Schema.structureBuilder(ShapeId.from("com.amazonaws.s3#GetBucketNotificationConfigurationRequest"))
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
             .builderSupplier(GetBucketNotificationConfigurationInput::builder)
             .shapeClass(GetBucketNotificationConfigurationInput.class)
             .build();

    static final Schema EVENT_BRIDGE_CONFIGURATION = Schema.structureBuilder(ShapeId.from("com.amazonaws.s3#EventBridgeConfiguration")).builderSupplier(EventBridgeConfiguration::builder)
             .shapeClass(EventBridgeConfiguration.class)
             .build();

    static final Schema EVENT_LIST = Schema.listBuilder(ShapeId.from("com.amazonaws.s3#EventList"))
        .putMember("member", Event.$SCHEMA)
        .build();

    static final Schema FILTER_RULE_VALUE = Schema.createString(ShapeId.from("com.amazonaws.s3#FilterRuleValue"));
    static final Schema FILTER_RULE = Schema.structureBuilder(ShapeId.from("com.amazonaws.s3#FilterRule"))
             .putMember("Name", FilterRuleName.$SCHEMA)
             .putMember("Value", Schemas2.FILTER_RULE_VALUE)
             .builderSupplier(FilterRule::builder)
             .shapeClass(FilterRule.class)
             .build();

    static final Schema FILTER_RULE_LIST = Schema.listBuilder(ShapeId.from("com.amazonaws.s3#FilterRuleList"))
        .putMember("member", Schemas2.FILTER_RULE)
        .build();

    static final Schema S3_KEY_FILTER = Schema.structureBuilder(ShapeId.from("com.amazonaws.s3#S3KeyFilter"))
             .putMember("FilterRules", Schemas2.FILTER_RULE_LIST,
                     new XmlNameTrait("FilterRule"),
                     new XmlFlattenedTrait())
             .builderSupplier(S3KeyFilter::builder)
             .shapeClass(S3KeyFilter.class)
             .build();

    static final Schema NOTIFICATION_CONFIGURATION_FILTER = Schema.structureBuilder(ShapeId.from("com.amazonaws.s3#NotificationConfigurationFilter"))
             .putMember("Key", Schemas2.S3_KEY_FILTER,
                     new XmlNameTrait("S3Key"))
             .builderSupplier(NotificationConfigurationFilter::builder)
             .shapeClass(NotificationConfigurationFilter.class)
             .build();

    static final Schema NOTIFICATION_ID = Schema.createString(ShapeId.from("com.amazonaws.s3#NotificationId"));
    static final Schema LAMBDA_FUNCTION_ARN = Schema.createString(ShapeId.from("com.amazonaws.s3#LambdaFunctionArn"));
    static final Schema LAMBDA_FUNCTION_CONFIGURATION = Schema.structureBuilder(ShapeId.from("com.amazonaws.s3#LambdaFunctionConfiguration"))
             .putMember("Id", Schemas2.NOTIFICATION_ID)
             .putMember("LambdaFunctionArn", Schemas2.LAMBDA_FUNCTION_ARN,
                     new XmlNameTrait("CloudFunction"),
                     new RequiredTrait())
             .putMember("Events", Schemas2.EVENT_LIST,
                     new XmlNameTrait("Event"),
                     new RequiredTrait(),
                     new XmlFlattenedTrait())
             .putMember("Filter", Schemas2.NOTIFICATION_CONFIGURATION_FILTER)
             .builderSupplier(LambdaFunctionConfiguration::builder)
             .shapeClass(LambdaFunctionConfiguration.class)
             .build();

    static final Schema LAMBDA_FUNCTION_CONFIGURATION_LIST = Schema.listBuilder(ShapeId.from("com.amazonaws.s3#LambdaFunctionConfigurationList"))
        .putMember("member", Schemas2.LAMBDA_FUNCTION_CONFIGURATION)
        .build();

    static final Schema QUEUE_ARN = Schema.createString(ShapeId.from("com.amazonaws.s3#QueueArn"));
    static final Schema QUEUE_CONFIGURATION = Schema.structureBuilder(ShapeId.from("com.amazonaws.s3#QueueConfiguration"))
             .putMember("Id", Schemas2.NOTIFICATION_ID)
             .putMember("QueueArn", Schemas2.QUEUE_ARN,
                     new XmlNameTrait("Queue"),
                     new RequiredTrait())
             .putMember("Events", Schemas2.EVENT_LIST,
                     new XmlNameTrait("Event"),
                     new RequiredTrait(),
                     new XmlFlattenedTrait())
             .putMember("Filter", Schemas2.NOTIFICATION_CONFIGURATION_FILTER)
             .builderSupplier(QueueConfiguration::builder)
             .shapeClass(QueueConfiguration.class)
             .build();

    static final Schema QUEUE_CONFIGURATION_LIST = Schema.listBuilder(ShapeId.from("com.amazonaws.s3#QueueConfigurationList"))
        .putMember("member", Schemas2.QUEUE_CONFIGURATION)
        .build();

    static final Schema TOPIC_ARN = Schema.createString(ShapeId.from("com.amazonaws.s3#TopicArn"));
    static final Schema TOPIC_CONFIGURATION = Schema.structureBuilder(ShapeId.from("com.amazonaws.s3#TopicConfiguration"))
             .putMember("Id", Schemas2.NOTIFICATION_ID)
             .putMember("TopicArn", Schemas2.TOPIC_ARN,
                     new XmlNameTrait("Topic"),
                     new RequiredTrait())
             .putMember("Events", Schemas2.EVENT_LIST,
                     new XmlNameTrait("Event"),
                     new RequiredTrait(),
                     new XmlFlattenedTrait())
             .putMember("Filter", Schemas2.NOTIFICATION_CONFIGURATION_FILTER)
             .builderSupplier(TopicConfiguration::builder)
             .shapeClass(TopicConfiguration.class)
             .build();

    static final Schema TOPIC_CONFIGURATION_LIST = Schema.listBuilder(ShapeId.from("com.amazonaws.s3#TopicConfigurationList"))
        .putMember("member", Schemas2.TOPIC_CONFIGURATION)
        .build();

    static final Schema GET_BUCKET_NOTIFICATION_CONFIGURATION_OUTPUT = Schema.structureBuilder(ShapeId.from("com.amazonaws.s3#NotificationConfiguration"))
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
             .builderSupplier(GetBucketNotificationConfigurationOutput::builder)
             .shapeClass(GetBucketNotificationConfigurationOutput.class)
             .build();

    static final Schema GET_BUCKET_OWNERSHIP_CONTROLS_INPUT = Schema.structureBuilder(ShapeId.from("com.amazonaws.s3#GetBucketOwnershipControlsRequest"))
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
             .builderSupplier(GetBucketOwnershipControlsInput::builder)
             .shapeClass(GetBucketOwnershipControlsInput.class)
             .build();

    static final Schema OWNERSHIP_CONTROLS_RULE = Schema.structureBuilder(ShapeId.from("com.amazonaws.s3#OwnershipControlsRule"))
             .putMember("ObjectOwnership", ObjectOwnership.$SCHEMA,
                     new RequiredTrait())
             .builderSupplier(OwnershipControlsRule::builder)
             .shapeClass(OwnershipControlsRule.class)
             .build();

    static final Schema OWNERSHIP_CONTROLS_RULES = Schema.listBuilder(ShapeId.from("com.amazonaws.s3#OwnershipControlsRules"))
        .putMember("member", Schemas2.OWNERSHIP_CONTROLS_RULE)
        .build();

    static final Schema OWNERSHIP_CONTROLS = Schema.structureBuilder(ShapeId.from("com.amazonaws.s3#OwnershipControls"))
             .putMember("Rules", Schemas2.OWNERSHIP_CONTROLS_RULES,
                     new XmlNameTrait("Rule"),
                     new RequiredTrait(),
                     new XmlFlattenedTrait())
             .builderSupplier(OwnershipControls::builder)
             .shapeClass(OwnershipControls.class)
             .build();

    static final Schema GET_BUCKET_OWNERSHIP_CONTROLS_OUTPUT = Schema.structureBuilder(ShapeId.from("com.amazonaws.s3#GetBucketOwnershipControlsOutput"))
             .putMember("OwnershipControls", Schemas2.OWNERSHIP_CONTROLS,
                     new HttpPayloadTrait())
             .builderSupplier(GetBucketOwnershipControlsOutput::builder)
             .shapeClass(GetBucketOwnershipControlsOutput.class)
             .build();

    static final Schema GET_BUCKET_POLICY_INPUT = Schema.structureBuilder(ShapeId.from("com.amazonaws.s3#GetBucketPolicyRequest"))
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
             .builderSupplier(GetBucketPolicyInput::builder)
             .shapeClass(GetBucketPolicyInput.class)
             .build();

    static final Schema POLICY = Schema.createString(ShapeId.from("com.amazonaws.s3#Policy"));
    static final Schema GET_BUCKET_POLICY_OUTPUT = Schema.structureBuilder(ShapeId.from("com.amazonaws.s3#GetBucketPolicyOutput"))
             .putMember("Policy", Schemas2.POLICY,
                     new HttpPayloadTrait())
             .builderSupplier(GetBucketPolicyOutput::builder)
             .shapeClass(GetBucketPolicyOutput.class)
             .build();

    private Schemas2() {}
}
