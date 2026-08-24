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
public final class PutMetricStreamInput implements SerializableStruct {

    public static final Schema $SCHEMA = Schemas.PUT_METRIC_STREAM_INPUT;
    private static final Schema $SCHEMA_NAME = $SCHEMA.member("Name");
    private static final Schema $SCHEMA_INCLUDE_FILTERS = $SCHEMA.member("IncludeFilters");
    private static final Schema $SCHEMA_EXCLUDE_FILTERS = $SCHEMA.member("ExcludeFilters");
    private static final Schema $SCHEMA_FIREHOSE_ARN = $SCHEMA.member("FirehoseArn");
    private static final Schema $SCHEMA_ROLE_ARN = $SCHEMA.member("RoleArn");
    private static final Schema $SCHEMA_OUTPUT_FORMAT = $SCHEMA.member("OutputFormat");
    private static final Schema $SCHEMA_TAGS = $SCHEMA.member("Tags");
    private static final Schema $SCHEMA_STATISTICS_CONFIGURATIONS = $SCHEMA.member("StatisticsConfigurations");
    private static final Schema $SCHEMA_INCLUDE_LINKED_ACCOUNTS_METRICS = $SCHEMA.member("IncludeLinkedAccountsMetrics");

    public static final ShapeId $ID = $SCHEMA.id();

    private final transient String name;
    private final transient List<MetricStreamFilter> includeFilters;
    private final transient List<MetricStreamFilter> excludeFilters;
    private final transient String firehoseArn;
    private final transient String roleArn;
    private final transient MetricStreamOutputFormat outputFormat;
    private final transient List<Tag> tags;
    private final transient List<MetricStreamStatisticsConfiguration> statisticsConfigurations;
    private final transient Boolean includeLinkedAccountsMetrics;

    private PutMetricStreamInput(Builder builder) {
        this.name = builder.name;
        this.includeFilters = builder.includeFilters == null ? null : Collections.unmodifiableList(builder.includeFilters);
        this.excludeFilters = builder.excludeFilters == null ? null : Collections.unmodifiableList(builder.excludeFilters);
        this.firehoseArn = builder.firehoseArn;
        this.roleArn = builder.roleArn;
        this.outputFormat = builder.outputFormat;
        this.tags = builder.tags == null ? null : Collections.unmodifiableList(builder.tags);
        this.statisticsConfigurations = builder.statisticsConfigurations == null ? null : Collections.unmodifiableList(builder.statisticsConfigurations);
        this.includeLinkedAccountsMetrics = builder.includeLinkedAccountsMetrics;
    }

    /**
     * If you are creating a new metric stream, this is the name for the new stream. The name must be different than the
     * names of other metric streams in this account and Region.
     *
     * <p>If you are updating a metric stream, specify the name of that stream here.
     *
     * <p>Valid characters are A-Z, a-z, 0-9, "-" and "_".
     */
    public String getName() {
        return name;
    }

    /**
     * If you specify this parameter, the stream sends only the metrics from the metric namespaces that you specify
     * here.
     *
     * <p>You cannot include <code>IncludeFilters</code> and <code>ExcludeFilters</code> in the same operation.
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
     * If you specify this parameter, the stream sends metrics from all metric namespaces except for the namespaces that
     * you specify here.
     *
     * <p>You cannot include <code>ExcludeFilters</code> and <code>IncludeFilters</code> in the same operation.
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
     * The ARN of the Amazon Kinesis Data Firehose delivery stream to use for this metric stream. This Amazon Kinesis
     * Data Firehose delivery stream must already exist and must be in the same account as the metric stream.
     */
    public String getFirehoseArn() {
        return firehoseArn;
    }

    /**
     * The ARN of an IAM role that this metric stream will use to access Amazon Kinesis Data Firehose resources. This
     * IAM role must already exist and must be in the same account as the metric stream. This IAM role must include the
     * following permissions:
     *
     * <ul>
     *   <li>
     *     firehose:PutRecord
     *   </li>
     *   <li>
     *     firehose:PutRecordBatch
     *   </li>
     * </ul>
     */
    public String getRoleArn() {
        return roleArn;
    }

