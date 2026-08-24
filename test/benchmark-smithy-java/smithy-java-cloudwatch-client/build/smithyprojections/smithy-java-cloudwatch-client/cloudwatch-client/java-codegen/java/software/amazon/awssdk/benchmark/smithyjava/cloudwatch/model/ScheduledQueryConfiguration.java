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

/**
 * The configuration of the CloudWatch Logs scheduled query that backs a log alarm.
 */
@SmithyGenerated
public final class ScheduledQueryConfiguration implements SerializableStruct {

    public static final Schema $SCHEMA = Schemas.SCHEDULED_QUERY_CONFIGURATION;
    private static final Schema $SCHEMA_QUERY_STRING = $SCHEMA.member("QueryString");
    private static final Schema $SCHEMA_LOG_GROUP_IDENTIFIERS = $SCHEMA.member("LogGroupIdentifiers");
    private static final Schema $SCHEMA_QUERYAR_N = $SCHEMA.member("QueryARN");
    private static final Schema $SCHEMA_SCHEDULED_QUERY_ROLEAR_N = $SCHEMA.member("ScheduledQueryRoleARN");
    private static final Schema $SCHEMA_SCHEDULE_CONFIGURATION = $SCHEMA.member("ScheduleConfiguration");
    private static final Schema $SCHEMA_AGGREGATION_EXPRESSION = $SCHEMA.member("AggregationExpression");
    private static final Schema $SCHEMA_TAGS = $SCHEMA.member("Tags");

    public static final ShapeId $ID = $SCHEMA.id();

    private final transient String queryString;
    private final transient List<String> logGroupIdentifiers;
    private final transient String queryarN;
    private final transient String scheduledQueryRolearN;
    private final transient ScheduleConfiguration scheduleConfiguration;
    private final transient String aggregationExpression;
    private final transient List<Tag> tags;

    private ScheduledQueryConfiguration(Builder builder) {
        this.queryString = builder.queryString;
        this.logGroupIdentifiers = builder.logGroupIdentifiers == null ? null : Collections.unmodifiableList(builder.logGroupIdentifiers);
        this.queryarN = builder.queryarN;
        this.scheduledQueryRolearN = builder.scheduledQueryRolearN;
        this.scheduleConfiguration = builder.scheduleConfiguration;
        this.aggregationExpression = builder.aggregationExpression;
        this.tags = builder.tags == null ? null : Collections.unmodifiableList(builder.tags);
    }

    /**
     * The CloudWatch Logs query to execute on each scheduled run. Length constraints: maximum of 10,000 characters.
     */
    public String getQueryString() {
        return queryString;
    }

    /**
     * The log groups to query. Each entry can be a log group name or ARN. Use the ARN form when querying log groups in
     * a different account (for example, when running cross-account queries from a monitoring account). The list must
     * contain between 1 and 50 entries.
     */
    public List<String> getLogGroupIdentifiers() {
        if (logGroupIdentifiers == null) {
            return Collections.emptyList();
        }
        return logGroupIdentifiers;
    }

    public boolean hasLogGroupIdentifiers() {
        return logGroupIdentifiers != null;
    }

    /**
     * The Amazon Resource Name (ARN) of the CloudWatch Logs scheduled query that the alarm uses. This field is
     * populated in <code>DescribeAlarms</code> responses.
     */
    public String getQueryarN() {
        return queryarN;
    }

    /**
     * The Amazon Resource Name (ARN) of the IAM role that CloudWatch assumes when executing the scheduled query against
     * the configured log groups.
     */
    public String getScheduledQueryRolearN() {
        return scheduledQueryRolearN;
    }

    /**
     * The schedule and time-range offset configuration for the underlying scheduled query.
     */
    public ScheduleConfiguration getScheduleConfiguration() {
        return scheduleConfiguration;
    }

