package software.amazon.awssdk.benchmark.smithyjava.cloudwatch.model;

import java.math.BigDecimal;
import software.amazon.smithy.aws.traits.protocols.AwsQueryErrorTrait;
import software.amazon.smithy.java.core.schema.Schema;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.model.traits.ErrorTrait;
import software.amazon.smithy.model.traits.HttpErrorTrait;
import software.amazon.smithy.model.traits.LengthTrait;
import software.amazon.smithy.model.traits.PatternTrait;
import software.amazon.smithy.model.traits.RangeTrait;
import software.amazon.smithy.model.traits.RequiredTrait;
import software.amazon.smithy.model.traits.UnitTypeTrait;

/**
 * Defines schemas for shapes in the model package.
 */
final class Schemas {
    static final Schema ACCOUNT_ID = Schema.createString(ShapeId.from("com.amazonaws.cloudwatch#AccountId"),
            LengthTrait.builder().min(1L).max(255L).build());
    static final Schema ACTION_LOG_LINE_COUNT = Schema.createInteger(ShapeId.from("com.amazonaws.cloudwatch#ActionLogLineCount"));
    static final Schema ACTION_LOG_LINE_ROLE_ARN = Schema.createString(ShapeId.from("com.amazonaws.cloudwatch#ActionLogLineRoleArn"));
    static final Schema ACTION_PREFIX = Schema.createString(ShapeId.from("com.amazonaws.cloudwatch#ActionPrefix"),
            LengthTrait.builder().min(1L).max(1024L).build());
    static final Schema ACTIONS_ENABLED = Schema.createBoolean(ShapeId.from("com.amazonaws.cloudwatch#ActionsEnabled"));
    static final Schema ACTIONS_SUPPRESSED_REASON = Schema.createString(ShapeId.from("com.amazonaws.cloudwatch#ActionsSuppressedReason"),
            LengthTrait.builder().min(0L).max(1024L).build());
    static final Schema AGGREGATION_EXPRESSION = Schema.createString(ShapeId.from("com.amazonaws.cloudwatch#AggregationExpression"),
            LengthTrait.builder().min(1L).max(2048L).build());
    static final Schema ALARM_ARN = Schema.createString(ShapeId.from("com.amazonaws.cloudwatch#AlarmArn"),
            LengthTrait.builder().min(1L).max(1600L).build());
    static final Schema ATTRIBUTE_NAME = Schema.createString(ShapeId.from("com.amazonaws.cloudwatch#AttributeName"),
            LengthTrait.builder().min(1L).max(255L).build());
    static final Schema ATTRIBUTE_VALUE = Schema.createString(ShapeId.from("com.amazonaws.cloudwatch#AttributeValue"),
            LengthTrait.builder().min(1L).max(1024L).build());
    static final Schema CONTRIBUTOR_ATTRIBUTES = Schema.mapBuilder(ShapeId.from("com.amazonaws.cloudwatch#ContributorAttributes"),
            LengthTrait.builder().min(0L).max(150L).build())
        .putMember("key", Schemas.ATTRIBUTE_NAME)
        .putMember("value", Schemas.ATTRIBUTE_VALUE)
        .build();

    static final Schema CONTRIBUTOR_ID = Schema.createString(ShapeId.from("com.amazonaws.cloudwatch#ContributorId"),
            LengthTrait.builder().min(1L).max(16L).build());
    static final Schema STATE_REASON = Schema.createString(ShapeId.from("com.amazonaws.cloudwatch#StateReason"),
            LengthTrait.builder().min(0L).max(1023L).build());
    static final Schema TIMESTAMP = Schema.createTimestamp(ShapeId.from("com.amazonaws.cloudwatch#Timestamp"));
    static final Schema ALARM_CONTRIBUTOR = Schema.structureBuilder(ShapeId.from("com.amazonaws.cloudwatch#AlarmContributor"))
             .putMember("ContributorId", Schemas.CONTRIBUTOR_ID,
                     new RequiredTrait())
             .putMember("ContributorAttributes", Schemas.CONTRIBUTOR_ATTRIBUTES,
                     new RequiredTrait())
             .putMember("StateReason", Schemas.STATE_REASON,
                     new RequiredTrait())
             .putMember("StateTransitionedTimestamp", Schemas.TIMESTAMP)
             .builderSupplier(AlarmContributor::builder)
             .shapeClass(AlarmContributor.class)
             .build();

    static final Schema ALARM_CONTRIBUTORS = Schema.listBuilder(ShapeId.from("com.amazonaws.cloudwatch#AlarmContributors"))
        .putMember("member", Schemas.ALARM_CONTRIBUTOR)
        .build();

    static final Schema ALARM_DESCRIPTION = Schema.createString(ShapeId.from("com.amazonaws.cloudwatch#AlarmDescription"),
            LengthTrait.builder().min(0L).max(1024L).build());
    static final Schema ALARM_NAME = Schema.createString(ShapeId.from("com.amazonaws.cloudwatch#AlarmName"),
            LengthTrait.builder().min(1L).max(255L).build());
    static final Schema HISTORY_DATA = Schema.createString(ShapeId.from("com.amazonaws.cloudwatch#HistoryData"),
            LengthTrait.builder().min(1L).max(4095L).build());
    static final Schema HISTORY_SUMMARY = Schema.createString(ShapeId.from("com.amazonaws.cloudwatch#HistorySummary"),
            LengthTrait.builder().min(1L).max(255L).build());
    static final Schema ALARM_HISTORY_ITEM = Schema.structureBuilder(ShapeId.from("com.amazonaws.cloudwatch#AlarmHistoryItem"))
             .putMember("AlarmName", Schemas.ALARM_NAME)
             .putMember("AlarmContributorId", Schemas.CONTRIBUTOR_ID)
             .putMember("AlarmType", AlarmType.$SCHEMA)
             .putMember("Timestamp", Schemas.TIMESTAMP)
             .putMember("HistoryItemType", HistoryItemType.$SCHEMA)
             .putMember("HistorySummary", Schemas.HISTORY_SUMMARY)
             .putMember("HistoryData", Schemas.HISTORY_DATA)
             .putMember("AlarmContributorAttributes", Schemas.CONTRIBUTOR_ATTRIBUTES)
             .builderSupplier(AlarmHistoryItem::builder)
             .shapeClass(AlarmHistoryItem.class)
             .build();

    static final Schema ALARM_HISTORY_ITEMS = Schema.listBuilder(ShapeId.from("com.amazonaws.cloudwatch#AlarmHistoryItems"))
        .putMember("member", Schemas.ALARM_HISTORY_ITEM)
        .build();

    static final Schema ALARM_MUTE_RULE_STATUSES = Schema.listBuilder(ShapeId.from("com.amazonaws.cloudwatch#AlarmMuteRuleStatuses"))
        .putMember("member", AlarmMuteRuleStatus.$SCHEMA)
        .build();

    static final Schema ARN = Schema.createString(ShapeId.from("com.amazonaws.cloudwatch#Arn"),
            LengthTrait.builder().min(1L).max(1600L).build());
    static final Schema MUTE_TYPE = Schema.createString(ShapeId.from("com.amazonaws.cloudwatch#MuteType"));
    static final Schema ALARM_MUTE_RULE_SUMMARY = Schema.structureBuilder(ShapeId.from("com.amazonaws.cloudwatch#AlarmMuteRuleSummary"))
             .putMember("AlarmMuteRuleArn", Schemas.ARN)
             .putMember("ExpireDate", Schemas.TIMESTAMP)
             .putMember("Status", AlarmMuteRuleStatus.$SCHEMA)
             .putMember("MuteType", Schemas.MUTE_TYPE)
             .putMember("LastUpdatedTimestamp", Schemas.TIMESTAMP)
             .builderSupplier(AlarmMuteRuleSummary::builder)
             .shapeClass(AlarmMuteRuleSummary.class)
             .build();

    static final Schema ALARM_MUTE_RULE_SUMMARIES = Schema.listBuilder(ShapeId.from("com.amazonaws.cloudwatch#AlarmMuteRuleSummaries"))
        .putMember("member", Schemas.ALARM_MUTE_RULE_SUMMARY)
        .build();

    static final Schema ALARM_NAME_PREFIX = Schema.createString(ShapeId.from("com.amazonaws.cloudwatch#AlarmNamePrefix"),
            LengthTrait.builder().min(1L).max(255L).build());
    static final Schema ALARM_NAMES = Schema.listBuilder(ShapeId.from("com.amazonaws.cloudwatch#AlarmNames"),
            LengthTrait.builder().min(0L).max(100L).build())
        .putMember("member", Schemas.ALARM_NAME)
        .build();

    static final Schema PENDING_PERIOD = Schema.createInteger(ShapeId.from("com.amazonaws.cloudwatch#PendingPeriod"),
            RangeTrait.builder().min(new BigDecimal("0")).max(new BigDecimal("86400")).build());
    static final Schema QUERY = Schema.createString(ShapeId.from("com.amazonaws.cloudwatch#Query"),
            LengthTrait.builder().min(1L).max(10000L).build());
    static final Schema RECOVERY_PERIOD = Schema.createInteger(ShapeId.from("com.amazonaws.cloudwatch#RecoveryPeriod"),
            RangeTrait.builder().min(new BigDecimal("0")).max(new BigDecimal("86400")).build());
    static final Schema ALARM_PROM_QL_CRITERIA = Schema.structureBuilder(ShapeId.from("com.amazonaws.cloudwatch#AlarmPromQLCriteria"))
             .putMember("Query", Schemas.QUERY,
                     new RequiredTrait())
             .putMember("PendingPeriod", Schemas.PENDING_PERIOD)
             .putMember("RecoveryPeriod", Schemas.RECOVERY_PERIOD)
             .builderSupplier(AlarmPromQLCriteria::builder)
             .shapeClass(AlarmPromQLCriteria.class)
             .build();

    static final Schema ALARM_RULE = Schema.createString(ShapeId.from("com.amazonaws.cloudwatch#AlarmRule"),
            LengthTrait.builder().min(1L).max(10240L).build());
    static final Schema ALARM_TYPES = Schema.listBuilder(ShapeId.from("com.amazonaws.cloudwatch#AlarmTypes"))
        .putMember("member", AlarmType.$SCHEMA)
        .build();

    static final Schema AMAZON_RESOURCE_NAME = Schema.createString(ShapeId.from("com.amazonaws.cloudwatch#AmazonResourceName"),
            LengthTrait.builder().min(1L).max(1024L).build());
    static final Schema ANOMALY_DETECTOR_ID = Schema.createString(ShapeId.from("com.amazonaws.cloudwatch#AnomalyDetectorId"),
            LengthTrait.builder().min(1L).max(128L).build(),
            new PatternTrait("^[A-Za-z0-9_./:%()+-]+$"));
    static final Schema RANGE = Schema.structureBuilder(ShapeId.from("com.amazonaws.cloudwatch#Range"))
             .putMember("StartTime", Schemas.TIMESTAMP,
                     new RequiredTrait())
             .putMember("EndTime", Schemas.TIMESTAMP,
                     new RequiredTrait())
             .builderSupplier(Range::builder)
             .shapeClass(Range.class)
             .build();

    static final Schema ANOMALY_DETECTOR_EXCLUDED_TIME_RANGES = Schema.listBuilder(ShapeId.from("com.amazonaws.cloudwatch#AnomalyDetectorExcludedTimeRanges"))
        .putMember("member", Schemas.RANGE)
        .build();

    static final Schema ANOMALY_DETECTOR_METRIC_TIMEZONE = Schema.createString(ShapeId.from("com.amazonaws.cloudwatch#AnomalyDetectorMetricTimezone"),
            LengthTrait.builder().min(0L).max(50L).build(),
            new PatternTrait(".*"));
    static final Schema ANOMALY_DETECTOR_CONFIGURATION = Schema.structureBuilder(ShapeId.from("com.amazonaws.cloudwatch#AnomalyDetectorConfiguration"))
             .putMember("ExcludedTimeRanges", Schemas.ANOMALY_DETECTOR_EXCLUDED_TIME_RANGES)
             .putMember("MetricTimezone", Schemas.ANOMALY_DETECTOR_METRIC_TIMEZONE)
             .builderSupplier(AnomalyDetectorConfiguration::builder)
             .shapeClass(AnomalyDetectorConfiguration.class)
             .build();

    static final Schema DIMENSION_NAME = Schema.createString(ShapeId.from("com.amazonaws.cloudwatch#DimensionName"),
            LengthTrait.builder().min(1L).max(255L).build());
    static final Schema DIMENSION_VALUE = Schema.createString(ShapeId.from("com.amazonaws.cloudwatch#DimensionValue"),
            LengthTrait.builder().min(1L).max(1024L).build());
    static final Schema DIMENSION = Schema.structureBuilder(ShapeId.from("com.amazonaws.cloudwatch#Dimension"))
             .putMember("Name", Schemas.DIMENSION_NAME,
                     new RequiredTrait())
             .putMember("Value", Schemas.DIMENSION_VALUE,
                     new RequiredTrait())
             .builderSupplier(Dimension::builder)
             .shapeClass(Dimension.class)
             .build();

    static final Schema DIMENSIONS = Schema.listBuilder(ShapeId.from("com.amazonaws.cloudwatch#Dimensions"),
            LengthTrait.builder().min(0L).max(30L).build())
        .putMember("member", Schemas.DIMENSION)
        .build();

    static final Schema PERIODIC_SPIKES = Schema.createBoolean(ShapeId.from("com.amazonaws.cloudwatch#PeriodicSpikes"));
    static final Schema METRIC_CHARACTERISTICS = Schema.structureBuilder(ShapeId.from("com.amazonaws.cloudwatch#MetricCharacteristics"))
             .putMember("PeriodicSpikes", Schemas.PERIODIC_SPIKES)
             .builderSupplier(MetricCharacteristics::builder)
             .shapeClass(MetricCharacteristics.class)
             .build();

    static final Schema METRIC_EXPRESSION = Schema.createString(ShapeId.from("com.amazonaws.cloudwatch#MetricExpression"),
            LengthTrait.builder().min(1L).max(2048L).build());
    static final Schema METRIC_ID = Schema.createString(ShapeId.from("com.amazonaws.cloudwatch#MetricId"),
            LengthTrait.builder().min(1L).max(255L).build());
    static final Schema METRIC_LABEL = Schema.createString(ShapeId.from("com.amazonaws.cloudwatch#MetricLabel"));
    static final Schema METRIC_NAME = Schema.createString(ShapeId.from("com.amazonaws.cloudwatch#MetricName"),
            LengthTrait.builder().min(1L).max(255L).build());
    static final Schema NAMESPACE = Schema.createString(ShapeId.from("com.amazonaws.cloudwatch#Namespace"),
            LengthTrait.builder().min(1L).max(255L).build(),
            new PatternTrait("^[^:]"));
    static final Schema METRIC = Schema.structureBuilder(ShapeId.from("com.amazonaws.cloudwatch#Metric"))
             .putMember("Namespace", Schemas.NAMESPACE)
             .putMember("MetricName", Schemas.METRIC_NAME)
             .putMember("Dimensions", Schemas.DIMENSIONS)
             .builderSupplier(Metric::builder)
             .shapeClass(Metric.class)
             .build();

    static final Schema PERIOD = Schema.createInteger(ShapeId.from("com.amazonaws.cloudwatch#Period"),
            RangeTrait.builder().min(new BigDecimal("1")).build());
    static final Schema STAT = Schema.createString(ShapeId.from("com.amazonaws.cloudwatch#Stat"));
    static final Schema METRIC_STAT = Schema.structureBuilder(ShapeId.from("com.amazonaws.cloudwatch#MetricStat"))
             .putMember("Metric", Schemas.METRIC,
                     new RequiredTrait())
             .putMember("Period", Schemas.PERIOD,
                     new RequiredTrait())
             .putMember("Stat", Schemas.STAT,
                     new RequiredTrait())
             .putMember("Unit", StandardUnit.$SCHEMA)
             .builderSupplier(MetricStat::builder)
             .shapeClass(MetricStat.class)
             .build();

    static final Schema RETURN_DATA = Schema.createBoolean(ShapeId.from("com.amazonaws.cloudwatch#ReturnData"));
    static final Schema METRIC_DATA_QUERY = Schema.structureBuilder(ShapeId.from("com.amazonaws.cloudwatch#MetricDataQuery"))
             .putMember("Id", Schemas.METRIC_ID,
                     new RequiredTrait())
             .putMember("MetricStat", Schemas.METRIC_STAT)
             .putMember("Expression", Schemas.METRIC_EXPRESSION)
             .putMember("Label", Schemas.METRIC_LABEL)
             .putMember("ReturnData", Schemas.RETURN_DATA)
             .putMember("Period", Schemas.PERIOD)
             .putMember("AccountId", Schemas.ACCOUNT_ID)
             .builderSupplier(MetricDataQuery::builder)
             .shapeClass(MetricDataQuery.class)
             .build();

    static final Schema METRIC_DATA_QUERIES = Schema.listBuilder(ShapeId.from("com.amazonaws.cloudwatch#MetricDataQueries"))
        .putMember("member", Schemas.METRIC_DATA_QUERY)
        .build();

    static final Schema METRIC_MATH_ANOMALY_DETECTOR = Schema.structureBuilder(ShapeId.from("com.amazonaws.cloudwatch#MetricMathAnomalyDetector"))
             .putMember("MetricDataQueries", Schemas.METRIC_DATA_QUERIES)
             .builderSupplier(MetricMathAnomalyDetector::builder)
             .shapeClass(MetricMathAnomalyDetector.class)
             .build();

