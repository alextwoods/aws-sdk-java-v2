package software.amazon.awssdk.benchmark.smithyjava.cloudwatch.model;

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
 * This structure is used in both <code>GetMetricData</code> and <code>PutMetricAlarm</code>. The supported use of this
 * structure is different for those two operations.
 *
 * <p>When used in <code>GetMetricData</code>, it indicates the metric data to return, and whether this call is just
 * retrieving a batch set of data for one metric, or is performing a Metrics Insights query or a math expression. A
 * single <code>GetMetricData</code> call can include up to 500 <code>MetricDataQuery</code> structures.
 *
 * <p>When used in <code>PutMetricAlarm</code>, it enables you to create an alarm based on a metric math expression.
 * Each <code>MetricDataQuery</code> in the array specifies either a metric to retrieve, or a math expression to be
 * performed on retrieved metrics. A single <code>PutMetricAlarm</code> call can include up to 20 <code>MetricDataQuery</code>
 * structures in the array. The 20 structures can include as many as 10 structures that contain a <code>MetricStat</code>
 * parameter to retrieve a metric, and as many as 10 structures that contain the <code>Expression</code> parameter to
 * perform a math expression. Of those <code>Expression</code> structures, one must have <code>true</code> as the value
 * for <code>ReturnData</code>. The result of this expression is the value the alarm watches.
 *
 * <p>Any expression used in a <code>PutMetricAlarm</code> operation must return a single time series. For more
 * information, see <a href="https://docs.aws.amazon.com/AmazonCloudWatch/latest/monitoring/using-metric-math.html#metric-math-syntax">Metric Math Syntax and Functions</a> in the <i>Amazon CloudWatch User Guide</i>.
 *
 * <p>Some of the parameters of this structure also have different uses whether you are using this structure in a <code>
 * GetMetricData</code> operation or a <code>PutMetricAlarm</code> operation. These differences are explained in the
 * following parameter list.
 */
@SmithyGenerated
public final class MetricDataQuery implements SerializableStruct {

    public static final Schema $SCHEMA = Schemas.METRIC_DATA_QUERY;
    private static final Schema $SCHEMA_ID = $SCHEMA.member("Id");
    private static final Schema $SCHEMA_METRIC_STAT = $SCHEMA.member("MetricStat");
    private static final Schema $SCHEMA_EXPRESSION = $SCHEMA.member("Expression");
    private static final Schema $SCHEMA_LABEL = $SCHEMA.member("Label");
    private static final Schema $SCHEMA_RETURN_DATA = $SCHEMA.member("ReturnData");
    private static final Schema $SCHEMA_PERIOD = $SCHEMA.member("Period");
    private static final Schema $SCHEMA_ACCOUNT_ID = $SCHEMA.member("AccountId");

    public static final ShapeId $ID = $SCHEMA.id();

    private final transient String id;
    private final transient MetricStat metricStat;
    private final transient String expression;
    private final transient String label;
    private final transient Boolean returnData;
    private final transient Integer period;
    private final transient String accountId;

    private MetricDataQuery(Builder builder) {
        this.id = builder.id;
        this.metricStat = builder.metricStat;
        this.expression = builder.expression;
        this.label = builder.label;
        this.returnData = builder.returnData;
        this.period = builder.period;
        this.accountId = builder.accountId;
    }

    /**
     * A short name used to tie this object to the results in the response. This name must be unique within a single
     * call to <code>GetMetricData</code>. If you are performing math expressions on this set of data, this name
     * represents that data and can serve as a variable in the mathematical expression. The valid characters are
     * letters, numbers, and underscore. The first character must be a lowercase letter.
     */
    public String getId() {
        return id;
    }

    /**
     * The metric to be returned, along with statistics, period, and units. Use this parameter only if this object is
     * retrieving a metric and not performing a math expression on returned data.
     *
     * <p>Within one MetricDataQuery object, you must specify either <code>Expression</code> or <code>MetricStat</code>
     * but not both.
     */
    public MetricStat getMetricStat() {
        return metricStat;
    }