    /**
     * The expression that defines how to aggregate query results into one or more scalar values for alarm evaluation.
     * For example, <code>count(&#42;)</code> or <code>avg(latency) by host | sort desc</code>. Length constraints: minimum
     * 1 character, maximum 2048 characters.
     */
    public String getAggregationExpression() {
        return aggregationExpression;
    }

    /**
     * A list of key-value pairs to associate with the underlying scheduled query resource.
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
        ScheduledQueryConfiguration that = (ScheduledQueryConfiguration) other;
        return Objects.equals(this.queryString, that.queryString)
               && Objects.equals(this.queryarN, that.queryarN)
               && Objects.equals(this.scheduledQueryRolearN, that.scheduledQueryRolearN)
               && Objects.equals(this.aggregationExpression, that.aggregationExpression)
               && Objects.equals(this.scheduleConfiguration, that.scheduleConfiguration)
               && Objects.equals(this.logGroupIdentifiers, that.logGroupIdentifiers)
               && Objects.equals(this.tags, that.tags);
    }

    @Override
    public int hashCode() {
        int $hc = Objects.hashCode(queryString);
        $hc = 31 * $hc + Objects.hashCode(logGroupIdentifiers);
        $hc = 31 * $hc + Objects.hashCode(queryarN);
        $hc = 31 * $hc + Objects.hashCode(scheduledQueryRolearN);
        $hc = 31 * $hc + Objects.hashCode(scheduleConfiguration);
        $hc = 31 * $hc + Objects.hashCode(aggregationExpression);
        $hc = 31 * $hc + Objects.hashCode(tags);
        return $hc;
    }

    @Override
    public Schema schema() {
        return $SCHEMA;
    }

    @Override
    public void serializeMembers(ShapeSerializer serializer) {
        if (queryString != null) {
            serializer.writeString($SCHEMA_QUERY_STRING, queryString);
        }
        if (logGroupIdentifiers != null) {
            serializer.writeList($SCHEMA_LOG_GROUP_IDENTIFIERS, logGroupIdentifiers, logGroupIdentifiers.size(), SharedSerde.LogGroupIdentifiersSerializer.INSTANCE);
        }
        if (queryarN != null) {
            serializer.writeString($SCHEMA_QUERYAR_N, queryarN);
        }
        if (scheduledQueryRolearN != null) {
            serializer.writeString($SCHEMA_SCHEDULED_QUERY_ROLEAR_N, scheduledQueryRolearN);
        }
        if (scheduleConfiguration != null) {
            serializer.writeStruct($SCHEMA_SCHEDULE_CONFIGURATION, scheduleConfiguration);
        }
        if (aggregationExpression != null) {
            serializer.writeString($SCHEMA_AGGREGATION_EXPRESSION, aggregationExpression);
        }
        if (tags != null) {
            serializer.writeList($SCHEMA_TAGS, tags, tags.size(), SharedSerde.TagListSerializer.INSTANCE);
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T getMemberValue(Schema member) {
        return switch (member.memberIndex()) {
            case 0 -> (T) SchemaUtils.validateSameMember($SCHEMA_QUERY_STRING, member, queryString);
            case 1 -> (T) SchemaUtils.validateSameMember($SCHEMA_SCHEDULED_QUERY_ROLEAR_N, member, scheduledQueryRolearN);
            case 2 -> (T) SchemaUtils.validateSameMember($SCHEMA_SCHEDULE_CONFIGURATION, member, scheduleConfiguration);
            case 3 -> (T) SchemaUtils.validateSameMember($SCHEMA_AGGREGATION_EXPRESSION, member, aggregationExpression);
            case 4 -> (T) SchemaUtils.validateSameMember($SCHEMA_LOG_GROUP_IDENTIFIERS, member, logGroupIdentifiers);
            case 5 -> (T) SchemaUtils.validateSameMember($SCHEMA_QUERYAR_N, member, queryarN);
            case 6 -> (T) SchemaUtils.validateSameMember($SCHEMA_TAGS, member, tags);
            default -> throw new IllegalArgumentException("Attempted to get non-existent member: " + member.id());
        };
    }

    /**
     * Create a new builder containing all the current property values of this object.
     *
     * <p><strong>Note:</strong> This method performs only a shallow copy of the original properties.
     *
     * @return a builder for {@link ScheduledQueryConfiguration}.
     */
    public Builder toBuilder() {
        var builder = new Builder();
        builder.queryString(this.queryString);
        builder.logGroupIdentifiers(this.logGroupIdentifiers);
        builder.queryarN(this.queryarN);
        builder.scheduledQueryRolearN(this.scheduledQueryRolearN);
        builder.scheduleConfiguration(this.scheduleConfiguration);
        builder.aggregationExpression(this.aggregationExpression);
        builder.tags(this.tags);
        return builder;
    }