    static final Schema ANOMALY_DETECTOR_METRIC_STAT = Schema.createString(ShapeId.from("com.amazonaws.cloudwatch#AnomalyDetectorMetricStat"),
            LengthTrait.builder().min(0L).max(50L).build(),
            new PatternTrait("^(SampleCount|Average|Sum|Minimum|Maximum|IQM|(p|tc|tm|ts|wm)(\\d{1,2}(\\.\\d{0,10})?|100)|[ou]\\d+(\\.\\d*)?)(_E|_L|_H)?|(TM|TC|TS|WM)\\(((((\\d{1,2})(\\.\\d{0,10})?|100(\\.0{0,10})?)%)?:((\\d{1,2})(\\.\\d{0,10})?|100(\\.0{0,10})?)%|((\\d{1,2})(\\.\\d{0,10})?|100(\\.0{0,10})?)%:(((\\d{1,2})(\\.\\d{0,10})?|100(\\.0{0,10})?)%)?)\\)|(TM|TC|TS|WM|PR)\\(((\\d+(\\.\\d{0,10})?|(\\d+(\\.\\d{0,10})?[Ee][+-]?\\d+)):((\\d+(\\.\\d{0,10})?|(\\d+(\\.\\d{0,10})?[Ee][+-]?\\d+)))?|((\\d+(\\.\\d{0,10})?|(\\d+(\\.\\d{0,10})?[Ee][+-]?\\d+)))?:(\\d+(\\.\\d{0,10})?|(\\d+(\\.\\d{0,10})?[Ee][+-]?\\d+)))\\)$"));
    static final Schema SINGLE_METRIC_ANOMALY_DETECTOR = Schema.structureBuilder(ShapeId.from("com.amazonaws.cloudwatch#SingleMetricAnomalyDetector"))
             .putMember("AccountId", Schemas.ACCOUNT_ID)
             .putMember("Namespace", Schemas.NAMESPACE)
             .putMember("MetricName", Schemas.METRIC_NAME)
             .putMember("Dimensions", Schemas.DIMENSIONS)
             .putMember("Stat", Schemas.ANOMALY_DETECTOR_METRIC_STAT)
             .builderSupplier(SingleMetricAnomalyDetector::builder)
             .shapeClass(SingleMetricAnomalyDetector.class)
             .build();

    static final Schema ANOMALY_DETECTOR = Schema.structureBuilder(ShapeId.from("com.amazonaws.cloudwatch#AnomalyDetector"))
             .putMember("AnomalyDetectorId", Schemas.ANOMALY_DETECTOR_ID)
             .putMember("Namespace", Schemas.NAMESPACE)
             .putMember("MetricName", Schemas.METRIC_NAME)
             .putMember("Dimensions", Schemas.DIMENSIONS)
             .putMember("Stat", Schemas.ANOMALY_DETECTOR_METRIC_STAT)
             .putMember("Configuration", Schemas.ANOMALY_DETECTOR_CONFIGURATION)
             .putMember("StateValue", AnomalyDetectorStateValue.$SCHEMA)
             .putMember("MetricCharacteristics", Schemas.METRIC_CHARACTERISTICS)
             .putMember("SingleMetricAnomalyDetector", Schemas.SINGLE_METRIC_ANOMALY_DETECTOR)
             .putMember("MetricMathAnomalyDetector", Schemas.METRIC_MATH_ANOMALY_DETECTOR)
             .builderSupplier(AnomalyDetector::builder)
             .shapeClass(AnomalyDetector.class)
             .build();

    static final Schema ANOMALY_DETECTOR_IDS = Schema.listBuilder(ShapeId.from("com.amazonaws.cloudwatch#AnomalyDetectorIds"),
            LengthTrait.builder().min(0L).max(50L).build())
        .putMember("member", Schemas.ANOMALY_DETECTOR_ID)
        .build();

    static final Schema ANOMALY_DETECTORS = Schema.listBuilder(ShapeId.from("com.amazonaws.cloudwatch#AnomalyDetectors"))
        .putMember("member", Schemas.ANOMALY_DETECTOR)
        .build();

    static final Schema ANOMALY_DETECTOR_TYPES = Schema.listBuilder(ShapeId.from("com.amazonaws.cloudwatch#AnomalyDetectorTypes"),
            LengthTrait.builder().min(0L).max(2L).build())
        .putMember("member", AnomalyDetectorType.$SCHEMA)
        .build();

    static final Schema DATASET_IDENTIFIER = Schema.createString(ShapeId.from("com.amazonaws.cloudwatch#DatasetIdentifier"),
            LengthTrait.builder().min(1L).max(2048L).build(),
            new PatternTrait("^(default|arn:[a-zA-Z0-9-]+:cloudwatch:[a-zA-Z0-9-]*:\\d{12}:dataset/default)$"));
    static final Schema KMS_KEY_ARN = Schema.createString(ShapeId.from("com.amazonaws.cloudwatch#KmsKeyArn"),
            LengthTrait.builder().min(20L).max(2048L).build(),
            new PatternTrait("^arn:[a-zA-Z0-9-]+:kms:[a-zA-Z0-9-]+:\\d{12}:key/[a-f0-9-]+$"));
    static final Schema ASSOCIATE_DATASET_KMS_KEY_INPUT = Schema.structureBuilder(ShapeId.from("com.amazonaws.cloudwatch#AssociateDatasetKmsKeyInput"))
             .putMember("DatasetIdentifier", Schemas.DATASET_IDENTIFIER,
                     new RequiredTrait())
             .putMember("KmsKeyArn", Schemas.KMS_KEY_ARN,
                     new RequiredTrait())
             .builderSupplier(AssociateDatasetKmsKeyInput::builder)
             .shapeClass(AssociateDatasetKmsKeyInput.class)
             .build();

    static final Schema ASSOCIATE_DATASET_KMS_KEY_OUTPUT = Schema.structureBuilder(ShapeId.from("com.amazonaws.cloudwatch#AssociateDatasetKmsKeyOutput")).builderSupplier(AssociateDatasetKmsKeyOutput::builder)
             .shapeClass(AssociateDatasetKmsKeyOutput.class)
             .build();

    static final Schema ERROR_MESSAGE = Schema.createString(ShapeId.from("com.amazonaws.cloudwatch#ErrorMessage"),
            LengthTrait.builder().min(1L).max(255L).build());
    static final Schema CONFLICT_EXCEPTION = Schema.structureBuilder(ShapeId.from("com.amazonaws.cloudwatch#ConflictException"),
            new ErrorTrait("client"),
            new HttpErrorTrait(409))
             .putMember("Message", Schemas.ERROR_MESSAGE)
             .builderSupplier(ConflictException::builder)
             .shapeClass(ConflictException.class)
             .build();

    static final Schema STRING = Schema.createString(ShapeId.from("com.amazonaws.cloudwatch#String"));
    static final Schema KMS_ACCESS_DENIED_EXCEPTION = Schema.structureBuilder(ShapeId.from("com.amazonaws.cloudwatch#KmsAccessDeniedException"),
            new ErrorTrait("client"))
             .putMember("Message", Schemas.STRING,
                     new RequiredTrait())
             .builderSupplier(KmsAccessDeniedException::builder)
             .shapeClass(KmsAccessDeniedException.class)
             .build();

    static final Schema KMS_KEY_DISABLED_EXCEPTION = Schema.structureBuilder(ShapeId.from("com.amazonaws.cloudwatch#KmsKeyDisabledException"),
            new ErrorTrait("client"))
             .putMember("Message", Schemas.STRING,
                     new RequiredTrait())
             .builderSupplier(KmsKeyDisabledException::builder)
             .shapeClass(KmsKeyDisabledException.class)
             .build();

    static final Schema KMS_KEY_NOT_FOUND_EXCEPTION = Schema.structureBuilder(ShapeId.from("com.amazonaws.cloudwatch#KmsKeyNotFoundException"),
            new ErrorTrait("client"))
             .putMember("Message", Schemas.STRING,
                     new RequiredTrait())
             .builderSupplier(KmsKeyNotFoundException::builder)
             .shapeClass(KmsKeyNotFoundException.class)
             .build();

    static final Schema FAULT_DESCRIPTION = Schema.createString(ShapeId.from("com.amazonaws.cloudwatch#FaultDescription"));
    static final Schema RESOURCE_ID = Schema.createString(ShapeId.from("com.amazonaws.cloudwatch#ResourceId"));
    static final Schema RESOURCE_TYPE = Schema.createString(ShapeId.from("com.amazonaws.cloudwatch#ResourceType"));
    static final Schema RESOURCE_NOT_FOUND_EXCEPTION = Schema.structureBuilder(ShapeId.from("com.amazonaws.cloudwatch#ResourceNotFoundException"),
            new ErrorTrait("client"),
            new HttpErrorTrait(404),
            new AwsQueryErrorTrait.Provider().createTrait(
                ShapeId.from("aws.protocols#awsQueryError"),
                Node.objectNodeBuilder()
                    .withMember("code", "ResourceNotFoundException")
                    .withMember("httpResponseCode", 404L)
                    .build()
            ))
             .putMember("ResourceType", Schemas.RESOURCE_TYPE)
             .putMember("ResourceId", Schemas.RESOURCE_ID)
             .putMember("Message", Schemas.FAULT_DESCRIPTION)
             .builderSupplier(ResourceNotFoundException::builder)
             .shapeClass(ResourceNotFoundException.class)
             .build();

    static final Schema AWS_QUERY_ERROR_MESSAGE = Schema.createString(ShapeId.from("com.amazonaws.cloudwatch#AwsQueryErrorMessage"));
    static final Schema EXCEPTION_TYPE = Schema.createString(ShapeId.from("com.amazonaws.cloudwatch#ExceptionType"));
    static final Schema FAILURE_CODE = Schema.createString(ShapeId.from("com.amazonaws.cloudwatch#FailureCode"));
    static final Schema FAILURE_DESCRIPTION = Schema.createString(ShapeId.from("com.amazonaws.cloudwatch#FailureDescription"));
    static final Schema FAILURE_RESOURCE = Schema.createString(ShapeId.from("com.amazonaws.cloudwatch#FailureResource"));
    static final Schema PARTIAL_FAILURE = Schema.structureBuilder(ShapeId.from("com.amazonaws.cloudwatch#PartialFailure"))
             .putMember("FailureResource", Schemas.FAILURE_RESOURCE)
             .putMember("ExceptionType", Schemas.EXCEPTION_TYPE)
             .putMember("FailureCode", Schemas.FAILURE_CODE)
             .putMember("FailureDescription", Schemas.FAILURE_DESCRIPTION)
             .builderSupplier(PartialFailure::builder)
             .shapeClass(PartialFailure.class)
             .build();

    static final Schema BATCH_FAILURES = Schema.listBuilder(ShapeId.from("com.amazonaws.cloudwatch#BatchFailures"))
        .putMember("member", Schemas.PARTIAL_FAILURE)
        .build();

    static final Schema SUPPRESSOR_PERIOD = Schema.createInteger(ShapeId.from("com.amazonaws.cloudwatch#SuppressorPeriod"));
    static final Schema RESOURCE_NAME = Schema.createString(ShapeId.from("com.amazonaws.cloudwatch#ResourceName"),
            LengthTrait.builder().min(1L).max(1024L).build());
    static final Schema RESOURCE_LIST = Schema.listBuilder(ShapeId.from("com.amazonaws.cloudwatch#ResourceList"),
            LengthTrait.builder().min(0L).max(5L).build())
        .putMember("member", Schemas.RESOURCE_NAME)
        .build();

    static final Schema STATE_REASON_DATA = Schema.createString(ShapeId.from("com.amazonaws.cloudwatch#StateReasonData"),
            LengthTrait.builder().min(0L).max(4000L).build());
    static final Schema COMPOSITE_ALARM = Schema.structureBuilder(ShapeId.from("com.amazonaws.cloudwatch#CompositeAlarm"))
             .putMember("ActionsEnabled", Schemas.ACTIONS_ENABLED)
             .putMember("AlarmActions", Schemas.RESOURCE_LIST)
             .putMember("AlarmArn", Schemas.ALARM_ARN)
             .putMember("AlarmConfigurationUpdatedTimestamp", Schemas.TIMESTAMP)
             .putMember("AlarmDescription", Schemas.ALARM_DESCRIPTION)
             .putMember("AlarmName", Schemas.ALARM_NAME)
             .putMember("AlarmRule", Schemas.ALARM_RULE)
             .putMember("InsufficientDataActions", Schemas.RESOURCE_LIST)
             .putMember("OKActions", Schemas.RESOURCE_LIST)
             .putMember("StateReason", Schemas.STATE_REASON)
             .putMember("StateReasonData", Schemas.STATE_REASON_DATA)
             .putMember("StateUpdatedTimestamp", Schemas.TIMESTAMP)
             .putMember("StateValue", StateValue.$SCHEMA)
             .putMember("StateTransitionedTimestamp", Schemas.TIMESTAMP)
             .putMember("ActionsSuppressedBy", ActionsSuppressedBy.$SCHEMA)
             .putMember("ActionsSuppressedReason", Schemas.ACTIONS_SUPPRESSED_REASON)
             .putMember("ActionsSuppressor", Schemas.ALARM_ARN)
             .putMember("ActionsSuppressorWaitPeriod", Schemas.SUPPRESSOR_PERIOD)
             .putMember("ActionsSuppressorExtensionPeriod", Schemas.SUPPRESSOR_PERIOD)
             .builderSupplier(CompositeAlarm::builder)
             .shapeClass(CompositeAlarm.class)
             .build();

    static final Schema COMPOSITE_ALARMS = Schema.listBuilder(ShapeId.from("com.amazonaws.cloudwatch#CompositeAlarms"))
        .putMember("member", Schemas.COMPOSITE_ALARM)
        .build();

    static final Schema CONCURRENT_MODIFICATION_EXCEPTION = Schema.structureBuilder(ShapeId.from("com.amazonaws.cloudwatch#ConcurrentModificationException"),
            new ErrorTrait("client"),
            new HttpErrorTrait(429),
            new AwsQueryErrorTrait.Provider().createTrait(
                ShapeId.from("aws.protocols#awsQueryError"),
                Node.objectNodeBuilder()
                    .withMember("code", "ConcurrentModificationException")
                    .withMember("httpResponseCode", 429L)
                    .build()
            ))
             .putMember("Message", Schemas.FAULT_DESCRIPTION)
             .builderSupplier(ConcurrentModificationException::builder)
             .shapeClass(ConcurrentModificationException.class)
             .build();

    static final Schema DATAPOINT_VALUE = Schema.createDouble(ShapeId.from("com.amazonaws.cloudwatch#DatapointValue"));
    static final Schema COUNTS = Schema.listBuilder(ShapeId.from("com.amazonaws.cloudwatch#Counts"))
        .putMember("member", Schemas.DATAPOINT_VALUE)
        .build();

    static final Schema DASHBOARD_ARN = Schema.createString(ShapeId.from("com.amazonaws.cloudwatch#DashboardArn"));
    static final Schema DASHBOARD_BODY = Schema.createString(ShapeId.from("com.amazonaws.cloudwatch#DashboardBody"));
    static final Schema DASHBOARD_NAME = Schema.createString(ShapeId.from("com.amazonaws.cloudwatch#DashboardName"));
    static final Schema LAST_MODIFIED = Schema.createTimestamp(ShapeId.from("com.amazonaws.cloudwatch#LastModified"));
    static final Schema SIZE = Schema.createLong(ShapeId.from("com.amazonaws.cloudwatch#Size"));
    static final Schema DASHBOARD_ENTRY = Schema.structureBuilder(ShapeId.from("com.amazonaws.cloudwatch#DashboardEntry"))
             .putMember("DashboardName", Schemas.DASHBOARD_NAME)
             .putMember("DashboardArn", Schemas.DASHBOARD_ARN)
             .putMember("LastModified", Schemas.LAST_MODIFIED)
             .putMember("Size", Schemas.SIZE)
             .builderSupplier(DashboardEntry::builder)
             .shapeClass(DashboardEntry.class)
             .build();

    static final Schema DASHBOARD_ENTRIES = Schema.listBuilder(ShapeId.from("com.amazonaws.cloudwatch#DashboardEntries"))
        .putMember("member", Schemas.DASHBOARD_ENTRY)
        .build();

    static final Schema DASHBOARD_ERROR_MESSAGE = Schema.createString(ShapeId.from("com.amazonaws.cloudwatch#DashboardErrorMessage"));
    static final Schema DATA_PATH = Schema.createString(ShapeId.from("com.amazonaws.cloudwatch#DataPath"));
    static final Schema MESSAGE = Schema.createString(ShapeId.from("com.amazonaws.cloudwatch#Message"));
    static final Schema DASHBOARD_VALIDATION_MESSAGE = Schema.structureBuilder(ShapeId.from("com.amazonaws.cloudwatch#DashboardValidationMessage"))
             .putMember("DataPath", Schemas.DATA_PATH)
             .putMember("Message", Schemas.MESSAGE)
             .builderSupplier(DashboardValidationMessage::builder)
             .shapeClass(DashboardValidationMessage.class)
             .build();

    static final Schema DASHBOARD_VALIDATION_MESSAGES = Schema.listBuilder(ShapeId.from("com.amazonaws.cloudwatch#DashboardValidationMessages"))
        .putMember("member", Schemas.DASHBOARD_VALIDATION_MESSAGE)
        .build();

    static final Schema DASHBOARD_INVALID_INPUT_ERROR = Schema.structureBuilder(ShapeId.from("com.amazonaws.cloudwatch#DashboardInvalidInputError"),
            new ErrorTrait("client"),
            new HttpErrorTrait(400),
            new AwsQueryErrorTrait.Provider().createTrait(
                ShapeId.from("aws.protocols#awsQueryError"),
                Node.objectNodeBuilder()
                    .withMember("code", "InvalidParameterInput")
                    .withMember("httpResponseCode", 400L)
                    .build()
            ))
             .putMember("message", Schemas.DASHBOARD_ERROR_MESSAGE)
             .putMember("dashboardValidationMessages", Schemas.DASHBOARD_VALIDATION_MESSAGES)
             .builderSupplier(DashboardInvalidInputError::builder)
             .shapeClass(DashboardInvalidInputError.class)
             .build();

    static final Schema DASHBOARD_NAME_PREFIX = Schema.createString(ShapeId.from("com.amazonaws.cloudwatch#DashboardNamePrefix"));
    static final Schema DASHBOARD_NAMES = Schema.listBuilder(ShapeId.from("com.amazonaws.cloudwatch#DashboardNames"))
        .putMember("member", Schemas.DASHBOARD_NAME)
        .build();

