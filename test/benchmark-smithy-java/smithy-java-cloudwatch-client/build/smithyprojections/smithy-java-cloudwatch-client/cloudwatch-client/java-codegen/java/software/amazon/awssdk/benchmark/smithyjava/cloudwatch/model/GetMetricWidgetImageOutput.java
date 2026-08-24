package software.amazon.awssdk.benchmark.smithyjava.cloudwatch.model;

import java.nio.ByteBuffer;
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
public final class GetMetricWidgetImageOutput implements SerializableStruct {

    public static final Schema $SCHEMA = Schemas.GET_METRIC_WIDGET_IMAGE_OUTPUT;
    private static final Schema $SCHEMA_METRIC_WIDGET_IMAGE = $SCHEMA.member("MetricWidgetImage");

    public static final ShapeId $ID = $SCHEMA.id();

    private final transient ByteBuffer metricWidgetImage;

    private GetMetricWidgetImageOutput(Builder builder) {
        this.metricWidgetImage = builder.metricWidgetImage == null ? null : builder.metricWidgetImage.duplicate();
    }

    /**
     * The image of the graph, in the output format specified. The output is base64-encoded.
     */
    public ByteBuffer getMetricWidgetImage() {
        return metricWidgetImage;
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
        GetMetricWidgetImageOutput that = (GetMetricWidgetImageOutput) other;
        return Objects.equals(this.metricWidgetImage, that.metricWidgetImage);
    }

    @Override
    public int hashCode() {
        int $hc = Objects.hashCode(metricWidgetImage);
        return $hc;
    }

    @Override
    public Schema schema() {
        return $SCHEMA;
    }

    @Override
    public void serializeMembers(ShapeSerializer serializer) {
        if (metricWidgetImage != null) {
            serializer.writeBlob($SCHEMA_METRIC_WIDGET_IMAGE, metricWidgetImage);
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T getMemberValue(Schema member) {
        return switch (member.memberIndex()) {
            case 0 -> (T) SchemaUtils.validateSameMember($SCHEMA_METRIC_WIDGET_IMAGE, member, metricWidgetImage);
            default -> throw new IllegalArgumentException("Attempted to get non-existent member: " + member.id());
        };
    }

    /**
     * Create a new builder containing all the current property values of this object.
     *
     * <p><strong>Note:</strong> This method performs only a shallow copy of the original properties.
     *
     * @return a builder for {@link GetMetricWidgetImageOutput}.
     */
    public Builder toBuilder() {
        var builder = new Builder();
        builder.metricWidgetImage(this.metricWidgetImage);
        return builder;
    }

    /**
     * @return returns a new Builder.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for {@link GetMetricWidgetImageOutput}.
     */
    public static final class Builder implements ShapeBuilder<GetMetricWidgetImageOutput> {
        private ByteBuffer metricWidgetImage;

        private Builder() {}

        @Override
        public Schema schema() {
            return $SCHEMA;
        }

        /**
         * The image of the graph, in the output format specified. The output is base64-encoded.
         *
         * @return this builder.
         */
        public Builder metricWidgetImage(ByteBuffer metricWidgetImage) {
            this.metricWidgetImage = metricWidgetImage;
            return this;
        }

        @Override
        public GetMetricWidgetImageOutput build() {
            return new GetMetricWidgetImageOutput(this);
        }

        @Override
        @SuppressWarnings("unchecked")
        public void setMemberValue(Schema member, Object value) {
            switch (member.memberIndex()) {
                case 0 -> metricWidgetImage((ByteBuffer) SchemaUtils.validateSameMember($SCHEMA_METRIC_WIDGET_IMAGE, member, value));
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
                    case 0 -> builder.metricWidgetImage(de.readBlob(member));
                    default -> throw new IllegalArgumentException("Unexpected member: " + member.memberName());
                }
            }
        }
    }
}
