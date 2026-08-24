package software.amazon.awssdk.benchmark.smithyjava.dynamodb.model;

import java.math.BigDecimal;
import software.amazon.smithy.java.core.schema.Schema;
import software.amazon.smithy.java.core.schema.SchemaBuilder;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.model.traits.DefaultTrait;
import software.amazon.smithy.model.traits.ErrorTrait;
import software.amazon.smithy.model.traits.IdempotencyTokenTrait;
import software.amazon.smithy.model.traits.LengthTrait;
import software.amazon.smithy.model.traits.PatternTrait;
import software.amazon.smithy.model.traits.RangeTrait;
import software.amazon.smithy.model.traits.RequiredTrait;
import software.amazon.smithy.model.traits.RetryableTrait;
import software.amazon.smithy.model.traits.UnitTypeTrait;
import software.amazon.smithy.rulesengine.traits.ContextParamTrait;

/**
 * Defines schemas for shapes in the model package.
 */
final class Schemas {
    static final SchemaBuilder ATTRIBUTE_VALUE_BUILDER = Schema.structureBuilder(ShapeId.from("com.amazonaws.dynamodb#AttributeValue"));
    static final SchemaBuilder LIST_ATTRIBUTE_VALUE_BUILDER = Schema.listBuilder(ShapeId.from("com.amazonaws.dynamodb#ListAttributeValue"));
    static final SchemaBuilder MAP_ATTRIBUTE_VALUE_BUILDER = Schema.mapBuilder(ShapeId.from("com.amazonaws.dynamodb#MapAttributeValue"));
    static final SchemaBuilder ATTRIBUTE_MAP_BUILDER = Schema.mapBuilder(ShapeId.from("com.amazonaws.dynamodb#AttributeMap"));
    static final SchemaBuilder ATTRIBUTE_VALUE_LIST_BUILDER = Schema.listBuilder(ShapeId.from("com.amazonaws.dynamodb#AttributeValueList"));
    static final SchemaBuilder ATTRIBUTE_VALUE_UPDATE_BUILDER = Schema.structureBuilder(ShapeId.from("com.amazonaws.dynamodb#AttributeValueUpdate"));
    static final SchemaBuilder EXPRESSION_ATTRIBUTE_VALUE_MAP_BUILDER = Schema.mapBuilder(ShapeId.from("com.amazonaws.dynamodb#ExpressionAttributeValueMap"));
    static final SchemaBuilder ITEM_COLLECTION_KEY_ATTRIBUTE_MAP_BUILDER = Schema.mapBuilder(ShapeId.from("com.amazonaws.dynamodb#ItemCollectionKeyAttributeMap"));
    static final SchemaBuilder KEY_BUILDER = Schema.mapBuilder(ShapeId.from("com.amazonaws.dynamodb#Key"));
    static final SchemaBuilder PREPARED_STATEMENT_PARAMETERS_BUILDER = Schema.listBuilder(ShapeId.from("com.amazonaws.dynamodb#PreparedStatementParameters"),
            LengthTrait.builder().min(1L).build());
    static final SchemaBuilder PUT_ITEM_INPUT_ATTRIBUTE_MAP_BUILDER = Schema.mapBuilder(ShapeId.from("com.amazonaws.dynamodb#PutItemInputAttributeMap"));
    static final SchemaBuilder SEARCH_VECTOR_LIST_BUILDER = Schema.listBuilder(ShapeId.from("com.amazonaws.dynamodb#SearchVectorList"),
            LengthTrait.builder().min(1L).max(4096L).build());
    static final SchemaBuilder ATTRIBUTE_UPDATES_BUILDER = Schema.mapBuilder(ShapeId.from("com.amazonaws.dynamodb#AttributeUpdates"));
    static final SchemaBuilder BATCH_STATEMENT_ERROR_BUILDER = Schema.structureBuilder(ShapeId.from("com.amazonaws.dynamodb#BatchStatementError"));
    static final SchemaBuilder BATCH_STATEMENT_REQUEST_BUILDER = Schema.structureBuilder(ShapeId.from("com.amazonaws.dynamodb#BatchStatementRequest"));
    static final SchemaBuilder CANCELLATION_REASON_BUILDER = Schema.structureBuilder(ShapeId.from("com.amazonaws.dynamodb#CancellationReason"));
    static final SchemaBuilder CONDITION_BUILDER = Schema.structureBuilder(ShapeId.from("com.amazonaws.dynamodb#Condition"));
    static final SchemaBuilder CONDITIONAL_CHECK_FAILED_EXCEPTION_BUILDER = Schema.structureBuilder(ShapeId.from("com.amazonaws.dynamodb#ConditionalCheckFailedException"),
            new ErrorTrait("client"));
    static final SchemaBuilder DELETE_REQUEST_BUILDER = Schema.structureBuilder(ShapeId.from("com.amazonaws.dynamodb#DeleteRequest"));
    static final SchemaBuilder EXECUTE_STATEMENT_INPUT_BUILDER = Schema.structureBuilder(ShapeId.from("com.amazonaws.dynamodb#ExecuteStatementInput"));
    static final SchemaBuilder GET_BUILDER = Schema.structureBuilder(ShapeId.from("com.amazonaws.dynamodb#Get"));
    static final SchemaBuilder GET_ITEM_INPUT_BUILDER = Schema.structureBuilder(ShapeId.from("com.amazonaws.dynamodb#GetItemInput"));
    static final SchemaBuilder GET_ITEM_OUTPUT_BUILDER = Schema.structureBuilder(ShapeId.from("com.amazonaws.dynamodb#GetItemOutput"));
    static final SchemaBuilder ITEM_COLLECTION_METRICS_BUILDER = Schema.structureBuilder(ShapeId.from("com.amazonaws.dynamodb#ItemCollectionMetrics"));
    static final SchemaBuilder ITEM_LIST_BUILDER = Schema.listBuilder(ShapeId.from("com.amazonaws.dynamodb#ItemList"));
    static final SchemaBuilder ITEM_RESPONSE_BUILDER = Schema.structureBuilder(ShapeId.from("com.amazonaws.dynamodb#ItemResponse"));
    static final SchemaBuilder KEY_LIST_BUILDER = Schema.listBuilder(ShapeId.from("com.amazonaws.dynamodb#KeyList"),
            LengthTrait.builder().min(1L).max(100L).build());
    static final SchemaBuilder PARAMETERIZED_STATEMENT_BUILDER = Schema.structureBuilder(ShapeId.from("com.amazonaws.dynamodb#ParameterizedStatement"));
    static final SchemaBuilder PUT_REQUEST_BUILDER = Schema.structureBuilder(ShapeId.from("com.amazonaws.dynamodb#PutRequest"));
    static final SchemaBuilder SEARCH_RESULT_ITEM_BUILDER = Schema.structureBuilder(ShapeId.from("com.amazonaws.dynamodb#SearchResultItem"));
    static final SchemaBuilder BATCH_GET_RESPONSE_MAP_BUILDER = Schema.mapBuilder(ShapeId.from("com.amazonaws.dynamodb#BatchGetResponseMap"));
    static final SchemaBuilder CANCELLATION_REASON_LIST_BUILDER = Schema.listBuilder(ShapeId.from("com.amazonaws.dynamodb#CancellationReasonList"),
            LengthTrait.builder().min(1L).max(100L).build());
    static final SchemaBuilder FILTER_CONDITION_MAP_BUILDER = Schema.mapBuilder(ShapeId.from("com.amazonaws.dynamodb#FilterConditionMap"));
    static final SchemaBuilder ITEM_COLLECTION_METRICS_MULTIPLE_BUILDER = Schema.listBuilder(ShapeId.from("com.amazonaws.dynamodb#ItemCollectionMetricsMultiple"));
    static final SchemaBuilder ITEM_RESPONSE_LIST_BUILDER = Schema.listBuilder(ShapeId.from("com.amazonaws.dynamodb#ItemResponseList"),
            LengthTrait.builder().min(1L).max(100L).build());
    static final SchemaBuilder KEY_CONDITIONS_BUILDER = Schema.mapBuilder(ShapeId.from("com.amazonaws.dynamodb#KeyConditions"));
    static final SchemaBuilder KEYS_AND_ATTRIBUTES_BUILDER = Schema.structureBuilder(ShapeId.from("com.amazonaws.dynamodb#KeysAndAttributes"));
    static final SchemaBuilder PARAMETERIZED_STATEMENTS_BUILDER = Schema.listBuilder(ShapeId.from("com.amazonaws.dynamodb#ParameterizedStatements"),
            LengthTrait.builder().min(1L).max(100L).build());
    static final SchemaBuilder PARTI_QL_BATCH_REQUEST_BUILDER = Schema.listBuilder(ShapeId.from("com.amazonaws.dynamodb#PartiQLBatchRequest"),
            LengthTrait.builder().min(1L).max(25L).build());
    static final SchemaBuilder SEARCH_RESULT_LIST_BUILDER = Schema.listBuilder(ShapeId.from("com.amazonaws.dynamodb#SearchResultList"));
    static final SchemaBuilder TRANSACT_GET_ITEM_BUILDER = Schema.structureBuilder(ShapeId.from("com.amazonaws.dynamodb#TransactGetItem"));
    static final SchemaBuilder BATCH_EXECUTE_STATEMENT_INPUT_BUILDER = Schema.structureBuilder(ShapeId.from("com.amazonaws.dynamodb#BatchExecuteStatementInput"));
    static final SchemaBuilder BATCH_GET_REQUEST_MAP_BUILDER = Schema.mapBuilder(ShapeId.from("com.amazonaws.dynamodb#BatchGetRequestMap"),
            LengthTrait.builder().min(1L).max(100L).build());
    static final SchemaBuilder EXECUTE_TRANSACTION_INPUT_BUILDER = Schema.structureBuilder(ShapeId.from("com.amazonaws.dynamodb#ExecuteTransactionInput"));
    static final SchemaBuilder EXECUTE_TRANSACTION_OUTPUT_BUILDER = Schema.structureBuilder(ShapeId.from("com.amazonaws.dynamodb#ExecuteTransactionOutput"));
    static final SchemaBuilder ITEM_COLLECTION_METRICS_PER_TABLE_BUILDER = Schema.mapBuilder(ShapeId.from("com.amazonaws.dynamodb#ItemCollectionMetricsPerTable"));
    static final SchemaBuilder SEARCH_VECTORS_OUTPUT_BUILDER = Schema.structureBuilder(ShapeId.from("com.amazonaws.dynamodb#SearchVectorsOutput"));
    static final SchemaBuilder TRANSACT_GET_ITEM_LIST_BUILDER = Schema.listBuilder(ShapeId.from("com.amazonaws.dynamodb#TransactGetItemList"),
            LengthTrait.builder().min(1L).max(100L).build());
    static final SchemaBuilder TRANSACT_GET_ITEMS_OUTPUT_BUILDER = Schema.structureBuilder(ShapeId.from("com.amazonaws.dynamodb#TransactGetItemsOutput"));
    static final SchemaBuilder TRANSACTION_CANCELED_EXCEPTION_BUILDER = Schema.structureBuilder(ShapeId.from("com.amazonaws.dynamodb#TransactionCanceledException"),
            new ErrorTrait("client"));
    static final SchemaBuilder BATCH_GET_ITEM_INPUT_BUILDER = Schema.structureBuilder(ShapeId.from("com.amazonaws.dynamodb#BatchGetItemInput"));
    static final SchemaBuilder EXPECTED_ATTRIBUTE_VALUE_BUILDER = Schema.structureBuilder(ShapeId.from("com.amazonaws.dynamodb#ExpectedAttributeValue"));
    static final SchemaBuilder TRANSACT_GET_ITEMS_INPUT_BUILDER = Schema.structureBuilder(ShapeId.from("com.amazonaws.dynamodb#TransactGetItemsInput"));
    static final SchemaBuilder TRANSACT_WRITE_ITEMS_OUTPUT_BUILDER = Schema.structureBuilder(ShapeId.from("com.amazonaws.dynamodb#TransactWriteItemsOutput"));
    static final SchemaBuilder CONDITION_CHECK_BUILDER = Schema.structureBuilder(ShapeId.from("com.amazonaws.dynamodb#ConditionCheck"));
    static final SchemaBuilder DELETE_BUILDER = Schema.structureBuilder(ShapeId.from("com.amazonaws.dynamodb#Delete"));
    static final SchemaBuilder PUT_BUILDER = Schema.structureBuilder(ShapeId.from("com.amazonaws.dynamodb#Put"));
    static final SchemaBuilder SEARCH_VECTORS_INPUT_BUILDER = Schema.structureBuilder(ShapeId.from("com.amazonaws.dynamodb#SearchVectorsInput"));
    static final SchemaBuilder UPDATE_BUILDER = Schema.structureBuilder(ShapeId.from("com.amazonaws.dynamodb#Update"));
    static final SchemaBuilder BATCH_STATEMENT_RESPONSE_BUILDER = Schema.structureBuilder(ShapeId.from("com.amazonaws.dynamodb#BatchStatementResponse"));
    static final SchemaBuilder DELETE_ITEM_OUTPUT_BUILDER = Schema.structureBuilder(ShapeId.from("com.amazonaws.dynamodb#DeleteItemOutput"));
    static final SchemaBuilder EXECUTE_STATEMENT_OUTPUT_BUILDER = Schema.structureBuilder(ShapeId.from("com.amazonaws.dynamodb#ExecuteStatementOutput"));
    static final SchemaBuilder EXPECTED_ATTRIBUTE_MAP_BUILDER = Schema.mapBuilder(ShapeId.from("com.amazonaws.dynamodb#ExpectedAttributeMap"));
    static final SchemaBuilder PUT_ITEM_OUTPUT_BUILDER = Schema.structureBuilder(ShapeId.from("com.amazonaws.dynamodb#PutItemOutput"));
    static final SchemaBuilder QUERY_OUTPUT_BUILDER = Schema.structureBuilder(ShapeId.from("com.amazonaws.dynamodb#QueryOutput"));
    static final SchemaBuilder SCAN_OUTPUT_BUILDER = Schema.structureBuilder(ShapeId.from("com.amazonaws.dynamodb#ScanOutput"));
    static final SchemaBuilder UPDATE_ITEM_OUTPUT_BUILDER = Schema.structureBuilder(ShapeId.from("com.amazonaws.dynamodb#UpdateItemOutput"));
    static final SchemaBuilder WRITE_REQUEST_BUILDER = Schema.structureBuilder(ShapeId.from("com.amazonaws.dynamodb#WriteRequest"));
    static final SchemaBuilder PARTI_QL_BATCH_RESPONSE_BUILDER = Schema.listBuilder(ShapeId.from("com.amazonaws.dynamodb#PartiQLBatchResponse"));
    static final SchemaBuilder WRITE_REQUESTS_BUILDER = Schema.listBuilder(ShapeId.from("com.amazonaws.dynamodb#WriteRequests"),
            LengthTrait.builder().min(1L).max(25L).build());
    static final SchemaBuilder BATCH_EXECUTE_STATEMENT_OUTPUT_BUILDER = Schema.structureBuilder(ShapeId.from("com.amazonaws.dynamodb#BatchExecuteStatementOutput"));
    static final SchemaBuilder BATCH_GET_ITEM_OUTPUT_BUILDER = Schema.structureBuilder(ShapeId.from("com.amazonaws.dynamodb#BatchGetItemOutput"));
    static final SchemaBuilder BATCH_WRITE_ITEM_REQUEST_MAP_BUILDER = Schema.mapBuilder(ShapeId.from("com.amazonaws.dynamodb#BatchWriteItemRequestMap"),
            LengthTrait.builder().min(1L).max(25L).build());
    static final SchemaBuilder SCAN_INPUT_BUILDER = Schema.structureBuilder(ShapeId.from("com.amazonaws.dynamodb#ScanInput"));
    static final SchemaBuilder BATCH_WRITE_ITEM_INPUT_BUILDER = Schema.structureBuilder(ShapeId.from("com.amazonaws.dynamodb#BatchWriteItemInput"));
    static final SchemaBuilder DELETE_ITEM_INPUT_BUILDER = Schema.structureBuilder(ShapeId.from("com.amazonaws.dynamodb#DeleteItemInput"));
    static final SchemaBuilder PUT_ITEM_INPUT_BUILDER = Schema.structureBuilder(ShapeId.from("com.amazonaws.dynamodb#PutItemInput"));
    static final SchemaBuilder QUERY_INPUT_BUILDER = Schema.structureBuilder(ShapeId.from("com.amazonaws.dynamodb#QueryInput"));
    static final SchemaBuilder BATCH_WRITE_ITEM_OUTPUT_BUILDER = Schema.structureBuilder(ShapeId.from("com.amazonaws.dynamodb#BatchWriteItemOutput"));
    static final SchemaBuilder UPDATE_ITEM_INPUT_BUILDER = Schema.structureBuilder(ShapeId.from("com.amazonaws.dynamodb#UpdateItemInput"));
    static final SchemaBuilder TRANSACT_WRITE_ITEM_BUILDER = Schema.structureBuilder(ShapeId.from("com.amazonaws.dynamodb#TransactWriteItem"));
    static final SchemaBuilder TRANSACT_WRITE_ITEM_LIST_BUILDER = Schema.listBuilder(ShapeId.from("com.amazonaws.dynamodb#TransactWriteItemList"),
            LengthTrait.builder().min(1L).max(100L).build());
    static final SchemaBuilder TRANSACT_WRITE_ITEMS_INPUT_BUILDER = Schema.structureBuilder(ShapeId.from("com.amazonaws.dynamodb#TransactWriteItemsInput"));
    static final Schema ARCHIVAL_REASON = Schema.createString(ShapeId.from("com.amazonaws.dynamodb#ArchivalReason"));
    static final Schema BACKUP_ARN = Schema.createString(ShapeId.from("com.amazonaws.dynamodb#BackupArn"),
            LengthTrait.builder().min(37L).max(1024L).build());
    static final Schema DATE = Schema.createTimestamp(ShapeId.from("com.amazonaws.dynamodb#Date"));
    static final Schema ARCHIVAL_SUMMARY = Schema.structureBuilder(ShapeId.from("com.amazonaws.dynamodb#ArchivalSummary"))
             .putMember("ArchivalDateTime", Schemas.DATE)
             .putMember("ArchivalReason", Schemas.ARCHIVAL_REASON)
             .putMember("ArchivalBackupArn", Schemas.BACKUP_ARN)
             .builderSupplier(ArchivalSummary::builder)
             .shapeClass(ArchivalSummary.class)
             .build();

    static final Schema KEY_SCHEMA_ATTRIBUTE_NAME = Schema.createString(ShapeId.from("com.amazonaws.dynamodb#KeySchemaAttributeName"),
            LengthTrait.builder().min(1L).max(255L).build());
    static final Schema ATTRIBUTE_DEFINITION = Schema.structureBuilder(ShapeId.from("com.amazonaws.dynamodb#AttributeDefinition"))
             .putMember("AttributeName", Schemas.KEY_SCHEMA_ATTRIBUTE_NAME,
                     new RequiredTrait())
             .putMember("AttributeType", ScalarAttributeType.$SCHEMA,
                     new RequiredTrait())
             .builderSupplier(AttributeDefinition::builder)
             .shapeClass(AttributeDefinition.class)
             .build();

    static final Schema ATTRIBUTE_DEFINITIONS = Schema.listBuilder(ShapeId.from("com.amazonaws.dynamodb#AttributeDefinitions"))
        .putMember("member", Schemas.ATTRIBUTE_DEFINITION)
        .build();

    static final Schema ATTRIBUTE_NAME = Schema.createString(ShapeId.from("com.amazonaws.dynamodb#AttributeName"),
            LengthTrait.builder().min(0L).max(65535L).build());
    static final Schema BINARY_ATTRIBUTE_VALUE = Schema.createBlob(ShapeId.from("com.amazonaws.dynamodb#BinaryAttributeValue"));
    static final Schema BOOLEAN_ATTRIBUTE_VALUE = Schema.createBoolean(ShapeId.from("com.amazonaws.dynamodb#BooleanAttributeValue"));
    static final Schema BINARY_SET_ATTRIBUTE_VALUE = Schema.listBuilder(ShapeId.from("com.amazonaws.dynamodb#BinarySetAttributeValue"))
        .putMember("member", Schemas.BINARY_ATTRIBUTE_VALUE)
        .build();

    static final Schema NUMBER_ATTRIBUTE_VALUE = Schema.createString(ShapeId.from("com.amazonaws.dynamodb#NumberAttributeValue"));
    static final Schema NUMBER_SET_ATTRIBUTE_VALUE = Schema.listBuilder(ShapeId.from("com.amazonaws.dynamodb#NumberSetAttributeValue"))
        .putMember("member", Schemas.NUMBER_ATTRIBUTE_VALUE)
        .build();

    static final Schema NULL_ATTRIBUTE_VALUE = Schema.createBoolean(ShapeId.from("com.amazonaws.dynamodb#NullAttributeValue"));
    static final Schema STRING_ATTRIBUTE_VALUE = Schema.createString(ShapeId.from("com.amazonaws.dynamodb#StringAttributeValue"));
    static final Schema STRING_SET_ATTRIBUTE_VALUE = Schema.listBuilder(ShapeId.from("com.amazonaws.dynamodb#StringSetAttributeValue"))
        .putMember("member", Schemas.STRING_ATTRIBUTE_VALUE)
        .build();

    static final Schema ATTRIBUTE_NAME_LIST = Schema.listBuilder(ShapeId.from("com.amazonaws.dynamodb#AttributeNameList"),
            LengthTrait.builder().min(1L).build())
        .putMember("member", Schemas.ATTRIBUTE_NAME)
        .build();

    static final Schema AUTO_SCALING_POLICY_NAME = Schema.createString(ShapeId.from("com.amazonaws.dynamodb#AutoScalingPolicyName"),
            LengthTrait.builder().min(1L).max(256L).build(),
            new PatternTrait("^\\p{Print}+$"));
    static final Schema BOOLEAN_OBJECT = Schema.createBoolean(ShapeId.from("com.amazonaws.dynamodb#BooleanObject"));
    static final Schema INTEGER_OBJECT = Schema.createInteger(ShapeId.from("com.amazonaws.dynamodb#IntegerObject"));
    static final Schema DOUBLE_OBJECT = Schema.createDouble(ShapeId.from("com.amazonaws.dynamodb#DoubleObject"));
    static final Schema AUTO_SCALING_TARGET_TRACKING_SCALING_POLICY_CONFIGURATION_DESCRIPTION = Schema.structureBuilder(ShapeId.from("com.amazonaws.dynamodb#AutoScalingTargetTrackingScalingPolicyConfigurationDescription"))
             .putMember("DisableScaleIn", Schemas.BOOLEAN_OBJECT)
             .putMember("ScaleInCooldown", Schemas.INTEGER_OBJECT)
             .putMember("ScaleOutCooldown", Schemas.INTEGER_OBJECT)
             .putMember("TargetValue", Schemas.DOUBLE_OBJECT,
                     new RequiredTrait())
             .builderSupplier(AutoScalingTargetTrackingScalingPolicyConfigurationDescription::builder)
             .shapeClass(AutoScalingTargetTrackingScalingPolicyConfigurationDescription.class)
             .build();

    static final Schema AUTO_SCALING_POLICY_DESCRIPTION = Schema.structureBuilder(ShapeId.from("com.amazonaws.dynamodb#AutoScalingPolicyDescription"))
             .putMember("PolicyName", Schemas.AUTO_SCALING_POLICY_NAME)
             .putMember("TargetTrackingScalingPolicyConfiguration", Schemas.AUTO_SCALING_TARGET_TRACKING_SCALING_POLICY_CONFIGURATION_DESCRIPTION)
             .builderSupplier(AutoScalingPolicyDescription::builder)
             .shapeClass(AutoScalingPolicyDescription.class)
             .build();

    static final Schema AUTO_SCALING_POLICY_DESCRIPTION_LIST = Schema.listBuilder(ShapeId.from("com.amazonaws.dynamodb#AutoScalingPolicyDescriptionList"))
        .putMember("member", Schemas.AUTO_SCALING_POLICY_DESCRIPTION)
        .build();

    static final Schema AUTO_SCALING_TARGET_TRACKING_SCALING_POLICY_CONFIGURATION_UPDATE = Schema.structureBuilder(ShapeId.from("com.amazonaws.dynamodb#AutoScalingTargetTrackingScalingPolicyConfigurationUpdate"))
             .putMember("DisableScaleIn", Schemas.BOOLEAN_OBJECT)
             .putMember("ScaleInCooldown", Schemas.INTEGER_OBJECT)
             .putMember("ScaleOutCooldown", Schemas.INTEGER_OBJECT)
             .putMember("TargetValue", Schemas.DOUBLE_OBJECT,
                     new RequiredTrait())
             .builderSupplier(AutoScalingTargetTrackingScalingPolicyConfigurationUpdate::builder)
             .shapeClass(AutoScalingTargetTrackingScalingPolicyConfigurationUpdate.class)
             .build();

    static final Schema AUTO_SCALING_POLICY_UPDATE = Schema.structureBuilder(ShapeId.from("com.amazonaws.dynamodb#AutoScalingPolicyUpdate"))
             .putMember("PolicyName", Schemas.AUTO_SCALING_POLICY_NAME)
             .putMember("TargetTrackingScalingPolicyConfiguration", Schemas.AUTO_SCALING_TARGET_TRACKING_SCALING_POLICY_CONFIGURATION_UPDATE,
                     new RequiredTrait())
             .builderSupplier(AutoScalingPolicyUpdate::builder)
             .shapeClass(AutoScalingPolicyUpdate.class)
             .build();

    static final Schema AUTO_SCALING_ROLE_ARN = Schema.createString(ShapeId.from("com.amazonaws.dynamodb#AutoScalingRoleArn"),
            LengthTrait.builder().min(1L).max(1600L).build(),
            new PatternTrait("^[\\u0020-\\uD7FF\\uE000-\\uFFFD\\uD800\\uDC00-\\uDBFF\\uDFFF\\r\\n\\t]*$"));
    static final Schema STRING = Schema.createString(ShapeId.from("com.amazonaws.dynamodb#String"));
    static final Schema POSITIVE_LONG_OBJECT = Schema.createLong(ShapeId.from("com.amazonaws.dynamodb#PositiveLongObject"),
            RangeTrait.builder().min(new BigDecimal("1")).build());
    static final Schema AUTO_SCALING_SETTINGS_DESCRIPTION = Schema.structureBuilder(ShapeId.from("com.amazonaws.dynamodb#AutoScalingSettingsDescription"))
             .putMember("MinimumUnits", Schemas.POSITIVE_LONG_OBJECT)
             .putMember("MaximumUnits", Schemas.POSITIVE_LONG_OBJECT)
             .putMember("AutoScalingDisabled", Schemas.BOOLEAN_OBJECT)
             .putMember("AutoScalingRoleArn", Schemas.STRING)
             .putMember("ScalingPolicies", Schemas.AUTO_SCALING_POLICY_DESCRIPTION_LIST)
             .builderSupplier(AutoScalingSettingsDescription::builder)
             .shapeClass(AutoScalingSettingsDescription.class)
             .build();

    static final Schema AUTO_SCALING_SETTINGS_UPDATE = Schema.structureBuilder(ShapeId.from("com.amazonaws.dynamodb#AutoScalingSettingsUpdate"))
             .putMember("MinimumUnits", Schemas.POSITIVE_LONG_OBJECT)
             .putMember("MaximumUnits", Schemas.POSITIVE_LONG_OBJECT)
             .putMember("AutoScalingDisabled", Schemas.BOOLEAN_OBJECT)
             .putMember("AutoScalingRoleArn", Schemas.AUTO_SCALING_ROLE_ARN)
             .putMember("ScalingPolicyUpdate", Schemas.AUTO_SCALING_POLICY_UPDATE)
             .builderSupplier(AutoScalingSettingsUpdate::builder)
             .shapeClass(AutoScalingSettingsUpdate.class)
             .build();

    static final Schema AVAILABILITY_ERROR_MESSAGE = Schema.createString(ShapeId.from("com.amazonaws.dynamodb#AvailabilityErrorMessage"));
    static final Schema BACKFILLING = Schema.createBoolean(ShapeId.from("com.amazonaws.dynamodb#Backfilling"));
    static final Schema BACKUP_CREATION_DATE_TIME = Schema.createTimestamp(ShapeId.from("com.amazonaws.dynamodb#BackupCreationDateTime"));
    static final Schema BACKUP_NAME = Schema.createString(ShapeId.from("com.amazonaws.dynamodb#BackupName"),
            LengthTrait.builder().min(3L).max(255L).build(),
            new PatternTrait("^[a-zA-Z0-9_.-]+$"));
    static final Schema BACKUP_SIZE_BYTES = Schema.createLong(ShapeId.from("com.amazonaws.dynamodb#BackupSizeBytes"),
            RangeTrait.builder().min(new BigDecimal("0")).build());
    static final Schema BACKUP_DETAILS = Schema.structureBuilder(ShapeId.from("com.amazonaws.dynamodb#BackupDetails"))
             .putMember("BackupArn", Schemas.BACKUP_ARN,
                     new RequiredTrait())
             .putMember("BackupName", Schemas.BACKUP_NAME,
                     new RequiredTrait())
             .putMember("BackupSizeBytes", Schemas.BACKUP_SIZE_BYTES)
             .putMember("BackupStatus", BackupStatus.$SCHEMA,
                     new RequiredTrait())
             .putMember("BackupType", BackupType.$SCHEMA,
                     new RequiredTrait())
             .putMember("BackupCreationDateTime", Schemas.BACKUP_CREATION_DATE_TIME,
                     new RequiredTrait())
             .putMember("BackupExpiryDateTime", Schemas.DATE)
             .builderSupplier(BackupDetails::builder)
             .shapeClass(BackupDetails.class)
             .build();

    static final Schema ITEM_COUNT = Schema.createLong(ShapeId.from("com.amazonaws.dynamodb#ItemCount"),
            RangeTrait.builder().min(new BigDecimal("0")).build());
    static final Schema KEY_SCHEMA_ELEMENT = Schema.structureBuilder(ShapeId.from("com.amazonaws.dynamodb#KeySchemaElement"))
             .putMember("AttributeName", Schemas.KEY_SCHEMA_ATTRIBUTE_NAME,
                     new RequiredTrait())
             .putMember("KeyType", KeyType.$SCHEMA,
                     new RequiredTrait())
             .builderSupplier(KeySchemaElement::builder)
             .shapeClass(KeySchemaElement.class)
             .build();

    static final Schema KEY_SCHEMA = Schema.listBuilder(ShapeId.from("com.amazonaws.dynamodb#KeySchema"),
            LengthTrait.builder().min(1L).build())
        .putMember("member", Schemas.KEY_SCHEMA_ELEMENT)
        .build();

    static final Schema LONG_OBJECT = Schema.createLong(ShapeId.from("com.amazonaws.dynamodb#LongObject"));
    static final Schema ON_DEMAND_THROUGHPUT = Schema.structureBuilder(ShapeId.from("com.amazonaws.dynamodb#OnDemandThroughput"))
             .putMember("MaxReadRequestUnits", Schemas.LONG_OBJECT)
             .putMember("MaxWriteRequestUnits", Schemas.LONG_OBJECT)
             .builderSupplier(OnDemandThroughput::builder)
             .shapeClass(OnDemandThroughput.class)
             .build();