    static final Schema DASHBOARD_NOT_FOUND_ERROR = Schema.structureBuilder(ShapeId.from("com.amazonaws.cloudwatch#DashboardNotFoundError"),
            new ErrorTrait("client"),
            new HttpErrorTrait(404),
            new AwsQueryErrorTrait.Provider().createTrait(
                ShapeId.from("aws.protocols#awsQueryError"),
                Node.objectNodeBuilder()
                    .withMember("code", "ResourceNotFound")
                    .withMember("httpResponseCode", 404L)
                    .build()
            ))
             .putMember("message", Schemas.DASHBOARD_ERROR_MESSAGE)
             .builderSupplier(DashboardNotFoundError::builder)
             .shapeClass(DashboardNotFoundError.class)
             .build();

    static final Schema EXTENDED_STATISTIC = Schema.createString(ShapeId.from("com.amazonaws.cloudwatch#ExtendedStatistic"));
    static final Schema DATAPOINT_VALUE_MAP = Schema.mapBuilder(ShapeId.from("com.amazonaws.cloudwatch#DatapointValueMap"))
        .putMember("key", Schemas.EXTENDED_STATISTIC)
        .putMember("value", Schemas.DATAPOINT_VALUE)
        .build();

    static final Schema DATAPOINT = Schema.structureBuilder(ShapeId.from("com.amazonaws.cloudwatch#Datapoint"))
             .putMember("Timestamp", Schemas.TIMESTAMP)
             .putMember("SampleCount", Schemas.DATAPOINT_VALUE)
             .putMember("Average", Schemas.DATAPOINT_VALUE)
             .putMember("Sum", Schemas.DATAPOINT_VALUE)
             .putMember("Minimum", Schemas.DATAPOINT_VALUE)
             .putMember("Maximum", Schemas.DATAPOINT_VALUE)
             .putMember("Unit", StandardUnit.$SCHEMA)
             .putMember("ExtendedStatistics", Schemas.DATAPOINT_VALUE_MAP)
             .builderSupplier(Datapoint::builder)
             .shapeClass(Datapoint.class)
             .build();

    static final Schema DATAPOINTS = Schema.listBuilder(ShapeId.from("com.amazonaws.cloudwatch#Datapoints"))
        .putMember("member", Schemas.DATAPOINT)
        .build();

    static final Schema DATAPOINTS_TO_ALARM = Schema.createInteger(ShapeId.from("com.amazonaws.cloudwatch#DatapointsToAlarm"),
            RangeTrait.builder().min(new BigDecimal("1")).build());
    static final Schema DATAPOINT_VALUES = Schema.listBuilder(ShapeId.from("com.amazonaws.cloudwatch#DatapointValues"))
        .putMember("member", Schemas.DATAPOINT_VALUE)
        .build();

    static final Schema DATASET_ARN = Schema.createString(ShapeId.from("com.amazonaws.cloudwatch#DatasetArn"),
            LengthTrait.builder().min(1L).max(2048L).build(),
            new PatternTrait("^arn:[a-zA-Z0-9-]+:cloudwatch:[a-zA-Z0-9-]*:\\d{12}:dataset/default$"));
    static final Schema DATASET_ID = Schema.createString(ShapeId.from("com.amazonaws.cloudwatch#DatasetId"),
            LengthTrait.builder().min(7L).max(7L).build(),
            new PatternTrait("^default$"));
    static final Schema NAME = Schema.createString(ShapeId.from("com.amazonaws.cloudwatch#Name"),
            LengthTrait.builder().min(1L).max(255L).build());
    static final Schema DELETE_ALARM_MUTE_RULE_INPUT = Schema.structureBuilder(ShapeId.from("com.amazonaws.cloudwatch#DeleteAlarmMuteRuleInput"))
             .putMember("AlarmMuteRuleName", Schemas.NAME,
                     new RequiredTrait())
             .builderSupplier(DeleteAlarmMuteRuleInput::builder)
             .shapeClass(DeleteAlarmMuteRuleInput.class)
             .build();

    static final Schema DELETE_ALARM_MUTE_RULE_OUTPUT = Schema.structureBuilder(ShapeId.from("com.amazonaws.cloudwatch#DeleteAlarmMuteRuleOutput"),
            new UnitTypeTrait()).builderSupplier(DeleteAlarmMuteRuleOutput::builder)
             .shapeClass(DeleteAlarmMuteRuleOutput.class)
             .build();

    static final Schema DELETE_ALARMS_INPUT = Schema.structureBuilder(ShapeId.from("com.amazonaws.cloudwatch#DeleteAlarmsInput"))
             .putMember("AlarmNames", Schemas.ALARM_NAMES,
                     new RequiredTrait())
             .builderSupplier(DeleteAlarmsInput::builder)
             .shapeClass(DeleteAlarmsInput.class)
             .build();

    static final Schema DELETE_ALARMS_OUTPUT = Schema.structureBuilder(ShapeId.from("com.amazonaws.cloudwatch#DeleteAlarmsOutput"),
            new UnitTypeTrait()).builderSupplier(DeleteAlarmsOutput::builder)
             .shapeClass(DeleteAlarmsOutput.class)
             .build();

    static final Schema RESOURCE_CONFLICT = Schema.structureBuilder(ShapeId.from("com.amazonaws.cloudwatch#ResourceConflict"),
            new ErrorTrait("client"),
            new HttpErrorTrait(409),
            new AwsQueryErrorTrait.Provider().createTrait(
                ShapeId.from("aws.protocols#awsQueryError"),
                Node.objectNodeBuilder()
                    .withMember("code", "ResourceConflict")
                    .withMember("httpResponseCode", 409L)
                    .build()
            ))
             .putMember("message", Schemas.ERROR_MESSAGE)
             .builderSupplier(ResourceConflict::builder)
             .shapeClass(ResourceConflict.class)
             .build();

    static final Schema RESOURCE_NOT_FOUND = Schema.structureBuilder(ShapeId.from("com.amazonaws.cloudwatch#ResourceNotFound"),
            new ErrorTrait("client"),
            new HttpErrorTrait(404),
            new AwsQueryErrorTrait.Provider().createTrait(
                ShapeId.from("aws.protocols#awsQueryError"),
                Node.objectNodeBuilder()
                    .withMember("code", "ResourceNotFound")
                    .withMember("httpResponseCode", 404L)
                    .build()
            ))
             .putMember("message", Schemas.ERROR_MESSAGE)
             .builderSupplier(ResourceNotFound::builder)
             .shapeClass(ResourceNotFound.class)
             .build();

    static final Schema DELETE_ANOMALY_DETECTOR_INPUT = Schema.structureBuilder(ShapeId.from("com.amazonaws.cloudwatch#DeleteAnomalyDetectorInput"))
             .putMember("AnomalyDetectorId", Schemas.ANOMALY_DETECTOR_ID)
             .putMember("Namespace", Schemas.NAMESPACE)
             .putMember("MetricName", Schemas.METRIC_NAME)
             .putMember("Dimensions", Schemas.DIMENSIONS)
             .putMember("Stat", Schemas.ANOMALY_DETECTOR_METRIC_STAT)
             .putMember("SingleMetricAnomalyDetector", Schemas.SINGLE_METRIC_ANOMALY_DETECTOR)
             .putMember("MetricMathAnomalyDetector", Schemas.METRIC_MATH_ANOMALY_DETECTOR)
             .builderSupplier(DeleteAnomalyDetectorInput::builder)
             .shapeClass(DeleteAnomalyDetectorInput.class)
             .build();

    static final Schema DELETE_ANOMALY_DETECTOR_OUTPUT = Schema.structureBuilder(ShapeId.from("com.amazonaws.cloudwatch#DeleteAnomalyDetectorOutput")).builderSupplier(DeleteAnomalyDetectorOutput::builder)
             .shapeClass(DeleteAnomalyDetectorOutput.class)
             .build();

    static final Schema INTERNAL_SERVICE_FAULT = Schema.structureBuilder(ShapeId.from("com.amazonaws.cloudwatch#InternalServiceFault"),
            new ErrorTrait("server"),
            new HttpErrorTrait(500),
            new AwsQueryErrorTrait.Provider().createTrait(
                ShapeId.from("aws.protocols#awsQueryError"),
                Node.objectNodeBuilder()
                    .withMember("code", "InternalServiceError")
                    .withMember("httpResponseCode", 500L)
                    .build()
            ))
             .putMember("Message", Schemas.FAULT_DESCRIPTION)
             .builderSupplier(InternalServiceFault::builder)
             .shapeClass(InternalServiceFault.class)
             .build();

    static final Schema INVALID_PARAMETER_COMBINATION_EXCEPTION = Schema.structureBuilder(ShapeId.from("com.amazonaws.cloudwatch#InvalidParameterCombinationException"),
            new ErrorTrait("client"),
            new HttpErrorTrait(400),
            new AwsQueryErrorTrait.Provider().createTrait(
                ShapeId.from("aws.protocols#awsQueryError"),
                Node.objectNodeBuilder()
                    .withMember("code", "InvalidParameterCombination")
                    .withMember("httpResponseCode", 400L)
                    .build()
            ))
             .putMember("message", Schemas.AWS_QUERY_ERROR_MESSAGE)
             .builderSupplier(InvalidParameterCombinationException::builder)
             .shapeClass(InvalidParameterCombinationException.class)
             .build();

    static final Schema INVALID_PARAMETER_VALUE_EXCEPTION = Schema.structureBuilder(ShapeId.from("com.amazonaws.cloudwatch#InvalidParameterValueException"),
            new ErrorTrait("client"),
            new HttpErrorTrait(400),
            new AwsQueryErrorTrait.Provider().createTrait(
                ShapeId.from("aws.protocols#awsQueryError"),
                Node.objectNodeBuilder()
                    .withMember("code", "InvalidParameterValue")
                    .withMember("httpResponseCode", 400L)
                    .build()
            ))
             .putMember("message", Schemas.AWS_QUERY_ERROR_MESSAGE)
             .builderSupplier(InvalidParameterValueException::builder)
             .shapeClass(InvalidParameterValueException.class)
             .build();

    static final Schema MISSING_REQUIRED_PARAMETER_EXCEPTION = Schema.structureBuilder(ShapeId.from("com.amazonaws.cloudwatch#MissingRequiredParameterException"),
            new ErrorTrait("client"),
            new HttpErrorTrait(400),
            new AwsQueryErrorTrait.Provider().createTrait(
                ShapeId.from("aws.protocols#awsQueryError"),
                Node.objectNodeBuilder()
                    .withMember("code", "MissingParameter")
                    .withMember("httpResponseCode", 400L)
                    .build()
            ))
             .putMember("message", Schemas.AWS_QUERY_ERROR_MESSAGE)
             .builderSupplier(MissingRequiredParameterException::builder)
             .shapeClass(MissingRequiredParameterException.class)
             .build();

    static final Schema DELETE_DASHBOARDS_INPUT = Schema.structureBuilder(ShapeId.from("com.amazonaws.cloudwatch#DeleteDashboardsInput"))
             .putMember("DashboardNames", Schemas.DASHBOARD_NAMES,
                     new RequiredTrait())
             .builderSupplier(DeleteDashboardsInput::builder)
             .shapeClass(DeleteDashboardsInput.class)
             .build();

    static final Schema DELETE_DASHBOARDS_OUTPUT = Schema.structureBuilder(ShapeId.from("com.amazonaws.cloudwatch#DeleteDashboardsOutput")).builderSupplier(DeleteDashboardsOutput::builder)
             .shapeClass(DeleteDashboardsOutput.class)
             .build();

    static final Schema INSIGHT_RULE_NAME = Schema.createString(ShapeId.from("com.amazonaws.cloudwatch#InsightRuleName"),
            LengthTrait.builder().min(1L).max(128L).build(),
            new PatternTrait("^[\\x20-\\x7E]+$"));
    static final Schema INSIGHT_RULE_NAMES = Schema.listBuilder(ShapeId.from("com.amazonaws.cloudwatch#InsightRuleNames"))
        .putMember("member", Schemas.INSIGHT_RULE_NAME)
        .build();

    static final Schema DELETE_INSIGHT_RULES_INPUT = Schema.structureBuilder(ShapeId.from("com.amazonaws.cloudwatch#DeleteInsightRulesInput"))
             .putMember("RuleNames", Schemas.INSIGHT_RULE_NAMES,
                     new RequiredTrait())
             .builderSupplier(DeleteInsightRulesInput::builder)
             .shapeClass(DeleteInsightRulesInput.class)
             .build();

    static final Schema DELETE_INSIGHT_RULES_OUTPUT = Schema.structureBuilder(ShapeId.from("com.amazonaws.cloudwatch#DeleteInsightRulesOutput"))
             .putMember("Failures", Schemas.BATCH_FAILURES)
             .builderSupplier(DeleteInsightRulesOutput::builder)
             .shapeClass(DeleteInsightRulesOutput.class)
             .build();

    static final Schema METRIC_STREAM_NAME = Schema.createString(ShapeId.from("com.amazonaws.cloudwatch#MetricStreamName"),
            LengthTrait.builder().min(1L).max(255L).build());
    static final Schema DELETE_METRIC_STREAM_INPUT = Schema.structureBuilder(ShapeId.from("com.amazonaws.cloudwatch#DeleteMetricStreamInput"))
             .putMember("Name", Schemas.METRIC_STREAM_NAME,
                     new RequiredTrait())
             .builderSupplier(DeleteMetricStreamInput::builder)
             .shapeClass(DeleteMetricStreamInput.class)
             .build();

    static final Schema DELETE_METRIC_STREAM_OUTPUT = Schema.structureBuilder(ShapeId.from("com.amazonaws.cloudwatch#DeleteMetricStreamOutput")).builderSupplier(DeleteMetricStreamOutput::builder)
             .shapeClass(DeleteMetricStreamOutput.class)
             .build();

    static final Schema NEXT_TOKEN = Schema.createString(ShapeId.from("com.amazonaws.cloudwatch#NextToken"));
    static final Schema DESCRIBE_ALARM_CONTRIBUTORS_INPUT = Schema.structureBuilder(ShapeId.from("com.amazonaws.cloudwatch#DescribeAlarmContributorsInput"))
             .putMember("AlarmName", Schemas.ALARM_NAME,
                     new RequiredTrait())
             .putMember("NextToken", Schemas.NEXT_TOKEN)
             .builderSupplier(DescribeAlarmContributorsInput::builder)
             .shapeClass(DescribeAlarmContributorsInput.class)
             .build();

    static final Schema DESCRIBE_ALARM_CONTRIBUTORS_OUTPUT = Schema.structureBuilder(ShapeId.from("com.amazonaws.cloudwatch#DescribeAlarmContributorsOutput"))
             .putMember("AlarmContributors", Schemas.ALARM_CONTRIBUTORS,
                     new RequiredTrait())
             .putMember("NextToken", Schemas.NEXT_TOKEN)
             .builderSupplier(DescribeAlarmContributorsOutput::builder)
             .shapeClass(DescribeAlarmContributorsOutput.class)
             .build();

    static final Schema INVALID_NEXT_TOKEN = Schema.structureBuilder(ShapeId.from("com.amazonaws.cloudwatch#InvalidNextToken"),
            new ErrorTrait("client"),
            new HttpErrorTrait(400),
            new AwsQueryErrorTrait.Provider().createTrait(
                ShapeId.from("aws.protocols#awsQueryError"),
                Node.objectNodeBuilder()
                    .withMember("code", "InvalidNextToken")
                    .withMember("httpResponseCode", 400L)
                    .build()
            ))
             .putMember("message", Schemas.ERROR_MESSAGE)
             .builderSupplier(InvalidNextToken::builder)
             .shapeClass(InvalidNextToken.class)
             .build();

    static final Schema MAX_RECORDS = Schema.createInteger(ShapeId.from("com.amazonaws.cloudwatch#MaxRecords"),
            RangeTrait.builder().min(new BigDecimal("1")).max(new BigDecimal("100")).build());
    static final Schema DESCRIBE_ALARM_HISTORY_INPUT = Schema.structureBuilder(ShapeId.from("com.amazonaws.cloudwatch#DescribeAlarmHistoryInput"))
             .putMember("AlarmName", Schemas.ALARM_NAME)
             .putMember("AlarmContributorId", Schemas.CONTRIBUTOR_ID)
             .putMember("AlarmTypes", Schemas.ALARM_TYPES)
             .putMember("HistoryItemType", HistoryItemType.$SCHEMA)
             .putMember("StartDate", Schemas.TIMESTAMP)
             .putMember("EndDate", Schemas.TIMESTAMP)
             .putMember("MaxRecords", Schemas.MAX_RECORDS)
             .putMember("NextToken", Schemas.NEXT_TOKEN)
             .putMember("ScanBy", ScanBy.$SCHEMA)
             .builderSupplier(DescribeAlarmHistoryInput::builder)
             .shapeClass(DescribeAlarmHistoryInput.class)
             .build();

    static final Schema DESCRIBE_ALARM_HISTORY_OUTPUT = Schema.structureBuilder(ShapeId.from("com.amazonaws.cloudwatch#DescribeAlarmHistoryOutput"))
             .putMember("AlarmHistoryItems", Schemas.ALARM_HISTORY_ITEMS)
             .putMember("NextToken", Schemas.NEXT_TOKEN)
             .builderSupplier(DescribeAlarmHistoryOutput::builder)
             .shapeClass(DescribeAlarmHistoryOutput.class)
             .build();

    static final Schema DESCRIBE_ALARMS_INPUT = Schema.structureBuilder(ShapeId.from("com.amazonaws.cloudwatch#DescribeAlarmsInput"))
             .putMember("AlarmNames", Schemas.ALARM_NAMES)
             .putMember("AlarmNamePrefix", Schemas.ALARM_NAME_PREFIX)
             .putMember("AlarmTypes", Schemas.ALARM_TYPES)
             .putMember("ChildrenOfAlarmName", Schemas.ALARM_NAME)
             .putMember("ParentsOfAlarmName", Schemas.ALARM_NAME)
             .putMember("StateValue", StateValue.$SCHEMA)
             .putMember("ActionPrefix", Schemas.ACTION_PREFIX)
             .putMember("MaxRecords", Schemas.MAX_RECORDS)
             .putMember("NextToken", Schemas.NEXT_TOKEN)
             .builderSupplier(DescribeAlarmsInput::builder)
             .shapeClass(DescribeAlarmsInput.class)
             .build();

