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
public final class GetMetricStreamOutput implements SerializableStruct {

    public static final Schema $SCHEMA = Schemas.GET_METRIC_STREAM_OUTPUT;
    private static final Schema $SCHEMA_ARN = $SCHEMA.member("Arn");
    private static final Schema $SCHEMA_NAME = $SCHEMA.member("Name");
    private static final Schema $SCHEMA_INCLUDE_FILTERS = $SCHEMA.member("IncludeFilters");
    private static final Schema $SCHEMA_EXCLUDE_FILTERS = $SCHEMA.member("ExcludeFilters");
    private static final Schema $SCHEMA_FIREHOSE_ARN = $SCHEMA.member("FirehoseArn");
    private static final Schema $SCHEMA_ROLE_ARN = $SCHEMA.member("RoleArn");
    private static final Schema $SCHEMA_STATE = $SCHEMA.member("State");
    private static final Schema $SCHEMA_CREATION_DATE = $SCHEMA.member("CreationDate");
    private static final Schema $SCHEMA_LAST_UPDATE_DATE = $SCHEMA.member("LastUpdateDate");
    private static final Schema $SCHEMA_OUTPUT_FORMAT = $SCHEMA.member("OutputFormat");
    private static final Schema $SCHEMA_STATISTICS_CONFIGURATIONS = $SCHEMA.member("StatisticsConfigurations");
    private static final Schema $SCHEMA_INCLUDE_LINKED_ACCOUNTS_METRICS = $SCHEMA.member("IncludeLinkedAccountsMetrics");

    public static final ShapeId $ID = $SCHEMA.id();

    private final transient String arn;
    private final transient String name;
    private final transient List<MetricStreamFilter> includeFilters;
    private final transient List<MetricStreamFilter> excludeFilters;
    private final transient String firehoseArn;
    private final transient String roleArn;
    private final transient String state;
    private final transient Instant creationDate;
    private final transient Instant lastUpdateDate;
    private final transient MetricStreamOutputFormat outputFormat;
    private final transient List<MetricStreamStatisticsConfiguration> statisticsConfigurations;
    private final transient Boolean includeLinkedAccountsMetrics;

    private GetMetricStreamOutput(Builder builder) {
        this.arn = builder.arn;
        this.name = builder.name;
        this.includeFilters = builder.includeFilters == null ? null : Collections.unmodifiableList(builder.includeFilters);
        this.excludeFilters = builder.excludeFilters == null ? null : Collections.unmodifiableList(builder.excludeFilters);
        this.firehoseArn = builder.firehoseArn;
        this.roleArn = builder.roleArn;
        this.state = builder.state;
        this.creationDate = builder.creationDate;
        this.lastUpdateDate = builder.lastUpdateDate;
        this.outputFormat = builder.outputFormat;
        this.statisticsConfigurations = builder.statisticsConfigurations == null ? null : Collections.unmodifiableList(builder.statisticsConfigurations);
        this.includeLinkedAccountsMetrics = builder.includeLinkedAccountsMetrics;
    }

    /**
     * The ARN of the metric stream.
     */
    public String getArn() {
        return arn;
    }

    /**
     * The name of the metric stream.
     */
    public String getName() {
        return name;
    }

    /**
     * If this array of metric namespaces is present, then these namespaces are the only metric namespaces that are
     * streamed by this metric stream.
     */
    public List<MetricStreamFilter> getIncludeFilters() {
        if (includeFilters == null) {
            return Collections.emptyList();
        }
        return includeFilters;
    }

    public boolean hasIncludeFilters() {
        return includeFilters != null;
    }

    /**
     * If this array of metric namespaces is present, then these namespaces are the only metric namespaces that are not
     * streamed by this metric stream. In this case, all other metric namespaces in the account are streamed by this
     * metric stream.
     */
    public List<MetricStreamFilter> getExcludeFilters() {
        if (excludeFilters == null) {
            return Collections.emptyList();
        }
        return excludeFilters;
    }

    public boolean hasExcludeFilters() {
        return excludeFilters != null;
    }

