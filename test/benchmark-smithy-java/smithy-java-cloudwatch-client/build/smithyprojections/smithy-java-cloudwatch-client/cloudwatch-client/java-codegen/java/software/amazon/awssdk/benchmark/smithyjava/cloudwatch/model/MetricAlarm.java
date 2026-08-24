package software.amazon.awssdk.benchmark.smithyjava.cloudwatch.model;

import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import software.amazon.smithy.java.core.schema.Schema;
import software.amazon.smithy.java.core.schema.SchemaUtils;
import software.amazon.smithy.java.core.schema.SerializableStruct;
import software.amazon.smithy.java.core.schema.ShapeBuilder;
import software.amazon.smithy.java.core.serde.ShapeDeserializer;
import software.amazon.smithy.java.core.serde.ShapeSerializer;
import software.amazon.smithy.java.core.serde.ToStringSerializer;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.utils.SmithyGenerated;

/**
 * The details about a metric alarm.
 */
@SmithyGenerated
public final class MetricAlarm implements SerializableStruct {

    public static final Schema $SCHEMA = Schemas.METRIC_ALARM;
    private static final Schema $SCHEMA_ALARM_NAME = $SCHEMA.member("AlarmName");
    private static final Schema $SCHEMA_ALARM_ARN = $SCHEMA.member("AlarmArn");
    private static final Schema $SCHEMA_ALARM_DESCRIPTION = $SCHEMA.member("AlarmDescription");
    private static final Schema $SCHEMA_ALARM_CONFIGURATION_UPDATED_TIMESTAMP = $SCHEMA.member("AlarmConfigurationUpdatedTimestamp");
    private static final Schema $SCHEMA_ACTIONS_ENABLED = $SCHEMA.member("ActionsEnabled");
    private static final Schema $SCHEMA_OK_ACTIONS = $SCHEMA.member("OKActions");
    private static final Schema $SCHEMA_ALARM_ACTIONS = $SCHEMA.member("AlarmActions");
    private static final Schema $SCHEMA_INSUFFICIENT_DATA_ACTIONS = $SCHEMA.member("InsufficientDataActions");
    private static final Schema $SCHEMA_STATE_VALUE = $SCHEMA.member("StateValue");
    private static final Schema $SCHEMA_STATE_REASON = $SCHEMA.member("StateReason");
    private static final Schema $SCHEMA_STATE_REASON_DATA = $SCHEMA.member("StateReasonData");
    private static final Schema $SCHEMA_STATE_UPDATED_TIMESTAMP = $SCHEMA.member("StateUpdatedTimestamp");
    private static final Schema $SCHEMA_METRIC_NAME = $SCHEMA.member("MetricName");
    private static final Schema $SCHEMA_NAMESPACE = $SCHEMA.member("Namespace");
    private static final Schema $SCHEMA_STATISTIC = $SCHEMA.member("Statistic");
    private static final Schema $SCHEMA_EXTENDED_STATISTIC = $SCHEMA.member("ExtendedStatistic");
    private static final Schema $SCHEMA_DIMENSIONS = $SCHEMA.member("Dimensions");
    private static final Schema $SCHEMA_PERIOD = $SCHEMA.member("Period");
    private static final Schema $SCHEMA_UNIT = $SCHEMA.member("Unit");
    private static final Schema $SCHEMA_EVALUATION_PERIODS = $SCHEMA.member("EvaluationPeriods");
    private static final Schema $SCHEMA_DATAPOINTS_TO_ALARM = $SCHEMA.member("DatapointsToAlarm");
    private static final Schema $SCHEMA_THRESHOLD = $SCHEMA.member("Threshold");
    private static final Schema $SCHEMA_COMPARISON_OPERATOR = $SCHEMA.member("ComparisonOperator");
    private static final Schema $SCHEMA_TREAT_MISSING_DATA = $SCHEMA.member("TreatMissingData");
    private static final Schema $SCHEMA_EVALUATE_LOW_SAMPLE_COUNT_PERCENTILE = $SCHEMA.member("EvaluateLowSampleCountPercentile");
    private static final Schema $SCHEMA_METRICS = $SCHEMA.member("Metrics");
    private static final Schema $SCHEMA_THRESHOLD_METRIC_ID = $SCHEMA.member("ThresholdMetricId");
    private static final Schema $SCHEMA_EVALUATION_STATE = $SCHEMA.member("EvaluationState");
    private static final Schema $SCHEMA_STATE_TRANSITIONED_TIMESTAMP = $SCHEMA.member("StateTransitionedTimestamp");
    private static final Schema $SCHEMA_EVALUATION_WINDOW = $SCHEMA.member("EvaluationWindow");
    private static final Schema $SCHEMA_WARM_UP_CONFIGURATION = $SCHEMA.member("WarmUpConfiguration");
    private static final Schema $SCHEMA_EVALUATION_CRITERIA = $SCHEMA.member("EvaluationCriteria");
    private static final Schema $SCHEMA_EVALUATION_INTERVAL = $SCHEMA.member("EvaluationInterval");

    public static final ShapeId $ID = $SCHEMA.id();

    private final transient String alarmName;
    private final transient String alarmArn;
    private final transient String alarmDescription;
    private final transient Instant alarmConfigurationUpdatedTimestamp;
    private final transient Boolean actionsEnabled;
    private final transient List<String> okActions;
    private final transient List<String> alarmActions;
    private final transient List<String> insufficientDataActions;
    private final transient StateValue stateValue;
    private final transient String stateReason;
    private final transient String stateReasonData;
    private final transient Instant stateUpdatedTimestamp;
    private final transient String metricName;
    private final transient String namespace;
    private final transient Statistic statistic;
    private final transient String extendedStatistic;
    private final transient List<Dimension> dimensions;
    private final transient Integer period;
    private final transient StandardUnit unit;
    private final transient Integer evaluationPeriods;
    private final transient Integer datapointsToAlarm;
    private final transient Double threshold;
    private final transient ComparisonOperator comparisonOperator;
    private final transient String treatMissingData;
    private final transient String evaluateLowSampleCountPercentile;
    private final transient List<MetricDataQuery> metrics;
    private final transient String thresholdMetricId;
    private final transient EvaluationState evaluationState;
    private final transient Instant stateTransitionedTimestamp;
    private final transient EvaluationWindow evaluationWindow;
    private final transient WarmUpConfiguration warmUpConfiguration;
    private final transient EvaluationCriteria evaluationCriteria;
    private final transient Integer evaluationInterval;

