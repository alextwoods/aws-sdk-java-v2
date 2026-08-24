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

@SmithyGenerated
public final class DescribeAlarmHistoryInput implements SerializableStruct {

    public static final Schema $SCHEMA = Schemas.DESCRIBE_ALARM_HISTORY_INPUT;
    private static final Schema $SCHEMA_ALARM_NAME = $SCHEMA.member("AlarmName");
    private static final Schema $SCHEMA_ALARM_CONTRIBUTOR_ID = $SCHEMA.member("AlarmContributorId");
    private static final Schema $SCHEMA_ALARM_TYPES = $SCHEMA.member("AlarmTypes");
    private static final Schema $SCHEMA_HISTORY_ITEM_TYPE = $SCHEMA.member("HistoryItemType");
    private static final Schema $SCHEMA_START_DATE = $SCHEMA.member("StartDate");
    private static final Schema $SCHEMA_END_DATE = $SCHEMA.member("EndDate");
    private static final Schema $SCHEMA_MAX_RECORDS = $SCHEMA.member("MaxRecords");
    private static final Schema $SCHEMA_NEXT_TOKEN = $SCHEMA.member("NextToken");
    private static final Schema $SCHEMA_SCAN_BY = $SCHEMA.member("ScanBy");

    public static final ShapeId $ID = $SCHEMA.id();

    private final transient String alarmName;
    private final transient String alarmContributorId;
    private final transient List<AlarmType> alarmTypes;
    private final transient HistoryItemType historyItemType;
    private final transient Instant startDate;
    private final transient Instant endDate;
    private final transient Integer maxRecords;
    private final transient String nextToken;
    private final transient ScanBy scanBy;

    private DescribeAlarmHistoryInput(Builder builder) {
        this.alarmName = builder.alarmName;
        this.alarmContributorId = builder.alarmContributorId;
        this.alarmTypes = builder.alarmTypes == null ? null : Collections.unmodifiableList(builder.alarmTypes);
        this.historyItemType = builder.historyItemType;
        this.startDate = builder.startDate;
        this.endDate = builder.endDate;
        this.maxRecords = builder.maxRecords;
        this.nextToken = builder.nextToken;
        this.scanBy = builder.scanBy;
    }

    /**
     * The name of the alarm.
     */
    public String getAlarmName() {
        return alarmName;
    }

    /**
     * The unique identifier of a specific alarm contributor to filter the alarm history results.
     */
    public String getAlarmContributorId() {
        return alarmContributorId;
    }

    /**
     * Use this parameter to specify whether you want the operation to return metric alarms, composite alarms, or log
     * alarms. If you omit this parameter, only metric alarms are returned.
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
     * The type of alarm histories to retrieve.
     */
    public HistoryItemType getHistoryItemType() {
        return historyItemType;
    }

    /**
     * The starting date to retrieve alarm history.
     */
    public Instant getStartDate() {
        return startDate;
    }

    /**
     * The ending date to retrieve alarm history.
     */
    public Instant getEndDate() {
        return endDate;
    }

    /**
     * The maximum number of alarm history records to retrieve.
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

    /**
     * Specified whether to return the newest or oldest alarm history first. Specify <code>TimestampDescending</code> to
     * have the newest event history returned first, and specify <code>TimestampAscending</code> to have the oldest
     * history returned first.
     */
    public ScanBy getScanBy() {
        return scanBy;
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
        DescribeAlarmHistoryInput that = (DescribeAlarmHistoryInput) other;
        return Objects.equals(this.maxRecords, that.maxRecords)
               && Objects.equals(this.alarmName, that.alarmName)
               && Objects.equals(this.alarmContributorId, that.alarmContributorId)
               && Objects.equals(this.nextToken, that.nextToken)
               && Objects.equals(this.historyItemType, that.historyItemType)
               && Objects.equals(this.scanBy, that.scanBy)
               && Objects.equals(this.startDate, that.startDate)
               && Objects.equals(this.endDate, that.endDate)
               && Objects.equals(this.alarmTypes, that.alarmTypes);
    }

