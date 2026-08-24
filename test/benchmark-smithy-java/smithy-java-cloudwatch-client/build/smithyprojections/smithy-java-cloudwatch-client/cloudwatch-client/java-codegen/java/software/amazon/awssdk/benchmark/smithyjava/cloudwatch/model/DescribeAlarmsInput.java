package software.amazon.awssdk.benchmark.smithyjava.cloudwatch.model;

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

@SmithyGenerated
public final class DescribeAlarmsInput implements SerializableStruct {

    public static final Schema $SCHEMA = Schemas.DESCRIBE_ALARMS_INPUT;
    private static final Schema $SCHEMA_ALARM_NAMES = $SCHEMA.member("AlarmNames");
    private static final Schema $SCHEMA_ALARM_NAME_PREFIX = $SCHEMA.member("AlarmNamePrefix");
    private static final Schema $SCHEMA_ALARM_TYPES = $SCHEMA.member("AlarmTypes");
    private static final Schema $SCHEMA_CHILDREN_OF_ALARM_NAME = $SCHEMA.member("ChildrenOfAlarmName");
    private static final Schema $SCHEMA_PARENTS_OF_ALARM_NAME = $SCHEMA.member("ParentsOfAlarmName");
    private static final Schema $SCHEMA_STATE_VALUE = $SCHEMA.member("StateValue");
    private static final Schema $SCHEMA_ACTION_PREFIX = $SCHEMA.member("ActionPrefix");
    private static final Schema $SCHEMA_MAX_RECORDS = $SCHEMA.member("MaxRecords");
    private static final Schema $SCHEMA_NEXT_TOKEN = $SCHEMA.member("NextToken");

    public static final ShapeId $ID = $SCHEMA.id();

    private final transient List<String> alarmNames;
    private final transient String alarmNamePrefix;
    private final transient List<AlarmType> alarmTypes;
    private final transient String childrenOfAlarmName;
    private final transient String parentsOfAlarmName;
    private final transient StateValue stateValue;
    private final transient String actionPrefix;
    private final transient Integer maxRecords;
    private final transient String nextToken;

    private DescribeAlarmsInput(Builder builder) {
        this.alarmNames = builder.alarmNames == null ? null : Collections.unmodifiableList(builder.alarmNames);
        this.alarmNamePrefix = builder.alarmNamePrefix;
        this.alarmTypes = builder.alarmTypes == null ? null : Collections.unmodifiableList(builder.alarmTypes);
        this.childrenOfAlarmName = builder.childrenOfAlarmName;
        this.parentsOfAlarmName = builder.parentsOfAlarmName;
        this.stateValue = builder.stateValue;
        this.actionPrefix = builder.actionPrefix;
        this.maxRecords = builder.maxRecords;
        this.nextToken = builder.nextToken;
    }

    /**
     * The names of the alarms to retrieve information about.
     */
    public List<String> getAlarmNames() {
        if (alarmNames == null) {
            return Collections.emptyList();
        }
        return alarmNames;
    }

    public boolean hasAlarmNames() {
        return alarmNames != null;
    }

    /**
     * An alarm name prefix. If you specify this parameter, you receive information about all alarms that have names
     * that start with this prefix.
     *
     * <p>If this parameter is specified, you cannot specify <code>AlarmNames</code>.
     */
    public String getAlarmNamePrefix() {
        return alarmNamePrefix;
    }

    /**
     * Use this parameter to specify whether you want the operation to return metric alarms, composite alarms, or log
     * alarms. If you omit this parameter, only metric alarms are returned, even if composite alarms or log alarms exist
     * in the account.
     *
     * <p>For example, if you omit this parameter or specify <code>MetricAlarms</code>, the operation returns only a
     * list of metric alarms. It does not return any composite alarms or log alarms, even if they exist in the account.
     *
     * <p>If you specify <code>CompositeAlarms</code>, the operation returns only a list of composite alarms, and does
     * not return any metric alarms or log alarms.
     *
     * <p>If you specify <code>LogAlarms</code>, the operation returns only a list of log alarms, and does not return
     * any metric alarms or composite alarms.
     */
    public List<AlarmType> getAlarmTypes() {
        if (alarmTypes == null) {
            return Collections.emptyList();
        }
        return alarmTypes;
    }

