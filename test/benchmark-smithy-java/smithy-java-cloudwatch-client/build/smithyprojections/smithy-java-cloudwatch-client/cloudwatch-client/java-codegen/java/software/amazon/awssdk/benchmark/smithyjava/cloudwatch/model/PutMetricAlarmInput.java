package software.amazon.awssdk.benchmark.smithyjava.cloudwatch.model;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import software.amazon.smithy.java.core.schema.PresenceTracker;
import software.amazon.smithy.java.core.schema.Schema;
import software.amazon.smithy.java.core.schema.SchemaUtils;
import software.amazon.smithy.java.core.schema.SerializableStruct;
import software.amazon.smithy.java.core.schema.ShapeBuilder;
import software.amazon.smithy.java.core.serde.ShapeDeserializer;
import software.amazon.smithy.java.core.serde.ShapeSerializer;
import software.amazon.smithy.java.core.serde.ToStringSerializer;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.utils.SmithyGenerated;

@SmithyGenerated
public final class PutMetricAlarmInput implements SerializableStruct {

    public static final Schema $SCHEMA = Schemas.PUT_METRIC_ALARM_INPUT;
    private static final Schema $SCHEMA_ALARM_NAME = $SCHEMA.member("AlarmName");
    private static final Schema $SCHEMA_ALARM_DESCRIPTION = $SCHEMA.member("AlarmDescription");
    private static final Schema $SCHEMA_ACTIONS_ENABLED = $SCHEMA.member("ActionsEnabled");
    private static final Schema $SCHEMA_OK_ACTIONS = $SCHEMA.member("OKActions");
    private static final Schema $SCHEMA_ALARM_ACTIONS = $SCHEMA.member("AlarmActions");
    private static final Schema $SCHEMA_INSUFFICIENT_DATA_ACTIONS = $SCHEMA.member("InsufficientDataActions");
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
    private static final Schema $SCHEMA_TAGS = $SCHEMA.member("Tags");
    private static final Schema $SCHEMA_THRESHOLD_METRIC_ID = $SCHEMA.member("ThresholdMetricId");
    private static final Schema $SCHEMA_EVALUATION_WINDOW = $SCHEMA.member("EvaluationWindow");
    private static final Schema $SCHEMA_WARM_UP_CONFIGURATION = $SCHEMA.member("WarmUpConfiguration");
    private static final Schema $SCHEMA_EVALUATION_CRITERIA = $SCHEMA.member("EvaluationCriteria");
    private static final Schema $SCHEMA_EVALUATION_INTERVAL = $SCHEMA.member("EvaluationInterval");

    public static final ShapeId $ID = $SCHEMA.id();

    private final transient String alarmName;
    private final transient String alarmDescription;
    private final transient Boolean actionsEnabled;
    private final transient List<String> okActions;
    private final transient List<String> alarmActions;
    private final transient List<String> insufficientDataActions;
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
    private final transient List<Tag> tags;
    private final transient String thresholdMetricId;
    private final transient EvaluationWindow evaluationWindow;
    private final transient WarmUpConfiguration warmUpConfiguration;
    private final transient EvaluationCriteria evaluationCriteria;
    private final transient Integer evaluationInterval;

    private PutMetricAlarmInput(Builder builder) {
        this.alarmName = builder.alarmName;
        this.alarmDescription = builder.alarmDescription;
        this.actionsEnabled = builder.actionsEnabled;
        this.okActions = builder.okActions == null ? null : Collections.unmodifiableList(builder.okActions);
        this.alarmActions = builder.alarmActions == null ? null : Collections.unmodifiableList(builder.alarmActions);
        this.insufficientDataActions = builder.insufficientDataActions == null ? null : Collections.unmodifiableList(builder.insufficientDataActions);
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
        this.tags = builder.tags == null ? null : Collections.unmodifiableList(builder.tags);
        this.thresholdMetricId = builder.thresholdMetricId;
        this.evaluationWindow = builder.evaluationWindow;
        this.warmUpConfiguration = builder.warmUpConfiguration;
        this.evaluationCriteria = builder.evaluationCriteria;
        this.evaluationInterval = builder.evaluationInterval;
    }

    /**
     * The name for the alarm. This name must be unique within the Region.
     *
     * <p>The name must contain only UTF-8 characters, and can't contain ASCII control characters
     */
    public String getAlarmName() {
        return alarmName;
    }

    /**
     * The description for the alarm.
     */
    public String getAlarmDescription() {
        return alarmDescription;
    }

    /**
     * Indicates whether actions should be executed during any changes to the alarm state. The default is
     * <code>TRUE</code>.
     */
    public Boolean isActionsEnabled() {
        return actionsEnabled;
    }

