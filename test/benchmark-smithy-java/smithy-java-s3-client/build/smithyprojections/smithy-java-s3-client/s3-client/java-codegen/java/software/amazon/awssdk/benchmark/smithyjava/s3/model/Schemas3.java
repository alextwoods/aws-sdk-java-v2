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
import software.amazon.smithy.model.traits.StreamingTrait;
import software.amazon.smithy.model.traits.TimestampFormatTrait;
import software.amazon.smithy.model.traits.XmlFlattenedTrait;
import software.amazon.smithy.model.traits.XmlNameTrait;
import software.amazon.smithy.rulesengine.traits.ContextParamTrait;

/**
 * Defines schemas for shapes in the model package.
 */
final class Schemas3 {
    static final Schema GET_BUCKET_POLICY_STATUS_INPUT = Schema.structureBuilder(ShapeId.from("com.amazonaws.s3#GetBucketPolicyStatusRequest"))
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
             .builderSupplier(GetBucketPolicyStatusInput::builder)
             .shapeClass(GetBucketPolicyStatusInput.class)
             .build();

    static final Schema IS_PUBLIC = Schema.createBoolean(ShapeId.from("com.amazonaws.s3#IsPublic"));
    static final Schema POLICY_STATUS = Schema.structureBuilder(ShapeId.from("com.amazonaws.s3#PolicyStatus"))
             .putMember("IsPublic", Schemas3.IS_PUBLIC,
                     new XmlNameTrait("IsPublic"))
             .builderSupplier(PolicyStatus::builder)
             .shapeClass(PolicyStatus.class)
             .build();

    static final Schema GET_BUCKET_POLICY_STATUS_OUTPUT = Schema.structureBuilder(ShapeId.from("com.amazonaws.s3#GetBucketPolicyStatusOutput"))
             .putMember("PolicyStatus", Schemas3.POLICY_STATUS,
                     new HttpPayloadTrait())
             .builderSupplier(GetBucketPolicyStatusOutput::builder)
             .shapeClass(GetBucketPolicyStatusOutput.class)
             .build();

    static final Schema GET_BUCKET_REPLICATION_INPUT = Schema.structureBuilder(ShapeId.from("com.amazonaws.s3#GetBucketReplicationRequest"))
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
             .builderSupplier(GetBucketReplicationInput::builder)
             .shapeClass(GetBucketReplicationInput.class)
             .build();

    static final Schema DELETE_MARKER_REPLICATION = Schema.structureBuilder(ShapeId.from("com.amazonaws.s3#DeleteMarkerReplication"))
             .putMember("Status", DeleteMarkerReplicationStatus.$SCHEMA)
             .builderSupplier(DeleteMarkerReplication::builder)
             .shapeClass(DeleteMarkerReplication.class)
             .build();

    static final Schema REPLICA_KMS_KEY_ID = Schema.createString(ShapeId.from("com.amazonaws.s3#ReplicaKmsKeyID"));
    static final Schema ENCRYPTION_CONFIGURATION = Schema.structureBuilder(ShapeId.from("com.amazonaws.s3#EncryptionConfiguration"))
             .putMember("ReplicaKmsKeyID", Schemas3.REPLICA_KMS_KEY_ID)
             .builderSupplier(EncryptionConfiguration::builder)
             .shapeClass(EncryptionConfiguration.class)
             .build();

    static final Schema MINUTES = Schema.createInteger(ShapeId.from("com.amazonaws.s3#Minutes"));
    static final Schema REPLICATION_TIME_VALUE = Schema.structureBuilder(ShapeId.from("com.amazonaws.s3#ReplicationTimeValue"))
             .putMember("Minutes", Schemas3.MINUTES)
             .builderSupplier(ReplicationTimeValue::builder)
             .shapeClass(ReplicationTimeValue.class)
             .build();

    static final Schema METRICS = Schema.structureBuilder(ShapeId.from("com.amazonaws.s3#Metrics"))
             .putMember("Status", MetricsStatus.$SCHEMA,
                     new RequiredTrait())
             .putMember("EventThreshold", Schemas3.REPLICATION_TIME_VALUE)
             .builderSupplier(Metrics::builder)
             .shapeClass(Metrics.class)
             .build();

    static final Schema REPLICATION_TIME = Schema.structureBuilder(ShapeId.from("com.amazonaws.s3#ReplicationTime"))
             .putMember("Status", ReplicationTimeStatus.$SCHEMA,
                     new RequiredTrait())
             .putMember("Time", Schemas3.REPLICATION_TIME_VALUE,
                     new RequiredTrait())
             .builderSupplier(ReplicationTime::builder)
             .shapeClass(ReplicationTime.class)
             .build();

