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
public final class PutLogAlarmInput implements SerializableStruct {

    public static final Schema $SCHEMA = Schemas.PUT_LOG_ALARM_INPUT;
    private static final Schema $SCHEMA_ALARM_NAME = $SCHEMA.member("AlarmName");
    private static final Schema $SCHEMA_ALARM_DESCRIPTION = $SCHEMA.member("AlarmDescription");
    private static final Schema $SCHEMA_SCHEDULED_QUERY_CONFIGURATION = $SCHEMA.member("ScheduledQueryConfiguration");
    private static final Schema $SCHEMA_ACTION_LOG_LINE_COUNT = $SCHEMA.member("ActionLogLineCount");
    private static final Schema $SCHEMA_ACTION_LOG_LINE_ROLE_ARN = $SCHEMA.member("ActionLogLineRoleArn");
    private static final Schema $SCHEMA_ACTIONS_ENABLED = $SCHEMA.member("ActionsEnabled");
    private static final Schema $SCHEMA_OK_ACTIONS = $SCHEMA.member("OKActions");
    private static final Schema $SCHEMA_ALARM_ACTIONS = $SCHEMA.member("AlarmActions");
    private static final Schema $SCHEMA_INSUFFICIENT_DATA_ACTIONS = $SCHEMA.member("InsufficientDataActions");
    private static final Schema $SCHEMA_QUERY_RESULTS_TO_EVALUATE = $SCHEMA.member("QueryResultsToEvaluate");
    private static final Schema $SCHEMA_QUERY_RESULTS_TO_ALARM = $SCHEMA.member("QueryResultsToAlarm");
    private static final Schema $SCHEMA_THRESHOLD = $SCHEMA.member("Threshold");
    private static final Schema $SCHEMA_COMPARISON_OPERATOR = $SCHEMA.member("ComparisonOperator");
    private static final Schema $SCHEMA_TREAT_MISSING_DATA = $SCHEMA.member("TreatMissingData");
    private static final Schema $SCHEMA_TAGS = $SCHEMA.member("Tags");
    private static final Schema $SCHEMA_WARM_UP_CONFIGURATION = $SCHEMA.member("WarmUpConfiguration");

    public static final ShapeId $ID = $SCHEMA.id();

    private final transient String alarmName;
    private final transient String alarmDescription;
    private final transient ScheduledQueryConfiguration scheduledQueryConfiguration;
    private final transient Integer actionLogLineCount;
    private final transient String actionLogLineRoleArn;
    private final transient Boolean actionsEnabled;
    private final transient List<String> okActions;
    private final transient List<String> alarmActions;
    private final transient List<String> insufficientDataActions;
    private final transient Integer queryResultsToEvaluate;
    private final transient Integer queryResultsToAlarm;
    private final transient Double threshold;
    private final transient ComparisonOperator comparisonOperator;
    private final transient String treatMissingData;
    private final transient List<Tag> tags;
    private final transient WarmUpConfiguration warmUpConfiguration;

    private PutLogAlarmInput(Builder builder) {
        this.alarmName = builder.alarmName;
        this.alarmDescription = builder.alarmDescription;
        this.scheduledQueryConfiguration = builder.scheduledQueryConfiguration;
        this.actionLogLineCount = builder.actionLogLineCount;
        this.actionLogLineRoleArn = builder.actionLogLineRoleArn;
        this.actionsEnabled = builder.actionsEnabled;
        this.okActions = builder.okActions == null ? null : Collections.unmodifiableList(builder.okActions);
        this.alarmActions = builder.alarmActions == null ? null : Collections.unmodifiableList(builder.alarmActions);
        this.insufficientDataActions = builder.insufficientDataActions == null ? null : Collections.unmodifiableList(builder.insufficientDataActions);
        this.queryResultsToEvaluate = builder.queryResultsToEvaluate;
        this.queryResultsToAlarm = builder.queryResultsToAlarm;
        this.threshold = builder.threshold;
        this.comparisonOperator = builder.comparisonOperator;
        this.treatMissingData = builder.treatMissingData;
        this.tags = builder.tags == null ? null : Collections.unmodifiableList(builder.tags);
        this.warmUpConfiguration = builder.warmUpConfiguration;
    }