    /**
     * The actions to execute when this alarm transitions to an <code>OK</code> state from any other state. Each action
     * is specified as an Amazon Resource Name (ARN). Valid values:
     *
     * <p><b>EC2 actions:</b>
     *
     * <ul>
     *   <li>
     *     <code>arn:aws:automate:<i>region</i>:ec2:stop</code>
     *   </li>
     *   <li>
     *     <code>arn:aws:automate:<i>region</i>:ec2:terminate</code>
     *   </li>
     *   <li>
     *     <code>arn:aws:automate:<i>region</i>:ec2:reboot</code>
     *   </li>
     *   <li>
     *     <code>arn:aws:automate:<i>region</i>:ec2:recover</code>
     *   </li>
     *   <li>
     *     <code>arn:aws:swf:<i>region</i>:<i>account-id</i>:action/actions/AWS_EC2.InstanceId.Stop/1.0</code>
     *   </li>
     *   <li>
     *     <code>arn:aws:swf:<i>region</i>:<i>account-id</i>:action/actions/AWS_EC2.InstanceId.Terminate/1.0</code>
     *   </li>
     *   <li>
     *     <code>arn:aws:swf:<i>region</i>:<i>account-id</i>:action/actions/AWS_EC2.InstanceId.Reboot/1.0</code>
     *   </li>
     *   <li>
     *     <code>arn:aws:swf:<i>region</i>:<i>account-id</i>:action/actions/AWS_EC2.InstanceId.Recover/1.0</code>
     *   </li>
     * </ul>
     *
     * <p><b>Autoscaling action:</b>
     *
     * <ul>
     *   <li>
     *     <code>arn:aws:autoscaling:<i>region</i>:<i>account-id</i>:scalingPolicy:<i>policy-id</i>
     *     :autoScalingGroupName/<i>group-friendly-name</i>:policyName/<i>policy-friendly-name</i></code>
     *   </li>
     * </ul>
     *
     * <p><b>Lambda actions:</b>
     *
     * <ul>
     *   <li>
     *     Invoke the latest version of a Lambda function: <code>arn:aws:lambda:<i>region</i>:<i>account-id</i>
     *     :function:<i>function-name</i></code>
     *   </li>
     *   <li>
     *     Invoke a specific version of a Lambda function: <code>arn:aws:lambda:<i>region</i>:<i>account-id</i>
     *     :function:<i>function-name</i>:<i>version-number</i></code>
     *   </li>
     *   <li>
     *     Invoke a function by using an alias Lambda function: <code>arn:aws:lambda:<i>region</i>:<i>account-id</i>
     *     :function:<i>function-name</i>:<i>alias-name</i></code>
     *   </li>
     * </ul>
     *
     * <p><b>SNS notification action:</b>
     *
     * <ul>
     *   <li>
     *     <code>arn:aws:sns:<i>region</i>:<i>account-id</i>:<i>sns-topic-name</i></code>
     *   </li>
     * </ul>
     *
     * <p><b>SSM integration actions:</b>
     *
     * <ul>
     *   <li>
     *     <code>arn:aws:ssm:<i>region</i>:<i>account-id</i>:opsitem:<i>severity</i>#CATEGORY=<i>category-name</i></code>
     *   </li>
     *   <li>
     *     <code>arn:aws:ssm-incidents::<i>account-id</i>:responseplan/<i>response-plan-name</i></code>
     *   </li>
     * </ul>
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
     * action is specified as an Amazon Resource Name (ARN). Valid values:
     *
     * <p><b>EC2 actions:</b>
     *
     * <ul>
     *   <li>
     *     <code>arn:aws:automate:<i>region</i>:ec2:stop</code>
     *   </li>
     *   <li>
     *     <code>arn:aws:automate:<i>region</i>:ec2:terminate</code>
     *   </li>
     *   <li>
     *     <code>arn:aws:automate:<i>region</i>:ec2:reboot</code>
     *   </li>
     *   <li>
     *     <code>arn:aws:automate:<i>region</i>:ec2:recover</code>
     *   </li>
     *   <li>
     *     <code>arn:aws:swf:<i>region</i>:<i>account-id</i>:action/actions/AWS_EC2.InstanceId.Stop/1.0</code>
     *   </li>
     *   <li>
     *     <code>arn:aws:swf:<i>region</i>:<i>account-id</i>:action/actions/AWS_EC2.InstanceId.Terminate/1.0</code>
     *   </li>
     *   <li>
     *     <code>arn:aws:swf:<i>region</i>:<i>account-id</i>:action/actions/AWS_EC2.InstanceId.Reboot/1.0</code>
     *   </li>
     *   <li>
     *     <code>arn:aws:swf:<i>region</i>:<i>account-id</i>:action/actions/AWS_EC2.InstanceId.Recover/1.0</code>
     *   </li>
     * </ul>
     *
     * <p><b>Autoscaling action:</b>
     *
     * <ul>
     *   <li>
     *     <code>arn:aws:autoscaling:<i>region</i>:<i>account-id</i>:scalingPolicy:<i>policy-id</i>
     *     :autoScalingGroupName/<i>group-friendly-name</i>:policyName/<i>policy-friendly-name</i></code>
     *   </li>
     * </ul>
     *
     * <p><b>Lambda actions:</b>
     *
     * <ul>
     *   <li>
     *     Invoke the latest version of a Lambda function: <code>arn:aws:lambda:<i>region</i>:<i>account-id</i>
     *     :function:<i>function-name</i></code>
     *   </li>
     *   <li>
     *     Invoke a specific version of a Lambda function: <code>arn:aws:lambda:<i>region</i>:<i>account-id</i>
     *     :function:<i>function-name</i>:<i>version-number</i></code>
     *   </li>
     *   <li>
     *     Invoke a function by using an alias Lambda function: <code>arn:aws:lambda:<i>region</i>:<i>account-id</i>
     *     :function:<i>function-name</i>:<i>alias-name</i></code>
     *   </li>
     * </ul>
     *
     * <p><b>SNS notification action:</b>
     *
     * <ul>
     *   <li>
     *     <code>arn:aws:sns:<i>region</i>:<i>account-id</i>:<i>sns-topic-name</i></code>
     *   </li>
     * </ul>
     *
     * <p><b>SSM integration actions:</b>
     *
     * <ul>
     *   <li>
     *     <code>arn:aws:ssm:<i>region</i>:<i>account-id</i>:opsitem:<i>severity</i>#CATEGORY=<i>category-name</i></code>
     *   </li>
     *   <li>
     *     <code>arn:aws:ssm-incidents::<i>account-id</i>:responseplan/<i>response-plan-name</i></code>
     *   </li>
     * </ul>
     *
     * <p><b>Start a Amazon Q Developer operational investigation</b>
     *
     * <p><code>arn:aws:aiops:<i>region</i>:<i>account-id</i>:investigation-group:<i>investigation-group-id</i></code>
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
     * state. Each action is specified as an Amazon Resource Name (ARN). Valid values:
     *
     * <p><b>EC2 actions:</b>
     *
     * <ul>
     *   <li>
     *     <code>arn:aws:automate:<i>region</i>:ec2:stop</code>
     *   </li>
     *   <li>
     *     <code>arn:aws:automate:<i>region</i>:ec2:terminate</code>
     *   </li>
     *   <li>
     *     <code>arn:aws:automate:<i>region</i>:ec2:reboot</code>
     *   </li>
     *   <li>
     *     <code>arn:aws:automate:<i>region</i>:ec2:recover</code>
     *   </li>
     *   <li>
     *     <code>arn:aws:swf:<i>region</i>:<i>account-id</i>:action/actions/AWS_EC2.InstanceId.Stop/1.0</code>
     *   </li>
     *   <li>
     *     <code>arn:aws:swf:<i>region</i>:<i>account-id</i>:action/actions/AWS_EC2.InstanceId.Terminate/1.0</code>
     *   </li>
     *   <li>
     *     <code>arn:aws:swf:<i>region</i>:<i>account-id</i>:action/actions/AWS_EC2.InstanceId.Reboot/1.0</code>
     *   </li>
     *   <li>
     *     <code>arn:aws:swf:<i>region</i>:<i>account-id</i>:action/actions/AWS_EC2.InstanceId.Recover/1.0</code>
     *   </li>
     * </ul>
     *
     * <p><b>Autoscaling action:</b>
     *
     * <ul>
     *   <li>
     *     <code>arn:aws:autoscaling:<i>region</i>:<i>account-id</i>:scalingPolicy:<i>policy-id</i>
     *     :autoScalingGroupName/<i>group-friendly-name</i>:policyName/<i>policy-friendly-name</i></code>
     *   </li>
     * </ul>
     *
     * <p><b>Lambda actions:</b>
     *
     * <ul>
     *   <li>
     *     Invoke the latest version of a Lambda function: <code>arn:aws:lambda:<i>region</i>:<i>account-id</i>
     *     :function:<i>function-name</i></code>
     *   </li>
     *   <li>
     *     Invoke a specific version of a Lambda function: <code>arn:aws:lambda:<i>region</i>:<i>account-id</i>
     *     :function:<i>function-name</i>:<i>version-number</i></code>
     *   </li>
     *   <li>
     *     Invoke a function by using an alias Lambda function: <code>arn:aws:lambda:<i>region</i>:<i>account-id</i>
     *     :function:<i>function-name</i>:<i>alias-name</i></code>
     *   </li>
     * </ul>
     *
     * <p><b>SNS notification action:</b>
     *
     * <ul>
     *   <li>
     *     <code>arn:aws:sns:<i>region</i>:<i>account-id</i>:<i>sns-topic-name</i></code>
     *   </li>
     * </ul>
     *
     * <p><b>SSM integration actions:</b>
     *
     * <ul>
     *   <li>
     *     <code>arn:aws:ssm:<i>region</i>:<i>account-id</i>:opsitem:<i>severity</i>#CATEGORY=<i>category-name</i></code>
     *   </li>
     *   <li>
     *     <code>arn:aws:ssm-incidents::<i>account-id</i>:responseplan/<i>response-plan-name</i></code>
     *   </li>
     * </ul>
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
     * The name for the metric associated with the alarm. For each <code>PutMetricAlarm</code> operation, you must
     * specify either <code>MetricName</code>, a <code>Metrics</code> array, or an <code>EvaluationCriteria</code>.
     *
     * <p>If you are creating an alarm based on a math expression, you cannot specify this parameter, or any of the <code>
     * Namespace</code>, <code>Dimensions</code>, <code>Period</code>, <code>Unit</code>, <code>Statistic</code>, or <code>
     * ExtendedStatistic</code> parameters. Instead, you specify all this information in the <code>Metrics</code> array.
     */
    public String getMetricName() {
        return metricName;
    }

    /**
     * The namespace for the metric associated specified in <code>MetricName</code>.
     */
    public String getNamespace() {
        return namespace;
    }

    /**
     * The statistic for the metric specified in <code>MetricName</code>, other than percentile. For percentile
     * statistics, use <code>ExtendedStatistic</code>. When you call <code>PutMetricAlarm</code> and specify a
     * <code>MetricName</code>, you must specify either <code>Statistic</code> or <code>ExtendedStatistic,</code> but
     * not both.
     */
    public Statistic getStatistic() {
        return statistic;
    }

    /**
     * The extended statistic for the metric specified in <code>MetricName</code>. When you call <code>PutMetricAlarm</code>
     * and specify a <code>MetricName</code>, you must specify either <code>Statistic</code> or <code>ExtendedStatistic</code>
     * but not both.
     *
     * <p>If you specify <code>ExtendedStatistic</code>, the following are valid values:
     *
     * <ul>
     *   <li>
     *     <code>p90</code>
     *   </li>
     *   <li>
     *     <code>tm90</code>
     *   </li>
     *   <li>
     *     <code>tc90</code>
     *   </li>
     *   <li>
     *     <code>ts90</code>
     *   </li>
     *   <li>
     *     <code>wm90</code>
     *   </li>
     *   <li>
     *     <code>IQM</code>
     *   </li>
     *   <li>
     *     <code>PR(<i>n</i>:<i>m</i>)</code> where n and m are values of the metric
     *   </li>
     *   <li>
     *     <code>TC(<i>X</i>%:<i>X</i>%)</code> where X is between 10 and 90 inclusive.
     *   </li>
     *   <li>
     *     <code>TM(<i>X</i>%:<i>X</i>%)</code> where X is between 10 and 90 inclusive.
     *   </li>
     *   <li>
     *     <code>TS(<i>X</i>%:<i>X</i>%)</code> where X is between 10 and 90 inclusive.
     *   </li>
     *   <li>
     *     <code>WM(<i>X</i>%:<i>X</i>%)</code> where X is between 10 and 90 inclusive.
     *   </li>
     * </ul>
     *
     * <p>For more information about these extended statistics, see <a href="https://docs.aws.amazon.com/AmazonCloudWatch/latest/monitoring/Statistics-definitions.html">CloudWatch statistics definitions</a>.
     */
    public String getExtendedStatistic() {
        return extendedStatistic;
    }

