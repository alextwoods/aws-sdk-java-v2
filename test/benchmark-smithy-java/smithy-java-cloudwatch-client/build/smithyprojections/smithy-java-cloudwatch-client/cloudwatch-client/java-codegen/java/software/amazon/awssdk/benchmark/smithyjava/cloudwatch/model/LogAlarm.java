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
 * The details about a log alarm.
 */
@SmithyGenerated
public final class LogAlarm implements SerializableStruct {

    public static final Schema $SCHEMA = Schemas.LOG_ALARM;
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
    private static final Schema $SCHEMA_SCHEDULED_QUERY_CONFIGURATION = $SCHEMA.member("ScheduledQueryConfiguration");
    private static final Schema $SCHEMA_QUERY_RESULTS_TO_EVALUATE = $SCHEMA.member("QueryResultsToEvaluate");
    private static final Schema $SCHEMA_QUERY_RESULTS_TO_ALARM = $SCHEMA.member("QueryResultsToAlarm");
    private static final Schema $SCHEMA_THRESHOLD = $SCHEMA.member("Threshold");
    private static final Schema $SCHEMA_COMPARISON_OPERATOR = $SCHEMA.member("ComparisonOperator");
    private static final Schema $SCHEMA_TREAT_MISSING_DATA = $SCHEMA.member("TreatMissingData");
    private static final Schema $SCHEMA_STATE_TRANSITIONED_TIMESTAMP = $SCHEMA.member("StateTransitionedTimestamp");
    private static final Schema $SCHEMA_EVALUATION_STATE = $SCHEMA.member("EvaluationState");
    private static final Schema $SCHEMA_ACTION_LOG_LINE_COUNT = $SCHEMA.member("ActionLogLineCount");
    private static final Schema $SCHEMA_ACTION_LOG_LINE_ROLE_ARN = $SCHEMA.member("ActionLogLineRoleArn");
    private static final Schema $SCHEMA_WARM_UP_CONFIGURATION = $SCHEMA.member("WarmUpConfiguration");

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
    private final transient ScheduledQueryConfiguration scheduledQueryConfiguration;
    private final transient Integer queryResultsToEvaluate;
    private final transient Integer queryResultsToAlarm;
    private final transient Double threshold;
    private final transient ComparisonOperator comparisonOperator;
    private final transient String treatMissingData;
    private final transient Instant stateTransitionedTimestamp;
    private final transient EvaluationState evaluationState;
    private final transient Integer actionLogLineCount;
    private final transient String actionLogLineRoleArn;
    private final transient WarmUpConfiguration warmUpConfiguration;