    private MetricAlarm(Builder builder) {
        this.alarmName = builder.alarmName;
        this.alarmArn = builder.alarmArn;
        this.alarmDescription = builder.alarmDescription;
        this.alarmConfigurationUpdatedTimestamp = builder.alarmConfigurationUpdatedTimestamp;
        this.actionsEnabled = builder.actionsEnabled;
        this.okActions = builder.okActions == null ? null : Collections.unmodifiableList(builder.okActions);
        this.alarmActions = builder.alarmActions == null ? null : Collections.unmodifiableList(builder.alarmActions);
        this.insufficientDataActions = builder.insufficientDataActions == null ? null : Collections.unmodifiableList(builder.insufficientDataActions);
        this.stateValue = builder.stateValue;
        this.stateReason = builder.stateReason;
        this.stateReasonData = builder.stateReasonData;
        this.stateUpdatedTimestamp = builder.stateUpdatedTimestamp;
        this.metricName = builder.metricName;
        this.namespace = builder.namespace;
        this.statistic = builder.statistic;
        this.extendedStatistic = builder.extendedStatistic;
        this.dimensions = builder.dimensions == null ? null : Collections.unmodifiableList(builder.dimensions);
        this.period = builder.period;
        this.unit = builder.unit;
        this.evaluationPeriods = builder.evaluationPeriods;
        this.datapointsToAlarm = builder.datapointsToAlarm;
        this.threshold = builder.threshold;
        this.comparisonOperator = builder.comparisonOperator;
        this.treatMissingData = builder.treatMissingData;
        this.evaluateLowSampleCountPercentile = builder.evaluateLowSampleCountPercentile;
        this.metrics = builder.metrics == null ? null : Collections.unmodifiableList(builder.metrics);
        this.thresholdMetricId = builder.thresholdMetricId;
        this.evaluationState = builder.evaluationState;
        this.stateTransitionedTimestamp = builder.stateTransitionedTimestamp;
        this.evaluationWindow = builder.evaluationWindow;
        this.warmUpConfiguration = builder.warmUpConfiguration;
        this.evaluationCriteria = builder.evaluationCriteria;
        this.evaluationInterval = builder.evaluationInterval;
    }

    /**
     * The name of the alarm.
     */
    public String getAlarmName() {
        return alarmName;
    }

    /**
     * The Amazon Resource Name (ARN) of the alarm.
     */
    public String getAlarmArn() {
        return alarmArn;
    }

    /**
     * The description of the alarm.
     */
    public String getAlarmDescription() {
        return alarmDescription;
    }

    /**
     * The time stamp of the last update to the alarm configuration.
     */
    public Instant getAlarmConfigurationUpdatedTimestamp() {
        return alarmConfigurationUpdatedTimestamp;
    }

    /**
     * Indicates whether actions should be executed during any changes to the alarm state.
     */
    public Boolean isActionsEnabled() {
        return actionsEnabled;
    }

    /**
     * The actions to execute when this alarm transitions to the <code>OK</code> state from any other state. Each action
     * is specified as an Amazon Resource Name (ARN).
     */
    public List<String> getOkActions() {
        if (okActions == null) {
            return Collections.emptyList();
        }
        return okActions;
    }

    public boolean hasOkActions() {
        return okActions != null;
    }

    /**
     * The actions to execute when this alarm transitions to the <code>ALARM</code> state from any other state. Each
     * action is specified as an Amazon Resource Name (ARN).
     */
    public List<String> getAlarmActions() {
        if (alarmActions == null) {
            return Collections.emptyList();
        }
        return alarmActions;
    }

    public boolean hasAlarmActions() {
        return alarmActions != null;
    }

    /**
     * The actions to execute when this alarm transitions to the <code>INSUFFICIENT_DATA</code> state from any other
     * state. Each action is specified as an Amazon Resource Name (ARN).
     */
    public List<String> getInsufficientDataActions() {
        if (insufficientDataActions == null) {
            return Collections.emptyList();
        }
        return insufficientDataActions;
    }

    public boolean hasInsufficientDataActions() {
        return insufficientDataActions != null;
    }

    /**
     * The state value for the alarm.
     */
    public StateValue getStateValue() {
        return stateValue;
    }

    /**
     * An explanation for the alarm state, in text format.
     */
    public String getStateReason() {
        return stateReason;
    }

    /**
     * An explanation for the alarm state, in JSON format.
     */
    public String getStateReasonData() {
        return stateReasonData;
    }

    /**
     * The time stamp of the last update to the value of either the <code>StateValue</code> or
     * <code>EvaluationState</code> parameters.
     */
    public Instant getStateUpdatedTimestamp() {
        return stateUpdatedTimestamp;
    }

    /**
     * The name of the metric associated with the alarm, if this is an alarm based on a single metric.
     */
    public String getMetricName() {
        return metricName;
    }

    /**
     * The namespace of the metric associated with the alarm.
     */
    public String getNamespace() {
        return namespace;
    }

    /**
     * The statistic for the metric associated with the alarm, other than percentile. For percentile statistics, use
     * <code>ExtendedStatistic</code>.
     */
    public Statistic getStatistic() {
        return statistic;
    }

    /**
     * The percentile statistic for the metric associated with the alarm. Specify a value between p0.0 and p100.
     */
    public String getExtendedStatistic() {
        return extendedStatistic;
    }

    /**
     * The dimensions for the metric associated with the alarm.
     */
    public List<Dimension> getDimensions() {
        if (dimensions == null) {
            return Collections.emptyList();
        }
        return dimensions;
    }

    public boolean hasDimensions() {
        return dimensions != null;
    }

    /**
     * The period, in seconds, over which the statistic is applied.
     */
    public Integer getPeriod() {
        return period;
    }

    /**
     * The unit of the metric associated with the alarm.
     */
    public StandardUnit getUnit() {
        return unit;
    }

    /**
     * The number of periods over which data is compared to the specified threshold.
     */
    public Integer getEvaluationPeriods() {
        return evaluationPeriods;
    }

    /**
     * The number of data points that must be breaching to trigger the alarm.
     */
    public Integer getDatapointsToAlarm() {
        return datapointsToAlarm;
    }

    /**
     * The value to compare with the specified statistic.
     */
    public Double getThreshold() {
        return threshold;
    }

    /**
     * The arithmetic operation to use when comparing the specified statistic and threshold. The specified statistic
     * value is used as the first operand.
     */
    public ComparisonOperator getComparisonOperator() {
        return comparisonOperator;
    }

    /**
     * Sets how this alarm is to handle missing data points. The valid values are <code>breaching</code>, <code>
     * notBreaching</code>, <code>ignore</code>, and <code>missing</code>. For more information, see <a href="https://docs.aws.amazon.com/AmazonCloudWatch/latest/monitoring/AlarmThatSendsEmail.html#alarms-and-missing-data">Configuring how
     * CloudWatch alarms treat missing data</a>.
     *
     * <p>If this parameter is omitted, the default behavior of <code>missing</code> is used.
     *
     * <p>This parameter is not applicable to PromQL alarms.
     */
    public String getTreatMissingData() {
        return treatMissingData;
    }