    /**
     * The name for the alarm. This name must be unique within the Amazon Web Services account and Region.
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
     * The configuration of the underlying CloudWatch Logs scheduled query that this alarm evaluates, including the
     * query string, log groups, schedule, and aggregation expression.
     */
    public ScheduledQueryConfiguration getScheduledQueryConfiguration() {
        return scheduledQueryConfiguration;
    }

    /**
     * The number of log lines from the most recent scheduled query execution to include in alarm action notifications.
     * Valid range is 0 through 50. The default is 0, which means no log lines are included.
     */
    public Integer getActionLogLineCount() {
        return actionLogLineCount;
    }

    /**
     * The Amazon Resource Name (ARN) of an IAM role that CloudWatch assumes to retrieve log events for inclusion in
     * alarm action notifications. Required when <code>ActionLogLineCount</code> is greater than 0.
     */
    public String getActionLogLineRoleArn() {
        return actionLogLineRoleArn;
    }

    /**
     * Indicates whether actions should be executed during any changes to the alarm state. The default is
     * <code>true</code>.
     */
    public Boolean isActionsEnabled() {
        return actionsEnabled;
    }

    /**
     * The actions to execute when this alarm transitions to the <code>OK</code> state from any other state. Each action
     * is specified as an Amazon Resource Name (ARN).
     *
     * <p>Valid Values:
     *
     * <p><b>Amazon SNS actions:</b>
     *
     * <p><code>arn:aws:sns:<i>region</i>:<i>account-id</i>:<i>sns-topic-name</i></code>
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
     *
     * <p>Valid Values:
     *
     * <p><b>Amazon SNS actions:</b>
     *
     * <p><code>arn:aws:sns:<i>region</i>:<i>account-id</i>:<i>sns-topic-name</i></code>
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
     * <p><b>Systems Manager actions:</b>
     *
     * <p><code>arn:aws:ssm:<i>region</i>:<i>account-id</i>:opsitem:<i>severity</i></code>
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
     *
     * <p>Valid Values:
     *
     * <p><b>Amazon SNS actions:</b>
     *
     * <p><code>arn:aws:sns:<i>region</i>:<i>account-id</i>:<i>sns-topic-name</i></code>
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
     * The number of most recent scheduled query results to evaluate against the threshold (the N in M-of-N evaluation).
     * Valid range is 1 through 100.
     */
    public Integer getQueryResultsToEvaluate() {
        return queryResultsToEvaluate;
    }

    /**
     * The number of query results, out of the most recent <code>QueryResultsToEvaluate</code> results, that must breach
     * the threshold to trigger the alarm to transition to <code>ALARM</code> (the M in M-of-N evaluation). Must be less
     * than or equal to <code>QueryResultsToEvaluate</code>.
     */
    public Integer getQueryResultsToAlarm() {
        return queryResultsToAlarm;
    }

    /**
     * The value to compare with the aggregated query result.
     */
    public Double getThreshold() {
        return threshold;
    }

    /**
     * The arithmetic operation to use when comparing the aggregated query result and the threshold. The aggregated
     * query result is used as the first operand. Valid values are <code>GreaterThanThreshold</code>,
     * <code>GreaterThanOrEqualToThreshold</code>, <code>LessThanThreshold</code>, and
     * <code>LessThanOrEqualToThreshold</code>.
     */
    public ComparisonOperator getComparisonOperator() {
        return comparisonOperator;
    }

    /**
     * Sets how this alarm is to handle missing data points. Valid values are <code>breaching</code>,
     * <code>notBreaching</code>, <code>ignore</code>, and <code>missing</code>. If this parameter is omitted, the
     * default behavior of <code>missing</code> is used.
     */
    public String getTreatMissingData() {
        return treatMissingData;
    }

