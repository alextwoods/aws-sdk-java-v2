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
 * The details about a composite alarm.
 */
@SmithyGenerated
public final class CompositeAlarm implements SerializableStruct {

    public static final Schema $SCHEMA = Schemas.COMPOSITE_ALARM;
    private static final Schema $SCHEMA_ACTIONS_ENABLED = $SCHEMA.member("ActionsEnabled");
    private static final Schema $SCHEMA_ALARM_ACTIONS = $SCHEMA.member("AlarmActions");
    private static final Schema $SCHEMA_ALARM_ARN = $SCHEMA.member("AlarmArn");
    private static final Schema $SCHEMA_ALARM_CONFIGURATION_UPDATED_TIMESTAMP = $SCHEMA.member("AlarmConfigurationUpdatedTimestamp");
    private static final Schema $SCHEMA_ALARM_DESCRIPTION = $SCHEMA.member("AlarmDescription");
    private static final Schema $SCHEMA_ALARM_NAME = $SCHEMA.member("AlarmName");
    private static final Schema $SCHEMA_ALARM_RULE = $SCHEMA.member("AlarmRule");
    private static final Schema $SCHEMA_INSUFFICIENT_DATA_ACTIONS = $SCHEMA.member("InsufficientDataActions");
    private static final Schema $SCHEMA_OK_ACTIONS = $SCHEMA.member("OKActions");
    private static final Schema $SCHEMA_STATE_REASON = $SCHEMA.member("StateReason");
    private static final Schema $SCHEMA_STATE_REASON_DATA = $SCHEMA.member("StateReasonData");
    private static final Schema $SCHEMA_STATE_UPDATED_TIMESTAMP = $SCHEMA.member("StateUpdatedTimestamp");
    private static final Schema $SCHEMA_STATE_VALUE = $SCHEMA.member("StateValue");
    private static final Schema $SCHEMA_STATE_TRANSITIONED_TIMESTAMP = $SCHEMA.member("StateTransitionedTimestamp");
    private static final Schema $SCHEMA_ACTIONS_SUPPRESSED_BY = $SCHEMA.member("ActionsSuppressedBy");
    private static final Schema $SCHEMA_ACTIONS_SUPPRESSED_REASON = $SCHEMA.member("ActionsSuppressedReason");
    private static final Schema $SCHEMA_ACTIONS_SUPPRESSOR = $SCHEMA.member("ActionsSuppressor");
    private static final Schema $SCHEMA_ACTIONS_SUPPRESSOR_WAIT_PERIOD = $SCHEMA.member("ActionsSuppressorWaitPeriod");
    private static final Schema $SCHEMA_ACTIONS_SUPPRESSOR_EXTENSION_PERIOD = $SCHEMA.member("ActionsSuppressorExtensionPeriod");

    public static final ShapeId $ID = $SCHEMA.id();

    private final transient Boolean actionsEnabled;
    private final transient List<String> alarmActions;
    private final transient String alarmArn;
    private final transient Instant alarmConfigurationUpdatedTimestamp;
    private final transient String alarmDescription;
    private final transient String alarmName;
    private final transient String alarmRule;
    private final transient List<String> insufficientDataActions;
    private final transient List<String> okActions;
    private final transient String stateReason;
    private final transient String stateReasonData;
    private final transient Instant stateUpdatedTimestamp;
    private final transient StateValue stateValue;
    private final transient Instant stateTransitionedTimestamp;
    private final transient ActionsSuppressedBy actionsSuppressedBy;
    private final transient String actionsSuppressedReason;
    private final transient String actionsSuppressor;
    private final transient Integer actionsSuppressorWaitPeriod;
    private final transient Integer actionsSuppressorExtensionPeriod;