    /**
     * Used only for alarms based on percentiles. If <code>ignore</code>, the alarm state does not change during periods
     * with too few data points to be statistically significant. If <code>evaluate</code> or this parameter is not used,
     * the alarm is always evaluated and possibly changes state no matter how many data points are available.
     */
    public String getEvaluateLowSampleCountPercentile() {
        return evaluateLowSampleCountPercentile;
    }

    /**
     * An array of MetricDataQuery structures, used in an alarm based on a metric math expression. Each structure either
     * retrieves a metric or performs a math expression. One item in the Metrics array is the math expression that the
     * alarm watches. This expression by designated by having <code>ReturnData</code> set to true.
     */
    public List<MetricDataQuery> getMetrics() {
        if (metrics == null) {
            return Collections.emptyList();
        }
        return metrics;
    }

    public boolean hasMetrics() {
        return metrics != null;
    }

    /**
     * In an alarm based on an anomaly detection model, this is the ID of the <code>ANOMALY_DETECTION_BAND</code>
     * function used as the threshold for the alarm.
     */
    public String getThresholdMetricId() {
        return thresholdMetricId;
    }

    /**
     * If the value of this field is <code>PARTIAL_DATA</code>, it indicates that not all the available data was able to
     * be retrieved due to quota limitations. For more information, see <a href="https://docs.aws.amazon.com/AmazonCloudWatch/latest/monitoring/Create_Metrics_Insights_Alarm.html">Create alarms on Metrics Insights queries</a>
     * .
     *
     * <p>If the value of this field is <code>EVALUATION_ERROR</code>, it indicates configuration errors in alarm setup
     * that require review and correction. Refer to StateReason field of the alarm for more details.
     *
     * <p>If the value of this field is <code>EVALUATION_FAILURE</code>, it indicates temporary CloudWatch issues. We
     * recommend manual monitoring until the issue is resolved
     */
    public EvaluationState getEvaluationState() {
        return evaluationState;
    }

    /**
     * The date and time that the alarm's <code>StateValue</code> most recently changed.
     */
    public Instant getStateTransitionedTimestamp() {
        return stateTransitionedTimestamp;
    }

    /**
     * The evaluation window that the alarm uses to select the range of metric data that it evaluates. This is either a
     * sliding window or a wall clock window. For more information, see <a href="https://docs.aws.amazon.com/AmazonCloudWatch/latest/monitoring/alarm-evaluation-window.html">Alarm evaluation windows</a> in the <i>
     * CloudWatch User Guide</i>.
     */
    public EvaluationWindow getEvaluationWindow() {
        return evaluationWindow;
    }

    /**
     * The warm-up configuration for the alarm. A warm-up period delays alarm evaluation after you create or update the
     * alarm. During the warm-up period, the alarm stays in <code>INSUFFICIENT_DATA</code> and does not perform alarm
     * actions.
     *
     * <p>For more information, see <a href="https://docs.aws.amazon.com/AmazonCloudWatch/latest/monitoring/alarm-warm-up.html">Alarm warm-up periods</a> in the <i>Amazon CloudWatch User Guide</i>.
     */
    public WarmUpConfiguration getWarmUpConfiguration() {
        return warmUpConfiguration;
    }

    /**
     * The evaluation criteria for the alarm.
     */
    public EvaluationCriteria getEvaluationCriteria() {
        return evaluationCriteria;
    }

    /**
     * The frequency, in seconds, at which the alarm is evaluated.
     */
    public Integer getEvaluationInterval() {
        return evaluationInterval;
    }

    @Override
    public String toString() {
        return ToStringSerializer.serialize(this);
    }

    @Override
    public boolean equals(Object other) {
        if (other == this) {
            return true;
        }
        if (other == null || getClass() != other.getClass()) {
            return false;
        }
        MetricAlarm that = (MetricAlarm) other;
        return Objects.equals(this.actionsEnabled, that.actionsEnabled)
               && Objects.equals(this.period, that.period)
               && Objects.equals(this.evaluationPeriods, that.evaluationPeriods)
               && Objects.equals(this.datapointsToAlarm, that.datapointsToAlarm)
               && Objects.equals(this.evaluationInterval, that.evaluationInterval)
               && Objects.equals(this.threshold, that.threshold)
               && Objects.equals(this.alarmName, that.alarmName)
               && Objects.equals(this.alarmArn, that.alarmArn)
               && Objects.equals(this.alarmDescription, that.alarmDescription)
               && Objects.equals(this.stateReason, that.stateReason)
               && Objects.equals(this.stateReasonData, that.stateReasonData)
               && Objects.equals(this.metricName, that.metricName)
               && Objects.equals(this.namespace, that.namespace)
               && Objects.equals(this.extendedStatistic, that.extendedStatistic)
               && Objects.equals(this.treatMissingData, that.treatMissingData)
               && Objects.equals(this.evaluateLowSampleCountPercentile, that.evaluateLowSampleCountPercentile)
               && Objects.equals(this.thresholdMetricId, that.thresholdMetricId)
               && Objects.equals(this.stateValue, that.stateValue)
               && Objects.equals(this.statistic, that.statistic)
               && Objects.equals(this.unit, that.unit)
               && Objects.equals(this.comparisonOperator, that.comparisonOperator)
               && Objects.equals(this.evaluationState, that.evaluationState)
               && Objects.equals(this.alarmConfigurationUpdatedTimestamp, that.alarmConfigurationUpdatedTimestamp)
               && Objects.equals(this.stateUpdatedTimestamp, that.stateUpdatedTimestamp)
               && Objects.equals(this.stateTransitionedTimestamp, that.stateTransitionedTimestamp)
               && Objects.equals(this.evaluationWindow, that.evaluationWindow)
               && Objects.equals(this.evaluationCriteria, that.evaluationCriteria)
               && Objects.equals(this.warmUpConfiguration, that.warmUpConfiguration)
               && Objects.equals(this.okActions, that.okActions)
               && Objects.equals(this.alarmActions, that.alarmActions)
               && Objects.equals(this.insufficientDataActions, that.insufficientDataActions)
               && Objects.equals(this.dimensions, that.dimensions)
               && Objects.equals(this.metrics, that.metrics);
    }

