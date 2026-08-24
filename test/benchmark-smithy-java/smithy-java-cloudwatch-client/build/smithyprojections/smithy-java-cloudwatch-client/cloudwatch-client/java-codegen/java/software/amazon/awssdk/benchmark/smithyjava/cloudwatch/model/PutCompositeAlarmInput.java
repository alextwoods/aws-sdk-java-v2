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
public final class PutCompositeAlarmInput implements SerializableStruct {

    public static final Schema $SCHEMA = Schemas.PUT_COMPOSITE_ALARM_INPUT;
    private static final Schema $SCHEMA_ACTIONS_ENABLED = $SCHEMA.member("ActionsEnabled");
    private static final Schema $SCHEMA_ALARM_ACTIONS = $SCHEMA.member("AlarmActions");
    private static final Schema $SCHEMA_ALARM_DESCRIPTION = $SCHEMA.member("AlarmDescription");
    private static final Schema $SCHEMA_ALARM_NAME = $SCHEMA.member("AlarmName");
    private static final Schema $SCHEMA_ALARM_RULE = $SCHEMA.member("AlarmRule");
    private static final Schema $SCHEMA_INSUFFICIENT_DATA_ACTIONS = $SCHEMA.member("InsufficientDataActions");
    private static final Schema $SCHEMA_OK_ACTIONS = $SCHEMA.member("OKActions");
    private static final Schema $SCHEMA_TAGS = $SCHEMA.member("Tags");
    private static final Schema $SCHEMA_ACTIONS_SUPPRESSOR = $SCHEMA.member("ActionsSuppressor");
    private static final Schema $SCHEMA_ACTIONS_SUPPRESSOR_WAIT_PERIOD = $SCHEMA.member("ActionsSuppressorWaitPeriod");
    private static final Schema $SCHEMA_ACTIONS_SUPPRESSOR_EXTENSION_PERIOD = $SCHEMA.member("ActionsSuppressorExtensionPeriod");

    public static final ShapeId $ID = $SCHEMA.id();

    private final transient Boolean actionsEnabled;
    private final transient List<String> alarmActions;
    private final transient String alarmDescription;
    private final transient String alarmName;
    private final transient String alarmRule;
    private final transient List<String> insufficientDataActions;
    private final transient List<String> okActions;
    private final transient List<Tag> tags;
    private final transient String actionsSuppressor;
    private final transient Integer actionsSuppressorWaitPeriod;
    private final transient Integer actionsSuppressorExtensionPeriod;

    private PutCompositeAlarmInput(Builder builder) {
        this.actionsEnabled = builder.actionsEnabled;
        this.alarmActions = builder.alarmActions == null ? null : Collections.unmodifiableList(builder.alarmActions);
        this.alarmDescription = builder.alarmDescription;
        this.alarmName = builder.alarmName;
        this.alarmRule = builder.alarmRule;
        this.insufficientDataActions = builder.insufficientDataActions == null ? null : Collections.unmodifiableList(builder.insufficientDataActions);
        this.okActions = builder.okActions == null ? null : Collections.unmodifiableList(builder.okActions);
        this.tags = builder.tags == null ? null : Collections.unmodifiableList(builder.tags);
        this.actionsSuppressor = builder.actionsSuppressor;
        this.actionsSuppressorWaitPeriod = builder.actionsSuppressorWaitPeriod;
        this.actionsSuppressorExtensionPeriod = builder.actionsSuppressorExtensionPeriod;
    }

    /**
     * Indicates whether actions should be executed during any changes to the alarm state of the composite alarm. The
     * default is <code>TRUE</code>.
     */
    public Boolean isActionsEnabled() {
        return actionsEnabled;
    }

    /**
     * The actions to execute when this alarm transitions to the <code>ALARM</code> state from any other state. Each
     * action is specified as an Amazon Resource Name (ARN).
     *
     * <p>Valid Values: ]
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
     * The description for the composite alarm.
     */
    public String getAlarmDescription() {
        return alarmDescription;
    }

    /**
     * The name for the composite alarm. This name must be unique within the Region.
     */
    public String getAlarmName() {
        return alarmName;
    }