    static final Schema QUERY_RESULTS_TO_ALARM = Schema.createInteger(ShapeId.from("com.amazonaws.cloudwatch#QueryResultsToAlarm"),
            RangeTrait.builder().min(new BigDecimal("1")).build());
    static final Schema QUERY_RESULTS_TO_EVALUATE = Schema.createInteger(ShapeId.from("com.amazonaws.cloudwatch#QueryResultsToEvaluate"),
            RangeTrait.builder().min(new BigDecimal("1")).build());
    static final Schema LOG_GROUP_IDENTIFIERS = Schema.listBuilder(ShapeId.from("com.amazonaws.cloudwatch#LogGroupIdentifiers"),
            LengthTrait.builder().min(1L).max(50L).build())
        .putMember("member", Schemas.AMAZON_RESOURCE_NAME)
        .build();

    static final Schema QUERY_STRING = Schema.createString(ShapeId.from("com.amazonaws.cloudwatch#QueryString"),
            LengthTrait.builder().min(0L).max(10000L).build());
    static final Schema END_TIME_OFFSET = Schema.createLong(ShapeId.from("com.amazonaws.cloudwatch#EndTimeOffset"),
            RangeTrait.builder().min(new BigDecimal("0")).max(new BigDecimal("2592000")).build());
    static final Schema SCHEDULE_EXPRESSION = Schema.createString(ShapeId.from("com.amazonaws.cloudwatch#ScheduleExpression"),
            LengthTrait.builder().min(1L).max(256L).build());
    static final Schema START_TIME_OFFSET = Schema.createLong(ShapeId.from("com.amazonaws.cloudwatch#StartTimeOffset"),
            RangeTrait.builder().min(new BigDecimal("1")).max(new BigDecimal("2592000")).build());
    static final Schema SCHEDULE_CONFIGURATION = Schema.structureBuilder(ShapeId.from("com.amazonaws.cloudwatch#ScheduleConfiguration"))
             .putMember("ScheduleExpression", Schemas.SCHEDULE_EXPRESSION,
                     new RequiredTrait())
             .putMember("StartTimeOffset", Schemas.START_TIME_OFFSET,
                     new RequiredTrait())
             .putMember("EndTimeOffset", Schemas.END_TIME_OFFSET)
             .builderSupplier(ScheduleConfiguration::builder)
             .shapeClass(ScheduleConfiguration.class)
             .build();

    static final Schema TAG_KEY = Schema.createString(ShapeId.from("com.amazonaws.cloudwatch#TagKey"),
            LengthTrait.builder().min(1L).max(128L).build());
    static final Schema TAG_VALUE = Schema.createString(ShapeId.from("com.amazonaws.cloudwatch#TagValue"),
            LengthTrait.builder().min(0L).max(256L).build());
    static final Schema TAG = Schema.structureBuilder(ShapeId.from("com.amazonaws.cloudwatch#Tag"))
             .putMember("Key", Schemas.TAG_KEY,
                     new RequiredTrait())
             .putMember("Value", Schemas.TAG_VALUE,
                     new RequiredTrait())
             .builderSupplier(Tag::builder)
             .shapeClass(Tag.class)
             .build();

    static final Schema TAG_LIST = Schema.listBuilder(ShapeId.from("com.amazonaws.cloudwatch#TagList"))
        .putMember("member", Schemas.TAG)
        .build();

    static final Schema SCHEDULED_QUERY_CONFIGURATION = Schema.structureBuilder(ShapeId.from("com.amazonaws.cloudwatch#ScheduledQueryConfiguration"))
             .putMember("QueryString", Schemas.QUERY_STRING,
                     new RequiredTrait())
             .putMember("LogGroupIdentifiers", Schemas.LOG_GROUP_IDENTIFIERS)
             .putMember("QueryARN", Schemas.AMAZON_RESOURCE_NAME)
             .putMember("ScheduledQueryRoleARN", Schemas.AMAZON_RESOURCE_NAME,
                     new RequiredTrait())
             .putMember("ScheduleConfiguration", Schemas.SCHEDULE_CONFIGURATION,
                     new RequiredTrait())
             .putMember("AggregationExpression", Schemas.AGGREGATION_EXPRESSION,
                     new RequiredTrait())
             .putMember("Tags", Schemas.TAG_LIST)
             .builderSupplier(ScheduledQueryConfiguration::builder)
             .shapeClass(ScheduledQueryConfiguration.class)
             .build();

    static final Schema THRESHOLD = Schema.createDouble(ShapeId.from("com.amazonaws.cloudwatch#Threshold"));
    static final Schema TREAT_MISSING_DATA = Schema.createString(ShapeId.from("com.amazonaws.cloudwatch#TreatMissingData"),
            LengthTrait.builder().min(1L).max(255L).build());
    static final Schema ONLY_START_EVALUATING_AFTER_WARM_UP_PERIOD_ENDS = Schema.createBoolean(ShapeId.from("com.amazonaws.cloudwatch#OnlyStartEvaluatingAfterWarmUpPeriodEnds"));
    static final Schema WARM_UP_PERIOD_DURATION_IN_MINUTES = Schema.createInteger(ShapeId.from("com.amazonaws.cloudwatch#WarmUpPeriodDurationInMinutes"),
            RangeTrait.builder().min(new BigDecimal("1")).max(new BigDecimal("2880")).build());
    static final Schema WARM_UP_CONFIGURATION = Schema.structureBuilder(ShapeId.from("com.amazonaws.cloudwatch#WarmUpConfiguration"))
             .putMember("WarmUpPeriodDurationInMinutes", Schemas.WARM_UP_PERIOD_DURATION_IN_MINUTES,
                     new RequiredTrait())
             .putMember("OnlyStartEvaluatingAfterWarmUpPeriodEnds", Schemas.ONLY_START_EVALUATING_AFTER_WARM_UP_PERIOD_ENDS)
             .builderSupplier(WarmUpConfiguration::builder)
             .shapeClass(WarmUpConfiguration.class)
             .build();

    static final Schema LOG_ALARM = Schema.structureBuilder(ShapeId.from("com.amazonaws.cloudwatch#LogAlarm"))
             .putMember("AlarmName", Schemas.ALARM_NAME)
             .putMember("AlarmArn", Schemas.ALARM_ARN)
             .putMember("AlarmDescription", Schemas.ALARM_DESCRIPTION)
             .putMember("AlarmConfigurationUpdatedTimestamp", Schemas.TIMESTAMP)
             .putMember("ActionsEnabled", Schemas.ACTIONS_ENABLED)
             .putMember("OKActions", Schemas.RESOURCE_LIST)
             .putMember("AlarmActions", Schemas.RESOURCE_LIST)
             .putMember("InsufficientDataActions", Schemas.RESOURCE_LIST)
             .putMember("StateValue", StateValue.$SCHEMA)
             .putMember("StateReason", Schemas.STATE_REASON)
             .putMember("StateReasonData", Schemas.STATE_REASON_DATA)
             .putMember("StateUpdatedTimestamp", Schemas.TIMESTAMP)
             .putMember("ScheduledQueryConfiguration", Schemas.SCHEDULED_QUERY_CONFIGURATION)
             .putMember("QueryResultsToEvaluate", Schemas.QUERY_RESULTS_TO_EVALUATE)
             .putMember("QueryResultsToAlarm", Schemas.QUERY_RESULTS_TO_ALARM)
             .putMember("Threshold", Schemas.THRESHOLD)
             .putMember("ComparisonOperator", ComparisonOperator.$SCHEMA)
             .putMember("TreatMissingData", Schemas.TREAT_MISSING_DATA)
             .putMember("StateTransitionedTimestamp", Schemas.TIMESTAMP)
             .putMember("EvaluationState", EvaluationState.$SCHEMA)
             .putMember("ActionLogLineCount", Schemas.ACTION_LOG_LINE_COUNT)
             .putMember("ActionLogLineRoleArn", Schemas.ACTION_LOG_LINE_ROLE_ARN)
             .putMember("WarmUpConfiguration", Schemas.WARM_UP_CONFIGURATION)
             .builderSupplier(LogAlarm::builder)
             .shapeClass(LogAlarm.class)
             .build();

    static final Schema LOG_ALARMS = Schema.listBuilder(ShapeId.from("com.amazonaws.cloudwatch#LogAlarms"))
        .putMember("member", Schemas.LOG_ALARM)
        .build();

    static final Schema EVALUATE_LOW_SAMPLE_COUNT_PERCENTILE = Schema.createString(ShapeId.from("com.amazonaws.cloudwatch#EvaluateLowSampleCountPercentile"),
            LengthTrait.builder().min(1L).max(255L).build());
    static final Schema EVALUATION_CRITERIA = Schema.unionBuilder(ShapeId.from("com.amazonaws.cloudwatch#EvaluationCriteria"))
             .putMember("PromQLCriteria", Schemas.ALARM_PROM_QL_CRITERIA)
             .builderSupplier(EvaluationCriteria::builder)
             .shapeClass(EvaluationCriteria.class)
             .build();

    static final Schema EVALUATION_INTERVAL = Schema.createInteger(ShapeId.from("com.amazonaws.cloudwatch#EvaluationInterval"),
            RangeTrait.builder().min(new BigDecimal("10")).max(new BigDecimal("3600")).build());
    static final Schema EVALUATION_PERIODS = Schema.createInteger(ShapeId.from("com.amazonaws.cloudwatch#EvaluationPeriods"),
            RangeTrait.builder().min(new BigDecimal("1")).build());
    static final Schema SLIDING_WINDOW = Schema.structureBuilder(ShapeId.from("com.amazonaws.cloudwatch#SlidingWindow")).builderSupplier(SlidingWindow::builder)
             .shapeClass(SlidingWindow.class)
             .build();

    static final Schema TIMEZONE = Schema.createString(ShapeId.from("com.amazonaws.cloudwatch#Timezone"),
            LengthTrait.builder().min(1L).max(50L).build());
    static final Schema WALL_CLOCK_WINDOW = Schema.structureBuilder(ShapeId.from("com.amazonaws.cloudwatch#WallClockWindow"))
             .putMember("Timezone", Schemas.TIMEZONE)
             .builderSupplier(WallClockWindow::builder)
             .shapeClass(WallClockWindow.class)
             .build();

    static final Schema EVALUATION_WINDOW = Schema.unionBuilder(ShapeId.from("com.amazonaws.cloudwatch#EvaluationWindow"))
             .putMember("WallClockWindow", Schemas.WALL_CLOCK_WINDOW)
             .putMember("SlidingWindow", Schemas.SLIDING_WINDOW)
             .builderSupplier(EvaluationWindow::builder)
             .shapeClass(EvaluationWindow.class)
             .build();

    static final Schema METRIC_ALARM = Schema.structureBuilder(ShapeId.from("com.amazonaws.cloudwatch#MetricAlarm"))
             .putMember("AlarmName", Schemas.ALARM_NAME)
             .putMember("AlarmArn", Schemas.ALARM_ARN)
             .putMember("AlarmDescription", Schemas.ALARM_DESCRIPTION)
             .putMember("AlarmConfigurationUpdatedTimestamp", Schemas.TIMESTAMP)
             .putMember("ActionsEnabled", Schemas.ACTIONS_ENABLED)
             .putMember("OKActions", Schemas.RESOURCE_LIST)
             .putMember("AlarmActions", Schemas.RESOURCE_LIST)
             .putMember("InsufficientDataActions", Schemas.RESOURCE_LIST)
             .putMember("StateValue", StateValue.$SCHEMA)
             .putMember("StateReason", Schemas.STATE_REASON)
             .putMember("StateReasonData", Schemas.STATE_REASON_DATA)
             .putMember("StateUpdatedTimestamp", Schemas.TIMESTAMP)
             .putMember("MetricName", Schemas.METRIC_NAME)
             .putMember("Namespace", Schemas.NAMESPACE)
             .putMember("Statistic", Statistic.$SCHEMA)
             .putMember("ExtendedStatistic", Schemas.EXTENDED_STATISTIC)
             .putMember("Dimensions", Schemas.DIMENSIONS)
             .putMember("Period", Schemas.PERIOD)
             .putMember("Unit", StandardUnit.$SCHEMA)
             .putMember("EvaluationPeriods", Schemas.EVALUATION_PERIODS)
             .putMember("DatapointsToAlarm", Schemas.DATAPOINTS_TO_ALARM)
             .putMember("Threshold", Schemas.THRESHOLD)
             .putMember("ComparisonOperator", ComparisonOperator.$SCHEMA)
             .putMember("TreatMissingData", Schemas.TREAT_MISSING_DATA)
             .putMember("EvaluateLowSampleCountPercentile", Schemas.EVALUATE_LOW_SAMPLE_COUNT_PERCENTILE)
             .putMember("Metrics", Schemas.METRIC_DATA_QUERIES)
             .putMember("ThresholdMetricId", Schemas.METRIC_ID)
             .putMember("EvaluationState", EvaluationState.$SCHEMA)
             .putMember("StateTransitionedTimestamp", Schemas.TIMESTAMP)
             .putMember("EvaluationWindow", Schemas.EVALUATION_WINDOW)
             .putMember("WarmUpConfiguration", Schemas.WARM_UP_CONFIGURATION)
             .putMember("EvaluationCriteria", Schemas.EVALUATION_CRITERIA)
             .putMember("EvaluationInterval", Schemas.EVALUATION_INTERVAL)
             .builderSupplier(MetricAlarm::builder)
             .shapeClass(MetricAlarm.class)
             .build();

    static final Schema METRIC_ALARMS = Schema.listBuilder(ShapeId.from("com.amazonaws.cloudwatch#MetricAlarms"))
        .putMember("member", Schemas.METRIC_ALARM)
        .build();

    static final Schema DESCRIBE_ALARMS_OUTPUT = Schema.structureBuilder(ShapeId.from("com.amazonaws.cloudwatch#DescribeAlarmsOutput"))
             .putMember("CompositeAlarms", Schemas.COMPOSITE_ALARMS)
             .putMember("MetricAlarms", Schemas.METRIC_ALARMS)
             .putMember("LogAlarms", Schemas.LOG_ALARMS)
             .putMember("NextToken", Schemas.NEXT_TOKEN)
             .builderSupplier(DescribeAlarmsOutput::builder)
             .shapeClass(DescribeAlarmsOutput.class)
             .build();

    static final Schema DESCRIBE_ALARMS_FOR_METRIC_INPUT = Schema.structureBuilder(ShapeId.from("com.amazonaws.cloudwatch#DescribeAlarmsForMetricInput"))
             .putMember("MetricName", Schemas.METRIC_NAME,
                     new RequiredTrait())
             .putMember("Namespace", Schemas.NAMESPACE,
                     new RequiredTrait())
             .putMember("Statistic", Statistic.$SCHEMA)
             .putMember("ExtendedStatistic", Schemas.EXTENDED_STATISTIC)
             .putMember("Dimensions", Schemas.DIMENSIONS)
             .putMember("Period", Schemas.PERIOD)
             .putMember("Unit", StandardUnit.$SCHEMA)
             .builderSupplier(DescribeAlarmsForMetricInput::builder)
             .shapeClass(DescribeAlarmsForMetricInput.class)
             .build();

    static final Schema DESCRIBE_ALARMS_FOR_METRIC_OUTPUT = Schema.structureBuilder(ShapeId.from("com.amazonaws.cloudwatch#DescribeAlarmsForMetricOutput"))
             .putMember("MetricAlarms", Schemas.METRIC_ALARMS)
             .builderSupplier(DescribeAlarmsForMetricOutput::builder)
             .shapeClass(DescribeAlarmsForMetricOutput.class)
             .build();

    static final Schema MAX_RETURNED_RESULTS_COUNT = Schema.createInteger(ShapeId.from("com.amazonaws.cloudwatch#MaxReturnedResultsCount"),
            RangeTrait.builder().min(new BigDecimal("1")).build());
    static final Schema DESCRIBE_ANOMALY_DETECTORS_INPUT = Schema.structureBuilder(ShapeId.from("com.amazonaws.cloudwatch#DescribeAnomalyDetectorsInput"))
             .putMember("AnomalyDetectorIds", Schemas.ANOMALY_DETECTOR_IDS)
             .putMember("NextToken", Schemas.NEXT_TOKEN)
             .putMember("MaxResults", Schemas.MAX_RETURNED_RESULTS_COUNT)
             .putMember("Namespace", Schemas.NAMESPACE)
             .putMember("MetricName", Schemas.METRIC_NAME)
             .putMember("Dimensions", Schemas.DIMENSIONS)
             .putMember("AnomalyDetectorTypes", Schemas.ANOMALY_DETECTOR_TYPES)
             .builderSupplier(DescribeAnomalyDetectorsInput::builder)
             .shapeClass(DescribeAnomalyDetectorsInput.class)
             .build();

    static final Schema DESCRIBE_ANOMALY_DETECTORS_OUTPUT = Schema.structureBuilder(ShapeId.from("com.amazonaws.cloudwatch#DescribeAnomalyDetectorsOutput"))
             .putMember("AnomalyDetectors", Schemas.ANOMALY_DETECTORS)
             .putMember("NextToken", Schemas.NEXT_TOKEN)
             .builderSupplier(DescribeAnomalyDetectorsOutput::builder)
             .shapeClass(DescribeAnomalyDetectorsOutput.class)
             .build();

    static final Schema INSIGHT_RULE_MAX_RESULTS = Schema.createInteger(ShapeId.from("com.amazonaws.cloudwatch#InsightRuleMaxResults"),
            RangeTrait.builder().min(new BigDecimal("1")).max(new BigDecimal("500")).build());
    static final Schema DESCRIBE_INSIGHT_RULES_INPUT = Schema.structureBuilder(ShapeId.from("com.amazonaws.cloudwatch#DescribeInsightRulesInput"))
             .putMember("NextToken", Schemas.NEXT_TOKEN)
             .putMember("MaxResults", Schemas.INSIGHT_RULE_MAX_RESULTS)
             .builderSupplier(DescribeInsightRulesInput::builder)
             .shapeClass(DescribeInsightRulesInput.class)
             .build();

