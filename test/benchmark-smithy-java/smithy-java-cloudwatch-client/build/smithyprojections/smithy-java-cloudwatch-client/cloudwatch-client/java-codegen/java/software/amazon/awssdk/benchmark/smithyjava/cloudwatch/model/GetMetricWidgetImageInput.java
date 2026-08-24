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

@SmithyGenerated
public final class GetMetricWidgetImageInput implements SerializableStruct {

    public static final Schema $SCHEMA = Schemas.GET_METRIC_WIDGET_IMAGE_INPUT;
    private static final Schema $SCHEMA_METRIC_WIDGET = $SCHEMA.member("MetricWidget");
    private static final Schema $SCHEMA_OUTPUT_FORMAT = $SCHEMA.member("OutputFormat");

    public static final ShapeId $ID = $SCHEMA.id();

    private final transient String metricWidget;
    private final transient String outputFormat;

    private GetMetricWidgetImageInput(Builder builder) {
        this.metricWidget = builder.metricWidget;
        this.outputFormat = builder.outputFormat;
    }

    /**
     * A JSON string that defines the bitmap graph to be retrieved. The string includes the metrics to include in the
     * graph, statistics, annotations, title, axis limits, and so on. You can include only one <code>MetricWidget</code>
     * parameter in each <code>GetMetricWidgetImage</code> call.
     *
     * <p>For more information about the syntax of <code>MetricWidget</code> see <a href="https://docs.aws.amazon.com/AmazonCloudWatch/latest/monitoring/CloudWatch-Metric-Widget-Structure.html">GetMetricWidgetImage: Metric Widget
     * Structure and Syntax</a>.
     *
     * <p>If any metric on the graph could not load all the requested data points, an orange triangle with an
     * exclamation point appears next to the graph legend.
     */
    public String getMetricWidget() {
        return metricWidget;
    }

    /**
     * The format of the resulting image. Only PNG images are supported.
     *
     * <p>The default is <code>png</code>. If you specify <code>png</code>, the API returns an HTTP response with the
     * content-type set to <code>text/xml</code>. The image data is in a <code>MetricWidgetImage</code> field. For
     * example:
     *
     * <p><code> &lt;GetMetricWidgetImageResponse xmlns=&lt;URLstring&gt;&gt;</code>
     *
     * <p><code> &lt;GetMetricWidgetImageResult&gt;</code>
     *
     * <p><code> &lt;MetricWidgetImage&gt;</code>
     *
     * <p><code> iVBORw0KGgoAAAANSUhEUgAAAlgAAAGQEAYAAAAip...</code>
     *
     * <p><code></code>
     *
     * <p><code></code>
     *
     * <p><code> &lt;ResponseMetadata&gt;</code>
     *
     * <p><code> &lt;RequestId&gt;6f0d4192-4d42-11e8-82c1-f539a07e0e3b</code>
     *
     * <p><code></code>
     *
     * <p><code></code>
     *
     * <p>The <code>image/png</code> setting is intended only for custom HTTP requests. For most use cases, and all
     * actions using an Amazon Web Services SDK, you should use <code>png</code>. If you specify <code>image/png</code>,
     * the HTTP response has a content-type set to <code>image/png</code>, and the body of the response is a PNG image.
     */
    public String getOutputFormat() {
        return outputFormat;
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
        GetMetricWidgetImageInput that = (GetMetricWidgetImageInput) other;
        return Objects.equals(this.metricWidget, that.metricWidget)
               && Objects.equals(this.outputFormat, that.outputFormat);
    }

    @Override
    public int hashCode() {
        int $hc = Objects.hashCode(metricWidget);
        $hc = 31 * $hc + Objects.hashCode(outputFormat);
        return $hc;
    }

    @Override
    public Schema schema() {
        return $SCHEMA;
    }