    /**
     * A list of key-value pairs to associate with the alarm. You can use tags to categorize and manage your alarms.
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
     * The warm-up configuration for the alarm. A warm-up period delays alarm evaluation after you create or update the
     * alarm. The warm-up period reduces alarm noise from missing data while a new resource or service starts publishing
     * data.
     *
     * <p>For more information, see <a href="https://docs.aws.amazon.com/AmazonCloudWatch/latest/monitoring/alarm-warm-up.html">Alarm warm-up periods</a> in the <i>Amazon CloudWatch User Guide</i>.
     */
    public WarmUpConfiguration getWarmUpConfiguration() {
        return warmUpConfiguration;
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
        PutLogAlarmInput that = (PutLogAlarmInput) other;
        return Objects.equals(this.actionsEnabled, that.actionsEnabled)
               && Objects.equals(this.actionLogLineCount, that.actionLogLineCount)
               && Objects.equals(this.queryResultsToEvaluate, that.queryResultsToEvaluate)
               && Objects.equals(this.queryResultsToAlarm, that.queryResultsToAlarm)
               && Objects.equals(this.threshold, that.threshold)
               && Objects.equals(this.alarmName, that.alarmName)
               && Objects.equals(this.alarmDescription, that.alarmDescription)
               && Objects.equals(this.actionLogLineRoleArn, that.actionLogLineRoleArn)
               && Objects.equals(this.treatMissingData, that.treatMissingData)
               && Objects.equals(this.comparisonOperator, that.comparisonOperator)
               && Objects.equals(this.scheduledQueryConfiguration, that.scheduledQueryConfiguration)
               && Objects.equals(this.warmUpConfiguration, that.warmUpConfiguration)
               && Objects.equals(this.okActions, that.okActions)
               && Objects.equals(this.alarmActions, that.alarmActions)
               && Objects.equals(this.insufficientDataActions, that.insufficientDataActions)
               && Objects.equals(this.tags, that.tags);
    }

