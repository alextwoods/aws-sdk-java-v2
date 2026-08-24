package software.amazon.awssdk.benchmark.smithyjava.cloudwatch.model;

import java.time.Instant;
import java.util.Collections;
import java.util.Map;
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
 * Represents the history of a specific alarm.
 */
@SmithyGenerated
public final class AlarmHistoryItem implements SerializableStruct {

    public static final Schema $SCHEMA = Schemas.ALARM_HISTORY_ITEM;
    private static final Schema $SCHEMA_ALARM_NAME = $SCHEMA.member("AlarmName");
    private static final Schema $SCHEMA_ALARM_CONTRIBUTOR_ID = $SCHEMA.member("AlarmContributorId");
    private static final Schema $SCHEMA_ALARM_TYPE = $SCHEMA.member("AlarmType");
    private static final Schema $SCHEMA_TIMESTAMP = $SCHEMA.member("Timestamp");
    private static final Schema $SCHEMA_HISTORY_ITEM_TYPE = $SCHEMA.member("HistoryItemType");
    private static final Schema $SCHEMA_HISTORY_SUMMARY = $SCHEMA.member("HistorySummary");
    private static final Schema $SCHEMA_HISTORY_DATA = $SCHEMA.member("HistoryData");
    private static final Schema $SCHEMA_ALARM_CONTRIBUTOR_ATTRIBUTES = $SCHEMA.member("AlarmContributorAttributes");

    public static final ShapeId $ID = $SCHEMA.id();

    private final transient String alarmName;
    private final transient String alarmContributorId;
    private final transient AlarmType alarmType;
    private final transient Instant timestamp;
    private final transient HistoryItemType historyItemType;
    private final transient String historySummary;
    private final transient String historyData;
    private final transient Map<String, String> alarmContributorAttributes;

    private AlarmHistoryItem(Builder builder) {
        this.alarmName = builder.alarmName;
        this.alarmContributorId = builder.alarmContributorId;
        this.alarmType = builder.alarmType;
        this.timestamp = builder.timestamp;
        this.historyItemType = builder.historyItemType;
        this.historySummary = builder.historySummary;
        this.historyData = builder.historyData;
        this.alarmContributorAttributes = builder.alarmContributorAttributes == null ? null : Collections.unmodifiableMap(builder.alarmContributorAttributes);
    }

    /**
     * The descriptive name for the alarm.
     */
    public String getAlarmName() {
        return alarmName;
    }

    /**
     * The unique identifier of the alarm contributor associated with this history item, if applicable.
     */
    public String getAlarmContributorId() {
        return alarmContributorId;
    }

    /**
     * The type of alarm, either metric alarm or composite alarm.
     */
    public AlarmType getAlarmType() {
        return alarmType;
    }

    /**
     * The time stamp for the alarm history item.
     */
    public Instant getTimestamp() {
        return timestamp;
    }

    /**
     * The type of alarm history item.
     */
    public HistoryItemType getHistoryItemType() {
        return historyItemType;
    }

    /**
     * A summary of the alarm history, in text format.
     */
    public String getHistorySummary() {
        return historySummary;
    }

    /**
     * Data about the alarm, in JSON format.
     */
    public String getHistoryData() {
        return historyData;
    }

    /**
     * A map of attributes that describe the alarm contributor associated with this history item, providing context
     * about the contributor's characteristics at the time of the event.
     */
    public Map<String, String> getAlarmContributorAttributes() {
        if (alarmContributorAttributes == null) {
            return Collections.emptyMap();
        }
        return alarmContributorAttributes;
    }

    public boolean hasAlarmContributorAttributes() {
        return alarmContributorAttributes != null;
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
        AlarmHistoryItem that = (AlarmHistoryItem) other;
        return Objects.equals(this.alarmName, that.alarmName)
               && Objects.equals(this.alarmContributorId, that.alarmContributorId)
               && Objects.equals(this.historySummary, that.historySummary)
               && Objects.equals(this.historyData, that.historyData)
               && Objects.equals(this.alarmType, that.alarmType)
               && Objects.equals(this.historyItemType, that.historyItemType)
               && Objects.equals(this.timestamp, that.timestamp)
               && Objects.equals(this.alarmContributorAttributes, that.alarmContributorAttributes);
    }