    /**
     * The ARN of the Amazon Kinesis Data Firehose delivery stream that is used by this metric stream.
     */
    public String getFirehoseArn() {
        return firehoseArn;
    }

    /**
     * The ARN of the IAM role that is used by this metric stream.
     */
    public String getRoleArn() {
        return roleArn;
    }

    /**
     * The state of the metric stream. The possible values are <code>running</code> and <code>stopped</code>.
     */
    public String getState() {
        return state;
    }

    /**
     * The date that the metric stream was created.
     */
    public Instant getCreationDate() {
        return creationDate;
    }

    /**
     * The date of the most recent update to the metric stream's configuration.
     */
    public Instant getLastUpdateDate() {
        return lastUpdateDate;
    }

    /**
     * The output format for the stream. Valid values are <code>json</code>, <code>opentelemetry1.0</code>, and
     * <code>opentelemetry0.7</code>. For more information about metric stream output formats, see <a
     * href="https://docs.aws.amazon.com/AmazonCloudWatch/latest/monitoring/CloudWatch-metric-streams-formats.html">Metric
     * streams output formats</a>.
     */
    public MetricStreamOutputFormat getOutputFormat() {
        return outputFormat;
    }

    /**
     * Each entry in this array displays information about one or more metrics that include additional statistics in the
     * metric stream. For more information about the additional statistics, see <a
     * href="https://docs.aws.amazon.com/AmazonCloudWatch/latest/monitoring/Statistics-definitions.html"> CloudWatch
     * statistics definitions</a>.
     */
    public List<MetricStreamStatisticsConfiguration> getStatisticsConfigurations() {
        if (statisticsConfigurations == null) {
            return Collections.emptyList();
        }
        return statisticsConfigurations;
    }

    public boolean hasStatisticsConfigurations() {
        return statisticsConfigurations != null;
    }

    /**
     * If this is <code>true</code> and this metric stream is in a monitoring account, then the stream includes metrics
     * from source accounts that the monitoring account is linked to.
     */
    public Boolean isIncludeLinkedAccountsMetrics() {
        return includeLinkedAccountsMetrics;
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
        GetMetricStreamOutput that = (GetMetricStreamOutput) other;
        return Objects.equals(this.includeLinkedAccountsMetrics, that.includeLinkedAccountsMetrics)
               && Objects.equals(this.arn, that.arn)
               && Objects.equals(this.name, that.name)
               && Objects.equals(this.firehoseArn, that.firehoseArn)
               && Objects.equals(this.roleArn, that.roleArn)
               && Objects.equals(this.state, that.state)
               && Objects.equals(this.outputFormat, that.outputFormat)
               && Objects.equals(this.creationDate, that.creationDate)
               && Objects.equals(this.lastUpdateDate, that.lastUpdateDate)
               && Objects.equals(this.includeFilters, that.includeFilters)
               && Objects.equals(this.excludeFilters, that.excludeFilters)
               && Objects.equals(this.statisticsConfigurations, that.statisticsConfigurations);
    }

    @Override
    public int hashCode() {
        int $hc = Objects.hashCode(arn);
        $hc = 31 * $hc + Objects.hashCode(name);
        $hc = 31 * $hc + Objects.hashCode(includeFilters);
        $hc = 31 * $hc + Objects.hashCode(excludeFilters);
        $hc = 31 * $hc + Objects.hashCode(firehoseArn);
        $hc = 31 * $hc + Objects.hashCode(roleArn);
        $hc = 31 * $hc + Objects.hashCode(state);
        $hc = 31 * $hc + Objects.hashCode(creationDate);
        $hc = 31 * $hc + Objects.hashCode(lastUpdateDate);
        $hc = 31 * $hc + Objects.hashCode(outputFormat);
        $hc = 31 * $hc + Objects.hashCode(statisticsConfigurations);
        $hc = 31 * $hc + Objects.hashCode(includeLinkedAccountsMetrics);
        return $hc;
    }

    @Override
    public Schema schema() {
        return $SCHEMA;
    }