    private LogAlarm(Builder builder) {
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
        this.scheduledQueryConfiguration = builder.scheduledQueryConfiguration;
        this.queryResultsToEvaluate = builder.queryResultsToEvaluate;
        this.queryResultsToAlarm = builder.queryResultsToAlarm;
        this.threshold = builder.threshold;
        this.comparisonOperator = builder.comparisonOperator;
        this.treatMissingData = builder.treatMissingData;
        this.stateTransitionedTimestamp = builder.stateTransitionedTimestamp;
        this.evaluationState = builder.evaluationState;
        this.actionLogLineCount = builder.actionLogLineCount;
        this.actionLogLineRoleArn = builder.actionLogLineRoleArn;
        this.warmUpConfiguration = builder.warmUpConfiguration;
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
     * The configuration of the underlying CloudWatch Logs scheduled query, including the query string, log groups,
     * schedule, aggregation expression, and the ARN of the managed scheduled query.
     */
    public ScheduledQueryConfiguration getScheduledQueryConfiguration() {
        return scheduledQueryConfiguration;
    }

    /**
     * The number of most recent scheduled query results that the alarm evaluates against the threshold (the N in M-of-N
     * evaluation).
     */
    public Integer getQueryResultsToEvaluate() {
        return queryResultsToEvaluate;
    }

    /**
     * The number of query results, out of the most recent <code>QueryResultsToEvaluate</code> results, that must breach
     * the threshold to trigger the alarm to transition to <code>ALARM</code> (the M in M-of-N evaluation).
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
     * query result is used as the first operand.
     */
    public ComparisonOperator getComparisonOperator() {
        return comparisonOperator;
    }

    /**
     * How this alarm handles missing data points. Valid values are <code>breaching</code>, <code>notBreaching</code>,
     * <code>ignore</code>, and <code>missing</code>.
     */
    public String getTreatMissingData() {
        return treatMissingData;
    }

    /**
     * The date and time that the alarm's <code>StateValue</code> most recently changed.
     */
    public Instant getStateTransitionedTimestamp() {
        return stateTransitionedTimestamp;
    }

    /**
     * If the value of this field is <code>EVALUATION_ERROR</code>, it indicates configuration errors in the alarm setup
     * that require review and correction. Refer to the <code>StateReason</code> field of the alarm for more details.
     *
     * <p>If the value of this field is <code>EVALUATION_FAILURE</code>, it indicates temporary CloudWatch issues. We
     * recommend manual monitoring until the issue is resolved.
     *
     * <p>If the value of this field is <code>PARTIAL_DATA</code>, it indicates that the query returned the maximum 500
     * contributor groups but more matched. The alarm evaluates the available contributors, but results might be
     * incomplete.
     */
    public EvaluationState getEvaluationState() {
        return evaluationState;
    }

    /**
     * The number of log lines from the most recent scheduled query execution that are included in alarm action
     * notifications. Valid range is 0 through 50. A value of 0 means no log lines are included.
     */
    public Integer getActionLogLineCount() {
        return actionLogLineCount;
    }

    /**
     * The Amazon Resource Name (ARN) of the IAM role that CloudWatch assumes to retrieve log events for inclusion in
     * alarm action notifications. Set when <code>ActionLogLineCount</code> is greater than 0.
     */
    public String getActionLogLineRoleArn() {
        return actionLogLineRoleArn;
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
        LogAlarm that = (LogAlarm) other;
        return Objects.equals(this.actionsEnabled, that.actionsEnabled)
               && Objects.equals(this.queryResultsToEvaluate, that.queryResultsToEvaluate)
               && Objects.equals(this.queryResultsToAlarm, that.queryResultsToAlarm)
               && Objects.equals(this.actionLogLineCount, that.actionLogLineCount)
               && Objects.equals(this.threshold, that.threshold)
               && Objects.equals(this.alarmName, that.alarmName)
               && Objects.equals(this.alarmArn, that.alarmArn)
               && Objects.equals(this.alarmDescription, that.alarmDescription)
               && Objects.equals(this.stateReason, that.stateReason)
               && Objects.equals(this.stateReasonData, that.stateReasonData)
               && Objects.equals(this.treatMissingData, that.treatMissingData)
               && Objects.equals(this.actionLogLineRoleArn, that.actionLogLineRoleArn)
               && Objects.equals(this.stateValue, that.stateValue)
               && Objects.equals(this.comparisonOperator, that.comparisonOperator)
               && Objects.equals(this.evaluationState, that.evaluationState)
               && Objects.equals(this.alarmConfigurationUpdatedTimestamp, that.alarmConfigurationUpdatedTimestamp)
               && Objects.equals(this.stateUpdatedTimestamp, that.stateUpdatedTimestamp)
               && Objects.equals(this.stateTransitionedTimestamp, that.stateTransitionedTimestamp)
               && Objects.equals(this.scheduledQueryConfiguration, that.scheduledQueryConfiguration)
               && Objects.equals(this.warmUpConfiguration, that.warmUpConfiguration)
               && Objects.equals(this.okActions, that.okActions)
               && Objects.equals(this.alarmActions, that.alarmActions)
               && Objects.equals(this.insufficientDataActions, that.insufficientDataActions);
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
        $hc = 31 * $hc + Objects.hashCode(scheduledQueryConfiguration);
        $hc = 31 * $hc + Objects.hashCode(queryResultsToEvaluate);
        $hc = 31 * $hc + Objects.hashCode(queryResultsToAlarm);
        $hc = 31 * $hc + Objects.hashCode(threshold);
        $hc = 31 * $hc + Objects.hashCode(comparisonOperator);
        $hc = 31 * $hc + Objects.hashCode(treatMissingData);
        $hc = 31 * $hc + Objects.hashCode(stateTransitionedTimestamp);
        $hc = 31 * $hc + Objects.hashCode(evaluationState);
        $hc = 31 * $hc + Objects.hashCode(actionLogLineCount);
        $hc = 31 * $hc + Objects.hashCode(actionLogLineRoleArn);
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
        if (scheduledQueryConfiguration != null) {
            serializer.writeStruct($SCHEMA_SCHEDULED_QUERY_CONFIGURATION, scheduledQueryConfiguration);
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
        if (stateTransitionedTimestamp != null) {
            serializer.writeTimestamp($SCHEMA_STATE_TRANSITIONED_TIMESTAMP, stateTransitionedTimestamp);
        }
        if (evaluationState != null) {
            serializer.writeString($SCHEMA_EVALUATION_STATE, evaluationState.getValue());
        }
        if (actionLogLineCount != null) {
            serializer.writeInteger($SCHEMA_ACTION_LOG_LINE_COUNT, actionLogLineCount);
        }
        if (actionLogLineRoleArn != null) {
            serializer.writeString($SCHEMA_ACTION_LOG_LINE_ROLE_ARN, actionLogLineRoleArn);
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
            case 12 -> (T) SchemaUtils.validateSameMember($SCHEMA_SCHEDULED_QUERY_CONFIGURATION, member, scheduledQueryConfiguration);
            case 13 -> (T) SchemaUtils.validateSameMember($SCHEMA_QUERY_RESULTS_TO_EVALUATE, member, queryResultsToEvaluate);
            case 14 -> (T) SchemaUtils.validateSameMember($SCHEMA_QUERY_RESULTS_TO_ALARM, member, queryResultsToAlarm);
            case 15 -> (T) SchemaUtils.validateSameMember($SCHEMA_THRESHOLD, member, threshold);
            case 16 -> (T) SchemaUtils.validateSameMember($SCHEMA_COMPARISON_OPERATOR, member, comparisonOperator);
            case 17 -> (T) SchemaUtils.validateSameMember($SCHEMA_TREAT_MISSING_DATA, member, treatMissingData);
            case 18 -> (T) SchemaUtils.validateSameMember($SCHEMA_STATE_TRANSITIONED_TIMESTAMP, member, stateTransitionedTimestamp);
            case 19 -> (T) SchemaUtils.validateSameMember($SCHEMA_EVALUATION_STATE, member, evaluationState);
            case 20 -> (T) SchemaUtils.validateSameMember($SCHEMA_ACTION_LOG_LINE_COUNT, member, actionLogLineCount);
            case 21 -> (T) SchemaUtils.validateSameMember($SCHEMA_ACTION_LOG_LINE_ROLE_ARN, member, actionLogLineRoleArn);
            case 22 -> (T) SchemaUtils.validateSameMember($SCHEMA_WARM_UP_CONFIGURATION, member, warmUpConfiguration);
            default -> throw new IllegalArgumentException("Attempted to get non-existent member: " + member.id());
        };
    }

    /**
     * Create a new builder containing all the current property values of this object.
     *
     * <p><strong>Note:</strong> This method performs only a shallow copy of the original properties.
     *
     * @return a builder for {@link LogAlarm}.
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
        builder.scheduledQueryConfiguration(this.scheduledQueryConfiguration);
        builder.queryResultsToEvaluate(this.queryResultsToEvaluate);
        builder.queryResultsToAlarm(this.queryResultsToAlarm);
        builder.threshold(this.threshold);
        builder.comparisonOperator(this.comparisonOperator);
        builder.treatMissingData(this.treatMissingData);
        builder.stateTransitionedTimestamp(this.stateTransitionedTimestamp);
        builder.evaluationState(this.evaluationState);
        builder.actionLogLineCount(this.actionLogLineCount);
        builder.actionLogLineRoleArn(this.actionLogLineRoleArn);
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
     * Builder for {@link LogAlarm}.
     */
    public static final class Builder implements ShapeBuilder<LogAlarm> {
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
        private ScheduledQueryConfiguration scheduledQueryConfiguration;
        private Integer queryResultsToEvaluate;
        private Integer queryResultsToAlarm;
        private Double threshold;
        private ComparisonOperator comparisonOperator;
        private String treatMissingData;
        private Instant stateTransitionedTimestamp;
        private EvaluationState evaluationState;
        private Integer actionLogLineCount;
        private String actionLogLineRoleArn;
        private WarmUpConfiguration warmUpConfiguration;

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
         * The configuration of the underlying CloudWatch Logs scheduled query, including the query string, log groups,
         * schedule, aggregation expression, and the ARN of the managed scheduled query.
         *
         * @return this builder.
         */
        public Builder scheduledQueryConfiguration(ScheduledQueryConfiguration scheduledQueryConfiguration) {
            this.scheduledQueryConfiguration = scheduledQueryConfiguration;
            return this;
        }

        /**
         * The number of most recent scheduled query results that the alarm evaluates against the threshold (the N in M-of-N
         * evaluation).
         *
         * @return this builder.
         */
        public Builder queryResultsToEvaluate(Integer queryResultsToEvaluate) {
            this.queryResultsToEvaluate = queryResultsToEvaluate;
            return this;
        }

        /**
         * The number of query results, out of the most recent <code>QueryResultsToEvaluate</code> results, that must breach
         * the threshold to trigger the alarm to transition to <code>ALARM</code> (the M in M-of-N evaluation).
         *
         * @return this builder.
         */
        public Builder queryResultsToAlarm(Integer queryResultsToAlarm) {
            this.queryResultsToAlarm = queryResultsToAlarm;
            return this;
        }

        /**
         * The value to compare with the aggregated query result.
         *
         * @return this builder.
         */
        public Builder threshold(Double threshold) {
            this.threshold = threshold;
            return this;
        }

        /**
         * The arithmetic operation to use when comparing the aggregated query result and the threshold. The aggregated
         * query result is used as the first operand.
         *
         * @return this builder.
         */
        public Builder comparisonOperator(ComparisonOperator comparisonOperator) {
            this.comparisonOperator = comparisonOperator;
            return this;
        }

        /**
         * How this alarm handles missing data points. Valid values are <code>breaching</code>, <code>notBreaching</code>,
         * <code>ignore</code>, and <code>missing</code>.
         *
         * @return this builder.
         */
        public Builder treatMissingData(String treatMissingData) {
            this.treatMissingData = treatMissingData;
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
         * If the value of this field is <code>EVALUATION_ERROR</code>, it indicates configuration errors in the alarm setup
         * that require review and correction. Refer to the <code>StateReason</code> field of the alarm for more details.
         *
         * <p>If the value of this field is <code>EVALUATION_FAILURE</code>, it indicates temporary CloudWatch issues. We
         * recommend manual monitoring until the issue is resolved.
         *
         * <p>If the value of this field is <code>PARTIAL_DATA</code>, it indicates that the query returned the maximum 500
         * contributor groups but more matched. The alarm evaluates the available contributors, but results might be
         * incomplete.
         *
         * @return this builder.
         */
        public Builder evaluationState(EvaluationState evaluationState) {
            this.evaluationState = evaluationState;
            return this;
        }

        /**
         * The number of log lines from the most recent scheduled query execution that are included in alarm action
         * notifications. Valid range is 0 through 50. A value of 0 means no log lines are included.
         *
         * @return this builder.
         */
        public Builder actionLogLineCount(Integer actionLogLineCount) {
            this.actionLogLineCount = actionLogLineCount;
            return this;
        }

        /**
         * The Amazon Resource Name (ARN) of the IAM role that CloudWatch assumes to retrieve log events for inclusion in
         * alarm action notifications. Set when <code>ActionLogLineCount</code> is greater than 0.
         *
         * @return this builder.
         */
        public Builder actionLogLineRoleArn(String actionLogLineRoleArn) {
            this.actionLogLineRoleArn = actionLogLineRoleArn;
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

        @Override
        public LogAlarm build() {
            return new LogAlarm(this);
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
                case 12 -> scheduledQueryConfiguration((ScheduledQueryConfiguration) SchemaUtils.validateSameMember($SCHEMA_SCHEDULED_QUERY_CONFIGURATION, member, value));
                case 13 -> queryResultsToEvaluate((Integer) SchemaUtils.validateSameMember($SCHEMA_QUERY_RESULTS_TO_EVALUATE, member, value));
                case 14 -> queryResultsToAlarm((Integer) SchemaUtils.validateSameMember($SCHEMA_QUERY_RESULTS_TO_ALARM, member, value));
                case 15 -> threshold((Double) SchemaUtils.validateSameMember($SCHEMA_THRESHOLD, member, value));
                case 16 -> comparisonOperator((ComparisonOperator) SchemaUtils.validateSameMember($SCHEMA_COMPARISON_OPERATOR, member, value));
                case 17 -> treatMissingData((String) SchemaUtils.validateSameMember($SCHEMA_TREAT_MISSING_DATA, member, value));
                case 18 -> stateTransitionedTimestamp((Instant) SchemaUtils.validateSameMember($SCHEMA_STATE_TRANSITIONED_TIMESTAMP, member, value));
                case 19 -> evaluationState((EvaluationState) SchemaUtils.validateSameMember($SCHEMA_EVALUATION_STATE, member, value));
                case 20 -> actionLogLineCount((Integer) SchemaUtils.validateSameMember($SCHEMA_ACTION_LOG_LINE_COUNT, member, value));
                case 21 -> actionLogLineRoleArn((String) SchemaUtils.validateSameMember($SCHEMA_ACTION_LOG_LINE_ROLE_ARN, member, value));
                case 22 -> warmUpConfiguration((WarmUpConfiguration) SchemaUtils.validateSameMember($SCHEMA_WARM_UP_CONFIGURATION, member, value));
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
                    case 12 -> builder.scheduledQueryConfiguration(ScheduledQueryConfiguration.builder().deserializeMember(de, member).build());
                    case 13 -> builder.queryResultsToEvaluate(de.readInteger(member));
                    case 14 -> builder.queryResultsToAlarm(de.readInteger(member));
                    case 15 -> builder.threshold(de.readDouble(member));
                    case 16 -> builder.comparisonOperator(ComparisonOperator.builder().deserializeMember(de, member).build());
                    case 17 -> builder.treatMissingData(de.readString(member));
                    case 18 -> builder.stateTransitionedTimestamp(de.readTimestamp(member));
                    case 19 -> builder.evaluationState(EvaluationState.builder().deserializeMember(de, member).build());
                    case 20 -> builder.actionLogLineCount(de.readInteger(member));
                    case 21 -> builder.actionLogLineRoleArn(de.readString(member));
                    case 22 -> builder.warmUpConfiguration(WarmUpConfiguration.builder().deserializeMember(de, member).build());
                    default -> throw new IllegalArgumentException("Unexpected member: " + member.memberName());
                }
            }
        }
    }
}