    static final Schema PROVISIONED_THROUGHPUT = Schema.structureBuilder(ShapeId.from("com.amazonaws.dynamodb#ProvisionedThroughput"))
             .putMember("ReadCapacityUnits", Schemas.POSITIVE_LONG_OBJECT,
                     new RequiredTrait())
             .putMember("WriteCapacityUnits", Schemas.POSITIVE_LONG_OBJECT,
                     new RequiredTrait())
             .builderSupplier(ProvisionedThroughput::builder)
             .shapeClass(ProvisionedThroughput.class)
             .build();

    static final Schema TABLE_ARN = Schema.createString(ShapeId.from("com.amazonaws.dynamodb#TableArn"),
            LengthTrait.builder().min(1L).max(1024L).build());
    static final Schema TABLE_CREATION_DATE_TIME = Schema.createTimestamp(ShapeId.from("com.amazonaws.dynamodb#TableCreationDateTime"));
    static final Schema TABLE_ID = Schema.createString(ShapeId.from("com.amazonaws.dynamodb#TableId"),
            new PatternTrait("^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$"));
    static final Schema TABLE_NAME = Schema.createString(ShapeId.from("com.amazonaws.dynamodb#TableName"),
            LengthTrait.builder().min(3L).max(255L).build(),
            new PatternTrait("^[a-zA-Z0-9_.-]+$"));
    static final Schema SOURCE_TABLE_DETAILS = Schema.structureBuilder(ShapeId.from("com.amazonaws.dynamodb#SourceTableDetails"))
             .putMember("TableName", Schemas.TABLE_NAME,
                     new RequiredTrait())
             .putMember("TableId", Schemas.TABLE_ID,
                     new RequiredTrait())
             .putMember("TableArn", Schemas.TABLE_ARN)
             .putMember("TableSizeBytes", Schemas.LONG_OBJECT)
             .putMember("KeySchema", Schemas.KEY_SCHEMA,
                     new RequiredTrait())
             .putMember("TableCreationDateTime", Schemas.TABLE_CREATION_DATE_TIME,
                     new RequiredTrait())
             .putMember("ProvisionedThroughput", Schemas.PROVISIONED_THROUGHPUT,
                     new RequiredTrait())
             .putMember("OnDemandThroughput", Schemas.ON_DEMAND_THROUGHPUT)
             .putMember("ItemCount", Schemas.ITEM_COUNT)
             .putMember("BillingMode", BillingMode.$SCHEMA)
             .builderSupplier(SourceTableDetails::builder)
             .shapeClass(SourceTableDetails.class)
             .build();

    static final Schema INDEX_NAME = Schema.createString(ShapeId.from("com.amazonaws.dynamodb#IndexName"),
            LengthTrait.builder().min(3L).max(255L).build(),
            new PatternTrait("^[a-zA-Z0-9_.-]+$"));
    static final Schema NON_KEY_ATTRIBUTE_NAME = Schema.createString(ShapeId.from("com.amazonaws.dynamodb#NonKeyAttributeName"),
            LengthTrait.builder().min(1L).max(255L).build());
    static final Schema NON_KEY_ATTRIBUTE_NAME_LIST = Schema.listBuilder(ShapeId.from("com.amazonaws.dynamodb#NonKeyAttributeNameList"),
            LengthTrait.builder().min(1L).max(20L).build())
        .putMember("member", Schemas.NON_KEY_ATTRIBUTE_NAME)
        .build();

    static final Schema PROJECTION = Schema.structureBuilder(ShapeId.from("com.amazonaws.dynamodb#Projection"))
             .putMember("ProjectionType", ProjectionType.$SCHEMA)
             .putMember("NonKeyAttributes", Schemas.NON_KEY_ATTRIBUTE_NAME_LIST)
             .builderSupplier(Projection::builder)
             .shapeClass(Projection.class)
             .build();

    static final Schema GLOBAL_SECONDARY_INDEX_INFO = Schema.structureBuilder(ShapeId.from("com.amazonaws.dynamodb#GlobalSecondaryIndexInfo"))
             .putMember("IndexName", Schemas.INDEX_NAME)
             .putMember("KeySchema", Schemas.KEY_SCHEMA)
             .putMember("Projection", Schemas.PROJECTION)
             .putMember("ProvisionedThroughput", Schemas.PROVISIONED_THROUGHPUT)
             .putMember("OnDemandThroughput", Schemas.ON_DEMAND_THROUGHPUT)
             .builderSupplier(GlobalSecondaryIndexInfo::builder)
             .shapeClass(GlobalSecondaryIndexInfo.class)
             .build();

    static final Schema GLOBAL_SECONDARY_INDEXES = Schema.listBuilder(ShapeId.from("com.amazonaws.dynamodb#GlobalSecondaryIndexes"))
        .putMember("member", Schemas.GLOBAL_SECONDARY_INDEX_INFO)
        .build();

    static final Schema LOCAL_SECONDARY_INDEX_INFO = Schema.structureBuilder(ShapeId.from("com.amazonaws.dynamodb#LocalSecondaryIndexInfo"))
             .putMember("IndexName", Schemas.INDEX_NAME)
             .putMember("KeySchema", Schemas.KEY_SCHEMA)
             .putMember("Projection", Schemas.PROJECTION)
             .builderSupplier(LocalSecondaryIndexInfo::builder)
             .shapeClass(LocalSecondaryIndexInfo.class)
             .build();

    static final Schema LOCAL_SECONDARY_INDEXES = Schema.listBuilder(ShapeId.from("com.amazonaws.dynamodb#LocalSecondaryIndexes"))
        .putMember("member", Schemas.LOCAL_SECONDARY_INDEX_INFO)
        .build();

    static final Schema KMS_MASTER_KEY_ARN = Schema.createString(ShapeId.from("com.amazonaws.dynamodb#KMSMasterKeyArn"));
    static final Schema SSE_DESCRIPTION = Schema.structureBuilder(ShapeId.from("com.amazonaws.dynamodb#SSEDescription"))
             .putMember("Status", SSEStatus.$SCHEMA)
             .putMember("SSEType", SSEType.$SCHEMA)
             .putMember("KMSMasterKeyArn", Schemas.KMS_MASTER_KEY_ARN)
             .putMember("InaccessibleEncryptionDateTime", Schemas.DATE)
             .builderSupplier(SSEDescription::builder)
             .shapeClass(SSEDescription.class)
             .build();

    static final Schema STREAM_ENABLED = Schema.createBoolean(ShapeId.from("com.amazonaws.dynamodb#StreamEnabled"));
    static final Schema STREAM_SPECIFICATION = Schema.structureBuilder(ShapeId.from("com.amazonaws.dynamodb#StreamSpecification"))
             .putMember("StreamEnabled", Schemas.STREAM_ENABLED,
                     new RequiredTrait())
             .putMember("StreamViewType", StreamViewType.$SCHEMA)
             .builderSupplier(StreamSpecification::builder)
             .shapeClass(StreamSpecification.class)
             .build();

    static final Schema TIME_TO_LIVE_ATTRIBUTE_NAME = Schema.createString(ShapeId.from("com.amazonaws.dynamodb#TimeToLiveAttributeName"),
            LengthTrait.builder().min(1L).max(255L).build());
    static final Schema TIME_TO_LIVE_DESCRIPTION = Schema.structureBuilder(ShapeId.from("com.amazonaws.dynamodb#TimeToLiveDescription"))
             .putMember("TimeToLiveStatus", TimeToLiveStatus.$SCHEMA)
             .putMember("AttributeName", Schemas.TIME_TO_LIVE_ATTRIBUTE_NAME)
             .builderSupplier(TimeToLiveDescription::builder)
             .shapeClass(TimeToLiveDescription.class)
             .build();

    static final Schema SEARCH_SCHEMA_ELEMENT = Schema.structureBuilder(ShapeId.from("com.amazonaws.dynamodb#SearchSchemaElement"))
             .putMember("AttributeName", Schemas.ATTRIBUTE_NAME,
                     new RequiredTrait())
             .putMember("SearchSchemaElementType", SearchSchemaElementType.$SCHEMA,
                     new RequiredTrait())
             .builderSupplier(SearchSchemaElement::builder)
             .shapeClass(SearchSchemaElement.class)
             .build();

    static final Schema SEARCH_SCHEMA = Schema.listBuilder(ShapeId.from("com.amazonaws.dynamodb#SearchSchema"),
            LengthTrait.builder().min(1L).build())
        .putMember("member", Schemas.SEARCH_SCHEMA_ELEMENT)
        .build();

    static final Schema VECTOR_ATTRIBUTE_NAME = Schema.createString(ShapeId.from("com.amazonaws.dynamodb#VectorAttributeName"),
            LengthTrait.builder().min(1L).max(255L).build());
    static final Schema VECTOR_ATTRIBUTE_DEFINITION = Schema.structureBuilder(ShapeId.from("com.amazonaws.dynamodb#VectorAttributeDefinition"))
             .putMember("AttributeName", Schemas.VECTOR_ATTRIBUTE_NAME,
                     new RequiredTrait())
             .builderSupplier(VectorAttributeDefinition::builder)
             .shapeClass(VectorAttributeDefinition.class)
             .build();

    static final Schema VECTOR_INDEX_INFO = Schema.structureBuilder(ShapeId.from("com.amazonaws.dynamodb#VectorIndexInfo"))
             .putMember("IndexName", Schemas.INDEX_NAME)
             .putMember("VectorAttribute", Schemas.VECTOR_ATTRIBUTE_DEFINITION)
             .putMember("SearchSchema", Schemas.SEARCH_SCHEMA)
             .putMember("Projection", Schemas.PROJECTION)
             .putMember("Dimensions", Schemas.POSITIVE_LONG_OBJECT)
             .putMember("DistanceFunction", VectorDistanceFunction.$SCHEMA)
             .builderSupplier(VectorIndexInfo::builder)
             .shapeClass(VectorIndexInfo.class)
             .build();

    static final Schema VECTOR_INDEXES = Schema.listBuilder(ShapeId.from("com.amazonaws.dynamodb#VectorIndexes"))
        .putMember("member", Schemas.VECTOR_INDEX_INFO)
        .build();

    static final Schema SOURCE_TABLE_FEATURE_DETAILS = Schema.structureBuilder(ShapeId.from("com.amazonaws.dynamodb#SourceTableFeatureDetails"))
             .putMember("LocalSecondaryIndexes", Schemas.LOCAL_SECONDARY_INDEXES)
             .putMember("GlobalSecondaryIndexes", Schemas.GLOBAL_SECONDARY_INDEXES)
             .putMember("StreamDescription", Schemas.STREAM_SPECIFICATION)
             .putMember("TimeToLiveDescription", Schemas.TIME_TO_LIVE_DESCRIPTION)
             .putMember("SSEDescription", Schemas.SSE_DESCRIPTION)
             .putMember("VectorIndexes", Schemas.VECTOR_INDEXES)
             .builderSupplier(SourceTableFeatureDetails::builder)
             .shapeClass(SourceTableFeatureDetails.class)
             .build();

    static final Schema BACKUP_DESCRIPTION = Schema.structureBuilder(ShapeId.from("com.amazonaws.dynamodb#BackupDescription"))
             .putMember("BackupDetails", Schemas.BACKUP_DETAILS)
             .putMember("SourceTableDetails", Schemas.SOURCE_TABLE_DETAILS)
             .putMember("SourceTableFeatureDetails", Schemas.SOURCE_TABLE_FEATURE_DETAILS)
             .builderSupplier(BackupDescription::builder)
             .shapeClass(BackupDescription.class)
             .build();

    static final Schema ERROR_MESSAGE = Schema.createString(ShapeId.from("com.amazonaws.dynamodb#ErrorMessage"));
    static final Schema BACKUP_IN_USE_EXCEPTION = Schema.structureBuilder(ShapeId.from("com.amazonaws.dynamodb#BackupInUseException"),
            new ErrorTrait("client"))
             .putMember("message", Schemas.ERROR_MESSAGE)
             .builderSupplier(BackupInUseException::builder)
             .shapeClass(BackupInUseException.class)
             .build();

    static final Schema BACKUP_NOT_FOUND_EXCEPTION = Schema.structureBuilder(ShapeId.from("com.amazonaws.dynamodb#BackupNotFoundException"),
            new ErrorTrait("client"))
             .putMember("message", Schemas.ERROR_MESSAGE)
             .builderSupplier(BackupNotFoundException::builder)
             .shapeClass(BackupNotFoundException.class)
             .build();

    static final Schema BACKUPS_INPUT_LIMIT = Schema.createInteger(ShapeId.from("com.amazonaws.dynamodb#BackupsInputLimit"),
            RangeTrait.builder().min(new BigDecimal("1")).max(new BigDecimal("100")).build());
    static final Schema BACKUP_SUMMARY = Schema.structureBuilder(ShapeId.from("com.amazonaws.dynamodb#BackupSummary"))
             .putMember("TableName", Schemas.TABLE_NAME)
             .putMember("TableId", Schemas.TABLE_ID)
             .putMember("TableArn", Schemas.TABLE_ARN)
             .putMember("BackupArn", Schemas.BACKUP_ARN)
             .putMember("BackupName", Schemas.BACKUP_NAME)
             .putMember("BackupCreationDateTime", Schemas.BACKUP_CREATION_DATE_TIME)
             .putMember("BackupExpiryDateTime", Schemas.DATE)
             .putMember("BackupStatus", BackupStatus.$SCHEMA)
             .putMember("BackupType", BackupType.$SCHEMA)
             .putMember("BackupSizeBytes", Schemas.BACKUP_SIZE_BYTES)
             .builderSupplier(BackupSummary::builder)
             .shapeClass(BackupSummary.class)
             .build();

    static final Schema BACKUP_SUMMARIES = Schema.listBuilder(ShapeId.from("com.amazonaws.dynamodb#BackupSummaries"))
        .putMember("member", Schemas.BACKUP_SUMMARY)
        .build();

    static final Schema CONSISTENT_READ = Schema.createBoolean(ShapeId.from("com.amazonaws.dynamodb#ConsistentRead"));
    static final Schema PARTI_QL_STATEMENT = Schema.createString(ShapeId.from("com.amazonaws.dynamodb#PartiQLStatement"),
            LengthTrait.builder().min(1L).max(8192L).build());
    static final Schema CONSUMED_CAPACITY_UNITS = Schema.createDouble(ShapeId.from("com.amazonaws.dynamodb#ConsumedCapacityUnits"));
    static final Schema CAPACITY = Schema.structureBuilder(ShapeId.from("com.amazonaws.dynamodb#Capacity"))
             .putMember("ReadCapacityUnits", Schemas.CONSUMED_CAPACITY_UNITS)
             .putMember("WriteCapacityUnits", Schemas.CONSUMED_CAPACITY_UNITS)
             .putMember("CapacityUnits", Schemas.CONSUMED_CAPACITY_UNITS)
             .builderSupplier(Capacity::builder)
             .shapeClass(Capacity.class)
             .build();

    static final Schema SECONDARY_INDEXES_CAPACITY_MAP = Schema.mapBuilder(ShapeId.from("com.amazonaws.dynamodb#SecondaryIndexesCapacityMap"))
        .putMember("key", Schemas.INDEX_NAME)
        .putMember("value", Schemas.CAPACITY)
        .build();

    static final Schema VECTOR_CAPACITY = Schema.structureBuilder(ShapeId.from("com.amazonaws.dynamodb#VectorCapacity"))
             .putMember("VectorSearchRequestBytes", Schemas.CONSUMED_CAPACITY_UNITS)
             .putMember("VectorWriteRequestBytes", Schemas.CONSUMED_CAPACITY_UNITS)
             .builderSupplier(VectorCapacity::builder)
             .shapeClass(VectorCapacity.class)
             .build();

    static final Schema VECTOR_INDEXES_CAPACITY_MAP = Schema.mapBuilder(ShapeId.from("com.amazonaws.dynamodb#VectorIndexesCapacityMap"))
        .putMember("key", Schemas.INDEX_NAME)
        .putMember("value", Schemas.VECTOR_CAPACITY)
        .build();

    static final Schema CONSUMED_CAPACITY = Schema.structureBuilder(ShapeId.from("com.amazonaws.dynamodb#ConsumedCapacity"))
             .putMember("TableName", Schemas.TABLE_ARN)
             .putMember("CapacityUnits", Schemas.CONSUMED_CAPACITY_UNITS)
             .putMember("ReadCapacityUnits", Schemas.CONSUMED_CAPACITY_UNITS)
             .putMember("WriteCapacityUnits", Schemas.CONSUMED_CAPACITY_UNITS)
             .putMember("Table", Schemas.CAPACITY)
             .putMember("LocalSecondaryIndexes", Schemas.SECONDARY_INDEXES_CAPACITY_MAP)
             .putMember("GlobalSecondaryIndexes", Schemas.SECONDARY_INDEXES_CAPACITY_MAP)
             .putMember("VectorIndexes", Schemas.VECTOR_INDEXES_CAPACITY_MAP)
             .builderSupplier(ConsumedCapacity::builder)
             .shapeClass(ConsumedCapacity.class)
             .build();

    static final Schema CONSUMED_CAPACITY_MULTIPLE = Schema.listBuilder(ShapeId.from("com.amazonaws.dynamodb#ConsumedCapacityMultiple"))
        .putMember("member", Schemas.CONSUMED_CAPACITY)
        .build();

    static final Schema INTERNAL_SERVER_ERROR = Schema.structureBuilder(ShapeId.from("com.amazonaws.dynamodb#InternalServerError"),
            new ErrorTrait("server"))
             .putMember("message", Schemas.ERROR_MESSAGE)
             .builderSupplier(InternalServerError::builder)
             .shapeClass(InternalServerError.class)
             .build();

    static final Schema REASON = Schema.createString(ShapeId.from("com.amazonaws.dynamodb#Reason"));
    static final Schema RESOURCE = Schema.createString(ShapeId.from("com.amazonaws.dynamodb#Resource"));
    static final Schema THROTTLING_REASON = Schema.structureBuilder(ShapeId.from("com.amazonaws.dynamodb#ThrottlingReason"))
             .putMember("reason", Schemas.REASON)
             .putMember("resource", Schemas.RESOURCE)
             .builderSupplier(ThrottlingReason::builder)
             .shapeClass(ThrottlingReason.class)
             .build();

    static final Schema THROTTLING_REASON_LIST = Schema.listBuilder(ShapeId.from("com.amazonaws.dynamodb#ThrottlingReasonList"))
        .putMember("member", Schemas.THROTTLING_REASON)
        .build();

    static final Schema REQUEST_LIMIT_EXCEEDED = Schema.structureBuilder(ShapeId.from("com.amazonaws.dynamodb#RequestLimitExceeded"),
            new ErrorTrait("client"))
             .putMember("message", Schemas.ERROR_MESSAGE)
             .putMember("ThrottlingReasons", Schemas.THROTTLING_REASON_LIST)
             .builderSupplier(RequestLimitExceeded::builder)
             .shapeClass(RequestLimitExceeded.class)
             .build();

    static final Schema THROTTLING_EXCEPTION = Schema.structureBuilder(ShapeId.from("com.amazonaws.dynamodb#ThrottlingException"),
            new ErrorTrait("client"))
             .putMember("message", Schemas.AVAILABILITY_ERROR_MESSAGE)
             .putMember("throttlingReasons", Schemas.THROTTLING_REASON_LIST)
             .builderSupplier(ThrottlingException::builder)
             .shapeClass(ThrottlingException.class)
             .build();

    static final Schema EXPRESSION_ATTRIBUTE_NAME_VARIABLE = Schema.createString(ShapeId.from("com.amazonaws.dynamodb#ExpressionAttributeNameVariable"));
    static final Schema EXPRESSION_ATTRIBUTE_NAME_MAP = Schema.mapBuilder(ShapeId.from("com.amazonaws.dynamodb#ExpressionAttributeNameMap"))
        .putMember("key", Schemas.EXPRESSION_ATTRIBUTE_NAME_VARIABLE)
        .putMember("value", Schemas.ATTRIBUTE_NAME)
        .build();

    static final Schema PROJECTION_EXPRESSION = Schema.createString(ShapeId.from("com.amazonaws.dynamodb#ProjectionExpression"));
    static final Schema INVALID_ENDPOINT_EXCEPTION = Schema.structureBuilder(ShapeId.from("com.amazonaws.dynamodb#InvalidEndpointException"),
            new ErrorTrait("client"))
             .putMember("Message", Schemas.STRING)
             .builderSupplier(InvalidEndpointException::builder)
             .shapeClass(InvalidEndpointException.class)
             .build();

    static final Schema PROVISIONED_THROUGHPUT_EXCEEDED_EXCEPTION = Schema.structureBuilder(ShapeId.from("com.amazonaws.dynamodb#ProvisionedThroughputExceededException"),
            new ErrorTrait("client"))
             .putMember("message", Schemas.ERROR_MESSAGE)
             .putMember("ThrottlingReasons", Schemas.THROTTLING_REASON_LIST)
             .builderSupplier(ProvisionedThroughputExceededException::builder)
             .shapeClass(ProvisionedThroughputExceededException.class)
             .build();

    static final Schema RESOURCE_NOT_FOUND_EXCEPTION = Schema.structureBuilder(ShapeId.from("com.amazonaws.dynamodb#ResourceNotFoundException"),
            new ErrorTrait("client"))
             .putMember("message", Schemas.ERROR_MESSAGE)
             .builderSupplier(ResourceNotFoundException::builder)
             .shapeClass(ResourceNotFoundException.class)
             .build();

    static final Schema ITEM_COLLECTION_SIZE_ESTIMATE_BOUND = Schema.createDouble(ShapeId.from("com.amazonaws.dynamodb#ItemCollectionSizeEstimateBound"));
    static final Schema ITEM_COLLECTION_SIZE_ESTIMATE_RANGE = Schema.listBuilder(ShapeId.from("com.amazonaws.dynamodb#ItemCollectionSizeEstimateRange"))
        .putMember("member", Schemas.ITEM_COLLECTION_SIZE_ESTIMATE_BOUND)
        .build();

    static final Schema ITEM_COLLECTION_SIZE_LIMIT_EXCEEDED_EXCEPTION = Schema.structureBuilder(ShapeId.from("com.amazonaws.dynamodb#ItemCollectionSizeLimitExceededException"),
            new ErrorTrait("client"))
             .putMember("message", Schemas.ERROR_MESSAGE)
             .builderSupplier(ItemCollectionSizeLimitExceededException::builder)
             .shapeClass(ItemCollectionSizeLimitExceededException.class)
             .build();

    static final Schema REPLICATED_WRITE_CONFLICT_EXCEPTION = Schema.structureBuilder(ShapeId.from("com.amazonaws.dynamodb#ReplicatedWriteConflictException"),
            new ErrorTrait("client"),
            RetryableTrait.builder().throttling(false).build())
             .putMember("message", Schemas.ERROR_MESSAGE)
             .builderSupplier(ReplicatedWriteConflictException::builder)
             .shapeClass(ReplicatedWriteConflictException.class)
             .build();

    static final Schema BILLED_SIZE_BYTES = Schema.createLong(ShapeId.from("com.amazonaws.dynamodb#BilledSizeBytes"),
            RangeTrait.builder().min(new BigDecimal("0")).build());
    static final Schema BILLING_MODE_SUMMARY = Schema.structureBuilder(ShapeId.from("com.amazonaws.dynamodb#BillingModeSummary"))
             .putMember("BillingMode", BillingMode.$SCHEMA)
             .putMember("LastUpdateToPayPerRequestDateTime", Schemas.DATE)
             .builderSupplier(BillingModeSummary::builder)
             .shapeClass(BillingModeSummary.class)
             .build();

    static final Schema CODE = Schema.createString(ShapeId.from("com.amazonaws.dynamodb#Code"));
    static final Schema CLIENT_REQUEST_TOKEN = Schema.createString(ShapeId.from("com.amazonaws.dynamodb#ClientRequestToken"),
            LengthTrait.builder().min(1L).max(36L).build());
    static final Schema CLIENT_TOKEN = Schema.createString(ShapeId.from("com.amazonaws.dynamodb#ClientToken"),
            new PatternTrait("^[^\\$]+$"));
    static final Schema CLOUD_WATCH_LOG_GROUP_ARN = Schema.createString(ShapeId.from("com.amazonaws.dynamodb#CloudWatchLogGroupArn"),
            LengthTrait.builder().min(1L).max(1024L).build());
    static final Schema CONDITION_EXPRESSION = Schema.createString(ShapeId.from("com.amazonaws.dynamodb#ConditionExpression"));
    static final Schema EXPRESSION_ATTRIBUTE_VALUE_VARIABLE = Schema.createString(ShapeId.from("com.amazonaws.dynamodb#ExpressionAttributeValueVariable"));
    static final Schema CONFIRM_REMOVE_SELF_RESOURCE_ACCESS = Schema.createBoolean(ShapeId.from("com.amazonaws.dynamodb#ConfirmRemoveSelfResourceAccess"),
            new DefaultTrait(Node.from(false)));
    static final Schema RECOVERY_PERIOD_IN_DAYS = Schema.createInteger(ShapeId.from("com.amazonaws.dynamodb#RecoveryPeriodInDays"),
            RangeTrait.builder().min(new BigDecimal("1")).max(new BigDecimal("35")).build());
    static final Schema POINT_IN_TIME_RECOVERY_DESCRIPTION = Schema.structureBuilder(ShapeId.from("com.amazonaws.dynamodb#PointInTimeRecoveryDescription"))
             .putMember("PointInTimeRecoveryStatus", PointInTimeRecoveryStatus.$SCHEMA)
             .putMember("RecoveryPeriodInDays", Schemas.RECOVERY_PERIOD_IN_DAYS)
             .putMember("EarliestRestorableDateTime", Schemas.DATE)
             .putMember("LatestRestorableDateTime", Schemas.DATE)
             .builderSupplier(PointInTimeRecoveryDescription::builder)
             .shapeClass(PointInTimeRecoveryDescription.class)
             .build();

    static final Schema CONTINUOUS_BACKUPS_DESCRIPTION = Schema.structureBuilder(ShapeId.from("com.amazonaws.dynamodb#ContinuousBackupsDescription"))
             .putMember("ContinuousBackupsStatus", ContinuousBackupsStatus.$SCHEMA,
                     new RequiredTrait())
             .putMember("PointInTimeRecoveryDescription", Schemas.POINT_IN_TIME_RECOVERY_DESCRIPTION)
             .builderSupplier(ContinuousBackupsDescription::builder)
             .shapeClass(ContinuousBackupsDescription.class)
             .build();

    static final Schema CONTINUOUS_BACKUPS_UNAVAILABLE_EXCEPTION = Schema.structureBuilder(ShapeId.from("com.amazonaws.dynamodb#ContinuousBackupsUnavailableException"),
            new ErrorTrait("client"))
             .putMember("message", Schemas.ERROR_MESSAGE)
             .builderSupplier(ContinuousBackupsUnavailableException::builder)
             .shapeClass(ContinuousBackupsUnavailableException.class)
             .build();

    static final Schema CONTRIBUTOR_INSIGHTS_RULE = Schema.createString(ShapeId.from("com.amazonaws.dynamodb#ContributorInsightsRule"),
            new PatternTrait("^[A-Za-z0-9][A-Za-z0-9\\-\\_\\.]{0,126}[A-Za-z0-9]$"));
    static final Schema CONTRIBUTOR_INSIGHTS_RULE_LIST = Schema.listBuilder(ShapeId.from("com.amazonaws.dynamodb#ContributorInsightsRuleList"))
        .putMember("member", Schemas.CONTRIBUTOR_INSIGHTS_RULE)
        .build();

    static final Schema CONTRIBUTOR_INSIGHTS_SUMMARY = Schema.structureBuilder(ShapeId.from("com.amazonaws.dynamodb#ContributorInsightsSummary"))
             .putMember("TableName", Schemas.TABLE_NAME)
             .putMember("IndexName", Schemas.INDEX_NAME)
             .putMember("ContributorInsightsStatus", ContributorInsightsStatus.$SCHEMA)
             .putMember("ContributorInsightsMode", ContributorInsightsMode.$SCHEMA)
             .builderSupplier(ContributorInsightsSummary::builder)
             .shapeClass(ContributorInsightsSummary.class)
             .build();

    static final Schema CONTRIBUTOR_INSIGHTS_SUMMARIES = Schema.listBuilder(ShapeId.from("com.amazonaws.dynamodb#ContributorInsightsSummaries"))
        .putMember("member", Schemas.CONTRIBUTOR_INSIGHTS_SUMMARY)
        .build();

    static final Schema CREATE_BACKUP_INPUT = Schema.structureBuilder(ShapeId.from("com.amazonaws.dynamodb#CreateBackupInput"))
             .putMember("TableName", Schemas.TABLE_ARN,
                     new ContextParamTrait.Provider().createTrait(
                         ShapeId.from("smithy.rules#contextParam"),
                         Node.objectNodeBuilder()
                             .withMember("name", "ResourceArn")
                             .build()
                     ),
                     new RequiredTrait())
             .putMember("BackupName", Schemas.BACKUP_NAME,
                     new RequiredTrait())
             .builderSupplier(CreateBackupInput::builder)
             .shapeClass(CreateBackupInput.class)
             .build();

    static final Schema CREATE_BACKUP_OUTPUT = Schema.structureBuilder(ShapeId.from("com.amazonaws.dynamodb#CreateBackupOutput"))
             .putMember("BackupDetails", Schemas.BACKUP_DETAILS)
             .builderSupplier(CreateBackupOutput::builder)
             .shapeClass(CreateBackupOutput.class)
             .build();

    static final Schema LIMIT_EXCEEDED_EXCEPTION = Schema.structureBuilder(ShapeId.from("com.amazonaws.dynamodb#LimitExceededException"),
            new ErrorTrait("client"))
             .putMember("message", Schemas.ERROR_MESSAGE)
             .builderSupplier(LimitExceededException::builder)
             .shapeClass(LimitExceededException.class)
             .build();

    static final Schema TABLE_IN_USE_EXCEPTION = Schema.structureBuilder(ShapeId.from("com.amazonaws.dynamodb#TableInUseException"),
            new ErrorTrait("client"))
             .putMember("message", Schemas.ERROR_MESSAGE)
             .builderSupplier(TableInUseException::builder)
             .shapeClass(TableInUseException.class)
             .build();

    static final Schema TABLE_NOT_FOUND_EXCEPTION = Schema.structureBuilder(ShapeId.from("com.amazonaws.dynamodb#TableNotFoundException"),
            new ErrorTrait("client"))
             .putMember("message", Schemas.ERROR_MESSAGE)
             .builderSupplier(TableNotFoundException::builder)
             .shapeClass(TableNotFoundException.class)
             .build();

    static final Schema WARM_THROUGHPUT = Schema.structureBuilder(ShapeId.from("com.amazonaws.dynamodb#WarmThroughput"))
             .putMember("ReadUnitsPerSecond", Schemas.LONG_OBJECT)
             .putMember("WriteUnitsPerSecond", Schemas.LONG_OBJECT)
             .builderSupplier(WarmThroughput::builder)
             .shapeClass(WarmThroughput.class)
             .build();