    /**
     * This field can contain either a Metrics Insights query, or a metric math expression to be performed on the
     * returned data. For more information about Metrics Insights queries, see <a href="https://docs.aws.amazon.com/AmazonCloudWatch/latest/monitoring/cloudwatch-metrics-insights-querylanguage">Metrics Insights query components and
     * syntax</a> in the <i>Amazon CloudWatch User Guide</i>.
     *
     * <p>A math expression can use the <code>Id</code> of the other metrics or queries to refer to those metrics, and
     * can also use the <code>Id</code> of other expressions to use the result of those expressions. For more
     * information about metric math expressions, see <a href="https://docs.aws.amazon.com/AmazonCloudWatch/latest/monitoring/using-metric-math.html#metric-math-syntax">Metric Math Syntax and Functions</a> in the <i>Amazon
     * CloudWatch User Guide</i>.
     *
     * <p>Within each MetricDataQuery object, you must specify either <code>Expression</code> or <code>MetricStat</code>
     * but not both.
     */
    public String getExpression() {
        return expression;
    }

    /**
     * A human-readable label for this metric or expression. This is especially useful if this is an expression, so that
     * you know what the value represents. If the metric or expression is shown in a CloudWatch dashboard widget, the
     * label is shown. If Label is omitted, CloudWatch generates a default.
     *
     * <p>You can put dynamic expressions into a label, so that it is more descriptive. For more information, see <a href="https://docs.aws.amazon.com/AmazonCloudWatch/latest/monitoring/graph-dynamic-labels.html">
     * Using Dynamic Labels</a>.
     */
    public String getLabel() {
        return label;
    }

    /**
     * When used in <code>GetMetricData</code>, this option indicates whether to return the timestamps and raw data
     * values of this metric. If you are performing this call just to do math expressions and do not also need the raw
     * data returned, you can specify <code>false</code>. If you omit this, the default of <code>true</code> is used.
     *
     * <p>When used in <code>PutMetricAlarm</code>, specify <code>true</code> for the one expression result to use as
     * the alarm. For all other metrics and expressions in the same <code>PutMetricAlarm</code> operation, specify <code>
     * ReturnData</code> as False.
     */
    public Boolean isReturnData() {
        return returnData;
    }

    /**
     * The granularity, in seconds, of the returned data points. For metrics with regular resolution, a period can be as
     * short as one minute (60 seconds) and must be a multiple of 60. For high-resolution metrics that are collected at
     * intervals of less than one minute, the period can be 1, 5, 10, 20, 30, 60, or any multiple of 60. High-resolution
     * metrics are those metrics stored by a <code>PutMetricData</code> operation that includes a
     * <code>StorageResolution of 1 second</code>.
     */
    public Integer getPeriod() {
        return period;
    }

    /**
     * The ID of the account where the metrics are located.
     *
     * <p>If you are performing a <code>GetMetricData</code> operation in a monitoring account, use this to specify
     * which account to retrieve this metric from.
     *
     * <p>If you are performing a <code>PutMetricAlarm</code> operation, use this to specify which account contains the
     * metric that the alarm is watching.
     */
    public String getAccountId() {
        return accountId;
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
        MetricDataQuery that = (MetricDataQuery) other;
        return Objects.equals(this.returnData, that.returnData)
               && Objects.equals(this.period, that.period)
               && Objects.equals(this.id, that.id)
               && Objects.equals(this.expression, that.expression)
               && Objects.equals(this.label, that.label)
               && Objects.equals(this.accountId, that.accountId)
               && Objects.equals(this.metricStat, that.metricStat);
    }

    @Override
    public int hashCode() {
        int $hc = Objects.hashCode(id);
        $hc = 31 * $hc + Objects.hashCode(metricStat);
        $hc = 31 * $hc + Objects.hashCode(expression);
        $hc = 31 * $hc + Objects.hashCode(label);
        $hc = 31 * $hc + Objects.hashCode(returnData);
        $hc = 31 * $hc + Objects.hashCode(period);
        $hc = 31 * $hc + Objects.hashCode(accountId);
        return $hc;
    }

    @Override
    public Schema schema() {
        return $SCHEMA;
    }