    static final Schema DESTINATION = Schema.structureBuilder(ShapeId.from("com.amazonaws.s3#Destination"))
             .putMember("Bucket", Schemas.BUCKET_NAME,
                     new RequiredTrait())
             .putMember("Account", Schemas.ACCOUNT_ID)
             .putMember("StorageClass", StorageClass.$SCHEMA)
             .putMember("AccessControlTranslation", Schemas.ACCESS_CONTROL_TRANSLATION)
             .putMember("EncryptionConfiguration", Schemas3.ENCRYPTION_CONFIGURATION)
             .putMember("ReplicationTime", Schemas3.REPLICATION_TIME)
             .putMember("Metrics", Schemas3.METRICS)
             .builderSupplier(Destination::builder)
             .shapeClass(Destination.class)
             .build();

    static final Schema EXISTING_OBJECT_REPLICATION = Schema.structureBuilder(ShapeId.from("com.amazonaws.s3#ExistingObjectReplication"))
             .putMember("Status", ExistingObjectReplicationStatus.$SCHEMA,
                     new RequiredTrait())
             .builderSupplier(ExistingObjectReplication::builder)
             .shapeClass(ExistingObjectReplication.class)
             .build();

    static final Schema REPLICATION_RULE_AND_OPERATOR = Schema.structureBuilder(ShapeId.from("com.amazonaws.s3#ReplicationRuleAndOperator"))
             .putMember("Prefix", Schemas1.PREFIX)
             .putMember("Tags", Schemas.TAG_SET,
                     new XmlNameTrait("Tag"),
                     new XmlFlattenedTrait())
             .builderSupplier(ReplicationRuleAndOperator::builder)
             .shapeClass(ReplicationRuleAndOperator.class)
             .build();

    static final Schema REPLICATION_RULE_FILTER = Schema.structureBuilder(ShapeId.from("com.amazonaws.s3#ReplicationRuleFilter"))
             .putMember("Prefix", Schemas1.PREFIX)
             .putMember("Tag", Schemas.TAG)
             .putMember("And", Schemas3.REPLICATION_RULE_AND_OPERATOR)
             .builderSupplier(ReplicationRuleFilter::builder)
             .shapeClass(ReplicationRuleFilter.class)
             .build();

    static final Schema PRIORITY = Schema.createInteger(ShapeId.from("com.amazonaws.s3#Priority"));
    static final Schema REPLICA_MODIFICATIONS = Schema.structureBuilder(ShapeId.from("com.amazonaws.s3#ReplicaModifications"))
             .putMember("Status", ReplicaModificationsStatus.$SCHEMA,
                     new RequiredTrait())
             .builderSupplier(ReplicaModifications::builder)
             .shapeClass(ReplicaModifications.class)
             .build();

    static final Schema SSE_KMS_ENCRYPTED_OBJECTS = Schema.structureBuilder(ShapeId.from("com.amazonaws.s3#SseKmsEncryptedObjects"))
             .putMember("Status", SseKmsEncryptedObjectsStatus.$SCHEMA,
                     new RequiredTrait())
             .builderSupplier(SseKmsEncryptedObjects::builder)
             .shapeClass(SseKmsEncryptedObjects.class)
             .build();

    static final Schema SOURCE_SELECTION_CRITERIA = Schema.structureBuilder(ShapeId.from("com.amazonaws.s3#SourceSelectionCriteria"))
             .putMember("SseKmsEncryptedObjects", Schemas3.SSE_KMS_ENCRYPTED_OBJECTS)
             .putMember("ReplicaModifications", Schemas3.REPLICA_MODIFICATIONS)
             .builderSupplier(SourceSelectionCriteria::builder)
             .shapeClass(SourceSelectionCriteria.class)
             .build();

    static final Schema REPLICATION_RULE = Schema.structureBuilder(ShapeId.from("com.amazonaws.s3#ReplicationRule"))
             .putMember("ID", Schemas.COM_AMAZONAWS_S3_ID)
             .putMember("Priority", Schemas3.PRIORITY)
             .putMember("Prefix", Schemas1.PREFIX)
             .putMember("Filter", Schemas3.REPLICATION_RULE_FILTER)
             .putMember("Status", ReplicationRuleStatus.$SCHEMA,
                     new RequiredTrait())
             .putMember("SourceSelectionCriteria", Schemas3.SOURCE_SELECTION_CRITERIA)
             .putMember("ExistingObjectReplication", Schemas3.EXISTING_OBJECT_REPLICATION)
             .putMember("Destination", Schemas3.DESTINATION,
                     new RequiredTrait())
             .putMember("DeleteMarkerReplication", Schemas3.DELETE_MARKER_REPLICATION)
             .builderSupplier(ReplicationRule::builder)
             .shapeClass(ReplicationRule.class)
             .build();