    @Override
    public int hashCode() {
        int $hc = Objects.hashCode(alarmName);
        $hc = 31 * $hc + Objects.hashCode(alarmContributorId);
        $hc = 31 * $hc + Objects.hashCode(alarmTypes);
        $hc = 31 * $hc + Objects.hashCode(historyItemType);
        $hc = 31 * $hc + Objects.hashCode(startDate);
        $hc = 31 * $hc + Objects.hashCode(endDate);
        $hc = 31 * $hc + Objects.hashCode(maxRecords);
        $hc = 31 * $hc + Objects.hashCode(nextToken);
        $hc = 31 * $hc + Objects.hashCode(scanBy);
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
        if (alarmContributorId != null) {
            serializer.writeString($SCHEMA_ALARM_CONTRIBUTOR_ID, alarmContributorId);
        }
        if (alarmTypes != null) {
            serializer.writeList($SCHEMA_ALARM_TYPES, alarmTypes, alarmTypes.size(), SharedSerde.AlarmTypesSerializer.INSTANCE);
        }
        if (historyItemType != null) {
            serializer.writeString($SCHEMA_HISTORY_ITEM_TYPE, historyItemType.getValue());
        }
        if (startDate != null) {
            serializer.writeTimestamp($SCHEMA_START_DATE, startDate);
        }
        if (endDate != null) {
            serializer.writeTimestamp($SCHEMA_END_DATE, endDate);
        }
        if (maxRecords != null) {
            serializer.writeInteger($SCHEMA_MAX_RECORDS, maxRecords);
        }
        if (nextToken != null) {
            serializer.writeString($SCHEMA_NEXT_TOKEN, nextToken);
        }
        if (scanBy != null) {
            serializer.writeString($SCHEMA_SCAN_BY, scanBy.getValue());
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T getMemberValue(Schema member) {
        return switch (member.memberIndex()) {
            case 0 -> (T) SchemaUtils.validateSameMember($SCHEMA_ALARM_NAME, member, alarmName);
            case 1 -> (T) SchemaUtils.validateSameMember($SCHEMA_ALARM_CONTRIBUTOR_ID, member, alarmContributorId);
            case 2 -> (T) SchemaUtils.validateSameMember($SCHEMA_ALARM_TYPES, member, alarmTypes);
            case 3 -> (T) SchemaUtils.validateSameMember($SCHEMA_HISTORY_ITEM_TYPE, member, historyItemType);
            case 4 -> (T) SchemaUtils.validateSameMember($SCHEMA_START_DATE, member, startDate);
            case 5 -> (T) SchemaUtils.validateSameMember($SCHEMA_END_DATE, member, endDate);
            case 6 -> (T) SchemaUtils.validateSameMember($SCHEMA_MAX_RECORDS, member, maxRecords);
            case 7 -> (T) SchemaUtils.validateSameMember($SCHEMA_NEXT_TOKEN, member, nextToken);
            case 8 -> (T) SchemaUtils.validateSameMember($SCHEMA_SCAN_BY, member, scanBy);
            default -> throw new IllegalArgumentException("Attempted to get non-existent member: " + member.id());
        };
    }

    /**
     * Create a new builder containing all the current property values of this object.
     *
     * <p><strong>Note:</strong> This method performs only a shallow copy of the original properties.
     *
     * @return a builder for {@link DescribeAlarmHistoryInput}.
     */
    public Builder toBuilder() {
        var builder = new Builder();
        builder.alarmName(this.alarmName);
        builder.alarmContributorId(this.alarmContributorId);
        builder.alarmTypes(this.alarmTypes);
        builder.historyItemType(this.historyItemType);
        builder.startDate(this.startDate);
        builder.endDate(this.endDate);
        builder.maxRecords(this.maxRecords);
        builder.nextToken(this.nextToken);
        builder.scanBy(this.scanBy);
        return builder;
    }

    /**
     * @return returns a new Builder.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for {@link DescribeAlarmHistoryInput}.
     */
    public static final class Builder implements ShapeBuilder<DescribeAlarmHistoryInput> {
        private String alarmName;
        private String alarmContributorId;
        private List<AlarmType> alarmTypes;
        private HistoryItemType historyItemType;
        private Instant startDate;
        private Instant endDate;
        private Integer maxRecords;
        private String nextToken;
        private ScanBy scanBy;

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
         * The unique identifier of a specific alarm contributor to filter the alarm history results.
         *
         * @return this builder.
         */
        public Builder alarmContributorId(String alarmContributorId) {
            this.alarmContributorId = alarmContributorId;
            return this;
        }

        /**
         * Use this parameter to specify whether you want the operation to return metric alarms, composite alarms, or log
         * alarms. If you omit this parameter, only metric alarms are returned.
         *
         * @return this builder.
         */
        public Builder alarmTypes(List<AlarmType> alarmTypes) {
            this.alarmTypes = alarmTypes;
            return this;
        }

        /**
         * The type of alarm histories to retrieve.
         *
         * @return this builder.
         */
        public Builder historyItemType(HistoryItemType historyItemType) {
            this.historyItemType = historyItemType;
            return this;
        }

        /**
         * The starting date to retrieve alarm history.
         *
         * @return this builder.
         */
        public Builder startDate(Instant startDate) {
            this.startDate = startDate;
            return this;
        }

        /**
         * The ending date to retrieve alarm history.
         *
         * @return this builder.
         */
        public Builder endDate(Instant endDate) {
            this.endDate = endDate;
            return this;
        }

        /**
         * The maximum number of alarm history records to retrieve.
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

        /**
         * Specified whether to return the newest or oldest alarm history first. Specify <code>TimestampDescending</code> to
         * have the newest event history returned first, and specify <code>TimestampAscending</code> to have the oldest
         * history returned first.
         *
         * @return this builder.
         */
        public Builder scanBy(ScanBy scanBy) {
            this.scanBy = scanBy;
            return this;
        }

        @Override
        public DescribeAlarmHistoryInput build() {
            return new DescribeAlarmHistoryInput(this);
        }

        @Override
        @SuppressWarnings("unchecked")
        public void setMemberValue(Schema member, Object value) {
            switch (member.memberIndex()) {
                case 0 -> alarmName((String) SchemaUtils.validateSameMember($SCHEMA_ALARM_NAME, member, value));
                case 1 -> alarmContributorId((String) SchemaUtils.validateSameMember($SCHEMA_ALARM_CONTRIBUTOR_ID, member, value));
                case 2 -> alarmTypes((List<AlarmType>) SchemaUtils.validateSameMember($SCHEMA_ALARM_TYPES, member, value));
                case 3 -> historyItemType((HistoryItemType) SchemaUtils.validateSameMember($SCHEMA_HISTORY_ITEM_TYPE, member, value));
                case 4 -> startDate((Instant) SchemaUtils.validateSameMember($SCHEMA_START_DATE, member, value));
                case 5 -> endDate((Instant) SchemaUtils.validateSameMember($SCHEMA_END_DATE, member, value));
                case 6 -> maxRecords((Integer) SchemaUtils.validateSameMember($SCHEMA_MAX_RECORDS, member, value));
                case 7 -> nextToken((String) SchemaUtils.validateSameMember($SCHEMA_NEXT_TOKEN, member, value));
                case 8 -> scanBy((ScanBy) SchemaUtils.validateSameMember($SCHEMA_SCAN_BY, member, value));
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
                    case 1 -> builder.alarmContributorId(de.readString(member));
                    case 2 -> builder.alarmTypes(SharedSerde.deserializeAlarmTypes(member, de));
                    case 3 -> builder.historyItemType(HistoryItemType.builder().deserializeMember(de, member).build());
                    case 4 -> builder.startDate(de.readTimestamp(member));
                    case 5 -> builder.endDate(de.readTimestamp(member));
                    case 6 -> builder.maxRecords(de.readInteger(member));
                    case 7 -> builder.nextToken(de.readString(member));
                    case 8 -> builder.scanBy(ScanBy.builder().deserializeMember(de, member).build());
                    default -> throw new IllegalArgumentException("Unexpected member: " + member.memberName());
                }
            }
        }
    }
}