    private CompositeAlarm(Builder builder) {
        this.actionsEnabled = builder.actionsEnabled;
        this.alarmActions = builder.alarmActions == null ? null : Collections.unmodifiableList(builder.alarmActions);
        this.alarmArn = builder.alarmArn;
        this.alarmConfigurationUpdatedTimestamp = builder.alarmConfigurationUpdatedTimestamp;
        this.alarmDescription = builder.alarmDescription;
        this.alarmName = builder.alarmName;
        this.alarmRule = builder.alarmRule;
        this.insufficientDataActions = builder.insufficientDataActions == null ? null : Collections.unmodifiableList(builder.insufficientDataActions);
        this.okActions = builder.okActions == null ? null : Collections.unmodifiableList(builder.okActions);
        this.stateReason = builder.stateReason;
        this.stateReasonData = builder.stateReasonData;
        this.stateUpdatedTimestamp = builder.stateUpdatedTimestamp;
        this.stateValue = builder.stateValue;
        this.stateTransitionedTimestamp = builder.stateTransitionedTimestamp;
        this.actionsSuppressedBy = builder.actionsSuppressedBy;
        this.actionsSuppressedReason = builder.actionsSuppressedReason;
        this.actionsSuppressor = builder.actionsSuppressor;
        this.actionsSuppressorWaitPeriod = builder.actionsSuppressorWaitPeriod;
        this.actionsSuppressorExtensionPeriod = builder.actionsSuppressorExtensionPeriod;
    }

    /**
     * Indicates whether actions should be executed during any changes to the alarm state.
     */
    public Boolean isActionsEnabled() {
        return actionsEnabled;
    }

    /**
     * The actions to execute when this alarm transitions to the ALARM state from any other state. Each action is
     * specified as an Amazon Resource Name (ARN).
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
     * The Amazon Resource Name (ARN) of the alarm.
     */
    public String getAlarmArn() {
        return alarmArn;
    }

    /**
     * The time stamp of the last update to the alarm configuration.
     */
    public Instant getAlarmConfigurationUpdatedTimestamp() {
        return alarmConfigurationUpdatedTimestamp;
    }

    /**
     * The description of the alarm.
     */
    public String getAlarmDescription() {
        return alarmDescription;
    }

    /**
     * The name of the alarm.
     */
    public String getAlarmName() {
        return alarmName;
    }

    /**
     * The rule that this alarm uses to evaluate its alarm state.
     */
    public String getAlarmRule() {
        return alarmRule;
    }

    /**
     * The actions to execute when this alarm transitions to the INSUFFICIENT_DATA state from any other state. Each
     * action is specified as an Amazon Resource Name (ARN).
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
     * The actions to execute when this alarm transitions to the OK state from any other state. Each action is specified
     * as an Amazon Resource Name (ARN).
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
     * Tracks the timestamp of any state update, even if <code>StateValue</code> doesn't change.
     */
    public Instant getStateUpdatedTimestamp() {
        return stateUpdatedTimestamp;
    }

    /**
     * The state value for the alarm.
     */
    public StateValue getStateValue() {
        return stateValue;
    }

    /**
     * The timestamp of the last change to the alarm's <code>StateValue</code>.
     */
    public Instant getStateTransitionedTimestamp() {
        return stateTransitionedTimestamp;
    }

    /**
     * When the value is <code>ALARM</code>, it means that the actions are suppressed because the suppressor alarm is in
     * <code>ALARM</code> When the value is <code>WaitPeriod</code>, it means that the actions are suppressed because
     * the composite alarm is waiting for the suppressor alarm to go into into the <code>ALARM</code> state. The maximum
     * waiting time is as specified in <code>ActionsSuppressorWaitPeriod</code>. After this time, the composite alarm
     * performs its actions. When the value is <code>ExtensionPeriod</code>, it means that the actions are suppressed
     * because the composite alarm is waiting after the suppressor alarm went out of the <code>ALARM</code> state. The
     * maximum waiting time is as specified in <code>ActionsSuppressorExtensionPeriod</code>. After this time, the
     * composite alarm performs its actions.
     */
    public ActionsSuppressedBy getActionsSuppressedBy() {
        return actionsSuppressedBy;
    }

    /**
     * Captures the reason for action suppression.
     */
    public String getActionsSuppressedReason() {
        return actionsSuppressedReason;
    }

    /**
     * Actions will be suppressed if the suppressor alarm is in the <code>ALARM</code> state.
     * <code>ActionsSuppressor</code> can be an AlarmName or an Amazon Resource Name (ARN) from an existing alarm.
     */
    public String getActionsSuppressor() {
        return actionsSuppressor;
    }

    /**
     * The maximum time in seconds that the composite alarm waits for the suppressor alarm to go into the <code>ALARM</code>
     * state. After this time, the composite alarm performs its actions.
     *
     * <p><code>WaitPeriod</code> is required only when <code>ActionsSuppressor</code> is specified.
     */
    public Integer getActionsSuppressorWaitPeriod() {
        return actionsSuppressorWaitPeriod;
    }