    static final Schema REPLICATION_RULES = Schema.listBuilder(ShapeId.from("com.amazonaws.s3#ReplicationRules"))
        .putMember("member", Schemas3.REPLICATION_RULE)
        .build();

    static final Schema REPLICATION_CONFIGURATION = Schema.structureBuilder(ShapeId.from("com.amazonaws.s3#ReplicationConfiguration"))
             .putMember("Role", Schemas.ROLE,
                     new RequiredTrait())
             .putMember("Rules", Schemas3.REPLICATION_RULES,
                     new XmlNameTrait("Rule"),
                     new RequiredTrait(),
                     new XmlFlattenedTrait())
             .builderSupplier(ReplicationConfiguration::builder)
             .shapeClass(ReplicationConfiguration.class)
             .build();

    static final Schema GET_BUCKET_REPLICATION_OUTPUT = Schema.structureBuilder(ShapeId.from("com.amazonaws.s3#GetBucketReplicationOutput"))
             .putMember("ReplicationConfiguration", Schemas3.REPLICATION_CONFIGURATION,
                     new HttpPayloadTrait())
             .builderSupplier(GetBucketReplicationOutput::builder)
             .shapeClass(GetBucketReplicationOutput.class)
             .build();

    static final Schema GET_BUCKET_REQUEST_PAYMENT_INPUT = Schema.structureBuilder(ShapeId.from("com.amazonaws.s3#GetBucketRequestPaymentRequest"))
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
             .builderSupplier(GetBucketRequestPaymentInput::builder)
             .shapeClass(GetBucketRequestPaymentInput.class)
             .build();

    static final Schema GET_BUCKET_REQUEST_PAYMENT_OUTPUT = Schema.structureBuilder(ShapeId.from("com.amazonaws.s3#GetBucketRequestPaymentOutput"),
            new XmlNameTrait("RequestPaymentConfiguration"))
             .putMember("Payer", Payer.$SCHEMA)
             .builderSupplier(GetBucketRequestPaymentOutput::builder)
             .shapeClass(GetBucketRequestPaymentOutput.class)
             .build();

    static final Schema GET_BUCKET_TAGGING_INPUT = Schema.structureBuilder(ShapeId.from("com.amazonaws.s3#GetBucketTaggingRequest"))
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
             .builderSupplier(GetBucketTaggingInput::builder)
             .shapeClass(GetBucketTaggingInput.class)
             .build();

    static final Schema GET_BUCKET_TAGGING_OUTPUT = Schema.structureBuilder(ShapeId.from("com.amazonaws.s3#GetBucketTaggingOutput"),
            new XmlNameTrait("Tagging"))
             .putMember("TagSet", Schemas.TAG_SET,
                     new RequiredTrait())
             .builderSupplier(GetBucketTaggingOutput::builder)
             .shapeClass(GetBucketTaggingOutput.class)
             .build();

    static final Schema GET_BUCKET_VERSIONING_INPUT = Schema.structureBuilder(ShapeId.from("com.amazonaws.s3#GetBucketVersioningRequest"))
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
             .builderSupplier(GetBucketVersioningInput::builder)
             .shapeClass(GetBucketVersioningInput.class)
             .build();

    static final Schema GET_BUCKET_VERSIONING_OUTPUT = Schema.structureBuilder(ShapeId.from("com.amazonaws.s3#GetBucketVersioningOutput"),
            new XmlNameTrait("VersioningConfiguration"))
             .putMember("Status", BucketVersioningStatus.$SCHEMA)
             .putMember("MFADelete", MFADeleteStatus.$SCHEMA,
                     new XmlNameTrait("MfaDelete"))
             .builderSupplier(GetBucketVersioningOutput::builder)
             .shapeClass(GetBucketVersioningOutput.class)
             .build();

    static final Schema GET_BUCKET_WEBSITE_INPUT = Schema.structureBuilder(ShapeId.from("com.amazonaws.s3#GetBucketWebsiteRequest"))
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
             .builderSupplier(GetBucketWebsiteInput::builder)
             .shapeClass(GetBucketWebsiteInput.class)
             .build();

    static final Schema ERROR_DOCUMENT = Schema.structureBuilder(ShapeId.from("com.amazonaws.s3#ErrorDocument"))
             .putMember("Key", Schemas.OBJECT_KEY,
                     new RequiredTrait())
             .builderSupplier(ErrorDocument::builder)
             .shapeClass(ErrorDocument.class)
             .build();

