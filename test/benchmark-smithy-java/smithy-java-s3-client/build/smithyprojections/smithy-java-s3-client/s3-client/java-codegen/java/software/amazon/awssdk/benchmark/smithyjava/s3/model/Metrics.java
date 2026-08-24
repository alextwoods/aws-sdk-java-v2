package software.amazon.awssdk.benchmark.smithyjava.s3.model;

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
 * A container specifying replication metrics-related settings enabling replication metrics and events.
 */
@SmithyGenerated
public final class Metrics implements SerializableStruct {

    public static final Schema $SCHEMA = Schemas3.METRICS;
    private static final Schema $SCHEMA_STATUS = $SCHEMA.member("Status");
    private static final Schema $SCHEMA_EVENT_THRESHOLD = $SCHEMA.member("EventThreshold");

    public static final ShapeId $ID = $SCHEMA.id();

    private final transient MetricsStatus status;
    private final transient ReplicationTimeValue eventThreshold;

    private Metrics(Builder builder) {
        this.status = builder.status;
        this.eventThreshold = builder.eventThreshold;
    }

    /**
     * Specifies whether the replication metrics are enabled.
     */
    public MetricsStatus getStatus() {
        return status;
    }

    /**
     * A container specifying the time threshold for emitting the <code>s3:Replication:OperationMissedThreshold</code>
     * event.
     */
    public ReplicationTimeValue getEventThreshold() {
        return eventThreshold;
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
        Metrics that = (Metrics) other;
        return Objects.equals(this.status, that.status)
               && Objects.equals(this.eventThreshold, that.eventThreshold);
    }

    @Override
    public int hashCode() {
        int $hc = Objects.hashCode(status);
        $hc = 31 * $hc + Objects.hashCode(eventThreshold);
        return $hc;
    }

    @Override
    public Schema schema() {
        return $SCHEMA;
    }

    @Override
    public void serializeMembers(ShapeSerializer serializer) {
        serializer.writeString($SCHEMA_STATUS, status.getValue());
        if (eventThreshold != null) {
            serializer.writeStruct($SCHEMA_EVENT_THRESHOLD, eventThreshold);
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T getMemberValue(Schema member) {
        return switch (member.memberIndex()) {
            case 0 -> (T) SchemaUtils.validateSameMember($SCHEMA_STATUS, member, status);
            case 1 -> (T) SchemaUtils.validateSameMember($SCHEMA_EVENT_THRESHOLD, member, eventThreshold);
            default -> throw new IllegalArgumentException("Attempted to get non-existent member: " + member.id());
        };
    }

    /**
     * Create a new builder containing all the current property values of this object.
     *
     * <p><strong>Note:</strong> This method performs only a shallow copy of the original properties.
     *
     * @return a builder for {@link Metrics}.
     */
    public Builder toBuilder() {
        var builder = new Builder();
        builder.status(this.status);
        builder.eventThreshold(this.eventThreshold);
        return builder;
    }

    /**
     * @return returns a new Builder.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for {@link Metrics}.
     */
    public static final class Builder implements ShapeBuilder<Metrics> {
        private final PresenceTracker tracker = PresenceTracker.of($SCHEMA);
        private MetricsStatus status;
        private ReplicationTimeValue eventThreshold;

        private Builder() {}

        @Override
        public Schema schema() {
            return $SCHEMA;
        }

        /**
         * Specifies whether the replication metrics are enabled.
         *
         * <p><strong>Required</strong>
         * @return this builder.
         */
        public Builder status(MetricsStatus status) {
            this.status = Objects.requireNonNull(status, "status cannot be null");
            tracker.setMember($SCHEMA_STATUS);
            return this;
        }

        /**
         * A container specifying the time threshold for emitting the <code>s3:Replication:OperationMissedThreshold</code>
         * event.
         *
         * @return this builder.
         */
        public Builder eventThreshold(ReplicationTimeValue eventThreshold) {
            this.eventThreshold = eventThreshold;
            return this;
        }

        @Override
        public Metrics build() {
            tracker.validate();
            return new Metrics(this);
        }

        @Override
        @SuppressWarnings("unchecked")
        public void setMemberValue(Schema member, Object value) {
            switch (member.memberIndex()) {
                case 0 -> status((MetricsStatus) SchemaUtils.validateSameMember($SCHEMA_STATUS, member, value));
                case 1 -> eventThreshold((ReplicationTimeValue) SchemaUtils.validateSameMember($SCHEMA_EVENT_THRESHOLD, member, value));
                default -> ShapeBuilder.super.setMemberValue(member, value);
            }
        }

        @Override
        public ShapeBuilder<Metrics> errorCorrection() {
            if (tracker.allSet()) {
                return this;
            }
            if (!tracker.checkMember($SCHEMA_STATUS)) {
                status(MetricsStatus.unknown(""));
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
                    case 0 -> builder.status(MetricsStatus.builder().deserializeMember(de, member).build());
                    case 1 -> builder.eventThreshold(ReplicationTimeValue.builder().deserializeMember(de, member).build());
                    default -> throw new IllegalArgumentException("Unexpected member: " + member.memberName());
                }
            }
        }
    }
}