    /**
     * An expression that specifies which other alarms are to be evaluated to determine this composite alarm's state.
     * For each alarm that you reference, you designate a function that specifies whether that alarm needs to be in
     * ALARM state, OK state, or INSUFFICIENT_DATA state. You can use operators (AND, OR and NOT) to combine multiple
     * functions in a single expression. You can use parenthesis to logically group the functions in your expression.
     *
     * <p>You can use either alarm names or ARNs to reference the other alarms that are to be evaluated.
     *
     * <p>Functions can include the following:
     *
     * <ul>
     *   <li>
     *     <code>ALARM("<i>alarm-name</i> or <i>alarm-ARN</i>")</code> is TRUE if the named alarm is in ALARM state.
     *   </li>
     *   <li>
     *     <code>OK("<i>alarm-name</i> or <i>alarm-ARN</i>")</code> is TRUE if the named alarm is in OK state.
     *   </li>
     *   <li>
     *     <code>INSUFFICIENT_DATA("<i>alarm-name</i> or <i>alarm-ARN</i>")</code> is TRUE if the named alarm is in
     *     INSUFFICIENT_DATA state.
     *   </li>
     *   <li>
     *     <code>TRUE</code> always evaluates to TRUE.
     *   </li>
     *   <li>
     *     <code>FALSE</code> always evaluates to FALSE.
     *   </li>
     * </ul>
     *
     * <p>TRUE and FALSE are useful for testing a complex <code>AlarmRule</code> structure, and for testing your alarm
     * actions.
     *
     * <p>Alarm names specified in <code>AlarmRule</code> can be surrounded with double-quotes ("), but do not have to
     * be.
     *
     * <p>The following are some examples of <code>AlarmRule</code>:
     *
     * <ul>
     *   <li>
     *     <code>ALARM(CPUUtilizationTooHigh) AND ALARM(DiskReadOpsTooHigh)</code> specifies that the composite
     *     alarm goes into ALARM state only if both CPUUtilizationTooHigh and DiskReadOpsTooHigh alarms are in ALARM
     *     state.
     *   </li>
     *   <li>
     *     <code>ALARM(CPUUtilizationTooHigh) AND NOT ALARM(DeploymentInProgress)</code> specifies that the alarm
     *     goes to ALARM state if CPUUtilizationTooHigh is in ALARM state and DeploymentInProgress is not in ALARM
     *     state. This example reduces alarm noise during a known deployment window.
     *   </li>
     *   <li>
     *     <code>(ALARM(CPUUtilizationTooHigh) OR ALARM(DiskReadOpsTooHigh)) AND OK(NetworkOutTooHigh)</code> goes
     *     into ALARM state if CPUUtilizationTooHigh OR DiskReadOpsTooHigh is in ALARM state, and if
     *     NetworkOutTooHigh is in OK state. This provides another example of using a composite alarm to prevent
     *     noise. This rule ensures that you are not notified with an alarm action on high CPU or disk usage if a
     *     known network problem is also occurring.
     *   </li>
     * </ul>
     *
     * <p>The <code>AlarmRule</code> can specify as many as 100 "children" alarms. The <code>AlarmRule</code> expression
     * can have as many as 500 elements. Elements are child alarms, TRUE or FALSE statements, and parentheses.
     */
    public String getAlarmRule() {
        return alarmRule;
    }