    /**
     * The output format for the stream. Valid values are <code>json</code>, <code>opentelemetry1.0</code>, and
     * <code>opentelemetry0.7</code>. For more information about metric stream output formats, see <a
     * href="https://docs.aws.amazon.com/AmazonCloudWatch/latest/monitoring/CloudWatch-metric-streams-formats.html">
     * Metric streams output formats</a>.
     */
    public MetricStreamOutputFormat getOutputFormat() {
        return outputFormat;
    }

    /**
     * A list of key-value pairs to associate with the metric stream. You can associate as many as 50 tags with a metric
     * stream.
     *
     * <p>Tags can help you organize and categorize your resources. You can also use them to scope user permissions by
     * granting a user permission to access or change only resources with certain tag values.
     *
     * <p>You can use this parameter only when you are creating a new metric stream. If you are using this operation to
     * update an existing metric stream, any tags you specify in this parameter are ignored. To change the tags of an
     * existing metric stream, use <a href="https://docs.aws.amazon.com/AmazonCloudWatch/latest/APIReference/API_TagResource.html">TagResource</a> or <a href="https://docs.aws.amazon.com/AmazonCloudWatch/latest/APIReference/API_UntagResource.html">UntagResource</a>.
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

    /**
     * By default, a metric stream always sends the <code>MAX</code>, <code>MIN</code>, <code>SUM</code>, and <code>
     * SAMPLECOUNT</code> statistics for each metric that is streamed. You can use this parameter to have the metric
     * stream also send additional statistics in the stream. This array can have up to 100 members.
     *
     * <p>For each entry in this array, you specify one or more metrics and the list of additional statistics to stream
     * for those metrics. The additional statistics that you can stream depend on the stream's <code>OutputFormat</code>
     * . If the <code>OutputFormat</code> is <code>json</code>, you can stream any additional statistic that is
     * supported by CloudWatch, listed in <a href="https://docs.aws.amazon.com/AmazonCloudWatch/latest/monitoring/Statistics-definitions.html"> CloudWatch statistics definitions</a>. If the <code>OutputFormat</code> is
     * <code>opentelemetry1.0</code> or <code>opentelemetry0.7</code>, you can stream percentile statistics such as p95,
     * p99.9, and so on.
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
     * If you are creating a metric stream in a monitoring account, specify <code>true</code> to include metrics from
     * source accounts in the metric stream.
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
        PutMetricStreamInput that = (PutMetricStreamInput) other;
        return Objects.equals(this.includeLinkedAccountsMetrics, that.includeLinkedAccountsMetrics)
               && Objects.equals(this.name, that.name)
               && Objects.equals(this.firehoseArn, that.firehoseArn)
               && Objects.equals(this.roleArn, that.roleArn)
               && Objects.equals(this.outputFormat, that.outputFormat)
               && Objects.equals(this.includeFilters, that.includeFilters)
               && Objects.equals(this.excludeFilters, that.excludeFilters)
               && Objects.equals(this.tags, that.tags)
               && Objects.equals(this.statisticsConfigurations, that.statisticsConfigurations);
    }

    @Override
    public int hashCode() {
        int $hc = Objects.hashCode(name);
        $hc = 31 * $hc + Objects.hashCode(includeFilters);
        $hc = 31 * $hc + Objects.hashCode(excludeFilters);
        $hc = 31 * $hc + Objects.hashCode(firehoseArn);
        $hc = 31 * $hc + Objects.hashCode(roleArn);
        $hc = 31 * $hc + Objects.hashCode(outputFormat);
        $hc = 31 * $hc + Objects.hashCode(tags);
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
        if (outputFormat != null) {
            serializer.writeString($SCHEMA_OUTPUT_FORMAT, outputFormat.getValue());
        }
        if (tags != null) {
            serializer.writeList($SCHEMA_TAGS, tags, tags.size(), SharedSerde.TagListSerializer.INSTANCE);
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
            case 0 -> (T) SchemaUtils.validateSameMember($SCHEMA_NAME, member, name);
            case 1 -> (T) SchemaUtils.validateSameMember($SCHEMA_FIREHOSE_ARN, member, firehoseArn);
            case 2 -> (T) SchemaUtils.validateSameMember($SCHEMA_ROLE_ARN, member, roleArn);
            case 3 -> (T) SchemaUtils.validateSameMember($SCHEMA_OUTPUT_FORMAT, member, outputFormat);
            case 4 -> (T) SchemaUtils.validateSameMember($SCHEMA_INCLUDE_FILTERS, member, includeFilters);
            case 5 -> (T) SchemaUtils.validateSameMember($SCHEMA_EXCLUDE_FILTERS, member, excludeFilters);
            case 6 -> (T) SchemaUtils.validateSameMember($SCHEMA_TAGS, member, tags);
            case 7 -> (T) SchemaUtils.validateSameMember($SCHEMA_STATISTICS_CONFIGURATIONS, member, statisticsConfigurations);
            case 8 -> (T) SchemaUtils.validateSameMember($SCHEMA_INCLUDE_LINKED_ACCOUNTS_METRICS, member, includeLinkedAccountsMetrics);
            default -> throw new IllegalArgumentException("Attempted to get non-existent member: " + member.id());
        };
    }

    /**
     * Create a new builder containing all the current property values of this object.
     *
     * <p><strong>Note:</strong> This method performs only a shallow copy of the original properties.
     *
     * @return a builder for {@link PutMetricStreamInput}.
     */
    public Builder toBuilder() {
        var builder = new Builder();
        builder.name(this.name);
        builder.includeFilters(this.includeFilters);
        builder.excludeFilters(this.excludeFilters);
        builder.firehoseArn(this.firehoseArn);
        builder.roleArn(this.roleArn);
        builder.outputFormat(this.outputFormat);
        builder.tags(this.tags);
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
     * Builder for {@link PutMetricStreamInput}.
     */
    public static final class Builder implements ShapeBuilder<PutMetricStreamInput> {
        private final PresenceTracker tracker = PresenceTracker.of($SCHEMA);
        private String name;
        private List<MetricStreamFilter> includeFilters;
        private List<MetricStreamFilter> excludeFilters;
        private String firehoseArn;
        private String roleArn;
        private MetricStreamOutputFormat outputFormat;
        private List<Tag> tags;
        private List<MetricStreamStatisticsConfiguration> statisticsConfigurations;
        private Boolean includeLinkedAccountsMetrics;

        private Builder() {
            // Tell the tracker to assume clientOptional members are present.
            tracker.setMember($SCHEMA_NAME);
            tracker.setMember($SCHEMA_FIREHOSE_ARN);
            tracker.setMember($SCHEMA_ROLE_ARN);
            tracker.setMember($SCHEMA_OUTPUT_FORMAT);
        }

        @Override
        public Schema schema() {
            return $SCHEMA;
        }

        /**
         * If you are creating a new metric stream, this is the name for the new stream. The name must be different than the
         * names of other metric streams in this account and Region.
         *
         * <p>If you are updating a metric stream, specify the name of that stream here.
         *
         * <p>Valid characters are A-Z, a-z, 0-9, "-" and "_".
         *
         * <p><strong>Required</strong>
         * @return this builder.
         */
        public Builder name(String name) {
            this.name = Objects.requireNonNull(name, "name cannot be null");
            tracker.setMember($SCHEMA_NAME);
            return this;
        }

        /**
         * If you specify this parameter, the stream sends only the metrics from the metric namespaces that you specify
         * here.
         *
         * <p>You cannot include <code>IncludeFilters</code> and <code>ExcludeFilters</code> in the same operation.
         *
         * @return this builder.
         */
        public Builder includeFilters(List<MetricStreamFilter> includeFilters) {
            this.includeFilters = includeFilters;
            return this;
        }

        /**
         * If you specify this parameter, the stream sends metrics from all metric namespaces except for the namespaces that
         * you specify here.
         *
         * <p>You cannot include <code>ExcludeFilters</code> and <code>IncludeFilters</code> in the same operation.
         *
         * @return this builder.
         */
        public Builder excludeFilters(List<MetricStreamFilter> excludeFilters) {
            this.excludeFilters = excludeFilters;
            return this;
        }

        /**
         * The ARN of the Amazon Kinesis Data Firehose delivery stream to use for this metric stream. This Amazon Kinesis
         * Data Firehose delivery stream must already exist and must be in the same account as the metric stream.
         *
         * <p><strong>Required</strong>
         * @return this builder.
         */
        public Builder firehoseArn(String firehoseArn) {
            this.firehoseArn = Objects.requireNonNull(firehoseArn, "firehoseArn cannot be null");
            tracker.setMember($SCHEMA_FIREHOSE_ARN);
            return this;
        }

        /**
         * The ARN of an IAM role that this metric stream will use to access Amazon Kinesis Data Firehose resources. This
         * IAM role must already exist and must be in the same account as the metric stream. This IAM role must include the
         * following permissions:
         *
         * <ul>
         *   <li>
         *     firehose:PutRecord
         *   </li>
         *   <li>
         *     firehose:PutRecordBatch
         *   </li>
         * </ul>
         *
         * <p><strong>Required</strong>
         * @return this builder.
         */
        public Builder roleArn(String roleArn) {
            this.roleArn = Objects.requireNonNull(roleArn, "roleArn cannot be null");
            tracker.setMember($SCHEMA_ROLE_ARN);
            return this;
        }

        /**
         * The output format for the stream. Valid values are <code>json</code>, <code>opentelemetry1.0</code>, and
         * <code>opentelemetry0.7</code>. For more information about metric stream output formats, see <a
         * href="https://docs.aws.amazon.com/AmazonCloudWatch/latest/monitoring/CloudWatch-metric-streams-formats.html">
         * Metric streams output formats</a>.
         *
         * <p><strong>Required</strong>
         * @return this builder.
         */
        public Builder outputFormat(MetricStreamOutputFormat outputFormat) {
            this.outputFormat = Objects.requireNonNull(outputFormat, "outputFormat cannot be null");
            tracker.setMember($SCHEMA_OUTPUT_FORMAT);
            return this;
        }

        /**
         * A list of key-value pairs to associate with the metric stream. You can associate as many as 50 tags with a metric
         * stream.
         *
         * <p>Tags can help you organize and categorize your resources. You can also use them to scope user permissions by
         * granting a user permission to access or change only resources with certain tag values.
         *
         * <p>You can use this parameter only when you are creating a new metric stream. If you are using this operation to
         * update an existing metric stream, any tags you specify in this parameter are ignored. To change the tags of an
         * existing metric stream, use <a href="https://docs.aws.amazon.com/AmazonCloudWatch/latest/APIReference/API_TagResource.html">TagResource</a> or <a href="https://docs.aws.amazon.com/AmazonCloudWatch/latest/APIReference/API_UntagResource.html">UntagResource</a>.
         *
         * @return this builder.
         */
        public Builder tags(List<Tag> tags) {
            this.tags = tags;
            return this;
        }

        /**
         * By default, a metric stream always sends the <code>MAX</code>, <code>MIN</code>, <code>SUM</code>, and <code>
         * SAMPLECOUNT</code> statistics for each metric that is streamed. You can use this parameter to have the metric
         * stream also send additional statistics in the stream. This array can have up to 100 members.
         *
         * <p>For each entry in this array, you specify one or more metrics and the list of additional statistics to stream
         * for those metrics. The additional statistics that you can stream depend on the stream's <code>OutputFormat</code>
         * . If the <code>OutputFormat</code> is <code>json</code>, you can stream any additional statistic that is
         * supported by CloudWatch, listed in <a href="https://docs.aws.amazon.com/AmazonCloudWatch/latest/monitoring/Statistics-definitions.html"> CloudWatch statistics definitions</a>. If the <code>OutputFormat</code> is
         * <code>opentelemetry1.0</code> or <code>opentelemetry0.7</code>, you can stream percentile statistics such as p95,
         * p99.9, and so on.
         *
         * @return this builder.
         */
        public Builder statisticsConfigurations(List<MetricStreamStatisticsConfiguration> statisticsConfigurations) {
            this.statisticsConfigurations = statisticsConfigurations;
            return this;
        }

        /**
         * If you are creating a metric stream in a monitoring account, specify <code>true</code> to include metrics from
         * source accounts in the metric stream.
         *
         * @return this builder.
         */
        public Builder includeLinkedAccountsMetrics(Boolean includeLinkedAccountsMetrics) {
            this.includeLinkedAccountsMetrics = includeLinkedAccountsMetrics;
            return this;
        }

        @Override
        public PutMetricStreamInput build() {
            tracker.validate();
            return new PutMetricStreamInput(this);
        }

        @Override
        @SuppressWarnings("unchecked")
        public void setMemberValue(Schema member, Object value) {
            switch (member.memberIndex()) {
                case 0 -> name((String) SchemaUtils.validateSameMember($SCHEMA_NAME, member, value));
                case 1 -> firehoseArn((String) SchemaUtils.validateSameMember($SCHEMA_FIREHOSE_ARN, member, value));
                case 2 -> roleArn((String) SchemaUtils.validateSameMember($SCHEMA_ROLE_ARN, member, value));
                case 3 -> outputFormat((MetricStreamOutputFormat) SchemaUtils.validateSameMember($SCHEMA_OUTPUT_FORMAT, member, value));
                case 4 -> includeFilters((List<MetricStreamFilter>) SchemaUtils.validateSameMember($SCHEMA_INCLUDE_FILTERS, member, value));
                case 5 -> excludeFilters((List<MetricStreamFilter>) SchemaUtils.validateSameMember($SCHEMA_EXCLUDE_FILTERS, member, value));
                case 6 -> tags((List<Tag>) SchemaUtils.validateSameMember($SCHEMA_TAGS, member, value));
                case 7 -> statisticsConfigurations((List<MetricStreamStatisticsConfiguration>) SchemaUtils.validateSameMember($SCHEMA_STATISTICS_CONFIGURATIONS, member, value));
                case 8 -> includeLinkedAccountsMetrics((Boolean) SchemaUtils.validateSameMember($SCHEMA_INCLUDE_LINKED_ACCOUNTS_METRICS, member, value));
                default -> ShapeBuilder.super.setMemberValue(member, value);
            }
        }

        @Override
        public ShapeBuilder<PutMetricStreamInput> errorCorrection() {
            if (tracker.allSet()) {
                return this;
            }
            if (!tracker.checkMember($SCHEMA_NAME)) {
                name("");
            }
            if (!tracker.checkMember($SCHEMA_FIREHOSE_ARN)) {
                firehoseArn("");
            }
            if (!tracker.checkMember($SCHEMA_ROLE_ARN)) {
                roleArn("");
            }
            if (!tracker.checkMember($SCHEMA_OUTPUT_FORMAT)) {
                outputFormat(MetricStreamOutputFormat.unknown(""));
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
                    case 0 -> builder.name(de.readString(member));
                    case 1 -> builder.firehoseArn(de.readString(member));
                    case 2 -> builder.roleArn(de.readString(member));
                    case 3 -> builder.outputFormat(MetricStreamOutputFormat.builder().deserializeMember(de, member).build());
                    case 4 -> builder.includeFilters(SharedSerde.deserializeMetricStreamFilters(member, de));
                    case 5 -> builder.excludeFilters(SharedSerde.deserializeMetricStreamFilters(member, de));
                    case 6 -> builder.tags(SharedSerde.deserializeTagList(member, de));
                    case 7 -> builder.statisticsConfigurations(SharedSerde.deserializeMetricStreamStatisticsConfigurations(member, de));
                    case 8 -> builder.includeLinkedAccountsMetrics(de.readBoolean(member));
                    default -> throw new IllegalArgumentException("Unexpected member: " + member.memberName());
                }
            }
        }
    }
}