    static final Schema CREATE_GLOBAL_SECONDARY_INDEX_ACTION = Schema.structureBuilder(ShapeId.from("com.amazonaws.dynamodb#CreateGlobalSecondaryIndexAction"))
             .putMember("IndexName", Schemas.INDEX_NAME,
                     new RequiredTrait())
             .putMember("KeySchema", Schemas.KEY_SCHEMA,
                     new RequiredTrait())
             .putMember("Projection", Schemas.PROJECTION,
                     new RequiredTrait())
             .putMember("ProvisionedThroughput", Schemas.PROVISIONED_THROUGHPUT)
             .putMember("OnDemandThroughput", Schemas.ON_DEMAND_THROUGHPUT)
             .putMember("WarmThroughput", Schemas.WARM_THROUGHPUT)
             .builderSupplier(CreateGlobalSecondaryIndexAction::builder)
             .shapeClass(CreateGlobalSecondaryIndexAction.class)
             .build();

    static final Schema REGION_NAME = Schema.createString(ShapeId.from("com.amazonaws.dynamodb#RegionName"));
    static final Schema REPLICA = Schema.structureBuilder(ShapeId.from("com.amazonaws.dynamodb#Replica"))
             .putMember("RegionName", Schemas.REGION_NAME)
             .builderSupplier(Replica::builder)
             .shapeClass(Replica.class)
             .build();

    static final Schema REPLICA_LIST = Schema.listBuilder(ShapeId.from("com.amazonaws.dynamodb#ReplicaList"))
        .putMember("member", Schemas.REPLICA)
        .build();

    static final Schema CREATE_GLOBAL_TABLE_INPUT = Schema.structureBuilder(ShapeId.from("com.amazonaws.dynamodb#CreateGlobalTableInput"))
             .putMember("GlobalTableName", Schemas.TABLE_NAME,
                     new ContextParamTrait.Provider().createTrait(
                         ShapeId.from("smithy.rules#contextParam"),
                         Node.objectNodeBuilder()
                             .withMember("name", "ResourceArn")
                             .build()
                     ),
                     new RequiredTrait())
             .putMember("ReplicationGroup", Schemas.REPLICA_LIST,
                     new RequiredTrait())
             .builderSupplier(CreateGlobalTableInput::builder)
             .shapeClass(CreateGlobalTableInput.class)
             .build();

    static final Schema GLOBAL_TABLE_ARN_STRING = Schema.createString(ShapeId.from("com.amazonaws.dynamodb#GlobalTableArnString"));
    static final Schema ON_DEMAND_THROUGHPUT_OVERRIDE = Schema.structureBuilder(ShapeId.from("com.amazonaws.dynamodb#OnDemandThroughputOverride"))
             .putMember("MaxReadRequestUnits", Schemas.LONG_OBJECT)
             .builderSupplier(OnDemandThroughputOverride::builder)
             .shapeClass(OnDemandThroughputOverride.class)
             .build();

    static final Schema PROVISIONED_THROUGHPUT_OVERRIDE = Schema.structureBuilder(ShapeId.from("com.amazonaws.dynamodb#ProvisionedThroughputOverride"))
             .putMember("ReadCapacityUnits", Schemas.POSITIVE_LONG_OBJECT)
             .builderSupplier(ProvisionedThroughputOverride::builder)
             .shapeClass(ProvisionedThroughputOverride.class)
             .build();

    static final Schema GLOBAL_SECONDARY_INDEX_WARM_THROUGHPUT_DESCRIPTION = Schema.structureBuilder(ShapeId.from("com.amazonaws.dynamodb#GlobalSecondaryIndexWarmThroughputDescription"))
             .putMember("ReadUnitsPerSecond", Schemas.POSITIVE_LONG_OBJECT)
             .putMember("WriteUnitsPerSecond", Schemas.POSITIVE_LONG_OBJECT)
             .putMember("Status", IndexStatus.$SCHEMA)
             .builderSupplier(GlobalSecondaryIndexWarmThroughputDescription::builder)
             .shapeClass(GlobalSecondaryIndexWarmThroughputDescription.class)
             .build();

    static final Schema REPLICA_GLOBAL_SECONDARY_INDEX_DESCRIPTION = Schema.structureBuilder(ShapeId.from("com.amazonaws.dynamodb#ReplicaGlobalSecondaryIndexDescription"))
             .putMember("IndexName", Schemas.INDEX_NAME)
             .putMember("ProvisionedThroughputOverride", Schemas.PROVISIONED_THROUGHPUT_OVERRIDE)
             .putMember("OnDemandThroughputOverride", Schemas.ON_DEMAND_THROUGHPUT_OVERRIDE)
             .putMember("WarmThroughput", Schemas.GLOBAL_SECONDARY_INDEX_WARM_THROUGHPUT_DESCRIPTION)
             .builderSupplier(ReplicaGlobalSecondaryIndexDescription::builder)
             .shapeClass(ReplicaGlobalSecondaryIndexDescription.class)
             .build();

    static final Schema REPLICA_GLOBAL_SECONDARY_INDEX_DESCRIPTION_LIST = Schema.listBuilder(ShapeId.from("com.amazonaws.dynamodb#ReplicaGlobalSecondaryIndexDescriptionList"))
        .putMember("member", Schemas.REPLICA_GLOBAL_SECONDARY_INDEX_DESCRIPTION)
        .build();

    static final Schema KMS_MASTER_KEY_ID = Schema.createString(ShapeId.from("com.amazonaws.dynamodb#KMSMasterKeyId"));
    static final Schema REPLICA_STATUS_DESCRIPTION = Schema.createString(ShapeId.from("com.amazonaws.dynamodb#ReplicaStatusDescription"));
    static final Schema REPLICA_STATUS_PERCENT_PROGRESS = Schema.createString(ShapeId.from("com.amazonaws.dynamodb#ReplicaStatusPercentProgress"));
    static final Schema TABLE_CLASS_SUMMARY = Schema.structureBuilder(ShapeId.from("com.amazonaws.dynamodb#TableClassSummary"))
             .putMember("TableClass", TableClass.$SCHEMA)
             .putMember("LastUpdateDateTime", Schemas.DATE)
             .builderSupplier(TableClassSummary::builder)
             .shapeClass(TableClassSummary.class)
             .build();

    static final Schema TABLE_WARM_THROUGHPUT_DESCRIPTION = Schema.structureBuilder(ShapeId.from("com.amazonaws.dynamodb#TableWarmThroughputDescription"))
             .putMember("ReadUnitsPerSecond", Schemas.POSITIVE_LONG_OBJECT)
             .putMember("WriteUnitsPerSecond", Schemas.POSITIVE_LONG_OBJECT)
             .putMember("Status", TableStatus.$SCHEMA)
             .builderSupplier(TableWarmThroughputDescription::builder)
             .shapeClass(TableWarmThroughputDescription.class)
             .build();

    static final Schema REPLICA_DESCRIPTION = Schema.structureBuilder(ShapeId.from("com.amazonaws.dynamodb#ReplicaDescription"))
             .putMember("RegionName", Schemas.REGION_NAME)
             .putMember("ReplicaStatus", ReplicaStatus.$SCHEMA)
             .putMember("ReplicaArn", Schemas.STRING)
             .putMember("ReplicaStatusDescription", Schemas.REPLICA_STATUS_DESCRIPTION)
             .putMember("ReplicaStatusPercentProgress", Schemas.REPLICA_STATUS_PERCENT_PROGRESS)
             .putMember("KMSMasterKeyId", Schemas.KMS_MASTER_KEY_ID)
             .putMember("ProvisionedThroughputOverride", Schemas.PROVISIONED_THROUGHPUT_OVERRIDE)
             .putMember("OnDemandThroughputOverride", Schemas.ON_DEMAND_THROUGHPUT_OVERRIDE)
             .putMember("WarmThroughput", Schemas.TABLE_WARM_THROUGHPUT_DESCRIPTION)
             .putMember("GlobalSecondaryIndexes", Schemas.REPLICA_GLOBAL_SECONDARY_INDEX_DESCRIPTION_LIST)
             .putMember("ReplicaInaccessibleDateTime", Schemas.DATE)
             .putMember("ReplicaTableClassSummary", Schemas.TABLE_CLASS_SUMMARY)
             .putMember("GlobalTableSettingsReplicationMode", GlobalTableSettingsReplicationMode.$SCHEMA)
             .builderSupplier(ReplicaDescription::builder)
             .shapeClass(ReplicaDescription.class)
             .build();

    static final Schema REPLICA_DESCRIPTION_LIST = Schema.listBuilder(ShapeId.from("com.amazonaws.dynamodb#ReplicaDescriptionList"))
        .putMember("member", Schemas.REPLICA_DESCRIPTION)
        .build();

    static final Schema GLOBAL_TABLE_DESCRIPTION = Schema.structureBuilder(ShapeId.from("com.amazonaws.dynamodb#GlobalTableDescription"))
             .putMember("ReplicationGroup", Schemas.REPLICA_DESCRIPTION_LIST)
             .putMember("GlobalTableArn", Schemas.GLOBAL_TABLE_ARN_STRING)
             .putMember("CreationDateTime", Schemas.DATE)
             .putMember("GlobalTableStatus", GlobalTableStatus.$SCHEMA)
             .putMember("GlobalTableName", Schemas.TABLE_NAME)
             .builderSupplier(GlobalTableDescription::builder)
             .shapeClass(GlobalTableDescription.class)
             .build();

    static final Schema CREATE_GLOBAL_TABLE_OUTPUT = Schema.structureBuilder(ShapeId.from("com.amazonaws.dynamodb#CreateGlobalTableOutput"))
             .putMember("GlobalTableDescription", Schemas.GLOBAL_TABLE_DESCRIPTION)
             .builderSupplier(CreateGlobalTableOutput::builder)
             .shapeClass(CreateGlobalTableOutput.class)
             .build();

    static final Schema GLOBAL_TABLE_ALREADY_EXISTS_EXCEPTION = Schema.structureBuilder(ShapeId.from("com.amazonaws.dynamodb#GlobalTableAlreadyExistsException"),
            new ErrorTrait("client"))
             .putMember("message", Schemas.ERROR_MESSAGE)
             .builderSupplier(GlobalTableAlreadyExistsException::builder)
             .shapeClass(GlobalTableAlreadyExistsException.class)
             .build();

    static final Schema CREATE_GLOBAL_TABLE_WITNESS_GROUP_MEMBER_ACTION = Schema.structureBuilder(ShapeId.from("com.amazonaws.dynamodb#CreateGlobalTableWitnessGroupMemberAction"))
             .putMember("RegionName", Schemas.REGION_NAME,
                     new RequiredTrait())
             .builderSupplier(CreateGlobalTableWitnessGroupMemberAction::builder)
             .shapeClass(CreateGlobalTableWitnessGroupMemberAction.class)
             .build();

    static final Schema CREATE_REPLICA_ACTION = Schema.structureBuilder(ShapeId.from("com.amazonaws.dynamodb#CreateReplicaAction"))
             .putMember("RegionName", Schemas.REGION_NAME,
                     new RequiredTrait())
             .builderSupplier(CreateReplicaAction::builder)
             .shapeClass(CreateReplicaAction.class)
             .build();

    static final Schema REPLICA_GLOBAL_SECONDARY_INDEX = Schema.structureBuilder(ShapeId.from("com.amazonaws.dynamodb#ReplicaGlobalSecondaryIndex"))
             .putMember("IndexName", Schemas.INDEX_NAME,
                     new RequiredTrait())
             .putMember("ProvisionedThroughputOverride", Schemas.PROVISIONED_THROUGHPUT_OVERRIDE)
             .putMember("OnDemandThroughputOverride", Schemas.ON_DEMAND_THROUGHPUT_OVERRIDE)
             .builderSupplier(ReplicaGlobalSecondaryIndex::builder)
             .shapeClass(ReplicaGlobalSecondaryIndex.class)
             .build();

    static final Schema REPLICA_GLOBAL_SECONDARY_INDEX_LIST = Schema.listBuilder(ShapeId.from("com.amazonaws.dynamodb#ReplicaGlobalSecondaryIndexList"),
            LengthTrait.builder().min(1L).build())
        .putMember("member", Schemas.REPLICA_GLOBAL_SECONDARY_INDEX)
        .build();

    static final Schema CREATE_REPLICATION_GROUP_MEMBER_ACTION = Schema.structureBuilder(ShapeId.from("com.amazonaws.dynamodb#CreateReplicationGroupMemberAction"))
             .putMember("RegionName", Schemas.REGION_NAME,
                     new RequiredTrait())
             .putMember("KMSMasterKeyId", Schemas.KMS_MASTER_KEY_ID)
             .putMember("ProvisionedThroughputOverride", Schemas.PROVISIONED_THROUGHPUT_OVERRIDE)
             .putMember("OnDemandThroughputOverride", Schemas.ON_DEMAND_THROUGHPUT_OVERRIDE)
             .putMember("GlobalSecondaryIndexes", Schemas.REPLICA_GLOBAL_SECONDARY_INDEX_LIST)
             .putMember("TableClassOverride", TableClass.$SCHEMA)
             .builderSupplier(CreateReplicationGroupMemberAction::builder)
             .shapeClass(CreateReplicationGroupMemberAction.class)
             .build();

    static final Schema DELETION_PROTECTION_ENABLED = Schema.createBoolean(ShapeId.from("com.amazonaws.dynamodb#DeletionProtectionEnabled"));
    static final Schema GLOBAL_SECONDARY_INDEX = Schema.structureBuilder(ShapeId.from("com.amazonaws.dynamodb#GlobalSecondaryIndex"))
             .putMember("IndexName", Schemas.INDEX_NAME,
                     new RequiredTrait())
             .putMember("KeySchema", Schemas.KEY_SCHEMA,
                     new RequiredTrait())
             .putMember("Projection", Schemas.PROJECTION,
                     new RequiredTrait())
             .putMember("ProvisionedThroughput", Schemas.PROVISIONED_THROUGHPUT)
             .putMember("OnDemandThroughput", Schemas.ON_DEMAND_THROUGHPUT)
             .putMember("WarmThroughput", Schemas.WARM_THROUGHPUT)
             .builderSupplier(GlobalSecondaryIndex::builder)
             .shapeClass(GlobalSecondaryIndex.class)
             .build();

    static final Schema GLOBAL_SECONDARY_INDEX_LIST = Schema.listBuilder(ShapeId.from("com.amazonaws.dynamodb#GlobalSecondaryIndexList"))
        .putMember("member", Schemas.GLOBAL_SECONDARY_INDEX)
        .build();

    static final Schema LOCAL_SECONDARY_INDEX = Schema.structureBuilder(ShapeId.from("com.amazonaws.dynamodb#LocalSecondaryIndex"))
             .putMember("IndexName", Schemas.INDEX_NAME,
                     new RequiredTrait())
             .putMember("KeySchema", Schemas.KEY_SCHEMA,
                     new RequiredTrait())
             .putMember("Projection", Schemas.PROJECTION,
                     new RequiredTrait())
             .builderSupplier(LocalSecondaryIndex::builder)
             .shapeClass(LocalSecondaryIndex.class)
             .build();

    static final Schema LOCAL_SECONDARY_INDEX_LIST = Schema.listBuilder(ShapeId.from("com.amazonaws.dynamodb#LocalSecondaryIndexList"))
        .putMember("member", Schemas.LOCAL_SECONDARY_INDEX)
        .build();

    static final Schema RESOURCE_POLICY = Schema.createString(ShapeId.from("com.amazonaws.dynamodb#ResourcePolicy"));
    static final Schema SSE_ENABLED = Schema.createBoolean(ShapeId.from("com.amazonaws.dynamodb#SSEEnabled"));
    static final Schema SSE_SPECIFICATION = Schema.structureBuilder(ShapeId.from("com.amazonaws.dynamodb#SSESpecification"))
             .putMember("Enabled", Schemas.SSE_ENABLED)
             .putMember("SSEType", SSEType.$SCHEMA)
             .putMember("KMSMasterKeyId", Schemas.KMS_MASTER_KEY_ID)
             .builderSupplier(SSESpecification::builder)
             .shapeClass(SSESpecification.class)
             .build();

    static final Schema TAG_KEY_STRING = Schema.createString(ShapeId.from("com.amazonaws.dynamodb#TagKeyString"),
            LengthTrait.builder().min(1L).max(128L).build());
    static final Schema TAG_VALUE_STRING = Schema.createString(ShapeId.from("com.amazonaws.dynamodb#TagValueString"),
            LengthTrait.builder().min(0L).max(256L).build());
    static final Schema TAG = Schema.structureBuilder(ShapeId.from("com.amazonaws.dynamodb#Tag"))
             .putMember("Key", Schemas.TAG_KEY_STRING,
                     new RequiredTrait())
             .putMember("Value", Schemas.TAG_VALUE_STRING,
                     new RequiredTrait())
             .builderSupplier(Tag::builder)
             .shapeClass(Tag.class)
             .build();

    static final Schema TAG_LIST = Schema.listBuilder(ShapeId.from("com.amazonaws.dynamodb#TagList"))
        .putMember("member", Schemas.TAG)
        .build();

    static final Schema VECTOR_INDEX = Schema.structureBuilder(ShapeId.from("com.amazonaws.dynamodb#VectorIndex"))
             .putMember("IndexName", Schemas.INDEX_NAME,
                     new RequiredTrait())
             .putMember("VectorAttribute", Schemas.VECTOR_ATTRIBUTE_DEFINITION,
                     new RequiredTrait())
             .putMember("SearchSchema", Schemas.SEARCH_SCHEMA)
             .putMember("Projection", Schemas.PROJECTION,
                     new RequiredTrait())
             .putMember("Dimensions", Schemas.POSITIVE_LONG_OBJECT,
                     new RequiredTrait())
             .putMember("DistanceFunction", VectorDistanceFunction.$SCHEMA,
                     new RequiredTrait())
             .builderSupplier(VectorIndex::builder)
             .shapeClass(VectorIndex.class)
             .build();

    static final Schema VECTOR_INDEX_LIST = Schema.listBuilder(ShapeId.from("com.amazonaws.dynamodb#VectorIndexList"))
        .putMember("member", Schemas.VECTOR_INDEX)
        .build();

    static final Schema CREATE_TABLE_INPUT = Schema.structureBuilder(ShapeId.from("com.amazonaws.dynamodb#CreateTableInput"))
             .putMember("AttributeDefinitions", Schemas.ATTRIBUTE_DEFINITIONS)
             .putMember("TableName", Schemas.TABLE_ARN,
                     new ContextParamTrait.Provider().createTrait(
                         ShapeId.from("smithy.rules#contextParam"),
                         Node.objectNodeBuilder()
                             .withMember("name", "ResourceArn")
                             .build()
                     ),
                     new RequiredTrait())
             .putMember("KeySchema", Schemas.KEY_SCHEMA)
             .putMember("LocalSecondaryIndexes", Schemas.LOCAL_SECONDARY_INDEX_LIST)
             .putMember("GlobalSecondaryIndexes", Schemas.GLOBAL_SECONDARY_INDEX_LIST)
             .putMember("BillingMode", BillingMode.$SCHEMA)
             .putMember("ProvisionedThroughput", Schemas.PROVISIONED_THROUGHPUT)
             .putMember("StreamSpecification", Schemas.STREAM_SPECIFICATION)
             .putMember("SSESpecification", Schemas.SSE_SPECIFICATION)
             .putMember("Tags", Schemas.TAG_LIST)
             .putMember("TableClass", TableClass.$SCHEMA)
             .putMember("DeletionProtectionEnabled", Schemas.DELETION_PROTECTION_ENABLED)
             .putMember("WarmThroughput", Schemas.WARM_THROUGHPUT)
             .putMember("ResourcePolicy", Schemas.RESOURCE_POLICY)
             .putMember("OnDemandThroughput", Schemas.ON_DEMAND_THROUGHPUT)
             .putMember("GlobalTableSourceArn", Schemas.TABLE_ARN)
             .putMember("GlobalTableSettingsReplicationMode", GlobalTableSettingsReplicationMode.$SCHEMA)
             .putMember("VectorIndexes", Schemas.VECTOR_INDEX_LIST)
             .builderSupplier(CreateTableInput::builder)
             .shapeClass(CreateTableInput.class)
             .build();

    static final Schema NON_NEGATIVE_LONG_OBJECT = Schema.createLong(ShapeId.from("com.amazonaws.dynamodb#NonNegativeLongObject"),
            RangeTrait.builder().min(new BigDecimal("0")).build());
    static final Schema PROVISIONED_THROUGHPUT_DESCRIPTION = Schema.structureBuilder(ShapeId.from("com.amazonaws.dynamodb#ProvisionedThroughputDescription"))
             .putMember("LastIncreaseDateTime", Schemas.DATE)
             .putMember("LastDecreaseDateTime", Schemas.DATE)
             .putMember("NumberOfDecreasesToday", Schemas.POSITIVE_LONG_OBJECT)
             .putMember("ReadCapacityUnits", Schemas.NON_NEGATIVE_LONG_OBJECT)
             .putMember("WriteCapacityUnits", Schemas.NON_NEGATIVE_LONG_OBJECT)
             .builderSupplier(ProvisionedThroughputDescription::builder)
             .shapeClass(ProvisionedThroughputDescription.class)
             .build();

    static final Schema GLOBAL_SECONDARY_INDEX_DESCRIPTION = Schema.structureBuilder(ShapeId.from("com.amazonaws.dynamodb#GlobalSecondaryIndexDescription"))
             .putMember("IndexName", Schemas.INDEX_NAME)
             .putMember("KeySchema", Schemas.KEY_SCHEMA)
             .putMember("Projection", Schemas.PROJECTION)
             .putMember("IndexStatus", IndexStatus.$SCHEMA)
             .putMember("Backfilling", Schemas.BACKFILLING)
             .putMember("ProvisionedThroughput", Schemas.PROVISIONED_THROUGHPUT_DESCRIPTION)
             .putMember("IndexSizeBytes", Schemas.LONG_OBJECT)
             .putMember("ItemCount", Schemas.LONG_OBJECT)
             .putMember("IndexArn", Schemas.STRING)
             .putMember("OnDemandThroughput", Schemas.ON_DEMAND_THROUGHPUT)
             .putMember("WarmThroughput", Schemas.GLOBAL_SECONDARY_INDEX_WARM_THROUGHPUT_DESCRIPTION)
             .builderSupplier(GlobalSecondaryIndexDescription::builder)
             .shapeClass(GlobalSecondaryIndexDescription.class)
             .build();

    static final Schema GLOBAL_SECONDARY_INDEX_DESCRIPTION_LIST = Schema.listBuilder(ShapeId.from("com.amazonaws.dynamodb#GlobalSecondaryIndexDescriptionList"))
        .putMember("member", Schemas.GLOBAL_SECONDARY_INDEX_DESCRIPTION)
        .build();

    static final Schema GLOBAL_TABLE_WITNESS_DESCRIPTION = Schema.structureBuilder(ShapeId.from("com.amazonaws.dynamodb#GlobalTableWitnessDescription"))
             .putMember("RegionName", Schemas.REGION_NAME)
             .putMember("WitnessStatus", WitnessStatus.$SCHEMA)
             .builderSupplier(GlobalTableWitnessDescription::builder)
             .shapeClass(GlobalTableWitnessDescription.class)
             .build();

    static final Schema GLOBAL_TABLE_WITNESS_DESCRIPTION_LIST = Schema.listBuilder(ShapeId.from("com.amazonaws.dynamodb#GlobalTableWitnessDescriptionList"))
        .putMember("member", Schemas.GLOBAL_TABLE_WITNESS_DESCRIPTION)
        .build();

    static final Schema STREAM_ARN = Schema.createString(ShapeId.from("com.amazonaws.dynamodb#StreamArn"),
            LengthTrait.builder().min(37L).max(1024L).build());
    static final Schema LOCAL_SECONDARY_INDEX_DESCRIPTION = Schema.structureBuilder(ShapeId.from("com.amazonaws.dynamodb#LocalSecondaryIndexDescription"))
             .putMember("IndexName", Schemas.INDEX_NAME)
             .putMember("KeySchema", Schemas.KEY_SCHEMA)
             .putMember("Projection", Schemas.PROJECTION)
             .putMember("IndexSizeBytes", Schemas.LONG_OBJECT)
             .putMember("ItemCount", Schemas.LONG_OBJECT)
             .putMember("IndexArn", Schemas.STRING)
             .builderSupplier(LocalSecondaryIndexDescription::builder)
             .shapeClass(LocalSecondaryIndexDescription.class)
             .build();

    static final Schema LOCAL_SECONDARY_INDEX_DESCRIPTION_LIST = Schema.listBuilder(ShapeId.from("com.amazonaws.dynamodb#LocalSecondaryIndexDescriptionList"))
        .putMember("member", Schemas.LOCAL_SECONDARY_INDEX_DESCRIPTION)
        .build();

    static final Schema RESTORE_IN_PROGRESS = Schema.createBoolean(ShapeId.from("com.amazonaws.dynamodb#RestoreInProgress"));
    static final Schema RESTORE_SUMMARY = Schema.structureBuilder(ShapeId.from("com.amazonaws.dynamodb#RestoreSummary"))
             .putMember("SourceBackupArn", Schemas.BACKUP_ARN)
             .putMember("SourceTableArn", Schemas.TABLE_ARN)
             .putMember("RestoreDateTime", Schemas.DATE,
                     new RequiredTrait())
             .putMember("RestoreInProgress", Schemas.RESTORE_IN_PROGRESS,
                     new RequiredTrait())
             .builderSupplier(RestoreSummary::builder)
             .shapeClass(RestoreSummary.class)
             .build();

    static final Schema VECTOR_INDEX_DESCRIPTION = Schema.structureBuilder(ShapeId.from("com.amazonaws.dynamodb#VectorIndexDescription"))
             .putMember("IndexName", Schemas.INDEX_NAME)
             .putMember("SearchSchema", Schemas.SEARCH_SCHEMA)
             .putMember("Projection", Schemas.PROJECTION)
             .putMember("VectorAttribute", Schemas.VECTOR_ATTRIBUTE_DEFINITION)
             .putMember("Dimensions", Schemas.POSITIVE_LONG_OBJECT)
             .putMember("DistanceFunction", VectorDistanceFunction.$SCHEMA)
             .putMember("IndexStatus", IndexStatus.$SCHEMA)
             .putMember("Backfilling", Schemas.BACKFILLING)
             .putMember("IndexSizeBytes", Schemas.LONG_OBJECT)
             .putMember("ItemCount", Schemas.LONG_OBJECT)
             .putMember("IndexArn", Schemas.STRING)
             .builderSupplier(VectorIndexDescription::builder)
             .shapeClass(VectorIndexDescription.class)
             .build();

    static final Schema VECTOR_INDEX_DESCRIPTION_LIST = Schema.listBuilder(ShapeId.from("com.amazonaws.dynamodb#VectorIndexDescriptionList"))
        .putMember("member", Schemas.VECTOR_INDEX_DESCRIPTION)
        .build();

    static final Schema TABLE_DESCRIPTION = Schema.structureBuilder(ShapeId.from("com.amazonaws.dynamodb#TableDescription"))
             .putMember("AttributeDefinitions", Schemas.ATTRIBUTE_DEFINITIONS)
             .putMember("TableName", Schemas.TABLE_NAME)
             .putMember("KeySchema", Schemas.KEY_SCHEMA)
             .putMember("TableStatus", TableStatus.$SCHEMA)
             .putMember("CreationDateTime", Schemas.DATE)
             .putMember("ProvisionedThroughput", Schemas.PROVISIONED_THROUGHPUT_DESCRIPTION)
             .putMember("TableSizeBytes", Schemas.LONG_OBJECT)
             .putMember("ItemCount", Schemas.LONG_OBJECT)
             .putMember("TableArn", Schemas.STRING)
             .putMember("TableId", Schemas.TABLE_ID)
             .putMember("BillingModeSummary", Schemas.BILLING_MODE_SUMMARY)
             .putMember("LocalSecondaryIndexes", Schemas.LOCAL_SECONDARY_INDEX_DESCRIPTION_LIST)
             .putMember("GlobalSecondaryIndexes", Schemas.GLOBAL_SECONDARY_INDEX_DESCRIPTION_LIST)
             .putMember("StreamSpecification", Schemas.STREAM_SPECIFICATION)
             .putMember("LatestStreamLabel", Schemas.STRING)
             .putMember("LatestStreamArn", Schemas.STREAM_ARN)
             .putMember("GlobalTableVersion", Schemas.STRING)
             .putMember("Replicas", Schemas.REPLICA_DESCRIPTION_LIST)
             .putMember("GlobalTableWitnesses", Schemas.GLOBAL_TABLE_WITNESS_DESCRIPTION_LIST)
             .putMember("GlobalTableSettingsReplicationMode", GlobalTableSettingsReplicationMode.$SCHEMA)
             .putMember("RestoreSummary", Schemas.RESTORE_SUMMARY)
             .putMember("SSEDescription", Schemas.SSE_DESCRIPTION)
             .putMember("ArchivalSummary", Schemas.ARCHIVAL_SUMMARY)
             .putMember("TableClassSummary", Schemas.TABLE_CLASS_SUMMARY)
             .putMember("DeletionProtectionEnabled", Schemas.DELETION_PROTECTION_ENABLED)
             .putMember("OnDemandThroughput", Schemas.ON_DEMAND_THROUGHPUT)
             .putMember("WarmThroughput", Schemas.TABLE_WARM_THROUGHPUT_DESCRIPTION)
             .putMember("MultiRegionConsistency", MultiRegionConsistency.$SCHEMA)
             .putMember("VectorIndexes", Schemas.VECTOR_INDEX_DESCRIPTION_LIST)
             .builderSupplier(TableDescription::builder)
             .shapeClass(TableDescription.class)
             .build();

    static final Schema CREATE_TABLE_OUTPUT = Schema.structureBuilder(ShapeId.from("com.amazonaws.dynamodb#CreateTableOutput"))
             .putMember("TableDescription", Schemas.TABLE_DESCRIPTION)
             .builderSupplier(CreateTableOutput::builder)
             .shapeClass(CreateTableOutput.class)
             .build();

    static final Schema RESOURCE_IN_USE_EXCEPTION = Schema.structureBuilder(ShapeId.from("com.amazonaws.dynamodb#ResourceInUseException"),
            new ErrorTrait("client"))
             .putMember("message", Schemas.ERROR_MESSAGE)
             .builderSupplier(ResourceInUseException::builder)
             .shapeClass(ResourceInUseException.class)
             .build();

    static final Schema CREATE_VECTOR_INDEX_ACTION = Schema.structureBuilder(ShapeId.from("com.amazonaws.dynamodb#CreateVectorIndexAction"))
             .putMember("IndexName", Schemas.INDEX_NAME,
                     new RequiredTrait())
             .putMember("VectorAttribute", Schemas.VECTOR_ATTRIBUTE_DEFINITION,
                     new RequiredTrait())
             .putMember("SearchSchema", Schemas.SEARCH_SCHEMA)
             .putMember("Projection", Schemas.PROJECTION,
                     new RequiredTrait())
             .putMember("Dimensions", Schemas.POSITIVE_LONG_OBJECT,
                     new RequiredTrait())
             .putMember("DistanceFunction", VectorDistanceFunction.$SCHEMA,
                     new RequiredTrait())
             .builderSupplier(CreateVectorIndexAction::builder)
             .shapeClass(CreateVectorIndexAction.class)
             .build();