    public boolean hasAlarmTypes() {
        return alarmTypes != null;
    }

    /**
     * If you use this parameter and specify the name of a composite alarm, the operation returns information about the
     * "children" alarms of the alarm you specify. These are the metric alarms and composite alarms referenced in the <code>
     * AlarmRule</code> field of the composite alarm that you specify in <code>ChildrenOfAlarmName</code>. Information
     * about the composite alarm that you name in <code>ChildrenOfAlarmName</code> is not returned.
     *
     * <p>If you specify <code>ChildrenOfAlarmName</code>, you cannot specify any other parameters in the request except
     * for <code>MaxRecords</code> and <code>NextToken</code>. If you do so, you receive a validation error.
     *
     * <p>Only the <code>Alarm Name</code>, <code>ARN</code>, <code>StateValue</code> (OK/ALARM/INSUFFICIENT_DATA), and <code>
     * StateUpdatedTimestamp</code> information are returned by this operation when you use this parameter. To get
     * complete information about these alarms, perform another <code>DescribeAlarms</code> operation and specify the
     * parent alarm names in the <code>AlarmNames</code> parameter.
     */
    public String getChildrenOfAlarmName() {
        return childrenOfAlarmName;
    }

    /**
     * If you use this parameter and specify the name of a metric or composite alarm, the operation returns information
     * about the "parent" alarms of the alarm you specify. These are the composite alarms that have <code>AlarmRule</code>
     * parameters that reference the alarm named in <code>ParentsOfAlarmName</code>. Information about the alarm that
     * you specify in <code>ParentsOfAlarmName</code> is not returned.
     *
     * <p>If you specify <code>ParentsOfAlarmName</code>, you cannot specify any other parameters in the request except
     * for <code>MaxRecords</code> and <code>NextToken</code>. If you do so, you receive a validation error.
     *
     * <p>Only the Alarm Name and ARN are returned by this operation when you use this parameter. To get complete
     * information about these alarms, perform another <code>DescribeAlarms</code> operation and specify the parent
     * alarm names in the <code>AlarmNames</code> parameter.
     */
    public String getParentsOfAlarmName() {
        return parentsOfAlarmName;
    }

    /**
     * Specify this parameter to receive information only about alarms that are currently in the state that you specify.
     */
    public StateValue getStateValue() {
        return stateValue;
    }

    /**
     * Use this parameter to filter the results of the operation to only those alarms that use a certain alarm action.
     * For example, you could specify the ARN of an SNS topic to find all alarms that send notifications to that topic.
     */
    public String getActionPrefix() {
        return actionPrefix;
    }

    /**
     * The maximum number of alarm descriptions to retrieve.
     */
    public Integer getMaxRecords() {
        return maxRecords;
    }

    /**
     * The token returned by a previous call to indicate that there is more data available.
     */
    public String getNextToken() {
        return nextToken;
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
        DescribeAlarmsInput that = (DescribeAlarmsInput) other;
        return Objects.equals(this.maxRecords, that.maxRecords)
               && Objects.equals(this.alarmNamePrefix, that.alarmNamePrefix)
               && Objects.equals(this.childrenOfAlarmName, that.childrenOfAlarmName)
               && Objects.equals(this.parentsOfAlarmName, that.parentsOfAlarmName)
               && Objects.equals(this.actionPrefix, that.actionPrefix)
               && Objects.equals(this.nextToken, that.nextToken)
               && Objects.equals(this.stateValue, that.stateValue)
               && Objects.equals(this.alarmNames, that.alarmNames)
               && Objects.equals(this.alarmTypes, that.alarmTypes);
    }

