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
public final class DescribeAlarmHistoryOutput implements SerializableStruct {

    public static final Schema $SCHEMA = Schemas.DESCRIBE_ALARM_HISTORY_OUTPUT;
    private static final Schema $SCHEMA_ALARM_HISTORY_ITEMS = $SCHEMA.member("AlarmHistoryItems");
    private static final Schema $SCHEMA_NEXT_TOKEN = $SCHEMA.member("NextToken");

    public static final ShapeId $ID = $SCHEMA.id();

    private final transient List<AlarmHistoryItem> alarmHistoryItems;
    private final transient String nextToken;

    private DescribeAlarmHistoryOutput(Builder builder) {
        this.alarmHistoryItems = builder.alarmHistoryItems == null ? null : Collections.unmodifiableList(builder.alarmHistoryItems);
        this.nextToken = builder.nextToken;
    }

    /**
     * The alarm histories, in JSON format.
     */
    public List<AlarmHistoryItem> getAlarmHistoryItems() {
        if (alarmHistoryItems == null) {
            return Collections.emptyList();
        }
        return alarmHistoryItems;
    }

    public boolean hasAlarmHistoryItems() {
        return alarmHistoryItems != null;
    }

    /**
     * The token that marks the start of the next batch of returned results.
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
        DescribeAlarmHistoryOutput that = (DescribeAlarmHistoryOutput) other;
        return Objects.equals(this.nextToken, that.nextToken)
               && Objects.equals(this.alarmHistoryItems, that.alarmHistoryItems);
    }

    @Override
    public int hashCode() {
        int $hc = Objects.hashCode(alarmHistoryItems);
        $hc = 31 * $hc + Objects.hashCode(nextToken);
        return $hc;
    }

    @Override
    public Schema schema() {
        return $SCHEMA;
    }

    @Override
    public void serializeMembers(ShapeSerializer serializer) {
        if (alarmHistoryItems != null) {
            serializer.writeList($SCHEMA_ALARM_HISTORY_ITEMS, alarmHistoryItems, alarmHistoryItems.size(), SharedSerde.AlarmHistoryItemsSerializer.INSTANCE);
        }
        if (nextToken != null) {
            serializer.writeString($SCHEMA_NEXT_TOKEN, nextToken);
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T getMemberValue(Schema member) {
        return switch (member.memberIndex()) {
            case 0 -> (T) SchemaUtils.validateSameMember($SCHEMA_ALARM_HISTORY_ITEMS, member, alarmHistoryItems);
            case 1 -> (T) SchemaUtils.validateSameMember($SCHEMA_NEXT_TOKEN, member, nextToken);
            default -> throw new IllegalArgumentException("Attempted to get non-existent member: " + member.id());
        };
    }

    /**
     * Create a new builder containing all the current property values of this object.
     *
     * <p><strong>Note:</strong> This method performs only a shallow copy of the original properties.
     *
     * @return a builder for {@link DescribeAlarmHistoryOutput}.
     */
    public Builder toBuilder() {
        var builder = new Builder();
        builder.alarmHistoryItems(this.alarmHistoryItems);
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
     * Builder for {@link DescribeAlarmHistoryOutput}.
     */
    public static final class Builder implements ShapeBuilder<DescribeAlarmHistoryOutput> {
        private List<AlarmHistoryItem> alarmHistoryItems;
        private String nextToken;

        private Builder() {}

        @Override
        public Schema schema() {
            return $SCHEMA;
        }

        /**
         * The alarm histories, in JSON format.
         *
         * @return this builder.
         */
        public Builder alarmHistoryItems(List<AlarmHistoryItem> alarmHistoryItems) {
            this.alarmHistoryItems = alarmHistoryItems;
            return this;
        }

        /**
         * The token that marks the start of the next batch of returned results.
         *
         * @return this builder.
         */
        public Builder nextToken(String nextToken) {
            this.nextToken = nextToken;
            return this;
        }

        @Override
        public DescribeAlarmHistoryOutput build() {
            return new DescribeAlarmHistoryOutput(this);
        }

        @Override
        @SuppressWarnings("unchecked")
        public void setMemberValue(Schema member, Object value) {
            switch (member.memberIndex()) {
                case 0 -> alarmHistoryItems((List<AlarmHistoryItem>) SchemaUtils.validateSameMember($SCHEMA_ALARM_HISTORY_ITEMS, member, value));
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
                    case 0 -> builder.alarmHistoryItems(SharedSerde.deserializeAlarmHistoryItems(member, de));
                    case 1 -> builder.nextToken(de.readString(member));
                    default -> throw new IllegalArgumentException("Unexpected member: " + member.memberName());
                }
            }
        }
    }
}