    static final Schema CSV_DELIMITER = Schema.createString(ShapeId.from("com.amazonaws.dynamodb#CsvDelimiter"),
            LengthTrait.builder().min(1L).max(1L).build(),
            new PatternTrait("^[,;:|\\t ]$"));
    static final Schema CSV_HEADER = Schema.createString(ShapeId.from("com.amazonaws.dynamodb#CsvHeader"),
            LengthTrait.builder().min(1L).max(65536L).build(),
            new PatternTrait("^[\\x20-\\x21\\x23-\\x2B\\x2D-\\x7E]*$"));
    static final Schema CSV_HEADER_LIST = Schema.listBuilder(ShapeId.from("com.amazonaws.dynamodb#CsvHeaderList"),
            LengthTrait.builder().min(1L).max(255L).build())
        .putMember("member", Schemas.CSV_HEADER)
        .build();

    static final Schema CSV_OPTIONS = Schema.structureBuilder(ShapeId.from("com.amazonaws.dynamodb#CsvOptions"))
             .putMember("Delimiter", Schemas.CSV_DELIMITER)
             .putMember("HeaderList", Schemas.CSV_HEADER_LIST)
             .builderSupplier(CsvOptions::builder)
             .shapeClass(CsvOptions.class)
             .build();

    static final Schema DELETE_BACKUP_INPUT = Schema.structureBuilder(ShapeId.from("com.amazonaws.dynamodb#DeleteBackupInput"))
             .putMember("BackupArn", Schemas.BACKUP_ARN,
                     new ContextParamTrait.Provider().createTrait(
                         ShapeId.from("smithy.rules#contextParam"),
                         Node.objectNodeBuilder()
                             .withMember("name", "ResourceArn")
                             .build()
                     ),
                     new RequiredTrait())
             .builderSupplier(DeleteBackupInput::builder)
             .shapeClass(DeleteBackupInput.class)
             .build();

    static final Schema DELETE_BACKUP_OUTPUT = Schema.structureBuilder(ShapeId.from("com.amazonaws.dynamodb#DeleteBackupOutput"))
             .putMember("BackupDescription", Schemas.BACKUP_DESCRIPTION)
             .builderSupplier(DeleteBackupOutput::builder)
             .shapeClass(DeleteBackupOutput.class)
             .build();

    static final Schema DELETE_GLOBAL_SECONDARY_INDEX_ACTION = Schema.structureBuilder(ShapeId.from("com.amazonaws.dynamodb#DeleteGlobalSecondaryIndexAction"))
             .putMember("IndexName", Schemas.INDEX_NAME,
                     new RequiredTrait())
             .builderSupplier(DeleteGlobalSecondaryIndexAction::builder)
             .shapeClass(DeleteGlobalSecondaryIndexAction.class)
             .build();

    static final Schema DELETE_GLOBAL_TABLE_WITNESS_GROUP_MEMBER_ACTION = Schema.structureBuilder(ShapeId.from("com.amazonaws.dynamodb#DeleteGlobalTableWitnessGroupMemberAction"))
             .putMember("RegionName", Schemas.REGION_NAME,
                     new RequiredTrait())
             .builderSupplier(DeleteGlobalTableWitnessGroupMemberAction::builder)
             .shapeClass(DeleteGlobalTableWitnessGroupMemberAction.class)
             .build();

    static final Schema TRANSACTION_CONFLICT_EXCEPTION = Schema.structureBuilder(ShapeId.from("com.amazonaws.dynamodb#TransactionConflictException"),
            new ErrorTrait("client"))
             .putMember("message", Schemas.ERROR_MESSAGE)
             .builderSupplier(TransactionConflictException::builder)
             .shapeClass(TransactionConflictException.class)
             .build();

    static final Schema DELETE_REPLICA_ACTION = Schema.structureBuilder(ShapeId.from("com.amazonaws.dynamodb#DeleteReplicaAction"))
             .putMember("RegionName", Schemas.REGION_NAME,
                     new RequiredTrait())
             .builderSupplier(DeleteReplicaAction::builder)
             .shapeClass(DeleteReplicaAction.class)
             .build();

    static final Schema DELETE_REPLICATION_GROUP_MEMBER_ACTION = Schema.structureBuilder(ShapeId.from("com.amazonaws.dynamodb#DeleteReplicationGroupMemberAction"))
             .putMember("RegionName", Schemas.REGION_NAME,
                     new RequiredTrait())
             .builderSupplier(DeleteReplicationGroupMemberAction::builder)
             .shapeClass(DeleteReplicationGroupMemberAction.class)
             .build();

    static final Schema POLICY_REVISION_ID = Schema.createString(ShapeId.from("com.amazonaws.dynamodb#PolicyRevisionId"),
            LengthTrait.builder().min(1L).max(255L).build());
    static final Schema RESOURCE_ARN_STRING = Schema.createString(ShapeId.from("com.amazonaws.dynamodb#ResourceArnString"),
            LengthTrait.builder().min(1L).max(1283L).build());
    static final Schema DELETE_RESOURCE_POLICY_INPUT = Schema.structureBuilder(ShapeId.from("com.amazonaws.dynamodb#DeleteResourcePolicyInput"))
             .putMember("ResourceArn", Schemas.RESOURCE_ARN_STRING,
                     new ContextParamTrait.Provider().createTrait(
                         ShapeId.from("smithy.rules#contextParam"),
                         Node.objectNodeBuilder()
                             .withMember("name", "ResourceArn")
                             .build()
                     ),
                     new RequiredTrait())
             .putMember("ExpectedRevisionId", Schemas.POLICY_REVISION_ID)
             .builderSupplier(DeleteResourcePolicyInput::builder)
             .shapeClass(DeleteResourcePolicyInput.class)
             .build();

    static final Schema DELETE_RESOURCE_POLICY_OUTPUT = Schema.structureBuilder(ShapeId.from("com.amazonaws.dynamodb#DeleteResourcePolicyOutput"))
             .putMember("RevisionId", Schemas.POLICY_REVISION_ID)
             .builderSupplier(DeleteResourcePolicyOutput::builder)
             .shapeClass(DeleteResourcePolicyOutput.class)
             .build();

    static final Schema POLICY_NOT_FOUND_EXCEPTION = Schema.structureBuilder(ShapeId.from("com.amazonaws.dynamodb#PolicyNotFoundException"),
            new ErrorTrait("client"))
             .putMember("message", Schemas.ERROR_MESSAGE)
             .builderSupplier(PolicyNotFoundException::builder)
             .shapeClass(PolicyNotFoundException.class)
             .build();

    static final Schema DELETE_TABLE_INPUT = Schema.structureBuilder(ShapeId.from("com.amazonaws.dynamodb#DeleteTableInput"))
             .putMember("TableName", Schemas.TABLE_ARN,
                     new ContextParamTrait.Provider().createTrait(
                         ShapeId.from("smithy.rules#contextParam"),
                         Node.objectNodeBuilder()
                             .withMember("name", "ResourceArn")
                             .build()
                     ),
                     new RequiredTrait())
             .builderSupplier(DeleteTableInput::builder)
             .shapeClass(DeleteTableInput.class)
             .build();

    static final Schema DELETE_TABLE_OUTPUT = Schema.structureBuilder(ShapeId.from("com.amazonaws.dynamodb#DeleteTableOutput"))
             .putMember("TableDescription", Schemas.TABLE_DESCRIPTION)
             .builderSupplier(DeleteTableOutput::builder)
             .shapeClass(DeleteTableOutput.class)
             .build();

    static final Schema DELETE_VECTOR_INDEX_ACTION = Schema.structureBuilder(ShapeId.from("com.amazonaws.dynamodb#DeleteVectorIndexAction"))
             .putMember("IndexName", Schemas.INDEX_NAME,
                     new RequiredTrait())
             .builderSupplier(DeleteVectorIndexAction::builder)
             .shapeClass(DeleteVectorIndexAction.class)
             .build();

    static final Schema DESCRIBE_BACKUP_INPUT = Schema.structureBuilder(ShapeId.from("com.amazonaws.dynamodb#DescribeBackupInput"))
             .putMember("BackupArn", Schemas.BACKUP_ARN,
                     new ContextParamTrait.Provider().createTrait(
                         ShapeId.from("smithy.rules#contextParam"),
                         Node.objectNodeBuilder()
                             .withMember("name", "ResourceArn")
                             .build()
                     ),
                     new RequiredTrait())
             .builderSupplier(DescribeBackupInput::builder)
             .shapeClass(DescribeBackupInput.class)
             .build();

    static final Schema DESCRIBE_BACKUP_OUTPUT = Schema.structureBuilder(ShapeId.from("com.amazonaws.dynamodb#DescribeBackupOutput"))
             .putMember("BackupDescription", Schemas.BACKUP_DESCRIPTION)
             .builderSupplier(DescribeBackupOutput::builder)
             .shapeClass(DescribeBackupOutput.class)
             .build();

    static final Schema DESCRIBE_CONTINUOUS_BACKUPS_INPUT = Schema.structureBuilder(ShapeId.from("com.amazonaws.dynamodb#DescribeContinuousBackupsInput"))
             .putMember("TableName", Schemas.TABLE_ARN,
                     new ContextParamTrait.Provider().createTrait(
                         ShapeId.from("smithy.rules#contextParam"),
                         Node.objectNodeBuilder()
                             .withMember("name", "ResourceArn")
                             .build()
                     ),
                     new RequiredTrait())
             .builderSupplier(DescribeContinuousBackupsInput::builder)
             .shapeClass(DescribeContinuousBackupsInput.class)
             .build();

    static final Schema DESCRIBE_CONTINUOUS_BACKUPS_OUTPUT = Schema.structureBuilder(ShapeId.from("com.amazonaws.dynamodb#DescribeContinuousBackupsOutput"))
             .putMember("ContinuousBackupsDescription", Schemas.CONTINUOUS_BACKUPS_DESCRIPTION)
             .builderSupplier(DescribeContinuousBackupsOutput::builder)
             .shapeClass(DescribeContinuousBackupsOutput.class)
             .build();

    static final Schema DESCRIBE_CONTRIBUTOR_INSIGHTS_INPUT = Schema.structureBuilder(ShapeId.from("com.amazonaws.dynamodb#DescribeContributorInsightsInput"))
             .putMember("TableName", Schemas.TABLE_ARN,
                     new ContextParamTrait.Provider().createTrait(
                         ShapeId.from("smithy.rules#contextParam"),
                         Node.objectNodeBuilder()
                             .withMember("name", "ResourceArn")
                             .build()
                     ),
                     new RequiredTrait())
             .putMember("IndexName", Schemas.INDEX_NAME)
             .builderSupplier(DescribeContributorInsightsInput::builder)
             .shapeClass(DescribeContributorInsightsInput.class)
             .build();

    static final Schema EXCEPTION_DESCRIPTION = Schema.createString(ShapeId.from("com.amazonaws.dynamodb#ExceptionDescription"));
    static final Schema EXCEPTION_NAME = Schema.createString(ShapeId.from("com.amazonaws.dynamodb#ExceptionName"));
    static final Schema FAILURE_EXCEPTION = Schema.structureBuilder(ShapeId.from("com.amazonaws.dynamodb#FailureException"))
             .putMember("ExceptionName", Schemas.EXCEPTION_NAME)
             .putMember("ExceptionDescription", Schemas.EXCEPTION_DESCRIPTION)
             .builderSupplier(FailureException::builder)
             .shapeClass(FailureException.class)
             .build();

    static final Schema LAST_UPDATE_DATE_TIME = Schema.createTimestamp(ShapeId.from("com.amazonaws.dynamodb#LastUpdateDateTime"));
    static final Schema DESCRIBE_CONTRIBUTOR_INSIGHTS_OUTPUT = Schema.structureBuilder(ShapeId.from("com.amazonaws.dynamodb#DescribeContributorInsightsOutput"))
             .putMember("TableName", Schemas.TABLE_NAME)
             .putMember("IndexName", Schemas.INDEX_NAME)
             .putMember("ContributorInsightsRuleList", Schemas.CONTRIBUTOR_INSIGHTS_RULE_LIST)
             .putMember("ContributorInsightsStatus", ContributorInsightsStatus.$SCHEMA)
             .putMember("LastUpdateDateTime", Schemas.LAST_UPDATE_DATE_TIME)
             .putMember("FailureException", Schemas.FAILURE_EXCEPTION)
             .putMember("ContributorInsightsMode", ContributorInsightsMode.$SCHEMA)
             .builderSupplier(DescribeContributorInsightsOutput::builder)
             .shapeClass(DescribeContributorInsightsOutput.class)
             .build();

    static final Schema DESCRIBE_ENDPOINTS_INPUT = Schema.structureBuilder(ShapeId.from("com.amazonaws.dynamodb#DescribeEndpointsRequest")).builderSupplier(DescribeEndpointsInput::builder)
             .shapeClass(DescribeEndpointsInput.class)
             .build();

    static final Schema LONG = Schema.createLong(ShapeId.from("com.amazonaws.dynamodb#Long"),
            new DefaultTrait(Node.from(0L)));
    static final Schema ENDPOINT = Schema.structureBuilder(ShapeId.from("com.amazonaws.dynamodb#Endpoint"))
             .putMember("Address", Schemas.STRING,
                     new RequiredTrait())
             .putMember("CachePeriodInMinutes", Schemas.LONG,
                     new DefaultTrait(Node.from(0L)),
                     new RequiredTrait())
             .builderSupplier(Endpoint::builder)
             .shapeClass(Endpoint.class)
             .build();

    static final Schema ENDPOINTS = Schema.listBuilder(ShapeId.from("com.amazonaws.dynamodb#Endpoints"))
        .putMember("member", Schemas.ENDPOINT)
        .build();

    static final Schema DESCRIBE_ENDPOINTS_OUTPUT = Schema.structureBuilder(ShapeId.from("com.amazonaws.dynamodb#DescribeEndpointsResponse"))
             .putMember("Endpoints", Schemas.ENDPOINTS,
                     new RequiredTrait())
             .builderSupplier(DescribeEndpointsOutput::builder)
             .shapeClass(DescribeEndpointsOutput.class)
             .build();

    static final Schema EXPORT_ARN = Schema.createString(ShapeId.from("com.amazonaws.dynamodb#ExportArn"),
            LengthTrait.builder().min(37L).max(1024L).build());
    static final Schema DESCRIBE_EXPORT_INPUT = Schema.structureBuilder(ShapeId.from("com.amazonaws.dynamodb#DescribeExportInput"))
             .putMember("ExportArn", Schemas.EXPORT_ARN,
                     new ContextParamTrait.Provider().createTrait(
                         ShapeId.from("smithy.rules#contextParam"),
                         Node.objectNodeBuilder()
                             .withMember("name", "ResourceArn")
                             .build()
                     ),
                     new RequiredTrait())
             .builderSupplier(DescribeExportInput::builder)
             .shapeClass(DescribeExportInput.class)
             .build();

    static final Schema EXPORT_END_TIME = Schema.createTimestamp(ShapeId.from("com.amazonaws.dynamodb#ExportEndTime"));
    static final Schema EXPORT_MANIFEST = Schema.createString(ShapeId.from("com.amazonaws.dynamodb#ExportManifest"));
    static final Schema EXPORT_TIME = Schema.createTimestamp(ShapeId.from("com.amazonaws.dynamodb#ExportTime"));
    static final Schema FAILURE_CODE = Schema.createString(ShapeId.from("com.amazonaws.dynamodb#FailureCode"));
    static final Schema FAILURE_MESSAGE = Schema.createString(ShapeId.from("com.amazonaws.dynamodb#FailureMessage"));
    static final Schema EXPORT_FROM_TIME = Schema.createTimestamp(ShapeId.from("com.amazonaws.dynamodb#ExportFromTime"));
    static final Schema EXPORT_TO_TIME = Schema.createTimestamp(ShapeId.from("com.amazonaws.dynamodb#ExportToTime"));
    static final Schema INCREMENTAL_EXPORT_SPECIFICATION = Schema.structureBuilder(ShapeId.from("com.amazonaws.dynamodb#IncrementalExportSpecification"))
             .putMember("ExportFromTime", Schemas.EXPORT_FROM_TIME)
             .putMember("ExportToTime", Schemas.EXPORT_TO_TIME)
             .putMember("ExportViewType", ExportViewType.$SCHEMA)
             .builderSupplier(IncrementalExportSpecification::builder)
             .shapeClass(IncrementalExportSpecification.class)
             .build();

    static final Schema S3_BUCKET = Schema.createString(ShapeId.from("com.amazonaws.dynamodb#S3Bucket"),
            LengthTrait.builder().min(0L).max(255L).build(),
            new PatternTrait("^[a-z0-9A-Z]+[\\.\\-\\w]*[a-z0-9A-Z]+$"));
    static final Schema S3_BUCKET_OWNER = Schema.createString(ShapeId.from("com.amazonaws.dynamodb#S3BucketOwner"),
            new PatternTrait("^[0-9]{12}$"));
    static final Schema S3_PREFIX = Schema.createString(ShapeId.from("com.amazonaws.dynamodb#S3Prefix"),
            LengthTrait.builder().min(0L).max(1024L).build());
    static final Schema S3_SSE_KMS_KEY_ID = Schema.createString(ShapeId.from("com.amazonaws.dynamodb#S3SseKmsKeyId"),
            LengthTrait.builder().min(1L).max(2048L).build());
    static final Schema EXPORT_START_TIME = Schema.createTimestamp(ShapeId.from("com.amazonaws.dynamodb#ExportStartTime"));
    static final Schema EXPORT_DESCRIPTION = Schema.structureBuilder(ShapeId.from("com.amazonaws.dynamodb#ExportDescription"))
             .putMember("ExportArn", Schemas.EXPORT_ARN)
             .putMember("ExportStatus", ExportStatus.$SCHEMA)
             .putMember("StartTime", Schemas.EXPORT_START_TIME)
             .putMember("EndTime", Schemas.EXPORT_END_TIME)
             .putMember("ExportManifest", Schemas.EXPORT_MANIFEST)
             .putMember("TableArn", Schemas.TABLE_ARN)
             .putMember("TableId", Schemas.TABLE_ID)
             .putMember("ExportTime", Schemas.EXPORT_TIME)
             .putMember("ClientToken", Schemas.CLIENT_TOKEN)
             .putMember("S3Bucket", Schemas.S3_BUCKET)
             .putMember("S3BucketOwner", Schemas.S3_BUCKET_OWNER)
             .putMember("S3Prefix", Schemas.S3_PREFIX)
             .putMember("S3SseAlgorithm", S3SseAlgorithm.$SCHEMA)
             .putMember("S3SseKmsKeyId", Schemas.S3_SSE_KMS_KEY_ID)
             .putMember("FailureCode", Schemas.FAILURE_CODE)
             .putMember("FailureMessage", Schemas.FAILURE_MESSAGE)
             .putMember("ExportFormat", ExportFormat.$SCHEMA)
             .putMember("BilledSizeBytes", Schemas.BILLED_SIZE_BYTES)
             .putMember("ItemCount", Schemas.ITEM_COUNT)
             .putMember("ExportType", ExportType.$SCHEMA)
             .putMember("IncrementalExportSpecification", Schemas.INCREMENTAL_EXPORT_SPECIFICATION)
             .builderSupplier(ExportDescription::builder)
             .shapeClass(ExportDescription.class)
             .build();

    static final Schema DESCRIBE_EXPORT_OUTPUT = Schema.structureBuilder(ShapeId.from("com.amazonaws.dynamodb#DescribeExportOutput"))
             .putMember("ExportDescription", Schemas.EXPORT_DESCRIPTION)
             .builderSupplier(DescribeExportOutput::builder)
             .shapeClass(DescribeExportOutput.class)
             .build();

    static final Schema EXPORT_NOT_FOUND_EXCEPTION = Schema.structureBuilder(ShapeId.from("com.amazonaws.dynamodb#ExportNotFoundException"),
            new ErrorTrait("client"))
             .putMember("message", Schemas.ERROR_MESSAGE)
             .builderSupplier(ExportNotFoundException::builder)
             .shapeClass(ExportNotFoundException.class)
             .build();

    static final Schema DESCRIBE_GLOBAL_TABLE_INPUT = Schema.structureBuilder(ShapeId.from("com.amazonaws.dynamodb#DescribeGlobalTableInput"))
             .putMember("GlobalTableName", Schemas.TABLE_NAME,
                     new ContextParamTrait.Provider().createTrait(
                         ShapeId.from("smithy.rules#contextParam"),
                         Node.objectNodeBuilder()
                             .withMember("name", "ResourceArn")
                             .build()
                     ),
                     new RequiredTrait())
             .builderSupplier(DescribeGlobalTableInput::builder)
             .shapeClass(DescribeGlobalTableInput.class)
             .build();

    static final Schema DESCRIBE_GLOBAL_TABLE_OUTPUT = Schema.structureBuilder(ShapeId.from("com.amazonaws.dynamodb#DescribeGlobalTableOutput"))
             .putMember("GlobalTableDescription", Schemas.GLOBAL_TABLE_DESCRIPTION)
             .builderSupplier(DescribeGlobalTableOutput::builder)
             .shapeClass(DescribeGlobalTableOutput.class)
             .build();

    static final Schema GLOBAL_TABLE_NOT_FOUND_EXCEPTION = Schema.structureBuilder(ShapeId.from("com.amazonaws.dynamodb#GlobalTableNotFoundException"),
            new ErrorTrait("client"))
             .putMember("message", Schemas.ERROR_MESSAGE)
             .builderSupplier(GlobalTableNotFoundException::builder)
             .shapeClass(GlobalTableNotFoundException.class)
             .build();

    static final Schema DESCRIBE_GLOBAL_TABLE_SETTINGS_INPUT = Schema.structureBuilder(ShapeId.from("com.amazonaws.dynamodb#DescribeGlobalTableSettingsInput"))
             .putMember("GlobalTableName", Schemas.TABLE_NAME,
                     new ContextParamTrait.Provider().createTrait(
                         ShapeId.from("smithy.rules#contextParam"),
                         Node.objectNodeBuilder()
                             .withMember("name", "ResourceArn")
                             .build()
                     ),
                     new RequiredTrait())
             .builderSupplier(DescribeGlobalTableSettingsInput::builder)
             .shapeClass(DescribeGlobalTableSettingsInput.class)
             .build();

    static final Schema REPLICA_GLOBAL_SECONDARY_INDEX_SETTINGS_DESCRIPTION = Schema.structureBuilder(ShapeId.from("com.amazonaws.dynamodb#ReplicaGlobalSecondaryIndexSettingsDescription"))
             .putMember("IndexName", Schemas.INDEX_NAME,
                     new RequiredTrait())
             .putMember("IndexStatus", IndexStatus.$SCHEMA)
             .putMember("ProvisionedReadCapacityUnits", Schemas.POSITIVE_LONG_OBJECT)
             .putMember("ProvisionedReadCapacityAutoScalingSettings", Schemas.AUTO_SCALING_SETTINGS_DESCRIPTION)
             .putMember("ProvisionedWriteCapacityUnits", Schemas.POSITIVE_LONG_OBJECT)
             .putMember("ProvisionedWriteCapacityAutoScalingSettings", Schemas.AUTO_SCALING_SETTINGS_DESCRIPTION)
             .builderSupplier(ReplicaGlobalSecondaryIndexSettingsDescription::builder)
             .shapeClass(ReplicaGlobalSecondaryIndexSettingsDescription.class)
             .build();

    static final Schema REPLICA_GLOBAL_SECONDARY_INDEX_SETTINGS_DESCRIPTION_LIST = Schema.listBuilder(ShapeId.from("com.amazonaws.dynamodb#ReplicaGlobalSecondaryIndexSettingsDescriptionList"))
        .putMember("member", Schemas.REPLICA_GLOBAL_SECONDARY_INDEX_SETTINGS_DESCRIPTION)
        .build();

    static final Schema REPLICA_SETTINGS_DESCRIPTION = Schema.structureBuilder(ShapeId.from("com.amazonaws.dynamodb#ReplicaSettingsDescription"))
             .putMember("RegionName", Schemas.REGION_NAME,
                     new RequiredTrait())
             .putMember("ReplicaStatus", ReplicaStatus.$SCHEMA)
             .putMember("ReplicaBillingModeSummary", Schemas.BILLING_MODE_SUMMARY)
             .putMember("ReplicaProvisionedReadCapacityUnits", Schemas.NON_NEGATIVE_LONG_OBJECT)
             .putMember("ReplicaProvisionedReadCapacityAutoScalingSettings", Schemas.AUTO_SCALING_SETTINGS_DESCRIPTION)
             .putMember("ReplicaProvisionedWriteCapacityUnits", Schemas.NON_NEGATIVE_LONG_OBJECT)
             .putMember("ReplicaProvisionedWriteCapacityAutoScalingSettings", Schemas.AUTO_SCALING_SETTINGS_DESCRIPTION)
             .putMember("ReplicaGlobalSecondaryIndexSettings", Schemas.REPLICA_GLOBAL_SECONDARY_INDEX_SETTINGS_DESCRIPTION_LIST)
             .putMember("ReplicaTableClassSummary", Schemas.TABLE_CLASS_SUMMARY)
             .builderSupplier(ReplicaSettingsDescription::builder)
             .shapeClass(ReplicaSettingsDescription.class)
             .build();

    static final Schema REPLICA_SETTINGS_DESCRIPTION_LIST = Schema.listBuilder(ShapeId.from("com.amazonaws.dynamodb#ReplicaSettingsDescriptionList"))
        .putMember("member", Schemas.REPLICA_SETTINGS_DESCRIPTION)
        .build();

    static final Schema DESCRIBE_GLOBAL_TABLE_SETTINGS_OUTPUT = Schema.structureBuilder(ShapeId.from("com.amazonaws.dynamodb#DescribeGlobalTableSettingsOutput"))
             .putMember("GlobalTableName", Schemas.TABLE_NAME)
             .putMember("ReplicaSettings", Schemas.REPLICA_SETTINGS_DESCRIPTION_LIST)
             .builderSupplier(DescribeGlobalTableSettingsOutput::builder)
             .shapeClass(DescribeGlobalTableSettingsOutput.class)
             .build();

    static final Schema IMPORT_ARN = Schema.createString(ShapeId.from("com.amazonaws.dynamodb#ImportArn"),
            LengthTrait.builder().min(37L).max(1024L).build());
    static final Schema DESCRIBE_IMPORT_INPUT = Schema.structureBuilder(ShapeId.from("com.amazonaws.dynamodb#DescribeImportInput"))
             .putMember("ImportArn", Schemas.IMPORT_ARN,
                     new ContextParamTrait.Provider().createTrait(
                         ShapeId.from("smithy.rules#contextParam"),
                         Node.objectNodeBuilder()
                             .withMember("name", "ResourceArn")
                             .build()
                     ),
                     new RequiredTrait())
             .builderSupplier(DescribeImportInput::builder)
             .shapeClass(DescribeImportInput.class)
             .build();

    static final Schema IMPORT_END_TIME = Schema.createTimestamp(ShapeId.from("com.amazonaws.dynamodb#ImportEndTime"));
    static final Schema ERROR_COUNT = Schema.createLong(ShapeId.from("com.amazonaws.dynamodb#ErrorCount"),
            new DefaultTrait(Node.from(0L)),
            RangeTrait.builder().min(new BigDecimal("0")).build());
    static final Schema IMPORTED_ITEM_COUNT = Schema.createLong(ShapeId.from("com.amazonaws.dynamodb#ImportedItemCount"),
            new DefaultTrait(Node.from(0L)),
            RangeTrait.builder().min(new BigDecimal("0")).build());
    static final Schema INPUT_FORMAT_OPTIONS = Schema.structureBuilder(ShapeId.from("com.amazonaws.dynamodb#InputFormatOptions"))
             .putMember("Csv", Schemas.CSV_OPTIONS)
             .builderSupplier(InputFormatOptions::builder)
             .shapeClass(InputFormatOptions.class)
             .build();

    static final Schema PROCESSED_ITEM_COUNT = Schema.createLong(ShapeId.from("com.amazonaws.dynamodb#ProcessedItemCount"),
            new DefaultTrait(Node.from(0L)),
            RangeTrait.builder().min(new BigDecimal("0")).build());
    static final Schema S3_BUCKET_SOURCE = Schema.structureBuilder(ShapeId.from("com.amazonaws.dynamodb#S3BucketSource"))
             .putMember("S3BucketOwner", Schemas.S3_BUCKET_OWNER)
             .putMember("S3Bucket", Schemas.S3_BUCKET,
                     new RequiredTrait())
             .putMember("S3KeyPrefix", Schemas.S3_PREFIX)
             .builderSupplier(S3BucketSource::builder)
             .shapeClass(S3BucketSource.class)
             .build();

    static final Schema IMPORT_START_TIME = Schema.createTimestamp(ShapeId.from("com.amazonaws.dynamodb#ImportStartTime"));
    static final Schema TABLE_CREATION_PARAMETERS = Schema.structureBuilder(ShapeId.from("com.amazonaws.dynamodb#TableCreationParameters"))
             .putMember("TableName", Schemas.TABLE_NAME,
                     new RequiredTrait())
             .putMember("AttributeDefinitions", Schemas.ATTRIBUTE_DEFINITIONS,
                     new RequiredTrait())
             .putMember("KeySchema", Schemas.KEY_SCHEMA,
                     new RequiredTrait())
             .putMember("BillingMode", BillingMode.$SCHEMA)
             .putMember("ProvisionedThroughput", Schemas.PROVISIONED_THROUGHPUT)
             .putMember("OnDemandThroughput", Schemas.ON_DEMAND_THROUGHPUT)
             .putMember("SSESpecification", Schemas.SSE_SPECIFICATION)
             .putMember("GlobalSecondaryIndexes", Schemas.GLOBAL_SECONDARY_INDEX_LIST)
             .putMember("VectorIndexes", Schemas.VECTOR_INDEX_LIST)
             .builderSupplier(TableCreationParameters::builder)
             .shapeClass(TableCreationParameters.class)
             .build();

    static final Schema IMPORT_TABLE_DESCRIPTION = Schema.structureBuilder(ShapeId.from("com.amazonaws.dynamodb#ImportTableDescription"))
             .putMember("ImportArn", Schemas.IMPORT_ARN)
             .putMember("ImportStatus", ImportStatus.$SCHEMA)
             .putMember("TableArn", Schemas.TABLE_ARN)
             .putMember("TableId", Schemas.TABLE_ID)
             .putMember("ClientToken", Schemas.CLIENT_TOKEN)
             .putMember("S3BucketSource", Schemas.S3_BUCKET_SOURCE)
             .putMember("ErrorCount", Schemas.ERROR_COUNT,
                     new DefaultTrait(Node.from(0L)))
             .putMember("CloudWatchLogGroupArn", Schemas.CLOUD_WATCH_LOG_GROUP_ARN)
             .putMember("InputFormat", InputFormat.$SCHEMA)
             .putMember("InputFormatOptions", Schemas.INPUT_FORMAT_OPTIONS)
             .putMember("InputCompressionType", InputCompressionType.$SCHEMA)
             .putMember("TableCreationParameters", Schemas.TABLE_CREATION_PARAMETERS)
             .putMember("StartTime", Schemas.IMPORT_START_TIME)
             .putMember("EndTime", Schemas.IMPORT_END_TIME)
             .putMember("ProcessedSizeBytes", Schemas.LONG_OBJECT)
             .putMember("ProcessedItemCount", Schemas.PROCESSED_ITEM_COUNT,
                     new DefaultTrait(Node.from(0L)))
             .putMember("ImportedItemCount", Schemas.IMPORTED_ITEM_COUNT,
                     new DefaultTrait(Node.from(0L)))
             .putMember("FailureCode", Schemas.FAILURE_CODE)
             .putMember("FailureMessage", Schemas.FAILURE_MESSAGE)
             .builderSupplier(ImportTableDescription::builder)
             .shapeClass(ImportTableDescription.class)
             .build();