    static final Schema INSIGHT_RULE_ON_TRANSFORMED_LOGS = Schema.createBoolean(ShapeId.from("com.amazonaws.cloudwatch#InsightRuleOnTransformedLogs"));
    static final Schema INSIGHT_RULE_DEFINITION = Schema.createString(ShapeId.from("com.amazonaws.cloudwatch#InsightRuleDefinition"),
            LengthTrait.builder().min(1L).max(8192L).build(),
            new PatternTrait("^[\\x00-\\x7F]+$"));
    static final Schema INSIGHT_RULE_IS_MANAGED = Schema.createBoolean(ShapeId.from("com.amazonaws.cloudwatch#InsightRuleIsManaged"));
    static final Schema INSIGHT_RULE_SCHEMA = Schema.createString(ShapeId.from("com.amazonaws.cloudwatch#InsightRuleSchema"));
    static final Schema INSIGHT_RULE_STATE = Schema.createString(ShapeId.from("com.amazonaws.cloudwatch#InsightRuleState"),
            LengthTrait.builder().min(1L).max(32L).build(),
            new PatternTrait("^[\\x20-\\x7E]+$"));
    static final Schema INSIGHT_RULE = Schema.structureBuilder(ShapeId.from("com.amazonaws.cloudwatch#InsightRule"))
             .putMember("Name", Schemas.INSIGHT_RULE_NAME,
                     new RequiredTrait())
             .putMember("State", Schemas.INSIGHT_RULE_STATE,
                     new RequiredTrait())
             .putMember("Schema", Schemas.INSIGHT_RULE_SCHEMA,
                     new RequiredTrait())
             .putMember("Definition", Schemas.INSIGHT_RULE_DEFINITION,
                     new RequiredTrait())
             .putMember("ManagedRule", Schemas.INSIGHT_RULE_IS_MANAGED)
             .putMember("ApplyOnTransformedLogs", Schemas.INSIGHT_RULE_ON_TRANSFORMED_LOGS)
             .builderSupplier(InsightRule::builder)
             .shapeClass(InsightRule.class)
             .build();

    static final Schema INSIGHT_RULES = Schema.listBuilder(ShapeId.from("com.amazonaws.cloudwatch#InsightRules"))
        .putMember("member", Schemas.INSIGHT_RULE)
        .build();

    static final Schema DESCRIBE_INSIGHT_RULES_OUTPUT = Schema.structureBuilder(ShapeId.from("com.amazonaws.cloudwatch#DescribeInsightRulesOutput"))
             .putMember("NextToken", Schemas.NEXT_TOKEN)
             .putMember("InsightRules", Schemas.INSIGHT_RULES)
             .builderSupplier(DescribeInsightRulesOutput::builder)
             .shapeClass(DescribeInsightRulesOutput.class)
             .build();

    static final Schema DIMENSION_FILTER = Schema.structureBuilder(ShapeId.from("com.amazonaws.cloudwatch#DimensionFilter"))
             .putMember("Name", Schemas.DIMENSION_NAME,
                     new RequiredTrait())
             .putMember("Value", Schemas.DIMENSION_VALUE)
             .builderSupplier(DimensionFilter::builder)
             .shapeClass(DimensionFilter.class)
             .build();

    static final Schema DIMENSION_FILTERS = Schema.listBuilder(ShapeId.from("com.amazonaws.cloudwatch#DimensionFilters"),
            LengthTrait.builder().min(0L).max(10L).build())
        .putMember("member", Schemas.DIMENSION_FILTER)
        .build();

    static final Schema DISABLE_ALARM_ACTIONS_INPUT = Schema.structureBuilder(ShapeId.from("com.amazonaws.cloudwatch#DisableAlarmActionsInput"))
             .putMember("AlarmNames", Schemas.ALARM_NAMES,
                     new RequiredTrait())
             .builderSupplier(DisableAlarmActionsInput::builder)
             .shapeClass(DisableAlarmActionsInput.class)
             .build();

    static final Schema DISABLE_ALARM_ACTIONS_OUTPUT = Schema.structureBuilder(ShapeId.from("com.amazonaws.cloudwatch#DisableAlarmActionsOutput"),
            new UnitTypeTrait()).builderSupplier(DisableAlarmActionsOutput::builder)
             .shapeClass(DisableAlarmActionsOutput.class)
             .build();

    static final Schema DISABLE_INSIGHT_RULES_INPUT = Schema.structureBuilder(ShapeId.from("com.amazonaws.cloudwatch#DisableInsightRulesInput"))
             .putMember("RuleNames", Schemas.INSIGHT_RULE_NAMES,
                     new RequiredTrait())
             .builderSupplier(DisableInsightRulesInput::builder)
             .shapeClass(DisableInsightRulesInput.class)
             .build();

    static final Schema DISABLE_INSIGHT_RULES_OUTPUT = Schema.structureBuilder(ShapeId.from("com.amazonaws.cloudwatch#DisableInsightRulesOutput"))
             .putMember("Failures", Schemas.BATCH_FAILURES)
             .builderSupplier(DisableInsightRulesOutput::builder)
             .shapeClass(DisableInsightRulesOutput.class)
             .build();

    static final Schema DISASSOCIATE_DATASET_KMS_KEY_INPUT = Schema.structureBuilder(ShapeId.from("com.amazonaws.cloudwatch#DisassociateDatasetKmsKeyInput"))
             .putMember("DatasetIdentifier", Schemas.DATASET_IDENTIFIER,
                     new RequiredTrait())
             .builderSupplier(DisassociateDatasetKmsKeyInput::builder)
             .shapeClass(DisassociateDatasetKmsKeyInput.class)
             .build();

    static final Schema DISASSOCIATE_DATASET_KMS_KEY_OUTPUT = Schema.structureBuilder(ShapeId.from("com.amazonaws.cloudwatch#DisassociateDatasetKmsKeyOutput")).builderSupplier(DisassociateDatasetKmsKeyOutput::builder)
             .shapeClass(DisassociateDatasetKmsKeyOutput.class)
             .build();

    static final Schema DURATION = Schema.createString(ShapeId.from("com.amazonaws.cloudwatch#Duration"),
            LengthTrait.builder().min(1L).max(50L).build());
    static final Schema ENABLE_ALARM_ACTIONS_INPUT = Schema.structureBuilder(ShapeId.from("com.amazonaws.cloudwatch#EnableAlarmActionsInput"))
             .putMember("AlarmNames", Schemas.ALARM_NAMES,
                     new RequiredTrait())
             .builderSupplier(EnableAlarmActionsInput::builder)
             .shapeClass(EnableAlarmActionsInput.class)
             .build();

    static final Schema ENABLE_ALARM_ACTIONS_OUTPUT = Schema.structureBuilder(ShapeId.from("com.amazonaws.cloudwatch#EnableAlarmActionsOutput"),
            new UnitTypeTrait()).builderSupplier(EnableAlarmActionsOutput::builder)
             .shapeClass(EnableAlarmActionsOutput.class)
             .build();

    static final Schema ENABLE_INSIGHT_RULES_INPUT = Schema.structureBuilder(ShapeId.from("com.amazonaws.cloudwatch#EnableInsightRulesInput"))
             .putMember("RuleNames", Schemas.INSIGHT_RULE_NAMES,
                     new RequiredTrait())
             .builderSupplier(EnableInsightRulesInput::builder)
             .shapeClass(EnableInsightRulesInput.class)
             .build();

    static final Schema ENABLE_INSIGHT_RULES_OUTPUT = Schema.structureBuilder(ShapeId.from("com.amazonaws.cloudwatch#EnableInsightRulesOutput"))
             .putMember("Failures", Schemas.BATCH_FAILURES)
             .builderSupplier(EnableInsightRulesOutput::builder)
             .shapeClass(EnableInsightRulesOutput.class)
             .build();

    static final Schema LIMIT_EXCEEDED_EXCEPTION = Schema.structureBuilder(ShapeId.from("com.amazonaws.cloudwatch#LimitExceededException"),
            new ErrorTrait("client"),
            new HttpErrorTrait(400),
            new AwsQueryErrorTrait.Provider().createTrait(
                ShapeId.from("aws.protocols#awsQueryError"),
                Node.objectNodeBuilder()
                    .withMember("code", "LimitExceededException")
                    .withMember("httpResponseCode", 400L)
                    .build()
            ))
             .putMember("Message", Schemas.FAULT_DESCRIPTION)
             .builderSupplier(LimitExceededException::builder)
             .shapeClass(LimitExceededException.class)
             .build();

    static final Schema ENTITY_ATTRIBUTES_MAP_KEY_STRING = Schema.createString(ShapeId.from("com.amazonaws.cloudwatch#EntityAttributesMapKeyString"),
            LengthTrait.builder().min(1L).max(256L).build());
    static final Schema ENTITY_ATTRIBUTES_MAP_VALUE_STRING = Schema.createString(ShapeId.from("com.amazonaws.cloudwatch#EntityAttributesMapValueString"),
            LengthTrait.builder().min(1L).max(2048L).build());
    static final Schema ENTITY_ATTRIBUTES_MAP = Schema.mapBuilder(ShapeId.from("com.amazonaws.cloudwatch#EntityAttributesMap"),
            LengthTrait.builder().min(0L).max(10L).build())
        .putMember("key", Schemas.ENTITY_ATTRIBUTES_MAP_KEY_STRING)
        .putMember("value", Schemas.ENTITY_ATTRIBUTES_MAP_VALUE_STRING)
        .build();

    static final Schema ENTITY_KEY_ATTRIBUTES_MAP_KEY_STRING = Schema.createString(ShapeId.from("com.amazonaws.cloudwatch#EntityKeyAttributesMapKeyString"),
            LengthTrait.builder().min(1L).max(32L).build());
    static final Schema ENTITY_KEY_ATTRIBUTES_MAP_VALUE_STRING = Schema.createString(ShapeId.from("com.amazonaws.cloudwatch#EntityKeyAttributesMapValueString"),
            LengthTrait.builder().min(1L).max(2048L).build());
    static final Schema ENTITY_KEY_ATTRIBUTES_MAP = Schema.mapBuilder(ShapeId.from("com.amazonaws.cloudwatch#EntityKeyAttributesMap"),
            LengthTrait.builder().min(2L).max(4L).build())
        .putMember("key", Schemas.ENTITY_KEY_ATTRIBUTES_MAP_KEY_STRING)
        .putMember("value", Schemas.ENTITY_KEY_ATTRIBUTES_MAP_VALUE_STRING)
        .build();

    static final Schema ENTITY = Schema.structureBuilder(ShapeId.from("com.amazonaws.cloudwatch#Entity"))
             .putMember("KeyAttributes", Schemas.ENTITY_KEY_ATTRIBUTES_MAP)
             .putMember("Attributes", Schemas.ENTITY_ATTRIBUTES_MAP)
             .builderSupplier(Entity::builder)
             .shapeClass(Entity.class)
             .build();

    static final Schema STATISTIC_SET = Schema.structureBuilder(ShapeId.from("com.amazonaws.cloudwatch#StatisticSet"))
             .putMember("SampleCount", Schemas.DATAPOINT_VALUE,
                     new RequiredTrait())
             .putMember("Sum", Schemas.DATAPOINT_VALUE,
                     new RequiredTrait())
             .putMember("Minimum", Schemas.DATAPOINT_VALUE,
                     new RequiredTrait())
             .putMember("Maximum", Schemas.DATAPOINT_VALUE,
                     new RequiredTrait())
             .builderSupplier(StatisticSet::builder)
             .shapeClass(StatisticSet.class)
             .build();

    static final Schema STORAGE_RESOLUTION = Schema.createInteger(ShapeId.from("com.amazonaws.cloudwatch#StorageResolution"),
            RangeTrait.builder().min(new BigDecimal("1")).build());
    static final Schema VALUES = Schema.listBuilder(ShapeId.from("com.amazonaws.cloudwatch#Values"))
        .putMember("member", Schemas.DATAPOINT_VALUE)
        .build();

    static final Schema METRIC_DATUM = Schema.structureBuilder(ShapeId.from("com.amazonaws.cloudwatch#MetricDatum"))
             .putMember("MetricName", Schemas.METRIC_NAME,
                     new RequiredTrait())
             .putMember("Dimensions", Schemas.DIMENSIONS)
             .putMember("Timestamp", Schemas.TIMESTAMP)
             .putMember("Value", Schemas.DATAPOINT_VALUE)
             .putMember("StatisticValues", Schemas.STATISTIC_SET)
             .putMember("Values", Schemas.VALUES)
             .putMember("Counts", Schemas.COUNTS)
             .putMember("Unit", StandardUnit.$SCHEMA)
             .putMember("StorageResolution", Schemas.STORAGE_RESOLUTION)
             .builderSupplier(MetricDatum::builder)
             .shapeClass(MetricDatum.class)
             .build();

    static final Schema METRIC_DATA = Schema.listBuilder(ShapeId.from("com.amazonaws.cloudwatch#MetricData"))
        .putMember("member", Schemas.METRIC_DATUM)
        .build();

    static final Schema ENTITY_METRIC_DATA = Schema.structureBuilder(ShapeId.from("com.amazonaws.cloudwatch#EntityMetricData"))
             .putMember("Entity", Schemas.ENTITY)
             .putMember("MetricData", Schemas.METRIC_DATA)
             .builderSupplier(EntityMetricData::builder)
             .shapeClass(EntityMetricData.class)
             .build();

    static final Schema ENTITY_METRIC_DATA_LIST = Schema.listBuilder(ShapeId.from("com.amazonaws.cloudwatch#EntityMetricDataList"))
        .putMember("member", Schemas.ENTITY_METRIC_DATA)
        .build();

    static final Schema EXPRESSION = Schema.createString(ShapeId.from("com.amazonaws.cloudwatch#Expression"),
            LengthTrait.builder().min(1L).max(256L).build());
    static final Schema EXTENDED_STATISTICS = Schema.listBuilder(ShapeId.from("com.amazonaws.cloudwatch#ExtendedStatistics"),
            LengthTrait.builder().min(1L).max(10L).build())
        .putMember("member", Schemas.EXTENDED_STATISTIC)
        .build();

    static final Schema GET_ALARM_MUTE_RULE_INPUT = Schema.structureBuilder(ShapeId.from("com.amazonaws.cloudwatch#GetAlarmMuteRuleInput"))
             .putMember("AlarmMuteRuleName", Schemas.NAME,
                     new RequiredTrait())
             .builderSupplier(GetAlarmMuteRuleInput::builder)
             .shapeClass(GetAlarmMuteRuleInput.class)
             .build();

    static final Schema MUTE_TARGET_ALARM_NAME_LIST = Schema.listBuilder(ShapeId.from("com.amazonaws.cloudwatch#MuteTargetAlarmNameList"),
            LengthTrait.builder().min(0L).max(100L).build())
        .putMember("member", Schemas.NAME)
        .build();

    static final Schema MUTE_TARGETS = Schema.structureBuilder(ShapeId.from("com.amazonaws.cloudwatch#MuteTargets"))
             .putMember("AlarmNames", Schemas.MUTE_TARGET_ALARM_NAME_LIST,
                     new RequiredTrait())
             .builderSupplier(MuteTargets::builder)
             .shapeClass(MuteTargets.class)
             .build();

    static final Schema SCHEDULE = Schema.structureBuilder(ShapeId.from("com.amazonaws.cloudwatch#Schedule"))
             .putMember("Expression", Schemas.EXPRESSION,
                     new RequiredTrait())
             .putMember("Duration", Schemas.DURATION,
                     new RequiredTrait())
             .putMember("Timezone", Schemas.TIMEZONE)
             .builderSupplier(Schedule::builder)
             .shapeClass(Schedule.class)
             .build();

    static final Schema RULE = Schema.structureBuilder(ShapeId.from("com.amazonaws.cloudwatch#Rule"))
             .putMember("Schedule", Schemas.SCHEDULE,
                     new RequiredTrait())
             .builderSupplier(Rule::builder)
             .shapeClass(Rule.class)
             .build();

    static final Schema GET_ALARM_MUTE_RULE_OUTPUT = Schema.structureBuilder(ShapeId.from("com.amazonaws.cloudwatch#GetAlarmMuteRuleOutput"))
             .putMember("Name", Schemas.NAME)
             .putMember("AlarmMuteRuleArn", Schemas.ARN)
             .putMember("Description", Schemas.ALARM_DESCRIPTION)
             .putMember("Rule", Schemas.RULE)
             .putMember("MuteTargets", Schemas.MUTE_TARGETS)
             .putMember("StartDate", Schemas.TIMESTAMP)
             .putMember("ExpireDate", Schemas.TIMESTAMP)
             .putMember("Status", AlarmMuteRuleStatus.$SCHEMA)
             .putMember("LastUpdatedTimestamp", Schemas.TIMESTAMP)
             .putMember("MuteType", Schemas.MUTE_TYPE)
             .builderSupplier(GetAlarmMuteRuleOutput::builder)
             .shapeClass(GetAlarmMuteRuleOutput.class)
             .build();

    static final Schema GET_DASHBOARD_INPUT = Schema.structureBuilder(ShapeId.from("com.amazonaws.cloudwatch#GetDashboardInput"))
             .putMember("DashboardName", Schemas.DASHBOARD_NAME,
                     new RequiredTrait())
             .builderSupplier(GetDashboardInput::builder)
             .shapeClass(GetDashboardInput.class)
             .build();

    static final Schema GET_DASHBOARD_OUTPUT = Schema.structureBuilder(ShapeId.from("com.amazonaws.cloudwatch#GetDashboardOutput"))
             .putMember("DashboardArn", Schemas.DASHBOARD_ARN)
             .putMember("DashboardBody", Schemas.DASHBOARD_BODY)
             .putMember("DashboardName", Schemas.DASHBOARD_NAME)
             .builderSupplier(GetDashboardOutput::builder)
             .shapeClass(GetDashboardOutput.class)
             .build();

    static final Schema GET_DATASET_INPUT = Schema.structureBuilder(ShapeId.from("com.amazonaws.cloudwatch#GetDatasetInput"))
             .putMember("DatasetIdentifier", Schemas.DATASET_IDENTIFIER,
                     new RequiredTrait())
             .builderSupplier(GetDatasetInput::builder)
             .shapeClass(GetDatasetInput.class)
             .build();

    static final Schema GET_DATASET_OUTPUT = Schema.structureBuilder(ShapeId.from("com.amazonaws.cloudwatch#GetDatasetOutput"))
             .putMember("DatasetId", Schemas.DATASET_ID,
                     new RequiredTrait())
             .putMember("Arn", Schemas.DATASET_ARN,
                     new RequiredTrait())
             .putMember("KmsKeyArn", Schemas.KMS_KEY_ARN)
             .builderSupplier(GetDatasetOutput::builder)
             .shapeClass(GetDatasetOutput.class)
             .build();