    /**
     * The maximum time in seconds that the composite alarm waits after suppressor alarm goes out of the <code>ALARM</code>
     * state. After this time, the composite alarm performs its actions.
     *
     * <p><code>ExtensionPeriod</code> is required only when <code>ActionsSuppressor</code> is specified.
     */
    public Integer getActionsSuppressorExtensionPeriod() {
        return actionsSuppressorExtensionPeriod;
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
        CompositeAlarm that = (CompositeAlarm) other;
        return Objects.equals(this.actionsEnabled, that.actionsEnabled)
               && Objects.equals(this.actionsSuppressorWaitPeriod, that.actionsSuppressorWaitPeriod)
               && Objects.equals(this.actionsSuppressorExtensionPeriod, that.actionsSuppressorExtensionPeriod)
               && Objects.equals(this.alarmArn, that.alarmArn)
               && Objects.equals(this.alarmDescription, that.alarmDescription)
               && Objects.equals(this.alarmName, that.alarmName)
               && Objects.equals(this.alarmRule, that.alarmRule)
               && Objects.equals(this.stateReason, that.stateReason)
               && Objects.equals(this.stateReasonData, that.stateReasonData)
               && Objects.equals(this.actionsSuppressedReason, that.actionsSuppressedReason)
               && Objects.equals(this.actionsSuppressor, that.actionsSuppressor)
               && Objects.equals(this.stateValue, that.stateValue)
               && Objects.equals(this.actionsSuppressedBy, that.actionsSuppressedBy)
               && Objects.equals(this.alarmConfigurationUpdatedTimestamp, that.alarmConfigurationUpdatedTimestamp)
               && Objects.equals(this.stateUpdatedTimestamp, that.stateUpdatedTimestamp)
               && Objects.equals(this.stateTransitionedTimestamp, that.stateTransitionedTimestamp)
               && Objects.equals(this.alarmActions, that.alarmActions)
               && Objects.equals(this.insufficientDataActions, that.insufficientDataActions)
               && Objects.equals(this.okActions, that.okActions);
    }

    @Override
    public int hashCode() {
        int $hc = Objects.hashCode(actionsEnabled);
        $hc = 31 * $hc + Objects.hashCode(alarmActions);
        $hc = 31 * $hc + Objects.hashCode(alarmArn);
        $hc = 31 * $hc + Objects.hashCode(alarmConfigurationUpdatedTimestamp);
        $hc = 31 * $hc + Objects.hashCode(alarmDescription);
        $hc = 31 * $hc + Objects.hashCode(alarmName);
        $hc = 31 * $hc + Objects.hashCode(alarmRule);
        $hc = 31 * $hc + Objects.hashCode(insufficientDataActions);
        $hc = 31 * $hc + Objects.hashCode(okActions);
        $hc = 31 * $hc + Objects.hashCode(stateReason);
        $hc = 31 * $hc + Objects.hashCode(stateReasonData);
        $hc = 31 * $hc + Objects.hashCode(stateUpdatedTimestamp);
        $hc = 31 * $hc + Objects.hashCode(stateValue);
        $hc = 31 * $hc + Objects.hashCode(stateTransitionedTimestamp);
        $hc = 31 * $hc + Objects.hashCode(actionsSuppressedBy);
        $hc = 31 * $hc + Objects.hashCode(actionsSuppressedReason);
        $hc = 31 * $hc + Objects.hashCode(actionsSuppressor);
        $hc = 31 * $hc + Objects.hashCode(actionsSuppressorWaitPeriod);
        $hc = 31 * $hc + Objects.hashCode(actionsSuppressorExtensionPeriod);
        return $hc;
    }

    @Override
    public Schema schema() {
        return $SCHEMA;
    }