    /**
     * @return returns a new Builder.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for {@link ScheduledQueryConfiguration}.
     */
    public static final class Builder implements ShapeBuilder<ScheduledQueryConfiguration> {
        private final PresenceTracker tracker = PresenceTracker.of($SCHEMA);
        private String queryString;
        private List<String> logGroupIdentifiers;
        private String queryarN;
        private String scheduledQueryRolearN;
        private ScheduleConfiguration scheduleConfiguration;
        private String aggregationExpression;
        private List<Tag> tags;

        private Builder() {
            // Tell the tracker to assume clientOptional members are present.
            tracker.setMember($SCHEMA_QUERY_STRING);
            tracker.setMember($SCHEMA_SCHEDULED_QUERY_ROLEAR_N);
            tracker.setMember($SCHEMA_SCHEDULE_CONFIGURATION);
            tracker.setMember($SCHEMA_AGGREGATION_EXPRESSION);
        }

        @Override
        public Schema schema() {
            return $SCHEMA;
        }

        /**
         * The CloudWatch Logs query to execute on each scheduled run. Length constraints: maximum of 10,000 characters.
         *
         * <p><strong>Required</strong>
         * @return this builder.
         */
        public Builder queryString(String queryString) {
            this.queryString = Objects.requireNonNull(queryString, "queryString cannot be null");
            tracker.setMember($SCHEMA_QUERY_STRING);
            return this;
        }

        /**
         * The log groups to query. Each entry can be a log group name or ARN. Use the ARN form when querying log groups in
         * a different account (for example, when running cross-account queries from a monitoring account). The list must
         * contain between 1 and 50 entries.
         *
         * @return this builder.
         */
        public Builder logGroupIdentifiers(List<String> logGroupIdentifiers) {
            this.logGroupIdentifiers = logGroupIdentifiers;
            return this;
        }

        /**
         * The Amazon Resource Name (ARN) of the CloudWatch Logs scheduled query that the alarm uses. This field is
         * populated in <code>DescribeAlarms</code> responses.
         *
         * @return this builder.
         */
        public Builder queryarN(String queryarN) {
            this.queryarN = queryarN;
            return this;
        }

        /**
         * The Amazon Resource Name (ARN) of the IAM role that CloudWatch assumes when executing the scheduled query against
         * the configured log groups.
         *
         * <p><strong>Required</strong>
         * @return this builder.
         */
        public Builder scheduledQueryRolearN(String scheduledQueryRolearN) {
            this.scheduledQueryRolearN = Objects.requireNonNull(scheduledQueryRolearN, "scheduledQueryRolearN cannot be null");
            tracker.setMember($SCHEMA_SCHEDULED_QUERY_ROLEAR_N);
            return this;
        }

        /**
         * The schedule and time-range offset configuration for the underlying scheduled query.
         *
         * <p><strong>Required</strong>
         * @return this builder.
         */
        public Builder scheduleConfiguration(ScheduleConfiguration scheduleConfiguration) {
            this.scheduleConfiguration = Objects.requireNonNull(scheduleConfiguration, "scheduleConfiguration cannot be null");
            tracker.setMember($SCHEMA_SCHEDULE_CONFIGURATION);
            return this;
        }