    static final Schema INSIGHT_RULE_UNBOUND_INTEGER = Schema.createInteger(ShapeId.from("com.amazonaws.cloudwatch#InsightRuleUnboundInteger"));
    static final Schema INSIGHT_RULE_METRIC_NAME = Schema.createString(ShapeId.from("com.amazonaws.cloudwatch#InsightRuleMetricName"),
            LengthTrait.builder().min(1L).max(32L).build(),
            new PatternTrait("^[\\x20-\\x7E]+$"));
    static final Schema INSIGHT_RULE_METRIC_LIST = Schema.listBuilder(ShapeId.from("com.amazonaws.cloudwatch#InsightRuleMetricList"))
        .putMember("member", Schemas.INSIGHT_RULE_METRIC_NAME)
        .build();

    static final Schema INSIGHT_RULE_ORDER_BY = Schema.createString(ShapeId.from("com.amazonaws.cloudwatch#InsightRuleOrderBy"),
            LengthTrait.builder().min(1L).max(32L).build(),
            new PatternTrait("^[\\x20-\\x7E]+$"));
    static final Schema GET_INSIGHT_RULE_REPORT_INPUT = Schema.structureBuilder(ShapeId.from("com.amazonaws.cloudwatch#GetInsightRuleReportInput"))
             .putMember("RuleName", Schemas.INSIGHT_RULE_NAME,
                     new RequiredTrait())
             .putMember("StartTime", Schemas.TIMESTAMP,
                     new RequiredTrait())
             .putMember("EndTime", Schemas.TIMESTAMP,
                     new RequiredTrait())
             .putMember("Period", Schemas.PERIOD,
                     new RequiredTrait())
             .putMember("MaxContributorCount", Schemas.INSIGHT_RULE_UNBOUND_INTEGER)
             .putMember("Metrics", Schemas.INSIGHT_RULE_METRIC_LIST)
             .putMember("OrderBy", Schemas.INSIGHT_RULE_ORDER_BY)
             .builderSupplier(GetInsightRuleReportInput::builder)
             .shapeClass(GetInsightRuleReportInput.class)
             .build();

    static final Schema INSIGHT_RULE_UNBOUND_DOUBLE = Schema.createDouble(ShapeId.from("com.amazonaws.cloudwatch#InsightRuleUnboundDouble"));
    static final Schema INSIGHT_RULE_AGGREGATION_STATISTIC = Schema.createString(ShapeId.from("com.amazonaws.cloudwatch#InsightRuleAggregationStatistic"));
    static final Schema INSIGHT_RULE_UNBOUND_LONG = Schema.createLong(ShapeId.from("com.amazonaws.cloudwatch#InsightRuleUnboundLong"));
    static final Schema INSIGHT_RULE_CONTRIBUTOR_DATAPOINT = Schema.structureBuilder(ShapeId.from("com.amazonaws.cloudwatch#InsightRuleContributorDatapoint"))
             .putMember("Timestamp", Schemas.TIMESTAMP,
                     new RequiredTrait())
             .putMember("ApproximateValue", Schemas.INSIGHT_RULE_UNBOUND_DOUBLE,
                     new RequiredTrait())
             .builderSupplier(InsightRuleContributorDatapoint::builder)
             .shapeClass(InsightRuleContributorDatapoint.class)
             .build();

    static final Schema INSIGHT_RULE_CONTRIBUTOR_DATAPOINTS = Schema.listBuilder(ShapeId.from("com.amazonaws.cloudwatch#InsightRuleContributorDatapoints"))
        .putMember("member", Schemas.INSIGHT_RULE_CONTRIBUTOR_DATAPOINT)
        .build();

    static final Schema INSIGHT_RULE_CONTRIBUTOR_KEY = Schema.createString(ShapeId.from("com.amazonaws.cloudwatch#InsightRuleContributorKey"));
    static final Schema INSIGHT_RULE_CONTRIBUTOR_KEYS = Schema.listBuilder(ShapeId.from("com.amazonaws.cloudwatch#InsightRuleContributorKeys"))
        .putMember("member", Schemas.INSIGHT_RULE_CONTRIBUTOR_KEY)
        .build();

    static final Schema INSIGHT_RULE_CONTRIBUTOR = Schema.structureBuilder(ShapeId.from("com.amazonaws.cloudwatch#InsightRuleContributor"))
             .putMember("Keys", Schemas.INSIGHT_RULE_CONTRIBUTOR_KEYS,
                     new RequiredTrait())
             .putMember("ApproximateAggregateValue", Schemas.INSIGHT_RULE_UNBOUND_DOUBLE,
                     new RequiredTrait())
             .putMember("Datapoints", Schemas.INSIGHT_RULE_CONTRIBUTOR_DATAPOINTS,
                     new RequiredTrait())
             .builderSupplier(InsightRuleContributor::builder)
             .shapeClass(InsightRuleContributor.class)
             .build();

    static final Schema INSIGHT_RULE_CONTRIBUTORS = Schema.listBuilder(ShapeId.from("com.amazonaws.cloudwatch#InsightRuleContributors"))
        .putMember("member", Schemas.INSIGHT_RULE_CONTRIBUTOR)
        .build();

    static final Schema INSIGHT_RULE_CONTRIBUTOR_KEY_LABEL = Schema.createString(ShapeId.from("com.amazonaws.cloudwatch#InsightRuleContributorKeyLabel"));
    static final Schema INSIGHT_RULE_CONTRIBUTOR_KEY_LABELS = Schema.listBuilder(ShapeId.from("com.amazonaws.cloudwatch#InsightRuleContributorKeyLabels"))
        .putMember("member", Schemas.INSIGHT_RULE_CONTRIBUTOR_KEY_LABEL)
        .build();

    static final Schema INSIGHT_RULE_METRIC_DATAPOINT = Schema.structureBuilder(ShapeId.from("com.amazonaws.cloudwatch#InsightRuleMetricDatapoint"))
             .putMember("Timestamp", Schemas.TIMESTAMP,
                     new RequiredTrait())
             .putMember("UniqueContributors", Schemas.INSIGHT_RULE_UNBOUND_DOUBLE)
             .putMember("MaxContributorValue", Schemas.INSIGHT_RULE_UNBOUND_DOUBLE)
             .putMember("SampleCount", Schemas.INSIGHT_RULE_UNBOUND_DOUBLE)
             .putMember("Average", Schemas.INSIGHT_RULE_UNBOUND_DOUBLE)
             .putMember("Sum", Schemas.INSIGHT_RULE_UNBOUND_DOUBLE)
             .putMember("Minimum", Schemas.INSIGHT_RULE_UNBOUND_DOUBLE)
             .putMember("Maximum", Schemas.INSIGHT_RULE_UNBOUND_DOUBLE)
             .builderSupplier(InsightRuleMetricDatapoint::builder)
             .shapeClass(InsightRuleMetricDatapoint.class)
             .build();

    static final Schema INSIGHT_RULE_METRIC_DATAPOINTS = Schema.listBuilder(ShapeId.from("com.amazonaws.cloudwatch#InsightRuleMetricDatapoints"))
        .putMember("member", Schemas.INSIGHT_RULE_METRIC_DATAPOINT)
        .build();

    static final Schema GET_INSIGHT_RULE_REPORT_OUTPUT = Schema.structureBuilder(ShapeId.from("com.amazonaws.cloudwatch#GetInsightRuleReportOutput"))
             .putMember("KeyLabels", Schemas.INSIGHT_RULE_CONTRIBUTOR_KEY_LABELS)
             .putMember("AggregationStatistic", Schemas.INSIGHT_RULE_AGGREGATION_STATISTIC)
             .putMember("AggregateValue", Schemas.INSIGHT_RULE_UNBOUND_DOUBLE)
             .putMember("ApproximateUniqueCount", Schemas.INSIGHT_RULE_UNBOUND_LONG)
             .putMember("Contributors", Schemas.INSIGHT_RULE_CONTRIBUTORS)
             .putMember("MetricDatapoints", Schemas.INSIGHT_RULE_METRIC_DATAPOINTS)
             .builderSupplier(GetInsightRuleReportOutput::builder)
             .shapeClass(GetInsightRuleReportOutput.class)
             .build();

    static final Schema GET_METRIC_DATA_LABEL_TIMEZONE = Schema.createString(ShapeId.from("com.amazonaws.cloudwatch#GetMetricDataLabelTimezone"));
    static final Schema LABEL_OPTIONS = Schema.structureBuilder(ShapeId.from("com.amazonaws.cloudwatch#LabelOptions"))
             .putMember("Timezone", Schemas.GET_METRIC_DATA_LABEL_TIMEZONE)
             .builderSupplier(LabelOptions::builder)
             .shapeClass(LabelOptions.class)
             .build();

    static final Schema GET_METRIC_DATA_MAX_DATAPOINTS = Schema.createInteger(ShapeId.from("com.amazonaws.cloudwatch#GetMetricDataMaxDatapoints"));
    static final Schema GET_METRIC_DATA_INPUT = Schema.structureBuilder(ShapeId.from("com.amazonaws.cloudwatch#GetMetricDataInput"))
             .putMember("MetricDataQueries", Schemas.METRIC_DATA_QUERIES,
                     new RequiredTrait())
             .putMember("StartTime", Schemas.TIMESTAMP,
                     new RequiredTrait())
             .putMember("EndTime", Schemas.TIMESTAMP,
                     new RequiredTrait())
             .putMember("NextToken", Schemas.NEXT_TOKEN)
             .putMember("ScanBy", ScanBy.$SCHEMA)
             .putMember("MaxDatapoints", Schemas.GET_METRIC_DATA_MAX_DATAPOINTS)
             .putMember("LabelOptions", Schemas.LABEL_OPTIONS)
             .builderSupplier(GetMetricDataInput::builder)
             .shapeClass(GetMetricDataInput.class)
             .build();

    static final Schema MESSAGE_DATA_CODE = Schema.createString(ShapeId.from("com.amazonaws.cloudwatch#MessageDataCode"));
    static final Schema MESSAGE_DATA_VALUE = Schema.createString(ShapeId.from("com.amazonaws.cloudwatch#MessageDataValue"));
    static final Schema MESSAGE_DATA = Schema.structureBuilder(ShapeId.from("com.amazonaws.cloudwatch#MessageData"))
             .putMember("Code", Schemas.MESSAGE_DATA_CODE)
             .putMember("Value", Schemas.MESSAGE_DATA_VALUE)
             .builderSupplier(MessageData::builder)
             .shapeClass(MessageData.class)
             .build();

    static final Schema METRIC_DATA_RESULT_MESSAGES = Schema.listBuilder(ShapeId.from("com.amazonaws.cloudwatch#MetricDataResultMessages"))
        .putMember("member", Schemas.MESSAGE_DATA)
        .build();

    static final Schema TIMESTAMPS = Schema.listBuilder(ShapeId.from("com.amazonaws.cloudwatch#Timestamps"))
        .putMember("member", Schemas.TIMESTAMP)
        .build();

    static final Schema METRIC_DATA_RESULT = Schema.structureBuilder(ShapeId.from("com.amazonaws.cloudwatch#MetricDataResult"))
             .putMember("Id", Schemas.METRIC_ID)
             .putMember("Label", Schemas.METRIC_LABEL)
             .putMember("Timestamps", Schemas.TIMESTAMPS)
             .putMember("Values", Schemas.DATAPOINT_VALUES)
             .putMember("StatusCode", StatusCode.$SCHEMA)
             .putMember("Messages", Schemas.METRIC_DATA_RESULT_MESSAGES)
             .builderSupplier(MetricDataResult::builder)
             .shapeClass(MetricDataResult.class)
             .build();

    static final Schema METRIC_DATA_RESULTS = Schema.listBuilder(ShapeId.from("com.amazonaws.cloudwatch#MetricDataResults"))
        .putMember("member", Schemas.METRIC_DATA_RESULT)
        .build();

    static final Schema GET_METRIC_DATA_OUTPUT = Schema.structureBuilder(ShapeId.from("com.amazonaws.cloudwatch#GetMetricDataOutput"))
             .putMember("MetricDataResults", Schemas.METRIC_DATA_RESULTS)
             .putMember("NextToken", Schemas.NEXT_TOKEN)
             .putMember("Messages", Schemas.METRIC_DATA_RESULT_MESSAGES)
             .builderSupplier(GetMetricDataOutput::builder)
             .shapeClass(GetMetricDataOutput.class)
             .build();

    static final Schema STATISTICS = Schema.listBuilder(ShapeId.from("com.amazonaws.cloudwatch#Statistics"),
            LengthTrait.builder().min(1L).max(5L).build())
        .putMember("member", Statistic.$SCHEMA)
        .build();

    static final Schema GET_METRIC_STATISTICS_INPUT = Schema.structureBuilder(ShapeId.from("com.amazonaws.cloudwatch#GetMetricStatisticsInput"))
             .putMember("Namespace", Schemas.NAMESPACE,
                     new RequiredTrait())
             .putMember("MetricName", Schemas.METRIC_NAME,
                     new RequiredTrait())
             .putMember("Dimensions", Schemas.DIMENSIONS)
             .putMember("StartTime", Schemas.TIMESTAMP,
                     new RequiredTrait())
             .putMember("EndTime", Schemas.TIMESTAMP,
                     new RequiredTrait())
             .putMember("Period", Schemas.PERIOD,
                     new RequiredTrait())
             .putMember("Statistics", Schemas.STATISTICS)
             .putMember("ExtendedStatistics", Schemas.EXTENDED_STATISTICS)
             .putMember("Unit", StandardUnit.$SCHEMA)
             .builderSupplier(GetMetricStatisticsInput::builder)
             .shapeClass(GetMetricStatisticsInput.class)
             .build();

    static final Schema GET_METRIC_STATISTICS_OUTPUT = Schema.structureBuilder(ShapeId.from("com.amazonaws.cloudwatch#GetMetricStatisticsOutput"))
             .putMember("Label", Schemas.METRIC_LABEL)
             .putMember("Datapoints", Schemas.DATAPOINTS)
             .builderSupplier(GetMetricStatisticsOutput::builder)
             .shapeClass(GetMetricStatisticsOutput.class)
             .build();

    static final Schema GET_METRIC_STREAM_INPUT = Schema.structureBuilder(ShapeId.from("com.amazonaws.cloudwatch#GetMetricStreamInput"))
             .putMember("Name", Schemas.METRIC_STREAM_NAME,
                     new RequiredTrait())
             .builderSupplier(GetMetricStreamInput::builder)
             .shapeClass(GetMetricStreamInput.class)
             .build();

    static final Schema METRIC_STREAM_FILTER_METRIC_NAMES = Schema.listBuilder(ShapeId.from("com.amazonaws.cloudwatch#MetricStreamFilterMetricNames"))
        .putMember("member", Schemas.METRIC_NAME)
        .build();

    static final Schema METRIC_STREAM_FILTER = Schema.structureBuilder(ShapeId.from("com.amazonaws.cloudwatch#MetricStreamFilter"))
             .putMember("Namespace", Schemas.NAMESPACE)
             .putMember("MetricNames", Schemas.METRIC_STREAM_FILTER_METRIC_NAMES)
             .builderSupplier(MetricStreamFilter::builder)
             .shapeClass(MetricStreamFilter.class)
             .build();

    static final Schema METRIC_STREAM_FILTERS = Schema.listBuilder(ShapeId.from("com.amazonaws.cloudwatch#MetricStreamFilters"))
        .putMember("member", Schemas.METRIC_STREAM_FILTER)
        .build();

    static final Schema INCLUDE_LINKED_ACCOUNTS_METRICS = Schema.createBoolean(ShapeId.from("com.amazonaws.cloudwatch#IncludeLinkedAccountsMetrics"));
    static final Schema METRIC_STREAM_STATE = Schema.createString(ShapeId.from("com.amazonaws.cloudwatch#MetricStreamState"));
    static final Schema METRIC_STREAM_STATISTIC = Schema.createString(ShapeId.from("com.amazonaws.cloudwatch#MetricStreamStatistic"));
    static final Schema METRIC_STREAM_STATISTICS_ADDITIONAL_STATISTICS = Schema.listBuilder(ShapeId.from("com.amazonaws.cloudwatch#MetricStreamStatisticsAdditionalStatistics"))
        .putMember("member", Schemas.METRIC_STREAM_STATISTIC)
        .build();

    static final Schema METRIC_STREAM_STATISTICS_METRIC = Schema.structureBuilder(ShapeId.from("com.amazonaws.cloudwatch#MetricStreamStatisticsMetric"))
             .putMember("Namespace", Schemas.NAMESPACE,
                     new RequiredTrait())
             .putMember("MetricName", Schemas.METRIC_NAME,
                     new RequiredTrait())
             .builderSupplier(MetricStreamStatisticsMetric::builder)
             .shapeClass(MetricStreamStatisticsMetric.class)
             .build();

    static final Schema METRIC_STREAM_STATISTICS_INCLUDE_METRICS = Schema.listBuilder(ShapeId.from("com.amazonaws.cloudwatch#MetricStreamStatisticsIncludeMetrics"))
        .putMember("member", Schemas.METRIC_STREAM_STATISTICS_METRIC)
        .build();

    static final Schema METRIC_STREAM_STATISTICS_CONFIGURATION = Schema.structureBuilder(ShapeId.from("com.amazonaws.cloudwatch#MetricStreamStatisticsConfiguration"))
             .putMember("IncludeMetrics", Schemas.METRIC_STREAM_STATISTICS_INCLUDE_METRICS,
                     new RequiredTrait())
             .putMember("AdditionalStatistics", Schemas.METRIC_STREAM_STATISTICS_ADDITIONAL_STATISTICS,
                     new RequiredTrait())
             .builderSupplier(MetricStreamStatisticsConfiguration::builder)
             .shapeClass(MetricStreamStatisticsConfiguration.class)
             .build();

    static final Schema METRIC_STREAM_STATISTICS_CONFIGURATIONS = Schema.listBuilder(ShapeId.from("com.amazonaws.cloudwatch#MetricStreamStatisticsConfigurations"))
        .putMember("member", Schemas.METRIC_STREAM_STATISTICS_CONFIGURATION)
        .build();

