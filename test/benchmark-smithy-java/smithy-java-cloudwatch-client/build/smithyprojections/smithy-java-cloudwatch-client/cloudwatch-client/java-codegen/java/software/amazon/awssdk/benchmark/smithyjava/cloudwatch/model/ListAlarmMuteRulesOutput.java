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
public final class ListAlarmMuteRulesOutput implements SerializableStruct {

    public static final Schema $SCHEMA = Schemas.LIST_ALARM_MUTE_RULES_OUTPUT;
    private static final Schema $SCHEMA_ALARM_MUTE_RULE_SUMMARIES = $SCHEMA.member("AlarmMuteRuleSummaries");
    private static final Schema $SCHEMA_NEXT_TOKEN = $SCHEMA.member("NextToken");

    public static final ShapeId $ID = $SCHEMA.id();

    private final transient List<AlarmMuteRuleSummary> alarmMuteRuleSummaries;
    private final transient String nextToken;

    private ListAlarmMuteRulesOutput(Builder builder) {
        this.alarmMuteRuleSummaries = builder.alarmMuteRuleSummaries == null ? null : Collections.unmodifiableList(builder.alarmMuteRuleSummaries);
        this.nextToken = builder.nextToken;
    }

    /**
     * A list of alarm mute rule summaries.
     */
    public List<AlarmMuteRuleSummary> getAlarmMuteRuleSummaries() {
        if (alarmMuteRuleSummaries == null) {
            return Collections.emptyList();
        }
        return alarmMuteRuleSummaries;
    }

    public boolean hasAlarmMuteRuleSummaries() {
        return alarmMuteRuleSummaries != null;
    }

    /**
     * The token to use when requesting the next set of results. If this field is absent, there are no more results to
     * retrieve.
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
        ListAlarmMuteRulesOutput that = (ListAlarmMuteRulesOutput) other;
        return Objects.equals(this.nextToken, that.nextToken)
               && Objects.equals(this.alarmMuteRuleSummaries, that.alarmMuteRuleSummaries);
    }

    @Override
    public int hashCode() {
        int $hc = Objects.hashCode(alarmMuteRuleSummaries);
        $hc = 31 * $hc + Objects.hashCode(nextToken);
        return $hc;
    }

    @Override
    public Schema schema() {
        return $SCHEMA;
    }

    @Override
    public void serializeMembers(ShapeSerializer serializer) {
        if (alarmMuteRuleSummaries != null) {
            serializer.writeList($SCHEMA_ALARM_MUTE_RULE_SUMMARIES, alarmMuteRuleSummaries, alarmMuteRuleSummaries.size(), SharedSerde.AlarmMuteRuleSummariesSerializer.INSTANCE);
        }
        if (nextToken != null) {
            serializer.writeString($SCHEMA_NEXT_TOKEN, nextToken);
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T getMemberValue(Schema member) {
        return switch (member.memberIndex()) {
            case 0 -> (T) SchemaUtils.validateSameMember($SCHEMA_ALARM_MUTE_RULE_SUMMARIES, member, alarmMuteRuleSummaries);
            case 1 -> (T) SchemaUtils.validateSameMember($SCHEMA_NEXT_TOKEN, member, nextToken);
            default -> throw new IllegalArgumentException("Attempted to get non-existent member: " + member.id());
        };
    }

    /**
     * Create a new builder containing all the current property values of this object.
     *
     * <p><strong>Note:</strong> This method performs only a shallow copy of the original properties.
     *
     * @return a builder for {@link ListAlarmMuteRulesOutput}.
     */
    public Builder toBuilder() {
        var builder = new Builder();
        builder.alarmMuteRuleSummaries(this.alarmMuteRuleSummaries);
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
     * Builder for {@link ListAlarmMuteRulesOutput}.
     */
    public static final class Builder implements ShapeBuilder<ListAlarmMuteRulesOutput> {
        private List<AlarmMuteRuleSummary> alarmMuteRuleSummaries;
        private String nextToken;

        private Builder() {}

        @Override
        public Schema schema() {
            return $SCHEMA;
        }

        /**
         * A list of alarm mute rule summaries.
         *
         * @return this builder.
         */
        public Builder alarmMuteRuleSummaries(List<AlarmMuteRuleSummary> alarmMuteRuleSummaries) {
            this.alarmMuteRuleSummaries = alarmMuteRuleSummaries;
            return this;
        }

        /**
         * The token to use when requesting the next set of results. If this field is absent, there are no more results to
         * retrieve.
         *
         * @return this builder.
         */
        public Builder nextToken(String nextToken) {
            this.nextToken = nextToken;
            return this;
        }

        @Override
        public ListAlarmMuteRulesOutput build() {
            return new ListAlarmMuteRulesOutput(this);
        }

        @Override
        @SuppressWarnings("unchecked")
        public void setMemberValue(Schema member, Object value) {
            switch (member.memberIndex()) {
                case 0 -> alarmMuteRuleSummaries((List<AlarmMuteRuleSummary>) SchemaUtils.validateSameMember($SCHEMA_ALARM_MUTE_RULE_SUMMARIES, member, value));
                case 1 -> nextToken((String) SchemaUtils.validateSameMember($SCHEMA_NEXT_TOKEN, member, value));
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
                    case 0 -> builder.alarmMuteRuleSummaries(SharedSerde.deserializeAlarmMuteRuleSummaries(member, de));
                    case 1 -> builder.nextToken(de.readString(member));
                    default -> throw new IllegalArgumentException("Unexpected member: " + member.memberName());
                }
            }
        }
    }
}