    @Override
    public int hashCode() {
        int $hc = Objects.hashCode(alarmName);
        $hc = 31 * $hc + Objects.hashCode(alarmDescription);
        $hc = 31 * $hc + Objects.hashCode(scheduledQueryConfiguration);
        $hc = 31 * $hc + Objects.hashCode(actionLogLineCount);
        $hc = 31 * $hc + Objects.hashCode(actionLogLineRoleArn);
        $hc = 31 * $hc + Objects.hashCode(actionsEnabled);
        $hc = 31 * $hc + Objects.hashCode(okActions);
        $hc = 31 * $hc + Objects.hashCode(alarmActions);
        $hc = 31 * $hc + Objects.hashCode(insufficientDataActions);
        $hc = 31 * $hc + Objects.hashCode(queryResultsToEvaluate);
        $hc = 31 * $hc + Objects.hashCode(queryResultsToAlarm);
        $hc = 31 * $hc + Objects.hashCode(threshold);
        $hc = 31 * $hc + Objects.hashCode(comparisonOperator);
        $hc = 31 * $hc + Objects.hashCode(treatMissingData);
        $hc = 31 * $hc + Objects.hashCode(tags);
        $hc = 31 * $hc + Objects.hashCode(warmUpConfiguration);
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
        if (scheduledQueryConfiguration != null) {
            serializer.writeStruct($SCHEMA_SCHEDULED_QUERY_CONFIGURATION, scheduledQueryConfiguration);
        }
        if (actionLogLineCount != null) {
            serializer.writeInteger($SCHEMA_ACTION_LOG_LINE_COUNT, actionLogLineCount);
        }
        if (actionLogLineRoleArn != null) {
            serializer.writeString($SCHEMA_ACTION_LOG_LINE_ROLE_ARN, actionLogLineRoleArn);
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
        if (queryResultsToEvaluate != null) {
            serializer.writeInteger($SCHEMA_QUERY_RESULTS_TO_EVALUATE, queryResultsToEvaluate);
        }
        if (queryResultsToAlarm != null) {
            serializer.writeInteger($SCHEMA_QUERY_RESULTS_TO_ALARM, queryResultsToAlarm);
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
        if (tags != null) {
            serializer.writeList($SCHEMA_TAGS, tags, tags.size(), SharedSerde.TagListSerializer.INSTANCE);
        }
        if (warmUpConfiguration != null) {
            serializer.writeStruct($SCHEMA_WARM_UP_CONFIGURATION, warmUpConfiguration);
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T getMemberValue(Schema member) {
        return switch (member.memberIndex()) {
            case 0 -> (T) SchemaUtils.validateSameMember($SCHEMA_ALARM_NAME, member, alarmName);
            case 1 -> (T) SchemaUtils.validateSameMember($SCHEMA_SCHEDULED_QUERY_CONFIGURATION, member, scheduledQueryConfiguration);
            case 2 -> (T) SchemaUtils.validateSameMember($SCHEMA_QUERY_RESULTS_TO_EVALUATE, member, queryResultsToEvaluate);
            case 3 -> (T) SchemaUtils.validateSameMember($SCHEMA_QUERY_RESULTS_TO_ALARM, member, queryResultsToAlarm);
            case 4 -> (T) SchemaUtils.validateSameMember($SCHEMA_THRESHOLD, member, threshold);
            case 5 -> (T) SchemaUtils.validateSameMember($SCHEMA_COMPARISON_OPERATOR, member, comparisonOperator);
            case 6 -> (T) SchemaUtils.validateSameMember($SCHEMA_ALARM_DESCRIPTION, member, alarmDescription);
            case 7 -> (T) SchemaUtils.validateSameMember($SCHEMA_ACTION_LOG_LINE_COUNT, member, actionLogLineCount);
            case 8 -> (T) SchemaUtils.validateSameMember($SCHEMA_ACTION_LOG_LINE_ROLE_ARN, member, actionLogLineRoleArn);
            case 9 -> (T) SchemaUtils.validateSameMember($SCHEMA_ACTIONS_ENABLED, member, actionsEnabled);
            case 10 -> (T) SchemaUtils.validateSameMember($SCHEMA_OK_ACTIONS, member, okActions);
            case 11 -> (T) SchemaUtils.validateSameMember($SCHEMA_ALARM_ACTIONS, member, alarmActions);
            case 12 -> (T) SchemaUtils.validateSameMember($SCHEMA_INSUFFICIENT_DATA_ACTIONS, member, insufficientDataActions);
            case 13 -> (T) SchemaUtils.validateSameMember($SCHEMA_TREAT_MISSING_DATA, member, treatMissingData);
            case 14 -> (T) SchemaUtils.validateSameMember($SCHEMA_TAGS, member, tags);
            case 15 -> (T) SchemaUtils.validateSameMember($SCHEMA_WARM_UP_CONFIGURATION, member, warmUpConfiguration);
            default -> throw new IllegalArgumentException("Attempted to get non-existent member: " + member.id());
        };
    }

    /**
     * Create a new builder containing all the current property values of this object.
     *
     * <p><strong>Note:</strong> This method performs only a shallow copy of the original properties.
     *
     * @return a builder for {@link PutLogAlarmInput}.
     */
    public Builder toBuilder() {
        var builder = new Builder();
        builder.alarmName(this.alarmName);
        builder.alarmDescription(this.alarmDescription);
        builder.scheduledQueryConfiguration(this.scheduledQueryConfiguration);
        builder.actionLogLineCount(this.actionLogLineCount);
        builder.actionLogLineRoleArn(this.actionLogLineRoleArn);
        builder.actionsEnabled(this.actionsEnabled);
        builder.okActions(this.okActions);
        builder.alarmActions(this.alarmActions);
        builder.insufficientDataActions(this.insufficientDataActions);
        builder.queryResultsToEvaluate(this.queryResultsToEvaluate);
        builder.queryResultsToAlarm(this.queryResultsToAlarm);
        builder.threshold(this.threshold);
        builder.comparisonOperator(this.comparisonOperator);
        builder.treatMissingData(this.treatMissingData);
        builder.tags(this.tags);
        builder.warmUpConfiguration(this.warmUpConfiguration);
        return builder;
    }

    /**
     * @return returns a new Builder.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for {@link PutLogAlarmInput}.
     */
    public static final class Builder implements ShapeBuilder<PutLogAlarmInput> {
        private final PresenceTracker tracker = PresenceTracker.of($SCHEMA);
        private String alarmName;
        private String alarmDescription;
        private ScheduledQueryConfiguration scheduledQueryConfiguration;
        private Integer actionLogLineCount;
        private String actionLogLineRoleArn;
        private Boolean actionsEnabled;
        private List<String> okActions;
        private List<String> alarmActions;
        private List<String> insufficientDataActions;
        private Integer queryResultsToEvaluate;
        private Integer queryResultsToAlarm;
        private Double threshold;
        private ComparisonOperator comparisonOperator;
        private String treatMissingData;
        private List<Tag> tags;
        private WarmUpConfiguration warmUpConfiguration;

        private Builder() {
            // Tell the tracker to assume clientOptional members are present.
            tracker.setMember($SCHEMA_ALARM_NAME);
            tracker.setMember($SCHEMA_SCHEDULED_QUERY_CONFIGURATION);
            tracker.setMember($SCHEMA_QUERY_RESULTS_TO_EVALUATE);
            tracker.setMember($SCHEMA_QUERY_RESULTS_TO_ALARM);
            tracker.setMember($SCHEMA_THRESHOLD);
            tracker.setMember($SCHEMA_COMPARISON_OPERATOR);
        }

        @Override
        public Schema schema() {
            return $SCHEMA;
        }

        /**
         * The name for the alarm. This name must be unique within the Amazon Web Services account and Region.
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
         * The configuration of the underlying CloudWatch Logs scheduled query that this alarm evaluates, including the
         * query string, log groups, schedule, and aggregation expression.
         *
         * <p><strong>Required</strong>
         * @return this builder.
         */
        public Builder scheduledQueryConfiguration(ScheduledQueryConfiguration scheduledQueryConfiguration) {
            this.scheduledQueryConfiguration = Objects.requireNonNull(scheduledQueryConfiguration, "scheduledQueryConfiguration cannot be null");
            tracker.setMember($SCHEMA_SCHEDULED_QUERY_CONFIGURATION);
            return this;
        }

        /**
         * The number of log lines from the most recent scheduled query execution to include in alarm action notifications.
         * Valid range is 0 through 50. The default is 0, which means no log lines are included.
         *
         * @return this builder.
         */
        public Builder actionLogLineCount(Integer actionLogLineCount) {
            this.actionLogLineCount = actionLogLineCount;
            return this;
        }

        /**
         * The Amazon Resource Name (ARN) of an IAM role that CloudWatch assumes to retrieve log events for inclusion in
         * alarm action notifications. Required when <code>ActionLogLineCount</code> is greater than 0.
         *
         * @return this builder.
         */
        public Builder actionLogLineRoleArn(String actionLogLineRoleArn) {
            this.actionLogLineRoleArn = actionLogLineRoleArn;
            return this;
        }

        /**
         * Indicates whether actions should be executed during any changes to the alarm state. The default is
         * <code>true</code>.
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
         * <p>Valid Values:
         *
         * <p><b>Amazon SNS actions:</b>
         *
         * <p><code>arn:aws:sns:<i>region</i>:<i>account-id</i>:<i>sns-topic-name</i></code>
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
         * <p>Valid Values:
         *
         * <p><b>Amazon SNS actions:</b>
         *
         * <p><code>arn:aws:sns:<i>region</i>:<i>account-id</i>:<i>sns-topic-name</i></code>
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
         * <p><b>Systems Manager actions:</b>
         *
         * <p><code>arn:aws:ssm:<i>region</i>:<i>account-id</i>:opsitem:<i>severity</i></code>
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
         * <p>Valid Values:
         *
         * <p><b>Amazon SNS actions:</b>
         *
         * <p><code>arn:aws:sns:<i>region</i>:<i>account-id</i>:<i>sns-topic-name</i></code>
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
         * @return this builder.
         */
        public Builder insufficientDataActions(List<String> insufficientDataActions) {
            this.insufficientDataActions = insufficientDataActions;
            return this;
        }

        /**
         * The number of most recent scheduled query results to evaluate against the threshold (the N in M-of-N evaluation).
         * Valid range is 1 through 100.
         *
         * <p><strong>Required</strong>
         * @return this builder.
         */
        public Builder queryResultsToEvaluate(Integer queryResultsToEvaluate) {
            this.queryResultsToEvaluate = queryResultsToEvaluate;
            tracker.setMember($SCHEMA_QUERY_RESULTS_TO_EVALUATE);
            return this;
        }

        /**
         * The number of query results, out of the most recent <code>QueryResultsToEvaluate</code> results, that must breach
         * the threshold to trigger the alarm to transition to <code>ALARM</code> (the M in M-of-N evaluation). Must be less
         * than or equal to <code>QueryResultsToEvaluate</code>.
         *
         * <p><strong>Required</strong>
         * @return this builder.
         */
        public Builder queryResultsToAlarm(Integer queryResultsToAlarm) {
            this.queryResultsToAlarm = queryResultsToAlarm;
            tracker.setMember($SCHEMA_QUERY_RESULTS_TO_ALARM);
            return this;
        }

        /**
         * The value to compare with the aggregated query result.
         *
         * <p><strong>Required</strong>
         * @return this builder.
         */
        public Builder threshold(Double threshold) {
            this.threshold = threshold;
            tracker.setMember($SCHEMA_THRESHOLD);
            return this;
        }

        /**
         * The arithmetic operation to use when comparing the aggregated query result and the threshold. The aggregated
         * query result is used as the first operand. Valid values are <code>GreaterThanThreshold</code>,
         * <code>GreaterThanOrEqualToThreshold</code>, <code>LessThanThreshold</code>, and
         * <code>LessThanOrEqualToThreshold</code>.
         *
         * <p><strong>Required</strong>
         * @return this builder.
         */
        public Builder comparisonOperator(ComparisonOperator comparisonOperator) {
            this.comparisonOperator = Objects.requireNonNull(comparisonOperator, "comparisonOperator cannot be null");
            tracker.setMember($SCHEMA_COMPARISON_OPERATOR);
            return this;
        }

        /**
         * Sets how this alarm is to handle missing data points. Valid values are <code>breaching</code>,
         * <code>notBreaching</code>, <code>ignore</code>, and <code>missing</code>. If this parameter is omitted, the
         * default behavior of <code>missing</code> is used.
         *
         * @return this builder.
         */
        public Builder treatMissingData(String treatMissingData) {
            this.treatMissingData = treatMissingData;
            return this;
        }

        /**
         * A list of key-value pairs to associate with the alarm. You can use tags to categorize and manage your alarms.
         *
         * @return this builder.
         */
        public Builder tags(List<Tag> tags) {
            this.tags = tags;
            return this;
        }

        /**
         * The warm-up configuration for the alarm. A warm-up period delays alarm evaluation after you create or update the
         * alarm. The warm-up period reduces alarm noise from missing data while a new resource or service starts publishing
         * data.
         *
         * <p>For more information, see <a href="https://docs.aws.amazon.com/AmazonCloudWatch/latest/monitoring/alarm-warm-up.html">Alarm warm-up periods</a> in the <i>Amazon CloudWatch User Guide</i>.
         *
         * @return this builder.
         */
        public Builder warmUpConfiguration(WarmUpConfiguration warmUpConfiguration) {
            this.warmUpConfiguration = warmUpConfiguration;
            return this;
        }

        @Override
        public PutLogAlarmInput build() {
            tracker.validate();
            return new PutLogAlarmInput(this);
        }

        @Override
        @SuppressWarnings("unchecked")
        public void setMemberValue(Schema member, Object value) {
            switch (member.memberIndex()) {
                case 0 -> alarmName((String) SchemaUtils.validateSameMember($SCHEMA_ALARM_NAME, member, value));
                case 1 -> scheduledQueryConfiguration((ScheduledQueryConfiguration) SchemaUtils.validateSameMember($SCHEMA_SCHEDULED_QUERY_CONFIGURATION, member, value));
                case 2 -> queryResultsToEvaluate((Integer) SchemaUtils.validateSameMember($SCHEMA_QUERY_RESULTS_TO_EVALUATE, member, value));
                case 3 -> queryResultsToAlarm((Integer) SchemaUtils.validateSameMember($SCHEMA_QUERY_RESULTS_TO_ALARM, member, value));
                case 4 -> threshold((Double) SchemaUtils.validateSameMember($SCHEMA_THRESHOLD, member, value));
                case 5 -> comparisonOperator((ComparisonOperator) SchemaUtils.validateSameMember($SCHEMA_COMPARISON_OPERATOR, member, value));
                case 6 -> alarmDescription((String) SchemaUtils.validateSameMember($SCHEMA_ALARM_DESCRIPTION, member, value));
                case 7 -> actionLogLineCount((Integer) SchemaUtils.validateSameMember($SCHEMA_ACTION_LOG_LINE_COUNT, member, value));
                case 8 -> actionLogLineRoleArn((String) SchemaUtils.validateSameMember($SCHEMA_ACTION_LOG_LINE_ROLE_ARN, member, value));
                case 9 -> actionsEnabled((Boolean) SchemaUtils.validateSameMember($SCHEMA_ACTIONS_ENABLED, member, value));
                case 10 -> okActions((List<String>) SchemaUtils.validateSameMember($SCHEMA_OK_ACTIONS, member, value));
                case 11 -> alarmActions((List<String>) SchemaUtils.validateSameMember($SCHEMA_ALARM_ACTIONS, member, value));
                case 12 -> insufficientDataActions((List<String>) SchemaUtils.validateSameMember($SCHEMA_INSUFFICIENT_DATA_ACTIONS, member, value));
                case 13 -> treatMissingData((String) SchemaUtils.validateSameMember($SCHEMA_TREAT_MISSING_DATA, member, value));
                case 14 -> tags((List<Tag>) SchemaUtils.validateSameMember($SCHEMA_TAGS, member, value));
                case 15 -> warmUpConfiguration((WarmUpConfiguration) SchemaUtils.validateSameMember($SCHEMA_WARM_UP_CONFIGURATION, member, value));
                default -> ShapeBuilder.super.setMemberValue(member, value);
            }
        }

        @Override
        public ShapeBuilder<PutLogAlarmInput> errorCorrection() {
            if (tracker.allSet()) {
                return this;
            }
            if (!tracker.checkMember($SCHEMA_ALARM_NAME)) {
                alarmName("");
            }
            if (!tracker.checkMember($SCHEMA_SCHEDULED_QUERY_CONFIGURATION)) {
                tracker.setMember($SCHEMA_SCHEDULED_QUERY_CONFIGURATION);
            }
            if (!tracker.checkMember($SCHEMA_QUERY_RESULTS_TO_EVALUATE)) {
                tracker.setMember($SCHEMA_QUERY_RESULTS_TO_EVALUATE);
            }
            if (!tracker.checkMember($SCHEMA_QUERY_RESULTS_TO_ALARM)) {
                tracker.setMember($SCHEMA_QUERY_RESULTS_TO_ALARM);
            }
            if (!tracker.checkMember($SCHEMA_THRESHOLD)) {
                tracker.setMember($SCHEMA_THRESHOLD);
            }
            if (!tracker.checkMember($SCHEMA_COMPARISON_OPERATOR)) {
                comparisonOperator(ComparisonOperator.unknown(""));
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
                    case 1 -> builder.scheduledQueryConfiguration(ScheduledQueryConfiguration.builder().deserializeMember(de, member).build());
                    case 2 -> builder.queryResultsToEvaluate(de.readInteger(member));
                    case 3 -> builder.queryResultsToAlarm(de.readInteger(member));
                    case 4 -> builder.threshold(de.readDouble(member));
                    case 5 -> builder.comparisonOperator(ComparisonOperator.builder().deserializeMember(de, member).build());
                    case 6 -> builder.alarmDescription(de.readString(member));
                    case 7 -> builder.actionLogLineCount(de.readInteger(member));
                    case 8 -> builder.actionLogLineRoleArn(de.readString(member));
                    case 9 -> builder.actionsEnabled(de.readBoolean(member));
                    case 10 -> builder.okActions(SharedSerde.deserializeResourceList(member, de));
                    case 11 -> builder.alarmActions(SharedSerde.deserializeResourceList(member, de));
                    case 12 -> builder.insufficientDataActions(SharedSerde.deserializeResourceList(member, de));
                    case 13 -> builder.treatMissingData(de.readString(member));
                    case 14 -> builder.tags(SharedSerde.deserializeTagList(member, de));
                    case 15 -> builder.warmUpConfiguration(WarmUpConfiguration.builder().deserializeMember(de, member).build());
                    default -> throw new IllegalArgumentException("Unexpected member: " + member.memberName());
                }
            }
        }
    }
}