    static final Schema SUFFIX = Schema.createString(ShapeId.from("com.amazonaws.s3#Suffix"));
    static final Schema INDEX_DOCUMENT = Schema.structureBuilder(ShapeId.from("com.amazonaws.s3#IndexDocument"))
             .putMember("Suffix", Schemas3.SUFFIX,
                     new RequiredTrait())
             .builderSupplier(IndexDocument::builder)
             .shapeClass(IndexDocument.class)
             .build();

    static final Schema HOST_NAME = Schema.createString(ShapeId.from("com.amazonaws.s3#HostName"));
    static final Schema REDIRECT_ALL_REQUESTS_TO = Schema.structureBuilder(ShapeId.from("com.amazonaws.s3#RedirectAllRequestsTo"))
             .putMember("HostName", Schemas3.HOST_NAME,
                     new RequiredTrait())
             .putMember("Protocol", Protocol.$SCHEMA)
             .builderSupplier(RedirectAllRequestsTo::builder)
             .shapeClass(RedirectAllRequestsTo.class)
             .build();

    static final Schema HTTP_ERROR_CODE_RETURNED_EQUALS = Schema.createString(ShapeId.from("com.amazonaws.s3#HttpErrorCodeReturnedEquals"));
    static final Schema KEY_PREFIX_EQUALS = Schema.createString(ShapeId.from("com.amazonaws.s3#KeyPrefixEquals"));
    static final Schema CONDITION = Schema.structureBuilder(ShapeId.from("com.amazonaws.s3#Condition"))
             .putMember("HttpErrorCodeReturnedEquals", Schemas3.HTTP_ERROR_CODE_RETURNED_EQUALS)
             .putMember("KeyPrefixEquals", Schemas3.KEY_PREFIX_EQUALS)
             .builderSupplier(Condition::builder)
             .shapeClass(Condition.class)
             .build();

    static final Schema HTTP_REDIRECT_CODE = Schema.createString(ShapeId.from("com.amazonaws.s3#HttpRedirectCode"));
    static final Schema REPLACE_KEY_PREFIX_WITH = Schema.createString(ShapeId.from("com.amazonaws.s3#ReplaceKeyPrefixWith"));
    static final Schema REPLACE_KEY_WITH = Schema.createString(ShapeId.from("com.amazonaws.s3#ReplaceKeyWith"));
    static final Schema REDIRECT = Schema.structureBuilder(ShapeId.from("com.amazonaws.s3#Redirect"))
             .putMember("HostName", Schemas3.HOST_NAME)
             .putMember("HttpRedirectCode", Schemas3.HTTP_REDIRECT_CODE)
             .putMember("Protocol", Protocol.$SCHEMA)
             .putMember("ReplaceKeyPrefixWith", Schemas3.REPLACE_KEY_PREFIX_WITH)
             .putMember("ReplaceKeyWith", Schemas3.REPLACE_KEY_WITH)
             .builderSupplier(Redirect::builder)
             .shapeClass(Redirect.class)
             .build();

    static final Schema ROUTING_RULE = Schema.structureBuilder(ShapeId.from("com.amazonaws.s3#RoutingRule"))
             .putMember("Condition", Schemas3.CONDITION)
             .putMember("Redirect", Schemas3.REDIRECT,
                     new RequiredTrait())
             .builderSupplier(RoutingRule::builder)
             .shapeClass(RoutingRule.class)
             .build();

    static final Schema ROUTING_RULES = Schema.listBuilder(ShapeId.from("com.amazonaws.s3#RoutingRules"))
        .putMember("member", Schemas3.ROUTING_RULE,
                new XmlNameTrait("RoutingRule"))
        .build();

    static final Schema GET_BUCKET_WEBSITE_OUTPUT = Schema.structureBuilder(ShapeId.from("com.amazonaws.s3#GetBucketWebsiteOutput"),
            new XmlNameTrait("WebsiteConfiguration"))
             .putMember("RedirectAllRequestsTo", Schemas3.REDIRECT_ALL_REQUESTS_TO)
             .putMember("IndexDocument", Schemas3.INDEX_DOCUMENT)
             .putMember("ErrorDocument", Schemas3.ERROR_DOCUMENT)
             .putMember("RoutingRules", Schemas3.ROUTING_RULES)
             .builderSupplier(GetBucketWebsiteOutput::builder)
             .shapeClass(GetBucketWebsiteOutput.class)
             .build();