    static final Schema DESCRIBE_IMPORT_OUTPUT = Schema.structureBuilder(ShapeId.from("com.amazonaws.dynamodb#DescribeImportOutput"))
             .putMember("ImportTableDescription", Schemas.IMPORT_TABLE_DESCRIPTION,
                     new RequiredTrait())
             .builderSupplier(DescribeImportOutput::builder)
             .shapeClass(DescribeImportOutput.class)
             .build();

    static final Schema IMPORT_NOT_FOUND_EXCEPTION = Schema.structureBuilder(ShapeId.from("com.amazonaws.dynamodb#ImportNotFoundException"),
            new ErrorTrait("client"))
             .putMember("message", Schemas.ERROR_MESSAGE)
             .builderSupplier(ImportNotFoundException::builder)
             .shapeClass(ImportNotFoundException.class)
             .build();

    static final Schema DESCRIBE_KINESIS_STREAMING_DESTINATION_INPUT = Schema.structureBuilder(ShapeId.from("com.amazonaws.dynamodb#DescribeKinesisStreamingDestinationInput"))
             .putMember("TableName", Schemas.TABLE_ARN,
                     new ContextParamTrait.Provider().createTrait(
                         ShapeId.from("smithy.rules#contextParam"),
                         Node.objectNodeBuilder()
                             .withMember("name", "ResourceArn")
                             .build()
                     ),
                     new RequiredTrait())
             .builderSupplier(DescribeKinesisStreamingDestinationInput::builder)
             .shapeClass(DescribeKinesisStreamingDestinationInput.class)
             .build();

    static final Schema KINESIS_DATA_STREAM_DESTINATION = Schema.structureBuilder(ShapeId.from("com.amazonaws.dynamodb#KinesisDataStreamDestination"))
             .putMember("StreamArn", Schemas.STREAM_ARN)
             .putMember("DestinationStatus", DestinationStatus.$SCHEMA)
             .putMember("DestinationStatusDescription", Schemas.STRING)
             .putMember("ApproximateCreationDateTimePrecision", ApproximateCreationDateTimePrecision.$SCHEMA)
             .builderSupplier(KinesisDataStreamDestination::builder)
             .shapeClass(KinesisDataStreamDestination.class)
             .build();

    static final Schema KINESIS_DATA_STREAM_DESTINATIONS = Schema.listBuilder(ShapeId.from("com.amazonaws.dynamodb#KinesisDataStreamDestinations"))
        .putMember("member", Schemas.KINESIS_DATA_STREAM_DESTINATION)
        .build();

    static final Schema DESCRIBE_KINESIS_STREAMING_DESTINATION_OUTPUT = Schema.structureBuilder(ShapeId.from("com.amazonaws.dynamodb#DescribeKinesisStreamingDestinationOutput"))
             .putMember("TableName", Schemas.TABLE_NAME)
             .putMember("KinesisDataStreamDestinations", Schemas.KINESIS_DATA_STREAM_DESTINATIONS)
             .builderSupplier(DescribeKinesisStreamingDestinationOutput::builder)
             .shapeClass(DescribeKinesisStreamingDestinationOutput.class)
             .build();

    static final Schema DESCRIBE_LIMITS_INPUT = Schema.structureBuilder(ShapeId.from("com.amazonaws.dynamodb#DescribeLimitsInput")).builderSupplier(DescribeLimitsInput::builder)
             .shapeClass(DescribeLimitsInput.class)
             .build();

    static final Schema DESCRIBE_LIMITS_OUTPUT = Schema.structureBuilder(ShapeId.from("com.amazonaws.dynamodb#DescribeLimitsOutput"))
             .putMember("AccountMaxReadCapacityUnits", Schemas.POSITIVE_LONG_OBJECT)
             .putMember("AccountMaxWriteCapacityUnits", Schemas.POSITIVE_LONG_OBJECT)
             .putMember("TableMaxReadCapacityUnits", Schemas.POSITIVE_LONG_OBJECT)
             .putMember("TableMaxWriteCapacityUnits", Schemas.POSITIVE_LONG_OBJECT)
             .builderSupplier(DescribeLimitsOutput::builder)
             .shapeClass(DescribeLimitsOutput.class)
             .build();

    static final Schema DESCRIBE_TABLE_INPUT = Schema.structureBuilder(ShapeId.from("com.amazonaws.dynamodb#DescribeTableInput"))
             .putMember("TableName", Schemas.TABLE_ARN,
                     new ContextParamTrait.Provider().createTrait(
                         ShapeId.from("smithy.rules#contextParam"),
                         Node.objectNodeBuilder()
                             .withMember("name", "ResourceArn")
                             .build()
                     ),
                     new RequiredTrait())
             .builderSupplier(DescribeTableInput::builder)
             .shapeClass(DescribeTableInput.class)
             .build();

    static final Schema DESCRIBE_TABLE_OUTPUT = Schema.structureBuilder(ShapeId.from("com.amazonaws.dynamodb#DescribeTableOutput"))
             .putMember("Table", Schemas.TABLE_DESCRIPTION)
             .builderSupplier(DescribeTableOutput::builder)
             .shapeClass(DescribeTableOutput.class)
             .build();

    static final Schema DESCRIBE_TABLE_REPLICA_AUTO_SCALING_INPUT = Schema.structureBuilder(ShapeId.from("com.amazonaws.dynamodb#DescribeTableReplicaAutoScalingInput"))
             .putMember("TableName", Schemas.TABLE_ARN,
                     new ContextParamTrait.Provider().createTrait(
                         ShapeId.from("smithy.rules#contextParam"),
                         Node.objectNodeBuilder()
                             .withMember("name", "ResourceArn")
                             .build()
                     ),
                     new RequiredTrait())
             .builderSupplier(DescribeTableReplicaAutoScalingInput::builder)
             .shapeClass(DescribeTableReplicaAutoScalingInput.class)
             .build();

    static final Schema REPLICA_GLOBAL_SECONDARY_INDEX_AUTO_SCALING_DESCRIPTION = Schema.structureBuilder(ShapeId.from("com.amazonaws.dynamodb#ReplicaGlobalSecondaryIndexAutoScalingDescription"))
             .putMember("IndexName", Schemas.INDEX_NAME)
             .putMember("IndexStatus", IndexStatus.$SCHEMA)
             .putMember("ProvisionedReadCapacityAutoScalingSettings", Schemas.AUTO_SCALING_SETTINGS_DESCRIPTION)
             .putMember("ProvisionedWriteCapacityAutoScalingSettings", Schemas.AUTO_SCALING_SETTINGS_DESCRIPTION)
             .builderSupplier(ReplicaGlobalSecondaryIndexAutoScalingDescription::builder)
             .shapeClass(ReplicaGlobalSecondaryIndexAutoScalingDescription.class)
             .build();

    static final Schema REPLICA_GLOBAL_SECONDARY_INDEX_AUTO_SCALING_DESCRIPTION_LIST = Schema.listBuilder(ShapeId.from("com.amazonaws.dynamodb#ReplicaGlobalSecondaryIndexAutoScalingDescriptionList"))
        .putMember("member", Schemas.REPLICA_GLOBAL_SECONDARY_INDEX_AUTO_SCALING_DESCRIPTION)
        .build();

    static final Schema REPLICA_AUTO_SCALING_DESCRIPTION = Schema.structureBuilder(ShapeId.from("com.amazonaws.dynamodb#ReplicaAutoScalingDescription"))
             .putMember("RegionName", Schemas.REGION_NAME)
             .putMember("GlobalSecondaryIndexes", Schemas.REPLICA_GLOBAL_SECONDARY_INDEX_AUTO_SCALING_DESCRIPTION_LIST)
             .putMember("ReplicaProvisionedReadCapacityAutoScalingSettings", Schemas.AUTO_SCALING_SETTINGS_DESCRIPTION)
             .putMember("ReplicaProvisionedWriteCapacityAutoScalingSettings", Schemas.AUTO_SCALING_SETTINGS_DESCRIPTION)
             .putMember("ReplicaStatus", ReplicaStatus.$SCHEMA)
             .builderSupplier(ReplicaAutoScalingDescription::builder)
             .shapeClass(ReplicaAutoScalingDescription.class)
             .build();

    static final Schema REPLICA_AUTO_SCALING_DESCRIPTION_LIST = Schema.listBuilder(ShapeId.from("com.amazonaws.dynamodb#ReplicaAutoScalingDescriptionList"))
        .putMember("member", Schemas.REPLICA_AUTO_SCALING_DESCRIPTION)
        .build();

    static final Schema TABLE_AUTO_SCALING_DESCRIPTION = Schema.structureBuilder(ShapeId.from("com.amazonaws.dynamodb#TableAutoScalingDescription"))
             .putMember("TableName", Schemas.TABLE_NAME)
             .putMember("TableStatus", TableStatus.$SCHEMA)
             .putMember("Replicas", Schemas.REPLICA_AUTO_SCALING_DESCRIPTION_LIST)
             .builderSupplier(TableAutoScalingDescription::builder)
             .shapeClass(TableAutoScalingDescription.class)
             .build();

    static final Schema DESCRIBE_TABLE_REPLICA_AUTO_SCALING_OUTPUT = Schema.structureBuilder(ShapeId.from("com.amazonaws.dynamodb#DescribeTableReplicaAutoScalingOutput"))
             .putMember("TableAutoScalingDescription", Schemas.TABLE_AUTO_SCALING_DESCRIPTION)
             .builderSupplier(DescribeTableReplicaAutoScalingOutput::builder)
             .shapeClass(DescribeTableReplicaAutoScalingOutput.class)
             .build();

    static final Schema DESCRIBE_TIME_TO_LIVE_INPUT = Schema.structureBuilder(ShapeId.from("com.amazonaws.dynamodb#DescribeTimeToLiveInput"))
             .putMember("TableName", Schemas.TABLE_ARN,
                     new ContextParamTrait.Provider().createTrait(
                         ShapeId.from("smithy.rules#contextParam"),
                         Node.objectNodeBuilder()
                             .withMember("name", "ResourceArn")
                             .build()
                     ),
                     new RequiredTrait())
             .builderSupplier(DescribeTimeToLiveInput::builder)
             .shapeClass(DescribeTimeToLiveInput.class)
             .build();

    static final Schema DESCRIBE_TIME_TO_LIVE_OUTPUT = Schema.structureBuilder(ShapeId.from("com.amazonaws.dynamodb#DescribeTimeToLiveOutput"))
             .putMember("TimeToLiveDescription", Schemas.TIME_TO_LIVE_DESCRIPTION)
             .builderSupplier(DescribeTimeToLiveOutput::builder)
             .shapeClass(DescribeTimeToLiveOutput.class)
             .build();

    static final Schema ENABLE_KINESIS_STREAMING_CONFIGURATION = Schema.structureBuilder(ShapeId.from("com.amazonaws.dynamodb#EnableKinesisStreamingConfiguration"))
             .putMember("ApproximateCreationDateTimePrecision", ApproximateCreationDateTimePrecision.$SCHEMA)
             .builderSupplier(EnableKinesisStreamingConfiguration::builder)
             .shapeClass(EnableKinesisStreamingConfiguration.class)
             .build();

    static final Schema DISABLE_KINESIS_STREAMING_DESTINATION_INPUT = Schema.structureBuilder(ShapeId.from("com.amazonaws.dynamodb#KinesisStreamingDestinationInput"))
             .putMember("TableName", Schemas.TABLE_ARN,
                     new ContextParamTrait.Provider().createTrait(
                         ShapeId.from("smithy.rules#contextParam"),
                         Node.objectNodeBuilder()
                             .withMember("name", "ResourceArn")
                             .build()
                     ),
                     new RequiredTrait())
             .putMember("StreamArn", Schemas.STREAM_ARN,
                     new RequiredTrait())
             .putMember("EnableKinesisStreamingConfiguration", Schemas.ENABLE_KINESIS_STREAMING_CONFIGURATION)
             .builderSupplier(DisableKinesisStreamingDestinationInput::builder)
             .shapeClass(DisableKinesisStreamingDestinationInput.class)
             .build();

    static final Schema DISABLE_KINESIS_STREAMING_DESTINATION_OUTPUT = Schema.structureBuilder(ShapeId.from("com.amazonaws.dynamodb#KinesisStreamingDestinationOutput"))
             .putMember("TableName", Schemas.TABLE_NAME)
             .putMember("StreamArn", Schemas.STREAM_ARN)
             .putMember("DestinationStatus", DestinationStatus.$SCHEMA)
             .putMember("EnableKinesisStreamingConfiguration", Schemas.ENABLE_KINESIS_STREAMING_CONFIGURATION)
             .builderSupplier(DisableKinesisStreamingDestinationOutput::builder)
             .shapeClass(DisableKinesisStreamingDestinationOutput.class)
             .build();

    static final Schema DUPLICATE_ITEM_EXCEPTION = Schema.structureBuilder(ShapeId.from("com.amazonaws.dynamodb#DuplicateItemException"),
            new ErrorTrait("client"))
             .putMember("message", Schemas.ERROR_MESSAGE)
             .builderSupplier(DuplicateItemException::builder)
             .shapeClass(DuplicateItemException.class)
             .build();

    static final Schema ENABLE_KINESIS_STREAMING_DESTINATION_INPUT = Schema.structureBuilder(ShapeId.from("com.amazonaws.dynamodb#KinesisStreamingDestinationInput"))
             .putMember("TableName", Schemas.TABLE_ARN,
                     new ContextParamTrait.Provider().createTrait(
                         ShapeId.from("smithy.rules#contextParam"),
                         Node.objectNodeBuilder()
                             .withMember("name", "ResourceArn")
                             .build()
                     ),
                     new RequiredTrait())
             .putMember("StreamArn", Schemas.STREAM_ARN,
                     new RequiredTrait())
             .putMember("EnableKinesisStreamingConfiguration", Schemas.ENABLE_KINESIS_STREAMING_CONFIGURATION)
             .builderSupplier(EnableKinesisStreamingDestinationInput::builder)
             .shapeClass(EnableKinesisStreamingDestinationInput.class)
             .build();

    static final Schema ENABLE_KINESIS_STREAMING_DESTINATION_OUTPUT = Schema.structureBuilder(ShapeId.from("com.amazonaws.dynamodb#KinesisStreamingDestinationOutput"))
             .putMember("TableName", Schemas.TABLE_NAME)
             .putMember("StreamArn", Schemas.STREAM_ARN)
             .putMember("DestinationStatus", DestinationStatus.$SCHEMA)
             .putMember("EnableKinesisStreamingConfiguration", Schemas.ENABLE_KINESIS_STREAMING_CONFIGURATION)
             .builderSupplier(EnableKinesisStreamingDestinationOutput::builder)
             .shapeClass(EnableKinesisStreamingDestinationOutput.class)
             .build();

    static final Schema POSITIVE_INTEGER_OBJECT = Schema.createInteger(ShapeId.from("com.amazonaws.dynamodb#PositiveIntegerObject"),
            RangeTrait.builder().min(new BigDecimal("1")).build());
    static final Schema PARTI_QL_NEXT_TOKEN = Schema.createString(ShapeId.from("com.amazonaws.dynamodb#PartiQLNextToken"),
            LengthTrait.builder().min(1L).max(32768L).build());
    static final Schema IDEMPOTENT_PARAMETER_MISMATCH_EXCEPTION = Schema.structureBuilder(ShapeId.from("com.amazonaws.dynamodb#IdempotentParameterMismatchException"),
            new ErrorTrait("client"))
             .putMember("Message", Schemas.ERROR_MESSAGE)
             .builderSupplier(IdempotentParameterMismatchException::builder)
             .shapeClass(IdempotentParameterMismatchException.class)
             .build();

    static final Schema TRANSACTION_IN_PROGRESS_EXCEPTION = Schema.structureBuilder(ShapeId.from("com.amazonaws.dynamodb#TransactionInProgressException"),
            new ErrorTrait("client"))
             .putMember("Message", Schemas.ERROR_MESSAGE)
             .builderSupplier(TransactionInProgressException::builder)
             .shapeClass(TransactionInProgressException.class)
             .build();

    static final Schema EXPORT_CONFLICT_EXCEPTION = Schema.structureBuilder(ShapeId.from("com.amazonaws.dynamodb#ExportConflictException"),
            new ErrorTrait("client"))
             .putMember("message", Schemas.ERROR_MESSAGE)
             .builderSupplier(ExportConflictException::builder)
             .shapeClass(ExportConflictException.class)
             .build();

    static final Schema EXPORT_TABLE_TO_POINT_IN_TIME_INPUT = Schema.structureBuilder(ShapeId.from("com.amazonaws.dynamodb#ExportTableToPointInTimeInput"))
             .putMember("TableArn", Schemas.TABLE_ARN,
                     new ContextParamTrait.Provider().createTrait(
                         ShapeId.from("smithy.rules#contextParam"),
                         Node.objectNodeBuilder()
                             .withMember("name", "ResourceArn")
                             .build()
                     ),
                     new RequiredTrait())
             .putMember("ExportTime", Schemas.EXPORT_TIME)
             .putMember("ClientToken", Schemas.CLIENT_TOKEN,
                     new IdempotencyTokenTrait())
             .putMember("S3Bucket", Schemas.S3_BUCKET,
                     new RequiredTrait())
             .putMember("S3BucketOwner", Schemas.S3_BUCKET_OWNER)
             .putMember("S3Prefix", Schemas.S3_PREFIX)
             .putMember("S3SseAlgorithm", S3SseAlgorithm.$SCHEMA)
             .putMember("S3SseKmsKeyId", Schemas.S3_SSE_KMS_KEY_ID)
             .putMember("ExportFormat", ExportFormat.$SCHEMA)
             .putMember("ExportType", ExportType.$SCHEMA)
             .putMember("IncrementalExportSpecification", Schemas.INCREMENTAL_EXPORT_SPECIFICATION)
             .builderSupplier(ExportTableToPointInTimeInput::builder)
             .shapeClass(ExportTableToPointInTimeInput.class)
             .build();

    static final Schema EXPORT_TABLE_TO_POINT_IN_TIME_OUTPUT = Schema.structureBuilder(ShapeId.from("com.amazonaws.dynamodb#ExportTableToPointInTimeOutput"))
             .putMember("ExportDescription", Schemas.EXPORT_DESCRIPTION)
             .builderSupplier(ExportTableToPointInTimeOutput::builder)
             .shapeClass(ExportTableToPointInTimeOutput.class)
             .build();

    static final Schema INVALID_EXPORT_TIME_EXCEPTION = Schema.structureBuilder(ShapeId.from("com.amazonaws.dynamodb#InvalidExportTimeException"),
            new ErrorTrait("client"))
             .putMember("message", Schemas.ERROR_MESSAGE)
             .builderSupplier(InvalidExportTimeException::builder)
             .shapeClass(InvalidExportTimeException.class)
             .build();

    static final Schema POINT_IN_TIME_RECOVERY_UNAVAILABLE_EXCEPTION = Schema.structureBuilder(ShapeId.from("com.amazonaws.dynamodb#PointInTimeRecoveryUnavailableException"),
            new ErrorTrait("client"))
             .putMember("message", Schemas.ERROR_MESSAGE)
             .builderSupplier(PointInTimeRecoveryUnavailableException::builder)
             .shapeClass(PointInTimeRecoveryUnavailableException.class)
             .build();

    static final Schema GET_RESOURCE_POLICY_INPUT = Schema.structureBuilder(ShapeId.from("com.amazonaws.dynamodb#GetResourcePolicyInput"))
             .putMember("ResourceArn", Schemas.RESOURCE_ARN_STRING,
                     new ContextParamTrait.Provider().createTrait(
                         ShapeId.from("smithy.rules#contextParam"),
                         Node.objectNodeBuilder()
                             .withMember("name", "ResourceArn")
                             .build()
                     ),
                     new RequiredTrait())
             .builderSupplier(GetResourcePolicyInput::builder)
             .shapeClass(GetResourcePolicyInput.class)
             .build();

    static final Schema GET_RESOURCE_POLICY_OUTPUT = Schema.structureBuilder(ShapeId.from("com.amazonaws.dynamodb#GetResourcePolicyOutput"))
             .putMember("Policy", Schemas.RESOURCE_POLICY)
             .putMember("RevisionId", Schemas.POLICY_REVISION_ID)
             .builderSupplier(GetResourcePolicyOutput::builder)
             .shapeClass(GetResourcePolicyOutput.class)
             .build();

    static final Schema IMPORT_CONFLICT_EXCEPTION = Schema.structureBuilder(ShapeId.from("com.amazonaws.dynamodb#ImportConflictException"),
            new ErrorTrait("client"))
             .putMember("message", Schemas.ERROR_MESSAGE)
             .builderSupplier(ImportConflictException::builder)
             .shapeClass(ImportConflictException.class)
             .build();

    static final Schema IMPORT_TABLE_INPUT = Schema.structureBuilder(ShapeId.from("com.amazonaws.dynamodb#ImportTableInput"))
             .putMember("ClientToken", Schemas.CLIENT_TOKEN,
                     new IdempotencyTokenTrait())
             .putMember("S3BucketSource", Schemas.S3_BUCKET_SOURCE,
                     new RequiredTrait())
             .putMember("InputFormat", InputFormat.$SCHEMA,
                     new RequiredTrait())
             .putMember("InputFormatOptions", Schemas.INPUT_FORMAT_OPTIONS)
             .putMember("InputCompressionType", InputCompressionType.$SCHEMA)
             .putMember("TableCreationParameters", Schemas.TABLE_CREATION_PARAMETERS,
                     new RequiredTrait())
             .builderSupplier(ImportTableInput::builder)
             .shapeClass(ImportTableInput.class)
             .build();

    static final Schema IMPORT_TABLE_OUTPUT = Schema.structureBuilder(ShapeId.from("com.amazonaws.dynamodb#ImportTableOutput"))
             .putMember("ImportTableDescription", Schemas.IMPORT_TABLE_DESCRIPTION,
                     new RequiredTrait())
             .builderSupplier(ImportTableOutput::builder)
             .shapeClass(ImportTableOutput.class)
             .build();

    static final Schema TIME_RANGE_LOWER_BOUND = Schema.createTimestamp(ShapeId.from("com.amazonaws.dynamodb#TimeRangeLowerBound"));
    static final Schema TIME_RANGE_UPPER_BOUND = Schema.createTimestamp(ShapeId.from("com.amazonaws.dynamodb#TimeRangeUpperBound"));
    static final Schema LIST_BACKUPS_INPUT = Schema.structureBuilder(ShapeId.from("com.amazonaws.dynamodb#ListBackupsInput"))
             .putMember("TableName", Schemas.TABLE_ARN,
                     new ContextParamTrait.Provider().createTrait(
                         ShapeId.from("smithy.rules#contextParam"),
                         Node.objectNodeBuilder()
                             .withMember("name", "ResourceArn")
                             .build()
                     ))
             .putMember("Limit", Schemas.BACKUPS_INPUT_LIMIT)
             .putMember("TimeRangeLowerBound", Schemas.TIME_RANGE_LOWER_BOUND)
             .putMember("TimeRangeUpperBound", Schemas.TIME_RANGE_UPPER_BOUND)
             .putMember("ExclusiveStartBackupArn", Schemas.BACKUP_ARN)
             .putMember("BackupType", BackupTypeFilter.$SCHEMA)
             .builderSupplier(ListBackupsInput::builder)
             .shapeClass(ListBackupsInput.class)
             .build();

    static final Schema LIST_BACKUPS_OUTPUT = Schema.structureBuilder(ShapeId.from("com.amazonaws.dynamodb#ListBackupsOutput"))
             .putMember("BackupSummaries", Schemas.BACKUP_SUMMARIES)
             .putMember("LastEvaluatedBackupArn", Schemas.BACKUP_ARN)
             .builderSupplier(ListBackupsOutput::builder)
             .shapeClass(ListBackupsOutput.class)
             .build();

    static final Schema LIST_CONTRIBUTOR_INSIGHTS_LIMIT = Schema.createInteger(ShapeId.from("com.amazonaws.dynamodb#ListContributorInsightsLimit"),
            new DefaultTrait(Node.from(0L)),
            RangeTrait.builder().max(new BigDecimal("100")).build());
    static final Schema NEXT_TOKEN_STRING = Schema.createString(ShapeId.from("com.amazonaws.dynamodb#NextTokenString"));
    static final Schema LIST_CONTRIBUTOR_INSIGHTS_INPUT = Schema.structureBuilder(ShapeId.from("com.amazonaws.dynamodb#ListContributorInsightsInput"))
             .putMember("TableName", Schemas.TABLE_ARN,
                     new ContextParamTrait.Provider().createTrait(
                         ShapeId.from("smithy.rules#contextParam"),
                         Node.objectNodeBuilder()
                             .withMember("name", "ResourceArn")
                             .build()
                     ))
             .putMember("NextToken", Schemas.NEXT_TOKEN_STRING)
             .putMember("MaxResults", Schemas.LIST_CONTRIBUTOR_INSIGHTS_LIMIT,
                     new DefaultTrait(Node.from(0L)))
             .builderSupplier(ListContributorInsightsInput::builder)
             .shapeClass(ListContributorInsightsInput.class)
             .build();

    static final Schema LIST_CONTRIBUTOR_INSIGHTS_OUTPUT = Schema.structureBuilder(ShapeId.from("com.amazonaws.dynamodb#ListContributorInsightsOutput"))
             .putMember("ContributorInsightsSummaries", Schemas.CONTRIBUTOR_INSIGHTS_SUMMARIES)
             .putMember("NextToken", Schemas.NEXT_TOKEN_STRING)
             .builderSupplier(ListContributorInsightsOutput::builder)
             .shapeClass(ListContributorInsightsOutput.class)
             .build();

    static final Schema LIST_EXPORTS_MAX_LIMIT = Schema.createInteger(ShapeId.from("com.amazonaws.dynamodb#ListExportsMaxLimit"),
            RangeTrait.builder().min(new BigDecimal("1")).max(new BigDecimal("25")).build());
    static final Schema EXPORT_NEXT_TOKEN = Schema.createString(ShapeId.from("com.amazonaws.dynamodb#ExportNextToken"));
    static final Schema LIST_EXPORTS_INPUT = Schema.structureBuilder(ShapeId.from("com.amazonaws.dynamodb#ListExportsInput"))
             .putMember("TableArn", Schemas.TABLE_ARN,
                     new ContextParamTrait.Provider().createTrait(
                         ShapeId.from("smithy.rules#contextParam"),
                         Node.objectNodeBuilder()
                             .withMember("name", "ResourceArn")
                             .build()
                     ))
             .putMember("MaxResults", Schemas.LIST_EXPORTS_MAX_LIMIT)
             .putMember("NextToken", Schemas.EXPORT_NEXT_TOKEN)
             .builderSupplier(ListExportsInput::builder)
             .shapeClass(ListExportsInput.class)
             .build();

    static final Schema EXPORT_SUMMARY = Schema.structureBuilder(ShapeId.from("com.amazonaws.dynamodb#ExportSummary"))
             .putMember("ExportArn", Schemas.EXPORT_ARN)
             .putMember("ExportStatus", ExportStatus.$SCHEMA)
             .putMember("ExportType", ExportType.$SCHEMA)
             .builderSupplier(ExportSummary::builder)
             .shapeClass(ExportSummary.class)
             .build();

    static final Schema EXPORT_SUMMARIES = Schema.listBuilder(ShapeId.from("com.amazonaws.dynamodb#ExportSummaries"))
        .putMember("member", Schemas.EXPORT_SUMMARY)
        .build();

    static final Schema LIST_EXPORTS_OUTPUT = Schema.structureBuilder(ShapeId.from("com.amazonaws.dynamodb#ListExportsOutput"))
             .putMember("ExportSummaries", Schemas.EXPORT_SUMMARIES)
             .putMember("NextToken", Schemas.EXPORT_NEXT_TOKEN)
             .builderSupplier(ListExportsOutput::builder)
             .shapeClass(ListExportsOutput.class)
             .build();

    static final Schema LIST_GLOBAL_TABLES_INPUT = Schema.structureBuilder(ShapeId.from("com.amazonaws.dynamodb#ListGlobalTablesInput"))
             .putMember("ExclusiveStartGlobalTableName", Schemas.TABLE_NAME)
             .putMember("Limit", Schemas.POSITIVE_INTEGER_OBJECT)
             .putMember("RegionName", Schemas.REGION_NAME)
             .builderSupplier(ListGlobalTablesInput::builder)
             .shapeClass(ListGlobalTablesInput.class)
             .build();

    static final Schema GLOBAL_TABLE = Schema.structureBuilder(ShapeId.from("com.amazonaws.dynamodb#GlobalTable"))
             .putMember("GlobalTableName", Schemas.TABLE_NAME)
             .putMember("ReplicationGroup", Schemas.REPLICA_LIST)
             .builderSupplier(GlobalTable::builder)
             .shapeClass(GlobalTable.class)
             .build();

    static final Schema GLOBAL_TABLE_LIST = Schema.listBuilder(ShapeId.from("com.amazonaws.dynamodb#GlobalTableList"))
        .putMember("member", Schemas.GLOBAL_TABLE)
        .build();

    static final Schema LIST_GLOBAL_TABLES_OUTPUT = Schema.structureBuilder(ShapeId.from("com.amazonaws.dynamodb#ListGlobalTablesOutput"))
             .putMember("GlobalTables", Schemas.GLOBAL_TABLE_LIST)
             .putMember("LastEvaluatedGlobalTableName", Schemas.TABLE_NAME)
             .builderSupplier(ListGlobalTablesOutput::builder)
             .shapeClass(ListGlobalTablesOutput.class)
             .build();

    static final Schema IMPORT_NEXT_TOKEN = Schema.createString(ShapeId.from("com.amazonaws.dynamodb#ImportNextToken"),
            LengthTrait.builder().min(112L).max(1024L).build(),
            new PatternTrait("^([0-9a-f]{16})+$"));
    static final Schema LIST_IMPORTS_MAX_LIMIT = Schema.createInteger(ShapeId.from("com.amazonaws.dynamodb#ListImportsMaxLimit"),
            RangeTrait.builder().min(new BigDecimal("1")).max(new BigDecimal("25")).build());
    static final Schema LIST_IMPORTS_INPUT = Schema.structureBuilder(ShapeId.from("com.amazonaws.dynamodb#ListImportsInput"))
             .putMember("TableArn", Schemas.TABLE_ARN,
                     new ContextParamTrait.Provider().createTrait(
                         ShapeId.from("smithy.rules#contextParam"),
                         Node.objectNodeBuilder()
                             .withMember("name", "ResourceArn")
                             .build()
                     ))
             .putMember("PageSize", Schemas.LIST_IMPORTS_MAX_LIMIT)
             .putMember("NextToken", Schemas.IMPORT_NEXT_TOKEN)
             .builderSupplier(ListImportsInput::builder)
             .shapeClass(ListImportsInput.class)
             .build();