    @Override
    public void serializeMembers(ShapeSerializer serializer) {
        if (id != null) {
            serializer.writeString($SCHEMA_ID, id);
        }
        if (metricStat != null) {
            serializer.writeStruct($SCHEMA_METRIC_STAT, metricStat);
        }
        if (expression != null) {
            serializer.writeString($SCHEMA_EXPRESSION, expression);
        }
        if (label != null) {
            serializer.writeString($SCHEMA_LABEL, label);
        }
        if (returnData != null) {
            serializer.writeBoolean($SCHEMA_RETURN_DATA, returnData);
        }
        if (period != null) {
            serializer.writeInteger($SCHEMA_PERIOD, period);
        }
        if (accountId != null) {
            serializer.writeString($SCHEMA_ACCOUNT_ID, accountId);
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T getMemberValue(Schema member) {
        return switch (member.memberIndex()) {
            case 0 -> (T) SchemaUtils.validateSameMember($SCHEMA_ID, member, id);
            case 1 -> (T) SchemaUtils.validateSameMember($SCHEMA_METRIC_STAT, member, metricStat);
            case 2 -> (T) SchemaUtils.validateSameMember($SCHEMA_EXPRESSION, member, expression);
            case 3 -> (T) SchemaUtils.validateSameMember($SCHEMA_LABEL, member, label);
            case 4 -> (T) SchemaUtils.validateSameMember($SCHEMA_RETURN_DATA, member, returnData);
            case 5 -> (T) SchemaUtils.validateSameMember($SCHEMA_PERIOD, member, period);
            case 6 -> (T) SchemaUtils.validateSameMember($SCHEMA_ACCOUNT_ID, member, accountId);
            default -> throw new IllegalArgumentException("Attempted to get non-existent member: " + member.id());
        };
    }

    /**
     * Create a new builder containing all the current property values of this object.
     *
     * <p><strong>Note:</strong> This method performs only a shallow copy of the original properties.
     *
     * @return a builder for {@link MetricDataQuery}.
     */
    public Builder toBuilder() {
        var builder = new Builder();
        builder.id(this.id);
        builder.metricStat(this.metricStat);
        builder.expression(this.expression);
        builder.label(this.label);
        builder.returnData(this.returnData);
        builder.period(this.period);
        builder.accountId(this.accountId);
        return builder;
    }

    /**
     * @return returns a new Builder.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for {@link MetricDataQuery}.
     */
    public static final class Builder implements ShapeBuilder<MetricDataQuery> {
        private final PresenceTracker tracker = PresenceTracker.of($SCHEMA);
        private String id;
        private MetricStat metricStat;
        private String expression;
        private String label;
        private Boolean returnData;
        private Integer period;
        private String accountId;

        private Builder() {
            // Tell the tracker to assume clientOptional members are present.
            tracker.setMember($SCHEMA_ID);
        }

        @Override
        public Schema schema() {
            return $SCHEMA;
        }

        /**
         * A short name used to tie this object to the results in the response. This name must be unique within a single
         * call to <code>GetMetricData</code>. If you are performing math expressions on this set of data, this name
         * represents that data and can serve as a variable in the mathematical expression. The valid characters are
         * letters, numbers, and underscore. The first character must be a lowercase letter.
         *
         * <p><strong>Required</strong>
         * @return this builder.
         */
        public Builder id(String id) {
            this.id = Objects.requireNonNull(id, "id cannot be null");
            tracker.setMember($SCHEMA_ID);
            return this;
        }

        /**
         * The metric to be returned, along with statistics, period, and units. Use this parameter only if this object is
         * retrieving a metric and not performing a math expression on returned data.
         *
         * <p>Within one MetricDataQuery object, you must specify either <code>Expression</code> or <code>MetricStat</code>
         * but not both.
         *
         * @return this builder.
         */
        public Builder metricStat(MetricStat metricStat) {
            this.metricStat = metricStat;
            return this;
        }

        /**
         * This field can contain either a Metrics Insights query, or a metric math expression to be performed on the
         * returned data. For more information about Metrics Insights queries, see <a href="https://docs.aws.amazon.com/AmazonCloudWatch/latest/monitoring/cloudwatch-metrics-insights-querylanguage">Metrics Insights query components and
         * syntax</a> in the <i>Amazon CloudWatch User Guide</i>.
         *
         * <p>A math expression can use the <code>Id</code> of the other metrics or queries to refer to those metrics, and
         * can also use the <code>Id</code> of other expressions to use the result of those expressions. For more
         * information about metric math expressions, see <a href="https://docs.aws.amazon.com/AmazonCloudWatch/latest/monitoring/using-metric-math.html#metric-math-syntax">Metric Math Syntax and Functions</a> in the <i>Amazon
         * CloudWatch User Guide</i>.
         *
         * <p>Within each MetricDataQuery object, you must specify either <code>Expression</code> or <code>MetricStat</code>
         * but not both.
         *
         * @return this builder.
         */
        public Builder expression(String expression) {
            this.expression = expression;
            return this;
        }

        /**
         * A human-readable label for this metric or expression. This is especially useful if this is an expression, so that
         * you know what the value represents. If the metric or expression is shown in a CloudWatch dashboard widget, the
         * label is shown. If Label is omitted, CloudWatch generates a default.
         *
         * <p>You can put dynamic expressions into a label, so that it is more descriptive. For more information, see <a href="https://docs.aws.amazon.com/AmazonCloudWatch/latest/monitoring/graph-dynamic-labels.html">
         * Using Dynamic Labels</a>.
         *
         * @return this builder.
         */
        public Builder label(String label) {
            this.label = label;
            return this;
        }

        /**
         * When used in <code>GetMetricData</code>, this option indicates whether to return the timestamps and raw data
         * values of this metric. If you are performing this call just to do math expressions and do not also need the raw
         * data returned, you can specify <code>false</code>. If you omit this, the default of <code>true</code> is used.
         *
         * <p>When used in <code>PutMetricAlarm</code>, specify <code>true</code> for the one expression result to use as
         * the alarm. For all other metrics and expressions in the same <code>PutMetricAlarm</code> operation, specify <code>
         * ReturnData</code> as False.
         *
         * @return this builder.
         */
        public Builder returnData(Boolean returnData) {
            this.returnData = returnData;
            return this;
        }

        /**
         * The granularity, in seconds, of the returned data points. For metrics with regular resolution, a period can be as
         * short as one minute (60 seconds) and must be a multiple of 60. For high-resolution metrics that are collected at
         * intervals of less than one minute, the period can be 1, 5, 10, 20, 30, 60, or any multiple of 60. High-resolution
         * metrics are those metrics stored by a <code>PutMetricData</code> operation that includes a
         * <code>StorageResolution of 1 second</code>.
         *
         * @return this builder.
         */
        public Builder period(Integer period) {
            this.period = period;
            return this;
        }

        /**
         * The ID of the account where the metrics are located.
         *
         * <p>If you are performing a <code>GetMetricData</code> operation in a monitoring account, use this to specify
         * which account to retrieve this metric from.
         *
         * <p>If you are performing a <code>PutMetricAlarm</code> operation, use this to specify which account contains the
         * metric that the alarm is watching.
         *
         * @return this builder.
         */
        public Builder accountId(String accountId) {
            this.accountId = accountId;
            return this;
        }

        @Override
        public MetricDataQuery build() {
            tracker.validate();
            return new MetricDataQuery(this);
        }

        @Override
        @SuppressWarnings("unchecked")
        public void setMemberValue(Schema member, Object value) {
            switch (member.memberIndex()) {
                case 0 -> id((String) SchemaUtils.validateSameMember($SCHEMA_ID, member, value));
                case 1 -> metricStat((MetricStat) SchemaUtils.validateSameMember($SCHEMA_METRIC_STAT, member, value));
                case 2 -> expression((String) SchemaUtils.validateSameMember($SCHEMA_EXPRESSION, member, value));
                case 3 -> label((String) SchemaUtils.validateSameMember($SCHEMA_LABEL, member, value));
                case 4 -> returnData((Boolean) SchemaUtils.validateSameMember($SCHEMA_RETURN_DATA, member, value));
                case 5 -> period((Integer) SchemaUtils.validateSameMember($SCHEMA_PERIOD, member, value));
                case 6 -> accountId((String) SchemaUtils.validateSameMember($SCHEMA_ACCOUNT_ID, member, value));
                default -> ShapeBuilder.super.setMemberValue(member, value);
            }
        }

        @Override
        public ShapeBuilder<MetricDataQuery> errorCorrection() {
            if (tracker.allSet()) {
                return this;
            }
            if (!tracker.checkMember($SCHEMA_ID)) {
                id("");
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
                    case 0 -> builder.id(de.readString(member));
                    case 1 -> builder.metricStat(MetricStat.builder().deserializeMember(de, member).build());
                    case 2 -> builder.expression(de.readString(member));
                    case 3 -> builder.label(de.readString(member));
                    case 4 -> builder.returnData(de.readBoolean(member));
                    case 5 -> builder.period(de.readInteger(member));
                    case 6 -> builder.accountId(de.readString(member));
                    default -> throw new IllegalArgumentException("Unexpected member: " + member.memberName());
                }
            }
        }
    }
}