    static final Schema IF_MODIFIED_SINCE = Schema.createTimestamp(ShapeId.from("com.amazonaws.s3#IfModifiedSince"));
    static final Schema IF_UNMODIFIED_SINCE = Schema.createTimestamp(ShapeId.from("com.amazonaws.s3#IfUnmodifiedSince"));
    static final Schema RANGE = Schema.createString(ShapeId.from("com.amazonaws.s3#Range"));
    static final Schema RESPONSE_CACHE_CONTROL = Schema.createString(ShapeId.from("com.amazonaws.s3#ResponseCacheControl"));
    static final Schema RESPONSE_CONTENT_DISPOSITION = Schema.createString(ShapeId.from("com.amazonaws.s3#ResponseContentDisposition"));
    static final Schema RESPONSE_CONTENT_ENCODING = Schema.createString(ShapeId.from("com.amazonaws.s3#ResponseContentEncoding"));
    static final Schema RESPONSE_CONTENT_LANGUAGE = Schema.createString(ShapeId.from("com.amazonaws.s3#ResponseContentLanguage"));
    static final Schema RESPONSE_CONTENT_TYPE = Schema.createString(ShapeId.from("com.amazonaws.s3#ResponseContentType"));
    static final Schema RESPONSE_EXPIRES = Schema.createTimestamp(ShapeId.from("com.amazonaws.s3#ResponseExpires"),
            new TimestampFormatTrait("http-date"));
    static final Schema GET_OBJECT_INPUT = Schema.structureBuilder(ShapeId.from("com.amazonaws.s3#GetObjectRequest"))
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
             .builderSupplier(GetObjectInput::builder)
             .shapeClass(GetObjectInput.class)
             .build();

    static final Schema STREAMING_BLOB = Schema.createBlob(ShapeId.from("com.amazonaws.s3#StreamingBlob"),
            new StreamingTrait());
    static final Schema CONTENT_LENGTH = Schema.createLong(ShapeId.from("com.amazonaws.s3#ContentLength"));
    static final Schema CONTENT_RANGE = Schema.createString(ShapeId.from("com.amazonaws.s3#ContentRange"));
    static final Schema MISSING_META = Schema.createInteger(ShapeId.from("com.amazonaws.s3#MissingMeta"));
    static final Schema PARTS_COUNT = Schema.createInteger(ShapeId.from("com.amazonaws.s3#PartsCount"));
    static final Schema RESTORE = Schema.createString(ShapeId.from("com.amazonaws.s3#Restore"));
    static final Schema TAG_COUNT = Schema.createInteger(ShapeId.from("com.amazonaws.s3#TagCount"));
    static final Schema GET_OBJECT_OUTPUT = Schema.structureBuilder(ShapeId.from("com.amazonaws.s3#GetObjectOutput"))
             .putMember("Body", Schemas3.STREAMING_BLOB,
                     new DefaultTrait(Node.from("")),
                     new HttpPayloadTrait())
             .putMember("DeleteMarker", Schemas1.DELETE_MARKER,
                     new HttpHeaderTrait("x-amz-delete-marker"))
             .putMember("AcceptRanges", Schemas.ACCEPT_RANGES,
                     new HttpHeaderTrait("accept-ranges"))
             .putMember("Expiration", Schemas.EXPIRATION,
                     new HttpHeaderTrait("x-amz-expiration"))
             .putMember("Restore", Schemas3.RESTORE,
                     new HttpHeaderTrait("x-amz-restore"))
             .putMember("LastModified", Schemas.LAST_MODIFIED,
                     new HttpHeaderTrait("Last-Modified"))
             .putMember("ContentLength", Schemas3.CONTENT_LENGTH,
                     new HttpHeaderTrait("Content-Length"))
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
             .putMember("ContentRange", Schemas3.CONTENT_RANGE,
                     new HttpHeaderTrait("Content-Range"))
             .putMember("ContentType", Schemas.CONTENT_TYPE,
                     new HttpHeaderTrait("Content-Type"))
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
             .builderSupplier(GetObjectOutput::builder)
             .shapeClass(GetObjectOutput.class)
             .build();

    static final Schema INVALID_OBJECT_STATE = Schema.structureBuilder(ShapeId.from("com.amazonaws.s3#InvalidObjectState"),
            new ErrorTrait("client"),
            new HttpErrorTrait(403))
             .putMember("StorageClass", StorageClass.$SCHEMA)
             .putMember("AccessTier", IntelligentTieringAccessTier.$SCHEMA)
             .builderSupplier(InvalidObjectState::builder)
             .shapeClass(InvalidObjectState.class)
             .build();

    static final Schema GET_OBJECT_ACL_INPUT = Schema.structureBuilder(ShapeId.from("com.amazonaws.s3#GetObjectAclRequest"))
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
             .putMember("RequestPayer", RequestPayer.$SCHEMA,
                     new HttpHeaderTrait("x-amz-request-payer"))
             .putMember("ExpectedBucketOwner", Schemas.ACCOUNT_ID,
                     new HttpHeaderTrait("x-amz-expected-bucket-owner"))
             .builderSupplier(GetObjectAclInput::builder)
             .shapeClass(GetObjectAclInput.class)
             .build();

