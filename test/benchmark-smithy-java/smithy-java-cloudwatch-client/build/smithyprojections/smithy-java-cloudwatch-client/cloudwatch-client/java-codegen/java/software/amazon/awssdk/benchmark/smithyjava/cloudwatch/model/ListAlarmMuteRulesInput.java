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
public final class ListAlarmMuteRulesInput implements SerializableStruct {

    public static final Schema $SCHEMA = Schemas.LIST_ALARM_MUTE_RULES_INPUT;
    private static final Schema $SCHEMA_ALARM_NAME = $SCHEMA.member("AlarmName");
    private static final Schema $SCHEMA_STATUSES = $SCHEMA.member("Statuses");
    private static final Schema $SCHEMA_MAX_RECORDS = $SCHEMA.member("MaxRecords");
    private static final Schema $SCHEMA_NEXT_TOKEN = $SCHEMA.member("NextToken");

    public static final ShapeId $ID = $SCHEMA.id();

    private final transient String alarmName;
    private final transient List<AlarmMuteRuleStatus> statuses;
    private final transient Integer maxRecords;
    private final transient String nextToken;

    private ListAlarmMuteRulesInput(Builder builder) {
        this.alarmName = builder.alarmName;
        this.statuses = builder.statuses == null ? null : Collections.unmodifiableList(builder.statuses);
        this.maxRecords = builder.maxRecords;
        this.nextToken = builder.nextToken;
    }

    /**
     * Filter results to show only mute rules that target the specified alarm name.
     */
    public String getAlarmName() {
        return alarmName;
    }

    /**
     * Filter results to show only mute rules with the specified statuses. Valid values are <code>SCHEDULED</code>,
     * <code>ACTIVE</code>, or <code>EXPIRED</code>.
     */
    public List<AlarmMuteRuleStatus> getStatuses() {
        if (statuses == null) {
            return Collections.emptyList();
        }
        return statuses;
    }

    public boolean hasStatuses() {
        return statuses != null;
    }

    /**
     * The maximum number of mute rules to return in one call. The default is 50.
     */
    public Integer getMaxRecords() {
        return maxRecords;
    }

    /**
     * The token returned from a previous call to indicate where to continue retrieving results.
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
        ListAlarmMuteRulesInput that = (ListAlarmMuteRulesInput) other;
        return Objects.equals(this.maxRecords, that.maxRecords)
               && Objects.equals(this.alarmName, that.alarmName)
               && Objects.equals(this.nextToken, that.nextToken)
               && Objects.equals(this.statuses, that.statuses);
    }

    @Override
    public int hashCode() {
        int $hc = Objects.hashCode(alarmName);
        $hc = 31 * $hc + Objects.hashCode(statuses);
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
        if (alarmName != null) {
            serializer.writeString($SCHEMA_ALARM_NAME, alarmName);
        }
        if (statuses != null) {
            serializer.writeList($SCHEMA_STATUSES, statuses, statuses.size(), SharedSerde.AlarmMuteRuleStatusesSerializer.INSTANCE);
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
            case 0 -> (T) SchemaUtils.validateSameMember($SCHEMA_ALARM_NAME, member, alarmName);
            case 1 -> (T) SchemaUtils.validateSameMember($SCHEMA_STATUSES, member, statuses);
            case 2 -> (T) SchemaUtils.validateSameMember($SCHEMA_MAX_RECORDS, member, maxRecords);
            case 3 -> (T) SchemaUtils.validateSameMember($SCHEMA_NEXT_TOKEN, member, nextToken);
            default -> throw new IllegalArgumentException("Attempted to get non-existent member: " + member.id());
        };
    }

    /**
     * Create a new builder containing all the current property values of this object.
     *
     * <p><strong>Note:</strong> This method performs only a shallow copy of the original properties.
     *
     * @return a builder for {@link ListAlarmMuteRulesInput}.
     */
    public Builder toBuilder() {
        var builder = new Builder();
        builder.alarmName(this.alarmName);
        builder.statuses(this.statuses);
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
     * Builder for {@link ListAlarmMuteRulesInput}.
     */
    public static final class Builder implements ShapeBuilder<ListAlarmMuteRulesInput> {
        private String alarmName;
        private List<AlarmMuteRuleStatus> statuses;
        private Integer maxRecords;
        private String nextToken;

        private Builder() {}

        @Override
        public Schema schema() {
            return $SCHEMA;
        }

        /**
         * Filter results to show only mute rules that target the specified alarm name.
         *
         * @return this builder.
         */
        public Builder alarmName(String alarmName) {
            this.alarmName = alarmName;
            return this;
        }

        /**
         * Filter results to show only mute rules with the specified statuses. Valid values are <code>SCHEDULED</code>,
         * <code>ACTIVE</code>, or <code>EXPIRED</code>.
         *
         * @return this builder.
         */
        public Builder statuses(List<AlarmMuteRuleStatus> statuses) {
            this.statuses = statuses;
            return this;
        }

        /**
         * The maximum number of mute rules to return in one call. The default is 50.
         *
         * @return this builder.
         */
        public Builder maxRecords(Integer maxRecords) {
            this.maxRecords = maxRecords;
            return this;
        }

        /**
         * The token returned from a previous call to indicate where to continue retrieving results.
         *
         * @return this builder.
         */
        public Builder nextToken(String nextToken) {
            this.nextToken = nextToken;
            return this;
        }

        @Override
        public ListAlarmMuteRulesInput build() {
            return new ListAlarmMuteRulesInput(this);
        }

        @Override
        @SuppressWarnings("unchecked")
        public void setMemberValue(Schema member, Object value) {
            switch (member.memberIndex()) {
                case 0 -> alarmName((String) SchemaUtils.validateSameMember($SCHEMA_ALARM_NAME, member, value));
                case 1 -> statuses((List<AlarmMuteRuleStatus>) SchemaUtils.validateSameMember($SCHEMA_STATUSES, member, value));
                case 2 -> maxRecords((Integer) SchemaUtils.validateSameMember($SCHEMA_MAX_RECORDS, member, value));
                case 3 -> nextToken((String) SchemaUtils.validateSameMember($SCHEMA_NEXT_TOKEN, member, value));
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
                    case 1 -> builder.statuses(SharedSerde.deserializeAlarmMuteRuleStatuses(member, de));
                    case 2 -> builder.maxRecords(de.readInteger(member));
                    case 3 -> builder.nextToken(de.readString(member));
                    default -> throw new IllegalArgumentException("Unexpected member: " + member.memberName());
                }
            }
        }
    }
}
