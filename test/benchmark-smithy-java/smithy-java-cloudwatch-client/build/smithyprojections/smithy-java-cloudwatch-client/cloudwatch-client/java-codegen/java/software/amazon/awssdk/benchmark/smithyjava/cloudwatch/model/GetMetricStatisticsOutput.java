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
public final class GetMetricStatisticsOutput implements SerializableStruct {

    public static final Schema $SCHEMA = Schemas.GET_METRIC_STATISTICS_OUTPUT;
    private static final Schema $SCHEMA_LABEL = $SCHEMA.member("Label");
    private static final Schema $SCHEMA_DATAPOINTS = $SCHEMA.member("Datapoints");

    public static final ShapeId $ID = $SCHEMA.id();

    private final transient String label;
    private final transient List<Datapoint> datapoints;

    private GetMetricStatisticsOutput(Builder builder) {
        this.label = builder.label;
        this.datapoints = builder.datapoints == null ? null : Collections.unmodifiableList(builder.datapoints);
    }

    /**
     * A label for the specified metric.
     */
    public String getLabel() {
        return label;
    }

    /**
     * The data points for the specified metric.
     */
    public List<Datapoint> getDatapoints() {
        if (datapoints == null) {
            return Collections.emptyList();
        }
        return datapoints;
    }

    public boolean hasDatapoints() {
        return datapoints != null;
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
        GetMetricStatisticsOutput that = (GetMetricStatisticsOutput) other;
        return Objects.equals(this.label, that.label)
               && Objects.equals(this.datapoints, that.datapoints);
    }

    @Override
    public int hashCode() {
        int $hc = Objects.hashCode(label);
        $hc = 31 * $hc + Objects.hashCode(datapoints);
        return $hc;
    }

    @Override
    public Schema schema() {
        return $SCHEMA;
    }

    @Override
    public void serializeMembers(ShapeSerializer serializer) {
        if (label != null) {
            serializer.writeString($SCHEMA_LABEL, label);
        }
        if (datapoints != null) {
            serializer.writeList($SCHEMA_DATAPOINTS, datapoints, datapoints.size(), SharedSerde.DatapointsSerializer.INSTANCE);
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T getMemberValue(Schema member) {
        return switch (member.memberIndex()) {
            case 0 -> (T) SchemaUtils.validateSameMember($SCHEMA_LABEL, member, label);
            case 1 -> (T) SchemaUtils.validateSameMember($SCHEMA_DATAPOINTS, member, datapoints);
            default -> throw new IllegalArgumentException("Attempted to get non-existent member: " + member.id());
        };
    }

    /**
     * Create a new builder containing all the current property values of this object.
     *
     * <p><strong>Note:</strong> This method performs only a shallow copy of the original properties.
     *
     * @return a builder for {@link GetMetricStatisticsOutput}.
     */
    public Builder toBuilder() {
        var builder = new Builder();
        builder.label(this.label);
        builder.datapoints(this.datapoints);
        return builder;
    }

    /**
     * @return returns a new Builder.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for {@link GetMetricStatisticsOutput}.
     */
    public static final class Builder implements ShapeBuilder<GetMetricStatisticsOutput> {
        private String label;
        private List<Datapoint> datapoints;

        private Builder() {}

        @Override
        public Schema schema() {
            return $SCHEMA;
        }

        /**
         * A label for the specified metric.
         *
         * @return this builder.
         */
        public Builder label(String label) {
            this.label = label;
            return this;
        }

        /**
         * The data points for the specified metric.
         *
         * @return this builder.
         */
        public Builder datapoints(List<Datapoint> datapoints) {
            this.datapoints = datapoints;
            return this;
        }

        @Override
        public GetMetricStatisticsOutput build() {
            return new GetMetricStatisticsOutput(this);
        }

        @Override
        @SuppressWarnings("unchecked")
        public void setMemberValue(Schema member, Object value) {
            switch (member.memberIndex()) {
                case 0 -> label((String) SchemaUtils.validateSameMember($SCHEMA_LABEL, member, value));
                case 1 -> datapoints((List<Datapoint>) SchemaUtils.validateSameMember($SCHEMA_DATAPOINTS, member, value));
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
                    case 0 -> builder.label(de.readString(member));
                    case 1 -> builder.datapoints(SharedSerde.deserializeDatapoints(member, de));
                    default -> throw new IllegalArgumentException("Unexpected member: " + member.memberName());
                }
            }
        }
    }
}