    static final Schema GET_OBJECT_ACL_OUTPUT = Schema.structureBuilder(ShapeId.from("com.amazonaws.s3#GetObjectAclOutput"),
            new XmlNameTrait("AccessControlPolicy"))
             .putMember("Owner", Schemas.OWNER)
             .putMember("Grants", Schemas.GRANTS,
                     new XmlNameTrait("AccessControlList"))
             .putMember("RequestCharged", RequestCharged.$SCHEMA,
                     new HttpHeaderTrait("x-amz-request-charged"))
             .builderSupplier(GetObjectAclOutput::builder)
             .shapeClass(GetObjectAclOutput.class)
             .build();

    static final Schema GET_OBJECT_ANNOTATION_INPUT = Schema.structureBuilder(ShapeId.from("com.amazonaws.s3#GetObjectAnnotationRequest"))
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
             .putMember("AnnotationName", Schemas1.ANNOTATION_NAME,
                     new RequiredTrait(),
                     new HttpQueryTrait("annotationName"))
             .putMember("VersionId", Schemas.OBJECT_VERSION_ID,
                     new HttpQueryTrait("versionId"))
             .putMember("RequestPayer", RequestPayer.$SCHEMA,
                     new HttpHeaderTrait("x-amz-request-payer"))
             .putMember("ExpectedBucketOwner", Schemas.ACCOUNT_ID,
                     new HttpHeaderTrait("x-amz-expected-bucket-owner"))
             .putMember("ChecksumMode", ChecksumMode.$SCHEMA,
                     new HttpHeaderTrait("x-amz-checksum-mode"))
             .builderSupplier(GetObjectAnnotationInput::builder)
             .shapeClass(GetObjectAnnotationInput.class)
             .build();

    static final Schema GET_OBJECT_ANNOTATION_OUTPUT = Schema.structureBuilder(ShapeId.from("com.amazonaws.s3#GetObjectAnnotationOutput"))
             .putMember("AnnotationPayload", Schemas3.STREAMING_BLOB,
                     new DefaultTrait(Node.from("")),
                     new HttpPayloadTrait())
             .putMember("ObjectVersionId", Schemas.OBJECT_VERSION_ID,
                     new HttpHeaderTrait("x-amz-object-version-id"))
             .putMember("LastModified", Schemas.LAST_MODIFIED,
                     new HttpHeaderTrait("Last-Modified"))
             .putMember("ContentLength", Schemas3.CONTENT_LENGTH,
                     new HttpHeaderTrait("Content-Length"))
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
             .putMember("ReplicationStatus", ReplicationStatus.$SCHEMA,
                     new HttpHeaderTrait("x-amz-replication-status"))
             .builderSupplier(GetObjectAnnotationOutput::builder)
             .shapeClass(GetObjectAnnotationOutput.class)
             .build();

    static final Schema NO_SUCH_ANNOTATION = Schema.structureBuilder(ShapeId.from("com.amazonaws.s3#NoSuchAnnotation"),
            new ErrorTrait("client"),
            new HttpErrorTrait(404)).builderSupplier(NoSuchAnnotation::builder)
             .shapeClass(NoSuchAnnotation.class)
             .build();

    static final Schema MAX_PARTS = Schema.createInteger(ShapeId.from("com.amazonaws.s3#MaxParts"));
    static final Schema OBJECT_ATTRIBUTES_LIST = Schema.listBuilder(ShapeId.from("com.amazonaws.s3#ObjectAttributesList"))
        .putMember("member", ObjectAttributes.$SCHEMA)
        .build();

    static final Schema PART_NUMBER_MARKER = Schema.createString(ShapeId.from("com.amazonaws.s3#PartNumberMarker"));
    static final Schema GET_OBJECT_ATTRIBUTES_INPUT = Schema.structureBuilder(ShapeId.from("com.amazonaws.s3#GetObjectAttributesRequest"))
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
             .putMember("MaxParts", Schemas3.MAX_PARTS,
                     new HttpHeaderTrait("x-amz-max-parts"))
             .putMember("PartNumberMarker", Schemas3.PART_NUMBER_MARKER,
                     new HttpHeaderTrait("x-amz-part-number-marker"))
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
             .putMember("ObjectAttributes", Schemas3.OBJECT_ATTRIBUTES_LIST,
                     new HttpHeaderTrait("x-amz-object-attributes"),
                     new RequiredTrait())
             .builderSupplier(GetObjectAttributesInput::builder)
             .shapeClass(GetObjectAttributesInput.class)
             .build();

