package software.amazon.awssdk.benchmark.smithyjava.s3.model;

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
public final class GetBucketMetricsConfigurationOutput implements SerializableStruct {

    public static final Schema $SCHEMA = Schemas2.GET_BUCKET_METRICS_CONFIGURATION_OUTPUT;
    private static final Schema $SCHEMA_METRICS_CONFIGURATION = $SCHEMA.member("MetricsConfiguration");

    public static final ShapeId $ID = $SCHEMA.id();

    private final transient MetricsConfiguration metricsConfiguration;

    private GetBucketMetricsConfigurationOutput(Builder builder) {
        this.metricsConfiguration = builder.metricsConfiguration;
    }

    /**
     * Specifies the metrics configuration.
     */
    public MetricsConfiguration getMetricsConfiguration() {
        return metricsConfiguration;
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
        GetBucketMetricsConfigurationOutput that = (GetBucketMetricsConfigurationOutput) other;
        return Objects.equals(this.metricsConfiguration, that.metricsConfiguration);
    }

    @Override
    public int hashCode() {
        int $hc = Objects.hashCode(metricsConfiguration);
        return $hc;
    }

    @Override
    public Schema schema() {
        return $SCHEMA;
    }

    @Override
    public void serializeMembers(ShapeSerializer serializer) {
        if (metricsConfiguration != null) {
            serializer.writeStruct($SCHEMA_METRICS_CONFIGURATION, metricsConfiguration);
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T getMemberValue(Schema member) {
        return switch (member.memberIndex()) {
            case 0 -> (T) SchemaUtils.validateSameMember($SCHEMA_METRICS_CONFIGURATION, member, metricsConfiguration);
            default -> throw new IllegalArgumentException("Attempted to get non-existent member: " + member.id());
        };
    }

    /**
     * Create a new builder containing all the current property values of this object.
     *
     * <p><strong>Note:</strong> This method performs only a shallow copy of the original properties.
     *
     * @return a builder for {@link GetBucketMetricsConfigurationOutput}.
     */
    public Builder toBuilder() {
        var builder = new Builder();
        builder.metricsConfiguration(this.metricsConfiguration);
        return builder;
    }

    /**
     * @return returns a new Builder.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for {@link GetBucketMetricsConfigurationOutput}.
     */
    public static final class Builder implements ShapeBuilder<GetBucketMetricsConfigurationOutput> {
        private MetricsConfiguration metricsConfiguration;

        private Builder() {}

        @Override
        public Schema schema() {
            return $SCHEMA;
        }

        /**
         * Specifies the metrics configuration.
         *
         * @return this builder.
         */
        public Builder metricsConfiguration(MetricsConfiguration metricsConfiguration) {
            this.metricsConfiguration = metricsConfiguration;
            return this;
        }

        @Override
        public GetBucketMetricsConfigurationOutput build() {
            return new GetBucketMetricsConfigurationOutput(this);
        }

        @Override
        @SuppressWarnings("unchecked")
        public void setMemberValue(Schema member, Object value) {
            switch (member.memberIndex()) {
                case 0 -> metricsConfiguration((MetricsConfiguration) SchemaUtils.validateSameMember($SCHEMA_METRICS_CONFIGURATION, member, value));
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
                    case 0 -> builder.metricsConfiguration(MetricsConfiguration.builder().deserializeMember(de, member).build());
                    default -> throw new IllegalArgumentException("Unexpected member: " + member.memberName());
                }
            }
        }
    }
}
