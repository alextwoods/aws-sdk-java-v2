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
public final class DescribeAnomalyDetectorsOutput implements SerializableStruct {

    public static final Schema $SCHEMA = Schemas.DESCRIBE_ANOMALY_DETECTORS_OUTPUT;
    private static final Schema $SCHEMA_ANOMALY_DETECTORS = $SCHEMA.member("AnomalyDetectors");
    private static final Schema $SCHEMA_NEXT_TOKEN = $SCHEMA.member("NextToken");

    public static final ShapeId $ID = $SCHEMA.id();

    private final transient List<AnomalyDetector> anomalyDetectors;
    private final transient String nextToken;

    private DescribeAnomalyDetectorsOutput(Builder builder) {
        this.anomalyDetectors = builder.anomalyDetectors == null ? null : Collections.unmodifiableList(builder.anomalyDetectors);
        this.nextToken = builder.nextToken;
    }

    /**
     * The list of anomaly detection models returned by the operation.
     */
    public List<AnomalyDetector> getAnomalyDetectors() {
        if (anomalyDetectors == null) {
            return Collections.emptyList();
        }
        return anomalyDetectors;
    }

    public boolean hasAnomalyDetectors() {
        return anomalyDetectors != null;
    }

    /**
     * A token that you can use in a subsequent operation to retrieve the next set of results.
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
        DescribeAnomalyDetectorsOutput that = (DescribeAnomalyDetectorsOutput) other;
        return Objects.equals(this.nextToken, that.nextToken)
               && Objects.equals(this.anomalyDetectors, that.anomalyDetectors);
    }

    @Override
    public int hashCode() {
        int $hc = Objects.hashCode(anomalyDetectors);
        $hc = 31 * $hc + Objects.hashCode(nextToken);
        return $hc;
    }

    @Override
    public Schema schema() {
        return $SCHEMA;
    }

    @Override
    public void serializeMembers(ShapeSerializer serializer) {
        if (anomalyDetectors != null) {
            serializer.writeList($SCHEMA_ANOMALY_DETECTORS, anomalyDetectors, anomalyDetectors.size(), SharedSerde.AnomalyDetectorsSerializer.INSTANCE);
        }
        if (nextToken != null) {
            serializer.writeString($SCHEMA_NEXT_TOKEN, nextToken);
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T getMemberValue(Schema member) {
        return switch (member.memberIndex()) {
            case 0 -> (T) SchemaUtils.validateSameMember($SCHEMA_ANOMALY_DETECTORS, member, anomalyDetectors);
            case 1 -> (T) SchemaUtils.validateSameMember($SCHEMA_NEXT_TOKEN, member, nextToken);
            default -> throw new IllegalArgumentException("Attempted to get non-existent member: " + member.id());
        };
    }

    /**
     * Create a new builder containing all the current property values of this object.
     *
     * <p><strong>Note:</strong> This method performs only a shallow copy of the original properties.
     *
     * @return a builder for {@link DescribeAnomalyDetectorsOutput}.
     */
    public Builder toBuilder() {
        var builder = new Builder();
        builder.anomalyDetectors(this.anomalyDetectors);
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
     * Builder for {@link DescribeAnomalyDetectorsOutput}.
     */
    public static final class Builder implements ShapeBuilder<DescribeAnomalyDetectorsOutput> {
        private List<AnomalyDetector> anomalyDetectors;
        private String nextToken;

        private Builder() {}

        @Override
        public Schema schema() {
            return $SCHEMA;
        }

        /**
         * The list of anomaly detection models returned by the operation.
         *
         * @return this builder.
         */
        public Builder anomalyDetectors(List<AnomalyDetector> anomalyDetectors) {
            this.anomalyDetectors = anomalyDetectors;
            return this;
        }

        /**
         * A token that you can use in a subsequent operation to retrieve the next set of results.
         *
         * @return this builder.
         */
        public Builder nextToken(String nextToken) {
            this.nextToken = nextToken;
            return this;
        }

        @Override
        public DescribeAnomalyDetectorsOutput build() {
            return new DescribeAnomalyDetectorsOutput(this);
        }

        @Override
        @SuppressWarnings("unchecked")
        public void setMemberValue(Schema member, Object value) {
            switch (member.memberIndex()) {
                case 0 -> anomalyDetectors((List<AnomalyDetector>) SchemaUtils.validateSameMember($SCHEMA_ANOMALY_DETECTORS, member, value));
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
                    case 0 -> builder.anomalyDetectors(SharedSerde.deserializeAnomalyDetectors(member, de));
                    case 1 -> builder.nextToken(de.readString(member));
                    default -> throw new IllegalArgumentException("Unexpected member: " + member.memberName());
                }
            }
        }
    }
}