    static final Schema CHECKSUM = Schema.structureBuilder(ShapeId.from("com.amazonaws.s3#Checksum"))
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
             .builderSupplier(Checksum::builder)
             .shapeClass(Checksum.class)
             .build();

    static final Schema IS_TRUNCATED = Schema.createBoolean(ShapeId.from("com.amazonaws.s3#IsTruncated"));
    static final Schema NEXT_PART_NUMBER_MARKER = Schema.createString(ShapeId.from("com.amazonaws.s3#NextPartNumberMarker"));
    static final Schema OBJECT_PART = Schema.structureBuilder(ShapeId.from("com.amazonaws.s3#ObjectPart"))
             .putMember("PartNumber", Schemas.PART_NUMBER)
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
             .builderSupplier(ObjectPart::builder)
             .shapeClass(ObjectPart.class)
             .build();

    static final Schema PARTS_LIST = Schema.listBuilder(ShapeId.from("com.amazonaws.s3#PartsList"))
        .putMember("member", Schemas3.OBJECT_PART)
        .build();

    static final Schema GET_OBJECT_ATTRIBUTES_PARTS = Schema.structureBuilder(ShapeId.from("com.amazonaws.s3#GetObjectAttributesParts"))
             .putMember("TotalPartsCount", Schemas3.PARTS_COUNT,
                     new XmlNameTrait("PartsCount"))
             .putMember("PartNumberMarker", Schemas3.PART_NUMBER_MARKER)
             .putMember("NextPartNumberMarker", Schemas3.NEXT_PART_NUMBER_MARKER)
             .putMember("MaxParts", Schemas3.MAX_PARTS)
             .putMember("IsTruncated", Schemas3.IS_TRUNCATED)
             .putMember("Parts", Schemas3.PARTS_LIST,
                     new XmlNameTrait("Part"),
                     new XmlFlattenedTrait())
             .builderSupplier(GetObjectAttributesParts::builder)
             .shapeClass(GetObjectAttributesParts.class)
             .build();

    static final Schema OBJECT_SIZE = Schema.createLong(ShapeId.from("com.amazonaws.s3#ObjectSize"));
    static final Schema GET_OBJECT_ATTRIBUTES_OUTPUT = Schema.structureBuilder(ShapeId.from("com.amazonaws.s3#GetObjectAttributesOutput"),
            new XmlNameTrait("GetObjectAttributesResponse"))
             .putMember("DeleteMarker", Schemas1.DELETE_MARKER,
                     new HttpHeaderTrait("x-amz-delete-marker"))
             .putMember("LastModified", Schemas.LAST_MODIFIED,
                     new HttpHeaderTrait("Last-Modified"))
             .putMember("VersionId", Schemas.OBJECT_VERSION_ID,
                     new HttpHeaderTrait("x-amz-version-id"))
             .putMember("RequestCharged", RequestCharged.$SCHEMA,
                     new HttpHeaderTrait("x-amz-request-charged"))
             .putMember("ETag", Schemas.E_TAG)
             .putMember("Checksum", Schemas3.CHECKSUM)
             .putMember("ObjectParts", Schemas3.GET_OBJECT_ATTRIBUTES_PARTS)
             .putMember("StorageClass", StorageClass.$SCHEMA)
             .putMember("ObjectSize", Schemas3.OBJECT_SIZE)
             .builderSupplier(GetObjectAttributesOutput::builder)
             .shapeClass(GetObjectAttributesOutput.class)
             .build();

    static final Schema GET_OBJECT_LEGAL_HOLD_INPUT = Schema.structureBuilder(ShapeId.from("com.amazonaws.s3#GetObjectLegalHoldRequest"))
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
             .putMember("RequestPayer", RequestPayer.$SCHEMA,
                     new HttpHeaderTrait("x-amz-request-payer"))
             .putMember("ExpectedBucketOwner", Schemas.ACCOUNT_ID,
                     new HttpHeaderTrait("x-amz-expected-bucket-owner"))
             .builderSupplier(GetObjectLegalHoldInput::builder)
             .shapeClass(GetObjectLegalHoldInput.class)
             .build();

    static final Schema OBJECT_LOCK_LEGAL_HOLD = Schema.structureBuilder(ShapeId.from("com.amazonaws.s3#ObjectLockLegalHold"))
             .putMember("Status", ObjectLockLegalHoldStatus.$SCHEMA)
             .builderSupplier(ObjectLockLegalHold::builder)
             .shapeClass(ObjectLockLegalHold.class)
             .build();

