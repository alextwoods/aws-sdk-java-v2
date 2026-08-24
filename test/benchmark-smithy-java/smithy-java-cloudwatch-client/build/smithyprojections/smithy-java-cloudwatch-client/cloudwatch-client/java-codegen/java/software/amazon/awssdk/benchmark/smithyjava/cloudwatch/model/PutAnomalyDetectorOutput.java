package software.amazon.awssdk.benchmark.smithyjava.cloudwatch.model;

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
public final class PutAnomalyDetectorOutput implements SerializableStruct {

    public static final Schema $SCHEMA = Schemas.PUT_ANOMALY_DETECTOR_OUTPUT;
    private static final Schema $SCHEMA_ANOMALY_DETECTOR_ID = $SCHEMA.member("AnomalyDetectorId");

    public static final ShapeId $ID = $SCHEMA.id();

    private final transient String anomalyDetectorId;

    private PutAnomalyDetectorOutput(Builder builder) {
        this.anomalyDetectorId = builder.anomalyDetectorId;
    }

    /**
     * The unique identifier of the anomaly detector that you created or updated.
     */
    public String getAnomalyDetectorId() {
        return anomalyDetectorId;
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
        PutAnomalyDetectorOutput that = (PutAnomalyDetectorOutput) other;
        return Objects.equals(this.anomalyDetectorId, that.anomalyDetectorId);
    }

    @Override
    public int hashCode() {
        int $hc = Objects.hashCode(anomalyDetectorId);
        return $hc;
    }

    @Override
    public Schema schema() {
        return $SCHEMA;
    }

    @Override
    public void serializeMembers(ShapeSerializer serializer) {
        if (anomalyDetectorId != null) {
            serializer.writeString($SCHEMA_ANOMALY_DETECTOR_ID, anomalyDetectorId);
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T getMemberValue(Schema member) {
        return switch (member.memberIndex()) {
            case 0 -> (T) SchemaUtils.validateSameMember($SCHEMA_ANOMALY_DETECTOR_ID, member, anomalyDetectorId);
            default -> throw new IllegalArgumentException("Attempted to get non-existent member: " + member.id());
        };
    }

    /**
     * Create a new builder containing all the current property values of this object.
     *
     * <p><strong>Note:</strong> This method performs only a shallow copy of the original properties.
     *
     * @return a builder for {@link PutAnomalyDetectorOutput}.
     */
    public Builder toBuilder() {
        var builder = new Builder();
        builder.anomalyDetectorId(this.anomalyDetectorId);
        return builder;
    }

    /**
     * @return returns a new Builder.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for {@link PutAnomalyDetectorOutput}.
     */
    public static final class Builder implements ShapeBuilder<PutAnomalyDetectorOutput> {
        private String anomalyDetectorId;

        private Builder() {}

        @Override
        public Schema schema() {
            return $SCHEMA;
        }

        /**
         * The unique identifier of the anomaly detector that you created or updated.
         *
         * @return this builder.
         */
        public Builder anomalyDetectorId(String anomalyDetectorId) {
            this.anomalyDetectorId = anomalyDetectorId;
            return this;
        }

        @Override
        public PutAnomalyDetectorOutput build() {
            return new PutAnomalyDetectorOutput(this);
        }

        @Override
        @SuppressWarnings("unchecked")
        public void setMemberValue(Schema member, Object value) {
            switch (member.memberIndex()) {
                case 0 -> anomalyDetectorId((String) SchemaUtils.validateSameMember($SCHEMA_ANOMALY_DETECTOR_ID, member, value));
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
                    case 0 -> builder.anomalyDetectorId(de.readString(member));
                    default -> throw new IllegalArgumentException("Unexpected member: " + member.memberName());
                }
            }
        }
    }
}