    @Override
    public void serializeMembers(ShapeSerializer serializer) {
        if (metricWidget != null) {
            serializer.writeString($SCHEMA_METRIC_WIDGET, metricWidget);
        }
        if (outputFormat != null) {
            serializer.writeString($SCHEMA_OUTPUT_FORMAT, outputFormat);
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T getMemberValue(Schema member) {
        return switch (member.memberIndex()) {
            case 0 -> (T) SchemaUtils.validateSameMember($SCHEMA_METRIC_WIDGET, member, metricWidget);
            case 1 -> (T) SchemaUtils.validateSameMember($SCHEMA_OUTPUT_FORMAT, member, outputFormat);
            default -> throw new IllegalArgumentException("Attempted to get non-existent member: " + member.id());
        };
    }

    /**
     * Create a new builder containing all the current property values of this object.
     *
     * <p><strong>Note:</strong> This method performs only a shallow copy of the original properties.
     *
     * @return a builder for {@link GetMetricWidgetImageInput}.
     */
    public Builder toBuilder() {
        var builder = new Builder();
        builder.metricWidget(this.metricWidget);
        builder.outputFormat(this.outputFormat);
        return builder;
    }

    /**
     * @return returns a new Builder.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for {@link GetMetricWidgetImageInput}.
     */
    public static final class Builder implements ShapeBuilder<GetMetricWidgetImageInput> {
        private final PresenceTracker tracker = PresenceTracker.of($SCHEMA);
        private String metricWidget;
        private String outputFormat;

        private Builder() {
            // Tell the tracker to assume clientOptional members are present.
            tracker.setMember($SCHEMA_METRIC_WIDGET);
        }

        @Override
        public Schema schema() {
            return $SCHEMA;
        }

        /**
         * A JSON string that defines the bitmap graph to be retrieved. The string includes the metrics to include in the
         * graph, statistics, annotations, title, axis limits, and so on. You can include only one <code>MetricWidget</code>
         * parameter in each <code>GetMetricWidgetImage</code> call.
         *
         * <p>For more information about the syntax of <code>MetricWidget</code> see <a href="https://docs.aws.amazon.com/AmazonCloudWatch/latest/monitoring/CloudWatch-Metric-Widget-Structure.html">GetMetricWidgetImage: Metric Widget
         * Structure and Syntax</a>.
         *
         * <p>If any metric on the graph could not load all the requested data points, an orange triangle with an
         * exclamation point appears next to the graph legend.
         *
         * <p><strong>Required</strong>
         * @return this builder.
         */
        public Builder metricWidget(String metricWidget) {
            this.metricWidget = Objects.requireNonNull(metricWidget, "metricWidget cannot be null");
            tracker.setMember($SCHEMA_METRIC_WIDGET);
            return this;
        }

        /**
         * The format of the resulting image. Only PNG images are supported.
         *
         * <p>The default is <code>png</code>. If you specify <code>png</code>, the API returns an HTTP response with the
         * content-type set to <code>text/xml</code>. The image data is in a <code>MetricWidgetImage</code> field. For
         * example:
         *
         * <p><code> &lt;GetMetricWidgetImageResponse xmlns=&lt;URLstring&gt;&gt;</code>
         *
         * <p><code> &lt;GetMetricWidgetImageResult&gt;</code>
         *
         * <p><code> &lt;MetricWidgetImage&gt;</code>
         *
         * <p><code> iVBORw0KGgoAAAANSUhEUgAAAlgAAAGQEAYAAAAip...</code>
         *
         * <p><code></code>
         *
         * <p><code></code>
         *
         * <p><code> &lt;ResponseMetadata&gt;</code>
         *
         * <p><code> &lt;RequestId&gt;6f0d4192-4d42-11e8-82c1-f539a07e0e3b</code>
         *
         * <p><code></code>
         *
         * <p><code></code>
         *
         * <p>The <code>image/png</code> setting is intended only for custom HTTP requests. For most use cases, and all
         * actions using an Amazon Web Services SDK, you should use <code>png</code>. If you specify <code>image/png</code>,
         * the HTTP response has a content-type set to <code>image/png</code>, and the body of the response is a PNG image.
         *
         * @return this builder.
         */
        public Builder outputFormat(String outputFormat) {
            this.outputFormat = outputFormat;
            return this;
        }

        @Override
        public GetMetricWidgetImageInput build() {
            tracker.validate();
            return new GetMetricWidgetImageInput(this);
        }

        @Override
        @SuppressWarnings("unchecked")
        public void setMemberValue(Schema member, Object value) {
            switch (member.memberIndex()) {
                case 0 -> metricWidget((String) SchemaUtils.validateSameMember($SCHEMA_METRIC_WIDGET, member, value));
                case 1 -> outputFormat((String) SchemaUtils.validateSameMember($SCHEMA_OUTPUT_FORMAT, member, value));
                default -> ShapeBuilder.super.setMemberValue(member, value);
            }
        }

        @Override
        public ShapeBuilder<GetMetricWidgetImageInput> errorCorrection() {
            if (tracker.allSet()) {
                return this;
            }
            if (!tracker.checkMember($SCHEMA_METRIC_WIDGET)) {
                metricWidget("");
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
                    case 0 -> builder.metricWidget(de.readString(member));
                    case 1 -> builder.outputFormat(de.readString(member));
                    default -> throw new IllegalArgumentException("Unexpected member: " + member.memberName());
                }
            }
        }
    }
}