    static final Schema IMPORT_SUMMARY = Schema.structureBuilder(ShapeId.from("com.amazonaws.dynamodb#ImportSummary"))
             .putMember("ImportArn", Schemas.IMPORT_ARN)
             .putMember("ImportStatus", ImportStatus.$SCHEMA)
             .putMember("TableArn", Schemas.TABLE_ARN)
             .putMember("S3BucketSource", Schemas.S3_BUCKET_SOURCE)
             .putMember("CloudWatchLogGroupArn", Schemas.CLOUD_WATCH_LOG_GROUP_ARN)
             .putMember("InputFormat", InputFormat.$SCHEMA)
             .putMember("StartTime", Schemas.IMPORT_START_TIME)
             .putMember("EndTime", Schemas.IMPORT_END_TIME)
             .builderSupplier(ImportSummary::builder)
             .shapeClass(ImportSummary.class)
             .build();

    static final Schema IMPORT_SUMMARY_LIST = Schema.listBuilder(ShapeId.from("com.amazonaws.dynamodb#ImportSummaryList"))
        .putMember("member", Schemas.IMPORT_SUMMARY)
        .build();

    static final Schema LIST_IMPORTS_OUTPUT = Schema.structureBuilder(ShapeId.from("com.amazonaws.dynamodb#ListImportsOutput"))
             .putMember("ImportSummaryList", Schemas.IMPORT_SUMMARY_LIST)
             .putMember("NextToken", Schemas.IMPORT_NEXT_TOKEN)
             .builderSupplier(ListImportsOutput::builder)
             .shapeClass(ListImportsOutput.class)
             .build();

    static final Schema LIST_TABLES_INPUT_LIMIT = Schema.createInteger(ShapeId.from("com.amazonaws.dynamodb#ListTablesInputLimit"),
            RangeTrait.builder().min(new BigDecimal("1")).max(new BigDecimal("100")).build());
    static final Schema LIST_TABLES_INPUT = Schema.structureBuilder(ShapeId.from("com.amazonaws.dynamodb#ListTablesInput"))
             .putMember("ExclusiveStartTableName", Schemas.TABLE_NAME)
             .putMember("Limit", Schemas.LIST_TABLES_INPUT_LIMIT)
             .builderSupplier(ListTablesInput::builder)
             .shapeClass(ListTablesInput.class)
             .build();

    static final Schema TABLE_NAME_LIST = Schema.listBuilder(ShapeId.from("com.amazonaws.dynamodb#TableNameList"))
        .putMember("member", Schemas.TABLE_NAME)
        .build();

    static final Schema LIST_TABLES_OUTPUT = Schema.structureBuilder(ShapeId.from("com.amazonaws.dynamodb#ListTablesOutput"))
             .putMember("TableNames", Schemas.TABLE_NAME_LIST)
             .putMember("LastEvaluatedTableName", Schemas.TABLE_NAME)
             .builderSupplier(ListTablesOutput::builder)
             .shapeClass(ListTablesOutput.class)
             .build();

    static final Schema LIST_TAGS_OF_RESOURCE_INPUT = Schema.structureBuilder(ShapeId.from("com.amazonaws.dynamodb#ListTagsOfResourceInput"))
             .putMember("ResourceArn", Schemas.RESOURCE_ARN_STRING,
                     new ContextParamTrait.Provider().createTrait(
                         ShapeId.from("smithy.rules#contextParam"),
                         Node.objectNodeBuilder()
                             .withMember("name", "ResourceArn")
                             .build()
                     ),
                     new RequiredTrait())
             .putMember("NextToken", Schemas.NEXT_TOKEN_STRING)
             .builderSupplier(ListTagsOfResourceInput::builder)
             .shapeClass(ListTagsOfResourceInput.class)
             .build();

    static final Schema LIST_TAGS_OF_RESOURCE_OUTPUT = Schema.structureBuilder(ShapeId.from("com.amazonaws.dynamodb#ListTagsOfResourceOutput"))
             .putMember("Tags", Schemas.TAG_LIST)
             .putMember("NextToken", Schemas.NEXT_TOKEN_STRING)
             .builderSupplier(ListTagsOfResourceOutput::builder)
             .shapeClass(ListTagsOfResourceOutput.class)
             .build();

    static final Schema PUT_RESOURCE_POLICY_INPUT = Schema.structureBuilder(ShapeId.from("com.amazonaws.dynamodb#PutResourcePolicyInput"))
             .putMember("ResourceArn", Schemas.RESOURCE_ARN_STRING,
                     new ContextParamTrait.Provider().createTrait(
                         ShapeId.from("smithy.rules#contextParam"),
                         Node.objectNodeBuilder()
                             .withMember("name", "ResourceArn")
                             .build()
                     ),
                     new RequiredTrait())
             .putMember("Policy", Schemas.RESOURCE_POLICY,
                     new RequiredTrait())
             .putMember("ExpectedRevisionId", Schemas.POLICY_REVISION_ID)
             .putMember("ConfirmRemoveSelfResourceAccess", Schemas.CONFIRM_REMOVE_SELF_RESOURCE_ACCESS,
                     new DefaultTrait(Node.from(false)))
             .builderSupplier(PutResourcePolicyInput::builder)
             .shapeClass(PutResourcePolicyInput.class)
             .build();

    static final Schema PUT_RESOURCE_POLICY_OUTPUT = Schema.structureBuilder(ShapeId.from("com.amazonaws.dynamodb#PutResourcePolicyOutput"))
             .putMember("RevisionId", Schemas.POLICY_REVISION_ID)
             .builderSupplier(PutResourcePolicyOutput::builder)
             .shapeClass(PutResourcePolicyOutput.class)
             .build();

    static final Schema KEY_EXPRESSION = Schema.createString(ShapeId.from("com.amazonaws.dynamodb#KeyExpression"));
    static final Schema INTEGER = Schema.createInteger(ShapeId.from("com.amazonaws.dynamodb#Integer"),
            new DefaultTrait(Node.from(0L)));
    static final Schema RESTORE_TABLE_FROM_BACKUP_INPUT = Schema.structureBuilder(ShapeId.from("com.amazonaws.dynamodb#RestoreTableFromBackupInput"))
             .putMember("TargetTableName", Schemas.TABLE_NAME,
                     new ContextParamTrait.Provider().createTrait(
                         ShapeId.from("smithy.rules#contextParam"),
                         Node.objectNodeBuilder()
                             .withMember("name", "ResourceArn")
                             .build()
                     ),
                     new RequiredTrait())
             .putMember("BackupArn", Schemas.BACKUP_ARN,
                     new RequiredTrait())
             .putMember("BillingModeOverride", BillingMode.$SCHEMA)
             .putMember("GlobalSecondaryIndexOverride", Schemas.GLOBAL_SECONDARY_INDEX_LIST)
             .putMember("LocalSecondaryIndexOverride", Schemas.LOCAL_SECONDARY_INDEX_LIST)
             .putMember("ProvisionedThroughputOverride", Schemas.PROVISIONED_THROUGHPUT)
             .putMember("OnDemandThroughputOverride", Schemas.ON_DEMAND_THROUGHPUT)
             .putMember("SSESpecificationOverride", Schemas.SSE_SPECIFICATION)
             .putMember("VectorIndexOverride", Schemas.VECTOR_INDEX_LIST)
             .builderSupplier(RestoreTableFromBackupInput::builder)
             .shapeClass(RestoreTableFromBackupInput.class)
             .build();

    static final Schema RESTORE_TABLE_FROM_BACKUP_OUTPUT = Schema.structureBuilder(ShapeId.from("com.amazonaws.dynamodb#RestoreTableFromBackupOutput"))
             .putMember("TableDescription", Schemas.TABLE_DESCRIPTION)
             .builderSupplier(RestoreTableFromBackupOutput::builder)
             .shapeClass(RestoreTableFromBackupOutput.class)
             .build();

    static final Schema TABLE_ALREADY_EXISTS_EXCEPTION = Schema.structureBuilder(ShapeId.from("com.amazonaws.dynamodb#TableAlreadyExistsException"),
            new ErrorTrait("client"))
             .putMember("message", Schemas.ERROR_MESSAGE)
             .builderSupplier(TableAlreadyExistsException::builder)
             .shapeClass(TableAlreadyExistsException.class)
             .build();

    static final Schema INVALID_RESTORE_TIME_EXCEPTION = Schema.structureBuilder(ShapeId.from("com.amazonaws.dynamodb#InvalidRestoreTimeException"),
            new ErrorTrait("client"))
             .putMember("message", Schemas.ERROR_MESSAGE)
             .builderSupplier(InvalidRestoreTimeException::builder)
             .shapeClass(InvalidRestoreTimeException.class)
             .build();

    static final Schema RESTORE_TABLE_TO_POINT_IN_TIME_INPUT = Schema.structureBuilder(ShapeId.from("com.amazonaws.dynamodb#RestoreTableToPointInTimeInput"))
             .putMember("SourceTableArn", Schemas.TABLE_ARN)
             .putMember("SourceTableName", Schemas.TABLE_NAME)
             .putMember("TargetTableName", Schemas.TABLE_NAME,
                     new ContextParamTrait.Provider().createTrait(
                         ShapeId.from("smithy.rules#contextParam"),
                         Node.objectNodeBuilder()
                             .withMember("name", "ResourceArn")
                             .build()
                     ),
                     new RequiredTrait())
             .putMember("UseLatestRestorableTime", Schemas.BOOLEAN_OBJECT)
             .putMember("RestoreDateTime", Schemas.DATE)
             .putMember("BillingModeOverride", BillingMode.$SCHEMA)
             .putMember("GlobalSecondaryIndexOverride", Schemas.GLOBAL_SECONDARY_INDEX_LIST)
             .putMember("LocalSecondaryIndexOverride", Schemas.LOCAL_SECONDARY_INDEX_LIST)
             .putMember("ProvisionedThroughputOverride", Schemas.PROVISIONED_THROUGHPUT)
             .putMember("OnDemandThroughputOverride", Schemas.ON_DEMAND_THROUGHPUT)
             .putMember("SSESpecificationOverride", Schemas.SSE_SPECIFICATION)
             .putMember("VectorIndexOverride", Schemas.VECTOR_INDEX_LIST)
             .builderSupplier(RestoreTableToPointInTimeInput::builder)
             .shapeClass(RestoreTableToPointInTimeInput.class)
             .build();

    static final Schema RESTORE_TABLE_TO_POINT_IN_TIME_OUTPUT = Schema.structureBuilder(ShapeId.from("com.amazonaws.dynamodb#RestoreTableToPointInTimeOutput"))
             .putMember("TableDescription", Schemas.TABLE_DESCRIPTION)
             .builderSupplier(RestoreTableToPointInTimeOutput::builder)
             .shapeClass(RestoreTableToPointInTimeOutput.class)
             .build();

    static final Schema SCAN_SEGMENT = Schema.createInteger(ShapeId.from("com.amazonaws.dynamodb#ScanSegment"),
            RangeTrait.builder().min(new BigDecimal("0")).max(new BigDecimal("999999")).build());
    static final Schema SCAN_TOTAL_SEGMENTS = Schema.createInteger(ShapeId.from("com.amazonaws.dynamodb#ScanTotalSegments"),
            RangeTrait.builder().min(new BigDecimal("1")).max(new BigDecimal("1000000")).build());
    static final Schema TOP_K_INTEGER = Schema.createInteger(ShapeId.from("com.amazonaws.dynamodb#TopKInteger"),
            RangeTrait.builder().min(new BigDecimal("1")).build());
    static final Schema SCORE_NUMBER = Schema.createDouble(ShapeId.from("com.amazonaws.dynamodb#ScoreNumber"),
            new DefaultTrait(Node.from(0L)));
    static final Schema TAG_RESOURCE_INPUT = Schema.structureBuilder(ShapeId.from("com.amazonaws.dynamodb#TagResourceInput"))
             .putMember("ResourceArn", Schemas.RESOURCE_ARN_STRING,
                     new ContextParamTrait.Provider().createTrait(
                         ShapeId.from("smithy.rules#contextParam"),
                         Node.objectNodeBuilder()
                             .withMember("name", "ResourceArn")
                             .build()
                     ),
                     new RequiredTrait())
             .putMember("Tags", Schemas.TAG_LIST,
                     new RequiredTrait())
             .builderSupplier(TagResourceInput::builder)
             .shapeClass(TagResourceInput.class)
             .build();

    static final Schema TAG_RESOURCE_OUTPUT = Schema.structureBuilder(ShapeId.from("com.amazonaws.dynamodb#TagResourceOutput"),
            new UnitTypeTrait()).builderSupplier(TagResourceOutput::builder)
             .shapeClass(TagResourceOutput.class)
             .build();

    static final Schema UPDATE_EXPRESSION = Schema.createString(ShapeId.from("com.amazonaws.dynamodb#UpdateExpression"));
    static final Schema TAG_KEY_LIST = Schema.listBuilder(ShapeId.from("com.amazonaws.dynamodb#TagKeyList"))
        .putMember("member", Schemas.TAG_KEY_STRING)
        .build();

    static final Schema UNTAG_RESOURCE_INPUT = Schema.structureBuilder(ShapeId.from("com.amazonaws.dynamodb#UntagResourceInput"))
             .putMember("ResourceArn", Schemas.RESOURCE_ARN_STRING,
                     new ContextParamTrait.Provider().createTrait(
                         ShapeId.from("smithy.rules#contextParam"),
                         Node.objectNodeBuilder()
                             .withMember("name", "ResourceArn")
                             .build()
                     ),
                     new RequiredTrait())
             .putMember("TagKeys", Schemas.TAG_KEY_LIST,
                     new RequiredTrait())
             .builderSupplier(UntagResourceInput::builder)
             .shapeClass(UntagResourceInput.class)
             .build();

    static final Schema UNTAG_RESOURCE_OUTPUT = Schema.structureBuilder(ShapeId.from("com.amazonaws.dynamodb#UntagResourceOutput"),
            new UnitTypeTrait()).builderSupplier(UntagResourceOutput::builder)
             .shapeClass(UntagResourceOutput.class)
             .build();

    static final Schema POINT_IN_TIME_RECOVERY_SPECIFICATION = Schema.structureBuilder(ShapeId.from("com.amazonaws.dynamodb#PointInTimeRecoverySpecification"))
             .putMember("PointInTimeRecoveryEnabled", Schemas.BOOLEAN_OBJECT,
                     new RequiredTrait())
             .putMember("RecoveryPeriodInDays", Schemas.RECOVERY_PERIOD_IN_DAYS)
             .builderSupplier(PointInTimeRecoverySpecification::builder)
             .shapeClass(PointInTimeRecoverySpecification.class)
             .build();

    static final Schema UPDATE_CONTINUOUS_BACKUPS_INPUT = Schema.structureBuilder(ShapeId.from("com.amazonaws.dynamodb#UpdateContinuousBackupsInput"))
             .putMember("TableName", Schemas.TABLE_ARN,
                     new ContextParamTrait.Provider().createTrait(
                         ShapeId.from("smithy.rules#contextParam"),
                         Node.objectNodeBuilder()
                             .withMember("name", "ResourceArn")
                             .build()
                     ),
                     new RequiredTrait())
             .putMember("PointInTimeRecoverySpecification", Schemas.POINT_IN_TIME_RECOVERY_SPECIFICATION,
                     new RequiredTrait())
             .builderSupplier(UpdateContinuousBackupsInput::builder)
             .shapeClass(UpdateContinuousBackupsInput.class)
             .build();

    static final Schema UPDATE_CONTINUOUS_BACKUPS_OUTPUT = Schema.structureBuilder(ShapeId.from("com.amazonaws.dynamodb#UpdateContinuousBackupsOutput"))
             .putMember("ContinuousBackupsDescription", Schemas.CONTINUOUS_BACKUPS_DESCRIPTION)
             .builderSupplier(UpdateContinuousBackupsOutput::builder)
             .shapeClass(UpdateContinuousBackupsOutput.class)
             .build();

    static final Schema UPDATE_CONTRIBUTOR_INSIGHTS_INPUT = Schema.structureBuilder(ShapeId.from("com.amazonaws.dynamodb#UpdateContributorInsightsInput"))
             .putMember("TableName", Schemas.TABLE_ARN,
                     new ContextParamTrait.Provider().createTrait(
                         ShapeId.from("smithy.rules#contextParam"),
                         Node.objectNodeBuilder()
                             .withMember("name", "ResourceArn")
                             .build()
                     ),
                     new RequiredTrait())
             .putMember("IndexName", Schemas.INDEX_NAME)
             .putMember("ContributorInsightsAction", ContributorInsightsAction.$SCHEMA,
                     new RequiredTrait())
             .putMember("ContributorInsightsMode", ContributorInsightsMode.$SCHEMA)
             .builderSupplier(UpdateContributorInsightsInput::builder)
             .shapeClass(UpdateContributorInsightsInput.class)
             .build();

    static final Schema UPDATE_CONTRIBUTOR_INSIGHTS_OUTPUT = Schema.structureBuilder(ShapeId.from("com.amazonaws.dynamodb#UpdateContributorInsightsOutput"))
             .putMember("TableName", Schemas.TABLE_NAME)
             .putMember("IndexName", Schemas.INDEX_NAME)
             .putMember("ContributorInsightsStatus", ContributorInsightsStatus.$SCHEMA)
             .putMember("ContributorInsightsMode", ContributorInsightsMode.$SCHEMA)
             .builderSupplier(UpdateContributorInsightsOutput::builder)
             .shapeClass(UpdateContributorInsightsOutput.class)
             .build();

    static final Schema REPLICA_ALREADY_EXISTS_EXCEPTION = Schema.structureBuilder(ShapeId.from("com.amazonaws.dynamodb#ReplicaAlreadyExistsException"),
            new ErrorTrait("client"))
             .putMember("message", Schemas.ERROR_MESSAGE)
             .builderSupplier(ReplicaAlreadyExistsException::builder)
             .shapeClass(ReplicaAlreadyExistsException.class)
             .build();

    static final Schema REPLICA_NOT_FOUND_EXCEPTION = Schema.structureBuilder(ShapeId.from("com.amazonaws.dynamodb#ReplicaNotFoundException"),
            new ErrorTrait("client"))
             .putMember("message", Schemas.ERROR_MESSAGE)
             .builderSupplier(ReplicaNotFoundException::builder)
             .shapeClass(ReplicaNotFoundException.class)
             .build();

    static final Schema REPLICA_UPDATE = Schema.structureBuilder(ShapeId.from("com.amazonaws.dynamodb#ReplicaUpdate"))
             .putMember("Create", Schemas.CREATE_REPLICA_ACTION)
             .putMember("Delete", Schemas.DELETE_REPLICA_ACTION)
             .builderSupplier(ReplicaUpdate::builder)
             .shapeClass(ReplicaUpdate.class)
             .build();

    static final Schema REPLICA_UPDATE_LIST = Schema.listBuilder(ShapeId.from("com.amazonaws.dynamodb#ReplicaUpdateList"))
        .putMember("member", Schemas.REPLICA_UPDATE)
        .build();

    static final Schema UPDATE_GLOBAL_TABLE_INPUT = Schema.structureBuilder(ShapeId.from("com.amazonaws.dynamodb#UpdateGlobalTableInput"))
             .putMember("GlobalTableName", Schemas.TABLE_NAME,
                     new ContextParamTrait.Provider().createTrait(
                         ShapeId.from("smithy.rules#contextParam"),
                         Node.objectNodeBuilder()
                             .withMember("name", "ResourceArn")
                             .build()
                     ),
                     new RequiredTrait())
             .putMember("ReplicaUpdates", Schemas.REPLICA_UPDATE_LIST,
                     new RequiredTrait())
             .builderSupplier(UpdateGlobalTableInput::builder)
             .shapeClass(UpdateGlobalTableInput.class)
             .build();

    static final Schema UPDATE_GLOBAL_TABLE_OUTPUT = Schema.structureBuilder(ShapeId.from("com.amazonaws.dynamodb#UpdateGlobalTableOutput"))
             .putMember("GlobalTableDescription", Schemas.GLOBAL_TABLE_DESCRIPTION)
             .builderSupplier(UpdateGlobalTableOutput::builder)
             .shapeClass(UpdateGlobalTableOutput.class)
             .build();

    static final Schema INDEX_NOT_FOUND_EXCEPTION = Schema.structureBuilder(ShapeId.from("com.amazonaws.dynamodb#IndexNotFoundException"),
            new ErrorTrait("client"))
             .putMember("message", Schemas.ERROR_MESSAGE)
             .builderSupplier(IndexNotFoundException::builder)
             .shapeClass(IndexNotFoundException.class)
             .build();

    static final Schema GLOBAL_TABLE_GLOBAL_SECONDARY_INDEX_SETTINGS_UPDATE = Schema.structureBuilder(ShapeId.from("com.amazonaws.dynamodb#GlobalTableGlobalSecondaryIndexSettingsUpdate"))
             .putMember("IndexName", Schemas.INDEX_NAME,
                     new RequiredTrait())
             .putMember("ProvisionedWriteCapacityUnits", Schemas.POSITIVE_LONG_OBJECT)
             .putMember("ProvisionedWriteCapacityAutoScalingSettingsUpdate", Schemas.AUTO_SCALING_SETTINGS_UPDATE)
             .builderSupplier(GlobalTableGlobalSecondaryIndexSettingsUpdate::builder)
             .shapeClass(GlobalTableGlobalSecondaryIndexSettingsUpdate.class)
             .build();

    static final Schema GLOBAL_TABLE_GLOBAL_SECONDARY_INDEX_SETTINGS_UPDATE_LIST = Schema.listBuilder(ShapeId.from("com.amazonaws.dynamodb#GlobalTableGlobalSecondaryIndexSettingsUpdateList"),
            LengthTrait.builder().min(1L).max(20L).build())
        .putMember("member", Schemas.GLOBAL_TABLE_GLOBAL_SECONDARY_INDEX_SETTINGS_UPDATE)
        .build();

    static final Schema REPLICA_GLOBAL_SECONDARY_INDEX_SETTINGS_UPDATE = Schema.structureBuilder(ShapeId.from("com.amazonaws.dynamodb#ReplicaGlobalSecondaryIndexSettingsUpdate"))
             .putMember("IndexName", Schemas.INDEX_NAME,
                     new RequiredTrait())
             .putMember("ProvisionedReadCapacityUnits", Schemas.POSITIVE_LONG_OBJECT)
             .putMember("ProvisionedReadCapacityAutoScalingSettingsUpdate", Schemas.AUTO_SCALING_SETTINGS_UPDATE)
             .builderSupplier(ReplicaGlobalSecondaryIndexSettingsUpdate::builder)
             .shapeClass(ReplicaGlobalSecondaryIndexSettingsUpdate.class)
             .build();

    static final Schema REPLICA_GLOBAL_SECONDARY_INDEX_SETTINGS_UPDATE_LIST = Schema.listBuilder(ShapeId.from("com.amazonaws.dynamodb#ReplicaGlobalSecondaryIndexSettingsUpdateList"),
            LengthTrait.builder().min(1L).max(20L).build())
        .putMember("member", Schemas.REPLICA_GLOBAL_SECONDARY_INDEX_SETTINGS_UPDATE)
        .build();

    static final Schema REPLICA_SETTINGS_UPDATE = Schema.structureBuilder(ShapeId.from("com.amazonaws.dynamodb#ReplicaSettingsUpdate"))
             .putMember("RegionName", Schemas.REGION_NAME,
                     new RequiredTrait())
             .putMember("ReplicaProvisionedReadCapacityUnits", Schemas.POSITIVE_LONG_OBJECT)
             .putMember("ReplicaProvisionedReadCapacityAutoScalingSettingsUpdate", Schemas.AUTO_SCALING_SETTINGS_UPDATE)
             .putMember("ReplicaGlobalSecondaryIndexSettingsUpdate", Schemas.REPLICA_GLOBAL_SECONDARY_INDEX_SETTINGS_UPDATE_LIST)
             .putMember("ReplicaTableClass", TableClass.$SCHEMA)
             .builderSupplier(ReplicaSettingsUpdate::builder)
             .shapeClass(ReplicaSettingsUpdate.class)
             .build();

    static final Schema REPLICA_SETTINGS_UPDATE_LIST = Schema.listBuilder(ShapeId.from("com.amazonaws.dynamodb#ReplicaSettingsUpdateList"),
            LengthTrait.builder().min(1L).max(50L).build())
        .putMember("member", Schemas.REPLICA_SETTINGS_UPDATE)
        .build();

    static final Schema UPDATE_GLOBAL_TABLE_SETTINGS_INPUT = Schema.structureBuilder(ShapeId.from("com.amazonaws.dynamodb#UpdateGlobalTableSettingsInput"))
             .putMember("GlobalTableName", Schemas.TABLE_NAME,
                     new ContextParamTrait.Provider().createTrait(
                         ShapeId.from("smithy.rules#contextParam"),
                         Node.objectNodeBuilder()
                             .withMember("name", "ResourceArn")
                             .build()
                     ),
                     new RequiredTrait())
             .putMember("GlobalTableBillingMode", BillingMode.$SCHEMA)
             .putMember("GlobalTableProvisionedWriteCapacityUnits", Schemas.POSITIVE_LONG_OBJECT)
             .putMember("GlobalTableProvisionedWriteCapacityAutoScalingSettingsUpdate", Schemas.AUTO_SCALING_SETTINGS_UPDATE)
             .putMember("GlobalTableGlobalSecondaryIndexSettingsUpdate", Schemas.GLOBAL_TABLE_GLOBAL_SECONDARY_INDEX_SETTINGS_UPDATE_LIST)
             .putMember("ReplicaSettingsUpdate", Schemas.REPLICA_SETTINGS_UPDATE_LIST)
             .builderSupplier(UpdateGlobalTableSettingsInput::builder)
             .shapeClass(UpdateGlobalTableSettingsInput.class)
             .build();

    static final Schema UPDATE_GLOBAL_TABLE_SETTINGS_OUTPUT = Schema.structureBuilder(ShapeId.from("com.amazonaws.dynamodb#UpdateGlobalTableSettingsOutput"))
             .putMember("GlobalTableName", Schemas.TABLE_NAME)
             .putMember("ReplicaSettings", Schemas.REPLICA_SETTINGS_DESCRIPTION_LIST)
             .builderSupplier(UpdateGlobalTableSettingsOutput::builder)
             .shapeClass(UpdateGlobalTableSettingsOutput.class)
             .build();

    static final Schema UPDATE_KINESIS_STREAMING_CONFIGURATION = Schema.structureBuilder(ShapeId.from("com.amazonaws.dynamodb#UpdateKinesisStreamingConfiguration"))
             .putMember("ApproximateCreationDateTimePrecision", ApproximateCreationDateTimePrecision.$SCHEMA)
             .builderSupplier(UpdateKinesisStreamingConfiguration::builder)
             .shapeClass(UpdateKinesisStreamingConfiguration.class)
             .build();

    static final Schema UPDATE_KINESIS_STREAMING_DESTINATION_INPUT = Schema.structureBuilder(ShapeId.from("com.amazonaws.dynamodb#UpdateKinesisStreamingDestinationInput"))
             .putMember("TableName", Schemas.TABLE_ARN,
                     new ContextParamTrait.Provider().createTrait(
                         ShapeId.from("smithy.rules#contextParam"),
                         Node.objectNodeBuilder()
                             .withMember("name", "ResourceArn")
                             .build()
                     ),
                     new RequiredTrait())
             .putMember("StreamArn", Schemas.STREAM_ARN,
                     new RequiredTrait())
             .putMember("UpdateKinesisStreamingConfiguration", Schemas.UPDATE_KINESIS_STREAMING_CONFIGURATION)
             .builderSupplier(UpdateKinesisStreamingDestinationInput::builder)
             .shapeClass(UpdateKinesisStreamingDestinationInput.class)
             .build();

    static final Schema UPDATE_KINESIS_STREAMING_DESTINATION_OUTPUT = Schema.structureBuilder(ShapeId.from("com.amazonaws.dynamodb#UpdateKinesisStreamingDestinationOutput"))
             .putMember("TableName", Schemas.TABLE_NAME)
             .putMember("StreamArn", Schemas.STREAM_ARN)
             .putMember("DestinationStatus", DestinationStatus.$SCHEMA)
             .putMember("UpdateKinesisStreamingConfiguration", Schemas.UPDATE_KINESIS_STREAMING_CONFIGURATION)
             .builderSupplier(UpdateKinesisStreamingDestinationOutput::builder)
             .shapeClass(UpdateKinesisStreamingDestinationOutput.class)
             .build();

    static final Schema UPDATE_GLOBAL_SECONDARY_INDEX_ACTION = Schema.structureBuilder(ShapeId.from("com.amazonaws.dynamodb#UpdateGlobalSecondaryIndexAction"))
             .putMember("IndexName", Schemas.INDEX_NAME,
                     new RequiredTrait())
             .putMember("ProvisionedThroughput", Schemas.PROVISIONED_THROUGHPUT)
             .putMember("OnDemandThroughput", Schemas.ON_DEMAND_THROUGHPUT)
             .putMember("WarmThroughput", Schemas.WARM_THROUGHPUT)
             .builderSupplier(UpdateGlobalSecondaryIndexAction::builder)
             .shapeClass(UpdateGlobalSecondaryIndexAction.class)
             .build();

    static final Schema GLOBAL_SECONDARY_INDEX_UPDATE = Schema.structureBuilder(ShapeId.from("com.amazonaws.dynamodb#GlobalSecondaryIndexUpdate"))
             .putMember("Update", Schemas.UPDATE_GLOBAL_SECONDARY_INDEX_ACTION)
             .putMember("Create", Schemas.CREATE_GLOBAL_SECONDARY_INDEX_ACTION)
             .putMember("Delete", Schemas.DELETE_GLOBAL_SECONDARY_INDEX_ACTION)
             .builderSupplier(GlobalSecondaryIndexUpdate::builder)
             .shapeClass(GlobalSecondaryIndexUpdate.class)
             .build();

    static final Schema GLOBAL_SECONDARY_INDEX_UPDATE_LIST = Schema.listBuilder(ShapeId.from("com.amazonaws.dynamodb#GlobalSecondaryIndexUpdateList"))
        .putMember("member", Schemas.GLOBAL_SECONDARY_INDEX_UPDATE)
        .build();

    static final Schema GLOBAL_TABLE_WITNESS_GROUP_UPDATE = Schema.structureBuilder(ShapeId.from("com.amazonaws.dynamodb#GlobalTableWitnessGroupUpdate"))
             .putMember("Create", Schemas.CREATE_GLOBAL_TABLE_WITNESS_GROUP_MEMBER_ACTION)
             .putMember("Delete", Schemas.DELETE_GLOBAL_TABLE_WITNESS_GROUP_MEMBER_ACTION)
             .builderSupplier(GlobalTableWitnessGroupUpdate::builder)
             .shapeClass(GlobalTableWitnessGroupUpdate.class)
             .build();