    static final Schema GET_METRIC_STREAM_OUTPUT = Schema.structureBuilder(ShapeId.from("com.amazonaws.cloudwatch#GetMetricStreamOutput"))
             .putMember("Arn", Schemas.AMAZON_RESOURCE_NAME)
             .putMember("Name", Schemas.METRIC_STREAM_NAME)
             .putMember("IncludeFilters", Schemas.METRIC_STREAM_FILTERS)
             .putMember("ExcludeFilters", Schemas.METRIC_STREAM_FILTERS)
             .putMember("FirehoseArn", Schemas.AMAZON_RESOURCE_NAME)
             .putMember("RoleArn", Schemas.AMAZON_RESOURCE_NAME)
             .putMember("State", Schemas.METRIC_STREAM_STATE)
             .putMember("CreationDate", Schemas.TIMESTAMP)
             .putMember("LastUpdateDate", Schemas.TIMESTAMP)
             .putMember("OutputFormat", MetricStreamOutputFormat.$SCHEMA)
             .putMember("StatisticsConfigurations", Schemas.METRIC_STREAM_STATISTICS_CONFIGURATIONS)
             .putMember("IncludeLinkedAccountsMetrics", Schemas.INCLUDE_LINKED_ACCOUNTS_METRICS)
             .builderSupplier(GetMetricStreamOutput::builder)
             .shapeClass(GetMetricStreamOutput.class)
             .build();

    static final Schema METRIC_WIDGET = Schema.createString(ShapeId.from("com.amazonaws.cloudwatch#MetricWidget"));
    static final Schema OUTPUT_FORMAT = Schema.createString(ShapeId.from("com.amazonaws.cloudwatch#OutputFormat"));
    static final Schema GET_METRIC_WIDGET_IMAGE_INPUT = Schema.structureBuilder(ShapeId.from("com.amazonaws.cloudwatch#GetMetricWidgetImageInput"))
             .putMember("MetricWidget", Schemas.METRIC_WIDGET,
                     new RequiredTrait())
             .putMember("OutputFormat", Schemas.OUTPUT_FORMAT)
             .builderSupplier(GetMetricWidgetImageInput::builder)
             .shapeClass(GetMetricWidgetImageInput.class)
             .build();

    static final Schema METRIC_WIDGET_IMAGE = Schema.createBlob(ShapeId.from("com.amazonaws.cloudwatch#MetricWidgetImage"));
    static final Schema GET_METRIC_WIDGET_IMAGE_OUTPUT = Schema.structureBuilder(ShapeId.from("com.amazonaws.cloudwatch#GetMetricWidgetImageOutput"))
             .putMember("MetricWidgetImage", Schemas.METRIC_WIDGET_IMAGE)
             .builderSupplier(GetMetricWidgetImageOutput::builder)
             .shapeClass(GetMetricWidgetImageOutput.class)
             .build();

    static final Schema GET_O_TEL_ENRICHMENT_INPUT = Schema.structureBuilder(ShapeId.from("com.amazonaws.cloudwatch#GetOTelEnrichmentInput")).builderSupplier(GetOTelEnrichmentInput::builder)
             .shapeClass(GetOTelEnrichmentInput.class)
             .build();

    static final Schema GET_O_TEL_ENRICHMENT_OUTPUT = Schema.structureBuilder(ShapeId.from("com.amazonaws.cloudwatch#GetOTelEnrichmentOutput"))
             .putMember("Status", OTelEnrichmentStatus.$SCHEMA,
                     new RequiredTrait())
             .builderSupplier(GetOTelEnrichmentOutput::builder)
             .shapeClass(GetOTelEnrichmentOutput.class)
             .build();

    static final Schema LIST_ALARM_MUTE_RULES_INPUT = Schema.structureBuilder(ShapeId.from("com.amazonaws.cloudwatch#ListAlarmMuteRulesInput"))
             .putMember("AlarmName", Schemas.NAME)
             .putMember("Statuses", Schemas.ALARM_MUTE_RULE_STATUSES)
             .putMember("MaxRecords", Schemas.MAX_RECORDS)
             .putMember("NextToken", Schemas.NEXT_TOKEN)
             .builderSupplier(ListAlarmMuteRulesInput::builder)
             .shapeClass(ListAlarmMuteRulesInput.class)
             .build();

    static final Schema LIST_ALARM_MUTE_RULES_OUTPUT = Schema.structureBuilder(ShapeId.from("com.amazonaws.cloudwatch#ListAlarmMuteRulesOutput"))
             .putMember("AlarmMuteRuleSummaries", Schemas.ALARM_MUTE_RULE_SUMMARIES)
             .putMember("NextToken", Schemas.NEXT_TOKEN)
             .builderSupplier(ListAlarmMuteRulesOutput::builder)
             .shapeClass(ListAlarmMuteRulesOutput.class)
             .build();

    static final Schema LIST_DASHBOARDS_INPUT = Schema.structureBuilder(ShapeId.from("com.amazonaws.cloudwatch#ListDashboardsInput"))
             .putMember("DashboardNamePrefix", Schemas.DASHBOARD_NAME_PREFIX)
             .putMember("NextToken", Schemas.NEXT_TOKEN)
             .builderSupplier(ListDashboardsInput::builder)
             .shapeClass(ListDashboardsInput.class)
             .build();

    static final Schema LIST_DASHBOARDS_OUTPUT = Schema.structureBuilder(ShapeId.from("com.amazonaws.cloudwatch#ListDashboardsOutput"))
             .putMember("DashboardEntries", Schemas.DASHBOARD_ENTRIES)
             .putMember("NextToken", Schemas.NEXT_TOKEN)
             .builderSupplier(ListDashboardsOutput::builder)
             .shapeClass(ListDashboardsOutput.class)
             .build();

    static final Schema LIST_MANAGED_INSIGHT_RULES_INPUT = Schema.structureBuilder(ShapeId.from("com.amazonaws.cloudwatch#ListManagedInsightRulesInput"))
             .putMember("ResourceARN", Schemas.AMAZON_RESOURCE_NAME,
                     new RequiredTrait())
             .putMember("NextToken", Schemas.NEXT_TOKEN)
             .putMember("MaxResults", Schemas.INSIGHT_RULE_MAX_RESULTS)
             .builderSupplier(ListManagedInsightRulesInput::builder)
             .shapeClass(ListManagedInsightRulesInput.class)
             .build();

    static final Schema MANAGED_RULE_STATE = Schema.structureBuilder(ShapeId.from("com.amazonaws.cloudwatch#ManagedRuleState"))
             .putMember("RuleName", Schemas.INSIGHT_RULE_NAME,
                     new RequiredTrait())
             .putMember("State", Schemas.INSIGHT_RULE_STATE,
                     new RequiredTrait())
             .builderSupplier(ManagedRuleState::builder)
             .shapeClass(ManagedRuleState.class)
             .build();

    static final Schema TEMPLATE_NAME = Schema.createString(ShapeId.from("com.amazonaws.cloudwatch#TemplateName"),
            LengthTrait.builder().min(1L).max(128L).build(),
            new PatternTrait("^[0-9A-Za-z][\\-\\.\\_0-9A-Za-z]{0,126}[0-9A-Za-z]$"));
    static final Schema MANAGED_RULE_DESCRIPTION = Schema.structureBuilder(ShapeId.from("com.amazonaws.cloudwatch#ManagedRuleDescription"))
             .putMember("TemplateName", Schemas.TEMPLATE_NAME)
             .putMember("ResourceARN", Schemas.AMAZON_RESOURCE_NAME)
             .putMember("RuleState", Schemas.MANAGED_RULE_STATE)
             .builderSupplier(ManagedRuleDescription::builder)
             .shapeClass(ManagedRuleDescription.class)
             .build();

    static final Schema MANAGED_RULE_DESCRIPTIONS = Schema.listBuilder(ShapeId.from("com.amazonaws.cloudwatch#ManagedRuleDescriptions"))
        .putMember("member", Schemas.MANAGED_RULE_DESCRIPTION)
        .build();

    static final Schema LIST_MANAGED_INSIGHT_RULES_OUTPUT = Schema.structureBuilder(ShapeId.from("com.amazonaws.cloudwatch#ListManagedInsightRulesOutput"))
             .putMember("ManagedRules", Schemas.MANAGED_RULE_DESCRIPTIONS)
             .putMember("NextToken", Schemas.NEXT_TOKEN)
             .builderSupplier(ListManagedInsightRulesOutput::builder)
             .shapeClass(ListManagedInsightRulesOutput.class)
             .build();

    static final Schema INCLUDE_LINKED_ACCOUNTS = Schema.createBoolean(ShapeId.from("com.amazonaws.cloudwatch#IncludeLinkedAccounts"));
    static final Schema LIST_METRICS_INPUT = Schema.structureBuilder(ShapeId.from("com.amazonaws.cloudwatch#ListMetricsInput"))
             .putMember("Namespace", Schemas.NAMESPACE)
             .putMember("MetricName", Schemas.METRIC_NAME)
             .putMember("Dimensions", Schemas.DIMENSION_FILTERS)
             .putMember("NextToken", Schemas.NEXT_TOKEN)
             .putMember("RecentlyActive", RecentlyActive.$SCHEMA)
             .putMember("IncludeLinkedAccounts", Schemas.INCLUDE_LINKED_ACCOUNTS)
             .putMember("OwningAccount", Schemas.ACCOUNT_ID)
             .builderSupplier(ListMetricsInput::builder)
             .shapeClass(ListMetricsInput.class)
             .build();

    static final Schema METRICS = Schema.listBuilder(ShapeId.from("com.amazonaws.cloudwatch#Metrics"))
        .putMember("member", Schemas.METRIC)
        .build();

    static final Schema OWNING_ACCOUNTS = Schema.listBuilder(ShapeId.from("com.amazonaws.cloudwatch#OwningAccounts"))
        .putMember("member", Schemas.ACCOUNT_ID)
        .build();

    static final Schema LIST_METRICS_OUTPUT = Schema.structureBuilder(ShapeId.from("com.amazonaws.cloudwatch#ListMetricsOutput"))
             .putMember("Metrics", Schemas.METRICS)
             .putMember("NextToken", Schemas.NEXT_TOKEN)
             .putMember("OwningAccounts", Schemas.OWNING_ACCOUNTS)
             .builderSupplier(ListMetricsOutput::builder)
             .shapeClass(ListMetricsOutput.class)
             .build();

    static final Schema LIST_METRIC_STREAMS_MAX_RESULTS = Schema.createInteger(ShapeId.from("com.amazonaws.cloudwatch#ListMetricStreamsMaxResults"),
            RangeTrait.builder().min(new BigDecimal("1")).max(new BigDecimal("500")).build());
    static final Schema LIST_METRIC_STREAMS_INPUT = Schema.structureBuilder(ShapeId.from("com.amazonaws.cloudwatch#ListMetricStreamsInput"))
             .putMember("NextToken", Schemas.NEXT_TOKEN)
             .putMember("MaxResults", Schemas.LIST_METRIC_STREAMS_MAX_RESULTS)
             .builderSupplier(ListMetricStreamsInput::builder)
             .shapeClass(ListMetricStreamsInput.class)
             .build();

    static final Schema METRIC_STREAM_ENTRY = Schema.structureBuilder(ShapeId.from("com.amazonaws.cloudwatch#MetricStreamEntry"))
             .putMember("Arn", Schemas.AMAZON_RESOURCE_NAME)
             .putMember("CreationDate", Schemas.TIMESTAMP)
             .putMember("LastUpdateDate", Schemas.TIMESTAMP)
             .putMember("Name", Schemas.METRIC_STREAM_NAME)
             .putMember("FirehoseArn", Schemas.AMAZON_RESOURCE_NAME)
             .putMember("State", Schemas.METRIC_STREAM_STATE)
             .putMember("OutputFormat", MetricStreamOutputFormat.$SCHEMA)
             .builderSupplier(MetricStreamEntry::builder)
             .shapeClass(MetricStreamEntry.class)
             .build();

    static final Schema METRIC_STREAM_ENTRIES = Schema.listBuilder(ShapeId.from("com.amazonaws.cloudwatch#MetricStreamEntries"))
        .putMember("member", Schemas.METRIC_STREAM_ENTRY)
        .build();

    static final Schema LIST_METRIC_STREAMS_OUTPUT = Schema.structureBuilder(ShapeId.from("com.amazonaws.cloudwatch#ListMetricStreamsOutput"))
             .putMember("NextToken", Schemas.NEXT_TOKEN)
             .putMember("Entries", Schemas.METRIC_STREAM_ENTRIES)
             .builderSupplier(ListMetricStreamsOutput::builder)
             .shapeClass(ListMetricStreamsOutput.class)
             .build();

    static final Schema LIST_TAGS_FOR_RESOURCE_INPUT = Schema.structureBuilder(ShapeId.from("com.amazonaws.cloudwatch#ListTagsForResourceInput"))
             .putMember("ResourceARN", Schemas.AMAZON_RESOURCE_NAME,
                     new RequiredTrait())
             .builderSupplier(ListTagsForResourceInput::builder)
             .shapeClass(ListTagsForResourceInput.class)
             .build();

    static final Schema LIST_TAGS_FOR_RESOURCE_OUTPUT = Schema.structureBuilder(ShapeId.from("com.amazonaws.cloudwatch#ListTagsForResourceOutput"))
             .putMember("Tags", Schemas.TAG_LIST)
             .builderSupplier(ListTagsForResourceOutput::builder)
             .shapeClass(ListTagsForResourceOutput.class)
             .build();

    static final Schema LIMIT_EXCEEDED_FAULT = Schema.structureBuilder(ShapeId.from("com.amazonaws.cloudwatch#LimitExceededFault"),
            new ErrorTrait("client"),
            new HttpErrorTrait(400),
            new AwsQueryErrorTrait.Provider().createTrait(
                ShapeId.from("aws.protocols#awsQueryError"),
                Node.objectNodeBuilder()
                    .withMember("code", "LimitExceeded")
                    .withMember("httpResponseCode", 400L)
                    .build()
            ))
             .putMember("message", Schemas.ERROR_MESSAGE)
             .builderSupplier(LimitExceededFault::builder)
             .shapeClass(LimitExceededFault.class)
             .build();

    static final Schema PUT_ALARM_MUTE_RULE_INPUT = Schema.structureBuilder(ShapeId.from("com.amazonaws.cloudwatch#PutAlarmMuteRuleInput"))
             .putMember("Name", Schemas.NAME,
                     new RequiredTrait())
             .putMember("Description", Schemas.ALARM_DESCRIPTION)
             .putMember("Rule", Schemas.RULE,
                     new RequiredTrait())
             .putMember("MuteTargets", Schemas.MUTE_TARGETS)
             .putMember("Tags", Schemas.TAG_LIST)
             .putMember("StartDate", Schemas.TIMESTAMP)
             .putMember("ExpireDate", Schemas.TIMESTAMP)
             .builderSupplier(PutAlarmMuteRuleInput::builder)
             .shapeClass(PutAlarmMuteRuleInput.class)
             .build();

    static final Schema PUT_ALARM_MUTE_RULE_OUTPUT = Schema.structureBuilder(ShapeId.from("com.amazonaws.cloudwatch#PutAlarmMuteRuleOutput"),
            new UnitTypeTrait()).builderSupplier(PutAlarmMuteRuleOutput::builder)
             .shapeClass(PutAlarmMuteRuleOutput.class)
             .build();

    static final Schema PUT_ANOMALY_DETECTOR_INPUT = Schema.structureBuilder(ShapeId.from("com.amazonaws.cloudwatch#PutAnomalyDetectorInput"))
             .putMember("Namespace", Schemas.NAMESPACE)
             .putMember("MetricName", Schemas.METRIC_NAME)
             .putMember("Dimensions", Schemas.DIMENSIONS)
             .putMember("Stat", Schemas.ANOMALY_DETECTOR_METRIC_STAT)
             .putMember("Configuration", Schemas.ANOMALY_DETECTOR_CONFIGURATION)
             .putMember("MetricCharacteristics", Schemas.METRIC_CHARACTERISTICS)
             .putMember("SingleMetricAnomalyDetector", Schemas.SINGLE_METRIC_ANOMALY_DETECTOR)
             .putMember("MetricMathAnomalyDetector", Schemas.METRIC_MATH_ANOMALY_DETECTOR)
             .builderSupplier(PutAnomalyDetectorInput::builder)
             .shapeClass(PutAnomalyDetectorInput.class)
             .build();

    static final Schema PUT_ANOMALY_DETECTOR_OUTPUT = Schema.structureBuilder(ShapeId.from("com.amazonaws.cloudwatch#PutAnomalyDetectorOutput"))
             .putMember("AnomalyDetectorId", Schemas.ANOMALY_DETECTOR_ID)
             .builderSupplier(PutAnomalyDetectorOutput::builder)
             .shapeClass(PutAnomalyDetectorOutput.class)
             .build();

    static final Schema PUT_COMPOSITE_ALARM_INPUT = Schema.structureBuilder(ShapeId.from("com.amazonaws.cloudwatch#PutCompositeAlarmInput"))
             .putMember("ActionsEnabled", Schemas.ACTIONS_ENABLED)
             .putMember("AlarmActions", Schemas.RESOURCE_LIST)
             .putMember("AlarmDescription", Schemas.ALARM_DESCRIPTION)
             .putMember("AlarmName", Schemas.ALARM_NAME,
                     new RequiredTrait())
             .putMember("AlarmRule", Schemas.ALARM_RULE,
                     new RequiredTrait())
             .putMember("InsufficientDataActions", Schemas.RESOURCE_LIST)
             .putMember("OKActions", Schemas.RESOURCE_LIST)
             .putMember("Tags", Schemas.TAG_LIST)
             .putMember("ActionsSuppressor", Schemas.ALARM_ARN)
             .putMember("ActionsSuppressorWaitPeriod", Schemas.SUPPRESSOR_PERIOD)
             .putMember("ActionsSuppressorExtensionPeriod", Schemas.SUPPRESSOR_PERIOD)
             .builderSupplier(PutCompositeAlarmInput::builder)
             .shapeClass(PutCompositeAlarmInput.class)
             .build();