    @Override
    public void serializeMembers(ShapeSerializer serializer) {
        if (arn != null) {
            serializer.writeString($SCHEMA_ARN, arn);
        }
        if (name != null) {
            serializer.writeString($SCHEMA_NAME, name);
        }
        if (includeFilters != null) {
            serializer.writeList($SCHEMA_INCLUDE_FILTERS, includeFilters, includeFilters.size(), SharedSerde.MetricStreamFiltersSerializer.INSTANCE);
        }
        if (excludeFilters != null) {
            serializer.writeList($SCHEMA_EXCLUDE_FILTERS, excludeFilters, excludeFilters.size(), SharedSerde.MetricStreamFiltersSerializer.INSTANCE);
        }
        if (firehoseArn != null) {
            serializer.writeString($SCHEMA_FIREHOSE_ARN, firehoseArn);
        }
        if (roleArn != null) {
            serializer.writeString($SCHEMA_ROLE_ARN, roleArn);
        }
        if (state != null) {
            serializer.writeString($SCHEMA_STATE, state);
        }
        if (creationDate != null) {
            serializer.writeTimestamp($SCHEMA_CREATION_DATE, creationDate);
        }
        if (lastUpdateDate != null) {
            serializer.writeTimestamp($SCHEMA_LAST_UPDATE_DATE, lastUpdateDate);
        }
        if (outputFormat != null) {
            serializer.writeString($SCHEMA_OUTPUT_FORMAT, outputFormat.getValue());
        }
        if (statisticsConfigurations != null) {
            serializer.writeList($SCHEMA_STATISTICS_CONFIGURATIONS, statisticsConfigurations, statisticsConfigurations.size(), SharedSerde.MetricStreamStatisticsConfigurationsSerializer.INSTANCE);
        }
        if (includeLinkedAccountsMetrics != null) {
            serializer.writeBoolean($SCHEMA_INCLUDE_LINKED_ACCOUNTS_METRICS, includeLinkedAccountsMetrics);
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T getMemberValue(Schema member) {
        return switch (member.memberIndex()) {
            case 0 -> (T) SchemaUtils.validateSameMember($SCHEMA_ARN, member, arn);
            case 1 -> (T) SchemaUtils.validateSameMember($SCHEMA_NAME, member, name);
            case 2 -> (T) SchemaUtils.validateSameMember($SCHEMA_INCLUDE_FILTERS, member, includeFilters);
            case 3 -> (T) SchemaUtils.validateSameMember($SCHEMA_EXCLUDE_FILTERS, member, excludeFilters);
            case 4 -> (T) SchemaUtils.validateSameMember($SCHEMA_FIREHOSE_ARN, member, firehoseArn);
            case 5 -> (T) SchemaUtils.validateSameMember($SCHEMA_ROLE_ARN, member, roleArn);
            case 6 -> (T) SchemaUtils.validateSameMember($SCHEMA_STATE, member, state);
            case 7 -> (T) SchemaUtils.validateSameMember($SCHEMA_CREATION_DATE, member, creationDate);
            case 8 -> (T) SchemaUtils.validateSameMember($SCHEMA_LAST_UPDATE_DATE, member, lastUpdateDate);
            case 9 -> (T) SchemaUtils.validateSameMember($SCHEMA_OUTPUT_FORMAT, member, outputFormat);
            case 10 -> (T) SchemaUtils.validateSameMember($SCHEMA_STATISTICS_CONFIGURATIONS, member, statisticsConfigurations);
            case 11 -> (T) SchemaUtils.validateSameMember($SCHEMA_INCLUDE_LINKED_ACCOUNTS_METRICS, member, includeLinkedAccountsMetrics);
            default -> throw new IllegalArgumentException("Attempted to get non-existent member: " + member.id());
        };
    }

    /**
     * Create a new builder containing all the current property values of this object.
     *
     * <p><strong>Note:</strong> This method performs only a shallow copy of the original properties.
     *
     * @return a builder for {@link GetMetricStreamOutput}.
     */
    public Builder toBuilder() {
        var builder = new Builder();
        builder.arn(this.arn);
        builder.name(this.name);
        builder.includeFilters(this.includeFilters);
        builder.excludeFilters(this.excludeFilters);
        builder.firehoseArn(this.firehoseArn);
        builder.roleArn(this.roleArn);
        builder.state(this.state);
        builder.creationDate(this.creationDate);
        builder.lastUpdateDate(this.lastUpdateDate);
        builder.outputFormat(this.outputFormat);
        builder.statisticsConfigurations(this.statisticsConfigurations);
        builder.includeLinkedAccountsMetrics(this.includeLinkedAccountsMetrics);
        return builder;
    }

    /**
     * @return returns a new Builder.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for {@link GetMetricStreamOutput}.
     */
    public static final class Builder implements ShapeBuilder<GetMetricStreamOutput> {
        private String arn;
        private String name;
        private List<MetricStreamFilter> includeFilters;
        private List<MetricStreamFilter> excludeFilters;
        private String firehoseArn;
        private String roleArn;
        private String state;
        private Instant creationDate;
        private Instant lastUpdateDate;
        private MetricStreamOutputFormat outputFormat;
        private List<MetricStreamStatisticsConfiguration> statisticsConfigurations;
        private Boolean includeLinkedAccountsMetrics;

        private Builder() {}

        @Override
        public Schema schema() {
            return $SCHEMA;
        }

        /**
         * The ARN of the metric stream.
         *
         * @return this builder.
         */
        public Builder arn(String arn) {
            this.arn = arn;
            return this;
        }

        /**
         * The name of the metric stream.
         *
         * @return this builder.
         */
        public Builder name(String name) {
            this.name = name;
            return this;
        }

        /**
         * If this array of metric namespaces is present, then these namespaces are the only metric namespaces that are
         * streamed by this metric stream.
         *
         * @return this builder.
         */
        public Builder includeFilters(List<MetricStreamFilter> includeFilters) {
            this.includeFilters = includeFilters;
            return this;
        }

        /**
         * If this array of metric namespaces is present, then these namespaces are the only metric namespaces that are not
         * streamed by this metric stream. In this case, all other metric namespaces in the account are streamed by this
         * metric stream.
         *
         * @return this builder.
         */
        public Builder excludeFilters(List<MetricStreamFilter> excludeFilters) {
            this.excludeFilters = excludeFilters;
            return this;
        }

        /**
         * The ARN of the Amazon Kinesis Data Firehose delivery stream that is used by this metric stream.
         *
         * @return this builder.
         */
        public Builder firehoseArn(String firehoseArn) {
            this.firehoseArn = firehoseArn;
            return this;
        }

        /**
         * The ARN of the IAM role that is used by this metric stream.
         *
         * @return this builder.
         */
        public Builder roleArn(String roleArn) {
            this.roleArn = roleArn;
            return this;
        }

        /**
         * The state of the metric stream. The possible values are <code>running</code> and <code>stopped</code>.
         *
         * @return this builder.
         */
        public Builder state(String state) {
            this.state = state;
            return this;
        }

        /**
         * The date that the metric stream was created.
         *
         * @return this builder.
         */
        public Builder creationDate(Instant creationDate) {
            this.creationDate = creationDate;
            return this;
        }

        /**
         * The date of the most recent update to the metric stream's configuration.
         *
         * @return this builder.
         */
        public Builder lastUpdateDate(Instant lastUpdateDate) {
            this.lastUpdateDate = lastUpdateDate;
            return this;
        }

        /**
         * The output format for the stream. Valid values are <code>json</code>, <code>opentelemetry1.0</code>, and
         * <code>opentelemetry0.7</code>. For more information about metric stream output formats, see <a
         * href="https://docs.aws.amazon.com/AmazonCloudWatch/latest/monitoring/CloudWatch-metric-streams-formats.html">Metric
         * streams output formats</a>.
         *
         * @return this builder.
         */
        public Builder outputFormat(MetricStreamOutputFormat outputFormat) {
            this.outputFormat = outputFormat;
            return this;
        }

        /**
         * Each entry in this array displays information about one or more metrics that include additional statistics in the
         * metric stream. For more information about the additional statistics, see <a
         * href="https://docs.aws.amazon.com/AmazonCloudWatch/latest/monitoring/Statistics-definitions.html"> CloudWatch
         * statistics definitions</a>.
         *
         * @return this builder.
         */
        public Builder statisticsConfigurations(List<MetricStreamStatisticsConfiguration> statisticsConfigurations) {
            this.statisticsConfigurations = statisticsConfigurations;
            return this;
        }

        /**
         * If this is <code>true</code> and this metric stream is in a monitoring account, then the stream includes metrics
         * from source accounts that the monitoring account is linked to.
         *
         * @return this builder.
         */
        public Builder includeLinkedAccountsMetrics(Boolean includeLinkedAccountsMetrics) {
            this.includeLinkedAccountsMetrics = includeLinkedAccountsMetrics;
            return this;
        }

        @Override
        public GetMetricStreamOutput build() {
            return new GetMetricStreamOutput(this);
        }

        @Override
        @SuppressWarnings("unchecked")
        public void setMemberValue(Schema member, Object value) {
            switch (member.memberIndex()) {
                case 0 -> arn((String) SchemaUtils.validateSameMember($SCHEMA_ARN, member, value));
                case 1 -> name((String) SchemaUtils.validateSameMember($SCHEMA_NAME, member, value));
                case 2 -> includeFilters((List<MetricStreamFilter>) SchemaUtils.validateSameMember($SCHEMA_INCLUDE_FILTERS, member, value));
                case 3 -> excludeFilters((List<MetricStreamFilter>) SchemaUtils.validateSameMember($SCHEMA_EXCLUDE_FILTERS, member, value));
                case 4 -> firehoseArn((String) SchemaUtils.validateSameMember($SCHEMA_FIREHOSE_ARN, member, value));
                case 5 -> roleArn((String) SchemaUtils.validateSameMember($SCHEMA_ROLE_ARN, member, value));
                case 6 -> state((String) SchemaUtils.validateSameMember($SCHEMA_STATE, member, value));
                case 7 -> creationDate((Instant) SchemaUtils.validateSameMember($SCHEMA_CREATION_DATE, member, value));
                case 8 -> lastUpdateDate((Instant) SchemaUtils.validateSameMember($SCHEMA_LAST_UPDATE_DATE, member, value));
                case 9 -> outputFormat((MetricStreamOutputFormat) SchemaUtils.validateSameMember($SCHEMA_OUTPUT_FORMAT, member, value));
                case 10 -> statisticsConfigurations((List<MetricStreamStatisticsConfiguration>) SchemaUtils.validateSameMember($SCHEMA_STATISTICS_CONFIGURATIONS, member, value));
                case 11 -> includeLinkedAccountsMetrics((Boolean) SchemaUtils.validateSameMember($SCHEMA_INCLUDE_LINKED_ACCOUNTS_METRICS, member, value));
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
                    case 0 -> builder.arn(de.readString(member));
                    case 1 -> builder.name(de.readString(member));
                    case 2 -> builder.includeFilters(SharedSerde.deserializeMetricStreamFilters(member, de));
                    case 3 -> builder.excludeFilters(SharedSerde.deserializeMetricStreamFilters(member, de));
                    case 4 -> builder.firehoseArn(de.readString(member));
                    case 5 -> builder.roleArn(de.readString(member));
                    case 6 -> builder.state(de.readString(member));
                    case 7 -> builder.creationDate(de.readTimestamp(member));
                    case 8 -> builder.lastUpdateDate(de.readTimestamp(member));
                    case 9 -> builder.outputFormat(MetricStreamOutputFormat.builder().deserializeMember(de, member).build());
                    case 10 -> builder.statisticsConfigurations(SharedSerde.deserializeMetricStreamStatisticsConfigurations(member, de));
                    case 11 -> builder.includeLinkedAccountsMetrics(de.readBoolean(member));
                    default -> throw new IllegalArgumentException("Unexpected member: " + member.memberName());
                }
            }
        }
    }
}