    /**
     * The dimensions for the metric specified in <code>MetricName</code>.
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
     * The length, in seconds, used each time the metric specified in <code>MetricName</code> is evaluated. Valid values
     * are 10, 20, 30, and any multiple of 60.
     *
     * <p><code>Period</code> is required for alarms based on static thresholds. If you are creating an alarm based on a
     * metric math expression, you specify the period for each metric within the objects in the <code>Metrics</code>
     * array.
     *
     * <p>Be sure to specify 10, 20, or 30 only for metrics that are stored by a <code>PutMetricData</code> call with a <code>
     * StorageResolution</code> of 1. If you specify a period of 10, 20, or 30 for a metric that does not have
     * sub-minute resolution, the alarm still attempts to gather data at the period rate that you specify. In this case,
     * it does not receive data for the attempts that do not correspond to a one-minute data resolution, and the alarm
     * might often lapse into INSUFFICENT_DATA status. Specifying 10, 20, or 30 also sets this alarm as a
     * high-resolution alarm, which has a higher charge than other alarms. For more information about pricing, see <a href="https://aws.amazon.com/cloudwatch/pricing/">
     * Amazon CloudWatch Pricing</a>.
     *
     * <p>An alarm's total current evaluation period can be no longer than seven days, so <code>Period</code> multiplied
     * by <code>EvaluationPeriods</code> can't be more than 604,800 seconds. For alarms with a period of less than one
     * hour (3,600 seconds), the total evaluation period can't be longer than one day (86,400 seconds).
     */
    public Integer getPeriod() {
        return period;
    }

    /**
     * The unit of measure for the statistic. For example, the units for the Amazon EC2 NetworkIn metric are Bytes
     * because NetworkIn tracks the number of bytes that an instance receives on all network interfaces. You can also
     * specify a unit when you create a custom metric. Units help provide conceptual meaning to your data. Metric data
     * points that specify a unit of measure, such as Percent, are aggregated separately. If you are creating an alarm
     * based on a metric math expression, you can specify the unit for each metric (if needed) within the objects in the
     * <code>Metrics</code> array.
     *
     * <p>If you don't specify <code>Unit</code>, CloudWatch retrieves all unit types that have been published for the
     * metric and attempts to evaluate the alarm. Usually, metrics are published with only one unit, so the alarm works
     * as intended.
     *
     * <p>However, if the metric is published with multiple types of units and you don't specify a unit, the alarm's
     * behavior is not defined and it behaves unpredictably.
     *
     * <p>We recommend omitting <code>Unit</code> so that you don't inadvertently specify an incorrect unit that is not
     * published for this metric. Doing so causes the alarm to be stuck in the <code>INSUFFICIENT DATA</code> state.
     */
    public StandardUnit getUnit() {
        return unit;
    }

    /**
     * The number of periods over which data is compared to the specified threshold. If you are setting an alarm that
     * requires that a number of consecutive data points be breaching to trigger the alarm, this value specifies that
     * number. If you are setting an "M out of N" alarm, this value is the N.
     */
    public Integer getEvaluationPeriods() {
        return evaluationPeriods;
    }

    /**
     * The number of data points that must be breaching to trigger the alarm. This is used only if you are setting an "M
     * out of N" alarm. In that case, this value is the M. For more information, see <a href="https://docs.aws.amazon.com/AmazonCloudWatch/latest/monitoring/AlarmThatSendsEmail.html#alarm-evaluation">Evaluating an Alarm</a> in the <i>
     * Amazon CloudWatch User Guide</i>.
     */
    public Integer getDatapointsToAlarm() {
        return datapointsToAlarm;
    }

    /**
     * The value against which the specified statistic is compared.
     *
     * <p>This parameter is required for alarms based on static thresholds, but should not be used for alarms based on
     * anomaly detection models.
     */
    public Double getThreshold() {
        return threshold;
    }

    /**
     * The arithmetic operation to use when comparing the specified statistic and threshold. The specified statistic
     * value is used as the first operand.
     *
     * <p>The values <code>LessThanLowerOrGreaterThanUpperThreshold</code>, <code>LessThanLowerThreshold</code>, and <code>
     * GreaterThanUpperThreshold</code> are used only for alarms based on anomaly detection models.
     */
    public ComparisonOperator getComparisonOperator() {
        return comparisonOperator;
    }

    /**
     * Sets how this alarm is to handle missing data points. If <code>TreatMissingData</code> is omitted, the default
     * behavior of <code>missing</code> is used. For more information, see <a href="https://docs.aws.amazon.com/AmazonCloudWatch/latest/monitoring/AlarmThatSendsEmail.html#alarms-and-missing-data">Configuring How CloudWatch Alarms Treats
     * Missing Data</a>.
     *
     * <p>Valid Values: <code>breaching | notBreaching | ignore | missing</code>
     *
     * <p>Alarms that evaluate metrics in the <code>AWS/DynamoDB</code> namespace always <code>ignore</code> missing
     * data even if you choose a different option for <code>TreatMissingData</code>. When an <code>AWS/DynamoDB</code>
     * metric has missing data, alarms that evaluate that metric remain in their current state.
     *
     * <p>This parameter is not applicable to PromQL alarms.
     */
    public String getTreatMissingData() {
        return treatMissingData;
    }

    /**
     * Used only for alarms based on percentiles. If you specify <code>ignore</code>, the alarm state does not change
     * during periods with too few data points to be statistically significant. If you specify <code>evaluate</code> or
     * omit this parameter, the alarm is always evaluated and possibly changes state no matter how many data points are
     * available. For more information, see <a href="https://docs.aws.amazon.com/AmazonCloudWatch/latest/monitoring/AlarmThatSendsEmail.html#percentiles-with-low-samples">Percentile-Based CloudWatch Alarms and Low Data Samples</a>.
     *
     * <p>Valid Values: <code>evaluate | ignore</code>
     */
    public String getEvaluateLowSampleCountPercentile() {
        return evaluateLowSampleCountPercentile;
    }

    /**
     * An array of <code>MetricDataQuery</code> structures that enable you to create an alarm based on the result of a
     * metric math expression. For each <code>PutMetricAlarm</code> operation, you must specify either <code>MetricName</code>
     * , a <code>Metrics</code> array, or an <code>EvaluationCriteria</code>.
     *
     * <p>Each item in the <code>Metrics</code> array either retrieves a metric or performs a math expression.
     *
     * <p>One item in the <code>Metrics</code> array is the expression that the alarm watches. You designate this
     * expression by setting <code>ReturnData</code> to true for this object in the array. For more information, see <a href="https://docs.aws.amazon.com/AmazonCloudWatch/latest/APIReference/API_MetricDataQuery.html">
     * MetricDataQuery</a>.
     *
     * <p>If you use the <code>Metrics</code> parameter, you cannot include the <code>Namespace</code>, <code>MetricName</code>
     * , <code>Dimensions</code>, <code>Period</code>, <code>Unit</code>, <code>Statistic</code>, or <code>
     * ExtendedStatistic</code> parameters of <code>PutMetricAlarm</code> in the same operation. Instead, you retrieve
     * the metrics you are using in your math expression as part of the <code>Metrics</code> array.
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
     * A list of key-value pairs to associate with the alarm. You can associate as many as 50 tags with an alarm. To be
     * able to associate tags with the alarm when you create the alarm, you must have the <code>cloudwatch:TagResource</code>
     * permission.
     *
     * <p>Tags can help you organize and categorize your resources. You can also use them to scope user permissions by
     * granting a user permission to access or change only resources with certain tag values.
     *
     * <p>If you are using this operation to update an existing alarm, any tags you specify in this parameter are
     * ignored. To change the tags of an existing alarm, use <a href="https://docs.aws.amazon.com/AmazonCloudWatch/latest/APIReference/API_TagResource.html">TagResource</a> or <a href="https://docs.aws.amazon.com/AmazonCloudWatch/latest/APIReference/API_UntagResource.html">UntagResource</a>.
     *
     * <p>To use this field to set tags for an alarm when you create it, you must be signed on with both the <code>
     * cloudwatch:PutMetricAlarm</code> and <code>cloudwatch:TagResource</code> permissions.
     */
    public List<Tag> getTags() {
        if (tags == null) {
            return Collections.emptyList();
        }
        return tags;
    }

    public boolean hasTags() {
        return tags != null;
    }

    /**
     * If this is an alarm based on an anomaly detection model, make this value match the ID of the <code>
     * ANOMALY_DETECTION_BAND</code> function.
     *
     * <p>For an example of how to use this parameter, see the <b>Anomaly Detection Model Alarm</b> example on this
     * page.
     *
     * <p>If your alarm uses this parameter, it cannot have Auto Scaling actions.
     */
    public String getThresholdMetricId() {
        return thresholdMetricId;
    }