    static final Schema GLOBAL_TABLE_WITNESS_GROUP_UPDATE_LIST = Schema.listBuilder(ShapeId.from("com.amazonaws.dynamodb#GlobalTableWitnessGroupUpdateList"),
            LengthTrait.builder().min(1L).max(1L).build())
        .putMember("member", Schemas.GLOBAL_TABLE_WITNESS_GROUP_UPDATE)
        .build();

    static final Schema UPDATE_REPLICATION_GROUP_MEMBER_ACTION = Schema.structureBuilder(ShapeId.from("com.amazonaws.dynamodb#UpdateReplicationGroupMemberAction"))
             .putMember("RegionName", Schemas.REGION_NAME,
                     new RequiredTrait())
             .putMember("KMSMasterKeyId", Schemas.KMS_MASTER_KEY_ID)
             .putMember("ProvisionedThroughputOverride", Schemas.PROVISIONED_THROUGHPUT_OVERRIDE)
             .putMember("OnDemandThroughputOverride", Schemas.ON_DEMAND_THROUGHPUT_OVERRIDE)
             .putMember("GlobalSecondaryIndexes", Schemas.REPLICA_GLOBAL_SECONDARY_INDEX_LIST)
             .putMember("TableClassOverride", TableClass.$SCHEMA)
             .builderSupplier(UpdateReplicationGroupMemberAction::builder)
             .shapeClass(UpdateReplicationGroupMemberAction.class)
             .build();

    static final Schema REPLICATION_GROUP_UPDATE = Schema.structureBuilder(ShapeId.from("com.amazonaws.dynamodb#ReplicationGroupUpdate"))
             .putMember("Create", Schemas.CREATE_REPLICATION_GROUP_MEMBER_ACTION)
             .putMember("Update", Schemas.UPDATE_REPLICATION_GROUP_MEMBER_ACTION)
             .putMember("Delete", Schemas.DELETE_REPLICATION_GROUP_MEMBER_ACTION)
             .builderSupplier(ReplicationGroupUpdate::builder)
             .shapeClass(ReplicationGroupUpdate.class)
             .build();

    static final Schema REPLICATION_GROUP_UPDATE_LIST = Schema.listBuilder(ShapeId.from("com.amazonaws.dynamodb#ReplicationGroupUpdateList"),
            LengthTrait.builder().min(1L).build())
        .putMember("member", Schemas.REPLICATION_GROUP_UPDATE)
        .build();

    static final Schema VECTOR_INDEX_UPDATE = Schema.structureBuilder(ShapeId.from("com.amazonaws.dynamodb#VectorIndexUpdate"))
             .putMember("Create", Schemas.CREATE_VECTOR_INDEX_ACTION)
             .putMember("Delete", Schemas.DELETE_VECTOR_INDEX_ACTION)
             .builderSupplier(VectorIndexUpdate::builder)
             .shapeClass(VectorIndexUpdate.class)
             .build();

    static final Schema VECTOR_INDEX_UPDATE_LIST = Schema.listBuilder(ShapeId.from("com.amazonaws.dynamodb#VectorIndexUpdateList"))
        .putMember("member", Schemas.VECTOR_INDEX_UPDATE)
        .build();

    static final Schema UPDATE_TABLE_INPUT = Schema.structureBuilder(ShapeId.from("com.amazonaws.dynamodb#UpdateTableInput"))
             .putMember("AttributeDefinitions", Schemas.ATTRIBUTE_DEFINITIONS)
             .putMember("TableName", Schemas.TABLE_ARN,
                     new ContextParamTrait.Provider().createTrait(
                         ShapeId.from("smithy.rules#contextParam"),
                         Node.objectNodeBuilder()
                             .withMember("name", "ResourceArn")
                             .build()
                     ),
                     new RequiredTrait())
             .putMember("BillingMode", BillingMode.$SCHEMA)
             .putMember("ProvisionedThroughput", Schemas.PROVISIONED_THROUGHPUT)
             .putMember("GlobalSecondaryIndexUpdates", Schemas.GLOBAL_SECONDARY_INDEX_UPDATE_LIST)
             .putMember("StreamSpecification", Schemas.STREAM_SPECIFICATION)
             .putMember("SSESpecification", Schemas.SSE_SPECIFICATION)
             .putMember("ReplicaUpdates", Schemas.REPLICATION_GROUP_UPDATE_LIST)
             .putMember("TableClass", TableClass.$SCHEMA)
             .putMember("DeletionProtectionEnabled", Schemas.DELETION_PROTECTION_ENABLED)
             .putMember("MultiRegionConsistency", MultiRegionConsistency.$SCHEMA)
             .putMember("GlobalTableWitnessUpdates", Schemas.GLOBAL_TABLE_WITNESS_GROUP_UPDATE_LIST)
             .putMember("OnDemandThroughput", Schemas.ON_DEMAND_THROUGHPUT)
             .putMember("WarmThroughput", Schemas.WARM_THROUGHPUT)
             .putMember("GlobalTableSettingsReplicationMode", GlobalTableSettingsReplicationMode.$SCHEMA)
             .putMember("VectorIndexUpdates", Schemas.VECTOR_INDEX_UPDATE_LIST)
             .builderSupplier(UpdateTableInput::builder)
             .shapeClass(UpdateTableInput.class)
             .build();

    static final Schema UPDATE_TABLE_OUTPUT = Schema.structureBuilder(ShapeId.from("com.amazonaws.dynamodb#UpdateTableOutput"))
             .putMember("TableDescription", Schemas.TABLE_DESCRIPTION)
             .builderSupplier(UpdateTableOutput::builder)
             .shapeClass(UpdateTableOutput.class)
             .build();

    static final Schema GLOBAL_SECONDARY_INDEX_AUTO_SCALING_UPDATE = Schema.structureBuilder(ShapeId.from("com.amazonaws.dynamodb#GlobalSecondaryIndexAutoScalingUpdate"))
             .putMember("IndexName", Schemas.INDEX_NAME)
             .putMember("ProvisionedWriteCapacityAutoScalingUpdate", Schemas.AUTO_SCALING_SETTINGS_UPDATE)
             .builderSupplier(GlobalSecondaryIndexAutoScalingUpdate::builder)
             .shapeClass(GlobalSecondaryIndexAutoScalingUpdate.class)
             .build();

    static final Schema GLOBAL_SECONDARY_INDEX_AUTO_SCALING_UPDATE_LIST = Schema.listBuilder(ShapeId.from("com.amazonaws.dynamodb#GlobalSecondaryIndexAutoScalingUpdateList"),
            LengthTrait.builder().min(1L).build())
        .putMember("member", Schemas.GLOBAL_SECONDARY_INDEX_AUTO_SCALING_UPDATE)
        .build();

    static final Schema REPLICA_GLOBAL_SECONDARY_INDEX_AUTO_SCALING_UPDATE = Schema.structureBuilder(ShapeId.from("com.amazonaws.dynamodb#ReplicaGlobalSecondaryIndexAutoScalingUpdate"))
             .putMember("IndexName", Schemas.INDEX_NAME)
             .putMember("ProvisionedReadCapacityAutoScalingUpdate", Schemas.AUTO_SCALING_SETTINGS_UPDATE)
             .builderSupplier(ReplicaGlobalSecondaryIndexAutoScalingUpdate::builder)
             .shapeClass(ReplicaGlobalSecondaryIndexAutoScalingUpdate.class)
             .build();

    static final Schema REPLICA_GLOBAL_SECONDARY_INDEX_AUTO_SCALING_UPDATE_LIST = Schema.listBuilder(ShapeId.from("com.amazonaws.dynamodb#ReplicaGlobalSecondaryIndexAutoScalingUpdateList"))
        .putMember("member", Schemas.REPLICA_GLOBAL_SECONDARY_INDEX_AUTO_SCALING_UPDATE)
        .build();

    static final Schema REPLICA_AUTO_SCALING_UPDATE = Schema.structureBuilder(ShapeId.from("com.amazonaws.dynamodb#ReplicaAutoScalingUpdate"))
             .putMember("RegionName", Schemas.REGION_NAME,
                     new RequiredTrait())
             .putMember("ReplicaGlobalSecondaryIndexUpdates", Schemas.REPLICA_GLOBAL_SECONDARY_INDEX_AUTO_SCALING_UPDATE_LIST)
             .putMember("ReplicaProvisionedReadCapacityAutoScalingUpdate", Schemas.AUTO_SCALING_SETTINGS_UPDATE)
             .builderSupplier(ReplicaAutoScalingUpdate::builder)
             .shapeClass(ReplicaAutoScalingUpdate.class)
             .build();

    static final Schema REPLICA_AUTO_SCALING_UPDATE_LIST = Schema.listBuilder(ShapeId.from("com.amazonaws.dynamodb#ReplicaAutoScalingUpdateList"),
            LengthTrait.builder().min(1L).build())
        .putMember("member", Schemas.REPLICA_AUTO_SCALING_UPDATE)
        .build();

    static final Schema UPDATE_TABLE_REPLICA_AUTO_SCALING_INPUT = Schema.structureBuilder(ShapeId.from("com.amazonaws.dynamodb#UpdateTableReplicaAutoScalingInput"))
             .putMember("GlobalSecondaryIndexUpdates", Schemas.GLOBAL_SECONDARY_INDEX_AUTO_SCALING_UPDATE_LIST)
             .putMember("TableName", Schemas.TABLE_ARN,
                     new ContextParamTrait.Provider().createTrait(
                         ShapeId.from("smithy.rules#contextParam"),
                         Node.objectNodeBuilder()
                             .withMember("name", "ResourceArn")
                             .build()
                     ),
                     new RequiredTrait())
             .putMember("ProvisionedWriteCapacityAutoScalingUpdate", Schemas.AUTO_SCALING_SETTINGS_UPDATE)
             .putMember("ReplicaUpdates", Schemas.REPLICA_AUTO_SCALING_UPDATE_LIST)
             .builderSupplier(UpdateTableReplicaAutoScalingInput::builder)
             .shapeClass(UpdateTableReplicaAutoScalingInput.class)
             .build();

    static final Schema UPDATE_TABLE_REPLICA_AUTO_SCALING_OUTPUT = Schema.structureBuilder(ShapeId.from("com.amazonaws.dynamodb#UpdateTableReplicaAutoScalingOutput"))
             .putMember("TableAutoScalingDescription", Schemas.TABLE_AUTO_SCALING_DESCRIPTION)
             .builderSupplier(UpdateTableReplicaAutoScalingOutput::builder)
             .shapeClass(UpdateTableReplicaAutoScalingOutput.class)
             .build();

    static final Schema TIME_TO_LIVE_ENABLED = Schema.createBoolean(ShapeId.from("com.amazonaws.dynamodb#TimeToLiveEnabled"));
    static final Schema TIME_TO_LIVE_SPECIFICATION = Schema.structureBuilder(ShapeId.from("com.amazonaws.dynamodb#TimeToLiveSpecification"))
             .putMember("Enabled", Schemas.TIME_TO_LIVE_ENABLED,
                     new RequiredTrait())
             .putMember("AttributeName", Schemas.TIME_TO_LIVE_ATTRIBUTE_NAME,
                     new RequiredTrait())
             .builderSupplier(TimeToLiveSpecification::builder)
             .shapeClass(TimeToLiveSpecification.class)
             .build();

    static final Schema UPDATE_TIME_TO_LIVE_INPUT = Schema.structureBuilder(ShapeId.from("com.amazonaws.dynamodb#UpdateTimeToLiveInput"))
             .putMember("TableName", Schemas.TABLE_ARN,
                     new ContextParamTrait.Provider().createTrait(
                         ShapeId.from("smithy.rules#contextParam"),
                         Node.objectNodeBuilder()
                             .withMember("name", "ResourceArn")
                             .build()
                     ),
                     new RequiredTrait())
             .putMember("TimeToLiveSpecification", Schemas.TIME_TO_LIVE_SPECIFICATION,
                     new RequiredTrait())
             .builderSupplier(UpdateTimeToLiveInput::builder)
             .shapeClass(UpdateTimeToLiveInput.class)
             .build();

    static final Schema UPDATE_TIME_TO_LIVE_OUTPUT = Schema.structureBuilder(ShapeId.from("com.amazonaws.dynamodb#UpdateTimeToLiveOutput"))
             .putMember("TimeToLiveSpecification", Schemas.TIME_TO_LIVE_SPECIFICATION)
             .builderSupplier(UpdateTimeToLiveOutput::builder)
             .shapeClass(UpdateTimeToLiveOutput.class)
             .build();