        /**
         * The expression that defines how to aggregate query results into one or more scalar values for alarm evaluation.
         * For example, <code>count(&#42;)</code> or <code>avg(latency) by host | sort desc</code>. Length constraints: minimum
         * 1 character, maximum 2048 characters.
         *
         * <p><strong>Required</strong>
         * @return this builder.
         */
        public Builder aggregationExpression(String aggregationExpression) {
            this.aggregationExpression = Objects.requireNonNull(aggregationExpression, "aggregationExpression cannot be null");
            tracker.setMember($SCHEMA_AGGREGATION_EXPRESSION);
            return this;
        }

        /**
         * A list of key-value pairs to associate with the underlying scheduled query resource.
         *
         * @return this builder.
         */
        public Builder tags(List<Tag> tags) {
            this.tags = tags;
            return this;
        }

        @Override
        public ScheduledQueryConfiguration build() {
            tracker.validate();
            return new ScheduledQueryConfiguration(this);
        }

        @Override
        @SuppressWarnings("unchecked")
        public void setMemberValue(Schema member, Object value) {
            switch (member.memberIndex()) {
                case 0 -> queryString((String) SchemaUtils.validateSameMember($SCHEMA_QUERY_STRING, member, value));
                case 1 -> scheduledQueryRolearN((String) SchemaUtils.validateSameMember($SCHEMA_SCHEDULED_QUERY_ROLEAR_N, member, value));
                case 2 -> scheduleConfiguration((ScheduleConfiguration) SchemaUtils.validateSameMember($SCHEMA_SCHEDULE_CONFIGURATION, member, value));
                case 3 -> aggregationExpression((String) SchemaUtils.validateSameMember($SCHEMA_AGGREGATION_EXPRESSION, member, value));
                case 4 -> logGroupIdentifiers((List<String>) SchemaUtils.validateSameMember($SCHEMA_LOG_GROUP_IDENTIFIERS, member, value));
                case 5 -> queryarN((String) SchemaUtils.validateSameMember($SCHEMA_QUERYAR_N, member, value));
                case 6 -> tags((List<Tag>) SchemaUtils.validateSameMember($SCHEMA_TAGS, member, value));
                default -> ShapeBuilder.super.setMemberValue(member, value);
            }
        }

        @Override
        public ShapeBuilder<ScheduledQueryConfiguration> errorCorrection() {
            if (tracker.allSet()) {
                return this;
            }
            if (!tracker.checkMember($SCHEMA_QUERY_STRING)) {
                queryString("");
            }
            if (!tracker.checkMember($SCHEMA_SCHEDULED_QUERY_ROLEAR_N)) {
                scheduledQueryRolearN("");
            }
            if (!tracker.checkMember($SCHEMA_SCHEDULE_CONFIGURATION)) {
                tracker.setMember($SCHEMA_SCHEDULE_CONFIGURATION);
            }
            if (!tracker.checkMember($SCHEMA_AGGREGATION_EXPRESSION)) {
                aggregationExpression("");
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
                    case 0 -> builder.queryString(de.readString(member));
                    case 1 -> builder.scheduledQueryRolearN(de.readString(member));
                    case 2 -> builder.scheduleConfiguration(ScheduleConfiguration.builder().deserializeMember(de, member).build());
                    case 3 -> builder.aggregationExpression(de.readString(member));
                    case 4 -> builder.logGroupIdentifiers(SharedSerde.deserializeLogGroupIdentifiers(member, de));
                    case 5 -> builder.queryarN(de.readString(member));
                    case 6 -> builder.tags(SharedSerde.deserializeTagList(member, de));
                    default -> throw new IllegalArgumentException("Unexpected member: " + member.memberName());
                }
            }
        }
    }
}