    /**
     * The evaluation window that the alarm uses to select the range of metric data that it evaluates. Specify either a
     * sliding window or a wall clock window. If you omit this parameter, the alarm uses a sliding window.
     *
     * <p>A sliding window advances each time the alarm is evaluated, forming a rolling time window. A wall clock window
     * aligns the evaluated range to fixed clock boundaries, such as the top of the hour or the start of the day.
     *
     * <p>You can use <code>EvaluationWindow</code> with any type of metric alarm except alarms that are based on a
     * PromQL query.
     *
     * <p>For more information, see <a href="https://docs.aws.amazon.com/AmazonCloudWatch/latest/monitoring/alarm-evaluation-window.html">Alarm evaluation windows</a> in the <i>CloudWatch User Guide</i>.
     */
    public EvaluationWindow getEvaluationWindow() {
        return evaluationWindow;
    }

    /**
     * The warm-up configuration for the alarm. A warm-up period delays alarm evaluation after you create or update the
     * alarm. The warm-up period reduces alarm noise from missing data while a new resource or service starts publishing
     * metrics.
     *
     * <p>For more information, see <a href="https://docs.aws.amazon.com/AmazonCloudWatch/latest/monitoring/alarm-warm-up.html">Alarm warm-up periods</a> in the <i>Amazon CloudWatch User Guide</i>.
     */
    public WarmUpConfiguration getWarmUpConfiguration() {
        return warmUpConfiguration;
    }

    /**
     * The evaluation criteria for the alarm. For each <code>PutMetricAlarm</code> operation, you must specify either <code>
     * MetricName</code>, a <code>Metrics</code> array, or an <code>EvaluationCriteria</code>.
     *
     * <p>If you use the <code>EvaluationCriteria</code> parameter, you cannot include the <code>Namespace</code>, <code>
     * MetricName</code>, <code>Dimensions</code>, <code>Period</code>, <code>Unit</code>, <code>Statistic</code>, <code>
     * ExtendedStatistic</code>, <code>Metrics</code>, <code>Threshold</code>, <code>ComparisonOperator</code>, <code>
     * ThresholdMetricId</code>, <code>EvaluationPeriods</code>, or <code>DatapointsToAlarm</code> parameters of <code>
     * PutMetricAlarm</code> in the same operation. Instead, all evaluation parameters are defined within this
     * structure.
     *
     * <p>For an example of how to use this parameter, see the <b>PromQL alarm</b> example on this page.
     */
    public EvaluationCriteria getEvaluationCriteria() {
        return evaluationCriteria;
    }