    @Override
    public int hashCode() {
        int $hc = Objects.hashCode(alarmName);
        $hc = 31 * $hc + Objects.hashCode(alarmArn);
        $hc = 31 * $hc + Objects.hashCode(alarmDescription);
        $hc = 31 * $hc + Objects.hashCode(alarmConfigurationUpdatedTimestamp);
        $hc = 31 * $hc + Objects.hashCode(actionsEnabled);
        $hc = 31 * $hc + Objects.hashCode(okActions);
        $hc = 31 * $hc + Objects.hashCode(alarmActions);
        $hc = 31 * $hc + Objects.hashCode(insufficientDataActions);
        $hc = 31 * $hc + Objects.hashCode(stateValue);
        $hc = 31 * $hc + Objects.hashCode(stateReason);
        $hc = 31 * $hc + Objects.hashCode(stateReasonData);
        $hc = 31 * $hc + Objects.hashCode(stateUpdatedTimestamp);
        $hc = 31 * $hc + Objects.hashCode(metricName);
        $hc = 31 * $hc + Objects.hashCode(namespace);
        $hc = 31 * $hc + Objects.hashCode(statistic);
        $hc = 31 * $hc + Objects.hashCode(extendedStatistic);
        $hc = 31 * $hc + Objects.hashCode(dimensions);
        $hc = 31 * $hc + Objects.hashCode(period);
        $hc = 31 * $hc + Objects.hashCode(unit);
        $hc = 31 * $hc + Objects.hashCode(evaluationPeriods);
        $hc = 31 * $hc + Objects.hashCode(datapointsToAlarm);
        $hc = 31 * $hc + Objects.hashCode(threshold);
        $hc = 31 * $hc + Objects.hashCode(comparisonOperator);
        $hc = 31 * $hc + Objects.hashCode(treatMissingData);
        $hc = 31 * $hc + Objects.hashCode(evaluateLowSampleCountPercentile);
        $hc = 31 * $hc + Objects.hashCode(metrics);
        $hc = 31 * $hc + Objects.hashCode(thresholdMetricId);
        $hc = 31 * $hc + Objects.hashCode(evaluationState);
        $hc = 31 * $hc + Objects.hashCode(stateTransitionedTimestamp);
        $hc = 31 * $hc + Objects.hashCode(evaluationWindow);
        $hc = 31 * $hc + Objects.hashCode(warmUpConfiguration);
        $hc = 31 * $hc + Objects.hashCode(evaluationCriteria);
        $hc = 31 * $hc + Objects.hashCode(evaluationInterval);
        return $hc;
    }

    @Override
    public Schema schema() {
        return $SCHEMA;
    }