    @Override
    public int hashCode() {
        int $hc = Objects.hashCode(alarmName);
        $hc = 31 * $hc + Objects.hashCode(alarmContributorId);
        $hc = 31 * $hc + Objects.hashCode(alarmType);
        $hc = 31 * $hc + Objects.hashCode(timestamp);
        $hc = 31 * $hc + Objects.hashCode(historyItemType);
        $hc = 31 * $hc + Objects.hashCode(historySummary);
        $hc = 31 * $hc + Objects.hashCode(historyData);
        $hc = 31 * $hc + Objects.hashCode(alarmContributorAttributes);
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
        if (alarmType != null) {
            serializer.writeString($SCHEMA_ALARM_TYPE, alarmType.getValue());
        }
        if (timestamp != null) {
            serializer.writeTimestamp($SCHEMA_TIMESTAMP, timestamp);
        }
        if (historyItemType != null) {
            serializer.writeString($SCHEMA_HISTORY_ITEM_TYPE, historyItemType.getValue());
        }
        if (historySummary != null) {
            serializer.writeString($SCHEMA_HISTORY_SUMMARY, historySummary);
        }
        if (historyData != null) {
            serializer.writeString($SCHEMA_HISTORY_DATA, historyData);
        }
        if (alarmContributorAttributes != null) {
            serializer.writeMap($SCHEMA_ALARM_CONTRIBUTOR_ATTRIBUTES, alarmContributorAttributes, alarmContributorAttributes.size(), SharedSerde.ContributorAttributesSerializer.INSTANCE);
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T getMemberValue(Schema member) {
        return switch (member.memberIndex()) {
            case 0 -> (T) SchemaUtils.validateSameMember($SCHEMA_ALARM_NAME, member, alarmName);
            case 1 -> (T) SchemaUtils.validateSameMember($SCHEMA_ALARM_CONTRIBUTOR_ID, member, alarmContributorId);
            case 2 -> (T) SchemaUtils.validateSameMember($SCHEMA_ALARM_TYPE, member, alarmType);
            case 3 -> (T) SchemaUtils.validateSameMember($SCHEMA_TIMESTAMP, member, timestamp);
            case 4 -> (T) SchemaUtils.validateSameMember($SCHEMA_HISTORY_ITEM_TYPE, member, historyItemType);
            case 5 -> (T) SchemaUtils.validateSameMember($SCHEMA_HISTORY_SUMMARY, member, historySummary);
            case 6 -> (T) SchemaUtils.validateSameMember($SCHEMA_HISTORY_DATA, member, historyData);
            case 7 -> (T) SchemaUtils.validateSameMember($SCHEMA_ALARM_CONTRIBUTOR_ATTRIBUTES, member, alarmContributorAttributes);
            default -> throw new IllegalArgumentException("Attempted to get non-existent member: " + member.id());
        };
    }

    /**
     * Create a new builder containing all the current property values of this object.
     *
     * <p><strong>Note:</strong> This method performs only a shallow copy of the original properties.
     *
     * @return a builder for {@link AlarmHistoryItem}.
     */
    public Builder toBuilder() {
        var builder = new Builder();
        builder.alarmName(this.alarmName);
        builder.alarmContributorId(this.alarmContributorId);
        builder.alarmType(this.alarmType);
        builder.timestamp(this.timestamp);
        builder.historyItemType(this.historyItemType);
        builder.historySummary(this.historySummary);
        builder.historyData(this.historyData);
        builder.alarmContributorAttributes(this.alarmContributorAttributes);
        return builder;
    }

    /**
     * @return returns a new Builder.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for {@link AlarmHistoryItem}.
     */
    public static final class Builder implements ShapeBuilder<AlarmHistoryItem> {
        private String alarmName;
        private String alarmContributorId;
        private AlarmType alarmType;
        private Instant timestamp;
        private HistoryItemType historyItemType;
        private String historySummary;
        private String historyData;
        private Map<String, String> alarmContributorAttributes;

        private Builder() {}

        @Override
        public Schema schema() {
            return $SCHEMA;
        }

        /**
         * The descriptive name for the alarm.
         *
         * @return this builder.
         */
        public Builder alarmName(String alarmName) {
            this.alarmName = alarmName;
            return this;
        }

        /**
         * The unique identifier of the alarm contributor associated with this history item, if applicable.
         *
         * @return this builder.
         */
        public Builder alarmContributorId(String alarmContributorId) {
            this.alarmContributorId = alarmContributorId;
            return this;
        }

        /**
         * The type of alarm, either metric alarm or composite alarm.
         *
         * @return this builder.
         */
        public Builder alarmType(AlarmType alarmType) {
            this.alarmType = alarmType;
            return this;
        }

        /**
         * The time stamp for the alarm history item.
         *
         * @return this builder.
         */
        public Builder timestamp(Instant timestamp) {
            this.timestamp = timestamp;
            return this;
        }

        /**
         * The type of alarm history item.
         *
         * @return this builder.
         */
        public Builder historyItemType(HistoryItemType historyItemType) {
            this.historyItemType = historyItemType;
            return this;
        }

        /**
         * A summary of the alarm history, in text format.
         *
         * @return this builder.
         */
        public Builder historySummary(String historySummary) {
            this.historySummary = historySummary;
            return this;
        }

        /**
         * Data about the alarm, in JSON format.
         *
         * @return this builder.
         */
        public Builder historyData(String historyData) {
            this.historyData = historyData;
            return this;
        }

        /**
         * A map of attributes that describe the alarm contributor associated with this history item, providing context
         * about the contributor's characteristics at the time of the event.
         *
         * @return this builder.
         */
        public Builder alarmContributorAttributes(Map<String, String> alarmContributorAttributes) {
            this.alarmContributorAttributes = alarmContributorAttributes;
            return this;
        }

        @Override
        public AlarmHistoryItem build() {
            return new AlarmHistoryItem(this);
        }

        @Override
        @SuppressWarnings("unchecked")
        public void setMemberValue(Schema member, Object value) {
            switch (member.memberIndex()) {
                case 0 -> alarmName((String) SchemaUtils.validateSameMember($SCHEMA_ALARM_NAME, member, value));
                case 1 -> alarmContributorId((String) SchemaUtils.validateSameMember($SCHEMA_ALARM_CONTRIBUTOR_ID, member, value));
                case 2 -> alarmType((AlarmType) SchemaUtils.validateSameMember($SCHEMA_ALARM_TYPE, member, value));
                case 3 -> timestamp((Instant) SchemaUtils.validateSameMember($SCHEMA_TIMESTAMP, member, value));
                case 4 -> historyItemType((HistoryItemType) SchemaUtils.validateSameMember($SCHEMA_HISTORY_ITEM_TYPE, member, value));
                case 5 -> historySummary((String) SchemaUtils.validateSameMember($SCHEMA_HISTORY_SUMMARY, member, value));
                case 6 -> historyData((String) SchemaUtils.validateSameMember($SCHEMA_HISTORY_DATA, member, value));
                case 7 -> alarmContributorAttributes((Map<String, String>) SchemaUtils.validateSameMember($SCHEMA_ALARM_CONTRIBUTOR_ATTRIBUTES, member, value));
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
                    case 2 -> builder.alarmType(AlarmType.builder().deserializeMember(de, member).build());
                    case 3 -> builder.timestamp(de.readTimestamp(member));
                    case 4 -> builder.historyItemType(HistoryItemType.builder().deserializeMember(de, member).build());
                    case 5 -> builder.historySummary(de.readString(member));
                    case 6 -> builder.historyData(de.readString(member));
                    case 7 -> builder.alarmContributorAttributes(SharedSerde.deserializeContributorAttributes(member, de));
                    default -> throw new IllegalArgumentException("Unexpected member: " + member.memberName());
                }
            }
        }
    }
}