    static final Schema PUT_COMPOSITE_ALARM_OUTPUT = Schema.structureBuilder(ShapeId.from("com.amazonaws.cloudwatch#PutCompositeAlarmOutput"),
            new UnitTypeTrait()).builderSupplier(PutCompositeAlarmOutput::builder)
             .shapeClass(PutCompositeAlarmOutput.class)
             .build();

    static final Schema PUT_DASHBOARD_INPUT = Schema.structureBuilder(ShapeId.from("com.amazonaws.cloudwatch#PutDashboardInput"))
             .putMember("DashboardName", Schemas.DASHBOARD_NAME,
                     new RequiredTrait())
             .putMember("DashboardBody", Schemas.DASHBOARD_BODY,
                     new RequiredTrait())
             .putMember("Tags", Schemas.TAG_LIST)
             .builderSupplier(PutDashboardInput::builder)
             .shapeClass(PutDashboardInput.class)
             .build();

    static final Schema PUT_DASHBOARD_OUTPUT = Schema.structureBuilder(ShapeId.from("com.amazonaws.cloudwatch#PutDashboardOutput"))
             .putMember("DashboardValidationMessages", Schemas.DASHBOARD_VALIDATION_MESSAGES)
             .builderSupplier(PutDashboardOutput::builder)
             .shapeClass(PutDashboardOutput.class)
             .build();

    static final Schema PUT_INSIGHT_RULE_INPUT = Schema.structureBuilder(ShapeId.from("com.amazonaws.cloudwatch#PutInsightRuleInput"))
             .putMember("RuleName", Schemas.INSIGHT_RULE_NAME,
                     new RequiredTrait())
             .putMember("RuleState", Schemas.INSIGHT_RULE_STATE)
             .putMember("RuleDefinition", Schemas.INSIGHT_RULE_DEFINITION,
                     new RequiredTrait())
             .putMember("Tags", Schemas.TAG_LIST)
             .putMember("ApplyOnTransformedLogs", Schemas.INSIGHT_RULE_ON_TRANSFORMED_LOGS)
             .builderSupplier(PutInsightRuleInput::builder)
             .shapeClass(PutInsightRuleInput.class)
             .build();

    static final Schema PUT_INSIGHT_RULE_OUTPUT = Schema.structureBuilder(ShapeId.from("com.amazonaws.cloudwatch#PutInsightRuleOutput")).builderSupplier(PutInsightRuleOutput::builder)
             .shapeClass(PutInsightRuleOutput.class)
             .build();

    static final Schema PUT_LOG_ALARM_INPUT = Schema.structureBuilder(ShapeId.from("com.amazonaws.cloudwatch#PutLogAlarmInput"))
             .putMember("AlarmName", Schemas.ALARM_NAME,
                     new RequiredTrait())
             .putMember("AlarmDescription", Schemas.ALARM_DESCRIPTION)
             .putMember("ScheduledQueryConfiguration", Schemas.SCHEDULED_QUERY_CONFIGURATION,
                     new RequiredTrait())
             .putMember("ActionLogLineCount", Schemas.ACTION_LOG_LINE_COUNT)
             .putMember("ActionLogLineRoleArn", Schemas.ACTION_LOG_LINE_ROLE_ARN)
             .putMember("ActionsEnabled", Schemas.ACTIONS_ENABLED)
             .putMember("OKActions", Schemas.RESOURCE_LIST)
             .putMember("AlarmActions", Schemas.RESOURCE_LIST)
             .putMember("InsufficientDataActions", Schemas.RESOURCE_LIST)
             .putMember("QueryResultsToEvaluate", Schemas.QUERY_RESULTS_TO_EVALUATE,
                     new RequiredTrait())
             .putMember("QueryResultsToAlarm", Schemas.QUERY_RESULTS_TO_ALARM,
                     new RequiredTrait())
             .putMember("Threshold", Schemas.THRESHOLD,
                     new RequiredTrait())
             .putMember("ComparisonOperator", ComparisonOperator.$SCHEMA,
                     new RequiredTrait())
             .putMember("TreatMissingData", Schemas.TREAT_MISSING_DATA)
             .putMember("Tags", Schemas.TAG_LIST)
             .putMember("WarmUpConfiguration", Schemas.WARM_UP_CONFIGURATION)
             .builderSupplier(PutLogAlarmInput::builder)
             .shapeClass(PutLogAlarmInput.class)
             .build();

    static final Schema PUT_LOG_ALARM_OUTPUT = Schema.structureBuilder(ShapeId.from("com.amazonaws.cloudwatch#PutLogAlarmOutput"),
            new UnitTypeTrait()).builderSupplier(PutLogAlarmOutput::builder)
             .shapeClass(PutLogAlarmOutput.class)
             .build();

    static final Schema MANAGED_RULE = Schema.structureBuilder(ShapeId.from("com.amazonaws.cloudwatch#ManagedRule"))
             .putMember("TemplateName", Schemas.TEMPLATE_NAME,
                     new RequiredTrait())
             .putMember("ResourceARN", Schemas.AMAZON_RESOURCE_NAME,
                     new RequiredTrait())
             .putMember("Tags", Schemas.TAG_LIST)
             .builderSupplier(ManagedRule::builder)
             .shapeClass(ManagedRule.class)
             .build();

    static final Schema MANAGED_RULES = Schema.listBuilder(ShapeId.from("com.amazonaws.cloudwatch#ManagedRules"))
        .putMember("member", Schemas.MANAGED_RULE)
        .build();

    static final Schema PUT_MANAGED_INSIGHT_RULES_INPUT = Schema.structureBuilder(ShapeId.from("com.amazonaws.cloudwatch#PutManagedInsightRulesInput"))
             .putMember("ManagedRules", Schemas.MANAGED_RULES,
                     new RequiredTrait())
             .builderSupplier(PutManagedInsightRulesInput::builder)
             .shapeClass(PutManagedInsightRulesInput.class)
             .build();

    static final Schema PUT_MANAGED_INSIGHT_RULES_OUTPUT = Schema.structureBuilder(ShapeId.from("com.amazonaws.cloudwatch#PutManagedInsightRulesOutput"))
             .putMember("Failures", Schemas.BATCH_FAILURES)
             .builderSupplier(PutManagedInsightRulesOutput::builder)
             .shapeClass(PutManagedInsightRulesOutput.class)
             .build();

    static final Schema PUT_METRIC_ALARM_INPUT = Schema.structureBuilder(ShapeId.from("com.amazonaws.cloudwatch#PutMetricAlarmInput"))
             .putMember("AlarmName", Schemas.ALARM_NAME,
                     new RequiredTrait())
             .putMember("AlarmDescription", Schemas.ALARM_DESCRIPTION)
             .putMember("ActionsEnabled", Schemas.ACTIONS_ENABLED)
             .putMember("OKActions", Schemas.RESOURCE_LIST)
             .putMember("AlarmActions", Schemas.RESOURCE_LIST)
             .putMember("InsufficientDataActions", Schemas.RESOURCE_LIST)
             .putMember("MetricName", Schemas.METRIC_NAME)
             .putMember("Namespace", Schemas.NAMESPACE)
             .putMember("Statistic", Statistic.$SCHEMA)
             .putMember("ExtendedStatistic", Schemas.EXTENDED_STATISTIC)
             .putMember("Dimensions", Schemas.DIMENSIONS)
             .putMember("Period", Schemas.PERIOD)
             .putMember("Unit", StandardUnit.$SCHEMA)
             .putMember("EvaluationPeriods", Schemas.EVALUATION_PERIODS)
             .putMember("DatapointsToAlarm", Schemas.DATAPOINTS_TO_ALARM)
             .putMember("Threshold", Schemas.THRESHOLD)
             .putMember("ComparisonOperator", ComparisonOperator.$SCHEMA)
             .putMember("TreatMissingData", Schemas.TREAT_MISSING_DATA)
             .putMember("EvaluateLowSampleCountPercentile", Schemas.EVALUATE_LOW_SAMPLE_COUNT_PERCENTILE)
             .putMember("Metrics", Schemas.METRIC_DATA_QUERIES)
             .putMember("Tags", Schemas.TAG_LIST)
             .putMember("ThresholdMetricId", Schemas.METRIC_ID)
             .putMember("EvaluationWindow", Schemas.EVALUATION_WINDOW)
             .putMember("WarmUpConfiguration", Schemas.WARM_UP_CONFIGURATION)
             .putMember("EvaluationCriteria", Schemas.EVALUATION_CRITERIA)
             .putMember("EvaluationInterval", Schemas.EVALUATION_INTERVAL)
             .builderSupplier(PutMetricAlarmInput::builder)
             .shapeClass(PutMetricAlarmInput.class)
             .build();

    static final Schema PUT_METRIC_ALARM_OUTPUT = Schema.structureBuilder(ShapeId.from("com.amazonaws.cloudwatch#PutMetricAlarmOutput"),
            new UnitTypeTrait()).builderSupplier(PutMetricAlarmOutput::builder)
             .shapeClass(PutMetricAlarmOutput.class)
             .build();

    static final Schema STRICT_ENTITY_VALIDATION = Schema.createBoolean(ShapeId.from("com.amazonaws.cloudwatch#StrictEntityValidation"));
    static final Schema PUT_METRIC_DATA_INPUT = Schema.structureBuilder(ShapeId.from("com.amazonaws.cloudwatch#PutMetricDataInput"))
             .putMember("Namespace", Schemas.NAMESPACE,
                     new RequiredTrait())
             .putMember("MetricData", Schemas.METRIC_DATA)
             .putMember("EntityMetricData", Schemas.ENTITY_METRIC_DATA_LIST)
             .putMember("StrictEntityValidation", Schemas.STRICT_ENTITY_VALIDATION)
             .builderSupplier(PutMetricDataInput::builder)
             .shapeClass(PutMetricDataInput.class)
             .build();

    static final Schema PUT_METRIC_DATA_OUTPUT = Schema.structureBuilder(ShapeId.from("com.amazonaws.cloudwatch#PutMetricDataOutput"),
            new UnitTypeTrait()).builderSupplier(PutMetricDataOutput::builder)
             .shapeClass(PutMetricDataOutput.class)
             .build();

    static final Schema PUT_METRIC_STREAM_INPUT = Schema.structureBuilder(ShapeId.from("com.amazonaws.cloudwatch#PutMetricStreamInput"))
             .putMember("Name", Schemas.METRIC_STREAM_NAME,
                     new RequiredTrait())
             .putMember("IncludeFilters", Schemas.METRIC_STREAM_FILTERS)
             .putMember("ExcludeFilters", Schemas.METRIC_STREAM_FILTERS)
             .putMember("FirehoseArn", Schemas.AMAZON_RESOURCE_NAME,
                     new RequiredTrait())
             .putMember("RoleArn", Schemas.AMAZON_RESOURCE_NAME,
                     new RequiredTrait())
             .putMember("OutputFormat", MetricStreamOutputFormat.$SCHEMA,
                     new RequiredTrait())
             .putMember("Tags", Schemas.TAG_LIST)
             .putMember("StatisticsConfigurations", Schemas.METRIC_STREAM_STATISTICS_CONFIGURATIONS)
             .putMember("IncludeLinkedAccountsMetrics", Schemas.INCLUDE_LINKED_ACCOUNTS_METRICS)
             .builderSupplier(PutMetricStreamInput::builder)
             .shapeClass(PutMetricStreamInput.class)
             .build();

    static final Schema PUT_METRIC_STREAM_OUTPUT = Schema.structureBuilder(ShapeId.from("com.amazonaws.cloudwatch#PutMetricStreamOutput"))
             .putMember("Arn", Schemas.AMAZON_RESOURCE_NAME)
             .builderSupplier(PutMetricStreamOutput::builder)
             .shapeClass(PutMetricStreamOutput.class)
             .build();

    static final Schema INVALID_FORMAT_FAULT = Schema.structureBuilder(ShapeId.from("com.amazonaws.cloudwatch#InvalidFormatFault"),
            new ErrorTrait("client"),
            new HttpErrorTrait(400),
            new AwsQueryErrorTrait.Provider().createTrait(
                ShapeId.from("aws.protocols#awsQueryError"),
                Node.objectNodeBuilder()
                    .withMember("code", "InvalidFormat")
                    .withMember("httpResponseCode", 400L)
                    .build()
            ))
             .putMember("message", Schemas.ERROR_MESSAGE)
             .builderSupplier(InvalidFormatFault::builder)
             .shapeClass(InvalidFormatFault.class)
             .build();

    static final Schema SET_ALARM_STATE_INPUT = Schema.structureBuilder(ShapeId.from("com.amazonaws.cloudwatch#SetAlarmStateInput"))
             .putMember("AlarmName", Schemas.ALARM_NAME,
                     new RequiredTrait())
             .putMember("StateValue", StateValue.$SCHEMA,
                     new RequiredTrait())
             .putMember("StateReason", Schemas.STATE_REASON,
                     new RequiredTrait())
             .putMember("StateReasonData", Schemas.STATE_REASON_DATA)
             .builderSupplier(SetAlarmStateInput::builder)
             .shapeClass(SetAlarmStateInput.class)
             .build();

    static final Schema SET_ALARM_STATE_OUTPUT = Schema.structureBuilder(ShapeId.from("com.amazonaws.cloudwatch#SetAlarmStateOutput"),
            new UnitTypeTrait()).builderSupplier(SetAlarmStateOutput::builder)
             .shapeClass(SetAlarmStateOutput.class)
             .build();

    static final Schema METRIC_STREAM_NAMES = Schema.listBuilder(ShapeId.from("com.amazonaws.cloudwatch#MetricStreamNames"))
        .putMember("member", Schemas.METRIC_STREAM_NAME)
        .build();

    static final Schema START_METRIC_STREAMS_INPUT = Schema.structureBuilder(ShapeId.from("com.amazonaws.cloudwatch#StartMetricStreamsInput"))
             .putMember("Names", Schemas.METRIC_STREAM_NAMES,
                     new RequiredTrait())
             .builderSupplier(StartMetricStreamsInput::builder)
             .shapeClass(StartMetricStreamsInput.class)
             .build();

    static final Schema START_METRIC_STREAMS_OUTPUT = Schema.structureBuilder(ShapeId.from("com.amazonaws.cloudwatch#StartMetricStreamsOutput")).builderSupplier(StartMetricStreamsOutput::builder)
             .shapeClass(StartMetricStreamsOutput.class)
             .build();

    static final Schema START_O_TEL_ENRICHMENT_INPUT = Schema.structureBuilder(ShapeId.from("com.amazonaws.cloudwatch#StartOTelEnrichmentInput")).builderSupplier(StartOTelEnrichmentInput::builder)
             .shapeClass(StartOTelEnrichmentInput.class)
             .build();

    static final Schema START_O_TEL_ENRICHMENT_OUTPUT = Schema.structureBuilder(ShapeId.from("com.amazonaws.cloudwatch#StartOTelEnrichmentOutput")).builderSupplier(StartOTelEnrichmentOutput::builder)
             .shapeClass(StartOTelEnrichmentOutput.class)
             .build();

    static final Schema STOP_METRIC_STREAMS_INPUT = Schema.structureBuilder(ShapeId.from("com.amazonaws.cloudwatch#StopMetricStreamsInput"))
             .putMember("Names", Schemas.METRIC_STREAM_NAMES,
                     new RequiredTrait())
             .builderSupplier(StopMetricStreamsInput::builder)
             .shapeClass(StopMetricStreamsInput.class)
             .build();

    static final Schema STOP_METRIC_STREAMS_OUTPUT = Schema.structureBuilder(ShapeId.from("com.amazonaws.cloudwatch#StopMetricStreamsOutput")).builderSupplier(StopMetricStreamsOutput::builder)
             .shapeClass(StopMetricStreamsOutput.class)
             .build();

    static final Schema STOP_O_TEL_ENRICHMENT_INPUT = Schema.structureBuilder(ShapeId.from("com.amazonaws.cloudwatch#StopOTelEnrichmentInput")).builderSupplier(StopOTelEnrichmentInput::builder)
             .shapeClass(StopOTelEnrichmentInput.class)
             .build();

    static final Schema STOP_O_TEL_ENRICHMENT_OUTPUT = Schema.structureBuilder(ShapeId.from("com.amazonaws.cloudwatch#StopOTelEnrichmentOutput")).builderSupplier(StopOTelEnrichmentOutput::builder)
             .shapeClass(StopOTelEnrichmentOutput.class)
             .build();

    static final Schema TAG_RESOURCE_INPUT = Schema.structureBuilder(ShapeId.from("com.amazonaws.cloudwatch#TagResourceInput"))
             .putMember("ResourceARN", Schemas.AMAZON_RESOURCE_NAME,
                     new RequiredTrait())
             .putMember("Tags", Schemas.TAG_LIST,
                     new RequiredTrait())
             .builderSupplier(TagResourceInput::builder)
             .shapeClass(TagResourceInput.class)
             .build();

    static final Schema TAG_RESOURCE_OUTPUT = Schema.structureBuilder(ShapeId.from("com.amazonaws.cloudwatch#TagResourceOutput")).builderSupplier(TagResourceOutput::builder)
             .shapeClass(TagResourceOutput.class)
             .build();

    static final Schema TAG_KEY_LIST = Schema.listBuilder(ShapeId.from("com.amazonaws.cloudwatch#TagKeyList"))
        .putMember("member", Schemas.TAG_KEY)
        .build();

    static final Schema UNTAG_RESOURCE_INPUT = Schema.structureBuilder(ShapeId.from("com.amazonaws.cloudwatch#UntagResourceInput"))
             .putMember("ResourceARN", Schemas.AMAZON_RESOURCE_NAME,
                     new RequiredTrait())
             .putMember("TagKeys", Schemas.TAG_KEY_LIST,
                     new RequiredTrait())
             .builderSupplier(UntagResourceInput::builder)
             .shapeClass(UntagResourceInput.class)
             .build();

    static final Schema UNTAG_RESOURCE_OUTPUT = Schema.structureBuilder(ShapeId.from("com.amazonaws.cloudwatch#UntagResourceOutput")).builderSupplier(UntagResourceOutput::builder)
             .shapeClass(UntagResourceOutput.class)
             .build();

    private Schemas() {}
}