    @Override
    public int hashCode() {
        int $hc = Objects.hashCode(alarmNames);
        $hc = 31 * $hc + Objects.hashCode(alarmNamePrefix);
        $hc = 31 * $hc + Objects.hashCode(alarmTypes);
        $hc = 31 * $hc + Objects.hashCode(childrenOfAlarmName);
        $hc = 31 * $hc + Objects.hashCode(parentsOfAlarmName);
        $hc = 31 * $hc + Objects.hashCode(stateValue);
        $hc = 31 * $hc + Objects.hashCode(actionPrefix);
        $hc = 31 * $hc + Objects.hashCode(maxRecords);
        $hc = 31 * $hc + Objects.hashCode(nextToken);
        return $hc;
    }

    @Override
    public Schema schema() {
        return $SCHEMA;
    }

    @Override
    public void serializeMembers(ShapeSerializer serializer) {
        if (alarmNames != null) {
            serializer.writeList($SCHEMA_ALARM_NAMES, alarmNames, alarmNames.size(), SharedSerde.AlarmNamesSerializer.INSTANCE);
        }
        if (alarmNamePrefix != null) {
            serializer.writeString($SCHEMA_ALARM_NAME_PREFIX, alarmNamePrefix);
        }
        if (alarmTypes != null) {
            serializer.writeList($SCHEMA_ALARM_TYPES, alarmTypes, alarmTypes.size(), SharedSerde.AlarmTypesSerializer.INSTANCE);
        }
        if (childrenOfAlarmName != null) {
            serializer.writeString($SCHEMA_CHILDREN_OF_ALARM_NAME, childrenOfAlarmName);
        }
        if (parentsOfAlarmName != null) {
            serializer.writeString($SCHEMA_PARENTS_OF_ALARM_NAME, parentsOfAlarmName);
        }
        if (stateValue != null) {
            serializer.writeString($SCHEMA_STATE_VALUE, stateValue.getValue());
        }
        if (actionPrefix != null) {
            serializer.writeString($SCHEMA_ACTION_PREFIX, actionPrefix);
        }
        if (maxRecords != null) {
            serializer.writeInteger($SCHEMA_MAX_RECORDS, maxRecords);
        }
        if (nextToken != null) {
            serializer.writeString($SCHEMA_NEXT_TOKEN, nextToken);
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T getMemberValue(Schema member) {
        return switch (member.memberIndex()) {
            case 0 -> (T) SchemaUtils.validateSameMember($SCHEMA_ALARM_NAMES, member, alarmNames);
            case 1 -> (T) SchemaUtils.validateSameMember($SCHEMA_ALARM_NAME_PREFIX, member, alarmNamePrefix);
            case 2 -> (T) SchemaUtils.validateSameMember($SCHEMA_ALARM_TYPES, member, alarmTypes);
            case 3 -> (T) SchemaUtils.validateSameMember($SCHEMA_CHILDREN_OF_ALARM_NAME, member, childrenOfAlarmName);
            case 4 -> (T) SchemaUtils.validateSameMember($SCHEMA_PARENTS_OF_ALARM_NAME, member, parentsOfAlarmName);
            case 5 -> (T) SchemaUtils.validateSameMember($SCHEMA_STATE_VALUE, member, stateValue);
            case 6 -> (T) SchemaUtils.validateSameMember($SCHEMA_ACTION_PREFIX, member, actionPrefix);
            case 7 -> (T) SchemaUtils.validateSameMember($SCHEMA_MAX_RECORDS, member, maxRecords);
            case 8 -> (T) SchemaUtils.validateSameMember($SCHEMA_NEXT_TOKEN, member, nextToken);
            default -> throw new IllegalArgumentException("Attempted to get non-existent member: " + member.id());
        };
    }

    /**
     * Create a new builder containing all the current property values of this object.
     *
     * <p><strong>Note:</strong> This method performs only a shallow copy of the original properties.
     *
     * @return a builder for {@link DescribeAlarmsInput}.
     */
    public Builder toBuilder() {
        var builder = new Builder();
        builder.alarmNames(this.alarmNames);
        builder.alarmNamePrefix(this.alarmNamePrefix);
        builder.alarmTypes(this.alarmTypes);
        builder.childrenOfAlarmName(this.childrenOfAlarmName);
        builder.parentsOfAlarmName(this.parentsOfAlarmName);
        builder.stateValue(this.stateValue);
        builder.actionPrefix(this.actionPrefix);
        builder.maxRecords(this.maxRecords);
        builder.nextToken(this.nextToken);
        return builder;
    }

    /**
     * @return returns a new Builder.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for {@link DescribeAlarmsInput}.
     */
    public static final class Builder implements ShapeBuilder<DescribeAlarmsInput> {
        private List<String> alarmNames;
        private String alarmNamePrefix;
        private List<AlarmType> alarmTypes;
        private String childrenOfAlarmName;
        private String parentsOfAlarmName;
        private StateValue stateValue;
        private String actionPrefix;
        private Integer maxRecords;
        private String nextToken;

        private Builder() {}

        @Override
        public Schema schema() {
            return $SCHEMA;
        }

        /**
         * The names of the alarms to retrieve information about.
         *
         * @return this builder.
         */
        public Builder alarmNames(List<String> alarmNames) {
            this.alarmNames = alarmNames;
            return this;
        }

        /**
         * An alarm name prefix. If you specify this parameter, you receive information about all alarms that have names
         * that start with this prefix.
         *
         * <p>If this parameter is specified, you cannot specify <code>AlarmNames</code>.
         *
         * @return this builder.
         */
        public Builder alarmNamePrefix(String alarmNamePrefix) {
            this.alarmNamePrefix = alarmNamePrefix;
            return this;
        }

        /**
         * Use this parameter to specify whether you want the operation to return metric alarms, composite alarms, or log
         * alarms. If you omit this parameter, only metric alarms are returned, even if composite alarms or log alarms exist
         * in the account.
         *
         * <p>For example, if you omit this parameter or specify <code>MetricAlarms</code>, the operation returns only a
         * list of metric alarms. It does not return any composite alarms or log alarms, even if they exist in the account.
         *
         * <p>If you specify <code>CompositeAlarms</code>, the operation returns only a list of composite alarms, and does
         * not return any metric alarms or log alarms.
         *
         * <p>If you specify <code>LogAlarms</code>, the operation returns only a list of log alarms, and does not return
         * any metric alarms or composite alarms.
         *
         * @return this builder.
         */
        public Builder alarmTypes(List<AlarmType> alarmTypes) {
            this.alarmTypes = alarmTypes;
            return this;
        }

        /**
         * If you use this parameter and specify the name of a composite alarm, the operation returns information about the
         * "children" alarms of the alarm you specify. These are the metric alarms and composite alarms referenced in the <code>
         * AlarmRule</code> field of the composite alarm that you specify in <code>ChildrenOfAlarmName</code>. Information
         * about the composite alarm that you name in <code>ChildrenOfAlarmName</code> is not returned.
         *
         * <p>If you specify <code>ChildrenOfAlarmName</code>, you cannot specify any other parameters in the request except
         * for <code>MaxRecords</code> and <code>NextToken</code>. If you do so, you receive a validation error.
         *
         * <p>Only the <code>Alarm Name</code>, <code>ARN</code>, <code>StateValue</code> (OK/ALARM/INSUFFICIENT_DATA), and <code>
         * StateUpdatedTimestamp</code> information are returned by this operation when you use this parameter. To get
         * complete information about these alarms, perform another <code>DescribeAlarms</code> operation and specify the
         * parent alarm names in the <code>AlarmNames</code> parameter.
         *
         * @return this builder.
         */
        public Builder childrenOfAlarmName(String childrenOfAlarmName) {
            this.childrenOfAlarmName = childrenOfAlarmName;
            return this;
        }

        /**
         * If you use this parameter and specify the name of a metric or composite alarm, the operation returns information
         * about the "parent" alarms of the alarm you specify. These are the composite alarms that have <code>AlarmRule</code>
         * parameters that reference the alarm named in <code>ParentsOfAlarmName</code>. Information about the alarm that
         * you specify in <code>ParentsOfAlarmName</code> is not returned.
         *
         * <p>If you specify <code>ParentsOfAlarmName</code>, you cannot specify any other parameters in the request except
         * for <code>MaxRecords</code> and <code>NextToken</code>. If you do so, you receive a validation error.
         *
         * <p>Only the Alarm Name and ARN are returned by this operation when you use this parameter. To get complete
         * information about these alarms, perform another <code>DescribeAlarms</code> operation and specify the parent
         * alarm names in the <code>AlarmNames</code> parameter.
         *
         * @return this builder.
         */
        public Builder parentsOfAlarmName(String parentsOfAlarmName) {
            this.parentsOfAlarmName = parentsOfAlarmName;
            return this;
        }

        /**
         * Specify this parameter to receive information only about alarms that are currently in the state that you specify.
         *
         * @return this builder.
         */
        public Builder stateValue(StateValue stateValue) {
            this.stateValue = stateValue;
            return this;
        }

        /**
         * Use this parameter to filter the results of the operation to only those alarms that use a certain alarm action.
         * For example, you could specify the ARN of an SNS topic to find all alarms that send notifications to that topic.
         *
         * @return this builder.
         */
        public Builder actionPrefix(String actionPrefix) {
            this.actionPrefix = actionPrefix;
            return this;
        }

        /**
         * The maximum number of alarm descriptions to retrieve.
         *
         * @return this builder.
         */
        public Builder maxRecords(Integer maxRecords) {
            this.maxRecords = maxRecords;
            return this;
        }

        /**
         * The token returned by a previous call to indicate that there is more data available.
         *
         * @return this builder.
         */
        public Builder nextToken(String nextToken) {
            this.nextToken = nextToken;
            return this;
        }

        @Override
        public DescribeAlarmsInput build() {
            return new DescribeAlarmsInput(this);
        }

        @Override
        @SuppressWarnings("unchecked")
        public void setMemberValue(Schema member, Object value) {
            switch (member.memberIndex()) {
                case 0 -> alarmNames((List<String>) SchemaUtils.validateSameMember($SCHEMA_ALARM_NAMES, member, value));
                case 1 -> alarmNamePrefix((String) SchemaUtils.validateSameMember($SCHEMA_ALARM_NAME_PREFIX, member, value));
                case 2 -> alarmTypes((List<AlarmType>) SchemaUtils.validateSameMember($SCHEMA_ALARM_TYPES, member, value));
                case 3 -> childrenOfAlarmName((String) SchemaUtils.validateSameMember($SCHEMA_CHILDREN_OF_ALARM_NAME, member, value));
                case 4 -> parentsOfAlarmName((String) SchemaUtils.validateSameMember($SCHEMA_PARENTS_OF_ALARM_NAME, member, value));
                case 5 -> stateValue((StateValue) SchemaUtils.validateSameMember($SCHEMA_STATE_VALUE, member, value));
                case 6 -> actionPrefix((String) SchemaUtils.validateSameMember($SCHEMA_ACTION_PREFIX, member, value));
                case 7 -> maxRecords((Integer) SchemaUtils.validateSameMember($SCHEMA_MAX_RECORDS, member, value));
                case 8 -> nextToken((String) SchemaUtils.validateSameMember($SCHEMA_NEXT_TOKEN, member, value));
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
                    case 0 -> builder.alarmNames(SharedSerde.deserializeAlarmNames(member, de));
                    case 1 -> builder.alarmNamePrefix(de.readString(member));
                    case 2 -> builder.alarmTypes(SharedSerde.deserializeAlarmTypes(member, de));
                    case 3 -> builder.childrenOfAlarmName(de.readString(member));
                    case 4 -> builder.parentsOfAlarmName(de.readString(member));
                    case 5 -> builder.stateValue(StateValue.builder().deserializeMember(de, member).build());
                    case 6 -> builder.actionPrefix(de.readString(member));
                    case 7 -> builder.maxRecords(de.readInteger(member));
                    case 8 -> builder.nextToken(de.readString(member));
                    default -> throw new IllegalArgumentException("Unexpected member: " + member.memberName());
                }
            }
        }
    }
}