    static final Schema GET_OBJECT_LEGAL_HOLD_OUTPUT = Schema.structureBuilder(ShapeId.from("com.amazonaws.s3#GetObjectLegalHoldOutput"))
             .putMember("LegalHold", Schemas3.OBJECT_LOCK_LEGAL_HOLD,
                     new XmlNameTrait("LegalHold"),
                     new HttpPayloadTrait())
             .builderSupplier(GetObjectLegalHoldOutput::builder)
             .shapeClass(GetObjectLegalHoldOutput.class)
             .build();

    static final Schema GET_OBJECT_LOCK_CONFIGURATION_INPUT = Schema.structureBuilder(ShapeId.from("com.amazonaws.s3#GetObjectLockConfigurationRequest"))
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
             .builderSupplier(GetObjectLockConfigurationInput::builder)
             .shapeClass(GetObjectLockConfigurationInput.class)
             .build();

    static final Schema YEARS = Schema.createInteger(ShapeId.from("com.amazonaws.s3#Years"));
    static final Schema DEFAULT_RETENTION = Schema.structureBuilder(ShapeId.from("com.amazonaws.s3#DefaultRetention"))
             .putMember("Mode", ObjectLockRetentionMode.$SCHEMA)
             .putMember("Days", Schemas2.DAYS)
             .putMember("Years", Schemas3.YEARS)
             .builderSupplier(DefaultRetention::builder)
             .shapeClass(DefaultRetention.class)
             .build();

    static final Schema OBJECT_LOCK_RULE = Schema.structureBuilder(ShapeId.from("com.amazonaws.s3#ObjectLockRule"))
             .putMember("DefaultRetention", Schemas3.DEFAULT_RETENTION)
             .builderSupplier(ObjectLockRule::builder)
             .shapeClass(ObjectLockRule.class)
             .build();

    static final Schema OBJECT_LOCK_CONFIGURATION = Schema.structureBuilder(ShapeId.from("com.amazonaws.s3#ObjectLockConfiguration"))
             .putMember("ObjectLockEnabled", ObjectLockEnabled.$SCHEMA)
             .putMember("Rule", Schemas3.OBJECT_LOCK_RULE)
             .builderSupplier(ObjectLockConfiguration::builder)
             .shapeClass(ObjectLockConfiguration.class)
             .build();

    static final Schema GET_OBJECT_LOCK_CONFIGURATION_OUTPUT = Schema.structureBuilder(ShapeId.from("com.amazonaws.s3#GetObjectLockConfigurationOutput"))
             .putMember("ObjectLockConfiguration", Schemas3.OBJECT_LOCK_CONFIGURATION,
                     new HttpPayloadTrait())
             .builderSupplier(GetObjectLockConfigurationOutput::builder)
             .shapeClass(GetObjectLockConfigurationOutput.class)
             .build();

    static final Schema GET_OBJECT_RETENTION_INPUT = Schema.structureBuilder(ShapeId.from("com.amazonaws.s3#GetObjectRetentionRequest"))
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
             .putMember("RequestPayer", RequestPayer.$SCHEMA,
                     new HttpHeaderTrait("x-amz-request-payer"))
             .putMember("ExpectedBucketOwner", Schemas.ACCOUNT_ID,
                     new HttpHeaderTrait("x-amz-expected-bucket-owner"))
             .builderSupplier(GetObjectRetentionInput::builder)
             .shapeClass(GetObjectRetentionInput.class)
             .build();

    static final Schema OBJECT_LOCK_RETENTION = Schema.structureBuilder(ShapeId.from("com.amazonaws.s3#ObjectLockRetention"))
             .putMember("Mode", ObjectLockRetentionMode.$SCHEMA)
             .putMember("RetainUntilDate", Schemas2.DATE)
             .builderSupplier(ObjectLockRetention::builder)
             .shapeClass(ObjectLockRetention.class)
             .build();

    static final Schema GET_OBJECT_RETENTION_OUTPUT = Schema.structureBuilder(ShapeId.from("com.amazonaws.s3#GetObjectRetentionOutput"))
             .putMember("Retention", Schemas3.OBJECT_LOCK_RETENTION,
                     new XmlNameTrait("Retention"),
                     new HttpPayloadTrait())
             .builderSupplier(GetObjectRetentionOutput::builder)
             .shapeClass(GetObjectRetentionOutput.class)
             .build();

    static final Schema GET_OBJECT_TAGGING_INPUT = Schema.structureBuilder(ShapeId.from("com.amazonaws.s3#GetObjectTaggingRequest"))
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
             .putMember("RequestPayer", RequestPayer.$SCHEMA,
                     new HttpHeaderTrait("x-amz-request-payer"))
             .builderSupplier(GetObjectTaggingInput::builder)
             .shapeClass(GetObjectTaggingInput.class)
             .build();

    private Schemas3() {}
}