    static {
            ATTRIBUTE_VALUE_BUILDER
                .putMember("S", Schemas.STRING_ATTRIBUTE_VALUE)
                .putMember("N", Schemas.NUMBER_ATTRIBUTE_VALUE)
                .putMember("B", Schemas.BINARY_ATTRIBUTE_VALUE)
                .putMember("SS", Schemas.STRING_SET_ATTRIBUTE_VALUE)
                .putMember("NS", Schemas.NUMBER_SET_ATTRIBUTE_VALUE)
                .putMember("BS", Schemas.BINARY_SET_ATTRIBUTE_VALUE)
                .putMember("M", Schemas.MAP_ATTRIBUTE_VALUE_BUILDER)
                .putMember("L", Schemas.LIST_ATTRIBUTE_VALUE_BUILDER)
                .putMember("NULL", Schemas.NULL_ATTRIBUTE_VALUE)
                .putMember("BOOL", Schemas.BOOLEAN_ATTRIBUTE_VALUE)
                .builderSupplier(AttributeValue::builder)
                .shapeClass(AttributeValue.class)
                .build();

            LIST_ATTRIBUTE_VALUE_BUILDER
                .putMember("member", Schemas.ATTRIBUTE_VALUE_BUILDER)
                .build();

            MAP_ATTRIBUTE_VALUE_BUILDER
                .putMember("key", Schemas.ATTRIBUTE_NAME)
                .putMember("value", Schemas.ATTRIBUTE_VALUE_BUILDER)
                .build();

            ATTRIBUTE_MAP_BUILDER
                .putMember("key", Schemas.ATTRIBUTE_NAME)
                .putMember("value", Schemas.ATTRIBUTE_VALUE_BUILDER)
                .build();

            ATTRIBUTE_VALUE_LIST_BUILDER
                .putMember("member", Schemas.ATTRIBUTE_VALUE_BUILDER)
                .build();

            ATTRIBUTE_VALUE_UPDATE_BUILDER
                .putMember("Value", Schemas.ATTRIBUTE_VALUE_BUILDER)
                .putMember("Action", AttributeAction.$SCHEMA)
                .builderSupplier(AttributeValueUpdate::builder)
                .shapeClass(AttributeValueUpdate.class)
                .build();

            EXPRESSION_ATTRIBUTE_VALUE_MAP_BUILDER
                .putMember("key", Schemas.EXPRESSION_ATTRIBUTE_VALUE_VARIABLE)
                .putMember("value", Schemas.ATTRIBUTE_VALUE_BUILDER)
                .build();

            ITEM_COLLECTION_KEY_ATTRIBUTE_MAP_BUILDER
                .putMember("key", Schemas.ATTRIBUTE_NAME)
                .putMember("value", Schemas.ATTRIBUTE_VALUE_BUILDER)
                .build();

            KEY_BUILDER
                .putMember("key", Schemas.ATTRIBUTE_NAME)
                .putMember("value", Schemas.ATTRIBUTE_VALUE_BUILDER)
                .build();

            PREPARED_STATEMENT_PARAMETERS_BUILDER
                .putMember("member", Schemas.ATTRIBUTE_VALUE_BUILDER)
                .build();

            PUT_ITEM_INPUT_ATTRIBUTE_MAP_BUILDER
                .putMember("key", Schemas.ATTRIBUTE_NAME)
                .putMember("value", Schemas.ATTRIBUTE_VALUE_BUILDER)
                .build();

            SEARCH_VECTOR_LIST_BUILDER
                .putMember("member", Schemas.ATTRIBUTE_VALUE_BUILDER)
                .build();

            ATTRIBUTE_UPDATES_BUILDER
                .putMember("key", Schemas.ATTRIBUTE_NAME)
                .putMember("value", Schemas.ATTRIBUTE_VALUE_UPDATE_BUILDER)
                .build();

            BATCH_STATEMENT_ERROR_BUILDER
                .putMember("Code", BatchStatementErrorCodeEnum.$SCHEMA)
                .putMember("Message", Schemas.STRING)
                .putMember("Item", Schemas.ATTRIBUTE_MAP_BUILDER)
                .builderSupplier(BatchStatementError::builder)
                .shapeClass(BatchStatementError.class)
                .build();

            BATCH_STATEMENT_REQUEST_BUILDER
                .putMember("Statement", Schemas.PARTI_QL_STATEMENT,
                        new RequiredTrait())
                .putMember("Parameters", Schemas.PREPARED_STATEMENT_PARAMETERS_BUILDER)
                .putMember("ConsistentRead", Schemas.CONSISTENT_READ)
                .putMember("ReturnValuesOnConditionCheckFailure", ReturnValuesOnConditionCheckFailure.$SCHEMA)
                .builderSupplier(BatchStatementRequest::builder)
                .shapeClass(BatchStatementRequest.class)
                .build();

            CANCELLATION_REASON_BUILDER
                .putMember("Item", Schemas.ATTRIBUTE_MAP_BUILDER)
                .putMember("Code", Schemas.CODE)
                .putMember("Message", Schemas.ERROR_MESSAGE)
                .builderSupplier(CancellationReason::builder)
                .shapeClass(CancellationReason.class)
                .build();

            CONDITION_BUILDER
                .putMember("AttributeValueList", Schemas.ATTRIBUTE_VALUE_LIST_BUILDER)
                .putMember("ComparisonOperator", ComparisonOperator.$SCHEMA,
                        new RequiredTrait())
                .builderSupplier(Condition::builder)
                .shapeClass(Condition.class)
                .build();

            CONDITIONAL_CHECK_FAILED_EXCEPTION_BUILDER
                .putMember("message", Schemas.ERROR_MESSAGE)
                .putMember("Item", Schemas.ATTRIBUTE_MAP_BUILDER)
                .builderSupplier(ConditionalCheckFailedException::builder)
                .shapeClass(ConditionalCheckFailedException.class)
                .build();

            DELETE_REQUEST_BUILDER
                .putMember("Key", Schemas.KEY_BUILDER,
                        new RequiredTrait())
                .builderSupplier(DeleteRequest::builder)
                .shapeClass(DeleteRequest.class)
                .build();

            EXECUTE_STATEMENT_INPUT_BUILDER
                .putMember("Statement", Schemas.PARTI_QL_STATEMENT,
                        new RequiredTrait())
                .putMember("Parameters", Schemas.PREPARED_STATEMENT_PARAMETERS_BUILDER)
                .putMember("ConsistentRead", Schemas.CONSISTENT_READ)
                .putMember("NextToken", Schemas.PARTI_QL_NEXT_TOKEN)
                .putMember("ReturnConsumedCapacity", ReturnConsumedCapacity.$SCHEMA)
                .putMember("Limit", Schemas.POSITIVE_INTEGER_OBJECT)
                .putMember("ReturnValuesOnConditionCheckFailure", ReturnValuesOnConditionCheckFailure.$SCHEMA)
                .builderSupplier(ExecuteStatementInput::builder)
                .shapeClass(ExecuteStatementInput.class)
                .build();

            GET_BUILDER
                .putMember("Key", Schemas.KEY_BUILDER,
                        new RequiredTrait())
                .putMember("TableName", Schemas.TABLE_ARN,
                        new RequiredTrait())
                .putMember("ProjectionExpression", Schemas.PROJECTION_EXPRESSION)
                .putMember("ExpressionAttributeNames", Schemas.EXPRESSION_ATTRIBUTE_NAME_MAP)
                .builderSupplier(Get::builder)
                .shapeClass(Get.class)
                .build();

            GET_ITEM_INPUT_BUILDER
                .putMember("TableName", Schemas.TABLE_ARN,
                        new ContextParamTrait.Provider().createTrait(
                            ShapeId.from("smithy.rules#contextParam"),
                            Node.objectNodeBuilder()
                                .withMember("name", "ResourceArn")
                                .build()
                        ),
                        new RequiredTrait())
                .putMember("Key", Schemas.KEY_BUILDER,
                        new RequiredTrait())
                .putMember("AttributesToGet", Schemas.ATTRIBUTE_NAME_LIST)
                .putMember("ConsistentRead", Schemas.CONSISTENT_READ)
                .putMember("ReturnConsumedCapacity", ReturnConsumedCapacity.$SCHEMA)
                .putMember("ProjectionExpression", Schemas.PROJECTION_EXPRESSION)
                .putMember("ExpressionAttributeNames", Schemas.EXPRESSION_ATTRIBUTE_NAME_MAP)
                .builderSupplier(GetItemInput::builder)
                .shapeClass(GetItemInput.class)
                .build();

            GET_ITEM_OUTPUT_BUILDER
                .putMember("Item", Schemas.ATTRIBUTE_MAP_BUILDER)
                .putMember("ConsumedCapacity", Schemas.CONSUMED_CAPACITY)
                .builderSupplier(GetItemOutput::builder)
                .shapeClass(GetItemOutput.class)
                .build();

            ITEM_COLLECTION_METRICS_BUILDER
                .putMember("ItemCollectionKey", Schemas.ITEM_COLLECTION_KEY_ATTRIBUTE_MAP_BUILDER)
                .putMember("SizeEstimateRangeGB", Schemas.ITEM_COLLECTION_SIZE_ESTIMATE_RANGE)
                .builderSupplier(ItemCollectionMetrics::builder)
                .shapeClass(ItemCollectionMetrics.class)
                .build();

            ITEM_LIST_BUILDER
                .putMember("member", Schemas.ATTRIBUTE_MAP_BUILDER)
                .build();

            ITEM_RESPONSE_BUILDER
                .putMember("Item", Schemas.ATTRIBUTE_MAP_BUILDER)
                .builderSupplier(ItemResponse::builder)
                .shapeClass(ItemResponse.class)
                .build();

            KEY_LIST_BUILDER
                .putMember("member", Schemas.KEY_BUILDER)
                .build();

            PARAMETERIZED_STATEMENT_BUILDER
                .putMember("Statement", Schemas.PARTI_QL_STATEMENT,
                        new RequiredTrait())
                .putMember("Parameters", Schemas.PREPARED_STATEMENT_PARAMETERS_BUILDER)
                .putMember("ReturnValuesOnConditionCheckFailure", ReturnValuesOnConditionCheckFailure.$SCHEMA)
                .builderSupplier(ParameterizedStatement::builder)
                .shapeClass(ParameterizedStatement.class)
                .build();

            PUT_REQUEST_BUILDER
                .putMember("Item", Schemas.PUT_ITEM_INPUT_ATTRIBUTE_MAP_BUILDER,
                        new RequiredTrait())
                .builderSupplier(PutRequest::builder)
                .shapeClass(PutRequest.class)
                .build();

            SEARCH_RESULT_ITEM_BUILDER
                .putMember("Item", Schemas.ATTRIBUTE_MAP_BUILDER)
                .putMember("Score", Schemas.SCORE_NUMBER,
                        new DefaultTrait(Node.from(0L)))
                .builderSupplier(SearchResultItem::builder)
                .shapeClass(SearchResultItem.class)
                .build();

            BATCH_GET_RESPONSE_MAP_BUILDER
                .putMember("key", Schemas.TABLE_ARN)
                .putMember("value", Schemas.ITEM_LIST_BUILDER)
                .build();

            CANCELLATION_REASON_LIST_BUILDER
                .putMember("member", Schemas.CANCELLATION_REASON_BUILDER)
                .build();

            FILTER_CONDITION_MAP_BUILDER
                .putMember("key", Schemas.ATTRIBUTE_NAME)
                .putMember("value", Schemas.CONDITION_BUILDER)
                .build();

            ITEM_COLLECTION_METRICS_MULTIPLE_BUILDER
                .putMember("member", Schemas.ITEM_COLLECTION_METRICS_BUILDER)
                .build();

            ITEM_RESPONSE_LIST_BUILDER
                .putMember("member", Schemas.ITEM_RESPONSE_BUILDER)
                .build();

            KEY_CONDITIONS_BUILDER
                .putMember("key", Schemas.ATTRIBUTE_NAME)
                .putMember("value", Schemas.CONDITION_BUILDER)
                .build();

            KEYS_AND_ATTRIBUTES_BUILDER
                .putMember("Keys", Schemas.KEY_LIST_BUILDER,
                        new RequiredTrait())
                .putMember("AttributesToGet", Schemas.ATTRIBUTE_NAME_LIST)
                .putMember("ConsistentRead", Schemas.CONSISTENT_READ)
                .putMember("ProjectionExpression", Schemas.PROJECTION_EXPRESSION)
                .putMember("ExpressionAttributeNames", Schemas.EXPRESSION_ATTRIBUTE_NAME_MAP)
                .builderSupplier(KeysAndAttributes::builder)
                .shapeClass(KeysAndAttributes.class)
                .build();

            PARAMETERIZED_STATEMENTS_BUILDER
                .putMember("member", Schemas.PARAMETERIZED_STATEMENT_BUILDER)
                .build();

            PARTI_QL_BATCH_REQUEST_BUILDER
                .putMember("member", Schemas.BATCH_STATEMENT_REQUEST_BUILDER)
                .build();

            SEARCH_RESULT_LIST_BUILDER
                .putMember("member", Schemas.SEARCH_RESULT_ITEM_BUILDER)
                .build();

            TRANSACT_GET_ITEM_BUILDER
                .putMember("Get", Schemas.GET_BUILDER,
                        new RequiredTrait())
                .builderSupplier(TransactGetItem::builder)
                .shapeClass(TransactGetItem.class)
                .build();

            BATCH_EXECUTE_STATEMENT_INPUT_BUILDER
                .putMember("Statements", Schemas.PARTI_QL_BATCH_REQUEST_BUILDER,
                        new RequiredTrait())
                .putMember("ReturnConsumedCapacity", ReturnConsumedCapacity.$SCHEMA)
                .builderSupplier(BatchExecuteStatementInput::builder)
                .shapeClass(BatchExecuteStatementInput.class)
                .build();

            BATCH_GET_REQUEST_MAP_BUILDER
                .putMember("key", Schemas.TABLE_ARN)
                .putMember("value", Schemas.KEYS_AND_ATTRIBUTES_BUILDER)
                .build();

            EXECUTE_TRANSACTION_INPUT_BUILDER
                .putMember("TransactStatements", Schemas.PARAMETERIZED_STATEMENTS_BUILDER,
                        new RequiredTrait())
                .putMember("ClientRequestToken", Schemas.CLIENT_REQUEST_TOKEN,
                        new IdempotencyTokenTrait())
                .putMember("ReturnConsumedCapacity", ReturnConsumedCapacity.$SCHEMA)
                .builderSupplier(ExecuteTransactionInput::builder)
                .shapeClass(ExecuteTransactionInput.class)
                .build();

            EXECUTE_TRANSACTION_OUTPUT_BUILDER
                .putMember("Responses", Schemas.ITEM_RESPONSE_LIST_BUILDER)
                .putMember("ConsumedCapacity", Schemas.CONSUMED_CAPACITY_MULTIPLE)
                .builderSupplier(ExecuteTransactionOutput::builder)
                .shapeClass(ExecuteTransactionOutput.class)
                .build();

            ITEM_COLLECTION_METRICS_PER_TABLE_BUILDER
                .putMember("key", Schemas.TABLE_ARN)
                .putMember("value", Schemas.ITEM_COLLECTION_METRICS_MULTIPLE_BUILDER)
                .build();

            SEARCH_VECTORS_OUTPUT_BUILDER
                .putMember("ConsumedCapacity", Schemas.VECTOR_CAPACITY)
                .putMember("SearchResults", Schemas.SEARCH_RESULT_LIST_BUILDER)
                .builderSupplier(SearchVectorsOutput::builder)
                .shapeClass(SearchVectorsOutput.class)
                .build();

            TRANSACT_GET_ITEM_LIST_BUILDER
                .putMember("member", Schemas.TRANSACT_GET_ITEM_BUILDER)
                .build();

            TRANSACT_GET_ITEMS_OUTPUT_BUILDER
                .putMember("ConsumedCapacity", Schemas.CONSUMED_CAPACITY_MULTIPLE)
                .putMember("Responses", Schemas.ITEM_RESPONSE_LIST_BUILDER)
                .builderSupplier(TransactGetItemsOutput::builder)
                .shapeClass(TransactGetItemsOutput.class)
                .build();

            TRANSACTION_CANCELED_EXCEPTION_BUILDER
                .putMember("Message", Schemas.ERROR_MESSAGE)
                .putMember("CancellationReasons", Schemas.CANCELLATION_REASON_LIST_BUILDER)
                .builderSupplier(TransactionCanceledException::builder)
                .shapeClass(TransactionCanceledException.class)
                .build();

            BATCH_GET_ITEM_INPUT_BUILDER
                .putMember("RequestItems", Schemas.BATCH_GET_REQUEST_MAP_BUILDER,
                        new RequiredTrait())
                .putMember("ReturnConsumedCapacity", ReturnConsumedCapacity.$SCHEMA)
                .builderSupplier(BatchGetItemInput::builder)
                .shapeClass(BatchGetItemInput.class)
                .build();

            EXPECTED_ATTRIBUTE_VALUE_BUILDER
                .putMember("Value", Schemas.ATTRIBUTE_VALUE_BUILDER)
                .putMember("Exists", Schemas.BOOLEAN_OBJECT)
                .putMember("ComparisonOperator", ComparisonOperator.$SCHEMA)
                .putMember("AttributeValueList", Schemas.ATTRIBUTE_VALUE_LIST_BUILDER)
                .builderSupplier(ExpectedAttributeValue::builder)
                .shapeClass(ExpectedAttributeValue.class)
                .build();

            TRANSACT_GET_ITEMS_INPUT_BUILDER
                .putMember("TransactItems", Schemas.TRANSACT_GET_ITEM_LIST_BUILDER,
                        new RequiredTrait())
                .putMember("ReturnConsumedCapacity", ReturnConsumedCapacity.$SCHEMA)
                .builderSupplier(TransactGetItemsInput::builder)
                .shapeClass(TransactGetItemsInput.class)
                .build();

            TRANSACT_WRITE_ITEMS_OUTPUT_BUILDER
                .putMember("ConsumedCapacity", Schemas.CONSUMED_CAPACITY_MULTIPLE)
                .putMember("ItemCollectionMetrics", Schemas.ITEM_COLLECTION_METRICS_PER_TABLE_BUILDER)
                .builderSupplier(TransactWriteItemsOutput::builder)
                .shapeClass(TransactWriteItemsOutput.class)
                .build();

            CONDITION_CHECK_BUILDER
                .putMember("Key", Schemas.KEY_BUILDER,
                        new RequiredTrait())
                .putMember("TableName", Schemas.TABLE_ARN,
                        new RequiredTrait())
                .putMember("ConditionExpression", Schemas.CONDITION_EXPRESSION,
                        new RequiredTrait())
                .putMember("ExpressionAttributeNames", Schemas.EXPRESSION_ATTRIBUTE_NAME_MAP)
                .putMember("ExpressionAttributeValues", Schemas.EXPRESSION_ATTRIBUTE_VALUE_MAP_BUILDER)
                .putMember("ReturnValuesOnConditionCheckFailure", ReturnValuesOnConditionCheckFailure.$SCHEMA)
                .builderSupplier(ConditionCheck::builder)
                .shapeClass(ConditionCheck.class)
                .build();

            DELETE_BUILDER
                .putMember("Key", Schemas.KEY_BUILDER,
                        new RequiredTrait())
                .putMember("TableName", Schemas.TABLE_ARN,
                        new RequiredTrait())
                .putMember("ConditionExpression", Schemas.CONDITION_EXPRESSION)
                .putMember("ExpressionAttributeNames", Schemas.EXPRESSION_ATTRIBUTE_NAME_MAP)
                .putMember("ExpressionAttributeValues", Schemas.EXPRESSION_ATTRIBUTE_VALUE_MAP_BUILDER)
                .putMember("ReturnValuesOnConditionCheckFailure", ReturnValuesOnConditionCheckFailure.$SCHEMA)
                .builderSupplier(Delete::builder)
                .shapeClass(Delete.class)
                .build();

            PUT_BUILDER
                .putMember("Item", Schemas.PUT_ITEM_INPUT_ATTRIBUTE_MAP_BUILDER,
                        new RequiredTrait())
                .putMember("TableName", Schemas.TABLE_ARN,
                        new RequiredTrait())
                .putMember("ConditionExpression", Schemas.CONDITION_EXPRESSION)
                .putMember("ExpressionAttributeNames", Schemas.EXPRESSION_ATTRIBUTE_NAME_MAP)
                .putMember("ExpressionAttributeValues", Schemas.EXPRESSION_ATTRIBUTE_VALUE_MAP_BUILDER)
                .putMember("ReturnValuesOnConditionCheckFailure", ReturnValuesOnConditionCheckFailure.$SCHEMA)
                .builderSupplier(Put::builder)
                .shapeClass(Put.class)
                .build();

            SEARCH_VECTORS_INPUT_BUILDER
                .putMember("TableName", Schemas.TABLE_ARN,
                        new ContextParamTrait.Provider().createTrait(
                            ShapeId.from("smithy.rules#contextParam"),
                            Node.objectNodeBuilder()
                                .withMember("name", "ResourceArn")
                                .build()
                        ),
                        new RequiredTrait())
                .putMember("IndexName", Schemas.INDEX_NAME,
                        new RequiredTrait())
                .putMember("ReturnConsumedCapacity", ReturnConsumedCapacity.$SCHEMA)
                .putMember("ExpressionAttributeNames", Schemas.EXPRESSION_ATTRIBUTE_NAME_MAP)
                .putMember("ExpressionAttributeValues", Schemas.EXPRESSION_ATTRIBUTE_VALUE_MAP_BUILDER)
                .putMember("ProjectionExpression", Schemas.PROJECTION_EXPRESSION)
                .putMember("SearchVector", Schemas.SEARCH_VECTOR_LIST_BUILDER,
                        new RequiredTrait())
                .putMember("SearchConditionExpression", Schemas.STRING)
                .putMember("TopK", Schemas.TOP_K_INTEGER,
                        new RequiredTrait())
                .builderSupplier(SearchVectorsInput::builder)
                .shapeClass(SearchVectorsInput.class)
                .build();

            UPDATE_BUILDER
                .putMember("Key", Schemas.KEY_BUILDER,
                        new RequiredTrait())
                .putMember("UpdateExpression", Schemas.UPDATE_EXPRESSION,
                        new RequiredTrait())
                .putMember("TableName", Schemas.TABLE_ARN,
                        new RequiredTrait())
                .putMember("ConditionExpression", Schemas.CONDITION_EXPRESSION)
                .putMember("ExpressionAttributeNames", Schemas.EXPRESSION_ATTRIBUTE_NAME_MAP)
                .putMember("ExpressionAttributeValues", Schemas.EXPRESSION_ATTRIBUTE_VALUE_MAP_BUILDER)
                .putMember("ReturnValuesOnConditionCheckFailure", ReturnValuesOnConditionCheckFailure.$SCHEMA)
                .builderSupplier(Update::builder)
                .shapeClass(Update.class)
                .build();

            BATCH_STATEMENT_RESPONSE_BUILDER
                .putMember("Error", Schemas.BATCH_STATEMENT_ERROR_BUILDER)
                .putMember("TableName", Schemas.TABLE_NAME)
                .putMember("Item", Schemas.ATTRIBUTE_MAP_BUILDER)
                .builderSupplier(BatchStatementResponse::builder)
                .shapeClass(BatchStatementResponse.class)
                .build();

            DELETE_ITEM_OUTPUT_BUILDER
                .putMember("Attributes", Schemas.ATTRIBUTE_MAP_BUILDER)
                .putMember("ConsumedCapacity", Schemas.CONSUMED_CAPACITY)
                .putMember("ItemCollectionMetrics", Schemas.ITEM_COLLECTION_METRICS_BUILDER)
                .builderSupplier(DeleteItemOutput::builder)
                .shapeClass(DeleteItemOutput.class)
                .build();

            EXECUTE_STATEMENT_OUTPUT_BUILDER
                .putMember("Items", Schemas.ITEM_LIST_BUILDER)
                .putMember("NextToken", Schemas.PARTI_QL_NEXT_TOKEN)
                .putMember("ConsumedCapacity", Schemas.CONSUMED_CAPACITY)
                .putMember("LastEvaluatedKey", Schemas.KEY_BUILDER)
                .builderSupplier(ExecuteStatementOutput::builder)
                .shapeClass(ExecuteStatementOutput.class)
                .build();

            EXPECTED_ATTRIBUTE_MAP_BUILDER
                .putMember("key", Schemas.ATTRIBUTE_NAME)
                .putMember("value", Schemas.EXPECTED_ATTRIBUTE_VALUE_BUILDER)
                .build();

            PUT_ITEM_OUTPUT_BUILDER
                .putMember("Attributes", Schemas.ATTRIBUTE_MAP_BUILDER)
                .putMember("ConsumedCapacity", Schemas.CONSUMED_CAPACITY)
                .putMember("ItemCollectionMetrics", Schemas.ITEM_COLLECTION_METRICS_BUILDER)
                .builderSupplier(PutItemOutput::builder)
                .shapeClass(PutItemOutput.class)
                .build();

            QUERY_OUTPUT_BUILDER
                .putMember("Items", Schemas.ITEM_LIST_BUILDER)
                .putMember("Count", Schemas.INTEGER,
                        new DefaultTrait(Node.from(0L)))
                .putMember("ScannedCount", Schemas.INTEGER,
                        new DefaultTrait(Node.from(0L)))
                .putMember("LastEvaluatedKey", Schemas.KEY_BUILDER)
                .putMember("ConsumedCapacity", Schemas.CONSUMED_CAPACITY)
                .builderSupplier(QueryOutput::builder)
                .shapeClass(QueryOutput.class)
                .build();

            SCAN_OUTPUT_BUILDER
                .putMember("Items", Schemas.ITEM_LIST_BUILDER)
                .putMember("Count", Schemas.INTEGER,
                        new DefaultTrait(Node.from(0L)))
                .putMember("ScannedCount", Schemas.INTEGER,
                        new DefaultTrait(Node.from(0L)))
                .putMember("LastEvaluatedKey", Schemas.KEY_BUILDER)
                .putMember("ConsumedCapacity", Schemas.CONSUMED_CAPACITY)
                .builderSupplier(ScanOutput::builder)
                .shapeClass(ScanOutput.class)
                .build();

            UPDATE_ITEM_OUTPUT_BUILDER
                .putMember("Attributes", Schemas.ATTRIBUTE_MAP_BUILDER)
                .putMember("ConsumedCapacity", Schemas.CONSUMED_CAPACITY)
                .putMember("ItemCollectionMetrics", Schemas.ITEM_COLLECTION_METRICS_BUILDER)
                .builderSupplier(UpdateItemOutput::builder)
                .shapeClass(UpdateItemOutput.class)
                .build();

            WRITE_REQUEST_BUILDER
                .putMember("PutRequest", Schemas.PUT_REQUEST_BUILDER)
                .putMember("DeleteRequest", Schemas.DELETE_REQUEST_BUILDER)
                .builderSupplier(WriteRequest::builder)
                .shapeClass(WriteRequest.class)
                .build();

            PARTI_QL_BATCH_RESPONSE_BUILDER
                .putMember("member", Schemas.BATCH_STATEMENT_RESPONSE_BUILDER)
                .build();

            WRITE_REQUESTS_BUILDER
                .putMember("member", Schemas.WRITE_REQUEST_BUILDER)
                .build();

            BATCH_EXECUTE_STATEMENT_OUTPUT_BUILDER
                .putMember("Responses", Schemas.PARTI_QL_BATCH_RESPONSE_BUILDER)
                .putMember("ConsumedCapacity", Schemas.CONSUMED_CAPACITY_MULTIPLE)
                .builderSupplier(BatchExecuteStatementOutput::builder)
                .shapeClass(BatchExecuteStatementOutput.class)
                .build();

            BATCH_GET_ITEM_OUTPUT_BUILDER
                .putMember("Responses", Schemas.BATCH_GET_RESPONSE_MAP_BUILDER)
                .putMember("UnprocessedKeys", Schemas.BATCH_GET_REQUEST_MAP_BUILDER)
                .putMember("ConsumedCapacity", Schemas.CONSUMED_CAPACITY_MULTIPLE)
                .builderSupplier(BatchGetItemOutput::builder)
                .shapeClass(BatchGetItemOutput.class)
                .build();

            BATCH_WRITE_ITEM_REQUEST_MAP_BUILDER
                .putMember("key", Schemas.TABLE_ARN)
                .putMember("value", Schemas.WRITE_REQUESTS_BUILDER)
                .build();

            SCAN_INPUT_BUILDER
                .putMember("TableName", Schemas.TABLE_ARN,
                        new ContextParamTrait.Provider().createTrait(
                            ShapeId.from("smithy.rules#contextParam"),
                            Node.objectNodeBuilder()
                                .withMember("name", "ResourceArn")
                                .build()
                        ),
                        new RequiredTrait())
                .putMember("IndexName", Schemas.INDEX_NAME)
                .putMember("AttributesToGet", Schemas.ATTRIBUTE_NAME_LIST)
                .putMember("Limit", Schemas.POSITIVE_INTEGER_OBJECT)
                .putMember("Select", Select.$SCHEMA)
                .putMember("ScanFilter", Schemas.FILTER_CONDITION_MAP_BUILDER)
                .putMember("ConditionalOperator", ConditionalOperator.$SCHEMA)
                .putMember("ExclusiveStartKey", Schemas.KEY_BUILDER)
                .putMember("ReturnConsumedCapacity", ReturnConsumedCapacity.$SCHEMA)
                .putMember("TotalSegments", Schemas.SCAN_TOTAL_SEGMENTS)
                .putMember("Segment", Schemas.SCAN_SEGMENT)
                .putMember("ProjectionExpression", Schemas.PROJECTION_EXPRESSION)
                .putMember("FilterExpression", Schemas.CONDITION_EXPRESSION)
                .putMember("ExpressionAttributeNames", Schemas.EXPRESSION_ATTRIBUTE_NAME_MAP)
                .putMember("ExpressionAttributeValues", Schemas.EXPRESSION_ATTRIBUTE_VALUE_MAP_BUILDER)
                .putMember("ConsistentRead", Schemas.CONSISTENT_READ)
                .builderSupplier(ScanInput::builder)
                .shapeClass(ScanInput.class)
                .build();

            BATCH_WRITE_ITEM_INPUT_BUILDER
                .putMember("RequestItems", Schemas.BATCH_WRITE_ITEM_REQUEST_MAP_BUILDER,
                        new RequiredTrait())
                .putMember("ReturnConsumedCapacity", ReturnConsumedCapacity.$SCHEMA)
                .putMember("ReturnItemCollectionMetrics", ReturnItemCollectionMetrics.$SCHEMA)
                .builderSupplier(BatchWriteItemInput::builder)
                .shapeClass(BatchWriteItemInput.class)
                .build();

            DELETE_ITEM_INPUT_BUILDER
                .putMember("TableName", Schemas.TABLE_ARN,
                        new ContextParamTrait.Provider().createTrait(
                            ShapeId.from("smithy.rules#contextParam"),
                            Node.objectNodeBuilder()
                                .withMember("name", "ResourceArn")
                                .build()
                        ),
                        new RequiredTrait())
                .putMember("Key", Schemas.KEY_BUILDER,
                        new RequiredTrait())
                .putMember("Expected", Schemas.EXPECTED_ATTRIBUTE_MAP_BUILDER)
                .putMember("ConditionalOperator", ConditionalOperator.$SCHEMA)
                .putMember("ReturnValues", ReturnValue.$SCHEMA)
                .putMember("ReturnConsumedCapacity", ReturnConsumedCapacity.$SCHEMA)
                .putMember("ReturnItemCollectionMetrics", ReturnItemCollectionMetrics.$SCHEMA)
                .putMember("ConditionExpression", Schemas.CONDITION_EXPRESSION)
                .putMember("ExpressionAttributeNames", Schemas.EXPRESSION_ATTRIBUTE_NAME_MAP)
                .putMember("ExpressionAttributeValues", Schemas.EXPRESSION_ATTRIBUTE_VALUE_MAP_BUILDER)
                .putMember("ReturnValuesOnConditionCheckFailure", ReturnValuesOnConditionCheckFailure.$SCHEMA)
                .builderSupplier(DeleteItemInput::builder)
                .shapeClass(DeleteItemInput.class)
                .build();

            PUT_ITEM_INPUT_BUILDER
                .putMember("TableName", Schemas.TABLE_ARN,
                        new ContextParamTrait.Provider().createTrait(
                            ShapeId.from("smithy.rules#contextParam"),
                            Node.objectNodeBuilder()
                                .withMember("name", "ResourceArn")
                                .build()
                        ),
                        new RequiredTrait())
                .putMember("Item", Schemas.PUT_ITEM_INPUT_ATTRIBUTE_MAP_BUILDER,
                        new RequiredTrait())
                .putMember("Expected", Schemas.EXPECTED_ATTRIBUTE_MAP_BUILDER)
                .putMember("ReturnValues", ReturnValue.$SCHEMA)
                .putMember("ReturnConsumedCapacity", ReturnConsumedCapacity.$SCHEMA)
                .putMember("ReturnItemCollectionMetrics", ReturnItemCollectionMetrics.$SCHEMA)
                .putMember("ConditionalOperator", ConditionalOperator.$SCHEMA)
                .putMember("ConditionExpression", Schemas.CONDITION_EXPRESSION)
                .putMember("ExpressionAttributeNames", Schemas.EXPRESSION_ATTRIBUTE_NAME_MAP)
                .putMember("ExpressionAttributeValues", Schemas.EXPRESSION_ATTRIBUTE_VALUE_MAP_BUILDER)
                .putMember("ReturnValuesOnConditionCheckFailure", ReturnValuesOnConditionCheckFailure.$SCHEMA)
                .builderSupplier(PutItemInput::builder)
                .shapeClass(PutItemInput.class)
                .build();

            QUERY_INPUT_BUILDER
                .putMember("TableName", Schemas.TABLE_ARN,
                        new ContextParamTrait.Provider().createTrait(
                            ShapeId.from("smithy.rules#contextParam"),
                            Node.objectNodeBuilder()
                                .withMember("name", "ResourceArn")
                                .build()
                        ),
                        new RequiredTrait())
                .putMember("IndexName", Schemas.INDEX_NAME)
                .putMember("Select", Select.$SCHEMA)
                .putMember("AttributesToGet", Schemas.ATTRIBUTE_NAME_LIST)
                .putMember("Limit", Schemas.POSITIVE_INTEGER_OBJECT)
                .putMember("ConsistentRead", Schemas.CONSISTENT_READ)
                .putMember("KeyConditions", Schemas.KEY_CONDITIONS_BUILDER)
                .putMember("QueryFilter", Schemas.FILTER_CONDITION_MAP_BUILDER)
                .putMember("ConditionalOperator", ConditionalOperator.$SCHEMA)
                .putMember("ScanIndexForward", Schemas.BOOLEAN_OBJECT)
                .putMember("ExclusiveStartKey", Schemas.KEY_BUILDER)
                .putMember("ReturnConsumedCapacity", ReturnConsumedCapacity.$SCHEMA)
                .putMember("ProjectionExpression", Schemas.PROJECTION_EXPRESSION)
                .putMember("FilterExpression", Schemas.CONDITION_EXPRESSION)
                .putMember("KeyConditionExpression", Schemas.KEY_EXPRESSION)
                .putMember("ExpressionAttributeNames", Schemas.EXPRESSION_ATTRIBUTE_NAME_MAP)
                .putMember("ExpressionAttributeValues", Schemas.EXPRESSION_ATTRIBUTE_VALUE_MAP_BUILDER)
                .builderSupplier(QueryInput::builder)
                .shapeClass(QueryInput.class)
                .build();

            BATCH_WRITE_ITEM_OUTPUT_BUILDER
                .putMember("UnprocessedItems", Schemas.BATCH_WRITE_ITEM_REQUEST_MAP_BUILDER)
                .putMember("ItemCollectionMetrics", Schemas.ITEM_COLLECTION_METRICS_PER_TABLE_BUILDER)
                .putMember("ConsumedCapacity", Schemas.CONSUMED_CAPACITY_MULTIPLE)
                .builderSupplier(BatchWriteItemOutput::builder)
                .shapeClass(BatchWriteItemOutput.class)
                .build();

            UPDATE_ITEM_INPUT_BUILDER
                .putMember("TableName", Schemas.TABLE_ARN,
                        new ContextParamTrait.Provider().createTrait(
                            ShapeId.from("smithy.rules#contextParam"),
                            Node.objectNodeBuilder()
                                .withMember("name", "ResourceArn")
                                .build()
                        ),
                        new RequiredTrait())
                .putMember("Key", Schemas.KEY_BUILDER,
                        new RequiredTrait())
                .putMember("AttributeUpdates", Schemas.ATTRIBUTE_UPDATES_BUILDER)
                .putMember("Expected", Schemas.EXPECTED_ATTRIBUTE_MAP_BUILDER)
                .putMember("ConditionalOperator", ConditionalOperator.$SCHEMA)
                .putMember("ReturnValues", ReturnValue.$SCHEMA)
                .putMember("ReturnConsumedCapacity", ReturnConsumedCapacity.$SCHEMA)
                .putMember("ReturnItemCollectionMetrics", ReturnItemCollectionMetrics.$SCHEMA)
                .putMember("UpdateExpression", Schemas.UPDATE_EXPRESSION)
                .putMember("ConditionExpression", Schemas.CONDITION_EXPRESSION)
                .putMember("ExpressionAttributeNames", Schemas.EXPRESSION_ATTRIBUTE_NAME_MAP)
                .putMember("ExpressionAttributeValues", Schemas.EXPRESSION_ATTRIBUTE_VALUE_MAP_BUILDER)
                .putMember("ReturnValuesOnConditionCheckFailure", ReturnValuesOnConditionCheckFailure.$SCHEMA)
                .builderSupplier(UpdateItemInput::builder)
                .shapeClass(UpdateItemInput.class)
                .build();

            TRANSACT_WRITE_ITEM_BUILDER
                .putMember("ConditionCheck", Schemas.CONDITION_CHECK_BUILDER)
                .putMember("Put", Schemas.PUT_BUILDER)
                .putMember("Delete", Schemas.DELETE_BUILDER)
                .putMember("Update", Schemas.UPDATE_BUILDER)
                .builderSupplier(TransactWriteItem::builder)
                .shapeClass(TransactWriteItem.class)
                .build();

            TRANSACT_WRITE_ITEM_LIST_BUILDER
                .putMember("member", Schemas.TRANSACT_WRITE_ITEM_BUILDER)
                .build();

            TRANSACT_WRITE_ITEMS_INPUT_BUILDER
                .putMember("TransactItems", Schemas.TRANSACT_WRITE_ITEM_LIST_BUILDER,
                        new RequiredTrait())
                .putMember("ReturnConsumedCapacity", ReturnConsumedCapacity.$SCHEMA)
                .putMember("ReturnItemCollectionMetrics", ReturnItemCollectionMetrics.$SCHEMA)
                .putMember("ClientRequestToken", Schemas.CLIENT_REQUEST_TOKEN,
                        new IdempotencyTokenTrait())
                .builderSupplier(TransactWriteItemsInput::builder)
                .shapeClass(TransactWriteItemsInput.class)
                .build();

    }

    static final Schema ATTRIBUTE_VALUE = ATTRIBUTE_VALUE_BUILDER.build().resolve();
    static final Schema LIST_ATTRIBUTE_VALUE = LIST_ATTRIBUTE_VALUE_BUILDER.build().resolve();
    static final Schema MAP_ATTRIBUTE_VALUE = MAP_ATTRIBUTE_VALUE_BUILDER.build().resolve();
    static final Schema ATTRIBUTE_MAP = ATTRIBUTE_MAP_BUILDER.build().resolve();
    static final Schema ATTRIBUTE_VALUE_LIST = ATTRIBUTE_VALUE_LIST_BUILDER.build().resolve();
    static final Schema ATTRIBUTE_VALUE_UPDATE = ATTRIBUTE_VALUE_UPDATE_BUILDER.build().resolve();
    static final Schema EXPRESSION_ATTRIBUTE_VALUE_MAP = EXPRESSION_ATTRIBUTE_VALUE_MAP_BUILDER.build().resolve();
    static final Schema ITEM_COLLECTION_KEY_ATTRIBUTE_MAP = ITEM_COLLECTION_KEY_ATTRIBUTE_MAP_BUILDER.build().resolve();
    static final Schema KEY = KEY_BUILDER.build().resolve();
    static final Schema PREPARED_STATEMENT_PARAMETERS = PREPARED_STATEMENT_PARAMETERS_BUILDER.build().resolve();
    static final Schema PUT_ITEM_INPUT_ATTRIBUTE_MAP = PUT_ITEM_INPUT_ATTRIBUTE_MAP_BUILDER.build().resolve();
    static final Schema SEARCH_VECTOR_LIST = SEARCH_VECTOR_LIST_BUILDER.build().resolve();
    static final Schema ATTRIBUTE_UPDATES = ATTRIBUTE_UPDATES_BUILDER.build().resolve();
    static final Schema BATCH_STATEMENT_ERROR = BATCH_STATEMENT_ERROR_BUILDER.build().resolve();
    static final Schema BATCH_STATEMENT_REQUEST = BATCH_STATEMENT_REQUEST_BUILDER.build().resolve();
    static final Schema CANCELLATION_REASON = CANCELLATION_REASON_BUILDER.build().resolve();
    static final Schema CONDITION = CONDITION_BUILDER.build().resolve();
    static final Schema CONDITIONAL_CHECK_FAILED_EXCEPTION = CONDITIONAL_CHECK_FAILED_EXCEPTION_BUILDER.build().resolve();
    static final Schema DELETE_REQUEST = DELETE_REQUEST_BUILDER.build().resolve();
    static final Schema EXECUTE_STATEMENT_INPUT = EXECUTE_STATEMENT_INPUT_BUILDER.build().resolve();
    static final Schema GET = GET_BUILDER.build().resolve();
    static final Schema GET_ITEM_INPUT = GET_ITEM_INPUT_BUILDER.build().resolve();
    static final Schema GET_ITEM_OUTPUT = GET_ITEM_OUTPUT_BUILDER.build().resolve();
    static final Schema ITEM_COLLECTION_METRICS = ITEM_COLLECTION_METRICS_BUILDER.build().resolve();
    static final Schema ITEM_LIST = ITEM_LIST_BUILDER.build().resolve();
    static final Schema ITEM_RESPONSE = ITEM_RESPONSE_BUILDER.build().resolve();
    static final Schema KEY_LIST = KEY_LIST_BUILDER.build().resolve();
    static final Schema PARAMETERIZED_STATEMENT = PARAMETERIZED_STATEMENT_BUILDER.build().resolve();
    static final Schema PUT_REQUEST = PUT_REQUEST_BUILDER.build().resolve();
    static final Schema SEARCH_RESULT_ITEM = SEARCH_RESULT_ITEM_BUILDER.build().resolve();
    static final Schema BATCH_GET_RESPONSE_MAP = BATCH_GET_RESPONSE_MAP_BUILDER.build().resolve();
    static final Schema CANCELLATION_REASON_LIST = CANCELLATION_REASON_LIST_BUILDER.build().resolve();
    static final Schema FILTER_CONDITION_MAP = FILTER_CONDITION_MAP_BUILDER.build().resolve();
    static final Schema ITEM_COLLECTION_METRICS_MULTIPLE = ITEM_COLLECTION_METRICS_MULTIPLE_BUILDER.build().resolve();
    static final Schema ITEM_RESPONSE_LIST = ITEM_RESPONSE_LIST_BUILDER.build().resolve();
    static final Schema KEY_CONDITIONS = KEY_CONDITIONS_BUILDER.build().resolve();
    static final Schema KEYS_AND_ATTRIBUTES = KEYS_AND_ATTRIBUTES_BUILDER.build().resolve();
    static final Schema PARAMETERIZED_STATEMENTS = PARAMETERIZED_STATEMENTS_BUILDER.build().resolve();
    static final Schema PARTI_QL_BATCH_REQUEST = PARTI_QL_BATCH_REQUEST_BUILDER.build().resolve();
    static final Schema SEARCH_RESULT_LIST = SEARCH_RESULT_LIST_BUILDER.build().resolve();
    static final Schema TRANSACT_GET_ITEM = TRANSACT_GET_ITEM_BUILDER.build().resolve();
    static final Schema BATCH_EXECUTE_STATEMENT_INPUT = BATCH_EXECUTE_STATEMENT_INPUT_BUILDER.build().resolve();
    static final Schema BATCH_GET_REQUEST_MAP = BATCH_GET_REQUEST_MAP_BUILDER.build().resolve();
    static final Schema EXECUTE_TRANSACTION_INPUT = EXECUTE_TRANSACTION_INPUT_BUILDER.build().resolve();
    static final Schema EXECUTE_TRANSACTION_OUTPUT = EXECUTE_TRANSACTION_OUTPUT_BUILDER.build().resolve();
    static final Schema ITEM_COLLECTION_METRICS_PER_TABLE = ITEM_COLLECTION_METRICS_PER_TABLE_BUILDER.build().resolve();
    static final Schema SEARCH_VECTORS_OUTPUT = SEARCH_VECTORS_OUTPUT_BUILDER.build().resolve();
    static final Schema TRANSACT_GET_ITEM_LIST = TRANSACT_GET_ITEM_LIST_BUILDER.build().resolve();
    static final Schema TRANSACT_GET_ITEMS_OUTPUT = TRANSACT_GET_ITEMS_OUTPUT_BUILDER.build().resolve();
    static final Schema TRANSACTION_CANCELED_EXCEPTION = TRANSACTION_CANCELED_EXCEPTION_BUILDER.build().resolve();
    static final Schema BATCH_GET_ITEM_INPUT = BATCH_GET_ITEM_INPUT_BUILDER.build().resolve();
    static final Schema EXPECTED_ATTRIBUTE_VALUE = EXPECTED_ATTRIBUTE_VALUE_BUILDER.build().resolve();
    static final Schema TRANSACT_GET_ITEMS_INPUT = TRANSACT_GET_ITEMS_INPUT_BUILDER.build().resolve();
    static final Schema TRANSACT_WRITE_ITEMS_OUTPUT = TRANSACT_WRITE_ITEMS_OUTPUT_BUILDER.build().resolve();
    static final Schema CONDITION_CHECK = CONDITION_CHECK_BUILDER.build().resolve();
    static final Schema DELETE = DELETE_BUILDER.build().resolve();
    static final Schema PUT = PUT_BUILDER.build().resolve();
    static final Schema SEARCH_VECTORS_INPUT = SEARCH_VECTORS_INPUT_BUILDER.build().resolve();
    static final Schema UPDATE = UPDATE_BUILDER.build().resolve();
    static final Schema BATCH_STATEMENT_RESPONSE = BATCH_STATEMENT_RESPONSE_BUILDER.build().resolve();
    static final Schema DELETE_ITEM_OUTPUT = DELETE_ITEM_OUTPUT_BUILDER.build().resolve();
    static final Schema EXECUTE_STATEMENT_OUTPUT = EXECUTE_STATEMENT_OUTPUT_BUILDER.build().resolve();
    static final Schema EXPECTED_ATTRIBUTE_MAP = EXPECTED_ATTRIBUTE_MAP_BUILDER.build().resolve();
    static final Schema PUT_ITEM_OUTPUT = PUT_ITEM_OUTPUT_BUILDER.build().resolve();
    static final Schema QUERY_OUTPUT = QUERY_OUTPUT_BUILDER.build().resolve();
    static final Schema SCAN_OUTPUT = SCAN_OUTPUT_BUILDER.build().resolve();
    static final Schema UPDATE_ITEM_OUTPUT = UPDATE_ITEM_OUTPUT_BUILDER.build().resolve();
    static final Schema WRITE_REQUEST = WRITE_REQUEST_BUILDER.build().resolve();
    static final Schema PARTI_QL_BATCH_RESPONSE = PARTI_QL_BATCH_RESPONSE_BUILDER.build().resolve();
    static final Schema WRITE_REQUESTS = WRITE_REQUESTS_BUILDER.build().resolve();
    static final Schema BATCH_EXECUTE_STATEMENT_OUTPUT = BATCH_EXECUTE_STATEMENT_OUTPUT_BUILDER.build().resolve();
    static final Schema BATCH_GET_ITEM_OUTPUT = BATCH_GET_ITEM_OUTPUT_BUILDER.build().resolve();
    static final Schema BATCH_WRITE_ITEM_REQUEST_MAP = BATCH_WRITE_ITEM_REQUEST_MAP_BUILDER.build().resolve();
    static final Schema SCAN_INPUT = SCAN_INPUT_BUILDER.build().resolve();
    static final Schema BATCH_WRITE_ITEM_INPUT = BATCH_WRITE_ITEM_INPUT_BUILDER.build().resolve();
    static final Schema DELETE_ITEM_INPUT = DELETE_ITEM_INPUT_BUILDER.build().resolve();
    static final Schema PUT_ITEM_INPUT = PUT_ITEM_INPUT_BUILDER.build().resolve();
    static final Schema QUERY_INPUT = QUERY_INPUT_BUILDER.build().resolve();
    static final Schema BATCH_WRITE_ITEM_OUTPUT = BATCH_WRITE_ITEM_OUTPUT_BUILDER.build().resolve();
    static final Schema UPDATE_ITEM_INPUT = UPDATE_ITEM_INPUT_BUILDER.build().resolve();
    static final Schema TRANSACT_WRITE_ITEM = TRANSACT_WRITE_ITEM_BUILDER.build().resolve();
    static final Schema TRANSACT_WRITE_ITEM_LIST = TRANSACT_WRITE_ITEM_LIST_BUILDER.build().resolve();
    static final Schema TRANSACT_WRITE_ITEMS_INPUT = TRANSACT_WRITE_ITEMS_INPUT_BUILDER.build().resolve();

    private Schemas() {}
}