    /**
     * The actions to execute when this alarm transitions to the <code>INSUFFICIENT_DATA</code> state from any other
     * state. Each action is specified as an Amazon Resource Name (ARN).
     *
     * <p>Valid Values: ]
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
     * The actions to execute when this alarm transitions to an <code>OK</code> state from any other state. Each action
     * is specified as an Amazon Resource Name (ARN).
     *
     * <p>Valid Values: ]
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
     * A list of key-value pairs to associate with the alarm. You can associate as many as 50 tags with an alarm. To be
     * able to associate tags with the alarm when you create the alarm, you must have the <code>cloudwatch:TagResource</code>
     * permission.
     *
     * <p>Tags can help you organize and categorize your resources. You can also use them to scope user permissions by
     * granting a user permission to access or change only resources with certain tag values.
     *
     * <p>If you are using this operation to update an existing alarm, any tags you specify in this parameter are
     * ignored. To change the tags of an existing alarm, use <a href="https://docs.aws.amazon.com/AmazonCloudWatch/latest/APIReference/API_TagResource.html">TagResource</a> or <a href="https://docs.aws.amazon.com/AmazonCloudWatch/latest/APIReference/API_UntagResource.html">UntagResource</a>.
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
        PutCompositeAlarmInput that = (PutCompositeAlarmInput) other;
        return Objects.equals(this.actionsEnabled, that.actionsEnabled)
               && Objects.equals(this.actionsSuppressorWaitPeriod, that.actionsSuppressorWaitPeriod)
               && Objects.equals(this.actionsSuppressorExtensionPeriod, that.actionsSuppressorExtensionPeriod)
               && Objects.equals(this.alarmDescription, that.alarmDescription)
               && Objects.equals(this.alarmName, that.alarmName)
               && Objects.equals(this.alarmRule, that.alarmRule)
               && Objects.equals(this.actionsSuppressor, that.actionsSuppressor)
               && Objects.equals(this.alarmActions, that.alarmActions)
               && Objects.equals(this.insufficientDataActions, that.insufficientDataActions)
               && Objects.equals(this.okActions, that.okActions)
               && Objects.equals(this.tags, that.tags);
    }

    @Override
    public int hashCode() {
        int $hc = Objects.hashCode(actionsEnabled);
        $hc = 31 * $hc + Objects.hashCode(alarmActions);
        $hc = 31 * $hc + Objects.hashCode(alarmDescription);
        $hc = 31 * $hc + Objects.hashCode(alarmName);
        $hc = 31 * $hc + Objects.hashCode(alarmRule);
        $hc = 31 * $hc + Objects.hashCode(insufficientDataActions);
        $hc = 31 * $hc + Objects.hashCode(okActions);
        $hc = 31 * $hc + Objects.hashCode(tags);
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
        if (tags != null) {
            serializer.writeList($SCHEMA_TAGS, tags, tags.size(), SharedSerde.TagListSerializer.INSTANCE);
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
            case 0 -> (T) SchemaUtils.validateSameMember($SCHEMA_ALARM_NAME, member, alarmName);
            case 1 -> (T) SchemaUtils.validateSameMember($SCHEMA_ALARM_RULE, member, alarmRule);
            case 2 -> (T) SchemaUtils.validateSameMember($SCHEMA_ACTIONS_ENABLED, member, actionsEnabled);
            case 3 -> (T) SchemaUtils.validateSameMember($SCHEMA_ALARM_ACTIONS, member, alarmActions);
            case 4 -> (T) SchemaUtils.validateSameMember($SCHEMA_ALARM_DESCRIPTION, member, alarmDescription);
            case 5 -> (T) SchemaUtils.validateSameMember($SCHEMA_INSUFFICIENT_DATA_ACTIONS, member, insufficientDataActions);
            case 6 -> (T) SchemaUtils.validateSameMember($SCHEMA_OK_ACTIONS, member, okActions);
            case 7 -> (T) SchemaUtils.validateSameMember($SCHEMA_TAGS, member, tags);
            case 8 -> (T) SchemaUtils.validateSameMember($SCHEMA_ACTIONS_SUPPRESSOR, member, actionsSuppressor);
            case 9 -> (T) SchemaUtils.validateSameMember($SCHEMA_ACTIONS_SUPPRESSOR_WAIT_PERIOD, member, actionsSuppressorWaitPeriod);
            case 10 -> (T) SchemaUtils.validateSameMember($SCHEMA_ACTIONS_SUPPRESSOR_EXTENSION_PERIOD, member, actionsSuppressorExtensionPeriod);
            default -> throw new IllegalArgumentException("Attempted to get non-existent member: " + member.id());
        };
    }

    /**
     * Create a new builder containing all the current property values of this object.
     *
     * <p><strong>Note:</strong> This method performs only a shallow copy of the original properties.
     *
     * @return a builder for {@link PutCompositeAlarmInput}.
     */
    public Builder toBuilder() {
        var builder = new Builder();
        builder.actionsEnabled(this.actionsEnabled);
        builder.alarmActions(this.alarmActions);
        builder.alarmDescription(this.alarmDescription);
        builder.alarmName(this.alarmName);
        builder.alarmRule(this.alarmRule);
        builder.insufficientDataActions(this.insufficientDataActions);
        builder.okActions(this.okActions);
        builder.tags(this.tags);
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
     * Builder for {@link PutCompositeAlarmInput}.
     */
    public static final class Builder implements ShapeBuilder<PutCompositeAlarmInput> {
        private final PresenceTracker tracker = PresenceTracker.of($SCHEMA);
        private Boolean actionsEnabled;
        private List<String> alarmActions;
        private String alarmDescription;
        private String alarmName;
        private String alarmRule;
        private List<String> insufficientDataActions;
        private List<String> okActions;
        private List<Tag> tags;
        private String actionsSuppressor;
        private Integer actionsSuppressorWaitPeriod;
        private Integer actionsSuppressorExtensionPeriod;

        private Builder() {
            // Tell the tracker to assume clientOptional members are present.
            tracker.setMember($SCHEMA_ALARM_NAME);
            tracker.setMember($SCHEMA_ALARM_RULE);
        }

        @Override
        public Schema schema() {
            return $SCHEMA;
        }

        /**
         * Indicates whether actions should be executed during any changes to the alarm state of the composite alarm. The
         * default is <code>TRUE</code>.
         *
         * @return this builder.
         */
        public Builder actionsEnabled(Boolean actionsEnabled) {
            this.actionsEnabled = actionsEnabled;
            return this;
        }

        /**
         * The actions to execute when this alarm transitions to the <code>ALARM</code> state from any other state. Each
         * action is specified as an Amazon Resource Name (ARN).
         *
         * <p>Valid Values: ]
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
         * The description for the composite alarm.
         *
         * @return this builder.
         */
        public Builder alarmDescription(String alarmDescription) {
            this.alarmDescription = alarmDescription;
            return this;
        }

        /**
         * The name for the composite alarm. This name must be unique within the Region.
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
         * An expression that specifies which other alarms are to be evaluated to determine this composite alarm's state.
         * For each alarm that you reference, you designate a function that specifies whether that alarm needs to be in
         * ALARM state, OK state, or INSUFFICIENT_DATA state. You can use operators (AND, OR and NOT) to combine multiple
         * functions in a single expression. You can use parenthesis to logically group the functions in your expression.
         *
         * <p>You can use either alarm names or ARNs to reference the other alarms that are to be evaluated.
         *
         * <p>Functions can include the following:
         *
         * <ul>
         *   <li>
         *     <code>ALARM("<i>alarm-name</i> or <i>alarm-ARN</i>")</code> is TRUE if the named alarm is in ALARM state.
         *   </li>
         *   <li>
         *     <code>OK("<i>alarm-name</i> or <i>alarm-ARN</i>")</code> is TRUE if the named alarm is in OK state.
         *   </li>
         *   <li>
         *     <code>INSUFFICIENT_DATA("<i>alarm-name</i> or <i>alarm-ARN</i>")</code> is TRUE if the named alarm is in
         *     INSUFFICIENT_DATA state.
         *   </li>
         *   <li>
         *     <code>TRUE</code> always evaluates to TRUE.
         *   </li>
         *   <li>
         *     <code>FALSE</code> always evaluates to FALSE.
         *   </li>
         * </ul>
         *
         * <p>TRUE and FALSE are useful for testing a complex <code>AlarmRule</code> structure, and for testing your alarm
         * actions.
         *
         * <p>Alarm names specified in <code>AlarmRule</code> can be surrounded with double-quotes ("), but do not have to
         * be.
         *
         * <p>The following are some examples of <code>AlarmRule</code>:
         *
         * <ul>
         *   <li>
         *     <code>ALARM(CPUUtilizationTooHigh) AND ALARM(DiskReadOpsTooHigh)</code> specifies that the composite
         *     alarm goes into ALARM state only if both CPUUtilizationTooHigh and DiskReadOpsTooHigh alarms are in ALARM
         *     state.
         *   </li>
         *   <li>
         *     <code>ALARM(CPUUtilizationTooHigh) AND NOT ALARM(DeploymentInProgress)</code> specifies that the alarm
         *     goes to ALARM state if CPUUtilizationTooHigh is in ALARM state and DeploymentInProgress is not in ALARM
         *     state. This example reduces alarm noise during a known deployment window.
         *   </li>
         *   <li>
         *     <code>(ALARM(CPUUtilizationTooHigh) OR ALARM(DiskReadOpsTooHigh)) AND OK(NetworkOutTooHigh)</code> goes
         *     into ALARM state if CPUUtilizationTooHigh OR DiskReadOpsTooHigh is in ALARM state, and if
         *     NetworkOutTooHigh is in OK state. This provides another example of using a composite alarm to prevent
         *     noise. This rule ensures that you are not notified with an alarm action on high CPU or disk usage if a
         *     known network problem is also occurring.
         *   </li>
         * </ul>
         *
         * <p>The <code>AlarmRule</code> can specify as many as 100 "children" alarms. The <code>AlarmRule</code> expression
         * can have as many as 500 elements. Elements are child alarms, TRUE or FALSE statements, and parentheses.
         *
         * <p><strong>Required</strong>
         * @return this builder.
         */
        public Builder alarmRule(String alarmRule) {
            this.alarmRule = Objects.requireNonNull(alarmRule, "alarmRule cannot be null");
            tracker.setMember($SCHEMA_ALARM_RULE);
            return this;
        }

        /**
         * The actions to execute when this alarm transitions to the <code>INSUFFICIENT_DATA</code> state from any other
         * state. Each action is specified as an Amazon Resource Name (ARN).
         *
         * <p>Valid Values: ]
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
         * The actions to execute when this alarm transitions to an <code>OK</code> state from any other state. Each action
         * is specified as an Amazon Resource Name (ARN).
         *
         * <p>Valid Values: ]
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
         * @return this builder.
         */
        public Builder tags(List<Tag> tags) {
            this.tags = tags;
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
        public PutCompositeAlarmInput build() {
            tracker.validate();
            return new PutCompositeAlarmInput(this);
        }

        @Override
        @SuppressWarnings("unchecked")
        public void setMemberValue(Schema member, Object value) {
            switch (member.memberIndex()) {
                case 0 -> alarmName((String) SchemaUtils.validateSameMember($SCHEMA_ALARM_NAME, member, value));
                case 1 -> alarmRule((String) SchemaUtils.validateSameMember($SCHEMA_ALARM_RULE, member, value));
                case 2 -> actionsEnabled((Boolean) SchemaUtils.validateSameMember($SCHEMA_ACTIONS_ENABLED, member, value));
                case 3 -> alarmActions((List<String>) SchemaUtils.validateSameMember($SCHEMA_ALARM_ACTIONS, member, value));
                case 4 -> alarmDescription((String) SchemaUtils.validateSameMember($SCHEMA_ALARM_DESCRIPTION, member, value));
                case 5 -> insufficientDataActions((List<String>) SchemaUtils.validateSameMember($SCHEMA_INSUFFICIENT_DATA_ACTIONS, member, value));
                case 6 -> okActions((List<String>) SchemaUtils.validateSameMember($SCHEMA_OK_ACTIONS, member, value));
                case 7 -> tags((List<Tag>) SchemaUtils.validateSameMember($SCHEMA_TAGS, member, value));
                case 8 -> actionsSuppressor((String) SchemaUtils.validateSameMember($SCHEMA_ACTIONS_SUPPRESSOR, member, value));
                case 9 -> actionsSuppressorWaitPeriod((Integer) SchemaUtils.validateSameMember($SCHEMA_ACTIONS_SUPPRESSOR_WAIT_PERIOD, member, value));
                case 10 -> actionsSuppressorExtensionPeriod((Integer) SchemaUtils.validateSameMember($SCHEMA_ACTIONS_SUPPRESSOR_EXTENSION_PERIOD, member, value));
                default -> ShapeBuilder.super.setMemberValue(member, value);
            }
        }

        @Override
        public ShapeBuilder<PutCompositeAlarmInput> errorCorrection() {
            if (tracker.allSet()) {
                return this;
            }
            if (!tracker.checkMember($SCHEMA_ALARM_NAME)) {
                alarmName("");
            }
            if (!tracker.checkMember($SCHEMA_ALARM_RULE)) {
                alarmRule("");
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
                    case 1 -> builder.alarmRule(de.readString(member));
                    case 2 -> builder.actionsEnabled(de.readBoolean(member));
                    case 3 -> builder.alarmActions(SharedSerde.deserializeResourceList(member, de));
                    case 4 -> builder.alarmDescription(de.readString(member));
                    case 5 -> builder.insufficientDataActions(SharedSerde.deserializeResourceList(member, de));
                    case 6 -> builder.okActions(SharedSerde.deserializeResourceList(member, de));
                    case 7 -> builder.tags(SharedSerde.deserializeTagList(member, de));
                    case 8 -> builder.actionsSuppressor(de.readString(member));
                    case 9 -> builder.actionsSuppressorWaitPeriod(de.readInteger(member));
                    case 10 -> builder.actionsSuppressorExtensionPeriod(de.readInteger(member));
                    default -> throw new IllegalArgumentException("Unexpected member: " + member.memberName());
                }
            }
        }
    }
}