    @Override
    public void serializeMembers(ShapeSerializer serializer) {
        if (alarmName != null) {
            serializer.writeString($SCHEMA_ALARM_NAME, alarmName);
        }
        if (alarmArn != null) {
            serializer.writeString($SCHEMA_ALARM_ARN, alarmArn);
        }
        if (alarmDescription != null) {
            serializer.writeString($SCHEMA_ALARM_DESCRIPTION, alarmDescription);
        }
        if (alarmConfigurationUpdatedTimestamp != null) {
            serializer.writeTimestamp($SCHEMA_ALARM_CONFIGURATION_UPDATED_TIMESTAMP, alarmConfigurationUpdatedTimestamp);
        }
        if (actionsEnabled != null) {
            serializer.writeBoolean($SCHEMA_ACTIONS_ENABLED, actionsEnabled);
        }
        if (okActions != null) {
            serializer.writeList($SCHEMA_OK_ACTIONS, okActions, okActions.size(), SharedSerde.ResourceListSerializer.INSTANCE);
        }
        if (alarmActions != null) {
            serializer.writeList($SCHEMA_ALARM_ACTIONS, alarmActions, alarmActions.size(), SharedSerde.ResourceListSerializer.INSTANCE);
        }
        if (insufficientDataActions != null) {
            serializer.writeList($SCHEMA_INSUFFICIENT_DATA_ACTIONS, insufficientDataActions, insufficientDataActions.size(), SharedSerde.ResourceListSerializer.INSTANCE);
        }
        if (stateValue != null) {
            serializer.writeString($SCHEMA_STATE_VALUE, stateValue.getValue());
        }
        if (stateReason != null) {
            serializer.writeString($SCHEMA_STATE_REASON, stateReason);
        }
        if (stateReasonData != null) {
            serializer.writeString($SCHEMA_STATE_REASON_DATA, stateReasonData);
        }
        if (stateUpdatedTimestamp != null) {
            serializer.writeTimestamp($SCHEMA_STATE_UPDATED_TIMESTAMP, stateUpdatedTimestamp);
        }
        if (metricName != null) {
            serializer.writeString($SCHEMA_METRIC_NAME, metricName);
        }
        if (namespace != null) {
            serializer.writeString($SCHEMA_NAMESPACE, namespace);
        }
        if (statistic != null) {
            serializer.writeString($SCHEMA_STATISTIC, statistic.getValue());
        }
        if (extendedStatistic != null) {
            serializer.writeString($SCHEMA_EXTENDED_STATISTIC, extendedStatistic);
        }
        if (dimensions != null) {
            serializer.writeList($SCHEMA_DIMENSIONS, dimensions, dimensions.size(), SharedSerde.DimensionsSerializer.INSTANCE);
        }
        if (period != null) {
            serializer.writeInteger($SCHEMA_PERIOD, period);
        }
        if (unit != null) {
            serializer.writeString($SCHEMA_UNIT, unit.getValue());
        }
        if (evaluationPeriods != null) {
            serializer.writeInteger($SCHEMA_EVALUATION_PERIODS, evaluationPeriods);
        }
        if (datapointsToAlarm != null) {
            serializer.writeInteger($SCHEMA_DATAPOINTS_TO_ALARM, datapointsToAlarm);
        }
        if (threshold != null) {
            serializer.writeDouble($SCHEMA_THRESHOLD, threshold);
        }
        if (comparisonOperator != null) {
            serializer.writeString($SCHEMA_COMPARISON_OPERATOR, comparisonOperator.getValue());
        }
        if (treatMissingData != null) {
            serializer.writeString($SCHEMA_TREAT_MISSING_DATA, treatMissingData);
        }
        if (evaluateLowSampleCountPercentile != null) {
            serializer.writeString($SCHEMA_EVALUATE_LOW_SAMPLE_COUNT_PERCENTILE, evaluateLowSampleCountPercentile);
        }
        if (metrics != null) {
            serializer.writeList($SCHEMA_METRICS, metrics, metrics.size(), SharedSerde.MetricDataQueriesSerializer.INSTANCE);
        }
        if (thresholdMetricId != null) {
            serializer.writeString($SCHEMA_THRESHOLD_METRIC_ID, thresholdMetricId);
        }
        if (evaluationState != null) {
            serializer.writeString($SCHEMA_EVALUATION_STATE, evaluationState.getValue());
        }
        if (stateTransitionedTimestamp != null) {
            serializer.writeTimestamp($SCHEMA_STATE_TRANSITIONED_TIMESTAMP, stateTransitionedTimestamp);
        }
        if (evaluationWindow != null) {
            serializer.writeStruct($SCHEMA_EVALUATION_WINDOW, evaluationWindow);
        }
        if (warmUpConfiguration != null) {
            serializer.writeStruct($SCHEMA_WARM_UP_CONFIGURATION, warmUpConfiguration);
        }
        if (evaluationCriteria != null) {
            serializer.writeStruct($SCHEMA_EVALUATION_CRITERIA, evaluationCriteria);
        }
        if (evaluationInterval != null) {
            serializer.writeInteger($SCHEMA_EVALUATION_INTERVAL, evaluationInterval);
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T getMemberValue(Schema member) {
        return switch (member.memberIndex()) {
            case 0 -> (T) SchemaUtils.validateSameMember($SCHEMA_ALARM_NAME, member, alarmName);
            case 1 -> (T) SchemaUtils.validateSameMember($SCHEMA_ALARM_ARN, member, alarmArn);
            case 2 -> (T) SchemaUtils.validateSameMember($SCHEMA_ALARM_DESCRIPTION, member, alarmDescription);
            case 3 -> (T) SchemaUtils.validateSameMember($SCHEMA_ALARM_CONFIGURATION_UPDATED_TIMESTAMP, member, alarmConfigurationUpdatedTimestamp);
            case 4 -> (T) SchemaUtils.validateSameMember($SCHEMA_ACTIONS_ENABLED, member, actionsEnabled);
            case 5 -> (T) SchemaUtils.validateSameMember($SCHEMA_OK_ACTIONS, member, okActions);
            case 6 -> (T) SchemaUtils.validateSameMember($SCHEMA_ALARM_ACTIONS, member, alarmActions);
            case 7 -> (T) SchemaUtils.validateSameMember($SCHEMA_INSUFFICIENT_DATA_ACTIONS, member, insufficientDataActions);
            case 8 -> (T) SchemaUtils.validateSameMember($SCHEMA_STATE_VALUE, member, stateValue);
            case 9 -> (T) SchemaUtils.validateSameMember($SCHEMA_STATE_REASON, member, stateReason);
            case 10 -> (T) SchemaUtils.validateSameMember($SCHEMA_STATE_REASON_DATA, member, stateReasonData);
            case 11 -> (T) SchemaUtils.validateSameMember($SCHEMA_STATE_UPDATED_TIMESTAMP, member, stateUpdatedTimestamp);
            case 12 -> (T) SchemaUtils.validateSameMember($SCHEMA_METRIC_NAME, member, metricName);
            case 13 -> (T) SchemaUtils.validateSameMember($SCHEMA_NAMESPACE, member, namespace);
            case 14 -> (T) SchemaUtils.validateSameMember($SCHEMA_STATISTIC, member, statistic);
            case 15 -> (T) SchemaUtils.validateSameMember($SCHEMA_EXTENDED_STATISTIC, member, extendedStatistic);
            case 16 -> (T) SchemaUtils.validateSameMember($SCHEMA_DIMENSIONS, member, dimensions);
            case 17 -> (T) SchemaUtils.validateSameMember($SCHEMA_PERIOD, member, period);
            case 18 -> (T) SchemaUtils.validateSameMember($SCHEMA_UNIT, member, unit);
            case 19 -> (T) SchemaUtils.validateSameMember($SCHEMA_EVALUATION_PERIODS, member, evaluationPeriods);
            case 20 -> (T) SchemaUtils.validateSameMember($SCHEMA_DATAPOINTS_TO_ALARM, member, datapointsToAlarm);
            case 21 -> (T) SchemaUtils.validateSameMember($SCHEMA_THRESHOLD, member, threshold);
            case 22 -> (T) SchemaUtils.validateSameMember($SCHEMA_COMPARISON_OPERATOR, member, comparisonOperator);
            case 23 -> (T) SchemaUtils.validateSameMember($SCHEMA_TREAT_MISSING_DATA, member, treatMissingData);
            case 24 -> (T) SchemaUtils.validateSameMember($SCHEMA_EVALUATE_LOW_SAMPLE_COUNT_PERCENTILE, member, evaluateLowSampleCountPercentile);
            case 25 -> (T) SchemaUtils.validateSameMember($SCHEMA_METRICS, member, metrics);
            case 26 -> (T) SchemaUtils.validateSameMember($SCHEMA_THRESHOLD_METRIC_ID, member, thresholdMetricId);
            case 27 -> (T) SchemaUtils.validateSameMember($SCHEMA_EVALUATION_STATE, member, evaluationState);
            case 28 -> (T) SchemaUtils.validateSameMember($SCHEMA_STATE_TRANSITIONED_TIMESTAMP, member, stateTransitionedTimestamp);
            case 29 -> (T) SchemaUtils.validateSameMember($SCHEMA_EVALUATION_WINDOW, member, evaluationWindow);
            case 30 -> (T) SchemaUtils.validateSameMember($SCHEMA_WARM_UP_CONFIGURATION, member, warmUpConfiguration);
            case 31 -> (T) SchemaUtils.validateSameMember($SCHEMA_EVALUATION_CRITERIA, member, evaluationCriteria);
            case 32 -> (T) SchemaUtils.validateSameMember($SCHEMA_EVALUATION_INTERVAL, member, evaluationInterval);
            default -> throw new IllegalArgumentException("Attempted to get non-existent member: " + member.id());
        };
    }

    /**
     * Create a new builder containing all the current property values of this object.
     *
     * <p><strong>Note:</strong> This method performs only a shallow copy of the original properties.
     *
     * @return a builder for {@link MetricAlarm}.
     */
    public Builder toBuilder() {
        var builder = new Builder();
        builder.alarmName(this.alarmName);
        builder.alarmArn(this.alarmArn);
        builder.alarmDescription(this.alarmDescription);
        builder.alarmConfigurationUpdatedTimestamp(this.alarmConfigurationUpdatedTimestamp);
        builder.actionsEnabled(this.actionsEnabled);
        builder.okActions(this.okActions);
        builder.alarmActions(this.alarmActions);
        builder.insufficientDataActions(this.insufficientDataActions);
        builder.stateValue(this.stateValue);
        builder.stateReason(this.stateReason);
        builder.stateReasonData(this.stateReasonData);
        builder.stateUpdatedTimestamp(this.stateUpdatedTimestamp);
        builder.metricName(this.metricName);
        builder.namespace(this.namespace);
        builder.statistic(this.statistic);
        builder.extendedStatistic(this.extendedStatistic);
        builder.dimensions(this.dimensions);
        builder.period(this.period);
        builder.unit(this.unit);
        builder.evaluationPeriods(this.evaluationPeriods);
        builder.datapointsToAlarm(this.datapointsToAlarm);
        builder.threshold(this.threshold);
        builder.comparisonOperator(this.comparisonOperator);
        builder.treatMissingData(this.treatMissingData);
        builder.evaluateLowSampleCountPercentile(this.evaluateLowSampleCountPercentile);
        builder.metrics(this.metrics);
        builder.thresholdMetricId(this.thresholdMetricId);
        builder.evaluationState(this.evaluationState);
        builder.stateTransitionedTimestamp(this.stateTransitionedTimestamp);
        builder.evaluationWindow(this.evaluationWindow);
        builder.warmUpConfiguration(this.warmUpConfiguration);
        builder.evaluationCriteria(this.evaluationCriteria);
        builder.evaluationInterval(this.evaluationInterval);
        return builder;
    }

    /**
     * @return returns a new Builder.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for {@link MetricAlarm}.
     */
    public static final class Builder implements ShapeBuilder<MetricAlarm> {
        private String alarmName;
        private String alarmArn;
        private String alarmDescription;
        private Instant alarmConfigurationUpdatedTimestamp;
        private Boolean actionsEnabled;
        private List<String> okActions;
        private List<String> alarmActions;
        private List<String> insufficientDataActions;
        private StateValue stateValue;
        private String stateReason;
        private String stateReasonData;
        private Instant stateUpdatedTimestamp;
        private String metricName;
        private String namespace;
        private Statistic statistic;
        private String extendedStatistic;
        private List<Dimension> dimensions;
        private Integer period;
        private StandardUnit unit;
        private Integer evaluationPeriods;
        private Integer datapointsToAlarm;
        private Double threshold;
        private ComparisonOperator comparisonOperator;
        private String treatMissingData;
        private String evaluateLowSampleCountPercentile;
        private List<MetricDataQuery> metrics;
        private String thresholdMetricId;
        private EvaluationState evaluationState;
        private Instant stateTransitionedTimestamp;
        private EvaluationWindow evaluationWindow;
        private WarmUpConfiguration warmUpConfiguration;
        private EvaluationCriteria evaluationCriteria;
        private Integer evaluationInterval;

        private Builder() {}

        @Override
        public Schema schema() {
            return $SCHEMA;
        }

        /**
         * The name of the alarm.
         *
         * @return this builder.
         */
        public Builder alarmName(String alarmName) {
            this.alarmName = alarmName;
            return this;
        }

        /**
         * The Amazon Resource Name (ARN) of the alarm.
         *
         * @return this builder.
         */
        public Builder alarmArn(String alarmArn) {
            this.alarmArn = alarmArn;
            return this;
        }

        /**
         * The description of the alarm.
         *
         * @return this builder.
         */
        public Builder alarmDescription(String alarmDescription) {
            this.alarmDescription = alarmDescription;
            return this;
        }

        /**
         * The time stamp of the last update to the alarm configuration.
         *
         * @return this builder.
         */
        public Builder alarmConfigurationUpdatedTimestamp(Instant alarmConfigurationUpdatedTimestamp) {
            this.alarmConfigurationUpdatedTimestamp = alarmConfigurationUpdatedTimestamp;
            return this;
        }

        /**
         * Indicates whether actions should be executed during any changes to the alarm state.
         *
         * @return this builder.
         */
        public Builder actionsEnabled(Boolean actionsEnabled) {
            this.actionsEnabled = actionsEnabled;
            return this;
        }

        /**
         * The actions to execute when this alarm transitions to the <code>OK</code> state from any other state. Each action
         * is specified as an Amazon Resource Name (ARN).
         *
         * @return this builder.
         */
        public Builder okActions(List<String> okActions) {
            this.okActions = okActions;
            return this;
        }

        /**
         * The actions to execute when this alarm transitions to the <code>ALARM</code> state from any other state. Each
         * action is specified as an Amazon Resource Name (ARN).
         *
         * @return this builder.
         */
        public Builder alarmActions(List<String> alarmActions) {
            this.alarmActions = alarmActions;
            return this;
        }

        /**
         * The actions to execute when this alarm transitions to the <code>INSUFFICIENT_DATA</code> state from any other
         * state. Each action is specified as an Amazon Resource Name (ARN).
         *
         * @return this builder.
         */
        public Builder insufficientDataActions(List<String> insufficientDataActions) {
            this.insufficientDataActions = insufficientDataActions;
            return this;
        }

        /**
         * The state value for the alarm.
         *
         * @return this builder.
         */
        public Builder stateValue(StateValue stateValue) {
            this.stateValue = stateValue;
            return this;
        }

        /**
         * An explanation for the alarm state, in text format.
         *
         * @return this builder.
         */
        public Builder stateReason(String stateReason) {
            this.stateReason = stateReason;
            return this;
        }

        /**
         * An explanation for the alarm state, in JSON format.
         *
         * @return this builder.
         */
        public Builder stateReasonData(String stateReasonData) {
            this.stateReasonData = stateReasonData;
            return this;
        }

        /**
         * The time stamp of the last update to the value of either the <code>StateValue</code> or
         * <code>EvaluationState</code> parameters.
         *
         * @return this builder.
         */
        public Builder stateUpdatedTimestamp(Instant stateUpdatedTimestamp) {
            this.stateUpdatedTimestamp = stateUpdatedTimestamp;
            return this;
        }

        /**
         * The name of the metric associated with the alarm, if this is an alarm based on a single metric.
         *
         * @return this builder.
         */
        public Builder metricName(String metricName) {
            this.metricName = metricName;
            return this;
        }

        /**
         * The namespace of the metric associated with the alarm.
         *
         * @return this builder.
         */
        public Builder namespace(String namespace) {
            this.namespace = namespace;
            return this;
        }

        /**
         * The statistic for the metric associated with the alarm, other than percentile. For percentile statistics, use
         * <code>ExtendedStatistic</code>.
         *
         * @return this builder.
         */
        public Builder statistic(Statistic statistic) {
            this.statistic = statistic;
            return this;
        }

        /**
         * The percentile statistic for the metric associated with the alarm. Specify a value between p0.0 and p100.
         *
         * @return this builder.
         */
        public Builder extendedStatistic(String extendedStatistic) {
            this.extendedStatistic = extendedStatistic;
            return this;
        }

        /**
         * The dimensions for the metric associated with the alarm.
         *
         * @return this builder.
         */
        public Builder dimensions(List<Dimension> dimensions) {
            this.dimensions = dimensions;
            return this;
        }

        /**
         * The period, in seconds, over which the statistic is applied.
         *
         * @return this builder.
         */
        public Builder period(Integer period) {
            this.period = period;
            return this;
        }

        /**
         * The unit of the metric associated with the alarm.
         *
         * @return this builder.
         */
        public Builder unit(StandardUnit unit) {
            this.unit = unit;
            return this;
        }

        /**
         * The number of periods over which data is compared to the specified threshold.
         *
         * @return this builder.
         */
        public Builder evaluationPeriods(Integer evaluationPeriods) {
            this.evaluationPeriods = evaluationPeriods;
            return this;
        }

        /**
         * The number of data points that must be breaching to trigger the alarm.
         *
         * @return this builder.
         */
        public Builder datapointsToAlarm(Integer datapointsToAlarm) {
            this.datapointsToAlarm = datapointsToAlarm;
            return this;
        }

        /**
         * The value to compare with the specified statistic.
         *
         * @return this builder.
         */
        public Builder threshold(Double threshold) {
            this.threshold = threshold;
            return this;
        }

        /**
         * The arithmetic operation to use when comparing the specified statistic and threshold. The specified statistic
         * value is used as the first operand.
         *
         * @return this builder.
         */
        public Builder comparisonOperator(ComparisonOperator comparisonOperator) {
            this.comparisonOperator = comparisonOperator;
            return this;
        }

        /**
         * Sets how this alarm is to handle missing data points. The valid values are <code>breaching</code>, <code>
         * notBreaching</code>, <code>ignore</code>, and <code>missing</code>. For more information, see <a href="https://docs.aws.amazon.com/AmazonCloudWatch/latest/monitoring/AlarmThatSendsEmail.html#alarms-and-missing-data">Configuring how
         * CloudWatch alarms treat missing data</a>.
         *
         * <p>If this parameter is omitted, the default behavior of <code>missing</code> is used.
         *
         * <p>This parameter is not applicable to PromQL alarms.
         *
         * @return this builder.
         */
        public Builder treatMissingData(String treatMissingData) {
            this.treatMissingData = treatMissingData;
            return this;
        }

        /**
         * Used only for alarms based on percentiles. If <code>ignore</code>, the alarm state does not change during periods
         * with too few data points to be statistically significant. If <code>evaluate</code> or this parameter is not used,
         * the alarm is always evaluated and possibly changes state no matter how many data points are available.
         *
         * @return this builder.
         */
        public Builder evaluateLowSampleCountPercentile(String evaluateLowSampleCountPercentile) {
            this.evaluateLowSampleCountPercentile = evaluateLowSampleCountPercentile;
            return this;
        }

        /**
         * An array of MetricDataQuery structures, used in an alarm based on a metric math expression. Each structure either
         * retrieves a metric or performs a math expression. One item in the Metrics array is the math expression that the
         * alarm watches. This expression by designated by having <code>ReturnData</code> set to true.
         *
         * @return this builder.
         */
        public Builder metrics(List<MetricDataQuery> metrics) {
            this.metrics = metrics;
            return this;
        }

        /**
         * In an alarm based on an anomaly detection model, this is the ID of the <code>ANOMALY_DETECTION_BAND</code>
         * function used as the threshold for the alarm.
         *
         * @return this builder.
         */
        public Builder thresholdMetricId(String thresholdMetricId) {
            this.thresholdMetricId = thresholdMetricId;
            return this;
        }

        /**
         * If the value of this field is <code>PARTIAL_DATA</code>, it indicates that not all the available data was able to
         * be retrieved due to quota limitations. For more information, see <a href="https://docs.aws.amazon.com/AmazonCloudWatch/latest/monitoring/Create_Metrics_Insights_Alarm.html">Create alarms on Metrics Insights queries</a>
         * .
         *
         * <p>If the value of this field is <code>EVALUATION_ERROR</code>, it indicates configuration errors in alarm setup
         * that require review and correction. Refer to StateReason field of the alarm for more details.
         *
         * <p>If the value of this field is <code>EVALUATION_FAILURE</code>, it indicates temporary CloudWatch issues. We
         * recommend manual monitoring until the issue is resolved
         *
         * @return this builder.
         */
        public Builder evaluationState(EvaluationState evaluationState) {
            this.evaluationState = evaluationState;
            return this;
        }

        /**
         * The date and time that the alarm's <code>StateValue</code> most recently changed.
         *
         * @return this builder.
         */
        public Builder stateTransitionedTimestamp(Instant stateTransitionedTimestamp) {
            this.stateTransitionedTimestamp = stateTransitionedTimestamp;
            return this;
        }

        /**
         * The evaluation window that the alarm uses to select the range of metric data that it evaluates. This is either a
         * sliding window or a wall clock window. For more information, see <a href="https://docs.aws.amazon.com/AmazonCloudWatch/latest/monitoring/alarm-evaluation-window.html">Alarm evaluation windows</a> in the <i>
         * CloudWatch User Guide</i>.
         *
         * @return this builder.
         */
        public Builder evaluationWindow(EvaluationWindow evaluationWindow) {
            this.evaluationWindow = evaluationWindow;
            return this;
        }

        /**
         * The warm-up configuration for the alarm. A warm-up period delays alarm evaluation after you create or update the
         * alarm. During the warm-up period, the alarm stays in <code>INSUFFICIENT_DATA</code> and does not perform alarm
         * actions.
         *
         * <p>For more information, see <a href="https://docs.aws.amazon.com/AmazonCloudWatch/latest/monitoring/alarm-warm-up.html">Alarm warm-up periods</a> in the <i>Amazon CloudWatch User Guide</i>.
         *
         * @return this builder.
         */
        public Builder warmUpConfiguration(WarmUpConfiguration warmUpConfiguration) {
            this.warmUpConfiguration = warmUpConfiguration;
            return this;
        }

        /**
         * The evaluation criteria for the alarm.
         *
         * @return this builder.
         */
        public Builder evaluationCriteria(EvaluationCriteria evaluationCriteria) {
            this.evaluationCriteria = evaluationCriteria;
            return this;
        }

        /**
         * The frequency, in seconds, at which the alarm is evaluated.
         *
         * @return this builder.
         */
        public Builder evaluationInterval(Integer evaluationInterval) {
            this.evaluationInterval = evaluationInterval;
            return this;
        }

        @Override
        public MetricAlarm build() {
            return new MetricAlarm(this);
        }

        @Override
        @SuppressWarnings("unchecked")
        public void setMemberValue(Schema member, Object value) {
            switch (member.memberIndex()) {
                case 0 -> alarmName((String) SchemaUtils.validateSameMember($SCHEMA_ALARM_NAME, member, value));
                case 1 -> alarmArn((String) SchemaUtils.validateSameMember($SCHEMA_ALARM_ARN, member, value));
                case 2 -> alarmDescription((String) SchemaUtils.validateSameMember($SCHEMA_ALARM_DESCRIPTION, member, value));
                case 3 -> alarmConfigurationUpdatedTimestamp((Instant) SchemaUtils.validateSameMember($SCHEMA_ALARM_CONFIGURATION_UPDATED_TIMESTAMP, member, value));
                case 4 -> actionsEnabled((Boolean) SchemaUtils.validateSameMember($SCHEMA_ACTIONS_ENABLED, member, value));
                case 5 -> okActions((List<String>) SchemaUtils.validateSameMember($SCHEMA_OK_ACTIONS, member, value));
                case 6 -> alarmActions((List<String>) SchemaUtils.validateSameMember($SCHEMA_ALARM_ACTIONS, member, value));
                case 7 -> insufficientDataActions((List<String>) SchemaUtils.validateSameMember($SCHEMA_INSUFFICIENT_DATA_ACTIONS, member, value));
                case 8 -> stateValue((StateValue) SchemaUtils.validateSameMember($SCHEMA_STATE_VALUE, member, value));
                case 9 -> stateReason((String) SchemaUtils.validateSameMember($SCHEMA_STATE_REASON, member, value));
                case 10 -> stateReasonData((String) SchemaUtils.validateSameMember($SCHEMA_STATE_REASON_DATA, member, value));
                case 11 -> stateUpdatedTimestamp((Instant) SchemaUtils.validateSameMember($SCHEMA_STATE_UPDATED_TIMESTAMP, member, value));
                case 12 -> metricName((String) SchemaUtils.validateSameMember($SCHEMA_METRIC_NAME, member, value));
                case 13 -> namespace((String) SchemaUtils.validateSameMember($SCHEMA_NAMESPACE, member, value));
                case 14 -> statistic((Statistic) SchemaUtils.validateSameMember($SCHEMA_STATISTIC, member, value));
                case 15 -> extendedStatistic((String) SchemaUtils.validateSameMember($SCHEMA_EXTENDED_STATISTIC, member, value));
                case 16 -> dimensions((List<Dimension>) SchemaUtils.validateSameMember($SCHEMA_DIMENSIONS, member, value));
                case 17 -> period((Integer) SchemaUtils.validateSameMember($SCHEMA_PERIOD, member, value));
                case 18 -> unit((StandardUnit) SchemaUtils.validateSameMember($SCHEMA_UNIT, member, value));
                case 19 -> evaluationPeriods((Integer) SchemaUtils.validateSameMember($SCHEMA_EVALUATION_PERIODS, member, value));
                case 20 -> datapointsToAlarm((Integer) SchemaUtils.validateSameMember($SCHEMA_DATAPOINTS_TO_ALARM, member, value));
                case 21 -> threshold((Double) SchemaUtils.validateSameMember($SCHEMA_THRESHOLD, member, value));
                case 22 -> comparisonOperator((ComparisonOperator) SchemaUtils.validateSameMember($SCHEMA_COMPARISON_OPERATOR, member, value));
                case 23 -> treatMissingData((String) SchemaUtils.validateSameMember($SCHEMA_TREAT_MISSING_DATA, member, value));
                case 24 -> evaluateLowSampleCountPercentile((String) SchemaUtils.validateSameMember($SCHEMA_EVALUATE_LOW_SAMPLE_COUNT_PERCENTILE, member, value));
                case 25 -> metrics((List<MetricDataQuery>) SchemaUtils.validateSameMember($SCHEMA_METRICS, member, value));
                case 26 -> thresholdMetricId((String) SchemaUtils.validateSameMember($SCHEMA_THRESHOLD_METRIC_ID, member, value));
                case 27 -> evaluationState((EvaluationState) SchemaUtils.validateSameMember($SCHEMA_EVALUATION_STATE, member, value));
                case 28 -> stateTransitionedTimestamp((Instant) SchemaUtils.validateSameMember($SCHEMA_STATE_TRANSITIONED_TIMESTAMP, member, value));
                case 29 -> evaluationWindow((EvaluationWindow) SchemaUtils.validateSameMember($SCHEMA_EVALUATION_WINDOW, member, value));
                case 30 -> warmUpConfiguration((WarmUpConfiguration) SchemaUtils.validateSameMember($SCHEMA_WARM_UP_CONFIGURATION, member, value));
                case 31 -> evaluationCriteria((EvaluationCriteria) SchemaUtils.validateSameMember($SCHEMA_EVALUATION_CRITERIA, member, value));
                case 32 -> evaluationInterval((Integer) SchemaUtils.validateSameMember($SCHEMA_EVALUATION_INTERVAL, member, value));
                default -> ShapeBuilder.super.setMemberValue(member, value);
            }
        }

        @Override
        public Builder deserialize(ShapeDeserializer decoder) {
            decoder.readStruct($SCHEMA, this, $InnerDeserializer.INSTANCE);
            return this;
        }

        @Override
        public Builder deserializeMember(ShapeDeserializer decoder, Schema schema) {
            decoder.readStruct(schema.assertMemberTargetIs($SCHEMA), this, $InnerDeserializer.INSTANCE);
            return this;
        }

        private static final class $InnerDeserializer implements ShapeDeserializer.StructMemberConsumer<Builder> {
            private static final $InnerDeserializer INSTANCE = new $InnerDeserializer();

            @Override
            @SuppressWarnings("unchecked")
            public void accept(Builder builder, Schema member, ShapeDeserializer de) {
                switch (member.memberIndex()) {
                    case 0 -> builder.alarmName(de.readString(member));
                    case 1 -> builder.alarmArn(de.readString(member));
                    case 2 -> builder.alarmDescription(de.readString(member));
                    case 3 -> builder.alarmConfigurationUpdatedTimestamp(de.readTimestamp(member));
                    case 4 -> builder.actionsEnabled(de.readBoolean(member));
                    case 5 -> builder.okActions(SharedSerde.deserializeResourceList(member, de));
                    case 6 -> builder.alarmActions(SharedSerde.deserializeResourceList(member, de));
                    case 7 -> builder.insufficientDataActions(SharedSerde.deserializeResourceList(member, de));
                    case 8 -> builder.stateValue(StateValue.builder().deserializeMember(de, member).build());
                    case 9 -> builder.stateReason(de.readString(member));
                    case 10 -> builder.stateReasonData(de.readString(member));
                    case 11 -> builder.stateUpdatedTimestamp(de.readTimestamp(member));
                    case 12 -> builder.metricName(de.readString(member));
                    case 13 -> builder.namespace(de.readString(member));
                    case 14 -> builder.statistic(Statistic.builder().deserializeMember(de, member).build());
                    case 15 -> builder.extendedStatistic(de.readString(member));
                    case 16 -> builder.dimensions(SharedSerde.deserializeDimensions(member, de));
                    case 17 -> builder.period(de.readInteger(member));
                    case 18 -> builder.unit(StandardUnit.builder().deserializeMember(de, member).build());
                    case 19 -> builder.evaluationPeriods(de.readInteger(member));
                    case 20 -> builder.datapointsToAlarm(de.readInteger(member));
                    case 21 -> builder.threshold(de.readDouble(member));
                    case 22 -> builder.comparisonOperator(ComparisonOperator.builder().deserializeMember(de, member).build());
                    case 23 -> builder.treatMissingData(de.readString(member));
                    case 24 -> builder.evaluateLowSampleCountPercentile(de.readString(member));
                    case 25 -> builder.metrics(SharedSerde.deserializeMetricDataQueries(member, de));
                    case 26 -> builder.thresholdMetricId(de.readString(member));
                    case 27 -> builder.evaluationState(EvaluationState.builder().deserializeMember(de, member).build());
                    case 28 -> builder.stateTransitionedTimestamp(de.readTimestamp(member));
                    case 29 -> builder.evaluationWindow(EvaluationWindow.builder().deserializeMember(de, member).build());
                    case 30 -> builder.warmUpConfiguration(WarmUpConfiguration.builder().deserializeMember(de, member).build());
                    case 31 -> builder.evaluationCriteria(EvaluationCriteria.builder().deserializeMember(de, member).build());
                    case 32 -> builder.evaluationInterval(de.readInteger(member));
                    default -> throw new IllegalArgumentException("Unexpected member: " + member.memberName());
                }
            }
        }
    }
}
