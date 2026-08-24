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
public final class DescribeAlarmContributorsOutput implements SerializableStruct {

    public static final Schema $SCHEMA = Schemas.DESCRIBE_ALARM_CONTRIBUTORS_OUTPUT;
    private static final Schema $SCHEMA_ALARM_CONTRIBUTORS = $SCHEMA.member("AlarmContributors");
    private static final Schema $SCHEMA_NEXT_TOKEN = $SCHEMA.member("NextToken");

    public static final ShapeId $ID = $SCHEMA.id();

    private final transient List<AlarmContributor> alarmContributors;
    private final transient String nextToken;

    private DescribeAlarmContributorsOutput(Builder builder) {
        this.alarmContributors = builder.alarmContributors == null ? null : Collections.unmodifiableList(builder.alarmContributors);
        this.nextToken = builder.nextToken;
    }

    /**
     * A list of alarm contributors that provide details about the individual time series contributing to the alarm's
     * state.
     */
    public List<AlarmContributor> getAlarmContributors() {
        if (alarmContributors == null) {
            return Collections.emptyList();
        }
        return alarmContributors;
    }

    public boolean hasAlarmContributors() {
        return alarmContributors != null;
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
        DescribeAlarmContributorsOutput that = (DescribeAlarmContributorsOutput) other;
        return Objects.equals(this.nextToken, that.nextToken)
               && Objects.equals(this.alarmContributors, that.alarmContributors);
    }

    @Override
    public int hashCode() {
        int $hc = Objects.hashCode(alarmContributors);
        $hc = 31 * $hc + Objects.hashCode(nextToken);
        return $hc;
    }

    @Override
    public Schema schema() {
        return $SCHEMA;
    }

    @Override
    public void serializeMembers(ShapeSerializer serializer) {
        if (alarmContributors != null) {
            serializer.writeList($SCHEMA_ALARM_CONTRIBUTORS, alarmContributors, alarmContributors.size(), SharedSerde.AlarmContributorsSerializer.INSTANCE);
        }
        if (nextToken != null) {
            serializer.writeString($SCHEMA_NEXT_TOKEN, nextToken);
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T getMemberValue(Schema member) {
        return switch (member.memberIndex()) {
            case 0 -> (T) SchemaUtils.validateSameMember($SCHEMA_ALARM_CONTRIBUTORS, member, alarmContributors);
            case 1 -> (T) SchemaUtils.validateSameMember($SCHEMA_NEXT_TOKEN, member, nextToken);
            default -> throw new IllegalArgumentException("Attempted to get non-existent member: " + member.id());
        };
    }

    /**
     * Create a new builder containing all the current property values of this object.
     *
     * <p><strong>Note:</strong> This method performs only a shallow copy of the original properties.
     *
     * @return a builder for {@link DescribeAlarmContributorsOutput}.
     */
    public Builder toBuilder() {
        var builder = new Builder();
        builder.alarmContributors(this.alarmContributors);
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
     * Builder for {@link DescribeAlarmContributorsOutput}.
     */
    public static final class Builder implements ShapeBuilder<DescribeAlarmContributorsOutput> {
        private final PresenceTracker tracker = PresenceTracker.of($SCHEMA);
        private List<AlarmContributor> alarmContributors;
        private String nextToken;

        private Builder() {
            // Tell the tracker to assume clientOptional members are present.
            tracker.setMember($SCHEMA_ALARM_CONTRIBUTORS);
        }

        @Override
        public Schema schema() {
            return $SCHEMA;
        }

        /**
         * A list of alarm contributors that provide details about the individual time series contributing to the alarm's
         * state.
         *
         * <p><strong>Required</strong>
         * @return this builder.
         */
        public Builder alarmContributors(List<AlarmContributor> alarmContributors) {
            this.alarmContributors = Objects.requireNonNull(alarmContributors, "alarmContributors cannot be null");
            tracker.setMember($SCHEMA_ALARM_CONTRIBUTORS);
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
        public DescribeAlarmContributorsOutput build() {
            tracker.validate();
            return new DescribeAlarmContributorsOutput(this);
        }

        @Override
        @SuppressWarnings("unchecked")
        public void setMemberValue(Schema member, Object value) {
            switch (member.memberIndex()) {
                case 0 -> alarmContributors((List<AlarmContributor>) SchemaUtils.validateSameMember($SCHEMA_ALARM_CONTRIBUTORS, member, value));
                case 1 -> nextToken((String) SchemaUtils.validateSameMember($SCHEMA_NEXT_TOKEN, member, value));
                default -> ShapeBuilder.super.setMemberValue(member, value);
            }
        }

        @Override
        public ShapeBuilder<DescribeAlarmContributorsOutput> errorCorrection() {
            if (tracker.allSet()) {
                return this;
            }
            if (!tracker.checkMember($SCHEMA_ALARM_CONTRIBUTORS)) {
                alarmContributors(Collections.emptyList());
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
                    case 0 -> builder.alarmContributors(SharedSerde.deserializeAlarmContributors(member, de));
                    case 1 -> builder.nextToken(de.readString(member));
                    default -> throw new IllegalArgumentException("Unexpected member: " + member.memberName());
                }
            }
        }
    }
}