    /**
     * The frequency, in seconds, at which the alarm is evaluated. Valid values are 10, 20, 30, and any multiple of 60.
     *
     * <p>This parameter is required for alarms that use <code>EvaluationCriteria</code>, and cannot be specified for
     * alarms configured with <code>MetricName</code> or <code>Metrics</code>.
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
        PutMetricAlarmInput that = (PutMetricAlarmInput) other;
        return Objects.equals(this.actionsEnabled, that.actionsEnabled)
               && Objects.equals(this.period, that.period)
               && Objects.equals(this.evaluationPeriods, that.evaluationPeriods)
               && Objects.equals(this.datapointsToAlarm, that.datapointsToAlarm)
               && Objects.equals(this.evaluationInterval, that.evaluationInterval)
               && Objects.equals(this.threshold, that.threshold)
               && Objects.equals(this.alarmName, that.alarmName)
               && Objects.equals(this.alarmDescription, that.alarmDescription)
               && Objects.equals(this.metricName, that.metricName)
               && Objects.equals(this.namespace, that.namespace)
               && Objects.equals(this.extendedStatistic, that.extendedStatistic)
               && Objects.equals(this.treatMissingData, that.treatMissingData)
               && Objects.equals(this.evaluateLowSampleCountPercentile, that.evaluateLowSampleCountPercentile)
               && Objects.equals(this.thresholdMetricId, that.thresholdMetricId)
               && Objects.equals(this.statistic, that.statistic)
               && Objects.equals(this.unit, that.unit)
               && Objects.equals(this.comparisonOperator, that.comparisonOperator)
               && Objects.equals(this.evaluationWindow, that.evaluationWindow)
               && Objects.equals(this.evaluationCriteria, that.evaluationCriteria)
               && Objects.equals(this.warmUpConfiguration, that.warmUpConfiguration)
               && Objects.equals(this.okActions, that.okActions)
               && Objects.equals(this.alarmActions, that.alarmActions)
               && Objects.equals(this.insufficientDataActions, that.insufficientDataActions)
               && Objects.equals(this.dimensions, that.dimensions)
               && Objects.equals(this.metrics, that.metrics)
               && Objects.equals(this.tags, that.tags);
    }

    @Override
    public int hashCode() {
        int $hc = Objects.hashCode(alarmName);
        $hc = 31 * $hc + Objects.hashCode(alarmDescription);
        $hc = 31 * $hc + Objects.hashCode(actionsEnabled);
        $hc = 31 * $hc + Objects.hashCode(okActions);
        $hc = 31 * $hc + Objects.hashCode(alarmActions);
        $hc = 31 * $hc + Objects.hashCode(insufficientDataActions);
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
        $hc = 31 * $hc + Objects.hashCode(tags);
        $hc = 31 * $hc + Objects.hashCode(thresholdMetricId);
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
        if (alarmDescription != null) {
            serializer.writeString($SCHEMA_ALARM_DESCRIPTION, alarmDescription);
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
        if (tags != null) {
            serializer.writeList($SCHEMA_TAGS, tags, tags.size(), SharedSerde.TagListSerializer.INSTANCE);
        }
        if (thresholdMetricId != null) {
            serializer.writeString($SCHEMA_THRESHOLD_METRIC_ID, thresholdMetricId);
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
            case 1 -> (T) SchemaUtils.validateSameMember($SCHEMA_ALARM_DESCRIPTION, member, alarmDescription);
            case 2 -> (T) SchemaUtils.validateSameMember($SCHEMA_ACTIONS_ENABLED, member, actionsEnabled);
            case 3 -> (T) SchemaUtils.validateSameMember($SCHEMA_OK_ACTIONS, member, okActions);
            case 4 -> (T) SchemaUtils.validateSameMember($SCHEMA_ALARM_ACTIONS, member, alarmActions);
            case 5 -> (T) SchemaUtils.validateSameMember($SCHEMA_INSUFFICIENT_DATA_ACTIONS, member, insufficientDataActions);
            case 6 -> (T) SchemaUtils.validateSameMember($SCHEMA_METRIC_NAME, member, metricName);
            case 7 -> (T) SchemaUtils.validateSameMember($SCHEMA_NAMESPACE, member, namespace);
            case 8 -> (T) SchemaUtils.validateSameMember($SCHEMA_STATISTIC, member, statistic);
            case 9 -> (T) SchemaUtils.validateSameMember($SCHEMA_EXTENDED_STATISTIC, member, extendedStatistic);
            case 10 -> (T) SchemaUtils.validateSameMember($SCHEMA_DIMENSIONS, member, dimensions);
            case 11 -> (T) SchemaUtils.validateSameMember($SCHEMA_PERIOD, member, period);
            case 12 -> (T) SchemaUtils.validateSameMember($SCHEMA_UNIT, member, unit);
            case 13 -> (T) SchemaUtils.validateSameMember($SCHEMA_EVALUATION_PERIODS, member, evaluationPeriods);
            case 14 -> (T) SchemaUtils.validateSameMember($SCHEMA_DATAPOINTS_TO_ALARM, member, datapointsToAlarm);
            case 15 -> (T) SchemaUtils.validateSameMember($SCHEMA_THRESHOLD, member, threshold);
            case 16 -> (T) SchemaUtils.validateSameMember($SCHEMA_COMPARISON_OPERATOR, member, comparisonOperator);
            case 17 -> (T) SchemaUtils.validateSameMember($SCHEMA_TREAT_MISSING_DATA, member, treatMissingData);
            case 18 -> (T) SchemaUtils.validateSameMember($SCHEMA_EVALUATE_LOW_SAMPLE_COUNT_PERCENTILE, member, evaluateLowSampleCountPercentile);
            case 19 -> (T) SchemaUtils.validateSameMember($SCHEMA_METRICS, member, metrics);
            case 20 -> (T) SchemaUtils.validateSameMember($SCHEMA_TAGS, member, tags);
            case 21 -> (T) SchemaUtils.validateSameMember($SCHEMA_THRESHOLD_METRIC_ID, member, thresholdMetricId);
            case 22 -> (T) SchemaUtils.validateSameMember($SCHEMA_EVALUATION_WINDOW, member, evaluationWindow);
            case 23 -> (T) SchemaUtils.validateSameMember($SCHEMA_WARM_UP_CONFIGURATION, member, warmUpConfiguration);
            case 24 -> (T) SchemaUtils.validateSameMember($SCHEMA_EVALUATION_CRITERIA, member, evaluationCriteria);
            case 25 -> (T) SchemaUtils.validateSameMember($SCHEMA_EVALUATION_INTERVAL, member, evaluationInterval);
            default -> throw new IllegalArgumentException("Attempted to get non-existent member: " + member.id());
        };
    }

    /**
     * Create a new builder containing all the current property values of this object.
     *
     * <p><strong>Note:</strong> This method performs only a shallow copy of the original properties.
     *
     * @return a builder for {@link PutMetricAlarmInput}.
     */
    public Builder toBuilder() {
        var builder = new Builder();
        builder.alarmName(this.alarmName);
        builder.alarmDescription(this.alarmDescription);
        builder.actionsEnabled(this.actionsEnabled);
        builder.okActions(this.okActions);
        builder.alarmActions(this.alarmActions);
        builder.insufficientDataActions(this.insufficientDataActions);
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
        builder.tags(this.tags);
        builder.thresholdMetricId(this.thresholdMetricId);
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
     * Builder for {@link PutMetricAlarmInput}.
     */
    public static final class Builder implements ShapeBuilder<PutMetricAlarmInput> {
        private final PresenceTracker tracker = PresenceTracker.of($SCHEMA);
        private String alarmName;
        private String alarmDescription;
        private Boolean actionsEnabled;
        private List<String> okActions;
        private List<String> alarmActions;
        private List<String> insufficientDataActions;
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
        private List<Tag> tags;
        private String thresholdMetricId;
        private EvaluationWindow evaluationWindow;
        private WarmUpConfiguration warmUpConfiguration;
        private EvaluationCriteria evaluationCriteria;
        private Integer evaluationInterval;

        private Builder() {
            // Tell the tracker to assume clientOptional members are present.
            tracker.setMember($SCHEMA_ALARM_NAME);
        }

        @Override
        public Schema schema() {
            return $SCHEMA;
        }

        /**
         * The name for the alarm. This name must be unique within the Region.
         *
         * <p>The name must contain only UTF-8 characters, and can't contain ASCII control characters
         *
         * <p><strong>Required</strong>
         * @return this builder.
         */
        public Builder alarmName(String alarmName) {
            this.alarmName = Objects.requireNonNull(alarmName, "alarmName cannot be null");
            tracker.setMember($SCHEMA_ALARM_NAME);
            return this;
        }

        /**
         * The description for the alarm.
         *
         * @return this builder.
         */
        public Builder alarmDescription(String alarmDescription) {
            this.alarmDescription = alarmDescription;
            return this;
        }

        /**
         * Indicates whether actions should be executed during any changes to the alarm state. The default is
         * <code>TRUE</code>.
         *
         * @return this builder.
         */
        public Builder actionsEnabled(Boolean actionsEnabled) {
            this.actionsEnabled = actionsEnabled;
            return this;
        }

        /**
         * The actions to execute when this alarm transitions to an <code>OK</code> state from any other state. Each action
         * is specified as an Amazon Resource Name (ARN). Valid values:
         *
         * <p><b>EC2 actions:</b>
         *
         * <ul>
         *   <li>
         *     <code>arn:aws:automate:<i>region</i>:ec2:stop</code>
         *   </li>
         *   <li>
         *     <code>arn:aws:automate:<i>region</i>:ec2:terminate</code>
         *   </li>
         *   <li>
         *     <code>arn:aws:automate:<i>region</i>:ec2:reboot</code>
         *   </li>
         *   <li>
         *     <code>arn:aws:automate:<i>region</i>:ec2:recover</code>
         *   </li>
         *   <li>
         *     <code>arn:aws:swf:<i>region</i>:<i>account-id</i>:action/actions/AWS_EC2.InstanceId.Stop/1.0</code>
         *   </li>
         *   <li>
         *     <code>arn:aws:swf:<i>region</i>:<i>account-id</i>:action/actions/AWS_EC2.InstanceId.Terminate/1.0</code>
         *   </li>
         *   <li>
         *     <code>arn:aws:swf:<i>region</i>:<i>account-id</i>:action/actions/AWS_EC2.InstanceId.Reboot/1.0</code>
         *   </li>
         *   <li>
         *     <code>arn:aws:swf:<i>region</i>:<i>account-id</i>:action/actions/AWS_EC2.InstanceId.Recover/1.0</code>
         *   </li>
         * </ul>
         *
         * <p><b>Autoscaling action:</b>
         *
         * <ul>
         *   <li>
         *     <code>arn:aws:autoscaling:<i>region</i>:<i>account-id</i>:scalingPolicy:<i>policy-id</i>
         *     :autoScalingGroupName/<i>group-friendly-name</i>:policyName/<i>policy-friendly-name</i></code>
         *   </li>
         * </ul>
         *
         * <p><b>Lambda actions:</b>
         *
         * <ul>
         *   <li>
         *     Invoke the latest version of a Lambda function: <code>arn:aws:lambda:<i>region</i>:<i>account-id</i>
         *     :function:<i>function-name</i></code>
         *   </li>
         *   <li>
         *     Invoke a specific version of a Lambda function: <code>arn:aws:lambda:<i>region</i>:<i>account-id</i>
         *     :function:<i>function-name</i>:<i>version-number</i></code>
         *   </li>
         *   <li>
         *     Invoke a function by using an alias Lambda function: <code>arn:aws:lambda:<i>region</i>:<i>account-id</i>
         *     :function:<i>function-name</i>:<i>alias-name</i></code>
         *   </li>
         * </ul>
         *
         * <p><b>SNS notification action:</b>
         *
         * <ul>
         *   <li>
         *     <code>arn:aws:sns:<i>region</i>:<i>account-id</i>:<i>sns-topic-name</i></code>
         *   </li>
         * </ul>
         *
         * <p><b>SSM integration actions:</b>
         *
         * <ul>
         *   <li>
         *     <code>arn:aws:ssm:<i>region</i>:<i>account-id</i>:opsitem:<i>severity</i>#CATEGORY=<i>category-name</i></code>
         *   </li>
         *   <li>
         *     <code>arn:aws:ssm-incidents::<i>account-id</i>:responseplan/<i>response-plan-name</i></code>
         *   </li>
         * </ul>
         *
         * @return this builder.
         */
        public Builder okActions(List<String> okActions) {
            this.okActions = okActions;
            return this;
        }

        /**
         * The actions to execute when this alarm transitions to the <code>ALARM</code> state from any other state. Each
         * action is specified as an Amazon Resource Name (ARN). Valid values:
         *
         * <p><b>EC2 actions:</b>
         *
         * <ul>
         *   <li>
         *     <code>arn:aws:automate:<i>region</i>:ec2:stop</code>
         *   </li>
         *   <li>
         *     <code>arn:aws:automate:<i>region</i>:ec2:terminate</code>
         *   </li>
         *   <li>
         *     <code>arn:aws:automate:<i>region</i>:ec2:reboot</code>
         *   </li>
         *   <li>
         *     <code>arn:aws:automate:<i>region</i>:ec2:recover</code>
         *   </li>
         *   <li>
         *     <code>arn:aws:swf:<i>region</i>:<i>account-id</i>:action/actions/AWS_EC2.InstanceId.Stop/1.0</code>
         *   </li>
         *   <li>
         *     <code>arn:aws:swf:<i>region</i>:<i>account-id</i>:action/actions/AWS_EC2.InstanceId.Terminate/1.0</code>
         *   </li>
         *   <li>
         *     <code>arn:aws:swf:<i>region</i>:<i>account-id</i>:action/actions/AWS_EC2.InstanceId.Reboot/1.0</code>
         *   </li>
         *   <li>
         *     <code>arn:aws:swf:<i>region</i>:<i>account-id</i>:action/actions/AWS_EC2.InstanceId.Recover/1.0</code>
         *   </li>
         * </ul>
         *
         * <p><b>Autoscaling action:</b>
         *
         * <ul>
         *   <li>
         *     <code>arn:aws:autoscaling:<i>region</i>:<i>account-id</i>:scalingPolicy:<i>policy-id</i>
         *     :autoScalingGroupName/<i>group-friendly-name</i>:policyName/<i>policy-friendly-name</i></code>
         *   </li>
         * </ul>
         *
         * <p><b>Lambda actions:</b>
         *
         * <ul>
         *   <li>
         *     Invoke the latest version of a Lambda function: <code>arn:aws:lambda:<i>region</i>:<i>account-id</i>
         *     :function:<i>function-name</i></code>
         *   </li>
         *   <li>
         *     Invoke a specific version of a Lambda function: <code>arn:aws:lambda:<i>region</i>:<i>account-id</i>
         *     :function:<i>function-name</i>:<i>version-number</i></code>
         *   </li>
         *   <li>
         *     Invoke a function by using an alias Lambda function: <code>arn:aws:lambda:<i>region</i>:<i>account-id</i>
         *     :function:<i>function-name</i>:<i>alias-name</i></code>
         *   </li>
         * </ul>
         *
         * <p><b>SNS notification action:</b>
         *
         * <ul>
         *   <li>
         *     <code>arn:aws:sns:<i>region</i>:<i>account-id</i>:<i>sns-topic-name</i></code>
         *   </li>
         * </ul>
         *
         * <p><b>SSM integration actions:</b>
         *
         * <ul>
         *   <li>
         *     <code>arn:aws:ssm:<i>region</i>:<i>account-id</i>:opsitem:<i>severity</i>#CATEGORY=<i>category-name</i></code>
         *   </li>
         *   <li>
         *     <code>arn:aws:ssm-incidents::<i>account-id</i>:responseplan/<i>response-plan-name</i></code>
         *   </li>
         * </ul>
         *
         * <p><b>Start a Amazon Q Developer operational investigation</b>
         *
         * <p><code>arn:aws:aiops:<i>region</i>:<i>account-id</i>:investigation-group:<i>investigation-group-id</i></code>
         *
         * @return this builder.
         */
        public Builder alarmActions(List<String> alarmActions) {
            this.alarmActions = alarmActions;
            return this;
        }

        /**
         * The actions to execute when this alarm transitions to the <code>INSUFFICIENT_DATA</code> state from any other
         * state. Each action is specified as an Amazon Resource Name (ARN). Valid values:
         *
         * <p><b>EC2 actions:</b>
         *
         * <ul>
         *   <li>
         *     <code>arn:aws:automate:<i>region</i>:ec2:stop</code>
         *   </li>
         *   <li>
         *     <code>arn:aws:automate:<i>region</i>:ec2:terminate</code>
         *   </li>
         *   <li>
         *     <code>arn:aws:automate:<i>region</i>:ec2:reboot</code>
         *   </li>
         *   <li>
         *     <code>arn:aws:automate:<i>region</i>:ec2:recover</code>
         *   </li>
         *   <li>
         *     <code>arn:aws:swf:<i>region</i>:<i>account-id</i>:action/actions/AWS_EC2.InstanceId.Stop/1.0</code>
         *   </li>
         *   <li>
         *     <code>arn:aws:swf:<i>region</i>:<i>account-id</i>:action/actions/AWS_EC2.InstanceId.Terminate/1.0</code>
         *   </li>
         *   <li>
         *     <code>arn:aws:swf:<i>region</i>:<i>account-id</i>:action/actions/AWS_EC2.InstanceId.Reboot/1.0</code>
         *   </li>
         *   <li>
         *     <code>arn:aws:swf:<i>region</i>:<i>account-id</i>:action/actions/AWS_EC2.InstanceId.Recover/1.0</code>
         *   </li>
         * </ul>
         *
         * <p><b>Autoscaling action:</b>
         *
         * <ul>
         *   <li>
         *     <code>arn:aws:autoscaling:<i>region</i>:<i>account-id</i>:scalingPolicy:<i>policy-id</i>
         *     :autoScalingGroupName/<i>group-friendly-name</i>:policyName/<i>policy-friendly-name</i></code>
         *   </li>
         * </ul>
         *
         * <p><b>Lambda actions:</b>
         *
         * <ul>
         *   <li>
         *     Invoke the latest version of a Lambda function: <code>arn:aws:lambda:<i>region</i>:<i>account-id</i>
         *     :function:<i>function-name</i></code>
         *   </li>
         *   <li>
         *     Invoke a specific version of a Lambda function: <code>arn:aws:lambda:<i>region</i>:<i>account-id</i>
         *     :function:<i>function-name</i>:<i>version-number</i></code>
         *   </li>
         *   <li>
         *     Invoke a function by using an alias Lambda function: <code>arn:aws:lambda:<i>region</i>:<i>account-id</i>
         *     :function:<i>function-name</i>:<i>alias-name</i></code>
         *   </li>
         * </ul>
         *
         * <p><b>SNS notification action:</b>
         *
         * <ul>
         *   <li>
         *     <code>arn:aws:sns:<i>region</i>:<i>account-id</i>:<i>sns-topic-name</i></code>
         *   </li>
         * </ul>
         *
         * <p><b>SSM integration actions:</b>
         *
         * <ul>
         *   <li>
         *     <code>arn:aws:ssm:<i>region</i>:<i>account-id</i>:opsitem:<i>severity</i>#CATEGORY=<i>category-name</i></code>
         *   </li>
         *   <li>
         *     <code>arn:aws:ssm-incidents::<i>account-id</i>:responseplan/<i>response-plan-name</i></code>
         *   </li>
         * </ul>
         *
         * @return this builder.
         */
        public Builder insufficientDataActions(List<String> insufficientDataActions) {
            this.insufficientDataActions = insufficientDataActions;
            return this;
        }

        /**
         * The name for the metric associated with the alarm. For each <code>PutMetricAlarm</code> operation, you must
         * specify either <code>MetricName</code>, a <code>Metrics</code> array, or an <code>EvaluationCriteria</code>.
         *
         * <p>If you are creating an alarm based on a math expression, you cannot specify this parameter, or any of the <code>
         * Namespace</code>, <code>Dimensions</code>, <code>Period</code>, <code>Unit</code>, <code>Statistic</code>, or <code>
         * ExtendedStatistic</code> parameters. Instead, you specify all this information in the <code>Metrics</code> array.
         *
         * @return this builder.
         */
        public Builder metricName(String metricName) {
            this.metricName = metricName;
            return this;
        }

        /**
         * The namespace for the metric associated specified in <code>MetricName</code>.
         *
         * @return this builder.
         */
        public Builder namespace(String namespace) {
            this.namespace = namespace;
            return this;
        }

        /**
         * The statistic for the metric specified in <code>MetricName</code>, other than percentile. For percentile
         * statistics, use <code>ExtendedStatistic</code>. When you call <code>PutMetricAlarm</code> and specify a
         * <code>MetricName</code>, you must specify either <code>Statistic</code> or <code>ExtendedStatistic,</code> but
         * not both.
         *
         * @return this builder.
         */
        public Builder statistic(Statistic statistic) {
            this.statistic = statistic;
            return this;
        }

        /**
         * The extended statistic for the metric specified in <code>MetricName</code>. When you call <code>PutMetricAlarm</code>
         * and specify a <code>MetricName</code>, you must specify either <code>Statistic</code> or <code>ExtendedStatistic</code>
         * but not both.
         *
         * <p>If you specify <code>ExtendedStatistic</code>, the following are valid values:
         *
         * <ul>
         *   <li>
         *     <code>p90</code>
         *   </li>
         *   <li>
         *     <code>tm90</code>
         *   </li>
         *   <li>
         *     <code>tc90</code>
         *   </li>
         *   <li>
         *     <code>ts90</code>
         *   </li>
         *   <li>
         *     <code>wm90</code>
         *   </li>
         *   <li>
         *     <code>IQM</code>
         *   </li>
         *   <li>
         *     <code>PR(<i>n</i>:<i>m</i>)</code> where n and m are values of the metric
         *   </li>
         *   <li>
         *     <code>TC(<i>X</i>%:<i>X</i>%)</code> where X is between 10 and 90 inclusive.
         *   </li>
         *   <li>
         *     <code>TM(<i>X</i>%:<i>X</i>%)</code> where X is between 10 and 90 inclusive.
         *   </li>
         *   <li>
         *     <code>TS(<i>X</i>%:<i>X</i>%)</code> where X is between 10 and 90 inclusive.
         *   </li>
         *   <li>
         *     <code>WM(<i>X</i>%:<i>X</i>%)</code> where X is between 10 and 90 inclusive.
         *   </li>
         * </ul>
         *
         * <p>For more information about these extended statistics, see <a href="https://docs.aws.amazon.com/AmazonCloudWatch/latest/monitoring/Statistics-definitions.html">CloudWatch statistics definitions</a>.
         *
         * @return this builder.
         */
        public Builder extendedStatistic(String extendedStatistic) {
            this.extendedStatistic = extendedStatistic;
            return this;
        }

        /**
         * The dimensions for the metric specified in <code>MetricName</code>.
         *
         * @return this builder.
         */
        public Builder dimensions(List<Dimension> dimensions) {
            this.dimensions = dimensions;
            return this;
        }

        /**
         * The length, in seconds, used each time the metric specified in <code>MetricName</code> is evaluated. Valid values
         * are 10, 20, 30, and any multiple of 60.
         *
         * <p><code>Period</code> is required for alarms based on static thresholds. If you are creating an alarm based on a
         * metric math expression, you specify the period for each metric within the objects in the <code>Metrics</code>
         * array.
         *
         * <p>Be sure to specify 10, 20, or 30 only for metrics that are stored by a <code>PutMetricData</code> call with a <code>
         * StorageResolution</code> of 1. If you specify a period of 10, 20, or 30 for a metric that does not have
         * sub-minute resolution, the alarm still attempts to gather data at the period rate that you specify. In this case,
         * it does not receive data for the attempts that do not correspond to a one-minute data resolution, and the alarm
         * might often lapse into INSUFFICENT_DATA status. Specifying 10, 20, or 30 also sets this alarm as a
         * high-resolution alarm, which has a higher charge than other alarms. For more information about pricing, see <a href="https://aws.amazon.com/cloudwatch/pricing/">
         * Amazon CloudWatch Pricing</a>.
         *
         * <p>An alarm's total current evaluation period can be no longer than seven days, so <code>Period</code> multiplied
         * by <code>EvaluationPeriods</code> can't be more than 604,800 seconds. For alarms with a period of less than one
         * hour (3,600 seconds), the total evaluation period can't be longer than one day (86,400 seconds).
         *
         * @return this builder.
         */
        public Builder period(Integer period) {
            this.period = period;
            return this;
        }

        /**
         * The unit of measure for the statistic. For example, the units for the Amazon EC2 NetworkIn metric are Bytes
         * because NetworkIn tracks the number of bytes that an instance receives on all network interfaces. You can also
         * specify a unit when you create a custom metric. Units help provide conceptual meaning to your data. Metric data
         * points that specify a unit of measure, such as Percent, are aggregated separately. If you are creating an alarm
         * based on a metric math expression, you can specify the unit for each metric (if needed) within the objects in the
         * <code>Metrics</code> array.
         *
         * <p>If you don't specify <code>Unit</code>, CloudWatch retrieves all unit types that have been published for the
         * metric and attempts to evaluate the alarm. Usually, metrics are published with only one unit, so the alarm works
         * as intended.
         *
         * <p>However, if the metric is published with multiple types of units and you don't specify a unit, the alarm's
         * behavior is not defined and it behaves unpredictably.
         *
         * <p>We recommend omitting <code>Unit</code> so that you don't inadvertently specify an incorrect unit that is not
         * published for this metric. Doing so causes the alarm to be stuck in the <code>INSUFFICIENT DATA</code> state.
         *
         * @return this builder.
         */
        public Builder unit(StandardUnit unit) {
            this.unit = unit;
            return this;
        }

        /**
         * The number of periods over which data is compared to the specified threshold. If you are setting an alarm that
         * requires that a number of consecutive data points be breaching to trigger the alarm, this value specifies that
         * number. If you are setting an "M out of N" alarm, this value is the N.
         *
         * @return this builder.
         */
        public Builder evaluationPeriods(Integer evaluationPeriods) {
            this.evaluationPeriods = evaluationPeriods;
            return this;
        }

        /**
         * The number of data points that must be breaching to trigger the alarm. This is used only if you are setting an "M
         * out of N" alarm. In that case, this value is the M. For more information, see <a href="https://docs.aws.amazon.com/AmazonCloudWatch/latest/monitoring/AlarmThatSendsEmail.html#alarm-evaluation">Evaluating an Alarm</a> in the <i>
         * Amazon CloudWatch User Guide</i>.
         *
         * @return this builder.
         */
        public Builder datapointsToAlarm(Integer datapointsToAlarm) {
            this.datapointsToAlarm = datapointsToAlarm;
            return this;
        }

        /**
         * The value against which the specified statistic is compared.
         *
         * <p>This parameter is required for alarms based on static thresholds, but should not be used for alarms based on
         * anomaly detection models.
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
         * <p>The values <code>LessThanLowerOrGreaterThanUpperThreshold</code>, <code>LessThanLowerThreshold</code>, and <code>
         * GreaterThanUpperThreshold</code> are used only for alarms based on anomaly detection models.
         *
         * @return this builder.
         */
        public Builder comparisonOperator(ComparisonOperator comparisonOperator) {
            this.comparisonOperator = comparisonOperator;
            return this;
        }

        /**
         * Sets how this alarm is to handle missing data points. If <code>TreatMissingData</code> is omitted, the default
         * behavior of <code>missing</code> is used. For more information, see <a href="https://docs.aws.amazon.com/AmazonCloudWatch/latest/monitoring/AlarmThatSendsEmail.html#alarms-and-missing-data">Configuring How CloudWatch Alarms Treats
         * Missing Data</a>.
         *
         * <p>Valid Values: <code>breaching | notBreaching | ignore | missing</code>
         *
         * <p>Alarms that evaluate metrics in the <code>AWS/DynamoDB</code> namespace always <code>ignore</code> missing
         * data even if you choose a different option for <code>TreatMissingData</code>. When an <code>AWS/DynamoDB</code>
         * metric has missing data, alarms that evaluate that metric remain in their current state.
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
         * Used only for alarms based on percentiles. If you specify <code>ignore</code>, the alarm state does not change
         * during periods with too few data points to be statistically significant. If you specify <code>evaluate</code> or
         * omit this parameter, the alarm is always evaluated and possibly changes state no matter how many data points are
         * available. For more information, see <a href="https://docs.aws.amazon.com/AmazonCloudWatch/latest/monitoring/AlarmThatSendsEmail.html#percentiles-with-low-samples">Percentile-Based CloudWatch Alarms and Low Data Samples</a>.
         *
         * <p>Valid Values: <code>evaluate | ignore</code>
         *
         * @return this builder.
         */
        public Builder evaluateLowSampleCountPercentile(String evaluateLowSampleCountPercentile) {
            this.evaluateLowSampleCountPercentile = evaluateLowSampleCountPercentile;
            return this;
        }

        /**
         * An array of <code>MetricDataQuery</code> structures that enable you to create an alarm based on the result of a
         * metric math expression. For each <code>PutMetricAlarm</code> operation, you must specify either <code>MetricName</code>
         * , a <code>Metrics</code> array, or an <code>EvaluationCriteria</code>.
         *
         * <p>Each item in the <code>Metrics</code> array either retrieves a metric or performs a math expression.
         *
         * <p>One item in the <code>Metrics</code> array is the expression that the alarm watches. You designate this
         * expression by setting <code>ReturnData</code> to true for this object in the array. For more information, see <a href="https://docs.aws.amazon.com/AmazonCloudWatch/latest/APIReference/API_MetricDataQuery.html">
         * MetricDataQuery</a>.
         *
         * <p>If you use the <code>Metrics</code> parameter, you cannot include the <code>Namespace</code>, <code>MetricName</code>
         * , <code>Dimensions</code>, <code>Period</code>, <code>Unit</code>, <code>Statistic</code>, or <code>
         * ExtendedStatistic</code> parameters of <code>PutMetricAlarm</code> in the same operation. Instead, you retrieve
         * the metrics you are using in your math expression as part of the <code>Metrics</code> array.
         *
         * @return this builder.
         */
        public Builder metrics(List<MetricDataQuery> metrics) {
            this.metrics = metrics;
            return this;
        }

        /**
         * A list of key-value pairs to associate with the alarm. You can associate as many as 50 tags with an alarm. To be
         * able to associate tags with the alarm when you create the alarm, you must have the <code>cloudwatch:TagResource</code>
         * permission.
         *
         * <p>Tags can help you organize and categorize your resources. You can also use them to scope user permissions by
         * granting a user permission to access or change only resources with certain tag values.
         *
         * <p>If you are using this operation to update an existing alarm, any tags you specify in this parameter are
         * ignored. To change the tags of an existing alarm, use <a href="https://docs.aws.amazon.com/AmazonCloudWatch/latest/APIReference/API_TagResource.html">TagResource</a> or <a href="https://docs.aws.amazon.com/AmazonCloudWatch/latest/APIReference/API_UntagResource.html">UntagResource</a>.
         *
         * <p>To use this field to set tags for an alarm when you create it, you must be signed on with both the <code>
         * cloudwatch:PutMetricAlarm</code> and <code>cloudwatch:TagResource</code> permissions.
         *
         * @return this builder.
         */
        public Builder tags(List<Tag> tags) {
            this.tags = tags;
            return this;
        }

        /**
         * If this is an alarm based on an anomaly detection model, make this value match the ID of the <code>
         * ANOMALY_DETECTION_BAND</code> function.
         *
         * <p>For an example of how to use this parameter, see the <b>Anomaly Detection Model Alarm</b> example on this
         * page.
         *
         * <p>If your alarm uses this parameter, it cannot have Auto Scaling actions.
         *
         * @return this builder.
         */
        public Builder thresholdMetricId(String thresholdMetricId) {
            this.thresholdMetricId = thresholdMetricId;
            return this;
        }

        /**
         * The evaluation window that the alarm uses to select the range of metric data that it evaluates. Specify either a
         * sliding window or a wall clock window. If you omit this parameter, the alarm uses a sliding window.
         *
         * <p>A sliding window advances each time the alarm is evaluated, forming a rolling time window. A wall clock window
         * aligns the evaluated range to fixed clock boundaries, such as the top of the hour or the start of the day.
         *
         * <p>You can use <code>EvaluationWindow</code> with any type of metric alarm except alarms that are based on a
         * PromQL query.
         *
         * <p>For more information, see <a href="https://docs.aws.amazon.com/AmazonCloudWatch/latest/monitoring/alarm-evaluation-window.html">Alarm evaluation windows</a> in the <i>CloudWatch User Guide</i>.
         *
         * @return this builder.
         */
        public Builder evaluationWindow(EvaluationWindow evaluationWindow) {
            this.evaluationWindow = evaluationWindow;
            return this;
        }

        /**
         * The warm-up configuration for the alarm. A warm-up period delays alarm evaluation after you create or update the
         * alarm. The warm-up period reduces alarm noise from missing data while a new resource or service starts publishing
         * metrics.
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
         * The evaluation criteria for the alarm. For each <code>PutMetricAlarm</code> operation, you must specify either <code>
         * MetricName</code>, a <code>Metrics</code> array, or an <code>EvaluationCriteria</code>.
         *
         * <p>If you use the <code>EvaluationCriteria</code> parameter, you cannot include the <code>Namespace</code>, <code>
         * MetricName</code>, <code>Dimensions</code>, <code>Period</code>, <code>Unit</code>, <code>Statistic</code>, <code>
         * ExtendedStatistic</code>, <code>Metrics</code>, <code>Threshold</code>, <code>ComparisonOperator</code>, <code>
         * ThresholdMetricId</code>, <code>EvaluationPeriods</code>, or <code>DatapointsToAlarm</code> parameters of <code>
         * PutMetricAlarm</code> in the same operation. Instead, all evaluation parameters are defined within this
         * structure.
         *
         * <p>For an example of how to use this parameter, see the <b>PromQL alarm</b> example on this page.
         *
         * @return this builder.
         */
        public Builder evaluationCriteria(EvaluationCriteria evaluationCriteria) {
            this.evaluationCriteria = evaluationCriteria;
            return this;
        }

        /**
         * The frequency, in seconds, at which the alarm is evaluated. Valid values are 10, 20, 30, and any multiple of 60.
         *
         * <p>This parameter is required for alarms that use <code>EvaluationCriteria</code>, and cannot be specified for
         * alarms configured with <code>MetricName</code> or <code>Metrics</code>.
         *
         * @return this builder.
         */
        public Builder evaluationInterval(Integer evaluationInterval) {
            this.evaluationInterval = evaluationInterval;
            return this;
        }

        @Override
        public PutMetricAlarmInput build() {
            tracker.validate();
            return new PutMetricAlarmInput(this);
        }

        @Override
        @SuppressWarnings("unchecked")
        public void setMemberValue(Schema member, Object value) {
            switch (member.memberIndex()) {
                case 0 -> alarmName((String) SchemaUtils.validateSameMember($SCHEMA_ALARM_NAME, member, value));
                case 1 -> alarmDescription((String) SchemaUtils.validateSameMember($SCHEMA_ALARM_DESCRIPTION, member, value));
                case 2 -> actionsEnabled((Boolean) SchemaUtils.validateSameMember($SCHEMA_ACTIONS_ENABLED, member, value));
                case 3 -> okActions((List<String>) SchemaUtils.validateSameMember($SCHEMA_OK_ACTIONS, member, value));
                case 4 -> alarmActions((List<String>) SchemaUtils.validateSameMember($SCHEMA_ALARM_ACTIONS, member, value));
                case 5 -> insufficientDataActions((List<String>) SchemaUtils.validateSameMember($SCHEMA_INSUFFICIENT_DATA_ACTIONS, member, value));
                case 6 -> metricName((String) SchemaUtils.validateSameMember($SCHEMA_METRIC_NAME, member, value));
                case 7 -> namespace((String) SchemaUtils.validateSameMember($SCHEMA_NAMESPACE, member, value));
                case 8 -> statistic((Statistic) SchemaUtils.validateSameMember($SCHEMA_STATISTIC, member, value));
                case 9 -> extendedStatistic((String) SchemaUtils.validateSameMember($SCHEMA_EXTENDED_STATISTIC, member, value));
                case 10 -> dimensions((List<Dimension>) SchemaUtils.validateSameMember($SCHEMA_DIMENSIONS, member, value));
                case 11 -> period((Integer) SchemaUtils.validateSameMember($SCHEMA_PERIOD, member, value));
                case 12 -> unit((StandardUnit) SchemaUtils.validateSameMember($SCHEMA_UNIT, member, value));
                case 13 -> evaluationPeriods((Integer) SchemaUtils.validateSameMember($SCHEMA_EVALUATION_PERIODS, member, value));
                case 14 -> datapointsToAlarm((Integer) SchemaUtils.validateSameMember($SCHEMA_DATAPOINTS_TO_ALARM, member, value));
                case 15 -> threshold((Double) SchemaUtils.validateSameMember($SCHEMA_THRESHOLD, member, value));
                case 16 -> comparisonOperator((ComparisonOperator) SchemaUtils.validateSameMember($SCHEMA_COMPARISON_OPERATOR, member, value));
                case 17 -> treatMissingData((String) SchemaUtils.validateSameMember($SCHEMA_TREAT_MISSING_DATA, member, value));
                case 18 -> evaluateLowSampleCountPercentile((String) SchemaUtils.validateSameMember($SCHEMA_EVALUATE_LOW_SAMPLE_COUNT_PERCENTILE, member, value));
                case 19 -> metrics((List<MetricDataQuery>) SchemaUtils.validateSameMember($SCHEMA_METRICS, member, value));
                case 20 -> tags((List<Tag>) SchemaUtils.validateSameMember($SCHEMA_TAGS, member, value));
                case 21 -> thresholdMetricId((String) SchemaUtils.validateSameMember($SCHEMA_THRESHOLD_METRIC_ID, member, value));
                case 22 -> evaluationWindow((EvaluationWindow) SchemaUtils.validateSameMember($SCHEMA_EVALUATION_WINDOW, member, value));
                case 23 -> warmUpConfiguration((WarmUpConfiguration) SchemaUtils.validateSameMember($SCHEMA_WARM_UP_CONFIGURATION, member, value));
                case 24 -> evaluationCriteria((EvaluationCriteria) SchemaUtils.validateSameMember($SCHEMA_EVALUATION_CRITERIA, member, value));
                case 25 -> evaluationInterval((Integer) SchemaUtils.validateSameMember($SCHEMA_EVALUATION_INTERVAL, member, value));
                default -> ShapeBuilder.super.setMemberValue(member, value);
            }
        }

        @Override
        public ShapeBuilder<PutMetricAlarmInput> errorCorrection() {
            if (tracker.allSet()) {
                return this;
            }
            if (!tracker.checkMember($SCHEMA_ALARM_NAME)) {
                alarmName("");
            }
            return this;
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
                    case 1 -> builder.alarmDescription(de.readString(member));
                    case 2 -> builder.actionsEnabled(de.readBoolean(member));
                    case 3 -> builder.okActions(SharedSerde.deserializeResourceList(member, de));
                    case 4 -> builder.alarmActions(SharedSerde.deserializeResourceList(member, de));
                    case 5 -> builder.insufficientDataActions(SharedSerde.deserializeResourceList(member, de));
                    case 6 -> builder.metricName(de.readString(member));
                    case 7 -> builder.namespace(de.readString(member));
                    case 8 -> builder.statistic(Statistic.builder().deserializeMember(de, member).build());
                    case 9 -> builder.extendedStatistic(de.readString(member));
                    case 10 -> builder.dimensions(SharedSerde.deserializeDimensions(member, de));
                    case 11 -> builder.period(de.readInteger(member));
                    case 12 -> builder.unit(StandardUnit.builder().deserializeMember(de, member).build());
                    case 13 -> builder.evaluationPeriods(de.readInteger(member));
                    case 14 -> builder.datapointsToAlarm(de.readInteger(member));
                    case 15 -> builder.threshold(de.readDouble(member));
                    case 16 -> builder.comparisonOperator(ComparisonOperator.builder().deserializeMember(de, member).build());
                    case 17 -> builder.treatMissingData(de.readString(member));
                    case 18 -> builder.evaluateLowSampleCountPercentile(de.readString(member));
                    case 19 -> builder.metrics(SharedSerde.deserializeMetricDataQueries(member, de));
                    case 20 -> builder.tags(SharedSerde.deserializeTagList(member, de));
                    case 21 -> builder.thresholdMetricId(de.readString(member));
                    case 22 -> builder.evaluationWindow(EvaluationWindow.builder().deserializeMember(de, member).build());
                    case 23 -> builder.warmUpConfiguration(WarmUpConfiguration.builder().deserializeMember(de, member).build());
                    case 24 -> builder.evaluationCriteria(EvaluationCriteria.builder().deserializeMember(de, member).build());
                    case 25 -> builder.evaluationInterval(de.readInteger(member));
                    default -> throw new IllegalArgumentException("Unexpected member: " + member.memberName());
                }
            }
        }
    }
}