    @Override
    public void serializeMembers(ShapeSerializer serializer) {
        if (actionsEnabled != null) {
            serializer.writeBoolean($SCHEMA_ACTIONS_ENABLED, actionsEnabled);
        }
        if (alarmActions != null) {
            serializer.writeList($SCHEMA_ALARM_ACTIONS, alarmActions, alarmActions.size(), SharedSerde.ResourceListSerializer.INSTANCE);
        }
        if (alarmArn != null) {
            serializer.writeString($SCHEMA_ALARM_ARN, alarmArn);
        }
        if (alarmConfigurationUpdatedTimestamp != null) {
            serializer.writeTimestamp($SCHEMA_ALARM_CONFIGURATION_UPDATED_TIMESTAMP, alarmConfigurationUpdatedTimestamp);
        }
        if (alarmDescription != null) {
            serializer.writeString($SCHEMA_ALARM_DESCRIPTION, alarmDescription);
        }
        if (alarmName != null) {
            serializer.writeString($SCHEMA_ALARM_NAME, alarmName);
        }
        if (alarmRule != null) {
            serializer.writeString($SCHEMA_ALARM_RULE, alarmRule);
        }
        if (insufficientDataActions != null) {
            serializer.writeList($SCHEMA_INSUFFICIENT_DATA_ACTIONS, insufficientDataActions, insufficientDataActions.size(), SharedSerde.ResourceListSerializer.INSTANCE);
        }
        if (okActions != null) {
            serializer.writeList($SCHEMA_OK_ACTIONS, okActions, okActions.size(), SharedSerde.ResourceListSerializer.INSTANCE);
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
        if (stateValue != null) {
            serializer.writeString($SCHEMA_STATE_VALUE, stateValue.getValue());
        }
        if (stateTransitionedTimestamp != null) {
            serializer.writeTimestamp($SCHEMA_STATE_TRANSITIONED_TIMESTAMP, stateTransitionedTimestamp);
        }
        if (actionsSuppressedBy != null) {
            serializer.writeString($SCHEMA_ACTIONS_SUPPRESSED_BY, actionsSuppressedBy.getValue());
        }
        if (actionsSuppressedReason != null) {
            serializer.writeString($SCHEMA_ACTIONS_SUPPRESSED_REASON, actionsSuppressedReason);
        }
        if (actionsSuppressor != null) {
            serializer.writeString($SCHEMA_ACTIONS_SUPPRESSOR, actionsSuppressor);
        }
        if (actionsSuppressorWaitPeriod != null) {
            serializer.writeInteger($SCHEMA_ACTIONS_SUPPRESSOR_WAIT_PERIOD, actionsSuppressorWaitPeriod);
        }
        if (actionsSuppressorExtensionPeriod != null) {
            serializer.writeInteger($SCHEMA_ACTIONS_SUPPRESSOR_EXTENSION_PERIOD, actionsSuppressorExtensionPeriod);
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T getMemberValue(Schema member) {
        return switch (member.memberIndex()) {
            case 0 -> (T) SchemaUtils.validateSameMember($SCHEMA_ACTIONS_ENABLED, member, actionsEnabled);
            case 1 -> (T) SchemaUtils.validateSameMember($SCHEMA_ALARM_ACTIONS, member, alarmActions);
            case 2 -> (T) SchemaUtils.validateSameMember($SCHEMA_ALARM_ARN, member, alarmArn);
            case 3 -> (T) SchemaUtils.validateSameMember($SCHEMA_ALARM_CONFIGURATION_UPDATED_TIMESTAMP, member, alarmConfigurationUpdatedTimestamp);
            case 4 -> (T) SchemaUtils.validateSameMember($SCHEMA_ALARM_DESCRIPTION, member, alarmDescription);
            case 5 -> (T) SchemaUtils.validateSameMember($SCHEMA_ALARM_NAME, member, alarmName);
            case 6 -> (T) SchemaUtils.validateSameMember($SCHEMA_ALARM_RULE, member, alarmRule);
            case 7 -> (T) SchemaUtils.validateSameMember($SCHEMA_INSUFFICIENT_DATA_ACTIONS, member, insufficientDataActions);
            case 8 -> (T) SchemaUtils.validateSameMember($SCHEMA_OK_ACTIONS, member, okActions);
            case 9 -> (T) SchemaUtils.validateSameMember($SCHEMA_STATE_REASON, member, stateReason);
            case 10 -> (T) SchemaUtils.validateSameMember($SCHEMA_STATE_REASON_DATA, member, stateReasonData);
            case 11 -> (T) SchemaUtils.validateSameMember($SCHEMA_STATE_UPDATED_TIMESTAMP, member, stateUpdatedTimestamp);
            case 12 -> (T) SchemaUtils.validateSameMember($SCHEMA_STATE_VALUE, member, stateValue);
            case 13 -> (T) SchemaUtils.validateSameMember($SCHEMA_STATE_TRANSITIONED_TIMESTAMP, member, stateTransitionedTimestamp);
            case 14 -> (T) SchemaUtils.validateSameMember($SCHEMA_ACTIONS_SUPPRESSED_BY, member, actionsSuppressedBy);
            case 15 -> (T) SchemaUtils.validateSameMember($SCHEMA_ACTIONS_SUPPRESSED_REASON, member, actionsSuppressedReason);
            case 16 -> (T) SchemaUtils.validateSameMember($SCHEMA_ACTIONS_SUPPRESSOR, member, actionsSuppressor);
            case 17 -> (T) SchemaUtils.validateSameMember($SCHEMA_ACTIONS_SUPPRESSOR_WAIT_PERIOD, member, actionsSuppressorWaitPeriod);
            case 18 -> (T) SchemaUtils.validateSameMember($SCHEMA_ACTIONS_SUPPRESSOR_EXTENSION_PERIOD, member, actionsSuppressorExtensionPeriod);
            default -> throw new IllegalArgumentException("Attempted to get non-existent member: " + member.id());
        };
    }

    /**
     * Create a new builder containing all the current property values of this object.
     *
     * <p><strong>Note:</strong> This method performs only a shallow copy of the original properties.
     *
     * @return a builder for {@link CompositeAlarm}.
     */
    public Builder toBuilder() {
        var builder = new Builder();
        builder.actionsEnabled(this.actionsEnabled);
        builder.alarmActions(this.alarmActions);
        builder.alarmArn(this.alarmArn);
        builder.alarmConfigurationUpdatedTimestamp(this.alarmConfigurationUpdatedTimestamp);
        builder.alarmDescription(this.alarmDescription);
        builder.alarmName(this.alarmName);
        builder.alarmRule(this.alarmRule);
        builder.insufficientDataActions(this.insufficientDataActions);
        builder.okActions(this.okActions);
        builder.stateReason(this.stateReason);
        builder.stateReasonData(this.stateReasonData);
        builder.stateUpdatedTimestamp(this.stateUpdatedTimestamp);
        builder.stateValue(this.stateValue);
        builder.stateTransitionedTimestamp(this.stateTransitionedTimestamp);
        builder.actionsSuppressedBy(this.actionsSuppressedBy);
        builder.actionsSuppressedReason(this.actionsSuppressedReason);
        builder.actionsSuppressor(this.actionsSuppressor);
        builder.actionsSuppressorWaitPeriod(this.actionsSuppressorWaitPeriod);
        builder.actionsSuppressorExtensionPeriod(this.actionsSuppressorExtensionPeriod);
        return builder;
    }

    /**
     * @return returns a new Builder.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for {@link CompositeAlarm}.
     */
    public static final class Builder implements ShapeBuilder<CompositeAlarm> {
        private Boolean actionsEnabled;
        private List<String> alarmActions;
        private String alarmArn;
        private Instant alarmConfigurationUpdatedTimestamp;
        private String alarmDescription;
        private String alarmName;
        private String alarmRule;
        private List<String> insufficientDataActions;
        private List<String> okActions;
        private String stateReason;
        private String stateReasonData;
        private Instant stateUpdatedTimestamp;
        private StateValue stateValue;
        private Instant stateTransitionedTimestamp;
        private ActionsSuppressedBy actionsSuppressedBy;
        private String actionsSuppressedReason;
        private String actionsSuppressor;
        private Integer actionsSuppressorWaitPeriod;
        private Integer actionsSuppressorExtensionPeriod;

        private Builder() {}

        @Override
        public Schema schema() {
            return $SCHEMA;
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
         * The actions to execute when this alarm transitions to the ALARM state from any other state. Each action is
         * specified as an Amazon Resource Name (ARN).
         *
         * @return this builder.
         */
        public Builder alarmActions(List<String> alarmActions) {
            this.alarmActions = alarmActions;
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
         * The time stamp of the last update to the alarm configuration.
         *
         * @return this builder.
         */
        public Builder alarmConfigurationUpdatedTimestamp(Instant alarmConfigurationUpdatedTimestamp) {
            this.alarmConfigurationUpdatedTimestamp = alarmConfigurationUpdatedTimestamp;
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
         * The name of the alarm.
         *
         * @return this builder.
         */
        public Builder alarmName(String alarmName) {
            this.alarmName = alarmName;
            return this;
        }

        /**
         * The rule that this alarm uses to evaluate its alarm state.
         *
         * @return this builder.
         */
        public Builder alarmRule(String alarmRule) {
            this.alarmRule = alarmRule;
            return this;
        }

        /**
         * The actions to execute when this alarm transitions to the INSUFFICIENT_DATA state from any other state. Each
         * action is specified as an Amazon Resource Name (ARN).
         *
         * @return this builder.
         */
        public Builder insufficientDataActions(List<String> insufficientDataActions) {
            this.insufficientDataActions = insufficientDataActions;
            return this;
        }

        /**
         * The actions to execute when this alarm transitions to the OK state from any other state. Each action is specified
         * as an Amazon Resource Name (ARN).
         *
         * @return this builder.
         */
        public Builder okActions(List<String> okActions) {
            this.okActions = okActions;
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
         * Tracks the timestamp of any state update, even if <code>StateValue</code> doesn't change.
         *
         * @return this builder.
         */
        public Builder stateUpdatedTimestamp(Instant stateUpdatedTimestamp) {
            this.stateUpdatedTimestamp = stateUpdatedTimestamp;
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
         * The timestamp of the last change to the alarm's <code>StateValue</code>.
         *
         * @return this builder.
         */
        public Builder stateTransitionedTimestamp(Instant stateTransitionedTimestamp) {
            this.stateTransitionedTimestamp = stateTransitionedTimestamp;
            return this;
        }

        /**
         * When the value is <code>ALARM</code>, it means that the actions are suppressed because the suppressor alarm is in
         * <code>ALARM</code> When the value is <code>WaitPeriod</code>, it means that the actions are suppressed because
         * the composite alarm is waiting for the suppressor alarm to go into into the <code>ALARM</code> state. The maximum
         * waiting time is as specified in <code>ActionsSuppressorWaitPeriod</code>. After this time, the composite alarm
         * performs its actions. When the value is <code>ExtensionPeriod</code>, it means that the actions are suppressed
         * because the composite alarm is waiting after the suppressor alarm went out of the <code>ALARM</code> state. The
         * maximum waiting time is as specified in <code>ActionsSuppressorExtensionPeriod</code>. After this time, the
         * composite alarm performs its actions.
         *
         * @return this builder.
         */
        public Builder actionsSuppressedBy(ActionsSuppressedBy actionsSuppressedBy) {
            this.actionsSuppressedBy = actionsSuppressedBy;
            return this;
        }

        /**
         * Captures the reason for action suppression.
         *
         * @return this builder.
         */
        public Builder actionsSuppressedReason(String actionsSuppressedReason) {
            this.actionsSuppressedReason = actionsSuppressedReason;
            return this;
        }

        /**
         * Actions will be suppressed if the suppressor alarm is in the <code>ALARM</code> state.
         * <code>ActionsSuppressor</code> can be an AlarmName or an Amazon Resource Name (ARN) from an existing alarm.
         *
         * @return this builder.
         */
        public Builder actionsSuppressor(String actionsSuppressor) {
            this.actionsSuppressor = actionsSuppressor;
            return this;
        }

        /**
         * The maximum time in seconds that the composite alarm waits for the suppressor alarm to go into the <code>ALARM</code>
         * state. After this time, the composite alarm performs its actions.
         *
         * <p><code>WaitPeriod</code> is required only when <code>ActionsSuppressor</code> is specified.
         *
         * @return this builder.
         */
        public Builder actionsSuppressorWaitPeriod(Integer actionsSuppressorWaitPeriod) {
            this.actionsSuppressorWaitPeriod = actionsSuppressorWaitPeriod;
            return this;
        }

        /**
         * The maximum time in seconds that the composite alarm waits after suppressor alarm goes out of the <code>ALARM</code>
         * state. After this time, the composite alarm performs its actions.
         *
         * <p><code>ExtensionPeriod</code> is required only when <code>ActionsSuppressor</code> is specified.
         *
         * @return this builder.
         */
        public Builder actionsSuppressorExtensionPeriod(Integer actionsSuppressorExtensionPeriod) {
            this.actionsSuppressorExtensionPeriod = actionsSuppressorExtensionPeriod;
            return this;
        }

        @Override
        public CompositeAlarm build() {
            return new CompositeAlarm(this);
        }

        @Override
        @SuppressWarnings("unchecked")
        public void setMemberValue(Schema member, Object value) {
            switch (member.memberIndex()) {
                case 0 -> actionsEnabled((Boolean) SchemaUtils.validateSameMember($SCHEMA_ACTIONS_ENABLED, member, value));
                case 1 -> alarmActions((List<String>) SchemaUtils.validateSameMember($SCHEMA_ALARM_ACTIONS, member, value));
                case 2 -> alarmArn((String) SchemaUtils.validateSameMember($SCHEMA_ALARM_ARN, member, value));
                case 3 -> alarmConfigurationUpdatedTimestamp((Instant) SchemaUtils.validateSameMember($SCHEMA_ALARM_CONFIGURATION_UPDATED_TIMESTAMP, member, value));
                case 4 -> alarmDescription((String) SchemaUtils.validateSameMember($SCHEMA_ALARM_DESCRIPTION, member, value));
                case 5 -> alarmName((String) SchemaUtils.validateSameMember($SCHEMA_ALARM_NAME, member, value));
                case 6 -> alarmRule((String) SchemaUtils.validateSameMember($SCHEMA_ALARM_RULE, member, value));
                case 7 -> insufficientDataActions((List<String>) SchemaUtils.validateSameMember($SCHEMA_INSUFFICIENT_DATA_ACTIONS, member, value));
                case 8 -> okActions((List<String>) SchemaUtils.validateSameMember($SCHEMA_OK_ACTIONS, member, value));
                case 9 -> stateReason((String) SchemaUtils.validateSameMember($SCHEMA_STATE_REASON, member, value));
                case 10 -> stateReasonData((String) SchemaUtils.validateSameMember($SCHEMA_STATE_REASON_DATA, member, value));
                case 11 -> stateUpdatedTimestamp((Instant) SchemaUtils.validateSameMember($SCHEMA_STATE_UPDATED_TIMESTAMP, member, value));
                case 12 -> stateValue((StateValue) SchemaUtils.validateSameMember($SCHEMA_STATE_VALUE, member, value));
                case 13 -> stateTransitionedTimestamp((Instant) SchemaUtils.validateSameMember($SCHEMA_STATE_TRANSITIONED_TIMESTAMP, member, value));
                case 14 -> actionsSuppressedBy((ActionsSuppressedBy) SchemaUtils.validateSameMember($SCHEMA_ACTIONS_SUPPRESSED_BY, member, value));
                case 15 -> actionsSuppressedReason((String) SchemaUtils.validateSameMember($SCHEMA_ACTIONS_SUPPRESSED_REASON, member, value));
                case 16 -> actionsSuppressor((String) SchemaUtils.validateSameMember($SCHEMA_ACTIONS_SUPPRESSOR, member, value));
                case 17 -> actionsSuppressorWaitPeriod((Integer) SchemaUtils.validateSameMember($SCHEMA_ACTIONS_SUPPRESSOR_WAIT_PERIOD, member, value));
                case 18 -> actionsSuppressorExtensionPeriod((Integer) SchemaUtils.validateSameMember($SCHEMA_ACTIONS_SUPPRESSOR_EXTENSION_PERIOD, member, value));
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
                    case 0 -> builder.actionsEnabled(de.readBoolean(member));
                    case 1 -> builder.alarmActions(SharedSerde.deserializeResourceList(member, de));
                    case 2 -> builder.alarmArn(de.readString(member));
                    case 3 -> builder.alarmConfigurationUpdatedTimestamp(de.readTimestamp(member));
                    case 4 -> builder.alarmDescription(de.readString(member));
                    case 5 -> builder.alarmName(de.readString(member));
                    case 6 -> builder.alarmRule(de.readString(member));
                    case 7 -> builder.insufficientDataActions(SharedSerde.deserializeResourceList(member, de));
                    case 8 -> builder.okActions(SharedSerde.deserializeResourceList(member, de));
                    case 9 -> builder.stateReason(de.readString(member));
                    case 10 -> builder.stateReasonData(de.readString(member));
                    case 11 -> builder.stateUpdatedTimestamp(de.readTimestamp(member));
                    case 12 -> builder.stateValue(StateValue.builder().deserializeMember(de, member).build());
                    case 13 -> builder.stateTransitionedTimestamp(de.readTimestamp(member));
                    case 14 -> builder.actionsSuppressedBy(ActionsSuppressedBy.builder().deserializeMember(de, member).build());
                    case 15 -> builder.actionsSuppressedReason(de.readString(member));
                    case 16 -> builder.actionsSuppressor(de.readString(member));
                    case 17 -> builder.actionsSuppressorWaitPeriod(de.readInteger(member));
                    case 18 -> builder.actionsSuppressorExtensionPeriod(de.readInteger(member));
                    default -> throw new IllegalArgumentException("Unexpected member: " + member.memberName());
                }
            }
        }
    }
}
